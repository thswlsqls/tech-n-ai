# Chatbot API Module

langchain4j를 활용한 RAG(Retrieval-Augmented Generation) 기반 챗봇 시스템입니다. MongoDB Atlas Vector Search를 활용하여 개발자 대회 정보, IT 테크 뉴스, 사용자 아카이브를 검색하고 자연어로 질문할 수 있는 기능을 제공합니다.

## 목차

1. [개요](#개요)
2. [아키텍처](#아키텍처)
3. [주요 기능](#주요-기능)
4. [기술 스택](#기술-스택)
5. [API 엔드포인트](#api-엔드포인트)
6. [설정](#설정)
7. [의존성](#의존성)
8. [구현 구조](#구현-구조)
9. [참고 자료](#참고-자료)

---

## 개요

### 배경

현재 프로젝트는 CQRS 패턴을 적용하여 Command Side(Aurora MySQL)와 Query Side(MongoDB Atlas)를 분리하고 있으며, MongoDB Atlas에는 다음과 같은 컬렉션이 저장되어 있습니다:

- **ContestDocument**: 개발자 대회 정보
- **NewsArticleDocument**: IT 테크 뉴스 기사
- **ArchiveDocument**: 사용자 아카이브 항목

이러한 도큐먼트들을 임베딩하여 벡터 검색 기반의 지식 검색 챗봇을 구축함으로써, 사용자가 자연어로 대회 정보, 뉴스 기사, 자신의 아카이브를 검색하고 질문할 수 있도록 합니다.

### LLM 및 Embedding Model 선택

- **LLM Provider**: OpenAI GPT-4o-mini (기본)
  - 비용 최적화: $0.15/$0.60 per 1M tokens (입력/출력)
  - 빠른 응답 속도
  - 128K 컨텍스트 윈도우

- **Embedding Model**: OpenAI text-embedding-3-small (기본)
  - LLM Provider와 동일한 Provider 사용으로 통합성 최적화
  - 비용 최적화: $0.02 per 1M tokens
  - 1536 dimensions (기본값)

### 설계 원칙

1. **클린코드 원칙**
   - 단일 책임 원칙 (SRP)
   - 의존성 역전 원칙 (DIP)
   - 개방-폐쇄 원칙 (OCP)

2. **최소 구현 원칙**
   - 현재 필요한 기능만 구현
   - 단순하고 명확한 구조
   - 단계적 확장 가능한 구조

3. **운영 환경 고려**
   - 비용 통제 (토큰 사용량 추적 및 제한)
   - 성능 최적화 (검색 결과 수 제한, 프롬프트 최적화)
   - 에러 처리 (각 단계별 에러 핸들링)

---

## 아키텍처

### Overall System Architecture

![Overall System Architecture](../../contents/api-chatbot/overall-system-architecture.png)

시스템은 다음과 같은 계층 구조로 구성됩니다:

- **API Layer**: RESTful API 엔드포인트 제공
- **Service Layer**: 비즈니스 로직 처리
- **Chain Layer**: RAG 파이프라인 체인 처리
- **Data Layer**: MongoDB Atlas Vector Search 및 Redis 캐싱
- **External**: LLM Provider 및 Embedding Model

### Chatbot LLM RAG Pipeline

![Chatbot LLM RAG Pipeline](../../contents/api-chatbot/chatbot-llm-rag-pipeline.png)

RAG 파이프라인은 다음과 같은 단계로 구성됩니다:

1. **입력 전처리**: 사용자 입력 검증 및 정규화
2. **의도 분류**: RAG 필요 여부 판단
3. **벡터 검색**: MongoDB Atlas Vector Search를 통한 관련 문서 검색
4. **결과 정제**: 유사도 필터링 및 중복 제거
5. **답변 생성**: LLM을 통한 최종 답변 생성

### 데이터 흐름

```
사용자 입력
  ↓
입력 전처리 (검증, 정규화)
  ↓
의도 분류 (RAG 필요 여부)
  ↓
[RAG 필요 시]
  ↓
입력 해석 체인 (검색 쿼리 추출)
  ↓
벡터 검색 (MongoDB Atlas Vector Search)
  ↓
결과 정제 체인 (유사도 필터링, 중복 제거)
  ↓
답변 생성 체인 (프롬프트 구축, LLM 호출)
  ↓
최종 답변 반환
```

---

## 주요 기능

### 1. RAG 기반 지식 검색

- MongoDB Atlas Vector Search를 활용한 의미 기반 검색
- 개발자 대회 정보, IT 테크 뉴스, 사용자 아카이브 검색
- 유사도 기반 관련 문서 추출

### 2. 멀티턴 대화 히스토리 관리

- 세션 기반 대화 컨텍스트 관리
- JWT 토큰 기반 사용자 인증 및 세션 소유권 검증
- ChatMemory를 통한 대화 히스토리 유지
- 토큰 수 기준 메시지 윈도우 관리 (기본 전략)

### 3. 의도 분류

- RAG 필요 질문과 일반 대화 자동 분류
- RAG 필요 시 벡터 검색 수행
- 일반 대화 시 LLM 직접 호출

### 4. 토큰 제어 및 비용 통제

- 입력/출력 토큰 사용량 추적
- 토큰 제한 검증 및 경고
- 캐싱을 통한 중복 호출 방지

### 5. Provider별 메시지 포맷 변환

- OpenAI 메시지 포맷 변환 (기본)
- Anthropic 메시지 포맷 변환 (대안)
- Provider별 특성에 맞는 메시지 포맷 자동 변환

### 6. 세션 생명주기 관리

- 비활성 세션 자동 비활성화 (30분 미사용 시)
- 만료된 세션 자동 처리 (90일 경과 시)
- 배치 작업을 통한 세션 정리

---

## 기술 스택

### Core Framework

- **Spring Boot**: 4.0.1
- **Java**: 21
- **Gradle**: 멀티모듈 빌드

### AI/ML 라이브러리

- **langchain4j**: 0.35.0
  - LLM 통합 및 추상화
  - MongoDB Atlas Vector Search 통합
  - ChatMemory 관리

### LLM Provider

- **OpenAI** (기본)
  - Model: GPT-4o-mini
  - Embedding Model: text-embedding-3-small

### 데이터베이스

- **MongoDB Atlas**: Vector Search를 위한 문서 저장소
- **Aurora MySQL**: 세션 및 메시지 히스토리 저장

### 캐싱

- **Redis**: 검색 결과 및 임베딩 캐싱

### 인증

- **Spring Security**: JWT 토큰 기반 인증
- **JWT**: 사용자 식별 및 권한 확인

---

## API 엔드포인트

### 1. 챗봇 대화

**POST** `/api/v1/chatbot`

사용자 메시지를 받아 챗봇 응답을 생성합니다.

**Request Body:**
```json
{
  "message": "최근 AI 관련 대회 정보를 알려주세요",
  "conversationId": "optional-session-id"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "최근 AI 관련 대회 정보는 다음과 같습니다...",
    "conversationId": "session-id",
    "sources": [
      {
        "type": "CONTEST",
        "id": "contest-id",
        "title": "AI Challenge 2024",
        "url": "https://example.com/contest"
      }
    ],
    "tokenUsage": {
      "inputTokens": 150,
      "outputTokens": 200,
      "totalTokens": 350
    }
  }
}
```

### 2. 세션 목록 조회

**GET** `/api/v1/chatbot/sessions?page=1&size=20`

사용자의 대화 세션 목록을 조회합니다.

**Query Parameters:**
- `page`: 페이지 번호 (기본값: 1)
- `size`: 페이지 크기 (기본값: 20)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "session-id",
        "title": "AI 관련 질문",
        "createdAt": "2024-01-16T10:00:00",
        "lastMessageAt": "2024-01-16T10:05:00",
        "isActive": true
      }
    ],
    "totalElements": 10,
    "totalPages": 1
  }
}
```

### 3. 세션 상세 조회

**GET** `/api/v1/chatbot/sessions/{sessionId}`

특정 세션의 상세 정보를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "session-id",
    "title": "AI 관련 질문",
    "createdAt": "2024-01-16T10:00:00",
    "lastMessageAt": "2024-01-16T10:05:00",
    "isActive": true,
    "messageCount": 5
  }
}
```

### 4. 메시지 히스토리 조회

**GET** `/api/v1/chatbot/sessions/{sessionId}/messages?page=1&size=50`

특정 세션의 메시지 히스토리를 조회합니다.

**Query Parameters:**
- `page`: 페이지 번호 (기본값: 1)
- `size`: 페이지 크기 (기본값: 50)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "message-id",
        "role": "USER",
        "content": "최근 AI 관련 대회 정보를 알려주세요",
        "sequenceNumber": 1,
        "createdAt": "2024-01-16T10:00:00"
      },
      {
        "id": "message-id-2",
        "role": "ASSISTANT",
        "content": "최근 AI 관련 대회 정보는 다음과 같습니다...",
        "sequenceNumber": 2,
        "createdAt": "2024-01-16T10:00:05"
      }
    ],
    "totalElements": 2,
    "totalPages": 1
  }
}
```

### 5. 세션 삭제

**DELETE** `/api/v1/chatbot/sessions/{sessionId}`

특정 세션을 삭제합니다.

**Response:**
```json
{
  "success": true,
  "data": null
}
```

### 인증

모든 API 엔드포인트는 JWT 토큰 기반 인증이 필요합니다.

- **Authorization Header**: `Bearer {jwt-token}`
- **userId**: JWT 토큰에서 자동 추출 (요청 본문에 포함하지 않음)
- **세션 소유권 검증**: 모든 세션 관련 작업은 소유권 검증 필수

---

## 설정

### application-chatbot-api.yml

```yaml
spring:
  application:
    name: chatbot-api
  profiles:
    include:
      - common-core
      - api-domain
      - mongodb-domain

module:
  aurora:
    schema: chatbot

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 2000
      timeout: 60s
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small
      dimensions: 1536
      timeout: 30s

chatbot:
  rag:
    max-search-results: 5
    min-similarity-score: 0.7
    max-context-tokens: 3000
  
  input:
    max-length: 500
    min-length: 1
  
  token:
    max-input-tokens: 4000
    max-output-tokens: 2000
    warning-threshold: 0.8
  
  cache:
    enabled: true
    ttl-hours: 1
    max-size: 1000
  
  session:
    inactive-threshold-minutes: 30
    expiration-days: 90
    batch-enabled: true
  
  chat-memory:
    max-tokens: 2000
    strategy: token-window
```

### 환경 변수

- `OPENAI_API_KEY`: OpenAI API 키
- `MONGODB_ATLAS_CONNECTION_STRING`: MongoDB Atlas 연결 문자열
- `MONGODB_ATLAS_DATABASE`: MongoDB Atlas 데이터베이스 이름

---

## 의존성

### build.gradle

```gradle
dependencies {
    // langchain4j Core
    implementation 'dev.langchain4j:langchain4j:0.35.0'
    
    // langchain4j MongoDB Atlas
    implementation 'dev.langchain4j:langchain4j-mongodb-atlas:0.35.0'
    
    // langchain4j OpenAI (LLM Provider - 기본 선택)
    implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'
    
    // 프로젝트 모듈 의존성
    implementation project(':common-core')
    implementation project(':common-exception')
    implementation project(':common-kafka')
    implementation project(':common-security')
    implementation project(':domain-aurora')
    implementation project(':domain-mongodb')
}
```

---

## 구현 구조

### 패키지 구조

```
api/chatbot/
├── src/main/java/com/tech/n/ai/api/chatbot/
│   ├── ApiChatbotApplication.java
│   ├── config/
│   │   ├── LangChain4jConfig.java
│   │   └── SchedulerConfig.java
│   ├── controller/
│   │   └── ChatbotController.java
│   ├── facade/
│   │   └── ChatbotFacade.java
│   ├── service/
│   │   ├── ChatbotService.java
│   │   ├── InputPreprocessingService.java
│   │   ├── IntentClassificationService.java
│   │   ├── VectorSearchService.java
│   │   ├── PromptService.java
│   │   ├── LLMService.java
│   │   ├── TokenService.java
│   │   ├── CacheService.java
│   │   ├── ConversationSessionService.java
│   │   └── ConversationMessageService.java
│   ├── chain/
│   │   ├── InputInterpretationChain.java
│   │   ├── ResultRefinementChain.java
│   │   └── AnswerGenerationChain.java
│   ├── memory/
│   │   ├── MongoDbChatMemoryStore.java
│   │   └── ConversationChatMemoryProvider.java
│   ├── converter/
│   │   ├── MessageFormatConverter.java
│   │   ├── OpenAiMessageConverter.java
│   │   └── AnthropicMessageConverter.java
│   ├── dto/
│   │   ├── request/
│   │   │   └── ChatRequest.java
│   │   └── response/
│   │       ├── ChatResponse.java
│   │       ├── SessionResponse.java
│   │       ├── MessageResponse.java
│   │       └── SourceResponse.java
│   ├── common/
│   │   └── exception/
│   │       ├── ChatbotExceptionHandler.java
│   │       ├── InvalidInputException.java
│   │       ├── TokenLimitExceededException.java
│   │       └── ConversationSessionNotFoundException.java
│   └── scheduler/
│       └── ConversationSessionLifecycleScheduler.java
└── src/main/resources/
    └── application-chatbot-api.yml
```

### 주요 컴포넌트

#### 1. Controller Layer

- **ChatbotController**: RESTful API 엔드포인트 제공
  - JWT 토큰에서 `userId` 추출
  - 요청/응답 DTO 변환

#### 2. Facade Layer

- **ChatbotFacade**: Controller와 Service 사이의 중간 계층
  - `userId`를 파라미터로 받아 서비스에 전달

#### 3. Service Layer

- **ChatbotService**: 챗봇 응답 생성 오케스트레이션
- **InputPreprocessingService**: 입력 전처리 및 검증
- **IntentClassificationService**: 의도 분류 (RAG 필요 여부)
- **VectorSearchService**: MongoDB Atlas Vector Search 수행
- **PromptService**: 프롬프트 구축 및 최적화
- **LLMService**: LLM 호출 및 응답 처리
- **TokenService**: 토큰 사용량 추적 및 제어
- **CacheService**: 검색 결과 및 임베딩 캐싱
- **ConversationSessionService**: 세션 관리 (생성, 조회, 수정, 삭제)
- **ConversationMessageService**: 메시지 히스토리 관리

#### 4. Chain Layer

- **InputInterpretationChain**: 입력 해석 및 검색 쿼리 추출
- **ResultRefinementChain**: 검색 결과 정제 (유사도 필터링, 중복 제거)
- **AnswerGenerationChain**: 최종 답변 생성

#### 5. Memory Layer

- **MongoDbChatMemoryStore**: MongoDB 기반 ChatMemory 저장소
- **ConversationChatMemoryProvider**: 세션별 ChatMemory 제공

#### 6. Converter Layer

- **MessageFormatConverter**: Provider별 메시지 포맷 변환 인터페이스
- **OpenAiMessageConverter**: OpenAI 메시지 포맷 변환
- **AnthropicMessageConverter**: Anthropic 메시지 포맷 변환

---

## 참고 자료

### 공식 문서

#### langchain4j

- **공식 문서**: https://docs.langchain4j.dev/
- **GitHub**: https://github.com/langchain4j/langchain4j
- **MongoDB Atlas 통합**: https://docs.langchain4j.dev/integrations/embedding-stores/mongodb-atlas

#### MongoDB Atlas Vector Search

- **공식 문서**: https://www.mongodb.com/docs/atlas/atlas-vector-search/
- **Vector Search Index 생성**: https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/

#### OpenAI

- **공식 문서**: https://platform.openai.com/docs
- **Chat Completions API**: https://platform.openai.com/docs/guides/chat-completions
- **GPT-4o-mini**: https://platform.openai.com/docs/models/gpt-4o-mini
- **Embeddings**: https://platform.openai.com/docs/guides/embeddings
- **text-embedding-3-small**: https://platform.openai.com/docs/models/text-embedding-3-small
- **Pricing**: https://openai.com/api/pricing/

#### Spring Security

- **공식 문서**: https://docs.spring.io/spring-security/reference/index.html
- **JWT 공식 스펙 (RFC 7519)**: https://tools.ietf.org/html/rfc7519

### 프로젝트 내 참고 문서

- **RAG 챗봇 설계서**: `docs/step12/rag-chatbot-design.md`
- **MongoDB 스키마 설계**: `docs/step1/2. mongodb-schema-design.md`
- **CQRS Kafka 동기화 설계**: `docs/step11/cqrs-kafka-sync-design.md`
- **API 엔드포인트 설계**: `docs/step2/1. api-endpoint-design.md`
- **Spring Security 인증 설계**: `docs/step6/spring-security-auth-design-guide.md`

---

## 버전 정보

- **langchain4j**: 0.35.0
- **Spring Boot**: 4.0.1
- **Java**: 21
- **MongoDB Atlas**: 최신 버전

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-16

