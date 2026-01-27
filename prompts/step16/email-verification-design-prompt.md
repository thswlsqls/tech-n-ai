# 이메일 인증 기능 설계서 작성 프롬프트

## 역할 (Role)

당신은 Spring Boot 멀티모듈 프로젝트의 시니어 백엔드 아키텍트입니다.  
클린 아키텍처, SOLID 원칙, 그리고 실용적인 설계를 통해 유지보수 가능한 코드를 작성하는 전문가입니다.

---

## 배경 (Context)

### 프로젝트 구조

```
shrimp-tm-demo/
├── api/
│   └── auth/           # 인증 API 모듈 (현재 이메일 인증 로직이 미완성)
├── client/
│   ├── feign/          # Feign 클라이언트 모듈
│   ├── slack/          # Slack 알림 클라이언트 (참조 패턴)
│   ├── rss/            # RSS 파서 클라이언트
│   └── scraper/        # 웹 스크래퍼 클라이언트
├── common/
│   ├── core/           # 공통 유틸리티
│   ├── exception/      # 예외 처리
│   ├── kafka/          # Kafka 이벤트 발행/구독
│   └── security/       # Spring Security, JWT
└── domain/
    ├── aurora/         # Aurora MySQL 엔티티/리포지토리
    └── mongodb/        # MongoDB 도큐먼트/리포지토리
```

### 현재 상황

#### 문제점

`POST /api/v1/auth/signup` 요청 시:
1. `users` 테이블에 사용자 정보 저장 ✅
2. `email_verifications` 테이블에 인증 토큰 저장 ✅  
3. "회원가입이 완료되었습니다. 이메일 인증을 완료해주세요." 응답 ✅
4. **등록된 이메일로 인증 메일 발송 ❌ (미구현)**

#### 현재 코드 분석

**AuthService.java**
```java
@Transactional
public AuthResponse signup(SignupRequest request) {
    userValidator.validateEmailNotExists(request.email());
    userValidator.validateUsernameNotExists(request.username());
    
    UserEntity user = UserEntity.createNewUser(
        request.email(),
        request.username(),
        passwordEncoder.encode(request.password())
    );
    userWriterRepository.save(user);
    emailVerificationService.createEmailVerificationToken(request.email());  // 토큰 생성만 함
    
    return new AuthResponse(
        user.getId(),
        user.getEmail(),
        user.getUsername(),
        "회원가입이 완료되었습니다. 이메일 인증을 완료해주세요."
    );
}
```

**EmailVerificationService.java**
```java
@Transactional
public String createEmailVerificationToken(String email) {
    String token = SecureTokenGenerator.generate();
    EmailVerificationEntity verification = EmailVerificationEntity.create(
        email, 
        token, 
        EMAIL_VERIFICATION_TYPE, 
        LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS)
    );
    emailVerificationWriterRepository.save(verification);
    return token;  // 토큰만 반환, 이메일 발송 로직 없음
}
```

#### 기존 client/slack 모듈 패턴 (참조용)

```java
// SlackNotificationService 인터페이스
public interface SlackNotificationService {
    void sendErrorNotification(String message, Throwable error);
    void sendSuccessNotification(String message);
    void sendInfoNotification(String message);
    void sendBatchJobNotification(SlackDto.BatchJobResult result);
}

// SlackNotificationServiceImpl 구현체
@Slf4j
@Service
@RequiredArgsConstructor
public class SlackNotificationServiceImpl implements SlackNotificationService {
    private final SlackContract slackContract;
    
    @Override
    public void sendErrorNotification(String message, Throwable error) {
        slackContract.sendErrorNotification(message, error);
    }
    // ...
}
```

---

## 과제 (Task)

이메일 인증 기능을 완성하기 위한 **기술 설계서**를 작성하세요.

### 설계서에 포함해야 할 내용

1. **아키텍처 설계**
   - 모듈 구조 (신규 모듈 추가 필요 시 위치와 역할 명시)
   - 컴포넌트 간 의존성 다이어그램
   - 데이터 흐름도

2. **이메일 발송 방식 선택 및 근거**
   - AWS SES vs Spring Mail (JavaMailSender) 비교 분석
   - 프로젝트 규모와 요구사항에 적합한 선택 및 근거
   - 선택한 방식의 공식 문서 기반 구현 가이드

3. **인터페이스 설계**
   - 이메일 서비스 인터페이스 정의 (메서드 시그니처, 파라미터, 반환값)
   - DTO 설계 (이메일 발송에 필요한 데이터 구조)
   - 설정 클래스 설계 (Properties, Config)

4. **구현 가이드**
   - 단계별 구현 순서
   - 각 클래스별 책임과 구현 포인트
   - 기존 코드(`EmailVerificationService`)와의 통합 방법

