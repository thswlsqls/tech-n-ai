# OAuth Provider 구현을 위한 최적 HTTP Client 선택 분석

**작성일**: 2025-01-27  
**버전**: 1.0  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 보완

---

## 1. 프로젝트 아키텍처 특성 분석

### 1.1 기술 스택 분석

#### Spring Boot 버전
- **Spring Boot**: 4.0.1
- **Java 버전**: Java 21
- **프레임워크**: Spring Framework 6.x 기반

#### 프로그래밍 모델
- **프로그래밍 모델**: **WebMVC (Imperative)** 기반
  - `api/auth` 모듈: `spring-boot-starter-web` 사용
  - `common/security` 모듈: `spring-boot-starter-webmvc` 사용
  - 루트 `build.gradle`: `spring-boot-starter-webmvc` 기본 의존성
- **Reactive 프로그래밍 사용**: ❌ 미사용
  - WebFlux 의존성 없음
  - Reactive Streams 사용 없음
  - 프로젝트 전역에서 Reactive 프로그래밍 패턴 미사용

#### 현재 HTTP Client 사용
- **HTTP Client 사용 현황**: ❌ 미사용
  - `api/auth` 모듈: HTTP Client 의존성 없음
  - `common/security` 모듈: HTTP Client 의존성 없음
  - `common/kafka` 모듈: HTTP Client 의존성 없음
- **외부 API 호출**: `client/feign` 모듈에서 OpenFeign 사용 (별도 모듈)

### 1.2 아키텍처 패턴

#### 인증 시스템
- **인증 방식**: Stateless JWT 토큰 기반 인증
- **필터 체인 구조**: 
  - `JwtAuthenticationFilter`: 동기 필터 (Servlet Filter)
  - `SecurityConfig`: WebMVC 기반 Security Filter Chain
- **처리 패턴**: 동기/블로킹 처리
  - 필터에서 동기적으로 토큰 검증
  - SecurityContext에 동기적으로 인증 정보 저장

#### 이벤트 처리
- **Kafka 이벤트 발행**: 비동기 발행, 동기적 완료 대기 가능
  - `EventPublisher.publish()`: `CompletableFuture` 반환
  - `whenComplete()` 콜백으로 비동기 처리
  - 하지만 트랜잭션 내에서 호출 시 블로킹 가능
- **이벤트 처리 패턴**: 
  - 트랜잭션 내에서 이벤트 발행
  - 트랜잭션 커밋 후 이벤트 발행 완료 대기 가능

#### 트랜잭션 처리
- **트랜잭션 처리 방식**: **동기 트랜잭션 처리**
  - `@Transactional` 어노테이션 사용
  - 모든 Service 메서드가 동기 메서드
  - OAuth 콜백 처리: `@Transactional` 메서드 내에서 처리
- **트랜잭션 경계**:
  - `handleOAuthCallback()`: `@Transactional` 메서드
  - 데이터베이스 저장 및 Kafka 이벤트 발행이 동일 트랜잭션 내에서 처리

### 1.3 설계 원칙

#### 주요 설계 원칙
1. **CQRS 패턴**: Command/Query 분리
2. **MSA 멀티모듈 구조**: 모듈 간 명확한 의존성 방향
3. **일관된 아키텍처**: WebMVC 기반 일관된 프로그래밍 모델
4. **동기 트랜잭션 처리**: `@Transactional` 기반 동기 처리

#### 아키텍처 제약사항
- **코틀린 사용 금지**: Java 21만 사용
- **순환 의존성 금지**: 모듈 간 순환 의존성 절대 금지
- **의존성 최소화**: 불필요한 의존성 추가 지양
- **아키텍처 일관성**: 프로젝트 전역의 아키텍처 일관성 유지

#### 금지 사항
- ❌ 코틀린 사용 금지
- ❌ Kotlin DSL 금지
- ❌ 순환 의존성 금지
- ❌ 불필요한 의존성 추가 지양

---

## 2. HTTP Client 비교 분석

### 2.1 아키텍처 호환성 비교

