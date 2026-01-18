package com.tech.n.ai.batch.source.common.utils.gc;


/**
 * GC 정보를 수집하는 전략 인터페이스
 * Strategy Pattern 을 활용하여 다양한 GC 정보 수집 방식 지원
 */
public interface GCInfoCollector {
    /**
     * 현재 GC 정보를 수집합니다.
     *
     * @return GC 정보 객체
     */
    GCInfo collectGCInfo();
}

