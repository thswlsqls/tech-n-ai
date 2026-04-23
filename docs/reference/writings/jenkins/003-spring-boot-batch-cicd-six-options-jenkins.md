# Spring Boot와 Spring Batch의 CI/CD — 여섯 가지 선택지와 Jenkins를 선택하는 기준

Spring Boot와 Spring Batch를 같은 레포지토리에 두고 배포 자동화를 고민해 본 분이라면, 한 번쯤 비슷한 지점에서 멈칫하게 됩니다. Boot는 기동된 뒤 계속 살아 있어야 하고, Batch는 일을 끝내면 깔끔하게 꺼져야 합니다. 그런데 CI/CD 도구를 고를 때 우리는 보통 두 워크로드를 한데 묶어 "Jenkins를 쓸까, GitHub Actions를 쓸까"라는 이분법으로 접근합니다. 최근 참고 프로젝트에서 Boot 6개 모듈과 Batch 1개 모듈을 함께 운영하는 파이프라인을 설계하면서, 이 질문이 실은 도구 선택의 문제라기보다 워크로드 특성을 먼저 이해하는 문제라는 점을 다시 느꼈습니다.

이 글에서는 상주형 Boot와 단명형 Batch가 CI/CD 도구에 요구하는 특성이 어떻게 다른지 먼저 정리하고, 대표적인 여섯 가지 선택지를 같은 축 위에서 비교해 본 뒤, 본 프로젝트가 로컬 단일 노드 환경에서 Jenkins를 고른 이유를 공유하려 합니다. 도구를 고른다는 행위 자체가 옳고 그름의 문제가 아니므로, 이 글도 "어떤 도구가 정답"이라는 결론보다는 "어떤 축으로 판단해 볼 수 있는가"에 무게를 두었습니다.

## 상주형 Boot와 단명형 Batch, 같은 자바가 아닙니다

Spring Boot 애플리케이션과 Spring Batch 애플리케이션은 같은 JVM 위에서 같은 빌드 도구로 JAR을 만들지만, 프로세스 수명주기가 다릅니다. Spring Boot 공식 문서의 Graceful Shutdown 항목에 따르면, Jetty·Reactor Netty·Tomcat 세 내장 서버 모두에서 graceful shutdown이 기본 활성화되어 있어, SIGTERM을 받은 순간 신규 요청 수신을 중단하고 진행 중 요청을 일정 시간 기다립니다. 이 대기 시간은 `spring.lifecycle.timeout-per-shutdown-phase`로 제어하며, 비활성화하려면 `server.shutdown=immediate`를 명시해야 한다는 점도 같은 문서에 기술되어 있습니다(`https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html`). 반면 Spring Batch는 Job이 종료되는 순간 JVM도 함께 내려가는 구조가 기본입니다. Spring Batch 공식 레퍼런스의 Monitoring and Metrics 장에서도, Batch가 Micrometer로 메트릭을 발행하지만 "단명 프로세스(short-lived process)"이기 때문에 Prometheus의 기본 pull 모델과 자연스럽게 맞지 않는 특성이 있다는 점을 언급합니다(`https://docs.spring.io/spring-batch/reference/monitoring-and-metrics.html`).

이 차이가 CI/CD 파이프라인 설계에 직접 영향을 줍니다. Boot 배포는 "기존 프로세스를 안전하게 내리고, 새 프로세스를 띄우고, 헬스 체크로 기동 완료를 확인"하는 순서를 따라야 합니다. 이 프로젝트에서 API 배포 파이프라인을 직접 구현하면서, Stop 단계는 SIGTERM을 보낸 뒤 1초 간격으로 최대 30초까지 프로세스 종료를 폴링하고 그래도 남아 있으면 SIGKILL로 에스컬레이션하도록 구성했습니다. 30초라는 상한을 임의로 잡은 것이 아니라 Spring Boot의 `spring.lifecycle.timeout-per-shutdown-phase`가 통제하는 shutdown phase 대기 시간에 맞춘 이유는, 두 도구의 기본값이 서로를 베어내지 않도록 정렬해 두는 편이 가장 놀라움이 적었기 때문입니다. 뒤이어 Health Check 단계에서는 `/actuator/health/readiness`를 5초 간격으로 30회까지 재시도합니다. Readiness 엔드포인트를 쓰기로 정한 이유는 Spring Boot Actuator 문서가 정의하는 대로 "애플리케이션이 트래픽을 받을 준비가 되었는가"가 배포 파이프라인이 실제로 묻고 싶은 질문이기 때문입니다(`https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health`). 단순 프로세스 생존 여부만 확인하는 Liveness로는 Kafka Consumer나 벡터 저장소 초기화가 끝나지 않은 상태를 걸러내지 못합니다.

