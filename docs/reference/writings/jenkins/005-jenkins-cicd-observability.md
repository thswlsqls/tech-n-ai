# Jenkins를 관측 가능하게 — Prometheus Plugin, OpenTelemetry Plugin, Pushgateway

CI/CD 파이프라인은 실패하면 가급적 빨리 알아차려야 합니다. 빌드가 20초 더 느려진 이유, 큐에 빌드가 쌓이는 이유, 어제까지 멀쩡하던 Job이 오늘 갑자기 실패한 이유를 사람의 기억이나 Console Output을 뒤로 감아가며 찾는 데에는 분명한 한계가 있습니다. 애플리케이션 서버에 APM을 붙이듯, CI/CD 자체에도 관측 가능성(Observability)이 필요하다고 느끼게 되는 이유입니다.

이 글은 Spring Boot API 6종과 단명 Spring Batch 한 벌을 로컬 단일 노드 Jenkins로 운영하는 참고 프로젝트에 Metrics, Traces, Logs 세 축을 붙이면서 정리한 기록입니다. Jenkins Prometheus Plugin과 OpenTelemetry Plugin, 그리고 단명 프로세스를 위한 Prometheus Pushgateway를 중심으로 살펴보고, 마지막에는 로컬 단일 노드 조건에서 운영하며 얻은 감상을 덧붙입니다. 설정 근거는 가급적 각 도구의 공식 문서를 1차 출처로 삼았습니다.

## 왜 CI/CD에도 Observability인가

CI/CD를 "돌아가기만 하면 되는 자동화"로 다루면, 이상 징후는 거의 언제나 사후에야 발견됩니다. 빌드 시간이 조금씩 늘어나다 임계치를 넘어 팀 알림이 울릴 때, 큐에 쌓인 빌드가 팀 전체의 배포를 막고 있을 때, 간헐적으로 실패하는 Flaky 빌드가 재실행으로 조용히 덮여 있을 때 — 이런 신호는 사람의 기억보다는 메트릭이 있어야 보입니다.

참고 프로젝트는 batch-source의 단명 프로세스 특성 때문에 처음부터 관측성을 전제로 파이프라인을 설계했습니다. Spring Batch Job은 실행이 끝나면 프로세스가 종료되므로 Prometheus의 Pull 모델로는 메트릭을 가져올 수 없다는 구조적 제약이 있기 때문입니다. 이 한 가지 제약만으로도 "수집 방식을 먼저 정해야 모니터링을 만들 수 있다"는 결론이 자연스럽게 따라옵니다.

Observability의 세 축(Metrics, Traces, Logs) 가운데 CI/CD에서 가장 먼저 도입 이득이 큰 것은 메트릭으로 보입니다. 빌드 소요 시간 회귀, Queue 적체, 성공/실패 카운터는 Prometheus 한 군데만 있어도 상당 부분 답이 나옵니다. 그 위에 Traces가 얹히면 개별 빌드의 Stage별 소요 구성을 한눈에 볼 수 있고, Logs까지 붙으면 추적 중인 Span과 로그를 같은 화면에서 상관지을 수 있습니다. 순서를 지키지 않으면 안 된다는 뜻은 아니고, 투자 대비 회수가 가장 큰 쪽부터 접근하는 편이 덜 지친다는 정도의 경험담입니다.

## Metrics — Jenkins Prometheus Plugin

