# Phase 6: AI Agent 조회 Tool 개선 및 Slack 비활성화 프롬프트

## 1. 검증 대상

### 1.1 설계서
- `docs/reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase4-analytics-tool-redesign-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase5-data-collection-agent-design.md`

### 1.2 대상 모듈
- `api/agent` (AI Agent 모듈, 포트 8086)

### 1.3 참조 모듈
- `api/emerging-tech` (Emerging Tech API 모듈, 포트 8082)
- `domain/mongodb` (MongoDB 도메인 모듈)

---

## 2. 검증 결과 요약

### 2.1 현재 api-agent Tool 구성

| Tool | 기능 | 카테고리 |
|------|------|---------|
| `fetch_github_releases` | GitHub 릴리스 조회 | 조회 |
| `scrape_web_page` | 웹 페이지 크롤링 | 조회 |
| `search_emerging_techs` | 키워드 기반 검색 (query + provider 필터) | 조회 |
| `get_emerging_tech_statistics` | Provider/SourceType/UpdateType별 통계 집계 | 분석 |
| `analyze_text_frequency` | 키워드 빈도 분석 | 분석 |
| `send_slack_notification` | Slack 알림 발송 | 알림 |
| `collect_github_releases` | GitHub 릴리스 수집+저장 | 수집 |
| `collect_rss_feeds` | RSS 수집+저장 | 수집 |
| `collect_scraped_articles` | 크롤링 수집+저장 | 수집 |

### 2.2 현재 api-emerging-tech 조회 API

| Method | Path | 기능 | 주요 파라미터 |
|--------|------|------|-------------|
| GET | `/api/v1/emerging-tech` | 목록 조회 | provider, updateType, status, page, size, sort |
| GET | `/api/v1/emerging-tech/{id}` | 상세 조회 | id |
| GET | `/api/v1/emerging-tech/search` | 제목 키워드 검색 | q, page, size |

### 2.3 검증 결과: GAP 분석

| # | 검증 항목 | 결과 | 상세 |
|---|----------|------|------|
| G1 | 목록 조회 Tool 존재 여부 | **미충족** | published_at 기간 필터, provider/update_type/source_type 필터 조합, 페이징, 정렬이 포함된 목록 조회 Tool 없음 |
| G2 | 상세 조회 Tool 존재 여부 | **미충족** | ID 기반 단건 상세 조회 Tool 없음 |
| G3 | 검색 Tool 기능 동등성 | **부분 충족** | `search_emerging_techs`가 query + provider 필터만 지원. api-emerging-tech의 제목 검색과 유사하나, published_at 기간 필터, source_type 필터 미지원 |
| G4 | Slack Tool 비활성화 | **미충족** | `send_slack_notification`이 실제 Slack 발송을 시도함. 비활성화 필요 |

---

## 3. 개선 요구 사항

### 3.1 신규 Tool 추가: `list_emerging_techs` (목록 조회)

#### 요구 배경
- 관리자가 자연어로 "최근 1주일간 OpenAI의 SDK_RELEASE 목록을 보여줘", "ANTHROPIC의 BLOG_POST를 최신순으로 10개 조회해줘" 등의 요청을 할 수 있어야 함
- `api-emerging-tech` 컨트롤러의 `GET /api/v1/emerging-tech` 엔드포인트와 동등한 기능 제공

#### Tool 정의

