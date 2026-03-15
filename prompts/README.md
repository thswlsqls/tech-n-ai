# 프롬프트 문서

이 폴더는 프로젝트의 단계별 프롬프트를 포함합니다.

## 메인 프롬프트

- `shrimp-task-prompt.md`: 전체 프로젝트 단계별 실행 가이드 (모든 단계의 plan task 포함)

## 단계별 프롬프트

### Step 1~4
- 메인 프롬프트의 해당 섹션 참고

### Step 5: 사용자 인증 및 관리 시스템 구현
- `api-auth-verification-prompt.md`: API 인증 검증 프롬프트

### Step 6: OAuth Provider별 로그인 기능 구현
- `oauth-provider-implementation-prompt.md`: OAuth Provider 구현 프롬프트
- `oauth-feign-client-migration-prompt.md`: OpenFeign 클라이언트 전환 프롬프트
- `oauth-http-client-selection-prompt.md`: HTTP Client 선택 프롬프트
- `oauth-state-storage-research-prompt.md`: State 파라미터 저장 연구 프롬프트
- `spring-security-auth-design-prompt.md`: Spring Security 설계 프롬프트

### Step 7: Redis 최적화 구현
- `redis-optimization-best-practices-prompt.md`: Redis 최적화 모범 사례 프롬프트

### Step 8: Client 모듈 구현
- `rss-modules-analysis-improvement-prompt.md`: RSS 모듈 분석 개선 프롬프트
- `rss-scraper-modules-analysis-improvement-prompt.md`: RSS/Scraper 모듈 분석 개선 프롬프트
- `scraper-modules-analysis-improvement-prompt.md`: Scraper 모듈 분석 개선 프롬프트
- `slack-integration-design-prompt.md`: Slack 연동 설계 프롬프트

### Step 9~18
- 메인 프롬프트의 해당 섹션 참고
- `step11-cqrs-sync-design-prompt.md`, `rag-chatbot-design-prompt.md` 등 개별 프롬프트 존재

## reference/ 문서 구조

### design/ — 설계 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [API Agent 모듈 분리](reference/design/001-api-agent-module-separation.md) | api-agent 모듈 분리 프롬프트 |
| 002 | [프론트엔드 웹앱](reference/design/002-frontend-webapp.md) | 프론트엔드 웹앱 설계 프롬프트 |
| 003 | [History 서비스 리팩토링](reference/design/003-history-service-layer-refactoring.md) | History 서비스 레이어 리팩토링 |
| 004 | [MongoDB Vector Search](reference/design/004-mongodb-vector-search.md) | MongoDB Atlas Vector Search 설계 |
| 005 | [Sources 동기화 Step2](reference/design/005-sources-sync-step2.md) | Sources 동기화 2단계 설계 |
| 006 | [사용자 탈퇴 API](reference/design/006-user-withdrawal-api.md) | 사용자 탈퇴 API 설계 |
| 007 | [DataSource 제거](reference/design/007-data-source-removal.md) | 불필요 DataSource 제거 |
| 008 | [내부 API 응답 로깅](reference/design/008-internal-api-response-logging.md) | 내부 API 응답 로깅 설계 |
| 009 | [관리자 역할 기반 인증](reference/design/009-admin-role-based-auth.md) | 관리자 인증/인가 설계 |
| 010 | [북마크 Emerging Tech 재설계](reference/design/010-bookmark-emerging-tech-redesign.md) | 북마크-EmergingTech 연동 재설계 |
| 011 | [Chatbot RAG 검색 개선](reference/design/011-chatbot-rag-redesign.md) | RAG 검색 대상 개선 |
| 012 | [하이브리드 검색 Score Fusion](reference/design/012-chatbot-hybrid-search-score-fusion.md) | 벡터 검색 + 최신성 정렬 결합 |
| 013 | [세션 타이틀 자동생성](reference/design/013-chatbot-session-title-generation.md) | 비동기 LLM 기반 타이틀 생성 |
| 014 | [북마크 태그 다중값](reference/design/014-bookmark-tag-multi-value.md) | 북마크 태그 다중값 지원 |
| 015 | [관리자 인증 보안 강화](reference/design/015-admin-auth-security-improvement.md) | 관리자 인증 보안 개선 |
| 016 | [API Gateway 개선](reference/design/016-api-gateway-improvement.md) | Gateway 회복탄력성·보안 개선 |
| 017 | [Common Conversation 모듈](reference/design/017-common-conversation-module.md) | 공통 대화 이력 모듈 설계 |

