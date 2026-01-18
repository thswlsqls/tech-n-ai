# Spring Batch에서 AI LLM 통합 분석 프롬프트

## 역할 및 목표 설정

당신은 **Spring AI 통합 아키텍트 및 기술 분석 전문가**입니다. Spring Batch 애플리케이션에서 AI LLM Model을 활용하여 `prompts/source-discovery-prompt.md` 프롬프트를 전달하고 `json/sources.json` 응답을 받는 구체적인 구현 방법을 분석하고 문서화해야 합니다.

## 작업 범위

다음 작업을 수행하여 `docs/ai-integration-analysis.md` 파일을 생성하세요:

1. **Spring Batch에서 AI LLM 통합 방법 분석**
   - `prompts/source-discovery-prompt.md` 프롬프트를 LLM에 전달
   - `json/sources.json` 형식의 구조화된 응답 수신
   - Spring Batch Step/Tasklet에서의 통합 패턴

2. **spring-ai 프레임워크 분석**
   - 공식 기술 문서 기반 구현 방법 탐색
   - Spring Batch와의 통합 패턴
   - 구조화된 출력(Structured Output) 처리 방법
   - 프롬프트 템플릿 활용 방법

3. **langchain4j 프레임워크 분석**
   - 공식 기술 문서 기반 구현 방법 탐색
   - Spring Batch와의 통합 패턴
   - 구조화된 출력(Structured Output) 처리 방법
   - 프롬프트 템플릿 활용 방법

4. **비교 분석 및 검증**
   - 두 프레임워크의 특징 비교
   - 장단점 분석
   - 공식 기술 문서 기반 검증
   - 오버엔지니어링 및 부정확한 내용 정제

## 분석 전략 (Chain of Thought)

### 1단계: 공식 기술 문서 수집 및 검증

**신뢰할 수 있는 공식 기술 문서만 참고:**
- Spring AI 공식 문서: https://docs.spring.io/spring-ai/reference/
- LangChain4j 공식 문서: https://github.com/langchain4j/langchain4j
- Spring Batch 공식 문서: https://docs.spring.io/spring-batch/docs/current/reference/html/
- 각 프레임워크의 GitHub 공식 저장소 README 및 Wiki

**검증 원칙:**
- 공식 문서의 예제 코드만 인용
- 최신 버전 정보 확인
- 공식 문서에 없는 내용은 추측하지 않음
- 불확실한 내용은 명시적으로 표시

### 2단계: Spring Batch 통합 패턴 분석

**분석 항목:**
1. **Tasklet 기반 통합**
   - AI LLM 호출을 Tasklet에서 처리
   - 프롬프트 파일 읽기 및 전달
   - 응답 파싱 및 검증
   - json/sources.json 파일 생성

2. **Chunk 기반 통합**
   - ItemReader에서 프롬프트 준비
   - ItemProcessor에서 LLM 호출
   - ItemWriter에서 결과 저장

3. **비동기 처리 고려사항**
   - LLM API 호출의 비동기 처리
   - 타임아웃 및 재시도 전략
   - 에러 핸들링

### 3단계: spring-ai 프레임워크 분석

**분석 항목:**
1. **의존성 및 설정**
   - build.gradle.kts 의존성 추가
   - application.yml 설정 (API 키, 모델 선택)
   - Spring Boot Auto-Configuration

2. **ChatClient 활용**
   - ChatClient 인터페이스
   - 프롬프트 전달 방법
   - 응답 수신 및 파싱

3. **구조화된 출력 처리**
   - OutputParser 활용
   - JSON 응답 파싱
   - json/sources.json 형식으로 변환

4. **프롬프트 템플릿**
   - PromptTemplate 활용
   - 파일 기반 프롬프트 로딩
   - 동적 변수 치환

5. **Spring Batch 통합 예제**
   - Tasklet 구현 예제
   - 설정 클래스 예제
   - 에러 핸들링 예제

### 4단계: langchain4j 프레임워크 분석

**분석 항목:**
1. **의존성 및 설정**
   - build.gradle.kts 의존성 추가
   - 설정 클래스 구현
   - LLM Provider 선택 (OpenAI, Anthropic 등)

2. **ChatLanguageModel 활용**
   - ChatLanguageModel 인터페이스
   - 프롬프트 전달 방법
   - 응답 수신 및 파싱

3. **구조화된 출력 처리**
   - StructuredOutputParser 활용
   - JSON 응답 파싱
   - json/sources.json 형식으로 변환

