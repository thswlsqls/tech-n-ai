# 09. IaC(Terraform) 모듈 구조 및 구현 가이드

> ⚙️ **실 동작 코드 위치**: 본 문서는 spec(설계 명세)이며, 실제 Terraform 모듈은 [`devops/terraform/`](../terraform/README.md) 에 있다.
>
> | 컴포넌트 | 진척 | 위치 |
> |---|---|---|
> | bootstrap (state + GitHub OIDC) | ✅ 세션 2a | [`devops/terraform/bootstrap/`](../terraform/bootstrap/README.md) |
> | modules/network | ✅ 세션 2a | [`devops/terraform/modules/network/`](../terraform/modules/network/README.md) |
> | modules/iam-role-workload | ✅ 세션 2a | [`devops/terraform/modules/iam-role-workload/`](../terraform/modules/iam-role-workload/README.md) |
> | modules/s3-bucket | ✅ 세션 2a | [`devops/terraform/modules/s3-bucket/`](../terraform/modules/s3-bucket/README.md) |
> | envs/dev 골격 | ✅ 세션 2a | [`devops/terraform/envs/dev/`](../terraform/envs/dev/README.md) |
> | modules/aurora-mysql | ✅ 세션 2b | [`devops/terraform/modules/aurora-mysql/`](../terraform/modules/aurora-mysql/README.md) |
> | modules/elasticache-valkey | ✅ 세션 2b | [`devops/terraform/modules/elasticache-valkey/`](../terraform/modules/elasticache-valkey/README.md) |
> | modules/msk-serverless | ✅ 세션 2b | [`devops/terraform/modules/msk-serverless/`](../terraform/modules/msk-serverless/README.md) |
> | modules/msk-provisioned (Kafka 3.9 KRaft) | ✅ 세션 2b | [`devops/terraform/modules/msk-provisioned/`](../terraform/modules/msk-provisioned/README.md) |
> | modules/ecs-service (Fargate + ALB Blue/Green + 워크로드 SG + 자동 롤백 알람) | ✅ 세션 2c | [`devops/terraform/modules/ecs-service/`](../terraform/modules/ecs-service/README.md) |
> | modules/cloudfront-spa (Amplify/S3 origin + 보안 헤더) | ✅ 세션 2c | [`devops/terraform/modules/cloudfront-spa/`](../terraform/modules/cloudfront-spa/README.md) |
> | modules/observability (CPU·Mem·RunningTaskCount 알람 + Overview Dashboard) | ✅ 세션 2c | [`devops/terraform/modules/observability/`](../terraform/modules/observability/README.md) |
> | envs/dev — 풀 워크로드 통합 (Cluster, ALB, 6 Service, 6 Task Role, 알람) | ✅ 세션 2c | [`devops/terraform/envs/dev/`](../terraform/envs/dev/README.md) |
> | envs/beta · envs/prod 골격 (메타파일) | ✅ 세션 2c | [`envs/beta`](../terraform/envs/beta/README.md), [`envs/prod`](../terraform/envs/prod/README.md) |
> | envs/beta · envs/prod 본문 (dev 동일 .tf + tfvars 차이, MSK 모드 분기) | ✅ 세션 5 | tfvars 만 환경 차이 |
> | Secrets Manager stub (jwt 듀얼 키, openai, mongodb, elasticache token) | ✅ 세션 5 | [`envs/dev/secrets.tf`](../terraform/envs/dev/secrets.tf) |
> | ECR 리포 7개 (단일 리포 D-1 정합) + KMS·Lifecycle·Repository Policy | ✅ 세션 6 | [`bootstrap/ecr.tf`](../terraform/bootstrap/ecr.tf) |
> | 10 런북 IaC 롤백 Quick Reference (1쪽 요약) | ✅ 세션 6 | [10 §5.0](10-deployment-runbook.md) |
> | ecs-service sidecar 옵션 (ADOT/FireLens 자동 추가, dependsOn·OTel 환경변수 자동 주입) | ✅ 세션 7 | [`ecs-service/main.tf`](../terraform/modules/ecs-service/main.tf) |
> | modules/amplify-app (Next.js 16 SSR, WEB_COMPUTE, GitHub PAT secret, monorepo) | ✅ 세션 7 | [`modules/amplify-app/`](../terraform/modules/amplify-app/README.md) |
> | envs/dev frontend.tf (app + admin × Amplify, enable_amplify 토글) | ✅ 세션 7 | [`envs/dev/frontend.tf`](../terraform/envs/dev/frontend.tf) |

> 본 문서는 선행 설계(01~08)에서 확정된 **ECS Fargate + CodeDeploy Blue/Green, MSK(Provisioned prod / Serverless dev·beta), Aurora MySQL, ElastiCache Valkey, CloudFront + Amplify(or S3), Organizations + Control Tower 기반 dev/beta/prod 분리 계정, GitHub Actions OIDC**를 **Terraform 코드로 구현**하기 위한 모듈 구조, 표준, 구현 가이드를 정의한다. HashiCorp Recommended Practices 및 AWS Prescriptive Guidance(Terraform) 기준을 채택한다.

---

## 1. 도구 선정 (tooling-decision)

### 1.1 IaC 도구 비교

