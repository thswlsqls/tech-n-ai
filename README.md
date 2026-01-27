# Tech N AI Demo

## 개요

tech-n-ai는 개발자 대회 정보와 최신 IT 테크 뉴스를 수집하고 제공하는 RESTful API 서버입니다. **CQRS 패턴 기반의 마이크로서비스 아키텍처**로 설계되어 있으며, **langchain4j RAG 기반 멀티턴 챗봇**을 핵심 기능으로 제공합니다. Spring Boot 4.0.1과 Java 21을 기반으로 구축되었습니다.

## 프로젝트 기획 의도 (해결하려고 하는 문제)

### 문제

기존 LLM(대규모 언어 모델)은 학습 데이터에 포함된 정보만을 기반으로 응답을 생성하기 때문에, 최신 개발자 대회 정보나 최근 IT 테크 뉴스와 같은 실시간 정보를 제공할 수 없었습니다. 특히 다음과 같은 한계가 있었습니다:

- **최신 정보 부재**: LLM의 학습 데이터는 특정 시점까지의 정보로 제한되어 있어, 최신 대회 일정이나 최근 뉴스 기사를 알 수 없음
- **비정규 데이터 접근 불가**: 대회 정보나 뉴스 기사 제공자의 일부 구조화되지 않은 비정규 데이터를 LLM이 직접 검색하거나 활용하는지 확인할 수 없음
- **동적 정보 업데이트 불가**: 새로운 대회나 뉴스가 발생해도 LLM의 지식 베이스에 자동으로 반영되지 않음

### 해결

이 프로젝트는 **RAG(Retrieval-Augmented Generation)** 기반 아키텍처와 **AI Agent 자동화 시스템**을 통해 이러한 문제를 해결합니다:

1. **🤖 AI Agent 기반 자동 정보 수집 시스템**
   - **LangChain4j 기반 자율 Agent**: 자연어 목표만 입력하면 필요한 작업을 자동으로 판단하고 실행
   - **GitHub API 통합**: OpenAI, Anthropic, Google, Meta의 SDK 릴리스를 자동 추적
   - **웹 스크래핑**: 공식 블로그의 최신 AI 업데이트 자동 수집
   - **중복 방지 및 검증**: 기존 데이터와 비교하여 중복 없이 새로운 정보만 저장
   - **자동 승인 워크플로우**: Draft → Review → Publish 프로세스 자동화
   - **6시간 주기 스케줄링**: 정기적으로 최신 AI 업데이트 자동 확인 및 저장

2. **최신 정보 수집 서버 구축**
   - 개발자 대회 정보 수집: RSS 피드, 웹 스크래핑, 외부 API를 통한 대회 정보 수집 (`api-contest` 모듈)
   - 최신 IT 테크 뉴스 수집: RSS 피드 파싱을 통한 뉴스 기사 수집 (`api-news` 모듈)
   - **AI 서비스 업데이트 추적**: AI Agent를 통한 빅테크 AI 서비스 업데이트 자동 수집 (`api-agent`, `api-ai-update` 모듈)
   - 정기적인 배치 작업을 통한 최신 정보 자동 업데이트

3. **비정규 데이터 임베딩 및 RAG 구축**
   - MongoDB Atlas에 저장된 비정규 데이터(ContestDocument, NewsArticleDocument, ArchiveDocument, AiUpdateDocument)를 OpenAI text-embedding-3-small 모델로 임베딩
   - MongoDB Atlas Vector Search를 활용한 벡터 검색 인덱스 구축 (1536차원, cosine similarity)
   - langchain4j RAG 파이프라인을 통한 지식 검색 및 응답 생성
   - 사용자 질문에 대한 관련 문서 검색 후, 검색된 컨텍스트를 기반으로 OpenAI GPT-4o-mini가 최신 정보를 포함한 응답 생성

4. **실시간 정보 제공**
   - 수집된 최신 대회 정보, 뉴스 기사, AI 업데이트를 MongoDB Atlas에 저장
   - 사용자 질문 시 Vector Search를 통해 관련 최신 정보를 실시간으로 검색
   - 검색된 최신 정보를 컨텍스트로 제공하여 LLM이 정확하고 최신의 응답을 생성

이를 통해 사용자는 자연어로 최신 개발자 대회 정보, IT 테크 뉴스, AI 서비스 업데이트를 검색하고 질문할 수 있으며, LLM이 학습 데이터에 없는 최신 정보도 정확하게 제공할 수 있습니다. 특히 **AI Agent 시스템**은 인간의 개입 없이 자율적으로 최신 AI 트렌드를 추적하고 정보를 업데이트합니다.


### 핵심 기능

- **🤖 LangChain4j 기반 자율 AI Agent 시스템**: 자연어 목표 입력만으로 빅테크 AI 서비스 업데이트를 자동 추적하고 수집하는 완전 자율 Agent
- **🌟 langchain4j RAG 기반 멀티턴 챗봇**: MongoDB Atlas Vector Search와 OpenAI GPT-4o-mini를 활용한 지식 검색 챗봇
- **AI 업데이트 자동화 파이프라인**: GitHub Release 추적, 웹 스크래핑, 중복 검증, 포스팅 자동화 (6시간 주기)
- **CQRS 패턴 기반 아키텍처**: Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 분리
- **Kafka 기반 실시간 동기화**: 이벤트 기반 CQRS 동기화 (1초 이내 목표)
- **OAuth 2.0 인증**: Google, Naver, Kakao 소셜 로그인 지원
- **API Gateway**: 중앙화된 라우팅 및 인증 처리
- **개발자 대회 정보 수집 및 제공**: 해커톤, 알고리즘 대회, 오픈소스 대회 등의 정보를 수집하고 API로 제공
- **최신 IT 테크 뉴스 수집 및 제공**: 최신 IT 기술 뉴스를 수집하고 API로 제공
- **사용자 아카이브 기능**: 사용자가 관심 있는 대회/뉴스를 개인 아카이브에 저장 및 관리


