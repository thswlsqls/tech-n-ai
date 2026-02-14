# Emerging Tech API 스펙 정의서

## 개요

| 항목 | 내용 |
|------|------|
| 모듈 | api-emerging-tech |
| Base URL | `/api/v1/emerging-tech` |
| 포트 | 8082 (via Gateway: 8081) |
| 설명 | AI 기술 업데이트 정보 관리 API |

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

## Enum 정의

### TechProvider (기술 서비스 제공자)

| 값 | 설명 |
|----|------|
| OPENAI | OpenAI (GPT, DALL-E 등) |
| ANTHROPIC | Anthropic (Claude) |
| GOOGLE | Google (Gemini) |
| META | Meta (LLaMA) |
| XAI | xAI (Grok) |

### EmergingTechType (업데이트 유형)

| 값 | 설명 |
|----|------|
| MODEL_RELEASE | AI 모델 출시 (GPT-5, Claude 4 등) |
| API_UPDATE | API 변경사항 |
| SDK_RELEASE | SDK 새 버전 |
| PRODUCT_LAUNCH | 신규 제품/서비스 출시 |
| PLATFORM_UPDATE | 플랫폼 업데이트 |
| BLOG_POST | 일반 기술 블로그 포스트 |

### PostStatus (게시물 상태)

| 값 | 설명 |
|----|------|
| DRAFT | 초안 (자동 수집됨) |
| PENDING | 승인 대기 |
| PUBLISHED | 게시됨 |
| REJECTED | 거부됨 |

### SourceType (데이터 수집 소스 유형)

| 값 | 설명 |
|----|------|
| GITHUB_RELEASE | GitHub Release |
| RSS | RSS 피드 |
| WEB_SCRAPING | 웹 스크래핑 |

---

## API 엔드포인트

### 1. 목록 조회

**GET** `/api/v1/emerging-tech`

공개 API. 등록된 Emerging Tech 목록을 페이지네이션으로 조회합니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| page | integer | N | 1 | 페이지 번호 (1 이상) |
| size | integer | N | 20 | 페이지 크기 (1~100) |
| provider | string | N | - | 필터: TechProvider enum 값 |
| updateType | string | N | - | 필터: EmergingTechType enum 값 |
| status | string | N | - | 필터: PostStatus enum 값 |
| sourceType | string | N | - | 필터: SourceType enum 값 |
| sort | string | N | - | 정렬 (예: "publishedAt,desc") |
| startDate | string | N | - | 조회 시작일 (YYYY-MM-DD) |
| endDate | string | N | - | 조회 종료일 (YYYY-MM-DD) |

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
    "pageSize": 20,
    "pageNumber": 1,
    "totalCount": 100,
    "items": [
      {
        "id": "507f1f77bcf86cd799439011",
        "provider": "OPENAI",
        "updateType": "MODEL_RELEASE",
        "title": "GPT-5 출시",
        "summary": "GPT-5 모델이 출시되었습니다.",
        "url": "https://openai.com/blog/gpt-5",
        "publishedAt": "2025-01-15T10:00:00",
        "sourceType": "RSS",
        "status": "PUBLISHED",
        "externalId": "gpt5-release-001",
        "metadata": {
          "version": "5.0",
          "tags": ["AI", "LLM", "GPT"],
          "author": "OpenAI",
          "githubRepo": null,
          "additionalInfo": {}
        },
        "createdAt": "2025-01-15T10:30:00",
        "updatedAt": "2025-01-15T10:30:00"
      }
    ]
  }
}
```

#### Response Data Types

**EmergingTechPageResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| pageSize | integer | Y | 페이지 크기 |
| pageNumber | integer | Y | 현재 페이지 번호 |
| totalCount | integer | Y | 전체 항목 수 |
| items | EmergingTechDetailResponse[] | Y | 항목 목록 |

---

### 2. 상세 조회

**GET** `/api/v1/emerging-tech/{id}`

공개 API. 특정 Emerging Tech의 상세 정보를 조회합니다.

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | MongoDB ObjectId (24자 hex string) |

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
    "id": "507f1f77bcf86cd799439011",
    "provider": "OPENAI",
    "updateType": "MODEL_RELEASE",
    "title": "GPT-5 출시",
    "summary": "GPT-5 모델이 출시되었습니다.",
    "url": "https://openai.com/blog/gpt-5",
    "publishedAt": "2025-01-15T10:00:00",
    "sourceType": "RSS",
    "status": "PUBLISHED",
    "externalId": "gpt5-release-001",
    "metadata": {
      "version": "5.0",
      "tags": ["AI", "LLM", "GPT"],
      "author": "OpenAI",
      "githubRepo": "https://github.com/openai/gpt-5",
      "additionalInfo": {
        "customKey": "customValue"
      }
    },
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  }
}
```

