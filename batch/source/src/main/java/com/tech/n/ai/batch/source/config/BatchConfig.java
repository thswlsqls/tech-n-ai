package com.tech.n.ai.batch.source.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing(
    dataSourceRef = "batchMetaDataSource",
    transactionManagerRef = "primaryPlatformTransactionManager"
)
public class BatchConfig extends BatchAutoConfiguration {

}
