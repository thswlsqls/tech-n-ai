# Jenkins CI/CD 및 Spring Batch 스케줄링 상세 설계서 작성 프롬프트

## 역할 및 목적

당신은 **CI/CD 및 빌드 자동화 전문 엔지니어**입니다. 로컬 테스트 환경에서 Jenkins를 활용하여 Spring Batch Application의 CI/CD 파이프라인과 Batch Job 스케줄링을 구축하기 위한 **상세 설계서**를 작성합니다.

## 대상 모듈 정보

### 프로젝트 개요
- **GitHub Repository**: https://github.com/thswlsqls/tech-n-ai-backend
- **대상 모듈**: `batch/source` (Gradle 모듈명: `batch-source`)
- **기술 스택**: Java 21, Spring Boot 4.0.2, Spring Batch 6.0.2, Gradle 9.2.1 (Groovy DSL)
- **환경 프로파일**: `local`, `dev`, `beta`, `prod`

### Batch Job 목록
현재 `batch-source` 모듈에 등록된 Spring Batch Job은 다음과 같습니다:

| Job Name | 설명 | JobConfig 클래스 |
|---|---|---|
| `emerging-tech.scraper.job` | 웹 크롤링 기반 기술 아티클 수집 | `EmergingTechScraperJobConfig` |
| `emerging-tech.rss.job` | RSS 피드 기반 기술 아티클 수집 | `EmergingTechRssJobConfig` |
| `emerging-tech.github.job` | GitHub Releases 기반 기술 정보 수집 | `GitHubReleasesJobConfig` |

### Batch Application 실행 방식
- `spring.batch.job.enabled=true`, `spring.batch.job.name=${job.name:NONE}`으로 Job 이름을 외부 파라미터로 지정
- 실행 명령 예시:
  ```bash
  java -jar batch-source-0.0.1-SNAPSHOT.jar --job.name=emerging-tech.scraper.job --spring.profiles.active=local
  ```
  > `--job.name`은 프로젝트 application.yml의 `spring.batch.job.name=${job.name:NONE}` placeholder에 바인딩되는 프로젝트 고유 파라미터입니다.
- Application은 Job 실행 완료 후 `System.exit(SpringApplication.exit(...))` 방식으로 종료됨

### 빌드 명령
```bash
./gradlew -x test :batch-source:clean :batch-source:build
```

### 참고 Jenkinsfile
기존 프로젝트에서 사용하던 Jenkins Pipeline 스크립트를 참고하되, 현재 프로젝트에 맞게 재작성합니다:
- 파일 위치: `scripts/reference/jenkinsfile`
- 주요 참고 포인트: Pipeline 구조(Prepare → Git Clone → Build), JAR 빌드 및 심볼릭 링크 배포 패턴

## 설계서 작성 범위

다음 **3개 섹션**으로 구성된 설계서를 작성합니다.

---

### 섹션 1. Jenkins 로컬 환경 설치 및 초기 세팅

로컬 macOS 머신에 Jenkins를 설치하고 초기 설정하는 가이드를 작성합니다.

**포함 내용:**

1. **Jenkins 설치**
   - Homebrew를 이용한 Jenkins LTS 설치 및 실행
   - 초기 관리자 비밀번호 확인 및 접속 (`http://localhost:8080`)
   - 권장 플러그인 설치

2. **Jenkins User 세팅**
   - 관리자 계정 생성 절차
   - 보안 설정 (CSRF 보호는 Jenkins 2.x 이후 기본 활성화 — 별도 설정 불필요함을 명시, 접근 제어 설정만 안내)

3. **GitHub Credential 세팅**
   - GitHub Personal Access Token (PAT) 생성 (필요 권한: `repo` scope)
   - Jenkins Credentials에 GitHub PAT 등록 — **Username with password** 타입 사용 (Username: GitHub 사용자명, Password: PAT 입력). Git checkout 시 HTTP 인증에 직접 사용 가능
   - Credential ID 네이밍 규칙 지정

4. **필수 플러그인 목록** (최소 필요 세트)
   - **Pipeline** (`workflow-aggregator`): Declarative/Scripted Pipeline 지원
   - **Git** (`git`): Git SCM 연동
   - **GitHub** (`github`): GitHub webhook 및 상태 연동
   - **Credentials Binding** (`credentials-binding`): Pipeline 내 Credential 참조

**작성 지침:**
- 각 단계는 실행 가능한 명령어 또는 UI 경로를 포함할 것
- 스크린샷 대신 Jenkins UI 메뉴 경로를 텍스트로 명시 (예: `Jenkins 관리 > Credentials > System > Global credentials`)

---

### 섹션 2. Jenkins Pipeline 스크립트 작성

GitHub Repository와 연동된 CI/CD Pipeline 및 Batch Job 스케줄링 Pipeline 스크립트를 작성합니다.

