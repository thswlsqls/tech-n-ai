variable "project" {
  description = "프로젝트 식별자. 모든 자원 이름의 접두어."
  type        = string
  default     = "techai"

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{2,15}$", var.project))
    error_message = "project 는 소문자·숫자·하이픈만 허용. 3~16자."
  }
}

variable "region" {
  description = "AWS 프라이머리 리전."
  type        = string
  default     = "ap-northeast-2"
}

variable "cost_center" {
  description = "비용 추적용 코스트 센터 태그."
  type        = string
  default     = "tech-n-ai-platform"
}

variable "github_org" {
  description = "GitHub 조직(또는 사용자) 이름. OIDC sub 조건 매칭에 사용."
  type        = string
}

variable "github_repo" {
  description = "GitHub 리포지토리 이름 (예: tech-n-ai)."
  type        = string
  default     = "tech-n-ai"
}

variable "environments" {
  description = "환경 목록. 각 환경별로 gha-deploy / gha-terraform-apply Role 생성."
  type        = list(string)
  default     = ["dev", "beta", "prod"]
}

variable "state_bucket_name_override" {
  description = "state S3 버킷 이름 강제. 미지정 시 `{project}-tfstate-{account}-{region-short}`."
  type        = string
  default     = null
}

variable "state_bucket_object_lock_days" {
  description = "state 버킷 Object Lock(Governance) 보관 일수. 사고성 삭제 보호."
  type        = number
  default     = 30
}

variable "state_bucket_noncurrent_version_expiration_days" {
  description = "state 버킷 이전 버전 만료 일수."
  type        = number
  default     = 90
}

variable "lock_table_name" {
  description = "DynamoDB Lock 테이블 이름."
  type        = string
  default     = "techai-tflock"
}

variable "permissions_boundary_managed" {
  description = "Permission Boundary로 사용할 Customer Managed Policy 이름."
  type        = string
  default     = "techai-permissions-boundary"
}
