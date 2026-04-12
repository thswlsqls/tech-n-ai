# 멀티모듈 Spring Boot 애플리케이션에서 Redis 최적화 베스트 프랙티스

## 1. 현재 구현 분석

### 1.1. 현재 Redis 설정의 문제점

**RedisConfig (`common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java`)**:

1. **연결 풀 설정 없음**
   - Lettuce 기본 설정만 사용
   - 프로덕션 환경에서 연결 풀 최적화 필요

2. **타임아웃 설정 없음**
   - 네트워크 지연 시 무한 대기 가능성
   - 애플리케이션 응답 시간 저하 위험

3. **직렬화 전략 제한**
   - `StringRedisSerializer`만 사용
   - 현재 사용 사례(OAuth State, Kafka 이벤트 ID)에는 적합하나, 향후 복잡한 객체 저장 시 제한

4. **모니터링 설정 없음**
   - Spring Boot Actuator 메트릭 수집 미설정
   - Redis 성능 모니터링 불가

5. **보안 설정 부족**
   - Redis 인증 설정이 환경 변수로만 관리
   - TLS/SSL 설정이 기본값(false)으로 설정

### 1.2. 현재 사용 패턴 분석

#### OAuth State 저장 (`OAuthStateService`)

**현재 구현**:
- Key 형식: `oauth:state:{state_value}`
- TTL: 10분 (`Duration.ofMinutes(10)`)
- 작업: 저장, 조회, 삭제 (일회성 사용)
- TTL 설정 방법: `Duration` 객체 직접 사용
  ```java
  redisTemplate.opsForValue().set(key, providerName, STATE_TTL);
  ```

**분석 결과**:
- ✅ TTL 설정이 적절함 (OAuth 2.0 권장 시간)
- ✅ Key 네이밍 전략이 명확함
- ✅ 일회성 사용 패턴이 적절함
- ⚠️ `Duration` 객체 직접 사용은 Spring Data Redis에서 권장하는 방법

#### RSS/Scraper 모듈 Rate Limiting (`RateLimiter`)

**사용 사례**: `client-rss`와 `client-scraper` 모듈에서 출처별 요청 간격 관리를 위해 Redis를 활용합니다.

**구현 예시**:
- Key 형식: `rate-limit:{source-name}`
- TTL: 없음 (수동 삭제 또는 덮어쓰기)
- 작업: 저장, 조회 (마지막 요청 시간 저장)
- 구현 패턴: 출처별 최소 간격 확인 및 대기
  ```java
  redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()));
  ```

**분석 결과**:
- ✅ 출처별 Rate Limiting 관리에 적합
- ✅ Key 네이밍 전략이 명확함 (`rate-limit:` 네임스페이스)
- ✅ 분산 환경에서의 Rate Limiting 지원
- ⚠️ TTL 설정이 없으므로 수동 정리 필요 (또는 TTL 추가 고려)

**상세 설계**: RSS/Scraper 모듈의 Rate Limiting 구현 상세는 `docs/step8/rss-scraper-modules-analysis.md` 문서의 "Rate Limiting (Redis 활용)" 섹션을 참고하세요.

#### Slack 모듈 Rate Limiting (`SlackRateLimiter`)

**사용 사례**: `client-slack` 모듈에서 Slack API 호출 빈도를 제어하기 위해 Redis를 활용합니다.

**구현 예시**:
- Key 형식: `rate-limit:slack:webhook:{webhook-id}` 또는 `rate-limit:slack:bot:{channel-id}`
- TTL: 1분 (`Duration.ofMinutes(1)`)
- 작업: 저장, 조회 (마지막 요청 시간 저장)
- 구현 패턴: 최소 간격 확인 및 대기
  ```java
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
  
  // 요청 시간 저장 (TTL: 1분)
  redisTemplate.opsForValue().set(
      key, 
      String.valueOf(System.currentTimeMillis()), 
      Duration.ofMinutes(1)
  );
  ```

