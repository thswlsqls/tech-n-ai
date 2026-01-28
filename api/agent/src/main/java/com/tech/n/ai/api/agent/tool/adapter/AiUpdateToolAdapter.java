package com.tech.n.ai.api.agent.tool.adapter;

import com.tech.n.ai.api.agent.tool.dto.AiUpdateDto;
import com.tech.n.ai.api.agent.tool.dto.ToolResult;
import com.tech.n.ai.client.feign.domain.internal.contract.AiUpdateInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * AI Update API를 LangChain4j Tool 형식으로 래핑하는 어댑터
 * AiUpdateInternalContract를 통해 api-ai-update 모듈 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiUpdateToolAdapter {

    private final AiUpdateInternalContract aiUpdateContract;
    private final ObjectMapper objectMapper;

    @Value("${internal-api.ai-update.api-key:}")
    private String apiKey;

    private static final String SUCCESS_CODE = "2000";

    /**
     * AI Update 검색
     *
     * @param query 검색 키워드
     * @param provider AI 제공자 필터 (빈 문자열이면 전체 검색)
     * @return 검색 결과 목록
     */
    @SuppressWarnings("unchecked")
    public List<AiUpdateDto> search(String query, String provider) {
        try {
            String providerParam = (provider != null && !provider.isBlank()) ? provider : null;
            ApiResponse<Object> response = aiUpdateContract.searchAiUpdate(apiKey, query, providerParam, 0, 20);

            if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
                log.warn("AI Update 검색 실패: code={}, message={}", response.code(), response.message());
                return List.of();
            }

            Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);
            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

            if (items == null) {
                return List.of();
            }

            return items.stream()
                .map(item -> new AiUpdateDto(
                    getString(item, "id"),
                    getString(item, "provider"),
                    getString(item, "updateType"),
                    getString(item, "title"),
                    getString(item, "url"),
                    getString(item, "status")
                ))
                .toList();
        } catch (Exception e) {
            log.error("AI Update 검색 실패: query={}, provider={}", query, provider, e);
            return List.of();
        }
    }

    /**
     * AI Update Draft 생성
     *
     * @param title 제목
     * @param summary 요약
     * @param provider AI 제공자
     * @param updateType 업데이트 유형
     * @param url 원본 URL
     * @return 생성 결과
     */
    @SuppressWarnings("unchecked")
    public ToolResult createDraft(String title, String summary, String provider, String updateType, String url) {
        try {
            InternalApiDto.AiUpdateCreateRequest request = InternalApiDto.AiUpdateCreateRequest.builder()
                .title(title)
                .summary(summary)
                .provider(provider)
                .updateType(updateType)
                .url(url)
                .sourceType("WEB_SCRAPING")
                .status("DRAFT")
                .build();

            ApiResponse<Object> response = aiUpdateContract.createAiUpdateInternal(apiKey, request);

            if (SUCCESS_CODE.equals(response.code())) {
                Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);
                String id = getString(data, "id");
                return ToolResult.success("초안 포스트 생성 완료", id);
            } else {
                return ToolResult.failure("포스트 생성 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("Draft 포스트 생성 실패: title={}", title, e);
            return ToolResult.failure("포스트 생성 실패: " + e.getMessage());
        }
    }

    /**
     * AI Update 승인 (게시)
     *
     * @param postId 포스트 ID
     * @return 승인 결과
     */
    public ToolResult publish(String postId) {
        try {
            ApiResponse<Object> response = aiUpdateContract.approveAiUpdate(apiKey, postId);

            if (SUCCESS_CODE.equals(response.code())) {
                return ToolResult.success("포스트 게시 완료");
            } else {
                return ToolResult.failure("포스트 게시 실패: " + response.message());
            }
        } catch (Exception e) {
            log.error("포스트 게시 실패: postId={}", postId, e);
            return ToolResult.failure("포스트 게시 실패: " + e.getMessage());
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
