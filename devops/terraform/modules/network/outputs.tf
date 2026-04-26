output "vpc_id" {
  description = "VPC ID."
  value       = aws_vpc.this.id
}

output "vpc_cidr" {
  description = "VPC CIDR 블록."
  value       = aws_vpc.this.cidr_block
}

output "public_subnet_ids" {
  description = "Public 서브넷 ID 목록 (AZ 순)."
  value       = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  description = "Private-App 서브넷 ID 목록. ECS Fargate Task ENI 가 배치된다."
  value       = aws_subnet.private_app[*].id
}

output "data_subnet_ids" {
  description = "Private-Data 서브넷 ID 목록. Aurora·ElastiCache·MongoDB Atlas Endpoint 전용."
  value       = aws_subnet.private_data[*].id
}

output "tgw_subnet_ids" {
  description = "Private-TGW 서브넷 ID 목록 (향후 TGW attach 용)."
  value       = aws_subnet.private_tgw[*].id
}

output "internet_gateway_id" {
  description = "Internet Gateway ID."
  value       = aws_internet_gateway.this.id
}

output "nat_gateway_ids" {
  description = "NAT Gateway ID 목록. enable_nat_gateway=false 면 빈 리스트."
  value       = aws_nat_gateway.this[*].id
}

output "public_route_table_id" {
  description = "Public 서브넷의 라우팅 테이블 ID."
  value       = aws_route_table.public.id
}

output "private_app_route_table_ids" {
  description = "Private-App 서브넷의 라우팅 테이블 ID 목록 (AZ별 분리)."
  value       = aws_route_table.private_app[*].id
}

output "private_data_route_table_id" {
  description = "Private-Data 서브넷의 라우팅 테이블 ID."
  value       = aws_route_table.private_data.id
}

output "vpce_security_group_id" {
  description = "VPCE 전용 SG ID. ecs-service 모듈이 outbound 443 to this 로 사용."
  value       = try(aws_security_group.vpce[0].id, null)
}

output "vpc_endpoint_ids" {
  description = "Interface VPC Endpoint ID 맵 (ecr_api, ecr_dkr, logs, kms, sts, secretsmanager, ssm, ssmmessages, ec2messages)."
  value       = { for k, ep in aws_vpc_endpoint.interface : k => ep.id }
}

output "s3_vpc_endpoint_id" {
  description = "S3 Gateway Endpoint ID."
  value       = try(aws_vpc_endpoint.s3[0].id, null)
}

output "dynamodb_vpc_endpoint_id" {
  description = "DynamoDB Gateway Endpoint ID."
  value       = try(aws_vpc_endpoint.dynamodb[0].id, null)
}

output "flow_log_group_arn" {
  description = "VPC Flow Logs CloudWatch Log Group ARN."
  value       = try(aws_cloudwatch_log_group.flow_logs[0].arn, null)
}
