# MySQL 8.0 Docker Compose 로컬 개발 환경 구축 가이드

## 1. 개요

### 1.1 목적 및 범위
AWS RDS Aurora MySQL 의존 없이 로컬 개발 환경에서 모듈별 독립 MySQL 8.0 인스턴스를 Docker Compose로 구축하고, 기존 `datasource/aurora` 모듈과 연동합니다.

### 1.2 대상 모듈 및 스키마

| 모듈 경로 | Gradle 모듈명 | Aurora 스키마 | 서버 포트 | 용도 |
|-----------|--------------|---------------|----------|------|
| `batch/source` | batch-source | `batch` | - | 배치 작업 (meta + business) |
| `api/auth` | api-auth | `auth` | 8083 | 인증/인가 |
| `api/bookmark` | api-bookmark | `bookmark` | 8085 | 북마크 관리 |
| `api/chatbot` | api-chatbot | `chatbot` | 8084 | RAG 챗봇 |

### 1.3 사전 요구사항
- Docker: 20.10 이상
- Docker Compose: 2.0 이상 (V2)
- JDK: 21
- Gradle: 9.x

### 1.4 현재 인프라 구성
기존 `docker-compose.yml`에 Kafka + Kafka UI가 구성되어 있으며, 이번 작업으로 MySQL 서비스 4개를 추가합니다.

## 2. Docker Compose 설정

### 2.1 추가할 MySQL 서비스

기존 `docker-compose.yml`에 다음 서비스들을 추가합니다:

```yaml
  # ============================================================
  # MySQL 8.0 로컬 개발용 (모듈별 독립 인스턴스)
  # ============================================================

  mysql-batch:
    image: mysql:8.0.41
    container_name: mysql-batch
    ports:
      - "3307:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: batch
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-batch-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-auth:
    image: mysql:8.0.41
    container_name: mysql-auth
    ports:
      - "3308:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: auth
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-auth-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-bookmark:
    image: mysql:8.0.41
    container_name: mysql-bookmark
    ports:
      - "3309:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: bookmark
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-bookmark-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-chatbot:
    image: mysql:8.0.41
    container_name: mysql-chatbot
    ports:
      - "3310:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: chatbot
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-chatbot-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

`volumes` 섹션에 MySQL 데이터 볼륨을 추가합니다 (MySQL 서비스는 별도 네트워크 없이 기본 네트워크를 사용):

```yaml
volumes:
  kafka-data:
    driver: local
  mysql-batch-data:
    driver: local
  mysql-auth-data:
    driver: local
  mysql-bookmark-data:
    driver: local
  mysql-chatbot-data:
    driver: local
```

### 2.2 전체 docker-compose.yml

```yaml
services:
  # ============================================================
  # Kafka (KRaft 모드)
  # ============================================================
  kafka:
    image: apache/kafka:4.1.1
    container_name: kafka-local
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_PROCESS_ROLES: controller,broker
      KAFKA_NODE_ID: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_LISTENERS: INTERNAL://0.0.0.0:9094,EXTERNAL://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9094,EXTERNAL://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_NUM_PARTITIONS: 3
      KAFKA_LOG_DIRS: /var/lib/kafka/data
      KAFKA_LOG_RETENTION_HOURS: 168
      KAFKA_LOG_SEGMENT_BYTES: 1073741824
    volumes:
      - kafka-data:/var/lib/kafka/data
    networks:
      - kafka-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9094"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "9090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9094
      DYNAMIC_CONFIG_ENABLED: 'true'
    depends_on:
      kafka:
        condition: service_healthy
    networks:
      - kafka-network
    restart: unless-stopped

  # ============================================================
  # MySQL 8.0 로컬 개발용 (모듈별 독립 인스턴스)
  # ============================================================

  mysql-batch:
    image: mysql:8.0.41
    container_name: mysql-batch
    ports:
      - "3307:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: batch
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-batch-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-auth:
    image: mysql:8.0.41
    container_name: mysql-auth
    ports:
      - "3308:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: auth
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-auth-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-bookmark:
    image: mysql:8.0.41
    container_name: mysql-bookmark
    ports:
      - "3309:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: bookmark
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-bookmark-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  mysql-chatbot:
    image: mysql:8.0.41
    container_name: mysql-chatbot
    ports:
      - "3310:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root1234}
      MYSQL_DATABASE: chatbot
      MYSQL_USER: ${MYSQL_USER:-admin}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-admin1234}
      TZ: Asia/Seoul
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_0900_ai_ci
      --default-time-zone=+09:00
    volumes:
      - mysql-chatbot-data:/var/lib/mysql
    deploy:
      resources:
        limits:
          memory: 512m
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysql", "-uroot", "-proot1234", "-e", "SELECT 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

