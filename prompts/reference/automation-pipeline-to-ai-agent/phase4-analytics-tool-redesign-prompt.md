# Phase 4: AI Agent Tool 재설계 - 데이터 분석 기능 전환 프롬프트

## 목표
기존 `api-agent` 모듈의 Tool 구성을 재설계한다.
의미가 낮은 `create_draft_post`, `publish_post` Tool을 제거하고, MongoDB Atlas에 저장된 EmergingTech 도큐먼트를 분석하여 통계/시각화 정보를 채팅 응답으로 제공하는 Tool로 대체한다.

## 전제 조건
- Phase 1~3 완료: 데이터 수집 파이프라인, LangChain4j Tool 래퍼, Agent 통합
- `api-agent` 모듈에 LangChain4j 1.10.0 + OpenAI GPT-4o-mini 설정 완료
- MongoDB Atlas `emerging_techs` 컬렉션에 데이터 수집 완료
- 기존 Tool: `fetch_github_releases`, `scrape_web_page`, `search_emerging_techs`, `create_draft_post`, `publish_post`, `send_slack_notification`

## 변경 범위

### 제거 대상 Tool
| Tool Name | 제거 사유 |
|-----------|-----------|
| `create_draft_post` | Agent가 자율적으로 포스트를 생성하는 것보다 데이터 분석 제공이 더 유의미 |
| `publish_post` | 게시 승인은 수동 관리가 적절, Agent의 자율 게시 불필요 |

### 신규 Tool
| Tool Name | 설명 | 입력 | 출력 |
|-----------|------|------|------|
| `get_emerging_tech_statistics` | Provider/SourceType/기간별 통계 집계 | groupBy, startDate, endDate | StatisticsDto (JSON) |
| `analyze_text_frequency` | title/summary 텍스트 빈도 분석 | provider, startDate, endDate, topN | WordFrequencyDto (JSON) |

### 유지 Tool
| Tool Name | 유지 사유 |
|-----------|-----------|
| `fetch_github_releases` | GitHub 릴리스 조회는 실시간 데이터 수집에 유용 |
| `scrape_web_page` | 웹 크롤링은 다양한 분석 시나리오에 활용 가능 |
| `search_emerging_techs` | 기존 데이터 검색은 분석의 기본 기능 |
| `send_slack_notification` | 분석 결과 알림 전송에 활용 |

## 설계서에 포함할 내용

### 1. MongoDB Aggregation 서비스 설계

`domain-mongodb` 모듈에 EmergingTech 집계 전용 서비스를 추가한다.

```java
/**
 * EmergingTech 데이터 집계 서비스
 */
@Service
@RequiredArgsConstructor
public class EmergingTechAggregationService {

    private final MongoTemplate mongoTemplate;

    /**
     * 그룹별 도큐먼트 수 집계
     * @param groupField 그룹 기준 필드 (provider, source_type, update_type)
     * @param startDate 조회 시작일 (nullable)
     * @param endDate 조회 종료일 (nullable)
     * @return 그룹별 집계 결과
     */
    public List<GroupCountResult> countByGroup(String groupField, LocalDateTime startDate, LocalDateTime endDate) {
        Criteria criteria = buildDateCriteria(startDate, endDate);

        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(criteria),
            Aggregation.group(groupField).count().as("count"),
            Aggregation.sort(Sort.Direction.DESC, "count")
        );

        return mongoTemplate.aggregate(aggregation, "emerging_techs", GroupCountResult.class)
            .getMappedResults();
    }

    /**
     * title, summary 텍스트 조회 (빈도 분석용)
     */
    public List<TextContentResult> fetchTextContent(String provider, LocalDateTime startDate, LocalDateTime endDate) {
        Criteria criteria = buildDateCriteria(startDate, endDate);
        if (provider != null && !provider.isBlank()) {
            criteria = criteria.and("provider").is(provider);
        }

        Query query = new Query(criteria);
        query.fields().include("title").include("summary");

        return mongoTemplate.find(query, TextContentResult.class, "emerging_techs");
    }
}
```

**설계 포인트:**
- `MongoTemplate`의 `Aggregation` API 사용 (Spring Data MongoDB 공식 지원)
- 날짜 범위 필터는 `published_at` 필드 기준
- `GroupCountResult`, `TextContentResult`는 단순한 projection DTO로 정의

### 2. 통계 분석 Tool 설계

#### 2.1 StatisticsDto 정의

