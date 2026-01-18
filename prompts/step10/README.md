# Step 10: 배치 잡 통합 및 내부 API 호출 구현

## 개요

이 단계는 배치 잡 통합 및 내부 API 호출을 구현합니다. Spring Batch를 활용하여 외부 데이터를 수집하고 내부 API를 호출합니다.

## 관련 파일

### 프롬프트
- `batch-job-integration-design-prompt.md`: 배치 잡 통합 설계 프롬프트

### 설계서
- `docs/step10/batch-job-integration-design.md`: 배치 잡 통합 설계서

## 주요 내용

- Spring Batch Job 구성
- 외부 API 데이터 수집
- 내부 API 호출 (Feign Client 활용)
- 배치 작업 스케줄링

## 의존성

- 8단계: Client 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수