| 항목 | Terraform | AWS CDK | CloudFormation | Pulumi |
|---|---|---|---|---|
| 언어 | HCL(DSL) | TypeScript/Python/Java/… | YAML/JSON | TypeScript/Python/Go/… |
| 멀티 클라우드 | O (공식 Provider 3,000+) | AWS 전용(CDKTF는 별개) | AWS 전용 | O |
| 상태 관리 | 외부(S3/DynamoDB) | CFN Stack에 의존 | CFN Stack | 외부(Pulumi Cloud/S3) |
| 모듈/재사용성 | Registry(Verified) + 자체 Module | Construct Library(L1/L2/L3) | Nested Stack / Module | ComponentResource |
| Drift 탐지 | `terraform plan`, TFC Drift Detection | CFN Drift Detection | CFN Drift Detection | `pulumi refresh` |
| 승인/Plan UX | plan 산출물 명확, PR 코멘트 성숙 | `cdk diff`는 상대적 단순 | Change Set | `pulumi preview` |
| 팀 적합성(Java/TS) | HCL 러닝커브 낮음, 도구/사례 풍부 | TS 네이티브, 코드 추상화 강점 | 러닝커브 중, 엔터프라이즈 레거시 | TS 친화적 |
| 커뮤니티 모듈 성숙도 | 최상(HashiCorp Verified + AWS) | 좋음(AWS Construct Hub) | 중간 | 중간 |
| 관측가능성/스캐너 | tfsec/Checkov/tflint/terraform-docs | cdk-nag, Checkov(일부) | cfn-lint, Checkov | Checkov(일부) |

### 1.2 선정 결론: **Terraform**

- **멀티 계정/멀티 리전 오케스트레이션**에서 Provider alias·상태 분리·원격 상태 참조(`terraform_remote_state`)가 검증된 표준 패턴.
- **팀 스택(Java/TypeScript) 무관하게 HCL이 진입 장벽이 낮고** 선언형으로 리뷰 용이 — "코드가 곧 리소스 명세"라는 특성이 인프라 리뷰 리듬과 부합.
- **커뮤니티 모듈 성숙도** — AWS 공식/HashiCorp Verified 모듈(VPC, EKS, RDS 등)을 활용해 구현 비용 최소화.
- **드리프트·Plan UX**가 PR 기반 운영에 최적(Atlantis, tfcmt, OpenTofu 생태계 호환).
- CDK는 L2/L3 추상화 이점이 있으나, 본 프로젝트는 **멀티 계정 + 멀티 환경 + 동일 설계를 재현**하는 요구가 강하므로 Terraform Module 체계가 더 적합.
- CloudFormation은 AWS 전용·Change Set UX·Drift 복구 비용으로 탈락, Pulumi는 상태 저장소(Pulumi Cloud) 종속성과 도구 체인 규모에서 Terraform 대비 열세.

### 1.3 버전 및 Provider 핀 고정

```hcl
# envs/prod/providers.tf
terraform {
  required_version = "~> 1.9.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.60"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }
}
```

- `.terraform.lock.hcl`을 **반드시 커밋**(HashiCorp Recommended Practices).
- Provider 버전 상승은 PR 별도 분리 후 stg/dev 사전 검증.

### 1.4 품질 도구 체인 (pre-commit)

| 도구 | 역할 | 실행 시점 |
|---|---|---|
| `terraform fmt` | 포매팅 | pre-commit |
| `terraform validate` | 구문/참조 검증 | pre-commit, CI |
| `tflint` | 린트(AWS Ruleset: `aws-ruleset`) | pre-commit, CI |
| `Trivy config` (Aqua) | 보안 정적 분석 (구 tfsec — 2024년 이후 Aqua가 Trivy로 통합) | CI 필수 게이트 |
| `Checkov` (Prisma) | 정책 스캔(CIS, PCI, HIPAA) | CI 필수 게이트 |
| `terraform-docs` | 모듈 README 자동 생성 | pre-commit |
| `trivy config` | IaC + SBOM 보조 | CI 보조 |

`.pre-commit-config.yaml` 예시:

```yaml
repos:
  - repo: https://github.com/antonbabenko/pre-commit-terraform
    rev: v1.96.2
    hooks:
      - id: terraform_fmt
      - id: terraform_validate
      - id: terraform_tflint
      - id: terraform_trivy   # 구 terraform_tfsec — Aqua가 2024년 이후 Trivy로 통합, tfsec hook은 아직 동작하나 신규는 Trivy 권장
      - id: terraform_checkov
      - id: terraform_docs
        args:
          - --hook-config=--path-to-file=README.md
          - --hook-config=--add-to-existing-file=true
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.6.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
```

---

## 2. 저장소 구조 (repo-structure)

### 2.1 디렉토리 트리 (실제 생성됨)

```
devops/terraform/                         # 실제 리포 경로 (`infra/terraform/` 가 아님)
├── .pre-commit-config.yaml
├── .tflint.hcl
├── .terraform-docs.yml
├── README.md
├── modules/                              # 재사용 모듈 (환경 독립)
│   ├── network/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── ecs-service/
│   │   ├── main.tf
│   │   ├── alb.tf
│   │   ├── autoscaling.tf
│   │   ├── codedeploy.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── aurora-mysql/
│   │   ├── main.tf
│   │   ├── parameter-group.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── elasticache-valkey/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── msk-serverless/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── msk-provisioned/
│   │   ├── main.tf
│   │   ├── config.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── s3-bucket/
│   │   ├── main.tf
│   │   ├── policy.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── iam-role-workload/
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   ├── cloudfront-spa/
│   │   ├── main.tf
│   │   ├── origin-amplify.tf
│   │   ├── origin-s3.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   ├── versions.tf
│   │   └── README.md
│   └── observability/
│       ├── log-groups.tf
│       ├── dashboards.tf
│       ├── alarms.tf
│       ├── variables.tf
│       ├── outputs.tf
│       ├── versions.tf
│       └── README.md
├── envs/
│   ├── dev/
│   │   ├── backend.tf
│   │   ├── providers.tf
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   ├── beta/
│   │   ├── backend.tf
│   │   ├── providers.tf
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   └── prod/
│       ├── backend.tf
│       ├── providers.tf
│       ├── main.tf
│       ├── variables.tf
│       ├── outputs.tf
│       └── terraform.tfvars
├── bootstrap/                            # state 버킷 / DynamoDB Lock / GitHub OIDC (실제 구현됨)
│                                         # 향후 Org/Control Tower/공통 KMS/Route53 등 계정 경계 자원은
│                                         # 별도 `global/` 디렉토리(미생성)로 분리 예정
└── examples/                             # 최소 작동 가능 예제
    ├── vpc.tf
    ├── ecs-api-auth.tf
    ├── aurora.tf
    └── cloudfront-amplify.tf
```

