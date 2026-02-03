# Slack 연동 설계 및 구현 가이드 프롬프트

## 역할 및 목표 설정

당신은 **Spring Boot 애플리케이션 아키텍트 및 Slack 통합 전문가**입니다. 프로젝트의 전체 구조를 분석하여 `client-slack` 모듈에 Slack 연동 기능을 구현하기 위한 베스트 프랙티스 설계 및 구현 가이드를 작성해야 합니다.

## 작업 범위

다음 작업을 수행하여 `docs/step8/slack-integration-design-guide.md` 파일을 생성하세요:

1. **프로젝트 구조 분석**
   - 전체 멀티모듈 구조 파악
   - `client` 모듈들의 구조 및 패턴 분석
   - `client/feign` 모듈의 구조를 참고하여 일관된 설계 제안
   - `client/slack` 모듈의 현재 상태 확인

2. **관련 설계 문서 분석**
   - `docs/step8/`, `docs/step7/`, `docs/step6/` 폴더의 모든 관련 설계서 참고
   - `docs/step2/` 폴더의 API 설계 및 데이터 모델 설계 참고
   - `docs/step1/` 폴더의 프로젝트 구조 설계 참고
   - `docs/reference/shrimp-task-prompts-final-goal.md`의 프로젝트 목표 확인

3. **Slack API 공식 문서 기반 분석**
   - Slack Incoming Webhooks 공식 문서 분석
   - Slack Web API (Bot Token) 공식 문서 분석
   - Slack Block Kit 공식 문서 분석
   - Rate Limiting 및 Best Practices 확인

4. **Spring Boot 베스트 프랙티스 적용**
   - Spring WebClient를 사용한 HTTP 클라이언트 패턴
   - Spring Boot 설정 관리 (application.yml)
   - Spring의 의존성 주입 및 빈 관리 패턴
   - 에러 핸들링 및 재시도 로직 (Spring Retry 또는 Resilience4j)

5. **클린코드 원칙 및 객체지향 설계**
   - 단일 책임 원칙 (SRP) 준수
   - 의존성 역전 원칙 (DIP) 적용
   - 개방-폐쇄 원칙 (OCP) 준수
   - 적절한 디자인 패턴 적용 (전략 패턴, 팩토리 패턴 등)

## 분석 전략 (Chain of Thought)

### 1단계: 프로젝트 구조 분석

**분석 대상:**

1. **전체 프로젝트 구조:**
   - `shrimp-rules.md`: 프로젝트 기술 스택 및 아키텍처 확인
   - `build.gradle`: 루트 프로젝트 설정 확인
   - `settings.gradle`: 모듈 구조 확인

2. **client 모듈 구조:**
   - `client/feign/`: OpenFeign 클라이언트 구조 분석
     - 패키지 구조: `domain/{domain}/api`, `client`, `contract`, `mock`, `config`
     - 인터페이스 기반 설계 패턴
     - DTO 및 Contract 정의 방식
   - `client/rss/`: RSS 모듈 구조 (참고)
   - `client/scraper/`: Scraper 모듈 구조 (참고)
   - `client/slack/`: 현재 상태 확인

3. **common 모듈 구조:**
   - `common/core/`: 공통 유틸리티 및 설정
   - `common/exception/`: 예외 처리 패턴
   - `common/kafka/`: 이벤트 처리 패턴

4. **관련 설계 문서:**
   - `docs/step8/rss-scraper-modules-analysis.md`: 클라이언트 모듈 구현 패턴 참고
   - `docs/step7/redis-optimization-best-practices.md`: Rate Limiting 패턴 참고
   - `docs/step6/oauth-provider-implementation-guide.md`: 외부 API 통합 패턴 참고
   - `docs/step2/1. api-endpoint-design.md`: API 설계 원칙 참고
   - `docs/step2/2. data-model-design.md`: 데이터 모델 설계 원칙 참고
   - `docs/reference/shrimp-task-prompts-final-goal.md`: 프로젝트 목표 및 Slack 사용 사례 확인

**분석 항목:**
- `client/feign` 모듈의 패키지 구조 및 네이밍 컨벤션
- 인터페이스 기반 설계 패턴
- DTO 및 Contract 정의 방식
- 설정 관리 방식 (application.yml)
- 에러 핸들링 패턴
- 테스트 작성 패턴

