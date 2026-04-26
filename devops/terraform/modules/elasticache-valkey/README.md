# `modules/elasticache-valkey` — ElastiCache Valkey

> 09 §5.4 + 04 §3 spec 구현. Valkey 8.0 (Redis OSS API 호환). 환경별 노드 타입과 복제본 수를 변수로 분기.

## 사용 예 — dev (단일 노드, 비용 최소)

```hcl
module "cache" {
  source = "../../modules/elasticache-valkey"

  project     = "techai"
  environment = "dev"

  vpc_id          = module.network.vpc_id
  data_subnet_ids = module.network.data_subnet_ids
  kms_key_arn     = aws_kms_key.data.arn

  node_type                = "cache.t4g.micro"
  replicas_per_node_group  = 0    # 단일 노드 — 비용 최소, 페일오버 없음
  multi_az_enabled         = false
  snapshot_retention_limit = 0    # dev 백업 미사용
  auth_mode                = "auth_token"
}
```

## 사용 예 — prod (현행: Multi-AZ + AUTH 토큰)

```hcl
module "cache" {
  source = "../../modules/elasticache-valkey"

  project     = "techai"
  environment = "prod"

  vpc_id          = module.network.vpc_id
  data_subnet_ids = module.network.data_subnet_ids
  kms_key_arn     = aws_kms_key.data.arn

  node_type                = "cache.t4g.small"
  replicas_per_node_group  = 1
  multi_az_enabled         = true
  snapshot_retention_limit = 7
  auth_mode                = "auth_token"   # 현행 — `envs/prod/main.tf` 와 정합
}
```

> **prod RBAC 전환 권장(향후)**: `auth_mode = "rbac"` + `rbac_user_group_ids = [aws_elasticache_user_group.prod.id]`. 사용자 그룹과 RBAC User 리소스를 envs/prod 에 별도 정의해야 한다(별도 ADR).

## AUTH 토큰 vs RBAC

| 항목 | AUTH 토큰 | RBAC |
|---|---|---|
| 단순도 | ★★★ | ★★ |
| 권한 분리 | 단일 토큰 (모든 권한) | 사용자별 역할·키스페이스 제한 |
| 현행 | dev / beta / prod 모두 적용 | (미적용 — prod 전환 후보) |
| 회전 | 90일 수동 (매트릭스 §4) | User 단위 |

본 모듈의 AUTH 토큰은 `random_password` 로 자동 생성되고 Terraform state 에 sensitive 로 보관된다. envs 의 호출부에서 출력값을 Secrets Manager 시크릿(`techai/{env}/elasticache-auth-token` — 매트릭스 §4 정의처)에 저장한다.

## 비용 (서울 리전, 24/7)

| 노드 타입 | 단가 | replica=0 | replica=1 (Multi-AZ) |
|---|---|---|---|
| `cache.t4g.micro` | $0.022/h | $16/월 | $32/월 |
| `cache.t4g.small` | $0.043/h | $31/월 | $62/월 |

dev 환경에서 replica=0 + micro 면 약 $16/월. 사용자 0 단계에서 충분.
