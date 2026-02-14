package com.tech.n.ai.api.bookmark;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(excludeName = {
	"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
	"org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration",
	"org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration",
	"org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration",
	"org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration",
	"org.springframework.boot.session.autoconfigure.SessionAutoConfiguration",
	"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
public class ApiBookmarkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiBookmarkApplication.class, args);
	}

}
