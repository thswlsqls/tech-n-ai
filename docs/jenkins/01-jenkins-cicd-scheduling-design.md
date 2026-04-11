# Jenkins CI/CD 및 Spring Batch 스케줄링 상세 설계서

## 1. Jenkins 로컬 환경 설치 및 초기 세팅

### 1.1 Jenkins 설치

#### Homebrew를 이용한 Jenkins LTS 설치

```bash
# Jenkins LTS 설치
brew install jenkins-lts

# Jenkins 서비스 시작
brew services start jenkins-lts

# 서비스 상태 확인
brew services info jenkins-lts
```

#### 클라우드 배포 시 systemd 서비스 설정 (Ubuntu)

클라우드 Ubuntu VM에 배포하는 경우, Jenkins 프로세스가 로그인 세션과 독립적으로 실행되고 비정상 종료 시 자동 재시작되도록 systemd 서비스를 구성해야 합니다.

> **배경**: Jenkins를 `java -jar jenkins.war &` 등으로 직접 실행하면 로그인 셸의 하위 프로세스가 되어, SSH 세션 종료(로그아웃) 시 Jenkins도 함께 종료됩니다. `apt install jenkins`는 systemd unit을 자동 생성하지만, `Restart=` 정책 기본값이 `no`이므로 비정상 종료 시 자동 복구되지 않습니다.

**1단계 — apt 설치 확인**

`apt install jenkins`로 설치하면 `/lib/systemd/system/jenkins.service` unit 파일이 자동 생성됩니다. 수동 설치(WAR 직접 실행)는 사용하지 않습니다.

**2단계 — systemd override로 자동 재시작 설정**

기본 unit 파일을 직접 수정하면 패키지 업데이트 시 덮어씌워지므로, override 방식을 사용합니다:

```bash
sudo systemctl edit jenkins
```

편집기가 열리면 다음 내용을 입력:

```ini
[Service]
Restart=on-failure
RestartSec=10
```

| 설정 | 값 | 설명 |
|---|---|---|
| `Restart` | `on-failure` | 비정상 종료(exit code ≠ 0, signal에 의한 종료) 시 자동 재시작. 정상 종료(`systemctl stop`)에는 재시작하지 않음 |
| `RestartSec` | `10` | 재시작 대기 시간(초). 즉시 재시작 시 반복 실패 루프 방지 |

저장 후 적용:

```bash
sudo systemctl daemon-reload
sudo systemctl enable jenkins    # 부팅 시 자동 시작
sudo systemctl start jenkins
```

**3단계 — 설정 확인**

```bash
# override 적용 확인
systemctl show jenkins | grep -E "Restart=|RestartUSec="

# 서비스 상태 확인
systemctl status jenkins
```

> macOS 로컬 환경에서는 `brew services start jenkins-lts`가 launchd에 의해 관리되므로 위 설정이 불필요합니다. launchd는 기본적으로 서비스 종료 시 자동 재시작합니다.

#### 초기 접속 및 잠금 해제

1. 브라우저에서 `http://localhost:8080` 접속
2. 초기 관리자 비밀번호 확인:
   ```bash
   cat ~/.jenkins/secrets/initialAdminPassword
   ```
3. 비밀번호 입력 후 **Unlock Jenkins** 클릭

#### 권장 플러그인 설치

- 초기 설정 화면에서 **Install suggested plugins** 선택
- 기본 권장 플러그인이 자동 설치됨 (Git, Pipeline 등 포함)

### 1.2 Jenkins User 세팅

#### 관리자 계정 생성

초기 플러그인 설치 완료 후 **Create First Admin User** 화면이 표시됩니다:

| 필드 | 예시 값 |
|---|---|
| Username | `admin` |
| Password | (보안 강도 높은 비밀번호) |
| Full name | `Admin` |
| E-mail address | `admin@localhost` |

**Save and Continue** > **Instance Configuration** 에서 Jenkins URL을 `http://localhost:8080/`으로 확인 후 **Save and Finish**.

#### 보안 설정

- **CSRF 보호**: Jenkins 2.x 이후 기본 활성화 상태. 별도 설정 불필요
- **접근 제어 확인**: `Jenkins 관리` > `Security` > **Authorization** 섹션에서 **Logged-in users can do anything** 선택 (로컬 테스트 환경 기준)

### 1.3 GitHub Credential 세팅

#### GitHub Personal Access Token (PAT) 생성

본 프로젝트의 GitHub Repository는 **public**입니다. 최소 권한 원칙에 따라 Fine-grained PAT을 사용하여 필요한 권한만 부여합니다.

1. GitHub 접속 > 우측 상단 프로필 > `Settings` > `Developer settings` > `Personal access tokens` > **`Fine-grained tokens`**
2. **Generate new token** 클릭
3. 설정:

   | 필드 | 값 |
   |---|---|
   | Token name | `jenkins-tech-n-ai-backend` |
   | Expiration | 90 days (만료 후 재발급) |
   | Resource owner | 본인 계정 |
   | Repository access | **Only select repositories** > `thswlsqls/tech-n-ai-backend` |

4. **Permissions** — 아래 2개만 설정, 나머지는 모두 **No access** 유지:

   | 카테고리 | Permission | 수준 | 용도 |
   |---|---|---|---|
   | Repository | **Contents** | **Read-only** | Git clone/fetch |
   | Repository | **Metadata** | **Read-only** | Repository 정보 조회 (필수 기본값) |

   > Public Repository이므로 clone 자체는 인증 없이 가능하지만, Jenkins Pipeline의 `git` step에서 Credential 기반 인증을 사용하면 API rate limit이 인증 사용자 기준(5,000 req/hr)으로 완화되고, 향후 private 전환 시 Pipeline 수정이 불필요합니다.

