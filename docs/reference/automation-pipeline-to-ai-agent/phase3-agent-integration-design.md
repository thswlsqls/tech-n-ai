# Phase 3: AI Agent 로직 통합 설계서

## 1. 개요

### 1.1 목적
Phase 2에서 정의한 Tool들을 사용하는 AI Agent를 구축하여, 자연어 목표(Goal)만 주어지면 자율적으로 데이터 수집 및 포스팅을 수행하도록 한다.

### 1.2 전제 조건
- Phase 1 완료: AiUpdate 엔티티, API 엔드포인트, Batch Job
- Phase 2 완료: LangChain4j Tool 래퍼 (AiUpdateAgentTools)

---

## 2. Agent 인터페이스 설계

### 2.1 인터페이스 정의

```java
package com.tech.n.ai.api.chatbot.agent;

/**
 * AI 업데이트 추적 Agent 인터페이스
 */
public interface AiUpdateAgent {

    /**
     * 자연어 목표를 받아 자율적으로 실행
     *
     * @param goal 실행 목표 (예: "OpenAI와 Anthropic의 최신 업데이트를 확인하고 중요한 것만 포스팅해줘")
     * @return 실행 결과 요약
     */
    AgentExecutionResult execute(String goal);
}
```

### 2.2 실행 결과 DTO

```java
public record AgentExecutionResult(
    boolean success,
    String summary,
    int toolCallCount,
    int postsCreated,
    long executionTimeMs,
    List<String> errors
) {
    public static AgentExecutionResult success(String summary, int toolCallCount, int postsCreated, long executionTimeMs) {
        return new AgentExecutionResult(true, summary, toolCallCount, postsCreated, executionTimeMs, List.of());
    }

    public static AgentExecutionResult failure(String summary, List<String> errors) {
        return new AgentExecutionResult(false, summary, 0, 0, 0, errors);
    }
}
```

---

## 3. Agent 구현 설계

### 3.1 디렉토리 구조

```
api/chatbot/src/main/java/.../chatbot/
├── agent/
│   ├── AiUpdateAgent.java           // 인터페이스
│   ├── AiUpdateAgentImpl.java       // 구현체
│   ├── AgentExecutionResult.java    // 결과 DTO
│   └── AgentAssistant.java          // LangChain4j AiServices 인터페이스
├── config/
│   └── AiAgentConfig.java           // Agent 설정
├── scheduler/
│   └── AiUpdateAgentScheduler.java  // 스케줄 트리거
└── controller/
    └── AgentController.java         // REST API 트리거
```

### 3.2 Agent 구현체

