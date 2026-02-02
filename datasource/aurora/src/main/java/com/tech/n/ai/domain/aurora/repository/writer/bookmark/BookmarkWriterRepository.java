package com.tech.n.ai.domain.aurora.repository.writer.bookmark;

import com.tech.n.ai.domain.aurora.entity.bookmark.BookmarkEntity;
import com.tech.n.ai.domain.aurora.repository.reader.auth.UserReaderRepository;
import com.tech.n.ai.domain.aurora.repository.writer.BaseWriterRepository;
import com.tech.n.ai.domain.aurora.service.history.HistoryService;
import com.tech.n.ai.domain.aurora.service.history.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

/**
 * BookmarkWriterRepository
 */
@Service
@RequiredArgsConstructor
public class BookmarkWriterRepository extends BaseWriterRepository<BookmarkEntity> {

    private final BookmarkWriterJpaRepository bookmarkWriterJpaRepository;
    private final UserReaderRepository userReaderRepository;
    private final HistoryService historyService;

    @Override
    protected JpaRepository<BookmarkEntity, Long> getJpaRepository() {
        return bookmarkWriterJpaRepository;
    }

    @Override
    protected HistoryService getHistoryService() {
        return historyService;
    }

    @Override
    protected String getEntityName() {
        return "Bookmark";
    }

    @Override
    public BookmarkEntity save(BookmarkEntity entity) {
        validateUserId(entity);
        return super.save(entity);
    }

    @Override
    public BookmarkEntity saveAndFlush(BookmarkEntity entity) {
        validateUserId(entity);
        return super.saveAndFlush(entity);
    }

    private void validateUserId(BookmarkEntity entity) {
        if (entity.getUserId() != null && !userReaderRepository.existsById(entity.getUserId())) {
            throw new IllegalArgumentException("User with id " + entity.getUserId() + " does not exist");
        }
    }
}
