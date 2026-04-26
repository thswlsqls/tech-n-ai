variable "project" {
  description = "프로젝트 식별자(`techai`)."
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

variable "cidr_block" {
  description = "VPC CIDR (/16). dev=10.10.0.0/16, beta=10.20.0.0/16, prod=10.30.0.0/16."
  type        = string

  validation {
    condition     = can(cidrnetmask(var.cidr_block))
    error_message = "유효한 CIDR 블록이어야 함."
  }
}

variable "azs" {
  description = "AZ 목록. 3개 권장 (서울 리전 a/b/c)."
  type        = list(string)

  validation {
    condition     = length(var.azs) == 3
    error_message = "정확히 3개 AZ 필요."
  }
}

variable "enable_nat_gateway" {
  description = "NAT Gateway 생성 여부. 비용 절감 시 dev=false 가능."
  type        = bool
  default     = true
}

variable "single_nat_gateway" {
  description = "true 면 모든 AZ 가 단일 NAT 공유. dev/beta 비용 절감용. prod=false 권장."
  type        = bool
  default     = false
}

variable "enable_vpc_endpoints" {
  description = "Interface VPC Endpoint 풀세트 생성 여부 (S3/DynamoDB Gateway + ECR/Logs/KMS/STS/SecretsManager/SSM Interface)."
  type        = bool
  default     = true
}

variable "enable_flow_logs" {
  description = "VPC Flow Logs 활성화 여부 (CloudWatch Logs 로 송신)."
  type        = bool
  default     = true
}

variable "flow_log_retention_days" {
  description = "Flow Logs CloudWatch Log Group 보관 일수."
  type        = number
  default     = 90
}

variable "tags" {
  description = "추가 태그."
  type        = map(string)
  default     = {}
}
