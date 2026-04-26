# 04. 데이터베이스 · 캐시 · 스토리지 설계

> **문서 범위**: Tech-N-AI CQRS(Aurora MySQL + MongoDB Atlas) 데이터 계층 및 캐시/오브젝트 스토리지의 AWS 관리형 설계서
>
> **대상 리전**: `ap-northeast-2` (서울) · **환경**: `dev` / `beta` / `prod`
>
> **SLO**: RPO ≤ 5분 · RTO ≤ 30분 (프로덕션 기준)
>
> **공식 출처**:
> - AWS Aurora 사용자 가이드: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/
> - AWS Aurora 모범 사례: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.BestPractices.html
> - ElastiCache 사용자 가이드: https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/
> - Amazon S3 사용자 가이드: https://docs.aws.amazon.com/AmazonS3/latest/userguide/
> - AWS DMS 사용자 가이드: https://docs.aws.amazon.com/dms/latest/userguide/
> - AWS Backup 사용자 가이드: https://docs.aws.amazon.com/aws-backup/latest/devguide/
> - MongoDB Atlas 공식 문서: https://www.mongodb.com/docs/atlas/
> - MongoDB Atlas AWS PrivateLink: https://www.mongodb.com/docs/atlas/security-private-endpoint/

---

## 1. Aurora MySQL (aurora-mysql)

### 1.1 엔진 선정 근거

| 후보 | 장점 | 단점 | 결정 |
|---|---|---|---|
| **Aurora MySQL (Provisioned)** | OLTP/배치 혼합 워크로드에 안정적, Reader 수평 확장, Global Database 지원, Performance Insights 심층 메트릭 | 최소 인스턴스 상시 비용 | **prod 채택** |
| Aurora Serverless v2 | ACU 단위 초단위 스케일, 비정형 트래픽 흡수 | 장기 고부하/배치 I/O에서 Provisioned 대비 비용 증가, Global Database 지원은 v2 라이터에서 2023년 이후 가능하나 제약 존재 | **dev/beta 채택** (저비용·탄력성) |
| RDS for MySQL | 오랜 운영 이력, 비용 예측 쉬움 | 스토리지 자동 확장 없음, Reader 복제 지연 비교적 큼, Aurora 수준의 고가용성 부재 | **기각** |

- **근거**: 배치 모듈(`batch-source`)은 대량 INSERT/UPDATE 을 발생시키고 10개 API 모듈이 `datasource-aurora` 를 공유하므로, **Writer 1 + Reader 2** 구조와 **스토리지 자동 분산 복제(6-way, 3 AZ)** 의 이점이 큰 Aurora MySQL 을 채택한다. Aurora 는 볼륨당 6벌 복제로 AZ 2개 손실에도 읽기, AZ 1개+디스크 1개 손실에도 쓰기가 가능하다(Aurora 사용자 가이드: *High availability for Amazon Aurora*).
- dev/beta 는 야간 유휴 시간이 길어 **Aurora Serverless v2** 로 비용을 낮춘다. 환경별 ACU 범위는 §1.3 표 참조(코드 tfvars 기준: dev `0.5–2`, beta `0.5–4`). prod 는 Provisioned 로 고정하여 p99 지연을 안정화한다.

### 1.2 엔진 버전 · 스토리지

- **엔진 버전**: `aurora-mysql 8.0.mysql_aurora.3.x` 계열 (MySQL 8.0 호환). 현시점 AWS가 LTS로 지정한 버전만 채택하여 장기 유지하며, **LTS 지정은 변동되므로 분기별 재확인 필요**(Aurora 사용자 가이드: *Aurora MySQL LTS releases*). `modules/aurora-mysql/variables.tf` `engine_version` 기본값은 `8.0.mysql_aurora.3.07.1` 이며 LTS 변경 시 envs 호출부에서 `engine_version` 명시 갱신.
- **스토리지 클래스**: **Aurora I/O-Optimized** 채택. 배치 대량 쓰기가 존재하고 월 I/O 비용이 총 비용의 25% 를 초과할 때 전환 이익이 발생한다는 AWS 권고(Aurora 사용자 가이드: *Storage configurations for Aurora DB clusters*) 에 근거한다. dev/beta 는 **Aurora Standard** 유지.
- **스토리지 암호화**: KMS CMK (`arn:aws:kms:ap-northeast-2:<acct>:key/aurora-<env>`) 로 at-rest 암호화. 전송 중 암호화는 TLS 1.2+ 강제.

### 1.3 클러스터 구성

| 환경 | 인스턴스 | 구성 | 비고 |
|---|---|---|---|
| dev | `db.serverless` | Writer 1 | Serverless v2, **0.5–2 ACU** (모듈 기본값, dev tfvars override 없음) |
| beta | `db.serverless` | Writer 1 | Serverless v2, **0.5–4 ACU** (`envs/beta/terraform.tfvars`). Serverless v2 는 모듈에서 `instance_count = 1` 로 고정 |
| prod | `db.r7g.large` × 3 | Writer 1 + Reader 2 | 3 AZ 분산, Graviton, I/O-Optimized (`envs/prod/terraform.tfvars` `aurora_instance_count = 3`) |

