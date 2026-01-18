package com.tech.n.ai.api.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(AuthApplication.class);
		app.setDefaultProperties(java.util.Map.of("spring.config.name", "application-auth-api"));
		app.run(args);
	}

}
