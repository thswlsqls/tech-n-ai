#!/bin/bash
# =============================================================================
# tech-n-ai-backend tmux 개발 환경 스크립트
# 세션: backend-session
# 윈도우: agent, auth, bookmark, chatbot, emerging, gateway
# 각 윈도우: claude-pane (좌) | build-pane (우) — 수직 분할
# =============================================================================

SESSION="backend-session"
BASE_DIR="/Users/m1/workspace/tech-n-ai/tech-n-ai-backend"

# 기존 세션이 있으면 attach
if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "세션 '$SESSION'이(가) 이미 존재합니다. attach 합니다."
    tmux attach-session -t "$SESSION"
    exit 0
fi

# 모듈 목록 (윈도우 이름 : 모듈 디렉토리)
declare -a MODULES=(
    "agent:api/agent"
    "auth:api/auth"
    "bookmark:api/bookmark"
    "chatbot:api/chatbot"
    "emerging:api/emerging-tech"
    "gateway:api/gateway"
)

# --- 첫 번째 윈도우 생성 (세션 생성 시 함께 만들어짐) ---
FIRST_MODULE="${MODULES[0]}"
FIRST_NAME="${FIRST_MODULE%%:*}"
FIRST_DIR="${FIRST_MODULE##*:}"

tmux new-session -d -s "$SESSION" -n "$FIRST_NAME" -c "$BASE_DIR/$FIRST_DIR"
# 수직 분할: 좌=claude-pane, 우=build-pane
tmux split-window -h -t "$SESSION:$FIRST_NAME" -c "$BASE_DIR/$FIRST_DIR"
# pane 이름 지정 (select-pane -T)
tmux select-pane -t "$SESSION:$FIRST_NAME.0" -T "claude-pane"
tmux select-pane -t "$SESSION:$FIRST_NAME.1" -T "build-pane"
# claude-pane(좌측)으로 포커스
tmux select-pane -t "$SESSION:$FIRST_NAME.0"

# --- 나머지 윈도우 생성 ---
for i in "${!MODULES[@]}"; do
    # 첫 번째는 이미 생성됨
    [[ $i -eq 0 ]] && continue

    MODULE="${MODULES[$i]}"
    WIN_NAME="${MODULE%%:*}"
    MOD_DIR="${MODULE##*:}"

    tmux new-window -t "$SESSION" -n "$WIN_NAME" -c "$BASE_DIR/$MOD_DIR"
    tmux split-window -h -t "$SESSION:$WIN_NAME" -c "$BASE_DIR/$MOD_DIR"
    tmux select-pane -t "$SESSION:$WIN_NAME.0" -T "claude-pane"
    tmux select-pane -t "$SESSION:$WIN_NAME.1" -T "build-pane"
    tmux select-pane -t "$SESSION:$WIN_NAME.0"
done

# 첫 번째 윈도우로 이동
tmux select-window -t "$SESSION:agent"

# attach
tmux attach-session -t "$SESSION"
