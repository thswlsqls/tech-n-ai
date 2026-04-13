# Jenkins API 모듈 CI/CD 및 배포 상세 설계서

## 1. API 모듈용 추가 Jenkins 설정

> Jenkins 설치, 초기 세팅, GitHub PAT Credential(`github-pat-tech-n-ai-backend`)은 `docs/jenkins/batch/01-jenkins-cicd-scheduling-design.md`의 섹션 1에서 완료된 상태입니다. 이 섹션에서는 API 모듈 CI/CD에 **추가로 필요한 설정만** 다룹니다.

### 1.1 기존 Credential 재사용

다음 Credential은 batch 설계서에서 이미 등록되어 있으므로 그대로 재사용합니다:

| Credential ID | 타입 | 용도 | 재사용 범위 |
|---|---|---|---|
| `github-pat-tech-n-ai-backend` | Username with password | Git checkout (SCM 인증) | 모든 API 모듈의 CI/CD Pipeline |

> API 모듈과 batch 모듈은 동일한 GitHub Repository를 사용하므로 SCM Credential을 공유합니다.

### 1.2 추가 Credential 등록

API 모듈은 실행 시 외부 API 인증 및 인프라 접속을 위한 환경변수가 필요합니다. `.env` 파일의 값을 Jenkins Credentials Store에 **Secret text** 타입으로 등록하고, 배포 Pipeline에서 `withCredentials`로 주입합니다.

#### 등록 절차

경로: `Jenkins 관리` > `Credentials` > **Stores scoped to Jenkins** 섹션의 `System` 클릭 > `Global credentials (unrestricted)` > **Add Credentials**

1. **Secret text** 선택 후 **Next** 클릭
2. 아래 표의 각 행에 대해 반복 등록

#### 공통 Credential (전체 모듈 공유)

다음은 여러 API 모듈이 공통으로 사용하는 시크릿입니다. 모듈별로 중복 등록하지 않고 하나의 Credential ID로 공유합니다.

| Credential ID | Secret 값 (`.env` 참조) | 사용 모듈 | Description |
|---|---|---|---|
| `openai-api-key` | `.env`의 `OPENAI_API_KEY` | emerging-tech, chatbot, agent | OpenAI API Key |
| `redis-password` | `.env`의 `REDIS_PASSWORD` | 전체 (gateway는 Rate Limiter용, 나머지는 common-core 상속) | Redis Password |
| `jwt-secret-key` | `.env`의 `JWT_SECRET_KEY` | gateway | JWT Signing Secret Key |
| `mongodb-atlas-connection-string` | `.env`의 `MONGODB_ATLAS_CONNECTION_STRING` | emerging-tech, chatbot, bookmark, agent | MongoDB Atlas Connection String |
| `mysql-user` | `.env`의 `MYSQL_USER` | auth, chatbot, bookmark, agent | MySQL Username |
| `mysql-password` | `.env`의 `MYSQL_PASSWORD` | auth, chatbot, bookmark, agent | MySQL Password |
| `emerging-tech-internal-api-key` | `.env`의 `EMERGING_TECH_INTERNAL_API_KEY` | emerging-tech, agent | Internal API Key |

#### 모듈 전용 Credential

다음은 특정 모듈에서만 사용하는 시크릿입니다.

| Credential ID | Secret 값 (`.env` 참조) | 사용 모듈 | Description |
|---|---|---|---|
| `mail-password-auth` | `.env`의 `MAIL_PASSWORD` | auth | Gmail SMTP App Password |
| `cohere-api-key-chatbot` | `.env`의 `COHERE_API_KEY` | chatbot | Cohere Reranking API Key |
| `google-search-api-key-chatbot` | `.env`의 `GOOGLE_SEARCH_API_KEY` | chatbot | Google Custom Search API Key |
| `google-search-engine-id-chatbot` | `.env`의 `GOOGLE_SEARCH_ENGINE_ID` | chatbot | Google Custom Search Engine ID |
| `slack-webhook-url-agent` | `.env`의 `SLACK_WEBHOOK_URL` | agent | Slack Webhook URL |
| `slack-bot-token-agent` | `.env`의 `SLACK_BOT_TOKEN` | agent | Slack Bot Token |

#### Credential ID 네이밍 규칙

| 패턴 | 적용 대상 | 예시 |
|---|---|---|
| `{secret-name}` | 여러 모듈 공유 | `openai-api-key`, `redis-password` |
| `{secret-name}-{module}` | 단일 모듈 전용 | `mail-password-auth`, `cohere-api-key-chatbot` |

> batch 설계서의 `github-token-batch-source` 패턴(`{secret-name}-{module-name}`)을 따르되, 여러 모듈이 공유하는 시크릿은 모듈명을 생략하여 중복 등록을 방지합니다.

#### 등록 예시 — `openai-api-key`

| 필드 | 값 |
|---|---|
| Scope | **Global (Jenkins, nodes, items, all child items, etc)** |
| Secret | `.env`의 `OPENAI_API_KEY` 값 |
| ID | `openai-api-key` |
| Description | `OpenAI API Key for emerging-tech, chatbot, agent modules` |

**Create** 클릭. 나머지 Credential도 동일한 절차로 등록합니다.

#### Keychain 백업

