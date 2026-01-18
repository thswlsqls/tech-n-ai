package com.tech.n.ai.batch.source.common.utils.gc;


import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;


/**
 * JVM GC 실행 횟수 및 작업 처리 시간 로깅 유틸리티
 * 
 * 참고 문서:
 * - G1 GC 튜닝 가이드: Recommendations 섹션의 Pause Time Goals 항목
 *
 */
@Slf4j
public final class GCLogger {

    private GCLogger() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static final String PHASE_BEFORE_STEP = "BEFORE_STEP";
    public static final String PHASE_AFTER_STEP = "AFTER_STEP";
    public static final double DEFAULT_GC_TIME_RATIO_WARNING_THRESHOLD_PERCENT = 15.0;
    private static final double VERY_HIGH_GC_TIME_RATIO_THRESHOLD_PERCENT = 50.0;

    public static void logBeforeStep(String context, GCInfo startGCInfo) {
        logGCInfo(context, PHASE_BEFORE_STEP, startGCInfo);
    }

    public static void logAfterStep(
            String context,
            GCInfo endGCInfo,
            LocalDateTime startTime,
            LocalDateTime endTime, 
            GCInfo startGCInfo) {
        
        logGCInfo(context, PHASE_AFTER_STEP, endGCInfo);
    }

    public static void logGCInfo(
        String context
        , String phase
        , GCInfo gcInfo) {
        String threadName = Thread.currentThread().getName();
        log.info(
            "[{}] [GC] [{}] [{}] Total GC Count: {}, Total GC Time: {} ms ({} s), Avg GC Time: {} ms",
            threadName,
            context,
            phase,
            gcInfo.getTotalCollectionCount(),
            gcInfo.getTotalCollectionTime(),
            String.format("%.2f", gcInfo.getTotalCollectionTimeInSeconds()),
            String.format("%.2f", gcInfo.getAverageCollectionTime())
        );
    }
    
    public static void logUnsupportedMetric(String collectorName, String metricType) {
        log.debug(
            "[{}] [GC] GC {} not supported for collector: {}",
            Thread.currentThread().getName(),
            metricType,
            collectorName
        );
    }
    
    public static void logCollectionError(Exception exception) {
        log.error(
            "[{}] [GC] Failed to collect GC information",
            Thread.currentThread().getName(),
            exception
        );
    }

    public static void logWithThreshold(
        String context,
        String phase,
        LocalDateTime startTime,
        LocalDateTime endTime,
        GCInfo startGCInfo,
        GCInfo endGCInfo,
        double thresholdPercent) {
        
        ExecutionTimeInfo executionTimeInfo = createExecutionTimeInfo(startTime, endTime);
        long gcTimeDelta = endGCInfo.getTotalCollectionTime() - startGCInfo.getTotalCollectionTime();
        
        if (executionTimeInfo.getDurationInMillis() > 0) {
            double gcTimeRatio = (double) gcTimeDelta / executionTimeInfo.getDurationInMillis() * 100.0;
            
            if (gcTimeRatio > thresholdPercent) {
                if (gcTimeRatio >= VERY_HIGH_GC_TIME_RATIO_THRESHOLD_PERCENT) {
                    log.warn(
                        "[{}] [GC] [{}] [{}] GC Time Ratio Exceeded Threshold! GC Time Ratio: {} % (Threshold: {} %) - GC Time: {} ms, Execution Time: {} ms - WARNING: Very high GC time ratio detected. This may indicate memory pressure or GC tuning issues. Please review GC logs and memory usage.",
                        Thread.currentThread().getName(),
                        context,
                        phase,
                        String.format("%.2f", gcTimeRatio),
                        thresholdPercent,
                        gcTimeDelta,
                        executionTimeInfo.getDurationInMillis()
                    );
                } else {
                    log.warn(
                        "[{}] [GC] [{}] [{}] GC Time Ratio Exceeded Threshold! GC Time Ratio: {} % (Threshold: {} %) - GC Time: {} ms, Execution Time: {} ms",
                        Thread.currentThread().getName(),
                        context,
                        phase,
                        String.format("%.2f", gcTimeRatio),
                        thresholdPercent,
                        gcTimeDelta,
                        executionTimeInfo.getDurationInMillis()
                    );
                }
            }
        }
    }

    private static ExecutionTimeInfo createExecutionTimeInfo(
            LocalDateTime startTime, 
            LocalDateTime endTime) {
        
        if (startTime == null || endTime == null) {
            return ExecutionTimeInfo.builder()
                .startTime(startTime)
                .endTime(endTime)
                .duration(null)
                .build();
        }

        Duration duration = Duration.between(startTime, endTime);
        
        return ExecutionTimeInfo.builder()
            .startTime(startTime)
            .endTime(endTime)
            .duration(duration)
            .build();
    }
}