```java
@Tool(name = "list_emerging_techs",
      value = "MongoDB Atlas emerging_techs 컬렉션에서 조건에 맞는 도큐먼트 목록을 조회합니다. "
            + "published_at 기준 기간 필터, provider, update_type, source_type, status 필터를 조합할 수 있습니다.")
public EmergingTechListDto listEmergingTechs(
    @P("조회 시작일 (YYYY-MM-DD 형식, 빈 문자열이면 제한 없음)") String startDate,
    @P("조회 종료일 (YYYY-MM-DD 형식, 빈 문자열이면 제한 없음)") String endDate,
    @P("Provider 필터 (OPENAI, ANTHROPIC, GOOGLE, META, XAI 또는 빈 문자열이면 전체)") String provider,
    @P("UpdateType 필터 (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, PRODUCT_LAUNCH, PLATFORM_UPDATE, BLOG_POST 또는 빈 문자열이면 전체)") String updateType,
    @P("SourceType 필터 (GITHUB_RELEASE, RSS, WEB_SCRAPING 또는 빈 문자열이면 전체)") String sourceType,
    @P("Status 필터 (DRAFT, PENDING, PUBLISHED, REJECTED 또는 빈 문자열이면 전체)") String status,
    @P("페이지 번호 (1부터 시작, 기본값 1)") int page,
    @P("페이지 크기 (기본값 20, 최대 100)") int size
)
```

#### 구현 방식 선택

두 가지 방식 중 택일:

**방식 A: Feign Client 경유 (기존 패턴 준수)**
- `EmergingTechInternalContract`에 목록 조회 API 추가
- Agent → Feign Client → api-emerging-tech 컨트롤러 → MongoDB
- 장점: 기존 아키텍처 패턴 일관성 유지, api-emerging-tech 모듈의 비즈니스 로직 재사용
- 단점: 네트워크 홉 추가

**방식 B: MongoDB 직접 조회 (domain-mongodb 모듈 활용)**
- Agent → domain-mongodb의 MongoTemplate/Repository → MongoDB
- 장점: 네트워크 홉 없음, 성능 우수
- 단점: api-emerging-tech의 비즈니스 로직(정렬 검증, 필터 검증)을 Agent에서 중복 구현

**권장: 방식 A (Feign Client 경유)**
- 기존 `EmergingTechToolAdapter`가 `EmergingTechInternalContract` Feign Client를 통해 api-emerging-tech를 호출하는 패턴을 따름
- `search_emerging_techs`도 이 패턴을 사용 중
- Phase 4의 `get_emerging_tech_statistics`와 `analyze_text_frequency`만 domain-mongodb 직접 접근을 사용 (이는 통계 집계용으로 별도 서비스가 필요했기 때문)

#### 반환 DTO

```java
/**
 * 목록 조회 결과 DTO
 */
public record EmergingTechListDto(
    int totalCount,       // 전체 건수
    int page,             // 현재 페이지
    int size,             // 페이지 크기
    int totalPages,       // 전체 페이지 수
    String period,        // 조회 기간 (예: "2025-01-01 ~ 2025-01-31")
    List<EmergingTechDto> items  // 목록 (기존 EmergingTechDto 재사용)
) {}
```

#### EmergingTechInternalContract 변경

기존 Feign Contract에 목록 조회 API 추가:

```java
// EmergingTechInternalContract.java에 추가
@GetMapping("/api/v1/emerging-tech")
ApiResponse<Object> listEmergingTechs(
    @RequestHeader("X-Internal-Api-Key") String apiKey,
    @RequestParam(required = false) String provider,
    @RequestParam(required = false) String updateType,
    @RequestParam(required = false) String status,
    @RequestParam(required = false) Integer page,
    @RequestParam(required = false) Integer size,
    @RequestParam(required = false) String sort
);
```

#### EmergingTechToolAdapter 변경

`list()` 메서드 추가:

```java
/**
 * 목록 조회 (필터 + 페이징)
 */
public EmergingTechListDto list(String startDate, String endDate,
                                 String provider, String updateType,
                                 String sourceType, String status,
                                 int page, int size) {
    // 1. Feign Client로 api-emerging-tech 호출
    // 2. 응답을 EmergingTechListDto로 변환
    // 3. 에러 시 빈 결과 반환
}
```

#### 추가 검토 사항: published_at 기간 필터

현재 `api-emerging-tech`의 목록 조회 API(`EmergingTechListRequest`)에는 **published_at 기간 필터(startDate, endDate)가 없음**.

**필요 조치**: `api-emerging-tech` 모듈의 목록 조회 API에 published_at 기간 필터를 추가해야 함.