공통 Credential과 모듈 전용 Credential 모두 macOS Keychain에 백업합니다. batch 설계서(섹션 1.3)의 Keychain 저장 방식과 동일합니다:

```bash
# 예시: OpenAI API Key 백업
security add-generic-password \
    -s "openai-api-key" \
    -a "tech-n-ai" \
    -w "실제_API_키_값" \
    -T "" \
    login.keychain-db
```

> 모든 Credential에 대해 `-s` 값을 Credential ID와 일치시킵니다.

### 1.3 추가 플러그인

추가 플러그인 **불필요**합니다.

batch 설계서에서 설치한 4개 플러그인(Pipeline, Git, GitHub, Credentials Binding)으로 API 모듈의 CI/CD 및 배포 Pipeline을 모두 구성할 수 있습니다. 배포 Pipeline의 Health Check는 `sh 'curl ...'` 명령으로 구현하며, 별도 플러그인이 필요하지 않습니다.

### 1.4 전체 Credential 현황

batch 설계서에서 등록한 Credential과 이 설계서에서 추가하는 Credential의 전체 목록입니다:

| Credential ID | 타입 | 등록 시점 | 용도 |
|---|---|---|---|
| `github-pat-tech-n-ai-backend` | Username with password | batch 설계서 | Git checkout (전체 모듈 공유) |
| `github-token-batch-source` | Secret text | batch 설계서 | batch-source 런타임 환경변수 |
| `openai-api-key` | Secret text | **이 설계서** | OpenAI API Key (공유) |
| `redis-password` | Secret text | **이 설계서** | Redis Password (공유) |
| `jwt-secret-key` | Secret text | **이 설계서** | JWT Secret (gateway) |
| `mongodb-atlas-connection-string` | Secret text | **이 설계서** | MongoDB Atlas (공유) |
| `mysql-user` | Secret text | **이 설계서** | MySQL Username (공유) |
| `mysql-password` | Secret text | **이 설계서** | MySQL Password (공유) |
| `emerging-tech-internal-api-key` | Secret text | **이 설계서** | Internal API Key (공유) |
| `mail-password-auth` | Secret text | **이 설계서** | Gmail SMTP Password (auth) |
| `cohere-api-key-chatbot` | Secret text | **이 설계서** | Cohere API Key (chatbot) |
| `google-search-api-key-chatbot` | Secret text | **이 설계서** | Google Search API Key (chatbot) |
| `google-search-engine-id-chatbot` | Secret text | **이 설계서** | Google Search Engine ID (chatbot) |
| `slack-webhook-url-agent` | Secret text | **이 설계서** | Slack Webhook URL (agent) |
| `slack-bot-token-agent` | Secret text | **이 설계서** | Slack Bot Token (agent) |

---

## 2. Jenkins Pipeline 스크립트

### 2.1 CI/CD Pipeline — `Jenkinsfile-cicd`

#### 구성 방식 비교

| 방식 | 장점 | 단점 |
|---|---|---|
| **파라미터화된 단일 Jenkinsfile** | 1개 파일로 6개 모듈 관리, Jenkinsfile 변경 시 전체 반영 | 모듈별 빌드 경로를 동적 생성해야 함 |
| **모듈별 개별 Jenkinsfile** | 모듈별 독립 설정, 단순한 구조 | 6개 파일 관리, 공통 변경 시 6곳 수정 |

**권장: 파라미터화된 단일 Jenkinsfile**

API 모듈의 CI/CD Pipeline은 Stage 구조가 동일하고, 차이점은 모듈명과 JAR 경로뿐입니다. `MODULE_NAME` 파라미터로 모듈을 선택하면 빌드 명령과 JAR 경로가 자동으로 결정됩니다. batch의 `Jenkinsfile-cicd` 구조를 그대로 유지하면서 모듈 선택만 추가한 형태입니다.

> 배포 Pipeline(`Jenkinsfile-deploy`)은 모듈별 시크릿 구성이 다르므로 별도로 평가합니다 (섹션 2.2 참조).

#### Pipeline 스크립트

파일 위치: `scripts/jenkins/api/Jenkinsfile-cicd`

```groovy
def git_repo_url = 'https://github.com/thswlsqls/tech-n-ai-backend.git'
def git_credentials_id = 'github-pat-tech-n-ai-backend'

// 모듈명 → JAR 소스 경로 매핑
def module_source_path = [
    'api-gateway'       : 'api/gateway/build/libs',
    'api-emerging-tech' : 'api/emerging-tech/build/libs',
    'api-auth'          : 'api/auth/build/libs',
    'api-chatbot'       : 'api/chatbot/build/libs',
    'api-bookmark'      : 'api/bookmark/build/libs',
    'api-agent'         : 'api/agent/build/libs'
]

pipeline {
    agent any

    parameters {
        choice(
            name: 'MODULE_NAME',
            choices: [
                'api-gateway',
                'api-emerging-tech',
                'api-auth',
                'api-chatbot',
                'api-bookmark',
                'api-agent'
            ],
            description: '빌드 대상 API 모듈'
        )
        choice(name: 'BRANCH', choices: ['main', 'dev', 'beta'], description: '빌드 대상 브랜치')
    }

    environment {
        GIT_REPO_URL = "${git_repo_url}"
        GIT_CREDENTIALS_ID = "${git_credentials_id}"
        BUILD_DIR = "${JENKINS_HOME}/builds"
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

                    sh """
                        chmod +x ./gradlew
                        ./gradlew -x test :${params.MODULE_NAME}:clean :${params.MODULE_NAME}:build --info
                    """
                }
            }
        }

        stage('Archive & Link') {
            steps {
                script {
                    def jarName = "${params.MODULE_NAME}-0.0.1-SNAPSHOT.jar"
                    def sourcePath = "${module_source_path[params.MODULE_NAME]}/${jarName}"
                    def targetPath = "${env.BUILD_DIR}/${params.MODULE_NAME}-${env.buildTimestamp}.jar"
                    def linkPath = "${env.BUILD_DIR}/${params.MODULE_NAME}.jar"

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
            echo "Build SUCCESS - Module: ${params.MODULE_NAME}, Branch: ${params.BRANCH}, Commit: ${env.commitHash}"
        }
        failure {
            echo "Build FAILED - Module: ${params.MODULE_NAME}, Branch: ${params.BRANCH}"
        }
    }
}
```

