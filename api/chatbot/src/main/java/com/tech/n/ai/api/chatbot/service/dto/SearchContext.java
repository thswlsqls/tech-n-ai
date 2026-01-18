package com.tech.n.ai.api.chatbot.service.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 검색 컨텍스트 DTO
 */
public class SearchContext {
    
    private final Set<String> collections = new HashSet<>();
    
    public void addCollection(String collection) {
        collections.add(collection);
    }
    
    public List<String> getCollections() {
        return new ArrayList<>(collections);
    }
    
    public boolean includesContests() {
        return collections.contains("contests");
    }
    
    public boolean includesNews() {
        return collections.contains("news_articles");
    }
    
    public boolean includesArchives() {
        return collections.contains("archives");
    }
}
