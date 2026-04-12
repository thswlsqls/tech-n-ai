# Spring Security 설계 및 사용자 인증/인가 로직 구현 활용방법 프롬프트

## 역할 및 목표 설정

당신은 **Spring Security 아키텍트 및 인증/인가 시스템 설계 전문가**입니다. `api/auth` 모듈과 `common/security` 모듈에 구현된 코드를 분석하여, Spring Security 설계와 사용자 인증/인가 로직 구현을 위한 활용방법에 대한 기술 문서를 작성해야 합니다.

## 작업 범위

다음 작업을 수행하여 `docs/spring-security-auth-design-guide.md` 파일을 생성하세요:

1. **현재 구현 코드 분석**
   - `api/auth` 모듈의 인증 로직 분석
   - `common/security` 모듈의 Spring Security 설정 분석
   - JWT 토큰 기반 인증 필터 분석
   - 각 컴포넌트의 역할과 책임 분석

2. **Spring Security 공식 문서 기반 설계 원칙 분석**
   - SecurityFilterChain 설정 원칙
   - JWT 인증 필터 구현 패턴
   - PasswordEncoder 선택 및 설정
   - CORS 설정 베스트 프랙티스

3. **인증/인가 로직 구현 패턴 분석**
   - 회원가입/로그인 플로우
   - JWT 토큰 생성/검증/갱신 메커니즘
   - Refresh Token 관리 전략
   - 이메일 인증 및 비밀번호 재설정 플로우

4. **실제 코드 활용 방법 가이드**
   - 각 컴포넌트의 사용 방법
   - 새로운 엔드포인트에 인증 적용 방법
   - 권한 기반 접근 제어 구현 방법
   - 보안 설정 커스터마이징 방법

## 분석 전략 (Chain of Thought)

### 1단계: 현재 구현 코드 분석

**분석 대상 파일:**

1. **common/security 모듈:**
   - `SecurityConfig.java`: Spring Security 필터 체인 설정
   - `JwtAuthenticationFilter.java`: JWT 토큰 기반 인증 필터
   - `JwtTokenProvider.java`: JWT 토큰 생성/검증/파싱
   - `JwtTokenPayload.java`: JWT 토큰 페이로드 레코드
   - `PasswordEncoderConfig.java`: BCryptPasswordEncoder 설정

2. **api/auth 모듈:**
   - `AuthController.java`: 인증 API 엔드포인트
   - `AuthService.java`: 인증 비즈니스 로직
   - `AuthFacade.java`: Controller-Service 중간 계층
   - `RefreshTokenService.java`: Refresh Token 관리

**분석 항목:**
- 각 클래스의 역할과 책임
- 컴포넌트 간 의존성 관계
- 데이터 흐름 및 처리 순서
- 보안 관련 설정 및 검증 로직

### 2단계: Spring Security 공식 문서 기반 설계 원칙 분석

**신뢰할 수 있는 공식 기술 문서만 참고:**

- Spring Security 공식 문서: https://docs.spring.io/spring-security/reference/
- Spring Security Servlet 설정: https://docs.spring.io/spring-security/reference/servlet/configuration/java.html
- Spring Security 인증 필터: https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html
- PasswordEncoder 공식 문서: https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
- JWT 공식 스펙 (RFC 7519): https://tools.ietf.org/html/rfc7519
- jjwt 라이브러리 공식 문서: https://github.com/jwtk/jjwt

**검증 원칙:**
- 공식 문서의 예제 코드만 인용
- 최신 버전 정보 확인 (Spring Security 6.x 기준)
- 공식 문서에 없는 내용은 추측하지 않음
- 불확실한 내용은 명시적으로 표시
- 오버엔지니어링 방지: 필요한 기능만 구현, 불필요한 복잡도 제거

**분석 항목:**

1. **SecurityFilterChain 설정 원칙**
   - `@EnableWebSecurity` 어노테이션의 역할
   - `SecurityFilterChain` 빈 등록 방법
   - CSRF 비활성화 조건 (REST API)
   - Session 정책 설정 (STATELESS)
   - 인증/인가 규칙 설정 (`authorizeHttpRequests`)
   - 커스텀 필터 추가 방법 (`addFilterBefore`)

2. **JWT 인증 필터 구현 패턴**
   - `OncePerRequestFilter` 상속 이유
   - `Authorization` 헤더에서 토큰 추출
   - 토큰 검증 및 `SecurityContext` 설정
   - 인증 실패 시 에러 응답 처리
   - 필터 체인에서의 위치 및 순서

