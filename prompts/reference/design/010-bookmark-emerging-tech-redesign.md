# 북마크 API EmergingTech 전용 재설계 프롬프트

## 역할 정의

당신은 Spring Boot 멀티모듈 프로젝트 전문 백엔드 아키텍트입니다. CQRS 패턴, JPA, MongoDB 기반의 기존 프로젝트 구조를 완전히 이해하고 있으며, 최소한의 변경으로 기능을 재설계할 수 있는 전문가입니다.

## 프로젝트 컨텍스트

### 기술 스택
- Java 21, Spring Boot 3.4.1, Spring Cloud 2024.0.0
- Command Side: Aurora MySQL (MariaDB), JPA + QueryDSL
- Query Side: MongoDB Atlas, Spring Data MongoDB
- 동기화: Kafka 기반 CQRS
- ID 전략: TSID (Aurora), ObjectId (MongoDB)

### 변경 대상 모듈
1. **`domain/aurora`**: `BookmarkEntity` 컬럼 변경, Repository 수정
2. **`api/bookmark`**: 엔티티 변경에 따른 API 전체 반영 (DTO, Service, Facade, Controller)
3. **`docs`**: 관련 설계서 업데이트

### 현재 BookmarkEntity 구조
```java
@Entity
@Table(name = "bookmarks")
public class BookmarkEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_type", length = 50, nullable = false)  // 삭제 대상
    private String itemType;

    @Column(name = "item_id", length = 255, nullable = false)   // 삭제 대상
    private String itemId;

    @Column(name = "tag", length = 100)
    private String tag;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;
}
```

### 현재 EmergingTechDocument 구조
```java
@Document(collection = "emerging_techs")
public class EmergingTechDocument {
    @Id
    private ObjectId id;                    // MongoDB ObjectId

    @Field("provider")
    private String provider;                // TechProvider enum (OPENAI, ANTHROPIC, GOOGLE, META, XAI)

    @Field("title")
    private String title;

    @Field("summary")
    private String summary;

    @Field("url")
    @Indexed
    private String url;

    @Field("published_at")
    @Indexed
    private LocalDateTime publishedAt;

    // ... (기타 필드 생략)
}
```

## Task

### 목표
북마크 기능을 **EmergingTech 전용**으로 재설계합니다. 기존 범용 `itemType`/`itemId` 구조를 제거하고, `EmergingTechDocument`의 핵심 데이터를 직접 저장하는 구조로 변경합니다.

### 변경 범위

#### 1. `domain/aurora` - BookmarkEntity 컬럼 변경

**삭제할 컬럼**:
- `item_type` (String)
- `item_id` (String)

**추가할 컬럼** (EmergingTechDocument에서 비정규화):
- `emerging_tech_id` (String, NOT NULL): EmergingTechDocument의 ObjectId 문자열
- `title` (String, NOT NULL): EmergingTech 제목
- `url` (String, NOT NULL): EmergingTech 원본 URL
- `provider` (String): TechProvider (OPENAI, ANTHROPIC 등)
- `summary` (String, TEXT): EmergingTech 요약
- `published_at` (LocalDateTime): EmergingTech 게시일

**UNIQUE 제약조건 변경**:
- 기존: `(user_id, item_type, item_id)`
- 변경: `(user_id, emerging_tech_id, url)`

**수정 대상 파일**:
- `domain/aurora/src/main/java/.../entity/bookmark/BookmarkEntity.java`
- `domain/aurora/src/main/java/.../repository/reader/bookmark/BookmarkReaderRepository.java`

**참고 파일**:
- `domain/aurora/src/main/java/.../entity/BaseEntity.java` - 기본 엔티티 구조
- `domain/aurora/src/main/java/.../repository/writer/bookmark/BookmarkWriterRepository.java` - Writer 패턴
- `domain/mongodb/src/main/java/.../document/EmergingTechDocument.java` - 참조 대상 Document

#### 2. `api/bookmark` - API 전체 반영

엔티티 변경에 따라 아래 파일들을 **모두** 수정:

