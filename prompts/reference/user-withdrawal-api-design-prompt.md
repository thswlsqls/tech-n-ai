# 회원탈퇴 API 설계서 작성 프롬프트

## 📋 프롬프트 목적

이 프롬프트는 `api/auth` 모듈에 **회원탈퇴 API**를 추가 구현하기 위한 **상세 설계서** 작성을 지시합니다. 설계서는 기존 인증/인가 시스템과의 정합성을 유지하며, 프로젝트의 아키텍처 원칙과 코딩 표준을 준수해야 합니다.

---

## 🎯 설계서 작성 목표

1. **회원탈퇴 API 엔드포인트 설계 및 구현 가이드 제공**
2. **기존 인증/인가 시스템과의 완벽한 통합 보장**
3. **Soft Delete 원칙의 일관된 적용**
4. **보안 및 데이터 무결성 요구사항 충족**
5. **클린코드, 객체지향 설계, SOLID 원칙 준수**

---

## 📚 필수 참고 문서

설계서 작성 시 반드시 다음 문서들을 참고하고, 설계 내용이 이 문서들과 정합성을 유지해야 합니다:

### 1. 핵심 설계 문서

- **`docs/step6/spring-security-auth-design-guide.md`**
  - Spring Security 인증/인가 아키텍처
  - JWT 토큰 관리 전략
  - 기존 API 엔드포인트 구조 및 패턴
  - 보안 고려사항 및 베스트 프랙티스

- **`docs/step1/3. aurora-schema-design.md`**
  - Aurora MySQL 테이블 설계 (User, RefreshToken, UserHistory 등)
  - Soft Delete 원칙 및 구현 방식
  - 히스토리 추적 메커니즘
  - Foreign Key 관계 및 제약조건

### 2. 기존 구현 코드 참고

- **`api/auth/src/main/java/com/tech/n/ai/api/auth/controller/AuthController.java`**
  - 기존 API 엔드포인트 구조 및 패턴
  - 요청/응답 처리 방식
  - 인증 정보 추출 방법 (`Authentication` 객체 활용)

- **`api/auth/src/main/java/com/tech/n/ai/api/auth/service/UserAuthenticationService.java`**
  - 로그아웃 구현 패턴 (RefreshToken 처리 방식)
  - 사용자 검증 로직
  - 트랜잭션 관리 방식

- **`api/auth/src/main/java/com/tech/n/ai/api/auth/service/RefreshTokenService.java`**
  - RefreshToken 조회 및 삭제 메서드
  - Soft Delete 처리 방식

- **`domain/aurora/src/main/java/com/tech/n/ai/domain/mariadb/repository/writer/BaseWriterRepository.java`**
  - Soft Delete 자동 처리 메커니즘
  - 히스토리 자동 저장 로직 (`delete()` 메서드)

- **`domain/aurora/src/main/java/com/tech/n/ai/domain/mariadb/service/history/HistoryServiceImpl.java`**
  - 히스토리 저장 서비스 구현
  - OperationType (INSERT, UPDATE, DELETE) 처리 방식

### 3. 추가 참고 문서

- **`api/auth/docs/[bug fix 2] history-service-layer-refactoring-design.md`**
  - 히스토리 서비스 레이어 리팩토링 설계
  - 히스토리 저장 패턴 및 베스트 프랙티스

---

## 🔍 설계서에 포함되어야 할 섹션

### 1. 개요 (Overview)

- 회원탈퇴 API의 목적 및 범위
- 기존 인증/인가 시스템과의 관계
- 주요 요구사항 요약

### 2. API 엔드포인트 설계

#### 2.1 엔드포인트 명세

- **HTTP Method**: `DELETE` 또는 `POST` (RESTful 원칙 준수)
- **URL**: `/api/v1/auth/withdraw` 또는 `/api/v1/auth/me` (DELETE)
- **인증 요구사항**: 인증된 사용자만 접근 가능 (Bearer Token 필수)
- **요청 본문**: 
  - 비밀번호 확인 (선택적, 보안 강화 목적)
  - 탈퇴 사유 (선택적)
- **응답 형식**: 기존 API와 일관된 `ApiResponse<T>` 형식

#### 2.2 요청/응답 DTO 설계

- **Request DTO**: `WithdrawRequest` (필요 시)
  - 비밀번호 필드 (선택적)
  - 탈퇴 사유 필드 (선택적)
- **Response DTO**: 기존 `ApiResponse<Void>` 또는 커스텀 응답
  - 성공 메시지 포함

#### 2.3 보안 고려사항

- **인증 검증**: JWT 토큰에서 추출한 `userId`와 요청 대상 사용자 ID 일치 여부 확인
- **권한 검증**: 사용자는 자신의 계정만 탈퇴 가능 (다른 사용자 계정 탈퇴 불가)
- **비밀번호 확인**: 선택적으로 비밀번호 재확인 요구 (보안 강화)
- **토큰 무효화**: 회원탈퇴 시 모든 활성 RefreshToken 즉시 무효화

