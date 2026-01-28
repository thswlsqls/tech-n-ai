package com.ebson.shrimp.tm.demo.api.gateway.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(basePackages = {
    "com.ebson.shrimp.tm.demo.api.gateway",
    "com.ebson.shrimp.tm.demo.common.security.jwt"
})
public class ServerConfig {

}