**분석 결과**:
- ✅ Slack API Rate Limit 준수에 적합
- ✅ Key 네이밍 전략이 명확함 (`rate-limit:slack:` 네임스페이스)
- ✅ TTL 설정으로 자동 정리 (1분)
- ✅ 분산 환경에서의 Rate Limiting 지원
- ✅ `Duration` 객체 직접 사용으로 일관성 유지

**상세 설계**: Slack 모듈의 Rate Limiting 구현 상세는 `docs/step8/slack-integration-design-guide.md` 문서의 "Rate Limiting 구현 (Redis 활용)" 섹션을 참고하세요.

#### Sources 메타데이터 캐싱 (`SourcesSyncJob Step2`)

**사용 사례**: `batch/source` 모듈의 SourcesSyncJob에서 MongoDB sources 컬렉션의 데이터를 Redis에 캐싱합니다.

**구현 예시**:
- Key 형식: `{url}:{category}`
- TTL: 없음 (배치 실행 시 자동 갱신)
- 작업: 저장 (배치), 조회 (API/배치)
- 구현 패턴: MongoDB → Redis 동기화
  ```java
  String sourceId = document.getId().toString();
  String key = document.getUrl() + ":" + document.getCategory();
  redisTemplate.opsForValue().set(key, sourceId);
  ```

**분석 결과**:
- ✅ URL+카테고리 복합 키로 유니크 보장
- ✅ 배치 작업에서 소스 검증에 활용
- ✅ TTL 없이 영구 저장 (배치 재실행 시 덮어쓰기)
- ✅ 메모리 효율적 (100개 소스 기준 약 7KB)

**상세 설계**: Sources 캐싱 구현 상세는 `docs/step18/sources-sync-step2-redis-caching-design.md` 문서를 참고하세요.

#### Kafka 이벤트 멱등성 보장 (`EventConsumer`)

**현재 구현**:
- Key 형식: `processed_event:{eventId}`
- TTL: 7일
- 작업: 저장, 조회 (중복 확인)
- TTL 설정 방법: `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` 혼용
  ```java
  redisTemplate.opsForValue().set(
      key,
      "processed",
      Duration.ofDays(PROCESSED_EVENT_TTL_DAYS).toSeconds(),
      TimeUnit.SECONDS
  );
  ```

**분석 결과**:
- ✅ TTL 설정이 적절함 (Kafka 이벤트 재처리 방지)
- ✅ Key 네이밍 전략이 명확함
- ❌ **TTL 설정 방법이 OAuthStateService와 불일치**
  - OAuthStateService: `Duration` 객체 직접 사용
  - EventConsumer: `Duration.toSeconds()` + `TimeUnit.SECONDS` 혼용
  - 일관성 개선 필요

#### Spring Session Data Redis

**현재 상태**:
- **의존성 포함**: `common/security/build.gradle`에 `spring-boot-starter-session-data-redis` 포함
- **실제 사용 여부**: 사용하지 않음
- **이유**: `SecurityConfig`에서 `SessionCreationPolicy.STATELESS` 정책 사용
- **관계**: Stateless 아키텍처이므로 세션 저장소로 Redis를 사용하지 않음

**분석 결과**:
- ⚠️ 불필요한 의존성이 포함되어 있음
- ⚠️ 향후 세션이 필요한 기능 추가 시 사용 가능하나, 현재는 미사용
- 권장: Stateless 아키텍처를 유지하는 경우 의존성 제거 고려

## 2. 연결 풀 최적화

### 2.1. Lettuce vs Jedis

**Spring Boot 4.0.1 기본 클라이언트**: **Lettuce**

**Lettuce의 장점** (Spring Boot 공식 문서 기준):
- 비동기/논블로킹 I/O 지원
- Netty 기반으로 높은 성능
- 연결 풀링이 선택 사항 (단일 연결로도 효율적)
- Thread-safe

**Lettuce의 단점**:
- 연결 풀 설정이 복잡할 수 있음
- Jedis 대비 상대적으로 높은 메모리 사용

**권장 사항**: Spring Boot 4.0.1에서 기본으로 사용하는 Lettuce를 그대로 사용

