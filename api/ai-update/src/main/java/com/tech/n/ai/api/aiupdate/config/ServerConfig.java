package com.tech.n.ai.api.aiupdate.config;

import com.tech.n.ai.datasource.mongodb.config.MongoIndexConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.aiupdate",
    "com.tech.n.ai.datasource.mongodb"
})
@Import({
    MongoIndexConfig.class,
})
@EnableConfigurationProperties(AiUpdateConfig.class)
public class ServerConfig {

}
