# Common Security Module

Spring Security 기반 JWT 인증 및 OAuth 인증 기능을 제공하는 모듈입니다.

## 개요

`common-security` 모듈은 Spring Security 6.x 기반의 JWT 토큰 인증 방식을 사용하는 REST API 서버를 위한 보안 기능을 제공합니다. Stateless 인증 방식을 채택하여 서버 측 세션을 유지하지 않고, JWT 토큰을 통해 사용자 인증 및 인가를 처리합니다.

## 주요 기능

### 1. JWT 토큰 관리

JWT 토큰 생성, 검증, 파싱 기능을 제공합니다.

- **JwtTokenProvider**: JWT 토큰 생성/검증/파싱 제공자
- **JwtTokenPayload**: JWT 토큰 페이로드 레코드
- **Access Token 및 Refresh Token**: 짧은 유효기간의 Access Token과 긴 유효기간의 Refresh Token 지원

**참고 설계서**: `docs/step6/spring-security-auth-design-guide.md`

### 2. JWT 인증 필터

JWT 토큰 기반 인증 필터를 제공합니다.

- **JwtAuthenticationFilter**: JWT 토큰 추출 및 검증 필터
- **SecurityContext 설정**: 인증 정보를 SecurityContext에 저장
- **OncePerRequestFilter**: 한 요청당 한 번만 실행되도록 보장

### 3. Spring Security 설정

Spring Security 필터 체인 및 보안 설정을 제공합니다.

- **SecurityConfig**: Spring Security 필터 체인 설정
- **PasswordEncoderConfig**: BCryptPasswordEncoder 설정
- **CORS 설정**: Cross-Origin Resource Sharing 설정

### 4. OAuth 지원

OAuth 2.0 인증을 위한 기본 설정을 제공합니다.

- **OAuth2 Authorization Server**: OAuth2 인증 서버 지원
- **OAuth2 Client**: OAuth2 클라이언트 지원
- **OAuth2 Resource Server**: OAuth2 리소스 서버 지원

## 주요 컴포넌트

### JWT 토큰 관리

#### JwtTokenProvider

JWT 토큰 생성, 검증, 파싱을 제공하는 컴포넌트입니다.

```java
@Component
public class JwtTokenProvider {
    
    public String generateAccessToken(JwtTokenPayload payload) {
        // Access Token 생성
    }
    
    public String generateRefreshToken(JwtTokenPayload payload) {
        // Refresh Token 생성
    }
    
    public JwtTokenPayload getPayloadFromToken(String token) {
        // 토큰에서 페이로드 추출
    }
    
    public boolean validateToken(String token) {
        // 토큰 검증
    }
}
```

**사용 예시**:
```java
@Autowired
private JwtTokenProvider jwtTokenProvider;

// 토큰 생성
JwtTokenPayload payload = new JwtTokenPayload(
    String.valueOf(user.getId()),
    user.getEmail(),
    "USER"
);
String accessToken = jwtTokenProvider.generateAccessToken(payload);
String refreshToken = jwtTokenProvider.generateRefreshToken(payload);

// 토큰 검증
if (jwtTokenProvider.validateToken(token)) {
    JwtTokenPayload payload = jwtTokenProvider.getPayloadFromToken(token);
    // 인증 정보 사용
}
```

**주요 기능**:
- HMAC-SHA 알고리즘을 사용한 토큰 서명
- Access Token: 짧은 유효기간 (기본 60분)
- Refresh Token: 긴 유효기간 (기본 7일)
- 토큰 검증: 서명 및 만료 시간 확인

#### JwtTokenPayload

JWT 토큰 페이로드를 담는 레코드입니다.

```java
public record JwtTokenPayload(
    String userId,
    String email,
    String role
) {}
```

### JWT 인증 필터

#### JwtAuthenticationFilter

JWT 토큰 기반 인증 필터입니다.

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        // 토큰 추출 및 검증
        // SecurityContext 설정
    }
}
```

**주요 기능**:
- `Authorization` 헤더에서 `Bearer <token>` 형식으로 토큰 추출
- 토큰 검증: `JwtTokenProvider.validateToken()`으로 토큰 유효성 검증
- 페이로드 추출: 검증 성공 시 토큰에서 사용자 정보 추출
- SecurityContext 설정: 인증 정보를 `SecurityContext`에 저장
- 인증 실패 처리: 유효하지 않은 토큰인 경우 401 Unauthorized 응답

### Spring Security 설정

#### SecurityConfig

Spring Security 필터 체인 및 보안 설정입니다.

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS 설정
    }
}
```

**주요 설정**:
- **CSRF 비활성화**: REST API는 Stateless이므로 CSRF 보호 불필요
- **Session 정책 (STATELESS)**: 세션을 생성하지 않음 (JWT 토큰 사용)
- **인증 규칙 설정**:
  - `/api/v1/auth/**`: 인증 없이 접근 가능 (회원가입, 로그인 등)
  - `/actuator/health`: 헬스 체크 엔드포인트 허용
  - 그 외 모든 요청: 인증 필요
- **커스텀 필터 추가**: `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 추가
- **CORS 설정**: Cross-Origin Resource Sharing 설정

#### PasswordEncoderConfig

BCryptPasswordEncoder 설정입니다.

```java
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

