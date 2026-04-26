# 02. 네트워크 · VPC 설계

## 역할
당신은 AWS 네트워킹 전문 DevOps 엔지니어입니다. 앞서 확정된 아키텍처(`devops/docs/01-architecture/`)를 기반으로 **VPC 네트워크 상세 설계서**를 작성합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/02-network/` 에 저장하세요.

### 산출물 1: `vpc-design.md`
1. **CIDR 계획**
   - 각 환경(dev/beta/prod) VPC CIDR 블록 (RFC 1918 준수, 향후 Peering/TGW 대비 중복 방지)
   - 서브넷별 CIDR 할당표 (Public / Private-App / Private-Data / Private-TGW)
   - 최소 3개 AZ(`ap-northeast-2a/2b/2c`) 사용
2. **서브넷 설계**
   - Public: ALB, NAT Gateway
   - Private-App: ECS/EKS 워커, Lambda(필요 시)
   - Private-Data: Aurora, ElastiCache, MSK
   - 각 서브넷에 IP 예약량과 장기 확장 여지 명시
3. **라우팅 테이블 및 인터넷 경로**
   - IGW, NAT Gateway(AZ별 고가용성), Egress-only IGW(IPv6) 사용 여부
4. **VPC Endpoint (PrivateLink)** — 다음 서비스에 대한 Interface/Gateway Endpoint 설계: S3, DynamoDB(사용 시), ECR, CloudWatch Logs, Secrets Manager, STS, SSM, KMS
5. **DNS** — Route 53 Private Hosted Zone 구성, 내부 서비스 디스커버리 전략(AWS Cloud Map)

### 산출물 2: `security-groups.md`
서비스별 Security Group 매트릭스. **최소 권한** 원칙 적용.
| SG 이름 | 인바운드(소스/포트/프로토콜) | 아웃바운드 | 적용 리소스 |
|---|---|---|---|

- ALB, api-gateway, 각 백엔드 서비스, Aurora, ElastiCache, MSK, Bastion(SSM 선호, SSH 금지)

### 산출물 3: `network-acl-and-flow-logs.md`
- NACL 설계(스테이트리스 방어 레이어)
- VPC Flow Logs 활성화 및 S3/CloudWatch 전송 방안
- GuardDuty / Network Firewall 적용 여부 및 근거

### 산출물 4: `diagram.md`
전체 네트워크 토폴로지를 Mermaid 다이어그램으로. 반드시 다음 요소를 포함:
- 3 AZ, Public/Private 서브넷, IGW, NAT GW, VPC Endpoint, TGW(멀티 계정 시), Direct Connect/Site-to-Site VPN(사내 연동 필요 시 옵션)

## 베스트 프랙티스 체크리스트
- [ ] NAT Gateway는 AZ별 배치 (단일 NAT SPOF 회피)
- [ ] 퍼블릭 서브넷에는 애플리케이션 워크로드 금지
- [ ] RDS/MSK/Cache는 Private-Data 서브넷에만 배치
- [ ] SG는 IP 대신 **SG ID를 소스**로 참조 (동적 스케일 대응)
- [ ] 모든 VPC에 Flow Logs 활성화
- [ ] SSH 22 대신 **SSM Session Manager** 사용

## 참고 자료 (공식 출처만)
- VPC 사용자 가이드: https://docs.aws.amazon.com/vpc/latest/userguide/
- AWS VPC Connectivity Options: https://docs.aws.amazon.com/whitepapers/latest/aws-vpc-connectivity-options/
- VPC Endpoints: https://docs.aws.amazon.com/vpc/latest/privatelink/concepts.html
- AWS Security Reference Architecture: https://docs.aws.amazon.com/prescriptive-guidance/latest/security-reference-architecture/
- CIS AWS Foundations Benchmark (네트워크 섹션): https://www.cisecurity.org/benchmark/amazon_web_services

## 제약
- 모든 CIDR은 겹치지 않도록 계산 근거 제시
- IPv6 듀얼스택 적용 여부 결정 및 근거 명시
