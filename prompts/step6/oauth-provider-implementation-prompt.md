# OAuth Provider별 로그인 기능 구현 설계 가이드 프롬프트

## 역할 및 목표 설정

당신은 **OAuth 2.0 인증 시스템 아키텍트 및 구현 전문가**입니다. `api/auth` 모듈과 `common/security` 모듈에 구현된 기존 인증 시스템을 분석하여, Google, Naver, Kakao OAuth 로그인 기능을 구현하기 위한 설계 가이드를 작성해야 합니다.

## 작업 범위

다음 작업을 수행하여 `docs/oauth-provider-implementation-guide.md` 파일을 생성하세요:

1. **현재 OAuth 구현 코드 분석**
   - `api/auth` 모듈의 OAuth 관련 코드 분석
   - `ProviderEntity` 구조 분석
   - `AuthService.startOAuthLogin()` 및 `handleOAuthCallback()` 메서드 분석
   - 기존 설계 가이드 문서와의 정합성 확인

2. **각 OAuth Provider 공식 문서 기반 분석**
   - Google OAuth 2.0 공식 개발자 가이드 분석
   - Naver OAuth 2.0 공식 개발자 가이드 분석
   - Kakao OAuth 2.0 공식 개발자 가이드 분석
   - 각 Provider별 인증 플로우 및 API 스펙 분석

3. **OAuth Provider별 구현 설계**
   - Google OAuth 구현 설계
   - Naver OAuth 구현 설계
   - Kakao OAuth 구현 설계
   - 공통 인터페이스 및 Provider별 차이점 분석

4. **실제 구현 가이드**
   - 각 Provider별 구현 단계별 가이드
   - 코드 예제 및 통합 방법
   - 보안 고려사항 및 베스트 프랙티스

## 분석 전략 (Chain of Thought)

### 1단계: 현재 구현 코드 분석

**분석 대상 파일:**

1. **api/auth 모듈:**
   - `AuthController.java`: OAuth 엔드포인트 (`/oauth2/{provider}`, `/oauth2/{provider}/callback`)
   - `AuthService.java`: `startOAuthLogin()`, `handleOAuthCallback()` 메서드
   - `AuthFacade.java`: OAuth 관련 Facade 메서드

2. **domain 모듈:**
   - `ProviderEntity.java`: OAuth Provider 엔티티 구조
   - `UserEntity.java`: User 엔티티의 OAuth 관련 필드

3. **기존 설계 가이드 문서:**
   - `docs/spring-security-auth-design-guide.md`: 기존 인증 시스템 설계
   - `docs/phase1/3. aurora-schema-design.md`: Provider 테이블 스키마 설계
   - `docs/phase2/2. data-model-design.md`: 데이터 모델 설계

**분석 항목:**
- 현재 OAuth 구현의 구조 및 플로우
- ProviderEntity의 필드 구조 및 용도
- OAuth 인증 URL 생성 방식
- OAuth 콜백 처리 로직
- 사용자 조회/생성 전략
- 기존 설계 가이드와의 정합성

### 2단계: OAuth Provider 공식 문서 기반 분석

**신뢰할 수 있는 공식 기술 문서만 참고:**

- **Google OAuth 2.0**:
  - Google OAuth 2.0 공식 문서: https://developers.google.com/identity/protocols/oauth2
  - Google OAuth 2.0 for Web Server Applications: https://developers.google.com/identity/protocols/oauth2/web-server
  - Google Identity Platform: https://developers.google.com/identity

- **Naver OAuth 2.0**:
  - Naver 개발자 센터: https://developers.naver.com/
  - Naver OAuth 2.0 API 명세: https://developers.naver.com/docs/login/overview/
  - Naver 로그인 API 가이드: https://developers.naver.com/docs/login/api/

- **Kakao OAuth 2.0**:
  - Kakao Developers: https://developers.kakao.com/
  - Kakao 로그인 REST API: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
  - Kakao 로그인 개요: https://developers.kakao.com/docs/latest/ko/kakaologin/common

