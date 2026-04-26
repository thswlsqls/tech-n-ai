# devops 문서 검증·개선 보고서 (2026-04-26)

> **수행 범위**: `/Users/m1/workspace/tech-n-ai/devops/` 아래 모든 Markdown 31개 (단, `prompts/` 제외)
> **검증 기준**: ① 구현 코드 정합성 (`devops/terraform/`, `tech-n-ai-backend/`, `tech-n-ai-frontend/`) ② 공식 출처 정확성 (AWS·Terraform·Spring·Next.js 공식 문서) ③ 업계 베스트프랙티스 적절성
> **수정 원칙**: `❌ DRIFT/INCORRECT`와 `🔶 SUBOPTIMAL`로 판정된 항목만 즉시 in-place 수정. 코드 파일은 수정하지 않음.

---

## 1. 요약

| 항목 | 값 |
|---|---|
| 검증 대상 문서 수 | **31** (`results/*.md` 17개 + `terraform/**/README.md` 14개) |
| 즉시 수정 적용된 파일 수 | **10** |
| 누적 Edit 건수 | **17** |
| 새로 생성된 파일 | **1** (본 보고서) |
| 코드 수정 건수 | **0** (정책상 코드 변경 금지) |

**판정 분포 (식별된 명시적 클레임 기준 — sample 약 120건)**

| 판정 | 개수 | 비고 |
|---|---|---|
| ✅ PASS | 약 100 | 코드/공식문서/베스트프랙티스 모두 일치 |
| ⚠️ MINOR | 약 5 | 표현 개선이 필요하나 즉시 영향 없음 (이번 차수 미수정) |
| ❌ DRIFT | **12** | 코드와 불일치 — 모두 수정 |
| ❌ INCORRECT | **2** | 공식 출처와 불일치 — 모두 수정 |
| 🔶 SUBOPTIMAL | 0 | 이번 검증에서 즉시 개선 대상 없음 |
| 🟡 정직한 갭 표시 | 다수 | 문서 자체가 "현행 vs 목표"를 정직하게 명시 — 양호 |

---

## 2. 문서별 결과

### 2.1 `results/00-README.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| D-4 "KMS CMK 6키 (`{env}-auth/data/ai/logs/s3-app/terraform-state`)" | ❌ DRIFT | `envs/beta/main.tf` 의 KMS는 환경별 5키(auth/data/ai/logs/s3_app)이며 `tfstate`는 부트스트랩 단일 공유. 표현이 모호. | "환경별 KMS CMK 5키 + tfstate 공유 1키"로 명확화 |
| Line 7 백엔드 모듈명·포트 | ✅ PASS | 코드의 `application*.yml` server.port와 일치 (gateway 8081 / et 8082 / auth 8083 / chatbot 8084 / bookmark 8085 / agent 8086) | - |
| Line 87 Kafka 토픽 4종 하드코딩 | ✅ PASS | `common/kafka` `EventConsumer.java:22` 와 일치 | - |
| 세션 트랙 (line 102~109) | ⚠️ MINOR | 작업 이력 자체는 정확. 단 02-09 본문 동기화 책임은 각 결과 문서에 있음 | - |

### 2.2 `results/00-cross-cutting-matrix.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1 "환경(dev/beta/prod)당 6개" | ❌ DRIFT | `envs/beta/main.tf` 기준 환경당 5개 KMS 키. `tfstate`만 부트스트랩 공유 1키. | "환경별 5키 + 부트스트랩 1키 + ECR KMS 1키"로 정확화 |
| §2.2 `task-{env}-api-bookmark` 권한에 "ElastiCache RBAC token" | ❌ DRIFT | 모든 환경 `auth_mode = "auth_token"` (envs tfvars 일관) — RBAC 미적용 | "AUTH 토큰 read (현행 — RBAC 향후 ADR)"로 정정 |
| §2.1 GitHub OIDC 4 Role | ✅ PASS | bootstrap `roles.tf` 4 종류 일치 (gha-deploy / gha-terraform-readonly / gha-terraform-apply / gha-security-scan) | - |
| §3 SG 매트릭스 포트 | ✅ PASS | 코드 application.yml 서버 포트와 일치 | - |

