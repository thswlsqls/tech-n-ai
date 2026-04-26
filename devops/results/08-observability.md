# 08. 관측성(Observability) 설계

> **문서 목적**: tech-n-ai 플랫폼(백엔드 API 6종 + batch-source + 프론트 2종 + Aurora + MongoDB Atlas + ElastiCache Valkey + MSK + ALB + CloudFront)에 대한 **3 Pillars(Logs/Metrics/Traces)** 기반 관측성 아키텍처를 정의한다.
> **원칙**: Google SRE의 **4 Golden Signals(Latency/Traffic/Errors/Saturation)** 와 AWS Well-Architected 운영 우수성 기둥을 준수하며, **OpenTelemetry 표준**을 1급 인터페이스로 채택해 벤더 락인을 최소화한다.

> ⚙️ **실 동작 코드 위치**: 본 문서는 spec(설계 명세)이며, ADOT Collector / FireLens 설정 실 파일은 [`devops/terraform/modules/observability/configs/`](../terraform/modules/observability/configs/README.md) 에 있다.
> | 파일 | 용도 |
> |---|---|
> | [`adot-collector.yaml`](../terraform/modules/observability/configs/adot-collector.yaml) | OTLP gRPC/HTTP → ECS 메타 검출 → X-Ray + CloudWatch EMF 송신 |
> | [`firelens-fluentbit.conf`](../terraform/modules/observability/configs/firelens-fluentbit.conf) | Spring Boot JSON 로그 → 파싱 → PII 마스킹 → CloudWatch Logs |
> | [`parsers.conf`](../terraform/modules/observability/configs/parsers.conf) | Spring·Nginx 파서 |
> | [`masking.lua`](../terraform/modules/observability/configs/masking.lua) | 이메일·전화·신용카드·RRN·JWT·AWS 키 마스킹 |
>
> ECS Task Definition 의 sidecar 통합 패턴은 [configs/README.md](../terraform/modules/observability/configs/README.md) 참조.
> CPU/Memory/RunningTaskCount 알람 + Overview Dashboard 는 [`modules/observability/`](../terraform/modules/observability/README.md) 에 IaC 화 완료.

---

## 전제 / 권고 (Preconditions & Recommendations)

본 설계가 적용되기 전 반드시 선행되어야 할 사항을 명시한다.

| 구분 | 항목 | 현재 상태 | 권고/조치 |
|---|---|---|---|
| 필수 | **Spring Boot Actuator liveness/readiness probe** | **미설정** | `management.endpoint.health.probes.enabled=true` + Kubernetes/ECS용 `/actuator/health/liveness`, `/actuator/health/readiness` 노출 **즉시 활성화 필요** |
| 필수 | Actuator 엔드포인트 | `health, metrics, prometheus` (common/core), `+gateway, info` (gateway) | 점진적으로 `httptrace`, `threaddump`, `heapdump`, `loggers` 를 내부 VPC 전용 포트로 분리 노출 |
| 필수 | 로그에 `trace_id` 주입 | 미적용 | Micrometer Tracing + Logback MDC 바인딩으로 **모든 로그라인에 `trace_id`/`span_id` 포함** 필수 |
| 필수 | 지표 카디널리티 관리 | 미관리 | `userId`, `sessionId`, `requestId` 등 고카디널리티 태그를 메트릭 라벨로 사용 금지 (로그/트레이스로 이관) |
| 권고 | 알람 설계 원칙 | - | **Symptom-based(증상 기반) 우선**, Cause-based는 보조 채널로 분리 |
| 권고 | SLO 위반 자동화 | - | 에러 버짓 50% 소진 시 GitHub Actions 배포 워크플로에 `Deploy Freeze` 게이트 자동 적용 (`workflow_dispatch` 조건 차단) |
| 권고 | 대시보드 IaC 관리 | - | Grafana Dashboards as Code(JSON) + CDK/Terraform 배포, 수동 편집 금지 |

### Actuator Probe 활성화 예시 (필수)

