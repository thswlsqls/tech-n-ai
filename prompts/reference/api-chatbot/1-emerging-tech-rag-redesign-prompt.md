# api-chatbot 모듈 RAG 검색 대상 Emerging Tech 전용 개선 설계서 작성 프롬프트

## 역할 정의

당신은 MongoDB Atlas Vector Search와 LangChain4j 기반 RAG 시스템 전문 아키텍트입니다. 현재 `api-chatbot` 모듈의 RAG 파이프라인을 분석하고, **Emerging Tech 문서 전용 벡터 검색**으로 개선하기 위한 설계서를 작성하세요.

---

## 프로젝트 컨텍스트

### 기술 스택

| 항목 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1, Spring Cloud 2024.0.0 |
| LLM | OpenAI GPT-4o-mini (via LangChain4j 1.10.0) |
| Embedding | OpenAI text-embedding-3-small (1536 dimensions) |
| Vector DB | MongoDB Atlas Vector Search |
| Re-Ranking | Cohere rerank-multilingual-v3.0 (선택적) |
| Command Store | Aurora MySQL (MariaDB), JPA + QueryDSL |
| Query Store | MongoDB Atlas, Spring Data MongoDB |
| 동기화 | Kafka 기반 CQRS |

### 현재 구현 상태

#### RAG 파이프라인 흐름 (현재)

```
사용자 입력 → IntentClassificationService (의도 분류)
  ├─ AGENT_COMMAND → AgentDelegationService
  ├─ WEB_SEARCH_REQUIRED → WebSearchService
  ├─ RAG_REQUIRED → RAG Pipeline (아래)
  └─ LLM_DIRECT → LLMService 직접 호출

RAG Pipeline:
  InputInterpretationChain (입력 해석, 검색 컬렉션 결정)
  → VectorSearchService (벡터 검색 실행)
  → ResultRefinementChain (중복 제거, Re-Ranking)
  → AnswerGenerationChain (프롬프트 구성 + LLM 호출)
```

#### 현재 벡터 검색 대상

| 컬렉션 | 검색 구현 상태 | 필터 | 비고 |
|--------|--------------|------|------|
| `bookmarks` | 구현 완료 | `userId` pre-filter | VectorSearchUtil.createBookmarkSearchPipeline() |
| `emerging_techs` | **미구현** | - | SearchOptions에 includeEmergingTechs 플래그만 존재 |

#### 핵심 문제점

1. `VectorSearchServiceImpl.search()`에서 **bookmarks 컬렉션만** 벡터 검색 수행
2. `emerging_techs` 컬렉션 벡터 검색 로직이 **미구현** (SearchOptions에 플래그만 존재)
3. `InputInterpretationChain`에서 키워드 기반으로 컬렉션을 결정하나, 실제 검색은 bookmarks만 수행
4. `IntentClassificationService`의 RAG_KEYWORDS에 emerging tech 관련 키워드 부족

---

## 분석 대상 파일

설계서 작성 전 **반드시** 다음 파일들을 분석하세요:

### api-chatbot 모듈

| 파일 | 역할 |
|------|------|
| `api/chatbot/src/main/java/.../service/ChatbotServiceImpl.java` | RAG 파이프라인 오케스트레이션 |
| `api/chatbot/src/main/java/.../service/VectorSearchServiceImpl.java` | 벡터 검색 실행 (현재 bookmarks만) |
| `api/chatbot/src/main/java/.../service/VectorSearchService.java` | 벡터 검색 인터페이스 |
| `api/chatbot/src/main/java/.../service/IntentClassificationServiceImpl.java` | 의도 분류 (키워드 기반) |
| `api/chatbot/src/main/java/.../chain/InputInterpretationChain.java` | 입력 해석 및 검색 컨텍스트 결정 |
| `api/chatbot/src/main/java/.../chain/AnswerGenerationChain.java` | 답변 생성 |
| `api/chatbot/src/main/java/.../chain/ResultRefinementChain.java` | 결과 정제 |
| `api/chatbot/src/main/java/.../service/PromptServiceImpl.java` | RAG 프롬프트 구성 |
| `api/chatbot/src/main/java/.../service/dto/SearchOptions.java` | 검색 옵션 DTO |
| `api/chatbot/src/main/java/.../service/dto/SearchContext.java` | 검색 컨텍스트 DTO |
| `api/chatbot/src/main/java/.../service/dto/SearchResult.java` | 검색 결과 DTO |
| `api/chatbot/src/main/java/.../config/LangChain4jConfig.java` | LangChain4j 설정 |
| `api/chatbot/src/main/resources/application-chatbot-api.yml` | chatbot 설정 |

### domain-mongodb 모듈

