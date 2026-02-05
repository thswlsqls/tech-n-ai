# Init Project Rules 작업 상세 분석

## 개요

`init project rules` 명령을 실행하면 Shrimp Task Manager MCP Server는 프로젝트 표준 문서(`shrimp-rules.md`)를 생성하기 위한 작업을 수행합니다.

## 실행 단계

### 1단계: 가이드라인 제공

MCP Server는 AI Agent에게 프로젝트 표준 문서 작성 가이드라인을 제공합니다:

**주요 목적:**
- AI Agent 전용 프로젝트 표준 문서 생성
- 프로젝트별 규칙과 제약사항 정의
- AI의 의사결정 프로세스 가이드 제공

**필수 요구사항:**
- 프로젝트 특정 규칙과 제약사항만 포함 (일반 개발 지식 제외)
- AI가 즉시 참조/수정해야 할 파일 명시
- 다중 파일 조정 요구사항 명확히 표시
- 명령형 언어 사용 (설명적 내용 지양)
- 프로젝트 기능 설명이 아닌 수정/추가 방법 설명
- **재귀적으로** 모든 폴더와 파일 검사

**금지 사항:**
- 일반 개발 지식 포함 금지
- LLM이 이미 알고 있는 일반 개발 지식 포함 금지
- 프로젝트 기능 설명 금지

### 2단계: 사고 과정 (process_thought)

AI Agent는 `process_thought` 도구를 사용하여 다음을 수행합니다:

1. **프로젝트 구조 분석**
   - 모든 폴더와 파일 재귀적 검사
   - 프로젝트 아키텍처 파악
   - 모듈 구조 및 의존성 분석

2. **표준 문서 구조 계획**
   - 다음 섹션 포함 계획:
     - 프로젝트 개요
     - 프로젝트 아키텍처
     - 코드 표준
     - 기능 구현 표준
     - 프레임워크/플러그인/서드파티 라이브러리 사용 표준
     - 워크플로우 표준
     - 주요 파일 상호작용 표준
     - AI 의사결정 표준
     - 금지 사항

3. **프로젝트 특정 규칙 식별**
   - 기술 스택 제약사항 (Java 21, Spring Boot 4.0.1, Gradle Groovy DSL 등)
   - 아키텍처 패턴 (CQRS, MSA 멀티모듈 등)
   - 코딩 컨벤션
   - 파일 구조 규칙

### 3단계: shrimp-rules.md 파일 생성

AI Agent는 프로젝트 루트 디렉토리에 `shrimp-rules.md` 파일을 생성합니다.

**파일 내용 구조:**

```markdown
# Development Guidelines

## Project Overview
- 프로젝트 목적, 기술 스택, 핵심 기능 간략 설명

## Project Architecture
- 주요 디렉토리 구조 및 모듈 분할 설명
- 멀티모듈 구조 (api, batch, common, client, domain)

## Code Standards
- 네이밍 컨벤션
- 포맷팅 요구사항
- 주석 규칙

## Functionality Implementation Standards
- 기능 구현 방법 및 주의사항
- 예: CQRS 패턴 구현 방법, Kafka 동기화 방법

## Framework/Plugin/Third-party Library Usage Standards
- 외부 의존성 사용 표준
- 예: Spring Boot, Spring Batch, OpenFeign 사용법

## Workflow Standards
- 워크플로우 가이드라인
- 워크플로우 다이어그램 또는 데이터 플로우

## Key File Interaction Standards
- 주요 파일 간 상호작용 표준
- 동시에 수정해야 하는 파일 명시
- 예: README.md 수정 시 /docs/zh/README.md도 함께 업데이트

## AI Decision-making Standards
- 모호한 상황 처리 시 의사결정 트리
- 우선순위 판단 기준

## Prohibited Actions
- 명확히 금지된 사항 나열
```

## 현재 프로젝트에 적용되는 주요 규칙