반면 Batch 파이프라인은 "기동"이라는 개념 자체가 다릅니다. 같은 프로젝트의 Batch 스케줄러 파이프라인을 직접 작성하면서 가장 크게 달랐던 지점은, `java -jar`를 foreground로 실행하고 그 exit code 하나로 성공 여부를 판정하도록 설계해 두었다는 점입니다. 상주 프로세스가 아니므로 PID 파일도, HTTP Health Check도 필요하지 않았습니다. 대신 관측 수단은 Job 실행 로그와 Spring Batch가 저장하는 `BATCH_JOB_EXECUTION` 메타데이터로 옮겨갑니다. 같은 자바 JAR이지만, 파이프라인이 기다려야 할 대상도, 실패 판정의 기준도 완전히 다른 셈입니다.

여기에 트리거 방식도 갈립니다. Boot는 보통 코드 변경이나 수동 요청으로 배포가 시작되지만, Batch는 cron 주기로 스스로 깨어나야 합니다. 참고 프로젝트에서 세 개의 Batch Job을 설계하면서 각각 `H */4 * * *`, `H */6 * * *`, `H H(0-6) * * *` 같은 서로 다른 표현식을 쓰고 `H` 심볼로 아이템 이름 해시 기반 분산을 적용한 이유는, 동시 실행이 로컬 단일 노드의 리소스 피크로 이어지는 경험을 줄이기 위해서였습니다. 이 기본 요건만 놓고 봐도, "CI/CD 도구에 cron 스케줄링이 내장되어 있는가"는 Boot만 배포할 때는 덜 중요하지만 Batch까지 포함하면 갑자기 중요한 축이 됩니다.

## 여섯 가지 선택지, 같은 축 위에 올려 두기

2026년 현시점에서 자바 프로젝트가 흔히 검토하는 CI/CD 도구는 대략 여섯 가지로 추려집니다. Jenkins, GitHub Actions, GitLab CI/CD, Argo CD, Spinnaker, Tekton입니다. 각 도구의 공식 문서를 1차 출처로 두고, 앞서 정리한 Boot/Batch 요구사항 관점에서 같은 축에 올려 보겠습니다. 단, 아래 비교는 본 프로젝트가 "로컬 단일 노드 + 일부 cloud VM" 환경을 전제로 검토한 결과라는 점을 밝혀 두는 편이 좋겠습니다. 동일한 비교를 대규모 Kubernetes 환경에서 다시 하면 우선순위가 달라질 여지가 충분히 있습니다.

Jenkins는 Jenkins Project의 공식 문서에서 소개하듯 온프레미스 설치와 Pipeline-as-Code를 표준으로 삼는 오픈소스 자동화 서버입니다(`https://www.jenkins.io/doc/book/pipeline/`). 플러그인 생태계가 크고, Declarative Pipeline 문법으로 `parameters`, `triggers`, `post` 같은 블록을 제공합니다. cron 스케줄링이 Pipeline 안에 직접 들어가고, Groovy 스크립트로 임의의 sh 명령을 엮을 수 있다는 점은 Batch 파이프라인에서 편한 측면입니다. 반대로 Jenkins Controller 자체의 운영 부담은 도구 선택과 별개로 따라옵니다.

