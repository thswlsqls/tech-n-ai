# 관리자 권한 기반 인증/인가 시스템 설계서 작성 프롬프트

## 프롬프트 목적

이 프롬프트는 `api/auth` 모듈에 **역할(Role) 기반 접근 제어**를 구현하기 위한 **상세 설계서** 작성을 지시합니다. 설계서는 기존 인증/인가 시스템과의 정합성을 유지하며, SOLID 원칙과 클린코드 원칙을 준수해야 합니다.

---

## 설계서 작성 목표

1. **기존 `AdminEntity`(`admins` 테이블)를 활용한 관리자 계정 CRUD API 설계**
2. **관리자 로그인 엔드포인트 설계 (`TokenService` role 파라미터 추가)**
3. **api-gateway에서 역할 기반 라우팅 검증 추가 설계**
4. **chatbot/agent 모듈의 역할 기반 접근 제어 설계**
5. **일반 채팅과 AI Agent 작업 지시(`@agent` 프리픽스) 구분 메커니즘 설계**

---

## 필수 참고 문서

설계서 작성 시 반드시 다음 문서 및 코드를 참고하고, 정합성을 유지해야 합니다.

### 1. 핵심 설계 문서

| 문서 경로 | 참고 내용 |
|-----------|-----------|
| `docs/step6/spring-security-auth-design-guide.md` | Spring Security 인증/인가 아키텍처, JWT 토큰 관리 전략 |
| `docs/step1/3. aurora-schema-design.md` | Aurora MySQL 테이블 설계, Soft Delete 원칙 |

### 2. 기존 구현 코드 참고

#### 인증/인가 핵심

| 코드 경로 | 참고 내용 | 핵심 확인 사항 |
|-----------|-----------|----------------|
| `domain/aurora/.../entity/auth/AdminEntity.java` | `admins` 테이블 엔티티 | role(String, 50), isActive(Boolean), email, username, password, lastLoginAt 필드 |
| `domain/aurora/.../entity/auth/UserEntity.java` | `users` 테이블 엔티티 | role 필드 없음 (수정 불필요) |
| `domain/aurora/.../repository/reader/auth/AdminReaderRepository.java` | 관리자 조회 Repository | JpaRepository 상속, 조회 메서드 추가 필요 |
| `domain/aurora/.../repository/writer/auth/AdminWriterRepository.java` | 관리자 저장 Repository | BaseWriterRepository 상속, 히스토리 자동 추적 |
| `api/auth/.../service/TokenService.java` | JWT 토큰 생성 | `generateTokens(Long userId, String email)` - role을 `USER_ROLE`로 하드코딩, role 파라미터 추가 필요 |
| `api/auth/.../service/TokenConstants.java` | 토큰 관련 상수 | `USER_ROLE = "USER"` |
| `api/auth/.../service/UserAuthenticationService.java` | 일반 회원 로그인 | TokenService 호출부 수정 필요 |

#### 보안/필터

| 코드 경로 | 참고 내용 | 핵심 확인 사항 |
|-----------|-----------|----------------|
| `common/security/.../jwt/JwtTokenPayload.java` | JWT 페이로드 record | `userId`, `email`, `role` 필드 (수정 불필요) |
| `common/security/.../filter/JwtAuthenticationFilter.java` | Spring Security 필터 | `ROLE_` 접두사로 권한 부여 → `hasRole("ADMIN")` 사용 가능 |
| `common/security/.../principal/UserPrincipal.java` | 사용자 Principal record | `userId`, `email`, `role` (수정 불필요) |
| `common/security/.../config/SecurityConfig.java` | Spring Security 설정 | `/api/v1/auth/**` 전체 permitAll → admin 경로 분리 필요 |
| `api/gateway/.../filter/JwtAuthenticationGatewayFilter.java` | Gateway JWT 필터 | `x-user-id/email/role` 헤더 주입, `/api/v1/agent`를 공개 경로로 처리 중 (수정 필요) |

#### 예외 처리 (이미 존재)

| 코드 경로 | 참고 내용 | 핵심 확인 사항 |
|-----------|-----------|----------------|
| `common/exception/.../exception/ForbiddenException.java` | 403 예외 | `ErrorCodeConstants.FORBIDDEN("4003")` 사용 (새로 만들지 말 것) |
| `common/core/.../constants/ErrorCodeConstants.java` | 에러 코드 상수 | `FORBIDDEN = "4003"`, `MESSAGE_CODE_FORBIDDEN` 이미 정의됨 |
| `common/exception/.../handler/GlobalExceptionHandler.java` | 전역 예외 핸들러 | ForbiddenException 핸들러 이미 존재, **ConflictException은 400/4006 반환 (409 아님)** |

