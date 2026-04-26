# 알람 통지 SNS Topic
# 구독자(이메일/Slack)는 본 IaC 외부에서 추가 (개인 정보 회피)

resource "aws_sns_topic" "alerts" {
  name              = "${var.project}-${var.environment}-alerts"
  kms_master_key_id = aws_kms_key.logs.id

  tags = local.common_tags
}
