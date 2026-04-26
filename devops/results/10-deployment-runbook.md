# 10. 배포 · 롤백 · 장애 대응 런북

> 본 문서는 SRE/운영 리드 관점에서 작성된 **실행 가능한 런북(Operational Runbook)** 이다. 모든 절차는 `단계 번호 → 명령어 → 예상 출력 → 실패 시 조치` 구조를 따르며, 1인 운영자가 처음 접해도 30분 내 수행 가능한 수준으로 기술되어 있다.
>
> 선행 문서(01~09)의 아키텍처 전제를 따른다.
> - ECS Fargate + CodeDeploy Blue/Green (`api-gateway:8081`, `api-emerging-tech:8082`, `api-auth:8083`, `api-chatbot:8084`, `api-bookmark:8085`, `api-agent:8086`)
> - 이미지: Paketo `bootBuildImage` → ECR `techai/{module}:{semver}-{git-sha}` (D-1: 단일 리포 + 환경 무관 태그)
> - Aurora MySQL + Flyway / MongoDB Atlas
> - 프론트: Amplify Hosting (`app`, `admin`)
> - 알람: CloudWatch → SNS → PagerDuty/Slack
> - 외부 의존: OpenAI / Cohere / MongoDB Atlas
>
> ⚠️ **중요**: 현재 백엔드 모듈은 `management.endpoint.health.probes.enabled=true` 가 **미설정 상태**이다. 최초 배포 전 반드시 `/actuator/health/liveness`, `/actuator/health/readiness` 200 응답을 확인해야 한다. 본 런북 §2.1 "사전 점검" 단계에 검증 절차가 포함되어 있다.

---

## 1. 릴리즈 프로세스 (release-process)

### 1.1 릴리즈 캘린더

| 구분 | 요일 / 시간 (KST) | 대상 환경 | 배포 창 길이 | 담당 |
|---|---|---|---|---|
| 정기 릴리즈 | **화요일 14:00 ~ 16:00** | prod | 2시간 | Release Manager |
| 정기 릴리즈 | **목요일 14:00 ~ 16:00** | prod | 2시간 | Release Manager |
| 상시 배포 | 평일 10:00 ~ 18:00 | dev / beta | 제한 없음 | 각 모듈 담당자 |
| 긴급 패치 | 24/7 | prod | 비상 창 | On-call IC 승인 후 |

**금지 시간대 (Deployment Freeze Window)**
- 금요일 12:00 이후 ~ 월요일 10:00 이전 (주말 대응 인력 부족)
- 매월 말일 18:00 ~ 익월 1일 10:00 (정산/매출 트랜잭션)
- 공휴일 전일 18:00 이후
- 대규모 마케팅 이벤트 전후 24시간 (PO 공지 기준)

### 1.2 코드 프리즈 규칙

| 프리즈 레벨 | 기간 | 허용 머지 |
|---|---|---|
| Soft Freeze | 릴리즈 48시간 전 | 기능 PR 금지, bugfix/doc 허용 |
| Hard Freeze | 릴리즈 4시간 전 | Sev1 긴급 패치만 (IC 승인) |
| Post-Deploy Freeze | 배포 후 1시간 | 모니터링 집중, 신규 변경 금지 |

### 1.3 릴리즈 체크리스트 (PR/이슈 템플릿)

각 항목은 담당자 서명(GitHub @mention)과 결과 링크를 남긴다.

| # | 항목 | 담당 | 증빙 | 확인란 |
|---|---|---|---|---|
| 1 | CHANGELOG 업데이트 (변경 범위·영향도·릴리즈 노트) | 개발 리드 | `CHANGELOG.md` 커밋 | ☐ |
| 2 | DB 마이그레이션 스크립트 리뷰 (Flyway `V__` 파일) | DBA | PR 링크 + Expand-Contract 여부 | ☐ |
| 3 | 기능 플래그(Feature Flag) 기본값/롤아웃 계획 | PO | Flag 대시보드 링크 | ☐ |
| 4 | 성능/부하 회귀 테스트 결과 (k6/Gatling) | QA | 리포트 URL, p95/p99 | ☐ |
| 5 | 보안 스캔 (Trivy 이미지 / Snyk / OWASP DC) | 보안 담당 | Critical/High = 0 | ☐ |
| 6 | Actuator Probe 설정 검증 (본 프로젝트 필수) | 개발 리드 | `/actuator/health/liveness` 200 | ☐ |
| 7 | ALB Target Group 헬스체크 경로 정합성 | SRE | TG ARN + HC path | ☐ |
| 8 | 승인 (PO / Tech Lead / 보안 / SRE) | 4인 | PR Approve 기록 | ☐ |
| 9 | 롤백 계획서 첨부 | Release Mgr | 본 런북 §5 인용 | ☐ |
| 10 | 모니터링·알람 상시 on 확인 (CloudWatch Dashboard) | SRE | 대시보드 URL | ☐ |

### 1.4 배포 창(Deployment Window) 운영 규칙

1. 배포 창 진입 전 **#deploy Slack 채널**에 공지 (T-30 min)
   - 템플릿: `[DEPLOY] {env} techai/{module}:{semver}-{git-sha} | Window: {start}~{end} KST | IC: @handle`
2. 배포 창 내부 원칙
   - 동시에 1개 모듈만 prod 배포 (블래스트 라디우스 축소)
   - CodeDeploy Blue/Green traffic shift 완료 후 10분 안정화 관찰 후 다음 모듈
   - 30분 내 실패 2회 이상 발생 시 당일 배포 중단 (IC 판단)
3. 배포 창 종료 후 보고: `#deploy` 채널에 완료/롤백/잔여 이슈 요약

참고: AWS Operational Readiness Reviews — https://docs.aws.amazon.com/wellarchitected/latest/operational-readiness-reviews/

---

## 2. 백엔드 배포 (deploy-backend)

환경별 차이점 요약.

| 환경 | 클러스터 | 승인 | 배포 전략 | 스모크 |
|---|---|---|---|---|
| dev | `tech-n-ai-dev` | 자동 (PR merge → main) | CodeDeploy Blue/Green **Canary10Percent5Minutes** (`modules/ecs-service` 기본값, envs/dev override 없음) | 자동 |
| beta | `tech-n-ai-beta` | 수동 1인 (Tech Lead) | CodeDeploy Blue/Green **Canary10Percent5Minutes** | 수동+자동 |
| prod | `tech-n-ai-prod` | 수동 2인 (Release Mgr + SRE) | CodeDeploy Blue/Green **Canary10Percent5Minutes** (현행 — `envs/prod` override 없음. 더 보수적인 `Canary10Percent15Minutes`/`Linear*` 로의 변경은 별도 ADR 후 prod tfvars `deployment_config_name` 오버라이드) | 수동 필수 |

### 2.1 사전 점검

**단계 1. 빌드/테스트 아티팩트 확인**

```bash
# 최신 main 빌드 성공 여부
gh run list --repo {owner}/tech-n-ai-backend --branch main --limit 1 \
  --json conclusion,status,headSha,url
```

예상 출력:
```json
[{"conclusion":"success","status":"completed","headSha":"{git-sha}","url":"https://github.com/.../actions/runs/..."}]
```

실패 시 조치:
- `conclusion != success` → 배포 중단. 실패 Job 로그 확인 후 원인 해결.
- 테스트 커버리지 80% 미만 → Release Manager가 예외 승인하지 않는 한 중단.

**단계 2. ECR 이미지 존재 및 스캔 결과 조회**