- **OAuth 2.0 표준**:
  - RFC 6749 (OAuth 2.0 Authorization Framework): https://tools.ietf.org/html/rfc6749
  - RFC 7636 (Proof Key for Code Exchange): https://tools.ietf.org/html/rfc7636

**검증 원칙:**
- 공식 문서의 예제 코드만 인용
- 최신 버전 정보 확인
- 공식 문서에 없는 내용은 추측하지 않음
- 불확실한 내용은 명시적으로 표시
- 오버엔지니어링 방지: 필요한 기능만 구현, 불필요한 복잡도 제거

**분석 항목:**

1. **OAuth 2.0 인증 플로우**
   - Authorization Code Flow 분석
   - 각 Provider별 인증 URL 형식
   - Redirect URI 설정 방법
   - State 파라미터 사용 방법 (CSRF 방지)

2. **Access Token 교환**
   - Authorization Code로 Access Token 교환 API
   - 각 Provider별 API 엔드포인트 및 요청 형식
   - 응답 형식 및 토큰 구조

3. **사용자 정보 조회**
   - 각 Provider별 사용자 정보 API
   - API 엔드포인트 및 요청 형식
   - 응답 형식 및 필드 매핑

4. **Provider별 차이점**
   - 인증 URL 파라미터 차이
   - 토큰 교환 방식 차이
   - 사용자 정보 응답 형식 차이
   - 에러 처리 방식 차이

### 3단계: OAuth Provider별 구현 설계

**설계 항목:**

1. **공통 인터페이스 설계**
   - OAuth Provider 공통 인터페이스 정의
   - Provider별 구현 클래스 구조
   - Factory 패턴 적용 여부 검토

2. **Google OAuth 구현 설계**
   - Google OAuth 인증 URL 생성
   - Google Access Token 교환
   - Google 사용자 정보 조회
   - Google 특화 처리 사항

3. **Naver OAuth 구현 설계**
   - Naver OAuth 인증 URL 생성
   - Naver Access Token 교환
   - Naver 사용자 정보 조회
   - Naver 특화 처리 사항

4. **Kakao OAuth 구현 설계**
   - Kakao OAuth 인증 URL 생성
   - Kakao Access Token 교환
   - Kakao 사용자 정보 조회
   - Kakao 특화 처리 사항

5. **통합 전략**
   - 기존 `AuthService`와의 통합 방법
   - ProviderEntity 활용 방법
   - 사용자 조회/생성 전략 통일
   - JWT 토큰 발급 통합

### 4단계: 실제 구현 가이드

**가이드 항목:**

1. **설정 및 준비**
   - 각 Provider별 개발자 콘솔 설정 방법
   - Client ID 및 Client Secret 발급 방법
   - Redirect URI 설정 방법
   - ProviderEntity 데이터 준비

2. **의존성 추가**
   - HTTP Client 라이브러리 선택 (예: Spring WebClient, RestTemplate)
   - JSON 파싱 라이브러리 (Jackson)
   - 필요한 추가 의존성

3. **구현 단계별 가이드**
   - Step 1: OAuth Provider 인터페이스 정의
   - Step 2: Google OAuth 구현
   - Step 3: Naver OAuth 구현
   - Step 4: Kakao OAuth 구현
   - Step 5: AuthService 통합
   - Step 6: 테스트 및 검증

4. **코드 예제**
   - 각 Provider별 구현 코드 예제
   - 통합 코드 예제
   - 에러 처리 예제

5. **보안 고려사항**
   - Client Secret 관리 방법
   - State 파라미터 검증
   - 토큰 저장 및 관리
   - HTTPS 사용 필수

## 출력 형식 및 구조

다음 구조로 `docs/oauth-provider-implementation-guide.md` 파일을 생성하세요:

```markdown
# OAuth Provider별 로그인 기능 구현 설계 가이드

## 목차
1. 개요
2. 현재 구현 분석
3. OAuth 2.0 표준 및 각 Provider 분석
4. OAuth Provider별 구현 설계
5. 실제 구현 가이드
6. 보안 고려사항
7. 참고 자료

## 1. 개요
- OAuth 2.0 개요
- 지원 Provider 목록
- 기존 인증 시스템과의 통합 전략

## 2. 현재 구현 분석
- 기존 OAuth 관련 코드 분석
- ProviderEntity 구조 분석
- AuthService OAuth 메서드 분석
- 기존 설계 가이드와의 정합성

## 3. OAuth 2.0 표준 및 각 Provider 분석
- OAuth 2.0 Authorization Code Flow
- Google OAuth 2.0 분석
- Naver OAuth 2.0 분석
- Kakao OAuth 2.0 분석
- Provider별 차이점 비교

## 4. OAuth Provider별 구현 설계
- 공통 인터페이스 설계
- Google OAuth 구현 설계
- Naver OAuth 구현 설계
- Kakao OAuth 구현 설계
- 통합 전략

## 5. 실제 구현 가이드
- 설정 및 준비
- 의존성 추가
- 구현 단계별 가이드
- 코드 예제
- 테스트 방법

## 6. 보안 고려사항
- Client Secret 관리
- State 파라미터 검증
- 토큰 관리
- HTTPS 사용

## 7. 참고 자료
- 각 Provider 공식 문서 링크
- OAuth 2.0 표준 문서 링크
- 관련 라이브러리 문서 링크
```

## 제약 조건 및 주의사항

### 1. 오버엔지니어링 방지
- **필요한 기능만 설명**: 각 Provider의 기본 OAuth 로그인 기능에 집중
- **불필요한 복잡도 제거**: 과도한 추상화나 패턴 설명 지양
- **실용성 우선**: 이론보다 실제 구현 방법 중심
- **간결한 설명**: 핵심만 명확하게 전달

### 2. 신뢰할 수 있는 공식 기술 문서만 참고
- **각 Provider 공식 개발자 문서 우선**: Google, Naver, Kakao 공식 문서만 참고
- **OAuth 2.0 표준 문서**: RFC 6749, RFC 7636 등 공식 표준 문서
- **공식 문서에 없는 내용은 추측하지 않음**
- **불확실한 내용은 명시적으로 표시**
- **블로그나 비공식 문서는 참고하지 않음**

### 3. 기존 설계 가이드와의 정합성 유지
- **기존 설계 가이드 문서 참조**: `docs/spring-security-auth-design-guide.md`와 일관성 유지
- **기존 코드 구조 준수**: `api/auth` 모듈의 기존 구조와 패턴 유지
- **ProviderEntity 활용**: 기존 데이터 모델 설계 준수
- **JWT 토큰 발급 통합**: 기존 JWT 토큰 발급 로직 재사용

### 4. 코드 인용 규칙
- **실제 구현 코드 인용**: `api/auth` 모듈의 실제 코드 사용
- **코드 참조 형식**: 파일 경로와 라인 번호 명시
- **공식 문서 예제 코드 인용**: 각 Provider 공식 문서의 예제 코드 사용
- **코드 설명**: 각 코드 블록에 대한 명확한 설명 제공

### 5. 프롬프트 엔지니어링 기법 적용
- **Role-Based Prompting**: OAuth 2.0 인증 시스템 아키텍트 역할 부여
- **Chain of Thought**: 단계별 분석 전략 명시
- **Context Setting**: 프로젝트 배경 및 기존 인증 시스템 명시
- **Structured Output**: 명확한 문서 구조 정의
- **Evaluation Criteria**: 공식 문서 기반 검증 기준

## 검증 기준

다음 기준을 만족하는 문서를 작성하세요:

1. **정확성 (Accuracy)**
   - 공식 문서 기반 정확한 정보 제공
   - 실제 구현 코드와 일치하는 설명
   - 오류나 추측 내용 없음
   - 각 Provider별 정확한 API 스펙 반영

