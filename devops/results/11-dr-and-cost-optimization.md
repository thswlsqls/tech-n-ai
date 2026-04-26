# 11. 재해복구(DR) · 백업 · 비용 최적화

> **문서 목적**: Tech-N-AI 플랫폼(`ap-northeast-2` 프라이머리)의 재해복구 전략과 FinOps 거버넌스를 정의한다. AWS Well-Architected Reliability Pillar 및 Cost Optimization Pillar에 부합하며, 실제 훈련으로 검증 가능한 수준으로 기술한다.
>
> **선행 문서**: 01 아키텍처(리전/환경), 04 DB/스토리지, 05 Kafka/MSK, 09 IaC 태그 표준, 10 배포 런북
>
> **작성 원칙**: (1) 한국어 본문 / 영문 기술용어 병기, (2) 공식 출처만 인용, (3) 비용은 AWS Pricing Calculator 산출식 기반 비교값, (4) 태그 표준 100% 준수.

---

## Part A. 재해복구 및 백업

> **현재 단계 정합성 알림 (D-5, 00-README §주요 정합성 결정)**: 본 Part A의 DR 설계(Tokyo 리전 Warm Standby/Pilot Light, Aurora Global Database, MirrorMaker 2 등)는 **목표 설계(target architecture)이며 현재는 미구축 상태로 유지**된다. 마케팅 전 실사용자 0 단계의 최소 세팅 정책에 따라 결정되었으며, 실사용자 진입 또는 SLA 승격 시점에 별도 ADR로 활성화한다. 본 절의 표·플로우·런북은 활성화 시 즉시 적용 가능한 형태로 보존한다.

### 1. DR 전략 (dr-strategy)

#### 1.1 서비스 티어 분류와 RTO/RPO 목표

서비스를 비즈니스 영향도(Business Impact)에 따라 3개 티어로 분류하고, 각 티어별로 복구 목표(Recovery Time Objective / Recovery Point Objective)를 정의한다.

| 티어 | 대상 서비스 | 비즈니스 영향 | RTO | RPO | 근거 |
|---|---|---|---|---|---|
| **Tier 0** | `api-auth`(8083), `api-gateway`(8081) 중 토큰 검증 경로, Aurora `auth_db`, ElastiCache(세션) | 전 사용자 로그인·인증 차단 → 전 서비스 마비 | ≤ **15분** | ≤ **1분** | 로그인 실패는 모든 후속 API 호출 실패로 파급. RPO 1분 = Aurora Global DB 평균 복제 지연 < 1s에 안전 마진 부여 |
| **Tier 1** | `api-emerging-tech`(8082), `api-chatbot`(8084), `api-bookmark`(8085), `api-agent`(8086), MongoDB Atlas(read model), MSK(prod Provisioned) | 핵심 사용자 기능(기술 탐색/챗봇/북마크) 중단 | ≤ **1시간** | ≤ **5분** | 사용자 체감 SLA 99.9% 월 허용 다운타임(43.8분) 내 복구. RPO 5분 = Kafka replay + Mongo 백업 주기 |
| **Tier 2** | Admin 앱(Amplify), Spring Batch(6시간 주기), 내부 대시보드, CUR/로그 아카이브 | 내부 운영자만 영향, 외부 서비스 무관 | ≤ **4시간** | ≤ **1시간** | 배치는 다음 실행 주기에 보정 가능. Admin은 외부 SLA 없음 |

> 환경별 적용: `prod`는 위 목표를 100% 준수, `beta`는 Tier 1/2 완화(RTO 4h/RPO 30m), `dev`는 DR 면제(Backup & Restore만).

#### 1.2 DR 패턴 비교

AWS Disaster Recovery of Workloads 백서의 4패턴을 티어에 매핑한다.

| 패턴 | RTO | RPO | 월 비용(2nd 리전) | 복잡도 | 설명 |
|---|---|---|---|---|---|
| **Backup & Restore** | 시간~일 | 시간~일 | 프라이머리의 **5~10%** | 낮음 | 스냅샷/백업만 크로스 리전 복사. 복구 시 리소스 프로비저닝부터 수행 |
| **Pilot Light** | 10분~1시간 | 분 단위 | 프라이머리의 **15~25%** | 중 | 데이터 계층(Aurora Replica, S3 Replication)만 상시 가동. 컴퓨팅은 off/minimal, 장애 시 스케일아웃 |
| **Warm Standby** | 분 단위 | 초~분 | 프라이머리의 **40~60%** | 중상 | 축소된 완전한 환경(1 task, 1 reader) 상시 가동. 스케일업으로 전환 |
| **Multi-site Active-Active** | 0~수초 | 0(동기) ~ 수초(비동기) | 프라이머리의 **90~110%** | 매우 높음 | 양쪽 리전이 모두 트래픽 처리. Global Accelerator/ARC 필수 |

#### 1.3 티어별 DR 패턴 선정

| 티어 | 선정 패턴 | 구성 |
|---|---|---|
| **Tier 0 (인증)** | **Warm Standby** | Aurora Global Database(Seoul writer + Tokyo secondary cluster, **typical cross-region replication lag < 1 s — Aurora Global DB User Guide / managed planned switchover 시 RPO=0**), ECS `api-auth` DR에 `desired_count=1`(최소 1 AZ) 상시, ALB DR 리전 상시, ElastiCache Valkey Global Datastore, Route 53 ARC Routing Control로 수동 승격 |
| **Tier 1 (핵심 API)** | **Pilot Light** | Aurora Global DB secondary(읽기 전용, failover 시 promote), MongoDB Atlas multi-region(M30+, Continuous Cloud Backup), MSK는 MirrorMaker 2로 DR 리전에 비동기 복제. ECS 서비스는 Task Definition만 배포된 상태(`desired_count=0`) → 장애 시 `desired_count=N`으로 스케일업 |
| **Tier 2 (admin/배치)** | **Backup & Restore** | AWS Backup의 Cross-Region Copy에 의존. 장애 시 Terraform으로 DR 리전에 재프로비저닝 후 복원 |

