# `modules/msk-serverless` — MSK Serverless (dev/beta 전용)

> 09 §5.5 + 05 §1.1 spec 구현. IAM Access Control 전용. prod 는 [msk-provisioned](../msk-provisioned/) 사용.

## 사용 예 — dev (사용 시점에 활성화)

```hcl
module "msk" {
  count  = var.enable_msk ? 1 : 0    # 비용 토글
  source = "../../modules/msk-serverless"

  project     = "techai"
  environment = "dev"

  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids

  allowed_security_group_ids = [
    module.api_emerging_tech.security_group_id,
    module.api_bookmark.security_group_id,
    module.batch_source.security_group_id,
  ]
}
```

## 비용 주의

MSK Serverless 는 다음 비용이 합산된다:

| 항목 | 단가 (서울) | dev MVP 추정 |
|---|---|---|
| 클러스터-시간 | $0.75/h | $540/월 |
| 파티션-시간 | $0.0015/h × 파티션 수 | 토픽 4종 × 파티션 6 ≈ $26/월 |
| 데이터 I/O | $0.10/GB | 트래픽 시점 |

**합계 ≈ 월 $570** — dev 에 거의 항상 켜두면 부담스러움.

**권장**: `enable_msk = false` 로 두고 실제 사용 시점에 활성화.

## 인증

IAM SASL 만 지원. Spring Boot 클라이언트 설정:

```yaml
spring.kafka:
  bootstrap-servers: ${MSK_BOOTSTRAP}
  properties:
    security.protocol: SASL_SSL
    sasl.mechanism: AWS_MSK_IAM
    sasl.jaas.config: software.amazon.msk.auth.iam.IAMLoginModule required;
    sasl.client.callback.handler.class: software.amazon.msk.auth.iam.IAMClientCallbackHandler
```

ECS Task Role 에 `kafka-cluster:Connect`, `DescribeCluster`, `ReadData`, `WriteData`, `DescribeTopic`, `DescribeGroup`, `AlterGroup` 권한 필요 (실제 적용 예: `envs/{beta,prod}/task_roles.tf` 의 `task_role_api_agent` 모듈 — 05 §1.4 IAM 정책 예 참조).
