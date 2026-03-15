# Chatbot API 스펙 정의서

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-chatbot |
| Base URL | `/api/v1/chatbot` |
| 포트 | 8084 (via Gateway: 8081) |
| 설명 | RAG 기반 AI 챗봇 API (대화, 세션 관리) |

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

### Page<T> (Spring Data Page)

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | T[] | Y | 데이터 목록 |
| pageable | Pageable | Y | 페이징 정보 |
| totalElements | number | Y | 전체 데이터 수 |
| totalPages | number | Y | 전체 페이지 수 |
| size | number | Y | 페이지 크기 |
| number | number | Y | 현재 페이지 번호 (0부터 시작) |
| first | boolean | Y | 첫 페이지 여부 |
| last | boolean | Y | 마지막 페이지 여부 |
| empty | boolean | Y | 빈 페이지 여부 |

---

## 인증

모든 API는 인증이 필요합니다.

### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` |

---

## 채팅 API

### 1. 채팅 메시지 전송

**POST** `/api/v1/chatbot`

AI 챗봇에게 메시지를 보내고 응답을 받습니다. 세션 ID가 없으면 새 세션이 생성됩니다.

#### Request Body

```json
{
  "message": "최신 AI 기술 트렌드에 대해 알려줘",
  "conversationId": "sess_abc123def456"
}
```

#### Request Body Types

**ChatRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| message | string | Y | NotBlank, Max(500) | 사용자 메시지 (최대 500자) |
| conversationId | string | N | - | 대화 세션 ID (없으면 새 세션 생성) |

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
    "response": "최신 AI 기술 트렌드를 알려드리겠습니다...",
    "conversationId": "sess_abc123def456",
    "sources": [
      {
        "documentId": "doc_789xyz",
        "collectionType": "EMERGING_TECH",
        "score": 0.95,
        "title": "2025 AI 트렌드 리포트",
        "url": "https://example.com/ai-trends-2025"
      }
    ]
  }
}
```

#### Response Data Types

**ChatResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| response | string | Y | AI 응답 메시지 |
| conversationId | string | Y | 대화 세션 ID |
| sources | SourceResponse[] | N | 참조한 소스 목록 (RAG) |

**SourceResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| documentId | string | N | 문서 ID |
| collectionType | string | N | 컬렉션 타입 (EMERGING_TECH, NEWS 등) |
| score | number | N | 관련도 점수 (0~1) |
| title | string | N | 소스 제목 (웹 검색 결과) |
| url | string | N | 소스 URL (웹 검색 결과) |

---

## 대화 세션 API

### 2. 세션 목록 조회

**GET** `/api/v1/chatbot/sessions`

사용자의 대화 세션 목록을 조회합니다. 최근 메시지 시간 기준 내림차순 정렬됩니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| page | number | N | 1 | Min(1) | 페이지 번호 (1부터 시작) |
| size | number | N | 20 | Min(1), Max(100) | 페이지 크기 |

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
    "content": [
      {
        "sessionId": "sess_abc123def456",
        "title": "AI 트렌드에 대한 대화",
        "createdAt": "2025-01-20T10:00:00",
        "lastMessageAt": "2025-01-20T14:30:00",
        "isActive": true
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": {
        "sorted": true,
        "direction": "DESC",
        "property": "lastMessageAt"
      }
    },
    "totalElements": 15,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

#### Response Data Types

**SessionResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sessionId | string | Y | 세션 ID |
| title | string | N | 세션 제목 (첫 메시지 기반 자동 생성) |
| createdAt | string (ISO 8601) | Y | 세션 생성일시 |
| lastMessageAt | string (ISO 8601) | N | 마지막 메시지 일시 |
| isActive | boolean | Y | 세션 활성화 여부 |

---

### 3. 세션 상세 조회

**GET** `/api/v1/chatbot/sessions/{sessionId}`

특정 대화 세션의 상세 정보를 조회합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| sessionId | string | Y | 세션 ID |

#### Response

`SessionResponse` 형식

---

### 4. 세션 메시지 목록 조회

**GET** `/api/v1/chatbot/sessions/{sessionId}/messages`

특정 세션의 메시지 목록을 조회합니다. 시퀀스 번호 기준 오름차순 정렬됩니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| sessionId | string | Y | 세션 ID |

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| page | number | N | 1 | Min(1) | 페이지 번호 (1부터 시작) |
| size | number | N | 50 | Min(1), Max(100) | 페이지 크기 |

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
    "content": [
      {
        "messageId": "msg_xyz789",
        "sessionId": "sess_abc123def456",
        "role": "USER",
        "content": "최신 AI 기술 트렌드에 대해 알려줘",
        "tokenCount": 25,
        "sequenceNumber": 1,
        "createdAt": "2025-01-20T14:25:00"
      },
      {
        "messageId": "msg_xyz790",
        "sessionId": "sess_abc123def456",
        "role": "ASSISTANT",
        "content": "최신 AI 기술 트렌드를 알려드리겠습니다...",
        "tokenCount": 150,
        "sequenceNumber": 2,
        "createdAt": "2025-01-20T14:25:05"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 50
    },
    "totalElements": 2,
    "totalPages": 1,
    "size": 50,
    "number": 0,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

#### Response Data Types

**MessageResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| messageId | string | Y | 메시지 ID |
| sessionId | string | Y | 세션 ID |
| role | string | Y | 메시지 역할 (`USER`, `ASSISTANT`) |
| content | string | Y | 메시지 내용 |
| tokenCount | number | N | 토큰 수 |
| sequenceNumber | number | Y | 메시지 순서 번호 |
| createdAt | string (ISO 8601) | Y | 메시지 생성일시 |

---

### 5. 세션 삭제

**DELETE** `/api/v1/chatbot/sessions/{sessionId}`

대화 세션을 삭제합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| sessionId | string | Y | 세션 ID |

#### Response

```json
{
  "code": "2000",
  "messageCode": {
    "code": "SUCCESS",
    "text": "성공"
  },
  "message": "success",
  "data": null
}
```

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

interface Page<T> {
  content: T[];
  pageable: Pageable;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

interface Pageable {
  pageNumber: number;
  pageSize: number;
  sort?: Sort;
}

interface Sort {
  sorted: boolean;
  direction?: 'ASC' | 'DESC';
  property?: string;
}

// Chatbot Types
interface ChatRequest {
  message: string;
  conversationId?: string;
}

interface ChatResponse {
  response: string;
  conversationId: string;
  sources?: SourceResponse[];
}

interface SourceResponse {
  documentId?: string;
  collectionType?: string;
  score?: number;
  title?: string;
  url?: string;
}

interface SessionResponse {
  sessionId: string;
  title?: string;
  createdAt: string; // ISO 8601
  lastMessageAt?: string; // ISO 8601
  isActive: boolean;
}

interface MessageResponse {
  messageId: string;
  sessionId: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  tokenCount?: number;
  sequenceNumber: number;
  createdAt: string; // ISO 8601
}

// Request Types
interface SessionListRequest {
  page?: number;
  size?: number;
}

interface MessageListRequest {
  page?: number;
  size?: number;
}
```

---

## 에러 응답

에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "code": "4040",
  "messageCode": {
    "code": "NOT_FOUND",
    "text": "세션을 찾을 수 없습니다."
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
| 4040 | 리소스 없음 (Not Found) |
| 5000 | 서버 에러 (Internal Server Error) |

### Chatbot 관련 에러 사례

| 상황 | 코드 | 설명 |
|------|------|------|
| 빈 메시지 | 4000 | 메시지는 필수입니다. |
| 메시지 길이 초과 | 4000 | 메시지는 500자를 초과할 수 없습니다. |
| 세션 없음 | 4040 | 세션을 찾을 수 없습니다. |
| 토큰 한도 초과 | 4000 | 토큰 한도를 초과했습니다. |
| 권한 없음 | 4030 | 해당 세션에 접근할 권한이 없습니다. |