### 2.2. Lettuce 연결 풀 설정

**권장 설정** (`application.yml`):

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
      timeout: 2000ms  # 연결 타임아웃
      lettuce:
        pool:
          max-active: 8      # 최대 활성 연결 수
          max-idle: 8        # 최대 유휴 연결 수
          min-idle: 2        # 최소 유휴 연결 수
          max-wait: -1ms     # 연결 대기 시간 (-1: 무한 대기)
        shutdown-timeout: 100ms  # 종료 시 대기 시간
```

**설정 근거** (Spring Boot 공식 문서 및 Redis 베스트 프랙티스):

1. **max-active: 8**
   - 일반적인 웹 애플리케이션에서 권장 값
   - 멀티모듈 환경에서 각 모듈이 독립적으로 연결을 사용하므로 충분
   - 트래픽이 높은 경우 16-32로 증가 가능

2. **max-idle: 8**
   - max-active와 동일하게 설정하여 연결 재사용 최적화
   - 유휴 연결을 유지하여 연결 생성 오버헤드 감소

3. **min-idle: 2**
   - 최소 유휴 연결을 유지하여 즉시 사용 가능한 연결 보장
   - 너무 높게 설정하면 불필요한 메모리 사용

4. **max-wait: -1ms**
   - 무한 대기로 설정 (기본값)
   - 연결 풀이 가득 찬 경우 대기
   - 타임아웃이 필요한 경우 양수 값 설정 (예: 5000ms)

5. **timeout: 2000ms**
   - Redis 서버 응답 대기 시간
   - 네트워크 지연을 고려한 적절한 값

**멀티모듈 환경 고려사항**:
- 각 모듈이 동일한 Redis 인스턴스를 사용하므로, 연결 풀 설정은 공통으로 적용
- 모듈별로 다른 설정이 필요한 경우는 드물지만, 필요 시 `@ConditionalOnProperty` 사용

### 2.3. 타임아웃 설정

**권장 설정**:

```yaml
spring:
  data:
    redis:
      timeout: 2000ms  # 명령 실행 타임아웃
      lettuce:
        shutdown-timeout: 100ms  # 애플리케이션 종료 시 연결 종료 대기 시간
