# Domain Aurora Module

## 개요

`domain-aurora` 모듈은 **CQRS 패턴의 Command Side (쓰기 전용)**를 담당하는 도메인 모듈입니다. Amazon Aurora MySQL 3.x를 데이터베이스로 사용하며, 모든 쓰기 작업(CREATE, UPDATE, DELETE)을 처리합니다.

이 모듈은 프로젝트의 핵심 도메인 엔티티와 JPA Repository를 제공하며, TSID(Time-Sorted Unique Identifier) Primary Key 전략, Soft Delete, 히스토리 추적 등의 기능을 지원합니다.

## 주요 특징

### 1. CQRS Command Side
- **역할**: 모든 쓰기 작업 전용
- **데이터베이스**: Amazon Aurora MySQL 3.x
- **정규화 수준**: 높은 정규화 (최소 3NF)
- **인덱스 전략**: 쓰기 성능 최적화를 위한 최소 인덱스

### 2. TSID Primary Key 전략
- 모든 엔티티는 TSID를 Primary Key로 사용
- `TsidGenerator`를 통한 자동 생성
- 시간 기반 정렬 가능한 고유 식별자

### 3. Soft Delete 지원
- 모든 메인 엔티티는 `BaseEntity`를 상속받아 Soft Delete 기능 제공
- `is_deleted`, `deleted_at`, `deleted_by` 필드 자동 관리

### 4. 히스토리 추적
- `HistoryEntityListener`를 통한 자동 히스토리 저장
- 모든 쓰기 작업(INSERT, UPDATE, DELETE)에 대한 변경 이력 추적
- JSON 형식으로 변경 전/후 데이터 저장

### 5. 모듈별 스키마 분리
- API 모듈별로 독립적인 스키마 사용
- `module.aurora.schema` 환경변수를 통한 동적 스키마 매핑

## 모듈 구조

```
domain/aurora/
├── src/main/java/com/tech/n/ai/domain/mariadb/
│   ├── annotation/
│   │   └── Tsid.java                    # TSID 어노테이션
│   ├── config/
│   │   ├── ApiDataSourceConfig.java    # API 모듈용 DataSource 설정
│   │   ├── ApiDomainConfig.java        # API 모듈용 Domain 설정
│   │   ├── ApiMybatisConfig.java       # API 모듈용 MyBatis 설정
│   │   ├── BatchBusinessDataSourceConfig.java  # Batch 모듈용 Business DataSource
│   │   ├── BatchDomainConfig.java      # Batch 모듈용 Domain 설정
│   │   ├── BatchEntityManagerConfig.java       # Batch 모듈용 EntityManager 설정
│   │   ├── BatchJpaTransactionConfig.java     # Batch 모듈용 JPA Transaction 설정
│   │   ├── BatchMetaDataSourceConfig.java      # Batch 모듈용 Meta DataSource
│   │   └── BatchMyBatisConfig.java     # Batch 모듈용 MyBatis 설정
│   ├── entity/
│   │   ├── BaseEntity.java             # 기본 엔티티 (공통 필드)
│   │   ├── archive/
│   │   │   ├── ArchiveEntity.java       # 아카이브 엔티티
│   │   │   └── ArchiveHistoryEntity.java # 아카이브 히스토리 엔티티
│   │   ├── auth/
│   │   │   ├── AdminEntity.java        # 관리자 엔티티
│   │   │   ├── AdminHistoryEntity.java  # 관리자 히스토리 엔티티
│   │   │   ├── EmailVerificationEntity.java # 이메일 인증 엔티티
│   │   │   ├── ProviderEntity.java     # OAuth 제공자 엔티티
│   │   │   ├── RefreshTokenEntity.java # Refresh Token 엔티티
│   │   │   ├── UserEntity.java         # 사용자 엔티티
│   │   │   └── UserHistoryEntity.java  # 사용자 히스토리 엔티티
│   │   └── chatbot/
│   │       ├── ConversationMessageEntity.java  # 대화 메시지 엔티티
│   │       └── ConversationSessionEntity.java  # 대화 세션 엔티티
│   ├── generator/
│   │   └── TsidGenerator.java         # TSID 생성기
│   ├── listener/
│   │   └── HistoryEntityListener.java  # 히스토리 자동 저장 리스너
│   ├── repository/
│   │   ├── reader/                     # 읽기 전용 Repository
│   │   │   ├── archive/
│   │   │   ├── auth/
│   │   │   └── chatbot/
│   │   └── writer/                     # 쓰기 전용 Repository
│   │       ├── archive/
│   │       ├── auth/
│   │       └── chatbot/
│   └── utils/
│       ├── JpaImplicitNamingStrategyCustom.java  # JPA 명명 전략
│       └── JpaPhysicalNamingStrategyCustom.java  # JPA 물리 명명 전략
└── src/main/resources/
    ├── application-api-domain.yml      # API 모듈용 설정
    ├── application-batch-domain.yml    # Batch 모듈용 설정
    └── db/migration/                   # Flyway 마이그레이션 스크립트
```

