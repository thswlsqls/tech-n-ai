# 멀티모듈 Spring Boot 애플리케이션에서 Redis 최적화 베스트 프랙티스 연구 프롬프트

## 역할 및 목표 설정

당신은 **Spring Data Redis 아키텍트 및 성능 최적화 전문가**입니다. 멀티모듈 Spring Boot 애플리케이션에서 Redis를 최적화하여 사용하기 위한 베스트 프랙티스를 연구하고 분석하여 기술 문서를 작성해야 합니다.

## 프로젝트 컨텍스트

### 프로젝트 구조

**멀티모듈 Spring Boot 애플리케이션**:
- **프레임워크**: Spring Boot 4.0.1, Java 21
- **아키텍처**: MSA 멀티모듈 구조 (domain, common, client, batch, api)
- **데이터베이스**: Amazon Aurora MySQL (Command Side), MongoDB Atlas (Query Side)
- **메시징**: Apache Kafka (CQRS 패턴 동기화)
- **캐싱**: Redis

**모듈 구조**:
- `common/core`: 공통 코어 모듈 (Redis 설정 포함)
- `common/security`: 보안 모듈 (Spring Session Data Redis 포함)
- `api/auth`: 인증 API 모듈
- `common/kafka`: Kafka 이벤트 처리 모듈

### 현재 Redis 사용 현황

**1. Redis 설정 (`common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java`)**:
- 기본 `RedisTemplate<String, String>` 설정
- StringRedisSerializer 사용 (Key, Value, HashKey, HashValue 모두)
- 연결 풀 설정 없음
- TTL 설정 없음

**2. Redis 사용 사례**:

**a) OAuth State 저장 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthStateService.java`)**:
- 용도: OAuth 2.0 CSRF 공격 방지를 위한 State 파라미터 저장
- Key 형식: `oauth:state:{state_value}`
- TTL: 10분 (600초)
- 작업: 저장, 조회, 삭제 (일회성 사용)

**b) Kafka 이벤트 멱등성 보장 (`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`)**:
- 용도: Kafka 이벤트 중복 처리 방지
- Key 형식: `processed_event:{eventId}`
- TTL: 7일
- 작업: 저장, 조회 (중복 확인)
- **주의사항**: TTL 설정 시 `Duration.ofDays().toSeconds()`와 `TimeUnit.SECONDS` 혼용 (일관성 개선 필요)
  ```java
  // 현재 코드 (일관성 부족)
  redisTemplate.opsForValue().set(
      key,
      "processed",
      Duration.ofDays(PROCESSED_EVENT_TTL_DAYS).toSeconds(),
      TimeUnit.SECONDS
  );
  // OAuthStateService는 Duration 객체를 직접 사용하므로 일관성 개선 필요
  ```

**c) Spring Session Data Redis (`common/security` 모듈)**:
- **현재 상태**: 의존성은 포함되어 있으나 실제로는 사용하지 않음
- **이유**: `SecurityConfig`에서 `SessionCreationPolicy.STATELESS` 정책 사용
- **의존성**: `spring-boot-starter-session-data-redis` (common/security/build.gradle)
- **참고**: Stateless 아키텍처이므로 세션 저장소로 Redis를 사용하지 않음
- **향후 사용 가능성**: 특정 기능에서 세션이 필요한 경우에만 사용 가능 (현재는 미사용)

**3. Redis 연결 설정 (`application.yml`)**:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
```

**현재 설정의 한계**:
- 연결 풀 설정 없음 (Lettuce 기본 설정 사용)
- 타임아웃 설정 없음
- 직렬화 전략이 String만 지원 (복잡한 객체 저장 불가)
- 모니터링 및 메트릭 수집 설정 없음
- Redis 클러스터/센티넬 설정 없음

### 프로젝트 목표 및 요구사항

