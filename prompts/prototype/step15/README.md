# Step 15: Sources 동기화 Batch Job 구현

## Plan Task

```
plan task: Sources 동기화 Batch Job 구현 - json/sources.json 데이터를 MongoDB Atlas sources 컬렉션으로 동기화
```

## 개요

json/sources.json 파일에 정의된 모든 Source 데이터를 MongoDB Atlas Cluster의 `sources` 컬렉션으로 동기화하는 Spring Batch Job을 구현합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 15단계 섹션 참고
- `sources-sync-batch-job-design-prompt.md`: Sources 동기화 배치 잡 설계 프롬프트

### 설계서
- `docs/step15/sources-sync-batch-job-design.md`: Sources 동기화 배치 잡 설계서

## 주요 작업 내용

- JSON 파일 읽기 → DTO 변환 → MongoDB UPSERT (name 기준)
- 기존 batch/source 모듈의 Job 패턴 준수
- domain/mongodb 모듈의 SourcesDocument 재사용
- MongoTemplate 직접 사용

## 의존성

- 4단계: Domain 모듈 구현 완료 필수 - SourcesDocument, MongoTemplate

## 다음 단계

- 16단계 (Batch 모듈 및 Jenkins 연동 구현)
