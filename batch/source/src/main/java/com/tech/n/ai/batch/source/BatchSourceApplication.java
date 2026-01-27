package com.tech.n.ai.batch.source;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	DataSourceTransactionManagerAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class
})
public class BatchSourceApplication {

	public static void main(String[] args) {
//		SpringApplication.run(BatchSourceApplication.class, args);

		System.exit(
			SpringApplication.exit(
				SpringApplication.run(BatchSourceApplication.class)
			)
		)
		;
	}

}
