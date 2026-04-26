# GitHub Actions OIDC Provider
# 모든 GHA 워크플로 신뢰 Role 의 Federated Principal 로 사용된다.
# 공식: https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services

resource "aws_iam_openid_connect_provider" "github" {
  url = "https://token.actions.githubusercontent.com"

  client_id_list = ["sts.amazonaws.com"]

  # GitHub OIDC 는 IAM 의 인증서 검증을 사용하지 않으므로 dummy thumbprint 가능 (AWS 공식 안내).
  # 다만 보수적으로 GitHub 의 실제 인증서 SHA-1 을 박는다 — 변경 시 갱신 필요.
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd",
  ]
}