3. **PasswordEncoder 선택 및 설정**
   - BCryptPasswordEncoder 선택 이유
   - Salt rounds 설정 (12 권장)
   - 비밀번호 해시 생성 및 검증 방법
   - 공식 문서 권장 사항

4. **CORS 설정 베스트 프랙티스**
   - `allowCredentials`와 `allowedOrigins` 제약사항
   - 개발/운영 환경별 설정 차이
   - 보안 고려사항

### 3단계: 인증/인가 로직 구현 패턴 분석

**분석 항목:**

1. **회원가입 플로우**
   - 이메일/사용자명 중복 검증
   - 비밀번호 해시 생성 및 저장
   - 이메일 인증 토큰 생성
   - Kafka 이벤트 발행

2. **로그인 플로우**
   - 사용자 조회 및 Soft Delete 확인
   - 비밀번호 검증
   - 이메일 인증 여부 확인
   - JWT Access/Refresh Token 생성
   - Refresh Token 저장
   - 마지막 로그인 시간 업데이트

3. **JWT 토큰 생성/검증/갱신 메커니즘**
   - Access Token 생성 (짧은 유효기간)
   - Refresh Token 생성 (긴 유효기간)
   - 토큰 페이로드 구조 (userId, email, role)
   - 토큰 검증 로직
   - 토큰에서 페이로드 추출

4. **Refresh Token 관리 전략**
   - Refresh Token 저장 (데이터베이스)
   - Refresh Token 검증 (유효성, 만료시간, Soft Delete 확인)
   - 토큰 갱신 시 기존 토큰 무효화 (Soft Delete)
   - 새로운 Access/Refresh Token 발급

5. **이메일 인증 및 비밀번호 재설정 플로우**
   - 이메일 인증 토큰 생성 및 검증
   - 비밀번호 재설정 토큰 생성 및 검증
   - 토큰 재사용 방지 (verifiedAt 필드 활용)
   - 기존 토큰 무효화 전략

6. **OAuth 로그인 플로우 (기본 구조)**
   - OAuth Provider 설정 조회
   - OAuth 인증 URL 생성
   - OAuth 콜백 처리
   - 사용자 조회/생성 및 JWT 토큰 발급

### 4단계: 실제 코드 활용 방법 가이드

**가이드 항목:**

1. **각 컴포넌트의 사용 방법**
   - `SecurityConfig`: 새로운 엔드포인트에 인증 규칙 추가
   - `JwtAuthenticationFilter`: 필터 동작 원리 및 커스터마이징
   - `JwtTokenProvider`: 토큰 생성/검증/파싱 사용법
   - `PasswordEncoder`: 비밀번호 해시 생성/검증 사용법

2. **새로운 엔드포인트에 인증 적용 방법**
   - `@PreAuthorize` 어노테이션 사용
   - `SecurityConfig`에서 경로별 인증 규칙 설정
   - `Authentication` 객체에서 사용자 정보 추출

3. **권한 기반 접근 제어 구현 방법**
   - 역할(Role) 기반 접근 제어
   - `SecurityConfig`에서 역할별 경로 설정
   - `@PreAuthorize("hasRole('ADMIN')")` 사용법

4. **보안 설정 커스터마이징 방법**
   - CORS 설정 변경
   - 세션 정책 변경
   - 추가 필터 추가
   - 에러 핸들링 커스터마이징

## 출력 형식 및 구조

다음 구조로 `docs/spring-security-auth-design-guide.md` 파일을 생성하세요:

```markdown
# Spring Security 설계 및 사용자 인증/인가 로직 구현 활용방법

## 목차
1. 개요
2. 아키텍처 개요
3. Spring Security 설정 분석
4. JWT 인증 필터 구현 분석
5. 인증/인가 로직 구현 패턴
6. 실제 코드 활용 방법
7. 보안 고려사항
8. 참고 자료

## 1. 개요
- 프로젝트의 인증/인가 시스템 개요
- 사용 기술 스택
- 주요 컴포넌트 소개

## 2. 아키텍처 개요
- 전체 인증/인가 플로우 다이어그램
- 컴포넌트 간 의존성 관계
- 데이터 흐름

## 3. Spring Security 설정 분석
- SecurityConfig 분석
- PasswordEncoderConfig 분석
- CORS 설정 분석
- 공식 문서 기반 베스트 프랙티스

## 4. JWT 인증 필터 구현 분석
- JwtAuthenticationFilter 동작 원리
- JwtTokenProvider 구현 분석
- SecurityContext 설정 방법
- 공식 문서 기반 구현 패턴

## 5. 인증/인가 로직 구현 패턴
- 회원가입 플로우
- 로그인 플로우
- JWT 토큰 생성/검증/갱신
- Refresh Token 관리
- 이메일 인증 및 비밀번호 재설정
- OAuth 로그인 (기본 구조)

## 6. 실제 코드 활용 방법
- 새로운 엔드포인트에 인증 적용
- 권한 기반 접근 제어 구현
- 보안 설정 커스터마이징
- 코드 예제

## 7. 보안 고려사항
- 토큰 보안 관리
- 비밀번호 보안
- CORS 보안
- 추가 보안 권장사항

## 8. 참고 자료
- Spring Security 공식 문서 링크
- JWT 공식 스펙 링크
- 관련 라이브러리 문서 링크
```

