# 프론트엔드 웹앱 개발 설계서 작성 프롬프트

## 배경 및 목적

현재 프로젝트는 Java 21 + Spring Boot 4.0.1 기반의 백엔드 API 서버가 구축되어 있으며, Spring Security + JWT 기반 인증 시스템이 완성되어 있습니다. 이제 이 백엔드 API와 연동할 프론트엔드 웹 애플리케이션의 설계서를 작성해야 합니다.

**프로젝트 기술 스택 개요**:
- **백엔드**: Java 21, Spring Boot 4.0.1, Spring Security 6.x
- **데이터베이스**: Aurora MySQL (Command Side), MongoDB Atlas (Query Side)
- **인증**: JWT (Access Token 1시간, Refresh Token 7일)
- **API Gateway**: Spring Cloud Gateway (포트 8081)
- **패턴**: CQRS, 이벤트 기반 (Kafka)

**설계서 작성 목적**:
1. 백엔드 API와 연동할 프론트엔드 웹앱의 기술 스택 선정
2. 9개 인증 API 엔드포인트 우선 연동을 위한 프로토타입 설계
3. 클린코드, SOLID 원칙, 프론트엔드 베스트프랙티스를 준수하는 구현 가이드 제공

---

## 입력 문서

다음 문서들을 참고하여 설계서를 작성하세요:

### 필수 참고 문서

1. **@docs/API-SPECIFICATION.md**
   - Base URL: `http://localhost:8081` (Local 환경)
   - 9개 인증 API 엔드포인트 명세:
     - POST `/api/v1/auth/signup` (회원가입)
     - POST `/api/v1/auth/login` (로그인)
     - POST `/api/v1/auth/logout` (로그아웃)
     - POST `/api/v1/auth/refresh` (토큰 갱신)
     - GET `/api/v1/auth/verify-email` (이메일 인증)
     - POST `/api/v1/auth/reset-password` (비밀번호 재설정 요청)
     - POST `/api/v1/auth/reset-password/confirm` (비밀번호 재설정 확인)
     - GET `/api/v1/auth/oauth2/{provider}` (OAuth 로그인 시작)
     - GET `/api/v1/auth/oauth2/{provider}/callback` (OAuth 로그인 콜백)
   - JWT 토큰 인증: `Authorization: Bearer {access_token}`
   - 토큰 만료 시간: Access Token 3600초(1시간), Refresh Token 604800초(7일)
   - 공통 응답 형식: `ApiResponse<T>` (code, messageCode, message, data)
   - 에러 응답 코드: 400, 401, 403, 404, 500, 502, 504

2. **@api/auth 모듈 구현**
   - Spring Security + JWT 기반 인증
   - OAuth 2.0 지원 (Google, GitHub, Kakao, Naver)
   - Refresh Token Rotation 전략 (기존 토큰 무효화 후 새 토큰 발급)
   - 비밀번호 정책: 최소 8자, 대소문자/숫자/특수문자 중 2가지 이상
   - BCrypt 비밀번호 해시 (salt rounds: 12)

3. **@docs/step6/spring-security-auth-design-guide.md**
   - JWT 토큰 생성/검증/갱신 메커니즘
   - 로그인/로그아웃 플로우
   - 이메일 인증 플로우
   - 비밀번호 재설정 전체 프로세스
   - OAuth 로그인 시작/콜백 플로우
   - State 파라미터 (Redis 저장, 10분 TTL, CSRF 방지)

4. **@docs/step14/gateway-design.md**
   - API Gateway 라우팅 규칙
   - JWT 토큰 검증 필터
   - 토큰 만료 시 처리 (401 Unauthorized 반환)
   - 토큰 갱신 흐름 (클라이언트가 자동 처리)
   - CORS 정책 (Local 환경: `http://localhost:*` 허용)

### 선택 참고 문서

5. **@docs/step2/1. api-endpoint-design.md**
   - CQRS 패턴 기반 API 설계
   - 공개 API vs 인증 필요 API 구분
   - Archive/Contest/News/Chatbot API 명세 (향후 연동 대상)

---

## 설계서 요구사항

다음 요구사항을 모두 충족하는 프론트엔드 웹앱 설계서를 작성하세요:

### 1. 프레임워크 선택 및 기술 스택

**목표**: Next.js, React, Vue 중 현재 프로젝트에 가장 적합한 프레임워크를 선정하고 근거를 제시하세요.