- **AZ 배치**: `ap-northeast-2a / 2b / 2c` 에 Writer·Reader 각각 분산. Writer 장애 시 Aurora 가 자동 failover(보통 30초 이내, 고성능 쓰기 failover 활성화 시 더 단축)를 수행(Aurora 사용자 가이드: *Fault tolerance for an Aurora DB cluster*).
- **엔드포인트 활용**
  - `cluster endpoint` (Writer) → 트랜잭션 커밋 경로
  - `reader endpoint` (Reader LB) → CQRS 보조 경로, 통계/어드민 조회
  - `custom endpoint` → 배치 전용 Reader 로 API 트래픽과 분리

### 1.4 파라미터 그룹

DB 클러스터/인스턴스 파라미터 그룹 분리:

| 파라미터 | 값 | 이유 |
|---|---|---|
| `time_zone` | `Asia/Seoul` | 백엔드 `TZ=Asia/Seoul` 일치 |
| `character_set_server` | `utf8mb4` | 이모지/다국어 |
| `collation_server` | `utf8mb4_0900_ai_ci` | MySQL 8 기본 |
| `max_connections` | `LEAST({DBInstanceClassMemory/12582880}, 3000)` | 기본 공식 유지 + HikariCP `maximum-pool-size=20` × 모듈 10개 × 인스턴스 N 대비 안전 마진 |
| `long_query_time` | `1` | 슬로우 쿼리 로깅 |
| `slow_query_log` | `ON` | CloudWatch Logs 내보내기 |
| `performance_schema` | `ON` | Performance Insights 요구 |
| `innodb_print_all_deadlocks` | `ON` | 운영 진단 |
| `binlog_format` | `ROW` | DMS/CDC 및 Kafka Outbox |
| `binlog_row_image` | `FULL` | DMS CDC 호환 |

### 1.5 고가용성 · DR

- **자동 백업 보존**: 환경별 차등 — dev `1`, beta `7`, **prod `30`일** (`envs/prod/terraform.tfvars` `aurora_backup_retention_period = 30`). AWS 공식 최대치는 35일이며 향후 컴플라이언스 요건 발생 시 35일로 확장 가능 (Aurora 사용자 가이드 *Backing up and restoring an Aurora DB cluster*).
- **PITR (Point-in-Time Recovery)**: 5분 이내 복구 가능. **RPO ≤ 5분 충족**.
- **수동 스냅샷 정책**: **AWS Backup** 사용, prod 는 **매일 02:00 KST** 스냅샷 + **주간 크로스 리전 복사(`ap-northeast-1`)** (AWS Backup 사용자 가이드: *Cross-Region backup*). 보존 = 90일.
- **Global Database (DR)**: prod 에 한해 **`ap-northeast-1`** 에 Secondary 클러스터 구성을 **설계만 유지**(D-5: 마케팅·실사용자 진입 전까지 미구축, 11 §DR 패턴 매핑 참조). 표준 복제 지연 1초 이내, 리전 장애 시 **managed planned failover** 로 약 1–2분 내 promote(Aurora 사용자 가이드: *Using Amazon Aurora Global Database*). **활성화 시 RTO ≤ 30분 충족**.
- **Backtrack**: Aurora MySQL 은 Backtrack 지원 가능하나 Global Database 와 병용 제한이 있어, 본 설계는 **PITR + 스냅샷** 전략을 기본으로 하고 Backtrack 은 dev/beta 에 한해 72시간 창으로 보조 활성화.

### 1.6 보안

- **네트워크**: Private-Data 서브넷에만 배치, 보안 그룹은 App 서브넷의 EKS/ECS 보안 그룹에서 `3306/tcp` 만 허용.
- **IAM DB Authentication**: **활성화**. 운영자 접근은 IAM 역할을 통해 토큰 발급, 애플리케이션 기본 경로는 Secrets Manager 의 아이디/비밀번호(로테이션 30일) 병행 (Aurora 사용자 가이드: *IAM database authentication*).
- **전송 중 암호화**: `require_secure_transport=ON`, 클라이언트는 AWS RDS CA 번들 사용.
- **저장 시 암호화**: KMS CMK 분리 (`alias/aurora-<env>`), 스냅샷/로그/Performance Insights 모두 동일 CMK.
- **감사**: `server_audit_logging=1`, `server_audit_events=CONNECT,QUERY_DDL,QUERY_DCL` CloudWatch Logs 송출.
- **RDS Data API**: 본 시스템은 HikariCP 기반 영구 연결이 주 경로이므로 **미사용**. Lambda 기반 보조 경로에서만 필요 시 활성화.

### 1.7 Spring Boot 연동 (HikariCP)

현재 `datasource/aurora/src/main/resources/application-api-domain.yml` 의 `aws-advanced-jdbc-wrapper` 설정을 AWS 환경에 맞춰 유지한다.

