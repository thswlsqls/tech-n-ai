package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.common.exception.TokenLimitExceededException;
import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.api.chatbot.service.dto.TokenUsage;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 토큰 서비스 구현체
 */
@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    private final OpenAiTokenizer tokenizer;

    @Value("${chatbot.token.max-input-tokens:4000}")
    private int maxInputTokens;

    @Value("${chatbot.token.max-output-tokens:2000}")
    private int maxOutputTokens;

    @Value("${chatbot.token.warning-threshold:0.8}")
    private double warningThreshold;

    public TokenServiceImpl(@Autowired(required = false) OpenAiTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }
    
    @Override
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // OpenAiTokenizer가 있으면 사용, 없으면 fallback
        if (tokenizer != null) {
            try {
                return tokenizer.estimateTokenCountInText(text);
            } catch (Exception e) {
                log.warn("OpenAiTokenizer failed, falling back to heuristic: {}", e.getMessage());
                return estimateTokensHeuristic(text);
            }
        }

        return estimateTokensHeuristic(text);
    }

    /**
     * Fallback: 휴리스틱 기반 토큰 추정
     */
    private int estimateTokensHeuristic(String text) {
        int wordCount = text.split("\\s+").length;
        int koreanCharCount = (int) text.chars()
            .filter(c -> c >= 0xAC00 && c <= 0xD7A3)
            .count();

        // 한국어 문자는 약 2 토큰, 영어 단어는 약 1.3 토큰
        int estimatedTokens = (int) (koreanCharCount * 2 + (wordCount - koreanCharCount) * 1.3);
        return Math.max(estimatedTokens, text.length() / 4);
    }
    
    @Override
    public void validateInputTokens(String prompt) {
        int tokenCount = estimateTokens(prompt);
        
        if (tokenCount > maxInputTokens) {
            throw new TokenLimitExceededException(
                String.format("입력 토큰 수(%d)가 최대 허용 토큰 수(%d)를 초과했습니다.", 
                    tokenCount, maxInputTokens)
            );
        }
        
        if (tokenCount > maxInputTokens * warningThreshold) {
            log.warn("입력 토큰 수가 경고 임계값을 초과했습니다: {}/{}", 
                tokenCount, maxInputTokens);
        }
    }
    
    @Override
    public List<SearchResult> truncateResults(List<SearchResult> results, int maxTokens) {
        List<SearchResult> truncated = new ArrayList<>();
        int currentTokens = 0;
        
        for (SearchResult result : results) {
            int resultTokens = estimateTokens(result.text());
            if (currentTokens + resultTokens > maxTokens) {
                break;
            }
            truncated.add(result);
            currentTokens += resultTokens;
        }
        
        return truncated;
    }
    
    @Override
    public TokenUsage trackUsage(String requestId, String userId, int inputTokens, int outputTokens) {
        TokenUsage usage = TokenUsage.builder()
            .requestId(requestId)
            .userId(userId)
            .inputTokens(inputTokens)
            .outputTokens(outputTokens)
            .totalTokens(inputTokens + outputTokens)
            .timestamp(Instant.now())
            .build();
        
        // 로깅
        log.info("Token usage tracked: requestId={}, userId={}, inputTokens={}, outputTokens={}, totalTokens={}",
            requestId, userId, inputTokens, outputTokens, usage.totalTokens());
        
        return usage;
    }
}
