# Common Kafka Module

CQRS 패턴 기반 Kafka 이벤트 발행 및 수신 기능을 제공하는 모듈입니다.

## 개요

`common-kafka` 모듈은 CQRS 패턴의 Command Side (Amazon Aurora MySQL)와 Query Side (MongoDB Atlas) 간의 실시간 동기화를 위한 Kafka 기반 이벤트 시스템을 제공합니다. 이 모듈은 이벤트 발행, 이벤트 수신, 멱등성 보장, 동기화 서비스 등을 포함합니다.

## 주요 기능

### 1. 이벤트 발행

Kafka에 이벤트를 발행하는 기능을 제공합니다.

- **EventPublisher**: 이벤트 발행 서비스
- **Partition Key 지원**: 이벤트 순서 보장을 위한 Partition Key 지정
- **비동기 처리**: `CompletableFuture`를 통한 비동기 이벤트 발행

**참고 설계서**: `docs/step11/cqrs-kafka-sync-design.md`

### 2. 이벤트 수신

Kafka에서 이벤트를 수신하고 처리하는 기능을 제공합니다.

- **EventConsumer**: 이벤트 수신 및 처리 서비스
- **멱등성 보장**: Redis 기반 중복 처리 방지
- **수동 커밋**: `Acknowledgment`를 통한 수동 커밋 지원

### 3. 동기화 서비스

이벤트를 MongoDB Atlas에 동기화하는 서비스를 제공합니다.

- **UserSyncService**: User 엔티티 동기화 서비스
- **ArchiveSyncService**: Archive 엔티티 동기화 서비스
- **ConversationSyncService**: Conversation 엔티티 동기화 서비스

### 4. 이벤트 모델

모든 이벤트 타입을 정의하는 이벤트 모델을 제공합니다.

- **BaseEvent**: 모든 이벤트의 기본 인터페이스
- **User 이벤트**: UserCreatedEvent, UserUpdatedEvent, UserDeletedEvent, UserRestoredEvent
- **Archive 이벤트**: ArchiveCreatedEvent, ArchiveUpdatedEvent, ArchiveDeletedEvent, ArchiveRestoredEvent
- **Conversation 이벤트**: ConversationSessionCreatedEvent, ConversationSessionUpdatedEvent, ConversationSessionDeletedEvent, ConversationMessageCreatedEvent

## 주요 컴포넌트

### 이벤트 발행

#### EventPublisher

Kafka에 이벤트를 발행하는 서비스입니다.

```java
@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publish(String topic, BaseEvent event, String partitionKey) {
        // 이벤트 발행 로직
    }
    
    public void publish(String topic, BaseEvent event) {
        // 기본 Partition Key 사용 이벤트 발행
    }
}
```

**사용 예시**:
```java
@Autowired
private EventPublisher eventPublisher;

// Partition Key 지정 이벤트 발행
UserCreatedEvent event = new UserCreatedEvent(payload);
eventPublisher.publish("user-events", event, userId);

// 기본 Partition Key 사용 이벤트 발행
eventPublisher.publish("user-events", event);
```

**주요 기능**:
- Partition Key 지원: 이벤트 순서 보장을 위한 Partition Key 지정
- 비동기 처리: `CompletableFuture`를 통한 비동기 이벤트 발행
- 에러 핸들링: 예외 발생 시 로깅 및 `RuntimeException` 전파
- 로깅: 성공/실패 로그 출력

### 이벤트 수신

#### EventConsumer

Kafka에서 이벤트를 수신하고 처리하는 서비스입니다.

```java
@Service
@RequiredArgsConstructor
public class EventConsumer {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final UserSyncService userSyncService;
    private final ArchiveSyncService archiveSyncService;
    private final ConversationSyncService conversationSyncService;
    
    @KafkaListener(
        topics = "${spring.kafka.consumer.topics:user-events,archive-events,contest-events,news-events,conversation-events}",
        groupId = "${spring.kafka.consumer.group-id:shrimp-task-manager-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
        @Payload BaseEvent event,
        Acknowledgment acknowledgment,
        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
        @Header(KafkaHeaders.OFFSET) long offset
    ) {
        // 이벤트 수신 및 처리 로직
    }
}
```

**주요 기능**:
- 멱등성 보장: Redis 기반 중복 처리 방지
- 수동 커밋: `Acknowledgment.acknowledge()`를 통한 수동 커밋
- 에러 핸들링: 예외 발생 시 로깅 및 예외 전파 (Spring Kafka 재시도 활용)
- 이벤트 타입별 분기 처리: 이벤트 타입에 따라 적절한 동기화 서비스 호출

### 동기화 서비스

#### UserSyncService

User 엔티티를 MongoDB Atlas에 동기화하는 서비스입니다.

```java
public interface UserSyncService {
    void syncUserCreated(UserCreatedEvent event);
    void syncUserUpdated(UserUpdatedEvent event);
    void syncUserDeleted(UserDeletedEvent event);
    void syncUserRestored(UserRestoredEvent event);
}
```