## 시스템 아키텍처

### 전체 시스템 아키텍처

![System Architecture Diagram](contents/system-architecture-diagram.png)

### CQRS 패턴 기반 아키텍처

이 프로젝트는 **CQRS (Command Query Responsibility Segregation) 패턴**을 적용하여 읽기와 쓰기 작업을 완전히 분리합니다.

#### Command Side (쓰기 전용)
- **데이터베이스**: Amazon Aurora MySQL 3.x
- **역할**: 모든 쓰기 작업 (CREATE, UPDATE, DELETE) 수행
- **특징**:
  - TSID (Time-Sorted Unique Identifier) Primary Key 전략
  - 높은 정규화 수준 (최소 3NF)
  - Soft Delete 지원
  - 히스토리 테이블을 통한 변경 이력 추적

#### Query Side (읽기 전용)
- **데이터베이스**: MongoDB Atlas 7.0+
- **역할**: 모든 읽기 작업 (SELECT) 수행
- **특징**:
  - 읽기 최적화된 비정규화 구조
  - ESR 규칙을 준수한 인덱스 설계
  - 프로젝션을 통한 네트워크 트래픽 최소화
  - **Vector Search 지원** (RAG 챗봇용)

#### CQRS 패턴 데이터 플로우

![CQRS Pattern Diagram](contents/cqrs-pattern-diagram.png)

#### Kafka 기반 실시간 동기화

**Apache Kafka**를 통한 이벤트 기반 CQRS 동기화 메커니즘:

- **Event Publisher**: Command Side의 모든 쓰기 작업을 Kafka 이벤트로 발행
- **Event Consumer**: Kafka 이벤트를 수신하여 Query Side (MongoDB Atlas)에 동기화
- **멱등성 보장**: Redis 기반 중복 처리 방지 (TTL: 7일)
- **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)

![CQRS Kafka Sync Flow](contents/cqrs-kafka-sync-flow.png)

![Kafka Events Diagram](contents/kafka-events-diagram.png)

자세한 CQRS 및 Kafka 동기화 설계는 다음 문서를 참고하세요:
- [CQRS Kafka 동기화 설계서](docs/step11/cqrs-kafka-sync-design.md)

## 🌟 langchain4j RAG 기반 멀티턴 챗봇

### 개요

**langchain4j RAG 기반 멀티턴 챗봇**은 이 프로젝트의 핵심 기능으로, MongoDB Atlas Vector Search와 OpenAI GPT-4o-mini를 활용하여 사용자가 자연어로 대회 정보, 뉴스 기사, 자신의 아카이브를 검색하고 질문할 수 있도록 합니다.

### 주요 특징

- **RAG (Retrieval-Augmented Generation)**: MongoDB Atlas Vector Search를 통한 지식 검색
- **멀티턴 대화 히스토리 관리**: 세션 기반 대화 컨텍스트 유지
- **OpenAI GPT-4o-mini**: 비용 최적화된 LLM (128K 컨텍스트 윈도우)
- **OpenAI text-embedding-3-small**: LLM과 동일한 Provider 사용으로 통합성 최적화 ($0.02 per 1M tokens)
- **토큰 기반 메모리 관리**: TokenWindowChatMemory를 통한 효율적인 컨텍스트 관리
- **의도 분류**: RAG 필요 여부 자동 판단
- **비용 통제**: 토큰 사용량 추적 및 제한

### RAG 파이프라인 아키텍처

![Chatbot LLM RAG Pipeline](contents/api-chatbot/chatbot-llm-rag-pipeline.png)

### 전체 시스템 아키텍처

![Overall System Architecture](contents/api-chatbot/overall-system-architecture.png)

### 데이터 소스

챗봇은 다음 MongoDB Atlas 컬렉션의 벡터 검색을 지원합니다:

- **ContestDocument**: 개발자 대회 정보 (`title + description + metadata.tags`)
- **NewsArticleDocument**: IT 테크 뉴스 기사 (`title + summary + content`)
- **ArchiveDocument**: 사용자 아카이브 항목 (`itemTitle + itemSummary + tag + memo`, 사용자별 필터링)

### API 엔드포인트

#### 챗봇 대화 API

- `POST /api/v1/chatbot/chat` - 챗봇 대화 (RAG 기반 응답 생성)
- `GET /api/v1/chatbot/sessions` - 대화 세션 목록 조회
- `GET /api/v1/chatbot/sessions/{sessionId}` - 대화 세션 상세 조회
- `DELETE /api/v1/chatbot/sessions/{sessionId}` - 대화 세션 삭제

### 기술 스택

- **langchain4j**: 0.35.0 (RAG 프레임워크)
- **MongoDB Atlas Vector Search**: 벡터 검색 인덱스 (1536차원, cosine similarity)
- **OpenAI GPT-4o-mini**: LLM Provider (기본 선택)
- **OpenAI text-embedding-3-small**: Embedding Model (기본 선택, LLM과 동일 Provider)

자세한 RAG 챗봇 설계는 다음 문서를 참고하세요:
- [langchain4j RAG 기반 챗봇 설계서](docs/step12/rag-chatbot-design.md)

