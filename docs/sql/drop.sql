-- =====================================================================
-- Aurora MySQL Schema & Table DROP DDL
-- =====================================================================
-- 작성 일시: 2026-02-22
-- 대상: CQRS 패턴의 Command Side (쓰기 전용)
-- 소스: datasource/aurora 모듈 JPA Entity 기반
--
-- 주의: 이 스크립트는 모든 데이터를 영구 삭제합니다.
-- 실행 순서: FK 의존성의 역순으로 삭제합니다.
-- =====================================================================


-- =====================================================================
-- 1. chatbot 스키마 테이블 삭제
-- =====================================================================

-- conversation_messages 먼저 삭제 (conversation_sessions FK 참조)
DROP TABLE IF EXISTS chatbot.conversation_messages;
DROP TABLE IF EXISTS chatbot.conversation_sessions;


-- =====================================================================
-- 2. bookmark 스키마 테이블 삭제
-- =====================================================================

-- bookmark_history 먼저 삭제 (bookmarks FK 참조)
DROP TABLE IF EXISTS bookmark.bookmark_history;
DROP TABLE IF EXISTS bookmark.bookmarks;


-- =====================================================================
-- 3. auth 스키마 테이블 삭제
-- =====================================================================

-- FK 참조 순서: history -> refresh_tokens -> users/admins -> providers
DROP TABLE IF EXISTS auth.admin_history;
DROP TABLE IF EXISTS auth.user_history;
DROP TABLE IF EXISTS auth.email_verifications;
DROP TABLE IF EXISTS auth.refresh_tokens;
DROP TABLE IF EXISTS auth.users;
DROP TABLE IF EXISTS auth.admins;
DROP TABLE IF EXISTS auth.providers;


-- =====================================================================
-- 4. 스키마(데이터베이스) 삭제
-- =====================================================================
-- 주의: 스키마 삭제는 해당 스키마 내 모든 객체를 영구 삭제합니다.
-- 필요한 경우에만 아래 주석을 해제하여 실행하세요.
-- =====================================================================

-- DROP DATABASE IF EXISTS chatbot;
-- DROP DATABASE IF EXISTS bookmark;
-- DROP DATABASE IF EXISTS auth;


-- =====================================================================
-- DROP DDL 실행 완료
-- =====================================================================
--
-- 삭제된 테이블: 12개
--   chatbot (2): conversation_messages, conversation_sessions
--   bookmark (2): bookmark_history, bookmarks
--   auth (7): admin_history, user_history, email_verifications,
--             refresh_tokens, users, admins, providers
--
-- =====================================================================
