variable "project" {
  description = "프로젝트 식별자."
  type        = string
}

variable "environment" {
  description = "주로 prod. (dev/beta 는 msk-serverless)"
  type        = string
  default     = "prod"
}

variable "cluster_name" {
  description = "클러스터 이름. 미지정 시 `{project}-{env}-msk`."
  type        = string
  default     = null
}

variable "kafka_version" {
  description = "Kafka 버전. KRaft 안정 — 3.9.x (D-8 결정)."
  type        = string
  default     = "3.9.x.kraft"
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private-Data 또는 Private-App 서브넷 ID 목록 (3 AZ)."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) == 3
    error_message = "MSK Provisioned 는 3 AZ 권장."
  }
}

variable "broker_count" {
  description = "브로커 수. RF=3 + min.insync.replicas=2 충족 위해 3 권장."
  type        = number
  default     = 3
}

variable "broker_instance_type" {
  description = "브로커 인스턴스 타입 (ARM Graviton 권장)."
  type        = string
  default     = "kafka.m7g.large"
}

variable "ebs_volume_size" {
  description = "브로커당 EBS GB. Retention × throughput × RF 기반 산정 (05 §1.2)."
  type        = number
  default     = 500
}

variable "enable_storage_autoscaling" {
  description = "EBS Storage Auto Scaling. true 면 사용률 기반 자동 확장."
  type        = bool
  default     = true
}

variable "storage_autoscaling_max_size" {
  description = "Storage Auto Scaling 최대 GB."
  type        = number
  default     = 1000
}

variable "storage_autoscaling_target_utilization" {
  description = "Storage Auto Scaling 목표 사용률 %."
  type        = number
  default     = 70
}

variable "kms_key_arn" {
  description = "저장 암호화 KMS CMK ARN. 매트릭스 §1 의 `{env}-data` 키."
  type        = string
}

variable "enhanced_monitoring" {
  description = "메트릭 수집 레벨."
  type        = string
  default     = "PER_TOPIC_PER_PARTITION"

  validation {
    condition = contains(
      ["DEFAULT", "PER_BROKER", "PER_TOPIC_PER_BROKER", "PER_TOPIC_PER_PARTITION"],
      var.enhanced_monitoring,
    )
    error_message = "유효하지 않은 enhanced_monitoring."
  }
}

variable "enable_open_monitoring" {
  description = "Prometheus JMX/Node Exporter 활성. Provisioned 만 지원."
  type        = bool
  default     = true
}

variable "log_group_name" {
  description = "CloudWatch Log Group 이름. 미지정 시 `/aws/msk/{cluster_name}`."
  type        = string
  default     = null
}

variable "log_retention_days" {
  description = "Log Group 보관 일수."
  type        = number
  default     = 90
}

variable "allowed_security_group_ids" {
  description = "9098 (IAM)·9094 (TLS) 인바운드 허용 SG."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
