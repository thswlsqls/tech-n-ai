# 00. 횡단 관심사 매트릭스 (Cross-Cutting Concerns)

> **문서 목적**: 여러 문서에 걸쳐 등장하는 자원(KMS·IAM·SG·Secret·환경변수)에 대해 **단일 정의처(Source of Truth)**와 **참조처(Consumers)**를 한 장에 명시한다. 이 매트릭스가 깨지면 정의 중복·정합성 누락이 발생하므로, 변경 시 본 문서를 먼저 갱신한 뒤 정의처 문서를 수정한다.
> **읽는 법**: "정의" 컬럼은 단 하나여야 하고, "참조" 컬럼의 문서들은 정의처를 링크로 가리키기만 해야 한다.

---

## 1. KMS Customer Master Key (CMK)

키 명명 규칙: `alias/techai/{env}-{purpose}` — **환경별 5키**(`auth`/`data`/`ai`/`logs`/`s3-app`)는 각 워크로드 계정의 `envs/<env>/main.tf`에서 정의. 추가로 `tfstate` 1키는 부트스트랩 계정(`bootstrap/kms.tf`)에 단일 정의되어 모든 환경이 공유(`alias/techai/tfstate`). ECR 이미지 암호화용 KMS 1키도 부트스트랩에서 단일 정의.

| 키 alias | 용도 | Principal (사용 주체) | **정의** | 참조 |
|---|---|---|---|---|
| `{env}-auth` | api-auth JWT 서명·검증, RDS IAM 인증 시 envelope | `api-auth` Task Role | [06 §3.5](06-security-and-iam.md) | [04 §1 Aurora](04-database-and-storage.md) |
| `{env}-data` | Aurora·MongoDB·ElastiCache·S3 dataset 암호화 | api-* Task Roles, Aurora·Mongo 서비스 | [06 §3.5](06-security-and-iam.md) | [04 §1·2·3](04-database-and-storage.md), [05 §1.5 MSK](05-messaging-kafka.md) |
| `{env}-ai` | OpenAI 키, Bedrock 호출 envelope, Vector Search payload 암호화 | `api-chatbot`, `api-agent` Task Roles | [06 §3.5](06-security-and-iam.md) | [04 §2 MongoDB](04-database-and-storage.md) |
| `{env}-logs` | CloudWatch Logs, OpenSearch, Athena query result | CloudWatch Logs, Logs Resource Policy | [06 §3.5](06-security-and-iam.md) | [08 §3 로그](08-observability.md) |
| `{env}-s3-app` | 어플리케이션용 S3 버킷(uploads, static-assets) | api-* Task Roles, CloudFront OAC | [06 §3.5](06-security-and-iam.md) | [04 §4 S3](04-database-and-storage.md) |
| `tfstate` (단일 공유) | Terraform state S3 버킷·DynamoDB Lock — bootstrap 계정에 단일, 환경 간 공유 | Terraform Apply Role, Read-only Role | [06 §3.5](06-security-and-iam.md), [09 §state-bootstrap](09-iac-terraform.md) | [07c §2 State](07c-cicd-infra.md) |

> **분리 기준**: 키 별 폭발 반경(Blast Radius) 최소화 — 한 키가 침해되어도 영향 범위가 한정됨. AI 호출 키와 인증 키를 분리해 chatbot 권한 침해가 인증 데이터로 번지지 않도록.

---

## 2. IAM Role 및 Trust Policy

### 2.1 GitHub OIDC 신뢰 Role (CI/CD 전용)

모든 Trust Policy의 Federated Principal은 동일 OIDC provider(`token.actions.githubusercontent.com`)를 사용하며, `sub` 조건만 다르다. **정의는 모두 [06 §2.4](06-security-and-iam.md)** 단일.

| Role 이름 | sub 조건 | 권한 요지 | 사용처 |
|---|---|---|---|
| `gha-deploy-{env}` | `repo:org/tech-n-ai:environment:{env}` | ECR push, ECS update-service, CodeDeploy create-deployment, SSM·Secrets read, Amplify start-job | [07a §7.2 backend-cd.yml](07a-cicd-backend.md), [07b §5 frontend-ci-cd.yml](07b-cicd-frontend.md) |
| `gha-terraform-readonly` | `repo:org/tech-n-ai:pull_request` | terraform plan, S3 state read, DynamoDB Lock read | [07c §5.1 terraform-plan.yml](07c-cicd-infra.md) |
| `gha-terraform-apply-{env}` | `repo:org/tech-n-ai:environment:tf-{env}` | terraform apply, AWS 리소스 생성·수정·삭제 | [07c §5.2 terraform-apply.yml](07c-cicd-infra.md) |
| `gha-security-scan` | `repo:org/tech-n-ai:ref:refs/heads/main` | ECR describe·pull, Inspector findings read | [07c §5.3 security-scan.yml](07c-cicd-infra.md) |

