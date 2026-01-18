package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.dto.SearchContext;
import com.tech.n.ai.api.chatbot.service.dto.SearchQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 입력 해석 체인
 */
@Slf4j
@Component
public class InputInterpretationChain {
    
    /**
     * 입력을 검색 쿼리로 변환
     * 
     * @param userInput 사용자 입력
     * @return 검색 쿼리
     */
    public SearchQuery interpret(String userInput) {
        // 1. 입력 정제
        String cleanedInput = cleanInput(userInput);
        
        // 2. 검색 쿼리 추출
        // - 불필요한 단어 제거 (예: "알려줘", "찾아줘")
        String searchQuery = extractSearchQuery(cleanedInput);
        
        // 3. 컨텍스트 파악
        SearchContext context = analyzeContext(cleanedInput);
        
        return SearchQuery.builder()
            .query(searchQuery)
            .context(context)
            .build();
    }
    
    /**
     * 입력 정제
     */
    private String cleanInput(String input) {
        // 불필요한 단어 제거
        return input.replaceAll("(알려줘|찾아줘|검색해줘|보여줘|알려주세요|찾아주세요|검색해주세요|보여주세요)", "")
            .trim();
    }
    
    /**
     * 검색 쿼리 추출
     */
    private String extractSearchQuery(String input) {
        // 질문 형태에서 핵심 키워드 추출
        // 예: "최근 대회 정보 알려줘" -> "최근 대회 정보"
        // 현재는 정제된 입력을 그대로 사용 (향후 개선 가능)
        return input;
    }
    
    /**
     * 컨텍스트 분석
     */
    private SearchContext analyzeContext(String input) {
        // 컨텍스트 분석 (대회, 뉴스, 아카이브 등)
        SearchContext context = new SearchContext();
        
        String lowerInput = input.toLowerCase();
        
        if (lowerInput.contains("대회") || lowerInput.contains("contest")) {
            context.addCollection("contests");
        }
        if (lowerInput.contains("뉴스") || lowerInput.contains("news") || lowerInput.contains("기사")) {
            context.addCollection("news_articles");
        }
        if (lowerInput.contains("아카이브") || lowerInput.contains("archive")) {
            context.addCollection("archives");
        }
        
        // 컨텍스트가 없으면 모든 컬렉션 포함
        if (context.getCollections().isEmpty()) {
            context.addCollection("contests");
            context.addCollection("news_articles");
            context.addCollection("archives");
        }
        
        return context;
    }
}
