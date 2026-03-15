# Kafka Docker Compose 로컬 개발 환경 구축 가이드

## 1. 개요

### 1.1 목적 및 범위
로컬 개발 환경에서 Apache Kafka를 Docker Compose로 구축하고 Spring Boot 프로젝트와 연동하여 이벤트 기반 아키텍처를 개발 및 테스트합니다.

### 1.2 현재 구현된 이벤트

이 가이드는 다음 이벤트들의 처리를 다룹니다:

| 이벤트 타입 | Kafka Topic | 용도 |
|------------|-------------|------|
| `CONVERSATION_SESSION_CREATED` | `shrimp-tm.conversation.session.created` | 대화 세션 생성 |
| `CONVERSATION_SESSION_UPDATED` | `shrimp-tm.conversation.session.updated` | 대화 세션 수정 |
| `CONVERSATION_SESSION_DELETED` | `shrimp-tm.conversation.session.deleted` | 대화 세션 삭제 |
| `CONVERSATION_MESSAGE_CREATED` | `shrimp-tm.conversation.message.created` | 대화 메시지 생성 |

**아키텍처 특징**:
- EventHandler 패턴을 사용한 이벤트 처리
- Redis 기반 멱등성 보장
- MongoDB Atlas 동기화 (CQRS Query Side)
- 수동 커밋을 통한 메시지 처리 보장

### 1.3 사전 요구사항
- Docker: 20.10 이상
- Docker Compose: 2.0 이상
- JDK: 17 이상
- Gradle: 8.x

### 1.4 기술 스택
- Apache Kafka 4.1.1 (KRaft 모드)
- Spring Boot 3.x
- Spring Kafka
- Redis (멱등성 보장용)
- Kafka UI (provectuslabs/kafka-ui)치

## 2. Docker Compose 설정

### 2.1 docker-compose.yml

프로젝트 루트에 `docker-compose.yml` 파일 생성:

```yaml
version: '3.8'

services:
  kafka:
    image: apache/kafka:4.1.1
    container_name: kafka-local
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      # KRaft 모드 설정
      KAFKA_PROCESS_ROLES: controller,broker
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      
      # 리스너 설정
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      
      # 로컬 개발 환경 최적화
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_NUM_PARTITIONS: 3
      
      # 로그 설정
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - kafka-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions.sh --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      DYNAMIC_CONFIG_ENABLED: 'true'
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - kafka-network
    restart: unless-stopped

networks:
  kafka-network:
    driver: bridge

volumes:
  kafka-data:
    driver: local
```

### 2.2 설정 설명

#### Kafka 브로커
- **KRaft 모드**: ZooKeeper 없이 Kafka 내부 컨트롤러 사용
- **포트**:
  - 9092: 클라이언트 연결 (PLAINTEXT)
  - 9093: 컨트롤러 통신 (내부)
- **리스너**:
  - `PLAINTEXT://0.0.0.0:9092`: 모든 인터페이스에서 수신
  - `ADVERTISED_LISTENERS`: 클라이언트에게 알리는 주소 (localhost:9092)
- **볼륨**: `/var/lib/kafka/data` 마운트로 데이터 영속성 보장
- **헬스체크**: `kafka-broker-api-versions.sh` 명령어로 브로커 상태 확인

#### Kafka UI
- **포트**: 8080
- **기능**: Topic 생성/조회, 메시지 조회, Consumer Group 모니터링

### 2.3 실행 및 종료

#### 시작
```bash
docker compose up -d
```

#### 로그 확인
```bash
docker compose logs -f kafka
```

#### 상태 확인
```bash
docker compose ps
```

#### 종료
```bash
docker compose down
```

#### 데이터 삭제 후 재시작
```bash
docker compose down -v
docker compose up -d
```

### 2.4 서비스 연결 확인

#### Kafka 연결 확인
```bash
# Topic 목록 조회
docker exec kafka-local kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list

# Kafka 브로커 정보 확인
docker exec kafka-local kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092
```

#### Kafka UI 접속
```bash
# 브라우저에서 접속
open http://localhost:8080
```

**확인 사항**:
- Clusters 목록에 "local" 클러스터가 표시되어야 함
- Brokers 탭에서 브로커 상태 확인 가능

#### Redis 연결 확인

**참고**: Redis는 로컬 환경에서 Docker를 사용하지 않고 터미널에서 직접 실행 중입니다.

```bash
# Redis CLI 접속
redis-cli

# PING 명령어 테스트
127.0.0.1:6379> PING
PONG

# 정보 확인
127.0.0.1:6379> INFO server

# 종료
127.0.0.1:6379> exit
```

**원라인 테스트**:
```bash
# PING 테스트
redis-cli ping

# 키 저장 및 조회 테스트
redis-cli SET test_key "hello"
redis-cli GET test_key
redis-cli DEL test_key
```

#### 모든 서비스 상태 확인
```bash
docker compose ps
```

**정상 상태 출력 예시**:
```
NAME          IMAGE                           STATUS         PORTS
kafka-local   apache/kafka:4.1.1              Up (healthy)   0.0.0.0:9092-9093->9092-9093/tcp
kafka-ui      provectuslabs/kafka-ui:latest   Up             0.0.0.0:8080->8080/tcp
```

**참고**: Redis는 Docker가 아닌 로컬에서 직접 실행 중이므로 위 목록에 표시되지 않습니다. Redis 상태는 `redis-cli ping` 명령어로 확인하세요.

