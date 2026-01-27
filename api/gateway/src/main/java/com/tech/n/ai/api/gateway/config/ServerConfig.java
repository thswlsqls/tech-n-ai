package com.tech.n.ai.api.gateway.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.gateway",
    "com.tech.n.ai.common.security.jwt"
})
public class ServerConfig {

}
