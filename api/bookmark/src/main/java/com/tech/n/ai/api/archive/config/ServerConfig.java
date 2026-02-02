package com.tech.n.ai.api.archive.config;

import com.tech.n.ai.common.security.config.SecurityConfig;
import com.tech.n.ai.datasource.mariadb.config.ApiDomainConfig;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Archive API 서버 설정
 */
@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.archive",
    "com.tech.n.ai.common.security",
    "com.tech.n.ai.datasource.mariadb"
})
@Import({
    ApiDomainConfig.class,
    SecurityConfig.class,
})
@EnableConfigurationProperties(ArchiveConfig.class)
public class ServerConfig {
}