5. **Generate token** 클릭 후 토큰 값 복사 (이 화면을 벗어나면 다시 확인 불가)

#### 토큰 보관 원칙

발급된 토큰은 다음 원칙에 따라 보관합니다.

**1차 보관: Jenkins Credentials Store (운영용)**

Jenkins Credentials에 등록하면 Jenkins가 AES 암호화로 `$JENKINS_HOME/secrets/` 디렉토리에 저장합니다. Pipeline에서는 Credential ID로만 참조하며 토큰 값이 코드에 노출되지 않습니다. 이것이 토큰의 **유일한 운영 사용처**입니다.

**2차 보관: macOS Keychain (백업용)**

Jenkins 재설치, 마이그레이션, 토큰 재등록 등의 상황에 대비하여 macOS Keychain에 백업합니다. macOS Keychain은 AES-256 암호화로 보호되며, 시스템 로그인 비밀번호 또는 Touch ID로만 접근 가능합니다.

**저장**

```bash
security add-generic-password \
    -s "github-pat-tech-n-ai-backend" \
    -a "thswlsqls" \
    -w "발급받은_토큰_값" \
    -T "" \
    login.keychain-db
```

| 옵션 | 값 | 설명 |
|---|---|---|
| `-s` | `github-pat-tech-n-ai-backend` | Service name. Credential을 식별하는 키. Jenkins Credential ID와 일치시켜 관리 용이성 확보 |
| `-a` | `thswlsqls` | Account name. GitHub 사용자명 |
| `-w` | 토큰 값 | 저장할 비밀 문자열 (PAT) |
| `-T ""` | (빈 문자열) | 허용 애플리케이션 없음 — Keychain 접근 시 **항상 사용자 인증(비밀번호/Touch ID) 요구** |

> `-T ""` 미지정 시 `security` CLI가 기본 허용 애플리케이션으로 등록되어 이후 조회 시 인증 없이 접근 가능해집니다. 반드시 지정합니다.

**조회**

```bash
security find-generic-password \
    -s "github-pat-tech-n-ai-backend" \
    -a "thswlsqls" \
    -w \
    login.keychain-db
```

실행 시 macOS가 Keychain 접근 허용 여부를 묻는 팝업을 표시합니다. 비밀번호 또는 Touch ID 인증 후 토큰 값이 stdout에 출력됩니다.

| 옵션 | 설명 |
|---|---|
| `-w` | 비밀 문자열(Password)만 출력. 미지정 시 Service, Account 등 메타데이터 전체 출력 |

**수정 (토큰 갱신 시)**

Keychain은 동일 `-s` + `-a` 조합의 중복 저장을 허용하지 않습니다. 기존 항목을 삭제 후 재저장합니다:

```bash
# 기존 항목 삭제
security delete-generic-password \
    -s "github-pat-tech-n-ai-backend" \
    -a "thswlsqls" \
    login.keychain-db

# 새 토큰으로 재저장
security add-generic-password \
    -s "github-pat-tech-n-ai-backend" \
    -a "thswlsqls" \
    -w "새로_발급받은_토큰_값" \
    -T "" \
    login.keychain-db
```

**GUI 확인 (선택)**

`키체인 접근` 앱(Spotlight에서 "키체인 접근" 검색) > **로그인** 키체인 > 검색창에 `github-pat-tech-n-ai-backend` 입력 > 해당 항목 더블 클릭 > **암호 보기** 체크 시 토큰 값 확인 가능.

**금지 사항:**

| 금지 항목 | 이유 |
|---|---|
| Jenkinsfile, 소스 코드에 토큰 하드코딩 | Repository에 push 시 토큰 유출 |
| `.env`, `.properties` 등 평문 파일 저장 | 파일 시스템 접근만으로 탈취 가능 |
| 메신저, 이메일로 토큰 전달 | 전송 경로에 평문 잔존 |
| 클립보드에 장시간 보관 | 다른 애플리케이션이 클립보드 접근 가능 |

**토큰 갱신 주기:**

Fine-grained PAT의 Expiration을 90일로 설정했으므로, 만료 전에 GitHub에서 재발급 후 Jenkins Credentials와 Keychain 백업을 함께 갱신합니다. GitHub > `Settings` > `Developer settings` > `Fine-grained tokens`에서 만료 예정 토큰을 확인할 수 있습니다.

#### Jenkins Credentials 등록 (Jenkins 2.541.3 기준)

경로: `Jenkins 관리` > `Credentials` > **Stores scoped to Jenkins** 섹션의 `System` 클릭 > `Global credentials (unrestricted)` > **Add Credentials**

**Step 1 — 타입 선택**

"Select a type of credential" 모달에서 다음 6개 타입이 표시됩니다:

| 타입 | 용도 |
|---|---|
| **Username with password** | Git, API, Registry 인증 (HTTP 기반) |
| GitHub App | GitHub App 기반 인증 |
| SSH Username with private key | SSH 키 기반 인증 |
| Secret file | 파일 형태의 시크릿 |
| Secret text | 단일 문자열 시크릿 |
| Certificate | PKCS#12/PEM 인증서 |

**Username with password** 선택 후 **Next** 클릭.

**Step 2 — "Add Username with password" 상세 입력**

| 필드 | 값 | 설명 |
|---|---|---|
| Scope | **Global (Jenkins, nodes, items, all child items, etc)** | 드롭다운에서 선택. 모든 Pipeline Item에서 이 Credential을 참조 가능. `System (Jenkins and nodes only)`은 Pipeline에서 사용 불가하므로 선택하지 않음 |
| Username | `thswlsqls` | GitHub 사용자명 |
| Treat username as secret | **체크 해제** (기본값 유지) | 체크 시 Username도 마스킹되어 로그에서 `****`로 표시됨. 사용자명은 민감 정보가 아니므로 해제 유지 |
| Password | 위에서 생성한 Fine-grained PAT 값 | GitHub 비밀번호가 아닌 **PAT 토큰**을 입력 |
| ID | `github-pat-tech-n-ai-backend` | Pipeline에서 `credentialsId`로 참조할 식별자. 미입력 시 Jenkins가 UUID를 자동 생성하나, 가독성을 위해 직접 지정 |
| Description | `GitHub PAT for tech-n-ai-backend` | 관리 화면에서 Credential 용도를 식별하기 위한 설명 |

