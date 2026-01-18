package com.tech.n.ai.batch.source.common.utils.cpu;


import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;


/**
 * JVM CPU 사용량 로깅 유틸리티
 * 
 * 참고 문서:
 * - Oracle Java 25: https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html
 *   - getProcessCpuLoad(), getCpuLoad() 메서드의 관찰 기간은 독립적이며 구현체에 따라 다를 수 있음
 *
 */
@Slf4j
public final class CPULogger {

    private CPULogger() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static final String PHASE_BEFORE_STEP = "BEFORE_STEP";
    public static final String PHASE_AFTER_STEP = "AFTER_STEP";
    public static final double DEFAULT_CPU_WARNING_THRESHOLD_PERCENT = 80.0;

    public static void logBeforeStep(String context, CPUInfo startCPUInfo) {
        logCPUInfo(context, PHASE_BEFORE_STEP, startCPUInfo);
    }

    public static void logAfterStep(
            String context,
            CPUInfo endCPUInfo,
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            CPUInfo startCPUInfo) {

        logCPUInfo(context, PHASE_AFTER_STEP, endCPUInfo);
    }

    public static void logCPUInfo(
        String context
        , String phase
        , CPUInfo cpuInfo) {
        String threadName = Thread.currentThread().getName();
        boolean isAbnormal = cpuInfo.isAbnormalMeasurement();
        boolean isSuspiciousZeroLoad = cpuInfo.isSuspiciousZeroLoad();
        
        if (!isAbnormal && !isSuspiciousZeroLoad) {
            String processCpuLoadStr = formatPercentage(cpuInfo.getProcessCpuLoadPercentage());
            String systemCpuLoadStr = formatPercentage(cpuInfo.getSystemCpuLoadPercentage());
            
            log.info(
                "[{}] [CPU] [{}] [{}] Current Thread CPU Time: {} ns ({} ms), Process CPU Time: {} ns ({} ms), Process CPU Load: {} %, System CPU Load: {} %",
                threadName,
                context,
                phase,
                cpuInfo.getCurrentThreadCpuTime(),
                String.format("%.2f", cpuInfo.getCurrentThreadCpuTimeInMillis()),
                cpuInfo.getProcessCpuTime(),
                String.format("%.2f", cpuInfo.getProcessCpuTimeInMillis()),
                processCpuLoadStr,
                systemCpuLoadStr
            );
        }
        
        logAbnormalMeasurementIfNeeded(context, phase, cpuInfo);
        logSuspiciousZeroLoadIfNeeded(context, phase, cpuInfo);
    }

    private static String formatPercentage(double percentage) {
        return percentage >= 0.0 
            ? String.format("%.2f", percentage) 
            : "N/A";
    }

    private static void logAbnormalMeasurementIfNeeded(String context, String phase, CPUInfo cpuInfo) {
        if (!cpuInfo.isAbnormalMeasurement()) {
            return;
        }
        
        String threadName = Thread.currentThread().getName();
        double processCpuLoadPercentage = cpuInfo.getProcessCpuLoadPercentage();
        double systemCpuLoadPercentage = cpuInfo.getSystemCpuLoadPercentage();
        double difference = processCpuLoadPercentage - systemCpuLoadPercentage;
        CPUInfo.AbnormalMeasurementReason reason = cpuInfo.analyzeAbnormalMeasurement();
        String processCpuLoadStr = formatPercentage(processCpuLoadPercentage);
        String systemCpuLoadStr = formatPercentage(systemCpuLoadPercentage);
        
        log.warn(
            "[{}] [CPU] [{}] [{}] WARNING: Abnormal CPU measurement detected! " +
            "Current Thread CPU Time: {} ns ({} ms), Process CPU Time: {} ns ({} ms), " +
            "Process CPU Load: {} % > System CPU Load: {} %. Difference: {} %. " +
            "Reason: {}. " +
            "Observation periods - Overall: {} ms, Process CPU Load: {} ms, System CPU Load: {} ms. " +
            "This may occur due to: (1) Independent observation periods of getProcessCpuLoad() and getCpuLoad(), " +
            "(2) Different measurement timing, (3) Multi-core system characteristics, (4) Interference from other callers.",
            threadName,
            context,
            phase,
            cpuInfo.getCurrentThreadCpuTime(),
            String.format("%.2f", cpuInfo.getCurrentThreadCpuTimeInMillis()),
            cpuInfo.getProcessCpuTime(),
            String.format("%.2f", cpuInfo.getProcessCpuTimeInMillis()),
            processCpuLoadStr,
            systemCpuLoadStr,
            String.format("%.2f", difference),
            reason,
            cpuInfo.getTimeSinceLastCall() >= 0 ? cpuInfo.getTimeSinceLastCall() : "Unknown",
            cpuInfo.getEstimatedProcessCpuLoadObservationPeriod() >= 0 
                ? cpuInfo.getEstimatedProcessCpuLoadObservationPeriod() : "Unknown",
            cpuInfo.getEstimatedSystemCpuLoadObservationPeriod() >= 0 
                ? cpuInfo.getEstimatedSystemCpuLoadObservationPeriod() : "Unknown"
        );
    }

