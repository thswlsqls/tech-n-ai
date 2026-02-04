package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.EmergingTechDto;
import com.tech.n.ai.client.feign.domain.internal.contract.EmergingTechInternalContract;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            return items.stream()
                .map(item -> new EmergingTechDto(
                    getString(item, "id"),
                    getString(item, "provider"),
                    getString(item, "updateType"),
                    getString(item, "title"),
                    getString(item, "url"),
                    getString(item, "status")
                ))
                .toList();
        } catch (Exception e) {
            log.error("Emerging Tech 검색 실패: query={}, provider={}", query, provider, e);
            return List.of();
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