### 2단계: Slack API 공식 문서 기반 분석

**신뢰할 수 있는 공식 기술 문서만 참고:**

- **Slack Incoming Webhooks**:
  - Slack Incoming Webhooks 공식 문서: https://api.slack.com/messaging/webhooks
  - Webhook URL 생성 가이드: https://api.slack.com/messaging/webhooks#getting-started
  - 메시지 포맷 가이드: https://api.slack.com/reference/messaging/payload

- **Slack Web API (Bot Token)**:
  - Slack Web API 공식 문서: https://api.slack.com/web
  - Bot Token 생성 가이드: https://api.slack.com/authentication/token-types#bot
  - Web API 메서드 목록: https://api.slack.com/methods
  - `chat.postMessage` API: https://api.slack.com/methods/chat.postMessage

- **Slack Block Kit**:
  - Block Kit 공식 문서: https://api.slack.com/block-kit
  - Block Kit Builder: https://app.slack.com/block-kit-builder
  - Block Kit 구성 요소: https://api.slack.com/reference/block-kit/blocks

- **Slack Rate Limiting**:
  - Rate Limits 공식 문서: https://api.slack.com/docs/rate-limits
  - Tier별 Rate Limit 정책 확인

- **Slack Best Practices**:
  - Slack API Best Practices: https://api.slack.com/best-practices

**검증 원칙:**
- 공식 문서의 예제 코드만 인용
- 최신 버전 정보 확인
- 공식 문서에 없는 내용은 추측하지 않음
- 불확실한 내용은 명시적으로 표시
- 오버엔지니어링 방지: 필요한 기능만 구현, 불필요한 복잡도 제거

**분석 항목:**

1. **Incoming Webhooks**
   - Webhook URL 형식 및 생성 방법
   - 메시지 페이로드 구조
   - 텍스트 메시지 전송 방법
   - Block Kit 메시지 전송 방법
   - 첨부 파일 지원 여부
   - Rate Limiting 정책

2. **Web API (Bot Token)**
   - Bot Token 발급 방법
   - OAuth 2.0 인증 플로우
   - `chat.postMessage` API 사용법
   - 채널 지정 방법
   - 사용자 멘션 방법
   - 스레드 응답 방법
   - Rate Limiting 정책

3. **Block Kit**
   - Block Kit 구성 요소 (Section, Divider, Actions 등)
   - Block Kit Builder 활용 방법
   - 메시지 템플릿 설계
   - 에러 알림 템플릿
   - 성공 알림 템플릿
   - 정보 알림 템플릿
   - 배치 작업 완료 알림 템플릿

4. **Rate Limiting 및 Best Practices**
   - Tier별 Rate Limit 확인
   - 재시도 전략
   - 에러 처리 방법
   - 메시지 큐잉 전략 (필요 시)
   - **Redis를 활용한 Rate Limiting 패턴** (중요)
     - `docs/step7/redis-optimization-best-practices.md` 참고
     - Key 형식: `rate-limit:slack:{channel-name}` 또는 `rate-limit:slack:webhook`
     - TTL 설정 고려 (예: 1분)
     - 분산 환경에서의 Rate Limiting 지원
     - 마지막 요청 시간 저장 및 최소 간격 확인

### 3단계: Spring Boot 베스트 프랙티스 적용

**검증 항목:**

1. **HTTP 클라이언트 선택**
   - Spring WebClient vs RestTemplate (WebClient 권장)
   - 비동기 처리 패턴
   - 연결 풀 설정

2. **설정 관리**
   - `application.yml` 기반 설정
   - 환경 변수 활용
   - 프로파일별 설정 분리

3. **의존성 주입 및 빈 관리**
   - 인터페이스 기반 설계
   - `@Configuration` 클래스 활용
   - `@Bean` 메서드 정의

4. **에러 핸들링**
   - Spring의 `@ControllerAdvice` 패턴 (필요 시)
   - 커스텀 예외 정의
   - 재시도 로직 (Spring Retry 또는 Resilience4j)

5. **로깅**
   - SLF4J + Logback 활용
   - 적절한 로그 레벨 설정

