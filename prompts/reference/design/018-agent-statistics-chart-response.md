# Agent 통계 Tool 차트 데이터 응답 개선 설계서 작성 프롬프트

## 목표

api-agent 모듈의 `get_emerging_tech_statistics` Tool이 반환하는 통계 데이터를 프론트엔드에서 구조화된 차트(도표, 그래프)로 렌더링할 수 있도록 응답 구조를 개선하는 설계서를 작성하라.

## 배경

### 현재 구조

현재 `get_emerging_tech_statistics` Tool의 데이터 흐름:

1. LLM이 Tool을 호출하면 `StatisticsDto`(groupBy, totalCount, `List<GroupCount>`)가 JSON으로 직렬화되어 LLM에게 전달됨
2. LLM이 JSON을 해석하여 Markdown 표 + Mermaid 차트 텍스트를 생성
3. 생성된 텍스트가 `AgentExecutionResult.summary` 필드(String)에 포함
4. 프론트엔드가 `summary`를 Markdown으로 렌더링하고, Mermaid 코드 블록을 차트로 변환

```
Tool 호출 → StatisticsDto(JSON) → LLM 해석 → Mermaid 텍스트 생성 → summary(String) → 프론트엔드 Mermaid 렌더링
```

### 현재 문제

1. **LLM 의존적 시각화**: 차트 생성이 전적으로 LLM의 텍스트 출력에 의존한다. LLM이 Mermaid 문법을 잘못 생성하면 차트가 깨진다.
2. **구조화 데이터 부재**: 프론트엔드가 받는 것은 `summary` 문자열뿐이다. 구조화된 차트 데이터(JSON)가 없으므로 Recharts, Chart.js 등으로 직접 렌더링할 수 없다.
3. **차트 커스터마이징 불가**: Mermaid의 제한된 스타일링으로 인해 프론트엔드 디자인 시스템(Neo-Brutalism)에 맞는 차트 커스터마이징이 어렵다.
4. **데이터 재활용 불가**: 통계 데이터가 텍스트에 묻혀 있어, 프론트엔드에서 정렬/필터/다운로드 등 추가 인터랙션을 구현할 수 없다.

### 기대 결과

프론트엔드가 `AgentExecutionResult` 응답에서 구조화된 통계 데이터를 추출하여, 자체 차트 컴포넌트로 도표와 그래프를 렌더링할 수 있어야 한다. 동시에 LLM의 자연어 해석(summary)도 유지한다.

---

## 현재 코드베이스 맥락

> 설계서 작성 전에 아래 파일들을 반드시 읽고 현재 구조를 이해하라.

### Agent Tool 계층

```
# Tool 정의 (get_emerging_tech_statistics 포함)
api/agent/src/main/java/.../tool/EmergingTechAgentTools.java

# 통계 어댑터 (MongoDB 집계 → StatisticsDto 변환)
api/agent/src/main/java/.../tool/adapter/AnalyticsToolAdapter.java

# 통계 응답 DTO (현재 Tool이 LLM에게 반환하는 JSON 구조)
api/agent/src/main/java/.../tool/dto/StatisticsDto.java

# 키워드 빈도 분석 DTO (참고용)
api/agent/src/main/java/.../tool/dto/WordFrequencyDto.java

# Tool 입력값 검증 (groupBy 필드 검증/정규화)
api/agent/src/main/java/.../tool/validation/ToolInputValidator.java
```

### Agent 실행 계층

```
# Agent 실행 결과 (프론트엔드가 받는 최종 응답 구조)
api/agent/src/main/java/.../agent/AgentExecutionResult.java

# Agent 구현체 (LangChain4j AiServices 조립, Tool 등록, 실행 루프)
api/agent/src/main/java/.../agent/EmergingTechAgentImpl.java

# Facade (세션 관리, 메시지 영속화, Agent 실행 오케스트레이션)
api/agent/src/main/java/.../facade/AgentFacade.java

# Controller (HTTP 엔드포인트)
api/agent/src/main/java/.../controller/AgentController.java

# 시스템 프롬프트 설정 (visualization 가이드 포함)
api/agent/src/main/java/.../config/AgentPromptConfig.java
```

### 데이터 집계 계층

```
# MongoDB Aggregation Pipeline (countByGroup, aggregateWordFrequency)
datasource/mongodb/src/main/java/.../service/EmergingTechAggregationService.java

# 집계 결과 타입
datasource/mongodb/src/main/java/.../service/GroupCountResult.java
datasource/mongodb/src/main/java/.../service/WordFrequencyResult.java

# 소스 도큐먼트 (통계 대상 필드: provider, source_type, update_type, published_at)
datasource/mongodb/src/main/java/.../document/EmergingTechDocument.java
```

### 관련 문서

```
# API 정의서 (프론트엔드 연동 스펙)
docs/reference/api-specifications/001-api-agent.md
```

### 프론트엔드 참고 (tech-n-ai-frontend/admin)

