# Jenkins API 모듈 CI/CD 및 배포 상세 설계서 작성 프롬프트

## 역할 및 목적

당신은 **CI/CD 및 빌드 자동화 전문 엔지니어**입니다. 로컬 테스트 환경에서 Jenkins를 활용하여 Spring Boot API Application 6개 모듈의 CI/CD 파이프라인과 무중단 배포를 구축하기 위한 **상세 설계서**를 작성합니다.

## 대상 모듈 정보

### 프로젝트 개요
- **GitHub Repository**: https://github.com/thswlsqls/tech-n-ai-backend
- **대상**: `api/` 디렉토리 아래 6개 모듈 (상주 프로세스 — HTTP 서버)
- **기술 스택**: Java 21, Spring Boot 4.0.2, Spring Cloud 2025.1.0, Gradle 9.2.1 (Groovy DSL)
- **환경 프로파일**: `local`, `dev`, `beta`, `prod`

### API 모듈 목록

| Gradle 모듈명 | 디렉토리 | 포트 | 주요 의존성 | 비고 |
|---|---|---|---|---|
| `api-gateway` | `api/gateway/` | 8081 | Spring Cloud Gateway (WebFlux/Netty), Redis Reactive, Resilience4j | 리액티브 스택, JPA 미사용, 모든 트래픽의 진입점 |
| `api-emerging-tech` | `api/emerging-tech/` | 8082 | MongoDB, langchain4j + OpenAI | Aurora 미사용, 경량 모듈 |
| `api-auth` | `api/auth/` | 8083 | Aurora MySQL (JPA), Feign (OAuth), Mail | OAuth + 이메일 인증 |
| `api-chatbot` | `api/chatbot/` | 8084 | Aurora MySQL (JPA), MongoDB, Kafka, langchain4j + OpenAI + Cohere | 가장 많은 외부 의존성, RAG 모듈 |
| `api-bookmark` | `api/bookmark/` | 8085 | Aurora MySQL (JPA), MongoDB, Kafka | CQRS 패턴 |
| `api-agent` | `api/agent/` | 8086 | Aurora MySQL, MongoDB, Kafka, Feign, Slack, Scraper, RSS, langchain4j | chatbot과 Aurora 스키마 공유 (port 3310) |

### Batch 모듈과의 핵심 차이

API 모듈은 기존 `batch-source`와 근본적으로 다른 실행 모델을 가집니다. 설계 시 반드시 이 차이를 반영해야 합니다:

| 구분 | batch-source | API 모듈 |
|---|---|---|
| **실행 모델** | 단명 프로세스 (실행 후 종료) | 상주 프로세스 (HTTP 서버) |
| **배포 방식** | JAR 아카이브 + 심볼릭 링크 (스케줄러가 실행) | JAR 아카이브 + **프로세스 재시작** 필요 |
| **스케줄러 Jenkinsfile** | 필요 (Jenkins cron으로 Job 트리거) | 불필요 (서버가 자체 실행 유지) |
| **헬스체크** | exit code 기반 | **HTTP 기반** (`/actuator/health`) |
| **배포 시 고려사항** | 없음 (다음 실행에 새 JAR 사용) | **기존 프로세스 종료 → 새 프로세스 시작** (다운타임 관리) |

### 빌드 명령

```bash
# 개별 모듈 빌드 (모듈명은 {parentDir}-{moduleDir} 패턴)
./gradlew -x test :api-gateway:clean :api-gateway:build
./gradlew -x test :api-auth:clean :api-auth:build
# ... 나머지 동일 패턴
```

### JAR 산출물 경로

```
api/{module}/build/libs/api-{module}-0.0.1-SNAPSHOT.jar
# 예: api/gateway/build/libs/api-gateway-0.0.1-SNAPSHOT.jar
```

### 모듈별 런타임 환경변수 (시크릿)

| 모듈 | 필요 시크릿 |
|---|---|
| `api-gateway` | `JWT_SECRET_KEY`, `REDIS_HOST`, `REDIS_PORT` |
| `api-emerging-tech` | `OPENAI_API_KEY`, `EMERGING_TECH_INTERNAL_API_KEY` |
| `api-auth` | DB 크리덴셜, OAuth 클라이언트 시크릿 (Google/Naver/Kakao), `MAIL_PASSWORD` |
| `api-chatbot` | DB 크리덴셜, `OPENAI_API_KEY`, `COHERE_API_KEY`, `GOOGLE_SEARCH_API_KEY`, Kafka 설정 |
| `api-bookmark` | DB 크리덴셜, Kafka 설정 |
| `api-agent` | DB 크리덴셜, `OPENAI_API_KEY`, `EMERGING_TECH_INTERNAL_API_KEY`, `SLACK_WEBHOOK_URL`, Kafka 설정 |

