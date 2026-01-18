package com.tech.n.ai.api.archive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiArchiveApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ApiArchiveApplication.class);
		app.setDefaultProperties(java.util.Map.of("spring.config.name", "application-archive-api"));
		app.run(args);
	}

}
