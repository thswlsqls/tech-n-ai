package com.tech.n.ai.api.chatbot.service.dto;

import lombok.Builder;

/**
 * 벡터 검색 결과
 */
@Builder
public record SearchResult(
    String documentId,      // Document ID
    String text,            // 검색된 텍스트
    Double score,           // 유사도 점수
    String collectionType,  // 컬렉션 타입 (CONTEST, NEWS, BOOKMARK)
    Object metadata         // 메타데이터
) {}