4. **프롬프트 템플릿**
   - PromptTemplate 활용
   - 파일 기반 프롬프트 로딩
   - 동적 변수 치환

5. **Spring Batch 통합 예제**
   - Tasklet 구현 예제
   - 설정 클래스 예제
   - 에러 핸들링 예제

### 5단계: 비교 분석

**비교 항목:**
1. **프레임워크 특징**
   - 아키텍처 및 설계 철학
   - Spring 생태계 통합 수준
   - 커뮤니티 및 생태계

2. **구현 복잡도**
   - 설정 복잡도
   - 코드 작성 복잡도
   - 학습 곡선

3. **기능 비교**
   - LLM Provider 지원 범위
   - 구조화된 출력 지원
   - 프롬프트 관리 기능
   - 스트리밍 지원

4. **성능 및 안정성**
   - API 호출 최적화
   - 에러 핸들링
   - 재시도 메커니즘
   - 타임아웃 처리

5. **유지보수성**
   - 문서화 수준
   - 업데이트 빈도
   - 이전 버전 호환성
   - 마이그레이션 난이도

### 6단계: 검증 및 정제

**검증 절차:**
1. 공식 기술 문서 재확인
2. 예제 코드의 정확성 검증
3. 버전 호환성 확인
4. 실제 구현 가능성 검증

**정제 원칙:**
- 오버엔지니어링 제거: 불필요하게 복잡한 패턴 제거
- 억지스러운 내용 제거: 공식 문서에 없는 추측성 내용 제거
- 방어적 내용 정제: 과도한 예외 처리나 방어적 코딩 패턴 정제
- 정확하지 않은 내용 제거: 검증되지 않은 주장 제거
- 간결성 추구: 핵심 내용만 남기고 불필요한 설명 제거

## 출력 형식 (Structured Output)

다음 구조로 `docs/ai-integration-analysis.md` 파일을 작성하세요:

```markdown
# Spring Batch에서 AI LLM 통합 분석 문서

## 개요
- 목적 및 배경
- 분석 범위
- 문서 버전 및 작성일

## 요구사항 분석
- Spring Batch에서 AI LLM 통합 요구사항
- json/sources.json 생성 프로세스
- 프롬프트 전달 및 응답 처리 흐름

## spring-ai 프레임워크 분석

### 1. 프레임워크 개요
- 공식 문서 링크
- 주요 특징
- Spring 생태계 통합 수준

### 2. 의존성 및 설정
- build.gradle.kts 의존성 (공식 문서 기반)
- application.yml 설정 예제
- Spring Boot Auto-Configuration

### 3. Spring Batch 통합 구현
- Tasklet 구현 예제 (공식 문서 기반)
- ChatClient 활용 방법
- 프롬프트 파일 로딩 및 전달
- 구조화된 출력 처리
- json/sources.json 생성 로직

### 4. 장점
- (공식 문서 기반 실제 장점만)

### 5. 단점
- (공식 문서 및 실제 사용 경험 기반)

## langchain4j 프레임워크 분석

### 1. 프레임워크 개요
- 공식 문서 링크
- 주요 특징
- Spring 생태계 통합 수준

### 2. 의존성 및 설정
- build.gradle.kts 의존성 (공식 문서 기반)
- 설정 클래스 구현
- LLM Provider 설정

### 3. Spring Batch 통합 구현
- Tasklet 구현 예제 (공식 문서 기반)
- ChatLanguageModel 활용 방법
- 프롬프트 파일 로딩 및 전달
- 구조화된 출력 처리
- json/sources.json 생성 로직

### 4. 장점
- (공식 문서 기반 실제 장점만)

### 5. 단점
- (공식 문서 및 실제 사용 경험 기반)

## 비교 분석

### 기능 비교표
| 항목 | spring-ai | langchain4j |
|------|-----------|-------------|
| Spring 통합 | | |
| LLM Provider 지원 | | |
| 구조화된 출력 | | |
| 프롬프트 관리 | | |
| 문서화 수준 | | |

### 구현 복잡도 비교
- 설정 복잡도
- 코드 작성 복잡도
- 학습 곡선

### 성능 및 안정성 비교
- API 호출 최적화
- 에러 핸들링
- 재시도 메커니즘

### 유지보수성 비교
- 문서화 수준
- 업데이트 빈도
- 커뮤니티 지원

## 권장 사항

### 사용 시나리오별 권장 프레임워크
- Spring 생태계 중심 프로젝트: spring-ai
- 다양한 LLM Provider 필요: langchain4j
- 간단한 통합 필요: spring-ai
- 고급 기능 필요: langchain4j

### 구현 우선순위
1. Phase 1: spring-ai로 프로토타입 구현
2. Phase 2: 요구사항에 따라 langchain4j 검토
3. Phase 3: 프로덕션 환경에 맞는 프레임워크 선택

## 참고 자료
- Spring AI 공식 문서: [링크]
- LangChain4j 공식 문서: [링크]
- Spring Batch 공식 문서: [링크]
- 각 프레임워크 GitHub 저장소: [링크]

## 부록: 구현 예제 코드
- spring-ai Tasklet 예제 (공식 문서 기반)
- langchain4j Tasklet 예제 (공식 문서 기반)
- 설정 파일 예제
```

