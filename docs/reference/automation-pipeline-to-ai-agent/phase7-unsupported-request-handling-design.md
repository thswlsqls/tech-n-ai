# Phase 7: AI Agent 미지원 요청 처리 개선 설계서

## 1. 개요

### 1.1 목적

`api-agent` 모듈의 AI Agent가 지원되지 않는 대상(LangChain, LlamaIndex, Hugging Face 등)에 대한 요청을 받았을 때, 관련 없는 데이터를 수집하는 대신 **명확한 미지원 안내 메시지**를 반환하도록 System Prompt를 개선한다.

### 1.2 전제 조건

- Phase 1~6 완료: 데이터 수집 파이프라인, LangChain4j Tool 래퍼, Agent 통합, 분석 Tool, 데이터 수집 Tool, 조회 Tool
- `api-agent` 모듈: LangChain4j 1.10.0 + OpenAI GPT-4o-mini 설정 완료
- Agent는 **5개 Provider(OPENAI, ANTHROPIC, GOOGLE, META, XAI)만 지원**

### 1.3 변경 요약

| 구분 | 항목 | 설명 |
|------|------|------|
| 수정 | `AgentPromptConfig.java` | `constraints` 필드 추가, `buildPrompt()` 메서드 수정 |
| 수정 (선택) | `application-agent-api.yml` | `agent.prompt.constraints` 외부 설정 지원 |

---

## 2. 문제 분석

### 2.1 테스트 케이스

**테스트 5번: 여러 AI 도구 정보 수집**

```json
{
  "goal": "LangChain, LlamaIndex, Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

### 2.2 기대 동작 vs 실제 동작

| 구분 | 내용 |
|------|------|
| **기대 동작** | "LangChain, LlamaIndex, Hugging Face는 현재 지원되지 않습니다. 지원되는 Provider: OpenAI, Anthropic, Google, Meta, xAI" |
| **실제 동작** | Anthropic 블로그 14건, Meta 블로그 18건, GitHub 30건, RSS 60건 수집 (요청과 무관한 데이터) |

### 2.3 지원 범위 정의

| Provider | 지원 여부 | 비고 |
|----------|----------|------|
| OPENAI | O | GPT, ChatGPT, DALL-E, Whisper 등 |
| ANTHROPIC | O | Claude 등 |
| GOOGLE | O | Gemini, PaLM, Bard 등 |
| META | O | LLaMA, Code Llama 등 |
| XAI | O | Grok 등 |
| LangChain | **X** | 프레임워크 - 미지원 |
| LlamaIndex | **X** | 프레임워크 - 미지원 |
| Hugging Face | **X** | 플랫폼 - 미지원 |
| Mistral | **X** | 미지원 |
| Cohere | **X** | 미지원 |

### 2.4 문제점 분석

| # | 문제 | 원인 | 영향 |
|---|------|------|------|
| P1 | 요청-응답 불일치 | LLM이 지원 범위를 인식하지 못함 | 사용자가 원하지 않는 데이터 수신 |
| P2 | 미지원 안내 부재 | System Prompt에 지원 범위 미명시 | 사용자 혼란 |
| P3 | 불필요한 리소스 소비 | 미지원 요청에도 Tool 호출 실행 | API 비용, 처리 시간 낭비 |

---

## 3. 아키텍처 설계

### 3.1 개선 접근 방식

**System Prompt 개선만으로 해결** (Tool 코드 수정 없음)

```
사용자 요청
    |
    v
[System Prompt: constraints 섹션 추가]
    ├── 지원 범위 명시: OPENAI, ANTHROPIC, GOOGLE, META, XAI
    └── 미지원 요청 처리 규칙 명시
    |
    v
LLM이 요청 분석
    ├── 지원 대상 → Tool 호출 및 결과 반환
    └── 미지원 대상 → Tool 미호출, 안내 메시지 반환
```

### 3.2 설계 원칙

| 원칙 | 설명 |
|------|------|
| 최소 변경 | Tool 코드, 검증 로직 수정 없음 |
| 외부 설정 지원 | `application.yml`로 constraints 커스터마이징 가능 |
| 명확한 안내 | 미지원 대상과 지원 대상을 명확히 구분하여 안내 |
| 부분 지원 처리 | 일부만 지원되는 경우 지원 대상만 처리 |

---

## 4. 상세 설계

### 4.1 AgentPromptConfig 변경

**파일**: `api/agent/src/main/java/com/ebson/shrimp/tm/demo/api/agent/config/AgentPromptConfig.java`

#### 4.1.1 현재 구조

```java
@Data
@Component
@ConfigurationProperties(prefix = "agent.prompt")
public class AgentPromptConfig {
    private String role;          // 역할 정의
    private String tools;         // Tool 목록
    private String repositories;  // 저장소 정보
    private String rules;         // 사용 규칙
    private String visualization; // 시각화 가이드

