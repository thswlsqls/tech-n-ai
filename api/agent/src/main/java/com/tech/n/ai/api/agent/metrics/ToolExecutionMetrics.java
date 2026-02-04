package com.tech.n.ai.api.agent.metrics;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tool 실행 메트릭 추적
 * 매 Agent 실행마다 새 인스턴스를 생성하여 스레드 안전성 보장
 */
public class ToolExecutionMetrics {

    private final AtomicInteger toolCallCount = new AtomicInteger(0);
    private final AtomicInteger analyticsCallCount = new AtomicInteger(0);
    private final AtomicInteger validationErrorCount = new AtomicInteger(0);

    public void incrementToolCall() {
        toolCallCount.incrementAndGet();
    }

    public void incrementAnalyticsCall() {
        analyticsCallCount.incrementAndGet();
    }

    public void incrementValidationError() {
        validationErrorCount.incrementAndGet();
    }

    public int getToolCallCount() {
        return toolCallCount.get();
    }

    public int getAnalyticsCallCount() {
        return analyticsCallCount.get();
    }

    public int getValidationErrorCount() {
        return validationErrorCount.get();
    }
}
