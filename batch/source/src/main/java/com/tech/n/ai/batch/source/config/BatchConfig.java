package com.tech.n.ai.batch.source.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing(
    // dataSourceRef = "batchMetaDataSource",
    transactionManagerRef = "primaryPlatformTransactionManager"
)
public class BatchConfig extends DefaultBatchConfiguration {

}