| 파일 | 역할 |
|------|------|
| `domain/mongodb/src/main/java/.../document/EmergingTechDocument.java` | Emerging Tech Document 스키마 |
| `domain/mongodb/src/main/java/.../util/VectorSearchUtil.java` | 벡터 검색 파이프라인 유틸리티 |
| `domain/mongodb/src/main/java/.../util/VectorSearchOptions.java` | 벡터 검색 옵션 |
| `domain/mongodb/src/main/java/.../config/VectorSearchIndexConfig.java` | 벡터 인덱스 설정 |
| `domain/mongodb/src/main/java/.../repository/EmergingTechRepository.java` | Emerging Tech 리포지토리 |

### 참고 API 스펙

| 파일 | 역할 |
|------|------|
| `prompts/reference/API-SPECIFICATIONS/api-chatbot-specification.md` | Chatbot API 스펙 |
| `prompts/reference/API-SPECIFICATIONS/api-emerging-tech-specification.md` | Emerging Tech API 스펙 |

---

## 설계 요구사항

### 1. RAG 검색 대상을 Emerging Tech 전용으로 변경

**목표**: 최신 기술 동향 관련 채팅 요청은 모두 `emerging_techs` 컬렉션을 벡터 검색하도록 개선.

#### 1.1 VectorSearchService 개선

**현재 상태**:
```java
// VectorSearchServiceImpl.java - bookmarks만 검색
public List<SearchResult> search(String query, Long userId, SearchOptions options) {
    Embedding embedding = embeddingModel.embed(query).content();
    List<Float> queryVector = embedding.vectorAsList();
    List<SearchResult> results = new ArrayList<>();

    if (Boolean.TRUE.equals(options.includeBookmarks()) && userId != null) {
        results.addAll(searchBookmarks(queryVector, userId.toString(), options));
    }
    // emerging_techs 검색 로직 없음!
    return results.stream()
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .limit(options.maxResults())
        .collect(Collectors.toList());
}
```

**개선 방향**:
- `emerging_techs` 컬렉션 벡터 검색 메서드 추가
- `status: "PUBLISHED"` pre-filter 적용 (게시된 문서만 검색)
- `VectorSearchUtil`에 Emerging Tech 전용 파이프라인 생성 메서드 추가
- `VectorSearchIndexConfig`에 정의된 `vector_index_emerging_techs` 인덱스 사용

**Emerging Tech Vector Search Index 구조** (이미 정의됨):
```json
{
  "fields": [
    { "type": "vector", "path": "embedding_vector", "numDimensions": 1536, "similarity": "cosine" },
    { "type": "filter", "path": "provider" },
    { "type": "filter", "path": "status" }
  ]
}
```

#### 1.2 VectorSearchUtil 확장

- `createEmergingTechSearchPipeline()` 메서드 추가
- `status: "PUBLISHED"` pre-filter 기본 적용
- 선택적 `provider` filter 지원

#### 1.3 SearchOptions / SearchContext 단순화

- Emerging Tech 전용이므로 `includeBookmarks` / `includeEmergingTechs` 플래그 제거 검토
- 또는 기본값을 `includeEmergingTechs = true`, `includeBookmarks = false`로 변경

#### 1.4 IntentClassificationService 개선

- RAG_KEYWORDS에 emerging tech 관련 키워드 추가:
  - AI, 인공지능, LLM, GPT, Claude, Gemini, 모델, API, SDK
  - OpenAI, Anthropic, Google, Meta, xAI 등 provider 키워드
  - 기술 동향, 트렌드, 업데이트, 릴리즈, 출시 등

#### 1.5 InputInterpretationChain 개선

- 기본 검색 대상을 `emerging_techs`로 변경
- 키워드 매칭 없이도 RAG_REQUIRED 시 `emerging_techs` 검색

#### 1.6 PromptServiceImpl 개선

- RAG 프롬프트 템플릿을 Emerging Tech 컨텍스트에 맞게 개선
- 검색 결과에 provider, title, publishedAt 등 메타데이터 포함

### 2. 멀티턴 채팅 + RAG 통합 검증

다음 항목이 업계 표준의 베스트 프랙티스를 준수하는지 검증하세요:

#### 2.1 대화 컨텍스트 압축

