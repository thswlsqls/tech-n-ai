# `modules/msk-provisioned` — MSK Provisioned (prod, Kafka 3.9 KRaft)

> 09 §5.6 + 05 §1.2 spec 구현. Kafka 3.9.x KRaft (D-8 결정). dev/beta 는 [msk-serverless](../msk-serverless/) 사용.

## 사용 예 — prod

```hcl
module "msk" {
  source = "../../modules/msk-provisioned"

  project     = "techai"
  environment = "prod"

  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.data_subnet_ids   # Private-Data 권장
  kms_key_arn        = aws_kms_key.data.arn

  kafka_version          = "3.9.x.kraft"
  broker_count           = 3
  broker_instance_type   = "kafka.m7g.large"
  ebs_volume_size        = 500

  enable_open_monitoring = true
  enhanced_monitoring    = "PER_TOPIC_PER_PARTITION"

  allowed_security_group_ids = [
    module.api_emerging_tech.security_group_id,
    module.api_bookmark.security_group_id,
    module.batch_source.security_group_id,
    module.api_agent.security_group_id,
  ]
}
```

## Configuration 정책

본 모듈이 강제하는 server.properties (KRaft 호환, `main.tf:106-128`):

| 키 | 값 | 사유 |
|---|---|---|
| `auto.create.topics.enable` | `false` | 운영 위생 — 토픽은 명시적으로만 생성 |
| `default.replication.factor` | `3` | 3 AZ × 1 broker 기준 |
| `min.insync.replicas` | `2` | RF=3 + ISR=2 으로 1 AZ 장애 허용하면서 데이터 안전성 보장 |
| `num.partitions` | `6` | 기본 파티션 수 (오버라이드 가능) |
| `unclean.leader.election.enable` | `false` | 데이터 손실 위험 차단 |
| `delete.topic.enable` | `true` | 운영 자동화 |
| `log.retention.hours` | `168` (7일) | 04 §1 retention |
| `log.segment.bytes` | `1073741824` (1 GiB) | 세그먼트 크기 표준값 (Kafka Docs 기본 권장) |
| `num.replica.fetchers` | `4` | ISR 동기화 throughput (MSK Best Practices) |
| `transaction.state.log.replication.factor` | `3` | 트랜잭션 로그 안전성 (`__transaction_state`) |
| `transaction.state.log.min.isr` | `2` | 트랜잭션 로그 ISR=2 보장 |
| `offsets.topic.replication.factor` | `3` | `__consumer_offsets` 안전성 |

## 인증

- IAM SASL (port 9098) — 워크로드 권장
- TLS (port 9094) — 외부 파트너 mTLS 옵션 (보류)
- Plain text 미지원

ECS Task Role 정책 예 (api-agent 가 conversation 토픽 produce·consume):

```json
{
  "Effect": "Allow",
  "Action": [
    "kafka-cluster:Connect",
    "kafka-cluster:DescribeCluster",
    "kafka-cluster:WriteData",
    "kafka-cluster:ReadData",
    "kafka-cluster:DescribeTopic",
    "kafka-cluster:DescribeGroup",
    "kafka-cluster:AlterGroup"
  ],
  "Resource": [
    "<CLUSTER_ARN>",
    "<CLUSTER_ARN_PREFIX>/topic/tech-n-ai.conversation.*",
    "<CLUSTER_ARN_PREFIX>/group/tech-n-ai.*"
  ]
}
```

## Open Monitoring (Prometheus)

`enable_open_monitoring = true` 면 JMX Exporter (11001) + Node Exporter (11002) 가 활성. 08 관측성 모듈의 Managed Prometheus(AMP) 또는 자체 Prometheus 가 스크레이프.

## 비용 (서울, 24/7)

| 항목 | 단가 | 합계 |
|---|---|---|
| `kafka.m7g.large` × 3 | $0.21/h × 3 × 720 | ~$453/월 |
| EBS gp3 500GB × 3 | $0.114/GB-월 × 1500 | ~$171/월 |
| 데이터 전송 | 트래픽 비례 | — |
| **합계** | | **~$624/월** |

dev/beta 가 Serverless 인 이유 — Provisioned 의 고정비 부담 회피.

## EBS Storage Auto Scaling

`enable_storage_autoscaling = true` (디폴트) 시 Application Auto Scaling 으로 브로커 EBS 가 자동 확장된다.

| 변수 | 디폴트 | 설명 |
|---|---|---|
| `enable_storage_autoscaling` | `true` | 활성 여부 |
| `storage_autoscaling_max_size` | `1000` (GB) | 브로커당 상한 |
| `storage_autoscaling_target_utilization` | `70` (%) | 사용률 목표 (`KafkaBrokerStorageUtilization`) |

(`main.tf:219-244`)

## 변경 시 주의

- `kafka_version` 자동 마이너 패치는 `lifecycle.ignore_changes` 로 무시 (`main.tf:207-212`).
- Configuration 변경(`server.properties`)은 `aws_msk_configuration` 의 새 revision 을 만들고 클러스터에 reapply 됨 — 롤링 업데이트로 다운타임 없음. 모듈은 `create_before_destroy = true` 로 안전하게 교체 (`main.tf:125-127`).
- 인스턴스 타입 변경은 롤링 (수십 분 소요).
- `broker_count` 증가는 가능, 감소는 불가 — 새 클러스터 생성 후 마이그레이션.
