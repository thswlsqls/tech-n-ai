# AWS 배포 프롬프트 시리즈 (Index)

이 폴더는 `tech-n-ai` 프로젝트를 AWS 클라우드로 배포하기 위한 **DevOps 산출물 생성 프롬프트** 모음입니다.
각 프롬프트는 독립적으로 실행 가능하며, 순서대로 수행하면 설계 → 구현 → 운영까지의 산출물이 완성됩니다.

## 대상 시스템 개요

- **백엔드**: Spring Boot 4.0.2 멀티모듈 (api-gateway, api-auth, api-chatbot, api-agent, api-bookmark, api-emerging-tech, batch-*)
- **프론트엔드**: Next.js 16 App Router × 2 앱 (`app` 공개용, `admin` 내부용)
- **데이터 계층 (CQRS)**: Aurora MySQL (writes, JPA) + MongoDB Atlas (reads) + Kafka 동기화
- **부가 스택**: Redis, langchain4j + MongoDB Atlas Vector Search + OpenAI (RAG 챗봇)

## 프롬프트 목록

| No. | 파일 | 산출물 |
|-----|------|--------|
| 01 | [01-architecture-design.md](01-architecture-design.md) | AWS 전체 아키텍처 설계서 + 다이어그램 |
| 02 | [02-network-vpc.md](02-network-vpc.md) | VPC / 서브넷 / 보안그룹 설계 문서 |
| 03 | [03-compute-and-frontend-hosting.md](03-compute-and-frontend-hosting.md) | 컴퓨팅(ECS/EKS) · 프론트 호스팅 설계 |
| 04 | [04-database-and-storage.md](04-database-and-storage.md) | DB · 캐시 · 오브젝트 스토리지 설계 |
| 05 | [05-messaging-kafka.md](05-messaging-kafka.md) | Amazon MSK 기반 메시징 설계 |
| 06 | [06-security-and-iam.md](06-security-and-iam.md) | 보안 · IAM · 시크릿 · WAF 설계 |
| 07 | [07-cicd-pipeline.md](07-cicd-pipeline.md) | CI/CD 파이프라인 설계 및 구현 가이드 |
| 08 | [08-observability.md](08-observability.md) | 관측성(로그/지표/트레이스) 설계 |
| 09 | [09-iac-terraform.md](09-iac-terraform.md) | IaC(Terraform) 모듈 구조 및 구현 가이드 |
| 10 | [10-deployment-runbook.md](10-deployment-runbook.md) | 배포 · 롤백 · 장애 대응 런북 |
| 11 | [11-dr-and-cost-optimization.md](11-dr-and-cost-optimization.md) | DR · 백업 · 비용 최적화 가이드 |

## 공통 제약 (모든 프롬프트에 적용)

1. **외부 자료 출처 제한**
   - AWS 공식 문서(`docs.aws.amazon.com`), AWS Well-Architected Framework, AWS Prescriptive Guidance, AWS Architecture Center
   - 각 기술의 **공식 문서**(Spring, Next.js, MongoDB, Kafka, Terraform, OpenTelemetry, CNCF 등)
   - 검증된 학술 논문 / RFC / NIST 표준
   - **금지**: 개인 블로그, Medium, Qiita, AI 생성 요약글, Stack Overflow 답변 본문
2. **업계 표준 베스트 프랙티스 준수**
   - AWS Well-Architected 6 Pillars (운영 우수성, 보안, 안정성, 성능 효율성, 비용 최적화, 지속 가능성)
   - 12-Factor App, OWASP ASVS, CIS Benchmarks
3. **언어**: 문서/주석은 한국어, 코드·식별자·UI 텍스트·커맨드는 영어
4. **환경 프로파일**: `dev`, `beta`, `prod` 3단계 분리 (백엔드 기존 프로파일과 정합)
5. **인용 형식**: 모든 기술적 주장은 `[출처: 문서명 - URL]` 형태로 각주 또는 본문 링크

## 사용 방법

```
# 각 프롬프트는 독립적으로 실행 가능
# 순서대로 수행 시 선행 산출물이 다음 단계의 입력이 됨
# 산출물은 /Users/m1/workspace/tech-n-ai/devops/docs/ 하위에 저장 (각 프롬프트에 경로 명시)
```
