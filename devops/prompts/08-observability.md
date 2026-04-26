# 08. 관측성 (Observability) 설계

## 역할
당신은 SRE/DevOps 엔지니어이며, 로그/지표/트레이스의 **3 pillars**를 기반으로 시스템 관측성을 설계합니다. Google SRE의 **4 Golden Signals**(Latency, Traffic, Errors, Saturation)와 AWS Well-Architected 운영 우수성 원칙에 부합해야 합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/08-observability/` 에 저장하세요.

### 산출물 1: `strategy.md`
1. **관측 대상 인벤토리**: 백엔드 6개 API + 배치 + 프론트 2개 + Aurora + MongoDB Atlas + ElastiCache + MSK + ALB + CloudFront
2. **관측 스택 선정**
   - 옵션 A: **CloudWatch + X-Ray + Application Signals** (AWS 네이티브)
   - 옵션 B: **Amazon Managed Grafana + Amazon Managed Prometheus + OpenTelemetry + OpenSearch**
   - 옵션 C: 3rd Party(Datadog/New Relic) — 공식 문서 범위 내에서 언급만
   - 트레이드오프 표(기능/운영/비용/벤더 락인)
3. **SLI/SLO 프레임워크**
   - 서비스별 SLI 정의(예: api-chatbot p95 latency, error rate)
   - SLO 목표값(99.9% 가용성, p95 응답 시간 등)과 에러 버짓 정책

### 산출물 2: `logging.md`
1. **로그 구조화**: JSON 포맷(timestamp, level, trace_id, span_id, service, env, message)
2. Spring Boot: `logback-spring.xml` — `logstash-logback-encoder` 또는 OpenTelemetry Logs
3. Next.js: `pino` 권장 (공식 레퍼런스 기반)
4. **수집**: FireLens(Fluent Bit) → CloudWatch Logs 또는 OpenSearch Ingestion
5. **중앙화**: Log Archive 계정 S3 (Object Lock) + Athena 쿼리
6. 로그 보존 정책(hot: 30일 CloudWatch, warm: 90일 S3 Standard-IA, cold: 7년 Glacier)
7. PII 마스킹(Lambda 처리 또는 OpenSearch Ingestion 파이프라인)

### 산출물 3: `metrics.md`
1. **지표 수집**
   - Spring Boot Actuator + Micrometer → CloudWatch or ADOT(OpenTelemetry Collector) → AMP
   - JVM 지표(힙, GC, 스레드), Tomcat/Reactor Netty 지표
   - 비즈니스 지표(챗봇 요청 수, 북마크 생성 수 등)
2. **ECS/EKS 컨테이너 지표**: Container Insights 또는 Prometheus
3. **인프라 지표**: Aurora Performance Insights, ElastiCache CloudWatch, MSK Open Monitoring
4. **RED/USE 방법론** 적용 다이어그램
5. 대시보드 템플릿(Grafana/CloudWatch Dashboard JSON) 구조

### 산출물 4: `tracing.md`
1. **분산 트레이싱**: OpenTelemetry SDK → ADOT Collector → AWS X-Ray (또는 Tempo)
2. Spring Boot: Micrometer Tracing + OpenTelemetry Bridge
3. Next.js: `@opentelemetry/sdk-trace-node`, 서버 컴포넌트 trace 전파
4. **컨텍스트 전파**: W3C Trace Context 헤더 사용 (`traceparent`)
5. 샘플링 전략(Head-based 초기 적용 → Tail-based 검토)
6. Kafka 메시지 trace 전파(헤더 인젝션)

### 산출물 5: `alerting.md`
1. **알람 채널**: Amazon SNS → PagerDuty/Slack Webhook
2. **알람 카탈로그** (Service × Signal 매트릭스)
   - 가용성(5xx 비율, Target Unhealthy), 지연(p95/p99), 포화(CPU/Mem/DB CPU/Connection), 트래픽 급감/급증
3. **런북 링크**: 각 알람에 대응 런북 URL 필수
4. **노이즈 제어**: Flapping 억제, 복합 조건, 비즈니스 시간 vs 야간 차등
5. **On-call 로테이션** 원칙

### 산출물 6: `dashboards/`
CloudWatch Dashboard JSON 또는 Grafana JSON:
- `overview.json` — 전체 상태 한 눈에
- `service-api.json` — API 서비스 템플릿 (서비스명 변수화)
- `data-layer.json` — Aurora/Mongo/Cache/MSK
- `business.json` — 핵심 KPI

## 베스트 프랙티스 체크리스트
- [ ] 모든 로그에 `trace_id` 포함 (로그-트레이스 연계)
- [ ] 지표 카디널리티 관리 (고카디널리티 태그 지양)
- [ ] 알람은 **증상 기반(Symptom-based)** 우선, 원인 기반 보조
- [ ] SLO 위반 시 자동 Deployment Freeze 절차
- [ ] 대시보드는 IaC로 관리 (Grafana Dashboard as Code / CDK)
- [ ] CloudWatch Contributor Insights로 Top-N 이상 탐지

## 참고 자료 (공식 출처만)
- Amazon CloudWatch User Guide: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/
- AWS X-Ray Developer Guide: https://docs.aws.amazon.com/xray/latest/devguide/
- Amazon Managed Service for Prometheus: https://docs.aws.amazon.com/prometheus/latest/userguide/
- Amazon Managed Grafana: https://docs.aws.amazon.com/grafana/latest/userguide/
- AWS Distro for OpenTelemetry: https://aws-otel.github.io/docs/
- OpenTelemetry Documentation: https://opentelemetry.io/docs/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/reference/actuator/
- Micrometer Documentation: https://docs.micrometer.io/micrometer/reference/
- Amazon OpenSearch Service: https://docs.aws.amazon.com/opensearch-service/
- Google SRE Book (The Four Golden Signals): https://sre.google/sre-book/monitoring-distributed-systems/
- Google SRE Workbook (SLO): https://sre.google/workbook/implementing-slos/
- W3C Trace Context Recommendation: https://www.w3.org/TR/trace-context/

## 제약
- 모든 트레이싱/메트릭 구성은 **OpenTelemetry 표준**을 기본으로 함 (AWS 전용 SDK 의존 최소화)
- 로그는 PII가 포함될 수 있음을 전제로 마스킹 설계를 반드시 포함
