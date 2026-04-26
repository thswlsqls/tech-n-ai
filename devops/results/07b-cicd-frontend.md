# 07b. CI/CD 파이프라인 — 프론트엔드

> **상위 문서**: [07-cicd-overview.md](07-cicd-overview.md)
> **대상 앱**: `tech-n-ai-frontend/app` (port 3000), `tech-n-ai-frontend/admin` (port 3001)
> **빌드 환경**: Next.js 16.1.6, Node LTS 20.x, npm

---

## 1. Next.js 빌드

```bash
# Node 버전 고정 (package.json engines + .nvmrc)
node --version   # v20.x.x LTS

npm ci --prefer-offline --no-audit --fund=false
npm run lint
npm run build   # Next.js 16 Turbopack production build
npm test        # (선택) Vitest
npx playwright test --project=chromium  # (선택) E2E
```

`package.json` engines:
```json
{
  "engines": { "node": ">=20.0.0 <21.0.0", "npm": ">=10.0.0" }
}
```

---

## 2. 캐시 전략

```yaml
- uses: actions/setup-node@v4
  with:
    node-version-file: 'tech-n-ai-frontend/app/.nvmrc'
    cache: 'npm'
    cache-dependency-path: 'tech-n-ai-frontend/app/package-lock.json'

- uses: actions/cache@v4
  with:
    path: |
      tech-n-ai-frontend/app/.next/cache
    key: ${{ runner.os }}-nextjs-${{ hashFiles('tech-n-ai-frontend/app/package-lock.json') }}-${{ hashFiles('tech-n-ai-frontend/app/**/*.[jt]s', 'tech-n-ai-frontend/app/**/*.[jt]sx') }}
    restore-keys: |
      ${{ runner.os }}-nextjs-${{ hashFiles('tech-n-ai-frontend/app/package-lock.json') }}-
```

