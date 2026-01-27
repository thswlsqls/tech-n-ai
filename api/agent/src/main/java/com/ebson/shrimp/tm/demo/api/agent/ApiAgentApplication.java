package com.tech.n.ai.api.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
@EnableScheduling
public class ApiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAgentApplication.class, args);
    }
}