```
# 프론트엔드 프로젝트 구조: Next.js 16, React 19, TypeScript 5, Tailwind CSS 4
# 디자인 시스템: Neo-Brutalism (sharp edges, brutal-border, brutal-shadow)
# 차트 라이브러리: 현재 미도입 (Mermaid 텍스트 렌더링만 사용)
```

---

## 설계서에 포함할 내용

### 1. 응답 구조 설계

현재 `AgentExecutionResult`의 구조를 분석하고, 통계 데이터를 구조화하여 전달하는 방안을 설계하라.

고려할 선택지:

- **선택지 A**: `AgentExecutionResult`에 `chartData` 필드를 추가하여 구조화된 통계 JSON을 별도 전달
- **선택지 B**: `summary` 내에 구조화 데이터 블록을 임베딩 (예: 특정 구분자로 JSON 블록 삽입)
- **선택지 C**: Tool 실행 결과를 별도 필드로 수집하여 반환

각 선택지의 장단점을 분석하고, **하나를 선택하여 상세 설계**하라.

판단 기준:
- 프론트엔드 파싱 복잡도
- 기존 `summary` Markdown 렌더링 호환성
- LLM 응답 흐름에 대한 영향 최소화
- 구현 복잡도

### 2. 차트 데이터 포맷 설계

프론트엔드가 차트를 렌더링하기 위해 필요한 데이터 구조를 정의하라.

필수 포함 항목:
- 차트 유형 식별 (pie, bar 등)
- 데이터 포인트 (label + value 쌍)
- 메타데이터 (제목, 집계 기준, 기간, 총합)

설계 시 고려:
- `get_emerging_tech_statistics`와 `analyze_text_frequency` 두 Tool의 결과를 동일한 포맷으로 통합할 수 있는지
- 프론트엔드 차트 라이브러리(Recharts, Chart.js 등)에서 바로 사용할 수 있는 범용 포맷인지
- 향후 새로운 차트 유형 추가 시 확장 가능한지

### 3. Tool 반환값과 Agent 응답의 관계 설계

현재 LangChain4j의 `@Tool` 메서드 반환값은 LLM에게만 전달되고 프론트엔드에는 도달하지 않는다. Tool 실행 중 생성된 구조화 데이터를 프론트엔드까지 전달하는 메커니즘을 설계하라.

현재 흐름:
```
@Tool 반환값(StatisticsDto) → LangChain4j JSON 직렬화 → LLM 입력 → LLM 텍스트 출력 → summary
```

개선 흐름 (설계 필요):
```
@Tool 반환값(StatisticsDto) → LangChain4j JSON 직렬화 → LLM 입력
                            ↘ (사이드 채널) → 구조화 데이터 수집 → AgentExecutionResult
```

구체적으로:
- Tool 실행 시 구조화된 통계 데이터를 어디에 임시 저장할 것인가 (ThreadLocal, 별도 컨텍스트 객체 등)
- `EmergingTechAgentImpl.execute()` 완료 후 수집된 데이터를 `AgentExecutionResult`에 어떻게 포함할 것인가
- 여러 Tool이 여러 번 호출될 경우 데이터를 어떻게 누적할 것인가

### 4. StatisticsDto 개선

현재 `StatisticsDto`:
```java
public record StatisticsDto(
    String groupBy,
    String startDate,
    String endDate,
    long totalCount,
    List<GroupCount> groups,
    String message
) {
    public record GroupCount(String name, long count) {}
}
```

이 DTO를 프론트엔드 차트 렌더링에 적합하도록 개선하라.

고려 사항:
- LLM이 읽는 JSON 구조와 프론트엔드가 받는 구조가 동일해야 하는지, 별도 DTO가 필요한지
- 비율(percentage) 계산을 서버에서 할 것인지, 프론트엔드에 위임할 것인지
- 차트 유형 힌트(pie/bar)를 서버에서 결정할 것인지

### 5. AgentPromptConfig 시스템 프롬프트 수정

구조화 데이터 전달 방식이 변경되면, 시스템 프롬프트의 `visualization` 섹션도 수정이 필요할 수 있다. 변경이 필요한 경우 수정 방안을 제시하라.

### 6. API 정의서 업데이트 범위

`docs/reference/api-specifications/001-api-agent.md`에서 업데이트가 필요한 부분을 명시하라:
- `AgentExecutionResult` 필드 변경 사항
- 응답 예시 JSON 업데이트
- 프론트엔드 연동 가이드 추가

### 7. 마이그레이션 순서

단계별 구현 순서를 정의하라. 각 단계는 독립적으로 빌드·테스트 가능해야 한다.

### 8. 검증 체크리스트

- [ ] 기존 Agent 실행 기능 정상 동작 (summary Markdown 렌더링 유지)
- [ ] 통계 Tool 호출 시 구조화된 차트 데이터가 응답에 포함됨
- [ ] 프론트엔드에서 차트 데이터로 도표/그래프 렌더링 가능
- [ ] 차트 데이터 없는 응답(비-통계 요청)에서 기존 동작 유지
- [ ] 전체 프로젝트 빌드 성공: `./gradlew build`
- [ ] API 정의서와 실제 응답 일치