- **드라이버**: `software.aws.rds.jdbc.mysql.Driver` (AWS JDBC Wrapper)
- **wrapperPlugins**: `readWriteSplitting,failover,efm` — Writer/Reader 분리, 빠른 failover, Enhanced Failure Monitoring 포함.
- **HikariCP (Writer)** — 기존 설정 반영

  | 항목 | 값 | 비고 |
  |---|---|---|
  | `maximum-pool-size` | 20 | 모듈별 |
  | `minimum-idle` | 5 | |
  | `connection-timeout` | 5000ms | |
  | `max-lifetime` | 28,795,000ms | Aurora Writer 안전 마진(`wait_timeout=28800`) |
  | `idle-timeout` | 300,000ms | |
  | `auto-commit` | false | JPA 권장 |
  | `transaction-isolation` | READ_COMMITTED | MySQL 기본값은 REPEATABLE-READ 이나, 현재 yml 의 `TRANSACTION_READ_UNCOMMITTED` 는 더티 리드가 발생해 JPA + OLTP 환경에 부적합 — 반드시 READ_COMMITTED 이상 사용 |

- **엔드포인트**: prod URL은 **클러스터 엔드포인트(Writer)** 와 **리더 엔드포인트** 로 분리한다. 현재 `yml` 에 `instance-1.*.rds.amazonaws.com` 로 고정된 값은 **Writer→ cluster endpoint**, **Reader→ reader endpoint** 로 교체한다 (failover 안전성).
- **CQRS 읽기 보조 경로**: 주 읽기는 MongoDB Atlas 이지만, 어드민 통계/정합성 검증 쿼리는 `readWriteSplitting` 플러그인을 통해 **Reader 엔드포인트**로 라우팅. `@Transactional(readOnly = true)` 마크.
- **Flyway 통합**: `batch-source` 와 `api-*` 공용으로 `datasource-aurora` 에 Flyway 마이그레이션 배치. CI 파이프라인에서 **`flyway migrate` 는 배포 직전 단일 잡(`flyway-apply`)** 으로 실행하고, Spring 부트시 `spring.flyway.enabled=false` 로 애플리케이션에서는 적용하지 않는다(동시 기동 시 lock 경쟁 방지). 환경별 location: `classpath:db/migration/{env}`.
- **Secrets Manager 연동**: `spring.datasource.*.password` 는 `aws-secretsmanager-jdbc` 또는 `spring-cloud-aws-secrets-manager` 로 런타임 주입. Secrets Manager 로테이션은 **30일 주기** (AWS Secrets Manager: *Rotate AWS Secrets Manager secrets*).

### 1.8 관측성

- **Performance Insights**: **활성화**, 보존 7일(무료 티어) + prod 는 **24개월 장기 보존**.
- **Enhanced Monitoring**: 1초 granularity, prod 에 필수.
- **CloudWatch Logs Export**: `error, slowquery, audit, general(off)`.
- **Event Subscription**: `failover, maintenance, backup` → SNS → Slack.

### 1.9 RPO/RTO 충족 정리

| 지표 | 설계 | 값 |
|---|---|---|
| RPO | PITR 5분 + Global Database 복제 ~1초 | **≤ 5분** |
| RTO | Writer 자동 failover + 운영자 DNS 승격 | **≤ 30분** |

---

## 2. MongoDB Atlas (mongodb-atlas)

MongoDB Atlas 는 AWS 외부의 관리형 서비스로 **AWS PrivateLink** 를 통해 VPC 내에서만 접근하도록 구성한다. 본 섹션의 모든 근거는 MongoDB 공식 문서를 인용한다.

### 2.1 클러스터 티어 · 리전

| 환경 | 티어 | 리전 | 스토리지 | 샤드 |
|---|---|---|---|---|
| dev | M10 | AWS `AP_NORTHEAST_2` | 10 GB | 1 Replica Set |
| beta | M20 | AWS `AP_NORTHEAST_2` | 20 GB | 1 Replica Set |
| prod | M30 이상 | AWS `AP_NORTHEAST_2` | 40 GB+ (Auto-scaling ON) | 1 Replica Set (PRIMARY + 2 SECONDARY, 3 AZ) |

- Atlas 가 AWS `ap-northeast-2` 에 동일 리전 배포되므로 RTT 를 최소화한다 (MongoDB Atlas 공식 문서: *Configure a Cluster* — Cloud Provider Region 선택).
- Auto-scaling (storage, compute) 활성화 (MongoDB Atlas 공식 문서: *Auto-Scale Your Cluster*).

### 2.2 연결: AWS PrivateLink (필수)

- **방식**: MongoDB Atlas Private Endpoint(AWS PrivateLink)를 각 환경 VPC 의 **Private-Data 서브넷**에 생성 (MongoDB 공식 문서: *Set Up a Private Endpoint for a Dedicated Cluster*).
- **절차 요약**
  1. Atlas UI/API → Network Access → Private Endpoint → **AWS PrivateLink** 선택, Service Name 획득.
  2. AWS 콘솔 → VPC → Endpoint → Interface Endpoint 생성 (서비스 이름 입력, 서브넷 = Private-Data × 3 AZ, 보안그룹 = `sg-mongodb-atlas`).
  3. Atlas 에 Endpoint ID 입력하여 연동 완료.
  4. 애플리케이션은 `mongodb+srv://...-pl-0.<hash>.mongodb.net/...` 형태의 **PrivateLink DNS** 로 접속.
