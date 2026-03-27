# tmux-backend.sh 활용 가이드

## 개요

`tmux-backend.sh`는 tech-n-ai-backend 개발을 위한 3개 윈도우(project, module, test) 구조의 tmux 환경을 자동 구성하는 스크립트이다.

## 세션 구조

```
backend-session
├── project  [0]  ← 인프라/프로젝트 상태 모니터링 (4-pane)
├── module   [1]  ← Claude Code + Gradle 빌드 (2-pane)
└── test     [2]  ← 단위/통합 테스트 (2-pane)
```

### project-window (2×2 격자)

```
┌──────────────────┬──────────────────┐
│ docker-compose   │ redis            │
│ pane             │ pane             │
├──────────────────┼──────────────────┤
│ gradle           │ git              │
│ pane             │ pane             │
└──────────────────┴──────────────────┘
```

### module-window (좌우 분할)

```
┌──────────────────┬──────────────────┐
│ claude-pane      │ gradle-pane      │
│                  │                  │
│ Claude Code      │ 빌드/서버 실행   │
│ 코드 작업        │                  │
└──────────────────┴──────────────────┘
```

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

## 활용 예시

### 1. 인프라 모니터링 (project-window)

```
# project 윈도우 (Ctrl-b 0)
[docker-compose-pane] docker compose up
[redis-pane]          redis-cli monitor
[gradle-pane]         ./gradlew clean build
[git-pane]            git status / git log
```

### 2. 모듈 개발 (module-window)

```
# module 윈도우 (Ctrl-b 1)
[claude-pane]  Claude Code로 코드 수정
[gradle-pane]  ./gradlew :api-auth:build
               ./gradlew :api-gateway:bootRun
```

### 3. 테스트 실행 (test-window)

```
# test 윈도우 (Ctrl-b 2)
[unit-pane]        ./gradlew :api-auth:test
[integration-pane] ./gradlew :api-auth:test --tests "com.tech.n.ai.api.auth.service.AuthServiceTest"
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
