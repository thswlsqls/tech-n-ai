# 프로젝트 전체 아키텍처 다이어그램

> 이 문서는 tech-n-ai 프로젝트의 전체 모듈 구조, 의존성 관계, 데이터 흐름을 Mermaid 다이어그램으로 정리합니다.

## 1. 시스템 전체 구조

클라이언트 → API Gateway → 백엔드 서비스 → 데이터 저장소 흐름을 보여줍니다.

```mermaid
graph TB
    subgraph Client["Client"]
        WEB["Web Frontend"]
        ADMIN["Admin Console"]
    end

    subgraph Gateway["API Gateway :8081"]
        GW["api-gateway<br/>JWT 검증 · CORS · 라우팅<br/>Rate Limiting · Circuit Breaker"]
    end

    subgraph APIs["Backend API Services"]
        AUTH["api-auth :8083<br/>인증/인가 · OAuth2"]
        CHATBOT["api-chatbot :8084<br/>RAG 챗봇 · 하이브리드 검색"]
        BOOKMARK["api-bookmark :8085<br/>북마크 관리"]
        EMERGING["api-emerging-tech :8082<br/>신기술 업데이트 관리"]
        AGENT["api-agent :8086<br/>LLM Agent · Tool 실행"]
    end

    subgraph Batch["Batch"]
        BATCH["batch-source<br/>데이터 수집 배치"]
    end

    subgraph Data["Data Stores"]
        AURORA[("Aurora MySQL<br/>Command Side")]
        MONGO[("MongoDB Atlas<br/>Query Side")]
        REDIS[("Redis<br/>Session · Cache")]
        KAFKA["Apache Kafka<br/>이벤트 동기화"]
    end

    subgraph External["External Services"]
        OPENAI["OpenAI API<br/>GPT-4o-mini"]
        GITHUB["GitHub API"]
        SLACK["Slack API"]
        RSS["RSS Feeds<br/>OpenAI · Google"]
        SCRAPE["Web Scraping<br/>Anthropic · Meta"]
    end

    WEB --> GW
    ADMIN --> GW
    GW --> AUTH
    GW --> CHATBOT
    GW --> BOOKMARK
    GW --> EMERGING
    GW --> AGENT

    AUTH --> AURORA
    AUTH --> REDIS

    CHATBOT --> AURORA
    CHATBOT --> MONGO
    CHATBOT --> OPENAI
    CHATBOT --> KAFKA

    BOOKMARK --> AURORA
    BOOKMARK --> MONGO

    EMERGING --> MONGO
    EMERGING --> OPENAI

    AGENT --> AURORA
    AGENT --> MONGO
    AGENT --> GITHUB
    AGENT --> SLACK
    AGENT --> RSS
    AGENT --> SCRAPE
    AGENT --> OPENAI
    AGENT --> KAFKA

    BATCH --> AURORA
    BATCH --> MONGO
    BATCH --> GITHUB
    BATCH --> RSS
    BATCH --> SCRAPE
    BATCH --> KAFKA

    AURORA -- "Kafka 이벤트" --> KAFKA
    KAFKA -- "CQRS Sync" --> MONGO
```

## 2. 멀티 모듈 의존성 그래프

모듈 간 `project(':xxx')` 의존성 관계를 계층별로 보여줍니다.

