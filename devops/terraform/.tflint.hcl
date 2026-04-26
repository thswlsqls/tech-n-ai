# tflint 설정 — AWS 룰셋 활성화
# 공식: https://github.com/terraform-linters/tflint-ruleset-aws

config {
  call_module_type = "all"
  force            = false
}

plugin "terraform" {
  enabled = true
  preset  = "recommended"
}

plugin "aws" {
  enabled = true
  version = "0.32.0"
  source  = "github.com/terraform-linters/tflint-ruleset-aws"
}

# 잘못된 인스턴스 타입 탐지
rule "aws_instance_invalid_type" { enabled = true }
rule "aws_db_instance_invalid_type" { enabled = true }
rule "aws_elasticache_cluster_invalid_type" { enabled = true }

# 빈 리소스 이름·태그 누락 검사
rule "terraform_naming_convention" {
  enabled = true
  format  = "snake_case"
}

rule "terraform_required_version" { enabled = true }
rule "terraform_required_providers" { enabled = true }
rule "terraform_unused_declarations" { enabled = true }
rule "terraform_documented_outputs" { enabled = true }
rule "terraform_documented_variables" { enabled = true }
