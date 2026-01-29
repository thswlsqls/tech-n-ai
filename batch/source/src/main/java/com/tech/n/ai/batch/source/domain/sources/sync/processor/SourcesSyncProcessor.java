package com.tech.n.ai.batch.source.domain.sources.sync.processor;

import com.tech.n.ai.batch.source.domain.sources.sync.dto.SourceJsonDto;
import com.tech.n.ai.domain.mongodb.document.SourcesDocument;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class SourcesSyncProcessor implements ItemProcessor<SourceJsonDto, SourcesDocument> {

    private static final String BATCH_SYSTEM = "batch-system";

    @Override
    public @Nullable SourcesDocument process(SourceJsonDto item) {
        if (item == null) {
            log.warn("SourceJsonDto is null");
            return null;
        }

        if (!isValid(item)) {
            return null;
        }

        return createDocument(item);
    }

    private boolean isValid(SourceJsonDto item) {
        if (isBlank(item.getUrl())) {
            log.warn("Source url is null or blank, skipping source");
            return false;
        }

        if (isBlank(item.getName())) {
            log.warn("Source name is null or blank, skipping source");
            return false;
        }

        if (isBlank(item.getType())) {
            log.warn("Source type is null or blank, skipping source: {}", item.getName());
            return false;
        }

        if (isBlank(item.getCategory())) {
            log.warn("Source category is null or blank, skipping source: {}", item.getName());
            return false;
        }

        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private SourcesDocument createDocument(SourceJsonDto item) {
        SourcesDocument document = new SourcesDocument();
        
        mapSourceFields(item, document);
        setDefaultValues(document);
        setAuditFields(document);
        
        return document;
    }

    private void mapSourceFields(SourceJsonDto item, SourcesDocument document) {
        document.setName(item.getName());
        document.setType(item.getType());
        document.setCategory(item.getCategory());
        document.setUrl(item.getUrl());
        document.setApiEndpoint(item.getApiEndpoint());
        document.setRssFeedUrl(item.getRssFeedUrl());
        document.setDescription(item.getDescription());
        document.setReliabilityScore(item.getReliabilityScore());
        document.setAccessibilityScore(item.getAccessibilityScore());
        document.setDataQualityScore(item.getDataQualityScore());
        document.setLegalEthicalScore(item.getLegalEthicalScore());
        document.setTotalScore(item.getTotalScore());
        document.setPriority(item.getPriority());
        document.setAuthenticationRequired(item.getAuthenticationRequired());
        document.setAuthenticationMethod(item.getAuthenticationMethod());
        document.setRateLimit(item.getRateLimit());
        document.setDocumentationUrl(item.getDocumentationUrl());
        document.setUpdateFrequency(item.getUpdateFrequency());
        document.setDataFormat(item.getDataFormat());
        document.setPros(item.getPros());
        document.setCons(item.getCons());
        document.setImplementationDifficulty(item.getImplementationDifficulty());
        document.setCost(item.getCost());
        document.setCostDetails(item.getCostDetails());
        document.setRecommendedUseCase(item.getRecommendedUseCase());
        document.setIntegrationExample(item.getIntegrationExample());
        document.setAlternativeSources(item.getAlternativeSources());
    }

    private void setDefaultValues(SourcesDocument document) {
        document.setEnabled(true);
    }

    private void setAuditFields(SourcesDocument document) {
        LocalDateTime now = LocalDateTime.now();
        document.setCreatedAt(now);
        document.setCreatedBy(BATCH_SYSTEM);
        document.setUpdatedAt(now);
        document.setUpdatedBy(BATCH_SYSTEM);
    }
}
