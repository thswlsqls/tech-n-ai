# 06. 보안 · IAM · 시크릿 · WAF 설계

> 본 문서는 AWS Security Reference Architecture(SRA), CIS AWS Foundations Benchmark, NIST SP 800-61 Rev.2, OWASP ASVS를 기준으로 tech-n-ai 플랫폼(백엔드 6개 api-* 모듈 + 배치 1 + 프론트 2개)의 계정·IAM·시크릿·네트워크·데이터·탐지 영역을 단일 산출물로 통합 설계한 보안 설계서이다. 규제 범위는 대한민국 개인정보보호법 및 GDPR(해외 이용자 발생 시)을 포함한다.

---

## 1. 계정·정체성 (identity-and-account)

### 1.1 멀티 계정 전략 (AWS Organizations + Control Tower)

AWS SRA 권고에 따라 워크로드·보안·감사·로깅을 분리한 멀티 계정 구조를 채택한다. AWS Control Tower로 랜딩 존을 부트스트랩하고 Organizations의 SCP(Service Control Policy)로 예방적 가드레일을 강제한다.

#### 1.1.1 OU(Organizational Unit) 구조

```
Root Org (tech-n-ai)
├── Security OU
│   ├── Log Archive Account    # CloudTrail, Config, ALB/WAF 로그 중앙화 (Object Lock)
│   └── Audit Account          # Security Hub / GuardDuty / Macie 위임 관리자
├── Infrastructure OU
│   ├── Network Account        # Transit Gateway, Route 53 Resolver, 중앙 VPC Endpoints
│   └── Shared Services Account # ECR, SSM, CodeArtifact, Terraform State
├── Workloads OU
│   ├── Workloads-dev          # 개발 환경 (로컬/PR 배포)
│   ├── Workloads-beta         # 스테이징 (QA, 성능 시험)
│   └── Workloads-prod         # 운영 (고가용/고격리)
├── Sandbox OU                 # 실험용, 예산 한도 $100/계정, 자동 정리
└── Suspended OU               # 해지 예정 계정 격리 (SCP로 전면 차단)
```

#### 1.1.2 핵심 SCP 가드레일 (예방적 통제)

| SCP 이름                         | 적용 대상 OU                | 효과                                                                |
| -------------------------------- | --------------------------- | ------------------------------------------------------------------- |
| `DenyRegionExceptAllowed`        | 전체 OU                     | `ap-northeast-2`, `us-east-1`(글로벌 서비스) 외 리전 전면 차단      |
| `DenyRootUserActions`            | 전체 OU                     | 루트 사용자의 모든 액션 거부(초기 설정 이후)                        |
| `DenyLeaveOrganization`          | 전체 OU                     | `organizations:LeaveOrganization` 차단                              |
| `DenyDisableGuardDuty`           | 전체 OU                     | GuardDuty/SecurityHub/Config/CloudTrail 비활성화 차단               |
| `DenyIAMUserAccessKey`           | Workloads/Infrastructure OU | `iam:CreateAccessKey`, `iam:CreateUser` 차단(Identity Center 강제)  |
| `RequireIMDSv2`                  | Workloads OU                | `ec2:RunInstances` 시 `HttpTokens=required` 미지정 요청 거부        |
| `DenyS3PublicAccess`             | Workloads OU                | S3 Public ACL/Policy 설정 차단, Account Block Public Access 잠금    |
| `DenyKMSKeyDeletion`             | Workloads-prod              | `kms:ScheduleKeyDeletion`, `kms:DisableKey` 차단                    |
| `DenyCloudTrailTampering`        | 전체 OU                     | `cloudtrail:StopLogging`, `DeleteTrail`, `UpdateTrail` 거부         |
| `RequireTLSInTransit`            | Workloads OU                | S3/SQS/SNS 등 `aws:SecureTransport=false` 요청 거부                 |

### 1.2 로깅·감사 계정 분리

- **Log Archive Account**: 조직 CloudTrail, Config, VPC Flow Logs, ALB/CloudFront/WAF 로그 S3를 중앙 수집. 버킷은 **S3 Object Lock(Compliance 모드, 7년 보존)** + KMS CMK + Versioning. Log Archive 계정의 S3 관리자 외에는 Put/Delete 불가.
- **Audit Account**: Security Hub · GuardDuty · Macie · Access Analyzer · IAM Access Analyzer의 **Delegated Administrator**. 운영자는 여기에서 조직 전역 시야 확보.

### 1.3 인증 (AWS IAM Identity Center, 구 AWS SSO)

- **IdP 연동**: AWS IAM Identity Center(구 AWS Single Sign-On, SSO)를 ap-northeast-2 리전에 활성화. 외부 IdP는 Google Workspace(SAML 2.0 기준) 또는 Okta를 커넥터로 연결. 프로비저닝은 SCIM v2.0.
- **MFA 강제**: Identity Center 인증 정책에서 `Context-aware` 또는 `Always-on` MFA 적용. 가능하면 **FIDO2 보안 키(WebAuthn)** 의무화, TOTP는 보조.
- **Permission Set**(사람용 역할)
  - `OrganizationAdmin` — 관리 계정 한정, Break-glass 전용
  - `SecurityAudit` — Security OU 읽기 + SecurityAudit 관리형 정책
  - `PlatformEngineer` — Infrastructure/Workloads 계정 운영 권한
  - `DeveloperReadOnly` — Workloads-dev/beta 읽기
  - `DeveloperDev` — Workloads-dev 쓰기(CloudWatch/Logs/ECS Exec)
  - `OnCallSRE` — Workloads-prod 제한 쓰기(ECS Exec, RDS Describe, SSM 세션)
  - `DataAnalyst` — Athena/Glue 읽기, S3 데이터 레이크 read-only
- **세션 만료**: 최대 8시간, 운영(prod)은 2시간.
- **액세스 키 금지**: IAM User/Access Key 생성은 SCP로 차단. 예외는 레거시 툴 한정 + 90일 내 제거 플랜.

### 1.4 워크로드 아이덴티티 원칙

| 워크로드                         | 사용 원칙                                                                                         |
| -------------------------------- | ------------------------------------------------------------------------------------------------- |
| ECS Fargate Task                 | **ECS Task Role**(`sts:AssumeRole` by `ecs-tasks.amazonaws.com`), 작업별 최소 권한                |
| EKS Pod (확장 대비)              | **IRSA**(IAM Roles for Service Accounts), OIDC Provider + ServiceAccount 매핑                     |
| EC2 (Jumphost, 배치 예외)        | **Instance Profile**, IMDSv2 강제, 세션 매니저 전용                                               |
| Lambda (글루 코드)               | 실행 역할 + VPC 구성 + KMS 조건부 접근                                                            |
| GitHub Actions                   | **GitHub OIDC Provider → IAM Role**(`sts:AssumeRoleWithWebIdentity`) — 장기 키 금지               |
| Developer/Operator               | Identity Center Permission Set → `sts:AssumeRole` chain                                           |