networks:
  kafka-network:
    driver: bridge

volumes:
  kafka-data:
    driver: local
  mysql-batch-data:
    driver: local
  mysql-auth-data:
    driver: local
  mysql-bookmark-data:
    driver: local
  mysql-chatbot-data:
    driver: local
```

### 2.3 서비스 설정 설명

| 설정 항목 | 값 | 설명 |
|----------|---|------|
| `image` | `mysql:8.0.41` | Docker Hub 공식 MySQL 8.0 최신 안정 버전 |
| `MYSQL_DATABASE` | 모듈별 스키마명 | 컨테이너 초기화 시 자동 생성 |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `.env` 파일에서 주입 (기본값: `admin` / `admin1234`) | 기존 Aurora 설정과 동일한 인증 정보 |
| `character-set-server` | `utf8mb4` | 4바이트 Unicode 지원 |
| `collation-server` | `utf8mb4_0900_ai_ci` | MySQL 8.0 기본 collation (Unicode 9.0 기반, `utf8mb4_unicode_ci` 대비 정렬 성능 20-30% 향상) |
| `default-time-zone` | `+09:00` | Asia/Seoul (KST) |
| `deploy.resources.limits.memory` | `512m` | 개발 머신 메모리 보호를 위한 컨테이너당 제한 |
| `healthcheck` | `mysql -e "SELECT 1"` | 실제 SQL 실행으로 readiness 검증, `start_period: 30s` 포함 |

### 2.4 네트워크 구성
MySQL 서비스는 별도 네트워크를 지정하지 않으며, Docker Compose의 기본(default) 네트워크를 사용합니다. Spring Boot 애플리케이션이 호스트에서 `localhost`로 각 컨테이너에 접근하고, 컨테이너 간 직접 통신이 필요하지 않으므로 별도 네트워크 생성은 불필요합니다.

### 2.5 포트 매핑 요약

| 서비스 | 호스트 포트 | 컨테이너 포트 | JDBC URL |
|--------|-----------|-------------|----------|
| mysql-batch | 3307 | 3306 | `jdbc:mysql://localhost:3307/batch` |
| mysql-auth | 3308 | 3306 | `jdbc:mysql://localhost:3308/auth` |
| mysql-bookmark | 3309 | 3306 | `jdbc:mysql://localhost:3309/bookmark` |
| mysql-chatbot | 3310 | 3306 | `jdbc:mysql://localhost:3310/chatbot` |

### 2.6 환경변수 관리 (.env 파일)

Docker Compose 파일에 비밀번호를 직접 기입하는 대신, `.env` 파일을 사용하여 환경변수를 분리합니다. `docker-compose.yml`의 `${VAR:-default}` 구문은 `.env` 파일이 없어도 기본값으로 동작하므로, `.env` 파일 없이도 즉시 사용 가능합니다.

**`.env.example`** (Git에 커밋 — 팀원에게 필요한 변수를 안내):
```env
# MySQL Docker 로컬 개발 환경 설정
MYSQL_ROOT_PASSWORD=root1234
MYSQL_USER=admin
MYSQL_PASSWORD=admin1234
```

**`.env`** (`.gitignore`에 추가 — 실제 사용 값):
```env
MYSQL_ROOT_PASSWORD=root1234
MYSQL_USER=admin
MYSQL_PASSWORD=admin1234
```

> **참고**: `.env` 파일은 `docker-compose.yml`과 같은 디렉토리에 위치해야 Docker Compose가 자동으로 읽습니다. 로컬 개발 전용이므로 기본값을 그대로 사용해도 무방하며, `.env` 파일 생성은 선택사항입니다.

## 3. Spring Boot 연동

### 3.1 AWS RDS JDBC 드라이버 로컬 호환성

현재 프로젝트는 `software.aws.rds:aws-mysql-jdbc:1.1.15` 드라이버(`software.aws.rds.jdbc.mysql.Driver`)를 사용합니다.

**핵심 사항**: 이 드라이버는 표준 MySQL 프로토콜을 지원하므로 로컬 MySQL에도 접속 가능합니다. 단, AWS 전용 플러그인 관련 설정을 조정해야 합니다.

