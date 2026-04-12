# Spring Batch에서 AI LLM 통합 분석 문서

## 개요

### 목적 및 배경
본 문서는 Spring Batch 애플리케이션에서 AI LLM Model을 활용하여 `prompts/source-discovery-prompt.md` 프롬프트를 전달하고 `json/sources.json` 형식의 구조화된 응답을 받는 구체적인 구현 방법을 분석합니다.

### 분석 범위
- Spring Batch와 AI LLM 통합 패턴 분석
- spring-ai 프레임워크 분석 및 구현 방법
- langchain4j 프레임워크 분석 및 구현 방법
- 두 프레임워크의 비교 분석 및 권장 사항

### 문서 버전 및 작성일
- 버전: 1.1.0
- 작성일: 2025-01-05
- 최종 수정일: 2025-01-05 (Anthropic LLM 모델로 변경)

## 요구사항 분석

### Spring Batch에서 AI LLM 통합 요구사항

1. **프롬프트 파일 로딩**
   - `prompts/source-discovery-prompt.md` 파일을 읽어서 LLM에 전달
   - 파일 경로는 환경 변수 또는 설정 파일로 관리

2. **LLM API 호출**
   - 프롬프트를 LLM에 전달
   - 구조화된 JSON 응답 수신
   - 타임아웃 및 재시도 처리

3. **응답 처리 및 검증**
   - JSON 응답 파싱
   - `json/sources.json` 스키마 검증
   - 데이터 정제 및 변환

4. **파일 생성**
   - 검증된 데이터를 `json/sources.json` 파일로 저장
   - 기존 파일 백업 및 버전 관리

### json/sources.json 생성 프로세스

```
1. Spring Batch Job 시작
   ↓
2. SourceDiscoveryStep 실행
   ↓
3. 프롬프트 파일 로딩 (source-discovery-prompt.md)
   ↓
4. LLM API 호출 (프롬프트 전달)
   ↓
5. JSON 응답 수신 및 파싱
   ↓
6. 스키마 검증 및 데이터 정제
   ↓
7. json/sources.json 파일 생성
   ↓
8. 버전 관리 및 백업
```

### 프롬프트 전달 및 응답 처리 흐름

```
Tasklet.execute()
  ├─> PromptFileLoader.load("prompts/source-discovery-prompt.md")
  ├─> LLMClient.call(prompt)
  ├─> ResponseParser.parse(jsonResponse)
  ├─> SchemaValidator.validate(sourcesData)
  └─> FileWriter.write("json/sources.json", sourcesData)
```

## spring-ai 프레임워크 분석

### 1. 프레임워크 개요

**공식 문서 링크:**
- 공식 문서: https://docs.spring.io/spring-ai/reference/
- GitHub 저장소: https://github.com/spring-projects/spring-ai

**주요 특징:**
- Spring 생태계에 최적화된 AI 통합 프레임워크
- Spring Boot Auto-Configuration 지원
- 다양한 LLM Provider 지원 (Anthropic, OpenAI, Azure OpenAI 등)
- 구조화된 출력 처리 지원

**Spring 생태계 통합 수준:**
- ⭐⭐⭐⭐⭐ (5/5)
- Spring Boot Starter 제공
- 자동 설정 및 의존성 주입 완벽 지원
- Spring의 관례 및 패턴 준수

### 2. 의존성 및 설정

#### build.gradle.kts 의존성

```kotlin
dependencies {
    // Spring AI Anthropic
    implementation("org.springframework.ai:spring-ai-anthropic-spring-boot-starter:1.0.0")
    
    // 또는 OpenAI (대안)
    // implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:1.0.0")
    
    // Spring Batch
    implementation("org.springframework.batch:spring-batch-core:5.0.0")
}
```

**참고:** 실제 버전은 공식 문서에서 확인 필요

#### application.yml 설정 예제

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-opus-20240229
          temperature: 0.7
          max-tokens: 4000
```

#### Spring Boot Auto-Configuration

Spring AI는 Spring Boot Starter를 통해 자동으로 다음을 구성합니다:
- `ChatClient` 빈 자동 생성
- `PromptTemplate` 빈 자동 생성
- API 키 및 설정 자동 주입

### 3. Spring Batch 통합 구현

#### Tasklet 구현 예제

```java
@Component
public class SourceDiscoveryTasklet implements Tasklet {
    
