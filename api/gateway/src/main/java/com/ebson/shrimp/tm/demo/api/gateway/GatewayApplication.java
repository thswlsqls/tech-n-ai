package com.ebson.shrimp.tm.demo.api.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;


@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	FlywayAutoConfiguration.class,
	MongoAutoConfiguration.class,
	MongoDataAutoConfiguration.class,
	MongoReactiveAutoConfiguration.class,
	MongoReactiveDataAutoConfiguration.class,
	RedisAutoConfiguration.class,
	RedisReactiveAutoConfiguration.class,
	SecurityAutoConfiguration.class,
	ReactiveSecurityAutoConfiguration.class,
	ReactiveUserDetailsServiceAutoConfiguration.class,
	ReactiveManagementWebSecurityAutoConfiguration.class
})
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
