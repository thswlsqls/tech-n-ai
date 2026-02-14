# 북마크 태그 다중값 저장 설계 개선 프롬프트

## 역할 정의

당신은 Spring Boot 멀티모듈 프로젝트 전문 백엔드 개발자입니다. JPA Entity, DTO, Service 레이어에서 구분자 기반 다중값 처리 패턴을 이해하고 있으며, 최소한의 변경으로 기능을 구현할 수 있습니다.

## 프로젝트 컨텍스트

### 기술 스택
- Java 21, Spring Boot 3.4.1
- Aurora MySQL (MariaDB), JPA
- ID 전략: TSID

### 변경 대상 모듈
1. **`domain/aurora`**: `BookmarkEntity`의 tag 처리 로직 추가
2. **`api/bookmark`**: DTO의 tag 필드를 `List<String>`으로 변경, Service/Facade 변환 로직 추가

### 현재 BookmarkEntity 구조
```java
@Entity
@Table(name = "bookmarks")
public class BookmarkEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "emerging_tech_id", nullable = false, length = 24)
    private String emergingTechId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "published_at", precision = 6)
    private LocalDateTime publishedAt;

    @Column(name = "tag", length = 100)
    private String tag;  // 현재: 단일 문자열

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
}
```

### 현재 DTO 구조

**BookmarkCreateRequest**:
```java
public record BookmarkCreateRequest(
    @NotBlank(message = "EmergingTech ID는 필수입니다.")
    String emergingTechId,
    String tag,   // 현재: 단일 문자열
    String memo
) {}
```

**BookmarkUpdateRequest**:
```java
public record BookmarkUpdateRequest(
    String tag,   // 현재: 단일 문자열
    String memo
) {}
```

**BookmarkDetailResponse**:
```java
public record BookmarkDetailResponse(
    String bookmarkTsid,
    String userId,
    String emergingTechId,
    String title,
    String url,
    String provider,
    String summary,
    LocalDateTime publishedAt,
    String tag,   // 현재: 단일 문자열
    String memo,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {}
```

## Task

### 목표
`tag` 컬럼에 구분자(`|`)를 사용하여 N개의 태그를 저장하고, API 응답 시 `List<String>`으로 반환하도록 개선합니다.

### 설계 요구사항

1. **DB 컬럼**: 변경 없음 (`VARCHAR(100)` 유지)
2. **구분자**: `|` (파이프) 사용
   - 예: `"AI|Machine Learning|Python"` → `["AI", "Machine Learning", "Python"]`
3. **Request DTO**: `List<String> tags`로 변경
4. **Response DTO**: `List<String> tags`로 변경
5. **Entity**: 내부 저장은 `String tag` 유지, 변환 메서드 추가

### 변경 범위

#### 1. `domain/aurora` - BookmarkEntity 변환 메서드 추가

**수정 파일**: `domain/aurora/src/main/java/.../entity/bookmark/BookmarkEntity.java`

**추가할 메서드**:
```java
// 구분자 상수
private static final String TAG_DELIMITER = "|";

// List<String> → String 변환 (저장용)
public void setTagsAsList(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
        this.tag = null;
    } else {
        this.tag = String.join(TAG_DELIMITER, tags);
    }
}

// String → List<String> 변환 (조회용)
public List<String> getTagsAsList() {
    if (this.tag == null || this.tag.isBlank()) {
        return List.of();
    }
    return Arrays.asList(this.tag.split("\\" + TAG_DELIMITER));
}
```

**updateContent 메서드 수정**:
```java
public void updateContent(List<String> tags, String memo) {
    if (tags != null) {
        setTagsAsList(tags);
    }
    if (memo != null) {
        this.memo = memo;
    }
}
```

#### 2. `api/bookmark` - DTO 변경

**BookmarkCreateRequest 수정**:
```java
public record BookmarkCreateRequest(
    @NotBlank(message = "EmergingTech ID는 필수입니다.")
    String emergingTechId,
    List<String> tags,  // 변경: String → List<String>
    String memo
) {}
```

**BookmarkUpdateRequest 수정**:
```java
public record BookmarkUpdateRequest(
    List<String> tags,  // 변경: String → List<String>
    String memo
) {}
```

**BookmarkDetailResponse 수정**:
```java
public record BookmarkDetailResponse(
    String bookmarkTsid,
    String userId,
    String emergingTechId,
    String title,
    String url,
    String provider,
    String summary,
    LocalDateTime publishedAt,
    List<String> tags,  // 변경: String → List<String>
    String memo,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy
) {
    public static BookmarkDetailResponse from(BookmarkEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BookmarkDetailResponse(
            entity.getId() != null ? entity.getId().toString() : null,
            entity.getUserId() != null ? entity.getUserId().toString() : null,
            entity.getEmergingTechId(),
            entity.getTitle(),
            entity.getUrl(),
            entity.getProvider(),
            entity.getSummary(),
            entity.getPublishedAt(),
            entity.getTagsAsList(),  // 변경: getTag() → getTagsAsList()
            entity.getMemo(),
            entity.getCreatedAt(),
            entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null,
            entity.getUpdatedAt(),
            entity.getUpdatedBy() != null ? entity.getUpdatedBy().toString() : null
        );
    }
}
```

