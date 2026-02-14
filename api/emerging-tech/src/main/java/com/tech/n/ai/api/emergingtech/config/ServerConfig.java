package com.tech.n.ai.api.emergingtech.config;

import com.tech.n.ai.domain.mongodb.config.MongoClientConfig;
import com.tech.n.ai.domain.mongodb.config.MongoIndexConfig;
import com.tech.n.ai.domain.mongodb.config.VectorSearchIndexConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.emergingtech",
    "com.tech.n.ai.domain.mongodb",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.exception"
})
@Import({
    MongoClientConfig.class,
    MongoIndexConfig.class,
    VectorSearchIndexConfig.class
})
@EnableConfigurationProperties(EmergingTechConfig.class)
public class ServerConfig {

}
