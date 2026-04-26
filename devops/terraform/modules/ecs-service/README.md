# `modules/ecs-service` — Fargate 서비스

> 09 §5.2 + 03 §2 spec 구현. ALB Target Group(blue/green) + ECS Service + CodeDeploy + 워크로드 SG + Auto Scaling + 자동 롤백 알람.

## 무엇을 만드는가

| 자원 | 개수 |
|---|---|
| `aws_security_group` (워크로드 SG) | 1 |
| `aws_lb_target_group` (blue/green) | 2 |
| `aws_lb_listener_rule` | 1 |
| `aws_ecs_task_definition` | 1 (이후 CodeDeploy 가 revision 생성) |
| `aws_ecs_service` | 1 (deployment_controller=CODE_DEPLOY) |
| `aws_codedeploy_app` + `aws_codedeploy_deployment_group` | 1+1 |
| Auto Scaling Target + 2 Policy (CPU, Memory) | 1+2 |
| CloudWatch Alarm (5xx rate, p95 latency) — 자동 롤백 | 2 |
| CloudWatch Log Group | 1 (자동 생성 모드) |
| CodeDeploy 서비스 Role | 1 |

## 사용 예 — api-auth (8083)

```hcl
module "api_auth" {
  source = "../../modules/ecs-service"

  project     = "techai"
  environment = "dev"
  service_name = "api-auth"

  cluster_arn        = aws_ecs_cluster.main.arn
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids

  # 단일 ECR 리포 (D-1) — digest 참조 권장
  container_image = "${var.ecr_registry}/techai/api-auth@${var.api_auth_digest}"
  container_port  = 8083
  cpu             = 512
  memory          = 1024
  desired_count   = 2

  task_role_arn      = module.task_role_api_auth.role_arn
  execution_role_arn = module.task_execution_role.role_arn

  alb_listener_arn       = aws_lb_listener.https.arn
  alb_security_group_id  = aws_security_group.alb.id
  listener_rule_priority = 100
  listener_path_patterns = ["/auth/*"]

  log_kms_key_arn = aws_kms_key.logs.arn

  environment_vars = [
    { name = "SPRING_PROFILES_ACTIVE",                 value = "dev" },
    { name = "MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED", value = "true" },
  ]

  secrets_arn_map = {
    AURORA_PASSWORD = module.aurora.master_user_secret_arn
    JWT_SIGNING_KEY = aws_secretsmanager_secret.jwt.arn
  }

  autoscaling_min_count = 2
  autoscaling_max_count = 6
}
```

## 워크로드 SG cross-reference (envs 계층에서)

본 모듈은 자기 SG 만 만들고, **다른 워크로드와의 인바운드 규칙은 envs 에서 별도로 추가**한다 (단일 정의처 원칙).

```hcl
# envs/dev/main.tf 일부
resource "aws_security_group_rule" "auth_from_gateway" {
  type                     = "ingress"
  from_port                = 8083
  to_port                  = 8083
  protocol                 = "tcp"
  source_security_group_id = module.api_gateway.security_group_id
  security_group_id        = module.api_auth.security_group_id
  description              = "api-gateway → api-auth 8083"
}
```

## 자동 롤백 안전망 (D-2 — Hook 람다 미사용)

- ALB Target Group health check `/actuator/health/readiness` (HTTP 200)
- ALB 5xx 비율 알람 (1% over 2분)
- Target Response Time p95 (1.5s over 3분)
- CodeDeploy `auto_rollback_configuration.events = [DEPLOYMENT_FAILURE, DEPLOYMENT_STOP_ON_ALARM]`

세 알람 중 어느 하나라도 트리거되면 즉시 이전 revision 으로 복귀.

## 주의

- `container_image` 는 digest 형태(`@sha256:...`) 를 권장. 태그 형태는 immutable 가정이 깨질 수 있음.
- 모듈은 readonly_root_filesystem 을 false 로 둔다 — Spring Boot 가 `/tmp` 를 사용하기 때문. 보안 강화 시 `/tmp` mount 추가 후 true 가능.
