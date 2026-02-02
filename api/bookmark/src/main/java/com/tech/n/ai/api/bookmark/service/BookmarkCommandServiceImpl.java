package com.tech.n.ai.api.bookmark.service;

import com.tech.n.ai.api.bookmark.common.exception.BookmarkDuplicateException;
import com.tech.n.ai.api.bookmark.common.exception.BookmarkNotFoundException;
import com.tech.n.ai.api.bookmark.common.exception.BookmarkValidationException;
import com.tech.n.ai.api.bookmark.dto.request.BookmarkCreateRequest;
import com.tech.n.ai.api.bookmark.dto.request.BookmarkUpdateRequest;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkEntity;
import com.tech.n.ai.domain.mariadb.repository.reader.bookmark.BookmarkReaderRepository;
import com.tech.n.ai.domain.mariadb.repository.writer.bookmark.BookmarkWriterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkCommandServiceImpl implements BookmarkCommandService {
    
    private static final int RESTORE_DAYS_LIMIT = 30;
    
    private final BookmarkReaderRepository bookmarkReaderRepository;
    private final BookmarkWriterRepository bookmarkWriterRepository;
    
    @Transactional
    @Override
    public BookmarkEntity saveBookmark(Long userId, BookmarkCreateRequest request) {
        validateDuplicateBookmark(userId, request.itemType(), request.itemId());
        
        BookmarkEntity bookmark = createBookmark(userId, request);
        BookmarkEntity savedBookmark = bookmarkWriterRepository.save( bookmark);
        
        log.debug("Bookmark created: id={}, userId={}, itemType={}, itemId={}", 
            savedBookmark.getId(), userId, request.itemType(), request.itemId());
        
        return savedBookmark;
    }
    
    private void validateDuplicateBookmark(Long userId, String itemType, String itemId) {
        bookmarkReaderRepository.findByUserIdAndItemTypeAndItemIdAndIsDeletedFalse(
            userId, itemType, itemId
        ).ifPresent(bookmark -> {
            throw new BookmarkDuplicateException("이미 존재하는 북마크입니다.");
        });
    }
    
    private BookmarkEntity createBookmark(Long userId, BookmarkCreateRequest request) {
        BookmarkEntity bookmark = new BookmarkEntity();
        bookmark.setUserId(userId);
        bookmark.setItemType(request.itemType());
        bookmark.setItemId(request.itemId());
        bookmark.setTag(request.tag());
        bookmark.setMemo(request.memo());
        return bookmark;
    }
    
    @Transactional
    @Override
    public BookmarkEntity updateBookmark(Long userId, String bookmarkTsid, BookmarkUpdateRequest request) {
        Long bookmarkId = Long.parseLong(bookmarkTsid);
        BookmarkEntity bookmark = findAndValidateBookmark(userId, bookmarkId);
        
        bookmark.updateContent(request.tag(), request.memo());
        BookmarkEntity updatedBookmark = bookmarkWriterRepository.save( bookmark);
        
        log.debug("Bookmark updated: id={}, userId={}", bookmarkId, userId);
        
        return updatedBookmark;
    }
    
    private BookmarkEntity findAndValidateBookmark(Long userId, Long bookmarkId) {
        BookmarkEntity bookmark = bookmarkReaderRepository.findById(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException("북마크를 찾을 수 없습니다: " + bookmarkId));
        
        if (!bookmark.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 북마크만 접근할 수 있습니다.");
        }
        
        if (Boolean.TRUE.equals(bookmark.getIsDeleted())) {
            throw new BookmarkNotFoundException("삭제된 북마크입니다.");
        }
        
        return bookmark;
    }
    
    @Transactional
    @Override
    public void deleteBookmark(Long userId, String bookmarkTsid) {
        Long bookmarkId = Long.parseLong(bookmarkTsid);
        BookmarkEntity bookmark = findAndValidateBookmark(userId, bookmarkId);
        
        bookmark.setDeletedBy(userId);
        bookmarkWriterRepository.delete( bookmark);
        
        log.debug("Bookmark deleted: id={}, userId={}", bookmarkId, userId);
    }
    
    @Transactional
    @Override
    public BookmarkEntity restoreBookmark(Long userId, String bookmarkTsid) {
        Long bookmarkId = Long.parseLong(bookmarkTsid);
        BookmarkEntity bookmark = findDeletedBookmark(userId, bookmarkId);
        validateRestorePeriod( bookmark);
        
        bookmark.restore();
        BookmarkEntity restoredBookmark = bookmarkWriterRepository.save( bookmark);
        
        log.debug("Bookmark restored: id={}, userId={}", bookmarkId, userId);
        
        return restoredBookmark;
    }
    
    private BookmarkEntity findDeletedBookmark(Long userId, Long bookmarkId) {
        BookmarkEntity bookmark = bookmarkReaderRepository.findById(bookmarkId)
            .orElseThrow(() -> new BookmarkNotFoundException("북마크를 찾을 수 없습니다: " + bookmarkId));
        
        if (!bookmark.isOwnedBy(userId)) {
            throw new UnauthorizedException("본인의 북마크만 접근할 수 있습니다.");
        }
        
        if (!Boolean.TRUE.equals(bookmark.getIsDeleted())) {
            throw new BookmarkValidationException("삭제되지 않은 북마크입니다.");
        }
        
        return bookmark;
    }
    
    private void validateRestorePeriod(BookmarkEntity bookmark) {
        if (!bookmark.canBeRestored(RESTORE_DAYS_LIMIT)) {
            throw new BookmarkValidationException(
                "복구 가능 기간이 지났습니다. (" + RESTORE_DAYS_LIMIT + "일 이내만 복구 가능)");
        }
    }
}