```bash
# 이미지 태그 존재 확인 (D-1: 단일 ECR 리포 `techai/{module}`, 환경 무관 태그)
aws ecr describe-images \
  --repository-name techai/{module} \
  --image-ids imageTag={semver}-{git-sha} \
  --query 'imageDetails[0].{pushed:imagePushedAt,size:imageSizeInBytes}'

# 이미지 스캔 결과 (Enhanced Scanning / Inspector)
aws ecr describe-image-scan-findings \
  --repository-name techai/{module} \
  --image-id imageTag={semver}-{git-sha} \
  --query 'imageScanFindings.findingSeverityCounts'
```

예상 출력:
```json
{"CRITICAL":0,"HIGH":0,"MEDIUM":2,"LOW":5}
```

실패 시 조치:
- 이미지 없음 → CI 파이프라인 재실행(`gh workflow run release.yml -f sha={git-sha}`).
- CRITICAL/HIGH > 0 → 보안팀 승인 없이 prod 배포 금지. dev/beta는 예외 기록 후 진행.

**단계 3. Actuator Probe 활성화 검증 (⚠️ 현재 미설정 — 최초 배포 전 필수)**

사전에 각 모듈의 `application.yml` 또는 `application-{env}.yml` 에 아래 설정이 반영되어 있어야 한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
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
```

검증 명령 (dev/beta/prod 모두 실행):

```bash
# Gateway를 통한 내부 경로 (각 모듈은 gateway가 프록시)
for MODULE in gateway emerging-tech auth chatbot bookmark agent; do
  echo "== ${MODULE} =="
  curl -fsS -o /dev/null -w "liveness:  %{http_code}\n" \
    https://{env}-api.tech-n-ai.com/internal/${MODULE}/actuator/health/liveness
  curl -fsS -o /dev/null -w "readiness: %{http_code}\n" \
    https://{env}-api.tech-n-ai.com/internal/${MODULE}/actuator/health/readiness
done
```

예상 출력:
```
== gateway ==
liveness:  200
readiness: 200
== emerging-tech ==
liveness:  200
readiness: 200
...
```

실패 시 조치:
- `404` 반환 → `probes.enabled=true` 미반영. 해당 모듈 `application.yml` PR 생성 후 재빌드/재배포.
- `503` 반환(readiness) → 의존성(DB/Mongo) 상태 확인. 아래 단계 4로 이동.
- **최초 배포** 상황이라면 prod 배포 절대 금지. dev에서 설정 적용 → beta 검증 → prod 순으로만 진행.

**단계 4. ALB Target Group 헬스체크 경로 정합성**

```bash
TG_ARN=$(aws elbv2 describe-target-groups \
  --names {env}-tg-{module} \
  --query 'TargetGroups[0].TargetGroupArn' --output text)

aws elbv2 describe-target-groups --target-group-arns ${TG_ARN} \
  --query 'TargetGroups[0].{path:HealthCheckPath,code:Matcher.HttpCode,interval:HealthCheckIntervalSeconds,healthy:HealthyThresholdCount,unhealthy:UnhealthyThresholdCount}'

aws elbv2 describe-target-health --target-group-arn ${TG_ARN} \
  --query 'TargetHealthDescriptions[].{id:Target.Id,state:TargetHealth.State,reason:TargetHealth.Reason}'
```

예상 출력:
```json
{"path":"/actuator/health/readiness","code":"200","interval":10,"healthy":2,"unhealthy":3}
[{"id":"10.0.1.23","state":"healthy","reason":null}, ...]
```

실패 시 조치:
- `path != /actuator/health/readiness` → Target Group 헬스체크 경로 수정 (Terraform 변경 후 apply).
- `state: unhealthy` → 해당 Task 로그(`/aws/ecs/{env}-{module}`) 확인, 기동 실패 원인 파악.

**단계 5. 모니터링 대시보드 상시 open**

- CloudWatch Dashboard: `https://console.aws.amazon.com/cloudwatch/home?region=ap-northeast-2#dashboards:name={env}-backend-overview`
- PagerDuty 서비스: `https://{org}.pagerduty.com/services/{service-id}`
- APM(예: AWS X-Ray 또는 Datadog): `https://app.datadoghq.com/apm/services?env={env}`

### 2.2 Blue/Green 배포 실행

**단계 1. Task Definition 신규 리비전 등록**

```bash
# 템플릿에서 이미지 SHA 치환
cat infra/ecs/taskdef-{env}-{module}.json \
  | jq --arg IMG "{account}.dkr.ecr.ap-northeast-2.amazonaws.com/techai/{module}:{semver}-{git-sha}" \
       '.containerDefinitions[0].image = $IMG' \
  > /tmp/taskdef.json

NEW_TD_ARN=$(aws ecs register-task-definition \
  --cli-input-json file:///tmp/taskdef.json \
  --query 'taskDefinition.taskDefinitionArn' --output text)

echo "NEW_TD_ARN=${NEW_TD_ARN}"
```

예상 출력:
```
NEW_TD_ARN=arn:aws:ecs:ap-northeast-2:{account}:task-definition/{env}-{module}:{rev}
```

실패 시 조치:
- `InvalidParameterException` → JSON 스키마 오류. `jq '.' /tmp/taskdef.json` 로 구조 확인.
- 권한 오류 → 배포 역할(`arn:aws:iam::{account}:role/deploy-{env}`) IAM 정책 확인.

**단계 2. CodeDeploy Deployment 생성 (Blue/Green)**

`appspec.yaml` 예시 (D-2: CodeDeploy Hook 람다 미사용 — ALB readiness probe + CloudWatch 알람 자동 롤백으로 동등 안전망 확보):

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: "{NEW_TD_ARN}"
        LoadBalancerInfo:
          ContainerName: "{module}"
          ContainerPort: {module-port}
        PlatformVersion: "LATEST"   # 07a §6.2 와 일치. 핀 고정이 필요할 때만 명시 버전 사용
# Hooks 섹션은 D-2 결정에 따라 사용하지 않는다.
# - 안전망: ALB Target Group `/actuator/health/readiness` + CodeDeploy Auto-Rollback(CW Alarm)
# - 향후 통합 스모크 테스트 자동화가 필요하면 ADR 별도 신설 후 추가


배포 트리거:

```bash
APPSPEC=$(cat appspec.yaml | sed "s|{NEW_TD_ARN}|${NEW_TD_ARN}|g" | jq -Rs .)

aws deploy create-deployment \
  --application-name {env}-ecs-{module} \
  --deployment-group-name {env}-dg-{module} \
  --deployment-config-name CodeDeployDefault.ECSCanary10Percent15Minutes \
  --revision "revisionType=AppSpecContent,appSpecContent={content=${APPSPEC}}" \
  --description "Deploy {module} {git-sha} by {operator}" \
  --query 'deploymentId' --output text
```

예상 출력:
```
d-ABCDEF123
```

실패 시 조치:
- `DeploymentLimitExceeded` → 진행 중 배포 존재. `aws deploy list-deployments --include-only-statuses Created InProgress` 로 확인 후 취소 또는 완료 대기.
- `InvalidTaskDefinition` → TD의 컨테이너 포트와 TG 포트 불일치. TD 재생성.

**단계 3. 배포 진행 모니터링**

```bash
DEPLOY_ID=d-ABCDEF123
watch -n 10 "aws deploy get-deployment --deployment-id ${DEPLOY_ID} \
  --query 'deploymentInfo.{status:status,info:deploymentOverview,errorInfo:errorInformation}'"
```