#### 1.4 DR 리전 선정 근거

프라이머리 `ap-northeast-2`(서울) 기준 DR 후보 비교(AWS Regional Services List 및 공식 Pricing 기준).

| 항목 | `ap-northeast-1` (도쿄) | `ap-southeast-1` (싱가포르) |
|---|---|---|
| 서울↔DR RTT | **~30ms** | ~75ms |
| 지리적 독립성 | 별도 판(일본 판), 다른 지진 리스크 프로파일 | 별도 대륙, 독립성 최상 |
| Aurora Global DB 지원 | 지원(서울 → 도쿄 검증된 경로) | 지원 |
| MSK / ECS / Amplify 지원 | 전 서비스 지원 | 전 서비스 지원 |
| Data Transfer Out 요금(프라이머리 → DR, per GB) | inter-region 출구 요금(서울 → Asia Pacific). **단가는 AWS EC2 On-Demand Pricing — Data Transfer 페이지에서 발표 시점 기준 확인 필수** (2025 기준 도쿄가 싱가포르보다 통상 소폭 저렴) | 동일 페이지에서 확인 |
| 운영팀 타임존(KST 기준) | **동일(UTC+9)** | UTC+8 (1시간 차) |
| Compliance(개인정보 국외이전) | 일본 PIPA, 한국 방통위 적정성 인정 절차 경험 다수 | 싱가포르 PDPA, 별도 SCC 필요 |

**결정**: **`ap-northeast-1` (도쿄)** 선정.
- Aurora Global DB 복제 지연 최소화(RTT 30ms)
- 운영팀 주간 근무 시간 겹침으로 유사시 의사결정 신속
- 크로스리전 데이터 전송 비용이 싱가포르 대비 통상 소폭 저렴 (정확한 단가는 AWS Pricing 페이지 확인)
- 한-일 간 개인정보 이전 법무 선례 축적

#### 1.5 트래픽 전환 설계

**Route 53** (DNS 기반) + **Application Recovery Controller(ARC)** (결정적 장애 조치)를 조합한다.

```text
[User] → api.techn-ai.com (Route 53)
         │
         ├─ Primary: ALB (ap-northeast-2)  ── Health Check (HTTPS /actuator/health)
         └─ Secondary: ALB (ap-northeast-1) ── Failover Routing Policy
                                              └─ ARC Routing Control (수동 승격 스위치)
```

- **Health Check**: ALB Target Group의 `/actuator/health` 엔드포인트를 Route 53 Health Check로 30초 간격 모니터링. 연속 3회 실패 시 unhealthy.
- **Failover Policy**: Primary-Secondary. Primary unhealthy 시 자동 Secondary로 DNS 응답 전환(TTL 60s).
- **ARC Routing Control**: Tier 0 인증 서비스에 한해 자동 failover 대신 **수동 승격** 채택. 이유는 Split-brain 및 Aurora unplanned failover 중 write 충돌 방지. On-call이 ARC 콘솔/CLI로 `ON/OFF` 스위치를 조작(latency 수초).
- **ARC Readiness Check**: Capacity/Configuration/Routing 3 차원 정기 점검으로 DR 리전 실제 수용 가능 여부 상시 검증.
- **Aurora Global DB Failover**: Managed planned failover(훈련용)와 Unplanned cross-Region failover(장애 시) 모두 Runbook화.

출처: AWS Well-Architected Reliability Pillar — REL13 BP Plan for DR / AWS Route 53 ARC Developer Guide.

---

### 2. 백업 플랜 (backup-plan)

#### 2.1 백업 중앙화 아키텍처

```text
[Source Accounts: dev / beta / prod]
   │  AWS Backup (per-account)
   │    ├─ Local Vault (ap-northeast-2)
   │    ├─ Cross-Region Copy → ap-northeast-1 Vault
   │    └─ Cross-Account Copy ↓
[Backup Account (Organization member)]
   Backup Vault (locked, immutable)
     ├─ Seoul Vault (primary copy)
     └─ Tokyo Vault (DR copy)
```

- 조직(AWS Organizations) 수준 **Backup Policy**로 전 계정에 백업 플랜을 강제.
- Backup 전용 계정에 Copy Destination Vault를 두어 **Source 계정 침해 시에도 백업이 보호**되는 구조(Ransomware Protection).

#### 2.2 백업 대상 및 플랜

| 리소스 | 백업 방식 | 일일 | 주간 | 월간 | 보존 | Cross-Region | Vault Lock |
|---|---|---|---|---|---|---|---|
| **Aurora MySQL (prod)** | AWS Backup(Continuous + Snapshot) | 매일 03:00 KST | 일요일 | 1일 00:00 | 일일 35일 / 주간 12주 / 월간 12개월 | ON (Tokyo) | Compliance mode, 12개월 |
| **Aurora MySQL (beta/dev)** | Snapshot | 매일 03:00 | - | - | 7일 / 3일 | OFF | Governance mode |
| **EBS (ECS ephemeral 제외, Batch work vol)** | AWS Backup | 매일 04:00 | 일요일 | - | 7일 / 4주 | ON(prod) | Governance |
| **S3 (원본 업로드, 문서 RAG 소스)** | S3 Versioning + Replication + AWS Backup for S3 | 지속 | - | 월 1회 스냅샷 | 버전 90일 + 스냅샷 12개월 | ON (CRR to Tokyo) | Compliance(프로덕션) |
| **MongoDB Atlas** | Atlas Continuous Cloud Backup | PITR 72h | 일요일 스냅샷 | 월간 스냅샷 | PITR 72h / 주간 4주 / 월간 12개월 | Atlas Multi-Region Backup(Tokyo) | Atlas Backup Compliance Policy |
| **MSK (prod)** | Topic 스키마는 Git, Replay는 Source(DB)에서 재생성. 장애 데이터만 MirrorMaker 2로 Tokyo MSK 복제 | 지속(스트림) | - | - | 보존 정책 = topic retention | N/A (MM2 실시간) | N/A |
| **CUR, CloudTrail, Config** | S3 → Glacier Deep Archive | 지속 | - | - | **7년**(규제/감사) | ON | Compliance 7년 |