GitHub Actions는 GitHub Docs에서 정의하듯 GitHub 레포지토리와 깊게 결합된 호스티드 러너 기반의 CI/CD 플랫폼입니다(`https://docs.github.com/actions`). YAML 워크플로우와 Marketplace 액션 생태계가 강점이고, `workflow_dispatch`·`push`·`schedule` 등 트리거가 풍부합니다. cron 스케줄도 `on.schedule`로 선언할 수 있어 Batch 트리거에 쓸 여지가 있지만, 호스티드 러너에서 상주형 Boot 프로세스를 돌리는 것은 적절하지 않고, 일반적으로 배포 대상 서버에 SSH로 들어가거나 registry를 경유하는 구조가 됩니다. 퍼블릭 클라우드와 GitHub 레포지토리를 전제로 할 때 가장 무난한 선택지 중 하나입니다.

GitLab CI/CD는 GitLab 공식 문서에서 기술하는 것처럼 GitLab과 Runner가 분리된 구조를 가진 통합형 CI/CD입니다(`https://docs.gitlab.com/ee/ci/`). `.gitlab-ci.yml` 하나로 빌드·테스트·배포 파이프라인을 정의하고, Runner를 온프레미스로 설치해 호스티드 모델과 셀프 호스트 모델을 절충할 수 있습니다. GitHub Actions와 구조가 유사하되 인프라 소유권이 다릅니다.

Argo CD는 Argo CD 공식 문서에 따르면 Kubernetes를 타깃으로 하는 GitOps 스타일의 Continuous Delivery 도구입니다(`https://argo-cd.readthedocs.io/`). Git 레포지토리의 선언적 상태를 Kubernetes 클러스터로 지속적으로 수렴시키는 구조이므로, 컨테이너화된 Boot 애플리케이션의 배포 자동화에 강점이 있습니다. 다만 Argo CD는 빌드를 담당하지 않고, 빌드 파이프라인은 별도 도구(Jenkins, GitHub Actions 등)와 조합하는 것이 일반적입니다. 본 프로젝트처럼 JAR을 직접 `java -jar`로 띄우는 non-Kubernetes 환경에서는 적합도가 떨어집니다.

Spinnaker는 Spinnaker 공식 문서에서 설명하듯 멀티 클라우드 Continuous Delivery에 특화된 플랫폼입니다(`https://spinnaker.io/docs/`). Canary, Blue/Green 같은 배포 전략을 기본 개념으로 제공하고, Kubernetes·EC2·GCE 등 여러 플랫폼을 동시에 타깃할 수 있습니다. 조직 규모와 운영 인력이 받쳐 주는 상황에서 강점이 있지만, 구성 요소가 많고 운영 비용이 결코 작지 않습니다.

Tekton은 Tekton 공식 문서에서 "Kubernetes-native CI/CD 빌딩 블록"으로 소개되는 CRD 기반 파이프라인 프레임워크입니다(`https://tekton.dev/docs/`). `Task`·`Pipeline`·`PipelineRun` 같은 리소스로 파이프라인을 Kubernetes 위에 선언적으로 정의하고, 대시보드나 Trigger 등 주변 컴포넌트를 필요에 따라 조합합니다. Kubernetes 플랫폼을 이미 전제로 하는 조직에 적합한 방향입니다.

이 여섯을 동일한 축으로 간단히 비교해 보면 다음과 같습니다. 표를 덧붙이는 이유는 한 화면에 대조 지점을 놓고 보면 선택 기준이 더 분명해지기 때문이고, 세부 수치가 아니라 성격의 방향을 보기 위한 것입니다.

| 도구 | 인프라 소유권 | 스케줄링 내장 | Pipeline-as-Code | 플러그인/확장 생태계 | 운영 비용 | GitOps 적합성 |
|---|---|---|---|---|---|---|
| Jenkins | 온프레/셀프 호스트 중심 | 있음 (cron, `triggers`) | Declarative Pipeline | 매우 큼 | 중간~높음 (Controller 운영) | 낮음 |
| GitHub Actions | 호스티드 또는 셀프 호스트 러너 | 있음 (`on.schedule`) | 워크플로우 YAML | 큼 (Marketplace) | 낮음 (호스티드 기준) | 중간 |
| GitLab CI/CD | 호스티드 또는 셀프 호스트 Runner | 있음 (pipeline schedules) | `.gitlab-ci.yml` | 중간 | 낮음~중간 | 중간 |
| Argo CD | Kubernetes 클러스터 | 없음 (CD 전용) | Application 매니페스트 | 중간 | 중간 | 매우 높음 |
| Spinnaker | 멀티 클라우드 | 있음 (Pipeline triggers) | Pipeline as Code 지원 | 큼 | 높음 | 중간 |
| Tekton | Kubernetes 클러스터 | `TriggerTemplate` 등 조합 | CRD 기반 | 중간 | 중간 | 높음 |

