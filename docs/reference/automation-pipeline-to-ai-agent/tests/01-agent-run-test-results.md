# 01-agent-run.http 테스트 결과

> 테스트 실행일: 2026-02-05

## 테스트 개요

Agent 엔드포인트(`POST /api/v1/agent/run`)의 정상/실패/엣지 케이스 테스트 결과입니다.

---

## 성공 케이스 (Success Cases)

### 1. 기본 Agent 실행 (sessionId 자동 생성)

**Request:**
```json
{
  "goal": "최근 AI 업데이트 정보를 수집해주세요."
}
```

**Response Summary:**

최근 AI 업데이트 정보 수집 결과는 다음과 같습니다:

### GitHub Releases
| Provider   | 총 수집 | 총 처리 | 신규 | 중복 | 실패 |
|------------|----------|---------|------|------|------|
| OPENAI     | 10       | 10      | 0    | 10   | 0    |
| ANTHROPIC  | 10       | 10      | 0    | 10   | 0    |
| GOOGLE     | 10       | 10      | 0    | 10   | 0    |
| META       | 0        | 0       | 0    | 0    | 0    |
| XAI        | 0        | 0       | 0    | 0    | 0    |

### RSS Feeds
| Provider   | 총 수집 | 총 처리 | 신규 | 중복 | 실패 |
|------------|----------|---------|------|------|------|
| ALL        | 852      | 60      | 0    | 60   | 0    |

### Web Scraping
| Provider   | 총 수집 | 총 처리 | 신규 | 중복 | 실패 |
|------------|----------|---------|------|------|------|
| ALL        | 32       | 32      | 0    | 32   | 0    |

### 요약
- **GitHub Releases**: OpenAI, Anthropic, Google의 모든 릴리스가 중복으로 처리되었습니다.
- **RSS Feeds**: 총 852개의 피드가 수집되었으나, 60개는 중복으로 처리되었습니다.
- **Web Scraping**: 32개의 웹 페이지가 수집되었으나, 모두 중복으로 처리되었습니다.

이 데이터는 AI 기술 업데이트를 추적하는 데 유용할 것입니다. 추가적으로 필요한 정보가 있으시면 말씀해 주세요!

**실행 정보:**
- toolCallCount: 7
- analyticsCallCount: 0
- executionTimeMs: 35033

---

### 2. sessionId 지정하여 실행

**Request:**
```json
{
  "goal": "GitHub에서 최신 AI 프레임워크 릴리스 정보를 확인해주세요.",
  "sessionId": "admin-563108741828608000-4636218e"
}
```

**Response Summary:**

다음은 GitHub에서 수집한 최신 AI 프레임워크 릴리스 정보입니다.