```

**설정 근거**:
- `timeout`: Redis 명령 실행 시 최대 대기 시간
  - 너무 짧으면 네트워크 지연 시 실패
  - 너무 길면 애플리케이션 응답 시간 저하
  - 2000ms는 일반적인 프로덕션 환경에서 적절한 값

## 3. RedisTemplate 설정 최적화

### 3.1. 현재 RedisConfig 분석

**현재 설정의 문제점**:

1. **`afterPropertiesSet()` 호출**
   - 현재 코드에서 명시적으로 호출하고 있음
   - Spring Boot가 자동으로 호출하므로 불필요할 수 있음
   - 하지만 명시적 호출도 문제없음 (방어적 프로그래밍)

2. **트랜잭션 지원 없음**
   - 현재 사용 사례(OAuth State, Kafka 이벤트 멱등성)에서는 트랜잭션이 필요 없음
   - 향후 복잡한 작업이 필요한 경우에만 고려

3. **직렬화 전략 제한**
   - `StringRedisSerializer`만 사용
   - 현재 사용 사례에는 적합하나, 향후 확장성 고려 필요

### 3.2. 최적화된 RedisConfig

**최적화된 설정**:

```java
package com.ebson.shrimp.tm.demo.common.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        // 기본 직렬화 비활성화 (명시적 직렬화만 사용)
        template.setEnableDefaultSerializer(false);
        
        // 트랜잭션 지원 비활성화 (현재 사용 사례에서 불필요)
        template.setEnableTransactionSupport(false);
        
        // 초기화
        template.afterPropertiesSet();
        
        return template;
    }
}
```

**변경 사항 설명**:

1. **`setEnableDefaultSerializer(false)` 추가**
   - 명시적으로 설정한 직렬화만 사용
   - 예상치 못한 직렬화 동작 방지
   - Spring Data Redis 공식 문서 권장

2. **`setEnableTransactionSupport(false)` 명시**
   - 현재 사용 사례에서는 트랜잭션이 필요 없음
   - 명시적으로 비활성화하여 불필요한 오버헤드 방지
   - 향후 필요 시 활성화 가능

3. **`afterPropertiesSet()` 유지**
   - 방어적 프로그래밍
   - Spring Boot가 자동으로 호출하지만, 명시적 호출도 문제없음

## 4. 직렬화 전략

### 4.1. 현재 전략 평가

**현재 전략**: `StringRedisSerializer`만 사용

**평가 결과**:
- ✅ **현재 사용 사례에 적합**: OAuth State와 Kafka 이벤트 ID는 모두 String
- ✅ **성능**: String 직렬화는 가장 빠름
- ✅ **호환성**: 모든 Redis 클라이언트와 호환
- ⚠️ **확장성 제한**: 복잡한 객체 저장 시 제한

### 4.2. 향후 확장성

**복잡한 객체 저장 시 권장 직렬화 전략**:

#### JSON 직렬화 (권장)

**장점**:
- 가독성: Redis에서 직접 확인 가능
- 호환성: 다른 언어/플랫폼과 호환
- 유연성: 스키마 변경에 유연

**단점**:
- 성능: String 직렬화보다 느림
- 크기: Java 직렬화보다 큼

**구현 예시**:

```java
@Bean
public RedisTemplate<String, Object> redisTemplateForObjects(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // Key는 String 직렬화
    template.setKeySerializer(new StringRedisSerializer());
    
    // Value는 JSON 직렬화
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);
    
    template.setEnableDefaultSerializer(false);
    template.afterPropertiesSet();
    
    return template;
}
```

#### Java 직렬화 (비권장)

**장점**:
- 간단한 구현
- Java 객체를 그대로 저장

**단점**:
- 성능: JSON보다 느림
- 호환성: Java 전용
- 보안: 역직렬화 시 보안 취약점 가능
- 크기: JSON보다 큼

**권장 사항**: JSON 직렬화 사용

## 4.3. TTL 설정 일관성 개선

### 4.3.1. 현재 TTL 설정 방법 비교

**OAuthStateService**:
```java
redisTemplate.opsForValue().set(key, providerName, STATE_TTL);
// STATE_TTL은 Duration.ofMinutes(10)
```

**EventConsumer**:
```java
redisTemplate.opsForValue().set(
    key,
    "processed",
    Duration.ofDays(PROCESSED_EVENT_TTL_DAYS).toSeconds(),
    TimeUnit.SECONDS
);
```

**차이점**:
- OAuthStateService: `Duration` 객체 직접 사용
- EventConsumer: `Duration.toSeconds()` + `TimeUnit.SECONDS` 혼용

**Spring Data Redis 공식 문서 기준**:
- `RedisTemplate.opsForValue().set(key, value, timeout)` 메서드는 `Duration` 객체를 직접 받을 수 있음
- `Duration` 객체 사용이 더 간결하고 타입 안전함

### 4.3.2. 권장 TTL 설정 방법

**권장 방법**: `Duration` 객체 직접 사용

**이유**:
1. **타입 안전성**: 컴파일 타임에 타입 체크
2. **가독성**: 코드가 더 명확함
3. **일관성**: Spring Data Redis API와 일치
4. **간결성**: `TimeUnit` 변환 불필요

**개선된 EventConsumer 코드**:

```java
package com.ebson.shrimp.tm.demo.common.kafka.consumer;

