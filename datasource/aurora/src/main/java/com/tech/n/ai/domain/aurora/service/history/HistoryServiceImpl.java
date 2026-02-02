package com.tech.n.ai.domain.aurora.service.history;

import com.tech.n.ai.domain.aurora.entity.BaseEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HistoryServiceImpl
 */
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private static final ObjectMapper objectMapper = createObjectMapper();
    
    private final List<HistoryEntityFactory> historyEntityFactories;
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public void saveHistory(BaseEntity entity, OperationType operationType, Object beforeData, Object afterData) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        String beforeJson = serializeToJson(beforeData);
        String afterJson = serializeToJson(afterData);
        Long changedBy = getCurrentUserId();
        LocalDateTime changedAt = LocalDateTime.now();

        HistoryEntityFactory factory = findFactory(entity);
        factory.createAndSave(entity, operationType, beforeJson, afterJson, changedBy, changedAt);
    }

    private HistoryEntityFactory findFactory(BaseEntity entity) {
        return historyEntityFactories.stream()
                .filter(factory -> factory.supports(entity))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported entity type: " + entity.getClass().getName()));
    }

    private String serializeToJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    private Long getCurrentUserId() {
        // TODO: SecurityContext에서 현재 사용자 ID 추출
        // 현재는 null 반환
        return null;
    }
}
