# envs/dev — 개발 환경

## 적용 절차

### 사전 조건

- `bootstrap/` 적용 완료 (state 인프라 + GitHub OIDC Role)
- AWS 계정 자격 증명: 관리자 또는 `gha-terraform-apply-dev` Role 로 AssumeRole 가능

### 1. backend 초기화

`backend.hcl` 의 `bucket` 값을 부트스트랩 출력(`state_bucket_name`)으로 교체:

```hcl
# backend.hcl
bucket = "techai-tfstate-111122223333-apne2"   # 실제 account id
```

### 2. terraform 적용

```bash
cd devops/terraform/envs/dev

terraform init -backend-config=backend.hcl
terraform plan -var-file=terraform.tfvars -out=tfplan
terraform apply tfplan
```

## 무엇이 만들어지는가 (세션 2a → 2b)

| 자원 | 규모 | 세션 |
|---|---|---|
| VPC `techai-dev-vpc` (`10.10.0.0/16`) | 1 | 2a |
| 서브넷 (Public·Private-App·Private-Data·Private-TGW) | 12 (3 AZ × 4 티어) | 2a |
| NAT Gateway | 1 (single_nat_gateway=true, 비용 절감) | 2a |
| VPC Endpoint (Gateway 2 + Interface 9) | 11 | 2a |
| KMS CMK (`{env}-data`·`{env}-s3-app`·`{env}-auth`·`{env}-ai`·`{env}-logs`) | 5 (매트릭스 §1, `tfstate` 1키는 부트스트랩 단일 공유) | 2a |
| S3 버킷 (`techai-dev-app-uploads`) | 1 | 2a |
| ECS Task Execution Role | 1 | 2a |
| VPC Flow Logs CloudWatch LogGroup | 1 | 2a |
| **Aurora MySQL (Serverless v2, 0.5~2 ACU)** | 1 클러스터 + 1 인스턴스 | **2b** |
| **ElastiCache Valkey (cache.t4g.micro, replicas=0)** | 1 노드 | **2b** |
| **MSK Serverless** | 0 (enable_msk=false 기본) | 2b |

ECS 워크로드(api-* + frontend) + 워크로드 SG + observability 는 **세션 2c**.

## 추정 월 비용 (dev, 24/7, enable_msk=false)

| 항목 | 단가 | 비용 |
|---|---|---|
| NAT Gateway × 1 | $0.062/hr + DPGB | ~$45 |
| VPC Endpoint Interface × 9 | $0.01/hr × 9 | ~$65 |
| Flow Logs CloudWatch | 0.5GB × $0.50/GB | ~$0.25 |
| KMS CMK × 5 | $1/월 × 5 | $5 |
| S3 (저용량) | — | ~$0 |
| **Aurora Serverless v2 (0.5 ACU baseline)** | $0.12/ACU-h × 720 × 0.5 | **~$43** |
| **ElastiCache cache.t4g.micro × 1** | $0.022/h × 720 | **~$16** |
| **MSK Serverless (비활성)** | $0 | $0 |
| **합계** | | **~$175/월** |

> Aurora Serverless v2 자동 일시정지는 `min_capacity = 0` 설정 시에만 활성화된다(공식: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-serverless-v2-auto-pause.html). 본 환경 기본값은 `aurora_min_acu = 0.5` 로 24/7 과금이며, 진짜 사용 안 하는 환경이면 `aurora_min_acu = 0` 으로 변경하거나 destroy 후 필요 시 재생성이 더 저렴.

## 주요 토글

```bash
# 데이터 모듈 끄기 (인프라만 두고 데이터는 나중에)
terraform apply -var="enable_aurora=false" -var="enable_elasticache=false"

# MSK 활성화 시점
terraform apply -var="enable_msk=true"
```

## 관련 문서

- [09 §5 모듈 spec](../../../results/09-iac-terraform.md)
- [00 매트릭스](../../../results/00-cross-cutting-matrix.md)
- [11 비용 최적화](../../../results/11-dr-and-cost-optimization.md)