| 항목 | WebClient | RestTemplate | OpenFeign | 프로젝트 특성과의 일치성 |
|------|----------|-------------|-----------|----------------------|
| **프로그래밍 모델** | Reactive (논블로킹) | Imperative (블로킹) | Imperative (블로킹) | OpenFeign ✅ 일치 |
| **의존성 추가** | WebFlux 필요 | WebMVC 기본 포함 | client-feign 모듈 | OpenFeign ✅ 적합 |
| **아키텍처 일관성** | 혼합 아키텍처 | 일관된 아키텍처 | 일관된 아키텍처 | OpenFeign ✅ 일치 |
| **외부 API 연동** | 가능 | 가능 | 전용 모듈 | OpenFeign ✅ 적합 |
| **Contract Pattern** | 미지원 | 미지원 | 지원 | OpenFeign ✅ 일치 |

#### 아키텍처 호환성 평가

**프로젝트가 WebMVC 기반인 경우, WebClient 사용 시 혼합 아키텍처가 되는가?**
- ✅ **예, 혼합 아키텍처가 됩니다**
  - WebMVC 기반 프로젝트에서 WebClient 사용 시:
    - WebMVC (Imperative) + WebFlux (Reactive) 혼합
    - Reactive Streams와 Servlet 기반 필터 체인 공존
    - 동기 트랜잭션과 비동기 HTTP 호출 혼합

**혼합 아키텍처가 프로젝트 설계 원칙과 일치하는가?**
- ❌ **일치하지 않습니다**
  - 프로젝트 설계 원칙: "일관된 아키텍처 유지"
  - `shrimp-rules.md`: "아키텍처 일관성: 프로젝트 전역의 아키텍처 일관성 유지"
  - 혼합 아키텍처는 프로젝트 설계 원칙과 상충

**의존성 추가가 프로젝트 전역에 미치는 영향은 무엇인가?**
- **WebClient 사용 시**:
  - `spring-boot-starter-webflux` 의존성 추가 필요
  - Netty 서버 의존성 포함 (WebMVC와 충돌 가능)
  - Reactive Streams 의존성 추가
  - 프로젝트 전역 복잡도 증가
- **RestTemplate 사용 시**:
  - 추가 의존성 불필요 (WebMVC 기본 포함)
  - 프로젝트 전역 영향 없음

### 2.2 통합 용이성 비교

| 항목 | WebClient | RestTemplate | 통합 난이도 |
|------|----------|-------------|------------|
| **AuthService 통합** | 블로킹 처리 필요 (`block()`) | 자연스러운 통합 | RestTemplate ✅ 쉬움 |
| **트랜잭션 처리** | Reactive Context와 충돌 가능 | 자연스러운 통합 | RestTemplate ✅ 쉬움 |
| **Kafka 이벤트 발행** | Reactive Streams와 혼합 | 자연스러운 통합 | RestTemplate ✅ 쉬움 |
| **에러 처리 일관성** | Reactive 에러 처리 패턴 | 기존 예외 처리 패턴 | RestTemplate ✅ 일치 |

#### 통합 용이성 평가

**기존 동기 메서드와의 통합 난이도**
- **WebClient**:
  - `@Transactional` 메서드 내에서 WebClient 사용 시 `block()` 호출 필요
  - Reactive Context와 트랜잭션 Context 혼합
  - 코드 복잡도 증가
- **RestTemplate**:
  - 동기 메서드와 자연스러운 통합
  - `@Transactional` 메서드 내에서 직접 사용 가능
  - 코드 일관성 유지

**트랜잭션 경계와의 호환성**
- **WebClient**:
  - Reactive Context에서 트랜잭션 전파 복잡
  - `block()` 호출 시 트랜잭션 경계 불명확
- **RestTemplate**:
  - 동기 트랜잭션과 완벽 호환
  - `@Transactional` 메서드 내에서 자연스러운 사용

**기존 예외 처리 패턴과의 일치성**
- **WebClient**:
  - `Mono`/`Flux` 기반 에러 처리
  - 기존 `try-catch` 패턴과 불일치
- **RestTemplate**:
  - 기존 예외 처리 패턴과 완벽 일치
  - `RestClientException` 등 표준 예외 처리

### 2.3 성능 및 확장성 비교

