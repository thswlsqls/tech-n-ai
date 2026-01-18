# Slack Client 모듈

## 개요

`client-slack` 모듈은 프로젝트의 다양한 시스템 이벤트를 Slack 채널로 알림하는 클라이언트 모듈입니다. Incoming Webhooks 및 Bot Token을 통한 메시지 전송을 지원하며, Block Kit을 사용한 구조화된 메시지 포맷팅, Redis 기반 Rate Limiting, Resilience4j를 통한 재시도 로직을 제공합니다.

## 주요 기능

### 1. Slack 알림 전송
- **Incoming Webhooks**: Webhook URL을 통한 간단한 메시지 전송
- **Bot Token (선택사항)**: OAuth 2.0 인증을 통한 고급 기능 지원
- **Block Kit 메시지**: 구조화된 메시지 포맷팅
- **메시지 타입**: 에러, 성공, 정보, 배치 작업 알림

### 2. 알림 시나리오
- **배치 작업 모니터링**: Spring Batch 작업의 실행 상태 및 결과 알림
- **에러 알림**: 시스템 에러 발생 시 즉시 개발팀에 알림
- **상태 변경 알림**: 정보 출처 상태 변경, API 엔드포인트 유효성 검증 실패 등 중요 이벤트 알림

### 3. Rate Limiting
- **Redis 기반 Rate Limiting**: 분산 환경에서의 Rate Limit 관리
- **최소 요청 간격**: 기본 1초 (설정 가능)
- **Slack API Rate Limit 준수**: Incoming Webhooks (초당 1개), Bot API (Tier별 제한)

### 4. 에러 핸들링 및 재시도
- **Resilience4j 재시도**: 최대 3회, 지수 백오프 적용
- **알림 실패 처리**: 알림 실패는 시스템에 치명적이지 않으므로 로깅 후 계속 진행

## 아키텍처

### 패키지 구조

```
com.tech.n.ai.client.slack
├── config/
│   ├── SlackConfig.java (빈 설정)
│   └── SlackProperties.java (@ConfigurationProperties)
├── domain/
│   └── slack/
│       ├── api/
│       │   └── SlackApi.java (Contract 구현체)
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
├── util/
│   └── SlackRateLimiter.java (Redis 기반 Rate Limiting)
└── exception/
    └── SlackException.java (Slack 예외)
```

### 설계 패턴

#### 1. Contract 패턴
비즈니스 로직 인터페이스와 클라이언트 인터페이스를 분리하여 의존성 역전 원칙(DIP)을 준수합니다.

```java
// Contract 인터페이스 (비즈니스 로직)
public interface SlackContract {
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}

// Client 인터페이스 (HTTP 클라이언트)
public interface SlackClient {
    void sendMessage(SlackDto.SlackMessage message);
    void sendMessage(String text);
}

// API 구현체 (Contract 구현)
public class SlackApi implements SlackContract {
    private final SlackClient slackClient;
    // ...
}
```

#### 2. 서비스 계층
고수준 알림 서비스 인터페이스를 제공하여 사용 편의성을 높입니다.

```java
@Service
public class SlackNotificationServiceImpl implements SlackNotificationService {
    private final SlackContract slackContract;
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        // 메시지 포맷팅 및 전송
    }
}
```

#### 3. Builder 패턴
Block Kit 메시지를 쉽게 구성할 수 있도록 Builder 패턴을 제공합니다.

```java
SlackMessageBuilder builder = new SlackMessageBuilder();
builder.addSection("*❌ 에러 발생*")
    .addDivider()
    .addSection("메시지: " + message)
    .addContext("발생 시간: " + LocalDateTime.now());
SlackDto.SlackMessage message = builder.build();
```

## 기술 스택

### 의존성

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

### Slack API

- **Incoming Webhooks**: https://api.slack.com/messaging/webhooks
- **Web API (Bot Token)**: https://api.slack.com/web
- **Block Kit**: https://api.slack.com/block-kit
- **Rate Limits**: https://api.slack.com/docs/rate-limits

## 설정

### application-slack.yml

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
        max-attempts: 3
        wait-duration: 500ms
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - org.springframework.web.reactive.function.client.WebClientException
          - java.net.ConnectException
    instances:
      slackRetry:
        base-config: default
```

### 환경 변수

- `SLACK_WEBHOOK_URL`: Incoming Webhook URL (필수, Webhook 사용 시)
- `SLACK_WEBHOOK_ENABLED`: Webhook 활성화 여부 (기본값: true)
- `SLACK_BOT_TOKEN`: Bot Token (선택사항, Bot API 사용 시)
- `SLACK_BOT_ENABLED`: Bot API 활성화 여부 (기본값: false)
- `SLACK_DEFAULT_CHANNEL`: 기본 채널 (기본값: #general)
- `SLACK_NOTIFICATION_LEVEL`: 알림 레벨 (INFO, WARN, ERROR)
- `SLACK_RATE_LIMIT_MIN_INTERVAL_MS`: 최소 요청 간격 (밀리초, 기본값: 1000)

### Webhook URL 생성 방법

1. Slack 워크스페이스 설정 → Apps → Incoming Webhooks
2. "Add to Slack" 클릭
3. 채널 선택
4. Webhook URL 생성 및 복사

## 사용 예시

### 에러 알림

```java
@Service
@RequiredArgsConstructor
public class ErrorHandler {
    private final SlackNotificationService slackNotificationService;
    
