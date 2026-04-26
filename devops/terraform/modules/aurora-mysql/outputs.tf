output "cluster_identifier" {
  description = "Aurora 클러스터 식별자."
  value       = aws_rds_cluster.this.cluster_identifier
}

output "cluster_arn" {
  description = "Aurora 클러스터 ARN."
  value       = aws_rds_cluster.this.arn
}

output "cluster_endpoint" {
  description = "Writer 엔드포인트. 쓰기·DDL 트래픽."
  value       = aws_rds_cluster.this.endpoint
}

output "reader_endpoint" {
  description = "Reader 엔드포인트. 읽기 트래픽 분산."
  value       = aws_rds_cluster.this.reader_endpoint
}

output "port" {
  description = "MySQL 포트 (3306)."
  value       = aws_rds_cluster.this.port
}

output "database_name" {
  description = "초기 데이터베이스 이름."
  value       = aws_rds_cluster.this.database_name
}

output "master_username" {
  description = "마스터 사용자명."
  value       = aws_rds_cluster.this.master_username
}

output "master_user_secret_arn" {
  description = "Managed Master User Password Secrets Manager ARN. api-auth Task Role 이 GetSecretValue."
  value       = try(aws_rds_cluster.this.master_user_secret[0].secret_arn, null)
  sensitive   = true
}

output "security_group_id" {
  description = "Aurora SG ID. 워크로드 SG 가 outbound 3306 으로 사용."
  value       = aws_security_group.this.id
}

output "instance_endpoints" {
  description = "각 인스턴스 엔드포인트 (운영 디버그용)."
  value       = { for inst in aws_rds_cluster_instance.this : inst.identifier => inst.endpoint }
}

output "cluster_resource_id" {
  description = "RDS Resource ID. IAM DB 인증 정책의 리소스 ARN 에 사용."
  value       = aws_rds_cluster.this.cluster_resource_id
}
