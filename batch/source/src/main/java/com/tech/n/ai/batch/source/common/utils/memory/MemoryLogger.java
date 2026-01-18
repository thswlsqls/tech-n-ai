package com.tech.n.ai.batch.source.common.utils.memory;


import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;


/**
 * JVM 힙 및 non-heap 메모리 사용량 로깅 유틸리티
 *
 */
@Slf4j
public final class MemoryLogger {
    private MemoryLogger() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static final String PHASE_BEFORE_STEP = "BEFORE_STEP";
    public static final String PHASE_AFTER_STEP = "AFTER_STEP";
    public static final double DEFAULT_MEMORY_WARNING_THRESHOLD_PERCENT = 80.0;
    private static final double VERY_HIGH_MEMORY_USAGE_THRESHOLD_PERCENT = 95.0;
    private static final double BYTES_TO_MB_DIVISOR = 1024.0 * 1024.0;

    public static void logHeapMemory(
        String context
        , String phase
        , HeapMemoryInfo memoryInfo) {
        String threadName = Thread.currentThread().getName();
        log.info(
            "[{}] [MEMORY] [{}] [{}] Heap Memory - Used: {} MB / Max: {} MB / Committed: {} MB / Usage: {} %",
            threadName,
            context,
            phase,
            String.format("%.2f", memoryInfo.getUsedInMB()),
            String.format("%.2f", memoryInfo.getMaxInMB()),
            String.format("%.2f", memoryInfo.getCommittedInMB()),
            String.format("%.2f", memoryInfo.getUsagePercentage())
        );
    }

    public static void logBeforeStep(String stepName, HeapMemoryInfo memoryInfo) {
        logHeapMemory(stepName, PHASE_BEFORE_STEP, memoryInfo);
    }

    public static void logAfterStep(String stepName, HeapMemoryInfo memoryInfo) {
        logHeapMemory(stepName, PHASE_AFTER_STEP, memoryInfo);
    }

    public static void logAfterStep(
            String context,
            LocalDateTime startTime,
            LocalDateTime endTime,
            HeapMemoryInfo startMemoryInfo,
            HeapMemoryInfo endMemoryInfo) {
        logHeapMemory(context, PHASE_AFTER_STEP, endMemoryInfo);
    }

    public static void logWithThreshold(
        String context
        , HeapMemoryInfo memoryInfo
        , String phase
        , double thresholdPercent) {

        if (memoryInfo.getUsagePercentage() > thresholdPercent) {
            logMemoryThresholdWarning(context, phase, memoryInfo, thresholdPercent, "Heap");
        }
    }

    public static void logNonHeapMemory(
        String context
        , String phase
        , NonHeapMemoryInfo memoryInfo) {
        String threadName = Thread.currentThread().getName();
        log.info(
            "[{}] [MEMORY] [{}] [{}] Non-Heap Memory - Used: {} MB / Max: {} MB / Committed: {} MB / Usage: {} %",
            threadName,
            context,
            phase,
            String.format("%.2f", memoryInfo.getUsedInMB()),
            String.format("%.2f", memoryInfo.getMaxInMB()),
            String.format("%.2f", memoryInfo.getCommittedInMB()),
            String.format("%.2f", memoryInfo.getUsagePercentage())
        );
    }

    public static void logNonHeapBeforeStep(String context, NonHeapMemoryInfo memoryInfo) {
        logNonHeapMemory(context, PHASE_BEFORE_STEP, memoryInfo);
    }

    public static void logNonHeapAfterStep(String context, NonHeapMemoryInfo memoryInfo) {
        logNonHeapMemory(context, PHASE_AFTER_STEP, memoryInfo);
    }

    public static void logNonHeapWithThreshold(
        String context,
        NonHeapMemoryInfo memoryInfo,
        String phase,
        double thresholdPercent) {

        if (memoryInfo.getUsagePercentage() > thresholdPercent) {
            logMemoryThresholdWarning(context, phase, memoryInfo, thresholdPercent, "Non-Heap");
        }
    }

    private static void logMemoryThresholdWarning(
            String context,
            String phase,
            Object memoryInfo,
            double thresholdPercent,
            String memoryType) {
        
        double usagePercentage = getUsagePercentage(memoryInfo);
        String threadName = Thread.currentThread().getName();
        
        if (usagePercentage >= VERY_HIGH_MEMORY_USAGE_THRESHOLD_PERCENT) {
            String warningMessage = "Heap".equals(memoryType)
                ? "Very high memory usage detected. This may lead to OutOfMemoryError. Please review memory allocation and consider increasing heap size or optimizing memory usage."
                : "Very high non-heap memory usage detected. This may indicate issues with method area, code cache, or other JVM internal structures.";
            
            log.warn(
                "[{}] [MEMORY] [{}] [{}] {} Memory Usage Exceeded Threshold! Usage: {} % (Threshold: {} %) - WARNING: {}",
                threadName,
                context,
                phase,
                memoryType,
                String.format("%.2f", usagePercentage),
                thresholdPercent,
                warningMessage
            );
        } else {
            log.warn(
                "[{}] [MEMORY] [{}] [{}] {} Memory Usage Exceeded Threshold! Usage: {} % (Threshold: {} %)",
                threadName,
                context,
                phase,
                memoryType,
                String.format("%.2f", usagePercentage),
                thresholdPercent
            );
        }
    }

    private static double getUsagePercentage(Object memoryInfo) {
        if (memoryInfo instanceof HeapMemoryInfo) {
            return ((HeapMemoryInfo) memoryInfo).getUsagePercentage();
        } else if (memoryInfo instanceof NonHeapMemoryInfo) {
            return ((NonHeapMemoryInfo) memoryInfo).getUsagePercentage();
        }
        return 0.0;
    }

    private static double convertBytesToMB(long bytes) {
        return bytes / BYTES_TO_MB_DIVISOR;
    }

    public static void logCollectionError(Exception exception, String memoryType) {
        log.error(
            "[{}] [MEMORY] Failed to collect {} memory information",
            Thread.currentThread().getName(),
            memoryType,
            exception
        );
    }
}
