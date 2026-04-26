# `modules/observability` — Log Groups + 표준 알람 + Overview Dashboard

> 09 §5.10 + 08 §5 spec 의 IaC 부분 구현. ADOT Collector / Fluent Bit YAML 은 세션 4 에서 별도 산출.

## 사용 예

```hcl
module "observability" {
  source = "../../modules/observability"

  project     = "techai"
  environment = "dev"

  log_kms_key_arn = aws_kms_key.logs.arn

  log_groups = [
    { name = "/aws/lambda/techai-dev-secret-rotator", retention_days = 30 },
  ]

  alarm_sns_topic_arn = aws_sns_topic.alerts.arn

  service_alarms = [
    { cluster_name = "techai-dev", service_name = "techai-dev-api-gateway" },
    { cluster_name = "techai-dev", service_name = "techai-dev-api-auth" },
    { cluster_name = "techai-dev", service_name = "techai-dev-api-chatbot" },
    { cluster_name = "techai-dev", service_name = "techai-dev-api-bookmark" },
    { cluster_name = "techai-dev", service_name = "techai-dev-api-agent" },
    { cluster_name = "techai-dev", service_name = "techai-dev-api-emerging-tech" },
  ]
}
```

## 알람 카탈로그 (서비스당)

| 알람 | 메트릭 | 임계값 (기본) | 윈도우 |
|---|---|---|---|
| `*-cpu-high` | `AWS/ECS` `CPUUtilization` | > 80% | 3 × 1분 |
| `*-mem-high` | `AWS/ECS` `MemoryUtilization` | > 85% | 3 × 1분 |
| `*-running-low` | `ECS/ContainerInsights` `RunningTaskCount` | < 1 | 3 × 1분 |

자동 롤백용 ALB 5xx·latency 알람은 `ecs-service` 모듈이 자체적으로 만든다 — 본 모듈과 분리.

## 대시보드

`overview` 대시보드: 서비스별 CPU 라인 그래프 그리드. 추후 P95 latency, 5xx, 데이터 계층(Aurora ACU, ElastiCache Hit, MSK lag) 위젯 추가 예정 (세션 4).
