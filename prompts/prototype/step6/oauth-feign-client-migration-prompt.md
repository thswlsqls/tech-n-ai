# OAuth Provider 구현을 OpenFeign 클라이언트로 전환 검토 및 개선 프롬프트

## 연구 목표

OAuth Provider별 로그인 기능 구현에서 OAuth 인증서버로의 API 요청을 `client/feign` 모듈의 OpenFeign 클라이언트로 처리하도록 설계가 가능한지 검토하고, 가능하다면 `docs/oauth-provider-implementation-guide.md` 문서 및 관련 설계 문서의 모든 관련 항목을 수정하기 위한 가이드를 작성합니다.

## 프로젝트 컨텍스트 분석

### 1단계: 프로젝트 구조 및 모듈 의존성 분석

다음 정보를 수집하여 프로젝트의 모듈 구조와 의존성 관계를 파악하세요:

#### 모듈 구조 분석
- **모듈 의존성 방향**: `API → Domain → Common → Client`
  - `shrimp-rules.md`에서 확인
  - 순환 의존성 금지 원칙 확인
- **client/feign 모듈 구조**:
  - Contract Pattern 사용 여부 확인
  - 기존 Feign Client 구현 패턴 확인
  - 설정 파일 구조 확인

#### 모듈별 의존성 확인
- **api/auth 모듈**:
  - `build.gradle` 확인
  - 현재 의존하는 모듈 목록
  - `client/feign` 모듈 의존 가능 여부 확인
- **client/feign 모듈**:
  - `build.gradle` 확인
  - 의존하는 모듈 목록
  - `api/auth` 모듈과의 순환 의존성 가능 여부 확인

### 2단계: OpenFeign 클라이언트 구조 분석

#### Contract Pattern 구조 파악
다음 파일들을 분석하여 OpenFeign 클라이언트 구조를 파악하세요:

1. **Contract 인터페이스**:
   - `client/feign/src/main/java/.../contract/{Domain}Contract.java`
   - 비즈니스 메서드 시그니처 정의 방식
   - DTO 클래스 정의 방식

2. **FeignClient 인터페이스**:
   - `client/feign/src/main/java/.../client/{Domain}FeignClient.java`
   - `@FeignClient` 어노테이션 사용 방식
   - URL 설정 방식 (`${feign-clients.{domain}.uri}`)

3. **Api 구현체**:
   - `client/feign/src/main/java/.../api/{Domain}Api.java`
   - Contract 인터페이스 구현 방식
   - FeignClient 사용 방식

4. **Config 클래스**:
   - `client/feign/src/main/java/.../config/{Domain}FeignConfig.java`
   - `@EnableFeignClients` 설정 방식
   - Mock/Real 구현체 선택 방식

5. **설정 파일**:
   - `client/feign/src/main/resources/application-feign-{domain}.yml`
   - Profile별 설정 구조
   - 타임아웃 및 연결 설정

### 3단계: OAuth Provider API 요청 분석

#### OAuth Provider별 API 요청 특성
다음 OAuth Provider의 API 요청 특성을 분석하세요:

1. **Google OAuth 2.0**:
   - Token Endpoint: `https://oauth2.googleapis.com/token` (POST)
   - UserInfo Endpoint: `https://www.googleapis.com/oauth2/v2/userinfo` (GET)
   - 요청 형식: `application/x-www-form-urlencoded` (Token), `Bearer Token` (UserInfo)

2. **Naver OAuth 2.0**:
   - Token Endpoint: `https://nid.naver.com/oauth2.0/token` (GET/POST)
   - UserInfo Endpoint: `https://openapi.naver.com/v1/nid/me` (GET)
   - 요청 형식: `application/x-www-form-urlencoded` (Token), `Bearer Token` (UserInfo)

3. **Kakao OAuth 2.0**:
   - Token Endpoint: `https://kauth.kakao.com/oauth/token` (POST)
   - UserInfo Endpoint: `https://kapi.kakao.com/v2/user/me` (GET)
   - 요청 형식: `application/x-www-form-urlencoded` (Token), `Bearer Token` (UserInfo)

#### OpenFeign 호환성 검토
- **Content-Type**: `application/x-www-form-urlencoded` 지원 여부
- **GET 요청 파라미터**: Query Parameter 전달 방식
- **POST 요청 Body**: Form Data 전달 방식
- **Authorization Header**: Bearer Token 전달 방식
- **동적 URL**: Provider별 다른 Base URL 처리 방식

### 4단계: 현재 OAuth 구현 설계 분석