> DB 크리덴셜은 Aurora 사용 모듈만 해당. emerging-tech와 gateway는 Aurora 미사용.

### 기존 참고 자료

다음 기존 batch 모듈의 Jenkins 설계서와 스크립트를 참고하되, API 모듈의 실행 모델 차이를 반영하여 재설계합니다:
- 설계서: `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md`
- 스크립트: `scripts/jenkins/batch/Jenkinsfile-cicd`, `scripts/jenkins/batch/Jenkinsfile-scheduler`

## 설계서 작성 범위

다음 **3개 섹션**으로 구성된 설계서를 작성합니다.

> Jenkins 설치 및 초기 세팅은 `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md`의 섹션 1에서 이미 완료되었으므로 이 설계서에서는 다루지 않습니다. API 모듈에 추가로 필요한 Credential과 플러그인만 포함합니다.

---

### 섹션 1. API 모듈용 추가 Jenkins 설정

기존 Jenkins 환경(batch 설계서 기준 설치 완료 상태)에 API 모듈 CI/CD를 위해 추가로 필요한 설정을 작성합니다.

**포함 내용:**

1. **추가 Credential 등록**
   - API 모듈별 런타임 시크릿을 Jenkins Credentials에 등록
   - 기존 batch에서 등록한 Credential(GitHub PAT 등)은 재사용하고, 추가분만 안내
   - Credential ID 네이밍 규칙: 기존 batch의 규칙(`{secret-name}-{module-name}`)을 준수
   - **Secret text** 타입 사용 (Pipeline 내 `withCredentials`로 환경변수 주입)

2. **추가 플러그인** (필요한 경우에만)
   - batch 설계서에서 설치한 플러그인(Pipeline, Git, GitHub, Credentials Binding) 외에 API 모듈 CI/CD에 추가로 필요한 플러그인이 있다면 안내
   - 추가 필요 없다면 "추가 플러그인 불필요"로 명시

**작성 지침:**
- 기존 batch 설계서와 중복되는 내용(Jenkins 설치, 기본 Credential 등)은 포함하지 않음
- "이미 등록되어 있으므로 재사용" vs "새로 등록 필요" 구분을 명확히 할 것

---

### 섹션 2. Jenkins Pipeline 스크립트 작성

6개 API 모듈의 CI/CD Pipeline과 배포 Pipeline 스크립트를 작성합니다.

**2-1. CI/CD Pipeline (Jenkinsfile-cicd)**

기존 batch의 `Jenkinsfile-cicd`와 동일한 구조를 기반으로, API 모듈용으로 수정합니다:

| Stage | 설명 |
|---|---|
| Prepare Workspace | 워크스페이스 초기화 |
| Git Checkout | GitHub Repository에서 지정 브랜치 체크아웃 (기존 Credential 재사용) |
| Build JAR | `./gradlew -x test :api-{module}:clean :api-{module}:build` 실행 |
| Archive & Link | 빌드된 JAR을 타임스탬프 포함 경로에 복사, 심볼릭 링크 생성 |

**작성 지침:**
- 기존 `scripts/jenkins/batch/Jenkinsfile-cicd`의 패턴을 최대한 유지 (변수 구조, `agent any`, `JENKINS_HOME` 기반 `BUILD_DIR`, `deleteDir()`, `git rev-parse`, `returnStatus` 등)
- **6개 모듈에 대해 하나의 파라미터화된 Jenkinsfile**을 작성할지, **모듈별 개별 Jenkinsfile**을 작성할지 비교하여 권장안 제시
  - 파라미터화: `MODULE_NAME` 파라미터로 모듈 선택, JAR 경로·빌드 명령을 동적 생성
  - 개별: 모듈당 1개씩 6개 Jenkinsfile, 각각 모듈 정보 하드코딩
- 권장안에 따른 Jenkinsfile 전체 스크립트 제공
- Pipeline parameters로 브랜치명 선택 가능하도록 구성

**2-2. 배포 Pipeline (Jenkinsfile-deploy)**

API 모듈은 상주 프로세스이므로 batch의 스케줄러 Jenkinsfile 대신 **배포 Pipeline**이 필요합니다:

| Stage | 설명 |
|---|---|
| Validate JAR | 심볼릭 링크된 JAR 파일 존재 여부 확인 |
| Stop Running Process | 기존 실행 중인 프로세스 안전 종료 |
| Deploy | 새 JAR로 프로세스 시작 (`nohup java -jar ... &`) |
| Health Check | `/actuator/health` HTTP 응답으로 기동 확인 (타임아웃 포함) |

