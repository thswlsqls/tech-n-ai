# Batch Job 통합 설계 프롬프트

**작성 일시**: 2026-01-XX  
**대상**: `batch-source` 모듈 및 `client-feign` 모듈 설계 문서 작성  
**목적**: 모든 클라이언트 모듈의 데이터 수집을 위한 배치 잡 통합 설계 문서 작성

## 프롬프트 목적

이 프롬프트는 다음 설계 문서 작성을 위한 가이드를 제공합니다:

1. **client-feign 모듈의 내부 API 호출 Feign Client 설계문서**
   - `api-contest` 모듈의 `createContestInternal`, `createContestBatchInternal` API 호출
   - `api-news` 모듈의 `createNewsInternal`, `createNewsBatchInternal` API 호출
2. **batch-source 모듈의 배치 잡 통합 설계문서**
   - `ContestCodeforcesJobConfig` 패턴을 참고한 일관된 배치 잡 구조
   - 모든 클라이언트 모듈(`client-feign`, `client-rss`, `client-scraper`)에 대한 JobConfig 추가
   - 각 클라이언트의 데이터 수집 전용 `*PagingItemReader` 구현
3. **데이터 수집 및 저장 파이프라인 설계문서**
   - Client 모듈 → Batch 모듈 → API 모듈 → MongoDB Atlas 데이터 흐름

## 핵심 요구사항

### 1. 프로젝트 구조 일관성
- `ContestCodeforcesApiJobConfig` 패턴을 엄격히 준수하여 일관성 있는 배치 잡 구조 설계
- **JobConfig 클래스 이름 규칙**:
  - **client-feign 클라이언트**: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`)
  - **client-rss 클라이언트**: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
  - **client-scraper 클라이언트**: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
- 기존 배치 잡 구조와의 일관성 유지
- 패키지 구조: `domain/{contest|news}/{source-name}/` 형식 유지

### 2. Feign Client 내부 API 호출 설계
- `client-feign` 모듈에 내부 API 호출을 위한 Feign Client 추가
- `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 호출
- 내부 API 키 인증 처리
- 기존 Feign Client 패턴(`CodeforcesContract` 등)과 일관성 유지

### 3. 배치 잡 통합 설계
- 모든 클라이언트 모듈의 외부 요청에 대해 배치 잡 추가
- `ContestCodeforcesApiJobConfig` 패턴을 참고하여 일관된 구조 유지
- **JobConfig 클래스 이름 규칙 준수**:
  - **client-feign**: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`)
  - **client-rss**: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
  - **client-scraper**: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
- 각 클라이언트별 `*PagingItemReader` 구현
- Item Writer에서 내부 API 호출

### 3-1. DTO 독립성 원칙 (중요)
- **각 모듈은 독립적으로 DTO를 정의해야 함**: 필드가 동일하더라도 각 모듈에서 별도로 정의
- **batch-source 모듈의 DTO**: `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/{contest|news}/dto/`
  - `ContestCreateRequest`, `ContestBatchRequest` 등
- **api-contest 모듈의 DTO**: `api/contest/src/main/java/com/ebson/shrimp/tm/demo/api/contest/dto/`
  - `ContestCreateRequest`, `ContestBatchRequest` 등 (batch-source와 별도 정의)
- **api-news 모듈의 DTO**: `api/news/src/main/java/com/ebson/shrimp/tm/demo/api/news/dto/`
  - `NewsCreateRequest`, `NewsBatchRequest` 등
- **client-feign 모듈의 내부 API DTO**: `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/internal/contract/`
  - `ContestCreateRequest`, `ContestBatchRequest` 등 (api-contest와 별도 정의)
  - `NewsCreateRequest`, `NewsBatchRequest` 등 (api-news와 별도 정의)
- **Item Processor에서 DTO 변환**: Client DTO → batch-source DTO → API DTO 변환
- **모듈 간 DTO 공유 금지**: 공통 모듈에 DTO를 정의하여 공유하지 않음

### 4. 설계 문서 참고
- `docs/step1/`: MongoDB/Aurora 스키마 설계 문서
- `docs/step2/`: API 엔드포인트 설계, 데이터 모델 설계
- `docs/step8/`: RSS/Scraper 모듈 분석
- `docs/step9/`: Contest/News API 설계
- 기존 설계 문서의 패턴과 원칙 준수

### 5. 클린코드 및 객체지향 설계
- SOLID 원칙 준수
- 단일 책임 원칙 (SRP)
- 의존성 역전 원칙 (DIP)
- 인터페이스 기반 설계
- 명확한 책임 분리

### 6. 공식 문서 참고
- **Spring 프레임워크 공식 문서**:
  - Spring Boot 공식 문서만 참고
  - Spring Batch 공식 문서만 참고
  - Spring Cloud OpenFeign 공식 문서만 참고
- **외부 정보 제공자 공식 문서** (Processor 필드 매핑 시 필수):
  - 각 출처의 공식 API 문서만 참고 (`json/sources.json`의 `documentation_url` 참고)
  - RSS 피드 스펙 문서 (RSS 2.0, Atom 1.0 공식 스펙)
  - 웹 스크래핑 대상 사이트의 공식 문서
  - **신뢰할 수 없는 자료 참고 금지**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음

## 필수 참고 문서

### 프로젝트 내 설계 문서
1. **Contest/News API 설계**: `docs/step9/contest-news-api-design.md`
   - 내부 API 엔드포인트: `POST /api/v1/contest/internal`, `POST /api/v1/contest/internal/batch`
   - 내부 API 엔드포인트: `POST /api/v1/news/internal`, `POST /api/v1/news/internal/batch`
   - 내부 API 키 인증 방식

2. **RSS/Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
   - client-rss 모듈의 데이터 수집 방식
   - client-scraper 모듈의 데이터 수집 방식
   - 데이터 정제 및 변환 로직

3. **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
   - API 엔드포인트 구조 및 응답 형식

4. **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
   - ContestDocument 구조
   - NewsArticleDocument 구조

### 기존 모듈 구조 참고
1. **ContestCodeforcesApiJobConfig 구조** (기존 ContestCodeforcesJobConfig, 이름 변경 필요):
   - `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/jobconfig/ContestCodeforcesJobConfig.java`
   - Job, Step, Reader, Processor, Writer 구조
   - Incrementer, JobParameter 패턴
   - **명명 규칙**: client-feign 클라이언트는 `*ApiJobConfig` 사용

2. **CodeforcesApiPagingItemReader 구조**:
   - `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/reader/CodeforcesApiPagingItemReader.java`
   - `AbstractPagingItemReader` 상속
   - Service를 통한 데이터 수집

3. **CodeforcesStep1Writer 구조**:
   - `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/writer/CodeforcesStep1Writer.java`
   - 내부 API 호출 (현재 미완성, Feign Client 필요)

4. **client-feign 모듈 구조**:
   - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/{source}/contract/`
   - Contract 인터페이스 패턴
   - Feign Client 설정 패턴

