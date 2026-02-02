# 멀티턴 대화 히스토리 관리 설계 항목 추가 프롬프트

## 역할 정의

당신은 **백엔드 아키텍트**이자 **LLM 통합 전문가**입니다. langchain4j, OpenAI API, Anthropic API의 공식 문서를 기반으로 각 LLM Provider별 멀티턴 대화 히스토리 관리 설계를 추가할 수 있는 전문가입니다.

## 작업 목표

`docs/step12/rag-chatbot-design.md` 설계서에 **멀티턴 대화 히스토리 관리** 설계 항목을 추가합니다. 현재 설계서는 단일 턴 대화만 고려하고 있으며, 멀티턴 대화 히스토리 관리 기능이 누락되어 있습니다.

## 필수 참고 자료 (공식 기술 출처만 사용)

### 1. langchain4j 공식 문서
- **ChatMemory 인터페이스**: https://docs.langchain4j.dev/apidocs/dev/langchain4j/memory/ChatMemory.html
- **ChatMemoryStore 인터페이스**: https://docs.langchain4j.dev/tutorials/chat-memory
- **MessageWindowChatMemory**: 메시지 개수 기반 메모리 관리
- **TokenWindowChatMemory**: 토큰 수 기반 메모리 관리
- **Memory vs History 구분**: Memory는 LLM에 전달되는 컨텍스트, History는 전체 대화 기록
- **ChatMemoryProvider**: 사용자별/대화별 메모리 관리

### 2. OpenAI API 공식 문서
- **Chat Completions API**: https://platform.openai.com/docs/api-reference/chat
- **Multi-turn Conversation**: messages 배열로 대화 히스토리 관리
- **Message Roles**: `system`, `user`, `assistant` 역할 지원
- **Messages Array**: messages 배열 내에 system, user, assistant 메시지 모두 포함
- **Stored Completions**: `store: true` 옵션으로 대화 저장
- **Responses API**: `previous_response_id`를 통한 상태 관리 (향후 고려)
- **Context Window**: 모델별 최대 컨텍스트 길이 제한 (예: GPT-4o-mini 128K, GPT-4o 128K)

### 3. Anthropic Claude API 공식 문서
- **Messages API**: https://docs.anthropic.com/en/api/messages
- **Multi-turn Conversation**: messages 배열로 대화 히스토리 관리
- **Message Roles**: messages 배열에는 `user`와 `assistant`만 포함
- **System Parameter**: `system` 파라미터가 messages 배열 밖에 별도로 존재 (messages 내부에 system role 없음)
- **Message Limits**: 최대 100,000개 메시지 지원
- **Message Merging**: 연속된 같은 role의 메시지는 자동 병합
- **Prefilling**: 마지막 메시지가 assistant role이면 해당 메시지에서 이어서 생성
- **Context Window**: 모델별 최대 컨텍스트 길이 제한 (예: Claude 3.5 Sonnet 200K, Claude 3 Opus 200K)

### 4. MongoDB Atlas 공식 문서
- **MongoDB Atlas 공식 문서**: https://www.mongodb.com/docs/atlas/
- **TTL 인덱스**: 세션 만료 정책 구현

### 5. Spring Boot 공식 문서
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Spring Data MongoDB**: https://spring.io/projects/spring-data-mongodb

## 설계 항목 추가 요구사항

### 1. 대화 세션 관리 설계

#### 1.1 대화 세션 엔티티/Document 설계
- **Command Side (Aurora MySQL)**: `ConversationSession` 엔티티
  - 필드: `sessionId` (TSID Primary Key), `userId`, `title` (선택), `createdAt`, `updatedAt`, `lastMessageAt`, `isActive` (boolean)
  - 인덱스: `userId + isActive + lastMessageAt` 복합 인덱스
  - Soft Delete 지원: `deleteYn`, `deletedAt`
  
- **Query Side (MongoDB Atlas)**: `ConversationSessionDocument`
  - 필드: `sessionId`, `userId`, `title`, `createdAt`, `updatedAt`, `lastMessageAt`, `isActive`
  - 인덱스: `userId + isActive + lastMessageAt` 복합 인덱스
  - TTL 인덱스: 비활성 세션 자동 삭제 (예: 90일)

