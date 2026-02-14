package com.tech.n.ai.api.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(excludeName = {
    "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration",
    "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration"
})
@EnableScheduling
public class ApiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAgentApplication.class, args);
    }
}