## 설계 문서 작성 가이드

### 1. Feign Client 내부 API 호출 설계

#### 패키지 구조
```
client-feign/
  src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/
    internal/
      api/
        ContestInternalApi.java
        NewsInternalApi.java
      client/
        ContestInternalFeignClient.java
        NewsInternalFeignClient.java
      contract/
        ContestInternalContract.java
        NewsInternalContract.java
        InternalApiDto.java
      config/
        InternalApiFeignConfig.java
```

#### Contract 인터페이스 설계
- `ContestInternalContract`: `createContestInternal`, `createContestBatchInternal` 메서드
- `NewsInternalContract`: `createNewsInternal`, `createNewsBatchInternal` 메서드
- 내부 API 키 헤더 처리 (`@RequestHeader("X-Internal-Api-Key")`)
- **Request/Response DTO는 client-feign 모듈에서 독립적으로 정의**
  - `ContestCreateRequest`, `ContestBatchRequest` 등은 api-contest 모듈의 DTO와 필드가 같아도 별도 정의
  - `NewsCreateRequest`, `NewsBatchRequest` 등은 api-news 모듈의 DTO와 필드가 같아도 별도 정의
  - DTO 위치: `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/internal/contract/InternalApiDto.java`

#### Feign Client 설정
- `InternalApiFeignConfig`: 내부 API 호출을 위한 Feign Client 설정
- `application-feign-internal.yml`: 내부 API 엔드포인트 설정
- 타임아웃, 재시도 로직 설정

#### 구현 예시
```java
// client-feign 모듈의 DTO (api-contest 모듈의 DTO와 별도 정의)
package com.ebson.shrimp.tm.demo.client.feign.domain.internal.contract;

public class InternalApiDto {
    // Contest 관련 DTO (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
    @Data
    @Builder
    public static class ContestCreateRequest {
        private String sourceId;
        private String title;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        // ... 기타 필드 (api-contest 모듈의 ContestCreateRequest와 동일한 필드)
    }
    
    @Data
    @Builder
    public static class ContestBatchRequest {
        private List<ContestCreateRequest> contests;
    }
    
    // News 관련 DTO (api-news 모듈의 DTO와 필드가 같아도 별도 정의)
    @Data
    @Builder
    public static class NewsCreateRequest {
        private String sourceId;
        private String title;
        private String content;
        // ... 기타 필드 (api-news 모듈의 NewsCreateRequest와 동일한 필드)
    }
    
    @Data
    @Builder
    public static class NewsBatchRequest {
        private List<NewsCreateRequest> news;
    }
}

// Contract 인터페이스
public interface ContestInternalContract {
    @PostMapping("/api/v1/contest/internal")
    ApiResponse<ContestDetailResponse> createContestInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestCreateRequest request);
    
    @PostMapping("/api/v1/contest/internal/batch")
    ApiResponse<ContestBatchResponse> createContestBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestBatchRequest request);
}
```

### 2. 배치 잡 통합 설계

#### 배치 잡 대상 클라이언트 목록

**Contest 데이터 수집 대상**:
- **client-feign**: Codeforces, GitHub, Kaggle, ProductHunt, Reddit, HackerNews, DevTo
- **client-scraper**: LeetCode, Google Summer of Code, Devpost, MLH, AtCoder

**News 데이터 수집 대상**:
- **client-feign**: NewsAPI, DevTo, Reddit, HackerNews
- **client-rss**: TechCrunch, Google Developers Blog, Ars Technica, Medium Technology

#### 패키지 구조 설계

**배치 잡 이름 규칙** (중요):
- **client-feign 클라이언트**: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`, `NewsNewsApiApiJobConfig`)
- **client-rss 클라이언트**: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`, `NewsGoogleDevelopersRssParserJobConfig`)
- **client-scraper 클라이언트**: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`, `ContestAtCoderScraperJobConfig`)

**Contest 배치 잡**:
```
batch-source/
  src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/
    contest/
      codeforces/ (기존, client-feign)
        jobconfig/ContestCodeforcesApiJobConfig.java
        reader/CodeforcesApiPagingItemReader.java
        writer/CodeforcesStep1Writer.java
        ...
      github/ (client-feign)
        jobconfig/ContestGitHubApiJobConfig.java
        reader/GitHubApiPagingItemReader.java
        writer/GitHubStep1Writer.java
        ...
      kaggle/ (client-feign)
        jobconfig/ContestKaggleApiJobConfig.java
        reader/KaggleApiPagingItemReader.java
        writer/KaggleStep1Writer.java
        ...
      leetcode/ (client-scraper)
        jobconfig/ContestLeetCodeScraperJobConfig.java
        reader/LeetCodeScrapingItemReader.java
        writer/LeetCodeStep1Writer.java
        ...
      atcoder/ (client-scraper)
        jobconfig/ContestAtCoderScraperJobConfig.java
        reader/AtCoderScrapingItemReader.java
        writer/AtCoderStep1Writer.java
        ...
      ...
    news/
      newsapi/ (client-feign)
        jobconfig/NewsNewsApiApiJobConfig.java
        reader/NewsApiPagingItemReader.java
        writer/NewsApiStep1Writer.java
        ...
      techcrunch/ (client-rss)
        jobconfig/NewsTechCrunchRssParserJobConfig.java
        reader/TechCrunchRssItemReader.java
        writer/TechCrunchStep1Writer.java
        ...
      google-developers/ (client-rss)
        jobconfig/NewsGoogleDevelopersRssParserJobConfig.java
        reader/GoogleDevelopersRssItemReader.java
        writer/GoogleDevelopersStep1Writer.java
        ...
      ...
