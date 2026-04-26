# 05. 메시징 · Kafka(MSK) 설계

> 본 문서는 Tech-N-AI 시스템의 CQRS 동기화(Aurora → MongoDB Atlas)와 도메인 이벤트 전달을 위한 **Amazon MSK 기반 메시징 플랫폼** 설계를 다룬다.
> 기존 코드(`common/kafka/`)에 하드코딩된 토픽(`tech-n-ai.conversation.session.created/updated/deleted`, `tech-n-ai.conversation.message.created`)과 `KafkaConfig.java`의 Producer/Consumer 설정을 그대로 전제로 한다.

**공식 출처**
- Amazon MSK Developer Guide — https://docs.aws.amazon.com/msk/latest/developerguide/
- Amazon MSK Best Practices — https://docs.aws.amazon.com/msk/latest/developerguide/bestpractices.html
- Amazon MSK Serverless — https://docs.aws.amazon.com/msk/latest/developerguide/serverless.html
- Amazon MSK Connect — https://docs.aws.amazon.com/msk/latest/developerguide/msk-connect.html
- AWS Glue Schema Registry — https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html
- Apache Kafka Documentation — https://kafka.apache.org/documentation/
- Spring for Apache Kafka Reference — https://docs.spring.io/spring-kafka/reference/
- Debezium MySQL Connector — https://debezium.io/documentation/reference/stable/connectors/mysql.html

---

## 1. MSK 클러스터 설계 (msk-cluster-design)

### 1.1 MSK Provisioned vs MSK Serverless 비교 · 선정

