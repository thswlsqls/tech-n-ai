# 09. IaC (Terraform) 모듈 구조 및 구현 가이드

## 역할
당신은 IaC 엔지니어입니다. 앞선 설계(01~08)를 **Terraform 코드로 구현**하기 위한 모듈 구조, 표준, 구현 가이드를 작성합니다. HashiCorp의 Recommended Practices와 AWS Prescriptive Guidance를 기준으로 합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/09-iac/` 에 저장하세요. 예제 코드는 `devops/terraform/` 스켈레톤으로 제시.

### 산출물 1: `tooling-decision.md`
1. **IaC 도구 선정**: Terraform vs AWS CDK vs CloudFormation vs Pulumi
   - 팀 스택(Java/TypeScript), 멀티 클라우드 여지, 커뮤니티 모듈 성숙도 관점
   - **선정: Terraform** (근거 명시) 또는 대안
2. Terraform 버전 고정(`>= 1.9`), Provider 버전 고정
3. tflint, tfsec, Checkov, terraform-docs, pre-commit 훅 적용

### 산출물 2: `repo-structure.md`
권장 저장소 구조:
```
devops/terraform/
├── modules/                      # 재사용 모듈
│   ├── network/                  # VPC, subnet, NAT, endpoints
│   ├── ecs-service/              # ECS 서비스 + ALB 리스너 + 오토스케일
│   ├── aurora-mysql/
│   ├── elasticache-valkey/
│   ├── msk-serverless/
│   ├── s3-bucket/
│   ├── iam-role-workload/
│   ├── cloudfront-spa/
│   └── observability/            # 로그 그룹, 대시보드, 알람
├── envs/
│   ├── dev/
│   ├── beta/
│   └── prod/
│       ├── backend.tf            # S3 + DynamoDB lock
│       ├── providers.tf
│       ├── main.tf               # 모듈 호출만
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars      # 환경값 (비밀 제외)
└── global/                       # Organizations, Identity Center, KMS 공통
```

### 산출물 3: `state-and-backend.md`
- 원격 상태: **S3 버킷(버전관리, KMS, Object Lock) + DynamoDB Lock 테이블**
- 상태 분리 전략(환경별 + 도메인별 workspace 또는 별도 state)
- 상태 접근 IAM 역할(최소권한, 조건키 `aws:SourceVpc`)
- Import 절차, 상태 손상 복구 절차

### 산출물 4: `module-standards.md`
모든 커스텀 모듈이 따라야 할 규칙:
1. 입력: `required` vs `optional` 구분, 검증(`validation` 블록) 필수
2. 출력: 다른 모듈/스택이 사용할 핵심 값만 노출
3. 태그 표준(`module-tagging`):
   - `Project=tech-n-ai`, `Environment`, `Owner`, `CostCenter`, `DataClassification`, `ManagedBy=terraform`
4. 네이밍: `{project}-{env}-{component}-{purpose}` (e.g., `techai-prod-ecs-apiauth`)
5. 문서: `terraform-docs` 자동 생성 README
6. 테스트: `terraform test` 또는 Terratest

### 산출물 5: `module-specs.md`
모듈별 인터페이스 명세 (입력/출력/의존 관계):
- `network`: cidr_block, azs → vpc_id, public_subnet_ids, private_subnet_ids, ...
- `ecs-service`: service_name, container_image, cpu, memory, desired_count, port, ... → service_arn, target_group_arn
- `aurora-mysql`: engine_version, instance_count, kms_key_arn → cluster_endpoint, reader_endpoint, secret_arn
- (기타 모듈 동일 포맷)

### 산출물 6: `secrets-in-iac.md`
- Terraform 코드에 시크릿 **직접 작성 금지**
- 관리 방법: Secrets Manager 리소스 생성만 IaC에서, 값은 초기화 Lambda 또는 수동 로테이션
- `data "aws_secretsmanager_secret_version"` 사용 시 주의(상태 파일 노출 위험) — 대신 애플리케이션이 런타임에 조회

### 산출물 7: `examples/`
최소 작동 가능한 예제:
- `examples/vpc.tf` — `modules/network` 호출
- `examples/ecs-api-auth.tf` — api-auth 서비스 배포
- `examples/aurora.tf`
- `examples/cloudfront-next.tf`

## 베스트 프랙티스 체크리스트
- [ ] 모든 리소스에 공통 태그 자동 적용 (`default_tags`)
- [ ] 모듈은 `count`/`for_each` 남용하지 않고 목적 단위로 명확히
- [ ] Provider 버전 `~>` 범위 고정, lockfile(`.terraform.lock.hcl`) 커밋
- [ ] plan은 PR에 자동 코멘트, apply는 승인 후 CI 전용 Role로
- [ ] 드리프트 탐지 주 1회 자동 실행
- [ ] Destroy 방지 리소스(`lifecycle { prevent_destroy = true }`) — DB/KMS/상태 버킷

## 참고 자료 (공식 출처만)
- Terraform Documentation: https://developer.hashicorp.com/terraform/docs
- Terraform Recommended Practices: https://developer.hashicorp.com/terraform/cloud-docs/recommended-practices
- Terraform AWS Provider: https://registry.terraform.io/providers/hashicorp/aws/latest/docs
- Terraform Module Structure: https://developer.hashicorp.com/terraform/language/modules/develop/structure
- AWS Prescriptive Guidance — Terraform: https://docs.aws.amazon.com/prescriptive-guidance/latest/terraform-aws-provider-best-practices/
- tfsec: https://aquasecurity.github.io/tfsec/
- Checkov: https://www.checkov.io/
- AWS Tagging Strategies: https://docs.aws.amazon.com/whitepapers/latest/tagging-best-practices/tagging-best-practices.html

## 제약
- 커뮤니티 모듈 사용 시 **HashiCorp Verified** 또는 **AWS 공식 Terraform Modules**만 사용 (또는 자체 포크)
- 상태 파일에 평문 시크릿 **0건**
