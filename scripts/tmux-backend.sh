#!/bin/bash
# =============================================================================
# tech-n-ai-backend tmux 개발 환경 스크립트
# 세션: backend-session
# 윈도우: project (4-pane), module (2-pane), test (2-pane)
# =============================================================================

SESSION="backend-session"
BASE_DIR="/Users/m1/workspace/tech-n-ai/tech-n-ai-backend"

# 기존 세션이 있으면 attach
if tmux has-session -t "$SESSION" 2>/dev/null; then
    echo "세션 '$SESSION'이(가) 이미 존재합니다. attach 합니다."
    tmux attach-session -t "$SESSION"
    exit 0
fi

# =============================================================================
# project-window: 2×2 격자 (docker-compose, redis, gradle, git)
# =============================================================================
tmux new-session -d -s "$SESSION" -n "project" -c "$BASE_DIR"

# 초기 pane(0)을 수평 분할 → 상(0), 하(1)
tmux split-window -v -t "$SESSION:project.0" -c "$BASE_DIR"
# 상단(0)을 수직 분할 → 좌상(0), 우상(1)
tmux split-window -h -t "$SESSION:project.0" -c "$BASE_DIR"
# 하단(2)을 수직 분할 → 좌하(2), 우하(3)
tmux split-window -h -t "$SESSION:project.2" -c "$BASE_DIR"

tmux select-pane -t "$SESSION:project.0" -T "docker-compose-pane"
tmux select-pane -t "$SESSION:project.1" -T "redis-pane"
tmux select-pane -t "$SESSION:project.2" -T "gradle-pane"
tmux select-pane -t "$SESSION:project.3" -T "git-pane"
tmux select-pane -t "$SESSION:project.0"

# =============================================================================
# module-window: 수평 분할 (claude-pane | gradle-pane)
# =============================================================================
tmux new-window -t "$SESSION" -n "module" -c "$BASE_DIR"
tmux split-window -h -t "$SESSION:module" -c "$BASE_DIR"
tmux select-pane -t "$SESSION:module.0" -T "claude-pane"
tmux select-pane -t "$SESSION:module.1" -T "gradle-pane"
tmux select-pane -t "$SESSION:module.0"

# =============================================================================
# test-window: 수평 분할 (unit-pane | integration-pane)
# =============================================================================
tmux new-window -t "$SESSION" -n "test" -c "$BASE_DIR"
tmux split-window -h -t "$SESSION:test" -c "$BASE_DIR"
tmux select-pane -t "$SESSION:test.0" -T "unit-pane"
tmux select-pane -t "$SESSION:test.1" -T "integration-pane"
tmux select-pane -t "$SESSION:test.0"

# 첫 번째 윈도우로 이동
tmux select-window -t "$SESSION:project"

# attach
tmux attach-session -t "$SESSION"
