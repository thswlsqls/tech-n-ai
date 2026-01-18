package com.tech.n.ai.client.scraper.scraper;

import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;

import java.util.List;

/**
 * 웹 스크래퍼 인터페이스
 * DIP (Dependency Inversion Principle) 준수를 위한 인터페이스
 */
public interface WebScraper {
    
    /**
     * 웹 페이지를 스크래핑하여 대회 정보 리스트를 반환
     * @return 스크래핑된 대회 정보 리스트
     */
    List<ScrapedContestItem> scrape();
    
    /**
     * 소스 이름을 반환
     * @return 소스 이름 (예: "Devpost", "MLH")
     */
    String getSourceName();
    
    /**
     * 기본 URL을 반환
     * @return 기본 URL
     */
    String getBaseUrl();
}
