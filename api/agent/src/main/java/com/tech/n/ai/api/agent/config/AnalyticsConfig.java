package com.tech.n.ai.api.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 분석 기능 설정
 * 불용어, 분석 파라미터 등을 외부 설정으로 관리
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.analytics")
public class AnalyticsConfig {

    /**
     * 영문 불용어 목록
     * application-agent-api.yml에서 오버라이드 가능
     */
    private List<String> stopWords = List.of(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "can", "shall", "must",
        "in", "on", "at", "to", "for", "of", "with", "by", "from", "as",
        "into", "through", "during", "before", "after", "above", "below",
        "between", "out", "off", "over", "under", "again", "further",
        "and", "but", "or", "nor", "not", "no", "so", "if", "than", "too",
        "very", "just", "about", "also", "now", "here", "there",
        "this", "that", "these", "those", "it", "its", "we", "our",
        "they", "them", "their", "he", "she", "his", "her", "you", "your",
        "all", "each", "every", "both", "few", "more", "most", "other",
        "some", "such", "only", "own", "same", "which", "while", "what",
        "when", "where", "who", "how", "why",
        "up", "down", "then", "once", "any", "new", "been", "being"
    );

    /** 기본 상위 키워드 개수 */
    private int defaultTopN = 20;

    /** 최대 상위 키워드 개수 */
    private int maxTopN = 100;
}
