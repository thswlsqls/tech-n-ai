# Jenkins 및 Spring Batch APM 모니터링 설계서 작성 프롬프트

## 역할 및 목적

당신은 **Observability 및 APM 전문 엔지니어**입니다. 로컬 macOS 테스트 환경에서 Jenkins CI/CD 작업과 Spring Batch Job의 리소스 사용량 및 핵심 지표를 모니터링하기 위한 **APM 상세 설계서**를 작성합니다.

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
- 모든 모듈이 `common-core` 프로파일을 상속하므로 `/actuator/prometheus` 엔드포인트가 기본 활성화 상태
- OpenTelemetry OTLP export는 환경변수(`OTLP_METRICS_ENABLED`)로 제어 가능하나 현재 `false`

**3. Jenkins 설계서 (`docs/jenkins/01-jenkins-cicd-scheduling-design.md`)**
- Jenkins CI/CD 및 Spring Batch 스케줄링 파이프라인이 이미 설계/운영 중
- Pipeline 타입을 선택한 이유 중 하나로 "향후 APM 구축 시 Jenkins OpenTelemetry Plugin으로 Stage 단위 분산 트레이싱 가능"을 명시

### 아직 없는 것 (설계서에서 다뤄야 할 범위)

| 영역 | 현재 상태 |
|---|---|
| Prometheus 서버 | docker-compose에 미포함 |
| Grafana | docker-compose에 미포함 |
| Jaeger (분산 트레이싱) | docker-compose에 미포함. 단, `spring-boot-starter-opentelemetry` 의존성은 이미 존재 |
| Loki (로그 집계) | docker-compose에 미포함. 로그는 콘솔 stdout에만 출력 중 |
| Alertmanager (알림) | docker-compose에 미포함. 장애 알림 체계 없음 |
| Jenkins 메트릭 수집 | 플러그인 미설치, 설정 없음 |
| Spring Batch 전용 메트릭 | Micrometer 자동 계측 외 추가 설정 없음 |
| 대시보드 | 없음 |

### 실행 중인 인프라

**docker-compose.yml 서비스:**
- Apache Kafka 4.1.1 (KRaft), Kafka UI (:9090)
- MySQL 8.0.41 (batch, auth, bookmark, chatbot 인스턴스)

**tmux-backend.sh로 실행 중인 서비스:**
- Redis
- API 모듈 6개: gateway(:8081), emerging-tech(:8082), auth(:8083), chatbot(:8084), bookmark(:8085), agent(:8086)

**Jenkins:** localhost:8080 (Homebrew `jenkins-lts`)

**batch-source 모듈:**
- Jenkins Pipeline에서 `java -jar`로 실행되는 Spring Batch Application
- 실행 완료 후 프로세스 종료됨 (장기 실행 서비스가 아님)

### Batch Job 목록

| Job Name | 설명 | 실행 주기 |
|---|---|---|
| `emerging-tech.scraper.job` | 웹 크롤링 기반 기술 아티클 수집 | 6시간마다 |
| `emerging-tech.rss.job` | RSS 피드 기반 기술 아티클 수집 | 4시간마다 |
| `emerging-tech.github.job` | GitHub Releases 기반 기술 정보 수집 | 매일 새벽 1회 |

### 프로젝트 기술 스택

- Java 21, Spring Boot 4.0.2, Spring Batch 6.0.2
- Gradle 9.2.1 (Groovy DSL)
- 환경 프로파일: `local`, `dev`, `beta`, `prod`

## 설계서 작성 범위

다음 **7개 섹션**으로 구성된 설계서를 작성합니다.

---

### 섹션 1. 모니터링 아키텍처 개요

전체 모니터링 스택의 구성과 데이터 흐름을 설계합니다.

**포함 내용:**

