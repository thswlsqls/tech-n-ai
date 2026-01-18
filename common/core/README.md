# Common Core Module

공통 핵심 모듈로, 프로젝트 전반에서 사용되는 기본 기능을 제공합니다.

## 개요

`common-core` 모듈은 모든 API 모듈에서 공통으로 사용되는 핵심 기능을 제공합니다. 이 모듈은 다른 모든 모듈의 기반이 되며, 표준화된 응답 형식, 예외 처리, 상수 정의, 유틸리티 클래스 등을 포함합니다.

## 주요 기능

### 1. 표준 API 응답 형식

모든 API 응답이 일관된 형식을 따르도록 표준화된 응답 래퍼를 제공합니다.

- **ApiResponse**: 성공/에러 응답을 통합하는 제네릭 응답 래퍼
- **MessageCode**: 국제화(i18n) 지원을 위한 메시지 코드 객체
- **PageData**: 페이징이 필요한 리스트 응답을 위한 페이징 데이터 객체

**참고 설계서**: `docs/step2/3. api-response-format-design.md`

### 2. 예외 처리 기반 클래스

모든 커스텀 예외의 부모 클래스를 제공합니다.

- **BaseException**: 모든 커스텀 예외의 부모 클래스
- **BusinessException**: 비즈니스 로직 위반 시 사용하는 예외

**참고 설계서**: `docs/step2/4. error-handling-strategy-design.md`

### 3. 상수 정의

프로젝트 전반에서 사용되는 상수를 정의합니다.

- **ApiConstants**: API 관련 상수 (버전, 경로, 헤더 등)
- **ErrorCodeConstants**: 에러 코드 및 메시지 코드 상수

### 4. 유틸리티 클래스

공통으로 사용되는 유틸리티 메서드를 제공합니다.

- **DateUtils**: 날짜/시간 포맷팅 및 파싱 유틸리티
- **StringUtils**: 문자열 처리 유틸리티
- **ValidationUtils**: 데이터 검증 유틸리티 (이메일, 비밀번호 등)

### 5. Redis 설정

Redis 연결 및 템플릿 설정을 제공합니다.

- **RedisConfig**: RedisTemplate 빈 설정

## 주요 컴포넌트

### DTO (Data Transfer Object)

#### ApiResponse

표준 API 응답 래퍼입니다.

```java
// 성공 응답
ApiResponse<User> response = ApiResponse.success(user);

// 에러 응답
ApiResponse<Void> errorResponse = ApiResponse.error(
    ErrorCodeConstants.NOT_FOUND,
    new MessageCode(ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND, "리소스를 찾을 수 없습니다.")
);
```

#### MessageCode

국제화 지원을 위한 메시지 코드 객체입니다.

```java
MessageCode messageCode = new MessageCode("SUCCESS", "성공");
MessageCode successCode = MessageCode.success();
```

#### PageData

페이징이 필요한 리스트 응답을 위한 페이징 데이터 객체입니다.

```java
PageData<User> pageData = PageData.of(
    pageSize,
    pageNumber,
    totalSize,
    userList
);
```

### 예외 클래스

#### BaseException

모든 커스텀 예외의 부모 클래스입니다.

```java
public class BaseException extends RuntimeException {
    private final String errorCode;
    private final String messageCode;
}
```

#### BusinessException

비즈니스 로직 위반 시 사용하는 예외입니다.

```java
throw new BusinessException(
    ErrorCodeConstants.CONFLICT,
    ErrorCodeConstants.MESSAGE_CODE_CONFLICT,
    "이미 사용 중인 이메일입니다."
);
```

### 상수 클래스

#### ApiConstants

API 관련 상수를 정의합니다.

```java
String apiBasePath = ApiConstants.API_BASE_PATH; // "/api/v1"
String authHeader = ApiConstants.HEADER_AUTHORIZATION; // "Authorization"
```

#### ErrorCodeConstants

에러 코드 및 메시지 코드 상수를 정의합니다.

