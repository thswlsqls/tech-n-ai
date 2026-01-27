# Step 8: Client 모듈 구현

## Plan Task

```
plan task: 외부 API 연동 Client 모듈 구현 (Contract 패턴 적용)
```

## 개요

json/sources.json에 정의된 Priority 1 출처와의 API 통합을 위한 Client 모듈 구현. Contract 패턴을 적용하여 Mock/Rest 모드 전환 가능한 구조 구현.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 8단계 섹션 참고
- `rss-modules-analysis-improvement-prompt.md`: RSS 모듈 분석 개선 프롬프트
- `rss-scraper-modules-analysis-improvement-prompt.md`: RSS 스크래퍼 모듈 분석 개선 프롬프트
- `scraper-modules-analysis-improvement-prompt.md`: 스크래퍼 모듈 분석 개선 프롬프트
- `slack-integration-design-prompt.md`: Slack 통합 설계 프롬프트

### 설계서
- `docs/step8/rss-scraper-modules-analysis.md`: RSS 스크래퍼 모듈 분석
- `docs/step8/slack-integration-design-guide.md`: Slack 통합 설계 가이드

## 주요 작업 내용

- client-feign: Priority 1 API 출처와의 통합
- client-rss: 4개 RSS 출처로부터 데이터 수집
- client-scraper: 5개 웹 스크래핑 출처로부터 데이터 수집
- Contract 패턴 적용

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 권장

## 다음 단계

- 9단계 (Contest 및 News API 모듈 구현) 또는 10단계 (외부 API 통합 및 데이터 수집) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)
