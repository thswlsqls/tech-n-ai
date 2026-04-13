# Jenkins 및 API 모듈 APM 모니터링 설계서 작성 프롬프트

## 역할 및 목적

당신은 **Observability 및 APM 전문 엔지니어**입니다. 로컬 macOS 테스트 환경에서 Jenkins CI/CD 작업과 6개 Spring Boot API 모듈의 리소스 사용량 및 핵심 지표를 모니터링하기 위한 **APM 상세 설계서**를 작성합니다.

**최우선 원칙:**
- 오픈소스만 사용하여 비용 최소화
- 업계 표준 베스트 프랙티스 준수
- 현재 프로젝트에 이미 존재하는 설정을 최대한 활용하고, 꼭 필요한 추가 작업만 수행

## 현재 환경 상태

### 이미 존재하는 것 (추가 설치 불필요)

다음은 이미 프로젝트에 구성되어 있습니다. **중복 설정하지 마세요.**

**1. 의존성 (root `build.gradle`)**
- `spring-boot-starter-actuator`
- `spring-boot-starter-opentelemetry`
- `micrometer-registry-prometheus` (runtimeOnly)

**2. Actuator 메트릭 설정 (`common/core/src/main/resources/application-common-core.yml`)**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        enabled: true
```
- 모든 API 모듈이 `common-core` 프로파일을 상속하므로 `/actuator/prometheus` 엔드포인트가 기본 활성화 상태
- OpenTelemetry OTLP export는 환경변수(`OTLP_METRICS_ENABLED`)로 제어 가능하나 현재 `false`

**3. 모니터링 인프라 (docker-compose.yml) — 이미 구축 완료**

batch APM 설계서(`docs/jenkins/batch/02-jenkins-batch-apm-design.md`) 기반으로 다음 서비스가 이미 docker-compose에 추가되어 운영 중입니다:

| 서비스 | 포트 | 용도 |
|---|---|---|
| Prometheus | 9091 | 메트릭 수집/저장 |
| Grafana | 3002 | 시각화 대시보드 |
| Jaeger (all-in-one) | 4317 (gRPC), 4318 (HTTP), 16686 (UI) | 분산 트레이싱 |
| Loki | 3100 | 로그 집계 |
| Promtail | — | 로그 수집 에이전트 |
| Alertmanager | 9093 | 알림 관리 |
| Pushgateway | 9092 | 단명 프로세스 메트릭 Push (batch-source 전용) |

**4. Jenkins APM 설정 — 이미 완료**
- Jenkins Prometheus Plugin 설치 완료, `/prometheus/` 엔드포인트 활성화
- Jenkins OpenTelemetry Plugin 설치 완료, Pipeline Stage 트레이싱 활성화
- Prometheus의 Jenkins scrape target 설정 완료

**5. 로그 수집 인프라 — 이미 완료**
- Promtail이 `$HOME/workspace/tech-n-ai/tech-n-ai-backend/logs`와 `$HOME/.jenkins` 경로를 수집 중
- Grafana에 Prometheus, Jaeger, Loki 데이터소스 자동 프로비저닝 완료

### 아직 없는 것 (이 설계서에서 다뤄야 할 범위)

| 영역 | 현재 상태 |
|---|---|
| API 모듈 Prometheus scrape 설정 | Prometheus에 6개 API 모듈의 scrape target 미등록 |
| API 모듈 OpenTelemetry 트레이스 export | `application-common-core.yml`에 OTLP export 설정 비활성화 상태 |
| API 모듈 로그 파일 출력 | 콘솔 stdout에만 출력 중, 파일 로그(Promtail 수집용) 미설정 |
| API 모듈 전용 Grafana 대시보드 | batch-source 대시보드만 존재, API 모듈용 없음 |
| API 모듈 전용 알림 규칙 | batch 전용 규칙만 존재, API 모듈용 없음 |
| 배포 Pipeline 모니터링 연동 | CI/CD 배포 후 헬스체크와 모니터링 지표 연동 없음 |

### API 모듈과 Batch 모듈의 모니터링 차이

API 모듈은 상주 프로세스이므로 batch-source와 모니터링 방식이 근본적으로 다릅니다:

| 구분 | batch-source | API 모듈 |
|---|---|---|
| **메트릭 수집 방식** | Pushgateway Push (프로세스 종료 전 push) | Prometheus Pull (`/actuator/prometheus` 직접 scrape) |
| **Pushgateway 필요 여부** | 필수 | **불필요** |
| **추가 의존성** | `prometheus-metrics-exporter-pushgateway` | **추가 의존성 없음** (기존 Micrometer + Prometheus 레지스트리 활용) |
| **메트릭 특성** | Job 실행 단위 (실행 시간, 처리 건수) | 서버 운영 지표 (요청률, 응답 시간, 에러율, 리소스 사용률) |
| **트레이스 범위** | Feign Client 호출만 | **전체 HTTP 요청 체인** (Gateway → 하위 서비스 → DB/Kafka/Redis) |
| **헬스체크** | exit code | HTTP `/actuator/health` |
| **대시보드 관점** | Job 실행 결과 중심 | **서비스 SLA 중심** (가용성, 응답시간, 에러율) |

### API 모듈 상세 정보

| Gradle 모듈명 | 포트 | 주요 의존성 | 트레이스 대상 |
|---|---|---|---|
| `api-gateway` | 8081 | WebFlux/Netty, Redis Reactive, Resilience4j | HTTP 라우팅, 서킷브레이커, Rate Limiter |
| `api-emerging-tech` | 8082 | MongoDB, langchain4j + OpenAI | MongoDB 쿼리, OpenAI API 호출 |
| `api-auth` | 8083 | Aurora (JPA), Feign (OAuth), Mail | JPA 쿼리, OAuth 외부 호출, 메일 발송 |
| `api-chatbot` | 8084 | Aurora (JPA), MongoDB, Kafka, langchain4j + OpenAI + Cohere | JPA/MongoDB 쿼리, Kafka, OpenAI/Cohere API 호출 |
| `api-bookmark` | 8085 | Aurora (JPA), MongoDB, Kafka | JPA/MongoDB 쿼리, Kafka 발행/소비 |
| `api-agent` | 8086 | Aurora, MongoDB, Kafka, Feign, Slack, Scraper, RSS, langchain4j | JPA/MongoDB, Kafka, 다수 외부 API 호출 |

### 프로젝트 기술 스택

- Java 21, Spring Boot 4.0.2, Spring Cloud 2025.1.0
- Gradle 9.2.1 (Groovy DSL)
- 환경 프로파일: `local`, `dev`, `beta`, `prod`

## 설계서 작성 범위

다음 **5개 섹션**으로 구성된 설계서를 작성합니다.

> 모니터링 인프라 구축(docker-compose), Jenkins 플러그인 설정, 데이터소스 프로비저닝은 batch APM 설계서에서 이미 완료되었으므로 이 설계서에서는 다루지 않습니다. **API 모듈에 추가로 필요한 설정만** 포함합니다.

---

### 섹션 1. Prometheus scrape 설정 추가

기존 Prometheus 설정에 6개 API 모듈의 scrape target을 추가합니다.

**포함 내용:**

1. **API 모듈 scrape job 추가**
   - `prometheus.yml`에 추가할 scrape_configs 블록
   - 6개 모듈 각각의 `targets` (localhost:{port})
   - `metrics_path`: `/actuator/prometheus`
   - scrape 주기, timeout 설정
   - `application` 라벨링으로 모듈 식별

2. **Gateway 특수성**
   - Gateway는 WebFlux(Netty) 기반이므로 Actuator 엔드포인트 경로가 동일한지 확인
   - WebFlux 환경에서의 Micrometer 메트릭 차이점이 있다면 명시

**작성 지침:**
- 기존 Prometheus 설정 파일의 **추가분만** 제시 (전체 파일을 다시 작성하지 않음)
- batch-source의 Pushgateway scrape 설정과 병존하는 구조

---

### 섹션 2. OpenTelemetry 트레이스 export 활성화

API 모듈의 분산 트레이싱을 활성화합니다.

**포함 내용:**

1. **OTLP exporter 설정**
   - `application-common-core.yml`에 추가할 OpenTelemetry 설정
   - Jaeger로의 OTLP export endpoint, 프로토콜(gRPC/HTTP), 샘플링 비율
   - 로컬 환경이므로 샘플링 비율 100% (전수 수집)

2. **수집 대상 트레이스**

   | 소스 | 자동 계측 범위 | 설명 |
   |---|---|---|
   | Gateway | HTTP 라우팅, 서킷브레이커 | 전체 요청의 진입점 Span |
   | API 모듈 | HTTP 수신, JPA/JDBC, MongoDB, Kafka, Feign, Redis | 모듈별 의존성에 따른 자동 Span 생성 |
   | 외부 API 호출 | OpenAI, Cohere, OAuth, Slack 등 | Feign/RestClient를 통한 외부 호출 |

3. **Gateway → 하위 서비스 트레이스 전파**
   - Spring Cloud Gateway의 WebFlux 환경에서 Trace Context가 하위 서비스로 정상 전파되는지 확인
   - 추가 설정이 필요한 경우에만 안내

**작성 지침:**
- `spring-boot-starter-opentelemetry` 의존성이 이미 존재하므로 추가 의존성은 최소화
- batch에서 설정한 Jaeger OTLP 수신 설정(4317/4318)을 그대로 활용

---

### 섹션 3. 로그 파일 출력 설정

API 모듈의 로그를 Promtail이 수집할 수 있도록 파일 출력을 추가합니다.

**포함 내용:**

1. **Logback 설정**
   - `logback-spring.xml` 작성 (또는 기존 파일 수정)
   - 기존 Console 출력은 유지하면서 RollingFile appender 추가
   - JSON 포맷으로 출력 (Loki 파싱용)
   - Trace ID가 자동 포함되도록 MDC 활용 (Traces ↔ Logs 상관관계)

2. **로그 파일 경로**
   - `logging.file.path` 설정: 기존 Promtail이 수집하는 경로(`$HOME/workspace/tech-n-ai/tech-n-ai-backend/logs`)에 맞춤
   - 모듈별 로그 파일 구분 (예: `logs/api-gateway.log`, `logs/api-auth.log`)

3. **Promtail 라벨 추가**
   - 기존 Promtail 설정에 API 모듈 로그 파일을 위한 라벨링 추가
   - `job`, `module` 라벨로 Loki에서 모듈별 필터링 가능하도록 설정

**작성 지침:**
- batch APM 설계서(`docs/jenkins/batch/02-jenkins-batch-apm-design.md`)의 로그 설정 패턴(Dual appender: Console + RollingFile with JsonEncoder)을 따름
- 기존 Promtail 설정 파일의 **추가분만** 제시

---

### 섹션 4. 알림 규칙 추가

API 모듈의 운영 안정성을 위한 알림 규칙을 추가합니다.

**포함 내용:**

1. **API 모듈 전용 알림 규칙**

   | 알림명 | 조건 | 심각도 | 설명 |
   |---|---|---|---|
   | ApiModuleDown | API 모듈 메트릭 수집 실패 지속 | critical | 특정 API 모듈 프로세스 다운 |
   | HighErrorRate | HTTP 5xx 응답 비율 > 임계치 | critical | 서비스 오류율 급증 |
   | HighResponseTime | HTTP 응답 시간 P95 > 임계치 | warning | 응답 지연 |
   | HighMemoryUsage | JVM Heap 사용률 > 임계치 | warning | 메모리 부족 |
   | GatewayCircuitBreakerOpen | Resilience4j 서킷브레이커 Open 상태 | critical | 하위 서비스 장애 전파 차단 중 |

   > 위 표는 최소 권장 규칙입니다. Prometheus에서 수집 가능한 지표를 기반으로 조정하세요.

2. **알림 규칙 파일**
   - 기존 `alert-rules.yml`에 API 모듈 그룹을 추가하는 형태
   - 각 규칙의 PromQL 표현식, `for` 대기 시간, labels, annotations 포함

3. **Alertmanager 라우팅 추가**
   - 기존 batch 알림 라우팅에 API 모듈용 라우팅 추가
   - 심각도별 알림 라우팅 (기존 패턴 준수: critical → 즉시, warning → 집계 후 발송)

**작성 지침:**
- 기존 batch 알림 규칙과 충돌하지 않도록 그룹명과 알림명을 구분
- 알림 피로(alert fatigue) 방지를 위해 적절한 `for` 대기 시간 설정
- 기존 설정 파일의 **추가분만** 제시

---

### 섹션 5. Grafana 대시보드 설계

API 모듈의 운영 지표를 시각화하는 대시보드를 설계합니다.

**5-1. API 모듈 통합 대시보드**

전체 API 모듈의 상태를 한눈에 파악하는 개요 대시보드:

| 패널 | 시각화 타입 | 표시 지표 |
|---|---|---|
| 서비스 상태 | Stat (6개) | 각 모듈의 Up/Down 상태 |
| 요청률 | Time Series | 모듈별 초당 요청 수 (RPS) |
| 에러율 | Time Series | 모듈별 HTTP 5xx 비율 |
| 응답 시간 P95 | Time Series | 모듈별 응답 시간 트렌드 |
| JVM 메모리 | Gauge (6개) | 각 모듈의 Heap 사용률 |

**5-2. 모듈별 상세 대시보드**

개별 모듈을 드릴다운하여 상세 지표를 확인하는 대시보드:

| 패널 | 시각화 타입 | 표시 지표 |
|---|---|---|
| HTTP 요청 분포 | Table | 엔드포인트별 요청 수, 에러 수, 평균 응답 시간 |
| 응답 시간 분포 | Heatmap | 응답 시간 히스토그램 |
| JVM 상세 | Time Series | Heap/Non-Heap, GC 횟수/시간, 스레드 수 |
| DB 커넥션 풀 | Gauge | HikariCP 활성/유휴/대기 커넥션 (Aurora 사용 모듈) |
| Kafka 지표 | Time Series | Consumer lag, 처리 TPS (Kafka 사용 모듈) |

**5-3. Gateway 전용 대시보드**

Gateway는 모든 트래픽의 진입점이므로 전용 대시보드가 필요합니다:

| 패널 | 시각화 타입 | 표시 지표 |
|---|---|---|
| 라우트별 트래픽 | Time Series | 라우트(서비스)별 요청 수 |
| 서킷브레이커 상태 | Stat | 라우트별 서킷브레이커 상태 (Closed/Open/Half-Open) |
| Rate Limiter | Time Series | Rate limit 허용/거부 비율 |
| Netty 이벤트 루프 | Gauge | 이벤트 루프 활성 스레드 수 |

**5-4. Observability 통합 뷰**

Grafana 데이터 소스 간 상관관계 기능 활용:
- 대시보드에서 특정 시점의 메트릭 이상 → 해당 시점의 트레이스 조회 (Metrics → Traces)
- 트레이스에서 에러 Span → 해당 Trace ID의 로그 조회 (Traces → Logs)
- 이 연결이 가능하도록 Grafana 데이터 소스 간 상관관계 설정을 포함할 것

> Grafana Community 대시보드 중 재사용 가능한 것이 있다면 ID와 함께 안내하세요.

**작성 지침:**
- 대시보드는 JSON 모델이 아닌 **패널 구성 명세** 수준으로 작성 (PromQL/LogQL 쿼리 포함)
- batch 대시보드와의 네비게이션 연결 고려 (같은 Grafana 인스턴스)
- Metrics ↔ Traces ↔ Logs 간 상관관계 탐색 흐름을 명시

---

## 공통 작성 원칙

1. **기존 설정 활용 우선**: batch APM 설계서에서 구축한 인프라와 설정을 그대로 활용합니다. 중복 추가하지 않습니다.
2. **증분(incremental) 설계**: 이미 존재하는 설정 파일에 대해서는 **추가할 부분만** 제시합니다. 전체 파일을 다시 작성하지 않습니다.
3. **최소 구성**: 로컬 테스트 환경에서 동작하는 최소한의 설정만 포함합니다. 프로덕션 레벨의 고가용성, 장기 보존 등은 범위 밖입니다.
4. **실행 가능성**: 모든 명령어, 설정 파일, UI 경로는 복사-붙여넣기로 즉시 사용 가능해야 합니다.
5. **공식 출처 준수**: 기술적 근거가 필요한 경우 다음 공식 문서만 참조합니다:
   - Prometheus: https://prometheus.io/docs/
   - Grafana: https://grafana.com/docs/grafana/latest/
   - Jaeger: https://www.jaegertracing.io/docs/
   - Grafana Loki: https://grafana.com/docs/loki/latest/
   - Alertmanager: https://prometheus.io/docs/alerting/latest/alertmanager/
   - OpenTelemetry: https://opentelemetry.io/docs/
   - Spring Boot Actuator: https://docs.spring.io/spring-boot/reference/actuator/
   - Micrometer: https://micrometer.io/docs/
   - Spring Cloud Gateway: https://docs.spring.io/spring-cloud-gateway/reference/
   - Resilience4j: https://resilience4j.readme.io/docs/
6. **오버엔지니어링 경계**: API 모듈의 Metrics, Traces, Logs 수집과 Alerting, Dashboard는 모두 범위 내입니다. 단, 각 컴포넌트를 로컬 테스트에 필요한 최소 수준으로 구성하고, 불필요한 중복이나 과잉 설정은 지양합니다.

## 출력 형식

- Markdown 형식으로 작성
- 코드 블록에는 언어 태그 명시 (```yaml, ```bash, ```groovy, ```promql 등)
- 각 섹션은 `##` 헤딩으로 구분
- 설정 파일은 프로젝트 루트(`tech-n-ai-backend/`) 기준 상대 경로를 파일명 상단에 명시