    private final ChatClient chatClient;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    public SourceDiscoveryTasklet(
            ChatClient chatClient,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, 
                               ChunkContext chunkContext) throws Exception {
        
        // 1. 프롬프트 파일 로딩
        Resource promptResource = resourceLoader.getResource(
            "classpath:prompts/source-discovery-prompt.md");
        String prompt = Files.readString(
            Paths.get(promptResource.getURI()));
        
        // 2. LLM 호출
        String response = chatClient.call(prompt);
        
        // 3. JSON 응답 파싱
        SourcesResponse sourcesResponse = objectMapper.readValue(
            response, SourcesResponse.class);
        
        // 4. json/sources.json 파일 생성
        Path outputPath = Paths.get("json/sources.json");
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputPath.toFile(), sourcesResponse);
        
        return RepeatStatus.FINISHED;
    }
}
```

#### ChatClient 활용 방법

```java
// 기본 사용
String response = chatClient.call("프롬프트 내용");

// 구조화된 출력 요청
String jsonResponse = chatClient.call(
    "다음 JSON 형식으로 응답하세요: " + prompt
);
```

**참고:** 실제 API는 공식 문서에서 확인 필요. Spring AI의 ChatClient 인터페이스는 버전에 따라 다를 수 있음.

#### 프롬프트 파일 로딩 및 전달

```java
// ResourceLoader를 통한 파일 로딩
Resource resource = resourceLoader.getResource(
    "file:prompts/source-discovery-prompt.md");
String prompt = new String(Files.readAllBytes(
    Paths.get(resource.getURI())));
```

#### 구조화된 출력 처리

Spring AI는 구조화된 출력을 위한 `OutputParser`를 제공합니다:

```java
// 예시 (실제 API는 공식 문서 확인 필요)
OutputParser<SourcesResponse> parser = 
    new JsonOutputParser<>(SourcesResponse.class);
SourcesResponse response = parser.parse(llmResponse);
```

**주의:** 실제 OutputParser API는 공식 문서에서 확인 필요.

#### json/sources.json 생성 로직

```java
// Jackson ObjectMapper를 사용한 JSON 생성
ObjectMapper mapper = new ObjectMapper();
mapper.enable(SerializationFeature.INDENT_OUTPUT);

SourcesResponse data = parseResponse(llmResponse);
mapper.writeValue(new File("json/sources.json"), data);
```

### 4. 장점

1. **Spring 생태계 완벽 통합**
   - Spring Boot Auto-Configuration
   - 의존성 주입 및 관례 준수
   - Spring의 설정 관리 시스템 활용

2. **간단한 설정**
   - Starter 의존성만 추가하면 자동 설정
   - application.yml로 간단한 설정

3. **공식 지원**
   - Spring 공식 프로젝트
   - 지속적인 업데이트 및 지원

4. **다양한 LLM Provider 지원**
   - Anthropic, OpenAI, Azure OpenAI 등
   - Provider 간 전환 용이

### 5. 단점

1. **상대적으로 새로운 프레임워크**
   - 커뮤니티가 아직 성장 중
   - 예제 및 레퍼런스 제한적

2. **문서화 수준**
   - 공식 문서는 있으나 예제가 제한적일 수 있음
   - 실제 사용 사례 부족

3. **구조화된 출력 처리**
   - JSON 파싱은 수동으로 처리해야 할 수 있음
   - OutputParser 기능이 제한적일 수 있음

**주의:** 위 단점은 일반적인 관찰이며, 실제 기능은 공식 문서에서 확인 필요.

## langchain4j 프레임워크 분석

### 1. 프레임워크 개요

**공식 문서 링크:**
- GitHub 저장소: https://github.com/langchain4j/langchain4j
- 공식 문서: GitHub README 및 Wiki

**주요 특징:**
- Java/Kotlin용 LangChain 구현체
- 다양한 LLM Provider 지원
- 구조화된 출력 처리 강력 지원
- 체인(Chain) 기반 처리 패턴

**Spring 생태계 통합 수준:**
- ⭐⭐⭐ (3/5)
- Spring 통합 모듈 제공 (langchain4j-spring)
- Spring Boot Starter는 별도 제공되지 않을 수 있음
- 수동 설정 필요

### 2. 의존성 및 설정

#### build.gradle.kts 의존성

```kotlin
dependencies {
    // LangChain4j Core
    implementation("dev.langchain4j:langchain4j:0.29.1")
    
    // Anthropic 모듈
    implementation("dev.langchain4j:langchain4j-anthropic:0.29.1")
    
    // Spring 통합 (있는 경우)
    // implementation("dev.langchain4j:langchain4j-spring:0.29.1")
    
    // Spring Batch
    implementation("org.springframework.batch:spring-batch-core:5.0.0")
}
```

**참고:** 실제 버전 및 모듈명은 GitHub 저장소에서 확인 필요

#### 설정 클래스 구현

```java
@Configuration
public class LangChain4jConfig {
    
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .modelName("claude-3-opus-20240229")
            .temperature(0.7)
            .maxTokens(4000)
            .build();
    }
}
```

#### LLM Provider 설정

```java
// Anthropic
ChatLanguageModel model = AnthropicChatModel.builder()
    .apiKey(apiKey)
    .modelName("claude-3-opus-20240229")
    .build();

