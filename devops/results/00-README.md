# AWS 배포 산출물 (Results) 인덱스

`/Users/m1/workspace/tech-n-ai/devops/prompts/` 의 11개 프롬프트를 순차 실행하여 생성한 결과물에 **세션별 정합성 작업**으로 분할·정리한 문서 집합입니다.

## 대상 시스템

- **백엔드**: Spring Boot 4.0.2 멀티모듈 — `api-gateway(8081)`, `api-emerging-tech(8082)`, `api-auth(8083)`, `api-chatbot(8084)`, `api-bookmark(8085)`, `api-agent(8086)` + 배치 `batch-source`
- **프론트엔드**: Next.js 16.1.6 × 2 앱 — `app`(3000 공개), `admin`(3001 내부)
- **데이터**: Aurora MySQL(JPA + Flyway) + MongoDB Atlas(Vector Search) + Kafka 동기화 + ElastiCache(Valkey)
- **컨테이너 이미지**: Paketo `bootBuildImage` (Dockerfile 미사용), **단일 ECR 리포 + 환경 무관 태그**
- **신규 구축**: 마이그레이션 대상 인프라 없음. 마케팅·실사용자 전, **최소 시드 환경** 우선.

## 문서 목록

| No. | 파일 | 핵심 결정 |
|-----|------|----------|
| **00-매트릭스** | [00-cross-cutting-matrix.md](00-cross-cutting-matrix.md) | **횡단 관심사 단일 정의 매트릭스** — KMS·IAM·SG·Secret·환경변수의 정의처와 참조처 |
| 01 | [01-architecture-design.md](01-architecture-design.md) | ECS Fargate + CodeDeploy Blue/Green, Amplify Hosting, Aurora Multi-AZ, MSK(prod Provisioned KRaft / dev·beta Serverless), DR 도쿄는 설계만 유지(미구축) |
| 02 | [02-network-vpc.md](02-network-vpc.md) | VPC CIDR 분리(`10.{10,20,30}.0.0/16`), 3 AZ, NAT GW AZ별, VPC Endpoint 풀세트, MongoDB Atlas PrivateLink, SSH 전면 금지 |
| 03 | [03-compute-and-frontend-hosting.md](03-compute-and-frontend-hosting.md) | `bootBuildImage` 유지(Corretto 21), ECS Fargate ARM64 Graviton, EventBridge Scheduler 배치, Amplify Hosting 프론트, **Actuator probes 활성화 필수** |
| 04 | [04-database-and-storage.md](04-database-and-storage.md) | Aurora MySQL Multi-AZ(prod만, dev/beta는 Serverless v2), MongoDB Atlas PrivateLink, ElastiCache Valkey, S3 OAC+SSE-KMS |
| 05 | [05-messaging-kafka.md](05-messaging-kafka.md) | MSK 하이브리드(prod Provisioned + Kafka 3.9.x KRaft 신규 구축 / dev·beta Serverless), IAM Access Control + TLS + KMS, 토픽 4종 신규 생성, Glue Schema Registry, Debezium CDC + Outbox |
| 06 | [06-security-and-iam.md](06-security-and-iam.md) | Organizations + Control Tower 계정 분리, IAM Identity Center + MFA, **GitHub Actions OIDC 4 Role 단일 정의**, Secrets Manager + **KMS CMK 6키**, WAF Managed Rules + Bot Control, GuardDuty 전 리전 |
| **07 (분할)** | [07-cicd-overview.md](07-cicd-overview.md) | CI/CD 공통 정책·품질 게이트 — GitHub Actions + OIDC, 단일 ECR 리포, Trunk-based |
| 07a | [07a-cicd-backend.md](07a-cicd-backend.md) | 백엔드 빌드·이미지·배포 — `bootBuildImage`, Notation+AWS Signer 1회 서명, **CodeDeploy Hook 람다 미사용**(ALB readiness + CW 알람 자동 롤백) |
| 07b | [07b-cicd-frontend.md](07b-cicd-frontend.md) | 프론트 빌드·배포 — Amplify Hosting, Lighthouse·번들 분석, SSM Parameter Store 환경변수 주입 |
| 07c | [07c-cicd-infra.md](07c-cicd-infra.md) | Terraform PR plan 코멘트 + apply 환경별 승인 + tfsec/Checkov/KICS/OPA 정책 게이트 + 주간 종합 보안 스캔 |
| 08 | [08-observability.md](08-observability.md) | OpenTelemetry 표준(ADOT Collector), 하이브리드(A: CloudWatch+X-Ray+App Signals → B: AMP+AMG 전환 가능), Fluent Bit + CloudWatch/OpenSearch, SLO Burn Rate 기반 Deploy Freeze 자동화 |
| 09 | [09-iac-terraform.md](09-iac-terraform.md) | Terraform ≥ 1.9, 10개 모듈(network/ecs-service/aurora-mysql/elasticache-valkey/msk-*/s3-bucket/iam-role-workload/cloudfront-spa/observability), S3 + DynamoDB Lock(KMS+Object Lock), pre-commit + tfsec + Checkov |
| 10 | [10-deployment-runbook.md](10-deployment-runbook.md) | 릴리즈 체크리스트, CodeDeploy Canary10Percent, Expand-Contract DB 마이그레이션, NIST SP 800-61 인시던트 플레이북 5종, PagerDuty 5단계 에스컬레이션, Break-glass 2인 승인 |
| 11 | [11-dr-and-cost-optimization.md](11-dr-and-cost-optimization.md) | 티어별 RTO/RPO(15m/1m · 1h/5m · 4h/1h), DR 패턴 매핑(설계만 유지, 도쿄 리전 미구축), AWS Backup Cross-Region+Vault Lock, Compute SP·Fargate Spot·Graviton·Aurora I/O-Optimized·S3 IT 레버 |

