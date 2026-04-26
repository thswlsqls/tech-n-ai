# 03. 컴퓨팅 · 프론트엔드 호스팅 설계

> 작성자: AWS DevOps 엔지니어
> 대상 프로젝트: `tech-n-ai` (Spring Boot 4 멀티 모듈 백엔드 + Next.js 16 프론트엔드 `app` / `admin`)
> 범위: 백엔드 컴퓨팅 오케스트레이션, 컨테이너 이미지 파이프라인, Next.js 호스팅, CDN·엣지 설계

---

## 0. 문서 요약 및 전제 사실

### 0.1 프로젝트 전제 (현 상태 스냅샷 — 반드시 설계에 반영)

| 항목 | 현재 상태 | 설계 반영 포인트 |
|---|---|---|
| 백엔드 이미지 빌드 | Spring Boot `bootBuildImage` (Paketo Buildpacks), **Dockerfile 없음** | 유지(§1.2). Buildpack 기반 OCI 이미지를 ECR로 푸시 |
| 백엔드 런타임 | Spring Boot 4.0.2, Java 21 | Amazon Corretto 21 buildpack 강제 지정 (`BP_JVM_VERSION=21`) |
| 백엔드 모듈 | api-gateway(8081), api-emerging-tech(8082), api-auth(8083), api-chatbot(8084), api-bookmark(8085), api-agent(8086), `batch-source` | 모듈별 ECS Service + 모듈별 단일 ECR 리포(`techai/{module}`, 환경 무관 — D-1), 배치는 EventBridge + ECS RunTask |
| Actuator probe | **미설정** (`management.endpoint.health.probes.*` 없음) | **배포 전 필수 조치 경고** — §1.7 |
| 프론트 프레임워크 | Next.js 16.1.6, app(3000) / admin(3001) | 기본 SSR로 두고 Amplify Hosting 우선 |
| `next.config.ts` `output` | **미설정** (기본 SSR) | Amplify 선택 시 그대로, ECS/Lambda 선택 시 `output: 'standalone'` 추가 필요 |
| `.env` 파일 | **없음** | `NEXT_PUBLIC_API_BASE_URL` 도입 + 환경별 주입 §2.2 |
| `next.config.ts` rewrite destination | `http://localhost:8081` 하드코딩 | `process.env.API_GATEWAY_URL`로 치환 |
| `next/image`, `images.remotePatterns` | **미사용/미설정** | CloudFront 원본 캐싱 전략으로 충분, 향후 도입 시 remotePatterns 필수 |
| 미들웨어 | 쿠키 `accessToken` → `Authorization: Bearer` 헤더 주입 (리다이렉트 X) | 호스팅이 Node.js 런타임(SSR/미들웨어) 지원해야 함 → Amplify / ECS 모두 가능, S3 정적 호스팅 **불가** |

### 0.2 설계 결정 요약

| 레이어 | 선정 |
|---|---|
| 백엔드 오케스트레이터 | **Amazon ECS on AWS Fargate** |
| 컨테이너 이미지 빌드 | **Paketo Buildpacks `bootBuildImage` 유지** (Dockerfile 전환 불필요) |
| 이미지 레지스트리 | ECR Private + Enhanced Scanning(Inspector) + Notation 서명 |
| API 엣지 | ALB → Spring Cloud Gateway(`api-gateway`) (AWS API Gateway 중복 제거) |
| 프론트 호스팅 | **AWS Amplify Hosting**(app), **CloudFront + Amplify (비공개 배포)**(admin) |
| 엣지 | CloudFront(정적·이미지), Amplify 기본 CDN, WAF는 §06에서 정의 |

---

## 1. 백엔드 컴퓨팅 전략 (backend-compute-design)

### 1.1 오케스트레이터 선정 — ECS Fargate vs EKS vs App Runner

| 항목 | ECS Fargate | EKS (Fargate/EC2) | App Runner |
|---|---|---|---|
| 관리 오버헤드 | 낮음 (AWS 관리 컨트롤 플레인·데이터 플레인) | 높음 (K8s 업그레이드·애드온·CNI 관리) | 매우 낮음 (소스/이미지 URL만 제공) |
| 멀티 마이크로서비스 | 우수 (Service 단위 분리, Service Connect, ALB 타깃 그룹) | 우수 (가장 풍부한 생태계) | 단일 서비스 지향. 서비스 간 내부 통신·서비스 메시 부족 |
| 배치 워크로드 | ECS RunTask + EventBridge Scheduler, AWS Batch 통합 | CronJob/Argo Workflows | 상시 HTTP 서비스 전제 — 배치 부적합 |
| 네트워크 정책 | Security Group + Service Connect | NetworkPolicy(Cilium/Calico) | 제한적(자동 VPC 커넥터) |
| Spring Boot 적합도 | 매우 우수 (graceful shutdown + `stopTimeout`, ALB deregistration delay) | 매우 우수 | 적합하나 JVM 워밍업·스케일 제로 시 콜드 스타트 부담 |
| 비용 | Fargate per-task 과금, 유휴 최소 | 컨트롤 플레인 $0.10/h + 데이터 플레인 | 요청 기반 + Idle $/h — 낮은 트래픽에 유리, 많은 서비스일 때 누적 |
| 배포 | CodeDeploy Blue/Green, Rolling | Argo Rollouts/Flagger, Helm | 자동 배포 (제어 한계) |
| 관측성 | Container Insights, FireLens | Container Insights, Prometheus | CloudWatch 기본만 |

