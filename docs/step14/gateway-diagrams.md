# API Gateway Mermaid 다이어그램

이 문서는 API Gateway의 아키텍처와 동작을 시각화한 Mermaid 다이어그램 모음입니다.

## 1. 전체 아키텍처 다이어그램

```mermaid
graph TB
    subgraph "Client Layer"
        WEB[Web Client]
        MOBILE[Mobile Client]
    end

    subgraph "Load Balancer"
        ALB[AWS ALB<br/>600초 timeout]
    end

    subgraph "API Gateway Layer"
        GATEWAY[API Gateway<br/>Spring Cloud Gateway<br/>Port: 8081]
        GATEWAY_COMPONENTS[Gateway Components]
        GATEWAY_COMPONENTS --> JWT_FILTER[JWT 인증 필터]
        GATEWAY_COMPONENTS --> CORS[CORS 처리]
        GATEWAY_COMPONENTS --> ROUTER[라우터]
        GATEWAY_COMPONENTS --> EXCEPTION_HANDLER[예외 처리]
    end

    subgraph "API Services Layer"
        AUTH_API[API Auth<br/>Port: 8082<br/>인증 불필요]
        ARCHIVE_API[API Archive<br/>Port: 8083<br/>인증 필요]
        CONTEST_API[API Contest<br/>Port: 8084<br/>공개 API]
        NEWS_API[API News<br/>Port: 8085<br/>공개 API]
        CHATBOT_API[API Chatbot<br/>Port: 8086<br/>인증 필요]
    end

    WEB --> ALB
    MOBILE --> ALB
    ALB --> GATEWAY

    GATEWAY -->|/api/v1/auth/**| AUTH_API
    GATEWAY -->|/api/v1/archive/**| ARCHIVE_API
    GATEWAY -->|/api/v1/contest/**| CONTEST_API
    GATEWAY -->|/api/v1/news/**| NEWS_API
    GATEWAY -->|/api/v1/chatbot/**| CHATBOT_API

    style GATEWAY fill:#e0e7ff,stroke:#6366f1,stroke-width:3px,color:#312e81
    style AUTH_API fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    style ARCHIVE_API fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    style CONTEST_API fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    style NEWS_API fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    style CHATBOT_API fill:#e0e7ff,stroke:#6366f1,stroke-width:2px,color:#312e81
    style ALB fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
```

## 2. 요청 처리 시퀀스 다이어그램

### 2.1 인증이 필요한 요청 처리

```mermaid
sequenceDiagram
    participant Client as 클라이언트<br/>(Web/Mobile)
    participant ALB as AWS ALB<br/>(600초 timeout)
    participant Gateway as API Gateway<br/>(Spring Cloud Gateway)
    participant JWTFilter as JWT 인증 필터
    participant JWTProvider as JwtTokenProvider<br/>(common-security)
    participant ApiServer as API 서버<br/>(archive/chatbot)

    Note over Client, ApiServer: 인증이 필요한 요청 처리

    Client->>ALB: HTTP 요청<br/>GET /api/v1/archive<br/>Authorization: Bearer {JWT_TOKEN}
    ALB->>Gateway: 요청 전달
    
    Gateway->>Gateway: 라우팅 규칙 매칭<br/>(Path=/api/v1/archive/**)
    
    Gateway->>JWTFilter: JWT 인증 필터 실행
    JWTFilter->>JWTFilter: 경로 확인<br/>(인증 필요 경로)
    JWTFilter->>JWTFilter: JWT 토큰 추출<br/>(Authorization 헤더)
    
    alt 토큰 없음
        JWTFilter->>Client: 401 Unauthorized<br/>(ApiResponse 형식)
    else 토큰 있음
        JWTFilter->>JWTProvider: JWT 토큰 검증<br/>(validateToken)
        
        alt 토큰 무효
            JWTProvider-->>JWTFilter: 검증 실패
            JWTFilter->>Client: 401 Unauthorized<br/>(ApiResponse 형식)
        else 토큰 유효
            JWTProvider-->>JWTFilter: 검증 성공
            JWTFilter->>JWTProvider: 사용자 정보 추출<br/>(getPayloadFromToken)
            JWTProvider-->>JWTFilter: JwtTokenPayload<br/>(userId, email, role)
            JWTFilter->>JWTFilter: 헤더 주입<br/>(x-user-id, x-user-email, x-user-role)
            JWTFilter->>Gateway: 인증 성공
            Gateway->>ApiServer: 인증된 요청 전달<br/>(사용자 정보 헤더 포함)
            ApiServer->>Gateway: API 응답<br/>(200 OK)
            Gateway->>Gateway: CORS 헤더 추가
            Gateway->>ALB: 최종 응답
            ALB->>Client: 응답 전달
        end
    end
```

