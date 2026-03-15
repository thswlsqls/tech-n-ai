# 세션 타이틀 자동 생성 개선 설계서

**작성일**: 2026-02-13
**대상 모듈**: api-chatbot
**버전**: v1

---

## 1. 개요

### 1.1 개선 배경

현재 api-chatbot 모듈의 세션 생성 시 `title` 필드가 항상 `null`로 설정됩니다. `ConversationSessionEntity`와 `ConversationSessionDocument` 모두 `title` 필드를 갖고 있고, `SessionResponse`에도 `title`이 포함되어 있지만, 실제로 타이틀을 생성하는 로직이 존재하지 않습니다. 또한 `ChatResponse`에는 `title` 필드 자체가 없어 새 세션 생성 시 클라이언트가 타이틀을 알 수 없습니다.

### 1.2 목표

1. 새 세션의 첫 메시지-응답 쌍 완료 후, LLM을 호출하여 3~5단어의 세션 타이틀을 **비동기**로 자동 생성
2. `ChatResponse`에 `title` 필드를 추가하여 클라이언트에 타이틀 제공
3. `PATCH /api/v1/chatbot/sessions/{sessionId}/title` 엔드포인트를 추가하여 사용자가 타이틀을 수동 변경 가능하도록 지원

### 1.3 범위

| 구분 | 범위 내 | 범위 외 |
|------|---------|---------|
| 타이틀 생성 | 새 세션 첫 메시지-응답 후 비동기 생성 | 기존 세션 타이틀 재생성 |
| API 변경 | ChatResponse title 추가, PATCH 엔드포인트 | 세션 목록/상세 응답 구조 변경 (이미 title 포함) |
| CQRS | title 변경 시 Kafka 이벤트 발행 | MongoDB sync 로직 변경 (이미 title 처리 구현됨) |

---

## 2. 현재 상태 분석

### 2.1 현재 세션 생성 흐름

```
POST /api/v1/chatbot (conversationId 없음)
  → ChatbotController.chat()
    → ChatbotFacade.chat()
      → ChatbotServiceImpl.generateResponse()
        → getOrCreateSession(request, userId)
          → ConversationSessionServiceImpl.createSession(userId, null)  // title = null
            → ConversationSessionEntity 저장 (Aurora MySQL)
            → ConversationSessionCreatedEvent 발행 (Kafka)
            → ConversationSyncServiceImpl.syncSessionCreated() (MongoDB 동기화)
        → Intent 분류 → 응답 생성 (LLM_DIRECT / RAG / WEB_SEARCH / AGENT)
        → saveCurrentMessages(sessionId, chatMemory, userMessage, response)
        → sessionService.updateLastMessageAt(sessionId)
        → ChatResponse(response, conversationId, sources) 반환
```

### 2.2 현재 문제점

| 문제 | 현재 상태 | 영향 |
|------|-----------|------|
| title 항상 null | `createSession(userId, null)` | 세션 목록에서 식별 불가 |
| ChatResponse에 title 없음 | `ChatResponse(response, conversationId, sources)` | 새 세션 생성 시 클라이언트가 타이틀 표시 불가 |
| 타이틀 수동 변경 불가 | 수정 API 엔드포인트 없음 | 사용자 커스터마이징 불가 |

### 2.3 이미 구현된 인프라

타이틀 변경을 위한 CQRS 인프라는 **이미 준비되어 있습니다**:

- **Aurora Entity**: `ConversationSessionEntity.title` (VARCHAR(200), nullable) - 존재
- **MongoDB Document**: `ConversationSessionDocument.title` - 존재
- **Kafka 이벤트**: `ConversationSessionUpdatedEvent.updatedFields` Map에 `"title"` 키 지원
- **MongoDB 동기화**: `ConversationSyncServiceImpl.updateSessionDocumentFields()`에서 `"title"` case 구현됨
- **응답 DTO**: `SessionResponse.title` - 존재
- **비동기 설정**: `common-exception` 모듈에 `@EnableAsync` 설정이 있어 `@Async` 사용 가능

