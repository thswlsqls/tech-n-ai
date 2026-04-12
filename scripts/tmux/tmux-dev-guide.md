# tmux-backend.sh 활용 가이드

## 개요

`tmux-backend.sh`는 tech-n-ai-backend 개발을 위한 3개 윈도우(project, module, test) 구조의 tmux 환경을 자동 구성하는 스크립트이다.

## 세션 구조

```
backend-session
├── project  [0]  ← 인프라/프로젝트 상태 모니터링 (3-pane)
├── module   [1]  ← Claude Code + 모듈별 Gradle 실행 (7-pane)
└── test     [2]  ← 단위/통합 테스트 (2-pane)
```

### project-window (3-pane)

```
┌──────────────────┬──────────────────┐
│                  │ redis-pane       │
│ docker-compose   ├──────────────────┤
│ pane (50%)       │ git-pane         │
│                  │                  │
└──────────────────┴──────────────────┘
```

### module-window (claude + 모듈별 gradle)

```
┌──────────────┬──────────────────────┐
│              │ gradle-gateway-pane  │
│              ├──────────────────────┤
│              │ gradle-emerging-     │
│              │ tech-pane            │
│              ├──────────────────────┤
│ claude-pane  │ gradle-auth-pane     │
│ (35%)        ├──────────────────────┤ (65%)
│              │ gradle-chatbot-pane  │
│              ├──────────────────────┤
│              │ gradle-bookmark-pane │
│              ├──────────────────────┤
│              │ gradle-agent-pane    │
└──────────────┴──────────────────────┘
```

각 gradle-*-pane에서 해당 모듈의 `bootRun`이 자동 실행되어 로그를 실시간으로 확인할 수 있다.
특정 pane을 전체화면으로 확대하려면 `Ctrl-b z`를 사용한다.

### test-window (좌우 분할)

```
┌──────────────────┬──────────────────┐
│ unit-pane        │ integration-pane │
│                  │                  │
│ 단위 테스트      │ 통합 테스트      │
│                  │                  │
└──────────────────┴──────────────────┘
```

## 실행 방법

```bash
# 스크립트 실행 (세션 생성 + attach)
./scripts/tmux-backend.sh

# 이미 세션이 존재하면 자동으로 attach
./scripts/tmux-backend.sh
```

## 윈도우 간 이동

| 단축키 | 대상 윈도우 |
|--------|-------------|
| `Ctrl-b 0` | project |
| `Ctrl-b 1` | module |
| `Ctrl-b 2` | test |
| `Ctrl-b n` | 다음 윈도우 |
| `Ctrl-b p` | 이전 윈도우 |

## pane 간 이동

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b o` | 다음 pane으로 전환 |
| `Ctrl-b 방향키` | 방향으로 pane 이동 |
| `Ctrl-b z` | 현재 pane 전체화면 토글 (로그 확인 시 유용) |

## 개발 환경 시작 순서

스크립트 실행 후 아래 순서대로 인프라 → 빌드 → 서버를 기동한다.

### Step 1. 인프라 기동 (project-window, Ctrl-b 0)

```bash
# docker-compose-pane (좌측, pane 0)
cd ~/workspace/tech-n-ai/tech-n-ai-backend && docker compose down -v && docker compose up -d  # 백그라운드 기동
docker compose logs -f  # 로그 모니터링 (필요 시)

# redis-pane (우상, pane 1) — Redis는 로컬 머신에 설치됨
redis-server                   # Redis 서버 시작 (포그라운드)
# 또는 brew services start redis  # 백그라운드 서비스로 시작

# 별도 터미널에서 확인
redis-cli ping                 # 연결 확인 (PONG 응답)
redis-cli monitor              # 실시간 커맨드 모니터링