**Create** 클릭.

> **Scope 선택 기준**: `Global`은 모든 Jenkins Item(Pipeline, Freestyle 등)에서 참조 가능합니다. `System`은 Jenkins 시스템 설정(노드 연결 등)에서만 사용되며 Pipeline의 `git` step에서 참조할 수 없습니다. Git checkout 용도이므로 반드시 `Global`을 선택합니다.

> **Username with password** 타입을 사용하면 Pipeline의 `git` step에서 `credentialsId`로 참조 시 HTTP 인증에 직접 사용됩니다. Secret text 타입은 Pipeline 내에서 별도의 `withCredentials` 래핑이 필요하므로 Git checkout 용도에는 부적합합니다.

### 1.4 애플리케이션 환경변수 Credential 등록

`batch-source` 모듈은 실행 시 외부 API 인증을 위한 환경변수가 필요합니다. 이 값들은 Jenkins Credentials Store에 **Secret text** 타입으로 등록하고, Pipeline에서 `withCredentials`로 주입합니다.

#### 필수 환경변수

| 환경변수 | 용도 | Credential ID | 비고 |
|---|---|---|---|
| `GITHUB_TOKEN` | GitHub API 인증 (Feign Client) | `github-token-batch-source` | `emerging-tech.github.job` 등에서 사용 |

> `.env` 파일의 `GITHUB_TOKEN`과 동일한 값입니다. `.env`는 로컬 IDE 개발용이며, Jenkins에서는 Credentials Store를 통해 주입합니다.

#### 등록 절차

경로: `Jenkins 관리` > `Credentials` > **Stores scoped to Jenkins** 섹션의 `System` 클릭 > `Global credentials (unrestricted)` > **Add Credentials**

**Step 1 — 타입 선택**

**Secret text** 선택 후 **Next** 클릭.

> Git checkout용 Credential(`github-pat-tech-n-ai-backend`)은 `Username with password` 타입이지만, 환경변수 주입 용도에는 `Secret text`가 적합합니다. `withCredentials`의 `string()` 바인딩으로 환경변수에 직접 매핑되어 추가 파싱이 불필요합니다.

**Step 2 — "Add Secret text" 상세 입력**

| 필드 | 값 | 설명 |
|---|---|---|
| Scope | **Global (Jenkins, nodes, items, all child items, etc)** | Pipeline에서 참조 가능하도록 Global 선택 |
| Secret | `.env`의 `GITHUB_TOKEN` 값 | GitHub Fine-grained PAT 값 입력 |
| ID | `github-token-batch-source` | Pipeline에서 참조할 식별자 |
| Description | `GitHub API Token for batch-source module` | Credential 용도 설명 |

**Create** 클릭.

#### Credential 용도 구분

| Credential ID | 타입 | 용도 |
|---|---|---|
| `github-pat-tech-n-ai-backend` | Username with password | Git checkout (SCM 인증) |
| `github-token-batch-source` | Secret text | 애플리케이션 런타임 환경변수 주입 |

> 동일한 PAT 값이지만 용도와 타입이 다르므로 별도 Credential로 등록합니다. Git checkout은 `git` step이 Username/Password 형식을 요구하고, 환경변수 주입은 `withCredentials`의 `string()` 바인딩이 Secret text를 요구합니다.

#### Keychain 백업

Git checkout용 PAT과 동일한 토큰이므로 기존 Keychain 항목(`github-pat-tech-n-ai-backend`)을 공유합니다. 별도 Keychain 항목 추가는 불필요합니다.

### 1.5 필수 플러그인 확인

초기 설정에서 **Install suggested plugins**를 선택했다면, 아래 플러그인은 대부분 이미 설치되어 있습니다.

#### 설치 여부 확인

경로: `Jenkins 관리` > `Plugins` > **Installed plugins**

검색창에 플러그인명을 입력하여 설치 여부를 확인합니다. **Available plugins** 탭에서는 이미 설치된 플러그인이 검색되지 않습니다.

#### 필수 플러그인 목록

| 플러그인명 (검색 키워드) | Plugin ID | 용도 | Suggested plugins 포함 |
|---|---|---|---|
| Pipeline | `workflow-aggregator` | Declarative/Scripted Pipeline 지원 | O |
| Git plugin | `git` | Git SCM 연동 | O |
| GitHub plugin | `github` | GitHub webhook 및 상태 연동 | O |
| Credentials Binding | `credentials-binding` | Pipeline 내 Credential 참조 | O |

#### 미설치 플러그인이 있는 경우

경로: `Jenkins 관리` > `Plugins` > **Available plugins**

1. 검색창에 플러그인명 입력
2. 해당 플러그인 체크
3. 우측 상단 **Install** 클릭
4. 설치 완료 후 Jenkins 재시작 (필요 시 `brew services restart jenkins-lts`)

---

## 2. Jenkins Pipeline 스크립트

### 2.1 CI/CD Pipeline — `Jenkinsfile-cicd`

GitHub Repository에서 소스를 체크아웃하고 `batch-source` 모듈의 JAR을 빌드하는 Pipeline입니다.