GitHub OIDC Trust 정책 예시(정확한 `sub` 조건으로 리포지토리·브랜치·환경 제한):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
        "token.actions.githubusercontent.com:sub": "repo:tech-n-ai/tech-n-ai-backend:environment:prod"
      }
    }
  }]
}
```

### 1.5 Break-glass 계정 절차

- 관리 계정에 **`break-glass-root`**(루트 사용자)와 **`break-glass-admin`** IAM 사용자 2개만 생성(Identity Center 장애 대비).
- 루트 MFA는 **FIDO2 하드웨어 키 2개**(본/부) — 물리 금고 보관, 봉인 후 CISO·CTO 2인 공동 관리.
- 비밀번호는 1Password Business의 Vault(접근 로그 기록) + 오프라인 봉인 종이 백업.
- 사용 절차: 장애 선언 → CISO 승인 → 봉인 해제 → CloudTrail/Organization Events 실시간 알림 → 사용 후 24h 내 **비밀번호·MFA 디바이스 재발급** + 사후 보고서 작성.
- 분기 1회 모의 훈련, 연 1회 봉인 교체.

---

## 2. IAM 정책 (iam-policies)

### 2.1 정책 작성 원칙

- **`Resource: "*"` 절대 금지**(읽기 전용 메타 액션 예외만 허용, 해당 시 Condition 필수).
- **조건 키 기본 장착**: `aws:SourceVpc`, `aws:SourceVpce`, `aws:PrincipalTag/*`, `aws:RequestTag/*`, `aws:SecureTransport`, `aws:ResourceTag/*`.
- **태그 기반 ABAC**: Task Role에 `PrincipalTag/service=api-auth` 부여, 리소스에 `service=api-auth` 태그 → 조건으로 일치 강제.
- **IAM Access Analyzer**: 정책 검증(Validation) + 정책 생성(Policy Generation) + 외부 액세스 분석을 CI 파이프라인에 포함.
- **Boundary**: 개발자 생성 역할에는 `PermissionsBoundary` 부착하여 권한 상한 제한.

### 2.2 백엔드 모듈별 ECS Task Role 스켈레톤

공통 신뢰 정책(AssumeRole):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Service": "ecs-tasks.amazonaws.com" },
    "Action": "sts:AssumeRole",
    "Condition": {
      "ArnLike": { "aws:SourceArn": "arn:aws:ecs:ap-northeast-2:<ACCOUNT>:*" },
      "StringEquals": { "aws:SourceAccount": "<ACCOUNT>" }
    }
  }]
}
```

#### 2.2.1 `api-gateway` (포트 8081) — 라우팅 전용, 최소 권한

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadGatewayConfigSSM",
      "Effect": "Allow",
      "Action": ["ssm:GetParameter","ssm:GetParameters","ssm:GetParametersByPath"],
      "Resource": "arn:aws:ssm:ap-northeast-2:<ACCOUNT>:parameter/techai/${aws:PrincipalTag/env}/api-gateway/*",
      "Condition": { "StringEquals": { "aws:SourceVpc": "vpc-<id>" } }
    },
    {
      "Sid": "CWLogs",
      "Effect": "Allow",
      "Action": ["logs:CreateLogStream","logs:PutLogEvents"],
      "Resource": "arn:aws:logs:ap-northeast-2:<ACCOUNT>:log-group:/ecs/api-gateway:*"
    },
    {
      "Sid": "XRay",
      "Effect": "Allow",
      "Action": ["xray:PutTraceSegments","xray:PutTelemetryRecords"],
      "Resource": "*",
      "Condition": { "StringEquals": { "aws:SourceVpc": "vpc-<id>" } }
    }
  ]
}
```

#### 2.2.2 `api-auth` (8083) — JWT 서명키, OAuth 시크릿, Aurora 접근

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadAuthSecrets",
      "Effect": "Allow",
      "Action": ["secretsmanager:GetSecretValue","secretsmanager:DescribeSecret"],
      "Resource": [
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/auth/jwt-*",
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/auth/oauth-google-*",
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/auth/oauth-slack-*",
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/aurora/auth-*"
      ],
      "Condition": { "StringEquals": { "aws:SourceVpc": "vpc-<id>" } }
    },
    {
      "Sid": "KMSDecryptAuth",
      "Effect": "Allow",
      "Action": ["kms:Decrypt","kms:DescribeKey"],
      "Resource": "arn:aws:kms:ap-northeast-2:<ACCOUNT>:key/<auth-cmk-id>",
      "Condition": {
        "StringEquals": {
          "kms:ViaService": [
            "secretsmanager.ap-northeast-2.amazonaws.com",
            "ssm.ap-northeast-2.amazonaws.com"
          ]
        }
      }
    },
    {
      "Sid": "RDSIAMAuthAurora",
      "Effect": "Allow",
      "Action": ["rds-db:connect"],
      "Resource": "arn:aws:rds-db:ap-northeast-2:<ACCOUNT>:dbuser:<cluster-resource-id>/api_auth_app"
    },
    {
      "Sid": "CWLogs",
      "Effect": "Allow",
      "Action": ["logs:CreateLogStream","logs:PutLogEvents"],
      "Resource": "arn:aws:logs:ap-northeast-2:<ACCOUNT>:log-group:/ecs/api-auth:*"
    }
  ]
}
```

#### 2.2.3 `api-emerging-tech` (8082) / `api-bookmark` (8085) — 조회 중심

- SSM Parameter Store 읽기(`/techai/{env}/{service}/*`), Aurora IAM 인증(`api-bookmark`), X-Ray, CloudWatch Logs.
- **MSK 권한은 현재 미부여**: `envs/prod/task_roles.tf` 기준 `api-bookmark`/`api-emerging-tech` 모두 `kafka-cluster:*` 미포함. Kafka 컨슈머 도입 시 아래 정책을 task_roles 에 추가.

```json
{
  "Sid": "MSKIAMAuth",
  "Effect": "Allow",
  "Action": ["kafka-cluster:Connect","kafka-cluster:DescribeCluster",
             "kafka-cluster:ReadData","kafka-cluster:WriteData",
             "kafka-cluster:DescribeTopic","kafka-cluster:DescribeGroup",
             "kafka-cluster:AlterGroup"],
  "Resource": [
    "arn:aws:kafka:ap-northeast-2:<ACCOUNT>:cluster/techai-${aws:PrincipalTag/env}-msk/*",
    "arn:aws:kafka:ap-northeast-2:<ACCOUNT>:topic/techai-${aws:PrincipalTag/env}-msk/*/tech-n-ai.*",
    "arn:aws:kafka:ap-northeast-2:<ACCOUNT>:group/techai-${aws:PrincipalTag/env}-msk/*/${aws:PrincipalTag/service}-*"
  ]
}
```

#### 2.2.4 `api-chatbot` (8084) — RAG, OpenAI Key, MongoDB Atlas, S3 업로드

> **현재 IaC 상태** (`envs/prod/task_roles.tf:99-137`): OpenAI Key + MongoDB URI 시크릿 + KMS(`ai`) 키만 부여. **Bedrock 권한 미부여 (D-12 결정)** — langchain4j 가 OpenAI 만 사용 중. Cohere/S3 권한도 현재 미부여, 실제 도입 시 아래 정책을 task_roles 에 추가.

```json
{
  "Statement": [
    {
      "Sid": "ChatbotSecrets",
      "Effect": "Allow",
      "Action": ["secretsmanager:GetSecretValue","secretsmanager:DescribeSecret"],
      "Resource": [
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/openai-api-key-*",
        "arn:aws:secretsmanager:ap-northeast-2:<ACCOUNT>:secret:techai/${aws:PrincipalTag/env}/mongodb-uri-*"
      ],
      "Condition": { "StringEquals": { "aws:SourceVpc": "vpc-<id>" } }
    },
    {
      "Sid": "S3UploadBucketScoped",
      "Comment": "도입 시 추가 — 현재 IaC 미부여",
      "Effect": "Allow",
      "Action": ["s3:PutObject","s3:GetObject","s3:AbortMultipartUpload"],
      "Resource": "arn:aws:s3:::techai-${aws:PrincipalTag/env}-chatbot-uploads/*",
      "Condition": {
        "StringEquals": { "s3:x-amz-server-side-encryption": "aws:kms" },
        "Bool": { "aws:SecureTransport": "true" }
      }
    },
    {
      "Sid": "BedrockInvokeOptional",
      "Comment": "ADR 승인 후 추가 — D-12 결정으로 현재 미부여",
      "Effect": "Allow",
      "Action": ["bedrock:InvokeModel","bedrock:InvokeModelWithResponseStream"],
      "Resource": [
        "arn:aws:bedrock:ap-northeast-2::foundation-model/anthropic.claude-*",
        "arn:aws:bedrock:ap-northeast-2::foundation-model/amazon.titan-embed-*"
      ]
    }
  ]
}
```

#### 2.2.5 `api-agent` (8086) — 외부 도구 호출, SQS/SNS, Step Functions(옵션)

- SQS 큐: `arn:aws:sqs:ap-northeast-2:<ACCOUNT>:techai-${env}-agent-*` 로 제한.
- Step Functions `states:StartExecution` — 특정 State Machine ARN에 한정.
- Outbound 외부 API 키는 Secrets Manager에서만 조회.

### 2.3 배치 Job Role (`batch-scheduler`)

```json
{
  "Statement": [
    {
      "Sid": "ReadBatchConfig",
      "Effect": "Allow",
      "Action": ["ssm:GetParameter","ssm:GetParametersByPath"],
      "Resource": "arn:aws:ssm:ap-northeast-2:<ACCOUNT>:parameter/techai/${aws:PrincipalTag/env}/batch/*"
    },
    {
      "Sid": "AuroraIAMConnect",
      "Effect": "Allow",
      "Action": ["rds-db:connect"],
      "Resource": "arn:aws:rds-db:ap-northeast-2:<ACCOUNT>:dbuser:<cluster-resource-id>/batch_app"
    },
    {
      "Sid": "S3DataLake",
      "Effect": "Allow",
      "Action": ["s3:GetObject","s3:PutObject","s3:ListBucket"],
      "Resource": [
        "arn:aws:s3:::techai-${aws:PrincipalTag/env}-datalake",
        "arn:aws:s3:::techai-${aws:PrincipalTag/env}-datalake/*"
      ],
      "Condition": { "Bool": { "aws:SecureTransport": "true" } }
    },
    {
      "Sid": "EventBridgeReport",
      "Effect": "Allow",
      "Action": ["events:PutEvents"],
      "Resource": "arn:aws:events:ap-northeast-2:<ACCOUNT>:event-bus/techai-${aws:PrincipalTag/env}-bus"
    }
  ]
}
```

### 2.4 CI/CD 배포 Role (GitHub Actions OIDC) — 권한 분리

#### 2.4.1 `gha-ecr-push`

```json
{
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["ecr:GetAuthorizationToken"],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability","ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart","ecr:CompleteLayerUpload","ecr:PutImage",
        "ecr:DescribeRepositories","ecr:BatchGetImage"
      ],
      "Resource": [
        "arn:aws:ecr:ap-northeast-2:<ACCOUNT>:repository/techai/api-*",
        "arn:aws:ecr:ap-northeast-2:<ACCOUNT>:repository/techai/batch-*",
        "arn:aws:ecr:ap-northeast-2:<ACCOUNT>:repository/techai/frontend-*"
      ]
    }
  ]
}
```

#### 2.4.2 `gha-ecs-deploy`

```json
{
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecs:UpdateService","ecs:DescribeServices","ecs:DescribeTasks",
        "ecs:RegisterTaskDefinition","ecs:DescribeTaskDefinition","ecs:ListTasks"
      ],
      "Resource": [
        "arn:aws:ecs:ap-northeast-2:<ACCOUNT>:service/techai-${aws:PrincipalTag/env}/*",
        "arn:aws:ecs:ap-northeast-2:<ACCOUNT>:task-definition/techai-*:*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": ["iam:PassRole"],
      "Resource": [
        "arn:aws:iam::<ACCOUNT>:role/techai-${env}-task-api-*",
        "arn:aws:iam::<ACCOUNT>:role/techai-${env}-task-execution"
      ],
      "Condition": {
        "StringEquals": { "iam:PassedToService": "ecs-tasks.amazonaws.com" }
      }
    }
  ]
}
```

#### 2.4.3 `gha-terraform-apply` (Workloads 각 계정에 분리 배치)

- 인프라 리소스 생성/변경 권한은 이 역할에 한정. `ecs:UpdateService`와는 별도.
- `iam:*` 권한은 `PermissionsBoundary` 조건으로 제한(개발자 생성 Role에 상한 강제).
- 조건 키 `aws:RequestTag/managed-by=terraform` 강제.

```json
{
  "Statement": [
    {
      "Sid": "TerraformCoreInfra",
      "Effect": "Allow",
      "NotAction": ["iam:*","organizations:*","account:*","aws-portal:*"],
      "Resource": "*",
      "Condition": {
        "StringEquals": { "aws:RequestedRegion": ["ap-northeast-2","us-east-1"] }
      }
    },
    {
      "Sid": "IAMWithBoundary",
      "Effect": "Allow",
      "Action": ["iam:CreateRole","iam:AttachRolePolicy","iam:PutRolePolicy","iam:CreatePolicy"],
      "Resource": "arn:aws:iam::<ACCOUNT>:role/techai/*",
      "Condition": {
        "StringEquals": {
          "iam:PermissionsBoundary": "arn:aws:iam::<ACCOUNT>:policy/techai-developer-boundary"
        }
      }
    }
  ]
}
```

### 2.5 Terraform 상태 관리 Role (`tfstate-access`)

```json
{
  "Statement": [
    {
      "Sid": "StateBucketRW",
      "Effect": "Allow",
      "Action": ["s3:GetObject","s3:PutObject","s3:DeleteObject"],
      "Resource": "arn:aws:s3:::techai-tfstate-<ACCOUNT>/*",
      "Condition": {
        "Bool": { "aws:SecureTransport": "true" },
        "StringEquals": { "s3:x-amz-server-side-encryption": "aws:kms" }
      }
    },
    {
      "Sid": "StateBucketList",
      "Effect": "Allow",
      "Action": ["s3:ListBucket","s3:GetBucketVersioning"],
      "Resource": "arn:aws:s3:::techai-tfstate-<ACCOUNT>"
    },
    {
      "Sid": "LockTable",
      "Effect": "Allow",
      "Action": ["dynamodb:GetItem","dynamodb:PutItem","dynamodb:DeleteItem","dynamodb:DescribeTable"],
      "Resource": "arn:aws:dynamodb:ap-northeast-2:<ACCOUNT>:table/techai-tfstate-lock"
    },
    {
      "Sid": "KMSForState",
      "Effect": "Allow",
      "Action": ["kms:Encrypt","kms:Decrypt","kms:GenerateDataKey"],
      "Resource": "arn:aws:kms:ap-northeast-2:<ACCOUNT>:key/<tfstate-cmk>"
    }
  ]
}
```

---

## 3. 시크릿 관리 (secrets-management)

### 3.1 Secrets Manager vs SSM Parameter Store 분리 기준

| 구분             | AWS Secrets Manager                                                                                       | SSM Parameter Store                                                                                       |
| ---------------- | --------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| 주 용도          | 로테이션이 필요한 **민감한 자격증명**(DB, API Key, JWT Secret, OAuth Client Secret)                       | 로테이션 불필요한 **구성값**(Feature flag, 엔드포인트 URL, timeout, 로그 레벨)                            |
| 자동 로테이션    | 지원(Lambda 기반)                                                                                         | 미지원                                                                                                    |
| 버전/스테이지    | `AWSCURRENT`/`AWSPREVIOUS`, 스테이지 라벨                                                                 | 버전 번호(정책 기반)                                                                                      |
| 암호화           | KMS CMK 필수                                                                                              | SecureString일 때 KMS                                                                                     |
| 가격             | 시크릿당 월 $0.40 + API 호출당 과금                                                                       | Standard 무료 / Advanced $0.05/월                                                                         |
| 선택 원칙        | **모든 시크릿** 기본값                                                                                    | 민감도 낮은 **환경 구성**                                                                                 |

### 3.2 관리 대상 시크릿 목록

> 실제 IaC(`envs/prod/task_roles.tf`)에서 참조하는 패턴은 도메인 segment 없는 **`{project}/{env}/{secret-name}-*`** 형식이다(예: `techai/prod/openai-api-key-*`, `techai/prod/jwt-signing-key-*`, `techai/prod/mongodb-uri-*`, `techai/prod/elasticache-auth-token-*`). 아래 표의 `aurora/`, `chatbot/`, `auth/` 경로는 **신규 시크릿 추가 시 분류 권장 명세**이며, 기존 시크릿 명은 위 actual ARN 패턴을 우선 따른다.

| 시크릿 ID 패턴                                                    | 대상                                | 저장소           | KMS 키                      | 로테이션                    |
| ----------------------------------------------------------------- | ----------------------------------- | ---------------- | --------------------------- | --------------------------- |
| `techai/{env}/aurora-{service}` (또는 RDS Managed master secret)  | Aurora DB 자격증명(서비스별 사용자) | Secrets Manager  | `cmk-{env}-data`            | 30일 자동(RDS 통합)         |
| `techai/{env}/mongodb-uri` *(현행)*                               | MongoDB Atlas 연결 문자열(SRV+cred) | Secrets Manager  | `cmk-{env}-ai`/`-data`      | 90일 수동(Atlas API)        |
| `techai/{env}/openai-api-key` *(현행)*                            | OpenAI API Key                      | Secrets Manager  | `cmk-{env}-ai`              | 90일 수동                   |
| `techai/{env}/cohere-api-key`                                     | Cohere API Key (도입 시)            | Secrets Manager  | `cmk-{env}-ai`              | 90일 수동                   |
| `techai/{env}/jwt-signing-key` *(현행, active/next 듀얼)*          | JWT 서명키(HS512) 또는 RSA KeyPair  | Secrets Manager  | `cmk-{env}-auth`            | 180일(무중단: kid 롤오버)   |
| `techai/{env}/oauth-google`                                       | Google OAuth client secret          | Secrets Manager  | `cmk-{env}-auth`            | 365일 수동                  |
| `techai/{env}/oauth-slack`                                        | Slack OAuth client secret           | Secrets Manager  | `cmk-{env}-auth`            | 365일 수동                  |
| `techai/{env}/elasticache-auth-token` *(현행)*                    | ElastiCache AUTH 토큰               | Secrets Manager  | `cmk-{env}-data`            | 90일 수동                   |
| `techai/{env}/msk-sasl-scram`                                     | Kafka SASL/SCRAM 자격(IAM 미사용 시; 현 IaC 미사용) | Secrets Manager  | `cmk-{env}-data`            | 60일 자동                   |
| `/techai/{env}/{service}/application.yml-override`                | 비민감 설정값                       | SSM Parameter    | -                           | -                           |
| `/techai/{env}/common/cors-origins`                               | CORS 허용 도메인                    | SSM Parameter    | -                           | -                           |

### 3.3 자동 로테이션 정책

- **Aurora**: Secrets Manager ↔ RDS 네이티브 통합, 멀티 유저 로테이션(싱글 유저보다 다운타임 0). 주기 **30일**.
- **JWT 서명키**: kid 기반 듀얼 키 운영 — `active` + `next`. 180일마다 `next` 생성 → 24시간 관용 기간 → `active` 교체. 검증 측은 양쪽 kid 수용.
- **OpenAI/Cohere**: 외부 API이므로 Secrets Manager Lambda(커스텀)에서 플랫폼 콘솔 API 호출 불가 → **분기 수동 로테이션 + 캘린더 알림(EventBridge Scheduler)** + 만료 30일 전 Slack 알림.
- **MongoDB Atlas**: Atlas Programmatic API로 임시 DB user 재생성 → 시크릿 업데이트 → 구 user 삭제(Lambda, 90일).
- **MSK SASL/SCRAM**: Secrets Manager Lambda + `kafka:BatchAssociateScramSecret` 연계, 60일.

### 3.4 Spring Boot 연동

두 가지 방식 비교:

| 항목                   | `spring-cloud-aws-secrets-manager`(권장)                        | 환경변수(ECS `secrets` 블록)                                  |
| ---------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------- |
| 갱신                   | 애플리케이션 재기동 없이 refresh(Actuator `refresh`) 가능        | 컨테이너 재기동 필요                                           |
| 코드 의존              | Starter 필요                                                     | 코드 변경 없음                                                 |
| 경로                   | `spring.config.import=aws-secretsmanager:techai/${env}/...`      | Task Definition `secrets[].valueFrom=<ARN>`                    |
| 권한                   | Task Role `secretsmanager:GetSecretValue` 필요                    | Task **Execution** Role에 권한 필요(기동 시 주입)              |
| 캐시                   | 클라이언트 사이드 TTL                                            | -                                                              |
| 선택                   | **백엔드 모듈 기본**(JWT kid 롤오버 대응)                         | 프론트 Next.js 런타임 ENV, 초기 부트스트랩 전용                |

- `api-auth`는 kid 롤오버 대응 위해 Spring Cloud AWS + `@RefreshScope` 필수.
- 전파: 로테이션 후 EventBridge → ECS Service 태스크 SIGTERM 롤링 or `/actuator/refresh` hit.

### 3.5 KMS CMK 키 계층 및 키 정책

환경(env) × 도메인 축으로 CMK 분리:

```
cmk-{env}-auth       # api-auth, OAuth, JWT
cmk-{env}-data       # Aurora, MongoDB, MSK, EBS, ElastiCache
cmk-{env}-ai         # OpenAI/Cohere, Atlas Vector 업로드 S3
cmk-{env}-logs       # CloudWatch Logs, CloudTrail, VPC Flow Logs
cmk-{env}-s3-app     # 애플리케이션 S3(uploads, datalake)
cmk-tfstate          # Terraform State 전용(관리 계정)
```

키 정책 원칙:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EnableRootAccount",
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<ACCOUNT>:root" },
      "Action": "kms:*",
      "Resource": "*"
    },
    {
      "Sid": "KeyAdmins",
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<ACCOUNT>:role/aws-reserved/sso.amazonaws.com/AWSReservedSSO_SecurityAudit_*" },
      "Action": ["kms:Describe*","kms:List*","kms:GetKeyPolicy","kms:PutKeyPolicy","kms:EnableKeyRotation"],
      "Resource": "*"
    },
    {
      "Sid": "AllowServiceUseViaSecretsManager",
      "Effect": "Allow",
      "Principal": { "AWS": "arn:aws:iam::<ACCOUNT>:role/techai-${env}-task-api-auth" },
      "Action": ["kms:Decrypt","kms:DescribeKey"],
      "Resource": "*",
      "Condition": {
        "StringEquals": { "kms:ViaService": "secretsmanager.ap-northeast-2.amazonaws.com" }
      }
    },
    {
      "Sid": "DenyWithoutTLS",
      "Effect": "Deny",
      "Principal": "*",
      "Action": "kms:*",
      "Resource": "*",
      "Condition": { "Bool": { "aws:SecureTransport": "false" } }
    }
  ]
}
```

