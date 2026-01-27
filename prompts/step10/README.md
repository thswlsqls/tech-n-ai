# Step 10: 배치 잡 통합 및 내부 API 호출 구현

## Plan Task

```
plan task: 배치 잡 통합 및 내부 API 호출 구현 (Spring Batch 기반 데이터 수집 파이프라인)
```

## 개요

`batch-source` 모듈의 배치 잡 통합 구현 및 `client-feign` 모듈의 내부 API 호출 Feign Client 구현.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 10단계 섹션 참고
- `batch-job-integration-design-prompt.md`: 배치 잡 통합 설계 프롬프트

### 설계서
- `docs/step10/batch-job-integration-design.md`: 배치 잡 통합 설계서

## 주요 작업 내용

- `client-feign` 모듈: 내부 API 호출 Feign Client 구현
- `batch-source` 모듈: 모든 클라이언트 모듈에 대한 JobConfig 추가
- PagingItemReader, Item Processor, Item Writer 구현
- DTO 변환 흐름 구현

## 의존성

- 8단계: Client 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수

## 다음 단계

- 11단계 (CQRS 패턴 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)