2. **완전성 (Completeness)**
   - 모든 주요 컴포넌트 설명 포함
   - 각 Provider별 전체 플로우 설명
   - 실제 구현 방법 가이드 포함
   - 기존 설계 가이드와의 통합 방법 설명

3. **실용성 (Practicality)**
   - 실제 코드 예제 포함
   - 단계별 구현 가이드 제공
   - 각 Provider별 구체적인 설정 방법
   - 통합 및 테스트 방법 설명

4. **간결성 (Conciseness)**
   - 불필요한 설명 제거
   - 핵심 내용만 명확하게 전달
   - 오버엔지니어링 방지
   - Provider별 차이점만 명확히 구분

5. **참조 가능성 (Referenceability)**
   - 각 Provider 공식 문서 링크 제공
   - OAuth 2.0 표준 문서 링크
   - 코드 참조 경로 명시
   - 추가 학습 자료 제공

## 실행 지시사항

1. **코드 분석**: `api/auth` 모듈의 OAuth 관련 코드를 읽고 분석하세요.

2. **기존 설계 가이드 확인**: `docs/spring-security-auth-design-guide.md` 및 관련 설계 문서를 확인하여 정합성을 유지하세요.

3. **공식 문서 참조**: 각 Provider의 공식 개발자 문서를 참조하여 정확한 API 스펙을 확인하세요.

4. **문서 작성**: 위의 출력 형식에 따라 `docs/oauth-provider-implementation-guide.md` 파일을 작성하세요.

5. **코드 인용**: 실제 구현 코드를 인용할 때는 파일 경로와 라인 번호를 명시하세요.

6. **검증**: 작성한 문서가 검증 기준을 만족하는지 확인하세요.

## 추가 참고사항

- 현재 구현은 Spring Security 6.x 기반입니다.
- JWT 토큰은 jjwt 라이브러리를 사용합니다.
- HTTP Client는 Spring WebClient 또는 RestTemplate을 사용할 수 있습니다.
- CQRS 패턴을 사용하여 Reader/Writer Repository를 분리했습니다.
- Soft Delete 패턴을 사용하여 데이터를 삭제합니다.
- Kafka를 사용하여 이벤트를 발행합니다.
- ProviderEntity는 이미 데이터베이스에 정의되어 있습니다.

## 각 Provider별 필수 분석 항목

### Google OAuth 2.0
- Authorization Endpoint: `https://accounts.google.com/o/oauth2/v2/auth`
- Token Endpoint: `https://oauth2.googleapis.com/token`
- UserInfo Endpoint: `https://www.googleapis.com/oauth2/v2/userinfo`
- 필수 파라미터: `client_id`, `redirect_uri`, `response_type`, `scope`, `state`
- 사용자 정보 필드: `id`, `email`, `name`, `picture` 등

### Naver OAuth 2.0
- Authorization Endpoint: `https://nid.naver.com/oauth2.0/authorize`
- Token Endpoint: `https://nid.naver.com/oauth2.0/token`
- UserInfo Endpoint: `https://openapi.naver.com/v1/nid/me`
- 필수 파라미터: `client_id`, `redirect_uri`, `response_type`, `state`
- 사용자 정보 필드: `id`, `email`, `name`, `profile_image` 등

### Kakao OAuth 2.0
- Authorization Endpoint: `https://kauth.kakao.com/oauth/authorize`
- Token Endpoint: `https://kauth.kakao.com/oauth/token`
- UserInfo Endpoint: `https://kapi.kakao.com/v2/user/me`
- 필수 파라미터: `client_id`, `redirect_uri`, `response_type`, `state`
- 사용자 정보 필드: `id`, `kakao_account.email`, `kakao_account.profile.nickname` 등

---

**중요**: 이 프롬프트는 실제 구현 코드와 공식 문서를 기반으로 실용적인 가이드를 작성하는 것이 목적입니다. 각 Provider의 공식 문서만 참고하고, 추측이나 비공식 자료는 사용하지 마세요.
