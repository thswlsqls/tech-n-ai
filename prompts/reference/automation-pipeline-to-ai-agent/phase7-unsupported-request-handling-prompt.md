# Phase 7: AI Agent 미지원 요청 처리 개선 프롬프트

## 1. 검증 대상

### 1.1 설계서
- `docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase5-data-collection-agent-design.md`

### 1.2 대상 모듈
- `api/agent` (AI Agent 모듈, 포트 8087)

### 1.3 관련 파일
- `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/config/AgentPromptConfig.java`
- `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/EmergingTechAgentTools.java`
- `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/tool/validation/ToolInputValidator.java`

---

## 2. 검증 결과 요약

### 2.1 테스트 케이스

**테스트 5번: 여러 AI 도구 정보 수집**

```json
{
  "goal": "LangChain, LlamaIndex, Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

### 2.2 기대 동작

사용자가 요청한 `LangChain, LlamaIndex, Hugging Face`는 Agent에 등록된 Tool들의 **지원 범위 밖**입니다.

| 지원 Provider | Tool 지원 여부 |
|--------------|----------------|
| OPENAI | O |
| ANTHROPIC | O |
| GOOGLE | O |
| META | O |
| XAI | O |
| **LangChain** | **X** |
| **LlamaIndex** | **X** |
| **Hugging Face** | **X** |

**기대 응답:**
```
죄송합니다. 현재 LangChain, LlamaIndex, Hugging Face는 지원되지 않는 대상입니다.
현재 지원되는 Provider는 다음과 같습니다: OpenAI, Anthropic, Google, Meta, xAI
```

### 2.3 실제 동작

Agent가 **지원되지 않는 요청을 인식하지 못하고**, 관련 없는 데이터를 수집하여 반환했습니다:

| 수집 대상 | 수집 건수 |
|----------|----------|
| Anthropic 블로그 | 14건 |
| Meta 블로그 | 18건 |
| GitHub (OpenAI, Google, Anthropic) | 각 10건 |
| RSS (OpenAI, Google) | 60건 |

### 2.4 문제점 분석

| # | 문제 | 원인 |
|---|------|------|
| P1 | 요청-응답 불일치 | LLM이 지원 범위를 인식하지 못하고 유사한 작업 수행 |
| P2 | 미지원 안내 부재 | System Prompt에 지원 범위와 미지원 요청 처리 지침 미명시 |
| P3 | Fallback 로직 부재 | 요청 분석 후 지원 가능 여부 판단 로직 없음 |

---

## 3. 개선 요구 사항

### 3.1 System Prompt 개선: 지원 범위 명시 및 미지원 요청 처리 규칙 추가

#### 현재 AgentPromptConfig 구조

```java
@ConfigurationProperties(prefix = "agent.prompt")
public class AgentPromptConfig {
    private String role;        // 역할 정의
    private String tools;       // Tool 목록
    private String repositories; // 저장소 정보
    private String rules;       // 사용 규칙
    private String visualization; // 시각화 가이드
}
```

#### 개선 방안: `constraints` 필드 추가

```java
private String constraints = """
    ## 지원 범위 제한
    이 Agent는 다음 5개 Provider의 AI 기술 업데이트만 추적합니다:
    - OPENAI: OpenAI 관련 (GPT, ChatGPT, DALL-E 등)
    - ANTHROPIC: Anthropic 관련 (Claude 등)
    - GOOGLE: Google AI 관련 (Gemini, PaLM 등)
    - META: Meta AI 관련 (LLaMA 등)
    - XAI: xAI 관련 (Grok 등)

    ## 미지원 요청 처리 규칙
    1. 사용자 요청에서 대상을 분석합니다
    2. 지원되지 않는 대상(LangChain, LlamaIndex, Hugging Face, Mistral, Cohere 등)이 포함된 경우:
       - Tool을 호출하지 않습니다
       - 다음 형식으로 안내합니다:
         "죄송합니다. [요청 대상]은(는) 현재 지원되지 않습니다.
          현재 지원되는 Provider: OpenAI, Anthropic, Google, Meta, xAI"
    3. 일부만 지원되는 경우:
       - 지원되는 대상에 대해서만 작업을 수행합니다
       - 미지원 대상은 별도로 안내합니다
    """;
