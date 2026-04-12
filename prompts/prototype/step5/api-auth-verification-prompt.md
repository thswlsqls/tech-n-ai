# 사용자 인증 API 설계 검증 프롬프트

## 역할 정의

당신은 **Java 백엔드 아키텍처 검증 전문가**입니다. 다음 역할과 책임을 수행합니다:

- **역할**: 
  - Spring Boot 기반 RESTful API 설계 검증 전문가
  - CQRS 패턴 및 마이크로서비스 아키텍처 검증 전문가
  - 보안 및 인증/인가 시스템 검증 전문가
  - 데이터베이스 스키마 설계 검증 전문가

- **전문 분야**:
  - JWT 기반 인증 시스템
  - OAuth 2.0 인증 플로우
  - Aurora MySQL 및 MongoDB Atlas 데이터 모델
  - Kafka 이벤트 기반 동기화
  - Spring Security 및 보안 베스트 프랙티스

- **검증 원칙**:
  - 설계서 간 일관성 엄격히 검증
  - 보안 취약점 및 버그 사전 발견
  - 공식 출처 기반 베스트 프랙티스 준수 확인
  - 실용성과 구현 가능성 고려
  - 오버엔지니어링 방지

## 검증 목적

`docs/phase2/1. api-endpoint-design.md` 문서의 **인증 API 엔드포인트** 섹션에 정의된 모든 사용자 인증 관련 API 설계를 검증합니다. 그 후, 다음의 첨부하는 plan task: [프롬프트] 의 프롬프트가 @docs/phase2/1. api-endpoint-design.md 설계서와 그와 관련된 다른 설계서들 그리고 코드들의 구현 명세를 정확히 반영하며, 정합성이 100% 충족되는지 여부를 검증하세요. 

@shrimp-task-prompt.md (2077-2371) 

## 검증 대상

다음 API 엔드포인트들을 검증 대상으로 합니다:

1. **회원가입** (`POST /api/v1/auth/signup`)
2. **로그인** (`POST /api/v1/auth/login`)
3. **로그아웃** (`POST /api/v1/auth/logout`)
4. **토큰 갱신** (`POST /api/v1/auth/refresh`)
5. **이메일 인증** (`GET /api/v1/auth/verify-email`)
6. **비밀번호 재설정 요청** (`POST /api/v1/auth/reset-password`)
7. **비밀번호 재설정 확인** (`POST /api/v1/auth/reset-password/confirm`)
8. **OAuth 2.0 로그인 시작** (`GET /api/v1/auth/oauth2/{provider}`)
9. **OAuth 2.0 로그인 콜백** (`GET /api/v1/auth/oauth2/{provider}/callback`)

## 참고 설계서

다음 설계서들을 참고하여 검증을 수행합니다:

1. **테이블 설계서**: `docs/phase1/3. aurora-schema-design.md`
   - `auth` 스키마의 테이블 구조 (providers, users, admins, refresh_tokens, email_verifications, user_history)
   - 각 테이블의 필드 타입, 제약조건, 인덱스
   - Foreign Key 관계

2. **데이터 모델 설계서**: `docs/phase2/2. data-model-design.md`
   - Command Side (Aurora MySQL) 엔티티 구조
   - Query Side (MongoDB Atlas) Document 구조
   - TSID 필드 기반 매핑 전략
   - Kafka 이벤트 동기화 전략

3. **에러 핸들링 전략**: `docs/phase2/4. error-handling-strategy-design.md`
   - HTTP 상태 코드와 비즈니스 에러 코드 분리
   - 에러 응답 형식

4. **설계 검증 보고서**: `docs/phase2/5. design-verification-report.md`
   - 기존 검증 결과 참고

## 검증 프로세스

검증은 다음 단계로 진행합니다:

1. **설계서 읽기**: 
   - 참고 설계서들을 먼저 읽고 이해합니다.
   - 테이블 구조, 필드 타입, 제약조건, 인덱스를 정확히 파악합니다.
   - 설계서 간 불일치 사항을 미리 식별합니다.

2. **API 설계 분석**: 
   - 각 API 엔드포인트의 처리 로직을 상세히 분석합니다.
   - 엔티티 필드 사용, 인덱스 활용, Foreign Key 관계를 확인합니다.
   - 트랜잭션 처리, 에러 처리, 보안 측면을 검토합니다.