    public void handleError(String message, Throwable error) {
        slackNotificationService.sendErrorNotification(message, error);
    }
}
```

### 배치 작업 알림

```java
@Service
@RequiredArgsConstructor
public class BatchJobListener {
    private final SlackNotificationService slackNotificationService;
    
    @AfterJob
    public void afterJob(JobExecution jobExecution) {
        SlackDto.BatchJobResult result = SlackDto.BatchJobResult.builder()
            .jobName(jobExecution.getJobInstance().getJobName())
            .status(jobExecution.getStatus() == BatchStatus.COMPLETED 
                ? SlackDto.JobStatus.SUCCESS 
                : SlackDto.JobStatus.FAILED)
            .startTime(jobExecution.getStartTime())
            .endTime(jobExecution.getEndTime())
            .processedItems(jobExecution.getStepExecutions().stream()
                .mapToInt(StepExecution::getWriteCount)
                .sum())
            .errorMessage(jobExecution.getFailureExceptions().isEmpty() 
                ? null 
                : jobExecution.getFailureExceptions().get(0).getMessage())
            .build();
        
        slackNotificationService.sendBatchJobNotification(result);
    }
}
```

### 성공/정보 알림

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final SlackNotificationService slackNotificationService;
    
    public void notifySuccess(String message) {
        slackNotificationService.sendSuccessNotification(message);
    }
    
    public void notifyInfo(String message) {
        slackNotificationService.sendInfoNotification(message);
    }
}
```

## 보안 고려사항

### 1. Webhook URL 보안 관리
- **환경 변수로 관리**: `SLACK_WEBHOOK_URL` 환경 변수 사용
- **Git에 커밋하지 않음**: `.gitignore`에 환경 변수 파일 추가
- **프로덕션 환경에서만 사용**: 개발 환경에서는 Mock 사용 권장

### 2. Bot Token 보안 관리
- **환경 변수로 관리**: `SLACK_BOT_TOKEN` 환경 변수 사용
- **Git에 커밋하지 않음**: `.gitignore`에 환경 변수 파일 추가
- **토큰 갱신 정책**: 정기적인 토큰 갱신 및 만료 관리

### 3. 민감 정보 처리
- **로그에 민감 정보 출력 금지**: Webhook URL 및 Bot Token은 로그에 출력하지 않음
- **설정 파일에 하드코딩 금지**: 모든 민감 정보는 환경 변수로 관리

## 테스트

### 테스트 컨텍스트

```java
@ImportAutoConfiguration({
    WebFluxAutoConfiguration.class,
})
@Import({
    SlackConfig.class,
})
class SlackTestContext {
}
```

### 테스트 예시

```java
@SpringBootTest(classes = {
    SlackTestContext.class,
    SlackConfig.class
}, properties = {
    "spring.profiles.active=local",
    "slack.webhook.enabled=false"  // 테스트 시 실제 Slack API 호출 방지
})
public class SlackClientTest {
    @Autowired
    private SlackNotificationService notificationService;
    
    @Test
    @DisplayName("에러 알림 전송 테스트")
    void testSendErrorNotification() {
        // given
        String message = "테스트 에러 메시지";
        Throwable error = new RuntimeException("테스트 예외");
        
        // when
        notificationService.sendErrorNotification(message, error);
        
        // then
        assertDoesNotThrow(() -> 
            notificationService.sendErrorNotification(message, error)
        );
    }
    
    @Test
    @DisplayName("배치 작업 알림 전송 테스트")
    void testSendBatchJobNotification() {
        // given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);
        LocalDateTime endTime = LocalDateTime.now();
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
        assertDoesNotThrow(() -> 
            notificationService.sendBatchJobNotification(result)
        );
    }
}
```

## 참고 문서

### 프로젝트 내부 문서

- **Slack 연동 설계 가이드**: `docs/step8/slack-integration-design-guide.md`
- **RSS 및 Scraper 모듈 분석**: `docs/step8/rss-scraper-modules-analysis.md` (Rate Limiting 패턴 참고)
- **Redis 최적화 베스트 프랙티스**: `docs/step7/redis-optimization-best-practices.md` (Rate Limiting 패턴 참고)

### 공식 문서

- [Slack API 공식 문서](https://api.slack.com/)
- [Slack Incoming Webhooks](https://api.slack.com/messaging/webhooks)
- [Slack Web API](https://api.slack.com/web)
- [Slack Block Kit](https://api.slack.com/block-kit)
- [Slack Rate Limits](https://api.slack.com/docs/rate-limits)
- [Slack API Best Practices](https://api.slack.com/best-practices)
- [Spring WebClient 공식 문서](https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html)
- [Resilience4j 공식 문서](https://resilience4j.readme.io/)