## 3. 모듈 의존성 설정

각 API 모듈에서 Kafka를 사용하려면 `common-kafka` 의존성을 추가해야 합니다.

### 3.1 build.gradle 의존성 추가

예시 (`api/contest/build.gradle`):

```gradle
dependencies {
    implementation project(':common-core')
    implementation project(':common-exception')
    implementation project(':common-kafka')  // Kafka 의존성 추가
    implementation project(':domain-mongodb')
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

**이미 의존성이 추가된 모듈**:
- `api-gateway`
- `api-chatbot` ✅ Conversation 이벤트 발행 및 ConversationSyncService 사용
- `batch-source`

**의존성 추가 필요 모듈** (필요시):
- `api-contest` ✅ 추가됨
- `api-news` ✅ 추가됨
- `api-auth` (필요시 추가)
- `api-bookmark` (필요시 추가)

## 4. Spring Boot 연동

### 4.1 Kafka 설정

#### 공통 Kafka 설정 (common-kafka 모듈)

`common/kafka/src/main/resources/application-kafka.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    
    # Producer 설정
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: 1
      retries: 3
      batch-size: 16384
      linger-ms: 10
      buffer-memory: 33554432
      properties:
        max.in.flight.requests.per.connection: 5
        enable.idempotence: true
    
    # Consumer 설정
    consumer:
      group-id: ${spring.application.name:shrimp-tm-group}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      properties:
        spring.json.trusted.packages: com.ebson.shrimp.tm.demo.common.kafka.event,com.ebson.shrimp.tm.demo.*.event
        isolation.level: read_committed
        max.poll.interval.ms: 600000
      # Consumer가 구독할 Topic 목록
      topics: shrimp-tm.conversation.session.created,shrimp-tm.conversation.session.updated,shrimp-tm.conversation.session.deleted,shrimp-tm.conversation.message.created
    
    # Listener 설정
    listener:
      ack-mode: manual
      concurrency: 3
      missing-topics-fatal: false
    
    # Admin 설정
    admin:
      auto-create: false

logging:
  level:
    org.apache.kafka: INFO
    org.springframework.kafka: INFO
```

#### 각 모듈에서 Kafka 설정 활성화

각 API 모듈의 `application.yml`에 다음과 같이 kafka profile 포함:

```yaml
spring:
  profiles:
    include:
      - common-core
      - kafka  # Kafka 설정 활성화
      - mongodb-domain
```

### 4.2 Kafka Configuration 클래스

`common/kafka` 모듈에는 Kafka 설정을 위한 Configuration 클래스가 포함되어 있습니다:

`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/config/KafkaConfig.java`

이 클래스는 다음 Bean들을 제공합니다:
- `ProducerFactory<String, Object>`: Kafka Producer 팩토리
- `KafkaTemplate<String, Object>`: 메시지 발행용 템플릿
- `ConsumerFactory<String, Object>`: Kafka Consumer 팩토리
- `ConcurrentKafkaListenerContainerFactory`: Listener 컨테이너 팩토리

**자동 구성**: `@EnableKafka` 어노테이션과 함께 Spring Boot Auto-configuration과 조합하여 동작하며, `application-kafka.yml`의 설정값을 읽어 Bean을 구성합니다.

### 4.3 설정 설명

#### Producer
- **acks=1**: 리더 파티션 응답만 대기 (로컬 개발 최적)
- **retries=3**: 전송 실패 시 재시도
- **batch-size**: 배치 크기 (16KB)
- **linger.ms**: 배치 대기 시간 (10ms)
- **enable.idempotence**: 중복 메시지 방지

#### Consumer
- **group-id**: 애플리케이션 이름 사용 (기본값: `shrimp-tm-group`)
- **auto-offset-reset=earliest**: Topic 없을 때 처음부터 읽기
- **enable-auto-commit=false**: 수동 커밋 (메시지 처리 보장)
- **trusted.packages**: 역직렬화 허용 패키지 (와일드카드 패턴 지원)
- **isolation.level=read_committed**: 트랜잭션 커밋된 메시지만 읽기
- **max.poll.interval.ms**: Consumer 처리 시간 제한 (기본 10분)
- **topics**: Consumer가 구독할 Topic 목록 (쉼표로 구분)

#### Listener
- **ack-mode=manual**: 수동 ACK
- **concurrency=3**: Consumer 스레드 수
- **missing-topics-fatal=false**: Topic 없어도 시작

## 5. Redis 연동 및 멱등성 보장

### 5.1 Redis 개요

Kafka Consumer가 동일한 이벤트를 중복으로 처리하는 것을 방지하기 위해 Redis를 사용한 멱등성(Idempotency) 보장 메커니즘을 구현합니다.

#### 멱등성이 필요한 이유

Kafka는 **at-least-once** 전달을 보장하므로, 다음과 같은 상황에서 동일한 메시지가 중복 처리될 수 있습니다:

1. **Consumer 재시작**: 오프셋 커밋 전에 Consumer가 종료된 경우
2. **네트워크 지연**: ACK가 늦게 도착하여 재전송된 경우
3. **Rebalancing**: Consumer Group 재조정 시 메시지 재처리
4. **수동 오프셋 리셋**: 개발 환경에서 오프셋을 초기화한 경우

#### 해결 방법

Redis에 이벤트 ID를 저장하여 이미 처리된 이벤트인지 확인:

```
이벤트 수신 → Redis에서 eventId 확인 
  → 존재하면 Skip (이미 처리됨)
  → 존재하지 않으면 처리 후 Redis에 eventId 저장
