# Phase 4 개선: Word Cloud 텍스트 빈도 분석 - update_type, source_type 필터 추가 프롬프트

## 목표
`analyze_text_frequency` Tool과 `EmergingTechAggregationService.aggregateWordFrequency()`에 **update_type, source_type 필터**를 추가한다.
현재 provider 필터만 지원하여 update_type별(예: SDK_RELEASE만의 키워드), source_type별(예: RSS 소스만의 키워드) Word Cloud를 생성할 수 없는 문제를 해결한다.

## 전제 조건
- Phase 4 설계 및 구현 완료 (분석 Tool 재설계)
- Phase 5 설계 및 구현 완료 (데이터 수집 Agent)
- `analyze_text_frequency` Tool이 provider 필터만 지원하는 상태

## 현재 상태 (AS-IS)

### analyze_text_frequency Tool 파라미터
```java
@Tool(name = "analyze_text_frequency", ...)
public WordFrequencyDto analyzeTextFrequency(
    @P("Provider 필터 (OPENAI, ANTHROPIC 등, 빈 문자열이면 전체)") String provider,
    @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
    @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate,
    @P("상위 키워드 개수 (기본값 20)") int topN
)
```

### EmergingTechAggregationService.aggregateWordFrequency()
```java
public List<WordFrequencyResult> aggregateWordFrequency(
    String provider, LocalDateTime startDate, LocalDateTime endDate,
    List<String> stopWords, int topN)
```

**문제**: provider 필터만 존재. update_type, source_type 필터 없음.

## 변경 범위 (TO-BE)

### 변경 대상 파일

| 파일 | 변경 내용 |
|------|-----------|
| `EmergingTechAggregationService.java` | `aggregateWordFrequency()`에 updateType, sourceType 파라미터 추가 |
| `EmergingTechAggregationService.java` | `countDocuments()`에 updateType, sourceType 파라미터 추가 |
| `EmergingTechAgentTools.java` | `analyzeTextFrequency()`에 updateType, sourceType 파라미터 추가 |
| `AnalyticsToolAdapter.java` | `analyzeTextFrequency()`에 updateType, sourceType 파라미터 전달 |
| `ToolInputValidator.java` | `validateUpdateTypeOptional()` 메서드 추가, `validateSourceTypeOptional()` 메서드 추가 |

### 변경하지 않는 파일

| 파일 | 사유 |
|------|------|
| `WordFrequencyDto.java` | 응답 DTO 구조 변경 불필요 (필터 조건은 입력이지 출력이 아님) |
| `WordFrequencyResult.java` | MongoDB 집계 결과 DTO 변경 불필요 |
| `AnalyticsConfig.java` | 불용어 설정 변경 불필요 |
| `AgentPromptConfig.java` | Tool 설명은 @Tool.value에서 충분히 안내됨 |

## 설계서에 포함할 내용

### 1. EmergingTechAggregationService 변경

#### 1.1 aggregateWordFrequency() 시그니처 변경

```java
/**
 * 서버사이드 텍스트 빈도 집계
 * provider, updateType, sourceType 조합으로 필터링 가능
 */
public List<WordFrequencyResult> aggregateWordFrequency(
        String provider, String updateType, String sourceType,
        LocalDateTime startDate, LocalDateTime endDate,
        List<String> stopWords, int topN)
```

#### 1.2 필터 조건 추가

기존 `buildDateCriteria()` 이후 provider 필터만 적용하던 로직에 updateType, sourceType 필터를 추가한다.

```java
Criteria criteria = buildDateCriteria(startDate, endDate);
if (provider != null && !provider.isBlank()) {
    criteria = criteria.and("provider").is(provider);
}
if (updateType != null && !updateType.isBlank()) {
    criteria = criteria.and("update_type").is(updateType);
}
if (sourceType != null && !sourceType.isBlank()) {
    criteria = criteria.and("source_type").is(sourceType);
}
```

#### 1.3 countDocuments() 시그니처 변경

```java
public long countDocuments(String provider, String updateType, String sourceType,
                           LocalDateTime startDate, LocalDateTime endDate)
```

동일한 필터 조건 적용.

### 2. EmergingTechAgentTools 변경

#### 2.1 analyze_text_frequency Tool 파라미터 추가

```java
@Tool(name = "analyze_text_frequency",
      value = "EmergingTech 도큐먼트의 title, summary에서 주요 키워드 빈도를 분석합니다. "
            + "Provider, UpdateType, SourceType으로 필터링할 수 있습니다. "
            + "Mermaid 차트나 Word Cloud 형태로 결과를 정리할 수 있습니다.")
public WordFrequencyDto analyzeTextFrequency(
    @P("Provider 필터 (OPENAI, ANTHROPIC, GOOGLE, META, XAI 또는 빈 문자열이면 전체)") String provider,
    @P("UpdateType 필터 (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, PRODUCT_LAUNCH, PLATFORM_UPDATE, BLOG_POST 또는 빈 문자열이면 전체)") String updateType,
    @P("SourceType 필터 (GITHUB_RELEASE, RSS, WEB_SCRAPING 또는 빈 문자열이면 전체)") String sourceType,
    @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
    @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate,
    @P("상위 키워드 개수 (기본값 20)") int topN
)
```

#### 2.2 입력 검증 추가

기존 provider, startDate, endDate 검증에 updateType, sourceType 검증을 추가한다.

```java
String updateTypeError = ToolInputValidator.validateUpdateTypeOptional(updateType);
if (updateTypeError != null) {
    metrics().incrementValidationError();
    return new WordFrequencyDto(0, "", List.of(), List.of());
}

String sourceTypeError = ToolInputValidator.validateSourceTypeOptional(sourceType);
if (sourceTypeError != null) {
    metrics().incrementValidationError();
    return new WordFrequencyDto(0, "", List.of(), List.of());
}
```

