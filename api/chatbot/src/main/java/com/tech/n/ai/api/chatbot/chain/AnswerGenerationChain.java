package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.PromptService;
import com.tech.n.ai.api.chatbot.service.LLMService;
import com.tech.n.ai.api.chatbot.service.dto.RefinedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 답변 생성 체인
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerGenerationChain {
    
    private final PromptService promptService;
    private final LLMService llmService;
    
    /**
     * 답변 생성
     * 
     * @param query 사용자 쿼리
     * @param refinedResults 정제된 검색 결과
     * @return 생성된 답변
     */
    public String generate(String query, List<RefinedResult> refinedResults) {
        // 1. RefinedResult를 SearchResult로 변환
        List<com.tech.n.ai.api.chatbot.service.dto.SearchResult> searchResults = refinedResults.stream()
            .map(r -> com.tech.n.ai.api.chatbot.service.dto.SearchResult.builder()
                .documentId(r.documentId())
                .text(r.text())
                .score(r.score())
                .collectionType(r.collectionType())
                .metadata(r.metadata())
                .build())
            .toList();
        
        // 2. 프롬프트 생성
        String prompt = promptService.buildPrompt(query, searchResults);
        
        // 3. LLM 호출
        String answer = llmService.generate(prompt);
        
        // 4. 답변 후처리
        return postProcess(answer);
    }
    
    /**
     * 답변 후처리
     */
    private String postProcess(String answer) {
        // 1. 앞뒤 공백 제거
        String cleaned = answer.trim();
        
        // 2. 불필요한 접두사 제거 (예: "답변:", "응답:")
        cleaned = cleaned.replaceAll("^(답변|응답|Answer|Response):\\s*", "");
        
        return cleaned.trim();
    }
}
