package com.tech.n.ai.api.agent.scheduler;

import com.tech.n.ai.api.agent.agent.AgentExecutionResult;
import com.tech.n.ai.api.agent.agent.AiUpdateAgent;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * AI Update Agent 스케줄러
 * 6시간마다 자동으로 AI 업데이트 추적 및 포스팅 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class AiUpdateAgentScheduler {

    private final AiUpdateAgent agent;
    private final SlackContract slackContract;

    private static final String DEFAULT_GOAL = """
        OpenAI, Anthropic, Google, Meta의 최신 업데이트를 확인하고 중요한 것만 초안으로 생성해줘.
        이미 포스팅된 것은 제외하고, 생성 후 Slack으로 알려줘.
        """;

    @Scheduled(cron = "${agent.scheduler.cron:0 0 */6 * * *}")
    public void scheduledRun() {
        log.info("Agent 스케줄 실행 시작");

        try {
            AgentExecutionResult result = agent.execute(DEFAULT_GOAL);

            if (result.success()) {
                log.info("Agent 스케줄 실행 완료: postsCreated={}, elapsed={}ms",
                    result.postsCreated(), result.executionTimeMs());
            } else {
                log.warn("Agent 스케줄 실행 실패: errors={}", result.errors());
                notifyFailure(result);
            }
        } catch (Exception e) {
            log.error("Agent 스케줄 실행 중 예외 발생", e);
            slackContract.sendErrorNotification("Agent 스케줄 실행 실패", e);
        }
    }

    private void notifyFailure(AgentExecutionResult result) {
        try {
            slackContract.sendErrorNotification(
                "Agent 스케줄 실행 실패: " + String.join(", ", result.errors()),
                null
            );
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }
}
