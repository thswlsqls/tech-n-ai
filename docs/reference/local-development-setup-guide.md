# 로컬 개발 환경 설정 가이드

## 개요

이 문서는 shrimp-tm-demo 프로젝트의 로컬 개발 환경 설정 절차를 안내합니다.

### 사전 요구사항

| 도구 | 버전 | 확인 명령어 |
|------|------|-------------|
| Docker | 20.10+ | `docker --version` |
| Docker Compose | 2.0+ | `docker compose version` |
| JDK | 21+ | `java --version` |
| Redis | 7.x | `redis-server --version` |
| Gradle | 8.x | `./gradlew --version` |

### 인프라 구성

| 서비스 | 실행 방식 | 포트 | 용도 |
|--------|----------|------|------|
| Kafka | Docker | 9092 | 메시지 브로커 |
| Kafka UI | Docker | 8080 | Kafka 모니터링 |
| Redis | 로컬 설치 | 6379 | 멱등성 보장, 캐시 |

---

## 설정 절차

### Step 1: Kafka 시작 (Docker)

```bash
# 프로젝트 루트 디렉토리에서 실행
docker compose up -d
```

**실행 확인:**
```bash
# 컨테이너 상태 확인

# 예상 출력:
# NAME          IMAGE                           STATUS         PORTS
# kafka-local   apache/kafka:4.1.1              Up (healthy)   0.0.0.0:9092->9092/tcp
# kafka-ui      provectuslabs/kafka-ui:latest   Up             0.0.0.0:8080->8080/tcp
```

**Kafka 브로커 연결 확인:**
```bash

```

### Step 2: Kafka Topic 생성

```bash
# 스크립트 실행 권한 부여 (최초 1회)
chmod +x scripts/create-topics.sh

# Topic 생성
./scripts/create-topics.sh
```

**생성되는 Topic:**

| Topic | 용도 |
|-------|------|
| `shrimp-tm.conversation.session.created` | 대화 세션 생성 이벤트 |
| `shrimp-tm.conversation.session.updated` | 대화 세션 수정 이벤트 |
| `shrimp-tm.conversation.session.deleted` | 대화 세션 삭제 이벤트 |
| `shrimp-tm.conversation.message.created` | 대화 메시지 생성 이벤트 |

**Topic 생성 확인:**
```bash
docker exec kafka-local kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Step 3: Redis 시작 (로컬)

```bash
# Redis 서버 시작
redis-server

# 또는 백그라운드 실행 (macOS Homebrew)
brew services start redis
```

**연결 확인:**
```bash
redis-cli ping
# 예상 출력: PONG
```

### Step 4: 애플리케이션 실행

```bash
# API Gateway (포트 8081)
./gradlew :api-gateway:bootRun --args='--spring.profiles.active=local'

# Auth API (포트 8083)
./gradlew :api-auth:bootRun --args='--spring.profiles.active=local'

# Chatbot API (포트 8084)
./gradlew :api-chatbot:bootRun --args='--spring.profiles.active=local'

# Batch Source
./gradlew :batch-source:bootRun --args='--spring.profiles.active=local'
```

---

## 전체 시작 스크립트

개발 편의를 위한 통합 스크립트:

```bash
#!/bin/bash
# scripts/start-local.sh

set -e

echo "=== 로컬 개발 환경 시작 ==="

# 1. Kafka 시작
echo "[1/4] Kafka 시작..."
docker compose up -d

# 2. Kafka healthy 대기
echo "[2/4] Kafka 준비 대기..."
until docker exec kafka-local kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1; do
  echo "  Kafka 시작 대기 중..."
  sleep 2
done
echo "  Kafka 준비 완료"

# 3. Topic 생성
echo "[3/4] Kafka Topic 생성..."
./scripts/create-topics.sh

# 4. Redis 확인
echo "[4/4] Redis 확인..."
if redis-cli ping > /dev/null 2>&1; then
  echo "  Redis 연결 확인 완료"
