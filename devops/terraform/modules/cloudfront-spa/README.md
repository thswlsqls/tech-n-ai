# `modules/cloudfront-spa` — CloudFront SPA 배포

> 09 §5.9 spec 구현. Amplify Hosting 또는 S3 + OAC origin 분기.

## 사용 예 — Amplify origin (현재 구성)

```hcl
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

module "cf_app" {
  source = "../../modules/cloudfront-spa"
  providers = {
    aws.us_east_1 = aws.us_east_1
  }

  project           = "techai"
  environment       = "dev"
  distribution_name = "app"

  origin_type           = "amplify"
  amplify_origin_domain = "${aws_amplify_branch.app_dev.branch_name}.${aws_amplify_app.app.default_domain}"

  domain_aliases       = []   # 도메인 미연결 단계
  acm_certificate_arn  = null
  waf_web_acl_arn      = null
}
```

## 사용 예 — S3 origin (정적 호스팅)

```hcl
module "cf_static" {
  source = "../../modules/cloudfront-spa"
  providers = { aws.us_east_1 = aws.us_east_1 }

  project           = "techai"
  environment       = "prod"
  distribution_name = "marketing"

  origin_type               = "s3"
  s3_bucket_regional_domain = module.marketing_bucket.bucket_regional_domain_name
  s3_bucket_arn             = module.marketing_bucket.bucket_arn

  domain_aliases      = ["www.tech-n-ai.example.com"]
  acm_certificate_arn = aws_acm_certificate.cf_us_east_1.arn
  waf_web_acl_arn     = aws_wafv2_web_acl.cf_global.arn

  spa_404_to_index = true
}
```

## us-east-1 의존성

CloudFront 의 ACM 인증서·WAF Web ACL 은 모두 **us-east-1** 발급이어야 한다. envs 의 providers.tf 에 `aws.us_east_1` alias 정의 후 본 모듈에 전달.

## 보안 헤더

본 모듈이 자동 적용:
- CSP: `default-src 'self'; ...`
- HSTS: 1년 + includeSubDomains + preload
- X-Frame-Options: DENY
- Referrer-Policy: strict-origin-when-cross-origin
- X-XSS-Protection: 1; mode=block

## 비용

CloudFront PriceClass_200(북미·유럽·아시아·중동) 기본. dev 트래픽 0 단계에서는 거의 무료(요청·전송량 비례).
