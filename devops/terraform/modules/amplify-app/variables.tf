variable "project" {
  description = "프로젝트 식별자."
  type        = string
}

variable "environment" {
  description = "dev / beta / prod"
  type        = string
}

variable "app_name" {
  description = "Amplify 앱 식별자 (`app` 또는 `admin`)."
  type        = string

  validation {
    condition     = contains(["app", "admin"], var.app_name)
    error_message = "app_name 은 `app` 또는 `admin`."
  }
}

variable "repository_url" {
  description = "GitHub 리포지토리 URL (예: `https://github.com/org/tech-n-ai-frontend`)."
  type        = string
}

variable "github_access_token_secret_arn" {
  description = "GitHub Personal Access Token 이 저장된 Secrets Manager ARN. Amplify 가 리포 클론·웹훅 등록에 사용."
  type        = string
  default     = null
}

variable "branch_name" {
  description = "추적할 git 브랜치 (`main` for prod, `develop` for beta/dev)."
  type        = string
}

variable "stage" {
  description = "Amplify 배포 stage (`PRODUCTION` / `BETA` / `DEVELOPMENT`)."
  type        = string
  default     = "PRODUCTION"

  validation {
    condition     = contains(["PRODUCTION", "BETA", "DEVELOPMENT", "EXPERIMENTAL", "PULL_REQUEST"], var.stage)
    error_message = "유효한 Amplify stage."
  }
}

variable "platform" {
  description = "Amplify 플랫폼 — Next.js 16 SSR 은 `WEB_COMPUTE` 필수."
  type        = string
  default     = "WEB_COMPUTE"

  validation {
    condition     = contains(["WEB", "WEB_COMPUTE", "WEB_DYNAMIC"], var.platform)
    error_message = "platform 은 WEB, WEB_COMPUTE, WEB_DYNAMIC."
  }
}

variable "framework" {
  description = "프레임워크 식별자."
  type        = string
  default     = "Next.js - SSR"
}

variable "environment_variables" {
  description = "빌드타임 환경변수 (NEXT_PUBLIC_* 등). 런타임 변수는 Branch 레벨에 별도 정의."
  type        = map(string)
  default     = {}
}

variable "branch_environment_variables" {
  description = "브랜치 전용 환경변수. App 레벨 변수보다 우선."
  type        = map(string)
  default     = {}
}

variable "build_spec" {
  description = "Amplify build spec (amplify.yml). null 이면 기본값(Next.js 표준)."
  type        = string
  default     = null
}

variable "iam_service_role_arn" {
  description = "Amplify 서비스 Role ARN. SSM/Secrets/CloudWatch Logs 권한 필요. null 이면 모듈이 자동 생성."
  type        = string
  default     = null
}

variable "enable_basic_auth" {
  description = "Amplify Basic Auth (admin 앱 등 내부 전용)."
  type        = bool
  default     = false
}

variable "basic_auth_credentials" {
  description = "Basic Auth credentials (base64 인코딩 `user:password`). enable_basic_auth=true 시 필수."
  type        = string
  default     = null
  sensitive   = true
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
