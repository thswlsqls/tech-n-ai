# Phase 6: AI Agent 조회 Tool 개선 및 Slack 비활성화 설계서

## 1. 개요

### 1.1 목적
`api-agent` 모듈의 AI Agent가 `api-emerging-tech` 모듈의 3개 조회 API(목록, 상세, 검색)와 동등한 기능을
자연어 입력만으로 자율 수행할 수 있도록 Tool을 개선한다.
또한, `send_slack_notification` Tool을 비활성화하여 실제 Slack 발송 없이 Mock 응답을 반환하도록 전환한다.

### 1.2 전제 조건
- Phase 1~5 완료: 데이터 수집 파이프라인, LangChain4j Tool 래퍼, Agent 통합, 분석 Tool, 데이터 수집 Tool
- `api-agent` 모듈: LangChain4j 1.10.0 + OpenAI GPT-4o-mini 설정 완료
- `api-emerging-tech` 모듈: MongoDB Atlas `emerging_techs` 컬렉션 기반 3개 조회 API 정상 동작
- `EmergingTechInternalContract` Feign Client를 통한 모듈 간 통신 정상 동작

### 1.3 변경 요약

| 구분 | 항목 | 설명 |
|------|------|------|
| 신규 Tool | `list_emerging_techs` | 기간/Provider/UpdateType/SourceType/Status 필터 목록 조회 |
| 신규 Tool | `get_emerging_tech_detail` | ID 기반 상세 조회 |
| 신규 DTO | `EmergingTechListDto.java` | 목록 조회 결과 (페이징 메타 포함) |
| 신규 DTO | `EmergingTechDetailDto.java` | 상세 조회 결과 (summary, metadata 포함) |
| 수정 | `EmergingTechAgentTools.java` | 2개 @Tool 메서드 추가 |
| 수정 | `EmergingTechToolAdapter.java` | `list()`, `getDetail()` 메서드 추가 |
| 수정 | `SlackToolAdapter.java` | `slackEnabled` 플래그 + Mock 응답 로직 |
| 수정 | `ToolInputValidator.java` | `validateStatusOptional`, `validateObjectId`, `normalizePage`, `normalizeSize` 추가 |
| 수정 | `ToolErrorHandlers.java` | Tool 목록 문자열 업데이트 |
| 수정 | `AgentPromptConfig.java` | tools/rules 필드 업데이트 |
| 수정 | `application-agent-api.yml` | `agent.slack.enabled: false` 추가 |
| 수정 | `EmergingTechInternalContract.java` | `listEmergingTechs`, `getEmergingTechDetail` Feign 메서드 추가 |
| 수정 | `EmergingTechListRequest.java` | `startDate`, `endDate`, `sourceType` 필드 추가 |
| 수정 | `EmergingTechQueryService.java` | 인터페이스 시그니처 확장 |
| 수정 | `EmergingTechQueryServiceImpl.java` | `buildFilterCriteria()`에 source_type, published_at 기간 필터 추가 |
| 수정 | `EmergingTechFacade.java` | sourceType 검증 + 날짜/sourceType 파라미터 전달 |

---

## 2. GAP 분석

### 2.1 현재 api-agent Tool 구성 (Phase 5 완료 기준)

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
| GET | `/api/v1/emerging-tech/{id}` | 상세 조회 | id (ObjectId) |
| GET | `/api/v1/emerging-tech/search` | 제목 키워드 검색 | q, page, size |

### 2.3 GAP 식별

| # | 항목 | 결과 | 상세 |
|---|------|------|------|
| G1 | 목록 조회 Tool | **미충족** | published_at 기간 필터, provider/update_type/source_type 필터 조합, 페이징, 정렬이 포함된 목록 조회 Tool 없음 |
| G2 | 상세 조회 Tool | **미충족** | ID 기반 단건 상세 조회 Tool 없음 |
| G3 | 검색 Tool 기능 동등성 | **부분 충족** | `search_emerging_techs`가 query + provider 필터만 지원. published_at 기간 필터, source_type 필터 미지원 |
| G4 | Slack Tool 비활성화 | **미충족** | `send_slack_notification`이 실제 Slack 발송을 시도함 |

---

## 3. 아키텍처 설계

### 3.1 데이터 흐름