import com.ebson.shrimp.tm.demo.common.kafka.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // ... 기존 코드 ...
    
    /**
     * 이벤트를 처리 완료로 표시 (Redis에 저장)
     * 
     * @param eventId 이벤트 ID
     */
    private void markEventAsProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        // Duration 객체 직접 사용 (일관성 개선)
        redisTemplate.opsForValue().set(key, "processed", PROCESSED_EVENT_TTL);
    }
}
```

**변경 사항**:
- `PROCESSED_EVENT_TTL_DAYS` (long) → `PROCESSED_EVENT_TTL` (Duration)
- `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` → `Duration` 객체 직접 사용
- `TimeUnit` import 제거

## 5. 성능 최적화

### 5.1. 네트워크 최적화

#### Pipeline/Batch 작업

**현재 사용 사례 분석**:
- OAuth State: 단일 키 저장/조회 (Pipeline 불필요)
- Kafka 이벤트 멱등성: 단일 키 저장/조회 (Pipeline 불필요)

**Pipeline 사용 시나리오**:
- 여러 키를 동시에 조회/저장해야 하는 경우
- 현재 사용 사례에서는 불필요

**권장 사항**: 현재 사용 사례에서는 Pipeline 불필요, 향후 여러 키를 동시에 처리해야 하는 경우에만 고려

#### 비동기 작업

**ReactiveRedisTemplate 사용**:
- 현재 사용 사례에서는 동기 작업으로 충분
- 비동기가 필요한 경우:
  - 대량의 데이터 처리
  - 실시간 스트리밍
  - 현재는 해당 없음

**권장 사항**: 현재는 동기 `RedisTemplate` 사용 유지

### 5.2. 메모리 최적화

#### Key 네이밍 전략

**현재 Key 네이밍**:
- `oauth:state:{state_value}`
- `processed_event:{eventId}`

**평가**:
- ✅ 네임스페이스 구분 명확 (`oauth:`, `processed_event:`)
- ✅ Key 충돌 방지
- ✅ 가독성 좋음

**권장 사항**: 현재 Key 네이밍 전략 유지

**향후 확장 시 고려사항**:
- 모듈별 네임스페이스 추가 (예: `api:auth:oauth:state:`)
- 버전 관리 (예: `v1:oauth:state:`)

#### TTL 설정 최적화

**현재 TTL 설정**:
- OAuth State: 10분 (적절)
- Kafka 이벤트: 7일 (적절)

**최적화 방안**:
- TTL이 너무 짧으면 재사용 가능성이 있는 데이터가 조기에 삭제
- TTL이 너무 길면 메모리 낭비
- 현재 설정은 적절함

**모니터링**:
- Redis 메모리 사용량 모니터링
- TTL 만료 전 데이터 사용 패턴 분석
- 필요 시 TTL 조정

### 5.3. 응답 시간 최적화

**현재 사용 패턴**:
- OAuth State: 저장/조회가 사용자 요청 중에 발생 (동기 처리 필요)
- Kafka 이벤트: 이벤트 처리 중에 발생 (동기 처리 필요)

**권장 사항**: 현재는 동기 처리 유지, 비동기 처리는 불필요

## 6. 모니터링 및 관찰 가능성

### 6.1. Spring Boot Actuator 설정

**설정 예시** (`common/core/src/main/resources/application-common-core.yml`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

**공통 설정 파일 사용**:
- `common/core/src/main/resources/application-common-core.yml`에 management 설정 추가
- 다른 모듈에서 `spring.profiles.include: common-core`로 공통 설정 참조

**Redis 메트릭 자동 수집**:
- Spring Boot Actuator가 자동으로 Redis 메트릭 수집
- `spring-boot-starter-actuator` 의존성만 추가하면 자동 활성화

**주요 메트릭**:
- `spring.data.redis.connection.active`: 활성 연결 수
- `spring.data.redis.connection.idle`: 유휴 연결 수
- `spring.data.redis.command.duration`: 명령 실행 시간

### 6.2. 커스텀 메트릭 수집

**구현 예시**:

```java
package com.ebson.shrimp.tm.demo.common.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMetrics {
    
    private final MeterRegistry meterRegistry;
    private final RedisTemplate<String, String> redisTemplate;
    
    public void recordRedisOperation(String operation, Runnable task) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            task.run();
        } finally {
            sample.stop(Timer.builder("redis.operation")
                .tag("operation", operation)
                .register(meterRegistry));
        }
    }
}
```

### 6.3. 로깅 전략

**권장 로깅 레벨**:
- 개발 환경: DEBUG (Redis 작업 상세 로깅)
- 프로덕션 환경: INFO (에러 및 중요 이벤트만)

**성능 로깅**:
- Slow Query 감지: 100ms 이상 소요되는 작업 로깅
- 에러 로깅: Redis 연결 실패, 타임아웃 등

## 7. 보안 고려사항

### 7.1. 연결 보안

#### Redis 인증 설정

**설정 방법** (`application.yml`):

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}  # 환경 변수로 관리
```

