# CQRS 패턴 기반 Kafka 동기화 설계서

**작성 일시**: 2026-01-22  
**대상 모듈**: `common-kafka`, `domain-mongodb`  
**목적**: Kafka를 통한 Amazon Aurora MySQL과 MongoDB Atlas 간 실시간 동기화 구현 설계  
**최종 업데이트**: 2026-01-22 (구현 완료 기준 업데이트)

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
- ✅ **Kafka Consumer (`EventConsumer`)**: 완료 (EventHandlerRegistry 패턴 적용)
- ✅ **멱등성 보장 (`IdempotencyService`)**: 완료 (Redis 기반)
- ✅ **이벤트 핸들러 패턴**: 완료 (EventHandler 인터페이스 + EventHandlerRegistry)
- ✅ **이벤트 모델**: Conversation 관련 이벤트 정의 완료
- ✅ **MongoDB Document 및 Repository**: 완료
- ✅ **MongoDB 인덱스 설정 (`MongoIndexConfig`)**: 완료
- ✅ **MongoDB Atlas 연결 설정**: 완료
- ✅ **동기화 서비스**: `ConversationSyncService` 완료
- ✅ **이벤트 핸들러**: 4개 구현 완료

### 현재 구현된 이벤트

| 이벤트 타입 | 이벤트 클래스 | 핸들러 | Kafka Topic |
|------------|--------------|--------|-------------|
| `CONVERSATION_SESSION_CREATED` | `ConversationSessionCreatedEvent` | `ConversationSessionCreatedEventHandler` | `shrimp-tm.conversation.session.created` |
| `CONVERSATION_SESSION_UPDATED` | `ConversationSessionUpdatedEvent` | `ConversationSessionUpdatedEventHandler` | `shrimp-tm.conversation.session.updated` |
| `CONVERSATION_SESSION_DELETED` | `ConversationSessionDeletedEvent` | `ConversationSessionDeletedEventHandler` | `shrimp-tm.conversation.session.deleted` |
| `CONVERSATION_MESSAGE_CREATED` | `ConversationMessageCreatedEvent` | `ConversationMessageCreatedEventHandler` | `shrimp-tm.conversation.message.created` |

### 설계서 범위

**포함 사항**:
- Conversation 엔티티의 MongoDB 동기화 서비스 설계
- EventHandler 패턴 기반 이벤트 처리 아키텍처
- `updatedFields` (Map<String, Object>) 처리 전략

**제외 사항**:
- Kafka 토픽 생성 (인프라 작업, 코드에 포함 불필요)
- 복잡한 DLQ 처리 (기본 재시도로 충분, 필요 시 후속 단계에서 추가)
- 복잡한 모니터링 시스템 (기본 로깅으로 충분)
- Contest/News 동기화 서비스 (배치 작업을 통해 직접 MongoDB에 저장되므로 Kafka 동기화 불필요)

---

## 설계 원칙

### 1. 클린코드 원칙

1. **단일 책임 원칙 (SRP)**
   - 각 동기화 서비스는 하나의 엔티티 타입만 담당
   - `ConversationSyncService`: Conversation 엔티티 동기화만 담당
   - 향후 확장: `UserSyncService`, `BookmarkSyncService` 등

2. **의존성 역전 원칙 (DIP)**
   - 인터페이스 기반 설계
   - 동기화 서비스는 인터페이스로 정의하고 구현체 분리
   - EventHandler는 동기화 서비스 인터페이스에만 의존

3. **개방-폐쇄 원칙 (OCP)**
   - 새로운 이벤트 타입 추가 시 기존 코드 수정 없이 확장 가능
   - EventHandler 패턴을 통한 이벤트 처리 로직 확장

### 2. 객체지향 설계 기법

1. **전략 패턴 (Strategy Pattern)**
   - 이벤트 타입별 처리 전략 분리
   - EventHandler 인터페이스를 통한 전략 구현
   - EventHandlerRegistry가 이벤트 타입에 따라 적절한 핸들러 선택

2. **레지스트리 패턴 (Registry Pattern)**
   - EventHandlerRegistry가 모든 EventHandler 구현체를 자동 등록
   - Spring DI를 통한 핸들러 자동 주입
   - 런타임에 이벤트 타입으로 핸들러 조회

3. **템플릿 메서드 패턴 (Template Method Pattern)**
   - 공통 동기화 흐름 정의 (이벤트 수신 → 멱등성 확인 → 핸들러 실행 → 완료 표시)
   - `EventConsumer`에서 이미 구현됨

### 3. CQRS 패턴 원칙

1. **Command Side와 Query Side 완전 분리**
   - Command Side: Aurora MySQL에만 쓰기
   - Query Side: MongoDB Atlas에서만 읽기
   - 동기화는 Kafka 이벤트를 통해서만 수행

