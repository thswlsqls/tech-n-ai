package com.tech.n.ai.api.contest.config;


import com.tech.n.ai.datasource.mongodb.config.MongoIndexConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.contest",
    "com.tech.n.ai.datasource.mongodb"
})
@Import({
    MongoIndexConfig.class,
})
@EnableConfigurationProperties(ContestConfig.class)
public class ServerConfig {

}
