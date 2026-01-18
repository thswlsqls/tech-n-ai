package com.tech.n.ai.batch.source.common.utils.cpu;


import lombok.Builder;
import lombok.Value;


/**
 * JVM CPU 사용량 정보 불변 객체
 * 
 * 참고 문서:
 * - ThreadMXBean: https://docs.oracle.com/javase/8/docs/api/java/lang/management/ThreadMXBean.html
 * - OperatingSystemMXBean (Java 25): https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html
 *   - getProcessCpuLoad(), getCpuLoad()의 관찰 기간은 독립적이며 구현체에 따라 다를 수 있음
 *   - 첫 호출 시 관찰 기간이 정의되지 않을 수 있음
 * 
 * @see java.lang.management.ThreadMXBean
 * @see com.sun.management.OperatingSystemMXBean
 */
@Value
@Builder
public class CPUInfo {
    long currentThreadCpuTime;
    long processCpuTime;
    double processCpuLoad;
    double systemCpuLoad;
    long timeSinceLastCall;
    long estimatedProcessCpuLoadObservationPeriod;
    long estimatedSystemCpuLoadObservationPeriod;

    public double getCurrentThreadCpuTimeInMillis() {
        return currentThreadCpuTime / 1_000_000.0;
    }

    public double getProcessCpuTimeInMillis() {
        return processCpuTime / 1_000_000.0;
    }

    public double getProcessCpuLoadPercentage() {
        return processCpuLoad >= 0.0 ? processCpuLoad * 100.0 : -1.0;
    }

    public double getSystemCpuLoadPercentage() {
        return systemCpuLoad >= 0.0 ? systemCpuLoad * 100.0 : -1.0;
    }

    public boolean isShortObservationPeriod() {
        return timeSinceLastCall > 0 && timeSinceLastCall < 1000;
    }

    public boolean isAbnormalMeasurement() {
        if (processCpuLoad < 0.0 || systemCpuLoad < 0.0) {
            return false;
        }
        return processCpuLoad > systemCpuLoad;
    }

    public AbnormalMeasurementReason analyzeAbnormalMeasurement() {
        if (!isAbnormalMeasurement()) {
            return AbnormalMeasurementReason.NORMAL;
        }

        boolean hasShortObservationPeriod = estimatedProcessCpuLoadObservationPeriod > 0 
            && estimatedProcessCpuLoadObservationPeriod < 1000;
        boolean hasVeryShortSystemObservationPeriod = estimatedSystemCpuLoadObservationPeriod > 0 
            && estimatedSystemCpuLoadObservationPeriod < 1000;

        if (hasShortObservationPeriod || hasVeryShortSystemObservationPeriod) {
            return AbnormalMeasurementReason.SHORT_OBSERVATION_PERIOD;
        }

        if (estimatedProcessCpuLoadObservationPeriod > 0 && estimatedSystemCpuLoadObservationPeriod > 0) {
            long periodDifference = Math.abs(estimatedProcessCpuLoadObservationPeriod - estimatedSystemCpuLoadObservationPeriod);
            if (periodDifference > 1000) {
                return AbnormalMeasurementReason.SIGNIFICANT_OBSERVATION_PERIOD_DIFFERENCE;
            }
        }

        if (estimatedProcessCpuLoadObservationPeriod < 0 || estimatedSystemCpuLoadObservationPeriod < 0) {
            return AbnormalMeasurementReason.UNKNOWN_OBSERVATION_PERIOD;
        }

        return AbnormalMeasurementReason.OTHER;
    }

    public enum AbnormalMeasurementReason {
        NORMAL,
        SHORT_OBSERVATION_PERIOD,
        SIGNIFICANT_OBSERVATION_PERIOD_DIFFERENCE,
        UNKNOWN_OBSERVATION_PERIOD,
        OTHER
    }

    public ObservationPeriodStatus getObservationPeriodStatus() {
        if (timeSinceLastCall < 0) {
            return ObservationPeriodStatus.UNKNOWN;
        }
        if (timeSinceLastCall == 0) {
            return ObservationPeriodStatus.FIRST_CALL;
        }
        if (timeSinceLastCall < 1000) {
            return ObservationPeriodStatus.SHORT;
        }
        return ObservationPeriodStatus.NORMAL;
    }

    public boolean isSuspiciousZeroLoad() {
        if (processCpuLoad != 0.0) {
            return false;
        }
        if (timeSinceLastCall < 0) {
            return true;
        }
        if (timeSinceLastCall > 0 && timeSinceLastCall < 1000) {
            return true;
        }
        return false;
    }

    public SuspiciousZeroLoadReason analyzeSuspiciousZeroLoad() {
        if (!isSuspiciousZeroLoad()) {
            return SuspiciousZeroLoadReason.NORMAL;
        }
        if (timeSinceLastCall < 0) {
            return SuspiciousZeroLoadReason.FIRST_CALL_UNDEFINED_OBSERVATION_PERIOD;
        }
        if (timeSinceLastCall > 0 && timeSinceLastCall < 1000) {
            return SuspiciousZeroLoadReason.SHORT_OBSERVATION_PERIOD;
        }
        return SuspiciousZeroLoadReason.OTHER;
    }

    public enum SuspiciousZeroLoadReason {
        NORMAL,
        FIRST_CALL_UNDEFINED_OBSERVATION_PERIOD,
        SHORT_OBSERVATION_PERIOD,
        OTHER
    }

    public enum ObservationPeriodStatus {
        UNKNOWN,
        FIRST_CALL,
        SHORT,
        NORMAL
    }
}

