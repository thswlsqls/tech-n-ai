# Domain MongoDB Module

## 개요

`domain-mongodb` 모듈은 **CQRS 패턴의 Query Side (읽기 전용)**를 담당하는 도메인 모듈입니다. MongoDB Atlas 7.0+를 데이터베이스로 사용하며, 모든 읽기 작업(SELECT)을 처리합니다.

이 모듈은 읽기 성능 최적화를 위해 설계되었으며, 비정규화된 도큐먼트 구조, ESR 규칙을 준수한 인덱스 전략, 프로젝션 최적화 등의 기능을 제공합니다.

## 주요 특징

### 1. CQRS Query Side
- **역할**: 모든 읽기 작업 전용
- **데이터베이스**: MongoDB Atlas 7.0+
- **정규화 수준**: 비정규화 (읽기 최적화)
- **인덱스 전략**: ESR 규칙 준수 및 쿼리 패턴 기반 인덱스

### 2. 읽기 성능 최적화
- 비정규화된 도큐먼트 구조로 조인 최소화
- 프로젝션을 통한 네트워크 트래픽 최소화
- ESR 규칙을 준수한 복합 인덱스 설계

### 3. CQRS 동기화 지원
- Aurora MySQL의 TSID Primary Key와 1:1 매핑
- `archiveTsid`, `userTsid`, `session_id`, `message_id` 필드를 통한 동기화
- Kafka 이벤트 기반 실시간 동기화 (1초 이내 목표)

### 4. Vector Search 지원
- RAG 챗봇을 위한 벡터 검색 지원
- OpenAI text-embedding-3-small 임베딩 모델 사용
- MongoDB Atlas Vector Search 통합

### 5. TTL 인덱스
- 임시 데이터 자동 삭제 (뉴스 기사, 예외 로그 등)
- 90일 또는 1년 후 자동 삭제

## 모듈 구조

```
domain/mongodb/
├── src/main/java/com/tech/n/ai/domain/mongodb/
│   ├── config/
│   │   ├── MongoClientConfig.java      # MongoDB 클라이언트 설정 및 최적화
│   │   └── MongoIndexConfig.java       # 인덱스 자동 생성 설정
│   ├── document/
│   │   ├── ArchiveDocument.java        # 아카이브 도큐먼트
│   │   ├── ContestDocument.java        # 대회 정보 도큐먼트
│   │   ├── ConversationMessageDocument.java  # 대화 메시지 도큐먼트
│   │   ├── ConversationSessionDocument.java  # 대화 세션 도큐먼트
│   │   ├── ExceptionLogDocument.java  # 예외 로그 도큐먼트
│   │   ├── NewsArticleDocument.java    # 뉴스 기사 도큐먼트
│   │   ├── SourcesDocument.java        # 정보 출처 도큐먼트
│   │   └── UserProfileDocument.java    # 사용자 프로필 도큐먼트
│   └── repository/
│       ├── ArchiveRepository.java      # 아카이브 Repository
│       ├── ContestRepository.java      # 대회 정보 Repository
│       ├── ConversationMessageRepository.java  # 대화 메시지 Repository
│       ├── ConversationSessionRepository.java  # 대화 세션 Repository
│       ├── ExceptionLogRepository.java # 예외 로그 Repository
│       ├── NewsArticleRepository.java   # 뉴스 기사 Repository
│       ├── SourcesRepository.java      # 정보 출처 Repository
│       └── UserProfileRepository.java  # 사용자 프로필 Repository
└── src/main/resources/
    └── application-mongodb-domain.yml  # MongoDB 설정
```

## 주요 도큐먼트

### SourcesDocument

정보 출처(Source) 정보를 저장하는 도큐먼트입니다. RSS, Web Scraping, API 등 다양한 출처 타입을 지원합니다.

**주요 필드**:
- `name`: 출처 이름 (UNIQUE)
- `type`: 출처 타입 ("API", "RSS", "Web Scraping")
- `category`: 카테고리 ("개발자 대회 정보", "최신 IT 테크 뉴스 정보")
- `priority`: 우선순위 (1, 2, 3)
- `enabled`: 활성화 여부

### ContestDocument

개발자 대회 정보를 저장하는 도큐먼트입니다. 읽기 최적화를 위해 비정규화된 구조를 사용합니다.

