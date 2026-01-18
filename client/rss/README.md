# RSS Client 모듈

## 개요

`client-rss` 모듈은 RSS(Really Simple Syndication) 및 Atom 피드를 파싱하여 IT 테크 뉴스 정보를 수집하는 클라이언트 모듈입니다. Rome 라이브러리를 사용하여 RSS 2.0 및 Atom 1.0 형식을 지원하며, Spring WebClient를 통한 비동기 HTTP 요청과 Resilience4j를 통한 재시도 로직을 제공합니다.

## 주요 기능

### 1. RSS/Atom 피드 파싱
- **RSS 2.0 형식 지원**: TechCrunch, Ars Technica, Medium Technology
- **Atom 1.0 형식 지원**: Google Developers Blog
- **피드 검증**: 필수 필드 존재 여부 확인, 중복 항목 제거
- **데이터 정제**: HTML 태그 제거, 특수 문자 정규화

### 2. 대상 출처
- **Google Developers Blog** (Priority 1, total_score: 36)
  - 피드 URL: `https://developers.googleblog.com/feeds/posts/default`
  - 피드 형식: Atom 1.0
  - 업데이트 빈도: 주간
- **TechCrunch** (Priority 1, total_score: 35)
  - 피드 URL: `https://techcrunch.com/feed/`
  - 피드 형식: RSS 2.0
  - 업데이트 빈도: 일일
- **Ars Technica** (Priority 2, total_score: 34)
  - 피드 URL: `https://feeds.arstechnica.com/arstechnica/index`
  - 피드 형식: RSS 2.0
  - 업데이트 빈도: 일일
- **Medium Technology** (Priority 2, total_score: 30)
  - 피드 URL: `https://medium.com/feed/tag/technology`
  - 피드 형식: RSS 2.0
  - 업데이트 빈도: 일일

### 3. 에러 핸들링 및 재시도
- **Resilience4j 재시도**: 최대 3회, 지수 백오프 적용
- **타임아웃 처리**: 기본 30초
- **실패 시 로깅**: 에러 발생 시 상세 로그 기록

## 아키텍처

### 패키지 구조

```
com.tech.n.ai.client.rss
├── config/
│   ├── RssParserConfig.java (WebClient 빈 설정)
│   └── RssProperties.java (@ConfigurationProperties)
├── parser/
│   ├── RssParser.java (인터페이스)
│   ├── TechCrunchRssParser.java
│   ├── GoogleDevelopersBlogRssParser.java
│   ├── ArsTechnicaRssParser.java
│   └── MediumTechnologyRssParser.java
├── dto/
│   └── RssFeedItem.java (파싱된 RSS 아이템 DTO)
├── util/
│   ├── RssFeedValidator.java (피드 검증)
│   └── RssDataCleaner.java (데이터 정제)
└── exception/
    └── RssParsingException.java (파싱 예외)
```

### 설계 원칙

#### 1. 단일 책임 원칙 (SRP)
- `RssParser`: RSS 피드 파싱만 담당
- `RssFeedValidator`: 피드 검증만 담당
- `RssDataCleaner`: 데이터 정제만 담당

#### 2. 의존성 역전 원칙 (DIP)
- `RssParser` 인터페이스를 통한 추상화
- 구현체는 인터페이스에 의존

#### 3. 개방-폐쇄 원칙 (OCP)
- 새로운 RSS 출처 추가 시 기존 코드 수정 없이 새로운 Parser 구현체만 추가
- 전략 패턴을 통한 유연한 확장

### 데이터 흐름

```
외부 RSS 피드
  → WebClient (비동기 HTTP 요청)
  → Rome 라이브러리 (피드 파싱)
  → RssFeedValidator (피드 검증)
  → RssDataCleaner (데이터 정제)
  → RssFeedItem DTO
  → MongoDB Atlas 저장 (Query Side)
  → API 제공 (api-news)
```

## 기술 스택

### 의존성

- **Rome 라이브러리** (RSS/Atom 피드 파싱)
  - 공식 문서: https://rometools.github.io/rome/
  - Maven/Gradle: `com.rometools:rome:1.19.0`
  - RSS 2.0 및 Atom 1.0 형식 지원
- **Spring WebFlux** (WebClient 사용)
  - 비동기 HTTP 요청
  - Reactor 기반 논블로킹 I/O
