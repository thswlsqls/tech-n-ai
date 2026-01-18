package com.tech.n.ai.batch.source.common.utils.memory;

/**
 * 메모리 정보를 수집하는 전략 인터페이스
 * Strategy Pattern 을 활용하여 다양한 메모리 수집 방식 지원
 *
 */
public interface MemoryInfoCollector {
    /**
     * 현재 힙 메모리 사용량 정보를 수집합니다.
     *
     * @return 힙 메모리 정보
     */
    HeapMemoryInfo collectHeapMemoryInfo();

    /**
     * 현재 non-heap 메모리 사용량 정보를 수집합니다.
     *
     * @return non-heap 메모리 정보
     */
    NonHeapMemoryInfo collectNonHeapMemoryInfo();
}
