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

## RAG 챗봇 개선 설계서

api-chatbot 모듈의 RAG 파이프라인 개선 관련 설계 문서입니다.

| 문서 | 설명 |
|------|------|
| [Emerging Tech 전용 RAG 검색 개선](reference/api-chatbot/1-emerging-tech-rag-redesign.md) | 벡터 검색 대상을 `emerging_techs` 컬렉션 전용으로 개선 |
| [하이브리드 검색 Score Fusion 설계](reference/api-chatbot/2-hybrid-search-score-fusion-design.md) | 벡터 검색 + 최신성 정렬 결합 (Score Fusion + RRF) |
| [세션 타이틀 자동생성 설계](reference/api-chatbot/3-session-title-generation-design.md) | 비동기 LLM 호출 기반 세션 타이틀 자동 생성 |

## API 명세서

각 API 모듈별 상세 명세 문서입니다.

| 모듈 | 문서 | 설명 |
|------|------|------|
| 통합 | [API 통합 명세서](reference/API-SPECIFICATIONS/API-SPECIFICATION.md) | 전체 API 라우팅 규칙, 공통 응답 형식 |
| Agent | [Agent API 명세서](reference/API-SPECIFICATIONS/api-agent-specification.md) | ADMIN 역할 전용 AI Agent 실행 API |
| Auth | [Auth API 명세서](reference/API-SPECIFICATIONS/api-auth-specification.md) | OAuth 2.0, JWT 인증 API |
| Bookmark | [Bookmark API 명세서](reference/API-SPECIFICATIONS/api-bookmark-specification.md) | 사용자 북마크 CRUD, 히스토리 관리 API |
| Chatbot | [Chatbot API 명세서](reference/API-SPECIFICATIONS/api-chatbot-specification.md) | RAG 챗봇, 세션 관리, 타이틀 수정 API |
| Emerging Tech | [Emerging Tech API 명세서](reference/API-SPECIFICATIONS/api-emerging-tech-specification.md) | AI 업데이트 조회/관리 API (공개/내부) |

## AI Agent 자동화 파이프라인 설계서

LangChain4j 기반 AI Agent 시스템의 단계별 설계 및 구현 문서입니다.

### 설계 문서 목록

| Phase | 문서명 | 설명 |
|-------|--------|------|
| Phase 1 | [데이터 수집 파이프라인 설계서](reference/automation-pipeline-to-ai-agent/phase1-data-pipeline-design.md) | GitHub Release, RSS, Web Scraping 기반 데이터 수집 |
| Phase 2 | [LangChain4j Tools 설계서](reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md) | Tool 인터페이스 및 Adapter 패턴 설계 |
| Phase 3 | [AI Agent 통합 설계서](reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md) | OpenAI GPT-4o-mini 기반 Agent 통합 |
| Phase 4 | [데이터 분석 기능 전환 설계서](reference/automation-pipeline-to-ai-agent/phase4-analytics-tool-redesign-design.md) | MongoDB Aggregation 기반 통계/키워드 분석 Tool |
| Phase 5 | [데이터 수집 Agent 설계서](reference/automation-pipeline-to-ai-agent/phase5-data-collection-agent-design.md) | 자율 데이터 수집 Tool (GitHub, RSS, Scraping) |
| Phase 6 | [Agent Query Tool 개선 설계서](reference/automation-pipeline-to-ai-agent/phase6-agent-query-tool-improvement-design.md) | 목록/상세 조회 Tool, Slack Mock 전환 |
| Phase 7 | [미지원 요청 처리 설계서](reference/automation-pipeline-to-ai-agent/phase7-unsupported-request-handling-design.md) | System Prompt 기반 미지원 대상 안내 |

### Agent Tool 구성

| Tool | 기능 | 카테고리 |
|------|------|---------|
| `fetch_github_releases` | GitHub 릴리스 조회 | 조회 |
| `scrape_web_page` | 웹 페이지 크롤링 | 조회 |
| `search_emerging_techs` | 키워드 기반 검색 | 조회 |
| `list_emerging_techs` | 필터 기반 목록 조회 | 조회 |
| `get_emerging_tech_detail` | ID 기반 상세 조회 | 조회 |
| `get_emerging_tech_statistics` | Provider/SourceType/UpdateType별 통계 | 분석 |
| `analyze_text_frequency` | 키워드 빈도 분석 | 분석 |
| `collect_github_releases` | GitHub 릴리스 수집+저장 | 수집 |
| `collect_rss_feeds` | RSS 수집+저장 | 수집 |
| `collect_scraped_articles` | 크롤링 수집+저장 | 수집 |
| `send_slack_notification` | Slack 알림 (Mock 지원) | 알림 |

### 지원 Provider

| Provider | 지원 여부 | 대상 서비스 |
|----------|----------|------------|
| OPENAI | O | GPT, ChatGPT, DALL-E, Whisper |
| ANTHROPIC | O | Claude |
| GOOGLE | O | Gemini, PaLM, Bard |
| META | O | LLaMA, Code Llama |
| XAI | O | Grok |

### 테스트 결과

Agent API 테스트 결과 문서: [tests/](reference/automation-pipeline-to-ai-agent/tests/)

| 테스트 카테고리 | 문서 | 결과 |
|----------------|------|------|
| Agent 기본 실행 | [01-agent-run-test-results.md](reference/automation-pipeline-to-ai-agent/tests/01-agent-run-test-results.md) | Pass |
| 통계/분석 기능 | [02-agent-analytics-test-results.md](reference/automation-pipeline-to-ai-agent/tests/02-agent-analytics-test-results.md) | Pass |
| 데이터 수집 | [03-agent-data-collection-test-results.md](reference/automation-pipeline-to-ai-agent/tests/03-agent-data-collection-test-results.md) | Pass |
| 목록/상세 조회 | [04-agent-query-tools-test-results.md](reference/automation-pipeline-to-ai-agent/tests/04-agent-query-tools-test-results.md) | Pass |


## 개발 환경 가이드

- [로컬 개발 환경 설정 가이드](./local-development-setup-guide.md): Kafka, Redis, 애플리케이션 실행 절차
- [Kafka Docker 로컬 설정 가이드](./kafka-docker-local-setup-guide.md): Kafka 상세 설정 및 트러블슈팅

## 기타 참고 문서

- [북마크 Emerging Tech 재설계](reference/bookmark-emerging-tech-redesign.md)
- [북마크 태그 다중값 설계](reference/bookmark-tag-multi-value-design.md)
- [프론트엔드 웹앱 설계](reference/FRONTEND-WEBAPP-DESIGN.md)
- [관리자 역할 기반 인증 설계](reference/admin-role-based-auth-design.md)
- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
