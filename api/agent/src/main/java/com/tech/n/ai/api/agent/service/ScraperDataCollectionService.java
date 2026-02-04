package com.tech.n.ai.api.agent.service;

import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.client.scraper.scraper.AnthropicNewsScraper;
import com.tech.n.ai.client.scraper.scraper.MetaAiBlogScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent용 웹 스크래핑 수집 서비스
 * 기술 블로그 스크래퍼를 통해 기사를 수집하고 제공자별 필터링 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScraperDataCollectionService {

    private final AnthropicNewsScraper anthropicNewsScraper;
    private final MetaAiBlogScraper metaAiBlogScraper;

    /**
     * 기술 블로그 스크래핑
     *
     * @param provider 제공자 필터 ("ANTHROPIC", "META", 빈 문자열=전체)
     * @return 스크래핑된 기사 목록
     */
    public List<ScrapedTechArticle> scrapeArticles(String provider) {
        List<ScrapedTechArticle> allArticles = new ArrayList<>();

        boolean fetchAll = provider == null || provider.isBlank();

        if (fetchAll || "ANTHROPIC".equalsIgnoreCase(provider)) {
            allArticles.addAll(scrapeAnthropic());
        }

        if (fetchAll || "META".equalsIgnoreCase(provider)) {
            allArticles.addAll(scrapeMeta());
        }

        log.info("웹 스크래핑 완료: provider={}, total={}", provider, allArticles.size());
        return allArticles;
    }

    private List<ScrapedTechArticle> scrapeAnthropic() {
        try {
            List<ScrapedTechArticle> articles = anthropicNewsScraper.scrapeArticles();
            log.info("Anthropic 스크래핑 완료: {} articles", articles.size());
            return articles;
        } catch (Exception e) {
            log.error("Anthropic 스크래핑 실패", e);
            return List.of();
        }
    }

    private List<ScrapedTechArticle> scrapeMeta() {
        try {
            List<ScrapedTechArticle> articles = metaAiBlogScraper.scrapeArticles();
            log.info("Meta AI 스크래핑 완료: {} articles", articles.size());
            return articles;
        } catch (Exception e) {
            log.error("Meta AI 스크래핑 실패", e);
            return List.of();
        }
    }
}