// OpenAI (대안)
// ChatLanguageModel model = OpenAiChatModel.builder()
//     .apiKey(apiKey)
//     .modelName("gpt-4")
//     .build();
```

### 3. Spring Batch 통합 구현

#### Tasklet 구현 예제

```java
@Component
public class SourceDiscoveryTasklet implements Tasklet {
    
    private final ChatLanguageModel chatModel;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    public SourceDiscoveryTasklet(
            ChatLanguageModel chatModel,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, 
                               ChunkContext chunkContext) throws Exception {
        
        // 1. 프롬프트 파일 로딩
        Resource promptResource = resourceLoader.getResource(
            "classpath:prompts/source-discovery-prompt.md");
        String prompt = Files.readString(
            Paths.get(promptResource.getURI()));
        
        // 2. LLM 호출
        String response = chatModel.generate(prompt);
        
        // 3. JSON 응답 파싱
        SourcesResponse sourcesResponse = objectMapper.readValue(
            response, SourcesResponse.class);
        
        // 4. json/sources.json 파일 생성
        Path outputPath = Paths.get("json/sources.json");
        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(outputPath.toFile(), sourcesResponse);
        
        return RepeatStatus.FINISHED;
    }
}
```

#### ChatLanguageModel 활용 방법

```java
// 기본 사용
String response = chatModel.generate("프롬프트 내용");

// 구조화된 출력 요청
String jsonPrompt = "다음 JSON 형식으로 응답하세요:\n" + prompt;
String jsonResponse = chatModel.generate(jsonPrompt);
```

#### 프롬프트 파일 로딩 및 전달

```java
// ResourceLoader를 통한 파일 로딩
Resource resource = resourceLoader.getResource(
    "file:prompts/source-discovery-prompt.md");
String prompt = new String(Files.readAllBytes(
    Paths.get(resource.getURI())));
```

#### 구조화된 출력 처리

LangChain4j는 구조화된 출력을 위한 강력한 기능을 제공합니다:

```java
// StructuredOutputParser 사용 (예시)
// 실제 API는 GitHub 문서에서 확인 필요
StructuredOutputParser<SourcesResponse> parser = 
    StructuredOutputParser.fromClass(SourcesResponse.class);
SourcesResponse response = parser.parse(llmResponse);
```

**주의:** 실제 StructuredOutputParser API는 GitHub 문서에서 확인 필요.

#### json/sources.json 생성 로직

```java
// Jackson ObjectMapper를 사용한 JSON 생성
ObjectMapper mapper = new ObjectMapper();
mapper.enable(SerializationFeature.INDENT_OUTPUT);

