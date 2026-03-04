-- =====================================================================
-- Spring Batch 6 Metadata - MySQL DROP DDL
-- =====================================================================
-- Spring Batch 버전: 6.0.x (Spring Boot 4.x)
-- 출처: spring-batch-core/org/springframework/batch/core/schema-drop-mysql.sql
-- 참고: https://github.com/spring-projects/spring-batch/blob/main/spring-batch-core/src/main/resources/org/springframework/batch/core/schema-drop-mysql.sql
--
-- Batch 5 vs Batch 6 차이:
--   시퀀스 테이블: BATCH_JOB_SEQ (v5) → BATCH_JOB_INSTANCE_SEQ (v6)
-- =====================================================================

-- FK 의존성 역순으로 삭제
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_CONTEXT;
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_PARAMS;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE;

-- 시퀀스 테이블 삭제
DROP TABLE IF EXISTS BATCH_STEP_EXECUTION_SEQ;
DROP TABLE IF EXISTS BATCH_JOB_EXECUTION_SEQ;
DROP TABLE IF EXISTS BATCH_JOB_INSTANCE_SEQ;
