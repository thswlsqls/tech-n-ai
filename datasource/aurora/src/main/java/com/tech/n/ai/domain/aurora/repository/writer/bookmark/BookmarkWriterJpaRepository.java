package com.tech.n.ai.domain.mariadb.repository.writer.bookmark;

import com.tech.n.ai.domain.mariadb.entity.bookmark.BookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * BookmarkWriterJpaRepository
 */
@Repository
public interface BookmarkWriterJpaRepository extends JpaRepository<BookmarkEntity, Long> {
}
