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
    private final List<String> detectedProviders = new ArrayList<>();
    private final List<String> detectedUpdateTypes = new ArrayList<>();
    private boolean recencyDetected;

    public void addCollection(String collection) {
        collections.add(collection);
    }

    public List<String> getCollections() {
        return new ArrayList<>(collections);
    }

    public boolean includesEmergingTechs() {
        return collections.contains("emerging_techs");
    }

    public List<String> getDetectedProviders() {
        return detectedProviders;
    }

    public void addDetectedProvider(String provider) {
        if (!detectedProviders.contains(provider)) {
            detectedProviders.add(provider);
        }
    }

    public List<String> getDetectedUpdateTypes() {
        return detectedUpdateTypes;
    }

    public void addDetectedUpdateType(String updateType) {
        if (!detectedUpdateTypes.contains(updateType)) {
            detectedUpdateTypes.add(updateType);
        }
    }

    public boolean isRecencyDetected() {
        return recencyDetected;
    }

    public void setRecencyDetected(boolean recencyDetected) {
        this.recencyDetected = recencyDetected;
    }
}
