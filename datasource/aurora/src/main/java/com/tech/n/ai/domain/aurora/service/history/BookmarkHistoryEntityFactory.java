package com.tech.n.ai.domain.mariadb.service.history;

import com.tech.n.ai.domain.mariadb.entity.BaseEntity;
import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkEntity;
import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkHistoryEntity;
import com.tech.n.ai.domain.mariadb.repository.writer.history.BookmarkHistoryWriterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * BookmarkHistoryEntity 생성을 담당하는 Factory
 */
@Component
@RequiredArgsConstructor
public class BookmarkHistoryEntityFactory implements HistoryEntityFactory {

    private final BookmarkHistoryWriterRepository bookmarkHistoryWriterRepository;

    @Override
    public void createAndSave(BaseEntity entity, OperationType operationType, 
                              String beforeJson, String afterJson, 
                              Long changedBy, LocalDateTime changedAt) {
        BookmarkEntity bookmarkEntity = (BookmarkEntity) entity;
        BookmarkHistoryEntity history = new BookmarkHistoryEntity();
        history.setBookmark(bookmarkEntity);
        history.setBookmarkId(bookmarkEntity.getId());
        history.setOperationType(operationType.name());
        history.setBeforeData(beforeJson);
        history.setAfterData(afterJson);
        history.setChangedBy(changedBy);
        history.setChangedAt(changedAt);
        bookmarkHistoryWriterRepository.save(history);
    }

    @Override
    public boolean supports(BaseEntity entity) {
        return entity instanceof BookmarkEntity;
    }
}
