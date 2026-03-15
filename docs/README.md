# 설계서 문서

이 폴더는 프로젝트의 단계별 설계서를 포함합니다.

## 단계별 설계서

### Step 1: 프로젝트 구조 및 설계서 생성
- 멀티모듈 프로젝트 구조 검증
- MongoDB 스키마 설계
- Aurora MySQL 스키마 설계

### Step 2: API 설계 및 데이터 모델링
- API 엔드포인트 설계
- 데이터 모델 설계
- API 응답 형식 설계
- 에러 처리 전략 설계

### Step 3: Common 모듈 구현
- (설계서 없음 - 구현 단계)

### Step 4: Domain 모듈 구현
- (설계서 없음 - 구현 단계)

### Step 5: 사용자 인증 및 관리 시스템 구현
- (설계서 없음 - 구현 단계)

### Step 6: OAuth Provider별 로그인 기능 구현
- OAuth Provider 구현 가이드
- OpenFeign 클라이언트 전환 분석
- HTTP Client 선택 분석
- State 파라미터 저장 연구 결과
- Spring Security 설계 가이드

### Step 7: Redis 최적화 구현
- Redis 최적화 모범 사례

### Step 8: Client 모듈 구현
- RSS/Scraper 모듈 분석
- Slack 연동 설계 가이드

### Step 9: Contest 및 News API 모듈 구현
- Contest 및 News API 설계서

### Step 10: 배치 잡 통합 및 내부 API 호출 구현
- 배치 잡 통합 설계서

### Step 11: CQRS 패턴 구현 (Kafka 동기화)
- CQRS Kafka 동기화 설계서
- AI 통합 분석 문서

### Step 12: langchain4j를 활용한 RAG 기반 챗봇 구현
- RAG 챗봇 설계서

### Step 13: 사용자 아카이브 기능 구현
- 사용자 아카이브 기능 구현 설계서

### Step 14: API Gateway 서버 구현
- API Gateway 설계서
- Gateway 구현 계획

### Step 15: Sources 동기화 Batch Job 구현
- Sources 동기화 배치 잡 설계서

### Step 16: 이메일 인증 기능 구현 (api/auth 모듈)
- 이메일 인증 기능 구현 설계서 (step19/email-verification-implementation-design.md)

### Step 17: 테스트 및 Spring REST Docs 기반 API 문서화
- MongoDB Atlas Vector Search 구현 가이드 (step18/)

## reference/ 문서 구조

### design/ — 모듈/기능 설계 문서

| # | 문서 | 설명 |
|---|------|------|
| 001 | [프론트엔드 웹앱 설계](reference/design/001-frontend-webapp.md) | 프론트엔드 웹 애플리케이션 설계 |
| 002 | [관리자 역할 기반 인증 설계](reference/design/002-admin-role-based-auth.md) | 관리자 인증/인가 설계 |
| 003 | [북마크 Emerging Tech 재설계](reference/design/003-bookmark-emerging-tech-redesign.md) | 북마크-EmergingTech 연동 재설계 |
| 004 | [Chatbot RAG 검색 개선](reference/design/004-chatbot-rag-redesign.md) | 벡터 검색 대상을 `emerging_techs` 컬렉션 전용으로 개선 |
| 005 | [하이브리드 검색 Score Fusion](reference/design/005-chatbot-hybrid-search-score-fusion.md) | 벡터 검색 + 최신성 정렬 결합 (Score Fusion + RRF) |
| 006 | [세션 타이틀 자동생성](reference/design/006-chatbot-session-title-generation.md) | 비동기 LLM 호출 기반 세션 타이틀 자동 생성 |
| 007 | [북마크 태그 다중값 설계](reference/design/007-bookmark-tag-multi-value.md) | 북마크 태그 다중값 지원 설계 |
| 008 | [API Gateway 개선 설계](reference/design/008-api-gateway-improvement.md) | Gateway 회복탄력성·보안 개선 |
| 009 | [Common Conversation 모듈](reference/design/009-common-conversation-module.md) | 공통 대화 이력 모듈 설계 |

### guide/ — 개발 환경 설정 가이드

| # | 문서 | 설명 |
|---|------|------|
| 001 | [로컬 개발 환경 설정](reference/guide/001-local-development-setup.md) | Kafka, Redis, 애플리케이션 실행 절차 |
| 002 | [Kafka Docker 로컬 설정](reference/guide/002-kafka-docker-local-setup.md) | Kafka 상세 설정 및 트러블슈팅 |
| 003 | [MySQL Docker 로컬 설정](reference/guide/003-mysql-docker-local-setup.md) | MySQL Docker Compose 설정 |

### research/ — 기술 리서치