### 3. AnalyticsToolAdapter 변경

#### 3.1 analyzeTextFrequency() 시그니처 변경

```java
public WordFrequencyDto analyzeTextFrequency(
        String provider, String updateType, String sourceType,
        String startDate, String endDate, int topN)
```

updateType, sourceType를 `EmergingTechAggregationService`에 그대로 전달한다.

### 4. ToolInputValidator 변경

#### 4.1 validateUpdateTypeOptional() 추가

```java
private static final Set<String> VALID_SOURCE_TYPES = Set.of(
    "GITHUB_RELEASE", "RSS", "WEB_SCRAPING");

/**
 * UpdateType 검증 (선택적, 빈 문자열 허용)
 */
public static String validateUpdateTypeOptional(String updateType) {
    if (updateType == null || updateType.isBlank()) {
        return null;
    }
    return validateEnum(updateType, "updateType", VALID_UPDATE_TYPES);
}

/**
 * SourceType 검증 (선택적, 빈 문자열 허용)
 */
public static String validateSourceTypeOptional(String sourceType) {
    if (sourceType == null || sourceType.isBlank()) {
        return null;
    }
    return validateEnum(sourceType, "sourceType", VALID_SOURCE_TYPES);
}
```

**참고**: `VALID_UPDATE_TYPES`는 이미 존재함. `VALID_SOURCE_TYPES`만 신규 추가.

### 5. ToolErrorHandlers 변경

`handleHallucinatedToolName()`의 Tool 목록 문자열은 변경 불필요. (Tool 이름 변경 없음, 파라미터만 추가)

## emerging_techs 도큐먼트 필드 참조

```
provider     : "OPENAI" | "ANTHROPIC" | "GOOGLE" | "META" | "XAI"
update_type  : "MODEL_RELEASE" | "API_UPDATE" | "SDK_RELEASE" | "PRODUCT_LAUNCH" | "PLATFORM_UPDATE" | "BLOG_POST"
source_type  : "GITHUB_RELEASE" | "RSS" | "WEB_SCRAPING"
title        : String (Word Cloud 분석 대상)
summary      : String (Word Cloud 분석 대상)
```

## Agent 행동 흐름 예시

### provider별 Word Cloud (기존 동작 유지)
```
User: "OpenAI 키워드를 분석해줘"
Agent → analyze_text_frequency("OPENAI", "", "", "", "", 20)
```

### update_type별 Word Cloud (신규)
```
User: "SDK 릴리스에서 많이 나오는 키워드가 뭐야?"
Agent → analyze_text_frequency("", "SDK_RELEASE", "", "", "", 20)
```

### source_type별 Word Cloud (신규)
```
User: "RSS에서 수집된 기사의 키워드 트렌드를 보여줘"
Agent → analyze_text_frequency("", "", "RSS", "", "", 20)
```

### 복합 필터 Word Cloud (신규)
```
User: "Anthropic의 블로그 포스트 키워드를 분석해줘"
Agent → analyze_text_frequency("ANTHROPIC", "BLOG_POST", "", "", "", 20)
```

## 제약 조건
- 오버엔지니어링 금지: 파라미터 추가와 필터 조건 추가만 수행
- 기존 API 호환성: 빈 문자열은 "전체" 필터로 동작하므로 기존 호출 패턴 유지
- 기존 코드 패턴(Adapter 패턴, 입력 검증 패턴, ThreadLocal 메트릭) 유지
- 최소한의 한글 주석 추가

## 구현 순서

| 단계 | 작업 | 파일 |
|------|------|------|
| 1 | `VALID_SOURCE_TYPES` 상수 및 `validateUpdateTypeOptional()`, `validateSourceTypeOptional()` 추가 | `ToolInputValidator.java` |
| 2 | `aggregateWordFrequency()`, `countDocuments()` 시그니처 변경 및 필터 조건 추가 | `EmergingTechAggregationService.java` |
| 3 | `analyzeTextFrequency()` 시그니처 변경 및 파라미터 전달 | `AnalyticsToolAdapter.java` |
| 4 | `analyzeTextFrequency()` Tool 파라미터 추가 및 검증 로직 추가 | `EmergingTechAgentTools.java` |
| 5 | 빌드 확인 | `./gradlew :api-agent:compileJava` |

## 산출물
1. 설계서: Phase 4 설계서(`phase4-analytics-tool-redesign-design.md`) 내 해당 섹션 업데이트
   - `EmergingTechAggregationService` 시그니처 변경
   - `analyze_text_frequency` Tool 파라미터 변경
   - `AnalyticsToolAdapter` 시그니처 변경
   - `ToolInputValidator` 검증 메서드 추가

## 참고 자료
- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- Spring Data MongoDB Aggregation: https://docs.spring.io/spring-data/mongodb/reference/mongodb/aggregation-framework.html
- MongoDB Aggregation Pipeline: https://www.mongodb.com/docs/manual/core/aggregation-pipeline/

## 현재 코드 참조
- Tool 정의: `/api/agent/src/main/java/.../tool/EmergingTechAgentTools.java`
- Adapter: `/api/agent/src/main/java/.../tool/adapter/AnalyticsToolAdapter.java`
- Aggregation 서비스: `/domain/mongodb/src/main/java/.../service/EmergingTechAggregationService.java`
- 입력 검증: `/api/agent/src/main/java/.../tool/validation/ToolInputValidator.java`
- MongoDB Document: `/domain/mongodb/src/main/java/.../document/EmergingTechDocument.java`
- Enum 참조: `/domain/mongodb/src/main/java/.../enums/EmergingTechType.java`, `SourceType.java`, `TechProvider.java`