- 비-Aurora 엔드포인트 연결 시, 드라이버는 자동으로 일반 JDBC 연결을 반환합니다.
- HikariCP `data-source-properties`의 `wrapperPlugins`, `failoverMode`는 AWS Aurora 전용이므로, **local 프로필에서 `useConnectionPlugins=false`로 비활성화**합니다.

> **참고**: `aws-mysql-jdbc` 1.1.15는 AWS에서 2024년 7월 지원 종료된 레거시 드라이버입니다. 장기적으로 `software.amazon.jdbc:aws-advanced-jdbc-wrapper`로 마이그레이션을 권장합니다.

### 3.2 application-api-domain.yml 변경사항

`datasource/aurora/src/main/resources/application-api-domain.yml`의 `local` 프로필 섹션을 `dev`와 분리합니다.

**변경 전** (local과 dev가 동일 프로필):
```yaml
---
spring:
  config.activate.on-profile: local,dev

  datasource:
    writer:
      url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.schema}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
      username: admin
      password: admin1234
    reader:
      url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.schema}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
      username: admin
      password: admin1234
```

**변경 후** (local 프로필 분리):
```yaml
---
spring:
  config.activate.on-profile: local

  datasource:
    api:
      writer:
        hikari:
          data-source-properties:
            wrapperPlugins: ""
            useConnectionPlugins: false
      reader:
        hikari:
          data-source-properties:
            wrapperPlugins: ""
            useConnectionPlugins: false
    writer:
      url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.schema}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
      username: admin
      password: admin1234
    reader:
      url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.schema}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
      username: admin
      password: admin1234

---
spring:
  config.activate.on-profile: dev

  datasource:
    writer:
      url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.schema}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
      username: admin
      password: admin1234
    reader:
      url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.schema}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
      username: admin
      password: admin1234
```

**변경 포인트**:
- `local,dev` → `local`과 `dev`를 별도 프로필 섹션으로 분리
- JDBC URL: `localhost:${module.mysql.port}` — 각 모듈의 `application-*-api.yml`에서 정의한 Docker MySQL 포트로 자동 매핑
- `useSSL=false` — 로컬 Docker에서 SSL 불필요
- `allowPublicKeyRetrieval=true` — `caching_sha2_password` 인증 시 필요
- `wrapperPlugins: ""`, `useConnectionPlugins: false` — AWS 전용 플러그인 비활성화

**각 모듈별 포트 매핑** (`module.mysql.port` 프로퍼티):

| 모듈 | 스키마 (`module.aurora.schema`) | MySQL 포트 (`module.mysql.port`) | 설정 위치 |
|------|-------------------------------|--------------------------------|----------|
| api-auth | `auth` | 3308 | `application-auth-api.yml` |
| api-bookmark | `bookmark` | 3309 | `application-bookmark-api.yml` |
| api-chatbot | `chatbot` | 3310 | `application-chatbot-api.yml` |
| batch-source | `batch` | 3307 | `application.yml` (batch) |

> **동작 원리**: `module.mysql.port`는 `module.aurora.schema`와 동일한 패턴으로, 각 모듈의 `application-*-api.yml`에서 정의합니다. `datasource/aurora` 모듈의 `application-api-domain.yml`에서 `${module.mysql.port}` placeholder로 참조하므로, 모듈별 JDBC URL이 자동으로 올바른 포트에 매핑됩니다.

### 3.3 application-batch-domain.yml 변경사항

`datasource/aurora/src/main/resources/application-batch-domain.yml`의 `local` 프로필을 분리합니다.

**변경 전** (local과 dev가 동일 프로필):
```yaml
---
spring:
  config.activate.on-profile: local,dev

  datasource:
    writer:
      meta:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.meta.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.business.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
    reader:
      meta:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.meta.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.business.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
```

**변경 후** (local 프로필 분리):
```yaml
---
spring:
  config.activate.on-profile: local

  datasource:
    batch:
      meta:
        hikari:
          data-source-properties:
            wrapperPlugins: ""
            useConnectionPlugins: false
      writer:
        hikari:
          data-source-properties:
            wrapperPlugins: ""
            useConnectionPlugins: false
      reader:
        hikari:
          data-source-properties:
            wrapperPlugins: ""
            useConnectionPlugins: false
    writer:
      meta:
        url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.meta.schema:batch}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.business.schema:batch}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
        username: admin
        password: admin1234
    reader:
      meta:
        url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.meta.schema:batch}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://localhost:${module.mysql.port}/${module.aurora.business.schema:batch}?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
        username: admin
        password: admin1234

---
spring:
  config.activate.on-profile: dev

  datasource:
    writer:
      meta:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.meta.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.business.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
    reader:
      meta:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.meta.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
      business:
        url: jdbc:mysql://tech-n-ai-aurora-cluster-instance-1.c8hcnykzcnr8.ap-northeast-2.rds.amazonaws.com:3306/${module.aurora.business.schema:batch}?${AURORA_OPTIONS:useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}
        username: admin
        password: admin1234
```