**프로젝트 목표** (`prompts/shrimp-task-prompt.md` 참고):
- 개발자 대회 정보와 최신 IT 테크 뉴스를 제공하는 API Server 구축
- CQRS 패턴 기반 아키텍처 (Aurora MySQL + MongoDB Atlas + Kafka)
- Stateless JWT 토큰 기반 인증
- OAuth 2.0 소셜 로그인 (Google, Naver, Kakao)
- 사용자 아카이브 기능 (Soft Delete 포함)
- Kafka 이벤트 기반 실시간 동기화

**Redis 사용 요구사항**:
- OAuth State 파라미터 저장 및 검증 (TTL 10분)
- Kafka 이벤트 멱등성 보장 (TTL 7일)
- 향후 확장 가능성: 캐싱, Rate Limiting, 세션 저장 등

## 연구 범위 및 제약 조건

### 필수 요구사항

1. **멀티모듈 아키텍처 고려**: 
   - `common/core` 모듈에서 공통 Redis 설정 제공
   - 다른 모듈에서 일관된 Redis 사용 패턴 유지
   - 모듈 간 의존성 최소화

2. **공식 문서 기반**: 
   - Spring Data Redis 공식 문서만 참고
   - Redis 공식 문서만 참고
   - Spring Boot 공식 문서만 참고
   - 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지
   - (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)

3. **실용성 우선**: 
   - 프로덕션 환경에서 검증된 방법만 제시
   - 현재 사용 사례(OAuth State, Kafka 이벤트 멱등성)에 최적화
   - 향후 확장 가능성 고려하되 오버엔지니어링 지양

4. **오버엔지니어링 지양**: 
   - 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음
   - 단일 구현체를 위한 인터페이스 생성 금지
   - 불필요한 추상화 계층 추가 금지
   - "나중을 위해" 추가하는 코드 금지
   - 과도한 디자인 패턴 적용 금지

5. **성능 최적화**: 
   - 연결 풀 최적화
   - 직렬화 전략 최적화
   - 네트워크 지연 최소화
   - 메모리 사용량 최적화

### 제외 사항

- Redis Cluster/Sentinel 설정 (단일 인스턴스 환경 가정)
- Redis Streams, Pub/Sub 등 현재 사용하지 않는 기능
- 과도한 복잡성을 도입하는 방법
- 공식 문서에 근거하지 않은 방법론
- 현재 요구사항에 없는 캐싱 전략 (향후 확장 가능성만 언급)

## 연구 질문

다음 질문에 대해 **공식 문서를 기반으로** 답변하세요:

### 1. 연결 풀 최적화

1.1. **Lettuce vs Jedis**:
   - Spring Boot 4.0.1에서 기본 사용하는 Lettuce의 장단점은?
   - 프로덕션 환경에서 Lettuce 연결 풀 설정의 적절한 값은?
   - `spring.data.redis.lettuce.pool` 설정의 권장 값은?

1.2. **연결 풀 설정**:
   - `max-active`, `max-idle`, `min-idle`의 적절한 값은?
   - 멀티모듈 환경에서 각 모듈별로 다른 연결 풀 설정이 필요한가?
   - 타임아웃 설정의 적절한 값은?

### 2. 직렬화 전략 최적화

2.1. **현재 직렬화 전략 분석**:
   - 현재 `StringRedisSerializer`만 사용하는 것이 적절한가?
   - OAuth State와 Kafka 이벤트 ID는 String이므로 현재 전략이 적합한가?

2.2. **향후 확장성**:
   - 복잡한 객체를 저장해야 할 경우 어떤 직렬화 전략을 사용해야 하는가?
   - JSON 직렬화 vs Java 직렬화 비교
   - 성능 및 호환성 측면에서 권장 사항은?

### 2.3. TTL 설정 일관성 개선

2.3.1. **현재 TTL 설정 방법 분석**:
   - OAuthStateService: `Duration` 객체를 직접 사용 (`redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(10))`)
   - EventConsumer: `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` 혼용
   - 두 방법의 차이점과 장단점은?

2.3.2. **권장 TTL 설정 방법**:
   - Spring Data Redis 공식 문서에서 권장하는 TTL 설정 방법은?
   - `Duration` 객체 직접 사용 vs `long` + `TimeUnit` 비교
   - 일관성 있는 TTL 설정 방법 제시