```java
/**
 * 통계 집계 결과 DTO
 * LangChain4j가 JSON 직렬화하여 LLM에게 전달
 */
public record StatisticsDto(
    String groupBy,
    String startDate,
    String endDate,
    long totalCount,
    List<GroupCount> groups
) {
    public record GroupCount(
        String name,
        long count
    ) {}
}
```

#### 2.2 get_emerging_tech_statistics Tool 구현

```java
@Tool(name = "get_emerging_tech_statistics",
      value = "조회 기간 기준으로 EmergingTech 데이터를 Provider, SourceType, UpdateType별로 집계합니다. "
            + "결과를 도표나 차트로 정리하여 보여줄 수 있습니다.")
public StatisticsDto getStatistics(
    @P("집계 기준 필드: provider, source_type, update_type") String groupBy,
    @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
    @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate
) {
    // 입력 검증
    // Adapter를 통해 AggregationService 호출
    // StatisticsDto로 변환 후 반환
}
```

**LLM 활용 방식:**
- Tool이 StatisticsDto(JSON)를 반환하면, LLM이 이를 분석하여 Markdown 표, ASCII 막대 차트 등으로 포맷팅
- LLM은 데이터를 해석하여 비교 분석 코멘트도 추가 가능

**예상 채팅 응답 형식:**

```
## Provider별 수집 현황 (2024-01-01 ~ 2024-12-31)

| Provider   | 수집 건수 | 비율    |
|------------|----------|---------|
| OPENAI     | 145      | 35.2%   |
| ANTHROPIC  | 98       | 23.8%   |
| GOOGLE     | 87       | 21.1%   |
| META       | 52       | 12.6%   |
| XAI        | 30       | 7.3%    |

OpenAI      ████████████████████ 145
Anthropic   █████████████░░░░░░░  98
Google      ████████████░░░░░░░░  87
Meta        ███████░░░░░░░░░░░░░  52
xAI         ████░░░░░░░░░░░░░░░░  30

📊 분석: OpenAI가 전체의 35.2%로 가장 높은 비중을 차지합니다.
```

### 3. 텍스트 빈도 분석 Tool 설계

#### 3.1 WordFrequencyDto 정의

```java
/**
 * 텍스트 빈도 분석 결과 DTO
 */
public record WordFrequencyDto(
    int totalDocuments,
    long totalWords,
    int uniqueWords,
    String period,
    List<WordCount> topWords
) {
    public record WordCount(
        String word,
        int count,
        double percentage
    ) {}
}
```

#### 3.2 analyze_text_frequency Tool 구현

```java
@Tool(name = "analyze_text_frequency",
      value = "EmergingTech 도큐먼트의 title, summary에서 주요 키워드 빈도를 분석합니다. "
            + "Word Cloud 형태로 결과를 정리할 수 있습니다.")
public WordFrequencyDto analyzeTextFrequency(
    @P("Provider 필터 (OPENAI, ANTHROPIC 등, 빈 문자열이면 전체)") String provider,
    @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String startDate,
    @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 전체 기간)") String endDate,
    @P("상위 키워드 개수 (기본값 20)") int topN
) {
    // 1. MongoDB에서 title, summary 텍스트 조회
    // 2. 텍스트 토큰화 (공백 분리, 소문자 변환)
    // 3. 불용어(stopword) 제거 (영문 일반 불용어 + 한글 조사)
    // 4. 빈도 집계 (Map<String, Integer>)
    // 5. 상위 N개 추출 후 WordFrequencyDto 반환
}
```

**불용어 처리:**
- 영문: "the", "a", "is", "in", "to", "and", "of", "for" 등 일반 불용어
- 단어 길이 2 미만 제외
- 숫자만으로 구성된 토큰 제외
- 별도 불용어 목록은 `Set<String>` 상수로 관리

**예상 채팅 응답 형식:**

```
## 키워드 빈도 분석 (전체 Provider, 2024-01-01 ~ 2024-12-31)

총 분석 도큐먼트: 412건 | 총 단어 수: 45,230 | 고유 단어: 3,847

### TOP 15 키워드

 1. model       ████████████████████ 312 (4.2%)
 2. release     ██████████████░░░░░░ 218 (2.9%)
 3. api         ████████████░░░░░░░░ 187 (2.5%)
 4. update      ██████████░░░░░░░░░░ 156 (2.1%)
 5. performance ████████░░░░░░░░░░░░ 134 (1.8%)
 ...

주요 키워드: model, release, api가 상위를 차지하며,
최근 AI 모델 출시와 API 업데이트에 대한 콘텐츠가 집중되어 있습니다.
```

### 4. Tool Adapter 설계

