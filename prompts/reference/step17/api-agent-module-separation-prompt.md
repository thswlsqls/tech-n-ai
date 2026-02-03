# api-agent 모듈 분리 구현 프롬프트

## 목적
api-chatbot 모듈에서 `/api/v1/agent/*` 엔드포인트와 Agent Tool 관련 코드를 새로운 api-agent 모듈로 분리한다.

## 배경
- 현재 api-chatbot 모듈에 Chatbot(사용자 대화)과 Agent(자동화 파이프라인) 기능이 혼재
- 관심사 분리를 통해 각 모듈의 책임을 명확히 함
- 독립적인 배포 및 스케일링 가능

## 참조 설계서
- `docs/reference/automation-pipeline-to-ai-agent/phase1-data-pipeline-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase2-langchain4j-tools-design.md`
- `docs/reference/automation-pipeline-to-ai-agent/phase3-agent-integration-design.md`

---

## 구현 작업 목록

### 1. api-agent 모듈 생성

#### 1.1 settings.gradle 수정
```groovy
// api-agent 모듈 추가
include 'api-agent'
project(':api-agent').projectDir = file('api/agent')
```

#### 1.2 api/agent/build.gradle 생성
```groovy
group = 'com.tech.n.ai.api'
version = '0.0.1-SNAPSHOT'
description = 'api-agent'

bootJar.enabled = true
jar.enabled = false

apply from: "${rootDir.absolutePath}/docs.gradle"

dependencies {
    // langchain4j Core + OpenAI
    implementation 'dev.langchain4j:langchain4j:0.35.0'
    implementation 'dev.langchain4j:langchain4j-open-ai:0.35.0'

    // 프로젝트 모듈 의존성
    implementation project(':common-core')
    implementation project(':common-exception')

    // Agent Tool용 클라이언트 모듈
    implementation project(':client-feign')
    implementation project(':client-slack')
    implementation project(':client-scraper')

    // Jsoup (ScraperToolAdapter용 HTML 파싱)
    implementation 'org.jsoup:jsoup:1.17.2'

    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
}
```

#### 1.3 디렉토리 구조 생성
```
api/agent/
├── build.gradle
└── src/main/
    ├── java/com/tech/n/ai/api/agent/
    │   ├── ApiAgentApplication.java
    │   ├── config/
    │   │   ├── ServerConfig.java
    │   │   ├── AgentConfig.java
    │   │   └── AiAgentConfig.java
    │   ├── controller/
    │   │   └── AgentController.java
    │   ├── agent/
    │   │   ├── AiUpdateAgent.java
    │   │   ├── AiUpdateAgentImpl.java
    │   │   ├── AgentExecutionResult.java
    │   │   └── AgentAssistant.java
    │   ├── tool/
    │   │   ├── AiUpdateAgentTools.java
    │   │   ├── dto/
    │   │   │   ├── ToolResult.java
    │   │   │   ├── GitHubReleaseDto.java
    │   │   │   ├── AiUpdateDto.java
    │   │   │   └── ScrapedContentDto.java
    │   │   ├── adapter/
    │   │   │   ├── GitHubToolAdapter.java
    │   │   │   ├── AiUpdateToolAdapter.java
    │   │   │   ├── SlackToolAdapter.java
    │   │   │   └── ScraperToolAdapter.java
    │   │   └── util/
    │   │       └── TextTruncator.java
    │   └── scheduler/
    │       └── AiUpdateAgentScheduler.java
    └── resources/
        ├── application.yml
        └── application-agent-api.yml
```

---

### 2. 파일 이동 및 패키지 변경

#### 2.1 이동할 파일 목록 (api-chatbot → api-agent)

| 원본 경로 | 대상 경로 | 패키지 변경 |
|----------|----------|------------|
| `api/chatbot/.../controller/AgentController.java` | `api/agent/.../controller/AgentController.java` | `api.chatbot` → `api.agent` |
| `api/chatbot/.../agent/AiUpdateAgent.java` | `api/agent/.../agent/AiUpdateAgent.java` | `api.chatbot.agent` → `api.agent.agent` |
| `api/chatbot/.../agent/AiUpdateAgentImpl.java` | `api/agent/.../agent/AiUpdateAgentImpl.java` | `api.chatbot.agent` → `api.agent.agent` |
| `api/chatbot/.../agent/AgentExecutionResult.java` | `api/agent/.../agent/AgentExecutionResult.java` | `api.chatbot.agent` → `api.agent.agent` |
| `api/chatbot/.../agent/AgentAssistant.java` | `api/agent/.../agent/AgentAssistant.java` | `api.chatbot.agent` → `api.agent.agent` |
| `api/chatbot/.../tool/AiUpdateAgentTools.java` | `api/agent/.../tool/AiUpdateAgentTools.java` | `api.chatbot.tool` → `api.agent.tool` |
| `api/chatbot/.../tool/dto/*.java` | `api/agent/.../tool/dto/*.java` | `api.chatbot.tool.dto` → `api.agent.tool.dto` |
| `api/chatbot/.../tool/adapter/*.java` | `api/agent/.../tool/adapter/*.java` | `api.chatbot.tool.adapter` → `api.agent.tool.adapter` |
| `api/chatbot/.../tool/util/TextTruncator.java` | `api/agent/.../tool/util/TextTruncator.java` | `api.chatbot.tool.util` → `api.agent.tool.util` |
| `api/chatbot/.../scheduler/AiUpdateAgentScheduler.java` | `api/agent/.../scheduler/AiUpdateAgentScheduler.java` | `api.chatbot.scheduler` → `api.agent.scheduler` |
| `api/chatbot/.../config/AgentConfig.java` | `api/agent/.../config/AgentConfig.java` | `api.chatbot.config` → `api.agent.config` |
| `api/chatbot/.../config/AiAgentConfig.java` | `api/agent/.../config/AiAgentConfig.java` | `api.chatbot.config` → `api.agent.config` |