5. **이메일 템플릿 설계**
   - 회원가입 인증 이메일 템플릿
   - 비밀번호 재설정 이메일 템플릿
   - 템플릿 엔진 선택 근거 (Thymeleaf 등)

6. **에러 처리 및 복원력**
   - 이메일 발송 실패 시 처리 전략
   - 재시도 정책
   - 모니터링/로깅 방안

7. **테스트 전략**
   - 단위 테스트 설계
   - 통합 테스트 설계
   - 로컬 개발 환경에서의 테스트 방법

8. **설정 가이드**
   - application.yml 설정 예시
   - 환경별 설정 분리 (local, dev, prod)
   - 민감 정보 관리 방법 (환경 변수)

---

## 제약조건 (Constraints)

### 반드시 준수

1. **공식 문서 기반**: 모든 기술 선택과 구현 방법은 Spring 공식 문서, AWS 공식 문서 등 신뢰할 수 있는 공식 출처만 참조
2. **클린코드 원칙**: 의미 있는 네이밍, 작은 함수, 단일 책임 원칙
3. **SOLID 원칙**: 특히 단일 책임(SRP), 의존성 역전(DIP) 원칙 준수
4. **기존 패턴 일관성**: `client/slack` 모듈의 구조 패턴 참조
5. **모듈 독립성**: 이메일 클라이언트는 독립 모듈로 분리

### 금지 사항

1. **오버엔지니어링 금지**: 현재 요구사항에 필요한 기능만 설계
2. **불필요한 추상화 금지**: 당장 확장이 필요하지 않은 곳에 과도한 인터페이스/팩토리 패턴 적용 금지
3. **장황한 주석 금지**: 코드로 설명 가능한 내용은 주석 대신 명확한 네이밍 사용
4. **미래 예측 설계 금지**: "나중에 ~할 수 있으니까" 식의 설계 금지, YAGNI 원칙 적용

---

## 출력 형식 (Output Format)

설계서는 다음 구조로 작성:

```markdown
# api/auth 모듈 이메일 인증 기능 설계서

## 목차
1. 개요
2. 아키텍처 설계
3. 이메일 발송 방식 선택
4. 인터페이스 설계
5. 구현 가이드
6. 이메일 템플릿 설계
7. 에러 처리 및 복원력
8. 테스트 전략
9. 설정 가이드
10. 참고 자료

## 1. 개요
### 1.1 배경 및 목적
### 1.2 범위

## 2. 아키텍처 설계
### 2.1 모듈 구조
### 2.2 컴포넌트 의존성 다이어그램 (Mermaid)
### 2.3 시퀀스 다이어그램 (Mermaid)

## 3. 이메일 발송 방식 선택
### 3.1 선택지 비교
### 3.2 결정 및 근거
### 3.3 공식 문서 참조

(이하 생략)
```

### 다이어그램 형식

- Mermaid 문법 사용
- 클래스 다이어그램, 시퀀스 다이어그램 포함

### 코드 예시 형식

- 인터페이스는 전체 코드
- 구현체는 핵심 로직만 발췌 또는 pseudocode
- 설정 파일은 전체 예시

---

## 참고 정보 (Reference)

### 기존 엔티티 구조

```java
// EmailVerificationEntity
@Entity
@Table(name = "email_verifications")
public class EmailVerificationEntity extends BaseEntity {
    private String email;      // 인증 대상 이메일
    private String token;      // 인증 토큰 (UUID)
    private String type;       // "EMAIL_VERIFICATION" 또는 "PASSWORD_RESET"
    private LocalDateTime expiresAt;   // 만료 시간
    private LocalDateTime verifiedAt;  // 인증 완료 시간
}
```

### 기존 상수

```java
// VerificationConstants
public static final String EMAIL_VERIFICATION_TYPE = "EMAIL_VERIFICATION";
public static final String PASSWORD_RESET_TYPE = "PASSWORD_RESET";
public static final long TOKEN_EXPIRY_HOURS = 24;
```

### 인증 URL 형식

- 이메일 인증: `GET /api/v1/auth/verify-email?token={token}`
- 비밀번호 재설정: 별도 프론트엔드 페이지에서 토큰 사용

### 환경 정보

- Spring Boot 3.x
- Java 17+
- Gradle 멀티모듈 프로젝트
- AWS 인프라 사용 중 (Aurora MySQL, ElastiCache Redis)

---

## 추가 지침 (Additional Instructions)

1. **단계적 사고(Chain-of-Thought)**: 각 설계 결정 시 선택지 → 비교 → 결정 → 근거 순으로 논리적으로 전개
2. **트레이드오프 명시**: 설계 결정에서 포기한 것과 얻은 것을 명확히 기술
3. **구현 가능성 검증**: 제안하는 모든 코드는 실제 컴파일 가능한 수준으로 작성
4. **기존 코드 존중**: 기존 코드 스타일과 네이밍 컨벤션 유지
