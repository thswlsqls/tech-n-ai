# Agent API 설계서

**작성일**: 2026-02-06
**대상 모듈**: api-agent
**버전**: v1

---

## 1. 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-agent |
| Base URL | `/api/v1/agent` |
| 포트 | 8086 (via Gateway: 8081) |
| 설명 | AI Agent 수동 실행 API (ADMIN 역할 전용) |

### 인증

모든 API는 **ADMIN 역할** JWT 인증이 필요합니다.
Gateway에서 JWT 역할 기반 인증을 수행합니다.

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | String | O | `Bearer {accessToken}` |
| x-user-id | String | O | Gateway가 주입한 사용자 ID (자동) |

---

## 2. 공통 응답 형식

### ApiResponse<T>

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": {...}
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| code | String | O | 응답 코드 |
| messageCode | MessageCode | O | 메시지 코드 객체 |
| message | String | X | 응답 메시지 |
| data | T | X | 응답 데이터 |

---

## 3. Agent API

### 3.1 Agent 수동 실행

**POST** `/api/v1/agent/run`

**인증**: 필요 (ADMIN)

AI Agent를 수동으로 실행합니다. Emerging Tech 데이터 수집 및 분석을 수행합니다.

**Request Body**

```json
{
  "goal": "최신 AI 기술 동향을 수집하고 분석해줘",
  "sessionId": "admin-123-abc12345"
}
```

**AgentRunRequest 필드**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| goal | String | O | NotBlank | 실행 목표 |
| sessionId | String | X | - | 세션 식별자 (미지정 시 자동 생성: `admin-{userId}-{uuid}`) |

**Response** (200 OK) `ApiResponse<AgentExecutionResult>`

```json
{
  "code": "2000",
  "messageCode": { "code": "SUCCESS", "text": "성공" },
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

**AgentExecutionResult 필드**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| success | Boolean | O | 실행 성공 여부 |
| summary | String | O | 실행 결과 요약 |
| toolCallCount | Integer | O | 사용된 도구 호출 횟수 |
| analyticsCallCount | Integer | O | 분석 도구 호출 횟수 |
| executionTimeMs | Long | O | 실행 시간 (밀리초) |
| errors | String[] | O | 에러 메시지 목록 (성공 시 빈 배열) |

**Errors**
- `400` - goal 필드 누락/빈 값
- `401` - 인증 실패
- `403` - ADMIN 역할 없음
- `500` - Agent 실행 중 오류 발생

---

## 4. 에러 코드

| HTTP 상태 | 에러 코드 | 설명 |
|----------|---------|------|
| 400 | 4000 | 잘못된 요청 (Validation Error) |
| 401 | 4010 | 인증 실패 (Unauthorized) |
| 403 | 4030 | 권한 없음 (Forbidden) |
| 500 | 5000 | 서버 에러 (Internal Server Error) |

### Agent 관련 에러 메시지

| 상황 | 에러 코드 | 메시지 |
|------|---------|--------|
| 빈 goal | 4000 | goal은 필수입니다. |
| 권한 없음 | 4030 | 관리자 권한이 필요합니다. |
| Agent 실행 실패 | 5000 | Agent 실행 중 오류가 발생했습니다. |

---

## 5. 엔드포인트 요약

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/api/v1/agent/run` | O (ADMIN) | Agent 수동 실행 |

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-02-06
