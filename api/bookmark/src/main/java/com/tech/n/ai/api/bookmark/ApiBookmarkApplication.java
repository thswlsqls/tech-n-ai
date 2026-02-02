package com.tech.n.ai.api.bookmark;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;


@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	MongoAutoConfiguration.class,
	MongoDataAutoConfiguration.class,
	MongoReactiveAutoConfiguration.class,
	MongoReactiveDataAutoConfiguration.class,
	SessionAutoConfiguration.class,
	FlywayAutoConfiguration.class
})
public class ApiBookmarkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiBookmarkApplication.class, args);
	}

}
