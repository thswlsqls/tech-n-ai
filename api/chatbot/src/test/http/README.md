# Chatbot API HTTP 테스트 파일

이 디렉토리는 **IntelliJ IDEA 내장 HTTP Client**를 사용하여 챗봇 API를 테스트하기 위한 `.http` 파일들을 포함하고 있습니다.

별도의 Postman이나 다른 도구 없이 **IDE 내에서 바로 API 테스트**가 가능합니다.

## 파일 구조

```
http/
├── README.md                           # 이 파일
├── http-client.env.json                # 환경별 공개 변수 (baseUrl, chatbotUrl 등)
├── http-client.private.env.json        # 환경별 비공개 변수 (토큰 등) - Git 제외
├── .gitignore                          # Git 제외 파일 목록
├── 01-chat.http                        # 챗봇 채팅 API 테스트
├── 02-sessions-list.http               # 세션 목록 조회 API 테스트
├── 03-session-detail.http              # 세션 상세 조회 API 테스트
├── 04-session-messages.http            # 세션 메시지 목록 조회 API 테스트
└── 05-session-delete.http              # 세션 삭제 API 테스트
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
    "baseUrl": "http://localhost:8080",
    "chatbotUrl": "http://localhost:8084"
  }
}
```

**중요**: `http-client.private.env.json` 파일에 Access Token을 설정해야 합니다:

```json
{
  "local": {
    "accessToken": "YOUR_ACCESS_TOKEN_HERE"
  }
}
```

### 3. Access Token 발급 방법

Chatbot API는 인증이 필요합니다. 먼저 Auth API에서 로그인하여 토큰을 발급받으세요:

1. `api/auth/src/test/http/02-login.http` 실행
2. 응답에서 `accessToken` 값 복사
3. `http-client.private.env.json`의 `accessToken`에 붙여넣기

### 4. 자동 토큰 관리

테스트 스크립트가 자동으로 세션 ID를 저장합니다:

```http
> {%
    // conversationId를 저장하여 후속 요청에서 사용
    client.global.set("conversationId", response.body.data.conversationId);
    client.global.set("testSessionId", response.body.data.content[0].sessionId);
%}
```

### 5. 테스트 순서

#### 기본 플로우 (권장)
1. **01-chat.http** - 채팅으로 새 세션 생성 및 대화
2. **02-sessions-list.http** - 세션 목록 확인
3. **03-session-detail.http** - 특정 세션 상세 조회
4. **04-session-messages.http** - 세션의 메시지 히스토리 조회
5. **05-session-delete.http** - 세션 삭제 테스트

## API 엔드포인트 목록

| 파일 | 메서드 | 엔드포인트 | 설명 | 인증 필요 |
|------|--------|-----------|------|----------|
| 01-chat.http | POST | `/api/v1/chatbot` | 챗봇 채팅 | O |
| 02-sessions-list.http | GET | `/api/v1/chatbot/sessions` | 세션 목록 조회 | O |
| 03-session-detail.http | GET | `/api/v1/chatbot/sessions/{sessionId}` | 세션 상세 조회 | O |
| 04-session-messages.http | GET | `/api/v1/chatbot/sessions/{sessionId}/messages` | 세션 메시지 목록 | O |
| 05-session-delete.http | DELETE | `/api/v1/chatbot/sessions/{sessionId}` | 세션 삭제 | O |

## 테스트 케이스

### 01-chat.http (챗봇 채팅)
- 새 대화 시작 (conversationId 없이)
- 기존 대화 이어서 진행
- 다양한 질문 테스트 (대회/뉴스/일반)
- 실패: 메시지 누락, 빈 메시지, 길이 초과
- 실패: 인증 없음, 잘못된 토큰, 존재하지 않는 세션

### 02-sessions-list.http (세션 목록)
- 기본 페이지네이션 조회
- 다양한 페이지/사이즈 파라미터
- 실패: 인증 오류, 잘못된 파라미터

### 03-session-detail.http (세션 상세)
- 특정 세션 상세 조회
- 실패: 존재하지 않는 세션, 권한 없음

### 04-session-messages.http (메시지 목록)
- 메시지 목록 페이지네이션 조회
- 메시지 데이터 형식 검증
- 실패: 인증 오류, 존재하지 않는 세션

### 05-session-delete.http (세션 삭제)
- 세션 삭제 및 삭제 확인
- 삭제된 세션으로 채팅 시도
- 실패: 존재하지 않는 세션, 중복 삭제, 권한 없음

## 요청/응답 형식

### ChatRequest (채팅 요청)
```json
{
  "message": "질문 내용 (필수, 최대 500자)",
  "conversationId": "세션 ID (선택, 없으면 새 세션 생성)"
}
```

### ChatResponse (채팅 응답)
```json
{
  "success": true,
  "data": {
    "response": "AI 응답 메시지",
    "conversationId": "세션 ID",
    "sources": [
      {
        "title": "출처 제목",
        "url": "출처 URL",
        "type": "CONTEST | NEWS"
      }
    ]
  }
}
```

### SessionResponse (세션 정보)
```json
{
  "sessionId": "세션 ID",
  "title": "대화 제목",
  "createdAt": "2024-01-01T00:00:00",
  "lastMessageAt": "2024-01-01T00:00:00",
  "isActive": true
}
```

### MessageResponse (메시지 정보)
```json
{
  "messageId": "메시지 ID",
  "sessionId": "세션 ID",
  "role": "USER | ASSISTANT",
  "content": "메시지 내용",
  "tokenCount": 100,
  "sequenceNumber": 1,
  "createdAt": "2024-01-01T00:00:00"
}
```

## 페이지네이션 파라미터

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| page | 1 | 페이지 번호 (1부터 시작) |
| size | 20 (sessions), 50 (messages) | 페이지 크기 |

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
GET {{chatbotUrl}}/api/v1/chatbot/sessions/{{$uuid}}
X-Request-Id: {{$timestamp}}
```

## 문제 해결

### 401 Unauthorized
- Access Token이 만료되었거나 유효하지 않은 경우
- Auth API에서 다시 로그인하여 새 토큰 발급

### 403 Forbidden
- 다른 사용자의 세션에 접근한 경우
- 자신의 세션만 접근 가능

### 404 Not Found
- 존재하지 않거나 삭제된 세션에 접근한 경우

### 400 Bad Request
- 메시지 누락, 빈 메시지, 길이 초과 등 유효성 검증 실패

## 체크리스트

시작하기 전에 확인하세요:

- [ ] IntelliJ IDEA (Community 또는 Ultimate) 설치
- [ ] Chatbot API 서버가 실행 중 (`http://localhost:8086`)
- [ ] Gateway 서버가 실행 중 (`http://localhost:8080`) - 선택
- [ ] `http-client.private.env.json`에 Access Token 설정
- [ ] 환경 선택 (local, dev, prod)
- [ ] 첫 번째 요청 실행 테스트

## 참고 자료

- [IntelliJ HTTP Client 공식 문서](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)
- [HTTP 요청 응답 핸들러 스크립트](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#response-handler-scripts)
- [환경 변수 설정 방법](https://www.jetbrains.com/help/idea/exploring-http-syntax.html#environment-variables)