```mermaid
graph TD
    subgraph API["API Layer (Runnable Applications)"]
        api-gateway["api-gateway<br/>:8081"]
        api-auth["api-auth<br/>:8083"]
        api-chatbot["api-chatbot<br/>:8084"]
        api-bookmark["api-bookmark<br/>:8085"]
        api-emerging-tech["api-emerging-tech<br/>:8082"]
        api-agent["api-agent<br/>:8086"]
    end

    subgraph BATCH["Batch Layer"]
        batch-source["batch-source"]
    end

    subgraph CLIENT["Client Layer (외부 연동)"]
        client-feign["client-feign<br/>Feign · Internal API"]
        client-mail["client-mail<br/>메일 발송"]
        client-rss["client-rss<br/>RSS 파싱"]
        client-scraper["client-scraper<br/>웹 스크래핑"]
        client-slack["client-slack<br/>Slack 알림"]
    end

    subgraph COMMON["Common Layer (공유 라이브러리)"]
        common-core["common-core<br/>기반 유틸리티"]
        common-exception["common-exception<br/>전역 예외 처리"]
        common-security["common-security<br/>JWT · OAuth2"]
        common-kafka["common-kafka<br/>Kafka 이벤트"]
        common-conversation["common-conversation<br/>대화 세션/메시지"]
    end

    subgraph DS["Datasource Layer (데이터 접근)"]
        datasource-aurora["datasource-aurora<br/>Aurora MySQL · JPA/MyBatis"]
        datasource-mongodb["datasource-mongodb<br/>MongoDB Atlas"]
    end

    %% API → Client
    api-auth --> client-feign
    api-auth --> client-mail
    api-chatbot --> client-feign
    api-agent --> client-feign
    api-agent --> client-slack
    api-agent --> client-scraper
    api-agent --> client-rss

    %% API → Common
    api-gateway --> common-core
    api-gateway --> common-security
    api-auth --> common-core
    api-auth --> common-security
    api-auth --> common-exception
    api-bookmark --> common-core
    api-bookmark --> common-exception
    api-bookmark --> common-security
    api-chatbot --> common-core
    api-chatbot --> common-exception
    api-chatbot --> common-security
    api-chatbot --> common-conversation
    api-chatbot --> common-kafka
    api-emerging-tech --> common-core
    api-emerging-tech --> common-exception
    api-agent --> common-core
    api-agent --> common-exception
    api-agent --> common-conversation
    api-agent --> common-kafka

    %% API → Datasource
    api-auth --> datasource-aurora
    api-bookmark --> datasource-aurora
    api-bookmark --> datasource-mongodb
    api-chatbot --> datasource-aurora
    api-chatbot --> datasource-mongodb
    api-emerging-tech --> datasource-mongodb
    api-agent --> datasource-aurora
    api-agent --> datasource-mongodb

    %% Batch → others
    batch-source --> common-core
    batch-source --> common-security
    batch-source --> common-kafka
    batch-source --> datasource-aurora
    batch-source --> datasource-mongodb
    batch-source --> client-feign
    batch-source --> client-rss
    batch-source --> client-scraper

    %% Client → Common
    client-feign --> common-core
    client-feign --> common-exception
    client-feign --> common-kafka
    client-feign --> datasource-aurora
    client-feign --> datasource-mongodb
    client-mail --> common-core
    client-mail --> common-exception
    client-rss --> common-core
    client-rss --> common-exception
    client-scraper --> common-core
    client-scraper --> common-exception
    client-slack --> common-core
    client-slack --> common-exception

    %% Common → Datasource
    common-exception --> common-core
    common-exception --> datasource-mongodb
    common-kafka --> common-core
    common-kafka --> datasource-mongodb
    common-conversation --> common-core
    common-conversation --> common-exception
    common-conversation --> common-kafka
    common-conversation --> datasource-aurora
    common-conversation --> datasource-mongodb
    common-security --> common-core

    %% Datasource → Common
    datasource-aurora --> common-core
    datasource-mongodb --> common-core
```

## 3. CQRS 데이터 흐름

Command/Query 분리 패턴과 Kafka 기반 동기화 흐름입니다.

```mermaid
flowchart LR
    subgraph Command["Command Side (Write)"]
        API_W["API Service<br/>CUD 요청"]
        JPA["JPA Writer<br/>repository/writer/"]
        AURORA[("Aurora MySQL")]
    end

    subgraph Sync["Event Sync"]
        LISTENER["HistoryEntityListener"]
        KAFKA["Apache Kafka"]
        CONSUMER["Kafka Consumer"]
    end

    subgraph Query["Query Side (Read)"]
        MONGO[("MongoDB Atlas")]
        MYBATIS["MyBatis Reader<br/>repository/reader/"]
        MONGO_REPO["MongoRepository"]
        API_R["API Service<br/>Read 요청"]
    end

    API_W --> JPA --> AURORA
    AURORA --> LISTENER
    LISTENER --> KAFKA
    KAFKA --> CONSUMER --> MONGO
    MONGO --> MONGO_REPO --> API_R
    AURORA --> MYBATIS --> API_R
```

## 4. API Gateway 라우팅

Gateway에서 각 백엔드 서비스로의 라우팅 구조입니다.

