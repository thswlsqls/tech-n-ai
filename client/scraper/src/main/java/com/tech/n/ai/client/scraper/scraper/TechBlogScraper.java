package com.tech.n.ai.client.scraper.scraper;

import com.tech.n.ai.client.scraper.dto.ScrapedTechArticle;

import java.util.List;

/**
 * 기술 블로그 스크래퍼 인터페이스
 */
public interface TechBlogScraper {

    /**
     * 기술 블로그 페이지를 스크래핑하여 기사 리스트 반환
     */
    List<ScrapedTechArticle> scrapeArticles();

    /**
     * Provider 이름 반환
     */
    String getProviderName();

    /**
     * 기본 URL 반환
     */
    String getBaseUrl();
}