#### Response Data Types

**EmergingTechDetailResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| id | string | Y | MongoDB ObjectId |
| provider | string | Y | TechProvider enum 값 |
| updateType | string | Y | EmergingTechType enum 값 |
| title | string | Y | 제목 |
| summary | string | N | 요약 |
| url | string | Y | 원본 URL |
| publishedAt | string (ISO 8601) | N | 발행일시 |
| sourceType | string | Y | SourceType enum 값 |
| status | string | Y | PostStatus enum 값 |
| externalId | string | N | 외부 시스템 ID (중복 체크용) |
| metadata | EmergingTechMetadataResponse | N | 메타데이터 |
| createdAt | string (ISO 8601) | Y | 생성일시 |
| updatedAt | string (ISO 8601) | Y | 수정일시 |

**EmergingTechMetadataResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| version | string | N | 버전 정보 |
| tags | string[] | N | 태그 목록 |
| author | string | N | 작성자 |
| githubRepo | string | N | GitHub 저장소 URL |
| additionalInfo | object | N | 추가 정보 (key-value) |

---

### 3. 검색

**GET** `/api/v1/emerging-tech/search`

공개 API. 키워드로 Emerging Tech를 검색합니다.

#### Request (Query Parameters)

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| q | string | Y | - | 검색어 (빈 문자열 불가) |
| page | integer | N | 1 | 페이지 번호 (1 이상) |
| size | integer | N | 20 | 페이지 크기 (1~100) |

#### Response

목록 조회와 동일한 `EmergingTechPageResponse` 형식

---

### 4. 단건 생성 (내부 API)

**POST** `/api/v1/emerging-tech/internal`

내부 API. 새로운 Emerging Tech를 등록합니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| X-Internal-Api-Key | string | Y | 내부 API 인증 키 |

#### Request Body

```json
{
  "provider": "OPENAI",
  "updateType": "MODEL_RELEASE",
  "title": "GPT-5 출시",
  "summary": "GPT-5 모델이 출시되었습니다.",
  "url": "https://openai.com/blog/gpt-5",
  "publishedAt": "2025-01-15T10:00:00",
  "sourceType": "RSS",
  "status": "DRAFT",
  "externalId": "gpt5-release-001",
  "metadata": {
    "version": "5.0",
    "tags": ["AI", "LLM", "GPT"],
    "author": "OpenAI",
    "githubRepo": "https://github.com/openai/gpt-5",
    "additionalInfo": {
      "customKey": "customValue"
    }
  }
}
```

#### Request Body Types

**EmergingTechCreateRequest**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| provider | string | Y | TechProvider enum 값 |
| updateType | string | Y | EmergingTechType enum 값 |
| title | string | Y | 제목 (빈 문자열 불가) |
| summary | string | N | 요약 |
| url | string | Y | 원본 URL (빈 문자열 불가) |
| publishedAt | string (ISO 8601) | N | 발행일시 |
| sourceType | string | Y | SourceType enum 값 |
| status | string | Y | PostStatus enum 값 |
| externalId | string | N | 외부 시스템 ID (중복 체크용) |
| metadata | EmergingTechMetadataRequest | N | 메타데이터 |

**EmergingTechMetadataRequest**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| version | string | N | 버전 정보 |
| tags | string[] | N | 태그 목록 |
| author | string | N | 작성자 |
| githubRepo | string | N | GitHub 저장소 URL |
| additionalInfo | object | N | 추가 정보 (key-value) |

#### Response

`EmergingTechDetailResponse` 형식

---

### 5. 다건 생성 (내부 API)

**POST** `/api/v1/emerging-tech/internal/batch`

내부 API. 여러 Emerging Tech를 일괄 등록합니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| X-Internal-Api-Key | string | Y | 내부 API 인증 키 |

#### Request Body

```json
{
  "items": [
    {
      "provider": "OPENAI",
      "updateType": "MODEL_RELEASE",
      "title": "GPT-5 출시",
      "summary": "GPT-5 모델이 출시되었습니다.",
      "url": "https://openai.com/blog/gpt-5",
      "publishedAt": "2025-01-15T10:00:00",
      "sourceType": "RSS",
      "status": "DRAFT",
      "externalId": "gpt5-release-001",
      "metadata": null
    }
  ]
}
```

#### Request Body Types

