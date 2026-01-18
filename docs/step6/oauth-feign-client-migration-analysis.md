# OAuth Provider 구현을 OpenFeign 클라이언트로 전환 검토 및 개선 가이드

**작성일**: 2025-01-27  
**버전**: 1.0  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 개선

---

## 1. 구현 가능성 검토 결과

### 결론: ✅ 구현 가능

### 검토 항목

#### 1.1 모듈 의존성

**api/auth 모듈이 client/feign 모듈을 의존할 수 있는가?**

- ✅ **의존 가능**
  - **모듈 의존성 방향**: `API → Domain → Common → Client`
  - `api/auth`는 `API` 계층, `client/feign`은 `Client` 계층
  - 의존성 방향 준수: `api/auth` → `client/feign` ✅
  - `shrimp-rules.md` 확인: "의존성 방향: API → Domain → Common → Client"

**순환 의존성 발생 가능 여부:**

- ❌ **순환 의존성 없음**
  - `client/feign/build.gradle` 확인 결과:
    - `api/auth` 모듈 의존 없음
    - `domain-aurora`, `common-core` 모듈만 의존
  - Client 모듈은 API 모듈을 의존하지 않는 구조
  - 순환 의존성 발생 가능성 없음

#### 1.2 OpenFeign 호환성

**OAuth Provider API 요청을 OpenFeign으로 처리 가능한가?**

##### Token 교환 요청 (POST, application/x-www-form-urlencoded)

- ✅ **지원**
  - OpenFeign은 `application/x-www-form-urlencoded` Content-Type 지원
  - `@RequestBody` 또는 `@RequestParam` 사용 가능
  - `consumes = MediaType.APPLICATION_FORM_URLENCODED` 설정 가능

**구현 방식:**
```java
@PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED)
TokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
```

또는

```java
@PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED)
TokenResponse exchangeToken(
    @RequestParam("code") String code,
    @RequestParam("client_id") String clientId,
    @RequestParam("client_secret") String clientSecret,
    @RequestParam("redirect_uri") String redirectUri,
    @RequestParam("grant_type") String grantType
);
```

##### 사용자 정보 조회 요청 (GET, Bearer Token)

- ✅ **지원**
  - `@RequestHeader("Authorization")` 사용 가능
  - Bearer Token 전달 가능

**구현 방식:**
```java
@GetMapping(value = "/oauth2/v2/userinfo")
UserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
```

또는 Feign RequestInterceptor 사용:
```java
@Bean
public RequestInterceptor oauthRequestInterceptor() {
    return requestTemplate -> {
        // Bearer Token 자동 추가
    };
}
```

##### 동적 URL 처리

- ✅ **지원**
  - Provider별 다른 Base URL 처리 가능
  - `@FeignClient(url = "${feign-clients.oauth.{provider}.uri}")` 사용

**구현 방식:**
```java
@FeignClient(name = "GoogleOAuth", url = "${feign-clients.oauth.google.uri}")
public interface GoogleOAuthFeignClient { ... }

@FeignClient(name = "NaverOAuth", url = "${feign-clients.oauth.naver.uri}")
public interface NaverOAuthFeignClient { ... }

@FeignClient(name = "KakaoOAuth", url = "${feign-clients.oauth.kakao.uri}")
public interface KakaoOAuthFeignClient { ... }
```

#### 1.3 아키텍처 일관성

**OpenFeign 사용이 프로젝트 아키텍처와 일치하는가?**

- ✅ **일치**
  - `client/feign` 모듈은 외부 API 연동 전용 모듈
  - OAuth Provider API는 외부 API이므로 `client/feign` 모듈 사용 적합
  - Contract Pattern 사용으로 일관된 구조 유지

**기존 구현 패턴과의 일치성:**

- ✅ **일치**
  - Contract Pattern 사용 (`SampleContract`, `SampleApi`, `SampleFeignClient`)
  - Mock/Real 구현체 선택 가능 (`@ConditionalOnProperty`)
  - Profile별 설정 분리 (`application-feign-{domain}.yml`)

#### 1.4 통합 용이성

**api/auth 모듈과의 통합 용이성:**

