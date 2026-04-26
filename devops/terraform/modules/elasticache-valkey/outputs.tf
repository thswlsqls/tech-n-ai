output "replication_group_id" {
  description = "Valkey Replication Group ID."
  value       = aws_elasticache_replication_group.this.id
}

output "primary_endpoint" {
  description = "Primary 엔드포인트 (쓰기). num_node_groups=1 일 때 유효."
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "reader_endpoint" {
  description = "Reader 엔드포인트 (읽기 분산). 복제본이 있을 때 유효."
  value       = aws_elasticache_replication_group.this.reader_endpoint_address
}

output "configuration_endpoint" {
  description = "Configuration 엔드포인트 (cluster mode enabled 시)."
  value       = aws_elasticache_replication_group.this.configuration_endpoint_address
}

output "port" {
  description = "Valkey 포트 (6379)."
  value       = aws_elasticache_replication_group.this.port
}

output "security_group_id" {
  description = "Valkey SG ID."
  value       = aws_security_group.this.id
}

output "auth_token" {
  description = "AUTH 토큰. auth_mode=auth_token 시만 의미. Secrets Manager 에 저장 후 application 이 fetch."
  value       = local.use_auth_token ? random_password.auth_token[0].result : null
  sensitive   = true
}

output "auth_mode" {
  description = "현재 인증 모드 (auth_token / rbac)."
  value       = var.auth_mode
}