```

#### JobConfig 패턴 설계

**일관된 구조**:
1. **Job Bean**: Job 이름, Step 연결, Incrementer 설정
2. **Step Bean**: Chunk 크기, Reader, Processor, Writer 설정
3. **Reader Bean**: `*PagingItemReader` 또는 `*ItemReader` 구현
4. **Processor Bean**: Client DTO → batch-source DTO 변환
   - **중요**: batch-source 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
   - Client 모듈의 DTO (예: `CodeforcesDto.Contest`) → batch-source 모듈의 DTO (예: `ContestCreateRequest`)
5. **Writer Bean**: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
   - **중요**: client-feign 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
   - batch-source 모듈의 DTO → client-feign 모듈의 DTO 변환 → Feign Client 호출
6. **JobParameter Bean**: Job 파라미터 관리
7. **Incrementer Bean**: Job 실행 제어

**구현 예시 1: client-feign 클라이언트** (`*ApiJobConfig` 패턴):
```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestGitHubApiJobConfig {  // *ApiJobConfig 명명 규칙
    
    @Value("${baseDate:#{null}}")
    private String baseDate;
    
    private final GitHubApiService service;
    private final ContestGitHubJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    
    @Bean(name=Constants.CONTEST_GITHUB + Constants.PARAMETER)
    @JobScope
    public ContestGitHubJobParameter parameter() { 
        return new ContestGitHubJobParameter(); 
    }
    
    @Bean(name=Constants.CONTEST_GITHUB)
    public Job ContestGitHubJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_GITHUB + Constants.STEP_1) Step step1) {
        return new JobBuilder(Constants.CONTEST_GITHUB, jobRepository)
            .start(step1)
            .incrementer(new ContestGitHubIncrementer(baseDate))
            .build();
    }
    
    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository) {
        // GitHubEvent (client-feign DTO) → ContestCreateRequest (batch-source DTO)
        return new StepBuilder(Constants.CONTEST_GITHUB + Constants.STEP_1, jobRepository)
            .<GitHubEvent, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(step1Reader())
            .processor(step1Processor()) // Client DTO → batch-source DTO 변환
            .writer(step1Writer()) // batch-source DTO → client-feign DTO 변환 후 API 호출
            .build();
    }
    
    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_READER)
    public GitHubApiPagingItemReader<GitHubEvent> step1Reader() {
        return new GitHubApiPagingItemReader<GitHubEvent>(
            Constants.CHUNK_SIZE_10, service);
    }
    
    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    public GitHubStep1Processor step1Processor() {
        return new GitHubStep1Processor();
    }
    
    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_WRITER)
    public GitHubStep1Writer step1Writer() {
        return new GitHubStep1Writer(contestInternalApi);
    }
}
```

**구현 예시 2: client-rss 클라이언트** (`*RssParserJobConfig` 패턴):
```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class NewsTechCrunchRssParserJobConfig {  // *RssParserJobConfig 명명 규칙
    
    @Value("${baseDate:#{null}}")
    private String baseDate;
    
    private final TechCrunchRssParser rssParser;
    private final NewsTechCrunchJobParameter parameter;
    private final NewsInternalContract newsInternalApi;
    
    @Bean(name=Constants.NEWS_TECHCRUNCH + Constants.PARAMETER)
    @JobScope
    public NewsTechCrunchJobParameter parameter() { 
        return new NewsTechCrunchJobParameter(); 
    }
    
    @Bean(name=Constants.NEWS_TECHCRUNCH)
    public Job NewsTechCrunchJob(JobRepository jobRepository
        , @Qualifier(Constants.NEWS_TECHCRUNCH + Constants.STEP_1) Step step1) {
        return new JobBuilder(Constants.NEWS_TECHCRUNCH, jobRepository)
            .start(step1)
            .incrementer(new NewsTechCrunchIncrementer(baseDate))
            .build();
    }
    
    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository) {
        // RssFeedItem (client-rss DTO) → NewsCreateRequest (batch-source DTO)
        return new StepBuilder(Constants.NEWS_TECHCRUNCH + Constants.STEP_1, jobRepository)
            .<RssFeedItem, NewsCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(step1Reader())
            .processor(step1Processor()) // Client DTO → batch-source DTO 변환
            .writer(step1Writer()) // batch-source DTO → client-feign DTO 변환 후 API 호출
            .build();
    }
    
    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_READER)
    public TechCrunchRssItemReader<RssFeedItem> step1Reader() {
        return new TechCrunchRssItemReader<RssFeedItem>(
            Constants.CHUNK_SIZE_10, rssParser);
    }
    
    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    public TechCrunchStep1Processor step1Processor() {
        return new TechCrunchStep1Processor();
    }
    
    @Bean(name = Constants.NEWS_TECHCRUNCH + Constants.STEP_1 + Constants.ITEM_WRITER)
    public TechCrunchStep1Writer step1Writer() {
        return new TechCrunchStep1Writer(newsInternalApi);
    }
}
```

**구현 예시 3: client-scraper 클라이언트** (`*ScraperJobConfig` 패턴):
```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContestLeetCodeScraperJobConfig {  // *ScraperJobConfig 명명 규칙
    
    @Value("${baseDate:#{null}}")
    private String baseDate;
    
    private final LeetCodeScraper scraper;
    private final ContestLeetCodeJobParameter parameter;
    private final ContestInternalContract contestInternalApi;
    
    @Bean(name=Constants.CONTEST_LEETCODE + Constants.PARAMETER)
    @JobScope
    public ContestLeetCodeJobParameter parameter() { 
        return new ContestLeetCodeJobParameter(); 
    }
    
    @Bean(name=Constants.CONTEST_LEETCODE)
    public Job ContestLeetCodeJob(JobRepository jobRepository
        , @Qualifier(Constants.CONTEST_LEETCODE + Constants.STEP_1) Step step1) {
        return new JobBuilder(Constants.CONTEST_LEETCODE, jobRepository)
            .start(step1)
            .incrementer(new ContestLeetCodeIncrementer(baseDate))
            .build();
    }
    
    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1)
    @JobScope
    public Step step1(JobRepository jobRepository) {
        // ScrapedContestItem (client-scraper DTO) → ContestCreateRequest (batch-source DTO)
        return new StepBuilder(Constants.CONTEST_LEETCODE + Constants.STEP_1, jobRepository)
            .<ScrapedContestItem, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(step1Reader())
            .processor(step1Processor()) // Client DTO → batch-source DTO 변환
            .writer(step1Writer()) // batch-source DTO → client-feign DTO 변환 후 API 호출
            .build();
    }
    
    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_READER)
    public LeetCodeScrapingItemReader<ScrapedContestItem> step1Reader() {
        return new LeetCodeScrapingItemReader<ScrapedContestItem>(
            Constants.CHUNK_SIZE_10, scraper);
    }
    
    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_PROCESSOR)
    public LeetCodeStep1Processor step1Processor() {
        return new LeetCodeStep1Processor();
    }
    
    @Bean(name = Constants.CONTEST_LEETCODE + Constants.STEP_1 + Constants.ITEM_WRITER)
    public LeetCodeStep1Writer step1Writer() {
        return new LeetCodeStep1Writer(contestInternalApi);
    }
}
```

### 3. PagingItemReader 설계

#### 패턴 설계
- `AbstractPagingItemReader` 상속
- Service를 통한 데이터 수집
- 페이징 처리 로직

#### 구현 예시 (CodeforcesApiPagingItemReader 패턴 준수)
```java
@Slf4j
public class GitHubApiPagingItemReader<T> extends AbstractPagingItemReader<T> {
    