- ✅ **통합 용이**
  - `api/auth` 모듈에서 `client/feign` 모듈의 Contract 인터페이스 주입 가능
  - `@Transactional` 메서드 내에서 동기적으로 호출 가능
  - 기존 예외 처리 패턴과 일치

**트랜잭션 처리와의 호환성:**

- ✅ **호환**
  - OpenFeign은 동기/블로킹 방식
  - `@Transactional` 메서드 내에서 자연스럽게 사용 가능
  - `AuthService.handleOAuthCallback()` 메서드와 완벽 호환

---

## 2. 구현 가이드

### 2.1 client/feign 모듈 구현

#### 파일 구조

```
client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/
├── contract/
│   ├── OAuthProviderContract.java
│   └── OAuthDto.java
├── client/
│   ├── GoogleOAuthFeignClient.java
│   ├── NaverOAuthFeignClient.java
│   └── KakaoOAuthFeignClient.java
├── api/
│   ├── GoogleOAuthApi.java
│   ├── NaverOAuthApi.java
│   └── KakaoOAuthApi.java
├── mock/
│   ├── GoogleOAuthMock.java
│   ├── NaverOAuthMock.java
│   └── KakaoOAuthMock.java
└── config/
    └── OAuthFeignConfig.java
```

#### 구현 단계

##### Step 1: Contract 인터페이스 정의

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/contract/OAuthProviderContract.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.contract;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthTokenRequest;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthTokenResponse;

/**
 * OAuth Provider Contract 인터페이스
 * 
 * OAuth Provider별 공통 비즈니스 메서드 시그니처 정의
 */
public interface OAuthProviderContract {
    
    /**
     * Authorization Code로 Access Token 교환
     * 
     * @param provider Provider 이름 (GOOGLE, NAVER, KAKAO)
     * @param request Token 교환 요청
     * @return Access Token
     */
    String exchangeAccessToken(String provider, OAuthTokenRequest request);
    
    /**
     * Access Token으로 사용자 정보 조회
     * 
     * @param provider Provider 이름 (GOOGLE, NAVER, KAKAO)
     * @param accessToken Access Token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String provider, String accessToken);
}
```

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/contract/OAuthDto.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.contract;

import lombok.Builder;

/**
 * OAuth DTO 클래스
 */
public class OAuthDto {
    
    @Builder
    public record OAuthTokenRequest(
        String code,
        String clientId,
        String clientSecret,
        String redirectUri,
        String grantType
    ) {}
    
    @Builder
    public record OAuthTokenResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        String refreshToken
    ) {}
    
    @Builder
    public record OAuthUserInfo(
        String providerUserId,
        String email,
        String username
    ) {}
    
    // Google OAuth Response
    @Builder
    public record GoogleTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String refresh_token
    ) {}
    
    @Builder
    public record GoogleUserInfoResponse(
        String id,
        String email,
        String name,
        String picture
    ) {}
    
    // Naver OAuth Response
    @Builder
    public record NaverTokenResponse(
        String access_token,
        String refresh_token,
        String token_type,
        Long expires_in
    ) {}
    
    @Builder
    public record NaverUserInfoResponse(
        String resultcode,
        String message,
        NaverUserInfo response
    ) {}
    
    @Builder
    public record NaverUserInfo(
        String id,
        String email,
        String name,
        String nickname
    ) {}
    
    // Kakao OAuth Response
    @Builder
    public record KakaoTokenResponse(
        String access_token,
        String token_type,
        Long expires_in,
        String refresh_token
    ) {}
    
    @Builder
    public record KakaoUserInfoResponse(
        Long id,
        KakaoAccount kakao_account
    ) {}
    
    @Builder
    public record KakaoAccount(
        String email,
        Boolean email_needs_agreement,
        Boolean is_email_valid,
        Boolean is_email_verified,
        KakaoProfile profile
    ) {}
    
    @Builder
    public record KakaoProfile(
        String nickname,
        String thumbnail_image_url,
        String profile_image_url
    ) {}
}
```

##### Step 2: FeignClient 인터페이스 정의

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/client/GoogleOAuthFeignClient.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.GoogleTokenResponse;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.GoogleUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "GoogleOAuth", url = "${feign-clients.oauth.google.uri}")
public interface GoogleOAuthFeignClient {
    
    @PostMapping(
        value = "/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    GoogleTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
    
    @GetMapping(value = "/oauth2/v2/userinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    GoogleUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
```

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/client/NaverOAuthFeignClient.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.NaverTokenResponse;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.NaverUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "NaverOAuth", url = "${feign-clients.oauth.naver.uri}")
public interface NaverOAuthFeignClient {
    