```yaml
# application-common.yml
management:
  endpoint:
    health:
      probes:
        enabled: true           # /actuator/health/liveness, /actuator/health/readiness 활성화
      show-details: when_authorized
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState, db, mongo, redis
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus, info, loggers
      base-path: /actuator
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

ECS Fargate Task Definition의 `healthCheck.command`에는 **liveness** 를, ALB Target Group의 `HealthCheckPath`에는 **readiness** 를 지정한다(참고: [Spring Boot Actuator Health Probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.kubernetes-probes)).

---

## 1. 전략 (Strategy)

### 1.1 관측 대상 인벤토리

| 레이어 | 대상 | 유형 | 기본 지표원 | 기본 로그원 | 트레이스 대상 |
|---|---|---|---|---|---|
| Edge | CloudFront | AWS Managed | CloudWatch Metrics(`Requests`, `BytesDownloaded`, `5xxErrorRate`) | Standard Logs → S3 | - |
| Edge | ALB (gateway 전면) | AWS Managed | CloudWatch Metrics(`TargetResponseTime`, `HTTPCode_ELB_5XX_Count`) | Access Log → S3 | X-Ray 연동 (ALB → X-Ray header) |
| App | api-gateway (8081) | Spring Boot 4 | Micrometer → Prometheus | Logback JSON | OTel/X-Ray |
| App | api-emerging-tech (8082) | Spring Boot 4 | 동일 | 동일 | 동일 |
| App | api-auth (8083) | Spring Boot 4 | 동일 | 동일 | 동일 |
| App | api-chatbot (8084) | Spring Boot 4 + langchain4j | 동일 + LLM 지표(토큰, 응답시간) | 동일 | 동일 |
| App | api-bookmark (8085) | Spring Boot 4 | 동일 | 동일 | 동일 |
| App | api-agent (8086) | Spring Boot 4 | 동일 | 동일 | 동일 |
| Batch | batch-source | Spring Batch 6.0.2 | Micrometer Batch 지표(Step/Job) | 동일 | OTel (Job 단위 루트 스팬) |
| Front | app (3000) | Next.js 16 | Web Vitals + CloudWatch RUM (선택) | pino JSON → CloudWatch | `@opentelemetry/sdk-trace-node` |
| Front | admin (3001) | Next.js 16 | 동일 | 동일 | 동일 |
| Data | Aurora MySQL | AWS Managed | CloudWatch + Performance Insights | Slow Query Log → CloudWatch | - |
| Data | MongoDB Atlas | SaaS | Atlas Metrics → CloudWatch Integration | Atlas Log → S3 Push | - |
| Data | ElastiCache (Valkey) | AWS Managed | CloudWatch Metrics(`CacheHits`, `Evictions`, `CPUUtilization`) | Engine/Slow Log → CloudWatch Logs | - |
| Msg | MSK Kafka | AWS Managed | Open Monitoring(Prometheus JMX/Node Exporter) | Broker Log → CloudWatch | Kafka Header Propagation |

### 1.2 관측 스택 옵션 비교

#### 옵션 A — CloudWatch + X-Ray + Application Signals (AWS 네이티브)

- **구성**: CloudWatch Logs/Metrics/Alarms + AWS X-Ray + CloudWatch Application Signals + Container Insights
- **계측**: CloudWatch Agent + ADOT Collector (Application Signals는 OTel 기반)
- **대시보드**: CloudWatch Dashboard
- **공식 문서**: [CloudWatch UG](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/), [X-Ray DG](https://docs.aws.amazon.com/xray/latest/devguide/)

#### 옵션 B — Amazon Managed Grafana + AMP + OpenTelemetry + OpenSearch

- **구성**: AMP(Prometheus 호환) + AMG(Grafana) + ADOT Collector + OpenSearch Service(로그) + X-Ray(트레이스, 옵션)
- **계측**: ADOT Collector (OTLP) + Micrometer Prometheus Registry
- **대시보드**: Managed Grafana (Grafana Dashboards as Code)
- **공식 문서**: [AMP UG](https://docs.aws.amazon.com/prometheus/latest/userguide/), [AMG UG](https://docs.aws.amazon.com/grafana/latest/userguide/), [ADOT](https://aws-otel.github.io/docs/)

#### 옵션 C — 3rd Party (Datadog / New Relic) [참고]

- 상용 SaaS. 공식 문서 범위 제한으로 **본 문서에서는 채택 대상 아님**. 향후 규모 확장 시 POC 대상으로만 기록.

#### 트레이드오프 표

| 항목 | 옵션 A (CloudWatch 네이티브) | 옵션 B (AMP + AMG + OTel) | 옵션 C (3rd Party) |
|---|---|---|---|
| 기능(로그/지표/트레이스 통합) | 상 (Application Signals로 SLO 연계) | 상 (Grafana의 LogQL/PromQL/TraceQL) | 최상 |
| 운영 부담 | **최소** (Managed 일체) | 중 (Workspace/Scraper 관리) | 최소 |
| 학습 곡선 | 낮음 | 중 (Prometheus/Grafana 친숙 필요) | 낮음 |
| 표준성/이식성 | 낮음 (AWS 종속) | **높음** (Prometheus/OTel/Grafana 오픈 표준) | 중 |
| 초기 비용 | 낮음~중 | 중 | **높음** |
| 대규모 비용 | **급증 위험**(Logs Ingest 단가) | 예측 가능(메트릭 샘플 단위) | 매우 높음 |
| 벤더 락인 | 높음 | **낮음** | 높음 |
| 팀 역량 적합도 | AWS Console 친숙 팀 | 오픈소스 친숙 팀 | - |

#### 본 프로젝트 선정안 — **하이브리드: 옵션 A(기본) + 옵션 B(트레이싱/대시보드 일부)**

**결정 근거**
1. 팀 규모(소규모, AWS 경험 다수) 및 초기 운영 부담 최소화 → **기본 스택은 CloudWatch Logs/Metrics/Alarms + X-Ray**
2. OpenTelemetry 표준 계측(**ADOT Collector**)을 파이프라인 앞단에 두어 **옵션 B로 언제든 전환 가능한 구조** 확보
3. 트레이스는 **X-Ray**(Application Signals 연계 이득)로 통일, 메트릭은 **CloudWatch**(Actuator Prometheus 스크랩을 ADOT이 EMF 변환)로 통일
4. 추후 트래픽/팀 확대 시 AMP/AMG로 이관(ADOT 설정만 교체) — 애플리케이션 코드 무변경

### 1.3 SLI / SLO 프레임워크

#### 정의 원칙
- **사용자 체감(Symptom)** 기반 SLI 우선. 시스템 내부 지표(Cause)는 디버깅 용도.
- 측정 구간: **28일 롤링 윈도우** (SRE Workbook 표준)
- 에러 버짓 = `1 - SLO`. 예: SLO 99.9% → 에러 버짓 = **28일 중 40분 20초**.

#### 서비스별 SLI / SLO

| 서비스 | SLI (Good events / Valid events) | SLO | 측정 소스 |
|---|---|---|---|
| api-gateway | (상태 `< 500` 응답 수) / (전체 응답 수) | **99.95%** 가용성 | ALB `HTTPCode_Target_*` |
| api-gateway | p95 TargetResponseTime < 300ms | **99% 충족** | ALB `TargetResponseTime` |
| api-auth | 로그인 성공률(의도적 4xx 제외) | **99.9%** | Micrometer `http.server.requests` (tag `uri=/login`) |
| api-chatbot | p95 end-to-end latency < **3.0s** | **95% 충족** (LLM 외부 의존) | Micrometer + LLM SDK timer |
| api-chatbot | 5xx 비율 < 1% | **99%** | Micrometer |
| api-bookmark | p95 latency < 200ms, 5xx < 0.5% | **99.9% 가용성** | Micrometer |
| api-emerging-tech | p95 latency < 500ms (검색 포함) | **99.9% 가용성** | Micrometer + MongoDB Atlas |
| api-agent | p95 agentic task 완료시간 < 10s | **95% 충족** | Micrometer + 커스텀 타이머 |
| batch-source | Daily Job 성공률 | **99.5%** | Spring Batch `spring.batch.job` 메트릭 |
| Next.js app | p75 LCP < 2.5s, INP < 200ms | **90% 충족** | Web Vitals / CloudWatch RUM |
| Next.js admin | SSR p95 TTFB < 800ms | **99%** | CloudFront + Next.js 서버 로그 |
| Aurora | CPUUtilization < 75%, Replica Lag < 1s | **99%** | CloudWatch |
| MSK | Under-replicated partitions = 0 | **99.99%** | MSK Open Monitoring |

#### 에러 버짓 정책

| 버짓 소진률 (28d rolling) | 조치 |
|---|---|
| < 25% | 정상 운영. 기능 개발 속도 유지. |
| 25% ~ 50% | 신규 기능 배포 심사 강화 (리뷰 2인, Canary 5% 필수) |
| 50% ~ 75% | **배포 알림 강화** + Postmortem 회고 우선 순위화 |
| > 75% | **Deploy Freeze 자동 발동** — GitHub Actions `deploy.yml`의 guard job이 CloudWatch SLO API를 조회하여 차단. 장애 개선 및 신뢰성 작업만 배포 가능. |
| 100% 초과 (SLO 위반) | Incident 선언, 경영진 보고, SLO 재산정 또는 아키텍처 개선 액션 아이템 도출 |

참고: [Google SRE Book — Monitoring](https://sre.google/sre-book/monitoring-distributed-systems/), [Google SRE Workbook — Implementing SLOs](https://sre.google/workbook/implementing-slos/).

---

## 2. 로깅 (Logging)

### 2.1 구조화 로그 스키마 (JSON)

모든 서비스는 다음 공통 필드를 포함한 **단일 라인 JSON**으로 stdout에 기록한다.

```json
{
  "timestamp": "2026-04-20T01:23:45.678Z",
  "level": "INFO",
  "service": "api-chatbot",
  "env": "prod",
  "version": "1.24.0",
  "host": "ip-10-11-12-13",
  "thread": "http-nio-8084-exec-3",
  "logger": "com.tech.n.ai.api.chatbot.service.ChatService",
  "trace_id": "4bf92f3577b34da6a3ce929d0e0e4736",
  "span_id": "00f067aa0ba902b7",
  "user_id_hash": "sha256:b94d27b9...",
  "request_id": "01HW7K3YQ8...",
  "http_method": "POST",
  "http_path": "/v1/chat",
  "http_status": 200,
  "duration_ms": 842,
  "message": "chat completion succeeded"
}
```

- **`trace_id` / `span_id`** 는 OpenTelemetry Context에서 Logback MDC로 자동 주입(Micrometer Tracing 표준).
- **PII 필드는 원천에서 제외** (이메일/전화/평문 userId 금지). 식별 필요 시 **SHA-256 해시**(`user_id_hash`).
- `service`, `env`, `version` 는 ECS Task 환경변수에서 주입(`SERVICE_NAME`, `SPRING_PROFILES_ACTIVE`, `APP_VERSION`).

### 2.2 Spring Boot — `logback-spring.xml` + `logstash-logback-encoder`

```xml
<configuration>
  <springProperty scope="context" name="SERVICE" source="spring.application.name"/>
  <springProperty scope="context" name="ENV" source="spring.profiles.active"/>
  <springProperty scope="context" name="VERSION" source="app.version" defaultValue="dev"/>

  <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeMdcKeyName>trace_id</includeMdcKeyName>
      <includeMdcKeyName>span_id</includeMdcKeyName>
      <includeMdcKeyName>request_id</includeMdcKeyName>
      <customFields>{"service":"${SERVICE}","env":"${ENV}","version":"${VERSION}"}</customFields>
      <fieldNames>
        <timestamp>timestamp</timestamp>
        <message>message</message>
        <logger>logger</logger>
        <thread>thread</thread>
        <level>level</level>
      </fieldNames>
    </encoder>
  </appender>

  <!-- Micrometer Tracing이 MDC에 traceId/spanId를 주입하도록 설정되어 있어야 함 -->
  <!-- management.tracing.enabled=true, logging.pattern.correlation 사용 또는 MDC 직접 주입 -->

  <root level="INFO">
    <appender-ref ref="JSON_CONSOLE"/>
  </root>

  <logger name="com.tech.n.ai" level="DEBUG"/>
  <logger name="org.springframework.web" level="INFO"/>