#### 1.2 대화 메시지 히스토리 저장소 설계
- **Command Side (Aurora MySQL)**: `ConversationMessage` 엔티티
  - 필드: `messageId` (TSID Primary Key), `sessionId` (Foreign Key), `role` (USER/ASSISTANT/SYSTEM), `content`, `tokenCount` (선택), `createdAt`, `sequenceNumber` (대화 순서)
  - 인덱스: `sessionId + sequenceNumber` 복합 인덱스 (순서 조회 최적화)
  
- **Query Side (MongoDB Atlas)**: `ConversationMessageDocument`
  - 필드: `messageId`, `sessionId`, `role`, `content`, `tokenCount`, `createdAt`, `sequenceNumber`
  - 인덱스: `sessionId + sequenceNumber` 복합 인덱스
  - TTL 인덱스: 오래된 메시지 자동 삭제 (예: 1년)

#### 1.3 세션 생성 및 관리 전략
- **세션 생성**: 사용자가 첫 메시지를 보낼 때 자동 생성
- **세션 식별**: `conversationId` (UUID 또는 TSID)로 식별
- **세션 활성화**: 마지막 메시지 시간 기준으로 활성 세션 판단 (예: 30분 이내)
- **세션 만료**: 비활성 세션은 일정 기간 후 자동 만료 (예: 90일)

### 2. langchain4j ChatMemory 통합 설계

#### 2.1 ChatMemoryStore 구현
- **역할**: langchain4j의 `ChatMemoryStore` 인터페이스 구현
- **구현 클래스**: `MongoDbChatMemoryStore` 또는 `AuroraChatMemoryStore`
- **메서드 구현**:
  - `getMessages(memoryId)`: 세션 ID로 메시지 목록 조회
  - `updateMessages(memoryId, List<ChatMessage>)`: 메시지 목록 업데이트
  - `deleteMessages(memoryId)`: 세션 메시지 삭제

#### 2.2 ChatMemory 전략 선택
- **MessageWindowChatMemory**: 최근 N개 메시지만 유지
  - 설정: `maxMessages` (예: 10개)
  - 장점: 단순하고 예측 가능
  - 단점: 토큰 수 예측 어려움
  
- **TokenWindowChatMemory**: 토큰 수 기준으로 메시지 유지
  - 설정: `maxTokens` (예: 2000 토큰)
  - 장점: 토큰 제한 준수 보장
  - 단점: TokenCountEstimator 필요

#### 2.3 ChatMemoryProvider 구현
- **역할**: 사용자별/세션별 ChatMemory 인스턴스 제공
- **구현 클래스**: `ConversationChatMemoryProvider`
- **메서드**: `get(memoryId)` - 세션 ID로 ChatMemory 반환

#### 2.4 Provider별 메시지 포맷 변환 설계
- **필요성**: langchain4j의 ChatMessage를 각 Provider API 포맷으로 변환 필요
- **변환 전략**:
  - **OpenAI 변환**: ChatMessage → OpenAI messages 배열
    - SystemMessage → `{"role": "system", "content": "..."}`
    - UserMessage → `{"role": "user", "content": "..."}`
    - AiMessage → `{"role": "assistant", "content": "..."}`
  - **Anthropic 변환**: ChatMessage → Anthropic messages 배열 + system 파라미터
    - SystemMessage → `system` 파라미터로 추출 (messages 배열 밖으로)
    - UserMessage → `{"role": "user", "content": "..."}`
    - AiMessage → `{"role": "assistant", "content": "..."}`
    - **주의**: SystemMessage는 messages 배열에 포함하지 않고 별도 system 파라미터로 전달
- **구현 클래스**: `MessageFormatConverter` 인터페이스 및 Provider별 구현
  - `OpenAiMessageConverter`: OpenAI 포맷 변환
  - `AnthropicMessageConverter`: Anthropic 포맷 변환

### 3. 대화 히스토리 저장 및 조회 설계