else
  echo "  [오류] Redis가 실행되지 않았습니다."
  echo "  다음 명령어로 Redis를 시작하세요: redis-server"
  exit 1
fi

echo ""
echo "=== 환경 준비 완료 ==="
echo ""
echo "애플리케이션 실행:"
echo "  ./gradlew :api-chatbot:bootRun --args='--spring.profiles.active=local'"
echo ""
echo "모니터링:"
echo "  Kafka UI: http://localhost:8080"
```

---

## 종료 절차

### Kafka 종료

```bash
# 컨테이너 종료 (데이터 유지)
docker compose down

# 컨테이너 및 데이터 삭제
docker compose down -v
```

### Redis 종료

```bash
# Redis CLI로 종료
redis-cli shutdown

# 또는 Homebrew 사용 시
brew services stop redis
```

---

## 모니터링

### Kafka UI

브라우저에서 `http://localhost:8080` 접속

**주요 기능:**
- Topics: Topic 목록 및 메시지 조회
- Consumers: Consumer Group 상태 및 Lag 확인
- Brokers: 브로커 상태 확인

### Kafka CLI

```bash
# Topic 목록
docker exec kafka-local kafka-topics.sh --list --bootstrap-server localhost:9092

# Topic 상세 정보
docker exec kafka-local kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic shrimp-tm.conversation.session.created

# Consumer Group 목록
docker exec kafka-local kafka-consumer-groups.sh --list --bootstrap-server localhost:9092

# Consumer Group Lag 확인
docker exec kafka-local kafka-consumer-groups.sh --describe --bootstrap-server localhost:9092 --group chatbot-api
```

### Redis CLI

```bash
# 연결 테스트
redis-cli ping

# 처리된 이벤트 키 조회
redis-cli KEYS "processed_event:*"

# 메모리 사용량
redis-cli INFO memory
```

---

## 트러블슈팅

### Kafka 연결 실패

**증상:**
```
org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms
```

**해결:**
```bash
# Kafka 상태 확인
docker compose ps

# Kafka 로그 확인
docker compose logs kafka

# Kafka 재시작
docker compose restart kafka
```

### Redis 연결 실패

**증상:**
```
io.lettuce.core.RedisConnectionException: Unable to connect to localhost:6379
```

**해결:**
```bash
# Redis 프로세스 확인
ps aux | grep redis-server

# Redis 시작
redis-server
```

### Topic 생성 실패

**증상:**
```
Error while executing topic command: Topic already exists
```

**해결:**
- `--if-not-exists` 옵션 사용으로 이미 처리됨
- 강제 재생성 필요시:
```bash
# Topic 삭제
docker exec kafka-local kafka-topics.sh --delete --bootstrap-server localhost:9092 --topic shrimp-tm.conversation.session.created

# Topic 재생성
./scripts/create-topics.sh
```

### 전체 초기화

```bash
# Kafka 데이터 삭제 및 재시작
docker compose down -v
docker compose up -d
./scripts/create-topics.sh

# Redis 데이터 삭제
redis-cli FLUSHALL
```

---

## 애플리케이션 포트 정리

| 모듈 | 포트 | 설명 |
|------|------|------|
| api-gateway | 8081 | API Gateway |
| api-emerging-tech | 8082 | Emerging Tech 서비스 |
| api-auth | 8083 | 인증 서비스 |
| api-chatbot | 8084 | 챗봇 서비스 |
| api-bookmark | 8085 | 북마크 서비스 |
| api-agent | 8086 | AI Agent 서비스 |
| kafka-ui | 8080 | Kafka 모니터링 UI |

---

## 참고 자료

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Spring Kafka Reference](https://docs.spring.io/spring-kafka/reference/)
- [Redis Documentation](https://redis.io/documentation)
- [프로젝트 Kafka 설정 가이드](./kafka-docker-local-setup-guide.md)
