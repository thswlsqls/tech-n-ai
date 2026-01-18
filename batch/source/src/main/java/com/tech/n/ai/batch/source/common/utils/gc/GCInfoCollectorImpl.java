package com.tech.n.ai.batch.source.common.utils.gc;


import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * GarbageCollectorMXBean을 사용하여 GC 정보를 수집하는 구현체
 * 
 * 이 클래스는 싱글톤 패턴을 사용하여 GarbageCollectorMXBean 인스턴스를 관리합니다.
 * 모든 GC 컬렉터의 정보를 수집하여 통합된 GC 정보를 제공합니다.
 *
 */
public class GCInfoCollectorImpl implements GCInfoCollector {

    /**
     * 싱글톤 인스턴스 (Thread-safe)
     * GarbageCollectorMXBean 은 JVM 당 하나이므로 싱글톤으로 관리
     */
    private static final GCInfoCollectorImpl INSTANCE = new GCInfoCollectorImpl();

    private GCInfoCollectorImpl() {
        // Private constructor for singleton
    }

    public static GCInfoCollectorImpl getInstance() {
        return INSTANCE;
    }

    @Override
    public GCInfo collectGCInfo() {
        try {
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            long totalCollectionCount = 0;
            long totalCollectionTime = 0;
            Map<String, GCInfo.GCCollectorInfo> collectorDetails = new HashMap<>();

            for (GarbageCollectorMXBean gcBean : gcBeans) {
                String name = gcBean.getName();
                long collectionCount = gcBean.getCollectionCount();
                long collectionTime = gcBean.getCollectionTime();

                // collectionCount 가 -1인 경우는 지원되지 않음을 의미
                if (collectionCount < 0) {
                    GCLogger.logUnsupportedMetric(name, "collection count");
                } else {
                    totalCollectionCount += collectionCount;
                }
                
                // collectionTime 이 -1인 경우는 지원되지 않음을 의미
                if (collectionTime < 0) {
                    GCLogger.logUnsupportedMetric(name, "collection time");
                } else {
                    totalCollectionTime += collectionTime;
                }

                // 개별 GC 컬렉터 정보 저장
                GCInfo.GCCollectorInfo collectorInfo = GCInfo.GCCollectorInfo.builder()
                    .name(name)
                    .collectionCount(collectionCount >= 0 ? collectionCount : 0)
                    .collectionTime(collectionTime >= 0 ? collectionTime : 0)
                    .build();

                collectorDetails.put(name, collectorInfo);
            }

            return GCInfo.builder()
                .totalCollectionCount(totalCollectionCount)
                .totalCollectionTime(totalCollectionTime)
                .collectorDetails(collectorDetails)
                .build();

        } catch (Exception e) {
            // 예외 발생 시 로깅 및 기본값 반환
            GCLogger.logCollectionError(e);
            return GCInfo.builder()
                .totalCollectionCount(0L)
                .totalCollectionTime(0L)
                .collectorDetails(new HashMap<>())
                .build();
        }
    }
}

