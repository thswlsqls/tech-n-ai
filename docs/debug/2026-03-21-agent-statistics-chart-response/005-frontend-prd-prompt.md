# 005 - 프론트엔드 PRD 프롬프트 작성

## 기본 정보
- **작업일**: 2026-03-21
- **산출물**: `tech-n-ai-frontend/docs/prompts/008-agent-chart-data-prd-generation-prompt.md`
- **유형**: PRD 작성을 지시하는 프롬프트 문서

## 작업 내용

기존 프롬프트 파일(006, 007)의 패턴을 분석하여, Agent 차트 데이터 렌더링 PRD 작성 프롬프트를 작성했다.

## 프롬프트 구조

| 섹션 | 내용 |
|------|------|
| 역할 | 데이터 시각화 전문가 + Recharts/react-markdown 공식 문서 전문가 |
| 입력 자료 | `<api-spec>`, `<design-doc>` 두 백엔드 문서 삽입 영역 |
| 배경 | 현재 상태(chartData 미반영), 범위, 범위 밖 |
| 코드 컨텍스트 | 현재 타입, chartData 구조, 응답 예시 3종, Before/After 와이어프레임, 실제 코드 |
| 기능 요구사항 | F1~F6 — 타입, 데이터 전달, AgentChart, ChartSection, 버블 통합, 반응형 |
| 출력 형식 | 9개 섹션 PRD 구조 |
| 제약 조건 | Recharts 공식 문서만, SSR 제약, 오버엔지니어링 금지 |

## 적용된 프롬프트 엔지니어링 기법 (14개)

Role Prompting, Authoritative Source Anchoring, Dual Document Input, Concrete Data Examples, Before/After Wireframe, Code Snippet Grounding, Enumerated Features, Explicit Output Format, Scope Boundary, SSR Constraint, Negative Constraint, Data Trust Model, Design System Consistency, Directory Structure Anchoring

## 코드 리뷰 후 수정 사항

1. 메시지 버블 코드 스니펫을 실제 코드와 일치하도록 수정 (`!isUser` 가드, failed 블록 포함)
2. `ExecutionMeta` Pick 타입 관계를 명시적으로 추가
3. Java `long` → TypeScript `number` 매핑 주석 추가
4. "처리 예정" → "별도 다룸"으로 수정 (007 PRD 상태 반영)
