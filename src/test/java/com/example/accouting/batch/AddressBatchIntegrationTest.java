package com.example.accouting.batch;

import com.example.accouting.model.Address;
import com.example.accouting.processor.AddressItemProcessor;
import com.example.accouting.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class AddressBatchIntegrationTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AddressItemProcessor processor;

    @Test
    void testAddressProcessing() throws Exception {
        // Clear the database
        addressRepository.deleteAll();

        // Initialize test data
        List<Address> addresses = Arrays.asList(
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

        // Save all addresses
        addressRepository.saveAll(addresses);

        // Process each address manually and save it back to the database
        for (Address address : addressRepository.findAll()) {
            Address processedAddress = processor.process(address);
            addressRepository.save(processedAddress);
        }

        // Verify that all addresses have the expected TempID
        List<Address> processedAddresses = addressRepository.findAll();
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
    }
}
