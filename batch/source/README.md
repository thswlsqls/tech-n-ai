# Batch Source 모듈

## 개요

`batch-source` 모듈은 외부 출처(API, RSS, Web Scraping)로부터 Contest 및 News 데이터를 수집하여 내부 API를 통해 저장하는 Spring Batch 기반 배치 처리 모듈입니다. 다양한 클라이언트 모듈(`client-feign`, `client-rss`, `client-scraper`)을 활용하여 데이터를 수집하고, 내부 API(`api-contest`, `api-news`)를 통해 MongoDB Atlas에 저장합니다.

## 주요 기능

### 1. 데이터 수집 배치 잡

#### Contest 데이터 수집 (12개 출처)

**client-feign 기반 (7개)**:
- Codeforces
- GitHub
- Kaggle
- ProductHunt
- Reddit
- HackerNews
- DevTo

**client-scraper 기반 (5개)**:
- LeetCode
- Google Summer of Code
- Devpost
- MLH
- AtCoder

#### News 데이터 수집 (8개 출처)

**client-feign 기반 (4개)**:
- NewsAPI
- DevTo
- Reddit
- HackerNews

**client-rss 기반 (4개)**:
- TechCrunch
- Google Developers Blog
- Ars Technica
- Medium Technology

#### Emerging Tech 데이터 수집 (5개 Provider)

AI 서비스 업데이트를 자동 수집하는 배치 잡입니다. 수집된 데이터는 `api-emerging-tech` 내부 API를 통해 MongoDB Atlas에 저장됩니다.

| 배치 잡 | 수집 방식 | 대상 Provider |
|---------|----------|---------------|
| `emerging-tech.github.job` | GitHub API (client-feign) | OpenAI, Anthropic, Google, Meta, xAI |
| `emerging-tech.rss.job` | RSS 파싱 (client-rss) | OpenAI, Google |
| `emerging-tech.scraper.job` | 웹 크롤링 (client-scraper) | Anthropic, Meta, xAI |

**대상 GitHub 저장소**:
- OpenAI: `openai/openai-python`
- Anthropic: `anthropics/anthropic-sdk-python`
- Google: `google/generative-ai-python`
- Meta: `facebookresearch/llama`
- xAI: `xai-org/grok-1`

**저장 구조**: MongoDB Atlas `emerging_techs` 컬렉션 (CQRS 없이 단일 저장소)

**`api-agent` 모듈과의 연관성**: `api-agent` 모듈의 데이터 수집 Tool(`collect_github_releases`, `collect_rss_feeds`, `collect_scraped_articles`)이 배치 Processor 로직을 `DataCollectionProcessorUtil`로 공유하여 재사용합니다.

### 2. 배치 처리 아키텍처

각 배치 잡은 Spring Batch의 표준 구조를 따릅니다:

- **Item Reader**: 외부 출처로부터 데이터 읽기
  - `*PagingItemReader`: Feign API 기반 데이터 수집
  - `*RssItemReader`: RSS 피드 파싱
  - `*ScrapingItemReader`: Web Scraping 데이터 수집
- **Item Processor**: Client DTO → batch-source DTO 변환
- **Item Writer**: batch-source DTO → client-feign DTO 변환 후 내부 API 호출

### 3. DTO 독립성 원칙

각 모듈은 독립적으로 DTO를 정의합니다:

1. **Client 모듈 DTO**: 외부 출처의 원본 데이터 구조
2. **batch-source 모듈 DTO**: 배치 처리용 중간 DTO (`ContestCreateRequest`, `NewsCreateRequest`)
3. **client-feign 모듈 DTO**: 내부 API 호출용 DTO (`InternalApiDto.ContestCreateRequest`, `InternalApiDto.NewsCreateRequest`)
4. **api-contest/api-news 모듈 DTO**: API 엔드포인트용 DTO

**중요**: 필드가 동일하더라도 각 모듈에서 별도로 정의하여 모듈 간 의존성을 최소화합니다.

## 아키텍처

### 배치 잡 구조

```
외부 출처 (API/RSS/Web)
  ↓
client-feign / client-rss / client-scraper
  ↓ (외부 요청, 데이터 수집 및 정제)
  Client DTO
  ↓
Batch 모듈 (batch-source)
  ├─ Item Reader: client/* 모듈의 수집 데이터 읽기
  ├─ Item Processor: Client DTO → batch-source DTO 변환
  └─ Item Writer: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
      ↓
      ContestInternalContract / NewsInternalContract (Feign Client)
      ↓
      HTTP 요청
  ↓
api-contest / api-news
  ↓ (MongoDB 저장)
MongoDB Atlas (ContestDocument / NewsArticleDocument)
```

