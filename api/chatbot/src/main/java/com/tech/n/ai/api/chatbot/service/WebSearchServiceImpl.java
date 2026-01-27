package com.tech.n.ai.api.chatbot.service;

import com.tech.n.ai.api.chatbot.service.dto.WebSearchDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Web 검색 서비스 구현체 (Google Custom Search API)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchServiceImpl implements WebSearchService {

    private final RestTemplate restTemplate;

    @Value("${chatbot.web-search.enabled:false}")
    private boolean enabled;

    @Value("${chatbot.web-search.api-key:}")
    private String apiKey;

    @Value("${chatbot.web-search.search-engine-id:}")
    private String searchEngineId;

    @Value("${chatbot.web-search.max-results:5}")
    private int defaultMaxResults;

    private static final String GOOGLE_SEARCH_API_URL =
        "https://www.googleapis.com/customsearch/v1?key={apiKey}&cx={cx}&q={query}&num={num}";

    @Override
    public List<WebSearchDocument> search(String query) {
        return search(query, defaultMaxResults);
    }

    @Override
    public List<WebSearchDocument> search(String query, int maxResults) {
        if (!enabled) {
            log.warn("Web search is disabled");
            return Collections.emptyList();
        }

        if (apiKey.isBlank() || searchEngineId.isBlank()) {
            log.error("Web search API key or search engine ID is not configured");
            return Collections.emptyList();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                GOOGLE_SEARCH_API_URL,
                Map.class,
                apiKey, searchEngineId, query, maxResults
            );

            return parseSearchResults(response);
        } catch (Exception e) {
            log.error("Web search failed for query: {}", query, e);
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<WebSearchDocument> parseSearchResults(Map<String, Object> response) {
        if (response == null || !response.containsKey("items")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");

        return items.stream()
            .map(item -> WebSearchDocument.builder()
                .title((String) item.get("title"))
                .url((String) item.get("link"))
                .snippet((String) item.get("snippet"))
                .source((String) item.get("displayLink"))
                .build())
            .toList();
    }
}
