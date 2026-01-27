# 프롬프트 문서

이 폴더는 프로젝트의 단계별 프롬프트를 포함합니다.

## 메인 프롬프트

- `shrimp-task-prompt.md`: 전체 프로젝트 단계별 실행 가이드 (모든 단계의 plan task 포함)

## 단계별 프롬프트

### Step 1: 프로젝트 구조 및 설계서 생성
- (프롬프트 없음 - 메인 프롬프트의 1단계 섹션 참고)

### Step 2: API 설계 및 데이터 모델링
- (프롬프트 없음 - 메인 프롬프트의 2단계 섹션 참고)

### Step 3: Common 모듈 구현
- (프롬프트 없음 - 메인 프롬프트의 3단계 섹션 참고)

### Step 4: Domain 모듈 구현
- (프롬프트 없음 - 메인 프롬프트의 4단계 섹션 참고)

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

### Step 9: Contest 및 News API 모듈 구현
- `contest-news-api-design-prompt.md`: Contest 및 News API 설계 프롬프트

### Step 10: 배치 잡 통합 및 내부 API 호출 구현
- `batch-job-integration-design-prompt.md`: 배치 잡 통합 설계 프롬프트

### Step 11: CQRS 패턴 구현 (Kafka 동기화)
- `step11-cqrs-sync-design-prompt.md`: CQRS 동기화 설계서 작성 프롬프트
- `ai-integration-analysis-prompt.md`: AI 통합 분석 프롬프트
- `source-discovery-prompt.md`: 출처 탐색 프롬프트

### Step 12: langchain4j를 활용한 RAG 기반 챗봇 구현
- `rag-chatbot-design-prompt.md`: RAG 챗봇 설계서 작성 프롬프트
- `rag-chatbot-multiturn-history-prompt.md`: 멀티턴 대화 히스토리 관리 설계 항목 추가 프롬프트

### Step 13: 사용자 아카이브 기능 구현
- `user-archive-feature-design-prompt.md`: 사용자 아카이브 기능 구현 설계서 작성 프롬프트

### Step 14: API Gateway 서버 구현
- `gateway-design-prompt.md`: API Gateway 설계 프롬프트

### Step 15: Sources 동기화 Batch Job 구현
- `sources-sync-batch-job-design-prompt.md`: Sources 동기화 배치 잡 설계 프롬프트

### Step 16: 이메일 인증 기능 구현 (api/auth 모듈)
- `step19/email-verification-design-prompt.md`: 이메일 인증 기능 설계서 작성 프롬프트

### Step 17: Batch 모듈 및 Jenkins 연동 구현
- (프롬프트 없음 - 메인 프롬프트의 17단계 섹션 참고)

### Step 18: 테스트 및 Spring REST Docs 기반 API 문서화
- (프롬프트 없음 - 메인 프롬프트의 18단계 섹션 참고)

## 사용 방법

각 단계별 프롬프트는 해당 단계의 설계서 작성 또는 구현 가이드로 사용됩니다. 메인 프롬프트(`shrimp-task-prompt.md`)의 해당 단계 섹션과 함께 참고하세요.
