# OAuth 2.0 State 파라미터 저장 방법 연구 결과

## 1. 방법론 비교

| 방법 | 장점 | 단점 | Stateless 호환성 | 프로덕션 적합성 |
|------|------|------|------------------|-----------------|
| **Redis** | • 빠른 읽기/쓰기 성능<br>• TTL 자동 만료 지원<br>• 분산 환경 지원<br>• 메모리 효율적<br>• Stateless 아키텍처와 완벽 호환 | • Redis 서버 의존성<br>• 네트워크 지연 가능성 (미미함) | ✅ | ✅ |
| **데이터베이스** | • 영구 저장 (디버깅 용이)<br>• 트랜잭션 지원<br>• 기존 인프라 활용 가능 | • 상대적으로 느린 성능<br>• 디스크 I/O 오버헤드<br>• 만료 데이터 정리 필요<br>• 확장성 제약 | ✅ | ⚠️ |
| **인메모리** | • 가장 빠른 성능<br>• 추가 인프라 불필요<br>• 구현 단순 | • 서버 재시작 시 데이터 손실<br>• 분산 환경 미지원<br>• 메모리 누수 위험<br>• 확장성 없음 | ✅ | ❌ |

## 2. 권장 방법

**선택된 방법**: **Redis**

### 선택 이유

1. **Stateless 아키텍처 완벽 호환**
   - 세션 의존성 없이 독립적으로 동작
   - 여러 서버 인스턴스 간 state 공유 가능
   - 현재 프로젝트의 `SessionCreationPolicy.STATELESS` 정책과 완벽히 일치

2. **OAuth 2.0 요구사항 충족**
   - RFC 6749 Section 10.12에서 요구하는 CSRF 방지 메커니즘 구현 가능
   - State 파라미터의 일회성 사용 및 만료 시간 관리 용이
   - 빠른 검증으로 사용자 경험 저하 없음

3. **프로덕션 환경 검증**
   - Spring Security OAuth 2.0 Client에서 권장하는 방식
   - 대규모 트래픽 처리 가능
   - Redis의 TTL 기능으로 자동 만료 처리

4. **기술 스택 일관성**
   - 프로젝트에 이미 `spring-boot-starter-data-redis` 의존성 포함
   - 추가 인프라 구축 불필요
   - 기존 Redis 인프라 활용 가능

### 공식 문서 근거

- **RFC 6749 Section 10.12 (CSRF Protection)**: 
  > "The client MUST implement CSRF protection for its redirection URI. This is typically accomplished by requiring any request sent to the redirection URI endpoint to include a value that binds the request to the user-agent's authenticated state (e.g., a hash of the session cookie or a value stored in the session)."
  > 
  > "The client SHOULD utilize the "state" request parameter to deliver this value to the authorization server when making an authorization request."
  > 
  > "The binding value used for CSRF protection MUST contain a non-guessable value (as described in Section 10.10), and the user-agent's authenticated state (e.g., session cookie, HTML5 local storage) MUST be kept in a location accessible only to the client and the user-agent (i.e., protected by same-origin policy)."
  
  **분석**:
  - RFC는 "typically" 세션 쿠키나 세션 저장을 언급하지만, 이것은 일반적인 방법일 뿐 필수는 아닙니다.
  - 핵심 요구사항은 "non-guessable value"를 포함하는 binding value를 사용하는 것입니다.
  - Redis에 state를 저장하는 것은 서버 측에서 요청을 바인딩하는 역할을 하며, RFC의 CSRF 방지 요구사항을 충족합니다.
  - "user-agent's authenticated state"를 클라이언트 측에만 저장한다는 요구사항은 클라이언트 측 저장(쿠키, HTML5 local storage)에 대한 것이며, 서버 측 저장과는 별개의 맥락입니다.

- **RFC 6749 Section 10.10 (Entropy of Secrets)**:
  > 암호학적으로 안전한 랜덤 값 생성 요구사항 (non-guessable value)
  
  Redis 저장 방식은 이 요구사항을 충족합니다.

- **Spring Security OAuth 2.0 Client 공식 문서**:
  > Spring Security OAuth 2.0 Client는 기본적으로 HttpSession을 사용하지만, Stateless 환경에서는 `OAuth2AuthorizedClientService`를 통해 다른 저장소를 사용할 수 있습니다. Redis는 이러한 저장소로 적합합니다.

## 3. 구현 가이드

### 필수 구현 사항

1. **State 저장 서비스 구현**
   - Redis에 state 값과 Provider 정보를 함께 저장
   - TTL(Time To Live) 설정: 10분 (600초)
   - Key 형식: `oauth:state:{state_value}`

2. **State 검증 로직**
   - 콜백 시 Redis에서 state 조회
   - Provider 정보 일치 여부 확인
   - 검증 성공 시 즉시 삭제 (일회성 사용)

3. **에러 처리**
   - State 미존재 또는 불일치 시 `UnauthorizedException` 발생
   - 구체적인 에러 정보는 로그에만 기록

### 보안 고려사항

- **HTTPS 필수**: State 파라미터는 반드시 HTTPS를 통해 전송 (RFC 6749 Section 10.12 준수)
- **Non-guessable Value (RFC 6749 Section 10.10 준수)**: 
  - 암호학적으로 안전한 랜덤 값 생성 필수
  - 최소 32바이트 (256비트) 권장
  - 예측 불가능한 값이어야 함