2. **이벤트 기반 비동기 동기화**
   - 모든 쓰기 작업 후 Kafka 이벤트 발행
   - 비동기 처리로 Command Side 성능 영향 최소화

3. **세션 ID 기반 1:1 매핑 보장**
   - `ConversationSession.id(TSID)` → `ConversationSessionDocument.sessionId`
   - `ConversationMessage.messageId(TSID)` → `ConversationMessageDocument.messageId`
   - UNIQUE 인덱스를 통한 정확성 보장

### 4. 최소 구현 원칙

1. **현재 필요한 기능만 구현**
   - Conversation 동기화만 구현 (대화 세션 및 메시지)
   - Contest/News는 배치 작업으로 직접 저장되므로 Kafka 동기화 불필요
   - 향후 확장: User, Bookmark 동기화 추가 가능

2. **단순하고 명확한 구조**
   - 복잡한 추상화 레이어 지양
   - 직접적이고 이해하기 쉬운 코드
   - EventHandler 패턴으로 확장성 확보

3. **단계적 확장 가능한 구조**
   - 향후 다른 엔티티 동기화 추가 시 동일한 패턴 적용 가능
   - 새 EventHandler 구현체만 추가하면 자동으로 등록됨

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

#### 1.2 EventConsumer 및 EventHandler 패턴 구현 상태

**위치**: 
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventHandlerRegistry.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventHandler.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/IdempotencyService.java`

**EventConsumer 구현 내용**:
- ✅ `@KafkaListener` 설정: Conversation 관련 토픽 수신
  - `shrimp-tm.conversation.session.created`
  - `shrimp-tm.conversation.session.updated`
  - `shrimp-tm.conversation.session.deleted`
  - `shrimp-tm.conversation.message.created`
- ✅ 멱등성 보장: `IdempotencyService`를 통한 Redis 기반 중복 처리 방지
- ✅ 수동 커밋: `Acknowledgment.acknowledge()` 사용
- ✅ 에러 핸들링: 예외 발생 시 로깅 및 예외 전파 (Spring Kafka 재시도 활용)
- ✅ EventHandlerRegistry에 이벤트 처리 위임

**EventHandler 패턴 구조**:
```java
// EventHandler 인터페이스
public interface EventHandler<T extends BaseEvent> {
    void handle(T event);
    String getEventType();
}

// EventHandlerRegistry
@Component
public class EventHandlerRegistry {
    private final Map<String, EventHandler<? extends BaseEvent>> handlers;
    
    // Spring DI를 통해 모든 EventHandler 구현체 자동 등록
    public EventHandlerRegistry(List<EventHandler<? extends BaseEvent>> handlerList) {
        handlerList.forEach(handler -> 
            handlers.put(handler.getEventType(), handler)
        );
    }
    