### 2.2 인증이 불필요한 요청 처리

```mermaid
sequenceDiagram
    participant Client as 클라이언트<br/>(Web/Mobile)
    participant ALB as AWS ALB
    participant Gateway as API Gateway<br/>(Spring Cloud Gateway)
    participant JWTFilter as JWT 인증 필터
    participant ApiServer as API 서버<br/>(contest/news)

    Note over Client, ApiServer: 인증이 불필요한 요청 처리

    Client->>ALB: HTTP 요청<br/>GET /api/v1/contest
    ALB->>Gateway: 요청 전달
    
    Gateway->>Gateway: 라우팅 규칙 매칭<br/>(Path=/api/v1/contest/**)
    
    Gateway->>JWTFilter: JWT 인증 필터 실행
    JWTFilter->>JWTFilter: 경로 확인<br/>(공개 API 경로)
    JWTFilter->>Gateway: 인증 필터 우회<br/>(공개 API)
    
    Gateway->>ApiServer: 요청 전달
    ApiServer->>Gateway: API 응답<br/>(200 OK)
    Gateway->>Gateway: CORS 헤더 추가
    Gateway->>ALB: 최종 응답
    ALB->>Client: 응답 전달
```

### 2.3 인증 서버 요청 처리

```mermaid
sequenceDiagram
    participant Client as 클라이언트<br/>(Web/Mobile)
    participant ALB as AWS ALB
    participant Gateway as API Gateway<br/>(Spring Cloud Gateway)
    participant JWTFilter as JWT 인증 필터
    participant AuthService as API Auth<br/>(인증 서버)

    Note over Client, AuthService: 인증 서버 요청 처리

    Client->>ALB: HTTP 요청<br/>POST /api/v1/auth/login<br/>{email, password}
    ALB->>Gateway: 요청 전달
    
    Gateway->>Gateway: 라우팅 규칙 매칭<br/>(Path=/api/v1/auth/**)
    
    Gateway->>JWTFilter: JWT 인증 필터 실행
    JWTFilter->>JWTFilter: 경로 확인<br/>(인증 서버 경로)
    JWTFilter->>Gateway: 인증 필터 우회<br/>(인증 서버 경로)
    
    Gateway->>AuthService: 인증 요청 전달
    AuthService->>AuthService: 이메일/비밀번호 검증
    AuthService->>AuthService: JWT 토큰 생성<br/>(accessToken, refreshToken)
    AuthService->>Gateway: JWT 토큰 응답<br/>(200 OK, ApiResponse)
    Gateway->>Gateway: CORS 헤더 추가
    Gateway->>ALB: 최종 응답
    ALB->>Client: 응답 전달<br/>(JWT 토큰 포함)
```

## 3. Gateway 내부 구조 다이어그램

