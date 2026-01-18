package com.tech.n.ai.client.slack.domain.slack.service;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Slack 알림 서비스 구현체
 * 고수준 알림 서비스로 SlackContract를 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationServiceImpl implements SlackNotificationService {
    
    private final SlackContract slackContract;
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        slackContract.sendErrorNotification(message, error);
    }
    
    @Override
    public void sendSuccessNotification(String message) {
        slackContract.sendSuccessNotification(message);
    }
    
    @Override
    public void sendInfoNotification(String message) {
        slackContract.sendInfoNotification(message);
    }
    
    @Override
    public void sendBatchJobNotification(SlackDto.BatchJobResult result) {
        slackContract.sendBatchJobNotification(result);
    }
}
