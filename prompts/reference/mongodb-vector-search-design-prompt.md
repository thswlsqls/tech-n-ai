# MongoDB Atlas Vector Search 활용 설계서 작성 프롬프트

## 역할 정의

당신은 MongoDB Atlas와 Spring Boot 기반 멀티모듈 프로젝트의 Vector Search 아키텍트입니다. 현재 프로젝트의 구현 상태를 분석하고, MongoDB Atlas Vector Search를 실제로 활용하기 위한 설계서를 작성하세요.

---

## 프로젝트 컨텍스트

### 현재 구현 상태

1. **Document 클래스에 Vector Search 필드 정의됨**
   - `NewsArticleDocument`, `ContestDocument`, `ArchiveDocument`
   - 필드: `embeddingText` (임베딩 대상 텍스트), `embeddingVector` (1536차원 벡터)

2. **Vector Search 유틸리티 구현됨**
   - `VectorSearchUtil`: $vectorSearch aggregation pipeline 생성 유틸리티
   - `VectorSearchOptions`: 검색 옵션 정의
   - `VectorSearchIndexConfig`: Index 정의 및 생성 가이드

3. **API 저장 로직에서 Vector 필드 미사용**
   - `NewsServiceImpl.saveNews()`: embeddingText, embeddingVector 설정하지 않음
   - `ContestServiceImpl.saveContest()`: embeddingText, embeddingVector 설정하지 않음

### 핵심 질문

> **Document에 Vector Search용 필드가 정의되어 있으나, 저장하는 로직이 없는 이유는 무엇인가?**

---

## 설계서 요구사항

### 1. 현재 구현 분석

다음 파일들을 분석하여 현재 구현 상태를 파악하세요:

**API Server 모듈**:
- `api/news/src/main/java/com/ebson/shrimp/tm/demo/api/news/service/NewsServiceImpl.java`
- `api/contest/src/main/java/com/ebson/shrimp/tm/demo/api/contest/service/ContestServiceImpl.java`
- `api/` 아래의 모든 API Server 모듈에서 MongoDB Atlas 연동 구현

**Domain MongoDB 모듈**:
- `domain/mongodb/` 전체 구현
- Document 클래스들의 embeddingText, embeddingVector 필드
- VectorSearchUtil, VectorSearchOptions, VectorSearchIndexConfig

**기존 설계서**:
- `docs/step1/2. mongodb-schema-design.md`
- `docs/step1/6. mongodb-atlas-integration-guide.md`
- `docs/` 아래 관련 설계서

### 2. Vector 필드 미저장 이유 분석

다음 관점에서 분석하세요:

1. **아키텍처 관점**: 관심사 분리(SoC) 원칙
2. **성능 관점**: 동기 vs 비동기 처리
3. **비용 관점**: OpenAI API 호출 비용 최적화
4. **운영 관점**: 배치 처리 vs 실시간 처리
5. **데이터 품질 관점**: 임베딩 텍스트 생성 규칙

### 3. 실질적인 Vector Search 활용 가이드

다음 내용을 포함하세요:

#### 3.1 임베딩 생성 전략

```
[ 데이터 수집/저장 ] ──(동기)──> [ MongoDB 저장 ]
                                      │
                                      ▼ (비동기 이벤트)
                              [ 임베딩 생성 서비스 ]
                                      │
                                      ▼
                              [ MongoDB 업데이트 ]
                              (embeddingText, embeddingVector)
```

#### 3.2 구현 방안

**Option A**: 이벤트 기반 비동기 처리
- Kafka/Redis 이벤트 발행
- 임베딩 생성 Consumer 구현

**Option B**: Spring Batch 기반 배치 처리
- 스케줄 기반 임베딩 생성
- embeddingVector가 null인 도큐먼트 대상

**Option C**: API 계층에서 선택적 동기 처리
- 특정 조건(예: 사용자 직접 생성)에서만 동기 처리
- LLM API Rate Limit 고려

#### 3.3 임베딩 텍스트 생성 규칙

컬렉션별 embeddingText 생성 규칙:

| 컬렉션 | embeddingText 생성 규칙 | 예시 |
|--------|------------------------|------|
| contests | title + description + tags | "Codeforces Round 900..." |
| news_articles | title + summary + content(2000자) | "Spring Boot 4.0..." |
| archives | itemTitle + itemSummary + tag + memo | "Codeforces Round 900..." |

### 4. 설계 원칙 준수

다음 원칙을 반드시 준수하세요:

1. **클린코드 원칙**: 의미 있는 명명, 단일 책임, 가독성
2. **객체지향 설계**: 캡슐화, 추상화, 의존성 역전
3. **SOLID 원칙**: 특히 SRP, OCP, DIP
4. **오버엔지니어링 금지**: 실제 필요한 수준의 복잡도만 유지
5. **장황한 주석 금지**: 코드로 의도를 표현

### 5. 외부 자료 참고 시 주의사항

반드시 다음 공식 문서만 참고하세요:
- [MongoDB Atlas Vector Search 공식 문서](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [Spring AI 공식 문서](https://docs.spring.io/spring-ai/reference/)
- [langchain4j 공식 문서](https://docs.langchain4j.dev/)
- [OpenAI Embeddings API 공식 문서](https://platform.openai.com/docs/guides/embeddings)

---

## 출력 형식

### 설계서 구조

```markdown
# MongoDB Atlas Vector Search 활용 설계서

## 1. 개요
   - 목적
   - 범위
   - 용어 정의

## 2. 현재 구현 상태 분석
   - Document 스키마 분석
   - Vector Search 유틸리티 분석
   - 저장 로직 분석

## 3. Vector 필드 미저장 이유
   - 아키텍처적 결정 배경
   - 관심사 분리 원칙
   - 비동기 처리 필요성
   - 비용 최적화 전략

## 4. 임베딩 생성 구현 설계
   - 전략 선택 (권장안)
   - 컴포넌트 설계
   - 시퀀스 다이어그램
   - 인터페이스 정의

## 5. Vector Search 쿼리 구현
   - $vectorSearch aggregation 활용
   - Spring Data MongoDB 통합
   - 성능 최적화

## 6. 운영 고려사항
   - 배치 작업 설계
   - 모니터링 전략
   - 비용 관리

## 7. 구현 체크리스트
   - Phase 1: 기반 구현
   - Phase 2: 통합 테스트
   - Phase 3: 프로덕션 배포

## 8. 참고 자료
   - 공식 문서 링크
   - 관련 설계서
```

---

## 제약 조건

1. **LLM 오버엔지니어링 금지**: 불필요한 추상화 계층, 과도한 패턴 적용 금지
2. **불필요한 추가 작업 금지**: 요청된 범위 내에서만 설계
3. **장황한 주석 금지**: 코드는 자기 문서화, 주석은 Why만 설명
4. **추측 금지**: 확인되지 않은 정보는 명시적으로 가정으로 표시
5. **현재 코드베이스 존중**: 기존 구현 스타일과 일관성 유지

---

## 핵심 결론 가이드

설계서의 핵심 결론은 다음 질문에 답해야 합니다:

> **Q: 왜 embeddingText와 embeddingVector를 저장 시점에 설정하지 않는가?**

**A (예상 결론)**:
1. **관심사 분리**: 데이터 저장과 임베딩 생성은 별개의 책임
2. **성능 최적화**: 외부 API 호출은 비동기로 처리하여 응답 시간 단축
3. **비용 효율**: 배치 처리로 API 호출 최적화
4. **유연성**: 임베딩 모델/전략 변경 시 저장 로직 수정 불필요
5. **장애 격리**: 임베딩 서비스 장애가 데이터 저장에 영향 없음

---

## 체크리스트

설계서 작성 전 다음을 확인하세요:

- [ ] 모든 관련 소스 코드 분석 완료
- [ ] 기존 설계서 참고 완료
- [ ] 공식 문서 기반 내용 확인
- [ ] SOLID 원칙 적용 여부 검토
- [ ] 오버엔지니어링 여부 검토
- [ ] 실질적으로 구현 가능한 수준의 설계
