# Jenkins Declarative Pipeline을 운영에서 버티게 만드는 다섯 가지 설계 결정

CI/CD 파이프라인은 한 번 만들면 오래 갑니다. 정확히는, 한 번 만든 그대로 오래 "가 보이기만 할" 때가 많습니다. 콘솔 로그 마지막 줄에 `BUILD SUCCESS`가 찍히고 빨간 아이콘 대신 파란 아이콘이 남으면, 그다음부터는 아무도 그 파이프라인을 다시 열어보지 않게 됩니다. 문제가 수면 위로 떠오르는 순간은 대개 배포 중 장애가 났을 때이고, 그제서야 `pkill -f` 한 줄이 의도치 않은 프로세스를 죽였거나, PID 파일이 남은 채 프로세스만 사라진 좀비 상태였거나, 헬스체크가 끝나기도 전에 Jenkins가 자식 프로세스를 함께 거둬간 것이 드러납니다.

이 글은 Spring Boot API 6종과 Spring Batch 1종을 로컬 단일 노드 Jenkins로 운영하는 참고 프로젝트에서, 기본값만 쓰다가 깨지기 쉬운 다섯 지점을 어떻게 설계로 묶어 두었는지 정리한 기록입니다. 파라미터화된 단일 Jenkinsfile, Credentials 이중 보관, 심볼릭 링크 기반 산출물 핀 전략, PID 파일과 포트 점유 폴백을 함께 쓰는 프로세스 제어, 그리고 graceful shutdown과 readiness 헬스체크 루프의 타이밍 맞춤까지 — 각 결정 뒤에 있는 선택지 비교와 공식 문서 근거를 함께 기록합니다. Jenkins의 진가는 "된다/안 된다"가 아니라 운영에서 버티는 구간에서 드러난다는 관찰이 글 전체의 전제입니다.

## 여러 모듈을 한 Jenkinsfile로 묶어 두기

참고 프로젝트의 API 모듈은 `api-gateway`, `api-emerging-tech`, `api-auth`, `api-chatbot`, `api-bookmark`, `api-agent` 여섯 개입니다. 공통점은 Gradle 멀티 모듈 구조를 따르고, Stage 흐름이 모두 `Prepare Workspace → Git Checkout → Build JAR → Archive & Link`로 동일하다는 점입니다. 차이점은 모듈명과 빌드 산출물 경로뿐입니다. 이 관찰을 그대로 설계로 옮기면 Jenkinsfile을 여섯 벌 두는 대신 하나로 묶을 수 있습니다.

직접 설계한 Jenkinsfile에서는 `MODULE_NAME`을 `choice` 파라미터로 받도록 했습니다. Jenkins 공식 Declarative Pipeline Syntax 문서는 `parameters` 디렉티브가 Pipeline Run 진입점에서 사용자가 값을 고르는 선언적 방법이라는 점과 `choice`가 고정된 선택지 집합에 적합하다는 점을 기술합니다. 이 타입을 고른 덕분에 모듈이 늘거나 줄 때 `choices` 목록만 수정하면 되고, 여섯 모듈을 한 곳에서 관리하는 경계가 자연스럽게 생깁니다.

반면 빌드 대상 브랜치는 Jenkinsfile 파라미터에서 제외했습니다. 이 파이프라인은 Jenkins Job 구성 화면의 `Pipeline` 섹션에서 `Definition: Pipeline script from SCM`을 선택해 Jenkinsfile을 Git에서 로드하는 방식을 쓰는데, Jenkins는 이 단계에서 이미 `SCM: Git`의 `Branches to build` 값으로 브랜치를 결정해 Jenkinsfile과 소스 트리를 함께 체크아웃합니다. 만약 같은 Jenkinsfile 안에 `BRANCH` 파라미터를 다시 두면, 파라미터 값은 Jenkinsfile이 로드된 이후에야 평가되므로 이미 결정된 SCM 브랜치 선택을 덮지 못하고 의도와 다른 브랜치가 빌드되는 충돌이 발생합니다. 그래서 브랜치는 Jenkins Job 구성 화면의 `Pipeline` > `Definition: Pipeline script from SCM` > `SCM: Git` > `Branches to build` 한 곳에서만 관리하고, Jenkinsfile 내부에서는 `checkout scm`으로 이 설정을 그대로 재사용하도록 설계했습니다. 브랜치 선택지는 Jenkins 관리자가 Job 구성 화면에서 수정하는 운영 동선을 유지하면서, Jenkinsfile에서는 중복되는 파라미터 정의를 완전히 제거한 형태입니다.