### 기술 스택 제약사항
- **언어**: Java 21 (코틀린 사용 금지)
- **빌드 도구**: Gradle (Groovy DSL, Kotlin DSL 사용 금지)
- **프레임워크**: Spring Boot 4.0.1
- **데이터베이스**: Amazon Aurora MySQL (Command Side), MongoDB (Query Side)
- **메시징**: Apache Kafka
- **캐싱**: Redis

### 아키텍처 패턴
- **CQRS 패턴**: 읽기(MongoDB)와 쓰기(Aurora MySQL) 분리
- **MSA 멀티모듈 구조**: 
  - `api/` - API 서버 모듈
  - `batch/` - 배치 처리 모듈
  - `common/` - 공통 모듈
  - `client/` - 외부 API 클라이언트 모듈
  - `domain/` - 도메인 모듈 (aurora, mongodb)

### 모듈 네이밍 규칙
- 형식: `{parentDir}-{moduleDir}`
- 예: `api/gateway` → `api-gateway`
- 예: `domain/aurora` → `domain-aurora`

### 빌드 검증 규칙
- 각 작업 완료 시 관련 서브 모듈들의 개별 빌드 성공 확인
- 루트 프로젝트에서 전체 빌드 성공 확인
- 컴파일 에러 없음 확인

## 작업 흐름도

```
사용자 입력: "init project rules"
    ↓
MCP Server: 가이드라인 제공
    ↓
AI Agent: process_thought 호출
    ├─ 프로젝트 구조 분석
    ├─ 표준 문서 구조 계획
    └─ 프로젝트 특정 규칙 식별
    ↓
AI Agent: shrimp-rules.md 파일 생성
    ├─ 프로젝트 개요 작성
    ├─ 아키텍처 설명 작성
    ├─ 코드 표준 작성
    ├─ 기능 구현 표준 작성
    ├─ 프레임워크 사용 표준 작성
    ├─ 워크플로우 표준 작성
    ├─ 파일 상호작용 표준 작성
    ├─ AI 의사결정 표준 작성
    └─ 금지 사항 작성
    ↓
완료: shrimp-rules.md 파일 생성됨
```

## 주요 특징

### 1. AI Agent 최적화
- 문서는 Coding Agent AI를 위한 프롬프트로 제공됨
- 프롬프트 최적화를 위해 구조화된 형식 사용 (리스트, 테이블 등)

### 2. 개발 가이드 중심
- 사용법 튜토리얼이 아닌 지속적인 개발을 위한 규칙 제공
- "해야 할 것"과 "하지 말아야 할 것"의 구체적 예시 제공

### 3. 명령형 언어 사용
- 설명적 언어 대신 직접적인 지시사항 사용
- 설명적 내용 최소화

### 4. 구조화된 표현
- 모든 내용은 리스트, 테이블 등 구조화된 형식으로 표현
- AI 파싱 용이성을 위한 형식

### 5. 주요 표시 강조
- 볼드, 경고 마커 등을 사용하여 주요 규칙과 금지 사항 강조

### 6. 일반 지식 제외
- LLM이 이미 알고 있는 일반 개발 지식 포함 금지
- 프로젝트 특정 규칙만 포함

## 업데이트 모드 가이드라인

프로젝트 규칙 업데이트 시:

1. **최소 변경 원칙**: 기존 규칙 유지, 필요한 경우에만 변경
2. **시의성**: 기존 규칙이 여전히 유효한지 확인, 오래된 규칙 수정/제거
3. **완전성**: 모든 폴더와 파일 내용 확인, 새로 추가/수정된 코드에 대한 규칙 보완
4. **모호한 요청 자율 처리**: 구체적 내용 없이 "규칙 업데이트" 요청 시, AI는 자율적으로 코드베이스 분석 후 업데이트 포인트 추론

## 제약사항