## API 모듈별 스키마 매핑

각 API 모듈은 독립적인 스키마를 사용합니다:

| API 모듈 | 스키마명 | 관리 테이블 |
|---------|---------|------------|
| `api-auth` | `auth` | providers, users, admins, refresh_tokens, email_verifications, user_history, admin_history |
| `api-archive` | `archive` | archives, archive_history |
| `api-chatbot` | `chatbot` | conversation_sessions, conversation_messages |
| `api-contest` | ❌ 미사용 | MongoDB Atlas 사용 |
| `api-news` | ❌ 미사용 | MongoDB Atlas 사용 |

### 스키마 설정 방법

각 API 모듈의 `application.yml` 파일에서 `module.aurora.schema` 속성을 설정합니다:

```yaml
module:
  aurora:
    schema: auth  # 또는 archive, chatbot
```

`domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 환경변수를 사용하여 동적으로 스키마를 참조합니다.

## 주요 엔티티

### BaseEntity

모든 엔티티의 기본 클래스로, 다음 필드를 제공합니다:

- `id` (Long): TSID Primary Key
- `isDeleted` (Boolean): 삭제 여부
- `deletedAt` (LocalDateTime): 삭제 일시
- `deletedBy` (Long): 삭제한 사용자 ID
- `createdAt` (LocalDateTime): 생성 일시
- `createdBy` (Long): 생성한 사용자 ID
- `updatedAt` (LocalDateTime): 수정 일시
- `updatedBy` (Long): 수정한 사용자 ID

### 주요 엔티티 목록

#### Auth 스키마
- **ProviderEntity**: OAuth 제공자 정보
- **UserEntity**: 사용자 정보
- **AdminEntity**: 관리자 정보
- **RefreshTokenEntity**: JWT Refresh Token
- **EmailVerificationEntity**: 이메일 인증 정보
- **UserHistoryEntity**: 사용자 변경 이력
- **AdminHistoryEntity**: 관리자 변경 이력

#### Archive 스키마
- **ArchiveEntity**: 사용자 아카이브 정보
- **ArchiveHistoryEntity**: 아카이브 변경 이력

#### Chatbot 스키마
- **ConversationSessionEntity**: 대화 세션 정보
- **ConversationMessageEntity**: 대화 메시지 히스토리

## Repository 구조

### Reader Repository (읽기 전용)

읽기 작업은 Reader Repository를 통해 수행됩니다. Aurora Reader Endpoint에 연결됩니다.

```java
@Repository
public interface UserReaderRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByUsername(String username);
}
```

### Writer Repository (쓰기 전용)

쓰기 작업은 Writer Repository를 통해 수행됩니다. Aurora Writer Endpoint에 연결됩니다.

```java
@Repository
public interface UserWriterRepository extends JpaRepository<UserEntity, Long> {
    UserEntity save(UserEntity user);
    void deleteById(Long id);
}
```

## 환경 설정

### 필수 환경변수

Aurora DB Cluster 연결을 위한 환경변수:

| 환경변수명 | 설명 | 예시 |
|-----------|------|------|
| `AURORA_WRITER_ENDPOINT` | Aurora Writer 엔드포인트 | `aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com` |
| `AURORA_READER_ENDPOINT` | Aurora Reader 엔드포인트 | `aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com` |
| `AURORA_USERNAME` | 데이터베이스 사용자명 | `admin` |
| `AURORA_PASSWORD` | 데이터베이스 비밀번호 | `********` |
| `AURORA_OPTIONS` | JDBC 연결 옵션 | `useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |

### 로컬 환경 설정

로컬 환경에서는 `.env` 파일을 사용하여 환경변수를 관리합니다:

```bash
# Aurora DB Cluster 연결 정보
AURORA_WRITER_ENDPOINT=aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_READER_ENDPOINT=aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_USERNAME=admin
AURORA_PASSWORD=your-password-here
AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8

# 기타 설정
DB_FETCH_CHUNKSIZE=250
DB_BATCH_SIZE=50
TZ=Asia/Seoul
```

## 의존성

### 주요 의존성

- **Spring Boot Data JPA**: JPA 및 Repository 지원
- **MariaDB JDBC Driver**: Aurora MySQL 연결 드라이버
- **Flyway**: 데이터베이스 마이그레이션
- **MyBatis Spring Boot Starter**: MyBatis 통합
- **TSID Creator**: TSID 생성 라이브러리

### build.gradle

```gradle
dependencies {
    implementation project(':common-core')
    
    implementation 'com.github.f4b6a3:tsid-creator:5.2.6'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
    implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'
    implementation 'org.springframework.boot:spring-boot-starter-flyway'
    implementation 'org.springframework.boot:spring-boot-starter-mongodb'
    implementation 'org.flywaydb:flyway-mysql'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1'
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client'
}
```

## 히스토리 추적 메커니즘

### HistoryEntityListener

`HistoryEntityListener`는 JPA Entity Listener로, 모든 엔티티의 변경 사항을 자동으로 히스토리 테이블에 저장합니다.

### 지원하는 작업 타입

- **INSERT**: 새로운 레코드 생성
- **UPDATE**: 기존 레코드 수정
- **DELETE**: Soft Delete 처리 (`is_deleted = TRUE`로 변경)

### 히스토리 테이블 구조

모든 히스토리 테이블은 다음 구조를 가집니다:

- `history_id` (Long): TSID Primary Key
- `{entity}_id` (Long): 엔티티 ID (Foreign Key)
- `operation_type` (String): 작업 타입 (INSERT, UPDATE, DELETE)
- `before_data` (JSON): 변경 전 데이터
- `after_data` (JSON): 변경 후 데이터
- `changed_by` (Long): 변경한 사용자 ID
- `changed_at` (LocalDateTime): 변경 일시
- `change_reason` (String): 변경 사유

## CQRS 동기화

이 모듈은 Command Side로, 모든 쓰기 작업을 처리합니다. 쓰기 작업 후 Kafka 이벤트를 발행하여 Query Side(MongoDB Atlas)와 동기화합니다.

자세한 내용은 다음 문서를 참고하세요:
- [CQRS Kafka 동기화 설계서](../../docs/step11/cqrs-kafka-sync-design.md)

## 참고 문서

### 설계서
- [Aurora MySQL 스키마 설계서](../../docs/step1/3.%20aurora-schema-design.md)
- [Aurora MySQL 베스트 프랙티스](../../docs/step1/aurora-mysql-schema-design-best-practices.md)

### 공식 문서
- [Spring Data JPA 공식 문서](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
- [TSID Creator GitHub](https://github.com/f4b6a3/tsid-creator)
- [Flyway 공식 문서](https://flywaydb.org/documentation/)

## 라이선스

이 모듈은 프로젝트의 라이선스를 따릅니다.

