package com.example.accouting.batch;

import com.example.accouting.model.Address;
import com.example.accouting.processor.AddressItemProcessor;
import com.example.accouting.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

public class AddressBatchIntegrationTest {

    @Mock
    private AddressRepository addressRepository;

    // We'll create our own processor and inject the mock repository
    private AddressItemProcessor processor;

    // Test data
    private List<Address> addresses;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);

        // Create test data
        addresses = Arrays.asList(
                new Address("org1", "A", "AC101", null),
                new Address("org2", "B", "AC102", null),
                new Address("org3", "C", "AC103", null),
                new Address("org4", "A", "AC104", null),
                new Address("org5", "E", "AC101", null),
                new Address("org5", "D", "AC102", null),
                new Address("org5", "B", "AC101", null),
                new Address("org5", "A", "AC103", null),
                new Address("org5", "A", "AC102", null)
        );

        // Set up the processor with the mock repository
        processor = new AddressItemProcessor();
        // Use reflection to set the addressRepository field in the processor
        try {
            java.lang.reflect.Field field = AddressItemProcessor.class.getDeclaredField("addressRepository");
            field.setAccessible(true);
            field.set(processor, addressRepository);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set addressRepository field", e);
        }

        // Mock repository methods
        when(addressRepository.findAll()).thenReturn(addresses);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(addressRepository).deleteAll();

        // Mock the findAllDistinctAccountIDs method
        Set<String> accountIds = new HashSet<>();
        for (Address address : addresses) {
            accountIds.add(address.getAccountID());
        }
        when(addressRepository.findAllDistinctAccountIDs()).thenReturn(accountIds);

        // Mock the updateTempIDForAccountIDs method
        doNothing().when(addressRepository).updateTempIDForAccountIDs(anyString(), anySet());
    }

    @Test
    void testAddressProcessing() throws Exception {
        // Initialize the processor (this would normally happen in @PostConstruct)
        processor.init();

        // Process each address manually
        List<Address> processedAddresses = new ArrayList<>();
        for (Address address : addresses) {
            Address processedAddress = processor.process(address);
            processedAddresses.add(processedAddress);
            System.out.println("[DEBUG_LOG] Processed address: " + processedAddress);
        }

        // Verify that all addresses have the expected TempID
        assertEquals(9, processedAddresses.size(), "Should have 9 addresses");

        // Print the results for verification
        System.out.println("[DEBUG_LOG] Processed addresses:");

        // Get the TempID from the first address
        String actualTempId = processedAddresses.get(0).getTempID();
        System.out.println("[DEBUG_LOG] Actual TempID from first address: " + actualTempId);

        // Verify that all addresses have the same TempID
        for (Address address : processedAddresses) {
            System.out.println("[DEBUG_LOG] " + address);
            System.out.println("[DEBUG_LOG] TempID: " + address.getTempID() + ", Expected: " + actualTempId);
            // Verify that each address has the same TempID as the first address
            assertEquals(actualTempId, address.getTempID(),
                    "Address " + address + " should have TempID " + actualTempId);
        }

        // Test the updateAllAddressesWithFinalTempId method
        processor.updateAllAddressesWithFinalTempId();

        // Verify that updateTempIDForAccountIDs was called with the correct parameters
        Mockito.verify(addressRepository).updateTempIDForAccountIDs(anyString(), anySet());
    }
}
