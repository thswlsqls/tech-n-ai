# Spring Cloud Gateway 기반 API Gateway 서버 구현 계획

**작성 일시**: 2026-01-XX  
**대상**: API Gateway 서버 구현 계획  
**기술 스택**: Spring Cloud Gateway (Netty 기반), Java 21, Spring Boot 4.0.1

## 목차

1. [작업 목표 분석](#작업-목표-분석)
2. [프로젝트 아키텍처 파악](#프로젝트-아키텍처-파악)
3. [정보 수집 결과](#정보-수집-결과)
4. [기존 프로그램 및 구조 확인](#기존-프로그램-및-구조-확인)
5. [작업 유형별 가이드라인](#작업-유형별-가이드라인)
6. [초기 설계 솔루션](#초기-설계-솔루션)

---

## 작업 목표 분석

### 작업 목표

Spring Cloud Gateway 기반 API Gateway 서버 구현:
- URI 기반 라우팅 규칙 구현 (5개 API 서버: auth, bookmark, contest, news, chatbot)
- JWT 토큰 기반 인증 필터 구현
- 연결 풀 설정 및 Connection reset by peer 방지
- CORS 설정 (환경별 차별화)
- 에러 처리 및 모니터링 구현

### 배경

**인프라 아키텍처**: Client → ALB → Gateway Server → API Servers

**라우팅 흐름**:
- 인증이 필요한 요청 (예: `/api/v1/bookmark/**`, `/api/v1/chatbot/**`):
  - Client → ALB → Gateway → [JWT 인증 필터 (Gateway 내부)] → Bookmark/Chatbot 서버
  - Gateway 내부의 JWT 인증 필터가 토큰을 검증하므로 인증 서버(@api/auth)를 거치지 않음
  - JWT는 stateless이므로 Gateway에서 직접 검증 가능 (common-security 모듈의 JwtTokenProvider 활용)
  - JWT 토큰 만료/무효 시: Gateway는 401 Unauthorized를 반환하고, 인증 서버를 자동으로 호출하지 않음

- 인증 서버 요청 (예: `/api/v1/auth/login`, `/api/v1/auth/refresh`):
  - Client → ALB → Gateway → Auth 서버
  - `/api/v1/auth/**` 경로만 @api/auth 서버로 라우팅

**인증 통합 방안**: 옵션 B 채택 (@api/auth 모듈을 별도 서버로 유지, Gateway에서 JWT 검증만 수행)

### 토큰 갱신 흐름 (사용자 응답 기준)

**시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)**
1. Bookmark 요청 (만료된 토큰) → Gateway: 401 Unauthorized
2. POST /api/v1/auth/refresh (유효한 Refresh Token, 자동 요청) → Gateway → Auth 서버: 200 OK (새 토큰 발급)
3. Bookmark 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Bookmark 서버: 200 OK

**시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)**
1. Bookmark 요청 (만료된 토큰) → Gateway: 401 Unauthorized
2. POST /api/v1/auth/refresh (만료된/없는 Refresh Token, 자동 시도) → Gateway → Auth 서버: 401 Unauthorized
3. 사용자 개입: 로그인 화면 표시, 이메일/비밀번호 입력
4. POST /api/v1/auth/login (사용자 입력 후 요청) → Gateway → Auth 서버: 200 OK (새 토큰 발급)
5. Bookmark 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Bookmark 서버: 200 OK

### 작업 요구사항

- 설계서 엄격 준수: `docs/step17/gateway-design.md`의 모든 설계 사항을 정확히 구현
- 오버엔지니어링 금지: 설계서에 명시되지 않은 기능 추가 금지 (Rate Limiting, Circuit Breaker 등은 명시적 요구 시에만)
- 공식 문서만 참고: Spring Cloud Gateway, Reactor Netty, Spring Boot 공식 문서
- 기존 프로젝트 구조 준수: api/gateway 모듈 구조, 기존 API 모듈 패턴 준수

---

## 프로젝트 아키텍처 파악

### 루트 디렉토리 구조

- **프로젝트 구조**: 멀티 모듈 Gradle 프로젝트
- **모듈 구조**: `api/`, `batch/`, `common/`, `client/`, `domain/`
- **Gateway 모듈**: `api/gateway/`

### 핵심 설정 파일

- **build.gradle**: Spring Boot 4.0.1, Spring Cloud 2025.1.0
- **settings.gradle**: 동적 모듈 검색 구조
- **api/gateway/build.gradle**: 현재 Spring Cloud Gateway 의존성 없음 (추가 필요)

### 모듈 의존성 관계

```
api/gateway
├── common-core          # 공통 유틸리티, ApiResponse 등
├── common-security      # JwtTokenProvider, JWT 검증 로직
├── common-kafka         # Kafka 이벤트 (선택)
├── domain-aurora        # Aurora MySQL 도메인 (선택)
└── domain-mongodb       # MongoDB 도메인 (선택)
```

### 기존 코드 구조

- **GatewayApplication.java**: Spring Boot 메인 클래스 (기본 구조만 존재)
- **ApiGatewayExceptionHandler.java**: 빈 클래스 (구현 필요)
- **설정 파일**: application.yml, application-local.yml, application-dev.yml, application-beta.yml, application-prod.yml (기본 구조만 존재)

---

## 정보 수집 결과

### 설계서 분석

**docs/step17/gateway-design.md**:
- 패키지 구조: `config/`, `filter/`, `common/exception/`, `util/`
- 라우팅 설정: 5개 Route 정의 (auth, bookmark, contest, news, chatbot)
- 인증 필터: `JwtAuthenticationGatewayFilter` 구현 (GatewayFilter 인터페이스)
- 연결 풀 설정: Reactor Netty 연결 풀 설정 값 명시
- CORS 설정: Global CORS, 환경별 차별화, DedupeResponseHeader 필터
- 에러 처리: ErrorWebExceptionHandler 또는 GlobalFilter 활용 (Reactive 기반)

**api/gateway/REF.md**:
- Saturn Gateway Server 아키텍처 참고
- 연결 풀 설정 값 및 근거 설명
- CORS 설정 가이드라인 (중복 헤더 제거)

### 기존 모듈 분석

**common-security 모듈**:
- `JwtTokenProvider`: `validateToken(token)`, `getPayloadFromToken(token)` 메서드 제공
- `JwtTokenPayload`: `userId`, `email`, `role` 필드

**common-core 모듈**:
- `ApiResponse<T>`: 표준 API 응답 래퍼 (`code`, `messageCode`, `message`, `data`)
- `MessageCode`: 메시지 코드 객체 (`code`, `text`)
- `ErrorCodeConstants`: 에러 코드 상수 정의 (`AUTH_FAILED = "4001"` 등)

### 기존 API 모듈 엔드포인트 구조

- **api/auth**: `/api/v1/auth/**`
- **api/bookmark**: `/api/v1/bookmark/**`
- **api/contest**: `/api/v1/contest/**`
- **api/news**: `/api/v1/news/**`
- **api/chatbot**: `/api/v1/chatbot/**`

---

## 기존 프로그램 및 구조 확인

### 현재 구현 상태

**api/gateway 모듈**:
- GatewayApplication.java: 기본 Spring Boot 애플리케이션 클래스만 존재
- ApiGatewayExceptionHandler.java: 빈 클래스 (구현 필요)
- 설정 파일: 기본 구조만 존재 (라우팅, 연결 풀, CORS 설정 없음)
- Spring Cloud Gateway 의존성: 없음 (추가 필요)

**common-security 모듈**:
- JwtTokenProvider: 완전히 구현됨
  - `validateToken(String token)`: boolean 반환 (토큰 유효성 검증)
  - `getPayloadFromToken(String token)`: JwtTokenPayload 반환 (사용자 정보 추출)
- JwtTokenPayload: record 타입 (`userId`, `email`, `role`)

**common-core 모듈**:
- ApiResponse: 완전히 구현됨
  - `ApiResponse.error(String code, MessageCode messageCode)`: 에러 응답 생성
- MessageCode: record 타입 (`code`, `text`)
- ErrorCodeConstants: 에러 코드 상수 정의
  - `AUTH_FAILED = "4001"`
  - `MESSAGE_CODE_AUTH_FAILED = "AUTH_FAILED"`

### 코드 스타일 및 규칙

- **명명 규칙**: camelCase 사용
- **주석 스타일**: JavaDoc 형식
- **에러 처리 패턴**: ApiResponse 형식 사용
- **로깅**: SLF4J 사용 (`@Slf4j`)

---

## 작업 유형별 가이드라인

### Spring Cloud Gateway Tasks

**라우팅 설정 패턴**:
- Route → Predicate → Filter → Backend Service 구조
- Path 기반 라우팅 (Path=/api/v1/auth/** 등)
- 환경별 백엔드 서비스 URL 설정 (Local: localhost, Dev/Beta/Prod: service-name)

**Gateway Filter 패턴**:
- Reactive 기반 (Mono<Void> 반환, ServerWebExchange 활용)
- GatewayFilter 인터페이스 구현 또는 AbstractGatewayFilterFactory 상속
- JwtTokenProvider는 동기 메서드이므로 Gateway Filter에서 직접 사용 가능 (블로킹 호출 허용)
- Route 설정에서 필터 등록 방법: GatewayConfig에서 Bean 등록 또는 application.yml에서 필터 설정

**인증 필터 구현 패턴**:
- 인증 필요/불필요 경로 구분 (`isPublicPath` 메서드)
- JWT 토큰 추출 (Authorization: Bearer {token})
- JWT 토큰 검증 (JwtTokenProvider.validateToken)
- 토큰 만료/무효 시 처리: 401 Unauthorized 반환, 인증 서버를 자동 호출하지 않음 (`handleUnauthorized` 메서드)
- 응답 형식: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
- 사용자 정보 추출 및 헤더 주입 (x-user-id, x-user-email, x-user-role)

**연결 풀 설정 패턴**:
- Reactor Netty 연결 풀 설정 (`spring.cloud.gateway.httpclient.pool.*`)
- Connection reset by peer 방지 설정 (max-idle-time: 30초, max-life-time: 300초)
- 타임아웃 설정 (connection-timeout: 30초, response-timeout: 60초)

**CORS 설정 패턴**:
- Global CORS 설정 (`spring.cloud.gateway.globalcors.*`)
- 환경별 CORS 정책 차별화 (Local/Dev: 개발 편의성, Beta/Prod: 보안 우선)
- 외부 API 연동 시 중복 헤더 제거 (DedupeResponseHeader 필터)

### 에러 처리 Tasks

**예외 처리 전략**:
- Gateway Filter에서 발생하는 예외 처리: `handleUnauthorized` 메서드로 401 응답 반환
- Global 예외 처리:
  - 설계서에는 ApiGatewayExceptionHandler가 명시되어 있으나, Spring Cloud Gateway는 Reactive 기반이므로 @ControllerAdvice 사용 불가
  - ErrorWebExceptionHandler 구현 또는 GlobalFilter 활용하여 예외 처리
  - 설계서의 ApiGatewayExceptionHandler는 참고용이며, 실제 구현은 Reactive 기반으로 수행
- 공통 에러 응답 형식: ApiResponse 형식 (JSON 응답, ServerHttpResponse에 직접 작성)
- HTTP 상태 코드 매핑 (401: 인증 실패, 404: 라우팅 실패, 502: 백엔드 연결 실패, 504: 백엔드 타임아웃, 500: 내부 서버 오류)

**로깅 전략**:
- 환경별 로그 레벨 (Local/Dev: DEBUG, Beta: INFO, Prod: WARN)
- 요청 로깅 (URI, HTTP 메서드, 헤더 - 민감 정보 제외)
- 인증 로깅 (인증 성공/실패, JWT 토큰 검증 결과)
- 라우팅 로깅 (라우팅 규칙 매칭 결과, 백엔드 서버 URL)
- 에러 로깅 (에러 발생 시 상세 스택 트레이스, 에러 코드 및 메시지)

---

## 초기 설계 솔루션

### 1. 의존성 추가

**api/gateway/build.gradle**:
```gradle
dependencies {
    // Spring Cloud Gateway
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    
    // 기존 의존성 유지
    implementation project(':common-core')
    implementation project(':common-security')
    // ...
}
```

### 2. 패키지 구조

```
api/gateway/src/main/java/com/ebson/shrimp/tm/demo/api/gateway/
├── GatewayApplication.java                    # Spring Boot 메인 클래스 (기존)
├── config/
│   ├── GatewayConfig.java                     # Spring Cloud Gateway 라우팅 설정
│   └── CorsConfig.java                        # CORS 설정 (선택, Global CORS로 대체 가능)
├── filter/
│   ├── JwtAuthenticationGatewayFilter.java    # JWT 인증 Gateway Filter
│   └── RequestLoggingFilter.java              # 요청 로깅 필터 (선택)
├── common/
│   └── exception/
│       └── ApiGatewayExceptionHandler.java    # 공통 예외 처리 (ErrorWebExceptionHandler 구현)
└── util/
    └── HeaderUtils.java                       # 헤더 유틸리티 (선택)
```

### 3. 라우팅 설정 설계

**GatewayConfig.java**:
- 5개 Route 정의 (auth, bookmark, contest, news, chatbot)
- Path 기반 라우팅 (Path=/api/v1/{service}/**)
- 환경별 백엔드 서비스 URL 설정 (Local: localhost, Dev/Beta/Prod: service-name)
- JwtAuthenticationGatewayFilter Bean 등록

**application.yml**:
- 기본 라우팅 설정 (환경 변수 사용)
- 연결 풀 설정
- Global CORS 설정
- JWT 설정

**환경별 설정 파일**:
- application-local.yml: localhost URL, DEBUG 로그 레벨
- application-dev.yml: service-name URL, INFO 로그 레벨
- application-beta.yml: service-name URL, WARN 로그 레벨
- application-prod.yml: service-name URL, WARN 로그 레벨

### 4. JWT 인증 필터 설계

**JwtAuthenticationGatewayFilter.java**:
- GatewayFilter 인터페이스 구현
- Reactive 기반 (Mono<Void> 반환, ServerWebExchange 활용)
- `isPublicPath(String path)`: 인증 필요/불필요 경로 구분
  - 인증 불필요: `/api/v1/auth/**`, `/api/v1/contest/**`, `/api/v1/news/**`, `/actuator/**`
  - 인증 필요: `/api/v1/bookmark/**`, `/api/v1/chatbot/**`
- `extractToken(ServerHttpRequest request)`: Authorization 헤더에서 Bearer 토큰 추출
- `validateToken(String token)`: JwtTokenProvider.validateToken 활용
- `handleUnauthorized(ServerWebExchange exchange)`: 401 Unauthorized 반환
  - 응답 형식: `ApiResponse.error(ErrorCodeConstants.AUTH_FAILED, new MessageCode(ErrorCodeConstants.MESSAGE_CODE_AUTH_FAILED, "인증에 실패했습니다."))`
  - JSON 응답 작성 (ObjectMapper 사용, Reactive 방식)
- 사용자 정보 헤더 주입: `x-user-id`, `x-user-email`, `x-user-role`

### 5. 연결 풀 설정 설계

**application.yml**:
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-idle-time: 30000        # 30초 (백엔드 keep-alive 60초보다 짧게)
          max-life-time: 300000       # 5분 (300초)
          max-connections: 500       # 최대 연결 수
          acquire-timeout: 45000       # 연결 획득 타임아웃 (45초)
          pending-acquire-timeout: 60000  # 대기 타임아웃 (60초)
        connection-timeout: 30000     # 연결 타임아웃 (30초)
        response-timeout: 60000       # 응답 타임아웃 (60초)
```

**설정 근거**:
- max-idle-time: 30초 (백엔드 keep-alive 60초보다 짧게 설정하여 Connection reset 에러 방지)
- max-life-time: 300초 (연결의 최대 생명주기)
- connection-timeout: 30초 (백엔드 서버와의 연결 시도 타임아웃)
- response-timeout: 60초 (백엔드 서버의 응답 대기 타임아웃, 백엔드 타임아웃보다 길게 설정)

### 6. CORS 설정 설계

**application.yml (기본 설정)**:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowCredentials: true
            allowedOriginPatterns:
              - "http://localhost:*"
              - "http://127.0.0.1:*"
            allowedHeaders: "*"
            allowedMethods: [GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD]
            maxAge: 3600
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin, RETAIN_LAST
```

**환경별 CORS 정책**:
- Local/Dev: 개발 편의성을 위해 넓은 범위 허용 (localhost, 127.0.0.1 와일드카드)
- Beta/Prod: 보안을 위해 제한적인 도메인만 허용 (구체적 도메인 목록)

### 7. 에러 처리 설계

**ApiGatewayExceptionHandler.java**:
- ErrorWebExceptionHandler 인터페이스 구현 (Reactive 기반)
- @ControllerAdvice 사용 불가 (Spring Cloud Gateway는 Reactive 기반)
- HTTP 상태 코드 매핑:
  - 401: 인증 실패 → `ApiResponse.error(ErrorCodeConstants.AUTH_FAILED, ...)`
  - 404: 라우팅 실패 → `ApiResponse.error(ErrorCodeConstants.NOT_FOUND, ...)`
  - 502: 백엔드 연결 실패 → `ApiResponse.error(ErrorCodeConstants.EXTERNAL_API_ERROR, ...)`
  - 504: 백엔드 타임아웃 → `ApiResponse.error(ErrorCodeConstants.TIMEOUT, ...)`
  - 500: 내부 서버 오류 → `ApiResponse.error(ErrorCodeConstants.INTERNAL_SERVER_ERROR, ...)`
- JSON 응답 작성 (ObjectMapper 사용, Reactive 방식)

**로깅 전략**:
- 환경별 로그 레벨 (Local/Dev: DEBUG, Beta: INFO, Prod: WARN)
- 요청 로깅 (URI, HTTP 메서드, 헤더 - 민감 정보 제외)
- 인증 로깅 (인증 성공/실패, JWT 토큰 검증 결과)
- 라우팅 로깅 (라우팅 규칙 매칭 결과, 백엔드 서버 URL)
- 에러 로깅 (에러 발생 시 상세 스택 트레이스, 에러 코드 및 메시지)

### 8. 설정 파일 설계

**application.yml (기본 설정)**:
- 라우팅 설정 (환경 변수 사용)
- 연결 풀 설정
- Global CORS 설정
- JWT 설정 (common-security 모듈 사용)
- 로깅 설정

**application-local.yml**:
- 프로필: local
- 서버 포트: 8081
- 백엔드 서비스 URL: localhost (8082, 8083, 8084, 8085, 8086)
- 로그 레벨: DEBUG

**application-dev.yml**:
- 프로필: dev
- 백엔드 서비스 URL: service-name (api-auth-service:8080 등)
- 로그 레벨: INFO

**application-beta.yml**:
- 프로필: beta
- 백엔드 서비스 URL: service-name
- 로그 레벨: WARN

**application-prod.yml**:
- 프로필: prod
- 백엔드 서비스 URL: service-name
- 로그 레벨: WARN

### 9. 구현 우선순위

**Phase 1: 기본 설정 및 라우팅** (우선순위: 높음)
- Spring Cloud Gateway 의존성 추가
- 기본 라우팅 설정 (5개 Route)
- Gateway 서버 실행 확인

**Phase 2: JWT 인증 필터 구현** (우선순위: 높음)
- JwtAuthenticationGatewayFilter 구현
- 인증 필요/불필요 경로 구분
- 사용자 정보 헤더 주입

**Phase 3: 연결 풀 설정** (우선순위: 높음)
- HTTP 클라이언트 연결 풀 설정
- Connection reset by peer 방지 설정

**Phase 4: CORS 설정** (우선순위: 중간)
- Global CORS 설정
- 환경별 CORS 정책 설정
- 외부 API 연동 시 중복 헤더 제거 필터

**Phase 5: 에러 처리 및 모니터링** (우선순위: 중간)
- ErrorWebExceptionHandler 구현
- 공통 에러 응답 형식
- 로깅 전략

### 10. 검증 기준

- [ ] 설계서(docs/step17/gateway-design.md)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] 라우팅 설정 설계 완성: 5개 API 서버에 대한 라우팅 규칙 설계
- [ ] JWT 인증 필터 설계 완성: JwtAuthenticationGatewayFilter 클래스 전체 구조 설계
- [ ] 연결 풀 설정 설계 완성: HTTP 클라이언트 연결 풀 설정 값 설계
- [ ] CORS 설정 설계 완성: Global CORS 설정 설계, 환경별 CORS 정책 설계
- [ ] 에러 처리 설계 완성: ErrorWebExceptionHandler 구현 설계
- [ ] 설정 파일 설계 완성: application.yml 기본 설정 설계, 환경별 설정 파일 설계
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음
- [ ] 공식 문서만 참고: Spring Cloud Gateway, Reactor Netty, Spring Boot 공식 문서

---

## 다음 단계

이 초기 설계 솔루션을 기반으로 실제 구현을 진행합니다. 구현 단계는 다음과 같습니다:

1. **의존성 추가**: api/gateway/build.gradle에 Spring Cloud Gateway 의존성 추가
2. **기본 라우팅 설정**: GatewayConfig.java 및 application.yml에 라우팅 설정 추가
3. **JWT 인증 필터 구현**: JwtAuthenticationGatewayFilter.java 구현
4. **연결 풀 설정**: application.yml에 연결 풀 설정 추가
5. **CORS 설정**: application.yml 및 환경별 설정 파일에 CORS 설정 추가
6. **에러 처리 구현**: ApiGatewayExceptionHandler.java 구현 (ErrorWebExceptionHandler)
7. **테스트 및 검증**: 라우팅, 인증, CORS, 에러 처리 테스트

---

**작성 완료일**: 2026-01-XX  
**검토 필요**: 초기 설계 솔루션 검토 및 구현 시작
