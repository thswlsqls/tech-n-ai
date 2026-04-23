# Jenkins CI/CD 기술 블로그 시리즈 — 주제 추출 결과

> 참고자료: `docs/jenkins/api/01-jenkins-api-cicd-deploy-design.md`, `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md`, `docs/jenkins/batch/02-jenkins-batch-apm-design.md`, `scripts/jenkins/api/Jenkinsfile-cicd`, `scripts/jenkins/api/Jenkinsfile-deploy`, `scripts/jenkins/batch/Jenkinsfile-cicd`, `scripts/jenkins/batch/Jenkinsfile-scheduler`

## 시리즈 개요

- **시리즈 제목**: "Java 개발자를 위한 Jenkins CI/CD 실전기 — SDLC 발전사부터 Pipeline 최적화, 관측성까지"
- **전체 메시지**: CI/CD 선택은 기술 유행이 아니라 SDLC의 변화가 만들어낸 필연이다. Spring Boot/Batch의 서로 다른 수명주기에 맞춰 Jenkins 기능을 깊게 쓸 때 CI/CD는 '자동화 스크립트'에서 '관측 가능한 시스템'으로 진화한다.

| 편 | 제목 | 핵심 메시지 |
|---|---|---|
| 1편 | CI/CD는 왜 태어났나 — SDLC 관점에서 다시 읽는 Jenkins 등장기 | CI/CD는 배포 자동화 이전에, SDLC 피드백 루프 단축을 위한 프랙티스다 |
| 2편 | Spring Boot와 Spring Batch의 CI/CD — 6가지 선택지와 Jenkins를 선택하는 기준 | 상주형(Boot)과 단명형(Batch)은 같은 CI/CD 도구로 풀리지 않는다. 요구사항 축으로 고르자 |
| 3편 | Jenkins Declarative Pipeline 실전 설계 — 파라미터화·Credentials·PID·Graceful Shutdown | Jenkins의 진가는 '된다/안 된다'가 아니라 '운영에서 버틴다/무너진다'에 있다 |
| 4편 | Jenkins를 관측 가능하게 — Prometheus Plugin, OpenTelemetry Plugin, Pushgateway | Pipeline을 Trace로, Build를 Metric으로 바꾸는 순간 CI/CD가 관측 대상이 된다 |

---

## 1편. CI/CD는 왜 태어났나 — SDLC 관점에서 다시 읽는 Jenkins 등장기

- **핵심 메시지**: CI/CD는 Jenkins가 만든 게 아니라, Waterfall → Agile → DevOps로 이어지는 SDLC 피드백 루프 단축 요구가 만든 결과물이다.
- **타깃 독자/선수 지식**: Spring Boot 경험 1~3년차, Git/Gradle 사용 경험. SDLC 단계 용어 친숙성.
- **상세 목차**:
  - **H2. Waterfall의 한계와 "통합 지옥(Integration Hell)"**
    - 빅뱅 통합 실패율, 회귀 비용이 SDLC 후반부에 폭증하는 구조
    - Martin Fowler의 Continuous Integration 정의 인용
  - **H2. Agile과 XP가 요구한 것 — "매일 통합하라"**
    - eXtreme Programming의 CI 프랙티스와 자동 빌드의 등장
  - **H2. Hudson에서 Jenkins로 — 오픈소스 거버넌스 분기**
    - 2004 Hudson 공개, 2011 Oracle 상표권 분쟁과 Jenkins 포크 (Jenkins 프로젝트 공식 히스토리)
  - **H2. DevOps·CD의 추가 — Continuous Delivery에서 Deployment로**
    - Humble & Farley의 Continuous Delivery (2010) 8원칙
  - **H2. Jenkins가 표준이 된 기술적·생태계적 이유**
    - Pipeline-as-Code(Jenkinsfile) 도입, 2,000+ 플러그인, 온프레미스 친화성
    - 참고자료의 `Jenkinsfile-cicd`가 "Git으로 버전 관리되는 Pipeline"이라는 예시로 인용
- **참고자료 인용**:
  - `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md` §3.2 (Freestyle vs Pipeline 비교표) — Pipeline-as-Code의 실무 가치 근거
  - `scripts/jenkins/batch/Jenkinsfile-cicd` — "Jenkinsfile이 Git에 커밋되는 모습"의 구체 예시
