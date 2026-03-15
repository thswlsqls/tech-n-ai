# api-chatbot 모듈 세션 타이틀 자동 생성 개선 설계서 작성 프롬프트

## 역할 정의

당신은 Spring Boot 3.4.1과 LangChain4j 1.0.0 기반 RAG 챗봇 시스템의 백엔드 아키텍트입니다.
LLM 기반 멀티턴 채팅에서 세션 타이틀을 자동 생성하는 기능의 **개선 설계서**를 작성하세요.

---

## 프로젝트 컨텍스트

### 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1, Spring Cloud 2024.0.0 |
| LLM | OpenAI GPT-4o-mini (via LangChain4j 1.0.0) |
| Database (Write) | Aurora MySQL (MariaDB), JPA + QueryDSL |
| Database (Read) | MongoDB Atlas 8.0.x |
| 동기화 | Kafka 기반 CQRS (Aurora → Kafka → MongoDB) |
| ID 전략 | TSID (@Tsid annotation) |
| 비동기 | Spring @Async / CompletableFuture |

### 현재 구현 상태

#### 세션 생성 흐름 (현재)

```
POST /api/v1/chatbot (conversationId 없음)
  → ChatbotController.chat()
    → ChatbotFacade.chat()
      → ChatbotServiceImpl.generateResponse()
        → getOrCreateSession()
          → ConversationSessionServiceImpl.createSession(userId, null)  // title = null
            → Aurora MySQL 저장
            → Kafka ConversationSessionCreatedEvent 발행
            → MongoDB 동기화
        → Intent 분류 → 응답 생성
        → saveCurrentMessages() → updateLastMessageAt()
        → ChatResponse(response, conversationId, sources) 반환  // title 없음
```

#### 현재 문제점

1. **title이 항상 null**: `createSession(userId, null)` 호출 시 title 파라미터가 null로 전달됨
2. **ChatResponse에 title 필드 없음**: 새 세션 생성 시 클라이언트가 타이틀을 알 수 없음
3. **수동 변경 불가**: 세션 타이틀 수정 API 엔드포인트가 존재하지 않음

#### 핵심 파일 목록

| 파일 | 역할 |
|------|------|
| `api/chatbot/.../controller/ChatbotController.java` | REST 컨트롤러 (5개 엔드포인트) |
| `api/chatbot/.../facade/ChatbotFacade.java` | 파사드 (얇은 위임 레이어) |
| `api/chatbot/.../service/ChatbotServiceImpl.java` | 핵심 비즈니스 로직 (의도 분류, RAG, 메시지 저장) |
| `api/chatbot/.../service/ConversationSessionServiceImpl.java` | 세션 CRUD + Kafka 이벤트 발행 |
| `api/chatbot/.../service/ConversationMessageServiceImpl.java` | 메시지 저장/조회 (CQRS) |
| `api/chatbot/.../service/LLMService.java` | LLM 호출 인터페이스 |
| `api/chatbot/.../config/LangChain4jConfig.java` | ChatModel, EmbeddingModel Bean 설정 |
| `api/chatbot/.../dto/response/ChatResponse.java` | 채팅 응답 DTO (record) |
| `api/chatbot/.../dto/response/SessionResponse.java` | 세션 응답 DTO (record, title 필드 포함) |
| `domain/aurora/.../entity/chatbot/ConversationSessionEntity.java` | Aurora 세션 엔티티 (title 컬럼 존재) |
| `domain/mongodb/.../document/ConversationSessionDocument.java` | MongoDB 세션 문서 (title 필드 존재) |
| `common/kafka/.../event/ConversationSessionUpdatedEvent.java` | 세션 업데이트 이벤트 (updatedFields Map) |
| `common/kafka/.../sync/ConversationSyncServiceImpl.java` | CQRS 동기화 (title 변경 지원) |

---

## 개선 요구사항

### 핵심 기능

