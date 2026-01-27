# Sources 동기화 Batch Job 설계서 작성 프롬프트

## 역할 (Role)

당신은 Spring Batch와 MongoDB Atlas를 전문으로 하는 시니어 백엔드 엔지니어입니다. 기존 프로젝트 코드베이스의 패턴과 컨벤션을 준수하면서 안정적이고 유지보수 가능한 배치 작업을 설계합니다.

---

## 목표 (Objective)

`json/sources.json` 파일에 정의된 모든 Source 데이터를 MongoDB Atlas Cluster의 `sources` 컬렉션으로 동기화하는 Spring Batch Job의 **기술 설계서**를 작성하세요.

---

## 컨텍스트 (Context)

### 1. 프로젝트 구조
- **Batch 모듈**: `batch/source` - Spring Batch 기반 배치 작업 모듈
- **Domain 모듈**: `domain/mongodb` - MongoDB Document 및 Repository 정의
- **데이터 소스**: `json/sources.json` - 동기화 대상 원본 데이터

### 2. 기존 MongoDB Document 스키마 (참조 필수)
```java
// domain/mongodb/src/main/java/.../document/SourcesDocument.java
@Document(collection = "sources")
public class SourcesDocument {
    @Id
    private ObjectId id;
    
    @Field("name") @Indexed(unique = true)
    private String name;
    
    @Field("type")
    private String type;
    
    @Field("category")
    private String category;
    
    @Field("url")
    private String url;
    
    @Field("api_endpoint")
    private String apiEndpoint;
    
    @Field("rss_feed_url")
    private String rssFeedUrl;
    
    @Field("description")
    private String description;
    
    @Field("priority")
    private Integer priority;
    
    @Field("reliability_score")
    private Integer reliabilityScore;
    
    @Field("accessibility_score")
    private Integer accessibilityScore;
    
    @Field("data_quality_score")
    private Integer dataQualityScore;
    
    @Field("legal_ethical_score")
    private Integer legalEthicalScore;
    
    @Field("total_score")
    private Integer totalScore;
    
    @Field("authentication_required")
    private Boolean authenticationRequired;
    
    @Field("authentication_method")
    private String authenticationMethod;
    
    @Field("rate_limit")
    private String rateLimit;
    
    @Field("documentation_url")
    private String documentationUrl;
    
    @Field("update_frequency")
    private String updateFrequency;
    
    @Field("data_format")
    private String dataFormat;
    
    @Field("enabled")
    private Boolean enabled;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("created_by")
    private String createdBy;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("updated_by")
    private String updatedBy;
}
```

### 3. 기존 Repository (참조 필수)
```java
// domain/mongodb/src/main/java/.../repository/SourcesRepository.java
@Repository
public interface SourcesRepository extends MongoRepository<SourcesDocument, ObjectId> {
}
```

### 4. sources.json 데이터 구조 (입력 데이터)
```json
{
  "categories": [
    {
      "category": "개발자 대회 정보",
      "sources": [
        {
          "name": "Codeforces API",
          "type": "API",
          "url": "https://codeforces.com",
          "api_endpoint": "https://codeforces.com/api",
          "rss_feed_url": null,
          "description": "알고리즘 대회 정보를 제공하는 공식 API...",
          "reliability_score": 10,
          "accessibility_score": 10,
          "data_quality_score": 10,
          "legal_ethical_score": 9,
          "total_score": 39,
          "priority": 1,
          "authentication_required": false,
          "authentication_method": "None",
          "rate_limit": "No strict rate limit...",
          "documentation_url": "https://codeforces.com/apiHelp",
          "update_frequency": "실시간",
          "data_format": "JSON",
          // ... 기타 필드
        }
      ]
    },
    {
      "category": "최신 IT 테크 뉴스 정보",
      "sources": [...]
    }
  ]
}
```

### 5. 기존 Batch Job 패턴 (참조 필수)
```java
// batch/source/.../jobconfig/NewsGoogleDevelopersRssParserJobConfig.java 패턴 참조

@Configuration
@RequiredArgsConstructor
public class [JobName]JobConfig {
    
    @Bean(name = Constants.[JOB_NAME])
    public Job [jobName]Job(JobRepository jobRepository, Step step1) {
        return new JobBuilder(Constants.[JOB_NAME], jobRepository)
            .start(step1)
            .incrementer(new [JobName]Incrementer(baseDate))
            .build();
    }
    
    @Bean(name = Constants.[JOB_NAME] + Constants.STEP_1)
    @JobScope
    public Step step1(...) {
        return new StepBuilder(...)
            .<InputType, OutputType>chunk(Constants.CHUNK_SIZE_10, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
    }
    
    // Reader, Processor, Writer Bean 정의
}
```

### 6. 관련 문서 (참조 필수)
- MongoDB 스키마 설계: `docs/step1/2. mongodb-schema-design.md`

---

## 설계 요구사항 (Requirements)

### 필수 요구사항

1. **직접 MongoDB 연동**
   - 내부 API를 호출하지 않고 `domain/mongodb` 모듈의 Repository를 직접 사용
   - `SourcesRepository`를 통한 CRUD 작업 수행

2. **데이터 동기화 전략**
   - UPSERT 방식: `name` 필드 기준으로 존재하면 UPDATE, 없으면 INSERT
   - 중복 데이터 방지 (`name` 필드 UNIQUE 인덱스 활용)

