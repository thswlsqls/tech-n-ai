# Bookmark API 스펙 정의서

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-bookmark |
| Base URL | `/api/v1/bookmark` |
| 포트 | 8085 (via Gateway: 8081) |
| 설명 | 사용자 북마크 관리 API |

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

### PageData<T>

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| pageSize | number | Y | 페이지 크기 |
| pageNumber | number | Y | 현재 페이지 번호 (1부터 시작) |
| totalPageNumber | number | Y | 전체 페이지 수 |
| totalSize | number | Y | 전체 데이터 수 |
| list | T[] | Y | 데이터 리스트 |

---

## 인증

모든 API는 인증이 필요합니다.

### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| Authorization | string | Y | `Bearer {accessToken}` |

---

## 북마크 API

### 1. 북마크 저장

**POST** `/api/v1/bookmark`

새로운 북마크를 저장합니다.

#### Request Body

```json
{
  "emergingTechId": "et_123456789",
  "tags": ["AI", "Machine Learning"],
  "memo": "관심 있는 기술 아티클"
}
```

#### Request Body Types

**BookmarkCreateRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| emergingTechId | string | Y | NotBlank | EmergingTech 콘텐츠 ID |
| tags | string[] | N | - | 사용자 지정 태그 목록 |
| memo | string | N | - | 사용자 메모 |

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
    "bookmarkTsid": "123456789012345678",
    "userId": "100000001",
    "emergingTechId": "et_123456789",
    "title": "GPT-5 출시 예정",
    "url": "https://example.com/article/123",
    "provider": "HACKERNEWS",
    "summary": "OpenAI가 새로운 GPT-5 모델 출시를 예고했습니다.",
    "publishedAt": "2025-01-15T10:00:00",
    "tags": ["AI", "Machine Learning"],
    "memo": "관심 있는 기술 아티클",
    "createdAt": "2025-01-20T14:30:00",
    "createdBy": "100000001",
    "updatedAt": "2025-01-20T14:30:00",
    "updatedBy": "100000001"
  }
}
```

#### Response Data Types

**BookmarkDetailResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| bookmarkTsid | string | Y | 북마크 고유 ID (TSID) |
| userId | string | Y | 사용자 ID |
| emergingTechId | string | Y | EmergingTech 콘텐츠 ID |
| title | string | N | 콘텐츠 제목 |
| url | string | N | 콘텐츠 URL |
| provider | string | N | 콘텐츠 제공자 (HACKERNEWS, DEVTO, REDDIT 등) |
| summary | string | N | 콘텐츠 요약 |
| publishedAt | string (ISO 8601) | N | 콘텐츠 게시일시 |
| tags | string[] | N | 사용자 지정 태그 목록 |
| memo | string | N | 사용자 메모 |
| createdAt | string (ISO 8601) | Y | 북마크 생성일시 |
| createdBy | string | N | 생성자 ID |
| updatedAt | string (ISO 8601) | N | 북마크 수정일시 |
| updatedBy | string | N | 수정자 ID |

---

### 2. 북마크 목록 조회

**GET** `/api/v1/bookmark`

사용자의 북마크 목록을 조회합니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| page | number | N | 1 | Min(1) | 페이지 번호 |
| size | number | N | 10 | Min(1), Max(100) | 페이지 크기 |
| sort | string | N | "createdAt,desc" | - | 정렬 기준 (필드명,방향) |
| provider | string | N | - | - | 콘텐츠 제공자 필터 |

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
    "data": {
      "pageSize": 10,
      "pageNumber": 1,
      "totalPageNumber": 5,
      "totalSize": 45,
      "list": [
        {
          "bookmarkTsid": "123456789012345678",
          "userId": "100000001",
          "emergingTechId": "et_123456789",
          "title": "GPT-5 출시 예정",
          "url": "https://example.com/article/123",
          "provider": "HACKERNEWS",
          "summary": "OpenAI가 새로운 GPT-5 모델 출시를 예고했습니다.",
          "publishedAt": "2025-01-15T10:00:00",
          "tags": ["AI", "Machine Learning"],
          "memo": "관심 있는 기술 아티클",
          "createdAt": "2025-01-20T14:30:00",
          "createdBy": "100000001",
          "updatedAt": "2025-01-20T14:30:00",
          "updatedBy": "100000001"
        }
      ]
    }
  }
}
```

#### Response Data Types

**BookmarkListResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| data | PageData<BookmarkDetailResponse> | Y | 페이징된 북마크 목록 |

---

### 3. 북마크 상세 조회