```groovy
def git_repo_url = 'https://github.com/thswlsqls/tech-n-ai-backend.git'
def git_credentials_id = 'github-pat-tech-n-ai-backend'
def project_name = 'batch-source'
def jar_name = 'batch-source-0.0.1-SNAPSHOT.jar'

pipeline {
    agent any

    parameters {
        choice(name: 'BRANCH', choices: ['main', 'dev', 'beta'], description: '빌드 대상 브랜치')
    }

    environment {
        GIT_REPO_URL = "${git_repo_url}"
        GIT_CREDENTIALS_ID = "${git_credentials_id}"
        PROJECT_NAME = "${project_name}"
        BUILD_DIR = "${JENKINS_HOME}/builds"
        JAR_NAME = "${jar_name}"
    }

    stages {
        stage('Prepare Workspace') {
            steps {
                echo "Cleaning workspace..."
                deleteDir()
            }
        }

        stage('Git Checkout') {
            steps {
                git branch: "${params.BRANCH}",
                    credentialsId: "${env.GIT_CREDENTIALS_ID}",
                    url: "${env.GIT_REPO_URL}"

                script {
                    env.commitHash = sh(returnStdout: true, script: 'git rev-parse --short HEAD').trim()
                    echo "Current Git Commit: ${env.commitHash}"
                }
            }
        }

        stage('Build JAR') {
            steps {
                script {
                    env.buildTimestamp = sh(returnStdout: true, script: 'date +%Y%m%d%H%M%S').trim()

                    sh '''
                        chmod +x ./gradlew
                        ./gradlew -x test :${PROJECT_NAME}:clean :${PROJECT_NAME}:build --info
                    '''
                }
            }
        }

        stage('Archive & Link') {
            steps {
                script {
                    def sourcePath = "batch/source/build/libs/${env.JAR_NAME}"
                    def targetPath = "${env.BUILD_DIR}/${env.PROJECT_NAME}-${env.buildTimestamp}.jar"
                    def linkPath = "${env.BUILD_DIR}/${env.PROJECT_NAME}.jar"

                    sh """
                        if [ ! -f ${sourcePath} ]; then
                            echo "JAR file not found: ${sourcePath}"
                            exit 1
                        fi

                        mkdir -p ${env.BUILD_DIR}
                        cp ${sourcePath} ${targetPath}
                        ln -sf ${targetPath} ${linkPath}
                    """

                    echo "JAR archived: ${targetPath}"
                    echo "Symlink updated: ${linkPath}"
                }
            }
        }
    }

    post {
        success {
            echo "Build SUCCESS - Branch: ${params.BRANCH}, Commit: ${env.commitHash}"
        }
        failure {
            echo "Build FAILED - Branch: ${params.BRANCH}"
        }
    }
}
```

#### 주요 설계 결정

| 항목 | 선택 | 이유 |
|---|---|---|
| `agent any` | 로컬 단일 노드 | 테스트 환경에서 label 기반 agent 불필요 |
| `git` step | Declarative `git` directive | `checkout scm` 대비 credential/branch 명시적 지정 가능 |
| `deleteDir()` | Prepare 단계 | 이전 빌드 잔여물로 인한 빌드 오류 방지 |
| 심볼릭 링크 | `ln -sf` | 스케줄링 Pipeline에서 고정 경로로 JAR 참조 가능 |

#### `JENKINS_HOME` 기반 경로 자동 해석

`BUILD_DIR`은 Jenkins 내장 환경변수 `JENKINS_HOME`으로부터 파생됩니다. 환경별 경로가 자동으로 결정되므로 Jenkinsfile 수정 없이 macOS, Ubuntu, Docker 어디서든 동일하게 동작합니다.

| 설치 방식 | OS | `JENKINS_HOME` (자동) | `BUILD_DIR` 해석 결과 |
|---|---|---|---|
| Homebrew (`brew install jenkins-lts`) | macOS | `/Users/{user}/.jenkins` | `/Users/{user}/.jenkins/builds` |
| apt (`sudo apt install jenkins`) | Ubuntu/Debian | `/var/lib/jenkins` | `/var/lib/jenkins/builds` |
| Docker (`jenkins/jenkins` 이미지) | Any | `/var/jenkins_home` | `/var/jenkins_home/builds` |

> `JENKINS_HOME`은 Jenkins가 기동 시 자동으로 설정하는 내장 환경변수입니다. 모든 Pipeline의 `environment` 블록에서 참조 가능하며, 별도 설정이 필요하지 않습니다.

### 2.2 Batch Job 스케줄링 Pipeline — `Jenkinsfile-scheduler`

빌드된 JAR을 사용하여 Spring Batch Job을 주기적으로 실행하는 Pipeline입니다.

