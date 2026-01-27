package com.tech.n.ai.client.mail.config;

import com.tech.n.ai.client.mail.domain.mail.service.EmailSender;
import com.tech.n.ai.client.mail.domain.mail.service.SmtpEmailSender;
import com.tech.n.ai.client.mail.domain.mail.template.EmailTemplateService;
import com.tech.n.ai.client.mail.domain.mail.template.ThymeleafEmailTemplateService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;

import java.util.concurrent.Executor;

/**
 * 이메일 발송 관련 Bean 설정.
 * client/slack 모듈의 SlackConfig 패턴을 참조.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
    
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor(MailProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getAsync().getCorePoolSize());
        executor.setMaxPoolSize(properties.getAsync().getMaxPoolSize());
        executor.setQueueCapacity(properties.getAsync().getQueueCapacity());
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }
    
    @Bean
    @ConditionalOnMissingBean(EmailSender.class)
    public EmailSender emailSender(
            JavaMailSender mailSender,
            MailProperties properties,
            Executor mailTaskExecutor) {
        return new SmtpEmailSender(mailSender, properties, mailTaskExecutor);
    }
    
    @Bean
    @ConditionalOnMissingBean(EmailTemplateService.class)
    public EmailTemplateService emailTemplateService(TemplateEngine templateEngine) {
        return new ThymeleafEmailTemplateService(templateEngine);
    }
}