</configuration>
```

**Micrometer Tracing → MDC 브리지**: `application.yml`에서 `management.tracing.enabled=true` 및 `logging.pattern.correlation=[%X{traceId:-},%X{spanId:-}] ` 설정(또는 Logstash encoder가 MDC를 그대로 JSON 필드화). 공식 참고: [Spring Boot Actuator — Tracing](https://docs.spring.io/spring-boot/reference/actuator/tracing.html).

### 2.3 Next.js — `pino`

```ts
// lib/logger.ts
import pino from "pino";
import { trace, context } from "@opentelemetry/api";

export const logger = pino({
  level: process.env.LOG_LEVEL ?? "info",
  base: {
    service: process.env.SERVICE_NAME,
    env: process.env.APP_ENV,
    version: process.env.APP_VERSION,
  },
  timestamp: pino.stdTimeFunctions.isoTime,
  mixin() {
    const span = trace.getSpan(context.active());
    if (!span) return {};
    const { traceId, spanId } = span.spanContext();
    return { trace_id: traceId, span_id: spanId };
  },
  redact: {
    paths: ["req.headers.authorization", "req.headers.cookie", "password", "token", "*.email"],
    censor: "[REDACTED]",
  },
});
```

Next.js 서버 런타임에서는 `pino` stdout 출력 → FireLens가 수집. 브라우저 측 로그는 발생량을 제한하고 CloudWatch RUM 또는 전용 엔드포인트를 통해서만 수집.

### 2.4 수집 파이프라인

```
[ECS Task stdout]
      │
      ▼