```groovy
def project_name = 'batch-source'

pipeline {
    agent any

    parameters {
        choice(
            name: 'JOB_NAME',
            choices: [
                'emerging-tech.scraper.job',
                'emerging-tech.rss.job',
                'emerging-tech.github.job'
            ],
            description: '실행할 Spring Batch Job 이름'
        )
        choice(
            name: 'SPRING_PROFILE',
            choices: ['local', 'dev', 'beta', 'prod'],
            description: 'Spring 프로파일'
        )
    }

    environment {
        BUILD_DIR = "${JENKINS_HOME}/builds"
        PROJECT_NAME = "${project_name}"
        JAR_PATH = "${JENKINS_HOME}/builds/${project_name}.jar"
    }

    // 기본 스케줄: 4시간마다 실행 (H 심볼로 부하 분산)
    // Job별 권장 스케줄:
    //   emerging-tech.scraper.job : H */6 * * *   (6시간마다)
    //   emerging-tech.rss.job    : H */4 * * *   (4시간마다)
    //   emerging-tech.github.job : H H(0-6) * * * (매일 새벽 0~6시 사이 1회)
    triggers {
        cron('H */4 * * *')
    }

    stages {
        stage('Validate JAR') {
            steps {
                script {
                    if (!fileExists("${env.JAR_PATH}")) {
                        error "JAR file not found: ${env.JAR_PATH}. Run CI/CD Pipeline first."
                    }
                    echo "JAR validated: ${env.JAR_PATH}"
                }
            }
        }

        stage('Execute Batch Job') {
            steps {
                withCredentials([
                    string(credentialsId: 'github-token-batch-source', variable: 'GITHUB_TOKEN')
                ]) {
                    script {
                        echo "Executing: ${params.JOB_NAME} with profile ${params.SPRING_PROFILE}"

                        def exitCode = sh(
                            returnStatus: true,
                            script: """
                                java -Dspring.profiles.active=${params.SPRING_PROFILE} \
                                    -jar ${env.JAR_PATH} \
                                    --job.name=${params.JOB_NAME}
                            """
                        )

                        env.batchExitCode = "${exitCode}"

                        if (exitCode != 0) {
                            error "Batch Job failed with exit code: ${exitCode}"
                        }
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                echo "Batch Job completed successfully."
                echo "Job: ${params.JOB_NAME}"
                echo "Profile: ${params.SPRING_PROFILE}"
                echo "Exit Code: ${env.batchExitCode}"
            }
        }
    }

    post {
        failure {
            echo "=== BATCH JOB FAILURE ==="
            echo "Job: ${params.JOB_NAME}"
            echo "Profile: ${params.SPRING_PROFILE}"
            echo "Exit Code: ${env.batchExitCode}"
            echo "Check console output for detailed error logs."
        }
    }
}
```

#### cron 표현식 참고

| 표현식 | 의미 |
|---|---|
| `H */4 * * *` | 4시간마다 (분은 Jenkins가 자동 분산) |
| `H */6 * * *` | 6시간마다 |
| `H H(0-6) * * *` | 매일 새벽 0~6시 사이 1회 |
| `H H(9-17) * * 1-5` | 평일 09~17시 사이 1회 |

> `H` 심볼은 Jenkins가 아이템 이름의 해시값을 기반으로 실행 시점을 분산합니다. 여러 스케줄링 아이템이 동시에 실행되는 것을 방지합니다.

---

## 3. Jenkins 아이템 등록 및 관리 가이드

### 3.1 CI/CD 아이템 등록

#### Step 1 — Item 생성

1. Jenkins 대시보드 좌측 메뉴 > **새로운 Item** 클릭
2. 설정:
   - **Enter an item name**: `batch-source-cicd`
   - 아이템 타입 목록에서 **Pipeline** 선택
3. **OK** 클릭 → 구성 화면으로 이동

#### Step 2 — General 설정

`batch-source-cicd` 구성 화면 상단의 **General** 섹션:

| 설정 | 값 | 설명 |
|---|---|---|
| Description | `batch-source 모듈 CI/CD — GitHub에서 소스 체크아웃 후 JAR 빌드 및 배포` | 대시보드에서 아이템 용도 식별 |
| **오래된 빌드 삭제** | 체크 | 빌드 이력 자동 정리 활성화 |
| ↳ 보관할 최대 빌드 수 | `10` | 최근 10회 빌드만 보존 |
| ↳ 보관 기간 (일) | `30` | 30일 초과 빌드 자동 삭제 |

#### Step 3 — Pipeline 설정

구성 화면 하단의 **Pipeline** 섹션:

| 필드 | 값 | 설명 |
|---|---|---|
| Definition | **Pipeline script from SCM** | Repository의 Jenkinsfile을 사용 |
| SCM | **Git** | Git SCM 선택 |
| Repository URL | `https://github.com/thswlsqls/tech-n-ai-backend.git` | GitHub Repository 주소 |
| Credentials | `github-pat-tech-n-ai-backend` | 1.3에서 등록한 Credential 선택 |
| Branch Specifier | `*/main` | 기본 빌드 브랜치 (`parameters`의 `BRANCH`로 런타임 오버라이드 가능) |
| Script Path | `scripts/jenkins/Jenkinsfile-cicd` | Repository 루트 기준 상대 경로 |
| Lightweight checkout | 체크 (기본값) | Jenkinsfile만 먼저 fetch하여 Pipeline 파싱 속도 향상 |

> **Script Path**는 Repository 루트 기준 상대 경로입니다. Pipeline 스크립트 파일을 `scripts/jenkins/` 디렉토리에 커밋해 두어야 합니다.

**저장** 클릭.

#### Step 4 — 수동 빌드 실행 및 결과 확인

**최초 실행:**

첫 빌드는 Pipeline의 `parameters` 블록을 Jenkins가 인식하기 위한 초기화 빌드입니다.

1. `batch-source-cicd` 아이템 클릭
2. 좌측 메뉴 **지금 빌드** 클릭 (첫 빌드에서는 Build with Parameters가 아직 표시되지 않음)
3. 첫 빌드가 완료되면 Jenkins가 `parameters` 블록을 파싱하여 이후부터 **Build with Parameters** 메뉴가 활성화됨

**이후 실행:**

1. 좌측 메뉴 **Build with Parameters** 클릭
2. `BRANCH` 드롭다운에서 빌드 대상 브랜치 선택 (기본값: `main`)
3. **빌드하기** 클릭

**결과 확인:**

1. 좌측 **Build History**에서 빌드 번호 클릭 (파란색 아이콘: 성공, 빨간색: 실패)
2. **Console Output** 클릭하여 전체 로그 확인
3. 확인 포인트:

   | 로그 메시지 | 의미 |
   |---|---|
   | `BUILD SUCCESSFUL` | Gradle 빌드 성공 |
   | `JAR archived: /Users/m1/.jenkins/builds/batch-source-{timestamp}.jar` | JAR 복사 완료 |
   | `Symlink updated: /Users/m1/.jenkins/builds/batch-source.jar` | 심볼릭 링크 갱신 완료 |
   | `Build SUCCESS - Branch: main, Commit: abc1234` | Pipeline 정상 종료 |

