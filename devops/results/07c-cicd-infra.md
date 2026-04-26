# 07c. CI/CD 파이프라인 — 인프라 (Terraform)

> **상위 문서**: [07-cicd-overview.md](07-cicd-overview.md)
> **대상**: `devops/terraform/` 디렉토리의 모든 모듈·환경(envs) — 실제 리포 경로는 [`devops/terraform/`](../terraform/README.md). 본 문서 내 워크플로 예시는 `infra/terraform/` 표기를 유지하나, 채택 시 실제 경로(`devops/terraform/...`)로 치환해야 한다.
> **연계**: [09 IaC](09-iac-terraform.md) 모듈 정의가 본 파이프라인의 입력이다.

---

## 1. Terraform 워크플로

### 1.1 디렉토리 레이아웃

```
devops/terraform/         # 실제 리포 경로 (이전 표기 infra/terraform/ → devops/terraform/)
├── modules/              # 재사용 모듈 (network, ecs-service, aurora-mysql, ...)
├── bootstrap/            # state 버킷 / DynamoDB Lock / GitHub OIDC (09 §state-bootstrap)
└── envs/
    ├── dev/
    ├── beta/
    └── prod/
```

각 환경 디렉토리는 `backend.hcl` (state 파라미터, 실제는 `backend.tf` + `-backend-config=backend.hcl` 주입), `terraform.tfvars` (변수), `main.tf` (모듈 호출), `outputs.tf`를 포함한다. 09 §state 설명 참조.

### 1.2 PR 단계 — `terraform-plan.yml`

```bash
terraform fmt -check -recursive
terraform init -backend-config=backend.hcl
terraform validate
terraform plan -out=tfplan -var-file=<env>.tfvars
terraform show -no-color tfplan > plan.txt
terraform show -json tfplan > tfplan.json

# 보안·정책 스캔
tfsec . --format sarif --out tfsec.sarif
checkov -d . --framework terraform --output sarif --output-file checkov.sarif
kics scan -p . -o kics-out --report-formats sarif
conftest test tfplan.json --policy ../../../policy/ --all-namespaces
```

- `plan.txt`를 **PR 코멘트**로 자동 게시 (`marocchino/sticky-pull-request-comment@v2`).
- tfsec / Checkov / KICS High 이상 0건이어야 머지 가능.

### 1.3 Merge 후 — `terraform-apply.yml`

- 환경별 **수동 승인 Job** (GitHub Environments `dev` → `beta` → `prod`, 각 환경에 Required reviewers 지정).
- `apply`는 동일 `tfplan` 아티팩트를 다운로드하여 실행(plan/apply 사이의 drift 방지).

---

## 2. State 관리

- Backend: **S3 + DynamoDB Lock** (버전 관리 On, S3 Object Lock GOVERNANCE 모드 — `devops/terraform/bootstrap/state.tf:51` 참조. MFA Delete는 권장이나 미적용)
- 암호화: SSE-KMS (CMK `alias/techai/tfstate` — bootstrap 단일 공유 키, [00-매트릭스 §1](00-cross-cutting-matrix.md) · [09 §state-bootstrap](09-iac-terraform.md)에서 정의)
- State 파일 key 규칙: `envs/<env>/terraform.tfstate` (또는 `envs/<env>/<stack>/terraform.tfstate`)

```hcl
terraform {
  backend "s3" {
    bucket         = "techai-tfstate-<ACCOUNT_ID>-apne2"
    key            = "envs/dev/terraform.tfstate"
    region         = "ap-northeast-2"
    encrypt        = true
    kms_key_id     = "alias/techai/tfstate"
    dynamodb_table = "techai-tflock"
  }
}
```

> **state 부트스트랩 순환 의존**: state 버킷·DynamoDB Lock·KMS 키를 어떻게 처음 만드는지는 [09 §state-bootstrap]에서 별도 부트스트랩 스크립트로 다룬다.

