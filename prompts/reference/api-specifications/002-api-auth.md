# Auth API 스펙 정의서

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-auth |
| Base URL | `/api/v1/auth` |
| 포트 | 8083 (via Gateway: 8081) |
| 설명 | 사용자 인증 및 OAuth 로그인 API |

---

## 공통 응답 형식

### ApiResponse<T>

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | string | Y | 응답 코드 (성공: "2000") |
| messageCode | MessageCode | Y | 메시지 코드 객체 |
| message | string | N | 응답 메시지 (성공 시: "success") |
| data | T | N | 응답 데이터 |

### MessageCode

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | string | Y | 메시지 코드 (성공: "SUCCESS") |
| text | string | Y | 메시지 텍스트 (성공: "성공") |

---

## 사용자 인증 API

### 1. 회원가입

**POST** `/api/v1/auth/signup`

새로운 사용자 계정을 생성합니다. 이메일 인증이 필요합니다.

#### Request Body

```json
{
  "email": "user@example.com",
  "username": "사용자명",
  "password": "Password123!"
}
```

#### Request Body Types

**SignupRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| email | string | Y | 이메일 형식 | 사용자 이메일 |
| username | string | Y | 3~50자 | 사용자명 |
| password | string | Y | 최소 8자, 대소문자/숫자/특수문자 중 2가지 이상 | 비밀번호 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "userId": 12345,
    "email": "user@example.com",
    "username": "사용자명",
    "message": "이메일 인증 링크가 발송되었습니다."
  }
}
```

#### Response Data Types

**AuthResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| userId | number | Y | 사용자 ID |
| email | string | Y | 이메일 |
| username | string | Y | 사용자명 |
| message | string | Y | 안내 메시지 |

---

### 2. 로그인

**POST** `/api/v1/auth/login`

이메일/비밀번호로 로그인합니다.

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "Password123!"
}
```

#### Request Body Types

**LoginRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| email | string | Y | 이메일 형식 | 사용자 이메일 |
| password | string | Y | - | 비밀번호 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenExpiresIn": 604800
  }
}
```

#### Response Data Types

**TokenResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| accessToken | string | Y | JWT 액세스 토큰 |
| refreshToken | string | Y | 리프레시 토큰 |
| tokenType | string | Y | 토큰 타입 (항상 "Bearer") |
| expiresIn | number | Y | 액세스 토큰 만료 시간 (초) |
| refreshTokenExpiresIn | number | Y | 리프레시 토큰 만료 시간 (초) |

---

### 3. 로그아웃

**POST** `/api/v1/auth/logout`

현재 세션을 로그아웃합니다. 리프레시 토큰이 무효화됩니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` |

#### Request Body

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

#### Request Body Types

**LogoutRequest**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | string | Y | 리프레시 토큰 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

### 4. 회원탈퇴

**DELETE** `/api/v1/auth/me`

현재 사용자 계정을 탈퇴합니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` |

#### Request Body (Optional)

```json
{
  "password": "Password123!",
  "reason": "서비스를 더 이상 사용하지 않습니다."
}
```

#### Request Body Types

**WithdrawRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| password | string | N | 8~100자 | 비밀번호 확인 (보안 강화용) |
| reason | string | N | 최대 500자 | 탈퇴 사유 (피드백 수집용) |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

### 5. 토큰 갱신

**POST** `/api/v1/auth/refresh`

리프레시 토큰으로 새 액세스 토큰을 발급받습니다.

#### Request Body

```json
{
  "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
}
```

#### Request Body Types

**RefreshTokenRequest**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | string | Y | 리프레시 토큰 |

#### Response

`TokenResponse` 형식 (로그인 응답과 동일)

---

### 6. 이메일 인증

**GET** `/api/v1/auth/verify-email`

이메일 인증 토큰을 검증합니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| token | string | Y | 이메일 인증 토큰 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

### 7. 비밀번호 재설정 요청

**POST** `/api/v1/auth/reset-password`

비밀번호 재설정 이메일을 발송합니다.

#### Request Body

```json
{
  "email": "user@example.com"
}
```

#### Request Body Types

**ResetPasswordRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| email | string | Y | 이메일 형식 | 사용자 이메일 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

### 8. 비밀번호 재설정 확인

**POST** `/api/v1/auth/reset-password/confirm`

토큰으로 비밀번호를 재설정합니다.

#### Request Body

```json
{
  "token": "reset-password-token-string",
  "newPassword": "NewPassword123!"
}
```

#### Request Body Types

**ResetPasswordConfirmRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| token | string | Y | - | 비밀번호 재설정 토큰 |
| newPassword | string | Y | 최소 8자, 대소문자/숫자/특수문자 중 2가지 이상 | 새 비밀번호 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

## OAuth 인증 API

### 9. OAuth 로그인 시작

**GET** `/api/v1/auth/oauth2/{provider}`

OAuth 인증 페이지로 리다이렉트합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| provider | string | Y | OAuth 제공자 (`google`, `kakao`, `naver`) |

#### Response

HTTP 302 Redirect → OAuth 제공자 인증 페이지

---

### 10. OAuth 콜백

**GET** `/api/v1/auth/oauth2/{provider}/callback`

OAuth 인증 완료 후 토큰을 발급합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| provider | string | Y | OAuth 제공자 (`google`, `kakao`, `naver`) |

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| code | string | Y | OAuth 인증 코드 |
| state | string | N | CSRF 방지용 state 값 |

#### Response

`TokenResponse` 형식 (로그인 응답과 동일)

---

## 관리자 API

### 11. 관리자 로그인

**POST** `/api/v1/auth/admin/login`

관리자 계정으로 로그인합니다.

#### Request Body

`LoginRequest` 형식 (일반 로그인과 동일)

#### Response

`TokenResponse` 형식 (로그인 응답과 동일)

---

### 12. 관리자 계정 생성

**POST** `/api/v1/auth/admin/accounts`

새 관리자 계정을 생성합니다. SUPER_ADMIN 권한 필요.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` (SUPER_ADMIN) |