    protected GitHubApiService service;
    
    public GitHubApiPagingItemReader(int pageSize, GitHubApiService service) {
        setPageSize(pageSize);
        this.service = service;
    }
    
    @Override
    protected void doReadPage() {
        initResults();
        
        List<GitHubEvent> itemList = service.getEvents();
        
        for (GitHubEvent item : itemList) {
            results.add((T) item);
        }
    }
    
    protected void initResults() {
        if (CollectionUtils.isEmpty(results)) {
            results = new CopyOnWriteArrayList<>();
        } else {
            results.clear();
        }
    }
    
    @Override
    protected void doOpen() throws Exception {
        log.info("doOpen ... ");
        log.info("pageSize : {}", getPageSize());
    }
    
    @Override
    protected void doClose() throws Exception {
        log.info("doClose ... ");
    }
}
```

#### RSS/Scraper ItemReader 설계
- RSS: `RssParser`를 통한 데이터 수집
- Scraper: `WebScraper`를 통한 데이터 수집
- `AbstractPagingItemReader` 패턴 준수

### 4. Item Processor 설계

#### DTO 변환 패턴
- **Client DTO → batch-source DTO 변환**
- Client 모듈의 DTO (예: `CodeforcesDto.Contest`, `RssFeedItem`, `ScrapedContestItem`)
- batch-source 모듈의 DTO (예: `ContestCreateRequest`, `NewsCreateRequest`)
- **중요**: batch-source 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의

#### 외부 정보 제공자 공식 문서 참고 (필수)
- **각 Processor는 반드시 외부 정보 제공자의 공식 문서를 참고하여 필드 매핑 수행**
- **공식 문서만 참고**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음
- **문서 참고 방법**:
  1. `json/sources.json` 파일에서 각 출처의 `documentation_url` 확인
  2. 공식 문서에서 API 응답 필드 구조 확인
  3. Client 모듈의 DTO 필드와 공식 문서의 필드 매핑 확인
  4. batch-source 모듈의 DTO 필드에 정확히 매핑
- **주요 출처별 공식 문서 URL** (sources.json 참고):
  - Codeforces: `https://codeforces.com/apiHelp`
  - GitHub: `https://docs.github.com/en/rest`
  - Kaggle: `https://www.kaggle.com/docs/api`
  - Hacker News: `https://github.com/HackerNews/API`
  - NewsAPI: `https://newsapi.org/docs`
  - DevTo: `https://developers.forem.com/api`
  - Reddit: `https://www.reddit.com/dev/api`
  - ProductHunt: `https://api.producthunt.com/v2/docs`
  - RSS 피드: 각 출처의 RSS 피드 스펙 문서 (RSS 2.0, Atom 1.0 등)
  - Web Scraping: 각 웹사이트의 공식 문서 또는 robots.txt

#### 필드 매핑 가이드
- **필수 필드 매핑**: 
  - `sourceId`: 출처 ID (MongoDB ObjectId)
  - `title`: 제목 (대회명 또는 뉴스 제목)
  - `startDate`: 시작 일시 (Contest의 경우)
  - `endDate`: 종료 일시 (Contest의 경우)
  - `url`: 원본 URL
  - `description`: 설명
- **선택 필드 매핑**:
  - `metadata`: 메타데이터 (prize, participants, tags 등)
  - `status`: 상태 (UPCOMING, ONGOING, ENDED 등)
