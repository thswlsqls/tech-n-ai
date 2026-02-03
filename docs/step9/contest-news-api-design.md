> ⚠️ 본 설계서는 Contest/News 수집 기능 폐기로 더 이상 유효하지 않습니다. (폐기됨)

# Contest 및 News API 구현 설계서

**작성 일시**: 2026-01-07  
**대상 모듈**: `api-contest`, `api-news`  
**아키텍처**: CQRS 패턴 기반 (Query Side: MongoDB Atlas)

## 목차

1. [개요](#개요)
2. [API 모듈 구조 설계](#api-모듈-구조-설계)
3. [데이터 수집 및 저장 흐름 설계](#데이터-수집-및-저장-흐름-설계)
4. [MongoDB 저장 API 설계](#mongodb-저장-api-설계)
5. [조회 API 설계](#조회-api-설계)
6. [에러 처리 설계](#에러-처리-설계)
7. [테스트 전략](#테스트-전략)

---

## 개요

이 설계서는 `api-contest`와 `api-news` 모듈의 API 구현을 위한 종합 설계 문서입니다. 두 모듈은 MongoDB Atlas를 사용하여 Contest 및 News 데이터를 조회하는 읽기 전용 API를 제공하며, Batch 모듈을 통해 데이터를 수집하고 저장합니다.

### 설계 원칙

1. **프로젝트 구조 일관성**: `api-auth`, `api-gateway` 모듈 구조를 참고하여 일관성 있는 패키지 구조 설계
2. **Facade 패턴**: Controller → Facade → Service → Repository 계층 구조 유지
3. **CQRS 패턴**: 읽기 전용 API (MongoDB Atlas만 사용, Aurora DB 미사용), 읽기 데이터 세팅을 위한 쓰기 API 사용 
4. **트랜잭션 관리**: 단건 처리와 다건 처리의 트랜잭션 전략 분리
5. **클린코드 원칙**: SOLID 원칙 준수, 단일 책임 원칙, 의존성 역전 원칙

### 참고 문서

- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
- **RSS/Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
- **API 응답 형식 설계**: `docs/step2/3. api-response-format-design.md`
- **에러 처리 전략 설계**: `docs/step2/4. error-handling-strategy-design.md`

---

## API 모듈 구조 설계

### 패키지 구조

#### api-contest 모듈

```
api-contest/
  src/main/java/com/tech/n/ai/api/contest/
    ContestApplication.java
    controller/
      ContestController.java
    facade/
      ContestFacade.java
    service/
      ContestService.java
      ContestServiceImpl.java
    dto/
      request/
        ContestListRequest.java
        ContestSearchRequest.java
        ContestCreateRequest.java (내부 API용)
        ContestBatchRequest.java (내부 API용)
      response/
        ContestListResponse.java
        ContestDetailResponse.java
        ContestSearchResponse.java
        ContestBatchResponse.java (내부 API용)
    config/
      ContestConfig.java
    common/
      exception/
        ContestExceptionHandler.java
        ContestNotFoundException.java
        ContestValidationException.java
        ContestDuplicateException.java
```

#### api-news 모듈

```
api-news/
  src/main/java/com/tech/n/ai/api/news/
    NewsApplication.java
    controller/
      NewsController.java
    facade/
      NewsFacade.java
    service/
      NewsService.java
      NewsServiceImpl.java
    dto/
      request/
        NewsListRequest.java
        NewsSearchRequest.java
        NewsCreateRequest.java (내부 API용)
        NewsBatchRequest.java (내부 API용)
      response/
        NewsListResponse.java
        NewsDetailResponse.java
        NewsSearchResponse.java
        NewsBatchResponse.java (내부 API용)
    config/
      NewsConfig.java
    common/
      exception/
        NewsExceptionHandler.java
        NewsNotFoundException.java
        NewsValidationException.java
        NewsDuplicateException.java
```

### 계층 구조

#### 1. Controller 계층

**역할**: HTTP 요청/응답 처리

**특징**:
- `@RestController`, `@RequestMapping("/api/v1/contest")` 또는 `@RequestMapping("/api/v1/news")`
- Facade 호출
- DTO 변환
- 공통 응답 형식 (`ApiResponse<T>`) 사용

**예시**:
```java
@RestController
@RequestMapping("/api/v1/contest")
@RequiredArgsConstructor
public class ContestController {
    private final ContestFacade contestFacade;
    
    @GetMapping
    public ResponseEntity<ApiResponse<ContestListResponse>> getContestList(
            @Valid ContestListRequest request) {
        ContestListResponse response = contestFacade.getContestList(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> getContestDetail(
            @PathVariable String id) {
        ContestDetailResponse response = contestFacade.getContestDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ContestSearchResponse>> searchContest(
            @Valid ContestSearchRequest request) {
        ContestSearchResponse response = contestFacade.searchContest(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // 내부 API (Batch 모듈 전용)
    @PostMapping("/internal")
    public ResponseEntity<ApiResponse<ContestDetailResponse>> createContestInternal(
            @Valid @RequestBody ContestCreateRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        ContestDetailResponse response = contestFacade.createContest(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/internal/batch")
    public ResponseEntity<ApiResponse<ContestBatchResponse>> createContestBatchInternal(
            @Valid @RequestBody ContestBatchRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        // 내부 API 키 검증
        validateInternalApiKey(apiKey);
        
        ContestBatchResponse response = contestFacade.createContestBatch(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

#### 2. Facade 계층

**역할**: Controller와 Service 사이의 중간 계층

**특징**:
- 비즈니스 로직 조합
- 다건 처리 API의 부분 롤백 구현
  - 단건 처리 Service 메서드를 반복 호출
  - 예외 발생 시 catch하여 로그만 출력하고 다음 항목 계속 처리
  - `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)

**예시**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ContestFacade {
    private final ContestService contestService;
    
    public ContestListResponse getContestList(ContestListRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getSize(),
            parseSort(request.getSort())
        );
        
        Page<ContestDocument> page = contestService.findContests(
            request.getSourceId(),
            request.getStatus(),
            pageable
        );
        
        return ContestListResponse.from(page);
    }
    
    public ContestDetailResponse getContestDetail(String id) {
        ContestDocument document = contestService.findContestById(id);
        return ContestDetailResponse.from(document);
    }
    
    public ContestSearchResponse searchContest(ContestSearchRequest request) {
        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            request.getSize()
        );
        
        Page<ContestDocument> page = contestService.searchContest(
            request.getQuery(),
            pageable
        );
        
        return ContestSearchResponse.from(page);
    }
    
    // 단건 처리 (내부 API)
    public ContestDetailResponse createContest(ContestCreateRequest request) {
        ContestDocument document = contestService.saveContest(request);
        return ContestDetailResponse.from(document);
    }
    
    // 다건 처리 (내부 API) - 부분 롤백 구현
    // @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
    public ContestBatchResponse createContestBatch(ContestBatchRequest request) {
        int successCount = 0;
        int failureCount = 0;
        List<String> failureMessages = new ArrayList<>();
        
        for (ContestCreateRequest item : request.getContests()) {
            try {
                // 단건 처리 Service 메서드 호출 (각 호출마다 독립적인 트랜잭션)
                contestService.saveContest(item);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                String errorMessage = String.format(
                    "Contest 저장 실패: sourceId=%s, title=%s, error=%s",
                    item.getSourceId(), item.getTitle(), e.getMessage()
                );
                log.error(errorMessage, e);
                failureMessages.add(errorMessage);
                // 예외를 catch하고 로그만 출력하여 다음 항목 계속 처리
            }
        }
        
        return ContestBatchResponse.builder()
            .totalCount(request.getContests().size())
            .successCount(successCount)
            .failureCount(failureCount)
            .failureMessages(failureMessages)
            .build();
    }
}
```

#### 3. Service 계층

**역할**: 핵심 비즈니스 로직

**특징**:
- MongoDB Repository 호출
- 데이터 검증 및 변환
- 페이징 처리
- 단건 처리 API의 트랜잭션 관리
  - `@Transactional` 어노테이션으로 트랜잭션 생성
  - 실패 시 자동 롤백

**예시**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ContestServiceImpl implements ContestService {
    private final ContestRepository contestRepository;
    
    @Override
    public Page<ContestDocument> findContests(
            ObjectId sourceId,
            String status,
            Pageable pageable) {
        Query query = new Query();
        
        if (sourceId != null) {
            query.addCriteria(Criteria.where("sourceId").is(sourceId));
        }
        
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        
        query.with(pageable);
        
        long total = mongoTemplate.count(query, ContestDocument.class);
        List<ContestDocument> list = mongoTemplate.find(query, ContestDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    @Override
    public ContestDocument findContestById(String id) {
        ObjectId objectId = new ObjectId(id);
        return contestRepository.findById(objectId)
            .orElseThrow(() -> new ContestNotFoundException("대회를 찾을 수 없습니다: " + id));
    }
    
    @Override
    public Page<ContestDocument> searchContest(String query, Pageable pageable) {
        TextCriteria criteria = TextCriteria.forDefaultLanguage()
            .matching(query);
        
        Query mongoQuery = TextQuery.queryText(criteria)
            .with(pageable);
        
        long total = mongoTemplate.count(mongoQuery, ContestDocument.class);
        List<ContestDocument> list = mongoTemplate.find(mongoQuery, ContestDocument.class);
        
        return new PageImpl<>(list, pageable, total);
    }
    
    // 단건 처리 - @Transactional 사용
    @Transactional
    @Override
    public ContestDocument saveContest(ContestCreateRequest request) {
        // 중복 체크
        if (contestRepository.existsBySourceIdAndUrl(
                new ObjectId(request.getSourceId()),
                request.getUrl())) {
            throw new ContestDuplicateException("이미 존재하는 대회입니다.");
        }
        
        // Document 생성 및 저장
        ContestDocument document = ContestDocument.builder()
            .sourceId(new ObjectId(request.getSourceId()))
            .title(request.getTitle())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .status(calculateStatus(request.getStartDate(), request.getEndDate()))
            .description(request.getDescription())
            .url(request.getUrl())
            .metadata(ContestDocument.ContestMetadata.builder()
                .sourceName(request.getMetadata().getSourceName())
                .prize(request.getMetadata().getPrize())
                .participants(request.getMetadata().getParticipants())
                .tags(request.getMetadata().getTags())
                .build())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return contestRepository.save(document);
    }
    
    private String calculateStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) {
            return "UPCOMING";
        } else if (now.isAfter(endDate)) {
            return "ENDED";
        } else {
            return "ONGOING";
        }
    }
}
```

#### 4. Repository 계층

**역할**: MongoDB 데이터 접근

**특징**:
- `domain-mongodb` 모듈의 Repository 사용
- 커스텀 쿼리 메서드 (필요 시)

**예시**:
```java
// domain-mongodb 모듈에 이미 정의됨
@Repository
public interface ContestRepository extends MongoRepository<ContestDocument, ObjectId> {
    boolean existsBySourceIdAndUrl(ObjectId sourceId, String url);
    List<ContestDocument> findBySourceIdOrderByStartDateDesc(ObjectId sourceId);
    List<ContestDocument> findByStatusOrderByStartDateDesc(String status);
}
```

---

## 데이터 수집 및 저장 흐름 설계

### 데이터 흐름

```
외부 출처 (RSS/Web/API)
  ↓
client-rss / client-scraper / client-feign
  ↓ (외부 요청, 데이터 수집 및 정제)
Batch 모듈 (batch-source)
  ├─ Item Reader: client/* 모듈의 수집 데이터 읽기
  ├─ Item Processor: Client DTO → API DTO 변환
  └─ Item Writer: api-contest/api-news 모듈로 HTTP 요청
  ↓
api-contest / api-news
  ↓ (MongoDB 저장)
MongoDB Atlas (ContestDocument / NewsArticleDocument)
```

### Batch 모듈 설계

#### 역할

- Client 모듈에서 수집한 데이터를 API 모듈로 전달
- Spring Batch의 Item Reader/Writer 패턴 사용

#### 구현 방식

1. **Item Reader**: `client-feign`, `client-rss`, `client-scraper` 모듈에서 수집한 데이터를 읽음
2. **Item Processor**: 데이터 검증, Client 모듈 DTO → API 모듈 DTO 변환
3. **Item Writer**: `api-contest`, `api-news` 모듈의 내부 API를 호출하여 MongoDB Atlas에 저장

#### 데이터 변환

- **Client 모듈 DTO**: `RssFeedItem`, `ScrapedContestItem` 등
- **API 모듈 DTO**: `ContestCreateRequest`, `NewsCreateRequest`
- **변환 로직**: Item Processor에서 처리

#### 에러 처리

- 재시도 로직: Spring Batch의 재시도 설정 활용
- Dead Letter Queue: 실패한 항목은 별도 큐에 저장하여 후속 처리

### API 모듈 저장 로직

#### 단건 처리 엔드포인트

- **엔드포인트**: `POST /api/v1/contest/internal` (내부 API, Batch 모듈 전용)
- **인증**: 내부 API 키 또는 서비스 간 인증
- **데이터 검증**: DTO 검증, 중복 체크
- **MongoDB 저장**: ContestDocument / NewsArticleDocument 저장
- **트랜잭션 관리**: Service 레이어에서 `@Transactional` 사용

#### 다건 처리 엔드포인트

- **엔드포인트**: `POST /api/v1/contest/internal/batch` (내부 API, Batch 모듈 전용)
- **인증**: 내부 API 키 또는 서비스 간 인증
- **데이터 검증**: 각 항목별 DTO 검증, 중복 체크
- **MongoDB 저장**: ContestDocument / NewsArticleDocument 저장
- **트랜잭션 관리**: 
  - Facade 레이어에서 단건 처리 Service 메서드를 반복 호출
  - 부분 롤백 구현: 실패한 항목은 롤백, 성공한 항목은 정상 커밋
  - Facade 레이어에는 `@Transactional` 사용하지 않음

---

## MongoDB 저장 API 설계

### 단건 처리 API

#### 저장 API 엔드포인트

```http
POST /api/v1/contest/internal
Content-Type: application/json
Authorization: Bearer {internal-api-key}
X-Internal-Api-Key: {internal-api-key}

Request Body:
{
  "sourceId": "507f1f77bcf86cd799439011",
  "title": "Codeforces Round 900",
  "startDate": "2026-01-15T10:00:00Z",
  "endDate": "2026-01-15T12:30:00Z",
  "status": "UPCOMING",
  "description": "Regular Codeforces contest",
  "url": "https://codeforces.com/contests/1900",
  "metadata": {
    "sourceName": "Codeforces API",
    "prize": null,
    "participants": null,
    "tags": ["algorithm", "competitive-programming"]
  }
}
```

#### 저장 로직 (Service 계층)

1. **트랜잭션 관리**: `@Transactional` 어노테이션으로 트랜잭션 생성
2. **데이터 검증**: DTO 검증 (`@Valid`)
3. **중복 체크**: `sourceId + url` 또는 `sourceId + title + startDate` 기준
4. **Document 생성**: ContestDocument / NewsArticleDocument 생성
5. **MongoDB 저장**: Repository.save()
6. **트랜잭션 커밋**: 성공 시 자동 커밋, 실패 시 자동 롤백
7. **응답 반환**: 저장된 Document ID 반환

#### 구현 예시

```java
@Transactional
@Override
public ContestDocument saveContest(ContestCreateRequest request) {
    // 중복 체크
    if (contestRepository.existsBySourceIdAndUrl(
            new ObjectId(request.getSourceId()),
            request.getUrl())) {
        throw new ContestDuplicateException("이미 존재하는 대회입니다.");
    }
    
    // Document 생성 및 저장
    ContestDocument document = ContestDocument.builder()
        .sourceId(new ObjectId(request.getSourceId()))
        .title(request.getTitle())
        .startDate(request.getStartDate())
        .endDate(request.getEndDate())
        .status(calculateStatus(request.getStartDate(), request.getEndDate()))
        .description(request.getDescription())
        .url(request.getUrl())
        .metadata(ContestDocument.ContestMetadata.builder()
            .sourceName(request.getMetadata().getSourceName())
            .prize(request.getMetadata().getPrize())
            .participants(request.getMetadata().getParticipants())
            .tags(request.getMetadata().getTags())
            .build())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    return contestRepository.save(document);
}
```

### 다건 처리 API

#### 저장 API 엔드포인트

```http
POST /api/v1/contest/internal/batch
Content-Type: application/json
Authorization: Bearer {internal-api-key}
X-Internal-Api-Key: {internal-api-key}

Request Body:
{
  "contests": [
    {
      "sourceId": "507f1f77bcf86cd799439011",
      "title": "Codeforces Round 900",
      "startDate": "2026-01-15T10:00:00Z",
      "endDate": "2026-01-15T12:30:00Z",
      "status": "UPCOMING",
      "description": "Regular Codeforces contest",
      "url": "https://codeforces.com/contests/1900",
      "metadata": {
        "sourceName": "Codeforces API",
        "prize": null,
        "participants": null,
        "tags": ["algorithm", "competitive-programming"]
      }
    },
    // ... 여러 개의 Contest 데이터
  ]
}
```

#### 저장 로직 (Facade 계층)

1. **트랜잭션 없음**: Facade 계층에서는 `@Transactional` 사용하지 않음
2. **반복 처리**: 요청된 리스트의 각 항목에 대해 단건 처리 API의 Service 메서드를 반복 호출
3. **부분 롤백 처리**: 
   - 각 항목 처리 시 예외 발생 시 `try-catch`로 예외를 catch
   - 예외 발생 시 로그만 출력하고 다음 항목 계속 처리
   - 실패한 항목은 롤백되지만, 성공한 항목은 정상 커밋됨
4. **응답 반환**: 성공/실패 통계 정보 반환

#### 구현 예시

```java
// @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
public ContestBatchResponse createContestBatch(ContestBatchRequest request) {
    int successCount = 0;
    int failureCount = 0;
    List<String> failureMessages = new ArrayList<>();
    
    for (ContestCreateRequest item : request.getContests()) {
        try {
            // 단건 처리 Service 메서드 호출 (각 호출마다 독립적인 트랜잭션)
            contestService.saveContest(item);
            successCount++;
        } catch (Exception e) {
            failureCount++;
            String errorMessage = String.format(
                "Contest 저장 실패: sourceId=%s, title=%s, error=%s",
                item.getSourceId(), item.getTitle(), e.getMessage()
            );
            log.error(errorMessage, e);
            failureMessages.add(errorMessage);
            // 예외를 catch하고 로그만 출력하여 다음 항목 계속 처리
        }
    }
    
    return ContestBatchResponse.builder()
        .totalCount(request.getContests().size())
        .successCount(successCount)
        .failureCount(failureCount)
        .failureMessages(failureMessages)
        .build();
}
```

#### 부분 롤백 설계 요구사항

- **단건 처리 API의 Service 레이어**: `@Transactional`로 트랜잭션 생성, 실패 시 롤백
- **다건 처리 API의 Facade 레이어**: 
  - 단건 처리 API의 Service 메서드를 반복 호출
  - 실패 시 예외를 catch하고 로그만 출력
  - 나머지 요소들은 정상 커밋되도록 처리
  - Facade 레이어에는 `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)

#### 응답 형식

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failureCount": 2,
    "failureMessages": [
      "Contest 저장 실패: sourceId=xxx, title=yyy, error=중복된 데이터",
      "Contest 저장 실패: sourceId=zzz, title=aaa, error=유효성 검증 실패"
    ]
  }
}
```

### News 저장 API

#### 단건 처리 API

##### 저장 API 엔드포인트

```http
POST /api/v1/news/internal
Content-Type: application/json
Authorization: Bearer {internal-api-key}
X-Internal-Api-Key: {internal-api-key}

Request Body:
{
  "sourceId": "507f1f77bcf86cd799439014",
  "title": "Spring Boot 4.0 Released",
  "content": "Spring Boot 4.0 has been released with new features...",
  "summary": "Spring Boot 4.0 brings significant improvements...",
  "publishedAt": "2026-01-05T08:00:00Z",
  "url": "https://example.com/news/spring-boot-4",
  "author": "John Doe",
  "metadata": {
    "sourceName": "Hacker News API",
    "tags": ["spring", "java", "framework"],
    "viewCount": 1500,
    "likeCount": 120
  }
}
```

##### 저장 로직 (Service 계층)

1. **트랜잭션 관리**: `@Transactional` 어노테이션으로 트랜잭션 생성
2. **데이터 검증**: DTO 검증 (`@Valid`)
3. **중복 체크**: `sourceId + url` 또는 `sourceId + title + publishedAt` 기준
4. **Document 생성**: NewsArticleDocument 생성
5. **MongoDB 저장**: Repository.save()
6. **트랜잭션 커밋**: 성공 시 자동 커밋, 실패 시 자동 롤백
7. **응답 반환**: 저장된 Document ID 반환

##### 구현 예시

```java
@Transactional
@Override
public NewsArticleDocument saveNews(NewsCreateRequest request) {
    // 중복 체크
    if (newsArticleRepository.existsBySourceIdAndUrl(
            new ObjectId(request.getSourceId()),
            request.getUrl())) {
        throw new NewsDuplicateException("이미 존재하는 뉴스 기사입니다.");
    }
    
    // Document 생성 및 저장
    NewsArticleDocument document = NewsArticleDocument.builder()
        .sourceId(new ObjectId(request.getSourceId()))
        .title(request.getTitle())
        .content(request.getContent())
        .summary(request.getSummary())
        .publishedAt(request.getPublishedAt())
        .url(request.getUrl())
        .author(request.getAuthor())
        .metadata(NewsArticleDocument.NewsArticleMetadata.builder()
            .sourceName(request.getMetadata().getSourceName())
            .tags(request.getMetadata().getTags())
            .viewCount(request.getMetadata().getViewCount())
            .likeCount(request.getMetadata().getLikeCount())
            .build())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
    
    return newsArticleRepository.save(document);
}
```

#### 다건 처리 API

##### 저장 API 엔드포인트

```http
POST /api/v1/news/internal/batch
Content-Type: application/json
Authorization: Bearer {internal-api-key}
X-Internal-Api-Key: {internal-api-key}

Request Body:
{
  "newsArticles": [
    {
      "sourceId": "507f1f77bcf86cd799439014",
      "title": "Spring Boot 4.0 Released",
      "content": "Spring Boot 4.0 has been released with new features...",
      "summary": "Spring Boot 4.0 brings significant improvements...",
      "publishedAt": "2026-01-05T08:00:00Z",
      "url": "https://example.com/news/spring-boot-4",
      "author": "John Doe",
      "metadata": {
        "sourceName": "Hacker News API",
        "tags": ["spring", "java", "framework"],
        "viewCount": 1500,
        "likeCount": 120
      }
    },
    // ... 여러 개의 News 데이터
  ]
}
```

##### 저장 로직 (Facade 계층)

1. **트랜잭션 없음**: Facade 계층에서는 `@Transactional` 사용하지 않음
2. **반복 처리**: 요청된 리스트의 각 항목에 대해 단건 처리 API의 Service 메서드를 반복 호출
3. **부분 롤백 처리**: 
   - 각 항목 처리 시 예외 발생 시 `try-catch`로 예외를 catch
   - 예외 발생 시 로그만 출력하고 다음 항목 계속 처리
   - 실패한 항목은 롤백되지만, 성공한 항목은 정상 커밋됨
4. **응답 반환**: 성공/실패 통계 정보 반환

##### 구현 예시

```java
// @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
public NewsBatchResponse createNewsBatch(NewsBatchRequest request) {
    int successCount = 0;
    int failureCount = 0;
    List<String> failureMessages = new ArrayList<>();
    
    for (NewsCreateRequest item : request.getNewsArticles()) {
        try {
            // 단건 처리 Service 메서드 호출 (각 호출마다 독립적인 트랜잭션)
            newsService.saveNews(item);
            successCount++;
        } catch (Exception e) {
            failureCount++;
            String errorMessage = String.format(
                "News 저장 실패: sourceId=%s, title=%s, error=%s",
                item.getSourceId(), item.getTitle(), e.getMessage()
            );
            log.error(errorMessage, e);
            failureMessages.add(errorMessage);
            // 예외를 catch하고 로그만 출력하여 다음 항목 계속 처리
        }
    }
    
    return NewsBatchResponse.builder()
        .totalCount(request.getNewsArticles().size())
        .successCount(successCount)
        .failureCount(failureCount)
        .failureMessages(failureMessages)
        .build();
}
```

##### 부분 롤백 설계 요구사항

- **단건 처리 API의 Service 레이어**: `@Transactional`로 트랜잭션 생성, 실패 시 롤백
- **다건 처리 API의 Facade 레이어**: 
  - 단건 처리 API의 Service 메서드를 반복 호출
  - 실패 시 예외를 catch하고 로그만 출력
  - 나머지 요소들은 정상 커밋되도록 처리
  - Facade 레이어에는 `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)

##### 응답 형식

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "totalCount": 10,
    "successCount": 8,
    "failureCount": 2,
    "failureMessages": [
      "News 저장 실패: sourceId=xxx, title=yyy, error=중복된 데이터",
      "News 저장 실패: sourceId=zzz, title=aaa, error=유효성 검증 실패"
    ]
  }
}
```

---

## 조회 API 설계

### 목록 조회 API

#### 엔드포인트

- **Contest**: `GET /api/v1/contest`
- **News**: `GET /api/v1/news`

#### 쿼리 파라미터

- `page` (Integer, optional): 페이지 번호 (기본값: 1)
- `size` (Integer, optional): 페이지 크기 (기본값: 10, 최대: 100)
- `sort` (String, optional): 정렬 기준
  - Contest: `startDate,asc`, `startDate,desc`, `endDate,asc`, `endDate,desc` (기본값: "startDate,desc")
  - News: `publishedAt,asc`, `publishedAt,desc` (기본값: "publishedAt,desc")
- `sourceId` (String, optional): 출처 ID (ObjectId)
- `status` (String, optional): 대회 상태 필터 (Contest만, 가능한 값: `UPCOMING`, `ONGOING`, `ENDED`)

#### 페이징

- Spring Data의 `Pageable` 사용
- MongoDB의 `Query`와 `Pageable`을 활용한 페이징 처리

#### 정렬

- `startDate`, `endDate` 기준 정렬 (Contest)
- `publishedAt` 기준 정렬 (News)

#### 필터링

- `sourceId`, `status` 필터 (Contest)
- `sourceId` 필터 (News)

#### 응답 예시

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "pageSize": 10,
    "pageNumber": 1,
    "totalPageNumber": 5,
    "totalSize": 50,
    "list": [
      {
        "_id": "507f1f77bcf86cd799439012",
        "sourceId": "507f1f77bcf86cd799439011",
        "title": "Codeforces Round 900",
        "startDate": "2026-01-15T10:00:00Z",
        "endDate": "2026-01-15T12:30:00Z",
        "status": "UPCOMING",
        "description": "Regular Codeforces contest",
        "url": "https://codeforces.com/contests/1900",
        "metadata": {
          "sourceName": "Codeforces API",
          "prize": null,
          "participants": null,
          "tags": ["algorithm", "competitive-programming"]
        }
      }
    ]
  }
}
```

### 상세 조회 API

#### 엔드포인트

- **Contest**: `GET /api/v1/contest/{id}`
- **News**: `GET /api/v1/news/{id}`

#### 경로 파라미터

- `id` (String, required): Contest/News ID (ObjectId)

#### 응답

- ContestDocument / NewsArticleDocument 전체 정보

#### 응답 예시

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "_id": "507f1f77bcf86cd799439012",
    "sourceId": "507f1f77bcf86cd799439011",
    "title": "Codeforces Round 900",
    "startDate": "2026-01-15T10:00:00Z",
    "endDate": "2026-01-15T12:30:00Z",
    "status": "UPCOMING",
    "description": "Regular Codeforces contest",
    "url": "https://codeforces.com/contests/1900",
    "metadata": {
      "sourceName": "Codeforces API",
      "prize": null,
      "participants": null,
      "tags": ["algorithm", "competitive-programming"]
    }
  }
}
```

### 검색 API

#### 엔드포인트

- **Contest**: `GET /api/v1/contest/search`
- **News**: `GET /api/v1/news/search`

#### 쿼리 파라미터

- `q` (String, required): 검색어
- `page` (Integer, optional): 페이지 번호 (기본값: 1)
- `size` (Integer, optional): 페이지 크기 (기본값: 10, 최대: 100)

#### 검색 방식

- MongoDB Full-text Search 또는 `$text` 쿼리
- Spring Data MongoDB의 `TextCriteria` 및 `TextQuery` 활용

#### 응답 형식

- 목록 조회와 동일한 페이징 응답 형식

---

## 에러 처리 설계

### 예외 처리 전략

#### 커스텀 예외

- **ContestNotFoundException**: 대회를 찾을 수 없을 때
- **ContestValidationException**: 유효성 검증 실패 시
- **ContestDuplicateException**: 중복 데이터 시
- **NewsNotFoundException**: 뉴스를 찾을 수 없을 때
- **NewsValidationException**: 유효성 검증 실패 시
- **NewsDuplicateException**: 중복 데이터 시

#### 글로벌 예외 핸들러

- `@ControllerAdvice` 사용
- `common-core` 모듈의 에러 코드 체계 준수
- 공통 응답 형식 (`ApiResponse<T>`) 사용

#### 구현 예시

```java
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class ContestExceptionHandler {
    
    @ExceptionHandler(ContestNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestNotFoundException(
            ContestNotFoundException e) {
        log.warn("Contest not found: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("4004", "NOT_FOUND", "대회를 찾을 수 없습니다."));
    }
    
    @ExceptionHandler(ContestValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestValidationException(
            ContestValidationException e) {
        log.warn("Contest validation failed: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("4006", "VALIDATION_ERROR", "유효성 검증에 실패했습니다."));
    }
    
    @ExceptionHandler(ContestDuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleContestDuplicateException(
            ContestDuplicateException e) {
        log.warn("Contest duplicate: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("4005", "CONFLICT", "이미 존재하는 대회입니다."));
    }
}
```

### 에러 코드 체계

- **4004**: 리소스 없음 (NOT_FOUND)
- **4005**: 충돌 (CONFLICT)
- **4006**: 유효성 검증 실패 (VALIDATION_ERROR)
- **5000**: 내부 서버 오류 (INTERNAL_SERVER_ERROR)

자세한 에러 코드는 `docs/step2/4. error-handling-strategy-design.md`를 참고하세요.

---

## 테스트 전략

### 단위 테스트

#### Service 계층 단위 테스트

- MongoDB Repository Mock 사용
- 데이터 검증 로직 테스트
- 페이징 처리 테스트

#### Repository 계층 단위 테스트

- MongoDB Embedded 또는 Testcontainers 사용
- 커스텀 쿼리 메서드 테스트

#### DTO 변환 로직 테스트

- Request/Response DTO 변환 테스트
- Document ↔ DTO 변환 테스트

### 통합 테스트

#### Controller → Facade → Service → Repository 통합 테스트

- `@SpringBootTest` 사용
- MongoDB 실제 연결 테스트 (Testcontainers 권장)
- 전체 흐름 테스트

#### 내부 API 테스트

- 단건 처리 API 테스트
- 다건 처리 API 테스트 (부분 롤백 검증)

---

## 결론

이 설계서는 `api-contest`와 `api-news` 모듈의 API 구현을 위한 종합 설계 문서입니다. `api-auth`, `api-gateway` 모듈 구조를 참고하여 일관성 있는 패키지 구조를 설계하고, CQRS 패턴을 준수하여 MongoDB Atlas를 사용한 읽기 전용 API를 제공합니다.

### 주요 특징

1. ✅ **프로젝트 구조 일관성**: `api-auth`, `api-gateway` 모듈 구조와 일관성 유지
2. ✅ **Facade 패턴**: Controller → Facade → Service → Repository 계층 구조
3. ✅ **CQRS 패턴**: 읽기 전용 API (MongoDB Atlas만 사용)
4. ✅ **트랜잭션 관리**: 단건 처리와 다건 처리의 트랜잭션 전략 분리
5. ✅ **부분 롤백**: 다건 처리 API의 부분 롤백 구현
6. ✅ **클린코드 원칙**: SOLID 원칙 준수, 단일 책임 원칙, 의존성 역전 원칙

### 다음 단계

1. API 모듈 패키지 구조 생성
2. Controller, Facade, Service, Repository 계층 구현
3. DTO 클래스 생성 (Request/Response)
4. MongoDB 저장 API 구현 (단건/다건 처리)
5. 조회 API 구현 (목록, 상세, 검색)
6. 에러 처리 구현 (커스텀 예외, 글로벌 예외 핸들러)
7. 테스트 코드 작성 (단위 테스트, 통합 테스트)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-07  
**작성자**: API Architect
