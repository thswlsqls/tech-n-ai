# RAG 기반 멀티턴 채팅 기능 개선 상세 구현 설계서 작성 프롬프트

**대상 모듈**: `api/chatbot`
**참고 분석 리포트**: `docs/step12/rag-chatbot-analysis-report.md`
**참고 설계서**: `docs/step12/rag-chatbot-design.md`
**설계서 출력 경로**: `docs/step12/rag-chatbot-improvement-design.md`

---

## 설계서 작성 목적

분석 리포트에서 도출된 개선사항들을 실제 구현 가능한 수준으로 상세 설계한다.

---

## 개선 대상 항목

### 1. 즉시 개선 필요 (Critical)

#### 1.1 인텐트 분류 우선순위 수정

**현재 문제**:
- 인사말이 RAG 키워드보다 우선 처리되어 "안녕하세요 대회 정보 알려줘"가 `GENERAL_CONVERSATION`으로 분류됨

**설계 요구사항**:
- `IntentClassificationServiceImpl.java`의 분류 우선순위 변경
- RAG 키워드 → 질문 형태 → 인사말 순으로 체크
- 변경 전/후 동작 비교 테이블 포함

#### 1.2 TokenCountEstimator Bean 활성화

**현재 문제**:
- `LangChain4jConfig.java`에서 `TokenCountEstimator` Bean이 주석 처리됨
- 휴리스틱 기반 토큰 예측으로 인한 부정확한 추정

**설계 요구사항**:
- `OpenAiTokenCountEstimator` Bean 활성화 코드
- `TokenServiceImpl`에서 `TokenCountEstimator` 의존성 주입 및 사용
- 기존 휴리스틱 로직과의 비교 (fallback 전략 포함)

---

### 2. 개선 권장 (Recommended)

#### 2.1 Prompt Injection 방지 로직 추가

**현재 문제**:
- 악의적인 프롬프트 주입 패턴 필터링 없음

**설계 요구사항**:
- `InputPreprocessingServiceImpl.java`에 추가할 필터링 로직
- 탐지 패턴 정의 (예: `ignore previous instructions`, `<|system|>`, `[INST]`)
- 탐지 시 처리 정책 (로깅, 필터링, 거부 중 선택)
- 패턴은 설정 파일로 외부화 여부 결정

#### 2.2 히스토리 중복 로드 최적화

**현재 문제**:
- `ChatbotServiceImpl.saveMessagesToMemory()`에서 매 요청마다 전체 히스토리 재로드

**설계 요구사항**:
- 히스토리 로드 로직 최적화 방안
- DB 저장과 ChatMemory 관리 분리 전략
- 메모리 중복 방지 로직

#### 2.3 DTO 변환 단순화

**현재 문제**:
- `RefinedResult`와 `SearchResult` 간 불필요한 변환 존재
- `AnswerGenerationChain`에서 `RefinedResult` → `SearchResult` 역변환

**설계 요구사항**:
- DTO 구조 단순화 방안 (통합 또는 인터페이스 추출)
- `PromptService.buildPrompt()` 시그니처 변경 여부
- 영향받는 클래스 목록 및 수정 범위

---

### 3. 향후 고려 (Optional)

#### 3.1 의문사 패턴 확장

**현재 문제**:
- 구어체 의문사("뭐", "몇", "얼마") 미포함

**설계 요구사항**:
- 확장할 의문사 패턴 목록
- 정규식 패턴 수정 내용

#### 3.2 키워드 외부화

**현재 문제**:
- `GREETING_KEYWORDS`, `RAG_KEYWORDS`가 하드코딩

**설계 요구사항**:
- `application.yml`로 키워드 외부화 방안
- `@ConfigurationProperties` 활용 설계

#### 3.3 실제 토큰 수 수집

**현재 문제**:
- `LLMServiceImpl.generate()`가 String만 반환하여 실제 토큰 사용량 미수집

**설계 요구사항**:
- `LLMResponse` record 설계 (content, inputTokens, outputTokens)
- langchain4j `Response<AiMessage>` 활용 방안
- 토큰 사용량 로깅 및 집계 전략

---

## 설계서 출력 형식