> EFS/FSx/DynamoDB는 현 아키텍처에서 미사용이나, 향후 도입 시 AWS Backup 계획에 동일 패턴으로 추가.

#### 2.3 Cross-Region Copy / Cross-Account Copy

- AWS Backup **Copy Action**을 단일 플랜 내에서 `destinationBackupVaultArn`으로 두 단계 복사:
  1. Source Vault(ap-northeast-2) → Backup 계정 Seoul Vault
  2. Backup 계정 Seoul Vault → Backup 계정 Tokyo Vault
- KMS 키는 리전별로 Customer Managed Key(CMK)를 사용하고 Backup 계정 IAM이 `kms:CreateGrant` 권한 보유.

#### 2.4 Vault Lock (WORM)

- **Compliance mode** (루트 계정도 해제 불가) → prod의 Aurora/S3/CUR Vault에 적용. `minRetentionDays=35`, `maxRetentionDays=2555`(7년), Grace Period 3일.
- **Governance mode** (특정 IAM만 해제 가능) → 비프로덕션 Vault.
- Vault Lock은 설정 후 **72시간 Cooling-off** 동안만 취소 가능하므로, 프로덕션 적용 전 dev 계정에서 리허설 필수.

#### 2.5 복원 훈련 (Restore Validation)

**분기 1회 이상** 복원을 실제로 수행하여 백업 유효성 검증.

| 주기 | 대상 | 훈련 방법 | 성공 기준 |
|---|---|---|---|
| 분기 1회 | Aurora(prod 스냅샷) | 샌드박스 계정에 Restore → 핵심 쿼리(auth 로그인, user 조회) 실행 | 10분 내 restore 시작, 1시간 내 query OK |
| 분기 1회 | S3(임의 prefix) | Cross-Region Vault에서 새 버킷으로 복원 | 체크섬 일치율 100% |
| 반기 1회 | MongoDB Atlas | Atlas Point-in-Time Restore(48시간 전 시점) | 컬렉션 doc count ±0.1% |
| 연 1회 | 전체 티어 0 | DR 리전으로 풀 복구 후 Route 53 ARC 스위치 | RTO/RPO 목표 달성 |

출처: AWS Backup Developer Guide — Restore testing plans / Cross-Region and Cross-Account backup copy.

---

### 3. DR 테스트 플랜 (dr-test-plan)

#### 3.1 원칙

- **연 1회 이상 실제 장애 주입** (Chaos Engineering). AWS **Fault Injection Service(FIS, 구 FIS Simulator)** 사용.
- 훈련 전 **Blast Radius 축소**: staging/beta 환경에서 검증 후 prod 적용.
- 모든 훈련은 **GameDay** 형식으로 On-call, SRE, Dev, Product Owner 합동 참여.

#### 3.2 시나리오

| # | 시나리오 | 도구 / 액션 | 목표 검증 |
|---|---|---|---|
| 1 | **AZ 장애** | FIS `aws:ec2:stop-instances` with AZ filter, ALB health check | ECS Task가 다른 AZ로 재배치, p99 latency 영향 ≤ 10% |
| 2 | **리전 장애(부분)** | FIS 네트워크 차단(`aws:network:disrupt-connectivity`) + Route 53 Failover | Tier 0 RTO 15분 달성, Aurora Global DB unplanned failover |
| 3 | **DB 장애** | FIS `aws:rds:reboot-db-instances` / `aws:rds:failover-db-cluster` | ECS connection pool 복구, backoff retry 동작, RPO ≤ 1분 |
| 4 | **네트워크 분할 (NAT/TGW)** | FIS `aws:network:disrupt-connectivity` on subnet | VPC Endpoint 경유 경로 fallback, OpenAI 외부 호출 Circuit Breaker 발동 |
| 5 | **ElastiCache 장애** | FIS `aws:elasticache:replicationgroup-interrupt-az-power` | 세션 재발급, auth fallback 동작 |
| 6 | **MSK Broker 장애** | FIS custom(SSM Document) | Producer 재시도, Consumer lag 복구 시간 측정 |
| 7 | **Amplify 빌드 장애** | 수동(실패 배포) | 롤백 Runbook(10번 문서) 유효성 |

#### 3.3 평가 지표

| 지표 | 측정 방법 | 합격선 |
|---|---|---|
| 실측 RTO | 장애 주입 시각 ~ 서비스 정상화(Synthetic Monitoring 200 OK 연속 3회) | 티어별 목표 100% |
| 실측 RPO | 장애 직전 마지막 write timestamp ~ 복구 후 최신 read timestamp | 티어별 목표 100% |
| 자동화 커버리지 | 자동 수행 단계 수 / 전체 Runbook 단계 수 | ≥ 70% |
| 사용자 영향도 | 5xx 에러율, CloudFront/ALB 4xx 증가, p99 latency | 각 지표 baseline +20% 이내 |
| MTTD(감지 시간) | 장애 주입 시각 ~ PagerDuty 알람 발생 | ≤ 5분 |
| Runbook Drift | 실제 대응과 Runbook 차이 항목 수 | ≤ 2개 |

#### 3.4 결과 리포트 템플릿

```markdown
# DR Drill Report — YYYY-QN / Scenario #X

## 1. 개요
- 일시: YYYY-MM-DD HH:MM ~ HH:MM KST
- 시나리오: (예: AZ 장애)
- 참여자: SRE(), On-call(), Dev(), PO()
- Blast Radius: (beta/prod)

## 2. 타임라인
| 시각(KST) | 이벤트 | 담당 | 비고 |
| HH:MM | FIS experiment 시작 | SRE | |
| HH:MM | CloudWatch Alarm 발화 | 자동 | MTTD: N분 |
| HH:MM | On-call 대응 개시 | On-call | |
| HH:MM | Failover 완료 | SRE | |
| HH:MM | 서비스 정상화 확인 | QA | |

## 3. RTO/RPO 측정
- 목표 RTO: 15분 / 실측: N분 → (PASS/FAIL)
- 목표 RPO: 1분 / 실측: N초 → (PASS/FAIL)

## 4. 발견 사항
- (예: Runbook의 Step 3이 매뉴얼 개입 필요, 자동화 개선 후보)

## 5. Action Items
| # | 항목 | 담당 | 기한 | JIRA |
| 1 | ... | | | |

## 6. 아티팩트
- FIS Experiment ARN:
- CloudWatch Dashboard 스냅샷:
- 로그 쿼리(Insights):
```

