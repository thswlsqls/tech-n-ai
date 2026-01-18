package com.tech.n.ai.client.scraper.util;

import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 스크래핑된 데이터 검증 유틸리티 클래스
 * 필수 필드 존재 여부 확인 및 중복 항목 제거
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScrapedDataValidator {
    
    /**
     * ScrapedContestItem 리스트 검증 및 중복 제거
     * URL을 기준으로 중복 항목 제거
     * 
     * @param items 검증할 아이템 리스트
     * @return 검증 및 중복 제거된 아이템 리스트
     */
    public List<ScrapedContestItem> validate(List<ScrapedContestItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        
        Set<String> seenUrls = new HashSet<>();
        
        List<ScrapedContestItem> validatedItems = items.stream()
            .filter(item -> {
                if (item == null) {
                    log.warn("Null item found in scraped data, skipping");
                    return false;
                }
                
                // 필수 필드 검증
                if (item.title() == null || item.title().isEmpty()) {
                    log.warn("Item with missing title found, skipping. URL: {}", item.url());
                    return false;
                }
                
                if (item.url() == null || item.url().isEmpty()) {
                    log.warn("Item with missing URL found, skipping. Title: {}", item.title());
                    return false;
                }
                
                // 중복 제거 (URL 기준)
                if (seenUrls.contains(item.url())) {
                    log.debug("Duplicate item found by URL: {}", item.url());
                    return false;
                }
                seenUrls.add(item.url());
                
                return true;
            })
            .collect(Collectors.toList());
        
        log.debug("Validated {} items, removed {} duplicates", 
            validatedItems.size(), items.size() - validatedItems.size());
        
        return validatedItems;
    }
}