---

## 3. 개선 설계

### 3.1 비동기 타이틀 생성 서비스

#### 3.1.1 인터페이스 설계

```java
package com.ebson.shrimp.tm.demo.api.chatbot.service;

/**
 * 세션 타이틀 생성 서비스 인터페이스
 */
public interface SessionTitleGenerationService {

    /**
     * 첫 메시지-응답 쌍을 기반으로 세션 타이틀을 비동기 생성하고 저장
     *
     * @param sessionId  세션 ID (TSID String)
     * @param userId     사용자 ID
     * @param userMessage 첫 번째 사용자 메시지
     * @param aiResponse  첫 번째 AI 응답
     */
    void generateAndSaveTitleAsync(String sessionId, Long userId,
                                    String userMessage, String aiResponse);
}
```

**설계 의도**:
- 단일 책임 원칙(SRP): 타이틀 생성 로직을 `ChatbotServiceImpl`에서 분리
- 인터페이스 분리 원칙(ISP): 타이틀 생성에 필요한 메서드만 정의
- 의존성 역전 원칙(DIP): `ChatbotServiceImpl`은 인터페이스에만 의존

#### 3.1.2 구현체 설계

```java
package com.ebson.shrimp.tm.demo.api.chatbot.service;

import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTitleGenerationServiceImpl implements SessionTitleGenerationService {

    private final ChatModel chatModel;   // 기존 GPT-4o-mini Bean 재사용
    private final ConversationSessionService sessionService;

    @Async
    @Override
    public void generateAndSaveTitleAsync(String sessionId, Long userId,
                                           String userMessage, String aiResponse) {
        try {
            String title = generateTitle(userMessage, aiResponse);
            sessionService.updateSessionTitle(sessionId, userId, title);
            log.info("Session title generated: sessionId={}, title={}", sessionId, title);
        } catch (Exception e) {
            log.warn("Failed to generate session title: sessionId={}", sessionId, e);
            // 실패해도 메인 채팅 흐름에 영향 없음 (비동기 + 예외 흡수)
        }
    }

    private String generateTitle(String userMessage, String aiResponse) {
        String prompt = buildTitlePrompt(userMessage, aiResponse);
        String rawTitle = chatModel.chat(prompt);
        return sanitizeTitle(rawTitle);
    }

    private String buildTitlePrompt(String userMessage, String aiResponse) {
        return """
            ---BEGIN Conversation---
            User: %s
            Assistant: %s
            ---END Conversation---

            Generate a concise title (3-5 words) that captures the main topic of this conversation.
            Write the title in the same language as the user's message.
            Do not use quotation marks, special formatting, or emojis.
            Respond with only the title text, nothing else.
            """.formatted(
                truncate(userMessage, 300),
                truncate(aiResponse, 300)
            );
    }

    private String sanitizeTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return null;
        }
        String title = rawTitle.strip()
            .replaceAll("^\"|\"$", "")   // 앞뒤 인용부호 제거
            .replaceAll("^'|'$", "");    // 앞뒤 작은따옴표 제거
        // VARCHAR(200) 제약에 맞게 잘라내기
        return title.length() > 200 ? title.substring(0, 200) : title;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
```

#### 3.1.3 타이틀 생성 프롬프트 설계

프롬프트는 업계 베스트 프랙티스를 따릅니다:

| 요소 | 적용 |
|------|------|
| 대화 내용을 프롬프트 앞에 배치 | LLM이 지시를 더 잘 따르도록 (출처: Open WebUI, OpenAI Community) |
| 3~5단어 제한 | 주요 제품 표준 (ChatGPT, Copilot) |
| 사용자 메시지 언어 따르기 | `"Write the title in the same language as the user's message"` |
| 서식 금지 | `"Do not use quotation marks, special formatting, or emojis"` |
| 타이틀만 출력 | `"Respond with only the title text, nothing else"` |
| 입력 truncation | 토큰 비용 절감을 위해 각 300자로 제한 |