**변경 포인트**:
- JDBC URL에서 `${module.mysql.port}`를 사용 — `batch/source/application.yml`에서 `module.mysql.port: 3307`로 정의
- meta와 business가 동일 스키마(`batch`)를 사용하므로 같은 컨테이너를 가리킴
- AWS 전용 플러그인 3개 풀 모두 비활성화

### 3.4 모듈별 `module.mysql.port` 설정

API 모듈은 모두 `application-api-domain.yml`의 공통 JDBC URL 템플릿을 사용하며, `${module.aurora.schema}`로 스키마를, `${module.mysql.port}`로 Docker MySQL 포트를 구분합니다. 기존에 각 모듈이 `module.aurora.schema`를 정의하는 것과 동일한 패턴으로 `module.mysql.port`를 추가합니다.

**API 모듈 설정**:

```yaml
# api/auth/src/main/resources/application-auth-api.yml
module:
  aurora:
    schema: auth
  mysql:
    port: 3308
```

```yaml
# api/bookmark/src/main/resources/application-bookmark-api.yml
module:
  aurora:
    schema: bookmark
  mysql:
    port: 3309
```

```yaml
# api/chatbot/src/main/resources/application-chatbot-api.yml
module:
  aurora:
    schema: chatbot
  mysql:
    port: 3310
```

**Batch 모듈 설정**:

```yaml
# batch/source/src/main/resources/application.yml
module:
  aurora:
    meta:
      schema: batch
  mysql:
    port: 3307
```

> **동작 원리**: `module.mysql.port`는 프로필에 관계없이 모듈의 기본 프로퍼티로 정의됩니다. `application-api-domain.yml`과 `application-batch-domain.yml`의 local 프로필에서 `${module.mysql.port}` placeholder로 참조하므로, local 프로필이 아닌 환경(dev, beta, prod)에서는 해당 placeholder가 사용되지 않습니다. 따라서 dev/beta/prod 환경에서는 Aurora RDS 엔드포인트(포트 3306)가 하드코딩된 기존 URL이 그대로 사용됩니다.

## 4. 실행 및 검증

### 4.1 Docker Compose 실행

```bash
# 전체 서비스 시작 (백그라운드)
docker compose up -d

# MySQL 서비스만 시작
docker compose up -d mysql-batch mysql-auth mysql-bookmark mysql-chatbot

# 서비스 상태 확인
docker compose ps

# 로그 확인 (전체)
docker compose logs -f mysql-batch mysql-auth mysql-bookmark mysql-chatbot

# 특정 서비스 로그
docker compose logs -f mysql-auth
```

### 4.2 MySQL 접속 확인

각 컨테이너에 직접 접속하여 확인합니다:

```bash
# mysql-batch (포트 3307, 스키마 batch)
docker exec -it mysql-batch mysql -u admin -padmin1234 batch -e "SELECT 1;"

# mysql-auth (포트 3308, 스키마 auth)
docker exec -it mysql-auth mysql -u admin -padmin1234 auth -e "SELECT 1;"

# mysql-bookmark (포트 3309, 스키마 bookmark)
docker exec -it mysql-bookmark mysql -u admin -padmin1234 bookmark -e "SELECT 1;"

# mysql-chatbot (포트 3310, 스키마 chatbot)
docker exec -it mysql-chatbot mysql -u admin -padmin1234 chatbot -e "SELECT 1;"
```

호스트에서 MySQL 클라이언트로 접속하는 경우:

```bash
# mysql CLI 사용
mysql -h 127.0.0.1 -P 3308 -u admin -padmin1234 auth

# 문자셋 확인
mysql -h 127.0.0.1 -P 3308 -u admin -padmin1234 -e "SHOW VARIABLES LIKE 'character_set%';"

# 타임존 확인
mysql -h 127.0.0.1 -P 3308 -u admin -padmin1234 -e "SELECT @@global.time_zone, @@session.time_zone;"
```