Gradle 모듈명(`api-auth`)과 실제 디렉토리 경로(`api/auth`)가 일치하지 않는다는 점은 설계상 불편이 아니라 오히려 명시적 매핑을 요구하는 지점입니다. 이 매핑을 Groovy Map으로 "모듈명 → JAR 소스 경로"에 고정해 두도록 설계한 이유는, Archive & Link 스테이지에서 조건문 분기 없이 Map 조회만으로 JAR 위치를 결정하고 싶어서였습니다. 모듈을 추가할 때 Map 한 줄만 늘리면 다른 코드를 건드릴 필요가 없습니다.

이 구조를 배포 파이프라인에도 그대로 가져가면 한 가지 고민이 생깁니다. 모듈마다 필요한 시크릿이 다르기 때문입니다. 참고 프로젝트의 배포 Jenkinsfile에 `module_credentials`라는 모듈별 `withCredentials` 바인딩 맵을 둔 이유는, Deploy 스테이지에서 모듈명 키로 바인딩 배열을 꺼내 그대로 주입하고 싶었기 때문입니다. 바인딩 배열을 값으로 갖는 Map이라는 표현은 다소 낯설지만 Groovy에서는 자연스럽게 허용되는 패턴이고, Jenkins 공식 "Using Credentials" 문서의 `withCredentials` 블록이 정확히 이 바인딩 배열을 인자로 받습니다. 결과적으로 "하나의 Jenkinsfile, 여섯 모듈, 모듈마다 다른 시크릿 세트"를 조건 분기 없이 다룰 수 있게 됩니다.

## 동일한 PAT을 Credential 두 개로 나누는 이유

참고 프로젝트는 동일한 GitHub Personal Access Token을 Jenkins Credentials Store에 두 번 등록합니다. 하나는 `github-pat-tech-n-ai-backend`이고 다른 하나는 `github-token-batch-source`입니다. 값은 같은데 ID가 다릅니다. 한 번만 등록하는 편이 관리에 편하지 않냐는 질문이 당연히 떠오릅니다.

답은 Jenkins의 Credentials 타입이 용도에 따라 요구 형태가 다르다는 데 있습니다. Jenkins "Using Credentials" 공식 문서는 `Username with password`, `SSH Username with private key`, `Secret file`, `Secret text`, `Certificate` 등의 타입을 구분하며, 각 타입이 Pipeline에서 쓰이는 방식도 다릅니다. Git 체크아웃 경로는 `credentialsId`에 `Username with password` 타입을 그대로 받아 HTTP Basic Auth에 사용합니다. 참고 프로젝트는 Jenkinsfile 내부의 `git` step으로 URL/credentials/branch를 다시 선언하는 대신, Jenkins Job 구성 화면의 `Pipeline` > `Definition: Pipeline script from SCM` > `SCM: Git` > `Credentials` 드롭다운에서 이 `Username with password` Credential(`github-pat-tech-n-ai-backend`)을 한 번만 선택해 두고, Jenkinsfile 안에서는 `checkout scm`이 이 설정을 그대로 재사용하도록 했습니다. 반면 애플리케이션 런타임에 환경변수로 주입해야 하는 토큰은 `withCredentials`의 `string()` 바인딩을 타야 하고, 이 바인딩은 `Secret text` 타입을 요구합니다.

참고 프로젝트에서 두 개의 Credential을 나눠 둔 이유도 여기에 있습니다. Git 체크아웃용은 `Username with password` 타입으로 Username에 GitHub 사용자명을, Password에 PAT 값을 저장해 Jenkins Job 구성 화면의 `Pipeline script from SCM`에서 참조하고, 애플리케이션 런타임용은 동일한 PAT를 `Secret text` 타입으로 한 번 더 등록해 `withCredentials`의 `string()` 바인딩으로 주입하도록 설계했습니다. 타입이 다르기 때문에 "같은 값"이라는 이유로 하나를 공유할 수 없는 구조입니다.