#### 주요 설계 결정

| 항목 | 선택 | 이유 |
|---|---|---|
| 파라미터화된 단일 Jenkinsfile | `MODULE_NAME` choice parameter | 6개 모듈의 Stage 구조가 동일. 공통 변경 시 1곳만 수정하면 됨 |
| `module_source_path` Map | Groovy Map으로 모듈별 JAR 경로 관리 | Gradle 모듈명(`api-auth`)과 디렉토리 구조(`api/auth/`)의 불일치를 명시적 매핑으로 해결 |
| `agent any` | batch와 동일 | 로컬 단일 노드 환경 |
| `deleteDir()` | batch와 동일 | 이전 빌드 잔여물 방지 |
| 심볼릭 링크 | `ln -sf` | 배포 Pipeline에서 고정 경로(`${JENKINS_HOME}/builds/${MODULE_NAME}.jar`)로 참조 |

#### batch `Jenkinsfile-cicd`와의 차이점

| 항목 | batch | API |
|---|---|---|
| 모듈 지정 | `project_name` 변수 하드코딩 | `MODULE_NAME` choice parameter |
| JAR 경로 | 단일 경로 (`batch/source/build/libs/`) | `module_source_path` Map에서 동적 결정 |
| JAR 이름 | `batch-source-0.0.1-SNAPSHOT.jar` 하드코딩 | `${params.MODULE_NAME}-0.0.1-SNAPSHOT.jar` 동적 생성 |
| 나머지 구조 | — | **동일** (Prepare → Checkout → Build → Archive & Link) |

---

### 2.2 배포 Pipeline — `Jenkinsfile-deploy`

#### 구성 방식 비교

| 방식 | 장점 | 단점 |
|---|---|---|
| **파라미터화된 단일 Jenkinsfile** | 1개 파일 관리 | 모듈별 `withCredentials` 블록이 복잡한 조건 분기가 됨 |
| **모듈별 개별 Jenkinsfile** | 모듈별 시크릿 명확, 단순한 구조 | 6개 파일 관리 |

**권장: 파라미터화된 단일 Jenkinsfile**

모듈별 시크릿 구성이 다르지만, Groovy의 `switch`문으로 모듈별 `withCredentials` 바인딩을 관리할 수 있습니다. 6개의 개별 파일을 관리하는 것보다 Stage 구조(Validate → Stop → Deploy → Health Check)를 한 곳에서 유지하는 편이 변경 관리에 유리합니다.

#### 프로세스 관리 전략 비교

| 방식 | 장점 | 단점 |
|---|---|---|
| **PID 파일 기반** (`kill $(cat pid-file)`) | 프로세스 식별 정확, 다른 프로세스 오종료 불가 | PID 파일 관리 필요 (생성/삭제/유효성 검증) |
| **프로세스명 기반** (`pkill -f`) | PID 파일 관리 불필요, 간단 | 패턴 매칭 오류 시 다른 프로세스 종료 위험 |

**권장: PID 파일 기반**

API 모듈은 동일 JAR 이름 패턴(`api-*.jar`)을 사용하므로, `pkill -f` 패턴 매칭이 의도치 않은 프로세스를 종료할 위험이 있습니다. PID 파일은 `${JENKINS_HOME}/pids/{module-name}.pid`에 저장하여 프로세스를 정확히 식별합니다.

#### 모듈별 Credential 바인딩

각 모듈의 배포 시 `withCredentials`로 주입되는 환경변수 목록입니다:

**공통 (전체 모듈)**

| Credential ID | 환경변수 |
|---|---|
| `redis-password` | `REDIS_PASSWORD` |

**api-gateway**

| Credential ID | 환경변수 |
|---|---|
| `jwt-secret-key` | `JWT_SECRET_KEY` |

**api-emerging-tech**

| Credential ID | 환경변수 |
|---|---|
| `openai-api-key` | `OPENAI_API_KEY` |
| `emerging-tech-internal-api-key` | `EMERGING_TECH_INTERNAL_API_KEY` |
| `mongodb-atlas-connection-string` | `MONGODB_ATLAS_CONNECTION_STRING` |

**api-auth**

| Credential ID | 환경변수 |
|---|---|
| `mysql-user` | `MYSQL_USER` |
| `mysql-password` | `MYSQL_PASSWORD` |
| `mail-password-auth` | `MAIL_PASSWORD` |