- **도구 호출 필수**: AI는 도구를 호출하지 않고 중단하는 것이 금지됨
- **자율 완료**: 지시 수신부터 수정 구현까지 전체 프로세스를 자율적으로 완료해야 함
- **사용자 입력 금지**: 기술적 오류나 해결 불가능한 의존성 충돌이 아닌 이상 사용자 입력 요청 금지

## 출력 결과

최종적으로 프로젝트 루트 디렉토리에 `shrimp-rules.md` 파일이 생성되며, 이 파일은:

1. 이후 모든 작업에서 AI Agent가 참조하는 프로젝트 표준 문서가 됨
2. 작업 계획 수립, 작업 실행, 작업 검증 시 기준이 됨
3. 프로젝트 특정 규칙과 제약사항을 명확히 정의함
4. AI의 의사결정 프로세스를 가이드함

## 검증 기준

프로젝트 규칙 초기화가 성공적으로 완료되었는지 확인:

- `shrimp-rules.md` 파일이 프로젝트 루트에 생성되어야 함
- 파일 내용이 프로젝트 구조와 일치해야 함
- 모든 주요 섹션이 포함되어야 함
- 프로젝트 특정 규칙이 명확히 정의되어야 함
- **빌드 검증**: 프로젝트 규칙 초기화 후 빌드가 정상적으로 동작해야 함 (`./gradlew clean build` 명령이 성공해야 함)

## Shrimp Task Manager MCP Server 작업 상세 분석

### MCP Server 응답 구조

`init_project_rules` 함수 호출 시 MCP Server는 다음 구조의 가이드라인을 반환합니다:

1. **목적 명시**: AI Agent 전용 프로젝트 표준 문서 생성
2. **필수 요구사항 목록**: 프로젝트 특정 규칙만 포함, 재귀적 검사 등
3. **금지 사항 목록**: 일반 개발 지식 포함 금지 등
4. **제안된 문서 구조**: Markdown 형식의 구조 템플릿
5. **내용 가이드라인**: 9개 주요 섹션 정의
6. **AI Agent 행동 지시**: 
   - `process_thought` 도구 호출 필수
   - `analyze_task` 도구 호출 금지
   - 도구 호출 없이 중단 금지
   - 자율적으로 전체 프로세스 완료

### 실제 수행되는 작업 단계별 상세

#### Step 1: MCP Server 가이드라인 수신

**입력**: 사용자 명령 `init project rules`

**MCP Server 응답**:
- 프로젝트 표준 문서 작성 가이드라인 제공
- AI Agent 행동 지시사항 포함
- 문서 구조 템플릿 제공
- 필수/금지 사항 명시

**AI Agent 행동**:
- 가이드라인 내용 파싱
- 다음 단계 계획 수립

#### Step 2: 프로젝트 구조 정보 수집

**사용 도구**: `list_dir`, `read_file`, `glob_file_search`

**수집 정보**:
1. **루트 디렉토리 구조**
   - `list_dir`로 전체 디렉토리 구조 확인
   - 모듈 디렉토리 식별 (api/, batch/, common/, client/, domain/)

2. **빌드 설정 파일**
   - `read_file`: `build.gradle` (루트)
   - `read_file`: `settings.gradle` (모듈 자동 검색 로직 확인)
   - `read_file`: 각 모듈의 `build.gradle` (api/gateway/build.gradle, domain/aurora/build.gradle 등)

3. **프로젝트 설정 파일**
   - `read_file`: `prompts/shrimp-task-prompt.md` (프로젝트 제약사항 확인)
   - `read_file`: `json/sources.json` (프로젝트 데이터 확인)

4. **코드 구조 확인**
   - `list_dir`: 각 모듈의 src 디렉토리 구조 확인
   - 패키지 구조 파악 (com.ebson.shrimp.tm.demo)
   - 도메인 구조 확인 (api/gateway/domain/sample/)
   - Client 구조 확인 (client/feign/domain/sample/)