```
관리자 자연어 입력 (예: "최근 1주일간 ANTHROPIC의 BLOG_POST 목록을 보여줘")
    |
    v
LLM이 Tool 선택 (예: list_emerging_techs)
    |
    v
EmergingTechAgentTools.listEmergingTechs(startDate, endDate, provider, ...)
    ├── 입력값 검증 (ToolInputValidator)
    ├── 메트릭 기록 (metrics.incrementToolCall)
    └── EmergingTechToolAdapter.list(startDate, endDate, provider, ...)
        └── EmergingTechInternalContract.listEmergingTechs(apiKey, ...)
            └── api-emerging-tech → EmergingTechController → Facade → QueryService → MongoDB
    |
    v
LLM이 EmergingTechListDto(JSON)를 해석하여 관리자에게 Markdown 표로 보고
```

### 3.2 계층 구조

```
EmergingTechAgentTools (Tool Layer)
    ├── EmergingTechToolAdapter (Adapter Layer)
    │   └── EmergingTechInternalContract (Client Layer - client-feign)
    │       └── api-emerging-tech (REST API)
    │           └── EmergingTechFacade → QueryService → MongoTemplate → MongoDB
    └── SlackToolAdapter (Adapter Layer)
        └── SlackContract (Client Layer - client-slack)  ← Mock 전환 대상
```

### 3.3 구현 방식: Feign Client 경유 (방식 A)

기존 `search_emerging_techs` Tool이 `EmergingTechInternalContract` Feign Client를 통해 api-emerging-tech를 호출하는 패턴을 따른다.

| 장점 | 단점 |
|------|------|
| 기존 아키텍처 패턴 일관성 유지 | 네트워크 홉 추가 |
| api-emerging-tech 비즈니스 로직(필터 검증, 정렬 검증) 재사용 | - |
| 관심사 분리 유지 | - |

> Phase 4의 `get_emerging_tech_statistics`와 `analyze_text_frequency`는 통계 집계용으로 domain-mongodb 직접 접근을 사용하지만, 일반적인 CRUD 조회는 Feign Client 경유 방식을 유지한다.

---

## 4. 상세 설계

### 4.1 `api-emerging-tech` 모듈 변경

#### 4.1.1 EmergingTechListRequest 변경

**파일**: `api/emerging-tech/src/main/java/.../dto/request/EmergingTechListRequest.java`

**현재 상태**:
```java
public record EmergingTechListRequest(
    Integer page,
    Integer size,
    String provider,
    String updateType,
    String status,
    String sort
) {}
```

**변경 후**:
```java
public record EmergingTechListRequest(
    @Min(value = 1, message = "page는 1 이상이어야 합니다.") Integer page,
    @Min(value = 1, message = "size는 1 이상이어야 합니다.")
    @Max(value = 100, message = "size는 100 이하여야 합니다.") Integer size,
    String provider,
    String updateType,
    String status,
    String sort,
    String startDate,      // 추가: 조회 시작일 (YYYY-MM-DD)
    String endDate,        // 추가: 조회 종료일 (YYYY-MM-DD)
    String sourceType      // 추가: SourceType enum value 필터
) {
    public EmergingTechListRequest {
        if (page == null) page = 1;
        if (size == null) size = 20;
    }
}
```

#### 4.1.2 EmergingTechQueryService 인터페이스 변경

**파일**: `api/emerging-tech/src/main/java/.../service/EmergingTechQueryService.java`

**변경 후**:
```java
public interface EmergingTechQueryService {

    Page<EmergingTechDocument> findEmergingTechs(
        String provider, String updateType, String status,
        String sourceType, String startDate, String endDate,    // 추가
        Pageable pageable);

    EmergingTechDocument findEmergingTechById(String id);

    Page<EmergingTechDocument> searchEmergingTech(String query, Pageable pageable);
}
```

#### 4.1.3 EmergingTechQueryServiceImpl 변경

**파일**: `api/emerging-tech/src/main/java/.../service/EmergingTechQueryServiceImpl.java`

`buildFilterCriteria()` 메서드에 source_type, published_at 기간 필터 추가:

```java
@Override
public Page<EmergingTechDocument> findEmergingTechs(
        String provider, String updateType, String status,
        String sourceType, String startDate, String endDate,
        Pageable pageable) {
    Criteria criteria = buildFilterCriteria(provider, updateType, status, sourceType, startDate, endDate);
    Query query = new Query(criteria).with(pageable);

    List<EmergingTechDocument> content = mongoTemplate.find(query, EmergingTechDocument.class);
    return PageableExecutionUtils.getPage(content, pageable,
            () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), EmergingTechDocument.class));
}

private Criteria buildFilterCriteria(String provider, String updateType, String status,
                                      String sourceType, String startDate, String endDate) {
    Criteria criteria = new Criteria();

    if (provider != null) {
        criteria = criteria.and("provider").is(provider);
    }
    if (updateType != null) {
        criteria = criteria.and("update_type").is(updateType);
    }
    if (status != null) {
        criteria = criteria.and("status").is(status);
    }
    if (sourceType != null) {
        criteria = criteria.and("source_type").is(sourceType);
    }
    if (startDate != null) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        criteria = criteria.and("published_at").gte(start);
    }
    if (endDate != null) {
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
        criteria = criteria.andOperator(Criteria.where("published_at").lte(end));
    }

    return criteria;
}
```

> **주의**: `published_at`에 대해 `gte`와 `lte`를 동시에 적용할 때 동일 필드에 `.and()` 중복 호출 시 MongoDB Driver 오류 발생 가능. `startDate`와 `endDate` 모두 존재하는 경우 `Criteria.where("published_at").gte(start).lte(end)` 형태로 합쳐야 한다.

**수정된 날짜 필터 로직**:

```java
// published_at 기간 필터
if (startDate != null || endDate != null) {
    Criteria dateCriteria = Criteria.where("published_at");
    if (startDate != null) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        dateCriteria = dateCriteria.gte(start);
    }
    if (endDate != null) {
        LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
        dateCriteria = dateCriteria.lte(end);
    }
    criteria = criteria.andOperator(dateCriteria);
}
```

#### 4.1.4 EmergingTechFacade 변경

**파일**: `api/emerging-tech/src/main/java/.../facade/EmergingTechFacade.java`

`getEmergingTechList()` 메서드에 sourceType 검증 및 날짜/sourceType 파라미터 전달:

```java
public EmergingTechPageResponse getEmergingTechList(EmergingTechListRequest request) {
    validateFilters(request.provider(), request.updateType(), request.status(), request.sourceType());

    Pageable pageable = PageRequest.of(
        request.page() - 1,
        request.size(),
        parseSort(request.sort())
    );

    Page<EmergingTechDocument> page = queryService.findEmergingTechs(
        request.provider(),
        request.updateType(),
        request.status(),
        request.sourceType(),        // 추가
        request.startDate(),         // 추가
        request.endDate(),           // 추가
        pageable
    );

    return toPageResponse(page, request.page(), request.size());
}
```

`validateFilters()` 메서드에 sourceType 검증 추가:

```java
private void validateFilters(String provider, String updateType, String status, String sourceType) {
    if (provider != null) {
        validateEnumValue(TechProvider.class, provider, "provider");
    }
    if (updateType != null) {
        validateEnumValue(EmergingTechType.class, updateType, "updateType");
    }
    if (status != null) {
        validateEnumValue(PostStatus.class, status, "status");
    }
    if (sourceType != null) {
        validateEnumValue(SourceType.class, sourceType, "sourceType");
    }
}
```

> `SourceType` enum은 `domain-mongodb` 모듈의 `com.ebson.shrimp.tm.demo.domain.mongodb.enums.SourceType`에 이미 존재.

---

### 4.2 `client-feign` 모듈 변경

#### 4.2.1 EmergingTechInternalContract 변경

**파일**: `client/feign/src/main/java/.../domain/internal/contract/EmergingTechInternalContract.java`

**추가 메서드**:

```java
/**
 * Emerging Tech 목록 조회 (필터 + 페이징)
 */
@GetMapping("/api/v1/emerging-tech")
ApiResponse<Object> listEmergingTechs(
    @RequestHeader("X-Internal-Api-Key") String apiKey,
    @RequestParam(value = "provider", required = false) String provider,
    @RequestParam(value = "updateType", required = false) String updateType,
    @RequestParam(value = "status", required = false) String status,
    @RequestParam(value = "sourceType", required = false) String sourceType,
    @RequestParam(value = "startDate", required = false) String startDate,
    @RequestParam(value = "endDate", required = false) String endDate,
    @RequestParam(value = "page", defaultValue = "1") int page,
    @RequestParam(value = "size", defaultValue = "20") int size,
    @RequestParam(value = "sort", required = false) String sort
);

/**
 * Emerging Tech 상세 조회 (ID 기반)
 */
@GetMapping("/api/v1/emerging-tech/{id}")
ApiResponse<Object> getEmergingTechDetail(
    @RequestHeader("X-Internal-Api-Key") String apiKey,
    @PathVariable("id") String id
);
```

