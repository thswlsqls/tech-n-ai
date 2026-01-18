# CQRS 패턴 기반 Kafka 동기화 설계서

**작성 일시**: 2026-01-XX  
**대상 모듈**: `common-kafka`, `domain-mongodb`  
**목적**: Kafka를 통한 Amazon Aurora MySQL과 MongoDB Atlas 간 실시간 동기화 구현 설계

## 목차

1. [개요](#개요)
2. [설계 원칙](#설계-원칙)
3. [현재 구현 상태 분석](#현재-구현-상태-분석)
4. [MongoDB Atlas 연결 설정 및 최적화](#mongodb-atlas-연결-설정-및-최적화)
5. [상세 설계](#상세-설계)
   - [아키텍처 설계](#아키텍처-설계)
   - [동기화 서비스 설계](#동기화-서비스-설계)
   - [이벤트 처리 로직 설계](#이벤트-처리-로직-설계)
6. [구현 가이드](#구현-가이드)
7. [검증 기준](#검증-기준)

---

## 개요

이 설계서는 CQRS 패턴의 Command Side (Amazon Aurora MySQL)와 Query Side (MongoDB Atlas) 간의 실시간 동기화를 위한 Kafka 기반 이벤트 동기화 시스템을 설계합니다.

### 배경

CQRS 패턴을 적용하여 읽기와 쓰기 작업을 분리함으로써 성능 최적화 및 확장성을 향상시킵니다:
- **Command Side (Aurora MySQL)**: 모든 쓰기 작업 처리
- **Query Side (MongoDB Atlas)**: 읽기 최적화된 데이터 조회
- **동기화**: Kafka 이벤트를 통한 비동기 실시간 동기화

### 현재 구현 상태 요약

- ✅ **Kafka Producer (`EventPublisher`)**: 완료
- ✅ **Kafka Consumer 기본 구조 (`EventConsumer`)**: 완료 (멱등성 보장 포함)
- ✅ **이벤트 모델**: 모든 이벤트 타입 정의 완료
- ✅ **MongoDB Document 및 Repository**: 완료
- ✅ **MongoDB 인덱스 설정 (`MongoIndexConfig`)**: 완료
- ✅ **MongoDB Atlas 연결 설정**: 완료
  - ✅ 연결 설정 파일 (`application-mongodb-domain.yml`)
  - ✅ 연결 풀 최적화 Config (`MongoClientConfig`)
  - ✅ Read Preference 설정 (`secondaryPreferred`)
  - ✅ API 모듈 Profile 설정 업데이트
- ❌ **동기화 서비스**: 미구현 (`UserSyncService`, `ArchiveSyncService`)
- ❌ **이벤트 처리 로직**: `EventConsumer.processEvent` 빈 구현

### 설계서 범위

**포함 사항**:
- User 및 Archive 엔티티의 MongoDB 동기화 서비스 설계
- `EventConsumer.processEvent` 메서드 구현 설계
- 이벤트 타입별 처리 로직 설계
- `updatedFields` (Map<String, Object>) 처리 전략

**제외 사항**:
- Kafka 토픽 생성 (인프라 작업, 코드에 포함 불필요)
- 복잡한 DLQ 처리 (기본 재시도로 충분, 필요 시 후속 단계에서 추가)
- 복잡한 모니터링 시스템 (기본 로깅으로 충분)
- Contest/News 동기화 서비스 (배치 작업을 통해 직접 MongoDB에 저장되므로 Kafka 동기화 불필요)
  - `ContestSyncedEvent`, `NewsArticleSyncedEvent`는 배치 작업에서 직접 MongoDB에 저장
  - 이벤트는 로깅 목적으로만 사용될 수 있음

---

## 설계 원칙

### 1. 클린코드 원칙

1. **단일 책임 원칙 (SRP)**
   - 각 동기화 서비스는 하나의 엔티티 타입만 담당
   - `UserSyncService`: User 엔티티 동기화만 담당
   - `ArchiveSyncService`: Archive 엔티티 동기화만 담당

2. **의존성 역전 원칙 (DIP)**
   - 인터페이스 기반 설계
   - 동기화 서비스는 인터페이스로 정의하고 구현체 분리
   - `EventConsumer`는 동기화 서비스 인터페이스에만 의존

3. **개방-폐쇄 원칙 (OCP)**
   - 새로운 이벤트 타입 추가 시 기존 코드 수정 없이 확장 가능
   - 전략 패턴을 통한 이벤트 처리 로직 확장

### 2. 객체지향 설계 기법

1. **전략 패턴**
   - 이벤트 타입별 처리 전략 분리
   - `EventConsumer`에서 이벤트 타입에 따라 적절한 동기화 서비스 선택

2. **팩토리 패턴 (선택사항)**
   - 이벤트 타입 → 동기화 서비스 매핑을 팩토리로 캡슐화 가능
   - 현재는 단순 if-else 또는 switch로 구현 (오버엔지니어링 방지)

3. **템플릿 메서드 패턴**
   - 공통 동기화 흐름 정의 (이벤트 수신 → 멱등성 확인 → 동기화 → 완료 표시)
   - `EventConsumer`에서 이미 구현됨

### 3. CQRS 패턴 원칙

1. **Command Side와 Query Side 완전 분리**
   - Command Side: Aurora MySQL에만 쓰기
   - Query Side: MongoDB Atlas에서만 읽기
   - 동기화는 Kafka 이벤트를 통해서만 수행

2. **이벤트 기반 비동기 동기화**
   - 모든 쓰기 작업 후 Kafka 이벤트 발행
   - 비동기 처리로 Command Side 성능 영향 최소화

3. **TSID 필드 기반 1:1 매핑 보장**
   - `User.id(TSID)` → `UserProfileDocument.userTsid`
   - `Archive.id(TSID)` → `ArchiveDocument.archiveTsid`
   - UNIQUE 인덱스를 통한 정확성 보장

### 4. 최소 구현 원칙

1. **현재 필요한 기능만 구현**
   - User 및 Archive 동기화만 구현
   - Contest/News는 배치 작업으로 직접 저장되므로 제외

2. **단순하고 명확한 구조**
   - 복잡한 추상화 레이어 지양
   - 직접적이고 이해하기 쉬운 코드

3. **단계적 확장 가능한 구조**
   - 향후 다른 엔티티 동기화 추가 시 동일한 패턴 적용 가능

---

## 현재 구현 상태 분석

### 1. 이미 구현된 부분

#### 1.1 EventPublisher 구현 상태

**위치**: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/publisher/EventPublisher.java`

**구현 내용**:
- ✅ `publish(String topic, BaseEvent event, String partitionKey)`: Partition Key 지정 이벤트 발행
- ✅ `publish(String topic, BaseEvent event)`: 기본 Partition Key 사용 이벤트 발행
- ✅ 비동기 처리: `CompletableFuture` 사용
- ✅ 에러 핸들링: 예외 발생 시 로깅 및 `RuntimeException` 전파
- ✅ 로깅: 성공/실패 로그 출력

**특징**:
- Partition Key를 통한 이벤트 순서 보장 지원
- 비동기 처리로 Command Side 성능 영향 최소화

#### 1.2 EventConsumer 기본 구조

**위치**: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`

**구현 내용**:
- ✅ `@KafkaListener` 설정: 여러 토픽 수신 (`user-events`, `archive-events`, `conversation-events`, `contest-events`, `news-events`)
- ✅ 멱등성 보장: Redis 기반 중복 처리 방지
  - `isEventProcessed(String eventId)`: Redis에서 처리 여부 확인
  - `markEventAsProcessed(String eventId)`: Redis에 처리 완료 표시 (TTL: 7일)
- ✅ 수동 커밋: `Acknowledgment.acknowledge()` 사용
- ✅ 에러 핸들링: 예외 발생 시 로깅 및 예외 전파 (Spring Kafka 재시도 활용)
- ❌ `processEvent(BaseEvent event)`: 빈 구현 (로깅만 수행)

**특징**:
- 멱등성 보장 로직이 이미 구현되어 있어 중복 처리 방지
- `processEvent` 메서드만 구현하면 됨

#### 1.3 이벤트 모델 구조

**BaseEvent 인터페이스**:
```java
public interface BaseEvent {
    String eventId();      // UUID 형식
    String eventType();    // 이벤트 타입 문자열
    Instant timestamp();   // 이벤트 발생 시각
}
```

**User 관련 이벤트**:
- ✅ `UserCreatedEvent`: `userTsid`, `userId`, `username`, `email`, `profileImageUrl`
- ✅ `UserUpdatedEvent`: `userTsid`, `userId`, `updatedFields` (Map<String, Object>)
- ✅ `UserDeletedEvent`: `userTsid`, `userId`, `deletedAt`
- ✅ `UserRestoredEvent`: `userTsid`, `userId`, `username`, `email`, `profileImageUrl`

**Archive 관련 이벤트**:
- ✅ `ArchiveCreatedEvent`: `archiveTsid`, `userId`, `itemType`, `itemId`, `itemTitle`, `itemSummary`, `tag`, `memo`, `archivedAt`
- ✅ `ArchiveUpdatedEvent`: `archiveTsid`, `userId`, `updatedFields` (Map<String, Object>)
  - **주의**: `updatedFields`에는 `tag`, `memo`만 포함 가능 (ArchiveEntity에 있는 필드만)
  - `itemTitle`, `itemSummary`는 ArchiveEntity에 없는 필드이므로 `updatedFields`에 포함될 수 없음
- ✅ `ArchiveDeletedEvent`: `archiveTsid`, `userId`, `deletedAt`
- ✅ `ArchiveRestoredEvent`: `archiveTsid`, `userId`, `itemType`, `itemId`, `itemTitle`, `itemSummary`, `tag`, `memo`, `archivedAt`

**Conversation 관련 이벤트**:
- ✅ `ConversationSessionCreatedEvent`: `sessionId`, `userId`, `title`, `lastMessageAt`, `isActive`, `createdAt`
- ✅ `ConversationSessionUpdatedEvent`: `sessionId`, `userId`, `updatedFields` (Map<String, Object>)
  - **주의**: `updatedFields`에는 `title`, `lastMessageAt`, `isActive`만 포함 가능 (ConversationSessionEntity에 있는 필드만)
- ✅ `ConversationSessionDeletedEvent`: `sessionId`, `userId`, `deletedAt`
- ✅ `ConversationMessageCreatedEvent`: `messageId`, `sessionId`, `role`, `content`, `tokenCount`, `sequenceNumber`, `createdAt`

**특징**:
- 모든 이벤트는 `record` 타입으로 정의 (불변성 보장)
- `updatedFields`는 `Map<String, Object>` 타입으로 부분 업데이트 지원

#### 1.4 MongoDB Repository 인터페이스

**UserProfileRepository**:
```java
public interface UserProfileRepository extends MongoRepository<UserProfileDocument, ObjectId> {
    Optional<UserProfileDocument> findByUserTsid(String userTsid);
    Optional<UserProfileDocument> findByUserId(String userId);
    Optional<UserProfileDocument> findByUsername(String username);
    Optional<UserProfileDocument> findByEmail(String email);
}
```

**ArchiveRepository**:
```java
public interface ArchiveRepository extends MongoRepository<ArchiveDocument, ObjectId> {
    Optional<ArchiveDocument> findByArchiveTsid(String archiveTsid);
    List<ArchiveDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    List<ArchiveDocument> findByUserIdAndItemTypeOrderByCreatedAtDesc(String userId, String itemType);
    Optional<ArchiveDocument> findByUserIdAndItemTypeAndItemId(String userId, String itemType, ObjectId itemId);
}
```

**특징**:
- Spring Data MongoDB의 `MongoRepository` 활용
- `userTsid`, `archiveTsid` 기반 조회 메서드 제공
- UNIQUE 인덱스가 설정되어 있어 중복 방지

### 2. 미구현 부분

#### 2.1 동기화 서비스

**미구현 서비스**:
- ❌ `UserSyncService`: User 이벤트 → UserProfileDocument 동기화
- ❌ `ArchiveSyncService`: Archive 이벤트 → ArchiveDocument 동기화
- ❌ `ConversationSyncService`: Conversation 이벤트 → ConversationSessionDocument, ConversationMessageDocument 동기화

**주의사항**:
- `ContestSyncService`, `NewsArticleSyncService`는 **제외**
  - 배치 작업을 통해 직접 MongoDB에 저장되므로 Kafka 동기화 불필요
  - `ContestSyncedEvent`, `NewsArticleSyncedEvent`는 로깅 목적으로만 사용 가능

#### 2.2 EventConsumer.processEvent 구현

**현재 상태**:
```java
private void processEvent(BaseEvent event) {
    // 실제 이벤트 처리 로직은 각 도메인별로 구현
    // 여기서는 기본 구현만 제공
    log.info("Processing event: eventId={}, eventType={}", event.eventId(), event.eventType());
}
```

**필요한 구현**:
- 이벤트 타입별 분기 처리
- 적절한 동기화 서비스 호출
- 에러 핸들링

#### 2.3 updatedFields 처리 로직

**문제점**:
- `UserUpdatedEvent`, `ArchiveUpdatedEvent`의 `updatedFields`가 `Map<String, Object>` 타입
- MongoDB Document 필드 타입과의 변환 필요
- 부분 업데이트 vs 전체 교체 전략 결정 필요

**필요한 처리**:
- `Map<String, Object>` → MongoDB Document 필드 매핑
- 필드 타입 변환 (String → LocalDateTime 등)
- null 값 처리 전략

### 3. 구현 우선순위

#### 필수 구현 항목 (Phase 1)
1. ✅ `UserSyncService` 인터페이스 및 구현 클래스
2. ✅ `ArchiveSyncService` 인터페이스 및 구현 클래스
3. ✅ `ConversationSyncService` 인터페이스 및 구현 클래스
4. ✅ `EventConsumer.processEvent` 메서드 구현
5. ✅ `updatedFields` 처리 로직

#### 선택 구현 항목 (후속 단계)
- 이벤트 처리 성능 모니터링
- 동기화 실패 알림 시스템
- 복잡한 DLQ 처리

---

## MongoDB Atlas 연결 설정 및 최적화

### 현재 상태

**구현 완료**:
- ✅ MongoDB Atlas 연결 설정 파일 (`application-mongodb-domain.yml`)
- ✅ 연결 풀 최적화 Config 클래스 (`MongoClientConfig`)
- ✅ Read Preference 설정 (`secondaryPreferred`)
- ✅ SSL/TLS 설정 (URI 기반)
- ✅ 타임아웃 설정 (connectTimeout, readTimeout)
- ✅ MongoDB 인덱스 설정 (`MongoIndexConfig`)
- ✅ Document 및 Repository 구조

**구현 내용**:
1. **연결 설정 파일**: `domain/mongodb/src/main/resources/application-mongodb-domain.yml`
   - MongoDB Atlas Cluster 연결 문자열 설정
   - 환경변수 기반 관리 (`MONGODB_ATLAS_CONNECTION_STRING`)
   - Profile별 설정 분리 (local, dev, prod)

2. **연결 풀 최적화 Config**: `domain/mongodb/src/main/java/.../config/MongoClientConfig.java`
   - 연결 풀 최적화 (maxSize: 100, minSize: 10)
   - 타임아웃 설정 (connectTimeout: 10초, readTimeout: 30초)
   - Read Preference 설정 (`secondaryPreferred`)
   - Write Concern 설정 (`w: "majority"`)
   - Retry 설정 (retryWrites, retryReads)

### MongoDB Atlas 연결 설정 설계

#### 1. 연결 설정 파일 생성

**파일 위치**: `domain/mongodb/src/main/resources/application-mongodb-domain.yml`

**설정 내용**:

```yaml
spring:
  data:
    mongodb:
      # MongoDB Atlas Cluster 연결 문자열
      # 환경변수로 관리: MONGODB_ATLAS_CONNECTION_STRING
      uri: ${MONGODB_ATLAS_CONNECTION_STRING:mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/database?retryWrites=true&w=majority}
      
      # 데이터베이스 이름 (URI에 포함되어 있으면 생략 가능)
      database: ${MONGODB_ATLAS_DATABASE:shrimp_task_manager}
      
      # 연결 풀 최적화 설정
      # Spring Data MongoDB는 내부적으로 MongoClient를 사용하며 연결 풀을 자동 관리
      # MongoClientSettings를 통한 세부 설정은 별도 Config 클래스에서 수행
      
      # 타임아웃 설정 (밀리초)
      # connection-timeout: 연결 타임아웃 (기본값: 10000ms)
      # socket-timeout: 소켓 타임아웃 (기본값: 0ms, 무한 대기)
      # server-selection-timeout: 서버 선택 타임아웃 (기본값: 30000ms)
      
      # SSL/TLS 설정
      # URI에 ssl=true가 포함되어 있으면 자동 활성화
      # 프로덕션 환경에서는 필수
      
      # Read Preference 설정
      # URI에 readPreference=secondaryPreferred가 포함되어 있으면 자동 적용
      # 또는 MongoClientSettings에서 설정
```

**환경변수 관리**:
- `MONGODB_ATLAS_CONNECTION_STRING`: MongoDB Atlas Cluster 연결 문자열
  - 형식: `mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true`
  - 또는 Standard Connection String: `mongodb://{username}:{password}@{cluster-endpoint}:27017/{database}?ssl=true&replicaSet=...&readPreference=secondaryPreferred`
  - **보안**: 환경변수로 관리 (코드에 하드코딩 금지)
- `MONGODB_ATLAS_DATABASE`: 데이터베이스 이름 (선택사항, URI에 포함 가능, 기본값: `shrimp_task_manager`)

**API 모듈 Profile 설정**:
- `api-news`, `api-contest`, `api-gateway` 모듈의 `application-*-api.yml`에 `mongodb-domain` profile 추가 필요
- 예시:
  ```yaml
  spring:
    profiles:
      include:
        - common-core
        - mongodb-domain  # 추가
  ```

#### 2. MongoClientSettings 최적화 Config 클래스

**파일 위치**: `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/MongoClientConfig.java`

**설정 내용**:

```java
package com.tech.n.ai.domain.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.connection.ConnectionPoolSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB Atlas 연결 설정 및 최적화
 * 
 * 참고:
 * - Spring Data MongoDB 공식 문서: https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
 * - MongoDB Java Driver 공식 문서: https://www.mongodb.com/docs/drivers/java/sync/current/
 */
@Slf4j
@Configuration
public class MongoClientConfig extends AbstractMongoClientConfiguration {
    
    @Value("${spring.data.mongodb.uri}")
    private String connectionString;
    
    @Value("${spring.data.mongodb.database:shrimp_task_manager}")
    private String databaseName;
    
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }
    
    @Override
    protected void configureClientSettings(MongoClientSettings.Builder builder) {
        ConnectionString connString = new ConnectionString(connectionString);
        
        // 연결 풀 최적화 설정
        ConnectionPoolSettings.Builder poolBuilder = ConnectionPoolSettings.builder()
            .maxSize(100)                    // 최대 연결 수 (기본값: 100)
            .minSize(10)                     // 최소 연결 수 (기본값: 0)
            .maxWaitTime(120000, TimeUnit.MILLISECONDS)  // 연결 대기 시간 (기본값: 120초)
            .maxConnectionLifeTime(0, TimeUnit.MILLISECONDS)  // 연결 최대 수명 (0: 무제한)
            .maxConnectionIdleTime(60000, TimeUnit.MILLISECONDS)  // 유휴 연결 타임아웃 (60초)
            .maxConnecting(2);               // 동시 연결 생성 수 (기본값: 2)
        
        // 타임아웃 설정
        builder.applyConnectionString(connString)
            .applyToSocketSettings(settings -> settings
                .connectTimeout(10000, TimeUnit.MILLISECONDS)  // 연결 타임아웃 (10초)
                .readTimeout(30000, TimeUnit.MILLISECONDS)     // 읽기 타임아웃 (30초)
            )
            .applyToServerSettings(settings -> settings
                .heartbeatFrequency(10000, TimeUnit.MILLISECONDS)  // 하트비트 주기 (10초)
                .minHeartbeatFrequency(500, TimeUnit.MILLISECONDS)  // 최소 하트비트 주기 (0.5초)
            )
            .applyToConnectionPoolSettings(settings -> poolBuilder.build())
            
            // Read Preference 설정 (읽기 복제본 우선)
            // URI에 readPreference가 포함되어 있으면 자동 적용되지만, 명시적으로 설정 가능
            .readPreference(ReadPreference.secondaryPreferred())
            
            // Write Concern 설정
            .writeConcern(WriteConcern.MAJORITY.withWTimeout(5000, TimeUnit.MILLISECONDS))
            
            // Retry 설정
            .retryWrites(true)
            .retryReads(true);
        
        log.info("MongoDB Atlas connection configured: database={}, readPreference={}", 
            databaseName, ReadPreference.secondaryPreferred());
    }
}
```

**설정 근거** (MongoDB Java Driver 공식 문서 및 Atlas 베스트 프랙티스):

1. **연결 풀 최적화**:
   - `maxSize: 100`: MongoDB Atlas 클러스터 티어에 따라 조정 (M10: 100, M20: 200 등)
   - `minSize: 10`: 최소 연결 수를 유지하여 연결 생성 오버헤드 감소
   - `maxConnectionIdleTime: 60초`: 유휴 연결을 적절히 정리하여 리소스 관리

2. **타임아웃 설정**:
   - `connectTimeout: 10초`: 연결 타임아웃 (네트워크 지연 고려)
   - `readTimeout: 30초`: 읽기 타임아웃 (복잡한 쿼리 고려)
   - `maxWaitTime: 120초`: 연결 풀 대기 시간

3. **Read Preference**:
   - `secondaryPreferred`: 읽기 복제본 우선 사용 (기본 노드 부하 감소)
   - CQRS 패턴의 Query Side 특성상 최종 일관성 허용 가능

4. **Write Concern**:
   - `w: "majority"`: 대다수 노드에서 확인 (데이터 일관성 보장)
   - `wtimeout: 5초`: Write Concern 타임아웃

#### 3. 연결 설정 다이어그램

```mermaid
flowchart TB
    subgraph App["Spring Boot Application"]
        MC[MongoClient<br/>Connection Pool]
        MT[MongoTemplate]
        Repo[Repository]
    end
    
    subgraph Atlas["MongoDB Atlas Cluster"]
        Primary[Primary Node]
        Secondary1[Secondary Node 1]
        Secondary2[Secondary Node 2]
    end
    
    MC -->|Connection Pool<br/>maxSize: 100<br/>minSize: 10| Primary
    MC -->|Read Preference<br/>secondaryPreferred| Secondary1
    MC -->|Read Preference<br/>secondaryPreferred| Secondary2
    
    MT --> MC
    Repo --> MT
    
    style MC fill:#87CEEB
    style Primary fill:#90EE90
    style Secondary1 fill:#FFE4B5
    style Secondary2 fill:#FFE4B5
```

### 최적화 전략

#### 1. 연결 풀 최적화

**권장 설정** (프로덕션 환경):
- `maxSize`: 클러스터 티어에 따라 조정
  - M10: 100
  - M20: 200
  - M30: 300
- `minSize`: `maxSize`의 10% (최소 10)
- `maxConnectionIdleTime`: 60초 (유휴 연결 정리)

**주의사항**:
- 연결 풀 크기는 클러스터 리소스에 맞게 조정
- 너무 큰 연결 풀은 메모리 사용량 증가
- 너무 작은 연결 풀은 성능 저하

#### 2. Read Preference 최적화

**CQRS 패턴에서의 활용**:
- Query Side는 읽기 전용이므로 `secondaryPreferred` 사용
- 기본 노드 부하 감소 및 읽기 성능 향상
- 최종 일관성 허용 가능 (동기화 지연 1초 이내)

**Read Preference 모드 비교**:

| 모드 | 설명 | CQRS 적용 |
|------|------|-----------|
| `primary` | 기본 노드만 읽기 | ❌ (부하 집중) |
| `primaryPreferred` | 기본 노드 우선 | ⚠️ (기본 노드 부하) |
| `secondary` | 보조 노드만 읽기 | ✅ (부하 분산) |
| `secondaryPreferred` | 보조 노드 우선 | ✅ **권장** |
| `nearest` | 지연 시간 최소 노드 | ⚠️ (일관성 저하 가능) |

#### 3. Write Concern 최적화

**동기화 서비스에서의 활용**:
- `w: "majority"`: 데이터 일관성 보장
- `wtimeout: 5초`: 타임아웃 설정으로 무한 대기 방지
- 동기화 실패 시 재시도 메커니즘 활용

#### 4. 인덱스 최적화

**현재 구현 상태**: ✅ `MongoIndexConfig`에서 인덱스 자동 생성

**최적화 확인 사항**:
- ESR 규칙 준수 (Equality → Sort → Range)
- UNIQUE 인덱스 설정 (`userTsid`, `archiveTsid`)
- TTL 인덱스 설정 (`news_articles.publishedAt`, `exception_logs.occurredAt`)

#### 5. 프로젝션 최적화

**권장 사항**:
- 필요한 필드만 선택하여 네트워크 트래픽 최소화
- Repository 메서드에서 프로젝션 활용

**예시**:
```java
// 필요한 필드만 선택
@Query(value = "{userId: ?0}", fields = "{_id: 1, itemTitle: 1, archivedAt: 1}")
List<ArchiveDocument> findArchivesByUserId(String userId);
```

### 설정 파일 구조

**공통 설정**: `domain/mongodb/src/main/resources/application-mongodb-domain.yml`
- 모든 API 모듈에서 공통으로 사용
- Profile별 설정 분리 가능

**Profile별 설정** (선택사항):
- `application-mongodb-domain-local.yml`: 로컬 개발 환경
- `application-mongodb-domain-dev.yml`: 개발 환경
- `application-mongodb-domain-prod.yml`: 프로덕션 환경

### 환경변수 관리

**필수 환경변수**:
- `MONGODB_ATLAS_CONNECTION_STRING`: MongoDB Atlas Cluster 연결 문자열
  - 형식: `mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true`
  - 보안: 환경변수로 관리 (코드에 하드코딩 금지)

**선택 환경변수**:
- `MONGODB_ATLAS_DATABASE`: 데이터베이스 이름 (기본값: `shrimp_task_manager`)

**환경별 관리**:
- 로컬: `.env` 파일 사용 (`.gitignore`에 포함)
- 프로덕션: AWS Secrets Manager, Parameter Store 등 활용

### 검증 기준

**연결 설정 검증**:
- ✅ MongoDB Atlas Cluster 연결 성공
- ✅ 연결 풀 정상 동작
- ✅ Read Preference 적용 확인
- ✅ SSL/TLS 연결 확인 (프로덕션 환경)

**성능 검증**:
- ✅ 연결 풀 모니터링 (연결 수, 유휴 연결 수)
- ✅ 쿼리 성능 측정
- ✅ 읽기 복제본 활용 확인

---

## 상세 설계

### 아키텍처 설계

#### 전체 아키텍처 다이어그램

```mermaid
flowchart TB
    subgraph CommandSide["Command Side (Aurora MySQL)"]
        UserEntity[User Entity]
        ArchiveEntity[Archive Entity]
        ConversationSessionEntity[ConversationSession Entity]
        ConversationMessageEntity[ConversationMessage Entity]
        EventPublisher[EventPublisher<br/>✅ 완료]
    end
    
    subgraph Kafka["Kafka"]
        UserTopic[user-events Topic]
        ArchiveTopic[archive-events Topic]
        ConversationTopic[conversation-events Topic]
        EventConsumer[EventConsumer<br/>✅ 기본 구조<br/>❌ processEvent 미구현]
    end
    
    subgraph QuerySide["Query Side (MongoDB Atlas)"]
        UserProfileDoc[UserProfileDocument]
        ArchiveDoc[ArchiveDocument]
        ConversationSessionDoc[ConversationSessionDocument]
        ConversationMessageDoc[ConversationMessageDocument]
        UserSyncService[UserSyncService<br/>❌ 미구현]
        ArchiveSyncService[ArchiveSyncService<br/>❌ 미구현]
        ConversationSyncService[ConversationSyncService<br/>❌ 미구현]
    end
    
    UserEntity -->|이벤트 발행| EventPublisher
    ArchiveEntity -->|이벤트 발행| EventPublisher
    ConversationSessionEntity -->|이벤트 발행| EventPublisher
    ConversationMessageEntity -->|이벤트 발행| EventPublisher
    EventPublisher -->|publish| UserTopic
    EventPublisher -->|publish| ArchiveTopic
    EventPublisher -->|publish| ConversationTopic
    UserTopic -->|이벤트 수신| EventConsumer
    ArchiveTopic -->|이벤트 수신| EventConsumer
    ConversationTopic[conversation-events Topic] -->|이벤트 수신| EventConsumer
    EventConsumer -->|syncUserCreated<br/>syncUserUpdated<br/>syncUserDeleted<br/>syncUserRestored| UserSyncService
    EventConsumer -->|syncArchiveCreated<br/>syncArchiveUpdated<br/>syncArchiveDeleted<br/>syncArchiveRestored| ArchiveSyncService
    EventConsumer -->|syncSessionCreated<br/>syncSessionUpdated<br/>syncSessionDeleted<br/>syncMessageCreated| ConversationSyncService
    UserSyncService -->|save/delete| UserProfileDoc
    ArchiveSyncService -->|save/delete| ArchiveDoc
    ConversationSyncService -->|save/delete| ConversationSessionDoc
    ConversationSyncService -->|save/delete| ConversationMessageDoc
    
    style EventPublisher fill:#90EE90
    style EventConsumer fill:#FFE4B5
    style UserSyncService fill:#FFB6C1
    style ArchiveSyncService fill:#FFB6C1
    style ConversationSyncService fill:#FFB6C1
    style UserProfileDoc fill:#87CEEB
    style ArchiveDoc fill:#87CEEB
    style ConversationSessionDoc fill:#87CEEB
    style ConversationMessageDoc fill:#87CEEB
```

#### 동기화 흐름

**동기화 시퀀스 다이어그램**:

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client
    participant API as API Service
    participant DomainAurora as Domain Aurora<br/>(Command Side)
    participant AuroraMySQL as Aurora MySQL<br/>(Writer)
    participant EP as Event Publisher
    participant Kafka as Kafka Broker
    participant EC as Event Consumer
    participant Redis as Redis<br/>(Idempotency)
    participant DomainMongo as Domain MongoDB<br/>(Query Side)
    participant MongoDB as MongoDB Atlas<br/>(Query Side)
    
    Note over Client,MongoDB: Write Operation Flow (CQRS Command Side)
    Client->>API: POST /api/v1/archives<br/>(Create Archive)
    API->>DomainAurora: ArchiveService.create()
    DomainAurora->>AuroraMySQL: INSERT INTO archives<br/>(TSID Primary Key)
    DomainAurora->>AuroraMySQL: INSERT INTO archive_history<br/>(operation_type: INSERT)
    DomainAurora->>EP: publish ArchiveCreatedEvent
    EP->>Kafka: topic: archive-events<br/>partitionKey: userId
    Kafka-->>EP: Acknowledgment
    EP-->>DomainAurora: Event Published
    DomainAurora-->>API: Archive Created
    API-->>Client: 201 Created<br/>(Archive Response)
    
    Note over Kafka,MongoDB: Event Consumption & Synchronization
    EC->>Kafka: consume ArchiveCreatedEvent
    EC->>Redis: Check Event Processed<br/>(eventId)
    alt Not Processed
        Redis-->>EC: false
        EC->>DomainMongo: Sync ArchiveDocument<br/>(archiveTsid: TSID)
        DomainMongo->>MongoDB: INSERT ArchiveDocument<br/>(archiveTsid, userId, itemType, itemId)
        MongoDB-->>DomainMongo: Document Created
        DomainMongo-->>EC: Complete
        EC->>Redis: Mark Event Processed<br/>(TTL: 7 days)
        Redis-->>EC: Acknowledged
    else Already Processed
        Redis-->>EC: true
        Note over EC: Skip processing
    end
    EC->>Kafka: Acknowledge Offset
    
    Note over Client,MongoDB: Read Operation Flow (CQRS Query Side)
    Client->>API: GET /api/v1/archives<br/>(List Archives)
    API->>DomainMongo: ArchiveRepository.findByUserId()
    DomainMongo->>MongoDB: Query ArchiveDocument<br/>(userId index)
    MongoDB-->>DomainMongo: ArchiveDocument List
    DomainMongo-->>API: Archive List
    API-->>Client: 200 OK<br/>(Archive List)
    
    Note over Client,MongoDB: Synchronization Delay Target: < 1 second
```

**동기화 흐름 단계**:

1. **Command Side (Aurora MySQL)**
   - User 또는 Archive 엔티티 생성/수정/삭제/복원
   - `EventPublisher.publish()` 호출하여 Kafka 이벤트 발행
   - Partition Key 사용 (userId, archiveTsid 등)으로 이벤트 순서 보장

2. **Kafka**
   - 이벤트를 토픽에 저장
   - Consumer 그룹을 통한 이벤트 수신

3. **EventConsumer**
   - `@KafkaListener`를 통한 이벤트 수신
   - 멱등성 확인 (Redis 기반)
   - `processEvent()` 호출하여 실제 동기화 수행
   - 처리 완료 표시 (Redis에 저장)
   - 수동 커밋

4. **동기화 서비스**
   - 이벤트 타입에 따라 적절한 동기화 서비스 선택
   - MongoDB Repository를 통한 Document 생성/수정/삭제

5. **Query Side (MongoDB Atlas)**
   - 동기화된 Document 저장
   - 읽기 최적화된 쿼리 수행

#### 데이터 매핑 다이어그램

```mermaid
erDiagram
    User ||--o{ UserProfileDocument : "1:1 매핑"
    Archive ||--o{ ArchiveDocument : "1:1 매핑"
    
    User {
        BIGINT id "TSID PK"
        VARCHAR userId
        VARCHAR username
        VARCHAR email
        VARCHAR profileImageUrl
    }
    
    UserProfileDocument {
        ObjectId _id "MongoDB PK"
        String userTsid "UNIQUE, TSID 매핑"
        String userId "UNIQUE"
        String username "UNIQUE"
        String email "UNIQUE"
        String profileImageUrl
    }
    
    Archive {
        BIGINT id "TSID PK"
        BIGINT userId "FK to User"
        VARCHAR itemType
        VARCHAR itemId
        VARCHAR itemTitle
        TEXT itemSummary
        VARCHAR tag
        TEXT memo
    }
    
    ArchiveDocument {
        ObjectId _id "MongoDB PK"
        String archiveTsid "UNIQUE, TSID 매핑"
        String userId
        String itemType
        ObjectId itemId
        String itemTitle
        String itemSummary
        String tag
        String memo
    }
```

**매핑 규칙**:
- `User.id` (TSID) → `UserProfileDocument.userTsid` (1:1 매핑, UNIQUE 인덱스)
- `Archive.id` (TSID) → `ArchiveDocument.archiveTsid` (1:1 매핑, UNIQUE 인덱스)
- `ConversationSession.session_id` (TSID) → `ConversationSessionDocument.session_id` (1:1 매핑, UNIQUE 인덱스)
- `ConversationMessage.message_id` (TSID) → `ConversationMessageDocument.message_id` (1:1 매핑, UNIQUE 인덱스)
- TSID 필드를 통한 정확한 동기화 보장

### 동기화 서비스 설계

#### 클래스 다이어그램

```mermaid
classDiagram
    class BaseEvent {
        <<interface>>
        +String eventId()
        +String eventType()
        +Instant timestamp()
    }
    
    class UserCreatedEvent {
        +UserCreatedPayload payload
    }
    
    class UserUpdatedEvent {
        +UserUpdatedPayload payload
        +Map~String,Object~ updatedFields
    }
    
    class UserDeletedEvent {
        +UserDeletedPayload payload
    }
    
    class UserRestoredEvent {
        +UserRestoredPayload payload
    }
    
    class ArchiveCreatedEvent {
        +ArchiveCreatedPayload payload
    }
    
    class ArchiveUpdatedEvent {
        +ArchiveUpdatedPayload payload
        +Map~String,Object~ updatedFields
    }
    
    class ArchiveDeletedEvent {
        +ArchiveDeletedPayload payload
    }
    
    class ArchiveRestoredEvent {
        +ArchiveRestoredPayload payload
    }
    
    class ConversationSessionCreatedEvent {
        +ConversationSessionCreatedPayload payload
    }
    
    class ConversationSessionUpdatedEvent {
        +ConversationSessionUpdatedPayload payload
        +Map~String,Object~ updatedFields
    }
    
    class ConversationSessionDeletedEvent {
        +ConversationSessionDeletedPayload payload
    }
    
    class ConversationMessageCreatedEvent {
        +ConversationMessageCreatedPayload payload
    }
    
    class EventConsumer {
        -RedisTemplate redisTemplate
        -UserSyncService userSyncService
        -ArchiveSyncService archiveSyncService
        -ConversationSyncService conversationSyncService
        +consume(BaseEvent, Acknowledgment)
        -processEvent(BaseEvent)
        -isEventProcessed(String) boolean
        -markEventAsProcessed(String)
    }
    
    class UserSyncService {
        <<interface>>
        +syncUserCreated(UserCreatedEvent)
        +syncUserUpdated(UserUpdatedEvent)
        +syncUserDeleted(UserDeletedEvent)
        +syncUserRestored(UserRestoredEvent)
    }
    
    class UserSyncServiceImpl {
        -UserProfileRepository repository
        +syncUserCreated(UserCreatedEvent)
        +syncUserUpdated(UserUpdatedEvent)
        +syncUserDeleted(UserDeletedEvent)
        +syncUserRestored(UserRestoredEvent)
        -updateDocumentFields(Document, Map)
    }
    
    class ArchiveSyncService {
        <<interface>>
        +syncArchiveCreated(ArchiveCreatedEvent)
        +syncArchiveUpdated(ArchiveUpdatedEvent)
        +syncArchiveDeleted(ArchiveDeletedEvent)
        +syncArchiveRestored(ArchiveRestoredEvent)
    }
    
    class ArchiveSyncServiceImpl {
        -ArchiveRepository repository
        +syncArchiveCreated(ArchiveCreatedEvent)
        +syncArchiveUpdated(ArchiveUpdatedEvent)
        +syncArchiveDeleted(ArchiveDeletedEvent)
        +syncArchiveRestored(ArchiveRestoredEvent)
        -updateDocumentFields(Document, Map)
        -convertToLocalDateTime(Instant) LocalDateTime
    }
    
    class ConversationSyncService {
        <<interface>>
        +syncSessionCreated(ConversationSessionCreatedEvent)
        +syncSessionUpdated(ConversationSessionUpdatedEvent)
        +syncSessionDeleted(ConversationSessionDeletedEvent)
        +syncMessageCreated(ConversationMessageCreatedEvent)
    }
    
    class ConversationSyncServiceImpl {
        -ConversationSessionDocumentRepository sessionRepository
        -ConversationMessageDocumentRepository messageRepository
        +syncSessionCreated(ConversationSessionCreatedEvent)
        +syncSessionUpdated(ConversationSessionUpdatedEvent)
        +syncSessionDeleted(ConversationSessionDeletedEvent)
        +syncMessageCreated(ConversationMessageCreatedEvent)
        -updateDocumentFields(Document, Map)
        -convertToLocalDateTime(Instant) LocalDateTime
    }
    
    class UserProfileRepository {
        <<interface>>
        +findByUserTsid(String) Optional
        +save(UserProfileDocument)
        +deleteByUserTsid(String)
    }
    
    class ArchiveRepository {
        <<interface>>
        +findByArchiveTsid(String) Optional
        +save(ArchiveDocument)
        +deleteByArchiveTsid(String)
    }
    
    class ConversationSessionDocumentRepository {
        <<interface>>
        +findBySessionId(String) Optional
        +save(ConversationSessionDocument)
        +deleteBySessionId(String)
    }
    
    class ConversationMessageDocumentRepository {
        <<interface>>
        +findByMessageId(String) Optional
        +save(ConversationMessageDocument)
    }
    
    class UserProfileDocument {
        +String userTsid
        +String userId
        +String username
        +String email
        +String profileImageUrl
    }
    
    class ArchiveDocument {
        +String archiveTsid
        +String userId
        +String itemType
        +ObjectId itemId
        +String itemTitle
        +String itemSummary
        +String tag
        +String memo
    }
    
    class ConversationSessionDocument {
        +String session_id
        +String user_id
        +String title
        +Date last_message_at
        +Boolean is_active
    }
    
    class ConversationMessageDocument {
        +String message_id
        +String session_id
        +String role
        +String content
        +Integer token_count
        +Integer sequence_number
    }
    
    BaseEvent <|.. UserCreatedEvent
    BaseEvent <|.. UserUpdatedEvent
    BaseEvent <|.. UserDeletedEvent
    BaseEvent <|.. UserRestoredEvent
    BaseEvent <|.. ArchiveCreatedEvent
    BaseEvent <|.. ArchiveUpdatedEvent
    BaseEvent <|.. ArchiveDeletedEvent
    BaseEvent <|.. ArchiveRestoredEvent
    BaseEvent <|.. ConversationSessionCreatedEvent
    BaseEvent <|.. ConversationSessionUpdatedEvent
    BaseEvent <|.. ConversationSessionDeletedEvent
    BaseEvent <|.. ConversationMessageCreatedEvent
    
    EventConsumer --> UserSyncService : uses
    EventConsumer --> ArchiveSyncService : uses
    EventConsumer --> ConversationSyncService : uses
    EventConsumer --> BaseEvent : consumes
    
    UserSyncService <|.. UserSyncServiceImpl : implements
    ArchiveSyncService <|.. ArchiveSyncServiceImpl : implements
    ConversationSyncService <|.. ConversationSyncServiceImpl : implements
    
    UserSyncServiceImpl --> UserProfileRepository : uses
    ArchiveSyncServiceImpl --> ArchiveRepository : uses
    ConversationSyncServiceImpl --> ConversationSessionDocumentRepository : uses
    ConversationSyncServiceImpl --> ConversationMessageDocumentRepository : uses
    
    UserProfileRepository --> UserProfileDocument : manages
    ArchiveRepository --> ArchiveDocument : manages
    ConversationSessionDocumentRepository --> ConversationSessionDocument : manages
    ConversationMessageDocumentRepository --> ConversationMessageDocument : manages
```

#### UserSyncService

**인터페이스 정의**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;

/**
 * User 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 User 엔티티 변경을 MongoDB Atlas의 UserProfileDocument에 동기화합니다.
 */
public interface UserSyncService {
    
    /**
     * User 생성 이벤트 동기화
     * 
     * @param event UserCreatedEvent
     */
    void syncUserCreated(UserCreatedEvent event);
    
    /**
     * User 수정 이벤트 동기화
     * 
     * @param event UserUpdatedEvent
     */
    void syncUserUpdated(UserUpdatedEvent event);
    
    /**
     * User 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event UserDeletedEvent
     */
    void syncUserDeleted(UserDeletedEvent event);
    
    /**
     * User 복원 이벤트 동기화
     * MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
     * 
     * @param event UserRestoredEvent
     */
    void syncUserRestored(UserRestoredEvent event);
}
```

**구현 클래스 설계**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;
import com.tech.n.ai.domain.mongodb.document.UserProfileDocument;
import com.tech.n.ai.domain.mongodb.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * User 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncServiceImpl implements UserSyncService {
    
    private final UserProfileRepository userProfileRepository;
    private final MongoTemplate mongoTemplate;
    
    @Override
    public void syncUserCreated(UserCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: userTsid로 조회하여 없으면 생성, 있으면 업데이트
            UserProfileDocument document = userProfileRepository
                .findByUserTsid(payload.userTsid())
                .orElse(new UserProfileDocument());
            
            document.setUserTsid(payload.userTsid());
            document.setUserId(payload.userId());
            document.setUsername(payload.username());
            document.setEmail(payload.email());
            document.setProfileImageUrl(payload.profileImageUrl());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserCreatedEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserCreatedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserCreatedEvent", e);
        }
    }
    
    @Override
    public void syncUserUpdated(UserUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // userTsid로 Document 조회
            UserProfileDocument document = userProfileRepository
                .findByUserTsid(payload.userTsid())
                .orElseThrow(() -> new RuntimeException(
                    "UserProfileDocument not found: userTsid=" + payload.userTsid()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserUpdatedEvent: userTsid={}, updatedFields={}", 
                payload.userTsid(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync UserUpdatedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncUserDeleted(UserDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            userProfileRepository.deleteByUserTsid(payload.userTsid());
            
            log.debug("Successfully synced UserDeletedEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserDeletedEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserDeletedEvent", e);
        }
    }
    
    @Override
    public void syncUserRestored(UserRestoredEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
            UserProfileDocument document = new UserProfileDocument();
            document.setUserTsid(payload.userTsid());
            document.setUserId(payload.userId());
            document.setUsername(payload.username());
            document.setEmail(payload.email());
            document.setProfileImageUrl(payload.profileImageUrl());
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            userProfileRepository.save(document);
            
            log.debug("Successfully synced UserRestoredEvent: userTsid={}, userId={}", 
                payload.userTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync UserRestoredEvent: eventId={}, userTsid={}", 
                event.eventId(), event.payload().userTsid(), e);
            throw new RuntimeException("Failed to sync UserRestoredEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵
     */
    private void updateDocumentFields(UserProfileDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            switch (fieldName) {
                case "username":
                    document.setUsername((String) value);
                    break;
                case "email":
                    document.setEmail((String) value);
                    break;
                case "profileImageUrl":
                    document.setProfileImageUrl((String) value);
                    break;
                default:
                    log.warn("Unknown field in updatedFields: {}", fieldName);
            }
        }
    }
}
```

**특징**:
- Upsert 패턴 사용: `findByUserTsid().orElse(new UserProfileDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑
- MongoDB Soft Delete 미지원: 삭제 시 물리적 삭제, 복원 시 새로 생성
- 에러 핸들링: 예외 발생 시 로깅 및 `RuntimeException` 전파

#### ConversationSyncService

**인터페이스 정의**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;

/**
 * Conversation 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 ConversationSession 및 ConversationMessage 엔티티 변경을 
 * MongoDB Atlas의 ConversationSessionDocument 및 ConversationMessageDocument에 동기화합니다.
 */
public interface ConversationSyncService {
    
    /**
     * ConversationSession 생성 이벤트 동기화
     * 
     * @param event ConversationSessionCreatedEvent
     */
    void syncSessionCreated(ConversationSessionCreatedEvent event);
    
    /**
     * ConversationSession 수정 이벤트 동기화
     * 
     * @param event ConversationSessionUpdatedEvent
     */
    void syncSessionUpdated(ConversationSessionUpdatedEvent event);
    
    /**
     * ConversationSession 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event ConversationSessionDeletedEvent
     */
    void syncSessionDeleted(ConversationSessionDeletedEvent event);
    
    /**
     * ConversationMessage 생성 이벤트 동기화
     * 
     * @param event ConversationMessageCreatedEvent
     */
    void syncMessageCreated(ConversationMessageCreatedEvent event);
}
```

**구현 클래스 설계**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;
import com.tech.n.ai.domain.mongodb.document.ConversationSessionDocument;
import com.tech.n.ai.domain.mongodb.document.ConversationMessageDocument;
import com.tech.n.ai.domain.mongodb.repository.ConversationSessionDocumentRepository;
import com.tech.n.ai.domain.mongodb.repository.ConversationMessageDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Conversation 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationSyncServiceImpl implements ConversationSyncService {
    
    private final ConversationSessionDocumentRepository sessionRepository;
    private final ConversationMessageDocumentRepository messageRepository;
    
    @Override
    public void syncSessionCreated(ConversationSessionCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: session_id로 조회하여 없으면 생성, 있으면 업데이트
            ConversationSessionDocument document = sessionRepository
                .findBySessionId(payload.sessionId())
                .orElse(new ConversationSessionDocument());
            
            document.setSessionId(payload.sessionId());
            document.setUserId(payload.userId().toString());
            document.setTitle(payload.title());
            document.setLastMessageAt(convertToLocalDateTime(payload.lastMessageAt()));
            document.setIsActive(payload.isActive());
            document.setCreatedAt(convertToLocalDateTime(payload.createdAt()));
            document.setUpdatedAt(LocalDateTime.now());
            
            sessionRepository.save(document);
            
            log.debug("Successfully synced ConversationSessionCreatedEvent: sessionId={}, userId={}", 
                payload.sessionId(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionCreatedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionCreatedEvent", e);
        }
    }
    
    @Override
    public void syncSessionUpdated(ConversationSessionUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // session_id로 Document 조회
            ConversationSessionDocument document = sessionRepository
                .findBySessionId(payload.sessionId())
                .orElseThrow(() -> new RuntimeException(
                    "ConversationSessionDocument not found: sessionId=" + payload.sessionId()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            sessionRepository.save(document);
            
            log.debug("Successfully synced ConversationSessionUpdatedEvent: sessionId={}, updatedFields={}", 
                payload.sessionId(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionUpdatedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncSessionDeleted(ConversationSessionDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            sessionRepository.deleteBySessionId(payload.sessionId());
            
            log.debug("Successfully synced ConversationSessionDeletedEvent: sessionId={}, userId={}", 
                payload.sessionId(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationSessionDeletedEvent: eventId={}, sessionId={}", 
                event.eventId(), event.payload().sessionId(), e);
            throw new RuntimeException("Failed to sync ConversationSessionDeletedEvent", e);
        }
    }
    
    @Override
    public void syncMessageCreated(ConversationMessageCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: message_id로 조회하여 없으면 생성, 있으면 업데이트
            ConversationMessageDocument document = messageRepository
                .findByMessageId(payload.messageId())
                .orElse(new ConversationMessageDocument());
            
            document.setMessageId(payload.messageId());
            document.setSessionId(payload.sessionId());
            document.setRole(payload.role());
            document.setContent(payload.content());
            document.setTokenCount(payload.tokenCount());
            document.setSequenceNumber(payload.sequenceNumber());
            document.setCreatedAt(convertToLocalDateTime(payload.createdAt()));
            
            messageRepository.save(document);
            
            log.debug("Successfully synced ConversationMessageCreatedEvent: messageId={}, sessionId={}", 
                payload.messageId(), payload.sessionId());
        } catch (Exception e) {
            log.error("Failed to sync ConversationMessageCreatedEvent: eventId={}, messageId={}", 
                event.eventId(), event.payload().messageId(), e);
            throw new RuntimeException("Failed to sync ConversationMessageCreatedEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * 주의: updatedFields에는 ConversationSessionEntity에 있는 필드만 포함 가능
     * - 지원 필드: `title`, `lastMessageAt`, `isActive`
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵
     */
    private void updateDocumentFields(ConversationSessionDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            switch (fieldName) {
                case "title":
                    document.setTitle((String) value);
                    break;
                case "lastMessageAt":
                    if (value instanceof Instant instant) {
                        document.setLastMessageAt(convertToLocalDateTime(instant));
                    } else if (value instanceof LocalDateTime localDateTime) {
                        document.setLastMessageAt(localDateTime);
                    }
                    break;
                case "isActive":
                    document.setIsActive((Boolean) value);
                    break;
                default:
                    log.warn("Unknown field in updatedFields: {}", fieldName);
            }
        }
    }
    
    /**
     * Instant를 LocalDateTime으로 변환
     * 
     * @param instant Instant 객체
     * @return LocalDateTime 객체
     */
    private LocalDateTime convertToLocalDateTime(Instant instant) {
        return instant != null 
            ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            : null;
    }
}
```

**특징**:
- Upsert 패턴 사용: `findBySessionId().orElse(new ConversationSessionDocument())`로 생성/수정 통합
- Upsert 패턴 사용: `findByMessageId().orElse(new ConversationMessageDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑
- 타입 변환: `Instant` → `LocalDateTime`, `Long` → `String` (userId)
- MongoDB Soft Delete 미지원: 삭제 시 물리적 삭제

---

#### ArchiveSyncService

**인터페이스 정의**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;

/**
 * Archive 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 Archive 엔티티 변경을 MongoDB Atlas의 ArchiveDocument에 동기화합니다.
 */
public interface ArchiveSyncService {
    
    /**
     * Archive 생성 이벤트 동기화
     * 
     * @param event ArchiveCreatedEvent
     */
    void syncArchiveCreated(ArchiveCreatedEvent event);
    
    /**
     * Archive 수정 이벤트 동기화
     * 
     * @param event ArchiveUpdatedEvent
     */
    void syncArchiveUpdated(ArchiveUpdatedEvent event);
    
    /**
     * Archive 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event ArchiveDeletedEvent
     */
    void syncArchiveDeleted(ArchiveDeletedEvent event);
    
    /**
     * Archive 복원 이벤트 동기화
     * MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
     * 
     * @param event ArchiveRestoredEvent
     */
    void syncArchiveRestored(ArchiveRestoredEvent event);
}
```

**구현 클래스 설계**:

```java
package com.tech.n.ai.common.kafka.sync;

import com.tech.n.ai.common.kafka.event.*;
import com.tech.n.ai.domain.mongodb.document.ArchiveDocument;
import com.tech.n.ai.domain.mongodb.repository.ArchiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Archive 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveSyncServiceImpl implements ArchiveSyncService {
    
    private final ArchiveRepository archiveRepository;
    
    @Override
    public void syncArchiveCreated(ArchiveCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: archiveTsid로 조회하여 없으면 생성, 있으면 업데이트
            ArchiveDocument document = archiveRepository
                .findByArchiveTsid(payload.archiveTsid())
                .orElse(new ArchiveDocument());
            
            document.setArchiveTsid(payload.archiveTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setArchivedAt(convertToLocalDateTime(payload.archivedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveCreatedEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveCreatedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveCreatedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveUpdated(ArchiveUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // archiveTsid로 Document 조회
            ArchiveDocument document = archiveRepository
                .findByArchiveTsid(payload.archiveTsid())
                .orElseThrow(() -> new RuntimeException(
                    "ArchiveDocument not found: archiveTsid=" + payload.archiveTsid()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveUpdatedEvent: archiveTsid={}, updatedFields={}", 
                payload.archiveTsid(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveUpdatedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveDeleted(ArchiveDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            archiveRepository.deleteByArchiveTsid(payload.archiveTsid());
            
            log.debug("Successfully synced ArchiveDeletedEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveDeletedEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveDeletedEvent", e);
        }
    }
    
    @Override
    public void syncArchiveRestored(ArchiveRestoredEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
            ArchiveDocument document = new ArchiveDocument();
            document.setArchiveTsid(payload.archiveTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setArchivedAt(convertToLocalDateTime(payload.archivedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            archiveRepository.save(document);
            
            log.debug("Successfully synced ArchiveRestoredEvent: archiveTsid={}, userId={}", 
                payload.archiveTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync ArchiveRestoredEvent: eventId={}, archiveTsid={}", 
                event.eventId(), event.payload().archiveTsid(), e);
            throw new RuntimeException("Failed to sync ArchiveRestoredEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * 주의: itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로
     * ArchiveUpdatedEvent의 updatedFields에 포함될 수 없습니다.
     * 이 필드들은 원본 아이템(ContestDocument/NewsArticleDocument) 변경 시
     * 별도의 동기화 메커니즘으로 업데이트되어야 합니다.
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵 (tag, memo만 가능)
     */
    private void updateDocumentFields(ArchiveDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            switch (fieldName) {
                // itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로 제외
                // 원본 아이템 변경 시 별도 동기화 메커니즘 필요
                case "tag":
                    document.setTag((String) value);
                    break;
                case "memo":
                    document.setMemo((String) value);
                    break;
                default:
                    log.warn("Unknown field in updatedFields: {} (itemTitle, itemSummary are not supported as they are not in ArchiveEntity)", fieldName);
            }
        }
    }
    
    /**
     * Instant를 LocalDateTime으로 변환
     * 
     * @param instant Instant 객체
     * @return LocalDateTime 객체
     */
    private LocalDateTime convertToLocalDateTime(Instant instant) {
        return instant != null 
            ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            : null;
    }
}
```

**특징**:
- Upsert 패턴 사용: `findByArchiveTsid().orElse(new ArchiveDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑
- 타입 변환: `Instant` → `LocalDateTime`, `String` → `ObjectId`
- MongoDB Soft Delete 미지원: 삭제 시 물리적 삭제, 복원 시 새로 생성

#### updatedFields 처리 전략

**부분 업데이트 전략**:
- `Map<String, Object>`의 각 키를 Document 필드명과 매핑
- `switch` 문을 통한 필드별 처리
- 알 수 없는 필드는 경고 로그만 출력 (무시)

**중요 제약사항**:
- **ArchiveUpdatedEvent의 updatedFields에는 ArchiveEntity에 있는 필드만 포함 가능**
  - 지원 필드: `tag`, `memo` (ArchiveEntity에 존재하는 필드)
  - **제외 필드**: `itemTitle`, `itemSummary` (ArchiveEntity에 없는 필드)
- `itemTitle`, `itemSummary`는 비정규화 필드로, 원본 아이템(ContestDocument/NewsArticleDocument)의 정보를 중복 저장
- 원본 아이템 변경 시 별도의 동기화 메커니즘으로 `ArchiveDocument`의 `itemTitle`, `itemSummary`를 업데이트해야 함
- `ArchiveUpdatedEvent`의 `updatedFields`에 `itemTitle`, `itemSummary`가 포함되면 경고 로그만 출력하고 무시

**타입 변환**:
- `String` → `String`: 그대로 사용
- `String` → `ObjectId`: `new ObjectId(stringValue)` 사용
- `Instant` → `LocalDateTime`: `LocalDateTime.ofInstant(instant, ZoneId.systemDefault())` 사용
- `null` 값: Document 필드에 `null` 설정 (nullable 필드인 경우)

**에러 핸들링**:
- 타입 변환 실패 시 `ClassCastException` 발생 → 로깅 및 `RuntimeException` 전파
- 알 수 없는 필드는 경고 로그만 출력하고 계속 진행

### 이벤트 처리 로직 설계

#### 이벤트 처리 흐름 다이어그램

```mermaid
flowchart TD
    Start([이벤트 수신]) --> CheckIdempotency{멱등성 확인<br/>Redis}
    CheckIdempotency -->|이미 처리됨| Skip[이벤트 스킵<br/>acknowledge]
    CheckIdempotency -->|처리 필요| ProcessEvent[processEvent 호출]
    
    ProcessEvent --> EventType{이벤트 타입 확인}
    
    EventType -->|USER_CREATED| UserCreated[UserSyncService<br/>syncUserCreated]
    EventType -->|USER_UPDATED| UserUpdated[UserSyncService<br/>syncUserUpdated]
    EventType -->|USER_DELETED| UserDeleted[UserSyncService<br/>syncUserDeleted]
    EventType -->|USER_RESTORED| UserRestored[UserSyncService<br/>syncUserRestored]
    
    EventType -->|ARCHIVE_CREATED| ArchiveCreated[ArchiveSyncService<br/>syncArchiveCreated]
    EventType -->|ARCHIVE_UPDATED| ArchiveUpdated[ArchiveSyncService<br/>syncArchiveUpdated]
    EventType -->|ARCHIVE_DELETED| ArchiveDeleted[ArchiveSyncService<br/>syncArchiveDeleted]
    EventType -->|ARCHIVE_RESTORED| ArchiveRestored[ArchiveSyncService<br/>syncArchiveRestored]
    
    EventType -->|CONVERSATION_SESSION_CREATED| SessionCreated[ConversationSyncService<br/>syncSessionCreated]
    EventType -->|CONVERSATION_SESSION_UPDATED| SessionUpdated[ConversationSyncService<br/>syncSessionUpdated]
    EventType -->|CONVERSATION_SESSION_DELETED| SessionDeleted[ConversationSyncService<br/>syncSessionDeleted]
    EventType -->|CONVERSATION_MESSAGE_CREATED| MessageCreated[ConversationSyncService<br/>syncMessageCreated]
    
    EventType -->|CONTEST_SYNCED<br/>NEWS_ARTICLE_SYNCED| BatchEvent[로깅만 수행<br/>동기화 불필요]
    EventType -->|Unknown| UnknownEvent[경고 로그<br/>무시]
    
    UserCreated --> SyncSuccess{동기화 성공?}
    UserUpdated --> SyncSuccess
    UserDeleted --> SyncSuccess
    UserRestored --> SyncSuccess
    ArchiveCreated --> SyncSuccess
    ArchiveUpdated --> SyncSuccess
    ArchiveDeleted --> SyncSuccess
    ArchiveRestored --> SyncSuccess
    SessionCreated --> SyncSuccess
    SessionUpdated --> SyncSuccess
    SessionDeleted --> SyncSuccess
    MessageCreated --> SyncSuccess
    BatchEvent --> MarkProcessed
    UnknownEvent --> MarkProcessed
    
    SyncSuccess -->|성공| MarkProcessed[Redis에 처리 완료 표시]
    SyncSuccess -->|실패| ErrorLog[에러 로깅<br/>예외 전파]
    ErrorLog --> Retry[Spring Kafka<br/>재시도 메커니즘]
    
    MarkProcessed --> Commit[acknowledge<br/>커밋]
    Retry --> End([종료])
    Commit --> End
    Skip --> End
    
    style CheckIdempotency fill:#FFE4B5
    style EventType fill:#FFE4B5
    style SyncSuccess fill:#90EE90
    style ErrorLog fill:#FFB6C1
    style MarkProcessed fill:#87CEEB
```

#### EventConsumer.processEvent 구현

**구현 설계**:

```java
private void processEvent(BaseEvent event) {
    String eventType = event.eventType();
    
    try {
        switch (eventType) {
            case "USER_CREATED":
                if (event instanceof UserCreatedEvent userEvent) {
                    userSyncService.syncUserCreated(userEvent);
                }
                break;
            case "USER_UPDATED":
                if (event instanceof UserUpdatedEvent userEvent) {
                    userSyncService.syncUserUpdated(userEvent);
                }
                break;
            case "USER_DELETED":
                if (event instanceof UserDeletedEvent userEvent) {
                    userSyncService.syncUserDeleted(userEvent);
                }
                break;
            case "USER_RESTORED":
                if (event instanceof UserRestoredEvent userEvent) {
                    userSyncService.syncUserRestored(userEvent);
                }
                break;
            case "ARCHIVE_CREATED":
                if (event instanceof ArchiveCreatedEvent archiveEvent) {
                    archiveSyncService.syncArchiveCreated(archiveEvent);
                }
                break;
            case "ARCHIVE_UPDATED":
                if (event instanceof ArchiveUpdatedEvent archiveEvent) {
                    archiveSyncService.syncArchiveUpdated(archiveEvent);
                }
                break;
            case "ARCHIVE_DELETED":
                if (event instanceof ArchiveDeletedEvent archiveEvent) {
                    archiveSyncService.syncArchiveDeleted(archiveEvent);
                }
                break;
            case "ARCHIVE_RESTORED":
                if (event instanceof ArchiveRestoredEvent archiveEvent) {
                    archiveSyncService.syncArchiveRestored(archiveEvent);
                }
                break;
            case "CONVERSATION_SESSION_CREATED":
                if (event instanceof ConversationSessionCreatedEvent sessionEvent) {
                    conversationSyncService.syncSessionCreated(sessionEvent);
                }
                break;
            case "CONVERSATION_SESSION_UPDATED":
                if (event instanceof ConversationSessionUpdatedEvent sessionEvent) {
                    conversationSyncService.syncSessionUpdated(sessionEvent);
                }
                break;
            case "CONVERSATION_SESSION_DELETED":
                if (event instanceof ConversationSessionDeletedEvent sessionEvent) {
                    conversationSyncService.syncSessionDeleted(sessionEvent);
                }
                break;
            case "CONVERSATION_MESSAGE_CREATED":
                if (event instanceof ConversationMessageCreatedEvent messageEvent) {
                    conversationSyncService.syncMessageCreated(messageEvent);
                }
                break;
            case "CONTEST_SYNCED":
            case "NEWS_ARTICLE_SYNCED":
                // 배치 작업에서 직접 MongoDB에 저장되므로 동기화 불필요
                // 로깅만 수행
                log.debug("Skipping sync for batch event: eventType={}, eventId={}", 
                    eventType, event.eventId());
                break;
            default:
                log.warn("Unknown event type: eventType={}, eventId={}", 
                    eventType, event.eventId());
        }
    } catch (Exception e) {
        log.error("Error processing event: eventType={}, eventId={}", 
            eventType, event.eventId(), e);
        throw e; // 예외 전파하여 Spring Kafka 재시도 메커니즘 활용
    }
}
```

**의존성 주입**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
    private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserSyncService userSyncService;        // 추가
    private final ArchiveSyncService archiveSyncService;  // 추가
    private final ConversationSyncService conversationSyncService;  // 추가
    
    // ... 기존 코드 ...
}
```

**특징**:
- `switch` 문을 통한 이벤트 타입별 분기 처리 (단순하고 명확)
- Pattern Matching for `instanceof` 사용 (Java 16+)
- Contest/News 이벤트는 로깅만 수행 (동기화 불필요)
- 예외 발생 시 전파하여 Spring Kafka 재시도 메커니즘 활용

**에러 핸들링**:
- 동기화 실패 시 로깅 및 예외 전파
- Spring Kafka의 기본 재시도 메커니즘 활용
- 최대 재시도 횟수 초과 시 Dead Letter Queue로 이동 (Spring Kafka 기본 동작)

#### updatedFields 처리 흐름 다이어그램

```mermaid
flowchart TD
    Start([UserUpdatedEvent<br/>또는<br/>ArchiveUpdatedEvent]) --> Extract[updatedFields<br/>Map 추출]
    Extract --> Loop{모든 필드<br/>순회}
    Loop -->|다음 필드| CheckField{필드명 확인}
    
    CheckField -->|username| SetUsername[document.setUsername]
    CheckField -->|email| SetEmail[document.setEmail]
    CheckField -->|profileImageUrl| SetProfileImage[document.setProfileImageUrl]
    CheckField -->|itemTitle| WarnLog[경고 로그<br/>ArchiveEntity에 없는 필드<br/>무시]
    CheckField -->|itemSummary| WarnLog
    CheckField -->|tag| SetTag[document.setTag]
    CheckField -->|memo| SetMemo[document.setMemo]
    CheckField -->|알 수 없는 필드| WarnLog
    
    SetUsername --> SetUpdatedAt
    SetEmail --> SetUpdatedAt
    SetProfileImage --> SetUpdatedAt
    SetTag --> SetUpdatedAt
    SetMemo --> SetUpdatedAt
    WarnLog --> Loop
    
    SetUpdatedAt[document.setUpdatedAt<br/>LocalDateTime.now] --> Save[repository.save]
    Save --> End([완료])
    
    Loop -->|모든 필드 처리 완료| SetUpdatedAt
    
    style CheckField fill:#FFE4B5
    style SetUpdatedAt fill:#90EE90
    style WarnLog fill:#FFB6C1
```

#### Upsert 패턴 다이어그램

```mermaid
sequenceDiagram
    participant SS as SyncService
    participant Repo as Repository
    participant Doc as Document
    participant DB as MongoDB Atlas
    
    Note over SS,DB: Upsert 패턴: 생성/수정 통합
    
    SS->>Repo: findByTsid(tsid)
    Repo->>DB: 조회 쿼리
    alt Document 존재
        DB-->>Repo: 기존 Document 반환
        Repo-->>SS: Optional.of(document)
        SS->>Doc: 필드 업데이트
        SS->>Doc: setUpdatedAt(now)
    else Document 없음
        DB-->>Repo: null 반환
        Repo-->>SS: Optional.empty()
        SS->>Doc: new Document()
        SS->>Doc: 모든 필드 설정
        SS->>Doc: setCreatedAt(now)
        SS->>Doc: setUpdatedAt(now)
    end
    SS->>Repo: save(document)
    Repo->>DB: 저장/업데이트
    DB-->>Repo: 완료
    Repo-->>SS: 완료
```

---

## 구현 가이드

### 1. 동기화 서비스 인터페이스 생성

**패키지 구조**: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/sync/`

**파일 생성**:
1. `UserSyncService.java` (인터페이스)
2. `ArchiveSyncService.java` (인터페이스)
3. `ConversationSyncService.java` (인터페이스)

**주의사항**:
- 인터페이스는 `public` 접근 제어자 사용
- 각 메서드는 `void` 반환 타입 (비동기 처리)
- 메서드 파라미터는 구체적인 이벤트 타입 사용

### 2. 동기화 서비스 구현 클래스 생성

**파일 생성**:
1. `UserSyncServiceImpl.java` (구현 클래스)
2. `ArchiveSyncServiceImpl.java` (구현 클래스)
3. `ConversationSyncServiceImpl.java` (구현 클래스)

**의존성 주입**:
- `@Service` 어노테이션 사용
- `@RequiredArgsConstructor` 사용 (Lombok)
- `UserProfileRepository`, `ArchiveRepository`, `ConversationSessionDocumentRepository`, `ConversationMessageDocumentRepository` 주입
- `MongoTemplate` 주입 (필요 시, 현재는 Repository만 사용)

**Upsert 패턴 구현**:
```java
// 생성/수정 통합
Document document = repository
    .findByTsid(tsid)
    .orElse(new Document());
    
// 필드 설정
document.setField(value);
document.setUpdatedAt(LocalDateTime.now());

repository.save(document);
```

**updatedFields 처리**:
- `switch` 문을 통한 필드별 매핑
- 타입 변환 로직 포함
- 알 수 없는 필드는 경고 로그만 출력

### 3. EventConsumer.processEvent 구현

**수정 파일**: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`

**수정 내용**:
1. 동기화 서비스 의존성 주입 추가
2. `processEvent` 메서드 구현
3. 이벤트 타입별 분기 처리

**주의사항**:
- Pattern Matching for `instanceof` 사용 (Java 16+)
- Contest/News 이벤트는 로깅만 수행
- 예외 발생 시 전파하여 재시도 메커니즘 활용

### 4. 의존성 주입 설정

**Spring Bean 등록**:
- `@Service` 어노테이션으로 자동 등록
- `@RequiredArgsConstructor`로 생성자 주입

**순환 의존성 방지**:
- `EventConsumer` → `UserSyncService`, `ArchiveSyncService`, `ConversationSyncService`
- `UserSyncService`, `ArchiveSyncService`, `ConversationSyncService` → `Repository`
- 순환 의존성 없음 (단방향 의존성)

### 5. MongoDB Atlas 연결 설정 구현

**필수 작업**:
1. `application-mongodb-domain.yml` 파일 생성
2. `MongoClientConfig` 클래스 생성 (연결 풀 최적화)
3. 환경변수 설정 (`MONGODB_ATLAS_CONNECTION_STRING`)

**주의사항**:
- 연결 문자열에 `readPreference=secondaryPreferred` 포함
- SSL/TLS 설정 확인 (프로덕션 환경 필수)
- 연결 풀 크기는 클러스터 티어에 맞게 조정

---

## 검증 기준

### 1. 기능 검증

#### 1.1 모든 이벤트 타입에 대한 동기화 동작 확인

**User 이벤트**:
- ✅ `UserCreatedEvent` → `UserProfileDocument` 생성
- ✅ `UserUpdatedEvent` → `UserProfileDocument` 업데이트
- ✅ `UserDeletedEvent` → `UserProfileDocument` 삭제
- ✅ `UserRestoredEvent` → `UserProfileDocument` 생성

**Archive 이벤트**:
- ✅ `ArchiveCreatedEvent` → `ArchiveDocument` 생성
- ✅ `ArchiveUpdatedEvent` → `ArchiveDocument` 업데이트
- ✅ `ArchiveDeletedEvent` → `ArchiveDocument` 삭제
- ✅ `ArchiveRestoredEvent` → `ArchiveDocument` 생성

**Conversation 이벤트**:
- ✅ `ConversationSessionCreatedEvent` → `ConversationSessionDocument` 생성
- ✅ `ConversationSessionUpdatedEvent` → `ConversationSessionDocument` 업데이트
- ✅ `ConversationSessionDeletedEvent` → `ConversationSessionDocument` 삭제
- ✅ `ConversationMessageCreatedEvent` → `ConversationMessageDocument` 생성

**검증 방법**:
- 각 이벤트 타입별 통합 테스트 작성
- MongoDB Atlas에서 Document 생성/수정/삭제 확인

#### 1.2 멱등성 보장 확인

**검증 시나리오**:
1. 동일한 이벤트 ID로 이벤트 2회 수신
2. 첫 번째 수신: 정상 처리
3. 두 번째 수신: Redis에서 처리 여부 확인 후 스킵

**검증 방법**:
- 동일한 `eventId`로 이벤트 2회 발행
- 두 번째 수신 시 로그에서 "Event already processed" 메시지 확인
- MongoDB Atlas에서 Document가 1회만 생성/수정되었는지 확인

#### 1.3 에러 핸들링 동작 확인

**검증 시나리오**:
1. MongoDB 연결 실패 시나리오
2. 잘못된 이벤트 페이로드 시나리오
3. 알 수 없는 이벤트 타입 시나리오

**검증 방법**:
- 에러 발생 시 로깅 확인
- Spring Kafka 재시도 메커니즘 동작 확인
- 예외 전파 확인

### 2. 성능 검증

#### 2.1 동기화 지연 시간 측정

**목표**: 1초 이내

**측정 방법**:
1. 이벤트 발행 시각 기록
2. MongoDB Atlas에 Document 저장 완료 시각 기록
3. 지연 시간 = 저장 완료 시각 - 발행 시각

**검증 기준**:
- 평균 지연 시간 < 1초
- 95 백분위수 지연 시간 < 1초

#### 2.2 동시성 처리 확인

**검증 시나리오**:
1. 동일한 `userTsid`, `archiveTsid`, `session_id`, `message_id`로 여러 이벤트 동시 수신
2. Partition Key를 통한 순서 보장 확인

**검증 방법**:
- 동일한 Partition Key로 여러 이벤트 발행
- 이벤트 처리 순서 확인
- 최종 Document 상태 확인

### 3. 빌드 검증

**검증 명령어**:
```bash
# 개별 모듈 빌드
./gradlew :common-kafka:build
./gradlew :domain-mongodb:build

# 전체 빌드
./gradlew clean build
```

**검증 기준**:
- ✅ 모든 빌드 명령어 성공
- ✅ 컴파일 에러 없음
- ✅ 테스트 통과 (통합 테스트 포함)

### 4. MongoDB Atlas 연결 검증

**연결 검증**:
- ✅ MongoDB Atlas Cluster 연결 성공
- ✅ 연결 풀 정상 동작 (연결 수 모니터링)
- ✅ Read Preference 적용 확인 (`secondaryPreferred`)
- ✅ SSL/TLS 연결 확인 (프로덕션 환경)

**성능 검증**:
- ✅ 연결 풀 모니터링 (최대/최소/유휴 연결 수)
- ✅ 쿼리 성능 측정 (인덱스 활용 확인)
- ✅ 읽기 복제본 활용 확인 (Read Preference 동작)

---

## 다이어그램 요약

이 설계서에 포함된 Mermaid 다이어그램:

1. **전체 아키텍처 다이어그램** (flowchart)
   - Command Side, Kafka, Query Side 간의 관계
   - 각 컴포넌트의 구현 상태 표시 (✅ 완료, ❌ 미구현)
   - 이벤트 흐름 및 의존성 관계

2. **동기화 시퀀스 다이어그램** (sequenceDiagram)
   - 이벤트 발행부터 MongoDB 동기화까지의 전체 흐름
   - 멱등성 확인 및 처리 로직 포함
   - alt 구문을 통한 분기 처리 시각화

3. **데이터 매핑 다이어그램** (erDiagram)
   - Aurora MySQL 엔티티와 MongoDB Document 간의 매핑 관계
   - TSID 필드 기반 1:1 매핑 시각화
   - 필드 구조 및 관계 명시

4. **클래스 다이어그램** (classDiagram)
   - 모든 관련 클래스 및 인터페이스 관계
   - 의존성 및 구현 관계 표시
   - 이벤트 모델과 서비스 계층 구조

5. **이벤트 처리 흐름 다이어그램** (flowchart)
   - 이벤트 타입별 분기 처리
   - 에러 핸들링 및 재시도 메커니즘
   - 멱등성 확인부터 커밋까지의 전체 흐름

6. **updatedFields 처리 흐름 다이어그램** (flowchart)
   - 부분 업데이트 처리 로직
   - 필드별 매핑 과정
   - 타입 변환 및 에러 처리

7. **Upsert 패턴 다이어그램** (sequenceDiagram)
   - 생성/수정 통합 처리 로직
   - Document 조회 및 저장 과정
   - alt 구문을 통한 생성/수정 분기

**다이어그램 사용 방법**:
- 모든 다이어그램은 [Mermaid Live Editor](https://mermaid.live)에서 확인 및 편집 가능
- Markdown 뷰어에서 자동으로 렌더링됨 (GitHub, GitLab, VS Code 등)
- 다이어그램 코드를 복사하여 Mermaid Live Editor에 붙여넣으면 상호작용 가능한 다이어그램으로 확인 가능
- 다이어그램을 이미지로 내보내기 가능 (PNG, SVG 형식)

---

## 참고 문서

### 설계서
- `docs/step2/2. data-model-design.md` - 실시간 동기화 전략 (620-808라인)
- `docs/step1/2. mongodb-schema-design.md` - MongoDB 스키마 설계
- `docs/step1/3. aurora-schema-design.md` - Aurora 스키마 설계
- `docs/reference/shrimp-task-prompts-final-goal.md` - 최종 프로젝트 목표

### 공식 문서
- [Spring Kafka 공식 문서](https://docs.spring.io/spring-kafka/reference/html/)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring Data MongoDB 공식 문서](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [MongoDB 공식 문서](https://www.mongodb.com/docs/)

---

## MongoDB Atlas 연결 설정 구현 상태

### 구현 완료 항목

**✅ 완료된 작업**:
1. **연결 설정 파일 생성**: `domain/mongodb/src/main/resources/application-mongodb-domain.yml`
   - MongoDB Atlas Cluster 연결 문자열 설정
   - 환경변수 기반 관리 (`MONGODB_ATLAS_CONNECTION_STRING`)
   - Profile별 설정 분리 (local, dev, prod)

2. **연결 풀 최적화 Config 클래스 생성**: `domain/mongodb/src/main/java/.../config/MongoClientConfig.java`
   - 연결 풀 최적화 (maxSize: 100, minSize: 10)
   - 타임아웃 설정 (connectTimeout: 10초, readTimeout: 30초)
   - Read Preference 설정 (`secondaryPreferred`)
   - Write Concern 설정 (`w: "majority"`)
   - Retry 설정 (retryWrites, retryReads)

3. **API 모듈 Profile 설정 업데이트**:
   - `api-news`: `mongodb-domain` profile 추가
   - `api-contest`: `mongodb-domain` profile 추가
   - `api-gateway`: `mongodb-domain` profile 추가

### 환경변수 설정

**필수 환경변수**:
- `MONGODB_ATLAS_CONNECTION_STRING`: MongoDB Atlas Cluster 연결 문자열
  - 형식: `mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true`
  - 또는 Standard Connection String: `mongodb://{username}:{password}@{cluster-endpoint}:27017/{database}?ssl=true&replicaSet=...&readPreference=secondaryPreferred`
  - **보안**: 환경변수로 관리 (코드에 하드코딩 금지)

**선택 환경변수**:
- `MONGODB_ATLAS_DATABASE`: 데이터베이스 이름 (기본값: `shrimp_task_manager`)

**환경별 관리**:
- 로컬: `.env` 파일 사용 (`.gitignore`에 포함)
- 프로덕션: AWS Secrets Manager, Parameter Store 등 활용

### 최적화 설정 요약

**연결 풀 최적화**:
- `maxSize: 100`: 최대 연결 수 (클러스터 티어에 따라 조정)
- `minSize: 10`: 최소 연결 수 (연결 생성 오버헤드 감소)
- `maxConnectionIdleTime: 60초`: 유휴 연결 타임아웃

**Read Preference**:
- `secondaryPreferred`: 읽기 복제본 우선 사용
- CQRS 패턴의 Query Side 특성상 최종 일관성 허용 가능

**Write Concern**:
- `w: "majority"`: 대다수 노드에서 확인 (데이터 일관성 보장)
- `wtimeout: 5초`: Write Concern 타임아웃

### 주의사항

1. **연결 문자열 설정**:
   - 연결 문자열에 `readPreference=secondaryPreferred` 포함 권장
   - 프로덕션 환경에서는 SSL/TLS 필수 (`ssl=true`)

2. **연결 풀 크기 조정**:
   - 클러스터 티어에 맞게 조정 필요
   - M10: maxSize 100
   - M20: maxSize 200
   - M30: maxSize 300

3. **Profile 활성화**:
   - API 모듈의 `application-*-api.yml`에 `mongodb-domain` profile이 포함되어 있어야 함
   - 이미 업데이트 완료: `api-news`, `api-contest`, `api-gateway`

---

**작성 완료 일시**: 2026-01-XX