#### 3.1 메시지 저장 전략
- **저장 시점**: LLM 응답 생성 후 즉시 저장
- **저장 위치**: Command Side (Aurora MySQL)에 먼저 저장
- **동기화**: Kafka 이벤트를 통한 Query Side (MongoDB Atlas) 동기화
- **트랜잭션**: 세션 생성/업데이트와 메시지 저장을 동일 트랜잭션으로 처리

#### 3.2 히스토리 조회 전략
- **조회 위치**: Query Side (MongoDB Atlas) 우선 사용 (읽기 최적화)
- **조회 범위**: 세션별 최근 N개 메시지 또는 전체 메시지
- **정렬**: `sequenceNumber` 기준 오름차순 정렬
- **페이징**: 대화 히스토리가 긴 경우 페이징 지원

#### 3.3 Memory vs History 구분
- **History (전체 대화 기록)**: 모든 메시지를 `ConversationMessage` 테이블에 저장
- **Memory (LLM 컨텍스트)**: ChatMemory가 관리하는 메시지만 LLM에 전달
- **관계**: History에서 Memory로 메시지 선택 (최근 메시지 우선, 토큰 제한 고려)

### 4. 토큰 관리 확장 설계

#### 4.1 히스토리 포함 토큰 계산
- **계산 대상**: 
  - 시스템 프롬프트 토큰
  - 대화 히스토리 토큰 (ChatMemory에서 관리하는 메시지)
  - 현재 사용자 입력 토큰
  - 검색 결과 컨텍스트 토큰 (RAG 사용 시)
- **토큰 예측**: 각 메시지의 토큰 수를 저장하여 누적 계산
- **Provider별 토큰 제한**:
  - **OpenAI**: 모델별 최대 컨텍스트 길이 (예: GPT-4o-mini 128K, GPT-4o 128K)
  - **Anthropic**: 모델별 최대 컨텍스트 길이 (예: Claude 3.5 Sonnet 200K, Claude 3 Opus 200K)
  - 선택한 Provider의 모델에 맞는 토큰 제한 적용

#### 4.2 히스토리 압축 전략
- **메시지 제거**: 오래된 메시지부터 제거 (FIFO)
- **메시지 요약**: 오래된 메시지를 요약하여 토큰 수 절감 (향후 고려)
- **시스템 메시지 우선 유지**: SystemMessage는 항상 유지

### 5. API 엔드포인트 확장 설계

#### 5.1 ChatRequest 확장
- **conversationId 필드**: 기존 설계에 이미 포함되어 있으나, 필수/선택 여부 명확화
- **새 세션 생성**: `conversationId`가 없으면 새 세션 생성
- **기존 세션 사용**: `conversationId`가 있으면 해당 세션에 메시지 추가

#### 5.2 ChatResponse 확장
- **conversationId 필드 추가**: 생성된 또는 사용된 세션 ID 반환
- **sessionInfo 필드 추가** (선택): 세션 메타데이터 (제목, 생성일, 메시지 수 등)

#### 5.3 추가 API 엔드포인트
- **GET /api/v1/chatbot/sessions**: 사용자의 대화 세션 목록 조회
- **GET /api/v1/chatbot/sessions/{sessionId}**: 특정 세션 정보 조회
- **GET /api/v1/chatbot/sessions/{sessionId}/messages**: 세션의 메시지 히스토리 조회
- **DELETE /api/v1/chatbot/sessions/{sessionId}**: 세션 삭제 (Soft Delete)
- **PUT /api/v1/chatbot/sessions/{sessionId}/title**: 세션 제목 수정

### 6. 서비스 레이어 확장 설계

#### 6.1 ConversationSessionService
- **역할**: 대화 세션 생성, 조회, 수정, 삭제
- **메서드**:
  - `createSession(userId, title)`: 새 세션 생성
  - `getSession(sessionId, userId)`: 세션 조회 (권한 검증)
  - `updateSession(sessionId, userId, title)`: 세션 제목 수정
  - `deleteSession(sessionId, userId)`: 세션 삭제
  - `listSessions(userId, pageable)`: 사용자 세션 목록 조회
  - `getActiveSession(userId)`: 사용자의 활성 세션 조회

