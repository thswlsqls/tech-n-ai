# 004 - 백엔드 구현

## 기본 정보
- **작업일**: 2026-03-21
- **모듈**: `api-agent`
- **빌드 검증**: `./gradlew :api-agent:build` 성공

## 구현 순서 (설계서 마이그레이션 순서 준수)

### Step 1: ChartData DTO 생성
- **파일**: `api/agent/src/main/java/.../agent/dto/ChartData.java` (신규)
- Java record로 `ChartData`, `ChartMeta`, `DataPoint` 중첩 record 정의
- `agent/dto/` 패키지에 배치 (Tool DTO가 아닌 응답 구성요소)

### Step 2: ToolExecutionMetrics 확장
- **파일**: `api/agent/src/main/java/.../metrics/ToolExecutionMetrics.java`
- `ArrayList<ChartData> chartDataList` 필드 추가
- `addChartData(ChartData)`, `getChartData()` (List.copyOf 불변 복사본) 메서드 추가

### Step 3: EmergingTechAgentTools에 차트 데이터 수집
- **파일**: `api/agent/src/main/java/.../tool/EmergingTechAgentTools.java`
- `getStatistics()`: 정상 응답(`message == null && !groups.isEmpty()`) 시 차트 데이터 수집
- `analyzeTextFrequency()`: 정상 응답(`message == null && !topWords.isEmpty()`) 시 차트 데이터 수집
- 변환 메서드 3개 추가:
  - `toStatisticsChartData(StatisticsDto)` → chartType="pie"
  - `toWordFrequencyChartData(WordFrequencyDto, int topN)` → chartType="bar"
  - `parsePeriod(String period)` → [startDate, endDate] 배열
- `GROUP_BY_LABELS` 맵으로 groupBy → 표시 레이블 변환 (provider → "Provider")

### Step 4: AgentExecutionResult 수정
- **파일**: `api/agent/src/main/java/.../agent/AgentExecutionResult.java`
- `List<ChartData> chartData` 필드 추가 (8번째 필드)
- `success()` 팩토리: `chartData` 파라미터 추가
- `failure()` 팩토리: `List.of()` (빈 리스트) 반환

### Step 5: EmergingTechAgentImpl.execute() 수정
- **파일**: `api/agent/src/main/java/.../agent/EmergingTechAgentImpl.java`
- 정상 경로: `metrics.getChartData()` → `AgentExecutionResult.success()`에 전달
- `AgentLoopDetectedException` 핸들러: 동일하게 `metrics.getChartData()` 전달 (루프 감지 전 수집된 데이터 보존)

### Step 6: 테스트 파일 업데이트
- `AgentControllerTest.java`: `success()` 호출 3곳에 `List.of()` 추가
- `AgentFacadeTest.java`: `success()` 호출 2곳에 `List.of()` 추가

### Step 7: API 정의서 업데이트
- **파일**: `docs/reference/api-specifications/001-api-agent.md` (v4 → v5)
- `AgentExecutionResult` 필드 표에 `chartData` 추가
- `ChartData`, `ChartMeta`, `DataPoint` 타입 정의 섹션 추가
- 통계 분석 포함/미포함 응답 예시 JSON 2종 추가
- 프론트엔드 연동 가이드 섹션 추가

## 핵심 구현 패턴

### 차트 데이터 수집 조건
```java
// STOP 메시지가 아니고, 실제 데이터가 있을 때만 수집
if (result.message() == null && !result.groups().isEmpty()) {
    metrics().addChartData(toStatisticsChartData(result));
}
```

### parsePeriod 계약
| period 값 | 반환 |
|-----------|------|
| `"2026-01-01 ~ 2026-03-21"` | `["2026-01-01", "2026-03-21"]` |
| `"전체 기간"` | `[null, null]` |
| `""` (빈 문자열) | `[null, null]` |
| 구분자 없는 문자열 | `[null, null]` |
