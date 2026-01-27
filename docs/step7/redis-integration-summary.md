# Redis 연동 완료 요약

**작성 일시**: 2026-01-22  
**대상 파일**:
- `docker-compose.yml`
- `docs/kafka-docker-local-setup-guide.md`

---

## 1. 주요 변경 사항

### 1.1 docker-compose.yml 업데이트

Redis 서비스를 추가하여 Kafka 이벤트 멱등성 보장을 지원합니다.

#### 추가된 서비스

```yaml
redis:
  image: redis:7-alpine
  container_name: redis-local
  ports:
    - "6379:6379"
  command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
  volumes:
    - redis-data:/data
  networks:
    - kafka-network
  restart: unless-stopped
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 3s
    retries: 5
```

#### 주요 설정

- **이미지**: redis:7-alpine (경량화)
- **포트**: 6379
- **AOF 영속성**: `--appendonly yes`
- **메모리 제한**: 256MB
- **메모리 정책**: allkeys-lru (가장 오래된 키 삭제)
- **볼륨**: redis-data (데이터 영속성 보장)
- **헬스체크**: redis-cli ping

### 1.2 가이드 문서 업데이트

`docs/kafka-docker-local-setup-guide.md` v1.3으로 업데이트:

#### 추가된 섹션

1. **Section 2 업데이트**:
   - docker-compose.yml 예시에 Redis 서비스 추가
   - Redis 설정 설명 추가
   - Redis 연결 확인 방법 추가

2. **Section 5 (신규)**: Redis 연동 및 멱등성 보장
   - 5.1 Redis 개요 및 멱등성 필요성
   - 5.2 Spring Data Redis 의존성
   - 5.3 Redis 연결 설정
   - 5.4 IdempotencyService 구현
   - 5.5 EventConsumer에서 멱등성 보장 사용
   - 5.6 Redis 키 관리
   - 5.7 Redis 명령어로 멱등성 확인
   - 5.8 멱등성 테스트 시나리오
   - 5.9 Redis 모니터링
   - 5.10 Redis 운영 고려사항

3. **Section 9.4 (신규)**: Redis 관련 트러블슈팅
   - Connection refused
   - NOAUTH Authentication required
   - OOM (Out of Memory)
   - 멱등성 체크 실패
   - 데이터 영속성 문제
   - 로그 확인 방법

4. **Section 11 업데이트**: Redis 참고자료 추가
   - Redis Documentation
   - Spring Data Redis Reference
   - Redis Docker Hub
   - Idempotent Consumer Pattern

---

## 2. 멱등성 보장 메커니즘

### 2.1 동작 원리

```
Kafka 이벤트 수신
  ↓
IdempotencyService.isProcessed(eventId) 확인
  ↓
이미 처리됨?
  ├─ Yes → Skip 후 ACK (중복 처리 방지)
  └─ No  → EventHandler 실행
            ↓
         ConversationSyncService (MongoDB 동기화)
            ↓
         IdempotencyService.markAsProcessed(eventId, 7일)
            ↓
         수동 ACK
```

### 2.2 Redis 키 구조

- **Key Pattern**: `processed_event:{eventId}`
- **Value**: 처리 시각 타임스탬프 (밀리초)
- **TTL**: 7일 (Kafka 메시지 보관 기간과 동일)

**예시**:
```
Key: processed_event:a1b2c3d4-e5f6-7890-abcd-ef1234567890
Value: 1706000000456
TTL: 604800 seconds (7 days)
```

### 2.3 멱등성이 필요한 상황

1. **Consumer 재시작**: 오프셋 커밋 전 종료
2. **네트워크 지연**: ACK 늦게 도착하여 재전송
3. **Rebalancing**: Consumer Group 재조정
4. **수동 오프셋 리셋**: 개발 환경에서 오프셋 초기화

---

## 3. 실행 순서

### Step 1: 인프라 실행

```bash
# Docker Compose로 Kafka, Kafka UI, Redis 실행
docker compose up -d

# 서비스 상태 확인
docker compose ps

# 예상 출력:
# kafka-local   Up (healthy)   0.0.0.0:9092-9093->9092-9093/tcp
# kafka-ui      Up             0.0.0.0:8080->8080/tcp
# redis-local   Up (healthy)   0.0.0.0:6379->6379/tcp
```

### Step 2: Kafka 토픽 생성

```bash
./scripts/create-topics.sh
```

### Step 3: 서비스 연결 확인

#### Kafka 확인
```bash
docker exec kafka-local kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list
```

#### Redis 확인
```bash
docker exec redis-local redis-cli ping
# 출력: PONG
```

#### Kafka UI 접속
```bash
open http://localhost:8080
```

### Step 4: 애플리케이션 실행

```bash
# @api/chatbot 모듈 실행 (not @api/auth)
./gradlew :api-chatbot:bootRun
```

**확인 사항**:
- Spring Boot가 Redis에 연결되었는지 확인
- EventConsumer가 Kafka 토픽을 구독했는지 확인
- IdempotencyService 빈이 생성되었는지 확인

---

## 4. 테스트 시나리오

### 4.1 정상 처리 테스트