**EmergingTechBatchRequest**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| items | EmergingTechCreateRequest[] | Y | 생성 요청 목록 (1개 이상) |

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
    "totalCount": 10,
    "successCount": 8,
    "newCount": 6,
    "duplicateCount": 2,
    "failureCount": 2,
    "failureMessages": [
      "index 3: Invalid provider value",
      "index 7: URL is required"
    ]
  }
}
```

#### Response Data Types

**EmergingTechBatchResponse**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| totalCount | integer | Y | 전체 요청 수 |
| successCount | integer | Y | 성공 수 (신규 + 중복) |
| newCount | integer | Y | 신규 등록 수 |
| duplicateCount | integer | Y | 중복 스킵 수 |
| failureCount | integer | Y | 실패 수 |
| failureMessages | string[] | Y | 실패 메시지 목록 |

---

### 6. 승인 (내부 API)

**POST** `/api/v1/emerging-tech/{id}/approve`

내부 API. Emerging Tech를 승인하여 게시 상태로 변경합니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| X-Internal-Api-Key | string | Y | 내부 API 인증 키 |

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | MongoDB ObjectId |

#### Response

`EmergingTechDetailResponse` 형식

---

### 7. 거부 (내부 API)

**POST** `/api/v1/emerging-tech/{id}/reject`

내부 API. Emerging Tech를 거부합니다.

#### Request Headers

| 헤더 | 타입 | 필수 | 설명 |
|------|------|------|------|
| X-Internal-Api-Key | string | Y | 내부 API 인증 키 |

#### Request (Path Parameters)

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| id | string | Y | MongoDB ObjectId |

#### Response

`EmergingTechDetailResponse` 형식

---

## TypeScript 타입 정의 (Frontend)

```typescript
// Enum Types
type TechProvider = 'OPENAI' | 'ANTHROPIC' | 'GOOGLE' | 'META' | 'XAI';

type EmergingTechType =
  | 'MODEL_RELEASE'
  | 'API_UPDATE'
  | 'SDK_RELEASE'
  | 'PRODUCT_LAUNCH'
  | 'PLATFORM_UPDATE'
  | 'BLOG_POST';

type PostStatus = 'DRAFT' | 'PENDING' | 'PUBLISHED' | 'REJECTED';

type SourceType = 'GITHUB_RELEASE' | 'RSS' | 'WEB_SCRAPING';

// Response Types
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

interface EmergingTechMetadata {
  version?: string;
  tags?: string[];
  author?: string;
  githubRepo?: string;
  additionalInfo?: Record<string, unknown>;
}

interface EmergingTechDetail {
  id: string;
  provider: TechProvider;
  updateType: EmergingTechType;
  title: string;
  summary?: string;
  url: string;
  publishedAt?: string; // ISO 8601
  sourceType: SourceType;
  status: PostStatus;
  externalId?: string;
  metadata?: EmergingTechMetadata;
  createdAt: string; // ISO 8601
  updatedAt: string; // ISO 8601
}

interface EmergingTechPageResponse {
  pageSize: number;
  pageNumber: number;
  totalCount: number;
  items: EmergingTechDetail[];
}

interface EmergingTechBatchResponse {
  totalCount: number;
  successCount: number;
  newCount: number;
  duplicateCount: number;
  failureCount: number;
  failureMessages: string[];
}

// Request Types
interface EmergingTechListRequest {
  page?: number;
  size?: number;
  provider?: TechProvider;
  updateType?: EmergingTechType;
  status?: PostStatus;
  sourceType?: SourceType;
  sort?: string;
  startDate?: string; // YYYY-MM-DD
  endDate?: string;   // YYYY-MM-DD
}

interface EmergingTechSearchRequest {
  q: string;
  page?: number;
  size?: number;
}

interface EmergingTechMetadataRequest {
  version?: string;
  tags?: string[];
  author?: string;
  githubRepo?: string;
  additionalInfo?: Record<string, unknown>;
}

interface EmergingTechCreateRequest {
  provider: TechProvider;
  updateType: EmergingTechType;
  title: string;
  summary?: string;
  url: string;
  publishedAt?: string; // ISO 8601
  sourceType: SourceType;
  status: PostStatus;
  externalId?: string;
  metadata?: EmergingTechMetadataRequest;
}

interface EmergingTechBatchRequest {
  items: EmergingTechCreateRequest[];
}
```

---

## 에러 응답

에러 발생 시 다음 형식으로 응답합니다:

```json
{
  "code": "4000",
  "messageCode": {
    "code": "VALIDATION_ERROR",
    "text": "입력값 검증 실패"
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