**선정 기준**:
- **백엔드 통합**: Java/Spring Boot 기반 REST API와의 연동 용이성
- **인증 시스템**: JWT + Refresh Token 관리, OAuth 2.0 지원
- **라우팅**: SPA (Single Page Application) vs SSR (Server-Side Rendering) vs SSG (Static Site Generation)
- **타입 안전성**: TypeScript 지원 및 API 타입 안전성
- **개발 생산성**: 빌드 속도, 개발자 경험, 커뮤니티 지원
- **배포 및 운영**: Docker 컨테이너화, CI/CD 지원

**필수 포함 내용**:
1. 프레임워크 비교표 (Next.js, React, Vue)
2. 선정된 프레임워크와 그 근거
3. 핵심 라이브러리 선정 (상태 관리, API 클라이언트, UI 프레임워크, 폼 관리 등)
4. TypeScript 설정 및 타입 안전성 확보 방안

**제약사항**:
- 공식 문서만 참고 (Next.js 공식 문서, React 공식 문서, Vue 공식 문서)
- 2026년 1월 기준 최신 안정 버전 기준으로 선정
- 오버엔지니어링 금지: 단순하고 검증된 라이브러리 우선 선택

---

### 2. 프로토타입 설계 (9개 인증 API 우선 연동)

**목표**: 9개 인증 API 엔드포인트를 우선 연동하는 프로토타입 설계를 제시하세요.

**필수 기능**:

#### 2.1. 인증 페이지
1. **회원가입 페이지** (`POST /api/v1/auth/signup`)
   - 입력 필드: 이메일, 사용자명, 비밀번호
   - 유효성 검증: 비밀번호 정책 (최소 8자, 대소문자/숫자/특수문자 중 2가지 이상)
   - 에러 처리: 이메일 중복, 사용자명 중복, 비밀번호 정책 위반
   - 성공 후 처리: 이메일 인증 안내 메시지 표시

2. **로그인 페이지** (`POST /api/v1/auth/login`)
   - 입력 필드: 이메일, 비밀번호
   - OAuth 로그인 버튼: Google, GitHub, Kakao, Naver
   - 에러 처리: 이메일 미인증, 이메일/비밀번호 불일치
   - 성공 후 처리: Access Token 및 Refresh Token 저장 (LocalStorage 또는 Cookie), 메인 페이지 이동

3. **이메일 인증 페이지** (`GET /api/v1/auth/verify-email?token=xxx`)
   - URL 쿼리 파라미터에서 토큰 추출
   - 자동 인증 요청 실행
   - 성공: "이메일 인증이 완료되었습니다. 로그인해주세요." 메시지 표시
   - 실패: "유효하지 않거나 만료된 토큰입니다." 메시지 표시

4. **비밀번호 재설정 요청 페이지** (`POST /api/v1/auth/reset-password`)
   - 입력 필드: 이메일
   - 성공: "비밀번호 재설정 이메일이 발송되었습니다." 메시지 표시

5. **비밀번호 재설정 확인 페이지** (`POST /api/v1/auth/reset-password/confirm`)
   - URL 쿼리 파라미터에서 토큰 추출
   - 입력 필드: 새 비밀번호
   - 유효성 검증: 비밀번호 정책 준수
   - 에러 처리: 이전 비밀번호와 동일
   - 성공: "비밀번호가 성공적으로 변경되었습니다. 로그인해주세요." 메시지 표시

6. **OAuth 로그인 흐름** (`GET /api/v1/auth/oauth2/{provider}`, `GET /api/v1/auth/oauth2/{provider}/callback`)
   - OAuth 로그인 버튼 클릭 시 백엔드 OAuth 시작 엔드포인트로 리다이렉트
   - 콜백 URL에서 토큰 수신
   - Access Token 및 Refresh Token 저장
   - 메인 페이지 이동

#### 2.2. JWT 토큰 관리
1. **Access Token 저장**
   - 저장 위치: LocalStorage 또는 HttpOnly Cookie (XSS 방지)
   - 만료 시간: 3600초(1시간)

2. **Refresh Token 저장**
   - 저장 위치: HttpOnly Cookie (권장) 또는 LocalStorage
   - 만료 시간: 604800초(7일)