1. `EmergingTechListRequest`에 `startDate`, `endDate` 필드 추가
2. `EmergingTechQueryServiceImpl.buildFilterCriteria()`에 날짜 범위 조건 추가
3. `EmergingTechFacade.getEmergingTechList()`에서 날짜 파라미터 전달

```java
// EmergingTechListRequest에 추가
public record EmergingTechListRequest(
    // ... 기존 필드 ...
    String startDate,    // 조회 시작일 (YYYY-MM-DD)
    String endDate,      // 조회 종료일 (YYYY-MM-DD)
    String sourceType    // 필터: SourceType enum value (추가)
) {}
```

```java
// EmergingTechQueryServiceImpl.buildFilterCriteria()에 추가
if (sourceType != null) {
    criteria = criteria.and("source_type").is(sourceType);
}
// published_at 기간 필터
if (startDate != null) {
    LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
    criteria = criteria.and("published_at").gte(start);
}
if (endDate != null) {
    LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
    criteria = criteria.and("published_at").lte(end);
}
```

---

### 3.2 신규 Tool 추가: `get_emerging_tech_detail` (상세 조회)

#### 요구 배경
- 관리자가 목록에서 특정 항목의 상세 정보를 확인하고자 할 때 사용
- "이 항목의 전체 내용을 보여줘", "ID xxx의 상세 정보를 알려줘" 등의 자연어 요청 처리

#### Tool 정의

```java
@Tool(name = "get_emerging_tech_detail",
      value = "Emerging Tech 도큐먼트의 상세 정보를 ID로 조회합니다. "
            + "목록 조회나 검색 결과에서 얻은 ID를 사용합니다.")
public EmergingTechDetailDto getEmergingTechDetail(
    @P("조회할 도큐먼트 ID (MongoDB ObjectId)") String id
)
```

#### 반환 DTO

```java
/**
 * 상세 조회 결과 DTO
 * EmergingTechDto보다 summary, publishedAt, sourceType, metadata 등 추가 정보 포함
 */
public record EmergingTechDetailDto(
    String id,
    String provider,
    String updateType,
    String title,
    String summary,
    String url,
    String publishedAt,
    String sourceType,
    String status,
    String externalId,
    String createdAt,
    String updatedAt,
    EmergingTechMetadataDto metadata
) {
    public record EmergingTechMetadataDto(
        String version,
        List<String> tags,
        String author,
        String githubRepo
    ) {}
}
```

#### EmergingTechInternalContract 변경

```java
// EmergingTechInternalContract.java에 추가
@GetMapping("/api/v1/emerging-tech/{id}")
ApiResponse<Object> getEmergingTechDetail(
    @RequestHeader("X-Internal-Api-Key") String apiKey,
    @PathVariable("id") String id
);
```

#### EmergingTechToolAdapter 변경

```java
/**
 * 상세 조회 (ID 기반)
 */
public EmergingTechDetailDto getDetail(String id) {
    // 1. Feign Client로 api-emerging-tech 호출
    // 2. 응답을 EmergingTechDetailDto로 변환
    // 3. 에러 시 빈 결과 또는 에러 메시지 포함 DTO 반환
}
```

---

### 3.3 기존 `search_emerging_techs` Tool 개선

#### 현재 상태
- 파라미터: `query` (검색 키워드), `provider` (선택)
- 반환: `List<EmergingTechDto>` (id, provider, updateType, title, url, status)

#### 개선 사항
현재 `search_emerging_techs`는 api-emerging-tech의 검색 API를 Feign Client로 호출하므로 기본 기능은 충분. 단, `list_emerging_techs`가 추가되면 역할이 명확히 분리됨:
- `search_emerging_techs`: **제목 키워드 검색** (자유 텍스트 검색)
- `list_emerging_techs`: **필터 기반 목록 조회** (기간, provider, updateType, sourceType, status 조합)