**2-1. CI/CD Pipeline (Jenkinsfile)**

다음 Stage 구조로 Declarative Pipeline 스크립트를 작성합니다:

| Stage | 설명 |
|---|---|
| Prepare Workspace | 워크스페이스 초기화 |
| Git Checkout | GitHub Repository에서 지정 브랜치 체크아웃 (Credential 사용) |
| Build JAR | `./gradlew -x test :batch-source:clean :batch-source:build` 실행 |
| Archive & Link | 빌드된 JAR을 타임스탬프 포함 경로에 복사, 심볼릭 링크 생성 |

**작성 지침:**
- `scripts/reference/jenkinsfile`의 구조를 참고하되, GitHub 인증 방식으로 변경 (CodeCommit → GitHub PAT)
- `agent { label }` 대신 로컬 환경에 맞는 `agent any` 사용
- 환경변수로 분리할 항목: `GIT_REPO_URL`, `BRANCH`, `PROJECT_NAME`, `BUILD_DIR`, `JAR_NAME` (실제 빌드 산출물: `batch-source-0.0.1-SNAPSHOT.jar`, 경로: `batch/source/build/libs/`)
- Pipeline parameters로 브랜치명을 선택 가능하도록 구성

**2-2. Batch Job 스케줄링 Pipeline (Jenkinsfile)**

Jenkins의 `cron` trigger를 활용하여 Spring Batch Job을 주기적으로 실행하는 Pipeline 스크립트를 작성합니다:

| Stage | 설명 |
|---|---|
| Validate JAR | 심볼릭 링크된 JAR 파일 존재 여부 확인 |
| Execute Batch Job | `java -jar` 명령으로 지정된 Batch Job 실행 |
| Health Check | Job 실행 결과 확인 (exit code 기반) |

**작성 지침:**
- Pipeline parameter로 `JOB_NAME`을 선택 가능하도록 구성 (선택지: `emerging-tech.scraper.job`, `emerging-tech.rss.job`, `emerging-tech.github.job`)
- Pipeline parameter로 `SPRING_PROFILE`을 선택 가능하도록 구성
- `triggers { cron('...') }` 블록에 기본 스케줄을 설정하되, Jenkins 공식 권장인 `H` 심볼을 사용하여 빌드 부하를 분산할 것 (예: `H */4 * * *`). 각 Job별 권장 스케줄을 주석으로 안내
- Job 실행 실패 시 `post { failure { ... } }` 블록에서 로그 출력

---

### 섹션 3. Jenkins 아이템 등록 및 관리 가이드

작성한 Pipeline 스크립트를 Jenkins에 등록하고 운영하는 절차를 설명합니다.

**3-1. CI/CD 아이템 등록**
- Pipeline 타입의 Jenkins Item 생성 절차
- Pipeline script from SCM 설정 (GitHub Repository URL, Credential, Branch, Jenkinsfile 경로)
- 수동 빌드 실행 및 결과 확인 방법

**3-2. Batch Job 스케줄링 아이템 등록**
- 각 Batch Job별 개별 Jenkins Item 생성 또는 파라미터화된 단일 Item 사용 — 두 방식의 장단점 비교 후 권장안 제시
- cron 표현식 설정 방법 및 각 Job별 권장 스케줄 예시
- Build with Parameters 실행 방법

**3-3. 운영 관리**
- 빌드 이력 관리 (보존 정책 설정)
- 빌드 실패 시 로그 확인 방법
- JAR 파일 관리 (오래된 빌드 정리)

**작성 지침:**
- Jenkins UI 메뉴 경로를 단계별로 명시
- 설정값은 구체적인 예시를 포함

---

## 공통 작성 원칙

1. **간결성**: 각 항목은 실행에 필요한 최소한의 정보만 포함합니다. 불필요한 배경 설명이나 대안 비교를 남발하지 않습니다.
2. **실행 가능성**: 모든 명령어, 설정값, UI 경로는 복사-붙여넣기로 즉시 사용 가능해야 합니다.
3. **공식 출처 준수**: 기술적 근거가 필요한 경우 다음 공식 문서만 참조합니다:
   - Jenkins 공식 문서: https://www.jenkins.io/doc/
   - Spring Batch 공식 문서: https://docs.spring.io/spring-batch/reference/
   - Gradle 공식 문서: https://docs.gradle.org/
4. **현재 프로젝트 맥락 준수**: 대상 모듈 정보에 명시된 기술 스택, Job 목록, 실행 방식, 빌드 명령을 그대로 사용합니다. 임의로 변경하거나 추가하지 않습니다.

## 출력 형식

- Markdown 형식으로 작성
- 코드 블록에는 언어 태그 명시 (```groovy, ```bash, ```yaml 등)
- 각 섹션은 `##` 헤딩으로 구분
