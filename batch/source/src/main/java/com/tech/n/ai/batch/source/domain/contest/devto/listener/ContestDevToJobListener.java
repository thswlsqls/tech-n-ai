package com.tech.n.ai.batch.source.domain.contest.devto.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class ContestDevToJobListener implements JobExecutionListener {

    private static final String SOURCE_URL = "https://dev.to";
    private static final String SOURCE_CATEGORY = "개발자 대회 정보";
    public static final String SOURCE_ID_KEY = "contest.devto.sourceId";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String sourceId = fetchSourceIdFromRedis();
        jobExecution.getExecutionContext().putString(SOURCE_ID_KEY, sourceId);
        log.info("Cached sourceId in JobExecutionContext: {}", sourceId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("DevTo(Contest) job completed with status: {}", jobExecution.getStatus());
    }

    private String fetchSourceIdFromRedis() {
        String redisKey = SOURCE_URL + ":" + SOURCE_CATEGORY;
        String sourceId = redisTemplate.opsForValue().get(redisKey);

        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalStateException("Source ID not found in Redis: " + redisKey);
        }

        return sourceId;
    }
}