출처: AWS Well-Architected Reliability Pillar — REL13 / AWS Fault Injection Service User Guide.

---

## Part B. 비용 최적화 (FinOps)

### 4. 비용 거버넌스 (cost-governance)

#### 4.0 환경별 월간 추정 비용 (시드 가정)

> ⚠️ **본 추정의 가정**: 실사용자 0(마케팅 전), 24/7 가동, 단일 리전(서울), Tokyo DR 미구축(D-5), 외부 API 호출 비용(OpenAI 등) 제외. 단가는 **2026-04 기준 서울 리전 공시가**. 실 청구는 트래픽·데이터 전송에 따라 ±20%.

**가정 시나리오**:

| 항목 | dev | beta | prod |
|---|---|---|---|
| 백엔드 평균 RPS | 0 (테스트) | 5 | 50 |
| ECS Task 수 (Service당) | 1 | 1~2 | 2~3 |
| 데이터 (Aurora 저장) | <1 GB | <10 GB | <100 GB |
| MSK 일 처리량 | — | <0.1 MB/s | <1 MB/s |
| Auto-shutdown | 미적용 (기본) | 미적용 (기본) | 적용 불가 |

**환경별 합산 추정** (USD/월):

| 컴포넌트 | 단가 출처 | dev | beta | prod |
|---|---|---|---|---|
| **네트워크 — NAT Gateway** | $0.062/h × hours | $45 (1×) | $45 (1×) | **$135 (3×)** |
| **네트워크 — VPC Endpoint Interface** | $0.01/h × ENI × 3 AZ × 9 endpoint | $194 | $194 | $194 |
| **ALB** | $0.0252/h + LCU | $18 | $18 | $25 |
| **ECS Fargate** (ARM64 Graviton) | vCPU $0.04045/h, Mem $0.00444/GB-h | $115 (6×1 task) | $130 (6×1.5) | $230 (6×2) |
| **Aurora MySQL** | Serverless v2 $0.12/ACU-h, `db.r7g.large` $0.21/h, I/O-Optimized +30% | $43 (0.5 ACU) | $86 (1 ACU) | **$423 (2× r7g.large I/O-Opt)** |
| **ElastiCache Valkey** | t4g.micro $0.022/h, t4g.small $0.043/h | $16 (micro×1) | $62 (small×2) | $62 (small×2) |
| **MSK** | Provisioned: $0.21/h × broker + EBS $0.114/GB-월. Serverless: $0.75/h cluster-h + 파티션·I/O | **$0** (비활성) | $540 (Serverless) | **$624** (3× m7g.large + 1.5TB EBS) |
| **CloudWatch** (Logs ingestion + Alarms 24개) | Logs $0.50/GB, Alarm $0.10/each | $4 | $7 | $15 |
| **KMS CMK** | $1/key/월 | $5 (5 keys) | $5 | $6 (6 keys) |
| **Secrets Manager** | $0.40/secret + API call | $2 (5 secrets) | $2 | $2 |
| **S3 (저용량)** | $0.025/GB-월 | <$1 | <$1 | <$2 |
| **Observability sidecar** (ADOT + FireLens) | Fargate vCPU·Mem 추가 | $14 (6×1) | $14 (6×1.5) | $28 (6×2) |
| **WAF** | $5 + $1×rule | — | — | $15 |
| **GuardDuty + Inspector + Config** | 사용량 비례 | <$5 | <$8 | $15~25 |
| **CloudFront + Route53** | 트래픽 비례 + $0.50/zone | $0 (미적용) | $0 (미적용) | $5 |
| **합계 (raw)** | | **~$461/월** | **~$1,111/월** | **~$1,781/월** |

**비용 절감 옵션 적용 후**:

| 적용 | dev | beta | prod |
|---|---|---|---|
| VPC Endpoint Interface 비활성 (NAT 폴백) | -$194 | -$194 | (적용 안 함) |
| MSK Serverless 비활성 | (이미 비활성) | -$540 | (적용 안 함) |
| Aurora 0.5 ACU baseline auto-pause(불가, 항상 0.5) | — | — | — |
| Compute Savings Plan 1년 No-Upfront 70% 커버리지 (ECS) | — | — | -$48 |
| Aurora Reserved Instance 1년 No-Upfront 30% off | — | — | -$127 |
| **절감 후 합계** | **~$267** | **~$377** | **~$1,606** |

**시드 단계 권장 (실사용자 0 단계)**:
- dev: `enable_vpc_endpoints=false`, `enable_msk=false` 유지 → **약 $267/월**
- beta: `enable_msk=false` 유지, 통합 테스트 시점에 임시 활성 후 즉시 비활성 → **약 $377/월**
- prod: 트래픽 본격 발생 전까지 ECS desired_count=1, Aurora 1× r7g.large(Multi-AZ 유지)로 절반 → **약 $900~1,100/월** (런칭 후 실측 트래픽 기반 SP 약정)

**VPC Endpoint Interface 비용 주의**: 9개 × 3 AZ × $0.01/h = $194/월. 트래픽이 적은 시드 환경에서는 NAT 경유로 폴백이 더 저렴. `network` 모듈의 `enable_vpc_endpoints=false` 로 즉시 절감 가능.

**MSK 비용 주의**: Serverless 클러스터-시간(시드 환경 트래픽 0이어도 $540/월), Provisioned brokers(서울 m7g.large × 3 = $453/월 + EBS $171). MVP 기간 비활성 또는 SQS·EventBridge 대안 검토.