- **필드 변환 규칙**:
  - 날짜/시간 필드: 공식 문서의 형식에 따라 적절히 변환 (ISO 8601 권장)
  - 숫자 필드: null 처리 및 기본값 설정
  - 문자열 필드: null 체크 및 빈 문자열 처리
  - 배열 필드: null 체크 및 빈 리스트 처리

#### 구현 예시
```java
@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Processor implements ItemProcessor<Contest, ContestCreateRequest> {
    
    /**
     * Codeforces API 공식 문서 참고:
     * https://codeforces.com/apiHelp
     * 
     * Contest 객체 필드:
     * - id: Integer (대회 ID)
     * - name: String (대회명)
     * - type: String (대회 타입)
     * - phase: String (대회 단계)
     * - startTimeSeconds: Long (시작 시간, Unix timestamp)
     * - durationSeconds: Integer (지속 시간, 초)
     * - websiteUrl: String (웹사이트 URL)
     * - description: String (설명)
     */
    @Override
    public ContestCreateRequest process(Contest item) throws Exception {
        // 공식 문서를 참고하여 정확한 필드 매핑
        LocalDateTime startDate = item.startTimeSeconds() != null
            ? LocalDateTime.ofEpochSecond(item.startTimeSeconds(), 0, ZoneOffset.UTC)
            : null;
        
        LocalDateTime endDate = (item.startTimeSeconds() != null && item.durationSeconds() != null)
            ? LocalDateTime.ofEpochSecond(
                item.startTimeSeconds() + item.durationSeconds(), 0, ZoneOffset.UTC)
            : null;
        
        return ContestCreateRequest.builder()
            .sourceId(getSourceId()) // 출처 ID (MongoDB ObjectId)
            .title(item.name() != null ? item.name() : "")
            .startDate(startDate)
            .endDate(endDate)
            .url(item.websiteUrl() != null ? item.websiteUrl() : 
                "https://codeforces.com/contests/" + item.id())
            .description(item.description() != null ? item.description() : "")
            .metadata(ContestMetadata.builder()
                .sourceName("Codeforces API")
                .tags(extractTags(item))
                .build())
            .build();
    }
    
    private String getSourceId() {
        // SourcesDocument에서 Codeforces 출처의 ID 조회
        // 구현 생략
    }
    
    private List<String> extractTags(Contest item) {
        // type, kind 등에서 태그 추출
        // 구현 생략
    }
}
```

#### RSS/Scraper Processor 설계
- **RSS Processor**: RSS 피드 스펙 문서 참고 (RSS 2.0, Atom 1.0)
  - `RssFeedItem`의 필드 구조 확인
  - 공식 RSS 스펙에 따른 필드 매핑
- **Scraper Processor**: 각 웹사이트의 공식 문서 참고
  - `ScrapedContestItem`의 필드 구조 확인
  - 웹사이트의 HTML 구조 및 데이터 형식 확인

### 5. Item Writer 설계

#### 내부 API 호출 패턴
- **batch-source DTO → client-feign DTO 변환 후 Feign Client 호출**
- Feign Client (`ContestInternalContract`, `NewsInternalContract`) 사용
- Chunk 단위로 배치 요청 (`createContestBatchInternal`, `createNewsBatchInternal`)
- 내부 API 키 헤더 설정
- 에러 처리 및 재시도 로직
- **중요**: client-feign 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의

#### 구현 예시
```java
@Slf4j
@StepScope
@RequiredArgsConstructor
public class GitHubStep1Writer implements ItemWriter<ContestCreateRequest> {
    
    private final ContestInternalContract contestInternalApi;
    private final ContestConfig contestConfig; // 내부 API 키 설정
    
    @Override
    public void write(Chunk<? extends ContestCreateRequest> chunk) throws Exception {
        List<? extends ContestCreateRequest> items = chunk.getItems();
        
        // batch-source DTO → client-feign DTO 변환
        List<InternalApiDto.ContestCreateRequest> feignRequests = items.stream()
            .map(item -> InternalApiDto.ContestCreateRequest.builder()
                .sourceId(item.getSourceId())
                .title(item.getTitle())
                .startDate(item.getStartDate())
                // ... 필드 매핑 (필드가 같아도 별도 객체 생성)
                .build())
            .collect(Collectors.toList());
        
        InternalApiDto.ContestBatchRequest batchRequest = InternalApiDto.ContestBatchRequest.builder()
            .contests(feignRequests)
            .build();
        
        // 내부 API 호출 (client-feign DTO 사용)
        ApiResponse<ContestBatchResponse> response = contestInternalApi
            .createContestBatchInternal(contestConfig.getApiKey(), batchRequest);
        
        if (!response.isSuccess()) {
            log.error("Failed to create contests batch: {}", response.getMessage());
            throw new BatchWriteException("Failed to create contests batch");
        }
        
        log.info("Successfully created {} contests", response.getData().getSuccessCount());
    }
}
```

### 6. 데이터 흐름 설계

#### 전체 데이터 흐름
```
외부 출처 (API/RSS/Web)
  ↓
client-feign / client-rss / client-scraper
  ↓ (외부 요청, 데이터 수집 및 정제)
  Client DTO (예: CodeforcesDto.Contest, RssFeedItem, ScrapedContestItem)
  ↓
Batch 모듈 (batch-source)
  ├─ Item Reader: client/* 모듈의 수집 데이터 읽기
  │   ├─ *PagingItemReader (Feign API)
  │   ├─ *RssItemReader (RSS Parser)
  │   └─ *ScrapingItemReader (Web Scraper)
  ├─ Item Processor: Client DTO → batch-source DTO 변환
  │   Client DTO → ContestCreateRequest (batch-source 모듈의 DTO, api-contest와 별도 정의)
  └─ Item Writer: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
      batch-source DTO → InternalApiDto.ContestCreateRequest (client-feign 모듈의 DTO)
      ↓
      ContestInternalContract / NewsInternalContract (Feign Client)
      ↓
      HTTP 요청 (client-feign DTO 사용)
  ↓
api-contest / api-news
  api-contest/api-news 모듈의 DTO로 수신 (client-feign DTO와 별도 정의)
  ↓ (MongoDB 저장)
MongoDB Atlas (ContestDocument / NewsArticleDocument)
```

