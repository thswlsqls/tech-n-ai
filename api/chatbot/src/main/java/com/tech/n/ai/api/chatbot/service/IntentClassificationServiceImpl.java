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
        "대회", "contest", "뉴스", "news", "기사", "북마크", "bookmark",
        "검색", "찾아", "알려", "정보", "어떤", "무엇",
        "kaggle", "codeforces", "leetcode", "hackathon"
    );

    // Web 검색이 필요한 최신/실시간 정보 키워드
    private static final Set<String> WEB_SEARCH_KEYWORDS = Set.of(
        "오늘", "현재", "지금", "최근", "today", "now", "latest", "current",
        "날씨", "weather", "주가", "stock", "환율", "exchange rate",
        "뉴스 속보", "breaking news", "실시간",
        "검색해줘", "찾아줘", "인터넷에서"
    );

    // 창작/텍스트 처리 요청 키워드 (LLM 직접 처리)
    private static final Set<String> LLM_DIRECT_KEYWORDS = Set.of(
        "작성해줘", "만들어줘", "써줘", "번역", "요약", "설명해줘",
        "write", "create", "translate", "summarize", "explain"
    );

    @Override
    public Intent classifyIntent(String preprocessedInput) {
        String lowerInput = preprocessedInput.toLowerCase();

        // 1. Web 검색 키워드 체크 (최우선)
        if (containsWebSearchKeywords(lowerInput)) {
            log.info("Intent: WEB_SEARCH_REQUIRED - {}", truncateForLog(preprocessedInput));
            return Intent.WEB_SEARCH_REQUIRED;
        }

        // 2. RAG 키워드 체크
        if (containsRagKeywords(lowerInput)) {
            log.info("Intent: RAG_REQUIRED - {}", truncateForLog(preprocessedInput));
            return Intent.RAG_REQUIRED;
        }

        // 3. 질문 형태 체크 (RAG 관련 질문일 가능성)
        if (isQuestion(lowerInput) && !containsLlmDirectKeywords(lowerInput)) {
            log.info("Intent: RAG_REQUIRED (question) - {}", truncateForLog(preprocessedInput));
            return Intent.RAG_REQUIRED;
        }

        // 4. 기본값: LLM 직접 처리
        log.info("Intent: LLM_DIRECT - {}", truncateForLog(preprocessedInput));
        return Intent.LLM_DIRECT;
    }

    private boolean isGreeting(String input) {
        return GREETING_KEYWORDS.stream().anyMatch(input::contains);
    }

    private boolean containsRagKeywords(String input) {
        return RAG_KEYWORDS.stream().anyMatch(input::contains);
    }

    private boolean containsWebSearchKeywords(String input) {
        return WEB_SEARCH_KEYWORDS.stream().anyMatch(input::contains);
    }

    private boolean containsLlmDirectKeywords(String input) {
        return LLM_DIRECT_KEYWORDS.stream().anyMatch(input::contains);
    }

    private boolean isQuestion(String input) {
        // 의문사 체크 (구어체 포함)
        boolean hasQuestionWords = input.matches(
            ".*(무엇|어떤|어디|언제|누가|왜|어떻게|뭐|몇|얼마|어느).*"
        );
        boolean hasQuestionMark = input.contains("?") || input.contains("？");
        return hasQuestionWords || hasQuestionMark;
    }

    private String truncateForLog(String input) {
        return input.length() > 50 ? input.substring(0, 50) + "..." : input;
    }
}