예상 출력:
```json
{"status":"InProgress","info":{"Pending":0,"InProgress":1,"Succeeded":0,"Failed":0,"Skipped":0,"Ready":0},"errorInfo":null}
```

참고: CodeDeploy Deployment Configurations — https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-configurations.html

### 2.3 스모크 테스트 (핵심 API 10개)

gateway(8081)를 경유하여 Green 환경 Test Listener 로 호출한다. 호출은 `AfterAllowTestTraffic` 훅에서 자동화되며, 수동 검증도 아래 스크립트로 수행.

```bash
BASE={env}-api.tech-n-ai.com    # 예: beta-api.tech-n-ai.com
TOKEN={test-jwt}

# 1) Gateway health
curl -fsS -o /dev/null -w "1 gw/health                %{http_code} %{time_total}s\n" \
  https://${BASE}/actuator/health

# 2) Auth - 로그인
curl -fsS -o /dev/null -w "2 auth/login               %{http_code} %{time_total}s\n" \
  -X POST https://${BASE}/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@tech-n-ai.com","password":"{smoke-pw}"}'

# 3) Auth - 토큰 리프레시
curl -fsS -o /dev/null -w "3 auth/refresh             %{http_code} %{time_total}s\n" \
  -X POST https://${BASE}/api/v1/auth/refresh \
  -H "Cookie: refresh_token={smoke-refresh}"

# 4) Emerging-tech - 기술 트렌드 목록
curl -fsS -o /dev/null -w "4 emerging-tech/trends     %{http_code} %{time_total}s\n" \
  https://${BASE}/api/v1/emerging-tech/trends?size=10

# 5) Emerging-tech - 단건 조회
curl -fsS -o /dev/null -w "5 emerging-tech/detail     %{http_code} %{time_total}s\n" \
  https://${BASE}/api/v1/emerging-tech/trends/{trend-tsid}

# 6) Chatbot - 세션 생성
curl -fsS -o /dev/null -w "6 chatbot/session          %{http_code} %{time_total}s\n" \
  -X POST https://${BASE}/api/v1/chatbot/sessions \
  -H "Authorization: Bearer ${TOKEN}"

# 7) Chatbot - RAG 질의 (OpenAI 의존, 타임아웃 확장)
curl -fsS --max-time 30 -o /dev/null -w "7 chatbot/ask              %{http_code} %{time_total}s\n" \
  -X POST https://${BASE}/api/v1/chatbot/ask \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"{sid}","question":"smoke test"}'

# 8) Bookmark - 목록
curl -fsS -o /dev/null -w "8 bookmark/list            %{http_code} %{time_total}s\n" \
  https://${BASE}/api/v1/bookmarks \
  -H "Authorization: Bearer ${TOKEN}"

# 9) Bookmark - 생성
curl -fsS -o /dev/null -w "9 bookmark/create          %{http_code} %{time_total}s\n" \
  -X POST https://${BASE}/api/v1/bookmarks \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"targetType":"TREND","targetId":"{trend-tsid}"}'

# 10) Agent - 작업 조회
curl -fsS -o /dev/null -w "10 agent/tasks             %{http_code} %{time_total}s\n" \
  https://${BASE}/api/v1/agents/tasks?size=5 \
  -H "Authorization: Bearer ${TOKEN}"
```

예상 출력:
```
1 gw/health                200 0.12s
2 auth/login               200 0.28s
3 auth/refresh             200 0.19s
4 emerging-tech/trends     200 0.35s
5 emerging-tech/detail     200 0.22s
6 chatbot/session          201 0.18s
7 chatbot/ask              200 4.82s
8 bookmark/list            200 0.21s
9 bookmark/create          201 0.25s
10 agent/tasks             200 0.33s
```

통과 기준:
- 모든 호출 2xx
- p99 < 1.5s (단 chatbot/ask 는 < 10s)
- 1회 실패 → 10초 후 1회 재시도. 2회 연속 실패 → 배포 중단.

실패 시 조치:
- CodeDeploy 콘솔에서 **Stop deployment and roll back deployment**.
- 실패 API 로그 수집: `aws logs tail /aws/ecs/{env}-{module} --since 10m --follow`.

### 2.4 트래픽 전환 검증

**단계 1. ALB Listener 규칙 확인**

```bash
LISTENER_ARN=$(aws elbv2 describe-listeners \
  --load-balancer-arn $(aws elbv2 describe-load-balancers \
     --names {env}-alb --query 'LoadBalancers[0].LoadBalancerArn' --output text) \
  --query 'Listeners[?Port==`443`].ListenerArn' --output text)

aws elbv2 describe-rules --listener-arn ${LISTENER_ARN} \
  --query 'Rules[].Actions[].ForwardConfig.TargetGroups'
```

예상 출력 (100% Green 전환 완료):
```json
[[{"TargetGroupArn":"...:targetgroup/{env}-tg-{module}-green/...","Weight":100},
  {"TargetGroupArn":"...:targetgroup/{env}-tg-{module}-blue/...","Weight":0}]]
```

**단계 2. 실제 트래픽 지표 확인 (5분 관찰)**

```bash
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApplicationELB \
  --metric-name HTTPCode_Target_5XX_Count \
  --dimensions Name=TargetGroup,Value=targetgroup/{env}-tg-{module}-green/{id} \
               Name=LoadBalancer,Value=app/{env}-alb/{id} \
  --statistics Sum \
  --start-time $(date -u -v-10M +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 60
```

예상 출력:
```json
{"Datapoints":[{"Timestamp":"...","Sum":0.0,"Unit":"Count"}, ...]}
```

실패 시 조치:
- 5xx 합계 > 10/min → CodeDeploy CloudWatch Alarm 발동으로 자동 롤백 시작. §5.1 절차로 수동 확인.

### 2.5 실패 감지 기준 및 자동 롤백

**CloudWatch 알람 (CodeDeploy Auto-Rollback 트리거)**

| 알람 이름 | 메트릭 | 임계치 | 평가 기간 |
|---|---|---|---|
| `{env}-{module}-5xx-rate` | `HTTPCode_Target_5XX_Count / RequestCount` | > 1% | 3/3 (1min) |
| `{env}-{module}-p99-latency` | `TargetResponseTime p99` | > 2000ms (chatbot 10000ms) | 3/3 (1min) |
| `{env}-{module}-healthy-host-count` | `HealthyHostCount` | < desiredCount | 2/2 (1min) |
| `{env}-{module}-task-oom` | `ECS/ContainerInsights OOMKilledCount` | > 0 | 1/1 (1min) |

**CodeDeploy Auto-Rollback 설정 확인**

```bash
aws deploy get-deployment-group \
  --application-name {env}-ecs-{module} \
  --deployment-group-name {env}-dg-{module} \
  --query 'deploymentGroupInfo.{autoRollback:autoRollbackConfiguration,alarms:alarmConfiguration}'
```

예상 출력:
```json
{
  "autoRollback":{"enabled":true,"events":["DEPLOYMENT_FAILURE","DEPLOYMENT_STOP_ON_ALARM"]},
  "alarms":{"enabled":true,"alarms":[{"name":"{env}-{module}-5xx-rate"},{"name":"{env}-{module}-p99-latency"}]}
}
```

실패 시 조치:
- `enabled:false` → 배포 중단. Terraform `aws_codedeploy_deployment_group.alarm_configuration` 블록 수정 후 apply.

**수동 중단 및 롤백**

```bash
aws deploy stop-deployment --deployment-id ${DEPLOY_ID} --auto-rollback-enabled
```

---

## 3. 프론트엔드 배포 (deploy-frontend)

