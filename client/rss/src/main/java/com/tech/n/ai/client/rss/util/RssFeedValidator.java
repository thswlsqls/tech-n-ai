package com.tech.n.ai.client.rss.util;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.exception.RssParsingException;
import com.rometools.rome.feed.synd.SyndFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RSS 피드 검증 유틸리티 클래스
 * 피드의 필수 필드 존재 여부 확인 및 중복 항목 제거
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RssFeedValidator {
    
    /**
     * SyndFeed 검증
     * 필수 필드 존재 여부 확인
     * 
     * @param feed 검증할 피드
     * @throws RssParsingException 필수 필드가 없는 경우
     */
    public void validate(SyndFeed feed) {
        if (feed == null) {
            throw new RssParsingException("Feed is null");
        }
        
        if (feed.getTitle() == null || feed.getTitle().isEmpty()) {
            log.warn("Feed title is missing");
        }
        
        if (feed.getLink() == null || feed.getLink().isEmpty()) {
            log.warn("Feed link is missing");
        }
        
        if (feed.getEntries() == null || feed.getEntries().isEmpty()) {
            log.warn("Feed has no entries");
        }
        
        log.debug("Feed validation completed. Title: {}, Entries: {}", 
            feed.getTitle(), feed.getEntries() != null ? feed.getEntries().size() : 0);
    }
    
    /**
     * RssFeedItem 리스트 검증 및 중복 제거
     * GUID 또는 링크를 기준으로 중복 항목 제거
     * 
     * @param items 검증할 아이템 리스트
     * @return 검증 및 중복 제거된 아이템 리스트
     */
    public List<RssFeedItem> validateAndRemoveDuplicates(List<RssFeedItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        
        Set<String> seenGuids = new HashSet<>();
        Set<String> seenLinks = new HashSet<>();
        
        List<RssFeedItem> validatedItems = items.stream()
            .filter(item -> {
                if (item == null) {
                    log.warn("Null item found in feed, skipping");
                    return false;
                }
                
                // 필수 필드 검증
                if (item.title() == null || item.title().isEmpty()) {
                    log.warn("Item with missing title found, skipping. Link: {}", item.link());
                    return false;
                }
                
                if (item.link() == null || item.link().isEmpty()) {
                    log.warn("Item with missing link found, skipping. Title: {}", item.title());
                    return false;
                }
                
                // 중복 제거 (GUID 우선, 없으면 링크 사용)
                if (item.guid() != null && !item.guid().isEmpty()) {
                    if (seenGuids.contains(item.guid())) {
                        log.debug("Duplicate item found by GUID: {}", item.guid());
                        return false;
                    }
                    seenGuids.add(item.guid());
                }
                
                if (seenLinks.contains(item.link())) {
                    log.debug("Duplicate item found by link: {}", item.link());
                    return false;
                }
                seenLinks.add(item.link());
                
                return true;
            })
            .collect(Collectors.toList());
        
        log.debug("Validated {} items, removed {} duplicates", 
            validatedItems.size(), items.size() - validatedItems.size());
        
        return validatedItems;
    }
}
