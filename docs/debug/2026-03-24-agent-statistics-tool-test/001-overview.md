# get_emerging_tech_statistics 툴 테스트 개요

## 목적

`get_emerging_tech_statistics` 툴의 다양한 호출 시나리오를 체계적으로 테스트하여
Agent가 사용자의 자연어 질의를 올바른 파라미터로 변환하는지 검증한다.

## 툴 파라미터 구조

| 파라미터 | 타입 | 설명 | 유효값 |
|---------|------|------|--------|
| `groupBy` | String | 집계 기준 필드 | `provider`, `source_type`, `update_type` |
| `startDate` | String | 조회 시작일 (YYYY-MM-DD) | 빈 문자열이면 전체 기간 |
| `endDate` | String | 조회 종료일 (YYYY-MM-DD) | 빈 문자열이면 전체 기간 |

## Enum 값 참조

### TechProvider
`OPENAI`, `ANTHROPIC`, `GOOGLE`, `META`, `XAI`

### SourceType
`GITHUB_RELEASE`, `RSS`, `WEB_SCRAPING`

### EmergingTechType (UpdateType)
`MODEL_RELEASE`, `API_UPDATE`, `SDK_RELEASE`, `PRODUCT_LAUNCH`, `PLATFORM_UPDATE`, `BLOG_POST`

## 테스트 카테고리

| 파일 | 카테고리 | 테스트 수 |
|------|---------|----------|
| 002 | groupBy별 기본 테스트 (전체 기간) | 3 |
| 003 | groupBy + 특정 기간 조합 | 6 |
| 004 | 시작일만 / 종료일만 지정 | 2 |
| 005 | Provider 중심 질의 | 3 |
| 006 | SourceType 중심 질의 | 3 |
| 007 | UpdateType 중심 질의 | 4 |
| 008 | 복합 분석 질의 (다중 호출) | 4 |
| 009 | 엣지 케이스 / 예외 | 5 |
| **합계** | | **30** |
