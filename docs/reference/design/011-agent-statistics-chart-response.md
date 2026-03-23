# Agent 통계 Tool 차트 데이터 응답 개선 설계서

**작성일**: 2026-03-21
**대상 모듈**: api-agent
**관련 프롬프트**: prompts/reference/design/018-agent-statistics-chart-response.md

---

## 1. 개요

### 목적

`get_emerging_tech_statistics`와 `analyze_text_frequency` Tool이 생성하는 통계 데이터를 프론트엔드에서 구조화된 차트(도표, 그래프)로 직접 렌더링할 수 있도록, `AgentExecutionResult` 응답에 구조화된 차트 데이터를 추가한다.

### 현재 문제

1. **LLM 의존적 시각화**: 차트가 LLM의 Mermaid 텍스트 출력에 전적으로 의존하여 문법 오류 시 차트가 깨짐
2. **구조화 데이터 부재**: 프론트엔드가 받는 것은 `summary` 문자열뿐으로, Recharts/Chart.js 등으로 직접 렌더링 불가
3. **차트 커스터마이징 불가**: Mermaid의 제한된 스타일링으로 Neo-Brutalism 디자인 시스템 적용 불가
4. **데이터 재활용 불가**: 통계 데이터가 텍스트에 묻혀 정렬/필터/다운로드 등 인터랙션 불가

### 해결 방안 요약

기존 `ToolExecutionMetrics`의 ThreadLocal 패턴을 확장하여, 분석 Tool 실행 시 차트 데이터를 사이드 채널로 수집하고 `AgentExecutionResult`에 `chartData` 필드로 전달한다. 기존 `summary` Markdown 렌더링은 그대로 유지한다.

---

## 2. 현재 구조 분석

### 2.1 데이터 흐름도

```
[현재 흐름]

  프론트엔드 → POST /api/v1/agent/run
                    ↓
              AgentController.runAgent()
                    ↓
              AgentFacade.runAgent()
                    ↓
              EmergingTechAgentImpl.execute(goal, sessionId)
                    ↓
              ToolExecutionMetrics 생성 → ThreadLocal 바인딩
                    ↓
              assistant.chat(sessionId, builtPrompt)
                    ↓
              ┌─ LangChain4j ReAct 루프 ──────────────────────┐
              │  LLM → tool_call: get_emerging_tech_statistics │
              │         ↓                                      │
              │  EmergingTechAgentTools.getStatistics()         │
              │         ↓                                      │
              │  AnalyticsToolAdapter.getStatistics()           │
              │         ↓                                      │
              │  EmergingTechAggregationService.countByGroup()  │
              │         ↓                                      │
              │  StatisticsDto 반환 → JSON 직렬화 → LLM 입력   │
              │         ↓                                      │
              │  LLM이 Mermaid 차트 + Markdown 표 텍스트 생성   │
              └────────────────────────────────────────────────┘
                    ↓
              String response (Markdown 텍스트)
                    ↓
              AgentExecutionResult.success(response, ...)
                    ↓
              ApiResponse<AgentExecutionResult> → 프론트엔드
```

**문제 지점**: `StatisticsDto`는 LLM에게만 전달되고, 프론트엔드에는 LLM이 생성한 텍스트만 도달한다.

### 2.2 현재 DTO 구조

**StatisticsDto** (LLM이 읽는 JSON):
```java
public record StatisticsDto(
    String groupBy,          // "provider", "source_type", "update_type"
    String startDate,        // "2026-01-01" 또는 null
    String endDate,          // "2026-03-21" 또는 null
    long totalCount,         // 그룹별 count 합산
    List<GroupCount> groups,  // [{name: "OPENAI", count: 145}, ...]
    String message           // null (정상) 또는 "STOP: ..." (루프 감지)
) {
    public record GroupCount(String name, long count) {}
}
```

**WordFrequencyDto** (LLM이 읽는 JSON):
```java
public record WordFrequencyDto(
    long totalDocuments,           // 분석 대상 도큐먼트 수
    String period,                 // "2026-01-01 ~ 2026-03-21" 또는 "전체 기간"
    List<WordCount> topWords,      // [{word: "model", count: 312}, ...]
    List<WordCount> topBigrams,    // (현재 빈 리스트)
    String message                 // null 또는 "STOP: ..."
) {
    public record WordCount(String word, long count) {}
}
```

### 2.3 AgentExecutionResult 현재 구조

