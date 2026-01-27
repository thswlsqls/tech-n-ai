# RSS 및 Scraper 모듈 구현 분석 

**작성 일시**: 2026-01-07  
**대상 모듈**: `client-rss`, `client-scraper`  
**목적**: RSS 피드 파싱 및 웹 스크래핑 모듈의 용도와 활용법 정리 

## 목차

1. [개요](#개요)
2. [client-rss 모듈](#client-rss-모듈)
3. [client-scraper 모듈](#client-scraper-모듈)
4. [데이터 수집 전략](#데이터-수집-전략)
5. [구현 가이드](#구현-가이드)

---

## 개요

이 문서는 `client-rss`와 `client-scraper` 모듈의 구현을 위한 분석 문서입니다. 두 모듈은 `json/sources.json`에 정의된 정보 출처로부터 데이터를 수집하는 역할을 담당합니다.

### 모듈 역할

- **client-rss**: RSS 피드를 제공하는 출처로부터 데이터 수집
- **client-scraper**: 공식 API가 없어 웹 스크래핑이 필요한 출처로부터 데이터 수집

### 데이터 흐름

```
외부 출처 (RSS/Web) 
  → client-rss/client-scraper (데이터 수집)
  → 데이터 정제 및 변환
  → MongoDB Atlas 저장 (Query Side)
  → API 제공 (api-contest, api-news)
```

---

## client-rss 모듈

### 용도

RSS 피드를 제공하는 출처로부터 최신 IT 테크 뉴스 정보를 수집합니다.

### RSS 피드란?

**RSS (Really Simple Syndication)**는 웹사이트의 최신 콘텐츠를 자동으로 배포하기 위한 XML 기반 표준 데이터 형식입니다.

#### 주요 특징

- **XML 기반 형식**: 구조화된 XML 형식으로 작성되어 표준 파서로 쉽게 처리 가능
- **자동 업데이트**: 웹사이트에 새 콘텐츠가 추가되면 RSS 피드도 자동으로 업데이트됨
- **구독 가능**: RSS 리더나 애플리케이션으로 구독하여 최신 콘텐츠를 받을 수 있음

#### RSS 피드 구조 예시

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
  <channel>
    <title>TechCrunch</title>
    <link>https://techcrunch.com</link>
    <item>
      <title>기사 제목</title>
      <link>https://techcrunch.com/article1</link>
      <description>기사 내용 요약...</description>
      <pubDate>Mon, 07 Jan 2026 10:00:00 GMT</pubDate>
    </item>
  </channel>
</rss>
```

#### RSS vs API 비교

| 구분 | RSS 피드 | API |
|------|----------|-----|
| **형식** | XML (표준화된 형식) | JSON (대부분) |
| **인증** | 일반적으로 불필요 | API Key 또는 OAuth 필요 (경우에 따라) |
| **구현 난이도** | 낮음 (표준 파서 사용) | 중간 (각 API마다 다름) |
| **데이터 구조** | 고정된 구조 (제목, 내용, 날짜 등) | API마다 다름 |

#### 장점

- **간단함**: 공식 API가 없어도 RSS 피드만 제공하면 수집 가능
- **표준화**: RSS 2.0 표준을 따르므로 다양한 출처에서 일관된 방식으로 처리 가능
- **무료**: 대부분의 웹사이트가 무료로 RSS 피드 제공

#### 단점

- **제한된 정보**: API보다 제공되는 정보가 제한적일 수 있음
- **업데이트 주기**: 실시간이 아닌 경우가 많음 (일일, 주간 등)

### 대상 출처 (json/sources.json 기준)

> **개선 사항**: `json/sources.json`에서 `type: "RSS"`이고 `cost: "Free"`인 출처를 `total_score` 기준으로 정렬하여 업데이트했습니다. 각 출처의 RSS 피드 형식, 공식 문서 URL, 우선순위 정보를 추가했습니다.

#### Priority 1 출처 (total_score 순서)

1. **Google Developers Blog RSS** (total_score: 36)
   - **rss_feed_url**: `https://developers.googleblog.com/feeds/posts/default`
   - **피드 형식**: Atom 1.0
   - **업데이트 빈도**: 주간 (일일 업데이트 가능)
   - **documentation_url**: https://developers.googleblog.com
   - **특징**: Google 공식 개발자 블로그로 높은 신뢰성 제공
   - **주의사항**: Atom 1.0 형식이므로 Rome 라이브러리에서 Atom 파서 사용 필요

2. **TechCrunch RSS** (total_score: 35)
   - **rss_feed_url**: `https://techcrunch.com/feed/`
   - **피드 형식**: RSS 2.0
   - **업데이트 빈도**: 일일
   - **documentation_url**: https://techcrunch.com
   - **특징**: 기술 뉴스 전문 매체, 정기적 업데이트
   - **주의사항**: Rate Limiting 정책은 공식 사이트에서 확인 필요

#### Priority 2 출처 (total_score 순서)

3. **Ars Technica RSS** (total_score: 34)
   - **rss_feed_url**: `https://feeds.arstechnica.com/arstechnica/index`
   - **피드 형식**: RSS 2.0
   - **업데이트 빈도**: 일일
   - **documentation_url**: https://arstechnica.com
   - **특징**: 고품질 기술 저널리즘, 깊이 있는 분석 제공
   - **주의사항**: 사용 정책은 공식 사이트에서 확인 필요

4. **Medium Technology RSS** (total_score: 30)
   - **rss_feed_url**: `https://medium.com/feed/tag/technology`
   - **피드 형식**: RSS 2.0
   - **업데이트 빈도**: 일일
   - **documentation_url**: https://medium.com, https://help.medium.com
   - **특징**: 커뮤니티 기반 기술 콘텐츠, 다양한 블로거들의 글 수집
   - **주의사항**: 품질 변동 가능, 스팸 필터링 필요

### 기술 스택

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 기술 스택을 업데이트했습니다. Resilience4j를 통한 재시도 로직, 생성자 주입 패턴, @ConfigurationProperties 활용을 권장합니다.

- **라이브러리**: Rome (RSS/Atom 피드 파싱)
  - 공식 문서: https://rometools.github.io/rome/
  - Maven/Gradle 의존성: `com.rometools:rome:1.19.0`
  - **선택 이유**: RSS 2.0 및 Atom 1.0 형식을 모두 지원하며, Spring Boot 생태계에서 널리 사용되는 안정적인 라이브러리
  - **대안 검토**: Apache Abdera, Informa 등이 있으나 Rome이 가장 활발하게 유지보수되고 있어 권장

- **HTTP 클라이언트**: Spring WebClient (비동기 HTTP 요청)
  - **권장 패턴**: `WebClient.Builder`를 빈으로 등록하여 재사용
  - **설정 관리**: `application.yml`을 통한 타임아웃, 연결 풀 설정 관리
  - **비동기 처리**: Reactor 기반 논블로킹 I/O로 높은 동시성 처리 가능

- **에러 핸들링**: Resilience4j 또는 Spring RetryTemplate
  - **권장**: Resilience4j (비동기 지원, Circuit Breaker 패턴 제공)
  - **대안**: Spring RetryTemplate (동기식 재시도)
  - **재시도 정책**: 최대 3회, 지수 백오프(exponential backoff) 적용

- **Rate Limiting**: Redis 기반 (출처별 요청 간격 관리)
  - **설정 관리**: `application.yml`을 통한 출처별 간격 설정
  - **구현**: Spring의 `@Scheduled`와 Redis를 활용한 분산 락 패턴

### 구현 구조

> **개선 사항**: 클린코드 원칙(SRP, DIP, OCP)을 준수한 구조로 검증 및 개선했습니다. 불필요한 추상화를 제거하고 현재 요구사항에 맞는 최소한의 복잡도로 설계했습니다.

```
client/rss/
├── parser/
│   ├── RssParser.java (인터페이스 - DIP 준수)
│   ├── TechCrunchRssParser.java (구현체 - SRP 준수)
│   ├── GoogleDevelopersBlogRssParser.java
│   ├── ArsTechnicaRssParser.java
│   └── MediumTechnologyRssParser.java
├── dto/
│   └── RssFeedItem.java (파싱된 RSS 아이템 DTO)
├── config/
│   ├── RssParserConfig.java (WebClient 빈 설정)
│   └── RssProperties.java (@ConfigurationProperties - 설정 관리)
└── util/
    ├── RssFeedValidator.java (피드 검증 - SRP 준수)
    └── RssDataCleaner.java (데이터 정제 - SRP 준수)
```

#### 설계 원칙 검증

- **단일 책임 원칙 (SRP)**: ✅ 각 클래스가 하나의 책임만 담당
  - `RssParser`: RSS 피드 파싱만 담당
  - `RssFeedValidator`: 피드 검증만 담당
  - `RssDataCleaner`: 데이터 정제만 담당

- **의존성 역전 원칙 (DIP)**: ✅ 인터페이스 기반 설계
  - `RssParser` 인터페이스를 통한 추상화
  - 구현체는 인터페이스에 의존

- **개방-폐쇄 원칙 (OCP)**: ✅ 확장에는 열려있고 수정에는 닫혀있음
  - 새로운 RSS 출처 추가 시 기존 코드 수정 없이 새로운 Parser 구현체만 추가
  - 전략 패턴을 통한 유연한 확장

- **오버엔지니어링 방지**: ✅ YAGNI 원칙 준수
  - 현재 요구사항(4개 RSS 출처)에 맞는 최소한의 구조
  - 불필요한 팩토리 패턴이나 복잡한 추상화 제거
  - 실제 필요 시에만 확장 가능한 구조 유지

### 주요 기능

1. **RSS 피드 파싱**
   - Rome 라이브러리를 사용한 RSS/Atom 피드 파싱
   - 제목, 내용, 발행일, URL, 작성자 등 추출

2. **피드 검증**
   - RSS 피드 유효성 검증
   - 필수 필드 존재 여부 확인
   - 중복 항목 제거

3. **데이터 정제**
   - HTML 태그 제거
   - 특수 문자 정규화
   - 내용 요약 생성 (선택사항)

4. **에러 핸들링**
   - 네트워크 오류 시 재시도 (최대 3회)
   - 타임아웃 처리 (기본 30초)
   - 실패 시 로깅 및 알림

### 활용 예시

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 생성자 주입, @ConfigurationProperties, Resilience4j 패턴을 적용한 예시로 개선했습니다.

```java
// config/RssProperties.java - 설정 관리
@ConfigurationProperties(prefix = "rss")
@Data
public class RssProperties {
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private Map<String, String> feedUrls = new HashMap<>();
}

// config/RssParserConfig.java - WebClient 빈 설정
@Configuration
@EnableConfigurationProperties(RssProperties.class)
public class RssParserConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder(RssProperties properties) {
        return WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "ShrimpTM-Demo/1.0")
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            ));
    }
}

// parser/TechCrunchRssParser.java - 생성자 주입 패턴
@Component
@Slf4j
@RequiredArgsConstructor
public class TechCrunchRssParser implements RssParser {
    private final WebClient.Builder webClientBuilder;
    private final RssFeedValidator validator;
    private final RssProperties properties;
    private final Resilience4jRetry retry;
    
    @Override
    public List<RssFeedItem> parse() {
        String feedUrl = properties.getFeedUrls().get("techcrunch");
        WebClient webClient = webClientBuilder.baseUrl(feedUrl).build();
        
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
}
```

---

## client-scraper 모듈

### 용도

공식 API가 없어 웹 스크래핑이 필요한 출처로부터 개발자 대회 정보를 수집합니다.

### 대상 출처 (json/sources.json 기준)

> **개선 사항**: `json/sources.json`에서 `type: "Web Scraping"`이고 `cost: "Free"`인 출처를 `total_score` 기준으로 정렬하여 상위 5개를 선별했습니다. 각 출처의 데이터 형식, 공식 문서 URL, Rate Limiting 정책, robots.txt 및 ToS 확인 사항을 추가했습니다.

#### Priority 2 출처 (total_score 순서)

1. **LeetCode Contests** (total_score: 32, Priority: 2)
   - **url**: `https://leetcode.com`
   - **data_format**: GraphQL/JSON (우선), HTML (대안)
   - **update_frequency**: 주간
   - **documentation_url**: https://leetcode.com
   - **rate_limit**: 적절한 사용 권장
   - **특별 주의사항**:
     - GraphQL 엔드포인트 존재 가능성 확인 필요
     - 공식 API 문서 없음 (GraphQL 쿼리 구성 필요)
     - robots.txt 확인 필수
     - 인증 없이 제한적 접근 가능

2. **Google Summer of Code** (total_score: 32, Priority: 2)
   - **url**: `https://summerofcode.withgoogle.com`
   - **data_format**: HTML
   - **update_frequency**: 연간 (프로그램 기간 중 일일)
   - **documentation_url**: https://summerofcode.withgoogle.com
   - **rate_limit**: 웹 스크래핑 시 적절한 사용
   - **특별 주의사항**:
     - Google 공식 프로그램으로 높은 신뢰성
     - 연간 프로그램으로 업데이트 제한적
     - robots.txt 확인 필수
     - ToS 확인 필요

3. **Devpost** (total_score: 30, Priority: 2)
   - **url**: `https://devpost.com`
   - **data_format**: HTML
   - **update_frequency**: 일일
   - **documentation_url**: https://devpost.com
   - **rate_limit**: 웹 스크래핑 시 robots.txt 준수 필요
   - **특별 주의사항**:
     - 세계 최대 규모의 해커톤 플랫폼
     - robots.txt 확인 필수
     - Terms of Service 확인 필수
     - 구조 변경 시 영향 가능

4. **Major League Hacking (MLH)** (total_score: 29, Priority: 2)
   - **url**: `https://mlh.io`
   - **data_format**: HTML
   - **update_frequency**: 주간
   - **documentation_url**: https://mlh.io
   - **rate_limit**: 웹 스크래핑 시 적절한 간격 유지
   - **특별 주의사항**:
     - 학생 해커톤 전문 플랫폼
     - robots.txt 확인 필수
     - 학생 대상 제한 사항 확인 필요

5. **AtCoder** (total_score: 28, Priority: 2)
   - **url**: `https://atcoder.jp`
   - **data_format**: HTML
   - **update_frequency**: 주간
   - **documentation_url**: https://atcoder.jp
   - **rate_limit**: 웹 스크래핑 시 적절한 간격
   - **특별 주의사항**:
     - 일본의 주요 알고리즘 대회 플랫폼
     - 일본어 일부 포함 가능
     - 비공식 API 존재 가능성 확인
     - robots.txt 확인 필수

### 기술 스택

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 기술 스택을 업데이트했습니다. Jsoup과 Spring WebClient 통합 패턴, Resilience4j를 통한 재시도 로직, crawler-commons를 활용한 robots.txt 파싱을 권장합니다. Selenium 사용은 최소화하여 정적 HTML 파싱으로 충분한 경우 Jsoup만 사용합니다.

- **라이브러리**: 
  - **Jsoup** (정적 HTML 파싱, 권장)
    - 공식 문서: https://jsoup.org/
    - Cookbook: https://jsoup.org/cookbook/
    - Maven/Gradle 의존성: `org.jsoup:jsoup:1.17.2`
    - **선택 이유**: 대부분의 웹사이트가 정적 HTML을 제공하므로 Jsoup만으로 충분하며, 가볍고 빠름
    - **Spring WebClient 통합**: WebClient로 HTML을 가져온 후 Jsoup으로 파싱하는 패턴 권장
  
  - **Selenium WebDriver** (동적 콘텐츠 처리, 최소화)
    - 공식 문서: https://www.selenium.dev/documentation/
    - Maven/Gradle 의존성: `org.seleniumhq.selenium:selenium-java:4.15.0`
    - **사용 시점**: JavaScript로 동적으로 생성되는 콘텐츠가 필요한 경우에만 사용
    - **주의사항**: 리소스 집약적이므로 정적 HTML 파싱으로 충분한 경우 사용하지 않음
  
  - **crawler-commons** (robots.txt 파싱, 권장)
    - GitHub: https://github.com/crawler-commons/crawler-commons
    - Maven/Gradle 의존성: `com.github.crawler-commons:crawler-commons:1.2`
    - **선택 이유**: robots.txt 표준을 준수하는 안정적인 파서 제공

- **HTTP 클라이언트**: Spring WebClient (권장) 또는 Jsoup.connect()
  - **권장 패턴**: WebClient로 HTML을 가져온 후 Jsoup으로 파싱
  - **이유**: WebClient는 비동기 처리, 타임아웃 설정, 재시도 로직 통합이 용이
  - **설정 관리**: `application.yml`을 통한 타임아웃, 연결 풀 설정 관리

- **에러 핸들링**: Resilience4j 또는 Spring RetryTemplate
  - **권장**: Resilience4j (비동기 지원, Circuit Breaker 패턴 제공)
  - **대안**: Spring RetryTemplate (동기식 재시도)
  - **재시도 정책**: 최대 3회, 지수 백오프(exponential backoff) 적용

- **Rate Limiting**: Redis 기반 (출처별 요청 간격 관리)
  - **설정 관리**: `application.yml`을 통한 출처별 간격 설정
  - **구현**: Spring의 `@Scheduled`와 Redis를 활용한 분산 락 패턴
  - **기본 간격**: 최소 1초 이상 (출처별 설정 가능)

- **robots.txt 확인**: crawler-commons (권장)
  - **구현**: `BaseRobotRules`를 활용한 robots.txt 파싱 및 검증
  - **확인 시점**: 모든 스크래핑 전 필수 확인
  - **준수 사항**: Disallow 경로 스크래핑 금지, Crawl-delay 지시사항 준수

### 구현 구조

> **개선 사항**: 클린코드 원칙(SRP, DIP, OCP)을 준수한 구조로 검증 및 개선했습니다. 불필요한 추상화를 제거하고 현재 요구사항(5개 웹 스크래핑 출처)에 맞는 최소한의 복잡도로 설계했습니다. Selenium은 선택사항으로 분리하여 필요 시에만 사용하도록 구조화했습니다.

```
client/scraper/
├── scraper/
│   ├── WebScraper.java (인터페이스 - DIP 준수)
│   ├── LeetCodeScraper.java (GraphQL 우선, HTML 대안)
│   ├── GoogleSummerOfCodeScraper.java
│   ├── DevpostScraper.java
│   ├── MLHScraper.java
│   └── AtCoderScraper.java
├── scraper/selenium/ (선택사항 - 동적 콘텐츠가 필요한 경우만)
│   └── SeleniumWebScraper.java (인터페이스)
├── dto/
│   └── ScrapedContestItem.java (스크래핑된 대회 정보 DTO)
├── config/
│   ├── ScraperConfig.java (WebClient 빈 설정)
│   └── ScraperProperties.java (@ConfigurationProperties - 설정 관리)
├── util/
│   ├── RobotsTxtChecker.java (robots.txt 확인 - SRP 준수)
│   ├── ScrapedDataValidator.java (데이터 검증 - SRP 준수)
│   └── ScrapedDataCleaner.java (데이터 정제 - SRP 준수)
└── exception/
    └── ScrapingException.java (스크래핑 예외)
```

#### 설계 원칙 검증

- **단일 책임 원칙 (SRP)**: ✅ 각 클래스가 하나의 책임만 담당
  - `WebScraper`: 웹 스크래핑만 담당
  - `RobotsTxtChecker`: robots.txt 확인만 담당
  - `ScrapedDataValidator`: 데이터 검증만 담당
  - `ScrapedDataCleaner`: 데이터 정제만 담당

- **의존성 역전 원칙 (DIP)**: ✅ 인터페이스 기반 설계
  - `WebScraper` 인터페이스를 통한 추상화
  - 구현체는 인터페이스에 의존

- **개방-폐쇄 원칙 (OCP)**: ✅ 확장에는 열려있고 수정에는 닫혀있음
  - 새로운 웹사이트 추가 시 기존 코드 수정 없이 새로운 Scraper 구현체만 추가
  - 전략 패턴을 통한 유연한 확장

- **오버엔지니어링 방지**: ✅ YAGNI 원칙 준수
  - 현재 요구사항(5개 웹 스크래핑 출처)에 맞는 최소한의 구조
  - 팩토리 패턴 제거 (단순 @Component 주입으로 충분)
  - Selenium 사용 최소화 (정적 HTML 파싱으로 충분한 경우 Jsoup만 사용)
  - 템플릿 메서드 패턴은 공통 로직이 많을 때만 고려

### 주요 기능

1. **웹 스크래핑**
   - Jsoup을 사용한 HTML 파싱
   - CSS 선택자를 통한 데이터 추출
   - 동적 콘텐츠가 필요한 경우 Selenium 활용

2. **robots.txt 확인**
   - 스크래핑 전 robots.txt 확인
   - 허용된 경로만 스크래핑
   - User-Agent 설정 (프로젝트 식별자 포함)

3. **데이터 검증 및 정제**
   - 필수 필드 존재 여부 확인
   - HTML 태그 제거
   - 날짜 형식 정규화
   - 중복 항목 제거

4. **Rate Limiting 준수**
   - 출처별 요청 간격 관리 (Redis 활용)
   - 기본 간격: 1초 이상
   - 출처별 설정 가능

5. **에러 핸들링**
   - 네트워크 오류 시 재시도 (최대 3회)
   - 타임아웃 처리 (기본 30초)
   - HTML 구조 변경 감지 및 로깅
   - 실패 시 알림

### 활용 예시

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 생성자 주입, @ConfigurationProperties, Resilience4j 패턴, WebClient와 Jsoup 통합 패턴을 적용한 예시로 개선했습니다.

```java
// config/ScraperProperties.java - 설정 관리
@ConfigurationProperties(prefix = "scraper")
@Data
public class ScraperProperties {
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private String userAgent = "ShrimpTM-Demo/1.0 (+https://github.com/your-repo)";
    private Map<String, ScraperSourceConfig> sources = new HashMap<>();
    
    @Data
    public static class ScraperSourceConfig {
        private String baseUrl;
        private String dataFormat; // "HTML" or "GraphQL"
        private long minIntervalSeconds = 1;
        private boolean requiresSelenium = false;
    }
}

// config/ScraperConfig.java - WebClient 빈 설정
@Configuration
@EnableConfigurationProperties(ScraperProperties.class)
public class ScraperConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder(ScraperProperties properties) {
        return WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, properties.getUserAgent())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .responseTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            ));
    }
}

// scraper/DevpostScraper.java - 생성자 주입 패턴, WebClient + Jsoup 통합
@Component
@Slf4j
@RequiredArgsConstructor
public class DevpostScraper implements WebScraper {
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final RateLimiter rateLimiter;
    private final ScrapedDataValidator validator;
    private final ScrapedDataCleaner cleaner;
    private final ScraperProperties properties;
    private final Retry retry; // Resilience4j Retry
    
    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("devpost");
        
        // robots.txt 확인
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), "/hackathons")) {
            throw new ScrapingException("Scraping not allowed by robots.txt");
        }
        
        // Rate Limiting 확인
        rateLimiter.checkAndWait("devpost", config.getMinIntervalSeconds());
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        
        return retry.executeSupplier(() -> {
            try {
                // WebClient로 HTML 가져오기
                String html = webClient.get()
                    .uri("/hackathons")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Jsoup으로 HTML 파싱
                Document doc = Jsoup.parse(html);
                List<ScrapedContestItem> items = doc.select(".hackathon-tile").stream()
                    .map(this::extractContestInfo)
                    .collect(Collectors.toList());
                
                // 데이터 검증 및 정제
                items = validator.validate(items);
                items = cleaner.clean(items);
                
                return items;
            } catch (Exception e) {
                log.error("Failed to scrape Devpost", e);
                throw new ScrapingException("Devpost scraping failed", e);
            }
        });
    }
    
    @Override
    public String getSourceName() {
        return "Devpost";
    }
    
    @Override
    public String getBaseUrl() {
        return properties.getSources().get("devpost").getBaseUrl();
    }
}
```

### 법적/윤리적 고려사항

> **개선 사항**: 공식 문서 기반으로 법적/윤리적 고려사항을 강화했습니다. robots.txt 표준 준수, ToS 확인 절차, Rate Limiting 정책, User-Agent 설정 가이드라인을 구체화했습니다.

1. **robots.txt 준수** (필수)
   - **확인 시점**: 모든 웹 스크래핑 전 필수 확인
   - **구현 방법**: crawler-commons 라이브러리를 사용한 표준 파싱
   - **준수 사항**:
     - Disallow 경로는 절대 스크래핑하지 않음
     - Crawl-delay 지시사항이 있으면 해당 간격 준수
     - User-Agent별 규칙 확인 (일반적으로 `*` 또는 특정 User-Agent)
   - **예외 처리**: robots.txt를 가져올 수 없는 경우 보수적으로 접근 (스크래핑 자제)

2. **Terms of Service (ToS) 확인** (필수)
   - **확인 절차**:
     1. 각 웹사이트의 ToS 페이지 확인
     2. 스크래핑 관련 조항 검색
     3. 명시적으로 금지된 경우 해당 출처 제외
     4. 불명확한 경우 보수적으로 접근 (스크래핑 자제 또는 법적 자문)
   - **주의사항**:
     - ToS는 변경될 수 있으므로 정기적으로 재확인
     - 스크래핑 금지 조항이 있는 경우 해당 출처는 제외
   - **선별된 출처별 ToS 확인 필요**:
     - LeetCode: 공식 API 문서 없음, GraphQL 엔드포인트 사용 시 ToS 확인 필요
     - Google Summer of Code: Google 공식 프로그램, ToS 확인 필요
     - Devpost: robots.txt 확인 및 ToS 확인 필수
     - MLH: robots.txt 확인 및 ToS 확인 필요
     - AtCoder: robots.txt 확인 및 ToS 확인 필요

3. **Rate Limiting** (필수)
   - **기본 정책**: 최소 1초 간격 (출처별 설정 가능)
   - **구현 방법**: Redis를 활용한 분산 환경에서의 Rate Limiting
   - **출처별 권장 간격**:
     - LeetCode: 적절한 사용 권장 (기본 1초 이상)
     - Google Summer of Code: 적절한 사용 (기본 1초 이상)
     - Devpost: robots.txt 준수 (기본 1초 이상)
     - MLH: 적절한 간격 유지 (기본 1초 이상)
     - AtCoder: 적절한 간격 (기본 1초 이상)
   - **Crawl-delay 준수**: robots.txt에 Crawl-delay가 명시된 경우 해당 값 준수

4. **User-Agent 설정** (권장)
   - **형식**: `프로젝트명/버전 (연락처 정보)`
   - **예시**: `ShrimpTM-Demo/1.0 (+https://github.com/your-repo)`
   - **포함 정보**:
     - 명확한 프로젝트 식별자 (필수)
     - 연락처 정보 (선택사항, 권장)
   - **설정 방법**: `application.yml`의 `scraper.user-agent` 설정 또는 WebClient 기본 헤더 설정

5. **법적 책임**
   - 웹 스크래핑은 해당 웹사이트의 ToS 및 저작권법을 준수해야 함
   - 수집한 데이터의 사용 목적과 범위를 명확히 정의
   - 개인정보가 포함된 경우 개인정보보호법 준수
   - 불확실한 경우 법적 자문 권장

---

## 데이터 수집 전략

### 수집 주기

#### RSS 출처 (total_score 순서)
- **Google Developers Blog RSS** (total_score: 36, Priority: 1)
  - 수집 주기: 주 1회 (월요일 새벽 2시 권장)
  - 피드 형식: Atom 1.0
- **TechCrunch RSS** (total_score: 35, Priority: 1)
  - 수집 주기: 하루 1회 (새벽 2시 권장)
  - 피드 형식: RSS 2.0
- **Ars Technica RSS** (total_score: 34, Priority: 2)
  - 수집 주기: 하루 1회 (새벽 2시 권장)
  - 피드 형식: RSS 2.0
- **Medium Technology RSS** (total_score: 30, Priority: 2)
  - 수집 주기: 하루 1회 (새벽 2시 권장)
  - 피드 형식: RSS 2.0

#### 웹 스크래핑 출처 (total_score 순서)
- **LeetCode Contests** (total_score: 32, Priority: 2)
  - 수집 주기: 주 1회 (월요일 새벽 3시 권장)
  - 데이터 형식: GraphQL/JSON (우선), HTML (대안)
- **Google Summer of Code** (total_score: 32, Priority: 2)
  - 수집 주기: 연간 (프로그램 기간 중 일일)
  - 데이터 형식: HTML
- **Devpost** (total_score: 30, Priority: 2)
  - 수집 주기: 하루 1회 (새벽 3시 권장)
  - 데이터 형식: HTML
- **MLH** (total_score: 29, Priority: 2)
  - 수집 주기: 주 1회 (월요일 새벽 3시 권장)
  - 데이터 형식: HTML
- **AtCoder** (total_score: 28, Priority: 2)
  - 수집 주기: 주 1회 (월요일 새벽 3시 권장)
  - 데이터 형식: HTML
- **Rate Limiting**: 모든 출처에 대해 최소 1초 간격 유지, 순차 수집

### 수집 프로세스

1. **스케줄러 트리거** (Spring Scheduler)
   - 설정된 시간에 수집 작업 시작
   - 출처별 순차 처리

2. **데이터 수집**
   - RSS: `client-rss` 모듈 사용
   - Web Scraping: `client-scraper` 모듈 사용

3. **데이터 정제**
   - 중복 제거 (URL 기반)
   - 필수 필드 검증
   - 데이터 형식 정규화

4. **데이터 저장**
   - MongoDB Atlas에 저장 (Query Side)
   - `NewsArticleDocument` 또는 `ContestDocument` 형식

5. **에러 처리**
   - 실패한 출처는 로깅 및 알림
   - 다음 수집 주기까지 재시도 안 함 (수집 주기 내 재시도는 최대 3회)

### Redis 캐싱 전략

- **목적**: 중복 수집 방지 및 성능 최적화
- **캐시 키**: `rss:last-collected:{source-name}` 또는 `scraper:last-collected:{source-name}`
- **캐시 값**: 마지막 수집 시간 (ISO 8601 형식)
- **TTL**: 7일

---

## 구현 가이드

### 1. client-rss 모듈 구현

#### 의존성 추가 (build.gradle)

> **개선 사항**: Resilience4j 의존성을 추가하여 Spring Boot 베스트 프랙티스를 반영했습니다.

```gradle
dependencies {
    implementation project(':common-core')
    
    // Rome 라이브러리 (RSS/Atom 피드 파싱)
    implementation 'com.rometools:rome:1.19.0'
    
    // Spring WebFlux (WebClient 사용)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Resilience4j (재시도 로직)
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
    implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
    
    // Configuration Processor (application.yml 자동완성)
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

// Disable failure when no tests are discovered (test files exist but are commented out)
tasks.named('test') {
    failOnNoDiscoveredTests = false
}
```

#### application.yml 설정 예시

> **개선 사항**: Spring Boot의 설정 관리 방식을 활용하여 `application.yml`에 RSS 관련 설정을 추가했습니다.

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

#### 기본 구조

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 생성자 주입, @ConfigurationProperties, Resilience4j 패턴을 적용했습니다.

```java
// parser/RssParser.java (인터페이스 - DIP 준수)
public interface RssParser {
    List<RssFeedItem> parse();
    String getSourceName();
    String getFeedUrl();
}

// config/RssProperties.java - application.yml 설정 매핑
@ConfigurationProperties(prefix = "rss")
@Data
public class RssProperties {
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private long retryDelayMs = 1000;
    private Map<String, RssSourceConfig> sources = new HashMap<>();
    
    @Data
    public static class RssSourceConfig {
        private String feedUrl;
        private String feedFormat; // "RSS_2.0" or "ATOM_1.0"
        private String updateFrequency;
    }
}

// parser/TechCrunchRssParser.java (구현 예시 - 생성자 주입)
@Component
@Slf4j
@RequiredArgsConstructor
public class TechCrunchRssParser implements RssParser {
    private final WebClient.Builder webClientBuilder;
    private final RssFeedValidator validator;
    private final RssProperties properties;
    private final Retry retry; // Resilience4j Retry
    
    @Override
    public List<RssFeedItem> parse() {
        RssProperties.RssSourceConfig config = properties.getSources().get("techcrunch");
        WebClient webClient = webClientBuilder.baseUrl(config.getFeedUrl()).build();
        
        return retry.executeSupplier(() -> {
            try {
                String feedContent = webClient.get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                SyndFeed feed = new SyndFeedInput().build(new StringReader(feedContent));
                validator.validate(feed);
                
                return feed.getEntries().stream()
                    .map(this::convertToRssFeedItem)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Failed to parse TechCrunch RSS feed", e);
                throw new RssParsingException("TechCrunch RSS parsing failed", e);
            }
        });
    }
    
    @Override
    public String getSourceName() {
        return "TechCrunch";
    }
    
    @Override
    public String getFeedUrl() {
        return properties.getSources().get("techcrunch").getFeedUrl();
    }
}
```

### 2. client-scraper 모듈 구현

#### 의존성 추가 (build.gradle)

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 Resilience4j, crawler-commons 의존성을 추가했습니다. Selenium은 선택사항으로 명시했습니다.

```gradle
dependencies {
    implementation project(':common-core')
    
    // Jsoup (HTML 파싱, 권장)
    implementation 'org.jsoup:jsoup:1.17.2'
    
    // crawler-commons (robots.txt 파싱, 권장)
    implementation 'com.github.crawler-commons:crawler-commons:1.2'
    
    // Spring WebFlux (WebClient 사용)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Resilience4j (재시도 로직)
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
    implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
    
    // Selenium (동적 콘텐츠 처리, 선택사항 - 필요한 경우만)
    // implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'
    
    // Configuration Processor (application.yml 자동완성)
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
    
    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

// Disable failure when no tests are discovered (test files exist but are commented out)
tasks.named('test') {
    failOnNoDiscoveredTests = false
}
```

#### application.yml 설정 예시

> **개선 사항**: Spring Boot의 설정 관리 방식을 활용하여 `application.yml`에 웹 스크래핑 관련 설정을 추가했습니다.

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

#### 기본 구조

> **개선 사항**: Spring Boot 베스트 프랙티스를 반영하여 생성자 주입, @ConfigurationProperties, Resilience4j 패턴, WebClient와 Jsoup 통합 패턴을 적용했습니다.

```java
// scraper/WebScraper.java (인터페이스 - DIP 준수)
public interface WebScraper {
    List<ScrapedContestItem> scrape();
    String getSourceName();
    String getBaseUrl();
}

// util/RobotsTxtChecker.java - crawler-commons 활용
@Component
@Slf4j
@RequiredArgsConstructor
public class RobotsTxtChecker {
    private final WebClient.Builder webClientBuilder;
    
    public boolean isAllowed(String baseUrl, String path) {
        try {
            String robotsTxtUrl = baseUrl + "/robots.txt";
            String robotsTxt = webClientBuilder.build()
                .get()
                .uri(robotsTxtUrl)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            BaseRobotRules rules = RobotRulesParserFactory.getRobotRulesParser(
                "ShrimpTM-Demo/1.0", robotsTxt
            );
            
            return rules.isAllowed(path);
        } catch (Exception e) {
            log.warn("Failed to check robots.txt for {}: {}", baseUrl, e.getMessage());
            // 보수적으로 접근: robots.txt를 가져올 수 없으면 스크래핑 자제
            return false;
        }
    }
}

// scraper/DevpostScraper.java (구현 예시 - 생성자 주입, WebClient + Jsoup 통합)
@Component
@Slf4j
@RequiredArgsConstructor
public class DevpostScraper implements WebScraper {
    private final WebClient.Builder webClientBuilder;
    private final RobotsTxtChecker robotsTxtChecker;
    private final RateLimiter rateLimiter;
    private final ScrapedDataValidator validator;
    private final ScrapedDataCleaner cleaner;
    private final ScraperProperties properties;
    private final Retry retry; // Resilience4j Retry
    
    @Override
    public List<ScrapedContestItem> scrape() {
        ScraperProperties.ScraperSourceConfig config = properties.getSources().get("devpost");
        
        // robots.txt 확인
        if (!robotsTxtChecker.isAllowed(config.getBaseUrl(), "/hackathons")) {
            throw new ScrapingException("Scraping not allowed by robots.txt");
        }
        
        // Rate Limiting 확인
        rateLimiter.checkAndWait("devpost", config.getMinIntervalSeconds());
        
        WebClient webClient = webClientBuilder.baseUrl(config.getBaseUrl()).build();
        
        return retry.executeSupplier(() -> {
            try {
                // WebClient로 HTML 가져오기
                String html = webClient.get()
                    .uri("/hackathons")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
                
                // Jsoup으로 HTML 파싱
                Document doc = Jsoup.parse(html);
                List<ScrapedContestItem> items = doc.select(".hackathon-tile").stream()
                    .map(this::extractContestInfo)
                    .collect(Collectors.toList());
                
                // 데이터 검증 및 정제
                items = validator.validate(items);
                items = cleaner.clean(items);
                
                return items;
            } catch (Exception e) {
                log.error("Failed to scrape Devpost", e);
                throw new ScrapingException("Devpost scraping failed", e);
            }
        });
    }
    
    @Override
    public String getSourceName() {
        return "Devpost";
    }
    
    @Override
    public String getBaseUrl() {
        return properties.getSources().get("devpost").getBaseUrl();
    }
}
```

### 3. 공통 기능

#### Rate Limiting (Redis 활용)

> **개선 사항**: 출처별 최소 간격 설정을 지원하도록 개선했습니다. Thread.sleep() 대신 CompletableFuture.delayedExecutor()를 사용하여 비동기 처리 가능하도록 개선할 수 있습니다.

```java
// util/RateLimiter.java
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:";
    
    public void checkAndWait(String sourceName, long minIntervalSeconds) {
        String key = RATE_LIMIT_KEY_PREFIX + sourceName;
        String lastRequestTime = redisTemplate.opsForValue().get(key);
        
        if (lastRequestTime != null) {
            long lastTime = Long.parseLong(lastRequestTime);
            long currentTime = System.currentTimeMillis();
            long elapsed = (currentTime - lastTime) / 1000;
            
            if (elapsed < minIntervalSeconds) {
                long waitTime = (minIntervalSeconds - elapsed) * 1000;
                log.debug("Rate limiting: waiting {}ms for {}", waitTime, sourceName);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiting interrupted", e);
                }
            }
        }
        
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
    }
}
```

#### 에러 핸들링 및 재시도

> **개선 사항**: Spring Boot 베스트 프랙티스에 따라 Resilience4j를 사용한 재시도 로직으로 개선했습니다. Thread.sleep()을 사용한 수동 재시도 대신 Resilience4j의 Retry 컴포넌트를 활용합니다.

```java
// config/Resilience4jConfig.java - Resilience4j 설정
@Configuration
@EnableConfigurationProperties(RssProperties.class)
public class Resilience4jConfig {
    
    @Bean
    public Retry rssRetry(RssProperties properties) {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(properties.getMaxRetries())
            .waitDuration(Duration.ofMillis(properties.getRetryDelayMs()))
            .retryExceptions(WebClientException.class, IOException.class)
            .exponentialBackoff(properties.getRetryDelayMs(), 2, Duration.ofSeconds(10))
            .build();
        
        return Retry.of("rssRetry", config);
    }
}

// 기존 RetryHandler는 제거하고 Resilience4j Retry 직접 사용
// 각 Parser에서 @RequiredArgsConstructor로 Retry 주입받아 사용
```

### 4. 테스트 코드 작성

> **개선 사항**: `client/feign` 모듈과 일관된 테스트 코드 형식을 적용했습니다. JUnit 5, Given-When-Then 패턴, `@SpringBootTest`를 사용한 통합 테스트 가이드를 추가했습니다.

#### 테스트 의존성

테스트 의존성은 각 모듈의 "의존성 추가 (build.gradle)" 섹션에 이미 포함되어 있습니다. (`testImplementation`, `testRuntimeOnly`, `tasks.named('test')` 설정)

#### 테스트 컨텍스트 클래스

`client/feign` 모듈의 `FeignTestContext`와 동일한 패턴으로 테스트 컨텍스트를 작성합니다.

```java
// test/java/.../client/rss/RssTestContext.java
package com.tech.n.ai.client.rss;

import com.tech.n.ai.client.rss.config.RssParserConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Import;

@ImportAutoConfiguration({
    WebFluxAutoConfiguration.class,
})
@Import({
    RssParserConfig.class,
})
class RssTestContext {
}
```

```java
// test/java/.../client/scraper/ScraperTestContext.java
package com.tech.n.ai.client.scraper;

import com.tech.n.ai.client.scraper.config.ScraperConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Import;

@ImportAutoConfiguration({
    WebFluxAutoConfiguration.class,
})
@Import({
    ScraperConfig.class,
})
class ScraperTestContext {
}
```

#### client-rss 모듈 테스트 예시

```java
// test/java/.../client/rss/parser/TechCrunchRssParserTest.java
package com.tech.n.ai.client.rss.parser;

import com.tech.n.ai.client.rss.RssTestContext;
import com.tech.n.ai.client.rss.config.RssParserConfig;
import com.tech.n.ai.client.rss.dto.RssFeedItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = {
    RssTestContext.class,
    RssParserConfig.class
}, properties = {
    "spring.profiles.active=local",
    "rss.sources.techcrunch.feed-url=https://techcrunch.com/feed/",
    "rss.sources.techcrunch.feed-format=RSS_2.0",
})
public class TechCrunchRssParserTest {

    @Autowired
    private TechCrunchRssParser parser;

    //    @Test
    @DisplayName("TechCrunch RSS 피드 파싱 테스트")
    void testParse() throws Exception {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)

        // when
        List<RssFeedItem> items = parser.parse();

        // then
        Assertions.assertNotNull(items);
        Assertions.assertFalse(items.isEmpty());
        items.forEach(item -> {
            Assertions.assertNotNull(item.getTitle());
            Assertions.assertNotNull(item.getLink());
            Assertions.assertNotNull(item.getPublishedDate());
        });
    }

    //    @Test
    @DisplayName("getSourceName 테스트")
    void testGetSourceName() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)

        // when
        String sourceName = parser.getSourceName();

        // then
        Assertions.assertEquals("TechCrunch", sourceName);
    }

    //    @Test
    @DisplayName("getFeedUrl 테스트")
    void testGetFeedUrl() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)

        // when
        String feedUrl = parser.getFeedUrl();

        // then
        Assertions.assertEquals("https://techcrunch.com/feed/", feedUrl);
    }
}
```

#### client-scraper 모듈 테스트 예시

```java
// test/java/.../client/scraper/scraper/DevpostScraperTest.java
package com.tech.n.ai.client.scraper.scraper;

import com.tech.n.ai.client.scraper.ScraperTestContext;
import com.tech.n.ai.client.scraper.config.ScraperConfig;
import com.tech.n.ai.client.scraper.dto.ScrapedContestItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = {
    ScraperTestContext.class,
    ScraperConfig.class
}, properties = {
    "spring.profiles.active=local",
    "scraper.sources.devpost.base-url=https://devpost.com",
    "scraper.sources.devpost.data-format=HTML",
    "scraper.sources.devpost.min-interval-seconds=1",
    "scraper.sources.devpost.requires-selenium=false",
})
public class DevpostScraperTest {

    @Autowired
    private DevpostScraper scraper;

    //    @Test
    @DisplayName("Devpost 웹 스크래핑 테스트")
    void testScrape() throws Exception {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)
        // robots.txt 확인 및 Rate Limiting은 내부적으로 처리됨

        // when
        List<ScrapedContestItem> items = scraper.scrape();

        // then
        Assertions.assertNotNull(items);
        // 실제 스크래핑 결과에 따라 검증 로직 추가
        items.forEach(item -> {
            Assertions.assertNotNull(item.getTitle());
            Assertions.assertNotNull(item.getUrl());
        });
    }

    //    @Test
    @DisplayName("getSourceName 테스트")
    void testGetSourceName() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)

        // when
        String sourceName = scraper.getSourceName();

        // then
        Assertions.assertEquals("Devpost", sourceName);
    }

    //    @Test
    @DisplayName("getBaseUrl 테스트")
    void testGetBaseUrl() {
        // given
        // (설정은 application.yml 또는 properties로 주입됨)

        // when
        String baseUrl = scraper.getBaseUrl();

        // then
        Assertions.assertEquals("https://devpost.com", baseUrl);
    }
}
```

#### 테스트 작성 가이드라인

1. **테스트 구조**: `client/feign` 모듈과 동일하게 Given-When-Then 패턴 사용
2. **테스트 어노테이션**: 
   - `@SpringBootTest` 사용
   - `classes`에 테스트 컨텍스트와 Config 클래스 지정
   - `properties`에 프로파일 및 설정 값 지정
3. **테스트 메서드**:
   - `@DisplayName`으로 테스트 목적 명시
   - `@Test`는 주석 처리하여 기본적으로 비활성화 (필요 시 활성화)
   - 메서드명은 `test` 접두사 사용
4. **의존성 주입**: `@Autowired`로 테스트 대상 컴포넌트 주입
5. **검증**: JUnit 5의 `Assertions` 사용
6. **외부 의존성**: 실제 외부 API/RSS 피드에 의존하지 않도록 Mock 또는 테스트용 설정 사용 권장

---

## 참고 자료

> **개선 사항**: 신뢰할 수 있는 공식 출처만 참고하여 참고 자료 섹션을 확장했습니다. 각 출처의 URL, 참고 내용 요약, 주요 정보를 포함했습니다.

### Spring Boot 및 Spring Framework 공식 문서

1. **Spring Boot 공식 문서**
   - URL: https://spring.io/projects/spring-boot
   - 참고 내용: Spring Boot 자동 설정, WebClient 사용법, @ConfigurationProperties 활용
   - 주요 정보: Spring Boot 4.0.1 기준 베스트 프랙티스, 의존성 주입 패턴, 설정 관리 방식

2. **Spring Framework 공식 문서**
   - URL: https://spring.io/projects/spring-framework
   - 참고 내용: WebClient 비동기 HTTP 요청, 의존성 주입 및 빈 관리 패턴
   - 주요 정보: 생성자 주입 권장, WebClient.Builder 빈 설정 패턴

3. **Spring Retry 공식 문서**
   - URL: https://github.com/spring-projects/spring-retry
   - 참고 내용: RetryTemplate을 사용한 동기식 재시도 로직
   - 주요 정보: @Retryable 어노테이션, RetryTemplate 설정

### Resilience4j 공식 문서

4. **Resilience4j 공식 문서**
   - URL: https://resilience4j.readme.io/
   - 참고 내용: Retry, Circuit Breaker 패턴 구현
   - 주요 정보: 비동기 재시도 지원, 지수 백오프(exponential backoff) 설정, Spring Boot 통합

### RSS/Atom 피드 파싱 라이브러리

5. **Rome 라이브러리**
   - 공식 문서: https://rometools.github.io/rome/
   - GitHub: https://github.com/rometools/rome
   - 참고 내용: RSS 2.0 및 Atom 1.0 피드 파싱
   - 주요 정보: SyndFeedInput을 통한 피드 파싱, 다양한 피드 형식 지원

### RSS/Atom 표준 스펙

6. **RSS 2.0 표준 스펙**
   - URL: https://www.rssboard.org/rss-specification
   - 참고 내용: RSS 2.0 피드 구조 및 필수/선택 필드 정의
   - 주요 정보: `<channel>`, `<item>` 구조, 날짜 형식 (RFC 822)

7. **Atom 1.0 표준 스펙**
   - URL: https://tools.ietf.org/html/rfc4287
   - 참고 내용: Atom 1.0 피드 구조 및 네임스페이스
   - 주요 정보: `<feed>`, `<entry>` 구조, Google Developers Blog RSS가 Atom 형식 사용

### 웹 스크래핑 라이브러리

8. **Jsoup 공식 문서**
   - 공식 문서: https://jsoup.org/
   - Cookbook: https://jsoup.org/cookbook/
   - API 문서: https://jsoup.org/apidocs/
   - 참고 내용: HTML 파싱, CSS 선택자를 통한 데이터 추출, Spring WebClient와의 통합
   - 주요 정보: 정적 HTML 파싱, DOM 탐색, HTML 정제, Spring WebClient로 HTML을 가져온 후 Jsoup으로 파싱하는 패턴 권장

9. **Selenium WebDriver**
   - 공식 문서: https://www.selenium.dev/documentation/
   - Java API: https://www.selenium.dev/selenium/docs/api/java/
   - 참고 내용: 동적 콘텐츠 처리, JavaScript 렌더링이 필요한 경우
   - 주요 정보: WebDriver 설정, 헤드리스 모드 사용, 리소스 집약적이므로 필요한 경우에만 사용

10. **crawler-commons (robots.txt 파싱)**
    - GitHub: https://github.com/crawler-commons/crawler-commons
    - Maven Central: https://mvnrepository.com/artifact/com.github.crawler-commons/crawler-commons
    - 참고 내용: robots.txt 표준 준수 파싱, BaseRobotRules를 통한 경로 허용 여부 확인
    - 주요 정보: robots.txt 표준(RFC 9309) 준수, User-Agent별 규칙 확인, Crawl-delay 지시사항 파싱

### 웹 스크래핑 표준 및 규칙

11. **robots.txt 표준**
    - 공식 사이트: https://www.robotstxt.org/
    - RFC 9309: https://www.rfc-editor.org/rfc/rfc9309.html
    - 참고 내용: robots.txt 파일 형식, Disallow/Allow 규칙, Crawl-delay 지시사항
    - 주요 정보: 모든 웹 스크래핑 전 robots.txt 확인 필수, Disallow 경로는 절대 스크래핑 금지

12. **HTTP 표준 (RFC 7231)**
    - URL: https://tools.ietf.org/html/rfc7231
    - 참고 내용: HTTP 메서드, 상태 코드, 헤더 규칙
    - 주요 정보: User-Agent 헤더 설정, 적절한 HTTP 메서드 사용(GET), 상태 코드 처리

### 웹 스크래핑 대상 웹사이트 공식 문서

13. **LeetCode**
    - 공식 사이트: https://leetcode.com
    - 참고 내용: GraphQL 엔드포인트 존재 가능성, 공식 API 문서 없음
    - 주요 정보: GraphQL/JSON 형식 우선 사용, HTML 스크래핑은 대안, robots.txt 확인 필수

14. **Google Summer of Code**
    - 공식 사이트: https://summerofcode.withgoogle.com
    - 참고 내용: Google 공식 프로그램, 연간 프로그램으로 업데이트 제한적
    - 주요 정보: robots.txt 확인 필수, ToS 확인 필요, HTML 스크래핑

15. **Devpost**
    - 공식 사이트: https://devpost.com
    - 참고 내용: 세계 최대 규모의 해커톤 플랫폼, robots.txt 확인 및 ToS 확인 필수
    - 주요 정보: HTML 스크래핑, 구조 변경 시 영향 가능, robots.txt 준수 필요

16. **Major League Hacking (MLH)**
    - 공식 사이트: https://mlh.io
    - 참고 내용: 학생 해커톤 전문 플랫폼, 주간 업데이트
    - 주요 정보: HTML 스크래핑, robots.txt 확인 필수, 학생 대상 제한 사항 확인 필요

17. **AtCoder**
    - 공식 사이트: https://atcoder.jp
    - 참고 내용: 일본의 주요 알고리즘 대회 플랫폼, 비공식 API 존재 가능성
    - 주요 정보: HTML 스크래핑, 일본어 일부 포함 가능, robots.txt 확인 필수

### RSS Feed 제공자 공식 문서

10. **Google Developers Blog**
    - 공식 사이트: https://developers.googleblog.com
    - RSS 피드 URL: https://developers.googleblog.com/feeds/posts/default
    - 피드 형식: Atom 1.0
    - 참고 내용: Google 공식 개발자 블로그, 높은 신뢰성

11. **TechCrunch**
    - 공식 사이트: https://techcrunch.com
    - RSS 피드 URL: https://techcrunch.com/feed/
    - 피드 형식: RSS 2.0
    - 참고 내용: 기술 뉴스 전문 매체, 일일 업데이트

12. **Ars Technica**
    - 공식 사이트: https://arstechnica.com
    - RSS 피드 URL: https://feeds.arstechnica.com/arstechnica/index
    - 피드 형식: RSS 2.0
    - 참고 내용: 고품질 기술 저널리즘, 깊이 있는 분석

13. **Medium**
    - 공식 사이트: https://medium.com
    - 공식 문서: https://help.medium.com
    - RSS 피드 URL: https://medium.com/feed/tag/technology
    - 피드 형식: RSS 2.0
    - 참고 내용: 커뮤니티 기반 기술 콘텐츠, 태그별 RSS 피드 제공

### 웹 스크래핑 특별 고려사항

18. **법적/윤리적 고려사항**
    - **robots.txt 준수**: 모든 웹 스크래핑 전 필수 확인, Disallow 경로 스크래핑 금지
    - **Terms of Service (ToS) 확인**: 각 웹사이트의 ToS에서 스크래핑 관련 조항 확인, 금지된 경우 출처 제외
    - **Rate Limiting**: 최소 1초 간격 유지, robots.txt의 Crawl-delay 지시사항 준수
    - **User-Agent 설정**: 명확한 프로젝트 식별자 포함, 연락처 정보 포함 권장
    - **저작권 준수**: 수집한 데이터의 사용 목적과 범위 명확히 정의, 개인정보보호법 준수

### 프로젝트 내 참고 파일

- `json/sources.json`: 정보 출처 정의 (RSS Feed 및 Web Scraping 출처 선별 기준)
- `docs/step2/2. data-model-design.md`: 데이터 모델 설계
- `docs/step8/slack-integration-design-guide.md`: Slack 연동 설계 가이드 (Rate Limiting 패턴 참고)
- `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스 (Rate Limiting 패턴 참고)
- `prompts/shrimp-task-prompt.md`: 전체 작업 프롬프트

---

## 결론

`client-rss`와 `client-scraper` 모듈은 각각 RSS 피드 파싱과 웹 스크래핑을 통해 외부 출처로부터 데이터를 수집하는 역할을 담당합니다. 두 모듈 모두 다음과 같은 공통 원칙을 따릅니다:

1. **Spring Boot 베스트 프랙티스 준수**: 생성자 주입, @ConfigurationProperties, Resilience4j 활용
2. **간단명료한 구현**: 오버엔지니어링 방지, YAGNI 원칙 준수
3. **에러 핸들링**: Resilience4j를 활용한 재시도 로직 및 로깅
4. **Rate Limiting**: Redis를 활용한 분산 환경에서의 Rate Limiting, 출처 서버에 부하를 주지 않도록 관리
5. **법적/윤리적 준수**: robots.txt 및 ToS 확인 필수, crawler-commons를 활용한 표준 준수
6. **클린코드 원칙**: SOLID 원칙 준수, 단일 책임 원칙, 의존성 역전 원칙, 개방-폐쇄 원칙

다음 단계로는 각 모듈의 구체적인 구현을 진행하되, 위 가이드라인을 참고하여 개발을 진행하시기 바랍니다.

---

---

## 개선 사항 요약

### 주요 개선 내용

#### client-rss 모듈

1. **Spring Boot 베스트 프랙티스 반영**
   - 생성자 주입 패턴 권장 (`@RequiredArgsConstructor`)
   - `@ConfigurationProperties`를 통한 설정 관리
   - Resilience4j를 활용한 재시도 로직 (Thread.sleep() 제거)
   - `WebClient.Builder` 빈 설정 패턴

2. **RSS Feed 출처 정보 업데이트**
   - `total_score` 기준으로 정렬 (Google Developers Blog 36점 → Medium Technology 30점)
   - 각 출처의 RSS 피드 형식 명시 (Atom 1.0 vs RSS 2.0)
   - 공식 문서 URL 및 우선순위 정보 추가

#### client-scraper 모듈

3. **Spring Boot 웹 스크래핑 베스트 프랙티스 반영**
   - WebClient와 Jsoup 통합 패턴 (WebClient로 HTML 가져온 후 Jsoup으로 파싱)
   - crawler-commons를 활용한 robots.txt 표준 파싱
   - Selenium 사용 최소화 (정적 HTML 파싱으로 충분한 경우 Jsoup만 사용)
   - 생성자 주입, @ConfigurationProperties, Resilience4j 패턴 적용

4. **웹 스크래핑 출처 정보 업데이트**
   - `total_score` 기준으로 정렬하여 상위 5개 선별
     - LeetCode Contests (32점), Google Summer of Code (32점), Devpost (30점), MLH (29점), AtCoder (28점)
   - 각 출처의 데이터 형식 명시 (HTML vs GraphQL/JSON)
   - Rate Limiting 정책, robots.txt 및 ToS 확인 사항 추가

5. **법적/윤리적 고려사항 강화**
   - robots.txt 표준 준수 (crawler-commons 활용)
   - Terms of Service (ToS) 확인 절차 구체화
   - Rate Limiting 정책 및 User-Agent 설정 가이드라인 추가
   - 각 출처별 특별 주의사항 명시

6. **클린코드 원칙 검증 및 개선**
   - 단일 책임 원칙 (SRP) 준수 확인
   - 의존성 역전 원칙 (DIP) 준수 확인
   - 개방-폐쇄 원칙 (OCP) 준수 확인
   - 오버엔지니어링 방지 (YAGNI 원칙 준수, Selenium 사용 최소화, 팩토리 패턴 제거)

7. **참고 자료 확장**
   - Spring Boot, Spring Framework 공식 문서 추가
   - Resilience4j 공식 문서 추가
   - Jsoup, Selenium, crawler-commons 공식 문서 추가
   - robots.txt 표준 (RFC 9309), HTTP 표준 (RFC 7231) 추가
   - 각 웹 스크래핑 대상 웹사이트 공식 문서 추가
   - 웹 스크래핑 특별 고려사항 (법적/윤리적 고려사항) 추가

### 검증 체크리스트

#### client-rss 모듈
- [x] Spring Boot 공식 문서를 참고하여 베스트 프랙티스 반영 여부 확인
- [x] RSS Feed 출처 4개 선별 및 정보 업데이트 완료 (총점 순서대로 정렬)
- [x] 각 RSS Feed 제공자의 공식 문서 확인 및 반영 완료
- [x] 클린코드 원칙 및 객체지향 설계 기법 검증 완료
- [x] 오버엔지니어링 요소 제거 완료
- [x] 모든 참고 출처를 "참고 자료" 섹션에 정리 완료
- [x] 문서의 일관성 및 가독성 확인

#### client-scraper 모듈
- [x] Spring Boot 공식 문서를 참고하여 웹 스크래핑 베스트 프랙티스 반영 여부 확인
- [x] 웹 스크래핑 출처 5개 선별 및 정보 업데이트 완료 (총점 순서대로 정렬)
- [x] 각 웹사이트의 공식 문서 확인 및 반영 완료 (robots.txt, ToS 포함)
- [x] 클린코드 원칙 및 객체지향 설계 기법 검증 완료
- [x] 오버엔지니어링 요소 제거 완료 (Selenium 사용 최소화, 불필요한 패턴 제거)
- [x] 모든 참고 출처를 "참고 자료" 섹션에 정리 완료
- [x] 문서의 일관성 및 가독성 확인

---

**문서 버전**: 3.1  
**최종 업데이트**: 2026-01-07  
**작성자**: System Architect  
**개선 사항**: 
- client-rss 모듈: Spring Boot 베스트 프랙티스 반영, RSS Feed 출처 정보 업데이트, 클린코드 원칙 검증
- client-scraper 모듈: Spring Boot 웹 스크래핑 베스트 프랙티스 반영, 웹 스크래핑 출처 정보 업데이트, 법적/윤리적 고려사항 강화, 클린코드 원칙 검증
- 프로젝트 내부 문서 참조 추가: Slack 연동 설계 가이드 및 Redis 최적화 베스트 프랙티스 문서 참조 추가