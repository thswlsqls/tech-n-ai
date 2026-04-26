output "role_arn" {
  description = "생성된 IAM Role ARN."
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "생성된 IAM Role 이름."
  value       = aws_iam_role.this.name
}

output "role_id" {
  description = "Role 의 IAM ID (Role ID, AROA...)."
  value       = aws_iam_role.this.unique_id
}