#### 현재 설계 구조
다음 문서들을 분석하여 현재 OAuth 구현 설계를 파악하세요:

1. **`docs/oauth-provider-implementation-guide.md`**:
   - OAuth Provider 인터페이스 설계
   - HTTP Client 선택 (RestTemplate 권장)
   - 구현 단계별 가이드

2. **`docs/oauth-http-client-selection-analysis.md`**:
   - HTTP Client 선택 근거 (RestTemplate)
   - 프로젝트 아키텍처 특성
   - 통합 용이성 분석

3. **`api/auth` 모듈 구현 코드**:
   - `AuthService.handleOAuthCallback()` 메서드
   - OAuth Provider 인터페이스 사용 방식
   - 트랜잭션 처리 방식

## 검토 질문

### 1. 모듈 의존성 검토

**api/auth 모듈이 client/feign 모듈을 의존할 수 있는가?**
- 모듈 의존성 방향: `API → Domain → Common → Client`
- `api/auth`는 `API` 계층, `client/feign`은 `Client` 계층
- **결론**: ✅ 의존 가능 (의존성 방향 준수)

**순환 의존성 발생 가능 여부**:
- `client/feign` 모듈이 `api/auth` 모듈을 의존하는가?
- `client/feign/build.gradle` 확인 필요
- **예상**: ❌ 순환 의존성 없음 (Client 모듈은 API 모듈을 의존하지 않음)

### 2. OpenFeign 호환성 검토

**OAuth Provider API 요청을 OpenFeign으로 처리 가능한가?**

#### Token 교환 요청 (POST, application/x-www-form-urlencoded)
- **OpenFeign 지원 여부**: ✅ 지원
  - `@RequestParam` 또는 `@RequestBody` 사용
  - `consumes = MediaType.APPLICATION_FORM_URLENCODED` 설정 가능
- **구현 방식**:
  - `MultiValueMap<String, String>` 사용
  - 또는 `@RequestParam`으로 개별 파라미터 전달

#### 사용자 정보 조회 요청 (GET, Bearer Token)
- **OpenFeign 지원 여부**: ✅ 지원
  - `@RequestHeader("Authorization")` 사용
  - 또는 `@RequestHeader` 어노테이션으로 Bearer Token 전달
- **구현 방식**:
  - `@RequestHeader("Authorization") String authorization` 파라미터 사용
  - 또는 Feign RequestInterceptor 사용

#### 동적 URL 처리
- **Provider별 다른 Base URL**: ✅ 지원
  - `@FeignClient(url = "${feign-clients.oauth.{provider}.uri}")` 사용
  - 또는 `@FeignClient(name = "...", url = "...")` 사용

### 3. 아키텍처 일관성 검토

**OpenFeign 사용이 프로젝트 아키텍처와 일치하는가?**
- ✅ **일치**: 
  - `client/feign` 모듈은 외부 API 연동 전용 모듈
  - OAuth Provider API는 외부 API이므로 `client/feign` 모듈 사용 적합
  - Contract Pattern 사용으로 일관된 구조 유지

**기존 구현 패턴과의 일치성**:
- ✅ **일치**:
  - Contract Pattern 사용
  - Mock/Real 구현체 선택 가능
  - Profile별 설정 분리

### 4. 통합 용이성 검토

**api/auth 모듈과의 통합 용이성**:
- ✅ **통합 용이**:
  - `api/auth` 모듈에서 `client/feign` 모듈의 Contract 인터페이스 주입
  - `@Transactional` 메서드 내에서 동기적으로 호출 가능
  - 기존 예외 처리 패턴과 일치

**트랜잭션 처리와의 호환성**:
- ✅ **호환**:
  - OpenFeign은 동기/블로킹 방식
  - `@Transactional` 메서드 내에서 자연스럽게 사용 가능

## 구현 가능성 결론

### 결론: ✅ 구현 가능

**이유**:
1. **모듈 의존성**: `api/auth` → `client/feign` 의존 가능 (의존성 방향 준수)
2. **OpenFeign 호환성**: OAuth Provider API 요청 형식 모두 지원
3. **아키텍처 일관성**: 외부 API 연동은 `client/feign` 모듈 사용이 적합
4. **통합 용이성**: 기존 구현 패턴과 일치, 트랜잭션 처리와 호환

## 수정 범위

### 1. 코드 구현

#### client/feign 모듈
1. **OAuth Provider Contract 정의**:
   - `client/feign/src/main/java/.../domain/oauth/contract/OAuthProviderContract.java`
   - `client/feign/src/main/java/.../domain/oauth/contract/OAuthDto.java`