    private static void logSuspiciousZeroLoadIfNeeded(String context, String phase, CPUInfo cpuInfo) {
        if (!cpuInfo.isSuspiciousZeroLoad()) {
            return;
        }
        
        String threadName = Thread.currentThread().getName();
        CPUInfo.SuspiciousZeroLoadReason reason = cpuInfo.analyzeSuspiciousZeroLoad();
        String overallObservationPeriod = cpuInfo.getTimeSinceLastCall() >= 0 
            ? String.valueOf(cpuInfo.getTimeSinceLastCall()) 
            : "Unknown (first call)";
        String processObservationPeriod = cpuInfo.getEstimatedProcessCpuLoadObservationPeriod() >= 0 
            ? String.valueOf(cpuInfo.getEstimatedProcessCpuLoadObservationPeriod()) 
            : "Unknown";
        String systemObservationPeriod = cpuInfo.getEstimatedSystemCpuLoadObservationPeriod() >= 0 
            ? String.valueOf(cpuInfo.getEstimatedSystemCpuLoadObservationPeriod()) 
            : "Unknown";
        
        log.warn(
            "[{}] [CPU] [{}] [{}] WARNING: Suspicious zero CPU Load measurement detected! " +
            "Process CPU Load: 0.00 %, System CPU Load: 0.00 %. " +
            "Reason: {}. " +
            "Observation periods - Overall: {} ms, Process CPU Load: {} ms, System CPU Load: {} ms. " +
            "This may occur due to: (1) First call with undefined observation period, " +
            "(2) Very short observation period (< 1000ms). " +
            "The measurement may be inaccurate. Please verify actual CPU usage.",
            threadName,
            context,
            phase,
            reason,
            overallObservationPeriod,
            processObservationPeriod,
            systemObservationPeriod
        );
    }

    public static void logWithThreshold(
        String context,
        CPUInfo CPUInfo,
        String phase,
        double thresholdPercent) {
        String threadName = Thread.currentThread().getName();
        double processCpuLoadPercentage = CPUInfo.getProcessCpuLoadPercentage();
        
        if (processCpuLoadPercentage < 0.0) {
            log.debug(
                "[{}] [CPU] [{}] [{}] Process CPU Load measurement not available (first call or unsupported)",
                threadName,
                context,
                phase
            );
        } else {
            if (processCpuLoadPercentage > thresholdPercent) {
                if (CPUInfo.isShortObservationPeriod()) {
                    log.warn(
                        "[{}] [CPU] [{}] [{}] Process CPU Load: {} % (Threshold: {} %) - WARNING: Measured with short observation period ({} ms). This may be a false positive. Please verify actual CPU usage.",
                        threadName,
                        context,
                        phase,
                        String.format("%.2f", processCpuLoadPercentage),
                        thresholdPercent,
                        CPUInfo.getTimeSinceLastCall()
                    );
                } else {
                    log.warn(
                        "[{}] [CPU] [{}] [{}] Process CPU Load Exceeded Threshold! Process CPU Load: {} % (Threshold: {} %)",
                        threadName,
                        context,
                        phase,
                        String.format("%.2f", processCpuLoadPercentage),
                        thresholdPercent
                    );
                }
            }
        }
        
        double systemCpuLoadPercentage = CPUInfo.getSystemCpuLoadPercentage();
        
        if (systemCpuLoadPercentage < 0.0) {
            log.debug(
                "[{}] [CPU] [{}] [{}] System CPU Load measurement not available (first call or unsupported)",
                threadName,
                context,
                phase
            );
        } else {
            if (systemCpuLoadPercentage > thresholdPercent) {
                if (CPUInfo.isShortObservationPeriod()) {
                    log.warn(
                        "[{}] [CPU] [{}] [{}] System CPU Load: {} % (Threshold: {} %) - WARNING: Measured with short observation period ({} ms). This may be a false positive. Please verify actual CPU usage.",
                        threadName,
                        context,
                        phase,
                        String.format("%.2f", systemCpuLoadPercentage),
                        thresholdPercent,
                        CPUInfo.getTimeSinceLastCall()
                    );
                } else {
                    log.warn(
                        "[{}] [CPU] [{}] [{}] System CPU Load Exceeded Threshold! System CPU Load: {} % (Threshold: {} %) - Please verify actual CPU usage if this seems incorrect.",
                        threadName,
                        context,
                        phase,
                        String.format("%.2f", systemCpuLoadPercentage),
                        thresholdPercent
                    );
                }
            }
        }
    }

    public static void logCollectionError(Exception exception) {
        log.error(
            "[{}] [CPU] Failed to collect CPU information",
            Thread.currentThread().getName(),
            exception
        );
    }

}