### 2.2 레이어 규칙

- `modules/*` — **환경·계정 독립**. 하드코딩된 ARN/ID/계정 금지.
- `envs/*` — 모듈 호출 **조립 계층**. 리소스 직접 선언은 지양(예외: 환경 고유의 KMS 키, Route53 레코드, SSM Parameter, ECS Cluster, ALB).
- `bootstrap/*` — state 인프라(S3 버킷·DynamoDB Lock 테이블·KMS) + GitHub OIDC IAM. 별도 state 파일. 본 디렉토리만은 `terraform.tfstate` 를 로컬 보관(부트스트랩 순환 의존 회피).
- `examples/*` — 문서/PoC용 최소 예제. CI에서 `plan` 검증만 수행.

### 2.3 모듈 호출 패턴 (`envs/prod/main.tf` 발췌)

```hcl
module "network" {
  source = "../../modules/network"

  project     = var.project
  environment = var.environment
  cidr_block  = "10.30.0.0/16"
  azs         = ["ap-northeast-2a", "ap-northeast-2b", "ap-northeast-2c"]
  tags        = local.common_tags
}

module "api_auth" {
  source = "../../modules/ecs-service"

  service_name       = "api-auth"
  container_image    = "ghcr.io/tech-n-ai/api-auth:${var.api_auth_image_tag}"
  cpu                = 1024
  memory             = 2048
  desired_count      = 3
  container_port     = 8083
  vpc_id             = module.network.vpc_id
  private_subnet_ids = module.network.private_subnet_ids
  tags               = local.common_tags
}
```

---

## 3. 상태 및 백엔드 (state-and-backend)

### 3.1 원격 상태 아키텍처

- **S3 버킷 per 계정**: `techai-tfstate-{account_id}-apne2`
  - **버전 관리** 활성화
  - **KMS CMK**(`alias/techai/tfstate`)로 SSE
  - **Object Lock** (Governance, 30일) — 사고성 삭제 보호
  - **퍼블릭 액세스 차단**(Account + Bucket 양쪽 `BlockPublicAccess=true`)
- **DynamoDB Lock 테이블**: `techai-tflock`
  - 파티션 키 `LockID` (String), On-Demand, PITR 활성화
- **계정 분리**: dev/beta/prod 각 계정이 **자기 state**만 보유. 관리 계정(management)은 `global/*` state 보유.

`envs/prod/backend.tf` (실제 구현은 `bucket` 값을 `-backend-config=backend.hcl` 로 주입 — `devops/terraform/envs/dev/backend.tf` 참조):

```hcl
terraform {
  backend "s3" {
    # bucket 은 환경별 다를 수 있으므로 `terraform init -backend-config=backend.hcl` 로 주입.
    # 예) bucket = "techai-tfstate-111122223333-apne2"
    key            = "envs/prod/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "techai-tflock"  # Terraform 1.9 기준 DynamoDB 기반 상태 잠금
    encrypt        = true
    kms_key_id     = "alias/techai/tfstate"
    # 주의: Terraform 1.10+에서 도입된 `use_lockfile`(S3 네이티브 잠금)은 본 버전(1.9)에서 미지원.
    # 상위 버전으로 업그레이드 시 DynamoDB 락 제거하고 `use_lockfile = true`로 전환 가능.
  }
}
```

### 3.2 상태 분리 전략 (Workspace vs Separate State)

| 기준 | Workspace | 별도 State(권장) |
|---|---|---|
| 환경(dev/beta/prod) | 비권장 | **별도 state 파일 + 별도 S3 prefix + 별도 계정** |
| 도메인(network/data/apps) | 규모 작을 때 유용 | **규모 중~대는 별도 state 권장** |
| 실수 위험 | `workspace select` 오인 위험 | 디렉토리 자체가 컨텍스트 |

- **정책**: 환경은 **별도 계정 + 별도 state**, 도메인은 **별도 state**(예: `envs/prod/network`, `envs/prod/data`, `envs/prod/apps`)로 분리해 Blast Radius 축소.
- 모듈 간 참조는 `terraform_remote_state` data source로 **출력만** 읽는다 (ID, ARN).

```hcl
data "terraform_remote_state" "network" {
  backend = "s3"
  config = {
    bucket = "techai-tfstate-111122223333-apne2"
    key    = "envs/prod/network/terraform.tfstate"
    region = "ap-northeast-2"
  }
}

# 사용: data.terraform_remote_state.network.outputs.private_subnet_ids
```

### 3.3 상태 접근 IAM Role

