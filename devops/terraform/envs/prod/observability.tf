# Observability — 표준 알람 + 대시보드
# Log Group 은 ecs-service 모듈이 자체 생성하므로 본 모듈은 추가 LogGroup 0 + 알람만

module "observability" {
  source = "../../modules/observability"

  project     = var.project
  environment = var.environment

  log_kms_key_arn     = aws_kms_key.logs.arn
  alarm_sns_topic_arn = aws_sns_topic.alerts.arn

  service_alarms = [
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_gateway.service_name, min_running_count = 1 },
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_auth.service_name, min_running_count = 1 },
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_emerging_tech.service_name, min_running_count = 1 },
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_chatbot.service_name, min_running_count = 1 },
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_bookmark.service_name, min_running_count = 1 },
    { cluster_name = aws_ecs_cluster.main.name, service_name = module.api_agent.service_name, min_running_count = 1 },
  ]

  dashboard_enabled = true
}
