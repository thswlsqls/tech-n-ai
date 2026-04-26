# `modules/iam-role-workload` — 표준 워크로드 Role

> 09 §5.8 spec 구현. ECS Task Role 6+1, Task Execution Role, Lambda 등에 모두 사용.

## 사용 예 — ECS Task Role (api-auth)

```hcl
module "task_role_api_auth" {
  source = "../../modules/iam-role-workload"

  project       = "techai"
  environment   = "dev"
  workload_name = "api-auth"
  trust_service = "ecs-tasks.amazonaws.com"

  trust_conditions = [
    {
      test     = "StringEquals"
      variable = "aws:SourceAccount"
      values   = [data.aws_caller_identity.current.account_id]
    }
  ]

  inline_policies = {
    secrets-read = data.aws_iam_policy_document.api_auth_secrets.json
    rds-iam-auth = data.aws_iam_policy_document.api_auth_rds.json
  }
}
```

## 사용 예 — ECS Task Execution Role

```hcl
module "task_execution_role" {
  source = "../../modules/iam-role-workload"

  project            = "techai"
  environment        = "dev"
  workload_name      = "task-execution"
  role_name_override = "techai-dev-task-execution"  # 단일 공유 Role
  trust_service      = "ecs-tasks.amazonaws.com"

  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy",
  ]

  inline_policies = {
    secrets-read = data.aws_iam_policy_document.execution_secrets.json
  }
}
```

## 주요 옵션

- `trust_service` — `*.amazonaws.com` 형식 (validation, `variables.tf:31-34`)
- `trust_conditions` — `aws:SourceAccount` 같은 confused-deputy 방지 조건 (06 §2.2 신뢰 정책 가이드)
- `inline_policies` (map) / `managed_policy_arns` (list) — 둘 다 부착 가능
- `permissions_boundary_arn` — 개발자 생성 Role 상한 강제 (06 §2.1)
- `max_session_duration` — 디폴트 3600s

Role 이름 디폴트: `{project}-{environment}-task-{workload_name}` (`main.tf:6-7`). `role_name_override` 로 단일 공유 Role(예: task-execution) 명명 가능.

## 입출력

`variables.tf`, `outputs.tf` 참조. `terraform-docs` 가 본 README 에 자동 주입한다.