1. **비동기 타이틀 자동 생성**: 새 세션의 첫 메시지-응답 쌍 완료 후, 별도 스레드에서 LLM을 호출하여 3~5단어 타이틀을 생성하고 세션에 저장
2. **ChatResponse에 title 필드 추가**: 새 세션 생성 시 생성된 title을 응답에 포함
3. **세션 타이틀 수동 변경 API**: `PATCH /api/v1/chatbot/sessions/{sessionId}/title` 엔드포인트 추가

### 설계 결정사항 (확정)

| 항목 | 결정 |
|------|------|
| 타이틀 생성 시점 | 첫 사용자 메시지 + 첫 AI 응답 완료 후 비동기 생성 |
| 타이틀 길이 | 3~5단어 |
| 타이틀 언어 | 사용자 메시지의 언어를 따름 (프롬프트에서 지시) |
| ChatResponse 변경 | title 필드 추가 |
| 수동 변경 | PATCH 엔드포인트 추가 |
| LLM 모델 | 기존 ChatModel Bean 재사용 (GPT-4o-mini, 비용 효율적) |

---

## 설계 원칙 및 제약조건

### 준수 원칙

- **객체지향 설계**: 단일 책임 원칙에 따라 타이틀 생성 로직을 별도 서비스로 분리
- **SOLID 원칙**: 인터페이스 기반 추상화, 의존성 역전
- **클린코드**: 명확한 네이밍, 작은 메서드, 부수효과 최소화
- **기존 패턴 준수**: 프로젝트의 Service/ServiceImpl, Reader/Writer Repository, Kafka CQRS 패턴 따르기

### 제약조건

- 오버엔지니어링 금지: 필요 최소한의 변경만 수행
- 기존 코드 변경 최소화: 새로운 클래스 추가 우선, 기존 코드는 꼭 필요한 부분만 수정
- 비동기 타이틀 생성 실패 시에도 메인 채팅 흐름에 영향 없어야 함

---

## 업계 표준 참고사항

### LLM 채팅 세션 타이틀 자동 생성 베스트 프랙티스

다음은 주요 LLM 채팅 제품들의 세션 타이틀 생성 방식입니다. 설계 시 참고하세요.

#### 1. 주요 제품 구현 방식

| 제품 | 방식 | 생성 시점 | 비고 |
|------|------|-----------|------|
| ChatGPT (OpenAI) | 비동기 | 첫 메시지-응답 완료 후 | 별도 내부 API (`conversation/gen_title`) 호출 |
| Microsoft 365 Copilot | 비동기 | 첫 응답 후 | 2025년 3월부터 LLM 기반 타이틀 생성 도입 |
| Claude (Anthropic) | 비동기 | 대화 내용 기반 | 사이드바에 자동 표시, 수동 이름 변경 가능 |
| Open WebUI | 비동기 | 설정 가능 | `TITLE_GENERATION_PROMPT_TEMPLATE` 환경 변수로 프롬프트 커스터마이징 |

**공통 패턴**: 모든 주요 제품이 **비동기 방식**을 채택하여 메인 응답 흐름에 지연을 주지 않음.

#### 2. 타이틀 생성 프롬프트 베스트 프랙티스

검증된 프롬프트 패턴 (출처: OpenAI Community, Open WebUI):

```
핵심 요소:
- 3~5단어로 제한
- 대화의 핵심 주제 추출
- 사용자 메시지 언어로 작성
- 인용부호, 특수 서식 사용 금지
- JSON 형식 출력 시 일관성 향상 ({"title": "..."})
- 토큰 제한 (15~30 tokens)으로 장황한 출력 방지
- 프롬프트를 대화 내용 뒤에 배치하면 LLM 지시 따르기 개선
```