    @PostMapping(
        value = "/oauth2.0/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    NaverTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
    
    @GetMapping(value = "/v1/nid/me", produces = MediaType.APPLICATION_JSON_VALUE)
    NaverUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
```

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/client/KakaoOAuthFeignClient.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.client;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.KakaoTokenResponse;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.KakaoUserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KakaoOAuth", url = "${feign-clients.oauth.kakao.uri}")
public interface KakaoOAuthFeignClient {
    
    @PostMapping(
        value = "/oauth/token",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    KakaoTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
    
    @GetMapping(value = "/v2/user/me", produces = MediaType.APPLICATION_JSON_VALUE)
    KakaoUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
```

##### Step 3: Api 구현체 작성

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/api/GoogleOAuthApi.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.api;

import com.tech.n.ai.client.feign.domain.oauth.client.GoogleOAuthFeignClient;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.*;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

**NaverOAuthApi.java**와 **KakaoOAuthApi.java**도 동일한 패턴으로 구현 (Provider별 응답 구조에 맞게 수정)

##### Step 4: Config 클래스 작성

**파일 위치**: `client/feign/src/main/java/.../domain/oauth/config/OAuthFeignConfig.java`

```java
package com.tech.n.ai.client.feign.domain.oauth.config;

import com.tech.n.ai.client.feign.domain.oauth.api.GoogleOAuthApi;
import com.tech.n.ai.client.feign.domain.oauth.api.KakaoOAuthApi;
import com.tech.n.ai.client.feign.domain.oauth.api.NaverOAuthApi;
import com.tech.n.ai.client.feign.domain.oauth.client.GoogleOAuthFeignClient;
import com.tech.n.ai.client.feign.domain.oauth.client.KakaoOAuthFeignClient;
import com.tech.n.ai.client.feign.domain.oauth.client.NaverOAuthFeignClient;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import com.tech.n.ai.client.feign.domain.oauth.mock.GoogleOAuthMock;
import com.tech.n.ai.client.feign.domain.oauth.mock.KakaoOAuthMock;
import com.tech.n.ai.client.feign.domain.oauth.mock.NaverOAuthMock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(clients = {
    GoogleOAuthFeignClient.class,
    NaverOAuthFeignClient.class,
    KakaoOAuthFeignClient.class
})
@Import({
    com.tech.n.ai.client.feign.config.OpenFeignConfig.class
})
@Configuration
public class OAuthFeignConfig {
    
    private static final String CLIENT_MODE = "feign-clients.oauth.mode";
    
    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
    public OAuthProviderContract googleOAuthMock() {
        return new GoogleOAuthMock();
    }
    
    @Bean
    @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
    public OAuthProviderContract googleOAuthApi(GoogleOAuthFeignClient feignClient) {
        return new GoogleOAuthApi(feignClient);
    }
    
    // Naver, Kakao도 동일한 패턴으로 Bean 등록
}
```

##### Step 5: 설정 파일 작성

**파일 위치**: `client/feign/src/main/resources/application-feign-oauth.yml`

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
  config.activate.on-profile: test
feign-clients:
  oauth:
    mode: mock

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

feign-clients:
  oauth:
    mode: rest
    google:
      uri: ${GOOGLE_OAUTH_URI:https://oauth2.googleapis.com}
    naver:
      uri: ${NAVER_OAUTH_URI:https://nid.naver.com}
    kakao:
      uri: ${KAKAO_OAUTH_URI:https://kauth.kakao.com}
```

### 2.2 api/auth 모듈 수정

#### 수정 항목

##### 1. 의존성 추가

**파일 위치**: `api/auth/build.gradle`

```gradle
dependencies {
    // 기존 의존성...
    implementation project(':client-feign')  // 추가
    // ...
}
```

##### 2. OAuth Provider 인터페이스 수정

**파일 위치**: `api/auth/src/main/java/.../oauth/OAuthProvider.java`

```java
package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;

/**
 * OAuth Provider 공통 인터페이스
 * 
 * OpenFeign Contract 인터페이스를 래핑하여 사용
 */
public interface OAuthProvider {
    
    /**
     * OAuth 인증 URL 생성
     * 
     * @param clientId Client ID
     * @param redirectUri Redirect URI
     * @param state State 파라미터 (CSRF 방지)
     * @return OAuth 인증 URL
     */
    String generateAuthorizationUrl(String clientId, String redirectUri, String state);
    
    /**
     * Authorization Code로 Access Token 교환
     * 
     * @param code Authorization Code
     * @param clientId Client ID
     * @param clientSecret Client Secret
     * @param redirectUri Redirect URI
     * @return Access Token
     */
    String exchangeAccessToken(String code, String clientId, String clientSecret, String redirectUri);
    
    /**
     * Access Token으로 사용자 정보 조회
     * 
     * @param accessToken Access Token
     * @return 사용자 정보
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
```

##### 3. OAuth Provider 구현체 수정

**파일 위치**: `api/auth/src/main/java/.../oauth/GoogleOAuthProvider.java`

```java
package com.tech.n.ai.api.auth.oauth;

import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthTokenRequest;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthDto.OAuthUserInfo;
import com.tech.n.ai.client.feign.domain.oauth.contract.OAuthProviderContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

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

##### 4. AuthService 수정

**파일 위치**: `api/auth/src/main/java/.../service/AuthService.java`

기존 코드는 변경 없음. `OAuthProvider` 인터페이스를 통해 OpenFeign Contract를 사용하므로 `AuthService`는 수정 불필요.

---

## 3. 문서 수정 가이드

### 3.1 docs/oauth-provider-implementation-guide.md 수정

#### 수정 섹션 1: HTTP Client 선택 (5.2 섹션)

**기존 내용**:
```markdown
### 5.2 의존성 추가

**build.gradle (api/auth 모듈):**

```gradle
dependencies {
    // 기존 의존성...
    
    // Spring WebClient (HTTP Client) - 비동기, 논블로킹
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    
    // Jackson (JSON 파싱) - 이미 포함되어 있을 수 있음
    // implementation 'com.fasterxml.jackson.core:jackson-databind'
}
```

**HTTP Client 선택:**

- **Spring WebClient**: 비동기, 논블로킹 (권장)
  - Spring 5.0+에서 권장하는 HTTP Client
  - Reactive Streams 지원
  - 더 나은 성능 및 확장성
```

**수정 내용**:
```markdown
### 5.2 의존성 추가

**build.gradle (api/auth 모듈):**

```gradle
dependencies {
    // 기존 의존성...
    
    // OpenFeign 클라이언트 모듈
    implementation project(':client-feign')
}
```

**HTTP Client 선택:**

- **OpenFeign 클라이언트**: 외부 API 연동 전용 (권장)
  - 프로젝트의 `client/feign` 모듈 사용
  - Contract Pattern으로 일관된 구조 유지
  - Mock/Real 구현체 선택 가능
  - Profile별 설정 분리
  - 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치
```

#### 수정 섹션 2: 구현 단계별 가이드 (5.3 섹션)

**기존 내용**: RestTemplate/WebClient 기반 구현 가이드

**수정 내용**: OpenFeign 클라이언트 기반 구현 가이드로 전체 교체

1. **Step 1**: OAuth Provider Contract 정의 (client/feign 모듈)
2. **Step 2**: OAuth Provider별 FeignClient 정의 (client/feign 모듈)
3. **Step 3**: OAuth Provider별 Api 구현체 작성 (client/feign 모듈)
4. **Step 4**: OAuth Provider Config 클래스 작성 (client/feign 모듈)
5. **Step 5**: 설정 파일 작성 (client/feign 모듈)
6. **Step 6**: api/auth 모듈 의존성 추가
7. **Step 7**: OAuth Provider 인터페이스 수정 (api/auth 모듈)
8. **Step 8**: OAuth Provider 구현체 수정 (api/auth 모듈)

#### 수정 섹션 3: 코드 예시

**기존 내용**: RestTemplate/WebClient 코드 예시

**수정 내용**: OpenFeign 클라이언트 코드 예시로 교체 (위의 구현 가이드 참조)

### 3.2 docs/oauth-http-client-selection-analysis.md 수정

#### 수정 섹션 1: HTTP Client 비교 분석

**추가 항목**: OpenFeign 비교 항목 추가

| 항목 | WebClient | RestTemplate | OpenFeign | 프로젝트 특성과의 일치성 |
|------|----------|-------------|-----------|----------------------|
| **프로그래밍 모델** | Reactive | Imperative | Imperative | OpenFeign ✅ 일치 |
| **의존성 추가** | WebFlux 필요 | WebMVC 기본 | client-feign 모듈 | OpenFeign ✅ 적합 |
| **아키텍처 일관성** | 혼합 아키텍처 | 일관된 아키텍처 | 일관된 아키텍처 | OpenFeign ✅ 일치 |
| **외부 API 연동** | 가능 | 가능 | 전용 모듈 | OpenFeign ✅ 적합 |
| **Contract Pattern** | 미지원 | 미지원 | 지원 | OpenFeign ✅ 일치 |

#### 수정 섹션 2: 최종 권장 선택

**기존 내용**: RestTemplate 권장

**수정 내용**: OpenFeign 권장으로 변경

**선택된 HTTP Client**: **OpenFeign (client/feign 모듈)**

**선택 근거**:
1. **아키텍처 일관성**: 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치
2. **Contract Pattern**: 기존 구현 패턴과 일치
3. **모듈 의존성**: `api/auth` → `client/feign` 의존 가능 (의존성 방향 준수)
4. **통합 용이성**: 기존 구현 패턴과 일치, 트랜잭션 처리와 호환

---

## 4. 주의사항 및 제약사항

### 4.1 구현 시 주의사항

1. **Provider별 FeignClient 분리**:
   - 각 Provider별로 별도의 FeignClient 인터페이스 정의
   - Base URL이 다르므로 분리 필요

2. **Form Data 전송**:
   - `@RequestBody MultiValueMap<String, String>` 사용
   - `consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE` 설정 필수

3. **Bearer Token 전달**:
   - `@RequestHeader("Authorization")` 사용
   - "Bearer " 접두사 포함하여 전달

4. **에러 처리**:
   - FeignClient 호출 시 `FeignException` 처리
   - Provider별 응답 구조에 맞는 에러 처리

5. **설정 파일 관리**:
   - Profile별 설정 분리 (`application-feign-oauth.yml`)
   - 환경 변수 사용 권장

### 4.2 제약사항

1. **동적 URL 처리**:
   - Provider별 Base URL이 다르므로 각각의 FeignClient 필요
   - 단일 FeignClient로 통합 불가

2. **인증 URL 생성**:
   - 인증 URL 생성은 FeignClient 사용 불필요 (단순 URL 생성)
   - 기존 방식 유지 (UriComponentsBuilder 사용)

3. **Mock 구현**:
   - 테스트 환경에서 Mock 구현체 사용 가능
   - `@ConditionalOnProperty`로 선택

---

## 검증 기준

제시된 분석은 다음 기준을 만족합니다:

1. ✅ **프로젝트 구조 준수**: 모듈 의존성 방향 및 순환 의존성 금지 원칙 준수
2. ✅ **공식 문서 기반**: Spring Cloud OpenFeign 공식 문서 참고
3. ✅ **아키텍처 일관성**: Contract Pattern 및 기존 구현 패턴 준수
4. ✅ **실용성**: OAuth 구현 요구사항에 적합한 설계
5. ✅ **간결성**: 오버엔지니어링 없이 핵심만 제시
6. ✅ **문서 일관성**: 관련 문서 간 일관성 유지

---

**작성일**: 2025-01-27  
**버전**: 1.0  
**대상**: OAuth Provider 구현을 OpenFeign 클라이언트로 전환 검토 및 개선  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 개선