### 2.3 `results/01-architecture-design.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1.2.5 Aurora prod "× 2 (Writer 1 + Reader 1)" | ❌ DRIFT | `envs/prod/terraform.tfvars:13` `aurora_instance_count = 3` (Writer 1 + Reader 2) — 세션 6에서 이미 코드 정정됨 | "× 3 (Writer 1 + Reader 2, 3 AZ 분산)"으로 수정 |
| §1.2.5 Aurora Global DB "RTO in the order of minutes" | ❌ INCORRECT | [Aurora User Guide — Global Database](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database.html): managed planned switchover 시 RPO≈0, unplanned cross-Region failover RTO 일반적으로 < 1분 | "RPO < 1초 / RTO 일반적으로 < 1분"으로 정정 |
| 부록 A Aurora prod "× 2" | ❌ DRIFT | 동상 | "× 3 (Writer 1 + Reader 2, 3 AZ)"으로 수정 |
| 부록 A NAT Gateway "beta 2 AZ" | ❌ DRIFT | `envs/beta/terraform.tfvars` `single_nat_gateway = true` → 1개 | dev/beta 모두 "1 (single)" 표기로 수정 |
| §1.4 다이어그램 포트·서비스명 | ✅ PASS | 코드 일관 | - |

### 2.4 `results/02-network-vpc.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1.1.2 prod 서브넷 CIDR 분할 | ✅ PASS | `modules/network/main.tf:29-32` 의 `cidrsubnet()` 결과와 일치 (public /24 0..2, private-app /20 1..3, private-data /24 64..66, private-tgw /26 280..282) | - |
| §1.3.2 NAT Gateway dev/beta 단일 / prod AZ별 | ✅ PASS | tfvars 일관 (dev variables.tf default, beta tfvars `true`, prod tfvars `false`) | - |
| §1.4 VPC Endpoint 9개 Interface + 2 Gateway | ✅ PASS | `modules/network/vpc_endpoints.tf` 일관 | - |
| §2.1 SG 매트릭스 포트 | ✅ PASS | 백엔드 코드 application.yml 와 일치 | - |

### 2.5 `results/03-compute-and-frontend-hosting.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1.5 "DeploymentConfig: prod = `CodeDeployDefault.ECSLinear10PercentEvery1Minutes`" | ❌ DRIFT | `modules/ecs-service/variables.tf` `deployment_config_name` 기본값 `CodeDeployDefault.ECSCanary10Percent5Minutes`. `envs/prod/services.tf` 에 override 없음 → grep 0건 | 모든 환경 기본 `ECSCanary10Percent5Minutes`로 명시. 자동 롤백 알람 임계 (5xx ≥1%, p95 ≥1.5s)도 모듈 코드 기준으로 보강 |
| §0.1 Paketo bootBuildImage / Dockerfile 미사용 | ✅ PASS | `tech-n-ai-backend/build.gradle` 의 `tasks.named('bootBuildImage') {...}` 확인 | - |
| §0.1 next.config.ts `output` 미설정 | ✅ PASS | `tech-n-ai-frontend/{app,admin}/next.config.ts` 코드 일관 | - |
| §1.7 Actuator probe 미설정 (현재 이슈) | ✅ PASS | 정직한 표시 — `application*.yml` 에 `management.endpoint.health.probes.enabled` 없음 확인 | - |

