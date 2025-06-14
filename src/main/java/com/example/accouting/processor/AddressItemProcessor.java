package com.example.accouting.processor;

import com.example.accouting.model.Address;
import com.example.accouting.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import jakarta.annotation.PostConstruct;

/**
 * Optimized processor for handling large volumes of data (5+ million records)
 */
public class AddressItemProcessor implements ItemProcessor<Address, Address> {

    private static final Logger log = LoggerFactory.getLogger(AddressItemProcessor.class);

    @Autowired
    private AddressRepository addressRepository;

    private final AtomicLong tempIdCounter = new AtomicLong(0);

    // Cache to store known account IDs to avoid repeated database lookups
    private final Set<String> knownAccountIds = ConcurrentHashMap.newKeySet();

    // Cache to store the current tempID for each account ID
    private final Map<String, String> accountTempIdMap = new ConcurrentHashMap<>();

    // Current global tempID that all accounts will eventually share
    private String currentGlobalTempId;

    // Counter for logging progress
    private long processedCount = 0;

    // Log frequency - only log every N records
    @Value("${batch.log.frequency:10000}")
    private int logFrequency;

    @PostConstruct
    public void init() {
        // Initialize with a tempID
        currentGlobalTempId = generateTempId();

        // Pre-load existing account IDs if the dataset is not too large
        // For very large datasets, this would be skipped and the cache would be built incrementally
        try {
            Set<String> existingAccounts = addressRepository.findAllDistinctAccountIDs();
            if (existingAccounts.size() < 100000) { // Only preload if reasonable size
                knownAccountIds.addAll(existingAccounts);
                log.info("Preloaded {} existing account IDs", existingAccounts.size());
            }
        } catch (Exception e) {
            log.warn("Could not preload account IDs, will build cache incrementally", e);
        }
    }

    @Override
    public Address process(Address address) throws Exception {
        processedCount++;

        // Only log occasionally for performance reasons
        if (processedCount % logFrequency == 0) {
            log.info("Processed {} records", processedCount);
        }

        String accountId = address.getAccountID();

        // Check if we've seen this account ID before
        boolean isNewAccount = !knownAccountIds.contains(accountId);

        if (isNewAccount) {
            // Add to our known accounts cache
            knownAccountIds.add(accountId);
            // Assign the current global tempID
            accountTempIdMap.put(accountId, currentGlobalTempId);
        } else {
            // This is an existing account ID, so we need a new global tempID
            currentGlobalTempId = generateTempId();

            // Update all account IDs to use the new tempID
            // This is done in memory only - the database update is handled in batches by the writer
            accountTempIdMap.keySet().forEach(id -> accountTempIdMap.put(id, currentGlobalTempId));
        }

        // Set the tempID on the current address
        address.setTempID(currentGlobalTempId);

        // Every 100,000 records, log a summary of the current state
        if (processedCount % 100000 == 0) {
            log.info("Processed {} records, current global tempID: {}, known accounts: {}", 
                    processedCount, currentGlobalTempId, knownAccountIds.size());
        }

        return address;
    }

    private String generateTempId() {
        // Format: T + 14 zeros + counter
        return String.format("T%014d", tempIdCounter.getAndIncrement());
    }

    /**
     * This method should be called after all processing is complete
     * to update all addresses in the database with their final tempID
     */
    public void updateAllAddressesWithFinalTempId() {
        log.info("Updating all addresses with final tempID: {}", currentGlobalTempId);
        addressRepository.updateTempIDForAccountIDs(currentGlobalTempId, knownAccountIds);
        log.info("Updated {} accounts with tempID: {}", knownAccountIds.size(), currentGlobalTempId);
    }
}
