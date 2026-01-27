# API 정의서 작성 프롬프트

## Role Definition
당신은 백엔드 시니어 개발자입니다. 프론트엔드 개발자와 협업하기 위한 명확하고 실용적인 API 정의서를 작성해야 합니다.

## Context
- **대상 모듈**: `@api/gateway`, `@api/auth`
- **목적**: 웹/모바일 클라이언트 연동을 위한 API 스펙 문서화
- **기술 스택**: Spring Cloud Gateway, Spring Boot, JWT 인증
- **참고 문서**:
  - `@docs/step2/1. api-endpoint-design.md` - API 엔드포인트 설계
  - `@docs/step14/gateway-design.md` - Gateway 아키텍처 설계
  - `@api/gateway/REF.md` - Gateway 구현 참고
  - `@api/auth/src/main/java/.../controller/AuthController.java` - Auth API 구현

## Task
아래 구조와 내용으로 API 정의서를 작성하세요.

### 1. 문서 구조
```
1. 개요
   - API 서버 기본 정보 (Base URL, 버전)
   - 인증 방식 (JWT)
   - 공통 응답 형식

2. Gateway 서버 정보
   - 라우팅 규칙
   - CORS 정책
   - 타임아웃 설정

3. 인증 API (api/auth)
   - 회원가입
   - 로그인
   - 로그아웃
   - 토큰 갱신
   - 이메일 인증
   - 비밀번호 재설정
   - OAuth 로그인

4. 에러 처리
   - HTTP 상태 코드별 에러 응답
   - 에러 코드 정의

5. 부록
   - 환경별 엔드포인트
   - JWT 토큰 갱신 플로우
```

### 2. 필수 포함 내용

#### API 기본 정보
- **Base URL**: 
  - Local: `http://localhost:8081`
  - Dev: `https://api.dev.example.com`
  - Prod: `https://api.example.com`
- **API Version**: `v1`
- **Content-Type**: `application/json`
- **Authorization Header**: `Authorization: Bearer {access_token}`

#### 공통 응답 형식
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

#### 각 API 엔드포인트 정의 항목
1. **HTTP Method + Path**
2. **설명** (1줄)
3. **인증 필요 여부**
4. **Request Headers** (필요한 경우만)
5. **Request Body** (JSON 예시)
6. **Response** (JSON 예시)
7. **주요 에러 케이스** (HTTP 상태 코드 + 에러 메시지)

### 3. 제약사항 (Critical)

❌ **금지사항**:
- 과도한 설명이나 배경 지식 서술
- 구현 방법이나 내부 로직 언급
- "이렇게 하면 좋습니다", "권장합니다" 등의 조언성 문구
- 불필요한 예제나 대안 제시
- LLM 스타일의 장황한 주석
- 추가 기능이나 개선 사항 제안

✅ **필수사항**:
- 명확하고 간결한 기술적 사실만 기술
- 실제 구현된 API만 문서화 (추측 금지)
- 참고 문서에 명시된 내용만 사용
- 프론트엔드 개발자가 즉시 사용 가능한 형식
- 예시는 실제 사용 가능한 데이터로만 작성

### 4. 작성 가이드

#### Step 1: 참고 문서 분석
1. `@docs/step2/1. api-endpoint-design.md`에서 Auth API 엔드포인트 확인
2. `@docs/step14/gateway-design.md`에서 라우팅 규칙과 인증 흐름 확인
3. `@api/gateway/REF.md`에서 실제 구현된 Gateway 설정 확인
4. `@api/auth/src/.../controller/AuthController.java`에서 실제 API 시그니처 확인

#### Step 2: API 정의서 작성
- **간결성**: 각 항목은 최소한의 필수 정보만 포함
- **정확성**: 참고 문서의 정확한 경로, 파라미터, 응답 형식 사용
- **일관성**: 모든 API는 동일한 형식으로 작성
- **실용성**: 프론트엔드 개발자가 복사-붙여넣기 가능한 예시 제공

#### Step 3: 검증
- 모든 엔드포인트가 참고 문서에 존재하는지 확인
- Request/Response 예시가 실제 구현과 일치하는지 확인
- 불필요한 설명이나 주석이 없는지 확인

## Output Format

```markdown
# API 정의서

## 1. 개요

### Base URL
- Local: `http://localhost:8081`
- Dev: `https://api.dev.example.com`
- Prod: `https://api.example.com`

### 인증
- Type: Bearer Token (JWT)
- Header: `Authorization: Bearer {access_token}`

### 공통 응답 형식
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

## 2. Gateway 정보

### 라우팅
| 경로 패턴 | 대상 서버 | 인증 필요 |
|----------|---------|---------|
| `/api/v1/auth/**` | api-auth | ❌ |
| `/api/v1/archive/**` | api-archive | ✅ |

### CORS
- Allowed Origins: `http://localhost:*`, `https://*.example.com`
- Allowed Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- Max Age: `3600`

## 3. 인증 API

### 3.1 회원가입

**POST** `/api/v1/auth/signup`

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
- `400` - 이메일 중복, 비밀번호 정책 위반
- `500` - 서버 오류

... (이하 동일 형식으로 계속)
```

## Validation Checklist

작성 완료 후 다음 항목을 확인하세요:

- [ ] 모든 API는 참고 문서에 실제로 존재함
- [ ] Request/Response 예시가 정확함
- [ ] 불필요한 설명이나 조언성 문구가 없음
- [ ] LLM 스타일의 장황한 주석이 없음
- [ ] 추가 기능 제안이나 개선 사항이 없음
- [ ] 프론트엔드 개발자가 즉시 사용 가능한 형식임
- [ ] 에러 케이스가 명확하게 정의됨

## Reference Sources

작성 시 **반드시** 다음 문서만 참고:
1. `@docs/step2/1. api-endpoint-design.md` - API 엔드포인트 스펙
2. `@docs/step14/gateway-design.md` - Gateway 라우팅 및 인증
3. `@api/gateway/REF.md` - Gateway 실제 구현 참고
4. `@api/auth/src/.../controller/AuthController.java` - Auth API 실제 구현

외부 자료 참고 시:
- Spring Cloud Gateway 공식 문서만 참고
- JWT (RFC 7519) 표준만 참고
- 개인 블로그, 튜토리얼 등은 참고 금지

---

**작성 시작**: 위 지침에 따라 API 정의서 작성을 시작하세요.