**출처**:
- [OpenAI Community - Prompt to get ChatGPT API to write concise chat titles](https://community.openai.com/t/prompt-to-get-chatgpt-api-to-write-concise-chat-titles-as-it-does-in-chatgpt-chat-application/85644)
- [Open WebUI - Title Generation Prompt Discussion](https://github.com/open-webui/open-webui/discussions/1692)
- [Open WebUI - Environment Variable Configuration](https://docs.openwebui.com/getting-started/env-configuration/)

### LangChain4j 관련 가이드

LangChain4j는 타이틀 생성을 위한 별도 내장 기능을 제공하지 않습니다. 아래 접근 방식으로 커스텀 구현해야 합니다.

#### 권장 구현 패턴 (LangChain4j AI Services 활용)

```java
// LangChain4j AI Services를 활용한 타이틀 생성 인터페이스 예시
interface TitleGenerator {
    @UserMessage("Generate a concise 3-5 word title for this conversation: {{message}}")
    String generateTitle(@V("message") String firstMessage);
}
```

- `ChatModel` Bean을 재사용하여 별도의 LLM 설정 불필요
- `@Async` + `CompletableFuture`로 비동기 실행
- 실패 시 graceful 처리 (null 또는 기본 타이틀 유지)

**출처**:
- [LangChain4j AI Services Tutorial](https://docs.langchain4j.dev/tutorials/ai-services/)
- [LangChain4j Chat Memory Tutorial](https://docs.langchain4j.dev/tutorials/chat-memory/)
- [LangChain4j Spring Boot Integration](https://docs.langchain4j.dev/tutorials/spring-boot-integration/)

---

## 설계서 작성 지시

위의 컨텍스트를 바탕으로 아래 구조에 따라 설계서를 작성하세요.

### 출력 구조

```markdown
# 세션 타이틀 자동 생성 개선 설계서

## 1. 개요
- 개선 배경, 목표, 범위

## 2. 현재 상태 분석
- 현재 세션 생성/관리 흐름
- 문제점 요약

## 3. 개선 설계

### 3.1 비동기 타이틀 생성 서비스
- SessionTitleGenerationService 인터페이스 설계
- 타이틀 생성 프롬프트 설계 (언어 감지 포함)
- 비동기 실행 전략 (@Async + CompletableFuture)
- 에러 처리 전략 (실패 시 graceful fallback)

### 3.2 ChatbotServiceImpl 변경
- generateResponse() 메서드 수정 사항
- 새 세션 판별 및 비동기 타이틀 생성 호출 시점

### 3.3 ChatResponse DTO 변경
- title 필드 추가
- 기존 세션 vs 새 세션 응답 차이

### 3.4 세션 타이틀 수동 변경 API
- PATCH /api/v1/chatbot/sessions/{sessionId}/title
- 요청/응답 스펙
- ConversationSessionServiceImpl 수정사항

### 3.5 CQRS 이벤트 흐름
- 타이틀 변경 시 Kafka ConversationSessionUpdatedEvent 발행
- MongoDB 동기화 (updateSessionDocumentFields에서 title 처리 - 이미 구현됨)

## 4. 수정 대상 파일 목록
- 신규 생성 파일
- 기존 수정 파일 (변경 내용 요약)

## 5. API 명세 업데이트
- api-chatbot-specification.md 변경 내용
- 변경 전/후 비교

## 6. 테스트 계획
- 단위 테스트 시나리오
- 통합 테스트 시나리오
```

### 작성 지침

1. **구체적인 코드 수준의 설계**: 각 클래스/메서드의 시그니처, 주요 로직 흐름을 명시하세요.
2. **기존 패턴 따르기**: 프로젝트의 Service 인터페이스 + ServiceImpl 패턴, Kafka 이벤트 패턴, record DTO 패턴을 그대로 따르세요.
3. **변경 최소화**: 기존 코드 수정은 필요한 부분만, 새 기능은 새 클래스로 분리하세요.
4. **비동기 안전성**: 타이틀 생성 실패가 메인 채팅 흐름을 절대 중단시키지 않아야 합니다.
5. **CQRS 정합성**: Aurora 업데이트 → Kafka 이벤트 → MongoDB 동기화 흐름을 반드시 포함하세요.

### 주의사항

- LangChain4j AI Services 패턴을 활용하되, 별도 ChatModel Bean을 생성하지 마세요 (기존 Bean 재사용).
- 오버엔지니어링 금지: 재시도 로직, 캐싱, 큐 등 불필요한 인프라 추가 금지.
- `api-chatbot-specification.md` 업데이트 내용을 반드시 포함하세요.
