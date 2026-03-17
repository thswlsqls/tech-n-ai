package com.tech.n.ai.api.agent.metrics;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tool 실행 메트릭 추적
 * 매 Agent 실행마다 새 인스턴스를 생성하여 스레드 안전성 보장
 */
public class ToolExecutionMetrics {

    private final AtomicInteger toolCallCount = new AtomicInteger(0);
    private final AtomicInteger analyticsCallCount = new AtomicInteger(0);
    private final AtomicInteger validationErrorCount = new AtomicInteger(0);

    /** collect_github_releases로 이미 수집 완료된 저장소 (owner/repo 형식) */
    private final Set<String> collectedGitHubRepos = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /** fetch_github_releases 차단 횟수 (루프 감지용) */
    private final AtomicInteger fetchBlockedCount = new AtomicInteger(0);

    public void incrementToolCall() {
        toolCallCount.incrementAndGet();
    }

    public void incrementAnalyticsCall() {
        analyticsCallCount.incrementAndGet();
    }

    public void incrementValidationError() {
        validationErrorCount.incrementAndGet();
    }

    /**
     * GitHub 저장소를 수집 완료로 기록
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     */
    public void markGitHubRepoCollected(String owner, String repo) {
        collectedGitHubRepos.add(owner.toLowerCase() + "/" + repo.toLowerCase());
    }

    /**
     * 해당 GitHub 저장소가 이미 수집 완료되었는지 확인
     * @param owner 저장소 소유자
     * @param repo 저장소 이름
     * @return 이미 수집된 경우 true
     */
    public boolean isGitHubRepoCollected(String owner, String repo) {
        return collectedGitHubRepos.contains(owner.toLowerCase() + "/" + repo.toLowerCase());
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

    /**
     * fetch 차단 횟수를 증가시키고 현재 값을 반환
     * @return 증가 후의 차단 횟수
     */
    public int incrementAndGetFetchBlockedCount() {
        return fetchBlockedCount.incrementAndGet();
    }

    public int getFetchBlockedCount() {
        return fetchBlockedCount.get();
    }
}
