package com.tech.n.ai.batch.source.common.utils.gc;


import java.util.Map;
import lombok.Builder;
import lombok.Value;


/**
 * JVM GC 정보 불변 객체
 * 
 * 참고 문서:
 * - GarbageCollectorMXBean: https://docs.oracle.com/javase/8/docs/api/java/lang/management/GarbageCollectorMXBean.html
 * 
 * @see java.lang.management.GarbageCollectorMXBean
 */
@Value
@Builder
public class GCInfo {
    long totalCollectionCount;
    long totalCollectionTime;
    Map<String, GCCollectorInfo> collectorDetails;

    @Value
    @Builder
    public static class GCCollectorInfo {
        String name;
        long collectionCount;
        long collectionTime;
    }

    public double getTotalCollectionTimeInSeconds() {
        return totalCollectionTime / 1000.0;
    }

    public double getAverageCollectionTime() {
        return totalCollectionCount > 0 
            ? (double) totalCollectionTime / totalCollectionCount 
            : 0.0;
    }
}