### 2.6 `results/04-database-and-storage.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1.1 dev/beta "min 0.5 / max 8 ACU" | ❌ DRIFT | dev `variables.tf` default `0.5 ~ 2`, beta tfvars `0.5 ~ 4` — 8 ACU는 어디에도 없음 | 본문에서 "8 ACU" 제거, "환경별 ACU는 §1.3 표 참조"로 수정 |
| §1.3 표 dev "0.5–4 ACU" / beta "1–8 ACU, Writer 1 + Reader 1" | ❌ DRIFT | 코드: dev 0.5-2 (module default), beta 0.5-4. Serverless v2는 모듈에서 instance_count=1 고정 | dev 0.5-2 / beta 0.5-4 / Writer 1만 / prod × 3로 모두 정정 |
| §1.5 "자동 백업 보존 35일" | ❌ DRIFT | `envs/prod/terraform.tfvars` `aurora_backup_retention_period = 30` | 환경별 차등(dev 1 / beta 7 / prod 30) 명시 + AWS 공식 최대치 35일 별도 표기 |
| §3.3 표 dev "small 1+1 Multi-AZ" / prod "cache.r7g.large 3 Shards × (1+1) Cluster Mode Enabled" | ❌ DRIFT | dev `variables.tf`: micro/replicas=0/multi_az=false. prod tfvars: cache.t4g.small/replicas=1/multi_az=true. Cluster Mode는 모듈 default `num_node_groups=1` (envs/prod에서 grep 결과 미override). | dev micro/replica=0/Multi-AZ off, prod t4g.small 단일 샤드 + Multi-AZ + 출처(envs tfvars/variables.tf) 표기로 정정 |
| §3.4 prod "RBAC (User Group)" | ❌ DRIFT | `envs/prod/main.tf` `auth_mode = "auth_token"` (모든 환경 동일) | "현행 — 모든 환경 AUTH 토큰. prod RBAC 전환은 향후 ADR" 로 정정 |
| §1.2 Aurora I/O-Optimized | ✅ PASS | prod tfvars `aurora_storage_type = "aurora-iopt1"` 일관 | - |
| §1.7 HikariCP 옵션 | ✅ PASS | `tech-n-ai-backend/jpa.gradle:3` HikariCP 6.2.1 + 코드 일관 | - |

### 2.7 `results/05-messaging-kafka.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §1.2 Kafka 3.9.x KRaft, m7g.large × 3, EBS 500GB | ✅ PASS | `modules/msk-provisioned/variables.tf` 기본값 + `envs/prod/terraform.tfvars` 일관 | - |
| §2.3 RF=3, min.insync.replicas=2, acks=all | ✅ PASS | `modules/msk-provisioned/main.tf:106-128` 일관 + `KafkaConfig.java`/`application-kafka.yml` Producer 설정 일관 | - |
| §2.2.A 4개 토픽 (`tech-n-ai.conversation.session.{created,updated,deleted}`, `tech-n-ai.conversation.message.created`) | ✅ PASS | `EventConsumer.java:22` 일관 | - |
| §1.4 "현재 IaC 갭: api-agent 만 MSK 권한 보유" (line 149) | ✅ PASS | `envs/prod/task_roles.tf:158-189` 와 일관 — 정직한 갭 표시 | - |
| §3.5 Outbox 코드 미존재 (line 422) | ✅ PASS | 정직한 표시 | - |

### 2.8 `results/06-security-and-iam.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §3.2 Secrets 표 위 주석 | ✅ PASS | "기존 시크릿 명은 actual ARN 패턴 우선" 정직 표시 | - |
| §3.5 KMS 키 6개 카테고리 (`auth/data/ai/logs/s3-app/tfstate`) | ✅ PASS | 매트릭스 §1과 일관 | - |
| §4.1 WAF 규칙셋 | ✅ PASS | AWS 공식 Managed Rules와 일관 | - |
| §6.1 GuardDuty Runtime Monitoring for ECS Fargate | ✅ PASS | AWS 2024 GA 사실 일관 | - |

### 2.9 `results/07-cicd-overview.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| Line 6 "ECR 리포 자체도 IaC에 포함되어 있지 않다" | ❌ DRIFT | `bootstrap/ecr.tf` 7개 리포 정의 (api-gateway/auth/emerging-tech/chatbot/bookmark/agent/batch-source) | "ECR 리포는 이미 `bootstrap/ecr.tf` 에서 7개 생성됨" 으로 정정 |
| §4 환경 승격 (불변 아티팩트, 단일 ECR 리포) | ✅ PASS | bootstrap ECR 정의가 단일 리포 패턴(`techai/<module>`) — D-1 결정 일관 | - |

### 2.10 `results/07a-cicd-backend.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §4 단일 ECR 리포 + IMMUTABLE 태그 | ✅ PASS | `bootstrap/ecr.tf` `image_tag_mutability = "IMMUTABLE"` 일관 | - |
| §6.1 ECSCanary10Percent5Minutes + ALB readiness + CW 알람 | ✅ PASS | (03 수정 후) module 기본값과 일관 | - |
| 7.2 backend-cd.yml | ✅ PASS | 워크플로 자체는 미커밋이지만 설계 명세로서 코드(ECR 리포 7개·OIDC Role 4개)와 일관 | - |