**주요 설정**:
- **BCryptPasswordEncoder**: 비밀번호 해싱을 위한 안전한 알고리즘
- **Salt rounds: 12**: Spring Security 공식 문서에서 권장하는 값
- **자동 Salt 생성**: 매번 다른 Salt를 자동 생성하여 레인보우 테이블 공격 방지

## 의존성

### 주요 의존성

- **common-core**: 공통 핵심 모듈 (BaseException, ApiResponse 등)
- **Spring Boot Starter Security**: Spring Security 기본 기능
- **Spring Boot Starter Security OAuth2 Authorization Server**: OAuth2 인증 서버 지원
- **Spring Boot Starter Security OAuth2 Client**: OAuth2 클라이언트 지원
- **Spring Boot Starter Security OAuth2 Resource Server**: OAuth2 리소스 서버 지원
- **jjwt**: JWT 토큰 생성/검증 라이브러리 (io.jsonwebtoken:jjwt-api:0.12.5)
- **Jackson**: JSON 처리 라이브러리

## 사용 방법

### 1. 의존성 추가

다른 모듈에서 `common-security` 모듈을 사용하려면 `build.gradle`에 다음을 추가합니다:

```gradle
dependencies {
    implementation project(':common-security')
}
```

### 2. JWT 토큰 생성

로그인 시 JWT 토큰을 생성합니다:

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    public TokenResponse login(LoginRequest request) {
        // 사용자 인증 로직
        User user = authenticate(request);
        
        // JWT 토큰 생성
        JwtTokenPayload payload = new JwtTokenPayload(
            String.valueOf(user.getId()),
            user.getEmail(),
            "USER"
        );
        String accessToken = jwtTokenProvider.generateAccessToken(payload);
        String refreshToken = jwtTokenProvider.generateRefreshToken(payload);
        
        return new TokenResponse(accessToken, refreshToken, "Bearer", 3600L, 604800L);
    }
}
```

### 3. 인증된 요청 처리

컨트롤러에서 인증된 사용자 정보를 사용합니다:

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName(); // JWT 토큰의 subject (userId)
        
        // 권한 정보 추출
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // 사용자 정보 조회
        User user = userService.findById(Long.parseLong(userId));
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
```

### 4. 권한 기반 접근 제어

`SecurityConfig`에서 경로별 인증 규칙을 설정합니다:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/user/**").hasAnyRole("USER", "ADMIN")
            .anyRequest().authenticated()
        );
    return http.build();
}
```

또는 `@PreAuthorize` 어노테이션을 사용합니다:

```java
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        // 관리자만 접근 가능
    }
}
```

**주의사항**: `@PreAuthorize`를 사용하려면 `SecurityConfig`에 `@EnableMethodSecurity` 어노테이션을 추가해야 합니다.

## 설정

### application.yml

JWT 및 보안 설정을 추가합니다:

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY:default-secret-key-change-in-production-minimum-256-bits}
  access-token-validity-minutes: ${JWT_ACCESS_TOKEN_VALIDITY_MINUTES:60}
  refresh-token-validity-days: ${JWT_REFRESH_TOKEN_VALIDITY_DAYS:7}
```

**환경변수**:
- `JWT_SECRET_KEY`: JWT 서명에 사용할 Secret Key (최소 256비트 권장)
- `JWT_ACCESS_TOKEN_VALIDITY_MINUTES`: Access Token 유효기간 (분)
- `JWT_REFRESH_TOKEN_VALIDITY_DAYS`: Refresh Token 유효기간 (일)

## 보안 고려사항

### 1. 토큰 보안 관리

- **Secret Key 관리**: 운영 환경에서는 반드시 강력한 Secret Key 사용 (최소 256비트)
- **환경 변수 사용**: Secret Key는 환경 변수나 시크릿 관리 시스템 사용
- **코드에 하드코딩 금지**: Secret Key는 절대 코드에 하드코딩하지 않음

### 2. 토큰 만료 시간 설정

- **Access Token**: 짧은 유효기간 (1시간 이내 권장)
- **Refresh Token**: 긴 유효기간 (7일 이내 권장)
- **토큰 갱신**: 토큰 갱신 시 기존 토큰 무효화

### 3. HTTPS 사용

- **프로덕션 환경**: 반드시 HTTPS 사용
- **토큰 암호화**: 토큰이 네트워크를 통해 전송될 때 암호화

### 4. CORS 보안

- **운영 환경**: 특정 도메인만 허용
- **allowCredentials**: `allowCredentials(true)` 사용 시 특정 도메인 지정 필수
- **개발 환경**: 개발 환경에서만 `allowedOrigins("*")` 사용

### 5. 비밀번호 보안

- **BCryptPasswordEncoder**: Salt rounds 10-12 권장 (현재 12 사용)
- **평문 저장 금지**: 비밀번호는 절대 평문으로 저장하지 않음

## 참고 자료

### 설계서

- `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 및 사용자 인증/인가 로직 구현 활용방법

### 공식 문서

- [Spring Security 공식 문서](https://docs.spring.io/spring-security/reference/)
- [Spring Security Servlet Configuration](https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
- [Spring Security Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)
- [Spring Security Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
- [JWT 공식 스펙 (RFC 7519)](https://tools.ietf.org/html/rfc7519)
- [jjwt 라이브러리 공식 문서](https://github.com/jwtk/jjwt)

---

**작성일**: 2026-01-XX  
**버전**: 0.0.1-SNAPSHOT