```

#### buildPrompt 메소드 수정

```java
public String buildPrompt(String goal) {
    return """
        %s

        ## 역할
        - 빅테크 IT 기업(OpenAI, Anthropic, Google, Meta, xAI)의 최신 업데이트 추적
        - 데이터 분석 결과를 도표와 차트로 시각화하여 제공

        %s

        ## 사용 가능한 도구
        %s

        ## 주요 저장소 정보
        %s

        ## 규칙
        %s

        %s

        ## 사용자 요청
        %s
        """.formatted(role, constraints, tools, repositories, rules, visualization, goal);
}
```

### 3.2 application.yml 설정 옵션 (선택사항)

System Prompt를 외부 설정으로 관리하는 경우:

```yaml
agent:
  prompt:
    constraints: |
      ## 지원 범위 제한
      이 Agent는 다음 5개 Provider의 AI 기술 업데이트만 추적합니다:
      - OPENAI, ANTHROPIC, GOOGLE, META, XAI

      ## 미지원 요청 처리 규칙
      1. 사용자 요청에서 대상을 분석합니다
      2. 지원되지 않는 대상이 포함된 경우 Tool을 호출하지 말고 안내합니다
      3. 일부만 지원되는 경우 지원 대상만 처리하고 미지원 대상을 안내합니다
```

---

## 4. 구현 범위

### 4.1 수정 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `AgentPromptConfig.java` | `constraints` 필드 추가, `buildPrompt()` 수정 |
| `application.yml` (선택) | agent.prompt.constraints 기본값 설정 |

### 4.2 구현 제약

- Tool 코드 수정 없음 (System Prompt만 개선)
- 기존 Tool 동작 영향 없음
- LangChain4j 설정 변경 없음

---

## 5. 검증 시나리오

### 5.1 미지원 요청 테스트

```json
{
  "goal": "LangChain, LlamaIndex, Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

**예상 응답:**
```
죄송합니다. LangChain, LlamaIndex, Hugging Face는 현재 지원되지 않는 대상입니다.

현재 지원되는 Provider:
- OpenAI (GPT, ChatGPT, DALL-E 등)
- Anthropic (Claude 등)
- Google (Gemini, PaLM 등)
- Meta (LLaMA 등)
- xAI (Grok 등)

위 Provider에 대한 정보 수집을 원하시면 말씀해 주세요.
```

### 5.2 부분 지원 요청 테스트

```json
{
  "goal": "OpenAI와 Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

**예상 응답:**
```
OpenAI에 대해서는 정보를 수집하겠습니다.
단, Hugging Face는 현재 지원되지 않는 대상입니다.

[OpenAI 수집 결과 표시]
```

### 5.3 지원 요청 테스트 (기존 동작 유지)

```json
{
  "goal": "OpenAI의 최신 릴리스 정보를 수집해주세요."
}
```

**예상 응답:** 기존과 동일하게 OpenAI 데이터 수집 및 반환

---

## 6. 테스트 계획

### 6.1 HTTP 테스트 추가

`api/agent/src/test/http/01-agent-run.http`에 테스트 케이스 추가:

```http
### 미지원 Provider 요청 테스트
POST {{gatewayUrl}}/api/v1/agent/run
Content-Type: application/json
x-user-id: {{adminId}}
Authorization: Bearer {{adminAccessToken}}

{
  "goal": "LangChain, LlamaIndex, Hugging Face의 최신 릴리스 정보를 수집해주세요."
}

> {%
    client.test("미지원 Provider 요청 - HTTP 200", function() {
        client.assert(response.status === 200, "응답 상태 코드가 200이어야 합니다");
        client.assert(response.body.code === "2000", "code가 2000이어야 합니다");
    });

    client.test("미지원 Provider 요청 - 안내 메시지 포함", function() {
        var summary = response.body.data.summary;
        client.assert(summary.includes("지원") || summary.includes("죄송"),
            "미지원 안내 메시지가 포함되어야 합니다");
    });

    client.test("미지원 Provider 요청 - Tool 미호출", function() {
        var data = response.body.data;
        client.assert(data.toolCallCount === 0, "Tool이 호출되지 않아야 합니다");
    });
%}
```

---

## 7. 참고 사항

### 7.1 LangChain4j 공식 문서

- [System Messages](https://docs.langchain4j.dev/tutorials/chat-and-language-models#system-messages)
- [AI Services](https://docs.langchain4j.dev/tutorials/ai-services)

### 7.2 관련 검증 테스트 결과

- 문서: `docs/reference/automation-pipeline-to-ai-agent/tests/01-agent-run-test-results.md`
- 테스트 케이스 5번 응답 참조
