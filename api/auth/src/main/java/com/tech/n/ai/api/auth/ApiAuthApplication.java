package com.tech.n.ai.api.auth;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(excludeName = {
	"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
	"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
	"org.springframework.boot.session.autoconfigure.SessionAutoConfiguration",
	"org.springframework.boot.session.jdbc.autoconfigure.JdbcSessionAutoConfiguration"
})
public class ApiAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiAuthApplication.class, args);
	}

}