#### 3. `api/bookmark` - Service 변경

**BookmarkCommandServiceImpl 수정**:

`createBookmark` 메서드에서:
```java
bookmark.setTagsAsList(request.tags());  // 변경: setTag() → setTagsAsList()
```

`updateBookmark` 메서드에서:
```java
bookmark.updateContent(request.tags(), request.memo());  // 기존 시그니처 변경됨
```

### 변경 대상 파일 목록

1. `domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/mariadb/entity/bookmark/BookmarkEntity.java`
2. `api/bookmark/src/main/java/com/ebson/shrimp/tm/demo/api/bookmark/dto/request/BookmarkCreateRequest.java`
3. `api/bookmark/src/main/java/com/ebson/shrimp/tm/demo/api/bookmark/dto/request/BookmarkUpdateRequest.java`
4. `api/bookmark/src/main/java/com/ebson/shrimp/tm/demo/api/bookmark/dto/response/BookmarkDetailResponse.java`
5. `api/bookmark/src/main/java/com/ebson/shrimp/tm/demo/api/bookmark/service/BookmarkCommandServiceImpl.java`

## 제약사항

### 필수 준수

1. **DB 스키마 변경 없음**: `tag VARCHAR(100)` 유지
2. **구분자**: `|` (파이프) 문자 사용
3. **빈 태그 처리**: `null` 또는 빈 리스트 → `null` 저장
4. **기존 데이터 호환**: 기존 단일 태그 데이터도 `List<String>`으로 정상 조회되어야 함

### 금지 사항

1. **별도 태그 테이블 생성 금지**: 정규화 목적의 별도 테이블 생성 불필요
2. **JPA AttributeConverter 사용 금지**: 단순한 변환 메서드로 충분
3. **오버엔지니어링 금지**: 태그 검증, 정렬, 중복 제거 등 추가 로직 불필요
4. **불필요한 리팩토링 금지**: 변경 대상이 아닌 코드 수정 금지
5. **추가 기능 제안 금지**: 태그 자동완성, 태그 통계 등 추가 기능 제안 금지

## 작업 순서

### Step 1: BookmarkEntity 변환 메서드 추가
1. `TAG_DELIMITER` 상수 추가
2. `setTagsAsList(List<String>)` 메서드 추가
3. `getTagsAsList()` 메서드 추가
4. `updateContent` 메서드 시그니처 변경

### Step 2: Request DTO 변경
1. `BookmarkCreateRequest`의 `tag` → `tags` (`List<String>`)
2. `BookmarkUpdateRequest`의 `tag` → `tags` (`List<String>`)

### Step 3: Response DTO 변경
1. `BookmarkDetailResponse`의 `tag` → `tags` (`List<String>`)
2. `from()` 메서드에서 `getTagsAsList()` 호출

### Step 4: Service 변경
1. `BookmarkCommandServiceImpl`의 `createBookmark`에서 `setTagsAsList()` 사용
2. `updateContent` 호출 부분 수정

### Step 5: 검증
- 컴파일 확인: `./gradlew :api-bookmark:build`
- 기존 `tag` 필드 참조가 모두 `tags` / `getTagsAsList()`로 변경되었는지 확인

## 검증 체크리스트

- [ ] `BookmarkEntity`에 `TAG_DELIMITER` 상수가 추가됨
- [ ] `BookmarkEntity`에 `setTagsAsList(List<String>)` 메서드가 추가됨
- [ ] `BookmarkEntity`에 `getTagsAsList()` 메서드가 추가됨
- [ ] `BookmarkEntity.updateContent()` 시그니처가 `(List<String>, String)`으로 변경됨
- [ ] `BookmarkCreateRequest`의 필드가 `tags: List<String>`으로 변경됨
- [ ] `BookmarkUpdateRequest`의 필드가 `tags: List<String>`으로 변경됨
- [ ] `BookmarkDetailResponse`의 필드가 `tags: List<String>`으로 변경됨
- [ ] `BookmarkDetailResponse.from()`에서 `entity.getTagsAsList()` 호출함
- [ ] `BookmarkCommandServiceImpl`에서 `setTagsAsList()` 사용함
- [ ] `./gradlew :api-bookmark:build` 성공

## 참고 자료

작성 시 다음 파일만 참고:
1. `domain/aurora/src/main/java/.../entity/bookmark/BookmarkEntity.java` - 현재 Entity 구조
2. `api/bookmark/src/main/java/.../dto/` - 현재 DTO 구조
3. `api/bookmark/src/main/java/.../service/BookmarkCommandServiceImpl.java` - 현재 Service 구조

---

**작성 시작**: 위 지침에 따라 Step 1부터 순서대로 진행하세요.