이 표는 "Jenkins가 모든 축에서 뛰어나다"는 이야기를 하려는 것이 아닙니다. 축마다 강점과 약점이 뚜렷하며, 그중 어떤 축이 우리 프로젝트 요구와 맞닿아 있는지 판단하는 기준이 필요하다는 쪽에 가깝습니다.

## 네 가지 축으로 추려 보기

도구 자체의 특성만 나열하면 결국 선택이 어려워집니다. 개인적으로는 다음 네 가지 축을 먼저 점검해 보는 편이 판단을 단순하게 만들어 준다고 느꼈습니다.

첫째는 인프라 소유권입니다. 애플리케이션이 돌아갈 런타임이 온프레미스 단일 노드인지, 매니지드 Kubernetes 클러스터인지, 여러 클라우드를 교차하는 환경인지에 따라 적합한 도구가 갈립니다. Argo CD나 Tekton은 기본 전제가 Kubernetes이고, Spinnaker는 멀티 클라우드 배포 전략에 초점이 맞춰져 있습니다. 반대로 로컬 혹은 단일 VM 위에 JAR을 그대로 띄우는 구조라면, 이 세 도구의 장점이 현실에 반영되기 어렵습니다.

둘째는 워크로드 수명주기입니다. 앞서 정리한 대로 상주형과 단명형은 필요한 절차가 완전히 다릅니다. cron 스케줄링이 도구에 내장되어 있으면 Batch 파이프라인 구성이 단순해지고, 내장되어 있지 않으면 별도 스케줄러(외부 cron, Kubernetes CronJob 등)를 함께 설계해야 합니다. 또한 상주형을 다룰 때는 PID 관리, graceful shutdown, 헬스 체크 루프를 얼마나 자연스럽게 파이프라인에 녹여 낼 수 있는가가 중요해집니다.

셋째는 조직 규모와 운영 여력입니다. Jenkins Controller 자체의 운영 부담, Argo CD·Spinnaker의 컴포넌트 복잡도, Tekton 클러스터의 유지 관리 모두 "누가 이 도구를 돌봐 줄 것인가"라는 질문을 던집니다. 소규모 팀이나 1인 사이드 프로젝트에서 Spinnaker의 풀 기능을 가동하는 것은 배보다 배꼽이 커질 가능성이 높고, 반대로 이미 다수의 Kubernetes 서비스를 운영하는 조직에서 Jenkins만 고집하는 것도 어색한 선택이 될 수 있습니다.

넷째는 기존 Observability 스택과의 통합성입니다. CI/CD 도구가 Prometheus 메트릭을 어떻게 노출하는지, OpenTelemetry Trace를 자동 생성할 수 있는지, 기존 Grafana 대시보드와 어떻게 연결되는지에 따라 "파이프라인을 관측 대상에 포함시키는 비용"이 달라집니다. 이 축은 4편에서 다룰 관측성 주제와 연결되므로 이 글에서는 가볍게 언급하는 선에서 그치겠습니다.

이 네 축을 모두 같은 무게로 둘 필요는 없습니다. 본 프로젝트에서는 첫 번째와 두 번째 축에 가중치를 크게 두었습니다. "로컬 단일 노드 + 일부 클라우드 VM"이라는 인프라 소유권, 그리고 "Boot 6개 모듈 + Batch 3개 Job"이라는 수명주기 혼재가 가장 크게 작용한 요인이었습니다.

## 본 프로젝트가 Jenkins로 정리된 흐름