3. **토큰 자동 갱신 로직** (`POST /api/v1/auth/refresh`)
   - API 요청 시 401 Unauthorized 응답 감지
   - Refresh Token으로 자동 갱신 요청
   - 새 Access Token 및 Refresh Token 저장
   - 원래 요청 자동 재시도
   - Refresh Token 만료 시: 로그인 페이지로 리다이렉트

4. **로그아웃** (`POST /api/v1/auth/logout`)
   - Refresh Token 무효화 요청
   - 저장된 토큰 삭제 (LocalStorage 또는 Cookie)
   - 로그인 페이지로 리다이렉트

#### 2.3. API 클라이언트 구현
1. **Axios 또는 Fetch 기반 HTTP 클라이언트**
   - Base URL 설정: `http://localhost:8081`
   - 공통 헤더 설정: `Authorization: Bearer {access_token}`
   - 요청 인터셉터: Access Token 자동 주입
   - 응답 인터셉터: 401 Unauthorized 감지 및 토큰 자동 갱신

2. **API 응답 타입 정의**
   - `ApiResponse<T>` 타입 정의 (code, messageCode, message, data)
   - 각 API의 Request/Response DTO 타입 정의

3. **에러 처리**
   - HTTP 상태 코드별 에러 처리 (400, 401, 403, 404, 500, 502, 504)
   - 에러 메시지 표시 (Toast 또는 Alert)

#### 2.4. 라우팅 설계
1. **인증 필요 페이지**
   - 메인 페이지 (로그인 후 이동)
   - 마이페이지 (향후 구현)

2. **인증 불필요 페이지**
   - 로그인 페이지
   - 회원가입 페이지
   - 이메일 인증 페이지
   - 비밀번호 재설정 요청 페이지
   - 비밀번호 재설정 확인 페이지

3. **라우트 가드 (Route Guard)**
   - 인증 필요 페이지 접근 시 토큰 유무 확인
   - 토큰이 없으면 로그인 페이지로 리다이렉트
   - 토큰이 있으면 유효성 검증 (만료 시간 확인)

**필수 포함 내용**:
1. 폴더 구조 및 파일 구성
2. 각 페이지의 UI/UX 플로우 다이어그램
3. JWT 토큰 관리 로직 (저장, 갱신, 삭제)
4. API 클라이언트 구현 가이드
5. 라우팅 설정 및 라우트 가드 구현
6. 에러 처리 전략

**제약사항**:
- 오버엔지니어링 금지: 프로토타입이므로 최소 기능만 구현
- 불필요한 추가 작업 금지: 인증 API 9개 엔드포인트 연동에만 집중
- 장황한 주석 금지: 코드 자체가 설명되도록 작성

---

### 3. @api/auth 모듈 구현 준수

**목표**: 백엔드 @api/auth 모듈의 구현과 완벽히 호환되는 프론트엔드 설계를 제시하세요.

**필수 준수 사항**:
1. **JWT 토큰 형식**
   - Authorization 헤더: `Authorization: Bearer {access_token}`
   - Access Token 만료: 3600초(1시간)
   - Refresh Token 만료: 604800초(7일)

2. **OAuth 2.0 흐름**
   - OAuth 로그인 시작: 백엔드 엔드포인트로 리다이렉트 (`GET /api/v1/auth/oauth2/{provider}`)
   - OAuth 콜백: 백엔드 콜백 엔드포인트가 처리 (`GET /api/v1/auth/oauth2/{provider}/callback`)
   - State 파라미터: 백엔드에서 자동 생성 및 검증 (프론트엔드는 신경 쓰지 않음)

3. **Refresh Token Rotation**
   - 토큰 갱신 시 기존 Refresh Token은 무효화됨
   - 새 Access Token과 새 Refresh Token을 모두 받아서 저장

4. **비밀번호 정책**
   - 최소 길이: 8자
   - 필수 포함: 대소문자/숫자/특수문자 중 2가지 이상

5. **이메일 인증 및 비밀번호 재설정**
   - 이메일로 발송된 링크의 토큰은 24시간 유효
   - 토큰은 일회성 (한 번 사용하면 재사용 불가)

**필수 포함 내용**:
1. JWT 토큰 관리 로직 (저장, 갱신, 삭제)
2. OAuth 2.0 흐름 구현 가이드
3. Refresh Token Rotation 처리
4. 비밀번호 정책 유효성 검증
5. 이메일 인증 및 비밀번호 재설정 플로우

