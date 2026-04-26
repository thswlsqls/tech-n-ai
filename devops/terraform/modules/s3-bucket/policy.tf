# 버킷 정책 — HTTPS 강제 + SSE 강제 (모든 버킷 공통)
# 사용자 추가 정책(var.bucket_policy_json)이 있으면 합친다.

data "aws_iam_policy_document" "default" {
  statement {
    sid    = "DenyInsecureTransport"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions = ["s3:*"]
    resources = [
      aws_s3_bucket.this.arn,
      "${aws_s3_bucket.this.arn}/*",
    ]
    condition {
      test     = "Bool"
      variable = "aws:SecureTransport"
      values   = ["false"]
    }
  }

  statement {
    sid    = "DenyUnencryptedPut"
    effect = "Deny"
    principals {
      type        = "*"
      identifiers = ["*"]
    }
    actions   = ["s3:PutObject"]
    resources = ["${aws_s3_bucket.this.arn}/*"]
    condition {
      test     = "StringNotEquals"
      variable = "s3:x-amz-server-side-encryption"
      values   = ["aws:kms"]
    }
  }
}

# 사용자 추가 정책이 있으면 source_policy_documents 로 머지
data "aws_iam_policy_document" "merged" {
  source_policy_documents = compact([
    data.aws_iam_policy_document.default.json,
    var.bucket_policy_json,
  ])
}

resource "aws_s3_bucket_policy" "this" {
  bucket = aws_s3_bucket.this.id
  policy = data.aws_iam_policy_document.merged.json

  depends_on = [aws_s3_bucket_public_access_block.this]
}