3. **검증 항목별 확인**: 
   - **1단계**: 테이블 설계 및 데이터 모델 설계와의 일치성 검증
   - **2단계**: 설계상 버그 및 누락 사항 검증
   - **3단계**: 베스트 프랙티스 준수 검증
   - 각 검증 항목에 대해 상태(통과/경고/실패)와 심각도를 판단합니다.

4. **결과 정리**: 
   - 발견된 문제점을 심각도별로 분류합니다 (🔴 높음 / 🟡 중간 / 🟢 낮음).
   - 설계서 불일치 사항을 명확히 식별합니다.
   - **반드시 "검증 결과 정리 형식" 섹션에 명시된 형식으로 채팅창에 핵심 요약을 제공**합니다.
   - 상세 검증 내용은 필요시 별도로 제공할 수 있지만, 핵심 요약은 필수입니다.

## 검증 항목

### 1. 테이블 설계 및 데이터 모델 설계와의 일치성 검증

**검증 기준**: 각 API의 처리 로직이 테이블 설계서와 데이터 모델 설계서에 정의된 구조와 일치하는지 확인합니다.

#### 1.1 엔티티 필드 사용 검증

각 API에서 사용하는 엔티티 필드가 테이블 설계서에 정의된 필드와 일치하는지 확인:

- **User 엔티티 필드**:
  - `id` (BIGINT UNSIGNED, TSID, PRIMARY KEY)
  - `email` (VARCHAR(100), UNIQUE, NOT NULL)
  - `username` (VARCHAR(50), UNIQUE, NOT NULL)
  - `password` (VARCHAR(255), NULL - OAuth 사용자 제외)
  - `provider_id` (BIGINT UNSIGNED, FOREIGN KEY, NULL)
  - `provider_user_id` (VARCHAR(255), NULL)
  - `is_email_verified` (BOOLEAN, NOT NULL DEFAULT FALSE)
  - `last_login_at` (TIMESTAMP(6), NULL)
  - `is_deleted` (BOOLEAN, NOT NULL DEFAULT FALSE)
  - Soft Delete 필드: `deleted_at`, `deleted_by`
  - 감사 필드: `created_at`, `created_by`, `updated_at`, `updated_by`

- **RefreshToken 엔티티 필드**:
  - `id` (BIGINT UNSIGNED, TSID, PRIMARY KEY)
  - `user_id` (BIGINT UNSIGNED, FOREIGN KEY, NOT NULL)
  - `token` (VARCHAR(500), UNIQUE, NOT NULL)
  - `expires_at` (TIMESTAMP(6), NOT NULL)
  - `is_deleted` (BOOLEAN, NOT NULL DEFAULT FALSE)
  - Soft Delete 필드: `deleted_at`, `deleted_by`

- **EmailVerification 엔티티 필드**:
  - `id` (BIGINT UNSIGNED, TSID, PRIMARY KEY)
  - `email` (VARCHAR(100), NOT NULL)
  - `token` (VARCHAR(255), UNIQUE, NOT NULL)
  - `expires_at` (TIMESTAMP(6), NOT NULL)
  - `verified_at` (TIMESTAMP(6), NULL)
  - `is_deleted` (BOOLEAN, NOT NULL DEFAULT FALSE)
  - Soft Delete 필드: `deleted_at`, `deleted_by`
  - **주의**: API 설계서에서 `type` 필드를 언급하지만, 테이블 설계서에는 `type` 필드가 없음. 이 불일치를 반드시 검증해야 함

- **Provider 엔티티 필드**:
  - `id` (BIGINT UNSIGNED, TSID, PRIMARY KEY)
  - `name` (VARCHAR(50), UNIQUE, NOT NULL)
  - `is_enabled` (BOOLEAN, NOT NULL DEFAULT TRUE)

#### 1.2 인덱스 활용 검증

각 API의 쿼리 패턴이 테이블 설계서에 정의된 인덱스를 적절히 활용하는지 확인:

- `users.email` UNIQUE 인덱스
- `users.username` UNIQUE 인덱스
- `users.provider_id + provider_user_id` 복합 인덱스
- `refresh_tokens.token` UNIQUE 인덱스
- `refresh_tokens.user_id` 외래 키 인덱스
- `refresh_tokens.expires_at` 인덱스
- `email_verifications.token` UNIQUE 인덱스
- `email_verifications.email` 인덱스
- `email_verifications.expires_at` 인덱스
- `providers.name` UNIQUE 인덱스
- `providers.is_enabled` 인덱스

#### 1.3 Foreign Key 관계 검증

각 API에서 참조하는 Foreign Key 관계가 테이블 설계서와 일치하는지 확인:

- `users.provider_id` → `providers.id` (ON DELETE SET NULL)
- `refresh_tokens.user_id` → `users.id` (ON DELETE CASCADE)
- `user_history.user_id` → `users.id` (ON DELETE CASCADE)

#### 1.4 Soft Delete 패턴 검증

Soft Delete가 필요한 엔티티에서 `is_deleted`, `deleted_at`, `deleted_by` 필드를 적절히 사용하는지 확인:

- `RefreshToken` 엔티티 Soft Delete 시 `is_deleted=TRUE`, `deleted_at` 설정
- `User` 엔티티 조회 시 `is_deleted=FALSE` 필터링 필요 여부 확인

#### 1.5 히스토리 테이블 사용 검증

모든 쓰기 작업에서 `UserHistory` 엔티티 생성이 명시되어 있는지 확인:

- `operation_type` 값이 올바른지 확인 (INSERT, UPDATE, DELETE)
- `before_data`, `after_data` JSON 필드 사용 여부 확인

#### 1.6 Kafka 이벤트 발행 검증

모든 쓰기 작업에서 적절한 Kafka 이벤트가 발행되는지 확인:

- `UserCreatedEvent`: User 엔티티 생성 시
- `UserUpdatedEvent`: User 엔티티 수정 시
- 이벤트 페이로드에 `userTsid` (User.id) 포함 여부 확인

### 2. 설계상 버그 및 누락 사항 검증

**검증 기준**: 각 API의 처리 로직에서 발생할 수 있는 버그나 설계상 누락된 사항을 확인합니다.

#### 2.1 회원가입 API 검증

- **이메일 중복 검증**: `is_deleted=FALSE` 조건 포함 여부 확인
- **사용자명 중복 검증**: `is_deleted=FALSE` 조건 포함 여부 확인
- **비밀번호 해시 알고리즘**: 설계서에 명시되어 있는지 확인 (예: BCrypt)
- **EmailVerification 엔티티 생성**: 
  - API 설계서에서 `type` 필드를 언급하지만 테이블 설계서에는 `type` 필드가 없음. **이 불일치를 반드시 검증해야 함**
  - 테이블 설계서에 `type` 필드 추가 필요 여부 또는 API 설계서 수정 필요 여부 확인
- **트랜잭션 처리**: User 생성, EmailVerification 생성, UserHistory 생성이 하나의 트랜잭션으로 처리되는지 확인
- **이메일 발송 실패 처리**: 이메일 발송 실패 시 롤백 여부 확인 (일반적으로는 비동기 처리로 롤백하지 않음)

#### 2.2 로그인 API 검증

- **비밀번호 검증**: 해시된 비밀번호와 입력 비밀번호 비교 로직 명시 여부
- **이메일 인증 여부 확인**: `is_email_verified=FALSE`인 경우 로그인 차단 여부 확인
- **Soft Delete된 사용자 처리**: `is_deleted=TRUE`인 사용자 로그인 차단 여부 확인
- **RefreshToken 생성**: `expires_at` 필드 설정 여부 확인
- **RefreshToken 중복**: 동일 사용자의 여러 RefreshToken 허용 여부 확인 (설계서에 명시되어 있는지)
- **last_login_at 업데이트**: 트랜잭션 내에서 처리되는지 확인

#### 2.3 로그아웃 API 검증

- **RefreshToken 조회**: `is_deleted=FALSE`, `expires_at` 확인 여부
- **RefreshToken Soft Delete**: `deleted_by` 필드에 userId 설정 여부 확인
- **UserHistory 생성**: 로그아웃 시 UserHistory 생성이 필요한지 확인 (설계서에 명시되어 있음)

#### 2.4 토큰 갱신 API 검증