**JSON 출력 형식을 사용하지 않는 이유**: 3~5단어의 단순 텍스트 출력에는 JSON 파싱 오버헤드가 불필요합니다. `sanitizeTitle()`에서 인용부호 제거로 충분히 일관된 결과를 얻을 수 있습니다.

#### 3.1.4 비동기 실행 전략

- `@Async` 어노테이션 사용 (Spring 기본 `SimpleAsyncTaskExecutor`)
- `common-exception` 모듈의 `@EnableAsync` 설정이 이미 활성화되어 있으므로 추가 설정 불필요
- 반환 타입: `void` (결과를 기다리지 않음)
- 호출자(`ChatbotServiceImpl`)는 fire-and-forget 방식으로 호출

#### 3.1.5 에러 처리 전략

```
비동기 타이틀 생성 실패 시:
  → catch (Exception e)
    → log.warn() (경고 레벨 로그)
    → 예외 흡수 (throw 하지 않음)
    → title은 null로 유지
    → 세션 목록에서 title 없이 표시
    → 사용자가 필요 시 PATCH API로 수동 설정 가능
```

**실패 가능 원인 및 대응**:

| 원인 | 대응 |
|------|------|
| LLM API 타임아웃 | 기존 ChatModel의 60s 타임아웃 적용, 초과 시 예외 흡수 |
| LLM API 오류 (429, 500 등) | 예외 흡수, title null 유지 |
| 세션 이미 삭제됨 | `updateSessionTitle()`에서 NotFoundException → 예외 흡수 |

---

### 3.2 ChatbotServiceImpl 변경

#### 3.2.1 변경 개요

`generateResponse()` 메서드에 두 가지 변경이 필요합니다:
1. 새 세션 여부 판별 및 비동기 타이틀 생성 호출
2. `ChatResponse`에 `title` 포함

#### 3.2.2 수정 사항

```java
// 기존 의존성에 추가
private final SessionTitleGenerationService titleGenerationService;

@Override
public ChatResponse generateResponse(ChatRequest request, Long userId, String userRole) {
    // 새 세션 여부 판별
    boolean isNewSession = request.conversationId() == null || request.conversationId().isBlank();
    String sessionId = getOrCreateSession(request, userId);
    ChatMemory chatMemory = memoryProvider.get(sessionId);

    // 기존 세션이면 히스토리 로드
    if (!isNewSession) {
        loadHistoryToMemory(sessionId, chatMemory);
    }

    // ... (Intent 분류 및 응답 생성 로직 - 변경 없음) ...

    // 메시지 저장 및 세션 업데이트
    saveCurrentMessages(sessionId, chatMemory, request.message(), response);
    sessionService.updateLastMessageAt(sessionId);
    trackTokenUsage(sessionId, userId, request.message(), response);

    // [NEW] 새 세션이면 비동기 타이틀 생성 호출
    String title = null;
    if (isNewSession) {
        titleGenerationService.generateAndSaveTitleAsync(
            sessionId, userId, request.message(), response);
    }

    return ChatResponse.builder()
        .response(response)
        .conversationId(sessionId)
        .title(title)  // [NEW] 새 세션: null (비동기 생성 대기), 기존 세션: null
        .sources(sources)
        .build();
}
```

#### 3.2.3 새 세션 판별 로직

```java
// 기존 코드 (변경 없음)
boolean isNewSession = request.conversationId() == null || request.conversationId().isBlank();
```

기존 `isExistingSession` 변수를 `isNewSession`으로 의미를 명확히 변경합니다 (역전).

#### 3.2.4 비동기 호출 시점

```
응답 생성 완료 → 메시지 저장 완료 → 세션 타임스탬프 업데이트 완료
→ [여기서 비동기 호출] titleGenerationService.generateAndSaveTitleAsync()
→ ChatResponse 즉시 반환 (title = null)
```