### 3. 비즈니스 로직 설계

#### 3.1 회원탈퇴 프로세스

다음 단계를 포함한 상세 시퀀스 다이어그램 제공:

1. **사용자 인증 확인**
   - JWT 토큰에서 `userId` 추출
   - `Authentication` 객체에서 사용자 정보 확인

2. **사용자 존재 및 활성 상태 확인**
   - `UserReaderRepository.findByEmail()` 또는 `findById()`로 사용자 조회
   - Soft Delete 상태 확인 (`is_deleted = FALSE`)
   - 이미 탈퇴된 사용자인지 확인

3. **비밀번호 확인 (선택적)**
   - 요청에 비밀번호가 포함된 경우, `PasswordEncoder.matches()`로 검증
   - 비밀번호 불일치 시 `UnauthorizedException` 발생

4. **관련 데이터 처리**
   - **RefreshToken 삭제**: 해당 사용자의 모든 활성 RefreshToken Soft Delete
     - `RefreshTokenService`를 통해 사용자 ID로 모든 RefreshToken 조회
     - 각 RefreshToken에 대해 `refreshTokenService.deleteRefreshToken()` 호출
   - **EmailVerification 토큰 처리**: 만료된 토큰은 자동 정리되므로 별도 처리 불필요 (선택적)
   - **관련 엔티티 처리**: 
     - `bookmarks` 테이블의 사용자 북마크는 스키마 분리로 인해 별도 처리 불필요 (애플리케이션 레벨에서 처리)
     - 필요 시 관련 데이터 정리 로직 포함

5. **User 엔티티 Soft Delete**
   - `UserWriterRepository.delete()` 또는 `deleteById()` 호출
   - `BaseWriterRepository.delete()` 메서드가 자동으로:
     - `is_deleted = TRUE` 설정
     - `deleted_at = 현재 시간` 설정
     - `deleted_by = 현재 사용자 ID` 설정
     - `UserHistory`에 `OperationType.DELETE` 히스토리 자동 저장

6. **Kafka 이벤트 발행**
   - `UserDeletedEvent` 발행
   - 이벤트 페이로드에 사용자 ID, 이메일, 탈퇴 일시 포함
   - 토픽: `user-events` (기존 `KAFKA_TOPIC_USER_EVENTS` 상수 사용)

7. **응답 반환**
   - 성공 응답 반환 (`200 OK`)

#### 3.2 시퀀스 다이어그램

Mermaid 형식으로 다음 컴포넌트 간 상호작용을 시각화:

```
Client → AuthController → AuthFacade → AuthService → UserReaderRepository
                                                      → UserWriterRepository
                                                      → RefreshTokenService
                                                      → PasswordEncoder
                                                      → EventPublisher (Kafka)
                                                      → HistoryService (자동)
```

### 4. 데이터베이스 설계

#### 4.1 User 테이블 변경사항

- **Soft Delete 처리**:
  - `is_deleted = TRUE`
  - `deleted_at = 현재 시간`
  - `deleted_by = 현재 사용자 ID` (자기 자신)
- **기존 컬럼 유지**: 모든 데이터는 보존 (GDPR 준수 고려)

#### 4.2 RefreshToken 테이블 처리

- **모든 활성 RefreshToken Soft Delete**:
  - 사용자 ID로 모든 RefreshToken 조회
  - 각 RefreshToken에 대해 Soft Delete 수행
  - 히스토리 자동 저장 (BaseWriterRepository가 처리)

#### 4.3 UserHistory 테이블 자동 저장

- **OperationType**: `DELETE` (Soft Delete 의미)
- **before_data**: 삭제 전 User 엔티티 JSON
- **after_data**: 삭제 후 User 엔티티 JSON (`is_deleted = TRUE` 포함)
- **changed_by**: 현재 사용자 ID
- **changed_at**: 현재 시간
- **change_reason**: 탈퇴 사유 (요청에 포함된 경우)

#### 4.4 트랜잭션 관리

- **@Transactional** 어노테이션 사용
- 모든 작업이 하나의 트랜잭션 내에서 수행되어야 함
- 실패 시 롤백 보장

### 5. 구현 가이드

#### 5.1 Controller 레이어

- **클래스**: `AuthController`
- **메서드**: `withdraw()` 또는 `deleteAccount()`
- **인증**: `Authentication` 파라미터로 현재 사용자 정보 추출
- **요청 검증**: `@Valid` 어노테이션 사용 (필요 시)
- **응답**: `ResponseEntity<ApiResponse<Void>>`

#### 5.2 Facade 레이어

