# 002 - 설계 프롬프트 작성

## 기본 정보
- **작업일**: 2026-03-21
- **산출물**: `prompts/reference/design/018-agent-statistics-chart-response.md`
- **유형**: 설계서 작성을 지시하는 프롬프트 문서

## 작업 내용

기존 `prompts/reference/design/` 디렉토리의 프롬프트 패턴(017-common-conversation-module.md 등)을 분석하여, Agent 통계 Tool 차트 데이터 응답 개선 설계서를 작성하도록 지시하는 프롬프트를 작성했다.

## 프롬프트 구조

| 섹션 | 내용 |
|------|------|
| 목표 | `get_emerging_tech_statistics` 응답을 차트 친화적으로 개선 |
| 배경 | 현재 문제 4가지 (LLM 의존 시각화, 구조화 데이터 부재 등) |
| 코드베이스 맥락 | Tool 계층, Agent 실행 계층, 데이터 집계 계층의 핵심 파일 경로 |
| 설계 요구사항 | 8개 섹션 — 응답 구조, 차트 포맷, Tool-to-Frontend 전달 메커니즘 등 |
| 제약사항 | 하위 호환성, LLM 동작 보존, 의존성 방향, 오버엔지니어링 금지 |

## 코드 리뷰 후 수정 사항

1. 의존성 방향 오류 수정: `API → Common → Datasource` → `API → Datasource → Common → Client`
2. `AgentExecutionResult` 하위 호환성 제약에 모든 7개 필드와 팩토리 메서드를 명시적으로 나열