---

### 4. @docs 아래 관련 설계서 참고

**목표**: @docs 폴더의 모든 관련 설계서를 찾아 참고하고, 프론트엔드 설계에 반영하세요.

**참고 문서 목록**:
- `@docs/API-SPECIFICATION.md`: API 명세
- `@docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드
- `@docs/step14/gateway-design.md`: API Gateway 설계
- `@docs/step2/1. api-endpoint-design.md`: API 엔드포인트 설계 (향후 연동 대상)

**필수 포함 내용**:
1. 참고한 문서 목록
2. 각 문서에서 추출한 핵심 정보
3. 프론트엔드 설계에 반영된 내용

---

### 5. 외부 자료는 반드시 신뢰할 수 있는 공식 기술 문서만 참고

**목표**: 프레임워크, 라이브러리 선정 시 반드시 공식 문서만 참고하고, 비공식 블로그나 커뮤니티 글은 참고하지 마세요.

**허용되는 공식 문서**:
- Next.js 공식 문서: https://nextjs.org/docs
- React 공식 문서: https://react.dev/
- Vue 공식 문서: https://vuejs.org/guide/
- TypeScript 공식 문서: https://www.typescriptlang.org/docs/
- Axios 공식 문서: https://axios-http.com/docs/intro
- TanStack Query (React Query) 공식 문서: https://tanstack.com/query/latest
- Zustand 공식 문서: https://zustand-demo.pmnd.rs/
- Tailwind CSS 공식 문서: https://tailwindcss.com/docs

**제약사항**:
- Medium, 개인 블로그, Stack Overflow 등 비공식 자료 참고 금지
- 공식 문서의 URL을 명시하여 출처를 명확히 표기

---

### 6. LLM 오버엔지니어링 금지

**목표**: LLM이 생성하는 불필요한 코드, 추가 작업, 장황한 주석을 제거하고, 실용적인 설계서를 작성하세요.

**금지 사항**:
1. **오버엔지니어링**
   - 프로토타입에 불필요한 고급 패턴 (예: Clean Architecture, Hexagonal Architecture)
   - 과도한 추상화 (예: 모든 기능을 인터페이스로 분리)
   - 사용하지 않는 라이브러리 추가

2. **불필요한 추가 작업**
   - 요구사항에 없는 기능 추가 (예: 다국어 지원, 다크 모드, 테마 변경)
   - 인증 API 9개 엔드포인트 외의 API 연동

3. **장황한 주석**
   - "사용자가 로그인 버튼을 클릭하면..." 형식의 주석
   - 코드를 설명하는 주석보다 코드 자체가 설명되도록 작성
   - 주석은 "왜"에 집중 (예: "Refresh Token Rotation 전략 적용")

**권장 사항**:
1. **단순하고 명확한 코드**
   - 함수명, 변수명이 명확하여 주석 없이도 이해 가능
   - 한 함수는 한 가지 일만 수행 (Single Responsibility)

2. **실용적인 설계**
   - 검증된 라이브러리 사용 (Axios, React Query, Zustand 등)
   - 프로토타입에 적합한 간단한 폴더 구조

3. **주석은 최소화**
   - JSDoc으로 타입 정보만 명시
   - 복잡한 로직에만 "왜"를 설명하는 주석 추가

---

### 7. 모든 설계의 코드 구현은 클린코드 원칙 준수

**목표**: 클린코드 원칙, SOLID 원칙, 프론트엔드 베스트프랙티스를 준수하는 설계를 제시하세요.

**클린코드 원칙**:
1. **명확한 네이밍**
   - 변수명: `accessToken`, `refreshToken`, `isLoading`, `errorMessage`
   - 함수명: `login()`, `logout()`, `refreshToken()`, `validatePassword()`

2. **함수는 한 가지 일만 수행**
   - `login()`: 로그인 API 호출 및 토큰 저장
   - `logout()`: 로그아웃 API 호출 및 토큰 삭제
   - `refreshToken()`: 토큰 갱신 API 호출 및 새 토큰 저장

3. **에러 처리**
   - try-catch 블록으로 에러 처리
   - 에러 메시지는 사용자에게 표시 (Toast 또는 Alert)

**SOLID 원칙**:
1. **단일 책임 원칙 (SRP)**
   - API 클라이언트는 HTTP 요청만 처리
   - 토큰 관리는 별도 모듈로 분리 (`TokenService`)

2. **개방-폐쇄 원칙 (OCP)**
   - API 클라이언트는 확장에는 열려 있고 수정에는 닫혀 있음
   - 새로운 API 추가 시 기존 코드 수정 없이 확장 가능

3. **인터페이스 분리 원칙 (ISP)**
   - 각 API 서비스는 독립적인 인터페이스 제공
   - `AuthService`, `ArchiveService`, `ContestService` 등

**프론트엔드 베스트프랙티스**:
1. **컴포넌트 설계**
   - Presentational 컴포넌트 vs Container 컴포넌트 분리
   - 재사용 가능한 컴포넌트 설계 (Button, Input, Form 등)

2. **상태 관리**
   - 전역 상태 (인증 상태, 사용자 정보)
   - 로컬 상태 (폼 입력 값, UI 상태)

3. **비동기 처리**
   - React Query 또는 SWR 사용 (API 요청 캐싱, 자동 재시도)
   - Loading, Error, Success 상태 관리

4. **보안**
   - XSS 방지: HttpOnly Cookie 사용 (Refresh Token)
   - CSRF 방지: SameSite Cookie 속성 설정

**필수 포함 내용**:
1. 폴더 구조 및 모듈 분리 (API 클라이언트, 토큰 관리, 상태 관리)
2. 컴포넌트 설계 (Presentational vs Container)
3. 에러 처리 전략
4. 보안 고려사항 (XSS, CSRF 방지)

---

## 제약사항

### 1. 프롬프트 엔지니어링 기법

다음 프롬프트 엔지니어링 기법을 사용하여 설계서를 작성하세요:

1. **Few-Shot Prompting**
   - 예시 코드 제공: API 클라이언트 구현 예시, 토큰 관리 로직 예시
   - 예시 폴더 구조 제공: Next.js, React, Vue 각각의 폴더 구조

2. **Chain-of-Thought Prompting**
   - 설계 의사결정 과정을 명확히 설명
   - 프레임워크 선정 근거, 라이브러리 선정 근거

3. **Role Prompting**
   - "당신은 프론트엔드 아키텍트입니다."
   - "당신은 Next.js/React/Vue 전문가입니다."

4. **Constraint Prompting**
   - 명확한 제약사항 제시 (오버엔지니어링 금지, 공식 문서만 참고)
   - 요구사항 명확히 정의 (9개 인증 API 우선 연동)

### 2. 출력 형식

설계서는 다음 구조로 작성하세요:

```markdown
# 프론트엔드 웹앱 개발 설계서

