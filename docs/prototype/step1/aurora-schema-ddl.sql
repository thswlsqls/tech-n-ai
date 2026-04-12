-- =====================================================================
-- Aurora MySQL Schema DDL
-- =====================================================================
-- 작성 일시: 2026-01-22
-- 대상: CQRS 패턴의 Command Side (쓰기 전용)
-- Aurora MySQL 버전: 3.x
-- MySQL 버전: 8.0+
--
-- 실행 순서:
-- 1. 스키마 생성
-- 2. auth 스키마 테이블 생성 (의존성 순서 고려)
-- 3. bookmark 스키마 테이블 생성
-- 4. chatbot 스키마 테이블 생성
-- =====================================================================

-- =====================================================================
-- 1. 스키마 생성
-- =====================================================================

-- auth 스키마 생성
CREATE DATABASE IF NOT EXISTS auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- bookmark 스키마 생성
CREATE DATABASE IF NOT EXISTS bookmark
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- chatbot 스키마 생성
CREATE DATABASE IF NOT EXISTS chatbot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- =====================================================================
-- 2. auth 스키마 테이블 생성
-- =====================================================================

USE auth;

-- ---------------------------------------------------------------------
-- 2.1. providers 테이블 (의존성 없음)
-- ---------------------------------------------------------------------