- **외부 공식 출처**:
  - Jenkins Project — History (https://www.jenkins.io/project/history/) — Hudson→Jenkins 분기 1차 사료
  - Martin Fowler — Continuous Integration (https://martinfowler.com/articles/continuousIntegration.html) — CI 용어 정의의 원전
  - Jez Humble — Continuous Delivery 개요 (https://continuousdelivery.com/) — CD 8원칙의 저자 공식 사이트
- **예상 분량**: 한글 2,500~3,000 단어
- **연결 고리**: 다음 편에서 "그래서 2026년에도 Jenkins를 고르는 게 맞나?"를 선택지 비교로 이어감.

---

## 2편. Spring Boot와 Spring Batch의 CI/CD — 6가지 선택지와 Jenkins를 선택하는 기준

- **핵심 메시지**: Boot(상주형)와 Batch(단명형)는 수명주기가 다르므로 CI/CD 도구 요구사항도 다르다. "Jenkins냐 아니냐"가 아니라 "어떤 축에서 무엇이 유리한가"로 판단해야 한다.
- **타깃 독자/선수 지식**: Spring Boot/Batch 모두 작성해본 1~3년차. Docker·Kubernetes 기초.
- **상세 목차**:
  - **H2. Boot와 Batch의 CI/CD 요구사항 차이**
    - Boot: 상주 프로세스, graceful shutdown, Health Check, 무중단 기대
    - Batch: `web-application-type=none`, 단명 프로세스, cron 트리거, exit code 기반 성공 판단
    - 참고자료의 `Jenkinsfile-deploy`(Boot)와 `Jenkinsfile-scheduler`(Batch) 차이표 인용
  - **H2. 선택지 6종 비교 — Jenkins, GitHub Actions, GitLab CI, ArgoCD, Spinnaker, Tekton**
    - 축: 온프레미스 여부 / 스케줄링 내장 / Pipeline-as-Code / 플러그인 생태계 / 운영 비용 / GitOps 적합성
    - 각 도구 공식 문서를 1차 출처로 인용
  - **H2. 의사결정 프레임 — 4개 축으로 판단하기**
    - ① 인프라 소유권(온프레/매니지드)
    - ② 워크로드 수명주기(상주 vs 단명)
    - ③ 조직 규모와 운영 여력
    - ④ 기존 Observability 스택과의 통합성
  - **H2. 참고 프로젝트 케이스 스터디 — 왜 Jenkins를 골랐나**
    - 로컬/단일 노드 환경, Batch의 cron 스케줄 내장 필요, OpenTelemetry Plugin 확장성
    - 참고자료 §3.2의 Freestyle vs Pipeline 트레이드오프와 APM 플러그인 확장 여지
- **참고자료 인용**:
  - `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md` §3.2 — Freestyle vs Pipeline 결정표
  - `docs/jenkins/api/01-jenkins-api-cicd-deploy-design.md` §2.2 — Boot 배포 Pipeline의 PID 관리/HTTP Health Check 요구사항
  - `scripts/jenkins/batch/Jenkinsfile-scheduler`의 `environment` 블록 `JAR_PATH` 고정 경로 사용
  - `scripts/jenkins/api/Jenkinsfile-deploy`의 `Deploy` 스테이지 — `JENKINS_NODE_COOKIE=dontKillMe`로 자식 프로세스 분리
- **외부 공식 출처**:
  - Jenkins — Pipeline (https://www.jenkins.io/doc/book/pipeline/) — Declarative Pipeline 1차 레퍼런스
  - GitHub Actions Docs (https://docs.github.com/actions) — GitHub 호스티드 러너 비교 근거
  - Argo CD Docs (https://argo-cd.readthedocs.io/) — GitOps 기반 CD 도구 비교 근거
  - Spring Batch — Monitoring and Metrics (https://docs.spring.io/spring-batch/reference/monitoring-and-metrics.html) — Batch 단명 프로세스 메트릭 이슈 근거
  - Spring Boot — Graceful Shutdown (https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html) — Boot 상주형 배포 요구사항 근거
- **예상 분량**: 한글 3,500~4,000 단어
- **연결 고리**: 선택을 마친 뒤 "실제 Pipeline은 어떻게 써야 버티는가"를 3편으로.

---

## 3편. Jenkins Declarative Pipeline 실전 설계 — 파라미터화·Credentials·PID·Graceful Shutdown

- **핵심 메시지**: Jenkins의 기본값만 쓰면 Pipeline은 금방 깨진다. 참고 프로젝트가 실전에서 쓰는 다섯 가지 기법(파라미터 단일화, Credentials 이중보관, 심볼릭 링크, PID + 포트 폴백, graceful→SIGKILL)을 중심으로 설계 결정의 근거를 공개한다.
- **타깃 독자/선수 지식**: Jenkinsfile 기초, Groovy 문법 기초.
- **상세 목차**:
  - **H2. 파라미터화된 단일 Jenkinsfile로 6개 모듈 관리**
    - `choice` 파라미터 + Groovy Map으로 모듈별 JAR 경로/시크릿 분기
    - 인용: `scripts/jenkins/api/Jenkinsfile-cicd`의 상단 `module_source_path` Map 정의와 `parameters` 블록
  - **H2. Credentials 이중 보관 — Jenkins Store + macOS Keychain**
    - `Username with password` vs `Secret text` 선택 기준
    - 왜 동일 PAT을 2개 Credential(`github-pat-...`, `github-token-...`)로 나누는가
    - 인용: `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md` §1.3 "Credential 용도 구분", `security add-generic-password -T ""` 보안 이유
  - **H2. 심볼릭 링크 기반 산출물 핀(Pin) 전략**
    - `${JENKINS_HOME}/builds/{module}-{timestamp}.jar` + `ln -sf {module}.jar`
    - 배포 Pipeline이 항상 고정 경로를 참조, 롤백 시 링크만 교체
    - 인용: `scripts/jenkins/batch/Jenkinsfile-cicd`의 `Archive & Link` 스테이지
  - **H2. 상주 프로세스 제어 — PID 파일 + 포트 점유 폴백 + `JENKINS_NODE_COOKIE`**
    - `pkill -f` 패턴 오매칭 위험 vs PID 파일
    - `lsof -ti:${port}` 폴백으로 좀비 상태 방지
    - `JENKINS_NODE_COOKIE=dontKillMe`로 Jenkins가 자식 프로세스를 kill하지 않도록 분리
    - 인용: `scripts/jenkins/api/Jenkinsfile-deploy`의 `Stop Running Process` 스테이지와 `Deploy` 스테이지
  - **H2. Graceful Shutdown과 Health Check 루프**
    - SIGTERM 30초 대기 → SIGKILL 에스컬레이션이 Spring Boot의 `spring.lifecycle.timeout-per-shutdown-phase` 기본 30초와 맞춰진 이유
    - `/actuator/health/readiness` 30회 × 5초 재시도, 실패 시 배포 프로세스 정리
    - 인용: `scripts/jenkins/api/Jenkinsfile-deploy`의 `Stop Running Process` 스테이지 SIGTERM→SIGKILL 에스컬레이션 로직과 `Health Check` 스테이지
  - **H2. cron과 `H` 심볼 — 부하 분산 트리거**
    - `H */4 * * *`의 해시 분산 원리, Item별 cron 선택 근거
    - Jenkinsfile `triggers` vs Jenkins UI "Build periodically" — 중복 실행 사고 방지 기준
    - 인용: `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md` §3.2 "스케줄 관리 전략"
- **외부 공식 출처**:
  - Jenkins — Declarative Pipeline Syntax (https://www.jenkins.io/doc/book/pipeline/syntax/) — `parameters`, `post`, `environment` 사용법
  - Jenkins — Using Credentials (https://www.jenkins.io/doc/book/using/using-credentials/) — `withCredentials` 바인딩 1차 문서
  - Jenkins — Cron syntax (https://www.jenkins.io/doc/book/pipeline/syntax/#cron-syntax) — `H` 심볼 공식 설명
  - Spring Boot — Graceful Shutdown (https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html) — 30초 기본 타임아웃 근거
  - Spring Boot — Actuator Health (https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health) — `/actuator/health/readiness` 공식 정의
- **예상 분량**: 한글 4,500~5,500 단어
- **연결 고리**: 파이프라인이 돌기 시작했으니, 4편에서 "돌고 있는지 관측"으로 마무리.

---

## 4편. Jenkins를 관측 가능하게 — Prometheus Plugin, OpenTelemetry Plugin, Pushgateway

- **핵심 메시지**: CI/CD는 실패하면 알아야 한다. Jenkins의 두 플러그인(Prometheus, OpenTelemetry)과 Batch용 Pushgateway를 붙이면 Pipeline Stage가 Span이 되고, 단명 Batch도 메트릭을 잃지 않는다.
- **타깃 독자/선수 지식**: Prometheus/Grafana 기초. Observability 3축(Metrics/Traces/Logs) 용어 친숙성.
- **상세 목차**:
  - **H2. 왜 CI/CD에도 Observability인가**
    - 빌드 소요 시간 회귀, Queue 적체, Flaky 빌드는 메트릭 없이는 감지되지 않음
  - **H2. Metrics — Jenkins Prometheus Plugin**
    - `/prometheus/` 엔드포인트, 빌드 duration/성공·실패 counter
    - 카디널리티 폭발을 피하는 옵션 선택(`Add build parameter label` 미체크 이유)
    - 인용: `docs/jenkins/batch/02-jenkins-batch-apm-design.md` §3.1 "추가 메트릭 옵션", 수집 가능 지표 표
  - **H2. Traces — Jenkins OpenTelemetry Plugin과 Stage-level Span**
    - Pipeline Run(Root) → Stage(Child) 트레이스 구조
    - Declarative Pipeline만이 얻는 관측성 이득(Freestyle 불가)
    - 인용: `docs/jenkins/batch/02-jenkins-batch-apm-design.md` §4.2 "자동 생성되는 트레이스 구조"
  - **H2. 단명 Batch 프로세스를 위한 Pushgateway**
    - Pull 모델 한계와 Prometheus 공식 권고안
    - `io.prometheus:prometheus-metrics-exporter-pushgateway` + `shutdown-operation: POST`
    - 인용: `docs/jenkins/batch/02-jenkins-batch-apm-design.md` §3.2 "Pushgateway를 통한 단명 프로세스 메트릭 Push"
  - **H2. Logs — Loki + Promtail로 Jenkins 로그까지 한곳에**
    - Promtail이 `${JENKINS_HOME}/logs/*.log`를 직접 수집, structured_metadata로 `traceId` 보존
    - Grafana의 Traces ↔ Logs 상관 탐색(derivedFields)
    - 인용: `docs/jenkins/batch/02-jenkins-batch-apm-design.md` §2.6, §2.7 `tracesToLogsV2`
  - **H2. 실전 운영 팁 3가지**
    - ① 로컬 환경 retention 7일이 합리적인 이유 / ② `scrape_interval 15s` 고정 근거 / ③ Jaeger 인메모리 저장의 한계와 업그레이드 경로
- **외부 공식 출처**:
  - Prometheus — Pushing metrics (https://prometheus.io/docs/practices/pushing/) — Pushgateway 권고안 원전
  - Jenkins — Prometheus metrics plugin (https://plugins.jenkins.io/prometheus/) — 플러그인 공식 페이지
  - Jenkins — OpenTelemetry plugin (https://plugins.jenkins.io/opentelemetry/) — Stage Span 자동 생성 근거
  - Jaeger — Configuration 2.x (https://www.jaegertracing.io/docs/2.6/configuration/) — v1→v2 설정 변경 근거
  - Spring Batch — Monitoring and Metrics (https://docs.spring.io/spring-batch/reference/monitoring-and-metrics.html) — `spring.batch.*` 메트릭 1차 문서
  - Micrometer — Prometheus Registry (https://docs.micrometer.io/micrometer/reference/implementations/prometheus.html) — 노출 방식 근거
  - Grafana Loki — Configuration (https://grafana.com/docs/loki/latest/configure/) — Loki/Promtail 설정 근거
- **예상 분량**: 한글 4,000~4,500 단어
- **연결 고리**: 시리즈 마무리. 후속 주제(예: Jenkins Shared Library, Multibranch Pipeline, K8s agent 전환)는 별도 심화편 예고.

---

## 집필 시 공통 체크리스트

- 외부 링크는 반드시 위 "외부 공식 출처"만 사용. 개인 블로그/AI 생성물 인용 금지.
- 참고자료 인용은 `파일경로:라인` 포맷으로 명시.
- 참고자료에 **근거 없는 주장**(예: "실무에서는 모두 Kubernetes를 쓴다")은 작성 금지 — "본 프로젝트는 로컬 단일 노드 기준" 같이 맥락을 명시.
- 한국어 본문, 코드/명령/UI 텍스트는 영어 유지.
