package com.tech.n.ai.api.bookmark.controller;

import com.tech.n.ai.api.bookmark.dto.request.*;
import com.tech.n.ai.api.bookmark.dto.response.*;
import com.tech.n.ai.api.bookmark.facade.BookmarkFacade;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.security.principal.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Bookmark API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/bookmark")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkFacade bookmarkFacade;

    /**
     * 북마크 저장
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> saveBookmark(
            @Valid @RequestBody BookmarkCreateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkDetailResponse response = bookmarkFacade.saveBookmark(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<BookmarkListResponse>> getBookmarkList(
            @Valid BookmarkListRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkListResponse response = bookmarkFacade.getBookmarkList(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> getBookmarkDetail(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkDetailResponse response = bookmarkFacade.getBookmarkDetail(userPrincipal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> updateBookmark(
            @PathVariable String id,
            @Valid @RequestBody BookmarkUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkDetailResponse response = bookmarkFacade.updateBookmark(userPrincipal.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBookmark(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        bookmarkFacade.deleteBookmark(userPrincipal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 삭제된 북마크 목록 조회
     */
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<BookmarkListResponse>> getDeletedBookmarks(
            @Valid BookmarkDeletedListRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkListResponse response = bookmarkFacade.getDeletedBookmarks(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 복구
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> restoreBookmark(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkDetailResponse response = bookmarkFacade.restoreBookmark(userPrincipal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 북마크 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<BookmarkSearchResponse>> searchBookmarks(
            @Valid BookmarkSearchRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkSearchResponse response = bookmarkFacade.searchBookmarks(userPrincipal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 변경 이력 조회
     */
    @GetMapping("/history/{entityId}")
    public ResponseEntity<ApiResponse<BookmarkHistoryListResponse>> getHistory(
            @PathVariable String entityId,
            @Valid BookmarkHistoryListRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkHistoryListResponse response = bookmarkFacade.getHistory(userPrincipal.userId(), entityId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 시점 데이터 조회
     */
    @GetMapping("/history/{entityId}/at")
    public ResponseEntity<ApiResponse<BookmarkHistoryDetailResponse>> getHistoryAt(
            @PathVariable String entityId,
            @RequestParam String timestamp,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkHistoryDetailResponse response = bookmarkFacade.getHistoryAt(userPrincipal.userId(), entityId, timestamp);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 버전으로 복구
     */
    @PostMapping("/history/{entityId}/restore")
    public ResponseEntity<ApiResponse<BookmarkDetailResponse>> restoreFromHistory(
            @PathVariable String entityId,
            @RequestParam String historyId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        BookmarkDetailResponse response = bookmarkFacade.restoreFromHistory(userPrincipal.userId(), entityId, historyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