**호출 시점 선택 이유**:
- 메시지 저장/업데이트 후 호출하여 데이터 정합성 확보
- 응답 반환 전 호출하지만, `@Async`이므로 즉시 반환됨
- title은 null로 응답하고, 클라이언트는 세션 목록 조회 시 생성된 타이틀을 확인

#### 3.2.5 ChatResponse title 값 정책

| 상황 | title 값 | 설명 |
|------|---------|------|
| 새 세션 첫 메시지 | `null` | 비동기 생성 중이므로 아직 없음 |
| 기존 세션 메시지 | `null` | 현재 ChatResponse에서는 세션 조회를 하지 않으므로 null |

**참고**: 클라이언트가 타이틀을 표시하려면 세션 목록/상세 조회 API(`GET /sessions`, `GET /sessions/{id}`)를 사용합니다. 이 API들은 이미 `SessionResponse.title`을 반환합니다.

---

### 3.3 ChatResponse DTO 변경

#### 3.3.1 변경 전

```java
@Builder
public record ChatResponse(
    String response,
    String conversationId,
    List<SourceResponse> sources
) {}
```

#### 3.3.2 변경 후

```java
@Builder
public record ChatResponse(
    String response,
    String conversationId,
    String title,                    // [NEW] 세션 타이틀 (nullable)
    List<SourceResponse> sources
) {}
```

**하위 호환성**: `title`은 nullable이므로 기존 클라이언트가 이 필드를 무시해도 문제 없습니다. JSON 직렬화 시 `null`로 포함됩니다.

---

### 3.4 세션 타이틀 수동 변경 API

#### 3.4.1 엔드포인트 설계

**PATCH** `/api/v1/chatbot/sessions/{sessionId}/title`

**Request Body**

```java
package com.ebson.shrimp.tm.demo.api.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSessionTitleRequest(
    @NotBlank(message = "타이틀은 필수입니다.")
    @Size(max = 200, message = "타이틀은 200자를 초과할 수 없습니다.")
    String title
) {}
```

**Request 예시**

```json
{
  "title": "AI 트렌드 대화"
}
```

**Response** (200 OK) `ApiResponse<SessionResponse>`

```json
{
  "code": "2000",
  "messageCode": { "code": "SUCCESS", "text": "성공" },
  "message": "success",
  "data": {
    "sessionId": "sess_abc123def456",
    "title": "AI 트렌드 대화",
    "createdAt": "2025-01-20T10:00:00",
    "lastMessageAt": "2025-01-20T14:30:00",
    "isActive": true
  }
}
```

**Errors**

| HTTP 상태 | 에러 코드 | 상황 |
|----------|---------|------|
| 400 | 4000 | 빈 타이틀, 타이틀 길이 초과 (200자) |
| 401 | 4010 | 인증 실패 |
| 403 | 4030 | 해당 세션에 접근할 권한 없음 |
| 404 | 4040 | 세션 없음 |

#### 3.4.2 ChatbotController 추가 메서드

```java
@PatchMapping("/sessions/{sessionId}/title")
public ResponseEntity<ApiResponse<SessionResponse>> updateSessionTitle(
        @PathVariable String sessionId,
        @Valid @RequestBody UpdateSessionTitleRequest request,
        @AuthenticationPrincipal UserPrincipal userPrincipal) {
    SessionResponse response = conversationSessionService.updateSessionTitle(
        sessionId, userPrincipal.userId(), request.title());
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

#### 3.4.3 ConversationSessionService 인터페이스 변경

```java
// 추가할 메서드
/**
 * 세션 타이틀 업데이트
 *
 * @param sessionId 세션 ID (TSID String)
 * @param userId    사용자 ID (소유권 검증용)
 * @param title     새 타이틀
 * @return 업데이트된 세션 정보
 */