### 3. RedisTemplate 설정 최적화

3.1. **현재 RedisConfig 분석**:
   - 현재 설정의 문제점은?
   - `afterPropertiesSet()` 호출이 필요한가?
   - 트랜잭션 지원이 필요한가?

3.2. **최적화 방안**:
   - `enableDefaultSerializer` 설정의 적절한 값은?
   - `enableTransactionSupport` 설정이 필요한가?
   - `setExposeConnection` 설정의 필요성은?

### 4. 성능 최적화

4.1. **네트워크 최적화**:
   - Pipeline/Batch 작업이 필요한가?
   - 현재 사용 사례(OAuth State, Kafka 이벤트)에서 Pipeline이 유용한가?

4.2. **메모리 최적화**:
   - Key 네이밍 전략 (현재 `oauth:state:`, `processed_event:` 사용)
   - TTL 설정 최적화 및 일관성 개선
     * 현재 EventConsumer에서 `Duration.ofDays().toSeconds()`와 `TimeUnit.SECONDS` 혼용
     * OAuthStateService는 `Duration` 객체를 직접 사용
     * TTL 설정 방법의 일관성 개선 방안 제시
     * Spring Data Redis 공식 문서에서 권장하는 TTL 설정 방법은?
   - 메모리 사용량 모니터링 방법

4.3. **응답 시간 최적화**:
   - 비동기 작업이 필요한가?
   - `ReactiveRedisTemplate` 사용이 필요한가?

### 5. 모니터링 및 관찰 가능성

5.1. **메트릭 수집**:
   - Spring Boot Actuator를 통한 Redis 메트릭 수집 방법
   - 커스텀 메트릭 수집 방법

5.2. **로깅**:
   - Redis 작업 로깅 전략
   - 성능 로깅 (Slow Query 등)

### 6. 보안 고려사항

6.1. **연결 보안**:
   - Redis 인증(AUTH) 설정 방법
   - TLS/SSL 암호화 전송 설정 방법
   - 네트워크 격리 전략

6.2. **데이터 보안**:
   - 민감한 데이터(OAuth State) 저장 시 보안 고려사항
   - Key 네이밍 전략의 보안 영향

### 7. 멀티모듈 환경 최적화

7.1. **설정 공유**:
   - `common/core` 모듈에서 공통 Redis 설정 제공 방법
     * `common/core/src/main/resources/application-common-core.yml` 파일 생성
     * 다른 모듈에서 `spring.profiles.include: common-core`로 공통 설정 참조
   - 모듈별 커스텀 설정이 필요한 경우 처리 방법

7.2. **의존성 관리**:
   - `spring-boot-starter-data-redis` 의존성 전이 설정
   - 모듈별 필요한 Redis 기능만 의존성 추가
   - Spring Session Data Redis 의존성 관리
     * 현재 `common/security` 모듈에 의존성이 있으나 실제로는 사용하지 않음
     * Stateless 아키텍처에서 Spring Session Data Redis 의존성 유지 여부 판단
     * 불필요한 의존성 제거 vs 향후 사용 가능성을 위한 유지 비교

## 참고 자료 (공식 문서만 사용)

### 필수 참고 문서

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

4. **Lettuce 공식 문서** (Spring Boot 기본 클라이언트)
   - Lettuce GitHub: https://github.com/lettuce-io/lettuce-core
   - Lettuce Connection Pooling: https://github.com/lettuce-io/lettuce-core/wiki/Connection-Pooling

### 참고 금지

- 블로그 포스트, 개인 의견, 비공식 튜토리얼
- 공식 문서에 근거하지 않은 방법론
- 오래된 버전의 문서 (Spring Boot 4.0.1 기준)

## 분석 전략 (Chain of Thought)

### 1단계: 현재 구현 분석

**분석 대상**:
1. `common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java`
2. `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthStateService.java`
3. `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`
   - **특별 분석**: TTL 설정 방법의 일관성 (Duration vs TimeUnit 혼용 문제)