**주요 기능**:
- Upsert 패턴: `findByUserTsid().orElse(new UserProfileDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑
- MongoDB Soft Delete 미지원: 삭제 시 물리적 삭제, 복원 시 새로 생성

#### ArchiveSyncService

Archive 엔티티를 MongoDB Atlas에 동기화하는 서비스입니다.

```java
public interface ArchiveSyncService {
    void syncArchiveCreated(ArchiveCreatedEvent event);
    void syncArchiveUpdated(ArchiveUpdatedEvent event);
    void syncArchiveDeleted(ArchiveDeletedEvent event);
    void syncArchiveRestored(ArchiveRestoredEvent event);
}
```

**주요 기능**:
- Upsert 패턴: `findByArchiveTsid().orElse(new ArchiveDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑 (tag, memo만 지원)
- 타입 변환: `Instant` → `LocalDateTime`, `String` → `ObjectId`

#### ConversationSyncService

Conversation 엔티티를 MongoDB Atlas에 동기화하는 서비스입니다.

```java
public interface ConversationSyncService {
    void syncSessionCreated(ConversationSessionCreatedEvent event);
    void syncSessionUpdated(ConversationSessionUpdatedEvent event);
    void syncSessionDeleted(ConversationSessionDeletedEvent event);
    void syncMessageCreated(ConversationMessageCreatedEvent event);
}
```

**주요 기능**:
- Upsert 패턴: `findBySessionId().orElse(new ConversationSessionDocument())`로 생성/수정 통합
- 부분 업데이트: `updatedFields`를 Document 필드에 매핑 (title, lastMessageAt, isActive만 지원)
- 타입 변환: `Instant` → `LocalDateTime`, `Long` → `String` (userId)

### 이벤트 모델

#### BaseEvent

모든 이벤트의 기본 인터페이스입니다.

```java
public interface BaseEvent {
    String eventId();      // UUID 형식
    String eventType();    // 이벤트 타입 문자열
    Instant timestamp();   // 이벤트 발생 시각
}
```

#### User 이벤트

- **UserCreatedEvent**: User 생성 이벤트
- **UserUpdatedEvent**: User 수정 이벤트 (updatedFields 포함)
- **UserDeletedEvent**: User 삭제 이벤트
- **UserRestoredEvent**: User 복원 이벤트

#### Archive 이벤트

- **ArchiveCreatedEvent**: Archive 생성 이벤트
- **ArchiveUpdatedEvent**: Archive 수정 이벤트 (updatedFields 포함, tag, memo만 지원)
- **ArchiveDeletedEvent**: Archive 삭제 이벤트
- **ArchiveRestoredEvent**: Archive 복원 이벤트

#### Conversation 이벤트

- **ConversationSessionCreatedEvent**: ConversationSession 생성 이벤트
- **ConversationSessionUpdatedEvent**: ConversationSession 수정 이벤트 (updatedFields 포함)
- **ConversationSessionDeletedEvent**: ConversationSession 삭제 이벤트
- **ConversationMessageCreatedEvent**: ConversationMessage 생성 이벤트

## 의존성

### 주요 의존성

- **common-core**: 공통 핵심 모듈 (BaseException, ApiResponse 등)
- **domain-mongodb**: MongoDB 도메인 모듈 (동기화 대상)
- **Spring Boot Starter Kafka**: Kafka 연동
- **Apache Kafka Streams**: Kafka Streams 지원

## 사용 방법

### 1. 의존성 추가

다른 모듈에서 `common-kafka` 모듈을 사용하려면 `build.gradle`에 다음을 추가합니다:

```gradle
dependencies {
    implementation project(':common-kafka')
}
```

### 2. 이벤트 발행

비즈니스 로직에서 이벤트를 발행합니다:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final EventPublisher eventPublisher;
    
    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = userRepository.save(new User(request));
        
        // 이벤트 발행
        UserCreatedEvent.UserCreatedPayload payload = new UserCreatedEvent.UserCreatedPayload(
            String.valueOf(user.getId()),
            String.valueOf(user.getId()),
            user.getUsername(),
            user.getEmail(),
            user.getProfileImageUrl()
        );
        UserCreatedEvent event = new UserCreatedEvent(payload);
        eventPublisher.publish("user-events", event, String.valueOf(user.getId()));
        
        return user;
    }
}
```

### 3. 이벤트 수신

`EventConsumer`가 자동으로 이벤트를 수신하고 처리합니다. 별도의 설정이 필요하지 않습니다.

### 4. 동기화 서비스 구현

동기화 서비스는 이미 구현되어 있으며, `EventConsumer`가 자동으로 호출합니다.

## 멱등성 보장

`EventConsumer`는 Redis를 사용하여 이벤트 중복 처리를 방지합니다.

**동작 방식**:
1. 이벤트 수신 시 `eventId`를 기반으로 Redis에서 처리 여부 확인
2. 이미 처리된 이벤트인 경우 스킵
3. 처리되지 않은 이벤트인 경우 처리 후 Redis에 저장 (TTL: 7일)

**Redis Key 형식**:
```
processed_event:{eventId}
```

## 이벤트 처리 흐름

1. **이벤트 발행**: Command Side에서 이벤트 발행
2. **Kafka 저장**: Kafka에 이벤트 저장
3. **이벤트 수신**: `EventConsumer`가 이벤트 수신
4. **멱등성 확인**: Redis에서 처리 여부 확인
5. **이벤트 처리**: 이벤트 타입에 따라 적절한 동기화 서비스 호출
6. **MongoDB 동기화**: MongoDB Atlas에 Document 저장/수정/삭제
7. **처리 완료 표시**: Redis에 처리 완료 표시
8. **커밋**: Kafka offset 커밋

## 설정

### application.yml

Kafka 설정을 추가합니다:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: shrimp-task-manager-group
      topics: user-events,archive-events,contest-events,news-events,conversation-events
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

## 참고 자료

### 설계서

- `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계서

### 공식 문서

- [Spring Kafka 공식 문서](https://docs.spring.io/spring-kafka/reference/html/)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Apache Kafka Producer API 공식 문서](https://kafka.apache.org/documentation/#producerapi)
- [Apache Kafka Consumer API 공식 문서](https://kafka.apache.org/documentation/#consumerapi)

---

**작성일**: 2026-01-XX  
**버전**: 0.0.1-SNAPSHOT

