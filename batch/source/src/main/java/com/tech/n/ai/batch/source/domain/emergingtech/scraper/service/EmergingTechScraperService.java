package com.tech.n.ai.batch.source.domain.emergingtech.scraper.service;

import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;
import com.tech.n.ai.client.scraper.scraper.AnthropicNewsScraper;
import com.tech.n.ai.client.scraper.scraper.MetaAiBlogScraper;
import com.tech.n.ai.client.scraper.scraper.XaiNewsScraper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Emerging Tech 웹 크롤링 수집 서비스
 * Anthropic + Meta AI + xAI 기술 블로그를 모두 크롤링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergingTechScraperService {

    private final AnthropicNewsScraper anthropicScraper;
    private final MetaAiBlogScraper metaScraper;
    private final XaiNewsScraper xaiScraper;

    public List<ScrapedTechArticle> scrapeAllSources() {
        List<ScrapedTechArticle> allArticles = new ArrayList<>();

        try {
            List<ScrapedTechArticle> anthropicArticles = anthropicScraper.scrapeArticles();
            log.info("Anthropic News: {} articles scraped", anthropicArticles.size());
            allArticles.addAll(anthropicArticles);
        } catch (Exception e) {
            log.error("Failed to scrape Anthropic News", e);
        }

        try {
            List<ScrapedTechArticle> metaArticles = metaScraper.scrapeArticles();
            log.info("Meta AI Blog: {} articles scraped", metaArticles.size());
            allArticles.addAll(metaArticles);
        } catch (Exception e) {
            log.error("Failed to scrape Meta AI Blog", e);
        }

        /** TODO.
        try {
            List<ScrapedTechArticle> xaiArticles = xaiScraper.scrapeArticles();
            log.info("xAI News: {} articles scraped", xaiArticles.size());
            allArticles.addAll(xaiArticles);
        } catch (Exception e) {
            log.error("Failed to scrape xAI News", e);
        }
        */

        return allArticles;
    }
}
