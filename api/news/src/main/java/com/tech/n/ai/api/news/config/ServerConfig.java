package com.tech.n.ai.api.news.config;


import com.tech.n.ai.datasource.mongodb.config.MongoClientConfig;
import com.tech.n.ai.datasource.mongodb.config.MongoIndexConfig;
import com.tech.n.ai.datasource.mongodb.config.VectorSearchIndexConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.news",
    "com.tech.n.ai.datasource.mongodb"
})
@Import({
    MongoClientConfig.class,
    MongoIndexConfig.class,
    VectorSearchIndexConfig.class
})
@EnableConfigurationProperties(NewsConfig.class)
public class ServerConfig {

}