---

### 3.2 Batch Job 스케줄링 아이템 등록

#### 아이템 타입 선택: Freestyle vs Pipeline

| 관점 | Freestyle | Pipeline |
|---|---|---|
| 설정 방식 | GUI에서 빌드 단계 클릭 구성 | Groovy 스크립트(Jenkinsfile) 작성 |
| 설정 버전 관리 | 불가 (Jenkins 내부 XML 저장) | **Jenkinsfile이 Git에 커밋되어 변경 이력 추적 가능** |
| 실패 단계 식별 | Console Output 전체를 읽어야 함 | **Stage 단위 시각화로 즉시 식별** |
| 환경 이관 | 수작업 재구성 | **Repository 연결만으로 복원** |
| CI/CD와 통일성 | CI/CD가 Pipeline이면 타입 혼재 | **동일 타입으로 운영 복잡도 감소** |
| 설정 복잡도 | 낮음 | 중간 (Groovy 문법 필요) |
| APM 연계 (Stage 트레이싱) | **불가** (Stage 개념 없음) | **가능** (OpenTelemetry Plugin으로 Stage별 Span 추적) |

**Pipeline 선택 이유:**

단순히 `java -jar`을 cron으로 실행하는 것만이 목적이라면 Freestyle이 더 간단합니다. 그러나 현재 프로젝트에서는 다음 이유로 Pipeline을 선택합니다:

1. `batch-source-cicd`(CI/CD)가 이미 Pipeline이므로 **아이템 타입을 통일**하여 운영 복잡도를 낮춤
2. Validate JAR → Execute Batch Job → Health Check의 **다단계 흐름에서 실패 지점을 Stage 시각화로 즉시 식별** 가능
3. Jenkinsfile이 Git에 관리되어 **환경 이관 시 재구성 불필요**
4. 향후 APM 구축 시 **Jenkins OpenTelemetry Plugin**으로 Stage 단위 분산 트레이싱 가능 — Freestyle에서는 구조적으로 불가

> **APM 관점 참고**: Jenkins Prometheus Plugin의 Job 레벨 메트릭(실행 횟수, 소요 시간, 성공/실패율)은 Freestyle과 Pipeline 모두 동일하게 노출됩니다. Pipeline만의 추가 이점은 OpenTelemetry Plugin을 통한 **Stage 단위 관측 가능성(Observability)**입니다. Spring Batch 측 메트릭은 Micrometer + Prometheus Pushgateway로 수집하며, Grafana에서 Jenkins 메트릭과 통합 대시보드로 구성할 수 있습니다. APM 스택 구축 상세는 별도 설계서에서 다룹니다.

#### 사전 조건

스케줄링 아이템은 CI/CD Pipeline이 빌드한 JAR 파일을 실행합니다. 반드시 아래 조건을 충족한 후 등록합니다:

1. `batch-source-cicd` 아이템이 **1회 이상 성공**하여 `/Users/m1/.jenkins/builds/batch-source.jar` 심볼릭 링크가 존재할 것
2. 확인 명령:
   ```bash
   ls -la /Users/m1/.jenkins/builds/batch-source.jar
   ```

#### 아이템 구성 방식 비교

| 방식 | 장점 | 단점 |
|---|---|---|
| **Job별 개별 Item** | 독립 스케줄, 독립 이력 관리, 개별 모니터링 용이 | Item 수 증가 |
| **파라미터화된 단일 Item** | Item 관리 간소화 | cron trigger 시 파라미터 고정 불가, 스케줄 분리 불가 |

**권장: Job별 개별 Item 생성**

스케줄링 아이템은 각 Job이 서로 다른 실행 주기를 가지므로, Job별 개별 Item으로 생성하여 독립적인 cron 스케줄과 빌드 이력을 관리합니다.

#### 스케줄 관리 전략

cron 스케줄을 설정하는 방법은 두 가지입니다:

| 방식 | 설정 위치 | 장점 | 단점 |
|---|---|---|---|
| **Jenkinsfile `triggers` 블록** | `scripts/jenkins/Jenkinsfile-scheduler` 내 `triggers { cron('...') }` | 스케줄이 Git으로 버전 관리됨, 코드 리뷰 가능 | 변경 시 commit & push 필요 |
| **Jenkins UI Build periodically** | 각 Item 구성 > Build Triggers > Build periodically | 즉시 변경 가능, 재시작 불필요 | Git에 이력이 남지 않음 |

**권장: Jenkins UI (Build periodically) 사용**

로컬 테스트 환경에서는 스케줄을 빈번하게 조정하므로 Jenkins UI에서 직접 관리합니다. 이 경우 Jenkinsfile-scheduler의 `triggers` 블록은 제거하거나 주석 처리합니다.

> **중복 설정 주의**: 둘 다 설정된 경우 Jenkins가 **중복 실행**합니다. 반드시 한 곳에서만 관리합니다.

#### 전체 아이템 목록

| Jenkins Item Name | Batch Job | 권장 cron | 설명 |
|---|---|---|---|
| `batch-source-schedule-scraper` | `emerging-tech.scraper.job` | `H */6 * * *` | 6시간마다 웹 크롤링 |
| `batch-source-schedule-rss` | `emerging-tech.rss.job` | `H */4 * * *` | 4시간마다 RSS 수집 |
| `batch-source-schedule-github` | `emerging-tech.github.job` | `H H(0-6) * * *` | 매일 새벽 GitHub Releases 수집 |

#### 등록 절차 (3개 Item 각각 반복)

아래는 `batch-source-schedule-rss`를 예시로 전체 절차를 설명합니다. 나머지 2개 Item도 동일한 절차로 생성하되, "나머지 Item 생성 시 변경 항목" 섹션의 값만 변경합니다.

**Step 1 — Item 생성**