    public String buildPrompt(String goal) {
        return """
            %s

            ## 역할
            - 빅테크 IT 기업(OpenAI, Anthropic, Google, Meta, xAI)의 최신 업데이트 추적
            - 데이터 분석 결과를 도표와 차트로 시각화하여 제공

            ## 사용 가능한 도구
            %s

            ## 주요 저장소 정보
            %s

            ## 규칙
            %s

            %s

            ## 사용자 요청
            %s
            """.formatted(role, tools, repositories, rules, visualization, goal);
    }
}
```

#### 4.1.2 변경 후 구조

```java
@Data
@Component
@ConfigurationProperties(prefix = "agent.prompt")
public class AgentPromptConfig {
    private String role = "당신은 Emerging Tech 데이터 분석 및 업데이트 추적 전문가입니다.";

    private String constraints = """
        ## 지원 범위 제한

        이 Agent는 다음 5개 Provider의 AI 기술 업데이트만 추적합니다:
        - OPENAI: OpenAI 관련 (GPT, ChatGPT, DALL-E, Whisper, Codex 등)
        - ANTHROPIC: Anthropic 관련 (Claude 등)
        - GOOGLE: Google AI 관련 (Gemini, PaLM, Bard 등)
        - META: Meta AI 관련 (LLaMA, Code Llama 등)
        - XAI: xAI 관련 (Grok 등)

        ## 미지원 요청 처리 규칙

        1. 사용자 요청에서 대상을 먼저 분석합니다.
        2. 지원되지 않는 대상이 포함된 경우:
           - 미지원 대상: LangChain, LlamaIndex, Hugging Face, Mistral, Cohere, Stability AI, Midjourney 등
           - Tool을 호출하지 않습니다.
           - 다음 형식으로 안내합니다:
             "죄송합니다. [요청 대상]은(는) 현재 지원되지 않습니다.
              현재 지원되는 Provider: OpenAI, Anthropic, Google, Meta, xAI
              위 Provider에 대한 정보가 필요하시면 말씀해 주세요."
        3. 일부만 지원되는 경우:
           - 지원되는 대상에 대해서만 작업을 수행합니다.
           - 미지원 대상은 별도로 안내합니다.
           - 예: "OpenAI 정보는 수집하겠습니다. 단, Hugging Face는 현재 지원되지 않습니다."
        4. 모호한 요청인 경우:
           - "AI 업데이트", "최신 정보" 등 특정 대상이 명시되지 않은 경우
           - 지원되는 5개 Provider 전체에 대해 작업을 수행합니다.
        """;

    private String tools = """
        - fetch_github_releases: GitHub 저장소 릴리스 조회
        - scrape_web_page: 웹 페이지 크롤링
        - list_emerging_techs: 기간/Provider/UpdateType/SourceType/Status별 목록 조회 (페이징 지원)
        - get_emerging_tech_detail: ID로 상세 조회
        - search_emerging_techs: 제목 키워드 검색
        - get_emerging_tech_statistics: Provider/SourceType/기간별 통계 집계
        - analyze_text_frequency: 키워드 빈도 분석 (Word Cloud)
        - send_slack_notification: Slack 알림 전송 (현재 비활성화 - Mock 응답)
        - collect_github_releases: GitHub 저장소 릴리스 수집 및 DB 저장
        - collect_rss_feeds: OpenAI/Google 블로그 RSS 피드 수집 및 DB 저장
        - collect_scraped_articles: Anthropic/Meta 블로그 크롤링 및 DB 저장""";

    private String repositories = """
        - OpenAI: openai/openai-python
        - Anthropic: anthropics/anthropic-sdk-python
        - Google: google/generative-ai-python
        - Meta: facebookresearch/llama
        - xAI: xai-org/grok-1""";

    private String rules = """
        1. 목록 조회 요청 시 list_emerging_techs를 사용하여 기간, Provider, UpdateType, SourceType, Status 필터를 조합
        2. 특정 항목의 상세 정보 요청 시 get_emerging_tech_detail을 사용
        3. 제목 키워드로 자유 검색 시 search_emerging_techs 사용
        4. 통계 요청 시 get_emerging_tech_statistics로 집계하고, Markdown 표와 Mermaid 차트로 정리
        5. 키워드 분석 요청 시 analyze_text_frequency로 빈도를 집계하고, Mermaid 차트와 해석을 함께 제공
        6. 데이터 수집 요청 시 fetch_github_releases, scrape_web_page 활용
        7. 중복 확인은 search_emerging_techs 사용
        8. Slack 알림은 현재 비활성화 상태. send_slack_notification 호출 시 Mock 응답이 반환됨
        9. 데이터 수집 및 저장 요청 시 collect_* 도구를 사용
        10. 전체 소스 수집 요청 시: collect_github_releases(각 저장소별) → collect_rss_feeds("") → collect_scraped_articles("") 순서로 실행
        11. 수집 결과의 신규/중복/실패 건수를 Markdown 표로 정리하여 제공
        12. 작업 완료 후 결과 요약 제공""";