2. **OAuth Provider별 FeignClient 정의**:
   - `client/feign/src/main/java/.../domain/oauth/client/GoogleOAuthFeignClient.java`
   - `client/feign/src/main/java/.../domain/oauth/client/NaverOAuthFeignClient.java`
   - `client/feign/src/main/java/.../domain/oauth/client/KakaoOAuthFeignClient.java`

3. **OAuth Provider별 Api 구현체**:
   - `client/feign/src/main/java/.../domain/oauth/api/GoogleOAuthApi.java`
   - `client/feign/src/main/java/.../domain/oauth/api/NaverOAuthApi.java`
   - `client/feign/src/main/java/.../domain/oauth/api/KakaoOAuthApi.java`

4. **OAuth Provider Mock 구현체**:
   - `client/feign/src/main/java/.../domain/oauth/mock/GoogleOAuthMock.java`
   - `client/feign/src/main/java/.../domain/oauth/mock/NaverOAuthMock.java`
   - `client/feign/src/main/java/.../domain/oauth/mock/KakaoOAuthMock.java`

5. **OAuth Provider Config 클래스**:
   - `client/feign/src/main/java/.../domain/oauth/config/OAuthFeignConfig.java`

6. **설정 파일**:
   - `client/feign/src/main/resources/application-feign-oauth.yml`

#### api/auth 모듈
1. **의존성 추가**:
   - `api/auth/build.gradle`에 `client-feign` 모듈 의존성 추가

2. **OAuth Provider 인터페이스 수정**:
   - `api/auth/src/main/java/.../oauth/OAuthProvider.java` 수정
   - OpenFeign Contract 인터페이스 사용하도록 변경

3. **OAuth Provider 구현체 수정**:
   - `api/auth/src/main/java/.../oauth/GoogleOAuthProvider.java` 수정
   - `api/auth/src/main/java/.../oauth/NaverOAuthProvider.java` 수정
   - `api/auth/src/main/java/.../oauth/KakaoOAuthProvider.java` 수정
   - OpenFeign Contract 인터페이스 주입하여 사용

4. **AuthService 수정**:
   - OpenFeign Contract 인터페이스 사용하도록 수정

### 2. 문서 수정

#### docs/oauth-provider-implementation-guide.md
1. **HTTP Client 선택 섹션 수정**:
   - RestTemplate 대신 OpenFeign 사용으로 변경
   - OpenFeign 선택 근거 추가

2. **의존성 추가 섹션 수정**:
   - `spring-boot-starter-webflux` 제거
   - `client-feign` 모듈 의존성 추가

3. **구현 단계별 가이드 수정**:
   - OpenFeign 클라이언트 구현 단계 추가
   - Contract Pattern 사용 가이드 추가

4. **코드 예시 수정**:
   - RestTemplate 코드 예시 제거
   - OpenFeign 클라이언트 코드 예시 추가

#### docs/oauth-http-client-selection-analysis.md
1. **HTTP Client 비교 분석 수정**:
   - OpenFeign 추가 비교 항목
   - OpenFeign 선택 근거 추가

2. **최종 권장 선택 수정**:
   - RestTemplate → OpenFeign 변경
   - 선택 근거 업데이트

#### 기타 관련 문서
1. **shrimp-rules.md** (필요 시):
   - OAuth Provider API 연동 패턴 추가

2. **docs/phase2/1. api-endpoint-design.md** (필요 시):
   - OAuth 엔드포인트 설계 업데이트

## 구현 가이드 (요약)

### OpenFeign 클라이언트 구현 패턴

#### 1. Contract 인터페이스 정의
```java
public interface OAuthProviderContract {
    OAuthUserInfo getUserInfo(String provider, String accessToken);
    String exchangeAccessToken(String provider, OAuthTokenRequest request);
}
```

#### 2. FeignClient 인터페이스 정의
```java
@FeignClient(name = "GoogleOAuth", url = "${feign-clients.oauth.google.uri}")
public interface GoogleOAuthFeignClient {
    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED)
    GoogleTokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> params);
    
    @GetMapping(value = "/oauth2/v2/userinfo")
    GoogleUserInfoResponse getUserInfo(@RequestHeader("Authorization") String authorization);
}
```

#### 3. Api 구현체
```java
@RequiredArgsConstructor
public class GoogleOAuthApi implements OAuthProviderContract {
    private final GoogleOAuthFeignClient feignClient;
    
    @Override
    public OAuthUserInfo getUserInfo(String provider, String accessToken) {
        // FeignClient 호출 및 응답 변환
    }
}
```

## 참고 자료 (공식 문서만 사용)

