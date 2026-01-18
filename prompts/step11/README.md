# Step 11: CQRS 패턴 구현 (Kafka 동기화)

## 개요

이 단계는 CQRS 패턴 기반 아키텍처를 구현하고, Kafka를 통한 Amazon Aurora MySQL과 MongoDB Atlas 동기화를 수행합니다.

## 관련 파일

### 프롬프트
- `step11-cqrs-sync-design-prompt.md`: CQRS 동기화 설계서 작성 프롬프트
- `ai-integration-analysis-prompt.md`: AI 통합 분석 프롬프트
- `source-discovery-prompt.md`: 출처 탐색 프롬프트

### 설계서
- `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계서
- `docs/step11/ai-integration-analysis.md`: AI 통합 분석 문서

## 주요 내용

- CQRS 패턴 구현
- Kafka 이벤트 발행 및 구독
- Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 동기화
- EventPublisher 및 EventConsumer 구현
- 동기화 서비스 구현

## 의존성

- 4단계: Domain 모듈 구현 완료 필수
- 8단계: Client 모듈 구현 완료 필수