출처:
- AWS Pricing — <https://aws.amazon.com/pricing/>
- ECS Fargate Pricing (Seoul) — <https://aws.amazon.com/fargate/pricing/>
- RDS Aurora Pricing (Seoul) — <https://aws.amazon.com/rds/aurora/pricing/>
- MSK Pricing — <https://aws.amazon.com/msk/pricing/>
- VPC Endpoint Pricing — <https://aws.amazon.com/privatelink/pricing/>
- AWS Pricing Calculator — <https://calculator.aws/>

---

#### 4.1 가시성 스택

```text
[모든 계정 사용량]
   │ CUR 2.0 (hourly) → Org Management 계정 S3 버킷
   │                      │
   │                      ├─ Athena (Glue Crawler로 파티션 자동 등록)
   │                      └─ QuickSight 대시보드(환경별/서비스별/태그별)
   │
   ├─ Cost Explorer (최근 13개월, UI 탐색)
   ├─ AWS Budgets (예산/알림)
   └─ Cost Anomaly Detection (ML 기반 이상 탐지)
```

- **CUR 2.0** (Cost and Usage Report v2): Parquet, hourly, resource ID 포함 활성화.
- **Cost Categories**: `Environment`, `Workload`(backend/frontend/data/shared), `Tier`(0/1/2)로 Cost Explorer 내 가상 그룹핑.
- QuickSight 대시보드 3종: **Executive**(월별 트렌드/환경별/예산 대비), **Engineering**(서비스별 일별/태그 누락 리포트), **FinOps**(커버리지/SP 사용률/Anomaly 이력).

#### 4.2 태깅 표준 (09 문서 정합)

| 태그 키 | 필수 | 값 예시 | 비용 할당 키 활성화 |
|---|---|---|---|
| `Project` | Y | `tech-n-ai` | Y |
| `Environment` | Y | `dev`/`beta`/`prod` | Y |
| `Owner` | Y | `team-platform@techn-ai.com` | Y |
| `CostCenter` | Y | `CC-1001`(Engineering) | Y |
| `DataClassification` | Y | `public`/`internal`/`confidential`/`restricted` | N |
| `ManagedBy` | Y | `terraform` | N |
| `Service`(모듈별) | Y | `api-auth`/`api-gateway`/... | Y |
| `Tier` | 권장 | `tier-0`/`tier-1`/`tier-2` | Y |

- **Cost Allocation Tag 활성화**: Billing Console에서 위 표시된 키를 활성화(활성화 이후 발생 비용만 집계됨에 유의).
- **Terraform 강제**: 모듈 공통 `default_tags`와 Sentinel/OPA 정책으로 누락 차단.
- **주간 태그 미준수 리포트**: Athena 쿼리로 `tags IS NULL` 비율 ≥ 1% 시 Slack 알람.

#### 4.3 예산(Budgets) 및 알림

| Budget 이름 | 범위 | 월 예산 (시드, 4.0 추정 기반) | 알림 임계치 |
|---|---|---|---|
| `budget-prod-total` | Env=prod 전체 | **$2,000** (절감 후 $1,606 + 마진 25%) | 80% 실적치 / 100% 예측치 / 120% 실적치 |
| `budget-beta-total` | Env=beta | **$500** (MSK 비활성 가정 $377 + 마진) | 80/100/120% |
| `budget-dev-total` | Env=dev | **$300** (시드 권장 $267 + 마진) | 80/100% |
| `budget-prod-aurora` | Service=aurora, Env=prod | **$500** (Reserved 미적용 $423, 적용 시 $296) | 80/100/120% |
| `budget-prod-ecs` | Service=ecs, Env=prod | **$300** (Fargate $230 + sidecar $28 + SP 마진) | 80/100/120% |
| `budget-prod-openai-via-cohere` | Service=ai-external | **$200** (시드, 사용 시작 후 즉시 재산정) | 80/100/120% |
| `budget-data-transfer` | Usage Type Group: data-transfer | **$100** (시드 트래픽 가정) | 80/100% |

**예산 재산정 트리거**: 트래픽 본격 발생, MSK 활성화, OpenAI 호출량 변화. 매 분기 1회 정기 재산정 + 임시 변경 시 즉시.