---

### 4.3 `api-agent` 모듈 변경

#### 4.3.1 EmergingTechListDto (신규)

**파일**: `api/agent/src/main/java/.../tool/dto/EmergingTechListDto.java`

```java
package com.ebson.shrimp.tm.demo.api.agent.tool.dto;

import java.util.List;

/**
 * 목록 조회 결과 DTO
 * LangChain4j @Tool 반환 시 JSON 직렬화되어 LLM에 전달됨
 */
public record EmergingTechListDto(
    int totalCount,
    int page,
    int size,
    int totalPages,
    String period,                  // 조회 기간 (예: "2026-01-01 ~ 2026-01-31" 또는 "전체")
    List<EmergingTechDto> items     // 기존 EmergingTechDto 재사용
) {
    public static EmergingTechListDto empty(int page, int size, String period) {
        return new EmergingTechListDto(0, page, size, 0, period, List.of());
    }
}
```

#### 4.3.2 EmergingTechDetailDto (신규)

**파일**: `api/agent/src/main/java/.../tool/dto/EmergingTechDetailDto.java`

```java
package com.ebson.shrimp.tm.demo.api.agent.tool.dto;

import java.util.List;

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

    public static EmergingTechDetailDto notFound(String id) {
        return new EmergingTechDetailDto(
            id, null, null, null,
            "해당 ID의 도큐먼트를 찾을 수 없습니다: " + id,
            null, null, null, null, null, null, null, null
        );
    }
}
```

#### 4.3.3 ToolInputValidator 변경

**파일**: `api/agent/src/main/java/.../tool/validation/ToolInputValidator.java`

**추가 메서드**:

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

// 페이지 번호 정규화 (1 미만이면 1)
public static int normalizePage(int page) {
    return Math.max(1, page);
}

// 페이지 크기 정규화 (0 이하면 20, 100 초과면 100)
public static int normalizeSize(int size) {
    if (size <= 0) return 20;
    return Math.min(size, 100);
}
```

#### 4.3.4 EmergingTechToolAdapter 변경

**파일**: `api/agent/src/main/java/.../tool/adapter/EmergingTechToolAdapter.java`

**추가 메서드**:

```java
/**
 * 목록 조회 (필터 + 페이징)
 */
@SuppressWarnings("unchecked")
public EmergingTechListDto list(String startDate, String endDate,
                                 String provider, String updateType,
                                 String sourceType, String status,
                                 int page, int size) {
    try {
        String providerParam = toNullIfBlank(provider);
        String updateTypeParam = toNullIfBlank(updateType);
        String sourceTypeParam = toNullIfBlank(sourceType);
        String statusParam = toNullIfBlank(status);
        String startDateParam = toNullIfBlank(startDate);
        String endDateParam = toNullIfBlank(endDate);

        ApiResponse<Object> response = emergingTechContract.listEmergingTechs(
            apiKey, providerParam, updateTypeParam, statusParam,
            sourceTypeParam, startDateParam, endDateParam,
            page, size, "publishedAt,desc"
        );

        if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
            log.warn("Emerging Tech 목록 조회 실패: code={}, message={}", response.code(), response.message());
            String period = buildPeriodString(startDate, endDate);
            return EmergingTechListDto.empty(page, size, period);
        }

        Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);

        int totalCount = getInt(data, "totalCount", 0);
        int pageNumber = getInt(data, "pageNumber", page);
        int pageSize = getInt(data, "pageSize", size);
        int totalPages = (totalCount + pageSize - 1) / pageSize;

        List<Map<String, Object>> rawItems = (List<Map<String, Object>>) data.get("items");
        List<EmergingTechDto> items = (rawItems != null)
            ? rawItems.stream()
                .map(item -> new EmergingTechDto(
                    getString(item, "id"),
                    getString(item, "provider"),
                    getString(item, "updateType"),
                    getString(item, "title"),
                    getString(item, "url"),
                    getString(item, "status")
                ))
                .toList()
            : List.of();

        String period = buildPeriodString(startDate, endDate);
        return new EmergingTechListDto(totalCount, pageNumber, pageSize, totalPages, period, items);

    } catch (Exception e) {
        log.error("Emerging Tech 목록 조회 실패", e);
        String period = buildPeriodString(startDate, endDate);
        return EmergingTechListDto.empty(page, size, period);
    }
}