SourcesResponse data = parseResponse(llmResponse);
mapper.writeValue(new File("json/sources.json"), data);
```

### 4. 장점

1. **구조화된 출력 강력 지원**
   - StructuredOutputParser 제공
   - JSON 스키마 기반 파싱

2. **다양한 LLM Provider 지원**
   - Anthropic, OpenAI, Azure OpenAI 등
   - Provider 간 일관된 인터페이스

3. **체인(Chain) 기반 처리**
   - 복잡한 AI 워크플로우 구성 가능
   - 재사용 가능한 컴포넌트

4. **활발한 커뮤니티**
   - GitHub에서 활발한 개발
   - 다양한 예제 및 사용 사례

### 5. 단점

1. **Spring 통합 수준**
   - Spring Boot Starter 없음
   - 수동 설정 필요
   - Spring 생태계와의 통합이 spring-ai보다 낮음

2. **설정 복잡도**
   - Provider별 설정 클래스 수동 작성
   - Auto-Configuration 없음

3. **문서화**
   - GitHub README 중심
   - 공식 문서 사이트 없음

**주의:** 위 단점은 일반적인 관찰이며, 실제 기능은 GitHub 문서에서 확인 필요.

## 비교 분석

### 기능 비교표

| 항목 | spring-ai | langchain4j |
|------|-----------|-------------|
| Spring 통합 | ⭐⭐⭐⭐⭐ (Auto-Configuration) | ⭐⭐⭐ (수동 설정) |
| LLM Provider 지원 | ⭐⭐⭐⭐ (다양한 Provider) | ⭐⭐⭐⭐⭐ (매우 다양한 Provider) |
| 구조화된 출력 | ⭐⭐⭐ (기본 지원) | ⭐⭐⭐⭐⭐ (강력한 지원) |
| 프롬프트 관리 | ⭐⭐⭐⭐ (PromptTemplate) | ⭐⭐⭐⭐ (PromptTemplate) |
| 문서화 수준 | ⭐⭐⭐⭐ (공식 문서) | ⭐⭐⭐ (GitHub README) |
| 커뮤니티 | ⭐⭐⭐ (성장 중) | ⭐⭐⭐⭐ (활발) |
| 설정 간편성 | ⭐⭐⭐⭐⭐ (Starter) | ⭐⭐⭐ (수동) |

### 구현 복잡도 비교

#### 설정 복잡도
- **spring-ai**: ⭐⭐⭐⭐⭐ (매우 간단)
  - Starter 의존성 추가
  - application.yml 설정만으로 완료
  
- **langchain4j**: ⭐⭐⭐ (보통)
  - 의존성 추가
  - 설정 클래스 수동 작성 필요

#### 코드 작성 복잡도
- **spring-ai**: ⭐⭐⭐⭐ (간단)
  - ChatClient 인터페이스 간단
  - Spring의 관례 준수
  
- **langchain4j**: ⭐⭐⭐⭐ (간단)
  - ChatLanguageModel 인터페이스 간단
  - 직관적인 API

#### 학습 곡선
- **spring-ai**: ⭐⭐⭐⭐ (낮음)
  - Spring 개발자에게 친숙
  - Spring 패턴 준수
  
- **langchain4j**: ⭐⭐⭐ (보통)
  - LangChain 개념 이해 필요
  - 체인 패턴 학습 필요

### 성능 및 안정성 비교

#### API 호출 최적화
- **spring-ai**: 
  - Spring의 RestTemplate/WebClient 활용
  - 연결 풀링 자동 관리
  
- **langchain4j**:
  - Provider별 HTTP 클라이언트 사용
  - 연결 풀링은 Provider 구현에 의존

#### 에러 핸들링
- **spring-ai**:
  - Spring의 예외 처리 메커니즘 활용
  - 통합된 에러 핸들링
  
- **langchain4j**:
  - Provider별 예외 처리
  - 커스텀 에러 핸들링 필요

#### 재시도 메커니즘
- **spring-ai**:
  - Spring Retry 통합 가능
  - 설정 기반 재시도
  
- **langchain4j**:
  - 수동 재시도 구현 필요
  - 또는 외부 라이브러리 활용

#### 타임아웃 처리
- **spring-ai**:
  - Spring의 타임아웃 설정 활용
  - 통합된 타임아웃 관리
  
- **langchain4j**:
  - Provider별 타임아웃 설정
  - 수동 타임아웃 관리

### 유지보수성 비교

#### 문서화 수준
- **spring-ai**: ⭐⭐⭐⭐
  - 공식 문서 사이트
  - Spring 공식 프로젝트
  
- **langchain4j**: ⭐⭐⭐
  - GitHub README 중심
  - 커뮤니티 기반 문서

#### 업데이트 빈도
- **spring-ai**: 
  - Spring 공식 프로젝트로 정기 업데이트
  - 버전 관리 체계적
  
- **langchain4j**:
  - 활발한 커뮤니티 개발
  - 빠른 기능 추가

#### 이전 버전 호환성
- **spring-ai**:
  - Spring의 버전 관리 정책 준수
  - 마이그레이션 가이드 제공 가능성 높음
  
- **langchain4j**:
  - 커뮤니티 프로젝트로 호환성 정책 불명확
  - 마이그레이션 가이드 확인 필요

#### 마이그레이션 난이도
- **spring-ai**: ⭐⭐⭐⭐
  - Spring 생태계 내에서 마이그레이션 용이
  
- **langchain4j**: ⭐⭐⭐
  - 다른 프레임워크로의 마이그레이션 시 재작성 필요

## 권장 사항

### 사용 시나리오별 권장 프레임워크

#### Spring 생태계 중심 프로젝트: **spring-ai** ⭐⭐⭐⭐⭐
- Spring Boot 프로젝트
- 빠른 프로토타이핑 필요
- 최소한의 설정으로 시작

**이유:**
- Spring Boot Auto-Configuration
- Spring의 관례 및 패턴 준수
- 설정 간편성

#### 다양한 LLM Provider 필요: **langchain4j** ⭐⭐⭐⭐
- 여러 Provider 동시 사용
- Provider 간 전환 빈번
- 고급 AI 워크플로우 필요

**이유:**
- 더 많은 Provider 지원
- 체인 기반 복잡한 워크플로우 구성 가능

#### 간단한 통합 필요: **spring-ai** ⭐⭐⭐⭐⭐
- 기본적인 LLM 호출만 필요
- 빠른 개발 필요
- 최소한의 코드 작성

**이유:**
- Starter 의존성만 추가
- 간단한 API

#### 고급 기능 필요: **langchain4j** ⭐⭐⭐⭐
- 구조화된 출력 강력 필요
- 체인 기반 처리 필요
- 커스텀 AI 워크플로우

**이유:**
- StructuredOutputParser 강력
- 체인 패턴 지원

### 구현 우선순위

#### Phase 1: spring-ai로 프로토타입 구현 ⭐⭐⭐⭐⭐
**권장 이유:**
1. 설정 간편성
2. Spring 생태계 통합
3. 빠른 개발 가능
4. 공식 지원

**구현 단계:**
1. Starter 의존성 추가
2. application.yml 설정
3. Tasklet 구현
4. 테스트 및 검증

#### Phase 2: 요구사항에 따라 langchain4j 검토 ⭐⭐⭐
**검토 시점:**
- 구조화된 출력이 복잡한 경우
- 여러 Provider 동시 사용 필요
- 체인 기반 워크플로우 필요

#### Phase 3: 프로덕션 환경에 맞는 프레임워크 선택
**선택 기준:**
- 성능 요구사항
- 유지보수성
- 팀의 기술 스택
- 장기 지원 필요성

## 참고 자료

### 공식 문서
- **Spring AI 공식 문서**: https://docs.spring.io/spring-ai/reference/
- **Spring AI GitHub**: https://github.com/spring-projects/spring-ai
- **LangChain4j GitHub**: https://github.com/langchain4j/langchain4j
- **Spring Batch 공식 문서**: https://docs.spring.io/spring-batch/docs/current/reference/html/

### 추가 자료
- Spring AI 예제: https://github.com/spring-projects/spring-ai/tree/main/examples
- LangChain4j 예제: https://github.com/langchain4j/langchain4j-examples

## 부록: 구현 예제 코드

### spring-ai Tasklet 예제

```java
package com.example.batch.tasklet;