## 제약 조건 및 주의사항

### 1. 오버엔지니어링 방지
- **필요한 기능만 설명**: 현재 구현된 기능에 집중
- **불필요한 복잡도 제거**: 과도한 추상화나 패턴 설명 지양
- **실용성 우선**: 이론보다 실제 사용 방법 중심
- **간결한 설명**: 핵심만 명확하게 전달

### 2. 신뢰할 수 있는 공식 기술 문서만 참고
- **Spring Security 공식 문서 우선**: https://docs.spring.io/spring-security/reference/
- **JWT 공식 스펙 (RFC 7519)**: https://tools.ietf.org/html/rfc7519
- **jjwt 라이브러리 공식 문서**: https://github.com/jwtk/jjwt
- **공식 문서에 없는 내용은 추측하지 않음**
- **불확실한 내용은 명시적으로 표시**

### 3. 코드 인용 규칙
- **실제 구현 코드 인용**: `api/auth` 및 `common/security` 모듈의 실제 코드 사용
- **코드 참조 형식**: 파일 경로와 라인 번호 명시
- **공식 문서 예제 코드 인용**: Spring Security 공식 문서의 예제 코드 사용
- **코드 설명**: 각 코드 블록에 대한 명확한 설명 제공

### 4. 프롬프트 엔지니어링 기법 적용
- **Role-Based Prompting**: Spring Security 아키텍트 역할 부여
- **Chain of Thought**: 단계별 분석 전략 명시
- **Context Setting**: 프로젝트 배경 및 기술 스택 명시
- **Structured Output**: 명확한 문서 구조 정의
- **Evaluation Criteria**: 공식 문서 기반 검증 기준

## 검증 기준

다음 기준을 만족하는 문서를 작성하세요:

1. **정확성 (Accuracy)**
   - 공식 문서 기반 정확한 정보 제공
   - 실제 구현 코드와 일치하는 설명
   - 오류나 추측 내용 없음

2. **완전성 (Completeness)**
   - 모든 주요 컴포넌트 설명 포함
   - 인증/인가 플로우 전체 설명
   - 실제 사용 방법 가이드 포함

3. **실용성 (Practicality)**
   - 실제 코드 예제 포함
   - 단계별 구현 가이드 제공
   - 커스터마이징 방법 설명

4. **간결성 (Conciseness)**
   - 불필요한 설명 제거
   - 핵심 내용만 명확하게 전달
   - 오버엔지니어링 방지

5. **참조 가능성 (Referenceability)**
   - 공식 문서 링크 제공
   - 코드 참조 경로 명시
   - 추가 학습 자료 제공

## 실행 지시사항

1. **코드 분석**: `api/auth` 및 `common/security` 모듈의 모든 관련 파일을 읽고 분석하세요.

2. **공식 문서 참조**: Spring Security 공식 문서를 참조하여 현재 구현이 공식 권장 사항을 따르는지 확인하세요.

3. **문서 작성**: 위의 출력 형식에 따라 `docs/spring-security-auth-design-guide.md` 파일을 작성하세요.

4. **코드 인용**: 실제 구현 코드를 인용할 때는 파일 경로와 라인 번호를 명시하세요.

5. **검증**: 작성한 문서가 검증 기준을 만족하는지 확인하세요.

## 추가 참고사항

- 현재 구현은 Spring Security 6.x 기반입니다.
- JWT 토큰은 jjwt 라이브러리를 사용합니다.
- 비밀번호 인코딩은 BCryptPasswordEncoder를 사용합니다.
- CQRS 패턴을 사용하여 Reader/Writer Repository를 분리했습니다.
- Soft Delete 패턴을 사용하여 데이터를 삭제합니다.
- Kafka를 사용하여 이벤트를 발행합니다.

---

**중요**: 이 프롬프트는 실제 구현 코드를 분석하여 실용적인 가이드를 작성하는 것이 목적입니다. 이론적 설명보다는 실제 코드 활용 방법에 집중하세요.
