package com.tech.n.ai.client.rss.parser;

import com.tech.n.ai.client.rss.dto.RssFeedItem;

import java.util.List;

/**
 * RSS 피드 파서 인터페이스
 * DIP (Dependency Inversion Principle) 준수를 위한 인터페이스
 */
public interface RssParser {
    
    /**
     * RSS 피드를 파싱하여 아이템 리스트를 반환
     * @return 파싱된 RSS 피드 아이템 리스트
     */
    List<RssFeedItem> parse();
    
    /**
     * 소스 이름을 반환
     * @return 소스 이름 (예: "TechCrunch", "Google Developers Blog")
     */
    String getSourceName();
    
    /**
     * RSS 피드 URL을 반환
     * @return RSS 피드 URL
     */
    String getFeedUrl();
}