```markdown
# RAG 기반 멀티턴 채팅 기능 개선 상세 구현 설계서

**작성 일시**: YYYY-MM-DD
**참고 분석 리포트**: `docs/step12/rag-chatbot-analysis-report.md`

---

## 1. 개요

### 1.1 목적
### 1.2 개선 범위
### 1.3 우선순위별 분류

---

## 2. 즉시 개선 항목 (Critical)

### 2.1 인텐트 분류 우선순위 수정

#### 2.1.1 현재 구현
(코드 스니펫)

#### 2.1.2 문제점 분석
(구체적 시나리오)

#### 2.1.3 개선 설계
(수정 코드, 변경 전/후 동작 비교)

#### 2.1.4 테스트 케이스
(입력 → 기대 결과)

---

### 2.2 TokenCountEstimator 활성화

#### 2.2.1 현재 구현
(주석 처리된 코드)

#### 2.2.2 개선 설계

**LangChain4jConfig.java 수정**:
(Bean 정의 코드)

**TokenServiceImpl.java 수정**:
(의존성 주입 및 사용 코드)

#### 2.2.3 Fallback 전략
(TokenCountEstimator 실패 시 기존 휴리스틱 사용 여부)

---

## 3. 개선 권장 항목 (Recommended)

### 3.1 Prompt Injection 방지

#### 3.1.1 탐지 패턴 정의
(Pattern 정규식)

#### 3.1.2 처리 정책
(탐지 시 동작: 로깅/필터링/거부)

#### 3.1.3 구현 코드
(InputPreprocessingServiceImpl 수정 내용)

---

### 3.2 히스토리 로드 최적화

#### 3.2.1 현재 구현의 문제
(코드 분석)

#### 3.2.2 개선 설계
(최적화된 로직)

---

### 3.3 DTO 변환 단순화

#### 3.3.1 현재 DTO 구조
(클래스 다이어그램 또는 필드 비교)

#### 3.3.2 개선 방안
(통합/인터페이스 추출 중 선택)

#### 3.3.3 영향 범위
(수정 필요 클래스 목록)

---

## 4. 향후 고려 항목 (Optional)

### 4.1 의문사 패턴 확장
### 4.2 키워드 외부화
### 4.3 실제 토큰 수 수집

---

## 5. 구현 순서

| 순서 | 항목 | 예상 영향 범위 | 비고 |
|------|------|---------------|------|
| 1 | 인텐트 분류 수정 | 1개 파일 | Critical |
| 2 | TokenCountEstimator 활성화 | 2개 파일 | Critical |
| 3 | Prompt Injection 방지 | 1개 파일 | Recommended |
| ... | ... | ... | ... |

---

## 6. 참고 자료

- langchain4j 공식 문서: https://docs.langchain4j.dev/
- MongoDB Atlas Vector Search: https://www.mongodb.com/docs/atlas/atlas-vector-search/
- OpenAI Tokenizer: https://platform.openai.com/tokenizer
```

---

## 설계서 작성 지침

### 필수 준수 사항

1. **분석 리포트 기반**: `docs/step12/rag-chatbot-analysis-report.md`의 모든 개선사항 반영
2. **공식 문서만 참고**: langchain4j, MongoDB Atlas, OpenAI 공식 문서만 사용
3. **구현 가능한 수준**: 실제 코드 수정이 가능한 상세 설계
4. **기존 패턴 준수**: 현재 프로젝트의 코드 스타일 및 패턴 유지
5. **영향 범위 명시**: 각 개선 항목별 수정 파일 목록 명시

### 금지 사항

1. **오버엔지니어링 금지**: 분석 리포트에 없는 추가 개선사항 제안 금지
2. **비공식 자료 참고 금지**: 블로그, Stack Overflow 등 비공식 자료 사용 금지
3. **불필요한 추상화 금지**: 단순 문제에 복잡한 패턴 적용 금지
4. **범위 확대 금지**: 분석 리포트의 개선 범위를 넘어서는 설계 금지

---

## 시작 지시

위의 모든 요구사항과 지침을 준수하여 **RAG 기반 멀티턴 채팅 기능 개선 상세 구현 설계서**를 작성하세요.

설계서는 `docs/step12/rag-chatbot-improvement-design.md` 경로에 저장될 예정입니다.