```java
package com.tech.n.ai.api.chatbot.agent;

import com.tech.n.ai.api.chatbot.tool.AiUpdateAgentTools;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiUpdateAgentImpl implements AiUpdateAgent {

    @Qualifier("agentChatModel")
    private final ChatLanguageModel chatModel;  // OpenAI GPT-4o-mini
    private final AiUpdateAgentTools tools;
    private final SlackContract slackApi;

    private static final int MAX_TOOL_CALLS = 20;  // 무한 루프 방지

    @Override
    public AgentExecutionResult execute(String goal) {
        long startTime = System.currentTimeMillis();

        try {
            // 세션별 독립 메모리 생성
            ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(30)
                .build();

            // AiServices로 Agent 생성
            AgentAssistant assistant = AiServices.builder(AgentAssistant.class)
                .chatLanguageModel(chatModel)
                .tools(tools)
                .chatMemory(memory)
                .build();

            // Agent 실행
            String response = assistant.chat(buildPrompt(goal));

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Agent 실행 완료: goal={}, elapsed={}ms", goal, elapsed);

            return AgentExecutionResult.success(
                response,
                0,  // Tool 호출 횟수는 LangChain4j 내부에서 추적 어려움
                0,  // 생성된 포스트 수도 응답에서 파싱 필요
                elapsed
            );

        } catch (Exception e) {
            log.error("Agent 실행 실패: goal={}", goal, e);
            notifyError(goal, e);
            return AgentExecutionResult.failure(
                "Agent 실행 중 오류 발생: " + e.getMessage(),
                List.of(e.getMessage())
            );
        }
    }

    private String buildPrompt(String goal) {
        return """
            당신은 AI 업데이트 추적 전문가입니다.

            ## 역할
            - 빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta)의 최신 업데이트를 추적
            - 중요한 업데이트를 식별하고 포스팅

            ## 사용 가능한 도구
            - fetchGitHubReleases: GitHub 저장소 릴리스 조회
            - scrapeWebPage: 웹 페이지 크롤링
            - searchAiUpdates: 기존 업데이트 검색 (중복 방지)
            - createDraftPost: 초안 포스트 생성
            - publishPost: 포스트 게시
            - sendSlackNotification: Slack 알림

            ## 주요 저장소 정보
            - OpenAI: openai/openai-python
            - Anthropic: anthropics/anthropic-sdk-python
            - Google: google/generative-ai-python
            - Meta: facebookresearch/llama

            ## 규칙
            1. 작업 전 항상 searchAiUpdates로 중복 확인
            2. 중요도가 높은 업데이트만 포스팅 (모델 출시, 주요 API 변경, 메이저 버전 릴리스)
            3. 마이너 버그 수정이나 문서 업데이트는 건너뛰기
            4. 확신이 낮으면 Slack으로 관리자에게 확인 요청
            5. 작업 완료 후 결과 요약 제공

            ## 사용자 요청
            %s
            """.formatted(goal);
    }

    private void notifyError(String goal, Exception e) {
        try {
            slackApi.sendErrorNotification(
                "Agent 실행 실패\nGoal: " + goal,
                e
            );
        } catch (Exception slackError) {
            log.error("Slack 에러 알림 전송 실패", slackError);
        }
    }
}
```

### 3.3 AgentAssistant 인터페이스

```java
package com.tech.n.ai.api.chatbot.agent;

/**
 * LangChain4j AiServices용 Assistant 인터페이스
 */
public interface AgentAssistant {

    /**
     * 사용자 메시지에 응답
     *
     * @param userMessage 사용자 메시지 (System Prompt 포함)
     * @return Agent 응답
     */
    String chat(String userMessage);
}
```

---

## 4. 트리거 설계

### 4.1 REST API 트리거

```java
package com.tech.n.ai.api.chatbot.controller;

import com.tech.n.ai.api.chatbot.agent.AiUpdateAgent;
import com.tech.n.ai.api.chatbot.agent.AgentExecutionResult;
import com.tech.n.ai.common.core.dto.ApiResponse;
import com.tech.n.ai.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AiUpdateAgent agent;

    @Value("${internal-api.agent.api-key}")
    private String apiKey;

    /**
     * Agent 수동 실행
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AgentExecutionResult>> runAgent(
            @Valid @RequestBody AgentRunRequest request,
            @RequestHeader("X-Internal-Api-Key") String requestApiKey
    ) {
        validateApiKey(requestApiKey);
        log.info("Agent 수동 실행 요청: goal={}", request.goal());

        AgentExecutionResult result = agent.execute(request.goal());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void validateApiKey(String requestApiKey) {
        if (requestApiKey == null || !apiKey.equals(requestApiKey)) {
            throw new UnauthorizedException("유효하지 않은 API 키입니다.");
        }
    }

    public record AgentRunRequest(
        @NotBlank(message = "goal은 필수입니다.")
        String goal
    ) {}
}
```

### 4.2 스케줄 트리거