[FireLens Sidecar (Fluent Bit aws-for-fluent-bit 이미지)]
      │
      ▼
[CloudWatch Logs (Hot 30일, trace_id 인덱싱)]
      │  (Subscription Filter, 장애 시 리플레이 안전)
      ▼
[Kinesis Data Firehose]
      ├──► S3 (Log Archive 계정, Object Lock Compliance Mode)
      └──► OpenSearch Service (선택, 장애조사용 최근 14일 인덱스)
                                                ▼
                            [Athena External Table (S3 JSON, Partition: service/env/date)]
```

- FireLens의 `parser`는 JSON, `log_format`은 `json_lines`.
- CloudWatch Logs Subscription Filter로 Firehose 전달(장애 시 리플레이 안전).
- **PII 마스킹**: Firehose `Processor = Lambda` 단계에서 정규식 기반 마스킹(이메일/JWT/카드번호 패턴). 실패 시 격리 S3 버킷.

### 2.5 보존 정책 (Lifecycle)

| 단계 | 저장소 | 보존 | 용도 |
|---|---|---|---|
| Hot | CloudWatch Logs | **30일** | 실시간 장애 조사, Live Tail |
| Warm | S3 Standard-IA | **90일** | Athena 분기별 감사/포렌식 |
| Cold | S3 Glacier Flexible Retrieval | **7년** | 법적/감사 보존 (금융/개인정보보호법) |
| Archive Lock | S3 Object Lock (Compliance) | 7년 | **삭제/변조 방지** (Log Archive 계정, 별도 OU) |

### 2.6 PII 마스킹 파이프라인

```
Firehose → Lambda(Node.js 20)
  - email: [\w.+-]+@[\w-]+\.[\w.-]+  → "[EMAIL]"
  - JWT:   eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+  → "[JWT]"
  - 카드:  \b(?:\d[ -]*?){13,16}\b → "[CARD]"
  - 주민번호: \d{6}-\d{7} → "[RRN]"
  → Lambda 결과 = 마스킹된 JSON
```

Lambda 실패 이벤트는 `errorOutputPrefix`로 격리 버킷에 적재, CloudWatch Alarm(`Masking Failure > 0 for 5m`)으로 즉시 알람.

---

## 3. 메트릭 (Metrics)

### 3.1 수집 파이프라인

```
[Spring Boot Actuator + Micrometer]
        │  /actuator/prometheus (scrape endpoint)
        ▼
[ADOT Collector (OpenTelemetry) — ECS Sidecar 또는 Daemon Task]
        │        ├── prometheusreceiver (Spring Boot scrape)
        │        ├── hostmetricsreceiver (container)
        │        ├── awsecscontainermetricsreceiver (Task)
        │        └── otlpreceiver (Next.js OTLP push)
        │
        ├──► awsemfexporter → CloudWatch Metrics (기본)
        ├──► prometheusremotewriteexporter → AMP (옵션, 전환 예정)
        └──► awsxrayexporter → X-Ray (트레이스는 §4 참조)