- **RefreshToken 검증**: JWT 서명 검증 및 `expires_at` 확인 여부
- **RefreshToken 조회**: `is_deleted=FALSE` 조건 포함 여부
- **RefreshToken expires_at 갱신**: 기존 토큰의 만료 시간을 갱신하는 것이 맞는지 확인 (일반적으로는 새 토큰 생성)
- **UserHistory 생성**: 토큰 갱신 시 UserHistory 생성이 필요한지 확인 (설계서에 명시되어 있음)

#### 2.5 이메일 인증 API 검증

- **EmailVerification 조회**: `is_deleted=FALSE`, `expires_at` 확인 여부
- **중복 인증 방지**: `verified_at`이 이미 설정되어 있는 경우 처리 여부 확인
- **User 업데이트**: `is_email_verified=TRUE` 설정 시 트랜잭션 처리 여부 확인
- **EmailVerification verified_at 설정**: 중복 설정 방지 여부 확인

#### 2.6 비밀번호 재설정 요청 API 검증

- **User 조회**: `is_deleted=FALSE` 조건 포함 여부 확인
- **EmailVerification type 필드**: 
  - API 설계서에서 `type=PASSWORD_RESET`을 명시하지만 테이블 설계서에는 `type` 필드가 없음. **이 불일치를 반드시 검증해야 함**
  - 테이블 설계서에 `type` 필드 추가 필요 여부 또는 API 설계서 수정 필요 여부 확인
- **기존 토큰 처리**: 동일 이메일의 기존 PASSWORD_RESET 토큰 무효화 여부 확인 (type 필드가 없는 경우 email + token으로 구분 불가)
- **이메일 발송**: 존재하지 않는 이메일인 경우에도 성공 응답 반환 여부 확인 (보안상 일반적)

#### 2.7 비밀번호 재설정 확인 API 검증

- **EmailVerification 조회**: 
  - API 설계서에서 `type=PASSWORD_RESET`을 언급하지만 테이블 설계서에는 `type` 필드가 없음. **이 불일치를 반드시 검증해야 함**
  - `is_deleted=FALSE`, `expires_at` 확인 여부
- **토큰 재사용 방지**: `verified_at`이 이미 설정되어 있는 경우 처리 여부 확인
- **비밀번호 해시**: 새로운 비밀번호 해시 생성 로직 명시 여부
- **트랜잭션 처리**: User 업데이트, EmailVerification 업데이트, UserHistory 생성이 하나의 트랜잭션으로 처리되는지 확인

#### 2.8 OAuth 로그인 시작 API 검증

- **Provider 조회**: `is_enabled=TRUE`, `is_deleted=FALSE` 조건 포함 여부 확인
- **CSRF 방지**: `state` 파라미터 생성 및 검증 여부 확인 (설계서에 명시되어 있는지)

#### 2.9 OAuth 로그인 콜백 API 검증

- **state 검증**: CSRF 방지를 위한 `state` 파라미터 검증 여부 확인
- **OAuth 인증 코드 교환**: Access Token 교환 실패 시 에러 처리 여부 확인
- **사용자 정보 조회**: OAuth 제공자 API 호출 실패 시 에러 처리 여부 확인
- **User 조회/생성**: `provider_id + provider_user_id` 복합 인덱스 활용 여부 확인
- **OAuth 사용자 비밀번호**: OAuth 사용자는 `password` 필드가 NULL이어야 함
- **트랜잭션 처리**: User 생성/수정, RefreshToken 생성이 하나의 트랜잭션으로 처리되는지 확인

#### 2.10 공통 검증 사항

- **에러 응답 형식**: 각 API의 에러 응답이 `docs/phase2/4. error-handling-strategy-design.md`의 형식을 따르는지 확인
- **HTTP 상태 코드**: 각 API의 HTTP 상태 코드가 적절한지 확인
- **비즈니스 에러 코드**: 각 에러 시나리오에 대한 비즈니스 에러 코드가 정의되어 있는지 확인
- **트랜잭션 일관성**: 여러 엔티티를 수정하는 API에서 트랜잭션 처리가 명시되어 있는지 확인
- **동시성 제어**: 동시 요청 시 데이터 일관성 문제가 없는지 확인 (예: 중복 회원가입, 중복 토큰 생성)

### 3. 베스트 프랙티스 준수 검증

