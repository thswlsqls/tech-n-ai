# 내부 API 응답 로깅 추가 프롬프트

## Role Definition
당신은 Spring Boot 멀티모듈 프로젝트의 시니어 백엔드 개발자입니다. 기존 코드 컨벤션을 준수하며 최소한의 변경으로 요구사항을 구현합니다.

## Context
- **프로젝트**: shrimp-tm-demo (Spring Boot 3.4.1 멀티모듈)
- **목적**: batch-source → api-emerging-tech 내부 API 호출 시 응답 결과의 가시성(observability) 확보
- **현재 상태**:
  - `EmergingTechInternalContract`의 반환 타입이 `ApiResponse<Object>`로 되어 있어 응답 데이터의 타입 정보가 소실됨
  - batch-source Writer에서 응답 코드만 검증하고 응답 데이터는 로깅하지 않음
  - api-emerging-tech Facade에서 내부 API 호출 결과를 로깅하지 않음

## 참고 파일

### api-emerging-tech 모듈
- `@api/emerging-tech/src/main/java/.../facade/EmergingTechFacade.java` - Facade 레이어
- `@api/emerging-tech/src/main/java/.../controller/EmergingTechController.java` - 컨트롤러
- `@api/emerging-tech/src/main/java/.../dto/response/EmergingTechDetailResponse.java` - 단건 응답 DTO
- `@api/emerging-tech/src/main/java/.../dto/response/EmergingTechBatchResponse.java` - 다건 응답 DTO

### client-feign 모듈
- `@client/feign/src/main/java/.../domain/internal/contract/EmergingTechInternalContract.java` - Contract 인터페이스
- `@client/feign/src/main/java/.../domain/internal/contract/InternalApiDto.java` - 요청 DTO
- `@client/feign/src/main/java/.../domain/internal/api/EmergingTechInternalApi.java` - API 구현체
- `@client/feign/src/main/java/.../domain/internal/client/EmergingTechInternalFeignClient.java` - Feign 클라이언트

### batch-source 모듈
- `@batch/source/src/main/java/.../domain/emergingtech/scraper/writer/EmergingTechScraperWriter.java`
- `@batch/source/src/main/java/.../domain/emergingtech/rss/writer/EmergingTechRssWriter.java`
- `@batch/source/src/main/java/.../domain/emergingtech/github/writer/GitHubReleasesWriter.java`

## Task

아래 3개 모듈에 대해 순서대로 변경사항을 구현하세요.

---

### 작업 1: api-emerging-tech Facade 로깅 추가

**대상 파일**: `EmergingTechFacade.java`

**변경 내용**:
- `createEmergingTech()` 메서드에 info 레벨 로그 추가
- `createEmergingTechBatch()` 메서드에 info 레벨 로그 추가

**로깅 형식**:
```java
// 단건 생성
log.info("Emerging Tech 생성 완료: id={}, title={}, provider={}",
    response.id(), response.title(), response.provider());

// 다건 생성 (for문 이후, return 직전)
log.info("Emerging Tech 다건 생성 완료: total={}, success={}, failure={}",
    request.items().size(), successCount, failureCount);
```

**제약**: 기존 에러 로그(`log.error`)는 변경하지 않음. 새로운 info 로그만 추가.

---

### 작업 2: client-feign 응답 DTO 추가 및 Contract 수정

**2-1. 응답 DTO 생성**

**대상 파일** (신규): `InternalApiDto.java`에 내부 클래스 추가

api-emerging-tech 모듈의 응답 DTO와 매핑되는 응답 DTO를 `InternalApiDto` 클래스 내부에 추가:

```java
// EmergingTechDetailResponse에 대응
@Data
@Builder
public static class EmergingTechDetailResponse {
    private String id;
    private String provider;
    private String updateType;
    private String title;
    private String summary;
    private String url;
    private LocalDateTime publishedAt;
    private String sourceType;
    private String status;
    private String externalId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// EmergingTechBatchResponse에 대응
@Data
@Builder
public static class EmergingTechBatchResponse {
    private int totalCount;
    private int successCount;
    private int failureCount;
    private List<String> failureMessages;
}
```