- **DNS 해석**: Interface Endpoint 의 `enableDnsSupport=true` 옵션으로 VPC Route 53 Resolver 가 자동 사설 해석한다 (MongoDB 공식 문서: *Private Endpoint Concepts → DNS*).
- **보안 그룹**: `sg-mongodb-atlas` 는 App 서브넷 SG 로부터 `TCP 1024-65535` 인바운드 허용 (PrivateLink 서비스 포트 동적).
- **IP Access List**: PrivateLink 전용 구성 시에도 Atlas 규정상 빈 IP Access List 또는 PrivateLink-only 설정으로 퍼블릭 접근을 차단한다 (MongoDB 공식 문서: *Configure IP Access List Entries*).

### 2.3 백업 스냅샷

- **Cloud Backup** 활성화 (MongoDB 공식 문서: *Back Up Your Cluster*).
- **스냅샷 정책 (prod)**
  - 시간별: 6시간마다, 2일 보존
  - 일별: 매일, 7일 보존
  - 주별: 매주, 4주 보존
  - 월별: 매월, 12개월 보존
- **Continuous Cloud Backup (Point-in-Time Restore)** 활성화 — 백업 윈도우 내 임의 시점 복원 가능. 정량 RPO는 공식 미보장(MongoDB 공식 문서: *Continuous Cloud Backups*). 본 설계는 보수적으로 **RPO 목표 ≤ 1분**으로 운영하고 분기별 복원 검증으로 실측.
- **크로스 리전 스냅샷 복사**: prod 는 `ap-northeast-1` 으로 스냅샷 복사 활성화 (MongoDB 공식 문서: *Back Up Cluster to Another Region*).

### 2.4 Vector Search 인덱스

현재 `VectorSearchIndexConfig.java` 에 정의된 스펙을 그대로 Atlas 에 적용한다. 소비자: `api-chatbot`, `api-emerging-tech`, `api-agent`.

- **인덱스 이름**: `vector_index_emerging_techs`
- **임베딩 모델**: OpenAI `text-embedding-3-small`
- **차원(numDimensions)**: **1536**
- **유사도(similarity)**: **cosine**
- **필터 필드**: `provider`, `status`, `published_at`, `update_type`

Atlas Vector Search 인덱스 정의(MongoDB 공식 문서: *Create an Atlas Vector Search Index*):

```json
{
  "fields": [
    { "type": "vector", "path": "embedding_vector", "numDimensions": 1536, "similarity": "cosine" },
    { "type": "filter", "path": "provider" },
    { "type": "filter", "path": "status" },
    { "type": "filter", "path": "published_at" },
    { "type": "filter", "path": "update_type" }
  ]
}
```

- **생성 방법**: Atlas UI 또는 Atlas CLI (`atlas clusters search indexes create ... --type vectorSearch`) — 기존 `VectorSearchIndexConfig.getAtlasCliCommand()` 헬퍼 사용.
- **ANN 설정**: langchain4j `MongoDbEmbeddingStore` 는 `$vectorSearch` 단계를 사용하며 `numCandidates` 는 `topK * 10–20` 권장 (MongoDB 공식 문서: *Atlas Vector Search — Run Vector Search Queries*). 현재 재랭킹 단계(`CohereReRankingServiceImpl`) 가 있어 `numCandidates=topK*15` 로 설정.
- **임베딩 업데이트 파이프라인**: `EmergingTechCommandServiceImpl` 에서 저장 시 자동 임베딩 생성 → Atlas 에 upsert → 인덱스가 비동기 재구성.

### 2.5 Spring Boot 연결 문자열 관리

- **원칙**: 현재 `application-mongodb-domain.yml` 에 평문으로 노출된 문자열(사용자/비밀번호 포함)은 **즉시 Secrets Manager 로 이관**한다.
- **Secret 구조** (예: `secret/tech-n-ai/<env>/mongodb-atlas`)

  ```json
  {
    "MONGODB_ATLAS_CONNECTION_STRING": "mongodb+srv://<user>:<pwd>@<cluster>-pl-0.<hash>.mongodb.net/tech_n_ai?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true&appName=tech-n-ai-<env>",
    "MONGODB_ATLAS_DATABASE": "tech_n_ai"
  }
  ```

- **주입 경로**: `spring-cloud-aws-secrets-manager` 또는 ECS/EKS External Secrets Operator 를 통해 환경변수로 주입 → 기존 `${MONGODB_ATLAS_CONNECTION_STRING}` 자리 표시자 재사용.
- **로테이션**: Atlas Database User 는 API 로 비밀번호 로테이션 가능 (MongoDB Atlas Admin API: *Update One Database User*). Lambda 로테이터를 작성하여 Secrets Manager 와 연동, **60일 주기**.
- **Read Preference**: `secondaryPreferred` 유지 (쓰기 후 읽기 지연 허용 가능 영역에만 사용. 강한 일관성 필요 시 `primary`).
- **클라이언트 옵션**: `maxPoolSize=100` (MongoDB 공식 문서: *MongoClient Settings*), `serverSelectionTimeoutMS=5000`, `connectTimeoutMS=10000`, `socketTimeoutMS=60000`.

