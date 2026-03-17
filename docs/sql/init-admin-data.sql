-- =====================================================================
-- Aurora MySQL 초기 데이터 INSERT DML
-- =====================================================================
-- 작성 일시: 2026-03-05
-- 대상: auth 스키마 - 슈퍼 관리자 계정 및 생성 이력
-- 비밀번호: Admin1234! (BCrypt encoded, strength=12)
--
-- 실행 순서:
-- 1. create.sql 실행 후 본 스크립트 실행
-- 2. admins 테이블 UPSERT → admin_history 테이블 UPSERT
--
-- 참고:
-- - 멱등성 보장: INSERT ... ON DUPLICATE KEY UPDATE 사용
-- - 재실행 시 비밀번호, 잠금 상태, 로그인 실패 횟수 초기화
-- =====================================================================

USE auth;

-- =====================================================================
-- 1. 슈퍼 관리자 계정 (UPSERT)
-- =====================================================================

INSERT INTO admins (
    id,
    email,
    username,
    password,
    role,
    is_active,
    is_deleted,
    failed_login_attempts,
    account_locked_until,
    last_login_at,
    created_at,
    created_by,
    updated_at,
    updated_by
) VALUES (
    817225402408821626,
    'thsdmsqlsspdlqj@naver.com',
    'superadmin',
    '$2a$12$4VZ3MJ/gjyQeatMpEeiL3OQ2ZetwYFJIR0zXe1q582333WUkunHVS',
    'ADMIN',
    TRUE,
    FALSE,
    0,
    NULL,
    NULL,
    CURRENT_TIMESTAMP(6),
    NULL,
    CURRENT_TIMESTAMP(6),
    NULL
) ON DUPLICATE KEY UPDATE
    password              = VALUES(password),
    is_active             = TRUE,
    is_deleted            = FALSE,
    failed_login_attempts = 0,
    account_locked_until  = NULL,
    updated_at            = CURRENT_TIMESTAMP(6);

-- =====================================================================
-- 2. 슈퍼 관리자 생성 이력 (UPSERT)
-- =====================================================================

INSERT INTO admin_history (
    history_id,
    admin_id,
    operation_type,
    before_data,
    after_data,
    changed_by,
    changed_at,
    change_reason
) VALUES (
    817225402429793147,
    817225402408821626,
    'CREATE',
    NULL,
    JSON_OBJECT(
        'email', 'thsdmsqlsspdlqj@naver.com',
        'username', 'superadmin',
        'role', 'ADMIN',
        'isActive', TRUE
    ),
    NULL,
    CURRENT_TIMESTAMP(6),
    '시스템 초기 슈퍼 관리자 계정 생성'
) ON DUPLICATE KEY UPDATE
    changed_at = CURRENT_TIMESTAMP(6);
