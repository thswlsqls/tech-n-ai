# Feign Client 모듈

## 개요

`client-feign` 모듈은 OpenFeign을 사용하여 외부 API 및 내부 API를 호출하는 클라이언트 모듈입니다. 다양한 외부 서비스(Codeforces, DevTo, GitHub, HackerNews, Kaggle, NewsAPI, ProductHunt, Reddit 등)와 OAuth Provider(Google, Kakao, Naver) API를 통합하며, 내부 API(Contest, News) 호출도 지원합니다.

## 주요 기능

### 1. 외부 API 통합
- **대회 정보 API**: Codeforces, Kaggle
- **뉴스 API**: DevTo, HackerNews, NewsAPI, ProductHunt, Reddit
- **소셜 플랫폼 API**: GitHub
- **OAuth Provider API**: Google, Kakao, Naver

### 2. 내부 API 통합
- **Contest Internal API**: `api-contest` 모듈의 내부 API 호출
- **News Internal API**: `api-news` 모듈의 내부 API 호출

### 3. Contract 패턴
- **Contract 인터페이스**: 비즈니스 로직 인터페이스 정의
- **Feign Client 인터페이스**: OpenFeign 기반 HTTP 클라이언트
- **API 구현체**: Contract 구현체로 Feign Client 사용
- **Mock 구현**: 테스트 및 개발 환경용 Mock 구현체 제공

### 4. 환경별 설정
- **Mock 모드**: 테스트 환경에서 Mock 구현체 사용
- **REST 모드**: 실제 외부 API 호출
- **프로파일별 설정**: local, dev, beta, prod 환경별 타임아웃 및 연결 설정

## 아키텍처

### 패키지 구조

```
com.tech.n.ai.client.feign
├── config/
│   ├── OpenFeignConfig.java (전역 설정)
│   ├── CodeforcesFeignConfig.java
│   ├── DevToFeignConfig.java
│   ├── GitHubFeignConfig.java
│   ├── HackerNewsFeignConfig.java
│   ├── KaggleFeignConfig.java
│   ├── NewsAPIFeignConfig.java
│   ├── ProductHuntFeignConfig.java
│   ├── RedditFeignConfig.java
│   ├── ContestInternalFeignConfig.java
│   ├── NewsInternalFeignConfig.java
│   └── OAuthFeignConfig.java
└── domain/
    ├── codeforces/
    │   ├── api/ (Contract 구현체)
    │   ├── client/ (Feign Client 인터페이스)
    │   ├── contract/ (Contract 인터페이스 및 DTO)
    │   └── mock/ (Mock 구현)
    ├── devto/
    ├── github/
    ├── hackernews/
    ├── kaggle/
    ├── newsapi/
    ├── producthunt/
    ├── reddit/
    ├── internal/
    │   ├── api/
    │   ├── client/
    │   └── contract/
    └── oauth/
        ├── api/
        ├── client/
        ├── config/
        ├── contract/
        └── mock/
```

### 설계 패턴

#### 1. Contract 패턴
비즈니스 로직 인터페이스와 Feign Client 인터페이스를 분리하여 의존성 역전 원칙(DIP)을 준수합니다.

```java
// Contract 인터페이스 (비즈니스 로직)
public interface CodeforcesContract {
    CodeforcesDto.ContestResponse getContests(CodeforcesDto.ContestRequest request);
}

// Feign Client 인터페이스 (HTTP 클라이언트)
@FeignClient(name = "codeforces-api", url = "${feign-clients.codeforces.uri}")
public interface CodeforcesFeignClient extends CodeforcesContract {
}

// API 구현체 (Contract 구현)
public class CodeforcesApi implements CodeforcesContract {
    private final CodeforcesFeignClient feignClient;
    // ...
}
```

#### 2. 조건부 빈 생성
프로파일 및 설정에 따라 Mock 또는 실제 API 구현체를 선택적으로 주입합니다.

```java
@Bean
@ConditionalOnProperty(name = "feign-clients.codeforces.mode", havingValue = "mock")
public CodeforcesContract codeforcesMock() {
    return new CodeforcesMock();
}

@Bean
@ConditionalOnProperty(name = "feign-clients.codeforces.mode", havingValue = "rest")
public CodeforcesContract codeforcesApi(CodeforcesFeignClient feignClient) {
    return new CodeforcesApi(feignClient);
}
```

#### 3. DTO 정의
Record 클래스를 사용한 불변 DTO로 데이터 전송 객체를 정의합니다.

```java
public class CodeforcesDto {
    @Builder
    public record ContestRequest(String apiKey) {}
    
    @Builder
    public record ContestResponse(List<Contest> contests) {}
}
```

## 기술 스택

### 의존성

- **Spring Cloud OpenFeign**: HTTP 클라이언트 프레임워크
  - 공식 문서: https://spring.io/projects/spring-cloud-openfeign
