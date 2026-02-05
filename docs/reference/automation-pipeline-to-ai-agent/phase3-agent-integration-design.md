# Phase 3: AI Agent 로직 통합 설계서

## 1. 개요

### 1.1 목적
Phase 2에서 정의한 Tool들을 사용하는 AI Agent를 구축하여, 자연어 목표(Goal)만 주어지면 자율적으로 데이터 수집 및 포스팅을 수행하도록 한다.

### 1.2 전제 조건
- Phase 1 완료: EmergingTech 엔티티, API 엔드포인트, Batch Job
- Phase 2 완료: LangChain4j Tool 래퍼 (EmergingTechAgentTools)

---

## 2. Agent 인터페이스 설계

### 2.1 인터페이스 정의

```java
package com.ebson.shrimp.tm.demo.api.agent.agent;

/**
 * Emerging Tech 업데이트 추적 Agent 인터페이스
 */
public interface EmergingTechAgent {

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
api/agent/src/main/java/.../agent/
├── agent/
│   ├── EmergingTechAgent.java           // 인터페이스
│   ├── EmergingTechAgentImpl.java       // 구현체
│   ├── AgentExecutionResult.java        // 결과 DTO
│   └── AgentAssistant.java             // LangChain4j AiServices 인터페이스
├── config/
│   └── AgentConfig.java                // Agent 설정
├── scheduler/
│   └── EmergingTechAgentScheduler.java  // 스케줄 트리거
└── controller/
    └── AgentController.java             // REST API 트리거
```

### 3.2 Agent 구현체

```java
package com.ebson.shrimp.tm.demo.api.agent.agent;

import com.ebson.shrimp.tm.demo.api.agent.tool.EmergingTechAgentTools;
import com.ebson.shrimp.tm.demo.client.slack.domain.slack.contract.SlackContract;
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
public class EmergingTechAgentImpl implements EmergingTechAgent {

    @Qualifier("agentChatModel")
    private final ChatLanguageModel chatModel;  // OpenAI GPT-4o-mini
    private final EmergingTechAgentTools tools;
    private final SlackContract slackApi;

    private static final int MAX_TOOL_CALLS = 20;  // 무한 루프 방지

    @Override
    public AgentExecutionResult execute(String goal) {
        long startTime = System.currentTimeMillis();

        try {
            // 카운터 초기화
            tools.resetCounters();

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
            log.info("Agent 실행 완료: goal={}, elapsed={}ms, toolCalls={}, postsCreated={}",
                goal, elapsed, tools.getToolCallCount(), tools.getPostsCreatedCount());

            return AgentExecutionResult.success(
                response,
                tools.getToolCallCount(),
                tools.getPostsCreatedCount(),
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
            당신은 Emerging Tech 업데이트 추적 전문가입니다.

            ## 역할
            - 빅테크 IT 기업(OpenAI, Anthropic, Google, Meta, xAI)의 최신 업데이트를 추적
            - 중요한 업데이트를 식별하고 포스팅

            ## 사용 가능한 도구
            - fetch_github_releases: GitHub 저장소 릴리스 조회
            - scrape_web_page: 웹 페이지 크롤링
            - search_emerging_techs: 기존 Emerging Tech 업데이트 검색 (중복 방지)
            - create_draft_post: 초안 포스트 생성
            - publish_post: 포스트 게시
            - send_slack_notification: Slack 알림

            ## 주요 저장소 정보
            - OpenAI: openai/openai-python
            - Anthropic: anthropics/anthropic-sdk-python
            - Google: google/generative-ai-python
            - Meta: facebookresearch/llama
            - xAI: xai-org/grok-1

            ## 규칙
            1. 작업 전 항상 search_emerging_techs로 중복 확인
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
package com.ebson.shrimp.tm.demo.api.agent.agent;

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
package com.ebson.shrimp.tm.demo.api.agent.controller;

import com.ebson.shrimp.tm.demo.api.agent.agent.EmergingTechAgent;
import com.ebson.shrimp.tm.demo.api.agent.agent.EmergingTechAgentImpl;
import com.ebson.shrimp.tm.demo.api.agent.agent.AgentExecutionResult;
import com.ebson.shrimp.tm.demo.api.agent.config.AgentConfig;
import com.ebson.shrimp.tm.demo.common.core.dto.ApiResponse;
import com.ebson.shrimp.tm.demo.common.exception.exception.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.UUID;

/**
 * Emerging Tech Agent REST API 컨트롤러
 * 내부 API Key 인증을 통한 수동 실행 트리거
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final EmergingTechAgent agent;
    private final AgentConfig agentConfig;

    /**
     * Agent 수동 실행
     *
     * POST /api/v1/agent/run
     * Header: X-Internal-Api-Key
     *
     * @param request goal (필수), sessionId (선택)
     */
    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AgentExecutionResult>> runAgent(
            @Valid @RequestBody AgentRunRequest request,
            @RequestHeader("X-Internal-Api-Key") String requestApiKey
    ) {
        validateApiKey(requestApiKey);

        // sessionId 지정되지 않으면 자동 생성
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : "manual-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Agent 수동 실행 요청: goal={}, sessionId={}", request.goal(), sessionId);

        // sessionId가 지정된 경우 오버로드된 execute 메서드 호출
        AgentExecutionResult result;
        if (agent instanceof EmergingTechAgentImpl agentImpl) {
            result = agentImpl.execute(request.goal(), sessionId);
        } else {
            result = agent.execute(request.goal());
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private void validateApiKey(String requestApiKey) {
        if (requestApiKey == null || requestApiKey.isBlank()) {
            throw new UnauthorizedException("내부 API 키가 제공되지 않았습니다.");
        }

        String configuredApiKey = agentConfig.getApiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            log.warn("내부 API 키가 설정되지 않았습니다. 설정 파일을 확인하세요.");
            throw new UnauthorizedException("내부 API 키가 설정되지 않았습니다.");
        }

        if (!MessageDigest.isEqual(configuredApiKey.getBytes(), requestApiKey.getBytes())) {
            throw new UnauthorizedException("유효하지 않은 내부 API 키입니다.");
        }
    }

    /**
     * Agent 실행 요청 DTO
     *
     * @param goal 실행 목표 (필수)
     * @param sessionId 세션 식별자 (선택, 미지정 시 자동 생성)
     */
    public record AgentRunRequest(
            @NotBlank(message = "goal은 필수입니다.")
            String goal,
            String sessionId
    ) {}
}
```