5. **Config 클래스 확인**
   - `read_file`: `ApiDomainConfig.java` (Profile 기반 설정 확인)
   - `read_file`: `BatchDomainConfig.java` (Profile 기반 설정 확인)

**분석 결과**:
- 모듈 구조: api/gateway, batch/source, common/core, client/feign, domain/aurora
- 모듈 네이밍: `{parentDir}-{moduleDir}` 형식 (settings.gradle 자동 검색)
- 패키지 구조: `com.ebson.shrimp.tm.demo.{module}.{domain}`
- Profile 분리: `@Profile("api-domain")`, `@Profile("batch-domain")`
- 도메인 구조: controller, facade, service, repository/reader, repository/writer
- Client 구조: contract, api, client, mock

#### Step 3: process_thought 도구 호출 (사고 과정)

**도구**: `mcp_shrimp-task-manager_process_thought`

**호출 횟수**: 6회 (thought_number: 1~6)

**각 Thought 단계별 내용**:

**Thought 1/6 - Information Gathering (정보 수집)**
- **Stage**: Information Gathering
- **내용**: 프로젝트 재분석 시작, 전체 구조 재확인 계획
- **Tags**: project-analysis, structure-review, re-initialization
- **결과**: 프로젝트 구조 분석 체크리스트 생성

**Thought 2/6 - Analysis (분석)**
- **Stage**: Analysis
- **내용**: 프로젝트 구조 상세 분석 결과 정리
- **Tags**: module-structure, dependencies, profile-separation
- **결과**: 
  - 모듈 구조 및 네이밍 규칙 확인
  - 의존성 관계 파악 (api-gateway → domain-mariadb)
  - Profile 분리 구조 확인
  - DataSource 분리 구조 확인

**Thought 3/6 - Analysis (분석)**
- **Stage**: Analysis
- **내용**: 코드 패턴 및 구조 규칙 식별
- **Tags**: code-patterns, architecture-patterns, file-structure
- **결과**:
  - API Gateway 도메인 구조 규칙
  - Service 레이어 Reader/Writer 분리 규칙
  - Client Feign 구조 규칙
  - Contract 패턴 규칙
  - Repository 분리 규칙

**Thought 4/6 - Synthesis (종합)**
- **Stage**: Synthesis
- **내용**: 제약사항 및 금지 사항 정리
- **Tags**: constraints, prohibitions, rules
- **결과**:
  - 언어 및 빌드 도구 제약사항
  - 아키텍처 패턴 제약사항
  - 모듈 의존성 제약사항
  - 빌드 검증 필수 사항

**Thought 5/6 - Synthesis (종합)**
- **Stage**: Synthesis
- **내용**: AI 의사결정 기준 및 워크플로우 정리
- **Tags**: decision-making, workflow, verification
- **결과**:
  - 모듈/도메인/Client 추가 워크플로우
  - 빌드 실패 처리 절차
  - 의존성 추가 절차
  - 작업 완료 검증 절차

**Thought 6/6 - Planning (계획)**
- **Stage**: Planning
- **내용**: shrimp-rules.md 파일 생성 계획
- **Tags**: document-structure, content-planning, finalization
- **결과**: 
  - 문서 구조 계획 (10개 주요 섹션)
  - 각 섹션별 포함 내용 계획
  - 작성 원칙 확인 (명령형 언어, 프로젝트 특정 규칙만 포함)

**process_thought 특징**:
- 각 Thought는 `thought_number`, `total_thoughts`, `next_thought_needed`, `stage`, `tags` 파라미터 포함
- `next_thought_needed`가 `false`가 될 때까지 연속 호출
- 각 Thought는 이전 Thought의 결과를 기반으로 진행
- 최종 Thought에서 `next_thought_needed: false`로 설정하여 사고 과정 종료

#### Step 4: 프로젝트 파일 상세 분석