```java
public record AgentExecutionResult(
    boolean success,
    String summary,              // LLM 생성 Markdown 텍스트
    String sessionId,
    int toolCallCount,
    int analyticsCallCount,
    long executionTimeMs,
    List<String> errors
) {
    public static AgentExecutionResult success(...) { ... }
    public static AgentExecutionResult failure(...) { ... }
}
```

---

## 3. 응답 구조 설계

### 3.1 선택지 비교 분석

| 기준 | A: chartData 필드 추가 | B: summary 내 데이터 임베딩 | C: Tool 결과 별도 필드 |
|------|----------------------|-------------------------|---------------------|
| 프론트엔드 파싱 복잡도 | **낮음** — JSON 필드 직접 접근 | 높음 — 정규식/구분자 파싱 필요 | 낮음 — JSON 직접 접근 |
| summary 호환성 | **변경 없음** | 구분자가 Markdown에 노출될 위험 | 변경 없음 |
| LLM 흐름 영향 | **없음** — 사이드 채널 | 없음 | 없음 |
| 구현 복잡도 | **낮음** — ToolExecutionMetrics 확장 | 중간 — 파싱 로직 필요 | 높음 — 모든 Tool 결과 수집 범용화 |
| 데이터 크기 | 분석 Tool 결과만 포함 | summary 크기 증가 | 모든 Tool 결과 포함으로 과대 |

### 3.2 선택: A — `AgentExecutionResult`에 `chartData` 필드 추가

**선택 이유**:
- 프론트엔드 파싱이 가장 단순 (JSON 필드 접근)
- 기존 `summary` 필드에 일절 영향 없음 (하위 호환성 완벽 보장)
- LLM 흐름에 영향 없음 (Tool 반환값은 그대로 LLM에게 전달)
- 기존 `ToolExecutionMetrics`의 ThreadLocal 패턴을 재사용하여 구현 복잡도 최소화
- 분석 Tool 결과만 선택적으로 수집하므로 응답 크기 제어 가능

### 3.3 AgentExecutionResult 변경 사항

```java
public record AgentExecutionResult(
    boolean success,
    String summary,
    String sessionId,
    int toolCallCount,
    int analyticsCallCount,
    long executionTimeMs,
    List<String> errors,
    List<ChartData> chartData       // 신규 필드
) {
    public static AgentExecutionResult success(
            String summary, String sessionId,
            int toolCallCount, int analyticsCallCount,
            long executionTimeMs, List<ChartData> chartData) {
        return new AgentExecutionResult(
            true, summary, sessionId,
            toolCallCount, analyticsCallCount, executionTimeMs,
            List.of(), chartData);
    }

    public static AgentExecutionResult failure(
            String summary, String sessionId, List<String> errors) {
        return new AgentExecutionResult(
            false, summary, sessionId,
            0, 0, 0, errors, List.of());
    }
}
```

**하위 호환성 보장**:
- 기존 7개 필드의 위치와 의미 변경 없음
- `chartData`는 신규 필드로 끝에 추가
- `failure()` 시 빈 리스트 반환 → 프론트엔드가 이 필드를 무시해도 기존 동작 유지
- 분석 Tool이 호출되지 않은 요청에서도 빈 리스트로 안전하게 반환

---

## 4. 차트 데이터 포맷

### 4.1 ChartData DTO 설계

```java
/**
 * 프론트엔드 차트 렌더링용 구조화 데이터
 *
 * <p>통계 집계와 키워드 빈도 분석 결과를 통일된 포맷으로 전달한다.
 * 프론트엔드 차트 라이브러리(Recharts 등)에서 dataPoints를 직접
 * data prop으로 전달할 수 있도록 label/value 쌍으로 구성한다.
 */
public record ChartData(
    String chartType,              // "pie" | "bar"
    String title,                  // 차트 제목 (예: "Provider별 통계")
    ChartMeta meta,                // 메타데이터
    List<DataPoint> dataPoints     // 차트 데이터 포인트
) {
    public record ChartMeta(
        String groupBy,            // 집계 기준 ("provider", "source_type", "update_type", "keyword")
        String startDate,          // 조회 시작일 (nullable)
        String endDate,            // 조회 종료일 (nullable)
        long totalCount            // 전체 합계
    ) {}

    public record DataPoint(
        String label,              // 항목 이름 ("OPENAI", "model" 등)
        long value                 // 수치
    ) {}
}
```

### 4.2 설계 판단

**chartType 결정 규칙**:
- `get_emerging_tech_statistics` → `"pie"` (비율 비교에 적합)
- `analyze_text_frequency` → `"bar"` (빈도 순위 비교에 적합)

