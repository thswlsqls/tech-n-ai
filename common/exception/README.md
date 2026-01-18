# Common Exception Module

전역 예외 처리 및 예외 로깅 기능을 제공하는 모듈입니다.

## 개요

`common-exception` 모듈은 프로젝트 전반에서 발생하는 예외를 일관된 형식으로 처리하고, MongoDB Atlas에 예외 로그를 저장하는 기능을 제공합니다. 이 모듈은 `common-core` 모듈의 `BaseException`을 확장하여 구체적인 예외 클래스를 제공하고, `GlobalExceptionHandler`를 통해 모든 예외를 중앙에서 처리합니다.

## 주요 기능

### 1. 전역 예외 처리

모든 예외를 일관된 형식으로 처리하는 `GlobalExceptionHandler`를 제공합니다.

- **BaseException 처리**: 모든 커스텀 예외의 부모 클래스 처리
- **비즈니스 예외 처리**: ResourceNotFoundException, UnauthorizedException, ForbiddenException 등
- **유효성 검증 예외 처리**: MethodArgumentNotValidException 처리
- **예상치 못한 예외 처리**: 모든 예외를 포괄하는 기본 핸들러

**참고 설계서**: `docs/step2/4. error-handling-strategy-design.md`

### 2. 구체적인 예외 클래스

비즈니스 로직에서 사용할 수 있는 구체적인 예외 클래스를 제공합니다.

- **ResourceNotFoundException**: 리소스를 찾을 수 없을 때 (HTTP 404)
- **UnauthorizedException**: 인증 실패 시 (HTTP 401)
- **ForbiddenException**: 권한 없음 시 (HTTP 403)
- **ConflictException**: 충돌 발생 시 (HTTP 409)
- **RateLimitExceededException**: Rate limit 초과 시 (HTTP 429)
- **ExternalApiException**: 외부 API 호출 실패 시

### 3. 예외 로깅

MongoDB Atlas에 예외 로그를 저장하는 기능을 제공합니다.

- **ExceptionLoggingService**: 예외를 MongoDB Atlas에 비동기로 저장
- **ExceptionContext**: 예외 발생 컨텍스트 정보 (모듈, 메서드, 사용자 ID 등)
- **READ/WRITE 구분**: 읽기 예외와 쓰기 예외를 구분하여 로깅

## 주요 컴포넌트

### 예외 핸들러

#### GlobalExceptionHandler

모든 예외를 중앙에서 처리하는 핸들러입니다.

```java
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
        BaseException e, HttpServletRequest request) {
        // 예외 처리 로직
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException e, HttpServletRequest request) {
        // 유효성 검증 예외 처리
    }
}
```

**주요 기능**:
- HTTP 상태 코드 매핑: 에러 코드를 HTTP 상태 코드로 자동 매핑
- 메시지 코드 처리: 국제화 지원을 위한 메시지 코드 처리
- 예외 로깅: `ExceptionLoggingService`를 통한 예외 로깅
- 컨텍스트 정보 수집: 요청 정보(모듈, 메서드, 사용자 ID 등) 수집

### 예외 클래스

#### ResourceNotFoundException

리소스를 찾을 수 없을 때 발생하는 예외입니다.

```java
throw new ResourceNotFoundException("사용자를 찾을 수 없습니다.");
```

#### UnauthorizedException

인증 실패 시 발생하는 예외입니다.

```java
throw new UnauthorizedException("인증에 실패했습니다.");
```

#### ForbiddenException

권한 없음 시 발생하는 예외입니다.

```java
throw new ForbiddenException("권한이 없습니다.");
```

#### ConflictException

충돌 발생 시 발생하는 예외입니다.

```java
throw new ConflictException("이미 사용 중인 이메일입니다.");
```

#### RateLimitExceededException

Rate limit 초과 시 발생하는 예외입니다.

```java
throw new RateLimitExceededException("요청 한도를 초과했습니다.");
```

#### ExternalApiException

외부 API 호출 실패 시 발생하는 예외입니다.

```java
throw new ExternalApiException("외부 API 호출에 실패했습니다.");
```

### 예외 로깅 서비스

#### ExceptionLoggingService

예외를 MongoDB Atlas에 비동기로 저장하는 서비스입니다.

