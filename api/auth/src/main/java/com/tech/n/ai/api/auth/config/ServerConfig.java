package com.tech.n.ai.api.auth.config;


import com.tech.n.ai.client.feign.domain.oauth.config.OAuthFeignConfig;
import com.tech.n.ai.client.mail.config.MailConfig;
import com.tech.n.ai.common.core.config.RedisConfig;
import com.tech.n.ai.common.security.config.PasswordEncoderConfig;
import com.tech.n.ai.common.security.config.SecurityConfig;
import com.tech.n.ai.datasource.mariadb.config.ApiDomainConfig;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;


@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.auth",
    "com.tech.n.ai.datasource.mariadb",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.exception"
})
@Import({
    ApiDomainConfig.class,

    RedisConfig.class,
    PasswordEncoderConfig.class,
    SecurityConfig.class,

    OAuthFeignConfig.class,
    MailConfig.class
})
public class ServerConfig {

}
