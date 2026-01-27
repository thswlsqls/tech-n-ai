# langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 설계서 작성 프롬프트

## 역할 정의

당신은 **백엔드 아키텍트**이자 **LLM 통합 전문가**입니다. 현재 프로젝트의 구조와 설계 패턴을 완전히 이해하고 있으며, langchain4j 오픈소스를 활용하여 운영 환경에서 유지 가능한 RAG 기반 챗봇 시스템을 설계할 수 있는 전문가입니다.

## 프로젝트 컨텍스트

### 프로젝트 구조
- **프로젝트 타입**: Spring Boot 기반 멀티모듈 프로젝트
- **아키텍처 패턴**: CQRS 패턴 적용 (Command Side: Aurora MySQL, Query Side: MongoDB Atlas)
- **동기화 메커니즘**: Kafka 기반 이벤트 동기화
- **API 모듈 구조**: `api/auth`, `api/contest`, `api/news`, `api/gateway`, `api/archive`

### MongoDB Atlas 컬렉션 구조
현재 프로젝트에서 사용 중인 MongoDB Atlas 컬렉션:
1. **ContestDocument** (`contests`): 개발자 대회 정보
   - 주요 필드: `title`, `description`, `startDate`, `endDate`, `status`, `metadata.tags`
2. **NewsArticleDocument** (`news_articles`): IT 테크 뉴스 기사
   - 주요 필드: `title`, `content`, `summary`, `publishedAt`, `metadata.tags`
3. **ArchiveDocument** (`archives`): 사용자 아카이브 항목
   - 주요 필드: `itemTitle`, `itemSummary`, `tag`, `memo`
4. **UserProfileDocument** (`user_profiles`): 사용자 프로필 정보
5. **SourcesDocument** (`sources`): 정보 출처 메타데이터

### 기존 설계서 참고 경로
- MongoDB 스키마 설계: `docs/step1/2. mongodb-schema-design.md`
- CQRS Kafka 동기화 설계: `docs/step11/cqrs-kafka-sync-design.md`
- AI 통합 분석: `docs/step11/ai-integration-analysis.md`
- API 엔드포인트 설계: `docs/step2/1. api-endpoint-design.md`

### 참고 자료
- **langchain4j 공식 문서**: https://docs.langchain4j.dev/
- **langchain4j GitHub**: https://github.com/langchain4j/langchain4j
- **MongoDB Atlas Vector Search 공식 문서**: https://www.mongodb.com/docs/atlas/atlas-vector-search/
- **참고 블로그 글**: https://ebson.tistory.com/423

## 설계서 작성 요구사항

### 필수 요구사항