1. Jenkins 대시보드 > **새로운 Item**
2. 설정:
   - **Enter an item name**: `batch-source-schedule-rss`
   - 아이템 타입: **Pipeline**
3. **OK** 클릭 → 구성 화면으로 이동

**Step 2 — General 설정**

| 설정 | 값 | 설명 |
|---|---|---|
| Description | `emerging-tech.rss.job 스케줄링 — 4시간마다 RSS 피드 수집` | 대시보드에서 아이템 용도 식별 |
| **오래된 빌드 삭제** | 체크 | 빌드 이력 자동 정리 활성화 |
| ↳ 보관할 최대 빌드 수 | `30` | 스케줄링은 빈번하게 실행되므로 CI/CD(`10`)보다 넉넉히 설정 |
| ↳ 보관 기간 (일) | `7` | 7일 초과 빌드 자동 삭제. CI/CD(`30`일)보다 짧게 설정 |

**Step 3 — Build Triggers 설정**

구성 화면의 **Build Triggers** 섹션:

1. **Build periodically** 체크
2. **Schedule** 입력란에 cron 표현식 입력:
   ```
   H */4 * * *
   ```
3. 입력 후 Jenkins가 하단에 다음 예정 실행 시간을 표시하는지 확인:
   ```
   Would last have run at Thursday, April 10, 2026 오후 8:32:XX KST;
   would next run at Friday, April 11, 2026 오전 0:32:XX KST.
   ```

> cron 표현식 입력 시 Jenkins가 자동으로 다음 실행 시간 미리보기를 표시합니다. 이 미리보기가 의도한 주기와 일치하는지 반드시 확인합니다.

**cron 표현식 필드 구조 참고:**

```
분  시  일  월  요일
H  */4  *   *   *
│   │   │   │   └── 요일 (0-7, 0과 7은 일요일)
│   │   │   └────── 월 (1-12)
│   │   └────────── 일 (1-31)
│   └────────────── 시 (0-23), */4 = 4시간마다
└────────────────── 분 (0-59), H = Jenkins가 해시 기반 자동 분산
```

**Step 4 — Pipeline 설정**

구성 화면 하단의 **Pipeline** 섹션:

| 필드 | 값 | 설명 |
|---|---|---|
| Definition | **Pipeline script from SCM** | Repository의 Jenkinsfile을 사용 |
| SCM | **Git** | Git SCM 선택 |
| Repository URL | `https://github.com/thswlsqls/tech-n-ai-backend.git` | GitHub Repository 주소 |
| Credentials | `github-pat-tech-n-ai-backend` | 1.3에서 등록한 Credential 선택 |
| Branch Specifier | `*/main` | 기본 브랜치 |
| Script Path | `scripts/jenkins/Jenkinsfile-scheduler` | 3개 스케줄링 Item 모두 동일한 스크립트 공유 |
| Lightweight checkout | 체크 (기본값) | Jenkinsfile만 먼저 fetch |

**저장** 클릭.

**Step 5 — 수동 테스트 실행**

**5-1. 초기화 빌드**

첫 빌드는 `parameters` 블록 인식을 위한 초기화 빌드입니다.

1. 좌측 메뉴 **지금 빌드** 클릭
2. 이 빌드는 `parameters` 기본값으로 실행됨 (JOB_NAME: `emerging-tech.scraper.job`, SPRING_PROFILE: `local`)
3. 빌드 완료 후 좌측 메뉴에 **Build with Parameters**가 활성화되는지 확인

> 초기화 빌드는 성공/실패 여부와 관계없이 `parameters` 블록을 인식시키는 것이 목적입니다.

**5-2. 파라미터 지정 실행**

1. 좌측 메뉴 **Build with Parameters** 클릭
2. 파라미터 선택:

   | 파라미터 | 선택 값 | 설명 |
   |---|---|---|
   | `JOB_NAME` | `emerging-tech.rss.job` | 이 Item에서 실행할 Batch Job |
   | `SPRING_PROFILE` | `local` | 로컬 환경 프로파일 |

3. **빌드하기** 클릭

**5-3. 결과 확인**

좌측 **Build History**에서 빌드 번호 클릭 > **Console Output**:

| 로그 메시지 | 의미 | 정상 여부 |
|---|---|---|
| `JAR validated: /Users/m1/.jenkins/builds/batch-source.jar` | JAR 파일 존재 확인 완료 | 정상 |
| `Executing: emerging-tech.rss.job with profile local` | Batch Job 실행 시작 | 정상 |
| `Batch Job completed successfully.` | Job 정상 종료 | 성공 |
| `Exit Code: 0` | Spring Batch Job이 정상 완료 코드 반환 | 성공 |
| `JAR file not found` | 심볼릭 링크 또는 JAR 부재 | 실패 — CI/CD 먼저 실행 |
| `Batch Job failed with exit code: N` | Spring Batch Job 실행 오류 | 실패 — 로그 상세 확인 |

#### 나머지 Item 생성 시 변경 항목

아래 표의 값만 변경하고 나머지 설정(Pipeline, Lightweight checkout 등)은 `batch-source-schedule-rss`와 동일합니다.

**`batch-source-schedule-scraper`**

| Step | 설정 | 값 |
|---|---|---|
| Step 1 | Item name | `batch-source-schedule-scraper` |
| Step 2 | Description | `emerging-tech.scraper.job 스케줄링 — 6시간마다 웹 크롤링` |
| Step 3 | Build periodically | `H */6 * * *` |
| Step 5 | 수동 테스트 시 JOB_NAME | `emerging-tech.scraper.job` |

**`batch-source-schedule-github`**