    public <T extends BaseEvent> void handle(T event) {
        // 이벤트 타입으로 핸들러 조회 및 실행
    }
}
```

**구현된 EventHandler 구현체**:
- ✅ `ConversationSessionCreatedEventHandler`
- ✅ `ConversationSessionUpdatedEventHandler`
- ✅ `ConversationSessionDeletedEventHandler`
- ✅ `ConversationMessageCreatedEventHandler`

**특징**:
- EventHandler 패턴으로 이벤트 타입별 처리 로직 완전 분리
- 새 이벤트 추가 시 EventHandler 구현체만 추가하면 자동 등록
- IdempotencyService를 통한 멱등성 보장 (Redis, TTL 7일)
- 각 핸들러는 ConversationSyncService에 동기화 위임

#### 1.3 이벤트 모델 구조

**BaseEvent 인터페이스**:
```java
public interface BaseEvent {
    String eventId();      // UUID 형식
    String eventType();    // 이벤트 타입 문자열
    Instant timestamp();   // 이벤트 발생 시각
}
```

**Conversation 관련 이벤트 (현재 구현됨)**:

1. **ConversationSessionCreatedEvent**: 대화 세션 생성
   - Payload: `sessionId`, `userId`, `title`, `lastMessageAt`, `isActive`
   - 이벤트 타입: `CONVERSATION_SESSION_CREATED`

2. **ConversationSessionUpdatedEvent**: 대화 세션 수정
   - Payload: `sessionId`, `userId`, `updatedFields` (Map<String, Object>)
   - 이벤트 타입: `CONVERSATION_SESSION_UPDATED`
   - updatedFields 가능 필드: `title`, `lastMessageAt`, `isActive`

3. **ConversationSessionDeletedEvent**: 대화 세션 삭제 (Soft Delete)
   - Payload: `sessionId`, `userId`, `deletedAt`
   - 이벤트 타입: `CONVERSATION_SESSION_DELETED`

4. **ConversationMessageCreatedEvent**: 대화 메시지 생성
   - Payload: `messageId`, `sessionId`, `role`, `content`, `tokenCount`, `sequenceNumber`, `createdAt`
   - 이벤트 타입: `CONVERSATION_MESSAGE_CREATED`

**이벤트 구조 예시**:
```java
public record ConversationSessionCreatedEvent(
    @JsonProperty("eventId") String eventId,
    @JsonProperty("eventType") String eventType,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("payload") ConversationSessionCreatedPayload payload
) implements BaseEvent {
    
    public ConversationSessionCreatedEvent(ConversationSessionCreatedPayload payload) {
        this(
            UUID.randomUUID().toString(),
            "CONVERSATION_SESSION_CREATED",
            Instant.now(),
            payload
        );
    }
    
    public record ConversationSessionCreatedPayload(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("userId") String userId,
        @JsonProperty("title") String title,
        @JsonProperty("lastMessageAt") Instant lastMessageAt,
        @JsonProperty("isActive") Boolean isActive
    ) {}
}
```

**특징**:
- 모든 이벤트는 `record` 타입으로 정의 (불변성 보장)
- `updatedFields`는 `Map<String, Object>` 타입으로 부분 업데이트 지원
- `eventId`, `eventType`, `timestamp`는 생성자에서 자동 생성
- `@JsonProperty`로 직렬화/역직렬화 명확성 보장

#### 1.4 MongoDB Repository 인터페이스

**ConversationSessionRepository**:
```java
public interface ConversationSessionRepository extends MongoRepository<ConversationSessionDocument, ObjectId> {
    Optional<ConversationSessionDocument> findBySessionId(String sessionId);
    List<ConversationSessionDocument> findByUserIdOrderByCreatedAtDesc(String userId);
    Page<ConversationSessionDocument> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}