## 문서 간 종속 관계

실제 의존 방향(생성 시 참조 필요한 정의가 있는 문서를 위에 배치):

```
00-매트릭스 (횡단 정의 — 모든 문서가 참조)
    │
    ▼
01 (전체 아키텍처)
    ├─► 02 (네트워크 — SG·VPC Endpoint 단일 정의)
    ├─► 03 (컴퓨팅·프론트 — Task Def, Amplify env, Actuator probe 단일 정의)
    ├─► 04 (데이터 — Aurora·Mongo·Cache·S3)
    ├─► 05 (메시징 — MSK)
    ├─► 06 (보안 횡단 — KMS 6키·IAM Role·Secrets 단일 정의)
    └─► 08 (관측성 횡단 — 메트릭·로그·OTel)

07-overview ── 07a-backend, 07b-frontend, 07c-infra
   ▲
   └── 01·03·06 결정 반영, 09 모듈을 apply

09 (IaC) ◄── 02·03·04·05·06·08 코드화

10 (런북) ◄── 03·07a·08 운영 절차

11 (DR·비용) ◄── 04·05·09 기반 (도쿄 리전은 설계만)
```

## 주요 정합성 결정 (세션 1 적용)

| # | 결정 | 사유 | 영향 문서 |
|---|---|---|---|
| D-1 | **단일 ECR 리포** `techai/<module>` (환경 무관 태그 `{semver}-{sha}`) | Notation 서명 referrer가 원본 리포에 종속 → 환경별 리포 분리 시 SIGNATURE_NOT_FOUND. 단일 리포로 자연 해소 | 07a, 07c |
| D-2 | **CodeDeploy Hook 람다 미사용** | 추가 비용은 없으나 운영 표면(코드·권한·VPC·로깅) 증가. ALB readiness probe + CloudWatch 알람 자동 롤백으로 동등 안전망 가능 → 최소 세팅 정책에 부합 | 07a |
| D-3 | **이미지에 Spring 프로파일 미고정** | 단일 빌드 → 다중 환경 배포 원칙. 프로파일은 Task Definition `environment[]`로 주입 | 03, 07a |
| D-4 | **환경별 KMS CMK 5키**(`{env}-{auth/data/ai/logs/s3-app}`) **+ tfstate 공유 1키**(부트스트랩 정의, 모든 환경 공유) | 폭발 반경 최소화. 변경 시 합리적 사유 필요 | 매트릭스 §1, 06 |
| D-5 | **Tokyo DR 리전 — 설계만 유지, 미구축** | 마케팅 전 실사용자 0. 최소 세팅 정책 | 11 |
| D-6 | **신규 구축 전제** — KRaft 마이그레이션 / 토픽 리네이밍 절차 불필요 | 마이그레이션 대상 인프라 없음 | 05 |
| D-7 | Terraform 코드 위치 = `devops/terraform/` | 09 spec 일관성 | 09, 07c |
| D-8 | MSK Kafka **3.9.x KRaft** (3.8 → 변경) | 매트릭스 갱신 반영 | 05, 09, msk-provisioned |
| D-9 | Aurora 환경별 분기 — dev/beta Serverless v2(0.5~2/0.5~4 ACU), prod Provisioned `db.r7g.large` × 2 | 비용 최소화 + 04 §1 정합 | 04, aurora-mysql 모듈 |
| D-10 | ElastiCache 환경별 분기 — dev micro replica=0, beta/prod small replica=1 Multi-AZ | 실사용자 0 단계 비용 최소화 | 04, elasticache-valkey 모듈 |
| D-11 | dev 환경 데이터 모듈 토글 (`enable_aurora/elasticache/msk`). MSK는 default=false | 클러스터-시간 비용 회피 | envs/dev |
| D-12 | api-chatbot Task Role에 Bedrock 권한 미부여 | 매트릭스 갱신 반영 (코드 미도입, 별도 ADR) | 06, ECS Task Role (세션 2c) |