기존 Adapter 패턴을 따라 신규 Tool용 Adapter를 추가한다.

```
Tool Method → Adapter → AggregationService (domain-mongodb)

get_emerging_tech_statistics() → AnalyticsToolAdapter → EmergingTechAggregationService
analyze_text_frequency()      → AnalyticsToolAdapter → EmergingTechAggregationService
```

```java
/**
 * 분석 기능을 LangChain4j Tool 형식으로 래핑하는 어댑터
 */
@Component
@RequiredArgsConstructor
public class AnalyticsToolAdapter {

    private final EmergingTechAggregationService aggregationService;

    public StatisticsDto getStatistics(String groupBy, LocalDate startDate, LocalDate endDate) {
        // AggregationService 호출 → StatisticsDto 변환
    }

    public WordFrequencyDto analyzeTextFrequency(String provider, LocalDate startDate, LocalDate endDate, int topN) {
        // AggregationService에서 텍스트 조회 → 빈도 분석 → WordFrequencyDto 반환
    }
}
```

### 5. 입력 검증 추가

`ToolInputValidator`에 신규 검증 메서드를 추가한다.

```java
/**
 * 집계 기준 필드 검증
 */
public static String validateGroupByField(String groupBy) {
    Set<String> VALID_GROUP_FIELDS = Set.of("provider", "source_type", "update_type");
    // groupBy가 유효한 필드인지 검증
}

/**
 * 날짜 형식 검증 (YYYY-MM-DD, 빈 문자열 허용)
 */
public static String validateDateOptional(String date, String fieldName) {
    // 빈 문자열이면 null 반환 (전체 기간)
    // YYYY-MM-DD 형식 검증
}
```

### 6. System Prompt 수정

`AgentPromptConfig`의 프롬프트를 수정하여 분석 기능을 안내한다.

```yaml
agent:
  prompt:
    role: "당신은 Emerging Tech 데이터 분석 및 업데이트 추적 전문가입니다."
    tools: |
      - fetch_github_releases: GitHub 저장소 릴리스 조회
      - scrape_web_page: 웹 페이지 크롤링
      - search_emerging_techs: 기존 업데이트 검색
      - get_emerging_tech_statistics: Provider/SourceType/기간별 통계 집계
      - analyze_text_frequency: 키워드 빈도 분석 (Word Cloud)
      - send_slack_notification: Slack 알림 전송
    rules: |
      1. 통계 요청 시 get_emerging_tech_statistics로 데이터를 집계하고, Markdown 표와 ASCII 차트로 보기 쉽게 정리
      2. 키워드 분석 요청 시 analyze_text_frequency로 빈도를 집계하고, 막대 차트와 해석을 함께 제공
      3. 데이터 수집 요청 시 fetch_github_releases, scrape_web_page 활용
      4. 중복 확인은 search_emerging_techs 사용
      5. 결과 공유 시 send_slack_notification 활용
      6. 작업 완료 후 결과 요약 제공
```

### 7. 모듈 의존성 변경

`api-agent`에 `domain-mongodb` 의존성을 추가한다.

```gradle
// api/agent/build.gradle
dependencies {
    implementation project(':domain-mongodb')  // 추가: MongoDB Aggregation 서비스 사용
    // 기존 의존성 유지
    implementation project(':common-core')
    implementation project(':common-exception')
    implementation project(':client-feign')
    implementation project(':client-slack')
    implementation project(':client-scraper')
}
```

`ServerConfig`에 MongoDB 관련 ComponentScan 추가:

```java
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.agent",
    "com.tech.n.ai.domain.mongodb",  // 추가
    "com.tech.n.ai.client.feign",
    "com.tech.n.ai.client.slack",
    "com.tech.n.ai.client.scraper",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.exception"
})
```

`application.yml`에 MongoDB 프로파일 추가:

```yaml
spring:
  profiles:
    include:
      - common-core
      - agent-api
      - mongodb-domain  # 추가
      - feign-github
      - feign-internal
      - slack
      - scraper
```

### 8. 삭제 대상 코드

다음 파일/코드를 삭제한다:

| 파일/위치 | 삭제 내용 |
|-----------|-----------|
| `EmergingTechAgentTools.java` | `createDraftPost()`, `publishPost()` 메서드 |
| `EmergingTechToolAdapter.java` | `createDraft()`, `publish()` 메서드 |
| `ToolErrorHandlers.java` | `handleHallucinatedToolName()`의 Tool 목록 업데이트 |

### 9. Agent 행동 흐름 예시