#### Chatbot/Agent

| 코드 경로 | 참고 내용 | 핵심 확인 사항 |
|-----------|-----------|----------------|
| `api/chatbot/.../service/dto/Intent.java` | Intent enum | LLM_DIRECT, RAG_REQUIRED, WEB_SEARCH_REQUIRED (AGENT_COMMAND 추가 필요) |
| `api/chatbot/.../service/IntentClassificationServiceImpl.java` | 의도 분류 서비스 | 키워드 기반 분류, `@agent` 프리픽스 감지 추가 필요 |
| `api/chatbot/.../service/ChatbotServiceImpl.java` | Chatbot 서비스 | AGENT_COMMAND case 추가 필요 |
| `api/agent/.../controller/AgentController.java` | Agent 컨트롤러 | 현재 `X-Internal-Api-Key` 인증 → JWT 역할 기반으로 변경 |

### 3. 기존 패턴 참고

| 코드 경로 | 참고 내용 |
|-----------|-----------|
| `api/auth/.../controller/AuthController.java` | Controller 패턴 (Controller → Facade → Service) |
| `api/auth/.../facade/AuthFacade.java` | Facade 패턴 |
| `api/auth/.../service/AuthService.java` | Service 패턴 |
| `api/bookmark/src/test/http/*.http` | IntelliJ HTTP Client 테스트 패턴 |

---

## 기존 시스템 현황 (필수 반영)

### 이미 존재하는 인프라 (새로 만들지 말 것)

| 컴포넌트 | 현황 |
|----------|------|
| `AdminEntity` | `admins` 테이블, role(String), isActive 필드 포함 |
| `AdminReaderRepository` | JpaRepository 상속 (조회 메서드만 추가 필요) |
| `AdminWriterRepository` | BaseWriterRepository 상속, 히스토리 자동 추적 |
| `AdminHistoryEntity` / `AdminHistoryEntityFactory` | 관리자 변경 이력 추적 |
| `JwtTokenPayload` | `userId`, `email`, `role` 필드 포함 |
| `JwtAuthenticationGatewayFilter` | `x-user-id`, `x-user-email`, `x-user-role` 헤더 주입 |
| `UserPrincipal` | `userId`, `email`, `role` 필드 포함 |
| `JwtAuthenticationFilter` | `ROLE_` 접두사로 Spring Security 권한 부여 |
| `ForbiddenException` | BaseException 상속, 403/4003 코드 |
| `ErrorCodeConstants.FORBIDDEN` / `MESSAGE_CODE_FORBIDDEN` | 상수 정의 완료 |
| `GlobalExceptionHandler` | ForbiddenException 핸들러 존재 |

### 초기 관리자 계정 (Bootstrap)

관리자 계정 생성 API는 ADMIN 역할의 JWT 토큰이 필요하므로, **최초 관리자**는 Flyway 마이그레이션 스크립트로 `admins` 테이블에 직접 삽입합니다.

- Flyway 마이그레이션: `V{version}__seed_initial_admin.sql`
- BCrypt 인코딩된 비밀번호 사용
- 운영 절차: 시드 관리자 로그인 → JWT 발급 → 추가 관리자 생성

> **참고**: 테스트에서 사용하는 `adminAccessToken`은 관리자 로그인(`POST /api/v1/auth/admin/login`) 후 발급받은 JWT accessToken을 의미합니다.

### 수정이 필요한 부분

| 컴포넌트 | 현황 | 변경 필요 |
|----------|------|-----------|
| `TokenService.generateTokens()` | role을 `USER_ROLE` 상수로 하드코딩 | role 파라미터 추가, 기존 호출부도 수정 |
| `JwtAuthenticationGatewayFilter.isPublicPath()` | `/api/v1/agent`를 공개 경로로 처리 | 관리자 전용으로 변경 |
| `SecurityConfig` | `/api/v1/auth/**` 전체 permitAll | `/api/v1/auth/admin/login` permitAll + `/api/v1/auth/admin/**` hasRole("ADMIN") |
| `AgentController` | `X-Internal-Api-Key` 인증 | JWT 역할 기반으로 변경 |
| `RefreshTokenEntity` / `RefreshTokenService` | `user_id` NOT NULL, UserEntity만 지원 | `admin_id` 컬럼 추가, `user_id` NULL 허용, 관리자 토큰 저장 지원 |

---

## 설계서에 포함되어야 할 섹션

### 1. 개요 (Overview)

