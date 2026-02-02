package com.tech.n.ai.api.bookmark.service;


import com.tech.n.ai.api.bookmark.common.exception.BookmarkNotFoundException;
import com.tech.n.ai.api.bookmark.common.exception.BookmarkValidationException;
import com.tech.n.ai.api.bookmark.dto.request.BookmarkHistoryListRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkEntity;
import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkHistoryEntity;
import com.tech.n.ai.domain.mariadb.repository.reader.bookmark.BookmarkHistoryReaderRepository;
import com.tech.n.ai.domain.mariadb.repository.reader.bookmark.BookmarkReaderRepository;
import com.tech.n.ai.domain.mariadb.repository.writer.bookmark.BookmarkWriterRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkHistoryServiceImpl implements BookmarkHistoryService {
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    
    private final BookmarkHistoryReaderRepository bookmarkHistoryReaderRepository;
    private final BookmarkReaderRepository bookmarkReaderRepository;
    private final BookmarkWriterRepository bookmarkWriterRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public Page<BookmarkHistoryEntity> findHistory(String userId, String entityId, BookmarkHistoryListRequest request) {
        Long bookmarkId = Long.parseLong(entityId);
        Long currentUserId = Long.parseLong(userId);
        
        validateBookmarkOwnership(bookmarkId, currentUserId);
        
        // 2. 페이징 처리
        Pageable pageable = PageRequest.of(
            request.page() - 1,
            request.size(),
            Sort.by(Sort.Direction.DESC, "changedAt")
        );
        
        // 3. 필터링 조건에 따라 조회
        if (request.operationType() != null && !request.operationType().isBlank()) {
            // operationType 필터링
            if (request.startDate() != null && request.endDate() != null) {
                // 날짜 범위 필터링
                LocalDateTime startDate = LocalDateTime.parse(request.startDate(), ISO_FORMATTER);
                LocalDateTime endDate = LocalDateTime.parse(request.endDate(), ISO_FORMATTER);
                return bookmarkHistoryReaderRepository.findByBookmarkIdAndChangedAtBetween(
                    bookmarkId, startDate, endDate, pageable
                );
            } else {
                // operationType만 필터링
                return bookmarkHistoryReaderRepository.findByBookmarkIdAndOperationType(
                    bookmarkId, request.operationType(), pageable
                );
            }
        } else if (request.startDate() != null && request.endDate() != null) {
            // 날짜 범위만 필터링
            LocalDateTime startDate = LocalDateTime.parse(request.startDate(), ISO_FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(request.endDate(), ISO_FORMATTER);
            return bookmarkHistoryReaderRepository.findByBookmarkIdAndChangedAtBetween(
                bookmarkId, startDate, endDate, pageable
            );
        } else {
            // 필터링 없이 전체 조회
            return bookmarkHistoryReaderRepository.findByBookmarkId(bookmarkId, pageable);
        }
    }
    
    @Override
    public BookmarkHistoryEntity findHistoryAt(String userId, String entityId, String timestamp) {
        Long bookmarkId = Long.parseLong(entityId);
        Long currentUserId = Long.parseLong(userId);
        
        validateBookmarkOwnership(bookmarkId, currentUserId);
        
        // 2. 시점 파싱
        LocalDateTime targetTime = LocalDateTime.parse(timestamp, ISO_FORMATTER);
        
        // 3. 특정 시점 이전의 가장 최근 히스토리 조회
        List<BookmarkHistoryEntity> histories = bookmarkHistoryReaderRepository
            .findTop1ByBookmarkIdAndChangedAtLessThanEqualOrderByChangedAtDesc(bookmarkId, targetTime);
        
        if (histories.isEmpty()) {
            throw new BookmarkNotFoundException("해당 시점의 히스토리를 찾을 수 없습니다: " + timestamp);
        }
        
        return histories.get(0);
    }
    
    @Transactional
    @Override
    public BookmarkEntity restoreFromHistory(String userId, String entityId, String historyId) {
        Long historyIdLong = Long.parseLong(historyId);
        Long bookmarkId = Long.parseLong(entityId);
        
        BookmarkHistoryEntity history = findHistoryById(historyIdLong);
        BookmarkEntity bookmark = findBookmarkById(bookmarkId);
        
        Map<String, Object> afterDataMap = parseHistoryData(history, entityId, historyId);
        updateBookmarkFromHistory(bookmark, afterDataMap);
        
        BookmarkEntity updatedBookmark = bookmarkWriterRepository.save( bookmark);
        
        log.debug("Bookmark restored from history: bookmarkId={}, historyId={}, userId={}", 
            entityId, historyId, userId);
        
        return updatedBookmark;
    }
    
    private void validateBookmarkOwnership(Long bookmarkId, Long userId) {
        BookmarkEntity bookmark = bookmarkReaderRepository.findById(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException("북마크를 찾을 수 없습니다: " + bookmarkId));
        
        if (!bookmark.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 북마크 히스토리만 조회할 수 있습니다.");
        }
    }
    
    private BookmarkHistoryEntity findHistoryById(Long historyId) {
        return bookmarkHistoryReaderRepository.findByHistoryId(historyId)
            .orElseThrow(() -> new BookmarkNotFoundException("히스토리를 찾을 수 없습니다: " + historyId));
    }
    
    private BookmarkEntity findBookmarkById(Long bookmarkId) {
        return bookmarkReaderRepository.findById(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException("북마크를 찾을 수 없습니다: " + bookmarkId));
    }
    
    private Map<String, Object> parseHistoryData(BookmarkHistoryEntity history, String entityId, String historyId) {
        if (history.getAfterData() == null) {
            throw new BookmarkValidationException("히스토리에 복구할 데이터가 없습니다.");
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> afterDataMap = objectMapper.readValue(history.getAfterData(), Map.class);
            return afterDataMap;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse history after_data: bookmarkId={}, historyId={}", entityId, historyId, e);
            throw new BookmarkValidationException("히스토리 데이터 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    private void updateBookmarkFromHistory(BookmarkEntity bookmark, Map<String, Object> afterDataMap) {
        String tag = afterDataMap.containsKey("tag") ? (String) afterDataMap.get("tag") : bookmark.getTag();
        String memo = afterDataMap.containsKey("memo") ? (String) afterDataMap.get("memo") : bookmark.getMemo();
        bookmark.updateContent(tag, memo);
    }
}