| 항목 | MSK Provisioned | MSK Serverless |
|---|---|---|
| 용량 모델 | 브로커 수/인스턴스 타입/EBS 크기를 사용자가 결정 | 용량 자동 스케일(최대 200 MiB/s 수신, 400 MiB/s 송신/클러스터) |
| 인증 | IAM / SASL/SCRAM / mTLS 모두 지원 | **IAM Access Control 전용** |
| 최대 파티션 | 인스턴스 타입별 가이드(예: `m7g.large` 1,000 파티션/브로커) | 클러스터당 **non-compacted 2,400 / compacted 120 파티션**, 파티션 자동 할당 ([MSK Serverless quotas](https://docs.aws.amazon.com/msk/latest/developerguide/limits.html)) |
| 네트워크 | VPC Peering, Private Link, Public Access 선택 | VPC 내부 전용 (Private) |
| 스키마 레지스트리 | Glue Schema Registry 연동 가능 | Glue Schema Registry 연동 가능 |
| Open Monitoring | Prometheus JMX/Node Exporter 지원 | 미지원(CloudWatch 메트릭만) |
| MSK Connect | 지원 | 지원 |
| 요금 모델 | 브로커 시간당 + EBS GB-월 | 클러스터-시간 + 파티션-시간 + 데이터 I/O(GB) |
| KRaft/ZooKeeper | Kafka 3.7+ 선택 시 **KRaft 가능**(Zk 유지 선택도 가능) | **KRaft 고정(사용자 선택 불가)** |

**트래픽 예측 (가정)**

| 구간 | 이벤트 TPS(전 토픽 합) | 평균 메시지 크기 | 피크 수신 대역 | 비고 |
|---|---|---|---|---|
| 초기(MVP) | 50 TPS | 2 KB | ~0.1 MB/s | conversation 토픽 4종 중심 |
| 중기 | 500 TPS | 2 KB | ~1 MB/s | bookmark/auth 이벤트 추가, CDC 트래픽 포함 |
| 최대(1년 내) | 2,000 TPS | 3 KB | ~6 MB/s | agent 실행 이벤트 포함 |

**선정: `dev`/`beta`는 MSK Serverless, `prod`는 MSK Provisioned**

근거:
1. `dev`/`beta`는 트래픽이 매우 낮고 운영 부담 최소화가 목표 → Serverless의 용량 자동 스케일과 무관리 운영 이점이 큼.
2. `prod`는 ①Prometheus Open Monitoring 기반 상세 SLI 관측(아래 4장), ②Kafka 3.9.x (AWS 권장 LTS — extended 2년 지원, ZooKeeper·KRaft 양 지원 마지막 버전) 기준 **KRaft 모드 신규 구축과 파라미터 튜닝(세그먼트 크기, `num.replica.fetchers` 등)** 필요, ③향후 mTLS/SASL 혼합 인증 요구 가능성 → **Provisioned**가 유리.
3. Serverless는 IAM 인증 전용이므로 `prod`의 외부 파트너 연동(향후 mTLS 요구) 확장성이 제한됨.
4. 요금 관점에서 `prod` 최대 부하(6 MB/s)는 Provisioned **3AZ × `kafka.m7g.large`**(§1.2의 RF=3 결정과 정합)가 Serverless 대비 저렴할 수 있음 — 정확한 비교는 AWS Pricing Calculator로 워크로드별 산출(MSK Pricing, Seoul 리전 기준).

### 1.2 클러스터 사양 (prod, Provisioned)

| 항목 | 값 | 근거 |
|---|---|---|
| Kafka 버전 | **3.9.x** | MSK Developer Guide: *Supported Apache Kafka versions*. 3.9.x는 AWS 권장 LTS(extended 2년 지원, ZooKeeper·KRaft 양 지원 마지막 버전). 4.0+는 ZooKeeper 모드 deprecation 시작이므로 신규 구축은 보수적으로 3.9.x KRaft 채택 |
| 메타데이터 모드 | **KRaft** | Provisioned + Kafka 3.7+ 선택 시 적용. ZooKeeper 의존 제거, 컨트롤러 failover 수초 단축 |
| 브로커 수 | **3 (AZ 당 1, 총 3 AZ)** | RF=3 + `min.insync.replicas=2` 충족 + AZ 장애 허용 |
| 인스턴스 타입 | **`kafka.m7g.large`** | ARM Graviton3 기반, 2 vCPU / 8 GiB. Best Practices의 "≤1,000 파티션/브로커" 가이드 내 |
| EBS 유형 | **gp3** | `kafka.m7g.large`는 gp3 지원. 기본 3,000 IOPS / 125 MB/s |
| EBS 크기 | **500 GB/브로커** | Retention 7일 × 평균 1 MB/s × 3 RF × 3 brokers × 안전계수 2 ≈ 450 GB → 500 GB |
| AZ 분산 | **ap-northeast-2a/b/c** | 서울 리전 3 AZ |
| Auto Scaling | **EBS 스토리지 Auto Scaling 활성** | `TargetUtilizationPercentage=70`, Max 1,000 GB |
| Transit Encryption | **TLS In-Cluster + TLS Client** | TLS only (평문 허용 금지) |
| At-Rest Encryption | **KMS CMK (`alias/techai/prod-data`)** | 고객 관리형 키, 키 로테이션 연 1회 |
| Enhanced Monitoring | **PER_TOPIC_PER_PARTITION** | Under-replicated/lag/latency 세밀 관측 |
| Open Monitoring | **JMX Exporter + Node Exporter** | Prometheus 스크레이프 엔드포인트 활성 |

`dev`/`beta` (Serverless)

| 항목 | 값 |
|---|---|
| 클러스터 당 최대 처리량 | 기본 쿼터(수신 200 MiB/s, 송신 400 MiB/s) |
| 인증 | IAM 전용 (AWS_MSK_IAM SASL mechanism) |
| Kafka 버전 | MSK가 자동 관리 (Serverless는 버전 선택 불가) |

> **KRaft 주의(프롬프트 전제 재확인)**: Serverless는 내부적으로 KRaft를 사용하며 사용자 결정 불필요. 본 문서의 KRaft 적용은 **prod(Provisioned) + Kafka 3.9.x** 한정.

### 1.3 네트워크 배치

- **서브넷**: 3 AZ × `subnet-private-data-{a,b,c}` (문서 02와 동일한 Private-Data 티어)
- **보안 그룹 규칙**

| SG | Inbound | Outbound |
|---|---|---|
| `sg-msk-broker` | TCP 9098(IAM)/9094(TLS) from `sg-api-service`, `sg-msk-connect` | All to VPC |
| `sg-api-service` | — | TCP 9098/9094 to `sg-msk-broker`, 443 to `glue-schema-registry` VPCE |
| `sg-msk-connect` | — | TCP 9098 to `sg-msk-broker`, 3306 to `sg-aurora`, 443 to `s3`/`glue` VPCE |

- **VPC Endpoint**
  - `com.amazonaws.ap-northeast-2.glue` (Glue Schema Registry)
  - `com.amazonaws.ap-northeast-2.kafka` (MSK 관리 API)
  - `com.amazonaws.ap-northeast-2.s3` (MSK Connect 커넥터 플러그인 로드)
- **Spring Boot 클라이언트 접속**

```yaml
spring:
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP}   # b-1.techai.xxxx.kafka.ap-northeast-2.amazonaws.com:9098,...
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: AWS_MSK_IAM
      sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
      sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

### 1.4 인증/인가 비교 · 선정

| 방식 | 장점 | 단점 | 적용 여부 |
|---|---|---|---|
| **IAM Access Control** | AWS IAM 정책으로 토픽/그룹 ACL 통합, 키 로테이션 자동, Serverless 지원 | Kafka 네이티브 ACL 도구(`kafka-acls.sh`) 직접 사용 불가 | **채택 (주)** |
| mTLS (ACM PCA) | 표준 Kafka 인증, 외부 파트너 연동 시 유리 | PCA 비용(월 $400+), 인증서 수명/회전 운영 부담 | **보류** (파트너 연동 발생 시 추가) |
| SASL/SCRAM | 단순, 레거시 클라이언트 호환 | Secrets Manager + KMS 관리, 비밀번호 회전 스크립트 필요 | 미채택 |

**선정: IAM Access Control**

모든 환경에서 IAM을 주 인증으로 사용. Serverless는 IAM 필수이며, Provisioned도 IAM을 선택해 운영 일관성 확보.

**토픽 패턴별 최소 권한 IAM 정책 예(Conversation 퍼블리셔)**

> 클러스터 이름은 모듈 디폴트(`{project}-{env}-msk-sl` for Serverless, `{project}-{env}-msk` for Provisioned — `modules/msk-serverless/main.tf:7-9`, `modules/msk-provisioned/main.tf:7-10`)를 따른다. 아래는 prod Provisioned (`techai-prod-msk`) 예시.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["kafka-cluster:Connect", "kafka-cluster:DescribeCluster"],
      "Resource": "arn:aws:kafka:ap-northeast-2:<acct>:cluster/techai-prod-msk/*"
    },
    {
      "Effect": "Allow",
      "Action": ["kafka-cluster:WriteData", "kafka-cluster:DescribeTopic"],
      "Resource": "arn:aws:kafka:ap-northeast-2:<acct>:topic/techai-prod-msk/*/tech-n-ai.conversation.*"
    },
    {
      "Effect": "Allow",
      "Action": ["kafka-cluster:ReadData", "kafka-cluster:DescribeGroup", "kafka-cluster:AlterGroup"],
      "Resource": [
        "arn:aws:kafka:ap-northeast-2:<acct>:topic/techai-prod-msk/*/tech-n-ai.conversation.*",
        "arn:aws:kafka:ap-northeast-2:<acct>:group/techai-prod-msk/*/chatbot-conversation-sync"
      ]
    }
  ]
}
```

- 서비스별 IAM Role 명: `iam-role-workload` 모듈 디폴트(`{project}-{env}-task-{workload_name}`) → `techai-prod-task-api-chatbot`, `...-api-bookmark`, `...-api-auth`, `...-api-agent` (`modules/iam-role-workload/main.tf:6-7`). MSK Connect 용 `techai-prod-task-msk-connect-debezium` 은 도입 시점에 별도 생성.
- 토픽 패턴별로 Produce/Consume 범위를 분리(예: chatbot은 `tech-n-ai.conversation.*`만 Read/Write).
- **현재 IaC 갭**: `envs/prod/task_roles.tf:158-189` 의 `api-agent` 만 MSK 권한을 보유. 본 문서가 가정하는 `api-chatbot`/`api-bookmark`/`api-auth` MSK Producer/Consumer 권한은 미적용 — 실제 Kafka 사용 시점에 task_roles 갱신 필요.

### 1.5 암호화

| 구간 | 방식 | 비고 |
|---|---|---|
| In-Transit (client ↔ broker) | **TLS 1.2+ (SASL_SSL)** | 평문 포트(9092) 비활성 |
| In-Transit (broker ↔ broker) | **TLS In-Cluster** | MSK 콘솔 `encryptionInTransit.inCluster=true` |
| At-Rest (로그 세그먼트/EBS) | **KMS CMK (`alias/techai/prod-data`)** | AWS 관리 키가 아닌 CMK. 키 정책에 `role-msk-service` 허용 |
| 클라이언트 측 암호화 | **대화 원문(message content)만 애플리케이션 레이어에서 KMS Envelope 암호화 후 publish** | 민감도 높은 사용자 발화 보호. `ConversationMessageCreatedEvent.content` 필드 대상 |

---

## 2. 토픽 & 스키마 (topic-and-schema)

### 2.1 네이밍 규칙 결정 — ADR-005-001

**배경**
- 기존 코드(`EventConsumer.java:22`)가 다음 토픽을 하드코딩: `tech-n-ai.conversation.session.created`, `...session.updated`, `...session.deleted`, `tech-n-ai.conversation.message.created`
- 패턴: `{product}.{domain}.{entity}.{event}` (버전/환경 접두사 없음)
- 환경 구분은 "클러스터 분리"로 이미 처리(`dev`/`beta`/`prod` 각각 독립 MSK 클러스터)

**옵션 A — 기존 패턴 유지 (`{product}.{domain}.{entity}.{event}`)**

| 항목 | 내용 |
|---|---|
| 장점 | 기존 코드·설정 무변경, 환경 분리는 클러스터 레벨로 명확, 토픽명 짧음 |
| 장점 | `spring.kafka.consumer.topics` 환경변수 기반 오버라이드 이미 구현되어 있어 마이그레이션 리스크 0 |
| 단점 | 스키마 버전을 토픽명에 담지 않아, 비호환 변경 시 **신규 토픽 생성 + 코드 분기** 필요 |
| 단점 | 크로스 리전 복제(MirrorMaker2) 도입 시 `{cluster}.{topic}` 접두사가 자동 부여되어 혼란 가능 |

**옵션 B — 재정의 (`{env}.{product}.{domain}.{entity}.{event}.v{n}`)**

| 항목 | 내용 |
|---|---|
| 장점 | 토픽명만으로 환경·버전 식별 가능, 동일 계정 공용 클러스터 시나리오 대비 |
| 장점 | 스키마 진화(v1 → v2) 시 토픽 공존 가능, Dual-write 패턴으로 안전한 마이그레이션 |
| 단점 | 기존 하드코딩 4개 토픽 리네이밍 필요 → 다운타임 없는 마이그레이션 계획 동반 필수 |
| 단점 | 클러스터 분리가 이미 환경 구분을 해결하므로 `{env}` 접두사가 **중복** |
| 단점 | Debezium CDC의 `topic.prefix` 설정을 재정렬해야 함 |

**결정: 옵션 A 유지 + 버전 서픽스만 선택 도입 (`.v{n}`은 breaking change 발생 시에만)**

근거:
1. 환경은 클러스터 분리(`dev`/`beta`/`prod` 각각 전용 MSK)로 충분히 격리되며, `{env}` 접두사는 중복.
2. 기존 4개 토픽 리네이밍 비용(Dual-consume, 오프셋 복구, 배포 동시성 조율) 대비 얻는 이득이 제한적.
3. 스키마 진화는 **Glue Schema Registry의 BACKWARD 호환성**으로 처리하고, 깨지는 변경이 실제로 발생한 경우에 한해 **`.v2` 서픽스 토픽 신설**(예: `tech-n-ai.conversation.message.created.v2`)로 공존·커트오버.
4. 향후 도메인 확장 시에도 동일 패턴 준수: `tech-n-ai.{domain}.{entity}.{event}`.

**ADR 상태**: Accepted, Owner: Platform Team, Review: 반기 1회

### 2.2 초기 토픽 카탈로그

**A. 실제 코드에 존재하는 토픽 (유지)**

| 토픽 | 유형 | Partition | RF | Retention | Cleanup | Key | 소비자 | 출처 |
|---|---|---|---|---|---|---|---|---|
| `tech-n-ai.conversation.session.created` | 이벤트 스트림 | 6 | 3 | 7일 | `delete` | `sessionId` | `chatbot-conversation-sync` | `EventConsumer.java:22` |
| `tech-n-ai.conversation.session.updated` | 이벤트 스트림 | 6 | 3 | 7일 | `delete` | `sessionId` | `chatbot-conversation-sync` | 동일 |
| `tech-n-ai.conversation.session.deleted` | 이벤트 스트림 | 6 | 3 | 7일 | `delete` | `sessionId` | `chatbot-conversation-sync` | 동일 |
| `tech-n-ai.conversation.message.created` | 이벤트 스트림 | 12 | 3 | 7일 | `delete` | `sessionId` | `chatbot-conversation-sync` | 동일 |

**B. 추가 필요 — 향후 도메인 이벤트 설계 가이드**

아래 토픽들은 현재 코드에 없으며, `BaseEvent`/`UserDeletedEvent`(이미 정의됨) 패턴과 `scripts/ api-{auth,bookmark,agent}` 모듈 구조를 근거로 예측한 것이다. 실제 도입 시 ADR로 확정한다.

| 토픽 (예정) | 유형 | Partition | RF | Retention | Cleanup | Key |
|---|---|---|---|---|---|---|
| `tech-n-ai.auth.user.created` | 이벤트 스트림 | 6 | 3 | 14일 | `delete` | `userId` |
| `tech-n-ai.auth.user.updated` | 이벤트 스트림 | 6 | 3 | 14일 | `delete` | `userId` |
| `tech-n-ai.auth.user.deleted` | 이벤트 스트림 | 3 | 3 | 30일 | `delete` | `userId` |
| `tech-n-ai.auth.user.state` | **Compacted** | 6 | 3 | 무제한 | `compact` | `userId` |
| `tech-n-ai.bookmark.folder.created` | 이벤트 스트림 | 3 | 3 | 7일 | `delete` | `userId` |
| `tech-n-ai.bookmark.item.created` | 이벤트 스트림 | 6 | 3 | 7일 | `delete` | `userId` |
| `tech-n-ai.bookmark.item.deleted` | 이벤트 스트림 | 3 | 3 | 7일 | `delete` | `userId` |
| `tech-n-ai.agent.execution.started` | 이벤트 스트림 | 12 | 3 | 3일 | `delete` | `executionId` |
| `tech-n-ai.agent.execution.completed` | 이벤트 스트림 | 12 | 3 | 7일 | `delete` | `executionId` |
| `tech-n-ai.agent.execution.failed` | 이벤트 스트림 | 6 | 3 | 14일 | `delete` | `executionId` |
| `cdc.aurora.techai.*` | CDC (Debezium) | 6 | 3 | 3일 | `delete` | PK |
| `<topic>.dlq` | DLQ | 3 | 3 | 30일 | `delete` | 원본 Key 유지 |

### 2.3 파티션/복제 정책

**공통**
- **Replication Factor = 3**, **`min.insync.replicas = 2`**, Producer `acks=all` (Apache Kafka Docs 권장)
- `unclean.leader.election.enable = false`

**파티션 수 결정 기준** (Kafka Docs "How to choose the number of partitions for a topic")
```
partitions = ceil( target_throughput_MBps / min(producer_MBps_per_partition, consumer_MBps_per_partition) )
```
- 보수적으로 파티션당 처리량 5 MB/s로 가정
- 순서 보장 단위 Key(`sessionId`, `userId`, `executionId`)별로 단일 파티션 → 동일 Key의 이벤트 순서 보장

**토픽별 산정 근거**

| 토픽 | 피크 TPS | 평균 크기 | 필요 대역 | 파티션 | 비고 |
|---|---|---|---|---|---|
| `...session.created/updated/deleted` | 100 | 1 KB | 0.1 MB/s | 6 | 여유분 포함, 소비자 Concurrency 3까지 수용 |
| `...message.created` | 500 | 3 KB | 1.5 MB/s | 12 | 대화 메시지 피크 대응 |
| `...auth.user.state` (compacted) | 10 | 2 KB | — | 6 | 사용자 현재 상태 KTable용 |
| `...agent.execution.*` | 200 | 5 KB | 1 MB/s | 12 | Long-running 작업, 병렬 소비 필요 |

### 2.4 스키마 관리 — AWS Glue Schema Registry

**포맷 선정: Avro**

| 포맷 | 장점 | 단점 | 결정 |
|---|---|---|---|
| Avro | 스키마-데이터 분리, Glue 네이티브, 스키마 진화 룰 명확(BACKWARD/FORWARD/FULL) | Jackson 3 JSON 직렬화 대비 스키마 컴파일 필요 | **채택** |
| JSON Schema | 기존 Jackson 3 코드와 호환, 학습 곡선 낮음 | 바이너리 효율 낮음, Union 타입 표현 제한 | — |
| Protobuf | 최고 효율 | `.proto` 빌드 파이프라인 추가 필요, Glue는 지원하나 스키마 진화 규칙 단순 | — |

> 현재 코드는 `JacksonJsonSerializer`(JSON)를 사용 중이므로, **단기는 JSON으로 운영하되 Glue Schema Registry는 JSON Schema 모드로 먼저 도입**하고, 스키마 안정화 이후 Avro로 전환하는 **2단계 마이그레이션**을 권장.

**호환성 모드: BACKWARD (Glue 기본값)**
- BACKWARD: 신규 스키마를 사용하는 **컨슈머**가 과거 메시지를 읽을 수 있어야 함
- 근거: 본 시스템은 **프로듀서 선배포 → 컨슈머 후배포** 순서를 강제하기 어렵고, 재처리(offset reset)가 흔함. BACKWARD가 재처리 안전성 보장.

**Schema Registry 설정 (Terraform 예시)**

```hcl
resource "aws_glue_registry" "techai" {
  registry_name = "techai-${var.env}"
}

resource "aws_glue_schema" "conversation_message_created" {
  schema_name       = "tech-n-ai.conversation.message.created"
  registry_arn      = aws_glue_registry.techai.arn
  data_format       = "JSON"             # 1단계: JSON, 이후 AVRO로 전환
  compatibility     = "BACKWARD"
  schema_definition = file("${path.module}/schemas/conversation_message_created_v1.json")
}
```

**`ConversationMessageCreatedEvent` JSON Schema (v1) 예**

```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ConversationMessageCreatedEvent",
  "type": "object",
  "required": ["eventId", "eventType", "occurredAt", "sessionId", "messageId", "role", "content"],
  "properties": {
    "eventId":    { "type": "string", "format": "uuid" },
    "eventType":  { "type": "string", "const": "conversation.message.created" },
    "occurredAt": { "type": "string", "format": "date-time" },
    "sessionId":  { "type": "string" },
    "messageId":  { "type": "string" },
    "role":       { "type": "string", "enum": ["USER", "ASSISTANT", "SYSTEM"] },
    "content":    { "type": "string" },
    "metadata":   { "type": "object", "additionalProperties": true }
  },
  "additionalProperties": false
}
```

### 2.5 Retention / Compaction 구분

| 분류 | 대상 | `cleanup.policy` | Retention | 근거 |
|---|---|---|---|---|
| **이벤트 스트림** | `conversation.*`, `bookmark.*`, `agent.execution.*` | `delete` | 3~14일 | 재처리 창을 1주일 이상 보장, 디스크 비용 상한 |
| **이벤트 소싱용 Compacted** | `auth.user.state` (예정) | `compact` | 무제한 | 최신 상태 스냅샷 유지(KTable) |
| **DLQ** | `<topic>.dlq` | `delete` | 30일 | 수동 재처리 가능한 충분한 기간 |
| **CDC 스트림** | `cdc.aurora.techai.*` | `delete` | 3일 | MongoDB 적재 실패 시 짧은 재처리 창 |

---

## 3. 컨슈머 & DLQ (consumer-and-dlq)

### 3.1 컨슈머 그룹 네이밍 — `{service}-{purpose}`

| 서비스 | 용도 | 그룹 ID | 구독 토픽 |
|---|---|---|---|
| api-chatbot | Aurora→Mongo 동기화 | `chatbot-conversation-sync` | `tech-n-ai.conversation.*` |
| api-chatbot | RAG 인덱싱 | `chatbot-rag-indexer` | `tech-n-ai.conversation.message.created` |
| api-auth | 사용자 상태 KTable | `auth-user-state-builder` | `tech-n-ai.auth.user.*` |
| api-bookmark | 검색 인덱싱 | `bookmark-search-indexer` | `tech-n-ai.bookmark.*` |
| api-agent | 실행 결과 수집 | `agent-execution-tracker` | `tech-n-ai.agent.execution.*` |

> **현재 기본값** `${spring.application.name:tech-n-ai-group}` (`application-kafka.yml:20`, `KafkaConfig.java:38` 의 `@Value` 디폴트도 `tech-n-ai-group`) — Spring `application.name` 이 지정되어 있으면 자동으로 서비스별 그룹이 되며, 미지정 시 `tech-n-ai-group` 으로 폴백. 위 매트릭스의 `chatbot-conversation-sync` 같은 *목적별* 그룹은 `spring.kafka.consumer.group-id` 를 명시 override 해 분리.

### 3.2 멱등성 & 재처리

**Producer (이미 적용됨)** — `KafkaConfig.java`
- `enable.idempotence = true` (`KafkaConfig.java:92`)
- `acks = all` (`application-kafka.yml:9` 에서 이미 `all`. `KafkaConfig.java:53` 의 `@Value` 디폴트는 `1` 이지만 yml override 로 `all` 적용)
- `retries = Integer.MAX_VALUE` 권장 (현재 yml 기본 3)
- `max.in.flight.requests.per.connection = 5` (`KafkaConfig.java:91`, idempotent 하에서 순서 보장 유지)

**트랜잭션 (CDC/Outbox 패턴용)**
- `spring.kafka.producer.transaction-id-prefix: techai-tx-` 추가
- `ProducerFactory.setTransactionIdPrefix(...)` + `@Transactional(KafkaTransactionManager)` 로 Aurora 쓰기+Kafka publish 원자화 (Spring Kafka Reference: "Transactions")
- Consumer는 `isolation.level = read_committed` 유지 (이미 `KafkaConfig.java:119`에 적용)

**컨슈머 측 멱등성 (이미 구현됨)** — `IdempotencyService`
- `EventConsumer.consume()`가 `idempotencyService.isEventProcessed(eventId)`로 중복 검사
- 저장소는 Redis 사용. 현재 구현(`IdempotencyService.java:13,14`)은 키 prefix `processed_event:`, TTL 7일(`Duration.ofDays(7)`) — `RedisTemplate.hasKey` + `opsForValue().set(..., 7d)` 조합 (원자적 SETNX 가 아님은 향후 개선 포인트)

### 3.3 DLQ 설계

**전략: Kafka DLQ 토픽 (`{topic}.dlq`)**

이유:
- 동일 Kafka 생태계에서 재처리 툴링 일관성
- SQS 대비 순서/키 보존 가능
- DLQ 자체가 또 다른 Kafka consumer 대상이 되어 알람·리드라이브 자동화 용이

**Spring Kafka `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` 설정**

```java
@Bean
public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
    );
    // 고정 간격 재시도: 1초, 2초, 4초 (3회) → 모두 실패 시 DLQ
    ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
    backOff.setInitialInterval(1000L);
    backOff.setMultiplier(2.0);
    backOff.setMaxInterval(10_000L);

    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
    // 역직렬화/스키마 오류는 즉시 DLQ (재시도 불필요)
    handler.addNotRetryableExceptions(
        DeserializationException.class,
        SchemaValidationException.class,
        IllegalArgumentException.class
    );
    return handler;
}
```

- `kafkaListenerContainerFactory.setCommonErrorHandler(errorHandler)` 연결
- DLQ 토픽은 **원본 파티션·오프셋·예외 정보를 헤더로 보존**(`DeadLetterPublishingRecoverer` 기본 동작)

### 3.4 백프레셔/재시도 파라미터

| 파라미터 | 값 | 근거 |
|---|---|---|
| `max.poll.records` | 500 (현재값) | 대용량 배치로 throughput 확보 |
| `max.poll.interval.ms` | 600,000 (10분, 현재값) | Long-running 핸들러(LLM 호출 등) 대응 |
| `fetch.min.bytes` | 1 (기본) | 지연 최소화 |
| `session.timeout.ms` | 45,000 | Heartbeat 기반 rebalancing 여유 |
| `heartbeat.interval.ms` | 3,000 | `session.timeout.ms`의 1/15 이하 |
| Container `concurrency` | 3 (현재값) | 파티션 6~12개 토픽에서 3 컨슈머로 시작, 파티션 수 이하로 유지 |
| `AckMode` | MANUAL (현재값) | `acknowledgment.acknowledge()` 로 명시적 커밋, 실패 시 재전달 |

> **백프레셔**: 하류(예: MongoDB 쓰기)가 느려지면 `max.poll.records` 감소 또는 `pause()`/`resume()` 수동 호출. Spring Kafka 3.x의 `MessageListenerContainer.pausePartition()` 활용.

### 3.5 CDC 파이프라인 — Aurora → Kafka

**옵션 비교**

| 항목 | MSK Connect + Debezium MySQL | DMS → Kinesis Data Streams |
|---|---|---|
| 변경 단위 | Row-level CDC (binlog 기반) | Row-level CDC (binlog 기반) |
| 타겟 | Kafka 토픽 (MSK) 직행 | Kinesis → Lambda/Firehose → Kafka 재게시 필요 |
| 스키마 진화 | Debezium + Schema Registry 네이티브 연동 | 자체 관리, JSON 고정 |
| Outbox 패턴 지원 | **Debezium Outbox SMT 공식 제공** | 미지원 (애플리케이션에서 별도 구현) |
| 운영 | MSK Connect 워커(매니지드) | DMS 인스턴스 + Kinesis + Lambda (구성 요소 많음) |
| 비용 | MCU-시간 + Connect 작업자 | DMS vCPU-시간 + Kinesis Shard + Lambda 호출 |
| 복구 시 포지션 | `database.history.kafka.topic`에 스키마 히스토리 | DMS 체크포인트 |
| Kafka 에코시스템 통합 | **네이티브** | Bridge 필요 |

**선정: MSK Connect + Debezium MySQL Connector**

근거:
1. 타겟이 Kafka이므로 Kinesis 경유 브릿지는 불필요한 이중 운영.
2. Outbox SMT를 통해 Aurora 단일 트랜잭션 내 Outbox 테이블 insert만으로 보장된 이벤트 발행 가능 — CQRS 일관성의 골든 패턴.
3. Schema Registry 연동이 커넥터 설정 1줄.

> **현재 백엔드 코드 갭**: `common/kafka/` 에 Outbox 테이블 / 엔티티 / 트랜잭션 publisher 가 아직 없음(`EventPublisher.java` 가 `KafkaTemplate.send` 직접 호출). 본 절의 `table.include.list` 의 `techai.outbox_event` 는 도입 예정 테이블이며, MSK Connect 도입 단계 이전에 백엔드에 Outbox 패턴을 먼저 추가해야 한다.

**Aurora MySQL 사전 설정**
- `binlog_format = ROW`
- `binlog_row_image = FULL`
- `binlog_expire_logs_seconds = 86400` (1일 이상)
- Debezium 전용 DB 사용자: `debezium` (권한: `SELECT, RELOAD, SHOW DATABASES, REPLICATION SLAVE, REPLICATION CLIENT`)

**MSK Connect 커넥터 설정 예**

```json
{
  "name": "techai-aurora-debezium",
  "connector.class": "io.debezium.connector.mysql.MySqlConnector",
  "tasks.max": "1",
  "database.hostname": "aurora-writer.cluster-xxx.ap-northeast-2.rds.amazonaws.com",
  "database.port": "3306",
  "database.user": "${secretsmanager:techai/debezium:username}",
  "database.password": "${secretsmanager:techai/debezium:password}",
  "database.server.id": "184054",
  "topic.prefix": "cdc.aurora.techai",
  "database.include.list": "techai",
  "table.include.list": "techai.outbox_event,techai.conversation_session,techai.conversation_message",
  "schema.history.internal.kafka.bootstrap.servers": "${MSK_BOOTSTRAP}",
  "schema.history.internal.kafka.topic": "cdc.aurora.techai.schema-history",
  "include.schema.changes": "true",
  "snapshot.mode": "initial",
  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.route.topic.replacement": "tech-n-ai.${routedByValue}",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "com.amazonaws.services.schemaregistry.kafkaconnect.jsonschema.AWSKafkaJsonSchemaConverter",
  "value.converter.region": "ap-northeast-2",
  "value.converter.registry.name": "techai-prod",
  "value.converter.schemaAutoRegistrationEnabled": "false"
}
```

- 커넥터 IAM: `role-msk-connect-debezium` (MSK IAM + Secrets Manager Read + Glue Schema Registry Read/Write)
- **바이너리 로그 포지션**: Debezium이 `schema.history.internal.kafka.topic`과 오프셋 토픽에 자동 기록, Connector 재시작 시 복구 가능.

---

## 4. 관측성 & 운영 (observability-and-ops)

### 4.1 메트릭 수집

**Prometheus Open Monitoring (Provisioned 전용)**
- 브로커에 JMX Exporter(포트 11001), Node Exporter(포트 11002) 자동 구동
- Managed Prometheus(AMP) 또는 자체 Prometheus에서 스크레이프
- 주요 JMX 메트릭: `kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions`, `kafka.network:type=RequestMetrics,name=TotalTimeMs,request=Produce`, `kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec`

**CloudWatch (Provisioned + Serverless 공통)**
- 네임스페이스 `AWS/Kafka`
- Provisioned 상세 레벨: `PER_TOPIC_PER_PARTITION`
- Serverless 메트릭: `BytesInPerSec`, `BytesOutPerSec`, `MessagesInPerSec`, `FetchMessageConversionsPerSec` 등 (클러스터·토픽 단위)

### 4.2 SLI / SLO

| SLI | 측정 | 목표 (SLO) | 출처 |
|---|---|---|---|
| Under-replicated partitions | `UnderReplicatedPartitions` (JMX) | **0 (99.9% 시간)** | Kafka Docs "Monitoring" |
| Offline partitions | `OfflinePartitionsCount` | **0 (100%)** | Kafka Docs |
| Consumer lag (max per group) | `MaxOffsetLag` (CloudWatch `EstimatedMaxTimeLag`) | **< 30s (p95), < 120s (p99)** | MSK CloudWatch 메트릭 |
| Produce request latency (p99) | `TotalTimeMs{request=Produce}` | **< 200 ms** | Kafka Docs |
| Broker CPU (User) | `CpuUser` | **< 60% (평균), < 80% (5분)** | MSK Best Practices |
| Disk used | `KafkaDataLogsDiskUsed` | **< 70%** | MSK Best Practices |
| Active controller count | `ActiveControllerCount` | **= 1 (100%)** | Kafka Docs |

### 4.3 알람 임계치 (CloudWatch Alarms 초안)

| 알람 | 조건 | Severity | 액션 |
|---|---|---|---|
| `MSK-URP` | `UnderReplicatedPartitions > 0` for 5 min | **Critical** | PagerDuty → 온콜 |
| `MSK-OfflinePartitions` | `OfflinePartitionsCount > 0` for 1 min | **Critical** | PagerDuty + Slack #ops |
| `MSK-DiskUsed` | `KafkaDataLogsDiskUsed > 70` for 15 min | High | Slack #ops, 수동 확인 |
| `MSK-DiskUsed-Emergency` | `KafkaDataLogsDiskUsed > 85` for 5 min | **Critical** | PagerDuty (자동 스케일업 실패 대비) |
| `MSK-CPUUser` | `CpuUser > 80` for 15 min | High | Slack #ops |
| `MSK-ActiveController` | `ActiveControllerCount != 1` for 5 min | **Critical** | PagerDuty |
| `MSK-ConsumerLag-chatbot-sync` | `SumOffsetLag > 10000` for 10 min | High | Slack, 스케일아웃 고려 |
| `MSK-Connect-TaskFailed` | Connector task state `FAILED` | **Critical** | PagerDuty |
| `MSK-DLQ-Messages` | `{topic}.dlq` `MessagesInPerSec > 0` 지속 10분 | High | Slack #ops + Runbook |

### 4.4 Rolling 업그레이드 절차 (Provisioned)

> MSK Provisioned의 브로커 패치/버전 업그레이드는 **MSK가 자동 Rolling** 수행. 사용자 책임은 **사전 점검 + 릴리스 모니터링**.

**절차**

1. **사전 점검 (D-7)**
   - `UnderReplicatedPartitions = 0`, `OfflinePartitionsCount = 0` 확인
   - `min.insync.replicas = 2`, RF=3 충족 토픽 목록 확인
   - 클라이언트 Kafka 클라이언트 버전이 대상 브로커 버전과 호환되는지 검증 (Kafka Docs "Upgrading")
   - DLQ 백로그 0 확인
2. **스테이징 적용 (D-3)**
   - `dev` → `beta` 순서로 동일 버전 업그레이드 수행
   - 24시간 관찰(컨슈머 lag, produce latency p99)
3. **운영 적용 (D-Day, 트래픽 저점 시간대)**
   - MSK 콘솔 → Cluster → "Update cluster configuration" 또는 버전 업그레이드
   - MSK가 브로커를 **한 번에 하나씩 재시작** (Rolling)
   - 평균 소요: 브로커당 10~15분, 3 브로커 기준 **30~45분**
4. **실시간 모니터링**
   - Prometheus/CloudWatch 대시보드에서 `UnderReplicatedPartitions`, `ActiveControllerCount`, `ProduceTotalTimeMs(p99)`, 각 Consumer group `MaxOffsetLag`
   - Alert: URP > 0 지속 5분 시 즉시 롤백 검토
5. **사후 검증 (D+1)**
   - 주요 비즈니스 지표(대화 세션 생성 성공률, 메시지 지연) 비교
   - DLQ 증가 여부 확인

**KRaft 전환 시 추가 주의**
- ZooKeeper → KRaft 마이그레이션은 **비가역**. 롤백 불가.
- 사전에 **스테이징 클러스터에서 전체 마이그레이션 리허설** 필수.
- Controller quorum 수 (Provisioned 3 브로커 기준 3 컨트롤러)

**설정 변경의 Rolling 적용 (`server.properties`)**
- MSK의 "Cluster configuration" 리비전 업데이트도 Rolling으로 적용됨
- 변경 전후 `kafka-configs.sh --describe`로 비교 (MSK IAM 인증으로 VPC 내부 bastion에서 실행)

### 4.5 운영 베스트 프랙티스 체크리스트

- [x] RF=3, `min.insync.replicas=2`, `acks=all`
- [x] 3 AZ 분산 브로커 배치
- [x] IAM 인증 + SG 제한 + TLS 동시 적용
- [x] Glue Schema Registry 도입으로 스키마 드리프트 방지
- [x] Consumer lag 알람 (`chatbot-conversation-sync` 기준)
- [ ] 멀티 테넌트 쿼터(`kafka-configs.sh --entity-type users --alter --add-config producer_byte_rate=...`) — 외부 파트너 연동 시점에 도입
- [x] EBS Auto Scaling (70% 임계)
- [x] Open Monitoring (Prometheus) + CloudWatch 이중 관측

---

## 부록 A. 애플리케이션 설정 변경 요약 (현재 코드 → MSK 전환)

| 파일 | 현재 값 | 변경 값 | 이유 |
|---|---|---|---|
| `application-kafka.yml` `spring.kafka.bootstrap-servers` | `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}` | MSK 부트스트랩 (9098) — `KAFKA_BOOTSTRAP_SERVERS` env 주입 | MSK IAM 엔드포인트 |
| `application-kafka.yml` `spring.kafka.producer.acks` | `all` (이미 적용) | 변경 불필요 | `min.insync.replicas=2` + 데이터 유실 방지 |
| `application-kafka.yml` `spring.kafka.producer.properties.security.protocol` | 없음 | `SASL_SSL` | MSK IAM |
| `application-kafka.yml` `spring.kafka.producer.properties.sasl.mechanism` | 없음 | `AWS_MSK_IAM` | MSK IAM |
| `application-kafka.yml` `spring.kafka.consumer.group-id` | `${spring.application.name:tech-n-ai-group}` (즉, 서비스명 우선) | 그대로 유지 — 서비스별로 자동 격리 | 서비스 단위 격리 (이미 구현) |
| `spring.kafka.producer.transaction-id-prefix` | 없음 | `techai-tx-` | Outbox 트랜잭션 사용 시 |
| `KafkaConfig.java` DLQ ErrorHandler | 없음 | `DefaultErrorHandler + DeadLetterPublishingRecoverer` | 3.3절 |

> `EventConsumer.java:22`의 하드코딩 토픽 목록은 **그대로 유지**. 환경변수 `spring.kafka.consumer.topics`로 override 가능하도록 이미 설계되어 있음.

## 부록 B. ADR 목록

- **ADR-005-001** 토픽 네이밍 규칙 — 옵션 A(기존 유지) + 필요 시 `.v{n}` 서픽스. Status: Accepted.
- **ADR-005-002** MSK 형태 선정 — `dev`/`beta` Serverless, `prod` Provisioned + KRaft (Kafka 3.9.x — AWS 권장 LTS, ZooKeeper·KRaft 양 지원 마지막 버전). Status: Accepted.
- **ADR-005-003** 인증 방식 — IAM Access Control 채택, mTLS 보류. Status: Accepted.
- **ADR-005-004** 스키마 포맷 — 1단계 JSON Schema + Glue Registry, 2단계 Avro 전환. Status: Accepted.
- **ADR-005-005** CDC 도구 — MSK Connect + Debezium MySQL + Outbox SMT. Status: Accepted.