#### 6.2 ConversationMessageService
- **역할**: 대화 메시지 저장 및 조회
- **메서드**:
  - `saveMessage(sessionId, role, content, tokenCount)`: 메시지 저장
  - `getMessages(sessionId, limit)`: 세션의 메시지 목록 조회
  - `getMessagesForMemory(sessionId, maxTokens)`: ChatMemory용 메시지 조회 (토큰 제한 고려)

#### 6.3 ChatbotService 확장
- **기존 메서드 수정**: `generateResponse(ChatRequest)` 메서드에 히스토리 관리 로직 추가
- **히스토리 로드**: `conversationId`가 있으면 해당 세션의 히스토리 로드
- **ChatMemory 통합**: 로드한 히스토리를 ChatMemory에 추가
- **Provider별 메시지 변환**: ChatMemory의 메시지를 선택한 Provider 포맷으로 변환
- **메시지 저장**: 사용자 메시지와 LLM 응답을 ConversationMessage에 저장

#### 6.4 Provider별 LLM 호출 전략
- **OpenAI 호출**:
  - ChatMemory의 모든 메시지를 OpenAI messages 배열로 변환
  - SystemMessage는 messages 배열 내부에 포함
  - 최대 컨텍스트 길이 확인 (예: GPT-4o-mini 128K)
  
- **Anthropic 호출**:
  - ChatMemory의 SystemMessage를 system 파라미터로 추출
  - UserMessage와 AiMessage만 messages 배열에 포함
  - 최대 컨텍스트 길이 확인 (예: Claude 3.5 Sonnet 200K)
  - 연속된 같은 role 메시지 병합 고려 (자동 처리되지만 인지 필요)

### 7. 비용 통제 전략 확장

#### 7.1 히스토리 기반 비용 계산
- **토큰 사용량 추적**: 각 메시지의 토큰 수 저장
- **세션별 비용 집계**: 세션별 총 토큰 사용량 계산
- **사용자별 비용 집계**: 사용자별 일일/월별 토큰 사용량 제한

#### 7.2 히스토리 캐싱 전략
- **Redis 캐싱**: 활성 세션의 ChatMemory를 Redis에 캐싱
- **캐시 키**: `chatbot:memory:{sessionId}`
- **캐시 만료**: 세션 비활성화 시 캐시 만료 (예: 30분)

### 8. 데이터베이스 스키마 설계

#### 8.1 Aurora MySQL 스키마
- **스키마**: `chatbot` 스키마 생성
- **테이블**:
  - `conversation_sessions`: 대화 세션 테이블
  - `conversation_messages`: 대화 메시지 테이블
- **인덱스**: 성능 최적화를 위한 복합 인덱스 설계
- **Soft Delete**: `deleteYn`, `deletedAt` 필드 추가

#### 8.2 MongoDB Atlas 컬렉션
- **컬렉션**:
  - `conversation_sessions`: 대화 세션 Document
  - `conversation_messages`: 대화 메시지 Document
- **인덱스**: ESR 규칙 준수한 복합 인덱스
- **TTL 인덱스**: 자동 만료 정책 구현

### 9. Kafka 이벤트 동기화 설계

#### 9.1 이벤트 모델
- **ConversationSessionCreatedEvent**: 세션 생성 이벤트
- **ConversationSessionUpdatedEvent**: 세션 수정 이벤트
- **ConversationSessionDeletedEvent**: 세션 삭제 이벤트
- **ConversationMessageCreatedEvent**: 메시지 생성 이벤트

#### 9.2 동기화 서비스
- **ConversationSyncService**: Command Side → Query Side 동기화
- **구현 패턴**: 기존 `BookmarkSyncService` 패턴 참고

### 10. Provider별 상세 설계

#### 10.1 OpenAI API 멀티턴 대화 설계
- **메시지 구조**: 
  - messages 배열에 system, user, assistant 모두 포함
  - SystemMessage는 `{"role": "system", "content": "..."}` 형태
  - UserMessage는 `{"role": "user", "content": "..."}` 형태
  - AiMessage는 `{"role": "assistant", "content": "..."}` 형태
- **컨텍스트 길이**: 모델별 제한 (GPT-4o-mini: 128K, GPT-4o: 128K)
- **토큰 계산**: 모든 메시지의 토큰 수 합산하여 컨텍스트 길이 확인
- **구현 클래스**: `OpenAiChatModel` 사용, langchain4j의 `OpenAiChatModel` 활용