1. **Observability 3축 + Alerting 스택 선정 및 근거**
   - 업계 표준 Observability는 Metrics, Traces, Logs 3축으로 구성됨. 각 축별 선택한 오픈소스 도구와 선정 이유를 제시
   - Alerting은 관측만으로는 장애를 인지할 수 없으므로 필수 포함
   - 이미 프로젝트에 존재하는 Micrometer + Prometheus 레지스트리, OpenTelemetry ���존성을 기반으로 자연스럽게 확장할 것

   | 축 | 역할 | 후보 도구 (오픈소스) |
   |---|---|---|
   | Metrics | 수치 지표 수집/저장/시각화 | Prometheus, Grafana |
   | Traces | 서비스 간 요청 흐름 추적 | Jaeger, Tempo |
   | Logs | 로그 중앙 집계/검색 | Loki, ELK |
   | Alerting | 임계치 초과 시 자동 알림 | Alertmanager, Grafana Alerting |

   > 도구 선정 시 Grafana 에코시스템과의 통합성, 리소스 경량성, 프로젝트 기존 의존성과의 호환성을 기준으로 비교 후 결정할 것

2. **데이터 흐름 다이어그램**
   - Observability 3축 각각의 데이터 흐름을 포함:
     - Metrics: 소스 → Prometheus → Grafana
     - Traces: 소스 → OpenTelemetry → 트레이싱 백엔드 → Grafana
     - Logs: 소스 → 로그 수집기 → 로그 저장소 → Grafana
     - Alerting: Prometheus → Alertmanager → 알림 채널
   - 텍스트 기반 다이어그램 사용

3. **batch-source의 특수성 고려**
   - batch-source는 Job 실행 후 프로세스가 종료되는 단명(short-lived) 프로세스임
   - Prometheus의 기본 Pull 방식으로는 종료된 프로세스의 메트릭을 수집할 수 없음
   - 이 문제에 대한 업계 표준 해결 방안을 제시할 것 (예: Prometheus Pushgateway 등)

**작성 지침:**
- 도구 선정 시 "로컬 테스트 환경"과 "향후 클라우드 배포" 양쪽을 고려하되, 로컬 우선으로 설계
- 현재 docker-compose에 Kafka, MySQL이 이미 있으므로 모니터링 컨테이너 추가 시 리소스 부담을 의식할 것

---

### 섹션 2. 모니터링 인프라 구축

docker-compose에 모니터링 스택을 추가하고 설정 파일을 작성합니다.

**포함 내용:**

1. **docker-compose 서비스 추가**
   - 기존 `docker-compose.yml`에 모니터링 서비스를 추가하는 구성
   - 섹션 1에서 선정한 모든 컴포넌트 포함 (Metrics, Traces, Logs, Alerting)
   - 각 서비스의 포트 매핑, 볼륨, 네트워크 설정
   - 단명 프로세스(batch-source) 메트릭 수집을 위한 서비스 (예: Pushgateway)

2. **메트릭 수집 서버 설정**
   - scrape 대상 설정 (Jenkins, API 모듈, batch-source, Pushgateway)
   - scrape 주기, timeout 등 운영 파라미터
   - Alertmanager 연동 설정
   - 알림 규칙(alert rules) 파일 경로 설정

3. **트레이싱 백엔드 설정**
   - 트레이스 수신 프로토콜 및 포트 설정
   - 저장 설정 (로컬 환경이므로 인메모리 또는 경량 스토리지)

4. **로그 수집 설정**
   - 로그 저장소 설정 파일
   - Docker 컨테이너 로그 수집 설정 (로그 드라이버 또는 수집 에이전트)

5. **시각화 도구 설정**
   - 데이터 소스 자동 프로비저닝: Metrics, Traces, Logs 각각의 데이터 소스 등록
   - 대시보드 자동 프로비저닝 설정

**작성 지침:**
- 설정 파일의 전체 내용을 제공하고, 각 설정값에 대한 설명을 주석 또는 표로 제공
- 파일 경로는 프로젝트 루트 기준으로 명시

---

### 섹션 3. 메트릭 수집 설정

Jenkins와 Spring Batch 양쪽의 메트릭 수집을 설정���니다. Observability의 Metrics 축에 해당합니다.