## 공통 준수 사항

- 외부 자료: **AWS 공식 문서 / 각 기술 공식 문서 / AWS Well-Architected / NIST / CIS / OWASP / Google SRE**에 한정
- 언어: 본문·코드 주석 한국어, 코드·명령·URL·식별자는 영어
- 환경: `dev`, `beta`, `prod` 3단계
- 리전: 프라이머리 `ap-northeast-2`(서울), DR `ap-northeast-1`(도쿄, **설계만**)
- 설계·런북·IaC 전반에서 현재 코드 사실을 반영:
  - Paketo `bootBuildImage` 사용, Dockerfile 없음
  - Actuator `liveness/readiness` probe **미설정 → 배포 전 활성화 필수** (03 §2.7, 08 §2)
  - 프론트 `output` 미설정, `.env` 전무, rewrite에 `localhost:8081` 하드코딩 → SSM Parameter Store 환경변수화 필요 (03 §3.4)
  - Kafka 토픽 4종 코드 하드코딩(`tech-n-ai.conversation.session/message.*`) → 신규 클러스터에 동일 이름으로 생성

## 산출물 규모

| 항목 | 값 |
|---|---|
| 문서 수 | 15 (00-매트릭스 신규 + 07 분할 +3) |
| 총 라인 수 | ~9,500+ |
| 디렉토리 | `/devops/results/` |

## 세션 작업 트랙

본 정합성 작업은 단계별 세션으로 진행되며, 각 세션은 검증 가능한 산출물을 생성한다.

| 세션 | 범위 | 상태 |
|---|---|---|
| **세션 1** | 분할 적절성·이해도 정합성 (매트릭스, 07 분할, README) | **완료** ✓ |
| **세션 2a** | 09 IaC 실코드화 — `devops/terraform/` bootstrap + network + iam + s3 + envs/dev 골격 | **완료** ✓ |
| **세션 2b** | 데이터 계층 모듈 — aurora-mysql(Multi-AZ + Serverless v2 분기), elasticache-valkey, msk-serverless, msk-provisioned(Kafka 3.9 KRaft) + envs/dev 통합 | **완료** ✓ |
| **세션 2c** | 워크로드·관측성 — ecs-service(워크로드 SG·CodeDeploy·자동 롤백 알람), cloudfront-spa, observability + envs/dev 풀 통합(6 Service + 6 Task Role + 데이터 SG cross-ref) + envs/{beta,prod} 메타 골격 | **완료** ✓ |
| **세션 4** | 08 관측성 실설정(ADOT Collector YAML + FireLens config + PII 마스킹 Lua) + 11 비용 추정 표 (환경별 USD) | **완료** ✓ |
| **세션 5** | envs 변수 통합화 + envs/{beta,prod} 본문 완성(dev 동일 .tf + tfvars) + Secrets Manager stub(jwt 듀얼 키 등) | **완료** ✓ |
| **세션 6** | ECR 리포 IaC 7개 (단일 리포 D-1 정합) + Aurora prod instance_count 정정(2→3) + 10 런북 IaC 롤백 Quick Reference (1쪽 요약) | **완료** ✓ |
| **세션 7** | ecs-service 모듈 sidecar 자동 옵션(ADOT/FireLens, OTel 환경변수 자동 주입) + modules/amplify-app(Next.js 16 SSR WEB_COMPUTE) + envs/dev frontend.tf(app·admin × Amplify, enable_amplify 토글) + envs/{beta,prod} 동기화 | **완료** ✓ |