서버에서 chartType을 결정하는 이유: Tool의 특성에 따라 최적 차트 유형이 고정적이며, 프론트엔드에서 임의 판단하게 하면 일관성이 깨진다. 프론트엔드는 chartType을 힌트로 사용하되, 필요 시 다른 유형으로 전환할 수 있다.

**percentage 계산**: 프론트엔드에 위임한다. `totalCount`와 각 `value`를 제공하므로 프론트엔드에서 `value / totalCount * 100`으로 계산 가능하다. 서버에서 비율을 계산하면 부동소수점 정밀도 문제가 발생하고, 프론트엔드 차트 라이브러리가 자체적으로 비율을 계산하는 것이 일반적이다.

**title 생성 규칙**:
- 통계: `"{groupBy}별 통계"` (예: "Provider별 통계", "SourceType별 통계")
- 키워드: `"키워드 빈도 TOP {topN}"`

### 4.3 Tool별 매핑

**get_emerging_tech_statistics → ChartData**:

| StatisticsDto 필드 | ChartData 필드 |
|---------------------|----------------|
| groupBy | meta.groupBy |
| startDate | meta.startDate |
| endDate | meta.endDate |
| totalCount | meta.totalCount |
| groups[].name | dataPoints[].label |
| groups[].count | dataPoints[].value |
| — | chartType = "pie" |
| — | title = "{groupBy}별 통계" |

**analyze_text_frequency → ChartData**:

| WordFrequencyDto 필드 | ChartData 필드 |
|------------------------|----------------|
| — | meta.groupBy = "keyword" |
| period → startDate, endDate | meta.startDate, meta.endDate |
| totalDocuments | meta.totalCount |
| topWords[].word | dataPoints[].label |
| topWords[].count | dataPoints[].value |
| — | chartType = "bar" |
| — | title = "키워드 빈도 TOP {N}" |

---

## 5. Tool-to-Frontend 데이터 전달 메커니즘

### 5.1 설계 원칙

기존 `ToolExecutionMetrics`가 ThreadLocal로 Tool 실행 메트릭을 수집하는 패턴이 이미 검증되어 있다. 같은 패턴을 확장하여 차트 데이터도 수집한다.

### 5.2 데이터 수집 경로

```
[개선 흐름]

  EmergingTechAgentImpl.execute()
       ↓
  ToolExecutionMetrics 생성 → ThreadLocal 바인딩
       ↓
  assistant.chat() — LangChain4j ReAct 루프 시작
       ↓
  ┌─ Tool 호출 시 ──────────────────────────────────────────┐
  │  EmergingTechAgentTools.getStatistics()                  │
  │       ↓                                                  │
  │  AnalyticsToolAdapter.getStatistics() → StatisticsDto    │
  │       ↓                                                  │
  │  ★ metrics().addChartData(toChartData(statisticsDto))    │
  │       ↓                                                  │
  │  StatisticsDto 반환 → LLM 입력 (기존 흐름 유지)          │
  └──────────────────────────────────────────────────────────┘
       ↓
  assistant.chat() 완료 → String response
       ↓
  List<ChartData> chartData = metrics.getChartData()
       ↓
  AgentExecutionResult.success(response, ..., chartData)
```

### 5.3 ToolExecutionMetrics 확장

`ToolExecutionMetrics`에 차트 데이터 수집 기능을 추가한다:

```java
public class ToolExecutionMetrics {
    // ... 기존 필드 유지 ...

    /** 분석 Tool 실행 중 수집된 차트 데이터 */
    private final List<ChartData> chartDataList = new ArrayList<>();

    /**
     * 차트 데이터 추가
     * 분석 Tool(통계/키워드) 실행 시 호출
     */
    public void addChartData(ChartData chartData) {
        chartDataList.add(chartData);
    }

    /**
     * 수집된 차트 데이터 반환 (불변 복사본)
     */
    public List<ChartData> getChartData() {
        return List.copyOf(chartDataList);
    }
}
```

**여러 Tool이 여러 번 호출될 경우**: `chartDataList`에 순서대로 누적된다. 예를 들어 "Provider별 통계와 키워드 분석을 해줘"라는 요청에서 `get_emerging_tech_statistics`와 `analyze_text_frequency`가 각각 호출되면, `chartData` 리스트에 2개의 `ChartData`가 포함된다.