### 4.3 Spring Boot 애플리케이션 실행

```bash
# Gateway (포트 8081)
./gradlew :api-gateway:bootRun

# Auth (포트 8083, MySQL 3308)
./gradlew :api-auth:bootRun

# Chatbot (포트 8084, MySQL 3310)
./gradlew :api-chatbot:bootRun

# Bookmark (포트 8085, MySQL 3309)
./gradlew :api-bookmark:bootRun

# Batch (MySQL 3307)
./gradlew :batch-source:bootRun
```

### 4.4 연결 검증 체크리스트

- [ ] `docker compose ps` — 4개 MySQL 컨테이너 모두 `healthy` 상태
- [ ] 각 컨테이너에 `docker exec`로 접속하여 스키마 존재 확인
- [ ] 호스트에서 각 포트(3307~3310)로 MySQL 접속 확인
- [ ] Spring Boot 모듈 실행 시 HikariCP 로그에서 연결 성공 확인 (`API-WRITER`, `API-READER` 풀 생성 로그)
- [ ] 간단한 API 호출로 DB 연동 확인

## 5. 초기 데이터 설정 (선택)

### 5.1 docker-entrypoint-initdb.d 활용

MySQL Docker 이미지는 컨테이너 **최초 시작 시** `/docker-entrypoint-initdb.d/` 디렉토리의 `.sql`, `.sh`, `.sql.gz` 파일을 알파벳 순으로 실행합니다. 이미 데이터 볼륨에 데이터가 존재하면 스킵됩니다.

프로젝트의 `docs/sql/` 디렉토리에 기존 SQL 파일이 있습니다:
- `create.sql` — 메인 테이블 생성
- `batch5-create.sql`, `batch6-create.sql` — Spring Batch 메타데이터 테이블

활용 예시 (docker-compose.yml에 볼륨 마운트 추가):

```yaml
  mysql-batch:
    # ... 기존 설정 ...
    volumes:
      - mysql-batch-data:/var/lib/mysql
      - ./docs/sql/batch5-create.sql:/docker-entrypoint-initdb.d/01-batch-meta.sql:ro

  mysql-auth:
    # ... 기존 설정 ...
    volumes:
      - mysql-auth-data:/var/lib/mysql
      - ./docs/sql/create.sql:/docker-entrypoint-initdb.d/01-create.sql:ro
```

> **주의**: 초기화 스크립트는 데이터 볼륨이 비어 있을 때만 실행됩니다. 기존 볼륨 데이터를 초기화하려면 `docker compose down -v`로 볼륨을 삭제한 후 다시 시작해야 합니다.

## 6. 트러블슈팅

### 6.1 일반적인 문제

#### Connection refused

```
Communications link failure
The last packet sent successfully to the server was 0 milliseconds ago.
```

**원인**: MySQL 컨테이너가 아직 ready 상태가 아님
**해결**:
```bash
# 컨테이너 상태 확인
docker compose ps

# 헬스체크 상태 확인 (healthy가 될 때까지 대기)
docker inspect --format='{{.State.Health.Status}}' mysql-auth

# MySQL 초기화 완료까지 대기 (첫 실행 시 30초~1분 소요)
docker compose logs -f mysql-auth | grep "ready for connections"
```

#### Access denied for user

```
Access denied for user 'admin'@'172.x.x.x' (using password: YES)
```

**원인**: 인증 정보 불일치 또는 `MYSQL_USER`/`MYSQL_PASSWORD` 미적용
**해결**:
```bash
# 볼륨 삭제 후 재시작 (환경변수는 최초 초기화 시에만 적용)
docker compose down -v
docker compose up -d mysql-auth
```

#### Public Key Retrieval is not allowed

```
java.sql.SQLNonTransientConnectionException: Public Key Retrieval is not allowed
```

**원인**: `caching_sha2_password` 인증 시 공개키 교환 필요
**해결**: JDBC URL에 `allowPublicKeyRetrieval=true` 추가 (3.2절 참고)

#### wrapperPlugins 관련 오류

```
software.aws.rds.jdbc.mysql.shading.com.mysql.cj.exceptions.CJException:
Unable to load plugin 'readWriteSplitting'
```

**원인**: AWS 전용 플러그인이 로컬 MySQL에서 활성화됨
**해결**: local 프로필에서 `wrapperPlugins: ""`, `useConnectionPlugins: false` 설정 (3.2절 참고)