**api-chatbot**

| Credential ID | 환경변수 |
|---|---|
| `openai-api-key` | `OPENAI_API_KEY` |
| `cohere-api-key-chatbot` | `COHERE_API_KEY` |
| `google-search-api-key-chatbot` | `GOOGLE_SEARCH_API_KEY` |
| `google-search-engine-id-chatbot` | `GOOGLE_SEARCH_ENGINE_ID` |
| `mysql-user` | `MYSQL_USER` |
| `mysql-password` | `MYSQL_PASSWORD` |
| `mongodb-atlas-connection-string` | `MONGODB_ATLAS_CONNECTION_STRING` |

**api-bookmark**

| Credential ID | 환경변수 |
|---|---|
| `mysql-user` | `MYSQL_USER` |
| `mysql-password` | `MYSQL_PASSWORD` |
| `mongodb-atlas-connection-string` | `MONGODB_ATLAS_CONNECTION_STRING` |

**api-agent**

| Credential ID | 환경변수 |
|---|---|
| `openai-api-key` | `OPENAI_API_KEY` |
| `emerging-tech-internal-api-key` | `EMERGING_TECH_INTERNAL_API_KEY` |
| `slack-webhook-url-agent` | `SLACK_WEBHOOK_URL` |
| `slack-bot-token-agent` | `SLACK_BOT_TOKEN` |
| `mysql-user` | `MYSQL_USER` |
| `mysql-password` | `MYSQL_PASSWORD` |
| `mongodb-atlas-connection-string` | `MONGODB_ATLAS_CONNECTION_STRING` |

#### 모듈별 포트 및 Actuator 경로

| 모듈 | 포트 | Health Check URL |
|---|---|---|
| `api-gateway` | 8081 | `http://localhost:8081/actuator/health` |
| `api-emerging-tech` | 8082 | `http://localhost:8082/actuator/health` |
| `api-auth` | 8083 | `http://localhost:8083/actuator/health` |
| `api-chatbot` | 8084 | `http://localhost:8084/actuator/health` |
| `api-bookmark` | 8085 | `http://localhost:8085/actuator/health` |
| `api-agent` | 8086 | `http://localhost:8086/actuator/health` |

#### Pipeline 스크립트

파일 위치: `scripts/jenkins/api/Jenkinsfile-deploy`

