# 001 - Agent 통계 Tool 차트 데이터 응답 개선 — 전체 개요

## 기본 정보
- **작업일**: 2026-03-21
- **모듈**: `api-agent` (백엔드), `admin` (프론트엔드)
- **유형**: 기능 개선 (Feature Enhancement)
- **상태**: 구현 완료, 빌드 검증 통과

## 작업 목적

`get_emerging_tech_statistics` 및 `analyze_text_frequency` Tool이 반환하는 통계 데이터를 프론트엔드에서 구조화된 차트(원형/막대)로 직접 렌더링할 수 있도록, 백엔드 응답 구조 개선 + 프론트엔드 차트 컴포넌트 구현.

### 기존 문제
1. 통계 데이터가 LLM의 Mermaid 텍스트 출력에만 의존 → 문법 오류 시 차트 깨짐
2. 프론트엔드가 받는 것은 `summary` 문자열뿐 → 구조화된 차트 렌더링 불가
3. Mermaid의 제한된 스타일링 → Neo-Brutalism 디자인 시스템 적용 불가
4. 통계 데이터가 텍스트에 묻혀 → 정렬/필터/다운로드 등 인터랙션 불가

### 해결 방안
`AgentExecutionResult`에 `chartData` 필드를 추가하여, Tool 실행 중 생성된 구조화 데이터를 사이드 채널로 수집 → 프론트엔드에서 Recharts 기반 차트로 렌더링.

## 작업 순서

전체 작업은 7단계로 진행되었다:

| 단계 | 작업 | 상세 문서 |
|------|------|---------|
| 1 | 설계 프롬프트 작성 | [002-design-prompt.md](002-design-prompt.md) |
| 2 | 백엔드 설계서 작성 | [003-backend-design.md](003-backend-design.md) |
| 3 | 백엔드 구현 | [004-backend-implementation.md](004-backend-implementation.md) |
| 4 | 프론트엔드 PRD 프롬프트 작성 | [005-frontend-prd-prompt.md](005-frontend-prd-prompt.md) |
| 5 | 프론트엔드 PRD 작성 | [006-frontend-prd.md](006-frontend-prd.md) |
| 6 | 프론트엔드 구현 | [007-frontend-implementation.md](007-frontend-implementation.md) |
| 7 | 연동 검증 | [008-integration-verification.md](008-integration-verification.md) |

## 산출물 요약

### 백엔드
| 파일 | 변경 |
|------|------|
| `prompts/reference/design/018-agent-statistics-chart-response.md` | 신규 — 설계 프롬프트 |
| `docs/reference/design/011-agent-statistics-chart-response.md` | 신규 — 설계서 |
| `docs/reference/api-specifications/001-api-agent.md` | v4 → v5 업데이트 |
| `api/agent/.../agent/dto/ChartData.java` | 신규 — 차트 DTO |
| `api/agent/.../agent/AgentExecutionResult.java` | chartData 필드 추가 |
| `api/agent/.../metrics/ToolExecutionMetrics.java` | 차트 데이터 수집 기능 추가 |
| `api/agent/.../tool/EmergingTechAgentTools.java` | 차트 데이터 수집 + 변환 메서드 |
| `api/agent/.../agent/EmergingTechAgentImpl.java` | chartData 전달 |
| 테스트 파일 2개 | success() 시그니처 업데이트 |

### 프론트엔드
| 파일 | 변경 |
|------|------|
| `docs/prompts/008-agent-chart-data-prd-generation-prompt.md` | 신규 — PRD 프롬프트 |
| `docs/PRDS/008-agent-chart-data-rendering.md` | 신규 — PRD |
| `admin/src/types/agent.ts` | ChartData/ChartMeta/DataPoint 타입 추가 |
| `admin/src/components/agent/agent-chart.tsx` | 신규 — Recharts 차트 |
| `admin/src/components/agent/chart-section.tsx` | 신규 — 차트 섹션 레이아웃 |
| `admin/src/components/agent/agent-message-bubble.tsx` | ChartSection 통합 |
| `admin/src/components/agent/agent-message-area.tsx` | chartData prop 전달 |
| `admin/src/app/agent/page.tsx` | chartData 매핑 |
| `admin/package.json` | recharts 의존성 추가 |