import org.springframework.ai.chat.ChatClient;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SourceDiscoveryTasklet implements Tasklet {
    
    private final ChatClient chatClient;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    public SourceDiscoveryTasklet(
            ChatClient chatClient,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, 
                               ChunkContext chunkContext) throws Exception {
        
        try {
            // 1. 프롬프트 파일 로딩
            Resource promptResource = resourceLoader.getResource(
                "file:prompts/source-discovery-prompt.md");
            String prompt = Files.readString(
                Paths.get(promptResource.getURI()));
            
            // 2. LLM 호출
            String response = chatClient.call(prompt);
            
            // 3. JSON 응답 파싱
            // JSON 응답에서 json/sources.json 형식 추출
            String jsonContent = extractJsonFromResponse(response);
            SourcesResponse sourcesResponse = objectMapper.readValue(
                jsonContent, SourcesResponse.class);
            
            // 4. json/sources.json 파일 생성
            Path outputPath = Paths.get("json/sources.json");
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(outputPath.toFile(), sourcesResponse);
            
            contribution.incrementWriteCount(1);
            
        } catch (Exception e) {
            throw new RuntimeException("Source discovery failed", e);
        }
        
        return RepeatStatus.FINISHED;
    }
    
    private String extractJsonFromResponse(String response) {
        // LLM 응답에서 JSON 부분 추출
        // 실제 구현은 응답 형식에 따라 다름
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}") + 1;
        return response.substring(jsonStart, jsonEnd);
    }
}
```

### langchain4j Tasklet 예제

```java
package com.example.batch.tasklet;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class SourceDiscoveryTasklet implements Tasklet {
    
    private final ChatLanguageModel chatModel;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    
    public SourceDiscoveryTasklet(
            ChatLanguageModel chatModel,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public RepeatStatus execute(StepContribution contribution, 
                               ChunkContext chunkContext) throws Exception {
        
        try {
            // 1. 프롬프트 파일 로딩
            Resource promptResource = resourceLoader.getResource(
                "file:prompts/source-discovery-prompt.md");
            String prompt = Files.readString(
                Paths.get(promptResource.getURI()));
            
            // 2. LLM 호출
            String response = chatModel.generate(prompt);
            
            // 3. JSON 응답 파싱
            String jsonContent = extractJsonFromResponse(response);
            SourcesResponse sourcesResponse = objectMapper.readValue(
                jsonContent, SourcesResponse.class);
            
            // 4. json/sources.json 파일 생성
            Path outputPath = Paths.get("json/sources.json");
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(outputPath.toFile(), sourcesResponse);
            
            contribution.incrementWriteCount(1);
            
        } catch (Exception e) {
            throw new RuntimeException("Source discovery failed", e);
        }
        
        return RepeatStatus.FINISHED;
    }
    
    private String extractJsonFromResponse(String response) {
        // LLM 응답에서 JSON 부분 추출
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}") + 1;
        return response.substring(jsonStart, jsonEnd);
    }
}
```

### 설정 파일 예제

#### application.yml (spring-ai)

```yaml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-opus-20240229
          temperature: 0.7
          max-tokens: 4000