이 이중 보관은 Jenkins Credentials Store 안에서만의 이야기가 아닙니다. PAT 자체를 macOS Keychain에 한 번 더 백업해 두는 결정도 같은 맥락에 있습니다. Jenkins 재설치나 마이그레이션 과정에서 Credentials Store 자체가 사라질 수 있고, PAT은 GitHub UI에서 발급 직후 한 번만 확인 가능한 값이기 때문입니다. `security add-generic-password` 명령으로 저장할 때 `-T ""` 옵션을 지정하는 이유는 의외로 자주 간과됩니다. Apple의 `security` CLI man page에 따르면 `-T`는 이 Keychain 항목에 무인증으로 접근할 수 있는 애플리케이션 목록을 지정하는 옵션입니다. 생략하면 `security` 자신이 허용 목록에 들어가 이후 조회 시 인증이 없어도 값이 흘러나올 수 있습니다. 빈 문자열을 넘기면 허용 애플리케이션을 두지 않는다는 뜻이 되어, 모든 접근이 로그인 비밀번호 또는 Touch ID 인증을 거치게 됩니다. "편의를 위해 지정하지 않는" 기본값이 오히려 보안 구멍이 되는 전형적인 예입니다.

## 심볼릭 링크를 산출물 핀으로 쓰는 전략

CI 단계에서 만들어지는 JAR은 빌드마다 이름이 달라지는 편이 안전합니다. 참고 프로젝트에서 `${module}-${timestamp}.jar` 형태로 `${JENKINS_HOME}/builds/` 아래에 쌓아 두도록 설계한 이유도 같은 맥락입니다. 동시에, 배포 파이프라인이 참조할 고정 경로도 필요합니다. 이 두 요구를 잇는 고전적인 도구가 심볼릭 링크입니다.

Archive & Link 스테이지에서는 실제 산출물을 타임스탬프 경로로 복사한 뒤, `ln -sf`로 `${module}.jar`라는 고정 이름의 심볼릭 링크를 최신 빌드에 다시 걸어 둡니다. `ln -sf`를 고른 이유는 기존 링크가 있어도 덮어써 주기 때문에 이전 실행의 링크를 삭제하는 별도 단계가 필요 없기 때문입니다. 배포 파이프라인은 이 고정 경로만 바라보면 되므로, CI와 CD를 분리하면서도 둘 사이의 계약이 "심볼릭 링크 이름" 하나로 줄어듭니다.

롤백 시나리오에서 이 구조가 주는 이점은 분명합니다. 이전 빌드의 타임스탬프 JAR이 `${JENKINS_HOME}/builds/` 아래에 여전히 남아 있으므로, `ln -sf`로 링크만 과거 JAR로 다시 걸고 배포 파이프라인을 재실행하면 그만입니다. Git revert와 재빌드를 강제하지 않아도 이전 바이너리로 복귀할 수 있습니다. 다만 로컬 단일 노드 환경에서는 빌드 이력을 무한히 쌓을 수 없어, `find ... -name "{module}-*.jar" -mtime +N` 패턴의 정리 스크립트를 함께 두는 편이 안전했습니다. Batch CI/CD 쪽 Archive & Link도 같은 형태를 따르도록 정렬해 둔 이유는, batch와 API의 배포 규약을 "심볼릭 링크 이름"이라는 단일 인터페이스로 맞추고 싶어서였습니다.

## PID 파일, 포트 점유 폴백, JENKINS_NODE_COOKIE를 함께 쓰는 이유

상주 프로세스 종료에 흔히 쓰이는 방법은 `pkill -f`입니다. 그러나 참고 프로젝트의 API 모듈은 `api-gateway.jar`, `api-auth.jar`처럼 공통 접두사를 갖는 JAR 이름을 쓰고, 실행 명령어에는 `java -jar ...` 형태가 공통으로 포함됩니다. `pkill -f api-`만 해도 다른 모듈까지 함께 종료할 위험이 생깁니다. 배포 스크립트가 의도한 프로세스보다 많은 프로세스를 끄는 순간, CI/CD가 만드는 피해가 장애보다 커집니다.

대안은 PID 파일입니다. 참고 프로젝트의 Deploy 스테이지는 프로세스를 띄우면서 `$!`로 받은 PID를 파일에 남기고, 다음 실행 때 Stop Running Process 스테이지가 같은 파일을 읽어 `kill -0 ${pid}`로 프로세스 생존 여부를 확인한 뒤 SIGTERM을 보내도록 설계돼 있습니다. `kill -0`은 시그널을 보내지 않고 대상 프로세스 존재 여부와 권한만 확인하는 POSIX 관례로, `kill(2)` 시스템 콜 명세에서 "If sig is 0, then no signal is sent, but existence and permission checks are still performed"로 기술됩니다. PID 파일만 있고 프로세스가 없는 "stale" 상태는 `kill -0`이 0이 아닌 코드를 반환하면서 감지되며, 이 경우 파일만 정리하고 다음 단계로 넘어가도록 묶어 두었습니다.