/**
 * 상세 조회 (ID 기반)
 */
@SuppressWarnings("unchecked")
public EmergingTechDetailDto getDetail(String id) {
    try {
        ApiResponse<Object> response = emergingTechContract.getEmergingTechDetail(apiKey, id);

        if (!SUCCESS_CODE.equals(response.code()) || response.data() == null) {
            log.warn("Emerging Tech 상세 조회 실패: code={}, message={}", response.code(), response.message());
            return EmergingTechDetailDto.notFound(id);
        }

        Map<String, Object> data = objectMapper.convertValue(response.data(), Map.class);

        EmergingTechDetailDto.EmergingTechMetadataDto metadata = null;
        Map<String, Object> rawMetadata = (Map<String, Object>) data.get("metadata");
        if (rawMetadata != null) {
            metadata = new EmergingTechDetailDto.EmergingTechMetadataDto(
                getString(rawMetadata, "version"),
                rawMetadata.get("tags") instanceof List<?> tags
                    ? tags.stream().map(Object::toString).toList()
                    : List.of(),
                getString(rawMetadata, "author"),
                getString(rawMetadata, "githubRepo")
            );
        }

        return new EmergingTechDetailDto(
            getString(data, "id"),
            getString(data, "provider"),
            getString(data, "updateType"),
            getString(data, "title"),
            getString(data, "summary"),
            getString(data, "url"),
            getString(data, "publishedAt"),
            getString(data, "sourceType"),
            getString(data, "status"),
            getString(data, "externalId"),
            getString(data, "createdAt"),
            getString(data, "updatedAt"),
            metadata
        );

    } catch (Exception e) {
        log.error("Emerging Tech 상세 조회 실패: id={}", id, e);
        return EmergingTechDetailDto.notFound(id);
    }
}

// 헬퍼 메서드
private String toNullIfBlank(String value) {
    return (value != null && !value.isBlank()) ? value : null;
}

private int getInt(Map<String, Object> map, String key, int defaultValue) {
    Object value = map.get(key);
    if (value instanceof Number number) {
        return number.intValue();
    }
    return defaultValue;
}