**검증 기준**: 신뢰할 수 있는 공식 출처(OAuth 2.0 RFC, JWT RFC, Spring Security 공식 문서 등)를 기준으로 베스트 프랙티스를 준수하는지 확인합니다.

**중요**: 오버엔지니어링을 피하고, 신뢰할 수 있는 공식 출처만 참고합니다.

#### 3.1 JWT 토큰 관리 베스트 프랙티스

**참고 출처**: 
- RFC 7519 (JSON Web Token)
- OAuth 2.0 RFC 6749
- Spring Security 공식 문서

**검증 항목**:
- **Access Token 만료 시간**: 일반적으로 15분~1시간 (설계서에 3600초=1시간 명시됨)
- **Refresh Token 만료 시간**: 일반적으로 7일~30일 (설계서에 명시되어 있는지 확인)
- **Refresh Token 저장**: 데이터베이스에 저장하는 방식 (설계서에 명시됨)
- **Refresh Token 회전**: 토큰 갱신 시 기존 토큰 무효화 여부 확인 (일반적으로는 새 토큰 생성)
- **토큰 서명 알고리즘**: HS256 또는 RS256 사용 여부 확인 (설계서에 명시되어 있는지)

#### 3.2 비밀번호 보안 베스트 프랙티스

**참고 출처**:
- OWASP Password Storage Cheat Sheet
- NIST Digital Identity Guidelines

**검증 항목**:
- **비밀번호 해시 알고리즘**: BCrypt, Argon2 등 적절한 알고리즘 사용 여부 확인 (설계서에 명시되어 있는지)
- **비밀번호 정책**: 최소 길이, 복잡도 요구사항 명시 여부 확인
- **비밀번호 재사용 방지**: 이전 비밀번호 재사용 방지 로직 여부 확인 (설계서에 명시되어 있는지)

#### 3.3 이메일 인증 베스트 프랙티스

**참고 출처**:
- OWASP Authentication Cheat Sheet

**검증 항목**:
- **토큰 만료 시간**: 일반적으로 24시간~7일 (설계서에 명시되어 있는지)
- **토큰 형식**: 암호학적으로 안전한 랜덤 토큰 사용 여부 확인 (설계서에 명시되어 있는지)
- **토큰 재사용 방지**: 한 번 사용된 토큰은 재사용 불가 여부 확인
- **이메일 발송**: 존재하지 않는 이메일인 경우에도 성공 응답 반환 (보안상 일반적)

#### 3.4 OAuth 2.0 베스트 프랙티스

**참고 출처**:
- RFC 6749 (OAuth 2.0 Authorization Framework)
- RFC 7636 (Proof Key for Code Exchange by OAuth Public Clients, PKCE)
- OAuth 2.0 Security Best Current Practice

**검증 항목**:
- **Authorization Code Flow**: 표준 OAuth 2.0 Authorization Code Flow 사용 여부 확인
- **state 파라미터**: CSRF 방지를 위한 `state` 파라미터 사용 여부 확인 (설계서에 명시됨)
- **PKCE**: Public Client의 경우 PKCE 사용 권장 (설계서에 명시되어 있는지)
- **토큰 저장**: OAuth Access Token을 데이터베이스에 저장하지 않는 것이 일반적 (설계서 확인)
- **사용자 정보 동기화**: OAuth 사용자 정보를 User 엔티티에 저장하는 방식 확인

#### 3.5 세션 관리 베스트 프랙티스

**참고 출처**:
- OWASP Session Management Cheat Sheet

**검증 항목**:
- **Refresh Token 무효화**: 로그아웃 시 Refresh Token 무효화 여부 확인 (설계서에 명시됨)
- **동시 세션 관리**: 동일 사용자의 여러 Refresh Token 허용 여부 확인 (설계서에 명시되어 있는지)
- **토큰 갱신 시 기존 토큰 처리**: 기존 Refresh Token 무효화 또는 유지 여부 확인

#### 3.6 에러 처리 베스트 프랙티스

**참고 출처**:
- OWASP API Security Top 10
- RESTful API Design Best Practices

**검증 항목**:
- **에러 메시지**: 민감한 정보(예: 사용자 존재 여부)를 노출하지 않는지 확인
- **Rate Limiting**: 무차별 대입 공격 방지를 위한 Rate Limiting 명시 여부 확인 (설계서에 명시되어 있는지)
- **에러 응답 일관성**: 모든 에러 응답이 표준 형식을 따르는지 확인