### guide/ — 환경 설정 가이드 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [Kafka Docker 설정](reference/guide/001-kafka-docker-setup.md) | Kafka Docker 환경 설정 |
| 002 | [Redis 설정 개선](reference/guide/002-local-redis-setup-enhancement.md) | 로컬 Redis 설정 개선 |
| 003 | [MySQL Docker Compose](reference/guide/003-mysql-docker-compose-setup.md) | MySQL Docker Compose 설정 |

### research/ — 리서치 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [API Gateway 패턴 리서치](reference/research/001-api-gateway-pattern-research.md) | API Gateway 패턴 비교 분석 |

### migration/ — 마이그레이션 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [Spring Cloud 2025 마이그레이션](reference/migration/001-spring-cloud-2025-migration.md) | Spring Cloud 2025 마이그레이션 |
| 002 | [Spring Cloud 2025 Shrimp Task](reference/migration/002-spring-cloud-2025-shrimp-task.md) | Shrimp Task 기반 마이그레이션 |

### api-specifications/ — API 스펙 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 000 | [API 스펙 작성 가이드](reference/api-specifications/000-overview.md) | API 명세서 작성 프롬프트 |
| 001 | [Agent API](reference/api-specifications/001-api-agent.md) | Agent API 스펙 프롬프트 |
| 002 | [Auth API](reference/api-specifications/002-api-auth.md) | Auth API 스펙 프롬프트 |
| 003 | [Bookmark API](reference/api-specifications/003-api-bookmark.md) | Bookmark API 스펙 프롬프트 |
| 004 | [Chatbot API](reference/api-specifications/004-api-chatbot.md) | Chatbot API 스펙 프롬프트 |
| 005 | [Emerging Tech API](reference/api-specifications/005-api-emerging-tech.md) | Emerging Tech API 스펙 프롬프트 |

### agent-pipeline/ — AI Agent 파이프라인 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [데이터 수집 파이프라인](reference/agent-pipeline/001-data-pipeline.md) | GitHub Release, RSS, Scraping 데이터 수집 |
| 002 | [LangChain4j Tools](reference/agent-pipeline/002-langchain4j-tools.md) | Tool 인터페이스 설계 |
| 003 | [OpenAI 전환 (Phase 2)](reference/agent-pipeline/003-modification-to-openai-phase2.md) | Phase 2 OpenAI 전환 |
| 004 | [Agent 통합](reference/agent-pipeline/004-agent-integration.md) | Agent 통합 설계 |
| 005 | [OpenAI 전환 (Phase 3)](reference/agent-pipeline/005-modification-to-openai-phase3.md) | Phase 3 OpenAI 전환 |
| 006 | [데이터 분석 Tool 전환](reference/agent-pipeline/006-analytics-tool-redesign.md) | 통계/키워드 분석 Tool |
| 007 | [데이터 수집 Agent 개선](reference/agent-pipeline/007-data-collection-agent-improvement.md) | 수집 Agent 개선 |
| 008 | [워드클라우드 필터 개선](reference/agent-pipeline/008-word-cloud-filter-improvement.md) | 워드클라우드 필터 개선 |
| 009 | [Agent Query Tool 개선](reference/agent-pipeline/009-agent-query-tool-improvement.md) | 목록/상세 조회 Tool |
| 010 | [미지원 요청 처리](reference/agent-pipeline/010-unsupported-request-handling.md) | 미지원 대상 안내 |

### writing/ — 글쓰기 프롬프트

| # | 문서 | 설명 |
|---|------|------|
| 001 | [글쓰기 프롬프트](reference/writing/001-writing-prompt.md) | 블로그 글 작성용 프롬프트 |
