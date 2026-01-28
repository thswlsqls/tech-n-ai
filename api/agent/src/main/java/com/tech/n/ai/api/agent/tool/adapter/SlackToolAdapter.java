package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.ToolResult;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Slack API를 LangChain4j Tool 형식으로 래핑하는 어댑터
 * SlackContract를 통해 Slack 메시지 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackToolAdapter {

    private final SlackContract slackContract;

    /**
     * Slack 알림 전송
     *
     * @param message 전송할 메시지
     * @return 전송 결과
     */
    public ToolResult sendNotification(String message) {
        try {
            slackContract.sendInfoNotification(message);
            return ToolResult.success("Slack 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            return ToolResult.failure("Slack 알림 전송 실패: " + e.getMessage());
        }
    }
}