```mermaid
flowchart LR
    CLIENT["Client Request"] --> GW["API Gateway :8081"]

    GW -->|"JWT 검증"| JWT{"JWT Filter"}
    JWT -->|유효| ROUTE["Route Matching"]
    JWT -->|만료/무효| REJECT["401 Unauthorized"]

    ROUTE -->|"/api/v1/auth/**"| AUTH["api-auth :8083"]
    ROUTE -->|"/api/v1/chatbot/**"| CHATBOT["api-chatbot :8084"]
    ROUTE -->|"/api/v1/bookmark/**"| BOOKMARK["api-bookmark :8085"]
    ROUTE -->|"/api/v1/emerging-tech/**"| EMERGING["api-emerging-tech :8082"]
    ROUTE -->|"/api/v1/agent/**"| AGENT["api-agent :8086"]

    subgraph Resilience["회복탄력성"]
        CB["Circuit Breaker"]
        RL["Rate Limiter"]
        RETRY["Retry"]
    end

    ROUTE --> CB --> RL --> RETRY
```

## 5. Agent 모듈 Tool 실행 흐름

LangChain4j AiServices 기반 Agent의 Tool 호출 흐름입니다.

```mermaid
flowchart TB
    USER["사용자 요청<br/>(자연어 Goal)"]
    CTRL["AgentController"]
    FACADE["AgentFacade<br/>오케스트레이션"]
    AGENT["EmergingTechAgentImpl"]
    AI["AiServices<br/>(AgentAssistant)"]
    LLM["OpenAI GPT-4o-mini"]

    USER --> CTRL --> FACADE --> AGENT --> AI

    AI <-->|"프롬프트/응답"| LLM

    subgraph Tools["Agent Tools"]
        direction LR
        T1["fetch_github_releases"]
        T2["scrape_web_page"]
        T3["list_emerging_techs"]
        T4["get_emerging_tech_detail"]
        T5["search_emerging_techs"]
        T6["get_statistics"]
        T7["analyze_text_frequency"]
        T8["send_slack_notification"]
        T9["collect_github_releases"]
        T10["collect_rss_feeds"]
        T11["collect_scraped_articles"]
    end

    LLM -->|"Tool 호출 결정"| Tools
    Tools -->|"결과 반환"| LLM

    subgraph Adapters["Tool Adapters"]
        A1["GitHubToolAdapter"]
        A2["ScraperToolAdapter"]
        A3["EmergingTechToolAdapter"]
        A4["AnalyticsToolAdapter"]
        A5["SlackToolAdapter"]
        A6["DataCollectionToolAdapter"]
    end

    T1 --> A1
    T2 --> A2
    T3 --> A3
    T4 --> A3
    T5 --> A3
    T6 --> A4
    T7 --> A4
    T8 --> A5
    T9 --> A6
    T10 --> A6
    T11 --> A6

    subgraph ErrorHandling["Error Handlers"]
        EH1["ToolExecutionErrorHandler"]
        EH2["ToolArgumentsErrorHandler"]
        EH3["HallucinatedToolNameStrategy"]
    end

    Tools -.->|"예외 발생 시"| ErrorHandling
    ErrorHandling -.->|"에러 메시지"| LLM

    subgraph Memory["Chat Memory"]
        MEM["MongoDbChatMemoryStore<br/>(세션별 대화 이력)"]
    end

    AI <--> MEM
```

## 6. 기술 스택 요약

```mermaid
mindmap
  root((tech-n-ai))
    Runtime
      Java 21
      Spring Boot 4.0.2
      Spring Cloud 2025.1.0
      Hibernate 7.2.1.Final
    Data
      Aurora MySQL 3.x
        JPA Writer
        MyBatis Reader
      MongoDB Atlas 7.0+
        Vector Search
        Aggregation
      Apache Kafka
        CQRS Sync
      Redis
        Session
        Cache
    AI / LLM
      langchain4j 1.10.0
        OpenAI GPT-4o-mini
        MongoDB Atlas VectorStore
        AiServices Agent
    Client
      OpenFeign
      WebClient
      Jsoup 1.17.2
      Rome RSS 1.19.0
    Build
      Gradle 9.2.1
        Groovy DSL
      Spring REST Docs
    Infra
      API Gateway
        Resilience4j
        Rate Limiting
      Monitoring
        Micrometer
        Prometheus
        OpenTelemetry
```
