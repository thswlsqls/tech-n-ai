output "log_group_arns" {
  description = "추가 Log Group ARN map."
  value       = { for k, lg in aws_cloudwatch_log_group.extra : k => lg.arn }
}

output "cpu_alarm_arns" {
  description = "서비스별 CPU 알람 ARN map."
  value       = { for k, a in aws_cloudwatch_metric_alarm.ecs_cpu : k => a.arn }
}

output "memory_alarm_arns" {
  description = "서비스별 메모리 알람 ARN map."
  value       = { for k, a in aws_cloudwatch_metric_alarm.ecs_memory : k => a.arn }
}

output "running_count_alarm_arns" {
  description = "서비스별 RunningTaskCount 알람 ARN map."
  value       = { for k, a in aws_cloudwatch_metric_alarm.ecs_running_count : k => a.arn }
}

output "dashboard_name" {
  description = "Overview 대시보드 이름."
  value       = try(aws_cloudwatch_dashboard.overview[0].dashboard_name, null)
}
