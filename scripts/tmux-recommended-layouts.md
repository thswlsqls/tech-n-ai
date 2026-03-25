
# 추천 tmux 레이아웃 구조

> 현재 구조(모듈별 윈도우 × claude/build pane) 외에 고려할 수 있는 대안 구조들

## 1. 역할 기반 구조 (Role-Based)

모듈이 아닌 **작업 역할** 기준으로 윈도우를 나누는 방식.

```
backend-session
├── code-window          # Claude Code 전용 (모듈 간 이동하며 코딩)
│   └── 단일 pane
├── build-window         # 빌드/테스트 전용
│   ├── pane-1: gradle build --continuous
│   └── pane-2: 테스트 실행
├── server-window        # 서비스 실행
│   ├── pane-1: gateway 실행
│   ├── pane-2: auth 실행
│   └── pane-3: 기타 서비스
├── log-window           # 로그 모니터링
│   ├── pane-1: gateway 로그
│   └── pane-2: 에러 로그 필터링
└── db-window            # DB 클라이언트
    ├── pane-1: MySQL CLI
    └── pane-2: MongoDB CLI
```

**장점**: 윈도우 수가 적어 전환이 단순하다. 한 윈도우에서 여러 서비스 상태를 한눈에 본다.
**단점**: 모듈별 컨텍스트가 섞인다. 특정 모듈에 집중하기 어렵다.
**적합**: 통합 테스트, 전체 서비스 기동 시 모니터링 목적.

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

## 3. 현재 구조 + 공용 윈도우 추가 (Hybrid)

현재 모듈별 구조를 유지하면서 공용 윈도우를 추가하는 방식. **가장 추천하는 구조.**

```
backend-session
├── overview-window      # 전체 상태 모니터링
│   ├── docker-pane      # docker compose 상태
│   ├── git-pane         # git 상태 확인
│   └── gradle-pane      # 전체 빌드
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

**장점**: 모듈별 독립성을 유지하면서 전체 프로젝트 상태를 한 곳에서 볼 수 있다.
**단점**: 윈도우 수가 7개로 약간 많다.
**적합**: 현재 프로젝트 구조에서 가장 실용적인 확장.

## 4. Frontend 확장 구조

현재 app/admin 외에 공통 작업용 윈도우를 추가하는 방식.

```
frontend-session
├── shared-window        # 공통 컴포넌트, 유틸리티
│   ├── claude-pane
│   └── storybook-pane   # 컴포넌트 미리보기
├── app-window
│   ├── claude-pane
│   └── dev-pane         # npm run dev (port 3000)
├── admin-window
│   ├── claude-pane
│   └── dev-pane         # npm run dev (port 3001)
└── test-window
    ├── lint-pane        # npm run lint
    └── test-pane        # 테스트 실행
```

**장점**: 공통 컴포넌트 작업과 앱별 작업을 분리할 수 있다.
**단점**: 윈도우가 4개로 늘어난다.

## 5. 멀티 세션 구조 (Multi-Session)

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
| 단일 모듈 집중 개발 | 현재 구조 (모듈별 윈도우) |
| 전체 서비스 기동 + 개발 | Hybrid (현재 + overview) |
| 통합 테스트 / 디버깅 | 역할 기반 |
| 공통 모듈 리팩토링 | 계층 기반 |
| 멀티 모니터 환경 | 멀티 세션 |
