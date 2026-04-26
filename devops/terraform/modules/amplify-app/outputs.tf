output "app_id" {
  description = "Amplify App ID. GitHub Actions 의 `aws amplify start-job --app-id` 에 사용."
  value       = aws_amplify_app.this.id
}

output "app_arn" {
  description = "Amplify App ARN."
  value       = aws_amplify_app.this.arn
}

output "default_domain" {
  description = "Amplify 기본 도메인 (예: `dxxxxxx.amplifyapp.com`)."
  value       = aws_amplify_app.this.default_domain
}

output "branch_name" {
  description = "활성 브랜치 이름."
  value       = aws_amplify_branch.this.branch_name
}

output "branch_url" {
  description = "브랜치 URL (`https://{branch}.{default_domain}`)."
  value       = "https://${aws_amplify_branch.this.branch_name}.${aws_amplify_app.this.default_domain}"
}

output "service_role_arn" {
  description = "Amplify 서비스 Role ARN."
  value       = local.service_role_arn
}
