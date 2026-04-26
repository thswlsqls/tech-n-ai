# envs/beta — 통합 테스트 환경

## dev 와의 차이

| 항목 | dev | **beta** |
|---|---|---|
| VPC CIDR | 10.10.0.0/16 | 10.20.0.0/16 |
| NAT Gateway | 1 (single) | 1 (single) — 비용 절감 유지 |
| Aurora ACU | 0.5~2 | **0.5~4** |
| Aurora 백업 | 1일 | **7일** |
| ElastiCache | micro × 1 (replicas=0) | **small × 2 Multi-AZ** (replicas=1) |
| ElastiCache 백업 | 0 | **3일** |
| MSK | 비활성 | **Serverless 활성** |
| ECS Auto Scaling | 1~3 | 1~4 |
| ALB | HTTP only | HTTP only |

본 디렉토리는 dev 와 **동일한 .tf 파일**을 사용한다. 환경 차이는 `terraform.tfvars` 의 변수값으로만 표현된다.

## 적용

```bash
cd devops/terraform/envs/beta
# backend.hcl 의 bucket 을 부트스트랩 출력으로 갱신 후
terraform init -backend-config=backend.hcl
terraform plan -var-file=terraform.tfvars
terraform apply
```

## 추정 월 비용 (시드, 현 tfvars)

[11번 §4.0 환경별 월간 추정](../../../results/11-dr-and-cost-optimization.md#40-환경별-월간-추정-비용-시드-가정) 참조 — beta 풀 활성 ~$1,111/월. MSK 활성·비활성 토글이 가장 큰 변수($540).

## 시드 단계 권장

- 통합 테스트가 없는 기간: `enable_msk = false` 임시 설정 (월 $540 절감)
- `enable_aurora = false`, `enable_elasticache = false` 도 가능 (월 ~$108 추가 절감)

## 관련

- [envs/dev/README.md](../dev/README.md)
- [00 매트릭스](../../../results/00-cross-cutting-matrix.md)
- [11 비용](../../../results/11-dr-and-cost-optimization.md)
