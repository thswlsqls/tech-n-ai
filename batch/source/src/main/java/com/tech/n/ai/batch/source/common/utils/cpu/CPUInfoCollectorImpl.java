package com.tech.n.ai.batch.source.common.utils.cpu;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;


/**
 * CPU 정보 수집 구현체 (싱글톤)
 *
 * 참고 문서:
 * - Oracle Java 25: https://docs.oracle.com/en/java/javase/25/docs/api/jdk.management/com/sun/management/OperatingSystemMXBean.html
 *   - getProcessCpuLoad(), getCpuLoad()의 관찰 기간은 독립적이며 구현체에 따라 다를 수 있음
 *
 */
public class CPUInfoCollectorImpl implements CPUInfoCollector {

    private static final ThreadMXBean THREAD_MX_BEAN = ManagementFactory.getThreadMXBean();
    private static final OperatingSystemMXBean OS_MX_BEAN = 
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private static final CPUInfoCollectorImpl INSTANCE = new CPUInfoCollectorImpl();
    private static volatile long lastCallTime = 0;
    private static volatile long lastProcessCpuLoadCallTime = 0;
    private static volatile long lastSystemCpuLoadCallTime = 0;
    private static final Object LOCK = new Object();
    private static final long MIN_OBSERVATION_PERIOD_MS = 1000L;

    private CPUInfoCollectorImpl() {
    }

    public static CPUInfoCollectorImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public CPUInfo collectCPUInfo() {
        try {
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = calculateTimeSinceLastCall(currentTime);
            long currentThreadCpuTime = collectCurrentThreadCpuTime();
            long processCpuTime = OS_MX_BEAN.getProcessCpuTime();
            double processCpuLoad = collectProcessCpuLoad(currentTime);
            double systemCpuLoad = collectSystemCpuLoad(currentTime);
            long estimatedProcessCpuLoadObservationPeriod = calculateEstimatedObservationPeriod(
                lastProcessCpuLoadCallTime, currentTime);
            long estimatedSystemCpuLoadObservationPeriod = calculateEstimatedObservationPeriod(
                lastSystemCpuLoadCallTime, currentTime);

            return CPUInfo.builder()
                .currentThreadCpuTime(currentThreadCpuTime)
                .processCpuTime(processCpuTime)
                .processCpuLoad(processCpuLoad)
                .systemCpuLoad(systemCpuLoad)
                .timeSinceLastCall(timeSinceLastCall)
                .estimatedProcessCpuLoadObservationPeriod(estimatedProcessCpuLoadObservationPeriod)
                .estimatedSystemCpuLoadObservationPeriod(estimatedSystemCpuLoadObservationPeriod)
                .build();

        } catch (Exception e) {
            CPULogger.logCollectionError(e);
            return createDefaultCPUInfo();
        }
    }

    private long calculateTimeSinceLastCall(long currentTime) {
        synchronized (LOCK) {
            if (lastCallTime > 0) {
                long timeSinceLastCall = currentTime - lastCallTime;
                lastCallTime = currentTime;
                return timeSinceLastCall;
            } else {
                lastCallTime = currentTime;
                return -1L;
            }
        }
    }

    private long collectCurrentThreadCpuTime() {
        if (!THREAD_MX_BEAN.isThreadCpuTimeSupported()) {
            return 0L;
        }
        if (!THREAD_MX_BEAN.isThreadCpuTimeEnabled()) {
            THREAD_MX_BEAN.setThreadCpuTimeEnabled(true);
        }
        
        return THREAD_MX_BEAN.getCurrentThreadCpuTime();
    }

    private double collectProcessCpuLoad(long currentTime) {
        double processCpuLoad = OS_MX_BEAN.getProcessCpuLoad();
        synchronized (LOCK) {
            lastProcessCpuLoadCallTime = currentTime;
        }
        
        return processCpuLoad;
    }

    private double collectSystemCpuLoad(long currentTime) {
        double systemCpuLoad = OS_MX_BEAN.getCpuLoad();
        synchronized (LOCK) {
            lastSystemCpuLoadCallTime = currentTime;
        }
        
        return systemCpuLoad;
    }

    private CPUInfo createDefaultCPUInfo() {
        return CPUInfo.builder()
            .currentThreadCpuTime(0L)
            .processCpuTime(0L)
            .processCpuLoad(-1.0)
            .systemCpuLoad(-1.0)
            .timeSinceLastCall(-1L)
            .estimatedProcessCpuLoadObservationPeriod(-1L)
            .estimatedSystemCpuLoadObservationPeriod(-1L)
            .build();
    }

    public long getEstimatedProcessCpuLoadObservationPeriod() {
        synchronized (LOCK) {
            if (lastProcessCpuLoadCallTime > 0) {
                return System.currentTimeMillis() - lastProcessCpuLoadCallTime;
            }
            return -1L;
        }
    }

    public long getEstimatedSystemCpuLoadObservationPeriod() {
        synchronized (LOCK) {
            if (lastSystemCpuLoadCallTime > 0) {
                return System.currentTimeMillis() - lastSystemCpuLoadCallTime;
            }
            return -1L;
        }
    }

    private long calculateEstimatedObservationPeriod(long lastCallTime, long currentTime) {
        if (lastCallTime > 0) {
            return currentTime - lastCallTime;
        }
        return -1L;
    }

    public static long getMinObservationPeriodMs() {
        return MIN_OBSERVATION_PERIOD_MS;
    }
}