### 2.6 RPO/RTO 충족 정리

| 지표 | 설계 | 값 |
|---|---|---|
| RPO | Continuous Cloud Backup | **≤ 1분** |
| RTO | Replica Set 자동 failover(몇 초~수십 초) + Atlas UI 에서 스냅샷 복원(수 분) | **≤ 30분** |

---

## 3. ElastiCache (cache-elasticache)

### 3.1 엔진 선정: Valkey vs Redis OSS

AWS 는 **ElastiCache for Valkey** 와 **ElastiCache for Redis OSS** 를 **별개의 엔진 옵션**으로 제공한다(ElastiCache 사용자 가이드: *Choosing between Valkey, Memcached, and Redis OSS self-designed caches*).

| 축 | ElastiCache for Valkey | ElastiCache for Redis OSS |
|---|---|---|
| 라이선스 | **BSD-3-Clause** (오픈소스) | Redis 7.4 이후 RSALv2/SSPLv1 (소스 공개 의무) |
| 호환성 | RESP 프로토콜·API 가 Redis OSS 와 호환 (Valkey 7.2 ⇄ Redis 7.2, Valkey 8.0 ⇄ Redis 7.2 슈퍼셋). 기존 Redis 클라이언트 그대로 사용 가능 | Redis 7.x 계열 |
| 가격 | **AWS 상 Valkey 온디맨드가 Redis OSS 대비 약 20% 저렴** (ElastiCache 사용자 가이드) | 표준 가격 |
| 로드맵 | AWS·Linux Foundation 주도, 활발 | Redis Inc. 주도 |
| Multi-AZ, RBAC, TLS, Auto-failover | 모두 지원 | 모두 지원 |

**선정: ElastiCache for Valkey 8.0** — BSD 라이선스, 가격 우위, Redis 클라이언트와 **무중단 호환** 이 근거. (`modules/elasticache-valkey/variables.tf` `engine_version` 기본값과 정합)

### 3.2 기존 Redis 사용처 Valkey 전환 검증

현재 소스를 점검한 5개 사용처 모두 **표준 RESP 명령(`GET/SET/DEL/HASKEY`, `opsForValue`)** 만 사용하며 Valkey 와 완전 호환된다.

| 사용처 | 파일 | 사용 명령 | Valkey 호환 |
|---|---|---|---|
| 공통 템플릿/직렬화 설정 | `common/core/config/RedisConfig.java` | `RedisTemplate` (Lettuce 드라이버) | **OK** |
| OAuth State | `api/auth/oauth/OAuthStateService.java` | `SET ... EX`, `GET`, `DEL` | **OK** |
| 챗봇 응답 캐시 | `api/chatbot/service/CacheServiceImpl.java` | `SET ... EX`, `GET`, `DEL` (JSON 직렬화) | **OK** |
| 멱등성 키 | `common/kafka/consumer/IdempotencyService.java` | `EXISTS`, `SET ... EX` | **OK** |
| Slack 레이트리밋 | `client/slack/util/SlackRateLimiter.java` | `GET`, `SET ... EX` | **OK** |

- Spring Data Redis(Lettuce) 는 Valkey 엔드포인트에 그대로 연결 가능하다(ElastiCache 사용자 가이드: *Getting started with ElastiCache for Valkey*). 클라이언트 측 코드 변경 **0 라인**.
- TLS/AUTH 설정만 환경변수(`spring.data.redis.ssl.enabled=true`, `spring.data.redis.password=<AUTH-TOKEN>`) 로 추가하면 된다.

### 3.3 클러스터 구성

| 환경 | 노드 타입 | 구성 | 클러스터 모드 | Multi-AZ | 출처 |
|---|---|---|---|---|---|
| dev | `cache.t4g.micro` | Primary 1 (`replicas=0`) | **Disabled** | OFF | `envs/dev/variables.tf` defaults (D-10) |
| beta | `cache.t4g.small` | Primary 1 + Replica 1 | **Disabled** | ON | `envs/beta/terraform.tfvars` |
| prod | `cache.t4g.small` | Primary 1 + Replica 1 (`num_node_groups=1`) | **Disabled** (단일 샤드) | ON | `envs/prod/terraform.tfvars` (D-10) |