**주요 필드**:
- `sourceId`: SourcesDocument 참조
- `title`: 대회 제목
- `startDate`, `endDate`: 시작/종료 일시
- `status`: 상태 ("UPCOMING", "ONGOING", "ENDED")
- `metadata.sourceName`: 출처 이름 (비정규화)

### NewsArticleDocument

IT 테크 뉴스 기사를 저장하는 도큐먼트입니다. 읽기 최적화를 위해 비정규화된 구조를 사용합니다.

**주요 필드**:
- `sourceId`: SourcesDocument 참조
- `title`: 기사 제목
- `content`: 기사 내용 (전체 텍스트)
- `summary`: 요약 (비정규화)
- `publishedAt`: 발행 일시
- `metadata.sourceName`: 출처 이름 (비정규화)

**TTL 인덱스**: `publishedAt` 필드에 90일 TTL 인덱스 적용

### ArchiveDocument

사용자가 아카이브한 항목을 저장하는 도큐먼트입니다. Aurora MySQL의 `Archive` 테이블과 동기화됩니다.

**주요 필드**:
- `archiveTsid`: Aurora MySQL Archive 테이블 TSID PK (UNIQUE, 동기화용)
- `userId`: 사용자 ID
- `itemType`: 항목 타입 ("CONTEST", "NEWS_ARTICLE")
- `itemId`: 항목 ID (ContestDocument 또는 NewsArticleDocument 참조)
- `itemTitle`, `itemSummary`: 항목 제목/요약 (비정규화)
- `embeddingText`: 임베딩 대상 텍스트 (RAG 챗봇용)
- `embeddingVector`: 벡터 필드 (1536차원)

### UserProfileDocument

사용자 프로필 정보를 저장하는 도큐먼트입니다. Aurora MySQL의 `User` 테이블과 동기화됩니다.

**주요 필드**:
- `userTsid`: Aurora MySQL User 테이블 TSID PK (UNIQUE, 동기화용)
- `userId`: 사용자 ID (UNIQUE)
- `username`: 사용자명 (UNIQUE)
- `email`: 이메일 (UNIQUE)
- `profileImageUrl`: 프로필 이미지 URL

### ConversationSessionDocument

대화 세션 정보를 저장하는 도큐먼트입니다. Aurora MySQL의 `ConversationSession` 테이블과 동기화됩니다.

**주요 필드**:
- `session_id`: Aurora MySQL ConversationSession 테이블 TSID PK (UNIQUE, 동기화용)
- `user_id`: 사용자 ID
- `title`: 세션 제목
- `last_message_at`: 마지막 메시지 시간
- `is_active`: 활성 세션 여부

**TTL 인덱스**: `last_message_at` 필드에 90일 TTL 인덱스 적용

### ConversationMessageDocument

대화 메시지 히스토리를 저장하는 도큐먼트입니다. Aurora MySQL의 `ConversationMessage` 테이블과 동기화됩니다.

**주요 필드**:
- `message_id`: Aurora MySQL ConversationMessage 테이블 TSID PK (UNIQUE, 동기화용)
- `session_id`: 세션 ID (ConversationSessionDocument 참조)
- `role`: 메시지 역할 ("USER", "ASSISTANT", "SYSTEM")
- `content`: 메시지 내용
- `token_count`: 토큰 수 (비용 계산용)
- `sequence_number`: 대화 순서 (1부터 시작)

**TTL 인덱스**: `created_at` 필드에 1년 TTL 인덱스 적용

### ExceptionLogDocument

예외 로그를 저장하는 도큐먼트입니다. 읽기/쓰기 예외를 모두 기록합니다.

**주요 필드**:
- `source`: 예외 소스 ("READ", "WRITE")
- `exceptionType`: 예외 타입
- `exceptionMessage`: 예외 메시지
- `stackTrace`: 스택 트레이스
- `context`: 컨텍스트 정보 (모듈명, 메서드명, 파라미터 등)
- `occurredAt`: 발생 일시
- `severity`: 심각도 ("LOW", "MEDIUM", "HIGH", "CRITICAL")

**TTL 인덱스**: `occurredAt` 필드에 90일 TTL 인덱스 적용

## 인덱스 전략

### ESR 규칙 준수

모든 복합 인덱스는 ESR 규칙을 준수하여 설계되었습니다:

1. **Equality (등가)**: 등가 조건에 사용되는 필드
2. **Sort (정렬)**: 정렬에 사용되는 필드
3. **Range (범위)**: 범위 쿼리에 사용되는 필드

### 주요 인덱스

| 컬렉션 | 인덱스 | 타입 | ESR 규칙 | 설명 |
|--------|--------|------|----------|------|
| `sources` | `name` | UNIQUE | - | 출처 이름 고유성 |
| `sources` | `category, priority` | 복합 | E, S | 카테고리별 우선순위 정렬 |
| `contests` | `sourceId, startDate` | 복합 | E, S | 출처별 시작 일시 정렬 |
| `contests` | `status, startDate` | 복합 | E, S | 상태별 시작 일시 정렬 |
| `news_articles` | `sourceId, publishedAt` | 복합 | E, S | 출처별 발행 일시 정렬 |
| `news_articles` | `publishedAt` | TTL | - | 90일 후 자동 삭제 |
| `archives` | `archiveTsid` | UNIQUE | - | Aurora MySQL 동기화용 |
| `archives` | `userId, createdAt` | 복합 | E, S | 사용자별 생성 일시 정렬 |
| `archives` | `userId, itemType, itemId` | UNIQUE | E, E, E | 중복 아카이브 방지 |
| `user_profiles` | `userTsid` | UNIQUE | - | Aurora MySQL 동기화용 |
| `user_profiles` | `userId`, `username`, `email` | UNIQUE | - | 각각 고유성 보장 |
| `conversation_sessions` | `session_id` | UNIQUE | - | Aurora MySQL 동기화용 |
| `conversation_sessions` | `user_id, is_active, last_message_at` | 복합 | E, E, S | 사용자별 활성 세션 조회 |
| `conversation_messages` | `message_id` | UNIQUE | - | Aurora MySQL 동기화용 |
| `conversation_messages` | `session_id, sequence_number` | 복합 | E, S | 세션별 메시지 순서 |

### 인덱스 자동 생성

`MongoIndexConfig` 클래스에서 애플리케이션 시작 시 인덱스를 자동으로 생성합니다.

## 환경 설정

### 필수 환경변수

MongoDB Atlas 연결을 위한 환경변수:

| 환경변수명 | 설명 | 예시 |
|-----------|------|------|
| `MONGODB_ATLAS_CONNECTION_STRING` | MongoDB Atlas 연결 문자열 | `mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/database?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true` |
| `MONGODB_ATLAS_DATABASE` | 데이터베이스 이름 | `shrimp_task_manager` |

### 연결 문자열 형식

**SRV Connection String** (권장):
```
mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true
```

**Standard Connection String**:
```
mongodb://{username}:{password}@{cluster-endpoint}:27017/{database}?ssl=true&replicaSet=...&readPreference=secondaryPreferred
```

### 연결 풀 최적화

`MongoClientConfig` 클래스에서 연결 풀을 최적화합니다:

- **최대 연결 수**: 100 (MongoDB Atlas 클러스터 티어에 따라 조정)
- **최소 연결 수**: 10 (연결 생성 오버헤드 감소)
- **Read Preference**: `secondaryPreferred` (읽기 복제본 우선)
- **Write Concern**: `majority` (데이터 일관성 보장)
- **Retry**: 읽기/쓰기 재시도 활성화

## 의존성

### 주요 의존성

- **Spring Boot Data MongoDB**: MongoDB 및 Repository 지원
- **Spring Boot Data MongoDB Reactive**: Reactive MongoDB 지원
- **MongoDB Driver**: MongoDB Java Driver

### build.gradle

```gradle
dependencies {
    implementation project(':common-core')
    
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.boot:spring-boot-starter-mongodb'
}
```

## Repository 사용 예제

### ArchiveRepository

```java
@Autowired
private ArchiveRepository archiveRepository;

// Aurora MySQL TSID로 도큐먼트 조회 (동기화 확인용)
Optional<ArchiveDocument> archive = archiveRepository.findByArchiveTsid("1234567890123456789");

// 사용자의 최근 아카이브 조회
List<ArchiveDocument> archives = archiveRepository.findByUserIdOrderByCreatedAtDesc("user123");

// 사용자의 특정 타입 아카이브 조회
List<ArchiveDocument> contestArchives = archiveRepository.findByUserIdAndItemTypeOrderByCreatedAtDesc("user123", "CONTEST");

// 페이징 지원
Page<ArchiveDocument> page = archiveRepository.findByUserId("user123", PageRequest.of(0, 20));
```