```bash
# 1. 대화 세션 생성 (이벤트 발행)
curl -X POST http://localhost:8080/api/v1/chatbot/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"title": "Test Session"}'

# 2. EventConsumer 로그 확인
# "Successfully processed event: eventId=xxx, eventType=CONVERSATION_SESSION_CREATED"

# 3. Redis에서 확인
docker exec redis-local redis-cli GET processed_event:{eventId}
# 출력: 타임스탬프 값

# 4. MongoDB에서 동기화 확인
# ConversationSessionDocument가 생성되었는지 확인
```

### 4.2 중복 처리 방지 테스트

```bash
# 1. 애플리케이션 중지
kill <pid>

# 2. Consumer Group 오프셋 리셋
docker exec kafka-local kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group shrimp-tm-group \
  --reset-offsets \
  --to-earliest \
  --topic shrimp-tm.conversation.session.created \
  --execute

# 3. 애플리케이션 재시작
./gradlew :api-chatbot:bootRun

# 4. 로그 확인
# "Skipping already processed event: eventId=xxx" 출력 확인
# → 중복 처리되지 않음 ✅
```

### 4.3 Redis 장애 시나리오 테스트

```bash
# 1. Redis 중지
docker stop redis-local

# 2. 이벤트 발행 시도
# → RedisConnectionFailureException 발생
# → EventConsumer에서 예외 발생
# → Kafka 메시지 재시도

# 3. Redis 재시작
docker start redis-local

# 4. 재시도 성공 확인
# → 이벤트가 정상 처리됨
```

---

## 5. Redis 모니터링

### 5.1 실시간 명령어 모니터링

```bash
docker exec redis-local redis-cli MONITOR
```

**출력 예시**:
```
1706000000.123456 [0 172.18.0.5:12345] "EXISTS" "processed_event:abc123"
1706000000.456789 [0 172.18.0.5:12345] "SETEX" "processed_event:abc123" "604800" "1706000000456"
```

### 5.2 메모리 사용량 확인

```bash
docker exec redis-local redis-cli INFO memory | grep used_memory
```

### 5.3 키 통계 확인

```bash
# 전체 키 개수
docker exec redis-local redis-cli DBSIZE

# 키 공간 정보
docker exec redis-local redis-cli INFO keyspace
```

### 5.4 Spring Boot Actuator Health Check

```bash
curl http://localhost:8080/actuator/health | jq .components.redis
```

**출력 예시**:
```json
{
  "status": "UP",
  "details": {
    "version": "7.2.4"
  }
}
```

---

## 6. 메모리 사용량 추정

### 6.1 키당 메모리

- 이벤트 ID: UUID (36자) + Key Prefix (16자) = 52바이트
- Value: 타임스탬프 (13자) = 13바이트
- Redis 오버헤드: 약 90바이트
- **총 메모리**: 약 155바이트/키

### 6.2 예상 사용량

| 일일 이벤트 수 | 7일 보관 시 메모리 |
|--------------|------------------|
| 1,000 events/day | 1.1 MB |
| 10,000 events/day | 11 MB |
| 100,000 events/day | 110 MB |

→ 로컬 개발 환경(256MB)으로 충분

---

## 7. 트러블슈팅 빠른 참조

### Redis Connection refused
```bash
docker compose ps redis
docker compose logs redis
docker compose restart redis
```

### OOM (Out of Memory)
```bash
# 메모리 제한 증가
# docker-compose.yml에서 --maxmemory 512mb로 변경

# 또는 오래된 데이터 삭제
docker exec redis-local redis-cli FLUSHDB
```

### 멱등성 체크 실패
```bash
# Redis 연결 확인
docker exec redis-local redis-cli ping

# 특정 이벤트 ID 확인
docker exec redis-local redis-cli GET processed_event:{eventId}
```

---

## 8. 운영 환경 권장사항

### 8.1 인프라

- **AWS ElastiCache for Redis** 또는 **Redis Enterprise Cloud** 사용
- **고가용성**: Multi-AZ, Replication
- **메모리**: 최소 1GB 이상 (트래픽에 따라 조정)
- **모니터링**: CloudWatch 또는 Redis Insight

### 8.2 보안

- VPC 내부 배치
- 비밀번호 인증 (`requirepass`)
- TLS/SSL 암호화
- IAM 기반 접근 제어 (AWS)

### 8.3 백업

- 일일 스냅샷
- RDB + AOF 동시 사용
- 백업 보관 기간: 7일

---

## 9. 체크리스트

Kafka + Redis 연동이 완료되었는지 확인하세요:

- [ ] docker-compose.yml에 Redis 서비스 추가됨
- [ ] docker compose up -d로 Redis 실행됨
- [ ] docker exec redis-local redis-cli ping → PONG 출력
- [ ] common-kafka 모듈에 spring-boot-starter-data-redis 의존성 있음
- [ ] application-kafka.yml에 Redis 연결 설정 있음
- [ ] IdempotencyService 구현체 있음
- [ ] EventConsumer에서 IdempotencyService 사용함
- [ ] @api/chatbot 모듈 실행 시 Redis 연결 성공
- [ ] 이벤트 발행 후 Redis에 processed_event:{eventId} 키 생성 확인
- [ ] 중복 처리 방지 테스트 성공 (오프셋 리셋 후 Skip 로그 확인)

---

**검증 완료**: ✅  
**문서 버전**: v1.3  
**최종 업데이트**: 2026-01-22
