package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.dto.SearchContext;
import com.tech.n.ai.api.chatbot.service.dto.SearchQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class InputInterpretationChain {
    
    private static final String NOISE_PATTERN = "(알려줘|찾아줘|검색해줘|보여줘|알려주세요|찾아주세요|검색해주세요|보여주세요)";
    private static final Set<String> CONTEST_KEYWORDS = Set.of("대회", "contest");
    private static final Set<String> NEWS_KEYWORDS = Set.of("뉴스", "news", "기사");
    private static final Set<String> ARCHIVE_KEYWORDS = Set.of("아카이브", "archive");
    
    public SearchQuery interpret(String userInput) {
        String cleanedInput = cleanInput(userInput);
        String searchQuery = extractSearchQuery(cleanedInput);
        SearchContext context = analyzeContext(cleanedInput);
        
        return SearchQuery.builder()
            .query(searchQuery)
            .context(context)
            .build();
    }
    
    private String cleanInput(String input) {
        return input.replaceAll(NOISE_PATTERN, "").trim();
    }
    
    private String extractSearchQuery(String input) {
        return input;
    }
    
    private SearchContext analyzeContext(String input) {
        SearchContext context = new SearchContext();
        String lowerInput = input.toLowerCase();
        
        if (containsAny(lowerInput, CONTEST_KEYWORDS)) {
            context.addCollection("contests");
        }
        if (containsAny(lowerInput, NEWS_KEYWORDS)) {
            context.addCollection("news_articles");
        }
        if (containsAny(lowerInput, ARCHIVE_KEYWORDS)) {
            context.addCollection("archives");
        }
        
        if (context.getCollections().isEmpty()) {
            context.addCollection("contests");
            context.addCollection("news_articles");
            context.addCollection("archives");
        }
        
        return context;
    }
    
    private boolean containsAny(String input, Set<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }
}