```groovy
// 모듈별 포트 매핑
def module_ports = [
    'api-gateway'       : '8081',
    'api-emerging-tech' : '8082',
    'api-auth'          : '8083',
    'api-chatbot'       : '8084',
    'api-bookmark'      : '8085',
    'api-agent'         : '8086'
]

// 모듈별 Credential 바인딩 정의
def module_credentials = [
    'api-gateway': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'jwt-secret-key', variable: 'JWT_SECRET_KEY')
    ],
    'api-emerging-tech': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY'),
        string(credentialsId: 'emerging-tech-internal-api-key', variable: 'EMERGING_TECH_INTERNAL_API_KEY'),
        string(credentialsId: 'mongodb-atlas-connection-string', variable: 'MONGODB_ATLAS_CONNECTION_STRING')
    ],
    'api-auth': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'mysql-user', variable: 'MYSQL_USER'),
        string(credentialsId: 'mysql-password', variable: 'MYSQL_PASSWORD'),
        string(credentialsId: 'mail-password-auth', variable: 'MAIL_PASSWORD')
    ],
    'api-chatbot': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY'),
        string(credentialsId: 'cohere-api-key-chatbot', variable: 'COHERE_API_KEY'),
        string(credentialsId: 'google-search-api-key-chatbot', variable: 'GOOGLE_SEARCH_API_KEY'),
        string(credentialsId: 'google-search-engine-id-chatbot', variable: 'GOOGLE_SEARCH_ENGINE_ID'),
        string(credentialsId: 'mysql-user', variable: 'MYSQL_USER'),
        string(credentialsId: 'mysql-password', variable: 'MYSQL_PASSWORD'),
        string(credentialsId: 'mongodb-atlas-connection-string', variable: 'MONGODB_ATLAS_CONNECTION_STRING')
    ],
    'api-bookmark': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'mysql-user', variable: 'MYSQL_USER'),
        string(credentialsId: 'mysql-password', variable: 'MYSQL_PASSWORD'),
        string(credentialsId: 'mongodb-atlas-connection-string', variable: 'MONGODB_ATLAS_CONNECTION_STRING')
    ],
    'api-agent': [
        string(credentialsId: 'redis-password', variable: 'REDIS_PASSWORD'),
        string(credentialsId: 'openai-api-key', variable: 'OPENAI_API_KEY'),
        string(credentialsId: 'emerging-tech-internal-api-key', variable: 'EMERGING_TECH_INTERNAL_API_KEY'),
        string(credentialsId: 'slack-webhook-url-agent', variable: 'SLACK_WEBHOOK_URL'),
        string(credentialsId: 'slack-bot-token-agent', variable: 'SLACK_BOT_TOKEN'),
        string(credentialsId: 'mysql-user', variable: 'MYSQL_USER'),
        string(credentialsId: 'mysql-password', variable: 'MYSQL_PASSWORD'),
        string(credentialsId: 'mongodb-atlas-connection-string', variable: 'MONGODB_ATLAS_CONNECTION_STRING')
    ]
]

pipeline {
    agent any

    parameters {
        choice(
            name: 'MODULE_NAME',
            choices: [
                'api-gateway',
                'api-emerging-tech',
                'api-auth',
                'api-chatbot',
                'api-bookmark',
                'api-agent'
            ],
            description: '배포 대상 API 모듈'
        )
        choice(
            name: 'SPRING_PROFILE',
            choices: ['local', 'dev', 'beta', 'prod'],
            description: 'Spring 프로파일'
        )
    }

    environment {
        BUILD_DIR = "${JENKINS_HOME}/builds"
        PID_DIR = "${JENKINS_HOME}/pids"
        LOG_DIR = "${HOME}/workspace/tech-n-ai/tech-n-ai-backend/logs"
    }

    stages {
        stage('Validate JAR') {
            steps {
                script {
                    env.jarPath = "${env.BUILD_DIR}/${params.MODULE_NAME}.jar"
                    env.pidFile = "${env.PID_DIR}/${params.MODULE_NAME}.pid"
                    env.modulePort = module_ports[params.MODULE_NAME]

                    if (!fileExists("${env.jarPath}")) {
                        error "JAR file not found: ${env.jarPath}. Run CI/CD Pipeline first."
                    }
                    echo "JAR validated: ${env.jarPath}"
                }
            }
        }

        stage('Stop Running Process') {
            steps {
                script {
                    sh "mkdir -p ${env.PID_DIR}"

                    if (fileExists("${env.pidFile}")) {
                        def pid = sh(returnStdout: true, script: "cat ${env.pidFile}").trim()

                        def isRunning = sh(
                            returnStatus: true,
                            script: "kill -0 ${pid} 2>/dev/null"
                        )

                        if (isRunning == 0) {
                            echo "Stopping process ${pid} (${params.MODULE_NAME})..."

                            // Graceful shutdown via SIGTERM
                            sh "kill ${pid}"

                            // 최대 30초 대기
                            def stopped = false
                            for (int i = 0; i < 30; i++) {
                                def check = sh(
                                    returnStatus: true,
                                    script: "kill -0 ${pid} 2>/dev/null"
                                )
                                if (check != 0) {
                                    stopped = true
                                    break
                                }
                                sleep(time: 1, unit: 'SECONDS')
                            }

                            if (!stopped) {
                                echo "Process ${pid} did not stop gracefully. Sending SIGKILL..."
                                sh "kill -9 ${pid} 2>/dev/null || true"
                            }

                            echo "Process ${pid} stopped."
                        } else {
                            echo "PID file exists but process ${pid} is not running. Cleaning up stale PID file."
                        }

                        sh "rm -f ${env.pidFile}"
                    } else {
                        echo "No PID file found. No process to stop."
                    }

                    // 포트 점유 확인 — PID 파일 없이 프로세스가 실행 중인 경우 대비
                    def portCheck = sh(
                        returnStatus: true,
                        script: "lsof -ti:${env.modulePort} > /dev/null 2>&1"
                    )
                    if (portCheck == 0) {
                        echo "WARNING: Port ${env.modulePort} is still in use. Attempting to free it..."
                        sh "lsof -ti:${env.modulePort} | xargs kill -9 2>/dev/null || true"
                        sleep(time: 2, unit: 'SECONDS')
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials(module_credentials[params.MODULE_NAME]) {
                    script {
                        sh "mkdir -p ${env.LOG_DIR}"

                        // nohup으로 백그라운드 실행, PID 기록
                        sh """
                            nohup java \
                                -Dspring.profiles.active=${params.SPRING_PROFILE} \
                                -jar ${env.jarPath} \
                                > ${env.LOG_DIR}/${params.MODULE_NAME}.log 2>&1 &
                            echo \$! > ${env.pidFile}
                        """

                        env.deployedPid = sh(returnStdout: true, script: "cat ${env.pidFile}").trim()
                        echo "Started ${params.MODULE_NAME} (PID: ${env.deployedPid})"
                    }
                }
            }
        }

        stage('Health Check') {
            steps {
                script {
                    def healthUrl = "http://localhost:${env.modulePort}/actuator/health"
                    def maxRetries = 30
                    def retryInterval = 5
                    def healthy = false

                    echo "Waiting for ${params.MODULE_NAME} to be ready at ${healthUrl}..."

                    for (int i = 1; i <= maxRetries; i++) {
                        def exitCode = sh(
                            returnStatus: true,
                            script: "curl -sf ${healthUrl} > /dev/null 2>&1"
                        )

                        if (exitCode == 0) {
                            healthy = true
                            echo "Health check passed on attempt ${i}/${maxRetries}"
                            break
                        }

                        echo "Attempt ${i}/${maxRetries} failed. Retrying in ${retryInterval}s..."
                        sleep(time: retryInterval, unit: 'SECONDS')
                    }

                    if (!healthy) {
                        // 헬스체크 실패 시 프로세스 정리
                        echo "Health check failed after ${maxRetries * retryInterval}s. Stopping deployed process..."
                        sh "kill ${env.deployedPid} 2>/dev/null || true"
                        sh "rm -f ${env.pidFile}"
                        error "Health check failed for ${params.MODULE_NAME} at ${healthUrl}"
                    }

                    echo "Deploy SUCCESS - Module: ${params.MODULE_NAME}, Port: ${env.modulePort}, PID: ${env.deployedPid}"
                }
            }
        }
    }

    post {
        failure {
            echo "=== DEPLOY FAILURE ==="
            echo "Module: ${params.MODULE_NAME}"
            echo "Profile: ${params.SPRING_PROFILE}"
            echo "JAR: ${env.jarPath}"
            echo "Check log: ${env.LOG_DIR}/${params.MODULE_NAME}.log"
        }
    }
}
```