**STOP 메시지 반환 시**: 루프 감지로 `message = "STOP: ..."`인 `StatisticsDto`가 반환되는 경우, 차트 데이터를 수집하지 않는다 (데이터가 비어있으므로 의미 없음).

### 5.4 EmergingTechAgentImpl.execute() 수정

```java
// assistant.chat() 완료 후
List<ChartData> chartData = metrics.getChartData();

return AgentExecutionResult.success(
    response, sessionId,
    toolCallCount, analyticsCallCount,
    elapsed, chartData);
```

---

## 6. DTO 변경 사항

### 6.1 StatisticsDto — 변경 없음

`StatisticsDto`는 변경하지 않는다.

**이유**:
- `StatisticsDto`는 LLM이 읽는 Tool 반환값이다. 필드를 추가/변경하면 LLM의 Tool 활용 패턴이 영향받을 수 있다.
- 차트 데이터는 별도 `ChartData` DTO를 통해 프론트엔드에 전달하므로, `StatisticsDto`에 프론트엔드용 필드를 혼합할 필요가 없다.
- 단일 책임 원칙(SRP): `StatisticsDto`는 LLM과의 통신 계약, `ChartData`는 프론트엔드와의 통신 계약.

### 6.2 WordFrequencyDto — 변경 없음

같은 이유로 변경하지 않는다.

### 6.3 ChartData 변환 로직 위치

`EmergingTechAgentTools`에 private 메서드로 변환 로직을 배치한다:

```java
// EmergingTechAgentTools 내부

private ChartData toStatisticsChartData(StatisticsDto dto) {
    String title = resolveGroupByLabel(dto.groupBy()) + "별 통계";
    List<ChartData.DataPoint> dataPoints = dto.groups().stream()
        .map(g -> new ChartData.DataPoint(g.name(), g.count()))
        .toList();
    return new ChartData(
        "pie", title,
        new ChartData.ChartMeta(dto.groupBy(), dto.startDate(), dto.endDate(), dto.totalCount()),
        dataPoints);
}

private ChartData toWordFrequencyChartData(WordFrequencyDto dto, int topN) {
    String title = "키워드 빈도 TOP " + topN;
    List<ChartData.DataPoint> dataPoints = dto.topWords().stream()
        .map(w -> new ChartData.DataPoint(w.word(), w.count()))
        .toList();
    // period에서 startDate/endDate 추출
    String[] dates = parsePeriod(dto.period());
    return new ChartData(
        "bar", title,
        new ChartData.ChartMeta("keyword", dates[0], dates[1], dto.totalDocuments()),
        dataPoints);
}

/**
 * WordFrequencyDto.period 문자열을 [startDate, endDate] 배열로 분리
 *
 * period 형식 (AnalyticsToolAdapter.buildPeriodString에서 생성):
 *   - "2026-01-01 ~ 2026-03-21" → ["2026-01-01", "2026-03-21"]
 *   - "~ ~ 2026-03-21"          → [null, "2026-03-21"]
 *   - "2026-01-01 ~ ~"          → ["2026-01-01", null]
 *   - "전체 기간"                → [null, null]
 *   - "" (빈 문자열)             → [null, null]
 *
 * 구분자: " ~ " (공백 포함)
 * "~" 단독 토큰은 null로 변환
 */
private String[] parsePeriod(String period) { ... }
```

**위치 선택 근거**: `EmergingTechAgentTools`는 Tool 실행과 메트릭 수집을 모두 담당하는 클래스이다. 변환 로직이 간단하고(DTO → DTO 매핑) 이 클래스 외에서 사용되지 않으므로, 별도 클래스를 만들지 않는다.

### 6.4 LLM 호환성 영향 분석

| 항목 | 영향 |
|------|------|
| `StatisticsDto` JSON 구조 | **변경 없음** — LLM이 읽는 JSON이 동일 |
| `WordFrequencyDto` JSON 구조 | **변경 없음** |
| `@Tool` 어노테이션 (name, value) | **변경 없음** |
| `@P` 파라미터 설명 | **변경 없음** |
| 시스템 프롬프트 | 변경 최소화 (7장 참조) |

---

## 7. AgentPromptConfig 수정안

### 7.1 visualization 섹션 — 유지

현재 시스템 프롬프트의 `visualization` 섹션(Mermaid 차트 가이드)은 **그대로 유지**한다.

