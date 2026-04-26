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

variable "replication_group_id" {
  description = "Replication Group ID. 미지정 시 `{project}-{env}-cache`."
  type        = string
  default     = null
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "data_subnet_ids" {
  description = "Private-Data 서브넷 ID 목록 (Multi-AZ 위해 2개 이상)."
  type        = list(string)
}

variable "kms_key_arn" {
  description = "저장 암호화 KMS CMK ARN. 매트릭스 §1 의 `{env}-data` 키."
  type        = string
}

variable "engine_version" {
  description = "Valkey 엔진 버전."
  type        = string
  default     = "8.0"
}

variable "node_type" {
  description = "노드 타입. dev=`cache.t4g.micro`, beta/prod=`cache.t4g.small` 권장."
  type        = string
  default     = "cache.t4g.small"
}

variable "num_node_groups" {
  description = "샤드 수. 1=클러스터 모드 비활성과 동등."
  type        = number
  default     = 1
}

variable "replicas_per_node_group" {
  description = "샤드당 복제본 수. dev=0, beta/prod=1 권장 (Multi-AZ 위해 1 이상)."
  type        = number
  default     = 1
}

variable "automatic_failover_enabled" {
  description = "자동 페일오버. replicas_per_node_group >= 1 일 때만 의미 있음."
  type        = bool
  default     = true
}

variable "multi_az_enabled" {
  description = "Multi-AZ 배치. automatic_failover 와 함께 활성."
  type        = bool
  default     = true
}

variable "transit_encryption_enabled" {
  description = "TLS in-transit. 항상 true."
  type        = bool
  default     = true
}

variable "at_rest_encryption_enabled" {
  description = "저장 암호화. 항상 true."
  type        = bool
  default     = true
}

variable "auth_mode" {
  description = "인증 — `auth_token` (TOKEN, dev/beta) 또는 `rbac` (User Group, prod 권장)."
  type        = string
  default     = "auth_token"

  validation {
    condition     = contains(["auth_token", "rbac"], var.auth_mode)
    error_message = "auth_mode 는 auth_token 또는 rbac."
  }
}

variable "rbac_user_group_ids" {
  description = "RBAC 모드 시 부착할 User Group ID 목록."
  type        = list(string)
  default     = []
}

variable "snapshot_retention_limit" {
  description = "백업 보관 일수. dev=0, beta=3, prod=7 권장."
  type        = number
  default     = 1
}

variable "snapshot_window" {
  description = "스냅샷 생성 시간 (UTC)."
  type        = string
  default     = "17:00-19:00"
}

variable "maintenance_window" {
  description = "유지보수 시간 (UTC)."
  type        = string
  default     = "sun:19:00-sun:20:00"
}

variable "allowed_security_group_ids" {
  description = "6379 인바운드 허용 SG."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
