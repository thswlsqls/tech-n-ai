# 10. 배포 · 롤백 · 장애 대응 런북

## 역할
당신은 SRE/운영 리드 엔지니어입니다. 실제 운영자가 그대로 수행 가능한 **실행 가능한 런북(Operational Runbook)**을 작성합니다. 각 절차는 단계 번호, 명령어, 예상 결과, 실패 시 조치를 반드시 포함합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/10-runbook/` 에 저장하세요.

### 산출물 1: `release-process.md` — 릴리즈 프로세스
1. **릴리즈 캘린더**: 정기 릴리즈 요일/시간대, 코드 프리즈 규칙
2. **릴리즈 체크리스트** (각 단계 담당자/결과 확인란 포함)
   - 변경 범위 문서화(CHANGELOG)
   - DB 마이그레이션 검토 (Flyway/Liquibase)
   - 기능 플래그 상태 확인
   - 성능/부하 회귀 테스트 결과
   - 보안 스캔 결과
   - 승인(PO/Tech Lead/보안)
3. **배포 창(Deployment Window)** 운영 규칙

### 산출물 2: `deploy-backend.md` — 백엔드 배포
각 환경(dev/beta/prod)별 상세 절차:
1. 사전 점검
   - 테스트 통과 / 이미지 스캔 결과 / 헬스체크 대상 정상
   - **Actuator liveness/readiness probe 활성화 확인** (`management.endpoint.health.probes.enabled=true`, `/actuator/health/liveness`, `/actuator/health/readiness` 200 응답). 현재 프로젝트는 해당 설정이 **미적용 상태**이므로 최초 배포 전에 반드시 적용 여부를 검증
   - ALB Target Group 헬스체크 경로/성공 코드 정합성
2. Blue/Green 또는 Rolling 배포 명령
   ```bash
   aws ecs update-service --cluster {cluster} --service {service} \
       --task-definition {td-arn} --force-new-deployment
   ```
3. 스모크 테스트(핵심 API 10개 호출 예시)
4. 트래픽 전환 및 검증
5. 실패 감지 기준 (CloudWatch 알람, 에러율 임계치)
6. 자동 롤백 설정 (CodeDeploy Deployment Config + Alarm)

### 산출물 3: `deploy-frontend.md` — 프론트엔드 배포
- `app`, `admin` 각각에 대해
- Amplify Hosting: 머지 시 자동 배포 + 수동 승격
- CloudFront+S3: 동기화 + Invalidation 단계별 명령

### 산출물 4: `database-migration.md` — DB 마이그레이션
1. **Aurora 스키마 변경**
   - 무중단 마이그레이션 원칙(Expand-Contract 패턴)
   - 대용량 테이블 변경: pt-online-schema-change or `ALGORITHM=INSTANT`
   - 마이그레이션 롤백 전략
2. **MongoDB Atlas 인덱스/스키마 변경**
   - 인덱스 빌드 Rolling, `hidden index` 활용
3. 실패 시 복구 절차 (PITR, 스냅샷 복원)

### 산출물 5: `rollback.md` — 롤백 절차
- **애플리케이션 롤백**: 이전 Task Definition 리비전 재배포
- **이미지 롤백**: ECR 이전 태그로 교체
- **스키마 롤백**: 호환성 유지 원칙, Forward-only migration 권장
- **IaC 롤백**: Terraform plan/apply 이전 커밋 기반, 상태 손상 시 복구 절차
- **RTO 목표** 및 검증 방법

### 산출물 6: `incident-response.md` — 장애 대응
NIST SP 800-61 기반 + Google SRE Incident Management 프랙티스:
1. **감지 & 분류**: Sev1/Sev2/Sev3 기준 (영향 범위, 시간)
2. **대응 조직**: Incident Commander, Ops Lead, Comms Lead 역할
3. **커뮤니케이션 템플릿**: 초기 공지, 업데이트, 종료 공지 (Slack/Status Page)
4. **핵심 플레이북**
   - API 5xx 폭증
   - DB 연결 고갈 / RDS CPU 100%
   - Kafka 컨슈머 랙 폭증
   - CloudFront/ALB 지연
   - 외부 의존성(OpenAI, MongoDB Atlas) 장애
5. **포스트모템** 템플릿 (Blameless, 5 Whys, Action Items 추적)

### 산출물 7: `on-call.md` — On-call 운영
- 로테이션 스케줄 원칙, 주/야간 분리
- 에스컬레이션 정책 (PagerDuty 단계별)
- On-call 인수인계 체크리스트
- 피로도 관리(연속 야간 제한)

### 산출물 8: `break-glass.md` — 비상 접근
- Break-glass 계정 사용 조건, 승인 절차
- 사용 후 감사 및 크레덴셜 재발급

## 베스트 프랙티스 체크리스트
- [ ] 모든 런북은 **단계 번호 + 명령어 + 예상 출력 + 실패 시 조치** 포함
- [ ] 런북 내 링크는 실제 대시보드/알람 URL
- [ ] 분기마다 **GameDay**(Chaos Engineering) 실시 — AWS Fault Injection Service 활용
- [ ] 포스트모템은 비난 없는(blameless) 포맷
- [ ] 런북은 코드 레포에 커밋되고 PR 리뷰를 거친다

## 참고 자료 (공식 출처만)
- AWS Operational Readiness Reviews: https://docs.aws.amazon.com/wellarchitected/latest/operational-readiness-reviews/
- AWS Resilience Hub: https://docs.aws.amazon.com/resilience-hub/
- AWS Fault Injection Service: https://docs.aws.amazon.com/fis/latest/userguide/
- AWS CodeDeploy Deployment Configurations: https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-configurations.html
- Amazon RDS Best Practices (Failover, Backup): https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html
- Google SRE — Managing Incidents: https://sre.google/sre-book/managing-incidents/
- Google SRE — Postmortem Culture: https://sre.google/sre-book/postmortem-culture/
- NIST SP 800-61 Rev.2: https://csrc.nist.gov/publications/detail/sp/800-61/rev-2/final

## 제약
- 모든 명령어는 복사-붙여넣기로 실행 가능한 형태 (placeholder는 `{명시}`로 표기)
- 런북은 1인 운영자가 처음 접해도 30분 내 수행 가능한 수준으로 작성
