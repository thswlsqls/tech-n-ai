package com.tech.n.ai.api.chatbot.service;

/**
 * 세션 타이틀 생성 서비스 인터페이스
 */
public interface SessionTitleGenerationService {

    /**
     * 첫 메시지-응답 쌍을 기반으로 세션 타이틀을 비동기 생성하고 저장
     *
     * @param sessionId   세션 ID (TSID String)
     * @param userId      사용자 ID
     * @param userMessage 첫 번째 사용자 메시지
     * @param aiResponse  첫 번째 AI 응답
     */
    void generateAndSaveTitleAsync(String sessionId, Long userId,
                                    String userMessage, String aiResponse);
}