### 필수 참고 문서
1. **Spring Cloud OpenFeign 공식 문서**
   - Spring Cloud OpenFeign Reference: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - Feign Client Configuration: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#feign-client-configuration
   - Form Data 전송: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/#feign-requestbody-parameters

2. **OAuth 2.0 Provider 공식 문서**
   - Google OAuth 2.0: https://developers.google.com/identity/protocols/oauth2/web-server
   - Naver OAuth 2.0: https://developers.naver.com/docs/login/api/
   - Kakao OAuth 2.0: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api

3. **프로젝트 설계 문서**
   - `shrimp-rules.md`: 모듈 구조 및 의존성 규칙
   - `docs/oauth-provider-implementation-guide.md`: 현재 OAuth 구현 설계
   - `docs/oauth-http-client-selection-analysis.md`: HTTP Client 선택 분석

### 참고 금지
- 블로그 포스트, 개인 의견, 비공식 튜토리얼
- 공식 문서에 근거하지 않은 방법론
- 오래된 버전의 문서

## 출력 형식

다음 형식으로 결과를 제시하세요:

### 1. 구현 가능성 검토 결과

**결론**: [구현 가능/불가능]

**검토 항목**:
1. **모듈 의존성**: [의존 가능 여부 및 근거]
2. **OpenFeign 호환성**: [호환성 여부 및 근거]
3. **아키텍처 일관성**: [일치 여부 및 근거]
4. **통합 용이성**: [통합 난이도 및 근거]

### 2. 구현 가이드 (구현 가능한 경우)

#### 2.1 client/feign 모듈 구현

**파일 구조**:
```
client/feign/src/main/java/.../domain/oauth/
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

**구현 단계**:
1. [단계 1: Contract 인터페이스 정의]
2. [단계 2: FeignClient 인터페이스 정의]
3. [단계 3: Api 구현체 작성]
4. [단계 4: Config 클래스 작성]
5. [단계 5: 설정 파일 작성]

#### 2.2 api/auth 모듈 수정

**수정 항목**:
1. [의존성 추가]
2. [OAuth Provider 인터페이스 수정]
3. [OAuth Provider 구현체 수정]
4. [AuthService 수정]

### 3. 문서 수정 가이드

#### 3.1 docs/oauth-provider-implementation-guide.md 수정

**수정 섹션**:
1. [섹션 1: HTTP Client 선택]
2. [섹션 2: 의존성 추가]
3. [섹션 3: 구현 단계별 가이드]
4. [섹션 4: 코드 예시]

**수정 내용**:
- [구체적인 수정 내용]

#### 3.2 docs/oauth-http-client-selection-analysis.md 수정

**수정 섹션**:
1. [섹션 1: HTTP Client 비교 분석]
2. [섹션 2: 최종 권장 선택]

**수정 내용**:
- [구체적인 수정 내용]

### 4. 주의사항 및 제약사항

**구현 시 주의사항**:
1. [주의사항 1]
2. [주의사항 2]
3. [주의사항 3]

**제약사항**:
1. [제약사항 1]
2. [제약사항 2]

## 검증 기준

제시된 분석은 다음 기준을 만족해야 합니다:

1. ✅ **프로젝트 구조 준수**: 모듈 의존성 방향 및 순환 의존성 금지 원칙 준수
2. ✅ **공식 문서 기반**: Spring Cloud OpenFeign 공식 문서만 참고
3. ✅ **아키텍처 일관성**: Contract Pattern 및 기존 구현 패턴 준수
4. ✅ **실용성**: OAuth 구현 요구사항에 적합한 설계
5. ✅ **간결성**: 오버엔지니어링 없이 핵심만 제시
6. ✅ **문서 일관성**: 관련 문서 간 일관성 유지

## 주의사항

- **프로젝트 구조 우선**: 모듈 의존성 방향 및 순환 의존성 금지 원칙 준수
- **공식 문서만 참고**: 추측이나 개인 의견 배제
- **아키텍처 일관성**: Contract Pattern 및 기존 구현 패턴 준수
- **실용성 우선**: OAuth 구현 요구사항에 적합한 실용적 설계
- **간결성**: 불필요한 복잡성 도입 지양
- **오버엔지니어링 방지**: 요구사항을 충족하는 가장 단순하고 일관된 방법 우선
- **문서 일관성**: 관련 문서 간 일관성 유지

---

**작성일**: 2025-01-27  
**프롬프트 버전**: 1.0  
**대상**: OAuth Provider 구현을 OpenFeign 클라이언트로 전환 검토 및 개선  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 개선
