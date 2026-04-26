# 11. 재해복구(DR) · 백업 · 비용 최적화

## 역할
당신은 DevOps 엔지니어이며 재해복구 설계자(DRP)와 FinOps 담당을 겸합니다. AWS Well-Architected의 **Reliability Pillar**와 **Cost Optimization Pillar**에 부합하는 산출물을 작성합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/11-dr-cost/` 에 저장하세요.

---

## Part A. 재해복구 및 백업

### 산출물 1: `dr-strategy.md`
1. **RTO / RPO 정의** (환경별, 서비스 티어별)
   - Tier 0(결제/인증): RTO ≤ 15분, RPO ≤ 1분
   - Tier 1(핵심 API): RTO ≤ 1시간, RPO ≤ 5분
   - Tier 2(내부 admin): RTO ≤ 4시간, RPO ≤ 1시간
2. **DR 패턴 선정**
   - Backup & Restore / Pilot Light / Warm Standby / Multi-site Active-Active
   - 각 패턴의 비용/복구 시간/복잡도 비교 후 티어별 선정
3. **DR 리전**: 프라이머리 `ap-northeast-2`(서울) → DR 후보(`ap-northeast-1`(도쿄) 또는 `ap-southeast-1`(싱가포르)) 선정 근거
4. **도메인/트래픽 전환**: Route 53 Health Check + Failover / Latency / ARC(Application Recovery Controller) Routing Control

### 산출물 2: `backup-plan.md`
AWS Backup 중앙화 전략:
1. 백업 대상(Aurora, EBS, EFS(사용 시), S3, DynamoDB(사용 시), FSx(사용 시))
2. 백업 플랜(일일/주간/월간, 보존 기간)
3. **Cross-Region Copy** 설정
4. **Cross-Account Copy**(Log Archive/Backup 계정)
5. Vault Lock(변경 불가) — 규제 요건 시
6. 복원 훈련 절차 및 주기 (분기 1회 이상)

### 산출물 3: `dr-test-plan.md`
- 연 1회 이상 **실제 장애 주입 훈련**(AWS FIS 사용)
- 시나리오: AZ 장애, 리전 장애, DB 장애, 네트워크 분할
- 훈련 평가 지표(목표 RTO/RPO 달성 여부, 자동화 커버리지)
- 결과 리포트 템플릿

---

## Part B. 비용 최적화 (FinOps)

### 산출물 4: `cost-governance.md`
1. **비용 가시성**
   - Cost Explorer / Cost and Usage Report → S3 → Athena/QuickSight
   - **태깅 전략** (09-iac의 공통 태그 준수) + Cost Allocation Tag 활성화
   - 조직 단위/환경별 대시보드
2. **예산 및 알림**
   - AWS Budgets 환경별/서비스별 월 예산 + 80/100/120% 임계치 알림
   - Anomaly Detection(Cost Anomaly Detection) 활성화
3. **Showback/Chargeback 모델** (필요 시)

### 산출물 5: `cost-optimization-levers.md`
서비스별 최적화 방법 카탈로그:

| 영역 | 최적화 수단 | 예상 절감 | 리스크 |
|---|---|---|---|
| 컴퓨팅 | Compute Savings Plans / Fargate Spot(배치) / ARM Graviton 전환 | 20~40% | 장기 약정 |
| DB | Aurora I/O-Optimized vs Standard 선택 / Reader 수 조정 / Serverless v2 하한 조정 | 10~30% | 성능 영향 |
| 스토리지 | S3 Intelligent-Tiering, Lifecycle, Glacier | 30~60% | 복원 지연 |
| 네트워크 | VPC Endpoint로 NAT 비용 절감 / CloudFront 오리진 비용 절감 | 중 | 낮음 |
| 관측성 | CloudWatch Logs 보존기간 조정 / 지표 카디널리티 감소 / Log 필터 | 중 | 디버깅 제약 |
| 비운영 시간 | dev/beta ECS 서비스 야간/주말 desired_count=0 스케줄 | 50%+ | 개발 UX |

- 각 항목에 **측정 방법**과 **롤백 기준** 명시

### 산출물 6: `right-sizing.md`
1. **Compute Optimizer** 권고 반영 절차
2. **Aurora/ElastiCache** 사이즈 조정 기준 (CPU, 메모리, 연결 수 p95)
3. **ECS Task 사이징 실험** (Load test → percentile 기반 리사이즈)
4. 리사이즈 이력 관리

### 산출물 7: `reservation-and-commitment.md`
- **Savings Plans(Compute/EC2/SageMaker)** vs **Reserved Instance** 비교
- 약정 기간(1년 vs 3년), 결제 옵션(All/Partial/No Upfront) 결정 프레임워크
- 커버리지 목표(정상 사용량의 70~80%)
- 만료 관리 대시보드

## 베스트 프랙티스 체크리스트
- [ ] 모든 리소스 태그 표준 100% 준수 → 비용 할당 가능
- [ ] 비운영 환경 야간/주말 Auto-shutdown
- [ ] S3 버킷 Lifecycle 필수 (비만료 버킷 0)
- [ ] 미사용 리소스 주간 리포트 및 정리 (미연결 EBS, 미사용 EIP 등)
- [ ] 분기마다 DR 훈련 수행 및 결과 문서화
- [ ] 백업 복원을 **실제로** 주기적으로 수행 (백업 유효성 검증)

## 참고 자료 (공식 출처만)
- AWS Well-Architected — Reliability Pillar: https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html
- AWS Well-Architected — Cost Optimization Pillar: https://docs.aws.amazon.com/wellarchitected/latest/cost-optimization-pillar/welcome.html
- AWS Disaster Recovery of Workloads on AWS: https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/
- AWS Backup Developer Guide: https://docs.aws.amazon.com/aws-backup/latest/devguide/
- Amazon Route 53 ARC: https://docs.aws.amazon.com/r53recovery/latest/dg/
- AWS Fault Injection Service: https://docs.aws.amazon.com/fis/latest/userguide/
- AWS Cost Management: https://docs.aws.amazon.com/cost-management/
- AWS Compute Optimizer: https://docs.aws.amazon.com/compute-optimizer/latest/ug/
- AWS Savings Plans: https://docs.aws.amazon.com/savingsplans/latest/userguide/
- FinOps Foundation Framework: https://www.finops.org/framework/

## 제약
- 비용 예측값은 **AWS Pricing Calculator** 기반 산출식으로 제시(절대값보다 비교값 강조)
- DR 설계는 **실제 훈련으로 검증**된 절차만 운영 단계에 반영