#### 2.2 패키지명 변경 규칙
- `com.tech.n.ai.api.chatbot` → `com.tech.n.ai.api.agent`
- 모든 import 문 업데이트

---

### 3. 새로 생성할 파일

#### 3.1 ApiAgentApplication.java
```java
package com.tech.n.ai.api.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class
})
@EnableScheduling
public class ApiAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiAgentApplication.class, args);
    }
}
```

#### 3.2 config/ServerConfig.java
```java
package com.tech.n.ai.api.agent.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "com.tech.n.ai.api.agent",
    "com.tech.n.ai.client.feign",
    "com.tech.n.ai.client.slack",
    "com.tech.n.ai.client.scraper",
    "com.tech.n.ai.common.core",
    "com.tech.n.ai.common.exception"
})
public class ServerConfig {
}
```

#### 3.3 resources/application.yml
```yaml
server:
  port: 8087

spring:
  application:
    name: agent-api
  profiles:
    include:
      - common-core
      - agent-api
      - feign-github
      - feign-internal
      - slack
      - scraper
```

#### 3.4 resources/application-agent-api.yml
```yaml
# Agent용 OpenAI 설정
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      model-name: gpt-4o-mini
      temperature: 0.3
      max-tokens: 4096
      timeout: 120s

# AI Update 내부 API 설정
internal-api:
  ai-update:
    api-key: ${AI_UPDATE_INTERNAL_API_KEY:}

# AI Update Agent 스케줄러 설정
agent:
  scheduler:
    enabled: ${AGENT_SCHEDULER_ENABLED:false}
    cron: "0 0 */6 * * *"
```

---

### 4. api-chatbot 모듈에서 제거

#### 4.1 삭제할 파일
- `api/chatbot/src/main/java/.../controller/AgentController.java`
- `api/chatbot/src/main/java/.../agent/` 디렉토리 전체
- `api/chatbot/src/main/java/.../tool/` 디렉토리 전체
- `api/chatbot/src/main/java/.../scheduler/AiUpdateAgentScheduler.java`
- `api/chatbot/src/main/java/.../config/AgentConfig.java`
- `api/chatbot/src/main/java/.../config/AiAgentConfig.java`

#### 4.2 build.gradle 수정 (선택적)
Agent 전용 의존성 제거 가능 (chatbot에서 사용하지 않는 경우):
```groovy
// 제거 가능한 의존성 (Agent 전용)
// implementation project(':client-feign')  // chatbot에서 다른 용도로 사용 시 유지
// implementation project(':client-slack')
// implementation project(':client-scraper')
// implementation 'org.jsoup:jsoup:1.17.2'
```

#### 4.3 application.yml 수정
profiles.include에서 Agent 전용 프로파일 제거:
```yaml
spring:
  profiles:
    include:
      - common-core
      - kafka
      - api-domain
      - mongodb-domain
      - chatbot-api
      # 제거: feign-github, feign-internal, slack, scraper (Agent 전용)
```

#### 4.4 application-chatbot-api.yml 수정
Agent 관련 설정 제거:
- `langchain4j.open-ai.agent` 섹션 제거
- `internal-api.ai-update` 섹션 제거
- `agent.scheduler` 섹션 제거

---

### 5. Gateway 라우팅 수정

#### 5.1 application.yml 수정
```yaml
spring:
  cloud:
    gateway:
      routes:
        # 기존 라우팅 유지...

        - id: agent-route
          uri: ${gateway.routes.agent.uri:http://localhost:8087}  # chatbot → agent
          predicates:
            - Path=/api/v1/agent/**
```

#### 5.2 application-local.yml 수정
```yaml
gateway:
  routes:
    auth:
      uri: http://localhost:8082
    bookmark:
      uri: http://localhost:8083
    contest:
      uri: http://localhost:8084
    news:
      uri: http://localhost:8085
    chatbot:
      uri: http://localhost:8086
    agent:
      uri: http://localhost:8087  # 신규 추가
```

#### 5.3 다른 환경 설정 파일도 동일하게 수정
- `application-dev.yml`
- `application-beta.yml`
- `application-prod.yml`

---

### 6. 검증 체크리스트

- [ ] api-agent 모듈 빌드 성공: `./gradlew :api-agent:build`
- [ ] api-chatbot 모듈 빌드 성공: `./gradlew :api-chatbot:build`
- [ ] api-gateway 모듈 빌드 성공: `./gradlew :api-gateway:build`
- [ ] 전체 프로젝트 빌드 성공: `./gradlew build`
- [ ] api-agent 애플리케이션 시작 확인 (포트 8087)
- [ ] `/api/v1/agent/run` 엔드포인트 정상 동작
- [ ] Gateway를 통한 Agent 라우팅 정상 동작

---

## 주의사항

1. **패키지명 일관성**: 모든 파일에서 `api.chatbot` → `api.agent`로 변경
2. **import 문 업데이트**: 이동된 클래스 간의 import 경로 수정
3. **설정 파일 분리**: Agent 전용 설정만 application-agent-api.yml로 이동
4. **의존성 최소화**: api-agent는 DB 의존성 없음 (domain-aurora, domain-mongodb 제외)
5. **오버엔지니어링 금지**: 기존 코드 그대로 이동, 불필요한 리팩토링 금지