#### 주요 설계 결정

| 항목 | 선택 | 이유 |
|---|---|---|
| PID 파일 기반 프로세스 관리 | `${JENKINS_HOME}/pids/{module}.pid` | 동일 JAR 이름 패턴의 여러 프로세스를 정확히 식별. `pkill -f` 패턴 오매칭 위험 방지 |
| PID 파일 읽기 | `sh 'cat ...'` (readFile 대신) | Jenkins `readFile` step은 workspace 상대 경로 전용. PID 파일은 `${JENKINS_HOME}/pids/`(절대 경로)에 있으므로 `sh` + `cat` 사용 |
| Graceful shutdown → SIGKILL | SIGTERM 후 30초 대기, 타임아웃 시 SIGKILL | Spring Boot 4.x는 graceful shutdown이 기본 활성화(`server.shutdown=graceful`). SIGTERM 시 신규 요청 수신을 중단하고 진행 중 요청 완료를 대기함. 기본 타임아웃(`spring.lifecycle.timeout-per-shutdown-phase`)이 30초이므로 대기 시간을 30초로 맞춤 |
| 포트 점유 확인 | `lsof -ti:{port}` fallback | PID 파일 없이 프로세스가 실행 중인 비정상 상태 대비 |
| Health Check | curl + retry 루프 (30회 × 5초 = 최대 150초) | Spring Boot 기동 시간 고려. chatbot/agent는 langchain4j 초기화로 기동이 느릴 수 있음 |
| 헬스체크 실패 시 프로세스 정리 | 배포 실패 판정 후 프로세스 종료 | 비정상 기동된 프로세스가 포트를 점유한 채 남아있는 것을 방지 |
| `withCredentials` 블록 | Deploy stage에서만 적용 | 시크릿 노출 범위를 최소화. `nohup` 실행 시 환경변수로 주입되어 프로세스에 전달됨 |
| 로그 출력 경로 | `${HOME}/workspace/tech-n-ai/tech-n-ai-backend/logs/` | 기존 Promtail 수집 경로와 일치 (APM 설계서와 연계) |
| 롤백 전략 | 미포함 | 로컬 테스트 환경에서는 이전 JAR로의 자동 롤백보다 실패 원인 파악 후 수동 재배포가 적합. 심볼릭 링크 대상이 항상 최신 빌드이므로, 이전 빌드로 롤백 시 `ln -sf`로 심볼릭 링크만 변경 후 재배포하면 됨 |

#### batch `Jenkinsfile-scheduler`와의 차이점

| 항목 | batch Jenkinsfile-scheduler | API Jenkinsfile-deploy |
|---|---|---|
| 용도 | Spring Batch Job 실행 (단명 프로세스) | API 서버 배포 (상주 프로세스) |
| 실행 방식 | `java -jar` (포그라운드, 완료까지 대기) | `nohup java -jar &` (백그라운드) |
| cron 트리거 | 있음 (`triggers { cron(...) }`) | 없음 (수동 또는 CI/CD 후 연동) |
| 기존 프로세스 처리 | 불필요 (매회 새 프로세스) | **Stop → Deploy** (기존 프로세스 종료 필수) |
| Health Check | exit code 기반 | **HTTP 기반** (`/actuator/health`) |
| PID 관리 | 불필요 | **PID 파일 기반** |
| 시크릿 주입 | 1개 (`GITHUB_TOKEN`) | **모듈별 다수** (최대 8개) |

---

## 3. Jenkins 아이템 등록 및 운영 가이드

### 3.1 CI/CD 아이템 등록

6개 API 모듈이 하나의 파라미터화된 Jenkinsfile을 공유하므로, **Jenkins Item도 1개**만 생성합니다.

#### Step 1 — Item 생성

1. Jenkins 대시보드 좌측 메뉴 > **새로운 Item** 클릭
2. 설정:
   - **Enter an item name**: `api-cicd`
   - 아이템 타입 목록에서 **Pipeline** 선택
3. **OK** 클릭 → 구성 화면으로 이동

#### Step 2 — General 설정

| 설정 | 값 | 설명 |
|---|---|---|
| Description | `API 모듈 CI/CD — GitHub 소스 체크아웃 후 선택한 모듈의 JAR 빌드 및 아카이브` | |
| **오래된 빌드 삭제** | 체크 | |
| ↳ 보관할 최대 빌드 수 | `30` | 6개 모듈이 공유하므로 batch(`10`)보다 넉넉히 설정 |
| ↳ 보관 기간 (일) | `30` | |

#### Step 3 — Pipeline 설정