    private String visualization = """
        ## 시각화 가이드
        통계 결과를 시각화할 때 Mermaid 다이어그램 문법을 사용하세요.
        프론트엔드에서 자동으로 렌더링됩니다.

        ### 파이 차트 (비율 표시에 적합)
        ```mermaid
        pie title Provider별 수집 현황
            "OPENAI" : 145
            "ANTHROPIC" : 98
            "GOOGLE" : 87
        ```

        ### 바 차트 (빈도/수량 비교에 적합)
        ```mermaid
        xychart-beta
            title "키워드 빈도 TOP 10"
            x-axis ["model", "release", "api", "update"]
            y-axis "빈도" 0 --> 350
            bar [312, 218, 187, 156]
        ```

        ### 사용 규칙
        - 비율 분석: pie 차트 사용
        - 빈도 비교: xychart-beta의 bar 사용
        - Markdown 표도 함께 제공하여 정확한 수치 확인 가능하게 함
        - Mermaid 코드 블록은 반드시 ```mermaid로 시작""";

    /**
     * System Prompt 생성
     * constraints 섹션을 추가하여 지원 범위와 미지원 요청 처리 규칙을 명시
     *
     * @param goal 사용자 요청 목표
     * @return 완성된 프롬프트
     */
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
}
```

### 4.2 application.yml 설정 (선택사항)

**파일**: `api/agent/src/main/resources/application-agent-api.yml`

```yaml
agent:
  prompt:
    role: "당신은 Emerging Tech 데이터 분석 및 업데이트 추적 전문가입니다."
    constraints: |
      ## 지원 범위 제한

      이 Agent는 다음 5개 Provider의 AI 기술 업데이트만 추적합니다:
      - OPENAI: OpenAI 관련 (GPT, ChatGPT, DALL-E, Whisper, Codex 등)
      - ANTHROPIC: Anthropic 관련 (Claude 등)
      - GOOGLE: Google AI 관련 (Gemini, PaLM, Bard 등)
      - META: Meta AI 관련 (LLaMA, Code Llama 등)
      - XAI: xAI 관련 (Grok 등)

      ## 미지원 요청 처리 규칙

      1. 사용자 요청에서 대상을 먼저 분석합니다.
      2. 지원되지 않는 대상(LangChain, LlamaIndex, Hugging Face, Mistral 등)이 포함된 경우:
         - Tool을 호출하지 않습니다.
         - 미지원 안내 메시지를 반환합니다.
      3. 일부만 지원되는 경우 지원 대상만 처리하고 미지원 대상을 안내합니다.
```

---

## 5. 구현 명세

### 5.1 변경 파일 목록

| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `AgentPromptConfig.java` | 수정 | `constraints` 필드 추가, `buildPrompt()` 수정 |
| `application-agent-api.yml` | 수정 (선택) | `agent.prompt.constraints` 외부 설정 |

### 5.2 구현 제약사항

| 제약 | 설명 |
|------|------|
| Tool 코드 미수정 | `EmergingTechAgentTools.java` 변경 없음 |
| Validator 미수정 | `ToolInputValidator.java` 변경 없음 |
| LangChain4j 설정 미수정 | `AiAgentConfig.java` 변경 없음 |
| 기존 동작 유지 | 지원 대상 요청은 기존과 동일하게 동작 |

### 5.3 변경 전후 비교

**buildPrompt() 출력 구조 비교:**

| 섹션 | 변경 전 | 변경 후 |
|------|---------|---------|
| role | O | O |
| 역할 | O | O |
| **constraints** | **X** | **O (추가)** |
| 사용 가능한 도구 | O | O |
| 주요 저장소 정보 | O | O |
| 규칙 | O | O |
| 시각화 가이드 | O | O |
| 사용자 요청 | O | O |

---

## 6. 검증 시나리오

### 6.1 시나리오 1: 완전 미지원 요청

**입력:**
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

위 Provider에 대한 정보가 필요하시면 말씀해 주세요.
```

**검증 항목:**
- HTTP 200 응답
- `toolCallCount = 0` (Tool 미호출)
- `summary`에 "지원" 또는 "죄송" 포함

### 6.2 시나리오 2: 부분 지원 요청

**입력:**
```json
{
  "goal": "OpenAI와 Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

**예상 응답:**
```
OpenAI에 대한 정보를 수집하겠습니다.
단, Hugging Face는 현재 지원되지 않는 대상입니다.

