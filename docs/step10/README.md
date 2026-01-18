# Step 10: 배치 잡 통합 및 내부 API 호출 구현

## 개요

이 단계는 배치 잡 통합 및 내부 API 호출을 구현합니다. Spring Batch를 활용하여 외부 데이터를 수집하고 내부 API를 호출합니다.

## 관련 설계서

### 생성 설계서
- `batch-job-integration-design.md`: 배치 잡 통합 및 내부 API 호출 설계 문서

### 참조 문서
- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
- `../step9/contest-news-api-design.md`: Contest/News API 설계
- `../step8/rss-scraper-modules-analysis.md`: RSS/Scraper 모듈 분석
- `../step2/1. api-endpoint-design.md`: API 엔드포인트 설계
- `../step2/2. data-model-design.md`: 데이터 모델 설계

## 주요 내용

- Spring Batch Job 구성
- 외부 API 데이터 수집
- 내부 API 호출 (Feign Client 활용)
- 배치 작업 스케줄링

## 의존성

- 8단계: Client 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수
- 9단계: Contest 및 News API 모듈 구현 완료 필수