```

### 5.2 Spring Data Redis 의존성

`common/kafka/build.gradle`에 Redis 의존성이 포함되어 있어야 합니다:

```gradle
dependencies {
    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'
    
    // 기타 의존성...
}
```

### 5.3 Redis 연결 설정

#### application-kafka.yml에 Redis 설정 추가

```yaml
spring:
  # Redis 설정
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 10
          min-idle: 2
          max-wait: 3000ms
```

#### 환경별 설정

**로컬 개발 환경** (`application-local.yml`):
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**운영 환경** (`application-prod.yml`):
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:redis-prod.example.com}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl: true
```

### 5.4 IdempotencyService 구현

#### 인터페이스

`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/IdempotencyService.java`

```java
public interface IdempotencyService {
    /**
     * 이벤트가 이미 처리되었는지 확인
     * @param eventId 이벤트 고유 ID
     * @return 이미 처리된 경우 true, 처음 처리하는 경우 false
     */
    boolean isProcessed(String eventId);
    
    /**
     * 이벤트를 처리 완료로 표시
     * @param eventId 이벤트 고유 ID
     * @param ttlDays TTL (일 단위)
     */
    void markAsProcessed(String eventId, long ttlDays);
}
```

#### 구현체

`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/IdempotencyServiceImpl.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {
    
    private static final String KEY_PREFIX = "processed_event:";
    private static final long DEFAULT_TTL_DAYS = 7L;
    
    private final StringRedisTemplate redisTemplate;
    
    @Override
    public boolean isProcessed(String eventId) {
        String key = KEY_PREFIX + eventId;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Event already processed: eventId={}", eventId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public void markAsProcessed(String eventId, long ttlDays) {
        String key = KEY_PREFIX + eventId;
        redisTemplate.opsForValue().set(
            key, 
            String.valueOf(System.currentTimeMillis()),
            Duration.ofDays(ttlDays > 0 ? ttlDays : DEFAULT_TTL_DAYS)
        );
        log.debug("Marked event as processed: eventId={}, ttl={}days", eventId, ttlDays);
    }
}
```

**핵심 포인트**:
- **Key Prefix**: `processed_event:{eventId}` 형식으로 저장
- **Value**: 처리 시각 타임스탬프 (디버깅용)
- **TTL**: 기본 7일, 이후 자동 삭제 (메모리 절약)

### 5.5 EventConsumer에서 멱등성 보장 사용

`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class EventConsumer {
    
    private final IdempotencyService idempotencyService;
    private final EventHandlerRegistry eventHandlerRegistry;
    
    @KafkaListener(
        topics = {
            "shrimp-tm.conversation.session.created",
            "shrimp-tm.conversation.session.updated",
            "shrimp-tm.conversation.session.deleted",
            "shrimp-tm.conversation.message.created"
        },
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, BaseEvent> record, Acknowledgment ack) {
        BaseEvent event = record.value();
        String eventId = event.eventId();
        
        try {
            // 1. 멱등성 체크
            if (idempotencyService.isProcessed(eventId)) {
                log.info("Skipping already processed event: eventId={}", eventId);
                ack.acknowledge();
                return;
            }
            
            // 2. 이벤트 처리
            log.info("Consuming event: eventId={}, eventType={}, topic={}", 
                eventId, event.eventType(), record.topic());
            
            eventHandlerRegistry.handle(event);
            
            // 3. 처리 완료 표시
            idempotencyService.markAsProcessed(eventId, 7L);
            
            // 4. 수동 커밋
            ack.acknowledge();
            
            log.info("Successfully processed event: eventId={}, eventType={}", 
                eventId, event.eventType());
            
        } catch (Exception e) {
            log.error("Failed to process event: eventId={}, eventType={}, error={}", 
                eventId, event.eventType(), e.getMessage(), e);
            throw e; // Spring Kafka 재시도 메커니즘 활용
        }
    }
}
```

**처리 흐름**:
1. **멱등성 체크**: Redis에서 `eventId` 존재 여부 확인
2. **이미 처리됨**: Skip 후 ACK (중복 처리 방지)
3. **처음 처리**: EventHandler 실행 → Redis에 저장 → ACK
4. **예외 발생**: 예외 전파 → Spring Kafka 재시도 (Redis에 저장 안 됨)

### 5.6 Redis 키 관리

#### 키 네이밍 규칙

```
processed_event:{eventId}
```

**예시**:
```
processed_event:a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

#### TTL 정책

- **기본 TTL**: 7일
- **이유**: 
  - Kafka 메시지 보관 기간(168시간 = 7일)과 동일
  - 오래된 이벤트 ID는 자동 삭제되어 메모리 절약
  - 7일 이내에는 중복 처리 완벽 방지

#### 메모리 사용량 추정

**가정**:
- 이벤트 ID: UUID (36자) + Key Prefix (16자) = 약 52바이트
- Value: 타임스탬프 (13자) = 약 13바이트
- Redis 오버헤드: 약 90바이트
- **키당 총 메모리**: 약 155바이트

**일일 이벤트 예상**:
- 1,000 events/day × 155 bytes ≈ 155 KB/day
- 7일 보관 시: 155 KB × 7 ≈ **1.1 MB**

**10,000 events/day**:
- 7일 보관 시: **11 MB**

→ 로컬 개발 환경에서는 256MB Redis로 충분

### 5.7 Redis 명령어로 멱등성 확인

**참고**: Redis는 로컬 환경에서 Docker를 사용하지 않고 터미널에서 직접 실행 중입니다.

#### 처리된 이벤트 조회

```bash
# Redis CLI 접속
redis-cli

