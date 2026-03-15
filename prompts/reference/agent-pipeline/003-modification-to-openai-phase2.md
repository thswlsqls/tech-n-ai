# Phase 2 설계서 수정 프롬프트 - GPT-4o-mini 기반으로 변경

## 목표
`/docs/reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md` 설계서에서 Anthropic Claude 모델 대신 OpenAI GPT-4o-mini 모델을 사용하도록 수정한다.

## 수정 대상 파일
`/Users/r00442/Documents/workspace/shrimp-tm-demo/docs/reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md`

## 수정 사항

### 1. 의존성 설정 섹션 (2.2) 수정

**변경 전:**
```gradle
// Anthropic 모델 지원 (Agent용)
implementation 'dev.langchain4j:langchain4j-anthropic:0.35.0'
```

**변경 후:**
```gradle
// OpenAI 모델 지원 (Agent용) - 기존 의존성 활용
// implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0' (이미 존재)
```

**설명:** Anthropic 의존성 추가 부분 삭제. 기존 프로젝트에 이미 `langchain4j-open-ai:0.35.0` 의존성이 존재하므로 추가 의존성 불필요.

### 2. Tool 등록 및 설정 섹션 (7.1) 수정

**변경 전:**
```java
@Bean("agentChatModel")
public ChatLanguageModel agentChatLanguageModel() {
    return AnthropicChatModel.builder()
        .apiKey(anthropicApiKey)
        .modelName("claude-sonnet-4-20250514")
        .temperature(0.3)
        .maxTokens(4096)
        .timeout(Duration.ofSeconds(120))
        .logRequests(true)
        .logResponses(true)
        .build();
}
```

**변경 후:**
```java
@Bean("agentChatModel")
public ChatLanguageModel agentChatLanguageModel() {
    return OpenAiChatModel.builder()
        .apiKey(openAiApiKey)  // 기존 OpenAI API 키 재사용
        .modelName("gpt-4o-mini")
        .temperature(0.3)  // Tool 호출에는 낮은 temperature
        .maxTokens(4096)
        .timeout(Duration.ofSeconds(120))
        .logRequests(true)
        .logResponses(true)
        .build();
}
```

### 3. 설정 파일 섹션 (7.2) 수정

**변경 전:**
```yaml
langchain4j:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY}
    model-name: claude-sonnet-4-20250514
```

**변경 후:**
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}  # 기존 API 키 재사용
      model-name: gpt-4o-mini     # Agent용 모델
    embedding-model:
      api-key: ${OPENAI_API_KEY}
      model-name: text-embedding-3-small  # 기존 유지
```

### 4. import 문 수정

**변경 전:**
```java
import dev.langchain4j.model.anthropic.AnthropicChatModel;
```

**변경 후:**
```java
import dev.langchain4j.model.openai.OpenAiChatModel;
```

## 제약 조건
- 기존 `api-chatbot` 모듈의 LangChain4jConfig 패턴과 일관성 유지
- 기존 OpenAI API 키 재사용 (별도 Anthropic API 키 불필요)
- 오버엔지니어링 금지

## 참고 자료
- 기존 설정: `/api/chatbot/src/main/java/.../config/LangChain4jConfig.java`
- LangChain4j OpenAI: https://docs.langchain4j.dev/integrations/language-models/open-ai
- OpenAI API Pricing: https://openai.com/api/pricing/
