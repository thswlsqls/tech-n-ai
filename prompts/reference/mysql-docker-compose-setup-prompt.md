# MySQL 8.0 Docker Compose 로컬 개발 환경 구축

## 역할 및 목표
당신은 Docker 컨테이너 기반 데이터베이스 인프라 전문가입니다. Spring Boot 멀티모듈 프로젝트에서 모듈별 독립 MySQL 8.0 인스턴스를 Docker Compose로 구성하고, 기존 datasource/aurora 모듈과 연동하는 작업을 수행해야 합니다.

## 프로젝트 컨텍스트

### 현재 아키텍처
- **프레임워크**: Spring Boot 4.0.2, Java 21, Gradle 멀티모듈
- **CQRS 패턴**: Command(Aurora MySQL) / Query(MongoDB Atlas) / Sync(Kafka)
- **데이터소스 모듈**: `datasource/aurora` — 모든 API/Batch 모듈의 Aurora MySQL 연결을 중앙 관리
- **현재 상태**: 로컬 개발 시 AWS RDS Aurora 클라우드 인스턴스에 직접 연결 중 → Docker 로컬 MySQL로 전환 필요

### 기존 docker-compose.yml
프로젝트 루트에 이미 `docker-compose.yml`이 존재하며, Kafka + Kafka UI 서비스가 구성되어 있습니다.

```yaml
version: '3.8'
services:
  kafka:
    image: apache/kafka:4.1.1
    container_name: kafka-local
    ports:
      - "9092:9092"
      - "9093:9093"
    # ... (Kafka 설정)
    networks:
      - kafka-network

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    ports:
      - "9090:8080"
    # ... (Kafka UI 설정)
    networks:
      - kafka-network

networks:
  kafka-network:
    driver: bridge

volumes:
  kafka-data:
    driver: local
```

### 대상 모듈 및 스키마

| 모듈 경로 | 모듈명 | Aurora 스키마 | 용도 |
|-----------|--------|---------------|------|
| `batch/source` | batch-source | `batch` | 배치 작업 (meta + business 스키마) |
| `api/auth` | api-auth | `auth` | 인증/인가 |
| `api/bookmark` | api-bookmark | `bookmark` | 북마크 관리 |
| `api/chatbot` | api-chatbot | `chatbot` | RAG 챗봇 |

### 데이터소스 설정 구조

#### API 모듈 (api/auth, api/bookmark, api/chatbot)
- **프로필**: `api-domain`
- **설정 파일**: `datasource/aurora/src/main/resources/application-api-domain.yml`
- **DataSource 구성**: Writer/Reader 이중 풀 (HikariCP)
- **Java Config**: `ApiDataSourceConfig.java` — `spring.datasource.writer.url`, `spring.datasource.reader.url` 환경변수로 JDBC URL 주입
- **스키마 변수**: `${module.aurora.schema}` — 각 모듈의 `application-*.yml`에서 정의
- **JDBC URL 패턴 (현재 local 프로필)**:
  ```
  jdbc:mysql://tech-n-ai-aurora-cluster-instance-1...rds.amazonaws.com:3306/${module.aurora.schema}?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
  ```

#### Batch 모듈 (batch/source)
- **프로필**: `batch-domain`
- **설정 파일**: `datasource/aurora/src/main/resources/application-batch-domain.yml`
- **DataSource 구성**: Meta(Writer/Reader) + Business(Writer/Reader) — 3개 풀
- **스키마 변수**: `${module.aurora.meta.schema:batch}`, `${module.aurora.business.schema:batch}`
- **JDBC URL 패턴 (현재 local 프로필)**:
  ```
  jdbc:mysql://tech-n-ai-aurora-cluster-instance-1...rds.amazonaws.com:3306/${module.aurora.meta.schema:batch}?...
  jdbc:mysql://tech-n-ai-aurora-cluster-instance-1...rds.amazonaws.com:3306/${module.aurora.business.schema:batch}?...
  ```

### JDBC 드라이버
```gradle
// jpa.gradle
runtimeOnly 'software.aws.rds:aws-mysql-jdbc:1.1.15'  // AWS RDS 전용 드라이버
```
- **현재 드라이버**: `software.aws.rds.jdbc.mysql.Driver`
- **Docker 로컬 MySQL 연결 시**: AWS RDS 드라이버는 표준 MySQL 프로토콜도 지원하므로 `jdbc:mysql://localhost:PORT/SCHEMA` 형태로 접속 가능

### HikariConfig 설정 참고
```yaml
# API Writer/Reader 공통 (application-api-domain.yml)
spring.datasource.api.writer.hikari:
  driver-class-name: software.aws.rds.jdbc.mysql.Driver
  connection-timeout: 5000
  maximum-pool-size: 20
  minimum-idle: 5
  auto-commit: false
  data-source-properties:
    wrapperPlugins: readWriteSplitting,failover,efm  # AWS 전용 플러그인
```

## 작업 요구사항

### 1. Docker Compose 서비스 추가
기존 `docker-compose.yml`에 MySQL 8.0 서비스 4개를 추가합니다:

| 서비스명 | 컨테이너명 | 호스트 포트 | 스키마 | 대상 모듈 |
|---------|-----------|------------|--------|----------|
| `mysql-batch` | mysql-batch | 3307 | `batch` | batch/source |
| `mysql-auth` | mysql-auth | 3308 | `auth` | api/auth |
| `mysql-bookmark` | mysql-bookmark | 3309 | `bookmark` | api/bookmark |
| `mysql-chatbot` | mysql-chatbot | 3310 | `chatbot` | api/chatbot |