# 모든 processed_event 키 조회
127.0.0.1:6379> KEYS processed_event:*

# 특정 이벤트 ID 확인
127.0.0.1:6379> GET processed_event:a1b2c3d4-e5f6-7890-abcd-ef1234567890

# TTL 확인 (남은 시간, 초 단위)
127.0.0.1:6379> TTL processed_event:a1b2c3d4-e5f6-7890-abcd-ef1234567890

# 모든 키 개수 확인
127.0.0.1:6379> DBSIZE
```

#### 테스트용 데이터 삭제

```bash
# 특정 이벤트 ID 삭제
127.0.0.1:6379> DEL processed_event:a1b2c3d4-e5f6-7890-abcd-ef1234567890

# 모든 processed_event 키 삭제
127.0.0.1:6379> EVAL "return redis.call('del', unpack(redis.call('keys', ARGV[1])))" 0 processed_event:*

# 전체 데이터베이스 초기화 (주의!)
127.0.0.1:6379> FLUSHDB
```

### 5.8 멱등성 테스트 시나리오

#### 1. 정상 처리 테스트

```bash
# 1. 이벤트 발행 (Conversation 세션 생성)
curl -X POST http://localhost:8080/api/v1/chatbot/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "Test Session"}'

# 2. EventConsumer 로그 확인
# "Successfully processed event: eventId=xxx"

# 3. Redis에서 확인
redis-cli GET processed_event:xxx

# 4. MongoDB에서 동기화 확인
# ConversationSessionDocument가 생성되었는지 확인
```

#### 2. 중복 처리 방지 테스트

```bash
# 1. Consumer Group 오프셋 리셋 (애플리케이션 중지 후)
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group shrimp-tm-group \
  --reset-offsets \
  --to-earliest \
  --topic shrimp-tm.conversation.session.created \
  --execute

# 2. 애플리케이션 재시작

# 3. 로그 확인
# "Skipping already processed event: eventId=xxx" 출력 확인
# → 중복 처리되지 않음 ✅
```

#### 3. Redis 장애 시나리오 테스트

**참고**: Redis는 로컬 환경에서 Docker를 사용하지 않고 터미널에서 직접 실행 중입니다.

```bash
# 1. Redis 중지 (터미널에서 Ctrl+C 또는 redis-cli SHUTDOWN)
redis-cli SHUTDOWN

# 2. 이벤트 발행 시도
# → RedisConnectionFailureException 발생
# → EventConsumer에서 예외 발생
# → Kafka 메시지 재시도

# 3. Redis 재시작 (터미널에서 redis-server 실행)
redis-server

# 4. 재시도 성공 확인
```

### 5.9 Redis 모니터링

**참고**: Redis는 로컬 환경에서 Docker를 사용하지 않고 터미널에서 직접 실행 중입니다.

#### 실시간 명령어 모니터링

```bash
# Redis에 실행되는 모든 명령어 실시간 모니터링
redis-cli MONITOR
```

**출력 예시**:
```
1706000000.123456 [0 127.0.0.1:12345] "EXISTS" "processed_event:abc123"
1706000000.456789 [0 127.0.0.1:12345] "SETEX" "processed_event:abc123" "604800" "1706000000456"
```

#### Redis 정보 확인

```bash
# 메모리 사용량
redis-cli INFO memory

# 키 통계
redis-cli INFO keyspace

# 전체 정보
redis-cli INFO all
```

#### Spring Boot Actuator를 통한 모니터링

`application.yml`에 Redis Health Indicator 활성화:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  health:
    redis:
      enabled: true
```

**Health 체크**:
```bash
curl http://localhost:8080/actuator/health
```

**출력 예시**:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.2.4"
      }
    }
  }
}
```

### 5.10 Redis 운영 고려사항

#### 로컬 개발 환경
- ✅ 터미널에서 직접 실행 중 (Docker 미사용)
- ✅ 기본 설정 사용 (메모리 제한 없음 또는 시스템 기본값)
- ✅ LRU 정책: 메모리 가득 차면 오래된 키 자동 삭제 (설정 시)
- ✅ AOF/RDB 영속성: 재시작 시 데이터 유지 (설정 시)

#### 운영 환경 권장사항
- **AWS ElastiCache for Redis** 또는 **Redis Enterprise Cloud** 사용
- **고가용성**: Multi-AZ, Replication
- **메모리**: 최소 1GB 이상 (트래픽에 따라 조정)
- **모니터링**: CloudWatch 또는 Redis Insight
- **백업**: 일일 스냅샷
- **보안**: VPC 내부 배치, 비밀번호 인증

## 6. Topic 설계

### 5.1 Topic 생성 스크립트

`scripts/create-topics.sh` 생성:

```bash
#!/bin/bash

KAFKA_CONTAINER="kafka-local"
BOOTSTRAP_SERVER="localhost:9092"

# Topic 생성 함수
create_topic() {
  local topic_name=$1
  local partitions=$2
  local replication=$3
  
  docker exec $KAFKA_CONTAINER kafka-topics.sh \
    --create \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --topic $topic_name \
    --partitions $partitions \
    --replication-factor $replication \
    --if-not-exists \
    --config retention.ms=604800000 \
    --config segment.ms=86400000
  
  echo "Topic created: $topic_name"
}

