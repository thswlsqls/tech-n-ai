package com.tech.n.ai.batch.source.common.utils.memory;

import lombok.Builder;
import lombok.Value;

/**
 * JVM 힙 메모리 사용량 정보 불변 객체
 * 
 * 참고 문서:
 * - MemoryMXBean: https://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryMXBean.html
 * - MemoryUsage: https://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryUsage.html
 * 
 * @see java.lang.management.MemoryMXBean
 * @see java.lang.management.MemoryUsage
 */
@Value
@Builder
public class HeapMemoryInfo {
    long used;
    long max;
    long committed;
    long init;
    double usagePercentage;

    public double getUsedInMB() {
        return used / (1024.0 * 1024.0);
    }

    /**
     * 메가바이트 단위로 변환된 최대 메모리
     */
    public double getMaxInMB() {
        return max != -1 ? max / (1024.0 * 1024.0) : 0.0;
    }

    public double getCommittedInMB() {
        return committed / (1024.0 * 1024.0);
    }
}
