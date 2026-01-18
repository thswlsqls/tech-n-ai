# Contest API 모듈

## 개요

`api-contest` 모듈은 대회(Contest) 정보를 조회하는 REST API 모듈입니다. CQRS 패턴을 기반으로 MongoDB Atlas를 사용하여 Contest 데이터를 조회하는 읽기 전용 API를 제공하며, Batch 모듈을 통해 데이터를 수집하고 저장합니다.

## 주요 기능

### 1. Contest 조회
- **목록 조회**: 페이징, 필터링, 정렬 지원
- **상세 조회**: 특정 Contest의 상세 정보 조회
- **검색**: Full-text Search를 통한 Contest 검색

### 2. 내부 API (Batch 모듈 전용)
- **단건 생성**: Contest 단건 저장
- **다건 생성**: Contest 다건 저장 (부분 롤백 지원)
- **인증**: 내부 API 키 검증

## 아키텍처

### CQRS 패턴 적용

이 모듈은 CQRS(Command Query Responsibility Segregation) 패턴을 적용합니다:

- **Query Side (읽기)**: MongoDB Atlas `ContestDocument`
  - 모든 조회 작업은 MongoDB Atlas 사용
- **Command Side (쓰기)**: 내부 API를 통한 MongoDB Atlas 저장
  - Batch 모듈에서 호출하는 내부 API
  - 단건/다건 처리 지원

### 데이터 흐름

```
외부 출처 (RSS/Web/API)
  ↓
client-rss / client-scraper / client-feign
  ↓ (외부 요청, 데이터 수집 및 정제)
Batch 모듈 (batch-source)
  ├─ Item Reader: client/* 모듈의 수집 데이터 읽기
  ├─ Item Processor: Client DTO → API DTO 변환
  └─ Item Writer: api-contest 모듈로 HTTP 요청
  ↓
api-contest
  ↓ (MongoDB 저장)
MongoDB Atlas (ContestDocument)
```

### 계층 구조

```
Controller → Facade → Service → Repository
```

- **Controller**: HTTP 요청/응답 처리
- **Facade**: Controller와 Service 사이의 중간 계층, 비즈니스 로직 조합
- **Service**: 핵심 비즈니스 로직, MongoDB Repository 호출
- **Repository**: MongoDB Atlas 데이터 접근

### 트랜잭션 관리

- **단건 처리**: `@Transactional` 어노테이션으로 트랜잭션 생성, 실패 시 자동 롤백
- **다건 처리**: 
  - Facade 레이어에서 단건 처리 Service 메서드를 반복 호출
  - 부분 롤백 구현: 실패한 항목은 롤백, 성공한 항목은 정상 커밋
  - Facade 레이어에는 `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)

## API 엔드포인트

### 기본 정보

- **Base URL**: `/api/v1/contest`
- **인증**: 
  - 공개 API: 인증 불필요
  - 내부 API: `X-Internal-Api-Key` 헤더 필요

### 엔드포인트 목록

#### 1. Contest 목록 조회
- **엔드포인트**: `GET /api/v1/contest`
- **설명**: Contest 목록을 조회합니다.
- **쿼리 파라미터**:
  - `page` (Integer, optional): 페이지 번호 (기본값: 1)
  - `size` (Integer, optional): 페이지 크기 (기본값: 10, 최대: 100)
  - `sort` (String, optional): 정렬 기준 (예: "startDate,desc", 기본값: "startDate,desc")
  - `sourceId` (String, optional): 출처 ID (ObjectId)
  - `status` (String, optional): 대회 상태 필터 ("UPCOMING", "ONGOING", "ENDED")

#### 2. Contest 상세 조회
- **엔드포인트**: `GET /api/v1/contest/{id}`
- **설명**: 특정 Contest의 상세 정보를 조회합니다.
- **경로 파라미터**:
  - `id` (String, required): Contest ID (ObjectId)

#### 3. Contest 검색
- **엔드포인트**: `GET /api/v1/contest/search`
- **설명**: Contest를 검색합니다.
- **쿼리 파라미터**:
  - `q` (String, required): 검색어
  - `page` (Integer, optional): 페이지 번호 (기본값: 1)
  - `size` (Integer, optional): 페이지 크기 (기본값: 10, 최대: 100)

#### 4. Contest 생성 (내부 API)
- **엔드포인트**: `POST /api/v1/contest/internal`
- **설명**: Contest를 생성합니다 (Batch 모듈 전용).
- **인증**: `X-Internal-Api-Key` 헤더 필요
- **요청 Body**: `ContestCreateRequest`

#### 5. Contest 다건 생성 (내부 API)
- **엔드포인트**: `POST /api/v1/contest/internal/batch`
- **설명**: Contest를 다건 생성합니다 (Batch 모듈 전용).
- **인증**: `X-Internal-Api-Key` 헤더 필요
- **요청 Body**: `ContestBatchRequest`
- **응답**: 성공/실패 통계 정보 포함

## 기술 스택

### 의존성

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Data MongoDB**: MongoDB Atlas 데이터 접근
- **Common 모듈**:
  - `common-core`: 공통 DTO 및 유틸리티
  - `common-exception`: 예외 처리
- **Domain 모듈**:
  - `domain-mongodb`: MongoDB Document 및 Repository

### 데이터베이스

- **MongoDB Atlas**: Contest 정보 저장 및 조회

## 설정

### application-contest-api.yml

```yaml
spring:
  application:
    name: contest-api
  profiles:
    include:
      - common-core
      - mongodb-domain

contest:
  internal:
    api-key: ${CONTEST_INTERNAL_API_KEY:default-internal-api-key-change-in-production}
```

### 환경 변수

- `CONTEST_INTERNAL_API_KEY`: 내부 API 키 (Batch 모듈에서 사용)
- `MONGODB_ATLAS_URI`: MongoDB Atlas 연결 URI
- `MONGODB_DATABASE`: MongoDB 데이터베이스 이름

## 에러 처리

### 커스텀 예외

- **ContestNotFoundException**: 대회를 찾을 수 없을 때
- **ContestValidationException**: 유효성 검증 실패 시
- **ContestDuplicateException**: 중복 데이터 시

### 에러 코드

- **4004**: 리소스 없음 (NOT_FOUND)
- **4005**: 충돌 (CONFLICT)
- **4006**: 유효성 검증 실패 (VALIDATION_ERROR)
- **5000**: 내부 서버 오류 (INTERNAL_SERVER_ERROR)

## 참고 문서

### 프로젝트 내부 문서

- **Contest 및 News API 설계서**: `docs/step9/contest-news-api-design.md`
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **에러 처리 전략 설계**: `docs/step2/4. error-handling-strategy-design.md`

### 공식 문서

- [Spring Data MongoDB 공식 문서](https://spring.io/projects/spring-data-mongodb)
- [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)