# Topic 목록
create_topic "shrimp-tm.conversation.session.created" 3 1
create_topic "shrimp-tm.conversation.session.updated" 3 1
create_topic "shrimp-tm.conversation.session.deleted" 3 1
create_topic "shrimp-tm.conversation.message.created" 3 1

echo "All topics created successfully"
```

실행 권한 부여 및 실행:
```bash
chmod +x scripts/create-topics.sh
./scripts/create-topics.sh
```

### 5.2 Topic 목록

| Topic 이름 | Event 클래스 | Partition | Replication | Retention |
|-----------|-------------|-----------|-------------|-----------|
| `shrimp-tm.conversation.session.created` | ConversationSessionCreatedEvent | 3 | 1 | 7일 |
| `shrimp-tm.conversation.session.updated` | ConversationSessionUpdatedEvent | 3 | 1 | 7일 |
| `shrimp-tm.conversation.session.deleted` | ConversationSessionDeletedEvent | 3 | 1 | 7일 |
| `shrimp-tm.conversation.message.created` | ConversationMessageCreatedEvent | 3 | 1 | 7일 |

#### 명명 규칙
- 형식: `{project}.{domain}.{action}`
- 소문자, 점(`.`) 구분
- 과거형 동사 사용 (synced, created, updated, deleted)

### 5.3 Topic 조회

모든 Topic 조회:
```bash
docker exec kafka-local kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092
```

특정 Topic 상세 정보:
```bash
docker exec kafka-local kafka-topics.sh \
  --describe \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created
```

## 7. 개발/테스트 워크플로우

### 6.1 환경 시작

1. **Docker Compose 시작**
```bash
docker compose up -d
```

2. **Kafka 브로커 준비 대기**
```bash
docker compose logs kafka | grep "Kafka Server started"
```

3. **Topic 생성**
```bash
./scripts/create-topics.sh
```

4. **Spring Boot 애플리케이션 시작**
```bash
# Gateway API
./gradlew :api-gateway:bootRun --args='--spring.profiles.active=local'

# Bookmark API (다른 터미널)
./gradlew :api-bookmark:bootRun --args='--spring.profiles.active=local'
```

### 6.2 동작 확인

#### Kafka 브로커 상태
```bash
docker exec kafka-local kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092
```

#### Topic 목록 확인
```bash
docker exec kafka-local kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092
```

#### Consumer Group 확인
```bash
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

#### Consumer Group 상세
```bash
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group gateway-api \
  --describe
```

### 6.3 메시지 테스트

#### Producer 테스트 (콘솔)
```bash
docker exec -it kafka-local kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created
```

메시지 입력:
```json
{"eventId":"test-001","timestamp":"2026-01-21T10:00:00Z","eventType":"CONVERSATION_SESSION_CREATED"}
```

#### Consumer 테스트 (콘솔)
```bash
docker exec -it kafka-local kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created \
  --from-beginning
```

#### 애플리케이션 코드로 Producer 테스트

`EventPublisher` 사용 예시:
```java
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class KafkaTestController {
    
    private final EventPublisher eventPublisher;
    
    @PostMapping("/publish/conversation-session")
    public ResponseEntity<String> publishConversationSessionEvent() {
        // Payload 생성
        ConversationSessionCreatedEvent.ConversationSessionCreatedPayload payload = 
            new ConversationSessionCreatedEvent.ConversationSessionCreatedPayload(
                "session-123",           // sessionId
                "user-456",              // userId
                "New Chat Session",      // title
                Instant.now(),           // lastMessageAt
                true                     // isActive
            );
        
        // Event 생성 (eventId, eventType, timestamp는 자동 생성)
        ConversationSessionCreatedEvent event = new ConversationSessionCreatedEvent(payload);
        
        // 이벤트 발행
        eventPublisher.publish("shrimp-tm.conversation.session.created", event);
        
        return ResponseEntity.ok("Event published: " + event.eventId());
    }
}
```

**중요 사항**:
- Event 클래스는 `record` 타입으로 구현되어 있어 Builder 패턴을 사용하지 않습니다
- `eventId`, `eventType`, `timestamp`는 생성자에서 자동으로 생성됩니다
- `Instant` 타입을 사용합니다 (LocalDateTime이 아님)

#### 애플리케이션 코드로 Consumer 테스트

`EventConsumer`의 `@KafkaListener` 로그 확인:
```bash
# 애플리케이션 로그에서 이벤트 처리 확인
tail -f logs/application.log | grep "Successfully processed event"

# 또는 에러 확인
tail -f logs/application.log | grep "Error processing event"

# 멱등성 체크 로그 확인
tail -f logs/application.log | grep "Event already processed"
```

**로그 예시**:
```
Successfully processed event: eventId=550e8400-e29b-41d4-a716-446655440000, eventType=CONVERSATION_SESSION_CREATED, partition=0, offset=100
Event already processed, skipping: eventId=550e8400-e29b-41d4-a716-446655440000, eventType=CONVERSATION_SESSION_CREATED, partition=0, offset=101
```

## 8. 모니터링

### 7.1 Kafka UI 사용

#### 접속
브라우저에서 `http://localhost:8080` 접속

