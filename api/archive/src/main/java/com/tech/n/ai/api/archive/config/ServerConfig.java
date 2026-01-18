package com.tech.n.ai.api.archive.config;

import com.tech.n.ai.datasource.aurora.config.ApiDomainConfig;
import com.tech.n.ai.datasource.mongodb.config.MongoIndexConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Archive API 서버 설정
 */
@Configuration
@EnableJpaRepositories(value = {
    "com.tech.n.ai.datasource.aurora.repository",
})
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.archive",
    "com.tech.n.ai.datasource.aurora",
    "com.tech.n.ai.datasource.mongodb"
})
@Import({
    ApiDomainConfig.class,
    MongoIndexConfig.class,
})
@EnableConfigurationProperties(ArchiveConfig.class)
public class ServerConfig {
}