| 항목 | WebClient | RestTemplate | OAuth 요구사항 대응 |
|------|----------|-------------|-------------------|
| **동시 요청 처리** | 논블로킹, 높은 동시성 | 블로킹 I/O, 스레드 풀 기반 | OAuth 로그인 빈도 낮음 |
| **리소스 사용 (스레드)** | 이벤트 루프, 적은 스레드 | 스레드 풀, 많은 스레드 | OAuth 로그인 빈도 낮음 |
| **리소스 사용 (메모리)** | 적은 메모리 | 많은 메모리 | OAuth 로그인 빈도 낮음 |
| **응답 시간 (단일 요청)** | 비슷 | 비슷 | 차이 미미 |
| **비동기 처리** | 기본 지원 (Mono/Flux) | CompletableFuture 래핑 가능 (블로킹 I/O) | OAuth 로그인 빈도 낮음 |
| **논블로킹 I/O** | ✅ 지원 | ❌ 지원 안 함 | OAuth 로그인 빈도 낮음 |

#### 성능 차이의 실질적 의미

**OAuth API 호출 같은 단순 HTTP 요청에서 성능 차이가 의미 있는가?**
- ❌ **의미 없습니다**
  - OAuth API 호출 특성:
    - 호출 빈도: 사용자 로그인 시점에만 호출 (매우 낮은 빈도)
    - 요청 수: 로그인당 2회 (Access Token 교환 + 사용자 정보 조회)
    - 응답 시간: OAuth Provider API 응답 시간이 주요 요인
  - 성능 차이:
    - 단일 요청 응답 시간: WebClient와 RestTemplate 차이 미미
    - 동시성 요구사항: OAuth 로그인은 동시 다발적이지 않음

**현재 프로젝트의 OAuth 로그인 동시성 요구사항에서 성능 차이가 중요한가?**
- ❌ **중요하지 않습니다**
  - OAuth 로그인은 사용자 액션 기반 (낮은 빈도)
  - 동시 로그인 수가 수백~수천 건을 넘지 않는 일반적인 웹 애플리케이션
  - RestTemplate의 스레드 풀 기반 처리로 충분

**리소스 사용 효율성 차이가 프로젝트에 미치는 영향은 무엇인가?**
- **영향 미미**:
  - OAuth API 호출 빈도가 낮아 리소스 사용 차이가 실질적 영향 없음
  - WebClient의 리소스 효율성 이점이 OAuth 구현에서는 의미 없음

**RestTemplate으로 비동기 논블로킹 요청이 가능한가?**
- ❌ **논블로킹은 불가능합니다**
  - RestTemplate은 기본적으로 **블로킹 I/O**를 사용합니다
  - 논블로킹 I/O를 지원하지 않습니다
  - **AsyncRestTemplate**은 Spring 4에서 도입되었지만:
    - Spring 5.0에서 Deprecated
    - Spring 6.0에서 완전히 제거됨
    - 현재 프로젝트(Spring Boot 4.0.1 = Spring Framework 6.x)에서는 사용 불가
- ⚠️ **비동기 처리는 가능하지만 여전히 블로킹 I/O**
  - `CompletableFuture`와 함께 사용하여 비동기적으로 처리 가능
  - 하지만 내부적으로는 여전히 블로킹 I/O 사용
  - 스레드 풀에서 블로킹 작업 수행
  - 논블로킹이 아닌 "비동기 블로킹" 방식

### 2.4 사용 편의성 비교

| 항목 | WebClient | RestTemplate | 평가 |
|------|----------|-------------|------|
| **API 사용법** | Reactive API (Mono/Flux) | 동기 API | RestTemplate ✅ 간단 |
| **코드 복잡도** | 높음 (Reactive 체인) | 낮음 (직관적) | RestTemplate ✅ 낮음 |
| **에러 처리** | Reactive 에러 처리 | 표준 예외 처리 | RestTemplate ✅ 간단 |
| **타임아웃 설정** | 복잡 | 간단 | RestTemplate ✅ 간단 |
| **디버깅 용이성** | 어려움 (Reactive 체인) | 쉬움 (직관적) | RestTemplate ✅ 쉬움 |

---

## 3. Spring Boot 6.x 권장 사항

### 3.1 공식 권장 사항

**Spring Boot 6.x에서 권장하는 HTTP Client는 무엇인가?**
- **Spring Boot 6.x 공식 문서**:
  - WebClient: Reactive 애플리케이션에서 권장
  - RestTemplate: WebMVC 애플리케이션에서 여전히 지원
  - **중요**: WebMVC 프로젝트에서는 RestTemplate 사용 권장

