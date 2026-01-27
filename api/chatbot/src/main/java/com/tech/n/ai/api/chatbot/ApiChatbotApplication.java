package com.tech.n.ai.api.chatbot;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	FlywayAutoConfiguration.class
})
public class ApiChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiChatbotApplication.class, args);
    }

}
