package com.tech.n.ai.batch.source.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository(
    dataSourceRef = "batchMetaDataSource",
    transactionManagerRef = "primaryPlatformTransactionManager"
)
public class BatchConfig {

}
