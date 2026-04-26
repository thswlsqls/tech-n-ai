output "cluster_arn" {
  description = "MSK Serverless 클러스터 ARN."
  value       = aws_msk_serverless_cluster.this.arn
}

output "cluster_name" {
  description = "클러스터 이름."
  value       = aws_msk_serverless_cluster.this.cluster_name
}

output "bootstrap_brokers_sasl_iam" {
  description = "IAM SASL bootstrap brokers. Spring Kafka 의 bootstrap-servers 에 사용."
  value       = aws_msk_serverless_cluster.this.bootstrap_brokers_sasl_iam
}

output "security_group_id" {
  description = "MSK SG ID. 워크로드 SG 가 outbound 9098 으로 사용."
  value       = aws_security_group.this.id
}