**원칙**: DTO 독립성 - 기존 `InternalApiDto`의 주석에 명시된 "각 모듈에서 독립적으로 DTO 정의" 원칙을 준수. api-emerging-tech의 DTO를 import하지 않고 독립적으로 정의.

**2-2. Contract 인터페이스 반환 타입 변경**

**대상 파일**: `EmergingTechInternalContract.java`

| 메서드 | 변경 전 | 변경 후 |
|--------|---------|---------|
| `createEmergingTechInternal` | `ApiResponse<Object>` | `ApiResponse<InternalApiDto.EmergingTechDetailResponse>` |
| `createEmergingTechBatchInternal` | `ApiResponse<Object>` | `ApiResponse<InternalApiDto.EmergingTechBatchResponse>` |

나머지 메서드(`searchEmergingTech`, `approveEmergingTech`)는 이번 범위에서 제외.

**2-3. API 구현체 반환 타입 동기화**

**대상 파일**: `EmergingTechInternalApi.java`

Contract 변경에 맞춰 `createEmergingTechInternal`, `createEmergingTechBatchInternal` 메서드의 반환 타입을 동일하게 수정.

---

### 작업 3: batch-source Writer 응답 로깅 추가

**대상 파일**: 3개 Writer 클래스 모두 동일하게 수정
- `EmergingTechScraperWriter.java`
- `EmergingTechRssWriter.java`
- `GitHubReleasesWriter.java`

**변경 내용**:

3-1. `ApiResponse<Object>` → `ApiResponse<InternalApiDto.EmergingTechBatchResponse>`로 변경

3-2. `validateAndLogResponse` 메서드에서 응답 데이터 로깅 추가:

```java
private void validateAndLogResponse(
        ApiResponse<InternalApiDto.EmergingTechBatchResponse> response,
        int requestedCount) {
    if (response == null) {
        throw new RuntimeException("...: Response is null");
    }

    if (!SUCCESS_CODE.equals(response.code())) {
        throw new RuntimeException("...: " + response.message());
    }

    // 응답 데이터 로깅
    InternalApiDto.EmergingTechBatchResponse data = response.data();
    if (data != null) {
        log.info("Internal API 응답: total={}, success={}, failure={}",
            data.getTotalCount(), data.getSuccessCount(), data.getFailureCount());
        if (data.getFailureCount() > 0) {
            log.warn("Internal API 실패 항목: {}", data.getFailureMessages());
        }
    }

    log.info("... batch creation completed: requested={}", requestedCount);
}
```

---

## 제약사항

1. **오버엔지니어링 금지**: 위에 명시된 변경사항만 구현. 불필요한 리팩토링, 추상화, 유틸 클래스 추가 금지.
2. **기존 컨벤션 준수**: `@Slf4j`, `@Data`, `@Builder`, record 패턴 등 기존 코드 스타일 유지.
3. **최소한의 한글 주석**: 새로 추가하는 DTO에 1줄 Javadoc 주석만 추가.
4. **하위 호환성**: `searchEmergingTech`, `approveEmergingTech`는 `ApiResponse<Object>` 유지.

## 검증 기준

- [ ] api-emerging-tech Facade에서 단건/다건 생성 시 info 로그 출력
- [ ] client-feign `InternalApiDto`에 응답 DTO 2개 추가
- [ ] `EmergingTechInternalContract`의 2개 메서드 반환 타입이 구체적 DTO로 변경
- [ ] `EmergingTechInternalApi`의 반환 타입 동기화
- [ ] batch-source 3개 Writer에서 응답 데이터를 info 레벨로 로깅
- [ ] 실패 항목이 있을 경우 warn 레벨로 failureMessages 로깅
- [ ] 빌드 성공 (`./gradlew build`)