```java
@Async
public void logReadException(Exception exception, ExceptionContext.ContextInfo context) {
    // MongoDB Atlas에 예외 로그 저장
}

@Async
public void logWriteException(Exception exception, ExceptionContext.ContextInfo context) {
    // MongoDB Atlas에 예외 로그 저장
}
```

**주요 기능**:
- 비동기 처리: `@Async`를 통한 비동기 예외 로깅
- READ/WRITE 구분: 읽기 예외와 쓰기 예외를 구분하여 로깅
- 폴백 처리: MongoDB 저장 실패 시 로컬 로그 파일에 기록
- 심각도 결정: 예외 타입에 따라 심각도 자동 결정

#### ExceptionContext

예외 발생 컨텍스트 정보를 담는 클래스입니다.

```java
public record ExceptionContext(
    String source,                    // "READ" 또는 "WRITE"
    String exceptionType,              // 예외 클래스 이름
    String message,                    // 예외 메시지
    String stackTrace,                 // 스택 트레이스
    ContextInfo context,              // 컨텍스트 정보
    Instant occurredAt,                // 발생 시각
    String severity                    // 심각도
) {
    public record ContextInfo(
        String module,                 // 모듈명
        String method,                 // HTTP 메서드
        Map<String, Object> parameters, // 요청 파라미터
        String userId,                 // 사용자 ID
        String requestId               // 요청 ID
    ) {}
}
```

### 비동기 설정

#### AsyncConfig

비동기 예외 로깅을 위한 설정 클래스입니다.

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    // 비동기 실행 설정
}
```

## 의존성

### 주요 의존성

- **common-core**: 공통 핵심 모듈 (BaseException, ApiResponse 등)
- **domain-mongodb**: MongoDB 도메인 모듈 (예외 로그 저장)

## 사용 방법

### 1. 의존성 추가

다른 모듈에서 `common-exception` 모듈을 사용하려면 `build.gradle`에 다음을 추가합니다:

```gradle
dependencies {
    implementation project(':common-exception')
}
```

### 2. 예외 발생

비즈니스 로직에서 구체적인 예외를 발생시킵니다:

```java
@Service
public class UserService {
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
    
    public void createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        // 사용자 생성 로직
    }
}
```

### 3. 전역 예외 처리

`GlobalExceptionHandler`가 자동으로 모든 예외를 처리합니다. 별도의 설정이 필요하지 않습니다.

### 4. 예외 로깅

예외가 발생하면 `ExceptionLoggingService`가 자동으로 MongoDB Atlas에 예외 로그를 저장합니다.

**예외 로그 구조**:
```json
{
  "source": "READ",
  "exceptionType": "com.tech.n.ai.common.exception.exception.ResourceNotFoundException",
  "message": "사용자를 찾을 수 없습니다.",
  "stackTrace": "...",
  "context": {
    "module": "api-auth",
    "method": "GET",
    "parameters": {
      "id": "123"
    },
    "userId": "user-123",
    "requestId": "req-456"
  },
  "occurredAt": "2026-01-15T10:30:00Z",
  "severity": "MEDIUM"
}
```

## 에러 코드 매핑

`GlobalExceptionHandler`는 에러 코드를 HTTP 상태 코드로 자동 매핑합니다:

| 에러 코드 | HTTP 상태 코드 | 예외 클래스 |
|----------|---------------|------------|
| 4000 | 400 Bad Request | BusinessException |
| 4001 | 401 Unauthorized | UnauthorizedException |
| 4003 | 403 Forbidden | ForbiddenException |
| 4004 | 404 Not Found | ResourceNotFoundException |
| 4005 | 409 Conflict | ConflictException |
| 4006 | 400 Bad Request | MethodArgumentNotValidException |
| 4029 | 429 Too Many Requests | RateLimitExceededException |
| 5000 | 500 Internal Server Error | Exception (기타) |
| 5003 | 503 Service Unavailable | ExternalApiException |

## 참고 자료

### 설계서

- `docs/step2/4. error-handling-strategy-design.md`: 에러 핸들링 전략 설계서

### 공식 문서

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/reference/)
- [Spring Data MongoDB 공식 문서](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [Spring @Async 공식 문서](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)

---

**작성일**: 2026-01-XX  
**버전**: 0.0.1-SNAPSHOT