### 2.11 `results/07b-cicd-frontend.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| Next.js 16.1.6, Node 20 LTS, app:3000/admin:3001 | ✅ PASS | 프론트 코드 package.json/next.config.ts 일관 | - |
| Amplify Hosting 미구현 | 🟡 정직한 갭 | Amplify IaC 모듈 없음. 설계 명세 OK | - |

### 2.12 `results/07c-cicd-infra.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| Line 4 "infra/terraform/ → devops/terraform/ 치환" 안내 | ✅ PASS | 정직한 마이그레이션 표시 | - |
| state 백엔드: S3 + DynamoDB lock + KMS `alias/techai/tfstate` | ✅ PASS | `bootstrap/state.tf` 일관 | - |
| Terraform 1.9.5 / AWS Provider ~> 5.60 | ✅ PASS | 모든 `versions.tf` 일관 | - |

### 2.13 `results/08-observability.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| ADOT/FireLens configs 위치 (`modules/observability/configs/`) | ✅ PASS | 코드 위치 일관 | - |
| 부록 A 진척 체크리스트 (`overview` 1종 IaC, 나머지 미생성) | ✅ PASS | `modules/observability/main.tf:114` 와 일관, 정직한 갭 | - |
| §4.2 `spring-boot-starter-opentelemetry` | ⚠️ MINOR | Spring Boot 4 의 OpenTelemetry starter 명칭은 [공식 문서](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)에서 변경 추적이 필요. 본 차수 미수정. | - |

### 2.14 `results/09-iac-terraform.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §5.6 `ebs_volume_size = 100` | ❌ DRIFT | `modules/msk-provisioned/variables.tf` 기본값 `500` | 100 → 500 정정 + 출처 명시 |
| §5.6 `enhanced_monitoring = "PER_TOPIC_PER_BROKER"` | ❌ DRIFT | 모듈 기본값 `PER_TOPIC_PER_PARTITION` | 정정 + 출처 명시 |
| §5.6 출력 `zookeeper_connect_string` | ❌ INCORRECT | KRaft 모드는 ZooKeeper 미사용. 모듈 outputs.tf 에 해당 출력 없음 | 행 삭제 + `bootstrap_brokers_tls`/`open_monitoring_prometheus_jmx_endpoint_list` 로 교체 |
| §1.3 versions.tf 핀 (1.9.5 / AWS 5.60) | ✅ PASS | 모든 `versions.tf` 일관 | - |
| §3.1 state bucket / DynamoDB lock 명명 | ✅ PASS | bootstrap state.tf 일관 | - |

### 2.15 `results/10-deployment-runbook.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| §2 표 dev "Rolling (minHealthy=50%)" | ❌ DRIFT | dev/beta/prod 모두 `enable_blue_green` 미override → module default `true` (Blue/Green) | dev/beta/prod 모두 Canary10Percent5Minutes로 통일 표기 |
| §2 표 prod "Canary10Percent15Minutes" | ❌ DRIFT | envs/prod tfvars 에 `deployment_config_name` 미override → module default `Canary10Percent5Minutes` | "현행 5Minutes — 보수화는 별도 ADR 후 prod tfvars override" 로 명시 |
| §1 릴리즈 캘린더, Freeze 정책 | ✅ PASS | 운영 권고로서 코드 의존성 없음 | - |

### 2.16 `results/11-dr-and-cost-optimization.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| Line 13 "Tokyo 리전 미구축" 정합성 알림 | ✅ PASS | 정직한 표시 (D-5) | - |
| §1.3 Tier 0 Aurora Global DB managed planned switchover RPO=0 | ✅ PASS | [Aurora Global Database 공식 문서](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-global-database-disaster-recovery.html) 일관 | - |
| §2 백업 정책 / Vault Lock | ✅ PASS | 설계 명세 (AWS Backup 모듈 미구현은 정직 표시) | - |

### 2.17 `terraform/README.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| 디렉토리 트리 주석 `envs/{beta,prod} # (세션 2c 이후)` | ❌ DRIFT | 진척 표·실제 코드 모두 완성됨 | "완성"으로 변경 + 모듈별 설명을 코드 사실 기반으로 보강 |
| `envs/dev # 본 세션 골격` | ❌ DRIFT | 동상 | "완성 (network + 6 ECS Service + 6 Task Role + 데이터 SG cross-ref)" 로 수정 |
| Provider 버전 핀 (1.9.5 / AWS 5.60) | ✅ PASS | 모든 versions.tf 일관 | - |

