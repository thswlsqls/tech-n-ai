# 03. 컴퓨팅 · 프론트엔드 호스팅 설계

## 역할
당신은 컨테이너 워크로드 및 프론트엔드 호스팅을 설계하는 AWS DevOps 엔지니어입니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/03-compute/` 에 저장하세요.

### 산출물 1: `backend-compute-design.md` — 백엔드 컴퓨팅 전략
1. **오케스트레이터 선정**
   - **ECS Fargate vs EKS vs App Runner** 비교 (비용/운영 복잡도/생태계/Spring Boot 적합도)
   - 최종 선택과 근거. 현재 규모(마이크로서비스 6개 + 배치)에 적합한 이유 명시
2. **컨테이너 이미지 전략**
   - **현재 프로젝트는 Dockerfile을 사용하지 않고 Spring Boot `bootBuildImage`(Paketo Buildpacks) 방식**으로 이미지 생성 (`build.gradle`의 `bootBuildImage` 태스크 기준). 이 전제를 유지하는 방안과 전통적 Dockerfile로 전환하는 방안 중 하나를 택일하고 근거 제시
   - 유지 시: Paketo Java Buildpack + Amazon Corretto 21 지정(`BP_JVM_VERSION=21`), 빌더 이미지 버전 고정
   - 전환 시: 멀티 스테이지 Dockerfile(Gradle layered JAR 활용) 예시 작성
   - ECR 리포지토리 네이밍/태깅 규칙 (`{env}/{module}:{git-sha}-{semver}`)
   - ECR 이미지 스캔(Basic + Enhanced with Inspector), 서명(Notation + AWS Signer Plugin 또는 Cosign) 여부
3. **서비스/Task 정의**
   - 모듈별 CPU/Memory 할당 초안 (p95 기준 사이징 근거)
   - 배치(batch-*) 모듈: EventBridge Scheduler + ECS RunTask 또는 AWS Batch 선택
   - JVM 튜닝: `-XX:MaxRAMPercentage`, `UseG1GC`, 컨테이너 인지 JVM 옵션
4. **오토스케일링**
   - ECS Service Auto Scaling 정책 (Target Tracking: CPU/RequestCount/CustomMetric)
   - HPA/KEDA 대안(EKS 선택 시)
5. **API Gateway 계층**
   - `api-gateway` 모듈(Spring Cloud Gateway) 앞에 ALB 사용
   - AWS API Gateway(HTTP/REST) 사용 여부 검토 → 중복 시 제거 근거

### 산출물 2: `frontend-hosting-design.md` — Next.js 호스팅
Next.js 16 App Router 앱(`app`, `admin`)의 호스팅 전략을 다음 3개 후보로 비교 후 선정:

| 후보 | 장점 | 단점 | 비용 |
|---|---|---|---|
| AWS Amplify Hosting | SSR/ISR 네이티브 지원 | 벤더 락인 가능 | ? |
| CloudFront + Lambda@Edge/OpenNext | 세밀한 제어 | 운영 복잡 | ? |
| ECS Fargate에 standalone Node.js 서버 | 백엔드와 동일 오케스트레이션 | 엣지 캐싱 약함 | ? |

- 선정 근거는 **Next.js 공식 AWS 배포 가이드**와 **AWS Amplify Hosting 문서**를 인용
- `admin` 앱은 내부 사용자 대상 → 공개 CDN 대신 Cognito + CloudFront 사설화 또는 VPN Only 검토
- **현재 프론트엔드 상태 주의사항** (반드시 설계서에 반영):
  - `app/next.config.ts`, `admin/next.config.ts`에 `output` 설정 **없음**(기본 SSR) → 호스팅 방식에 따라 `standalone`(자체 Node 서버) / 기본 SSR / Amplify 중 결정 필요
  - `.env` 파일 및 `NEXT_PUBLIC_*` 사용 **없음**, API 베이스 URL이 `rewrites`에 `http://localhost:8081`로 하드코딩 → AWS 배포 시 환경별 게이트웨이 URL 주입 전략(빌드타임 ENV + 런타임 rewrite destination) 설계 필수
  - 이미지 최적화 미적용 상태(`next/image` 미사용, `images.remotePatterns` 없음) → CloudFront 캐시/비용 설계에 반영
- 환경 변수 주입 전략(빌드타임 vs 런타임), `next.config.ts` rewrite 경로를 AWS에서 어떻게 유지할지

### 산출물 3: `cdn-and-edge.md`
- CloudFront 배포 설계 (오리진: ALB, S3, Amplify)
- 캐시 정책: Next.js 정적 자산/이미지 최적화 경로 분리
- WAF 연동(상세는 06-security에서)
- CloudFront Function / Lambda@Edge 사용 범위

## 베스트 프랙티스 체크리스트
- [ ] 컨테이너는 root 사용자 미사용, `readOnlyRootFilesystem` 적용
- [ ] Task IAM Role 분리 (모듈별 최소 권한)
- [ ] ECR Lifecycle Policy (미사용 이미지 자동 정리)
- [ ] Graceful Shutdown (Spring Boot `server.shutdown=graceful`, ALB Deregistration Delay 조정)
- [ ] Health Check: `/actuator/health/liveness`, `/actuator/health/readiness` 분리 — **현재 미설정 상태**. 배포 전 `management.endpoint.health.probes.enabled=true`, `management.health.livenessState.enabled=true`, `management.health.readinessState.enabled=true` 적용 및 ALB Target Group 헬스체크 경로 정합성 확보
- [ ] Blue/Green 또는 Canary 배포 (CodeDeploy 연동)

## 참고 자료 (공식 출처만)
- Amazon ECS 개발자 가이드: https://docs.aws.amazon.com/AmazonECS/latest/developerguide/
- EKS Best Practices Guides: https://docs.aws.amazon.com/eks/latest/best-practices/
- ECR 사용자 가이드: https://docs.aws.amazon.com/AmazonECR/latest/userguide/
- Spring Boot Reference (Container images): https://docs.spring.io/spring-boot/reference/packaging/container-images/
- Next.js Deployment: https://nextjs.org/docs/app/building-your-application/deploying
- AWS Amplify Hosting 사용자 가이드: https://docs.aws.amazon.com/amplify/latest/userguide/
- CloudFront 개발자 가이드: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/
- AWS Fargate Security Best Practices: https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/security-fargate.html

## 제약
- 사이징은 구체적 CPU/MEM 값과 근거(평균/피크 RPS 가정치) 제시
- 모든 공개 엔드포인트는 HTTPS(TLS 1.2+) 강제
