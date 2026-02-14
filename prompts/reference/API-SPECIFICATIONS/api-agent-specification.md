# Agent API 스펙 정의서

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-agent |
| Base URL | `/api/v1/agent` |
| 포트 | 8086 (via Gateway: 8081) |
| 설명 | AI Agent 수동 실행 API (ADMIN 역할 전용) |

---

## 공통 응답 형식

### ApiResponse<T>

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | string | Y | 응답 코드 (성공: "2000") |
| messageCode | MessageCode | Y | 메시지 코드 객체 |
| message | string | N | 응답 메시지 (성공 시: "success") |
| data | T | N | 응답 데이터 |

### MessageCode

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | string | Y | 메시지 코드 (성공: "SUCCESS") |
| text | string | Y | 메시지 텍스트 (성공: "성공") |

---

## 인증

모든 API는 **ADMIN 역할** 인증이 필요합니다.
Gateway에서 JWT 역할 기반 인증을 수행합니다.

### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` |
| x-user-id | string | Y | Gateway가 주입한 사용자 ID (자동) |

---

## Agent API

### 1. Agent 수동 실행

**POST** `/api/v1/agent/run`

AI Agent를 수동으로 실행합니다. Emerging Tech 데이터 수집 및 분석을 수행합니다.

#### Request Body

```json
{
  "goal": "최신 AI 기술 동향을 수집하고 분석해줘",
  "sessionId": "admin-123-abc12345"
}
```

#### Request Body Types

**AgentRunRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| goal | string | Y | NotBlank | 실행 목표 |
| sessionId | string | N | - | 세션 식별자 (미지정 시 자동 생성: `admin-{userId}-{uuid}`) |

#### Response

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
    "summary": "GitHub에서 10개, RSS에서 5개의 신기술 정보를 수집했습니다. 분석 결과: AI/ML 관련 업데이트 7건, 클라우드 관련 3건, 기타 5건입니다.",
    "toolCallCount": 15,
    "analyticsCallCount": 3,
    "executionTimeMs": 12500,
    "errors": []
  }
}
```

#### Response Data Types

**AgentExecutionResult**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | boolean | Y | 실행 성공 여부 |
| summary | string | Y | 실행 결과 요약 |
| toolCallCount | number | Y | 사용된 도구 호출 횟수 |
| analyticsCallCount | number | Y | 분석 도구 호출 횟수 |
| executionTimeMs | number | Y | 실행 시간 (밀리초) |
| errors | string[] | Y | 에러 메시지 목록 (성공 시 빈 배열) |

---

## TypeScript 타입 정의 (Frontend)

```typescript
// Common Types
interface MessageCode {
  code: string;
  text: string;
}

interface ApiResponse<T> {
  code: string;
  messageCode: MessageCode;
  message?: string;
  data?: T;
}

// Agent Types
interface AgentRunRequest {
  goal: string;
  sessionId?: string;
}

interface AgentExecutionResult {
  success: boolean;
  summary: string;
  toolCallCount: number;
  analyticsCallCount: number;
  executionTimeMs: number;
  errors: string[];
}
```

---

## 에러 응답

에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "code": "4030",
  "messageCode": {
    "code": "FORBIDDEN",
    "text": "관리자 권한이 필요합니다."
  },
  "message": null,
  "data": null
}
```

### 주요 에러 코드

| 코드 | 설명 |
|------|------|
| 4000 | 잘못된 요청 (Validation Error) |
| 4010 | 인증 실패 (Unauthorized) |
| 4030 | 권한 없음 (Forbidden) |
| 5000 | 서버 에러 (Internal Server Error) |

### Agent 관련 에러 사례

| 상황 | 코드 | 설명 |
|------|------|------|
| 빈 goal | 4000 | goal은 필수입니다. |
| 권한 없음 | 4030 | 관리자 권한이 필요합니다. |
| Agent 실행 실패 | 5000 | Agent 실행 중 오류가 발생했습니다. |

---

## 사용 예시 (Next.js)

```typescript
// API Client
async function runAgent(request: AgentRunRequest): Promise<ApiResponse<AgentExecutionResult>> {
  const response = await fetch('/api/v1/agent/run', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
    },
    body: JSON.stringify(request),
  });
  return response.json();
}

// Usage
const result = await runAgent({
  goal: '최신 AI 기술 동향을 수집하고 분석해줘',
});

if (result.data?.success) {
  console.log(`실행 완료: ${result.data.summary}`);
  console.log(`도구 호출: ${result.data.toolCallCount}회`);
  console.log(`실행 시간: ${result.data.executionTimeMs}ms`);
} else {
  console.error('실행 실패:', result.data?.errors);
}
```