#### DTO 변환 흐름 (중요)
```
1. Client 모듈 DTO
   - client-feign: CodeforcesDto.Contest, GitHubDto.Event 등
   - client-rss: RssFeedItem
   - client-scraper: ScrapedContestItem
   
2. batch-source 모듈 DTO (각 모듈에서 독립적으로 정의)
   - ContestCreateRequest (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
   - NewsCreateRequest (api-news 모듈의 DTO와 필드가 같아도 별도 정의)
   
3. client-feign 모듈의 내부 API DTO (각 모듈에서 독립적으로 정의)
   - InternalApiDto.ContestCreateRequest (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
   - InternalApiDto.NewsCreateRequest (api-news 모듈의 DTO와 필드가 같아도 별도 정의)
   
4. api-contest/api-news 모듈 DTO (각 모듈에서 독립적으로 정의)
   - ContestCreateRequest (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)
   - NewsCreateRequest (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)
```

### 7. Constants 설계

#### Job 이름 상수 추가
**명명 규칙**: Job 이름은 소문자와 점(.)을 사용하며, 클라이언트 타입을 구분하지 않음 (JobConfig 클래스 이름에서만 구분)

```java
public class Constants {
    // Contest Jobs (client-feign: *ApiJobConfig)
    public final static String CONTEST_CODEFORCES = "contest.codeforces.job";
    public final static String CONTEST_GITHUB = "contest.github.job";
    public final static String CONTEST_KAGGLE = "contest.kaggle.job";
    public final static String CONTEST_PRODUCTHUNT = "contest.producthunt.job";
    public final static String CONTEST_REDDIT = "contest.reddit.job";
    public final static String CONTEST_HACKERNEWS = "contest.hackernews.job";
    public final static String CONTEST_DEVTO = "contest.devto.job";
    
    // Contest Jobs (client-scraper: *ScraperJobConfig)
    public final static String CONTEST_LEETCODE = "contest.leetcode.job";
    public final static String CONTEST_GSOC = "contest.gsoc.job";
    public final static String CONTEST_DEVPOST = "contest.devpost.job";
    public final static String CONTEST_MLH = "contest.mlh.job";
    public final static String CONTEST_ATCODER = "contest.atcoder.job";
    
    // News Jobs (client-feign: *ApiJobConfig)
    public final static String NEWS_NEWSAPI = "news.newsapi.job";
    public final static String NEWS_DEVTO = "news.devto.job";
    public final static String NEWS_REDDIT = "news.reddit.job";
    public final static String NEWS_HACKERNEWS = "news.hackernews.job";
    
    // News Jobs (client-rss: *RssParserJobConfig)
    public final static String NEWS_TECHCRUNCH = "news.techcrunch.job";
    public final static String NEWS_GOOGLE_DEVELOPERS = "news.google.developers.job";
    public final static String NEWS_ARS_TECHNICA = "news.ars.technica.job";
    public final static String NEWS_MEDIUM = "news.medium.job";
}
```

#### JobConfig 클래스 이름 규칙
- **client-feign 클라이언트**: `{Domain}{Source}ApiJobConfig`
  - 예: `ContestCodeforcesApiJobConfig`, `NewsNewsApiApiJobConfig`
- **client-rss 클라이언트**: `{Domain}{Source}RssParserJobConfig`
  - 예: `NewsTechCrunchRssParserJobConfig`, `NewsGoogleDevelopersRssParserJobConfig`
- **client-scraper 클라이언트**: `{Domain}{Source}ScraperJobConfig`
  - 예: `ContestLeetCodeScraperJobConfig`, `ContestAtCoderScraperJobConfig`

## 설계 문서 작성 체크리스트

### 필수 포함 사항
- [ ] DTO 독립성 원칙 설계
  - [ ] 각 모듈의 DTO 정의 위치 명시
  - [ ] batch-source 모듈의 DTO 설계 (api-contest/api-news와 별도 정의)
  - [ ] client-feign 모듈의 내부 API DTO 설계 (api-contest/api-news와 별도 정의)
  - [ ] DTO 변환 흐름 설계
- [ ] Feign Client 내부 API 호출 설계
  - [ ] ContestInternalContract 설계
  - [ ] NewsInternalContract 설계
  - [ ] InternalApiFeignConfig 설계
  - [ ] application-feign-internal.yml 설정 설계
  - [ ] client-feign 모듈의 DTO 정의 (InternalApiDto)
- [ ] 배치 잡 통합 설계
  - [ ] 모든 클라이언트 모듈에 대한 JobConfig 목록
  - [ ] JobConfig 패턴 설계 (ContestCodeforcesApiJobConfig 준수)
  - [ ] JobConfig 클래스 이름 규칙 설계
    - [ ] client-feign: *ApiJobConfig
    - [ ] client-rss: *RssParserJobConfig
    - [ ] client-scraper: *ScraperJobConfig
  - [ ] 패키지 구조 설계
- [ ] PagingItemReader 설계
  - [ ] Feign API용 *PagingItemReader 설계
  - [ ] RSS용 *RssItemReader 설계
  - [ ] Scraper용 *ScrapingItemReader 설계
- [ ] Item Processor 설계
  - [ ] Client DTO → batch-source DTO 변환 패턴 설계
  - [ ] 각 클라이언트별 Processor 구현 설계
  - [ ] 외부 정보 제공자 공식 문서 참고 방법 설계
  - [ ] 각 출처별 필드 매핑 가이드 작성
  - [ ] sources.json의 documentation_url 활용 방법 설계
