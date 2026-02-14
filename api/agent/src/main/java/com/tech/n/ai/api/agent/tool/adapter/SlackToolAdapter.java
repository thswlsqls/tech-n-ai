package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.ToolResult;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Slack API를 LangChain4j Tool 형식으로 래핑하는 어댑터
 * 비활성화 시 Mock 응답을 반환
 */
@Slf4j
@Component
public class SlackToolAdapter {

    private final SlackContract slackContract;

    @Value("${slack.emerging-tech.channel:#emerging-tech}")
    private String defaultChannel;

    @Value("${agent.slack.enabled:false}")
    private boolean slackEnabled;

    public SlackToolAdapter(SlackContract slackContract) {
        this.slackContract = slackContract;
    }

    /**
     * Slack 알림 전송 (비활성화 시 Mock 응답 반환)
     */
    public ToolResult sendNotification(String message) {
        if (!slackEnabled) {
            log.info("Slack 비활성화 상태 - Mock 응답 반환: channel={}, message={}",
                    defaultChannel, message);
            return ToolResult.success(
                "[Slack 비활성화] 다음 메시지가 발송될 예정입니다",
                Map.of(
                    "channel", defaultChannel,
                    "message", message,
                    "status", "MOCK_SENT"
                )
            );
        }

        try {
            slackContract.sendInfoNotification(message);
            return ToolResult.success("Slack 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            return ToolResult.failure("Slack 알림 전송 실패: " + e.getMessage());
        }
    }
}