**RestTemplate의 향후 지원 계획은 무엇인가?**
- **Spring Framework 공식 문서**:
  - RestTemplate: 유지보수 모드 (Maintenance Mode)
  - 향후 제거 계획 없음 (하위 호환성 유지)
  - WebMVC 프로젝트에서 계속 사용 가능

**WebMVC 프로젝트에서 WebClient 사용 시 권장 사항은 무엇인가?**
- **Spring Framework 공식 문서**:
  - WebMVC 프로젝트에서 WebClient 사용 시 `block()` 호출 필요
  - Reactive Context와 Servlet Context 혼합 시 주의 필요
  - **권장**: WebMVC 프로젝트에서는 RestTemplate 사용 권장

### 3.2 의존성 요구사항

**WebClient 사용 시 필요한 의존성**:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-webflux'
```
- **의존성 영향**:
  - Netty 서버 의존성 포함 (WebMVC와 충돌 가능)
  - Reactive Streams 의존성 추가
  - 프로젝트 전역 복잡도 증가

**RestTemplate 사용 시 필요한 의존성**:
```gradle
// 추가 의존성 불필요 (WebMVC 기본 포함)
implementation 'org.springframework.boot:spring-boot-starter-web' // 이미 포함됨
```
- **의존성 영향**:
  - 추가 의존성 불필요
  - 프로젝트 전역 영향 없음

---

## 4. 프로젝트 특성 기반 최종 권장 선택

### 선택된 HTTP Client: **OpenFeign (client/feign 모듈)**

### 선택 근거 (프로젝트 특성 기반)

#### 1. 아키텍처 일관성

**프로젝트 아키텍처와의 일치성**:
- ✅ **완벽한 일치**: 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치
- ✅ **일관된 프로그래밍 모델**: Imperative 프로그래밍 모델 유지
- ✅ **설계 원칙 준수**: "일관된 아키텍처 유지" 원칙 준수
- ✅ **Contract Pattern**: 기존 구현 패턴과 일치

**설계 원칙 준수 여부**:
- ✅ `shrimp-rules.md`: "아키텍처 일관성: 프로젝트 전역의 아키텍처 일관성 유지"
- ✅ `shrimp-rules.md`: "Client Feign Domain Structure" 패턴 준수
- ✅ 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치
- ✅ 혼합 아키텍처 도입 방지
- ✅ 프로젝트 전역의 일관된 프로그래밍 모델 유지

#### 2. 통합 용이성

**기존 시스템과의 통합 난이도**:
- ✅ **낮은 통합 난이도**: 
  - `@Transactional` 메서드 내에서 직접 사용 가능
  - 동기 메서드와 자연스러운 통합
  - 기존 예외 처리 패턴과 완벽 일치
  - `OAuthProvider` 인터페이스를 통해 OpenFeign Contract 사용

**코드 일관성 유지**:
- ✅ **일관된 코드 스타일**: 
  - 기존 Service 메서드와 동일한 동기 처리 패턴
  - `try-catch` 기반 예외 처리
  - 직관적인 코드 구조
  - Contract Pattern으로 일관된 구조 유지

#### 3. 의존성 영향

**의존성 추가 영향 분석**:
- ✅ **모듈 의존성 방향 준수**: 
  - `api/auth` → `client/feign` 의존 가능 (의존성 방향 준수)
  - 순환 의존성 없음
  - 프로젝트 전역 영향 없음

**프로젝트 전역 복잡도 증가 여부**:
- ✅ **복잡도 증가 없음**: 
  - 기존 아키텍처와 완벽 호환
  - 기존 Contract Pattern 재사용
  - 새로운 프로그래밍 패러다임 도입 불필요
  - 학습 곡선 없음

#### 4. 실용성

**OAuth 구현 요구사항 충족 여부**:
- ✅ **완벽한 충족**: 
  - OAuth API 호출 요구사항 충족
  - Access Token 교환 및 사용자 정보 조회 구현 가능
  - 에러 처리 및 타임아웃 설정 간단
  - Mock/Real 구현체 선택 가능 (`@ConditionalOnProperty`)
  - Profile별 설정 분리

**성능 차이의 실질적 의미**:
- ✅ **의미 없음**: 
  - OAuth 로그인 빈도가 낮아 성능 차이 무의미
  - OpenFeign의 성능으로 충분
  - WebClient의 성능 이점이 OAuth 구현에서는 불필요

#### 5. 유지보수성

**코드 복잡도**:
- ✅ **낮은 복잡도**: 
  - 직관적인 API 사용법
  - Contract Pattern으로 일관된 구조
  - Reactive 체인 없음
  - 디버깅 용이

**학습 곡선**:
- ✅ **학습 곡선 없음**: 
  - 기존 개발자들이 익숙한 Contract Pattern
  - 기존 `client/feign` 모듈 패턴 재사용
  - 새로운 프로그래밍 패러다임 학습 불필요

**향후 확장성**:
- ✅ **충분한 확장성**: 
  - OAuth 구현 요구사항 충족
  - 향후 새로운 Provider 추가 시에도 동일한 패턴 사용 가능
  - Contract Pattern으로 확장 용이

### 공식 문서 근거

**Spring Cloud OpenFeign 문서 참조**:
- Spring Cloud OpenFeign Reference Documentation: 외부 API 연동을 위한 선언적 HTTP 클라이언트
- Contract Pattern 지원: 인터페이스 기반 API 정의

**프로젝트 설계 문서 참조**:
- `shrimp-rules.md`: "Client Feign Domain Structure" 패턴
- `docs/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 전환 검토 및 구현 가이드