- **GitHub Actions OIDC → `TerraformCIRole`** (장기 키 0개).
- 상태 버킷/락 접근은 **별도 Role(`TerraformStateAccessRole`)** 을 `AssumeRole`.
- 조건: `aws:PrincipalTag/role=terraform`, `aws:SourceVpc` (VPC Endpoint 경유한 Self-hosted Runner 사용 시), `aws:RequestedRegion` 제한.

```hcl
# 상태 접근 전용 정책 (요지)
data "aws_iam_policy_document" "tfstate_access" {
  statement {
    actions = ["s3:ListBucket"]
    resources = [aws_s3_bucket.tfstate.arn]
  }
  statement {
    actions   = ["s3:GetObject", "s3:PutObject"]
    resources = ["${aws_s3_bucket.tfstate.arn}/*"]
    condition {
      test     = "StringEquals"
      variable = "s3:x-amz-server-side-encryption"
      values   = ["aws:kms"]
    }
  }
  statement {
    actions   = ["dynamodb:GetItem", "dynamodb:PutItem", "dynamodb:DeleteItem"]
    resources = [aws_dynamodb_table.tflock.arn]
  }
  statement {
    actions   = ["kms:Encrypt", "kms:Decrypt", "kms:GenerateDataKey*", "kms:DescribeKey"]
    resources = [aws_kms_key.tfstate.arn]
  }
}
```

### 3.4 상태 Import 절차

1. **사전**: 대상 리소스를 모듈 호출로 코드화(plan 시 "will be created" 상태 확인).
2. `terraform import 'module.network.aws_vpc.this' vpc-0abc1234` — Terraform 1.5+에서는 `import` 블록을 권장.

```hcl
import {
  to = module.network.aws_vpc.this
  id = "vpc-0abc1234"
}
```

3. `terraform plan` — diff가 **0**에 수렴할 때까지 변수/어트리뷰트 조정.
4. 승인 후 `terraform apply` — state에 반영.
5. `import` 블록은 merge 이후 다음 PR에서 **제거**.

### 3.5 상태 손상 복구

1. **증상**: `terraform plan`이 비정상 drift / lock stuck / state corrupted.
2. **Lock 해제**: `terraform force-unlock <LOCK_ID>` (최소 2인 승인 후).
3. **백업 복구**:
   - S3 버전 관리에서 **직전 정상 버전**의 `terraform.tfstate` 복사.
   - `aws s3api copy-object --bucket ... --version-id ... --key ...`
4. **검증**: `terraform state list`, `terraform plan` diff 최소화.
5. **사후**: Postmortem + `terraform state` 조작 감사 로그(CloudTrail DataEvents) 확인.

---

## 4. 모듈 표준 (module-standards)

### 4.1 입력(`variables.tf`) 규칙

- `required`: `default` 미지정.
- `optional`: `default` 지정 + `nullable` 명시.
- **`validation` 블록 필수** — 타입 체크만으로 불충분한 모든 입력에 검증.

```hcl
variable "environment" {
  description = "배포 환경 (dev/beta/prod)"
  type        = string

  validation {
    condition     = contains(["dev", "beta", "prod"], var.environment)
    error_message = "environment는 dev|beta|prod 중 하나여야 합니다."
  }
}

variable "cidr_block" {
  description = "VPC CIDR 블록 (/16 권장)"
  type        = string

  validation {
    condition     = can(cidrhost(var.cidr_block, 0)) && tonumber(split("/", var.cidr_block)[1]) <= 16
    error_message = "유효한 CIDR(/16 이하)만 허용됩니다."
  }
}

variable "desired_count" {
  description = "ECS 태스크 희망 개수"
  type        = number
  default     = 2
  nullable    = false

  validation {
    condition     = var.desired_count >= 1 && var.desired_count <= 100
    error_message = "desired_count는 1~100 범위여야 합니다."
  }
}
```

### 4.2 출력(`outputs.tf`) 규칙

- **다른 모듈/스택이 필요로 하는 값만** 노출(ID, ARN, Endpoint).
- 민감 값은 `sensitive = true`, 평문 노출 금지.
- `description` 필수.

### 4.3 공통 태그 표준

`envs/*/main.tf`의 `locals`와 `providers.tf`의 `default_tags`로 **두 층 자동 적용**.

```hcl
# envs/prod/main.tf
locals {
  common_tags = {
    Project            = "tech-n-ai"
    Environment        = var.environment      # dev|beta|prod
    Owner              = "platform-eng"
    CostCenter         = "CC-1001"
    DataClassification = "Internal"           # Public|Internal|Confidential|Restricted
    ManagedBy          = "terraform"
    Repo               = "tech-n-ai/devops"
  }
}

# envs/prod/providers.tf
provider "aws" {
  region = "ap-northeast-2"
  default_tags {
    tags = local.common_tags
  }
}
```

**AWS Tagging Strategies whitepaper** 기준 6대 태그(Project/Environment/Owner/CostCenter/DataClassification/ManagedBy) 100% 커버.

### 4.4 네이밍 규칙

- 형식: `techai-{env}-{component}-{purpose}`
  - `techai-prod-ecs-apiauth`
  - `techai-prod-aurora-core`
  - `techai-dev-msk-serverless`
  - `techai-prod-s3-rawdata`
- 길이 제약(예: IAM Role ≤ 64, S3 ≤ 63) 고려해 `purpose`는 10자 이내 권장.

### 4.5 문서·테스트

- **`terraform-docs`** — 각 모듈 `README.md` 상단 `<!-- BEGIN_TF_DOCS --> ... <!-- END_TF_DOCS -->` 블록 자동 갱신.
- **테스트**:
  - 기본: **`terraform test`** (Terraform 1.6+) — `tests/*.tftest.hcl`.
  - 통합: **Terratest**(Go) — ALB Health Check, ECS Task 기동, Aurora 접속 테스트.
