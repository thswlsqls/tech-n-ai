# 003 - 백엔드 설계서 작성

## 기본 정보
- **작업일**: 2026-03-21
- **산출물**: `docs/reference/design/011-agent-statistics-chart-response.md`
- **유형**: 기능 설계 문서

## 핵심 설계 결정

### 응답 구조: 선택지 A 채택
`AgentExecutionResult`에 `List<ChartData> chartData` 필드를 추가하여 구조화된 차트 데이터를 별도 전달.

| 기준 | A: chartData 필드 추가 | B: summary 내 임베딩 | C: Tool 결과 별도 필드 |
|------|----------------------|---------------------|---------------------|
| 프론트엔드 파싱 | **낮음** | 높음 | 낮음 |
| summary 호환성 | **변경 없음** | 위험 | 변경 없음 |
| 구현 복잡도 | **낮음** | 중간 | 높음 |

### Tool-to-Frontend 데이터 전달 메커니즘
기존 `ToolExecutionMetrics`의 ThreadLocal 패턴을 확장하여 사이드 채널로 차트 데이터 수집.

```
@Tool 반환값(StatisticsDto) → LLM 입력 (기존 유지)
                            ↘ metrics().addChartData() → AgentExecutionResult.chartData
```

### LLM 영향 제로
- `StatisticsDto`, `WordFrequencyDto` 변경 없음
- `AgentPromptConfig` 시스템 프롬프트 변경 없음 (Mermaid 가이드 유지)

## 코드 리뷰 후 수정 사항

1. 빈 데이터 검증 조건 추가 — `message == null`뿐 아니라 `groups/topWords`가 비어있지 않을 때만 차트 데이터 수집
2. `AgentLoopDetectedException` 핸들러에서 `metrics.getChartData()` 전달 명시 (루프 감지 전 수집된 데이터 보존)
3. `parsePeriod()` 메서드의 계약 문서화 (구분자, null 변환 규칙)
4. `ChartData` 파일 위치를 `tool/dto/` → `agent/dto/`로 수정 (SRP: LLM 통신 계약 vs 프론트엔드 통신 계약)
