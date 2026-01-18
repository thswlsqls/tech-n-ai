# Scraper Client 모듈

## 개요

`client-scraper` 모듈은 공식 API가 없어 웹 스크래핑이 필요한 출처로부터 개발자 대회 정보를 수집하는 클라이언트 모듈입니다. Jsoup을 사용한 HTML 파싱, crawler-commons를 활용한 robots.txt 확인, Spring WebClient를 통한 비동기 HTTP 요청, Resilience4j를 통한 재시도 로직을 제공합니다.

## 주요 기능

### 1. 웹 스크래핑
- **HTML 파싱**: Jsoup을 사용한 정적 HTML 파싱
- **CSS 선택자**: 데이터 추출을 위한 CSS 선택자 활용
- **GraphQL 지원**: LeetCode의 경우 GraphQL 엔드포인트 우선 사용
- **robots.txt 확인**: crawler-commons를 활용한 표준 robots.txt 파싱 및 준수
- **데이터 검증 및 정제**: 필수 필드 확인, HTML 태그 제거, 날짜 형식 정규화

### 2. 대상 출처
- **LeetCode Contests** (Priority 2, total_score: 32)
  - URL: `https://leetcode.com`
  - 데이터 형식: GraphQL/JSON (우선), HTML (대안)
  - 업데이트 빈도: 주간
- **Google Summer of Code** (Priority 2, total_score: 32)
  - URL: `https://summerofcode.withgoogle.com`
  - 데이터 형식: HTML
  - 업데이트 빈도: 연간 (프로그램 기간 중 일일)
- **Devpost** (Priority 2, total_score: 30)
  - URL: `https://devpost.com`
  - 데이터 형식: HTML
  - 업데이트 빈도: 일일
- **Major League Hacking (MLH)** (Priority 2, total_score: 29)
  - URL: `https://mlh.io`
  - 데이터 형식: HTML
  - 업데이트 빈도: 주간
- **AtCoder** (Priority 2, total_score: 28)
  - URL: `https://atcoder.jp`
  - 데이터 형식: HTML
  - 업데이트 빈도: 주간

### 3. Rate Limiting 및 법적 준수
- **Rate Limiting**: 출처별 최소 요청 간격 관리 (기본 1초)
- **robots.txt 준수**: 모든 스크래핑 전 robots.txt 확인 필수
- **User-Agent 설정**: 명확한 프로젝트 식별자 포함
- **Terms of Service (ToS) 확인**: 각 웹사이트의 ToS 확인 필요

### 4. 에러 핸들링 및 재시도
- **Resilience4j 재시도**: 최대 3회, 지수 백오프 적용
- **타임아웃 처리**: 기본 30초
- **HTML 구조 변경 감지**: 구조 변경 시 로깅 및 알림

## 아키텍처

### 패키지 구조

```
com.tech.n.ai.client.scraper
├── config/
│   ├── ScraperConfig.java (WebClient 빈 설정)
│   └── ScraperProperties.java (@ConfigurationProperties)
├── scraper/
│   ├── WebScraper.java (인터페이스)
│   ├── LeetCodeScraper.java
│   ├── GoogleSummerOfCodeScraper.java
│   ├── DevpostScraper.java
│   ├── MLHScraper.java
│   └── AtCoderScraper.java
├── dto/
│   └── ScrapedContestItem.java (스크래핑된 대회 정보 DTO)
├── util/
│   ├── RobotsTxtChecker.java (robots.txt 확인)
│   ├── ScrapedDataValidator.java (데이터 검증)
│   └── ScrapedDataCleaner.java (데이터 정제)
└── exception/
    └── ScrapingException.java (스크래핑 예외)
```

### 설계 원칙

#### 1. 단일 책임 원칙 (SRP)
- `WebScraper`: 웹 스크래핑만 담당
- `RobotsTxtChecker`: robots.txt 확인만 담당
- `ScrapedDataValidator`: 데이터 검증만 담당
- `ScrapedDataCleaner`: 데이터 정제만 담당

#### 2. 의존성 역전 원칙 (DIP)
- `WebScraper` 인터페이스를 통한 추상화
- 구현체는 인터페이스에 의존

#### 3. 개방-폐쇄 원칙 (OCP)
- 새로운 웹사이트 추가 시 기존 코드 수정 없이 새로운 Scraper 구현체만 추가
- 전략 패턴을 통한 유연한 확장

### 데이터 흐름

```
외부 웹사이트
  → RobotsTxtChecker (robots.txt 확인)
  → Rate Limiting 확인
  → WebClient (비동기 HTTP 요청)
  → Jsoup (HTML 파싱) 또는 GraphQL (JSON 파싱)
  → ScrapedDataValidator (데이터 검증)
  → ScrapedDataCleaner (데이터 정제)
  → ScrapedContestItem DTO
  → MongoDB Atlas 저장 (Query Side)
  → API 제공 (api-contest)
```

## 기술 스택

### 의존성

- **Jsoup** (HTML 파싱)
  - 공식 문서: https://jsoup.org/
  - Cookbook: https://jsoup.org/cookbook/
  - Maven/Gradle: `org.jsoup:jsoup:1.17.2`
- **crawler-commons** (robots.txt 파싱)
  - GitHub: https://github.com/crawler-commons/crawler-commons
  - Maven/Gradle: `com.github.crawler-commons:crawler-commons:1.2`
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

### application-scraper.yml