## 🤖 AI Agent 자동화 시스템

### 개요

**AI Agent 자동화 시스템**은 LangChain4j를 기반으로 설계된 완전 자율 Agent로, 빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta)의 최신 업데이트를 자동으로 추적하고 수집합니다. 인간의 개입 없이 자연어 목표(Goal)만 입력하면 필요한 작업을 자동으로 판단하고 실행하는 혁신적인 시스템입니다.

### 3단계 자동화 파이프라인

AI 업데이트 자동화 시스템은 3단계로 구성된 파이프라인을 통해 동작합니다:

**Phase 1: 데이터 수집 (batch-source)**
- Spring Batch Jobs를 통한 GitHub Release 및 Web Scraping
- 주기적으로 OpenAI, Anthropic, Google, Meta의 업데이트 정보 수집

**Phase 2: 저장 및 관리 (api-ai-update)**
- MongoDB에 AiUpdateDocument 저장
- REST API를 통한 목록/상세 조회, 검색, 상태 관리
- Draft/Published 상태 관리

**Phase 3: AI Agent (api-agent)**
- LangChain4j Agent의 자율 실행
- Tool 선택 및 중복 검증
- GitHub API, Web Scraper, Search, Create/Publish 기능 통합
- 자연어 목표 기반 자율 의사결정