```

**ConversationMessageRepository**:
```java
public interface ConversationMessageRepository extends MongoRepository<ConversationMessageDocument, ObjectId> {
    Optional<ConversationMessageDocument> findByMessageId(String messageId);
    List<ConversationMessageDocument> findBySessionIdOrderBySequenceNumberAsc(String sessionId);
    Page<ConversationMessageDocument> findBySessionIdOrderBySequenceNumberAsc(String sessionId, Pageable pageable);
}
```

**특징**:
- Spring Data MongoDB의 `MongoRepository` 활용
- `sessionId`, `messageId` 기반 조회 메서드 제공
- UNIQUE 인덱스가 설정되어 있어 중복 방지
- 정렬 및 페이징 지원

### 2. 동기화 서비스 구현 상태

#### 2.1 ConversationSyncService (구현 완료)

**인터페이스**: `common/kafka/src/main/java/.../sync/ConversationSyncService.java`
```java
public interface ConversationSyncService {
    void syncSessionCreated(ConversationSessionCreatedEvent event);
    void syncSessionUpdated(ConversationSessionUpdatedEvent event);
    void syncSessionDeleted(ConversationSessionDeletedEvent event);
    void syncMessageCreated(ConversationMessageCreatedEvent event);
}
```

**구현 클래스**: `ConversationSyncServiceImpl`
- ✅ `@ConditionalOnBean(ConversationSessionRepository.class)`: MongoDB Repository가 있을 때만 활성화
- ✅ Upsert 패턴으로 중복 방지
- ✅ `updatedFields` 부분 업데이트 지원
- ✅ Soft Delete 처리 (MongoDB에서는 물리적 삭제)

**주요 특징**:
- Aurora MySQL → MongoDB Atlas 동기화
- Command Side (Write) → Query Side (Read)
- CQRS 패턴의 Query Side 업데이트 담당

#### 2.2 향후 확장 가능 서비스

**미래 확장 가능**:
- `UserSyncService`: User 이벤트 → UserProfileDocument 동기화
- `BookmarkSyncService`: Bookmark 이벤트 → BookmarkDocument 동기화

**제외 서비스**:
- Contest/News 수집 기능 폐기됨

### 3. updatedFields 처리 전략

#### 3.1 처리 방식

**ConversationSessionUpdatedEvent의 updatedFields**:
```java
private void updateSessionDocumentFields(ConversationSessionDocument document, 
                                         Map<String, Object> updatedFields) {
    for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
        String fieldName = entry.getKey();
        Object value = entry.getValue();
        
        try {
            switch (fieldName) {
                case "title":
                    document.setTitle((String) value);
                    break;
                case "lastMessageAt":
                    if (value instanceof Instant instant) {
                        document.setLastMessageAt(convertToLocalDateTime(instant));
                    }
                    break;
                case "isActive":
                    document.setIsActive((Boolean) value);
                    break;
                default:
                    log.warn("Unknown field in updatedFields: {}", fieldName);
            }
        } catch (ClassCastException e) {
            log.warn("Type mismatch for field {}: {}", fieldName, value.getClass().getName());
        }
    }
}
```

**특징**:
- `Map<String, Object>` → Document 필드 타입 변환
- 필드별 switch 문으로 안전한 타입 캐스팅
- 알 수 없는 필드는 경고 로그만 출력
- 타입 불일치 시 예외 처리

### 3. 구현 우선순위

#### 필수 구현 항목 (Phase 1)
1. ✅ `UserSyncService` 인터페이스 및 구현 클래스
2. ✅ `BookmarkSyncService` 인터페이스 및 구현 클래스
3. ✅ `EventConsumer.processEvent` 메서드 구현
4. ✅ `updatedFields` 처리 로직

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
- `api-gateway` 모듈의 `application-*-api.yml`에 `mongodb-domain` profile 추가 필요
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
package com.ebson.shrimp.tm.demo.domain.mongodb.config;

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
- UNIQUE 인덱스 설정 (`userTsid`, `bookmarkTsid`)
- TTL 인덱스 설정 (`news_articles.publishedAt`, `exception_logs.occurredAt`)

#### 5. 프로젝션 최적화

**권장 사항**:
- 필요한 필드만 선택하여 네트워크 트래픽 최소화
- Repository 메서드에서 프로젝션 활용

**예시**:
```java
// 필요한 필드만 선택
@Query(value = "{userId: ?0}", fields = "{_id: 1, itemTitle: 1, bookmarkedAt: 1}")
List<BookmarkDocument> findBookmarksByUserId(String userId);
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
        BookmarkEntity[Bookmark Entity]
        EventPublisher[EventPublisher<br/>✅ 완료]
    end
    
    subgraph Kafka["Kafka"]
        UserTopic[user-events Topic]
        BookmarkTopic[bookmark-events Topic]
        EventConsumer[EventConsumer<br/>✅ 기본 구조<br/>❌ processEvent 미구현]
    end
    
    subgraph QuerySide["Query Side (MongoDB Atlas)"]
        UserProfileDoc[UserProfileDocument]
        BookmarkDoc[BookmarkDocument]
        UserSyncService[UserSyncService<br/>❌ 미구현]
        BookmarkSyncService[BookmarkSyncService<br/>❌ 미구현]
    end
    
    UserEntity -->|이벤트 발행| EventPublisher
    BookmarkEntity -->|이벤트 발행| EventPublisher
    EventPublisher -->|publish| UserTopic
    EventPublisher -->|publish| BookmarkTopic
    UserTopic -->|이벤트 수신| EventConsumer
    BookmarkTopic -->|이벤트 수신| EventConsumer
    EventConsumer -->|syncUserCreated<br/>syncUserUpdated<br/>syncUserDeleted<br/>syncUserRestored| UserSyncService
    EventConsumer -->|syncBookmarkCreated<br/>syncBookmarkUpdated<br/>syncBookmarkDeleted<br/>syncBookmarkRestored| BookmarkSyncService
    UserSyncService -->|save/delete| UserProfileDoc
    BookmarkSyncService -->|save/delete| BookmarkDoc
    
    style EventPublisher fill:#90EE90
    style EventConsumer fill:#FFE4B5
    style UserSyncService fill:#FFB6C1
    style BookmarkSyncService fill:#FFB6C1
    style UserProfileDoc fill:#87CEEB
    style BookmarkDoc fill:#87CEEB
```

#### 동기화 흐름

**동기화 시퀀스 다이어그램**:

```mermaid
sequenceDiagram
    participant CS as Command Side<br/>(Aurora MySQL)
    participant EP as EventPublisher
    participant K as Kafka Topic
    participant EC as EventConsumer
    participant R as Redis
    participant SS as SyncService
    participant QS as Query Side<br/>(MongoDB Atlas)
    
    CS->>EP: 엔티티 변경<br/>(생성/수정/삭제/복원)
    EP->>K: 이벤트 발행<br/>(Partition Key 사용)
    K->>EC: 이벤트 수신<br/>(@KafkaListener)
    EC->>R: 멱등성 확인<br/>(isEventProcessed)
    alt 이미 처리된 이벤트
        R-->>EC: true (이미 처리됨)
        EC->>K: acknowledge (스킵)
    else 새로운 이벤트
        R-->>EC: false (처리 필요)
        EC->>EC: processEvent()
        EC->>SS: 동기화 서비스 호출<br/>(이벤트 타입별)
        SS->>QS: Document 저장/수정/삭제
        QS-->>SS: 완료
        SS-->>EC: 완료
        EC->>R: 처리 완료 표시<br/>(markEventAsProcessed)
        EC->>K: acknowledge (커밋)
    end