GitHub Actions에서 사용하는 secrets는 정확히 이 4개의 ARN이며, 워크플로 본문에 ARN을 박지 않고 `${{ secrets.AWS_*_ROLE_ARN }}`로만 참조한다.

### 2.2 ECS Task Role (워크로드 전용)

| Role 이름 | 부착 모듈 | 권한 요지 | **정의** | 참조 |
|---|---|---|---|---|
| `task-{env}-api-gateway` | api-gateway | SSM Parameter Store read | [06 §2.2.1](06-security-and-iam.md) | [03 §2 Task Def](03-compute-and-frontend-hosting.md) |
| `task-{env}-api-auth` | api-auth | Secrets Manager(aurora·jwt) read, RDS IAM connect, KMS Decrypt(`{env}-auth`) | [06 §2.2.2](06-security-and-iam.md) | [03 §2](03-compute-and-frontend-hosting.md), [04 §1.4 Aurora 인증](04-database-and-storage.md) |
| `task-{env}-api-chatbot` | api-chatbot | Secrets Manager(openai·cohere) read, KMS(`{env}-ai`) — Bedrock InvokeModel은 코드 도입 시 별도 ADR로 추가 | [06 §2.2.3](06-security-and-iam.md) | [03 §2](03-compute-and-frontend-hosting.md) |
| `task-{env}-api-agent` | api-agent | MSK kafka-cluster:Connect/Produce/Consume, Secrets Manager(mongo) read | [06 §2.2.4](06-security-and-iam.md) | [05 §3 컨슈머](05-messaging-kafka.md) |
| `task-{env}-api-bookmark` | api-bookmark | RDS IAM connect, ElastiCache AUTH 토큰 read (현행 — RBAC 전환은 별도 ADR, 04 §3.4) | [06 §2.2.5](06-security-and-iam.md) | [04 §3 Cache](04-database-and-storage.md) |
| `task-{env}-api-emerging-tech` | api-emerging-tech | Secrets Manager(openai) read | [06 §2.2.6](06-security-and-iam.md) | [03 §2](03-compute-and-frontend-hosting.md) |
| `task-{env}-batch-source` | batch-source | S3 read·write(`{env}-s3-app`), MSK Produce | [06 §2.2.7](06-security-and-iam.md) | [03 §2.7 배치](03-compute-and-frontend-hosting.md) |

### 2.3 ECS Task Execution Role (인프라 전용)

`task-execution-{env}` 단일 — ECR Pull, CloudWatch Logs `CreateLogStream`, Secrets Manager `GetSecretValue` (taskdef.json `secrets[]` 주입용). [06 §2.3](06-security-and-iam.md)에서 정의, 모든 Task Definition이 참조.

---

## 3. 보안 그룹 (Security Group)

| SG 이름 | 인바운드 소스 | **정의** | 참조 |
|---|---|---|---|
| `sg-alb-public` | 0.0.0.0/0 :443 | [02 §3 SG 매트릭스](02-network-vpc.md) | [03 §2 ECS Service](03-compute-and-frontend-hosting.md) |
| `sg-ecs-{module}` | `sg-alb-public` :{8081~8086} | [02 §3](02-network-vpc.md) | [03 §2](03-compute-and-frontend-hosting.md) |
| `sg-aurora` | `sg-ecs-*` :3306 | [02 §3](02-network-vpc.md) | [04 §1](04-database-and-storage.md) |
| `sg-elasticache` | `sg-ecs-*` :6379 | [02 §3](02-network-vpc.md) | [04 §3](04-database-and-storage.md) |
| `sg-msk` | `sg-ecs-*` :9098(IAM), :9094(TLS) | [02 §3](02-network-vpc.md) | [05 §1.4](05-messaging-kafka.md) |
| `sg-vpc-endpoints` | `sg-ecs-*` :443 | [02 §4 VPC Endpoint](02-network-vpc.md) | [04·05·06 등](.) |
| `sg-mongodb-pl` | `sg-ecs-*` :443/27017 | [02 §3 PrivateLink](02-network-vpc.md) | [04 §2 MongoDB](04-database-and-storage.md) |

> **변경 정책**: SG는 02에서만 정의·수정한다. 다른 문서는 SG 이름으로만 참조하고 인바운드/아웃바운드 규칙을 재정의하지 않는다.