**개선 방향:**
- Spring Boot 공식 문서 및 Spring 공식 가이드를 참고하여 베스트 프랙티스 반영
- 불필요한 복잡성 제거 및 Spring 생태계 표준 패턴 적용
- Spring Boot의 자동 설정(Auto-Configuration) 활용 가능 여부 검토

### 4단계: 클린코드 원칙 및 객체지향 설계

**검증 항목:**

#### 클린코드 원칙
- **단일 책임 원칙 (SRP)**: 각 클래스가 하나의 책임만 가지는지 확인
  - Webhook 클라이언트: Webhook 메시지 전송만 담당
  - Bot API 클라이언트: Bot API 메시지 전송만 담당
  - 메시지 빌더: 메시지 포맷팅만 담당
  - 알림 서비스: 알림 로직만 담당

- **의존성 역전 원칙 (DIP)**: 인터페이스 기반 설계인지 확인
  - `SlackClient` 인터페이스 정의
  - `SlackWebhookClient`, `SlackBotClient` 구현
  - `SlackNotificationService` 인터페이스 정의

- **개방-폐쇄 원칙 (OCP)**: 확장에는 열려있고 수정에는 닫혀있는지 확인
  - 새로운 메시지 템플릿 추가 시 기존 코드 수정 없이 확장 가능
  - 새로운 알림 타입 추가 시 기존 코드 수정 없이 확장 가능

- **명명 규칙**: 클래스, 메서드, 변수명이 명확하고 일관성 있는지 확인
  - `client/feign` 모듈의 네이밍 컨벤션 준수

#### 객체지향 설계
- **인터페이스 분리**: 불필요한 의존성을 만들지 않는지 확인
- **전략 패턴**: Webhook과 Bot API를 전략 패턴으로 구현 가능한지 검토
- **빌더 패턴**: Block Kit 메시지 빌더에 빌더 패턴 적용 검토
- **팩토리 패턴**: 클라이언트 생성 로직이 적절히 캡슐화되어 있는지 확인

**개선 방향:**
- 현재 제안된 구조가 위 원칙들을 준수하는지 검증
- 개선이 필요한 부분을 구체적으로 제시
- 코드 예시를 클린코드 원칙에 맞게 제시

### 5단계: 오버엔지니어링 방지

