# 추천 제목 후보

- **CI/CD는 왜 태어났나 — SDLC 변화가 만든 Jenkins의 등장기**
  Jenkins의 역사에 관심은 있지만 SDLC 맥락이 옅은 주니어~미들 개발자에게, 맥락과 도구를 한번에 잡아주는 각도입니다.

- **Waterfall에서 DevOps까지, CI/CD가 풀어온 문제들**
  도구 선택에 앞서 문제 정의부터 다시 이해하고 싶은 독자에게 어울리는 각도입니다.

- **Hudson에서 Jenkins로 — Pipeline-as-Code는 어떻게 자리를 잡았나**
  오픈소스 거버넌스와 Pipeline-as-Code의 기원이 궁금한 독자에게 어울리는 각도입니다.

- **SDLC 관점으로 읽는 Jenkins 등장기 — CI/CD의 뿌리 되짚기**
  역사·맥락형 기술 회고글을 선호하는 독자에게 자연스러운 각도입니다.

## 선택 제목

본문에서는 **CI/CD는 왜 태어났나 — SDLC 변화가 만든 Jenkins의 등장기** 를 사용합니다.

---

## 왜 지금 다시 CI/CD의 뿌리를 돌아보는가

요즘 새 프로젝트를 세팅할 때면 CI/CD는 "당연히 있어야 하는 것"처럼 다뤄지는 듯합니다. GitHub Actions에 워크플로 하나 올리고, 메인 브랜치에 머지되면 빌드가 돌고, 태그가 붙으면 배포가 실행되는 식의 구성이 기본값처럼 느껴지는 경우가 많습니다. 저도 처음에는 그 기본값을 그대로 받아 쓰기만 했습니다. 그러다 Spring Boot 서비스 여러 개와 Spring Batch 하나를 함께 굴리는 프로젝트에서 Jenkins 위에 CI/CD를 직접 설계하게 되면서, "왜 굳이 Jenkins인지", "CI/CD라는 말은 어디에서 출발했는지" 같은 기본적인 질문을 스스로 되물어볼 기회가 생겼습니다.

이 글은 그 질문에 대한 제 나름의 정리입니다. 기술적으로 새로운 팁을 소개하는 글이기보다는, CI/CD의 등장 맥락을 SDLC 관점에서 다시 읽어보고 Jenkins가 그 흐름에서 어떤 자리를 차지해 왔는지를 공식 출처 기반으로 짚어보는 글에 가깝습니다. 같은 길을 걷는 동료 개발자 입장에서 가볍게 읽어주시면 감사하겠습니다.

## 통합 지옥과 Waterfall 시대의 그림자

SDLC를 순차적 Waterfall 모델로 돌리던 시기에는 개발, 통합, 테스트, 배포가 긴 시간 간격을 두고 따로 이루어지는 경우가 많았다고 알려져 있습니다. 각 팀이 자기 모듈을 길게 개발한 뒤, 마지막에 한 번에 합쳐보는 이른바 "빅뱅 통합" 구조에 가깝습니다. 책상 앞에서 잘 돌아가던 코드가 막상 통합 단계에서 엮이면, 인터페이스 불일치, 의존 라이브러리 충돌, 환경 차이가 한꺼번에 쏟아져 나왔다는 이야기를 선배 개발자들의 회고에서 어렵지 않게 들을 수 있습니다.