### 3.1 Amplify Hosting — 자동 배포 + 수동 승격 (기본)

| 앱 | 브랜치(dev) | 브랜치(beta) | 브랜치(prod) | 도메인 |
|---|---|---|---|---|
| `app` | `develop` | `release/beta` | `main` | `www.tech-n-ai.com` |
| `admin` | `develop-admin` | `release/beta-admin` | `main-admin` | `admin.tech-n-ai.com` |

**단계 1. 변경사항 머지**

```bash
# app prod 예시: release/beta → main 머지
cd tech-n-ai-frontend
git switch main
git pull --ff-only
git merge --no-ff release/beta -m "release: {semver} {YYYY-MM-DD}"
git push origin main
```

예상 출력: GitHub Actions 또는 Amplify Webhook 트리거 → Amplify 콘솔에서 Provision/Build/Deploy 단계 진행.

**단계 2. Amplify 배포 상태 확인**

```bash
APP_ID=$(aws amplify list-apps \
  --query "apps[?name=='tech-n-ai-app'].appId | [0]" --output text)

aws amplify list-jobs \
  --app-id ${APP_ID} --branch-name main --max-results 1 \
  --query 'jobSummaries[0].{id:jobId,status:status,commit:commitId,start:startTime}'
```

예상 출력:
```json
{"id":"42","status":"SUCCEED","commit":"{git-sha}","start":"..."}
```

실패 시 조치:
- `FAILED` → `aws amplify get-job --app-id ${APP_ID} --branch-name main --job-id 42 --query 'job.steps[].{step:stepName,status:status,logUrl:logUrl}'` 로 실패 단계 로그 확인.
- `npm run build` 실패 → 로컬에서 `npm ci && npm run build` 재현 후 수정.

**단계 3. 프로덕션 스모크**

```bash
# 기본 페이지 및 주요 라우트 HTTP 상태
for PATH in "/" "/trends" "/login" "/api/health"; do
  curl -fsS -o /dev/null -w "${PATH}  %{http_code}\n" \
    https://www.tech-n-ai.com${PATH}
done

# 보안 헤더 확인 (예: CSP, HSTS)
curl -fsSI https://www.tech-n-ai.com/ \
  | grep -iE 'content-security-policy|strict-transport-security|x-frame-options'
```

예상 출력:
```
/  200
/trends  200
/login  200
/api/health  200
strict-transport-security: max-age=63072000; includeSubDomains; preload
content-security-policy: default-src 'self'; ...
x-frame-options: DENY
```

실패 시 조치:
- 404/5xx → 이전 배포로 즉시 롤백(§3.3).
- 보안 헤더 누락 → Amplify `customRules` / `customHeaders` 설정 확인.

**단계 4. 수동 승격 (beta → prod, redeploy 없이)**

```bash
# Amplify 수동 Job Start (deploy 타입)
aws amplify start-job \
  --app-id ${APP_ID} \
  --branch-name main \
  --job-type RELEASE \
  --query 'jobSummary.{id:jobId,status:status}'
```

### 3.2 CloudFront + S3 대체 경로 (선택)

일부 정적 자산(데모·사전 공개 페이지)은 S3+CloudFront 로 서빙한다.

**단계 1. 빌드 산출물 S3 동기화**

```bash
cd tech-n-ai-frontend/app
npm ci && npm run build

aws s3 sync ./out/ s3://tech-n-ai-{env}-static/app/ \
  --delete \
  --cache-control "public,max-age=31536000,immutable" \
  --exclude "*.html" --exclude "*.json"

aws s3 sync ./out/ s3://tech-n-ai-{env}-static/app/ \
  --cache-control "public,max-age=0,must-revalidate" \
  --exclude "*" --include "*.html" --include "*.json"
```

예상 출력:
```
upload: out/_next/static/... to s3://tech-n-ai-{env}-static/app/_next/static/...
...
```

**단계 2. CloudFront Invalidation**

```bash
DIST_ID=$(aws cloudfront list-distributions \
  --query "DistributionList.Items[?Aliases.Items[0]=='www.tech-n-ai.com'].Id | [0]" \
  --output text)

INV_ID=$(aws cloudfront create-invalidation \
  --distribution-id ${DIST_ID} \
  --paths "/index.html" "/" "/_next/data/*" \
  --query 'Invalidation.Id' --output text)

aws cloudfront get-invalidation --distribution-id ${DIST_ID} --id ${INV_ID} \
  --query 'Invalidation.Status'
```

예상 출력:
```
"InProgress"   # 1~3분 후 "Completed"
```

실패 시 조치:
- Invalidation 실패 → 상한(`Paths.Quantity` 3000) 초과 여부 확인, 개별 경로로 쪼개기.
- 구 버전 캐시 잔존 → 루트 `/*` 전체 Invalidation 실행(비용 고려).

### 3.3 프론트 롤백

Amplify는 이전 성공 Job을 재배포하여 롤백한다.

```bash
PREV_JOB=$(aws amplify list-jobs \
  --app-id ${APP_ID} --branch-name main --max-results 5 \
  --query 'jobSummaries[?status==`SUCCEED`] | [1].jobId' --output text)

aws amplify start-job \
  --app-id ${APP_ID} --branch-name main \
  --job-type RETRY \
  --job-id ${PREV_JOB}
```

---

## 4. DB 마이그레이션 (database-migration)

### 4.1 Aurora MySQL — Expand-Contract 패턴

무중단 원칙: **한 번의 릴리즈에서 하나의 단계만 수행**.

| 단계 | 내용 | 예시 |
|---|---|---|
| Expand | 컬럼/테이블 **추가**만. 기존 구조 유지 | `ALTER TABLE users ADD COLUMN phone VARCHAR(32) NULL` |
| Migrate | 애플리케이션이 **양쪽 쓰기/읽기** (feature flag) | Dual-write, backfill job |
| Contract | 구 컬럼/테이블 **삭제** | `ALTER TABLE users DROP COLUMN legacy_mobile` |

**단계 1. Flyway 마이그레이션 사전 검증 (dev/beta)**

```bash
cd tech-n-ai-backend
./gradlew :datasource-aurora:flywayInfo -Pprofile=dev
./gradlew :datasource-aurora:flywayValidate -Pprofile=dev
```

예상 출력:
```
+-----------+---------+--------------------+------+---------------------+--------+
| Category  | Version | Description        | Type | Installed On        | State  |
+-----------+---------+--------------------+------+---------------------+--------+
| Versioned | 24.01.1 | add phone to users | SQL  | 2026-04-20 14:02:11 | Success|
| Versioned | 24.01.2 | backfill phone     | SQL  |                     | Pending|
```

실패 시 조치:
- `Validate failed: Migration checksum mismatch` → 이미 적용된 파일 수정 금지. 신규 `V__` 추가로 해결.
- `Pending` 상태의 파일이 여러 개 → 한 번에 하나만 승인.

**단계 2. 대용량 테이블 변경**

`ALGORITHM=INSTANT` 우선 적용 (MySQL 8.0.29+).

```sql
-- 트랜잭션 분리, 컬럼 추가 (INSTANT 가능 여부 사전 EXPLAIN)
ALTER TABLE emerging_tech_trend
  ADD COLUMN sentiment_score DECIMAL(4,3) NULL,
  ALGORITHM=INSTANT, LOCK=NONE;
```

INSTANT 불가 변경(컬럼 타입 변경/PK 변경 등)은 `pt-online-schema-change`.

