package com.tech.n.ai.domain.mariadb.repository.writer.history;

import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * BookmarkHistoryWriterJpaRepository
 */
@Repository
public interface BookmarkHistoryWriterJpaRepository extends JpaRepository<BookmarkHistoryEntity, Long> {
}