**GET** `/api/v1/bookmark/{id}`

특정 북마크의 상세 정보를 조회합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | 북마크 ID (TSID) |

#### Response

`BookmarkDetailResponse` 형식 (북마크 저장 응답과 동일)

---

### 4. 북마크 수정

**PUT** `/api/v1/bookmark/{id}`

북마크의 태그와 메모를 수정합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | 북마크 ID (TSID) |

#### Request Body

```json
{
  "tags": ["AI", "Deep Learning", "NLP"],
  "memo": "수정된 메모 내용"
}
```

#### Request Body Types

**BookmarkUpdateRequest**

| 필드 | 타입 | 필수 | 검증 규칙 | 설명 |
|------|------|------|-----------|------|
| tags | string[] | N | - | 사용자 지정 태그 목록 |
| memo | string | N | - | 사용자 메모 |

#### Response

`BookmarkDetailResponse` 형식 (북마크 저장 응답과 동일)

---

### 5. 북마크 삭제

**DELETE** `/api/v1/bookmark/{id}`

북마크를 삭제합니다 (소프트 삭제).

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | 북마크 ID (TSID) |

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

### 6. 삭제된 북마크 목록 조회

**GET** `/api/v1/bookmark/deleted`

삭제된 북마크 목록을 조회합니다 (휴지통).

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| page | number | N | 1 | Min(1) | 페이지 번호 |
| size | number | N | 10 | Min(1), Max(100) | 페이지 크기 |
| days | number | N | 30 | - | 조회할 기간 (일 단위) |

#### Response

`BookmarkListResponse` 형식 (북마크 목록 조회 응답과 동일)

---

### 7. 북마크 복구

**POST** `/api/v1/bookmark/{id}/restore`

삭제된 북마크를 복구합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | 북마크 ID (TSID) |

#### Response

`BookmarkDetailResponse` 형식 (북마크 저장 응답과 동일)

---

### 8. 북마크 검색

**GET** `/api/v1/bookmark/search`

북마크를 검색합니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| q | string | Y | - | NotBlank | 검색어 |
| page | number | N | 1 | Min(1) | 페이지 번호 |
| size | number | N | 10 | Min(1), Max(100) | 페이지 크기 |
| searchField | string | N | "all" | - | 검색 필드 (all, title, memo, tags) |

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
    "data": {
      "pageSize": 10,
      "pageNumber": 1,
      "totalPageNumber": 2,
      "totalSize": 15,
      "list": [
        {
          "bookmarkTsid": "123456789012345678",
          "userId": "100000001",
          "emergingTechId": "et_123456789",
          "title": "GPT-5 출시 예정",
          "url": "https://example.com/article/123",
          "provider": "HACKERNEWS",
          "summary": "OpenAI가 새로운 GPT-5 모델 출시를 예고했습니다.",
          "publishedAt": "2025-01-15T10:00:00",
          "tags": ["AI", "Machine Learning"],
          "memo": "관심 있는 기술 아티클",
          "createdAt": "2025-01-20T14:30:00",
          "createdBy": "100000001",
          "updatedAt": "2025-01-20T14:30:00",
          "updatedBy": "100000001"
        }
      ]
    }
  }
}
```

#### Response Data Types

**BookmarkSearchResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| data | PageData<BookmarkDetailResponse> | Y | 페이징된 검색 결과 |

---

## 북마크 히스토리 API

### 9. 변경 이력 조회

**GET** `/api/v1/bookmark/history/{entityId}`

특정 북마크의 변경 이력을 조회합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| entityId | string | Y | 북마크 ID (TSID) |

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 검증 규칙 | 설명 |
|----------|------|------|--------|-----------|------|
| page | number | N | 1 | Min(1) | 페이지 번호 |
| size | number | N | 10 | Min(1), Max(100) | 페이지 크기 |
| operationType | string | N | - | - | 작업 유형 필터 (CREATE, UPDATE, DELETE) |
| startDate | string | N | - | ISO 8601 | 조회 시작일 |
| endDate | string | N | - | ISO 8601 | 조회 종료일 |

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
    "data": {
      "pageSize": 10,
      "pageNumber": 1,
      "totalPageNumber": 1,
      "totalSize": 3,
      "list": [
        {
          "historyId": "987654321098765432",
          "entityId": "123456789012345678",
          "operationType": "UPDATE",
          "beforeData": {
            "tags": ["AI"],
            "memo": "이전 메모"
          },
          "afterData": {
            "tags": ["AI", "Machine Learning"],
            "memo": "수정된 메모"
          },
          "changedBy": "100000001",
          "changedAt": "2025-01-20T15:00:00",
          "changeReason": null
        }
      ]
    }
  }
}
```

