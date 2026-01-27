# API 정의서

**작성일**: 2026-01-21  
**대상**: 웹/모바일 클라이언트 개발자  
**버전**: v1

---

## 1. 개요

### Base URL

| 환경 | URL |
|-----|-----|
| Local | `http://localhost:8081` |
| Dev | `http://api-gateway-service:8081` |
| Beta | `http://api-gateway-service:8081` |
| Prod | `http://api-gateway-service:8081` |

### 인증

- **Type**: Bearer Token (JWT)
- **Header**: `Authorization: Bearer {access_token}`
- **Access Token 만료**: 3600초 (1시간)
- **Refresh Token 만료**: 604800초 (7일)

### 공통 응답 형식

#### 성공 응답
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {...}
}
```

#### 에러 응답
```json
{
  "code": "4001",
  "messageCode": {
    "code": "AUTH_FAILED",
    "text": "인증에 실패했습니다."
  },
  "message": "JWT token is missing or invalid",
  "data": null
}
```

---

## 2. Gateway 정보

### 라우팅 규칙

| 경로 패턴 | 대상 서버 | 인증 필요 |
|----------|---------|---------|
| `/api/v1/auth/**` | api-auth | ❌ |
| `/api/v1/archive/**` | api-archive | ✅ |
| `/api/v1/contest/**` | api-contest | ❌ |
| `/api/v1/news/**` | api-news | ❌ |
| `/api/v1/chatbot/**` | api-chatbot | ✅ |

### CORS 정책

#### Local 환경
- **Allowed Origins**: `http://localhost:*`, `http://127.0.0.1:*`
- **Allowed Methods**: `GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD`
- **Allowed Headers**: `*`
- **Max Age**: `3600`

#### Dev/Beta/Prod 환경
- **Allowed Origins**: `https://dev.example.com`, `https://beta.example.com`, `https://example.com`
- **Allowed Methods**: `GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD`
- **Allowed Headers**: `*`
- **Max Age**: `3600`

### 타임아웃 설정

- **Connection Timeout**: 30초
- **Response Timeout**: 60초

### 인증된 요청 시 추가 헤더

Gateway는 JWT 검증 성공 시 다음 헤더를 추가하여 백엔드 서버로 전달:

```
x-user-id: {userId}
x-user-email: {email}
x-user-role: {role}
```

---

## 3. 인증 API

### 3.1 회원가입

**POST** `/api/v1/auth/signup`

**인증**: 불필요

**Request**
```json
{
  "email": "user@example.com",
  "username": "john_doe",
  "password": "securePassword123"
}
```

**Response** (200 OK)
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "userId": "1234567890123456789",
    "email": "user@example.com",
    "username": "john_doe",
    "message": "회원가입이 완료되었습니다. 이메일 인증을 완료해주세요."
  }
}
```

**Errors**
- `400` - 이메일 중복, 사용자명 중복, 비밀번호 정책 위반
- `500` - 서버 오류

---

### 3.2 로그인

**POST** `/api/v1/auth/login`

**인증**: 불필요

**Request**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

**Response** (200 OK)
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
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenExpiresIn": 604800
  }
}
```

**Errors**
- `401` - 이메일 또는 비밀번호 불일치, 이메일 미인증
- `500` - 서버 오류

---

### 3.3 로그아웃

**POST** `/api/v1/auth/logout`

**인증**: 필요

**Request**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK)
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

**Errors**
- `401` - 인증 실패, Refresh Token 불일치
- `500` - 서버 오류

---

### 3.4 토큰 갱신

**POST** `/api/v1/auth/refresh`

**인증**: 불필요

**Request**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response** (200 OK)
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
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**Errors**
- `401` - Refresh Token 만료 또는 무효
- `500` - 서버 오류

---

### 3.5 이메일 인증

**GET** `/api/v1/auth/verify-email`

**인증**: 불필요

**Query Parameters**
- `token` (required): 이메일 인증 토큰

**Request**
```
GET /api/v1/auth/verify-email?token=abc123xyz
```

**Response** (200 OK)
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "message": "이메일 인증이 완료되었습니다."
  }
}
```

**Errors**
- `400` - 토큰 만료, 토큰 무효, 중복 인증
- `500` - 서버 오류

---

### 3.6 비밀번호 재설정 요청

**POST** `/api/v1/auth/reset-password`

**인증**: 불필요

**Request**
```json
{
  "email": "user@example.com"
}
```

**Response** (200 OK)
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "message": "비밀번호 재설정 이메일이 발송되었습니다."
  }
}
```

**Errors**
- `500` - 서버 오류

**Note**: 보안상 존재하지 않는 이메일도 성공 응답 반환

---

### 3.7 비밀번호 재설정 확인

**POST** `/api/v1/auth/reset-password/confirm`

**인증**: 불필요

**Request**
```json
{
  "token": "reset-token-123",
  "newPassword": "newSecurePassword123"
}
```

**Response** (200 OK)
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "message": "비밀번호가 성공적으로 변경되었습니다."
  }
}
```

**Errors**
- `400` - 토큰 만료, 토큰 무효, 비밀번호 정책 위반, 이전 비밀번호와 동일
- `500` - 서버 오류

---

### 3.8 OAuth 로그인 시작

**GET** `/api/v1/auth/oauth2/{provider}`

**인증**: 불필요

**Path Parameters**
- `provider`: OAuth 제공자 (`google`, `github`, `kakao`, `naver`)

**Request**
```
GET /api/v1/auth/oauth2/google
```

**Response** (302 Redirect)

OAuth 제공자 인증 페이지로 리다이렉트

**Errors**
- `400` - 지원하지 않는 OAuth 제공자
- `500` - 서버 오류

---

### 3.9 OAuth 로그인 콜백

**GET** `/api/v1/auth/oauth2/{provider}/callback`

**인증**: 불필요

**Path Parameters**
- `provider`: OAuth 제공자 (`google`, `github`, `kakao`, `naver`)

**Query Parameters**
- `code` (required): OAuth 인증 코드
- `state` (optional): CSRF 방지 상태 토큰

**Request**
```
GET /api/v1/auth/oauth2/google/callback?code=auth_code_123&state=state_token_456
```

**Response** (200 OK)
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
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "refreshTokenExpiresIn": 604800
  }
}
```

**Errors**
- `401` - State 토큰 불일치, OAuth 인증 실패
- `500` - 서버 오류

---

## 4. 토큰 갱신 플로우

### 시나리오 1: Access Token 만료

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Auth

    Client->>Gateway: GET /api/v1/archive<br/>Bearer {expired_token}
    Gateway->>Client: 401 Unauthorized
    Client->>Gateway: POST /api/v1/auth/refresh<br/>{refreshToken}
    Gateway->>Auth: Forward
    Auth->>Gateway: 200 OK {new tokens}
    Gateway->>Client: 200 OK {new tokens}
    Client->>Gateway: GET /api/v1/archive<br/>Bearer {new_token}
    Gateway->>Client: 200 OK
```

### 시나리오 2: Refresh Token도 만료

```mermaid
sequenceDiagram
    participant Client
    participant User
    participant Gateway
    participant Auth

    Client->>Gateway: GET /api/v1/archive<br/>Bearer {expired_token}
    Gateway->>Client: 401 Unauthorized
    Client->>Gateway: POST /api/v1/auth/refresh<br/>{expired_refresh_token}
    Gateway->>Auth: Forward
    Auth->>Gateway: 401 Unauthorized
    Gateway->>Client: 401 Unauthorized
    Client->>User: 로그인 화면 표시
    User->>Client: 이메일/비밀번호 입력
    Client->>Gateway: POST /api/v1/auth/login
    Gateway->>Auth: Forward
    Auth->>Gateway: 200 OK {new tokens}
    Gateway->>Client: 200 OK {new tokens}
```

---

## 5. 에러 코드

### HTTP 상태 코드

| 상태 코드 | 에러 코드 | 설명 |
|---------|---------|------|
| 400 | `4000` | 잘못된 요청 |
| 401 | `4001` | 인증 실패 |
| 403 | `4003` | 권한 없음 |
| 404 | `4004` | 리소스 없음 |
| 500 | `5000` | 서버 내부 오류 |
| 502 | `5002` | 백엔드 서버 연결 실패 |
| 504 | `5004` | 백엔드 서버 타임아웃 |

### 인증 관련 에러 메시지

| 에러 코드 | 메시지 |
|---------|--------|
| `AUTH_FAILED` | 인증에 실패했습니다. |
| `INVALID_TOKEN` | 유효하지 않은 토큰입니다. |
| `TOKEN_EXPIRED` | 토큰이 만료되었습니다. |
| `EMAIL_NOT_VERIFIED` | 이메일 인증이 필요합니다. |
| `EMAIL_ALREADY_EXISTS` | 이미 존재하는 이메일입니다. |
| `USERNAME_ALREADY_EXISTS` | 이미 존재하는 사용자명입니다. |
| `PASSWORD_POLICY_VIOLATION` | 비밀번호 정책을 충족하지 않습니다. |
| `INVALID_CREDENTIALS` | 이메일 또는 비밀번호가 일치하지 않습니다. |

---

## 6. 개발 환경 설정

### Local 환경

1. **Gateway 서버**: `http://localhost:8081`
2. **Auth 서버**: `http://localhost:8082`
3. **Frontend 개발 서버**: `http://localhost:3000` (CORS 허용됨)

### 테스트용 계정

Local 환경에서 테스트용으로 사용 가능한 계정:

```json
{
  "email": "test@example.com",
  "password": "Test1234!"
}
```

### 환경 변수

Frontend 개발 시 다음 환경 변수 설정:

```bash
# .env.local
NEXT_PUBLIC_API_BASE_URL=http://localhost:8081
NEXT_PUBLIC_API_VERSION=v1
```

---

## 부록 A. Request 예시

### cURL 예시

#### 로그인
```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "securePassword123"
  }'
```

#### 인증 필요한 API 호출
```bash
curl -X GET http://localhost:8081/api/v1/archive \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### JavaScript/TypeScript 예시

```typescript
// 로그인
const response = await fetch('http://localhost:8081/api/v1/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'securePassword123',
  }),
});

const data = await response.json();
const accessToken = data.data.accessToken;

// 인증 필요한 API 호출
const archiveResponse = await fetch('http://localhost:8081/api/v1/archive', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
  },
});
```

---

## 부록 B. 비밀번호 정책

- **최소 길이**: 8자
- **필수 포함**: 대소문자/숫자/특수문자 중 2가지 이상

---

## 부록 C. Rate Limiting

| 엔드포인트 | 제한 |
|-----------|-----|
| `/api/v1/auth/login` | 5회/분 |
| `/api/v1/auth/reset-password` | 3회/시간 |
| `/api/v1/auth/oauth2/*` | 10회/분 |

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-21