| 필드 | 값 | 설명 |
|---|---|---|
| Definition | **Pipeline script from SCM** | |
| SCM | **Git** | |
| Repository URL | `https://github.com/thswlsqls/tech-n-ai-backend.git` | |
| Credentials | `github-pat-tech-n-ai-backend` | 기존 Credential 재사용 |
| Branch Specifier | `*/main` | |
| Script Path | `scripts/jenkins/api/Jenkinsfile-cicd` | |
| Lightweight checkout | 체크 (기본값) | |

**저장** 클릭.

#### Step 4 — 초기화 빌드 및 테스트

**초기화 빌드:**

1. `api-cicd` 아이템 클릭
2. 좌측 메뉴 **지금 빌드** 클릭
3. 첫 빌드 완료 후 **Build with Parameters** 메뉴가 활성화되는지 확인

> 초기화 빌드는 `parameters` 블록 인식을 위한 것입니다. 기본값(`MODULE_NAME`: `api-gateway`, `BRANCH`: `main`)으로 실행됩니다.

**파라미터 지정 테스트:**

1. 좌측 메뉴 **Build with Parameters** 클릭
2. 파라미터 선택:

   | 파라미터 | 선택 값 |
   |---|---|
   | `MODULE_NAME` | `api-gateway` |
   | `BRANCH` | `main` |

3. **빌드하기** 클릭

**결과 확인:**

좌측 **Build History**에서 빌드 번호 클릭 > **Console Output**:

| 로그 메시지 | 의미 |
|---|---|
| `BUILD SUCCESSFUL` | Gradle 빌드 성공 |
| `JAR archived: /Users/m1/.jenkins/builds/api-gateway-{timestamp}.jar` | JAR 복사 완료 |
| `Symlink updated: /Users/m1/.jenkins/builds/api-gateway.jar` | 심볼릭 링크 갱신 |
| `Build SUCCESS - Module: api-gateway, Branch: main, Commit: abc1234` | Pipeline 정상 종료 |

---

### 3.2 배포 아이템 등록

배포 Pipeline도 파라미터화된 단일 Jenkinsfile을 사용하므로, **Jenkins Item 1개**만 생성합니다.

#### Step 1 — Item 생성

1. Jenkins 대시보드 > **새로운 Item**
2. 설정:
   - **Enter an item name**: `api-deploy`
   - 아이템 타입: **Pipeline**
3. **OK** 클릭

#### Step 2 — General 설정

| 설정 | 값 | 설명 |
|---|---|---|
| Description | `API 모듈 배포 — 선택한 모듈의 기존 프로세스 종료 후 새 JAR로 기동` | |
| **오래된 빌드 삭제** | 체크 | |
| ↳ 보관할 최대 빌드 수 | `30` | |
| ↳ 보관 기간 (일) | `30` | |

#### Step 3 — Pipeline 설정

| 필드 | 값 | 설명 |
|---|---|---|
| Definition | **Pipeline script from SCM** | |
| SCM | **Git** | |
| Repository URL | `https://github.com/thswlsqls/tech-n-ai-backend.git` | |
| Credentials | `github-pat-tech-n-ai-backend` | |
| Branch Specifier | `*/main` | |
| Script Path | `scripts/jenkins/api/Jenkinsfile-deploy` | |
| Lightweight checkout | 체크 (기본값) | |

**저장** 클릭.

#### Step 4 — 초기화 빌드 및 테스트

**사전 조건:**

`api-cicd`에서 배포 대상 모듈을 **1회 이상 빌드**하여 심볼릭 링크가 존재해야 합니다.

```bash
ls -la ~/.jenkins/builds/api-gateway.jar
```

**초기화 빌드:**

1. 좌측 메뉴 **지금 빌드** 클릭 (기본값: `api-gateway`, `local`)
2. 빌드 완료 후 **Build with Parameters** 활성화 확인

**파라미터 지정 테스트:**

1. **Build with Parameters** 클릭
2. 파라미터 선택:

   | 파라미터 | 선택 값 |
   |---|---|
   | `MODULE_NAME` | `api-gateway` |
   | `SPRING_PROFILE` | `local` |

3. **빌드하기** 클릭

**결과 확인:**

| 로그 메시지 | 의미 |
|---|---|
| `JAR validated: /Users/m1/.jenkins/builds/api-gateway.jar` | JAR 파일 존재 확인 |
| `No PID file found. No process to stop.` | 첫 배포 (이전 프로세스 없음) |
| `Started api-gateway (PID: 12345)` | 프로세스 기동 |
| `Health check passed on attempt 3/30` | 헬스체크 성공 |
| `Deploy SUCCESS - Module: api-gateway, Port: 8081, PID: 12345` | 배포 성공 |

**실패 시 확인 포인트:**

| 로그 메시지 | 원인 | 조치 |
|---|---|---|
| `JAR file not found` | CI/CD 미실행 | `api-cicd`에서 해당 모듈 빌드 먼저 실행 |
| `Port 8081 is still in use` | 포트 점유 프로세스 존재 | 로그 확인 후 수동으로 `lsof -ti:8081 \| xargs kill` |
| `Health check failed` | 기동 실패 또는 타임아웃 | `~/workspace/tech-n-ai/tech-n-ai-backend/logs/api-gateway.log` 확인 |

---

### 3.3 운영 관리

#### 전체 Jenkins Item 현황

