package com.tech.n.ai.batch.source.domain.news.arstechnica.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class ArsTechnicaJobListener implements JobExecutionListener {

    private static final String SOURCE_URL = "https://arstechnica.com";
    private static final String SOURCE_CATEGORY = "최신 IT 테크 뉴스 정보";
    public static final String SOURCE_ID_KEY = "arstechnica.sourceId";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String sourceId = fetchSourceIdFromRedis();
        jobExecution.getExecutionContext().putString(SOURCE_ID_KEY, sourceId);
        log.info("Cached sourceId in JobExecutionContext: {}", sourceId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("ArsTechnica job completed with status: {}", jobExecution.getStatus());
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