다음 내용을 포함:
- 역할 기반 접근 제어(RBAC) 도입 목적 및 범위
- 기존 시스템 현황 (이미 존재하는 인프라 vs 수정 필요 부분)
- 주요 요구사항 요약
- 초기 관리자 계정 Bootstrap 전략 (Flyway 시드 + 관리자 로그인 → JWT 발급 → 추가 관리자 생성)

### 2. 관리자 계정 관리 API 설계

#### 2.1 엔드포인트 명세

| HTTP Method | URL | 설명 | 권한 |
|-------------|-----|------|------|
| POST | `/api/v1/auth/admin/login` | 관리자 로그인 | 공개 |
| POST | `/api/v1/auth/admin/accounts` | 관리자 계정 생성 | ADMIN |
| GET | `/api/v1/auth/admin/accounts` | 관리자 목록 조회 | ADMIN |
| GET | `/api/v1/auth/admin/accounts/{adminId}` | 관리자 상세 조회 | ADMIN |
| PUT | `/api/v1/auth/admin/accounts/{adminId}` | 관리자 정보 수정 | ADMIN |
| DELETE | `/api/v1/auth/admin/accounts/{adminId}` | 관리자 계정 삭제 (Soft Delete) | ADMIN |

#### 2.2 요청/응답 DTO 설계

- `AdminCreateRequest`: email, username, password (유효성 검증 포함)
- `AdminUpdateRequest`: username, password (선택적)
- `AdminResponse`: id, email, username, role, isActive, createdAt, lastLoginAt + `from(AdminEntity)` 팩토리 메서드

#### 2.3 계층 구조

기존 패턴을 따라 Controller → Facade → Service 구조:
- `AdminController`: `@AuthenticationPrincipal UserPrincipal` 사용 (x-user-role 헤더 직접 사용 금지)
- `AdminFacade`: 서비스 오케스트레이션
- `AdminService`: 비즈니스 로직

> **중요**: Gateway에서 `/api/v1/auth/admin` 경로에 대해 ADMIN 역할을 이미 검증하므로, Controller에서 별도 역할 검사를 중복하지 않습니다.

#### 2.4 비즈니스 로직

1. **관리자 생성**: 이메일/사용자명 중복 검사, 비밀번호 암호화 (BCrypt), role = "ADMIN"
2. **관리자 수정**: 사용자명 변경 시 중복 검사, 비밀번호 변경 시 암호화
3. **관리자 삭제**: Soft Delete (BaseWriterRepository.delete()), 자기 자신 삭제 방지
4. **관리자 로그인**: `admins` 테이블에서 조회, JWT 생성 시 role="ADMIN" 전달

#### 2.5 AdminReaderRepository 확장

기존 빈 Repository에 다음 메서드 추가:
- `findByEmail(String email)`: Optional
- `findByUsername(String username)`: Optional
- `findByIsActiveTrue()`: List
- `findByEmailAndIsActiveTrue(String email)`: Optional

### 3. 관리자 로그인 설계

#### 3.1 TokenService 수정

```java
// 현재 (role 하드코딩)
public TokenResponse generateTokens(Long userId, String email) {
    JwtTokenPayload payload = new JwtTokenPayload(String.valueOf(userId), email, USER_ROLE);
}

// 수정 (role 파라미터 추가)
public TokenResponse generateTokens(Long userId, String email, String role) {
    JwtTokenPayload payload = new JwtTokenPayload(String.valueOf(userId), email, role);
}
```

#### 3.2 기존 호출부 수정

`UserAuthenticationService`, `OAuthService` 등에서:
```java
tokenService.generateTokens(userId, email, TokenConstants.USER_ROLE);
```

#### 3.3 관리자 로그인

```java
// AdminService.login()
tokenService.generateTokens(admin.getId(), admin.getEmail(), admin.getRole());
```

#### 3.4 RefreshToken 관리자 지원

`refresh_tokens` 테이블에 `admin_id` 컬럼 추가, `user_id`를 NULL 허용으로 변경:

```sql
ALTER TABLE refresh_tokens
    MODIFY COLUMN user_id BIGINT UNSIGNED NULL COMMENT '사용자 ID (일반 회원)',
    ADD COLUMN admin_id BIGINT UNSIGNED NULL COMMENT '관리자 ID' AFTER user_id,
    ADD INDEX idx_refresh_token_admin_id (admin_id);
```

- `RefreshTokenEntity`: `AdminEntity` 관계 추가, `createForUser`/`createForAdmin` 팩토리 메서드
- `RefreshTokenService`: `saveAdminRefreshToken()` 메서드 추가
- `TokenService`: role 기반으로 `saveRefreshToken` / `saveAdminRefreshToken` 분기

