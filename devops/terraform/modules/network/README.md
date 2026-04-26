# `modules/network` — VPC + 4티어 서브넷 + VPC Endpoints

> 02-network-vpc.md 의 설계를 그대로 모듈로 구현. 환경(dev/beta/prod) 모두 동일 구조이며 CIDR 만 다르다.

## 무엇을 만드는가

| 자원 | 개수 | 비고 |
|---|---|---|
| `aws_vpc` | 1 | DNS hostnames+support 활성 |
| `aws_internet_gateway` | 1 | |
| Public 서브넷 (`/24`) | 3 (AZ당 1) | ALB·NAT GW 위치 |
| Private-App 서브넷 (`/20`) | 3 | ECS Fargate Task ENI |
| Private-Data 서브넷 (`/24`) | 3 | Aurora·ElastiCache·MongoDB PrivateLink |
| Private-TGW 서브넷 (`/26`) | 3 | 향후 TGW |
| NAT Gateway | 0~3 | `enable_nat_gateway` × `single_nat_gateway` 조합 |
| 라우팅 테이블 | 6 | public 1 + private-app 3 + private-data 1 + private-tgw 1 |
| VPCE Gateway | 2 | S3, DynamoDB |
| VPCE Interface | 9 | ECR.api/ECR.dkr/Logs/KMS/STS/SecretsManager/SSM/SSMMessages/EC2Messages |
| VPCE 전용 SG | 1 | Private-App CIDR 에서 443 인바운드 |
| Flow Logs | 1 | CloudWatch Logs (90일 기본) |

## CIDR 분할 규칙

`/16` → 결정적으로 분할 (`cidrsubnet()` 함수):

| 티어 | 비트 | netnum | 결과 (10.30.0.0/16 예시) |
|---|---|---|---|
| Public | /24 | 0, 1, 2 | 10.30.0.0/24, 10.30.1.0/24, 10.30.2.0/24 |
| Private-App | /20 | 1, 2, 3 | 10.30.16.0/20, 10.30.32.0/20, 10.30.48.0/20 |
| Private-Data | /24 | 64, 65, 66 | 10.30.64.0/24, 10.30.65.0/24, 10.30.66.0/24 |
| Private-TGW | /26 | 280, 281, 282 | 10.30.70.0/26, 10.30.70.64/26, 10.30.70.128/26 |

## 사용 예

```hcl
module "network" {
  source = "../../modules/network"

  project     = "techai"
  environment = "dev"
  cidr_block  = "10.10.0.0/16"
  azs         = ["ap-northeast-2a", "ap-northeast-2b", "ap-northeast-2c"]

  # 비용 절감: dev 는 단일 NAT 로 충분
  enable_nat_gateway = true
  single_nat_gateway = true

  enable_vpc_endpoints = true
  enable_flow_logs     = true

  tags = {
    CostCenter = "tech-n-ai-platform"
  }
}
```

## 출력 사용법

| 출력 | 사용처 |
|---|---|
| `vpc_id` | 모든 모듈 |
| `private_subnet_ids` | `ecs-service`, `msk-*` (Private-App) |
| `data_subnet_ids` | `aurora-mysql`, `elasticache-valkey` |
| `vpce_security_group_id` | `ecs-service` 의 outbound 443 |

## 비용 가이드

- NAT Gateway 가 가장 비싼 항목 (서울 리전 약 $0.062/시 + DPGB).
  - dev/beta: `single_nat_gateway = true` 권장 (1개로 통합 → 월 약 $45 절감)
  - prod: AZ별 1개 (`single_nat_gateway = false`) — AZ 장애 시 격리 유지
- VPC Endpoint Interface 는 9개 × $0.01/시 ≈ 월 $65. Gateway(S3/DynamoDB)는 무료.
  - dev 에서 비용 민감 시 `enable_vpc_endpoints = false` 가능 (NAT 경유로 폴백)

## 주의

- **워크로드 SG 는 본 모듈이 만들지 않는다.** sg-api-* 등 14개는 `ecs-service` 모듈이 자기 SG 를 만들고 인바운드 SG ID 만 외부에서 받는다 (00-매트릭스 §3 정합).
- `prevent_destroy` 미적용 — 환경 재구성 시 destroy 가능. state 버킷과 다름.