```bash
# EC2 점프호스트에서 실행
pt-online-schema-change \
  --alter "MODIFY COLUMN title VARCHAR(512) NOT NULL" \
  --execute \
  --chunk-size=1000 \
  --max-load "Threads_running=50" \
  --critical-load "Threads_running=100" \
  --no-drop-old-table \
  --recursion-method=dsn=D=percona,t=dsns \
  h={writer-endpoint},D=techai,t=emerging_tech_trend,u=migrator,p={pw}
```

예상 출력:
```
Copying `techai`.`emerging_tech_trend`:  12% 00:45 remain
...
Successfully altered `techai`.`emerging_tech_trend`.
```

실패 시 조치:
- `Threads_running` 초과로 일시 중단 → `--max-load` 자동 대기. 계속 지연 시 RDS Performance Insights 에서 Top SQL 확인.
- 중단 복구: `_<table>_new`, `_<table>_old`, triggers 잔존 여부 확인 후 `--resume` 또는 수동 정리.

**단계 3. Flyway 프로덕션 적용**

```bash
./gradlew :datasource-aurora:flywayMigrate -Pprofile=prod \
  -Dflyway.user=${FLYWAY_USER} -Dflyway.password=${FLYWAY_PWD} \
  -Dflyway.outOfOrder=false -Dflyway.validateOnMigrate=true
```

예상 출력:
```
Successfully applied 1 migration to schema `techai`, now at version v24.01.1 (execution time 00:00.182s)
```

**단계 4. 롤백 전략**

- **Forward-only 권장**: 롤백 마이그레이션 대신 **보정(compensating) 마이그레이션** 을 추가한다 (§5.3 참조).
- Expand 단계는 신규 컬럼 NULL 허용으로 호환성 보장 → 이전 버전 애플리케이션 무영향.
- Contract 단계 직전 최소 1개 릴리즈 (1주일) 안정화 기간 필요.

참고: Amazon RDS Best Practices — https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html

### 4.2 MongoDB Atlas — Rolling Index + Hidden Index

**단계 1. 인덱스 롤링 빌드 (Replica Set, 프로덕션 무중단)**

Atlas UI `Database > Collection > Indexes > Create Index > Options > Build in the background (Rolling)` 또는 CLI.

```bash
atlas clusters search indexes create --clusterName {cluster} --file - <<EOF
{
  "collectionName": "chat_vector",
  "database": "techai",
  "name": "embedding_idx",
  "type": "vectorSearch",
  "fields": [
    { "type": "vector", "path": "embedding", "numDimensions": 1536, "similarity": "cosine" }
  ]
}
EOF
```

**단계 2. Hidden Index 로 영향도 사전 검증**

```javascript
// mongosh
use techai;
db.chat_session.createIndex({ userId: 1, createdAt: -1 }, { hidden: true, name: "ix_user_time" });
// 1) 쿼리 플랜 확인 (hidden은 planner 무시)
db.chat_session.find({ userId: "u_1" }).sort({ createdAt: -1 }).explain("executionStats");
// 2) 성능 문제 없으면 unhide
db.chat_session.unhideIndex("ix_user_time");
```

실패 시 조치:
- Unhide 후 CPU/Latency 상승 → `db.chat_session.hideIndex("ix_user_time")` 로 즉시 복원.
- Rolling 빌드 중 Primary 교체 → 자동 재개. 30분 이상 지연 시 Atlas 지원 티켓.

### 4.3 실패 시 복구 (PITR / 스냅샷)

**Aurora PITR**

```bash
# 실행 전 확인
aws rds describe-db-clusters --db-cluster-identifier {env}-aurora-cluster \
  --query 'DBClusters[0].{earliest:EarliestRestorableTime,latest:LatestRestorableTime}'

# PITR 복원 (새 클러스터로)
aws rds restore-db-cluster-to-point-in-time \
  --db-cluster-identifier {env}-aurora-restore-$(date +%Y%m%d%H%M) \
  --source-db-cluster-identifier {env}-aurora-cluster \
  --restore-to-time $(date -u -v-30M +%Y-%m-%dT%H:%M:%SZ) \
  --use-latest-restorable-time false
```

예상 출력: 신규 클러스터 `status=creating` → 약 15-40분 후 `available`.

이후 애플리케이션 엔드포인트 전환은 Route53 가중치 변경 또는 JDBC URL 설정 롤아웃.

**MongoDB Atlas PITR**

```bash
atlas backups restores start pointInTime \
  --clusterName {cluster} \
  --pointInTimeUTC $(date -u -v-30M +%s) \
  --targetClusterName {cluster}-restore \
  --targetProjectId {project-id}
```

실패 시 조치:
- `Backup not found` → Continuous Backup 미활성화. Snapshot 복원으로 전환.
- RPO 목표(최대 5분) 초과 → IC에게 즉시 에스컬레이션.

---

## 5. 롤백 (rollback)

### 5.0 Quick Reference — 새벽 3시 1쪽 요약

> **결정 트리** (불 났을 때 첫 60초)
>
> ```
> 알람 트리거? ─Yes─► 최근 배포가 원인인지 (CloudWatch Deployments 위젯 확인)
>     │                  │
>     │                  ├─ 배포 직후 (15분 내) → §5.1 Task Definition 롤백 (가장 빠름, 5분)
>     │                  └─ 배포 무관 (시간 떨어짐) → §6 인시던트 플레이북
>     │
>     └─No─► §6 장애 대응
> ```

| 시나리오 | 명령 1줄 | 예상 RTO |
|---|---|---|
| **앱 롤백** (이전 TD revision) | `aws ecs update-service --cluster {env}-techai --service {env}-{module} --task-definition {family}:{prev-revision}` | ~5분 |
| **이미지 롤백** (이전 ECR digest) | `aws ecs register-task-definition --cli-input-json $(aws ecs describe-task-definition --task-definition {family} --query 'taskDefinition' \| jq '.containerDefinitions[0].image="<prev-digest>"') && aws ecs update-service ...` | ~10분 |
| **스키마 롤백** | Forward-only 원칙 — 새 마이그레이션으로 정정 (§5.3) | 마이그 시간 |
| **IaC 롤백** (코드 정상) | `git checkout {last-good-sha} -- . && terraform plan && terraform apply` | ~10분 |
| **IaC 롤백** (state 손상) | `aws s3api copy-object --bucket techai-tfstate-... --copy-source "...?versionId={prev}" --key ...` | ~15분 |
| **CodeDeploy 롤백** (자동) | (이미 진행 중인 배포는 알람 트리거 시 자동 — 메뉴얼 X) | < 5분 |
| **Amplify 롤백** (이전 deployment) | `aws amplify start-deployment --app-id ... --branch-name ... --source-url ... --job-id {prev-job-id}` | ~3분 |

> **금지 사항**: prod 에서 `terraform destroy` / `terraform state rm` / `git push --force` 절대 금지. 모든 IaC 변경은 새 PR + plan 검증 후 apply.



**RTO 목표**

| 구분 | 목표 RTO |
|---|---|
| 백엔드 단일 모듈 (TD 리비전) | **≤ 5분** |
| 백엔드 이미지 롤백 (ECR 전 태그) | **≤ 10분** |
| 프론트 Amplify | **≤ 5분** |
| Aurora PITR | ≤ 60분 |
| IaC (Terraform) | ≤ 30분 |

### 5.1 애플리케이션 롤백 — 이전 Task Definition 리비전 재배포

**단계 1. 최근 성공 리비전 식별**

```bash
aws ecs list-task-definitions \
  --family-prefix {env}-{module} --sort DESC --max-results 10 \
  --query 'taskDefinitionArns'
```