- [ ] Item Writer 설계
  - [ ] batch-source DTO → client-feign DTO 변환 패턴 설계
  - [ ] 내부 API 호출 패턴 설계
  - [ ] 에러 처리 및 재시도 로직 설계
- [ ] 데이터 흐름 설계
  - [ ] 전체 데이터 흐름 다이어그램
  - [ ] DTO 변환 흐름 다이어그램
  - [ ] 각 단계별 상세 설명
- [ ] Constants 설계
  - [ ] Job 이름 상수 정의
  - [ ] JobConfig 클래스 이름 규칙 정리
- [ ] 에러 처리 설계
  - [ ] 배치 잡 실패 처리 전략
  - [ ] 내부 API 호출 실패 처리 전략

### 참고 사항
- [ ] ContestCodeforcesApiJobConfig 패턴과의 일관성 확인
- [ ] JobConfig 클래스 이름 규칙 준수 확인
  - [ ] client-feign: *ApiJobConfig
  - [ ] client-rss: *RssParserJobConfig
  - [ ] client-scraper: *ScraperJobConfig
- [ ] client-feign, client-rss, client-scraper 모듈 구조와의 호환성 확인
- [ ] docs/step9/ Contest/News API 설계 문서 준수
- [ ] 클린코드 원칙 준수 (SOLID)
- [ ] 객체지향 설계 원칙 준수

## 주의사항

### 오버엔지니어링 방지
- **요청하지 않은 기능 추가 금지**: 요구사항에 명시되지 않은 기능은 구현하지 않음
- **과도한 추상화 금지**: 필요한 수준의 추상화만 사용
- **불필요한 레이어 추가 금지**: 명확한 목적이 없는 중간 레이어 추가 금지
- **기존 패턴 준수**: ContestCodeforcesApiJobConfig 패턴을 엄격히 준수하여 일관성 유지

### 공식 문서만 참고
- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **Spring Batch 공식 문서**: https://spring.io/projects/spring-batch
- **Spring Cloud OpenFeign 공식 문서**: https://spring.io/projects/spring-cloud-openfeign
- **신뢰할 수 없는 자료 참고 금지**: 블로그, 커뮤니티 자료는 참고하지 않음

### 구현 범위 제한
- **기존 패턴 준수**: ContestCodeforcesJobConfig 패턴을 엄격히 준수
- **필요한 JobConfig만 추가**: 모든 클라이언트 모듈에 대해 JobConfig 추가
- **일관된 구조 유지**: 모든 JobConfig가 동일한 구조를 따름

### DTO 독립성 원칙 (중요)
- **모듈 간 DTO 공유 금지**: 필드가 동일하더라도 각 모듈에서 독립적으로 DTO 정의
- **batch-source 모듈의 DTO**: 
  - 위치: `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/{contest|news}/dto/`
  - api-contest/api-news 모듈의 DTO와 필드가 같아도 별도 정의
- **client-feign 모듈의 내부 API DTO**:
  - 위치: `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/internal/contract/InternalApiDto.java`
  - api-contest/api-news 모듈의 DTO와 필드가 같아도 별도 정의
- **api-contest/api-news 모듈의 DTO**:
  - 위치: `api/{contest|news}/src/main/java/com/ebson/shrimp/tm/demo/api/{contest|news}/dto/`
  - batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의
- **DTO 변환 필수**: Item Processor와 Item Writer에서 DTO 변환 수행
- **공통 모듈에 DTO 정의 금지**: common 모듈 등에 DTO를 정의하여 공유하지 않음