- **연 1회 자동 회전** 활성화(`EnableKeyRotation`).
- Workloads-prod의 CMK는 SCP로 삭제·비활성화 차단.
- 다중 리전 복제는 재해 복구 요건 충족 시 Multi-Region Key.

### 3.6 하드코딩 시크릿 0건 정책

- **Pre-commit**: `pre-commit` 훅 + `detect-secrets` 또는 `git-secrets` 설치 스크립트 제공.
- **CI 스캔**: GitHub Actions에서 PR마다 **TruffleHog v3**(`trufflesecurity/trufflehog-actions-scan`) + **gitleaks** 병행 실행 → 발견 시 fail.
- **히스토리 스캔**: 월 1회 `trufflehog git file://... --since-commit=...` 전체 스캔.
- **발견 시 프로세스**: 해당 시크릿 즉시 무효화 → 로테이션 → `git filter-repo` 또는 BFG로 히스토리 purge → 포스트모템.
- **IDE**: Secret Lens(VSCode) / IntelliJ Secrets Plugin 권장.

---

## 4. 네트워크 보안 (network-security)

### 4.1 AWS WAF v2 규칙셋 (CloudFront + ALB 적용)

Web ACL 2개: `techai-{env}-cdn-acl`(CloudFront, us-east-1) / `techai-{env}-alb-acl`(ALB, ap-northeast-2).

