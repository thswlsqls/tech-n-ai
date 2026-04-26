# envs/prod — 프로덕션 환경

## dev/beta 와의 차이

| 항목 | dev | beta | **prod** |
|---|---|---|---|
| VPC CIDR | 10.10.0.0/16 | 10.20.0.0/16 | **10.30.0.0/16** |
| NAT Gateway | 1 single | 1 single | **3 (AZ별)** — AZ 격리 |
| Aurora 모드 | Serverless v2 | Serverless v2 | **Provisioned `db.r7g.large` × 3** Multi-AZ (Writer 1 + Reader 2, `aurora_instance_count = 3`) |
| Aurora 백업 | 1일 | 7일 | **30일** |
| Aurora I/O-Optimized | aurora | aurora | **aurora-iopt1** |
| Aurora Performance Insights | off | off | **on** |
| Aurora Deletion Protection | off | off | **on** |
| ElastiCache | micro × 1 | small × 2 Multi-AZ | **small × 2 Multi-AZ** (현행: `auth_token`. RBAC 전환은 별도 ADR) |
| ElastiCache 백업 | 0 | 3일 | **7일** |
| MSK | 비활성 | Serverless | **Provisioned 3.9.x KRaft, m7g.large × 3** |
| ECS desired_count | 1 | 1 | **2** (auto 2~6) |
| ALB | HTTP | HTTP | HTTP (도메인 보유 후 HTTPS 전환) |

본 디렉토리는 dev 와 **동일한 .tf 파일**을 사용한다. 환경 차이는 `terraform.tfvars` 의 변수값으로 표현되며, MSK 모드 분기(`use_msk_provisioned = true`)도 같은 main.tf 안에서 처리된다.

## 적용

```bash
cd devops/terraform/envs/prod
terraform init -backend-config=backend.hcl
terraform plan -var-file=terraform.tfvars
terraform apply
```

## 추정 월 비용

[11번 §4.0 환경별 월간 추정](../../../results/11-dr-and-cost-optimization.md#40-환경별-월간-추정-비용-시드-가정) 참조 — prod 풀 활성 ~$1,781/월, Compute SP + Aurora RI 적용 후 ~$1,606/월.

## 도메인·HTTPS 보강 (도메인 보유 후)

현재 `cluster.tf` 의 ALB 는 HTTP listener (port 80) 만. 도메인 보유 후:

1. us-east-1 ACM 인증서 발급 (CloudFront 용) — `aws_acm_certificate` (provider `aws.us_east_1`)
2. ap-northeast-2 ACM 인증서 발급 (ALB 용)
3. ALB 에 HTTPS Listener (port 443) 추가, HTTP → HTTPS 리다이렉트
4. WAF Web ACL 생성 + ALB 부착
5. CloudFront Distribution (`cloudfront-spa` 모듈) 생성
6. Route 53 레코드 생성

이 보강은 별도 PR 로 분리.

## prod 안전장치

- `aurora_deletion_protection = true` — terraform destroy 차단
- `aurora_skip_final_snapshot = false` — destroy 시 최종 스냅샷 강제
- GitHub Environment `tf-prod` 에 Required Reviewers 2명
- AWS Backup Vault Lock (Governance 30일)

## 관련

- [envs/dev/README.md](../dev/README.md)
- [11 DR 전략](../../../results/11-dr-and-cost-optimization.md)
