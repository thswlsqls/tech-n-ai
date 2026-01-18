package com.tech.n.ai.client.slack.domain.slack.contract;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Slack DTO 클래스
 * Slack API 요청/응답을 위한 데이터 전송 객체
 */
public class SlackDto {
    
    @Builder
    public record NotificationRequest(
            /**
             * 알림 메시지
             */
            String message,
            
            /**
             * 알림 타입
             */
            NotificationType type,
            
            /**
             * 추가 컨텍스트 정보
             */
            Map<String, Object> context
    ) {}
    
    @Builder
    public record BatchJobResult(
            /**
             * 작업 이름
             */
            String jobName,
            
            /**
             * 작업 상태
             */
            JobStatus status,
            
            /**
             * 시작 시간
             */
            LocalDateTime startTime,
            
            /**
             * 종료 시간
             */
            LocalDateTime endTime,
            
            /**
             * 처리된 항목 수
             */
            int processedItems,
            
            /**
             * 에러 메시지 (실패 시)
             */
            String errorMessage
    ) {}
    
    @Builder
    public record SlackMessage(
            /**
             * 간단한 텍스트 메시지 (fallback)
             */
            String text,
            
            /**
             * Block Kit 블록 리스트
             */
            List<Block> blocks
    ) {}
    
    @Builder
    public record Block(
            /**
             * 블록 타입 (section, divider, context 등)
             */
            String type,
            
            /**
             * 텍스트 정보 (type이 section인 경우)
             */
            Map<String, Object> text,
            
            /**
             * 요소 리스트 (type이 context인 경우)
             */
            List<Map<String, Object>> elements
    ) {}
    
    /**
     * 알림 타입 열거형
     */
    public enum NotificationType {
        ERROR,
        SUCCESS,
        INFO,
        BATCH_JOB
    }
    
    /**
     * 배치 작업 상태 열거형
     */
    public enum JobStatus {
        SUCCESS,
        FAILED,
        IN_PROGRESS
    }
}