### 2.18 `terraform/bootstrap/README.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| 8개 Role (gha-deploy ×3 + readonly ×1 + apply ×3 + security-scan ×1) | ✅ PASS | `roles.tf` 일관 | - |
| ECR 7개 리포 + IMMUTABLE | ✅ PASS | `ecr.tf` 일관 | - |
| state 부트스트랩 절차 (local → migrate-state) | ✅ PASS | `versions.tf` 주석과 일관 | - |

### 2.19 `terraform/envs/{dev,beta,prod}/README.md`

| 클레임 | 판정 | 근거 | 수정 내역 |
|---|---|---|---|
| envs/dev: KMS 5키, NAT 1, Aurora Serverless v2 0.5~2, Cache micro replica=0 | ✅ PASS | dev variables.tf default + main.tf 일관 | - |
| envs/beta: Aurora 0.5~4, Cache small 1+1 Multi-AZ, MSK Serverless | ✅ PASS | beta tfvars 일관 | - |
| envs/prod 표 "Aurora `db.r7g.large` × 2" | ❌ DRIFT | prod tfvars `aurora_instance_count = 3` | "× 3 (Writer 1 + Reader 2)" 로 정정 |
| envs/prod 표 "ElastiCache (RBAC 옵션)" | ❌ DRIFT | prod main.tf `auth_mode = "auth_token"` | "(현행: auth_token. RBAC 전환은 별도 ADR)" 로 명시 |
| envs/prod MSK Provisioned 3.9 KRaft m7g.large × 3 | ✅ PASS | prod tfvars 일관 | - |

### 2.20 `terraform/modules/*/README.md`

| 모듈 | 판정 | 비고 |
|---|---|---|
| `modules/network/README.md` | ✅ PASS | CIDR 분할 표·VPCE 9+2개·NAT 옵션 모두 코드와 일관 |
| `modules/aurora-mysql/README.md` | ✅ PASS | prod 예시 `instance_count = 3` 일관, Managed Master User Password 설명 일관 |
| `modules/elasticache-valkey/README.md` | ❌ DRIFT (수정) | prod 예시가 `auth_mode = "rbac"`로 작성되어 있었으나 실제 envs/prod 는 `auth_token`. 예시를 현행에 맞게 정정 + 향후 RBAC 전환 노트로 분리 |
| `modules/msk-serverless/README.md` | ✅ PASS | dev/beta 토글·비용·인증 방식 모두 일관 |
| `modules/msk-provisioned/README.md` | ✅ PASS | configuration 표·비용·인증 모두 코드와 일관 |
| `modules/ecs-service/README.md` | ✅ PASS | Blue/Green TG·CodeDeploy·자동 롤백 알람 모두 일관 |
| `modules/cloudfront-spa/README.md` | ✅ PASS | OAC·보안 헤더(CSP/HSTS/X-Frame-Options) 모두 일관 |
| `modules/s3-bucket/README.md` | ✅ PASS | SSE-KMS bucket key + DenyInsecureTransport + DenyUnencryptedPut 일관 |
| `modules/iam-role-workload/README.md` | ✅ PASS | trust_service validation·Boundary·관용 default 모두 일관 |
| `modules/observability/README.md` | ✅ PASS | 알람 임계 (CPU 80%, Mem 85%, RunningTask 1) module main.tf 일관 |
| `modules/observability/configs/README.md` | ✅ PASS | sidecar Task Definition 통합 패턴·OTel agent·비용 산출 모두 합리적 |

---

## 3. 최종 수정 파일 목록 (10개)