#### 주요 기능
1. **Topics**: Topic 목록 및 메시지 조회
2. **Consumers**: Consumer Group 및 Lag 모니터링
3. **Brokers**: 브로커 상태 및 설정 확인
4. **Schema Registry**: (사용 시)

#### Topic 메시지 조회
1. Topics 메뉴 선택
2. 원하는 Topic 클릭
3. Messages 탭에서 메시지 확인

#### Consumer Group Lag 확인
1. Consumers 메뉴 선택
2. Consumer Group 선택
3. Lag 및 Offset 확인

### 7.2 명령줄 모니터링

#### 메시지 개수 확인
```bash
docker exec kafka-local kafka-run-class.sh kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic shrimp-tm.conversation.session.created \
  --time -1
```

#### Consumer Lag 확인
```bash
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group shrimp-tm-group \
  --describe
```

출력 예시:
```
GROUP           TOPIC                                      PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
shrimp-tm-group shrimp-tm.conversation.session.created     0          100             100             0
shrimp-tm-group shrimp-tm.conversation.session.created     1          98              100             2
shrimp-tm-group shrimp-tm.conversation.session.created     2          99              100             1
```

## 9. 트러블슈팅

### 8.1 일반적인 문제

#### 1. Connection refused: localhost:9092

**증상**:
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms
```

**원인**: Kafka 브로커가 시작되지 않았거나 리스너 설정 오류

**해결**:
```bash
# 브로커 상태 확인
docker compose ps

# 로그 확인
docker compose logs kafka

# 헬스체크 확인
docker exec kafka-local kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# 재시작
docker compose restart kafka
```

#### 2. Offset commit failed

**증상**:
```
Commit cannot be completed since the group has already rebalanced
```

**원인**: Consumer 처리 시간이 `max.poll.interval.ms` 초과

**해결**:
`application-kafka.yml`에 이미 설정되어 있지만, 필요시 각 모듈에서 오버라이드 가능:
```yaml
spring:
  kafka:
    consumer:
      properties:
        max.poll.interval.ms: 600000  # 10분 (기본값)
        max.poll.records: 100  # 한 번에 가져오는 레코드 수 감소
```

#### 3. Serialization/Deserialization 에러

**증상**:
```
org.springframework.kafka.support.serializer.DeserializationException: 
failed to deserialize; nested exception is com.fasterxml.jackson.databind.exc.InvalidTypeIdException
```

**원인**: Event 클래스 패키지 경로가 `trusted.packages`에 없음

**해결**:
`application-kafka.yml`에 이미 설정되어 있습니다:
```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.json.trusted.packages: com.ebson.shrimp.tm.demo.common.kafka.event,com.ebson.shrimp.tm.demo.*.event
```

와일드카드 패턴(`*.event`)을 사용하여 모든 도메인 패키지의 이벤트를 허용합니다.

#### 4. Topic not found

**증상**:
```
org.apache.kafka.common.errors.UnknownTopicOrPartitionException: 
This server does not host this topic-partition
```

**원인**: Topic이 생성되지 않음

**해결**:
```bash
# Topic 생성
./scripts/create-topics.sh

# 또는 수동 생성
docker exec kafka-local kafka-topics.sh \
  --create \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created \
  --partitions 3 \
  --replication-factor 1
```

#### 5. Consumer rebalancing 반복

**증상**: Consumer가 계속 rebalance되며 메시지 처리 안 됨

**원인**: Consumer 인스턴스 수가 Partition 수보다 많음

**해결**:
```yaml
spring:
  kafka:
    listener:
      concurrency: 3  # Partition 수와 동일하거나 작게
```

### 9.2 로그 확인

#### Kafka 브로커 로그
```bash
docker compose logs kafka -f
```

#### 컨테이너 내부 로그 파일
```bash
docker exec kafka-local ls -la /var/lib/kafka/data/
```

#### Spring Boot 애플리케이션 로그 레벨 조정

`application-kafka.yml`에 기본 설정되어 있으며, 각 모듈에서 오버라이드 가능:
```yaml
logging:
  level:
    org.apache.kafka: DEBUG
    org.springframework.kafka: DEBUG
```

#### 특정 Topic 메시지 덤프
```bash
docker exec kafka-local kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created \
  --from-beginning \
  --max-messages 100 \
  --property print.timestamp=true \
  --property print.key=true
```

### 9.3 데이터 정리

#### 특정 Topic 삭제
```bash
docker exec kafka-local kafka-topics.sh \
  --delete \
  --bootstrap-server localhost:9092 \
  --topic shrimp-tm.conversation.session.created
```

#### 모든 데이터 삭제 및 재시작
```bash
docker compose down -v
docker compose up -d
./scripts/create-topics.sh
```

#### Consumer Group Offset 리셋
```bash
# 애플리케이션 중지 후 실행
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group shrimp-tm-group \
  --reset-offsets \
  --to-earliest \
  --topic shrimp-tm.conversation.session.created \
  --execute
```

### 9.4 Redis 관련 문제

**참고**: Redis는 로컬 환경에서 Docker를 사용하지 않고 터미널에서 직접 실행 중입니다.

#### 1. Redis Connection refused

**증상**:
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**원인**: Redis 서버가 실행되지 않았거나 포트가 다름

**해결**:
```bash
# Redis 상태 확인
redis-cli ping

# Redis 프로세스 확인
ps aux | grep redis-server

# Redis 재시작 (터미널에서)
redis-server