**작성 일시**: YYYY-MM-DD  
**대상**: 프론트엔드 개발팀  
**버전**: v1

## 목차

1. [개요](#개요)
2. [프레임워크 선택 및 기술 스택](#프레임워크-선택-및-기술-스택)
3. [프로토타입 설계 (9개 인증 API 우선 연동)](#프로토타입-설계-9개-인증-api-우선-연동)
4. [JWT 토큰 관리](#jwt-토큰-관리)
5. [API 클라이언트 구현](#api-클라이언트-구현)
6. [폴더 구조 및 파일 구성](#폴더-구조-및-파일-구성)
7. [라우팅 설계](#라우팅-설계)
8. [에러 처리 전략](#에러-처리-전략)
9. [보안 고려사항](#보안-고려사항)
10. [구현 가이드](#구현-가이드)
11. [참고 자료](#참고-자료)

---

## 1. 개요

### 1.1 프로젝트 배경
...

### 1.2 설계서 목적
...

### 1.3 기술 스택 개요
...

---

## 2. 프레임워크 선택 및 기술 스택

### 2.1 프레임워크 비교

| 항목 | Next.js | React | Vue |
|-----|---------|-------|-----|
| SSR 지원 | ✅ | ❌ | ✅ (Nuxt.js) |
| TypeScript 지원 | ✅ | ✅ | ✅ |
| ... | ... | ... | ... |

### 2.2 선정된 프레임워크: [프레임워크명]

**선정 근거**:
1. ...
2. ...

### 2.3 핵심 라이브러리

1. **API 클라이언트**: Axios (또는 Fetch)
2. **상태 관리**: Zustand (또는 Redux Toolkit, Pinia)
3. **API 요청 관리**: TanStack Query (React Query) 또는 SWR
4. **폼 관리**: React Hook Form (또는 Formik, VeeValidate)
5. **UI 프레임워크**: Tailwind CSS (또는 Material-UI, Vuetify)
6. **라우팅**: Next.js 내장 라우팅 (또는 React Router, Vue Router)

---

## 3. 프로토타입 설계 (9개 인증 API 우선 연동)

### 3.1 페이지 목록

1. **회원가입 페이지** (`/signup`)
2. **로그인 페이지** (`/login`)
3. **이메일 인증 페이지** (`/verify-email`)
4. **비밀번호 재설정 요청 페이지** (`/reset-password`)
5. **비밀번호 재설정 확인 페이지** (`/reset-password/confirm`)
6. **OAuth 콜백 페이지** (`/auth/callback/{provider}`)
7. **메인 페이지** (`/`) - 로그인 후 이동

### 3.2 UI/UX 플로우 다이어그램

(회원가입, 로그인, OAuth 로그인 등의 플로우 다이어그램 포함)

---

## 4. JWT 토큰 관리

### 4.1 토큰 저장 위치

- **Access Token**: LocalStorage 또는 HttpOnly Cookie
- **Refresh Token**: HttpOnly Cookie (권장)

### 4.2 토큰 자동 갱신 로직

(상세 구현 가이드 포함)

---

## 5. API 클라이언트 구현

### 5.1 Axios 설정

(Axios 인터셉터 구현 예시 포함)

### 5.2 API 응답 타입 정의

(TypeScript 타입 정의 예시 포함)

---

## 6. 폴더 구조 및 파일 구성

```
src/
├── pages/              # 페이지 컴포넌트 (Next.js) 또는 views/ (Vue)
├── components/         # 재사용 가능한 컴포넌트
├── services/           # API 클라이언트
├── hooks/              # Custom Hooks (React) 또는 composables/ (Vue)
├── stores/             # 상태 관리 (Zustand, Redux, Pinia)
├── utils/              # 유틸리티 함수
├── types/              # TypeScript 타입 정의
└── styles/             # 스타일 파일
```

---

## 7. 라우팅 설계

### 7.1 라우트 목록

...

### 7.2 라우트 가드 (Route Guard)

...

---

## 8. 에러 처리 전략

### 8.1 HTTP 상태 코드별 에러 처리

...

### 8.2 에러 메시지 표시

...

---

## 9. 보안 고려사항

### 9.1 XSS 방지

...

### 9.2 CSRF 방지

...

---

## 10. 구현 가이드

### 10.1 개발 환경 설정

...

### 10.2 구현 단계

1. ...
2. ...

---

## 11. 참고 자료

### 11.1 공식 문서

- Next.js 공식 문서: https://nextjs.org/docs
- ...

### 11.2 프로젝트 내 참고 문서

- @docs/API-SPECIFICATION.md
- ...

---

**작성 완료일**: YYYY-MM-DD  
**검토 필요**: 프레임워크 선택, JWT 토큰 관리, API 클라이언트 구현
```

---

## 지시사항

위 요구사항과 제약사항을 모두 준수하여 프론트엔드 웹앱 개발 설계서를 작성하세요. 설계서는 반드시 다음을 포함해야 합니다:

1. ✅ 프레임워크 비교표 및 선정 근거 (Next.js, React, Vue)
2. ✅ 9개 인증 API 엔드포인트 우선 연동을 위한 프로토타입 설계
3. ✅ JWT 토큰 관리 로직 (저장, 갱신, 삭제)
4. ✅ API 클라이언트 구현 가이드 (Axios 인터셉터 포함)
5. ✅ 폴더 구조 및 파일 구성
6. ✅ 라우팅 설계 및 라우트 가드
7. ✅ 에러 처리 전략
8. ✅ 보안 고려사항 (XSS, CSRF 방지)
9. ✅ 구현 가이드 (개발 환경 설정, 구현 단계)
10. ✅ 공식 문서 참고 자료 목록

**중요**: 설계서 작성 시 LLM 오버엔지니어링, 불필요한 추가 작업, 장황한 주석을 절대 포함하지 마세요. 클린코드 원칙, SOLID 원칙, 프론트엔드 베스트프랙티스를 준수하는 실용적인 설계서를 작성하세요.