각 서비스 필수 설정:
- **이미지**: `mysql:8.0` (Docker Hub 공식)
- **환경변수**: `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`
- **포트 매핑**: 호스트 포트 → 컨테이너 3306
- **볼륨**: 데이터 영속성을 위한 named volume
- **네트워크**: 기존 `kafka-network` 활용 또는 별도 `mysql-network` 구성 (판단하여 결정)
- **문자셋**: `utf8mb4`, collation `utf8mb4_unicode_ci`
- **타임존**: `Asia/Seoul`
- **헬스체크**: `mysqladmin ping` 기반
- **재시작 정책**: `unless-stopped`

### 2. Spring 설정 변경 (local 프로필만)
`datasource/aurora/src/main/resources/application-api-domain.yml`과 `application-batch-domain.yml`의 **local 프로필 섹션만** 수정합니다:

**변경 대상**:
- JDBC URL: AWS RDS 엔드포인트 → `localhost:PORT`
- JDBC 드라이버: AWS RDS 드라이버 유지 가능 (표준 MySQL 호환) 또는 로컬용 분기 검토
- `wrapperPlugins` 등 AWS 전용 옵션: 로컬에서 불필요하므로 local 프로필에서 비활성화 방안 검토
- 인증 정보: Docker 컨테이너에 설정한 username/password와 일치

**주의사항**:
- dev, beta, prod 프로필은 절대 수정하지 않음
- `${module.aurora.schema}` 변수 치환 메커니즘은 그대로 유지
- 기존 코드 변경 최소화

### 3. 초기화 스크립트 (선택)
- 각 MySQL 컨테이너에 초기 테이블 생성이 필요한 경우, `docker-entrypoint-initdb.d` 마운트 방식 안내
- 프로젝트 내 `docs/sql/` 디렉토리에 기존 SQL 파일 존재 (`create.sql`, `batch5-create.sql` 등) — 활용 가능

## 제약사항 및 가이드라인

### 필수 준수사항
1. **공식 문서만 참조**:
   - Docker Hub MySQL 공식 이미지: https://hub.docker.com/_/mysql
   - Docker Compose 공식 문서: https://docs.docker.com/compose/
   - MySQL 8.0 공식 문서: https://dev.mysql.com/doc/refman/8.0/en/

2. **오버엔지니어링 금지**:
   - MySQL 복제(Replication) 구성 제외
   - 로컬 환경에 불필요한 보안 강화 설정 제외
   - 모니터링/메트릭 수집 도구 추가 제외
   - 커스텀 MySQL 설정 파일(my.cnf) 마운트는 문자셋/타임존 설정에 필요한 경우에만

3. **불필요한 작업 금지**:
   - 기존 Java 코드(DataSourceConfig 등) 리팩토링 제안 금지
   - 아키텍처 변경 제안 금지
   - 추가 라이브러리 도입 제안 금지

4. **주석 작성 원칙**:
   - 설정값의 의미와 목적만 간결하게 설명
   - 자명한 설정에 주석 불필요

## 기대 결과물

### 파일 변경 목록

| 파일 | 작업 |
|-----|------|
| `docker-compose.yml` | 4개 MySQL 서비스 + 관련 volumes/networks 추가 |
| `datasource/aurora/src/main/resources/application-api-domain.yml` | local 프로필 JDBC URL 변경 |
| `datasource/aurora/src/main/resources/application-batch-domain.yml` | local 프로필 JDBC URL 변경 |

### 가이드 문서 구조

```markdown
# MySQL Docker Compose 로컬 개발 환경 구축 가이드

## 1. 개요
- 목적: AWS Aurora 의존 없이 로컬 개발 환경 구축
- 사전 요구사항 (Docker, Docker Compose 버전)

## 2. Docker Compose 설정
### 2.1 추가된 MySQL 서비스
[docker-compose.yml 변경 내용]

### 2.2 설정 설명
[각 서비스 설정 항목 설명]

## 3. Spring Boot 연동
### 3.1 application-api-domain.yml 변경사항
[local 프로필 JDBC URL 변경]

### 3.2 application-batch-domain.yml 변경사항
[local 프로필 JDBC URL 변경]

### 3.3 AWS RDS 드라이버 로컬 호환성 참고
[wrapperPlugins 비활성화 등]

## 4. 실행 및 검증
### 4.1 Docker Compose 실행
[명령어]

### 4.2 MySQL 접속 확인
[각 컨테이너별 접속 테스트 방법]

### 4.3 Spring Boot 애플리케이션 실행
[모듈별 실행 명령어]

## 5. 초기 데이터 설정 (선택)
### 5.1 SQL 스크립트 마운트 방법
[docker-entrypoint-initdb.d 활용]

## 6. 트러블슈팅
### 6.1 일반적인 문제
[접속 오류, 권한 문제, 포트 충돌 등]

## 7. 참고자료
[공식 문서 링크]
```

## 품질 기준
- [ ] 공식 문서 기반 정보만 사용
- [ ] 기존 docker-compose.yml과 자연스럽게 통합
- [ ] 4개 MySQL 컨테이너가 독립적으로 동작
- [ ] datasource/aurora 모듈에서 4개 DB 엔드포인트에 모두 접근 가능
- [ ] local 프로필만 변경, dev/beta/prod 무변경
- [ ] 모든 명령어 실행 가능
- [ ] 트러블슈팅 섹션 포함