예상 출력:
```json
["arn:aws:ecs:...:task-definition/{env}-{module}:42", "...:41", "...:40", ...]
```

현재 리비전이 `42`이고 문제 발생 → `41`로 롤백.

**단계 2. CodeDeploy 를 통한 롤백 배포**

```bash
ROLLBACK_TD=arn:aws:ecs:ap-northeast-2:{account}:task-definition/{env}-{module}:41
APPSPEC=$(jq -n --arg td "${ROLLBACK_TD}" '{
  version: "0.0",
  Resources: [{
    TargetService: { Type: "AWS::ECS::Service", Properties: {
      TaskDefinition: $td,
      LoadBalancerInfo: { ContainerName: "{module}", ContainerPort: {module-port} }
    }}
  }]
}' | jq -Rs .)

aws deploy create-deployment \
  --application-name {env}-ecs-{module} \
  --deployment-group-name {env}-dg-{module} \
  --deployment-config-name CodeDeployDefault.ECSAllAtOnce \
  --revision "revisionType=AppSpecContent,appSpecContent={content=${APPSPEC}}" \
  --description "ROLLBACK {module} to rev 41 by {operator}"
```

예상 출력: 새 `deploymentId` 반환 후 `status: Succeeded` 까지 약 3-5분.

실패 시 조치:
- `ECSAllAtOnce` 실패 → `Canary10Percent5Minutes` 로 재시도.
- 리비전이 더 이상 유효하지 않음(`INACTIVE`) → §5.2 이미지 롤백.

### 5.2 이미지 롤백 — ECR 이전 태그로 TD 재생성

```bash
# 전전 성공 이미지 태그 식별 (D-1: 단일 ECR 리포 `techai/{module}`)
PREV_TAG=$(aws ecr describe-images \
  --repository-name techai/{module} \
  --query 'sort_by(imageDetails,&imagePushedAt)[-2].imageTags[0]' --output text)

echo "Rollback image: techai/{module}:${PREV_TAG}"

# TD 템플릿에서 이미지 치환 후 register (§2.2 단계 1 재사용)
```

### 5.3 스키마 롤백 — Forward-only 원칙

- 이미 적용된 Flyway 마이그레이션은 **되돌리지 않는다**. 대신 **보정 마이그레이션**을 추가한다.

예시:
```
V24.01.2__add_sentiment_score.sql          -- 적용됨, 문제 발생
V24.01.3__hotfix_make_sentiment_nullable.sql  -- 보정 추가
```

- 애플리케이션 버전 호환성을 유지하려면 Expand 상태에서 롤백하는 것이 원칙. Contract 이후 롤백은 PITR 복원만이 유효.
- `flyway undo` 는 Teams(상용) 전용이므로 **사용하지 않는다**.

### 5.4 IaC 롤백 — Terraform

**정상 경로 (상태 건전)**

```bash
cd devops/terraform/envs/{env}   # 실제 리포 경로
git log --oneline -n 10
git checkout {last-good-sha} -- .
terraform plan -out=rollback.tfplan
terraform apply rollback.tfplan
git switch -c rollback/{ticket}
git commit -am "rollback(infra): revert to {last-good-sha}"
```

예상 출력: `Apply complete! Resources: X added, Y changed, Z destroyed.`

**상태 손상(state corruption) 복구**

```bash
# 1) 원격 state (S3) 버전 목록 확인 (S3 versioning 필수 전제)
aws s3api list-object-versions \
  --bucket tf-state-{env} --prefix {path}/terraform.tfstate \
  --query 'Versions[?IsLatest==`false`] | [0].{v:VersionId,ts:LastModified}'

# 2) 이전 버전으로 되돌리기
aws s3api copy-object \
  --bucket tf-state-{env} \
  --copy-source "tf-state-{env}/{path}/terraform.tfstate?versionId={prev-version}" \
  --key {path}/terraform.tfstate

# 3) 무결성 확인
terraform state list | head
terraform plan   # drift 없어야 정상
```

실패 시 조치:
- `plan` 이 수많은 drift 표시 → 마지막으로 성공한 apply 시점 기준 **부분 import** 수행.
- 절대 `terraform state rm` / `destroy` 를 먼저 시도하지 않는다. 이는 실자원 삭제 위험.

### 5.5 RTO 검증

분기 1회 GameDay에서 아래 시나리오 실측 후 RTO 초과 여부 기록.

```bash
# AWS Fault Injection Service — ECS Task 50% 종료 시나리오
aws fis start-experiment \
  --experiment-template-id {template-id-terminate-50pct} \
  --tags Key=purpose,Value=rollback-rto-test
```

참고: AWS Fault Injection Service — https://docs.aws.amazon.com/fis/latest/userguide/ , AWS Resilience Hub — https://docs.aws.amazon.com/resilience-hub/

---

## 6. 장애 대응 (incident-response)

본 절은 NIST SP 800-61 Rev.3 (2025-04 발행, Rev.2 supersede)의 사이클(Preparation / Detection & Analysis / Containment, Eradication, Recovery / Post-Incident Activity) 과 Google SRE *Managing Incidents* 의 IC 체계를 결합한다.

### 6.1 감지 & 분류 (Severity)

| Sev | 정의 | 예시 | 초기 대응 시간 | 공지 범위 |
|---|---|---|---|---|
| **Sev1** | 전체 서비스/주요 기능 불가, 데이터 유실 가능 | prod API 5xx > 30%, Aurora Writer 장애, 보안 침해 | **5분 내** | 전사 + 상태 페이지 |
| **Sev2** | 부분 기능 저하, 우회 가능 | 단일 모듈 5xx 5-30%, Kafka 컨슈머 랙 급증 | 15분 내 | 엔지니어링 + 고객 CS |
| **Sev3** | 성능 저하, 비핵심 기능 | p99 2배, 배치 지연 | 1시간 내 | 담당 팀 내부 |

분류 기준 지표 (자동): CloudWatch Composite Alarm `{env}-severity-classifier`.

### 6.2 대응 조직

| 역할 | 책임 | 배정 원칙 |
|---|---|---|
| **Incident Commander (IC)** | 전체 조율, 의사결정, 로그 유지 | 코드 터치 금지, 최초 On-call이 기본 IC |
| **Ops Lead** | 기술적 완화·복구 실행 | 장애 모듈 주 소유 팀 리드 |
| **Comms Lead** | 대내외 커뮤니케이션, 상태 페이지 | PM 또는 SRE 2차 On-call |
| **Scribe** | 타임라인 기록 (5분 단위) | Comms Lead 겸임 가능 |

Sev1에서는 4역할 분리 필수. Sev2~3은 IC+Ops 통합 가능.

### 6.3 커뮤니케이션 템플릿

**초기 공지 (T+0~5min)**

```
:rotating_light: [INCIDENT OPEN] Sev{1|2|3} | {title}
- Start:   {YYYY-MM-DD HH:MM KST}
- Impact:  {사용자/기능 영향}
- IC:      @{handle}
- Ops:     @{handle}
- Bridge:  {zoom or Slack huddle link}
- Status:  Investigating
- Ticket:  {PagerDuty incident url}
```

**업데이트 (30분 간격, 중대한 변화 시 즉시)**

```
:hourglass_flowing_sand: [INCIDENT UPDATE] {incident-id} | {status}
- Now:     {현재 조치}
- Done:    {지금까지 조치}
- Next:    {다음 15-30분 계획}
- ETA:     {예상 해소 시간 or "unknown"}
```

**종료 공지**