## 실행 지시사항

### 1단계: 공식 기술 문서 수집
1. Spring AI 공식 문서에서 다음 섹션 확인:
   - Getting Started Guide
   - ChatClient API 문서
   - PromptTemplate 문서
   - OutputParser 문서
   - Spring Boot Integration

2. LangChain4j 공식 문서에서 다음 섹션 확인:
   - Getting Started Guide
   - ChatLanguageModel API 문서
   - StructuredOutputParser 문서
   - Spring Integration (있는 경우)

3. Spring Batch 공식 문서에서 다음 섹션 확인:
   - Tasklet 구현 가이드
   - Chunk-oriented Processing
   - Error Handling

### 2단계: 구현 방법 분석
1. 각 프레임워크의 공식 예제 코드 분석
2. Spring Batch 통합 패턴 식별
3. 프롬프트 파일 로딩 방법 탐색
4. 구조화된 출력 처리 방법 탐색

### 3단계: 비교 분석 수행
1. 기능별 상세 비교
2. 실제 사용 시나리오 기반 장단점 분석
3. 공식 문서 기반 객관적 평가

### 4단계: 검증 및 정제
1. 모든 예제 코드를 공식 문서와 대조
2. 추측성 내용 제거
3. 오버엔지니어링 패턴 제거
4. 간결하고 정확한 설명으로 정제

## 품질 기준

### 정확성
- 모든 내용은 공식 기술 문서 기반
- 예제 코드는 공식 문서에서 인용
- 버전 정보 명시
- 불확실한 내용은 명시적으로 표시

### 완전성
- 두 프레임워크 모두 상세 분석
- 비교 분석 포함
- 구현 예제 포함
- 참고 자료 포함

### 간결성
- 핵심 내용만 포함
- 불필요한 설명 제거
- 오버엔지니어링 제거
- 방어적 내용 최소화

### 실용성
- 실제 구현 가능한 내용만 포함
- 구체적인 예제 코드 제공
- 단계별 구현 가이드 제공
- 권장 사항 명확히 제시

## 최종 산출물

`docs/ai-integration-analysis.md` 파일을 생성하고, 다음 내용을 포함하세요:

1. ✅ Spring Batch에서 AI LLM 통합 방법 상세 분석
2. ✅ spring-ai 프레임워크 분석 (공식 문서 기반)
3. ✅ langchain4j 프레임워크 분석 (공식 문서 기반)
4. ✅ 두 프레임워크 비교 분석 (장단점 포함)
5. ✅ 공식 기술 문서 기반 검증 완료
6. ✅ 오버엔지니어링 및 부정확한 내용 정제 완료
7. ✅ 구현 예제 코드 (공식 문서 기반)
8. ✅ 권장 사항 및 사용 시나리오별 가이드

## 중요 주의사항

1. **공식 문서만 참고**: 공식 기술 문서, GitHub 공식 저장소, 공식 예제만 사용
2. **추측 금지**: 공식 문서에 없는 내용은 추측하지 말고 명시적으로 표시
3. **버전 명시**: 모든 예제 코드에 프레임워크 버전 명시
4. **검증 필수**: 모든 내용을 공식 문서와 재확인
5. **정제 필수**: 오버엔지니어링, 억지스러운 내용, 방어적 내용 정제
6. **간결성 추구**: 핵심만 남기고 불필요한 설명 제거

## 시작 지시

위의 분석 전략과 출력 형식을 따라 `docs/ai-integration-analysis.md` 파일을 생성하세요. 각 섹션을 체계적으로 작성하고, 공식 기술 문서만 참고하여 정확하고 실용적인 내용을 제공하세요.