```mermaid
flowchart TB
    subgraph GA["Gateway Application"]
        APP["GatewayApplication<br/>@SpringBootApplication"]
    end

    subgraph CL["Configuration Layer"]
        GATEWAY_CONFIG["GatewayConfig<br/>라우팅 설정<br/>GlobalFilter 등록"]
    end

    subgraph FL["Filter Layer"]
        JWT_FILTER["JwtAuthenticationGatewayFilter<br/>JWT 토큰 검증<br/>사용자 정보 헤더 주입"]
        CORS_FILTER["CORS Filter<br/>Global CORS 설정<br/>DedupeResponseHeader"]
    end

    subgraph EH["Exception Handling"]
        EXCEPTION_HANDLER["ApiGatewayExceptionHandler<br/>WebExceptionHandler<br/>Reactive 기반 예외 처리"]
    end

    subgraph CM["Common Modules"]
        COMMON_CORE["common-core<br/>ApiResponse<br/>MessageCode<br/>ErrorCodeConstants"]
        COMMON_SECURITY["common-security<br/>JwtTokenProvider<br/>JwtTokenPayload"]
    end

    subgraph CF["Configuration Files"]
        APP_YML["application.yml<br/>라우팅 설정<br/>연결 풀 설정<br/>CORS 설정"]
        APP_LOCAL["application-local.yml<br/>로컬 환경 설정"]
        APP_DEV["application-dev.yml<br/>개발 환경 설정"]
        APP_BETA["application-beta.yml<br/>베타 환경 설정"]
        APP_PROD["application-prod.yml<br/>운영 환경 설정"]
    end

    APP --> GATEWAY_CONFIG
    GATEWAY_CONFIG --> JWT_FILTER
    GATEWAY_CONFIG --> CORS_FILTER
    APP --> EXCEPTION_HANDLER
    
    JWT_FILTER --> COMMON_SECURITY
    EXCEPTION_HANDLER --> COMMON_CORE
    
    APP --> APP_YML
    APP --> APP_LOCAL
    APP --> APP_DEV
    APP --> APP_BETA
    APP --> APP_PROD

    style APP fill:#e0e7ff,stroke:#6366f1,stroke-width:3px,color:#312e81
    style GATEWAY_CONFIG fill:#dbeafe,stroke:#3b82f6,stroke-width:2px,color:#1e3a8a
    style JWT_FILTER fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
    style CORS_FILTER fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
    style EXCEPTION_HANDLER fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#991b1b
    style COMMON_CORE fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
    style COMMON_SECURITY fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
```

## 4. 라우팅 규칙 플로우 다이어그램

```mermaid
flowchart TD
    START["클라이언트 요청"]
    ALB["AWS ALB"]
    GATEWAY["API Gateway"]
    
    ROUTE_MATCH{"라우팅 규칙<br/>매칭"}
    
    AUTH_PATH["/api/v1/auth/**"]
    ARCHIVE_PATH["/api/v1/archive/**"]
    CONTEST_PATH["/api/v1/contest/**"]
    NEWS_PATH["/api/v1/news/**"]
    CHATBOT_PATH["/api/v1/chatbot/**"]
    
    JWT_CHECK{"인증 필요<br/>경로 확인"}
    
    TOKEN_EXTRACT["JWT 토큰 추출"]
    TOKEN_VALIDATE["JWT 토큰 검증"]
    HEADER_INJECT["사용자 정보<br/>헤더 주입"]
    
    AUTH_SERVICE["API Auth<br/>서버"]
    ARCHIVE_SERVICE["API Archive<br/>서버"]
    CONTEST_SERVICE["API Contest<br/>서버"]
    NEWS_SERVICE["API News<br/>서버"]
    CHATBOT_SERVICE["API Chatbot<br/>서버"]
    
    UNAUTHORIZED["401 Unauthorized"]
    CORS_ADD["CORS 헤더 추가"]
    RESPONSE["응답 반환"]

    START --> ALB
    ALB --> GATEWAY
    GATEWAY --> ROUTE_MATCH
    
    ROUTE_MATCH -->|매칭| AUTH_PATH
    ROUTE_MATCH -->|매칭| ARCHIVE_PATH
    ROUTE_MATCH -->|매칭| CONTEST_PATH
    ROUTE_MATCH -->|매칭| NEWS_PATH
    ROUTE_MATCH -->|매칭| CHATBOT_PATH
    
    AUTH_PATH --> AUTH_SERVICE
    AUTH_PATH --> CORS_ADD
    
    ARCHIVE_PATH --> JWT_CHECK
    CHATBOT_PATH --> JWT_CHECK
    
    CONTEST_PATH --> CONTEST_SERVICE
    NEWS_PATH --> NEWS_SERVICE
    
    JWT_CHECK -->|인증 필요| TOKEN_EXTRACT
    TOKEN_EXTRACT -->|토큰 없음| UNAUTHORIZED
    TOKEN_EXTRACT -->|토큰 있음| TOKEN_VALIDATE
    
    TOKEN_VALIDATE -->|검증 실패| UNAUTHORIZED
    TOKEN_VALIDATE -->|검증 성공| HEADER_INJECT
    
    HEADER_INJECT --> ARCHIVE_SERVICE
    HEADER_INJECT --> CHATBOT_SERVICE
    
    AUTH_SERVICE --> CORS_ADD
    ARCHIVE_SERVICE --> CORS_ADD
    CONTEST_SERVICE --> CORS_ADD
    NEWS_SERVICE --> CORS_ADD
    CHATBOT_SERVICE --> CORS_ADD
    UNAUTHORIZED --> RESPONSE
    CORS_ADD --> RESPONSE

    style GATEWAY fill:#e0e7ff,stroke:#6366f1,stroke-width:3px,color:#312e81
    style JWT_CHECK fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
    style TOKEN_VALIDATE fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
    style UNAUTHORIZED fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#991b1b
    style CORS_ADD fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
```