```

참고: [ADOT Collector](https://aws-otel.github.io/docs/getting-started/collector), [Micrometer Prometheus](https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html).

### 3.2 핵심 지표 카탈로그

| 카테고리 | 지표 | 소스 | 라벨(카디널리티 주의) |
|---|---|---|---|
| JVM | `jvm.memory.used`, `jvm.gc.pause`, `jvm.threads.live`, `jvm.classes.loaded` | Micrometer JVM Binder | `area`, `pool`, `cause` |
| HTTP | `http.server.requests` (count/timer) | Micrometer Web | `method`, `uri`(Route Template), `status`, `outcome` |
| HTTP Client | `http.client.requests` | Micrometer RestClient | `method`, `uri`, `status` |
| Tomcat | `tomcat.threads.busy`, `tomcat.sessions.active` | Micrometer | - |
| DB Pool | `hikaricp.connections.active`, `hikaricp.connections.pending` | HikariCP Meter | `pool` |
| JPA | `hibernate.statements`, `hibernate.sessions.open` | Micrometer Hibernate | `entityManagerFactory` |
| MongoDB | `mongodb.driver.pool.size`, `mongodb.driver.commands` | Micrometer MongoDB | `cluster.id` |
| Kafka | `kafka.consumer.records.lag`, `kafka.producer.record.send.rate` | Micrometer Kafka | `topic`, `partition` |
| Batch | `spring.batch.job`, `spring.batch.step` | Micrometer Batch | `name`, `status` |
| 비즈니스 | `chatbot.requests.total`, `chatbot.tokens.total`, `bookmark.created.total`, `agent.tasks.completed` | 커스텀 Counter/Timer | `env` (저카디널리티만) |
| 인프라 | Aurora `CPUUtilization`, `DatabaseConnections`, `ReadLatency`, `WriteLatency` | CloudWatch | `DBInstanceIdentifier` |
| 인프라 | ElastiCache `CPUUtilization`, `CacheHits`, `Evictions`, `CurrConnections` | CloudWatch | `CacheClusterId` |
| 인프라 | MSK `BytesInPerSec`, `UnderReplicatedPartitions`, `MaxOffsetLag` | Open Monitoring | `Broker`, `Topic` |
| 컨테이너 | `CPUUtilization`, `MemoryUtilization`, `NetworkRxBytes` | Container Insights | `ClusterName`, `ServiceName`, `TaskDefinition` |

**카디널리티 가드**: `uri` 라벨은 **Route Template**(`/v1/users/{id}`)만 사용, 실제 ID 금지. `userId`/`sessionId`는 라벨로 절대 사용 금지 — 트레이스/로그로만 관찰.

### 3.3 RED / USE 방법론 매핑

| 방법론 | 구분 | 지표 | 본 프로젝트 매핑 |
|---|---|---|---|
| **RED** (서비스/요청) | **R**ate | 초당 요청 수 | `http.server.requests` rate |
| | **E**rrors | 오류 비율 | `http.server.requests{status=~"5.."}` rate |
| | **D**uration | 지연 분포 | `http.server.requests` p50/p95/p99 |
| **USE** (리소스) | **U**tilization | 사용률 | CPU/Mem/DiskIO/Network Util |
| | **S**aturation | 대기 큐 | HikariCP pending, Tomcat queue, K8s run_queue |
| | **E**rrors | 오류 이벤트 | GC full count, TCP retransmit, Disk IO errors |

### 3.4 대시보드 템플릿 JSON 구조 (Grafana 호환)

```json
{
  "title": "service-api (templated)",
  "templating": {
    "list": [
      { "name": "service", "type": "query", "query": "label_values(http_server_requests_seconds_count, application)" },
      { "name": "env", "type": "custom", "options": ["dev","beta","prod"] }
    ]
  },
  "panels": [
    { "title": "Request Rate (RED-R)",  "type": "timeseries",
      "targets": [{ "expr": "sum by (uri)(rate(http_server_requests_seconds_count{application=\"$service\",env=\"$env\"}[1m]))" }] },
    { "title": "Error Rate (RED-E)", "type": "timeseries",
      "targets": [{ "expr": "sum(rate(http_server_requests_seconds_count{application=\"$service\",status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count{application=\"$service\"}[5m]))" }] },
    { "title": "Latency p95 (RED-D)", "type": "timeseries",
      "targets": [{ "expr": "histogram_quantile(0.95, sum by (le,uri)(rate(http_server_requests_seconds_bucket{application=\"$service\"}[5m])))" }] },
    { "title": "JVM Heap", "type": "timeseries",
      "targets": [{ "expr": "sum by (area)(jvm_memory_used_bytes{application=\"$service\"})" }] },
    { "title": "HikariCP Pending", "type": "stat",
      "targets": [{ "expr": "max(hikaricp_connections_pending{application=\"$service\"})" }] }
  ]
}
```

CloudWatch Dashboard 변환 시 `metricQueries` + `DASHBOARD_BODY` JSON 구조를 사용(CDK `cloudwatch.Dashboard` L2 Construct 권장).

---

## 4. 트레이싱 (Tracing)

### 4.1 아키텍처

```
[Spring Boot App]
  Micrometer Tracing (OTel Bridge)
        │ OTLP gRPC
[Next.js App]
  @opentelemetry/sdk-trace-node
        │ OTLP HTTP
        ▼
[ADOT Collector]
        ├──► AWS X-Ray (기본)
        └──► Tempo / AMP-Traces (선택, 추후)
```

### 4.2 Spring Boot 설정

```yaml
# common/core/src/main/resources/application-common-core.yml 발췌 (실 동작 코드)
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}  # prod 는 0.1 권장 (env 로 주입)
  opentelemetry:
    tracing:
      export:
        otlp:
          endpoint: ${OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}
                                  # ECS sidecar 패턴: ADOT Collector 가 동일 task 내 localhost:4318 에서 수신
  prometheus:
    metrics:
      export:
        enabled: true
```

Gradle (Spring Boot 4 — `spring-boot-starter-opentelemetry` 가 OTel SDK + OTLP exporter + Micrometer bridge 를 일괄 제공):
```groovy
// 본 프로젝트는 Spring Boot 4 의 OpenTelemetry 통합 starter 를 사용한다.
// 루트 build.gradle 의 subprojects.dependencies 에서 이미 선언되어 있음(build.gradle:89).
implementation 'org.springframework.boot:spring-boot-starter-opentelemetry'
// 별도 외부 OTel SDK / micrometer-tracing-bridge-otel 추가는 불필요.
```

### 4.3 Next.js 설정

```ts
// instrumentation.ts (Next.js App Router)
import { NodeSDK } from "@opentelemetry/sdk-node";
import { OTLPTraceExporter } from "@opentelemetry/exporter-trace-otlp-http";
import { getNodeAutoInstrumentations } from "@opentelemetry/auto-instrumentations-node";
import { Resource } from "@opentelemetry/resources";
import { SemanticResourceAttributes } from "@opentelemetry/semantic-conventions";

