package com.tech.n.ai.batch.source.common.utils.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * MemoryMXBean을 사용하여 메모리 정보를 수집하는 구현체
 * 
 * 이 클래스는 싱글톤 패턴을 사용하여 MemoryMXBean 인스턴스를 관리합니다.
 * 힙 메모리 및 non-heap 메모리 사용량 정보를 수집하여 각각의 Info 객체로 반환합니다.
 *
 */
public class MemoryInfoCollectorImpl implements MemoryInfoCollector {

    private static final MemoryMXBean MEMORY_MX_BEAN = ManagementFactory.getMemoryMXBean();

    /**
     * 싱글톤 인스턴스 (Thread-safe)
     * MemoryMXBean 은 JVM 당 하나이므로 싱글톤으로 관리
     */
    private static final MemoryInfoCollectorImpl INSTANCE = new MemoryInfoCollectorImpl();

    private MemoryInfoCollectorImpl() {
        // Private constructor for singleton
    }

    public static MemoryInfoCollectorImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public HeapMemoryInfo collectHeapMemoryInfo() {
        try {
            MemoryUsage heapUsage = MEMORY_MX_BEAN.getHeapMemoryUsage();

            long used = heapUsage.getUsed();
            long max = heapUsage.getMax();
            long committed = heapUsage.getCommitted();
            long init = heapUsage.getInit();

            // 사용률 계산 (max 가 -1인 경우는 무제한을 의미)
            double usagePercentage = max != -1
                ? (double) used / max * 100.0
                : 0.0;

            return HeapMemoryInfo.builder()
                .used(used)
                .max(max)
                .committed(committed)
                .init(init)
                .usagePercentage(usagePercentage)
                .build();

        } catch (Exception e) {
            // 예외 발생 시 로깅 및 기본값 반환
            MemoryLogger.logCollectionError(e, "heap");
            return HeapMemoryInfo.builder()
                .used(0L)
                .max(0L)
                .committed(0L)
                .init(0L)
                .usagePercentage(0.0)
                .build();
        }
    }

    @Override
    public NonHeapMemoryInfo collectNonHeapMemoryInfo() {
        try {
            MemoryUsage nonHeapUsage = MEMORY_MX_BEAN.getNonHeapMemoryUsage();

            long used = nonHeapUsage.getUsed();
            long max = nonHeapUsage.getMax();
            long committed = nonHeapUsage.getCommitted();
            long init = nonHeapUsage.getInit();

            // 사용률 계산 (max 가 -1인 경우는 무제한을 의미)
            double usagePercentage = max != -1
                ? (double) used / max * 100.0
                : 0.0;

            return NonHeapMemoryInfo.builder()
                .used(used)
                .max(max)
                .committed(committed)
                .init(init)
                .usagePercentage(usagePercentage)
                .build();

        } catch (Exception e) {
            // 예외 발생 시 로깅 및 기본값 반환
            MemoryLogger.logCollectionError(e, "non-heap");
            return NonHeapMemoryInfo.builder()
                .used(0L)
                .max(0L)
                .committed(0L)
                .init(0L)
                .usagePercentage(0.0)
                .build();
        }
    }
}