선택의 결과 자체보다 어떤 질문을 어떤 순서로 했는지를 공유하는 편이 유용할 것 같습니다. 본 프로젝트의 환경은 macOS 로컬에서 개발하고, 필요 시 Ubuntu VM에 동일한 구성을 재현하는 구조입니다. Kubernetes 클러스터를 직접 운영하고 있지 않고, 컨테이너 이미지를 매번 빌드해 레지스트리에 올리는 플로우가 오히려 번거로운 단계가 됩니다. 이 지점에서 Argo CD, Tekton, Spinnaker 같은 Kubernetes 중심 도구는 자연스럽게 후보에서 멀어졌습니다. 언젠가 클러스터 기반으로 전환하는 상황이 오면 다시 판단해야 할 주제이지만, 지금 시점에서는 오버엔지니어링이라는 판단이 더 컸습니다.

남은 후보는 Jenkins, GitHub Actions, GitLab CI/CD였습니다. 본 프로젝트의 GitHub 레포지토리는 public이므로 GitHub Actions의 호스티드 러너를 활용하면 빌드 자체는 깔끔하게 돌릴 수 있습니다. 다만 배포 대상이 로컬 또는 VM이라는 점에서 두 가지가 걸렸습니다. 하나는 호스티드 러너에서 원격 호스트로 JAR을 전달하고 기동시키는 과정이 결국 SSH·registry 중 하나의 경로를 요구한다는 점이고, 다른 하나는 Batch Job의 cron 실행을 GitHub Actions `on.schedule`로 구성했을 때 Job 실패 로그와 애플리케이션 로그를 한 곳에 모으는 동선이 번거롭다는 점이었습니다. GitLab CI/CD도 구조적으로 유사한 절충이 필요했습니다.

Jenkins를 고르게 된 결정적인 이유는 세 가지였습니다. 첫 번째는 cron 스케줄링과 파이프라인 실행이 같은 도구 안에서 해결된다는 점입니다. 참고 프로젝트에서 Batch 스케줄러를 직접 설계하면서 가장 편했던 지점이 여기였습니다. Jenkins 아이템에 `BATCH_JOB_NAME` 문자열 파라미터를 바인딩하고 UI의 "Build periodically"에 cron을 입력해 두면, Jenkinsfile 쪽은 `java -jar ... --job.name=${jobName}`을 호출하고 exit code를 확인하는 역할만 맡습니다. "스케줄은 UI에, 실행 로직은 Jenkinsfile에" 식의 역할 분담이 자연스럽게 만들어집니다. Jenkins Pipeline 공식 문서의 cron 문법 섹션에 기술된 `H` 심볼이 Declarative Pipeline의 `triggers` 블록과 UI "Build periodically" 양쪽에서 동일하게 쓰인다는 점도 학습 비용을 낮춰 주었습니다.

두 번째는 상주형 Boot 배포에서 필요한 절차를 Groovy로 세밀하게 제어할 수 있었다는 점입니다. 예를 들어 `JENKINS_NODE_COOKIE=dontKillMe`를 붙여 `nohup`으로 Boot 프로세스를 띄우는 패턴을 직접 적용한 이유는, Jenkins가 빌드 종료 시점에 자식 프로세스를 함께 거둬가는 기본 동작을 우회하기 위함이었습니다. 이 환경변수는 Jenkins Controller가 자식 프로세스를 추적하는 데 쓰는 마커이기 때문에, 값이 비어 있지만 않으면 해당 프로세스는 "Jenkins 빌드에 속한 프로세스"에서 제외됩니다. Boot 서버가 Jenkins 빌드 종료와 함께 죽어 버리는 사고를 피하려는 실무 패턴인데, 이런 식의 세부 제어를 Pipeline 안에서 손쉽게 엮을 수 있다는 점이 컸습니다.

