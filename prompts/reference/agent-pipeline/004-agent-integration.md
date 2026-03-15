# Phase 3: AI Agent 로직 통합 설계서 작성 프롬프트

## 목표
Phase 2에서 정의한 Tool들을 사용하는 AI Agent를 구축하여, 목표(Goal)만 주어지면 자율적으로 데이터 수집 및 포스팅을 수행하도록 설계한다.

## 전제 조건
- Phase 1 완료: 데이터 수집 파이프라인, API 엔드포인트
- Phase 2 완료: LangChain4j Tool 래퍼

## 설계서에 포함할 내용

### 1. Agent 인터페이스 설계
```java
public interface AiUpdateAgent {
    /**
     * 자연어 목표를 받아 자율적으로 실행
     * @param goal 예: "OpenAI와 Anthropic의 최신 업데이트를 확인하고 중요한 것만 포스팅해줘"
     * @return 실행 결과 요약
     */
    String execute(String goal);
}
```

### 2. Agent 구현 설계 (LangChain4j AiServices)
```java
@Service
public class AiUpdateAgentImpl implements AiUpdateAgent {

    private final ChatLanguageModel model;
    private final AiUpdateTools tools;
    private final ChatMemory memory;

    @Override
    public String execute(String goal) {
        Assistant assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .tools(tools)
            .chatMemory(memory)
            .systemMessageProvider(this::getSystemPrompt)
            .build();

        return assistant.chat(goal);
    }

    private String getSystemPrompt() {
        return """
            당신은 AI 업데이트 추적 전문가입니다.

            역할:
            - 빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta)의 최신 업데이트를 추적
            - 중요한 업데이트를 식별하고 포스팅

            사용 가능한 도구:
            - fetch_rss_feed: RSS 피드 조회
            - fetch_github_releases: GitHub 릴리스 조회
            - scrape_web_page: 웹 페이지 크롤링
            - search_ai_updates: 기존 업데이트 검색 (중복 방지용)
            - create_draft_post: 초안 포스트 생성
            - publish_post: 포스트 게시
            - send_slack_notification: Slack 알림

            규칙:
            1. 항상 기존 업데이트 검색으로 중복 확인
            2. 중요도가 높은 업데이트(모델 출시, 주요 API 변경)만 포스팅
            3. 확신이 낮으면 Slack으로 관리자에게 확인 요청
            4. 작업 완료 후 결과 요약 제공
            """;
    }
}

interface Assistant {
    String chat(String userMessage);
}
```

### 3. Agent 트리거 방식 설계
Agent를 실행하는 3가지 방식:

#### A. REST API 트리거
```java
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AiUpdateAgent agent;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<AgentResultDto>> runAgent(
        @RequestBody AgentRunRequest request,
        @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        validateApiKey(apiKey);
        String result = agent.execute(request.goal());
        return ResponseEntity.ok(ApiResponse.success(new AgentResultDto(result)));
    }
}
```

#### B. 스케줄 트리거
```java
@Component
public class AgentScheduler {

    private final AiUpdateAgent agent;

    @Scheduled(cron = "0 0 */6 * * *") // 6시간마다
    public void scheduledRun() {
        String goal = "OpenAI, Anthropic, Google, Meta의 최신 업데이트를 확인하고 " +
                      "중요한 것만 포스팅해줘. 이미 포스팅된 것은 제외해.";
        agent.execute(goal);
    }
}
```

#### C. Slack 명령어 트리거 (선택)
```java
@RestController
@RequestMapping("/api/v1/slack")
public class SlackCommandController {

    @PostMapping("/commands")
    public ResponseEntity<String> handleCommand(
        @RequestParam("command") String command,
        @RequestParam("text") String text
    ) {
        if ("/ai-update".equals(command)) {
            agent.execute(text);
            return ResponseEntity.ok("Agent 실행을 시작했습니다.");
        }
        return ResponseEntity.ok("알 수 없는 명령입니다.");
    }
}
```