| # | 문서 | 설명 |
|---|------|------|
| 001 | [RAG 아키텍처 발전사](reference/research/001-rag-architecture-evolution.md) | Naive → Advanced → Self-Corrective → Agentic RAG 발전 분석 |
| 002 | [API Gateway 패턴 리서치](reference/research/002-api-gateway-pattern-research.md) | API Gateway 패턴 비교 분석 |

### api-specifications/ — API 명세서

| # | 문서 | 설명 |
|---|------|------|
| 000 | [API 통합 명세서](reference/api-specifications/000-overview.md) | 전체 API 라우팅 규칙, 공통 응답 형식 |
| 001 | [Agent API](reference/api-specifications/001-api-agent.md) | ADMIN 역할 전용 AI Agent 실행 API |
| 002 | [Auth API](reference/api-specifications/002-api-auth.md) | OAuth 2.0, JWT 인증 API |
| 003 | [Bookmark API](reference/api-specifications/003-api-bookmark.md) | 사용자 북마크 CRUD, 히스토리 관리 API |
| 004 | [Chatbot API](reference/api-specifications/004-api-chatbot.md) | RAG 챗봇, 세션 관리, 타이틀 수정 API |
| 005 | [Emerging Tech API](reference/api-specifications/005-api-emerging-tech.md) | AI 업데이트 조회/관리 API (공개/내부) |

### agent-pipeline/ — AI Agent 자동화 파이프라인

LangChain4j 기반 AI Agent 시스템의 단계별 설계 및 구현 문서입니다.

| # | 문서 | 설명 |
|---|------|------|
| 001 | [데이터 수집 파이프라인](reference/agent-pipeline/001-data-pipeline-design.md) | GitHub Release, RSS, Web Scraping 기반 데이터 수집 |
| 002 | [LangChain4j Tools](reference/agent-pipeline/002-langchain4j-tools-design.md) | Tool 인터페이스 및 Adapter 패턴 설계 |
| 003 | [AI Agent 통합](reference/agent-pipeline/003-agent-integration-design.md) | OpenAI GPT-4o-mini 기반 Agent 통합 |
| 004 | [데이터 분석 기능 전환](reference/agent-pipeline/004-analytics-tool-redesign.md) | MongoDB Aggregation 기반 통계/키워드 분석 Tool |
| 005 | [데이터 수집 Agent](reference/agent-pipeline/005-data-collection-agent.md) | 자율 데이터 수집 Tool (GitHub, RSS, Scraping) |
| 006 | [Agent Query Tool 개선](reference/agent-pipeline/006-agent-query-tool-improvement.md) | 목록/상세 조회 Tool, Slack Mock 전환 |
| 007 | [미지원 요청 처리](reference/agent-pipeline/007-unsupported-request-handling.md) | System Prompt 기반 미지원 대상 안내 |

테스트 결과: [tests/](reference/agent-pipeline/tests/)

| 테스트 카테고리 | 문서 | 결과 |
|----------------|------|------|
| Agent 기본 실행 | [01-agent-run-test-results.md](reference/agent-pipeline/tests/01-agent-run-test-results.md) | Pass |
| 통계/분석 기능 | [02-agent-analytics-test-results.md](reference/agent-pipeline/tests/02-agent-analytics-test-results.md) | Pass |
| 데이터 수집 | [03-agent-data-collection-test-results.md](reference/agent-pipeline/tests/03-agent-data-collection-test-results.md) | Pass |
| 목록/상세 조회 | [04-agent-query-tools-test-results.md](reference/agent-pipeline/tests/04-agent-query-tools-test-results.md) | Pass |

### writings/ — 블로그 글

| # | 문서 | 설명 |
|---|------|------|
| 001 | [프롬프트 모음](reference/writings/001-prompts.md) | 글쓰기 프롬프트 |
| 002 | [Naive RAG](reference/writings/002-rag-naive-rag.md) | RAG 시리즈 1편 — Naive RAG |
| 003 | [Advanced RAG](reference/writings/003-rag-advanced-rag.md) | RAG 시리즈 2편 — Advanced RAG |
| 004 | [Self-Corrective RAG](reference/writings/004-rag-self-corrective-rag.md) | RAG 시리즈 3편 — Self-Corrective RAG |
| 005 | [Ontology-Enhanced RAG](reference/writings/005-rag-ontology-enhanced-rag.md) | RAG 시리즈 4편 — Ontology-Enhanced RAG |
| 006 | [Agentic RAG](reference/writings/006-rag-agentic-rag.md) | RAG 시리즈 5편 — Agentic RAG |

### tools/ — 도구/프롬프트 설정

| # | 문서 | 설명 |
|---|------|------|
| 001 | [Shrimp Task 최종 목표](reference/tools/001-shrimp-task-prompts-final-goal.md) | 최종 프로젝트 목표 및 프롬프트 설정 |