**권장 사항**:
- 프로덕션 환경에서는 반드시 비밀번호 설정
- 환경 변수로 관리하여 코드에 하드코딩 방지
- Redis `requirepass` 설정과 일치해야 함

#### TLS/SSL 암호화 전송

**설정 방법** (`application.yml`):

```yaml
spring:
  data:
    redis:
      ssl:
        enabled: ${REDIS_SSL_ENABLED:true}  # 프로덕션에서는 true
```

**권장 사항**:
- 프로덕션 환경에서는 TLS/SSL 활성화
- 개발 환경에서는 선택 사항

#### 네트워크 격리

**권장 사항**:
- Redis 서버를 프라이빗 네트워크에 배치
- 방화벽 규칙으로 접근 제한
- VPC 내부 통신만 허용

### 7.2. 데이터 보안

#### 민감한 데이터 저장 시 고려사항

**OAuth State 파라미터**:
- 현재: State 값 자체는 암호학적으로 안전한 랜덤 값
- 권장: 추가 암호화는 불필요 (State는 이미 non-guessable)
- HTTPS 전송 필수 (애플리케이션 레벨)

**Key 네이밍 전략의 보안 영향**:
- 현재 Key 네이밍은 보안에 영향 없음
- Key는 공개되어도 문제없음 (Value가 중요)
- State 값 자체가 암호학적으로 안전하므로 Key 노출 문제 없음

## 8. 멀티모듈 환경 최적화

### 8.1. 공통 설정 제공 방법

**현재 구조**:
- `common/core` 모듈에서 `RedisConfig` 제공
- `common/core/src/main/resources/application-common-core.yml`에서 Redis 공통 설정 제공
- 다른 모듈에서 `common/core` 의존성 추가하여 사용

**공통 설정 파일 사용 방법**:

1. **공통 설정 파일 생성** (`common/core/src/main/resources/application-common-core.yml`):
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
        shutdown-timeout: 100ms

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

2. **다른 모듈에서 공통 설정 참조** (`api/auth/src/main/resources/application-auth-api.yml`):
```yaml
spring:
  profiles:
    include:
      - common-core
```

**권장 사항**: 공통 설정 파일을 사용하여 멀티모듈 환경에서 일관된 Redis 설정 제공

**모듈별 커스텀 설정이 필요한 경우**:

```java
@Configuration
@ConditionalOnProperty(name = "redis.custom.enabled", havingValue = "true")
public class CustomRedisConfig {
    // 모듈별 커스텀 설정
}
```

### 8.2. 의존성 관리

#### spring-boot-starter-data-redis 의존성 전이

**현재 설정** (`common/core/build.gradle`):

```gradle
api 'org.springframework.boot:spring-boot-starter-data-redis'
```

**설정 근거**:
- `api`로 선언하여 의존성이 전이되도록 설정
- 다른 모듈에서 `common/core` 의존성만 추가하면 Redis 사용 가능
- 일관된 Redis 설정 사용 보장

**권장 사항**: 현재 설정 유지

#### Spring Session Data Redis 의존성 관리

**현재 상태**:
- `common/security/build.gradle`에 `spring-boot-starter-session-data-redis` 포함
- 실제로는 사용하지 않음 (`SessionCreationPolicy.STATELESS`)

**권장 사항**:

**옵션 1: 의존성 제거 (권장)**
- Stateless 아키텍처를 유지하는 경우 의존성 제거
- 불필요한 의존성으로 인한 메모리/빌드 시간 낭비 방지