**이유**:
- LLM이 Mermaid 차트를 `summary`에 포함하는 기존 동작은 유지해야 한다. `chartData`는 프론트엔드가 직접 차트를 렌더링하기 위한 **보조 데이터**이지, Mermaid를 대체하는 것이 아니다.
- 프론트엔드가 `chartData`를 활용하여 자체 차트를 렌더링하면 Mermaid 블록을 숨길 수 있지만, 이는 프론트엔드의 선택이다.
- Mermaid 가이드를 제거하면 LLM이 통계 결과를 텍스트로만 출력하게 되어 `chartData`가 없는 환경(대화 이력 조회 등)에서 시각화가 없어진다.

### 7.2 변경 불필요 확인

시스템 프롬프트의 다른 섹션(role, constraints, tools, repositories, rules)도 변경할 필요가 없다. 차트 데이터 수집은 Tool 실행 계층에서 투명하게 처리되므로 LLM의 동작에 영향을 주지 않는다.

---

## 8. API 정의서 업데이트

### 8.1 AgentExecutionResult 필드 변경

`docs/reference/api-specifications/001-api-agent.md` 섹션 3.1의 `AgentExecutionResult 필드` 표에 추가:

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| chartData | ChartData[] | O | 분석 Tool 실행 결과 차트 데이터 (분석 Tool 미호출 시 빈 배열) |

### 8.2 ChartData 타입 정의 추가

```
### ChartData

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| chartType | String | O | 차트 유형 ("pie", "bar") |
| title | String | O | 차트 제목 |
| meta | ChartMeta | O | 메타데이터 |
| dataPoints | DataPoint[] | O | 차트 데이터 포인트 |

### ChartMeta

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| groupBy | String | O | 집계 기준 ("provider", "source_type", "update_type", "keyword") |
| startDate | String | X | 조회 시작일 (YYYY-MM-DD, 전체 기간이면 null) |
| endDate | String | X | 조회 종료일 (YYYY-MM-DD, 전체 기간이면 null) |
| totalCount | long | O | 전체 합계 |

### DataPoint

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| label | String | O | 항목 이름 (예: "OPENAI", "model") |
| value | long | O | 수치 |
```

### 8.3 응답 예시 JSON 업데이트

**통계 분석이 포함된 응답 예시**:

```json
{
  "code": "2000",
  "messageCode": { "code": "SUCCESS", "text": "성공" },
  "message": "success",
  "data": {
    "success": true,
    "summary": "## Provider별 통계\n\n| Provider | 건수 |\n|---|---|\n| OPENAI | 145 |\n| ANTHROPIC | 98 |\n\n```mermaid\npie title Provider별 수집 현황\n    \"OPENAI\" : 145\n    \"ANTHROPIC\" : 98\n```",
    "sessionId": "admin-123-abc12345",
    "toolCallCount": 1,
    "analyticsCallCount": 1,
    "executionTimeMs": 4821,
    "errors": [],
    "chartData": [
      {
        "chartType": "pie",
        "title": "Provider별 통계",
        "meta": {
          "groupBy": "provider",
          "startDate": "2026-01-01",
          "endDate": "2026-03-21",
          "totalCount": 243
        },
        "dataPoints": [
          { "label": "OPENAI", "value": 145 },
          { "label": "ANTHROPIC", "value": 98 }
        ]
      }
    ]
  }
}
```

**분석 Tool이 호출되지 않은 응답 예시** (데이터 수집 요청 등):

```json
{
  "code": "2000",
  "data": {
    "success": true,
    "summary": "GitHub에서 10개의 릴리스를 수집했습니다.",
    "sessionId": "admin-123-abc12345",
    "toolCallCount": 5,
    "analyticsCallCount": 0,
    "executionTimeMs": 15200,
    "errors": [],
    "chartData": []
  }
}
```

### 8.4 프론트엔드 연동 가이드 추가

API 정의서에 다음 참고 섹션을 추가한다:

```markdown
### 차트 데이터 활용 가이드

- `chartData`가 빈 배열이 아닌 경우, 프론트엔드에서 차트 컴포넌트로 시각화 가능
- `chartType`에 따라 적절한 차트 유형 선택 (pie → 원형 차트, bar → 막대 차트)
- `dataPoints`의 `label`/`value` 쌍을 차트 라이브러리의 data prop으로 직접 전달 가능
- `meta.totalCount`를 사용하여 비율(%) 계산 가능: `value / totalCount * 100`
- `chartData`가 빈 배열인 경우 기존 `summary` Markdown 렌더링만 수행
- `summary`의 Mermaid 블록과 `chartData`는 동일한 데이터를 다른 형태로 표현한 것이므로,
  프론트엔드에서 `chartData`가 존재할 때 Mermaid 블록을 숨기거나 자체 차트로 대체할 수 있음