## 5. 토큰 갱신 시나리오 다이어그램

### 5.1 Access Token 만료 (클라이언트 자동 처리)

```mermaid
sequenceDiagram
    participant Client as 클라이언트<br/>(프론트엔드/앱)
    participant Gateway as API Gateway
    participant AuthService as API Auth<br/>(인증 서버)
    participant ApiServer as API 서버<br/>(archive/chatbot)

    Note over Client, ApiServer: 1단계: Archive 요청 (만료된 Access Token)

    Client->>Gateway: GET /api/v1/archive<br/>Authorization: Bearer {만료된_토큰}
    Gateway->>Gateway: JWT 인증 필터 실행
    Gateway->>Gateway: 토큰 만료 감지
    Gateway->>Client: 401 Unauthorized<br/>{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}
    Note over Client: 401 응답 감지

    Note over Client, ApiServer: 2단계: Refresh Token으로 자동 갱신 요청

    Client->>Gateway: POST /api/v1/auth/refresh<br/>{"refreshToken": "{유효한_refresh_token}"}<br/>(자동 요청)
    Gateway->>AuthService: 인증 필터 우회 → Auth 서버로 라우팅
    AuthService->>Gateway: 200 OK<br/>{"code": "2000", "data": {"accessToken": "{새_토큰}", "refreshToken": "{새_refresh_토큰}", ...}}
    Gateway->>Client: 200 OK (새 Access Token + Refresh Token 발급)
    Note over Client: 새 Access Token 저장

    Note over Client, ApiServer: 3단계: 원래 요청 자동 재시도

    Client->>Gateway: GET /api/v1/archive<br/>Authorization: Bearer {새_토큰}<br/>(자동 재시도)
    Gateway->>Gateway: JWT 인증 필터 실행
    Gateway->>Gateway: JWT 검증 성공
    Gateway->>ApiServer: 인증된 요청 전달<br/>(사용자 정보 헤더 포함)
    ApiServer->>Gateway: 200 OK (정상 응답)
    Gateway->>Client: 200 OK
```

### 5.2 Refresh Token도 만료 (사용자 개입 필요)

