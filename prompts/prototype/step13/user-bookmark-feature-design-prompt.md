# 사용자 북마크 기능 구현 설계서 작성 프롬프트

## 역할 정의

당신은 **백엔드 아키텍트**이자 **Spring Boot 전문가**입니다. 현재 프로젝트의 구조와 설계 패턴을 완전히 이해하고 있으며, CQRS 패턴과 Kafka 기반 이벤트 동기화를 활용하여 운영 환경에서 유지 가능한 사용자 북마크 시스템을 설계할 수 있는 전문가입니다.

## 프로젝트 컨텍스트

### 프로젝트 구조
- **프로젝트 타입**: Spring Boot 기반 멀티모듈 프로젝트
- **아키텍처 패턴**: CQRS 패턴 적용 (Command Side: Aurora MySQL, Query Side: MongoDB Atlas)
- **동기화 메커니즘**: Kafka 기반 이벤트 동기화
- **API 모듈 구조**: `api/auth`, `api/contest`, `api/news`, `api/gateway`, `api/bookmark`

### 현재 북마크 관련 구조
1. **Aurora MySQL (Command Side)**: `bookmark` 스키마의 `bookmarks` 테이블
   - 주요 필드: `id` (TSID), `user_id`, `item_type`, `item_id`, `tag`, `memo`, `is_deleted`, `deleted_at`
   - Soft Delete 지원 (`is_deleted` 플래그)
   - 인덱스: `user_id + is_deleted`, `user_id + item_type + item_id` (UNIQUE)
2. **MongoDB Atlas (Query Side)**: `BookmarkDocument` 컬렉션
   - 주요 필드: `bookmarkTsid`, `userId`, `itemType`, `itemId`, `itemTitle`, `itemSummary`, `tag`, `memo`, `bookmarkedAt`
   - 비정규화된 필드: `itemTitle`, `itemSummary` (원본 아이템 정보 중복 저장)
   - 인덱스: `userId + createdAt`, `userId + itemType + createdAt`
3. **Kafka 이벤트**: `BookmarkCreatedEvent`, `BookmarkUpdatedEvent`, `BookmarkDeletedEvent`, `BookmarkRestoredEvent`
4. **동기화 서비스**: `BookmarkSyncService` (MongoDB Atlas 동기화)

### 기존 설계서 참고 경로
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md` (BookmarkDocument 섹션)
- **Aurora 스키마 설계**: `docs/step1/3. aurora-schema-design.md` (bookmarks 테이블 섹션)
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md` (사용자 북마크 API 엔드포인트 섹션)
- **데이터 모델 설계**: `docs/step2/2. data-model-design.md` (Bookmark 엔티티 및 BookmarkDocument 섹션)
- **CQRS Kafka 동기화 설계**: `docs/step11/cqrs-kafka-sync-design.md`
- **Contest/News API 설계**: `docs/step9/contest-news-api-design.md` (API 패턴 참고)

### 참고 자료
- **Spring Data JPA 공식 문서**: https://spring.io/projects/spring-data-jpa
- **Spring Data MongoDB 공식 문서**: https://spring.io/projects/spring-data-mongodb
- **MySQL 공식 문서**: https://dev.mysql.com/doc/
- **MongoDB Atlas 공식 문서**: https://www.mongodb.com/docs/atlas/
- **Kafka 공식 문서**: https://kafka.apache.org/documentation/

## 설계서 작성 요구사항

### 필수 요구사항

#### 1. 북마크 저장 및 관리 기능
- **저장 대상**: 로그인한 사용자가 조회할 수 있는 모든 `ContestDocument` 및 `NewsArticleDocument` 정보
- **저장 기능**:
  - 사용자가 원하는 contest/news 아이템을 개인 북마크에 저장
  - 저장 시 태그(`tag`)와 메모(`memo`) 필드 설정 가능 (선택 사항)