- 모든 모듈 최소 1개의 `tests/plan_only.tftest.hcl` 보유(구조적 검증).

```hcl
# modules/network/tests/plan_only.tftest.hcl
run "valid_cidr" {
  command = plan

  variables {
    project     = "techai"
    environment = "dev"
    cidr_block  = "10.10.0.0/16"
    azs         = ["ap-northeast-2a", "ap-northeast-2b"]
  }

  assert {
    condition     = output.vpc_id != null
    error_message = "vpc_id output이 비어있습니다."
  }
}
```

### 4.6 라이프사이클

- DB/KMS/상태 버킷: `lifecycle { prevent_destroy = true }` 필수.
- 불가역 속성(예: Aurora `engine`, KMS `key_usage`): `ignore_changes = [engine_version]` 대신 **변경 승인 절차**로 해결 — 드리프트 숨기지 않는다.

---

## 5. 모듈 스펙 (module-specs)

각 모듈의 **입력/출력/의존**. 컴포넌트명은 `iam-role-workload`의 `workload_name`과 같이 명확히.

### 5.1 `network`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `project` | string | O | 프로젝트 식별자(`techai`) |
| 입력 | `environment` | string | O | dev/beta/prod |
| 입력 | `cidr_block` | string | O | VPC CIDR(/16) |
| 입력 | `azs` | list(string) | O | 3 AZ 이상 권장 |
| 입력 | `enable_nat_gateway` | bool | X(true) | — |
| 입력 | `enable_vpc_endpoints` | bool | X(true) | S3/ECR/Logs/KMS/STS/SecretsManager |
| 입력 | `tags` | map(string) | X({}) | 추가 태그 |
| 출력 | `vpc_id` | string | — | — |
| 출력 | `public_subnet_ids` | list(string) | — | — |
| 출력 | `private_subnet_ids` | list(string) | — | — |
| 출력 | `data_subnet_ids` | list(string) | — | Aurora/ElastiCache 전용 |
| 출력 | `nat_gateway_ids` | list(string) | — | — |
| 출력 | `vpc_endpoint_ids` | map(string) | — | — |

**의존**: 없음(루트 모듈).

### 5.2 `ecs-service`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `service_name` | string | O | `api-auth` |
| 입력 | `container_image` | string | O | ECR/GHCR URI |
| 입력 | `cpu` | number | X(512) | Fargate CPU |
| 입력 | `memory` | number | X(1024) | Fargate Memory |
| 입력 | `desired_count` | number | X(2) | — |
| 입력 | `container_port` | number | O | — |
| 입력 | `vpc_id` | string | O | `network.vpc_id` |
| 입력 | `private_subnet_ids` | list(string) | O | — |
| 입력 | `alb_listener_arn` | string | O | 공용 ALB Listener ARN |
| 입력 | `health_check_path` | string | X(`/actuator/health`) | — |
| 입력 | `task_role_arn` | string | O | `iam-role-workload` 출력 |
| 입력 | `execution_role_arn` | string | O | — |
| 입력 | `enable_blue_green` | bool | X(true) | CodeDeploy 연동 |
| 입력 | `autoscaling_min` / `max` / `cpu_target` | number | X | — |
| 출력 | `service_arn` | string | — | — |
| 출력 | `task_definition_arn` | string | — | — |
| 출력 | `blue_target_group_arn` | string | — | — |
| 출력 | `green_target_group_arn` | string | — | — |
| 출력 | `codedeploy_app_name` | string | — | — |

**의존**: `network`, `iam-role-workload`, `observability`(Log Group).

### 5.3 `aurora-mysql`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `cluster_name` | string | O | `techai-prod-aurora-core` |
| 입력 | `engine_version` | string | O | `8.0.mysql_aurora.3.07.1` |
| 입력 | `instance_count` | number | X(2) | Writer 1 + Reader n |
| 입력 | `instance_class` | string | X(`db.r7g.large`) | — |
| 입력 | `vpc_id` / `data_subnet_ids` | — | O | — |
| 입력 | `kms_key_arn` | string | O | 저장 암호화 |
| 입력 | `db_name` | string | O | — |
| 입력 | `master_username` | string | O | — |
| 입력 | `master_user_secret_kms_key_id` | string | O | Managed Master User Password |
| 입력 | `backup_retention_days` | number | X(7/14/35) | 환경별 |
| 입력 | `deletion_protection` | bool | X(true) | prod=true 강제 |
| 입력 | `allowed_security_group_ids` | list(string) | O | — |
| 출력 | `cluster_endpoint` | string | — | Writer |
| 출력 | `reader_endpoint` | string | — | — |
| 출력 | `cluster_arn` | string | — | — |
| 출력 | `master_user_secret_arn` | string | sensitive | — |
| 출력 | `security_group_id` | string | — | — |

**의존**: `network`, 공통 KMS.

### 5.4 `elasticache-valkey`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `cluster_name` | string | O | — |
| 입력 | `engine_version` | string | X(`8.0`) | Valkey |
| 입력 | `node_type` | string | X(`cache.t4g.small`) | — |
| 입력 | `num_node_groups` | number | X(1) | 샤드 수 |
| 입력 | `replicas_per_node_group` | number | X(1) | — |
| 입력 | `automatic_failover_enabled` | bool | X(true) | — |
| 입력 | `transit_encryption_enabled` | bool | X(true) | — |
| 입력 | `at_rest_encryption_enabled` | bool | X(true) | — |
| 입력 | `kms_key_arn` | string | O | — |
| 입력 | `vpc_id` / `data_subnet_ids` | — | O | — |
| 출력 | `primary_endpoint` | string | — | — |
| 출력 | `reader_endpoint` | string | — | — |
| 출력 | `security_group_id` | string | — | — |