# 또는 Redis CLI로 종료 후 재시작
redis-cli SHUTDOWN
redis-server
```

#### 2. Redis NOAUTH Authentication required

**증상**:
```
io.lettuce.core.RedisCommandExecutionException: NOAUTH Authentication required
```

**원인**: Redis에 비밀번호가 설정되어 있지만 Spring Boot 설정에 없음

**해결**:
`application.yml`에 비밀번호 추가:
```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}
```

#### 3. Redis OOM (Out of Memory)

**증상**:
```
io.lettuce.core.RedisCommandExecutionException: OOM command not allowed when used memory > 'maxmemory'
```

**원인**: Redis 메모리 제한 초과

**해결**:

**방법 1**: 메모리 제한 증가 (redis.conf 또는 시작 옵션)
```bash
# redis.conf 파일 수정
maxmemory 512mb
maxmemory-policy allkeys-lru

# 또는 시작 시 옵션 지정
redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
```

**방법 2**: 오래된 데이터 수동 삭제
```bash
# Redis CLI 접속
redis-cli

# 특정 패턴 키 삭제
127.0.0.1:6379> EVAL "return redis.call('del', unpack(redis.call('keys', ARGV[1])))" 0 processed_event:*

# 또는 FLUSHDB (전체 삭제)
127.0.0.1:6379> FLUSHDB
```

**방법 3**: TTL 단축
```java
// IdempotencyServiceImpl.java
idempotencyService.markAsProcessed(eventId, 3L); // 7일 → 3일
```

#### 4. 멱등성 체크 실패 (중복 처리됨)

**증상**: 동일한 이벤트가 중복으로 처리되어 MongoDB에 중복 데이터 생성

**원인**: 
- Redis 장애 또는 연결 끊김
- TTL 만료로 키가 삭제됨
- Redis 데이터가 초기화됨

**확인**:
```bash
# 특정 이벤트 ID가 Redis에 있는지 확인
redis-cli GET processed_event:{eventId}

# EventConsumer 로그 확인
# "Skipping already processed event" 로그가 없으면 Redis에 키가 없는 것
```

**해결**:
```bash
# Redis 연결 상태 확인
redis-cli ping

# Spring Boot Actuator로 Health 체크
curl http://localhost:8080/actuator/health | jq .components.redis

# Redis 재시작 (터미널에서)
redis-cli SHUTDOWN
redis-server
```

#### 5. Redis 데이터 영속성 문제

**증상**: Redis 재시작 후 데이터 손실

**원인**: AOF 파일이 손상되었거나 영속성 설정이 비활성화됨

**확인**:
```bash
# Redis 설정 확인 (AOF 활성화 여부)
redis-cli CONFIG GET appendonly

# Redis 데이터 디렉토리 확인 (redis.conf에서 확인)
redis-cli CONFIG GET dir

# AOF 파일 확인 (dir 경로에서)
# 예: /usr/local/var/db/redis/appendonly.aof
cat /usr/local/var/db/redis/appendonly.aof
```

**해결**:
```bash
# AOF 복구 (AOF 파일 경로 지정)
redis-check-aof --fix /usr/local/var/db/redis/appendonly.aof

# 또는 RDB 파일 복구
redis-check-rdb /usr/local/var/db/redis/dump.rdb

# 복구 실패 시 데이터 초기화 (주의: 모든 데이터 삭제)
redis-cli FLUSHALL
```

#### 6. Redis 로그 확인

**실시간 명령어 모니터링**:
```bash
redis-cli MONITOR
```

**Redis 프로세스 로그 확인**:
```bash
# Redis 서버가 실행 중인 터미널에서 로그 확인
# 또는 시스템 로그 확인 (macOS의 경우)
tail -f /usr/local/var/log/redis.log

# 또는 Redis 서버 시작 시 로그 파일 지정
redis-server --logfile /path/to/redis.log
```

**메모리 사용량 확인**:
```bash
redis-cli INFO memory | grep used_memory
```

**키 통계 확인**:
```bash
redis-cli INFO keyspace
```

## 11. 참고자료

### 공식 문서
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Apache Kafka Docker Hub](https://hub.docker.com/r/apache/kafka)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Spring Boot Kafka Properties](https://docs.spring.io/spring-boot/reference/messaging/kafka.html)

### Redis 문서
- [Redis Documentation](https://redis.io/documentation)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/)
- [Redis Docker Hub](https://hub.docker.com/_/redis)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)

### 추가 자료
- [Kafka KRaft Mode](https://kafka.apache.org/documentation/#kraft)
- [Kafka UI GitHub](https://github.com/provectus/kafka-ui)
- [Spring Kafka Testing](https://docs.spring.io/spring-kafka/reference/testing.html)
- [Idempotent Consumer Pattern](https://microservices.io/patterns/communication-style/idempotent-consumer.html)

## 10. 프로젝트 구조

### 9.1 파일 구조

```
shrimp-tm-demo/
├── docker-compose.yml                          # Kafka 로컬 환경 구성
├── scripts/
│   └── create-topics.sh                        # Topic 생성 스크립트
├── common/
│   └── kafka/
│       ├── build.gradle
│       └── src/main/
│           ├── java/.../kafka/
│           │   ├── config/
│           │   │   └── KafkaConfig.java        # Kafka 설정 클래스
│           │   ├── consumer/
│           │   │   ├── EventConsumer.java      # 이벤트 소비자 (메인)
│           │   │   ├── EventHandler.java       # 이벤트 핸들러 인터페이스
│           │   │   ├── EventHandlerRegistry.java  # 핸들러 레지스트리
│           │   │   ├── IdempotencyService.java    # 멱등성 보장 서비스 (Redis 기반)
│           │   │   ├── ConversationSessionCreatedEventHandler.java
│           │   │   ├── ConversationSessionUpdatedEventHandler.java
│           │   │   ├── ConversationSessionDeletedEventHandler.java
│           │   │   └── ConversationMessageCreatedEventHandler.java
│           │   ├── publisher/
│           │   │   └── EventPublisher.java     # 이벤트 발행자
│           │   ├── event/
│           │   │   ├── BaseEvent.java          # 이벤트 인터페이스
│           │   │   ├── ConversationSessionCreatedEvent.java
│           │   │   ├── ConversationSessionUpdatedEvent.java
│           │   │   ├── ConversationSessionDeletedEvent.java
│           │   │   └── ConversationMessageCreatedEvent.java
│           │   └── sync/
│           │       ├── ConversationSyncService.java      # 동기화 서비스 인터페이스
│           │       └── ConversationSyncServiceImpl.java  # 구현체 (@ConditionalOnBean)
│           └── resources/
│               └── application-kafka.yml       # Kafka 공통 설정
└── api/
    ├── gateway/
    ├── contest/
    ├── news/
    ├── chatbot/  # ConversationSyncService 사용
    └── ...