SessionResponse updateSessionTitle(String sessionId, Long userId, String title);
```

#### 3.4.4 ConversationSessionServiceImpl 추가 메서드

```java
@Override
@Transactional
public SessionResponse updateSessionTitle(String sessionId, Long userId, String title) {
    Long sessionIdLong = parseSessionId(sessionId);
    ConversationSessionEntity session = conversationSessionReaderRepository.findById(sessionIdLong)
        .orElseThrow(() -> new ConversationSessionNotFoundException(
            "세션을 찾을 수 없습니다: " + sessionId));

    // 소유권 검증
    if (!session.getUserId().equals(userId)) {
        throw new UnauthorizedException("세션에 대한 접근 권한이 없습니다.");
    }

    // Soft Delete 확인
    if (Boolean.TRUE.equals(session.getIsDeleted())) {
        throw new ConversationSessionNotFoundException("삭제된 세션입니다: " + sessionId);
    }

    // 타이틀 업데이트
    session.setTitle(title);
    session.setUpdatedAt(LocalDateTime.now());
    ConversationSessionEntity updatedSession = conversationSessionWriterRepository.save(session);

    // Kafka 이벤트 발행
    Map<String, Object> updatedFields = new HashMap<>();
    updatedFields.put("title", title);

    ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload payload =
        new ConversationSessionUpdatedEvent.ConversationSessionUpdatedPayload(
            sessionId, userId.toString(), updatedFields);

    ConversationSessionUpdatedEvent event = new ConversationSessionUpdatedEvent(payload);
    eventPublisher.publish(TOPIC_SESSION_UPDATED, event, sessionId);

    log.info("Session title updated: sessionId={}, title={}", sessionId, title);

    return toResponse(updatedSession);
}
```

**설계 포인트**:
- 기존 `getSession()`, `deleteSession()`과 동일한 보안 검증 패턴 (소유권 + 삭제 상태)
- 기존 `updateLastMessageAt()`과 동일한 Kafka 이벤트 발행 패턴
- `toResponse()` 재사용

---

### 3.5 CQRS 이벤트 흐름

#### 3.5.1 타이틀 변경 시 전체 이벤트 흐름

```
1. ConversationSessionServiceImpl.updateSessionTitle()
   → Aurora MySQL: session.setTitle(title) → save()

2. EventPublisher.publish()
   → Kafka Topic: shrimp-tm.conversation.session.updated
   → Partition Key: sessionId (순서 보장)
   → Payload: { sessionId, userId, updatedFields: { "title": "새 타이틀" } }

3. EventConsumer.consume()
   → 멱등성 체크 (Redis: processed_event:{eventId})
   → EventHandlerRegistry → ConversationSessionUpdatedEventHandler

4. ConversationSyncServiceImpl.syncSessionUpdated()
   → MongoDB: findBySessionId() → document.setTitle(title) → save()
