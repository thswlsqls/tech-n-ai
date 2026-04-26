# 07. CI/CD 파이프라인 설계 및 구현 가이드

## 역할
당신은 DevOps 엔지니어로, 백엔드/프론트엔드/인프라(IaC)를 모두 포괄하는 **엔드-투-엔드 CI/CD 파이프라인**을 설계·구현합니다. DORA 4 metrics(배포 빈도, 리드 타임, 변경 실패율, MTTR) 개선을 목표로 합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/07-cicd/` 에 저장하고, 예제 워크플로는 `.github/workflows/` 형태의 YAML 스니펫으로 제시하세요.

### 산출물 1: `pipeline-overview.md`
1. **CI/CD 도구 선정**
   - **GitHub Actions + AWS OIDC** vs **AWS CodePipeline/CodeBuild/CodeDeploy** 비교
   - 선정 근거(이미 GitHub 사용, 외부 PR 기여자 고려 등)
2. **브랜치 전략**
   - `main`(prod), `develop`(beta), `feature/*`, `hotfix/*` (GitHub Flow 또는 Trunk-based 선택)
   - 릴리즈 태깅 규칙(SemVer + Git SHA)
3. **전체 파이프라인 플로우**
   - Mermaid 다이어그램: Commit → Build → Test → Security Scan → Image Push → Deploy dev → Smoke Test → Promote beta → Manual Approval → prod
4. **환경 승격(Promotion)** 전략 — 이미지 재빌드 없이 동일 아티팩트를 환경 간 승격

### 산출물 2: `backend-pipeline.md`
1. **Gradle 빌드 & 테스트**
   - 멀티모듈 병렬 빌드 (`./gradlew build --parallel --build-cache`)
   - 테스트 분리: unit / integration(Testcontainers) / contract
   - 리포트: JUnit, Jacoco (커버리지 게이트 70%+)
2. **정적 분석**
   - SpotBugs, Checkstyle, PMD 중 선택
   - SonarQube/SonarCloud 연동(선택)
3. **보안 스캔**
   - **SCA**: `gradle-dependency-check` 또는 Snyk Open Source / GitHub Dependabot
   - **SAST**: CodeQL(GitHub Advanced Security) 또는 Semgrep
   - **컨테이너 스캔**: ECR Enhanced Scanning(Inspector) + Trivy (PR 단계)
   - **IaC 스캔**: tfsec / Checkov / KICS
4. **이미지 빌드 & 푸시**
   - **기본 방식(프로젝트 현재 구성 유지)**: Spring Boot `bootBuildImage`(Paketo Buildpacks) 사용
     ```bash
     ./gradlew :api-auth:bootBuildImage \
       --imageName=$ECR_URI/{env}/api-auth:${GIT_SHA} \
       -PbuildImage.publish=true
     ```
     - Paketo Java Buildpack 및 빌더 이미지 버전을 `gradle.properties`에 고정
     - ECR 로그인은 `aws ecr get-login-password | docker login ...`로 선행
   - **대안**: 전통적 Dockerfile(layered JAR) + Docker Buildx — 선택 시 03번 문서의 결정과 일관 유지
   - ECR에 `{env}/{module}:{git-sha}` 사용(`latest` 태그는 prod에서 금지)
   - **이미지 서명**: **Notation(CNCF) + AWS Signer Plugin**(ECR 네이티브 지원) 또는 **Cosign**(Sigstore) 중 택일. "AWS Signer for Container Images"는 단독 기능명이 아니며, AWS Signer는 Notation 플러그인을 통해 ECR 이미지 서명을 지원
5. **배포**
   - ECS 선택 시: `aws ecs update-service` + CodeDeploy Blue/Green
   - EKS 선택 시: Argo CD 또는 Helm + GitOps

### 산출물 3: `frontend-pipeline.md`
1. **Next.js 빌드**
   - `npm ci` → `npm run lint` → `npm run build` → (선택) Playwright E2E
   - Node LTS 버전 고정, 캐시(`actions/cache`)
2. **번들 최적화 & 검증**
   - Bundle size 리포트, Lighthouse CI
3. **배포**
   - Amplify Hosting 선택 시: Amplify CLI / GitHub 연동
   - CloudFront+S3 선택 시: `aws s3 sync` + CloudFront Invalidation (변경된 경로만)
   - SSR on Fargate: 백엔드 파이프라인과 동일 패턴

### 산출물 4: `infra-pipeline.md`
1. **Terraform 파이프라인**
   - PR 단계: `terraform fmt/validate/plan` + tfsec/Checkov → plan을 PR 코멘트로 게시
   - Merge 후: `terraform apply`는 환경별 수동 승인 Job
   - 상태 파일: S3 + DynamoDB Lock (KMS 암호화)
2. **Drift Detection**: 주기적 `terraform plan` 예약 작업
3. **정책 준수**: OPA/Conftest 또는 HashiCorp Sentinel

### 산출물 5: `github-actions-examples/`
다음 워크플로 YAML을 실제 작동 가능하게 작성:
- `backend-ci.yml` — PR용
- `backend-cd.yml` — main/develop merge 시 배포
- `frontend-ci-cd.yml` — app/admin 분리 (path filter)
- `terraform-plan.yml`, `terraform-apply.yml`
- `security-scan.yml` — 주간 예약 전체 스캔

각 워크플로는 다음을 포함:
- `permissions: id-token: write` (OIDC)
- `aws-actions/configure-aws-credentials@v4` (role-to-assume)
- 캐시 전략, matrix 전략
- 동시성(`concurrency`) 제어

### 산출물 6: `quality-gates.md`
PR 머지 전 필수 통과 항목(Required Checks):
- 빌드/테스트 성공, 커버리지 ≥ 70%
- Lint 0 error
- SAST/SCA Critical 0건
- IaC 스캔 High 0건
- PR 리뷰 1명 이상(프로덕션 코드 2명)

## 베스트 프랙티스 체크리스트
- [ ] 장기 AWS 액세스 키 **0개** (OIDC만 사용)
- [ ] 빌드 재현성 (락파일 커밋, 버전 고정)
- [ ] 캐시 적극 활용 (Gradle, npm, Docker layer, Terraform plugin)
- [ ] 배포는 자동, 프로덕션 승격은 수동 승인
- [ ] 롤백 절차가 배포 절차와 동일한 수준으로 자동화
- [ ] 이미지/아티팩트 불변성 (한 번 빌드 → 모든 환경 재사용)

## 참고 자료 (공식 출처만)
- GitHub Actions Documentation: https://docs.github.com/en/actions
- Configuring OpenID Connect in AWS: https://docs.github.com/en/actions/deployment/security-hardening-your-deployments/configuring-openid-connect-in-amazon-web-services
- AWS CodePipeline User Guide: https://docs.aws.amazon.com/codepipeline/latest/userguide/
- AWS CodeBuild / CodeDeploy Docs: https://docs.aws.amazon.com/codebuild/, https://docs.aws.amazon.com/codedeploy/
- Amazon ECR 사용자 가이드: https://docs.aws.amazon.com/AmazonECR/latest/userguide/
- AWS Signer Developer Guide: https://docs.aws.amazon.com/signer/latest/developerguide/
- Terraform Recommended Practices: https://developer.hashicorp.com/terraform/cloud-docs/recommended-practices
- Gradle Build Cache: https://docs.gradle.org/current/userguide/build_cache.html
- Next.js Deployment: https://nextjs.org/docs/app/building-your-application/deploying
- DORA State of DevOps: https://dora.dev/research/

## 제약
- 워크플로 예제는 실제 디렉토리 구조(`tech-n-ai-backend/`, `tech-n-ai-frontend/app`, `tech-n-ai-frontend/admin`)의 path filter를 정확히 반영
- 모든 시크릿은 GitHub Environments + OIDC role로 처리 (리포지토리 시크릿 최소화)