```

### 9.2 모듈 의존성

- **common-kafka**: Kafka 관련 공통 기능 제공
  - `EventPublisher`: 이벤트 발행
  - `EventConsumer`: 이벤트 소비
  - `KafkaConfig`: Kafka 설정
  - Event 클래스들: 도메인 이벤트 정의

- **API 모듈들**: common-kafka 의존성 추가
  ```gradle
  dependencies {
      implementation project(':common-kafka')
  }
  ```

- **프로필 설정**: `application.yml`에 kafka 프로필 포함
  ```yaml
  spring:
    profiles:
      include:
        - kafka  # Kafka 설정 활성화
  ```

### 9.3 이벤트 처리 아키텍처

현재 구현은 **EventHandler 패턴**을 사용하여 이벤트를 처리합니다:

#### 핵심 컴포넌트

1. **EventConsumer** (메인 소비자)
   - `@KafkaListener`로 Kafka 메시지 수신
   - 멱등성 보장 (`IdempotencyService`)
   - EventHandlerRegistry에 이벤트 처리 위임

2. **EventHandler 인터페이스**
   ```java
   public interface EventHandler<T extends BaseEvent> {
       void handle(T event);
       String getEventType();
   }
   ```

3. **EventHandlerRegistry** (레지스트리 패턴)
   - 모든 EventHandler 구현체를 자동 등록
   - 이벤트 타입별로 적절한 핸들러 선택
   - Spring DI를 통한 핸들러 주입

4. **IdempotencyService** (멱등성 보장)
   - Redis 기반 중복 처리 방지
   - TTL 7일 설정
   - Key: `processed_event:{eventId}`

5. **구체적인 EventHandler 구현체**
   - `ConversationSessionCreatedEventHandler`
   - `ConversationSessionUpdatedEventHandler`
   - `ConversationSessionDeletedEventHandler`
   - `ConversationMessageCreatedEventHandler`
   - 각 핸들러는 `ConversationSyncService`에 동기화 위임

#### 처리 흐름

```
Kafka Topic 
  → EventConsumer (@KafkaListener)
    → IdempotencyService (중복 체크)
      → EventHandlerRegistry (핸들러 선택)
        → ConversationSessionCreatedEventHandler
          → ConversationSyncService (MongoDB 동기화)
            → ConversationSessionRepository (MongoDB 저장)
```

#### 장점

- **개방-폐쇄 원칙 (OCP)**: 새 이벤트 추가 시 새 핸들러만 추가
- **단일 책임 원칙 (SRP)**: 각 핸들러는 하나의 이벤트 타입만 처리
- **느슨한 결합**: EventConsumer는 구체적인 핸들러에 의존하지 않음
- **테스트 용이성**: 각 핸들러를 독립적으로 테스트 가능

#### ConversationSyncService

- `@ConditionalOnBean(ConversationSessionRepository.class)`: MongoDB Repository가 있을 때만 활성화
- MongoDB Document에 Aurora MySQL 데이터 동기화
- CQRS 패턴의 Query Side 업데이트 담당

---

**문서 버전**: 1.4  
**최종 업데이트**: 2026-01-23  
**Apache Kafka**: 4.1.1  
**Redis**: 로컬 실행 (Docker 미사용)  
**Spring Boot**: 3.x  
**변경 이력**:
- v1.4 (2026-01-23): Redis를 로컬 터미널에서 직접 실행하도록 문서 수정, application-kafka.yml에 Redis 설정 추가, Docker 명령어를 로컬 실행 명령어로 변경
- v1.3 (2026-01-22): Redis 연동 및 멱등성 보장 섹션 추가 (Section 5), docker-compose.yml에 Redis 서비스 추가, Redis 트러블슈팅 추가
- v1.2 (2026-01-22): EventHandler 패턴 아키텍처 설명 추가, 프로젝트 구조 업데이트, 로그 예시 개선
- v1.1 (2026-01-22): Kafka 설정 파일 및 Config 클래스 추가, Event 생성 예제 수정
- v1.0 (2026-01-21): 초기 버전
