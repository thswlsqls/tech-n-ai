# RAG 기반 멀티턴 채팅 기능 구현 코드 분석 프롬프트

**대상 모듈**: `api/chatbot`
**참고 설계서**: `docs/step12/rag-chatbot-design.md`
**분석 목적**: 현재 구현 코드와 설계서 대비 개선점 도출

---

## 분석 지시사항

다음 5개 항목에 대해 현재 구현 코드를 분석하고, 설계서 대비 누락/오류/개선 가능한 부분을 식별하시오.

### 분석 원칙

1. **공식 문서 기반 검증**: 외부 자료 참조 시 반드시 공식 문서만 사용
   - MongoDB Atlas Vector Search: https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/
   - langchain4j: https://docs.langchain4j.dev/
   - OpenAI Embeddings: https://platform.openai.com/docs/guides/embeddings

2. **실용적 개선안 제시**: 오버엔지니어링 없이 실제 운영 환경에서 유효한 개선점만 제시

3. **근거 명시**: 모든 개선 제안에 대해 구체적인 이유와 기대 효과 명시

---

## 분석 항목

### 1. 사용자 입력 전처리 로직 분석

**대상 파일**:
- `api/chatbot/src/main/java/.../service/InputPreprocessingServiceImpl.java`
- `api/chatbot/src/main/java/.../service/dto/PreprocessedInput.java`

**분석 관점**:
- 입력 검증(null, 빈값, 길이)의 완전성
- 정규화 로직(공백 처리, 특수문자 필터링)의 적절성
- Prompt Injection 방지 처리 여부
- 다국어(한국어/영어) 입력 처리 고려 여부

**검증 기준**:
```
설계서 기준값:
- max-length: 500
- min-length: 1
- 제어 문자(\\x00-\\x1F\\x7F) 제거
- 연속 공백 단일 공백 변환
```

---

### 2. 인텐트 구분 로직 분석

**대상 파일**:
- `api/chatbot/src/main/java/.../service/IntentClassificationServiceImpl.java`
- `api/chatbot/src/main/java/.../service/dto/Intent.java`

**분석 관점**:
- 키워드 기반 분류의 정확도 및 커버리지
- GREETING_KEYWORDS, RAG_KEYWORDS 세트의 완전성
- 의문사/물음표 패턴 매칭의 적절성
- 인사말과 RAG 키워드 동시 포함 시 우선순위 처리
- 확장 가능성 (새로운 Intent 추가 용이성)

**검증 기준**:
```
설계서 기준:
- Intent.RAG_REQUIRED: 검색 필요
- Intent.GENERAL_CONVERSATION: 일반 대화
- 인사말 우선 체크 후 RAG 키워드 체크
```

**테스트 케이스 제안**:
```
입력: "안녕하세요 대회 정보 알려줘" → 예상: RAG_REQUIRED? GENERAL?
입력: "오늘 날씨 어때?" → 예상: GENERAL? RAG_REQUIRED?
입력: "최근 Kaggle 대회" → 예상: RAG_REQUIRED
```

---

### 3. MongoDB Atlas Vector Search 구현 분석

**대상 파일**:
- `api/chatbot/src/main/java/.../service/VectorSearchServiceImpl.java`
- `domain/mongodb/src/main/java/.../util/VectorSearchUtil.java`
- `domain/mongodb/src/main/java/.../util/VectorSearchOptions.java`

**분석 관점**:

#### 3.1 임베딩 생성
- OpenAI text-embedding-3-small 모델 사용 여부 확인
- 임베딩 차원(1536) 설정 정확성
- document/query 구분 없이 동일 모델 사용 (OpenAI 특성)

#### 3.2 벡터 검색 파이프라인
- `$vectorSearch` aggregation stage 구성의 정확성
- `numCandidates` 파라미터 설정 (limit의 10~20배 권장)
- `$meta: "vectorSearchScore"` 활용한 유사도 점수 추출
- pre-filter (archives의 userId 필터) 구현 정확성

#### 3.3 최적화
- 검색 결과 수 제한 (기본값 5)
- 유사도 임계값 필터링 (기본값 0.7)
- 각 컬렉션별 독립 검색 후 통합 정렬

**검증 기준** (MongoDB 공식 문서 기반):
```javascript
// $vectorSearch stage 필수 파라미터
{
  $vectorSearch: {
    index: "vector_index_name",  // 필수
    path: "embedding_vector",     // 필수
    queryVector: [...],           // 필수
    numCandidates: 100,           // ANN에서 필수
    limit: 5,                     // 필수
    filter: { ... }               // 선택 (pre-filter)
  }
}
```