```java
package com.tech.n.ai.api.chatbot.scheduler;

import com.tech.n.ai.api.chatbot.agent.AiUpdateAgent;
import com.tech.n.ai.api.chatbot.agent.AgentExecutionResult;
import com.tech.n.ai.client.slack.domain.slack.contract.SlackContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiUpdateAgentScheduler {

    private final AiUpdateAgent agent;
    private final SlackContract slackApi;

    @Value("${agent.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    private static final String DEFAULT_GOAL = """
        OpenAI, Anthropic, Google, Meta의 최신 업데이트를 확인하고 중요한 것만 포스팅해줘.
        이미 포스팅된 것은 제외해.
        """;

    /**
     * 6시간마다 실행
     */
    @Scheduled(cron = "${agent.scheduler.cron:0 0 */6 * * *}")
    public void scheduledRun() {
        if (!schedulerEnabled) {
            log.debug("Agent 스케줄러 비활성화됨");
            return;
        }

        log.info("Agent 스케줄 실행 시작");

        try {
            AgentExecutionResult result = agent.execute(DEFAULT_GOAL);

            if (result.success()) {
                log.info("Agent 스케줄 실행 완료: postsCreated={}, elapsed={}ms",
                    result.postsCreated(), result.executionTimeMs());
            } else {
                log.warn("Agent 스케줄 실행 실패: errors={}", result.errors());
                notifyFailure(result);
            }
        } catch (Exception e) {
            log.error("Agent 스케줄 실행 중 예외 발생", e);
            slackApi.sendErrorNotification("Agent 스케줄 실행 실패", e);
        }
    }

    private void notifyFailure(AgentExecutionResult result) {
        slackApi.sendErrorNotification(
            "Agent 스케줄 실행 실패: " + String.join(", ", result.errors()),
            null
        );
    }
}
```

---

## 5. 설정

### 5.1 Agent 설정 클래스

```java
package com.tech.n.ai.api.chatbot.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiAgentConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    @Value("${langchain4j.open-ai.agent-model.model-name:gpt-4o-mini}")
    private String agentModelName;

    /**
     * Agent용 ChatLanguageModel (OpenAI GPT-4o-mini)
     * Tool 호출을 지원하는 모델 - 기존 OpenAI 인프라 활용
     */
    @Bean("agentChatModel")
    public ChatLanguageModel agentChatLanguageModel() {
        log.info("Agent ChatLanguageModel 초기화: model={}", agentModelName);

        return OpenAiChatModel.builder()
            .apiKey(openAiApiKey)  // 기존 OpenAI API 키 재사용
            .modelName(agentModelName)
            .temperature(0.3)  // Tool 호출에는 낮은 temperature
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(120))
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
```

### 5.2 application.yml 설정

```yaml
# Agent 설정 - 기존 OpenAI 설정과 통합
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini        # 챗봇용 (기존)
    agent-model:
      model-name: gpt-4o-mini        # Agent용 (동일 모델 사용)
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small  # 기존 유지

# 내부 API
internal-api:
  agent:
    api-key: ${AGENT_INTERNAL_API_KEY:default-agent-key}

# 스케줄러 설정
agent:
  scheduler:
    enabled: ${AGENT_SCHEDULER_ENABLED:false}
    cron: "0 0 */6 * * *"  # 6시간마다

# Slack 알림
slack:
  ai-update:
    channel: "#ai-updates"
  alerts:
    channel: "#alerts"
```

---

## 6. 에러 처리

### 6.1 예외 처리 전략

| 예외 유형 | 처리 방식 |
|----------|----------|
| Tool 실행 실패 | 로깅 + Slack 알림 + 계속 진행 |
| LLM API 타임아웃 | 로깅 + Slack 알림 + 실패 반환 |
| 인증 실패 | UnauthorizedException throw |
| 예상치 못한 예외 | 로깅 + Slack 알림 + 실패 반환 |

### 6.2 재시도 정책

```java
// Tool Adapter에서 개별 재시도 처리
@Retryable(
    value = {FeignException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public List<GitHubReleaseDto> getReleases(String owner, String repo) {
    // ...
}
```

---

## 7. 모니터링 및 로깅

### 7.1 로깅 포맷

```java
// 실행 시작
log.info("Agent 실행 시작: goal={}", goal);

// Tool 호출
log.info("Tool 호출: method={}, params={}", methodName, params);

// 실행 완료
log.info("Agent 실행 완료: elapsed={}ms, toolCalls={}, postsCreated={}",
    elapsed, toolCallCount, postsCreated);

// 오류
log.error("Agent 실행 실패: goal={}, error={}", goal, e.getMessage(), e);
```