- **클래스**: `AuthFacade`
- **역할**: Controller와 Service 간 중간 계층
- **메서드**: `withdraw(String userId, WithdrawRequest request)`

#### 5.3 Service 레이어

- **클래스**: `AuthService` 또는 새로운 `UserWithdrawalService` (단일 책임 원칙 고려)
- **메서드**: `withdraw(String userId, WithdrawRequest request)`
- **의존성**:
  - `UserReaderRepository`
  - `UserWriterRepository`
  - `RefreshTokenService`
  - `PasswordEncoder` (선택적)
  - `EventPublisher` (Kafka)

#### 5.4 Repository 레이어

- **기존 Repository 활용**:
  - `UserReaderRepository`: 사용자 조회
  - `UserWriterRepository`: Soft Delete 수행 (자동 히스토리 저장)
  - `RefreshTokenReaderRepository`: 사용자별 RefreshToken 조회
  - `RefreshTokenWriterRepository`: RefreshToken Soft Delete

### 6. 보안 설계

#### 6.1 인증 및 권한

- **Spring Security 설정**:
  - `/api/v1/auth/withdraw` 엔드포인트는 인증 필요 (`authenticated()`)
  - `SecurityConfig`의 `authorizeHttpRequests` 설정 확인

- **권한 검증**:
  - JWT 토큰의 `userId`와 요청 대상 사용자 ID 일치 확인
  - 다른 사용자 계정 탈퇴 시도 시 `ForbiddenException` 또는 `UnauthorizedException` 발생

#### 6.2 데이터 보안

- **개인정보 보호**:
  - Soft Delete로 데이터 보존 (GDPR 준수)
  - 필요 시 개인정보 마스킹 또는 암호화 고려

- **토큰 보안**:
  - 모든 RefreshToken 즉시 무효화
  - 탈퇴 후 기존 Access Token도 무효화됨 (사용자 ID가 더 이상 유효하지 않음)

### 7. 에러 처리

#### 7.1 예외 시나리오

다음 예외 상황에 대한 처리 방안 포함:

1. **사용자 미존재**: `ResourceNotFoundException`
2. **이미 탈퇴된 사용자**: `ConflictException` 또는 `ResourceNotFoundException`
3. **비밀번호 불일치**: `UnauthorizedException`
4. **권한 없음**: `ForbiddenException` 또는 `UnauthorizedException`
5. **트랜잭션 실패**: `TransactionException` (자동 롤백)

#### 7.2 에러 응답 형식

- 기존 `GlobalExceptionHandler` 활용
- 일관된 에러 응답 형식 (`ApiResponse<T>` with ErrorCode)

### 8. 테스트 전략

#### 8.1 단위 테스트

- **Service 레이어 테스트**:
  - 정상 탈퇴 시나리오
  - 비밀번호 불일치 시나리오
  - 이미 탈퇴된 사용자 시나리오
  - RefreshToken 삭제 확인

#### 8.2 통합 테스트

- **API 엔드포인트 테스트**:
  - 인증된 사용자 탈퇴
  - 인증되지 않은 사용자 요청
  - 다른 사용자 계정 탈퇴 시도

#### 8.3 HTTP 테스트 파일

- **파일**: `api/auth/src/test/http/08-withdraw.http`
- **기존 HTTP 테스트 파일 형식 준수** (`01-signup.http` 참고)

### 9. Kafka 이벤트 설계

#### 9.1 UserDeletedEvent

- **이벤트 타입**: `UserDeletedEvent`
- **토픽**: `user-events` (기존 `KAFKA_TOPIC_USER_EVENTS` 상수 사용)
- **페이로드**:
  ```java
  {
    "userId": "123456789",
    "email": "user@example.com",
    "username": "username",
    "deletedAt": "2026-01-27T10:00:00",
    "deletedBy": "123456789"
  }
  ```

#### 9.2 이벤트 발행 시점

- User 엔티티 Soft Delete 성공 후
- 트랜잭션 커밋 전 (트랜잭션 롤백 시 이벤트 미발행)

### 10. 클린코드 및 설계 원칙

#### 10.1 SOLID 원칙 준수

- **단일 책임 원칙 (SRP)**:
  - 각 클래스는 하나의 책임만 가짐
  - `UserWithdrawalService` 분리 고려 (선택적)

- **개방-폐쇄 원칙 (OCP)**:
  - 기존 코드 수정 최소화
  - 확장 가능한 구조

- **의존성 역전 원칙 (DIP)**:
  - 인터페이스에 의존
  - Repository 인터페이스 활용

#### 10.2 객체지향 설계

- **도메인 모델 활용**:
  - `UserEntity`의 비즈니스 로직 메서드 활용
  - 엔티티의 상태 변경은 엔티티 내부 메서드로 처리

- **서비스 계층 분리**:
  - 비즈니스 로직은 Service 레이어에 집중
  - Repository는 데이터 접근만 담당

