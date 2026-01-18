package com.tech.n.ai.batch.source.config;


import com.tech.n.ai.client.feign.config.ContestInternalFeignConfig;
import com.tech.n.ai.client.feign.config.NewsInternalFeignConfig;
import com.tech.n.ai.datasource.aurora.config.BatchDomainConfig;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EnableJpaRepositories(basePackages = {
    "com.tech.n.ai.datasource.aurora"
}
, transactionManagerRef = "jpaTransactionManagerAutoCommitF"
, entityManagerFactoryRef = "primaryEMF")
@ComponentScan(basePackages = {
    "com.tech.n.ai.datasource.aurora",
})
@Import({
    BatchDomainConfig.class,
    ContestInternalFeignConfig.class,
    NewsInternalFeignConfig.class,
})
public class ServerConfig {

    private final Environment env;

    @Qualifier(value = "batchBusinessWriterDataSource")
    private DataSource batchBusinessWriterDataSource;

    public ServerConfig(@Qualifier("batchBusinessWriterDataSource") DataSource batchBusinessWriterDataSource
        , Environment env) {
        this.env = env;
        this.batchBusinessWriterDataSource = batchBusinessWriterDataSource;
    }

//    @Bean
//    public JpaResultMapper jpaResultMapper() {
//        return new JpaResultMapper();
//    }

}
