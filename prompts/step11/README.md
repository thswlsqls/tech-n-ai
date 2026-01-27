# Step 11: CQRS 패턴 구현 (Kafka 동기화)

## Plan Task

```
plan task: CQRS 패턴 기반 Kafka 동기화 서비스 구현 - User 및 Archive 엔티티 MongoDB Atlas 동기화
```

## 개요

CQRS 패턴의 Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 간 실시간 동기화를 위한 Kafka 기반 이벤트 동기화 시스템을 구현합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 11단계 섹션 참고
- `ai-integration-analysis-prompt.md`: AI 통합 분석 프롬프트
- `step11-cqrs-sync-design-prompt.md`: CQRS 동기화 설계 프롬프트
- `source-discovery-prompt.md`: 소스 발견 프롬프트

### 설계서
- `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계서
- `docs/step11/ai-integration-analysis.md`: AI 통합 분석

## 주요 작업 내용

- `EventConsumer.processEvent` 메서드 구현
- `UserSyncService` 및 `ArchiveSyncService` 동기화 서비스 구현
- Kafka 이벤트를 통한 Aurora MySQL → MongoDB Atlas 실시간 동기화

## 의존성

- 2단계: API 설계 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 8단계: Client 모듈 구현 완료 필수
- 10단계: Batch 모듈 구현 완료 필수

## 다음 단계

- 12단계 (사용자 아카이브 기능 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (REST API 컨트롤러 및 비즈니스 로직 구현)