PID 파일이 만능은 아닙니다. 장비 재부팅이나 파이프라인 외부에서의 수동 조작으로 PID 파일 자체가 사라질 수 있습니다. 이럴 때 포트를 여전히 점유하고 있는 프로세스가 남아 있다면 새 배포가 `Port already in use`로 실패합니다. 같은 Stop 스테이지에서 PID 기반 정리 이후에도 `lsof -ti:${port}`로 포트 점유 프로세스를 한 번 더 확인하고 남아 있으면 SIGKILL로 정리한 뒤 10초 동안 포트 해제를 폴링하도록 설계한 이유가 여기에 있습니다. Apple의 `lsof(8)` man page는 `-t`가 PID만 출력하고 `-i:${port}`가 지정 포트 바인딩을 조회함을 기술하고 있어, 이 조합이 "포트를 잡고 있는 프로세스의 PID"를 얻는 가장 가벼운 방법이 됩니다. PID 파일은 정상 경로, 포트 폴백은 비정상 경로라고 생각하면 두 장치의 역할이 겹치지 않고 포개집니다.

여기에 한 겹을 더 얹는 장치가 `JENKINS_NODE_COOKIE=dontKillMe`입니다. Deploy 명령을 `JENKINS_NODE_COOKIE=dontKillMe nohup java ... &` 형태로 기동하도록 직접 설계한 이유는, Jenkins가 빌드 종료 시점에 자식 프로세스를 함께 정리하는 ProcessTreeKiller 메커니즘 때문이었습니다. 이 동작은 공식 Jenkins 위키의 `ProcessTreeKiller` 페이지에서 설명되며, 환경변수 `BUILD_ID` 또는 `JENKINS_NODE_COOKIE` 값을 빌드 기본값과 다르게 바꾸면 해당 자식 프로세스는 빌드 종료 시 자동 종료 대상에서 제외됩니다. API 서버처럼 빌드가 끝난 뒤에도 살아 있어야 하는 프로세스에는 반드시 필요한 설정입니다. PID 파일이 "누구를 죽일지", 포트 폴백이 "놓친 좀비를 어떻게 찾을지"를 담당한다면, `JENKINS_NODE_COOKIE`는 "Jenkins가 내 자식 프로세스를 먼저 죽여 버리지 않도록" 막는 세 번째 장치인 셈입니다.

## SIGTERM, 30초, 그리고 readiness 헬스체크

참고 프로젝트의 Stop Running Process 스테이지는 PID에 `kill`(기본 SIGTERM)을 보낸 뒤 최대 30회, 1초 간격으로 프로세스 종료를 확인하고, 30초가 지나도 살아 있으면 `kill -9`로 SIGKILL을 보내도록 설계돼 있습니다. 이 타이밍을 Spring Boot의 graceful shutdown 기본값과 정확히 맞춘 이유를 조금 풀어 두겠습니다.

Spring Boot 레퍼런스 문서의 "Graceful Shutdown" 페이지는 Spring Boot 4.x에서 graceful shutdown이 Jetty, Reactor Netty, Tomcat 임베디드 웹 서버 모두에서 **기본 활성화** 상태라는 점을 명시하고 있습니다(비활성화하려면 `server.shutdown=immediate`). 활성화 상태에서는 SIGTERM 수신 시 임베디드 웹 서버가 새로운 요청 수락을 중단하고 진행 중인 요청이 끝날 때까지 기다립니다. 대기 상한은 `spring.lifecycle.timeout-per-shutdown-phase` 프로퍼티가 결정하며, Spring Framework의 `DefaultLifecycleProcessor` 기본값을 따라 30초로 설정되어 있습니다. 배포 스크립트의 30초 폴링은 "애플리케이션이 스스로 떠날 수 있는 최대 시간"과 같은 지점에서 종료되고, 그래도 살아 있으면 SIGKILL로 끊는 것이 가장 단순한 상한 규칙이 됩니다. 타이밍을 10초로 줄이면 graceful shutdown 중인 정상 프로세스가 SIGKILL로 잘려 진행 중 요청이 중단될 수 있고, 60초로 늘리면 어딘가에서 막혀 있는 비정상 프로세스를 그만큼 오래 방치하게 됩니다. 기본값에 맞추는 선택이 가장 덜 놀라운 기본값입니다.