#### Response Data Types

**BookmarkHistoryListResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| data | PageData<BookmarkHistoryDetailResponse> | Y | 페이징된 히스토리 목록 |

**BookmarkHistoryDetailResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| historyId | string | Y | 히스토리 ID |
| entityId | string | Y | 북마크 ID |
| operationType | string | Y | 작업 유형 (CREATE, UPDATE, DELETE) |
| beforeData | object | N | 변경 전 데이터 (JSON) |
| afterData | object | N | 변경 후 데이터 (JSON) |
| changedBy | string | Y | 변경자 ID |
| changedAt | string (ISO 8601) | Y | 변경일시 |
| changeReason | string | N | 변경 사유 |

---

### 10. 특정 시점 데이터 조회

**GET** `/api/v1/bookmark/history/{entityId}/at`

특정 시점의 북마크 상태를 조회합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| entityId | string | Y | 북마크 ID (TSID) |

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| timestamp | string | Y | 조회 시점 (ISO 8601 형식) |

#### Response

`BookmarkHistoryDetailResponse` 형식

---

### 11. 특정 버전으로 복구

**POST** `/api/v1/bookmark/history/{entityId}/restore`

특정 히스토리 버전으로 북마크를 복구합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| entityId | string | Y | 북마크 ID (TSID) |

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| historyId | string | Y | 복구할 히스토리 ID |

#### Response

`BookmarkDetailResponse` 형식 (북마크 저장 응답과 동일)

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

interface PageData<T> {
  pageSize: number;
  pageNumber: number;
  totalPageNumber: number;
  totalSize: number;
  list: T[];
}

// Bookmark Types
interface BookmarkDetailResponse {
  bookmarkTsid: string;
  userId: string;
  emergingTechId: string;
  title?: string;
  url?: string;
  provider?: string;
  summary?: string;
  publishedAt?: string; // ISO 8601
  tags?: string[];
  memo?: string;
  createdAt: string; // ISO 8601
  createdBy?: string;
  updatedAt?: string; // ISO 8601
  updatedBy?: string;
}

interface BookmarkListResponse {
  data: PageData<BookmarkDetailResponse>;
}

interface BookmarkSearchResponse {
  data: PageData<BookmarkDetailResponse>;
}

interface BookmarkHistoryDetailResponse {
  historyId: string;
  entityId: string;
  operationType: 'CREATE' | 'UPDATE' | 'DELETE';
  beforeData?: Record<string, unknown>;
  afterData?: Record<string, unknown>;
  changedBy: string;
  changedAt: string; // ISO 8601
  changeReason?: string;
}

interface BookmarkHistoryListResponse {
  data: PageData<BookmarkHistoryDetailResponse>;
}

// Request Types
interface BookmarkCreateRequest {
  emergingTechId: string;
  tags?: string[];
  memo?: string;
}

interface BookmarkUpdateRequest {
  tags?: string[];
  memo?: string;
}

interface BookmarkListRequest {
  page?: number;
  size?: number;
  sort?: string;
  provider?: string;
}

interface BookmarkDeletedListRequest {
  page?: number;
  size?: number;
  days?: number;
}

interface BookmarkSearchRequest {
  q: string;
  page?: number;
  size?: number;
  searchField?: 'all' | 'title' | 'memo' | 'tags';
}

interface BookmarkHistoryListRequest {
  page?: number;
  size?: number;
  operationType?: 'CREATE' | 'UPDATE' | 'DELETE';
  startDate?: string; // ISO 8601
  endDate?: string; // ISO 8601
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
    "text": "북마크를 찾을 수 없습니다."
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
| 4090 | 충돌 (Conflict - 북마크 중복 등) |
| 5000 | 서버 에러 (Internal Server Error) |

### Bookmark 관련 에러 사례

| 상황 | 코드 | 설명 |
|------|------|------|
| 북마크 없음 | 4040 | 북마크를 찾을 수 없습니다. |
| 북마크 중복 | 4090 | 이미 북마크한 콘텐츠입니다. |
| 유효하지 않은 ID | 4000 | 유효하지 않은 북마크 ID 형식입니다. |
| EmergingTech 없음 | 4040 | EmergingTech 콘텐츠를 찾을 수 없습니다. |
| 히스토리 없음 | 4040 | 히스토리를 찾을 수 없습니다. |