**옵션 2: 의존성 유지**
- 향후 세션이 필요한 기능 추가 시 대비
- 현재는 미사용이지만 유지 가능

**최종 권장**: **옵션 1 (의존성 제거)**
- YAGNI 원칙 준수
- Stateless 아키텍처를 유지하는 것이 명확함
- 필요 시 나중에 추가 가능

## 9. 구현 가이드

### 9.1. 단계별 구현 가이드

#### 1단계: 연결 풀 설정 추가

**작업 내용**:
1. `common/core/src/main/resources/application-common-core.yml` 파일 생성 및 Lettuce 연결 풀 설정 추가
2. 타임아웃 설정 추가
3. 다른 모듈에서 `spring.profiles.include: common-core`로 공통 설정 참조

**파일**: 
- `common/core/src/main/resources/application-common-core.yml` (공통 설정 파일 생성)
- `api/auth/src/main/resources/application-auth-api.yml` (공통 설정 참조)

**공통 설정 파일** (`common/core/src/main/resources/application-common-core.yml`):
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
        shutdown-timeout: 100ms
```

**모듈별 설정 파일** (`api/auth/src/main/resources/application-auth-api.yml`):
```yaml
spring:
  profiles:
    include:
      - common-core
```

#### 2단계: RedisConfig 최적화

**작업 내용**:
1. `common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java` 수정
2. `setEnableDefaultSerializer(false)` 추가
3. `setEnableTransactionSupport(false)` 명시

**변경 사항**: 위의 "3.2. 최적화된 RedisConfig" 참고

#### 3단계: TTL 설정 일관성 개선

**작업 내용**:
1. `EventConsumer`의 TTL 설정 방법 변경
2. `Duration` 객체 직접 사용으로 통일

**변경 사항**: 위의 "4.3.2. 권장 TTL 설정 방법" 참고

#### 4단계: 모니터링 설정

**작업 내용**:
1. `common/core/src/main/resources/application-common-core.yml`에 Spring Boot Actuator 메트릭 수집 설정 추가
2. (선택) 커스텀 메트릭 수집 구현

**설정 파일** (`common/core/src/main/resources/application-common-core.yml`):
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
```

**참고**: 위의 "6. 모니터링 및 관찰 가능성" 참고

#### 5단계: 보안 설정 강화

**작업 내용**:
1. 프로덕션 환경에서 Redis 비밀번호 설정 확인
2. TLS/SSL 설정 활성화 (프로덕션)

**설정**: 위의 "7. 보안 고려사항" 참고

#### 6단계: (선택) Spring Session Data Redis 의존성 제거

**작업 내용**:
1. `common/security/build.gradle`에서 `spring-boot-starter-session-data-redis` 제거
2. 관련 테스트 의존성도 제거

**주의**: Stateless 아키텍처를 유지하는 경우에만 수행

### 9.2. 코드 예시

**최적화된 EventConsumer (TTL 설정 일관성 개선)**:

```java
package com.ebson.shrimp.tm.demo.common.kafka.consumer;

import com.ebson.shrimp.tm.demo.common.kafka.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @KafkaListener(
        topics = "${spring.kafka.consumer.topics:user-events,bookmark-events,contest-events,news-events}",
        groupId = "${spring.kafka.consumer.group-id:shrimp-task-manager-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload BaseEvent event,
        Acknowledgment acknowledgment,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        try {
            if (isEventProcessed(event.eventId())) {
                log.warn("Event already processed, skipping: eventId={}, eventType={}, partition={}, offset={}", 
                    event.eventId(), event.eventType(), partition, offset);
                acknowledgment.acknowledge();
                return;
            }
            
            processEvent(event);
            markEventAsProcessed(event.eventId());
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset);
        } catch (Exception e) {
            log.error("Error processing event: eventId={}, eventType={}, partition={}, offset={}", 
                event.eventId(), event.eventType(), partition, offset, e);
            throw e;
        }
    }
    
    private void processEvent(BaseEvent event) {
        log.info("Processing event: eventId={}, eventType={}", event.eventId(), event.eventType());
    }
    
    private boolean isEventProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    private void markEventAsProcessed(String eventId) {
        String key = PROCESSED_EVENT_PREFIX + eventId;
        // Duration 객체 직접 사용 (일관성 개선)
        redisTemplate.opsForValue().set(key, "processed", PROCESSED_EVENT_TTL);
    }
}
```