```

---

## 9. 마이그레이션 순서

각 단계는 독립적으로 빌드·테스트 가능하다.

### Step 1: ChartData DTO 추가

**범위**: 새 파일 생성만

- `api/agent/src/main/java/.../agent/dto/ChartData.java` 생성. `ChartData`는 Tool DTO가 아니라 `AgentExecutionResult`의 응답 구성요소이므로, `tool/dto/`가 아닌 `agent/dto/` 패키지에 배치한다 (SRP: `StatisticsDto`는 LLM 통신 계약, `ChartData`는 프론트엔드 통신 계약).

**검증**: `./gradlew :api-agent:build` 성공

### Step 2: ToolExecutionMetrics 확장

**범위**: 기존 파일 수정

- `ToolExecutionMetrics`에 `chartDataList` 필드, `addChartData()`, `getChartData()` 추가

**검증**: `./gradlew :api-agent:build` 성공 (기존 메트릭 기능 영향 없음)

### Step 3: EmergingTechAgentTools에 차트 데이터 수집 추가

**범위**: 기존 파일 수정

- `getStatistics()` 메서드에서 정상 응답(`message == null` **이고** `groups`가 비어있지 않을 때) 시 `metrics().addChartData(toStatisticsChartData(result))` 호출. 검증 실패로 빈 `groups`가 반환되는 경우 차트 데이터를 수집하지 않는다.
- `analyzeTextFrequency()` 메서드에서 정상 응답(`message == null` **이고** `topWords`가 비어있지 않을 때) 시 `metrics().addChartData(toWordFrequencyChartData(result, effectiveTopN))` 호출. 검증 실패로 빈 `topWords`가 반환되는 경우 차트 데이터를 수집하지 않는다.
- `toStatisticsChartData()`, `toWordFrequencyChartData()` private 메서드 추가

**검증**: `./gradlew :api-agent:build` 성공, 단위 테스트로 변환 로직 검증

### Step 4: AgentExecutionResult에 chartData 필드 추가

**범위**: 기존 파일 수정

- `AgentExecutionResult` record에 `List<ChartData> chartData` 필드 추가
- `success()`, `failure()` 팩토리 메서드 시그니처 업데이트
- `failure()` 시 빈 리스트 반환

**검증**: `./gradlew :api-agent:build` 성공

### Step 5: EmergingTechAgentImpl.execute() 수정

**범위**: 기존 파일 수정

- `execute()` 메서드에서 `metrics.getChartData()`를 가져와 `AgentExecutionResult.success()`에 전달
- `AgentLoopDetectedException` 핸들러에서도 `metrics.getChartData()`를 전달한다 (`List.of()`가 아님). 루프 감지 전에 이미 성공적으로 호출된 분석 Tool의 차트 데이터가 누적되어 있을 수 있으므로, 해당 데이터를 프론트엔드에 전달해야 한다.

**검증**: `./gradlew :api-agent:build` 성공, 통합 테스트 또는 수동 테스트로 엔드투엔드 검증

### Step 6: API 정의서 업데이트

**범위**: 문서 수정

- `docs/reference/api-specifications/001-api-agent.md` 업데이트 (8장 내용 반영)

**검증**: 실제 API 응답과 문서 일치 확인

---

## 10. 검증 체크리스트

- [ ] 기존 Agent 실행 기능 정상 동작 (`summary` Markdown 렌더링 유지)
- [ ] `get_emerging_tech_statistics` 호출 시 `chartData`에 pie 차트 데이터 포함
- [ ] `analyze_text_frequency` 호출 시 `chartData`에 bar 차트 데이터 포함
- [ ] 분석 Tool 미호출 요청에서 `chartData`가 빈 배열
- [ ] `failure()` 응답에서 `chartData`가 빈 배열
- [ ] 루프 감지(STOP 메시지) 시 빈 차트 데이터가 수집되지 않음
- [ ] 한 요청에서 여러 분석 Tool 호출 시 `chartData`에 모두 포함
- [ ] `AgentExecutionResult`의 기존 7개 필드 동작 변경 없음
- [ ] `StatisticsDto`, `WordFrequencyDto`의 JSON 구조 변경 없음 (LLM 호환성)
- [ ] 전체 프로젝트 빌드 성공: `./gradlew build`
- [ ] API 정의서와 실제 응답 일치

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-03-21