CREATE TABLE providers (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    name VARCHAR(50) NOT NULL COMMENT '제공자 이름',
    display_name VARCHAR(100) NOT NULL COMMENT '표시 이름',
    client_id VARCHAR(255) NULL COMMENT 'OAuth Client ID',
    client_secret VARCHAR(500) NULL COMMENT 'OAuth Client Secret',
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    UNIQUE KEY uk_provider_name (name),
    INDEX idx_provider_is_enabled (is_enabled),
    INDEX idx_provider_is_deleted (is_deleted)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='OAuth 제공자 테이블';

-- ---------------------------------------------------------------------
-- 2.2. users 테이블 (providers 참조)
-- ---------------------------------------------------------------------

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    email VARCHAR(100) NOT NULL COMMENT '이메일',
    username VARCHAR(50) NOT NULL COMMENT '사용자명',
    password VARCHAR(255) NULL COMMENT '비밀번호 해시',
    provider_id BIGINT UNSIGNED NULL COMMENT 'Provider ID',
    provider_user_id VARCHAR(255) NULL COMMENT 'OAuth 제공자의 사용자 ID',
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE COMMENT '이메일 인증 완료 여부',
    last_login_at TIMESTAMP(6) NULL COMMENT '마지막 로그인 일시',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_username (username),
    INDEX idx_user_provider_id (provider_id),
    INDEX idx_user_provider (provider_id, provider_user_id),
    INDEX idx_user_is_deleted (is_deleted),
    CONSTRAINT fk_user_provider FOREIGN KEY (provider_id) REFERENCES providers(id) ON DELETE SET NULL
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 테이블';

-- ---------------------------------------------------------------------
-- 2.3. admins 테이블 (의존성 없음)
-- ---------------------------------------------------------------------

CREATE TABLE admins (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    email VARCHAR(100) NOT NULL COMMENT '이메일',
    username VARCHAR(50) NOT NULL COMMENT '사용자명',
    password VARCHAR(255) NOT NULL COMMENT '비밀번호 해시',
    role VARCHAR(50) NOT NULL COMMENT '역할',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성화 여부',
    last_login_at TIMESTAMP(6) NULL COMMENT '마지막 로그인 일시',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    UNIQUE KEY uk_admin_email (email),
    UNIQUE KEY uk_admin_username (username),
    INDEX idx_admin_role (role),
    INDEX idx_admin_is_active (is_active),
    INDEX idx_admin_is_deleted (is_deleted)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='관리자 테이블';

-- ---------------------------------------------------------------------
-- 2.4. refresh_tokens 테이블 (users 참조)
-- ---------------------------------------------------------------------

CREATE TABLE refresh_tokens (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '사용자 ID',
    token VARCHAR(500) NOT NULL COMMENT 'Refresh Token',
    expires_at TIMESTAMP(6) NOT NULL COMMENT '만료 일시',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NULL COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    UNIQUE KEY uk_refresh_token_token (token),
    INDEX idx_refresh_token_user_id (user_id),
    INDEX idx_refresh_token_expires_at (expires_at),
    INDEX idx_refresh_token_is_deleted (is_deleted),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='Refresh Token 테이블';

-- ---------------------------------------------------------------------
-- 2.5. email_verifications 테이블 (의존성 없음)
-- ---------------------------------------------------------------------

CREATE TABLE email_verifications (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    email VARCHAR(100) NOT NULL COMMENT '인증 대상 이메일',
    token VARCHAR(255) NOT NULL COMMENT '인증 토큰',
    type VARCHAR(50) NOT NULL COMMENT '토큰 타입 (EMAIL_VERIFICATION, PASSWORD_RESET)',
    expires_at TIMESTAMP(6) NOT NULL COMMENT '만료 일시',
    verified_at TIMESTAMP(6) NULL COMMENT '인증 완료 일시',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NULL COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    UNIQUE KEY uk_email_verification_token (token),
    INDEX idx_email_verification_email (email),
    INDEX idx_email_verification_email_type (email, type),
    INDEX idx_email_verification_expires_at (expires_at),
    INDEX idx_email_verification_is_deleted (is_deleted)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='이메일 인증 테이블';

-- ---------------------------------------------------------------------
-- 2.6. user_history 테이블 (users 참조)
-- ---------------------------------------------------------------------

CREATE TABLE user_history (
    history_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '사용자 ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '작업 타입 (INSERT, UPDATE, DELETE - DELETE는 Soft Delete를 의미)',
    before_data JSON NULL COMMENT '변경 전 데이터',
    after_data JSON NULL COMMENT '변경 후 데이터',
    changed_by BIGINT UNSIGNED NULL COMMENT '변경한 사용자 ID',
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason VARCHAR(500) NULL COMMENT '변경 사유',
    INDEX idx_user_history_user_id (user_id),
    INDEX idx_user_history_changed_at (changed_at),
    INDEX idx_user_history_operation (operation_type, changed_at),
    CONSTRAINT fk_user_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 변경 이력 테이블';

-- ---------------------------------------------------------------------
-- 2.7. admin_history 테이블 (admins 참조)
-- ---------------------------------------------------------------------

CREATE TABLE admin_history (
    history_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    admin_id BIGINT UNSIGNED NOT NULL COMMENT '관리자 ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '작업 타입 (INSERT, UPDATE, DELETE - DELETE는 Soft Delete를 의미)',
    before_data JSON NULL COMMENT '변경 전 데이터',
    after_data JSON NULL COMMENT '변경 후 데이터',
    changed_by BIGINT UNSIGNED NULL COMMENT '변경한 관리자 ID',
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason VARCHAR(500) NULL COMMENT '변경 사유',
    INDEX idx_admin_history_admin_id (admin_id),
    INDEX idx_admin_history_changed_at (changed_at),
    INDEX idx_admin_history_operation (operation_type, changed_at),
    CONSTRAINT fk_admin_history_admin FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='관리자 변경 이력 테이블';

-- =====================================================================
-- 3. bookmark 스키마 테이블 생성
-- =====================================================================

USE bookmark;

-- ---------------------------------------------------------------------
-- 3.1. bookmarks 테이블 (의존성 없음 - FK 없음)
-- ---------------------------------------------------------------------
-- 주의: user_id는 auth.users를 참조하지만 스키마 간 FK는 지원되지 않음
-- 애플리케이션 레벨에서 참조 무결성 보장 필요

CREATE TABLE bookmarks (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '사용자 ID',
    item_type VARCHAR(50) NOT NULL COMMENT '항목 타입',
    item_id VARCHAR(255) NOT NULL COMMENT '항목 ID (MongoDB Atlas ObjectId)',
    tag VARCHAR(100) NULL COMMENT '태그',
    memo TEXT NULL COMMENT '메모',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) NULL COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED NULL COMMENT '삭제한 사용자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED NULL COMMENT '생성한 사용자 ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED NULL COMMENT '수정한 사용자 ID',
    INDEX idx_bookmark_user_id (user_id),
    INDEX idx_bookmark_user_is_deleted (user_id, is_deleted),
    UNIQUE KEY uk_bookmark_user_item (user_id, item_type, item_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='아카이브 테이블';

-- ---------------------------------------------------------------------
-- 3.2. bookmark_history 테이블 (bookmarks 참조)
-- ---------------------------------------------------------------------

CREATE TABLE bookmark_history (
    history_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    bookmark_id BIGINT UNSIGNED NOT NULL COMMENT '북마크 ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '작업 타입 (INSERT, UPDATE, DELETE - DELETE는 Soft Delete를 의미)',
    before_data JSON NULL COMMENT '변경 전 데이터',
    after_data JSON NULL COMMENT '변경 후 데이터',
    changed_by BIGINT UNSIGNED NULL COMMENT '변경한 사용자 ID',
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason VARCHAR(500) NULL COMMENT '변경 사유',
    INDEX idx_bookmark_history_bookmark_id (bookmark_id),
    INDEX idx_bookmark_history_changed_at (changed_at),
    INDEX idx_bookmark_history_operation (operation_type, changed_at),
    CONSTRAINT fk_bookmark_history_bookmark FOREIGN KEY (bookmark_id) REFERENCES bookmarks(id) ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='아카이브 변경 이력 테이블';

-- =====================================================================
-- 4. chatbot 스키마 테이블 생성
-- =====================================================================

USE chatbot;

-- ---------------------------------------------------------------------
-- 4.1. conversation_sessions 테이블 (의존성 없음 - FK 없음)
-- ---------------------------------------------------------------------
-- 주의: user_id는 auth.users를 참조하지만 스키마 간 FK는 지원되지 않음
-- 애플리케이션 레벨에서 참조 무결성 보장 필요

CREATE TABLE conversation_sessions (
    session_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID Primary Key',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '사용자 ID',
    title VARCHAR(200) COMMENT '세션 제목',
    last_message_at TIMESTAMP(6) NOT NULL COMMENT '마지막 메시지 시간',
    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '활성 세션 여부',
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부',
    deleted_at TIMESTAMP(6) COMMENT '삭제 일시',
    deleted_by BIGINT UNSIGNED COMMENT '삭제자 ID',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by BIGINT UNSIGNED COMMENT '생성자 ID',
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 일시',
    updated_by BIGINT UNSIGNED COMMENT '수정자 ID',
    INDEX idx_user_active_lastmsg (user_id, is_active, last_message_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='대화 세션 테이블';

-- ---------------------------------------------------------------------
-- 4.2. conversation_messages 테이블 (conversation_sessions 참조)
-- ---------------------------------------------------------------------

CREATE TABLE conversation_messages (
    message_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID Primary Key',
    session_id BIGINT UNSIGNED NOT NULL COMMENT '세션 ID',
    role VARCHAR(20) NOT NULL COMMENT '메시지 역할 (USER, ASSISTANT, SYSTEM)',
    content TEXT NOT NULL COMMENT '메시지 내용',
    token_count INT COMMENT '토큰 수',
    sequence_number INT NOT NULL COMMENT '대화 순서',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    INDEX idx_session_sequence (session_id, sequence_number),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES conversation_sessions(session_id) ON DELETE CASCADE
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='대화 메시지 테이블';

-- =====================================================================
-- DDL 실행 완료
-- =====================================================================
-- 
-- 생성된 스키마: auth, bookmark, chatbot
-- 생성된 테이블: 12개
--   - auth: providers, users, admins, refresh_tokens, email_verifications,
--           user_history, admin_history
--   - bookmark: bookmarks, bookmark_history
--   - chatbot: conversation_sessions, conversation_messages
-- 
-- 다음 단계:
-- 1. Spring Batch 메타데이터 테이블 생성 (별도 스크립트)
-- 2. 초기 데이터 삽입 (providers 테이블 등)
-- 3. JPA Entity 클래스와 연동 테스트
-- =====================================================================