```

#### 3.5.2 기존 인프라 활용 (변경 없음)

| 컴포넌트 | 변경 필요 여부 | 이유 |
|----------|-------------|------|
| `ConversationSessionUpdatedEvent` | 변경 없음 | `updatedFields` Map이 이미 유연한 구조 |
| `ConversationSyncServiceImpl.updateSessionDocumentFields()` | 변경 없음 | `"title"` case 이미 구현됨 |
| `ConversationSessionUpdatedEventHandler` | 변경 없음 | 이벤트 타입 기반 라우팅, 페이로드 무관 |
| `EventPublisher` | 변경 없음 | 범용 이벤트 발행기 |
| `EventConsumer` | 변경 없음 | 범용 이벤트 소비기 (멱등성 보장) |

---

## 4. 수정 대상 파일 목록

### 4.1 신규 생성 파일

| 파일 | 설명 |
|------|------|
| `api/chatbot/.../service/SessionTitleGenerationService.java` | 타이틀 생성 서비스 인터페이스 |
| `api/chatbot/.../service/SessionTitleGenerationServiceImpl.java` | 타이틀 생성 서비스 구현체 (비동기 LLM 호출) |
| `api/chatbot/.../dto/request/UpdateSessionTitleRequest.java` | 타이틀 수동 변경 요청 DTO |

### 4.2 기존 수정 파일

| 파일 | 변경 내용 |
|------|-----------|
| `api/chatbot/.../dto/response/ChatResponse.java` | `title` 필드 추가 |
| `api/chatbot/.../service/ChatbotServiceImpl.java` | `titleGenerationService` 의존성 추가, `generateResponse()`에서 비동기 타이틀 생성 호출, `isNewSession` 변수 추가, `ChatResponse.builder()`에 `.title(null)` 추가 |
| `api/chatbot/.../service/ConversationSessionService.java` | `updateSessionTitle()` 메서드 시그니처 추가 |
| `api/chatbot/.../service/ConversationSessionServiceImpl.java` | `updateSessionTitle()` 메서드 구현 추가 |
| `api/chatbot/.../controller/ChatbotController.java` | PATCH 엔드포인트 추가 |
| `docs/reference/API-SPECIFICATIONS/api-chatbot-specification.md` | API 명세 업데이트 (아래 5절 참조) |

### 4.3 변경 불필요 파일

| 파일 | 이유 |
|------|------|
| `ConversationSessionEntity.java` | `title` 컬럼 이미 존재 |
| `ConversationSessionDocument.java` | `title` 필드 이미 존재 |
| `SessionResponse.java` | `title` 필드 이미 존재 |
| `ConversationSessionUpdatedEvent.java` | `updatedFields` Map이 유연한 구조 |
| `ConversationSyncServiceImpl.java` | `"title"` case 이미 구현됨 |
| `LangChain4jConfig.java` | 기존 `ChatModel` Bean 재사용 |

---

## 5. API 명세 업데이트

### 5.1 ChatResponse 변경 (섹션 3.1)

#### 변경 전

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| response | String | O | AI 응답 메시지 |
| conversationId | String | O | 대화 세션 ID |
| sources | SourceResponse[] | X | 참조한 소스 목록 (RAG) |

#### 변경 후

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| response | String | O | AI 응답 메시지 |
| conversationId | String | O | 대화 세션 ID |
| **title** | **String** | **X** | **세션 타이틀 (비동기 생성으로 null일 수 있음)** |
| sources | SourceResponse[] | X | 참조한 소스 목록 (RAG) |

#### 응답 예시 변경

```json
{
  "code": "2000",
  "messageCode": { "code": "SUCCESS", "text": "성공" },
  "message": "success",
  "data": {
    "response": "최신 AI 기술 트렌드를 알려드리겠습니다...",
    "conversationId": "sess_abc123def456",
    "title": null,
    "sources": [...]
  }
}
```

### 5.2 세션 타이틀 수동 변경 API 추가 (새 섹션 4.5)

#### 4.5 세션 타이틀 수정

**PATCH** `/api/v1/chatbot/sessions/{sessionId}/title`

**인증**: 필요

세션의 타이틀을 수정합니다.

**Path Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| sessionId | String | O | 세션 ID |

**Request Body**

```json
{
  "title": "AI 트렌드 대화"
}
```

**UpdateSessionTitleRequest 필드**

| 필드 | 타입 | 필수 | 검증 | 설명 |
|------|------|------|------|------|
| title | String | O | NotBlank, Max(200) | 새 세션 타이틀 (최대 200자) |

**Response** (200 OK) `ApiResponse<SessionResponse>`

SessionResponse 형식

**Errors**

| HTTP 상태 | 에러 코드 | 상황 |
|----------|---------|------|
| 400 | 4000 | 빈 타이틀, 타이틀 길이 초과 (200자) |
| 401 | 4010 | 인증 실패 |
| 403 | 4030 | 해당 세션에 접근할 권한 없음 |
| 404 | 4040 | 세션 없음 |

### 5.3 엔드포인트 요약 업데이트 (섹션 6)

| Method | Endpoint | 인증 | 설명 |
|--------|----------|------|------|
| POST | `/api/v1/chatbot` | O | 채팅 메시지 전송 |
| GET | `/api/v1/chatbot/sessions` | O | 세션 목록 조회 |
| GET | `/api/v1/chatbot/sessions/{sessionId}` | O | 세션 상세 조회 |
| GET | `/api/v1/chatbot/sessions/{sessionId}/messages` | O | 세션 메시지 목록 조회 |
| **PATCH** | **`/api/v1/chatbot/sessions/{sessionId}/title`** | **O** | **세션 타이틀 수정** |
| DELETE | `/api/v1/chatbot/sessions/{sessionId}` | O | 세션 삭제 |

### 5.4 에러 메시지 추가

| 상황 | 에러 코드 | 메시지 |
|------|---------|--------|
| 빈 타이틀 | 4000 | 타이틀은 필수입니다. |
| 타이틀 길이 초과 | 4000 | 타이틀은 200자를 초과할 수 없습니다. |

---

## 6. 테스트 계획

### 6.1 단위 테스트

#### SessionTitleGenerationServiceImpl 테스트

| 테스트 시나리오 | 검증 항목 |
|----------------|----------|
| 정상 타이틀 생성 | `ChatModel.chat()` 호출 시 올바른 프롬프트 전달, `sessionService.updateSessionTitle()` 호출 검증 |
| LLM 응답에 인용부호 포함 | `sanitizeTitle()`이 인용부호를 제거하는지 검증 |
| LLM API 실패 시 예외 흡수 | `ChatModel.chat()` 예외 발생 시 로그만 남기고 예외를 전파하지 않는지 검증 |
| 200자 초과 타이틀 | `sanitizeTitle()`이 200자로 잘라내는지 검증 |
| LLM 빈 응답 | `sanitizeTitle()`이 null 반환하는지 검증 |
| 입력 메시지 truncation | 300자 초과 메시지가 잘려서 프롬프트에 포함되는지 검증 |

#### ConversationSessionServiceImpl.updateSessionTitle() 테스트

| 테스트 시나리오 | 검증 항목 |
|----------------|----------|
| 정상 타이틀 업데이트 | Aurora 저장, Kafka 이벤트 발행, SessionResponse 반환 검증 |
| 존재하지 않는 세션 | `ConversationSessionNotFoundException` 발생 검증 |
| 소유권 불일치 | `UnauthorizedException` 발생 검증 |
| 삭제된 세션 | `ConversationSessionNotFoundException` 발생 검증 |
| Kafka 이벤트 페이로드 | `updatedFields`에 `"title"` 키가 올바른 값으로 포함되는지 검증 |

#### ChatbotServiceImpl.generateResponse() 변경 테스트

| 테스트 시나리오 | 검증 항목 |
|----------------|----------|
| 새 세션 (conversationId = null) | `titleGenerationService.generateAndSaveTitleAsync()` 호출됨 |
| 기존 세션 (conversationId 존재) | `titleGenerationService.generateAndSaveTitleAsync()` 호출되지 않음 |
| ChatResponse에 title 필드 포함 | 빌더 패턴에서 `title` 필드가 포함된 응답 반환 검증 |

### 6.2 통합 테스트

| 테스트 시나리오 | 검증 항목 |
|----------------|----------|
| 새 세션 E2E | POST /chatbot (conversationId 없음) → 응답 수신 → 비동기 타이틀 생성 → GET /sessions에서 title 확인 |
| 타이틀 수동 변경 E2E | PATCH /sessions/{id}/title → 200 OK → GET /sessions/{id}에서 변경된 title 확인 |
| CQRS 동기화 | 타이틀 변경 → Kafka 이벤트 발행 → MongoDB 문서 title 업데이트 확인 |
| 비동기 실패 격리 | LLM 장애 상황에서 POST /chatbot 응답이 정상 반환되는지 확인 |

---

**문서 버전**: 1.0
**최종 업데이트**: 2026-02-13
