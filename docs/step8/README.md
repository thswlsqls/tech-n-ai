# Step 8: Client 모듈 구현

## Plan Task

```
plan task: 외부 API 연동 Client 모듈 구현 (Contract 패턴 적용)
```

## 개요

json/sources.json에 정의된 Priority 1 출처와의 API 통합을 위한 Client 모듈 구현. Contract 패턴을 적용하여 Mock/Rest 모드 전환 가능한 구조 구현. RSS 피드 및 웹 스크래핑을 통한 데이터 수집 모듈 구현.

## 작업 목표

- client-feign: Priority 1 API 출처와의 통합 완료
- client-rss: 4개 RSS 출처로부터 데이터 수집 가능
- client-scraper: 5개 웹 스크래핑 출처로부터 데이터 수집 가능

## 주요 특징

- Contract 패턴 적용: Mock/Rest 모드 전환 가능
- 기존 client-feign 모듈의 Contract 패턴 구조를 참고하여 일관성 유지
- 법적/윤리적 준수: robots.txt, ToS 확인 필수 (client-scraper)

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 권장

## 다음 단계

- 9단계 (Contest 및 News API 모듈 구현) 또는 10단계 (외부 API 통합 및 데이터 수집) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)