```
User: "최근 3개월간 Provider별 수집 현황을 분석해줘"

Agent 추론:
1. "통계 데이터를 집계해야 해"
   → Tool: get_emerging_tech_statistics("provider", "2024-10-01", "2025-01-01")
   → 결과: { totalCount: 412, groups: [{name:"OPENAI", count:145}, ...] }

2. "결과를 보기 쉽게 표와 차트로 정리할게"
   → Markdown 표 + ASCII 막대 차트 생성

Agent 응답:
"## Provider별 수집 현황 (2024-10 ~ 2025-01)
| Provider | 건수 | 비율 |
|----------|------|------|
| OPENAI | 145 | 35.2% |
..."
```

```
User: "올해 수집된 데이터의 주요 키워드를 Word Cloud로 보여줘"

Agent 추론:
1. "키워드 빈도 분석이 필요해"
   → Tool: analyze_text_frequency("", "2025-01-01", "2025-12-31", 20)
   → 결과: { totalDocuments: 412, topWords: [{word:"model", count:312}, ...] }

2. "Word Cloud 형태로 시각화해서 보여줄게"
   → ASCII 막대 차트 + 분석 코멘트 생성

Agent 응답:
"## 키워드 빈도 분석 (2025년)
 1. model       ████████████████████ 312
 2. release     ██████████████░░░░░░ 218
..."
```

### 10. LangChain4j 기술 검증 포인트

| 검증 항목 | 결과 | 근거 |
|-----------|------|------|
| Tool에서 복잡한 DTO(record) 반환 가능? | **가능** | LangChain4j 1.10.0은 Gson으로 자동 JSON 직렬화 |
| LLM이 JSON 데이터를 Markdown 표로 변환 가능? | **가능** | GPT-4o-mini는 구조화된 데이터 포맷팅에 우수 |
| LLM이 ASCII 차트 생성 가능? | **가능** | Unicode 블록 문자(█, ░)를 활용한 막대 차트 생성 가능 |
| MongoDB Aggregation과 LangChain4j 통합? | **가능** | Tool이 Spring Data MongoDB Aggregation 호출 후 DTO 반환 |

## 제약 조건
- 오버엔지니어링 금지: 필요한 집계/분석 기능만 구현
- 모든 설계 및 구현은 객체지향 설계 기법, SOLID 원칙, 클린코드 원칙을 준수
- 최소한의 한글 주석 추가
- 외부 자료는 반드시 신뢰할 수 있는 공식 출처만 참고
- 기존 Tool Adapter 패턴, 입력 검증 패턴, ThreadLocal 메트릭 패턴 유지
- 텍스트 빈도 분석은 서버 사이드 Java 코드로 처리 (LLM에 전체 텍스트를 전달하지 않음)
- 집계 결과의 시각화(표, 차트)는 LLM의 텍스트 생성 능력에 위임

## 산출물
1. 설계서: `/docs/reference/automation-pipeline-to-ai-agent/phase4-analytics-tool-redesign-design.md`
   - MongoDB Aggregation 서비스 설계
   - 신규 Tool 정의 및 DTO 설계
   - AnalyticsToolAdapter 설계
   - 입력 검증 추가
   - System Prompt 수정
   - 모듈 의존성 변경
   - 삭제 대상 코드 목록
   - 시퀀스 다이어그램

## 참고 자료
- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- Spring Data MongoDB Aggregation: https://docs.spring.io/spring-data/mongodb/reference/mongodb/aggregation-framework.html
- MongoDB Aggregation Pipeline: https://www.mongodb.com/docs/manual/core/aggregation-pipeline/
- LangChain4j OpenAI: https://docs.langchain4j.dev/integrations/language-models/open-ai

## 현재 코드 참조
- 기존 Tool 패턴: `/api/agent/src/main/java/.../tool/EmergingTechAgentTools.java`
- 기존 Adapter 패턴: `/api/agent/src/main/java/.../tool/adapter/EmergingTechToolAdapter.java`
- MongoDB Document: `/domain/mongodb/src/main/java/.../document/EmergingTechDocument.java`
- MongoDB Repository: `/domain/mongodb/src/main/java/.../repository/EmergingTechRepository.java`
- MongoTemplate 사용 예시: `/api/emerging-tech/src/main/java/.../service/EmergingTechQueryServiceImpl.java`
- 입력 검증: `/api/agent/src/main/java/.../tool/validation/ToolInputValidator.java`
- 프롬프트 설정: `/api/agent/src/main/java/.../config/AgentPromptConfig.java`