**추가 파일 읽기 작업**:
- `read_file`: 각 모듈의 build.gradle 파일
- `read_file`: Config 클래스 파일 (ApiDomainConfig, BatchDomainConfig)
- `read_file`: 도메인 구조 파일 (Sample 도메인 참고)
- `read_file`: Client 구조 파일 (Sample Client 참고)
- `read_file`: 프롬프트 파일 (제약사항 확인)

**분석 내용**:
1. **의존성 관계 분석**
   - api-gateway → domain-mariadb (실제 모듈명: domain-aurora)
   - domain-aurora → common-core
   - client-feign → common-core

2. **설정 파일 분석**
   - Profile별 설정 파일 분리 확인
   - application-api-domain.yml
   - application-batch-domain.yml
   - application-local.yml, application-dev.yml, application-beta.yml, application-prod.yml

3. **코드 패턴 분석**
   - Reader/Writer 인터페이스 분리 패턴
   - Contract 패턴 (Client 모듈)
   - Profile 기반 Config 분리 패턴
   - DataSource 분리 패턴

#### Step 5: shrimp-rules.md 파일 생성

**도구**: `write` 또는 `mcp_filesystem_write_file`

**생성 위치**: 프로젝트 루트 디렉토리 (`/Users/r00442/Documents/workspace/shrimp-tm-demo/shrimp-rules.md`)

**생성 내용 구조**:
1. **Project Overview**: 목적, 기술 스택, 핵심 기능
2. **Project Architecture**: 모듈 구조, 네이밍 규칙, 의존성, 패키지 구조
3. **Code Standards**: 언어 요구사항, 네이밍 규칙, 파일 구조 표준, 코드 패턴
4. **Functionality Implementation Standards**: CQRS 패턴, Profile 기반 설정, DataSource 분리, JPA/MyBatis 혼용, Spring Batch, Spring REST Docs
5. **Framework/Plugin/Third-party Library Usage Standards**: Spring Boot, Gradle, Spring Cloud, Database, Build Verification
6. **Workflow Standards**: 모듈 추가, 도메인 추가, Client 추가, Entity 추가, 설정 파일 수정
7. **Key File Interaction Standards**: build.gradle, settings.gradle, application.yml, Config Classes, prompts/
8. **AI Decision-making Standards**: 모듈 추가, 빌드 실패 처리, 의존성 추가, Profile 선택, CQRS 패턴, Repository 구현, 작업 완료 검증
9. **Prohibited Actions**: 언어/빌드 도구, 아키텍처, 코드 구조, 빌드/검증, 파일 수정, 문서
10. **Examples**: 올바른 예시와 잘못된 예시

**작성 원칙**:
- 명령형 언어 사용 (직접 지시)
- 프로젝트 특정 규칙만 포함 (일반 개발 지식 제외)
- 구조화된 형식 (리스트, 테이블 등)
- 볼드, 경고 마커로 주요 규칙 강조

### MCP Server와 AI Agent 간 상호작용

#### 1. 초기 가이드라인 제공
```
사용자: "init project rules"
    ↓
MCP Server: init_project_rules 함수 호출
    ↓
응답: Project Standards Initialization Guide 제공
    - 목적, 필수 요구사항, 금지 사항
    - 문서 구조 제안
    - AI Agent 행동 지시
```

#### 2. AI Agent 자율 작업 수행
```
AI Agent: 가이드라인 수신
    ↓
AI Agent: 프로젝트 구조 정보 수집
    - list_dir: 디렉토리 구조 확인
    - read_file: 빌드 설정 파일 읽기
    - read_file: 코드 구조 파일 읽기
    ↓
AI Agent: process_thought 호출 (6회)
    - Thought 1: 정보 수집 계획
    - Thought 2: 구조 분석
    - Thought 3: 패턴 식별
    - Thought 4: 제약사항 정리
    - Thought 5: 워크플로우 정리
    - Thought 6: 문서 생성 계획
    ↓
AI Agent: shrimp-rules.md 파일 생성
    - write 도구 사용
    - 프로젝트 루트에 파일 생성
```