### JobConfig 클래스 이름 규칙

- **client-feign 클라이언트**: `{Domain}{Source}ApiJobConfig`
  - 예: `ContestCodeforcesApiJobConfig`, `NewsNewsApiApiJobConfig`
- **client-rss 클라이언트**: `{Domain}{Source}RssParserJobConfig`
  - 예: `NewsTechCrunchRssParserJobConfig`, `NewsGoogleDevelopersRssParserJobConfig`
- **client-scraper 클라이언트**: `{Domain}{Source}ScraperJobConfig`
  - 예: `ContestLeetCodeScraperJobConfig`, `ContestAtCoderScraperJobConfig`

### 패키지 구조

```
batch-source/
  src/main/java/com/tech/n/ai/batch/source/
    BatchSourceApplication.java
    config/
      BatchConfig.java
      ServerConfig.java
    common/
      Constants.java
      reader/
        QuerydslPagingItemReader.java
        QuerydslZeroPagingItemReader.java
      incrementer/
        UniqueRunIdIncrementer.java
      utils/
        CodeVal.java
    domain/
      contest/
        {source}/
          jobconfig/
            Contest{Source}ApiJobConfig.java
            Contest{Source}ScraperJobConfig.java
          reader/
            {Source}ApiPagingItemReader.java
            {Source}ScrapingItemReader.java
          processor/
            {Source}Step1Processor.java
          writer/
            {Source}Step1Writer.java
          service/
            {Source}ApiService.java
          jobparameter/
            Contest{Source}JobParameter.java
          incrementer/
            Contest{Source}Incrementer.java
      news/
        {source}/
          jobconfig/
            News{Source}ApiJobConfig.java
            News{Source}RssParserJobConfig.java
          reader/
            {Source}ApiPagingItemReader.java
            {Source}RssItemReader.java
          processor/
            {Source}Step1Processor.java
          writer/
            {Source}Step1Writer.java
          service/
            {Source}ApiService.java
          jobparameter/
            News{Source}JobParameter.java
          incrementer/
            News{Source}Incrementer.java
      contest/dto/
        request/
          ContestCreateRequest.java
          ContestBatchRequest.java
      news/dto/
        request/
          NewsCreateRequest.java
          NewsBatchRequest.java
```

## 기술 스택

### 의존성

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Batch**: 배치 처리 프레임워크
- **Spring Cloud OpenFeign**: 내부 API 호출
- **Common 모듈**:
  - `common-core`: 공통 DTO 및 유틸리티
  - `common-security`: 보안 설정
  - `common-kafka`: Kafka 이벤트 발행/수신
- **Domain 모듈**:
  - `domain-aurora`: Aurora MySQL 엔티티 및 Repository
  - `domain-mongodb`: MongoDB Document 및 Repository
- **Client 모듈**:
  - `client-feign`: Feign Client 기반 외부 API 호출
  - `client-rss`: RSS 피드 파싱
  - `client-scraper`: Web Scraping
- **API 모듈**:
  - `api-contest`: Contest 내부 API
  - `api-news`: News 내부 API

### 데이터베이스

- **Aurora MySQL**: 배치 메타데이터 저장 (Spring Batch Job Repository)
- **MongoDB Atlas**: Contest/News 데이터 저장 (내부 API를 통해 간접 저장)

## 설정

### application.yml

```yaml
spring:
  application:
    name: batch-source
  profiles:
    include:
      - common-core
      - batch-domain
  batch:
    job:
      enabled: true
      name: ${job.name:NONE}
```

### application-local.yml

```yaml
spring:
  config.activate.on-profile: local
  main.web-application-type: none
  batch:
    job:
      enabled: true
      name: ${job.name:NONE}
```

### 내부 API 설정

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

### 환경 변수

- `AURORA_URL`: Aurora MySQL 연결 URL
- `AURORA_USERNAME`: Aurora MySQL 사용자명
- `AURORA_PASSWORD`: Aurora MySQL 비밀번호
- `MONGODB_ATLAS_URI`: MongoDB Atlas 연결 URI (Domain 모듈에서 사용)
- `MONGODB_DATABASE`: MongoDB 데이터베이스 이름
- `INTERNAL_API_CONTEST_KEY`: Contest 내부 API 키
- `INTERNAL_API_NEWS_KEY`: News 내부 API 키

