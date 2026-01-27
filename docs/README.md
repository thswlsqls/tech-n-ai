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


## 개발 환경 가이드

- [로컬 개발 환경 설정 가이드](./local-development-setup-guide.md): Kafka, Redis, 애플리케이션 실행 절차
- [Kafka Docker 로컬 설정 가이드](./kafka-docker-local-setup-guide.md): Kafka 상세 설정 및 트러블슈팅

## 참고 문서

- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