새 프로세스를 띄운 뒤에는 `http://localhost:${port}/actuator/health/readiness`를 5초 간격으로 30회까지 폴링하도록 묶어 두었습니다. 일반 `/actuator/health` 대신 `/actuator/health/readiness`를 고른 이유는 Spring Boot Actuator 문서의 "Application Availability" 섹션이 구분하는 두 신호의 의미에 따른 것입니다. Liveness는 "프로세스가 살아 있는가"에, Readiness는 "트래픽을 받을 준비가 되었는가"에 답합니다. 배포 파이프라인이 묻고 싶은 것은 후자입니다. Liveness가 OK여도 Kafka Consumer 초기화나 langchain4j 벡터 저장소 연결이 끝나지 않았다면 실제 요청은 실패할 수 있기 때문입니다.

Readiness 폴링이 30회 × 5초 = 150초 안에도 성공하지 못하면, 파이프라인은 배포 실패로 판정하고 방금 띄운 PID에 `kill`을 보낸 뒤 PID 파일까지 정리합니다. 실패한 프로세스가 포트를 붙들고 좀비로 남는 상황을 방지하기 위함입니다. 실패 시 정리 로직이 있다는 사실 자체가, 이 파이프라인이 "성공 경로"만이 아니라 "실패 경로"에서도 일관된 상태를 남기도록 설계되었다는 신호입니다. 이 부분을 빠뜨리면 다음 배포 시도 때 포트 충돌로 또 실패하고, 그 실패의 원인이 직전 실패의 잔해임을 추적하기가 쉽지 않아집니다.

## cron의 H 심볼, 그리고 두 군데에서 설정하지 않기

참고 프로젝트의 Batch 스케줄링 파이프라인에는 `triggers { cron('H */4 * * *') }` 형태의 cron 트리거를 넣어 두었습니다. 여기서 `H`는 표준 cron과는 다른 Jenkins 고유 확장으로, Declarative Pipeline Syntax의 Cron Syntax 섹션이 해시 기반 분산 심볼로 설명합니다. 각 Job 이름으로부터 해시값을 계산해 허용 구간 내에서 하나의 값을 선택하므로, 같은 `H */4 * * *` 표현식을 쓰는 여러 Job이 있어도 매 4시간마다 "정각 한 번에 몰려 실행"되지 않고 서로 다른 분에 분산됩니다.

`H */4 * * *`을 그대로 해석하면 "분 자리는 해시로 결정, 시 자리는 4시간 간격 스텝"이 됩니다. Job이 `emerging-tech.scraper.job`, `emerging-tech.rss.job`, `emerging-tech.github.job`처럼 셋 이상이 되면 동일 정각 동시 실행은 Jenkins 대시보드 체감 품질과 호스트 리소스 피크에 영향을 줍니다. `H`가 해 주는 자동 분산은 로컬 단일 노드 환경처럼 리소스에 여유가 없는 곳에서 특히 체감이 큽니다.

cron 스케줄을 어디에 둘지도 정해 두어야 합니다. Jenkinsfile의 `triggers` 블록과 Jenkins UI의 "Build periodically"가 둘 다 cron 트리거를 걸 수 있고, 두 곳 모두에 값이 들어가 있으면 Jenkins가 둘 다 실행해 버립니다. 이 점을 의식해 참고 프로젝트에서는 로컬 환경에서 스케줄을 빈번히 조정해야 한다는 조건 때문에 Jenkins UI를 단일 출처로 삼고, Jenkinsfile의 `triggers` 블록은 실제 운영 시 주석 처리하는 규칙을 두었습니다. 스케줄을 Git에 남기고 싶다는 이유로 두 곳 모두에 값을 넣어 두면, 테스트 환경에서는 평온해도 운영에서는 조용히 "두 번 실행"이 누적됩니다. 두 가지 장점을 동시에 취하려다 가장 피곤한 장애를 만나는 경우가 이 영역에서 자주 보입니다.

## 마무리하며 — 기본값과 실전 사이