**3-1. Jenkins 메트릭**

1. **플러그인 설치 및 설정**
   - Jenkins 메트릭 노출에 필요한 플러그인 식별 및 설치 절차
   - 플러그인 설정 (메트릭 엔드포인트 활성화)

2. **수집 대상 Jenkins 지표** — 다음 지표가 수집 가능한지 확인하고 설정:

   | 카테고리 | 지표 | 설명 |
   |---|---|---|
   | Job 성능 | 빌드 소요 시간 | Pipeline 및 Stage별 실행 시간 |
   | Job 성능 | 빌드 성공/실패율 | 최근 N회 빌드의 성공률 |
   | Job 성능 | 빌드 큐 대기 시간 | 빌드 요청 후 실행까지 대기 시간 |
   | 리소스 | JVM 메모리 사용량 | Jenkins 마스터의 Heap/Non-Heap |
   | 리소스 | CPU 사용률 | Jenkins 프로세스의 CPU 점유율 |
   | 시스템 | 활성 Executor 수 | 현재 실행 중인 빌드 수 |

**3-2. Spring Batch 메트릭**

1. **Micrometer 자동 계측 확인**
   - Spring Batch가 Micrometer를 통해 자동 노출하는 메트릭 목록 확인
   - 추가 설정이 필요한 경우에만 설정 추가

2. **수집 대상 Spring Batch 지표:**

   | 카테고리 | 지표 | 설명 |
   |---|---|---|
   | Job 실행 | Job 실행 시간 | Job 시작~종료 소요 시간 |
   | Job 실행 | Job 성공/실패 상태 | 최종 ExitStatus |
   | Step 실행 | Step별 실행 시간 | 각 Step의 소요 시간 |
   | Step 실행 | Step별 Read/Write/Skip 카운트 | ItemReader/Writer/Processor 처리량 |
   | 리소스 | JVM 메모리 사용량 | Batch 프로세스의 Heap/Non-Heap |
   | 리소스 | GC 횟수 및 시간 | Garbage Collection 부하 |

3. **단명 프로세스 메트릭 Push 설정**
   - batch-source 애플리케이션에서 메트릭을 Push하기 위한 설정
   - 필요한 의존성 추가 여부 확인 (이미 있는 것은 추가하지 않음)
   - `application.yml` 또는 `application-local.yml`에 추가할 설정

**작성 지침:**
- Spring Batch의 Micrometer 자동 계측 범위를 정확히 파악한 후, 부족한 부분만 커스텀 메트릭으로 보완
- Spring Boot Actuator의 `/actuator/prometheus` 엔드포인트가 이미 활성화되어 있음을 전제로 작성

---

### 섹션 4. 분산 트레이싱 설정

Observability의 Traces 축에 해당합니다. 서비스 간 요청 흐름을 추적합니다.

**포함 내용:**

1. **OpenTelemetry 트레이스 export 활성화**
   - 프로젝트에 이미 `spring-boot-starter-opentelemetry` 의존성이 존재함
   - `application-common-core.yml` 또는 `application-local.yml`에 추가할 OTLP exporter 설정
   - 트레이싱 백엔드로의 export endpoint, 프로토콜(gRPC/HTTP), 샘플링 비율 설정

2. **Jenkins Pipeline 트레이스 설정**
   - Jenkins OpenTelemetry Plugin 설치 및 설정 절차
   - Pipeline Stage별 Span 생성 → 트레이싱 백엔드로 전송
   - 설계서 `01-jenkins-cicd-scheduling-design.md`에서 이미 언급한 "Stage 단위 분산 트레이싱"을 구현

3. **수집 대상 트레이스:**

   | 소스 | 트레이스 범위 | 설명 |
   |---|---|---|
   | batch-source | Batch Job → 내부 API 호출 | Job 실행 중 API 모듈 호출 흐름 |
   | API 모듈 | 수신 요청 → DB/Kafka | Gateway부터 하위 서비스까지 |
   | Jenkins | Pipeline Stage | CI/CD 및 스케줄링 Stage별 실행 흐름 |