| 우선순위 | 규칙                                                        | 동작      | 비고                                                                         |
| -------- | ----------------------------------------------------------- | --------- | ---------------------------------------------------------------------------- |
| 0        | `AWSManagedRulesAmazonIpReputationList`                     | Block     | AWS 평판 기반 악성 IP 차단                                                   |
| 1        | `AWSManagedRulesAnonymousIpList`                            | Block     | VPN/Tor/Proxy (API 엔드포인트 한정)                                          |
| 2        | `AWSManagedRulesKnownBadInputsRuleSet`                      | Block     | Log4Shell 등 알려진 악성 페이로드                                            |
| 3        | `AWSManagedRulesSQLiRuleSet`                                | Block     | SQL Injection                                                                |
| 4        | `AWSManagedRulesCommonRuleSet`(CRS)                         | Block     | OWASP Top 10 커버(`SizeRestrictions_BODY` 예외: 멀티파트 업로드)             |
| 5        | `AWSManagedRulesLinuxRuleSet`                               | Block     | LFI/Path Traversal (리눅스 기반 ECS)                                         |
| 6        | `AWSManagedRulesUnixRuleSet`                                | Block     | 셸 명령 인젝션                                                               |
| 7        | `AWSManagedRulesATPRuleSet`(Account Takeover Prevention)    | Block     | `/api/auth/login`, `/api/auth/register` 한정 — 크리덴셜 스터핑 방어         |
| 8        | `AWSManagedRulesBotControlRuleSet`(Targeted)                | Count→Block | 초기 Count 운영 후 Block. 챌린지/캡차 연계. **추가 과금 발생**(Targeted는 ML 분석으로 단가 ↑) — Scope-down statement로 인증/주문 등 핵심 경로에만 적용 권장 ([WAF Pricing](https://aws.amazon.com/waf/pricing/)) |
| 9        | Custom: Rate-based(IP)                                      | Block     | 5분 창 **2000 req/IP**(일반), `/api/auth/*` **100 req/IP**                   |
| 10       | Custom: Geo-match                                           | Allow/Block | KR/US/JP/SG/EU Allow, 그 외 기본 Block(국제 서비스 단계별 확장)              |
| 11       | Custom: 관리자 엔드포인트 IP 화이트리스트(`/admin/*`)       | Allow     | 오피스/VPN CIDR만                                                            |
| 12       | Custom: Header `User-Agent` 공란 차단                       | Block     | 자동화 스크립트 1차 필터                                                     |
| Default  | -                                                           | Allow     |                                                                              |

- **로그**: Kinesis Firehose → Log Archive 계정 S3(파티션: `env/year/month/day/hour`) + Athena 쿼리.
- **CAPTCHA/Challenge**: ATP/Bot Control과 결합, UX 점검 후 점진 적용.
- **예외 관리**: IaC로 관리, 모든 예외는 `Issue ID`, `만료일` 태그 필수.

### 4.2 AWS Shield

- **Shield Standard**: 기본 활성(무료). L3/L4 자동 방어.
- **Shield Advanced**: **Workloads-prod의 CloudFront/ALB/Route 53/Global Accelerator**에 적용. DRT(DDoS Response Team) 연락 채널 확보, 비용 보호(Cost Protection), Enhanced Metrics.
- **준비 사항**: Health-based Detection(Route 53 헬스체크 연동), 실시간 Attack Metric → CloudWatch → PagerDuty.

### 4.3 TLS 및 인증서

- **ACM(AWS Certificate Manager)**: 도메인별 인증서 발급(DNS 검증), 자동 갱신.
- **TLS 최소 버전**: **TLSv1.2_2021** 이상(ALB/CloudFront). TLS 1.3 선호. TLS 1.0/1.1 차단.
- **HSTS**: `max-age=63072000; includeSubDomains; preload`.
- **Cipher**: AWS recommended security policy (`ELBSecurityPolicy-TLS13-1-2-2021-06`, CloudFront `TLSv1.2_2021`).
- **내부 통신**: 서비스 메시 시 mTLS(ACM Private CA) 옵션, 초기에는 SG + VPC 수준 격리.

### 4.4 CloudFront Response Headers Policy

```
Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
Content-Security-Policy:
  default-src 'self';
  script-src 'self' 'nonce-{generated}' https://cdn.jsdelivr.net;
  style-src 'self' 'unsafe-inline';
  img-src 'self' data: https://*.amazonaws.com;
  connect-src 'self' https://api.tech-n-ai.com https://*.openai.com;
  frame-ancestors 'none';
  form-action 'self';
  base-uri 'self';
  object-src 'none';
  upgrade-insecure-requests
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), camera=(), microphone=(), payment=()
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Resource-Policy: same-site
Cross-Origin-Embedder-Policy: require-corp
```

- `admin`(3001) 앱은 `frame-ancestors 'none'` 엄격 유지, SSO 팝업 시 예외 별도 정책.
- CSP는 **Report-Only → Enforce** 2주 관찰 기간.

### 4.5 Edge / Origin 보호 추가

- CloudFront → ALB: **Origin Custom Header**(비밀값) + ALB 리스너 룰에서 해당 헤더 강제 → Origin 우회 차단.
- VPC 내부는 **Security Group 참조** + **PrivateLink**(필요 시 Atlas PrivateLink)로 인터넷 노출 최소화.

---

## 5. 데이터 보호 (data-protection)

### 5.1 KMS 키 계층

| 계층              | 키                  | 용도                                                              |
| ----------------- | ------------------- | ----------------------------------------------------------------- |
| AWS Managed       | `aws/s3`, `aws/rds` | 임시/테스트, 저민감 리소스                                        |
| Customer Managed  | `cmk-{env}-*`(위)   | 운영 모든 스토리지/시크릿/로그 — 키 정책·회전 관리 주체 내재화    |
| Multi-Region Key  | `cmk-prod-dr-*`     | DR 리전 복제 필요 데이터(선택)                                    |

### 5.2 저장(at-rest) 암호화 매트릭스

| 리소스                     | 암호화 방식                 | 키                  | 비고                                                |
| -------------------------- | --------------------------- | ------------------- | --------------------------------------------------- |
| S3 (app uploads)           | SSE-KMS(`bucket-key` on)    | `cmk-{env}-s3-app`  | Block Public Access, Object Lock(규제 시)           |
| S3 (logs)                  | SSE-KMS                     | `cmk-{env}-logs`    | Object Lock Compliance 7y                           |
| Aurora MySQL               | Storage Encryption(KMS)     | `cmk-{env}-data`    | 스냅샷/백업 동일 키, IAM 인증 활성                  |
| ElastiCache Redis          | At-rest Encryption + AUTH   | `cmk-{env}-data`    | Transit 암호화 동시 활성                            |
| MSK                        | At-rest Encryption(KMS)     | `cmk-{env}-data`    | TLS in-transit, IAM 인증                            |
| EBS(ECS 호스트/Jumphost)   | EBS Encryption 기본 활성    | `cmk-{env}-data`    | Account default encryption 강제                     |
| EFS(필요 시)               | EFS Encryption(KMS)         | `cmk-{env}-data`    |                                                     |
| CloudWatch Logs            | Log Group KMS               | `cmk-{env}-logs`    |                                                     |
| Secrets Manager/SSM        | KMS                         | 도메인별 CMK        |                                                     |
| MongoDB Atlas              | Atlas at-rest encryption + BYOK(KMS) | Atlas용 KMS Key | Atlas KMS 통합, VPC Peering 또는 PrivateLink     |

### 5.3 전송(in-transit) 암호화 매트릭스

| 구간                                  | 프로토콜/방식                                              |
| ------------------------------------- | ---------------------------------------------------------- |
| Client ↔ CloudFront                   | TLS 1.2+ (HSTS, 1.3 선호)                                  |
| CloudFront ↔ ALB                      | TLS 1.2+, 커스텀 헤더 인증                                 |
| ALB ↔ ECS Task                        | TLS(애플리케이션 지원 시) 또는 VPC 내 SG 격리              |
| ECS ↔ Aurora                          | TLS(RDS CA) + IAM 인증                                     |
| ECS ↔ Atlas                           | TLS + PrivateLink                                          |
| ECS ↔ MSK                             | TLS + IAM SASL(or SCRAM)                                   |
| ECS ↔ ElastiCache                     | TLS + AUTH token                                           |
| Inter-VPC / Inter-Region              | Transit Gateway + IPsec(필요 시) 또는 VPC Peering + 암호화 |
| ECS ↔ AWS API                         | VPC Endpoints + TLS                                        |

### 5.4 Macie 적용

- **대상 버킷**: `techai-*-datalake`, `techai-*-chatbot-uploads`, `techai-*-user-exports`, `techai-*-logs`(샘플링).
- **Job**: 월 1회 Sensitive Data Discovery(국내 주민등록번호/휴대폰/이메일/계좌번호 Custom Data Identifier 포함).
- **Custom Data Identifier 예시**: 주민등록번호 `\b\d{6}-\d{7}\b`, 전화번호, 카드번호(PAN+Luhn).
- **발견 시 파이프라인**: Macie Finding → EventBridge → SNS(SecOps) → Jira 티켓 자동 생성.

### 5.5 개인정보 처리 (KISA 가이드 + GDPR)

- **수집·이용 최소화**: PII 테이블은 `pii_*` 접미/접두로 명시, ORM 어노테이션 `@Pii` 커스텀으로 로깅 시 자동 마스킹.
- **저장 규정**:
  - 주민등록번호는 **수집 금지** 원칙, 필요 시 대체 수단(i-PIN, CI/DI, 휴대폰 본인확인).
  - 비밀번호 BCrypt(cost ≥ 12) + 별도 salt.
  - 이메일/휴대폰 등 로그인 식별자는 AES-256-GCM 결정적 암호화(검색 가능) 또는 Hash(HMAC-SHA256) + 별도 salt.
- **마스킹**: 로그/응답에서 `user@example.com` → `u***@example.com`, 전화번호 중간 4자리 `***`. 공통 Spring AOP로 처리.
- **익명화/가명화**: 분석 파이프라인에 사용자 ID → Tokenization(Vaultless, HMAC + key). 배치에서 90일 지난 로그 익명화.
- **보존·파기**: 회원 탈퇴 시 30일 이내 파기(법정 보존 의무 데이터는 별도 테이블 격리, 파기 주기 기록). AWS Backup Lifecycle로 스냅샷 보존 기한 강제.
- **권리 대응(GDPR Art. 15–22)**: 열람/정정/삭제/이동 요청 API — `DELETE /me`(soft delete + PII 마스킹 + 14일 후 하드 삭제), `GET /me/export`(JSON+CSV).
- **국외 이전(GDPR Chapter V)**: OpenAI/Cohere 등 국외 처리자 목록 공개, SCCs(표준계약조항) 체결, DPA(Data Processing Addendum) 보관.
- **개인정보보호책임자(CPO)** 지정, 개인정보 처리방침 공개, 로그 열람 이력 보관(국내법 2~5년).

---

## 6. 탐지·대응 (detection-and-response)

### 6.1 GuardDuty

- **조직 위임**: Audit 계정을 **Delegated Administrator**, 전 계정 자동 등록, **ap-northeast-2 + us-east-1 + 기타 필수 리전 활성**.
- **보호 기능**:
  - Foundational Threat Detection(기본)
  - **S3 Protection** 활성
  - **EKS Protection**(확장 시), **Runtime Monitoring** for ECS Fargate/EC2
  - **Malware Protection for EC2**(EBS 스냅샷 스캔, 의심 findings 트리거)
  - **Malware Protection for S3**(객체 업로드 시 스캔, `techai-*-chatbot-uploads` 대상)
  - **RDS Protection** for Aurora
  - **Lambda Protection**
- **알림**: GuardDuty Finding → EventBridge → SNS(SecOps) + Slack(`#sec-alerts`) + PagerDuty(High/Critical).

### 6.2 Security Hub

- **표준 활성화**:
  - **CIS AWS Foundations Benchmark v3.0**
  - **AWS Foundational Security Best Practices v1.0.0**
  - **NIST SP 800-53 Rev.5**(선택)
  - PCI-DSS — 해당 없음(결제 직접 처리 미수행). 결제 대행사 도입 시 재평가.
- **위임 관리자**: Audit 계정, 조직 자동 활성화.
- **통합**: GuardDuty, Config, IAM Access Analyzer, Macie, Inspector, Firewall Manager, 파트너(Snyk/Prisma 등 선택).
- **Auto-suppression Rule**: False positive 관리. 모든 suppress는 Jira 티켓·만료일 필수.

### 6.3 AWS Config + 자동 교정

- **모든 계정·모든 리전** Config Recorder 활성(All resources + Global).
- **Config Aggregator**: Audit 계정에 조직 단위 집계.
- **주요 Managed Rules**:
  - `iam-user-no-policies-check`, `iam-password-policy`, `root-account-mfa-enabled`, `access-keys-rotated`
  - `s3-bucket-public-read-prohibited`, `s3-bucket-public-write-prohibited`, `s3-bucket-server-side-encryption-enabled`, `s3-bucket-ssl-requests-only`
  - `rds-storage-encrypted`, `rds-instance-public-access-check`, `rds-logging-enabled`
  - `ec2-imdsv2-check`, `ebs-encryption-by-default`
  - `vpc-flow-logs-enabled`, `vpc-default-security-group-closed`
  - `cloudtrail-enabled`, `cloudtrail-s3-dataevents-enabled`, `cloud-trail-log-file-validation-enabled`
  - `kms-cmk-not-scheduled-for-deletion`, `cmk-backing-key-rotation-enabled`
  - `guardduty-enabled-centralized`, `securityhub-enabled`
- **자동 교정(SSM Automation)**:
  - `AWS-DisablePublicAccessForSecurityGroup` — 0.0.0.0/0 개방 SG 자동 축소
  - `AWS-EnableS3BucketEncryption` — SSE 미설정 버킷 자동 활성
  - `AWSConfigRemediation-EncryptEBSVolumeAtRest` — 비암호화 EBS 교정
  - `AWS-EnableCloudTrail`, `AWSConfigRemediation-EnableVPCFlowLogs`
- **Conformance Pack**: `Operational-Best-Practices-for-CIS-AWS-Foundations-v3.0.0`, `Operational-Best-Practices-for-AWS-Well-Architected-Security-Pillar`.

### 6.4 CloudTrail 조직 트레일

- **조직 트레일** 1개: 관리 이벤트 + Insights + 주요 **데이터 이벤트**(S3 데이터레이크/업로드 버킷, Lambda Invoke, DynamoDB tfstate-lock).
- **저장**: Log Archive 계정 S3 `techai-org-cloudtrail-{accountId}` + SSE-KMS(`cmk-prod-logs`) + **S3 Object Lock Compliance 7년** + Versioning + MFA Delete(관리 계정).
- **무결성 검증**: Log file validation(서명 파일) 활성.
- **감시**: CloudTrail → CloudWatch Logs → Metric Filter(루트 로그인, IAM 변경, KMS 비활성, SCP 변경, GuardDuty 비활성) → Alarm → SNS.

### 6.5 추가 보안 서비스

- **IAM Access Analyzer**: 조직 레벨 활성, 외부 공유 Findings + **Unused Access(신규)** 기능으로 비활성 Role/Key 식별.
- **Amazon Inspector**: ECS/ECR 이미지 CVE 스캔, Lambda/EC2 취약점.
- **Amazon Detective**: **활성화**(Audit 계정 Delegated Admin). GuardDuty/VPC Flow/CloudTrail 그래프 기반 침해 조사. 비용 대비 효용: 침해 조사 능력이 부족한 조기 단계에 유용, 운영 정식화 후 유지 여부 재평가.
- **Firewall Manager**: 다계정 WAF/SG/Shield Advanced 정책 일괄 관리.

### 6.6 인시던트 대응 런북 (NIST SP 800-61 Rev.2)

#### 6.6.1 단계별 플로우

1. **Preparation (준비)**
   - 런북/연락망/도구 준비, `#sec-ir` Slack 전용 채널, PagerDuty On-call, 포렌식 계정 접근 절차.
   - 분기 테이블탑 훈련, 연 1회 Red Team 모의 침투.
2. **Detection & Analysis (탐지·분석)**
   - 트리거: GuardDuty Critical/High, Security Hub Critical, WAF anomaly, CloudTrail Metric Alarm, 사용자 제보.
   - Severity: **SEV1(서비스 중단/대규모 유출)** / **SEV2(국소 침해)** / **SEV3(정책 위반)**.
   - 초기 분류 15분 이내, Incident Commander(IC) 지명.
3. **Containment (격리)**
   - 단기: 의심 EC2/Task를 `quarantine` SG로 이동(모든 in/out 차단), IAM 세션 Revoke(`PutUserPolicy` deny-all + session revoke), 해당 역할 `aws:TokenIssueTime` 조건으로 무효화.
   - 장기: 영향 범위 확정 후 VPC 차단, CloudFront 차단 규칙, ALB 대상 그룹 교체.
4. **Eradication (제거)**
   - 침해 자격증명 모두 로테이션(CMK, Secrets, IAM Role 대체), 취약점 패치, 이미지 재빌드, 악성 S3 객체 Legal Hold 후 정리.
5. **Recovery (복구)**
   - 새 계정/리소스로 복원, 스냅샷에서 복구(사전 무결성 검증), 모니터링 강화 7일.
6. **Post-Incident Activity (사후)**
   - 72시간 내 포스트모템(RCA, 타임라인, IoC 목록, 재발 방지), 규제 기관 신고(개인정보 유출 시 **개인정보보호위원회/KISA 24시간 내 신고, GDPR 72시간**), 이용자 통지.

#### 6.6.2 자주 쓰는 플레이북

| 시나리오                              | 초기 조치                                                             |
| ------------------------------------- | --------------------------------------------------------------------- |
| IAM Access Key 유출                   | Key 즉시 비활성 → 관련 자격 로테이션 → 사용 이력 CloudTrail Athena 쿼리 |
| EC2 암호화폐 채굴 탐지(GuardDuty)     | Task/Instance Quarantine SG → EBS 스냅샷 → 포렌식 계정으로 공유 → 제거  |
| S3 공개 노출                          | Block Public Access 재설정 → 객체 목록 다운로드(Access Log) → 영향 산정 |
| 크리덴셜 스터핑(WAF ATP Spike)        | Rate-based 임계 하향, 영향 계정 비밀번호 강제 재설정, CAPTCHA 즉시 활성  |
| RCE 의심(SQLi/LFI 로그)               | WAF 룰 강화 + 해당 경로 Block → 이미지 재배포 → Inspector 재스캔        |

---

## 부록 A. 베스트 프랙티스 체크리스트

- [x] 루트 계정 MFA 활성, 액세스 키 0건
- [x] IAM User 대신 Identity Center Permission Set 사용
- [x] 모든 로그는 Log Archive 계정 S3 중앙화 + Object Lock
- [x] 모든 CMK 연 1회 이상 회전
- [x] SCP로 리전 제한(ap-northeast-2/us-east-1), 루트 권한 사용 차단
- [x] OWASP Top 10 대응: WAF Managed Rules + 애플리케이션 레벨 검증(ASVS L2)
- [x] `Resource: "*"` 없는 IAM 정책 + Access Analyzer 검증
- [x] GitHub Actions OIDC(장기 키 0건)
- [x] 하드코딩 시크릿 0건(git-secrets/TruffleHog/gitleaks)
- [x] GuardDuty + Security Hub + Config + CloudTrail 조직 전체 활성
- [x] 개인정보 마스킹/가명화, KISA/GDPR 대응 정책 문서화

## 부록 B. 공식 참고 자료

- AWS Security Reference Architecture: https://docs.aws.amazon.com/prescriptive-guidance/latest/security-reference-architecture/
- AWS IAM User Guide: https://docs.aws.amazon.com/IAM/latest/UserGuide/
- AWS IAM Best Practices: https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html
- AWS Organizations User Guide: https://docs.aws.amazon.com/organizations/latest/userguide/
- AWS IAM Identity Center (구 AWS SSO) User Guide: https://docs.aws.amazon.com/singlesignon/latest/userguide/
- AWS Secrets Manager User Guide: https://docs.aws.amazon.com/secretsmanager/latest/userguide/
- AWS KMS Developer Guide: https://docs.aws.amazon.com/kms/latest/developerguide/
- AWS WAF Developer Guide: https://docs.aws.amazon.com/waf/latest/developerguide/
- AWS Shield Developer Guide: https://docs.aws.amazon.com/waf/latest/developerguide/shield-chapter.html
- AWS GuardDuty User Guide: https://docs.aws.amazon.com/guardduty/latest/ug/
- AWS Security Hub User Guide: https://docs.aws.amazon.com/securityhub/latest/userguide/
- AWS Config Developer Guide: https://docs.aws.amazon.com/config/latest/developerguide/
- CIS AWS Foundations Benchmark: https://www.cisecurity.org/benchmark/amazon_web_services
- NIST SP 800-61 Rev.2 (Computer Security Incident Handling Guide): https://csrc.nist.gov/publications/detail/sp/800-61/rev-2/final
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/