이 글에서 정리한 다섯 가지 결정에는 공통된 관점이 하나 있습니다. Jenkins가 제공하는 기본값은 대부분 "된다"는 상태까지를 책임지고, 그 이후의 안정성은 사용자가 선택한 방어 장치의 조합으로 만들어진다는 점입니다. 파라미터화된 단일 Jenkinsfile은 모듈 증감을 코드 수정으로 다루는 경계를 세우고, Credentials 이중 보관은 Jenkins Store 밖의 실패 모드를 Keychain으로 대비합니다. 심볼릭 링크 핀 전략은 CI와 CD 사이의 계약을 한 경로로 줄이고, PID와 포트와 `JENKINS_NODE_COOKIE`의 삼중 구조는 상주 프로세스 제어에서 서로 다른 실패 경로를 덮습니다. 마지막으로 graceful shutdown과 readiness 헬스체크의 타이밍 맞춤은, 서로 다른 도구(Jenkins와 Spring Boot)의 기본값이 충돌하지 않도록 정렬하는 작업이었습니다.

개인적으로는 이 작업을 하면서, 파이프라인을 처음 만들 때와 운영에서 다시 열어볼 때의 시야가 꽤 다르다는 점을 확인했습니다. 처음에는 스테이지를 그리는 데 집중하기 쉽고, 시간이 지나면 각 스테이지가 실패할 때 시스템이 어떤 상태로 남는가가 더 중요해집니다. Jenkins, Spring Boot, macOS Keychain 모두 공식 문서가 "그 이후"를 위한 훅을 이미 준비해 두고 있다는 사실은, 도구를 더 깊게 읽을수록 설계가 단순해진다는 오래된 경험칙을 다시 떠오르게 했습니다. 다음 편에서는 이 파이프라인이 "돌고 있는지"를 메트릭과 트레이스로 관측하는 장치, 즉 Prometheus Plugin과 OpenTelemetry Plugin, 그리고 단명 Batch 프로세스를 위한 Pushgateway 연계를 정리해 볼 예정입니다.

## SEO 제목 후보

- **"Jenkins Declarative Pipeline 실전 설계 — 파라미터화·Credentials·PID·Graceful Shutdown로 버티는 CI/CD"**
  - 키워드: Jenkins Declarative Pipeline, Graceful Shutdown, PID 파일, CI/CD 설계
  - 추천 이유: 글에서 다루는 다섯 기법의 핵심 키워드를 한 줄에 압축했고, "실전 설계"와 "버티는"이라는 표현으로 단순 튜토리얼과 차별화됩니다.

- **"Spring Boot 배포에서 Jenkins가 무너지지 않게 — PID 파일, JENKINS_NODE_COOKIE, Readiness Probe 설계 정리"**
  - 키워드: Spring Boot 배포, Jenkins PID, JENKINS_NODE_COOKIE, Readiness Probe
  - 추천 이유: "Spring Boot 배포"라는 검색 유입 키워드를 전면에 두고, 공식 문서에서 자주 쓰이는 `JENKINS_NODE_COOKIE`, Readiness Probe 같은 고유 식별어를 포함시켜 관련 이슈를 찾는 독자의 쿼리에 걸릴 확률을 높였습니다.

- **"Jenkinsfile 하나로 API 6종을 — 파라미터화·Credentials 이중 보관·심볼릭 링크 Pin 전략"**
  - 키워드: Jenkinsfile 파라미터화, Credentials 관리, 심볼릭 링크 배포, 멀티 모듈 CI/CD
  - 추천 이유: 멀티 모듈 CI/CD 구성을 고민하는 독자에게 구체적인 숫자("6종")와 기법명을 직접 드러내서, 유사 규모의 프로젝트에서 검색했을 때 적합도가 높은 제목입니다.

## 참고자료

### 외부 공식 출처
- Jenkins — Declarative Pipeline Syntax (https://www.jenkins.io/doc/book/pipeline/syntax/)
- Jenkins — Using Credentials (https://www.jenkins.io/doc/book/using/using-credentials/)
- Jenkins — Cron Syntax (https://www.jenkins.io/doc/book/pipeline/syntax/#cron-syntax)
- Spring Boot — Graceful Shutdown (https://docs.spring.io/spring-boot/reference/web/graceful-shutdown.html)
- Spring Boot — Actuator Health (https://docs.spring.io/spring-boot/reference/actuator/endpoints.html#actuator.endpoints.health)

### 참고 프로젝트

이 글의 다섯 가지 설계 결정을 그대로 반영한 Jenkinsfile과 배포 스크립트는 아래 레포지토리에서 확인할 수 있습니다.

- tech-n-ai 백엔드: https://github.com/thswlsqls/tech-n-ai-backend
- tech-n-ai 프론트엔드: https://github.com/thswlsqls/tech-n-ai-frontend