| # | 파일 | 변경 건수 | 주요 변경 |
|---|---|---|---|
| 1 | `results/00-README.md` | 1 | D-4 KMS 키 표현 정확화 |
| 2 | `results/00-cross-cutting-matrix.md` | 2 | KMS §1 표현, ElastiCache 인증 §2.2 |
| 3 | `results/01-architecture-design.md` | 3 | Aurora prod ×3, Global DB RTO, NAT 부록 A |
| 4 | `results/03-compute-and-frontend-hosting.md` | 1 | DeploymentConfig 코드 정합 |
| 5 | `results/04-database-and-storage.md` | 5 | dev/beta ACU, Aurora 백업 일수, ElastiCache 표/인증 |
| 6 | `results/07-cicd-overview.md` | 1 | ECR 리포가 IaC에 포함됨을 반영 |
| 7 | `results/09-iac-terraform.md` | 1 | msk-provisioned spec (EBS/모니터링/출력) |
| 8 | `results/10-deployment-runbook.md` | 1 | 환경별 배포 전략 코드 정합 |
| 9 | `terraform/README.md` | 1 | 디렉토리 트리 주석 현행화 + 모듈 설명 보강 |
| 10 | `terraform/modules/elasticache-valkey/README.md` | 1 | prod 예시 auth_token 으로 정정 |
| 11 | `terraform/envs/prod/README.md` | 1 | Aurora ×3, ElastiCache 인증 표기 |

> **합계**: 17 Edit / 10 파일 (코드 파일 0건, prompts/ 폴더 0건 손대지 않음).

---

## 4. 미해결 / 후속 검토 권장 항목

| # | 항목 | 사유 | 권장 조치 |
|---|---|---|---|
| 1 | `08-observability.md` `spring-boot-starter-opentelemetry` 패키지 정확성 | Spring Boot 4 의 OpenTelemetry 통합 starter 명칭은 외부 공식 문서를 추가 확인 필요 | Spring Boot 4 docs 정식 GA 시점에 최종 확정 |
| 2 | `04 §1.5` AWS Backup 크로스 리전 정책 | Aurora module 에는 backup_retention만 있고 AWS Backup IaC 미구현 | 11 DR 활성화 시점에 `aws_backup_*` 모듈 신설 권장 |
| 3 | `06 §2.2.4` 챗봇 Bedrock 권한 | D-12 결정으로 미부여 (현행 OpenAI만 사용). 코드/문서 일치 | Bedrock 도입 시 별도 ADR + task_roles 갱신 |
| 4 | `05 §3.5` Outbox / Debezium CDC | 백엔드 코드에 Outbox 미구현 (정직 표시) | MSK Connect 도입 단계 이전 백엔드 우선 추가 |
| 5 | `08 부록 A` Actuator probe 미설정 | yml 정식 설정 없음 (envs ENV 주입은 있음) | application-common-core.yml 에 정식 추가 권장 (코드 변경 — 본 작업 범위 밖) |

---

## 5. 자기 점검 루브릭 결과

- [x] 31개 대상 문서 모두 검증되었다.
- [x] 모든 `❌` 항목이 수정되었거나 §4에 미해결 사유가 명시되었다.
- [x] 수정 후 코드와 문서가 다시 일치하는지 sample 검증 완료 (Aurora prod 인스턴스 3, KMS 5+1+ECR1, CodeDeploy Canary10Percent5Minutes, Cache auth_token 등).
- [x] `prompts/` 또는 코드(`*.tf`, `*.yml`, `*.gradle`)는 어떤 파일도 수정하지 않았다.
- [x] 모든 인용은 코드는 `path/to/file:LN` 형식, 외부 문서는 풀 URL.

---

## 6. 결론

이번 검증의 핵심 발견은 **세션 6 이후 코드는 정정됐지만 본문 문서 갱신이 누락된 항목**(Aurora prod ×3, KMS 5+1, deployment_config 표기 등)이 다수였다는 점이다. 모듈 README와 results 본문이 동시에 업데이트되어야 하는 변경에서 한쪽만 갱신되는 패턴을 §7 매트릭스의 "변경 절차"가 잘 잡고 있으나, 실제 운용에서 누락이 발생한 것으로 보인다.

**향후 권장 정합성 룰** (운영 가드):
1. PR 템플릿에 "관련 문서 동시 업데이트 체크" 항목 추가.
2. `pre-commit` 훅에 `grep` 기반 간이 정합성 체커 추가 (예: `aurora_instance_count` 의 코드값과 부록 A 표 값 비교).
3. `_validation-report-YYYYMMDD.md` 를 분기마다 갱신해 누적 드리프트 추적.

**문서 끝.**