private String buildPeriodString(String startDate, String endDate) {
    if ((startDate == null || startDate.isBlank()) && (endDate == null || endDate.isBlank())) {
        return "전체";
    }
    String start = (startDate != null && !startDate.isBlank()) ? startDate : "~";
    String end = (endDate != null && !endDate.isBlank()) ? endDate : "~";
    return start + " ~ " + end;
}
```

#### 4.3.5 EmergingTechAgentTools 변경

**파일**: `api/agent/src/main/java/.../tool/EmergingTechAgentTools.java`

기존 9개 @Tool 메서드 아래에 2개 추가:

```java
/**
 * 기간/필터별 Emerging Tech 목록 조회
 */
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
) {
    metrics().incrementToolCall();
    log.info("Tool 호출: list_emerging_techs(startDate={}, endDate={}, provider={}, updateType={}, sourceType={}, status={}, page={}, size={})",
            startDate, endDate, provider, updateType, sourceType, status, page, size);

    // 입력값 검증
    String startDateError = ToolInputValidator.validateDateOptional(startDate, "startDate");
    if (startDateError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", startDateError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    String endDateError = ToolInputValidator.validateDateOptional(endDate, "endDate");
    if (endDateError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", endDateError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    String providerError = ToolInputValidator.validateProviderOptional(provider);
    if (providerError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", providerError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    String updateTypeError = ToolInputValidator.validateUpdateTypeOptional(updateType);
    if (updateTypeError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", updateTypeError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    String sourceTypeError = ToolInputValidator.validateSourceTypeOptional(sourceType);
    if (sourceTypeError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", sourceTypeError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    String statusError = ToolInputValidator.validateStatusOptional(status);
    if (statusError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", statusError);
        return EmergingTechListDto.empty(page, size, "전체");
    }

    // 페이지 정규화
    int normalizedPage = ToolInputValidator.normalizePage(page);
    int normalizedSize = ToolInputValidator.normalizeSize(size);

    return emergingTechAdapter.list(startDate, endDate, provider, updateType,
                                     sourceType, status, normalizedPage, normalizedSize);
}

/**
 * Emerging Tech 상세 조회 (ID 기반)
 */
@Tool(name = "get_emerging_tech_detail",
      value = "Emerging Tech 도큐먼트의 상세 정보를 ID로 조회합니다. "
            + "목록 조회나 검색 결과에서 얻은 ID를 사용합니다.")
public EmergingTechDetailDto getEmergingTechDetail(
    @P("조회할 도큐먼트 ID (MongoDB ObjectId)") String id
) {
    metrics().incrementToolCall();
    log.info("Tool 호출: get_emerging_tech_detail(id={})", id);

    String validationError = ToolInputValidator.validateObjectId(id);
    if (validationError != null) {
        metrics().incrementValidationError();
        log.warn("Tool 입력값 검증 실패: {}", validationError);
        return EmergingTechDetailDto.notFound(id);
    }

    return emergingTechAdapter.getDetail(id);
}
```

#### 4.3.6 SlackToolAdapter Mock 전환

**파일**: `api/agent/src/main/java/.../tool/adapter/SlackToolAdapter.java`

**변경 후**:

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class SlackToolAdapter {

    private final SlackContract slackContract;

    @Value("${slack.emerging-tech.channel:#emerging-tech}")
    private String defaultChannel;

    @Value("${agent.slack.enabled:false}")
    private boolean slackEnabled;

    /**
     * Slack 알림 전송 (비활성화 시 Mock 응답 반환)
     */
    public ToolResult sendNotification(String message) {
        if (!slackEnabled) {
            log.info("Slack 비활성화 상태 - Mock 응답 반환: channel={}, message={}",
                    defaultChannel, message);
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
            slackContract.sendInfoNotification(message);
            return ToolResult.success("Slack 알림 전송 완료");
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
            return ToolResult.failure("Slack 알림 전송 실패: " + e.getMessage());
        }
    }
}
```

> `Map.of()` import: `java.util.Map`

#### 4.3.7 AgentPromptConfig 변경

**파일**: `api/agent/src/main/java/.../config/AgentPromptConfig.java`

**tools 필드 변경**:

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

**rules 필드 변경**:

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

#### 4.3.8 ToolErrorHandlers 변경

**파일**: `api/agent/src/main/java/.../tool/handler/ToolErrorHandlers.java`

`handleHallucinatedToolName()` 메서드의 Tool 목록 업데이트:

```java
String errorMessage = String.format("Error: Tool '%s'은(는) 존재하지 않습니다. " +
        "사용 가능한 Tool: fetch_github_releases, scrape_web_page, " +
        "list_emerging_techs, get_emerging_tech_detail, search_emerging_techs, " +
        "get_emerging_tech_statistics, analyze_text_frequency, " +
        "send_slack_notification, " +
        "collect_github_releases, collect_rss_feeds, collect_scraped_articles",
        toolName);
```

#### 4.3.9 application-agent-api.yml 변경

**파일**: `api/agent/src/main/resources/application-agent-api.yml`

`agent` 섹션에 `slack.enabled` 추가:

```yaml
agent:
  scheduler:
    enabled: ${AGENT_SCHEDULER_ENABLED:false}
    cron: "0 0 */6 * * *"
  analytics:
    default-top-n: 20
    max-top-n: 100
  slack:
    enabled: false    # Slack 발송 비활성화 (true로 변경 시 실제 발송)
```

---

## 5. Tool 역할 분리 가이드

Phase 6 완료 후 조회 관련 Tool 간 역할 분리:

| Tool | 역할 | 사용 시나리오 |
|------|------|-------------|
| `list_emerging_techs` | **필터 기반 목록 조회** | "최근 1주일간 OPENAI의 SDK_RELEASE 목록 보여줘", "ANTHROPIC의 BLOG_POST를 최신순으로 10개 조회" |
| `get_emerging_tech_detail` | **ID 기반 상세 조회** | "첫 번째 항목의 상세 내용을 보여줘", "ID xxx의 전체 정보 알려줘" |
| `search_emerging_techs` | **제목 키워드 자유 검색** | "Claude 관련 업데이트를 검색해줘", "GPT-4o 관련 데이터가 있는지 확인" |
| `get_emerging_tech_statistics` | **통계 집계** | "Provider별 수집 현황 보여줘", "이번 달 UpdateType별 통계" |
| `analyze_text_frequency` | **키워드 빈도 분석** | "최근 키워드 트렌드 분석해줘", "OPENAI 업데이트에서 자주 나오는 단어" |

---

## 6. Agent 행동 흐름 예시

### 6.1 목록 조회

```
관리자: "최근 1주일간 ANTHROPIC의 블로그 포스트를 보여줘"

Agent 추론:
1. "기간 + Provider + UpdateType 필터 조합 목록 조회가 필요하다"
   → Tool: list_emerging_techs(
       startDate="2026-01-29", endDate="2026-02-05",
       provider="ANTHROPIC", updateType="BLOG_POST",
       sourceType="", status="", page=1, size=20)
   → 결과: { totalCount: 5, page: 1, size: 20, totalPages: 1,
             period: "2026-01-29 ~ 2026-02-05", items: [...] }

2. "결과를 Markdown 표로 정리"

Agent 응답:
"## ANTHROPIC 블로그 포스트 (2026-01-29 ~ 2026-02-05)

| # | 제목 | 상태 |
|---|------|------|
| 1 | Claude 4.5 출시 | PUBLISHED |
| 2 | ... | ... |

총 5건이 조회되었습니다."
```

### 6.2 상세 조회

```
관리자: "첫 번째 항목의 상세 내용을 보여줘"

Agent 추론:
1. "이전 목록에서 첫 번째 항목 ID 확인"
   → Tool: get_emerging_tech_detail(id="65a1b2c3d4e5f6a7b8c9d0e1")
   → 결과: { title: "Claude 4.5 출시", summary: "...", metadata: {...} }

Agent 응답:
"## Claude 4.5 출시 상세

- **Provider**: ANTHROPIC
- **Type**: MODEL_RELEASE
- **Published**: 2026-02-03
- **Source**: WEB_SCRAPING
- **URL**: https://www.anthropic.com/news/claude-4-5

**요약**:
Anthropic이 Claude 4.5 모델을 출시했습니다. ..."
```

### 6.3 Slack Mock 동작

```
관리자: "이번 달 수집 현황을 Slack으로 보내줘"

Agent 추론:
1. → Tool: list_emerging_techs(startDate="2026-02-01", endDate="2026-02-05", ...)
   → 결과: { totalCount: 42, items: [...] }

2. → Tool: send_slack_notification("2026년 2월 수집 현황: 총 42건 ...")
   → 결과: { success: true, message: "[Slack 비활성화] 다음 메시지가 발송될 예정입니다",
             data: { channel: "#emerging-tech", message: "...", status: "MOCK_SENT" } }

Agent 응답:
"## 2026년 2월 수집 현황
총 42건이 조회되었습니다.

> Slack 알림은 현재 비활성화 상태입니다.
> 발송 예정 메시지: '2026년 2월 수집 현황: 총 42건 ...'"
```

---

## 7. 변경 파일 목록

### 7.1 api-emerging-tech 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 수정 | `dto/request/EmergingTechListRequest.java` | `startDate`, `endDate`, `sourceType` 필드 추가 |
| 수정 | `service/EmergingTechQueryService.java` | 인터페이스 시그니처에 sourceType/startDate/endDate 추가 |
| 수정 | `service/EmergingTechQueryServiceImpl.java` | `buildFilterCriteria()`에 source_type, published_at 기간 필터 추가 |
| 수정 | `facade/EmergingTechFacade.java` | sourceType 검증 + 날짜/sourceType 파라미터 전달 |

### 7.2 client-feign 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 수정 | `domain/internal/contract/EmergingTechInternalContract.java` | `listEmergingTechs`, `getEmergingTechDetail` Feign 메서드 추가 |

### 7.3 api-agent 모듈

| 구분 | 파일 | 변경 내용 |
|------|------|----------|
| 신규 | `tool/dto/EmergingTechListDto.java` | 목록 조회 결과 DTO |
| 신규 | `tool/dto/EmergingTechDetailDto.java` | 상세 조회 결과 DTO |
| 수정 | `tool/EmergingTechAgentTools.java` | `listEmergingTechs`, `getEmergingTechDetail` 2개 @Tool 메서드 추가 |
| 수정 | `tool/adapter/EmergingTechToolAdapter.java` | `list()`, `getDetail()` 메서드 추가 |
| 수정 | `tool/adapter/SlackToolAdapter.java` | `slackEnabled` 플래그 + Mock 응답 로직 |
| 수정 | `tool/validation/ToolInputValidator.java` | `validateStatusOptional`, `validateObjectId`, `normalizePage`, `normalizeSize` 추가 |
| 수정 | `tool/handler/ToolErrorHandlers.java` | Tool 목록 문자열 업데이트 |
| 수정 | `config/AgentPromptConfig.java` | tools/rules 필드 업데이트 |
| 수정 | `src/main/resources/application-agent-api.yml` | `agent.slack.enabled: false` 추가 |

---

## 8. 구현 순서

| 단계 | 작업 | 대상 모듈 | 의존 관계 |
|------|------|----------|----------|
| 1 | `EmergingTechListRequest`에 startDate/endDate/sourceType 추가 | api-emerging-tech | 없음 |
| 2 | `EmergingTechQueryService` 인터페이스 시그니처 변경 | api-emerging-tech | 단계 1 |
| 3 | `EmergingTechQueryServiceImpl`에 기간/sourceType 필터 추가 | api-emerging-tech | 단계 2 |
| 4 | `EmergingTechFacade`에 sourceType 검증 + 파라미터 전달 추가 | api-emerging-tech | 단계 3 |
| 5 | `EmergingTechInternalContract`에 list/detail Feign 메서드 추가 | client-feign | 단계 4 |
| 6 | `EmergingTechListDto`, `EmergingTechDetailDto` 신규 생성 | api-agent | 없음 |
| 7 | `ToolInputValidator`에 검증 메서드 추가 | api-agent | 없음 |
| 8 | `EmergingTechToolAdapter`에 `list()`, `getDetail()` 추가 | api-agent | 단계 5, 6 |
| 9 | `EmergingTechAgentTools`에 2개 @Tool 메서드 추가 | api-agent | 단계 7, 8 |
| 10 | `SlackToolAdapter` Mock 전환 + `application-agent-api.yml` 변경 | api-agent | 없음 |
| 11 | `AgentPromptConfig` tools/rules 업데이트 | api-agent | 없음 |
| 12 | `ToolErrorHandlers` Tool 목록 업데이트 | api-agent | 없음 |
| 13 | 빌드 검증 | 전체 | 전체 완료 |

**빌드 검증 명령**:
```bash
./gradlew :api-emerging-tech:compileJava :client-feign:compileJava :api-agent:compileJava
```

---

## 9. 검증 체크리스트

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

## 10. 참고 자료

### 핵심 참조 파일

| 파일 | 참조 사유 |
|------|----------|
| `api/agent/.../tool/EmergingTechAgentTools.java` | 현재 Tool 정의, 패턴 준수 |
| `api/agent/.../tool/adapter/EmergingTechToolAdapter.java` | Adapter 패턴 참조, `search()` 구현 패턴 |
| `api/agent/.../tool/adapter/SlackToolAdapter.java` | Mock 전환 대상 |
| `api/agent/.../tool/validation/ToolInputValidator.java` | 입력 검증 패턴 |
| `api/agent/.../config/AgentPromptConfig.java` | System Prompt 구성 |
| `api/agent/.../tool/handler/ToolErrorHandlers.java` | Tool 목록 업데이트 |
| `api/agent/.../tool/dto/EmergingTechDto.java` | 기존 DTO 패턴 참조 |
| `api/agent/.../tool/dto/ToolResult.java` | ToolResult.success(message, data) 패턴 |
| `api/emerging-tech/.../controller/EmergingTechController.java` | 조회 API 엔드포인트 |
| `api/emerging-tech/.../dto/request/EmergingTechListRequest.java` | 목록 조회 요청 DTO |
| `api/emerging-tech/.../dto/response/EmergingTechDetailResponse.java` | 상세 응답 DTO (from() 패턴) |
| `api/emerging-tech/.../dto/response/EmergingTechPageResponse.java` | 페이지 응답 DTO 구조 참조 |
| `api/emerging-tech/.../service/EmergingTechQueryServiceImpl.java` | 동적 Criteria 필터 패턴 |
| `api/emerging-tech/.../facade/EmergingTechFacade.java` | 비즈니스 로직 오케스트레이션, validateFilters/parseSort 패턴 |
| `client/feign/.../internal/contract/EmergingTechInternalContract.java` | Feign Client 계약 |

### 공식 문서

- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- Spring Data MongoDB Criteria: https://docs.spring.io/spring-data/mongodb/reference/mongodb/repositories/query-methods.html
- MongoDB Query Operators: https://www.mongodb.com/docs/manual/reference/operator/query/
