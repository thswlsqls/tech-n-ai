# Agent API HTTP 테스트 파일

이 디렉토리는 **IntelliJ IDEA 내장 HTTP Client**를 사용하여 Emerging Tech Agent API를 테스트하기 위한 `.http` 파일들을 포함하고 있습니다.

별도의 Postman이나 다른 도구 없이 **IDE 내에서 바로 API 테스트**가 가능합니다.

## 파일 구조

```
http/
├── README.md                               # 이 파일
├── http-client.env.json                    # 환경별 공개 변수 (baseUrl, userId 등)
├── http-client.private.env.json.template   # 환경별 비공개 변수 템플릿
├── http-client.private.env.json            # 환경별 비공개 변수 (Git 제외)
├── .gitignore                              # Git 제외 파일 목록
└── 01-agent-run.http                       # Agent 수동 실행 API 테스트
```

## IntelliJ HTTP Client 사용 방법

### 1. 시작하기

1. IntelliJ IDEA에서 `.http` 파일 열기
2. 파일 상단의 환경 선택 드롭다운에서 `local`, `dev`, `prod` 중 선택
3. 각 요청 옆의 **실행 버튼** 클릭 또는 `Ctrl+Enter` (Mac: `Cmd+Enter`)

### 2. 환경 설정

환경별 변수는 `http-client.env.json` 파일에서 관리됩니다:

```json
{
  "local": {
    "baseUrl": "http://localhost:8086",
    "gatewayUrl": "http://localhost:8081",
    "userId": "admin-user-001"
  }
}
```

필요 시 `http-client.private.env.json`에서 userId를 오버라이드할 수 있습니다:

```json
{
  "local": {
    "userId": "YOUR_ADMIN_USER_ID_HERE"
  }
}
```

### 3. 테스트 순서

#### 기본 플로우 (권장)
1. **01-agent-run.http** - Agent 수동 실행 테스트

## API 엔드포인트 목록

| 파일 | 메서드 | 엔드포인트 | 설명 | 인증 |
|------|--------|-----------|------|------|
| 01-agent-run.http | POST | `/api/v1/agent/run` | Emerging Tech Agent 수동 실행 | Gateway JWT (x-user-id 헤더) |

## 인증 방식

Agent API는 **Gateway에서 JWT ADMIN 역할 검증** 후 `x-user-id` 헤더를 주입하는 방식을 사용합니다.

직접 Agent API를 호출할 때는 `x-user-id` 헤더를 수동으로 설정합니다:

```http
POST /api/v1/agent/run
Content-Type: application/json
x-user-id: admin-user-001

{
  "goal": "AI 업데이트 정보를 수집해주세요."
}
```

## 테스트 케이스

### 01-agent-run.http (Agent 수동 실행) - 39개 테스트

#### 정상 케이스 (Success Cases) - 8개
- 기본 Agent 실행 (sessionId 자동 생성, 전체 응답 구조 검증)
- sessionId 지정하여 실행
- 다양한 goal 테스트 (OpenAI, Anthropic, 여러 AI 도구)
- 긴 goal 메시지 테스트
- 특수 문자가 포함된 goal 테스트
- 한글 goal 테스트

#### 실패 케이스 (Failure Cases) - 11개
- goal 필드 누락 (4006 VALIDATION_ERROR 검증)
- 빈 goal, 공백 goal, null goal (@NotBlank 검증)
- x-user-id 헤더 누락, 빈 x-user-id
- 잘못된 Content-Type (415)
- 빈 요청 본문
- 잘못된 JSON 형식
- 존재하지 않는 엔드포인트 (4004 NOT_FOUND 검증)
- 빈 JSON 객체 (goal 누락)

#### 엣지 케이스 (Edge Cases) - 20개
- 빈/공백/null sessionId (자동 생성 확인)
- sessionId 필드 없이 goal만 전달
- 긴 sessionId, 특수 문자/유니코드 sessionId
- 추가 필드가 포함된 요청 (무시되어야 함)
- 동시 요청 시뮬레이션 (같은 sessionId)
- 매우 짧은 goal, 이모지 포함, 줄바꿈 포함
- 잘못된 HTTP Method (GET, PUT, DELETE, PATCH → 405)
- Content-Type에 charset 포함
- 다른 x-user-id로 실행
- Agent 내부 실패 시 응답 구조 확인

## 요청/응답 형식

### AgentRunRequest (Agent 실행 요청)
```json
{
  "goal": "실행 목표 (필수, @NotBlank)",
  "sessionId": "세션 식별자 (선택, 미지정 시 자동 생성)"
}
```

### 성공 응답 - ApiResponse\<AgentExecutionResult\>
```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {
    "success": true,
    "summary": "실행 결과 요약",
    "toolCallCount": 5,
    "analyticsCallCount": 2,
    "executionTimeMs": 12500,
    "errors": []
  }
}
```

### 유효성 검증 에러 응답 (400)
```json
{
  "code": "4006",
  "messageCode": {
    "code": "VALIDATION_ERROR",
    "text": "유효성 검증에 실패했습니다."
  },
  "data": {
    "goal": "goal은 필수입니다."
  }
}
```

### 404 에러 응답
```json
{
  "code": "4004",
  "messageCode": {
    "code": "NOT_FOUND",
    "text": "요청한 리소스를 찾을 수 없습니다."
  }
}
```

## 유용한 팁

### 1. 단축키
- `Ctrl+Enter` (Mac: `Cmd+Enter`): 현재 요청 실행
- `Ctrl+\` (Mac: `Cmd+\`): 최근 실행한 요청 재실행
- `Alt+Enter`: 빠른 액션 (환경 전환 등)

### 2. 여러 요청 순차 실행
파일 내 모든 요청을 순서대로 실행하려면:
1. 파일 상단 실행 버튼 옆 드롭다운 클릭
2. "Run All Requests in File" 선택

### 3. 동적 변수 사용
```http
POST {{baseUrl}}/api/v1/agent/run
X-Request-Id: {{$uuid}}
X-Timestamp: {{$timestamp}}
```

## 문제 해결

### 400 Bad Request
- `x-user-id` 헤더가 누락된 경우
- goal 누락, 빈 goal 등 유효성 검증 실패
- 잘못된 JSON 형식

### 404 Not Found
- 존재하지 않는 엔드포인트에 요청한 경우

### 405 Method Not Allowed
- POST 외 다른 HTTP Method 사용 (GET, PUT, DELETE, PATCH)

### 415 Unsupported Media Type
- Content-Type이 application/json이 아닌 경우

## 체크리스트

시작하기 전에 확인하세요:

- [ ] IntelliJ IDEA (Community 또는 Ultimate) 설치
- [ ] Agent API 서버가 실행 중 (`http://localhost:8087`)
- [ ] 환경 선택 (local, dev, prod)
- [ ] 첫 번째 요청 실행 테스트

## 참고 자료

- [IntelliJ HTTP Client 공식 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [HTTP 요청 응답 핸들러 스크립트](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#response-handler-scripts)
- [환경 변수 설정 방법](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#environment-variables)
