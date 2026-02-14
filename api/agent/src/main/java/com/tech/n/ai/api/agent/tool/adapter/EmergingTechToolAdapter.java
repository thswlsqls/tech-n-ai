package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.EmergingTechDetailDto;
import com.tech.n.ai.api.agent.tool.dto.EmergingTechDto;
import com.tech.n.ai.api.agent.tool.dto.EmergingTechListDto;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.common.core.dto.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Emerging Tech API를 LangChain4j Tool 형식으로 래핑하는 어댑터
 * EmergingTechInternalContract를 통해 api-emerging-tech 모듈 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergingTechToolAdapter {

    private final EmergingTechInternalContract emergingTechContract;
    private final ObjectMapper objectMapper;

    @Value("${internal-api.emerging-tech.api-key:}")
    private String apiKey;

    private static final String SUCCESS_CODE = "2000";

    /**
     * Emerging Tech 검색
     *
     * @param query 검색 키워드
     * @param provider 기술 제공자 필터 (빈 문자열이면 전체 검색)
     * @return 검색 결과 목록
     */
    @SuppressWarnings("unchecked")
    public List<EmergingTechDto> search(String query, String provider) {
        try {
            String providerParam = (provider != null && !provider.isBlank()) ? provider : null;
            ApiResponse<Object> response = emergingTechContract.searchEmergingTech(apiKey, query, providerParam, 0, 20);

            if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
                log.warn("Emerging Tech 검색 실패: code={}, message={}", response.code(), response.message());
                return List.of();
            }

            Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            if (items == null) {
                return List.of();
            }

            return items.stream().map(this::mapToDto).toList();
        } catch (Exception e) {
            log.error("Emerging Tech 검색 실패: query={}, provider={}", query, provider, e);
            return List.of();
        }
    }

    /**
     * 목록 조회 (필터 + 페이징)
     */
    @SuppressWarnings("unchecked")
    public EmergingTechListDto list(String startDate, String endDate,
                                     String provider, String updateType,
                                     String sourceType, String status,
                                     int page, int size) {
        try {
            String providerParam = toNullIfBlank(provider);
            String updateTypeParam = toNullIfBlank(updateType);
            String sourceTypeParam = toNullIfBlank(sourceType);
            String statusParam = toNullIfBlank(status);
            String startDateParam = toNullIfBlank(startDate);
            String endDateParam = toNullIfBlank(endDate);

            ApiResponse<Object> response = emergingTechContract.listEmergingTechs(
                apiKey, providerParam, updateTypeParam, statusParam,
                sourceTypeParam, startDateParam, endDateParam,
                page, size, "publishedAt,desc"
            );

            if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
                log.warn("Emerging Tech 목록 조회 실패: code={}, message={}", response.code(), response.message());
                String period = buildPeriodString(startDate, endDate);
                return EmergingTechListDto.empty(page, size, period);
            }

            Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);

            int totalCount = getInt(data, "totalCount", 0);
            int pageNumber = getInt(data, "pageNumber", page);
            int pageSize = getInt(data, "pageSize", size);
            int totalPages = (totalCount + pageSize - 1) / pageSize;

            List<Map<String, Object>> rawItems = (List<Map<String, Object>>) data.get("items");
            List<EmergingTechDto> items = (rawItems != null)
                ? rawItems.stream().map(this::mapToDto).toList()
                : List.of();

            String period = buildPeriodString(startDate, endDate);
            return new EmergingTechListDto(totalCount, pageNumber, pageSize, totalPages, period, items);

        } catch (Exception e) {
            log.error("Emerging Tech 목록 조회 실패", e);
            String period = buildPeriodString(startDate, endDate);
            return EmergingTechListDto.empty(page, size, period);
        }
    }

    /**
     * 상세 조회 (ID 기반)
     */
    @SuppressWarnings("unchecked")
    public EmergingTechDetailDto getDetail(String id) {
        try {
            ApiResponse<Object> response = emergingTechContract.getEmergingTechDetail(apiKey, id);

            if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
                log.warn("Emerging Tech 상세 조회 실패: code={}, message={}", response.code(), response.message());
                return EmergingTechDetailDto.notFound(id);
            }

            Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);

            EmergingTechDetailDto.EmergingTechMetadataDto metadata = null;
            Map<String, Object> rawMetadata = (Map<String, Object>) data.get("metadata");
            if (rawMetadata != null) {
                metadata = new EmergingTechDetailDto.EmergingTechMetadataDto(
                    getString(rawMetadata, "version"),
                    rawMetadata.get("tags") instanceof List<?> tags
                        ? tags.stream().map(Object::toString).toList()
                        : List.of(),
                    getString(rawMetadata, "author"),
                    getString(rawMetadata, "githubRepo")
                );
            }

            return new EmergingTechDetailDto(
                getString(data, "id"),
                getString(data, "provider"),
                getString(data, "updateType"),
                getString(data, "title"),
                getString(data, "summary"),
                getString(data, "url"),
                getString(data, "publishedAt"),
                getString(data, "sourceType"),
                getString(data, "status"),
                getString(data, "externalId"),
                getString(data, "createdAt"),
                getString(data, "updatedAt"),
                metadata
            );

        } catch (Exception e) {
            log.error("Emerging Tech 상세 조회 실패: id={}", id, e);
            return EmergingTechDetailDto.notFound(id);
        }
    }

    /** Map → EmergingTechDto 변환 (공통 매핑) */
    private EmergingTechDto mapToDto(Map<String, Object> item) {
        return new EmergingTechDto(
            getString(item, "id"),
            getString(item, "provider"),
            getString(item, "updateType"),
            getString(item, "title"),
            getString(item, "url"),
            getString(item, "status")
        );
    }

    private String toNullIfBlank(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private String buildPeriodString(String startDate, String endDate) {
        if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
            return "전체";
        }
        String start = (startDate != null && !startDate.isBlank()) ? startDate : "~";
        String end = (endDate != null && !endDate.isBlank()) ? endDate : "~";
        return start + " ~ " + end;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
