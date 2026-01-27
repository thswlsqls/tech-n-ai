package com.tech.n.ai.api.chatbot.service.dto;

/**
 * 의도 분류 결과
 */
public enum Intent {
    /** LLM 직접 요청 (일반 대화, 창작, 번역 등) */
    LLM_DIRECT,

    /** RAG 요청 (내부 데이터 검색 필요) */
    RAG_REQUIRED,

    /** Web 검색 요청 (최신/실시간 정보 필요) */
    WEB_SEARCH_REQUIRED
}