### 4.2 스케줄 트리거

```java
package com.ebson.shrimp.tm.demo.api.agent.scheduler;

import com.ebson.shrimp.tm.demo.api.agent.agent.EmergingTechAgent;
import com.ebson.shrimp.tm.demo.api.agent.agent.AgentExecutionResult;
import com.ebson.shrimp.tm.demo.client.slack.domain.slack.contract.SlackContract;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emerging Tech Agent 스케줄러
 * 6시간마다 자동으로 Emerging Tech 업데이트 추적 및 포스팅 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "agent.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class EmergingTechAgentScheduler {

    private final EmergingTechAgent agent;
    private final SlackContract slackContract;

    private static final String DEFAULT_GOAL = """
        OpenAI, Anthropic, Google, Meta, xAI의 최신 업데이트를 확인하고 중요한 것만 초안으로 생성해줘.
        이미 포스팅된 것은 제외하고, 생성 후 Slack으로 알려줘.
        """;

    /**
     * 6시간마다 실행
     */
    @Scheduled(cron = "${agent.scheduler.cron:0 0 */6 * * *}")
    public void scheduledRun() {
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
            slackContract.sendErrorNotification("Agent 스케줄 실행 실패", e);
        }
    }

    private void notifyFailure(AgentExecutionResult result) {
        try {
            slackContract.sendErrorNotification(
                "Agent 스케줄 실행 실패: " + String.join(", ", result.errors()),
                null
            );
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }
}
```

---

## 5. 설정

### 5.1 Agent 설정 클래스

```java
package com.ebson.shrimp.tm.demo.api.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Agent 관련 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "internal-api.emerging-tech")
public class AgentConfig {

    /**
     * 내부 API 키 (Agent 엔드포인트 인증용)
     */
    private String apiKey;
}
```

### 5.2 application-agent-api.yml 설정

```yaml
# Agent용 OpenAI 설정
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      model-name: gpt-4o-mini
      temperature: 0.3
      max-tokens: 4096
      timeout: 120s

# Emerging Tech 내부 API 설정
internal-api:
  emerging-tech:
    api-key: ${EMERGING_TECH_INTERNAL_API_KEY:}

# Emerging Tech Agent 스케줄러 설정
agent:
  scheduler:
    enabled: ${AGENT_SCHEDULER_ENABLED:false}
    cron: "0 0 */6 * * *"

# Slack 알림
slack:
  emerging-tech:
    channel: "#emerging-tech"
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
class EmergingTechAgentImplTest {

    @Mock
    private ChatLanguageModel chatModel;

    @Mock
    private EmergingTechAgentTools tools;

    @Mock
    private SlackContract slackApi;

    @InjectMocks
    private EmergingTechAgentImpl agent;

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
class EmergingTechAgentIntegrationTest {

    @Autowired
    private EmergingTechAgent agent;

    @MockBean
    private EmergingTechAgentTools tools;

    @Test
    void execute_shouldCallToolsInOrder() {
        // Given
        when(tools.searchEmergingTechs(anyString(), anyString()))
            .thenReturn(List.of());
        when(tools.fetchGitHubReleases(anyString(), anyString()))
            .thenReturn(List.of(new GitHubReleaseDto("v1.0.0", "Release", "notes", "url", "2024-01-01")));

        // When
        AgentExecutionResult result = agent.execute("OpenAI SDK 업데이트 확인");

        // Then
        verify(tools, atLeastOnce()).searchEmergingTechs(anyString(), anyString());
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
   → Tool: fetch_github_releases("openai", "openai-python")
   → 결과: v1.50.0 릴리스 발견

2. "이미 포스팅했는지 확인해볼게"
   → Tool: search_emerging_techs("openai-python v1.50.0", "OPENAI")
   → 결과: 없음 (중복 아님)

3. "새로운 SDK 릴리스니까 초안 만들자"
   → Tool: create_draft_post(
       title="OpenAI Python SDK v1.50.0 릴리스",
       summary="...",
       provider="OPENAI",
       updateType="SDK_RELEASE",
       url="https://github.com/openai/openai-python/releases/tag/v1.50.0"
     )
   → 결과: Post ID "12345" 생성됨

4. "Slack으로 알려줄게"
   → Tool: send_slack_notification(
       message="새 업데이트 초안 생성: OpenAI Python SDK v1.50.0"
     )
   → 결과: 성공

5. "이제 Anthropic 확인해볼게"
   → Tool: fetch_github_releases("anthropics", "anthropic-sdk-python")
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
