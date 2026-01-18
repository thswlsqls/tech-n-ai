package com.tech.n.ai.api.contest.facade;

import com.tech.n.ai.api.contest.dto.request.ContestBatchRequest;
import com.tech.n.ai.api.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.api.contest.dto.request.ContestListRequest;
import com.tech.n.ai.api.contest.dto.request.ContestSearchRequest;
import com.tech.n.ai.api.contest.dto.response.ContestBatchResponse;
import com.tech.n.ai.api.contest.dto.response.ContestDetailResponse;
import com.tech.n.ai.api.contest.dto.response.ContestListResponse;
import com.tech.n.ai.api.contest.dto.response.ContestSearchResponse;
import com.tech.n.ai.api.contest.service.ContestService;
import com.tech.n.ai.common.core.dto.PageData;
import com.tech.n.ai.datasource.mongodb.document.ContestDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Contest Facade
 * Controller와 Service 사이의 중간 계층
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContestFacade {
    
    private final ContestService contestService;
    
    /**
     * Contest 목록 조회
     * 
     * @param request ContestListRequest
     * @return ContestListResponse
     */
    public ContestListResponse getContestList(ContestListRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            parseSort(request.sort())
        );
        
        ObjectId sourceId = request.sourceId() != null ? new ObjectId(request.sourceId()) : null;
        
        Page<ContestDocument> page = contestService.findContests(
            sourceId,
            request.status(),
            pageable
        );
        
        // Page<ContestDocument>를 PageData<ContestDetailResponse>로 변환
        List<ContestDetailResponse> list = page.getContent().stream()
            .map(ContestDetailResponse::from)
            .toList();
        
        PageData<ContestDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ContestListResponse.from(pageData);
    }
    
    /**
     * Contest 상세 조회
     * 
     * @param id Contest ID
     * @return ContestDetailResponse
     */
    public ContestDetailResponse getContestDetail(String id) {
        ContestDocument document = contestService.findContestById(id);
        return ContestDetailResponse.from(document);
    }
    
    /**
     * Contest 검색
     * 
     * @param request ContestSearchRequest
     * @return ContestSearchResponse
     */
    public ContestSearchResponse searchContest(ContestSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size()
        );
        
        Page<ContestDocument> page = contestService.searchContest(
            request.q(),
            pageable
        );
        
        // Page<ContestDocument>를 PageData<ContestDetailResponse>로 변환
        List<ContestDetailResponse> list = page.getContent().stream()
            .map(ContestDetailResponse::from)
            .toList();
        
        PageData<ContestDetailResponse> pageData = PageData.of(
            request.size(),
            request.page(),
            (int) page.getTotalElements(),
            list
        );
        
        return ContestSearchResponse.from(pageData);
    }
    
    /**
     * Contest 생성 (단건 처리, 내부 API)
     * 
     * @param request ContestCreateRequest
     * @return ContestDetailResponse
     */
    public ContestDetailResponse createContest(ContestCreateRequest request) {
        ContestDocument document = contestService.saveContest(request);
        return ContestDetailResponse.from(document);
    }
    
    /**
     * Contest 다건 생성 (내부 API) - 부분 롤백 구현
     * @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
     * 
     * @param request ContestBatchRequest
     * @return ContestBatchResponse
     */
    public ContestBatchResponse createContestBatch(ContestBatchRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failureMessages = new ArrayList<>();
        
        for (ContestCreateRequest item : request.contests()) {
            try {
                // 단건 처리 Service 메서드 호출 (각 호출마다 독립적인 트랜잭션)
                contestService.saveContest(item);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String errorMessage = String.format(
                    "Contest 저장 실패: sourceId=%s, title=%s, error=%s",
                    item.sourceId(), item.title(), e.getMessage()
                );
                log.error(errorMessage, e);
                failureMessages.add(errorMessage);
                // 예외를 catch하고 로그만 출력하여 다음 항목 계속 처리
            }
        }
        
        return ContestBatchResponse.builder()
            .totalCount(request.contests().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .failureMessages(failureMessages)
            .build();
    }
    
    /**
     * 정렬 문자열을 Sort 객체로 변환
     * 
     * @param sort 정렬 문자열 (예: "startDate,desc")
     * @return Sort 객체
     */
    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "startDate");
        }
        
        String[] parts = sort.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.DESC, "startDate");
        }
        
        String field = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        Sort.Direction sortDirection = "asc".equals(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        return Sort.by(sortDirection, field);
    }
}
