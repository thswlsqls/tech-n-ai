package com.tech.n.ai.batch.source.domain.sources.sync.reader;

import com.tech.n.ai.batch.source.domain.sources.sync.dto.SourceJsonDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SourcesJsonItemReader extends AbstractPagingItemReader<SourceJsonDto> {

    private static final String JSON_FILE_PATH = "json/sources.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private List<SourceJsonDto> allItems;
    private int currentIndex;

    public SourcesJsonItemReader(int pageSize) {
        setPageSize(pageSize);
    }

    @Override
    protected void doReadPage() {
        initResults();

        if (allItems == null) {
            allItems = loadSourcesFromJson();
            currentIndex = 0;
        }

        addItemsToCurrentPage();
    }

    private void addItemsToCurrentPage() {
        if (allItems == null || currentIndex >= allItems.size()) {
            return;
        }

        int endIndex = Math.min(currentIndex + getPageSize(), allItems.size());
        for (int i = currentIndex; i < endIndex; i++) {
            results.add(allItems.get(i));
        }
        currentIndex = endIndex;
    }

    private void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }

    private List<SourceJsonDto> loadSourcesFromJson() {
        try {
            File jsonFile = new ClassPathResource(JSON_FILE_PATH).getFile();
            JsonNode rootNode = OBJECT_MAPPER.readTree(jsonFile);
            JsonNode categoriesArray = rootNode.get("categories");

            List<SourceJsonDto> sources = flattenCategories(categoriesArray);
            log.info("Loaded {} sources from {}", sources.size(), JSON_FILE_PATH);
            return sources;
        } catch (Exception e) {
            log.error("Failed to load sources from {}", JSON_FILE_PATH, e);
            throw new RuntimeException("Failed to load sources from JSON file", e);
        }
    }

    private List<SourceJsonDto> flattenCategories(JsonNode categoriesArray) {
        List<SourceJsonDto> sources = new ArrayList<>();

        if (categoriesArray == null || !categoriesArray.isArray()) {
            return sources;
        }

        for (JsonNode categoryNode : categoriesArray) {
            String category = categoryNode.get("category").asText();
            JsonNode sourcesArray = categoryNode.get("sources");

            if (sourcesArray != null && sourcesArray.isArray()) {
                sources.addAll(parseSourcesFromCategory(sourcesArray, category));
            }
        }

        return sources;
    }

    private List<SourceJsonDto> parseSourcesFromCategory(JsonNode sourcesArray, String category) {
        List<SourceJsonDto> sources = new ArrayList<>();

        for (JsonNode sourceNode : sourcesArray) {
            try {
                SourceJsonDto dto = OBJECT_MAPPER.treeToValue(sourceNode, SourceJsonDto.class);
                dto.setCategory(category);
                sources.add(dto);
            } catch (Exception e) {
                log.warn("Failed to parse source node in category {}: {}", category, sourceNode, e);
            }
        }

        return sources;
    }

    @Override
    protected void doOpen() {
        allItems = null;
        currentIndex = 0;
    }

    @Override
    protected void doClose() {
        allItems = null;
        currentIndex = 0;
    }
}