Martin Fowler는 2000년에 처음 공개하고 이후 2006년에 보강한 글 "Continuous Integration"에서 이 문제를 "integration hell"이라는 표현으로 정리했습니다. 오래 떨어져 작업한 코드를 뒤늦게 합치려 하면 통합 자체가 하나의 거대한 프로젝트가 되어버린다는 지적입니다. 같은 글에서 그는 CI를 "팀 구성원 각자가 자신의 작업을 자주(최소 하루 한 번 이상) 공유 메인라인에 통합하고, 자동 빌드로 이를 검증하는 소프트웨어 개발 실천법"으로 정의하고 있습니다(https://martinfowler.com/articles/continuousIntegration.html).

이 정의를 지금 기준으로 보면 특별할 것이 없어 보일 수도 있습니다. 그러나 "매일 통합"이라는 조건은 당시 관행과 비교하면 꽤 강한 요구였다고 이해하는 편이 자연스럽습니다. 통합을 자주 하려면 빌드와 테스트가 자동으로, 짧은 시간 안에 돌아야 하고, 통합이 깨졌을 때 이를 빠르게 알리는 장치도 필요합니다. 즉 CI라는 개념이 자리를 잡으려면 자동 빌드 시스템이라는 기술적 토대가 함께 있어야 했던 셈입니다. Jenkins의 전신이 등장하게 된 배경도 이 지점과 맞닿아 있다고 읽을 수 있습니다.

## Agile과 XP가 요구한 "매일 통합하라"

Fowler의 CI 정의보다 조금 앞서, 1999년 Kent Beck이 eXtreme Programming(XP)을 소개하면서 "Continuous Integration"은 XP의 핵심 프랙티스 중 하나로 자리를 잡았습니다. XP 공식 사이트의 "Integrate Often" 문서는 "개발자는 자신의 변경 사항을 가능한 한 자주 공유 저장소에 통합해야 하며, 통합 간격은 몇 시간 단위를 넘지 않는 것이 바람직하다"는 취지의 설명을 제시하고 있습니다(http://www.extremeprogramming.org/rules/integrateoften.html).

XP 이후 애자일 선언(2001)을 거치며 "짧은 주기로 자주 릴리스한다"는 감각은 업계 전반에 퍼지기 시작했습니다. 짧은 주기는 곧 통합이 잦아진다는 뜻이고, 통합이 잦아지면 빌드와 테스트를 사람이 손으로 돌리기 어려워집니다. 자동화가 선택이 아니라 전제가 되는 것입니다. CruiseControl과 같은 초창기 CI 서버들이 이 시기에 사용되기 시작했고, 이후 Hudson을 비롯한 다음 세대 도구들이 그 자리를 이어받게 됩니다.

정리하자면 CI는 누군가 한 사람의 발상이라기보다는, Waterfall이 키운 통합 부담을 줄이려는 여러 흐름이 모여 만든 실천에 가깝다고 이해하는 편이 자연스럽습니다. Fowler는 이를 개념으로 정리했고, XP는 프랙티스 묶음에 편입시켰으며, 초기 CI 서버들은 이 실천을 자동화로 뒷받침했습니다. 이 세 축이 맞물리지 않았다면 "자주 통합"이라는 관행은 오래 유지되기 어려웠을 것이라는 인상을 받게 됩니다.

## Hudson에서 Jenkins로 — 오픈소스 거버넌스가 갈라진 순간

Jenkins의 직접적인 뿌리는 Kohsuke Kawaguchi가 Sun Microsystems 재직 중에 시작한 Hudson 프로젝트입니다. Jenkins 공식 프로젝트 히스토리 문서에 따르면 Hudson은 2000년대 중반 사내 프로젝트로 출발해 커뮤니티 기반 오픈소스로 성장했고, 자바 생태계에서 널리 쓰이는 CI 서버 중 하나로 자리를 잡았습니다(https://www.jenkins.io/project/history/).

변곡점은 Oracle이 Sun을 인수한 이후에 찾아왔습니다. 같은 공식 히스토리 문서는 Hudson이라는 이름의 상표와 프로젝트 인프라 관리 권한을 둘러싸고 Oracle과 커뮤니티 사이에 입장 차이가 생겼고, 커뮤니티가 결국 같은 코드베이스를 기반으로 프로젝트 이름을 바꿔 독립하기로 결정했다고 기록하고 있습니다. 그렇게 2011년 초 Jenkins라는 이름이 공식화되었고, 이후 대부분의 기여자와 플러그인 생태계가 Jenkins 쪽으로 따라 이동했다는 서술이 이어집니다.

이 사건은 단순한 이름 변경을 넘어, 오픈소스 거버넌스 구조가 도구의 장기 궤적을 어떻게 바꿀 수 있는지를 보여주는 사례로 자주 언급됩니다. Jenkins는 이후 커뮤니티 중심 거버넌스 아래에서 성장해 왔고, 2019년 3월 Linux Foundation이 출범시킨 Continuous Delivery Foundation의 창립 프로젝트 중 하나로 합류해 지금까지 운영되고 있습니다(https://cd.foundation/announcement/2019/03/12/cd-foundation-launches/). 특정 벤더의 로드맵에 종속되지 않은 채 플러그인 중심의 확장 구조를 키워 온 셈입니다. 도구를 고를 때 "누가 만들고 누가 유지하는가"라는 질문이 생각보다 오래 영향을 준다는 점을, 이 히스토리가 조용히 보여주는 셈입니다. Hudson과 Jenkins의 분기는 "좋은 코드가 있으면 된다"는 기준만으로 도구를 판단하기에는 부족할 수 있다는 힌트를 남겨둔 사건처럼 느껴지기도 합니다.

## DevOps와 Continuous Delivery — 배포가 일상이 되는 흐름

CI가 자리를 잡자 다음 질문은 자연스럽게 "그래서 이 빌드 결과물을 어떻게 안전하게 운영 환경까지 가져갈 것인가"로 옮겨갔습니다. 2010년 Jez Humble과 David Farley가 함께 쓴 책 *Continuous Delivery*는 이 질문에 정리된 답을 내놓은 대표적인 저작 중 하나입니다. Humble의 공식 사이트는 Continuous Delivery를 "모든 종류의 변경(기능, 설정 변경, 버그 수정, 실험)을 운영 환경이나 사용자에게 안전하고 빠르게, 지속 가능한 방식으로 전달할 수 있는 능력"으로 소개하고 있습니다(https://continuousdelivery.com/).

같은 사이트에서는 CD를 가능하게 하는 원칙들을 나열하는데, 자주 인용되는 것이 "프로세스는 반복 가능하고 신뢰할 수 있어야 한다", "거의 모든 작업을 자동화해야 한다", "고통스러운 일일수록 더 자주 해야 한다" 같은 문장입니다. 세 번째 원칙은 처음 읽으면 다소 역설적으로 보이지만, "배포가 아프다면 배포를 더 자주 해서 그 아픈 부분을 개선하라"는 취지로 이해할 수 있습니다. 이 관점은 지금 읽어도 실무 감각과 잘 맞닿아 있는 지점이 있습니다.

Continuous Delivery와 Continuous Deployment를 같은 말처럼 쓰는 경우가 있는데, 저자들의 구분에 따르면 Delivery는 "언제든 배포 가능한 상태를 유지하는 것"이고 Deployment는 "그 상태에서 실제 운영 반영까지 자동으로 이어지는 것"입니다. 조직마다 어디까지 자동화할지는 서로 다르며, 둘을 같은 수준으로 강제할 필요는 없어 보입니다. 실제로 금융·의료처럼 변경 승인이 필수인 도메인에서는 Delivery까지만 자동화해도 충분히 큰 진전이 될 수 있습니다.

여기에 DevOps라는 흐름이 얹히면서 CI/CD는 단순히 개발팀의 도구가 아니라 개발과 운영 사이의 협업 방식 그 자체로 묘사되기 시작했습니다. Jenkins 역시 이 시기를 지나며 "단일 빌드 서버"라는 인식을 넘어, Pipeline 개념으로 빌드·테스트·배포를 하나의 흐름으로 묶는 도구로 진화해 왔습니다. Jenkins 공식 Pipeline 문서는 이 흐름을 "Continuous Delivery Pipeline을 Jenkins에 모델링하고 구현하기 위한 플러그인 묶음"으로 소개하고 있습니다(https://www.jenkins.io/doc/book/pipeline/).

## Jenkins가 표준이 된 기술적·생태계적 이유

Jenkins가 오랫동안 많이 쓰여온 배경에는 여러 조건이 복합적으로 얽혀 있습니다. 공식 문서에서 반복적으로 강조되는 지점을 중심으로 정리해 보면, 크게 세 가지 축이 눈에 들어옵니다.

첫째는 플러그인 중심의 확장 구조입니다. Jenkins는 핵심 기능을 최소한으로 유지하고 필요한 기능을 플러그인으로 붙이는 구조를 오래 유지해 왔습니다. 공식 플러그인 사이트(https://plugins.jenkins.io/)에는 상당히 많은 플러그인이 등록되어 있으며, SCM 연동부터 알림, 관측성, 클라우드 연동까지 폭넓은 범위가 커버됩니다. 생태계가 커지면 도구 자체의 한계보다 "어떻게 조합할 것인가"가 더 중요한 문제가 되고, 이 점이 Jenkins를 특정 워크플로에 갇히지 않게 해 왔다고 이해할 수 있습니다.

둘째는 온프레미스 친화성입니다. Jenkins는 서버에 직접 설치해 운영하는 전통적인 구성을 계속 지원해 왔고, 이는 사내 네트워크 안에서만 접근 가능한 레포지토리나 내부망 리소스를 다뤄야 하는 조직에 여전히 유효한 선택지로 남아 있습니다. 팀이 쓰는 인프라가 매니지드 SaaS만으로 구성되어 있지 않다면 이 지점의 가치는 꽤 크게 느껴집니다. 최근에는 Jenkins도 컨테이너 기반 Agent와 Kubernetes Plugin을 지원하면서 구름 위 환경에도 무리 없이 올라갈 수 있게 되었지만, "기반부터 자기 인프라에 맞춰 쓰기 쉽다"는 특징은 변함없이 유지되고 있는 것으로 보입니다.

셋째는 Pipeline-as-Code입니다. Jenkins 2.0 이후 공식 Pipeline 문서(https://www.jenkins.io/doc/book/pipeline/)는 Jenkinsfile을 통한 Declarative Pipeline을 권장 방식으로 제시해 왔습니다. Pipeline을 코드로 기술해 리포지토리에 커밋하면, 파이프라인의 변경 이력이 Git에 그대로 남고, 코드 리뷰와 롤백이 익숙한 방식으로 가능해집니다. Freestyle 프로젝트처럼 GUI로만 구성된 빌드 설정과 비교했을 때 이 차이는 상당히 큽니다.

Spring Boot API 6종과 Spring Batch 1종이 섞인 자바 프로젝트에 Jenkins를 직접 붙이면서, Freestyle과 Pipeline 사이에서 한 번은 진지하게 고민할 수밖에 없었습니다. Freestyle은 Jenkins UI에서 클릭만으로 빠르게 만들 수 있지만 설정이 Jenkins 내부 XML로만 남기 때문에, 빌드 실패를 재현하려면 서버에 들어가 Console Output 전체를 되짚어야 하고, 환경을 이관할 때도 수작업이 크게 붙습니다. 반대로 Declarative Pipeline은 Jenkinsfile을 애플리케이션 코드와 같은 레포지토리에 나란히 커밋해 두는 구조라, 파이프라인 변경이 PR로 리뷰되고 과거 이력으로 남고 필요하면 브랜치 단위로 실험할 수 있습니다. 같은 조건에서 Pipeline을 선택한 이유는 결국 "빌드 설정도 코드처럼 관리된다"는 기대치 하나였고, 이 기대치가 자리를 잡으면 도구가 Jenkins든 다른 무엇이든 선택 기준이 한 단계 올라가는 것 같습니다.

## 정리하며 남은 생각

여기까지 살펴본 흐름을 거칠게 한 문장으로 요약해 본다면, CI/CD는 Jenkins가 만든 것이 아니라 SDLC의 피드백 루프를 점점 더 짧게 줄여온 오랜 요구가 만든 결과물에 가깝다고 말하고 싶습니다. Waterfall이 키운 통합 부담을 XP와 애자일이 "매일 통합"이라는 실천으로 밀어냈고, 그 실천을 자동화로 떠받치기 위해 Hudson과 Jenkins 같은 도구가 자라났고, 다시 그 위에 Continuous Delivery와 DevOps라는 더 넓은 운영 문화가 얹혀 왔다는 흐름입니다.

개인적으로 이 정리를 거치며 가장 크게 바뀐 감각은, 도구를 고를 때 "무엇이 최신인가"보다 "이 도구가 어떤 문제 정의 위에 서 있는가"를 먼저 물어보자는 것이었습니다. GitHub Actions, GitLab CI, Argo CD, Spinnaker, Tekton처럼 선택지가 쏟아지는 시대에 Jenkins를 그대로 쓰는 게 맞는지, 혹은 다른 도구와 함께 쓰는 것이 맞는지는 사람마다 답이 다를 수 있습니다. 다만 그 판단을 내리기 전에, 지금 해결하려는 문제가 CI에 가까운지, Delivery에 가까운지, 또는 DevOps 문화 정착에 가까운지를 먼저 구분해 두면 선택지 비교가 한결 깔끔해진다는 점은 꽤 분명해 보입니다.

다음 편에서는 이 문제 정의를 이어받아, 상주형인 Spring Boot 서비스와 단명형인 Spring Batch 잡을 두고 CI/CD 선택지들을 비교해 보려고 합니다. 각 도구의 공식 문서를 기반으로, 왜 어떤 상황에서는 Jenkins가 여전히 합리적인 선택이 되는지를 정리해 볼 생각입니다. 이번 글이 그 준비 과정으로 작게나마 도움이 되었다면 기쁠 것 같습니다.

## 참고

공식 외부 자료

- Jenkins Project — History: https://www.jenkins.io/project/history/
- Jenkins — Pipeline: https://www.jenkins.io/doc/book/pipeline/
- Jenkins — Plugins Index: https://plugins.jenkins.io/
- Martin Fowler — Continuous Integration: https://martinfowler.com/articles/continuousIntegration.html
- eXtreme Programming — Integrate Often: http://www.extremeprogramming.org/rules/integrateoften.html
- Jez Humble — Continuous Delivery: https://continuousdelivery.com/
- Continuous Delivery Foundation — Launch Announcement (2019-03-12): https://cd.foundation/announcement/2019/03/12/cd-foundation-launches/

이 글에서 배경으로 삼은 프로젝트의 CI/CD 설계와 Jenkinsfile은 아래 레포지토리에서 직접 확인할 수 있습니다.

- tech-n-ai 백엔드: https://github.com/thswlsqls/tech-n-ai-backend
- tech-n-ai 프론트엔드: https://github.com/thswlsqls/tech-n-ai-frontend