---

## 제약사항

### 필수 준수

1. **하위 호환성**: 기존 `AgentExecutionResult`의 모든 필드(`success`, `summary`, `sessionId`, `toolCallCount`, `analyticsCallCount`, `executionTimeMs`, `errors`)와 팩토리 메서드(`success()`, `failure()`)의 동작이 변경되어서는 안 된다. 프론트엔드가 차트 데이터를 활용하지 않더라도 기존 Markdown 렌더링이 정상 동작해야 한다.
2. **LLM 동작 보존**: `@Tool` 메서드의 반환값(LLM이 읽는 JSON)은 가능한 변경하지 않는다. LLM의 기존 Tool 활용 패턴이 깨지지 않아야 한다.
3. **의존성 방향 준수**: `API → Datasource → Common → Client` 방향만 허용. 순환 의존 금지.
4. **CQRS 패턴 유지**: 기존 Aurora(Command) + MongoDB(Query) 구조 유지.
5. **객체지향 설계 원칙 준수**: 단일 책임 원칙(SRP), 개방-폐쇄 원칙(OCP)을 준수하여 확장 가능한 설계를 한다.
6. **클린코드 원칙 준수**: 명확한 네이밍, 작은 메서드, 적절한 추상화 수준을 유지한다.

### 공식 참조 문서

외부 자료 참조 시 아래 공식 문서만 사용하라:

- **LangChain4j**: [https://docs.langchain4j.dev/](https://docs.langchain4j.dev/) — Tool 실행 결과 접근, AiServices 커스터마이징
- **Spring Boot 4**: [https://docs.spring.io/spring-boot/](https://docs.spring.io/spring-boot/) — ConfigurationProperties, REST 응답 구조
- **Jackson 3 (tools.jackson)**: [https://github.com/FasterXML/jackson](https://github.com/FasterXML/jackson) — JSON 직렬화
- **MongoDB Aggregation**: [https://www.mongodb.com/docs/manual/aggregation/](https://www.mongodb.com/docs/manual/aggregation/) — 집계 파이프라인
- **Recharts**: [https://recharts.org/](https://recharts.org/) — 프론트엔드 차트 라이브러리 데이터 구조 참고 (선택 사항)

비공식 블로그, 포럼, AI 생성 콘텐츠를 근거로 사용하지 않는다.

### 금지

1. **오버엔지니어링 금지**: 현재 필요하지 않은 추상화, 미래 대비 코드, 범용 차트 엔진 등을 설계하지 않는다. `get_emerging_tech_statistics`와 `analyze_text_frequency` 두 Tool에 집중한다.
2. **불필요한 리팩토링 금지**: 개선 대상이 아닌 코드(데이터 수집 Tool, Slack Tool 등)를 건드리지 않는다.
3. **구현 코드 작성 금지**: 설계서만 작성한다. 핵심 인터페이스 시그니처와 DTO 구조는 의사코드 또는 Java record 형태로 표현할 수 있다.
4. **프론트엔드 구현 금지**: 프론트엔드 차트 렌더링 구현은 범위 밖이다. 데이터 포맷만 정의한다.

---

## 출력 형식

Markdown 형식으로 다음 구조를 따르라:

```markdown
# Agent 통계 Tool 차트 데이터 응답 개선 설계서

## 1. 개요
- 목적, 현재 문제, 해결 방안 요약

## 2. 현재 구조 분석
- 데이터 흐름도 (Tool → LLM → summary → 프론트엔드)
- StatisticsDto / WordFrequencyDto 현재 구조

## 3. 응답 구조 설계
- 선택지 비교 분석표
- 선택한 방안의 상세 설계
- AgentExecutionResult 변경 사항

## 4. 차트 데이터 포맷
- 공통 차트 데이터 DTO 설계
- 통계 / 키워드 빈도별 매핑

## 5. Tool-to-Frontend 데이터 전달 메커니즘
- 데이터 수집 경로 설계
- ThreadLocal / 컨텍스트 객체 설계

## 6. DTO 변경 사항
- StatisticsDto 개선안
- 기존 LLM 호환성 영향 분석

## 7. AgentPromptConfig 수정안
- visualization 섹션 변경 (필요한 경우)

## 8. API 정의서 업데이트
- AgentExecutionResult 필드 변경
- 응답 예시 JSON

## 9. 마이그레이션 순서
- 단계별 구현 계획 (각 단계 독립 빌드/테스트 가능)

## 10. 검증 체크리스트
```

---

## 시작 지시

위 요구사항에 따라 설계서를 작성하라. 설계서 작성 전에 "현재 코드베이스 맥락"에 명시된 파일들을 모두 읽고 현재 구조를 이해한 뒤 시작하라.
