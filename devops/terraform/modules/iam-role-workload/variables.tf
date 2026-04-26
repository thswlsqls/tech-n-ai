variable "workload_name" {
  description = "워크로드 식별자 (예: api-auth, batch-source). Role 이름의 일부."
  type        = string
}

variable "role_name_override" {
  description = "Role 이름 강제 지정. 미지정 시 `{project}-{environment}-task-{workload_name}`."
  type        = string
  default     = null
}

variable "project" {
  description = "프로젝트 식별자."
  type        = string
}

variable "environment" {
  description = "dev / beta / prod"
  type        = string

  validation {
    condition     = contains(["dev", "beta", "prod"], var.environment)
    error_message = "environment 는 dev / beta / prod 중 하나."
  }
}

variable "trust_service" {
  description = "AssumeRole 을 허용할 AWS 서비스 (예: ecs-tasks.amazonaws.com)."
  type        = string

  validation {
    condition     = can(regex("\\.amazonaws\\.com$", var.trust_service))
    error_message = "trust_service 는 *.amazonaws.com 형식이어야 함."
  }
}

variable "trust_conditions" {
  description = "신뢰 정책에 추가할 조건. test/variable/values 객체 리스트. 예) aws:SourceAccount."
  type = list(object({
    test     = string
    variable = string
    values   = list(string)
  }))
  default = []
}

variable "managed_policy_arns" {
  description = "부착할 AWS Managed 또는 Customer Managed Policy ARN 목록."
  type        = list(string)
  default     = []
}

variable "inline_policies" {
  description = "인라인 정책 맵. 키=정책명, 값=JSON 문자열."
  type        = map(string)
  default     = {}
}

variable "permissions_boundary_arn" {
  description = "Permission Boundary 정책 ARN (선택)."
  type        = string
  default     = null
}

variable "max_session_duration" {
  description = "최대 세션 시간(초). 워크로드는 기본값(3600). 관리자는 더 길게."
  type        = number
  default     = 3600
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