현재 `search_emerging_techs`의 파라미터와 반환 DTO는 유지하되, System Prompt에서 두 Tool의 역할 차이를 명확히 안내해야 함.

---

### 3.4 `send_slack_notification` Tool Mock 전환

#### 요구 사항
- Tool 자체는 유지 (Agent가 Slack 알림을 전송하려는 의도를 표현할 수 있어야 함)
- 실제 Slack API 호출 대신, 발송될 메시지 내용을 `ToolResult.data`로 반환
- 향후 Slack 활성화 시 설정값 변경만으로 전환 가능해야 함

#### SlackToolAdapter 변경

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackToolAdapter {

    private final SlackContract slackApi;

    @Value("${slack.emerging-tech.channel:#emerging-tech}")
    private String defaultChannel;

    // Slack 발송 비활성화 플래그
    @Value("${agent.slack.enabled:false}")
    private boolean slackEnabled;

    /**
     * Slack 알림 발송 (비활성화 시 Mock 응답 반환)
     */
    public ToolResult sendNotification(String message) {
        if (!slackEnabled) {
            log.info("Slack 비활성화 상태 - Mock 응답 반환: channel={}, message={}", defaultChannel, message);
            return ToolResult.success(
                "[Slack 비활성화] 다음 메시지가 발송될 예정입니다",
                Map.of(
                    "channel", defaultChannel,
                    "message", message,
                    "status", "MOCK_SENT"
                )
            );
        }

        try {
            slackApi.sendInfoNotification(message);
            return ToolResult.success("Slack 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            return ToolResult.failure("Slack 알림 전송 실패: " + e.getMessage());
        }
    }
}
```

#### application-agent-api.yml 변경

```yaml
agent:
  slack:
    enabled: false  # Slack 발송 비활성화 (true로 변경 시 실제 발송)
```

#### ToolResult DTO 확인

현재 `ToolResult.data`가 `Object` 타입이므로 `Map<String, Object>`를 전달 가능. LangChain4j가 JSON 직렬화하여 LLM에 전달하면, LLM이 이를 해석하여 관리자에게 "이 메시지가 Slack으로 발송됩니다" 형태로 보여줄 수 있음.

---

### 3.5 System Prompt (`AgentPromptConfig`) 변경

#### tools 필드 변경

```java
private String tools = """
    - fetch_github_releases: GitHub 저장소 릴리스 조회
    - scrape_web_page: 웹 페이지 크롤링
    - list_emerging_techs: 기간/Provider/UpdateType/SourceType/Status별 목록 조회 (페이징 지원)
    - get_emerging_tech_detail: ID로 상세 조회
    - search_emerging_techs: 제목 키워드 검색
    - get_emerging_tech_statistics: Provider/SourceType/기간별 통계 집계
    - analyze_text_frequency: 키워드 빈도 분석 (Word Cloud)
    - send_slack_notification: Slack 알림 전송 (현재 비활성화 - Mock 응답)
    - collect_github_releases: GitHub 저장소 릴리스 수집 및 DB 저장
    - collect_rss_feeds: OpenAI/Google 블로그 RSS 피드 수집 및 DB 저장
    - collect_scraped_articles: Anthropic/Meta 블로그 크롤링 및 DB 저장""";
```

#### rules 필드 변경

```java
private String rules = """
    1. 목록 조회 요청 시 list_emerging_techs를 사용하여 기간, Provider, UpdateType, SourceType, Status 필터를 조합
    2. 특정 항목의 상세 정보 요청 시 get_emerging_tech_detail을 사용
    3. 제목 키워드로 자유 검색 시 search_emerging_techs 사용
    4. 통계 요청 시 get_emerging_tech_statistics로 집계하고, Markdown 표와 Mermaid 차트로 정리
    5. 키워드 분석 요청 시 analyze_text_frequency로 빈도를 집계하고, Mermaid 차트와 해석을 함께 제공
    6. 데이터 수집 요청 시 fetch_github_releases, scrape_web_page 활용
    7. 중복 확인은 search_emerging_techs 사용
    8. Slack 알림은 현재 비활성화 상태. send_slack_notification 호출 시 Mock 응답이 반환됨
    9. 데이터 수집 및 저장 요청 시 collect_* 도구를 사용
    10. 전체 소스 수집 요청 시: collect_github_releases(각 저장소별) → collect_rss_feeds("") → collect_scraped_articles("") 순서로 실행
    11. 수집 결과의 신규/중복/실패 건수를 Markdown 표로 정리하여 제공
    12. 작업 완료 후 결과 요약 제공""";
```

---

### 3.6 ToolInputValidator 변경

신규 검증 메서드 추가:

```java
// Status 검증 (선택적, 빈 문자열 허용)
private static final Set<String> VALID_STATUSES = Set.of(
    "DRAFT", "PENDING", "PUBLISHED", "REJECTED");

public static String validateStatusOptional(String status) {
    if (status == null || status.isBlank()) return null;
    return validateEnum(status, "status", VALID_STATUSES);
}

// MongoDB ObjectId 검증
private static final Pattern OBJECT_ID_PATTERN = Pattern.compile("^[a-fA-F0-9]{24}$");

public static String validateObjectId(String id) {
    String requiredError = validateRequired(id, "id");
    if (requiredError != null) return requiredError;
    if (!OBJECT_ID_PATTERN.matcher(id).matches()) {
        return String.format("Error: id는 24자리 16진수(MongoDB ObjectId)여야 합니다 (입력값: %s)", id);
    }
    return null;
}

// 페이지 번호/크기 정규화
public static int normalizePage(int page) {
    return Math.max(1, page);
}

public static int normalizeSize(int size) {
    if (size <= 0) return 20;
    return Math.min(size, 100);
}
```

---

### 3.7 ToolErrorHandlers 변경

`hallucinatedToolNameStrategy`의 Tool 목록 업데이트:

```java
String errorMessage = String.format("Error: Tool '%s'은(는) 존재하지 않습니다. " +
        "사용 가능한 Tool: fetch_github_releases, scrape_web_page, " +
        "list_emerging_techs, get_emerging_tech_detail, search_emerging_techs, " +
        "get_emerging_tech_statistics, analyze_text_frequency, " +
        "send_slack_notification, " +
        "collect_github_releases, collect_rss_feeds, collect_scraped_articles",
        toolName);
```

---

## 4. 변경 파일 목록

### 4.1 api-agent 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 신규 | `tool/dto/EmergingTechListDto.java` | 목록 조회 결과 DTO |
| 신규 | `tool/dto/EmergingTechDetailDto.java` | 상세 조회 결과 DTO |
| 수정 | `tool/EmergingTechAgentTools.java` | `listEmergingTechs`, `getEmergingTechDetail` 2개 @Tool 메서드 추가 |
| 수정 | `tool/adapter/EmergingTechToolAdapter.java` | `list()`, `getDetail()` 메서드 추가 |
| 수정 | `tool/adapter/SlackToolAdapter.java` | `slackEnabled` 플래그 + Mock 응답 로직 추가 |
| 수정 | `tool/validation/ToolInputValidator.java` | `validateStatusOptional`, `validateObjectId`, `normalizePage`, `normalizeSize` 추가 |
| 수정 | `tool/handler/ToolErrorHandlers.java` | Tool 목록 문자열 업데이트 |
| 수정 | `config/AgentPromptConfig.java` | tools, rules 필드 업데이트 |
| 수정 | `src/main/resources/application-agent-api.yml` | `agent.slack.enabled: false` 추가 |

### 4.2 client-feign 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 수정 | `domain/internal/contract/EmergingTechInternalContract.java` | `listEmergingTechs`, `getEmergingTechDetail` Feign 메서드 추가 |

### 4.3 api-emerging-tech 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 수정 | `dto/request/EmergingTechListRequest.java` | `startDate`, `endDate`, `sourceType` 필드 추가 |
| 수정 | `service/EmergingTechQueryServiceImpl.java` | `buildFilterCriteria()`에 source_type, published_at 기간 필터 추가 |
| 수정 | `facade/EmergingTechFacade.java` | 날짜/sourceType 파라미터 전달, sourceType 검증 추가 |

---

## 5. 구현 순서

| 단계 | 작업 | 의존 관계 |
|------|------|----------|
| 1 | `api-emerging-tech`: EmergingTechListRequest에 startDate/endDate/sourceType 추가 | 없음 |
| 2 | `api-emerging-tech`: EmergingTechQueryServiceImpl에 기간/sourceType 필터 추가 | 단계 1 |
| 3 | `api-emerging-tech`: EmergingTechFacade에 sourceType 검증 + 파라미터 전달 추가 | 단계 2 |
| 4 | `client-feign`: EmergingTechInternalContract에 list/detail Feign 메서드 추가 | 단계 3 |
| 5 | `api-agent`: EmergingTechListDto, EmergingTechDetailDto 신규 생성 | 없음 |
| 6 | `api-agent`: ToolInputValidator에 검증 메서드 추가 | 없음 |
| 7 | `api-agent`: EmergingTechToolAdapter에 list(), getDetail() 추가 | 단계 4, 5 |
| 8 | `api-agent`: EmergingTechAgentTools에 2개 @Tool 메서드 추가 | 단계 6, 7 |
| 9 | `api-agent`: SlackToolAdapter Mock 전환 + application-agent-api.yml 변경 | 없음 |
| 10 | `api-agent`: AgentPromptConfig tools/rules 업데이트 | 없음 |
| 11 | `api-agent`: ToolErrorHandlers Tool 목록 업데이트 | 없음 |
| 12 | 빌드 검증: `./gradlew :api-emerging-tech:compileJava :client-feign:compileJava :api-agent:compileJava` | 전체 완료 |

---

## 6. Agent 행동 흐름 예시

### 6.1 목록 조회 요청

```
관리자: "최근 1주일간 ANTHROPIC의 블로그 포스트를 보여줘"

Agent 추론:
1. "기간 + Provider + UpdateType 필터 조합 목록 조회가 필요해"
   → Tool: list_emerging_techs(
       startDate="2026-01-29", endDate="2026-02-05",
       provider="ANTHROPIC", updateType="BLOG_POST",
       sourceType="", status="", page=1, size=20)
   → 결과: { totalCount: 5, items: [...] }

2. "결과를 표로 정리"

Agent 응답:
"## ANTHROPIC 블로그 포스트 (2026-01-29 ~ 2026-02-05)

| # | 제목 | 발행일 | 상태 |
|---|------|--------|------|
| 1 | Claude 4 출시 | 2026-02-03 | PUBLISHED |
| 2 | ... | ... | ... |

총 5건이 조회되었습니다."
```

### 6.2 상세 조회 요청

```
관리자: "첫 번째 항목의 상세 내용을 보여줘"

Agent 추론:
1. "이전 목록에서 첫 번째 항목 ID 확인"
   → Tool: get_emerging_tech_detail(id="65a1b2c3d4e5f6a7b8c9d0e1")
   → 결과: { title: "Claude 4 출시", summary: "...", metadata: {...} }

Agent 응답:
"## Claude 4 출시 상세

- **Provider**: ANTHROPIC
- **Type**: MODEL_RELEASE
- **Published**: 2026-02-03
- **Source**: WEB_SCRAPING
- **URL**: https://www.anthropic.com/news/claude-4
- **Version**: -
- **Tags**: [ai, model, release]

**요약**:
Anthropic이 Claude 4 모델을 출시했습니다. ..."
```

### 6.3 기간별 Provider 목록 + Slack 알림

```
관리자: "이번 달 수집된 모든 데이터를 보여주고 Slack으로 요약 보내줘"

Agent 추론:
1. → Tool: list_emerging_techs(startDate="2026-02-01", endDate="2026-02-05", ...)
   → 결과: { totalCount: 42, items: [...] }

2. → Tool: send_slack_notification("2026년 2월 수집 현황: 총 42건 ...")
   → 결과: { success: true, message: "[Slack 비활성화] 다음 메시지가 발송될 예정입니다",
             data: { channel: "#emerging-tech", message: "...", status: "MOCK_SENT" } }

Agent 응답:
"## 2026년 2월 수집 현황

총 42건이 조회되었습니다.

| Provider | 건수 |
|----------|------|
| OPENAI | 15 |
| ANTHROPIC | 12 |
| ... | ... |

> **참고**: Slack 알림은 현재 비활성화 상태입니다. 아래 메시지가 발송될 예정이었습니다:
> '2026년 2월 수집 현황: 총 42건 ...'"
```

---

## 7. 검증 체크리스트

구현 완료 후 다음 항목을 검증:

- [ ] `list_emerging_techs` Tool이 published_at 기간 + provider + update_type + source_type + status 필터 조합을 지원하는가
- [ ] `list_emerging_techs` Tool이 페이징(page, size)을 지원하는가
- [ ] `get_emerging_tech_detail` Tool이 MongoDB ObjectId로 상세 조회를 지원하는가
- [ ] `get_emerging_tech_detail` Tool 반환 데이터에 summary, publishedAt, sourceType, metadata가 포함되는가
- [ ] `search_emerging_techs` Tool이 기존과 동일하게 동작하는가
- [ ] `send_slack_notification` Tool이 실제 Slack 발송 없이 Mock 응답을 반환하는가
- [ ] `agent.slack.enabled=true`로 변경 시 실제 Slack 발송이 가능한가
- [ ] System Prompt에 신규 Tool 목록과 역할 설명이 반영되었는가
- [ ] `api-emerging-tech` 목록 조회 API가 startDate/endDate/sourceType 파라미터를 지원하는가
- [ ] `./gradlew :api-emerging-tech:compileJava :client-feign:compileJava :api-agent:compileJava` 빌드 성공

---

## 8. 제한 사항

- 오버엔지니어링 금지: 신규 Tool 2개(list, detail)과 Slack Mock 전환만 수행. 불필요한 추가 기능 구현 금지
- 최소한의 한글 주석 추가
- 기존 패턴 준수: Adapter 패턴(Tool → Adapter → External Service), ThreadLocal 메트릭, ToolInputValidator 검증 패턴
- 기존 코드 수정 최소화: api-emerging-tech에는 필터 파라미터 추가만 수행

---

## 9. 참고 자료

### 핵심 참조 파일

| 파일 | 참조 사유 |
|------|----------|
| `api/agent/.../tool/EmergingTechAgentTools.java` | 현재 Tool 정의, 패턴 준수 |
| `api/agent/.../tool/adapter/EmergingTechToolAdapter.java` | Adapter 패턴 참조, search() 구현 패턴 |
| `api/agent/.../tool/adapter/SlackToolAdapter.java` | Mock 전환 대상 |
| `api/agent/.../tool/validation/ToolInputValidator.java` | 입력 검증 패턴 |
| `api/agent/.../config/AgentPromptConfig.java` | System Prompt 구성 |
| `api/agent/.../tool/handler/ToolErrorHandlers.java` | Tool 목록 업데이트 |
| `api/emerging-tech/.../controller/EmergingTechController.java` | 조회 API 엔드포인트 |
| `api/emerging-tech/.../dto/request/EmergingTechListRequest.java` | 목록 조회 요청 DTO |
| `api/emerging-tech/.../service/EmergingTechQueryServiceImpl.java` | 동적 Criteria 필터 패턴 |
| `api/emerging-tech/.../facade/EmergingTechFacade.java` | 비즈니스 로직 오케스트레이션 |
| `client/feign/.../internal/contract/EmergingTechInternalContract.java` | Feign Client 계약 |

### 공식 문서

- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- Spring Data MongoDB Query: https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html
- MongoDB Query Operators: https://www.mongodb.com/docs/manual/reference/operator/query/