- **Common 모듈**:
  - `common-core`: 공통 DTO 및 유틸리티
  - `common-kafka`: Kafka 이벤트 발행/수신
  - `common-exception`: 예외 처리
- **Domain 모듈**:
  - `domain-aurora`: Aurora MySQL 엔티티 및 Repository
  - `domain-mongodb`: MongoDB Document 및 Repository
- **API 모듈**:
  - `api-contest`: Contest API 모듈
  - `api-news`: News API 모듈

### HTTP 클라이언트 설정

프로파일별로 HTTP 클라이언트 설정이 다릅니다:

- **local/dev/beta**: 기본 타임아웃 설정 (30초)
- **prod**: OkHttp 활성화, 연결 풀 최적화 (최대 35,000 연결)

## 설정

### application-feign-{domain}.yml

각 도메인별 설정 파일이 존재합니다:

```yaml
feign-clients:
  codeforces:
    mode: rest  # mock 또는 rest
    uri: https://codeforces.com

---
spring:
  config.activate.on-profile: test
feign-clients:
  codeforces:
    mode: mock

---
spring:
  config.activate.on-profile: local, dev
  cloud:
    openfeign:
      client:
        config:
          CodeforcesFeign:
            readTimeout: 30000
            connectTimeout: 3000

---
spring:
  config.activate.on-profile: prod
  cloud:
    openfeign:
      client:
        okhttp:
          enabled: true
        httpclient:
          max-connections: 35000
          max-connections-per-route: 35000
          connection-timeout: 120000
```

### 환경 변수

각 외부 API별로 필요한 환경 변수를 설정합니다:

- `CODEFORCES_FEIGN_URI`: Codeforces API URI (선택사항)
- `DEVTO_API_KEY`: DevTo API Key
- `GITHUB_API_KEY`: GitHub API Key
- `NEWSAPI_API_KEY`: NewsAPI API Key
- `KAGGLE_USERNAME`: Kaggle 사용자명
- `KAGGLE_KEY`: Kaggle API Key
- `REDDIT_CLIENT_ID`: Reddit Client ID
- `REDDIT_CLIENT_SECRET`: Reddit Client Secret
- `GOOGLE_CLIENT_ID`: Google OAuth Client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth Client Secret
- `KAKAO_CLIENT_ID`: Kakao OAuth Client ID
- `KAKAO_CLIENT_SECRET`: Kakao OAuth Client Secret
- `NAVER_CLIENT_ID`: Naver OAuth Client ID
- `NAVER_CLIENT_SECRET`: Naver OAuth Client Secret

## 사용 예시

### 외부 API 호출

```java
@Service
@RequiredArgsConstructor
public class ContestService {
    private final CodeforcesContract codeforcesContract;
    
    public List<Contest> getCodeforcesContests() {
        CodeforcesDto.ContestRequest request = CodeforcesDto.ContestRequest.builder()
            .apiKey(apiKey)
            .build();
        
        CodeforcesDto.ContestResponse response = codeforcesContract.getContests(request);
        return response.contests();
    }
}
```

### 내부 API 호출

```java
@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsInternalContract newsInternalContract;
    
    public NewsArticle getNewsArticle(String id) {
        NewsInternalDto.NewsRequest request = NewsInternalDto.NewsRequest.builder()
            .id(id)
            .build();
        
        return newsInternalContract.getNewsArticle(request);
    }
}
```

## 테스트

### 테스트 컨텍스트

```java
@ImportAutoConfiguration({
    WebFluxAutoConfiguration.class,
})
@Import({
    CodeforcesFeignConfig.class,
})
class FeignTestContext {
}
```

### 테스트 예시

```java
@SpringBootTest(classes = {
    FeignTestContext.class,
    CodeforcesFeignConfig.class
}, properties = {
    "spring.profiles.active=test",
    "feign-clients.codeforces.mode=mock"
})
public class CodeforcesFeignTest {
    @Autowired
    private CodeforcesContract codeforcesContract;
    
    @Test
    @DisplayName("Codeforces API 호출 테스트")
    void testGetContests() {
        // given
        CodeforcesDto.ContestRequest request = CodeforcesDto.ContestRequest.builder()
            .apiKey("test-key")
            .build();
        
        // when
        CodeforcesDto.ContestResponse response = codeforcesContract.getContests(request);
        
        // then
        assertNotNull(response);
    }
}
```

## 참고 문서

### 프로젝트 내부 문서

- **Client 모듈 구현**: `docs/step8/README.md`
- **Slack 연동 설계 가이드**: `docs/step8/slack-integration-design-guide.md` (Contract 패턴 참고)
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **에러 처리 전략 설계**: `docs/step2/4. error-handling-strategy-design.md`

### 공식 문서

- [Spring Cloud OpenFeign 공식 문서](https://spring.io/projects/spring-cloud-openfeign)
- [OpenFeign GitHub](https://github.com/OpenFeign/feign)
- [Spring Framework 공식 문서](https://spring.io/projects/spring-framework)