| Jenkins Item | Pipeline 타입 | Script Path | 등록 시점 |
|---|---|---|---|
| `batch-source-cicd` | Pipeline from SCM | `scripts/jenkins/batch/Jenkinsfile-cicd` | batch 설계서 |
| `batch-source-schedule-scraper` | Pipeline from SCM | `scripts/jenkins/batch/Jenkinsfile-scheduler` | batch 설계서 |
| `batch-source-schedule-rss` | Pipeline from SCM | `scripts/jenkins/batch/Jenkinsfile-scheduler` | batch 설계서 |
| `batch-source-schedule-github` | Pipeline from SCM | `scripts/jenkins/batch/Jenkinsfile-scheduler` | batch 설계서 |
| `api-cicd` | Pipeline from SCM | `scripts/jenkins/api/Jenkinsfile-cicd` | **이 설계서** |
| `api-deploy` | Pipeline from SCM | `scripts/jenkins/api/Jenkinsfile-deploy` | **이 설계서** |

#### 빌드 이력 보존 정책

| Item | 최대 빌드 수 | 보존 기간 | 근거 |
|---|---|---|---|
| `batch-source-cicd` | 10 | 30일 | 수동 실행, 빈도 낮음 |
| `batch-source-schedule-*` | 30 | 7일 | 자동 실행, 빈도 높음 |
| `api-cicd` | 30 | 30일 | 6개 모듈 공유, 수동 실행 |
| `api-deploy` | 30 | 30일 | 6개 모듈 공유, 수동 실행 |

#### 빌드 실패 시 로그 확인

1. Jenkins 대시보드에서 해당 Item 클릭
2. **Build History**에서 실패한 빌드 번호(빨간색 아이콘) 클릭
3. **Console Output** 클릭하여 전체 로그 확인
4. 배포 실패의 경우 추가로 애플리케이션 로그 확인:
   ```bash
   tail -100 ~/workspace/tech-n-ai/tech-n-ai-backend/logs/{module-name}.log
   ```

#### JAR 파일 정리

CI/CD Pipeline이 타임스탬프 포함 JAR을 `${JENKINS_HOME}/builds/`에 누적합니다. 디스크 공간 관리를 위해 주기적으로 정리합니다.

**수동 정리:**

```bash
# 보존 대상 확인 (심볼릭 링크가 가리키는 최신 JAR)
ls -la ~/.jenkins/builds/api-*.jar

# 7일 이상 된 타임스탬프 JAR 삭제 (심볼릭 링크는 제외)
find ~/.jenkins/builds/ -name "api-*-[0-9]*.jar" -mtime +7 -exec rm {} \;
```

> 심볼릭 링크(`api-gateway.jar` 등)는 삭제하지 않습니다. `find`의 `-name` 패턴이 타임스탬프(`-[0-9]*.jar`)를 포함하는 파일만 대상으로 하므로 심볼릭 링크는 안전합니다.

#### 배포 순서 가이드

전체 API 모듈을 한꺼번에 배포할 때는 다음 순서를 권장합니다:

| 순서 | 모듈 | 이유 |
|---|---|---|
| 1 | `api-auth` | 독립적. 인증 서비스가 먼저 기동되어야 다른 서비스의 인증 토큰 발급 가능 |
| 2 | `api-emerging-tech` | 독립적. agent가 내부 API로 호출 |
| 3 | `api-bookmark` | CQRS 동기화용 Kafka consumer 기동 |
| 4 | `api-chatbot` | agent와 Aurora 스키마 공유 (`chatbot`). chatbot 먼저 기동 |
| 5 | `api-agent` | chatbot과 동일 Aurora 스키마. emerging-tech 내부 API 호출 |
| 6 | `api-gateway` | **마지막** — 모든 하위 서비스가 기동된 후 트래픽 라우팅 시작 |

> **Gateway를 마지막에 배포하는 이유**: Gateway는 기동 즉시 하위 서비스로 트래픽을 라우팅합니다. 하위 서비스가 아직 기동되지 않은 상태에서 Gateway가 먼저 기동되면, 사용자 요청이 `503 Service Unavailable`로 응답됩니다.

> **chatbot과 agent의 순서**: 두 모듈이 동일한 Aurora 스키마(`chatbot`)를 사용합니다. 스키마 마이그레이션이 포함된 배포에서는 두 모듈의 호환성을 확인한 후 순차 배포합니다.

#### PID 파일 관리

PID 파일은 `${JENKINS_HOME}/pids/` 디렉토리에 저장됩니다:

```bash
# 전체 PID 파일 확인
ls -la ~/.jenkins/pids/

# 특정 모듈의 프로세스 상태 확인
cat ~/.jenkins/pids/api-gateway.pid | xargs ps -p
```

비정상 상태(PID 파일은 있으나 프로세스 없음)의 경우 배포 Pipeline이 자동으로 stale PID 파일을 정리합니다. 수동 정리가 필요한 경우:

```bash
rm ~/.jenkins/pids/api-gateway.pid
```

#### 전체 모듈 상태 확인

모든 API 모듈의 기동 상태를 한 번에 확인하는 명령:

```bash
for port in 8081 8082 8083 8084 8085 8086; do
    status=$(curl -sf http://localhost:${port}/actuator/health 2>/dev/null && echo "UP" || echo "DOWN")
    echo "Port ${port}: ${status}"
done
```