```yaml
scraper:
  timeout-seconds: 30
  max-retries: 3
  retry-delay-ms: 1000
  user-agent: "ShrimpTM-Demo/1.0 (+https://github.com/your-repo)"
  sources:
    leetcode:
      base-url: https://leetcode.com
      data-format: GraphQL
      min-interval-seconds: 1
      requires-selenium: false
    google-summer-of-code:
      base-url: https://summerofcode.withgoogle.com
      data-format: HTML
      min-interval-seconds: 1
      requires-selenium: false
    devpost:
      base-url: https://devpost.com
      data-format: HTML
      min-interval-seconds: 1
      requires-selenium: false
    mlh:
      base-url: https://mlh.io
      data-format: HTML
      min-interval-seconds: 1
      requires-selenium: false
    atcoder:
      base-url: https://atcoder.jp
      data-format: HTML
      min-interval-seconds: 1
      requires-selenium: false

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
          - org.jsoup.HttpStatusException
    instances:
      scraperRetry:
        base-config: default
```

### 환경 변수

특별한 환경 변수는 필요하지 않습니다. 다만, Rate Limiting을 위해 Redis가 필요할 수 있습니다.

## 사용 예시

### 웹 스크래핑

```java
@Service
@RequiredArgsConstructor
public class ContestCollectionService {
    private final DevpostScraper devpostScraper;
    private final LeetCodeScraper leetCodeScraper;
    
    public List<ScrapedContestItem> collectDevpostContests() {
        return devpostScraper.scrape();
    }
    
    public List<ScrapedContestItem> collectLeetCodeContests() {
        return leetCodeScraper.scrape();
    }
}
```

### 커스텀 Scraper 구현

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class CustomScraper implements WebScraper {
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final ScrapedDataValidator validator;
    private final ScrapedDataCleaner cleaner;
    private final ScraperProperties properties;
    private final Retry retry; // Resilience4j Retry
    
    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("custom-source");
        
        // robots.txt 확인
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), "/contests")) {
            throw new ScrapingException("Scraping not allowed by robots.txt");
        }
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        
        return retry.executeSupplier(() -> {
            try {
                // WebClient로 HTML 가져오기
                String html = webClient.get()
                    .uri("/contests")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Jsoup으로 HTML 파싱
                Document doc = Jsoup.parse(html);
                List<ScrapedContestItem> items = doc.select(".contest-item").stream()
                    .map(this::extractContestInfo)
                    .collect(Collectors.toList());
                
                // 데이터 검증 및 정제
                items = validator.validate(items);
                items = cleaner.clean(items);
                
                return items;
            } catch (Exception e) {
                log.error("Failed to scrape custom source", e);
                throw new ScrapingException("Custom source scraping failed", e);
            }
        });
    }
    
    @Override
    public String getSourceName() {
        return "Custom Source";
    }
    
    @Override
    public String getBaseUrl() {
        return properties.getSources().get("custom-source").getBaseUrl();
    }
}
```

## 법적/윤리적 고려사항

### 1. robots.txt 준수 (필수)
- **확인 시점**: 모든 웹 스크래핑 전 필수 확인
- **구현 방법**: crawler-commons 라이브러리를 사용한 표준 파싱
- **준수 사항**:
  - Disallow 경로는 절대 스크래핑하지 않음
  - Crawl-delay 지시사항이 있으면 해당 간격 준수
  - User-Agent별 규칙 확인

### 2. Terms of Service (ToS) 확인 (필수)
- 각 웹사이트의 ToS 페이지 확인
- 스크래핑 관련 조항 검색
- 명시적으로 금지된 경우 해당 출처 제외
- 불명확한 경우 보수적으로 접근

### 3. Rate Limiting (필수)
- **기본 정책**: 최소 1초 간격 (출처별 설정 가능)
- **구현 방법**: Redis를 활용한 분산 환경에서의 Rate Limiting
- **Crawl-delay 준수**: robots.txt에 Crawl-delay가 명시된 경우 해당 값 준수

### 4. User-Agent 설정 (권장)
- **형식**: `프로젝트명/버전 (연락처 정보)`
- **예시**: `ShrimpTM-Demo/1.0 (+https://github.com/your-repo)`

## 테스트

### 테스트 컨텍스트

```java
@ImportAutoConfiguration({
    WebFluxAutoConfiguration.class,
})
@Import({
    ScraperConfig.class,
})
class ScraperTestContext {
}
```

### 테스트 예시

```java
@SpringBootTest(classes = {
    ScraperTestContext.class,
    ScraperConfig.class
}, properties = {
    "spring.profiles.active=local",
    "scraper.sources.devpost.base-url=https://devpost.com",
    "scraper.sources.devpost.data-format=HTML",
    "scraper.sources.devpost.min-interval-seconds=1"
})
public class DevpostScraperTest {
    @Autowired
    private DevpostScraper scraper;
    
    @Test
    @DisplayName("Devpost 웹 스크래핑 테스트")
    void testScrape() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)
        // robots.txt 확인 및 Rate Limiting은 내부적으로 처리됨
        
        // when
        List<ScrapedContestItem> items = scraper.scrape();
        
        // then
        assertNotNull(items);
        items.forEach(item -> {
            assertNotNull(item.getTitle());
            assertNotNull(item.getUrl());
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

- [Jsoup 공식 문서](https://jsoup.org/)
- [Jsoup Cookbook](https://jsoup.org/cookbook/)
- [crawler-commons GitHub](https://github.com/crawler-commons/crawler-commons)
- [robots.txt 표준 (RFC 9309)](https://www.rfc-editor.org/rfc/rfc9309.html)
- [robots.txt.org](https://www.robotstxt.org/)
- [Spring WebClient 공식 문서](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Resilience4j 공식 문서](https://resilience4j.readme.io/)
- [LeetCode](https://leetcode.com)
- [Google Summer of Code](https://summerofcode.withgoogle.com)
- [Devpost](https://devpost.com)
- [Major League Hacking (MLH)](https://mlh.io)
- [AtCoder](https://atcoder.jp)

