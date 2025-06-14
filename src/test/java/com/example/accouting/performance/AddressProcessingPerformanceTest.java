package com.example.accouting.performance;

import com.example.accouting.model.Address;
import com.example.accouting.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Performance test for processing large volumes of address records.
 * This test simulates processing a configurable number of records to evaluate
 * the performance of the batch processing system.
 */
@SpringBootTest
@ActiveProfiles("test")
public class AddressProcessingPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(AddressProcessingPerformanceTest.class);
    
    // Number of records to generate for the test
    // For a real test with 5 million records, set this to 5_000_000
    // For quick testing, use a smaller number like 10_000
    private static final int TEST_RECORD_COUNT = 10_000;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job addressJob;

    
    @BeforeEach
    void setUp() {
        // Clear the database
        addressRepository.deleteAll();
        log.info("Database cleared");
    }
    
    @Test
    void testLargeDatasetProcessing() throws Exception {
        // Generate test data
        log.info("Generating {} test records...", TEST_RECORD_COUNT);
        long startGeneration = System.nanoTime();
        
        List<Address> addresses = generateTestData(TEST_RECORD_COUNT);
        
        long generationTime = System.nanoTime() - startGeneration;
        log.info("Generated {} records in {} ms", 
                TEST_RECORD_COUNT, 
                TimeUnit.NANOSECONDS.toMillis(generationTime));
        
        // Save test data to database
        log.info("Saving test data to database...");
        long startSaving = System.nanoTime();
        
        // Save in batches to avoid memory issues
        int batchSize = 1000;
        for (int i = 0; i < addresses.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, addresses.size());
            addressRepository.saveAll(addresses.subList(i, endIndex));
            
            if (i % 10000 == 0 && i > 0) {
                log.info("Saved {} records so far...", i);
            }
        }
        
        long savingTime = System.nanoTime() - startSaving;
        log.info("Saved {} records in {} ms", 
                TEST_RECORD_COUNT, 
                TimeUnit.NANOSECONDS.toMillis(savingTime));
        
        // Run the batch job
        log.info("Starting batch job to process {} records...", TEST_RECORD_COUNT);
        long startProcessing = System.nanoTime();
        
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        
        JobExecution jobExecution = jobLauncher.run(addressJob, jobParameters);
        
        long processingTime = System.nanoTime() - startProcessing;
        log.info("Processed {} records in {} ms", 
                TEST_RECORD_COUNT, 
                TimeUnit.NANOSECONDS.toMillis(processingTime));
        
        // Verify job status
        log.info("Job status: {}", jobExecution.getStatus());
        log.info("Job exit status: {}", jobExecution.getExitStatus());
        
        // Calculate and log performance metrics
        double recordsPerSecond = (double) TEST_RECORD_COUNT / 
                (TimeUnit.NANOSECONDS.toSeconds(processingTime) + 1); // Add 1 to avoid division by zero
        
        log.info("Performance metrics:");
        log.info("Total records: {}", TEST_RECORD_COUNT);
        log.info("Total processing time: {} seconds", 
                TimeUnit.NANOSECONDS.toSeconds(processingTime));
        log.info("Records per second: {}", String.format("%.2f", recordsPerSecond));
        
        // Log memory usage
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        
        log.info("Memory usage:");
        log.info("Used memory: {} MB", usedMemory);
        log.info("Total memory: {} MB", totalMemory);
        log.info("Max memory: {} MB", maxMemory);
        
        // Print summary for easy comparison
        System.out.println("[DEBUG_LOG] Performance Test Summary:");
        System.out.println("[DEBUG_LOG] Records processed: " + TEST_RECORD_COUNT);
        System.out.println("[DEBUG_LOG] Processing time: " + 
                TimeUnit.NANOSECONDS.toSeconds(processingTime) + " seconds");
        System.out.println("[DEBUG_LOG] Records per second: " + 
                String.format("%.2f", recordsPerSecond));
        System.out.println("[DEBUG_LOG] Memory used: " + usedMemory + " MB");
    }
    
    /**
     * Generate test data with unique values to simulate real-world data
     */
    private List<Address> generateTestData(int count) {
        List<Address> addresses = new ArrayList<>(count);
        
        // Create a mix of organizations, AIC groups, and account IDs
        String[] orgIds = {"org1", "org2", "org3", "org4", "org5", "org6", "org7", "org8", "org9", "org10"};
        String[] aicGroupIds = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J"};
        String[] baseAccountIds = {"AC101", "AC102", "AC103", "AC104", "AC105", "AC106", "AC107", "AC108", "AC109", "AC110"};
        
        // Generate addresses with some patterns to trigger the tempID update logic
        for (int i = 0; i < count; i++) {
            String orgId = orgIds[i % orgIds.length];
            String aicGroupId = aicGroupIds[i % aicGroupIds.length];
            
            // Create some duplicate account IDs to trigger the update logic
            // but also ensure we have enough unique IDs to simulate real data
            String accountId;
            if (i % 5 == 0) {
                // Reuse an existing account ID to trigger updates
                accountId = baseAccountIds[i % baseAccountIds.length];
            } else {
                // Create a unique account ID
                accountId = baseAccountIds[i % baseAccountIds.length] + "-" + (i / baseAccountIds.length);
            }
            
            addresses.add(new Address(orgId, aicGroupId, accountId, null));
            
            // Log progress for large datasets
            if (i % 100000 == 0 && i > 0) {
                log.info("Generated {} records so far...", i);
            }
        }
        
        return addresses;
    }
}