- 알림 채널: Email(Owner) + SNS → Slack(#finops). 120% 초과 시 On-call PagerDuty.
- **AWS Budgets Actions**로 dev 환경 100% 초과 시 IAM Deny 정책 자동 적용(선택적, 안전장치).

#### 4.4 Cost Anomaly Detection

- Monitor 타입:
  1. **AWS services**(전 서비스 대상)
  2. **Linked account** 단위(dev/beta/prod 각각)
  3. **Cost Category**(Workload별)
- Subscription 임계치: 절대값 **$200** 또는 이전 대비 +20% 중 작은 값.
- 알림: Slack + 이메일 즉시, 3회 연속 발생 시 FinOps 주간 회의 안건 자동 등록.

#### 4.5 Showback / Chargeback

- **1단계(Showback)**: CostCenter 태그 기반으로 팀별 월 사용량 리포트(QuickSight)를 매월 5일에 회람. 실제 과금 이동 없음.
- **2단계(Chargeback, 조직 확장 후 도입)**: Engineering 공통 리소스(VPC, TGW, NAT)는 트래픽 비중(VPC Flow Logs) 또는 정액 배분. Workload 전용 리소스는 태그 기반 직접 배분.
- 산출식(예시, 공유 NAT Gateway):
  ```text
  팀_i의_NAT_월할당 = NAT_총비용 × (팀_i_ENI_outBytes / 전체_ENI_outBytes)
  ```

출처: AWS Cost Management — Budgets / Cost Anomaly Detection / CUR. FinOps Foundation Framework — Inform / Optimize / Operate.

---

### 5. 최적화 수단 (cost-optimization-levers)

각 레버(Lever)는 **예상 절감**, **측정 방법**, **롤백 기준**을 명시한다. 절감률은 AWS Pricing Calculator 및 공식 문서 기반 범위값이다.

#### 5.1 컴퓨팅

| 레버 | 대상 | 예상 절감 | 측정 방법 | 롤백 기준 |
|---|---|---|---|---|
| **Compute Savings Plans (1y, No Upfront)** | ECS Fargate on-demand 기본 사용량 | 공식 표기 최대 **up to 66%**(EC2 포함, 3y All Upfront 기준) — Fargate 단독 정확 수치는 미공개, AWS Pricing Calculator로 워크로드별 산출 | CUR의 `line_item_usage_type` 대비 `savings_plan_effective_cost` 비교 | 커버리지 목표(70~80%) 범위 벗어남 지속 2주 |
| **Fargate Spot (배치만)** | Spring Batch ECS Task (Tier 2) | 공식 최대 **~70%** off (Fargate Spot) | 배치 완료율 및 중단(reclaim) 횟수 | 배치 실패율 ≥ 5% 또는 SLA(6시간 주기) 위반 |
| **Graviton(ARM64) 전환** | ECS Fargate Task CPU=ARM64, Aurora `db.r7g.*`, ElastiCache `cache.r7g.*` | 공식 **~20% 성능/가격 개선** | Before/After 동일 트래픽 p95 latency, 비용/요청 | 특정 라이브러리(JNI 등) 호환 실패, p95 latency +10% |
| **ECS desired_count 스케줄** | dev/beta API 서비스 | 야간/주말 OFF → 약 **50~65%** (주 168h 중 65h만 가동 가정) | EventBridge Scheduler + CloudWatch 지표 | 긴급 작업 시 수동 override 필요 시 ≥ 월 3회 |

- Graviton 적용은 **Spring Boot 4 + JDK 21**이 AArch64 공식 지원이므로 호환성 리스크 낮음. 네이티브 이미지 기반 의존성만 확인 필요.
- Fargate Spot은 **장기 실행 배치에 체크포인트 필수** (Spring Batch JobRepository 재시작 가능 설정).

#### 5.2 데이터베이스

| 레버 | 대상 | 예상 절감 | 측정 방법 | 롤백 기준 |
|---|---|---|---|---|
| **Aurora I/O-Optimized vs Standard** | prod Aurora 클러스터 | I/O 비용이 총 Aurora 비용의 **25% 이상**이면 I/O-Optimized가 유리(공식 문서) | CUR에서 `Aurora:StorageIOUsage` 비중 월 추적 | I/O 비중 25% 미만으로 2개월 연속 → Standard 복귀 |
| **Aurora Reader 수 조정** | prod 2 readers → Auto Scaling 1~3 | CPU p95 < 40%일 때 1대 축소 시 reader 단가 **~33%** 절감 | Reader CPU p95, ReplicaLag, ReadIOPS | Reader CPU p95 > 70% 또는 ReplicaLag > 5초 |
| **Aurora Serverless v2 ACU 하한** | dev/beta writer | 하한 0.5 ACU 적용 시 상시 유휴 대비 **40~60%** 절감 | CloudWatch `ServerlessDatabaseCapacity` | Cold start로 인한 첫 쿼리 p99 > 3s 지속 |
| **MongoDB Atlas Cluster Tier 적정화** | M30 → M20(dev/beta) | Atlas 티어 차이 기준 ~40% | Atlas Metrics(OpCounters, Connections, Memory) | OpLag > 10s 또는 Connection Pool 포화 |

출처: Amazon Aurora I/O-Optimized pricing docs.

#### 5.3 스토리지

| 레버 | 대상 | 예상 절감 | 측정 방법 | 롤백 기준 |
|---|---|---|---|---|
| **S3 Intelligent-Tiering** | RAG 원본 문서, 사용자 업로드 | 자동 티어링 단계별(Standard 대비): Infrequent Access(30일+ 미접근) **~40%**, Archive Instant Access(90일+) **~68%**, Archive Access(90일+ 옵트인) **~71%**, Deep Archive Access(180일+ 옵트인) **~95%** | S3 Storage Lens의 티어별 분포 | Archive/Deep Archive는 분~시간 단위 복원 지연 → 핫 경로 객체 제외 필수 |
| **S3 Lifecycle → Glacier Deep Archive** | 감사 로그, CUR, CloudTrail | **~75%** 대비 Standard | CUR 스토리지 라인 비용 | 180일 내 조회 요청 빈도 ≥ 월 3회 |
| **EBS gp3 전환 + IOPS/Throughput 분리 과금** | Batch work volume | 공식 gp2 대비 최대 **~20%** | EBS 모니터링 VolumeThroughputPercentage | IOPS 상한 소진 지속 |
| **미사용 스냅샷/AMI 정리** | 전 계정 | 변동 | AWS Backup 보존 정책 외 수동 스냅샷 주간 리포트 | N/A (미사용 확정분만 삭제) |

#### 5.4 네트워크

| 레버 | 대상 | 예상 절감 | 측정 방법 | 롤백 기준 |
|---|---|---|---|---|
| **VPC Gateway Endpoint (S3, DynamoDB)** | 전 VPC | S3 트래픽의 **NAT 요금($0.045/GB) 및 data processing 요금 제거** | VPC Flow Logs의 NAT 경유 S3 트래픽 급감 | N/A (안전) |
| **VPC Interface Endpoint (ECR, Secrets Manager, Logs, STS, KMS)** | prod/beta | NAT 트래픽 감소. Endpoint 시간당 $0.01×AZ + $0.01/GB로 월 수백 GB 이상일 때 손익분기 | NAT Gateway `BytesOutToDestination` 감소 | 월 트래픽 < 100GB로 Endpoint 고정비가 더 클 때 |
| **CloudFront 앞단 통합** | Amplify 호스팅 + 이미지/정적 자산 | CloudFront 오리진 shielding과 첫 요청 압축으로 ALB/Amplify 원천 트래픽 감소 | CloudFront CacheHitRate, BytesDownloaded | 캐시 적중률 < 80% 장기 |
| **NAT Gateway 최소화** | 프라이빗 서브넷 구성 | AZ당 1개 대신 서비스 의존성 기반 공유 | NAT 시간당 요금 + data processing | HA 요구(AZ 장애 격리) 충돌 시 |

#### 5.5 관측성

| 레버 | 대상 | 예상 절감 | 측정 방법 | 롤백 기준 |
|---|---|---|---|---|
| **CloudWatch Logs 보존기간 조정** | 환경별(dev 7일, beta 30일, prod 90일 + Glacier 아카이브) | 보존 단축 비율만큼 Storage 비용 선형 절감 | `IncomingBytes`/`StoredBytes` | 사고 조사 시 필요 로그 누락 ≥ 월 1건 |
| **로그 카디널리티 감소** | 구조화 로그에서 고카디널리티 필드 샘플링 | 수집 비용($0.5/GB 수준) 감소 | Logs Insights `stats count by field` | 필수 디버그 필드 누락 |
| **Metric Filter / EMF 선택 적용** | 고빈도 커스텀 지표 | Custom Metric $0.30/metric 기준 카디널리티 1/N 감소 | CloudWatch Usage | 알람 대상 지표 소실 |
| **X-Ray 샘플링 룰 조정** | prod 1% 기본, 오류 100% | 스팬 비용 대폭 감소 | X-Ray Usage | 성능 회귀 원인 분석 불가 |

#### 5.6 비운영 환경 Auto-Shutdown (**필수 항목**)

| 리소스 | 스케줄 | 방법 | 예상 절감 |
|---|---|---|---|
| dev/beta ECS 서비스 | 평일 20:00~08:00 OFF, 주말 전일 OFF | EventBridge Scheduler → Lambda → `ecs update-service --desired-count` | 가동 시간 168h → 약 55h (**~67%**) |
| dev Aurora | Serverless v2 하한 0.5 ACU + 평일 밤 11시 이후 cluster stop(최대 7일 자동 재기동 전 cron 재실행) | EventBridge + Lambda | 일 약 50% |
| dev/beta ElastiCache | 필요 시에만 생성(ephemeral) | Terraform workspace + PR 기반 provision | 100% |
| dev Amplify | PR 미연결 브랜치 삭제 | Amplify CLI + GitHub Actions | 변동 |

출처: AWS Well-Architected Cost Optimization Pillar — COST 9 Manage demand and supplying resources.

---

### 6. 적정 규모 (right-sizing)

#### 6.1 Compute Optimizer 반영 절차

1. Organization 수준에서 **Compute Optimizer** 활성화.
   지원 리소스: EC2 인스턴스, EC2 Auto Scaling Group, EBS 볼륨, Lambda 함수, **ECS on EC2 서비스**, RDS DB 인스턴스 등.
   **ECS Fargate Task**는 공식 지원 대상이 아니므로 §6.3의 Load test 기반 percentile 실험으로 별도 사이징 수행.
2. 주간(매주 월요일) 권고 리포트 CSV export → S3 → Athena 쿼리.
3. 권고 우선순위 분류:
   - **Over-provisioned + High Savings**: 2주 내 반영 (비프로덕션 즉시, 프로덕션은 Load test 후).
   - **Under-provisioned**: 성능 영향 가능성 → 우선 SLO 검토.
   - **Optimal**: 무조치, 다음 주기 재평가.
4. 변경은 **Terraform PR**로만 반영(수동 변경 금지). PR에 Before/After Graviton/Size/Cost 표 첨부.
5. 적용 후 **2주 관측** → p95 latency, 에러율, CPU/Memory utilization 비교. 합격 시 closed.

#### 6.2 Aurora / ElastiCache 사이즈 기준

| 지표 | 스케일 다운 조건 | 스케일 업 조건 |
|---|---|---|
| Aurora CPU p95 | < 40% (14일) | > 70% (60분 지속) |
| Aurora FreeableMemory | > 30% 인스턴스 메모리 (14일) | < 10% |
| Aurora DatabaseConnections p95 | < 30% of `max_connections` | > 75% |
| ReplicaLag | - | > 5초 지속 → Reader 추가 |
| ElastiCache CPUUtilization p95 | < 40% | > 70% |
| ElastiCache EngineCPUUtilization | < 50% | > 85% |
| ElastiCache BytesUsedForCache / maxmemory | < 40% | > 80% |

- 변경 전 **Performance Insights** 상위 SQL 확인으로 리소스 대기 원인 분리.
- ElastiCache Valkey는 Graviton(r7g) 대응 티어로 전환 검토.

#### 6.3 ECS Task 사이징 실험

**프로토콜**:
1. 기준 트래픽 생성: k6/Locust로 프로덕션 RPS p50, p95, peak×1.5 재현.
2. 후보 사이즈 3종 실행(예: 0.5vCPU/1GB, 1vCPU/2GB, 2vCPU/4GB).
3. 관측 지표(부하 30분):
   - 응답 p50/p95/p99 latency
   - CPU/Memory utilization p95
   - Task 당 RPS 상한(throughput)
   - Cold start / JVM warmup 시간
4. 선정 기준:
   - **CPU p95 ∈ [55%, 70%]**, Memory p95 ≤ 75% 구간의 최소 사이즈.
   - 여유가 너무 크면(>30%) 한 단계 축소 후 재시험.
5. desired_count는 `peak RPS / Task RPS 상한 × 1.2(버퍼)` 로 초기값 설정, Target Tracking(ALB RequestCountPerTarget)으로 자동 조절.

#### 6.4 리사이즈 이력 관리

- Git 리포지토리 `infra/rightsizing/` 하위에 `YYYY-MM-DD-<service>.md` 형식 로그.
- 필드: Before/After 사양, 근거 지표(CloudWatch 링크), Compute Optimizer 추천 ID, Before/After 월비용(Pricing Calculator), 관측 2주 결과, 승인자.
- QuickSight에 리사이즈 누적 절감 대시보드.

출처: AWS Compute Optimizer User Guide.

---

### 7. 약정 관리 (reservation-and-commitment)

#### 7.1 Savings Plans vs Reserved Instance

| 항목 | Compute Savings Plans | EC2 Instance Savings Plans | Reserved Instance(RI) |
|---|---|---|---|
| 적용 범위 | EC2 + Fargate + Lambda, 리전/패밀리/사이즈/OS 무관 | 특정 인스턴스 패밀리 + 리전 | 특정 DB 엔진/클래스/리전(Aurora RI는 없고 Aurora는 Reserved Node 개념) |
| 유연성 | 최고 | 중간 | 낮음 |
| 공식 최대 절감 | **~66%**(3y All Upfront) | **~72%**(3y All Upfront) | Aurora RI **최대 ~66%**(3y All Upfront) |
| Tech-N-AI 활용 | **ECS Fargate 기본 커버리지의 주 수단** | 전환 계획 없음(EC2 최소) | **Aurora Reserved** 병행 |

#### 7.2 기간 · 결제 옵션 결정 프레임워크

결정 매트릭스:
```text
IF 서비스 3년 지속 확신도 ≥ 80% AND 현금 여유 OK
   → 3y All Upfront (최대 절감)
ELIF 1~3년 사이 구조 변경 가능성
   → 1y No/Partial Upfront (유연성)
ELIF 변동성 높음 / 실험 단계
   → 약정 없음, On-demand + Spot 병행
```

- **Tech-N-AI 기본 선정**:
  - ECS Fargate(전체 API) → **Compute SP, 1y, No Upfront**, 커버리지 70%.
  - Aurora prod writer → **RI 1y Partial Upfront**, 커버리지 80%(워크로드 안정).
  - Lambda, 배치 → 약정 없음(수요 변동 큼).
  - OpenAI/Cohere 외부 API → 약정 불가(사용량 기반 FinOps 관리).

#### 7.3 커버리지 목표

- 목표: **정상 사용량(Steady-state)의 70~80%**. 100%는 성장/축소에 과약정 리스크.
- 산출식:
  ```text
  커버리지(%) = (SP/RI 적용 사용량) / (전체 적용가능 사용량) × 100
  목표 약정 규모 = 최근 90일 p20(하위 20% 분위) 시간당 사용량
  ```
- 분위 기반 접근 이유: 최소 사용량 기준으로 약정하여 유휴 리스크 제거.

#### 7.4 만료 관리 대시보드 (QuickSight)

- 표 1: 약정 목록(종류/리전/시작일/만료일/잔여일/월 커밋 $)
- 표 2: **만료 D-90, D-60, D-30** 알림 자동화(Lambda + SES + Slack).
- 표 3: 커버리지 추이(일별), 활용률(Utilization), 추천 추가 약정(AWS 권고 API `GetSavingsPlansPurchaseRecommendation`).
- KPI: Coverage ≥ 70%, Utilization ≥ 95%, 만료 후 미갱신 결손 < 1일.

출처: AWS Savings Plans User Guide / AWS Cost Management — Reservation & Savings Plans Recommendations.

---

## 베스트 프랙티스 체크리스트

- [x] 모든 리소스 태그 표준(09 문서) 100% 준수 → Cost Allocation Tag 활성화
- [x] 비운영 환경(dev/beta) 야간·주말 Auto-shutdown(EventBridge Scheduler)
- [x] 모든 S3 버킷 Lifecycle 정책 보유(Lifecycle 미설정 버킷 0 목표)
- [x] 미사용 리소스 주간 리포트(미연결 EBS, 미사용 EIP, 오래된 AMI/Snapshot)
- [x] 분기 1회 DR 훈련 수행 및 결과 리포트 문서화(본 문서 §3.4 템플릿)
- [x] 백업 복원 실제 수행(분기 1회 이상) — 백업 유효성 검증
- [x] Cross-Region / Cross-Account Backup 복사 + Vault Lock(prod Compliance mode)
- [x] Route 53 Health Check + ARC Routing Control로 Tier 0 수동 승격 체계
- [x] Budgets 80/100/120% 알림 + Cost Anomaly Detection 활성화
- [x] Savings Plans 커버리지 70~80% 유지, 만료 D-90/60/30 알림

---

## 참고 자료 (공식 출처만)

- AWS Well-Architected — Reliability Pillar: https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html
- AWS Well-Architected — Cost Optimization Pillar: https://docs.aws.amazon.com/wellarchitected/latest/cost-optimization-pillar/welcome.html
- AWS Disaster Recovery of Workloads on AWS (whitepaper): https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/
- AWS Backup Developer Guide: https://docs.aws.amazon.com/aws-backup/latest/devguide/
- Amazon Route 53 Application Recovery Controller: https://docs.aws.amazon.com/r53recovery/latest/dg/
- AWS Fault Injection Service User Guide: https://docs.aws.amazon.com/fis/latest/userguide/
- AWS Cost Management (Budgets / CUR / Cost Anomaly Detection): https://docs.aws.amazon.com/cost-management/
- AWS Compute Optimizer User Guide: https://docs.aws.amazon.com/compute-optimizer/latest/ug/
- AWS Savings Plans User Guide: https://docs.aws.amazon.com/savingsplans/latest/userguide/
- Amazon Aurora Pricing (I/O-Optimized): https://aws.amazon.com/rds/aurora/pricing/
- FinOps Foundation Framework: https://www.finops.org/framework/

---

## 제약 및 운영 원칙

- **비용 예측**: 본 문서의 절감률은 모두 AWS Pricing Calculator 및 공식 Pricing 페이지의 범위값이며, 실제 값은 워크로드 프로파일에 따라 다름. 절대값보다 **비교값(Before/After)** 중심으로 의사결정.
- **DR 설계**: 본 문서에 정의된 Runbook/전환 절차는 **실제 FIS 훈련으로 검증된 것만** 운영 단계에 반영. 미검증 절차는 "draft" 상태로 유지.
- **변경 관리**: 모든 인프라 변경은 Terraform PR로만 수행, 수동 변경 금지(09 문서 IaC 원칙 준수).
- **문서 개정 주기**: 분기 1회 정기 리뷰(FinOps + SRE 합동), DR 훈련 직후 갱신.
