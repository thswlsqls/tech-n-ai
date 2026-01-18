package com.tech.n.ai.client.slack.domain.slack.contract;

/**
 * Slack Contract 인터페이스
 * 비즈니스 로직을 위한 Slack 알림 메서드 정의
 */
public interface SlackContract {
    
    /**
     * 일반 알림 전송
     * 
     * @param request 알림 요청 정보
     */
    void sendNotification(SlackDto.NotificationRequest request);
    
    /**
     * 에러 알림 전송
     * 
     * @param message 에러 메시지
     * @param error 에러 객체
     */
    void sendErrorNotification(String message, Throwable error);
    
    /**
     * 성공 알림 전송
     * 
     * @param message 성공 메시지
     */
    void sendSuccessNotification(String message);
    
    /**
     * 정보 알림 전송
     * 
     * @param message 정보 메시지
     */
    void sendInfoNotification(String message);
    
    /**
     * 배치 작업 알림 전송
     * 
     * @param result 배치 작업 결과
     */
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}