**선정: Amazon ECS on AWS Fargate**

근거:
1. 마이크로서비스 6개 + 배치 1개 규모에서 EKS의 컨트롤 플레인 오버헤드·쿠버네티스 운영 비용이 과도하다. ECS는 "컨테이너 우선" 추상화를 제공해 팀 규모 대비 빠른 배포가 가능하다. (Amazon ECS Developer Guide — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html>)
2. App Runner는 서비스 간 통신·배치·세밀한 네트워킹·Blue/Green 제어가 약하다. 본 프로젝트는 Spring Cloud Gateway로 내부 서비스 라우팅을 수행하므로 VPC 내부 통신·Service Connect가 필수다.
3. Fargate는 EC2 관리 없이 Task 단위로 실행·과금되어 Spring Boot 6개 마이크로서비스 + 배치에 이상적이다. (AWS Fargate Security Best Practices — <https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/security-fargate.html>)
4. CodeDeploy Blue/Green을 ECS와 네이티브로 연동하여 롤백 가능한 배포를 구현할 수 있다. (<https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html>)

### 1.2 컨테이너 이미지 전략 — Buildpacks 유지

**결정: 현재의 `bootBuildImage`(Paketo Buildpacks) 방식을 유지한다.**

근거:
- Spring Boot 공식 문서는 Buildpacks를 Dockerfile과 동등한 1급 이미지 생성 경로로 지원한다. (<https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html>, <https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html>)
- 레이어드 JAR(Buildpack이 자동 적용)을 Dockerfile로 수동 관리하면 의존성 레이어 분리 로직을 유지해야 하며 오류 가능성이 커진다.
- Paketo Java Buildpack이 **Amazon Corretto** 배포 지원(`BP_JVM_VERSION`, `BP_JVM_TYPE`)을 제공하므로 Java 21 Corretto를 공식 경로로 적용할 수 있다. (<https://paketo.io/docs/howto/java/>)
- 이미지 SBOM·취약점 스캔은 ECR Enhanced Scanning(Amazon Inspector) 측에서 일원화된다.

#### 1.2.1 `build.gradle` 설정 지침 (각 `api-*`, `batch-source` 모듈)

```groovy
// api-auth/build.gradle (공통 ext로 빼도 동일)
bootBuildImage {
    // 빌더·런 이미지 태그 모두 버전 고정 — "latest" 금지 (재현성 확보)
    // ※ 실제 버전은 CI에서 주기적으로 검증 후 업데이트. 예시값이며 `docker pull` 후 digest pin 권장.
    builder = "paketobuildpacks/builder-jammy-tiny:0.0.320"
    runImage = "paketobuildpacks/run-jammy-tiny:0.2.104"

    // ECR 타깃 네이밍
    imageName = "${project.findProperty('registry') ?: 'local'}/${project.name}:${project.findProperty('imageTag') ?: 'dev'}"

    environment = [
        // Java 21 Amazon Corretto 강제 (Paketo Java BP)
        "BP_JVM_VERSION": "21",
        "BP_JVM_TYPE"   : "JRE",
        "BP_JVM_CDS_ENABLED": "true",        // Class Data Sharing로 기동 단축
        "BPE_APPEND_JAVA_TOOL_OPTIONS": "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError",
        "BP_HEALTH_CHECKER_ENABLED": "true"
    ]

    publish = project.hasProperty("publish")
    docker {
        publishRegistry {
            url      = System.getenv("ECR_REGISTRY") ?: ""
            username = "AWS"
            password = System.getenv("ECR_PASSWORD") ?: ""   // aws ecr get-login-password로 주입
        }
    }
}
```

빌드·푸시 명령 예:

```bash
# ECR 로그인 (CI/CD 또는 로컬)
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin 123456789012.dkr.ecr.ap-northeast-2.amazonaws.com

# 개별 모듈 빌드 & 푸시
./gradlew :api-auth:bootBuildImage \
  -Pregistry=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/prod \
  -PimageTag=$(git rev-parse --short HEAD) \
  -Ppublish \
  --imageName=123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/techai/api-auth:$(git rev-parse --short HEAD)
```

> 빌더/런 이미지 태그는 **필수로 버전 고정**. `latest`/`:tiny`만 쓰면 재현 불가 빌드가 된다. (Paketo Builders — <https://paketo.io/docs/concepts/builders/>)

#### 1.2.2 ECR 리포지토리 네이밍·태깅

> **결정 D-1 적용 (00-README §주요 정합성 결정)**: 환경 무관 **단일 리포** `techai/{module}` 사용. 이미지는 한 번 빌드 → 태그·digest로 환경 간 promote. 환경별 리포 분리 시 Notation 서명 referrer가 원본 리포에 종속되어 SIGNATURE_NOT_FOUND 발생 위험을 단일 리포로 자연 해소.

| 항목 | 값 |
|---|---|
| 리포지토리 | `techai/api-auth`, `techai/api-gateway`, `techai/api-emerging-tech`, `techai/api-chatbot`, `techai/api-bookmark`, `techai/api-agent`, `techai/batch-source` (모듈당 1개, 환경 무관) |
| 불변 태그 | `{semver}-{git-sha}` (예: `1.4.2-a1b2c3d`) — `imageTagMutability=IMMUTABLE` |
| 환경 식별 | 태그·digest 자체로 식별. 이동형 환경 태그(`dev`/`beta`/`prod`) 사용 금지(Immutable 정책과 충돌) |
| 환경별 promote | Task Definition의 `image` 필드를 동일 digest로 가리키도록 갱신 (`@sha256:...`) |

- 네이밍 규칙: `techai/{module}` (단일 리포)
- 태그 규칙: `{semver}-{git-sha}` 불변 태그
- Immutable tag 강제: ECR 리포지토리 `imageTagMutability=IMMUTABLE`. 환경 식별은 태그가 아닌 Task Definition의 image digest로 결정 (ECR User Guide — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-tag-mutability.html>)

**Lifecycle Policy (예: prod)**:

```json
{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 30 prod release tagged images",
      "selection": {
        "tagStatus": "tagged",
        "tagPrefixList": ["v"],
        "countType": "imageCountMoreThan",
        "countNumber": 30
      },
      "action": { "type": "expire" }
    },
    {
      "rulePriority": 2,
      "description": "Expire untagged > 7 days",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 7
      },
      "action": { "type": "expire" }
    }
  ]
}
```

(ECR Lifecycle Policy — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html>)

#### 1.2.3 스캔 & 서명

- **Basic Scanning** (푸시 시 1회, CVE 스냅샷) 활성화.
- **Enhanced Scanning** (Amazon Inspector 통합, 지속·레이어·런타임 스캔)을 prod/beta 리포지토리에 활성화. SBOM(SPDX)도 생성된다. (<https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning-enhanced.html>)
- **서명**: Notation + AWS Signer Notation Plugin 사용 권장. ECR과 공식 통합되며 배포 시 `admission/verify` 단계로 Kyverno-like 정책을 구현할 수 있다. (<https://docs.aws.amazon.com/signer/latest/developerguide/container-image-signing.html>) Cosign 대안 가능하나 AWS 네이티브 통합(KMS 기반 키 관리) 측면에서 Notation을 우선 채택한다.

### 1.3 서비스 / Task 정의

#### 1.3.1 모듈별 CPU/MEM 초안 (p95 가정치 기반)

RPS 가정: app 프론트 + 외부 연동 합산 평균 40 rps, 피크 200 rps. 챗봇/에이전트는 LLM RTT로 느린 커넥션 비중 높음.

| 모듈 | Fargate CPU | Memory | Desired | Min | Max | 근거 |
|---|---|---|---|---|---|---|
| api-gateway | 1024 (1 vCPU) | 2048 | 2 | 2 | 8 | Spring Cloud Gateway는 I/O-bound, 적은 CPU로 다수 커넥션 처리. 멀티 AZ 고가용 2대 최소. |
| api-auth | 512 | 1024 | 2 | 2 | 6 | JWT 검증·리프레시 토큰, 짧은 TPS. 경량. |
| api-emerging-tech | 1024 | 2048 | 2 | 2 | 6 | 외부 API 호출 + 캐시. 중간 부하. |
| api-chatbot | 2048 (2 vCPU) | 4096 | 2 | 2 | 10 | langchain4j + MongoDB Vector Search, LLM 동시 스트리밍 커넥션 다수. |
| api-bookmark | 512 | 1024 | 2 | 2 | 4 | CRUD 중심. |
| api-agent | 2048 | 4096 | 2 | 2 | 10 | 에이전트 체인 추론, 외부 툴 호출. |
| batch-source | 2048 | 4096 | 0 | 0 | 1 | 스케줄 실행 시에만 기동. |

> 값은 **초기 시드**이며, Container Insights p95 CPU/Mem·ALB TargetResponseTime을 2주간 관측해 보정한다. (ECS Service Auto Scaling — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html>)

#### 1.3.2 JVM 튜닝 (Task Definition 환경변수)

```
JAVA_TOOL_OPTIONS = "-XX:MaxRAMPercentage=75.0 \
                     -XX:InitialRAMPercentage=50.0 \
                     -XX:+UseG1GC \
                     -XX:+UseContainerSupport \
                     -XX:+ExitOnOutOfMemoryError \
                     -Djava.security.egd=file:/dev/./urandom \
                     -Dfile.encoding=UTF-8 \
                     -Duser.timezone=Asia/Seoul"
SPRING_PROFILES_ACTIVE = "prod"
SERVER_SHUTDOWN = "graceful"
SPRING_LIFECYCLE_TIMEOUT_PER_SHUTDOWN_PHASE = "30s"
```

- `UseContainerSupport`는 JDK 21 기본이지만 명시. (<https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html>)
- Buildpack은 Memory Calculator를 자체 포함하므로 `MaxRAMPercentage`와 중복될 수 있다. Paketo Java BP 기본 계산을 쓰려면 `BPL_JVM_THREAD_COUNT` 등만 조정. 컨테이너 디버깅을 위해 본 설계에서는 명시적 `MaxRAMPercentage=75`로 고정한다. (<https://paketo.io/docs/reference/java-reference/>)

#### 1.3.3 Task Definition 보안 기본값

```jsonc
{
  "family": "api-auth",
  "runtimePlatform": { "cpuArchitecture": "ARM64", "operatingSystemFamily": "LINUX" },
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512", "memory": "1024",
  "executionRoleArn": "arn:aws:iam::...:role/ecs-exec-api-auth",
  "taskRoleArn":      "arn:aws:iam::...:role/ecs-task-api-auth",
  "containerDefinitions": [{
    "name": "api-auth",
    "image": "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/techai/api-auth@sha256:...",
    "user": "1000:1000",
    "readonlyRootFilesystem": true,
    "linuxParameters": { "initProcessEnabled": true },
    "portMappings": [{ "containerPort": 8083, "protocol": "tcp", "name": "http" }],
    "healthCheck": {
      "// 주의": "이 엔드포인트는 §1.7의 Actuator probe 활성화 선행 필수. 현재 프로젝트는 probes 미설정이며 설정 전 배포 시 컨테이너가 무한 재시작됨",
      "command": ["CMD-SHELL", "curl -f http://localhost:8083/actuator/health/liveness || exit 1"],
      "interval": 10, "timeout": 3, "retries": 3, "startPeriod": 60
    },
    "stopTimeout": 60,
    "logConfiguration": {
      "logDriver": "awslogs",
      "options": {
        "awslogs-group": "/ecs/prod/api-auth",
        "awslogs-region": "ap-northeast-2",
        "awslogs-stream-prefix": "api-auth"
      }
    }
  }]
}
```

- ARM64(Graviton) 사용 시 비용 ~20% 절감. Paketo Buildpack은 멀티 아키텍처를 빌더에서 결정하므로 빌드 머신에서 `--platform linux/arm64` 또는 CodeBuild Graviton 러너 사용. (<https://docs.aws.amazon.com/AmazonECS/latest/developerguide/graviton.html>)
- `readonlyRootFilesystem=true`. 쓰기 필요 시 `tmpfs` 볼륨 마운트(`/tmp`).
- Execution Role vs Task Role 분리:
  - **Execution Role**: ECR Pull, CloudWatch Logs, Secrets Manager 시크릿 가져오기.
  - **Task Role**: 서비스 기능(예: S3, MSK, RDS IAM auth) 접근. 모듈별로 최소 권한 분리.

#### 1.3.4 배치 모듈 (`batch-source`)

**선정: EventBridge Scheduler → ECS RunTask**

근거:
- AWS Batch는 대규모 HPC·배치 큐잉이 필요한 경우 유리하나, 본 프로젝트 배치는 단일 Spring Batch Job이므로 ECS 태스크 단발 실행이 충분하다.
- EventBridge Scheduler의 `aws-sdk:ecs:runTask` 타깃은 `LATEST` 태스크 정의 버전 호출·RetryPolicy·DLQ를 네이티브 지원한다. (<https://docs.aws.amazon.com/scheduler/latest/UserGuide/what-is-scheduler.html>, <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/scheduled_tasks.html>)
- 장시간(>30분) 배치는 Fargate `ephemeralStorage=200 GiB`까지 확장. Spring Batch `exitCode`를 컨테이너 종료 코드로 전파해 EventBridge DLQ 연동.

스케줄 예:

```
EventBridge Scheduler
 ├─ 이름: batch-source-nightly
 ├─ cron(0 17 * * ? *)  // KST 02:00 = UTC 17:00
 ├─ 타깃: arn:aws:ecs:...:cluster/prod → RunTask(batch-source:LATEST)
 ├─ FlexibleTimeWindow: off
 └─ DLQ: arn:aws:sqs:...:batch-source-dlq
```

### 1.4 오토스케일링

ECS Service Auto Scaling + Application Auto Scaling Target Tracking. (<https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html>)

| 모듈 | 1차 지표 | 2차 지표 (CustomMetric) | 타깃 |
|---|---|---|---|
| api-gateway | ALBRequestCountPerTarget | - | 200 req/target |
| api-auth | ECSServiceAverageCPUUtilization | - | 60% |
| api-emerging-tech | ECSServiceAverageCPUUtilization | - | 60% |
| api-chatbot | CustomMetric: `activeStreamingSessions` (CloudWatch EMF) | CPU 70% | 세션 100개/task |
| api-bookmark | ECSServiceAverageCPUUtilization | - | 60% |
| api-agent | ECSServiceAverageCPUUtilization | Mem 75% | 70% |

- **ScaleIn 보호**: 챗봇/에이전트 서비스는 `scaleInCooldown=300s`, `scaleOutCooldown=60s`. LLM 호출 도중 scaleIn 시 세션 끊김 방지.
- EKS 대안 참고(현재는 사용 안 함): HPA + KEDA + Cluster Autoscaler/Karpenter. KEDA는 Kafka lag, SQS depth 기반 스케일에 유리. (<https://docs.aws.amazon.com/eks/latest/best-practices/cluster-autoscaling.html>)

### 1.5 배포 전략 — Blue/Green with CodeDeploy

- ECS + CodeDeploy BLUE/GREEN 배포 타입. ALB Target Group 2개(Blue/Green), Listener Rule 스왑.
- `DeploymentConfig`: 모든 환경 기본 `CodeDeployDefault.ECSCanary10Percent5Minutes` (`modules/ecs-service/variables.tf` `deployment_config_name` 기본값과 정합. envs/{dev,beta,prod} 모두 override 없음). 문제 발생 시 자동 롤백 알람: `5xxErrorRate ≥ 1%`, `TargetResponseTime P95 ≥ 1.5s` (`modules/ecs-service/main.tf` `aws_cloudwatch_metric_alarm` 정의 기준).
- `TerminationWaitTimeInMinutes=10`으로 카나리 관찰 후 Blue 종료. (<https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html>)

### 1.6 API Gateway 계층 — ALB + Spring Cloud Gateway (AWS API Gateway 제거)

- 외부 엣지: **CloudFront → ALB(HTTPS) → api-gateway(Spring Cloud Gateway) Task**
- AWS API Gateway(HTTP/REST)는 사용하지 않는다. 근거:
  1. 이미 `api-gateway` 모듈이 라우팅·인증 필터·재시도·토큰 릴레이 역할을 수행. 이중 계층은 응답 지연·운영 복잡도·비용(AWS API Gateway 요청당 과금)을 유발.
  2. SSE/스트리밍(챗봇) 응답은 AWS API Gateway REST 타입의 29초 타임아웃 제한과 충돌. (<https://docs.aws.amazon.com/apigateway/latest/developerguide/limits.html>)
- ALB 설정:
  - Listener 443 HTTPS, TLS 정책 `ELBSecurityPolicy-TLS13-1-2-2021-06` (TLS 1.2+ 강제)
  - HTTP 80은 301 → HTTPS 리다이렉트
  - Target Group: `HTTP`, protocol version `HTTP/2`, deregistration_delay.timeout_seconds = **30** (Spring Boot graceful shutdown 30s와 정합)
  - Health Check: `GET /actuator/health/readiness`, healthy_threshold=2, unhealthy_threshold=3, interval=10s, matcher=200
  - 대상 응답 타임아웃: idle_timeout 120s (스트리밍 고려)
- Security Group: ALB ← 0.0.0.0/0:443, ALB → Task:8081 (SG 대 SG 레퍼런스)
- 내부 서비스 간 호출: Service Connect 또는 Cloud Map `*.prod.internal` 이용, ALB 미경유.

### 1.7 **경고 — Actuator 프로브 미설정 이슈 (배포 전 필수 조치)**

현재 `application.yml`에 `management.endpoint.health.probes`/`management.health.livenessState`/`management.health.readinessState` 설정이 없다. ALB Target Group이 `/actuator/health`(UP/DOWN 통합 상태)만 참조하게 되면 외부 의존성(DB·Kafka·MongoDB) 일시 장애 시 **모든 Task가 동시에 unhealthy → 503**이 될 수 있다.

배포 전 다음 설정을 모든 `api-*` 모듈 `application-prod.yml`에 반드시 추가한다. (Spring Boot Reference — Actuator Kubernetes probes — <https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes>)

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db,mongo
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,prometheus
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

ALB Health Check 경로:
- Liveness: ECS `healthCheck` 컨테이너 레벨 → `/actuator/health/liveness`
- Readiness: ALB Target Group → `/actuator/health/readiness`

### 1.8 베스트 프랙티스 체크리스트 (백엔드)

- [x] 컨테이너 non-root 실행 (`"user": "1000:1000"` 또는 Paketo BP `CNB_USER_ID`)
- [x] `readonlyRootFilesystem=true` + `/tmp` tmpfs
- [x] Task IAM Role 모듈별 분리 + 최소 권한
- [x] ECR Enhanced Scanning + Immutable tag + Lifecycle Policy
- [x] Graceful Shutdown: `server.shutdown=graceful`, `stopTimeout=60`, ALB deregistration_delay=30
- [x] Liveness/Readiness 분리 (§1.7 선행)
- [x] CodeDeploy Blue/Green + CloudWatch Alarm 롤백
- [x] HTTPS TLS 1.2+ (ALB TLS 1.3 정책)
- [x] Secrets: AWS Secrets Manager → Task Definition `secrets` 참조 (ENV 평문 금지)
- [x] Graviton ARM64 기본

---

## 2. 프론트엔드 호스팅 (frontend-hosting-design)

### 2.1 후보 비교

| 후보 | SSR/ISR | 미들웨어 | 배포 운영 | 엣지 캐싱 | 인증/사설화 | 월 비용(초기) | 적합도 |
|---|---|---|---|---|---|---|---|
| **AWS Amplify Hosting** | Next.js 16 SSR/ISR/Streaming 공식 지원 | 지원 | 매우 간편(Git 트리거) | Amplify 내장 CDN + CloudFront 프록시 | Access Control(Basic Auth), Custom Domain, Branch 단위 | Build 분 + 데이터 egress(낮은 트래픽 ~\$20–60) | app 최우선 |
| **CloudFront + OpenNext (S3 + Lambda)** | OpenNext 변환 후 Lambda@Edge / Lambda URL | 지원(변환) | 중간 — OpenNext 관리 필요, 공식 미지원 변환 | 최상 | Signed Cookie/Lambda auth | Lambda 요청 + CF 요청 | 트래픽 폭증 시 유리하나 운영 복잡 |
| **ECS Fargate + `next start`** | 완전 지원 (`output: 'standalone'` 필요) | 지원 | 백엔드와 동일 | 자체 약함(CloudFront 추가 필요) | ALB+Cognito OIDC | Task 상시 실행 비용 | admin 대안 |

(Next.js Deployment — <https://nextjs.org/docs/app/guides/deploying>, AWS Amplify Hosting SSR — <https://docs.aws.amazon.com/amplify/latest/userguide/ssr.html>)

**선정:**
- **`app` (사용자 공개) → AWS Amplify Hosting**: Next.js 16 SSR·미들웨어·ISR 공식 지원, 현재 `output` 미설정 상태(기본 SSR)를 그대로 수용한다. Git push 기반 Preview Branch가 PRD/QA 용 단기 환경에 유리.
- **`admin` (내부) → AWS Amplify Hosting + Access Control + CloudFront WAF Geo/IP 제한**: 별도 Amplify App, Branch Access Control 또는 Cognito Hosted UI 앞단. VPN-only가 엄격 요구되면 ECS Fargate + 내부 ALB(Private) + Client VPN으로 대체. 본 설계 기준은 Amplify + Cognito + WAF IP Set.

### 2.2 현재 프로젝트 상태 반영 — `output`·`.env`·rewrite 해결

#### 2.2.1 `output` 설정

- Amplify Hosting 선택 시: `next.config.ts`의 `output` 설정은 **그대로 비워둔다**. Amplify는 기본 SSR(`.next/` 서버 런타임)을 빌드·배포한다. (<https://docs.aws.amazon.com/amplify/latest/userguide/deploy-nextjs-app.html>)
- 향후 ECS 전환 시: `output: 'standalone'`을 추가해 `.next/standalone/server.js` 단일 번들로 실행.
- Lambda@Edge/OpenNext 전환 시: OpenNext CLI가 `standalone` 결과물을 Lambda 패키지로 변환.

```ts
// app/next.config.ts — 호스팅별 분기 예시
import type { NextConfig } from "next";

const API_BASE = process.env.API_GATEWAY_URL ?? "http://localhost:8081";

const config: NextConfig = {
  // ECS/Lambda 호스팅일 때만 주입
  ...(process.env.HOSTING_TARGET === "ecs" || process.env.HOSTING_TARGET === "lambda"
    ? { output: "standalone" as const }
    : {}),
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${API_BASE}/api/:path*` },
    ];
  },
  images: {
    // 현재 next/image 미사용. 도입 시 정의 필요
    remotePatterns: [
      { protocol: "https", hostname: "cdn.tech-n-ai.com" },
    ],
  },
};

export default config;
```

#### 2.2.2 `.env` 부재 해결 — 환경별 주입 전략

현재 `rewrites` destination이 `http://localhost:8081`로 하드코딩되어 있으므로 AWS 배포 시 반드시 환경 변수화한다.

**도입할 변수**:

| 변수 | 범위 | 용도 | 주입 시점 |
|---|---|---|---|
| `API_GATEWAY_URL` | 서버(런타임) | `next.config.ts` rewrite destination + SSR fetch | Amplify: Branch Environment Variables, ECS: Task Definition ENV |
| `NEXT_PUBLIC_API_BASE_URL` | 브라우저(빌드타임 inline) | 클라이언트 fetch (필요 시) | 빌드 시 주입 |
| `NEXT_PUBLIC_APP_ENV` | 브라우저 | `dev`/`beta`/`prod` | 빌드 시 |
| `AUTH_COOKIE_DOMAIN` | 서버 | 미들웨어 쿠키 읽기 | 런타임 |

**Amplify 환경변수 등록**:

```
Amplify Console → App → Hosting environments → {branch} → Environment variables
- API_GATEWAY_URL        = https://api.prod.tech-n-ai.com
- NEXT_PUBLIC_API_BASE_URL = https://api.prod.tech-n-ai.com
- NEXT_PUBLIC_APP_ENV    = prod
```

- `NEXT_PUBLIC_*` 접두어는 **빌드 시점**에 인라인된다. 환경별로 빌드가 분리되어야 한다(Amplify Branch = Environment). (<https://nextjs.org/docs/app/building-your-application/configuring/environment-variables>)
- 서버 전용 `API_GATEWAY_URL`은 런타임 조회이므로 동일 이미지로도 환경 분리 가능 — ECS 전환 시 유리.

#### 2.2.3 `amplify.yml` 빌드 명세 (app)

```yaml
version: 1
applications:
  - appRoot: tech-n-ai-frontend/app
    frontend:
      phases:
        preBuild:
          commands:
            - npm ci
        build:
          commands:
            - echo "API_GATEWAY_URL=$API_GATEWAY_URL"
            - npm run build
      artifacts:
        baseDirectory: .next
        files:
          - '**/*'
      cache:
        paths:
          - node_modules/**/*
          - .next/cache/**/*
```

(Amplify monorepo + Next.js — <https://docs.aws.amazon.com/amplify/latest/userguide/monorepo-configuration.html>)

### 2.3 `admin` 앱 보안 — 내부 사용자 전용 공개 제한

3가지 옵션 중 택일 (§06에서 최종 확정):

1. **Amplify Branch Access Control (Basic Auth) + Cognito Hosted UI 게이트** — 1차 IP/Basic 차단, 2차 OIDC로 사용자 인증. 배포 최단, 운영 간편.
2. **CloudFront + Signed Cookie/Signed URL** — Amplify 앞에 CloudFront Distribution을 두고 Signed Cookie 검증 Lambda@Edge 배치. 복잡도 중.
3. **Private ALB + AWS Client VPN (또는 AWS SSO + Verified Access)** — 완전 비공개. 사무실/VPN만 접근. ECS Fargate + Private ALB 조합으로 Next.js를 `output: 'standalone'`으로 호스팅.

권장: **1번 + WAF IP allowlist** (본 설계 기본). VPN 요구가 정책화되면 3번으로 확장.

### 2.4 이미지 최적화

- 현재 `next/image` 미사용, `images.remotePatterns` 미정의 → Amplify/CloudFront가 이미지 변환 람다를 띄우지 않으므로 **초기 비용 절감**.
- 도입 시 Amplify는 빌트인 `@next/image` 런타임을 지원한다. 또는 CloudFront Image Optimization(람다) 옵션. (<https://docs.aws.amazon.com/amplify/latest/userguide/deploy-nextjs-app.html#image-optimization>)
- 원격 이미지 도메인 허용은 반드시 `images.remotePatterns`에 한정(와일드카드 금지). (<https://nextjs.org/docs/app/api-reference/components/image#remotepatterns>)

### 2.5 미들웨어 — 쿠키 → Authorization 주입

- Next.js 미들웨어는 Amplify SSR 런타임과 Lambda@Edge에서 모두 실행된다. 단, Amplify에서는 Edge Runtime (Web API) 제한이 적용되므로 `accessToken` 쿠키 → `Authorization: Bearer` 헤더 주입 로직에서 Node.js 전용 API 금지.
- Set-Cookie 재발급(refresh flow)은 Next.js `route handler`에서 처리하고, 미들웨어는 읽기만 수행하는 현행 설계 유지.

### 2.6 프론트엔드 베스트 프랙티스 체크리스트

- [x] HTTPS TLS 1.2+ (Amplify는 기본 TLS 1.2+, CloudFront TLSv1.2_2021 Security Policy)
- [x] `NEXT_PUBLIC_*` 외 비밀 값 클라이언트 번들 포함 금지
- [x] 환경별 Branch/Build 분리 (dev/beta/prod)
- [x] CSP·HSTS·X-Frame-Options 헤더 (CloudFront Response Headers Policy 또는 `next.config.ts` headers)
- [x] admin IP/WAF allowlist
- [x] 빌드 결과 SBOM 생성(npm audit + cdxgen, CI 단계)

---

## 3. CDN & Edge (cdn-and-edge)

### 3.1 배포 토폴로지

```
[User] ──HTTPS──▶ CloudFront ──▶ (1) Amplify Hosting (app)          ── Next.js SSR
                                (2) Amplify Hosting (admin, WAF 제한) ── Next.js SSR
                                (3) ALB (api.prod.tech-n-ai.com)     ── ECS Fargate (api-gateway)
                                (4) S3 (assets.prod.tech-n-ai.com)    ── 정적 자산/다운로드
```

- Amplify는 자체 CDN을 가지나, 공통 도메인(`www.tech-n-ai.com`, `admin.tech-n-ai.com`)·공통 WAF·Route53 트래픽 정책·로그 집계를 위해 **단일 CloudFront 배포**를 앞단으로 둔다. (CloudFront Origin Selection — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/distribution-web-values-specify.html>)
- 동일 배포 내 Origin Request Policy / Cache Policy로 오리진 구분.

### 3.2 오리진별 캐시 정책

| 경로 패턴 | 오리진 | Cache Policy | Origin Request Policy | 비고 |
|---|---|---|---|---|
| `/api/*` | ALB (api-gateway) | `CachingDisabled` | `AllViewer` + `Authorization` 헤더 포함 | BFF 프록시, 캐시 금지 |
| `/_next/static/*` | Amplify app | `CachingOptimized` (1y immutable) | `CORS-S3Origin` | hash-busted 정적 |
| `/_next/image*` | Amplify app | `CachingOptimized` (30d) + Vary on query | `UserAgentRefererHeaders` | 현재 미사용, 도입 시 |
| `/_next/data/*` | Amplify app | 짧은 TTL(60s) | `AllViewerExceptHostHeader` | ISR/RSC data |
| `/` (SSR HTML) | Amplify app | `CachingDisabled` 또는 `Managed-CachingDisabled` | 쿠키·Auth 헤더 전달 | SSR 응답은 기본 no-cache |
| `/assets/*` | S3 | `CachingOptimized` (1y) | `CORS-S3Origin` | 공개 자산 |
| `admin.tech-n-ai.com/*` | Amplify admin | `CachingDisabled` | 전체 헤더/쿠키 | 내부 앱, 캐시 회피 |

(CloudFront Managed Policies — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-managed-cache-policies.html>)

### 3.3 S3 오리진 접근 — **OAC 사용 (OAI 금지)**

- S3 정적 자산 오리진은 **Origin Access Control (OAC)** 사용. OAI(Origin Access Identity)는 레거시로 신규 사용 금지. OAC는 SigV4 서명·KMS 암호화 S3 버킷 접근을 지원한다. (<https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html>)
- ALB/Amplify 오리진은 **Custom Origin**. 커스텀 헤더(`X-CloudFront-Secret`) + ALB Listener Rule로 CloudFront 우회 접근을 차단한다. (<https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/restrict-access-to-load-balancer.html>)

### 3.4 보안 헤더 (Response Headers Policy)

- Managed `SecurityHeadersPolicy` + 커스텀 CSP
- `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY` (admin은 `SAMEORIGIN`)
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), camera=(), microphone=()`

### 3.5 TLS·프로토콜

- CloudFront Viewer TLS 정책: `TLSv1.2_2021` 최소. HTTP/2·HTTP/3 활성.
- 오리진 프로토콜: ALB는 `https-only` + `TLSv1.2_2021`, Amplify는 `https-only`.
- 인증서: ACM us-east-1 발급(CloudFront 요구), Route53 DNS 검증.

### 3.6 CloudFront Function vs Lambda@Edge

| 구분 | CloudFront Function | Lambda@Edge |
|---|---|---|
| 이벤트 | Viewer Request/Response | 4가지(Viewer/Origin × Request/Response) |
| 런타임 | 순수 JS(V8, 1ms) | Node.js/Python, 외부 네트워크 가능 |
| 적합 용도 | URL rewrite, 헤더 검사, A/B 쿠키 설정, HSTS 주입 | SSR, Signed Cookie 검증, 외부 인증 콜백, 이미지 변환 |

(<https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions.html>)

**본 프로젝트 적용**:
- **CloudFront Function**:
  - Viewer Request: `/api/` 경로에 `x-forwarded-host` 헤더 주입, canonical 도메인 리다이렉트(www 없는 주소 → www).
  - Viewer Response: 보안 헤더 보강(Amplify가 기본 제공하지 않는 항목).
- **Lambda@Edge**:
  - admin 배포의 Signed Cookie 검증 (선택 옵션 2 채택 시).
  - 향후 이미지 변환 도입 시 Origin Response에서 Sharp 기반 리사이즈.

### 3.7 WAF

- CloudFront에 WebACL 연결. (상세 규칙은 `06-security-and-iam.md`에서 정의)
- 본 문서 범위: AWS Managed Rules Core Rule Set + Bot Control + admin 도메인 IP Set allowlist를 **연결 지점**으로 명시.

### 3.8 로깅·관측성

- CloudFront Standard Logs → S3(Parquet) + Athena 쿼리. (<https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/logging.html>)
- Real-time logs → Kinesis Data Streams (필요 시). 통상은 Standard Logs로 충분.
- Amplify Hosting Access Logs → CloudWatch.
- ECS Container Insights + ALB Access Logs → S3 → Athena.

---

## 4. 통합 체크리스트

### 4.1 배포 전 필수 (Go/No-Go)

- [ ] 모든 `api-*` 모듈에 `management.endpoint.health.probes.enabled=true` + liveness/readiness 그룹 정의 (§1.7)
- [ ] `server.shutdown=graceful`, `spring.lifecycle.timeout-per-shutdown-phase=30s` 추가
- [ ] `api-gateway`에 `spring.cloud.gateway.httpclient.connect-timeout`, `response-timeout` 명시
- [ ] `next.config.ts` rewrite destination을 `process.env.API_GATEWAY_URL`로 치환
- [ ] Amplify Branch Environment Variables에 `API_GATEWAY_URL`, `NEXT_PUBLIC_API_BASE_URL` 등록
- [ ] ECR 리포지토리 Immutable tag + Enhanced Scanning + Lifecycle Policy
- [ ] Task IAM Role 모듈별 분리 (최소 권한)
- [ ] CodeDeploy Blue/Green DeploymentGroup + CloudWatch Alarm 롤백
- [ ] CloudFront WebACL 연결, TLSv1.2_2021 최소
- [ ] ALB deregistration_delay=30, idle_timeout=120
- [ ] admin 앱 WAF IP allowlist 또는 Cognito Hosted UI 전단

### 4.2 운영 KPI

| 지표 | 목표 |
|---|---|
| ALB TargetResponseTime p95 (api-gateway) | < 400 ms |
| ECS Task readiness failure rate | < 0.5% |
| CloudFront 5xxErrorRate | < 0.1% |
| Amplify Build 성공률 | > 99% |
| 배포 롤백 트리거 응답 시간 | < 5 min (CodeDeploy auto rollback) |

---

## 5. 참고 자료 (공식)

- Amazon ECS Developer Guide — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/Welcome.html>
- ECS Service Auto Scaling — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/service-auto-scaling.html>
- ECS Blue/Green Deployment — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/deployment-type-bluegreen.html>
- ECS Scheduled Tasks — <https://docs.aws.amazon.com/AmazonECS/latest/developerguide/scheduled_tasks.html>
- AWS Fargate Security Best Practices — <https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/security-fargate.html>
- EKS Best Practices Guides — <https://docs.aws.amazon.com/eks/latest/best-practices/>
- Amazon ECR User Guide — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/>
- ECR Image Tag Mutability — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-tag-mutability.html>
- ECR Lifecycle Policy — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/LifecyclePolicies.html>
- ECR Enhanced Scanning — <https://docs.aws.amazon.com/AmazonECR/latest/userguide/image-scanning-enhanced.html>
- AWS Signer Container Image Signing — <https://docs.aws.amazon.com/signer/latest/developerguide/container-image-signing.html>
- EventBridge Scheduler — <https://docs.aws.amazon.com/scheduler/latest/UserGuide/what-is-scheduler.html>
- Spring Boot Container Images — <https://docs.spring.io/spring-boot/reference/packaging/container-images/>
- Spring Boot Buildpacks — <https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html>
- Spring Boot Kubernetes Probes — <https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes>
- Paketo Buildpacks Java Reference — <https://paketo.io/docs/reference/java-reference/>
- Paketo Builders — <https://paketo.io/docs/concepts/builders/>
- Next.js Deployment — <https://nextjs.org/docs/app/guides/deploying>
- Next.js Environment Variables — <https://nextjs.org/docs/app/building-your-application/configuring/environment-variables>
- AWS Amplify Hosting User Guide — <https://docs.aws.amazon.com/amplify/latest/userguide/>
- Amplify Hosting Next.js SSR — <https://docs.aws.amazon.com/amplify/latest/userguide/deploy-nextjs-app.html>
- Amplify Monorepo — <https://docs.aws.amazon.com/amplify/latest/userguide/monorepo-configuration.html>
- CloudFront Developer Guide — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/>
- CloudFront Managed Cache Policies — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/using-managed-cache-policies.html>
- CloudFront OAC for S3 — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/private-content-restricting-access-to-s3.html>
- CloudFront Edge Functions — <https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/edge-functions.html>