```
:white_check_mark: [INCIDENT RESOLVED] {incident-id}
- Duration:   {분}  (Start: {ts} → Resolved: {ts})
- Root cause (preliminary): {1-2줄}
- Customer impact: {영향 요약}
- Postmortem due: {YYYY-MM-DD}  owner: @{handle}
```

상태 페이지 (Statuspage/Instatus) 는 동일 내용을 고객 톤으로 축약.

### 6.4 핵심 플레이북

#### 6.4.1 API 5xx 폭증

**감지**: `{env}-{module}-5xx-rate` 알람.

1. 현재 배포 중인 CodeDeploy 확인
   ```bash
   aws deploy list-deployments --include-only-statuses InProgress \
     --query 'deployments'
   ```
   → 존재하면 즉시 중단: `aws deploy stop-deployment --deployment-id {id} --auto-rollback-enabled`.
2. 최근 Task 로그 확인
   ```bash
   aws logs tail /aws/ecs/{env}-{module} --since 15m --follow --format short
   ```
   Stack trace 상위 10개 에러 패턴 식별.
3. 의존성 헬스체크
   ```bash
   curl -fsS https://{env}-api.tech-n-ai.com/internal/{module}/actuator/health/readiness | jq
   ```
   DB/Mongo `DOWN` → §6.4.2 로 이동.
4. 문제 없는 이전 버전 존재 → §5.1 롤백.
5. 완화 후 30분 관찰, 알람 해제 확인.

#### 6.4.2 DB 연결 고갈 / RDS CPU 100%

1. Performance Insights Top SQL 확인
   ```bash
   aws pi get-resource-metrics \
     --service-type RDS \
     --identifier {db-resource-id} \
     --metric-queries '[{"Metric":"db.load.avg","GroupBy":{"Group":"db.sql_tokenized","Limit":10}}]' \
     --start-time $(date -u -v-15M +%s) --end-time $(date -u +%s) --period-in-seconds 60
   ```
2. 장시간 실행 쿼리 강제 종료
   ```sql
   SELECT id, user, host, time, state, LEFT(info,200) FROM information_schema.processlist
     WHERE command <> 'Sleep' AND time > 30 ORDER BY time DESC LIMIT 20;
   -- 문제 세션
   CALL mysql.rds_kill({pid});
   ```
3. HikariCP 풀 고갈 → 커넥션 누수 의심. 애플리케이션 재기동 전에 스레드 덤프.
   ```bash
   aws ecs execute-command --cluster {env}-{cluster} --task {task-arn} \
     --container {module} --interactive --command "/bin/sh -c 'jcmd 1 Thread.print'"
   ```
4. 완화 불가 시 Writer **Reader로 Failover**
   ```bash
   aws rds failover-db-cluster --db-cluster-identifier {env}-aurora-cluster
   ```
   RTO: 30-60초. 직후 애플리케이션 Connection Pool 워밍업 확인.

#### 6.4.3 Kafka 컨슈머 랙 폭증

1. 랙 조회 (MSK + Kafka CLI)
   ```bash
   kafka-consumer-groups.sh --bootstrap-server {brokers} \
     --describe --group {group} | awk 'NR==1 || $6+0 > 1000'
   ```
2. 컨슈머 Task 수 확장
   ```bash
   aws ecs update-service --cluster {env}-cluster \
     --service {env}-consumer-{name} --desired-count {N*2}
   ```
3. 파티션 증설 필요 시 (랙이 개별 파티션 편중)
   ```bash
   kafka-topics.sh --bootstrap-server {brokers} \
     --alter --topic {topic} --partitions {new-count}
   ```
   주의: 파티션 감소 불가, 키 기반 순서 보장 영향.
4. 소비 실패 메시지는 DLQ 확인: `{topic}.DLQ` 재처리.

#### 6.4.4 CloudFront / ALB 지연

1. Route53 헬스체크, CloudFront `5xxErrorRate` 확인.
2. Origin(ALB) TargetResponseTime 비교 — Origin 정상 & Edge 느리면 AWS 상태(Health Dashboard) 확인.
3. ALB Listener 규칙, WAF Rate Rule 차단율 확인
   ```bash
   aws wafv2 get-sampled-requests \
     --web-acl-arn {arn} --rule-metric-name {rule} --scope REGIONAL \
     --time-window StartTime=$(date -u -v-10M +%s),EndTime=$(date -u +%s) \
     --max-items 100
   ```
4. 대규모 봇 트래픽 감지 시 WAF Rate-based rule 임계치 하향 (Terraform 긴급 PR + apply).

#### 6.4.5 외부 의존성 장애 (OpenAI / Cohere / MongoDB Atlas)

1. 공식 상태 페이지 확인
   - OpenAI: https://status.openai.com
   - Cohere: https://status.cohere.com
   - Atlas: https://status.mongodb.com
2. `api-chatbot` 서킷브레이커(Resilience4j) 열림 여부
   ```bash
   curl -fsS https://{env}-api.tech-n-ai.com/internal/chatbot/actuator/circuitbreakers | jq
   ```
3. Fallback 활성화 (기능 플래그)
   ```bash
   aws appconfig start-deployment \
     --application-id {app} --environment-id {env-id} \
     --deployment-strategy-id {strategy} \
     --configuration-profile-id {profile} \
     --configuration-version {v} \
     --description "enable rag-fallback on openai-outage"
   ```
4. 캐시 TTL 연장(신규 요청 축소), CS 스크립트 배포.

### 6.5 포스트모템 템플릿 (Blameless)

파일 경로: `/postmortems/{YYYY-MM-DD}-{incident-id}.md`

```markdown
# Postmortem: {Incident Title}

- Incident ID: {id}
- Severity: Sev{1|2|3}
- Duration: {start ts KST} → {end ts KST} (총 {분})
- Authors: @{handle}, @{handle}
- Status: Draft / Review / Final

## 1. Summary
2-3 문장으로 "무엇이 일어났고 어떻게 해소되었는가".

## 2. Impact
- 영향 사용자 수 / 트랜잭션 수 / 매출 영향 / 데이터 유실 여부.

## 3. Timeline (KST)
| Time | Event |
|---|---|
| HH:MM | 알람 발생 `{alarm-name}` |
| HH:MM | IC @{handle} 배정, Bridge 오픈 |
| HH:MM | 완화 조치 {내용} |
| HH:MM | 서비스 정상화 확인 |

## 4. Root Cause (5 Whys)
- Why 1: ...
- Why 2: ...
- Why 3: ...
- Why 4: ...
- Why 5: ...
**근본 원인**: {1-2 문장}

## 5. What went well
- ...

## 6. What went wrong
- ...
(※ 비난 금지. 개인 지목 대신 "프로세스/시스템" 초점)

## 7. Where we got lucky
- ...

## 8. Action Items
| # | Action | Owner | Due | Ticket |
|---|---|---|---|---|
| 1 | {액션} | @{handle} | {YYYY-MM-DD} | {JIRA-123} |

## 9. Lessons Learned
- ...
```

참고:
- Google SRE — Managing Incidents: https://sre.google/sre-book/managing-incidents/
- Google SRE — Postmortem Culture: https://sre.google/sre-book/postmortem-culture/
- NIST SP 800-61 Rev.3 (2025-04, Rev.2 supersede): https://csrc.nist.gov/pubs/sp/800/61/r3/final

---

## 7. On-call 운영 (on-call)

### 7.1 로테이션 원칙