### 작업 수행 시간 및 리소스

**예상 작업 시간**:
- 가이드라인 수신: 즉시
- 프로젝트 구조 분석: 1-2분 (파일 읽기 작업)
- process_thought: 2-3분 (6회 호출)
- 파일 생성: 즉시

**사용되는 도구**:
1. `mcp_shrimp-task-manager_init_project_rules`: 초기 가이드라인 제공
2. `list_dir`: 디렉토리 구조 확인
3. `read_file`: 파일 내용 읽기
4. `glob_file_search`: 파일 검색
5. `mcp_shrimp-task-manager_process_thought`: 사고 과정 (6회)
6. `write`: 최종 파일 생성

**생성되는 파일**:
- `shrimp-rules.md`: 프로젝트 루트 디렉토리 (약 400-500줄)

### 주의사항 및 제약사항

#### MCP Server 제약사항
- **analyze_task 호출 금지**: 가이드라인에서 명시적으로 금지
- **도구 호출 필수**: 도구를 호출하지 않고 중단하는 것 금지
- **자율 완료**: 전체 프로세스를 자율적으로 완료해야 함
- **사용자 입력 금지**: 기술적 오류나 해결 불가능한 의존성 충돌이 아닌 이상 사용자 입력 요청 금지

#### AI Agent 행동 규칙
- **재귀적 검사**: 모든 폴더와 파일을 재귀적으로 검사해야 함
- **프로젝트 특정 규칙만 포함**: 일반 개발 지식 포함 금지
- **명령형 언어 사용**: 설명적 언어 대신 직접 지시사항 사용
- **구조화된 표현**: 리스트, 테이블 등 구조화된 형식 사용

### 실제 실행 예시

#### 실행 명령
```
init project rules
```

#### MCP Server 응답 (요약)
```
Please use the "process_thought" tool to consider the following issues

# Project Standards Initialization Guide
...
[가이드라인 내용]
...
**[AI Agent Action]** Now start calling the "process_thought" tool...
**[AI Agent Action]** After completing the thought process, immediately edit the shrimp-rules.md file...
**[AI Agent Action]** Strictly forbidden not to call tools...
```

#### AI Agent 실제 수행 작업
1. ✅ `list_dir` 호출: 프로젝트 루트 디렉토리 구조 확인
2. ✅ `read_file` 호출: build.gradle, settings.gradle 읽기
3. ✅ `read_file` 호출: 각 모듈의 build.gradle 읽기
4. ✅ `read_file` 호출: Config 클래스 읽기
5. ✅ `read_file` 호출: 프롬프트 파일 읽기
6. ✅ `mcp_shrimp-task-manager_process_thought` 호출 (6회)
   - Thought 1: Information Gathering
   - Thought 2: Analysis (구조 분석)
   - Thought 3: Analysis (패턴 식별)
   - Thought 4: Synthesis (제약사항)
   - Thought 5: Synthesis (워크플로우)
   - Thought 6: Planning (문서 계획)
7. ✅ `write` 호출: shrimp-rules.md 파일 생성

### 검증 및 완료 확인

**완료 확인 항목**:
1. ✅ `shrimp-rules.md` 파일이 프로젝트 루트에 생성됨
2. ✅ 파일 내용이 프로젝트 구조와 일치함
3. ✅ 모든 주요 섹션이 포함됨 (10개 섹션)
4. ✅ 프로젝트 특정 규칙이 명확히 정의됨
5. ⚠️ 빌드 검증: 기존 프로젝트 설정 문제로 인해 빌드 실패 (별도 수정 필요)

**생성된 파일 검증**:
- 파일 크기: 약 400-500줄
- 형식: Markdown
- 구조: 10개 주요 섹션 포함
- 내용: 프로젝트 특정 규칙만 포함, 일반 개발 지식 제외