#### 1. langchain4j 오픈소스 활용
- **반드시 사용**: langchain4j 오픈소스 라이브러리만 사용 (https://github.com/langchain4j/langchain4j)
- **제외 사항**: spring-ai, 다른 LLM 통합 프레임워크는 사용하지 않음
- **버전**: 최신 안정 버전 사용 (공식 문서 기준)
- **의존성**: `build.gradle`에 필요한 langchain4j 모듈 의존성 명시

#### 2. MongoDB Atlas Collections 임베딩
- **대상 컬렉션**: 다음 컬렉션의 도큐먼트들을 임베딩해야 함
  - `ContestDocument`: 대회 정보 검색용
  - `NewsArticleDocument`: 뉴스 기사 검색용
  - `ArchiveDocument`: 사용자 아카이브 검색용 (사용자별 필터링 필요)
- **임베딩 필드**: 각 도큐먼트의 검색에 적합한 필드 조합 설계
  - 예: `ContestDocument`: `title + description + metadata.tags`
  - 예: `NewsArticleDocument`: `title + summary + content` (길이 제한 고려)
  - 예: `ArchiveDocument`: `itemTitle + itemSummary + tag + memo`
- **임베딩 모델**: langchain4j에서 지원하는 임베딩 모델 선택 및 근거 제시
- **벡터 필드**: MongoDB Atlas Vector Search를 위한 벡터 필드 설계

#### 3. MongoDB Atlas Vector Search 최적화
- **Vector Index 설정**: HNSW 인덱스 파라미터 설계
  - `numLists`: 검색 정확도와 성능 균형
  - `similarity`: 코사인 유사도 또는 내적 유사도 선택 근거
  - `dimensions`: 선택한 임베딩 모델의 차원 수
- **검색 최적화 전략**:
  - 검색 결과 수 제한 (기본값 및 최대값)
  - 유사도 임계값 설정
  - 컬렉션별 검색 전략 차별화
- **리소스 관리**: MongoDB Atlas 클러스터 리소스와의 공유 고려

#### 4. 유저 입력 전처리 최적화
- **입력 검증**: 길이 제한, 빈 입력 처리, 특수 문자 필터링
- **입력 정규화**: 대소문자, 공백, 불필요한 문자 제거
- **의도 분류**: RAG가 필요한 질문인지 판단 로직
  - 예: "안녕하세요" → 일반 대화 (RAG 불필요)
  - 예: "최근 대회 정보 알려줘" → RAG 필요
- **전처리 파이프라인**: 단계별 처리 흐름 설계

#### 5. 토큰 제어 최적화
- **검색 결과 토큰 제어**:
  - 검색 결과 문서 수 제한
  - 각 문서의 최대 토큰 수 제한
  - 문서 요약 또는 필드 선택 전략
- **프롬프트 토큰 제어**:
  - 시스템 프롬프트 최적화
  - 컨텍스트 길이 제한
  - 불필요한 정보 제거
- **LLM 호출 전 토큰 예측**: 요청 전 토큰 수 예측 및 제한 검증

#### 6. 프롬프트 체인 구축 최적화
- **체인 분리 전략**:
  1. **입력 해석 체인**: 유저 입력을 검색 쿼리로 변환
  2. **검색 결과 정제 체인**: 검색 결과 필터링 및 요약
  3. **답변 생성 체인**: 최종 답변 생성
- **각 체인의 책임 분리**: 단일 책임 원칙 준수
- **체인 간 데이터 전달**: 인터페이스 설계
- **에러 처리**: 각 체인 단계별 에러 핸들링 전략

#### 7. 비용 통제 전략
- **요청 분류**: RAG 필요 여부에 따른 라우팅
  - RAG 불필요 요청: 일반 LLM 호출 (비용 절감)
  - RAG 필요 요청: 임베딩 + 벡터 검색 + LLM 호출
- **토큰 사용량 추적**:
  - 요청별 토큰 사용량 로깅
  - 사용자별/일별 토큰 사용량 집계
  - 비용 예측 및 알림
- **캐싱 전략**:
  - 유사 질문 캐싱 (임베딩 유사도 기반)
  - 검색 결과 캐싱 (TTL 설정)
- **Rate Limiting**: 사용자별 요청 제한

#### 8. AI API Provider 비교 분석
다음 LLM Provider들을 비교 분석하여 표로 정리:
- **OpenAI** (GPT-4, GPT-3.5-turbo)
- **Anthropic** (Claude 3.5 Sonnet, Claude 3 Opus)
- **Google** (Gemini Pro, Gemini Ultra)
- **Azure OpenAI** (GPT-4, GPT-3.5-turbo)

**비교 항목**:
- 성능 (응답 품질, 속도)
- 비용 (입력 토큰, 출력 토큰 단가)
- 특징 (컨텍스트 길이, 함수 호출 지원 등)
- 장단점
- 운영 환경 적합성
- langchain4j 지원 수준

**권장 Provider 선택 근거**: 프로젝트 특성에 맞는 Provider 선택 및 근거 제시

#### 9. Chatbot API 엔드포인트 설계
- **RESTful API 설계**: 현재 프로젝트의 API 패턴 준수
  - 경로: `/api/v1/chatbot` 또는 `/api/v1/chat`
  - HTTP 메서드: `POST` (채팅 메시지 전송)
- **요청 DTO 설계**:
  ```java
  - message: String (유저 메시지)
  - conversationId: String? (선택, 대화 세션 관리)
  - userId: String? (선택, 사용자별 아카이브 검색용)
  - options: ChatOptions? (선택, 검색 옵션)
  ```
- **응답 DTO 설계**:
  ```java
  - response: String (챗봇 응답)
  - sources: List<Source>? (참조된 문서 정보)
  - conversationId: String (대화 세션 ID)
  - tokenUsage: TokenUsage? (토큰 사용량 정보)
  ```
- **에러 처리**: 현재 프로젝트의 예외 처리 패턴 준수
- **인증/인가**: Spring Security 통합 (필요 시)

#### 10. 프로젝트 구조 통합
- **모듈 구조**: 새로운 모듈 생성 또는 기존 모듈 확장 결정
  - 옵션 1: `api/chatbot` 모듈 신규 생성
  - 옵션 2: `api/gateway` 모듈에 통합
- **의존성 관리**: `common` 모듈 활용 여부 결정
- **설정 파일**: `application-*.yml` 패턴 준수
- **패키지 구조**: 현재 프로젝트의 패키지 구조 패턴 준수
  - `controller`, `facade`, `service`, `dto`, `config` 등

#### 11. 외부 자료 참고 규칙
- **공식 출처만 사용**: 다음 출처의 정보만 참고
  - langchain4j 공식 문서 (https://docs.langchain4j.dev/)
  - langchain4j GitHub 저장소 (https://github.com/langchain4j/langchain4j)
  - MongoDB Atlas 공식 문서 (https://www.mongodb.com/docs/atlas/)
  - 각 LLM Provider 공식 문서 (OpenAI, Anthropic, Google 등)
- **비공식 자료 금지**: 블로그, 개인 문서 등 비공식 자료는 참고하지 않음
- **버전 정보**: 모든 라이브러리와 서비스의 버전 명시

#### 12. 오버엔지니어링 방지
- **최소 구현 원칙**: 현재 필요한 기능만 설계
- **복잡도 관리**: 불필요한 추상화 레이어 지양
- **단순성 우선**: 이해하기 쉽고 유지보수 가능한 구조
- **단계적 확장**: 향후 확장 가능하되 현재는 최소 기능만

#### 13. 객체지향 설계 및 클린코드 원칙
- **SOLID 원칙 준수**:
  - Single Responsibility Principle: 각 클래스는 단일 책임
  - Open/Closed Principle: 확장에는 열려있고 수정에는 닫혀있음
  - Liskov Substitution Principle: 인터페이스 기반 설계
  - Interface Segregation Principle: 작은 인터페이스 선호
  - Dependency Inversion Principle: 의존성 역전
- **클린코드 원칙**:
  - 의미 있는 이름 사용
  - 작은 함수/클래스
  - 주석보다 코드로 의도 표현
  - DRY (Don't Repeat Yourself)
- **디자인 패턴**: 적절한 패턴 사용 (전략, 팩토리, 빌더 등)

## 설계서 작성 지시사항

### 1단계: 프로젝트 분석
1. 현재 프로젝트의 모든 코드와 설계서를 철저히 분석
2. MongoDB Atlas 컬렉션 구조 완전히 이해
3. 기존 API 패턴 및 설계 원칙 파악
4. CQRS 패턴과 Kafka 동기화 구조 이해

### 2단계: 요구사항 분석
1. 각 요구사항별 상세 분석
2. 요구사항 간 의존성 파악
3. 우선순위 결정
4. 제약 조건 명확화

### 3단계: 아키텍처 설계
1. 전체 시스템 아키텍처 다이어그램 (Mermaid 형식)
2. 컴포넌트 간 상호작용 흐름도
3. 데이터 흐름도 (임베딩 생성 → 벡터 저장 → 검색 → LLM 호출)
4. 모듈 구조 및 패키지 구조

### 4단계: 상세 설계
각 섹션별로 다음 내용 포함:

#### 4.1 langchain4j 통합 설계
- 의존성 설정 (`build.gradle`)
- LLM Provider 설정
- 임베딩 모델 설정
- Vector Store 설정 (MongoDB Atlas)

#### 4.2 MongoDB Atlas Vector Search 설계
- 벡터 필드 스키마 설계
- Vector Index 설정
- 검색 쿼리 설계
- 성능 최적화 전략

#### 4.3 유저 입력 전처리 설계
- 전처리 파이프라인 설계
- 의도 분류 로직
- 검증 규칙
- 에러 처리

#### 4.4 토큰 제어 설계
- 토큰 예측 로직
- 검색 결과 토큰 제한
- 프롬프트 최적화
- 토큰 사용량 추적

#### 4.5 프롬프트 체인 설계
- 각 체인의 역할 및 책임
- 체인 간 인터페이스
- 에러 처리 전략
- 테스트 가능한 구조

#### 4.6 비용 통제 설계
- 요청 분류 로직
- 캐싱 전략
- Rate Limiting
- 모니터링 및 알림

#### 4.7 AI API Provider 비교 분석
- 상세 비교 표
- 선택 근거
- 마이그레이션 전략 (필요 시)

#### 4.8 API 엔드포인트 설계
- RESTful API 설계
- DTO 설계 (요청/응답)
- 에러 응답 설계
- 인증/인가 통합

### 5단계: 구현 가이드
1. 단계별 구현 순서
2. 각 컴포넌트 구현 가이드
3. 설정 파일 예제
4. 테스트 전략

### 6단계: 검증 기준
1. 기능 검증 기준
2. 성능 검증 기준
3. 비용 검증 기준
4. 품질 검증 기준

## 설계서 출력 형식

### 문서 구조
```markdown
# langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 설계서

**작성 일시**: YYYY-MM-DD
**대상 모듈**: `api/chatbot` (또는 통합 대상 모듈)
**목적**: langchain4j와 MongoDB Atlas Vector Search를 활용한 RAG 기반 챗봇 시스템 설계

## 목차
1. [개요](#개요)
2. [설계 원칙](#설계-원칙)
3. [현재 프로젝트 분석](#현재-프로젝트-분석)
4. [아키텍처 설계](#아키텍처-설계)
5. [상세 설계](#상세-설계)
   - [langchain4j 통합 설계](#langchain4j-통합-설계)
   - [MongoDB Atlas Vector Search 설계](#mongodb-atlas-vector-search-설계)
   - [유저 입력 전처리 설계](#유저-입력-전처리-설계)
   - [토큰 제어 설계](#토큰-제어-설계)
   - [프롬프트 체인 구축 설계](#프롬프트-체인-구축-설계)
   - [비용 통제 전략 설계](#비용-통제-전략-설계)
   - [AI API Provider 비교 분석](#ai-api-provider-비교-분석)
   - [Chatbot API 엔드포인트 설계](#chatbot-api-엔드포인트-설계)
6. [구현 가이드](#구현-가이드)
7. [검증 기준](#검증-기준)
8. [참고 자료](#참고-자료)
```

### 다이어그램 형식
- **Mermaid 형식 사용**: 모든 다이어그램은 Mermaid 형식으로 작성
- 다이어그램 종류:
  - 아키텍처 다이어그램
  - 시퀀스 다이어그램
  - 클래스 다이어그램 (주요 컴포넌트)
  - 데이터 흐름도

### 코드 예제
- **Java 코드**: 실제 구현 가능한 코드 예제 제공
- **설정 파일**: `application.yml`, `build.gradle` 예제
- **의사코드**: 복잡한 로직은 의사코드로 설명

## 중요 지침

### 반드시 준수할 사항
1. ✅ **langchain4j만 사용**: 다른 LLM 통합 프레임워크 사용 금지
2. ✅ **공식 문서만 참고**: 신뢰할 수 있는 공식 출처만 사용
3. ✅ **프로젝트 구조 준수**: 현재 프로젝트의 패턴과 구조 완전히 준수
4. ✅ **객체지향 설계**: SOLID 원칙 및 클린코드 원칙 준수
5. ✅ **오버엔지니어링 방지**: 필요한 기능만 설계
6. ✅ **운영 환경 고려**: 실제 운영 환경에서 유지 가능한 설계

### 금지 사항
1. ❌ **비공식 자료 참고**: 블로그, 개인 문서 등 비공식 자료 사용 금지
2. ❌ **spring-ai 사용**: langchain4j 외 다른 프레임워크 사용 금지
3. ❌ **과도한 추상화**: 불필요한 추상화 레이어 생성 금지
4. ❌ **불명확한 설계**: 모호하거나 구현 불가능한 설계 금지
5. ❌ **프로젝트 구조 무시**: 기존 프로젝트 구조와 다른 설계 금지

## 최종 확인 사항

설계서 작성 완료 후 다음 사항을 확인:

- [ ] langchain4j 오픈소스만 사용하는지 확인
- [ ] MongoDB Atlas Vector Search 설계가 포함되었는지 확인
- [ ] 유저 입력 전처리 설계가 포함되었는지 확인
- [ ] 토큰 제어 설계가 포함되었는지 확인
- [ ] 프롬프트 체인 구축 설계가 포함되었는지 확인
- [ ] 비용 통제 전략 설계가 포함되었는지 확인
- [ ] AI API Provider 비교 분석이 포함되었는지 확인
- [ ] Chatbot API 엔드포인트 설계가 포함되었는지 확인
- [ ] 현재 프로젝트 구조와 통합 가능한지 확인
- [ ] 공식 문서만 참고했는지 확인
- [ ] 오버엔지니어링을 방지했는지 확인
- [ ] 객체지향 설계 원칙을 준수했는지 확인
- [ ] 모든 다이어그램이 Mermaid 형식인지 확인
- [ ] 구현 가능한 코드 예제가 포함되었는지 확인

## 시작 지시

위의 모든 요구사항과 지침을 준수하여 **langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 설계서**를 작성하세요.

설계서는 `docs/step12/rag-chatbot-design.md` 경로에 저장될 예정입니다.

**중요**: 설계서는 실제 구현 가능해야 하며, 현재 프로젝트에 바로 통합 가능한 수준의 상세함을 가져야 합니다.
