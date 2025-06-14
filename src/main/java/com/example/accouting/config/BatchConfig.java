package com.example.accouting.config;

import com.example.accouting.model.Address;
import com.example.accouting.processor.AddressItemProcessor;
import com.example.accouting.repository.AddressRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.batch.item.data.builder.RepositoryItemWriterBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Batch configuration optimized for processing large volumes of data (5+ million records)
 * Uses separate datasources for application data and Spring Batch metadata
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    @Autowired
    @Qualifier("batchDataSource")
    private DataSource batchDataSource;

    @Autowired
    @Qualifier("batchTransactionManager")
    private PlatformTransactionManager batchTransactionManager;

    @Value("${batch.chunk.size:1000}")
    private int chunkSize;

    @Value("${batch.page.size:10000}")
    private int pageSize;

    @Value("${batch.max.threads:4}")
    private int maxThreads;

    /**
     * Configure a custom JobRepository that uses the batch datasource
     */
    @Bean
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(batchDataSource);
        factory.setTransactionManager(batchTransactionManager);
        factory.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factory.setTablePrefix("BATCH_");
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    /**
     * Configure a paged repository reader for efficient processing of large datasets
     */
    @Bean
    public RepositoryItemReader<Address> reader() {
        Map<String, Sort.Direction> sorts = new HashMap<>();
        sorts.put("id", Sort.Direction.ASC);

        return new RepositoryItemReaderBuilder<Address>()
                .name("addressReader")
                .repository(addressRepository)
                .methodName("findAll")
                .pageSize(pageSize)
                .sorts(sorts)
                .build();
    }

    /**
     * Configure the processor with optimizations for large datasets
     */
    @Bean
    public AddressItemProcessor processor() {
        return new AddressItemProcessor();
    }

    /**
     * Configure a JDBC batch writer for efficient batch updates
     */
    @Bean
    public JdbcBatchItemWriter<Address> jdbcBatchWriter() {
        return new JdbcBatchItemWriterBuilder<Address>()
                .dataSource(primaryDataSource)
                .sql("UPDATE address SET temp_id = :tempID WHERE id = :id")
                .itemSqlParameterSourceProvider(BeanPropertySqlParameterSource::new)
                .build();
    }

    /**
     * Configure a repository writer as fallback
     */
    @Bean
    public RepositoryItemWriter<Address> repositoryWriter() {
        return new RepositoryItemWriterBuilder<Address>()
                .repository(addressRepository)
                .methodName("save")
                .build();
    }

    /**
     * Configure a task executor for parallel processing
     */
    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("batch-");
        executor.setConcurrencyLimit(maxThreads);
        return executor;
    }

    /**
     * Configure the processing step with increased chunk size and parallel processing
     */
    @Bean
    public Step processAddressStep() throws Exception {
        return new StepBuilder("processAddressStep", jobRepository())
                .<Address, Address>chunk(chunkSize, batchTransactionManager)
                .reader(reader())
                .processor(processor())
                .writer(jdbcBatchWriter())
                .taskExecutor(taskExecutor())
                .build();
    }

    /**
     * Configure a step to update all addresses with the final tempID
     */
    @Bean
    public Step finalUpdateStep(AddressItemProcessor processor) throws Exception {
        return new StepBuilder("finalUpdateStep", jobRepository())
                .tasklet((contribution, chunkContext) -> {
                    processor.updateAllAddressesWithFinalTempId();
                    return null;
                }, batchTransactionManager)
                .build();
    }

    /**
     * Configure the job with both processing and final update steps
     */
    @Bean
    public Job addressJob(Step processAddressStep, Step finalUpdateStep) throws Exception {
        return new JobBuilder("addressJob", jobRepository())
                .start(processAddressStep)
                .next(finalUpdateStep)
                .build();
    }
}
