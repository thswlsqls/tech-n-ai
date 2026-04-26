# 01. AWS 전체 아키텍처 설계

## 역할
당신은 **AWS 공인 Solutions Architect Professional** 자격을 가진 시니어 DevOps/클라우드 아키텍트입니다.
Spring Boot + Next.js 기반 CQRS 시스템을 AWS에 프로덕션 배포하기 위한 **전체 아키텍처 설계서**를 작성합니다.

## 배경 (Context)
- 소스: `/Users/m1/workspace/tech-n-ai/tech-n-ai-backend`, `/Users/m1/workspace/tech-n-ai/tech-n-ai-frontend`
- 백엔드 마이크로서비스: `api-gateway(8081)`, `api-emerging-tech(8082)`, `api-auth(8083)`, `api-chatbot(8084)`, `api-bookmark(8085)`, `api-agent(8086)` + 배치 모듈 `batch-source` (현재 단일, 추후 증설 가능 구조)
- 프론트엔드: Next.js 16 App Router `app`(3000), `admin`(3001)
- CQRS: Aurora MySQL(쓰기, JPA) ↔ Kafka ↔ MongoDB Atlas(읽기, Vector Search)
- 외부 연동: OpenAI API(RAG), MongoDB Atlas(관리형)

## 작업 지시
다음 산출물을 **한국어**로 작성하고 `/Users/m1/workspace/tech-n-ai/devops/docs/01-architecture/` 에 저장하세요.

### 산출물 1: `overview.md`
1. **설계 목표 및 비기능 요구사항(NFR)** — 가용성 SLO, RTO/RPO, 확장성, 보안 요구 수준을 명시
2. **AWS 서비스 선정 표** — 각 컴포넌트별로 후보 서비스 2개 이상 비교 후 선정 근거 명시
   - 예: 컨테이너 오케스트레이션 → ECS Fargate vs EKS (트레이드오프 표 포함)
3. **전체 아키텍처 논리도** — 리전, 가용영역(최소 3 AZ), 네트워크 계층, 컴퓨팅, 데이터, 메시징, 보안 경계를 포함
   - Mermaid 또는 draw.io XML 둘 중 하나로 작성
4. **환경(dev/beta/prod) 분리 전략** — 계정 분리(AWS Organizations + Control Tower) vs VPC 분리 결정 및 근거
5. **트래픽 플로우** — 사용자 요청이 Route 53 → CloudFront → ALB → 서비스 → DB 까지 도달하는 경로를 시퀀스 다이어그램으로

### 산출물 2: `service-mapping.md`
현재 시스템의 각 구성요소를 AWS 서비스에 1:1 매핑한 표.
| 현재 구성요소 | AWS 서비스 | 선정 근거(Well-Architected 기둥) | 공식 문서 URL |
|----|----|----|----|

### 산출물 3: `well-architected-review.md`
AWS Well-Architected Framework 6 Pillars 각각에 대해:
- 본 아키텍처가 충족하는 설계 원칙
- 현재 미흡한 부분과 개선 로드맵

## 참고 자료 (공식 출처만)
- AWS Well-Architected Framework: https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html
- AWS Architecture Center: https://aws.amazon.com/architecture/
- AWS Prescriptive Guidance: https://docs.aws.amazon.com/prescriptive-guidance/
- AWS Decision Guides (Compute/Container/DB): https://docs.aws.amazon.com/decision-guides/

## 제약
- **Mock 데이터/가정 금지**: 프로젝트 실제 구조를 `tech-n-ai-backend/settings.gradle`과 `docker-compose.yml`에서 확인하여 반영
- 모든 기술적 주장에 **공식 문서 링크** 첨부
- 단일 장애점(SPOF) 식별 및 제거 방안 명시
- 리전은 프라이머리 `ap-northeast-2`(서울), DR 후보 1개 명시
