package com.tech.n.ai.api.aiupdate.facade;

import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateBatchRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateCreateRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateListRequest;
import com.tech.n.ai.api.aiupdate.dto.request.AiUpdateSearchRequest;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateBatchResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateDetailResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateListResponse;
import com.tech.n.ai.api.aiupdate.dto.response.AiUpdateSearchResponse;
import com.tech.n.ai.api.aiupdate.service.AiUpdateService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.datasource.mongodb.document.AiUpdateDocument;
import com.tech.n.ai.datasource.mongodb.enums.PostStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Update Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUpdateFacade {

    private final AiUpdateService aiUpdateService;

    /**
     * AI Update 목록 조회
     */
    public AiUpdateListResponse getAiUpdateList(AiUpdateListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );

        Page<AiUpdateDocument> page = aiUpdateService.findAiUpdates(
            request.provider(),
            request.updateType(),
            request.status(),
            pageable
        );

        List<AiUpdateDetailResponse> list = page.getContent().stream()
            .map(AiUpdateDetailResponse::from)
            .toList();

        PageData<AiUpdateDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );

        return AiUpdateListResponse.from(pageData);
    }

    /**
     * AI Update 상세 조회
     */
    public AiUpdateDetailResponse getAiUpdateDetail(String id) {
        AiUpdateDocument document = aiUpdateService.findAiUpdateById(id);
        return AiUpdateDetailResponse.from(document);
    }

    /**
     * AI Update 검색
     */
    public AiUpdateSearchResponse searchAiUpdate(AiUpdateSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size()
        );

        Page<AiUpdateDocument> page = aiUpdateService.searchAiUpdate(
            request.q(),
            pageable
        );

        List<AiUpdateDetailResponse> list = page.getContent().stream()
            .map(AiUpdateDetailResponse::from)
            .toList();

        PageData<AiUpdateDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );

        return AiUpdateSearchResponse.from(pageData);
    }

    /**
     * AI Update 생성 (단건)
     */
    public AiUpdateDetailResponse createAiUpdate(AiUpdateCreateRequest request) {
        AiUpdateDocument document = aiUpdateService.saveAiUpdate(request);
        return AiUpdateDetailResponse.from(document);
    }

    /**
     * AI Update 다건 생성 (부분 롤백)
     */
    public AiUpdateBatchResponse createAiUpdateBatch(AiUpdateBatchRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failureMessages = new ArrayList<>();

        for (AiUpdateCreateRequest item : request.items()) {
            try {
                aiUpdateService.saveAiUpdate(item);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String errorMessage = String.format(
                    "AI Update 저장 실패: title=%s, error=%s",
                    item.title(), e.getMessage()
                );
                log.error(errorMessage, e);
                failureMessages.add(errorMessage);
            }
        }

        return AiUpdateBatchResponse.builder()
            .totalCount(request.items().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .failureMessages(failureMessages)
            .build();
    }

    /**
     * AI Update 승인
     */
    public AiUpdateDetailResponse approveAiUpdate(String id) {
        AiUpdateDocument document = aiUpdateService.updateStatus(id, PostStatus.PUBLISHED.name());
        return AiUpdateDetailResponse.from(document);
    }

    /**
     * AI Update 거부
     */
    public AiUpdateDetailResponse rejectAiUpdate(String id) {
        AiUpdateDocument document = aiUpdateService.updateStatus(id, PostStatus.REJECTED.name());
        return AiUpdateDetailResponse.from(document);
    }

    /**
     * 정렬 문자열 파싱
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }

        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "publishedAt");
        }

        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();

        Sort.Direction sortDirection = "asc".equals(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        return Sort.by(sortDirection, field);
    }
}
