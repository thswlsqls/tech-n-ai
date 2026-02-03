> ⚠️ 본 설계서는 Contest/News 수집 기능 폐기로 더 이상 유효하지 않습니다. (폐기됨)

# Batch Job 통합 설계 문서

**작성 일시**: 2026-01-XX
**대상 모듈**: `batch-source`, `client-feign`
**목적**: 모든 클라이언트 모듈의 데이터 수집을 위한 배치 잡 통합 설계

## 목차

1. [개요](#개요) 
2. [DTO 독립성 원칙 설계](#dto-독립성-원칙-설계) 
3. [Feign Client 내부 API 호출 설계](#feign-client-내부-api-호출-설계) 
4. [배치 잡 통합 설계](#배치-잡-통합-설계) 
5. [PagingItemReader 설계](#pagingitemreader-설계) 
6. [Item Processor 설계](#item-processor-설계) 
7. [Item Writer 설계](#item-writer-설계) 
8. [데이터 흐름 설계](#데이터-흐름-설계) 
9. [Constants 설계](#constants-설계) 
10. [에러 처리 설계](#에러-처리-설계) 

---

## 개요

이 설계 문서는 `batch-source` 모듈의 배치 잡 통합 및 `client-feign` 모듈의 내부 API 호출을 위한 종합 설계 문서입니다. 모든 클라이언트 모듈(`client-feign`, `client-rss`, `client-scraper`)의 데이터 수집을 위한 일관된 배치 잡 구조를 제공합니다.

### 설계 원칙

1. **프로젝트 구조 일관성**: `ContestCodeforcesApiJobConfig` 패턴을 엄격히 준수
2. **DTO 독립성 원칙**: 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)
3. **JobConfig 클래스 이름 규칙**:
   - **client-feign**: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`)
   - **client-rss**: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
   - **client-scraper**: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
4. **클린코드 원칙**: SOLID 원칙 준수, 단일 책임 원칙, 의존성 역전 원칙
5. **공식 문서 참고**: Spring Boot, Spring Batch, Spring Cloud OpenFeign 공식 문서만 참고

### 참고 문서

- **Contest/News API 설계**: `docs/step9/contest-news-api-design.md`
- **RSS/Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
- **sources.json**: `json/sources.json` (각 출처의 documentation_url 참고)

---

## DTO 독립성 원칙 설계

### 원칙

**각 모듈은 독립적으로 DTO를 정의해야 함**: 필드가 동일하더라도 각 모듈에서 별도로 정의

### DTO 정의 위치

#### 1. batch-source 모듈의 DTO

**위치**: `batch/source/src/main/java/com/tech/n/ai/batch/source/domain/{contest|news}/dto/`

**DTO 목록**:
- `ContestCreateRequest` (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
- `ContestBatchRequest` (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
- `NewsCreateRequest` (api-news 모듈의 DTO와 필드가 같아도 별도 정의)
- `NewsBatchRequest` (api-news 모듈의 DTO와 필드가 같아도 별도 정의)

**특징**:
- `@Builder` 어노테이션 사용 (Item Processor에서 빌더 패턴으로 생성)
- `@Valid`, `@NotBlank`, `@NotNull` 등 검증 어노테이션 포함
- Item Processor에서 Client DTO → batch-source DTO 변환에 사용

#### 2. client-feign 모듈의 내부 API DTO

**위치**: `client/feign/src/main/java/com/tech/n/ai/client/feign/domain/internal/contract/InternalApiDto.java`

**DTO 목록**:
- `InternalApiDto.ContestCreateRequest` (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
- `InternalApiDto.ContestBatchRequest` (api-contest 모듈의 DTO와 필드가 같아도 별도 정의)
- `InternalApiDto.NewsCreateRequest` (api-news 모듈의 DTO와 필드가 같아도 별도 정의)
- `InternalApiDto.NewsBatchRequest` (api-news 모듈의 DTO와 필드가 같아도 별도 정의)

**특징**:
- `@Data`, `@Builder` 어노테이션 사용
- Feign Client의 Request/Response DTO로 사용
- Item Writer에서 batch-source DTO → client-feign DTO 변환에 사용

#### 3. api-contest/api-news 모듈의 DTO

**위치**: `api/{contest|news}/src/main/java/com/tech/n/ai/api/{contest|news}/dto/`

**DTO 목록**:
- `ContestCreateRequest` (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)
- `ContestBatchRequest` (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)
- `NewsCreateRequest` (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)
- `NewsBatchRequest` (batch-source, client-feign 모듈의 DTO와 필드가 같아도 별도 정의)

**특징**:
- `record` 타입 사용 (Java 14+)
- `@Valid`, `@NotBlank`, `@NotNull` 등 검증 어노테이션 포함
- 내부 API 엔드포인트의 Request DTO로 사용

### DTO 변환 흐름

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

### DTO 변환 전략

#### Item Processor에서의 변환

**Client DTO → batch-source DTO 변환**

```java
// CodeforcesStep1Processor 예시
@Override
public ContestCreateRequest process(CodeforcesDto.Contest item) throws Exception {
    // Client DTO (CodeforcesDto.Contest) → batch-source DTO (ContestCreateRequest)
    return ContestCreateRequest.builder()
        .sourceId(getSourceId())
        .title(item.name())
        .startDate(convertStartDate(item.startTimeSeconds()))
        .endDate(convertEndDate(item.startTimeSeconds(), item.durationSeconds()))
        .url(item.websiteUrl())
        .description(item.description())
        .metadata(ContestMetadataRequest.builder()
            .sourceName("Codeforces API")
            .tags(extractTags(item))
            .build())
        .build();
}
```

#### Item Writer에서의 변환

**batch-source DTO → client-feign DTO 변환**

```java
// CodeforcesStep1Writer 예시
@Override
public void write(Chunk<? extends ContestCreateRequest> chunk) throws Exception {
    List<? extends ContestCreateRequest> items = chunk.getItems();
    
    // batch-source DTO → client-feign DTO 변환
    List<InternalApiDto.ContestCreateRequest> feignRequests = items.stream()
        .map(item -> InternalApiDto.ContestCreateRequest.builder()
            .sourceId(item.sourceId())
            .title(item.title())
            .startDate(item.startDate())
            .endDate(item.endDate())
            .description(item.description())
            .url(item.url())
            .metadata(InternalApiDto.ContestMetadataRequest.builder()
                .sourceName(item.metadata().sourceName())
                .prize(item.metadata().prize())
                .participants(item.metadata().participants())
                .tags(item.metadata().tags())
                .build())
            .build())
        .collect(Collectors.toList());
    
    // Feign Client 호출
    InternalApiDto.ContestBatchRequest batchRequest = InternalApiDto.ContestBatchRequest.builder()
        .contests(feignRequests)
        .build();
    
    contestInternalApi.createContestBatchInternal(apiKey, batchRequest);
}
```

### 모듈 간 DTO 공유 금지 원칙

- **공통 모듈에 DTO 정의 금지**: `common` 모듈 등에 DTO를 정의하여 공유하지 않음
- **각 모듈의 독립성 유지**: 모듈 간 의존성을 최소화하여 독립적인 배포 및 테스트 가능
- **필드가 동일해도 별도 정의**: 필드가 완전히 동일하더라도 각 모듈에서 별도로 정의

---

## Feign Client 내부 API 호출 설계

### 패키지 구조

```
client-feign/
  src/main/java/com/tech/n/ai/client/feign/domain/
    internal/
      contract/
        ContestInternalContract.java
        NewsInternalContract.java
        InternalApiDto.java
      client/
        ContestInternalFeignClient.java
        NewsInternalFeignClient.java
      api/
        ContestInternalApi.java
        NewsInternalApi.java
  src/main/java/com/tech/n/ai/client/feign/config/
    ContestInternalFeignConfig.java
    NewsInternalFeignConfig.java
```

### Contract 인터페이스 설계

#### ContestInternalContract

```java
package com.tech.n.ai.client.feign.domain.internal.contract;

import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface ContestInternalContract {
    
    /**
     * Contest 단건 생성 (내부 API)
     */
    @PostMapping("/api/v1/contest/internal")
    ApiResponse<ContestDetailResponse> createContestInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestCreateRequest request);
    
    /**
     * Contest 다건 생성 (내부 API)
     */
    @PostMapping("/api/v1/contest/internal/batch")
    ApiResponse<ContestBatchResponse> createContestBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.ContestBatchRequest request);
}
```

#### NewsInternalContract

```java
package com.tech.n.ai.client.feign.domain.internal.contract;

import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface NewsInternalContract {
    
    /**
     * News 단건 생성 (내부 API)
     */
    @PostMapping("/api/v1/news/internal")
    ApiResponse<NewsDetailResponse> createNewsInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.NewsCreateRequest request);
    
    /**
     * News 다건 생성 (내부 API)
     */
    @PostMapping("/api/v1/news/internal/batch")
    ApiResponse<NewsBatchResponse> createNewsBatchInternal(
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @RequestBody InternalApiDto.NewsBatchRequest request);
}
```

### InternalApiDto 설계

```java
package com.tech.n.ai.client.feign.domain.internal.contract;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내부 API 호출용 DTO (client-feign 모듈에서 독립적으로 정의)
 * api-contest, api-news 모듈의 DTO와 필드가 같아도 별도 정의
 */
public class InternalApiDto {
    
    // ========== Contest 관련 DTO ==========
    
    @Data
    @Builder
    public static class ContestCreateRequest {
        private String sourceId;
        private String title;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String description;
        private String url;
        private ContestMetadataRequest metadata;
    }
    
    @Data
    @Builder
    public static class ContestBatchRequest {
        private List<ContestCreateRequest> contests;
    }
    
    @Data
    @Builder
    public static class ContestMetadataRequest {
        private String sourceName;
        private String prize;
        private Integer participants;
        private List<String> tags;
    }
    
    // ========== News 관련 DTO ==========
    
    @Data
    @Builder
    public static class NewsCreateRequest {
        private String sourceId;
        private String title;
        private String content;
        private String summary;
        private LocalDateTime publishedAt;
        private String url;
        private String author;
        private NewsMetadataRequest metadata;
    }
    
    @Data
    @Builder
    public static class NewsBatchRequest {
        private List<NewsCreateRequest> newsArticles;
    }
    
    @Data
    @Builder
    public static class NewsMetadataRequest {
        private String sourceName;
        private List<String> tags;
        private Integer viewCount;
        private Integer likeCount;
    }
}
```

### Feign Client 설정

#### ContestInternalFeignConfig

기존 `*FeignConfig` 패턴을 따르며, `OpenFeignConfig`를 `@Import`하여 공통 설정을 사용합니다.

```java
package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.ContestInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.ContestInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        ContestInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class ContestInternalFeignConfig {

    @Bean
    public ContestInternalContract contestInternalApi(ContestInternalFeignClient feignClient) {
        return new ContestInternalApi(feignClient);
    }
}
```

#### NewsInternalFeignConfig

```java
package com.tech.n.ai.client.feign.config;

import com.tech.n.ai.client.feign.domain.internal.api.NewsInternalApi;
import com.tech.n.ai.client.feign.domain.internal.client.NewsInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
        NewsInternalFeignClient.class,
})
@Import({
        OpenFeignConfig.class
})
@Configuration
public class NewsInternalFeignConfig {

    @Bean
    public NewsInternalContract newsInternalApi(NewsInternalFeignClient feignClient) {
        return new NewsInternalApi(feignClient);
    }
}
```

#### application-feign-internal.yml

```yaml
feign:
  client:
    config:
      contest-internal-api:
        url: http://localhost:8081  # api-contest 서버 URL
        connectTimeout: 5000
        readTimeout: 30000
        loggerLevel: full
      news-internal-api:
        url: http://localhost:8082  # api-news 서버 URL
        connectTimeout: 5000
        readTimeout: 30000
        loggerLevel: full

# 내부 API 키 설정
internal-api:
  contest:
    api-key: ${INTERNAL_API_CONTEST_KEY:default-contest-api-key}
  news:
    api-key: ${INTERNAL_API_NEWS_KEY:default-news-api-key}
```

### Feign Client 구현

#### ContestInternalFeignClient

```java
package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "contest-internal-api",
    url = "${feign.client.config.contest-internal-api.url}"
)
public interface ContestInternalFeignClient extends ContestInternalContract {
}
```

#### NewsInternalFeignClient

```java
package com.tech.n.ai.client.feign.domain.internal.client;

import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(
    name = "news-internal-api",
    url = "${feign.client.config.news-internal-api.url}"
)
public interface NewsInternalFeignClient extends NewsInternalContract {
}
```

### Api 구현

#### ContestInternalApi

```java
package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.client.feign.domain.internal.client.ContestInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ContestInternalApi implements ContestInternalContract {
    
    private final ContestInternalFeignClient feignClient;
    
    @Override
    public ApiResponse<ContestDetailResponse> createContestInternal(
            String apiKey,
            InternalApiDto.ContestCreateRequest request) {
        return feignClient.createContestInternal(apiKey, request);
    }
    
    @Override
    public ApiResponse<ContestBatchResponse> createContestBatchInternal(
            String apiKey,
            InternalApiDto.ContestBatchRequest request) {
        return feignClient.createContestBatchInternal(apiKey, request);
    }
}
```

#### NewsInternalApi

```java
package com.tech.n.ai.client.feign.domain.internal.api;

import com.tech.n.ai.client.feign.domain.internal.client.NewsInternalFeignClient;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NewsInternalApi implements NewsInternalContract {
    
    private final NewsInternalFeignClient feignClient;
    
    @Override
    public ApiResponse<NewsDetailResponse> createNewsInternal(
            String apiKey,
            InternalApiDto.NewsCreateRequest request) {
        return feignClient.createNewsInternal(apiKey, request);
    }
    
    @Override
    public ApiResponse<NewsBatchResponse> createNewsBatchInternal(
            String apiKey,
            InternalApiDto.NewsBatchRequest request) {
        return feignClient.createNewsBatchInternal(apiKey, request);
    }
}
```

---

## 배치 잡 통합 설계

### 배치 잡 대상 클라이언트 목록

#### Contest 데이터 수집 대상

**client-feign** (7개):
- Codeforces (기존)
- GitHub
- Kaggle
- ProductHunt
- Reddit
- HackerNews
- DevTo

**client-scraper** (5개):
- LeetCode
- Google Summer of Code
- Devpost
- MLH
- AtCoder

#### News 데이터 수집 대상

**client-feign** (4개):
- NewsAPI
- DevTo
- Reddit
- HackerNews

**client-rss** (4개):
- TechCrunch
- Google Developers Blog
- Ars Technica
- Medium Technology

### 패키지 구조 설계

#### Contest 배치 잡

```
batch-source/
  src/main/java/com/tech/n/ai/batch/source/domain/
    contest/
      codeforces/ (기존, client-feign)
        jobconfig/ContestCodeforcesApiJobConfig.java
        reader/CodeforcesApiPagingItemReader.java
        processor/CodeforcesStep1Processor.java
        writer/CodeforcesStep1Writer.java
        service/CodeforcesApiService.java
        jobparameter/ContestCodeforcesJobParameter.java
        incrementer/ContestCodeforcesIncrementer.java
      github/ (client-feign)
        jobconfig/ContestGitHubApiJobConfig.java
        reader/GitHubApiPagingItemReader.java
        processor/GitHubStep1Processor.java
        writer/GitHubStep1Writer.java
        service/GitHubApiService.java
        jobparameter/ContestGitHubJobParameter.java
        incrementer/ContestGitHubIncrementer.java
      kaggle/ (client-feign)
        jobconfig/ContestKaggleApiJobConfig.java
        ...
      leetcode/ (client-scraper)
        jobconfig/ContestLeetCodeScraperJobConfig.java
        reader/LeetCodeScrapingItemReader.java
        processor/LeetCodeStep1Processor.java
        writer/LeetCodeStep1Writer.java
        ...
      ...
    news/
      newsapi/ (client-feign)
        jobconfig/NewsNewsApiApiJobConfig.java
        reader/NewsApiPagingItemReader.java
        processor/NewsApiStep1Processor.java
        writer/NewsApiStep1Writer.java
        ...
      techcrunch/ (client-rss)
        jobconfig/NewsTechCrunchRssParserJobConfig.java
        reader/TechCrunchRssItemReader.java
        processor/TechCrunchStep1Processor.java
        writer/TechCrunchStep1Writer.java
        ...
      ...
```

### JobConfig 패턴 설계

#### 일관된 구조

1. **Job Bean**: Job 이름, Step 연결, Incrementer 설정
2. **Step Bean**: Chunk 크기, Reader, Processor, Writer 설정
3. **Reader Bean**: `*PagingItemReader` 또는 `*ItemReader` 구현
4. **Processor Bean**: Client DTO → batch-source DTO 변환
5. **Writer Bean**: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
6. **JobParameter Bean**: Job 파라미터 관리
7. **Incrementer Bean**: Job 실행 제어

#### 구현 예시 1: client-feign 클라이언트 (`*ApiJobConfig` 패턴)

```java
package com.tech.n.ai.batch.source.domain.contest.github.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.github.incrementer.ContestGitHubIncrementer;
import com.tech.n.ai.batch.source.domain.contest.github.jobparameter.ContestGitHubJobParameter;
import com.tech.n.ai.batch.source.domain.contest.github.processor.GitHubStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.github.reader.GitHubApiPagingItemReader;
import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.batch.source.domain.contest.github.writer.GitHubStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
            .<GitHubDto.Event, ContestCreateRequest>chunk(Constants.CHUNK_SIZE_10)
            .reader(step1Reader())
            .processor(step1Processor()) // Client DTO → batch-source DTO 변환
            .writer(step1Writer()) // batch-source DTO → client-feign DTO 변환 후 API 호출
            .build();
    }
    
    @Bean(name = Constants.CONTEST_GITHUB + Constants.STEP_1 + Constants.ITEM_READER)
    public GitHubApiPagingItemReader<GitHubDto.Event> step1Reader() {
        return new GitHubApiPagingItemReader<GitHubDto.Event>(
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

#### 구현 예시 2: client-rss 클라이언트 (`*RssParserJobConfig` 패턴)

```java
package com.tech.n.ai.batch.source.domain.news.techcrunch.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.news.techcrunch.incrementer.NewsTechCrunchIncrementer;
import com.tech.n.ai.batch.source.domain.news.techcrunch.jobparameter.NewsTechCrunchJobParameter;
import com.tech.n.ai.batch.source.domain.news.techcrunch.processor.TechCrunchStep1Processor;
import com.tech.n.ai.batch.source.domain.news.techcrunch.reader.TechCrunchRssItemReader;
import com.tech.n.ai.batch.source.domain.news.techcrunch.writer.TechCrunchStep1Writer;
import com.tech.n.ai.batch.source.domain.news.dto.request.NewsCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.NewsInternalContract;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

#### 구현 예시 3: client-scraper 클라이언트 (`*ScraperJobConfig` 패턴)

```java
package com.tech.n.ai.batch.source.domain.contest.leetcode.jobconfig;

import com.tech.n.ai.batch.source.common.Constants;
import com.tech.n.ai.batch.source.domain.contest.leetcode.incrementer.ContestLeetCodeIncrementer;
import com.tech.n.ai.batch.source.domain.contest.leetcode.jobparameter.ContestLeetCodeJobParameter;
import com.tech.n.ai.batch.source.domain.contest.leetcode.processor.LeetCodeStep1Processor;
import com.tech.n.ai.batch.source.domain.contest.leetcode.reader.LeetCodeScrapingItemReader;
import com.tech.n.ai.batch.source.domain.contest.leetcode.writer.LeetCodeStep1Writer;
import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

### JobConfig 클래스 이름 규칙

- **client-feign 클라이언트**: `{Domain}{Source}ApiJobConfig`
  - 예: `ContestCodeforcesApiJobConfig`, `NewsNewsApiApiJobConfig`
- **client-rss 클라이언트**: `{Domain}{Source}RssParserJobConfig`
  - 예: `NewsTechCrunchRssParserJobConfig`, `NewsGoogleDevelopersRssParserJobConfig`
- **client-scraper 클라이언트**: `{Domain}{Source}ScraperJobConfig`
  - 예: `ContestLeetCodeScraperJobConfig`, `ContestAtCoderScraperJobConfig`

---

## PagingItemReader 설계

### 패턴 설계

- `AbstractPagingItemReader` 상속
- Service를 통한 데이터 수집
- 페이징 처리 로직

### 구현 예시 (CodeforcesApiPagingItemReader 패턴 준수)

#### Feign API용 *PagingItemReader

```java
package com.tech.n.ai.batch.source.domain.contest.github.reader;

import com.tech.n.ai.batch.source.domain.contest.github.service.GitHubApiService;
import com.tech.n.ai.client.feign.domain.github.contract.GitHubDto;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

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
        
        List<GitHubDto.Event> itemList = service.getEvents();
        
        for (GitHubDto.Event item : itemList) {
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

#### RSS용 *RssItemReader

```java
package com.tech.n.ai.batch.source.domain.news.techcrunch.reader;

import com.tech.n.ai.client.rss.dto.RssFeedItem;
import com.tech.n.ai.client.rss.parser.TechCrunchRssParser;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class TechCrunchRssItemReader<T> extends AbstractPagingItemReader<T> {
    
    protected TechCrunchRssParser rssParser;
    
    public TechCrunchRssItemReader(int pageSize, TechCrunchRssParser rssParser) {
        setPageSize(pageSize);
        this.rssParser = rssParser;
    }
    
    @Override
    protected void doReadPage() {
        initResults();
        
        List<RssFeedItem> itemList = rssParser.parse();
        
        for (RssFeedItem item : itemList) {
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

#### Scraper용 *ScrapingItemReader

```java
package com.tech.n.ai.batch.source.domain.contest.leetcode.reader;

import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import com.tech.n.ai.client.scraper.scraper.LeetCodeScraper;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.util.CollectionUtils;

@Slf4j
public class LeetCodeScrapingItemReader<T> extends AbstractPagingItemReader<T> {
    
    protected LeetCodeScraper scraper;
    
    public LeetCodeScrapingItemReader(int pageSize, LeetCodeScraper scraper) {
        setPageSize(pageSize);
        this.scraper = scraper;
    }
    
    @Override
    protected void doReadPage() {
        initResults();
        
        List<ScrapedContestItem> itemList = scraper.scrape();
        
        for (ScrapedContestItem item : itemList) {
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

---

## Item Processor 설계

### DTO 변환 패턴

- **Client DTO → batch-source DTO 변환**
- Client 모듈의 DTO (예: `CodeforcesDto.Contest`, `RssFeedItem`, `ScrapedContestItem`)
- batch-source 모듈의 DTO (예: `ContestCreateRequest`, `NewsCreateRequest`)
- **중요**: batch-source 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의

### 외부 정보 제공자 공식 문서 참고 (필수)

- **각 Processor는 반드시 외부 정보 제공자의 공식 문서를 참고하여 필드 매핑 수행**
- **공식 문서만 참고**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음
- **문서 참고 방법**:
  1. `json/sources.json` 파일에서 각 출처의 `documentation_url` 확인
  2. 공식 문서에서 API 응답 필드 구조 확인
  3. Client 모듈의 DTO 필드와 공식 문서의 필드 매핑 확인
  4. batch-source 모듈의 DTO 필드에 정확히 매핑

### 주요 출처별 공식 문서 URL (sources.json 참고)

- **Codeforces**: `https://codeforces.com/apiHelp`
- **GitHub**: `https://docs.github.com/en/rest`
- **Kaggle**: `https://www.kaggle.com/docs/api`
- **Hacker News**: `https://github.com/HackerNews/API`
- **NewsAPI**: `https://newsapi.org/docs`
- **DevTo**: `https://developers.forem.com/api`
- **Reddit**: `https://www.reddit.com/dev/api`
- **ProductHunt**: `https://api.producthunt.com/v2/docs`
- **RSS 피드**: 각 출처의 RSS 피드 스펙 문서 (RSS 2.0, Atom 1.0 등)
- **Web Scraping**: 각 웹사이트의 공식 문서 또는 robots.txt

### 필드 매핑 가이드

#### 필수 필드 매핑

- `sourceId`: 출처 ID (MongoDB ObjectId)
- `title`: 제목 (대회명 또는 뉴스 제목)
- `startDate`: 시작 일시 (Contest의 경우)
- `endDate`: 종료 일시 (Contest의 경우)
- `url`: 원본 URL
- `description`: 설명

#### 선택 필드 매핑

- `metadata`: 메타데이터 (prize, participants, tags 등)
- `status`: 상태 (UPCOMING, ONGOING, ENDED 등)

#### 필드 변환 규칙

- 날짜/시간 필드: 공식 문서의 형식에 따라 적절히 변환 (ISO 8601 권장)
- 숫자 필드: null 처리 및 기본값 설정
- 문자열 필드: null 체크 및 빈 문자열 처리
- 배열 필드: null 체크 및 빈 리스트 처리

### 구현 예시

```java
package com.tech.n.ai.batch.source.domain.contest.codeforces.processor;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.codeforces.contract.CodeforcesDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;

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
@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Processor implements ItemProcessor<CodeforcesDto.Contest, ContestCreateRequest> {
    
    @Override
    public @Nullable ContestCreateRequest process(CodeforcesDto.Contest item) throws Exception {
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
            .metadata(ContestCreateRequest.ContestMetadataRequest.builder()
                .sourceName("Codeforces API")
                .tags(extractTags(item))
                .build())
            .build();
    }
    
    private String getSourceId() {
        // SourcesDocument에서 Codeforces 출처의 ID 조회
        // 구현 생략
        return "507f1f77bcf86cd799439011"; // 예시
    }
    
    private List<String> extractTags(CodeforcesDto.Contest item) {
        // type, kind 등에서 태그 추출
        List<String> tags = new ArrayList<>();
        if (item.type() != null) {
            tags.add(item.type());
        }
        if (item.kind() != null) {
            tags.add(item.kind());
        }
        return tags;
    }
}
```

---

## Item Writer 설계

### 내부 API 호출 패턴

- **batch-source DTO → client-feign DTO 변환 후 Feign Client 호출**
- Feign Client (`ContestInternalContract`, `NewsInternalContract`) 사용
- Chunk 단위로 배치 요청 (`createContestBatchInternal`, `createNewsBatchInternal`)
- 내부 API 키 헤더 설정
- 에러 처리 및 재시도 로직
- **중요**: client-feign 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의

### 구현 예시

```java
package com.tech.n.ai.batch.source.domain.contest.codeforces.writer;

import com.tech.n.ai.batch.source.domain.contest.dto.request.ContestCreateRequest;
import com.tech.n.ai.client.feign.domain.internal.contract.ContestInternalContract;
import com.tech.n.ai.client.feign.domain.internal.contract.InternalApiDto;
import com.tech.n.ai.common.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@StepScope
@RequiredArgsConstructor
public class CodeforcesStep1Writer implements ItemWriter<ContestCreateRequest> {
    
    private final ContestInternalContract contestInternalApi;
    
    @Value("${internal-api.contest.api-key}")
    private String apiKey;
    
    @Override
    public void write(Chunk<? extends ContestCreateRequest> chunk) throws Exception {
        List<? extends ContestCreateRequest> items = chunk.getItems();
        
        // batch-source DTO → client-feign DTO 변환
        List<InternalApiDto.ContestCreateRequest> feignRequests = items.stream()
            .map(item -> InternalApiDto.ContestCreateRequest.builder()
                .sourceId(item.sourceId())
                .title(item.title())
                .startDate(item.startDate())
                .endDate(item.endDate())
                .description(item.description())
                .url(item.url())
                .metadata(InternalApiDto.ContestMetadataRequest.builder()
                    .sourceName(item.metadata().sourceName())
                    .prize(item.metadata().prize())
                    .participants(item.metadata().participants())
                    .tags(item.metadata().tags())
                    .build())
                .build())
            .collect(Collectors.toList());
        
        InternalApiDto.ContestBatchRequest batchRequest = InternalApiDto.ContestBatchRequest.builder()
            .contests(feignRequests)
            .build();
        
        // 내부 API 호출 (client-feign DTO 사용)
        ApiResponse<ContestBatchResponse> response = contestInternalApi
            .createContestBatchInternal(apiKey, batchRequest);
        
        if (!response.isSuccess()) {
            log.error("Failed to create contests batch: {}", response.getMessage());
            throw new BatchWriteException("Failed to create contests batch");
        }
        
        log.info("Successfully created {} contests", response.getData().getSuccessCount());
    }
}
```

---

## 데이터 흐름 설계

### 전체 데이터 흐름

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

### DTO 변환 흐름 (중요)

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

---

## Constants 설계

### Job 이름 상수 추가

**명명 규칙**: Job 이름은 소문자와 점(.)을 사용하며, 클라이언트 타입을 구분하지 않음 (JobConfig 클래스 이름에서만 구분)

```java
package com.tech.n.ai.batch.source.common;

public class Constants {
    
    // ========== Contest Jobs (client-feign: *ApiJobConfig) ==========
    public final static String CONTEST_CODEFORCES = "contest.codeforces.job";
    public final static String CONTEST_GITHUB = "contest.github.job";
    public final static String CONTEST_KAGGLE = "contest.kaggle.job";
    public final static String CONTEST_PRODUCTHUNT = "contest.producthunt.job";
    public final static String CONTEST_REDDIT = "contest.reddit.job";
    public final static String CONTEST_HACKERNEWS = "contest.hackernews.job";
    public final static String CONTEST_DEVTO = "contest.devto.job";
    
    // ========== Contest Jobs (client-scraper: *ScraperJobConfig) ==========
    public final static String CONTEST_LEETCODE = "contest.leetcode.job";
    public final static String CONTEST_GSOC = "contest.gsoc.job";
    public final static String CONTEST_DEVPOST = "contest.devpost.job";
    public final static String CONTEST_MLH = "contest.mlh.job";
    public final static String CONTEST_ATCODER = "contest.atcoder.job";
    
    // ========== News Jobs (client-feign: *ApiJobConfig) ==========
    public final static String NEWS_NEWSAPI = "news.newsapi.job";
    public final static String NEWS_DEVTO = "news.devto.job";
    public final static String NEWS_REDDIT = "news.reddit.job";
    public final static String NEWS_HACKERNEWS = "news.hackernews.job";
    
    // ========== News Jobs (client-rss: *RssParserJobConfig) ==========
    public final static String NEWS_TECHCRUNCH = "news.techcrunch.job";
    public final static String NEWS_GOOGLE_DEVELOPERS = "news.google.developers.job";
    public final static String NEWS_ARS_TECHNICA = "news.ars.technica.job";
    public final static String NEWS_MEDIUM = "news.medium.job";
    
    // ========== 기존 상수 (유지) ==========
    public final static String PARAMETER = ".parameter";
    public final static String STEP_1 = ".step.1";
    public final static String STEP_2 = ".step.2";
    public final static String STEP_3 = ".step.3";
    public final static String STEP_4 = ".step.4";
    public final static String TASKLET = ".tasklet";
    public final static int CHUNK_SIZE_2 = 2;
    public final static int CHUNK_SIZE_5 = 5;
    public final static int CHUNK_SIZE_10 = 10;
    public final static int CHUNK_SIZE_50 = 50;
    public final static int CHUNK_SIZE_100 = 100;
    public final static int CHUNK_SIZE_300 = 300;
    public final static int CHUNK_SIZE_1000 = 1000;
    public final static int CHUNK_SIZE_2000 = 2000;
    public final static String ITEM_READER = ".item.reader";
    public final static String ITEM_PROCESSOR = ".item.processor";
    public final static String ITEM_WRITER = ".item.writer";
    public final static int GRID_SIZE_4 = 4;
    public final static String MANAGER = ".manager";
    public final static String WORKER = ".worker";
    public final static String TASK_POOL = ".task.pool";
    public final static String PARTITION_HANDLER = ".partition.handler";
    public final static String PARTITIONER = ".partitioner";
    public final static String BACKOFF_POLICY = ".backoff.policy";
}
```

---

## 에러 처리 설계

### 배치 잡 실패 처리 전략

#### 1. Item Reader 실패 처리

- **재시도 로직**: Spring Batch의 재시도 설정 활용
- **Skip 정책**: 특정 예외 발생 시 해당 항목만 스킵하고 계속 진행
- **로그 기록**: 실패한 항목에 대한 상세 로그 기록

#### 2. Item Processor 실패 처리

- **Skip 정책**: 변환 실패 시 해당 항목만 스킵
- **검증 로직**: 필수 필드 검증 실패 시 스킵
- **로그 기록**: 변환 실패 원인 기록

#### 3. Item Writer 실패 처리

- **재시도 로직**: Feign Client의 재시도 설정 활용
- **부분 실패 처리**: 배치 요청 중 일부 실패 시 성공한 항목은 유지
- **에러 응답 처리**: API 응답의 `failureMessages` 확인 및 로깅

### 내부 API 호출 실패 처리 전략

#### 1. 타임아웃 처리

- **연결 타임아웃**: 5초
- **읽기 타임아웃**: 30초
- **타임아웃 발생 시**: 재시도 또는 스킵

#### 2. 재시도 로직

- **최대 재시도 횟수**: 3회
- **재시도 간격**: 지수 백오프 (1초 → 2초 → 4초)
- **재시도 대상 예외**: `WebClientException`, `IOException`

#### 3. 에러 응답 처리

- **성공 응답 확인**: `ApiResponse.isSuccess()` 확인
- **에러 메시지 로깅**: `ApiResponse.getMessage()` 로깅
- **부분 실패 처리**: `ContestBatchResponse.failureMessages` 확인 및 로깅

### 재시도 및 알림 처리 전략

#### 1. 재시도 정책

- **Item Writer 레벨**: Feign Client의 재시도 설정 활용
- **Job 레벨**: Spring Batch의 재시도 설정 활용
- **최대 재시도 횟수**: 3회

#### 2. 알림 처리

- **실패 알림**: Slack 또는 이메일 알림 (선택사항)
- **로그 기록**: 모든 실패 사항에 대한 상세 로그 기록
- **모니터링**: 배치 잡 실행 상태 모니터링

---

## 결론

이 설계 문서는 `batch-source` 모듈의 배치 잡 통합 및 `client-feign` 모듈의 내부 API 호출을 위한 종합 설계 문서입니다. 모든 클라이언트 모듈의 데이터 수집을 위한 일관된 배치 잡 구조를 제공합니다.

### 주요 특징

1. ✅ **DTO 독립성 원칙**: 각 모듈에서 독립적으로 DTO 정의, 모듈 간 DTO 공유 금지
2. ✅ **Feign Client 내부 API 호출**: ContestInternalContract, NewsInternalContract 설계
   - ContestInternalFeignConfig, NewsInternalFeignConfig 설계 (기존 *FeignConfig 패턴 준수)
   - OpenFeignConfig를 @Import하여 공통 설정 사용
3. ✅ **배치 잡 통합**: 모든 클라이언트 모듈에 대한 JobConfig 설계
4. ✅ **JobConfig 클래스 이름 규칙**: client-feign(*ApiJobConfig), client-rss(*RssParserJobConfig), client-scraper(*ScraperJobConfig)
5. ✅ **PagingItemReader 설계**: Feign API, RSS, Scraper용 Reader 설계
6. ✅ **Item Processor 설계**: 외부 정보 제공자 공식 문서 참고 필수
7. ✅ **Item Writer 설계**: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
8. ✅ **데이터 흐름 설계**: 전체 데이터 흐름 및 DTO 변환 흐름 설계
9. ✅ **Constants 설계**: 모든 Job 이름 상수 정의
10. ✅ **에러 처리 설계**: 배치 잡 실패 및 내부 API 호출 실패 처리 전략

### 다음 단계

1. Feign Client 내부 API 호출 구현
   - ContestInternalContract, NewsInternalContract 인터페이스 구현
   - ContestInternalFeignClient, NewsInternalFeignClient 구현
   - ContestInternalApi, NewsInternalApi 구현
   - ContestInternalFeignConfig, NewsInternalFeignConfig 구현 (기존 *FeignConfig 패턴 준수)
2. InternalApiDto 구현
3. 배치 잡 통합 구현 (모든 클라이언트 모듈에 대한 JobConfig 추가)
4. PagingItemReader 구현 (Feign API, RSS, Scraper용)
5. Item Processor 구현 (외부 정보 제공자 공식 문서 참고)
6. Item Writer 구현 (내부 API 호출)
7. Constants 클래스 업데이트
8. 에러 처리 구현
9. 테스트 코드 작성

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-XX  
**작성자**: System Architect