- **Multi-AZ with Automatic Failover**: replica 가 있는 환경(beta/prod)만 ON. dev 는 비용 최소화 목적 단일 노드 (D-10 결정 — 시드 단계).
- **샤딩 결정**: 시드 단계에서는 **모든 환경 단일 샤드** 운영. 현재 사용처는 키 분리가 잘 되어 있어 **크로스슬롯 트랜잭션이 없음** → 향후 트래픽 증가 시 prod 만 클러스터 모드 활성화하고 `num_node_groups`를 늘려 수평 확장한다(별도 ADR). 클라이언트는 Lettuce 의 `useTopologyRefresh` + `ClusterClientOptions` 를 사전 활성화하여 무중단 전환 대비.
- **엔진 버전**: `valkey 8.0` (`modules/elasticache-valkey/variables.tf` 기본값과 정합. AWS는 Valkey 8.0 GA를 ElastiCache에 제공 — ElastiCache 사용자 가이드: *Supported ElastiCache engine versions for Valkey*).
- **스냅샷**: 매일 04:00 KST 자동 스냅샷, 보존 7일 (ElastiCache 사용자 가이드: *Backup and restore*).

### 3.4 네트워크 · 보안

- 배치: Private-Data 서브넷만, SG `sg-elasticache-<env>` 는 App SG 로부터 `6379/tcp` 인바운드.
- **전송 중 암호화(TLS)**: **ON**, 클라이언트는 `rediss://` 스킴.
- **저장 시 암호화**: KMS CMK(`alias/elasticache-<env>`) ON.
- **인증**
  - **현행 — 모든 환경 AUTH 토큰** (Secrets Manager `techai/{env}/elasticache-auth-token`, 90일 수동 회전 — 매트릭스 §4 정의처 준수). 코드 사실: `envs/{dev,beta,prod}/main.tf` 의 cache 모듈 호출 모두 `auth_mode = "auth_token"` (`modules/elasticache-valkey/variables.tf` `auth_mode` 기본값과 정합).
  - **prod 향후 — RBAC (User Group)** 전환 권장 (ElastiCache 사용자 가이드: *Authenticating users with Role-Based Access Control (RBAC)*). 도입 시 `auth_mode = "rbac"` + `rbac_user_group_ids` 지정. 사용자 그룹 예: `app-write-user` (+@all -@dangerous), `app-read-user` (+@read), `ops-admin` (+@all).
- **파라미터 그룹**: `maxmemory-policy=allkeys-lru` (캐시 용도), `notify-keyspace-events=""` (미사용), `cluster-require-full-coverage=no` (샤드 장애 시 부분 가용).

### 3.5 용도별 키 네임스페이스 · TTL

현재 소스의 TTL 을 그대로 채택하되 네임스페이스를 정규화한다.

| 용도 | 네임스페이스(Prefix) | TTL | 출처 | RBAC 사용자 |
|---|---|---|---|---|
| OAuth State | `oauth:state:{state}` | 10분 | `OAuthStateService` | app-write-user |
| JWT 블랙리스트 | `auth:jwt:bl:{jti}` | 토큰 만료시각까지 | `common-security` 정책(추가 예정) | app-write-user |
| 챗봇 응답 캐시 | `chatbot:cache:{hash}` | 1시간 (설정값) | `CacheServiceImpl` | app-write-user |
| Kafka 멱등성 | `processed_event:{eventId}` | 7일 | `IdempotencyService` | app-write-user |
| Slack 레이트리밋 | `rate-limit:slack:{identifier}` | 1분 | `SlackRateLimiter` | app-write-user |

- **키 네이밍 규약**: `{도메인}:{서브도메인}:{식별자}` 소문자 콜론 구분. 기존 코드의 상수와 일치한다.
- **이벌티션**: `allkeys-lru` → 캐시 외 데이터(멱등성, OAuth state) 도 TTL 로 자연 삭제되므로 메모리 압박 시 LRU 가 예측 가능하게 동작.

### 3.6 RPO/RTO 충족 정리

- 캐시 성격 데이터이므로 소실 시 애플리케이션 재계산으로 복구 가능(멱등성 키는 7일 TTL 중 잃어도 downstream 컨슈머가 idempotent 해야 함 — 별도 설계).
- **Automatic Failover**: 통상 수 초 내. **RTO ≤ 30분 충족**.

---

## 4. S3 오브젝트 스토리지 (object-storage)

### 4.1 버킷 설계 (환경 × 용도 분리)

명명 규약: `tech-n-ai-{env}-{purpose}-{region}` (전 세계 전역 유일). 예: `tech-n-ai-prod-user-uploads-apne2`.

| 버킷 | 용도 | Versioning | Lifecycle | Object Lock |
|---|---|---|---|---|
| `tech-n-ai-{env}-user-uploads-apne2` | 사용자 업로드(북마크 첨부, 아바타 등) | **ON** | Intelligent-Tiering 즉시, 노후 30일 Glacier IR | Off |
| `tech-n-ai-{env}-static-assets-apne2` | 공용 정적 자산 (admin/app 빌드 산출물 일부) | ON | 없음 (CloudFront 캐시) | Off |
| `tech-n-ai-{env}-app-logs-apne2` | App/ALB/CloudFront 로그 | ON | 30일 IA → 90일 Glacier IR → 365일 Deep Archive | Off |
| `tech-n-ai-{env}-backups-apne2` | Aurora 스냅샷 내보내기, DB 백업 아카이브 | ON | 즉시 Glacier IR | **Compliance 365일** |
| `tech-n-ai-{env}-iac-state-apne2` | Terraform state, lock | ON | 없음 | **Governance 90일** |
| `tech-n-ai-{env}-athena-results-apne2` | Athena 쿼리 결과 | Off | 7일 후 삭제 | Off |

