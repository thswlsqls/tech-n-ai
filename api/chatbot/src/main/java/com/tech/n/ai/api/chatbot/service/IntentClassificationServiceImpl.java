package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.Intent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 의도 분류 서비스 구현체
 */
@Slf4j
@Service
public class IntentClassificationServiceImpl implements IntentClassificationService {
    
    private static final Set<String> GREETING_KEYWORDS = Set.of(
        "안녕", "안녕하세요", "하이", "hi", "hello", "헬로"
    );
    
    private static final Set<String> RAG_KEYWORDS = Set.of(
        "대회", "contest", "뉴스", "news", "기사", "아카이브", "archive",
        "검색", "찾아", "알려", "정보", "어떤", "무엇"
    );
    
    @Override
    public Intent classifyIntent(String preprocessedInput) {
        String lowerInput = preprocessedInput.toLowerCase();
        
        // 1. 인사말 체크
        if (isGreeting(lowerInput)) {
            return Intent.GENERAL_CONVERSATION;
        }
        
        // 2. RAG 키워드 체크
        if (containsRagKeywords(lowerInput)) {
            return Intent.RAG_REQUIRED;
        }
        
        // 3. 질문 형태 체크 (의문사, 물음표)
        if (isQuestion(lowerInput)) {
            return Intent.RAG_REQUIRED;
        }
        
        // 4. 기본값: 일반 대화
        return Intent.GENERAL_CONVERSATION;
    }
    
    private boolean isGreeting(String input) {
        return GREETING_KEYWORDS.stream()
            .anyMatch(input::contains);
    }
    
    private boolean containsRagKeywords(String input) {
        return RAG_KEYWORDS.stream()
            .anyMatch(input::contains);
    }
    
    private boolean isQuestion(String input) {
        // 의문사 체크
        boolean hasQuestionWords = input.matches(".*(무엇|어떤|어디|언제|누가|왜|어떻게).*");
        
        // 물음표 체크
        boolean hasQuestionMark = input.contains("?") || input.contains("？");
        
        return hasQuestionWords || hasQuestionMark;
    }
}
