package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.SearchResult;
import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 프롬프트 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {
    
    private final TokenService tokenService;
    
    @Value("${chatbot.rag.max-context-tokens:3000}")
    private int maxContextTokens;
    
    @Override
    public String buildPrompt(String query, List<SearchResult> searchResults) {
        // 1. 검색 결과 토큰 제한
        List<SearchResult> truncatedResults = tokenService.truncateResults(
            searchResults, 
            maxContextTokens
        );
        
        // 2. 프롬프트 템플릿 구성
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 문서들을 참고하여 질문에 답변해주세요.\n\n");
        prompt.append("질문: ").append(query).append("\n\n");
        prompt.append("참고 문서:\n");
        
        for (int i = 0; i < truncatedResults.size(); i++) {
            SearchResult result = truncatedResults.get(i);
            prompt.append(String.format("[문서 %d]\n", i + 1));
            prompt.append(result.text()).append("\n\n");
        }
        
        prompt.append("위 문서들을 바탕으로 질문에 정확하고 간결하게 답변해주세요.");

        // 3. 토큰 수 검증
        tokenService.validateInputTokens(prompt.toString());

        return prompt.toString();
    }

    @Override
    public String buildWebSearchPrompt(String query, List<WebSearchDocument> webResults) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("다음 웹 검색 결과를 참고하여 질문에 답변해주세요.\n\n");
        prompt.append("질문: ").append(query).append("\n\n");
        prompt.append("웹 검색 결과:\n");

        for (int i = 0; i < webResults.size(); i++) {
            WebSearchDocument doc = webResults.get(i);
            prompt.append(String.format("[결과 %d] %s\n", i + 1, doc.title()));
            prompt.append("URL: ").append(doc.url()).append("\n");
            prompt.append(doc.snippet()).append("\n\n");
        }

        prompt.append("위 검색 결과를 바탕으로 질문에 정확하고 간결하게 답변해주세요.");

        tokenService.validateInputTokens(prompt.toString());

        return prompt.toString();
    }
}