```mermaid
sequenceDiagram
    participant Client as 클라이언트<br/>(프론트엔드/앱)
    participant User as 사용자
    participant Gateway as API Gateway
    participant AuthService as API Auth<br/>(인증 서버)
    participant ApiServer as API 서버<br/>(archive/chatbot)

    Note over Client, ApiServer: 1단계: Archive 요청 (만료된 Access Token)

    Client->>Gateway: GET /api/v1/archive<br/>Authorization: Bearer {만료된_토큰}
    Gateway->>Gateway: JWT 인증 필터 실행
    Gateway->>Gateway: 토큰 만료 감지
    Gateway->>Client: 401 Unauthorized<br/>{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}
    Note over Client: 401 응답 감지

    Note over Client, ApiServer: 2단계: Refresh Token으로 자동 갱신 시도

    Client->>Gateway: POST /api/v1/auth/refresh<br/>{"refreshToken": "{만료된_또는_없는_refresh_token}"}<br/>(자동 시도)
    Gateway->>AuthService: 인증 필터 우회 → Auth 서버로 라우팅
    AuthService->>Gateway: 401 Unauthorized<br/>{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "유효하지 않은 Refresh Token입니다."}}
    Gateway->>Client: 401 Unauthorized
    Note over Client: Refresh Token도 만료됨을 감지

    Note over Client, User: 3단계: 사용자 개입 (로그인 화면 표시)

    Client->>User: 로그인 화면 표시
    User->>Client: 이메일/비밀번호 입력<br/>(사용자 개입)

    Note over Client, ApiServer: 4단계: 로그인 요청 (사용자 입력 후)

    Client->>Gateway: POST /api/v1/auth/login<br/>{"email": "user@example.com", "password": "password"}<br/>(사용자 입력 후 요청)
    Gateway->>AuthService: 인증 필터 우회 → Auth 서버로 라우팅
    AuthService->>Gateway: 200 OK<br/>{"code": "2000", "data": {"accessToken": "{새_토큰}", "refreshToken": "{새_refresh_토큰}", ...}}
    Gateway->>Client: 200 OK (새 Access Token + Refresh Token 발급)
    Note over Client: 새 Access Token 저장

    Note over Client, ApiServer: 5단계: 원래 요청 자동 재시도

    Client->>Gateway: GET /api/v1/archive<br/>Authorization: Bearer {새_토큰}<br/>(자동 재시도)
    Gateway->>Gateway: JWT 인증 필터 실행
    Gateway->>Gateway: JWT 검증 성공
    Gateway->>ApiServer: 인증된 요청 전달<br/>(사용자 정보 헤더 포함)
    ApiServer->>Gateway: 200 OK (정상 응답)
    Gateway->>Client: 200 OK
```

## 6. 모듈 의존성 다이어그램

```mermaid
graph TB
    subgraph "API Gateway Module"
        GATEWAY[api-gateway]
    end

    subgraph "Common Modules"
        COMMON_CORE[common-core<br/>ApiResponse<br/>MessageCode<br/>ErrorCodeConstants]
        COMMON_SECURITY[common-security<br/>JwtTokenProvider<br/>JwtTokenPayload]
        COMMON_KAFKA[common-kafka<br/>Kafka 이벤트<br/>선택적]
    end

    subgraph "Domain Modules"
        DOMAIN_AURORA[domain-aurora<br/>Aurora MySQL<br/>선택적]
        DOMAIN_MONGODB[domain-mongodb<br/>MongoDB Atlas<br/>선택적]
    end

    subgraph "External Dependencies"
        SPRING_CLOUD_GATEWAY[Spring Cloud Gateway<br/>Netty 기반]
        REACTOR_NETTY[Reactor Netty<br/>비동기 네트워크]
    end

    GATEWAY --> COMMON_CORE
    GATEWAY --> COMMON_SECURITY
    GATEWAY --> COMMON_KAFKA
    GATEWAY --> DOMAIN_AURORA
    GATEWAY --> DOMAIN_MONGODB
    GATEWAY --> SPRING_CLOUD_GATEWAY
    SPRING_CLOUD_GATEWAY --> REACTOR_NETTY

    style GATEWAY fill:#e0e7ff,stroke:#6366f1,stroke-width:3px,color:#312e81
    style COMMON_CORE fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
    style COMMON_SECURITY fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
    style COMMON_KAFKA fill:#dcfce7,stroke:#22c55e,stroke-width:2px,color:#166534
    style DOMAIN_AURORA fill:#dbeafe,stroke:#3b82f6,stroke-width:2px,color:#1e3a8a
    style DOMAIN_MONGODB fill:#dbeafe,stroke:#3b82f6,stroke-width:2px,color:#1e3a8a
    style SPRING_CLOUD_GATEWAY fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
    style REACTOR_NETTY fill:#fef3c7,stroke:#f59e0b,stroke-width:2px,color:#92400e
```

## 사용 방법

1. **Mermaid Live Editor**: https://mermaid.live 에 접속
2. 위의 다이어그램 코드를 복사하여 붙여넣기
3. 다이어그램이 자동으로 렌더링됩니다
4. 필요시 PNG, SVG, 또는 다이어그램 코드를 다운로드할 수 있습니다

## 참고 문서

- [Gateway 설계서](gateway-design.md)
- [Gateway 구현 계획](gateway-implementation-plan.md)
- [Gateway API 모듈 README](../../api/gateway/README.md)

