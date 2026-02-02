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
    private static final Set<String> BOOKMARK_KEYWORDS = Set.of("북마크", "bookmark");
    private static final Set<String> EMERGING_TECH_KEYWORDS = Set.of("기술", "tech", "emerging");

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

        if (containsAny(lowerInput, BOOKMARK_KEYWORDS)) {
            context.addCollection("bookmarks");
        }
        if (containsAny(lowerInput, EMERGING_TECH_KEYWORDS)) {
            context.addCollection("emerging_techs");
        }

        // 키워드 매칭 없으면 전체 검색
        if (context.getCollections().isEmpty()) {
            context.addCollection("bookmarks");
            context.addCollection("emerging_techs");
        }

        return context;
    }
    
    private boolean containsAny(String input, Set<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }
}
