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

variable "cluster_identifier" {
  description = "Aurora 클러스터 식별자. 미지정 시 `{project}-{env}-aurora-core`."
  type        = string
  default     = null
}

variable "engine_mode" {
  description = "Aurora 모드 — `serverlessv2` (dev/beta 권장) 또는 `provisioned` (prod 권장)."
  type        = string
  default     = "serverlessv2"

  validation {
    condition     = contains(["serverlessv2", "provisioned"], var.engine_mode)
    error_message = "engine_mode 는 serverlessv2 또는 provisioned."
  }
}

variable "engine_version" {
  description = "Aurora MySQL 엔진 버전. 기본 8.0 호환 최신 안정."
  type        = string
  default     = "8.0.mysql_aurora.3.07.1"
}

variable "vpc_id" {
  description = "VPC ID. network 모듈 출력."
  type        = string
}

variable "data_subnet_ids" {
  description = "Private-Data 서브넷 ID 목록. 최소 2개(Multi-AZ)."
  type        = list(string)

  validation {
    condition     = length(var.data_subnet_ids) >= 2
    error_message = "Multi-AZ 충족 위해 최소 2개 서브넷 필요."
  }
}

variable "kms_key_arn" {
  description = "저장 암호화용 KMS CMK ARN. 매트릭스 §1 의 `{env}-data` 키."
  type        = string
}

variable "db_name" {
  description = "초기 데이터베이스 이름."
  type        = string
}

variable "master_username" {
  description = "마스터 사용자 이름. 비밀번호는 Managed Master User Password 로 자동 생성."
  type        = string
  default     = "techai_admin"
}

# Provisioned 전용
variable "instance_count" {
  description = "Provisioned 모드 인스턴스 수 (Writer 1 + Reader N). serverlessv2 모드에선 무시."
  type        = number
  default     = 2
}

variable "instance_class" {
  description = "Provisioned 모드 인스턴스 클래스 (예: `db.r7g.large`). serverlessv2 모드에선 무시."
  type        = string
  default     = "db.r7g.large"
}

# Serverless v2 전용
variable "serverlessv2_min_capacity" {
  description = "Serverless v2 최소 ACU."
  type        = number
  default     = 0.5
}

variable "serverlessv2_max_capacity" {
  description = "Serverless v2 최대 ACU."
  type        = number
  default     = 2.0
}

variable "allowed_security_group_ids" {
  description = "Aurora 3306 인바운드를 허용할 SG ID 목록 (워크로드 SG)."
  type        = list(string)
  default     = []
}

variable "backup_retention_period" {
  description = "백업 보관 일수. dev=1, beta=7, prod=30 권장."
  type        = number
  default     = 7
}

variable "preferred_backup_window" {
  description = "자동 백업 시간 (UTC). 한국 새벽 3시 = 18:00 UTC."
  type        = string
  default     = "18:00-19:00"
}

variable "preferred_maintenance_window" {
  description = "유지보수 시간 (UTC)."
  type        = string
  default     = "sun:19:00-sun:20:00"
}

variable "deletion_protection" {
  description = "삭제 보호. prod=true 강제 권장."
  type        = bool
  default     = true
}

variable "skip_final_snapshot" {
  description = "destroy 시 최종 스냅샷 생성 여부. dev=true 가능."
  type        = bool
  default     = false
}

variable "performance_insights_enabled" {
  description = "Performance Insights 활성화. prod=true."
  type        = bool
  default     = false
}

variable "performance_insights_retention_period" {
  description = "PI 보관 일수 (7 또는 731). 7=무료."
  type        = number
  default     = 7
}

variable "iam_database_authentication_enabled" {
  description = "IAM DB 인증 활성화. api-auth Task Role 이 RDS 인증 토큰 사용."
  type        = bool
  default     = true
}

variable "storage_type" {
  description = "Aurora 스토리지 타입 — `aurora-iopt1` (I/O-Optimized, prod 권장) 또는 `aurora` (표준)."
  type        = string
  default     = "aurora"

  validation {
    condition     = contains(["aurora", "aurora-iopt1"], var.storage_type)
    error_message = "storage_type 은 aurora 또는 aurora-iopt1."
  }
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