---

### 4. 토큰 사용량 제어 로직 분석

**대상 파일**:
- `api/chatbot/src/main/java/.../service/TokenServiceImpl.java`
- `api/chatbot/src/main/java/.../service/dto/TokenUsage.java`

**분석 관점**:

#### 4.1 토큰 예측 로직
```java
// 현재 구현
int estimatedTokens = (int) (koreanCharCount * 2 + (wordCount - koreanCharCount) * 1.3);
return Math.max(estimatedTokens, text.length() / 4);
```
- 한국어/영어 혼합 텍스트 토큰 예측 정확도
- langchain4j의 `OpenAiTokenCountEstimator` 사용 여부 (설계서에 Bean 정의됨)
- 예측값과 실제 LLM 응답의 토큰 수 비교 필요성

#### 4.2 토큰 제한
- max-input-tokens: 4000 설정의 적절성 (GPT-4o-mini 컨텍스트 128K 대비)
- max-output-tokens: 2000 설정의 적절성
- warning-threshold: 0.8 (80%) 경고 임계값

#### 4.3 검색 결과 truncate 로직
- 토큰 초과 시 검색 결과 잘라내기 동작 확인
- 유사도 높은 결과가 우선 포함되는지 확인

---

### 5. 프롬프트 체인 구현 분석

**대상 파일**:
- `api/chatbot/src/main/java/.../chain/InputInterpretationChain.java`
- `api/chatbot/src/main/java/.../chain/ResultRefinementChain.java`
- `api/chatbot/src/main/java/.../chain/AnswerGenerationChain.java`
- `api/chatbot/src/main/java/.../service/PromptServiceImpl.java`
- `api/chatbot/src/main/java/.../service/ChatbotServiceImpl.java`

**분석 관점**:

#### 5.1 InputInterpretationChain
```java
// 현재 구현
private static final String NOISE_PATTERN = "(알려줘|찾아줘|검색해줘|보여줘|...)";
```
- 노이즈 패턴 제거의 완전성
- 검색 쿼리 추출 로직 (`extractSearchQuery`가 단순 반환)
- 컬렉션 타겟팅 로직 (키워드 기반)

#### 5.2 ResultRefinementChain
- 유사도 필터링 중복 여부 (VectorSearchService에서 이미 필터링)
- 중복 제거 로직의 정확성 (documentId 기준)
- SearchResult → RefinedResult 변환 필요성

#### 5.3 AnswerGenerationChain
- RefinedResult → SearchResult 역변환 존재 (불필요한 변환?)
- 프롬프트 템플릿 구조의 적절성
- 후처리 로직 (불필요한 접두사 제거)

#### 5.4 체인 통합 (ChatbotServiceImpl)
- RAG 파이프라인과 일반 대화 분기 처리
- 멀티턴 대화 히스토리 저장/로드 로직
- ChatMemory 통합 구현 상태

**검증 기준**:
```
설계서 체인 흐름:
User Input → InputInterpretationChain → Vector Search
→ ResultRefinementChain → AnswerGenerationChain → Response
```

---

## 출력 형식

분석 결과를 다음 형식의 Markdown 파일로 정리:

```markdown
# RAG 기반 멀티턴 채팅 기능 구현 분석 리포트

## 1. 개요
- 분석 일시
- 분석 대상 파일 목록
- 참고 설계서

## 2. 분석 결과 요약

| 항목 | 현재 상태 | 개선 필요 여부 | 우선순위 |
|------|----------|--------------|---------|
| 입력 전처리 | | | |
| 인텐트 분류 | | | |
| Vector Search | | | |
| 토큰 제어 | | | |
| 프롬프트 체인 | | | |

## 3. 상세 분석

### 3.1 사용자 입력 전처리
#### 현재 구현
#### 개선점
#### 권장 조치

### 3.2 인텐트 구분 로직
...

## 4. 종합 권장사항

### 4.1 즉시 개선 필요 (Critical)
### 4.2 개선 권장 (Recommended)
### 4.3 향후 고려 (Optional)

## 5. 참고 자료
- 공식 문서 링크
```

---

## 분석 시 주의사항

1. **오버엔지니어링 금지**: 현재 요구사항 범위 내에서만 개선점 제시
2. **장황한 설명 지양**: 핵심만 간결하게 기술
3. **실행 가능한 제안**: 구체적인 코드 수정 방향 제시
4. **설계서 준수 확인**: 설계서와 구현의 일치 여부 우선 검토