- **수정 기능**:
  - 저장된 북마크의 태그(`tag`)와 메모(`memo`) 필드 수정 가능
  - 원본 아이템 정보(`itemTitle`, `itemSummary` 등)는 수정 불가 (원본 참조)
  - **중요**: `itemTitle`, `itemSummary`는 BookmarkEntity에 없는 필드이므로 BookmarkUpdatedEvent의 updatedFields에 포함할 수 없음
- **삭제 기능**:
  - Soft Delete 방식으로 삭제 (`is_deleted = true`, `deleted_at` 설정)
  - 삭제된 북마크는 기본 조회에서 제외
- **삭제된 북마크 조회 및 복구**:
  - 일정 기간 동안 삭제한 북마크 목록 조회 가능
  - 삭제된 북마크 복구 기능 (Soft Delete 해제)
  - 복구 기간 제한 정책 설계 (예: 30일, 90일 등)

#### 2. 북마크 검색 기능
- **검색 대상**: 로그인한 사용자의 개인 북마크에 저장된 모든 contest, news 정보
- **검색 기준**:
  - **태그(`tag`) 기반 검색**: 태그 필드에서 키워드 검색
  - **메모(`memo`) 기반 검색**: 메모 필드에서 키워드 검색
  - **통합 검색**: 태그와 메모를 모두 포함한 통합 검색
- **검색 최적화**:
  - MongoDB Atlas Full-text Search 또는 정규식 검색 활용
  - 인덱스 최적화 전략
  - 검색 성능 고려

#### 3. 북마크 정렬 기능
- **정렬 대상**: 로그인한 사용자의 개인 북마크에 저장된 모든 contest, news 정보
- **정렬 기준**: 아이템 원본의 정보를 기준으로 정렬
  - **날짜 정보 우선**: 
    - Contest: `startDate`, `endDate`, `createdAt` (원본 ContestDocument의 필드)
    - News: `publishedAt`, `createdAt` (원본 NewsArticleDocument의 필드)
  - **기타 정렬 옵션**:
    - 제목(`itemTitle`) 기준 정렬
    - 북마크 일시(`bookmarkedAt`) 기준 정렬
    - 생성 일시(`createdAt`) 기준 정렬
- **정렬 방향**: 오름차순(ASC), 내림차순(DESC) 지원
- **정렬 최적화**:
  - MongoDB Atlas 인덱스 활용 (ESR 규칙 준수)
  - 복합 정렬 지원 (예: 날짜 내림차순 → 제목 오름차순)

#### 4. 프로젝트 구조 및 기존 설계 참고
- **현재 프로젝트 구조 완전히 파악**:
  - 멀티모듈 구조 (`api`, `domain`, `common`, `batch`, `client`)
  - 패키지 구조 패턴 (`controller`, `facade`, `service`, `dto`, `config`, `common/exception`)
  - 기존 API 모듈 구조 (`api-contest`, `api-news`, `api-auth`) 참고
- **기존 설계서 철저히 분석**:
  - `docs/step1/2. mongodb-schema-design.md`: BookmarkDocument 스키마 구조
  - `docs/step1/3. aurora-schema-design.md`: bookmarks 테이블 DDL 및 제약조건
  - `docs/step2/1. api-endpoint-design.md`: 북마크 API 엔드포인트 설계
  - `docs/step2/2. data-model-design.md`: Bookmark 엔티티 및 BookmarkDocument 상세 설계
  - `docs/step11/cqrs-kafka-sync-design.md`: CQRS 패턴 및 Kafka 동기화 전략
- **기존 구현 코드 참고**:
  - `BookmarkSyncServiceImpl`: Kafka 이벤트 동기화 로직
  - `BookmarkRepository`: MongoDB 조회 메서드
  - `api-contest`, `api-news` 모듈의 Controller, Facade, Service 패턴