#### Request Body

```json
{
  "email": "admin@example.com",
  "username": "관리자",
  "password": "AdminPassword123!"
}
```

#### Request Body Types

**AdminCreateRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| email | string | Y | 이메일 형식 | 관리자 이메일 |
| username | string | Y | 2~50자 | 관리자명 |
| password | string | Y | 최소 8자 | 비밀번호 |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "id": 1,
    "email": "admin@example.com",
    "username": "관리자",
    "role": "ADMIN",
    "isActive": true,
    "createdAt": "2025-01-15T10:00:00",
    "lastLoginAt": null
  }
}
```

#### Response Data Types

**AdminResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | number | Y | 관리자 ID |
| email | string | Y | 이메일 |
| username | string | Y | 관리자명 |
| role | string | Y | 권한 (`ADMIN`, `SUPER_ADMIN`) |
| isActive | boolean | Y | 활성화 여부 |
| createdAt | string (ISO 8601) | Y | 생성일시 |
| lastLoginAt | string (ISO 8601) | N | 마지막 로그인 일시 |

---

### 13. 관리자 목록 조회

**GET** `/api/v1/auth/admin/accounts`

관리자 계정 목록을 조회합니다. 관리자 권한 필요.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` (ADMIN 이상) |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": [
    {
      "id": 1,
      "email": "admin@example.com",
      "username": "관리자",
      "role": "ADMIN",
      "isActive": true,
      "createdAt": "2025-01-15T10:00:00",
      "lastLoginAt": "2025-01-20T14:30:00"
    }
  ]
}
```

---

### 14. 관리자 상세 조회

**GET** `/api/v1/auth/admin/accounts/{adminId}`

특정 관리자 계정을 조회합니다. 관리자 권한 필요.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` (ADMIN 이상) |

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| adminId | number | Y | 관리자 ID |

#### Response

`AdminResponse` 형식

---

### 15. 관리자 정보 수정

**PUT** `/api/v1/auth/admin/accounts/{adminId}`

관리자 계정 정보를 수정합니다. SUPER_ADMIN 권한 필요.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` (SUPER_ADMIN) |

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| adminId | number | Y | 관리자 ID |

#### Request Body

```json
{
  "username": "수정된관리자명",
  "password": "NewPassword123!"
}
```

#### Request Body Types

**AdminUpdateRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| username | string | N | 2~50자 | 관리자명 |
| password | string | N | 최소 8자 | 비밀번호 |

#### Response

`AdminResponse` 형식

---

### 16. 관리자 계정 삭제

**DELETE** `/api/v1/auth/admin/accounts/{adminId}`

관리자 계정을 삭제합니다. SUPER_ADMIN 권한 필요. 자기 자신은 삭제 불가.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` (SUPER_ADMIN) |

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| adminId | number | Y | 관리자 ID |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

---

## TypeScript 타입 정의 (Frontend)

```typescript
// Response Types
interface MessageCode {
  code: string;
  text: string;
}

interface ApiResponse<T> {
  code: string;
  messageCode: MessageCode;
  message?: string;
  data?: T;
}

interface AuthResponse {
  userId: number;
  email: string;
  username: string;
  message: string;
}

interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  refreshTokenExpiresIn: number;
}

interface AdminResponse {
  id: number;
  email: string;
  username: string;
  role: 'ADMIN' | 'SUPER_ADMIN';
  isActive: boolean;
  createdAt: string; // ISO 8601
  lastLoginAt?: string; // ISO 8601
}

// Request Types
interface SignupRequest {
  email: string;
  username: string;
  password: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface LogoutRequest {
  refreshToken: string;
}

interface RefreshTokenRequest {
  refreshToken: string;
}

interface WithdrawRequest {
  password?: string;
  reason?: string;
}

interface ResetPasswordRequest {
  email: string;
}

interface ResetPasswordConfirmRequest {
  token: string;
  newPassword: string;
}

interface AdminCreateRequest {
  email: string;
  username: string;
  password: string;
}

interface AdminUpdateRequest {
  username?: string;
  password?: string;
}

// OAuth Provider Types
type OAuthProvider = 'google' | 'kakao' | 'naver';
```

---

## 에러 응답

에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "code": "4010",
  "messageCode": {
    "code": "UNAUTHORIZED",
    "text": "인증이 필요합니다."
  },
  "message": null,
  "data": null
}
```

### 주요 에러 코드

| 코드 | 설명 |
|------|------|
| 4000 | 잘못된 요청 (Validation Error) |
| 4010 | 인증 실패 (Unauthorized) |
| 4030 | 권한 없음 (Forbidden) |
| 4040 | 리소스 없음 (Not Found) |
| 4090 | 충돌 (Conflict - 이메일 중복 등) |
| 5000 | 서버 에러 (Internal Server Error) |

### 인증 관련 에러 사례

| 상황 | 코드 | 설명 |
|------|------|------|
| 잘못된 이메일/비밀번호 | 4010 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| 이메일 미인증 | 4010 | 이메일 인증이 완료되지 않았습니다. |
| 만료된 토큰 | 4010 | 토큰이 만료되었습니다. |
| 이메일 중복 | 4090 | 이미 사용 중인 이메일입니다. |
| 비밀번호 규칙 위반 | 4000 | 비밀번호는 대소문자/숫자/특수문자 중 2가지 이상을 포함해야 합니다. |
