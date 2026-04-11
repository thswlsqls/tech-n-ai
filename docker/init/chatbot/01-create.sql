-- =====================================================================
-- chatbot 스키마 테이블 CREATE DDL (docker-entrypoint-initdb.d 용)
-- MYSQL_DATABASE=chatbot 환경변수로 자동 USE 됨
-- V202603130001 마이그레이션 반영: user_id VARCHAR(50)
-- =====================================================================

CREATE TABLE conversation_sessions (
    session_id      BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    user_id         VARCHAR(50)         NOT NULL                    COMMENT '사용자 ID (String 타입 통합)',
    title           VARCHAR(200)        NULL                        COMMENT '세션 제목',
    last_message_at TIMESTAMP(6)        NOT NULL                    COMMENT '마지막 메시지 시간',
    is_active       BOOLEAN             NOT NULL    DEFAULT TRUE    COMMENT '활성 세션 여부',
    is_deleted      BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at      TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by      BIGINT UNSIGNED     NULL                        COMMENT '삭제자 ID',
    created_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by      BIGINT UNSIGNED     NULL                        COMMENT '생성자 ID',
    updated_at      TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by      BIGINT UNSIGNED     NULL                        COMMENT '수정자 ID',
    INDEX       idx_session_user_active_lastmsg (user_id, is_active, last_message_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='대화 세션 테이블';

CREATE TABLE conversation_messages (
    message_id      BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    session_id      BIGINT UNSIGNED     NOT NULL                    COMMENT '세션 ID',
    role            VARCHAR(20)         NOT NULL                    COMMENT '메시지 역할 (USER, ASSISTANT, SYSTEM)',
    content         TEXT                NOT NULL                    COMMENT '메시지 내용',
    token_count     INT                 NULL                        COMMENT '토큰 수 (비용 계산용)',
    sequence_number INT                 NOT NULL                    COMMENT '대화 순서 (1부터 시작)',
    created_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    INDEX       idx_message_session_sequence (session_id, sequence_number),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES conversation_sessions (session_id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='대화 메시지 테이블';