```java
String notFoundCode = ErrorCodeConstants.NOT_FOUND; // "4004"
String notFoundMessageCode = ErrorCodeConstants.MESSAGE_CODE_NOT_FOUND; // "NOT_FOUND"
```

### 유틸리티 클래스

#### DateUtils

날짜/시간 포맷팅 및 파싱 유틸리티입니다.

```java
// 포맷팅
String dateStr = DateUtils.format(LocalDate.now()); // "2026-01-15"
String dateTimeStr = DateUtils.format(LocalDateTime.now()); // "2026-01-15T10:30:00"

// 파싱
LocalDate date = DateUtils.parseDate("2026-01-15");
LocalDateTime dateTime = DateUtils.parseDateTime("2026-01-15T10:30:00");
```

#### StringUtils

문자열 처리 유틸리티입니다.

```java
boolean isEmpty = StringUtils.isEmpty(str);
boolean isBlank = StringUtils.isBlank(str);
String trimmed = StringUtils.trim(str);
String defaultStr = StringUtils.defaultString(str, "default");
```

#### ValidationUtils

데이터 검증 유틸리티입니다.

```java
boolean isValidEmail = ValidationUtils.isValidEmail("user@example.com");
boolean isValidPassword = ValidationUtils.isValidPassword("password123");
```

## 의존성

### 주요 의존성

- **Spring Boot Starter**: Spring Boot 기본 기능
- **Spring Boot Starter Actuator**: 모니터링 및 헬스 체크
- **Spring Boot Starter Data Redis**: Redis 연동
- **Spring Boot Starter Data Redis Reactive**: Reactive Redis 연동
- **Spring Boot Starter WebMVC**: Spring MVC 지원
- **Spring Boot Starter WebFlux**: Spring WebFlux 지원
- **Spring Boot Starter Validation**: 데이터 검증 지원
- **Spring Boot Starter RestClient**: REST 클라이언트 지원
- **Spring Boot Starter WebClient**: WebClient 지원
- **Spring Boot Starter OpenTelemetry**: 분산 추적 지원

### 런타임 의존성

- **Micrometer Registry Datadog**: Datadog 메트릭 수집
- **Micrometer Registry Prometheus**: Prometheus 메트릭 수집

## 사용 방법

### 1. 의존성 추가

다른 모듈에서 `common-core` 모듈을 사용하려면 `build.gradle`에 다음을 추가합니다:

```gradle
dependencies {
    implementation project(':common-core')
}
```

### 2. API 응답 사용

컨트롤러에서 표준 응답 형식을 사용합니다:

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
```

### 3. 예외 처리

비즈니스 로직에서 예외를 발생시킵니다:

```java
@Service
public class UserService {
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
```

### 4. 유틸리티 사용

유틸리티 클래스를 사용하여 공통 기능을 수행합니다:

```java
// 날짜 포맷팅
String formattedDate = DateUtils.format(LocalDate.now());

// 문자열 검증
if (StringUtils.isBlank(email)) {
    throw new ValidationException("이메일은 필수입니다.");
}

// 이메일 검증
if (!ValidationUtils.isValidEmail(email)) {
    throw new ValidationException("유효하지 않은 이메일 형식입니다.");
}
```

## 설정

### application-common-core.yml

공통 설정 파일에서 Redis 및 기타 공통 설정을 관리합니다.

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

## 참고 자료

### 설계서

- `docs/step2/3. api-response-format-design.md`: API 응답 형식 설계서
- `docs/step2/4. error-handling-strategy-design.md`: 에러 핸들링 전략 설계서

### 공식 문서

- [Spring Boot 공식 문서](https://docs.spring.io/spring-boot/reference/)
- [Spring Data Redis 공식 문서](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Jakarta Bean Validation 공식 문서](https://beanvalidation.org/)
- [Java 8+ java.time 패키지 공식 문서](https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)

---

**작성일**: 2026-01-XX  
**버전**: 0.0.1-SNAPSHOT

