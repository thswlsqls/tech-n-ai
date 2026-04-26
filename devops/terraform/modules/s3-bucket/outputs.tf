output "bucket_id" {
  description = "S3 버킷 ID (= 이름)."
  value       = aws_s3_bucket.this.id
}

output "bucket_arn" {
  description = "S3 버킷 ARN."
  value       = aws_s3_bucket.this.arn
}

output "bucket_domain_name" {
  description = "S3 버킷 도메인 이름 (글로벌)."
  value       = aws_s3_bucket.this.bucket_domain_name
}

output "bucket_regional_domain_name" {
  description = "S3 버킷 리전 도메인 이름. CloudFront Origin 에 사용."
  value       = aws_s3_bucket.this.bucket_regional_domain_name
}

output "bucket_hosted_zone_id" {
  description = "S3 버킷 호스팅 영역 ID. Route 53 alias 레코드에 사용."
  value       = aws_s3_bucket.this.hosted_zone_id
}