**작성 지침:**
- 로컬 테스트 환경이므로 샘플링 비율은 100% (전수 수집)로 설정
- 이미 존재하는 OpenTelemetry 의존성 활용에 집중하고, 추가 의존성은 최소화

---

### 섹션 5. 로그 집계 설정

Observability의 Logs 축에 해당합니다. 분산된 로그를 중앙에서 검색/조회합니다.

**포함 내용:**

1. **로그 수집 대상 및 방법**

   | 소스 | 현재 로그 출력 | 수집 방법 |
   |---|---|---|
   | API 모듈 (6개) | tmux pane에 stdout | 수집 에이전트 또는 Docker 로그 드라이버 |
   | batch-source | Jenkins Console Output + stdout | Jenkins 로그 + 프로세스 stdout |
   | Jenkins | `~/.jenkins/logs/` + Console Output | 파일 수집 |

2. **애플리케이션 로그 포맷 설정**
   - 로그 집계 시스템이 파싱할 수 있는 구조화된 로그 포맷 설정 (예: JSON 로그)
   - 기존 로그 설정을 변경해야 하는 경우, 변경 범위와 이유를 명시
   - Trace ID를 로그에 포함하여 Traces ↔ Logs 상관관계(correlation) 연결

3. **로그 저장소 설정**
   - 보존 기간 및 인덱싱 설정 (로컬 테스트 환경 기준)

**작성 지침:**
- batch-source는 tmux가 아닌 Jenkins Pipeline에서 `java -jar`로 실행되므로, Jenkins Console Output이 주요 로그 소스임을 고려
- 로그 ↔ 트레이스 상관관계를 위해 Trace ID가 로그에 자동 포함되는 설정이 핵심

---

### 섹션 6. 알림(Alerting) 설정

관측 데이터 기반으로 장애를 자동 감지하고 알림을 발송합니다.

**포함 내용:**

1. **알림 규칙 (Alert Rules)**

   Jenkins와 Spring Batch의 운영 안정성을 위한 최소 알림 규칙을 정의합니다:

   | 알림명 | 조건 | 심각도 | 설명 |
   |---|---|---|---|
   | BatchJobFailed | Batch Job exit code ≠ 0 | critical | Batch Job 실패 즉시 알림 |
   | BatchJobSlowExecution | Job 실행 시간 > 임계치 | warning | 평소 대비 비정상적으로 긴 실행 |
   | JenkinsDown | Jenkins 메트릭 수집 실패 지속 | critical | Jenkins 프로세스 다운 |
   | JenkinsBuildFailed | 빌드 실패 연속 N회 | warning | 반복 빌드 실패 |
   | HighMemoryUsage | JVM Heap 사용률 > 임계치 | warning | Jenkins 또는 Batch 메모리 부족 |

   > 위 표는 최소 권장 규칙입니다. 섹션 3에서 수집 가능한 지표를 기반으로 조정하세요.

2. **알림 규칙 파일 작성**
   - Prometheus alert rules 파일 (`.yml`) 전체 내용
   - 각 규칙의 PromQL 표현식, `for` 대기 시간, labels, annotations 포함

3. **알림 채널 설정**
   - 로컬 테스트 환경에서 사용 가능한 알림 수신 방법 (예: Slack webhook, email 등)
   - Alertmanager 설정 파일의 `receivers` 및 `route` 구성
   - 심각도별 알림 라우팅 (critical → 즉시, warning → 집계 후 발송)

**작성 지침:**
- 알림 규칙은 과도하게 많지 않게, 운영 안정성에 직결되는 핵심 지표만 포함
- 알림 피로(alert fatigue) 방지를 위해 적절한 `for` 대기 시간과 그룹핑 설정 포함

---

### 섹션 7. Grafana 대시보드 설계

수집된 Metrics, Traces, Logs를 통합 시각화하는 대시보드를 설계합니다.

