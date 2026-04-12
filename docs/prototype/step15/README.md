# Step 15: Sources 동기화 Batch Job 구현

## Plan Task

```
plan task: Sources 동기화 Batch Job 구현 - json/sources.json 데이터를 MongoDB Atlas sources 컬렉션으로 동기화
```

## 개요

json/sources.json 파일에 정의된 모든 Source 데이터를 MongoDB Atlas Cluster의 `sources` 컬렉션으로 동기화하는 Spring Batch Job을 구현합니다.

## 작업 목표

- JSON 파일 읽기 → DTO 변환 → MongoDB UPSERT (name 기준)
- sources.sync.job 실행 시 약 20건의 Source 데이터가 MongoDB에 저장됨

## 주요 특징

- 기존 batch/source 모듈의 Job 패턴 준수
- domain/mongodb 모듈의 SourcesDocument 재사용
- MongoTemplate 직접 사용 (내부 API 호출 금지, Feign 클라이언트 사용 금지)
- UPSERT 전략: name 필드 기준으로 존재하면 UPDATE, 없으면 INSERT

## 관련 설계서

- `sources-sync-batch-job-design.md`: Sources 동기화 배치 잡 설계서

## 의존성

- 4단계: Domain 모듈 구현 완료 필수 - SourcesDocument, MongoTemplate

## 다음 단계

- 16단계 (Batch 모듈 및 Jenkins 연동 구현)