- **Resilience4j** (재시도 로직)
  - 공식 문서: https://resilience4j.readme.io/
  - 비동기 지원, 지수 백오프 적용
  - Maven/Gradle: `io.github.resilience4j:resilience4j-spring-boot3:2.1.0`
- **Common 모듈**:
  - `common-core`: 공통 DTO 및 유틸리티
  - `common-exception`: 예외 처리

## 설정

### application-rss.yml

```yaml
rss:
  timeout-seconds: 30
  max-retries: 3
  retry-delay-ms: 1000
  sources:
    google-developers-blog:
      feed-url: https://developers.googleblog.com/feeds/posts/default
      feed-format: ATOM_1.0
      update-frequency: 주간
    techcrunch:
      feed-url: https://techcrunch.com/feed/
      feed-format: RSS_2.0
      update-frequency: 일일
    ars-technica:
      feed-url: https://feeds.arstechnica.com/arstechnica/index
      feed-format: RSS_2.0
      update-frequency: 일일
    medium-technology:
      feed-url: https://medium.com/feed/tag/technology
      feed-format: RSS_2.0
      update-frequency: 일일

resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3
        wait-duration: 1000ms
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.reactive.function.client.WebClientException
          - java.io.IOException
    instances:
      rssRetry:
        base-config: default
```

### 환경 변수

특별한 환경 변수는 필요하지 않습니다. RSS 피드는 일반적으로 공개적으로 접근 가능합니다.

## 사용 예시

### RSS 피드 파싱

```java
@Service
@RequiredArgsConstructor
public class NewsCollectionService {
    private final TechCrunchRssParser techCrunchParser;
    private final GoogleDevelopersBlogRssParser googleDevParser;
    
    public List<RssFeedItem> collectTechCrunchNews() {
        return techCrunchParser.parse();
    }
    
    public List<RssFeedItem> collectGoogleDevNews() {
        return googleDevParser.parse();
    }
}
```

### 커스텀 Parser 구현

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomRssParser implements RssParser {
    private final WebClient.Builder webClientBuilder;
    private final RssFeedValidator validator;
    private final RssProperties properties;
    private final Retry retry; // Resilience4j Retry
    
    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("custom-source");
        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        
        return retry.executeSupplier(() -> {
            String feedContent = webClient.get()
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            SyndFeed feed = new SyndFeedInput().build(new StringReader(feedContent));
            validator.validate(feed);
            
            return feed.getEntries().stream()
                .map(this::convertToRssFeedItem)
                .collect(Collectors.toList());
        });
    }
    
    @Override
    public String getSourceName() {
        return "Custom Source";
    }
    
    @Override
    public String getFeedUrl() {
        return properties.getSources().get("custom-source").getFeedUrl();
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
    RssParserConfig.class,
})
class RssTestContext {
}
```

### 테스트 예시

```java
@SpringBootTest(classes = {
    RssTestContext.class,
    RssParserConfig.class
}, properties = {
    "spring.profiles.active=local",
    "rss.sources.techcrunch.feed-url=https://techcrunch.com/feed/",
    "rss.sources.techcrunch.feed-format=RSS_2.0"
})
public class TechCrunchRssParserTest {
    @Autowired
    private TechCrunchRssParser parser;
    
    @Test
    @DisplayName("TechCrunch RSS 피드 파싱 테스트")
    void testParse() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)
        
        // when
        List<RssFeedItem> items = parser.parse();
        
        // then
        assertNotNull(items);
        assertFalse(items.isEmpty());
        items.forEach(item -> {
            assertNotNull(item.getTitle());
            assertNotNull(item.getLink());
            assertNotNull(item.getPublishedDate());
        });
    }
}
```

## 참고 문서

### 프로젝트 내부 문서

- **RSS 및 Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
- **데이터 모델 설계**: `docs/step2/2. data-model-design.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`

### 공식 문서

- [Rome 라이브러리 공식 문서](https://rometools.github.io/rome/)
- [RSS 2.0 표준 스펙](https://www.rssboard.org/rss-specification)
- [Atom 1.0 표준 스펙](https://tools.ietf.org/html/rfc4287)
- [Spring WebClient 공식 문서](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Resilience4j 공식 문서](https://resilience4j.readme.io/)
- [Google Developers Blog](https://developers.googleblog.com)
- [TechCrunch](https://techcrunch.com)
- [Ars Technica](https://arstechnica.com)
- [Medium](https://medium.com)

