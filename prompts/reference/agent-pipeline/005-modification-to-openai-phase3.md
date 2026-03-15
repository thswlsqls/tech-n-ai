# Phase 3 설계서 수정 프롬프트 - GPT-4o-mini 기반으로 변경

## 목표
`/docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md` 설계서에서 Anthropic Claude 모델 대신 OpenAI GPT-4o-mini 모델을 사용하도록 수정한다.

## 수정 대상 파일
`/Users/r00442/Documents/workspace/shrimp-tm-demo/docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md`

## 수정 사항

### 1. Agent 설정 클래스 섹션 (5.1) 수정

**변경 전:**
```java
package com.ebson.shrimp.tm.demo.api.chatbot.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
// ...

@Configuration
public class AiAgentConfig {

    @Value("${langchain4j.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${langchain4j.anthropic.model-name:claude-sonnet-4-20250514}")
    private String modelName;

    @Bean("agentChatModel")
    public ChatLanguageModel agentChatLanguageModel() {
        log.info("Agent ChatLanguageModel 초기화: model={}", modelName);

        return AnthropicChatModel.builder()
            .apiKey(anthropicApiKey)
            .modelName(modelName)
            .temperature(0.3)
            .maxTokens(4096)
            .timeout(Duration.ofSeconds(120))
            .logRequests(true)
            .logResponses(true)
            .build();
    }
}
```

**변경 후:**
```java
package com.ebson.shrimp.tm.demo.api.chatbot.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
// ...

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

### 2. application.yml 설정 섹션 (5.2) 수정

**변경 전:**
```yaml
# Agent 설정
langchain4j:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
    model-name: claude-sonnet-4-20250514
```

**변경 후:**
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
```

### 3. Agent 구현체 섹션 (3.2) - 주석 수정

**변경 전:**
```java
@Qualifier("agentChatModel")
private final ChatLanguageModel chatModel;  // Anthropic Claude
```

**변경 후:**
```java
@Qualifier("agentChatModel")
private final ChatLanguageModel chatModel;  // OpenAI GPT-4o-mini
```

### 4. 개요 섹션 (1.1) - 설명 수정

**변경 전 (있을 경우):**
> Anthropic Claude 모델을 사용하여...

**변경 후:**
> OpenAI GPT-4o-mini 모델을 사용하여... (기존 OpenAI 인프라 활용)

## 통합 시 주의사항

1. **기존 LangChain4jConfig와의 관계**
   - 기존 `LangChain4jConfig`에서 이미 `@Primary` ChatLanguageModel 정의됨
   - Agent용은 `@Qualifier("agentChatModel")`로 구분하여 사용
   - 동일한 OpenAI API 키 재사용

2. **Bean 이름 충돌 방지**
   - 기존: `chatLanguageModel` (챗봇용, @Primary)
   - 추가: `agentChatModel` (Agent용, @Qualifier 지정)

3. **설정 파일 구조**
   ```yaml
   langchain4j:
     open-ai:
       chat-model:        # 기존 챗봇용 (gpt-4o-mini)
       agent-model:       # Agent용 (gpt-4o-mini, 동일 또는 gpt-4o로 업그레이드 가능)
       embedding-model:   # 임베딩용 (기존 유지)
   ```

## 제약 조건
- 기존 `api-chatbot` 모듈의 구조 유지
- 기존 OpenAI API 키 단일 사용 (별도 키 불필요)
- 오버엔지니어링 금지 - 필요한 수정만 진행

## 참고 자료
- 기존 설정: `/api/chatbot/src/main/java/.../config/LangChain4jConfig.java`
- LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services
- LangChain4j OpenAI: https://docs.langchain4j.dev/integrations/language-models/open-ai
- OpenAI API: https://platform.openai.com/docs/api-reference
