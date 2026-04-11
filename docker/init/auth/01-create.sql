-- =====================================================================
-- auth 스키마 테이블 CREATE DDL (docker-entrypoint-initdb.d 용)
-- MYSQL_DATABASE=auth 환경변수로 자동 USE 됨
-- =====================================================================

CREATE TABLE providers (
    id              BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    name            VARCHAR(50)         NOT NULL                    COMMENT '제공자 이름 (google, github 등)',
    display_name    VARCHAR(100)        NOT NULL                    COMMENT '표시 이름',
    client_id       VARCHAR(255)        NULL                        COMMENT 'OAuth Client ID',
    client_secret   VARCHAR(500)        NULL                        COMMENT 'OAuth Client Secret',
    is_enabled      BOOLEAN             NOT NULL    DEFAULT TRUE    COMMENT '활성화 여부',
    is_deleted      BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at      TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by      BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by      BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at      TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by      BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    UNIQUE  KEY uk_provider_name        (name),
    INDEX       idx_provider_is_enabled (is_enabled),
    INDEX       idx_provider_is_deleted (is_deleted)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='OAuth 제공자 테이블';

CREATE TABLE users (
    id                  BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    email               VARCHAR(100)        NOT NULL                    COMMENT '이메일',
    username            VARCHAR(50)         NOT NULL                    COMMENT '사용자명',
    password            VARCHAR(255)        NULL                        COMMENT '비밀번호 해시 (OAuth 사용자는 NULL)',
    provider_id         BIGINT UNSIGNED     NULL                        COMMENT 'OAuth Provider ID',
    provider_user_id    VARCHAR(255)        NULL                        COMMENT 'OAuth 제공자의 사용자 ID',
    is_email_verified   BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '이메일 인증 완료 여부',
    last_login_at       TIMESTAMP(6)        NULL                        COMMENT '마지막 로그인 일시',
    is_deleted          BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at          TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by          BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at          TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by          BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at          TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by          BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    UNIQUE  KEY uk_user_email           (email),
    UNIQUE  KEY uk_user_username        (username),
    INDEX       idx_user_provider       (provider_id, provider_user_id),
    INDEX       idx_user_is_deleted     (is_deleted),
    CONSTRAINT fk_user_provider FOREIGN KEY (provider_id) REFERENCES providers (id) ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자 테이블';

CREATE TABLE admins (
    id              BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    email           VARCHAR(100)        NOT NULL                    COMMENT '이메일',
    username        VARCHAR(50)         NOT NULL                    COMMENT '사용자명',
    password        VARCHAR(255)        NOT NULL                    COMMENT '비밀번호 해시',
    role            VARCHAR(50)         NOT NULL                    COMMENT '역할 (ADMIN, SUPER_ADMIN 등)',
    is_active       BOOLEAN             NOT NULL    DEFAULT TRUE    COMMENT '활성화 여부',
    last_login_at   TIMESTAMP(6)        NULL                        COMMENT '마지막 로그인 일시',
    failed_login_attempts INT          NOT NULL    DEFAULT 0       COMMENT '연속 로그인 실패 횟수',
    account_locked_until  TIMESTAMP(6) NULL                        COMMENT '계정 잠금 해제 시각',
    is_deleted      BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at      TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by      BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by      BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at      TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by      BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    UNIQUE  KEY uk_admin_email      (email),
    UNIQUE  KEY uk_admin_username   (username),
    INDEX       idx_admin_role      (role),
    INDEX       idx_admin_is_active (is_active),
    INDEX       idx_admin_is_deleted(is_deleted)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='관리자 테이블';

CREATE TABLE refresh_tokens (
    id          BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    user_id     BIGINT UNSIGNED     NULL                        COMMENT '사용자 ID (User용 토큰)',
    admin_id    BIGINT UNSIGNED     NULL                        COMMENT '관리자 ID (Admin용 토큰)',
    token       VARCHAR(500)        NOT NULL                    COMMENT 'Refresh Token',
    expires_at  TIMESTAMP(6)        NOT NULL                    COMMENT '만료 일시',
    is_deleted  BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at  TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by  BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at  TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by  BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at  TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by  BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    UNIQUE  KEY uk_refresh_token        (token),
    INDEX       idx_refresh_user_id     (user_id),
    INDEX       idx_refresh_admin_id    (admin_id),
    INDEX       idx_refresh_expires_at  (expires_at),
    INDEX       idx_refresh_is_deleted  (is_deleted),
    CONSTRAINT fk_refresh_token_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_admin FOREIGN KEY (admin_id) REFERENCES admins (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Refresh Token 테이블';

CREATE TABLE email_verifications (
    id          BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    email       VARCHAR(100)        NOT NULL                    COMMENT '인증 대상 이메일',
    token       VARCHAR(255)        NOT NULL                    COMMENT '인증 토큰',
    type        VARCHAR(50)         NOT NULL                    COMMENT '토큰 타입 (EMAIL_VERIFICATION, PASSWORD_RESET)',
    expires_at  TIMESTAMP(6)        NOT NULL                    COMMENT '만료 일시',
    verified_at TIMESTAMP(6)        NULL                        COMMENT '인증 완료 일시',
    is_deleted  BOOLEAN             NOT NULL    DEFAULT FALSE   COMMENT '삭제 여부',
    deleted_at  TIMESTAMP(6)        NULL                        COMMENT '삭제 일시',
    deleted_by  BIGINT UNSIGNED     NULL                        COMMENT '삭제한 사용자 ID',
    created_at  TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
    created_by  BIGINT UNSIGNED     NULL                        COMMENT '생성한 사용자 ID',
    updated_at  TIMESTAMP(6)        NULL                        COMMENT '수정 일시',
    updated_by  BIGINT UNSIGNED     NULL                        COMMENT '수정한 사용자 ID',
    UNIQUE  KEY uk_email_verification_token     (token),
    INDEX       idx_email_verification_email    (email),
    INDEX       idx_email_verification_type     (email, type),
    INDEX       idx_email_verification_expires  (expires_at),
    INDEX       idx_email_verification_deleted  (is_deleted)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='이메일 인증 테이블';

CREATE TABLE user_history (
    history_id      BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    user_id         BIGINT UNSIGNED     NOT NULL                    COMMENT '사용자 ID',
    operation_type  VARCHAR(20)         NOT NULL                    COMMENT '작업 타입 (INSERT, UPDATE, DELETE)',
    before_data     JSON                NULL                        COMMENT '변경 전 데이터',
    after_data      JSON                NULL                        COMMENT '변경 후 데이터',
    changed_by      BIGINT UNSIGNED     NULL                        COMMENT '변경한 사용자 ID',
    changed_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason   VARCHAR(500)        NULL                        COMMENT '변경 사유',
    INDEX       idx_user_history_user_id    (user_id),
    INDEX       idx_user_history_changed_at (changed_at),
    INDEX       idx_user_history_operation  (operation_type, changed_at),
    CONSTRAINT fk_user_history_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자 변경 이력 테이블';

CREATE TABLE admin_history (
    history_id      BIGINT UNSIGNED     NOT NULL    PRIMARY KEY     COMMENT 'TSID',
    admin_id        BIGINT UNSIGNED     NOT NULL                    COMMENT '관리자 ID',
    operation_type  VARCHAR(20)         NOT NULL                    COMMENT '작업 타입 (INSERT, UPDATE, DELETE)',
    before_data     JSON                NULL                        COMMENT '변경 전 데이터',
    after_data      JSON                NULL                        COMMENT '변경 후 데이터',
    changed_by      BIGINT UNSIGNED     NULL                        COMMENT '변경한 관리자 ID',
    changed_at      TIMESTAMP(6)        NOT NULL    DEFAULT CURRENT_TIMESTAMP(6) COMMENT '변경 일시',
    change_reason   VARCHAR(500)        NULL                        COMMENT '변경 사유',
    INDEX       idx_admin_history_admin_id   (admin_id),
    INDEX       idx_admin_history_changed_at (changed_at),
    INDEX       idx_admin_history_operation  (operation_type, changed_at),
    CONSTRAINT fk_admin_history_admin FOREIGN KEY (admin_id) REFERENCES admins (id) ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='관리자 변경 이력 테이블';