### OpenAI (openai-python)
| 버전   | 발행일                  | 릴리스 노트 링크                                                                 |
|--------|-----------------------|---------------------------------------------------------------------------------|
| v2.16.0 | 2026-01-27           | [릴리스 노트](https://github.com/openai/openai-python/releases/tag/v2.16.0) |
| v2.15.0 | 2026-01-09           | [릴리스 노트](https://github.com/openai/openai-python/releases/tag/v2.15.0) |
| v2.14.0 | 2025-12-19           | [릴리스 노트](https://github.com/openai/openai-python/releases/tag/v2.14.0) |
| v2.13.0 | 2025-12-16           | [릴리스 노트](https://github.com/openai/openai-python/releases/tag/v2.13.0) |
| v2.12.0 | 2025-12-15           | [릴리스 노트](https://github.com/openai/openai-python/releases/tag/v2.12.0) |

### Anthropic (anthropic-sdk-python)
| 버전   | 발행일                  | 릴리스 노트 링크                                                                   |
|--------|-----------------------|-----------------------------------------------------------------------------------|
| v0.77.1 | 2026-02-03           | [릴리스 노트](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.77.1) |
| v0.77.0 | 2026-01-29           | [릴리스 노트](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.77.0) |
| v0.76.0 | 2026-01-13           | [릴리스 노트](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.76.0) |
| v0.75.0 | 2025-11-24           | [릴리스 노트](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.75.0) |
| v0.74.1 | 2025-11-19           | [릴리스 노트](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.74.1) |

### Google (generative-ai-python)
| 버전   | 발행일                  | 릴리스 노트 링크                                                                 |
|--------|-----------------------|---------------------------------------------------------------------------------|
| v0.8.6  | 2025-12-16           | [릴리스 노트](https://github.com/google-gemini/deprecated-generative-ai-python/releases/tag/v0.8.6) |
| v0.8.5  | 2025-04-17           | [릴리스 노트](https://github.com/google-gemini/deprecated-generative-ai-python/releases/tag/v0.8.5) |
| v0.8.4  | 2025-01-21           | [릴리스 노트](https://github.com/google-gemini/deprecated-generative-ai-python/releases/tag/v0.8.4) |
| v0.8.3  | 2024-10-31           | [릴리스 노트](https://github.com/google-gemini/deprecated-generative-ai-python/releases/tag/v0.8.3) |
| v0.8.2  | 2024-09-24           | [릴리스 노트](https://github.com/google-gemini/deprecated-generative-ai-python/releases/tag/v0.8.2) |

### Meta (LLaMA)
- 현재 Meta의 최신 릴리스 정보는 없습니다.

### xAI (Grok)
- 현재 xAI의 최신 릴리스 정보는 없습니다.

### 요약
- OpenAI와 Anthropic의 최신 릴리스 정보가 수집되었습니다.
- Google의 릴리스 정보는 여러 개가 수집되었으나, Meta와 xAI는 현재 정보가 없습니다.

필요한 추가 정보가 있으면 말씀해 주세요!

**실행 정보:**
- toolCallCount: 5
- analyticsCallCount: 0
- executionTimeMs: 21730

---

### 3. OpenAI 관련 정보 수집

**Request:**
```json
{
  "goal": "OpenAI의 최신 모델 업데이트 정보를 수집해주세요."
}
```

**Response Summary:**

OpenAI의 최신 모델 업데이트 정보를 수집한 결과는 다음과 같습니다.

### GitHub Releases
- **수집된 총 릴리스**: 10개
- **처리된 릴리스**: 10개
- **신규 릴리스**: 0개
- **중복 릴리스**: 10개
- **실패 릴리스**: 0개

### RSS Feeds
- **수집된 총 피드**: 832개
- **처리된 피드**: 44개
- **신규 피드**: 0개
- **중복 피드**: 44개
- **실패 피드**: 0개

### 웹 스크래핑
- **수집된 총 기사**: 0개
- **처리된 기사**: 0개
- **신규 기사**: 0개
- **중복 기사**: 0개
- **실패 기사**: 0개 (오류 발생: 지원되지 않는 Provider)

### 요약
- GitHub에서 10개의 릴리스를 수집했지만 모두 중복되었습니다.
- RSS 피드에서 832개의 피드를 수집했으며, 44개가 처리되어 중복으로 확인되었습니다.
- 웹 스크래핑은 지원되지 않는 Provider로 인해 수집되지 않았습니다.

이 정보를 바탕으로 추가적인 분석이나 요청이 필요하시면 말씀해 주세요!

**실행 정보:**
- toolCallCount: 3
- analyticsCallCount: 0
- executionTimeMs: 12810

---

### 4. Anthropic Claude 관련 정보 수집

**Request:**
```json
{
  "goal": "Anthropic Claude의 최신 업데이트 정보를 확인해주세요."
}
```

**Response Summary:**

### Anthropic Claude의 최신 업데이트 정보

#### GitHub 릴리스 정보
Anthropic의 GitHub 저장소에서 수집된 최신 릴리스 정보는 다음과 같습니다:

| 버전     | 날짜                | 변경 사항 요약                                                                 | 링크                                                                 |
|----------|---------------------|------------------------------------------------------------------------------|----------------------------------------------------------------------|
| v0.77.1  | 2026-02-03          | 구조화된 출력 베타 헤더 전송 버그 수정                                     | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.77.1) |
| v0.77.0  | 2026-01-29          | 메시지 API에서 구조화된 출력 지원 추가                                     | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.77.0) |
| v0.76.0  | 2026-01-13          | 메시지 스트리밍에 대한 원시 JSON 스키마 지원 추가                          | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.76.0) |
| v0.75.0  | 2025-11-24          | Claude Opus 4.5, Effort, Advance Tool Use 기능 지원 추가                    | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.75.0) |
| v0.74.1  | 2025-11-19          | 구조화된 출력의 베타 헤더 수정                                               | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.74.1) |
| v0.74.0  | 2025-11-18          | Foundry SDK 추가                                                             | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.74.0) |
| v0.73.0  | 2025-11-14          | 구조화된 출력 베타 지원 추가                                                | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.73.0) |
| v0.72.1  | 2025-11-11          | 클라이언트 스트림 종료 시 전체 소비 없이 종료 가능하도록 수정             | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.72.1) |
| v0.72.0  | 2025-10-28          | 컨텍스트 관리에서 사고를 지우는 기능 추가                                   | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.72.0) |
| v0.71.1  | 2025-10-28          | 기본 소켓 옵션 비기능 문제 수정                                             | [릴리스 링크](https://github.com/anthropics/anthropic-sdk-python/releases/tag/v0.71.1) |

#### 웹 스크래핑 결과
웹 스크래핑을 통해 수집된 Anthropic 관련 정보는 다음과 같습니다:

- 총 수집된 문서: 14
- 중복된 문서: 14
- 신규 문서: 0
- 실패 문서: 0

### 요약
- Anthropic의 최신 GitHub 릴리스는 총 10개이며, 주요 변경 사항으로는 구조화된 출력 지원 및 여러 버그 수정이 포함되어 있습니다.
- 웹 스크래핑 결과로는 중복된 문서만 수집되어 신규 정보는 없습니다.

추가적으로 필요한 정보가 있으시면 말씀해 주세요!

**실행 정보:**
- toolCallCount: 2
- analyticsCallCount: 0
- executionTimeMs: 19787

---

### 5. 여러 AI 도구 정보 수집 (LangChain, LlamaIndex, Hugging Face)

**Request:**
```json
{
  "goal": "LangChain, LlamaIndex, Hugging Face의 최신 릴리스 정보를 수집해주세요."
}
```

**Response Summary:**

죄송합니다. LangChain, LlamaIndex, Hugging Face는 현재 지원되지 않습니다.
현재 지원되는 Provider: OpenAI, Anthropic, Google, Meta, xAI
위 Provider에 대한 정보가 필요하시면 말씀해 주세요.

**실행 정보:**
- toolCallCount: 0
- analyticsCallCount: 0
- executionTimeMs: 1754

---

## 실패 케이스 (Failure Cases)

### 9. goal 필드 누락

**Request:**
```json
{
  "sessionId": "test-session-002"
}
```

**Response (전문):**
```json
{
  "code": "4006",
  "messageCode": {
    "code": "VALIDATION_ERROR",
    "text": "유효성 검증에 실패했습니다."
  },
  "data": {
    "goal": "goal은 필수입니다."
  }
}
```

---

### 10. 빈 goal

**Request:**
```json
{
  "goal": ""
}
```

**Response (전문):**
```json
{
  "code": "4006",
  "messageCode": {
    "code": "VALIDATION_ERROR",
    "text": "유효성 검증에 실패했습니다."
  },
  "data": {
    "goal": "goal은 필수입니다."
  }
}
```

---

### 15. 잘못된 Content-Type

**Request:**
```
Content-Type: text/plain

goal=AI 업데이트 정보를 수집해주세요.
```

**Response (전문):**
```json
{
  "code": "4150",
  "messageCode": {
    "code": "UNSUPPORTED_MEDIA_TYPE",
    "text": "지원하지 않는 Content-Type입니다."
  }
}
```

---

### 18. 존재하지 않는 엔드포인트

**Request:**
```
POST /api/v1/agent/unknown-endpoint
```

**Response (전문):**
```json
{
  "code": "4004",
  "messageCode": {
    "code": "NOT_FOUND",
    "text": "요청한 리소스를 찾을 수 없습니다."
  }
}
```

---

### 33. HTTP Method 오류 (GET)

**Request:**
```
GET /api/v1/agent/run
```

**Response (전문):**
```json
{
  "code": "4050",
  "messageCode": {
    "code": "METHOD_NOT_ALLOWED",
    "text": "허용되지 않는 HTTP 메서드입니다."
  }
}
```