공식 문서: [Next.js Deployment — Caching](https://nextjs.org/docs/app/building-your-application/deploying#caching)

---

## 3. 번들 최적화 & 검증

- **Bundle Size**: `@next/bundle-analyzer`로 client/edge/server 3개 리포트를 `actions/upload-artifact@v4`로 PR에 첨부.
- **Lighthouse CI**: PR당 4회 실행 평균. 예산:
  - Performance ≥ 90, Accessibility ≥ 95, Best Practices ≥ 95, SEO ≥ 90
  - FCP ≤ 1.8s, LCP ≤ 2.5s, TBT ≤ 200ms, CLS ≤ 0.1
- 실패 시 PR 코멘트에 `treemap` 링크 자동 생성.

---

## 4. 배포 — Amplify Hosting

- `main` 브랜치 → `app-prod`, `admin-prod` Amplify 앱의 prod 환경
- `develop` 브랜치 → `app-beta`, `admin-beta`
- Amplify의 **자동 빌드는 비활성화**하고 GitHub Actions에서 `aws amplify start-job`로 **수동 트리거**한다(동일 아티팩트 승격을 위해).
- Amplify 앱 환경변수는 Amplify Console이 아닌 **AWS SSM Parameter Store**에서 pre-build phase에 주입한다.
- Atomic deployment + 즉시 롤백 지원(이전 deployment id로 재배포).

---

## 5. GitHub Actions 워크플로 — `.github/workflows/frontend-ci-cd.yml`

```yaml
name: frontend-ci-cd

on:
  push:
    branches: [main, develop]
    paths:
      - 'tech-n-ai-frontend/app/**'
      - 'tech-n-ai-frontend/admin/**'
      - '.github/workflows/frontend-ci-cd.yml'
  pull_request:
    branches: [main, develop]
    paths:
      - 'tech-n-ai-frontend/app/**'
      - 'tech-n-ai-frontend/admin/**'

concurrency:
  group: frontend-${{ github.ref }}-${{ github.workflow }}
  cancel-in-progress: true

permissions:
  id-token: write
  contents: read
  pull-requests: write

env:
  AWS_REGION: ap-northeast-2

jobs:
  detect-changes:
    runs-on: ubuntu-latest
    outputs:
      app: ${{ steps.filter.outputs.app }}
      admin: ${{ steps.filter.outputs.admin }}
    steps:
      - uses: actions/checkout@v4
      - id: filter
        uses: dorny/paths-filter@v3
        with:
          filters: |
            app:
              - 'tech-n-ai-frontend/app/**'
            admin:
              - 'tech-n-ai-frontend/admin/**'

  ci:
    name: CI (${{ matrix.workspace }})
    needs: detect-changes
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        include:
          - workspace: app
            changed: ${{ needs.detect-changes.outputs.app }}
          - workspace: admin
            changed: ${{ needs.detect-changes.outputs.admin }}
    if: needs.detect-changes.outputs.app == 'true' || needs.detect-changes.outputs.admin == 'true'
    defaults:
      run:
        working-directory: tech-n-ai-frontend/${{ matrix.workspace }}
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version-file: tech-n-ai-frontend/${{ matrix.workspace }}/.nvmrc
          cache: npm
          cache-dependency-path: tech-n-ai-frontend/${{ matrix.workspace }}/package-lock.json

      - name: Install deps
        if: matrix.changed == 'true'
        run: npm ci --prefer-offline --no-audit --fund=false

      - name: Cache Next.js build
        if: matrix.changed == 'true'
        uses: actions/cache@v4
        with:
          path: tech-n-ai-frontend/${{ matrix.workspace }}/.next/cache
          key: ${{ runner.os }}-next-${{ matrix.workspace }}-${{ hashFiles(format('tech-n-ai-frontend/{0}/package-lock.json', matrix.workspace)) }}-${{ hashFiles(format('tech-n-ai-frontend/{0}/**/*.[jt]s', matrix.workspace), format('tech-n-ai-frontend/{0}/**/*.[jt]sx', matrix.workspace)) }}
          restore-keys: |
            ${{ runner.os }}-next-${{ matrix.workspace }}-${{ hashFiles(format('tech-n-ai-frontend/{0}/package-lock.json', matrix.workspace)) }}-

      - name: Lint
        if: matrix.changed == 'true'
        run: npm run lint

      - name: Build
        if: matrix.changed == 'true'
        run: npm run build

      - name: Bundle analyzer
        if: matrix.changed == 'true' && github.event_name == 'pull_request'
        run: ANALYZE=true npm run build

      - name: Upload bundle report
        if: matrix.changed == 'true' && github.event_name == 'pull_request'
        uses: actions/upload-artifact@v4
        with:
          name: bundle-${{ matrix.workspace }}
          path: tech-n-ai-frontend/${{ matrix.workspace }}/.next/analyze/

      - name: Lighthouse CI
        if: matrix.changed == 'true' && github.event_name == 'pull_request'
        uses: treosh/lighthouse-ci-action@v12
        with:
          configPath: tech-n-ai-frontend/${{ matrix.workspace }}/lighthouserc.json
          uploadArtifacts: true
          temporaryPublicStorage: true

  deploy:
    name: Deploy (${{ matrix.workspace }} → ${{ matrix.env }})
    needs: [detect-changes, ci]
    if: github.event_name == 'push'
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        workspace: [app, admin]
        include:
          - env: ${{ github.ref == 'refs/heads/main' && 'prod' || 'beta' }}
    environment:
      name: ${{ matrix.env }}-frontend-${{ matrix.workspace }}
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Trigger Amplify deployment
        run: |
          APP_ID=$(aws ssm get-parameter \
            --name "/tech-n-ai/${{ matrix.env }}/amplify/${{ matrix.workspace }}/app-id" \
            --query 'Parameter.Value' --output text)
          BRANCH="${{ matrix.env == 'prod' && 'main' || 'develop' }}"
          JOB_ID=$(aws amplify start-job \
            --app-id "${APP_ID}" \
            --branch-name "${BRANCH}" \
            --job-type RELEASE \
            --commit-id "${GITHUB_SHA}" \
            --query 'jobSummary.jobId' --output text)
          echo "Amplify jobId=${JOB_ID}"

          # SUCCEED/FAILED 까지 폴링
          while true; do
            STATUS=$(aws amplify get-job --app-id "${APP_ID}" --branch-name "${BRANCH}" --job-id "${JOB_ID}" --query 'job.summary.status' --output text)
            echo "status=${STATUS}"
            case "${STATUS}" in
              SUCCEED) exit 0 ;;
              FAILED|CANCELLED) exit 1 ;;
              *) sleep 15 ;;
            esac
          done
```

---

## 6. 관련 문서

- [07-cicd-overview.md](07-cicd-overview.md) — 공통 정책
- [03 §3 프론트엔드 호스팅](03-compute-and-frontend-hosting.md) — Amplify Hosting 설계, 환경변수 주입 전략
- [06 §3 시크릿 관리](06-security-and-iam.md) — SSM Parameter Store, Secrets Manager 정책

---

**문서 끝.**
