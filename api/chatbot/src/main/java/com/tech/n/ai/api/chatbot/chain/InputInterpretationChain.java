package com.tech.n.ai.api.chatbot.chain;

import com.tech.n.ai.api.chatbot.service.dto.SearchContext;
import com.tech.n.ai.api.chatbot.service.dto.SearchQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class InputInterpretationChain {

    private static final String NOISE_PATTERN = "(알려줘|찾아줘|검색해줘|보여줘|알려주세요|찾아주세요|검색해주세요|보여주세요)";
    private static final Set<String> RECENCY_KEYWORDS = Set.of(
        "최신", "최근", "latest", "newest", "recent", "새로운"
    );

    /**
     * 쿼리 키워드 → EmergingTechType enum value 매핑
     * API_UPDATE는 제외 (사용자가 자연어로 명시할 가능성이 낮음)
     */
    private static final Map<Pattern, String> UPDATE_TYPE_KEYWORD_MAP = Map.ofEntries(
        Map.entry(Pattern.compile("\\bsdk\\b"), "SDK_RELEASE"),
        Map.entry(Pattern.compile("모델\\s*(출시|릴리[스즈])"), "MODEL_RELEASE"),
        Map.entry(Pattern.compile("\\bmodel\\s*release\\b"), "MODEL_RELEASE"),
        Map.entry(Pattern.compile("제품\\s*(출시|런칭)"), "PRODUCT_LAUNCH"),
        Map.entry(Pattern.compile("\\bproduct\\s*launch\\b"), "PRODUCT_LAUNCH"),
        Map.entry(Pattern.compile("플랫폼\\s*(업데이트|변경)"), "PLATFORM_UPDATE"),
        Map.entry(Pattern.compile("\\bplatform\\s*update\\b"), "PLATFORM_UPDATE"),
        Map.entry(Pattern.compile("블로그\\s*포스트"), "BLOG_POST"),
        Map.entry(Pattern.compile("\\bblog\\s*post\\b"), "BLOG_POST")
    );

    /**
     * 쿼리 키워드 → TechProvider enum value 매핑 (word boundary pattern 사용)
     */
    private static final Map<Pattern, String> PROVIDER_KEYWORD_MAP = Map.ofEntries(
        Map.entry(Pattern.compile("\\bopenai\\b"), "OPENAI"),
        Map.entry(Pattern.compile("\\banthropic\\b"), "ANTHROPIC"),
        Map.entry(Pattern.compile("\\bclaude\\b"), "ANTHROPIC"),
        Map.entry(Pattern.compile("\\bgoogle\\b"), "GOOGLE"),
        Map.entry(Pattern.compile("\\bgemini\\b"), "GOOGLE"),
        Map.entry(Pattern.compile("\\bmeta\\b"), "META"),
        Map.entry(Pattern.compile("\\bllama\\b"), "META"),
        Map.entry(Pattern.compile("\\bxai\\b"), "XAI"),
        Map.entry(Pattern.compile("\\bgrok\\b"), "XAI")
    );

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

        // RAG 검색 대상: emerging_techs 컬렉션 전용
        context.addCollection("emerging_techs");

        // provider 키워드 감지
        detectProvider(lowerInput, context);

        // update_type 키워드 감지
        detectUpdateType(lowerInput, context);

        // 최신성 키워드 감지
        if (containsAny(lowerInput, RECENCY_KEYWORDS)) {
            context.setRecencyDetected(true);
            log.info("Recency keywords detected in query: {}", input);
        }

        return context;
    }

    /**
     * 쿼리에서 provider 키워드를 감지하여 SearchContext에 추가 (다중 매칭 지원)
     */
    private void detectProvider(String lowerInput, SearchContext context) {
        for (Map.Entry<Pattern, String> entry : PROVIDER_KEYWORD_MAP.entrySet()) {
            if (entry.getKey().matcher(lowerInput).find()) {
                context.addDetectedProvider(entry.getValue());
                log.info("Provider detected from query: pattern={}, provider={}",
                    entry.getKey().pattern(), entry.getValue());
            }
        }
    }
    
    /**
     * 쿼리에서 update_type 키워드를 감지하여 SearchContext에 추가 (다중 매칭 지원)
     */
    private void detectUpdateType(String lowerInput, SearchContext context) {
        for (Map.Entry<Pattern, String> entry : UPDATE_TYPE_KEYWORD_MAP.entrySet()) {
            if (entry.getKey().matcher(lowerInput).find()) {
                context.addDetectedUpdateType(entry.getValue());
                log.info("UpdateType detected from query: pattern={}, updateType={}",
                    entry.getKey().pattern(), entry.getValue());
            }
        }
    }

    private boolean containsAny(String input, Set<String> keywords) {
        return keywords.stream().anyMatch(input::contains);
    }
}
