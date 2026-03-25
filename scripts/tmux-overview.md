# tmux 소개 및 특징

> 참고 출처:
> - [tmux 공식 GitHub](https://github.com/tmux/tmux)
> - [tmux(1) OpenBSD man page](https://man.openbsd.org/tmux)
> - [tmux Wiki — Getting Started](https://github.com/tmux/tmux/wiki/Getting-Started)

## tmux란

tmux(terminal multiplexer)는 하나의 터미널에서 여러 터미널 세션을 생성, 관리, 전환할 수 있는 도구이다. BSD 라이선스로 배포되며, OpenBSD 프로젝트에서 시작되었다.

## 핵심 개념

tmux는 3단계 계층 구조로 동작한다.

```
Server
└── Session (세션)
    └── Window (윈도우)
        └── Pane (팬)
```

### Session (세션)
- tmux가 관리하는 최상위 단위이다.
- 터미널을 닫거나 SSH 연결이 끊겨도 서버 프로세스가 살아있는 한 세션은 유지된다.
- `tmux new-session -s name`으로 생성, `tmux attach -t name`으로 재접속한다.

### Window (윈도우)
- 세션 안에서 탭처럼 동작하는 독립적인 화면이다.
- 각 윈도우는 고유한 인덱스 번호를 가지며 `Ctrl-b 번호`로 전환한다.
- 하나의 윈도우는 하나 이상의 pane을 포함한다.

### Pane (팬)
- 윈도우를 분할하여 만든 개별 터미널 영역이다.
- 수직 분할(`Ctrl-b %`), 수평 분할(`Ctrl-b "`)이 가능하다.
- 각 pane은 독립적인 셸 프로세스를 실행한다.

## 주요 특징

### 1. 세션 영속성 (Session Persistence)
tmux의 가장 핵심적인 기능이다. 세션은 tmux 서버 프로세스에 의해 유지되므로, 클라이언트(터미널)가 종료되어도 세션 내의 모든 프로세스가 계속 실행된다. `detach`로 분리한 뒤 `attach`로 언제든 복귀할 수 있다.

### 2. 터미널 멀티플렉싱
하나의 터미널 연결 안에서 여러 셸을 동시에 운영한다. SSH 연결 하나로 여러 작업을 병렬 수행할 수 있어 원격 서버 작업에 특히 유용하다.

### 3. 스크립트 기반 자동화
tmux 명령어는 모두 CLI에서 실행 가능하므로, 셸 스크립트로 세션/윈도우/팬 구성을 자동화할 수 있다. 이를 통해 개발 환경 레이아웃을 코드로 관리하고 재현할 수 있다.

### 4. 설정 커스터마이징
`~/.tmux.conf` 파일을 통해 키 바인딩, 상태 바, 색상, 마우스 동작 등을 세밀하게 설정할 수 있다.

## 장점

| 항목 | 설명 |
|------|------|
| **세션 영속성** | 터미널 종료, SSH 끊김에도 작업 상태가 보존된다 |
| **리소스 효율** | GUI 터미널 탭보다 메모리 사용이 적다 |
| **환경 재현성** | 스크립트로 동일한 레이아웃을 반복 생성할 수 있다 |
| **원격 작업** | SSH 하나로 복수의 셸을 운영할 수 있다 |
| **동시 모니터링** | pane 분할로 로그, 빌드, 코드를 한 화면에 배치할 수 있다 |
| **팀 표준화** | 스크립트 공유로 팀원 간 동일한 개발 환경을 구성할 수 있다 |

## 단점

| 항목 | 설명 |
|------|------|
| **학습 곡선** | 키 바인딩과 session/window/pane 개념을 익히는 데 초기 비용이 든다 |
| **마우스 제한** | 기본적으로 마우스가 비활성화되어 있다 (`set -g mouse on`으로 활성화 가능) |
| **복사/붙여넣기** | OS 클립보드와 통합이 기본 제공되지 않는다 (macOS: `reattach-to-user-namespace` 또는 tmux 3.2+ `set -s copy-command` 설정 필요) |
| **화면 공간** | pane이 많을수록 각 영역이 좁아져 가독성이 떨어진다 |
| **IDE 기능 부재** | 자동 완성, 디버거, 코드 네비게이션 등은 tmux가 제공하지 않는다 |
| **설정 관리** | `.tmux.conf`가 개인별로 다를 경우 스크립트 동작이 달라질 수 있다 |

## 필수 단축키

> 기본 prefix: `Ctrl-b`

### 세션 관리

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b d` | 세션 분리 (detach) |
| `Ctrl-b s` | 세션 목록 (세션 간 전환) |
| `Ctrl-b $` | 현재 세션 이름 변경 |

### 윈도우 관리

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b c` | 새 윈도우 생성 |
| `Ctrl-b 0~9` | 번호로 윈도우 전환 |
| `Ctrl-b n` / `Ctrl-b p` | 다음 / 이전 윈도우 |
| `Ctrl-b w` | 윈도우 목록 (선택 전환) |
| `Ctrl-b ,` | 현재 윈도우 이름 변경 |
| `Ctrl-b &` | 현재 윈도우 종료 |

### Pane 관리

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b %` | 수직 분할 (좌우) |
| `Ctrl-b "` | 수평 분할 (상하) |
| `Ctrl-b o` | 다음 pane으로 이동 |
| `Ctrl-b 방향키` | 방향으로 pane 이동 |
| `Ctrl-b z` | 현재 pane 전체화면 토글 (zoom) |
| `Ctrl-b x` | 현재 pane 종료 |
| `Ctrl-b {` / `Ctrl-b }` | pane 위치 교환 |

### 기타

| 단축키 | 동작 |
|--------|------|
| `Ctrl-b [` | 스크롤(복사) 모드 진입 (`q`로 종료) |
| `Ctrl-b :` | 명령어 프롬프트 |
| `Ctrl-b ?` | 키 바인딩 목록 |
| `Ctrl-b t` | 시계 표시 |

## 자주 사용하는 CLI 명령어

```bash
# 세션 관리
tmux new-session -s name        # 이름 지정 세션 생성
tmux attach -t name             # 세션 연결
tmux detach                     # 세션 분리 (단축키: Ctrl-b d)
tmux ls                         # 세션 목록
tmux kill-session -t name       # 세션 종료
tmux kill-server                # tmux 서버 전체 종료

# 윈도우/팬 (스크립트에서 사용)
tmux new-window -t session -n name -c /path
tmux split-window -h -t session:window -c /path   # 수직 분할
tmux split-window -v -t session:window -c /path   # 수평 분할
tmux select-window -t session:window
tmux select-pane -t session:window.index
tmux select-pane -T "pane-title"                   # pane 이름 지정
```

## 권장 .tmux.conf 설정 (선택)

```bash
# 마우스 지원 활성화
set -g mouse on

# 256 색상 지원
set -g default-terminal "screen-256color"

# pane 번호 표시 시간 연장
set -g display-panes-time 2000

# 윈도우 번호 1부터 시작
set -g base-index 1
setw -g pane-base-index 1

# 상태 바에 세션/윈도우 이름 표시
set -g status-left "[#S] "
set -g status-right "%H:%M"
```