#### 10.2 Anthropic Claude API 멀티턴 대화 설계
- **메시지 구조**:
  - system 파라미터: messages 배열 밖에 별도 존재 (최상위 레벨)
  - messages 배열: user와 assistant만 포함
  - SystemMessage는 system 파라미터로 변환 (messages 배열에 포함하지 않음)
  - UserMessage는 `{"role": "user", "content": "..."}` 형태
  - AiMessage는 `{"role": "assistant", "content": "..."}` 형태
- **메시지 병합**: 연속된 같은 role의 메시지는 Anthropic API가 자동 병합 (인지 필요)
- **컨텍스트 길이**: 모델별 제한 (Claude 3.5 Sonnet: 200K, Claude 3 Opus: 200K)
- **토큰 계산**: system 파라미터 + messages 배열의 토큰 수 합산
- **Prefilling 지원**: 마지막 메시지가 assistant role이면 해당 메시지에서 이어서 생성 가능
- **구현 클래스**: `AnthropicChatModel` 사용, langchain4j의 `AnthropicChatModel` 활용

#### 10.3 Provider별 변환 로직 상세 설계
- **MessageFormatConverter 인터페이스**:
  ```java
  interface MessageFormatConverter {
      Object convertToProviderFormat(List<ChatMessage> messages, String systemPrompt);
      List<ChatMessage> convertFromProviderFormat(Object providerResponse);
  }
  ```
- **OpenAiMessageConverter 구현**:
  - SystemMessage → messages 배열 내부에 system role로 포함
  - UserMessage → messages 배열에 user role로 포함
  - AiMessage → messages 배열에 assistant role로 포함
  - 모든 메시지를 하나의 messages 배열로 구성
- **AnthropicMessageConverter 구현**:
  - SystemMessage → system 파라미터로 추출 (messages 배열 밖으로)
  - UserMessage → messages 배열에 user role로 포함
  - AiMessage → messages 배열에 assistant role로 포함
  - system 파라미터와 messages 배열을 분리하여 전달
  - **주의**: SystemMessage가 여러 개인 경우 첫 번째만 사용하거나 병합 전략 필요

#### 10.4 Provider 선택 및 전환 전략
- **설정 기반 선택**: application.yml에서 `chatbot.provider` 설정으로 선택
- **런타임 전환**: 동일 세션 내에서 Provider 전환 시 메시지 포맷 변환 필요
- **히스토리 호환성**: Provider 전환 시에도 기존 히스토리 유지 (저장된 메시지는 role 기반으로 변환 가능)

### 11. 구현 가이드

#### 11.1 구현 순서
1. 데이터베이스 스키마 생성 (Aurora MySQL, MongoDB Atlas)
2. Entity/Document 클래스 구현
3. Repository 인터페이스 구현
4. ChatMemoryStore 구현
5. MessageFormatConverter 인터페이스 및 Provider별 구현 (OpenAI, Anthropic)
6. ConversationSessionService, ConversationMessageService 구현
7. ChatbotService 확장 (히스토리 관리 로직 추가, Provider별 변환 로직 통합)
8. API 엔드포인트 확장
9. Kafka 이벤트 발행 및 동기화 구현

#### 11.2 테스트 전략
- **단위 테스트**: 각 서비스 메서드별 테스트
- **Provider별 변환 테스트**: OpenAI 변환, Anthropic 변환 각각 테스트
- **통합 테스트**: 세션 생성 → 메시지 저장 → 히스토리 조회 → LLM 호출 플로우 테스트 (OpenAI, Anthropic 각각)
- **Provider 전환 테스트**: 세션 중간에 Provider 전환 시나리오 테스트
- **성능 테스트**: 대량 메시지 저장 및 조회 성능 테스트

## 설계서 작성 지시사항

### 1. 설계서 구조
다음 섹션을 `docs/step12/rag-chatbot-design.md`에 추가하세요:

- **목차에 추가**: "멀티턴 대화 히스토리 관리 설계" 섹션
- **위치**: "프롬프트 체인 구축 설계" 섹션 다음, "비용 통제 전략 설계" 섹션 이전

