# Contest 및 News API 설계 프롬프트  

**작성 일시**: 2026-01-XX  
**대상**: `api-contest`, `api-news` 모듈 설계 문서 작성  
**목적**: Contest 및 News API 구현을 위한 종합 설계 문서 작성  

## 프롬프트 목적

이 프롬프트는 다음 설계 문서 작성을 위한 가이드를 제공합니다:

1. **api-contest, api-news 모듈의 API 구현 설계문서**
2. **client/* 모듈에서 외부로 요청하여 수집, 정제한 contest, news 데이터를 수신하고 MongoDB Atlas에 저장하는 API 설계문서**
3. **batch 모듈에서 Item Reader 으로 client/* 모듈의 수집 데이터를 읽고, Item Writer 에서 api-contest, api-news 모듈로 전달하는 것을 전제로 한 설계문서**
4. **api-auth, api-gateway 모듈 구조를 참고한 일관성 있는 구조 설계문서**

## 핵심 요구사항

### 1. 프로젝트 구조 일관성
- `api-auth`, `api-gateway` 모듈의 구조를 참고하여 일관성 있는 패키지 구조 설계
- Facade 패턴, Service 계층, Repository 계층 구조 준수
- Controller → Facade → Service → Repository 계층 구조 유지

### 2. 데이터 흐름 설계
- **Client 모듈** (`client-feign`, `client-rss`, `client-scraper`)에서 정제한 데이터
- **Batch 모듈**에서 Client 모듈 데이터 수집 및 API 호출
- **API 모듈** (`api-contest`, `api-news`)에서 MongoDB Atlas 저장
- **MongoDB Atlas**에 ContestDocument, NewsArticleDocument 저장

### 3. 설계 문서 참고
- `docs/step1/`: MongoDB/Aurora 스키마 설계 문서
- `docs/step2/`: API 엔드포인트 설계, 데이터 모델 설계, 응답 형식 설계
- `docs/step8/`: RSS/Scraper 모듈 분석
- `docs/step6/`: OAuth 구현 가이드
- 기존 설계 문서의 패턴과 원칙 준수

### 4. 클린코드 및 객체지향 설계
- SOLID 원칙 준수
- 단일 책임 원칙 (SRP)
- 의존성 역전 원칙 (DIP)
- 인터페이스 기반 설계
- 명확한 책임 분리

### 5. 공식 문서 참고
- Spring Boot 공식 문서만 참고
- MongoDB Atlas 공식 문서만 참고
- Spring Data MongoDB 공식 문서만 참고
- 신뢰할 수 없는 블로그나 커뮤니티 자료는 참고하지 않음

## 필수 참고 문서

### 프로젝트 내 설계 문서
1. **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
   - Contest API 엔드포인트: `GET /api/v1/contest`, `GET /api/v1/contest/{id}`, `GET /api/v1/contest/search`
   - News API 엔드포인트: `GET /api/v1/news`, `GET /api/v1/news/{id}`, `GET /api/v1/news/search`
   - 응답 형식 및 페이징 처리

2. **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
   - ContestDocument 구조
   - NewsArticleDocument 구조
   - MongoDB Atlas 인덱스 전략

3. **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
   - ContestDocument 필드 구조
   - NewsArticleDocument 필드 구조
   - 인덱스 설계 (ESR 규칙)

4. **RSS/Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
   - client-rss 모듈의 데이터 수집 방식
   - client-scraper 모듈의 데이터 수집 방식
   - 데이터 정제 및 변환 로직

### 기존 모듈 구조 참고
1. **api-auth 모듈 구조**:
   - `controller/AuthController.java`
   - `facade/AuthFacade.java`
   - `service/AuthService.java`
   - `dto/` 패키지 구조

2. **api-gateway 모듈 구조**:
   - `domain/sample/controller/SampleCategory1Controller.java`
   - `domain/sample/facade/SampleFacade.java`
   - `domain/sample/service/SampleCategory1Service.java`
   - `domain/sample/repository/` 패키지 구조 (Reader/Writer 분리)

3. **domain-mongodb 모듈**:
   - `document/ContestDocument.java`
   - `document/NewsArticleDocument.java`
   - `repository/ContestRepository.java`
   - `repository/NewsArticleRepository.java`

## 설계 문서 작성 가이드

### 1. API 모듈 구조 설계

#### 패키지 구조
```
api-contest/
  src/main/java/com/ebson/shrimp/tm/demo/api/contest/
    ContestApplication.java
    controller/
      ContestController.java
    facade/
      ContestFacade.java
    service/
      ContestService.java
      ContestServiceImpl.java
    dto/
      ContestListRequest.java
      ContestListResponse.java
      ContestDetailResponse.java
      ContestSearchRequest.java
      ContestSearchResponse.java
    config/
      ContestConfig.java
    common/
      exception/
        ContestExceptionHandler.java
```

#### 계층 구조
1. **Controller 계층**: HTTP 요청/응답 처리
   - `@RestController`, `@RequestMapping("/api/v1/contest")`
   - Facade 호출
   - DTO 변환

2. **Facade 계층**: Controller와 Service 사이의 중간 계층
   - 비즈니스 로직 조합
   - 다건 처리 API의 부분 롤백 구현
     - 단건 처리 Service 메서드를 반복 호출
     - 예외 발생 시 catch하여 로그만 출력하고 다음 항목 계속 처리
     - `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)

3. **Service 계층**: 핵심 비즈니스 로직
   - MongoDB Repository 호출
   - 데이터 검증 및 변환
   - 페이징 처리
   - 단건 처리 API의 트랜잭션 관리
     - `@Transactional` 어노테이션으로 트랜잭션 생성
     - 실패 시 자동 롤백

4. **Repository 계층**: MongoDB 데이터 접근
   - `domain-mongodb` 모듈의 Repository 사용
   - 커스텀 쿼리 메서드 (필요 시)

### 2. 데이터 수집 및 저장 흐름 설계

#### 데이터 흐름
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

#### Batch 모듈 설계
- **역할**: Client 모듈에서 수집한 데이터를 API 모듈로 전달
- **구현 방식**: Spring Batch의 Item Reader/Writer 패턴 사용
  - **Item Reader**: `client-feign`, `client-rss`, `client-scraper` 모듈에서 수집한 데이터를 읽음
  - **Item Processor**: Client 모듈 DTO → API 모듈 DTO 변환
  - **Item Writer**: `api-contest`, `api-news` 모듈의 내부 API를 호출하여 MongoDB Atlas에 저장
- **데이터 변환**: Client 모듈 DTO → API 모듈 DTO 변환 (Item Processor에서 처리)
- **에러 처리**: 재시도 로직, Dead Letter Queue 처리

#### API 모듈 저장 로직
- **단건 처리 엔드포인트**: `POST /api/v1/contest/internal` (내부 API, Batch 모듈 전용)
- **다건 처리 엔드포인트**: `POST /api/v1/contest/internal/batch` (내부 API, Batch 모듈 전용)
- **인증**: 내부 API 키 또는 서비스 간 인증
- **데이터 검증**: DTO 검증, 중복 체크
- **MongoDB 저장**: ContestDocument / NewsArticleDocument 저장
- **트랜잭션 관리**: 
  - 단건 처리: Service 레이어에서 `@Transactional` 사용
  - 다건 처리: Facade 레이어에서 단건 처리 Service 메서드를 반복 호출, 부분 롤백 구현

### 3. MongoDB 저장 API 설계

#### 단건 처리 API

##### 저장 API 엔드포인트
```java
// api-contest 모듈
POST /api/v1/contest/internal
Content-Type: application/json
Authorization: Bearer {internal-api-key}

Request Body:
{
  "sourceId": "ObjectId",
  "title": "String",
  "startDate": "ISO 8601",
  "endDate": "ISO 8601",
  "status": "UPCOMING|ONGOING|ENDED",
  "description": "String",
  "url": "String",
  "metadata": {
    "sourceName": "String",
    "prize": "String",
    "participants": Integer,
    "tags": ["String"]
  }
}
```

##### 저장 로직 (Service 계층)
1. **트랜잭션 관리**: `@Transactional` 어노테이션으로 트랜잭션 생성
2. **데이터 검증**: DTO 검증 (`@Valid`)
3. **중복 체크**: `sourceId + url` 또는 `sourceId + title + startDate` 기준
4. **Document 생성**: ContestDocument / NewsArticleDocument 생성
5. **MongoDB 저장**: Repository.save()
6. **트랜잭션 커밋**: 성공 시 자동 커밋, 실패 시 자동 롤백
7. **응답 반환**: 저장된 Document ID 반환

##### 구현 예시
```java
@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {
    
    private final ContestRepository contestRepository;
    
    @Transactional
    @Override
    public ContestDocument saveContest(ContestCreateRequest request) {
        // 중복 체크
        if (contestRepository.existsBySourceIdAndUrl(request.getSourceId(), request.getUrl())) {
            throw new ContestDuplicateException("이미 존재하는 대회입니다.");
        }
        
        // Document 생성 및 저장
        ContestDocument document = ContestDocument.builder()
            .sourceId(request.getSourceId())
            .title(request.getTitle())
            // ... 필드 설정
            .build();
            
        return contestRepository.save(document);
    }
}
```

#### 다건 처리 API

##### 저장 API 엔드포인트
```java
// api-contest 모듈
POST /api/v1/contest/internal/batch
Content-Type: application/json
Authorization: Bearer {internal-api-key}

Request Body:
{
  "contests": [
    {
      "sourceId": "ObjectId",
      "title": "String",
      // ... 단건과 동일한 구조
    },
    // ... 여러 개의 Contest 데이터
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
@Service
@RequiredArgsConstructor
public class ContestFacade {
    
    private final ContestService contestService;
    private static final Logger log = LoggerFactory.getLogger(ContestFacade.class);
    
    // @Transactional 없음 - 각 단건 처리가 독립적인 트랜잭션
    public ContestBatchResponse saveContestsBatch(ContestBatchRequest request) {
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
      "Contest 저장 실패: sourceId=xxx, title=yyy, error=중복된 데이터",
      "Contest 저장 실패: sourceId=zzz, title=aaa, error=유효성 검증 실패"
    ]
  }
}
```

### 4. 조회 API 설계

#### 목록 조회 API
- **엔드포인트**: `GET /api/v1/contest`
- **쿼리 파라미터**: `page`, `size`, `sort`, `sourceId`, `status`
- **페이징**: Spring Data의 `Pageable` 사용
- **정렬**: `startDate`, `endDate` 기준 정렬
- **필터링**: `sourceId`, `status` 필터

#### 상세 조회 API
- **엔드포인트**: `GET /api/v1/contest/{id}`
- **경로 파라미터**: `id` (ObjectId)
- **응답**: ContestDocument 전체 정보

#### 검색 API
- **엔드포인트**: `GET /api/v1/contest/search`
- **쿼리 파라미터**: `q` (검색어), `page`, `size`
- **검색 방식**: MongoDB Full-text Search 또는 `$text` 쿼리

### 5. 에러 처리 설계

#### 예외 처리 전략
- **커스텀 예외**: `ContestNotFoundException`, `ContestValidationException`
- **글로벌 예외 핸들러**: `@ControllerAdvice` 사용
- **응답 형식**: `ApiResponse<T>` 공통 응답 형식 사용
- **에러 코드**: `common-core` 모듈의 에러 코드 체계 준수

### 6. 테스트 설계

#### 단위 테스트
- Service 계층 단위 테스트
- Repository 계층 단위 테스트 (MongoDB Embedded 또는 Testcontainers)
- DTO 변환 로직 테스트

#### 통합 테스트
- Controller → Facade → Service → Repository 통합 테스트
- MongoDB 실제 연결 테스트 (Testcontainers 권장)

## 설계 문서 작성 체크리스트

### 필수 포함 사항
- [ ] API 모듈 패키지 구조 설계
- [ ] Controller, Facade, Service, Repository 계층 구조
- [ ] DTO 설계 (Request/Response)
- [ ] MongoDB 저장 API 엔드포인트 설계
  - [ ] 단건 처리 API 설계 (POST /api/v1/contest/internal)
  - [ ] 다건 처리 API 설계 (POST /api/v1/contest/internal/batch)
  - [ ] 부분 롤백 설계 (Facade 레이어에서 예외 처리)
- [ ] 조회 API 엔드포인트 설계 (목록, 상세, 검색)
- [ ] Batch 모듈과의 연동 설계
- [ ] 트랜잭션 관리 전략
  - [ ] 단건 처리: Service 레이어에서 @Transactional 사용
  - [ ] 다건 처리: Facade 레이어에서 @Transactional 미사용, 각 단건 처리가 독립적인 트랜잭션
- [ ] 에러 처리 전략
- [ ] 인덱스 활용 전략
- [ ] 페이징 및 정렬 전략
- [ ] 데이터 검증 전략
- [ ] 중복 체크 전략

### 참고 사항
- [ ] api-auth, api-gateway 모듈 구조와의 일관성 확인
- [ ] docs/step2/ API 엔드포인트 설계 문서 준수
- [ ] docs/step1/ MongoDB 스키마 설계 문서 준수
- [ ] 클린코드 원칙 준수 (SOLID)
- [ ] 객체지향 설계 원칙 준수

## 주의사항

### 오버엔지니어링 방지
- **요청하지 않은 기능 추가 금지**: 요구사항에 명시되지 않은 기능은 구현하지 않음
- **과도한 추상화 금지**: 필요한 수준의 추상화만 사용
- **불필요한 레이어 추가 금지**: 명확한 목적이 없는 중간 레이어 추가 금지

### 공식 문서만 참고
- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **MongoDB Atlas 공식 문서**: https://www.mongodb.com/docs/atlas/
- **Spring Data MongoDB 공식 문서**: https://spring.io/projects/spring-data-mongodb
- **신뢰할 수 없는 자료 참고 금지**: 블로그, 커뮤니티 자료는 참고하지 않음

### 구현 범위 제한
- **읽기 전용 API**: Contest, News는 읽기 전용 (MongoDB Atlas만 사용)
- **쓰기 API는 내부 전용**: Batch 모듈에서만 호출하는 내부 API
- **CQRS 패턴 준수**: Command Side (Aurora)는 사용하지 않음

### 트랜잭션 관리 주의사항
- **단건 처리 API**: Service 레이어에서 `@Transactional` 사용 필수
  - 실패 시 해당 항목만 롤백
  - MongoDB는 트랜잭션을 지원하므로 정상 동작
- **다건 처리 API**: Facade 레이어에서 `@Transactional` 사용 금지
  - Facade 레이어에 `@Transactional`을 사용하면 전체가 하나의 트랜잭션으로 묶여 부분 롤백 불가
  - 각 단건 처리가 독립적인 트랜잭션이 되도록 설계
  - 실패한 항목은 롤백, 성공한 항목은 정상 커밋
  - 예외 발생 시 catch하여 로그만 출력하고 다음 항목 계속 처리

## 출력 결과물

설계 문서 작성 후 다음 결과물을 제공해야 합니다:

1. **설계 문서**: `docs/step9/contest-news-api-design.md`
   - API 모듈 구조 설계
   - 데이터 흐름 설계
   - 엔드포인트 상세 설계
   - 에러 처리 설계
   - 테스트 전략

2. **검증 기준**:
   - api-auth, api-gateway 모듈과의 구조 일관성
   - docs/step2/ API 엔드포인트 설계 문서 준수
   - 클린코드 원칙 준수
   - 객체지향 설계 원칙 준수

## 실행 명령어

api-contest 및 api-news 모듈의 API 구현을 위한 종합 설계 문서 작성을 시작하세요.

참고 파일:
- docs/step2/1. api-endpoint-design.md (API 엔드포인트 설계)
- docs/step2/2. data-model-design.md (데이터 모델 설계)
- docs/step1/2. mongodb-schema-design.md (MongoDB 스키마 설계)
- docs/step8/rss-scraper-modules-analysis.md (RSS/Scraper 모듈 분석)
- api/auth 모듈 구조 (일관성 참고)
- api/gateway 모듈 구조 (일관성 참고)
- domain/mongodb 모듈 구조 (Repository 참고)

작업 내용:
1. api-contest, api-news 모듈의 패키지 구조 설계
   - Controller, Facade, Service, Repository 계층 구조
   - api-auth, api-gateway 모듈 구조와의 일관성 유지
   
2. 데이터 수집 및 저장 흐름 설계
   - client-rss, client-scraper, client-feign 모듈에서 외부로 요청하여 데이터 수집 및 정제
   - batch 모듈의 Item Reader에서 client/* 모듈의 수집 데이터를 읽음
   - batch 모듈의 Item Writer에서 api-contest, api-news 모듈로 데이터 전달
   - api-contest, api-news 모듈에서 MongoDB Atlas 저장
   
3. MongoDB 저장 API 설계 (내부 API)
   - 단건 처리 API: POST /api/v1/contest/internal (Batch 모듈 전용)
     - Service 레이어에서 @Transactional 사용
     - 실패 시 트랜잭션 롤백
   - 다건 처리 API: POST /api/v1/contest/internal/batch (Batch 모듈 전용)
     - Facade 레이어에서 단건 처리 Service 메서드를 반복 호출
     - 부분 롤백 구현: 실패한 항목은 롤백, 성공한 항목은 정상 커밋
     - Facade 레이어에는 @Transactional 사용하지 않음
   - 데이터 검증 및 중복 체크 로직
   - 단건/다건 처리 모두 동일하게 적용
   
4. 조회 API 설계 (공개 API)
   - GET /api/v1/contest (목록 조회)
   - GET /api/v1/contest/{id} (상세 조회)
   - GET /api/v1/contest/search (검색)
   - GET /api/v1/news (목록 조회)
   - GET /api/v1/news/{id} (상세 조회)
   - GET /api/v1/news/search (검색)
   
5. 에러 처리 및 예외 처리 전략 설계
   - 커스텀 예외 클래스 설계
   - 글로벌 예외 핸들러 설계
   - 공통 응답 형식 사용
   
6. 테스트 전략 설계
   - 단위 테스트 전략
   - 통합 테스트 전략
   
7. 클린코드 및 객체지향 설계 원칙 준수
   - SOLID 원칙 준수
   - 단일 책임 원칙 (SRP)
   - 의존성 역전 원칙 (DIP)
   - 인터페이스 기반 설계

검증 기준:
- api-auth, api-gateway 모듈과의 구조 일관성
- docs/step2/ API 엔드포인트 설계 문서 준수
- docs/step1/ MongoDB 스키마 설계 문서 준수
- 클린코드 원칙 준수 (SOLID)
- 객체지향 설계 원칙 준수
- 오버엔지니어링 방지 (요청하지 않은 기능 추가 금지)
- 공식 문서만 참고 (Spring Boot, MongoDB Atlas, Spring Data MongoDB)


---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-XX  
**작성자**: System Architect 
