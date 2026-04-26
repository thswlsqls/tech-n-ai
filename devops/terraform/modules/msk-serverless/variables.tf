variable "project" {
  description = "프로젝트 식별자."
  type        = string
}

variable "environment" {
  description = "dev / beta. (Serverless 는 prod 미사용 — prod 는 msk-provisioned)"
  type        = string

  validation {
    condition     = contains(["dev", "beta"], var.environment)
    error_message = "msk-serverless 는 dev 또는 beta 만 사용."
  }
}

variable "cluster_name" {
  description = "클러스터 이름. 미지정 시 `{project}-{env}-msk-sl`."
  type        = string
  default     = null
}

variable "vpc_id" {
  description = "VPC ID."
  type        = string
}

variable "private_subnet_ids" {
  description = "Private-App 서브넷 ID 목록 (3 AZ 권장)."
  type        = list(string)

  validation {
    condition     = length(var.private_subnet_ids) >= 2
    error_message = "MSK Serverless 는 최소 2개 AZ 필요."
  }
}

variable "allowed_security_group_ids" {
  description = "9098 (IAM SASL) 인바운드를 허용할 SG."
  type        = list(string)
  default     = []
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
