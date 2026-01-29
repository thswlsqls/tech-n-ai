package com.tech.n.ai.client.scraper.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;

/**
 * robots.txt 확인 유틸리티 클래스
 * crawler-commons를 사용하여 robots.txt를 파싱하고 스크래핑 허용 여부 확인
 */
@Component
@Slf4j
public class RobotsTxtChecker {
    
    private final WebClient.Builder webClientBuilder;
    
    public RobotsTxtChecker(@Qualifier("scraperWebClientBuilder") WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }
    
    /**
     * robots.txt를 확인하여 특정 경로의 스크래핑이 허용되는지 확인
     * 
     * @param baseUrl 기본 URL
     * @param path 확인할 경로
     * @return 스크래핑이 허용되면 true, 아니면 false
     */
    public boolean isAllowed(String baseUrl, String path) {
        try {
            String robotsTxtUrl = baseUrl.endsWith("/") 
                ? baseUrl + "robots.txt" 
                : baseUrl + "/robots.txt";
            
            log.debug("Checking robots.txt for {}: {}", baseUrl, robotsTxtUrl);
            
            String robotsTxt = webClientBuilder.build()
                .get()
                .uri(robotsTxtUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (robotsTxt == null || robotsTxt.isEmpty()) {
                log.warn("Empty robots.txt for {}", baseUrl);
                // 보수적으로 접근: robots.txt를 가져올 수 없으면 스크래핑 자제
                return false;
            }
            
            SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
            BaseRobotRules rules = parser.parseContent(robotsTxtUrl, robotsTxt.getBytes(), "text/plain", "ShrimpTM-Demo/1.0");
            
            boolean allowed = rules.isAllowed(path);
            log.debug("robots.txt check for {}: path={}, allowed={}", baseUrl, path, allowed);
            
            return allowed;
        } catch (Exception e) {
            log.warn("Failed to check robots.txt for {}: {}", baseUrl, e.getMessage());
            // 보수적으로 접근: robots.txt를 가져올 수 없으면 스크래핑 자제
            return false;
        }
    }
}