### ContestRepository

```java
@Autowired
private ContestRepository contestRepository;

// 특정 출처의 대회를 시작 일시 역순으로 조회
List<ContestDocument> contests = contestRepository.findBySourceIdOrderByStartDateDesc(sourceId);

// 진행 중인 대회 조회
List<ContestDocument> ongoingContests = contestRepository.findByStatusOrderByStartDateDesc("ONGOING");
```

### NewsArticleRepository

```java
@Autowired
private NewsArticleRepository newsArticleRepository;

// 특정 출처의 최신 뉴스 조회
Page<NewsArticleDocument> news = newsArticleRepository.findBySourceIdOrderByPublishedAtDesc(
    sourceId, 
    PageRequest.of(0, 20)
);

// 최근 7일간의 뉴스 조회
LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
List<NewsArticleDocument> recentNews = newsArticleRepository.findByPublishedAtAfterOrderByPublishedAtDesc(sevenDaysAgo);
```

## 프로젝션 최적화

필요한 필드만 선택하여 네트워크 트래픽을 최소화합니다:

```java
// 리스트 조회 시 요약 정보만
List<ContestDocument> contests = contestRepository.findByStatusOrderByStartDateDesc("UPCOMING");
// 프로젝션은 Repository 메서드에서 @Query 어노테이션으로 지정 가능

// 상세 조회 시 전체 정보
Optional<ContestDocument> contest = contestRepository.findById(contestId);
```

## CQRS 동기화

이 모듈은 Query Side로, Aurora MySQL(Command Side)의 데이터 변경을 Kafka 이벤트를 통해 수신하여 동기화합니다.

### 동기화 필드

다음 필드를 통해 Aurora MySQL과 1:1 매핑됩니다:

- `ArchiveDocument.archiveTsid` ↔ `Archive.id` (Aurora MySQL)
- `UserProfileDocument.userTsid` ↔ `User.id` (Aurora MySQL)
- `ConversationSessionDocument.session_id` ↔ `ConversationSession.session_id` (Aurora MySQL)
- `ConversationMessageDocument.message_id` ↔ `ConversationMessage.message_id` (Aurora MySQL)

### 동기화 지연 시간

- **목표**: 1초 이내
- **메커니즘**: Kafka 이벤트 기반 실시간 동기화
- **멱등성 보장**: Redis 기반 중복 처리 방지 (TTL: 7일)

자세한 내용은 다음 문서를 참고하세요:
- [CQRS Kafka 동기화 설계서](../../docs/step11/cqrs-kafka-sync-design.md)

## Vector Search (RAG 챗봇)

RAG 챗봇을 위한 벡터 검색을 지원합니다:

### 지원하는 컬렉션

- **ContestDocument**: `title + description + metadata.tags`
- **NewsArticleDocument**: `title + summary + content`
- **ArchiveDocument**: `itemTitle + itemSummary + tag + memo` (사용자별 필터링)

### 임베딩 모델

- **모델**: OpenAI text-embedding-3-small
- **차원**: 1536차원
- **비용**: $0.02 per 1M tokens

자세한 내용은 다음 문서를 참고하세요:
- [RAG 챗봇 설계서](../../docs/step12/rag-chatbot-design.md)

## 참고 문서

### 설계서
- [MongoDB Atlas 스키마 설계서](../../docs/step1/2.%20mongodb-schema-design.md)
- [MongoDB Atlas 베스트 프랙티스](../../docs/step1/mongodb-atlas-schema-design-best-practices.md)
- [CQRS Kafka 동기화 설계서](../../docs/step11/cqrs-kafka-sync-design.md)
- [RAG 챗봇 설계서](../../docs/step12/rag-chatbot-design.md)

### 공식 문서
- [Spring Data MongoDB 공식 문서](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [MongoDB Java Driver 공식 문서](https://www.mongodb.com/docs/drivers/java/sync/current/)
- [MongoDB Atlas 연결 가이드](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/)
- [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)

## 라이선스

이 모듈은 프로젝트의 라이선스를 따릅니다.