### 2. 설계 내용 포함 사항

#### 2.1 개요
- 멀티턴 대화 히스토리 관리의 필요성
- Memory vs History 구분 설명
- langchain4j ChatMemory 활용 전략
- Provider별 메시지 포맷 차이점 (OpenAI vs Anthropic)
  - OpenAI: messages 배열 내부에 system role 포함
  - Anthropic: system 파라미터가 messages 배열 밖에 별도 존재

#### 2.2 대화 세션 관리 설계
- ConversationSession 엔티티/Document 설계
- 세션 생성 및 관리 전략
- 세션 만료 정책

#### 2.3 대화 메시지 히스토리 저장소 설계
- ConversationMessage 엔티티/Document 설계
- 메시지 저장 및 조회 전략
- 히스토리 조회 최적화

#### 2.4 langchain4j ChatMemory 통합 설계
- ChatMemoryStore 구현 설계
- ChatMemory 전략 선택 (MessageWindowChatMemory vs TokenWindowChatMemory)
- ChatMemoryProvider 구현 설계
- Provider별 메시지 포맷 변환 설계 (OpenAI, Anthropic)

#### 2.5 토큰 관리 확장 설계
- 히스토리 포함 토큰 계산 (Provider별 토큰 제한 고려)
- 히스토리 압축 전략
- Provider별 컨텍스트 길이 제한 비교 (OpenAI vs Anthropic)

#### 2.6 API 엔드포인트 확장 설계
- ChatRequest/ChatResponse 확장
- 추가 API 엔드포인트 설계 (세션 관리, 히스토리 조회)

#### 2.7 서비스 레이어 확장 설계
- ConversationSessionService 설계
- ConversationMessageService 설계
- ChatbotService 확장 설계
- Provider별 LLM 호출 전략 설계 (OpenAI, Anthropic)

#### 2.8 데이터베이스 스키마 설계
- Aurora MySQL 스키마 설계 (테이블, 인덱스)
- MongoDB Atlas 컬렉션 설계 (Document, 인덱스, TTL)

#### 2.9 Kafka 이벤트 동기화 설계
- 이벤트 모델 설계
- 동기화 서비스 설계

#### 2.10 Provider별 상세 설계
- OpenAI API 멀티턴 대화 설계 (메시지 구조, 컨텍스트 길이, 토큰 계산)
- Anthropic Claude API 멀티턴 대화 설계 (system 파라미터 분리, 메시지 병합, Prefilling)
- Provider별 변환 로직 상세 설계 (MessageFormatConverter 인터페이스 및 구현)
- Provider 선택 및 전환 전략

#### 2.11 구현 가이드
- 구현 순서 (Provider별 변환 로직 포함)
- 코드 예시 (pseudocode, Provider별 변환 예시 포함)
- 테스트 전략 (Provider별 테스트, Provider 전환 테스트)

### 3. 다이어그램 추가
- **시퀀스 다이어그램**: 멀티턴 대화 플로우 (세션 생성 → 메시지 저장 → 히스토리 로드 → Provider별 변환 → LLM 호출)
  - OpenAI 플로우
  - Anthropic 플로우 (system 파라미터 분리 과정 포함)
- **데이터베이스 ERD**: ConversationSession, ConversationMessage 테이블 관계
- **아키텍처 다이어그램**: ChatMemory 통합 아키텍처 (Provider별 변환 레이어 포함)
- **Provider별 메시지 변환 다이어그램**: ChatMessage → OpenAI/Anthropic 포맷 변환 과정

### 4. 코드 예시
- **Entity/Document 클래스**: Java 코드 예시
- **ChatMemoryStore 구현**: Java 코드 예시
- **MessageFormatConverter 구현**: Provider별 변환 로직 Java 코드 예시
  - OpenAI 변환 예시
  - Anthropic 변환 예시 (system 파라미터 분리 로직 포함)
- **Service 메서드**: pseudocode 형식
- **API 엔드포인트**: Controller 메서드 시그니처

## 제약사항 및 주의사항

