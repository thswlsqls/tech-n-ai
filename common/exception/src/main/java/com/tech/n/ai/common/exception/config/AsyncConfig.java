package com.tech.n.ai.common.exception.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 설정
 * ExceptionLoggingService의 @Async 메서드를 활성화
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