#### 5. 외부 자료 참고 규칙
- **공식 출처만 사용**: 다음 출처의 정보만 참고
  - Spring Framework 공식 문서 (https://spring.io/projects/spring-framework)
  - Spring Data JPA 공식 문서 (https://spring.io/projects/spring-data-jpa)
  - Spring Data MongoDB 공식 문서 (https://spring.io/projects/spring-data-mongodb)
  - MySQL 공식 문서 (https://dev.mysql.com/doc/)
  - MongoDB Atlas 공식 문서 (https://www.mongodb.com/docs/atlas/)
  - Apache Kafka 공식 문서 (https://kafka.apache.org/documentation/)
- **비공식 자료 금지**: 블로그, 개인 문서, Stack Overflow 등 비공식 자료는 참고하지 않음
- **버전 정보**: 모든 라이브러리와 서비스의 버전 명시 (현재 프로젝트에서 사용 중인 버전 기준)

#### 6. 오버엔지니어링 방지
- **최소 구현 원칙**: 현재 요구사항에 명시된 기능만 설계
  - 요구사항에 없는 기능은 설계하지 않음
  - 향후 확장 가능성은 언급하되, 현재는 구현하지 않음
- **복잡도 관리**: 불필요한 추상화 레이어 지양
  - 기존 프로젝트 구조를 벗어나지 않음
  - 단순하고 명확한 구조 유지
- **단순성 우선**: 이해하기 쉽고 유지보수 가능한 구조
  - 과도한 디자인 패턴 사용 지양
  - 명확한 책임 분리
- **단계적 확장**: 향후 확장 가능하되 현재는 최소 기능만
  - 확장 포인트는 명시하되, 현재는 구현하지 않음

#### 7. 객체지향 설계 및 클린코드 원칙
- **SOLID 원칙 준수**:
  - **Single Responsibility Principle**: 각 클래스는 단일 책임만 가짐
    - Controller: HTTP 요청/응답 처리
    - Facade: 여러 Service 조합 및 트랜잭션 경계 설정
    - Service: 비즈니스 로직 처리
    - Repository: 데이터 접근 로직
  - **Open/Closed Principle**: 확장에는 열려있고 수정에는 닫혀있음
    - 인터페이스 기반 설계
    - 전략 패턴 활용 (필요 시)
  - **Liskov Substitution Principle**: 인터페이스 기반 설계
    - Service 인터페이스와 구현체 분리
  - **Interface Segregation Principle**: 작은 인터페이스 선호
    - 필요한 메서드만 포함하는 인터페이스 설계
  - **Dependency Inversion Principle**: 의존성 역전
    - 고수준 모듈이 저수준 모듈에 의존하지 않음
    - 인터페이스를 통한 의존성 주입
- **클린코드 원칙**:
  - **의미 있는 이름 사용**: 변수, 메서드, 클래스명이 의도를 명확히 표현
  - **작은 함수/클래스**: 단일 책임을 가진 작은 단위로 구성
  - **주석보다 코드로 의도 표현**: 코드 자체가 문서 역할
  - **DRY (Don't Repeat Yourself)**: 중복 코드 제거
  - **에러 처리**: 명확한 예외 처리 및 에러 메시지
- **디자인 패턴**: 적절한 패턴 사용
  - **Facade 패턴**: Controller와 Service 사이의 중간 계층
  - **Repository 패턴**: 데이터 접근 추상화
  - **Strategy 패턴**: 정렬 전략 등 (필요 시)

## 설계서 작성 지시사항

### 1단계: 프로젝트 분석
1. 현재 프로젝트의 모든 코드와 설계서를 철저히 분석
   - `api-contest`, `api-news` 모듈의 구조 및 패턴 분석
   - `BookmarkDocument`, `Bookmark` 엔티티 구조 완전히 이해
   - Kafka 이벤트 동기화 구조 이해
   - CQRS 패턴 적용 방식 이해
2. 기존 북마크 관련 설계서 완전히 파악
   - MongoDB 스키마 설계 (`docs/step1/2. mongodb-schema-design.md`)
   - Aurora 스키마 설계 (`docs/step1/3. aurora-schema-design.md`)
   - API 엔드포인트 설계 (`docs/step2/1. api-endpoint-design.md`)
   - 데이터 모델 설계 (`docs/step2/2. data-model-design.md`)
3. 기존 API 패턴 및 설계 원칙 파악
   - Controller → Facade → Service → Repository 계층 구조
   - DTO 설계 패턴 (Request/Response 분리)
   - 예외 처리 전략
   - 인증/인가 처리 방식

### 2단계: 요구사항 분석
1. 각 요구사항별 상세 분석
   - 북마크 저장/수정/삭제/복구 기능 요구사항
   - 검색 기능 요구사항 (태그, 메모 기반)
   - 정렬 기능 요구사항 (원본 아이템 정보 기준)
2. 요구사항 간 의존성 파악
   - 저장 기능 → 수정/삭제 기능
   - 검색 기능 → 저장 기능
   - 정렬 기능 → 저장 기능
3. 우선순위 결정
   - 핵심 기능 우선 (저장, 조회, 수정, 삭제)
   - 부가 기능 후순위 (검색, 정렬, 복구)
4. 제약 조건 명확화
   - CQRS 패턴 제약 (읽기: MongoDB, 쓰기: Aurora MySQL)
   - Soft Delete 제약
   - 권한 제약 (사용자별 데이터 격리)

### 3단계: 아키텍처 설계
1. 전체 시스템 아키텍처 다이어그램 (Mermaid 형식)
   - API 모듈 구조
   - CQRS 패턴 적용 구조
   - Kafka 이벤트 동기화 흐름
2. 컴포넌트 간 상호작용 흐름도
   - 북마크 저장 흐름
   - 북마크 조회 흐름 (일반 조회, 검색, 정렬)
   - 북마크 수정/삭제/복구 흐름
3. 데이터 흐름도
   - Command Side (Aurora MySQL) → Kafka → Query Side (MongoDB Atlas)
   - 원본 아이템 정보 조회 (ContestDocument, NewsArticleDocument)
4. 모듈 구조 및 패키지 구조
   - `api-bookmark` 모듈 구조
   - 패키지 구조 (`controller`, `facade`, `service`, `dto`, `config`, `common/exception`)

### 4단계: 상세 설계
각 섹션별로 다음 내용 포함:

#### 4.1 API 엔드포인트 설계
- **RESTful API 설계**: 현재 프로젝트의 API 패턴 준수
  - 경로: `/api/v1/bookmark` 또는 `/api/v1/bookmarks`
  - HTTP 메서드: `GET`, `POST`, `PUT`, `DELETE` 등
- **엔드포인트 목록**:
  - `POST /api/v1/bookmark`: 북마크 저장
  - `GET /api/v1/bookmark`: 북마크 목록 조회 (페이징, 필터링, 정렬)
  - `GET /api/v1/bookmark/{id}`: 북마크 상세 조회
  - `PUT /api/v1/bookmark/{id}`: 북마크 수정 (태그, 메모)
  - `DELETE /api/v1/bookmark/{id}`: 북마크 삭제 (Soft Delete)
  - `GET /api/v1/bookmark/deleted`: 삭제된 북마크 목록 조회
  - `POST /api/v1/bookmark/{id}/restore`: 북마크 복구
  - `GET /api/v1/bookmark/search`: 북마크 검색 (태그, 메모 기반)
  - `GET /api/v1/bookmark/history/{entityId}`: 변경 이력 조회 (CQRS 패턴 예외, Aurora MySQL)
  - `GET /api/v1/bookmark/history/{entityId}/at?timestamp={timestamp}`: 특정 시점 데이터 조회 (CQRS 패턴 예외, Aurora MySQL)
  - `POST /api/v1/bookmark/history/{entityId}/restore?historyId={historyId}`: 특정 버전으로 복구 (관리자만)
- **요청 DTO 설계**: 각 엔드포인트별 Request DTO
- **응답 DTO 설계**: 각 엔드포인트별 Response DTO
- **에러 처리**: 현재 프로젝트의 예외 처리 패턴 준수
- **인증/인가**: Spring Security 통합, JWT 토큰에서 userId 추출

#### 4.2 북마크 저장 기능 설계
- **저장 대상 확인**: ContestDocument, NewsArticleDocument 존재 여부 확인
- **중복 검증**: `user_id + item_type + item_id` UNIQUE 제약조건 확인
- **원본 아이템 정보 조회**: MongoDB Atlas에서 원본 아이템 정보 조회
  - `itemTitle`, `itemSummary` 필드 추출
  - Contest: `ContestDocument`에서 `title`, `description` 조회
  - News: `NewsArticleDocument`에서 `title`, `summary` 조회
- **Aurora MySQL 저장**: `Bookmark` 엔티티 생성
- **Kafka 이벤트 발행**: `BookmarkCreatedEvent` 발행
- **MongoDB Atlas 동기화**: `BookmarkSyncService`를 통한 자동 동기화

#### 4.3 북마크 수정 기능 설계
- **수정 가능 필드**: `tag`, `memo`만 수정 가능
- **원본 아이템 정보 동기화**: 원본 아이템 정보 변경 시 `itemTitle`, `itemSummary` 업데이트 전략
  - **중요**: `itemTitle`, `itemSummary`는 BookmarkEntity에 없는 필드이므로 BookmarkUpdatedEvent의 updatedFields에 포함할 수 없음
  - 원본 아이템(ContestDocument/NewsArticleDocument) 변경 시 별도의 동기화 메커니즘 필요
- **Aurora MySQL 업데이트**: `Bookmark` 엔티티 업데이트
- **Kafka 이벤트 발행**: `BookmarkUpdatedEvent` 발행 (변경된 필드만 포함)
- **MongoDB Atlas 동기화**: `BookmarkSyncService`를 통한 자동 동기화

#### 4.4 북마크 삭제 및 복구 기능 설계
- **Soft Delete 구현**: `is_deleted = true`, `deleted_at` 설정
- **삭제된 북마크 조회**: CQRS 패턴 예외로 Aurora MySQL에서 조회
  - `GET /api/v1/bookmark/deleted` 엔드포인트
  - 복구 기간 제한 정책 (예: 30일, 90일)
- **북마크 복구**: `is_deleted = false`, `deleted_at = null` 설정
- **Kafka 이벤트 발행**: `BookmarkDeletedEvent`, `BookmarkRestoredEvent` 발행
- **MongoDB Atlas 동기화**: 
  - 삭제 시: Document 물리적 삭제
  - 복구 시: Document 새로 생성

#### 4.5 북마크 검색 기능 설계
- **검색 대상**: 사용자별 북마크 (`userId` 필터링)
- **검색 필드**: `tag`, `memo` 필드
- **검색 방식**:
  - MongoDB Atlas Full-text Search 또는 정규식 검색
  - 태그 검색: `tag` 필드에서 키워드 검색
  - 메모 검색: `memo` 필드에서 키워드 검색
  - 통합 검색: `tag` 또는 `memo` 필드에서 키워드 검색
- **인덱스 최적화**: 검색 성능을 위한 인덱스 설계
- **페이징**: 검색 결과 페이징 처리

#### 4.6 북마크 정렬 기능 설계
- **정렬 대상**: 사용자별 북마크 (`userId` 필터링)
- **정렬 기준**: 원본 아이템 정보 기준 정렬
  - **Contest 정렬**:
    - `startDate` (ContestDocument의 `startDate`)
    - `endDate` (ContestDocument의 `endDate`)
    - `createdAt` (ContestDocument의 `createdAt`)
  - **News 정렬**:
    - `publishedAt` (NewsArticleDocument의 `publishedAt`)
    - `createdAt` (NewsArticleDocument의 `createdAt`)
  - **공통 정렬**:
    - `itemTitle` (BookmarkDocument의 `itemTitle`)
    - `bookmarkedAt` (BookmarkDocument의 `bookmarkedAt`)
    - `createdAt` (BookmarkDocument의 `createdAt`)
- **정렬 방향**: ASC, DESC 지원
- **복합 정렬**: 여러 필드 기준 정렬 지원
- **인덱스 최적화**: ESR 규칙 준수한 인덱스 설계
- **원본 아이템 정보 조회**: 정렬을 위한 원본 아이템 정보 조회 전략
  - 옵션 1: BookmarkDocument에 원본 날짜 정보 비정규화 저장 (추가 필드 필요)
  - 옵션 2: 정렬 시 원본 아이템 조회 후 정렬 (성능 고려)
  - 권장 방안 제시 및 근거

#### 4.7 북마크 조회 기능 설계
- **기본 조회**: 사용자별 북마크 목록 조회 (MongoDB Atlas)
- **필터링**: `itemType` 필터 (CONTEST, NEWS_ARTICLE)
- **페이징**: 페이지 번호, 페이지 크기 설정
- **정렬**: 위의 정렬 기능 활용
- **상세 조회**: `bookmarkTsid` 또는 `ObjectId`로 상세 조회

#### 4.8 권한 관리 설계
- **사용자별 데이터 격리**: JWT 토큰에서 `userId` 추출
- **권한 검증**: 
  - `@PreAuthorize` 어노테이션 활용
  - 사용자는 본인의 북마크만 조회/수정/삭제 가능
- **인증 필수**: 모든 북마크 API는 인증 필수

#### 4.9 히스토리 관리 기능 설계
- **자동 히스토리 저장**: `HistoryEntityListener`를 통한 자동 히스토리 저장
  - `@EntityListeners(HistoryEntityListener.class)` 어노테이션 활용
  - `@PrePersist`: INSERT 작업 시 히스토리 저장 (operation_type: INSERT)
  - `@PreUpdate`: UPDATE/DELETE 작업 시 히스토리 저장 (operation_type: UPDATE/DELETE)
  - `before_data`, `after_data` JSON 필드에 전체 엔티티 데이터 저장
- **변경 이력 조회**: `GET /api/v1/bookmark/history/{entityId}`
  - CQRS 패턴 예외로 Aurora MySQL `BookmarkHistory` 테이블 조회
  - 페이징, 필터링 지원 (operationType, startDate, endDate)
  - 권한 검증: 관리자 또는 본인만 조회 가능
  - 인덱스 활용: `operation_type + changed_at` 복합 인덱스
- **특정 시점 데이터 조회**: `GET /api/v1/bookmark/history/{entityId}/at?timestamp={timestamp}`
  - CQRS 패턴 예외로 Aurora MySQL 히스토리 테이블 조회
  - 해당 시점 이전의 가장 최근 이력 조회
  - `after_data` JSON 필드에서 데이터 추출
  - 권한 검증: 관리자 또는 본인만 조회 가능
- **특정 버전으로 복구**: `POST /api/v1/bookmark/history/{entityId}/restore?historyId={historyId}`
  - 히스토리 엔티티의 `after_data` JSON 필드를 기반으로 엔티티 복구
  - Aurora MySQL에서 엔티티 업데이트
  - 히스토리 엔티티 생성 (operation_type: UPDATE)
  - Kafka 이벤트 발행: `BookmarkUpdatedEvent`
  - 권한 검증: 관리자만 복구 가능
- **히스토리 테이블 구조**: `BookmarkHistory` 엔티티
  - `history_id` (TSID Primary Key)
  - `bookmark_id` (Foreign Key, Bookmark 테이블 참조)
  - `operation_type` (INSERT, UPDATE, DELETE)
  - `before_data` (JSON, 변경 전 데이터)
  - `after_data` (JSON, 변경 후 데이터)
  - `changed_by` (변경한 사용자 ID)
  - `changed_at` (변경 일시)
  - `change_reason` (변경 사유, 선택 사항)

#### 4.10 프로젝트 구조 통합
- **모듈 구조**: `api-bookmark` 모듈 구조 설계
  - 기존 `api-contest`, `api-news` 모듈 구조 참고
- **패키지 구조**: 
  - `controller`: REST API 엔드포인트
  - `facade`: Controller와 Service 사이의 중간 계층
  - `service`: 비즈니스 로직 처리
    - `BookmarkCommandService`: Aurora MySQL 쓰기 작업
    - `BookmarkQueryService`: MongoDB Atlas 읽기 작업
    - `BookmarkHistoryService`: 히스토리 조회 및 복구 작업 (Aurora MySQL)
  - `dto`: Request/Response DTO
  - `config`: 설정 클래스
  - `common/exception`: 예외 처리
- **의존성 관리**: `common`, `domain` 모듈 활용
- **설정 파일**: `application-bookmark-api.yml` 패턴 준수

### 5단계: 구현 가이드
1. 단계별 구현 순서
   - Domain 모듈 확장 (필요 시)
   - Service 레이어 구현
   - Facade 레이어 구현
   - Controller 레이어 구현
   - DTO 구현
   - 예외 처리 구현
2. 각 컴포넌트 구현 가이드
   - 코드 예제 (의사코드 또는 실제 코드)
   - 인터페이스 설계
   - 의존성 주입 설계
3. 설정 파일 예제
   - `application-bookmark-api.yml`
   - `build.gradle` 의존성
4. 테스트 전략
   - 단위 테스트
   - 통합 테스트
   - API 테스트

### 6단계: 검증 기준
1. 기능 검증 기준
   - 북마크 저장/수정/삭제/복구 기능 검증
   - 검색 기능 검증
   - 정렬 기능 검증
2. 성능 검증 기준
   - 조회 성능 (MongoDB Atlas 인덱스 활용)
   - 검색 성능
   - 정렬 성능
3. 데이터 일관성 검증 기준
   - CQRS 패턴 동기화 검증
   - 원본 아이템 정보 동기화 검증
4. 권한 검증 기준
   - 사용자별 데이터 격리 검증
   - 인증/인가 검증

## 설계서 출력 형식

### 문서 구조
```markdown
# 사용자 북마크 기능 구현 설계서

**작성 일시**: YYYY-MM-DD
**대상 모듈**: `api-bookmark`
**목적**: 사용자 북마크 저장, 조회, 수정, 삭제, 복구, 검색, 정렬 기능 구현 설계

## 목차
1. [개요](#개요)
2. [설계 원칙](#설계-원칙)
3. [현재 프로젝트 분석](#현재-프로젝트-분석)
4. [아키텍처 설계](#아키텍처-설계)
5. [상세 설계](#상세-설계)
   - [API 엔드포인트 설계](#api-엔드포인트-설계)
   - [북마크 저장 기능 설계](#북마크-저장-기능-설계)
   - [북마크 수정 기능 설계](#북마크-수정-기능-설계)
   - [북마크 삭제 및 복구 기능 설계](#북마크-삭제-및-복구-기능-설계)
   - [북마크 검색 기능 설계](#북마크-검색-기능-설계)
   - [북마크 정렬 기능 설계](#북마크-정렬-기능-설계)
   - [북마크 조회 기능 설계](#북마크-조회-기능-설계)
   - [권한 관리 설계](#권한-관리-설계)
   - [히스토리 관리 기능 설계](#히스토리-관리-기능-설계)
   - [프로젝트 구조 통합](#프로젝트-구조-통합)
6. [구현 가이드](#구현-가이드)
7. [검증 기준](#검증-기준)
8. [참고 자료](#참고-자료)
```

### 다이어그램 형식
- **Mermaid 형식 사용**: 모든 다이어그램은 Mermaid 형식으로 작성
- 다이어그램 종류:
  - 아키텍처 다이어그램
  - 시퀀스 다이어그램 (저장, 조회, 수정, 삭제, 복구, 검색, 정렬 흐름)
  - 클래스 다이어그램 (주요 컴포넌트)
  - 데이터 흐름도

### 코드 예제
- **Java 코드**: 실제 구현 가능한 코드 예제 제공
- **설정 파일**: `application.yml`, `build.gradle` 예제
- **의사코드**: 복잡한 로직은 의사코드로 설명

## 중요 지침

### 반드시 준수할 사항
1. ✅ **프로젝트 구조 준수**: 현재 프로젝트의 패턴과 구조 완전히 준수
2. ✅ **CQRS 패턴 준수**: 읽기 작업은 MongoDB Atlas, 쓰기 작업은 Aurora MySQL
3. ✅ **기존 설계서 참고**: `docs` 폴더의 모든 관련 설계서 완전히 분석
4. ✅ **객체지향 설계**: SOLID 원칙 및 클린코드 원칙 준수
5. ✅ **오버엔지니어링 방지**: 요구사항에 명시된 기능만 설계
6. ✅ **공식 문서만 참고**: 신뢰할 수 있는 공식 출처만 사용
7. ✅ **운영 환경 고려**: 실제 운영 환경에서 유지 가능한 설계

### 금지 사항
1. ❌ **비공식 자료 참고**: 블로그, 개인 문서 등 비공식 자료 사용 금지
2. ❌ **과도한 추상화**: 불필요한 추상화 레이어 생성 금지
3. ❌ **불명확한 설계**: 모호하거나 구현 불가능한 설계 금지
4. ❌ **프로젝트 구조 무시**: 기존 프로젝트 구조와 다른 설계 금지
5. ❌ **요구사항 외 기능**: 요구사항에 명시되지 않은 기능 설계 금지
6. ❌ **하드코딩**: 설정값, 상수값은 설정 파일 또는 상수 클래스로 관리

## 최종 확인 사항

설계서 작성 완료 후 다음 사항을 확인:

- [ ] 북마크 저장/수정/삭제/복구 기능 설계가 포함되었는지 확인
- [ ] 북마크 검색 기능 설계가 포함되었는지 확인 (태그, 메모 기반)
- [ ] 북마크 정렬 기능 설계가 포함되었는지 확인 (원본 아이템 정보 기준)
- [ ] 히스토리 관리 기능 설계가 포함되었는지 확인 (변경 이력 조회, 특정 시점 데이터 조회, 특정 버전으로 복구)
- [ ] 현재 프로젝트 구조와 통합 가능한지 확인
- [ ] CQRS 패턴이 올바르게 적용되었는지 확인 (히스토리 조회는 CQRS 패턴 예외)
- [ ] Kafka 이벤트 동기화가 포함되었는지 확인
- [ ] 권한 관리 설계가 포함되었는지 확인
- [ ] 공식 문서만 참고했는지 확인
- [ ] 오버엔지니어링을 방지했는지 확인
- [ ] 객체지향 설계 원칙을 준수했는지 확인
- [ ] 모든 다이어그램이 Mermaid 형식인지 확인
- [ ] 구현 가능한 코드 예제가 포함되었는지 확인
- [ ] 기존 설계서(`docs` 폴더)를 완전히 참고했는지 확인

## 시작 지시

위의 모든 요구사항과 지침을 준수하여 **사용자 북마크 기능 구현 설계서**를 작성하세요.

설계서는 `docs/step13/user-bookmark-feature-design.md` 경로에 저장될 예정입니다.

**중요**: 설계서는 실제 구현 가능해야 하며, 현재 프로젝트에 바로 통합 가능한 수준의 상세함을 가져야 합니다.
