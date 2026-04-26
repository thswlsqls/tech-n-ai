# `modules/s3-bucket` — 표준 S3 버킷

> 09 §5.7 spec 구현. SSE-KMS · Versioning · BPA · Lifecycle · Object Lock · HTTPS 강제 정책을 일관되게 적용.

## 사용 예 — 어플리케이션 업로드 버킷

```hcl
module "uploads_bucket" {
  source = "../../modules/s3-bucket"

  bucket_name = "techai-${var.environment}-app-uploads"
  kms_key_arn = aws_kms_key.s3_app.arn

  versioning_enabled  = true
  object_lock_enabled = false
  block_public_access = true

  lifecycle_rules = [
    {
      id                                         = "expire-old-versions"
      enabled                                    = true
      noncurrent_version_expiration_days         = 30
      noncurrent_version_transition_glacier_days = 7
    }
  ]

  tags = {
    Project     = "techai"
    Environment = var.environment
    Purpose     = "app-uploads"
  }
}
```

## 사용 예 — Athena query result (수명 짧은 버킷)

```hcl
module "athena_results" {
  source = "../../modules/s3-bucket"

  bucket_name = "techai-${var.environment}-athena-results"
  kms_key_arn = aws_kms_key.logs.arn

  lifecycle_rules = [
    {
      id              = "expire-30d"
      enabled         = true
      expiration_days = 30
    }
  ]
}
```

## 강제 정책

본 모듈이 자동 부착하는 정책:

1. **DenyInsecureTransport** — `aws:SecureTransport=false` 차단 (HTTPS 강제)
2. **DenyUnencryptedPut** — `s3:x-amz-server-side-encryption ≠ aws:kms` 차단 (SSE 우회 차단)

추가 정책이 필요하면 `bucket_policy_json` 변수에 JSON 으로 전달 (병합됨).

## Object Lock 주의

- `object_lock_enabled = true` 는 **버킷 생성 시점에만** 설정 가능. 한 번 켜면 끌 수 없음.
- 일반 어플리케이션 데이터는 false. 감사 로그·raw 이벤트에만 true.