---

## 4. Secrets Manager 시크릿

| 시크릿 이름 | 내용 | 회전 정책 | **정의** | 사용 (ECS 모듈) |
|---|---|---|---|---|
| `techai/{env}/aurora-credentials` | username, password | RDS 통합 자동 회전(`manage_master_user_password=true`, 기본 7일 — 정책상 30일 권장 시 `master_user_secret_rotation_interval`로 오버라이드) | [06 §3.2](06-security-and-iam.md) | api-auth, api-bookmark, api-emerging-tech, batch-source |
| `techai/{env}/mongodb-uri` | Atlas connection URI (X.509 인증) | 90일 수동(Atlas Programmatic API) | [06 §3.2](06-security-and-iam.md) | api-chatbot, api-agent, batch-source |
| `techai/{env}/jwt-signing-key` | HMAC SHA-512 비밀키(현행) — RS256 키페어로의 마이그레이션은 별도 ADR | **180일 무중단(kid 듀얼 키 `active`/`next` 롤오버, 24h 관용)** | [06 §3.2](06-security-and-iam.md) | api-auth |
| `techai/{env}/openai-api-key` | OpenAI API key | 90일 수동 | [06 §3.2](06-security-and-iam.md) | api-chatbot, api-emerging-tech |
| `techai/{env}/elasticache-auth-token` | Valkey AUTH 토큰 | 90일 수동 회전 | [06 §3.2](06-security-and-iam.md) | api-bookmark, api-auth |

ECS Task Definition은 시크릿 값을 본문에 박지 않고 `secrets[]` 필드에 ARN만 명시한다(execution role이 GetSecretValue 수행).

---

## 5. 환경 설정 주입 (Spring profile / Next.js 환경변수)

| 키 | 값 | 주입 방식 | **정의** | 사용 |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev`·`beta`·`prod` | ECS Task Definition `environment[]` | [03 §2.5](03-compute-and-frontend-hosting.md) | 백엔드 7개 모듈 |
| `JAVA_TOOL_OPTIONS` | 환경별 힙·GC 옵션 | ECS Task Definition `environment[]` | [03 §2.5](03-compute-and-frontend-hosting.md) | 백엔드 7개 모듈 |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | `true` | ECS Task Definition `environment[]` (필수) | [03 §2.7 Actuator probe](03-compute-and-frontend-hosting.md), [08 §2 Probe](08-observability.md) | **모든** 백엔드 모듈 |
| `NEXT_PUBLIC_*` | API base URL 등 빌드타임 인라인 | Amplify pre-build phase에서 SSM Parameter Store fetch | [03 §3.4](03-compute-and-frontend-hosting.md) | app, admin |
| `API_GATEWAY_URL` | Gateway ALB DNS | Amplify Branch Environment Variable | [03 §3.4](03-compute-and-frontend-hosting.md) | app, admin (런타임) |

> **이미지 무관 원칙**: Spring 프로파일·환경 분기 값은 **이미지에 박지 않는다**. 단일 이미지 + 단일 ECR 리포(07a §4.1) 전제와 일관.

---

## 6. 알람·관측성 메트릭 명명

| 메트릭/알람 | 명명 규칙 | **정의** | 참조 |
|---|---|---|---|
| CloudWatch Alarm | `techai/{env}/{service}/{metric}-{threshold}` | [08 §5 알람 카탈로그](08-observability.md) | [07a §6.1 자동 롤백](07a-cicd-backend.md) |
| OTel Resource attribute | `service.name = {module}`, `service.namespace = techai`, `deployment.environment = {env}` | [08 §4 트레이싱](08-observability.md) | 모든 Spring 모듈 (Actuator) |
| 로그 그룹 | `/aws/ecs/{env}/{module}` | [08 §3](08-observability.md) | 03 Task Definition `logConfiguration` |

---

## 7. 변경 절차

이 매트릭스에 영향을 주는 변경(키 추가, Role 권한 확장, SG 규칙 변경 등)은 다음 순서를 지킨다:

1. **본 문서 매트릭스 업데이트** — "정의처"가 결정되어 있는지 확인.
2. **정의처 문서 수정** (06·02·03 등).
3. **참조처 문서들의 링크가 깨지지 않았는지 확인** — `grep`으로 다른 문서가 같은 자원을 별도 정의하고 있지 않은지 검사.
4. **Terraform 모듈 적용** ([09](09-iac-terraform.md)).

> 정의처가 둘 이상 등장하면 매트릭스 위반. PR 리뷰 시 본 문서를 동봉한다.

---

**문서 끝.**