## 검증 결과 작성 형식

각 검증 항목에 대해 다음 형식으로 결과를 작성합니다:

### 검증 항목: [항목명]

**상태**: ✅ 통과 / ⚠️ 경고 / ❌ 실패

**심각도**: 🔴 높음 / 🟡 중간 / 🟢 낮음 (상태가 ⚠️ 경고 또는 ❌ 실패인 경우에만 표시)

**발견 사항**:
- [구체적인 발견 사항 설명]
- [설계서 불일치가 있는 경우, 어떤 설계서와 어떤 내용이 불일치하는지 명시]

**권장 사항**:
- [개선 권장 사항]
- [설계서 수정이 필요한 경우, 어떤 설계서를 어떻게 수정해야 하는지 명시]

**참고 출처**:
- [공식 출처 링크 또는 문서명]

**관련 API**: [해당 검증 항목과 관련된 API 엔드포인트 목록]

---

**참고**: 상세 검증 내용은 위 형식으로 작성하되, **최종적으로는 "검증 결과 정리 형식" 섹션에 명시된 형식으로 채팅창에 핵심 요약을 반드시 제공**합니다.

## 설계서 불일치 검증 우선순위

다음 항목들은 설계서 간 불일치 가능성이 높으므로 우선적으로 검증해야 합니다:

1. **EmailVerification 엔티티의 `type` 필드**: 
   - API 설계서에서 `type=PASSWORD_RESET`을 언급하지만 테이블 설계서에는 `type` 필드가 없음
   - 관련 API: 회원가입, 비밀번호 재설정 요청, 비밀번호 재설정 확인

2. **RefreshToken 갱신 전략**:
   - 토큰 갱신 시 기존 토큰 무효화 여부 vs 유지 여부
   - 관련 API: 토큰 갱신

3. **이메일 발송 실패 처리**:
   - 트랜잭션 롤백 여부 vs 비동기 처리
   - 관련 API: 회원가입, 비밀번호 재설정 요청

## 검증 시 주의사항

1. **오버엔지니어링 방지**: 불필요한 복잡성을 추가하지 않도록 주의합니다.
2. **공식 출처만 참고**: 공식 RFC, OWASP, Spring Security 공식 문서 등 신뢰할 수 있는 출처만 참고합니다.
3. **설계서 기반 검증**: 설계서에 명시된 내용을 기준으로 검증하며, 설계서에 명시되지 않은 내용은 "명시 필요"로 표시합니다.
4. **실용성 고려**: 이론적 완벽성보다 실용성과 구현 가능성을 우선시합니다.
5. **보안 우선**: 보안 관련 사항은 엄격하게 검증합니다.

## 검증 결과 정리 형식

검증 완료 후, **반드시 채팅창에 다음 형식으로 핵심 내용을 정리**합니다:

---

# 사용자 인증 API 설계 검증 결과 요약

## 📊 검증 통계

- **검증 대상 API**: 9개
- **검증 항목**: 3가지 (일치성, 버그/누락, 베스트 프랙티스)
- **전체 검증 결과**: ✅ 통과 / ⚠️ 경고 / ❌ 실패

## 🔴 심각도 높음 (즉시 수정 필요)

### [문제 제목]
- **상태**: ❌ 실패
- **영향 범위**: [관련 API 목록]
- **문제 설명**: [간단한 설명]
- **권장 조치**: [구체적인 수정 방안]
- **관련 설계서**: [어떤 설계서를 어떻게 수정해야 하는지]

## 🟡 심각도 중간 (수정 권장)

### [문제 제목]
- **상태**: ⚠️ 경고
- **영향 범위**: [관련 API 목록]
- **문제 설명**: [간단한 설명]
- **권장 조치**: [구체적인 수정 방안]

## 🟢 심각도 낮음 (개선 제안)

### [문제 제목]
- **상태**: ⚠️ 경고
- **영향 범위**: [관련 API 목록]
- **문제 설명**: [간단한 설명]
- **권장 조치**: [구체적인 수정 방안]

## ✅ 통과 항목

- [주요 통과 항목들을 간단히 나열]

## 📋 설계서 불일치 사항