### 배치 잡 설계 주의사항
- **Job 이름 일관성**: Constants에 정의된 Job 이름 사용
- **JobConfig 클래스 이름 규칙 준수** (중요):
  - **client-feign**: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`)
  - **client-rss**: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
  - **client-scraper**: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
- **Step 구조 일관성**: 모든 JobConfig가 동일한 Step 구조 사용
- **Reader/Processor/Writer 분리**: 각 컴포넌트의 책임 명확히 분리
- **에러 처리**: 배치 잡 실패 시 재시도 및 알림 처리

### Processor 필드 매핑 주의사항 (중요)
- **공식 문서 필수 참고**: 각 Processor는 반드시 외부 정보 제공자의 공식 문서를 참고하여 필드 매핑 수행
- **sources.json 활용**: `json/sources.json` 파일의 `documentation_url` 필드를 참고하여 공식 문서 URL 확인
- **정확한 필드 매핑**: 공식 문서의 필드 구조를 정확히 파악하여 batch-source DTO에 매핑
- **날짜/시간 변환**: 공식 문서의 날짜/시간 형식에 따라 적절히 변환 (ISO 8601 권장)
- **null 처리**: 필수 필드의 null 체크 및 기본값 설정
- **데이터 검증**: 필드 매핑 후 유효성 검사 로직 포함
- **문서화**: 각 Processor에 공식 문서 URL 및 필드 매핑 로직 주석 추가

## 출력 결과물

설계 문서 작성 후 다음 결과물을 제공해야 합니다:

1. **설계 문서**: `docs/step10/batch-job-integration-design.md`
   - Feign Client 내부 API 호출 설계
   - 배치 잡 통합 설계
   - PagingItemReader 설계
   - Item Writer 설계
   - 데이터 흐름 설계
   - Constants 설계
   - 에러 처리 설계

2. **검증 기준**:
   - ContestCodeforcesApiJobConfig 패턴과의 일관성
   - JobConfig 클래스 이름 규칙 준수 여부
   - 모든 클라이언트 모듈에 대한 JobConfig 포함 여부
   - client-feign, client-rss, client-scraper 모듈과의 호환성
   - 클린코드 원칙 준수
   - 객체지향 설계 원칙 준수

## 실행 명령어

batch-source 모듈의 배치 잡 통합 및 client-feign 모듈의 내부 API 호출을 위한 종합 설계 문서 작성을 시작하세요.

참고 파일:
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/jobconfig/ContestCodeforcesJobConfig.java` (JobConfig 패턴 참고, 이름은 `ContestCodeforcesApiJobConfig`로 변경 필요)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/reader/CodeforcesApiPagingItemReader.java` (Reader 패턴 참고)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/processor/CodeforcesStep1Processor.java` (Processor 패턴 참고, 필드 매핑 미완성 상태)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/writer/CodeforcesStep1Writer.java` (Writer 패턴 참고)
- `api/contest/src/main/java/com/ebson/shrimp/tm/demo/api/contest/controller/ContestController.java` (내부 API 엔드포인트 참고)
- `api/news/src/main/java/com/ebson/shrimp/tm/demo/api/news/controller/NewsController.java` (내부 API 엔드포인트 참고)
- `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/codeforces/contract/CodeforcesContract.java` (Contract 패턴 참고)
- `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/codeforces/contract/CodeforcesDto.java` (Client DTO 구조 참고)
- `json/sources.json` (각 출처의 documentation_url 참고, 필수)
- `docs/step9/contest-news-api-design.md` (API 설계 참고)
- `docs/step8/rss-scraper-modules-analysis.md` (RSS/Scraper 모듈 분석 참고)

작업 내용:
1. DTO 독립성 원칙 설계
   - 각 모듈의 DTO 정의 위치 및 구조 설계
   - batch-source 모듈의 DTO 설계 (api-contest/api-news와 별도 정의)
   - client-feign 모듈의 내부 API DTO 설계 (api-contest/api-news와 별도 정의)
   - DTO 변환 흐름 및 매핑 전략 설계
   - 모듈 간 DTO 공유 금지 원칙 명시
   
2. Feign Client 내부 API 호출 설계
   - ContestInternalContract, NewsInternalContract 인터페이스 설계
   - InternalApiFeignConfig 설정 설계
   - application-feign-internal.yml 설정 설계
   - client-feign 모듈의 DTO 정의 (InternalApiDto)
   - 기존 Feign Client 패턴과의 일관성 유지
   
3. 배치 잡 통합 설계
   - 모든 클라이언트 모듈에 대한 JobConfig 목록 작성
   - ContestCodeforcesApiJobConfig 패턴을 참고한 일관된 구조 설계
   - JobConfig 클래스 이름 규칙 설계
     - client-feign: `*ApiJobConfig`
     - client-rss: `*RssParserJobConfig`
     - client-scraper: `*ScraperJobConfig`
   - 패키지 구조 설계 (domain/{contest|news}/{source-name}/)
   - Job, Step, Reader, Processor, Writer 구조 설계
   
4. PagingItemReader 설계
   - Feign API용 *PagingItemReader 설계 (CodeforcesApiPagingItemReader 패턴 준수)
   - RSS용 *RssItemReader 설계
   - Scraper용 *ScrapingItemReader 설계
   - AbstractPagingItemReader 상속 패턴 준수
   
5. Item Processor 설계
   - Client DTO → batch-source DTO 변환 패턴 설계
   - 각 클라이언트별 Processor 구현 설계
   - DTO 변환 로직 및 매핑 전략 설계
   - **외부 정보 제공자 공식 문서 참고 방법 설계**
     - sources.json의 documentation_url 활용
     - 각 출처별 공식 문서 URL 정리
     - 필드 매핑 가이드 작성
   - **필드 매핑 규칙 설계**
     - 필수 필드 매핑 (sourceId, title, startDate, endDate, url 등)
     - 선택 필드 매핑 (metadata, status 등)
     - 날짜/시간 변환 규칙
     - null 처리 및 기본값 설정
   
6. Item Writer 설계
   - batch-source DTO → client-feign DTO 변환 패턴 설계
   - 내부 API 호출 패턴 설계 (Feign Client 사용)
   - Chunk 단위 배치 요청 설계
   - 에러 처리 및 재시도 로직 설계
   
7. 데이터 흐름 설계
   - 전체 데이터 흐름 다이어그램
   - DTO 변환 흐름 다이어그램 (각 모듈별 DTO 표시)
   - 각 단계별 상세 설명
   - Client 모듈 → Batch 모듈 → API 모듈 → MongoDB Atlas 흐름
   
8. Constants 설계
   - 모든 Job 이름 상수 정의
   - 기존 Constants 클래스와의 일관성 유지
   
9. 에러 처리 설계
   - 배치 잡 실패 처리 전략
   - 내부 API 호출 실패 처리 전략
   - 재시도 및 알림 처리 전략

검증 기준:
- **DTO 독립성 원칙 준수**: 각 모듈에서 독립적으로 DTO 정의, 모듈 간 DTO 공유 금지
- **외부 정보 제공자 공식 문서 참고**: 각 Processor가 공식 문서를 참고하여 필드 매핑 수행
- **sources.json 활용**: documentation_url을 활용하여 공식 문서 URL 확인
- **JobConfig 클래스 이름 규칙 준수**: 
  - client-feign: `*ApiJobConfig`
  - client-rss: `*RssParserJobConfig`
  - client-scraper: `*ScraperJobConfig`
- ContestCodeforcesApiJobConfig 패턴과의 일관성
- 모든 클라이언트 모듈에 대한 JobConfig 포함 여부
- client-feign, client-rss, client-scraper 모듈과의 호환성
- DTO 변환 흐름의 명확성 (Client DTO → batch-source DTO → client-feign DTO → api-contest/api-news DTO)
- 필드 매핑의 정확성 (공식 문서 기반)
- 클린코드 원칙 준수 (SOLID)
- 객체지향 설계 원칙 준수
- 오버엔지니어링 방지 (요청하지 않은 기능 추가 금지)
- 공식 문서만 참고 (Spring Boot, Spring Batch, Spring Cloud OpenFeign, 외부 정보 제공자 공식 문서)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-XX  
**작성자**: System Architect
