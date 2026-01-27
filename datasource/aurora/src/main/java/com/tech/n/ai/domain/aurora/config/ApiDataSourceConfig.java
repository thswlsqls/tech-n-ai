package com.tech.n.ai.datasource.mariadb.config;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Profile("api-domain")
@Configuration
@RequiredArgsConstructor
public class ApiDataSourceConfig {

    private final Environment env;

    @Bean(name="apiWriterHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.api.writer.hikari")
    public HikariConfig apiWriterHikariConfig() {
        return new HikariConfig();
    }

    @Bean(name="apiReaderHikariConfig")
    @ConfigurationProperties(prefix = "spring.datasource.api.reader.hikari")
    public HikariConfig apiReaderHikariConfig() {
        return new HikariConfig();
    }


    @Bean(name= "apiWriterDataSource")
    @Primary
    public DataSource apiWriterDataSource(@Qualifier("apiWriterHikariConfig") HikariConfig apiWriterHikariConfig) {

        // writer
        apiWriterHikariConfig.setPoolName("API-WRITER");
        apiWriterHikariConfig.setJdbcUrl(env.getProperty("spring.datasource.writer.url"));
        apiWriterHikariConfig.setUsername(env.getProperty("spring.datasource.writer.username"));
        apiWriterHikariConfig.setPassword(env.getProperty("spring.datasource.writer.password"));
        DataSource writerDataSource = new HikariDataSource(apiWriterHikariConfig);
        log.warn("Will be WORK WITH WRITER");

        return writerDataSource;
    }

    @Bean(name= "apiReaderDataSource")
    public DataSource apiReaderDataSource(@Qualifier("apiReaderHikariConfig") HikariConfig apiReaderHikariConfig) {

        // reader
        apiReaderHikariConfig.setReadOnly(true);
        apiReaderHikariConfig.setPoolName("API-READER");
        apiReaderHikariConfig.setJdbcUrl(env.getProperty("spring.datasource.reader.url"));
        apiReaderHikariConfig.setUsername(env.getProperty("spring.datasource.reader.username"));
        apiReaderHikariConfig.setPassword(env.getProperty("spring.datasource.reader.password"));
        DataSource readerDataSource = new HikariDataSource(apiReaderHikariConfig);
        log.warn("Will be WORK WITH READER");

        return readerDataSource;
    }

}