1. **[불일치 항목]**: [어떤 설계서와 어떤 내용이 불일치하는지]
   - **해결 방안**: [어떤 설계서를 어떻게 수정해야 하는지]

## 🎯 우선 수정 사항 (Top 3)

1. **[가장 중요한 문제]**: [간단한 설명]
2. **[두 번째로 중요한 문제]**: [간단한 설명]
3. **[세 번째로 중요한 문제]**: [간단한 설명]

## 📚 참고 사항

- 검증에 사용된 공식 출처: [RFC, OWASP 등]
- 설계서에 명시되지 않아 "명시 필요"로 표시된 항목: [항목 목록]

---

### 검증 결과 요약 작성 예시

```
# 사용자 인증 API 설계 검증 결과 요약

## 📊 검증 통계

- **검증 대상 API**: 9개
- **검증 항목**: 3가지 (일치성, 버그/누락, 베스트 프랙티스)
- **전체 검증 결과**: ⚠️ 경고 (심각도 높음 문제 1개 발견)

## 🔴 심각도 높음 (즉시 수정 필요)

### EmailVerification 엔티티 type 필드 불일치
- **상태**: ❌ 실패
- **영향 범위**: 회원가입, 비밀번호 재설정 요청, 비밀번호 재설정 확인
- **문제 설명**: API 설계서에서 `type=PASSWORD_RESET`을 언급하지만, 테이블 설계서(`docs/phase1/3. aurora-schema-design.md`)에는 `type` 필드가 정의되어 있지 않음
- **권장 조치**: 테이블 설계서에 `type` 필드(VARCHAR(50), NOT NULL) 추가 또는 API 설계서에서 `type` 필드 언급 제거
- **관련 설계서**: `docs/phase1/3. aurora-schema-design.md` (EmailVerification 테이블 DDL 수정 필요)

## 🟡 심각도 중간 (수정 권장)

### RefreshToken 갱신 전략 불명확
- **상태**: ⚠️ 경고
- **영향 범위**: 토큰 갱신
- **문제 설명**: 토큰 갱신 시 기존 RefreshToken의 `expires_at`을 갱신하는 것으로 명시되어 있으나, 일반적으로는 새 토큰을 생성하고 기존 토큰을 무효화하는 것이 베스트 프랙티스
- **권장 조치**: API 설계서에 RefreshToken 갱신 전략을 명확히 명시 (새 토큰 생성 vs 기존 토큰 갱신)

## ✅ 통과 항목

- JWT 토큰 관리 기본 구조 적절
- OAuth 2.0 Authorization Code Flow 표준 준수
- Soft Delete 패턴 일관성 유지
- Kafka 이벤트 발행 전략 명확

## 📋 설계서 불일치 사항

1. **EmailVerification.type 필드**: API 설계서와 테이블 설계서 간 불일치
   - **해결 방안**: 테이블 설계서에 `type VARCHAR(50) NOT NULL` 필드 추가 권장

## 🎯 우선 수정 사항 (Top 3)

1. **EmailVerification.type 필드 추가**: 테이블 설계서 수정 필요
2. **RefreshToken 갱신 전략 명확화**: API 설계서에 전략 명시 필요
3. **비밀번호 해시 알고리즘 명시**: BCrypt 등 구체적 알고리즘 명시 필요

## 📚 참고 사항

- 검증에 사용된 공식 출처: RFC 7519 (JWT), RFC 6749 (OAuth 2.0), OWASP Authentication Cheat Sheet
- 설계서에 명시되지 않아 "명시 필요"로 표시된 항목: 비밀번호 해시 알고리즘, RefreshToken 만료 시간, 이메일 인증 토큰 만료 시간
```

---

**중요 지시사항**:

1. **반드시 채팅창에 위 형식으로 검증 결과 요약을 제공**합니다.
2. 검증 완료 후 즉시 위 형식의 요약을 채팅창에 작성합니다.
3. 상세 검증 내용은 필요시 별도로 제공할 수 있지만, **핵심 요약은 필수**입니다.
4. 심각도 높음(🔴) 문제는 반드시 우선 수정 사항에 포함시킵니다.
5. 설계서 불일치 사항은 해결 방안을 구체적으로 제시합니다.
6. 통과 항목도 간단히 나열하여 검증의 완전성을 보여줍니다.