## 배치 잡 실행

### 로컬 실행

```bash
# 특정 Job 실행
java -jar batch-source.jar --spring.profiles.active=local --job.name=contest.codeforces.job

# NewsAPI Job 실행
java -jar batch-source.jar --spring.profiles.active=local --job.name=news.newsapi.job
```

### Jenkins 연동

Jenkins Pipeline을 통해 배치 잡을 스케줄링하고 실행할 수 있습니다.

## 에러 처리

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

## 주요 상수

### Job 이름 상수

`Constants` 클래스에 모든 Job 이름이 정의되어 있습니다:

```java
// Contest Jobs (client-feign)
public final static String CONTEST_CODEFORCES = "contest.codeforces.job";
public final static String CONTEST_GITHUB = "contest.github.job";
// ... 기타

// Contest Jobs (client-scraper)
public final static String CONTEST_LEETCODE = "contest.leetcode.job";
// ... 기타

// News Jobs (client-feign)
public final static String NEWS_NEWSAPI = "news.newsapi.job";
// ... 기타

// News Jobs (client-rss)
public final static String NEWS_TECHCRUNCH = "news.techcrunch.job";
// ... 기타
```

### Chunk Size 상수

```java
public final static int CHUNK_SIZE_2 = 2;
public final static int CHUNK_SIZE_5 = 5;
public final static int CHUNK_SIZE_10 = 10;
public final static int CHUNK_SIZE_50 = 50;
public final static int CHUNK_SIZE_100 = 100;
// ... 기타
```

## 구현 가이드

### 새로운 배치 잡 추가

1. **JobConfig 클래스 생성**: `{Domain}{Source}{Type}JobConfig` 패턴 준수
2. **Reader 구현**: `AbstractPagingItemReader` 상속 또는 적절한 Reader 구현
3. **Processor 구현**: Client DTO → batch-source DTO 변환
4. **Writer 구현**: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
5. **JobParameter 구현**: Job 파라미터 관리
6. **Incrementer 구현**: Job 실행 제어
7. **Constants 추가**: Job 이름 상수 추가

### 외부 정보 제공자 공식 문서 참고

각 Processor는 반드시 외부 정보 제공자의 공식 문서를 참고하여 필드 매핑을 수행해야 합니다:

1. `json/sources.json` 파일에서 각 출처의 `documentation_url` 확인
2. 공식 문서에서 API 응답 필드 구조 확인
3. Client 모듈의 DTO 필드와 공식 문서의 필드 매핑 확인
4. batch-source 모듈의 DTO 필드에 정확히 매핑

**중요**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않으며, 공식 문서만 참고합니다.

## 참고 문서

### 프로젝트 내부 문서

- **Emerging Tech 데이터 수집 파이프라인 설계서**: `docs/reference/automation-pipeline-to-ai-agent/phase1-data-pipeline-design.md`
- **배치 잡 통합 설계서**: `docs/step10/batch-job-integration-design.md`
- **Contest 및 News API 설계서**: `docs/step9/contest-news-api-design.md`
- **RSS/Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
- **Aurora 스키마 설계**: `docs/step1/3. aurora-schema-design.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
- **에러 처리 전략 설계**: `docs/step2/4. error-handling-strategy-design.md`

### 공식 문서

- [Spring Batch 공식 문서](https://docs.spring.io/spring-batch/reference/)
- [Spring Cloud OpenFeign 공식 문서](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Spring Data JPA 공식 문서](https://spring.io/projects/spring-data-jpa)
- [Spring Data MongoDB 공식 문서](https://spring.io/projects/spring-data-mongodb)
- [MySQL 공식 문서](https://dev.mysql.com/doc/)
- [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)

### 외부 정보 제공자 공식 문서

각 출처의 공식 문서는 `json/sources.json` 파일의 `documentation_url` 필드를 참고하세요:

- **Codeforces**: https://codeforces.com/apiHelp
- **GitHub**: https://docs.github.com/en/rest
- **Kaggle**: https://www.kaggle.com/docs/api
- **Hacker News**: https://github.com/HackerNews/API
- **NewsAPI**: https://newsapi.org/docs
- **DevTo**: https://developers.forem.com/api
- **Reddit**: https://www.reddit.com/dev/api
- **ProductHunt**: https://api.producthunt.com/v2/docs
- **RSS 피드**: 각 출처의 RSS 피드 스펙 문서 (RSS 2.0, Atom 1.0 등)
- **Web Scraping**: 각 웹사이트의 공식 문서 또는 robots.txt