## 10. 검증 기준

제시된 최적화 방안은 다음 기준을 만족합니다:

1. ✅ **성능 향상**: 연결 풀 최적화로 응답 시간 개선
   - 연결 풀 설정으로 연결 생성 오버헤드 감소
   - 타임아웃 설정으로 무한 대기 방지

2. ✅ **메모리 효율성**: 불필요한 메모리 사용 최소화
   - 적절한 연결 풀 크기 설정
   - TTL 설정으로 만료 데이터 자동 삭제

3. ✅ **확장성**: 향후 사용 사례 추가 시 유연한 확장 가능
   - JSON 직렬화 전략 준비
   - 모듈별 커스텀 설정 가능

4. ✅ **보안성**: Redis 연결 및 데이터 보안 강화
   - 인증 설정
   - TLS/SSL 암호화 전송

5. ✅ **관찰 가능성**: 모니터링 및 메트릭 수집 가능
   - Spring Boot Actuator 메트릭 수집
   - 커스텀 메트릭 수집 가능

6. ✅ **멀티모듈 호환성**: 멀티모듈 환경에서 일관된 사용 패턴
   - `common/core`에서 공통 설정 제공
   - 의존성 전이 설정

7. ✅ **실용성**: 프로덕션 환경 적용 가능
   - 공식 문서 기반 권장 사항
   - 검증된 설정 값

8. ✅ **단순성**: 오버엔지니어링 없이 요구사항 충족
   - 현재 사용 사례에 최적화
   - 불필요한 복잡성 제거

## 11. 참고 자료

### 공식 문서

1. **Spring Data Redis 공식 문서**
   - Spring Data Redis Reference Documentation: https://docs.spring.io/spring-data/redis/reference/
   - Redis Support: https://docs.spring.io/spring-data/redis/reference/redis/redis.html
   - Redis Connection: https://docs.spring.io/spring-data/redis/reference/redis/connection.html
   - Redis Template: https://docs.spring.io/spring-data/redis/reference/redis/redis-template.html

2. **Spring Boot 공식 문서**
   - Spring Boot Data Redis: https://docs.spring.io/spring-boot/reference/data/nosql/redis.html
   - Spring Boot Configuration Properties: https://docs.spring.io/spring-boot/reference/features/external-config/application-properties.html

3. **Redis 공식 문서**
   - Redis Documentation: https://redis.io/docs/
   - Redis Best Practices: https://redis.io/docs/manual/patterns/
   - Redis Security: https://redis.io/docs/management/security/
   - Redis Performance: https://redis.io/docs/manual/optimization/

4. **Lettuce 공식 문서**
   - Lettuce GitHub: https://github.com/lettuce-io/lettuce-core
   - Lettuce Connection Pooling: https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling

### 프로젝트 내부 문서

5. **Slack 연동 설계 가이드**
   - `docs/step8/slack-integration-design-guide.md`: Slack 모듈 Rate Limiting 구현 상세
   - Slack Rate Limiting 패턴 및 Redis 활용 방법

6. **RSS 및 Scraper 모듈 분석**
   - `docs/step8/rss-scraper-modules-analysis.md`: RSS/Scraper 모듈 Rate Limiting 구현 상세
   - 출처별 Rate Limiting 패턴

---

**작성일**: 2025-01-27  
**최종 업데이트**: 2026-01-07  
**버전**: 1.1  
**작성 근거**: Spring Data Redis, Spring Boot, Redis 공식 문서  
**대상**: 멀티모듈 Spring Boot 애플리케이션에서 Redis 최적화 베스트 프랙티스  
**업데이트 내용**: Slack 모듈 Rate Limiting 패턴 추가