**7-1. Jenkins 대시보드**

| 패널 | 시각화 타입 | 표시 지표 |
|---|---|---|
| 빌드 현황 | Stat | 최근 빌드 성공/실패 수 |
| 빌드 소요 시간 추이 | Time Series | Job별 빌드 시간 트렌드 |
| JVM 메모리 | Gauge | Heap 사용률 |

> 위 표는 최소 권장 패널입니다. 섹션 3에서 수집 가능한 지표를 기반으로 조정하세요.

**7-2. Spring Batch 대시보드**

| 패널 | 시각화 타입 | 표시 지표 |
|---|---|---|
| Job 실행 결과 | Stat | 최근 실행 성공/실패 |
| Job 실행 시간 추이 | Time Series | Job별 실행 시간 트렌드 |
| Step 처리량 | Bar Gauge | Read/Write/Skip 카운트 |
| JVM 리소스 | Time Series | 메모리, GC |

> 위 표는 최소 권장 패널입니다. 섹션 3에서 수집 가능한 지표를 기반으로 조정하세요.

**7-3. 통합 Observability 뷰**

Grafana의 데이터 소스 간 상관관계 기능을 활용하여:
- 대시보드에서 특정 시점의 메트릭 이상 → 해당 시점의 트레이스 조회 (Metrics → Traces 연결)
- 트레이스에서 에러 Span → 해당 Trace ID의 로그 조회 (Traces → Logs 연결)
- 이 연결이 가능하도록 Grafana 데이터 소스 간 상관관계 설정을 포함할 것

**작성 지침:**
- 대시보드는 JSON 모델이 아닌 **패널 구성 명세** 수준으로 작성 (PromQL/LogQL 쿼리 포함)
- Grafana Community 대시보드 중 재사용 가능한 것이 있다면 ID와 함께 안내
- Metrics ↔ Traces ↔ Logs 간 상관관계 탐색 흐름을 명시

---

## 공통 작성 원칙

1. **기존 설정 활용 우선**: 이미 프로젝트에 존재하는 의존성, 설정, 엔드포인트를 최대한 활용합니다. 중복 추가하지 않습니다.
2. **최소 구성**: 로컬 테스트 환경에서 동작하는 최소한의 설정만 포함합니다. 프로덕션 레벨의 고가용성, 클러스터링, 장기 보존(수개월 이상) 등은 범위 밖입니다.
3. **실행 가능성**: 모든 명령어, 설정 파일, UI 경로는 복사-붙여넣기로 즉시 사용 가능해야 합니다.
4. **공식 출처 준수**: 기술적 근거가 필요한 경우 다음 공식 문서만 참조합니다:
   - Prometheus: https://prometheus.io/docs/
   - Grafana: https://grafana.com/docs/grafana/latest/
   - Jaeger: https://www.jaegertracing.io/docs/
   - Grafana Loki: https://grafana.com/docs/loki/latest/
   - Alertmanager: https://prometheus.io/docs/alerting/latest/alertmanager/
   - OpenTelemetry: https://opentelemetry.io/docs/
   - Spring Boot Actuator: https://docs.spring.io/spring-boot/reference/actuator/
   - Spring Batch: https://docs.spring.io/spring-batch/reference/
   - Micrometer: https://micrometer.io/docs/
   - Jenkins: https://www.jenkins.io/doc/
5. **오버엔지니어링 경계**: Observability 3축(Metrics, Traces, Logs)과 Alerting은 모두 범위 내입니다. 단, 각 컴포넌트를 로컬 테스트에 필요한 최소 수준으로 구성하고, 불필요한 중복이나 과잉 설정은 지양합니다.

## 출력 형식

- Markdown 형식으로 작성
- 코드 블록에는 언어 태그 명시 (```yaml, ```bash, ```groovy, ```promql 등)
- 각 섹션은 `##` 헤딩으로 구분
- 설정 파일은 프로젝트 루트(`tech-n-ai-backend/`) 기준 상대 경로를 파일명 상단에 명시
