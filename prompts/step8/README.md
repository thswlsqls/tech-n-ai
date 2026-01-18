# Step 8: Client 모듈 구현

## 개요

이 단계는 외부 API 통합을 위한 Client 모듈을 구현합니다. Feign Client, RSS Scraper, Web Scraper, Slack Client를 포함합니다.

## 관련 파일

### 프롬프트
- `rss-modules-analysis-improvement-prompt.md`: RSS 모듈 분석 개선 프롬프트
- `rss-scraper-modules-analysis-improvement-prompt.md`: RSS/Scraper 모듈 분석 개선 프롬프트
- `scraper-modules-analysis-improvement-prompt.md`: Scraper 모듈 분석 개선 프롬프트
- `slack-integration-design-prompt.md`: Slack 연동 설계 프롬프트

### 설계서
- `docs/step8/rss-scraper-modules-analysis.md`: RSS/Scraper 모듈 분석
- `docs/step8/slack-integration-design-guide.md`: Slack 연동 설계 가이드

## 주요 내용

- client-feign: OpenFeign 클라이언트 구현
- client-rss: RSS 피드 수집 클라이언트
- client-scraper: 웹 스크래핑 클라이언트
- client-slack: Slack 알림 클라이언트

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 권장