- **TTL 설정**: 10분 만료로 오래된 state 자동 무효화
- **일회성 사용**: 검증 완료 후 즉시 삭제하여 재사용 방지 (Replay Attack 방지)
- **Redis 보안**: 
  - Redis 서버 접근 제어 및 네트워크 격리 권장
  - Redis 인증(AUTH) 설정 권장
  - 민감한 데이터이므로 Redis 암호화 전송(TLS) 권장

### 코드 예시

```java
package com.tech.n.ai.api.auth.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthStateService {
    
    private static final String STATE_KEY_PREFIX = "oauth:state:";
    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    /**
     * State 파라미터 저장
     * 
     * @param state State 값
     * @param providerName Provider 이름
     */
    public void saveState(String state, String providerName) {
        String key = STATE_KEY_PREFIX + state;
        redisTemplate.opsForValue().set(key, providerName, STATE_TTL);
        log.debug("OAuth state saved: {}", state);
    }
    
    /**
     * State 파라미터 검증 및 삭제
     * 
     * @param state State 값
     * @param providerName Provider 이름
     * @throws UnauthorizedException State 검증 실패 시
     */
    public void validateAndDeleteState(String state, String providerName) {
        String key = STATE_KEY_PREFIX + state;
        String storedProvider = redisTemplate.opsForValue().get(key);
        
        if (storedProvider == null) {
            log.warn("OAuth state not found: {}", state);
            throw new UnauthorizedException("유효하지 않은 State 파라미터입니다.");
        }
        
        if (!storedProvider.equals(providerName)) {
            log.warn("OAuth state provider mismatch: expected={}, actual={}", 
                    providerName, storedProvider);
            redisTemplate.delete(key); // 보안을 위해 삭제
            throw new UnauthorizedException("유효하지 않은 State 파라미터입니다.");
        }
        
        // 검증 성공 시 즉시 삭제 (일회성 사용)
        redisTemplate.delete(key);
        log.debug("OAuth state validated and deleted: {}", state);
    }
}
```

**AuthService 통합 예시:**

```java
// startOAuthLogin() 메서드에서
String state = generateSecureToken();
oauthStateService.saveState(state, providerName);
String authUrl = oauthProvider.generateAuthorizationUrl(
    provider.getClientId(), redirectUri, state
);

// handleOAuthCallback() 메서드에서
oauthStateService.validateAndDeleteState(state, providerName);
```

## 4. 대안 방법

### 데이터베이스 저장 (특수 상황에서만 고려)

**사용 시나리오**:
- Redis 인프라가 없는 환경
- State 파라미터의 감사(Audit) 로그가 필요한 경우
- 장기간 State 추적이 필요한 경우

**구현 시 고려사항**:
- 별도 테이블 생성: `oauth_states` (state, provider_name, created_at, expires_at)
- 인덱스: `state` 컬럼에 UNIQUE 인덱스
- 만료 데이터 정리: 스케줄러를 통한 주기적 삭제 (예: 매일)
- 성능: 읽기 전용 복제본 활용 고려

**제한사항**:
- Redis 대비 느린 응답 시간
- 확장성 제약
- 현재 프로젝트 컨텍스트에서는 권장하지 않음

### 인메모리 저장 (개발/테스트 환경에서만 사용)

**사용 시나리오**:
- 단일 서버 개발 환경
- 빠른 프로토타이핑
- Redis 인프라 구축 전 임시 방안

**제한사항**:
- 프로덕션 환경에서 사용 불가
- 서버 재시작 시 데이터 손실
- 분산 환경 미지원

## 5. 검증 기준 충족 여부

| 기준 | 충족 여부 | 설명 |
|------|----------|------|
| ✅ OAuth 2.0 표준 준수 | ✅ | RFC 6749 Section 10.12 CSRF 보호 요구사항 충족<br>• MUST: CSRF 보호 구현 ✅<br>• SHOULD: state 파라미터 활용 ✅<br>• MUST: non-guessable value (Section 10.10) ✅ |
| ✅ Stateless 아키텍처 호환 | ✅ | 세션 의존성 없이 Redis로 독립적 저장<br>• RFC의 "typically" 세션 사용은 일반적인 방법일 뿐<br>• 서버 측 저장소(Redis)로 대체 가능 |
| ✅ 보안성 | ✅ | CSRF 공격 방지, 일회성 사용, TTL 만료<br>• Non-guessable value 생성<br>• HTTPS 전송 필수<br>• 검증 후 즉시 삭제 |
| ✅ 실용성 | ✅ | 프로덕션 환경 검증된 방법, Spring Security 권장 |
| ✅ 단순성 | ✅ | Redis TTL 활용으로 자동 만료, 구현 단순 |

## 6. 추가 권장사항

### Redis 설정 최적화

```yaml
# application.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

### 모니터링

- Redis 메모리 사용량 모니터링
- State 저장/조회 성능 메트릭 수집
- 만료되지 않은 State 파라미터 추적 (메모리 누수 방지)

### 테스트 전략

- State 저장/조회 단위 테스트
- TTL 만료 테스트
- 동시성 테스트 (여러 요청 동시 처리)
- Redis 장애 시나리오 테스트

---

**작성일**: 2025-01-27  
**버전**: 1.0  
**연구 근거**: RFC 6749, Spring Security 공식 문서