### 7.2 메트릭 (향후 확장)

```java
// Micrometer 메트릭 (선택적 구현)
@Counted(value = "agent.executions", description = "Agent 실행 횟수")
@Timed(value = "agent.execution.time", description = "Agent 실행 시간")
public AgentExecutionResult execute(String goal) {
    // ...
}
```

---

## 8. 테스트 설계

### 8.1 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class AiUpdateAgentImplTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private AiUpdateAgentTools tools;

    @Mock
    private SlackContract slackApi;

    @InjectMocks
    private AiUpdateAgentImpl agent;

    @Test
    void execute_shouldReturnSuccessResult() {
        // Given
        String goal = "OpenAI 업데이트 확인";
        // LangChain4j AiServices는 통합 테스트에서 검증

        // When
        AgentExecutionResult result = agent.execute(goal);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void execute_shouldNotifyOnError() {
        // Given
        String goal = "에러 발생 시나리오";

        // When - Exception 발생 시뮬레이션
        // ...

        // Then
        verify(slackApi).sendErrorNotification(anyString(), any());
    }
}
```

### 8.2 통합 테스트

```java
@SpringBootTest
@ActiveProfiles("test")
class AiUpdateAgentIntegrationTest {

    @Autowired
    private AiUpdateAgent agent;

    @MockBean
    private AiUpdateAgentTools tools;

    @Test
    void execute_shouldCallToolsInOrder() {
        // Given
        when(tools.searchAiUpdates(anyString(), anyString()))
            .thenReturn(List.of());
        when(tools.fetchGitHubReleases(anyString(), anyString()))
            .thenReturn(List.of(new GitHubReleaseDto("v1.0.0", "Release", "notes", "url", "2024-01-01")));

        // When
        AgentExecutionResult result = agent.execute("OpenAI SDK 업데이트 확인");

        // Then
        verify(tools, atLeastOnce()).searchAiUpdates(anyString(), anyString());
        verify(tools, atLeastOnce()).fetchGitHubReleases(eq("openai"), eq("openai-python"));
    }
}
```

---

## 9. Agent 행동 흐름 예시

```
User Goal: "OpenAI와 Anthropic 최신 업데이트 확인하고 포스팅해줘"

Agent 추론:
1. "먼저 OpenAI GitHub 릴리스를 확인해볼게"
   → Tool: fetchGitHubReleases("openai", "openai-python")
   → 결과: v1.50.0 릴리스 발견

2. "이미 포스팅했는지 확인해볼게"
   → Tool: searchAiUpdates("openai-python v1.50.0", "OPENAI")
   → 결과: 없음 (중복 아님)

3. "새로운 SDK 릴리스니까 초안 만들자"
   → Tool: createDraftPost(
       title="OpenAI Python SDK v1.50.0 릴리스",
       summary="...",
       provider="OPENAI",
       updateType="SDK_RELEASE",
       url="https://github.com/openai/openai-python/releases/tag/v1.50.0"
     )
   → 결과: Post ID "12345" 생성됨

4. "Slack으로 알려줄게"
   → Tool: sendSlackNotification(
       message="새 업데이트 초안 생성: OpenAI Python SDK v1.50.0"
     )
   → 결과: 성공

5. "이제 Anthropic 확인해볼게"
   → Tool: fetchGitHubReleases("anthropics", "anthropic-sdk-python")
   → 결과: 새 릴리스 없음

Agent 응답:
"OpenAI Python SDK v1.50.0 릴리스를 발견하여 초안 포스트를 생성했습니다 (ID: 12345).
Slack 알림을 전송했습니다. Anthropic SDK에는 새 릴리스가 없었습니다."
```

---

## 10. 참고 자료

- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- LangChain4j Chat Memory: https://docs.langchain4j.dev/tutorials/chat-memory
- LangChain4j Tools: https://docs.langchain4j.dev/tutorials/tools
- LangChain4j OpenAI: https://docs.langchain4j.dev/integrations/language-models/open-ai
- OpenAI API: https://platform.openai.com/docs/api-reference
