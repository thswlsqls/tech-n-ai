# 일반 워크로드 Role 모듈
# - ECS Task Role, ECS Execution Role, Lambda Execution Role 등 모두 본 모듈 재사용 가능.
# - 신뢰 서비스(trust_service)와 정책(managed/inline)을 입력으로 받아 표준화.

locals {
  default_role_name = "${var.project}-${var.environment}-task-${var.workload_name}"
  role_name         = coalesce(var.role_name_override, local.default_role_name)

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "Terraform"
      Module      = "iam-role-workload"
      Workload    = var.workload_name
    },
    var.tags,
  )
}

# 신뢰 정책 — 단일 서비스 + 선택적 조건
data "aws_iam_policy_document" "assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = [var.trust_service]
    }

    dynamic "condition" {
      for_each = var.trust_conditions
      content {
        test     = condition.value.test
        variable = condition.value.variable
        values   = condition.value.values
      }
    }
  }
}

resource "aws_iam_role" "this" {
  name                 = local.role_name
  assume_role_policy   = data.aws_iam_policy_document.assume_role.json
  permissions_boundary = var.permissions_boundary_arn
  max_session_duration = var.max_session_duration

  tags = local.common_tags
}

# Managed Policy 부착
resource "aws_iam_role_policy_attachment" "managed" {
  for_each = toset(var.managed_policy_arns)

  role       = aws_iam_role.this.name
  policy_arn = each.value
}

# Inline Policy 부착
resource "aws_iam_role_policy" "inline" {
  for_each = var.inline_policies

  name   = each.key
  role   = aws_iam_role.this.id
  policy = each.value
}