export async function register() {
  if (process.env.NEXT_RUNTIME !== "nodejs") return;
  const sdk = new NodeSDK({
    resource: new Resource({
      [SemanticResourceAttributes.SERVICE_NAME]: process.env.SERVICE_NAME!,
      [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: process.env.APP_ENV!,
    }),
    traceExporter: new OTLPTraceExporter({
      url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT + "/v1/traces",
    }),
    instrumentations: [getNodeAutoInstrumentations()],
  });
  sdk.start();
}
```

### 4.4 컨텍스트 전파 — W3C Trace Context

- 모든 HTTP 호출은 `traceparent: 00-<trace-id>-<parent-id>-01` 및 `tracestate` 헤더를 전파 (참고: [W3C TraceContext](https://www.w3.org/TR/trace-context/)).
- ALB/CloudFront는 **X-Ray 트레이스 헤더(`X-Amzn-Trace-Id`)** 를 자동 삽입 — Spring Boot는 `AwsXrayPropagator` 를 OTel Propagator 체인에 추가하여 **W3C ↔ X-Ray 상호 변환**.

```java
// OtelConfig.java
OpenTelemetrySdk.builder()
  .setPropagators(ContextPropagators.create(TextMapPropagator.composite(
      W3CTraceContextPropagator.getInstance(),
      W3CBaggagePropagator.getInstance(),
      AwsXrayPropagator.getInstance()   // ALB/CloudFront 연계
  )))
  .build();
```

### 4.5 Kafka 트레이스 전파

- Producer 측: `KafkaTemplate`을 OTel `KafkaTelemetry`로 래핑 → 메시지 헤더에 `traceparent` 자동 주입.
- Consumer 측: `@KafkaListener` 수신 시 동일 Telemetry가 헤더에서 context 복원 → Consumer Span이 Producer Span의 자식으로 연결.

```java
// KafkaTelemetry 예시
KafkaTelemetry telemetry = KafkaTelemetry.create(openTelemetry);
producerFactory.addPostProcessor(telemetry.producerInterceptor());
consumerFactory.addPostProcessor(telemetry.consumerInterceptor());
```

토픽 `tech-n-ai.conversation.session.*`, `tech-n-ai.conversation.message.*` 에 대해 **CQRS Write → Kafka → MongoDB Projection** 전체 경로가 단일 Trace로 추적됨.

### 4.6 샘플링 전략

| 단계 | 전략 | 설정 |
|---|---|---|
| 1단계 (초기 배포) | **Head-based Probability** | prod 10%, beta 50%, dev/local 100% |
| 2단계 (트래픽 증가) | **Head-based + Rate Limiting** | 최대 100 spans/s per service |
| 3단계 (SLO 자동화 연계) | **Tail-based** (ADOT Collector `tailsamplingprocessor`) | error/slow(p95 초과) 100% 샘플 + 정상 5% |

Tail-based는 ADOT Collector에서 **Gateway Deployment** 패턴(단일 집계 Collector)이 필요하므로 트래픽 증가 단계에서 도입.

---

## 5. 알람 (Alerting)

### 5.1 채널 / 경로

```
CloudWatch Alarm (또는 AMP Alertmanager)
        │
        ▼
Amazon SNS Topic (env별 분리: alerts-prod, alerts-beta)
        ├──► AWS Chatbot → Slack (#alerts-prod, #alerts-beta)
        ├──► PagerDuty (HTTPS Integration) — P1/P2만
        └──► Email (archive용)
```

### 5.2 알람 카탈로그 (Service × Signal 매트릭스)

| Service | 가용성(Availability) | 지연(Latency) | 포화(Saturation) | 트래픽(Traffic) |
|---|---|---|---|---|
| ALB (gateway) | 5xx rate > 1% (5m) → **P2** [RB-ALB-5XX](#) | TargetResponseTime p95 > 1s (5m) → **P3** [RB-ALB-LAT](#) | ActiveConnections > 80% target → **P3** [RB-ALB-SAT](#) | RequestCount 50% drop vs 1w ago → **P3** [RB-ALB-DROP](#) |
| api-gateway | 5xx > 1% → P2 [RB-GW-5XX](#) | p95 > 500ms → P3 | CPU > 80% (10m) → P3 | RPS 3x spike → P3 |
| api-chatbot | 5xx > 2% → P2 [RB-CHAT-5XX](#) | p95 > 5s (10m) → **P2** [RB-CHAT-LAT](#) | LLM timeout rate > 5% → P2 | Token usage budget 80% → P3 |
| api-auth | Login fail rate > 10% (5m) → P1 [RB-AUTH-FAIL](#) | p95 > 500ms → P3 | JWT validation errors > 1% → P2 | Anomalous login spike → **P1** (보안) |
| api-bookmark | 5xx > 1% → P3 | p95 > 400ms → P4 | DB pool pending > 10 → P3 | - |
| api-emerging-tech | 5xx > 1% → P3 | p95 > 1s → P3 | MongoDB Slow Query > 100/m → P3 | - |
| api-agent | 5xx > 2% → P2 | p95 > 15s → P2 | Concurrent tasks > 80% limit → P3 | - |
| batch-source | Job Failure → **P2** [RB-BATCH-FAIL](#) | Job Duration > 2x baseline → P3 | - | Scheduled run missing → P2 |
| Next.js app | CloudFront 5xx > 0.5% → P2 | LCP p75 > 4s → P3 | - | - |
| Aurora | Replica Lag > 10s → **P1** [RB-AUR-LAG](#) | ReadLatency p95 > 100ms → P3 | CPU > 85% (10m) → **P2** [RB-AUR-CPU](#), Connections > 80% max → P2 | Deadlocks/min > 5 → P3 |
| MongoDB Atlas | Primary unavailable → P1 | Op latency p95 > 200ms → P3 | Connections > 80% → P3 | - |
| ElastiCache (Valkey) | Engine CPU > 85% → P2 | - | Evictions > 0 (5m) → P3 [RB-CACHE-EVICT](#) | - |
| MSK | UnderReplicatedPartitions > 0 → **P1** [RB-MSK-URP](#) | - | Consumer Lag > 10k (10m) → **P2** [RB-MSK-LAG](#), Disk > 75% → P2 | BytesInPerSec 3x spike → P3 |
| SLO Budget | 28d error budget burn > 2% / hour → **P1** [RB-SLO-BURN](#) | - | - | - |

> **런북 URL 규약**: `https://runbooks.tech-n-ai.internal/<code>` (예: `/RB-AUR-LAG`). 모든 알람은 **런북 URL이 description에 포함되어야 생성됨**(CDK 배포 시 검증). 런북 미존재 알람은 CI 단계에서 블록.

### 5.3 증상 기반 우선 원칙

- **Symptom Alert** (사용자 영향): 5xx rate, latency p95, Availability SLO Burn Rate → **PagerDuty 호출**
- **Cause Alert** (내부 원인): CPU, Memory, DB CPU, Pool Saturation → **Slack 채널 알림만**, 페이지 호출 없음

### 5.4 노이즈 제어

| 기법 | 적용 |
|---|---|
| **Flapping 억제** | CloudWatch Alarm `DatapointsToAlarm=3/5` (5개 중 3개 Breaching 시만 발화) |
| **복합 조건** | `Composite Alarm` — 5xx rate AND RequestCount > 100/m (저트래픽 False Positive 차단) |
| **야간 차등** | EventBridge Rule로 22:00–08:00 KST는 P3 이하 Slack만, P1/P2만 PagerDuty |
| **비즈니스 시간** | 평일 09–18 KST에 트래픽 급감 알람 민감도 상향 (5m→3m) |
| **Suppression** | PagerDuty Event Rule로 배포 중(+15m) Latency 알람 자동 억제 |
| **Alert Grouping** | Service 단위 그룹핑으로 폭풍 방지 |

### 5.5 SLO Burn Rate 알람 (Multi-Window)

Google SRE Workbook (*Alerting on SLOs* — multi-window multi-burn-rate) 기반:

| 감지 목적 | 단기 윈도우 | 장기 윈도우 | Burn Rate | 우선순위 |
|---|---|---|---|---|
| Fast burn (Page) | 5m | 1h | **14.4x** (에러 버짓 2% 소진) | P1 |
| Slow burn (Page) | 30m | 6h | **6x** (에러 버짓 5% 소진) | P2 |
| Chronic (Ticket) | 6h | 3d | **1x** (에러 버짓 10% 소진) | P3 |

### 5.6 SLO 위반 → Deploy Freeze 자동화

```
CloudWatch SLO (Application Signals)
      │ AttainmentGoal 위반 혹은 BurnRate > 6x
      ▼
EventBridge Rule → Lambda("set-deploy-freeze")
      │
      ├──► SSM Parameter Store /deploy/freeze = "true"
      ├──► GitHub Actions repository_dispatch("freeze")
      └──► Slack #releases 공지
```

GitHub Actions `deploy.yml`의 첫 job은 `Check Deploy Freeze` — SSM Parameter 또는 repo variable 확인 후 `true`면 `exit 1`. 신뢰성 수정은 `allow-freeze-override` 라벨이 있는 PR만 허용.

### 5.7 On-Call 로테이션 원칙

- **Primary/Secondary 2명 구성, 주간 로테이션(월요일 10:00 KST 시작)**
- Hand-off 체크리스트: 활성 인시던트, 진행 중 배포, 유지보수 창구
- **야간 페이지 수 < 2건/주** 을 지표로 관측(페이지 피로 방지)
- Postmortem: 모든 P1, 고객 영향 P2는 **48시간 내 Blameless Postmortem** 작성
- Compensation: 야간 페이지 및 주말 온콜은 별도 보상 (팀 정책)

---

## 6. 대시보드 (Dashboards)

모든 대시보드는 **IaC(Terraform `aws_cloudwatch_dashboard` 또는 Grafana Dashboards as Code)** 로 관리. 수동 편집 금지. 4개 기본 대시보드를 제공한다 (현재는 `overview` 1종만 IaC로 구현 — [`modules/observability/main.tf`](../terraform/modules/observability/main.tf), 나머지 3종은 추후 세션에서 추가).

### 6.1 `overview.json` — 전사 상태 한 눈에

```json
{
  "title": "tech-n-ai — Overview",
  "refresh": "30s",
  "panels": [
    {"title": "SLO Attainment (28d)", "type": "stat",
     "targets": [{"expr": "slo_attainment_ratio"}], "thresholds": [{"color":"red","value":0.999}]},
    {"title": "Global 5xx Rate",       "type": "timeseries",
     "targets": [{"expr": "sum(rate(http_server_requests_seconds_count{status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))"}]},
    {"title": "Services Up",           "type": "stat",
     "targets": [{"expr": "sum(up{job=~\"api-.*\"})"}]},
    {"title": "Active Alarms",         "type": "table", "datasource": "CloudWatch"},
    {"title": "Deploy Activity",       "type": "annotations"},
    {"title": "CloudFront Requests",   "type": "timeseries"},
    {"title": "Business KPI — Chats/Bookmarks/Agents", "type": "timeseries"}
  ]
}
```

### 6.2 `service-api.json` — API 서비스 템플릿 (서비스 변수화)

```json
{
  "title": "API Service — $service",
  "templating": {"list": [
    {"name": "service", "type": "query", "query": "label_values(http_server_requests_seconds_count, application)"},
    {"name": "env", "type": "custom", "options": ["dev","beta","prod"], "current": "prod"}
  ]},
  "rows": [
    {"title": "RED", "panels": [
      {"title": "Rate",    "expr": "sum by (uri)(rate(http_server_requests_seconds_count{application=\"$service\"}[1m]))"},
      {"title": "Errors", "expr": "sum(rate(http_server_requests_seconds_count{application=\"$service\",status=~\"5..\"}[5m])) / sum(rate(http_server_requests_seconds_count{application=\"$service\"}[5m]))"},
      {"title": "p95",    "expr": "histogram_quantile(0.95, sum by (le,uri)(rate(http_server_requests_seconds_bucket{application=\"$service\"}[5m])))"}
    ]},
    {"title": "JVM/USE", "panels": [
      {"title": "Heap", "expr": "sum by (area)(jvm_memory_used_bytes{application=\"$service\"})"},
      {"title": "GC Pause", "expr": "rate(jvm_gc_pause_seconds_sum{application=\"$service\"}[5m])"},
      {"title": "Threads", "expr": "jvm_threads_live_threads{application=\"$service\"}"},
      {"title": "HikariCP Pending", "expr": "hikaricp_connections_pending{application=\"$service\"}"}
    ]},
    {"title": "Traces", "panels": [
      {"title": "Top Slow Endpoints (X-Ray)", "datasource": "X-Ray"},
      {"title": "Error Traces", "datasource": "X-Ray"}
    ]}
  ]
}
```

### 6.3 `data-layer.json` — 데이터 계층

```json
{
  "title": "Data Layer — Aurora / MongoDB / ElastiCache / MSK",
  "rows": [
    {"title": "Aurora", "panels": [
      {"title": "CPU", "datasource": "CloudWatch", "metric": "AWS/RDS:CPUUtilization"},
      {"title": "Connections", "metric": "AWS/RDS:DatabaseConnections"},
      {"title": "Read/Write Latency", "metrics": ["ReadLatency","WriteLatency"]},
      {"title": "Replica Lag", "metric": "AuroraReplicaLag"}
    ]},
    {"title": "MongoDB Atlas", "panels": [
      {"title": "Op Latency", "datasource": "Atlas"},
      {"title": "Connections", "datasource": "Atlas"}
    ]},
    {"title": "ElastiCache (Valkey)", "panels": [
      {"title": "CPU", "metric": "AWS/ElastiCache:EngineCPUUtilization"},
      {"title": "Hit Ratio", "expr": "CacheHits/(CacheHits+CacheMisses)"},
      {"title": "Evictions", "metric": "Evictions"}
    ]},
    {"title": "MSK", "panels": [
      {"title": "BytesIn/Out", "datasource": "AMP"},
      {"title": "Under Replicated Partitions", "datasource": "AMP"},
      {"title": "Consumer Lag by Group", "datasource": "AMP"}
    ]}
  ]
}
```

### 6.4 `business.json` — 핵심 KPI

```json
{
  "title": "Business KPI",
  "refresh": "1m",
  "panels": [
    {"title": "Chat Requests (24h)",  "expr": "sum(increase(chatbot_requests_total[24h]))"},
    {"title": "LLM Tokens Consumed",  "expr": "sum(increase(chatbot_tokens_total[24h]))"},
    {"title": "Bookmarks Created",    "expr": "sum(increase(bookmark_created_total[24h]))"},
    {"title": "Agent Tasks Completed","expr": "sum(increase(agent_tasks_completed_total[24h]))"},
    {"title": "DAU (auth success)",   "expr": "count(count by (user_id_hash)(increase(auth_login_success_total[24h])))"},
    {"title": "Funnel — Search → Click → Bookmark", "type": "bargauge"},
    {"title": "Error Budget Remaining (28d)", "type": "gauge"}
  ]
}
```

---

## 부록 A. 구현 체크리스트

> 진척 상태 ✅(완료) / 🟡(부분) / ⬜(미착수) — 2026-04 기준.

- ⬜ 모든 서비스 Actuator **liveness/readiness probe 활성화** (`probes.enabled` 가 `application-common-core.yml` 에 미설정. 다만 envs/dev 의 `services.tf:13` 에서 `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true` 환경변수로 주입 중 — yml 에 정식 설정 추가 권장)
- 🟡 모든 로그 JSON + `trace_id`/`span_id` 포함 (Spring Boot 4 `spring-boot-starter-opentelemetry` 가 MDC 자동 주입. 다만 `logstash-logback-encoder` 의존성·`logback-spring.xml` 미커밋)
- ⬜ 메트릭 `uri` 라벨을 Route Template으로 통제 (고카디널리티 금지)
- ✅ ADOT Collector 사이드카 ECS Task Definition 템플릿 공용화 ([`devops/terraform/modules/observability/configs/adot-collector.yaml`](../terraform/modules/observability/configs/adot-collector.yaml))
- ⬜ W3C Trace Context 전파 검증 (브라우저 → ALB → API → Kafka → Consumer)
- ⬜ 모든 알람에 런북 URL 포함 (CI 검증 스크립트)
- ⬜ CloudWatch SLO (Application Signals) 및 Burn Rate 알람 구성
- ⬜ Deploy Freeze GitHub Actions guard job 추가 (`.github/workflows/` 자체가 미커밋)
- 🟡 대시보드 IaC 커밋 (`overview` 1종만 — `service-api`/`data-layer`/`business` 미생성, `modules/observability/main.tf:114` 참조)
- ⬜ PII 마스킹 Lambda + 실패 격리 알람 (`masking.lua` 는 Fluent Bit 단계에서 동작, Firehose Lambda 별도 미배치)
- ⬜ S3 Object Lock(Compliance) 7년 보존 로그 아카이브

---

## 부록 B. 공식 참고 자료

- Amazon CloudWatch User Guide — https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/
- AWS X-Ray Developer Guide — https://docs.aws.amazon.com/xray/latest/devguide/
- Amazon Managed Service for Prometheus — https://docs.aws.amazon.com/prometheus/latest/userguide/
- Amazon Managed Grafana — https://docs.aws.amazon.com/grafana/latest/userguide/
- AWS Distro for OpenTelemetry — https://aws-otel.github.io/docs/
- OpenTelemetry Documentation — https://opentelemetry.io/docs/
- Spring Boot Actuator — https://docs.spring.io/spring-boot/reference/actuator/
- Micrometer Documentation — https://docs.micrometer.io/micrometer/reference/
- Amazon OpenSearch Service — https://docs.aws.amazon.com/opensearch-service/
- Google SRE Book — Monitoring Distributed Systems — https://sre.google/sre-book/monitoring-distributed-systems/
- Google SRE Workbook — Implementing SLOs — https://sre.google/workbook/implementing-slos/
- W3C Trace Context Recommendation — https://www.w3.org/TR/trace-context/