**의존**: `network`, 공통 KMS.

### 5.5 `msk-serverless` (dev/beta)

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `cluster_name` | string | O | — |
| 입력 | `vpc_id` / `private_subnet_ids` | — | O | 3 AZ 권장 |
| 입력 | `client_security_group_ids` | list(string) | O | — |
| 입력 | `iam_sasl_enabled` | bool | X(true) | — |
| 출력 | `bootstrap_brokers_sasl_iam` | string | — | — |
| 출력 | `cluster_arn` | string | — | — |

### 5.6 `msk-provisioned` (prod)

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `cluster_name` | string | O | — |
| 입력 | `kafka_version` | string | X(`3.9.x.kraft`) | KRaft 모드 (D-8 결정) |
| 입력 | `broker_count` | number | X(3) | — |
| 입력 | `broker_instance_type` | string | X(`kafka.m7g.large`) | — |
| 입력 | `ebs_volume_size` | number | X(500) | GiB (`modules/msk-provisioned/variables.tf` 기본값) |
| 입력 | `kms_key_arn` | string | O | 저장 암호화 |
| 입력 | `encryption_in_transit` | object | (모듈 내부 고정) | TLS In-Cluster=true, ClientBroker=TLS |
| 입력 | `enhanced_monitoring` | string | X(`PER_TOPIC_PER_PARTITION`) | `modules/msk-provisioned/variables.tf` 기본값 |
| 입력 | `configuration` | (모듈 내부 정의) | — | auto.create.topics.enable=false, RF=3, min.isr=2 등 (modules/msk-provisioned/main.tf:106-128) |
| 출력 | `bootstrap_brokers_sasl_iam` | string | — | — |
| 출력 | `bootstrap_brokers_tls` | string | — | mTLS 클라이언트(외부 파트너) 옵션 |
| 출력 | `cluster_arn` | string | — | — |
| 출력 | `open_monitoring_prometheus_jmx_endpoint_list` | list(string) | — | Prometheus 스크레이프 (KRaft 모드, ZooKeeper 미사용) |

### 5.7 `s3-bucket`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `bucket_name` | string | O | 전역 유일 |
| 입력 | `kms_key_arn` | string | O | SSE-KMS |
| 입력 | `versioning_enabled` | bool | X(true) | — |
| 입력 | `object_lock_enabled` | bool | X(false) | raw/감사 로그는 true |
| 입력 | `lifecycle_rules` | list(object) | X | Glacier/Deep Archive 전환 |
| 입력 | `block_public_access` | bool | X(true) | 강제 |
| 입력 | `bucket_policy_json` | string | X(null) | — |
| 출력 | `bucket_arn` | string | — | — |
| 출력 | `bucket_domain_name` | string | — | — |
| 출력 | `bucket_regional_domain_name` | string | — | — |

### 5.8 `iam-role-workload`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `workload_name` | string | O | `api-auth` |
| 입력 | `trust_service` | string | O | `ecs-tasks.amazonaws.com` 등 |
| 입력 | `managed_policy_arns` | list(string) | X([]) | — |
| 입력 | `inline_policies` | map(string) | X({}) | name→json |
| 입력 | `permissions_boundary_arn` | string | X(null) | 강제 정책 |
| 출력 | `role_arn` | string | — | — |
| 출력 | `role_name` | string | — | — |

### 5.9 `cloudfront-spa`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `domain_aliases` | list(string) | O | `www.tech-n-ai.com` |
| 입력 | `acm_certificate_arn` | string | O | `us-east-1` 발급 |
| 입력 | `origin_type` | string | O | `amplify` \| `s3` |
| 입력 | `amplify_app_domain` | string | 조건 | origin_type=amplify |
| 입력 | `s3_bucket_regional_domain` | string | 조건 | origin_type=s3 |
| 입력 | `waf_web_acl_arn` | string | X(null) | CLOUDFRONT scope |
| 입력 | `default_ttl_seconds` | number | X(300) | — |
| 출력 | `distribution_id` | string | — | — |
| 출력 | `distribution_domain_name` | string | — | — |

### 5.10 `observability`

| 구분 | 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|---|
| 입력 | `service_name` | string | O | — |
| 입력 | `log_group_retention_days` | number | X(30/90/365) | 환경별 |
| 입력 | `log_kms_key_arn` | string | O | — |
| 입력 | `alarm_sns_topic_arn` | string | O | — |
| 입력 | `dashboard_widgets` | list(object) | X([]) | — |
| 출력 | `log_group_arn` | string | — | — |
| 출력 | `dashboard_arn` | string | — | — |

---

## 6. IaC 시크릿 관리 (secrets-in-iac)

### 6.1 원칙

1. **Terraform 코드/tfvars에 시크릿 직접 작성 금지**.
2. **Secrets Manager 리소스 자체는 IaC로 관리**, **값(`secret_string`)은 IaC 바깥**에서 설정/로테이션.
3. **`data "aws_secretsmanager_secret_version"` 사용 시** — 조회 값이 **tfstate에 평문 저장**되어 **절대 금지**. 대신 런타임 조회.
4. 상태 파일에 평문 시크릿 **0건** (CI에서 `tfstate` grep 스캔 권장).

### 6.2 권장 패턴