3. **JSON 파일 읽기**
   - `json/sources.json` 파일을 읽어 Source 목록 추출
   - 카테고리별로 중첩된 sources 배열 평탄화(flatten) 처리
   - 각 source 객체에 category 필드 매핑

4. **배치 컴포넌트 구조**
   - Job: `SourcesSyncJob`
   - Step: `SourcesSyncStep1` (Read → Process → Write)
   - Reader: JSON 파일에서 Source 데이터 읽기
   - Processor: DTO → Document 변환, `enabled` 필드 기본값 true 설정
   - Writer: MongoDB에 UPSERT

5. **기존 패턴 준수**
   - `batch/source` 모듈의 기존 Job 구성 패턴 준수
   - `Constants` 클래스에 Job 이름 상수 추가
   - `@JobScope`, `@StepScope` 어노테이션 활용

### 선택 요구사항 (구현 시 고려)

1. **에러 핸들링**
   - 개별 항목 실패 시 Skip 정책 적용
   - 실패 항목 로깅

2. **감사 필드 자동 설정**
   - `createdAt`, `updatedAt`: 현재 시간
   - `createdBy`, `updatedBy`: "batch-system" 고정값

---

## 제약사항 (Constraints)

1. **오버엔지니어링 금지**
   - 요구사항에 명시되지 않은 추가 기능 구현 금지
   - 복잡한 재시도 로직, 분산 처리 등 불필요한 고급 기능 배제

2. **불필요한 추가 작업 금지**
   - 새로운 인덱스 추가 불필요 (기존 `name` UNIQUE 인덱스 활용)
   - 새로운 테스트 코드 작성 범위 명시 필요 시에만 포함
   - 문서화는 설계서 범위 내로 제한

3. **기존 코드 수정 최소화**
   - 기존 `SourcesDocument`, `SourcesRepository` 수정 불필요
   - 기존 `MongoClientConfig` 설정 그대로 활용

4. **공식 출처만 참조**
   - Spring Batch 공식 문서: https://docs.spring.io/spring-batch/reference/
   - Spring Data MongoDB 공식 문서: https://docs.spring.io/spring-data/mongodb/reference/
   - MongoDB Java Driver 공식 문서: https://www.mongodb.com/docs/drivers/java/sync/current/

---

## 출력 형식 (Output Format)

설계서는 다음 구조로 작성하세요:

```markdown
# Sources 동기화 Batch Job 기술 설계서

## 1. 개요
- 목적
- 범위
- 관련 문서

## 2. 시스템 아키텍처
- 컴포넌트 다이어그램 (Mermaid)
- 데이터 흐름

## 3. 배치 Job 설계
### 3.1 Job 구성
- Job 이름, Step 구성

### 3.2 Reader 설계
- 입력 데이터 형식
- 읽기 전략
- DTO 클래스 정의

### 3.3 Processor 설계
- 변환 로직
- 기본값 설정

### 3.4 Writer 설계
- UPSERT 전략
- MongoDB 연동 방식

## 4. 클래스 설계
### 4.1 패키지 구조
### 4.2 클래스 다이어그램 (Mermaid)
### 4.3 주요 클래스 명세
- 클래스명, 역할, 메서드 시그니처

## 5. 설정 및 실행
### 5.1 application.yml 설정
### 5.2 Job 실행 방법

## 6. 에러 핸들링
- Skip 정책
- 로깅 전략

## 7. 검증 기준
- 성공 기준
- 테스트 케이스 목록
```

---

## 예시 (Few-shot Examples)

### Reader 구현 예시
```java
// 기존 패턴 참조: batch/source/.../reader/GoogleDevelopersRssItemReader.java
public class SourcesJsonItemReader implements ItemStreamReader<SourceDto> {
    
    private List<SourceDto> sources;
    private int currentIndex = 0;
    
    @Override
    public void open(ExecutionContext executionContext) {
        // JSON 파일 읽기 및 파싱
        // categories 배열 순회하여 sources 추출 및 평탄화
    }
    
    @Override
    public SourceDto read() {
        if (currentIndex < sources.size()) {
            return sources.get(currentIndex++);
        }
        return null; // 종료
    }
}
```

### Writer 구현 예시 (직접 Repository 사용)
```java
// 내부 API 호출 대신 Repository 직접 사용
public class SourcesMongoWriter implements ItemWriter<SourcesDocument> {
    
    private final SourcesRepository sourcesRepository;
    // 또는 MongoTemplate 사용 가능
    
    @Override
    public void write(Chunk<? extends SourcesDocument> chunk) {
        for (SourcesDocument doc : chunk) {
            // UPSERT 로직: name 기준 존재 여부 확인 후 저장
            // sourcesRepository.findByName() 또는 MongoTemplate.upsert() 활용
        }
    }
}
```

---

## 주의사항

1. **설계서만 작성**: 실제 코드 구현은 범위 외입니다. 구현 가이드와 의사 코드(pseudocode)만 제공하세요.

2. **기존 패턴 엄격 준수**: 새로운 패턴이나 라이브러리 도입을 지양하고, 기존 `batch/source` 모듈의 구조를 따르세요.

3. **명확한 인터페이스 정의**: 클래스 간 상호작용을 위한 메서드 시그니처, 파라미터, 반환값을 명시하세요.

4. **간결함 유지**: 불필요한 설명이나 중복 내용을 피하고, 핵심 설계 결정사항에 집중하세요.