**주의사항:**
- **불필요한 추상화 금지**: 현재 요구사항에 맞는 수준의 추상화만 유지
- **과도한 디자인 패턴 사용 금지**: 실제 필요에 맞는 패턴만 적용
- **미래 확장성을 위한 과도한 설계 금지**: YAGNI (You Aren't Gonna Need It) 원칙 준수
- **요청하지 않은 기능 추가 금지**: 설계 가이드 작성에 집중, 실제 구현 코드 작성은 제외

**검증 기준:**
- 제안된 구조가 현재 요구사항을 충족하는 최소한의 복잡도인지 확인
- 각 클래스와 인터페이스가 실제로 필요한지 검증
- 불필요한 레이어나 래퍼 클래스가 없는지 확인

**프로젝트에서의 Slack 사용 사례 확인:**
- `docs/reference/shrimp-task-prompts-final-goal.md`에서 Slack 알림 사용 사례 확인:
  - **배치 작업 완료/실패 알림** (구체적 시나리오)
    - `SourceUpdateJob`의 `NotificationStep`에서 사용
    - Step별 실행 결과 알림 (SourceDiscoveryStep, SourceValidationStep, SourceComparisonStep, SourceUpdateStep)
    - Job 전체 실행 완료/실패 알림
    - 실행 시간, 처리된 항목 수, 에러 정보 포함
  - **에러 발생 시 알림** (구체적 시나리오)
    - LLM API 호출 실패 시 재시도 후 최종 실패 알림
    - JSON 파싱 실패 시 알림
    - 파일 쓰기 실패 시 알림
    - 배치 작업 실패 시 즉시 알림
    - 심각도가 "HIGH" 또는 "CRITICAL"인 예외 발생 시 알림
  - **출처 상태 변경 알림** (구체적 시나리오)
    - 새로운 출처 발견 시 알림
    - 기존 출처의 API 엔드포인트 유효성 검증 실패 시 알림
    - Rate Limit 변경 감지 시 알림
    - 인증 방식 변경 감지 시 알림
- 실제 필요한 기능만 설계에 포함

### 6단계: 신뢰할 수 있는 공식 출처만 참고

**참고 가능한 출처:**
- Spring Boot 공식 문서: https://spring.io/projects/spring-boot
- Spring Framework 공식 문서: https://spring.io/projects/spring-framework
- Spring WebClient 공식 문서: https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html
- Slack API 공식 문서: https://api.slack.com/
- Slack Incoming Webhooks: https://api.slack.com/messaging/webhooks
- Slack Web API: https://api.slack.com/web
- Slack Block Kit: https://api.slack.com/block-kit
- Slack Rate Limits: https://api.slack.com/docs/rate-limits

**참고 불가능한 출처:**
- 개인 블로그나 비공식 튜토리얼
- Stack Overflow (공식 문서 우선 참고)
- 비공식 GitHub 저장소 (공식 저장소만 참고)

**참고 출처 정리:**
문서 마지막에 "참고 자료" 섹션을 확장하여 다음 정보를 포함:
- 각 참고 출처의 URL
- 참고한 내용의 요약
- 해당 출처에서 확인한 주요 정보

## 출력 형식 및 구조

다음 구조로 `docs/step8/slack-integration-design-guide.md` 파일을 생성하세요:

```markdown
# Slack 연동 설계 및 구현 가이드

## 목차
1. 개요
2. 프로젝트 구조 분석
3. Slack API 분석
4. client-slack 모듈 설계
5. 구현 가이드
6. 보안 고려사항
7. 참고 자료

## 1. 개요
- Slack 연동 목적 및 사용 사례
- 프로젝트에서의 Slack 활용 방안 (구체적 시나리오 포함)
  - 배치 작업 완료/실패 알림 시나리오
  - 에러 발생 시 알림 시나리오
  - 출처 상태 변경 알림 시나리오
- 모듈 구조 개요

## 2. 프로젝트 구조 분석
- 전체 프로젝트 구조
- client 모듈 구조 분석 (feign, rss, scraper 참고)
- client/feign 모듈의 패턴 분석
- 일관성 있는 설계 제안

## 3. Slack API 분석
- Incoming Webhooks 분석
- Web API (Bot Token) 분석
- Block Kit 분석
- Rate Limiting 정책
- Redis를 활용한 Rate Limiting 패턴
- Best Practices

## 4. client-slack 모듈 설계
- 패키지 구조 설계
- 인터페이스 설계
- 클래스 설계
- DTO 및 Contract 설계
- 설정 관리 설계
- 에러 핸들링 설계

## 5. 구현 가이드
- 의존성 추가
- 설정 파일 작성
- 인터페이스 구현
- 클라이언트 구현
- 메시지 템플릿 구현
- 알림 서비스 구현
- Rate Limiting 구현 (Redis 활용)
- 테스트 작성 가이드

## 6. 보안 고려사항
- Webhook URL 보안 관리
- Bot Token 보안 관리
- 환경 변수 활용
- 민감 정보 처리

## 7. 참고 자료
- 모든 참고 출처 정리
- 각 출처의 URL 및 주요 정보
```

## 작업 요구사항 상세

### 1. 프로젝트 구조 분석 및 일관성 유지

**분석 대상:**
- `client/feign` 모듈의 패키지 구조:
  ```
  com.tech.n.ai.client.feign
  ├── config/
  ├── domain/
  │   ├── {domain}/
  │   │   ├── api/          # API 인터페이스 정의
  │   │   ├── client/        # Feign Client 인터페이스
  │   │   ├── contract/      # Contract 인터페이스 및 DTO
  │   │   └── mock/          # Mock 구현 (테스트용)
  │   └── sample/           # 샘플 도메인
  ```

**적용 방안:**
- `client/slack` 모듈도 유사한 구조로 설계:
  ```
  com.tech.n.ai.client.slack
  ├── config/
  ├── domain/
  │   └── slack/
  │       ├── api/          # Slack API 인터페이스 (선택사항)
  │       ├── client/       # Slack Client 인터페이스 및 구현
  │       ├── contract/    # Contract 인터페이스 및 DTO
  │       └── service/      # 알림 서비스
  ```

**일관성 유지:**
- 네이밍 컨벤션: `client/feign` 모듈과 동일한 패턴 사용
- 인터페이스 기반 설계: `client/feign` 모듈과 동일한 패턴
- DTO 정의 방식: `client/feign` 모듈의 `SampleDto` 패턴 참고
- 설정 관리: `application.yml` 기반 설정

### 2. Slack API 분석 및 선택

**Incoming Webhooks vs Web API (Bot Token):**

1. **Incoming Webhooks (권장 - 기본 구현)**
   - 장점:
     - 설정이 간단함
     - Rate Limit이 넉넉함 (일반적으로 초당 1개 메시지)
     - 특정 채널로만 메시지 전송 가능
   - 단점:
     - 채널 변경이 어려움 (Webhook URL이 채널에 고정)
     - 사용자 멘션, 스레드 응답 등 고급 기능 제한
   - 사용 사례:
     - 배치 작업 완료/실패 알림
     - 에러 발생 시 알림
     - 출처 상태 변경 알림

2. **Web API (Bot Token) (선택사항 - 고급 기능)**
   - 장점:
     - 다양한 채널로 메시지 전송 가능
     - 사용자 멘션 가능
     - 스레드 응답 가능
     - 더 많은 API 기능 활용 가능
   - 단점:
     - OAuth 2.0 인증 필요
     - Rate Limit이 더 엄격함 (Tier별로 다름)
     - 설정이 복잡함
   - 사용 사례:
     - 고급 알림 기능이 필요한 경우

**권장 설계:**
- 기본 구현: Incoming Webhooks 사용
- 확장 가능성: Web API (Bot Token) 지원을 위한 인터페이스 설계
- 전략 패턴 적용: Webhook과 Bot API를 전략 패턴으로 구현

### 3. Spring Boot 베스트 프랙티스 적용

**HTTP 클라이언트:**
- Spring WebClient 사용 (비동기, 논블로킹)
- 연결 풀 설정 (Reactor Netty)
- 타임아웃 설정

**설정 관리:**
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
```

**의존성 주입:**
- 인터페이스 기반 설계
- `@Configuration` 클래스에서 빈 정의
- 조건부 빈 생성 (`@ConditionalOnProperty`)

**에러 핸들링:**
- 커스텀 예외 정의 (`SlackException`, `SlackApiException` 등)
- 재시도 로직 (Spring Retry 또는 Resilience4j)
- 실패 시 로깅 (알림 실패는 시스템에 치명적이지 않음)

**Rate Limiting 처리 (Redis 활용):**
- `common/core` 모듈의 `RedisTemplate` 활용
- Key 네이밍 전략: `rate-limit:slack:{identifier}`
  - Webhook의 경우: `rate-limit:slack:webhook:{webhook-id}`
  - Bot API의 경우: `rate-limit:slack:bot:{channel-id}`
- TTL 설정: 1분 (Slack Rate Limit 정책에 맞춰 조정)
- 구현 패턴:
  ```java
  // 마지막 요청 시간 확인
  String lastRequestTime = redisTemplate.opsForValue().get(key);
  if (lastRequestTime != null) {
      long elapsed = System.currentTimeMillis() - Long.parseLong(lastRequestTime);
      if (elapsed < MIN_INTERVAL_MS) {
          // Rate Limit 초과, 대기 또는 예외 발생
      }
  }
  // 요청 시간 저장
  redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), Duration.ofMinutes(1));
  ```
- `docs/step7/redis-optimization-best-practices.md`의 Rate Limiting 패턴 참고
- 분산 환경에서의 Rate Limiting 지원 (여러 인스턴스에서 동일한 Rate Limit 적용)

### 4. 클린코드 원칙 및 객체지향 설계

**인터페이스 설계:**
```java
// Slack Client 인터페이스
public interface SlackClient {
    void sendMessage(SlackMessage message);
    void sendMessage(String text);
}

