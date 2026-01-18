# Slack 연동 설계 및 구현 가이드

**작성 일시**: 2026-01-07  
**대상 모듈**: `client-slack`  
**목적**: Spring Boot 애플리케이션에서 Slack 연동 기능을 구현하기 위한 베스트 프랙티스 설계 및 구현 가이드

## 목차

1. [개요](#개요)
2. [프로젝트 구조 분석](#프로젝트-구조-분석)
3. [Slack API 분석](#slack-api-분석)
4. [client-slack 모듈 설계](#client-slack-모듈-설계)
5. [구현 가이드](#구현-가이드)
6. [보안 고려사항](#보안-고려사항)
7. [참고 자료](#참고-자료)

---

## 1. 개요

### 1.1. Slack 연동 목적 및 사용 사례

`client-slack` 모듈은 프로젝트의 다양한 시스템 이벤트를 Slack 채널로 알림하는 기능을 제공합니다. 주요 목적은 다음과 같습니다:

- **배치 작업 모니터링**: Spring Batch 작업의 실행 상태 및 결과를 실시간으로 알림
- **에러 알림**: 시스템 에러 발생 시 즉시 개발팀에 알림
- **상태 변경 알림**: 정보 출처 상태 변경, API 엔드포인트 유효성 검증 실패 등 중요 이벤트 알림

### 1.2. 프로젝트에서의 Slack 활용 방안 (구체적 시나리오)

#### 배치 작업 완료/실패 알림 시나리오

**시나리오 1: SourceUpdateJob 실행 완료 알림**

- **트리거**: `SourceUpdateJob`의 `NotificationStep`에서 실행
- **알림 시점**:
  - Step별 실행 결과 알림:
    - `SourceDiscoveryStep`: AI LLM을 통한 출처 탐색 완료/실패
    - `SourceValidationStep`: 기존 출처의 API 엔드포인트 유효성 검증 완료/실패
    - `SourceComparisonStep`: 변경 사항 비교 완료/실패
    - `SourceUpdateStep`: `json/sources.json` 업데이트 완료/실패
  - Job 전체 실행 완료/실패 알림
- **포함 정보**:
  - 작업 이름: `SourceUpdateJob`
  - 실행 상태: 성공/실패
  - 실행 시간: 시작 시간 ~ 종료 시간
  - 처리된 항목 수: 새로 발견된 출처 수, 검증된 출처 수, 변경된 출처 수
  - 에러 정보: 실패 시 에러 메시지 및 스택 트레이스 (선택사항)

**시나리오 2: 배치 작업 실패 시 즉시 알림**

- **트리거**: Step 실행 중 예외 발생 시
- **알림 시점**: 예외 발생 즉시 (Job 완료 대기 없음)
- **포함 정보**:
  - 실패한 Step 이름
  - 에러 메시지
  - 발생 시간
  - 재시도 가능 여부

#### 에러 발생 시 알림 시나리오

**시나리오 1: LLM API 호출 실패**

- **트리거**: `SourceDiscoveryStep`에서 Anthropic Claude API 호출 실패
- **알림 조건**: 재시도 로직 실행 후 최종 실패 시
- **포함 정보**:
  - API 엔드포인트
  - 에러 코드 및 메시지
  - 재시도 횟수
  - 실패 시간

**시나리오 2: JSON 파싱 실패**

- **트리거**: LLM API 응답의 JSON 파싱 실패
- **포함 정보**:
  - 원본 응답 (일부)
  - 파싱 에러 메시지
  - 발생 시간

**시나리오 3: 파일 쓰기 실패**

- **트리거**: `json/sources.json` 파일 쓰기 실패
- **포함 정보**:
  - 파일 경로
  - 디스크 용량 정보 (가능한 경우)
  - 권한 에러 여부
  - 발생 시간

**시나리오 4: 심각도가 높은 예외 발생**

- **트리거**: 심각도가 "HIGH" 또는 "CRITICAL"인 예외 발생
- **포함 정보**:
  - 예외 타입
  - 예외 메시지
  - 스택 트레이스 (선택사항)
  - 발생 모듈 및 컨텍스트 정보
  - 발생 시간

#### 출처 상태 변경 알림 시나리오

**시나리오 1: 새로운 출처 발견**

- **트리거**: `SourceDiscoveryStep`에서 새로운 출처 발견
- **포함 정보**:
  - 출처 이름
  - 출처 타입 (API, RSS, Web Scraping)
  - 출처 URL
  - 우선순위 (Priority)
  - 총점 (total_score)

**시나리오 2: API 엔드포인트 유효성 검증 실패**

- **트리거**: `SourceValidationStep`에서 기존 출처의 API 엔드포인트 유효성 검증 실패
- **포함 정보**:
  - 출처 이름
  - API 엔드포인트 URL
  - HTTP 상태 코드
  - 에러 메시지
  - 검증 시간

**시나리오 3: Rate Limit 변경 감지**

- **트리거**: `SourceValidationStep`에서 Rate Limit 변경 감지
- **포함 정보**:
  - 출처 이름
  - 이전 Rate Limit 값
  - 새로운 Rate Limit 값
  - 변경 시간

**시나리오 4: 인증 방식 변경 감지**

- **트리거**: `SourceValidationStep`에서 인증 방식 변경 감지
- **포함 정보**:
  - 출처 이름
  - 이전 인증 방식
  - 새로운 인증 방식
  - 변경 시간

### 1.3. 모듈 구조 개요

`client-slack` 모듈은 다음과 같은 구조로 설계됩니다:

```
client/slack/
├── config/
│   └── SlackConfig.java
├── domain/
│   └── slack/
│       ├── client/
│       │   ├── SlackClient.java (인터페이스)
│       │   ├── SlackWebhookClient.java (구현체)
│       │   └── SlackBotClient.java (구현체, 선택사항)
│       ├── contract/
│       │   ├── SlackContract.java (인터페이스)
│       │   └── SlackDto.java (DTO)
│       ├── service/
│       │   ├── SlackNotificationService.java (인터페이스)
│       │   └── SlackNotificationServiceImpl.java (구현체)
│       └── builder/
│           └── SlackMessageBuilder.java (Block Kit 메시지 빌더)
└── util/
    └── SlackRateLimiter.java (Redis 기반 Rate Limiting)
```

---

## 2. 프로젝트 구조 분석

### 2.1. 전체 프로젝트 구조

프로젝트는 멀티모듈 MSA 아키텍처를 따르며, 다음과 같은 구조를 가집니다:

```
tech-n-ai/
├── api/              # API 서버 모듈들
├── batch/            # 배치 작업 모듈
├── common/           # 공통 모듈들
│   ├── core/        # 공통 유틸리티 및 설정
│   ├── exception/   # 예외 처리
│   ├── kafka/       # Kafka 이벤트 처리
│   └── security/    # 보안 설정
├── client/           # 외부 서비스 클라이언트 모듈들
│   ├── feign/       # OpenFeign 클라이언트
│   ├── rss/         # RSS 피드 파서
│   ├── scraper/     # 웹 스크래핑
│   └── slack/       # Slack 연동 (대상 모듈)
└── domain/           # 도메인 모듈들
    ├── aurora/      # Aurora MySQL (Command Side)
    └── mongodb/     # MongoDB Atlas (Query Side)
```

### 2.2. client 모듈 구조 분석

#### client/feign 모듈 구조

`client/feign` 모듈은 OpenFeign을 사용한 외부 API 클라이언트 모듈로, 다음과 같은 패턴을 따릅니다:

**패키지 구조**:
```
com.tech.n.ai.client.feign
├── config/
│   ├── OpenFeignConfig.java
│   └── SampleFeignConfig.java
├── domain/
│   ├── sample/
│   │   ├── api/          # API 인터페이스 정의 (Contract 구현체)
│   │   ├── client/        # Feign Client 인터페이스
│   │   ├── contract/      # Contract 인터페이스 및 DTO
│   │   └── mock/          # Mock 구현 (테스트용)
│   └── oauth/
│       ├── api/
│       ├── client/
│       ├── contract/
│       └── mock/
```

**주요 패턴**:

1. **Contract 패턴**: 비즈니스 로직 인터페이스와 Feign Client 인터페이스를 분리
   - `SampleContract`: 비즈니스 메서드 시그니처 정의
   - `SampleFeignClient`: Feign Client 인터페이스
   - `SampleApi`: Contract 구현체 (FeignClient 사용)

2. **DTO 정의**: Record 클래스를 사용한 불변 DTO
   ```java
   public class SampleDto {
       @Builder
       public record SampleApiRequest() {}
       
       @Builder
       public record SampleApiResponse() {}
   }
   ```

3. **설정 관리**: `@Configuration` 클래스에서 조건부 빈 생성
   ```java
   @Bean
   @ConditionalOnProperty(name = "feign-clients.sample.mode", havingValue = "mock")
   public SampleContract sampleMock() { return new SampleMock(); }
   ```

4. **테스트 패턴**: `@SpringBootTest` 사용, `classes`에 테스트 컨텍스트와 Config 클래스 지정
   ```java
   @SpringBootTest(classes = {
       FeignTestContext.class,
       SampleFeignConfig.class
   }, properties = {
       "spring.profiles.active=local",
   })
   ```

#### client/rss 및 client/scraper 모듈 참고

- **Rate Limiting 패턴**: Redis를 활용한 출처별 요청 간격 관리
- **에러 핸들링**: Resilience4j를 통한 재시도 로직
- **설정 관리**: `@ConfigurationProperties` 활용

### 2.3. 일관성 있는 설계 제안

`client/slack` 모듈은 `client/feign` 모듈의 패턴을 최대한 준수하여 일관성을 유지합니다:

- **패키지 구조**: `domain/slack/` 하위에 `client/`, `contract/`, `service/` 디렉토리 구성
- **인터페이스 기반 설계**: Contract 패턴 적용
- **DTO 정의**: Record 클래스 사용
- **설정 관리**: `@Configuration` 클래스에서 조건부 빈 생성
- **테스트 패턴**: `@SpringBootTest` 사용, Given-When-Then 패턴

---

## 3. Slack API 분석

### 3.1. Incoming Webhooks 분석

#### 개요

**Incoming Webhooks**는 Slack에 메시지를 전송하는 가장 간단한 방법입니다. Webhook URL을 통해 HTTP POST 요청으로 메시지를 전송할 수 있습니다.

#### Webhook URL 형식 및 생성 방법

- **URL 형식**: `https://hooks.slack.com/services/{T}/{B}/{K}`
  - `T`: Team ID
  - `B`: Bot ID
  - `K`: Webhook Key
- **생성 방법**:
  1. Slack 워크스페이스 설정 → Apps → Incoming Webhooks
  2. "Add to Slack" 클릭
  3. 채널 선택
  4. Webhook URL 생성 및 복사

#### 메시지 페이로드 구조

**기본 텍스트 메시지**:
```json
{
  "text": "Hello, World!"
}
```

**Block Kit 메시지**:
```json
{
  "blocks": [
    {
      "type": "section",
      "text": {
        "type": "mrkdwn",
        "text": "Hello, World!"
      }
    }
  ]
}
```

#### Rate Limiting 정책

- **Incoming Webhooks**: 일반적으로 초당 1개 메시지 제한
- **재시도 전략**: Rate Limit 초과 시 429 응답, 지수 백오프로 재시도 권장
- **에러 처리**: 429 응답 시 `Retry-After` 헤더 확인

### 3.2. Web API (Bot Token) 분석

#### 개요

**Web API (Bot Token)**는 OAuth 2.0 인증을 통해 더 많은 기능을 제공하는 API입니다.

#### Bot Token 발급 방법

1. Slack App 생성: https://api.slack.com/apps
2. OAuth & Permissions 설정
3. Bot Token Scopes 설정 (예: `chat:write`, `channels:read`)
4. OAuth 2.0 인증 플로우 실행
5. Bot Token 발급

#### chat.postMessage API 사용법

**엔드포인트**: `POST https://slack.com/api/chat.postMessage`

**요청 헤더**:
```
Authorization: Bearer {BOT_TOKEN}
Content-Type: application/json
```

**요청 본문**:
```json
{
  "channel": "#general",
  "text": "Hello, World!",
  "blocks": [...]
}
```

#### Rate Limiting 정책

- **Tier별 Rate Limit**: Tier 1 (1 req/sec), Tier 2 (20 req/min), Tier 3 (50 req/min) 등
- **재시도 전략**: 429 응답 시 `Retry-After` 헤더 확인, 지수 백오프 적용

### 3.3. Block Kit 분석

#### 개요

**Block Kit**은 Slack 메시지를 구조화된 형식으로 작성할 수 있는 프레임워크입니다.

#### Block Kit 구성 요소

**주요 Block 타입**:
- **Section Block**: 텍스트 및 액세서리 요소 표시
- **Divider Block**: 구분선
- **Context Block**: 보조 정보 표시 (작은 텍스트)
- **Actions Block**: 버튼, 선택 메뉴 등 인터랙티브 요소
- **Header Block**: 제목 표시
- **Image Block**: 이미지 표시

#### Block Kit Builder 활용

- **온라인 빌더**: https://app.slack.com/block-kit-builder
- **용도**: 메시지 레이아웃 시각화 및 JSON 생성

### 3.4. Redis를 활용한 Rate Limiting 패턴

#### 개요

Slack API의 Rate Limit을 준수하기 위해 Redis를 활용한 Rate Limiting을 구현합니다.

#### Key 네이밍 전략

- **Webhook의 경우**: `rate-limit:slack:webhook:{webhook-id}`
- **Bot API의 경우**: `rate-limit:slack:bot:{channel-id}`

#### 구현 패턴

```java
// 마지막 요청 시간 확인
String key = "rate-limit:slack:webhook:" + webhookId;
String lastRequestTime = redisTemplate.opsForValue().get(key);

if (lastRequestTime != null) {
    long elapsed = System.currentTimeMillis() - Long.parseLong(lastRequestTime);
    if (elapsed < MIN_INTERVAL_MS) {
        // Rate Limit 초과, 대기 또는 예외 발생
        long waitTime = MIN_INTERVAL_MS - elapsed;
        Thread.sleep(waitTime);
    }
}

// 요청 시간 저장
redisTemplate.opsForValue().set(
    key, 
    String.valueOf(System.currentTimeMillis()), 
    Duration.ofMinutes(1)
);
```

#### 분산 환경 지원

- 여러 인스턴스에서 동일한 Rate Limit 적용
- Redis를 통한 중앙 집중식 Rate Limiting 관리

### 3.5. Best Practices

1. **메시지 포맷**: Block Kit을 사용하여 구조화된 메시지 전송
2. **에러 처리**: 알림 실패는 시스템에 치명적이지 않으므로 로깅 후 계속 진행
3. **재시도 로직**: Resilience4j 또는 Spring Retry를 통한 재시도
4. **Rate Limiting**: Redis를 활용한 분산 Rate Limiting
5. **보안**: Webhook URL 및 Bot Token은 환경 변수로 관리

---

## 4. client-slack 모듈 설계

### 4.1. 패키지 구조 설계

```
com.tech.n.ai.client.slack
├── config/
│   └── SlackConfig.java
├── domain/
│   └── slack/
│       ├── client/
│       │   ├── SlackClient.java (인터페이스)
│       │   ├── SlackWebhookClient.java (구현체)
│       │   └── SlackBotClient.java (구현체, 선택사항)
│       ├── contract/
│       │   ├── SlackContract.java (인터페이스)
│       │   └── SlackDto.java (DTO)
│       ├── service/
│       │   ├── SlackNotificationService.java (인터페이스)
│       │   └── SlackNotificationServiceImpl.java (구현체)
│       └── builder/
│           └── SlackMessageBuilder.java (Block Kit 메시지 빌더)
└── util/
    └── SlackRateLimiter.java (Redis 기반 Rate Limiting)
```

### 4.2. 인터페이스 설계

#### SlackClient 인터페이스

```java
public interface SlackClient {
    void sendMessage(SlackMessage message);
    void sendMessage(String text);
    void sendMessage(String text, String channel);
}
```

#### SlackContract 인터페이스

```java
public interface SlackContract {
    void sendNotification(SlackDto.NotificationRequest request);
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendInfoNotification(String message);
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}
```

#### SlackNotificationService 인터페이스

```java
public interface SlackNotificationService {
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendInfoNotification(String message);
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}
```

### 4.3. 클래스 설계

#### SlackWebhookClient 구현

**책임**: Incoming Webhooks를 통한 메시지 전송

**주요 메서드**:
- `sendMessage(SlackMessage message)`: Block Kit 메시지 전송
- `sendMessage(String text)`: 간단한 텍스트 메시지 전송

**의존성**:
- `WebClient`: HTTP 요청
- `SlackRateLimiter`: Rate Limiting
- `SlackProperties`: 설정 관리

#### SlackBotClient 구현 (선택사항)

**책임**: Bot Token을 통한 메시지 전송

**주요 메서드**:
- `sendMessage(SlackMessage message, String channel)`: 특정 채널로 메시지 전송
- `sendMessage(String text, String channel)`: 간단한 텍스트 메시지 전송

**의존성**:
- `WebClient`: HTTP 요청
- `SlackRateLimiter`: Rate Limiting
- `SlackProperties`: 설정 관리

#### SlackNotificationServiceImpl 구현

**책임**: 알림 로직 처리 및 메시지 템플릿 적용

**주요 메서드**:
- `sendErrorNotification(String message, Throwable error)`: 에러 알림 전송
- `sendSuccessNotification(String message)`: 성공 알림 전송
- `sendInfoNotification(String message)`: 정보 알림 전송
- `sendBatchJobNotification(SlackDto.BatchJobResult result)`: 배치 작업 알림 전송

**의존성**:
- `SlackContract`: 메시지 전송
- `SlackMessageBuilder`: 메시지 포맷팅

#### SlackMessageBuilder 구현

**책임**: Block Kit 메시지 빌더 패턴 적용

**주요 메서드**:
- `addSection(String text)`: Section Block 추가
- `addDivider()`: Divider Block 추가
- `addContext(String text)`: Context Block 추가
- `build()`: 최종 메시지 생성

### 4.4. DTO 및 Contract 설계

#### SlackDto 정의

```java
public class SlackDto {
    @Builder
    public record NotificationRequest(
        String message,
        NotificationType type,
        Map<String, Object> context
    ) {}
    
    @Builder
    public record BatchJobResult(
        String jobName,
        JobStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int processedItems,
        String errorMessage
    ) {}
    
    public enum NotificationType {
        ERROR, SUCCESS, INFO, BATCH_JOB
    }
    
    public enum JobStatus {
        SUCCESS, FAILED, IN_PROGRESS
    }
}
```

### 4.5. 설정 관리 설계

#### application.yml 설정

```yaml
slack:
  webhook:
    url: ${SLACK_WEBHOOK_URL}
    enabled: true
  bot:
    token: ${SLACK_BOT_TOKEN}
    enabled: false
  default-channel: ${SLACK_DEFAULT_CHANNEL:#general}
  notification:
    level: INFO  # INFO, WARN, ERROR
  rate-limit:
    min-interval-ms: 1000  # 최소 요청 간격 (밀리초)
    enabled: true
```

#### SlackProperties 클래스

```java
@ConfigurationProperties(prefix = "slack")
public class SlackProperties {
    private Webhook webhook = new Webhook();
    private Bot bot = new Bot();
    private String defaultChannel = "#general";
    private Notification notification = new Notification();
    private RateLimit rateLimit = new RateLimit();
    
    // Getters and Setters
}
```

### 4.6. 에러 핸들링 설계

#### 커스텀 예외 정의

```java
public class SlackException extends RuntimeException {
    public SlackException(String message) {
        super(message);
    }
    
    public SlackException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class SlackApiException extends SlackException {
    private final int statusCode;
    
    public SlackApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
```

#### 재시도 로직

- **Resilience4j 사용**: 비동기 지원, Circuit Breaker 패턴 제공
- **재시도 정책**: 최대 3회, 지수 백오프 적용
- **에러 처리**: 알림 실패는 시스템에 치명적이지 않으므로 로깅 후 계속 진행

---

## 5. 구현 가이드

### 5.1. 의존성 추가

#### build.gradle

```gradle
dependencies {
    implementation project(':common-core')
    
    // Spring WebClient (이미 포함되어 있을 수 있음)
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Resilience4j (재시도 로직)
    implementation 'io.github.resilience4j:resilience4j-spring-boot3'
    implementation 'io.github.resilience4j:resilience4j-reactor'
}
```

### 5.2. 설정 파일 작성

#### application.yml

```yaml
slack:
  webhook:
    url: ${SLACK_WEBHOOK_URL:}
    enabled: ${SLACK_WEBHOOK_ENABLED:true}
  bot:
    token: ${SLACK_BOT_TOKEN:}
    enabled: ${SLACK_BOT_ENABLED:false}
  default-channel: ${SLACK_DEFAULT_CHANNEL:#general}
  notification:
    level: ${SLACK_NOTIFICATION_LEVEL:INFO}  # INFO, WARN, ERROR
  rate-limit:
    min-interval-ms: ${SLACK_RATE_LIMIT_MIN_INTERVAL_MS:1000}
    enabled: ${SLACK_RATE_LIMIT_ENABLED:true}

resilience4j:
  retry:
    configs:
      default:
        maxAttempts: 3
        waitDuration: 500ms
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - org.springframework.web.reactive.function.client.WebClientException
          - java.net.ConnectException
```

### 5.3. DTO 및 Contract 정의

#### SlackDto.java

```java
package com.tech.n.ai.client.slack.domain.slack.contract;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.Map;

public class SlackDto {
    
    @Builder
    public record NotificationRequest(
        String message,
        NotificationType type,
        Map<String, Object> context
    ) {}
    
    @Builder
    public record BatchJobResult(
        String jobName,
        JobStatus status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int processedItems,
        String errorMessage
    ) {}
    
    @Builder
    public record SlackMessage(
        String text,
        java.util.List<Block> blocks
    ) {}
    
    @Builder
    public record Block(
        String type,
        Map<String, Object> text,
        Map<String, Object> elements
    ) {}
    
    public enum NotificationType {
        ERROR, SUCCESS, INFO, BATCH_JOB
    }
    
    public enum JobStatus {
        SUCCESS, FAILED, IN_PROGRESS
    }
}
```

#### SlackContract.java

```java
package com.tech.n.ai.client.slack.domain.slack.contract;

public interface SlackContract {
    void sendNotification(SlackDto.NotificationRequest request);
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendInfoNotification(String message);
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}
```

### 5.4. 인터페이스 정의

#### SlackClient.java

```java
package com.tech.n.ai.client.slack.domain.slack.client;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;

public interface SlackClient {
    void sendMessage(SlackDto.SlackMessage message);
    void sendMessage(String text);
    void sendMessage(String text, String channel);
}
```

### 5.5. 클라이언트 구현

#### SlackWebhookClient.java

```java
package com.tech.n.ai.client.slack.domain.slack.client;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import com.tech.n.ai.client.slack.util.SlackRateLimiter;
import com.tech.n.ai.client.slack.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class SlackWebhookClient implements SlackClient {
    
    private final WebClient webClient;
    private final SlackProperties properties;
    private final SlackRateLimiter rateLimiter;
    
    @Override
    public void sendMessage(SlackDto.SlackMessage message) {
        if (!properties.getWebhook().isEnabled()) {
            log.debug("Slack webhook is disabled");
            return;
        }
        
        String webhookUrl = properties.getWebhook().getUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack webhook URL is not configured");
            return;
        }
        
        // Rate Limiting 확인
        rateLimiter.checkAndWait("webhook", properties.getRateLimit().getMinIntervalMs());
        
        try {
            webClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(message)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.debug("Slack message sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Slack message", e);
            // 알림 실패는 시스템에 치명적이지 않으므로 예외를 다시 던지지 않음
        }
    }
    
    @Override
    public void sendMessage(String text) {
        SlackDto.SlackMessage message = SlackDto.SlackMessage.builder()
            .text(text)
            .build();
        sendMessage(message);
    }
    
    @Override
    public void sendMessage(String text, String channel) {
        // Webhook은 채널이 URL에 고정되어 있으므로 channel 파라미터 무시
        sendMessage(text);
    }
}
```

### 5.6. 메시지 빌더 구현

#### SlackMessageBuilder.java

```java
package com.tech.n.ai.client.slack.domain.slack.builder;

import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlackMessageBuilder {
    private final List<Map<String, Object>> blocks = new ArrayList<>();
    
    public SlackMessageBuilder addSection(String text) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "section");
        
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);
        block.put("text", textObj);
        
        blocks.add(block);
        return this;
    }
    
    public SlackMessageBuilder addDivider() {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "divider");
        blocks.add(block);
        return this;
    }
    
    public SlackMessageBuilder addContext(String text) {
        Map<String, Object> block = new HashMap<>();
        block.put("type", "context");
        
        List<Map<String, Object>> elements = new ArrayList<>();
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "mrkdwn");
        textObj.put("text", text);
        elements.add(textObj);
        block.put("elements", elements);
        
        blocks.add(block);
        return this;
    }
    
    public SlackDto.SlackMessage build() {
        return SlackDto.SlackMessage.builder()
            .blocks(blocks)
            .build();
    }
}
```

### 5.7. Contract 구현 (SlackApi)

#### SlackApi.java

```java
package com.tech.n.ai.client.slack.domain.slack.api;

import com.tech.n.ai.client.slack.domain.slack.client.SlackClient;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import com.tech.n.ai.client.slack.domain.slack.builder.SlackMessageBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;

@Slf4j
@RequiredArgsConstructor
public class SlackApi implements SlackContract {
    
    private final SlackClient slackClient;
    
    @Override
    public void sendNotification(SlackDto.NotificationRequest request) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        
        switch (request.type()) {
            case ERROR:
                builder.addSection("*❌ 에러*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case SUCCESS:
                builder.addSection("*✅ 성공*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case INFO:
                builder.addSection("*ℹ️ 정보*")
                    .addDivider()
                    .addSection(request.message());
                break;
            case BATCH_JOB:
                // 배치 작업 알림은 별도 메서드에서 처리
                break;
        }
        
        if (request.context() != null && !request.context().isEmpty()) {
            String contextText = request.context().entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            builder.addContext(contextText);
        }
        
        SlackDto.SlackMessage message = builder.build();
        slackClient.sendMessage(message);
    }
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*❌ 에러 발생*")
            .addDivider()
            .addSection("메시지: " + message);
        
        if (error != null) {
            builder.addSection("에러 타입: `" + error.getClass().getSimpleName() + "`")
                .addSection("에러 메시지: " + error.getMessage());
        }
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendSuccessNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*✅ 성공*")
            .addDivider()
            .addSection(message);
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendInfoNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*ℹ️ 정보*")
            .addDivider()
            .addSection(message);
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
    
    @Override
    public void sendBatchJobNotification(SlackDto.BatchJobResult result) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        
        String statusEmoji = result.status() == SlackDto.JobStatus.SUCCESS ? "✅" : "❌";
        builder.addSection("*" + statusEmoji + " 배치 작업: " + result.jobName() + "*")
            .addDivider()
            .addSection("상태: " + result.status())
            .addSection("시작 시간: " + result.startTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .addSection("종료 시간: " + result.endTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .addSection("처리된 항목 수: " + result.processedItems());
        
        if (result.status() == SlackDto.JobStatus.FAILED && result.errorMessage() != null) {
            builder.addDivider()
                .addSection("*에러 정보*")
                .addSection(result.errorMessage());
        }
        
        SlackDto.SlackMessage slackMessage = builder.build();
        slackClient.sendMessage(slackMessage);
    }
}
```

### 5.8. 알림 서비스 구현

#### SlackNotificationServiceImpl.java

```java
package com.tech.n.ai.client.slack.domain.slack.service;

import com.tech.n.ai.client.slack.domain.slack.builder.SlackMessageBuilder;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationServiceImpl implements SlackNotificationService {
    
    private final SlackContract slackContract;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*❌ 에러 발생*")
            .addDivider()
            .addSection("메시지: " + message);
        
        if (error != null) {
            builder.addSection("에러 타입: `" + error.getClass().getSimpleName() + "`")
                .addSection("에러 메시지: " + error.getMessage());
        }
        
        builder.addContext("발생 시간: " + java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        slackContract.sendNotification(
            SlackDto.NotificationRequest.builder()
                .message(builder.build().toString())
                .type(SlackDto.NotificationType.ERROR)
                .build()
        );
    }
    
    @Override
    public void sendSuccessNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*✅ 성공*")
            .addDivider()
            .addSection(message)
            .addContext("시간: " + java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        slackContract.sendNotification(
            SlackDto.NotificationRequest.builder()
                .message(builder.build().toString())
                .type(SlackDto.NotificationType.SUCCESS)
                .build()
        );
    }
    
    @Override
    public void sendInfoNotification(String message) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        builder.addSection("*ℹ️ 정보*")
            .addDivider()
            .addSection(message)
            .addContext("시간: " + java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER));
        
        slackContract.sendNotification(
            SlackDto.NotificationRequest.builder()
                .message(builder.build().toString())
                .type(SlackDto.NotificationType.INFO)
                .build()
        );
    }
    
    @Override
    public void sendBatchJobNotification(SlackDto.BatchJobResult result) {
        SlackMessageBuilder builder = new SlackMessageBuilder();
        
        String statusEmoji = result.status() == SlackDto.JobStatus.SUCCESS ? "✅" : "❌";
        builder.addSection("*" + statusEmoji + " 배치 작업: " + result.jobName() + "*")
            .addDivider()
            .addSection("상태: " + result.status())
            .addSection("시작 시간: " + result.startTime().format(DATE_TIME_FORMATTER))
            .addSection("종료 시간: " + result.endTime().format(DATE_TIME_FORMATTER))
            .addSection("처리된 항목 수: " + result.processedItems());
        
        if (result.status() == SlackDto.JobStatus.FAILED && result.errorMessage() != null) {
            builder.addDivider()
                .addSection("*에러 정보*")
                .addSection(result.errorMessage());
        }
        
        builder.addContext("실행 시간: " + 
            java.time.Duration.between(result.startTime(), result.endTime()).getSeconds() + "초");
        
        slackContract.sendNotification(
            SlackDto.NotificationRequest.builder()
                .message(builder.build().toString())
                .type(SlackDto.NotificationType.BATCH_JOB)
                .build()
        );
    }
}
```

### 5.9. Rate Limiting 구현 (Redis 활용)

#### SlackRateLimiter.java

```java
package com.tech.n.ai.client.slack.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackRateLimiter {
    
    private static final String RATE_LIMIT_KEY_PREFIX = "rate-limit:slack:";
    private final RedisTemplate<String, String> redisTemplate;
    
    public void checkAndWait(String identifier, long minIntervalMs) {
        String key = RATE_LIMIT_KEY_PREFIX + identifier;
        String lastRequestTime = redisTemplate.opsForValue().get(key);
        
        if (lastRequestTime != null) {
            long lastTime = Long.parseLong(lastRequestTime);
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastTime;
            
            if (elapsed < minIntervalMs) {
                long waitTime = minIntervalMs - elapsed;
                log.debug("Rate limiting: waiting {}ms for {}", waitTime, identifier);
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiting interrupted", e);
                }
            }
        }
        
        // 요청 시간 저장 (TTL: 1분)
        redisTemplate.opsForValue().set(
            key, 
            String.valueOf(System.currentTimeMillis()), 
            Duration.ofMinutes(1)
        );
    }
}
```

### 5.10. 설정 클래스 작성

#### SlackConfig.java

```java
package com.tech.n.ai.client.slack.config;

import com.tech.n.ai.client.slack.domain.slack.client.SlackClient;
import com.tech.n.ai.client.slack.domain.slack.client.SlackWebhookClient;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import com.tech.n.ai.client.slack.domain.slack.service.SlackNotificationService;
import com.tech.n.ai.client.slack.domain.slack.service.SlackNotificationServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SlackProperties.class)
public class SlackConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
    
    @Bean
    @ConditionalOnProperty(name = "slack.webhook.enabled", havingValue = "true")
    public SlackClient slackWebhookClient(
            WebClient.Builder webClientBuilder,
            SlackProperties properties,
            SlackRateLimiter rateLimiter) {
        return new SlackWebhookClient(
            webClientBuilder.build(),
            properties,
            rateLimiter
        );
    }
    
    @Bean
    public SlackContract slackContract(SlackClient slackClient) {
        return new com.tech.n.ai.client.slack.domain.slack.api.SlackApi(slackClient);
    }
    
    @Bean
    public SlackNotificationService slackNotificationService(SlackContract slackContract) {
        return new SlackNotificationServiceImpl(slackContract);
    }
}
```

### 5.11. 테스트 작성 가이드

#### 테스트 클래스 구조

**참고 파일**: `client/feign/src/test/java/com/ebson/shrimp/tm/demo/client/feign/SampleFeignTest.java`

```java
package com.tech.n.ai.client.slack;

import com.tech.n.ai.client.slack.config.SlackConfig;
import com.tech.n.ai.client.slack.domain.slack.service.SlackNotificationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {
    SlackTestContext.class,
    SlackConfig.class
}, properties = {
    "spring.profiles.active=local",
    "slack.webhook.enabled=false"  // 테스트 시 실제 Slack API 호출 방지
})
public class SlackClientTest {
    
    // SlackTestContext.java 예시
    // package com.tech.n.ai.client.slack;
    //
    // import com.tech.n.ai.client.slack.config.SlackConfig;
    // import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
    // import org.springframework.boot.web.reactive.function.client.WebClientAutoConfiguration;
    //
    // @ImportAutoConfiguration({
    //     SlackConfig.class,
    //     WebClientAutoConfiguration.class,
    // })
    // class SlackTestContext {
    // }
    
    @Autowired 
    private SlackNotificationService notificationService;
    
    // @Test는 주석 처리하여 기본적으로 비활성화
    // @DisplayName으로 테스트 목적 명시
    @DisplayName("에러 알림 전송 테스트")
    void testSendErrorNotification() {
        // given
        String message = "테스트 에러 메시지";
        Throwable error = new RuntimeException("테스트 예외");
        
        // when
        notificationService.sendErrorNotification(message, error);
        
        // then
        // Mock 서버 또는 로그 확인
        Assertions.assertDoesNotThrow(() -> 
            notificationService.sendErrorNotification(message, error)
        );
    }
    
    @DisplayName("배치 작업 알림 전송 테스트")
    void testSendBatchJobNotification() {
        // given
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now().minusMinutes(5);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
        SlackDto.BatchJobResult result = SlackDto.BatchJobResult.builder()
            .jobName("SourceUpdateJob")
            .status(SlackDto.JobStatus.SUCCESS)
            .startTime(startTime)
            .endTime(endTime)
            .processedItems(10)
            .build();
        
        // when
        notificationService.sendBatchJobNotification(result);
        
        // then
        Assertions.assertDoesNotThrow(() -> 
            notificationService.sendBatchJobNotification(result)
        );
    }
}
```

#### Mock 전략

1. **MockWebServer 활용**: Spring WebClient 테스트용 Mock 서버
2. **Redis Mock**: 테스트용 인메모리 Redis 또는 Mock 객체 사용
3. **환경 변수**: 테스트 시 `slack.webhook.enabled=false` 설정

#### 테스트 시나리오

- Webhook 메시지 전송 성공/실패
- Rate Limiting 동작 확인
- 에러 핸들링 확인
- 메시지 템플릿 포맷팅 확인
- 배치 작업 알림 전송 확인

---

## 6. 보안 고려사항

### 6.1. Webhook URL 보안 관리

- **환경 변수로 관리**: `SLACK_WEBHOOK_URL` 환경 변수 사용
- **Git에 커밋하지 않음**: `.gitignore`에 환경 변수 파일 추가
- **프로덕션 환경에서만 사용**: 개발 환경에서는 Mock 사용 권장

### 6.2. Bot Token 보안 관리

- **환경 변수로 관리**: `SLACK_BOT_TOKEN` 환경 변수 사용
- **Git에 커밋하지 않음**: `.gitignore`에 환경 변수 파일 추가
- **토큰 갱신 정책**: 정기적인 토큰 갱신 및 만료 관리

### 6.3. 민감 정보 처리

- **로그에 민감 정보 출력 금지**: Webhook URL 및 Bot Token은 로그에 출력하지 않음
- **설정 파일에 하드코딩 금지**: 모든 민감 정보는 환경 변수로 관리

---

## 7. 참고 자료

### 7.1. Spring Boot 공식 문서

- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
  - Spring Boot 4.0.1 버전 정보 및 기능
  - 자동 설정 (Auto-Configuration) 활용 방법
  - 설정 관리 베스트 프랙티스

- **Spring Framework 공식 문서**: https://spring.io/projects/spring-framework
  - Spring WebClient 사용법
  - 의존성 주입 및 빈 관리 패턴

- **Spring WebClient 공식 문서**: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
  - 비동기 HTTP 클라이언트 사용법
  - 연결 풀 설정
  - 타임아웃 설정

### 7.2. Slack API 공식 문서

- **Slack API 공식 문서**: https://api.slack.com/
  - Slack API 전체 개요
  - 인증 및 권한 관리

- **Slack Incoming Webhooks**: https://api.slack.com/messaging/webhooks
  - Incoming Webhooks 사용법
  - Webhook URL 생성 가이드
  - 메시지 포맷 가이드

- **Slack Web API**: https://api.slack.com/web
  - Web API 전체 개요
  - Bot Token 생성 가이드
  - `chat.postMessage` API 사용법

- **Slack Block Kit**: https://api.slack.com/block-kit
  - Block Kit 구성 요소
  - Block Kit Builder 활용 방법
  - 메시지 템플릿 설계 가이드

- **Slack Rate Limits**: https://api.slack.com/docs/rate-limits
  - Tier별 Rate Limit 정책
  - 재시도 전략
  - 에러 처리 방법

- **Slack API Best Practices**: https://api.slack.com/best-practices
  - Slack API 사용 베스트 프랙티스
  - 성능 최적화 가이드

### 7.3. 프로젝트 내부 문서

- **Redis 최적화 베스트 프랙티스**: `docs/step7/redis-optimization-best-practices.md`
  - Redis Rate Limiting 패턴
  - Key 네이밍 전략
  - TTL 설정 방법

- **RSS 및 Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md`
  - 클라이언트 모듈 구현 패턴
  - Rate Limiting 구현 예시

- **OAuth Provider 구현 가이드**: `docs/step6/oauth-provider-implementation-guide.md`
  - 외부 API 통합 패턴
  - 에러 핸들링 전략

- **프로젝트 최종 목표**: `docs/reference/shrimp-task-prompts-final-goal.md`
  - Slack 알림 사용 사례
  - 배치 작업 알림 시나리오

### 7.4. 라이브러리 공식 문서

- **Resilience4j**: https://resilience4j.readme.io/
  - 재시도 로직 구현
  - Circuit Breaker 패턴
  - 비동기 지원

- **Spring Retry**: https://github.com/spring-projects/spring-retry
  - 동기식 재시도 로직 (대안)

---

**작성 일시**: 2026-01-07  
**작성자**: System Architect  
**버전**: 1.0
