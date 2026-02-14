package com.tech.n.ai.api.emergingtech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
	"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
	"org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
	"org.springframework.boot.jpa.autoconfigure.HibernateJpaAutoConfiguration"
})
public class ApiEmergingTechApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiEmergingTechApplication.class, args);
	}

}