#### 포트 충돌

```
Error starting userland proxy: listen tcp4 0.0.0.0:3307: bind: address already in use
```

**원인**: 호스트에서 해당 포트를 이미 사용 중
**해결**:
```bash
# 포트 사용 중인 프로세스 확인
lsof -i :3307

# docker-compose.yml에서 포트 변경 (예: 3317:3306)
```

### 6.2 데이터 초기화

볼륨 데이터를 완전히 초기화하고 싶은 경우:

```bash
# 특정 서비스 볼륨만 삭제
docker compose down
docker volume rm tech-n-ai_mysql-auth-data

# 전체 MySQL 볼륨 삭제
docker compose down -v

# 다시 시작
docker compose up -d
```

### 6.3 로그 확인

```bash
# MySQL 에러 로그
docker compose logs mysql-auth 2>&1 | grep -i error

# 실시간 로그 모니터링
docker compose logs -f mysql-auth

# 컨테이너 내부에서 확인
docker exec -it mysql-auth mysql -u root -proot1234 -e "SHOW VARIABLES LIKE 'log_error';"
```

## 7. 서비스 관리

### 7.1 시작/중지

```bash
# MySQL만 시작
docker compose up -d mysql-batch mysql-auth mysql-bookmark mysql-chatbot

# MySQL만 중지 (데이터 유지)
docker compose stop mysql-batch mysql-auth mysql-bookmark mysql-chatbot

# 전체 중지 (데이터 유지)
docker compose down

# 전체 중지 + 볼륨 삭제
docker compose down -v
```

### 7.2 개별 컨테이너 재시작

```bash
docker compose restart mysql-auth
```

## 8. 파일 변경 요약

| 파일 | 작업 | 설명 |
|-----|------|------|
| `docker-compose.yml` | UPDATE | 4개 MySQL 서비스 + volumes 추가 (`version` 키 제거, 기본 네트워크 사용) |
| `.env.example` | CREATE | Docker Compose 환경변수 템플릿 (Git 커밋 대상) |
| `.env` | CREATE | 실제 환경변수 값 (`.gitignore` 등록 필요) |
| `datasource/aurora/.../application-api-domain.yml` | UPDATE | local/dev 프로필 분리, local에 `${module.mysql.port}` 기반 Docker MySQL URL 설정 |
| `datasource/aurora/.../application-batch-domain.yml` | UPDATE | local/dev 프로필 분리, local에 `${module.mysql.port}` 기반 Docker MySQL URL 설정 |
| `api/auth/.../application-auth-api.yml` | UPDATE | `module.mysql.port: 3308` 추가 |
| `api/bookmark/.../application-bookmark-api.yml` | UPDATE | `module.mysql.port: 3309` 추가 |
| `api/chatbot/.../application-chatbot-api.yml` | UPDATE | `module.mysql.port: 3310` 추가 |
| `batch/source/.../application.yml` | UPDATE | `module.mysql.port: 3307` 추가 |

## 9. 향후 개선 고려사항

### 9.1 Docker Compose Profiles
작업 중인 모듈만 선택적으로 기동하려면 [Docker Compose Profiles](https://docs.docker.com/compose/how-tos/profiles/)를 활용할 수 있습니다:
```yaml
  mysql-auth:
    profiles: ["auth", "full"]
    # ... 기존 설정 ...
```
```bash
# auth 모듈만 작업할 때
docker compose --profile auth up -d
```

### 9.2 Testcontainers (통합 테스트)
Docker Compose는 **로컬 개발 런타임** 용도이며, **통합 테스트**에는 [Testcontainers](https://testcontainers.com/)를 권장합니다:
- 테스트 클래스/스위트 단위로 컨테이너 생성/소멸 — 테스트 간 상태 오염 없음
- 랜덤 포트 매핑으로 포트 충돌 방지
- Spring Boot 3.1+의 [Testcontainers 자동 구성](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) 지원

## 10. 참고자료

- [Docker Hub - MySQL Official Image](https://hub.docker.com/_/mysql)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [Docker Compose Profiles](https://docs.docker.com/compose/how-tos/profiles/)
- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [AWS MySQL JDBC Driver (GitHub)](https://github.com/awslabs/aws-mysql-jdbc)
- [MySQL 8.0 caching_sha2_password 인증](https://dev.mysql.com/doc/refman/8.0/en/caching-sha2-pluggable-authentication.html)
- [Testcontainers - Java](https://java.testcontainers.org/)