| Step | 설정 | 값 |
|---|---|---|
| Step 1 | Item name | `batch-source-schedule-github` |
| Step 2 | Description | `emerging-tech.github.job 스케줄링 — 매일 새벽 GitHub Releases 수집` |
| Step 3 | Build periodically | `H H(0-6) * * *` |
| Step 5 | 수동 테스트 시 JOB_NAME | `emerging-tech.github.job` |

#### cron 자동 실행 시 파라미터 기본값

cron trigger로 자동 실행 시 `parameters`의 **첫 번째 값(기본값)**이 사용됩니다:

| 파라미터 | 기본값 (Jenkinsfile `choices` 배열의 첫 번째) |
|---|---|
| `JOB_NAME` | `emerging-tech.scraper.job` |
| `SPRING_PROFILE` | `local` |

3개 Item이 동일한 `Jenkinsfile-scheduler`를 공유하므로, cron 자동 실행 시에는 항상 `JOB_NAME`의 기본값인 `emerging-tech.scraper.job`이 실행됩니다. 이것이 의도와 다를 수 있으므로 다음 두 가지 대응 방안이 있습니다:

**방안 A — Jenkins UI에서 기본값 오버라이드 (권장)**

각 Item의 구성 > **This project is parameterised** 체크 > **Choice Parameter** 추가:

| Item | Parameter name | Choices (첫 번째가 기본값) |
|---|---|---|
| `batch-source-schedule-scraper` | `JOB_NAME` | `emerging-tech.scraper.job` (1개만 입력) |
| `batch-source-schedule-rss` | `JOB_NAME` | `emerging-tech.rss.job` (1개만 입력) |
| `batch-source-schedule-github` | `JOB_NAME` | `emerging-tech.github.job` (1개만 입력) |

Jenkins UI에서 정의한 파라미터가 Jenkinsfile의 `parameters` 블록보다 우선합니다. 이 방식으로 각 Item이 cron 실행 시 올바른 Job을 자동 실행합니다.

**방안 B — Item별 별도 Jenkinsfile 작성**

`Jenkinsfile-scheduler-rss`, `Jenkinsfile-scheduler-scraper`, `Jenkinsfile-scheduler-github`로 분리하여 각 스크립트에서 `choices`의 첫 번째 값을 해당 Job으로 설정합니다. 스크립트 관리 비용이 증가하므로 방안 A를 권장합니다.

---

### 3.3 운영 관리

#### 대시보드 아이템 구조

등록 완료 후 Jenkins 대시보드에 다음 4개 Item이 표시됩니다:

| Item Name | 타입 | 실행 방식 |
|---|---|---|
| `batch-source-cicd` | CI/CD | 수동 (Build with Parameters) |
| `batch-source-schedule-scraper` | 스케줄링 | cron 자동 + 수동 |
| `batch-source-schedule-rss` | 스케줄링 | cron 자동 + 수동 |
| `batch-source-schedule-github` | 스케줄링 | cron 자동 + 수동 |

#### 빌드 상태 아이콘

| 아이콘 | 의미 | 조치 |
|---|---|---|
| 파란색 (또는 녹색) 원 | 최근 빌드 성공 | 정상 |
| 빨간색 원 | 최근 빌드 실패 | Console Output 확인 |
| 회색 원 | 빌드 미실행 | 초기 상태 또는 비활성 |
| 깜빡이는 아이콘 | 빌드 진행 중 | 대기 |

#### 빌드 실패 시 로그 확인

1. 대시보드에서 실패한 아이템의 빌드 번호 클릭 (빨간색 아이콘)
2. 좌측 메뉴 **Console Output** 클릭
3. 주요 에러 패턴:

   | 에러 로그 | 원인 | 조치 |
   |---|---|---|
   | `JAR file not found: /Users/m1/.jenkins/builds/batch-source.jar` | CI/CD 미실행 또는 빌드 실패 | `batch-source-cicd` 아이템 먼저 실행 |
   | `Batch Job failed with exit code: N` | Spring Batch Job 실행 오류 | exit code와 Spring Batch 로그로 원인 분석 (DB 연결, 외부 API 등) |
   | `FAILURE` + Gradle stacktrace | Gradle 빌드 오류 | 컴파일 에러, 의존성 문제 확인 |
   | `ERROR: Error cloning remote repo` | Git 체크아웃 실패 | Credential 만료 여부, Repository URL 확인 |
   | `java: command not found` | Java 미설치 또는 PATH 미설정 | Jenkins 노드에 Java 21 설치 확인 |

#### JAR 파일 관리

CI/CD Pipeline 빌드 시마다 타임스탬프가 포함된 JAR이 누적됩니다:

```
/Users/m1/.jenkins/builds/
├── batch-source.jar -> batch-source-20260411143052.jar   # 심볼릭 링크 (최신)
├── batch-source-20260411143052.jar                       # 최신 빌드
├── batch-source-20260410120000.jar                       # 이전 빌드
├── batch-source-20260409080000.jar                       # 이전 빌드
└── ...
```

주기적으로 오래된 JAR을 정리합니다:

```bash
# 7일 이상 된 JAR 파일 목록 확인 (삭제 전 반드시 확인)
find /Users/m1/.jenkins/builds -name "batch-source-*.jar" -mtime +7 -not -name "batch-source.jar"

# 확인 후 삭제
find /Users/m1/.jenkins/builds -name "batch-source-*.jar" -mtime +7 -not -name "batch-source.jar" -delete
```

| 주의 사항 | 설명 |
|---|---|
| `batch-source.jar` 보호 | 심볼릭 링크이므로 `-not -name` 조건으로 삭제 대상에서 제외 |
| 심볼릭 링크 대상 보호 | `-mtime +7` 조건으로 최신 JAR이 삭제되지 않도록 보장. 최소 7일 이내 CI/CD를 1회 이상 실행해야 안전 |
| 디스크 용량 모니터링 | `du -sh /Users/m1/.jenkins/builds/`로 주기적 확인 |