| 항목 | 정책 |
|---|---|
| 1차 On-call | 주 단위 로테이션, 월~일 09:00 KST 교대 |
| 2차 On-call (Backup) | 동일 주기, 서로 다른 팀원 |
| 주간/야간 분리 | 주간(09~21) / 야간(21~09) 분리. 야간 단독 3일 연속 금지 |
| 최소 인원 | 모듈별 On-call Pool ≥ 3인 (부재 시 대체 가능) |
| 보상 | 야간 페이지 건당 보상, 주말 근무 대체 휴가 |

### 7.2 에스컬레이션 (PagerDuty 단계)

| 단계 | 시간 | 대상 |
|---|---|---|
| L1 | 0분 | 1차 On-call |
| L2 | +10분 미응답 | 2차 On-call (Backup) |
| L3 | +20분 미응답 | 팀 리드 |
| L4 | +30분 미응답 | SRE 리드 + 엔지니어링 디렉터 |
| L5 | Sev1 선언 시 즉시 | CTO / CPO / Comms Lead |

PagerDuty Escalation Policy 예시: `tech-n-ai-prod-backend` → 10m/10m/10m/0m.

### 7.3 인수인계 체크리스트

매주 금요일 17:00 KST Handover 미팅 (30분).

- [ ] 진행 중인 인시던트/티켓 요약
- [ ] 금주 발생 알람 Top 5 및 대응 결과
- [ ] 예정된 배포/마이그레이션/점검
- [ ] 알려진 장애 / 임시 완화 중인 항목 (workaround 문서 링크)
- [ ] 주요 지표 이상 여부 (에러율/지연/자원 사용률)
- [ ] 신규 런북/문서 업데이트 안내
- [ ] PagerDuty Override 승계 확인

### 7.4 피로도 관리

- 연속 야간 호출 3회 이상 시 익일 오전 근무 면제
- 주간 On-call 중 페이지 > 10회 → 익주 로테이션 제외 권장
- 분기별 On-call Load 리포트 (SRE 리드 리뷰)
- **Blameless postmortem** 문화로 신고 부담 최소화 — 알람 피로도 감소가 최종 목표

---

## 8. 비상 접근 (break-glass)

### 8.1 Break-glass 계정 개요

- AWS: `break-glass-{env}` IAM 사용자 (MFA 강제, 평시 비활성 정책 Deny\*)
- DB: Aurora `root` 자격 (AWS Secrets Manager `breakglass/aurora/{env}` 에 암호화 저장, 접근 로그 필수)
- Atlas: `breakglass-atlas` (Project Owner)

### 8.2 사용 조건 (필수 모두 충족)

1. Sev1 선언 + IC 승인
2. 통상 경로(IAM Identity Center / SSO)로는 복구가 기술적으로 **불가능**
3. 2인 승인 원칙: IC + SRE 리드(또는 CTO)

### 8.3 사용 절차

**단계 1. 승인 기록**

```bash
# 승인 이슈 생성 (Sev1 인시던트 링크 필수)
gh issue create --repo {org}/runbooks \
  --title "BREAK-GLASS {env} — {YYYY-MM-DD HH:MM} — {incident-id}" \
  --label break-glass,sev1 \
  --body "Requestor: @{handle}
Approver1 (IC): @{handle}
Approver2 (SRE Lead/CTO): @{handle}
Reason: {왜 통상 경로로 불가한가}
Scope: {수행할 작업 범위}
Expected duration: {분}"
```

**단계 2. 크레덴셜 인출**

```bash
aws secretsmanager get-secret-value \
  --secret-id breakglass/aws/{env} \
  --version-stage AWSCURRENT \
  --query SecretString --output text
```

예상 출력: JSON 형태의 AccessKey/SecretKey. 인출 이벤트는 CloudTrail `GetSecretValue` 로 자동 감사된다.

**단계 3. 작업 수행 (최소 권한 · 최단 시간)**

- 세션 녹화(Linux `script` 또는 AWS SSM Session Manager `StartSession`) 남기기
- 실행한 모든 명령을 인시던트 Slack 채널에 copy-paste

```bash
# SSM Session으로 DB 점프호스트 접근 (기본 권장)
aws ssm start-session --target {instance-id} \
  --document-name AWS-StartInteractiveCommand \
  --parameters 'command=["bash -l"]'
```

### 8.4 사후 감사 및 크레덴셜 재발급

**단계 1. 사용 내역 감사 (사용 후 24시간 내)**

```bash
# CloudTrail 에서 break-glass 사용자의 모든 이벤트 추출
aws cloudtrail lookup-events \
  --lookup-attributes AttributeKey=Username,AttributeValue=break-glass-{env} \
  --start-time $(date -u -v-1d +%Y-%m-%dT%H:%M:%SZ) \
  --max-results 200 \
  > /tmp/break-glass-audit-{incident-id}.json
```

SRE 리드 + 보안팀이 24시간 내 리뷰, 인시던트 이슈에 첨부.

**단계 2. 크레덴셜 즉시 로테이션**

```bash
# 1) AWS IAM Access Key 교체
aws iam create-access-key --user-name break-glass-{env}
aws iam update-access-key --user-name break-glass-{env} \
  --access-key-id {OLD} --status Inactive
aws iam delete-access-key --user-name break-glass-{env} --access-key-id {OLD}

# 2) Secrets Manager 신규 값 저장
aws secretsmanager put-secret-value \
  --secret-id breakglass/aws/{env} \
  --secret-string '{"accessKeyId":"{NEW}","secretAccessKey":"{NEW}"}'

# 3) Aurora break-glass 비밀번호 변경
aws rds modify-db-cluster \
  --db-cluster-identifier {env}-aurora-cluster \
  --master-user-password "{NEW}" --apply-immediately

# 4) MongoDB Atlas 비밀번호 변경
atlas dbusers update breakglass-atlas \
  --projectId {project-id} --password "{NEW}"
```

**단계 3. 개선 액션 기록**

- "왜 통상 경로로 불가했는가" 를 근본 원인으로 보고 SSO/런북/자동화 개선 액션을 포스트모템에 포함.
- 다음 분기 GameDay 에서 동일 시나리오 통상 경로로 복구 가능한지 재검증.

---

## 부록 A. 참고 공식 자료

- AWS Operational Readiness Reviews — https://docs.aws.amazon.com/wellarchitected/latest/operational-readiness-reviews/
- AWS Resilience Hub — https://docs.aws.amazon.com/resilience-hub/
- AWS Fault Injection Service (구 Fault Injection Simulator) — https://docs.aws.amazon.com/fis/latest/userguide/
- AWS CodeDeploy Deployment Configurations — https://docs.aws.amazon.com/codedeploy/latest/userguide/deployment-configurations.html
- Amazon RDS Best Practices — https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html
- Google SRE — Managing Incidents — https://sre.google/sre-book/managing-incidents/
- Google SRE — Postmortem Culture — https://sre.google/sre-book/postmortem-culture/
- NIST SP 800-61 Rev.3 (2025-04, Rev.2 supersede) — https://csrc.nist.gov/pubs/sp/800/61/r3/final

## 부록 B. 체크리스트 준수 현황

- [x] 모든 런북은 단계 번호 + 명령어 + 예상 출력 + 실패 시 조치 포함
- [x] 분기별 GameDay(AWS Fault Injection Service) 수행 정책 명시 (§5.5)
- [x] 포스트모템 Blameless 템플릿 제공 (§6.5)
- [x] 런북은 코드 레포에 커밋하고 PR 리뷰로 갱신 (본 문서 경로 `/devops/results/10-deployment-runbook.md`)
- [x] Actuator probe 미설정 상태 경고 및 검증 절차 명시 (§2.1 단계 3)