# git-pane (우하, pane 2)
git status                     # 작업 상태 확인
```

### Step 2. 전체 빌드 (별도 터미널 또는 module claude-pane)

```bash
./gradlew clean build          # 전체 프로젝트 빌드
```

### Step 3. API 서버 실행 (module-window, Ctrl-b 1)

스크립트 실행 시 각 gradle-*-pane에서 `bootRun`이 자동 실행된다.
수동으로 개별 모듈을 재시작하려면 해당 pane에서 `Ctrl-c` 후 다시 실행한다.

```bash
# 개별 모듈 재시작 예시 (해당 pane에서)
./gradlew :api-auth:bootRun
```

> **참고**: 특정 모듈만 실행하지 않으려면 해당 pane에서 `Ctrl-c`로 종료한다.

## 활용 예시

### 1. 인프라 모니터링 (project-window)

```bash
# project 윈도우 (Ctrl-b 0)
[docker-compose-pane] docker compose ps            # 컨테이너 상태 확인
[docker-compose-pane] docker compose logs -f        # 로그 모니터링
[redis-pane]          redis-server                  # Redis 서버 시작 (로컬 설치)
                      redis-cli monitor             # 별도 터미널에서 모니터링
[git-pane]            git status / git log --oneline
```

### 2. 모듈 개발 (module-window)

```bash
# module 윈도우 (Ctrl-b 1)
[claude-pane]              Claude Code로 코드 수정
[gradle-gateway-pane]      gateway 실시간 로그 (자동 실행)
[gradle-emerging-tech-pane] emerging-tech 실시간 로그 (자동 실행)
[gradle-auth-pane]         auth 실시간 로그 (자동 실행)
[gradle-chatbot-pane]      chatbot 실시간 로그 (자동 실행)
[gradle-bookmark-pane]     bookmark 실시간 로그 (자동 실행)
[gradle-agent-pane]        agent 실시간 로그 (자동 실행)

# 특정 모듈 로그를 전체화면으로 보기: 해당 pane에서 Ctrl-b z
# 모듈 재시작: 해당 pane에서 Ctrl-c → ./gradlew :api-auth:bootRun
```

### 3. 테스트 실행 (test-window)

```bash
# test 윈도우 (Ctrl-b 2)
[unit-pane]        ./gradlew :api-auth:test
[integration-pane] ./gradlew :api-auth:test --tests "com.tech.n.ai.api.auth.service.AuthServiceTest"
```

## API 모듈 참조

| 모듈 | 빌드 | 실행 | 포트 |
|------|------|------|------|
| gateway | `./gradlew :api-gateway:build` | `./gradlew :api-gateway:bootRun` | 8081 |
| emerging-tech | `./gradlew :api-emerging-tech:build` | `./gradlew :api-emerging-tech:bootRun` | 8082 |
| auth | `./gradlew :api-auth:build` | `./gradlew :api-auth:bootRun` | 8083 |
| chatbot | `./gradlew :api-chatbot:build` | `./gradlew :api-chatbot:bootRun` | 8084 |
| bookmark | `./gradlew :api-bookmark:build` | `./gradlew :api-bookmark:bootRun` | 8085 |
| agent | `./gradlew :api-agent:build` | `./gradlew :api-agent:bootRun` | 8086 |

### 빌드 명령어

```bash
# 전체 빌드
./gradlew clean build

# 단일 모듈 빌드
./gradlew :api-auth:build

# 단일 모듈 클린 빌드
./gradlew :api-auth:clean :api-auth:build

# 테스트 제외 빌드
./gradlew :api-auth:build -x test

# 단일 테스트 클래스 실행
./gradlew :api-auth:test --tests "com.tech.n.ai.api.auth.service.AuthServiceTest"
```

## 세션 관리

```bash
# 세션에서 분리 (세션은 백그라운드에서 유지)
Ctrl-b d

# 세션 다시 연결
tmux attach -t backend-session

# 세션 종료
tmux kill-session -t backend-session

# 모든 세션 목록 확인
tmux ls
```