```hcl
# (A) Secrets Manager 껍데기만 IaC
resource "aws_secretsmanager_secret" "api_auth_jwt" {
  name        = "techai/${var.environment}/api-auth/jwt-signing-key"
  kms_key_id  = var.kms_key_arn
  description = "JWT signing key (rotated by Lambda)"

  lifecycle {
    prevent_destroy = true
  }
}

# (B) Aurora: Managed Master User Password (RDS가 Secret 자동 관리)
resource "aws_rds_cluster" "this" {
  # ...
  manage_master_user_password   = true
  master_user_secret_kms_key_id = var.kms_key_arn
}

# (C) 초기값 주입은 별도 Lambda 또는 운영자가 CLI로 수행
#   aws secretsmanager put-secret-value --secret-id ... --secret-string file://...
```

### 6.3 안티 패턴(금지)

```hcl
# 금지 1: tfvars에 평문
db_password = "P@ssw0rd!"   # 금지

# 금지 2: data source로 값 읽기 → tfstate에 평문 저장
data "aws_secretsmanager_secret_version" "bad" {
  secret_id = aws_secretsmanager_secret.api_auth_jwt.id
}
# data.aws_secretsmanager_secret_version.bad.secret_string  ← tfstate 노출
```

### 6.4 애플리케이션 런타임 조회

- ECS Task Definition의 `secrets` 필드 → **Execution Role이 Secrets Manager에서 주입**.
- 애플리케이션은 환경 변수/`aws-secretsmanager-caching`으로 조회.
- IaC는 `secretsmanager:GetSecretValue` **권한만** 부여(값 접근 X).

```hcl
secrets = [
  {
    name      = "JWT_SIGNING_KEY"
    valueFrom = aws_secretsmanager_secret.api_auth_jwt.arn
  }
]
```

### 6.5 로테이션

- Aurora/ElastiCache — **Managed Rotation**(RDS/ElastiCache가 자동).
- 커스텀 — **Secrets Manager Rotation Lambda**를 IaC로 배치하되, **Lambda 코드는 별도 레포**에서 관리(인프라/애플리케이션 코드 분리).

---

## 7. 예제 스니펫 (examples)

### 7.1 `examples/vpc.tf` — `modules/network` 호출

```hcl
terraform {
  required_version = "~> 1.9.5"
  required_providers {
    aws = { source = "hashicorp/aws", version = "~> 5.60" }
  }
}

provider "aws" {
  region = "ap-northeast-2"
  default_tags {
    tags = {
      Project            = "tech-n-ai"
      Environment        = "dev"
      Owner              = "platform-eng"
      CostCenter         = "CC-1001"
      DataClassification = "Internal"
      ManagedBy          = "terraform"
    }
  }
}

module "network" {
  source = "../modules/network"

  project              = "techai"
  environment          = "dev"
  cidr_block           = "10.10.0.0/16"
  azs                  = ["ap-northeast-2a", "ap-northeast-2b", "ap-northeast-2c"]
  enable_nat_gateway   = true
  enable_vpc_endpoints = true
}

output "vpc_id"             { value = module.network.vpc_id }
output "private_subnet_ids" { value = module.network.private_subnet_ids }
output "data_subnet_ids"    { value = module.network.data_subnet_ids }
```

### 7.2 `examples/ecs-api-auth.tf` — api-auth 서비스 배포

```hcl
data "terraform_remote_state" "network" {
  backend = "s3"
  config = {
    bucket = "techai-tfstate-111122223333-apne2"
    key    = "envs/dev/network/terraform.tfstate"
    region = "ap-northeast-2"
  }
}

module "api_auth_role" {
  source = "../modules/iam-role-workload"

  workload_name = "api-auth"
  trust_service = "ecs-tasks.amazonaws.com"

  inline_policies = {
    "read-jwt-secret" = data.aws_iam_policy_document.read_jwt.json
  }

  permissions_boundary_arn = "arn:aws:iam::111122223333:policy/TechAiWorkloadBoundary"
}

data "aws_iam_policy_document" "read_jwt" {
  statement {
    actions   = ["secretsmanager:GetSecretValue", "kms:Decrypt"]
    resources = [
      "arn:aws:secretsmanager:ap-northeast-2:111122223333:secret:techai/dev/api-auth/*",
      "arn:aws:kms:ap-northeast-2:111122223333:key/..."
    ]
  }
}

module "observability_api_auth" {
  source = "../modules/observability"

  service_name              = "api-auth"
  log_group_retention_days  = 30
  log_kms_key_arn           = "arn:aws:kms:ap-northeast-2:111122223333:key/..."
  alarm_sns_topic_arn       = "arn:aws:sns:ap-northeast-2:111122223333:techai-dev-alerts"
}

module "api_auth" {
  source = "../modules/ecs-service"

  service_name         = "api-auth"
  container_image      = "111122223333.dkr.ecr.ap-northeast-2.amazonaws.com/api-auth:1.4.2"
  cpu                  = 512
  memory               = 1024
  desired_count        = 2
  container_port       = 8083
  vpc_id               = data.terraform_remote_state.network.outputs.vpc_id
  private_subnet_ids   = data.terraform_remote_state.network.outputs.private_subnet_ids
  alb_listener_arn     = "arn:aws:elasticloadbalancing:ap-northeast-2:111122223333:listener/..."
  task_role_arn        = module.api_auth_role.role_arn
  execution_role_arn   = "arn:aws:iam::111122223333:role/techai-dev-ecs-exec"
  health_check_path    = "/actuator/health"
  enable_blue_green    = true
  autoscaling_min      = 2
  autoscaling_max      = 10
  autoscaling_cpu_target = 60
}
```

