package com.example.accouting.runner;

import com.example.accouting.model.Address;
import com.example.accouting.repository.AddressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class BatchJobRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BatchJobRunner.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job addressJob;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public void run(String... args) throws Exception {
        // Initialize sample data
        initSampleData();

        // Run the batch job
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        jobLauncher.run(addressJob, jobParameters);

        // Print final results
        log.info("Final results:");
        addressRepository.findAll().forEach(address -> log.info(address.toString()));
    }

    private void initSampleData() {
        // Create sample addresses based on the issue description
        List<Address> addresses = Arrays.asList(
                new Address("org1", "A", "AC101"),
                new Address("org2", "B", "AC102"),
                new Address("org3", "C", "AC103"),
                new Address("org4", "A", "AC104"),
                new Address("org5", "E", "AC101"),
                new Address("org5", "D", "AC102"),
                new Address("org5", "B", "AC101"),
                new Address("org5", "A", "AC103"),
                new Address("org5", "A", "AC102")
        );

        // Save all addresses
        addressRepository.saveAll(addresses);
        log.info("Sample data initialized with {} addresses", addresses.size());
    }
}