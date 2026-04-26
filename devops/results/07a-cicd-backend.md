# 07a. CI/CD 파이프라인 — 백엔드

> **상위 문서**: [07-cicd-overview.md](07-cicd-overview.md) (공통 정책·품질 게이트)
> **대상 모듈**: `api-gateway(8081)`, `api-emerging-tech(8082)`, `api-auth(8083)`, `api-chatbot(8084)`, `api-bookmark(8085)`, `api-agent(8086)`, `batch-source`
> **빌드 도구**: Gradle 9.x + JDK 21 (Temurin), 이미지는 Paketo `bootBuildImage` (Dockerfile 미사용)

---

## 1. Gradle 빌드 & 테스트

### 1.1 병렬 빌드

```bash
./gradlew build --parallel --build-cache --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx4g -XX:+HeapDumpOnOutOfMemoryError"
```

루트 `gradle.properties`:
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.jvmargs=-Xmx4g
```

GitHub Actions에서는 `actions/cache@v4`로 `~/.gradle/caches`, `~/.gradle/wrapper`, `.gradle/`을 캐싱한다.

공식 문서: [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)

### 1.2 테스트 분리

| 테스트 유형 | 태그 | 실행 타이밍 | 도구 |
|---|---|---|---|
| Unit | `@Tag("unit")` | PR 필수 | JUnit 5 + Mockito |
| Integration | `@Tag("integration")` | PR 필수 (병렬 제한) | Testcontainers (MySQL/Redis/Mongo/Kafka) |
| Contract | `@Tag("contract")` | develop merge 후 | Spring Cloud Contract |

```groovy
test {
    useJUnitPlatform { includeTags 'unit' }
    finalizedBy jacocoTestReport
}
tasks.register('integrationTest', Test) {
    useJUnitPlatform { includeTags 'integration' }
    shouldRunAfter test
    maxParallelForks = 2
}
```

### 1.3 커버리지 게이트 (Jacoco ≥ 70%)

```groovy
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit { counter = 'LINE'; minimum = 0.70 }
            limit { counter = 'BRANCH'; minimum = 0.60 }
        }
        rule {
            element = 'CLASS'
            excludes = ['*.dto.*', '*.entity.*', '*.config.*', '*Application']
        }
    }
}
check.dependsOn jacocoTestCoverageVerification
```

---

## 2. 정적 분석

| 도구 | 대상 | 게이트 |
|---|---|---|
| **Checkstyle** | 코딩 컨벤션 | 0 error |
| **SpotBugs** | 버그 패턴 (null deref, resource leak) | High 0건 |
| **SonarCloud** (선택) | 품질/보안/중복도 | Quality Gate "Sonar way" 통과 |

루트 `build.gradle`:
```groovy
subprojects {
    apply plugin: 'checkstyle'
    apply plugin: 'com.github.spotbugs'
    checkstyle {
        toolVersion = '10.17.0'
        configFile = rootProject.file('config/checkstyle/checkstyle.xml')
    }
    spotbugs {
        toolVersion = '4.8.6'
        effort = 'max'
        reportLevel = 'high'
    }
}
```

---

## 3. 보안 스캔

| 계층 | 도구 | 트리거 | 게이트 |
|---|---|---|---|
| **SCA (의존성)** | GitHub Dependabot + `gradle-dependency-check` | 일일/PR | Critical CVE 0 |
| **SAST (코드)** | GitHub CodeQL (Advanced Security) | PR + 주간 | Critical 0 |
| **컨테이너 이미지** | ECR Enhanced Scanning (Inspector) + Trivy | 푸시 직후 + PR | Critical 0, High ≤ 5 |

ECR Enhanced Scanning은 Inspector 기반으로 언어 패키지 + OS 패키지 CVE를 자동 분석한다.
- 공식: [Amazon ECR Image Scanning](https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning.html)
- CodeQL 쿼리 팩: `security-and-quality`.

---

## 4. 이미지 빌드 & 푸시 — Paketo `bootBuildImage` (단일 ECR 리포)

### 4.1 단일 리포 전략

이전 버전은 환경별 리포 3개(`dev/<module>`, `beta/<module>`, `prod/<module>`)를 사용했으나, **Notation 서명의 OCI referrer가 원본 리포에 종속**되는 구조적 제약 때문에 환경 간 promote 시 SIGNATURE_NOT_FOUND가 발생했다. 본 설계는 **단일 리포 + 환경 무관 태그**로 전환한다.

| 항목 | 변경 전 | **변경 후** |
|---|---|---|
| ECR 리포 | `dev/api-auth`, `beta/api-auth`, `prod/api-auth` | **`techai/api-auth`** (단일) |
| 이미지 태그 | `dev/api-auth:1.4.2-7a9f3c1` | **`techai/api-auth:1.4.2-7a9f3c1`** |
| Notation 서명 | 환경별 재서명 필요 | **빌드 시 1회**, 모든 환경에서 그대로 검증 |
| Promote | manifest 복사 + 재서명 | **태그 추가만**(or 단순 digest 참조) |
| 환경 분기 | 리포 분기 | ECS Task Definition 환경변수·Secrets ARN |

ECR Repository Policy로 `latest` 태그 푸시는 거부한다(태그 불변성 보장). Image Tag Mutability는 `IMMUTABLE`.

### 4.2 빌드 명령

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin "$ECR_URI"

# 단일 리포 + 환경 무관 태그
./gradlew :api-auth:bootBuildImage \
    -PimageTag="${SEMVER}-${GIT_SHA}" \
    -PbuildImage.publish=true
```

