package com.tech.n.ai.api.chatbot.config;


import com.tech.n.ai.common.core.config.RedisConfig;
import com.tech.n.ai.common.kafka.config.KafkaConfig;
import com.tech.n.ai.common.security.config.SecurityConfig;
import com.tech.n.ai.domain.mariadb.config.ApiDomainConfig;
import com.tech.n.ai.domain.mongodb.config.MongoIndexConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.chatbot",
    "com.tech.n.ai.domain.mariadb",
    "com.tech.n.ai.domain.mongodb",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.kafka",
    "com.tech.n.ai.common.security",
    "com.tech.n.ai.common.exception"
})
@Import({
    ApiDomainConfig.class,
    MongoIndexConfig.class,
    KafkaConfig.class,
    RedisConfig.class,
    SecurityConfig.class
})
public class ServerConfig {

}
