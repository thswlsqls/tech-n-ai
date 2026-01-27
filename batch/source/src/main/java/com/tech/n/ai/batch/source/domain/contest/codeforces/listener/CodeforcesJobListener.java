package com.tech.n.ai.batch.source.domain.contest.codeforces.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@RequiredArgsConstructor
public class CodeforcesJobListener implements JobExecutionListener {

    private static final String SOURCE_URL = "https://codeforces.com";
    private static final String SOURCE_CATEGORY = "개발자 대회 정보";
    public static final String SOURCE_ID_KEY = "codeforces.sourceId";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String sourceId = fetchSourceIdFromRedis();
        jobExecution.getExecutionContext().putString(SOURCE_ID_KEY, sourceId);
        log.info("Cached sourceId in JobExecutionContext: {}", sourceId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("Codeforces job completed with status: {}", jobExecution.getStatus());
    }

    private String fetchSourceIdFromRedis() {
        String redisKey = buildRedisKey();
        String sourceId = redisTemplate.opsForValue().get(redisKey);

        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalStateException("Source ID not found in Redis: " + redisKey);
        }

        return sourceId;
    }

    private String buildRedisKey() {
        return SOURCE_URL + ":" + SOURCE_CATEGORY;
    }
}