`gradle.properties` (Buildpack 버전 고정 — 재현성):
```properties
paketoBuilderImage=paketobuildpacks/builder-jammy-base:0.3.380
paketoRunImage=paketobuildpacks/run-jammy-base:0.2.104
bpJvmVersion=21
```

모듈별 `build.gradle`:
```groovy
bootBuildImage {
    builder  = project.property('paketoBuilderImage')
    runImage = project.property('paketoRunImage')
    // 단일 리포: techai/<module>:<imageTag>
    imageName = "${System.getenv('ECR_URI') ?: 'local'}/techai/${project.name}:${project.findProperty('imageTag') ?: System.getenv('GIT_SHA') ?: 'local'}"
    environment = [
        'BP_JVM_VERSION'  : project.property('bpJvmVersion'),
        'BP_NATIVE_IMAGE' : 'false'
        // SPRING_PROFILES_ACTIVE 는 이미지에 박지 않는다.
        // 환경별 프로파일은 ECS Task Definition 환경변수로 주입한다.
    ]
    publish = project.hasProperty('buildImage.publish')
    docker {
        publishRegistry {
            username = System.getenv('AWS_ECR_USER') ?: 'AWS'
            password = System.getenv('AWS_ECR_PASSWORD')
            url      = System.getenv('ECR_URI')
        }
    }
}
```

> **변경점**: `BPE_SPRING_PROFILES_ACTIVE`를 빌드 시 환경에 박는 방식을 제거했다. 이미지가 환경 독립이어야 단일 빌드·다중 배포가 성립한다. Spring 프로파일은 ECS Task Definition `environment[].name=SPRING_PROFILES_ACTIVE`로 주입한다 (03 §2.5).

---

## 5. 이미지 서명 — Notation + AWS Signer Plugin

> "AWS Signer for Container Images"는 단독 기능명이 아니며, AWS Signer는 **Notation의 플러그인**(`notation-aws-signer-plugin`)을 통해 ECR 이미지 서명을 지원한다.