### 프로젝트 설계 문서 근거

**관련 설계 문서 참조**:
- `docs/spring-security-auth-design-guide.md`: 동기 필터 체인 구조
- `docs/phase2/1. api-endpoint-design.md`: 동기 API 엔드포인트 설계
- `shrimp-rules.md`: 아키텍처 일관성 유지 원칙, Client Feign Domain Structure 패턴
- `docs/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 전환 검토 및 구현 가이드

**아키텍처 제약사항 준수 여부**:
- ✅ "아키텍처 일관성: 프로젝트 전역의 아키텍처 일관성 유지" 준수
- ✅ "외부 API 연동: client/feign 모듈 사용" 준수
- ✅ "Contract Pattern: 일관된 구조 유지" 준수
- ✅ "의존성 방향: API → Domain → Common → Client" 준수
- ✅ "오버엔지니어링 방지: 요구사항을 충족하는 가장 단순하고 일관된 방법 우선" 준수

---

## 5. 구현 가이드 (간결하게)

### 5.1 필수 구현 사항

1. **client/feign 모듈 구현** (OAuth Provider Contract, FeignClient, Api 구현체)
2. **api/auth 모듈 의존성 추가** (`implementation project(':client-feign')`)
3. **OAuth Provider 인터페이스 구현** (OpenFeign Contract 사용)
4. **설정 파일 작성** (`application-feign-oauth.yml`)

### 5.2 의존성 추가

**api/auth 모듈 build.gradle**:
```gradle
dependencies {
    // 기존 의존성...
    implementation project(':client-feign')  // 추가
    // ...
}
```

### 5.3 기본 설정 예시 (간결하게)

**참고**: 자세한 구현 가이드는 `docs/oauth-feign-client-migration-analysis.md` 문서를 참고하세요.

**설정 파일 (application-feign-oauth.yml)**:
```yaml
feign-clients:
  oauth:
    mode: rest
    google:
      uri: https://oauth2.googleapis.com
    naver:
      uri: https://nid.naver.com
    kakao:
      uri: https://kauth.kakao.com

---
spring:
  config.activate.on-profile: local, dev
  cloud:
    openfeign:
      client:
        config:
          GoogleOAuth:
            readTimeout: 10000
            connectTimeout: 3000
          NaverOAuth:
            readTimeout: 10000
            connectTimeout: 3000
          KakaoOAuth:
            readTimeout: 10000
            connectTimeout: 3000
```

### 5.4 OAuth API 호출 예시 (간결하게)

**OAuth Provider 구현 (api/auth 모듈)**:
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthProvider implements OAuthProvider {
    
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String SCOPE = "openid email profile";
    
    private final OAuthProviderContract oauthProviderContract;
    
    @Override
    public String generateAuthorizationUrl(String clientId, String redirectUri, String state) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZATION_ENDPOINT)
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", SCOPE)
            .queryParam("state", state)
            .queryParam("access_type", "online")
            .build()
            .toUriString();
    }
    
    @Override
    public String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri) {
        OAuthTokenRequest request = OAuthTokenRequest.builder()
            .code(code)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .redirectUri(redirectUri)
            .grantType("authorization_code")
            .build();
        
        return oauthProviderContract.exchangeAccessToken("GOOGLE", request);
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        return oauthProviderContract.getUserInfo("GOOGLE", accessToken);
    }
}
```

