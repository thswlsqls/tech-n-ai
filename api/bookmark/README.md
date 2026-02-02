# Archive API 모듈

## 개요

`api-archive` 모듈은 사용자 아카이브 기능을 제공하는 REST API 모듈입니다. 로그인한 사용자가 조회할 수 있는 모든 contest, news 정보를 개인 아카이브에 저장하고, 태그와 메모를 수정하며, 삭제 및 복구할 수 있는 기능을 제공합니다.

## 주요 기능

### 1. 아카이브 관리
- **저장**: ContestDocument, NewsArticleDocument를 개인 아카이브에 저장
- **조회**: 사용자별 아카이브 목록 및 상세 조회
- **수정**: 태그(`tag`), 메모(`memo`) 필드 수정
- **삭제**: Soft Delete 방식으로 삭제
- **복구**: 삭제된 아카이브 복구 (일정 기간 내)

### 2. 검색 및 정렬
- **검색**: 태그, 메모 기반 검색
- **정렬**: 원본 아이템 정보(날짜 등) 기준 정렬

### 3. 히스토리 관리
- **변경 이력 조회**: 특정 아카이브의 변경 이력 조회
- **특정 시점 데이터 조회**: 특정 시점의 아카이브 데이터 조회
- **버전 복구**: 특정 히스토리 버전으로 아카이브 복구

## 아키텍처

### CQRS 패턴 적용

이 모듈은 CQRS(Command Query Responsibility Segregation) 패턴을 적용합니다:

- **Command Side (쓰기)**: Aurora MySQL `archive` 스키마
  - `archives` 테이블: 아카이브 정보 저장
  - `archive_history` 테이블: 아카이브 변경 이력 저장
- **Query Side (읽기)**: MongoDB Atlas `ArchiveDocument`
  - 일반 조회, 검색, 정렬은 MongoDB Atlas 사용
  - 삭제된 아카이브 조회, 히스토리 조회는 CQRS 패턴 예외로 Aurora MySQL 사용

### Kafka 이벤트 동기화

모든 쓰기 작업 후 Kafka 이벤트를 발행하여 MongoDB Atlas에 자동 동기화합니다:

- **이벤트 종류**:
  - `ArchiveCreatedEvent`: 아카이브 생성
  - `ArchiveUpdatedEvent`: 아카이브 수정
  - `ArchiveDeletedEvent`: 아카이브 삭제 (Soft Delete)
  - `ArchiveRestoredEvent`: 아카이브 복구
- **동기화 메커니즘**:
  - `EventPublisher`: Kafka 이벤트 발행 (토픽: `"archive-events"`, Partition Key: `archiveTsid`)
  - `EventConsumer`: Kafka 이벤트 수신 및 멱등성 보장 (Redis 기반)
  - `ArchiveSyncService`: MongoDB Atlas 동기화 처리
- **동기화 지연 시간**: 목표 1초 이내

### 계층 구조

```
Controller → Facade → Service → Repository
```

- **Controller**: HTTP 요청/응답 처리
- **Facade**: Controller와 Service 사이의 중간 계층, 비즈니스 로직 조합
- **Service**: 
  - `ArchiveCommandService`: Aurora MySQL 쓰기 작업
  - `ArchiveQueryService`: MongoDB Atlas 읽기 작업
  - `ArchiveHistoryService`: 히스토리 조회 및 복구 작업
- **Repository**: 데이터 접근 로직

## API 엔드포인트

### 기본 정보

- **Base URL**: `/api/v1/archive`
- **인증**: 모든 엔드포인트는 JWT Access Token 필요
- **응답 형식**: `ApiResponse<T>` 사용 (표준 응답 형식)

### 엔드포인트 목록

#### 1. 아카이브 저장
- **엔드포인트**: `POST /api/v1/archive`
- **설명**: 새로운 아카이브를 추가합니다.

#### 2. 아카이브 목록 조회
- **엔드포인트**: `GET /api/v1/archive`
- **설명**: 사용자의 아카이브 목록을 조회합니다.
- **쿼리 파라미터**:
  - `page` (Integer, optional): 페이지 번호 (기본값: 1)
  - `size` (Integer, optional): 페이지 크기 (기본값: 10, 최대: 100)
  - `itemType` (String, optional): 항목 타입 필터 ("CONTEST", "NEWS_ARTICLE")
  - `sort` (String, optional): 정렬 기준

#### 3. 아카이브 상세 조회
- **엔드포인트**: `GET /api/v1/archive/{id}`
- **설명**: 특정 아카이브의 상세 정보를 조회합니다.