Jenkins Prometheus Plugin(https://plugins.jenkins.io/prometheus/)은 `/prometheus/` 엔드포인트를 통해 메트릭을 노출합니다. 설치하면 `Jenkins 관리` > `System`에 Prometheus 섹션이 생기고, 기본값으로도 `default_jenkins_builds_duration_milliseconds_summary`, `default_jenkins_builds_success_build_count_total`, `default_jenkins_builds_failed_build_count_total` 같은 메트릭이 자동으로 노출됩니다. 공식 플러그인 페이지가 노출 엔드포인트와 옵션의 1차 출처입니다.

참고 프로젝트에서 눈여겨볼 선택은 `Add build parameter label to metrics`를 체크하지 않은 쪽입니다. 빌드 파라미터를 라벨로 붙이면 당장은 편리해 보이지만, 자유로운 문자열 값이 들어가는 파라미터일수록 Prometheus의 시계열이 무한히 증식하는 카디널리티 폭발로 이어집니다. 참고 프로젝트에서 이 옵션을 끈 이유도 같은 맥락이었습니다. Prometheus 공식 Instrumentation 가이드 역시 "Use labels, but be careful about cardinality"라는 문장으로 라벨을 낮게 유지할 것을 반복해 권고합니다. 관측성을 얻겠다고 옵션을 모두 켜 버리면 오히려 Prometheus 자체가 관측 불가능해지는 아이러니가 생기기 쉬운 지점입니다.

또 하나 유용한 옵션은 `Collect metrics for each run per build`입니다. 이것을 켜면 빌드별 상세 메트릭(예: `default_jenkins_builds_last_build_duration_milliseconds`)이 노출되어 개별 빌드의 소요 시간을 추적할 수 있습니다. 대신 `Per-build metrics max age in hours`와 `Per-build metrics max builds per job`를 `0`(제한 없음)으로 두면 이력이 계속 누적되므로, 운영 환경에서는 적절한 상한을 설정하는 편이 안전합니다. 로컬 환경에서는 Item 수가 적어 제한 없음도 부담이 크지 않았지만, Item 수백 개 규모가 되면 다른 이야기일 겁니다.

수집 주기(`Collecting metrics period in seconds`)는 참고 프로젝트에서 `55`로 두었습니다. 기본값은 120초이고, Item 수가 적은 로컬 환경에서는 이 정도의 갱신 주기로도 CPU 부담이 거의 없고 빌드 상태 반영이 체감상 빨라졌습니다. 반대로 Item 수백 개를 운용하는 환경이라면 기본값을 유지하거나 더 길게 두는 쪽이 적절할 것으로 보입니다.

노출된 메트릭은 Prometheus가 일정한 간격으로 Pull합니다. 참고 프로젝트에서 `scrape_interval`을 15초로 고정한 이유는 Prometheus 공식 문서가 일반적으로 예시로 드는 값과 맞추고 싶었기 때문이고, 이 값을 낮추면 해상도는 올라가지만 저장 공간과 CPU가 함께 증가합니다. 로컬 단일 노드 기준이라면 15초로 두고, 필요할 때만 특정 Job에 override하는 방식이 현실적인 타협점으로 느껴졌습니다.

## Traces — Jenkins OpenTelemetry Plugin과 Stage-level Span

CI/CD에 트레이스를 붙이는 가장 큰 이유는 "어느 Stage가 느려서 전체 빌드가 느려졌는가?"에 바로 답하기 위함입니다. Jenkins OpenTelemetry Plugin(https://plugins.jenkins.io/opentelemetry/)을 설치하면 Pipeline Run이 Root Span이 되고, 각 Stage가 Child Span으로 이어지는 트레이스가 자동으로 생성됩니다. 별도 계측 코드는 필요 없고, `Jenkins 관리` > `System`의 OpenTelemetry 섹션에서 OTLP Endpoint(예: `http://localhost:4317`)만 지정하면 됩니다.

자동 생성되는 트레이스는 다음과 같은 구조를 갖습니다.

```
Pipeline Run (Root Span)
├── Stage: Prepare Workspace
├── Stage: Git Checkout
├── Stage: Build JAR
└── Stage: Archive & Link
```

참고 프로젝트의 CI Jenkinsfile이 쓰는 `stages { ... }` 블록과 정확히 1:1로 대응되기 때문에, 별도 계측 없이도 Span 트리가 파이프라인 구조 그대로 나옵니다. 각 Span의 attribute에는 Jenkins Build URL, 파라미터, 결과 등이 자동으로 담기므로, Jaeger UI에서 Span 하나만 클릭해도 해당 빌드로 곧장 이동할 수 있습니다.

중요한 점은 이 관측성이 Declarative Pipeline일 때 주로 얻어진다는 것입니다. Freestyle Job은 Stage 개념 자체가 없으므로 Stage Span이 만들어지지 않습니다. 참고 프로젝트에서 Pipeline을 고른 이유가 "Jenkinsfile이 Git으로 관리된다" 수준에서 끝나지 않고 관측성까지 이어지는 연쇄적 이점에 있다고 정리한 배경이 여기에 있습니다. CI/CD 도구 선택 단계에서 Pipeline을 고르는 판단은 관측성 관점에서 한 번 더 설득력을 얻게 됩니다.

샘플링은 로컬 환경에서는 `1.0`(100%)으로 두는 것이 디버깅에 편했고, 프로덕션이라면 0.1~0.3 사이에서 조정하는 사례가 일반적으로 소개되곤 합니다. Jenkins OpenTelemetry Plugin은 OTLP gRPC(4317)와 HTTP(4318)를 모두 지원하는데, 참고 프로젝트에서는 Jenkins 쪽을 gRPC(4317)로, Spring Boot 쪽을 HTTP(4318)로 나눠 두었습니다. 한 인프라 안에서 두 방식이 공존해도 Jaeger 2.x의 OTLP receiver가 둘 다 받아 주므로 문제가 되지 않는다는 점(Jaeger 공식 문서, https://www.jaegertracing.io/docs/2.6/configuration/)이 이 분리 결정의 근거였습니다.

v1에서 v2로 넘어오면서 `COLLECTOR_OTLP_ENABLED` 같은 환경변수 방식 설정이 YAML 설정 파일 방식으로 바뀐 점은, 기존 Jaeger 사용자가 업그레이드할 때 한 번은 반드시 걸리는 지점이라고 합니다. 참고 프로젝트처럼 신규 구축하는 입장에서는 처음부터 v2 기준 설정을 작성하면 되므로 오히려 덜 번거로웠습니다.

## 단명 Batch 프로세스를 위한 Pushgateway

Spring Batch로 작성된 batch-source는 `spring.main.web-application-type=none`이 설정된 단명 프로세스입니다. Job이 끝나면 JVM이 내려가므로 `/actuator/prometheus`를 Prometheus가 Pull할 시점 자체가 존재하지 않습니다. Prometheus 공식 문서(https://prometheus.io/docs/practices/pushing/)가 권고하는 표준 해결책이 Pushgateway입니다. 공식 문서의 문장을 그대로 옮기면, Pushgateway는 "allows you to push metrics from jobs which cannot be scraped"인 중간 서비스입니다. 같은 문서는 Pushgateway가 "service-level batch jobs" 용도로 적합하며, 일반적인 수명 있는 인스턴스를 위한 것은 아니라는 점도 명시합니다. 즉 "Pull이 어렵다고 아무 프로세스에나 Push를 붙이지는 말 것"이라는 주의사항이 함께 붙어 있는 셈입니다.

참고 프로젝트에서는 의존성으로 `io.prometheus:prometheus-metrics-exporter-pushgateway`를 썼습니다. 과거 버전에서 자주 보이던 `io.prometheus:simpleclient_pushgateway` 계열은 Prometheus Java Client 1.0 이후 `io.prometheus:prometheus-metrics-*` 모듈로 세대 교체되었고, 기존 `simpleclient` 브랜치는 아카이브됐습니다. 최신 세대 클라이언트로 맞춘 근거는 Micrometer의 Prometheus Registry 문서(https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html)와 Prometheus Java Client의 공식 저장소에서 확인할 수 있습니다.

설정에서 가장 신경 써야 할 항목은 `shutdown-operation`입니다. 핵심 프로퍼티만 뽑으면 다음 세 줄 정도입니다.

```yaml
pushgateway:
  push-rate: 10s
  shutdown-operation: POST
```

`shutdown-operation: POST`는 프로세스 종료 시 마지막 메트릭을 Pushgateway에 한 번 더 밀어 넣고 기존 메트릭을 유지합니다. 대안인 `DELETE`는 Pushgateway에서 해당 grouping key의 메트릭을 제거하는 정리 용도입니다. 참고 프로젝트에서 `POST`를 고른 이유는 Spring Batch Job의 마지막 실행 결과를 대시보드에 남기고 싶었기 때문이고, 실행 결과의 "스냅샷"만 필요하고 오래된 결과를 정리하고 싶다면 `DELETE`도 유효한 선택입니다. `push-rate: 10s`는 실행 중인 Job이 주기적으로 메트릭을 보내는 간격이며, Prometheus의 `scrape_interval: 15s`와 함께 맞물려 전체 해상도가 결정됩니다.

Prometheus의 scrape 설정에서 `honor_labels: true`를 두는 것도 중요합니다. Pushgateway가 받아 둔 메트릭에는 batch-source가 push한 `job`, `instance` 라벨이 이미 붙어 있는데, 이 옵션이 꺼져 있으면 Prometheus가 자신의 job 라벨로 덮어쓰게 됩니다. 결과적으로 `job="batch-source"` 라벨로 필터링이 되지 않는 이상한 상황이 벌어집니다. 참고 프로젝트의 prometheus.yml에서 pushgateway target에만 `honor_labels: true`를 붙인 이유가 여기에 있습니다. Prometheus 공식 설정 문서의 `honor_labels` 설명도 "Pushgateway 시나리오에서 일반적으로 필요하다"는 취지로 안내합니다.

Spring Batch가 자동 노출하는 메트릭은 Spring Batch 공식 문서의 Monitoring and Metrics(https://docs.spring.io/spring-batch/reference/monitoring-and-metrics.html)에 정리되어 있습니다. `spring.batch.job`(Timer)은 Job 단위 실행 시간, `spring.batch.step`(Timer)은 Step 단위 실행 시간, `spring.batch.item.read`·`spring.batch.item.process`·`spring.batch.chunk.write`는 각각 Item과 Chunk 단계의 시간을 측정합니다. 한 가지 주의할 점은 read/write/skip **카운트**는 Micrometer 자동 계측에 포함돼 있지 않다는 점입니다. `StepExecution`에는 존재하지만 메트릭으로 자동 노출되지는 않으므로, 이 수치를 대시보드에 띄우려면 `StepExecutionListener`를 직접 작성해 Pushgateway에 밀어 넣는 식이 됩니다. 참고 프로젝트에서는 초기 구축 단계에서 자동 계측 범위만 쓰고, 필요성이 확인되면 추가하기로 미뤄 둔 상태입니다.

## Logs — Loki + Promtail로 Jenkins 로그 통합

메트릭과 트레이스를 붙여두면 이상 징후 포착 속도와 원인 구간을 좁히는 속도는 올라갑니다. 하지만 "그 빌드가 왜 실패했는지"를 마지막에 확인하려면 결국 로그가 필요합니다. Grafana Loki와 Promtail은 이 마지막 퍼즐을 담당합니다. Loki 공식 문서(https://grafana.com/docs/loki/latest/configure/)는 라벨 기반 인덱싱과 chunk 저장 구조를 기반으로 설계되어 있다는 점을 강조하며, 결과적으로 로그 전체를 풀텍스트 인덱싱하는 방식 대비 리소스 비용을 낮게 가져갑니다.

참고 프로젝트는 `${JENKINS_HOME}/logs/*.log`를 Promtail이 직접 수집하도록 구성했습니다. Promtail의 scrape_config를 Jenkins 로그용(`job: jenkins` 라벨)과 애플리케이션 로그용(`job: spring-boot` 라벨, `/app-logs/**/*.log`)로 나눈 이유는, Jenkins Item 하나가 실행될 때마다 남는 로그와 그 Item이 실행시킨 Spring Boot 프로세스의 로그를 같은 Loki에 담되 라벨만으로도 구분이 가능하게 하고 싶었기 때문입니다. 파이프라인을 구현하면서 한 Loki 안에서 "CI/CD 레이어의 로그"와 "애플리케이션 레이어의 로그"를 동시에 보는 경험이 의외로 큰 도움이 됐습니다.

이 배치에서 가장 보람을 느낀 지점은 Promtail의 `structured_metadata` 스테이지였습니다. JSON 로그의 `traceId`, `spanId`를 라벨로 승격시키면 카디널리티가 터지기 쉬운데, structured_metadata로 보존하면 라벨 카운트를 늘리지 않고도 나중에 Grafana에서 derived field로 꺼내 쓸 수 있습니다. 참고 프로젝트의 Loki 설정에서 `limits_config.allow_structured_metadata: true`를 명시적으로 켜둔 이유도 이 때문이었습니다. 기본값에 맡기지 않고 플래그를 분명히 둔 쪽을 택한 배경에는, Grafana Loki 문서가 `traceId`처럼 고유값이 큰 필드를 라벨 대신 structured metadata로 다루라고 권고하는 지침이 있습니다.

Grafana 쪽에서는 두 가지 상관관계 설정이 핵심입니다. Jaeger 데이터 소스의 `tracesToLogsV2`는 트레이스 Span 상세 화면에서 해당 traceId로 Loki를 자동 쿼리하여 같은 시점의 로그를 옆에 띄워줍니다. 반대로 Loki 데이터 소스의 `derivedFields`는 로그 속 `traceId` 문자열을 클릭 가능한 링크로 바꿔 Jaeger 트레이스로 이동시킵니다. 참고 프로젝트에서 이 두 설정을 한 쌍으로 묶어 두었던 이유는, 트레이스와 로그가 서로를 한 번의 클릭으로 불러올 수 있어야 비로소 "통합된 관측성"이라고 부를 수 있다는 실감 때문이었습니다. 빌드 하나가 실패하면 Grafana 대시보드 → 해당 시점의 트레이스 → 트레이스의 로그 → 다시 다른 트레이스 식으로 꼬리를 물고 탐색할 수 있는데, 이 흐름이 Observability 3축을 갖춘 스택의 실질적 가치라고 느꼈습니다.

## 운영하며 얻은 팁

로컬 단일 노드 환경에서 한동안 이 스택을 돌려보고 나서 정리한 몇 가지가 있습니다. 일반화라기보다는 "이 참고 프로젝트의 조건에서 이렇게 두니 문제가 덜 생겼다"는 기록에 가깝습니다.

첫째, Prometheus 데이터 보존(`storage.tsdb.retention.time`)은 `7d`로 두는 편이 적당했습니다. 로컬 환경에서 7일보다 긴 이력을 보관하는 것은 디스크 대비 이득이 크지 않았고, 실제로 회귀 분석에 쓰는 구간은 대부분 최근 며칠 이내였습니다. 장기 추세가 필요하다면 Prometheus 단독보다는 별도의 원격 저장(예: Thanos, Mimir 등)으로 옮기는 쪽이 구조적으로 맞는 방향으로 보입니다.

둘째, `scrape_interval`은 15초로 고정해두고 특정 Job만 더 잦은 주기가 필요할 때 Job 단위로 override하는 패턴이 안정적이었습니다. Prometheus 공식 문서가 흔히 예시로 드는 값이기도 하지만, 경험적으로도 15초보다 짧게 두면 Push 모델에서 오는 메트릭(Pushgateway 경유)과의 간섭이 애매해지는 구간이 생겼습니다. Push와 Pull의 주기를 비슷한 스케일로 맞춰두면 대시보드의 해상도가 고르게 나옵니다.

셋째, Jaeger 2.x의 인메모리 저장은 로컬 테스트용으로만 적합합니다. Jaeger 공식 문서의 Configuration 섹션도 memory backend는 개발용이라는 점을 분명히 합니다. 재시작하면 트레이스가 사라지므로, 팀 단위로 이 스택을 공유하기 시작한다면 가장 먼저 영속 backend(Cassandra, Elasticsearch/OpenSearch, 혹은 ClickHouse 등)로 옮기는 것이 현실적인 다음 단계였습니다. v1에서 v2로 넘어오면서 설정 방식이 크게 바뀐 지점이 있다는 점은 기존 사용자 입장에서 한 번 더 체크할 만한 지점입니다.

---

개인적으로 이 작업을 하면서 가장 크게 바뀐 건 CI/CD를 바라보는 관점이었던 것 같습니다. 예전에는 Jenkinsfile을 짜는 행위와, 그 파이프라인이 얼마나 잘 돌고 있는지를 지켜보는 행위가 별개로 느껴졌는데, Pipeline Stage가 Span이 되고 빌드 카운터가 Counter 메트릭이 되는 순간부터는 "같은 시스템의 두 단면"이라는 감각이 생겼습니다. 여전히 이 스택이 모든 조직에 맞는 정답일 리는 없고, 특히 Kubernetes 기반 환경이나 Jenkins Shared Library를 적극적으로 쓰는 조직이라면 고려할 지점이 더 많을 겁니다. 이번 글은 그중 로컬 단일 노드, Spring Boot 상주형과 Spring Batch 단명형을 동시에 운영하는 조건에서 정리한 한 가지 해석 정도로 봐주시면 감사하겠습니다.

---

## 참고

글에서 언급한 Jenkinsfile, Pushgateway·Promtail 설정, Observability 스택 구성은 아래 참고 프로젝트 저장소에서 확인하실 수 있습니다.

- Backend: https://github.com/thswlsqls/tech-n-ai-backend
- Frontend: https://github.com/thswlsqls/tech-n-ai-frontend

---

## SEO 블로그 제목 후보

- Jenkins 파이프라인을 관측 가능하게 — Prometheus, OpenTelemetry, Pushgateway로 CI/CD에 APM 붙이기
- 단명 Spring Batch 메트릭, Prometheus Pushgateway로 잃지 않는 법 — Jenkins Observability 실전
- Jenkins OpenTelemetry Plugin으로 Pipeline Stage를 Span으로: 자바 개발자의 CI/CD 관측성 구축기
- Jenkins CI/CD 모니터링 스택 구축기 — Prometheus·Jaeger·Loki 조합으로 보는 빌드 관측성