**작성 지침:**
- 프로세스 관리 전략을 설계할 것:
  - PID 파일 기반 종료 (`kill $(cat pid-file)`) 또는 프로세스명 기반 종료 (`pkill -f`)
  - 두 방식의 장단점을 비교한 후 권장안 선택, 설계 결정 사유 명시
- 모듈별 시크릿 주입: `withCredentials` 블록으로 해당 모듈의 런타임 환경변수 주입
- Health Check는 curl + retry 루프로 구현, 최대 대기 시간 설정
- Gateway(`api-gateway`)의 특수성 고려:
  - 다른 모듈의 트래픽 진입점이므로 배포 순서 또는 주의사항 안내
  - Reactive 스택(Netty)이므로 종료 시 graceful shutdown 고려
- `post { failure { ... } }` 블록에서 실패 시 기존 JAR로 롤백하는 전략 포함 여부 검토

---

### 섹션 3. Jenkins 아이템 등록 및 운영 가이드

작성한 Pipeline 스크립트를 Jenkins에 등록하고 운영하는 절차를 설명합니다.

**3-1. CI/CD 아이템 등록**
- 섹션 2의 권장안(파라미터화 vs 개별)에 따른 Jenkins Item 생성 절차
- Pipeline script from SCM 설정 (GitHub Repository URL, Credential, Branch, Jenkinsfile 경로)
- 아이템 네이밍 규칙: 기존 batch 규칙(`{module-name}-cicd`)을 준수

**3-2. 배포 아이템 등록**
- 배포 Pipeline의 Jenkins Item 생성 절차
- Build with Parameters 실행 방법 (모듈, 브랜치, 프로파일 선택)
- 아이템 네이밍 규칙: `{module-name}-deploy`

**3-3. 운영 관리**
- 빌드 이력 보존 정책 (CI/CD vs 배포 아이템별 권장값)
- 빌드 실패 시 로그 확인 방법
- JAR 파일 정리 전략 (오래된 빌드 자동 삭제)
- **배포 순서 가이드**: 전체 모듈 배포 시 권장 순서 (의존 관계 기반)
- 초기화 빌드 주의사항 (parameters 블록은 첫 실행 후에야 Jenkins UI에 표시됨)

**작성 지침:**
- Jenkins UI 메뉴 경로를 단계별로 명시
- 설정값은 구체적인 예시를 포함

---

## 공통 작성 원칙

1. **간결성**: 각 항목은 실행에 필요한 최소한의 정보만 포함합니다. 불필요한 배경 설명이나 대안 비교를 남발하지 않습니다.
2. **실행 가능성**: 모든 명령어, 설정값, UI 경로는 복사-붙여넣기로 즉시 사용 가능해야 합니다.
3. **기존 설계 준수**: batch 모듈의 Jenkins 설계(`docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md`)에서 확립한 패턴과 규칙을 따릅니다. 차이가 필요한 부분은 API 모듈의 실행 모델 차이로 인한 것만 허용합니다.
4. **공식 출처 준수**: 기술적 근거가 필요한 경우 다음 공식 문서만 참조합니다:
   - Jenkins 공식 문서: https://www.jenkins.io/doc/
   - Spring Boot 공식 문서: https://docs.spring.io/spring-boot/reference/
   - Spring Cloud Gateway 공식 문서: https://docs.spring.io/spring-cloud-gateway/reference/
   - Gradle 공식 문서: https://docs.gradle.org/
5. **현재 프로젝트 맥락 준수**: 대상 모듈 정보에 명시된 기술 스택, 모듈 목록, 포트, 시크릿을 그대로 사용합니다. 임의로 변경하거나 추가하지 않습니다.
6. **오버엔지니어링 경계**: 로컬 테스트 환경 수준의 CI/CD입니다. Kubernetes, Docker 컨테이너 배포, Blue-Green/Canary 배포 등 프로덕션 수준 전략은 범위 밖입니다. 단, 향후 확장 가능성이 자연스럽게 열려 있는 구조는 허용합니다.

## 출력 형식

- Markdown 형식으로 작성
- 코드 블록에는 언어 태그 명시 (```groovy, ```bash, ```yaml 등)
- 각 섹션은 `##` 헤딩으로 구분
- Jenkinsfile 스크립트 뒤에는 **주요 설계 결정** 표를 포함 (기존 batch 설계서 패턴 준수)