// Slack Webhook Client 구현
public class SlackWebhookClient implements SlackClient {
    // Webhook 구현
}

// Slack Bot Client 구현 (선택사항)
public class SlackBotClient implements SlackClient {
    // Bot API 구현
}
```

**메시지 빌더:**
```java
// Block Kit 메시지 빌더
public class SlackMessageBuilder {
    // 빌더 패턴 적용
}
```

**알림 서비스:**
```java
// 알림 서비스 인터페이스
public interface SlackNotificationService {
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendInfoNotification(String message);
    void sendBatchJobNotification(BatchJobResult result);
}
```

### 5. 메시지 템플릿 설계

**템플릿 타입:**
1. **에러 알림 템플릿**
   - 에러 메시지
   - 스택 트레이스 (선택사항)
   - 발생 시간
   - 관련 컨텍스트 정보

2. **성공 알림 템플릿**
   - 성공 메시지
   - 실행 시간
   - 관련 통계 정보

3. **정보 알림 템플릿**
   - 정보 메시지
   - 관련 링크 (선택사항)

4. **배치 작업 완료 알림 템플릿**
   - 작업 이름
   - 실행 상태 (성공/실패)
   - 실행 시간
   - 처리된 항목 수
   - 에러 정보 (실패 시)

**Block Kit 활용:**
- Section Block: 텍스트 표시
- Divider Block: 구분선
- Context Block: 보조 정보 표시
- Actions Block: 버튼 (필요 시)

### 6. 구현 가이드 작성

**단계별 구현 가이드:**
1. 의존성 추가 (`build.gradle`)
2. 설정 파일 작성 (`application.yml`)
3. DTO 및 Contract 정의
4. 인터페이스 정의
5. 클라이언트 구현
6. 메시지 빌더 구현
7. 알림 서비스 구현
8. 설정 클래스 작성
9. **Rate Limiting 구현 (Redis 활용)**
   - `common/core` 모듈의 `RedisTemplate` 활용
   - Key 네이밍: `rate-limit:slack:{identifier}`
   - TTL 설정: 1분 (Slack Rate Limit 정책에 맞춰 조정)
   - 마지막 요청 시간 저장 및 최소 간격 확인
   - `docs/step7/redis-optimization-best-practices.md`의 Rate Limiting 패턴 참고
10. 테스트 작성

**코드 예시:**
- 각 단계별 코드 예시 제공
- 클린코드 원칙 준수
- 주석 및 문서화

### 6-1. 테스트 작성 가이드 (상세)

**참고 파일**: `client/feign/src/test/java/com/tech/n/ai/client/feign/SampleFeignTest.java`

**테스트 작성 패턴:**

1. **테스트 클래스 구조**
   ```java
   @SpringBootTest(classes = {
       SlackTestContext.class,
       SlackConfig.class
   }, properties = {
       "spring.profiles.active=local",
   })
   public class SlackClientTest {
       @Autowired private SlackNotificationService notificationService;
       
       // @Test는 주석 처리하여 기본적으로 비활성화
       // @DisplayName으로 테스트 목적 명시
   }
   ```

2. **Given-When-Then 패턴 사용**
   - Given: 테스트 데이터 준비
   - When: 테스트 대상 메서드 실행
   - Then: 결과 검증

3. **테스트 작성 가이드라인**
   - `@SpringBootTest` 사용, `classes`에 테스트 컨텍스트와 Config 클래스 지정
   - `@DisplayName`으로 테스트 목적 명시, `@Test`는 주석 처리하여 기본적으로 비활성화
   - `@Autowired`로 테스트 대상 컴포넌트 주입, JUnit 5의 `Assertions` 사용
   - 실제 Slack API에 의존하지 않도록 Mock 또는 테스트용 설정 사용 권장
   - Webhook URL은 테스트용 Mock 서버 또는 환경 변수로 관리

4. **Mock 전략**
   - Spring WebClient의 경우 `MockWebServer` 활용
   - Slack API 응답을 Mock하여 테스트
   - Rate Limiting 테스트는 Redis를 Mock하거나 테스트용 Redis 인스턴스 사용

5. **테스트 시나리오**
   - Webhook 메시지 전송 성공/실패
   - Rate Limiting 동작 확인
   - 에러 핸들링 확인
   - 메시지 템플릿 포맷팅 확인
   - 배치 작업 알림 전송 확인

### 7. 보안 고려사항

**Webhook URL 보안:**
- 환경 변수로 관리
- Git에 커밋하지 않음
- 프로덕션 환경에서만 사용

**Bot Token 보안:**
- 환경 변수로 관리
- Git에 커밋하지 않음
- 토큰 갱신 정책

**민감 정보 처리:**
- 로그에 민감 정보 출력 금지
- 설정 파일에 하드코딩 금지

## 검증 체크리스트

작업 완료 후 다음 항목을 확인:

- [ ] 프로젝트 전체 구조 분석 완료
- [ ] `client/feign` 모듈 구조 분석 및 패턴 파악 완료
- [ ] 관련 설계 문서 (`docs/step8/`, `docs/step7/`, `docs/step6/`, `docs/step2/`, `docs/step1/`) 참고 완료
- [ ] Slack API 공식 문서 확인 및 분석 완료
- [ ] Spring Boot 베스트 프랙티스 반영 완료
- [ ] 클린코드 원칙 및 객체지향 설계 기법 검증 완료
- [ ] 오버엔지니어링 요소 제거 완료
- [ ] 모든 참고 출처를 "참고 자료" 섹션에 정리 완료
- [ ] 문서의 일관성 및 가독성 확인
- [ ] 실제 구현 가능한 수준의 가이드 작성 완료
- [ ] 테스트 작성 가이드 보강 완료 (`client/feign` 모듈 패턴 반영)
- [ ] Rate Limiting 처리 가이드 추가 완료 (Redis 활용 패턴 명시)
- [ ] 실제 사용 사례 구체적 시나리오 추가 완료

## 제약 조건

1. **코드 작성 금지**: 설계 가이드 작성에만 집중, 실제 구현 코드는 작성하지 않음 (예시 코드는 제외)
2. **기존 구조 준수**: `client/feign` 모듈의 구조와 패턴을 최대한 준수
3. **간결성 유지**: 불필요한 설명이나 중복 내용 제거
4. **실용성 우선**: 이론보다는 실제 구현에 도움이 되는 내용 중심
5. **오버엔지니어링 방지**: 현재 요구사항에 맞는 최소한의 설계만 제안

## 작업 순서

1. **프로젝트 구조 분석**
   - 전체 프로젝트 구조 파악
   - `client/feign` 모듈 구조 상세 분석
   - 관련 설계 문서 읽기

2. **Slack API 공식 문서 확인**
   - Incoming Webhooks 공식 문서 확인
   - Web API (Bot Token) 공식 문서 확인
   - Block Kit 공식 문서 확인
   - Rate Limiting 정책 확인

3. **Spring Boot 베스트 프랙티스 확인**
   - Spring WebClient 공식 문서 확인
   - Spring Boot 설정 관리 베스트 프랙티스 확인
   - Spring Retry 또는 Resilience4j 확인
   - Redis Rate Limiting 패턴 확인 (`docs/step7/redis-optimization-best-practices.md`)

4. **설계 도출**
   - `client/feign` 모듈 패턴을 참고하여 `client/slack` 모듈 설계
   - 클린코드 원칙 및 객체지향 설계 기법 적용
   - 오버엔지니어링 요소 제거
   - Rate Limiting 설계 (Redis 활용 패턴 적용)
   - 테스트 작성 패턴 설계 (`client/feign` 모듈 테스트 패턴 참고)

5. **문서 작성**
   - 구조화된 가이드 문서 작성
   - 코드 예시 포함 (구현 가이드)
   - 테스트 작성 가이드 상세화 (`client/feign` 모듈 패턴 반영)
   - Rate Limiting 구현 가이드 추가 (Redis 활용 패턴 명시)
   - 실제 사용 사례 구체적 시나리오 추가
   - 참고 출처 정리

6. **검증**
   - 체크리스트 확인
   - 문서 일관성 확인
   - 가독성 확인

## 예상 산출물

- `docs/step8/slack-integration-design-guide.md` 문서
- 프로젝트 구조 분석 결과
- Slack API 분석 결과
- Spring Boot 베스트 프랙티스 반영
- 클린코드 원칙 준수한 설계
- 참고 출처 정리 완료
- 실제 구현 가능한 수준의 가이드
- 테스트 작성 가이드 보강 (`client/feign` 모듈 패턴 반영)
- Rate Limiting 구현 가이드 (Redis 활용 패턴 명시)
- 실제 사용 사례 구체적 시나리오 포함

---

**작성 일시**: 2026-01-07  
**작성자**: System Architect  
**버전**: 1.0
