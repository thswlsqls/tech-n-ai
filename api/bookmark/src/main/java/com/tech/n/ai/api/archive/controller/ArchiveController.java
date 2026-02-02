package com.tech.n.ai.api.archive.controller;

import com.tech.n.ai.api.archive.dto.request.*;
import com.tech.n.ai.api.archive.dto.response.*;
import com.tech.n.ai.api.archive.facade.ArchiveFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Archive API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/archive")
@RequiredArgsConstructor
public class ArchiveController {
    
    private final ArchiveFacade archiveFacade;
    
    /**
     * 아카이브 저장
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArchiveDetailResponse>> saveArchive(
            @Valid @RequestBody ArchiveCreateRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveDetailResponse response = archiveFacade.saveArchive(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ArchiveListResponse>> getArchiveList(
            @Valid ArchiveListRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveListResponse response = archiveFacade.getArchiveList(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArchiveDetailResponse>> getArchiveDetail(
            @PathVariable String id,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveDetailResponse response = archiveFacade.getArchiveDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArchiveDetailResponse>> updateArchive(
            @PathVariable String id,
            @Valid @RequestBody ArchiveUpdateRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveDetailResponse response = archiveFacade.updateArchive(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArchive(
            @PathVariable String id,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        archiveFacade.deleteArchive(userId, id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    /**
     * 삭제된 아카이브 목록 조회
     */
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<ArchiveListResponse>> getDeletedArchives(
            @Valid ArchiveDeletedListRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveListResponse response = archiveFacade.getDeletedArchives(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 복구
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<ArchiveDetailResponse>> restoreArchive(
            @PathVariable String id,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveDetailResponse response = archiveFacade.restoreArchive(userId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 아카이브 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ArchiveSearchResponse>> searchArchives(
            @Valid ArchiveSearchRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveSearchResponse response = archiveFacade.searchArchives(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 변경 이력 조회
     */
    @GetMapping("/history/{entityId}")
    public ResponseEntity<ApiResponse<ArchiveHistoryListResponse>> getHistory(
            @PathVariable String entityId,
            @Valid ArchiveHistoryListRequest request,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveHistoryListResponse response = archiveFacade.getHistory(userId, entityId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 특정 시점 데이터 조회
     */
    @GetMapping("/history/{entityId}/at")
    public ResponseEntity<ApiResponse<ArchiveHistoryDetailResponse>> getHistoryAt(
            @PathVariable String entityId,
            @RequestParam String timestamp,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveHistoryDetailResponse response = archiveFacade.getHistoryAt(userId, entityId, timestamp);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 특정 버전으로 복구
     */
    @PostMapping("/history/{entityId}/restore")
    public ResponseEntity<ApiResponse<ArchiveDetailResponse>> restoreFromHistory(
            @PathVariable String entityId,
            @RequestParam String historyId,
            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        ArchiveDetailResponse response = archiveFacade.restoreFromHistory(userId, entityId, historyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