### 4. API Gateway 역할 검증 설계

#### 4.1 isPublicPath 수정

- `/api/v1/auth/admin/login`: 공개 (관리자 로그인)
- `/api/v1/auth/admin/**`: 인증 필요 (공개에서 제외)
- `/api/v1/agent`: 공개 경로에서 제거

#### 4.2 isAdminOnlyPath 추가

```java
private boolean isAdminOnlyPath(String path) {
    return path.startsWith("/api/v1/agent") ||
           path.startsWith("/api/v1/auth/admin");
}
```

#### 4.3 handleForbidden 추가

- 기존 `handleUnauthorized` 패턴과 동일한 구조
- `ErrorCodeConstants.FORBIDDEN` / `MESSAGE_CODE_FORBIDDEN` 사용

### 5. SecurityConfig 수정

```java
// 순서 중요: 더 구체적인 매처가 앞에 와야 함
.requestMatchers("/api/v1/auth/admin/login").permitAll()
.requestMatchers("/api/v1/auth/admin/**").hasRole("ADMIN")
.requestMatchers("/api/v1/auth/**").permitAll()
```

> `JwtAuthenticationFilter`가 `ROLE_` + role로 권한을 부여하므로 `hasRole("ADMIN")`은 `ROLE_ADMIN` 권한을 확인합니다.

### 6. Agent 모듈 접근 제어 설계

- `X-Internal-Api-Key` 인증 제거, JWT 역할 기반으로 변경
- Gateway 레벨에서 ADMIN 역할 검증 (중앙 집중식)
- `AgentController`는 `x-user-id` 헤더만 활용
- `EmergingTechAgentScheduler`의 자동 실행은 내부 호출이므로 변경 없음

### 7. Chatbot Intent 확장 설계

#### 7.1 Intent.AGENT_COMMAND 추가

#### 7.2 IntentClassificationServiceImpl 수정
- `@agent` 프리픽스 감지를 **최우선** 으로 체크 (다른 키워드 매칭보다 먼저)
- 키워드 기반 감지 사용 금지 ("에이전트", "수집해줘" 등은 오분류 위험)

#### 7.3 ChatbotServiceImpl 수정
- `AGENT_COMMAND` case 추가
- 일반 사용자가 `@agent` 명령 사용 시 403 에러 대신 **안내 메시지** 반환 (채팅 UX 고려)

#### 7.4 AgentDelegationService 생성
- Chatbot → Agent 모듈 내부 호출 서비스
- 기존 Feign Client 패턴 활용

### 8. 시퀀스 다이어그램

다음 흐름에 대한 다이어그램 포함:
1. 관리자 계정 생성 (Gateway → Auth → DB)
2. 관리자 로그인 (공개 경로, JWT role=ADMIN 생성)
3. Agent 명령 처리 (Chatbot → Intent 분류 → 역할 확인 → Agent 위임)

### 9. 에러 처리

| 상황 | 예외 클래스 | HTTP 상태 | 에러 코드 | 비고 |
|------|------------|-----------|-----------|------|
| 권한 없음 | `ForbiddenException` | 403 | 4003 | 이미 존재 |
| 이메일/사용자명 중복 | `ConflictException` | **400** | **4006** | **409가 아님** (VALIDATION_ERROR 형식) |
| 관리자 미존재 | `ResourceNotFoundException` | 404 | 4004 | 이미 존재 |
| 인증 실패 | `UnauthorizedException` | 401 | 4001 | 이미 존재 |
| 자기 자신 삭제 | `ForbiddenException` | 403 | 4003 | |

### 10. 테스트 전략

IntelliJ HTTP Client 테스트 파일 (`api/bookmark/src/test/http/` 패턴 참고):
- 관리자 로그인 테스트 (시드 관리자로 로그인 → `adminAccessToken` 발급)
- 관리자 계정 생성 테스트 (성공, 권한 없음, 이메일 중복, 유효성 실패)
- 관리자 관리 테스트 (목록, 상세, 수정, 삭제)
- Agent 명령 테스트 (관리자 성공, 일반 사용자 안내 메시지, 일반 채팅)

> **주의**: `adminAccessToken`은 관리자 로그인 후 발급받은 JWT accessToken입니다. 별도의 시크릿 키가 아닙니다.
> **주의**: ConflictException 테스트 시 HTTP 400 + code "4006" 확인 (409 아님)

### 11. 구현 순서