### 4.2 공통 보안 정책

- **Block Public Access**: 모든 버킷 **4개 옵션 전부 ON** (계정 단위 + 버킷 단위) (S3 사용자 가이드: *Blocking public access to your Amazon S3 storage*).
- **Versioning**: 기본 ON. 의도적 예외(`athena-results`)만 OFF.
- **Object Lock**: 규제/백업 대상 버킷에 한해 **Compliance** 또는 **Governance** 모드 (S3 사용자 가이드: *Using S3 Object Lock*). 기본 보존 정책 설정.
- **암호화**: **SSE-KMS** 필수, KMS CMK 분리(`alias/s3-{env}-{purpose}`), **Bucket Key 활성화** (S3 사용자 가이드: *Reducing the cost of SSE-KMS with Amazon S3 Bucket Keys*) → KMS 호출 비용 최대 99% 절감.
- **TLS 강제**: 버킷 정책에 `aws:SecureTransport=false` 거부 조건.
- **ACL 비활성화**: `BucketOwnerEnforced` (S3 권장값).
- **Access Logging**: 각 버킷 → `app-logs` 버킷의 `s3-access/{bucket}/` 접두사로 기록.
- **Inventory**: 일일 CSV Inventory → Athena 분석 용도.

### 4.3 Lifecycle 정책

- **Intelligent-Tiering**: 접근 패턴 불확실 버킷(`user-uploads`) 기본 저장 클래스로 전환 — Frequent/Infrequent/Archive Instant Access/Archive Access/Deep Archive 자동 이동 (S3 사용자 가이드: *Amazon S3 Intelligent-Tiering*).
- **Glacier Instant Retrieval (GIR)**: 3–5개월 이후 접근 빈도 낮은 로그/백업. 밀리초 검색 유지.
- **Deep Archive**: 365일+ 장기 보관 (저비용, 12시간 검색).
- **Expiration**: Athena 결과 7일, 불완전 멀티파트 업로드 7일 삭제 공통 적용.

### 4.4 CloudFront 연동

- **OAI 금지, OAC 사용** (S3 사용자 가이드 / CloudFront 개발자 가이드: *Restricting access to an Amazon S3 origin — Use OAC*).
- CloudFront 배포 → `static-assets`, `user-uploads` (서명 URL 기반) 오리진 으로 OAC 연결.
- 버킷 정책은 OAC 의 서비스 프린시펄(`cloudfront.amazonaws.com`) + `aws:SourceArn` 조건으로 제한.
- 업로드 경로: **S3 Presigned URL** 로 직접 업로드 (백엔드는 URL 서명만 수행), 다운로드는 CloudFront 경유.

### 4.5 VPC Endpoint

- **Gateway Endpoint**: S3 에 대한 `com.amazonaws.ap-northeast-2.s3` Gateway VPC Endpoint 를 각 VPC 의 Private Route Table 에 연결하여 트래픽을 AWS 백본 내에서 처리.
- KMS, Secrets Manager, STS 는 Interface Endpoint.

---

## 5. 데이터 마이그레이션 (data-migration)

### 5.1 이관 대상

| 원천 | 대상 | 방식 |
|---|---|---|
| 로컬 MySQL 8 (docker-compose) | Aurora MySQL (prod) | **AWS DMS Full Load + CDC** |
| 로컬 MongoDB / 기존 Atlas dev 클러스터 | Atlas prod 클러스터 | **mongodump/mongorestore** 또는 **Atlas Live Migration Service** |
| Redis dev 데이터 | ElastiCache (Valkey) | **미이관** (캐시 성격, 재생성) |
| 기존 파일 | S3 | **AWS DataSync** |

### 5.2 AWS DMS 파이프라인 (Aurora)

- **네트워크 전제**: 온프레/로컬에서 AWS VPN/Direct Connect/Site-to-Site VPN 이 연결되어 있다고 가정(02 문서 참조). DMS replication instance 는 Private-Data 서브넷에 배치 (DMS 사용자 가이드: *Working with an AWS DMS replication instance*).
- **구성 요소**
  1. Source endpoint: 로컬 MySQL (SSL, read-only 계정, `binlog_format=ROW`, `binlog_row_image=FULL`, `log_slave_updates=ON` 요구 — DMS 사용자 가이드 *Using MySQL as a source for AWS DMS*).
  2. Target endpoint: Aurora MySQL (admin 권한 계정, Secrets Manager 연동).
  3. Replication instance: `dms.r6i.large` Multi-AZ.
  4. Migration task: **Full Load + CDC**, `rdsdbdata` 제외, 테이블 매핑 JSON 으로 스키마/테이블 화이트리스트.
