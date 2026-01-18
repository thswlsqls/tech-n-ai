package com.tech.n.ai.api.gateway.config;

import com.tech.n.ai.datasource.aurora.config.ApiDomainConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@Configuration
@EnableJpaRepositories(value = {
    "com.tech.n.ai.datasource.aurora.repository",
})
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.gateway",
    "com.tech.n.ai.datasource.aurora"
})
@Import({
    ApiDomainConfig.class,
})
public class ServerConfig {

//    @Bean
//    public JpaResultMapper jpaResultMapper() {
//        return new JpaResultMapper();
//    }
}