### 1. 공식 문서만 참고
- **langchain4j**: 공식 GitHub 및 문서만 참고 (https://github.com/langchain4j/langchain4j, https://docs.langchain4j.dev/)
- **OpenAI API**: 공식 API 문서만 참고 (https://platform.openai.com/docs/api-reference/chat)
- **Anthropic API**: 공식 API 문서만 참고 (https://docs.anthropic.com/en/api/messages)
- **MongoDB Atlas**: 공식 문서만 참고 (https://www.mongodb.com/docs/atlas/)
- **추측 금지**: 공식 문서에 없는 내용은 추측하지 말고, 문서에 명시된 내용만 사용
- **Provider별 차이점 명확히 구분**: OpenAI와 Anthropic의 메시지 포맷 차이를 정확히 반영

### 2. 기존 설계와의 일관성
- **CQRS 패턴 준수**: Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 분리
- **Kafka 동기화**: 기존 `BookmarkSyncService` 패턴 참고
- **API 패턴**: 기존 `api-contest`, `api-news` 모듈 패턴 준수
- **코드 스타일**: 기존 프로젝트의 코드 스타일 및 명명 규칙 준수

### 3. 오버엔지니어링 방지
- **최소 구현**: 현재 필요한 기능만 설계 (향후 확장 가능한 구조)
- **복잡한 기능 제외**: 
  - 메시지 요약 기능은 향후 고려 사항으로만 언급
  - 복잡한 에이전트 시스템은 제외
  - 실시간 스트리밍은 제외

### 4. 클린코드 원칙
- **단일 책임 원칙 (SRP)**: 각 서비스는 하나의 책임만 담당
- **의존성 역전 원칙 (DIP)**: 인터페이스 기반 설계
- **개방-폐쇄 원칙 (OCP)**: 확장 가능한 구조

## 검증 기준

설계서 작성 완료 후 다음 사항을 검증하세요:

1. **공식 문서 준수**: 모든 기술 내용이 공식 문서 기반인지 확인 (OpenAI, Anthropic 공식 문서 포함)
2. **Provider별 설계 완전성**: OpenAI와 Anthropic 각각에 대한 상세 설계가 포함되었는지 확인
3. **메시지 포맷 차이점 명확성**: OpenAI의 system role vs Anthropic의 system 파라미터 차이가 명확히 설명되었는지 확인
4. **변환 로직 설계**: Provider별 메시지 포맷 변환 로직이 상세히 설계되었는지 확인
5. **설계 완전성**: 위의 모든 설계 항목이 포함되었는지 확인
6. **기존 설계와의 일관성**: 기존 설계서와 패턴이 일치하는지 확인
7. **구현 가능성**: 설계 내용이 실제 구현 가능한지 확인
8. **다이어그램 완성도**: 시퀀스 다이어그램, ERD, 아키텍처 다이어그램, Provider별 변환 다이어그램이 포함되었는지 확인

## 최종 확인 사항

설계서 작성 완료 후 다음을 확인하세요:

- [ ] 멀티턴 대화 히스토리 관리 설계 섹션이 추가되었는가?
- [ ] langchain4j ChatMemory 통합 설계가 포함되었는가?
- [ ] Provider별 메시지 포맷 변환 설계가 포함되었는가? (OpenAI, Anthropic)
- [ ] Provider별 LLM 호출 전략 설계가 포함되었는가? (OpenAI, Anthropic)
- [ ] 대화 세션 및 메시지 저장소 설계가 포함되었는가?
- [ ] API 엔드포인트 확장 설계가 포함되었는가?
- [ ] 데이터베이스 스키마 설계가 포함되었는가?
- [ ] Kafka 이벤트 동기화 설계가 포함되었는가?
- [ ] 모든 참고 자료가 공식 문서인가? (OpenAI, Anthropic 공식 문서 포함)
- [ ] Provider별 차이점이 명확히 구분되어 있는가? (OpenAI의 system role vs Anthropic의 system 파라미터)
- [ ] 기존 설계와의 일관성이 유지되는가?

---

**중요**: 이 프롬프트의 모든 요구사항을 엄격히 준수하여 설계서를 작성하세요. 공식 문서에 없는 내용은 추측하지 말고, 반드시 공식 문서만 참고하세요.
