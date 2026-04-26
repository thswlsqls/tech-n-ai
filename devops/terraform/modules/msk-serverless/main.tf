# MSK Serverless (dev/beta 용)
# - IAM Access Control 전용 (Serverless 의 유일한 인증 방식)
# - 클러스터-시간 + 파티션-시간 + 데이터 I/O 비용. 사용 전 비활성 권장.
# - 05 §1.1 spec 구현

locals {
  name = coalesce(
    var.cluster_name,
    "${var.project}-${var.environment}-msk-sl",
  )

  common_tags = merge(
    {
      Project     = var.project
      Environment = var.environment
      ManagedBy   = "Terraform"
      Module      = "msk-serverless"
    },
    var.tags,
  )
}

# ----------------------------------------------------------------------------
# Security Group
# ----------------------------------------------------------------------------

resource "aws_security_group" "this" {
  name        = "${local.name}-sg"
  description = "MSK Serverless 9098 IAM SASL inbound from workload SGs"
  vpc_id      = var.vpc_id

  tags = merge(local.common_tags, {
    Name = "${local.name}-sg"
  })
}

resource "aws_security_group_rule" "ingress_iam_sasl" {
  for_each = toset(var.allowed_security_group_ids)

  type                     = "ingress"
  description              = "Kafka IAM SASL 9098 from ${each.value}"
  from_port                = 9098
  to_port                  = 9098
  protocol                 = "tcp"
  source_security_group_id = each.value
  security_group_id        = aws_security_group.this.id
}

# ----------------------------------------------------------------------------
# Cluster
# ----------------------------------------------------------------------------

resource "aws_msk_serverless_cluster" "this" {
  cluster_name = local.name

  vpc_config {
    subnet_ids         = var.private_subnet_ids
    security_group_ids = [aws_security_group.this.id]
  }

  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

  tags = local.common_tags
}