## OpenAI 수집 결과
[OpenAI 데이터 표시]
```

**검증 항목:**
- HTTP 200 응답
- `toolCallCount >= 1` (OpenAI용 Tool 호출)
- `summary`에 OpenAI 결과 및 Hugging Face 미지원 안내 포함

### 6.3 시나리오 3: 완전 지원 요청 (기존 동작 유지)

**입력:**
```json
{
  "goal": "OpenAI의 최신 릴리스 정보를 수집해주세요."
}
```

**예상 응답:**
- 기존과 동일하게 OpenAI 데이터 수집 및 반환

**검증 항목:**
- HTTP 200 응답
- `toolCallCount >= 1`
- 정상적인 수집 결과 반환

### 6.4 시나리오 4: 모호한 요청 (전체 수집)

**입력:**
```json
{
  "goal": "최근 AI 업데이트 정보를 수집해주세요."
}
```

**예상 응답:**
- 지원되는 5개 Provider 전체에 대해 수집 수행

**검증 항목:**
- HTTP 200 응답
- `toolCallCount >= 1`
- 5개 Provider 데이터 포함

---

## 7. 테스트 계획

### 7.1 HTTP 테스트 추가

**파일**: `api/agent/src/test/http/01-agent-run.http`

```http
### 40. 미지원 Provider 전체 요청 테스트
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

### 41. 부분 지원 Provider 요청 테스트
POST {{gatewayUrl}}/api/v1/agent/run
Content-Type: application/json
x-user-id: {{adminId}}
Authorization: Bearer {{adminAccessToken}}

{
  "goal": "OpenAI와 Hugging Face의 최신 릴리스 정보를 수집해주세요."
}

> {%
    client.test("부분 지원 요청 - HTTP 200", function() {
        client.assert(response.status === 200, "응답 상태 코드가 200이어야 합니다");
        client.assert(response.body.code === "2000", "code가 2000이어야 합니다");
    });

    client.test("부분 지원 요청 - Tool 호출됨", function() {
        var data = response.body.data;
        client.assert(data.toolCallCount >= 1, "지원 대상에 대해 Tool이 호출되어야 합니다");
    });

    client.test("부분 지원 요청 - 미지원 안내 포함", function() {
        var summary = response.body.data.summary;
        client.assert(summary.includes("Hugging Face") || summary.includes("지원"),
            "미지원 대상 안내가 포함되어야 합니다");
    });
%}

### 42. Mistral 미지원 테스트
POST {{gatewayUrl}}/api/v1/agent/run
Content-Type: application/json
x-user-id: {{adminId}}
Authorization: Bearer {{adminAccessToken}}

{
  "goal": "Mistral AI의 최신 모델 정보를 알려주세요."
}

> {%
    client.test("Mistral 요청 - HTTP 200", function() {
        client.assert(response.status === 200, "응답 상태 코드가 200이어야 합니다");
    });

    client.test("Mistral 요청 - 미지원 안내", function() {
        var summary = response.body.data.summary;
        client.assert(summary.includes("지원") || summary.includes("죄송"),
            "미지원 안내 메시지가 포함되어야 합니다");
    });
%}
```

---

## 8. 구현 체크리스트

### 8.1 코드 변경

- [ ] `AgentPromptConfig.java`에 `constraints` 필드 추가
- [ ] `AgentPromptConfig.java`의 `buildPrompt()` 메서드에 `constraints` 포함
- [ ] (선택) `application-agent-api.yml`에 `agent.prompt.constraints` 설정 추가

### 8.2 테스트

- [ ] 미지원 Provider 전체 요청 테스트 (LangChain, LlamaIndex, Hugging Face)
- [ ] 부분 지원 요청 테스트 (OpenAI + Hugging Face)
- [ ] 완전 지원 요청 테스트 (OpenAI만) - 기존 동작 유지 확인
- [ ] 모호한 요청 테스트 (특정 대상 미지정)
- [ ] 다른 미지원 대상 테스트 (Mistral, Cohere 등)

### 8.3 문서화

- [ ] HTTP 테스트 파일에 신규 테스트 케이스 추가
- [ ] 테스트 결과 문서 업데이트

---

## 9. 참고 자료

### 9.1 LangChain4j 공식 문서

- [System Messages](https://docs.langchain4j.dev/tutorials/chat-and-language-models#system-messages)
- [AI Services](https://docs.langchain4j.dev/tutorials/ai-services)

### 9.2 관련 문서

- 프롬프트: `prompts/reference/automation-pipeline-to-ai-agent/phase7-unsupported-request-handling-prompt.md`
- 테스트 결과: `docs/reference/automation-pipeline-to-ai-agent/tests/01-agent-run-test-results.md` (테스트 5번)

### 9.3 관련 설계서

- Phase 3: `phase3-agent-integration-design.md`
- Phase 5: `phase5-data-collection-agent-design.md`
- Phase 6: `phase6-agent-query-tool-improvement-design.md`