### 7.3 `examples/aurora.tf`

```hcl
module "aurora_core" {
  source = "../modules/aurora-mysql"

  cluster_name                  = "techai-dev-aurora-core"
  engine_version                = "8.0.mysql_aurora.3.07.1"
  instance_count                = 2
  instance_class                = "db.r7g.large"
  vpc_id                        = data.terraform_remote_state.network.outputs.vpc_id
  data_subnet_ids               = data.terraform_remote_state.network.outputs.data_subnet_ids
  kms_key_arn                   = "arn:aws:kms:ap-northeast-2:111122223333:key/..."
  master_user_secret_kms_key_id = "arn:aws:kms:ap-northeast-2:111122223333:key/..."
  db_name                       = "techai"
  master_username               = "admin"
  backup_retention_days         = 7
  deletion_protection           = true

  allowed_security_group_ids = [
    module.api_auth.security_group_id
  ]
}

output "aurora_writer_endpoint" {
  value = module.aurora_core.cluster_endpoint
}

output "aurora_master_secret_arn" {
  value     = module.aurora_core.master_user_secret_arn
  sensitive = true
}
```

### 7.4 `examples/cloudfront-amplify.tf`

```hcl
module "spa_cdn" {
  source = "../modules/cloudfront-spa"

  domain_aliases      = ["www.tech-n-ai.com", "tech-n-ai.com"]
  acm_certificate_arn = "arn:aws:acm:us-east-1:111122223333:certificate/...."
  origin_type         = "amplify"
  amplify_app_domain  = "main.dxxxxxx.amplifyapp.com"
  waf_web_acl_arn     = "arn:aws:wafv2:us-east-1:111122223333:global/webacl/techai-prod-waf/...."
  default_ttl_seconds = 300
}

output "cdn_domain" {
  value = module.spa_cdn.distribution_domain_name
}
```

---

## 8. 베스트 프랙티스 체크리스트

- [x] **공통 태그 자동 적용** — Provider `default_tags` + 모듈 내부 `merge(var.tags, local.module_tags)`.
- [x] **모듈은 목적 단위** — `count`/`for_each` 남용 금지. `modules/ecs-service`는 "하나의 서비스"만 취급. 복수 서비스는 호출 측에서 반복.
- [x] **Provider 버전 `~>` 범위 고정 + `.terraform.lock.hcl` 커밋**.
- [x] **PR 기반 워크플로** — PR에 `terraform plan` 자동 코멘트, apply는 **환경별 승인(Required Reviewers) + CI 전용 OIDC Role**로만.
- [x] **Drift 탐지 주 1회** — GitHub Actions scheduled workflow로 `terraform plan -detailed-exitcode`; 0 아님 → Slack 알림.
- [x] **`prevent_destroy` 필수 리소스** — Aurora Cluster, MSK Cluster, KMS Key, S3 상태/로그 버킷, Route53 Hosted Zone.
- [x] **보안 스캔 필수 게이트** — `Trivy config`(구 tfsec, Aqua 통합), `Checkov` Critical/High = 0 아니면 머지 차단.
- [x] **시크릿 격리** — Secrets Manager 리소스만 IaC, 값은 런타임/로테이션 Lambda, `tfstate`에 평문 0건.
- [x] **태그 정책** — Organizations **Tag Policy**로 `Project/Environment/ManagedBy` 누락 리소스 감지.
- [x] **상태 접근 최소권한** — `TerraformStateAccessRole`은 상태 버킷/락/KMS만, 워크로드 권한과 분리.
- [x] **Import는 코드로** — Terraform 1.5+ `import { to = ... id = ... }` 블록, merge 후 제거.
- [x] **출력 최소화** — 모듈 Output은 다음 계층이 **실제 사용하는 값만**. ARN/ID/Endpoint 위주.
- [x] **`validation` 블록** — 모든 자유 입력에 필수.
- [x] **`terraform test`** — 모듈별 최소 `plan_only` 테스트 1개, 핵심 모듈(network/ecs-service/aurora)은 Terratest.
- [x] **Provider Region Guard** — `default_tags`와 함께 `allowed_account_ids`, `default_tags`에 `aws:RequestedRegion` 조건을 Role에서 강제.
- [x] **레지스트리 검증** — 커뮤니티 모듈은 **HashiCorp Verified**(예: `terraform-aws-modules/vpc/aws`) 또는 **AWS 공식** 모듈만 사용. 그 외는 자체 포크.

---

## 9. 참고 자료 (공식 출처만)

- Terraform Documentation — https://developer.hashicorp.com/terraform/docs
- Terraform Recommended Practices — https://developer.hashicorp.com/terraform/cloud-docs/recommended-practices
- Terraform AWS Provider — https://registry.terraform.io/providers/hashicorp/aws/latest/docs
- Terraform Module Structure — https://developer.hashicorp.com/terraform/language/modules/develop/structure
- AWS Prescriptive Guidance — Terraform AWS Provider Best Practices — https://docs.aws.amazon.com/prescriptive-guidance/latest/terraform-aws-provider-best-practices/
- Trivy (Aqua, 구 tfsec 통합) — https://github.com/aquasecurity/trivy
- Checkov — https://www.checkov.io/
- AWS Tagging Best Practices (Whitepaper) — https://docs.aws.amazon.com/whitepapers/latest/tagging-best-practices/tagging-best-practices.html

> 위 공식 출처 외의 블로그/AI 생성 문서는 본 가이드의 근거로 인용하지 않는다. 커뮤니티 모듈은 HashiCorp Verified 또는 AWS 공식 Terraform Modules만 사용한다.