```bash
# Notation 설치 + AWS Signer Plugin 등록
notation plugin install --file notation-aws-signer-plugin.tar.gz
notation key add --plugin com.amazonaws.signer.notation.plugin \
    --id "arn:aws:signer:ap-northeast-2:${AWS_ACCOUNT_ID}:/signing-profiles/tech_n_ai_container" \
    --default aws-signer

# 서명 (단일 리포에 1회만)
notation sign "$ECR_URI/techai/api-auth@${DIGEST}"

# 검증 (모든 환경 배포 단계에서 동일하게 동작)
notation verify "$ECR_URI/techai/api-auth@${DIGEST}" \
    --scope "$ECR_URI/techai/api-auth"
```

- 서명 결과는 OCI 1.1 referrer로 같은 리포에 저장된다 → **promote 시 referrer 손실 없음**.
- 공식: [AWS Signer Developer Guide — Container images](https://docs.aws.amazon.com/signer/latest/developerguide/sig-container-workflow.html)

`signing-profiles/tech_n_ai_container`의 IAM 권한·Trust Policy는 [06 §3 Secrets/Signer]에서 단일 정의된다.

---

## 6. 배포 — ECS update-service + CodeDeploy Blue/Green (Hook 람다 미사용)

### 6.1 안전망 구성

CodeDeploy Hook 람다(`BeforeAllowTraffic`/`AfterAllowTraffic`)는 사용하지 않는다. 동등한 안전망을 다음 3중으로 구성한다:

| 계층 | 메커니즘 | 검증 대상 |
|---|---|---|
| 태스크 헬스 | ALB Target Group health check `/actuator/health/readiness` (HTTP 200) | 의존성(DB·MQ·Cache) 포함 readiness |
| 트래픽 전환 중 | CodeDeploy `CodeDeployDefault.ECSCanary10Percent5Minutes` + CloudWatch 알람 | 5xx rate, p95 latency |
| 자동 롤백 | CodeDeploy auto-rollback on alarm | 알람 발생 시 즉시 이전 revision로 복귀 |
| 사후 검증 | 워크플로 step의 `curl /actuator/health` | 배포 직후 30초 이내 |

> **이유**: "최소 세팅" 정책(Q3) — Hook 람다는 추가 비용은 없으나 코드·권한·로깅·VPC 배치 등 운영 표면을 늘린다. 신규 구축에서 ALB health + CW 알람만으로 동등한 보호가 가능하므로 우선 미도입.

### 6.2 appspec.yaml — Hooks 섹션 없음

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: "arn:aws:ecs:ap-northeast-2:${ACCOUNT}:task-definition/${ENV}-api-auth:${REVISION}"
        LoadBalancerInfo:
          ContainerName: "api-auth"
          ContainerPort: 8083
        PlatformVersion: "LATEST"
# Hooks 미정의 — CloudWatch 알람 기반 자동 롤백만 사용
```

### 6.3 배포 명령

```bash
aws ecs register-task-definition \
    --cli-input-json "file://infra/ecs/taskdef-${ENV}-api-auth.json"

aws deploy create-deployment \
    --application-name "${ENV}-api-auth" \
    --deployment-group-name "${ENV}-api-auth-bg" \
    --deployment-config-name CodeDeployDefault.ECSCanary10Percent5Minutes \
    --revision "revisionType=AppSpecContent,appSpecContent={content='$(cat appspec.yaml | jq -Rs .)'}"
```

CodeDeploy DG의 `auto_rollback_configuration.events = [DEPLOYMENT_FAILURE, DEPLOYMENT_STOP_ON_ALARM]`, `alarm_configuration`에 5xx·latency 알람 ARN을 주입한다(09 IaC 모듈 `ecs-service`에서 정의).

공식: [AWS CodeDeploy ECS Deployments](https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-steps-ecs.html)

---

## 7. GitHub Actions 워크플로

### 7.1 `.github/workflows/backend-ci.yml`

```yaml
name: backend-ci

on:
  pull_request:
    branches: [main, develop]
    paths:
      - 'tech-n-ai-backend/**'
      - '.github/workflows/backend-ci.yml'

concurrency:
  group: backend-ci-${{ github.ref }}
  cancel-in-progress: true

permissions:
  id-token: write
  contents: read
  pull-requests: write
  checks: write
  security-events: write

defaults:
  run:
    working-directory: tech-n-ai-backend

jobs:
  build-test:
    name: Build & Test (${{ matrix.module }})
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        module:
          - api-gateway
          - api-emerging-tech
          - api-auth
          - api-chatbot
          - api-bookmark
          - api-agent
          - batch-source
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/develop' && github.ref != 'refs/heads/main' }}

      - name: Build & Unit Test
        run: |
          ./gradlew :${{ matrix.module }}:build :${{ matrix.module }}:jacocoTestCoverageVerification \
            --parallel --build-cache --no-daemon

      - name: Integration Test
        run: ./gradlew :${{ matrix.module }}:integrationTest --parallel --no-daemon

      - name: Upload JUnit Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: junit-${{ matrix.module }}
          path: tech-n-ai-backend/${{ matrix.module }}/build/test-results/**/*.xml

      - name: Upload Jacoco Report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jacoco-${{ matrix.module }}
          path: tech-n-ai-backend/${{ matrix.module }}/build/reports/jacoco/

  static-analysis:
    name: Static Analysis
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew checkstyleMain spotbugsMain --parallel --no-daemon
      - name: Upload SpotBugs SARIF
        if: always()
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: tech-n-ai-backend/build/reports/spotbugs/

  codeql:
    name: CodeQL (SAST)
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - uses: github/codeql-action/init@v3
        with:
          languages: java-kotlin
          queries: security-and-quality
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew build -x test --parallel --no-daemon
      - uses: github/codeql-action/analyze@v3
        with:
          category: /language:java-kotlin
```

### 7.2 `.github/workflows/backend-cd.yml` (단일 ECR 리포 + Hook 미사용 반영)

```yaml
name: backend-cd

on:
  push:
    branches: [main, develop]
    paths:
      - 'tech-n-ai-backend/**'
      - '.github/workflows/backend-cd.yml'

concurrency:
  group: backend-cd-${{ github.ref }}
  cancel-in-progress: false

permissions:
  id-token: write
  contents: read

env:
  AWS_REGION: ap-northeast-2
  ECR_REGISTRY: ${{ vars.ECR_REGISTRY }}        # 예: 1234.dkr.ecr.ap-northeast-2.amazonaws.com
  SIGNER_PROFILE_ARN: ${{ vars.SIGNER_PROFILE_ARN }}

jobs:
  build-sign-push:
    name: Build / Sign / Push (${{ matrix.module }})
    runs-on: ubuntu-latest
    strategy:
      fail-fast: true
      matrix:
        module: [api-gateway, api-emerging-tech, api-auth, api-chatbot, api-bookmark, api-agent, batch-source]
    outputs:
      image-digest-${{ matrix.module }}: ${{ steps.digest.outputs.digest }}
    steps:
      - uses: actions/checkout@v4

      - name: Configure AWS via OIDC
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: ECR password
        id: ecr-pwd
        run: |
          PASSWORD=$(aws ecr get-login-password --region "$AWS_REGION")
          echo "::add-mask::${PASSWORD}"
          echo "password=${PASSWORD}" >> "$GITHUB_OUTPUT"

      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - uses: gradle/actions/setup-gradle@v4

      - name: Compute tag
        id: tag
        run: |
          SHORT_SHA=$(git rev-parse --short=7 HEAD)
          # 단일 태그(env 무관). 시드 단계에서는 SemVer 미부여 시 SHA만 사용 가능.
          SEMVER=$(git describe --tags --abbrev=0 2>/dev/null || echo "0.1.0")
          TAG="${SEMVER#v}-${SHORT_SHA}"
          echo "short_sha=${SHORT_SHA}" >> "$GITHUB_OUTPUT"
          echo "tag=${TAG}" >> "$GITHUB_OUTPUT"

      - name: bootBuildImage & Push to ECR (single repo)
        env:
          ECR_URI: ${{ env.ECR_REGISTRY }}
          AWS_ECR_USER: AWS
          AWS_ECR_PASSWORD: ${{ steps.ecr-pwd.outputs.password }}
        working-directory: tech-n-ai-backend
        run: |
          ./gradlew :${{ matrix.module }}:bootBuildImage \
            -PimageTag="${{ steps.tag.outputs.tag }}" \
            -PbuildImage.publish=true \
            --no-daemon

      - name: Capture image digest
        id: digest
        run: |
          DIGEST=$(aws ecr describe-images \
            --repository-name "techai/${{ matrix.module }}" \
            --image-ids imageTag=${{ steps.tag.outputs.tag }} \
            --query 'imageDetails[0].imageDigest' --output text)
          echo "digest=${DIGEST}" >> "$GITHUB_OUTPUT"

      - name: Install Notation + AWS Signer plugin
        run: |
          curl -Lo notation.tar.gz https://github.com/notaryproject/notation/releases/download/v1.2.0/notation_1.2.0_linux_amd64.tar.gz
          tar xf notation.tar.gz && sudo mv notation /usr/local/bin/
          curl -Lo plugin.zip https://d2hvyiie56hcat.cloudfront.net/linux/amd64/plugin/latest/notation-aws-signer-plugin.zip
          notation plugin install --file plugin.zip

      - name: Sign image (once, single repo)
        run: |
          notation key add --plugin com.amazonaws.signer.notation.plugin \
            --id "${SIGNER_PROFILE_ARN}" --default aws-signer
          notation sign "${ECR_REGISTRY}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}"

  trivy-scan:
    needs: build-sign-push
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [api-gateway, api-emerging-tech, api-auth, api-chatbot, api-bookmark, api-agent, batch-source]
    steps:
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}
      - name: Resolve digest
        id: digest
        run: |
          DIGEST=$(aws ecr describe-images \
            --repository-name "techai/${{ matrix.module }}" \
            --query 'sort_by(imageDetails,&imagePushedAt)[-1].imageDigest' --output text)
          echo "digest=${DIGEST}" >> "$GITHUB_OUTPUT"
      - uses: aquasecurity/trivy-action@0.24.0
        with:
          image-ref: ${{ env.ECR_REGISTRY }}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}
          format: sarif
          output: trivy-${{ matrix.module }}.sarif
          severity: CRITICAL,HIGH
          exit-code: '1'
          ignore-unfixed: true
      - uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: trivy-${{ matrix.module }}.sarif

  deploy-dev:
    name: Deploy to dev (CodeDeploy Blue/Green)
    needs: [build-sign-push, trivy-scan]
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment:
      name: dev
      url: https://dev.tech-n-ai.example.com
    strategy:
      matrix:
        module: [api-gateway, api-emerging-tech, api-auth, api-chatbot, api-bookmark, api-agent]
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Resolve digest
        id: digest
        run: |
          SHORT_SHA=$(git rev-parse --short=7 HEAD)
          SEMVER=$(git describe --tags --abbrev=0 2>/dev/null || echo "0.1.0")
          TAG="${SEMVER#v}-${SHORT_SHA}"
          DIGEST=$(aws ecr describe-images \
            --repository-name "techai/${{ matrix.module }}" \
            --image-ids imageTag=${TAG} \
            --query 'imageDetails[0].imageDigest' --output text)
          echo "digest=${DIGEST}" >> "$GITHUB_OUTPUT"

      - name: Verify signature
        run: |
          notation verify "${ECR_REGISTRY}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}" \
            --scope "${ECR_REGISTRY}/techai/${{ matrix.module }}"

      - name: Render task definition
        id: render
        uses: aws-actions/amazon-ecs-render-task-definition@v1
        with:
          task-definition: infra/ecs/taskdef-dev-${{ matrix.module }}.json
          container-name: ${{ matrix.module }}
          image: ${{ env.ECR_REGISTRY }}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}

      - name: Deploy via CodeDeploy
        uses: aws-actions/amazon-ecs-deploy-task-definition@v2
        with:
          task-definition: ${{ steps.render.outputs.task-definition }}
          service: dev-${{ matrix.module }}
          cluster: dev-tech-n-ai
          codedeploy-appspec: infra/ecs/appspec-dev.yaml
          codedeploy-application: dev-${{ matrix.module }}
          codedeploy-deployment-group: dev-${{ matrix.module }}-bg
          wait-for-service-stability: true

      - name: Smoke test
        run: |
          curl -fsSL --retry 10 --retry-delay 5 \
            https://dev.tech-n-ai.example.com/actuator/health | jq -e '.status=="UP"'

  deploy-prod:
    name: Deploy to prod (manual approval)
    needs: build-sign-push
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: prod
      url: https://tech-n-ai.example.com
    strategy:
      matrix:
        module: [api-gateway, api-emerging-tech, api-auth, api-chatbot, api-bookmark, api-agent]
    steps:
      - uses: actions/checkout@v4
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_DEPLOY_ROLE_ARN }}
          aws-region: ${{ env.AWS_REGION }}

      - name: Resolve digest
        id: digest
        run: |
          SHORT_SHA=$(git rev-parse --short=7 HEAD)
          SEMVER=$(git describe --tags --abbrev=0 2>/dev/null || echo "0.1.0")
          TAG="${SEMVER#v}-${SHORT_SHA}"
          DIGEST=$(aws ecr describe-images \
            --repository-name "techai/${{ matrix.module }}" \
            --image-ids imageTag=${TAG} \
            --query 'imageDetails[0].imageDigest' --output text)
          echo "digest=${DIGEST}" >> "$GITHUB_OUTPUT"

      - name: Verify signature (single repo — referrer 일치)
        run: |
          notation verify "${ECR_REGISTRY}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}" \
            --scope "${ECR_REGISTRY}/techai/${{ matrix.module }}"

      - uses: aws-actions/amazon-ecs-render-task-definition@v1
        id: render
        with:
          task-definition: infra/ecs/taskdef-prod-${{ matrix.module }}.json
          container-name: ${{ matrix.module }}
          image: ${{ env.ECR_REGISTRY }}/techai/${{ matrix.module }}@${{ steps.digest.outputs.digest }}

      - uses: aws-actions/amazon-ecs-deploy-task-definition@v2
        with:
          task-definition: ${{ steps.render.outputs.task-definition }}
          service: prod-${{ matrix.module }}
          cluster: prod-tech-n-ai
          codedeploy-appspec: infra/ecs/appspec-prod.yaml
          codedeploy-application: prod-${{ matrix.module }}
          codedeploy-deployment-group: prod-${{ matrix.module }}-bg
          wait-for-service-stability: true
```

> **변경 요약**
> 1. `dev/<module>` → `techai/<module>` 단일 리포로 통일.
> 2. 환경별 manifest 복사·재서명 잡(`promote-beta`)을 삭제 — 단일 리포에서 자연스럽게 동일 digest 참조.
> 3. ECR 로그인 비밀번호를 `aws ecr get-login-password`로 직접 획득(계정 ID 하드코딩 제거).
> 4. Spring 프로파일을 이미지에서 분리 — Task Definition 환경변수로 주입.
> 5. CodeDeploy Hook 람다 미사용. ALB readiness + CloudWatch 알람 자동 롤백 + Smoke Test로 안전망 구성.

---

## 8. 관련 문서

- [07-cicd-overview.md](07-cicd-overview.md) — 공통 정책·품질 게이트
- [03 §2 백엔드 컴퓨팅](03-compute-and-frontend-hosting.md) — Task Definition 설계
- [06 §2.4 GitHub OIDC](06-security-and-iam.md) — `AWS_DEPLOY_ROLE_ARN` 신뢰 정책 단일 정의
- [09 §모듈 ecs-service](09-iac-terraform.md) — CodeDeploy DG·알람 IaC 정의

---

**문서 끝.**