전체 시스템 아키텍처는 [시스템 아키텍처](#시스템-아키텍처) 섹션을 참고하세요.

### Agent 동작 방식

#### 입력: 자연어 목표 (Goal)
```
"OpenAI와 Anthropic의 최신 업데이트를 확인하고 중요한 것만 포스팅해줘"
```

#### Agent의 자율 추론 및 실행
```
1. Tool 선택: fetchGitHubReleases("openai", "openai-python")
   → 결과: v1.50.0 릴리스 발견

2. Tool 선택: searchAiUpdates("openai-python v1.50.0", "OPENAI")
   → 결과: 기존 데이터 없음 (중복 아님)

3. Tool 선택: createDraftPost(...)
   → 결과: Draft 포스트 생성 (ID: 12345)

4. Tool 선택: sendSlackNotification(...)
   → 결과: Slack 알림 전송 완료

5. Tool 선택: fetchGitHubReleases("anthropics", "anthropic-sdk-python")
   → 결과: 새 릴리스 없음

최종 결과: "OpenAI SDK v1.50.0 초안 생성 완료, Anthropic 업데이트 없음"
```

### 주요 특징

#### 1. 완전 자율 실행
- **자연어 이해**: "최신 업데이트 확인해줘"와 같은 자연어 목표를 이해
- **Tool 자동 선택**: 목표 달성을 위해 필요한 Tool을 자동으로 선택하고 실행
- **상황 판단**: 중복 확인, 중요도 판단, 오류 처리 등을 자율적으로 수행

#### 2. LangChain4j Tools
Agent가 사용할 수 있는 6가지 Tool:

| Tool | 설명 |
|------|------|
| `fetchGitHubReleases` | GitHub 저장소의 최신 릴리스 목록 조회 |
| `scrapeWebPage` | 웹 페이지 크롤링 (robots.txt 준수) |
| `searchAiUpdates` | 저장된 AI 업데이트 검색 (중복 확인) |
| `createDraftPost` | DRAFT 상태 포스트 생성 |
| `publishPost` | 포스트 승인 (PUBLISHED) |
| `sendSlackNotification` | Slack 알림 전송 |

#### 3. 스케줄 자동 실행
- **주기**: 6시간마다 자동 실행
- **목표**: "OpenAI, Anthropic, Google, Meta의 최신 업데이트 확인 및 포스팅"
- **알림**: 실행 결과를 Slack으로 자동 알림

#### 4. 대상 AI 서비스

| Provider | GitHub Repository | 웹 소스 |
|----------|-------------------|---------|
| OpenAI | openai/openai-python | https://openai.com/blog |
| Anthropic | anthropics/anthropic-sdk-python | https://www.anthropic.com/news |
| Google | google/generative-ai-python | https://blog.google/technology/ai/ |
| Meta | facebookresearch/llama | https://ai.meta.com/blog/ |

### 시스템 아키텍처

![AI Agent System Architecture](contents/api-agent/sytem-architecture.png)

AI Agent는 REST API 또는 Scheduler를 통해 트리거되며, LangChain4j AiServices를 활용하여 OpenAI GPT-4o-mini와 통신합니다. Agent는 6개의 Tool을 사용하여 GitHub API, 웹 페이지, api-ai-update API, Slack과 상호작용하며, 최종적으로 MongoDB에 데이터를 저장합니다.

ai-update API는 batch-source와 api-agent로부터 데이터를 수신하여 MongoDB에 저장하고, 공개 API를 통해 사용자에게 AI 업데이트 정보를 제공합니다. Slack 알림 기능도 통합되어 있습니다.

### API 엔드포인트

#### Agent 실행 API
```http
POST /api/v1/agent/run
X-Internal-Api-Key: {api-key}
Content-Type: application/json

{
  "goal": "OpenAI, Anthropic, Google, Meta의 최신 업데이트를 확인하고 중요한 것만 포스팅해줘"
}
```

#### Response
```json
{
  "code": "2000",
  "message": "성공",
  "data": {
    "success": true,
    "summary": "OpenAI SDK v1.50.0 초안 포스트 생성 완료...",
    "toolCallCount": 8,
    "postsCreated": 1,
    "executionTimeMs": 15234,
    "errors": []
  }
}
```

#### AI Update API (api-ai-update)
```http
# 공개 API
GET /api/v1/ai-update                    # 목록 조회
GET /api/v1/ai-update/{id}               # 상세 조회
GET /api/v1/ai-update/search             # 검색

# 내부 API (X-Internal-Api-Key 필요)
POST /api/v1/ai-update/internal          # 단건 생성
POST /api/v1/ai-update/internal/batch    # 배치 생성
POST /api/v1/ai-update/{id}/approve      # 승인
POST /api/v1/ai-update/{id}/reject       # 거부
```

### 기술 스택

- **LangChain4j**: 0.35.0 (AI Agent 프레임워크)
- **OpenAI GPT-4o-mini**: Agent의 LLM (temperature: 0.3, max-tokens: 4096)
- **Spring Batch**: GitHub Release 및 Web Scraping Job
- **MongoDB**: AI 업데이트 저장소
- **Jsoup**: HTML 파싱 및 웹 스크래핑
- **OpenFeign**: GitHub API 및 내부 API 클라이언트

### 환경 변수

| 변수명 | 설명 | 필수 |
|--------|------|------|
| `OPENAI_API_KEY` | Agent용 OpenAI API 키 | Yes |
| `AI_UPDATE_INTERNAL_API_KEY` | ai-update 및 Agent API 인증 키 | Yes |
| `AGENT_SCHEDULER_ENABLED` | 스케줄러 활성화 (true/false) | No |
| `GITHUB_TOKEN` | GitHub API 토큰 (Rate Limit 완화) | No |

### 디렉토리 구조

```
api/
├── agent/                    # AI Agent 모듈 (Port 8087)
│   ├── agent/
│   │   ├── AiUpdateAgent.java
│   │   ├── AiUpdateAgentImpl.java
│   │   ├── AgentAssistant.java
│   │   └── AgentExecutionResult.java
│   ├── tool/
│   │   ├── AiUpdateAgentTools.java
│   │   └── adapter/
│   │       ├── GitHubToolAdapter.java
│   │       ├── ScraperToolAdapter.java
│   │       ├── AiUpdateToolAdapter.java
│   │       └── SlackToolAdapter.java
│   ├── controller/
│   │   └── AgentController.java
│   └── scheduler/
│       └── AiUpdateAgentScheduler.java
│
└── ai-update/               # AI Update API 모듈 (Port 8088)
    ├── controller/
    │   └── AiUpdateController.java
    ├── facade/
    │   └── AiUpdateFacade.java
    └── service/
        ├── AiUpdateService.java
        └── AiUpdateServiceImpl.java
```

자세한 AI Agent 설계는 [참고 문서](#참고-문서) 섹션의 "AI Agent 자동화 파이프라인 설계서"를 참고하세요.

## API Gateway

### 개요

**API Gateway**는 Spring Cloud Gateway 기반의 중앙화된 API Gateway 서버로, 모든 외부 요청을 중앙에서 관리하고 적절한 백엔드 API 서버로 라우팅하는 역할을 수행합니다. JWT 토큰 기반 인증, CORS 정책 관리, 연결 풀 최적화 등의 기능을 제공합니다.

### 주요 기능

- **URI 기반 라우팅**: 요청 URI 경로를 기준으로 5개 API 서버(auth, archive, contest, news, chatbot)로 요청 전달
- **JWT 토큰 검증**: `common-security` 모듈의 `JwtTokenProvider`를 활용한 JWT 토큰 검증
- **인증 필요/불필요 경로 구분**: 공개 API와 인증 필요 API 자동 구분
- **사용자 정보 헤더 주입**: 검증 성공 시 사용자 정보를 헤더에 주입하여 백엔드 서버로 전달
- **Global CORS 설정**: 모든 경로에 대한 CORS 정책 적용, 환경별 차별화
- **연결 풀 최적화**: Reactor Netty 연결 풀 설정으로 Connection reset by peer 에러 방지
- **공통 예외 처리**: `WebExceptionHandler`를 통한 Reactive 기반 예외 처리

### 인프라 아키텍처

```
Client (웹 브라우저, 모바일 앱)
  ↓ HTTP/HTTPS
ALB (AWS Application Load Balancer, 600초 timeout)
  ↓
API Gateway (Spring Cloud Gateway)
  ├── JWT 인증 필터
  ├── CORS 처리
  └── 라우팅
  ↓
  ├─ /api/v1/auth/** → @api/auth (인증 불필요)
  ├─ /api/v1/archive/** → @api/archive (인증 필요)
  ├─ /api/v1/contest/** → @api/contest (공개 API)
  ├─ /api/v1/news/** → @api/news (공개 API)
  └─ /api/v1/chatbot/** → @api/chatbot (인증 필요)
```

### 라우팅 규칙

| 경로 패턴 | 대상 서버 | 인증 필요 | 설명 |
|----------|---------|---------|------|
| `/api/v1/auth/**` | `@api/auth` | ❌ | 인증 서버 (회원가입, 로그인, 토큰 갱신 등) |
| `/api/v1/archive/**` | `@api/archive` | ✅ | 사용자 아카이브 관리 API |
| `/api/v1/contest/**` | `@api/contest` | ❌ | 대회 정보 조회 API (공개) |
| `/api/v1/news/**` | `@api/news` | ❌ | 뉴스 정보 조회 API (공개) |
| `/api/v1/chatbot/**` | `@api/chatbot` | ✅ | RAG 기반 챗봇 API |

### 요청 처리 흐름

**인증이 필요한 요청 처리**:
1. Client → ALB → Gateway: 요청 수신
2. Gateway: 라우팅 규칙 매칭 (`/api/v1/archive/**`)
3. Gateway: JWT 인증 필터 실행
   - JWT 토큰 추출 (Authorization 헤더)
   - JWT 토큰 검증 (`JwtTokenProvider.validateToken`)
   - 사용자 정보 추출 및 헤더 주입 (`x-user-id`, `x-user-email`, `x-user-role`)
4. Gateway → Archive 서버: 인증된 요청 전달 (사용자 정보 헤더 포함)
5. Archive 서버 → Gateway: API 응답
6. Gateway → ALB → Client: 최종 응답 (CORS 헤더 포함)

**인증이 불필요한 요청 처리**:
1. Client → ALB → Gateway: 요청 수신
2. Gateway: 라우팅 규칙 매칭 (`/api/v1/contest/**`)
3. Gateway: 인증 필터 우회 (공개 API)
4. Gateway → Contest 서버: 요청 전달
5. Contest 서버 → Gateway: API 응답
6. Gateway → ALB → Client: 최종 응답

### Gateway 모듈 구조

```
api/gateway/
├── GatewayApplication.java                    # Spring Boot 메인 클래스
├── config/
│   └── GatewayConfig.java                     # Spring Cloud Gateway 라우팅 설정
├── filter/
│   └── JwtAuthenticationGatewayFilter.java    # JWT 인증 Gateway Filter
├── common/
│   └── exception/
│       └── ApiGatewayExceptionHandler.java    # 공통 예외 처리
└── src/main/resources/
    ├── application.yml                        # 기본 설정 (라우팅, 연결 풀, CORS)
    ├── application-local.yml                  # 로컬 환경 설정
    ├── application-dev.yml                    # 개발 환경 설정
    ├── application-beta.yml                   # 베타 환경 설정
    └── application-prod.yml                  # 운영 환경 설정
```

### 기술 스택

- **Spring Cloud Gateway**: API Gateway 프레임워크 (Netty 기반)
- **Reactor Netty**: 비동기 네트워크 프레임워크
- **Java**: 21
- **Spring Boot**: 4.0.1
- **Spring Cloud**: 2025.1.0

자세한 Gateway 설계는 다음 문서를 참고하세요:
- [Gateway 설계서](docs/step14/gateway-design.md)
- [Gateway 구현 계획](docs/step14/gateway-implementation-plan.md)
- [Gateway API 모듈 README](api/gateway/README.md)

## OAuth 2.0 인증 시스템

### 개요

**OAuth 2.0 인증 시스템**은 Google, Naver, Kakao 소셜 로그인을 지원하며, 기존 JWT 토큰 기반 인증 시스템과 완전히 통합됩니다.

### 지원 Provider

- **Google OAuth 2.0**: Google 계정을 통한 로그인
- **Naver OAuth 2.0**: 네이버 계정을 통한 로그인
- **Kakao OAuth 2.0**: 카카오 계정을 통한 로그인

### OAuth 로그인 플로우

#### OAuth 로그인 시작

![OAuth Login Start](contents/api-auth/oauth-login-start.png)

#### OAuth 로그인 콜백

![OAuth Login Callback Flow](contents/api-auth/oauth-login-callback-flow.png)

### 인증/인가 플로우

![Authentication Authorization Flow](contents/api-auth/authentication-authorization-flow.png)


### 주요 인증 플로우

#### 회원가입 플로우

![Signup Flow](contents/api-auth/signup-flow.png)

#### 로그인 플로우

![Login Flow](contents/api-auth/login-flow.png)

#### 토큰 갱신 플로우

![Token Refresh Flow](contents/api-auth/token-refresh-flow.png)

#### 비밀번호 재설정 요청 플로우

![Password Reset Request Flow](contents/api-auth/password-reset-request-flow.png)


### State 파라미터 관리

OAuth 2.0 인증 플로우에서 **CSRF 공격 방지**를 위한 State 파라미터는 **Redis**에 저장됩니다:

- **Key 형식**: `oauth:state:{state_value}`
- **Value**: Provider 이름 (예: "GOOGLE", "NAVER", "KAKAO")
- **TTL**: 10분 (자동 만료)
- **일회성 사용**: 검증 완료 후 즉시 삭제

자세한 OAuth 구현은 다음 문서를 참고하세요:
- [OAuth Provider 구현 가이드](docs/step6/oauth-provider-implementation-guide.md)
- [Spring Security 인증/인가 설계 가이드](docs/step6/spring-security-auth-design-guide.md)

## 기술 스택

### 언어 및 프레임워크
- **Java**: 21
- **Spring Boot**: 4.0.1
- **Spring Cloud**: 2025.1.0
- **Gradle**: Groovy DSL (Kotlin DSL 사용 금지)

### 데이터베이스
- **Amazon Aurora MySQL**: 3.x (MySQL 8.0+ 호환) - Command Side (쓰기 전용)
- **MongoDB Atlas**: 7.0+ - Query Side (읽기 전용, Vector Search 지원)

### 메시징 시스템
- **Apache Kafka**: 이벤트 기반 CQRS 동기화

### AI/ML 라이브러리
- **langchain4j**: 0.35.0 (RAG 프레임워크)
- **OpenAI API**: GPT-4o-mini (LLM), text-embedding-3-small (Embedding)

### 기타 주요 라이브러리
- **Spring Security**: 인증/인가
- **Spring Batch**: 배치 처리
- **Spring Data JPA**: 데이터 접근 계층
- **Spring Data MongoDB**: MongoDB 접근 계층
- **MyBatis**: 복잡한 조회 쿼리 전용
- **Spring REST Docs**: API 문서화
- **OpenFeign**: 외부 API 클라이언트
- **Redis**: 캐싱, OAuth State 관리, 멱등성 보장, 세션 관리

## 프로젝트 구조

이 프로젝트는 Gradle 멀티모듈 구조로 구성되어 있으며, `settings.gradle`의 자동 모듈 검색 로직을 통해 모듈이 자동으로 등록됩니다.

```
tech-n-ai/
├── api/                    # REST API 서버 모듈
│   ├── agent/              # 🤖 LangChain4j AI Agent (자율 업데이트 추적)
│   ├── ai-update/          # AI 업데이트 정보 API
│   ├── auth/               # 인증 API (OAuth 2.0 지원)
│   ├── contest/            # 대회 정보 API
│   ├── gateway/            # API Gateway
│   ├── news/               # 뉴스 정보 API
│   ├── archive/            # 사용자 아카이브 API
│   └── chatbot/            # langchain4j RAG 기반 챗봇 API
├── batch/                  # 배치 처리 모듈
│   └── source/            # 정보 출처 업데이트 배치 (GitHub Release, Web Scraping)
├── client/                 # 외부 API 연동 모듈
│   ├── feign/              # OpenFeign 클라이언트 (OAuth, GitHub, Internal API)
│   ├── rss/                # RSS 피드 파서
│   ├── scraper/            # 웹 스크래핑
│   ├── slack/              # Slack 알림 클라이언트
│   └── mail/               # 이메일 전송 클라이언트
├── common/                 # 공통 모듈
│   ├── core/               # 핵심 유틸리티
│   ├── exception/          # 예외 처리
│   ├── kafka/              # Kafka 설정 및 이벤트 모델
│   └── security/           # 보안 관련 (JWT, Spring Security)
└── datasource/             # 데이터 소스 모듈 (데이터 접근 계층)
    ├── aurora/             # Amazon Aurora MySQL (Command Side)
    └── mongodb/            # MongoDB Atlas (Query Side)
```

### 모듈 간 의존성

의존성 방향: **API → Domain → Common → Client**

- **API 모듈**: Domain, Common, Client 모듈 의존
- **Domain 모듈**: Common 모듈 의존
- **Common 모듈**: 독립적 (다른 모듈에 의존하지 않음)
- **Client 모듈**: 독립적 (다른 모듈에 의존하지 않음)

### 모듈 네이밍 규칙

`settings.gradle`의 자동 모듈 검색 로직에 따라 모듈 이름은 `{parentDir}-{moduleDir}` 형식으로 자동 생성됩니다.

- 예: `api/auth` → `api-auth`
- 예: `domain/aurora` → `domain-aurora`

## 데이터베이스

### Aurora MySQL 스키마 개요

Command Side (쓰기 전용)로 사용되는 Aurora MySQL의 주요 테이블:

- **User**: 사용자 정보
- **Admin**: 관리자 정보
- **Archive**: 사용자 아카이브 정보
- **RefreshToken**: JWT Refresh Token
- **EmailVerification**: 이메일 인증 토큰
- **Provider**: OAuth Provider 정보
- **ConversationSession**: 대화 세션 정보 (RAG 챗봇용)
- **ConversationMessage**: 대화 메시지 히스토리 (RAG 챗봇용)
- **히스토리 테이블**: UserHistory, AdminHistory, ArchiveHistory

#### TSID Primary Key 전략

모든 테이블의 Primary Key는 TSID (Time-Sorted Unique Identifier) 방식을 사용합니다:

- **타입**: `BIGINT UNSIGNED`
- **생성 방식**: 애플리케이션 레벨에서 자동 생성
- **장점**: 시간 기반 정렬, 분산 환경에서 고유성 보장, 인덱스 효율성 향상

#### Aurora MySQL ERD

![Aurora MySQL ERD](contents/aurora-erd-diagram.png)

자세한 스키마 설계는 다음 문서를 참고하세요:
- [Amazon Aurora MySQL 테이블 설계서](docs/step1/3. aurora-schema-design.md)

### MongoDB Atlas 스키마 개요

Query Side (읽기 전용)로 사용되는 MongoDB Atlas의 주요 컬렉션:

- **SourcesDocument**: 정보 출처 정보
- **AiUpdateDocument**: AI 서비스 업데이트 정보 (OpenAI, Anthropic, Google, Meta)
- **ContestDocument**: 대회 정보 (읽기 최적화, Vector Search 지원)
- **NewsArticleDocument**: 뉴스 기사 정보 (읽기 최적화, Vector Search 지원)
- **ArchiveDocument**: 사용자 아카이브 정보 (읽기 최적화, Vector Search 지원)
- **UserProfileDocument**: 사용자 프로필 정보 (읽기 최적화)
- **ConversationSessionDocument**: 대화 세션 정보 (RAG 챗봇용)
- **ConversationMessageDocument**: 대화 메시지 히스토리 (RAG 챗봇용)
- **ExceptionLogDocument**: 예외 로그

#### 읽기 최적화 전략

- **비정규화**: 자주 함께 조회되는 데이터를 하나의 도큐먼트에 포함
- **인덱스 전략**: ESR 규칙 (Equality → Sort → Range) 준수
- **프로젝션**: 필요한 필드만 선택하여 네트워크 트래픽 최소화
- **Vector Search**: RAG 챗봇을 위한 벡터 검색 인덱스 (1536차원, cosine similarity)

#### MongoDB Atlas ERD

![MongoDB Atlas ERD](contents/mongodb-erd-diagram.png)

자세한 스키마 설계는 다음 문서를 참고하세요:
- [MongoDB Atlas 도큐먼트 설계서](docs/step1/2. mongodb-schema-design.md)

### 마이그레이션

Aurora MySQL의 스키마 변경은 Flyway를 통해 관리됩니다. 마이그레이션 스크립트는 각 모듈의 `src/main/resources/db/migration/` 디렉토리에 위치합니다.

### 요구스택

- **Java**: 21 이상
- **Gradle**: 프로젝트에 포함된 Gradle Wrapper 사용
- **데이터베이스**:
  - Amazon Aurora MySQL 클러스터 (또는 MySQL 8.0+ 호환 데이터베이스)
  - MongoDB Atlas 클러스터 (또는 MongoDB 7.0+)
- **메시징 시스템**: Apache Kafka
- **캐싱**: Redis

### 환경 변수 설정

```bash
# Aurora DB Cluster 연결 정보
export AURORA_WRITER_ENDPOINT=aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
export AURORA_READER_ENDPOINT=aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
export AURORA_USERNAME=admin
export AURORA_PASSWORD=your-password-here
export AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8

# MongoDB Atlas 연결 정보
export MONGODB_ATLAS_CONNECTION_STRING=mongodb+srv://username:password@cluster.mongodb.net/database?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true

# Kafka 연결 정보
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis 연결 정보
export REDIS_HOST=localhost
export REDIS_PORT=6379

# JWT 설정
export JWT_SECRET=your-jwt-secret-key
export JWT_ACCESS_TOKEN_EXPIRATION=900000  # 15분 (밀리초)
export JWT_REFRESH_TOKEN_EXPIRATION=604800000  # 7일 (밀리초)

# OAuth 설정
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
export NAVER_CLIENT_ID=your-naver-client-id
export NAVER_CLIENT_SECRET=your-naver-client-secret
export KAKAO_CLIENT_ID=your-kakao-client-id
export KAKAO_CLIENT_SECRET=your-kakao-client-secret

# OpenAI API 설정 (RAG 챗봇용)
export OPENAI_API_KEY=your-openai-api-key

# AI LLM 설정 (배치 작업용)
export ANTHROPIC_API_KEY=your-anthropic-api-key

# Slack 알림 설정 (선택적)
export SLACK_WEBHOOK_URL=your-slack-webhook-url
```


## API 목록

### API Gateway를 통한 접근

모든 API는 **API Gateway**를 통해 접근합니다:
- **Gateway Base URL**: `http://localhost:8081` (Local 환경)
- **Gateway 경로**: Gateway는 요청 URI 경로를 기준으로 적절한 백엔드 API 서버로 라우팅합니다.

### 주요 API 엔드포인트

#### 🤖 AI Agent API (`/api/v1/agent`)

- `POST /api/v1/agent/run` - AI Agent 수동 실행 (내부 API, X-Internal-Api-Key 필요)

#### AI 업데이트 API (`/api/v1/ai-update`)

**공개 API (인증 불필요)**:
- `GET /api/v1/ai-update` - AI 업데이트 목록 조회 (필터링, 페이지네이션)
- `GET /api/v1/ai-update/{id}` - AI 업데이트 상세 조회
- `GET /api/v1/ai-update/search` - AI 업데이트 검색

**내부 API (X-Internal-Api-Key 필요)**:
- `POST /api/v1/ai-update/internal` - AI 업데이트 단건 생성
- `POST /api/v1/ai-update/internal/batch` - AI 업데이트 배치 생성
- `POST /api/v1/ai-update/{id}/approve` - AI 업데이트 승인 (PUBLISHED)
- `POST /api/v1/ai-update/{id}/reject` - AI 업데이트 거부 (REJECTED)

#### 인증 API (`/api/v1/auth`)

- `POST /api/v1/auth/signup` - 회원가입
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/logout` - 로그아웃
- `POST /api/v1/auth/refresh` - 토큰 갱신
- `GET /api/v1/auth/verify-email` - 이메일 인증
- `POST /api/v1/auth/reset-password` - 비밀번호 재설정 요청
- `POST /api/v1/auth/reset-password/confirm` - 비밀번호 재설정 확인
- `GET /api/v1/auth/oauth2/{provider}` - OAuth 로그인 시작
- `GET /api/v1/auth/oauth2/{provider}/callback` - OAuth 로그인 콜백

#### 대회 정보 API (`/api/v1/contest`)

- `GET /api/v1/contest` - 대회 목록 조회 (공개 API, 인증 불필요)
- `GET /api/v1/contest/{id}` - 대회 상세 조회 (공개 API, 인증 불필요)
- `GET /api/v1/contest/search` - 대회 검색 (공개 API, 인증 불필요)

#### 뉴스 정보 API (`/api/v1/news`)

- `GET /api/v1/news` - 뉴스 목록 조회 (공개 API, 인증 불필요)
- `GET /api/v1/news/{id}` - 뉴스 상세 조회 (공개 API, 인증 불필요)
- `GET /api/v1/news/search` - 뉴스 검색 (공개 API, 인증 불필요)

#### 사용자 아카이브 API (`/api/v1/archive`)

- `POST /api/v1/archive` - 아카이브 저장 (인증 필요)
- `GET /api/v1/archive` - 아카이브 목록 조회 (인증 필요)
- `GET /api/v1/archive/{id}` - 아카이브 상세 조회 (인증 필요)
- `PUT /api/v1/archive/{id}` - 아카이브 수정 (인증 필요)
- `DELETE /api/v1/archive/{id}` - 아카이브 삭제 (인증 필요)
- `GET /api/v1/archive/deleted` - 삭제된 아카이브 목록 조회 (인증 필요)
- `POST /api/v1/archive/{id}/restore` - 아카이브 복구 (인증 필요)
- `GET /api/v1/archive/search` - 아카이브 검색 (인증 필요)

#### 🌟 챗봇 API (`/api/v1/chatbot`)

- `POST /api/v1/chatbot/chat` - 챗봇 대화 (RAG 기반 응답 생성)
- `GET /api/v1/chatbot/sessions` - 대화 세션 목록 조회
- `GET /api/v1/chatbot/sessions/{sessionId}` - 대화 세션 상세 조회
- `DELETE /api/v1/chatbot/sessions/{sessionId}` - 대화 세션 삭제

### 인증 방법

**인증이 필요한 API**는 JWT (JSON Web Token) 기반 인증을 사용합니다. **공개 API**는 인증 없이 접근할 수 있습니다.

#### 인증 필요 여부

- **인증 필요**: `/api/v1/archive/**`, `/api/v1/chatbot/**`
- **인증 불필요**: `/api/v1/auth/**`, `/api/v1/contest/**`, `/api/v1/news/**`

#### 인증 헤더

```
Authorization: Bearer {access_token}
```

#### 토큰 발급

1. 회원가입 또는 로그인을 통해 `access_token`과 `refresh_token`을 받습니다.
2. `access_token`은 15분 후 만료됩니다.
3. `refresh_token`을 사용하여 새로운 `access_token`을 발급받을 수 있습니다.
4. `refresh_token`은 7일 후 만료됩니다.

자세한 인증/인가 구현 방법은 다음 문서를 참고하세요:
- [Spring Security 인증/인가 설계 가이드](docs/step6/spring-security-auth-design-guide.md)
- [OAuth Provider 구현 가이드](docs/step6/oauth-provider-implementation-guide.md)


## 배포

### 배포 환경

- **개발 환경**: 로컬 개발 환경
- **베타 환경**: 베타 테스트 환경
- **프로덕션 환경**: 운영 환경

각 환경별 설정 파일은 각 API 모듈의 `src/main/resources/` 디렉토리에 위치합니다:
- `application.yml`: 공통 설정
- `application-local.yml`: 로컬 환경 설정
- `application-dev.yml`: 개발 환경 설정
- `application-beta.yml`: 베타 환경 설정
- `application-prod.yml`: 프로덕션 환경 설정

**API Gateway 설정**:
- Gateway는 모든 클라이언트 요청의 단일 진입점으로, 환경별 백엔드 서비스 URL을 설정합니다.
- Local 환경: `http://localhost:8082~8086` (각 API 서버별 포트)
- Dev/Beta/Prod 환경: `http://api-{service}-service:8080` (Kubernetes Service 이름)


## 참고 문서

### 설계 문서

#### 핵심 아키텍처 설계
- [CQRS Kafka 동기화 설계서](docs/step11/cqrs-kafka-sync-design.md)
- [langchain4j RAG 기반 챗봇 설계서](docs/step12/rag-chatbot-design.md)
- [AI Agent 자동화 파이프라인 설계서](docs/reference/automation-pipeline-to-ai-agent/)
  - [Phase 1: 데이터 수집 파이프라인 설계서](docs/reference/automation-pipeline-to-ai-agent/phase1-data-pipeline-design.md)
  - [Phase 2: LangChain4j Tools 설계서](docs/reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md)
  - [Phase 3: AI Agent 통합 설계서](docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md)
- [MongoDB Atlas 도큐먼트 설계서](docs/step1/2. mongodb-schema-design.md)
- [Amazon Aurora MySQL 테이블 설계서](docs/step1/3. aurora-schema-design.md)

#### 인증/인가 설계
- [Spring Security 인증/인가 설계 가이드](docs/step6/spring-security-auth-design-guide.md)
- [OAuth Provider 구현 가이드](docs/step6/oauth-provider-implementation-guide.md)

#### Gateway 설계
- [Gateway 설계서](docs/step14/gateway-design.md)
- [Gateway 구현 계획](docs/step14/gateway-implementation-plan.md)

#### API 설계
- [Contest 및 News API 설계서](docs/step9/contest-news-api-design.md)
- [사용자 아카이브 기능 설계서](docs/step13/user-archive-feature-design.md)

#### 기타 설계
- [AI LLM 통합 분석 문서](docs/step11/ai-integration-analysis.md)
- [배치 잡 통합 설계서](docs/step10/batch-job-integration-design.md)
- [Redis 최적화 베스트 프랙티스](docs/step7/redis-optimization-best-practices.md)
- [RSS/Scraper 모듈 분석](docs/step8/rss-scraper-modules-analysis.md)
- [Slack 연동 설계 가이드](docs/step8/slack-integration-design-guide.md)

### 공식 문서

- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [Spring Cloud Gateway 공식 문서](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Security 공식 문서](https://spring.io/projects/spring-security)
- [Reactor Netty 공식 문서](https://projectreactor.io/docs/netty/release/reference/index.html)
- [langchain4j 공식 문서](https://docs.langchain4j.dev/)
- [Amazon Aurora MySQL 공식 문서](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html)
- [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)
- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [OpenAI API 공식 문서](https://platform.openai.com/docs)

