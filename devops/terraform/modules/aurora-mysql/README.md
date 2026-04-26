# `modules/aurora-mysql` — Aurora MySQL 클러스터

> 09 §5.3 spec 구현. Provisioned (prod) 와 Serverless v2 (dev/beta) 두 모드를 같은 모듈로 처리.

## 사용 예 — dev (Serverless v2, 비용 최소)

```hcl
module "aurora" {
  source = "../../modules/aurora-mysql"

  project     = "techai"
  environment = "dev"

  vpc_id          = module.network.vpc_id
  data_subnet_ids = module.network.data_subnet_ids
  kms_key_arn     = aws_kms_key.data.arn

  engine_mode               = "serverlessv2"
  serverlessv2_min_capacity = 0.5
  serverlessv2_max_capacity = 2.0

  db_name = "techai"

  allowed_security_group_ids = [
    module.api_auth.security_group_id,
    module.api_bookmark.security_group_id,
  ]

  backup_retention_period = 1
  deletion_protection     = false
  skip_final_snapshot     = true
  storage_type            = "aurora"
}
```

## 사용 예 — prod (Provisioned, Multi-AZ + I/O-Optimized)

```hcl
module "aurora" {
  source = "../../modules/aurora-mysql"

  project     = "techai"
  environment = "prod"

  vpc_id          = module.network.vpc_id
  data_subnet_ids = module.network.data_subnet_ids
  kms_key_arn     = aws_kms_key.data.arn

  engine_mode    = "provisioned"
  instance_count = 3   # 04 §1.3 prod 권장 — Writer 1 + Reader 2 (3 AZ 분산)
  instance_class = "db.r7g.large"

  db_name = "techai"

  allowed_security_group_ids = [...]

  backup_retention_period       = 30
  performance_insights_enabled  = true
  deletion_protection           = true
  storage_type                  = "aurora-iopt1"
}
```

## 모드 비교

| 항목 | Serverless v2 | Provisioned |
|---|---|---|
| 비용 모델 | ACU-시간 (0.5 ACU 부터, $0.12/ACU-h 서울) | 인스턴스-시간 |
| 최소 비용 (24/7) | 0.5 ACU × $0.12 × 720 ≈ **$43/월** | db.r7g.large × $0.21 × 720 ≈ **$151/월** × 인스턴스 수 |
| 스케일링 | 0.5~256 ACU 자동 | 수동 (인스턴스 수 변경) |
| Cold start | 없음 (warm pool) | 없음 |
| 권장 | dev/beta 또는 가변 트래픽 | prod 정상 부하 |

## 비밀번호 관리 — Managed Master User Password

본 모듈은 `manage_master_user_password = true` 를 강제한다. 결과:

1. RDS 가 비밀번호를 자동 생성하고 Secrets Manager 시크릿에 저장.
2. 시크릿 자동 회전 (기본 7일) — Lambda 자체 구현 불필요.
3. Terraform state 에 비밀번호가 들어가지 않음.

api-auth 가 시크릿을 읽으려면 [매트릭스 §4 — `tech-n-ai/{env}/aurora-credentials`] 에 정의된 Task Role 권한이 필요. 본 모듈 출력 `master_user_secret_arn` 을 IAM 정책의 Resource 에 넣는다.

## IAM DB Authentication

`iam_database_authentication_enabled = true` 가 기본값. api-auth Task Role 이 RDS 토큰을 발급받아 비밀번호 없이 인증할 수 있다 — 이 경우 위의 master_user_secret 은 마이그레이션·관리자 작업에만 사용.

토큰 발급 IAM 정책 예 (api-auth Task Role 인라인):

```json
{
  "Effect": "Allow",
  "Action": "rds-db:connect",
  "Resource": "arn:aws:rds-db:ap-northeast-2:<ACCOUNT>:dbuser:<ClusterResourceId>/api_auth_user"
}
```

`<ClusterResourceId>` 는 본 모듈 출력 `cluster_resource_id` 에서 가져온다.

## 주의

- `final_snapshot_identifier` 에 `timestamp()` 를 사용해 매 plan 마다 변경 발생 — `lifecycle.ignore_changes` 로 무시.
- `engine_version` 도 자동 마이너 패치 무시.
- prod 변경은 `apply_immediately = false` 로 다음 maintenance window 까지 대기.
