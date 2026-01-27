package com.tech.n.ai.batch.source.domain.sources.sync.writer;

import com.tech.n.ai.datasource.mongodb.document.SourcesDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class SourcesMongoWriter implements ItemWriter<SourcesDocument> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MongoTemplate mongoTemplate;

    @Override
    public void write(Chunk<? extends SourcesDocument> chunk) {
        var items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        UpsertSummary summary = upsertDocuments(items);
        
        log.info("Upserted {} documents (INSERT: {}, UPDATE: {})", 
            items.size(), summary.insertCount(), summary.updateCount());
        
        logUpsertResults(summary.results());
    }

    private UpsertSummary upsertDocuments(List<? extends SourcesDocument> documents) {
        long insertCount = 0;
        long updateCount = 0;
        List<UpsertResult> results = new ArrayList<>();

        for (SourcesDocument document : documents) {
            UpdateResult result = upsertDocument(document);
            
            boolean isInsert = result.getMatchedCount() == 0;
            boolean isUpdate = result.getModifiedCount() == 1;
            
            if (isInsert) {
                insertCount++;
            }
            if (isUpdate) {
                updateCount++;
            }
            
            results.add(new UpsertResult(
                document.getUrl(),
                document.getCategory(),
                isInsert ? "INSERT" : "UPDATE"
            ));
        }

        return new UpsertSummary(insertCount, updateCount, results);
    }

    private UpdateResult upsertDocument(SourcesDocument document) {
        Query query = createQuery(document);
        Update update = createUpdate(document);
        return mongoTemplate.upsert(query, update, SourcesDocument.class);
    }

    private Query createQuery(SourcesDocument document) {
        return Query.query(
            Criteria.where("url").is(document.getUrl())
                .and("category").is(document.getCategory())
        );
    }

    private Update createUpdate(SourcesDocument document) {
        return new Update()
            .set("type", document.getType())
            .set("category", document.getCategory())
            .set("url", document.getUrl())
            .set("api_endpoint", document.getApiEndpoint())
            .set("rss_feed_url", document.getRssFeedUrl())
            .set("description", document.getDescription())
            .set("reliability_score", document.getReliabilityScore())
            .set("accessibility_score", document.getAccessibilityScore())
            .set("data_quality_score", document.getDataQualityScore())
            .set("legal_ethical_score", document.getLegalEthicalScore())
            .set("total_score", document.getTotalScore())
            .set("priority", document.getPriority())
            .set("authentication_required", document.getAuthenticationRequired())
            .set("authentication_method", document.getAuthenticationMethod())
            .set("rate_limit", document.getRateLimit())
            .set("documentation_url", document.getDocumentationUrl())
            .set("update_frequency", document.getUpdateFrequency())
            .set("data_format", document.getDataFormat())
            .set("pros", document.getPros())
            .set("cons", document.getCons())
            .set("implementation_difficulty", document.getImplementationDifficulty())
            .set("cost", document.getCost())
            .set("cost_details", document.getCostDetails())
            .set("recommended_use_case", document.getRecommendedUseCase())
            .set("integration_example", document.getIntegrationExample())
            .set("alternative_sources", document.getAlternativeSources())
            .set("enabled", document.getEnabled())
            .set("updated_at", document.getUpdatedAt())
            .set("updated_by", document.getUpdatedBy())
            .setOnInsert("created_at", document.getCreatedAt())
            .setOnInsert("created_by", document.getCreatedBy());
    }

    private void logUpsertResults(List<UpsertResult> results) {
        try {
            String jsonOutput = OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);
            log.info("Upsert results:\n{}", jsonOutput);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize upsert results to JSON", e);
        }
    }

    private record UpsertResult(String url, String category, String operation) {}
    
    private record UpsertSummary(long insertCount, long updateCount, List<UpsertResult> results) {}
}