- **Schema Conversion**: 원본이 MySQL 8 이므로 **AWS SCT 미필요**. 단, Aurora 전용 예약어·파라미터 차이를 위해 SCT **Assessment Report** 만 사전 실행 (AWS SCT 사용자 가이드).
- **검증**: DMS **Data Validation** 기능으로 Full Load 후 행 단위 해시 비교 (DMS 사용자 가이드: *Data validation task*).

### 5.3 MongoDB Atlas 이관

옵션 A(동일 Atlas 프로젝트 내 복제): **Atlas Live Migration**(MongoDB 공식 문서: *Migrate Data into Atlas — Live Migration*).
옵션 B(로컬 → Atlas): `mongodump` → S3 업로드 → `mongorestore` 또는 **mongosync**(MongoDB 공식 문서: *mongosync*). 실시간성이 필요한 경우 mongosync 권장.

### 5.4 컷오버 계획 (prod)

| 단계 | 시간 | 작업 | 롤백 트리거 |
|---|---|---|---|
| T-14d | | Aurora/Atlas/ElastiCache/S3 프로비저닝, PrivateLink·SG·IAM 완료 | — |
| T-7d | | DMS Full Load(읽기/쓰기 정상 서비스 유지), 검증 리포트 | 행 불일치 > 0 |
| T-7d~T-1d | | DMS **CDC** 지속, lag 메트릭 모니터링 | lag > 60s 지속 |
| T-1h | | **점검 모드**: API Gateway Maintenance Response, 쓰기 트래픽 차단 | — |
| T-30m | | 마지막 CDC flush 확인(lag=0), 애플리케이션 write 중단 | lag ≠ 0 |
| T-15m | | Flyway 최종 마이그레이션 적용(필요 시), Atlas mongosync stop, 데이터 최종 검증(샘플 SELECT/COUNT) | 검증 실패 |
| T-0 | | DNS/환경변수 전환 (`MONGODB_ATLAS_CONNECTION_STRING`, Aurora Writer/Reader URL), 애플리케이션 롤링 재기동 | p95 지연 2× 초과 |
| T+15m | | 헬스체크 + 관측성 대시보드 green | 에러율 > 1% |
| T+30m | | 점검 모드 해제, 트래픽 복귀 | — |
| T+24h | | DMS 태스크 정지/삭제, 로컬 DB 읽기 전용 아카이브 | — |

- **롤백**: 로컬 DB 는 읽기 전용으로 유지, 구 DNS 레코드 TTL=60s 로 낮춰둠. 치명 장애 시 애플리케이션 설정만 이전 URL 로 재배포.
- **관측**: 컷오버 중 CloudWatch 에서 **Aurora Writer CPU/Commit Latency**, **Atlas Ops Counters**, **ElastiCache Engine CPU**, **ALB 5xx** 를 실시간 모니터.

---

## 6. 베스트 프랙티스 체크리스트 매핑

| 요구 | 충족 여부 | 근거 섹션 |
|---|---|---|
| 모든 데이터 스토어 Private-Data 서브넷 | OK | §1.6, §2.2, §3.4 |
| 저장/전송 암호화(KMS CMK) | OK | §1.6, §2.2, §3.4, §4.2 |
| 자동 백업 + 크로스 리전 스냅샷 복제(prod) | OK | §1.5, §2.3 |
| Secrets Manager 로 자격 증명 로테이션 | OK | §1.7, §2.5, §3.4 |
| Performance Insights / Enhanced Monitoring | OK | §1.8 |
| Aurora Backtrack 또는 PITR 전략 결정 | PITR + 스냅샷 채택, Backtrack 은 dev/beta 보조 | §1.5 |
| RPO ≤ 5분 / RTO ≤ 30분 (prod) | Aurora §1.9, Atlas §2.6, Cache §3.6 모두 충족 | — |

---

## 7. 참고 자료 (공식 출처만)

- Amazon Aurora 사용자 가이드 — https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/
- Aurora MySQL 모범 사례 — https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.BestPractices.html
- Aurora Global Database — https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database.html
- Aurora I/O-Optimized — https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-storage-cluster-configs.html
- ElastiCache for Valkey — https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/WhatIs.html
- ElastiCache RBAC — https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/Clusters.RBAC.html
- Amazon S3 사용자 가이드 — https://docs.aws.amazon.com/AmazonS3/latest/userguide/
- S3 보안 모범 사례 — https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html
- S3 Object Lock — https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html
- CloudFront OAC — https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html
- AWS DMS 사용자 가이드 — https://docs.aws.amazon.com/dms/latest/userguide/
- AWS Backup 사용자 가이드 — https://docs.aws.amazon.com/aws-backup/latest/devguide/
- AWS Secrets Manager 로테이션 — https://docs.aws.amazon.com/secretsmanager/latest/userguide/rotating-secrets.html
- MongoDB Atlas Documentation — https://www.mongodb.com/docs/atlas/
- MongoDB Atlas AWS PrivateLink — https://www.mongodb.com/docs/atlas/security-private-endpoint/
- MongoDB Atlas Vector Search — https://www.mongodb.com/docs/atlas/atlas-vector-search/
- MongoDB Atlas Cloud Backups — https://www.mongodb.com/docs/atlas/backup/cloud-backup/overview/