0. 초기 관리자 시드 데이터 (Flyway 마이그레이션)
1. RefreshToken 관리자 지원 (refresh_tokens 스키마 변경, Entity/Service 수정)
2. TokenService role 파라미터 추가 + role 기반 RefreshToken 분기 + 기존 호출부 수정
3. AdminReaderRepository 확장
4. 관리자 관리 API 구현 (DTO → Service → Facade → Controller)
5. Gateway 역할 검증 (isPublicPath, isAdminOnlyPath, handleForbidden)
6. SecurityConfig 수정
7. Agent 모듈 인증 변경
8. Chatbot Intent 확장 + AgentDelegationService
9. 테스트 작성

---

## 검증 기준

설계서가 다음 기준을 **모두** 만족해야 합니다:

### 1. 정합성 검증

- [ ] 기존 `AdminEntity`(`admins` 테이블)를 활용 (UserEntity에 role 추가 금지)
- [ ] 기존 `AdminReaderRepository`, `AdminWriterRepository` 활용 (새로 만들지 않음)
- [ ] 기존 `ForbiddenException`, `ErrorCodeConstants.FORBIDDEN` 활용 (새로 만들지 않음)
- [ ] 기존 `GlobalExceptionHandler` ForbiddenException 핸들러 활용 (수정 불필요)
- [ ] `ConflictException`이 **400/4006** 반환하는 기존 동작 반영 (409 아님)
- [ ] JWT 토큰 페이로드 구조 유지 (userId, email, role)
- [ ] Gateway 필터 로직과의 일관성
- [ ] Soft Delete 원칙 준수 (BaseWriterRepository + HistoryService)

### 2. 완전성 검증

- [ ] 관리자 로그인 엔드포인트 설계 포함
- [ ] TokenService role 파라미터 추가 설계 포함
- [ ] 모든 API 엔드포인트 명세 포함
- [ ] 시퀀스 다이어그램 포함
- [ ] 에러 처리 시나리오 명시
- [ ] 테스트 전략 포함

### 3. 설계 원칙 준수

- [ ] SOLID 원칙 적용
- [ ] Controller에서 `@AuthenticationPrincipal UserPrincipal` 사용 (x-user-role 헤더 직접 사용 금지)
- [ ] 기존 Controller → Facade → Service 패턴 준수
- [ ] Reader/Writer Repository 분리 패턴 준수
- [ ] ApiResponse 형식 유지

### 4. 오버엔지니어링 방지

- [ ] Role enum 생성 금지 (AdminEntity.role은 String 타입)
- [ ] Agent 명령 감지는 `@agent` 프리픽스만 사용 (키워드 기반 감지 금지)
- [ ] 불필요한 예외 클래스 생성 금지 (기존 예외 재사용)
- [ ] 불필요한 추상화 계층 추가 금지
- [ ] 요구사항에 명시되지 않은 기능 추가 금지

### 5. 보안 검증

- [ ] Gateway 레벨에서 관리자 전용 경로 검증
- [ ] SecurityConfig에서 `hasRole("ADMIN")` 적용
- [ ] Admin 로그인 경로는 공개 (permitAll)
- [ ] 관리자 전용 API는 ADMIN 역할만 접근 가능

---

## 제한 사항

1. **외부 자료 참고 시 공식 문서만 사용**
   - Spring Security: https://docs.spring.io/spring-security/reference/
   - Spring Cloud Gateway: https://docs.spring.io/spring-cloud-gateway/reference/
   - JWT (RFC 7519): https://tools.ietf.org/html/rfc7519

2. **오버엔지니어링 금지**
   - 불필요한 추상화 계층 추가 금지
   - 요구사항에 명시되지 않은 기능 추가 금지
   - 복잡한 디자인 패턴 남용 금지
   - LLM이 임의로 생성하는 것 방지: 기존 코드 반드시 확인 후 설계

3. **기존 코드 패턴 준수**
   - Controller → Facade → Service 구조
   - Reader/Writer Repository 분리 유지
   - ApiResponse 형식 유지
   - Soft Delete 패턴 유지

---

## 설계서 출력 위치

```
docs/reference/admin-role-based-auth-design.md
```

---

## 실행 지시

다음 명령어로 설계서 작성을 시작하세요:

```
위의 모든 요구사항과 검증 기준을 충족하는 관리자 권한 기반 인증/인가 시스템 상세 설계서를 작성하세요.
반드시 기존 코드베이스를 확인하여 이미 존재하는 컴포넌트를 파악하고, 중복 생성하지 마세요.
```

---

**작성일**: 2026-02-04
**버전**: 2.0 (v1.0 대비 기존 코드베이스 정합성 전면 개정)
**대상 모듈**: api/auth, api/gateway, api/chatbot, api/agent