**LangChain4j 공식 권장사항** (출처: https://docs.langchain4j.dev/tutorials/rag/):
- `CompressingQueryTransformer`: 멀티턴 대화에서 대명사 해소(pronoun resolution) 수행
  - 예: "그것에 대해 더 알려줘" → "OpenAI GPT-4o 릴리즈에 대해 더 알려줘"
- 현재 구현에서 `InputInterpretationChain`이 이 역할을 하는지 검증

#### 2.2 ChatMemory 관리

**LangChain4j 공식 권장사항** (출처: https://docs.langchain4j.dev/tutorials/chat-memory/):
- `TokenWindowChatMemory` 사용 권장 (프로덕션 환경)
- `storeRetrievedContentInChatMemory = false` 설정으로 메모리 최적화
- 현재 구현의 ChatMemory 전략이 이 권장사항을 따르는지 검증

#### 2.3 검색 결과 품질

**MongoDB Atlas 공식 권장사항** (출처: https://www.mongodb.com/docs/atlas/atlas-vector-search/tune-vector-search/):
- `numCandidates` : `limit` 비율 >= 20배
- 현재 설정: numCandidates=100, limit=5 → 비율 20배 (적합)
- ANN 검색에서 90-95% recall 목표

**OpenAI 공식 권장사항** (출처: https://platform.openai.com/docs/guides/embeddings):
- text-embedding-3-small에 cosine similarity 사용 (벡터가 정규화되어 있으므로)
- 현재 VectorSearchIndexConfig에서 `"similarity": "cosine"` 설정 (적합)

### 3. 변경 범위 결정

설계서에서 다음 변경 범위를 명확히 정의하세요:

#### 수정 대상 파일

| 모듈 | 파일 | 변경 내용 |
|------|------|----------|
| api-chatbot | `VectorSearchServiceImpl.java` | emerging_techs 벡터 검색 메서드 추가 |
| api-chatbot | `IntentClassificationServiceImpl.java` | RAG 키워드 확장 |
| api-chatbot | `InputInterpretationChain.java` | 기본 검색 대상 변경 |
| api-chatbot | `PromptServiceImpl.java` | RAG 프롬프트 개선 |
| api-chatbot | `SearchOptions.java` | Emerging Tech 전용 옵션 |
| domain-mongodb | `VectorSearchUtil.java` | Emerging Tech 파이프라인 메서드 추가 |

#### 변경하지 않는 파일

- `ChatbotServiceImpl.java`: 파이프라인 오케스트레이션 구조 유지
- `LangChain4jConfig.java`: LLM/Embedding 설정 유지
- `AnswerGenerationChain.java`: 답변 생성 구조 유지
- `ResultRefinementChain.java`: 결과 정제 구조 유지
- 세션/메시지 관련 Service: 변경 불필요

---

## 설계 원칙

### 필수 준수

1. **SOLID 원칙**
   - SRP: VectorSearchService는 벡터 검색 책임만 담당
   - OCP: 새로운 컬렉션 검색 추가 시 기존 코드 수정 최소화
   - DIP: 인터페이스 기반 의존성 주입 유지

2. **클린코드 원칙**
   - 의미 있는 명명 (searchEmergingTechs, createEmergingTechSearchPipeline)
   - 단일 책임 메서드 (검색, 변환, 필터링 분리)
   - DRY: VectorSearchUtil의 기존 메서드 최대한 재사용

3. **기존 패턴 준수**
   - Controller → Facade → Service → Repository 계층 유지
   - VectorSearchUtil의 정적 팩토리 메서드 패턴 유지
   - SearchResult, SearchOptions 등 기존 DTO 구조 활용

### 금지 사항

1. **오버엔지니어링 금지**
   - 불필요한 추상화 레이어 생성 금지
   - Strategy/Factory 등 과도한 패턴 적용 금지
   - 현재 필요하지 않은 기능 설계 금지

2. **불필요한 리팩토링 금지**
   - 변경 대상이 아닌 코드 수정 금지
   - 기존 작동하는 로직 변경 금지

3. **비공식 자료 참고 금지**
   - 블로그, Stack Overflow, 비공식 튜토리얼 참고 금지
   - 공식 문서에 근거하지 않는 추천 금지

---

## 공식 참고 자료

설계서 작성 시 **반드시** 다음 공식 문서만 참고하세요:

| 주제 | 공식 문서 URL |
|------|-------------|
| MongoDB Atlas Vector Search | https://www.mongodb.com/docs/atlas/atlas-vector-search/ |
| $vectorSearch Stage | https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/ |
| Vector Search Index 설정 | https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-type/ |
| Vector Search 성능 튜닝 | https://www.mongodb.com/docs/atlas/atlas-vector-search/tune-vector-search/ |
| MongoDB RAG 가이드 | https://www.mongodb.com/docs/atlas/atlas-vector-search/rag/ |
| LangChain4j RAG | https://docs.langchain4j.dev/tutorials/rag/ |
| LangChain4j MongoDB Atlas | https://docs.langchain4j.dev/integrations/embedding-stores/mongodb-atlas/ |
| LangChain4j Chat Memory | https://docs.langchain4j.dev/tutorials/chat-memory/ |
| OpenAI Embeddings | https://platform.openai.com/docs/guides/embeddings |
| Spring Data MongoDB | https://docs.spring.io/spring-data/mongodb/reference/ |

---

## 출력 형식

### 설계서 구조

```markdown
# api-chatbot Emerging Tech 전용 RAG 검색 개선 설계서

## 1. 개요
   - 목적
   - 범위
   - 현재 구현 상태 요약

## 2. 현재 구현 분석
   - RAG 파이프라인 분석
   - VectorSearchService 분석
   - IntentClassificationService 분석
   - InputInterpretationChain 분석
   - 베스트 프랙티스 준수 여부 검증

## 3. 개선 설계
   ### 3.1 VectorSearchUtil - Emerging Tech 파이프라인
   - createEmergingTechSearchPipeline() 설계
   - Pre-filter 전략 (status, provider)
   - 인터페이스 / 메서드 시그니처 정의

   ### 3.2 VectorSearchServiceImpl - 검색 로직 추가
   - searchEmergingTechs() 메서드 설계
   - 기존 search() 메서드 수정 범위
   - SearchResult 변환 로직

   ### 3.3 IntentClassificationService - 키워드 확장
   - RAG_KEYWORDS 확장 목록
   - Emerging Tech 특화 키워드

   ### 3.4 InputInterpretationChain - 기본 검색 대상 변경
   - 기본 컬렉션 변경 (bookmarks → emerging_techs)
   - 키워드 매칭 로직 개선

   ### 3.5 PromptServiceImpl - 프롬프트 개선
   - Emerging Tech 특화 프롬프트 템플릿
   - 메타데이터 활용 (provider, title, publishedAt)

   ### 3.6 SearchOptions 단순화
   - Emerging Tech 전용 옵션

## 4. 멀티턴 RAG 베스트 프랙티스 검증
   - CompressingQueryTransformer 적용 검토
   - ChatMemory 전략 검증
   - 검색 결과 품질 검증

## 5. 수정 파일 목록 및 변경 요약

## 6. 구현 체크리스트

## 7. 참고 자료 (공식 문서 링크)
```

---

## 검증 체크리스트

설계서 완성 후 다음 항목을 확인하세요:

- [ ] `VectorSearchServiceImpl`에 `emerging_techs` 벡터 검색 로직이 설계됨
- [ ] `VectorSearchUtil`에 `createEmergingTechSearchPipeline()` 메서드가 설계됨
- [ ] `status: "PUBLISHED"` pre-filter가 적용됨
- [ ] `vector_index_emerging_techs` 인덱스가 사용됨
- [ ] `IntentClassificationService`의 RAG_KEYWORDS에 emerging tech 키워드가 추가됨
- [ ] `InputInterpretationChain`의 기본 검색 대상이 `emerging_techs`로 변경됨
- [ ] `PromptServiceImpl`에서 Emerging Tech 메타데이터가 프롬프트에 포함됨
- [ ] `numCandidates:limit` 비율이 20배 이상 유지됨 (MongoDB 공식 권장)
- [ ] cosine similarity 사용 확인 (OpenAI 임베딩 공식 권장)
- [ ] 멀티턴 대화에서 쿼리 압축 전략이 검토됨 (LangChain4j 공식 권장)
- [ ] `TokenWindowChatMemory` 사용이 검증됨 (LangChain4j 공식 권장)
- [ ] SOLID 원칙이 준수됨
- [ ] 오버엔지니어링 없이 최소 변경으로 설계됨
- [ ] 모든 참고 자료가 공식 문서임

---

## 작업 순서

### Step 1: 현재 코드 분석
1. 위 "분석 대상 파일" 목록의 모든 파일을 분석
2. 현재 VectorSearchService의 bookmarks 검색 로직 이해
3. VectorSearchUtil의 파이프라인 생성 패턴 이해
4. EmergingTechDocument의 스키마 및 embeddingVector 필드 확인

### Step 2: 베스트 프랙티스 검증
1. 현재 구현이 MongoDB Atlas Vector Search 공식 권장사항 준수 여부 확인
2. 현재 구현이 LangChain4j RAG 공식 권장사항 준수 여부 확인
3. 개선이 필요한 항목 식별

### Step 3: 개선 설계
1. VectorSearchUtil - Emerging Tech 파이프라인 설계
2. VectorSearchServiceImpl - 검색 로직 설계
3. IntentClassificationService - 키워드 확장 설계
4. InputInterpretationChain - 기본 검색 대상 변경 설계
5. PromptServiceImpl - 프롬프트 개선 설계

### Step 4: 설계서 작성
1. 위 "출력 형식"에 따라 설계서 작성
2. 각 설계 항목에 인터페이스/메서드 시그니처 포함
3. 변경 파일 목록 명확히 정의
4. 검증 체크리스트 포함

### Step 5: 최종 검증
- 검증 체크리스트 모든 항목 확인
- 오버엔지니어링 여부 재확인
- 공식 문서 기반 내용만 포함되었는지 확인
