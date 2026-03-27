
# 추천 tmux 레이아웃 구조

> 현재 구조(project/module/test 윈도우) 외에 고려할 수 있는 대안 구조들

## 1. 모듈별 구조 (Module-Per-Window)

각 API 모듈마다 독립 윈도우를 두는 방식.

```
backend-session
├── agent-window
│   ├── claude-pane
│   └── build-pane
├── auth-window
│   ├── claude-pane
│   └── build-pane
├── bookmark-window
│   ├── claude-pane
│   └── build-pane
├── chatbot-window
│   ├── claude-pane
│   └── build-pane
├── emerging-window
│   ├── claude-pane
│   └── build-pane
└── gateway-window
    ├── claude-pane
    └── build-pane
```

**장점**: 모듈별 컨텍스트가 완전히 분리된다. 모듈 전환 시 build 상태가 유지된다.
**단점**: 윈도우 수가 6개로 많다. 인프라 상태를 별도로 확인해야 한다.
**적합**: 여러 모듈을 동시에 서버 실행하며 개발하는 경우.

## 2. 계층 기반 구조 (Layer-Based)

아키텍처 계층(API / Datasource / Infra) 기준으로 분리하는 방식.

```
backend-session
├── api-window
│   ├── claude-pane
│   └── build-pane      # API 모듈 빌드
├── datasource-window
│   ├── claude-pane
│   └── build-pane      # datasource 모듈 빌드
├── infra-window
│   ├── claude-pane
│   └── build-pane      # Docker, config 관련
└── test-window
    ├── unit-pane        # 단위 테스트
    └── integration-pane # 통합 테스트
```

**장점**: 의존성 방향(API → Datasource → Common)에 맞는 작업 흐름.
**단점**: API 모듈이 6개로 많아 하나의 윈도우로 묶으면 혼잡할 수 있다.
**적합**: 공통 모듈(common, datasource) 변경이 잦은 리팩토링 시기.

## 3. 멀티 세션 구조 (Multi-Session)

하나의 세션 대신 목적별로 여러 세션을 운영하는 방식.

```
coding-session           # Claude Code 작업 전용
├── backend-window
└── frontend-window

running-session          # 서비스 실행 전용
├── gateway-window
├── auth-window
└── frontend-dev-window

monitoring-session       # 모니터링 전용
├── log-window
├── docker-window
└── db-window
```

**장점**: 목적별로 세션을 분리하여 `tmux switch-client`로 전환. 세션 단위 detach/attach가 가능.
**단점**: 세션 간 전환이 윈도우 전환보다 한 단계 더 복잡하다.
**적합**: 모니터 2대 이상에서 각 모니터에 세션을 할당하는 경우.

## 구조 선택 가이드

| 작업 패턴 | 추천 구조 |
|-----------|-----------|
| 일반 개발 (코딩 + 빌드 + 테스트) | 현재 구조 (project/module/test) |
| 여러 모듈 동시 서버 실행 | 모듈별 |
| 공통 모듈 리팩토링 | 계층 기반 |
| 멀티 모니터 환경 | 멀티 세션 |