**DTO 수정**:
- `BookmarkCreateRequest`: `itemType`/`itemId` → `emergingTechId` (ObjectId 문자열)
  - 저장 시 `emergingTechId`로 MongoDB 조회하여 title, url, provider, summary, publishedAt 자동 채움
- `BookmarkDetailResponse`: 새 컬럼 반영
- `BookmarkListResponse`: 새 컬럼 반영
- 기타 관련 DTO

**Service 수정**:
- `BookmarkCommandServiceImpl`:
  - `saveBookmark()`: EmergingTechDocument 조회 후 비정규화 데이터 저장
  - 중복 검증 로직 변경 (`emergingTechId` + `url` 기준)
  - MongoDB 조회를 위한 의존성 추가
- `BookmarkQueryServiceImpl`: 필터/검색 조건 변경

**Facade 수정**:
- `BookmarkFacade`: DTO 변환 로직 변경

**Controller 수정**:
- `BookmarkController`: 필요 시 엔드포인트 파라미터 변경

**Repository 수정**:
- `BookmarkReaderRepository`: 중복 검증 쿼리 변경 (`findByUserIdAndItemTypeAndItemIdAndIsDeletedFalse` → 새 조건)

**예외 처리 수정**:
- `BookmarkItemNotFoundException` 등 관련 예외 메시지 변경

**참고 파일** (기존 패턴 분석용):
- `api/bookmark/src/main/java/.../controller/BookmarkController.java`
- `api/bookmark/src/main/java/.../facade/BookmarkFacade.java`
- `api/bookmark/src/main/java/.../service/BookmarkCommandServiceImpl.java`
- `api/bookmark/src/main/java/.../service/BookmarkQueryServiceImpl.java`
- `api/bookmark/src/main/java/.../dto/request/BookmarkCreateRequest.java`
- `api/bookmark/src/main/java/.../dto/response/BookmarkDetailResponse.java`

#### 3. `docs` - 관련 설계서 업데이트

아래 설계서에서 bookmark 관련 섹션을 수정:
- `docs/step13/user-bookmark-feature-design.md`: 북마크 기능 설계서 전체 반영
- `docs/step1/3. aurora-schema-design.md`: bookmarks 테이블 DDL 변경 (해당 섹션이 있는 경우)
- `docs/step2/2. data-model-design.md`: Bookmark 데이터 모델 변경 (해당 섹션이 있는 경우)

### MongoDB 조회 설계

북마크 생성 시 `emergingTechId`(ObjectId)로 `EmergingTechDocument`를 조회하여 비정규화 데이터를 채워야 합니다.

**조회 방법**:
- `api/bookmark` 모듈에서 `domain/mongodb` 모듈의 `EmergingTechRepository` 의존성 추가
- `EmergingTechRepository.findById(ObjectId)` 호출
- 조회 결과에서 `title`, `url`, `provider`, `summary`, `publishedAt` 추출하여 `BookmarkEntity`에 저장
- 조회 실패 시 `BookmarkItemNotFoundException` 발생

**참고**: `api/bookmark`의 `build.gradle`에 `domain-mongodb` 의존성 추가 필요

## 제약사항

### 필수 준수

1. **SOLID 원칙**: 각 클래스 단일 책임, 인터페이스 기반 설계, 의존성 역전
2. **클린코드**: 의미 있는 이름, 작은 함수, DRY 원칙
3. **기존 패턴 준수**: Controller → Facade → Service → Repository 계층 구조 유지
4. **최소한의 한글 주석**: 코드 자체로 의도가 명확하지 않은 부분에만 간결한 한글 주석 추가
5. **기존 기능 보존**: History 관리, Soft Delete, 복구, 검색 기능은 그대로 유지 (컬럼만 변경)
6. **Flyway 마이그레이션**: DDL 변경 시 Flyway 스크립트 작성 포함

### 금지 사항

