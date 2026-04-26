output "distribution_id" {
  description = "CloudFront Distribution ID."
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_arn" {
  description = "CloudFront Distribution ARN."
  value       = aws_cloudfront_distribution.this.arn
}

output "distribution_domain_name" {
  description = "CloudFront 도메인 (예: dxyz.cloudfront.net). Route53 alias 레코드 대상."
  value       = aws_cloudfront_distribution.this.domain_name
}

output "distribution_hosted_zone_id" {
  description = "CloudFront 호스팅 영역 ID. Route53 alias 에 사용."
  value       = aws_cloudfront_distribution.this.hosted_zone_id
}

output "origin_access_control_id" {
  description = "OAC ID (S3 origin 시)."
  value       = try(aws_cloudfront_origin_access_control.this[0].id, null)
}