공식: [Terraform Recommended Practices](https://developer.hashicorp.com/terraform/cloud-docs/recommended-practices)

---

## 3. Drift Detection

- 매일 03:00 KST `terraform plan` 스케줄 실행.
- 변경 감지(`exit code 2`) 시 Slack `#devops-drift` 채널 알림 + JIRA 티켓 자동 생성.

---

## 4. 정책 준수 — OPA / Conftest

```rego
# policy/ecs.rego
package terraform.ecs

deny[msg] {
    r := input.resource.aws_ecs_task_definition[name]
    not r.execution_role_arn
    msg := sprintf("ECS task '%s' must specify execution_role_arn", [name])
}

deny[msg] {
    r := input.resource.aws_ecs_service[name]
    r.launch_type == "EC2"
    msg := sprintf("ECS service '%s' must use Fargate (project standard)", [name])
}
```

```bash
conftest test tfplan.json --policy policy/ --all-namespaces
```

---

## 5. GitHub Actions 워크플로

### 5.1 `.github/workflows/terraform-plan.yml`

```yaml
name: terraform-plan

on:
  pull_request:
    branches: [main, develop]
    paths:
      - 'infra/terraform/**'
      - '.github/workflows/terraform-plan.yml'

concurrency:
  group: tf-plan-${{ github.ref }}
  # 주의: workflow-level `concurrency` 에서는 `matrix.*` 컨텍스트를 사용할 수 없다.
  # 환경별 동시성 제어가 필요하면 job-level `concurrency` 로 이동 권장.
  cancel-in-progress: true

permissions:
  id-token: write
  contents: read
  pull-requests: write
  security-events: write

jobs:
  plan:
    name: Plan (${{ matrix.env }})
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        env: [dev, beta, prod]
    defaults:
      run:
        working-directory: infra/terraform/envs/${{ matrix.env }}
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_TERRAFORM_READONLY_ROLE_ARN }}
          aws-region: ap-northeast-2

      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.9.5
          terraform_wrapper: false

      - name: fmt
        run: terraform fmt -check -recursive

      - name: init
        run: terraform init -input=false -backend-config=backend.hcl

      - name: validate
        run: terraform validate

      - name: plan
        id: plan
        run: |
          terraform plan -input=false -out=tfplan -var-file=${{ matrix.env }}.tfvars -no-color | tee plan.txt
          terraform show -json tfplan > tfplan.json

      - name: tfsec
        uses: aquasecurity/tfsec-sarif-action@v0.1.4
        with:
          sarif_file: tfsec.sarif
          working_directory: infra/terraform/envs/${{ matrix.env }}

      - name: Checkov
        uses: bridgecrewio/checkov-action@master
        with:
          directory: infra/terraform/envs/${{ matrix.env }}
          framework: terraform
          output_format: sarif
          output_file_path: infra/terraform/envs/${{ matrix.env }}/checkov.sarif
          soft_fail: false

      - name: Conftest (OPA)
        run: |
          curl -Lo /tmp/conftest.tar.gz https://github.com/open-policy-agent/conftest/releases/download/v0.55.0/conftest_0.55.0_Linux_x86_64.tar.gz
          tar -xzf /tmp/conftest.tar.gz -C /tmp
          /tmp/conftest test tfplan.json --policy ../../../policy/ --all-namespaces

      - name: Upload SARIFs
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: infra/terraform/envs/${{ matrix.env }}/tfsec.sarif

      - name: PR comment with plan
        uses: marocchino/sticky-pull-request-comment@v2
        with:
          header: tf-plan-${{ matrix.env }}
          path: infra/terraform/envs/${{ matrix.env }}/plan.txt

      - name: Upload plan artifact
        uses: actions/upload-artifact@v4
        with:
          name: tfplan-${{ matrix.env }}-${{ github.run_id }}
          path: infra/terraform/envs/${{ matrix.env }}/tfplan
          retention-days: 14
```

### 5.2 `.github/workflows/terraform-apply.yml`

```yaml
name: terraform-apply

on:
  push:
    branches: [main, develop]
    paths:
      - 'infra/terraform/**'
      - '.github/workflows/terraform-apply.yml'
  workflow_dispatch:
    inputs:
      env:
        description: Target environment
        type: choice
        options: [dev, beta, prod]
        required: true

concurrency:
  group: tf-apply-${{ github.event.inputs.env || (github.ref == 'refs/heads/main' && 'prod' || 'dev') }}
  # workflow-level concurrency 는 `matrix.*` 컨텍스트 미지원. 본 표현은 inputs/ref 로만 구성되어 안전.
  cancel-in-progress: false

permissions:
  id-token: write
  contents: read

jobs:
  apply:
    name: Apply (${{ matrix.env }})
    runs-on: ubuntu-latest
    strategy:
      max-parallel: 1
      matrix:
        env:
          - ${{ github.event.inputs.env || (github.ref == 'refs/heads/main' && 'prod' || 'dev') }}
    environment:
      name: tf-${{ matrix.env }}
    defaults:
      run:
        working-directory: infra/terraform/envs/${{ matrix.env }}
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_TERRAFORM_APPLY_ROLE_ARN }}
          aws-region: ap-northeast-2
      - uses: hashicorp/setup-terraform@v3
        with:
          terraform_version: 1.9.5
          terraform_wrapper: false

      - name: init
        run: terraform init -input=false -backend-config=backend.hcl

      - name: plan (fresh, apply-time)
        run: terraform plan -input=false -out=tfplan -var-file=${{ matrix.env }}.tfvars

      - name: apply
        run: terraform apply -input=false -auto-approve tfplan

      - name: Notify Slack on failure
        if: failure()
        uses: slackapi/slack-github-action@v1.27.0
        with:
          webhook: ${{ secrets.SLACK_DEVOPS_WEBHOOK }}
          webhook-type: incoming-webhook
          payload: |
            {"text": ":rotating_light: terraform apply FAILED on ${{ matrix.env }} — run ${{ github.run_id }}"}
```

### 5.3 `.github/workflows/security-scan.yml` (주간 종합)

```yaml
name: security-scan

on:
  schedule:
    - cron: '0 17 * * 0'   # 매주 일요일 02:00 KST (17:00 UTC Sun)
  workflow_dispatch:

permissions:
  id-token: write
  contents: read
  security-events: write

concurrency:
  group: security-scan
  cancel-in-progress: false

jobs:
  codeql-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - uses: github/codeql-action/init@v3
        with: { languages: java-kotlin, queries: security-extended }
      - uses: gradle/actions/setup-gradle@v4
      - working-directory: tech-n-ai-backend
        run: ./gradlew build -x test --parallel --no-daemon
      - uses: github/codeql-action/analyze@v3

  codeql-frontend:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        workspace: [app, admin]
    steps:
      - uses: actions/checkout@v4
      - uses: github/codeql-action/init@v3
        with: { languages: javascript-typescript, queries: security-extended }
      - uses: github/codeql-action/analyze@v3

  trivy-repo-scan:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [api-gateway, api-emerging-tech, api-auth, api-chatbot, api-bookmark, api-agent, batch-source]
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ap-northeast-2
      - name: Resolve latest digest (single repo)
        id: latest
        run: |
          DIGEST=$(aws ecr describe-images \
            --repository-name "techai/${{ matrix.module }}" \
            --query 'sort_by(imageDetails,&imagePushedAt)[-1].imageDigest' --output text)
          echo "digest=${DIGEST}" >> "$GITHUB_OUTPUT"
      - uses: aquasecurity/trivy-action@0.24.0
        with:
          image-ref: ${{ vars.ECR_REGISTRY }}/techai/${{ matrix.module }}@${{ steps.latest.outputs.digest }}
          format: sarif
          output: trivy-${{ matrix.module }}.sarif
          severity: CRITICAL,HIGH,MEDIUM
          ignore-unfixed: true
      - uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: trivy-${{ matrix.module }}.sarif

  tfsec-full:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: aquasecurity/tfsec-sarif-action@v0.1.4
        with:
          sarif_file: tfsec.sarif
          working_directory: infra/terraform
      - uses: github/codeql-action/upload-sarif@v3
        with: { sarif_file: tfsec.sarif }

  kics-full:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: checkmarx/kics-github-action@v2.1.3
        with:
          path: infra/terraform
          output_formats: sarif
          output_path: kics-out
      - uses: github/codeql-action/upload-sarif@v3
        with: { sarif_file: kics-out/results.sarif }
```

---

## 6. 관련 문서

- [07-cicd-overview.md](07-cicd-overview.md) — 공통 정책·품질 게이트
- [09 IaC 모듈 카탈로그](09-iac-terraform.md) — apply 워크플로의 입력 모듈 정의
- [06 §2 IAM](06-security-and-iam.md) — `AWS_TERRAFORM_READONLY_ROLE_ARN`, `AWS_TERRAFORM_APPLY_ROLE_ARN` 단일 정의

---

**문서 끝.**