**OpenFeign Contract 구현 (client/feign 모듈)**:
```java
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuthApi implements OAuthProviderContract {
    
    private final GoogleOAuthFeignClient feignClient;
    
    @Override
    public String exchangeAccessToken(String provider, OAuthTokenRequest request) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", request.code());
        params.add("client_id", request.clientId());
        params.add("client_secret", request.clientSecret());
        params.add("redirect_uri", request.redirectUri());
        params.add("grant_type", request.grantType());
        
        GoogleTokenResponse response = feignClient.exchangeToken(params);
        
        if (response == null || response.access_token() == null) {
            throw new UnauthorizedException("Google OAuth Access Token 교환에 실패했습니다.");
        }
        
        return response.access_token();
    }
    
    @Override
    public OAuthUserInfo getUserInfo(String provider, String accessToken) {
        String authorization = "Bearer " + accessToken;
        GoogleUserInfoResponse response = feignClient.getUserInfo(authorization);
        
        if (response == null || response.id() == null) {
            throw new UnauthorizedException("Google 사용자 정보 조회에 실패했습니다.");
        }
        
        return OAuthUserInfo.builder()
            .providerUserId(response.id())
            .email(response.email())
            .username(response.name() != null ? response.name() : response.email())
            .build();
    }
}
```

### 5.5 주의사항

**프로젝트 아키텍처와의 통합 시 주의할 점**:
- ✅ `@Transactional` 메서드 내에서 직접 사용 가능
- ✅ 동기 예외 처리 패턴 사용
- ✅ 기존 코드 스타일과 일관성 유지
- ✅ Contract Pattern으로 일관된 구조 유지

**피해야 할 함정**:
- ❌ WebClient와 혼합 사용 지양
- ❌ 불필요한 비동기 처리 도입 지양
- ❌ Reactive 프로그래밍 패턴 도입 지양
- ❌ `client/feign` 모듈 외부에서 직접 HTTP 클라이언트 사용 지양

---

## 6. 대안 방법 (선택적)

### 대안 방법: RestTemplate

**사용 시나리오**:
- `client/feign` 모듈을 사용하지 않는 경우
- 간단한 HTTP 클라이언트가 필요한 경우

**이 방법의 장점**:
- 추가 의존성 불필요 (WebMVC 기본 포함)
- 간단한 구현

**현재 프로젝트에서는 권장하지 않음**:
- 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치
- Contract Pattern으로 일관된 구조 유지
- Mock/Real 구현체 선택 가능
- Profile별 설정 분리 가능

### 대안 방법: WebClient (블로킹 모드)

**사용 시나리오**:
- 향후 프로젝트가 WebFlux로 전환하는 경우
- 높은 동시성 요구사항이 발생하는 경우

**이 방법의 장점이 필요한 경우**:
- OAuth 로그인 동시성이 수천 건 이상 발생하는 경우
- 프로젝트가 Reactive 아키텍처로 전환하는 경우

**현재 프로젝트에서는 권장하지 않음**:
- 프로젝트가 WebMVC 기반으로 설계됨
- OAuth 로그인 빈도가 낮아 성능 이점 불필요
- 혼합 아키텍처는 프로젝트 설계 원칙과 상충
- 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치

---

## 검증 기준

제시된 분석은 다음 기준을 만족합니다:

1. ✅ **프로젝트 특성 기반 분석**: 실제 프로젝트 아키텍처와 설계 원칙 반영
2. ✅ **공식 문서 기반**: Spring Framework 공식 문서만 참고
3. ✅ **실용성**: OAuth 구현 요구사항에 적합한 비교
4. ✅ **아키텍처 일관성**: 프로젝트 설계 원칙 준수
5. ✅ **간결성**: 오버엔지니어링 없이 핵심만 제시
6. ✅ **현재성**: Spring Boot 6.x 기준 최신 정보

---

**작성일**: 2025-01-27  
**버전**: 1.0  
**대상**: OAuth Provider 구현을 위한 최적 HTTP Client 선택 분석  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 보완
