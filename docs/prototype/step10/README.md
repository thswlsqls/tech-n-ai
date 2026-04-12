# Step 10: 배치 잡 통합 및 내부 API 호출 구현

## Plan Task

```
plan task: 배치 잡 통합 및 내부 API 호출 구현 (Spring Batch 기반 데이터 수집 파이프라인)
```

## 개요

`batch-source` 모듈의 배치 잡 통합 구현 (모든 클라이언트 모듈에 대한 JobConfig 추가). `client-feign` 모듈의 내부 API 호출 Feign Client 구현. Client 모듈 → Batch 모듈 → API 모듈 → MongoDB Atlas 데이터 흐름 구현.

## 작업 목표

- `client-feign` 모듈: ContestInternalContract, NewsInternalContract 및 Feign Client 구현 완료
- `batch-source` 모듈: 모든 클라이언트 모듈에 대한 JobConfig 추가 완료
- PagingItemReader, Item Processor, Item Writer 구현 완료
- DTO 변환 흐름 구현: Client DTO → batch-source DTO → client-feign DTO → api-contest/api-news DTO

## 관련 설계서

- `batch-job-integration-design.md`: 배치 잡 통합 설계서

## 의존성

- 8단계: Client 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수

## 다음 단계

- 11단계 (CQRS 패턴 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)