### 4. Memory 및 State 관리
```java
@Configuration
public class AgentMemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        // 최근 20개 메시지 유지
        return MessageWindowChatMemory.withMaxMessages(20);
    }

    // 또는 Redis 기반 영구 메모리
    @Bean
    public ChatMemory persistentChatMemory(RedisTemplate<String, String> redis) {
        return new RedisChatMemory(redis, "agent:memory");
    }
}
```

### 5. 실행 로그 및 모니터링
```java
@Aspect
@Component
public class AgentLoggingAspect {

    @Around("execution(* *.AiUpdateAgent.execute(..))")
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String goal = (String) joinPoint.getArgs()[0];
        log.info("Agent 실행 시작: goal={}", goal);

        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long elapsed = System.currentTimeMillis() - start;

        log.info("Agent 실행 완료: elapsed={}ms, result={}", elapsed, result);
        return result;
    }
}
```

### 6. 에러 처리 및 Fallback
```java
@Service
public class AiUpdateAgentImpl implements AiUpdateAgent {

    @Override
    public String execute(String goal) {
        try {
            return doExecute(goal);
        } catch (ToolExecutionException e) {
            log.error("Tool 실행 실패: {}", e.getMessage());
            slackService.send("#alerts", "Agent Tool 실행 실패: " + e.getMessage());
            return "실행 중 오류 발생: " + e.getMessage();
        } catch (Exception e) {
            log.error("Agent 실행 실패", e);
            slackService.send("#alerts", "Agent 실행 실패: " + e.getMessage());
            return "예상치 못한 오류 발생";
        }
    }
}
```

### 7. 테스트 설계
```java
@SpringBootTest
class AiUpdateAgentTest {

    @MockBean
    private AiUpdateTools tools;

    @Autowired
    private AiUpdateAgent agent;

    @Test
    void execute_shouldFetchAndCreatePost() {
        // Given
        when(tools.fetchRssFeed(anyString())).thenReturn(mockArticles());
        when(tools.searchAiUpdates(anyString(), anyString())).thenReturn(List.of());

        // When
        String result = agent.execute("OpenAI 최신 업데이트를 확인해줘");

        // Then
        verify(tools).fetchRssFeed(contains("openai"));
        verify(tools).createDraftPost(any(), any(), eq("OPENAI"), any());
    }
}
```

## Agent 행동 흐름 예시
```
User: "OpenAI와 Anthropic 최신 업데이트 확인하고 포스팅해줘"

Agent 추론:
1. "먼저 OpenAI 블로그 RSS를 확인해볼게"
   → Tool: fetch_rss_feed("https://openai.com/blog/rss")
   → 결과: GPT-5 출시 기사 발견

2. "이미 포스팅했는지 확인해볼게"
   → Tool: search_ai_updates("GPT-5", "OPENAI")
   → 결과: 없음

3. "중요한 업데이트니까 초안 만들자"
   → Tool: create_draft_post(title, content, "OPENAI", "MODEL_RELEASE")
   → 결과: Post ID 12345

4. "이건 확실히 중요하니까 바로 게시하자"
   → Tool: publish_post("12345")
   → 결과: 성공

5. "이제 Anthropic 확인해볼게"
   → Tool: fetch_github_releases("anthropics", "anthropic-sdk-python")
   ...

Agent 응답: "OpenAI GPT-5 출시 소식을 포스팅했습니다. Anthropic SDK에는 새 릴리스가 없었습니다."
```

## 제약 조건
- Agent는 단일 목표에 집중 (복잡한 멀티 에이전트 구조 지양)
- Tool 호출 횟수 제한 고려 (무한 루프 방지)
- LLM API 비용 최적화 (불필요한 호출 최소화)
- 오버엔지니어링 금지: 기본 Agent 패턴만 구현

## 산출물
1. Agent 인터페이스 및 구현 클래스 설계
2. System Prompt 설계
3. 트리거 방식별 Controller/Scheduler 설계
4. Memory 설정
5. 테스트 케이스 명세

## 참고 자료
- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- LangChain4j Chat Memory: https://docs.langchain4j.dev/tutorials/chat-memory