```

**동기화 흐름 단계**:

1. **Command Side (Aurora MySQL)**
   - User 또는 Bookmark 엔티티 생성/수정/삭제/복원
   - `EventPublisher.publish()` 호출하여 Kafka 이벤트 발행
   - Partition Key 사용 (userId, bookmarkTsid 등)으로 이벤트 순서 보장

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
    Bookmark ||--o{ BookmarkDocument : "1:1 매핑"
    
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
    
    Bookmark {
        BIGINT id "TSID PK"
        BIGINT userId "FK to User"
        VARCHAR itemType
        VARCHAR itemId
        VARCHAR itemTitle
        TEXT itemSummary
        VARCHAR tag
        TEXT memo
    }
    
    BookmarkDocument {
        ObjectId _id "MongoDB PK"
        String bookmarkTsid "UNIQUE, TSID 매핑"
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
- `Bookmark.id` (TSID) → `BookmarkDocument.bookmarkTsid` (1:1 매핑, UNIQUE 인덱스)
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
    
    class BookmarkCreatedEvent {
        +BookmarkCreatedPayload payload
    }
    
    class BookmarkUpdatedEvent {
        +BookmarkUpdatedPayload payload
        +Map~String,Object~ updatedFields
    }
    
    class BookmarkDeletedEvent {
        +BookmarkDeletedPayload payload
    }
    
    class BookmarkRestoredEvent {
        +BookmarkRestoredPayload payload
    }
    
    class EventConsumer {
        -RedisTemplate redisTemplate
        -UserSyncService userSyncService
        -BookmarkSyncService bookmarkSyncService
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
    
    class BookmarkSyncService {
        <<interface>>
        +syncBookmarkCreated(BookmarkCreatedEvent)
        +syncBookmarkUpdated(BookmarkUpdatedEvent)
        +syncBookmarkDeleted(BookmarkDeletedEvent)
        +syncBookmarkRestored(BookmarkRestoredEvent)
    }
    
    class BookmarkSyncServiceImpl {
        -BookmarkRepository repository
        +syncBookmarkCreated(BookmarkCreatedEvent)
        +syncBookmarkUpdated(BookmarkUpdatedEvent)
        +syncBookmarkDeleted(BookmarkDeletedEvent)
        +syncBookmarkRestored(BookmarkRestoredEvent)
        -updateDocumentFields(Document, Map)
        -convertToLocalDateTime(Instant) LocalDateTime
    }
    
    class UserProfileRepository {
        <<interface>>
        +findByUserTsid(String) Optional
        +save(UserProfileDocument)
        +deleteByUserTsid(String)
    }
    
    class BookmarkRepository {
        <<interface>>
        +findByBookmarkTsid(String) Optional
        +save(BookmarkDocument)
        +deleteByBookmarkTsid(String)
    }
    
    class UserProfileDocument {
        +String userTsid
        +String userId
        +String username
        +String email
        +String profileImageUrl
    }
    
    class BookmarkDocument {
        +String bookmarkTsid
        +String userId
        +String itemType
        +ObjectId itemId
        +String itemTitle
        +String itemSummary
        +String tag
        +String memo
    }
    
    BaseEvent <|.. UserCreatedEvent
    BaseEvent <|.. UserUpdatedEvent
    BaseEvent <|.. UserDeletedEvent
    BaseEvent <|.. UserRestoredEvent
    BaseEvent <|.. BookmarkCreatedEvent
    BaseEvent <|.. BookmarkUpdatedEvent
    BaseEvent <|.. BookmarkDeletedEvent
    BaseEvent <|.. BookmarkRestoredEvent
    
    EventConsumer --> UserSyncService : uses
    EventConsumer --> BookmarkSyncService : uses
    EventConsumer --> BaseEvent : consumes
    
    UserSyncService <|.. UserSyncServiceImpl : implements
    BookmarkSyncService <|.. BookmarkSyncServiceImpl : implements
    
    UserSyncServiceImpl --> UserProfileRepository : uses
    BookmarkSyncServiceImpl --> BookmarkRepository : uses
    
    UserProfileRepository --> UserProfileDocument : manages
    BookmarkRepository --> BookmarkDocument : manages
```

#### UserSyncService

**인터페이스 정의**:

```java
package com.ebson.shrimp.tm.demo.common.kafka.sync;

import com.ebson.shrimp.tm.demo.common.kafka.event.*;

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
package com.ebson.shrimp.tm.demo.common.kafka.sync;

import com.ebson.shrimp.tm.demo.common.kafka.event.*;
import com.ebson.shrimp.tm.demo.domain.mongodb.document.UserProfileDocument;
import com.ebson.shrimp.tm.demo.domain.mongodb.repository.UserProfileRepository;
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

#### BookmarkSyncService

**인터페이스 정의**:

```java
package com.ebson.shrimp.tm.demo.common.kafka.sync;

import com.ebson.shrimp.tm.demo.common.kafka.event.*;

/**
 * Bookmark 엔티티 동기화 서비스 인터페이스
 * 
 * Aurora MySQL의 Bookmark 엔티티 변경을 MongoDB Atlas의 BookmarkDocument에 동기화합니다.
 */
public interface BookmarkSyncService {
    
    /**
     * Bookmark 생성 이벤트 동기화
     * 
     * @param event BookmarkCreatedEvent
     */
    void syncBookmarkCreated(BookmarkCreatedEvent event);
    
    /**
     * Bookmark 수정 이벤트 동기화
     * 
     * @param event BookmarkUpdatedEvent
     */
    void syncBookmarkUpdated(BookmarkUpdatedEvent event);
    
    /**
     * Bookmark 삭제 이벤트 동기화 (Soft Delete)
     * MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제 수행
     * 
     * @param event BookmarkDeletedEvent
     */
    void syncBookmarkDeleted(BookmarkDeletedEvent event);
    
    /**
     * Bookmark 복원 이벤트 동기화
     * MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
     * 
     * @param event BookmarkRestoredEvent
     */
    void syncBookmarkRestored(BookmarkRestoredEvent event);
}
```

**구현 클래스 설계**:

```java
package com.ebson.shrimp.tm.demo.common.kafka.sync;

