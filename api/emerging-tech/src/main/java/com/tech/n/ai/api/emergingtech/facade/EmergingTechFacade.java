package com.tech.n.ai.api.emergingtech.facade;

import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechBatchRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechCreateRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechListRequest;
import com.tech.n.ai.api.emergingtech.dto.request.EmergingTechSearchRequest;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechBatchResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechDetailResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechListResponse;
import com.tech.n.ai.api.emergingtech.dto.response.EmergingTechSearchResponse;
import com.tech.n.ai.api.emergingtech.service.EmergingTechService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.domain.mongodb.document.EmergingTechDocument;
import com.tech.n.ai.domain.mongodb.enums.PostStatus;
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
 * Emerging Tech Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergingTechFacade {

    private final EmergingTechService emergingTechService;

    /**
     * Emerging Tech 목록 조회
     */
    public EmergingTechListResponse getEmergingTechList(EmergingTechListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );

        Page<EmergingTechDocument> page = emergingTechService.findEmergingTechs(
            request.provider(),
            request.updateType(),
            request.status(),
            pageable
        );

        List<EmergingTechDetailResponse> list = page.getContent().stream()
            .map(EmergingTechDetailResponse::from)
            .toList();

        PageData<EmergingTechDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );

        return EmergingTechListResponse.from(pageData);
    }

    /**
     * Emerging Tech 상세 조회
     */
    public EmergingTechDetailResponse getEmergingTechDetail(String id) {
        EmergingTechDocument document = emergingTechService.findEmergingTechById(id);
        return EmergingTechDetailResponse.from(document);
    }

    /**
     * Emerging Tech 검색
     */
    public EmergingTechSearchResponse searchEmergingTech(EmergingTechSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size()
        );

        Page<EmergingTechDocument> page = emergingTechService.searchEmergingTech(
            request.q(),
            pageable
        );

        List<EmergingTechDetailResponse> list = page.getContent().stream()
            .map(EmergingTechDetailResponse::from)
            .toList();

        PageData<EmergingTechDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );

        return EmergingTechSearchResponse.from(pageData);
    }

    /**
     * Emerging Tech 생성 (단건)
     */
    public EmergingTechDetailResponse createEmergingTech(EmergingTechCreateRequest request) {
        EmergingTechDocument document = emergingTechService.saveEmergingTech(request);
        return EmergingTechDetailResponse.from(document);
    }

    /**
     * Emerging Tech 다건 생성 (부분 롤백)
     */
    public EmergingTechBatchResponse createEmergingTechBatch(EmergingTechBatchRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failureMessages = new ArrayList<>();

        for (EmergingTechCreateRequest item : request.items()) {
            try {
                emergingTechService.saveEmergingTech(item);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String errorMessage = String.format(
                    "Emerging Tech 저장 실패: title=%s, error=%s",
                    item.title(), e.getMessage()
                );
                log.error(errorMessage, e);
                failureMessages.add(errorMessage);
            }
        }

        return EmergingTechBatchResponse.builder()
            .totalCount(request.items().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .failureMessages(failureMessages)
            .build();
    }

    /**
     * Emerging Tech 승인
     */
    public EmergingTechDetailResponse approveEmergingTech(String id) {
        EmergingTechDocument document = emergingTechService.updateStatus(id, PostStatus.PUBLISHED.name());
        return EmergingTechDetailResponse.from(document);
    }

    /**
     * Emerging Tech 거부
     */
    public EmergingTechDetailResponse rejectEmergingTech(String id) {
        EmergingTechDocument document = emergingTechService.updateStatus(id, PostStatus.REJECTED.name());
        return EmergingTechDetailResponse.from(document);
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