세 번째는 관측성 확장 여지였습니다. 이 프로젝트는 Jenkins Prometheus Plugin으로 빌드 메트릭을, OpenTelemetry Plugin으로 Stage 단위 Span을, 단명 Batch 프로세스는 Prometheus Pushgateway로 수집하는 구조를 두기로 설계했습니다. Prometheus 공식 문서의 Pushing metrics 가이드(`https://prometheus.io/docs/practices/pushing/`)는 Pushgateway가 "서비스 레벨 배치성 작업"에 한해 권고된다는 점을 명시하면서, 단명 프로세스가 pull scrape 주기 안에 끝나 버리는 상황을 전형적인 사례로 제시합니다. Spring Batch의 Monitoring and Metrics 문서 역시 Micrometer Registry를 통한 메트릭 노출을 다루지만, 단명 프로세스에서 어떻게 수집 창구를 확보할지는 런타임 측 설계로 남겨 둡니다. Jenkins Prometheus Plugin과 OpenTelemetry Plugin은 Freestyle보다 Pipeline에서 얻는 이득이 큰데, 특히 Stage Span은 Declarative Pipeline 구조 덕분에 자동 생성됩니다. 이 확장 여지가 참고 프로젝트의 규모에 비해 충분히 매력적이었습니다.

그 결과 Freestyle 대 Pipeline을 놓고 Pipeline 쪽을 고른 이유도 단순히 "Groovy가 익숙해서"가 아니라, 위에서 정리한 네 축 중 두 축(수명주기, Observability 통합성)에서 Pipeline이 얻는 이득이 뚜렷했기 때문이었습니다. 물론 Freestyle도 충분히 합리적인 선택지가 될 수 있습니다. 선택에는 언제나 대안이 있고, 그 대안이 틀렸다는 식의 태도는 피하려고 했습니다.

## 마치며

도구를 고르는 일은 사실 "이 도구가 최고"라는 결론에 도달하는 과정이 아니라, "우리 프로젝트의 가정"을 글로 써 보는 과정에 가까웠던 것 같습니다. 로컬 단일 노드, Boot와 Batch의 혼재, 기존 Observability 스택의 방향성 같은 조건이 하나씩 분명해질 때마다 남는 후보가 줄어들었고, 그 과정에서 "왜 처음부터 Jenkins를 고려했는가"라는 질문에 스스로 답할 재료가 쌓였습니다.

조금 늦게 깨달은 부분은, 이 조건들은 고정된 값이 아니라는 점이었습니다. 팀 크기가 커지거나 Kubernetes 기반으로 이관하는 순간이 오면 같은 프로젝트에서도 전혀 다른 도구가 어울리는 조합이 될 수 있습니다. 그래서 최근에는 결론을 문서에 적을 때 "이 시점의 조건에서는"이라는 단서를 붙이는 습관이 생겼습니다. 같은 자바 개발자로서 이런 기록이 몇 년 뒤의 제 자신에게 더 솔직한 동료가 되어 줄 것이라는 기대가 있습니다.

다음 편에서는 이 선택 이후 실제로 Pipeline을 "운영에서 버티게" 만들기 위해 참고 프로젝트가 적용한 다섯 가지 기법(파라미터 단일화, Credentials 이중 보관, 심볼릭 링크 기반 산출물 핀, PID + 포트 폴백, graceful→SIGKILL 에스컬레이션)을 순서대로 공개해 보려 합니다. 도구를 고른 뒤가 훨씬 긴 여정이라는 이야기를, 설계 결정의 근거와 함께 풀어 볼 생각입니다.

## 참고

이 글에서 "참고 프로젝트"로 든 CI/CD 설계와 Jenkinsfile은 아래 레포지토리에서 직접 확인할 수 있습니다.

- tech-n-ai 백엔드: https://github.com/thswlsqls/tech-n-ai-backend
- tech-n-ai 프론트엔드: https://github.com/thswlsqls/tech-n-ai-frontend

## 추천 제목

- Spring Boot와 Spring Batch CI/CD, Jenkins를 고르게 된 네 가지 축
- 자바 개발자를 위한 CI/CD 도구 비교 — Jenkins·GitHub Actions·Argo CD·Tekton
- 상주형 Boot와 단명형 Batch, 같은 CI/CD로 풀 수 없는 이유
- Spring Boot Spring Batch 배포 자동화 — 여섯 가지 선택지 정리
- Jenkins를 선택하는 기준 — Boot와 Batch의 수명주기 관점에서
