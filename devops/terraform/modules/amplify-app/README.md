# `modules/amplify-app` — Amplify Hosting (Next.js 16 SSR)

> 03 §3.4 Amplify 설계 + 07b 프론트 배포 워크플로 정합. `WEB_COMPUTE` 플랫폼으로 Next.js 16 SSR/Server Components 지원.

## 사용 예 — dev app

```hcl
module "amplify_app" {
  source = "../../modules/amplify-app"

  project     = "techai"
  environment = "dev"
  app_name    = "app"

  repository_url                  = "https://github.com/your-org/tech-n-ai-frontend"
  github_access_token_secret_arn  = aws_secretsmanager_secret.github_pat.arn

  branch_name = "develop"
  stage       = "DEVELOPMENT"
  platform    = "WEB_COMPUTE"

  environment_variables = {
    NEXT_PUBLIC_APP_NAME = "tech-n-ai"
  }

  branch_environment_variables = {
    NEXT_PUBLIC_BUILD_ENV = "dev"
  }
}
```

## 사용 예 — prod admin (Basic Auth)

```hcl
module "amplify_admin" {
  source = "../../modules/amplify-app"

  project     = "techai"
  environment = "prod"
  app_name    = "admin"

  repository_url                  = "https://github.com/your-org/tech-n-ai-frontend"
  github_access_token_secret_arn  = aws_secretsmanager_secret.github_pat.arn

  branch_name = "main"
  stage       = "PRODUCTION"

  enable_basic_auth      = true
  basic_auth_credentials = base64encode("admin:${random_password.admin.result}")
}
```

## 핵심 설계 결정

- **`platform = "WEB_COMPUTE"`** — Next.js 16 의 SSR/Route Handlers/Server Components 지원에 필수. 기본 `WEB`은 정적+함수만.
- **`enable_branch_auto_build = false`** — Amplify Console 의 자동 빌드 차단. GitHub Actions 의 `aws amplify start-job` 으로만 트리거 (D-1 불변 아티팩트 + CI 일관성).
- **`enable_pull_request_preview = false`** — 비용 절감 (PR 마다 환경 생성 X). 필요 시 dev 환경에서만 true.
- **PAT 은 Secrets Manager 에 보관** — `github_access_token_secret_arn` 으로 ARN 만 받고, 모듈이 fetch 후 사용. lifecycle.ignore_changes 로 회전 시 plan 노이즈 회피.
- **build_spec 의 pre-build 에서 SSM Parameter 가져옴** — `NEXT_PUBLIC_*` 등을 빌드타임 인라인. 환경별 차이는 SSM 만으로 표현.

## 환경변수 우선순위

1. **Branch 레벨** (`branch_environment_variables`) — 최우선
2. **App 레벨** (`environment_variables`) — fallback
3. **build_spec 의 pre-build SSM fetch** — 빌드타임 동적 주입

## GitHub Actions 와의 통합

```yaml
# .github/workflows/frontend-ci-cd.yml 의 deploy job
- name: Trigger Amplify deployment
  run: |
    aws amplify start-job \
      --app-id ${{ vars.AMPLIFY_APP_ID }} \
      --branch-name develop \
      --job-type RELEASE \
      --commit-id "${GITHUB_SHA}"
```

App ID 는 본 모듈 출력 `app_id`. Terraform `output -raw app_id` → GitHub Variables 에 등록.

## 비용

| 항목 | 단가 (서울) | 시드 추정 |
|---|---|---|
| Amplify Build (분) | $0.01/분 | 빌드 5분 × 일 5회 = $7.5/월 |
| Amplify Hosting (요청) | $0.30/M requests | 시드 < $1 |
| Amplify Hosting (전송) | $0.15/GB | 시드 < $5 |
| SSR Compute | $0.30/시간 (활성 시) | 시드 < $5 |
| **합계 (시드)** | | **~$15~20/월/앱** |

## 주의

- **Repository Connect 후 PAT 권한 검증**: Amplify 가 리포 클론에 PAT 의 `repo:read` + 웹훅 등록을 위해 `admin:repo_hook` 권한 필요.
- **Custom Domain**: 본 모듈은 도메인 미연결. 도메인 보유 후 `aws_amplify_domain_association` 자원을 envs 에서 추가.
- **Monorepo**: `AMPLIFY_MONOREPO_APP_ROOT` 환경변수가 frontend 디렉토리(`app` / `admin`)를 가리킴.
