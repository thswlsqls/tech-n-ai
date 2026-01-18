package com.tech.n.ai.client.slack.domain.slack.api;

import com.tech.n.ai.client.slack.domain.slack.builder.SlackMessageBuilder;
import com.tech.n.ai.client.slack.domain.slack.client.SlackClient;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

/**
 * Slack API 구현체
 * Contract 패턴을 적용하여 SlackClient를 사용
 */
@Slf4j
@RequiredArgsConstructor
public class SlackApi implements SlackContract {
    
    private final SlackClient slackClient;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void sendNotification(SlackDto.NotificationRequest request) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        
        switch (request.type()) {
            case ERROR:
                builder.addSection("*❌ 에러*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case SUCCESS:
                builder.addSection("*✅ 성공*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case INFO:
                builder.addSection("*ℹ️ 정보*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case BATCH_JOB:
                // 배치 작업 알림은 별도 메서드에서 처리
                log.warn("BATCH_JOB type should use sendBatchJobNotification() method");
                builder.addSection("*📦 배치 작업*")
                    .addDivider()
                    .addSection(request.message());
                break;
        }
        
        if (request.context() != null && !request.context().isEmpty()) {
            String contextText = request.context().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            builder.addContext(contextText);
        }
        
        SlackDto.SlackMessage message = builder.build();
        slackClient.sendMessage(message);
    }
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*❌ 에러 발생*")
            .addDivider()
            .addSection("메시지: " + message);
        
        if (error != null) {
            builder.addSection("에러 타입: `" + error.getClass().getSimpleName() + "`")
                .addSection("에러 메시지: " + error.getMessage());
        }
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendSuccessNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*✅ 성공*")
            .addDivider()
            .addSection(message);
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendInfoNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*ℹ️ 정보*")
            .addDivider()
            .addSection(message);
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendBatchJobNotification(SlackDto.BatchJobResult result) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        
        String statusEmoji = result.status() == SlackDto.JobStatus.SUCCESS ? "✅" : "❌";
        builder.addSection("*" + statusEmoji + " 배치 작업: " + result.jobName() + "*")
            .addDivider()
            .addSection("상태: " + result.status())
            .addSection("시작 시간: " + (result.startTime() != null ? result.startTime().format(DATE_TIME_FORMATTER) : "N/A"))
            .addSection("종료 시간: " + (result.endTime() != null ? result.endTime().format(DATE_TIME_FORMATTER) : "N/A"))
            .addSection("처리된 항목 수: " + result.processedItems());
        
        if (result.status() == SlackDto.JobStatus.FAILED && result.errorMessage() != null) {
            builder.addDivider()
                .addSection("*에러 정보*")
                .addSection(result.errorMessage());
        }
        
        // 실행 시간 계산
        if (result.startTime() != null && result.endTime() != null) {
            long durationSeconds = java.time.Duration.between(result.startTime(), result.endTime()).getSeconds();
            builder.addContext("실행 시간: " + durationSeconds + "초");
        }
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
}