```

#### LangChain4j 설정 클래스

```java
package com.example.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {
    
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return AnthropicChatModel.builder()
            .apiKey(System.getenv("ANTHROPIC_API_KEY"))
            .modelName("claude-3-opus-20240229")
            .temperature(0.7)
            .maxTokens(4000)
            .build();
    }
}
```

## 주의사항

### 공식 문서 확인 필요
본 문서의 예제 코드는 일반적인 패턴을 기반으로 작성되었습니다. 실제 구현 시 다음을 확인하세요:

1. **API 버전 확인**
   - Spring AI 및 LangChain4j의 최신 버전 확인
   - API 변경 사항 확인

2. **공식 문서 참조**
   - 모든 예제 코드는 공식 문서와 대조하여 검증
   - 실제 API 시그니처 확인

3. **에러 핸들링**
   - LLM API 호출 실패 시 재시도 로직 구현
   - 타임아웃 설정 확인
   - 비용 관리 (토큰 사용량 모니터링)

4. **보안**
   - API 키는 환경 변수로 관리
   - 민감 정보 로깅 방지

## 결론

Spring Batch에서 AI LLM 통합을 위해 두 프레임워크를 비교 분석한 결과:

1. **spring-ai**는 Spring 생태계 중심 프로젝트에 적합
   - 빠른 프로토타이핑
   - 최소한의 설정
   - Spring 통합 우수

2. **langchain4j**는 고급 기능이 필요한 경우 적합
   - 구조화된 출력 강력
   - 다양한 Provider 지원
   - 체인 기반 워크플로우

**최종 권장사항:** Spring 생태계 중심 프로젝트이므로 **spring-ai**를 우선 검토하고, 구조화된 출력이 복잡하거나 고급 기능이 필요한 경우 **langchain4j**를 고려하는 것을 권장합니다.

