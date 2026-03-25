# tmux-backend.sh 활용 가이드

## 개요

`tmux-backend.sh`는 tech-n-ai-backend의 6개 API 모듈에 대해 동일한 레이아웃의 tmux 환경을 자동 구성하는 스크립트이다.

## 세션 구조

```
backend-session
├── agent     [0]  ← api/agent       (port 8086)
├── auth      [1]  ← api/auth        (port 8083)
├── bookmark  [2]  ← api/bookmark    (port 8085)
├── chatbot   [3]  ← api/chatbot     (port 8084)
├── emerging  [4]  ← api/emerging-tech (port 8082)
└── gateway   [5]  ← api/gateway     (port 8081)
```

각 윈도우는 수직 분할(50:50)되어 좌측 claude-pane, 우측 build-pane으로 구성된다.

```
┌─────────────────┬─────────────────┐
│  claude-pane    │  build-pane     │
│                 │                 │
│  Claude Code    │  빌드/테스트    │
│  코드 작업      │  서버 실행      │
│                 │                 │
└─────────────────┴─────────────────┘
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
| `Ctrl-b 0` | agent |
| `Ctrl-b 1` | auth |
| `Ctrl-b 2` | bookmark |
| `Ctrl-b 3` | chatbot |
| `Ctrl-b 4` | emerging |
| `Ctrl-b 5` | gateway |
| `Ctrl-b n` | 다음 윈도우 |
| `Ctrl-b p` | 이전 윈도우 |

## pane 간 이동

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b o` | claude-pane ↔ build-pane 전환 |
| `Ctrl-b z` | 현재 pane 전체화면 토글 (로그 확인 시 유용) |

## 활용 예시

### 1. 모듈별 Claude Code + 빌드 병렬 작업

```
# agent 윈도우 (Ctrl-b 0)
[claude-pane] Claude Code로 agent 모듈 코드 수정
[build-pane]  ./gradlew :api-agent:build
```

### 2. 모듈별 서버 실행 + 로그 모니터링

```
# gateway 윈도우 (Ctrl-b 5)
[claude-pane] Claude Code 대기 또는 코드 확인
[build-pane]  ./gradlew :api-gateway:bootRun

# auth 윈도우 (Ctrl-b 1)
[build-pane]  ./gradlew :api-auth:bootRun
```

### 3. 특정 모듈 테스트 실행

```
# auth 윈도우 (Ctrl-b 1)
[build-pane]  ./gradlew :api-auth:test --tests "com.tech.n.ai.api.auth.service.AuthServiceTest"
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

## 모듈 추가/제거 시 수정 방법

스크립트 상단의 `MODULES` 배열을 편집한다.

```bash
declare -a MODULES=(
    "agent:api/agent"
    "auth:api/auth"
    "bookmark:api/bookmark"
    "chatbot:api/chatbot"
    "emerging:api/emerging-tech"
    "gateway:api/gateway"
    # 새 모듈 추가 예시:
    # "newmodule:api/new-module"
)
```

형식: `"윈도우이름:모듈디렉토리상대경로"`