4. `common/core/build.gradle` (의존성 확인)
5. `common/security/build.gradle` (Spring Session Data Redis 의존성 확인)
   - **특별 분석**: Spring Session Data Redis 의존성이 있으나 실제 사용 여부 확인
   - `common/security/src/main/java/com/ebson/shrimp/tm/demo/common/security/config/SecurityConfig.java` 확인
6. `api/auth/src/main/resources/application-auth-api.yml` (Redis 연결 설정)

**분석 항목**:
- 현재 Redis 설정의 문제점 및 개선 가능성
- 사용 패턴 분석 (OAuth State, Kafka 이벤트 멱등성)
  - TTL 설정 방법의 일관성 문제 (Duration vs TimeUnit 혼용)
  - Spring Session Data Redis 실제 사용 여부 확인
- 성능 병목 지점 식별
- 보안 취약점 식별

### 2단계: 공식 문서 기반 베스트 프랙티스 연구

**연구 항목**:
1. Spring Data Redis 공식 문서의 권장 설정
2. Redis 공식 문서의 성능 최적화 가이드
3. Lettuce 연결 풀 최적화 방법
4. 직렬화 전략 비교 및 권장 사항
5. 모니터링 및 관찰 가능성 설정

**검증 원칙**:
- 공식 문서의 예제 코드만 인용
- 최신 버전 정보 확인 (Spring Boot 4.0.1 기준)
- 공식 문서에 없는 내용은 추측하지 않음
- 불확실한 내용은 명시적으로 표시
- 오버엔지니어링 방지: 필요한 기능만 구현, 불필요한 복잡도 제거

### 3단계: 멀티모듈 환경 최적화

**고려 사항**:
1. `common/core` 모듈에서 공통 설정 제공 방법
2. 모듈별 커스텀 설정이 필요한 경우 처리 방법
3. 의존성 전이 설정 최적화
4. 모듈 간 Redis 사용 패턴 일관성 유지

### 4단계: 현재 사용 사례 최적화

**최적화 대상**:
1. OAuth State 저장/조회 최적화
2. Kafka 이벤트 멱등성 보장 최적화
3. 향후 확장 가능성 고려 (캐싱, Rate Limiting 등)

## 출력 형식

다음 형식으로 결과를 제시하세요:

### 1. 현재 구현 분석

**현재 Redis 설정의 문제점**:
- [문제점 1]
- [문제점 2]
- [문제점 3]

**현재 사용 패턴 분석**:
- OAuth State 저장: [분석 결과]
  - TTL 설정 방법: `Duration` 객체 직접 사용
- Kafka 이벤트 멱등성: [분석 결과]
  - TTL 설정 방법: `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` 혼용
  - 일관성 개선 필요성
- Spring Session Data Redis: [실제 사용 여부 확인 결과]
  - 의존성 포함 여부 vs 실제 사용 여부
  - Stateless 아키텍처와의 관계

### 2. 연결 풀 최적화

**Lettuce 연결 풀 설정**:
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: [권장 값]
          max-idle: [권장 값]
          min-idle: [권장 값]
          max-wait: [권장 값]
```

**설정 근거**:
- [공식 문서 참조 및 근거]

**타임아웃 설정**:
```yaml
spring:
  data:
    redis:
      timeout: [권장 값]
      lettuce:
        shutdown-timeout: [권장 값]
