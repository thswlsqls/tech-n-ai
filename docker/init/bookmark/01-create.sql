-- =====================================================================
-- bookmark 스키마 테이블 CREATE DDL (docker-entrypoint-initdb.d 용)
-- MYSQL_DATABASE=bookmark 환경변수로 자동 USE 됨
-- =====================================================================

CREATE TABLE bookmarks (
    id                  BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    user_id             BIGINT UNSIGNED     NOT NULL                    COMMENT '사용자 ID (auth.users 참조)',
    emerging_tech_id    VARCHAR(24)         NOT NULL                    COMMENT 'MongoDB EmergingTech ObjectId',
    title               VARCHAR(500)        NOT NULL                    COMMENT '북마크 제목',
    url                 VARCHAR(2048)       NOT NULL                    COMMENT 'URL',
    provider            VARCHAR(50)         NULL                        COMMENT '콘텐츠 제공자 (hacker-news, dev.to 등)',
    summary             TEXT                NULL                        COMMENT '콘텐츠 요약',
    published_at        TIMESTAMP(6)        NULL                        COMMENT '원본 게시일',
    tag                 VARCHAR(100)        NULL                        COMMENT '태그 (파이프 구분: tag1|tag2|tag3)',
    memo                TEXT                NULL                        COMMENT '사용자 메모',
    is_deleted          BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at          TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by          BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at          TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by          BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at          TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by          BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    INDEX       idx_bookmark_user_id        (user_id),
    INDEX       idx_bookmark_user_deleted   (user_id, is_deleted),
    INDEX       idx_bookmark_emerging_tech  (emerging_tech_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='EmergingTech 북마크 테이블';

CREATE TABLE bookmark_history (
    history_id      BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    bookmark_id     BIGINT UNSIGNED     NOT NULL                    COMMENT '북마크 ID',
    operation_type  VARCHAR(20)         NOT NULL                    COMMENT '작업 타입 (INSERT, UPDATE, DELETE)',
    before_data     JSON                NULL                        COMMENT '변경 전 데이터',
    after_data      JSON                NULL                        COMMENT '변경 후 데이터',
    changed_by      BIGINT UNSIGNED     NULL                        COMMENT '변경한 사용자 ID',
    changed_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason   VARCHAR(500)        NULL                        COMMENT '변경 사유',
    INDEX       idx_bookmark_history_bookmark_id (bookmark_id),
    INDEX       idx_bookmark_history_changed_at  (changed_at),
    INDEX       idx_bookmark_history_operation   (operation_type, changed_at),
    CONSTRAINT fk_bookmark_history_bookmark FOREIGN KEY (bookmark_id) REFERENCES bookmarks (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='북마크 변경 이력 테이블';