import com.ebson.shrimp.tm.demo.common.kafka.event.*;
import com.ebson.shrimp.tm.demo.domain.mongodb.document.BookmarkDocument;
import com.ebson.shrimp.tm.demo.domain.mongodb.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * Bookmark 동기화 서비스 구현 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkSyncServiceImpl implements BookmarkSyncService {
    
    private final BookmarkRepository bookmarkRepository;
    
    @Override
    public void syncBookmarkCreated(BookmarkCreatedEvent event) {
        try {
            var payload = event.payload();
            
            // Upsert 패턴: bookmarkTsid로 조회하여 없으면 생성, 있으면 업데이트
            BookmarkDocument document = bookmarkRepository
                .findByBookmarkTsid(payload.bookmarkTsid())
                .orElse(new BookmarkDocument());
            
            document.setBookmarkTsid(payload.bookmarkTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setBookmarkedAt(convertToLocalDateTime(payload.bookmarkedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            bookmarkRepository.save(document);
            
            log.debug("Successfully synced BookmarkCreatedEvent: bookmarkTsid={}, userId={}", 
                payload.bookmarkTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync BookmarkCreatedEvent: eventId={}, bookmarkTsid={}", 
                event.eventId(), event.payload().bookmarkTsid(), e);
            throw new RuntimeException("Failed to sync BookmarkCreatedEvent", e);
        }
    }
    
    @Override
    public void syncBookmarkUpdated(BookmarkUpdatedEvent event) {
        try {
            var payload = event.payload();
            var updatedFields = payload.updatedFields();
            
            // bookmarkTsid로 Document 조회
            BookmarkDocument document = bookmarkRepository
                .findByBookmarkTsid(payload.bookmarkTsid())
                .orElseThrow(() -> new RuntimeException(
                    "BookmarkDocument not found: bookmarkTsid=" + payload.bookmarkTsid()));
            
            // updatedFields를 Document 필드에 매핑 (부분 업데이트)
            updateDocumentFields(document, updatedFields);
            document.setUpdatedAt(LocalDateTime.now());
            
            bookmarkRepository.save(document);
            
            log.debug("Successfully synced BookmarkUpdatedEvent: bookmarkTsid={}, updatedFields={}", 
                payload.bookmarkTsid(), updatedFields.keySet());
        } catch (Exception e) {
            log.error("Failed to sync BookmarkUpdatedEvent: eventId={}, bookmarkTsid={}", 
                event.eventId(), event.payload().bookmarkTsid(), e);
            throw new RuntimeException("Failed to sync BookmarkUpdatedEvent", e);
        }
    }
    
    @Override
    public void syncBookmarkDeleted(BookmarkDeletedEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 물리적 삭제
            bookmarkRepository.deleteByBookmarkTsid(payload.bookmarkTsid());
            
            log.debug("Successfully synced BookmarkDeletedEvent: bookmarkTsid={}, userId={}", 
                payload.bookmarkTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync BookmarkDeletedEvent: eventId={}, bookmarkTsid={}", 
                event.eventId(), event.payload().bookmarkTsid(), e);
            throw new RuntimeException("Failed to sync BookmarkDeletedEvent", e);
        }
    }
    
    @Override
    public void syncBookmarkRestored(BookmarkRestoredEvent event) {
        try {
            var payload = event.payload();
            
            // MongoDB는 Soft Delete를 지원하지 않으므로 Document 새로 생성
            BookmarkDocument document = new BookmarkDocument();
            document.setBookmarkTsid(payload.bookmarkTsid());
            document.setUserId(payload.userId());
            document.setItemType(payload.itemType());
            document.setItemId(new ObjectId(payload.itemId()));
            document.setItemTitle(payload.itemTitle());
            document.setItemSummary(payload.itemSummary());
            document.setTag(payload.tag());
            document.setMemo(payload.memo());
            document.setBookmarkedAt(convertToLocalDateTime(payload.bookmarkedAt()));
            document.setCreatedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            
            bookmarkRepository.save(document);
            
            log.debug("Successfully synced BookmarkRestoredEvent: bookmarkTsid={}, userId={}", 
                payload.bookmarkTsid(), payload.userId());
        } catch (Exception e) {
            log.error("Failed to sync BookmarkRestoredEvent: eventId={}, bookmarkTsid={}", 
                event.eventId(), event.payload().bookmarkTsid(), e);
            throw new RuntimeException("Failed to sync BookmarkRestoredEvent", e);
        }
    }
    
    /**
     * updatedFields를 Document 필드에 매핑 (부분 업데이트)
     * 
     * 주의: itemTitle, itemSummary는 BookmarkEntity에 없는 필드이므로
     * BookmarkUpdatedEvent의 updatedFields에 포함될 수 없습니다.
     * 이 필드들은 원본 아이템(ContestDocument/NewsArticleDocument) 변경 시
     * 별도의 동기화 메커니즘으로 업데이트되어야 합니다.
     * 
     * @param document 대상 Document
     * @param updatedFields 업데이트할 필드 맵 (tag, memo만 가능)
     */
    private void updateDocumentFields(BookmarkDocument document, Map<String, Object> updatedFields) {
        for (Map.Entry<String, Object> entry : updatedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            
            switch (fieldName) {
                // itemTitle, itemSummary는 BookmarkEntity에 없는 필드이므로 제외
                // 원본 아이템 변경 시 별도 동기화 메커니즘 필요
                case "tag":
                    document.setTag((String) value);
                    break;
                case "memo":
                    document.setMemo((String) value);
                    break;
                default:
                    log.warn("Unknown field in updatedFields: {} (itemTitle, itemSummary are not supported as they are not in BookmarkEntity)", fieldName);
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
- Upsert 패턴 사용: `findByBookmarkTsid().orElse(new BookmarkDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑
- 타입 변환: `Instant` → `LocalDateTime`, `String` → `ObjectId`
- MongoDB Soft Delete 미지원: 삭제 시 물리적 삭제, 복원 시 새로 생성

#### updatedFields 처리 전략

**부분 업데이트 전략**:
- `Map<String, Object>`의 각 키를 Document 필드명과 매핑
- `switch` 문을 통한 필드별 처리
- 알 수 없는 필드는 경고 로그만 출력 (무시)

**중요 제약사항**:
- **BookmarkUpdatedEvent의 updatedFields에는 BookmarkEntity에 있는 필드만 포함 가능**
  - 지원 필드: `tag`, `memo` (BookmarkEntity에 존재하는 필드)
  - **제외 필드**: `itemTitle`, `itemSummary` (BookmarkEntity에 없는 필드)
- `itemTitle`, `itemSummary`는 비정규화 필드로, 원본 아이템(ContestDocument/NewsArticleDocument)의 정보를 중복 저장
- 원본 아이템 변경 시 별도의 동기화 메커니즘으로 `BookmarkDocument`의 `itemTitle`, `itemSummary`를 업데이트해야 함
- `BookmarkUpdatedEvent`의 `updatedFields`에 `itemTitle`, `itemSummary`가 포함되면 경고 로그만 출력하고 무시

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
    
    EventType -->|BOOKMARK_CREATED| BookmarkCreated[BookmarkSyncService<br/>syncBookmarkCreated]
    EventType -->|BOOKMARK_UPDATED| BookmarkUpdated[BookmarkSyncService<br/>syncBookmarkUpdated]
    EventType -->|BOOKMARK_DELETED| BookmarkDeleted[BookmarkSyncService<br/>syncBookmarkDeleted]
    EventType -->|BOOKMARK_RESTORED| BookmarkRestored[BookmarkSyncService<br/>syncBookmarkRestored]
    
    EventType -->|Unknown| UnknownEvent[경고 로그<br/>무시]
    
    UserCreated --> SyncSuccess{동기화 성공?}
    UserUpdated --> SyncSuccess
    UserDeleted --> SyncSuccess
    UserRestored --> SyncSuccess
    BookmarkCreated --> SyncSuccess
    BookmarkUpdated --> SyncSuccess
    BookmarkDeleted --> SyncSuccess
    BookmarkRestored --> SyncSuccess
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
            case "BOOKMARK_CREATED":
                if (event instanceof BookmarkCreatedEvent bookmarkEvent) {
                    bookmarkSyncService.syncBookmarkCreated(bookmarkEvent);
                }
                break;
            case "BOOKMARK_UPDATED":
                if (event instanceof BookmarkUpdatedEvent bookmarkEvent) {
                    bookmarkSyncService.syncBookmarkUpdated(bookmarkEvent);
                }
                break;
            case "BOOKMARK_DELETED":
                if (event instanceof BookmarkDeletedEvent bookmarkEvent) {
                    bookmarkSyncService.syncBookmarkDeleted(bookmarkEvent);
                }
                break;
            case "BOOKMARK_RESTORED":
                if (event instanceof BookmarkRestoredEvent bookmarkEvent) {
                    bookmarkSyncService.syncBookmarkRestored(bookmarkEvent);
                }
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
    private final BookmarkSyncService bookmarkSyncService;  // 추가
    
    // ... 기존 코드 ...
}
```

**특징**:
- `switch` 문을 통한 이벤트 타입별 분기 처리 (단순하고 명확)
- Pattern Matching for `instanceof` 사용 (Java 16+)
- 예외 발생 시 전파하여 Spring Kafka 재시도 메커니즘 활용

**에러 핸들링**:
- 동기화 실패 시 로깅 및 예외 전파
- Spring Kafka의 기본 재시도 메커니즘 활용
- 최대 재시도 횟수 초과 시 Dead Letter Queue로 이동 (Spring Kafka 기본 동작)

#### updatedFields 처리 흐름 다이어그램

```mermaid
flowchart TD
    Start([UserUpdatedEvent<br/>또는<br/>BookmarkUpdatedEvent]) --> Extract[updatedFields<br/>Map 추출]
    Extract --> Loop{모든 필드<br/>순회}
    Loop -->|다음 필드| CheckField{필드명 확인}
    
    CheckField -->|username| SetUsername[document.setUsername]
    CheckField -->|email| SetEmail[document.setEmail]
    CheckField -->|profileImageUrl| SetProfileImage[document.setProfileImageUrl]
    CheckField -->|itemTitle| WarnLog[경고 로그<br/>BookmarkEntity에 없는 필드<br/>무시]
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
2. `BookmarkSyncService.java` (인터페이스)

**주의사항**:
- 인터페이스는 `public` 접근 제어자 사용
- 각 메서드는 `void` 반환 타입 (비동기 처리)
- 메서드 파라미터는 구체적인 이벤트 타입 사용

### 2. 동기화 서비스 구현 클래스 생성

**파일 생성**:
1. `UserSyncServiceImpl.java` (구현 클래스)
2. `BookmarkSyncServiceImpl.java` (구현 클래스)

**의존성 주입**:
- `@Service` 어노테이션 사용
- `@RequiredArgsConstructor` 사용 (Lombok)
- `UserProfileRepository` 또는 `BookmarkRepository` 주입
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
- 예외 발생 시 전파하여 재시도 메커니즘 활용

### 4. 의존성 주입 설정

**Spring Bean 등록**:
- `@Service` 어노테이션으로 자동 등록
- `@RequiredArgsConstructor`로 생성자 주입

**순환 의존성 방지**:
- `EventConsumer` → `UserSyncService`, `BookmarkSyncService`
- `UserSyncService`, `BookmarkSyncService` → `Repository`
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

**Bookmark 이벤트**:
- ✅ `BookmarkCreatedEvent` → `BookmarkDocument` 생성
- ✅ `BookmarkUpdatedEvent` → `BookmarkDocument` 업데이트
- ✅ `BookmarkDeletedEvent` → `BookmarkDocument` 삭제
- ✅ `BookmarkRestoredEvent` → `BookmarkDocument` 생성

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
1. 동일한 `userTsid` 또는 `bookmarkTsid`로 여러 이벤트 동시 수신
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
   - 이미 업데이트 완료: `api-gateway`

---

**작성 완료 일시**: 2026-01-XX
