# 06. 보안 · IAM · 시크릿 · WAF 설계

## 역할
당신은 AWS 클라우드 보안을 책임지는 시큐리티 엔지니어입니다. **AWS Security Reference Architecture(SRA)**와 **CIS AWS Foundations Benchmark**에 부합하는 보안 설계서를 작성합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/06-security/` 에 저장하세요.

### 산출물 1: `identity-and-account.md`
1. **멀티 계정 전략**
   - AWS Organizations + Control Tower + SCP 설계
   - OU 구조(Security, Infrastructure, Workloads-dev/beta/prod, Sandbox)
   - Log Archive / Audit 계정 분리
2. **인증**
   - AWS IAM Identity Center(구 SSO) 적용 — 외부 IdP(Google/Okta) 연동 여부
   - MFA 강제, 액세스 키 사용 금지 원칙
3. **IAM 원칙**
   - 사람: Identity Center Permission Set
   - 워크로드: **IAM Role for Service Accounts(EKS IRSA) / ECS Task Role**
   - CI/CD: **GitHub Actions OIDC → IAM Role** (장기 키 금지)
4. **Break-glass 계정** 절차 및 보관 방안

### 산출물 2: `iam-policies.md`
서비스별 최소 권한 IAM 정책 초안(JSON 스켈레톤):
- 각 백엔드 모듈 Task Role
- 배치 모듈 Job Role
- CI/CD 배포 Role (ECR push, ECS update-service, Terraform 실행)
- Terraform 상태 관리 Role
- 모든 정책은 `Resource: "*"` 금지, 조건 키(`aws:SourceVpc`, `aws:PrincipalTag`) 활용

### 산출물 3: `secrets-management.md`
1. **Secrets Manager vs SSM Parameter Store** 분리 기준
2. 관리 대상 목록
   - DB 자격증명(Aurora, MongoDB Atlas)
   - OpenAI API Key, JWT 서명키, OAuth 클라이언트 시크릿
   - Kafka SASL 자격증명(해당 시)
3. **자동 로테이션** 대상 및 주기
4. Spring Boot 연동: `spring-cloud-aws-secrets-manager` 또는 환경 변수 주입 비교
5. KMS CMK 분리(Per-env/Per-service) 및 키 정책

### 산출물 4: `network-security.md`
1. **AWS WAF** 규칙셋
   - AWS Managed Rules(Common, Known Bad Inputs, SQLi, Linux OS, Anonymous IP)
   - Rate-based rule(IP당 req/min 임계치)
   - Bot Control 적용 여부
2. **AWS Shield Standard/Advanced** 적용 범위
3. **TLS**: ACM 인증서 발급, HSTS, TLS 1.2+ 강제
4. **CloudFront 보안 헤더** (Response Headers Policy: CSP, X-Frame-Options, Referrer-Policy)

### 산출물 5: `data-protection.md`
1. KMS 키 계층 (AWS Managed vs Customer Managed)
2. 저장 암호화 매트릭스 (S3/Aurora/ElastiCache/MSK/EBS)
3. 전송 암호화 매트릭스 (ALB/MSK/Atlas PrivateLink)
4. **Macie**로 S3 민감정보 스캔 대상 선정
5. 개인정보 처리(국내 개인정보보호법, GDPR 해당 시) — 마스킹/익명화 방침

### 산출물 6: `detection-and-response.md`
1. **GuardDuty** 전 리전 활성화, Malware Protection for EC2/S3
2. **Security Hub** 표준(CIS, AWS Foundational, PCI-DSS 해당 시) 활성화
3. **AWS Config** 규칙셋 및 자동 교정(SSM Automation)
4. **CloudTrail** 조직 트레일(S3 + KMS + Object Lock, Log Archive 계정)
5. **Detective** 사용 여부
6. 인시던트 대응 런북 초안 (NIST SP 800-61 준거)

## 베스트 프랙티스 체크리스트
- [ ] 루트 계정 MFA, 액세스 키 미생성
- [ ] IAM 사용자 대신 Identity Center 사용
- [ ] 모든 로그는 Log Archive 계정 S3로 중앙화 (Object Lock)
- [ ] 모든 KMS 키 회전(연 1회 이상)
- [ ] SCP로 리전 제한, 루트 권한 남용 차단
- [ ] OWASP Top 10 대응 — WAF + 애플리케이션 레벨 검증

## 참고 자료 (공식 출처만)
- AWS Security Reference Architecture: https://docs.aws.amazon.com/prescriptive-guidance/latest/security-reference-architecture/
- AWS IAM 사용자 가이드: https://docs.aws.amazon.com/IAM/latest/UserGuide/
- AWS IAM Best Practices: https://docs.aws.amazon.com/IAM/latest/UserGuide/best-practices.html
- AWS Organizations 사용자 가이드: https://docs.aws.amazon.com/organizations/latest/userguide/
- AWS IAM Identity Center: https://docs.aws.amazon.com/singlesignon/latest/userguide/
- AWS Secrets Manager: https://docs.aws.amazon.com/secretsmanager/latest/userguide/
- AWS KMS: https://docs.aws.amazon.com/kms/latest/developerguide/
- AWS WAF Developer Guide: https://docs.aws.amazon.com/waf/latest/developerguide/
- AWS Shield: https://docs.aws.amazon.com/waf/latest/developerguide/shield-chapter.html
- AWS GuardDuty: https://docs.aws.amazon.com/guardduty/latest/ug/
- AWS Security Hub: https://docs.aws.amazon.com/securityhub/latest/userguide/
- AWS Config: https://docs.aws.amazon.com/config/latest/developerguide/
- CIS AWS Foundations Benchmark: https://www.cisecurity.org/benchmark/amazon_web_services
- NIST SP 800-61 (Computer Security Incident Handling): https://csrc.nist.gov/publications/detail/sp/800-61/rev-2/final
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/

## 제약
- 모든 IAM 정책은 **IAM Access Analyzer**로 검증 가능한 형태로 작성
- 하드코딩된 시크릿 **0건** 확보 — git-secrets, truffleHog 등 도구 적용 계획 포함