#### 4. 아카이브 수정
- **엔드포인트**: `PUT /api/v1/archive/{id}`
- **설명**: 아카이브의 태그와 메모를 수정합니다.

#### 5. 아카이브 삭제
- **엔드포인트**: `DELETE /api/v1/archive/{id}`
- **설명**: 아카이브를 삭제합니다 (Soft Delete).

#### 6. 삭제된 아카이브 목록 조회
- **엔드포인트**: `GET /api/v1/archive/deleted`
- **설명**: 삭제된 아카이브 목록을 조회합니다.

#### 7. 아카이브 복구
- **엔드포인트**: `POST /api/v1/archive/{id}/restore`
- **설명**: 삭제된 아카이브를 복구합니다.

#### 8. 아카이브 검색
- **엔드포인트**: `GET /api/v1/archive/search`
- **설명**: 태그와 메모를 기준으로 아카이브를 검색합니다.
- **쿼리 파라미터**:
  - `q` (String, required): 검색어
  - `page` (Integer, optional): 페이지 번호
  - `size` (Integer, optional): 페이지 크기
  - `searchField` (String, optional): 검색 필드 ("tag", "memo", "all", 기본값: "all")

#### 9. 변경 이력 조회
- **엔드포인트**: `GET /api/v1/archive/history/{entityId}`
- **설명**: 특정 아카이브 엔티티의 변경 이력을 조회합니다.

#### 10. 특정 시점 데이터 조회
- **엔드포인트**: `GET /api/v1/archive/history/{entityId}/at`
- **설명**: 특정 시점의 아카이브 엔티티 데이터를 조회합니다.
- **쿼리 파라미터**:
  - `timestamp` (String, required): 시점 (ISO 8601)

#### 11. 특정 버전으로 복구
- **엔드포인트**: `POST /api/v1/archive/history/{entityId}/restore`
- **설명**: 특정 히스토리 버전으로 아카이브 엔티티를 복구합니다.
- **쿼리 파라미터**:
  - `historyId` (String, required): 히스토리 ID (TSID)

## 기술 스택

### 의존성

- **Spring Boot**: 웹 애플리케이션 프레임워크
- **Spring Data JPA**: Aurora MySQL 데이터 접근
- **Spring Data MongoDB**: MongoDB Atlas 데이터 접근
- **Spring Security**: 인증/인가
- **Spring Kafka**: Kafka 이벤트 발행/수신
- **Common 모듈**:
  - `common-core`: 공통 DTO 및 유틸리티
  - `common-exception`: 예외 처리
  - `common-kafka`: Kafka 이벤트 발행/수신
  - `common-security`: 보안 설정
- **Domain 모듈**:
  - `domain-aurora`: Aurora MySQL 엔티티 및 Repository
  - `domain-mongodb`: MongoDB Document 및 Repository

### 데이터베이스

- **Aurora MySQL**: 아카이브 정보 및 히스토리 저장 (Command Side)
- **MongoDB Atlas**: 아카이브 조회용 (Query Side)

## 설정

### application-archive-api.yml

```yaml
spring:
  application:
    name: archive-api
  profiles:
    include:
      - common-core
      - api-domain
      - mongodb-domain

module:
  aurora:
    schema: archive

archive:
  restore:
    max-days: 30  # 복구 가능 최대 기간 (일)
```

### 환경 변수

- `AURORA_URL`: Aurora MySQL 연결 URL
- `AURORA_USERNAME`: Aurora MySQL 사용자명
- `AURORA_PASSWORD`: Aurora MySQL 비밀번호
- `MONGODB_ATLAS_URI`: MongoDB Atlas 연결 URI
- `MONGODB_DATABASE`: MongoDB 데이터베이스 이름

## 권한 관리

- 모든 API는 JWT Access Token 인증 필수
- 사용자는 본인의 아카이브만 조회/수정/삭제 가능
- 히스토리 조회: 관리자 또는 본인만 가능
- 히스토리 복구: 관리자만 가능

## 참고 문서

### 프로젝트 내부 문서

- **사용자 아카이브 기능 구현 설계서**: `docs/step13/user-archive-feature-design.md`
- **CQRS Kafka 동기화 설계서**: `docs/step11/cqrs-kafka-sync-design.md`
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
- **Aurora 스키마 설계**: `docs/step1/3. aurora-schema-design.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **에러 처리 전략 설계**: `docs/step2/4. error-handling-strategy-design.md`

### 공식 문서

- [Spring Data JPA 공식 문서](https://spring.io/projects/spring-data-jpa)
- [Spring Data MongoDB 공식 문서](https://spring.io/projects/spring-data-mongodb)
- [MySQL 공식 문서](https://dev.mysql.com/doc/)
- [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)