```

### 3. RedisTemplate 설정 최적화

**최적화된 RedisConfig**:
```java
@Configuration
public class RedisConfig {
    // [최적화된 설정 코드]
}
```

**변경 사항 설명**:
- [변경 사항 1 및 근거]
- [변경 사항 2 및 근거]

### 4. 직렬화 전략

**현재 전략 평가**:
- StringRedisSerializer 사용이 현재 사용 사례에 적합한가?

**향후 확장성**:
- 복잡한 객체 저장 시 권장 직렬화 전략
- JSON 직렬화 vs Java 직렬화 비교

### 4.1. TTL 설정 일관성 개선

**현재 TTL 설정 방법 비교**:
- OAuthStateService: `Duration` 객체 직접 사용
- EventConsumer: `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` 혼용

**권장 TTL 설정 방법**:
- Spring Data Redis 공식 문서 권장 방법
- 일관성 있는 TTL 설정 코드 예시
```java
// [권장 TTL 설정 방법 예시]
```

### 5. 성능 최적화

**네트워크 최적화**:
- Pipeline/Batch 작업 필요성
- 비동기 작업 필요성

**메모리 최적화**:
- Key 네이밍 전략 개선
- TTL 설정 최적화 및 일관성 개선
  - EventConsumer와 OAuthStateService의 TTL 설정 방법 통일
  - `Duration` 객체 직접 사용 vs `Duration.toSeconds()` + `TimeUnit` 비교
  - Spring Data Redis 공식 문서 권장 방법 제시

### 6. 모니터링 및 관찰 가능성

**Spring Boot Actuator 설정**:
```yaml
management:
  metrics:
    export:
      # [메트릭 수집 설정]
```

**커스텀 메트릭 수집**:
- [커스텀 메트릭 수집 방법]

### 7. 보안 고려사항

**연결 보안**:
- Redis 인증 설정 방법
- TLS/SSL 암호화 전송 설정

**데이터 보안**:
- 민감한 데이터 저장 시 보안 고려사항

### 8. 멀티모듈 환경 최적화

**공통 설정 제공 방법**:
- `common/core` 모듈에서 공통 RedisConfig 제공
- 모듈별 커스텀 설정 방법

**의존성 관리**:
- 의존성 전이 설정 최적화
- Spring Session Data Redis 의존성 관리
  - 현재 사용 여부 확인 결과
  - Stateless 아키텍처에서의 의존성 유지 여부 권장 사항

### 9. 구현 가이드

**단계별 구현 가이드**:
1. [구현 단계 1]
2. [구현 단계 2]
3. [구현 단계 3]

**코드 예시**:
```java
// [최적화된 구현 예시]
```

### 10. 검증 기준

제시된 최적화 방안은 다음 기준을 만족해야 합니다:

1. ✅ **성능 향상**: 연결 풀 최적화로 응답 시간 개선
2. ✅ **메모리 효율성**: 불필요한 메모리 사용 최소화
3. ✅ **확장성**: 향후 사용 사례 추가 시 유연한 확장 가능
4. ✅ **보안성**: Redis 연결 및 데이터 보안 강화
5. ✅ **관찰 가능성**: 모니터링 및 메트릭 수집 가능
6. ✅ **멀티모듈 호환성**: 멀티모듈 환경에서 일관된 사용 패턴
7. ✅ **실용성**: 프로덕션 환경 적용 가능
8. ✅ **단순성**: 오버엔지니어링 없이 요구사항 충족

## 주의사항

- **공식 문서만 참고**: 추측이나 개인 의견 배제
- **프로젝트 컨텍스트 준수**: 멀티모듈 환경, 현재 사용 사례 고려
- **실용성 우선**: 이론적 완벽함보다 실용적 해결책 제시
- **간결성**: 불필요한 복잡성 도입 지양
- **오버엔지니어링 방지**: 현재 요구사항에 맞는 최적화만 제시
- **향후 확장성**: 현재 사용하지 않는 기능은 언급만 하고 구현하지 않음

## 참고 설계서 및 프롬프트

다음 문서들을 참고하여 일관성을 유지하세요:

### 설계서 (`docs/` 폴더)
- `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 방법 연구 결과
- `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드
- `docs/step6/oauth-provider-implementation-guide.md`: OAuth Provider 구현 가이드

### 프롬프트 (`prompts/` 폴더)
- `prompts/shrimp-task-prompt.md`: 프로젝트 전체 목표 및 단계별 구현 사항
- `prompts/step6/oauth-state-storage-research-prompt.md`: OAuth State 저장 연구 프롬프트

---

**작성일**: 2025-01-27  
**프롬프트 버전**: 1.0  
**대상**: 멀티모듈 Spring Boot 애플리케이션에서 Redis 최적화 베스트 프랙티스 연구  
**출력 파일**: `docs/step7/redis-optimization-best-practices.md`