1. ❌ **오버엔지니어링**: 요구사항 외 기능 추가 금지. 불필요한 추상화 레이어 생성 금지
2. ❌ **불필요한 리팩토링**: 변경 대상이 아닌 코드 수정 금지
3. ❌ **장황한 주석**: LLM 스타일의 과도한 주석 금지. JavaDoc은 public API에만 작성
4. ❌ **비공식 자료 참고**: 블로그, 개인 문서, Stack Overflow 참고 금지
5. ❌ **Contest/News 북마크 호환**: 기존 `itemType`/`itemId` 기반 로직은 완전히 제거 (하위 호환 불필요)
6. ❌ **추가 기능 제안**: "이런 기능도 추가하면 좋겠습니다" 등의 제안 금지

## 작업 순서

### Step 1: 현재 코드 분석
1. `domain/aurora`의 BookmarkEntity, BookmarkReaderRepository, BookmarkWriterRepository 전체 파악
2. `api/bookmark`의 모든 Java 파일 전체 파악
3. `domain/mongodb`의 EmergingTechDocument, EmergingTechRepository 전체 파악
4. `docs` 폴더의 bookmark 관련 설계서 파악

### Step 2: domain/aurora 변경
1. `BookmarkEntity` 컬럼 변경 (itemType/itemId 삭제, 새 컬럼 추가)
2. `BookmarkReaderRepository` 쿼리 메서드 변경
3. Flyway 마이그레이션 스크립트 작성

### Step 3: api/bookmark 변경
1. DTO 수정 (Request/Response)
2. Service 수정 (Command/Query)
3. Facade 수정
4. Controller 수정 (필요 시)
5. build.gradle에 `domain-mongodb` 의존성 추가

### Step 4: docs 업데이트
1. 북마크 기능 설계서 업데이트
2. 스키마 설계서 업데이트
3. 데이터 모델 설계서 업데이트

### Step 5: 검증
- 컴파일 확인: `./gradlew :api-bookmark:build`
- 모든 변경 파일에서 `itemType`, `itemId` 참조가 완전히 제거되었는지 확인
- UNIQUE 제약조건이 올바르게 변경되었는지 확인
- MongoDB 조회 로직이 정상 동작 가능한 구조인지 확인

## 검증 체크리스트

- [ ] `BookmarkEntity`에서 `itemType`, `itemId` 컬럼이 완전히 제거됨
- [ ] `emergingTechId`, `title`, `url`, `provider`, `summary`, `publishedAt` 컬럼이 추가됨
- [ ] UNIQUE 제약조건이 `(user_id, emerging_tech_id, url)`로 변경됨
- [ ] `BookmarkCreateRequest`에서 `emergingTechId`만 받고, 나머지는 MongoDB 조회로 채움
- [ ] `BookmarkDetailResponse`에 새 컬럼이 반영됨
- [ ] `BookmarkCommandServiceImpl`에서 EmergingTechDocument 조회 로직이 추가됨
- [ ] `BookmarkReaderRepository`의 중복 검증 쿼리가 변경됨
- [ ] `api/bookmark`의 `build.gradle`에 `domain-mongodb` 의존성이 추가됨
- [ ] 기존 History, Soft Delete, 복구 기능이 정상 동작함
- [ ] 모든 코드에서 `itemType`, `itemId` 참조가 완전히 제거됨
- [ ] 관련 설계서가 업데이트됨
- [ ] Flyway 마이그레이션 스크립트가 작성됨
- [ ] 최소한의 한글 주석이 추가됨
- [ ] SOLID 원칙과 클린코드 원칙이 준수됨

## 참고 자료

작성 시 **반드시** 다음 문서만 참고:
1. `docs/step13/user-bookmark-feature-design.md` - 현재 북마크 기능 설계서
2. `docs/step11/cqrs-kafka-sync-design.md` - CQRS Kafka 동기화 설계
3. `docs/step1/3. aurora-schema-design.md` - Aurora 스키마 설계
4. `docs/step2/2. data-model-design.md` - 데이터 모델 설계
5. Spring Data JPA 공식 문서
6. Spring Data MongoDB 공식 문서

---

**작성 시작**: 위 지침에 따라 Step 1부터 순서대로 진행하세요.