#### 10.3 클린코드 원칙

- **명확한 네이밍**:
  - 메서드명: `withdraw()`, `deleteAccount()` 등 명확한 의미
  - 변수명: `user`, `refreshTokens` 등 의미 있는 이름

- **작은 메서드**:
  - 각 메서드는 하나의 책임만 수행
  - 복잡한 로직은 private 메서드로 분리

- **주석 및 문서화**:
  - JavaDoc 주석 작성
  - 복잡한 로직에 대한 설명 주석

### 11. 외부 자료 참고

#### 11.1 공식 문서

다음 공식 문서를 참고하여 설계의 정확성과 최신성을 보장:

- **Spring Security 공식 문서**:
  - [Spring Security Reference Documentation](https://docs.spring.io/spring-security/reference/)
  - [Spring Security Authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/basic.html)

- **Spring Data JPA 공식 문서**:
  - [Spring Data JPA Reference Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

- **JWT 공식 스펙**:
  - [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)

#### 11.2 베스트 프랙티스

- **RESTful API 설계 원칙**:
  - HTTP 메서드의 의미론적 사용
  - 리소스 중심 URL 설계

- **Soft Delete 패턴**:
  - 데이터 보존 및 복구 가능성
  - 히스토리 추적

---

## ✅ 설계서 검증 기준

설계서가 다음 기준을 모두 만족해야 합니다:

### 1. 정합성 검증

- ✅ 기존 `spring-security-auth-design-guide.md`와의 정합성
- ✅ 기존 `aurora-schema-design.md`와의 정합성
- ✅ 기존 API 엔드포인트 패턴과의 일관성
- ✅ Soft Delete 원칙의 일관된 적용

### 2. 완전성 검증

- ✅ 모든 필수 섹션 포함
- ✅ 시퀀스 다이어그램 포함
- ✅ 에러 처리 시나리오 명시
- ✅ 테스트 전략 포함

### 3. 구현 가능성 검증

- ✅ 구체적인 클래스명 및 메서드명 제시
- ✅ 의존성 관계 명확히 정의
- ✅ 트랜잭션 관리 방안 명시
- ✅ Kafka 이벤트 발행 방식 명시

### 4. 보안 검증

- ✅ 인증 및 권한 검증 로직 포함
- ✅ 토큰 무효화 처리 포함
- ✅ 데이터 보안 고려사항 포함

### 5. 코드 품질 검증

- ✅ SOLID 원칙 준수 방안 명시
- ✅ 객체지향 설계 원칙 적용
- ✅ 클린코드 원칙 준수

---

## 📝 설계서 작성 지시사항

### 1. 문서 위치

설계서는 다음 경로에 저장합니다:

```
api/auth/docs/user-withdrawal-api-design.md
```

### 2. 문서 형식

- **Markdown 형식** 사용
- **Mermaid 다이어그램** 사용 (시퀀스 다이어그램, 클래스 다이어그램 등)
- **코드 예제** 포함 (Java 코드, SQL 쿼리 등)

### 3. 문서 구조

위의 "설계서에 포함되어야 할 섹션"을 참고하여 구조화된 문서 작성

### 4. 참고 문서 인용

설계서 내에서 참고 문서를 인용할 때는 다음 형식 사용:

```markdown
> **참고**: [Spring Security Auth Design Guide](./spring-security-auth-design-guide.md) Section 5.2.1
```

### 5. 코드 예제

실제 구현 가능한 코드 예제를 포함하되, 다음 원칙 준수:

- 기존 코드 스타일과 일관성 유지
- Lombok 어노테이션 활용
- 명확한 변수명 및 메서드명 사용
- JavaDoc 주석 포함

---

## 🚀 실행 지시

다음 명령어로 설계서 작성을 시작하세요:

```
회원탈퇴 API 설계서를 작성하세요. 위의 모든 요구사항과 검증 기준을 충족하는 상세한 설계서를 작성해주세요.
```

---

## 📌 추가 고려사항

### 1. GDPR 및 개인정보 보호

- Soft Delete로 데이터 보존 (법적 요구사항 준수)
- 필요 시 개인정보 마스킹 또는 암호화 고려
- 데이터 보관 기간 정책 고려

### 2. 복구 기능 (선택적)

- 회원탈퇴 후 일정 기간 내 복구 가능 여부 고려
- 복구 API 설계 (별도 작업으로 분리 가능)

### 3. 알림 및 로깅

- 회원탈퇴 이벤트 로깅
- 관리자 알림 (선택적)

### 4. 성능 고려사항

- 대량의 RefreshToken 삭제 시 성능 최적화
- 트랜잭션 범위 최소화

---

**작성일**: 2026-01-27  
**버전**: 1.0  
**작성자**: AI Assistant
