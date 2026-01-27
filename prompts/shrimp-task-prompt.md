# Shrimp Task Manager 실행 프롬프트

## 📋 프롬프트 사용 가이드

**이 프롬프트의 목적**: 개발자 대회 정보와 최신 IT 테크 뉴스를 제공하는 API Server 구축 프로젝트의 단계별 실행 가이드

**프롬프트 구조**:
1. **프로젝트 초기화**: 프로젝트 규칙 설정
2. **작업 계획 수립**: 전체 프로젝트 요구사항 및 계획
3. **단계별 작업 실행**: 구체적인 작업 단계별 가이드

**사용 방법**:
- 각 섹션의 지시사항을 순차적으로 따라 실행
- 각 작업 단계는 독립적으로 실행 가능하지만, 의존성이 있는 경우 명시됨
- 검증 기준을 확인하여 작업 완료 여부를 판단

**역할 정의**:
- **개발자**: 코드 구현 및 테스트 작성
- **인프라 엔지니어**: AWS 인프라 설정 및 관리
- **검증자**: 코드 리뷰 및 통합 테스트 검증

**제약사항**:
- 모든 데이터베이스 작업은 Amazon Aurora MySQL을 사용
- 모든 쓰기 작업은 Command Side (Aurora)에서 수행
- 모든 읽기 작업은 Query Side (MongoDB Atlas 클라우드 서비스)에서 수행
- 모든 쓰기 작업 후 Kafka 이벤트 발행 필수
- **코틀린 사용 금지**: 모든 코드는 Java로 작성 (build.gradle 사용, build.gradle.kts 사용 금지)
- **Gradle 빌드 시스템**: Kotlin DSL 대신 Groovy DSL 사용

**기술 스택 제약사항**:
- **언어**: Java 21 (코틀린 사용 금지)
- **빌드 도구**: Gradle (Groovy DSL)
- **프레임워크**: Spring Boot 4.0.1
- **데이터베이스**: Amazon Aurora MySQL (Command Side), MongoDB Atlas 클라우드 서비스 (Query Side)
- **메시징**: Apache Kafka
- **캐싱**: Redis

## 프로젝트 초기화

**역할**: 프로젝트 규칙 및 표준 설정
**책임**: 
- 프로젝트 규칙 초기화
- 코딩 표준 설정
- 아키텍처 원칙 정의

**검증 기준**: 프로젝트 규칙이 정상적으로 초기화되어야 함
- **빌드 검증**: 프로젝트 규칙 초기화 후 빌드가 정상적으로 동작해야 함 (`./gradlew clean build` 명령이 성공해야 함)

먼저 프로젝트 규칙을 초기화하세요:

```
init project rules
```

## 작업 계획 수립

**역할**: 전체 프로젝트 계획 수립 및 작업 분해
**책임**: 
- 프로젝트 요구사항 분석
- 작업 단계 정의
- 의존성 파악
- 검증 기준 수립

**입력 요구사항**:
- 프로젝트 목표 및 범위
- 기술 스택 선정
- 제약사항 및 제약 조건

**출력 결과**:
- 작업 목록 (Task List)
- 작업 간 의존성 그래프
- 각 작업의 검증 기준
- 예상 소요 시간

**검증 기준**: 
- 모든 작업이 명확히 정의되어야 함
- 작업 간 의존성이 명확해야 함
- 각 작업의 완료 기준이 명확해야 함
- 작업 목록이 실행 가능한 단위로 분해되어야 함
- **빌드 검증**: 각 작업 완료 시 관련 서브 모듈들의 개별 빌드가 성공해야 함 (`./gradlew :{module-name}:build`)
- **빌드 검증**: 루트 프로젝트에서 전체 빌드가 성공해야 함 (`./gradlew clean build`)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**에러 처리**:
- 작업 계획 수립 실패 시: 요구사항 재분석 및 단계별 접근
- 의존성 충돌 시: 의존성 그래프 재검토 및 우선순위 조정

다음 프롬프트들을 사용하여 작업을 계획하세요:

### 1단계: 프로젝트 구조 및 설계서 생성

**단계 번호**: 1단계
**의존성**: 없음 (최우선 작업)
**다음 단계**: 2단계 (API 설계) 또는 3단계 (Domain 모듈 구현)

```
plan task: MSA 멀티모듈 프로젝트 구조 검증 및 데이터베이스 설계서 생성

참고 파일: docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표), json/sources.json, 작업 계획의 모든 요구사항

**역할**: 프로젝트 아키텍트 및 데이터베이스 설계자
**책임**: 
- 현재 멀티모듈 프로젝트 구조 검증
- CQRS 패턴 기반 데이터베이스 스키마 설계
- 설계서 문서화 및 검증

**작업 요약**:
현재 MSA 멀티모듈 프로젝트 구조를 검증하고, CQRS 패턴에 맞춰 Amazon Aurora MySQL(Command Side)과 MongoDB Atlas(Query Side) 데이터베이스 설계서를 생성합니다. 기존 Gradle 멀티모듈 설정 검증, 모듈 간 의존성 검증, 스키마 설계 및 문서화를 포함합니다.

**초기 해결 방안**:
1. 현재 Gradle 멀티모듈 프로젝트 구조 검증 (settings.gradle 자동 모듈 검색 로직 확인)
2. CQRS 패턴에 맞춰 Command Side(Aurora)와 Query Side(MongoDB Atlas) 스키마 분리 설계
3. 공식 문서 기반으로 설계서 작성 (docs/phase1/2. mongodb-schema-design.md, docs/phase1/3. aurora-schema-design.md)
4. 모듈 간 의존성 방향 검증 및 명확화 (API → Domain → Common → Client)

**핵심 제한사항 (절대 준수 필수)**:
1. **공식 개발문서만 참고**: Gradle, Spring Boot, Amazon Aurora MySQL, MongoDB Atlas 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 단일 구현체를 위한 인터페이스, 불필요한 추상화, "나중을 위해" 추가하는 코드 모두 금지.
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.

작업 내용:
1. 멀티모듈 프로젝트 구조 검증
   **주의**: 현재 프로젝트는 이미 멀티모듈 구조가 설정되어 있습니다. 이 작업은 기존 구조를 검증하고 필요 시 보완하는 것입니다.
   
   - 현재 Gradle 멀티모듈 프로젝트 구조 검증
     역할: 기존 멀티모듈 프로젝트 구조 검증
     책임: 현재 모듈 구조 확인 및 검증, 요구사항과의 일치 여부 확인
     검증 대상 모듈 구조: domain/ (domain-aurora, domain-mongodb), common/ (common-core, common-security, common-kafka, common-exception), client/ (client-feign, client-rss, client-scraper, client-slack), batch/ (batch-source), api/ (api-contest, api-news, api-auth, api-archive)
     검증 기준: 모든 필수 모듈이 존재하는지 확인, 모듈 구조가 요구사항과 일치하는지 확인
   
   - build.gradle 루트 설정 검증
     역할: 루트 프로젝트 빌드 설정 검증
     책임: 공통 의존성 관리 확인, 플러그인 버전 관리 확인, 공통 설정 확인
     참고 파일: `build.gradle` (루트)
     검증 기준: Spring Boot 버전이 4.0.1인지 확인, Java 버전이 21인지 확인, 공통 의존성이 적절히 관리되는지 확인
     참고 예제 (이미 설정되어 있는지 확인):
       ```gradle
       // build.gradle (루트)
       plugins {
           id 'java'
           id 'org.springframework.boot' version '4.0.1'
           id 'io.spring.dependency-management' version '1.1.0'
       }
       
       subprojects {
           apply plugin: 'java'
           apply plugin: 'io.spring.dependency-management'
           
           java {
               sourceCompatibility = '21'
           }
       }
       ```
   
   - settings.gradle 모듈 등록 검증 (자동 검색 방식)
     역할: 멀티모듈 프로젝트 구조 정의 및 자동 모듈 검색 로직 검증
     책임: 모듈 자동 검색 로직 확인, 모듈 네이밍 규칙 적용 여부 확인, src 디렉토리 기반 자동 등록 로직 확인
     모듈 네이밍 규칙: 형식 `{parentDir}-{moduleDir}` (예: `api/gateway` → `api-gateway`, `domain/aurora` → `domain-aurora`)
     참고 파일: `settings.gradle`
     검증 기준: 자동 모듈 검색 로직이 정상 작동하는지 확인, 모듈 네이밍이 일관되게 적용되는지 확인, src 디렉토리 존재 여부로 모듈 여부 판단 로직 확인
     참고 예제 (이미 설정되어 있는지 확인):
       ```gradle
       // settings.gradle
       rootProject.name = 'shrimp-tm-demo'
       
       // 자동 모듈 검색 함수 호출
       [
           "api", "batch", "common", "client", "domain"
       ].forEach(module -> {
           searchModules(file("${rootDir.absolutePath}/${module}") as File)
       })
       
       // 자동 모듈 검색 함수 정의
       private void searchModules(final File moduleDir, final String parentStr = "") {
           if (moduleDir.list()?.contains("src")) {
               // src 디렉토리가 존재하면 모듈로 등록
               def indexOf = parentStr.indexOf('-')
               def baseDir = indexOf < 0 ? parentStr : parentStr.substring(indexOf + 1)
               def moduleName = baseDir.empty ? moduleDir.name : "${baseDir}-${moduleDir.name}"
               include moduleName
               project(":${moduleName}").projectDir = moduleDir as File
           } else {
               // src 디렉토리가 없으면 하위 디렉토리 탐색
               for(File subModule in moduleDir.listFiles()) {
                   searchModules(subModule, parentStr.empty ? moduleDir.name : "${parentStr}-${moduleDir.name}")
               }
           }
       }
       ```
   
   - 모듈 간 의존성 검증
     역할: 모듈 간 의존성 검증 및 필요 시 보완
     책임: 의존성 방향 검증 (API → Domain → Common → Client), 순환 의존성 확인, 누락된 의존성 식별
     검증 기준: 모든 의존성이 명확히 정의되어 있는지 확인, 순환 의존성이 없는지 확인, 의존성 방향이 올바른지 확인
     참고 예제 (각 모듈의 build.gradle에서 확인):
       ```gradle
       // api/api-contest/build.gradle
       dependencies {
           implementation project(':domain:domain-aurora')
           implementation project(':domain:domain-mongodb')
           implementation project(':common:common-core')
           implementation project(':common:common-security')
           implementation project(':client:client-feign')
       }
       ```
   
   - 공통 Gradle 설정 파일 검증 (docs.gradle, jpa.gradle)
     역할: 공통 Gradle 설정 파일의 라이브러리 버전 검증 및 일관성 확인
     책임: docs.gradle의 라이브러리 버전이 프로젝트의 Java, Spring Boot, Gradle 버전과 호환되는지 확인, jpa.gradle의 라이브러리 버전이 프로젝트의 Java, Spring Boot, Gradle 버전과 호환되는지 확인, 버전 불일치 시 수정 지시
     참고 파일: `docs.gradle`, `jpa.gradle` (루트)
     프로젝트 버전 기준: Java 21, Spring Boot 4.0.1, Gradle 프로젝트에서 사용 중인 버전
     검증 기준: docs.gradle의 Spring REST Docs 관련 라이브러리 버전이 Spring Boot 4.0.1과 호환되는지 확인, docs.gradle의 Asciidoctor 플러그인 버전이 Gradle 버전과 호환되는지 확인, jpa.gradle의 JPA/Hibernate 관련 라이브러리 버전이 Spring Boot 4.0.1과 호환되는지 확인, jpa.gradle의 QueryDSL 버전이 Java 21과 호환되는지 확인, jpa.gradle의 HikariCP 버전이 Spring Boot 4.0.1과 호환되는지 확인, jpa.gradle의 AWS MySQL JDBC 드라이버 버전이 최신 안정 버전인지 확인
     수정 지시: 버전 불일치 발견 시 공식 문서를 참고하여 호환되는 버전으로 수정 지시

2. MongoDB Atlas 도큐먼트 설계서 작성 (docs/phase1/2. mongodb-schema-design.md)
   
   **참고 문서 (필수)**:
   - docs/mongodb-atlas-schema-design-best-practices.md
   - json/mongodb-atlas-best-practices.json
   
   **역할**: Query Side(읽기 전용) 도큐먼트 설계 및 문서화
   **책임**: 
   - 읽기 최적화된 도큐먼트 구조 설계
   - 인덱스 전략 수립 (ESR 규칙 준수)
   - CQRS 패턴에 맞는 읽기 모델 최적화
   - MongoDB Atlas 베스트 프랙티스 준수
   
   **설계 원칙 (베스트 프랙티스 준수)**:
   - **Primary Key (_id)**: 모든 도큐먼트의 _id는 ObjectId 자료형을 사용하며, MongoDB가 자동으로 생성합니다. ObjectId는 시간 기반 정렬이 가능하고 분산 환경에서 고유성을 보장합니다.
   - **도큐먼트 크기 제한**: 16MB 제한 고려, 필요한 경우 GridFS 사용
   - **Embedded vs Referenced**: 자주 함께 조회되는 데이터는 Embedded, 무제한 관계는 Referenced
   - **읽기 최적화**: 읽기 전용 구조로 설계, 자주 함께 조회되는 데이터를 하나의 도큐먼트에 포함하여 조인 회피
   - **프로젝션 활용**: 필요한 필드만 선택하여 네트워크 트래픽 최소화
   - **Read Preference**: secondaryPreferred 또는 secondary 사용하여 기본 노드 부하 감소
   - **외래 키 참조**: 다른 도큐먼트를 참조할 때는 ObjectId를 사용합니다. 예: ContestDocument.sourceId는 SourcesDocument._id를 ObjectId로 참조
   
   **인덱스 설계 전략 (ESR 규칙 준수)**:
   - **ESR 규칙**: Equality(등가) → Sort(정렬) → Range(범위) 순서로 복합 인덱스 필드 배치
   - **부분 인덱스**: 특정 조건의 문서만 자주 조회하는 경우 부분 인덱스 사용
   - **TTL 인덱스**: 임시 데이터(로그, 세션 등)는 TTL 인덱스로 자동 삭제
   - **인덱스 선택성**: 선택성이 높은 필드를 인덱스에 포함
   
   **도큐먼트 설계**:
   - **SourcesDocument**: 정보 출처 (json/sources.json 기반, 읽기 최적화)
     - 설계 원칙: LLM을 통해 수집한 신뢰할 수 있는 출처 정보를 저장, 다른 도큐먼트에서 참조
     - 필드 구조:
       - _id (ObjectId, PK, 자동 생성)
       - name (String) - 출처 이름 (예: "Codeforces API", "Hacker News API")
       - type (String) - 출처 타입 (예: "API", "RSS", "Web Scraping")
       - category (String) - 카테고리 (예: "개발자 대회 정보", "최신 IT 테크 뉴스 정보")
       - url (String) - 출처 URL
       - apiEndpoint (String, nullable) - API 엔드포인트 URL
       - rssFeedUrl (String, nullable) - RSS 피드 URL
       - description (String) - 출처 설명
       - priority (Integer) - 우선순위 (1, 2, 3)
       - reliabilityScore (Integer) - 신뢰성 점수
       - accessibilityScore (Integer) - 접근성 점수
       - dataQualityScore (Integer) - 데이터 품질 점수
       - legalEthicalScore (Integer) - 법적/윤리적 점수
       - totalScore (Integer) - 총점
       - authenticationRequired (Boolean) - 인증 필요 여부
       - authenticationMethod (String, nullable) - 인증 방법
       - rateLimit (String, nullable) - Rate limit 정보
       - documentationUrl (String, nullable) - 문서 URL
       - updateFrequency (String) - 업데이트 빈도
       - dataFormat (String) - 데이터 형식 (JSON, RSS/XML, HTML 등)
       - enabled (Boolean) - 활성화 여부
       - createdAt (Date) - 생성 시점
       - updatedAt (Date) - 수정 시점
     - 인덱스 전략 (ESR 규칙 준수): name (단일 필드 인덱스, UNIQUE) - 출처 이름으로 조회, category + priority (복합 인덱스) - 카테고리별 우선순위 조회, type + enabled (복합 인덱스) - 타입별 활성화된 출처 조회, priority (단일 필드 인덱스) - 우선순위별 조회
     - 프로젝션 최적화: 목록 조회 시 name, type, category, priority, enabled만 프로젝션
     - 참고: json/sources.json 파일의 구조를 기반으로 설계
   
   - **ContestDocument**: 대회 정보 (읽기 최적화)
     - 설계 원칙: 자주 함께 조회되는 필드를 하나의 도큐먼트에 포함
     - 필드 구조:
       - _id (ObjectId, PK, 자동 생성)
       - sourceId (ObjectId, FK to SourcesDocument._id) - 출처 참조
       - title (String) - 대회 제목
       - startDate (Date) - 시작 날짜
       - endDate (Date, nullable) - 종료 날짜 
       - status (String) - 상태 (예: "UPCOMING", "ONGOING", "ENDED") 
       - description (String, nullable) - 설명 
       - url (String, nullable) - 대회 URL 
       - metadata (Object, nullable) - 추가 메타데이터 
       - createdAt (Date) - 생성 시점 
       - updatedAt (Date) - 수정 시점 
     - 인덱스 전략 (ESR 규칙 준수): sourceId + startDate (복합 인덱스, ESR 규칙 준수) 
      - 출처별 시작일순 조회, endDate (단일 필드 인덱스) 
      - 종료일 범위 쿼리용, status + startDate (복합 인덱스, 부분 인덱스) 
      - 활성 대회만 인덱싱, sourceId (외래 키 인덱스) - 출처별 조회
     - 프로젝션 최적화: 목록 조회 시 필요한 필드만 프로젝션
   
   - **NewsArticleDocument**: 뉴스 기사 (읽기 최적화)
     - 설계 원칙: 읽기 전용 구조, 자주 함께 조회되는 메타데이터 포함
     - 필드 구조:
       - _id (ObjectId, PK, 자동 생성)
       - sourceId (ObjectId, FK to SourcesDocument._id) - 출처 참조
       - title (String) - 기사 제목
       - content (String, nullable) - 기사 내용
       - summary (String, nullable) - 요약
       - publishedAt (Date) - 발행 시점
       - url (String, nullable) - 기사 URL
       - author (String, nullable) - 작성자
       - metadata (Object, nullable) - 추가 메타데이터
       - createdAt (Date) - 생성 시점
       - updatedAt (Date) - 수정 시점
     - 인덱스 전략 (ESR 규칙 준수): sourceId + publishedAt (복합 인덱스, ESR 규칙 준수) 
      - 출처별 발행일순 조회, publishedAt (TTL 인덱스, 선택적) 
      - 오래된 기사 자동 삭제, sourceId (외래 키 인덱스) 
      - 출처별 조회
     - 프로젝션 최적화: 목록 조회 시 제목, 요약, 메타데이터만 프로젝션
   
   - **ArchiveDocument**: 사용자 아카이브 (읽기 최적화, 사용자별 인덱스)
     - 설계 원칙: 사용자별 조회 패턴에 최적화, Embedded 패턴으로 관련 정보 포함
     - 필드 구조:
       - _id (ObjectId, PK, 자동 생성)
       - userId (String) - 사용자 ID (Aurora MySQL의 User.id를 문자열로 저장)
       - itemType (String) - 아이템 타입 (예: "CONTEST", "NEWS_ARTICLE")
       - itemId (ObjectId) - 아이템 ID (ContestDocument._id 또는 NewsArticleDocument._id)
       - itemTitle (String) - 아이템 제목 (비정규화, 조인 회피)
       - itemSummary (String, nullable) - 아이템 요약 (비정규화)
       - tag (String, nullable) - 태그
       - memo (String, nullable) - 메모
       - archivedAt (Date) - 아카이브 시점
       - createdAt (Date) - 생성 시점
       - updatedAt (Date) - 수정 시점
     - 인덱스 전략 (ESR 규칙 준수): userId + createdAt (복합 인덱스, ESR 규칙 준수) 
      - 사용자별 생성일순 조회, userId + itemType + createdAt (복합 인덱스) 
      - 사용자별 타입별 생성일순 조회, userId + itemType + itemId (복합 인덱스) 
      - 중복 방지, itemId (단일 필드 인덱스) 
      - 아이템별 조회
     - 프로젝션 최적화: 사용자별 목록 조회 시 필요한 필드만 프로젝션
     - 참고: itemId는 MongoDB Atlas의 ContestDocument 또는 NewsArticleDocument의 _id 값을 참조
   
   - **UserProfileDocument**: 사용자 프로필 (읽기 최적화, 통계 정보 포함)
     - 설계 원칙: 읽기 전용 구조, 통계 정보를 비정규화하여 포함
     - 필드 구조:
       - _id (ObjectId, PK, 자동 생성)
       - userId (String, UNIQUE) - 사용자 ID (Aurora MySQL의 User.id를 문자열로 저장)
       - username (String, UNIQUE) - 사용자명
       - email (String, UNIQUE) - 이메일
       - profileImageUrl (String, nullable) - 프로필 이미지 URL
       - statistics (Object) - 통계 정보 (비정규화)
         - archiveCount (Integer) - 아카이브 수
         - lastActivityAt (Date, nullable) - 최근 활동 시점
       - createdAt (Date) - 생성 시점
       - updatedAt (Date) - 수정 시점
     - 인덱스 전략: userId (단일 필드 인덱스, UNIQUE) - 사용자 ID로 조회, username (단일 필드 인덱스, UNIQUE) - 사용자명으로 조회, email (단일 필드 인덱스, UNIQUE) - 이메일로 조회
     - 비정규화: 통계 정보(아카이브 수, 최근 활동 등)를 도큐먼트에 포함하여 조인 회피
   
   - **ExceptionLogDocument**: 예외 로깅 (CQRS 패턴의 예외 로깅 정책 구현)
     - 역할: 모든 예외 로그 저장 (읽기/쓰기 예외 구분)
     - 책임: 읽기 예외와 쓰기 예외를 구분하여 저장 (source 필드: "READ" 또는 "WRITE"), 예외 발생 시점, 스택 트레이스, 컨텍스트 정보 저장, 예외 로깅 실패 시 메인 로직에 영향을 주지 않도록 비동기 처리
     - 필드 구조:
       - _id (ObjectId, PK)
       - source (String, "READ" 또는 "WRITE") - 읽기/쓰기 구분
       - exceptionType (String) - 예외 타입
       - exceptionMessage (String) - 예외 메시지
       - stackTrace (String) - 스택 트레이스
       - context (Object) - 컨텍스트 정보 (요청 URL, 사용자 ID, 파라미터 등)
       - occurredAt (Date) - 발생 시점
       - severity (String) - 심각도 (ERROR, WARN, INFO)
     - 인덱스 전략 (ESR 규칙 준수): source + occurredAt (복합 인덱스) 
      - 읽기/쓰기별 시간순 조회 (Equality + Sort), exceptionType + occurredAt (복합 인덱스) 
      - 예외 타입별 분석 (Equality + Sort), occurredAt (TTL 인덱스, 90일) 
      - 오래된 로그 자동 삭제
     - 참고: docs/reference/shrimp-task-prompts-final-goal.md의 "예외 로깅 정책" 섹션
   
   **각 도큐먼트 포함 내용**:
   - _id 필드: ObjectId 자료형, MongoDB가 자동 생성
   - 필드 타입 및 BSON 데이터 타입 명시
   - 외래 키 참조: 다른 도큐먼트 참조 시 ObjectId 사용 (예: sourceId는 SourcesDocument._id를 ObjectId로 참조)
   - 인덱스 전략 (ESR 규칙 준수 여부 포함)
   - 프로젝션 최적화 전략
   - 샘플 데이터 예제
   - 도큐먼트 크기 예상치 (16MB 제한 고려)
   - Embedded vs Referenced 패턴 선택 근거
   
   **CQRS 패턴 적용 고려사항**:
   - **Read Preference**: secondaryPreferred 또는 secondary 사용
   - **Read Concern**: local 또는 majority 사용 (강한 일관성 필요한 경우 majority)
   - **읽기 모델 최적화**: 데이터 비정규화, 자주 함께 조회되는 데이터 포함
   - **프로젝션 활용**: 필요한 필드만 선택하여 네트워크 트래픽 최소화
   - **최종 일관성**: Query Side는 최종 일관성을 허용하여 성능 최적화
   - **SourcesDocument 참조**: ContestDocument와 NewsArticleDocument는 sourceId를 통해 SourcesDocument를 참조합니다. 출처 정보는 자주 변경되지 않으므로 Referenced 패턴을 사용하며, 필요한 경우 $lookup을 통해 조인합니다.

3. Amazon Aurora MySQL 테이블 설계서 작성 (docs/phase1/3. aurora-schema-design.md)
   
   **참고 문서 (필수)**:
   - docs/aurora-mysql-schema-design-best-practices.md
   - json/aurora-mysql-best-practices.json
   
   **역할**: Command Side 데이터베이스 스키마 설계 및 문서화
   **책임**: 
   - 모든 테이블 스키마 정의
   - 인덱스 전략 수립 (CQRS 패턴에 맞는 최적화)
   - 관계 무결성 보장
   - 성능 최적화 전략 수립
   - Aurora MySQL 특화 최적화 적용
   
   **검증 기준**:
   - 모든 테이블이 정규화되어야 함 (최소 3NF, Command Side는 높은 정규화)
   - 모든 외래 키 관계가 명확히 정의되어야 함
   - 인덱스가 쿼리 패턴에 최적화되어야 함 (Command Side: 최소화, Query Side: 최적화)
   - Aurora 특화 최적화 전략이 포함되어야 함
   - **TSID Primary Key 전략**: 모든 테이블의 Primary Key는 TSID (Time-Sorted Unique Identifier) 방식 사용
   - **빌드 검증**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   **테이블 설계 원칙 (베스트 프랙티스 준수)**:
   
   **1.1 Primary Key 설계 (TSID 기반)**:
   - TSID를 Primary Key로 사용하는 이유: 인덱스 효율성 (TSID는 시간 순서대로 생성되므로, InnoDB의 B-Tree 클러스터드 인덱스에서 페이지 분할을 최소화하여 인덱스 효율성을 높입니다), 쓰기 성능 향상 (순차적인 키 값은 인덱스의 균형을 유지하고 삽입 성능을 향상시킵니다. Aurora MySQL의 빠른 입력(Fast Insert) 기능과 함께 사용 시 더욱 효과적입니다), 분산 시스템 호환성 (TSID는 분산 환경에서 고유성과 순서를 보장하므로, 샤딩 키로 활용하여 데이터 분산을 균등하게 할 수 있습니다), 쿼리 최적화 (시간 기반 정렬 특성으로 인해 시간 범위 쿼리에서 파티션 프루닝이 용이합니다)
   - TSID와 클러스터드 인덱스의 관계: InnoDB는 Primary Key를 기반으로 클러스터드 인덱스를 생성합니다. TSID를 Primary Key로 사용하면 데이터의 물리적 정렬이 시간 순서대로 유지됩니다. 클러스터드 인덱스의 리프 노드에 실제 데이터가 저장되므로, Primary Key로 조회 시 추가 인덱스 조회가 불필요합니다. 순차 INSERT 작업 시 페이지 분할이 최소화되어 성능이 향상됩니다.
   - TSID 생성 전략: 애플리케이션 레벨 생성 권장 (TSID는 애플리케이션 레벨에서 생성하는 것을 권장합니다. 데이터베이스 레벨에서 생성 시 트랜잭션 경계에서 지연이 발생할 수 있습니다), 컬럼 타입 `BIGINT UNSIGNED NOT NULL PRIMARY KEY` 사용, 분산 환경 주의사항 (TSID 생성 시 노드 ID를 포함하여 고유성을 보장하고, NTP를 사용하여 시간 동기화를 유지해야 합니다)
   - 구현 방법: JPA Entity의 Primary Key 필드에 `@Tsid` 커스텀 어노테이션 적용, `@GeneratedValue(generator = "tsid-generator")`와 `@GenericGenerator` 사용, TSID 생성기 (`TsidGenerator`)를 통한 자동 ID 생성
   - TSID 커스텀 어노테이션: `@Tsid` (domain-aurora 모듈에 구현)
   - TSID 생성기: `TsidGenerator` (domain-aurora 모듈에 구현)
   - 의존성: `io.hypersistence:hypersistence-tsid:5.0.0` (또는 최신 버전)
   - 데이터 추가 규칙: ✅ 허용 - JPA Entity를 통한 데이터 추가 (`entityRepository.save(entity)`), ❌ 금지 - 직접 SQL INSERT 문 사용 (`INSERT INTO ...`), ❌ 금지 - MyBatis를 통한 INSERT 쿼리 작성, ✅ 허용 - JPA의 `@PrePersist` 또는 `@PreUpdate`를 통한 추가 로직
   - 참고: docs/reference/shrimp-task-prompts-final-goal.md의 "TSID Primary Key 전략 구현" 섹션, docs/aurora-mysql-schema-design-best-practices.md의 "1.1 Primary Key 설계 (TSID 기반)" 섹션
   
   **1.2 테이블 정규화 전략**:
   - CQRS 패턴에서의 정규화 수준 결정: Command Side 정규화 전략 (높은 정규화) - Command Side는 쓰기 작업에 최적화되어야 하므로, 높은 정규화 수준(최소 3NF)을 유지합니다. 데이터 일관성을 보장하고 중복을 최소화하며, 트랜잭션 경계를 최소화하여 쓰기 성능을 향상시킵니다. Query Side 비정규화 전략 - Query Side는 읽기 작업에 최적화되어야 하므로, 비정규화를 통해 조인을 최소화합니다. (참고: MongoDB Atlas 설계서)
   - 정규화 vs 비정규화 트레이드오프: 정규화 (데이터 일관성 향상, 저장 공간 절약, 쓰기 성능 향상), 비정규화 (읽기 성능 향상, 조인 최소화, 쿼리 복잡도 감소)
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "1.2 테이블 정규화 전략" 섹션
   
   **1.3 컬럼 타입 선택**:
   - TSID 컬럼 타입: `BIGINT UNSIGNED NOT NULL PRIMARY KEY` 사용
   - 문자열 vs 숫자형: Primary Key는 가능한 한 숫자형을 사용하는 것이 성능상 유리합니다. TSID는 숫자형이므로 이 요구사항을 만족합니다.
   - JSON 컬럼 사용 전략: MySQL 8.0은 JSON 타입을 지원하며, JSON 함수를 사용하여 효율적으로 조회할 수 있습니다. 유연한 스키마가 필요한 경우 JSON 컬럼을 활용할 수 있습니다. JSON 컬럼에도 인덱스를 생성할 수 있으므로, 자주 조회하는 필드에 대해 생성된 컬럼 인덱스를 생성하는 것을 고려해야 합니다. 예제: `INDEX idx_metadata_status ((CAST(metadata->>'$.status' AS CHAR(20))))`
   - ENUM vs VARCHAR 선택: ENUM (값의 범위가 제한적이고 변경이 거의 없는 경우 사용, 저장 공간 절약, 타입 안정성), VARCHAR (값의 범위가 자주 변경되거나 확장 가능성이 있는 경우 사용)
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "1.3 컬럼 타입 선택" 섹션
   
   **1.4 NULL 처리 전략**:
   - NOT NULL 제약조건 활용: 가능한 한 NOT NULL 제약조건을 사용하여 데이터 무결성을 보장합니다. NULL 값은 인덱스에서 별도로 처리되므로, NOT NULL을 사용하면 인덱스 효율이 향상됩니다.
   - 기본값 설정 전략: NOT NULL 컬럼에는 적절한 기본값을 설정합니다. TIMESTAMP 컬럼의 경우 `DEFAULT CURRENT_TIMESTAMP(6)`를 사용하여 자동으로 현재 시간을 설정할 수 있습니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "1.4 NULL 처리 전략" 섹션
   
   **인덱스 설계 전략 (CQRS 패턴에 맞는 최적화)**:
   
   **2.1 Primary Key 인덱스 (TSID)**:
   - 클러스터드 인덱스 특성: InnoDB는 Primary Key를 기반으로 클러스터드 인덱스를 생성합니다. TSID를 Primary Key로 사용하면 데이터의 물리적 정렬이 시간 순서대로 유지되고, Primary Key로 조회 시 추가 인덱스 조회가 불필요합니다.
   - INSERT 성능 최적화: Aurora MySQL의 빠른 입력(Fast Insert) 기능을 활용합니다. 기본 키에 의해 정렬되는 병렬 입력을 빠르게 처리하여 쓰기 성능을 향상시킵니다. `LOAD DATA` 및 `INSERT INTO ... SELECT ...` 문 사용 시 인덱스 순회를 최적화합니다. TSID와 같은 순차적인 키를 사용할 경우 이 기능의 효과가 극대화됩니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "2.1 Primary Key 인덱스 (TSID)" 섹션
   
   **2.2 Secondary Index 설계**:
   - Command Side 인덱스 (쓰기 최적화): Command Side는 쓰기 작업에 최적화되어야 하므로, 인덱스를 최소화합니다. 필수적인 인덱스만 생성하여 INSERT, UPDATE, DELETE 성능을 향상시킵니다. 외래 키 인덱스는 데이터 무결성을 위해 필수입니다.
   - 복합 인덱스 설계 원칙: 복합 인덱스의 컬럼 순서는 쿼리 패턴에 따라 결정합니다. 가장 선택도가 높은 컬럼을 앞에 배치합니다. 등호 조건 컬럼을 범위 조건 컬럼보다 앞에 배치합니다. 예제: `CREATE INDEX idx_user_status_created ON example_table (user_id, status, created_at);`
   - 커버링 인덱스 활용: 쿼리에 필요한 모든 컬럼이 인덱스에 포함되면, 테이블 조회 없이 인덱스만으로 결과를 반환할 수 있습니다. 읽기 성능을 크게 향상시킵니다. 예제: `CREATE INDEX idx_user_status_covering ON example_table (user_id, status, name, email);`
   - 인덱스 선택도 (Selectivity) 고려: 선택도 = (고유 값 수) / (전체 행 수). 선택도가 높을수록 인덱스 효율이 높습니다. 선택도가 낮은 인덱스는 제거를 고려해야 합니다.
   - 인덱스 모니터링: MySQL의 `sys` 스키마를 활용하여 인덱스 사용률을 모니터링하고, 사용되지 않는 인덱스를 제거하여 쓰기 성능을 향상시킵니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "2.2 Secondary Index 설계" 섹션
   
   **파티셔닝 전략 (선택사항, 대용량 테이블에 적용)**:
   
   **3.1 파티셔닝 타입 선택**:
   - Range 파티셔닝 (TSID 기반): TSID는 시간 기반으로 정렬되므로, Range 파티셔닝에 적합합니다. 시간 범위별로 파티션을 나누면 파티션 프루닝이 용이합니다.
   - Hash 파티셔닝: Hash 파티셔닝은 데이터를 균등하게 분산시킵니다. TSID를 Hash 함수에 적용하여 파티션을 결정할 수 있습니다.
   - CQRS 패턴에서의 파티셔닝 활용: Command Side는 쓰기 작업에 최적화되어야 하므로, 파티셔닝을 통해 쓰기 부하를 분산시킵니다. TSID 기반 파티셔닝을 사용하면 순차 INSERT 작업이 여러 파티션에 분산되어 성능이 향상됩니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "3. 파티셔닝 전략" 섹션
   
   **CQRS 패턴 최적화**:
   
   **4.1 Command Side 최적화**:
   - 쓰기 작업 최적화 전략: 트랜잭션 경계 최소화 (트랜잭션 범위를 최소화하여 락 경합을 줄입니다), 배치 INSERT 최적화 (여러 행을 한 번에 INSERT하여 네트워크 왕복을 최소화합니다. Aurora MySQL의 빠른 입력 기능을 활용합니다)
   - 이벤트 발행 최적화: Command Side에서 데이터 변경 후 이벤트를 발행합니다. 트랜잭션 커밋 후 이벤트를 발행하여 일관성을 보장합니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "4.1 Command Side 최적화" 섹션
   
   **Aurora MySQL 특화 최적화**:
   
   **5.1 스토리지 아키텍처 활용**:
   - 로그 구조화 스토리지 최적화: Aurora MySQL은 로그 구조화 스토리지를 사용하여 쓰기 성능을 향상시킵니다. 순차 쓰기 작업이 많을수록 성능이 향상되므로, TSID와 같은 순차적인 키를 사용하는 것이 유리합니다.
   - 백업 및 복구 전략: Aurora MySQL은 연속 백업을 제공하므로, Point-in-Time Recovery가 가능합니다. 자동 백업을 활성화하여 데이터 손실을 방지합니다.
   
   **5.2 읽기 전용 복제본**:
   - 읽기 부하 분산: 읽기 전용 복제본을 생성하여 읽기 부하를 분산시킵니다. (참고: Query Side는 MongoDB Atlas 사용)
   - 복제 지연 관리: 복제 지연을 모니터링하여 적절한 복제본을 선택합니다.
   
   **5.3 성능 모니터링**:
   - Performance Insights 활용: Aurora MySQL의 Performance Insights를 활용하여 쿼리 성능을 분석하고, 느린 쿼리를 식별하여 최적화합니다.
   - 인덱스 모니터링: MySQL의 `sys` 스키마를 활용하여 인덱스 사용률을 모니터링하고, 사용되지 않는 인덱스를 제거합니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "5. Aurora MySQL 특화 최적화" 섹션
   
   **메인 테이블 (Command Side - 쓰기 전용, 높은 정규화)**:
   
   - **Provider 테이블**: OAuth 제공자 정보 (쓰기 전용)
     - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - name (VARCHAR(50) NOT NULL, UNIQUE, 예: GOOGLE, KAKAO, NAVER 등)
       - display_name (VARCHAR(100) NOT NULL, 표시명)
       - client_id (VARCHAR(255) NULL, OAuth Client ID)
       - client_secret (VARCHAR(255) NULL, OAuth Client Secret, 암호화 저장)
       - is_enabled (BOOLEAN NOT NULL DEFAULT TRUE, 활성화 여부)
       - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
       - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
       - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
       - updated_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6))
     - 제약사항: name은 UNIQUE
     - 인덱스 전략 (Command Side 최소화): name (UNIQUE 인덱스) - 필수, is_enabled (단일 인덱스) - 활성화된 제공자 조회용, is_deleted (단일 인덱스) - Soft Delete 필터링용
     - Aurora 최적화: 빠른 입력 기능 활용, 트랜잭션 경계 최소화
   
   - **User 테이블**: 사용자 정보 (쓰기 전용, Soft Delete 지원)
     - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - email (VARCHAR(100) NOT NULL, UNIQUE)
       - username (VARCHAR(50) NOT NULL, UNIQUE)
       - password (VARCHAR(255) NULL, 암호화 저장, OAuth 사용자는 NULL 가능)
       - provider_id (BIGINT UNSIGNED NULL, FK to Provider.id, OAuth 제공자)
       - provider_user_id (VARCHAR(255) NULL, OAuth 제공자의 사용자 ID)
       - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
       - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
       - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
       - updated_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6))
     - 제약사항: email은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려), username은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려), provider_id + provider_user_id는 UNIQUE (OAuth 사용자 중복 방지, 삭제되지 않은 항목만, 부분 인덱스 고려)
     - 인덱스 전략 (Command Side 최소화): email (UNIQUE 인덱스) - 필수, username (UNIQUE 인덱스) - 필수, provider_id (외래 키 인덱스) - 필수, provider_id + provider_user_id (복합 인덱스) - OAuth 로그인용, delete_yn (단일 인덱스) - Soft Delete 필터링용
     - Aurora 최적화: 빠른 입력 기능 활용, 트랜잭션 경계 최소화
   
   - **Admin 테이블**: 관리자 정보 (쓰기 전용, Soft Delete 지원)
     - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - email (VARCHAR(100) NOT NULL, UNIQUE)
       - username (VARCHAR(50) NOT NULL, UNIQUE)
       - password (VARCHAR(255) NOT NULL, 암호화 저장)
       - role (VARCHAR(50) NOT NULL, 관리자 역할: SUPER_ADMIN, ADMIN 등)
       - is_active (BOOLEAN NOT NULL DEFAULT TRUE, 활성화 상태)
       - last_login_at (TIMESTAMP(6) NULL, 마지막 로그인 시간)
       - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
       - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
       - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
       - updated_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6))
     - 제약사항: email은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려), username은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려)
     - 인덱스 전략 (Command Side 최소화): email (UNIQUE 인덱스) - 필수, username (UNIQUE 인덱스) - 필수, role (단일 인덱스) - 역할별 조회용, is_active (단일 인덱스) - 활성화 상태별 조회용, is_deleted (단일 인덱스) - Soft Delete 필터링용
     - Aurora 최적화: 빠른 입력 기능 활용, 트랜잭션 경계 최소화
   
  - **Archive 테이블**: 아카이브 정보 (쓰기 전용, Soft Delete 지원)
    - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
    - 컬럼 타입:
      - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
      - user_id (BIGINT UNSIGNED NOT NULL, FK to User.id)
      - item_type (VARCHAR(50) NOT NULL, 아이템 타입: CONTEST, NEWS_ARTICLE 등)
      - item_id (VARCHAR(255) NOT NULL, MongoDB Atlas의 ContestDocument 또는 NewsArticleDocument의 _id 값)
      - tag (VARCHAR(255) NULL, 태그)
      - memo (TEXT NULL, 메모)
      - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
      - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
      - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
      - updated_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6))
     - 설명: item_id는 MongoDB Atlas 클러스터의 Query Side 데이터(ContestDocument, NewsArticleDocument)의 _id 값을 저장합니다. CQRS 패턴에 따라 Command Side(Aurora MySQL)에서는 Query Side(MongoDB Atlas)의 문서 ID를 참조하여 아카이브 정보를 관리합니다.
     - 제약사항: userId + itemType + itemId는 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려)
     - 인덱스 전략 (Command Side 최소화): user_id (외래 키 인덱스) - 필수, user_id + delete_yn (복합 인덱스) - Soft Delete 필터링용
     - Aurora 최적화: 배치 INSERT 최적화, 트랜잭션 경계 최소화
   
   - **RefreshToken 테이블**: JWT Refresh Token (Redis 대체용, Soft Delete 지원)
     - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - user_id (BIGINT UNSIGNED NOT NULL, FK to User.id)
       - token (VARCHAR(500) NOT NULL, UNIQUE)
       - expires_at (TIMESTAMP(6) NOT NULL)
       - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
       - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
       - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
     - 제약사항: token은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려)
     - 인덱스 전략 (Command Side 최소화): token (UNIQUE 인덱스) - 필수, user_id (외래 키 인덱스) - 필수, expires_at (단일 인덱스) - 만료 토큰 정리용, delete_yn (단일 인덱스) - Soft Delete 필터링용
   
   - **EmailVerification 테이블**: 이메일 인증 토큰 (Soft Delete 지원)
     - Primary Key: id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - email (VARCHAR(100) NOT NULL)
       - token (VARCHAR(255) NOT NULL, UNIQUE)
       - expires_at (TIMESTAMP(6) NOT NULL)
       - verified_at (TIMESTAMP(6) NULL)
       - delete_yn (BOOLEAN NOT NULL DEFAULT FALSE, Soft Delete 플래그)
       - deleted_at (TIMESTAMP(6) NULL, Soft Delete 시점)
       - created_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
     - 제약사항: token은 UNIQUE (삭제되지 않은 항목만, 부분 인덱스 고려)
     - 인덱스 전략 (Command Side 최소화): token (UNIQUE 인덱스) - 필수, email (단일 인덱스) - 이메일별 조회용, expires_at (단일 인덱스) - 만료 토큰 정리용, delete_yn (단일 인덱스) - Soft Delete 필터링용
   
   **히스토리 테이블 (변경 이력 추적 - Command Side)**:
   
   - **UserHistory 테이블**: User 엔티티 변경 이력
     - Primary Key: history_id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - history_id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - user_id (BIGINT UNSIGNED NOT NULL, FK to User.id)
       - operation_type (VARCHAR(20) NOT NULL, ENUM: 'INSERT', 'UPDATE', 'DELETE')
       - before_data (JSON NULL, 변경 전 데이터)
       - after_data (JSON NULL, 변경 후 데이터)
       - changed_by (BIGINT UNSIGNED NULL, FK to User.id, 변경한 사용자)
       - changed_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
       - change_reason (VARCHAR(500) NULL)
     - 인덱스 전략 (Command Side 최소화): user_id (외래 키 인덱스) - 필수, changed_at (단일 인덱스) - 시간순 조회용, operation_type + changed_at (복합 인덱스) - 작업 타입별 조회용
     - JSON 인덱스: before_data, after_data의 특정 필드에 대해 생성된 컬럼 인덱스 고려
   
   - **ArchiveHistory 테이블**: Archive 엔티티 변경 이력
     - Primary Key: history_id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입: UserHistory와 유사한 구조
     - 인덱스 전략: user_id, changed_at, operation_type + changed_at
   
   - **AdminHistory 테이블**: Admin 엔티티 변경 이력
     - Primary Key: history_id (TSID, BIGINT UNSIGNED, @Tsid 어노테이션 사용)
     - 컬럼 타입:
       - history_id (BIGINT UNSIGNED NOT NULL PRIMARY KEY)
       - admin_id (BIGINT UNSIGNED NOT NULL, FK to Admin.id)
       - operation_type (VARCHAR(20) NOT NULL, ENUM: 'INSERT', 'UPDATE', 'DELETE')
       - before_data (JSON NULL, 변경 전 데이터)
       - after_data (JSON NULL, 변경 후 데이터)
       - changed_by (BIGINT UNSIGNED NULL, FK to Admin.id, 변경한 관리자)
       - changed_at (TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6))
       - change_reason (VARCHAR(500) NULL)
     - 인덱스 전략 (Command Side 최소화): admin_id (외래 키 인덱스) - 필수, changed_at (단일 인덱스) - 시간순 조회용, operation_type + changed_at (복합 인덱스) - 작업 타입별 조회용
     - JSON 인덱스: before_data, after_data의 특정 필드에 대해 생성된 컬럼 인덱스 고려
   
   
   **Spring Batch 메타데이터 테이블**:
   - Spring Batch 라이브러리에서 제공하는 공식 테이블 생성문을 사용하도록 제한.
   - 공식 테이블 생성문의 메타데이터 테이블들은 batch 스키마에 별도로 생성하도록 (docs/phase1/3. aurora-schema-design.md) 으로 추가.
   - **참고**: Spring Batch 공식 문서의 메타데이터 테이블 스키마
   
   **Foreign Key 관계, 인덱스 전략, 제약조건**:
   - 모든 외래 키 관계를 명확히 정의하고, 외래 키 인덱스를 생성합니다. User.provider_id → Provider.id (OAuth 제공자 참조), Archive.user_id → User.id, RefreshToken.user_id → User.id
   - Command Side 인덱스 전략: 필수적인 인덱스만 생성하여 쓰기 성능을 최적화합니다.
   - NOT NULL 제약조건을 적극 활용하여 데이터 무결성을 보장합니다.
   - UNIQUE 제약조건은 비즈니스 규칙에 따라 적용합니다. Soft Delete를 고려하여 삭제되지 않은 항목만 UNIQUE 제약조건이 적용되도록 부분 인덱스를 활용합니다.
   - Soft Delete 전략: 모든 메인 테이블(User, Admin, Provider, Archive, RefreshToken, EmailVerification)은 delete_yn과 deleted_at 컬럼을 포함하여 Soft Delete를 지원합니다. 히스토리 테이블은 변경 이력 추적용이므로 Soft Delete를 적용하지 않습니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "2. 인덱스 설계 전략" 섹션

4. 설계서 검증
   
   **베스트 프랙티스 준수 검증**:
   - 참고 문서 확인: docs/aurora-mysql-schema-design-best-practices.md의 모든 섹션을 참고하여 설계가 베스트 프랙티스를 준수하는지 확인
   - CQRS 패턴 준수 확인: Command Side는 높은 정규화 수준(최소 3NF)을 유지하는지 확인, Command Side 인덱스가 최소화되어 쓰기 성능이 최적화되었는지 확인, 트랜잭션 경계가 최소화되어 락 경합이 줄어들었는지 확인
   - Soft Delete 구현 확인: 모든 메인 테이블(User, Admin, Provider, Archive, RefreshToken, EmailVerification)에 delete_yn (BOOLEAN)과 deleted_at (TIMESTAMP) 필드가 포함되었는지 확인, Soft Delete 필터링을 위한 인덱스가 적절히 설계되었는지 확인 (delete_yn 단일 인덱스 또는 복합 인덱스), UNIQUE 제약조건이 Soft Delete를 고려하여 부분 인덱스로 구현되었는지 확인
   - 히스토리 테이블 설계 확인: 모든 주요 엔티티에 대한 히스토리 테이블이 설계되었는지 확인 (User, Admin, Archive), JSON 컬럼(before_data, after_data)에 대한 인덱스 전략이 포함되었는지 확인
   - 인덱스 최적화 확인: Command Side 인덱스가 최소화되어 있는지 확인 (필수 인덱스만), 외래 키 인덱스가 모든 FK에 대해 생성되었는지 확인, 복합 인덱스의 컬럼 순서가 쿼리 패턴에 최적화되었는지 확인 (등호 조건 → 범위 조건), 인덱스 선택도가 고려되었는지 확인, 사용되지 않는 인덱스가 없는지 확인
   - 관계 무결성 확인: 모든 외래 키 관계가 명확히 정의되었는지 확인 (User.provider_id → Provider.id (OAuth 제공자 참조), Archive.user_id → User.id, RefreshToken.user_id → User.id), 외래 키 제약조건이 적절히 설정되었는지 확인
   - OAuth Provider 설계 확인: Provider 테이블이 추가되었는지 확인, User 테이블에서 provider_id를 통해 Provider를 참조하도록 설계되었는지 확인, provider_id + provider_user_id 복합 인덱스가 OAuth 로그인에 최적화되었는지 확인
   - Archive 테이블 item_id 설계 확인: Archive 테이블의 item_id가 MongoDB Atlas의 ContestDocument 또는 NewsArticleDocument의 _id 값을 저장한다는 설명이 포함되었는지 확인, CQRS 패턴에 따라 Command Side(Aurora MySQL)에서 Query Side(MongoDB Atlas)의 문서 ID를 참조하는 구조임을 명시했는지 확인
   - 멀티모듈 구조 검증: 모듈 간 의존성이 올바른지 확인
   
   **TSID Primary Key 전략 검증**:
   - 모든 테이블의 Primary Key가 TSID 방식으로 설계되었는지 확인
   - Primary Key 컬럼 타입이 `BIGINT UNSIGNED NOT NULL`로 정의되었는지 확인
   - JPA Entity에 `@Tsid` 어노테이션 사용이 명시되었는지 확인
   - TSID 생성기 (`TsidGenerator`) 구현이 포함되었는지 확인
   - TSID 라이브러리 의존성 (`io.hypersistence:hypersistence-tsid`)이 포함되었는지 확인
   - 데이터 추가 규칙 (JPA Entity만 사용, 직접 SQL INSERT 금지)이 명시되었는지 확인
   - TSID가 애플리케이션 레벨에서 생성되도록 설계되었는지 확인
   - 분산 환경 고려사항 (노드 ID 포함, NTP 동기화)이 문서화되었는지 확인
   - 참고: docs/reference/shrimp-task-prompts-final-goal.md의 "TSID Primary Key 전략 구현" 섹션, docs/aurora-mysql-schema-design-best-practices.md의 "1.1 Primary Key 설계 (TSID 기반)" 섹션
   
   **컬럼 타입 및 NULL 처리 검증**:
   - TSID 컬럼이 `BIGINT UNSIGNED` 타입으로 정의되었는지 확인
   - 가능한 한 NOT NULL 제약조건이 적용되었는지 확인
   - JSON 컬럼 사용 시 생성된 컬럼 인덱스 전략이 포함되었는지 확인
   - ENUM vs VARCHAR 선택이 적절한지 확인
   - TIMESTAMP 컬럼에 `DEFAULT CURRENT_TIMESTAMP(6)`가 설정되었는지 확인
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "1.3 컬럼 타입 선택", "1.4 NULL 처리 전략" 섹션
   
   **Aurora MySQL 특화 최적화 검증**:
   - 빠른 입력(Fast Insert) 기능 활용 전략이 포함되었는지 확인
   - 배치 INSERT 최적화 전략이 포함되었는지 확인
   - 트랜잭션 경계 최소화 전략이 포함되었는지 확인
   - 로그 구조화 스토리지 최적화 고려사항이 포함되었는지 확인
   - 백업 및 복구 전략이 명시되었는지 확인
   - Performance Insights 활용 전략이 포함되었는지 확인
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "4. CQRS 패턴 최적화", "5. Aurora MySQL 특화 최적화" 섹션
   
   **파티셔닝 전략 검증 (선택사항)**:
   - 대용량 테이블에 대해 파티셔닝 전략이 고려되었는지 확인
   - TSID 기반 Range 파티셔닝 전략이 포함되었는지 확인 (필요한 경우)
   - 파티션 프루닝이 가능하도록 설계되었는지 확인
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "3. 파티셔닝 전략" 섹션
   
   **MongoDB Atlas 예외 로깅 설계 검증**:
   - `exception_logs` 컬렉션이 설계서에 포함되었는지 확인
   - source 필드 ("READ" 또는 "WRITE")로 읽기/쓰기 예외 구분이 명시되었는지 확인
   - 필수 필드 (exceptionType, exceptionMessage, stackTrace, context, occurredAt, severity)가 포함되었는지 확인
   - 인덱스 전략 (source + occurredAt 복합 인덱스, exceptionType + occurredAt 복합 인덱스, occurredAt TTL 인덱스)이 포함되었는지 확인
   - 비동기 처리 및 예외 로깅 실패 시 대체 기록 방법이 명시되었는지 확인
   - 참고: docs/reference/shrimp-task-prompts-final-goal.md의 "예외 로깅 정책" 섹션
   
   **설계서 문서화 검증**:
   - 모든 테이블에 대한 DDL 예제가 포함되었는지 확인
   - 인덱스 생성 DDL이 포함되었는지 확인
   - Foreign Key 관계가 명확히 문서화되었는지 확인
   - 제약조건이 명확히 문서화되었는지 확인
   - Aurora MySQL 특화 최적화 전략이 문서화되었는지 확인
   - 코드 예제(TSID 생성, 배치 INSERT 등)가 포함되었는지 확인
   - Mermaid 다이어그램 포함 확인: 설계서에 mermaid.live에서 읽을 수 있는 Mermaid 다이어그램이 포함되어야 합니다. ERD(Entity Relationship Diagram): 테이블 간 관계를 시각화하는 다이어그램 포함, 인덱스 다이어그램: 주요 인덱스 구조를 시각화하는 다이어그램 포함 (선택사항), CQRS 아키텍처 다이어그램: Command Side와 Query Side의 데이터 흐름을 시각화하는 다이어그램 포함 (선택사항), Mermaid 코드 블록은 ```mermaid 형식으로 작성하여 GitHub, GitLab, mermaid.live 등에서 렌더링 가능하도록 해야 합니다.
   - 참고: docs/aurora-mysql-schema-design-best-practices.md의 "코드 예제" 섹션
```

### 2단계: API 설계 및 데이터 모델링

**단계 번호**: 2단계
**의존성**: 1단계 (프로젝트 구조 및 설계서 생성 완료 필요)
**다음 단계**: 3단계 (Common 모듈 구현)

```
plan task: API Server 아키텍처 설계 및 데이터 모델 정의

참고 파일: docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표), json/sources.json (이미 생성됨), docs/phase1/2. mongodb-schema-design.md, docs/phase1/3. aurora-schema-design.md

**역할**: API 아키텍트 및 데이터 모델 설계자
**책임**: 
- RESTful API 엔드포인트 설계
- CQRS 패턴 기반 데이터 모델 설계
- API 응답 형식 및 에러 핸들링 전략 수립
- 설계서 문서화 및 검증

**작업 요약**:
Phase 1에서 완료된 멀티모듈 프로젝트 구조 검증 및 데이터베이스 설계서(`docs/phase1/`)를 기반으로, API Server의 아키텍처를 설계하고 데이터 모델을 정의합니다. 

주요 작업 내용:
1. RESTful API 엔드포인트 설계: 공개 API(대회, 뉴스, 출처), 인증 API, 사용자 아카이브 API(JWT 토큰 기반 사용자별 타게팅), 변경 이력 조회 API(CQRS 패턴 예외로 Aurora MySQL 조회)를 CQRS 패턴에 맞춰 읽기/쓰기로 분리하여 설계
2. CQRS 패턴 기반 데이터 모델 설계: Command Side(Amazon Aurora MySQL)와 Query Side(MongoDB Atlas)의 데이터 모델을 분리 설계하고 실시간 동기화 전략 수립 (User → UserProfileDocument, Archive → ArchiveDocument, TSID 필드 기반 동기화)
3. API 응답 형식 표준화: 성공/에러 응답 구조를 일관되게 정의하여 API 인터페이스 표준화
4. 에러 핸들링 전략 수립: HTTP 상태 코드와 비즈니스 에러 코드를 분리하여 상세한 에러 정보 제공

모든 설계서는 생성 순서대로 번호를 붙여 `docs/phase2/` 디렉토리에 저장하며, Phase 1의 설계서(`docs/phase1/2. mongodb-schema-design.md`, `docs/phase1/3. aurora-schema-design.md`)를 참고하여 일관성을 유지합니다.

**초기 해결 방안**:
1. RESTful API 엔드포인트 설계
   - Phase 1의 MongoDB Atlas 및 Aurora MySQL 스키마 설계서 참고
   - 공개 API(인증 불필요): 대회 목록/상세/검색, 뉴스 목록/상세/검색, 출처 목록 조회
   - 인증 API: 회원가입, 로그인, 로그아웃, 토큰 갱신, 이메일 인증, 비밀번호 재설정, OAuth 2.0
   - 사용자 아카이브 API(인증 필요): JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 타게팅, 생성, 조회, 수정, 삭제(Soft Delete), 복원
   - 변경 이력 조회 API(인증 필요): 히스토리 조회, 특정 시점 데이터 조회, 복구 (모두 Aurora MySQL에서 조회, CQRS 패턴 예외)
   - CQRS 패턴 적용: 읽기 작업은 MongoDB Atlas, 쓰기 작업은 Aurora MySQL (변경 이력 조회 및 삭제된 아카이브 조회는 예외적으로 Aurora MySQL에서 조회)
   - 설계서 저장: `docs/phase2/1. api-endpoint-design.md`

2. CQRS 패턴 기반 데이터 모델 분리 설계
   - Phase 1의 스키마 설계서를 기반으로 Command Side와 Query Side 데이터 모델 상세 설계
   - Command Side(Aurora MySQL): TSID Primary Key, Soft Delete, 감사 필드, 히스토리 엔티티 설계
   - Query Side(MongoDB Atlas): ObjectId Primary Key, 읽기 최적화, 비정규화, ESR 규칙 준수 인덱스 설계
   - Kafka 이벤트 기반 실시간 동기화 전략 수립
     * User 엔티티 변경 시 즉시 UserProfileDocument로 동기화 (Kafka 이벤트: UserCreatedEvent, UserUpdatedEvent, UserDeletedEvent, UserRestoredEvent)
     * Archive 엔티티 변경 시 즉시 ArchiveDocument로 동기화 (Kafka 이벤트: ArchiveCreatedEvent, ArchiveUpdatedEvent, ArchiveDeletedEvent, ArchiveRestoredEvent)
     * TSID 필드 기반 동기화: User.id(TSID) → UserProfileDocument.userTsid, Archive.id(TSID) → ArchiveDocument.archiveTsid
     * MongoDB Document는 Soft Delete 미지원: Soft Delete 시 Document 물리적 삭제, 복원 시 Document 새로 생성
     * 동기화 지연 시간: 실시간 동기화 목표 (1초 이내)
   - 설계서 저장: `docs/phase2/2. data-model-design.md`

3. 표준 API 응답 형식 정의
   - 성공 응답: code, messageCode, message, data 구조 정의
   - 에러 응답: code, messageCode 구조 정의
   - 페이징 응답: pageSize, pageNumber, totalPageNumber, totalSize, list 구조 정의
   - 단일 객체/빈 응답 처리 규칙 정의
   - 설계서 저장: `docs/phase2/3. api-response-format-design.md`

4. HTTP 상태 코드와 비즈니스 에러 코드 분리 전략 수립
   - HTTP 상태 코드 매핑 규칙 정의 (200, 400, 401, 403, 404, 429, 500, 503)
   - 비즈니스 에러 코드 체계 정의 (2xxx: 성공, 4xxx: 클라이언트 에러, 5xxx: 서버 에러)
   - 에러 응답 형식 표준화
   - 설계서 저장: `docs/phase2/4. error-handling-strategy-design.md`

5. 설계서 검증 및 산출물 정리
   - 모든 설계서의 완전성 및 정확성 검증
   - Phase 1 설계서와의 일관성 검증
   - 검증 기준 충족 여부 확인
   - 검증 보고서 작성
   - 설계서 저장: `docs/phase2/5. design-verification-report.md`

**핵심 제한사항 (절대 준수 필수)**:
1. **공식 개발문서만 참고**: Spring Boot, REST API 설계, Amazon Aurora MySQL, MongoDB Atlas 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **Phase 1 설계서 준수**: `docs/phase1/2. mongodb-schema-design.md`와 `docs/phase1/3. aurora-schema-design.md`의 스키마 설계를 기반으로 API 설계 수행
3. **CQRS 패턴 준수**: 모든 쓰기는 Aurora MySQL(Command Side), 모든 읽기는 MongoDB Atlas(Query Side)에서 수행. 읽기/쓰기 엔드포인트 명확히 분리
4. **Kafka 이벤트 발행 필수**: 모든 쓰기 작업 후 Kafka 이벤트 발행 필수. 이벤트 기반 동기화로 Command Side와 Query Side 데이터 일관성 보장
5. **실시간 동기화 필수**: Aurora MySQL의 User, Archive 테이블 데이터 변경은 변경 즉시 MongoDB Atlas의 UserProfileDocument, ArchiveDocument로 반영되어야 함. Kafka 이벤트를 통한 실시간 동기화 구현 (목표: 1초 이내)
   - User 엔티티 변경 시: User.id(TSID) → UserProfileDocument.userTsid 필드를 통해 동기화
   - Archive 엔티티 변경 시: Archive.id(TSID) → ArchiveDocument.archiveTsid 필드를 통해 동기화
   - TSID 필드 기반 1:1 매핑으로 동기화 정확성 보장
   - **MongoDB Document는 Soft Delete 미지원**: Aurora MySQL에서 Soft Delete 발생 시 MongoDB Document는 물리적 삭제, 복원 시 Document 새로 생성
6. **TSID Primary Key 사용**: Command Side의 모든 엔티티는 TSID(Time-Sorted Unique Identifier)를 Primary Key로 사용
7. **Soft Delete 패턴**: 삭제 작업은 물리적 삭제가 아닌 Soft Delete(delete_yn 플래그) 사용 (Aurora MySQL Command Side에만 적용, MongoDB Atlas Query Side는 Soft Delete 미지원)
8. **히스토리 추적 필수**: 모든 쓰기 작업에 대해 히스토리 테이블에 변경 이력 자동 저장
9. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 단일 구현체를 위한 인터페이스, 불필요한 추상화, "나중을 위해" 추가하는 코드 모두 금지.
10. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.
11. **설계서 저장 규칙**: 모든 설계서는 생성되는 순서대로 번호를 붙여 `docs/phase2/` 디렉토리에 저장해야 함

**전체 작업 검증 기준**: 
- **API 엔드포인트 설계 검증**:
  - 모든 API 엔드포인트가 RESTful 원칙을 준수해야 함
  - 읽기 작업은 MongoDB Atlas에서, 쓰기 작업은 Aurora MySQL에서 수행되어야 함 (변경 이력 조회 및 삭제된 아카이브 조회는 예외적으로 Aurora MySQL에서 조회)
  - 모든 쓰기 작업 후 Kafka 이벤트 발행이 명시되어야 함
  - 사용자 아카이브 API는 JWT 토큰에서 userId를 추출하여 해당 사용자의 아카이브만 타게팅되도록 설계되어야 함
  - 삭제된 아카이브 조회는 Aurora MySQL에서 수행되어야 함 (MongoDB는 Soft Delete 미지원)
  - 각 엔드포인트의 HTTP 메서드, 경로, 파라미터, 응답 형식이 명확히 정의되어야 함
- **데이터 모델 설계 검증**:
  - CQRS 패턴에 맞춰 Command Side와 Query Side가 명확히 분리되어야 함
  - 모든 엔티티가 정규화되어야 함 (Command Side 최소 3NF)
  - 읽기 모델이 쿼리 패턴에 최적화되어야 함 (Query Side 비정규화, ESR 규칙 준수)
  - 쓰기와 읽기 간 데이터 일관성이 보장되어야 함 (Kafka 이벤트 기반 동기화)
  - User 엔티티 변경 시 즉시 UserProfileDocument로 동기화되어야 함 (실시간 동기화, 목표: 1초 이내, userTsid 필드 활용)
  - User 엔티티 Soft Delete 시 UserProfileDocument 물리적 삭제, 복원 시 재생성되어야 함 (MongoDB는 Soft Delete 미지원)
  - Archive 엔티티 변경 시 즉시 ArchiveDocument로 동기화되어야 함 (실시간 동기화, 목표: 1초 이내, archiveTsid 필드 활용)
  - Archive 엔티티 Soft Delete 시 ArchiveDocument 물리적 삭제, 복원 시 재생성되어야 함 (MongoDB는 Soft Delete 미지원)
  - Phase 1 설계서의 스키마와 일관성을 유지해야 함
- **API 응답 형식 검증**:
  - 모든 API 응답이 일관된 형식을 따라야 함
  - 성공/에러 응답 구조가 명확히 정의되어야 함
  - 페이징 정보가 표준화되어야 함
- **에러 핸들링 전략 검증**:
  - HTTP 상태 코드와 비즈니스 에러 코드가 명확히 분리되어야 함
  - 에러 코드 체계가 일관되게 정의되어야 함
  - 모든 에러 응답이 표준 형식을 따라야 함
- **설계서 문서화 검증**:
  - 모든 설계서가 `docs/phase2/` 디렉토리에 순서대로 저장되어야 함
  - 각 설계서가 완전하고 정확해야 함
  - Phase 1 설계서와의 일관성이 유지되어야 함
- **빌드 검증**: 
  - 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
  - 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**기존 코드 및 구조 확인 필수 사항** (작업 수행 전 반드시 확인):
1. **Phase 1 설계서 상세 검토**:
   - `docs/phase1/2. mongodb-schema-design.md`: MongoDB Document 구조, 인덱스 전략, TSID 필드(userTsid, archiveTsid) 활용 방법, 쿼리 패턴 확인
   - `docs/phase1/3. aurora-schema-design.md`: Aurora MySQL 엔티티 구조, TSID Primary Key 전략, Soft Delete 패턴, 히스토리 엔티티 구조 확인
   - `docs/phase1/4. schema-design-verification-report.md`: 설계 검증 결과 및 일관성 확인
   - `docs/phase1/1. multimodule-structure-verification.md`: 멀티모듈 프로젝트 구조 및 모듈 간 의존성 확인

2. **프로젝트 규칙 및 아키텍처 확인**:
   - `shrimp-rules.md` 파일 존재 여부 확인 및 상세 참고 (존재하는 경우)
   - 프로젝트 특정 규칙 및 제약사항 확인
   - CQRS 패턴: Command Side(Aurora MySQL)와 Query Side(MongoDB Atlas) 분리 원칙 확인
   - MSA 멀티모듈 구조: api/, domain/, common/, client/ 모듈 구조 확인
   - 이벤트 기반 아키텍처: Kafka를 통한 데이터 동기화 패턴 확인

3. **기존 설계 패턴 및 컨벤션 확인**:
   - Phase 1 설계서의 명명 규칙 및 문서화 스타일 확인
   - 설계서 저장 경로 및 번호 체계 확인 (`docs/phase1/` 구조 참고)
   - 설계서 작성 형식 및 구조 일관성 확인

4. **참고 파일 재확인**:
   - `docs/reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표 및 요구사항 재확인
   - `json/sources.json`: 정보 출처 데이터 구조 확인 (이미 생성됨)

5. **정보 수집 원칙**:
   - 모든 설계 결정은 추적 가능한 신뢰할 수 있는 출처를 가져야 함 

작업 내용:
1. RESTful API 엔드포인트 설계
   
   **설계서 저장 경로**: `docs/phase2/1. api-endpoint-design.md`
   
   **역할**: RESTful API 설계자
   **책임**: 
   - 공개 API, 인증 API, 사용자 아카이브 API, 변경 이력 조회 API 엔드포인트 설계
   - 각 엔드포인트의 HTTP 메서드, 경로, 파라미터, 응답 형식 정의
   - CQRS 패턴에 맞춰 읽기/쓰기 엔드포인트 분리
   
   **검증 기준**:
   - 모든 엔드포인트가 RESTful 원칙을 준수해야 함
   - 읽기 작업은 MongoDB Atlas에서, 쓰기 작업은 Aurora MySQL에서 수행되어야 함 (변경 이력 조회 및 삭제된 아카이브 조회는 예외적으로 Aurora MySQL에서 조회)
   - 모든 쓰기 작업 후 Kafka 이벤트 발행이 명시되어야 함
   - 사용자 아카이브 API는 JWT 토큰에서 userId를 추출하여 해당 사용자의 아카이브만 타게팅되도록 설계되어야 함
   - 삭제된 아카이브 조회는 Aurora MySQL에서 수행되어야 함 (MongoDB는 Soft Delete 미지원)
   - 설계서가 `docs/phase2/1. api-endpoint-design.md`에 저장되어야 함
   
   **하위 작업 1.1: 공개 API 엔드포인트 설계**
   - **역할**: 공개 API 설계자
   - **책임**: 
     * 인증 없이 접근 가능한 공개 API 엔드포인트 설계
     * MongoDB Atlas에서 읽기 전용 조회 엔드포인트 설계
     * 페이징, 필터링, 정렬 파라미터 정의
     * Full-text search 기능 설계
   
   공개 API (인증 불필요):
   - GET /api/v1/contests - 개발자 대회 목록 조회 (MongoDB Atlas에서 조회)
     * 쿼리 파라미터: page, size, sort, sourceId, status (UPCOMING|ONGOING|ENDED)
     * ContestDocument 조회 (sourceId + startDate 인덱스 활용)
   - GET /api/v1/contests/{id} - 특정 대회 상세 정보 (MongoDB Atlas에서 조회)
     * ContestDocument 조회
   - GET /api/v1/contests/search?q={query} - 대회 검색 (MongoDB Atlas Full-text search)
   - GET /api/v1/news - IT 테크 뉴스 목록 조회 (MongoDB Atlas에서 조회)
     * 쿼리 파라미터: page, size, sort, sourceId
     * NewsArticleDocument 조회 (sourceId + publishedAt 인덱스 활용)
   - GET /api/v1/news/{id} - 특정 뉴스 상세 정보 (MongoDB Atlas에서 조회)
     * NewsArticleDocument 조회
   - GET /api/v1/news/search?q={query} - 뉴스 검색 (MongoDB Atlas Full-text search)
   - GET /api/v1/sources - 정보 출처 목록 조회 (MongoDB Atlas에서 조회)
     * SourcesDocument 조회
     * 쿼리 파라미터: page, size, type, is_enabled
     * type + is_enabled 인덱스 활용

   **하위 작업 1.2: 인증 API 엔드포인트 설계**
   - **역할**: 인증 API 설계자
   - **책임**: 
     * 사용자 회원가입, 로그인, 로그아웃 엔드포인트 설계
     * JWT 토큰 관리 엔드포인트 설계
     * 이메일 인증 및 비밀번호 재설정 엔드포인트 설계
     * OAuth 2.0 SNS 로그인 엔드포인트 설계
     * Aurora MySQL 쓰기 작업 설계

   인증 API:
   - POST /api/v1/auth/signup - 회원가입 (Amazon Aurora MySQL에 쓰기)
     * User 엔티티 생성, EmailVerification 엔티티 생성
   - POST /api/v1/auth/login - 로그인
     * User 엔티티 조회, RefreshToken 엔티티 생성
   - POST /api/v1/auth/logout - 로그아웃
     * RefreshToken 엔티티 Soft Delete
   - POST /api/v1/auth/refresh - 토큰 갱신
     * RefreshToken 엔티티 조회 및 갱신
   - GET /api/v1/auth/verify-email?token={token} - 이메일 인증
     * EmailVerification 엔티티 조회 및 업데이트
   - POST /api/v1/auth/reset-password - 비밀번호 재설정 요청
     * EmailVerification 엔티티 생성
   - POST /api/v1/auth/reset-password/confirm - 비밀번호 재설정 확인
     * User 엔티티 업데이트, EmailVerification 엔티티 업데이트
   - GET /api/v1/auth/oauth2/{provider} - SNS 로그인 시작
     * Provider 엔티티 조회
   - GET /api/v1/auth/oauth2/{provider}/callback - SNS 로그인 콜백
     * Provider 엔티티 조회, User 엔티티 조회/생성, RefreshToken 엔티티 생성

   **하위 작업 1.3: 사용자 아카이브 API 엔드포인트 설계**
   - **역할**: 사용자 아카이브 API 설계자
   - **책임**: 
     * 사용자 아카이브 CRUD 엔드포인트 설계
     * Soft Delete 및 복원 엔드포인트 설계
     * JWT 토큰에서 로그인 사용자 정보(userId) 추출 및 권한 검증 로직 설계
     * 로그인 사용자의 아카이브만 타게팅되도록 설계
     * Aurora MySQL 쓰기 및 MongoDB Atlas 읽기 분리 설계
     * Kafka 이벤트 발행 설계

   사용자 아카이브 API (인증 필요):
   - POST /api/v1/archives - 아카이브 추가 (Amazon Aurora MySQL에 쓰기, Kafka 이벤트 발행)
     * **역할**: 사용자 아카이브 생성
     * **책임**: JWT 토큰에서 userId 추출, 데이터 검증, Aurora Archive 엔티티 저장(userId 자동 설정), ArchiveHistory 엔티티 생성, Kafka 이벤트 발행
     * **검증 기준**: 아카이브가 정상적으로 저장되고 이벤트가 발행되어야 함
     * **제약사항**: userId + itemType + itemId UNIQUE 제약조건 (중복 방지)
     * **주의사항**: 요청 body의 userId는 무시하고, JWT 토큰에서 추출한 userId를 사용
   - GET /api/v1/archives - 아카이브 목록 조회 (MongoDB Atlas에서 조회)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 조회
     * 파라미터: page, size, sort, itemType (CONTEST|NEWS_ARTICLE)
     * ArchiveDocument 조회 (userId + createdAt 인덱스 활용, userId는 JWT 토큰에서 추출)
     * **검증 기준**: 페이징이 정상적으로 동작하고 필터링이 적용되어야 함, 로그인 사용자의 아카이브만 조회되어야 함
   - GET /api/v1/archives/{id} - 아카이브 상세 조회 (MongoDB Atlas에서 조회)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 조회
     * ArchiveDocument 조회 (userId와 id로 조회하여 권한 검증)
     * **검증 기준**: 권한이 있는 사용자(본인)만 조회 가능해야 함
   - PUT /api/v1/archives/{id} - 아카이브 수정 (Amazon Aurora MySQL에 쓰기, Kafka 이벤트 발행)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 수정 가능
     * Archive 엔티티 업데이트(userId 검증), ArchiveHistory 엔티티 생성, Kafka 이벤트 발행
     * **검증 기준**: 수정된 데이터가 정상적으로 저장되고 이벤트가 발행되어야 함, 본인의 아카이브만 수정 가능해야 함
   - DELETE /api/v1/archives/{id} - 아카이브 삭제 (Soft Delete, Amazon Aurora MySQL에 쓰기, Kafka 이벤트 발행)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 삭제 가능
     * Archive 엔티티 Soft Delete (delete_yn='Y', deleted_at 설정, userId 검증), ArchiveHistory 엔티티 생성, Kafka 이벤트 발행
     * **검증 기준**: delete_yn 플래그가 'Y'로 설정되고 deleted_at이 기록되어야 함, 본인의 아카이브만 삭제 가능해야 함
   - POST /api/v1/archives/{id}/restore - 아카이브 복원 (Amazon Aurora MySQL에 쓰기, Kafka 이벤트 발행)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 아카이브만 복원 가능
     * Archive 엔티티 복원 (delete_yn='N', deleted_at=null, userId 검증), ArchiveHistory 엔티티 생성, ArchiveRestoredEvent 발행
     * ArchiveRestoredEvent 수신 시 MongoDB Atlas의 ArchiveDocument 새로 생성 (MongoDB는 Soft Delete 미지원이므로 복원 시 재생성 필요)
     * **검증 기준**: delete_yn 플래그가 'N'으로 변경되고 deleted_at이 null로 설정되어야 함, 본인의 아카이브만 복원 가능해야 함, ArchiveDocument가 새로 생성되어야 함
   - GET /api/v1/archives/deleted - 삭제된 아카이브 목록 (Amazon Aurora MySQL에서 조회, CQRS 패턴 예외)
     * JWT 토큰에서 userId 추출하여 해당 사용자의 삭제된 아카이브만 조회
     * Archive 엔티티 조회 (Aurora MySQL, userId + delete_yn='Y' 필터링, userId는 JWT 토큰에서 추출)
     * **주의사항**: MongoDB Atlas는 Soft Delete를 구현하지 않으므로, 삭제된 아카이브는 Aurora MySQL에서만 조회 가능 (CQRS 패턴 예외)
     * **검증 기준**: 로그인 사용자의 삭제된 아카이브만 조회되어야 함

   **하위 작업 1.4: 변경 이력 조회 API 엔드포인트 설계**
   - **역할**: 변경 이력 API 설계자
   - **책임**: 
     * 엔티티별 변경 이력 조회 엔드포인트 설계
     * 특정 시점 데이터 조회 엔드포인트 설계
     * 히스토리 기반 복구 엔드포인트 설계
     * 권한 검증 로직 설계 (관리자 또는 본인)
     * Aurora MySQL 히스토리 테이블 조회 설계 (CQRS 패턴 예외: 쓰기 전용이지만 이력 조회만 허용)

   변경 이력 조회 API (인증 필요, 관리자 또는 본인):
   - GET /api/v1/history/{entityType}/{entityId} - 특정 엔티티의 변경 이력 조회 (Amazon Aurora MySQL에서 조회)
     * entityType: user, admin, archive
     * 파라미터: page, size, operationType (INSERT|UPDATE|DELETE), startDate, endDate
     * UserHistory, AdminHistory, ArchiveHistory 엔티티 조회 (Aurora MySQL, operation_type + changed_at 인덱스 활용)
     * **주의사항**: CQRS 패턴 예외 - Aurora MySQL은 쓰기 전용이지만, 이력 조회만 예외적으로 허용
   - GET /api/v1/history/{entityType}/{entityId}/at?timestamp={timestamp} - 특정 시점 데이터 조회 (Amazon Aurora MySQL에서 조회)
     * 히스토리 테이블의 before_data/after_data JSON 필드 활용 (Aurora MySQL)
     * **주의사항**: CQRS 패턴 예외 - Aurora MySQL은 쓰기 전용이지만, 이력 조회만 예외적으로 허용
   - POST /api/v1/history/{entityType}/{entityId}/restore?historyId={historyId} - 특정 버전으로 복구 (관리자만, Amazon Aurora MySQL에 쓰기)
     * 히스토리 데이터를 기반으로 엔티티 복구 (Aurora MySQL), 히스토리 엔티티 생성, Kafka 이벤트 발행
     * **주의사항**: 복구 작업은 쓰기 작업이므로 Aurora MySQL에 쓰기, 이후 Kafka 이벤트 발행

2. 데이터 모델 설계 (CQRS 패턴 적용)
   
   **설계서 저장 경로**: `docs/phase2/2. data-model-design.md`
   
   **역할**: Command/Query 분리 아키텍처 설계
   **책임**: 
   - 쓰기와 읽기 데이터 모델 분리
   - 데이터 동기화 전략 수립 (User → UserProfileDocument, Archive → ArchiveDocument 실시간 동기화)
   - 성능 최적화 전략 수립
   
   **검증 기준**:
   - 모든 엔티티가 정규화되어야 함 (최소 3NF)
   - 읽기 모델이 쿼리 패턴에 최적화되어야 함 (비정규화, ESR 규칙 준수)
   - 쓰기와 읽기 간 데이터 일관성이 보장되어야 함 (Kafka 이벤트 기반 동기화)
   - User 엔티티 변경 시 즉시 UserProfileDocument로 동기화되어야 함 (실시간 동기화, 목표: 1초 이내, userTsid 필드 활용)
   - User 엔티티 Soft Delete 시 UserProfileDocument 물리적 삭제, 복원 시 재생성되어야 함 (MongoDB는 Soft Delete 미지원)
   - Archive 엔티티 변경 시 즉시 ArchiveDocument로 동기화되어야 함 (실시간 동기화, 목표: 1초 이내, archiveTsid 필드 활용)
   - Archive 엔티티 Soft Delete 시 ArchiveDocument 물리적 삭제, 복원 시 재생성되어야 함 (MongoDB는 Soft Delete 미지원)
   - 설계서가 `docs/phase2/2. data-model-design.md`에 저장되어야 함
   - **빌드 검증**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   **하위 작업 2.1: Command Side 데이터 모델 설계**
   - **역할**: Command Side 데이터 모델 설계자
   - **책임**: 
     * Aurora MySQL 엔티티 설계 (쓰기 전용)
     * TSID Primary Key 적용
     * Soft Delete 패턴 적용
     * 감사 필드 설계
     * 히스토리 엔티티 설계
     * Phase 1 설계서와의 일관성 유지

   Command Side (Amazon Aurora MySQL - 쓰기 전용):
   - **공통 사항**:
     * Primary Key: TSID (BIGINT UNSIGNED) 사용
     * Soft Delete: delete_yn (CHAR(1), 기본값 'N'), deleted_at (TIMESTAMP(6)), deleted_by (BIGINT UNSIGNED)
     * 감사 필드: created_at, created_by, updated_at, updated_by
   
   - Provider 엔티티: OAuth 제공자 정보
     * **필드**: id (TSID), name (VARCHAR(50), UNIQUE), display_name, client_id, client_secret, is_enabled
     * **제약사항**: name UNIQUE, is_enabled 인덱스, is_deleted 인덱스
   
   - User 엔티티: 사용자 정보
     * **필드**: id (TSID), email (VARCHAR(100), UNIQUE), username (VARCHAR(50), UNIQUE), password (암호화), provider_id (FK), provider_user_id
     * **제약사항**: email UNIQUE, username UNIQUE, provider_id + provider_user_id 복합 인덱스, delete_yn 인덱스
     * **관계**: Provider와 Many-to-One 관계
     * **동기화 전략**: User 엔티티 변경 시 즉시 MongoDB Atlas의 UserProfileDocument로 동기화 (Kafka 이벤트: UserCreatedEvent, UserUpdatedEvent, UserDeletedEvent, UserRestoredEvent)
     * **동기화 매핑**: User.id(TSID) → UserProfileDocument.userTsid (1:1 매핑)
     * **동기화 동작**: 
       - 생성: UserCreatedEvent → UserProfileDocument 생성
       - 수정: UserUpdatedEvent → UserProfileDocument 업데이트
       - Soft Delete: UserDeletedEvent → UserProfileDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
       - 복원: UserRestoredEvent → UserProfileDocument 새로 생성
     * **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)
   
   - Admin 엔티티: 관리자 정보
     * **필드**: id (TSID), email (VARCHAR(100), UNIQUE), username (VARCHAR(50), UNIQUE), password (암호화), role, is_active, last_login_at
     * **제약사항**: email UNIQUE, username UNIQUE, role 인덱스, is_active 인덱스, is_deleted 인덱스
   
   - Archive 엔티티: 사용자 아카이브 정보 (Soft Delete 지원)
     * **필드**: id (TSID), user_id (FK), item_type (VARCHAR(50)), item_id (VARCHAR(255), MongoDB ObjectId 문자열), tag, memo
     * **제약사항**: user_id + item_type + item_id UNIQUE (중복 방지), user_id + delete_yn 복합 인덱스
     * **관계**: User와 Many-to-One 관계
     * **Soft Delete**: delete_yn 기본값 'N', deleted_at nullable
     * **동기화 전략**: Archive 엔티티 변경 시 즉시 MongoDB Atlas의 ArchiveDocument로 동기화 (Kafka 이벤트: ArchiveCreatedEvent, ArchiveUpdatedEvent, ArchiveDeletedEvent, ArchiveRestoredEvent)
     * **동기화 매핑**: Archive.id(TSID) → ArchiveDocument.archiveTsid (1:1 매핑)
     * **동기화 동작**: 
       - 생성: ArchiveCreatedEvent → ArchiveDocument 생성
       - 수정: ArchiveUpdatedEvent → ArchiveDocument 업데이트
       - Soft Delete: ArchiveDeletedEvent → ArchiveDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
       - 복원: ArchiveRestoredEvent → ArchiveDocument 새로 생성
     * **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)
   
   - RefreshToken 엔티티: JWT Refresh Token
     * **필드**: id (TSID), user_id (FK), token (VARCHAR(500), UNIQUE), expires_at
     * **제약사항**: token UNIQUE, user_id 인덱스, expires_at 인덱스, delete_yn 인덱스
     * **관계**: User와 Many-to-One 관계
   
   - EmailVerification 엔티티: 이메일 인증 토큰
     * **필드**: id (TSID), email, token (VARCHAR(255), UNIQUE), expires_at, verified_at
     * **제약사항**: token UNIQUE, email 인덱스, expires_at 인덱스, delete_yn 인덱스
   
   - 히스토리 엔티티 (자동 생성):
     * **UserHistory**: User 엔티티 변경 이력
       - history_id (TSID), user_id (FK), operation_type (INSERT|UPDATE|DELETE), before_data (JSON), after_data (JSON), changed_by, changed_at, change_reason
       - user_id 인덱스, operation_type + changed_at 복합 인덱스
     * **AdminHistory**: Admin 엔티티 변경 이력
       - history_id (TSID), admin_id (FK), operation_type, before_data (JSON), after_data (JSON), changed_by, changed_at, change_reason
       - admin_id 인덱스, operation_type + changed_at 복합 인덱스
     * **ArchiveHistory**: Archive 엔티티 변경 이력
       - history_id (TSID), archive_id (FK), operation_type, before_data (JSON), after_data (JSON), changed_by, changed_at, change_reason
       - archive_id 인덱스, operation_type + changed_at 복합 인덱스
     * **주의**: operation_type='DELETE'는 실제 SQL DELETE가 아닌 Soft Delete를 의미

   **하위 작업 2.2: Query Side 데이터 모델 설계**
   - **역할**: Query Side 데이터 모델 설계자
   - **책임**: 
     * MongoDB Atlas Document 설계 (읽기 전용)
     * 읽기 최적화를 위한 비정규화 설계
     * ESR 규칙 준수 인덱스 설계
     * 쿼리 패턴에 맞춘 Document 구조 설계
     * Phase 1 설계서와의 일관성 유지

   Query Side (MongoDB Atlas - 읽기 전용):
   - **공통 사항**:
     * Primary Key: _id (ObjectId)
     * 감사 필드: createdAt, createdBy, updatedAt, updatedBy
     * 인덱스 전략: ESR 규칙 (Equality → Sort → Range) 준수
   
   - SourcesDocument: 정보 출처 (읽기 최적화)
     * **필드**: _id, name (UNIQUE), type, url, apiEndpoint, rssFeedUrl, description, priority, reliabilityScore, accessibilityScore, dataQualityScore, legalEthicalScore, totalScore, authenticationRequired, authenticationMethod, rateLimit, documentationUrl, updateFrequency, dataFormat, enabled
     * **인덱스**: name (UNIQUE), type + enabled (복합), priority (단일)
     * **쿼리 패턴**: 활성화된 출처 조회, 우선순위별 조회
   
   - ContestDocument: 대회 정보 (읽기 최적화, 비정규화)
     * **필드**: _id, sourceId (FK), title, startDate, endDate, status, description, url, metadata (sourceName, prize, participants, tags)
     * **인덱스**: sourceId + startDate (복합), endDate (단일), status + startDate (부분 인덱스, UPCOMING/ONGOING만)
     * **쿼리 패턴**: sourceId별 시작일시 역순 조회, 진행 중인 대회 조회
     * **비정규화**: metadata.sourceName (출처 이름 중복 저장)
   
   - NewsArticleDocument: 뉴스 기사 (읽기 최적화, 비정규화)
     * **필드**: _id, sourceId (FK), title, content, summary, publishedAt, url, author, metadata (sourceName, tags, viewCount, likeCount)
     * **인덱스**: sourceId + publishedAt (복합), publishedAt (TTL 인덱스, 90일 후 자동 삭제)
     * **쿼리 패턴**: sourceId별 발행일시 역순 조회, 최근 7일간 뉴스 조회
     * **비정규화**: metadata.sourceName (출처 이름 중복 저장)
   
   - ArchiveDocument: 사용자 아카이브 (읽기 최적화, 사용자별 인덱스)
     * **필드**: _id, archiveTsid (Aurora MySQL Archive.id(TSID)와 1:1 매핑, UNIQUE), userId, itemType, itemId, itemTitle, itemSummary, tag, memo, archivedAt
     * **인덱스**: archiveTsid (UNIQUE, Aurora MySQL 동기화용), userId + createdAt (복합), userId + itemType + createdAt (복합), userId + itemType + itemId (UNIQUE)
     * **쿼리 패턴**: userId별 생성일시 역순 조회, userId + itemType별 조회, archiveTsid로 동기화 확인
     * **비정규화**: itemTitle, itemSummary (항목 제목/요약 중복 저장)
     * **동기화 전략**: Aurora MySQL의 Archive 엔티티 변경 시 즉시 ArchiveDocument로 동기화 (Kafka 이벤트: ArchiveCreatedEvent, ArchiveUpdatedEvent, ArchiveDeletedEvent, ArchiveRestoredEvent)
     * **동기화 매핑**: Archive.id(TSID) → ArchiveDocument.archiveTsid (1:1 매핑)
     * **동기화 동작**: 
       - 생성: ArchiveCreatedEvent → ArchiveDocument 생성
       - 수정: ArchiveUpdatedEvent → ArchiveDocument 업데이트
       - Soft Delete: ArchiveDeletedEvent → ArchiveDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
       - 복원: ArchiveRestoredEvent → ArchiveDocument 새로 생성
     * **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)
   
   - UserProfileDocument: 사용자 프로필 (읽기 최적화)
     * **필드**: _id, userTsid (Aurora MySQL User.id(TSID)와 1:1 매핑, UNIQUE), userId (UNIQUE), username (UNIQUE), email (UNIQUE), profileImageUrl
     * **인덱스**: userTsid (UNIQUE, Aurora MySQL 동기화용), userId (UNIQUE), username (UNIQUE), email (UNIQUE)
     * **쿼리 패턴**: userId, username, email로 프로필 조회, userTsid로 동기화 확인
     * **동기화 전략**: Aurora MySQL의 User 엔티티 변경 시 즉시 UserProfileDocument로 동기화 (Kafka 이벤트: UserCreatedEvent, UserUpdatedEvent, UserDeletedEvent, UserRestoredEvent)
     * **동기화 매핑**: User.id(TSID) → UserProfileDocument.userTsid (1:1 매핑)
     * **동기화 동작**: 
       - 생성: UserCreatedEvent → UserProfileDocument 생성
       - 수정: UserUpdatedEvent → UserProfileDocument 업데이트
       - Soft Delete: UserDeletedEvent → UserProfileDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
       - 복원: UserRestoredEvent → UserProfileDocument 새로 생성
     * **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)
   
   - ExceptionLogDocument: 예외 로그 (읽기/쓰기 예외 모두 기록)
     * **필드**: _id, source (READ|WRITE), exceptionType, exceptionMessage, stackTrace, context (module, method, parameters, userId, requestId), occurredAt, severity
     * **인덱스**: source + occurredAt (복합), exceptionType + occurredAt (복합), occurredAt (TTL 인덱스, 90일 후 자동 삭제)
     * **쿼리 패턴**: source별 최근 예외 조회, exceptionType별 조회

   **하위 작업 2.3: 실시간 동기화 전략 설계**
   - **역할**: 데이터 동기화 전략 설계자
   - **책임**: 
     * Aurora MySQL과 MongoDB Atlas 간 실시간 동기화 전략 수립
     * Kafka 이벤트 기반 동기화 설계
     * TSID 필드 기반 1:1 매핑 설계
     * 동기화 지연 시간 최소화 전략 수립
     * 동기화 실패 시 재시도 및 복구 전략 수립
   
   **동기화 전략**:
   - **User 엔티티 → UserProfileDocument 동기화**:
     * Aurora MySQL의 User 엔티티 생성 시: UserCreatedEvent 발행 → UserProfileDocument 생성
     * Aurora MySQL의 User 엔티티 수정 시: UserUpdatedEvent 발행 → UserProfileDocument 업데이트
     * Aurora MySQL의 User 엔티티 Soft Delete 시: UserDeletedEvent 발행 → UserProfileDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
     * Aurora MySQL의 User 엔티티 복원 시: UserRestoredEvent 발행 → UserProfileDocument 새로 생성
     * 동기화 매핑: User.id(TSID) → UserProfileDocument.userTsid (1:1 매핑)
     * 동기화 지연 시간: 실시간 동기화 목표 (1초 이내)
     * 동기화 실패 시 재시도 로직 실행 (최대 3회)
     * 재시도 실패 시 Dead Letter Queue 처리
   
   - **Archive 엔티티 → ArchiveDocument 동기화**:
     * Aurora MySQL의 Archive 엔티티 생성 시: ArchiveCreatedEvent 발행 → ArchiveDocument 생성
     * Aurora MySQL의 Archive 엔티티 수정 시: ArchiveUpdatedEvent 발행 → ArchiveDocument 업데이트
     * Aurora MySQL의 Archive 엔티티 Soft Delete 시: ArchiveDeletedEvent 발행 → ArchiveDocument 물리적 삭제 (MongoDB는 Soft Delete 미지원)
     * Aurora MySQL의 Archive 엔티티 복원 시: ArchiveRestoredEvent 발행 → ArchiveDocument 새로 생성
     * 동기화 매핑: Archive.id(TSID) → ArchiveDocument.archiveTsid (1:1 매핑)
     * 동기화 지연 시간: 실시간 동기화 목표 (1초 이내)
     * 동기화 실패 시 재시도 로직 실행 (최대 3회)
     * 재시도 실패 시 Dead Letter Queue 처리
   
   - **Kafka 이벤트 설계**:
     * UserCreatedEvent: User 엔티티 생성 시 발행 (userTsid: User.id(TSID), userId, username, email, profileImageUrl 등) → UserProfileDocument 생성
     * UserUpdatedEvent: User 엔티티 수정 시 발행 (userTsid: User.id(TSID), 변경된 필드 정보 포함) → UserProfileDocument 업데이트
     * UserDeletedEvent: User 엔티티 Soft Delete 시 발행 (userTsid: User.id(TSID), userId, deletedAt 등) → UserProfileDocument 물리적 삭제
     * UserRestoredEvent: User 엔티티 복원 시 발행 (userTsid: User.id(TSID), userId, username, email, profileImageUrl 등) → UserProfileDocument 새로 생성
     * ArchiveCreatedEvent: Archive 엔티티 생성 시 발행 (archiveTsid: Archive.id(TSID), userId, itemType, itemId, tag, memo 등) → ArchiveDocument 생성
     * ArchiveUpdatedEvent: Archive 엔티티 수정 시 발행 (archiveTsid: Archive.id(TSID), 변경된 필드 정보 포함) → ArchiveDocument 업데이트
     * ArchiveDeletedEvent: Archive 엔티티 Soft Delete 시 발행 (archiveTsid: Archive.id(TSID), userId, deletedAt 등) → ArchiveDocument 물리적 삭제
     * ArchiveRestoredEvent: Archive 엔티티 복원 시 발행 (archiveTsid: Archive.id(TSID), userId, itemType, itemId, tag, memo 등) → ArchiveDocument 새로 생성
   
   - **동기화 보장 전략**:
     * 멱등성 보장: 이벤트 ID 기반 중복 처리 방지
     * 순서 보장: Partition Key를 userId 또는 archiveTsid로 설정하여 사용자별/아카이브별 순서 보장
     * 트랜잭션 관리: DB 커밋 후 Kafka 이벤트 발행 (트랜잭션 아웃박스 패턴 고려)
     * TSID 필드 기반 매핑: userTsid, archiveTsid 필드를 통해 정확한 1:1 매핑 보장
     * 동기화 상태 모니터링: 동기화 지연 시간, 실패율 모니터링

3. API 응답 형식 정의
   
   **설계서 저장 경로**: `docs/phase2/3. api-response-format-design.md`
   
   **역할**: API 응답 형식 설계자
   **책임**: 
   - 표준 성공 응답 형식 정의
   - 에러 응답 형식 정의
   - 페이징 응답 형식 정의
   
   **검증 기준**:
   - 모든 API 응답이 일관된 형식을 따라야 함
   - 성공/에러 응답 구조가 명확히 정의되어야 함
   - 페이징 정보가 표준화되어야 함
   - 설계서가 `docs/phase2/3. api-response-format-design.md`에 저장되어야 함
   
   **하위 작업 3.1: 표준 성공 응답 형식 정의**
   - **역할**: API 응답 형식 설계자
   - **책임**: 
     * 성공 응답 구조 정의 (code, messageCode, message, data)
     * 페이징 응답 구조 정의
     * 단일 객체 응답 구조 정의
     * 빈 응답 처리 규칙 정의

   - 표준 응답 포맷:
     ```json
     {
       "code": "2000",
       "messageCode": {
         "code": "",
         "text": ""
       },
       "message": "success",
       "data": {
         "pageSize": 10,
         "pageNumber": 1,
         "totalPageNumber": 10,
         "totalSize": 100,
         "list": [...]
       }
     }
     ```
     * `code`: 응답 코드 (성공: "2000", 기타 비즈니스 코드)
     * `messageCode`: 메시지 코드 객체 (국제화 지원)
       - `code`: 메시지 코드
       - `text`: 메시지 텍스트
     * `message`: 응답 메시지 (기본: "success")
     * `data`: 응답 데이터 객체
       - `pageSize`: 페이지 크기
       - `pageNumber`: 현재 페이지 번호
       - `totalPageNumber`: 전체 페이지 수
       - `totalSize`: 전체 데이터 수
       - `list`: 데이터 리스트 배열
     * 단일 객체 응답 시: `data`에 객체 직접 포함 (페이징 정보 없음)
     * 빈 응답 시: `data`를 `null` 또는 빈 객체로 설정
   
   **하위 작업 3.2: 에러 응답 형식 정의**
   - **역할**: 에러 응답 형식 설계자
   - **책임**: 
     * 에러 응답 구조 정의 (code, messageCode)
     * 에러 코드 체계 정의
     * 국제화 지원 메시지 코드 구조 정의

   - 에러 응답:
     ```json
     {
       "code": "",
       "messageCode": {
         "code": "",
         "text": ""
       }
     }
     ```
     * `code`: 에러 코드 (예: "4000", "4001", "5000" 등)
     * `messageCode`: 에러 메시지 코드 객체
       - `code`: 에러 메시지 코드
       - `text`: 에러 메시지 텍스트 (국제화 지원)
     * HTTP 상태 코드와 별도로 비즈니스 에러 코드 사용
     * 에러 코드 체계:
       - 2xxx: 성공
       - 4xxx: 클라이언트 에러 (4000: 잘못된 요청, 4001: 인증 실패, 4003: 권한 없음, 4004: 리소스 없음 등)
       - 5xxx: 서버 에러 (5000: 내부 서버 오류, 5003: 서비스 불가 등)

4. 에러 핸들링 전략
   
   **설계서 저장 경로**: `docs/phase2/4. error-handling-strategy-design.md`
   
   **역할**: 에러 핸들링 전략 설계자
   **책임**: 
   - HTTP 상태 코드와 비즈니스 에러 코드 분리 전략 수립
   - 에러 코드 체계 정의
   - 에러 응답 형식 표준화
   
   **검증 기준**:
   - HTTP 상태 코드와 비즈니스 에러 코드가 명확히 분리되어야 함
   - 에러 코드 체계가 일관되게 정의되어야 함
   - 모든 에러 응답이 표준 형식을 따라야 함
   - 설계서가 `docs/phase2/4. error-handling-strategy-design.md`에 저장되어야 함
   
   **하위 작업 4.1: HTTP 상태 코드와 비즈니스 에러 코드 분리 전략**
   - **역할**: 에러 핸들링 전략 설계자
   - **책임**: 
     * HTTP 상태 코드와 비즈니스 에러 코드 분리 원칙 수립
     * HTTP 상태 코드 매핑 규칙 정의
     * 비즈니스 에러 코드 체계 정의

   - HTTP 상태 코드와 비즈니스 에러 코드 분리
     * HTTP 상태 코드: HTTP 프로토콜 레벨의 상태 (200, 400, 401, 403, 404, 500 등)
     * 비즈니스 에러 코드: 애플리케이션 레벨의 상세 에러 코드 (응답 body의 `code` 필드)
   
   - HTTP 상태 코드 매핑:
     * 200 OK: 성공 (비즈니스 코드: "2000")
     * 400 Bad Request: 잘못된 요청 (비즈니스 코드: "4000")
     * 401 Unauthorized: 인증 실패 (비즈니스 코드: "4001")
     * 403 Forbidden: 권한 없음 (비즈니스 코드: "4003")
     * 404 Not Found: 리소스 없음 (비즈니스 코드: "4004")
     * 429 Too Many Requests: Rate limit 초과 (비즈니스 코드: "4029")
     * 500 Internal Server Error: 서버 오류 (비즈니스 코드: "5000")
     * 503 Service Unavailable: 외부 API 장애 (비즈니스 코드: "5003")
   
   - 비즈니스 에러 코드 체계:
     * 2xxx: 성공
       - 2000: 일반 성공
     * 4xxx: 클라이언트 에러
       - 4000: 잘못된 요청
       - 4001: 인증 실패
       - 4003: 권한 없음
       - 4004: 리소스 없음
       - 4029: Rate limit 초과
     * 5xxx: 서버 에러
       - 5000: 내부 서버 오류
       - 5003: 서비스 불가 (외부 API 장애 등)
   
   - 에러 응답 예시:
     ```json
     {
       "code": "4001",
       "messageCode": {
         "code": "AUTH_FAILED",
         "text": "인증에 실패했습니다."
       }
     }
     ```

5. 설계서 검증 및 산출물 정리
   
   **설계서 저장 경로**: `docs/phase2/5. design-verification-report.md`
   
   **역할**: 설계서 검증자
   **책임**: 
   - 모든 설계서의 완전성 및 정확성 검증
   - Phase 1 설계서와의 일관성 검증
   - 검증 기준 충족 여부 확인
   - 검증 보고서 작성
   
   **검증 기준**:
   - 모든 설계서가 생성되어 `docs/phase2/` 디렉토리에 저장되어야 함
   - 각 설계서가 완전하고 정확해야 함
   - Phase 1 설계서와의 일관성이 유지되어야 함
   - 모든 검증 기준이 충족되어야 함
   - 검증 보고서가 `docs/phase2/5. design-verification-report.md`에 저장되어야 함
   
   **검증 항목**:
   1. API 엔드포인트 설계 검증
      - 모든 엔드포인트가 RESTful 원칙을 준수하는지 확인
      - 읽기/쓰기 엔드포인트가 CQRS 패턴에 맞게 분리되었는지 확인 (변경 이력 조회 및 삭제된 아카이브 조회는 예외적으로 Aurora MySQL에서 조회)
      - 모든 쓰기 작업에 Kafka 이벤트 발행이 명시되었는지 확인
      - 사용자 아카이브 API가 JWT 토큰에서 userId를 추출하여 해당 사용자의 아카이브만 타게팅되도록 설계되었는지 확인
      - 삭제된 아카이브 조회가 Aurora MySQL에서 수행되도록 설계되었는지 확인 (MongoDB는 Soft Delete 미지원)
      - 각 엔드포인트의 파라미터, 응답 형식이 명확히 정의되었는지 확인
   
   2. 데이터 모델 설계 검증
      - Command Side와 Query Side가 명확히 분리되었는지 확인
      - Command Side 엔티티가 정규화(최소 3NF)되었는지 확인
      - Query Side Document가 읽기 최적화되었는지 확인
      - 인덱스 전략이 ESR 규칙을 준수하는지 확인
      - User 엔티티 변경 시 즉시 UserProfileDocument로 동기화되는 전략이 수립되었는지 확인 (userTsid 필드 활용)
      - User 엔티티 Soft Delete 시 UserProfileDocument 물리적 삭제, 복원 시 재생성 전략이 수립되었는지 확인 (MongoDB는 Soft Delete 미지원)
      - Archive 엔티티 변경 시 즉시 ArchiveDocument로 동기화되는 전략이 수립되었는지 확인 (archiveTsid 필드 활용)
      - Archive 엔티티 Soft Delete 시 ArchiveDocument 물리적 삭제, 복원 시 재생성 전략이 수립되었는지 확인 (MongoDB는 Soft Delete 미지원)
      - Kafka 이벤트 기반 실시간 동기화 전략이 명확히 정의되었는지 확인 (UserDeletedEvent, UserRestoredEvent, ArchiveDeletedEvent, ArchiveRestoredEvent 포함)
      - Phase 1 설계서의 스키마와 일관성이 유지되는지 확인
   
   3. API 응답 형식 검증
      - 성공/에러 응답 구조가 일관되게 정의되었는지 확인
      - 페이징 응답 구조가 표준화되었는지 확인
      - 단일 객체/빈 응답 처리 규칙이 명확한지 확인
   
   4. 에러 핸들링 전략 검증
      - HTTP 상태 코드와 비즈니스 에러 코드가 분리되었는지 확인
      - 에러 코드 체계가 일관되게 정의되었는지 확인
      - 모든 에러 응답이 표준 형식을 따르는지 확인
   
   5. 설계서 문서화 검증
      - 모든 설계서가 `docs/phase2/` 디렉토리에 순서대로 저장되었는지 확인
      - 각 설계서의 내용이 완전하고 정확한지 확인
      - Phase 1 설계서와의 일관성이 유지되는지 확인
   
   **검증 보고서 작성**:
   - 검증 일시 기록
   - 각 검증 항목별 결과 기록
   - 발견된 문제점 및 개선 사항 기록
   - 검증 완료 여부 확인
   - 검증 보고서를 `docs/phase2/5. design-verification-report.md`에 저장
```

### 3단계: Common 모듈 구현

**단계 번호**: 3단계
**의존성**: 1단계 (프로젝트 구조 생성 완료 필수), 2단계 (API 설계 완료 권장)
**다음 단계**: 4단계 (Domain 모듈 구현) 또는 5단계 (사용자 인증 시스템 구현)

```
plan task: Common 모듈 구현 (공통 기능 기반 구축)

참고 파일: docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표), docs/phase1/3. aurora-schema-design.md, docs/phase1/2. mongodb-schema-design.md, docs/phase2/1. api-endpoint-design.md, docs/phase2/2. data-model-design.md, docs/phase2/3. api-response-format-design.md, docs/phase2/4. error-handling-strategy-design.md

**역할**: 공통 모듈 개발자
**책임**: 
- Common 모듈 4개 구현 (common-core, common-exception, common-security, common-kafka)
- 공통 기능 제공 및 재사용성 확보
- 빌드 검증 및 통합 테스트

**작업 요약**:
Common 모듈(common-core, common-exception, common-security, common-kafka)을 구현하여 프로젝트 전반에서 사용할 공통 기능을 제공합니다. 유틸리티 클래스, 예외 처리, 보안 기능, Kafka 이벤트 처리를 포함하며, 모든 모듈이 정상적으로 빌드되고 다른 모듈에서 import 가능해야 합니다.

**초기 해결 방안**:
1. 기존 common 모듈 구조 확인 및 필요 시 생성 (list_dir, read_file 도구 사용) 
2. common-core: 유틸리티 클래스(DateUtils, StringUtils, ValidationUtils), 상수 클래스(ApiConstants, ErrorCodeConstants), 공통 DTO(ApiResponse, MessageCode, PageData), 기본 예외 클래스(BaseException, BusinessException) 구현 
3. common-exception: 전역 예외 처리(GlobalExceptionHandler), 커스텀 예외 클래스, MongoDB Atlas 예외 로깅 시스템(ExceptionLoggingService, ExceptionContext) 구현
4. common-security: JWT 토큰 관리(JwtTokenProvider), PasswordEncoder 설정(BCrypt), Spring Security 설정(SecurityConfig, JwtAuthenticationFilter) 구현
5. common-kafka: Kafka Producer/Consumer 설정(EventPublisher, EventConsumer), 이벤트 모델 정의(UserCreatedEvent, ArchiveCreatedEvent 등) 구현
6. 각 모듈별 빌드 검증 및 통합 테스트

**프로젝트 아키텍처 파악**:
- 기존 common 모듈 구조 확인 (list_dir, read_file 도구 사용)
  * common/ 디렉토리 구조 확인
  * 각 모듈의 build.gradle 파일 확인
  * 기존 패키지 구조 확인 (com.tech.n.ai.common.*)
- 모듈 간 의존성 확인 (build.gradle 파일 검토)
  * 다른 모듈에서 common 모듈 사용 여부 확인
  * 의존성 방향 검증 (API → Domain → Common → Client)
- 프로젝트 전체 아키텍처 패턴 확인
  * CQRS 패턴 적용 여부 확인
  * MSA 멀티모듈 구조 확인

**기존 코드 확인**:
- common 모듈의 기존 구현 확인 (있는 경우)
  * codebase_search로 기존 유틸리티 클래스 패턴 확인
  * 기존 예외 처리 방식 확인
  * 기존 보안 설정 확인
- 다른 모듈에서 사용 중인 공통 기능 패턴 확인
  * codebase_search로 유사 기능 구현 패턴 검색
  * 기존 DTO 구조 확인
  * 기존 상수 정의 패턴 확인
- 기존 예외 처리 방식 확인
  * GlobalExceptionHandler 존재 여부 확인
  * 커스텀 예외 클래스 패턴 확인
- 기존 보안 설정 확인
  * JWT 토큰 관리 구현 여부 확인
  * Spring Security 설정 확인
- 기존 Kafka 설정 확인
  * Kafka Producer/Consumer 구현 여부 확인
  * 이벤트 모델 정의 확인

**정보 수집**:
- 불확실한 부분은 codebase_search, read_file 도구 사용하여 정보 수집
  * 기존 구현 패턴이 있는 경우 해당 패턴을 따름
  * 공식 문서 참조 시 URL 명시 (현재 잘 되어 있음)
- 기존 구현 패턴 확인 후 설계 결정
  * 기존 코드 스타일과 일관성 유지
  * 기존 아키텍처 패턴 준수
- 공식 문서 참조 우선순위
  1. Spring Framework, Spring Security, Spring Kafka 공식 문서
  2. Apache Kafka, JWT (RFC 7519) 공식 문서
  3. Java 공식 문서
  4. 기타 공식 문서 (Jakarta Bean Validation 등)
- 추측 금지: 모든 정보는 도구를 사용하여 실제 코드베이스에서 확인하거나 공식 문서에서 참조

**핵심 제한사항 (절대 준수 필수)**:
1. **공식 개발문서만 참고**: Spring Framework, Spring Security, Spring Kafka, Apache Kafka, JWT (RFC 7519), Java 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 단일 구현체를 위한 인터페이스, 불필요한 추상화, "나중을 위해" 추가하는 코드 모두 금지.
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.

작업 내용:
1. common-core 모듈 구현
   - **역할**: 핵심 유틸리티 및 공통 기능 제공
   - **책임**: 
     * 유틸리티 클래스 구현
     * 상수 클래스 정의
     * 공통 DTO 정의
     * 기본 예외 클래스 정의
   - **검증 기준**: 
     * common-core 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-core:build` 명령이 성공해야 함)
     * 다른 모듈에서 import 가능해야 함
     * 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
     * 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   유틸리티 클래스 구현 (YAGNI 원칙 준수, 실제 필요한 기능만 구현):
   - DateUtils: 날짜/시간 유틸리티
     * **참고**: Java 8+ java.time 패키지 공식 문서 (https://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
     * LocalDate, LocalDateTime 포맷팅 및 파싱 (DateTimeFormatter 사용)
     * **주의**: 타임존 변환은 실제 요구사항에 명시된 경우에만 구현
     * **주의**: 날짜 범위 검증은 실제 사용되는 API에만 구현
   - StringUtils: 문자열 처리 유틸리티
     * **참고**: 
       - Apache Commons Lang StringUtils 공식 문서 (https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/StringUtils.html)
       - Java String API 공식 문서 (https://docs.oracle.com/javase/8/docs/api/java/lang/String.html)
     * 빈 문자열 검증 (isEmpty, isBlank)
     * 문자열 트림 및 정제
     * **주의**: 암호화/복호화는 common-security 모듈에서 처리, 여기서는 단순 유틸리티만
   - ValidationUtils: 데이터 검증 유틸리티
     * **참고**: Jakarta Bean Validation 공식 문서 (https://beanvalidation.org/)
     * 이메일 형식 검증 (정규식 또는 Jakarta Validation 사용)
     * 비밀번호 강도 검증 (실제 요구사항에 명시된 규칙만)
     * 입력값 null/empty 검증
   
   상수 클래스 정의:
   - ApiConstants: API 관련 상수
     * API 버전 상수
     * 엔드포인트 경로 상수
     * HTTP 헤더 상수
   - ErrorCodeConstants: 에러 코드 상수
     * 성공 코드: "2000"
     * 클라이언트 에러 코드: "4000", "4001", "4002", "4003", "4004", "4005", "4006", "4029"
       - 4000: BAD_REQUEST (잘못된 요청)
       - 4001: AUTH_FAILED (인증 실패)
       - 4002: AUTH_REQUIRED (인증 필요)
       - 4003: FORBIDDEN (권한 없음)
       - 4004: NOT_FOUND (리소스 없음)
       - 4005: CONFLICT (충돌)
       - 4006: VALIDATION_ERROR (유효성 검증 실패)
       - 4029: RATE_LIMIT_EXCEEDED (Rate limit 초과)
     * 서버 에러 코드: "5000", "5001", "5002", "5003", "5004"
       - 5000: INTERNAL_SERVER_ERROR (내부 서버 오류)
       - 5001: DATABASE_ERROR (데이터베이스 오류)
       - 5002: EXTERNAL_API_ERROR (외부 API 오류)
       - 5003: SERVICE_UNAVAILABLE (서비스 불가)
       - 5004: TIMEOUT (타임아웃)
     * **참고**: docs/phase2/4. error-handling-strategy-design.md
   
   공통 DTO 정의:
   - ApiResponse<T>: 표준 API 응답 래퍼
     * 구조: code, messageCode (MessageCode 객체), message, data (T)
     * 제네릭 타입 T는 실제 응답 데이터 타입
     * **성공 응답**: message 필드 필수 ("success" 기본값)
     * **에러 응답**: message 필드 없음 (에러 응답에는 messageCode만 포함)
     * **참고**: docs/phase2/3. api-response-format-design.md
   - MessageCode: 메시지 코드 객체
     * 구조: code (String), text (String)
     * 국제화(i18n) 지원을 위한 메시지 코드
   - PageData<T>: 페이징 데이터 객체
     * 구조: pageSize, pageNumber, totalPageNumber, totalSize, list (List<T>)
     * 페이징이 필요한 리스트 응답에 사용
   
   예외 클래스 정의:
   - BaseException: 기본 예외 클래스
     * 모든 커스텀 예외의 부모 클래스
     * 에러 코드 및 메시지 코드 포함
   - BusinessException: 비즈니스 예외
     * 비즈니스 로직 위반 시 사용
     * BaseException 상속

2. common-exception 모듈 구현
   - **역할**: 전역 예외 처리 및 커스텀 예외 제공
   - **책임**: 
     * GlobalExceptionHandler 구현
     * 커스텀 예외 클래스 정의
     * 예외 로깅 시스템 구현
   - **검증 기준**: 
     * common-exception 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-exception:build` 명령이 성공해야 함)
     * 예외 처리가 정상적으로 동작해야 함
     * 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
     * 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   GlobalExceptionHandler 구현:
   - 모든 예외를 일관된 형식으로 처리
   - HTTP 상태 코드와 비즈니스 에러 코드 매핑
   - 예외 응답 형식: { code, messageCode: { code, text } }
   - 로깅 및 모니터링 통합
   
   커스텀 예외 클래스:
   - ResourceNotFoundException: 리소스를 찾을 수 없을 때
     * HTTP 404, 비즈니스 코드 "4004"
   - ExternalApiException: 외부 API 호출 실패
     * HTTP 503, 비즈니스 코드 "5003"
   - RateLimitExceededException: Rate limit 초과
     * HTTP 429, 비즈니스 코드 "4029"
   - UnauthorizedException: 인증 실패
     * HTTP 401, 비즈니스 코드 "4001"
   - ForbiddenException: 권한 없음
     * HTTP 403, 비즈니스 코드 "4003"
   
   예외 로깅 시스템 (MongoDB Atlas 저장):
   - **참고**: Spring Data MongoDB 공식 문서 (https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
   - ExceptionLoggingService 구현
     * 위치: `common/exception/src/main/java/com/ebson/shrimp/tm/demo/common/exception/ExceptionLoggingService.java`
     * 비동기 처리: @Async 사용 (Spring 공식 문서 참고)
     * 실패 시 대체 로깅: MongoDB 저장 실패 시 로컬 로그 파일에 기록 (SLF4J 사용)
     * **주의**: 멱등성 보장은 실제 중복 저장 문제가 발생한 경우에만 구현 (YAGNI 원칙)
   - ExceptionContext 클래스 정의
     * 위치: `common/exception/src/main/java/com/ebson/shrimp/tm/demo/common/exception/ExceptionContext.java`
     * 예외 발생 컨텍스트 정보 수집
     * 필드:
       - source: String ("READ" 또는 "WRITE")
       - exceptionType: String (예외 타입, 예: "DataAccessException", "ValidationException")
       - exceptionMessage: String (예외 메시지)
       - stackTrace: String (스택 트레이스 전체)
       - context: Object (컨텍스트 정보)
         * module: String (모듈명)
         * method: String (메서드명)
         * parameters: Object (파라미터 정보)
         * userId: String (사용자 ID, nullable)
         * requestId: String (요청 ID, nullable)
       - occurredAt: Instant (발생 일시)
       - severity: String ("LOW", "MEDIUM", "HIGH", "CRITICAL")
     * **참고**: docs/phase1/2. mongodb-schema-design.md (ExceptionLogDocument)
     * **주의**: 설계서의 ExceptionLogDocument 구조와 일치하도록 구현
   - 읽기/쓰기 예외 구분 로깅
     * logReadException(): MongoDB Atlas 읽기 예외 저장
     * logWriteException(): Amazon Aurora MySQL 쓰기 예외 저장
   - GlobalExceptionHandler 확장
     * 모든 예외 처리 시 ExceptionLoggingService 호출
     * 읽기 예외와 쓰기 예외를 구분하여 로깅
   - **주의**: AOP 기반 예외 인터셉터는 실제 필요성이 확인된 경우에만 구현 (YAGNI 원칙)
     * 현재는 GlobalExceptionHandler로 충분한지 먼저 검토
     * AOP 구현 시 Spring AOP 공식 문서 참고 (https://docs.spring.io/spring-framework/reference/core/aop.html)

3. common-security 모듈 구현
   - **역할**: 보안 관련 공통 기능 제공
   - **책임**: 
     * JWT 토큰 관리
     * PasswordEncoder 설정
     * Security 설정 유틸리티
   - **검증 기준**: 
     * common-security 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-security:build` 명령이 성공해야 함)
     * JWT 토큰 생성/검증이 정상적으로 동작해야 함
     * 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
     * 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   JWT 토큰 관리:
   - **참고**: JWT 공식 스펙 (RFC 7519: https://tools.ietf.org/html/rfc7519), jjwt 라이브러리 공식 문서 (https://github.com/jwtk/jjwt)
   - JwtTokenProvider 구현
     * 토큰 생성, 검증, 갱신 기능 (jjwt 라이브러리 사용)
     * Access Token: 짧은 만료 시간 (15분)
     * Refresh Token: 긴 만료 시간 (7일), Redis 저장
     * 토큰 페이로드: userId, email, role (실제 필요한 필드만)
     * JWT Secret Key는 환경 변수로 관리 (JWT_SECRET_KEY)
     * **주의**: 공식 문서의 예제 코드 패턴만 사용, 불필요한 추상화 금지
   
   PasswordEncoder 설정:
   - **참고**: Spring Security PasswordEncoder 공식 문서 (https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
   - BCrypt 사용 (salt rounds: 12)
   - PasswordEncoder 빈 등록 (BCryptPasswordEncoder)
   - 비밀번호 암호화/검증 유틸리티 제공
   - **주의**: Spring Security의 표준 BCryptPasswordEncoder 사용, 커스텀 구현 금지
   
   Security 설정 유틸리티:
   - **참고**: Spring Security 공식 문서 (https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
   - SecurityConfig 기본 설정
     * 인증/인가 규칙 설정 (HttpSecurity 사용)
     * CORS 설정 유틸리티 (CorsConfigurationSource 사용)
     * CSRF 보호 설정 (실제 요구사항에 맞게)
   - JwtAuthenticationFilter 기본 구현
     * JWT 토큰 검증 필터 (OncePerRequestFilter 상속)
     * 인증 실패 시 적절한 에러 응답
     * **주의**: Spring Security의 표준 필터 체인 패턴 사용
   - Rate Limiting 유틸리티
     * **참고**: 
       - Spring Security Rate Limiter 공식 문서 (https://docs.spring.io/spring-security/reference/features/authentication/rate-limiting.html)
       - Redis 공식 문서 (https://redis.io/docs/)
     * 로그인 시도 제한 (IP 기반, Redis 사용)
     * 계정 잠금 기능 (5회 실패 시 30분 잠금)
     * **주의**: 실제 요구사항에 명시된 경우에만 구현 (YAGNI 원칙)

4. common-kafka 모듈 구현
   - **역할**: Kafka Producer/Consumer 공통 기능 제공
   - **책임**: 
     * Kafka Producer 설정
     * Kafka Consumer 설정
     * 이벤트 모델 정의
   - **검증 기준**: 
     * common-kafka 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-kafka:build` 명령이 성공해야 함)
     * Kafka Producer/Consumer가 정상적으로 동작해야 함
     * 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
     * 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   Kafka Producer 설정:
   - **참고**: Spring Kafka 공식 문서 (https://docs.spring.io/spring-kafka/reference/html/), Apache Kafka Producer API 공식 문서 (https://kafka.apache.org/documentation/#producerapi)
   - EventPublisher 서비스 구현
     * 모든 Command 작업 후 이벤트 발행 (KafkaTemplate 사용)
     * 트랜잭션 관리: DB 커밋 후 이벤트 발행
     * **주의**: Outbox 패턴은 실제 트랜잭션 문제가 발생한 경우에만 고려 (YAGNI 원칙)
     * 이벤트 순서 보장: Partition Key 사용 (userId, archiveId 등)
     * 에러 핸들링: 발행 실패 시 재시도 로직 (Spring Kafka의 기본 재시도 메커니즘 사용)
   
   Kafka Consumer 설정:
   - **참고**: Spring Kafka 공식 문서 (https://docs.spring.io/spring-kafka/reference/html/), Apache Kafka Consumer API 공식 문서 (https://kafka.apache.org/documentation/#consumerapi)
   - EventConsumer 서비스 구현
     * 이벤트 수신 및 처리 (@KafkaListener 사용)
     * 멱등성 보장: 이벤트 ID 기반 중복 처리 방지 (Redis 사용)
     * 에러 핸들링 및 재시도 로직 (Spring Kafka의 기본 재시도 메커니즘 사용)
     * Dead Letter Queue (DLQ) 처리 (실제 필요성이 확인된 경우에만)
     * Consumer 그룹 관리 (Spring Kafka의 기본 메커니즘 사용)
     * **주의**: 공식 문서의 표준 패턴만 사용, 불필요한 추상화 금지
   
   이벤트 모델 정의:
   - UserCreatedEvent: 사용자 생성 이벤트
   - UserUpdatedEvent: 사용자 업데이트 이벤트
   - UserDeletedEvent: 사용자 삭제 이벤트 (Soft Delete)
   - UserRestoredEvent: 사용자 복원 이벤트
   - ArchiveCreatedEvent: 아카이브 생성 이벤트
   - ArchiveUpdatedEvent: 아카이브 수정 이벤트
   - ArchiveDeletedEvent: 아카이브 삭제 이벤트 (Soft Delete)
   - ArchiveRestoredEvent: 아카이브 복원 이벤트
   - 모든 이벤트는 다음 공통 필드를 포함해야 함:
     * eventId: String (UUID 형식, 고유 식별자)
     * eventType: String (이벤트 타입, 예: "USER_CREATED", "ARCHIVE_CREATED")
     * timestamp: Instant (이벤트 발생 시각)
     * payload: Object (이벤트별 페이로드 데이터, 제네릭 타입)
       - userId는 payload 내부에 포함 (nullable)
   - 모든 이벤트는 직렬화 가능하도록 구현 (Jackson 사용)
   - **참고**: 
     * docs/phase2/1. api-endpoint-design.md (Kafka 이벤트 발행 전략)
     * docs/phase2/2. data-model-design.md (Kafka 이벤트 설계, 라인 708-861)

**의존성**: 1단계 (프로젝트 구조 생성 완료 필요), 2단계 (API 설계 완료 권장)
**다음 단계**: 4단계 (Domain 모듈 구현) 또는 5단계 (사용자 인증 시스템 구현)
```

### 4단계: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)

**단계 번호**: 4단계
**의존성**: 1단계 (프로젝트 구조 생성), 2단계 (API 설계 완료 권장), 3단계 (Common 모듈 구현 완료 필수)
**다음 단계**: 5단계 (사용자 인증 시스템 구현) 또는 8단계 (Client 모듈 구현)

```
plan task: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)

**Description (작업 설명)**:
CQRS 패턴 기반 아키텍처에서 Command Side와 Query Side 데이터 접근 계층을 구현합니다.

- **목표**: domain-aurora(Aurora MySQL)와 domain-mongodb(MongoDB Atlas) 모듈 완성
- **배경**: 현재 프로젝트는 CQRS 패턴을 채택하여 Command/Query 분리가 필요함
- **예상 결과**: 두 모듈 모두 독립적으로 빌드 가능하며, 기본 CRUD 동작이 검증됨

- **기술적 도전 과제 및 핵심 의사결정 포인트**:
  * TSID Primary Key 전략 구현 (애플리케이션 레벨 생성)
  * Profile 기반 설정 분리 (API Domain / Batch Domain)
  * 스키마 간 Foreign Key 제약조건 미지원에 대한 애플리케이션 레벨 처리
  * Soft Delete 패턴 구현 및 히스토리 자동 저장 메커니즘
  * CQRS 동기화 전략 (Kafka 이벤트 기반)
  * MongoDB 인덱스 전략 (ESR 규칙 준수)

- **기존 시스템/아키텍처와의 통합 요구사항**:
  * 기존 프로젝트 구조 및 모듈 경계 준수
  * 기존 코드 스타일 및 네이밍 컨벤션 준수
  * 기존 설정 파일 구조 및 Profile 관리 방식 준수
  * 기존 트랜잭션 관리 패턴 및 에러 처리 방식 준수
  * 기존 인덱스 전략 및 성능 최적화 원칙 준수

**Requirements (기술 요구사항 및 제약사항)**:
1. **공식 개발문서만 참고**: 
   - Spring Data JPA 공식 문서: https://docs.spring.io/spring-data/jpa/reference/
   - Spring Data MongoDB 공식 문서: https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
   - Hibernate 공식 문서: https://hibernate.org/orm/documentation/
   - MyBatis 공식 문서: https://mybatis.org/mybatis-3/
   - 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 단일 구현체를 위한 인터페이스, 불필요한 추상화, "나중을 위해" 추가하는 코드 모두 금지.
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.
4. **기존 패턴 준수 및 확인** (Database Operations 가이드라인):
   - 기존 데이터 접근 패턴 및 추상화 레이어 분석 필수
   - 기존 쿼리 빌딩 및 트랜잭션 처리 방식 확인 필수
   - 기존 관계 처리 및 데이터 검증 방법 이해 필수
   - 기존 캐싱 전략 및 성능 최적화 기법 확인 필수
   - 기존 코드 스타일 및 네이밍 컨벤션 준수 필수

**참고 문서**:
- docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표)
- docs/phase1/3. aurora-schema-design.md (Aurora 스키마 설계)
- docs/phase1/2. mongodb-schema-design.md (MongoDB 스키마 설계)
- docs/phase2/1. api-endpoint-design.md (API 엔드포인트 설계)
- docs/phase2/2. data-model-design.md (데이터 모델 설계)

**참고 문서 활용 방법**:
- 각 참고 문서를 읽고 관련 내용을 확인한 후 작업 진행
- 참고 문서의 설계 의도 및 제약사항을 반드시 준수
- 참고 문서에 명시된 스키마 설계, 데이터 모델 설계를 정확히 구현

**작업 범위 및 우선순위**:
1. domain-aurora 모듈 구현 (우선순위: 높음, 의존성: 없음)
2. domain-mongodb 모듈 구현 (우선순위: 높음, 의존성: 없음)
3. 변경 이력 추적 시스템 구현 (우선순위: 중간, 의존성: domain-aurora 모듈)

**작업 간 관계**:
- domain-aurora와 domain-mongodb는 독립적으로 구현 가능
- 변경 이력 추적 시스템은 domain-aurora 모듈의 엔티티가 완성된 후 구현

**중요: 작업 전 필수 확인 사항**:
- 기존 프로젝트 구조 및 모듈 경계 확인 (`settings.gradle`, `build.gradle` 등)
- 기존 도메인 모듈 구조 확인 (있다면)
- 기존 설정 파일 구조 및 네이밍 컨벤션 확인
- 기존 엔티티/Document 패턴 및 스타일 확인
- 기존 Repository 패턴 및 쿼리 작성 방식 확인
- 기존 트랜잭션 관리 및 에러 처리 패턴 확인
- 기존 인덱스 전략 및 성능 최적화 방법 확인

작업 내용:
1. domain-aurora 모듈 구현 (엔티티, Repository, Profile 기반 설정 분리)
   - **역할**: Command Side 데이터 접근 계층
   - **책임**: 
     * JPA 엔티티 정의 및 매핑
     * Repository 인터페이스 및 커스텀 쿼리 구현
     * 트랜잭션 관리
     * API Domain / Batch Domain 설정 분리 (Profile 기반)
   - **검증 기준**: 
     * 모든 엔티티가 정상적으로 저장/조회/수정/삭제 가능해야 함
     * 트랜잭션이 정상적으로 롤백 가능해야 함
     * Profile별 설정이 정상적으로 로드되어야 함
     * Reader/Writer DataSource가 정상적으로 분리되어야 함
     * TSID Primary Key가 정상적으로 생성되어야 함
     * Soft Delete가 정상적으로 동작해야 함
     * 히스토리 자동 저장이 정상적으로 동작해야 함
     * **API 모듈별 스키마 매핑 검증**:
       - `api-auth` 모듈이 `auth` 스키마에 정상적으로 연결되어야 함
       - `api-archive` 모듈이 `archive` 스키마에 정상적으로 연결되어야 함
       - 각 모듈의 `module.aurora.schema` 설정이 `application-api-domain.yml`의 `${module.aurora.schema}`에 정상적으로 반영되어야 함
       - 환경변수(`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`)가 정상적으로 로드되어야 함
       - 로컬 환경에서 `.env` 파일을 통한 환경변수 로드가 정상적으로 동작해야 함
     * **빌드 검증**: domain-aurora 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/), Hibernate 공식 문서 (https://hibernate.org/orm/documentation/)
   - **패키지 구조**: domain/aurora/config/, domain/aurora/entity/, domain/aurora/repository/reader/, domain/aurora/repository/writer/
   
   - **스키마별 엔티티 정의**:
     * **auth 스키마** (api-auth 모듈):
       - Provider 엔티티: OAuth 제공자 정보 (id, name, display_name, client_id, client_secret, is_enabled 등)
       - User 엔티티: 사용자 정보 (id, email, username, password, provider_id, provider_user_id 등)
       - Admin 엔티티: 관리자 정보 (id, email, username, password, role, is_active, last_login_at 등)
       - RefreshToken 엔티티: JWT Refresh Token (id, user_id, token, expires_at 등)
       - EmailVerification 엔티티: 이메일 인증 정보 (id, email, token, expires_at, verified_at 등)
       - UserHistory 엔티티: 사용자 변경 이력 (history_id, user_id, operation_type, before_data, after_data, changed_by, changed_at, change_reason)
       - AdminHistory 엔티티: 관리자 변경 이력 (history_id, admin_id, operation_type, before_data, after_data, changed_by, changed_at, change_reason)
     * **archive 스키마** (api-archive 모듈):
       - Archive 엔티티: 사용자 아카이브 정보 (id, user_id, item_type, item_id, tag, memo 등)
         - **중요**: `user_id` 필드는 `auth` 스키마의 `users` 테이블을 참조하지만, MySQL은 스키마 간 Foreign Key를 지원하지 않으므로 애플리케이션 레벨에서 참조 무결성을 보장해야 함
         - `user_id + item_type + item_id` UNIQUE 제약조건 (중복 아카이브 방지, Soft Delete 제외 시)
       - ArchiveHistory 엔티티: 아카이브 변경 이력 (history_id, archive_id, operation_type, before_data, after_data, changed_by, changed_at, change_reason)
     * **주의사항**: 
       - `api-contest`, `api-news` 모듈은 Aurora DB를 사용하지 않으므로 해당 스키마의 엔티티는 존재하지 않음
       - Contest와 NewsArticle 데이터는 MongoDB Atlas에만 저장됨 (읽기 전용 데이터)
       - Source는 `json/sources.json` 파일 기반으로 관리되며 Aurora MySQL에 Source 엔티티가 없음
     * 참고: docs/phase1/3. aurora-schema-design.md
   
   - **TSID Primary Key 전략**:
     * 모든 엔티티의 Primary Key는 TSID 방식(BIGINT UNSIGNED) 사용
     * 커스텀 어노테이션 `@Tsid` 사용
     * TSID 생성기 구현 (애플리케이션 레벨 생성)
     * 참고: docs/reference/shrimp-task-prompts-final-goal.md, docs/phase1/3. aurora-schema-design.md
   
   - **Soft Delete 구현**:
     * 모든 메인 엔티티에 delete_yn (CHAR(1), 기본값 'N'), deleted_at (TIMESTAMP(6)), deleted_by (BIGINT UNSIGNED) 필드 포함
     * 히스토리 테이블의 operation_type='DELETE'는 Soft Delete를 의미 (실제 SQL DELETE 아님)
     * 참고: docs/phase1/3. aurora-schema-design.md
   
   - **API Domain 설정** (@Profile("api-domain")):
     * **역할**: API 서버용 데이터베이스 설정
     * **책임**: 
       - API Writer/Reader DataSource 분리
       - API JPA 설정
       - API MyBatis 설정 (복잡한 조회 쿼리 전용)
       - API 모듈별 스키마 동적 매핑
     * **Config 클래스**: ApiDomainConfig, ApiDataSourceConfig, ApiMybatisConfig
     * **설정 파일**: application-api-domain.yml
     * **환경변수 관리**:
       - Aurora DB Cluster 접속 정보는 환경변수로 관리 (보안성 및 환경별 설정 분리)
       - 필수 환경변수: `AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`
       - 로컬 환경에서는 `.env` 파일 사용 (`.gitignore`에 포함되어야 함)
       - 프로덕션 환경에서는 AWS Secrets Manager, Parameter Store 등 활용
       - **JDBC 연결 옵션** (`AURORA_OPTIONS` 환경변수):
         - `useSSL=true`: SSL 연결 활성화
         - `serverTimezone=Asia/Seoul`: 서버 타임존 설정
         - `characterEncoding=UTF-8`: 문자 인코딩 설정
         - `rewriteBatchedStatements=true`: 배치 문 재작성 (성능 향상)
         - `cachePrepStmts=true`: Prepared Statement 캐싱
         - 기타 필요한 옵션들
     * **API 모듈별 스키마 매핑**:
       - **설정 구조**:
         1. 각 API 모듈의 `api-*-application.yml` 파일에서 `module.aurora.schema` 속성 설정
         2. `domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 환경변수를 사용하여 동적으로 스키마 참조
       - **스키마 매핑 설정**:
         - `api-auth` 모듈: `api-auth-application.yml`에서 `module.aurora.schema=auth` 설정 → `auth` 스키마 사용
         - `api-archive` 모듈: `api-archive-application.yml`에서 `module.aurora.schema=archive` 설정 → `archive` 스키마 사용
         - `api-contest`, `api-news` 모듈: Aurora DB 미사용 (MongoDB Atlas 사용)
       - **동적 스키마 참조**:
         - `application-api-domain.yml`에서 `${module.aurora.schema}` 환경변수를 사용하여 동적으로 스키마 참조
         - DataSource URL 형식: `jdbc:mysql://${AURORA_WRITER_ENDPOINT}:3306/${module.aurora.schema}?${AURORA_OPTIONS}`
         - 각 모듈은 자신이 설정한 스키마에 자동으로 연결됨
       - **설정 흐름**:
         1. 각 API 모듈의 `api-*-application.yml`에서 `module.aurora.schema` 값 설정
         2. Spring Boot가 설정 파일들을 로드할 때 `module.aurora.schema` 값을 읽음
         3. `application-api-domain.yml`의 `${module.aurora.schema}`가 실제 스키마명으로 치환됨
         4. 각 모듈은 자신이 설정한 스키마에 연결됨
     * **스키마별 관리 테이블**:
       - `auth` 스키마 (api-auth 모듈): providers, users, admins, refresh_tokens, email_verifications, user_history, admin_history
       - `archive` 스키마 (api-archive 모듈): archives, archive_history
       - **스키마 간 Foreign Key 제약조건 미지원**:
         - `archives` 테이블의 `user_id`는 `auth` 스키마의 `users` 테이블을 참조하지만, MySQL은 스키마 간 Foreign Key 제약조건을 지원하지 않음
         - **애플리케이션 레벨 처리**:
           - `user_id` 값의 유효성 검증은 애플리케이션 레벨에서 수행해야 함
           - `users` 테이블의 레코드 삭제 시 관련 `archives` 레코드 처리도 애플리케이션 레벨에서 관리해야 함
           - Repository 또는 Service 레이어에서 참조 무결성 검증 로직 구현 필요
     * 참고: docs/reference/shrimp-task-prompts-final-goal.md, docs/phase1/3. aurora-schema-design.md
   
   - **Batch Domain 설정** (@Profile("batch-domain")):
     * **역할**: Batch 서버용 데이터베이스 설정
     * **책임**: 
       - Batch Meta DataSource (Spring Batch 메타데이터용)
       - Batch Business Writer/Reader DataSource 분리
       - Batch JPA 설정
       - Batch MyBatis 설정 (복잡한 조회 쿼리 전용)
       - Batch Transaction Manager 설정
     * **Config 클래스**: BatchDomainConfig, BatchMetaDataSourceConfig, BatchBusinessDataSourceConfig, BatchEntityManagerConfig, BatchJpaTransactionConfig, BatchMyBatisConfig
     * **설정 파일**: application-batch-domain.yml
     * 참고: docs/reference/shrimp-task-prompts-final-goal.md
   
   - **MyBatis 사용 제한 정책**:
     * **사용 허용 범위**: 복잡한 조회 쿼리에만 제한적으로 사용
       - 데이터베이스 종속 함수를 사용해야 하는 경우 (예: MySQL의 DATE_FORMAT, JSON 함수 등)
       - 인라인 뷰 서브쿼리를 사용하는 복잡한 조회 쿼리
       - 성능 최적화가 필요한 복잡한 조회 쿼리 (예: 대량 데이터 조회, 복잡한 JOIN 등)
     * **사용 금지 범위**:
       - 모든 쓰기 작업 (INSERT, UPDATE, DELETE) - JPA Entity를 통해서만 수행
       - 단순 조회 쿼리 - JPA Repository 또는 QueryDSL 사용
       - CRUD 기본 작업 - JPA Repository 사용
     * 참고: docs/reference/shrimp-task-prompts-final-goal.md
   
   - **인덱스 전략**:
     * Command Side 인덱스 최소화 원칙 (쓰기 성능 최적화)
     * 필수 인덱스만 생성 (UNIQUE 제약조건, 외래 키, Soft Delete 인덱스)
     * 읽기 최적화 인덱스는 Query Side(MongoDB Atlas)에서 처리
     * 참고: docs/phase1/3. aurora-schema-design.md

2. domain-mongodb 모듈 구현 (Document, Repository)
   - **역할**: Query Side 데이터 접근 계층
   - **책임**: 
     * MongoDB Atlas Document 정의
     * Repository 인터페이스 구현
     * 읽기 최적화 쿼리 구현
   - **검증 기준**: 
     * 모든 Document가 정상적으로 저장/조회 가능해야 함
     * 인덱스가 정상적으로 생성되어야 함
     * ESR 규칙을 준수한 인덱스 설계 확인
     * 프로젝션 최적화가 정상적으로 동작해야 함
     * **빌드 검증**: domain-mongodb 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-mongodb:build` 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data MongoDB 공식 문서 (https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
   - **Document 정의**: 
     * SourcesDocument: 정보 출처 정보 (name, type, category, url, apiEndpoint, rssFeedUrl, priority 등)
     * ContestDocument: 개발자 대회 정보 (sourceId, title, startDate, endDate, status, description, url, metadata 등)
     * NewsArticleDocument: IT 테크 뉴스 기사 (sourceId, title, content, summary, publishedAt, url, author, metadata 등)
     * ArchiveDocument: 사용자 아카이브 정보 (archiveTsid, userId, itemType, itemId, itemTitle, itemSummary, tag, memo, archivedAt 등)
       - **중요**: `archiveTsid` 필드는 Aurora MySQL Archive.id(TSID)와 1:1 매핑 (UNIQUE 인덱스)
     * UserProfileDocument: 사용자 프로필 정보 (userTsid, userId, username, email, profileImageUrl 등)
       - **중요**: `userTsid` 필드는 Aurora MySQL User.id(TSID)와 1:1 매핑 (UNIQUE 인덱스)
     * ExceptionLogDocument: 예외 로그 (source, exceptionType, exceptionMessage, stackTrace, context, occurredAt, severity 등)
     * 참고: docs/phase1/2. mongodb-schema-design.md
   - **인덱스 전략**: 
     * ESR 규칙(Equality → Sort → Range) 준수
     * 쿼리 패턴 기반 인덱스 설계
     * TTL 인덱스 사용 (news_articles, exception_logs)
     * 참고: docs/phase1/2. mongodb-schema-design.md
   
   - **CQRS 동기화 전략**:
     * **ArchiveDocument.archiveTsid**: Aurora MySQL Archive.id(TSID)와 1:1 매핑 (UNIQUE 인덱스)
       - Archive 엔티티 생성/수정/삭제 시 Kafka 이벤트 발행 → ArchiveDocument 동기화
       - 동기화 지연 시간: 1초 이내 목표
     * **UserProfileDocument.userTsid**: Aurora MySQL User.id(TSID)와 1:1 매핑 (UNIQUE 인덱스)
       - User 엔티티 생성/수정/삭제 시 Kafka 이벤트 발행 → UserProfileDocument 동기화
       - 동기화 지연 시간: 1초 이내 목표
     * **MongoDB Soft Delete 미지원**:
       - MongoDB Atlas는 Soft Delete를 지원하지 않으므로, Soft Delete 시 Document는 물리적으로 삭제됨
       - 복원 시 Document를 새로 생성해야 함
       - ArchiveDeletedEvent, UserDeletedEvent 발행 시 MongoDB Atlas에서 Document 물리적 삭제
       - ArchiveRestoredEvent, UserRestoredEvent 발행 시 MongoDB Atlas에서 Document 새로 생성
     * 참고: docs/phase2/2. data-model-design.md

3. 변경 이력 추적 시스템 구현 (히스토리 테이블)
   - **역할**: 모든 쓰기 작업에 대한 변경 이력 추적
   - **책임**: 
     * 히스토리 테이블 설계 및 구현
     * 변경 이력 자동 저장
   - **검증 기준**: 
     * 모든 쓰기 작업에 대해 히스토리가 자동으로 저장되어야 함
     * 히스토리 테이블의 operation_type='DELETE'는 Soft Delete를 의미함을 확인
     * **빌드 검증**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **변경 이력 자동 저장 구현**:
     * JPA Entity Listener 패턴 사용 (@PrePersist, @PreUpdate, @PreRemove)
     * 또는 Hibernate Envers 라이브러리 활용
     * 트랜잭션 내에서 원본 데이터와 함께 저장
     * 참고: docs/reference/shrimp-task-prompts-final-goal.md, Hibernate Envers 공식 문서
   
   - **히스토리 테이블**:
     * **auth 스키마** (api-auth 모듈):
       - UserHistory: 사용자 변경 이력 (users 테이블 참조)
       - AdminHistory: 관리자 변경 이력 (admins 테이블 참조)
     * **archive 스키마** (api-archive 모듈):
       - ArchiveHistory: 아카이브 변경 이력 (archives 테이블 참조)
     * **주의**: ContestHistory, NewsArticleHistory, SourceHistory는 불필요함
       - Contest와 NewsArticle은 읽기 전용 데이터 (Query Side: MongoDB Atlas)
       - Command Side(Aurora MySQL)에는 Contest와 NewsArticle 엔티티가 존재하지 않음
       - Source는 json/sources.json 파일 기반으로 관리되며 Aurora MySQL에 Source 엔티티가 없음
     * **히스토리 테이블 공통 필드 구조**:
       - `history_id` (BIGINT UNSIGNED, TSID Primary Key): 히스토리 레코드 ID
       - `entity_id` 또는 `{entity}_id` (BIGINT UNSIGNED, FOREIGN KEY): 대상 엔티티 ID (예: user_id, admin_id, archive_id)
       - `operation_type` (VARCHAR(20), NOT NULL): 작업 타입 (`INSERT`, `UPDATE`, `DELETE`)
       - `before_data` (JSON, NULL): 변경 전 데이터 (전체 엔티티 데이터를 JSON으로 저장)
       - `after_data` (JSON, NULL): 변경 후 데이터 (전체 엔티티 데이터를 JSON으로 저장)
       - `changed_by` (BIGINT UNSIGNED, FOREIGN KEY, NULL): 변경한 사용자 ID (Admin 또는 User)
       - `changed_at` (TIMESTAMP(6), NOT NULL): 변경 일시
       - `change_reason` (VARCHAR(500), NULL): 변경 사유
     * **operation_type 설명**:
       - 히스토리 테이블의 `operation_type` 필드는 다음 값을 가질 수 있음: `INSERT`, `UPDATE`, `DELETE`
       - **중요**: `operation_type='DELETE'`는 실제 SQL DELETE 쿼리를 의미하는 것이 아니라, Soft Delete 처리를 의미함
       - 즉, `delete_yn` 필드를 'Y'로 변경하고 `deleted_at` 필드에 삭제 일시를 기록하는 작업을 의미함
       - 실제 SQL DELETE 쿼리는 실행되지 않으며, Soft Delete 패턴을 따름
       - `before_data`에는 Soft Delete 전 데이터, `after_data`에는 Soft Delete 후 데이터(delete_yn='Y', deleted_at 설정됨)가 저장됨
     * 참고: docs/phase1/3. aurora-schema-design.md, docs/phase2/2. data-model-design.md

**검증 기준 (전체 통합)**:

**1. domain-aurora 모듈 검증 기준**:
- 모든 엔티티가 정상적으로 저장/조회/수정/삭제 가능해야 함
- 트랜잭션이 정상적으로 롤백 가능해야 함
- Profile별 설정이 정상적으로 로드되어야 함
- Reader/Writer DataSource가 정상적으로 분리되어야 함
- TSID Primary Key가 정상적으로 생성되어야 함
- Soft Delete가 정상적으로 동작해야 함
- 히스토리 자동 저장이 정상적으로 동작해야 함
- API 모듈별 스키마 매핑 검증:
  * `api-auth` 모듈이 `auth` 스키마에 정상적으로 연결되어야 함
  * `api-archive` 모듈이 `archive` 스키마에 정상적으로 연결되어야 함
  * 각 모듈의 `module.aurora.schema` 설정이 `application-api-domain.yml`의 `${module.aurora.schema}`에 정상적으로 반영되어야 함
  * 환경변수(`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`)가 정상적으로 로드되어야 함
  * 로컬 환경에서 `.env` 파일을 통한 환경변수 로드가 정상적으로 동작해야 함
- 빌드 검증: domain-aurora 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
- 빌드 검증: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**2. domain-mongodb 모듈 검증 기준**:
- 모든 Document가 정상적으로 저장/조회 가능해야 함
- 인덱스가 정상적으로 생성되어야 함
- ESR 규칙을 준수한 인덱스 설계 확인
- 프로젝션 최적화가 정상적으로 동작해야 함
- 빌드 검증: domain-mongodb 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-mongodb:build` 명령이 성공해야 함)
- 빌드 검증: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**3. 변경 이력 추적 시스템 검증 기준**:
- 모든 쓰기 작업에 대해 히스토리가 자동으로 저장되어야 함
- 히스토리 테이블의 operation_type='DELETE'는 Soft Delete를 의미함을 확인
- 빌드 검증: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
- 빌드 검증: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**공통 검증 기준**:
- 모든 모듈이 독립적으로 빌드 가능해야 함
- 컴파일 에러 없음
- 기본 CRUD 동작 검증
- Profile별 설정 분리 검증
- 기존 프로젝트 구조 및 코드 스타일 준수 확인
```

### 5단계: 사용자 인증 및 관리 시스템 구현

**단계 번호**: 5단계
**의존성**: 1단계 (프로젝트 구조 생성), 3단계 (Common 모듈 구현 완료 필수), 4단계 (Domain 모듈 - User 엔티티 구현 완료 필수)
**다음 단계**: 11단계 (CQRS 패턴 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)

```
plan task: 사용자 인증 및 관리 시스템 구현

**작업 목표**:
사용자 인증 및 관리 시스템을 구현하여 회원가입, 로그인, 로그아웃, 토큰 갱신, 이메일 인증, 비밀번호 재설정, OAuth 2.0 SNS 로그인 기능을 제공합니다. CQRS 패턴을 기반으로 Command Side(Aurora MySQL)에서 쓰기 작업을 처리하고, Kafka 이벤트를 통해 Query Side(MongoDB Atlas)와 동기화합니다.

**작업 배경**:
- CQRS 패턴 기반 아키텍처에서 Command Side 구현 필요
- JWT 기반 인증 시스템 구축 필요
- OAuth 2.0을 통한 소셜 로그인 지원 필요
- 모든 쓰기 작업에 대한 히스토리 추적 및 Kafka 이벤트 발행 필요

**예상 결과**:
- 사용자 인증 및 관리 기능이 정상적으로 동작하는 API 서버
- Aurora MySQL에 사용자 정보가 정상적으로 저장/조회됨
- Kafka 이벤트를 통한 Query Side 동기화가 정상적으로 동작함
- 모든 엔티티 및 Repository가 정상적으로 빌드 및 컴파일됨

**참고 파일**:
- `docs/reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표 및 전체 시스템 아키텍처
- `docs/phase1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계 (테이블 구조, 인덱스, Foreign Key)
- `docs/phase2/1. api-endpoint-design.md`: API 엔드포인트 설계 (인증 API 처리 로직, Kafka 이벤트 발행 전략)
- `docs/phase2/4. error-handling-strategy-design.md`: 에러 처리 전략 (HTTP 상태 코드, 에러 코드 체계)

**핵심 제한사항 (절대 준수 필수)**:
1. **공식 개발문서만 참고**: Spring Security, JWT (RFC 7519), jjwt 라이브러리 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 다음 사항 모두 금지:
   - 단일 구현체를 위한 인터페이스 생성 (예: UserService 인터페이스가 하나의 구현체만 가질 경우)
   - 불필요한 추상화 계층 추가 (예: Repository 위에 Service 레이어를 추가하되 단순 위임만 하는 경우)
   - "나중을 위해" 추가하는 코드 (예: 현재 사용하지 않는 메서드나 필드)
   - 과도한 디자인 패턴 적용 (예: Factory, Strategy 패턴이 실제로 필요하지 않은 경우)
   - 미래 확장성을 고려한 과도한 추상화
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.

**모듈 구조**:
- `domain-aurora`: Command Side 엔티티 및 Repository (Aurora MySQL)
- `common-security`: 공통 보안 기능 (JWT, PasswordEncoder)
- `common-kafka`: Kafka 이벤트 발행 기능 (또는 api-auth 모듈에서 직접 사용)
- `api-auth`: 인증 API 엔드포인트 및 Spring Security 설정

**작업 의존성 관계**:
- 작업 1-3, 5-6 (엔티티 및 Repository 구현)은 서로 독립적으로 병렬 실행 가능
- 작업 4 (JWT 토큰 관리)는 작업 3 (RefreshToken 엔티티) 완료 후 실행 필요
- 작업 7 (PasswordEncoder 설정)은 작업 8 (인증 API 구현) 전에 완료 필요
- 작업 8 (인증 API 구현)은 작업 1-7 완료 후 실행 필요
- 작업 9 (Spring Security 설정)은 작업 4, 7, 8 완료 후 실행 필요

작업 내용:
1. Provider 엔티티 및 Repository 구현 (domain-aurora 모듈)
   - **역할**: OAuth 제공자 정보 저장 및 조회
   - **책임**: 
     * Provider 엔티티 정의 (id, name, display_name, client_id, client_secret, is_enabled, is_deleted, deleted_at, deleted_by, created_at, created_by, updated_at, updated_by)
     * ProviderWriterRepository 클래스 구현 (@Service, WriterJpaRepository 래핑, save, saveAndFlush, delete, deleteById 메서드)
     * ProviderWriterJpaRepository 인터페이스 구현 (JpaRepository 상속)
     * ProviderReaderRepository 인터페이스 구현 (JpaRepository 상속, Spring Data JPA 쿼리 메서드 규칙에 따라 findByName, findByIsEnabled 메서드 추가)
   - **검증 기준**: 
     * Provider 엔티티가 정상적으로 저장/조회 가능해야 함
     * ProviderReaderRepository의 findByName, findByIsEnabled 메서드가 정상적으로 동작해야 함
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/)

2. User 엔티티 및 Repository 구현 (domain-aurora 모듈)
   - **역할**: 사용자 정보 저장 및 조회
   - **책임**: 
     * User 엔티티 정의 (id, email, password, username, provider_id, provider_user_id, is_email_verified, last_login_at, is_deleted, deleted_at, deleted_by, created_at, created_by, updated_at, updated_by)
     * UserWriterRepository 클래스 구현 (@Service, WriterJpaRepository 래핑, save, saveAndFlush, delete, deleteById 메서드)
     * UserWriterJpaRepository 인터페이스 구현 (JpaRepository 상속)
     * UserReaderRepository 인터페이스 구현 (JpaRepository 상속, Spring Data JPA 쿼리 메서드 규칙에 따라 findByEmail, findByProviderIdAndProviderUserId 메서드 추가)
   - **검증 기준**: 
     * User 엔티티가 정상적으로 저장/조회 가능해야 함
     * UserReaderRepository의 findByEmail, findByProviderIdAndProviderUserId 메서드가 정상적으로 동작해야 함
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/)

3. RefreshToken 엔티티 및 Repository 구현 (domain-aurora 모듈)
   - **역할**: JWT Refresh Token 저장 및 조회
   - **책임**: 
     * RefreshToken 엔티티 정의 (id, user_id, token, expires_at, is_deleted, deleted_at, deleted_by, created_at, created_by, updated_at, updated_by)
     * RefreshTokenWriterRepository 클래스 구현 (@Service, WriterJpaRepository 래핑, save, saveAndFlush, delete, deleteById 메서드)
     * RefreshTokenWriterJpaRepository 인터페이스 구현 (JpaRepository 상속)
     * RefreshTokenReaderRepository 인터페이스 구현 (JpaRepository 상속, Spring Data JPA 쿼리 메서드 규칙에 따라 findByToken, findByUserId 메서드 추가)
   - **검증 기준**: 
     * RefreshToken 엔티티가 정상적으로 저장/조회 가능해야 함
     * RefreshTokenReaderRepository의 findByToken, findByUserId 메서드가 정상적으로 동작해야 함
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/)

4. JWT 토큰 관리 구현 (common-security 모듈)
   - **역할**: JWT 토큰 생성, 검증, 갱신
   - **책임**: 
     * JwtTokenProvider 구현 (jjwt 라이브러리 사용)
     * Access Token 생성 (만료 시간: 3600초=1시간)
     * Refresh Token 생성 및 Aurora MySQL 저장 (refresh_tokens 테이블, 만료 시간: 7일)
     * 토큰 검증 및 페이로드 추출
   - **검증 기준**: 
     * JWT Access Token이 정상적으로 생성되고 만료 시간(3600초=1시간)이 올바르게 설정되어야 함
     * JWT Refresh Token이 정상적으로 생성되고 만료 시간(7일)이 올바르게 설정되어야 함
     * JWT 토큰 검증이 정상적으로 동작해야 함 (유효한 토큰은 통과, 만료된 토큰은 거부)
     * JWT 토큰에서 페이로드(userId 등)가 정상적으로 추출되어야 함
     * Refresh Token이 Aurora MySQL에 정상적으로 저장/조회 가능해야 함
     * **빌드 검증**: common-security 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-security:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: JWT 공식 스펙 (RFC 7519: https://tools.ietf.org/html/rfc7519), jjwt 라이브러리 공식 문서 (https://github.com/jwtk/jjwt)
   - **주의**: 공식 문서의 예제 코드 패턴만 사용, 불필요한 추상화 금지

5. EmailVerification 엔티티 및 Repository 구현 (domain-aurora 모듈)
   - **역할**: 이메일 인증 정보 저장 및 조회
   - **책임**: 
     * EmailVerification 엔티티 정의 (id, email, token, type, expires_at, verified_at, is_deleted, deleted_at, deleted_by, created_at, created_by, updated_at, updated_by)
     * EmailVerificationWriterRepository 클래스 구현 (@Service, WriterJpaRepository 래핑, save, saveAndFlush, delete, deleteById 메서드)
     * EmailVerificationWriterJpaRepository 인터페이스 구현 (JpaRepository 상속)
     * EmailVerificationReaderRepository 인터페이스 구현 (JpaRepository 상속, Spring Data JPA 쿼리 메서드 규칙에 따라 findByToken, findByEmail, findByTokenAndType, findByEmailAndType 메서드 추가)
   - **검증 기준**: 
     * EmailVerification 엔티티가 정상적으로 저장/조회 가능해야 함
     * EmailVerificationReaderRepository의 findByToken, findByEmail 메서드가 정상적으로 동작해야 함
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/)

6. UserHistory 엔티티 및 Repository 구현 (domain-aurora 모듈)
   - **역할**: User 테이블 변경 이력 저장 및 조회
   - **책임**: 
     * UserHistory 엔티티 정의 (history_id, user_id, operation_type, before_data, after_data, changed_by, changed_at, change_reason)
     * UserHistoryReaderRepository 인터페이스 구현 (JpaRepository 상속, Spring Data JPA 쿼리 메서드 규칙에 따라 findByUserId, findByUserIdAndOperationType 메서드 추가)
     * HistoryEntityListener를 통한 자동 히스토리 생성 (UserEntity에 @EntityListeners(HistoryEntityListener.class) 적용, HistoryEntityListener가 User 엔티티 변경 시 자동으로 UserHistory 생성)
   - **검증 기준**: 
     * UserHistory 엔티티가 정상적으로 저장/조회 가능해야 함
     * UserHistoryReaderRepository의 findByUserId, findByUserIdAndOperationType 메서드가 정상적으로 동작해야 함
     * User 엔티티 변경 시 HistoryEntityListener가 자동으로 UserHistory를 생성해야 함 (테스트: User 엔티티 저장/수정 후 UserHistory 테이블에 레코드가 자동 생성되는지 확인)
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Data JPA 공식 문서 (https://docs.spring.io/spring-data/jpa/reference/)

7. PasswordEncoder 설정 (common-security 모듈)
   - **역할**: 비밀번호 암호화 및 검증
   - **책임**: 
     * BCryptPasswordEncoder 빈 등록 (salt rounds: 12)
     * 비밀번호 암호화/검증 유틸리티 제공
   - **검증 기준**: 
     * BCryptPasswordEncoder가 정상적으로 빈으로 등록되어야 함 (salt rounds: 12)
     * 비밀번호가 BCrypt로 정상적으로 암호화되어야 함 (동일한 비밀번호를 암호화해도 다른 해시값 생성 확인)
     * 암호화된 비밀번호 검증이 정상적으로 동작해야 함 (올바른 비밀번호는 통과, 잘못된 비밀번호는 거부)
     * **빌드 검증**: common-security 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-security:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Security PasswordEncoder 공식 문서 (https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)
   - **주의**: Spring Security의 표준 BCryptPasswordEncoder 사용, 커스텀 구현 금지

8. 인증 API 구현 (api-auth 모듈)
   - **역할**: 사용자 인증 API 엔드포인트 제공
   - **책임**: 
     * 회원가입 API (POST /api/v1/auth/signup)
       - 이메일/사용자명 중복 검증 (UserReaderRepository 사용, `is_deleted=FALSE` 조건 포함)
       - 비밀번호 정책 검증 (최소 8자, 대소문자/숫자/특수문자 중 2가지 이상 포함)
       - 비밀번호 해시 생성 (BCryptPasswordEncoder 사용, salt rounds: 12)
       - 트랜잭션 시작
       - `User` 엔티티 생성 (UserWriterRepository 사용)
       - `EmailVerification` 엔티티 생성 (EmailVerificationWriterRepository 사용, `type=EMAIL_VERIFICATION`, `expires_at`: 현재 시간 + 24시간)
       - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: INSERT)
       - 트랜잭션 커밋
       - **Kafka 이벤트 발행**: `UserCreatedEvent`
       - 이메일 인증 토큰 발송 (비동기 처리, 실패 시 재시도 로직 실행)
     * 로그인 API (POST /api/v1/auth/login)
       - 이메일로 `User` 엔티티 조회 (UserReaderRepository 사용, `is_deleted=FALSE` 조건 포함)
       - 비밀번호 검증 (BCryptPasswordEncoder 사용)
       - 이메일 인증 여부 확인 (`is_email_verified=TRUE` 확인, `FALSE`인 경우 로그인 차단)
       - JWT Access Token 생성 (JwtTokenProvider 사용, 만료 시간: 3600초=1시간)
       - JWT Refresh Token 생성 (JwtTokenProvider 사용, 만료 시간: 7일)
       - 트랜잭션 시작
       - `RefreshToken` 엔티티 생성 (RefreshTokenWriterRepository 사용, `expires_at`: 현재 시간 + 7일)
       - `User` 엔티티 업데이트 (`last_login_at` 필드 업데이트, UserWriterRepository 사용)
       - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: UPDATE)
       - 트랜잭션 커밋
       - **Rate Limiting**: 5회/분 (무차별 대입 공격 방지)
     * 로그아웃 API (POST /api/v1/auth/logout)
       - JWT 토큰에서 userId 추출
       - 트랜잭션 시작
       - `RefreshToken` 엔티티 조회 (RefreshTokenReaderRepository 사용, `token=요청의 refreshToken`, `is_deleted=FALSE`, `expires_at` 확인)
       - RefreshToken의 `userId`와 JWT 토큰의 `userId` 일치 여부 검증 (불일치 시 에러 반환)
       - `RefreshToken` 엔티티 Soft Delete (`is_deleted=TRUE`, `deleted_at` 설정, `deleted_by=userId`, RefreshTokenWriterRepository 사용)
       - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: UPDATE)
       - 트랜잭션 커밋
     * 토큰 갱신 API (POST /api/v1/auth/refresh)
       - Refresh Token 검증 (JWT 서명 검증, JwtTokenProvider 사용)
       - `RefreshToken` 엔티티 조회 (RefreshTokenReaderRepository 사용, `token=요청의 refreshToken`, `is_deleted=FALSE`, `expires_at` 확인)
       - 새로운 Access Token 생성 (JwtTokenProvider 사용, 만료 시간: 3600초=1시간)
       - 트랜잭션 시작
       - 기존 `RefreshToken` 엔티티 Soft Delete (`is_deleted=TRUE`, `deleted_at` 설정, RefreshTokenWriterRepository 사용)
       - 새로운 `RefreshToken` 엔티티 생성 (RefreshTokenWriterRepository 사용, `expires_at`: 현재 시간 + 7일)
       - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: UPDATE)
       - 트랜잭션 커밋
       - **참고**: RefreshToken 회전(Rotation) 전략 적용 - 기존 토큰 무효화 후 새 토큰 생성 (RFC 6749 베스트 프랙티스)
     * 이메일 인증 API (GET /api/v1/auth/verify-email?token={token})
       - `EmailVerification` 엔티티 조회 (EmailVerificationReaderRepository 사용, `token`, `type=EMAIL_VERIFICATION`, `is_deleted=FALSE`, `expires_at` 확인)
       - 토큰 만료 여부 확인 (`expires_at` 확인)
       - 중복 인증 방지 확인 (`verified_at`이 이미 설정되어 있는 경우 에러 반환)
       - 트랜잭션 시작
       - `EmailVerification` 엔티티 업데이트 (`verified_at` 설정, 중복 설정 방지, EmailVerificationWriterRepository 사용)
       - `User` 엔티티 업데이트 (`is_email_verified` 필드를 `TRUE`로 설정, UserWriterRepository 사용)
       - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: UPDATE)
       - 트랜잭션 커밋
       - **Kafka 이벤트 발행**: `UserUpdatedEvent`
     * 비밀번호 재설정 API (POST /api/v1/auth/reset-password, POST /api/v1/auth/reset-password/confirm)
       - 비밀번호 재설정 요청 (POST /api/v1/auth/reset-password):
         * 이메일로 `User` 엔티티 조회 (UserReaderRepository 사용, `is_deleted=FALSE` 조건 포함)
         * 비밀번호 재설정 토큰 생성 (암호학적으로 안전한 랜덤 토큰)
         * 트랜잭션 시작
         * 동일 이메일의 기존 `PASSWORD_RESET` 타입 토큰 무효화 (Soft Delete 처리, `email + type` 복합 인덱스 활용)
         * `EmailVerification` 엔티티 생성 (EmailVerificationWriterRepository 사용, `type=PASSWORD_RESET`, `expires_at`: 현재 시간 + 24시간)
         * 트랜잭션 커밋
         * 이메일 발송 (비동기 처리, 존재하지 않는 이메일인 경우에도 성공 응답 반환 - 보안상 일반적, 실패 시 재시도 로직 실행)
         * **Rate Limiting**: 3회/시간 (무차별 대입 공격 방지)
       - 비밀번호 재설정 확인 (POST /api/v1/auth/reset-password/confirm):
         * `EmailVerification` 엔티티 조회 (EmailVerificationReaderRepository 사용, `token`, `type=PASSWORD_RESET`, `is_deleted=FALSE`, `expires_at` 확인)
         * 토큰 만료 여부 확인 (`expires_at` 확인)
         * 토큰 재사용 방지 확인 (`verified_at`이 이미 설정되어 있는 경우 에러 반환)
         * 비밀번호 정책 검증 (최소 8자, 대소문자/숫자/특수문자 중 2가지 이상 포함)
         * 비밀번호 재사용 방지 확인 (이전 비밀번호와 동일한지 검증, BCrypt로 해시된 비밀번호 비교)
         * 새로운 비밀번호 해시 생성 (BCryptPasswordEncoder 사용, salt rounds: 12)
         * 트랜잭션 시작
         * `User` 엔티티 업데이트 (`password` 필드 업데이트, UserWriterRepository 사용)
         * `EmailVerification` 엔티티 업데이트 (`verified_at` 설정, 중복 설정 방지, EmailVerificationWriterRepository 사용)
         * HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: UPDATE)
         * 트랜잭션 커밋
         * **Kafka 이벤트 발행**: `UserUpdatedEvent`
     * OAuth 2.0 SNS 로그인 API (GET /api/v1/auth/oauth2/{provider}, GET /api/v1/auth/oauth2/{provider}/callback)
       - OAuth 로그인 시작 (GET /api/v1/auth/oauth2/{provider}):
         * `Provider` 엔티티 조회 (ProviderReaderRepository 사용, `name=provider`, `is_enabled=TRUE`, `is_deleted=FALSE`)
         * CSRF 방지를 위한 `state` 파라미터 생성 (암호학적으로 안전한 랜덤 토큰)
         * `state` 파라미터를 세션 또는 Redis에 저장 (만료 시간: 10분)
         * OAuth 인증 URL 생성 (`state` 파라미터 포함)
         * 리다이렉트
         * **Rate Limiting**: 10회/분 (무차별 대입 공격 방지)
       - OAuth 로그인 콜백 (GET /api/v1/auth/oauth2/{provider}/callback):
         * `state` 파라미터 검증 (세션 또는 Redis에서 조회, CSRF 방지)
         * `Provider` 엔티티 조회 (ProviderReaderRepository 사용, `is_enabled=TRUE`, `is_deleted=FALSE`)
         * OAuth 인증 코드로 Access Token 교환 (실패 시 에러 반환)
         * OAuth 제공자 API로 사용자 정보 조회 (실패 시 에러 반환)
         * 트랜잭션 시작
         * `User` 엔티티 조회/생성 (UserReaderRepository, UserWriterRepository 사용, `provider_id + provider_user_id` 복합 인덱스 활용, `is_deleted=FALSE` 조건 포함)
           - 존재하지 않으면 `User` 엔티티 생성 (`password=NULL`, OAuth 사용자는 비밀번호 없음)
           - 존재하면 `User` 엔티티 업데이트
           - HistoryEntityListener가 자동으로 `UserHistory` 엔티티 생성 (operation_type: INSERT 또는 UPDATE)
         * JWT Access Token 생성 (JwtTokenProvider 사용, 만료 시간: 3600초=1시간)
         * JWT Refresh Token 생성 (JwtTokenProvider 사용, 만료 시간: 7일)
         * `RefreshToken` 엔티티 생성 (RefreshTokenWriterRepository 사용, `expires_at`: 현재 시간 + 7일)
         * 트랜잭션 커밋
         * **Kafka 이벤트 발행**: `UserCreatedEvent` 또는 `UserUpdatedEvent`
   - **검증 기준**: 
     * 회원가입 API가 정상적으로 동작하고 User 엔티티가 생성되어야 함
     * 로그인 API가 정상적으로 동작하고 JWT 토큰이 발급되어야 함
     * 로그아웃 API가 정상적으로 동작하고 RefreshToken이 Soft Delete되어야 함
     * 토큰 갱신 API가 정상적으로 동작하고 새로운 Access Token이 발급되어야 함
     * 이메일 인증 API가 정상적으로 동작하고 User 엔티티의 is_email_verified가 TRUE로 업데이트되어야 함
     * 비밀번호 재설정 API가 정상적으로 동작하고 User 엔티티의 password가 업데이트되어야 함
     * OAuth 로그인 API가 정상적으로 동작하고 OAuth 인증이 완료되어야 함
     * 모든 쓰기 작업 후 HistoryEntityListener가 자동으로 UserHistory를 생성해야 함
     * 회원가입, 이메일 인증, 비밀번호 재설정, OAuth 로그인 시 Kafka 이벤트가 발행되어야 함
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **보안 요구사항**:
     * JWT (JSON Web Token) 기반 인증
     * Access Token + Refresh Token 패턴
     * 토큰 만료 시간 관리 (Access: 3600초=1시간, Refresh: 7일)
     * 비밀번호 암호화 (BCrypt, salt rounds: 12)
     * HTTPS 필수 (프로덕션 환경)
     * CSRF 보호 (Spring Security 기본 설정)

9. Spring Security 설정 (api-auth 모듈)
   - **역할**: Spring Security 필터 및 인터셉터 설정
   - **책임**: 
     * JwtAuthenticationFilter 구현 (JWT 토큰 검증)
     * SecurityConfig 설정 (인증/인가 규칙, CORS 설정)
   - **검증 기준**: 
     * JWT 토큰 검증이 정상적으로 동작해야 함
     * 인증/인가 규칙이 정상적으로 적용되어야 함
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: Spring Security 공식 문서 (https://docs.spring.io/spring-security/reference/servlet/configuration/java.html)
   - **에러 처리 시나리오** (에러 코드는 docs/phase2/4. error-handling-strategy-design.md 참고):
     * 중복 이메일: 409 Conflict, 에러 코드 4005 (CONFLICT)
     * 잘못된 자격증명: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED)
     * 토큰 만료: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED), Refresh Token으로 갱신 유도
     * 계정 잠금: 403 Forbidden, 에러 코드 4003 (FORBIDDEN), 잠금 해제 시간 안내
     * OAuth 인증 실패: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED)
```

### 6단계: OAuth Provider별 로그인 기능 구현

**단계 번호**: 6단계
**의존성**: 5단계 (사용자 인증 및 관리 시스템 구현 - AuthService OAuth 메서드 구현 완료 필수)
**다음 단계**: 7단계 (Redis 최적화 구현) 또는 8단계 (Client 모듈 구현)

```
plan task: OAuth Provider별 로그인 기능 구현

**description** (작업 목표와 배경):
Google, Naver, Kakao OAuth 2.0 로그인 기능을 구현하여 각 Provider별 인증 URL 생성, Access Token 교환, 사용자 정보 조회 기능을 제공합니다. 기존 AuthService의 OAuth 메서드와 통합하여 완전한 OAuth 로그인 플로우를 완성합니다. 각 Provider의 API 엔드포인트 URL과 Redirect URI는 환경변수로 관리하여 환경별 설정 분리와 보안성을 확보합니다. OAuth 2.0 CSRF 공격 방지를 위한 State 파라미터는 Redis를 활용하여 저장 및 검증합니다. OAuth Provider API 호출은 `client/feign` 모듈의 OpenFeign 클라이언트를 사용하여 프로젝트 아키텍처와 일관성을 유지합니다.

**작업 배경**:
- 5단계에서 AuthService의 OAuth 메서드가 예시 코드로만 구현되어 있음
- 각 OAuth Provider별 실제 API 통신 로직 구현 필요
- OAuth 2.0 Authorization Code Flow 표준 준수 필요
- 기존 JWT 토큰 발급 및 Kafka 이벤트 발행 로직 재사용
- 각 Provider의 API 엔드포인트 URL을 환경변수로 관리하여 유연성 확보 필요
- Stateless 아키텍처 환경에서 State 파라미터 저장을 위한 Redis 활용 필요
- 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치

**예상 결과**:
- Google, Naver, Kakao OAuth 로그인이 정상적으로 동작하는 API 서버
- 각 Provider별 인증 URL 생성이 정상적으로 동작함
- Authorization Code로 Access Token 교환이 정상적으로 동작함
- Access Token으로 사용자 정보 조회가 정상적으로 동작함
- 기존 AuthService와 완전히 통합되어 OAuth 로그인 플로우가 완성됨
- 각 Provider의 API 엔드포인트 URL이 환경변수로 관리되어 환경별 설정 분리됨
- State 파라미터가 Redis에 저장되고 검증되어 CSRF 공격이 방지됨
- `client/feign` 모듈의 OpenFeign 클라이언트를 통한 OAuth Provider API 호출이 정상적으로 동작함
- 모든 관련 코드가 정상적으로 빌드 및 컴파일됨

**requirements** (핵심 제한사항과 기술 요구사항):
1. **공식 개발문서만 참고**: OAuth 2.0 RFC 6749, Google OAuth 2.0 공식 문서, Naver OAuth 2.0 공식 문서, Kakao OAuth 2.0 공식 문서, Spring Cloud OpenFeign 공식 문서, Spring Boot Configuration Properties 공식 문서, Spring Data Redis 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 다음 사항 모두 금지:
   - 단일 구현체를 위한 불필요한 인터페이스 생성 (OAuthProvider 인터페이스는 여러 구현체를 위한 것이므로 허용)
   - 불필요한 추상화 계층 추가 (예: OAuthProvider 위에 추가 추상화 계층)
   - "나중을 위해" 추가하는 코드 (예: 현재 사용하지 않는 Provider 추가)
   - 과도한 디자인 패턴 적용 (예: Strategy 패턴이 실제로 필요하지 않은 경우)
   - 미래 확장성을 고려한 과도한 추상화
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.
4. **구현 가이드 문서 준수**: `docs/oauth-provider-implementation-guide.md`의 구현 가이드를 정확히 따름. 각 Provider별 API 엔드포인트, 파라미터, 응답 형식을 정확히 준수. OpenFeign 클라이언트 사용 방식은 `docs/oauth-feign-client-migration-analysis.md`를 참고.
5. **환경변수 관리 원칙 준수**: `docs/phase1/3. aurora-schema-design.md`의 환경변수 관리 원칙을 준수. 모든 OAuth Provider API 엔드포인트 URL과 Redirect URI는 환경변수로 관리. 하드코딩 절대 금지.
6. **RFC 6749 표준 준수**: State 파라미터 구현은 RFC 6749 Section 10.12 요구사항을 정확히 준수. State 값은 non-guessable value여야 하며, 검증 후 즉시 삭제되어야 함.
7. **Stateless 아키텍처 준수**: 세션 미사용, Redis를 활용한 State 파라미터 저장 및 검증 필수.
8. **프로젝트 아키텍처 일관성 준수**: 외부 API 연동은 `client/feign` 모듈 사용. Contract Pattern으로 일관된 구조 유지. 모듈 의존성 방향 준수: `api/auth` → `client/feign`.

**분석 가이드** (plan task 실행 시 필수 확인 사항):

1. **프로젝트 아키텍처 식별**:
   - `api/auth` 모듈 구조 확인 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/` 디렉토리 구조 분석)
   - `client/feign` 모듈 구조 확인 (`client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/` 디렉토리 구조 분석, Contract Pattern 사용 방식 확인)
   - 기존 Spring Boot Configuration Properties 사용 패턴 확인 (다른 모듈의 설정 클래스 참고)
   - 기존 Redis 사용 패턴 확인 (프로젝트 내 Redis 사용 사례 검색: `codebase_search` 사용)
   - 기존 환경변수 관리 패턴 확인 (`docs/phase1/3. aurora-schema-design.md` 참고)
   - 기존 OpenFeign 클라이언트 사용 패턴 확인 (`client/feign` 모듈의 기존 구현 참고)

2. **기존 코드 확인** (반드시 `codebase_search` 또는 `read_file` 도구 사용):
   - 기존 AuthService의 OAuth 메서드 구조 확인 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/service/AuthService.java`)
     - `startOAuthLogin()` 메서드 (예시 코드)
     - `handleOAuthCallback()` 메서드 (예시 코드)
   - 기존 `client/feign` 모듈의 Contract Pattern 사용 방식 확인 (예: `client/feign/src/main/java/.../domain/sample/` 디렉토리 구조)
   - 기존 코드 스타일, 네이밍 규칙, 아키텍처 패턴 확인 및 준수
   - 유사한 기능이 이미 구현되어 있는 경우 재사용 또는 확장 고려
   - **추측 금지**: 모든 설계 결정은 기존 코드베이스 또는 공식 문서 기반

3. **참고 문서 확인**:
   - `docs/oauth-provider-implementation-guide.md`: OAuth Provider별 구현 가이드
     - Google OAuth 2.0 분석 (API 엔드포인트, 파라미터, 응답 형식)
     - Naver OAuth 2.0 분석
     - Kakao OAuth 2.0 분석
     - 공통 인터페이스 설계
     - 구현 단계별 가이드 (OpenFeign 클라이언트 사용)
     - Redis를 활용한 State 파라미터 저장 설계
     - 보안 고려사항
   - `docs/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 전환 검토 및 구현 가이드
     - 구현 가능성 검토
     - `client/feign` 모듈 구현 가이드 (Contract, FeignClient, Api, Config)
     - `api/auth` 모듈 수정 가이드
     - Contract Pattern 사용 방식
   - `docs/oauth-http-client-selection-analysis.md`: HTTP Client 선택 분석 (OpenFeign 권장 근거)
   - `docs/oauth-state-storage-research-result.md`: State 파라미터 저장 방법 연구 결과
     - Redis 저장 방식 권장 근거 및 구현 가이드
     - RFC 6749 Section 10.12 요구사항 분석
     - Redis Key 설계 및 보안 고려사항
   - `docs/spring-security-auth-design-guide.md`: 기존 인증 시스템 설계
     - JWT 토큰 발급 로직 (재사용)
     - Kafka 이벤트 발행 로직 (재사용)
     - OAuth 로그인 플로우 및 State 파라미터 저장 설계
     - OpenFeign Contract를 통한 OAuth Provider API 호출 플로우
   - `docs/phase2/1. api-endpoint-design.md`: OAuth API 엔드포인트 설계
     - OAuth 로그인 시작 엔드포인트
     - OAuth 로그인 콜백 엔드포인트
     - OpenFeign 구현 참조
   - `docs/phase2/2. data-model-design.md`: Provider, User 엔티티 데이터 모델 설계
     - Provider 엔티티 구조
     - User 엔티티 OAuth 관련 필드
   - `docs/phase1/3. aurora-schema-design.md`: 환경변수 관리 원칙 및 패턴 참고
     - 환경변수 관리 원칙
     - 로컬 환경 설정 방법

4. **기술적 도전 과제 및 주요 결정 사항**:
   - OAuth Provider별 API 차이점 처리 (Google, Naver, Kakao 각각의 API 스펙 차이)
   - Stateless 아키텍처에서 State 파라미터 저장 방법 (Redis 활용)
   - 환경변수 기반 설정 관리 (Spring Boot Configuration Properties 활용)
   - 기존 AuthService와의 통합 전략 (인터페이스 기반 추상화)
   - `client/feign` 모듈의 Contract Pattern 사용 방식 (기존 구현 패턴 준수)

5. **통합 요구사항**:
   - 기존 JWT 토큰 발급 로직 재사용
   - 기존 Kafka 이벤트 발행 로직 재사용 (`UserCreatedEvent`, `UserUpdatedEvent`)
   - 기존 Provider 엔티티 및 User 엔티티 구조 활용
   - 기존 에러 처리 전략 준수 (`docs/phase2/4. error-handling-strategy-design.md` 참고)
   - `client/feign` 모듈의 기존 Contract Pattern 구조 준수

---

## 작업 분해 참고 정보 (splitTasksRaw 단계에서 활용)

**참고**: 아래 정보는 `splitTasksRaw` 단계에서 작업을 분해할 때 참고용입니다. plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행하세요.

**모듈 구조**:
- `client-feign`: OAuth Provider OpenFeign 클라이언트 구현
  - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/contract/`: OAuthProviderContract, OAuthDto
  - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/client/`: GoogleOAuthFeignClient, NaverOAuthFeignClient, KakaoOAuthFeignClient
  - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/api/`: GoogleOAuthApi, NaverOAuthApi, KakaoOAuthApi
  - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/mock/`: GoogleOAuthMock, NaverOAuthMock, KakaoOAuthMock (테스트용)
  - `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/oauth/config/`: OAuthFeignConfig
  - `client/feign/src/main/resources/application-feign-oauth.yml`: OpenFeign 설정 파일
- `api-auth`: OAuth Provider 구현 클래스 및 AuthService 통합
  - `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/`: OAuth Provider 인터페이스 및 구현체, OAuthStateService
  - `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/config/`: OAuth 설정 클래스
  - `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/service/AuthService.java`: OAuth 메서드 통합
  - `api/auth/src/main/resources/application.yml`: OAuth 환경변수 설정

**작업 의존성 관계**:
- 작업 0 (client/feign 모듈 구현)는 작업 1, 3-5, 7 전에 완료 필요
- 작업 1 (OAuth Provider 인터페이스 정의)는 작업 3-5, 7 전에 완료 필요
- 작업 2 (OAuth 설정 클래스)는 작업 3-5 전에 완료 필요 (환경변수 주입을 위해)
- 작업 8 (의존성 추가)는 작업 0, 2, 3-5, 9 전에 완료 필요 (client-feign 모듈, Configuration Processor, Redis)
- 작업 9 (OAuthStateService 구현)는 작업 1, 8 완료 후 실행 필요 (작업 7 전에 완료 필수)
- 작업 3-5 (각 Provider 구현)는 서로 독립적으로 병렬 실행 가능 (작업 0, 1, 2, 8 완료 후)
- 작업 6 (OAuthProviderFactory 구현)는 작업 3-5 완료 후 실행 필요
- 작업 7 (AuthService 통합)는 작업 0, 1, 2, 3-5, 6, 8, 9 완료 후 실행 필요

**의존성 그래프**:
```
작업 0 (client/feign 모듈 구현)
  ↓
작업 1 (인터페이스 정의)
  ↓
작업 2 (설정 클래스) ← 작업 8 (의존성 추가)
  ↓                    ↓
작업 9 (OAuthStateService) ← 작업 8
  ↓
작업 3-5 (Provider 구현) [병렬 실행 가능]
  ↓
작업 6 (Factory)
  ↓
작업 7 (AuthService 통합) ← 작업 9
```

작업 내용:
0. OAuth Provider OpenFeign 클라이언트 구현 (client-feign 모듈)
   - **역할**: OAuth Provider API 호출을 위한 OpenFeign 클라이언트 구현
   - **책임**: 
     * `OAuthProviderContract` 인터페이스 정의 (`client/feign/src/main/java/.../domain/oauth/contract/OAuthProviderContract.java`)
     * `OAuthDto` 클래스 정의 (`client/feign/src/main/java/.../domain/oauth/contract/OAuthDto.java`)
       - `OAuthTokenRequest`, `OAuthTokenResponse`, `OAuthUserInfo` 레코드
       - Provider별 응답 DTO (GoogleTokenResponse, NaverTokenResponse, KakaoTokenResponse 등)
     * Provider별 FeignClient 인터페이스 정의 (`GoogleOAuthFeignClient`, `NaverOAuthFeignClient`, `KakaoOAuthFeignClient`)
       - `@FeignClient` 어노테이션 사용
       - Base URL은 환경변수로 관리 (`${feign-clients.oauth.{provider}.uri}`)
     * Provider별 Api 구현체 작성 (`GoogleOAuthApi`, `NaverOAuthApi`, `KakaoOAuthApi`)
       - `OAuthProviderContract` 인터페이스 구현
       - FeignClient를 사용하여 실제 HTTP 요청 수행
     * `OAuthFeignConfig` 클래스 작성
       - `@EnableFeignClients` 설정
       - Mock/Real 구현체 선택 (`@ConditionalOnProperty`)
     * `application-feign-oauth.yml` 설정 파일 작성
       - Provider별 Base URL 설정
       - Profile별 설정 분리 (test: mock, local/dev: rest)
   - **검증 기준**: 
     * OAuthProviderContract 인터페이스가 정상적으로 정의되어야 함
     * Provider별 FeignClient가 정상적으로 정의되어야 함
     * Provider별 Api 구현체가 정상적으로 구현되어야 함
     * OAuthFeignConfig가 정상적으로 설정되어야 함
     * **빌드 검증**: client-feign 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :client-feign:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 구현 가이드 (Section 2.1)
     * `docs/oauth-provider-implementation-guide.md`: 구현 단계별 가이드 (Section 5.3 Step 1-5)
     * `client/feign/src/main/java/.../domain/sample/`: 기존 Contract Pattern 구현 참고
     * Spring Cloud OpenFeign 공식 문서: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - **주의**: 
     * Contract Pattern을 정확히 준수. 기존 `client/feign` 모듈의 구현 패턴과 일치해야 함.
     * Provider별 Base URL은 환경변수로 관리. 하드코딩 절대 금지.
     * Form Data 전송은 `@RequestBody MultiValueMap<String, String>` 사용.
     * Bearer Token 전달은 `@RequestHeader("Authorization")` 사용.

1. OAuth Provider 인터페이스 및 공통 클래스 정의 (api-auth 모듈)
   - **역할**: OAuth Provider 공통 인터페이스 정의
   - **책임**: 
     * `OAuthProvider` 인터페이스 정의 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthProvider.java`)
       - `generateAuthorizationUrl`, `exchangeAccessToken`, `getUserInfo` 메서드
     * `OAuthUserInfo`는 `client/feign` 모듈의 `OAuthDto.OAuthUserInfo`를 import하여 사용
   - **검증 기준**: 
     * OAuthProvider 인터페이스가 정상적으로 정의되어야 함
     * OAuthUserInfo는 client/feign 모듈의 것을 사용해야 함
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: 공통 인터페이스 설계 및 구현 가이드 (Section 4.1, 5.3 Step 7)
     * `docs/oauth-feign-client-migration-analysis.md`: OAuthProvider 인터페이스 수정 가이드 (Section 2.2)
   - **주의**: 
     * 인터페이스는 여러 구현체를 위한 것이므로 허용됨. 단순 위임만 하는 불필요한 추상화 계층은 금지.
     * `OAuthUserInfo`는 `client/feign` 모듈의 `OAuthDto.OAuthUserInfo`를 사용. api-auth 모듈에서 별도 정의하지 않음.

2. OAuth 설정 클래스 및 환경변수 관리 구현 (api-auth 모듈)
   - **역할**: OAuth Provider별 API 엔드포인트 URL과 Redirect URI를 환경변수로 관리
   - **책임**: 
     * `OAuthProperties` 클래스 구현 (`@ConfigurationProperties(prefix = "oauth")`, Spring Boot Configuration Properties 사용)
     * 각 Provider별 설정 클래스 정의 (`GoogleOAuthProperties`, `NaverOAuthProperties`, `KakaoOAuthProperties`)
     * 각 Provider별 필드 정의:
       - `authorizationEndpoint`: 인증 URL (예: `https://accounts.google.com/o/oauth2/v2/auth`)
       - `redirectUri`: 콜백 Redirect URI (예: `https://your-domain.com/api/v1/auth/oauth2/{provider}/callback`)
       - `scope`: OAuth Scope (Google만 필요, 예: `openid email profile`)
       - **참고**: Token Endpoint와 UserInfo Endpoint는 `client/feign` 모듈의 설정 파일(`application-feign-oauth.yml`)에서 관리됨.
     * `application.yml` 설정 파일 작성 (환경변수 참조 형식: `${OAUTH_GOOGLE_AUTHORIZATION_ENDPOINT}`)
     * `@EnableConfigurationProperties(OAuthProperties.class)` 설정 클래스 작성
     * **환경변수 검증 로직 구현**:
       - 애플리케이션 시작 시점에 필수 환경변수 존재 여부 검증
       - `@PostConstruct` 또는 `ApplicationListener<ApplicationReadyEvent>` 사용
       - 필수 환경변수 누락 시 명확한 에러 메시지와 함께 애플리케이션 시작 실패
       - 검증 대상: 각 Provider별 `authorizationEndpoint`, `redirectUri`
       - URL 형식 검증: `http://` 또는 `https://`로 시작하는지 확인
       - Redirect URI 형식 검증: 올바른 URL 형식인지 확인
   - **환경변수 네이밍 규칙**:
     * 형식: `OAUTH_{PROVIDER}_{SETTING_NAME}`
     * 예시:
       - `OAUTH_GOOGLE_AUTHORIZATION_ENDPOINT`
       - `OAUTH_GOOGLE_REDIRECT_URI`
       - `OAUTH_GOOGLE_SCOPE`
       - `OAUTH_NAVER_AUTHORIZATION_ENDPOINT`
       - `OAUTH_NAVER_REDIRECT_URI`
       - `OAUTH_KAKAO_AUTHORIZATION_ENDPOINT`
       - `OAUTH_KAKAO_REDIRECT_URI`
   - **검증 기준**: 
     * OAuthProperties 클래스가 정상적으로 정의되어야 함
     * application.yml에서 환경변수를 정상적으로 참조해야 함
     * 각 Provider별 설정이 정상적으로 주입되어야 함
     * **환경변수 검증**: 애플리케이션 시작 시 필수 환경변수 누락 시 명확한 에러 메시지와 함께 시작 실패
     * **환경변수 형식 검증**: URL 형식 검증 (http:// 또는 https://로 시작하는지 확인)
     * **환경변수 값 검증**: 필수 환경변수(`authorizationEndpoint`, `redirectUri`)가 모두 설정되어 있는지 확인
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/phase1/3. aurora-schema-design.md`: 환경변수 관리 원칙 참고
     * Spring Boot Configuration Properties 공식 문서: https://docs.spring.io/spring-boot/reference/features/external-config/configuration-properties.html
   - **주의**: 
     * 모든 URL은 환경변수로 관리. 하드코딩 절대 금지.
     * 로컬 환경에서는 `.env` 파일 사용 (`.gitignore`에 포함)
     * 프로덕션 환경에서는 AWS Secrets Manager, Parameter Store 등 활용
     * **참고**: Token Endpoint와 UserInfo Endpoint는 `client/feign` 모듈의 설정 파일(`application-feign-oauth.yml`)에서 관리됨.
     * application.yml 예시:
       ```yaml
       oauth:
         google:
           authorization-endpoint: ${OAUTH_GOOGLE_AUTHORIZATION_ENDPOINT:https://accounts.google.com/o/oauth2/v2/auth}
           redirect-uri: ${OAUTH_GOOGLE_REDIRECT_URI}
           scope: ${OAUTH_GOOGLE_SCOPE:openid email profile}
         naver:
           authorization-endpoint: ${OAUTH_NAVER_AUTHORIZATION_ENDPOINT:https://nid.naver.com/oauth2.0/authorize}
           redirect-uri: ${OAUTH_NAVER_REDIRECT_URI}
         kakao:
           authorization-endpoint: ${OAUTH_KAKAO_AUTHORIZATION_ENDPOINT:https://kauth.kakao.com/oauth/authorize}
           redirect-uri: ${OAUTH_KAKAO_REDIRECT_URI}
       ```

3. Google OAuth Provider 구현 (api-auth 모듈)
   - **역할**: Google OAuth 2.0 인증 처리
   - **책임**: 
     * `GoogleOAuthProvider` 클래스 구현 (`OAuthProvider` 인터페이스 구현)
     * `GoogleOAuthProperties` 주입 (`@RequiredArgsConstructor` 사용)
     * `OAuthProviderContract` 주입 (`@RequiredArgsConstructor` 사용)
     * Google OAuth 인증 URL 생성 (환경변수에서 `authorizationEndpoint` 가져오기, `scope=openid email profile`, `access_type=online`)
     * Authorization Code로 Access Token 교환 (`OAuthProviderContract.exchangeAccessToken()` 호출)
     * Access Token으로 사용자 정보 조회 (`OAuthProviderContract.getUserInfo()` 호출)
   - **검증 기준**: 
     * Google OAuth 인증 URL이 정상적으로 생성되어야 함 (필수 파라미터: `client_id`, `redirect_uri`, `response_type=code`, `scope=openid email profile`, `state`)
     * Authorization Code로 Access Token 교환이 정상적으로 동작해야 함
     * Access Token으로 사용자 정보 조회가 정상적으로 동작해야 함
     * 모든 URL이 환경변수에서 가져와야 함 (하드코딩 금지)
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: Google OAuth 2.0 분석 및 구현 설계 (Section 5.3 Step 8)
     * `docs/oauth-feign-client-migration-analysis.md`: OAuth Provider 구현체 수정 가이드 (Section 2.2)
     * Google OAuth 2.0 공식 문서: https://developers.google.com/identity/protocols/oauth2/web-server
     * Spring Cloud OpenFeign 공식 문서: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - **주의**: 
     * 공식 문서의 API 스펙을 정확히 준수. 추측하지 않음.
     * 모든 URL은 `GoogleOAuthProperties`에서 주입받아 사용. 하드코딩 절대 금지.
     * Access Token 교환 및 사용자 정보 조회는 `OAuthProviderContract` 인터페이스를 통해 수행. 직접 HTTP 요청하지 않음.

4. Naver OAuth Provider 구현 (api-auth 모듈)
   - **역할**: Naver OAuth 2.0 인증 처리
   - **책임**: 
     * `NaverOAuthProvider` 클래스 구현 (`OAuthProvider` 인터페이스 구현)
     * `NaverOAuthProperties` 주입 (`@RequiredArgsConstructor` 사용)
     * `OAuthProviderContract` 주입 (`@RequiredArgsConstructor` 사용)
     * Naver OAuth 인증 URL 생성 (환경변수에서 `authorizationEndpoint` 가져오기)
     * Authorization Code로 Access Token 교환 (`OAuthProviderContract.exchangeAccessToken()` 호출)
     * Access Token으로 사용자 정보 조회 (`OAuthProviderContract.getUserInfo()` 호출)
   - **검증 기준**: 
     * Naver OAuth 인증 URL이 정상적으로 생성되어야 함 (필수 파라미터: `client_id`, `redirect_uri`, `response_type=code`, `state`)
     * Authorization Code로 Access Token 교환이 정상적으로 동작해야 함
     * Access Token으로 사용자 정보 조회가 정상적으로 동작해야 함
     * 모든 URL이 환경변수에서 가져와야 함 (하드코딩 금지)
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: Naver OAuth 2.0 분석 및 구현 설계 (Section 5.3 Step 9)
     * `docs/oauth-feign-client-migration-analysis.md`: OAuth Provider 구현체 수정 가이드 (Section 2.2)
     * Naver 로그인 API 가이드: https://developers.naver.com/docs/login/api/
     * Spring Cloud OpenFeign 공식 문서: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - **주의**: 
     * Naver API 응답 파싱은 `client/feign` 모듈의 `NaverOAuthApi`에서 처리됨. `resultcode="00"` 확인 등은 Api 구현체에서 수행.
     * 모든 URL은 `NaverOAuthProperties`에서 주입받아 사용. 하드코딩 절대 금지.
     * Access Token 교환 및 사용자 정보 조회는 `OAuthProviderContract` 인터페이스를 통해 수행. 직접 HTTP 요청하지 않음.

5. Kakao OAuth Provider 구현 (api-auth 모듈)
   - **역할**: Kakao OAuth 2.0 인증 처리
   - **책임**: 
     * `KakaoOAuthProvider` 클래스 구현 (`OAuthProvider` 인터페이스 구현)
     * `KakaoOAuthProperties` 주입 (`@RequiredArgsConstructor` 사용)
     * `OAuthProviderContract` 주입 (`@RequiredArgsConstructor` 사용)
     * Kakao OAuth 인증 URL 생성 (환경변수에서 `authorizationEndpoint` 가져오기)
     * Authorization Code로 Access Token 교환 (`OAuthProviderContract.exchangeAccessToken()` 호출)
     * Access Token으로 사용자 정보 조회 (`OAuthProviderContract.getUserInfo()` 호출)
   - **검증 기준**: 
     * Kakao OAuth 인증 URL이 정상적으로 생성되어야 함 (필수 파라미터: `client_id`, `redirect_uri`, `response_type=code`, `state`)
     * Authorization Code로 Access Token 교환이 정상적으로 동작해야 함
     * Access Token으로 사용자 정보 조회가 정상적으로 동작해야 함
     * 모든 URL이 환경변수에서 가져와야 함 (하드코딩 금지)
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: Kakao OAuth 2.0 분석 및 구현 설계 (Section 5.3 Step 10)
     * `docs/oauth-feign-client-migration-analysis.md`: OAuth Provider 구현체 수정 가이드 (Section 2.2)
     * Kakao 로그인 REST API: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api
     * Spring Cloud OpenFeign 공식 문서: https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/
   - **주의**: 
     * Kakao API 응답 파싱은 `client/feign` 모듈의 `KakaoOAuthApi`에서 처리됨. `kakao_account` 추출, 이메일 처리, Provider User ID 변환 등은 Api 구현체에서 수행.
     * 모든 URL은 `KakaoOAuthProperties`에서 주입받아 사용. 하드코딩 절대 금지.
     * Access Token 교환 및 사용자 정보 조회는 `OAuthProviderContract` 인터페이스를 통해 수행. 직접 HTTP 요청하지 않음.

6. OAuthProviderFactory 구현 (api-auth 모듈)
   - **역할**: Provider 이름으로 적절한 OAuthProvider 구현체 반환
   - **책임**: 
     * `OAuthProviderFactory` 클래스 구현 (`@Component`, Spring의 `Map<String, OAuthProvider>` 자동 주입 활용)
     * `getProvider(String providerName)` 메서드 구현 (Provider 이름으로 OAuthProvider 조회, 대소문자 무시)
     * 지원하지 않는 Provider인 경우 `ResourceNotFoundException` 발생
   - **검증 기준**: 
     * OAuthProviderFactory가 정상적으로 빈으로 등록되어야 함
     * Provider 이름으로 적절한 OAuthProvider가 반환되어야 함 (예: "GOOGLE" → GoogleOAuthProvider, "NAVER" → NaverOAuthProvider, "KAKAO" → KakaoOAuthProvider)
     * 지원하지 않는 Provider인 경우 적절한 예외가 발생해야 함
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: `docs/oauth-provider-implementation-guide.md`: OAuthProviderFactory 및 구현 가이드
   - **주의**: Spring의 `Map<String, OAuthProvider>` 자동 주입 활용. Bean 이름은 Provider 이름과 일치해야 함 (예: `@Component("GOOGLE")`).

7. AuthService OAuth 메서드 통합 (api-auth 모듈)
   - **역할**: 기존 AuthService의 OAuth 메서드를 OAuthProvider 인터페이스를 사용하도록 통합
   - **책임**: 
     * `AuthService.startOAuthLogin(String providerName)` 메서드 수정
       - Provider 조회 및 활성화 확인 (기존 로직 유지)
       - State 파라미터 생성 (기존 로직 유지, `generateSecureToken()` 메서드 사용)
       - OAuthStateService를 통해 State 저장 (`oauthStateService.saveState(state, providerName.toUpperCase())`)
       - OAuthProviderFactory를 통해 OAuthProvider 조회
       - OAuthProvider에서 Redirect URI 가져오기 (환경변수에서 설정된 값 사용, `OAuthProperties`에서 주입)
       - OAuthProvider.generateAuthorizationUrl() 호출하여 인증 URL 생성
     * `AuthService.handleOAuthCallback(String providerName, String code, String state)` 메서드 수정
       - Provider 조회 및 활성화 확인 (기존 로직 유지)
       - OAuthStateService를 통해 State 검증 및 삭제 (`oauthStateService.validateAndDeleteState(state, providerName.toUpperCase())`)
       - OAuthProviderFactory를 통해 OAuthProvider 조회
       - OAuthProvider에서 Redirect URI 가져오기 (환경변수에서 설정된 값 사용, `OAuthProperties`에서 주입)
       - OAuthProvider.exchangeAccessToken() 호출하여 Access Token 교환
       - OAuthProvider.getUserInfo() 호출하여 사용자 정보 조회
       - User 엔티티 조회/생성 (기존 로직 유지, `findByProviderIdAndProviderUserId` 사용)
       - JWT 토큰 생성 (기존 로직 유지)
       - RefreshToken 저장 (기존 로직 유지)
       - Kafka 이벤트 발행 (기존 로직 유지, `UserCreatedEvent` 또는 `UserUpdatedEvent`)
   - **검증 기준**: 
     * startOAuthLogin 메서드가 OAuthProvider 인터페이스를 사용하여 인증 URL을 생성해야 함
     * startOAuthLogin 메서드가 OAuthStateService를 통해 State를 Redis에 저장해야 함
     * handleOAuthCallback 메서드가 OAuthProvider 인터페이스를 사용하여 Access Token 교환 및 사용자 정보 조회를 수행해야 함
     * handleOAuthCallback 메서드가 OAuthStateService를 통해 State를 검증하고 삭제해야 함
     * Redirect URI가 환경변수에서 가져와야 함 (하드코딩 금지)
     * 기존 JWT 토큰 발급 로직이 정상적으로 동작해야 함
     * 기존 Kafka 이벤트 발행 로직이 정상적으로 동작해야 함
     * OAuth 로그인 플로우가 완전히 동작해야 함 (인증 URL 생성 → OAuth Provider 인증 → 콜백 처리 → JWT 토큰 발급)
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: 통합 전략 및 Redis를 활용한 State 파라미터 저장 설계 (Section 5.3 Step 11)
     * `docs/oauth-feign-client-migration-analysis.md`: AuthService 수정 가이드 (Section 2.2)
     * `docs/oauth-state-storage-research-result.md`: State 파라미터 저장 방법 연구 결과
     * `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/service/AuthService.java`: 기존 OAuth 메서드
   - **주의**: 
     * 기존 코드 구조 유지. Provider별 구현은 인터페이스로 추상화하되, 기존 JWT 토큰 발급 및 Kafka 이벤트 발행 로직은 재사용.
     * Redirect URI는 환경변수에서 가져와야 함. 하드코딩 절대 금지.
     * State 저장은 OAuthStateService를 통해서만 수행. 직접 Redis 접근 금지.

8. 의존성 추가 (api-auth 모듈)
   - **역할**: OAuth Provider 구현에 필요한 의존성 추가
   - **책임**: 
     * `client-feign` 모듈 의존성 추가 (`implementation project(':client-feign')`)
     * Jackson 의존성 확인 (이미 포함되어 있을 수 있음)
     * Spring Boot Configuration Processor 의존성 추가 (선택적, IDE 자동완성 지원)
     * Spring Data Redis 의존성 확인 (이미 포함되어 있을 수 있음, `spring-boot-starter-data-redis`)
   - **검증 기준**: 
     * client-feign 모듈이 정상적으로 사용 가능해야 함
     * Jackson이 정상적으로 사용 가능해야 함
     * Spring Data Redis가 정상적으로 사용 가능해야 함 (OAuthStateService 구현을 위해)
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: 의존성 추가 가이드 (Section 5.2)
     * `docs/oauth-feign-client-migration-analysis.md`: api/auth 모듈 의존성 추가 가이드 (Section 2.2)
     * Spring Data Redis 공식 문서: https://docs.spring.io/spring-data/redis/docs/current/reference/html/
   - **주의**: 
     * 외부 API 연동은 `client/feign` 모듈 사용이 프로젝트 아키텍처와 일치.
     * 모듈 의존성 방향 준수: `api/auth` → `client/feign`.

9. OAuthStateService 구현 (api-auth 모듈)
   - **역할**: OAuth 2.0 State 파라미터를 Redis에 저장하고 검증하는 서비스 구현
   - **책임**: 
     * `OAuthStateService` 클래스 구현
       - 위치: `api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthStateService.java`
       - `@Service` 어노테이션 사용
       - `RedisTemplate<String, String>` 주입 (`@RequiredArgsConstructor` 사용)
     * `saveState(String state, String providerName)` 메서드 구현
       - Redis Key 형식: `oauth:state:{state_value}`
       - Value: Provider 이름 (예: "GOOGLE", "NAVER", "KAKAO")
       - TTL: 10분 (600초) - `Duration.ofMinutes(10)` 사용
       - Redis에 저장: `redisTemplate.opsForValue().set(key, providerName, STATE_TTL)`
     * `validateAndDeleteState(String state, String providerName)` 메서드 구현
       - Redis에서 State 조회: `redisTemplate.opsForValue().get(key)`
       - State 미존재 시: `UnauthorizedException` 발생
       - Provider 정보 불일치 시: `UnauthorizedException` 발생 및 State 삭제 (보안 강화)
       - 검증 성공 시: State 즉시 삭제 (일회성 사용, Replay Attack 방지)
       - `redisTemplate.delete(key)` 사용
   - **검증 기준**: 
     * OAuthStateService가 정상적으로 빈으로 등록되어야 함
     * `saveState()` 메서드가 Redis에 State를 정상적으로 저장해야 함 (TTL 10분 설정)
     * `validateAndDeleteState()` 메서드가 State를 정상적으로 검증하고 삭제해야 함
     * State 미존재 시 적절한 예외가 발생해야 함
     * Provider 정보 불일치 시 적절한 예외가 발생하고 State가 삭제되어야 함
     * 검증 성공 시 State가 즉시 삭제되어야 함 (일회성 사용)
     * Redis Key 형식이 `oauth:state:{state_value}`로 정확히 일치해야 함
     * **빌드 검증**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/oauth-provider-implementation-guide.md`: Redis를 활용한 State 파라미터 저장 설계
     * `docs/oauth-state-storage-research-result.md`: State 파라미터 저장 방법 연구 결과 (Redis Key 설계, 보안 고려사항)
     * `docs/spring-security-auth-design-guide.md`: OAuth 로그인 플로우 및 State 파라미터 저장 설계
     * RFC 6749 Section 10.12: CSRF Protection
     * RFC 6749 Section 10.10: Entropy of Secrets (Non-guessable value 요구사항)
     * Spring Data Redis 공식 문서: https://docs.spring.io/spring-data/redis/docs/current/reference/html/
   - **주의**: 
     * Redis Key 설계는 `oauth:state:{state_value}` 형식을 정확히 준수.
     * State 값은 `generateSecureToken()` 메서드로 생성된 암호학적으로 안전한 랜덤 값 (최소 32바이트 권장).
     * TTL은 정확히 10분 (600초)으로 설정.
     * 검증 실패 시 구체적인 에러 정보를 클라이언트에 노출하지 않음 (보안상 일반적인 에러 메시지만 반환).
     * 상세한 에러 정보는 서버 로그에만 기록 (`log.warn()` 사용).
     * 오버엔지니어링 금지: State 값과 Provider 정보만 저장. 추가 메타데이터(생성 시간, IP 주소 등) 저장하지 않음.

**보안 요구사항** (작업 분해 시 고려):
- State 파라미터 검증 (CSRF 방지)
  - RFC 6749 Section 10.12 요구사항 준수
  - Non-guessable value 생성 (RFC 6749 Section 10.10 준수, 최소 32바이트)
  - Redis에 State 저장 및 검증 (TTL 10분, 일회성 사용)
  - HTTPS 전송 필수 (프로덕션 환경)
- HTTPS 필수 (프로덕션 환경)
- Client Secret 관리 (환경 변수 또는 시크릿 관리 시스템 사용)
- OAuth Access Token은 사용자 정보 조회 후 즉시 폐기 (저장하지 않음)
- 모든 OAuth Provider API 엔드포인트 URL은 환경변수로 관리 (하드코딩 금지)
- Redis 보안 설정 (인증, TLS 암호화 전송 권장)

**환경변수 설정 예시** (`.env` 파일, 작업 분해 시 참고):
```bash
# Google OAuth 설정
OAUTH_GOOGLE_AUTHORIZATION_ENDPOINT=https://accounts.google.com/o/oauth2/v2/auth
OAUTH_GOOGLE_REDIRECT_URI=https://your-domain.com/api/v1/auth/oauth2/google/callback
OAUTH_GOOGLE_SCOPE=openid email profile

# Naver OAuth 설정
OAUTH_NAVER_AUTHORIZATION_ENDPOINT=https://nid.naver.com/oauth2.0/authorize
OAUTH_NAVER_REDIRECT_URI=https://your-domain.com/api/v1/auth/oauth2/naver/callback

# Kakao OAuth 설정
OAUTH_KAKAO_AUTHORIZATION_ENDPOINT=https://kauth.kakao.com/oauth/authorize
OAUTH_KAKAO_REDIRECT_URI=https://your-domain.com/api/v1/auth/oauth2/kakao/callback

# OpenFeign OAuth 설정 (client/feign 모듈)
FEIGN_CLIENTS_OAUTH_GOOGLE_URI=https://oauth2.googleapis.com
FEIGN_CLIENTS_OAUTH_NAVER_URI=https://nid.naver.com
FEIGN_CLIENTS_OAUTH_KAKAO_URI=https://kauth.kakao.com

# Redis 설정 (State 파라미터 저장용)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=  # 프로덕션 환경에서는 필수
REDIS_SSL_ENABLED=false  # 프로덕션 환경에서는 true 권장
```

**에러 처리 시나리오** (작업 분해 시 참고, 에러 코드는 docs/phase2/4. error-handling-strategy-design.md 참고):
- OAuth 인증 실패: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED)
- 지원하지 않는 Provider: 404 Not Found, 에러 코드 4004 (RESOURCE_NOT_FOUND)
- OAuth API 통신 실패: 502 Bad Gateway, 에러 코드 5002 (EXTERNAL_SERVICE_ERROR)
- State 파라미터 검증 실패: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED) - State 미존재, 불일치, 만료 시
- 환경변수 누락: 500 Internal Server Error, 에러 코드 5001 (INTERNAL_SERVER_ERROR) - 애플리케이션 시작 시점에 검증
- 환경변수 형식 오류: 500 Internal Server Error, 에러 코드 5001 (INTERNAL_SERVER_ERROR) - 애플리케이션 시작 시점에 검증 (URL 형식이 올바르지 않은 경우)
- Redirect URI 불일치: 400 Bad Request, 에러 코드 4000 (BAD_REQUEST) - OAuth Provider에서 반환한 redirect_uri와 설정된 값 불일치 시
```

### 7단계: Redis 최적화 구현

**단계 번호**: 7단계
**의존성**: 1단계 (프로젝트 구조 생성), 3단계 (Common 모듈 구현 완료 필수), 6단계 (OAuth Provider별 로그인 기능 구현 완료 필수 - OAuth State 저장에 Redis 사용 중)
**다음 단계**: 8단계 (Client 모듈 구현)

```
plan task: Redis 최적화 구현

**참고 파일**:
- `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스 분석 결과
- `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 방법 연구 결과 (Redis 사용 근거)
- `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드 (Stateless 아키텍처 확인)
- Spring Data Redis 공식 문서: https://docs.spring.io/spring-data/redis/reference/
- Spring Boot 공식 문서: https://docs.spring.io/spring-boot/reference/data/nosql/redis.html
- Redis 공식 문서: https://redis.io/docs/

**description** (작업 목표와 배경):
멀티모듈 Spring Boot 애플리케이션에서 Redis를 최적화하여 사용하기 위한 설정 및 코드 개선을 구현합니다. 현재 Redis는 OAuth State 저장 및 Kafka 이벤트 멱등성 보장에 사용되고 있으며, 연결 풀 설정 부재, TTL 설정 일관성 문제, 모니터링 설정 부재 등의 개선이 필요합니다. `docs/step7/redis-optimization-best-practices.md` 문서의 분석 결과를 기반으로 프로덕션 환경에 적합한 Redis 설정을 적용합니다.

**작업 배경**:
- 현재 Redis는 기본 설정만 사용 중 (연결 풀 설정 없음, 타임아웃 설정 없음)
- OAuthStateService와 EventConsumer에서 TTL 설정 방법이 불일치 (Duration vs TimeUnit 혼용)
- Spring Boot Actuator 메트릭 수집 설정 부재
- Redis 보안 설정이 환경 변수로만 관리 (프로덕션 환경 고려 필요)
- Spring Session Data Redis 의존성이 포함되어 있으나 실제로는 사용하지 않음 (Stateless 아키텍처)

**예상 결과**:
- Lettuce 연결 풀 최적화 설정이 적용되어 Redis 연결 성능이 개선됨
- RedisConfig가 최적화되어 명시적 직렬화 설정 및 트랜잭션 지원 명시
- EventConsumer의 TTL 설정이 OAuthStateService와 일관되게 `Duration` 객체 직접 사용으로 통일됨
- Spring Boot Actuator를 통한 Redis 메트릭 수집이 활성화됨
- Redis 보안 설정이 강화됨 (인증, TLS/SSL)
- (선택) Spring Session Data Redis 의존성이 제거되어 불필요한 의존성 제거
- 모든 관련 코드가 정상적으로 빌드 및 컴파일됨

**requirements** (핵심 제한사항과 기술 요구사항):
1. **공식 개발문서만 참고**: Spring Data Redis 공식 문서, Spring Boot 공식 문서, Redis 공식 문서, Lettuce 공식 문서만 참고. 블로그, 튜토리얼, Stack Overflow 등 비공식 자료 절대 금지. (예외: Stack Overflow 최상위 답변으로 채택된 내용은 다른 참고 자료가 없는 경우에만 예외적으로 참고)
2. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 현재 요구사항에 명시되지 않은 기능 절대 구현하지 않음. 다음 사항 모두 금지:
   - 현재 사용하지 않는 Redis 기능 구현 (Redis Streams, Pub/Sub 등)
   - 불필요한 추상화 계층 추가
   - "나중을 위해" 추가하는 코드
   - 과도한 디자인 패턴 적용
3. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수.
4. **구현 가이드 문서 준수**: `docs/step7/redis-optimization-best-practices.md`의 구현 가이드를 정확히 따름. 연결 풀 설정 값, RedisConfig 최적화 방안, TTL 설정 일관성 개선 방법을 정확히 준수.
5. **멀티모듈 아키텍처 준수**: `common/core` 모듈에서 공통 Redis 설정 제공. 다른 모듈에서 일관된 Redis 사용 패턴 유지.
6. **기존 코드 일관성 유지**: 기존 Redis 사용 패턴(OAuthStateService, EventConsumer)과 일관성 유지. 불필요한 변경 최소화.

**작업 의존성 관계**:
- 작업 1 (연결 풀 설정 추가)는 작업 2 전에 완료 필요
- 작업 2 (RedisConfig 최적화)는 작업 3 전에 완료 필요
- 작업 3 (TTL 설정 일관성 개선)는 작업 4 전에 완료 필요
- 작업 4 (모니터링 설정)는 작업 5 전에 완료 필요
- 작업 5 (보안 설정 강화)는 독립적으로 실행 가능
- 작업 6 (Spring Session Data Redis 의존성 제거)는 독립적으로 실행 가능 (선택 사항)

**의존성 그래프**:
```
작업 1 (연결 풀 설정 추가)
  ↓
작업 2 (RedisConfig 최적화)
  ↓
작업 3 (TTL 설정 일관성 개선)
  ↓
작업 4 (모니터링 설정)
  ↓
작업 5 (보안 설정 강화)
  (작업 6은 독립적으로 실행 가능)
```

**모듈 구조**:
- `common-core`: Redis 설정 최적화
  - `common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java`: RedisConfig 최적화
  - `common/core/src/main/resources/application-common-core.yml`: Redis 연결 풀 설정 및 공통 설정 제공
- `api-auth`: Redis 설정 참조
  - `api/auth/src/main/resources/application-auth-api.yml`: `spring.profiles.include: common-core`로 공통 설정 참조
- `common-kafka`: TTL 설정 일관성 개선
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`: TTL 설정 방법 변경
- `common-security`: (선택) Spring Session Data Redis 의존성 제거
  - `common/security/build.gradle`: 불필요한 의존성 제거

**분석 가이드** (plan task 실행 시 필수 확인 사항):

1. **프로젝트 아키텍처 식별**:
   - `common/core` 모듈 구조 확인 (`common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java` 확인)
   - 현재 Redis 사용 사례 확인 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthStateService.java`, `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`)
   - 기존 Redis 설정 확인 (`common/core/src/main/resources/application-common-core.yml` 공통 설정 파일)
   - Spring Boot Actuator 의존성 확인 (`common/core/build.gradle` 확인)
   - Spring Session Data Redis 의존성 확인 (`common/security/build.gradle` 확인)

2. **기존 코드 확인** (반드시 `codebase_search` 또는 `read_file` 도구 사용):
   - 현재 RedisConfig 구현 확인 (`common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java`)
   - OAuthStateService의 TTL 설정 방법 확인 (`api/auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/oauth/OAuthStateService.java`)
   - EventConsumer의 TTL 설정 방법 확인 (`common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`)
   - 기존 코드 스타일, 네이밍 규칙, 아키텍처 패턴 확인 및 준수
   - **추측 금지**: 모든 설계 결정은 기존 코드베이스 또는 공식 문서 기반

3. **참고 문서 확인**:
   - `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스 분석 결과
     - 현재 구현 분석 (문제점 식별)
     - 연결 풀 최적화 권장 사항
     - RedisTemplate 설정 최적화 방안
     - TTL 설정 일관성 개선 방법
     - 모니터링 및 관찰 가능성 설정
     - 보안 고려사항
     - 멀티모듈 환경 최적화
     - 구현 가이드 (8단계)
   - `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 방법 연구 결과 (Redis 사용 근거)
   - `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드 (Stateless 아키텍처 확인)
   - Spring Data Redis 공식 문서: https://docs.spring.io/spring-data/redis/reference/
   - Spring Boot 공식 문서: https://docs.spring.io/spring-boot/reference/data/nosql/redis.html
   - Redis 공식 문서: https://redis.io/docs/

4. **기술적 도전 과제 및 주요 결정 사항**:
   - 연결 풀 설정 값 결정 (프로덕션 환경에 적합한 값)
   - TTL 설정 방법 통일 (Duration 객체 직접 사용)
   - 모니터링 설정 방법 (Spring Boot Actuator 활용)
   - 보안 설정 강화 방법 (인증, TLS/SSL)
   - Spring Session Data Redis 의존성 제거 여부 결정 (Stateless 아키텍처 유지)

5. **통합 요구사항**:
   - 기존 OAuthStateService와의 호환성 유지
   - 기존 EventConsumer와의 호환성 유지
   - 멀티모듈 환경에서 일관된 Redis 사용 패턴 유지
   - `common/core` 모듈에서 공통 설정 제공

---

## 작업 분해 참고 정보 (splitTasksRaw 단계에서 활용)

**참고**: 아래 정보는 `splitTasksRaw` 단계에서 작업을 분해할 때 참고용입니다. plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행하세요. 모듈 구조와 작업 의존성 관계는 위의 섹션을 참고하세요.

작업 내용:
1. Lettuce 연결 풀 설정 추가
   - **역할**: Redis 연결 풀 최적화 설정 추가
   - **책임**: 
     * `common/core/src/main/resources/application-common-core.yml` 파일 생성 및 Lettuce 연결 풀 설정 추가
     * 타임아웃 설정 추가
     * `api/auth/src/main/resources/application-auth-api.yml`에서 `spring.profiles.include: common-core`로 공통 설정 참조
   - **설정 내용**:
     * `common/core/src/main/resources/application-common-core.yml` 파일 생성:
     ```yaml
     spring:
       data:
         redis:
           host: ${REDIS_HOST:localhost}
           port: ${REDIS_PORT:6379}
           password: ${REDIS_PASSWORD:}
           ssl:
             enabled: ${REDIS_SSL_ENABLED:false}
           timeout: 2000ms
           lettuce:
             pool:
               max-active: 8
               max-idle: 8
               min-idle: 2
               max-wait: -1ms
             shutdown-timeout: 100ms
     ```
     * `api/auth/src/main/resources/application-auth-api.yml`에서 공통 설정 참조:
     ```yaml
     spring:
       profiles:
         include:
           - common-core
     ```
   - **검증 기준**: 
     * 연결 풀 설정이 정상적으로 적용되어야 함
     * 타임아웃 설정이 정상적으로 적용되어야 함
     * **빌드 검증**: 관련 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: 연결 풀 최적화 섹션 (Section 2.2, 2.3)
     * Spring Boot 공식 문서: https://docs.spring.io/spring-boot/reference/data/nosql/redis.html
   - **주의**: 
     * 설정 값은 `docs/step7/redis-optimization-best-practices.md`의 권장 값을 정확히 따름.
     * `common/core` 모듈에서 공통 설정 파일(`application-common-core.yml`)을 제공하여 다른 모듈에서 재사용 가능하도록 구성.
     * 다른 모듈에서는 `spring.profiles.include: common-core`로 공통 설정을 참조.

2. RedisConfig 최적화 (common-core 모듈)
   - **역할**: RedisTemplate 설정 최적화
   - **책임**: 
     * `common/core/src/main/java/com/ebson/shrimp/tm/demo/common/core/config/RedisConfig.java` 수정
     * `setEnableDefaultSerializer(false)` 추가
     * `setEnableTransactionSupport(false)` 명시
   - **변경 사항**:
     ```java
     @Configuration
     public class RedisConfig {
         @Bean
         public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
             RedisTemplate<String, String> template = new RedisTemplate<>();
             template.setConnectionFactory(connectionFactory);
             
             // 직렬화 설정
             template.setKeySerializer(new StringRedisSerializer());
             template.setValueSerializer(new StringRedisSerializer());
             template.setHashKeySerializer(new StringRedisSerializer());
             template.setHashValueSerializer(new StringRedisSerializer());
             
             // 기본 직렬화 비활성화 (명시적 직렬화만 사용)
             template.setEnableDefaultSerializer(false);
             
             // 트랜잭션 지원 비활성화 (현재 사용 사례에서 불필요)
             template.setEnableTransactionSupport(false);
             
             // 초기화
             template.afterPropertiesSet();
             
             return template;
         }
     }
     ```
   - **검증 기준**: 
     * RedisConfig가 정상적으로 최적화되어야 함
     * `setEnableDefaultSerializer(false)`가 추가되어야 함
     * `setEnableTransactionSupport(false)`가 명시되어야 함
     * 기존 Redis 사용 코드(OAuthStateService, EventConsumer)가 정상적으로 동작해야 함
     * **빌드 검증**: common-core 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-core:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: RedisTemplate 설정 최적화 섹션 (Section 3.2)
     * Spring Data Redis 공식 문서: https://docs.spring.io/spring-data/redis/reference/redis/redis-template.html
   - **주의**: 
     * 기존 코드 구조 유지. 불필요한 변경 최소화.
     * `afterPropertiesSet()` 호출은 유지 (방어적 프로그래밍).

3. TTL 설정 일관성 개선 (common-kafka 모듈)
   - **역할**: EventConsumer의 TTL 설정 방법을 OAuthStateService와 일관되게 개선
   - **책임**: 
     * `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java` 수정
     * `PROCESSED_EVENT_TTL_DAYS` (long) → `PROCESSED_EVENT_TTL` (Duration) 변경
     * `Duration.ofDays().toSeconds()` + `TimeUnit.SECONDS` → `Duration` 객체 직접 사용으로 변경
     * `TimeUnit` import 제거
   - **변경 사항**:
     ```java
     private static final String PROCESSED_EVENT_PREFIX = "processed_event:";
     private static final Duration PROCESSED_EVENT_TTL = Duration.ofDays(7);
     
     // ...
     
     private void markEventAsProcessed(String eventId) {
         String key = PROCESSED_EVENT_PREFIX + eventId;
         // Duration 객체 직접 사용 (일관성 개선)
         redisTemplate.opsForValue().set(key, "processed", PROCESSED_EVENT_TTL);
     }
     ```
   - **검증 기준**: 
     * EventConsumer의 TTL 설정이 `Duration` 객체 직접 사용으로 변경되어야 함
     * OAuthStateService와 일관된 TTL 설정 방법을 사용해야 함
     * 기존 기능(이벤트 멱등성 보장)이 정상적으로 동작해야 함
     * **빌드 검증**: common-kafka 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-kafka:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: TTL 설정 일관성 개선 섹션 (Section 4.3.2)
     * Spring Data Redis 공식 문서: `RedisTemplate.opsForValue().set(key, value, timeout)` 메서드 시그니처 확인
   - **주의**: 
     * TTL 값(7일)은 변경하지 않음. 설정 방법만 변경.
     * `TimeUnit` import 제거 확인.

4. Spring Boot Actuator 메트릭 수집 설정
   - **역할**: Redis 메트릭 수집 활성화
   - **책임**: 
     * `common/core/src/main/resources/application-common-core.yml`에 Spring Boot Actuator 메트릭 수집 설정 추가
     * 다른 모듈에서 `spring.profiles.include: common-core`로 공통 설정 참조
   - **설정 내용**:
     * `common/core/src/main/resources/application-common-core.yml`에 추가:
     ```yaml
     management:
       endpoints:
         web:
           exposure:
             include: health,metrics,prometheus
       metrics:
         export:
           prometheus:
             enabled: true
         tags:
           application: ${spring.application.name}
     ```
   - **검증 기준**: 
     * Spring Boot Actuator 메트릭 수집이 활성화되어야 함
     * Redis 메트릭이 정상적으로 수집되어야 함 (`spring.data.redis.connection.active`, `spring.data.redis.connection.idle`, `spring.data.redis.command.duration`)
     * **빌드 검증**: 관련 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: 모니터링 및 관찰 가능성 섹션 (Section 6.1)
     * Spring Boot Actuator 공식 문서: https://docs.spring.io/spring-boot/reference/actuator/metrics.html
   - **주의**: 
     * `common/core/build.gradle`에 이미 `spring-boot-starter-actuator` 의존성이 포함되어 있음.
     * 프로덕션 환경에서는 보안을 고려하여 엔드포인트 노출 범위 제한.

5. Redis 보안 설정 강화
   - **역할**: Redis 연결 보안 강화 (인증, TLS/SSL)
   - **책임**: 
     * `application.yml`에 Redis 보안 설정 추가 (이미 환경 변수로 관리 중이지만 명시적으로 설정)
     * 설정 파일 위치: `common/core/src/main/resources/application-common-core.yml` (공통 설정 파일에 이미 포함되어 있음)
   - **설정 내용**:
     ```yaml
     spring:
       data:
         redis:
           password: ${REDIS_PASSWORD:}  # 프로덕션 환경에서는 필수
           ssl:
             enabled: ${REDIS_SSL_ENABLED:false}  # 프로덕션 환경에서는 true 권장
     ```
   - **검증 기준**: 
     * Redis 인증 설정이 정상적으로 적용되어야 함 (프로덕션 환경)
     * TLS/SSL 설정이 정상적으로 적용되어야 함 (프로덕션 환경)
     * **빌드 검증**: 관련 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: 보안 고려사항 섹션 (Section 7.1)
     * Redis 공식 문서: https://redis.io/docs/management/security/
   - **주의**: 
     * 환경 변수로 관리 중이므로 설정 파일에는 환경 변수 참조만 추가.
     * 프로덕션 환경에서는 반드시 비밀번호 및 TLS/SSL 설정 필요.

6. (선택) Spring Session Data Redis 의존성 제거 (common-security 모듈)
   - **역할**: Stateless 아키텍처에서 불필요한 Spring Session Data Redis 의존성 제거
   - **책임**: 
     * `common/security/build.gradle`에서 `spring-boot-starter-session-data-redis` 제거
     * 관련 테스트 의존성(`spring-boot-starter-session-data-redis-test`)도 제거
   - **검증 기준**: 
     * Spring Session Data Redis 의존성이 제거되어야 함
     * 관련 테스트 의존성도 제거되어야 함
     * 기존 기능(인증/인가)이 정상적으로 동작해야 함 (Stateless 아키텍처 유지)
     * **빌드 검증**: common-security 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-security:build` 명령이 성공해야 함)
     * **컴파일 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **참고**: 
     * `docs/step7/redis-optimization-best-practices.md`: 멀티모듈 환경 최적화 섹션 (Section 8.2)
     * `docs/step6/spring-security-auth-design-guide.md`: Stateless 아키텍처 확인
   - **주의**: 
     * Stateless 아키텍처를 유지하는 경우에만 수행.
     * 향후 세션이 필요한 기능 추가 시 다시 추가 가능.
     * YAGNI 원칙 준수.

**보안 요구사항** (작업 분해 시 고려):
- Redis 인증 설정 (프로덕션 환경 필수)
- TLS/SSL 암호화 전송 (프로덕션 환경 권장)
- 환경 변수로 관리 (코드에 하드코딩 금지)

**환경변수 설정 예시** (`.env` 파일, 작업 분해 시 참고):
```bash
# Redis 설정
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=  # 프로덕션 환경에서는 필수
REDIS_SSL_ENABLED=false  # 프로덕션 환경에서는 true 권장
```

**에러 처리 시나리오** (작업 분해 시 참고, 에러 코드는 docs/step2/4. error-handling-strategy-design.md 참고):
- Redis 연결 실패: 503 Service Unavailable, 에러 코드 5003 (SERVICE_UNAVAILABLE)
- Redis 타임아웃: 504 Gateway Timeout, 에러 코드 5004 (GATEWAY_TIMEOUT)
- Redis 인증 실패: 401 Unauthorized, 에러 코드 4001 (AUTH_FAILED)
```

### 8단계: Client 모듈 구현

**단계 번호**: 8단계
**의존성**: 1단계 (프로젝트 구조 생성), 3단계 (Common 모듈 구현 완료 필수), 4단계 (Domain 모듈 구현 완료 권장)
**다음 단계**: 9단계 (Contest 및 News API 모듈 구현) 또는 10단계 (외부 API 통합 및 데이터 수집) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)

```
plan task: 외부 API 연동 Client 모듈 구현 (Contract 패턴 적용)

**description** (작업 설명):
- **작업 목표**:
  - json/sources.json에 정의된 Priority 1 출처와의 API 통합을 위한 Client 모듈 구현
  - Contract 패턴을 적용하여 Mock/Rest 모드 전환 가능한 구조 구현
  - RSS 피드 및 웹 스크래핑을 통한 데이터 수집 모듈 구현

- **배경**:
  - 현재 프로젝트는 외부 API, RSS 피드, 웹 스크래핑을 통해 데이터를 수집해야 함
  - client-feign 모듈은 이미 Contract 패턴으로 구현되어 있음 (참고 가능)
  - client-rss와 client-scraper 모듈은 아직 구현되지 않음

- **예상 결과**:
  - client-feign: Priority 1 API 출처와의 통합 완료
  - client-rss: 4개 RSS 출처로부터 데이터 수집 가능
  - client-scraper: 5개 웹 스크래핑 출처로부터 데이터 수집 가능

**requirements** (기술 요구사항 및 제약조건):
1. **공식 문서만 참고**: Spring Boot, OpenFeign, Rome, Jsoup 공식 문서만 참고
2. **오버엔지니어링 금지**: YAGNI 원칙 준수, 현재 요구사항에 맞는 최소한의 구조
3. **기존 패턴 준수**: client-feign의 Contract 패턴 구조를 참고하여 일관성 유지
4. **법적/윤리적 준수**: robots.txt, ToS 확인 필수 (client-scraper)

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: json/sources.json에 정의된 Priority 1 출처와의 API 통합, Contract 패턴 적용, RSS 피드 및 웹 스크래핑을 통한 데이터 수집 모듈 구현
  - 배경: 현재 프로젝트는 외부 API, RSS 피드, 웹 스크래핑을 통해 데이터를 수집해야 함, client-feign 모듈은 이미 Contract 패턴으로 구현되어 있음
  - 예상 결과: client-feign (Priority 1 API 출처 통합), client-rss (4개 RSS 출처), client-scraper (5개 웹 스크래핑 출처)
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - 공식 문서만 참고 (Spring Boot, OpenFeign, Rome, Jsoup)
  - 오버엔지니어링 금지 (YAGNI 원칙)
  - 기존 패턴 준수 (client-feign의 Contract 패턴 구조 참고)
  - 법적/윤리적 준수 (robots.txt, ToS 확인 필수)
- **작업 분해 및 우선순위** (참고 정보):
  - **Phase 1: client-feign 모듈 확장** (우선순위: 높음)
    * 의존성: 없음
    * 작업: Priority 1 API 출처와의 통합 (Codeforces, Kaggle, GitHub, HackerNews 등)
    * 참고: client/feign/domain/sample/ 구조
  - **Phase 2: client-rss 모듈 구현** (우선순위: 중간)
    * 의존성: common-core 모듈
    * 작업: 4개 RSS 출처 파서 구현
    * 참고: docs/step8/rss-scraper-modules-analysis.md
  - **Phase 3: client-scraper 모듈 구현** (우선순위: 중간)
    * 의존성: common-core 모듈
    * 작업: 5개 웹 스크래핑 출처 구현
    * 참고: docs/step8/rss-scraper-modules-analysis.md
  - **Phase 4: 공통 기능 구현** (우선순위: 낮음)
    * 의존성: Phase 1-3 완료 후
    * 작업: Rate Limiting, Retry 로직, 에러 핸들링
- **확인 사항**:
  - 작업 목표와 예상 결과 명확화
  - 기술적 도전 과제 및 핵심 결정 사항 파악
  - 기존 시스템/아키텍처와의 통합 요구사항 확인

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **핵심 설정 파일 및 구조 확인**:
  - 루트 디렉토리 구조 및 중요 설정 파일 확인 (build.gradle, settings.gradle, shrimp-rules.md 등)
  - shrimp-rules.md가 존재하면 상세히 읽고 참고
  - 주요 디렉토리 조직 및 모듈 구분 분석
- **아키텍처 패턴 식별**:
  - 핵심 설계 패턴 및 아키텍처 스타일 식별 (MSA 멀티모듈, CQRS 패턴, Contract 패턴 등)
  - 프로젝트의 계층 구조 및 모듈 경계 결정
  - 모듈 간 의존성 방향 확인 (API → Domain → Common → Client)
- **핵심 컴포넌트 분석**:
  - 주요 클래스/인터페이스 설계 및 의존성 연구
  - 핵심 서비스/유틸리티 클래스 및 그들의 책임과 용도 표시
  - client/feign 모듈의 Contract 패턴 구조 분석
- **기존 패턴 문서화**:
  - 발견된 코드 조직 방법 및 아키텍처 규칙 문서화
  - 프로젝트의 기술 스택 및 아키텍처 특성에 대한 깊은 이해 확립

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - 사용자에게 명확화 요청
  - `query_task`, `read_file`, `codebase_search` 또는 유사한 도구를 사용하여 기존 프로그램/아키텍처 조회
  - `web_search` 또는 기타 웹 검색 도구를 사용하여 익숙하지 않은 개념이나 기술 조회
- **추측 금지**: 모든 정보는 추적 가능한 출처가 있어야 함
- **정보 수집 대상**:
  - Spring Boot, OpenFeign, Rome, Jsoup 공식 문서 확인
  - json/sources.json에서 Priority 1 출처 상세 정보 확인
  - 기존 client 모듈의 구현 패턴 확인

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **정확한 검색 전략 사용**:
  - `read_file`, `codebase_search` 또는 유사한 도구를 사용하여 작업과 관련된 기존 구현 방법 조회
  - 현재 작업과 기능이 유사한 기존 코드 찾기
  - 디렉토리 구조를 분석하여 유사한 기능 모듈 찾기
- **기존 코드 확인** (check first, then design):
  - client/feign/domain/sample/ 구조 상세 분석
    * Contract 인터페이스 정의 방식 확인
    * DTO Record 클래스 구조 확인
    * FeignClient 인터페이스 작성 방식 확인
    * Api 구현체 작성 방식 확인
    * Mock 구현체 작성 방식 확인
    * Config 클래스 및 application.yml 설정 확인
  - client/feign/domain/oauth/ 구조 상세 분석 (복잡한 구조 참고)
    * 여러 Provider 처리 방식 확인
    * 여러 FeignClient 사용 방식 확인
- **코드 스타일 및 규칙 분석**:
  - 기존 컴포넌트의 명명 규칙 확인 (camelCase 등)
  - 주석 스타일 및 형식 규칙 확인
  - 에러 처리 패턴 및 로깅 방법 분석
- **발견된 패턴 기록 및 준수**:
  - 코드 패턴 및 조직 구조를 상세히 문서화
  - 설계에서 이러한 패턴을 확장하는 방법 계획
- **기존 기능과의 중복 확인**:
  - 기존 기능과의 중복 여부 확인 및 "재사용" 또는 "추상화 및 리팩토링" 결정
- **중요**: 기존 코드를 확인하기 전에 설계를 생성하지 않음, 반드시 "check first, then design" 원칙 준수

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **Backend API Tasks 가이드라인** (이 작업에 해당):
  - API route 구조 및 명명 규칙 확인
  - 요청 처리 및 미들웨어 패턴 분석
  - 에러 처리 및 응답 형식 표준 확인
  - 인증/인가 구현 방법 이해
  - Contract 패턴 적용 방법 확인 (client/feign 모듈 참고)
- **Client 모듈 구현 가이드라인**:
  - Contract 패턴 구조 준수 (client/feign 모듈 참고)
  - Mock/Rest 모드 전환 가능한 구조 구현
  - 설정 관리 패턴 확인 (@ConfigurationProperties, application.yml)
  - 테스트 코드 작성 패턴 확인 (client/feign 모듈 테스트 코드 참고)

**6단계: Preliminary Solution Output (예비 솔루션 출력)**
- **예비 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "예비 설계 솔루션" 작성
  - **사실(facts)**과 **추론(inferences)**을 명확히 표시 (출처 vs 선택 기준)
  - 모호한 진술 금지, 최종 결과물 내용이어야 함
  - 프로젝트의 기존 아키텍처 패턴과 일관성 있는 솔루션 보장
  - 기존 컴포넌트 재사용 또는 기존 패턴 준수 방법 설명
- **사고 과정**:
  - 단계별로 사고하고 생각을 정리, 문제가 너무 복잡하면 `process_thought` 활용
- **중요 경고**: 모든 형태의 `assumptions`, `guesses`, `imagination`은 엄격히 금지됨. 사용 가능한 모든 도구를 사용하여 실제 정보를 수집해야 함
- **도구 호출**:
  - 반드시 `analyze_task` 도구를 호출하여 다음 단계로 전달:
    ```
    analyze_task({ 
      summary: <작업 요약>, 
      initialConcept: <초기 개념> 
    })
    ```

**중요**: 위 6단계 프로세스를 반드시 순서대로 수행하세요. 아래의 "참고 정보" 섹션들은 6단계 프로세스 수행 중 필요시 참고하세요.

---

## 참고 정보

**주요 참고 문서**:
- **구현 가이드**: 
  - docs/step8/rss-scraper-modules-analysis.md
    * client-rss 모듈: 전체 섹션 참고
    * client-scraper 모듈: 전체 섹션 참고
    * 공통 기능: 데이터 수집 전략 섹션 참고
  - docs/step8/slack-integration-design-guide.md
    * client-slack 모듈: 전체 섹션 참고
    * Contract 패턴, Block Kit 메시지 빌더, Rate Limiting 구현 가이드
- **구조 참고**: client/feign/domain/sample/ (Contract 패턴 예시)
- **설정 참고**: client/feign/src/main/resources/application-feign-sample.yml
- **출처 정보**: json/sources.json (Priority 1 출처부터 구현)
- **프로젝트 목표**: docs/reference/shrimp-task-prompts-final-goal.md

**참고 파일**: 
- docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표)
- json/sources.json (Priority 1 출처부터 구현)
- docs/step8/rss-scraper-modules-analysis.md (RSS 및 Scraper 모듈 분석 문서 - 구현 가이드 참고)
- docs/step8/slack-integration-design-guide.md (Slack 연동 설계 및 구현 가이드 - 전체 섹션 참고)
- client/feign/domain/sample/ (Sample 보일러플레이트 - 기본 구조 참고)
- client/feign/domain/oauth/ (OAuth 구현 예시 - 복잡한 구조 참고)
- client/feign/config/SampleFeignConfig.java (Config 예시)
- client/feign/src/main/resources/application-feign-sample.yml (설정 파일 예시)

**참고: 상세 구현 가이드** (6단계 프로세스 수행 중 필요시 참고):
1. client-feign 모듈 구현 (Contract 패턴 적용)
   
   **패키지 구조**:
   ```
   client/feign/
   ├── config/
   │   ├── OpenFeignConfig.java
   │   └── {Domain}FeignConfig.java (또는 domain/{domain}/config/{Domain}FeignConfig.java)
   └── domain/
       └── {domain-name}/
           ├── api/
           │   └── {Domain}Api.java (Contract 구현체)
           ├── client/
           │   └── {Domain}FeignClient.java (@FeignClient)
           ├── contract/
           │   ├── {Domain}Contract.java (인터페이스)
           │   └── {Domain}Dto.java (Record 클래스)
           └── mock/
               └── {Domain}Mock.java (테스트용 구현체)
   ```
   
   **구현 단계**:
   
   a. Contract 인터페이스 정의
      * **역할**: 도메인별 API 계약 정의
      * **책임**: 비즈니스 메서드 시그니처 정의
      * **구현 규칙**:
        - 메서드 시그니처는 실제 비즈니스 요구사항에 맞게 정의
        - 단순한 경우: DTO Record를 파라미터/반환값으로 사용
        - 복잡한 경우: 기본 타입(String, Integer 등)을 직접 사용 가능
      * **예제**:
        ```java
        // client/feign/domain/{domain}/contract/{Domain}Contract.java
        package com.tech.n.ai.client.feign.domain.{domain}.contract;
        
        public interface {Domain}Contract {
            {Domain}Dto.{Domain}ApiResponse get{Domain}({Domain}Dto.{Domain}ApiRequest request);
        }
        ```
      * **참고 파일**: 
        - `client/feign/domain/sample/contract/SampleContract.java` (단순 예시)
        - `client/feign/domain/oauth/contract/OAuthProviderContract.java` (복잡한 예시)
   
   b. DTO 클래스 정의 (Record 사용)
      * **역할**: API 요청/응답 데이터 구조 정의
      * **책임**: 요청 및 응답 데이터 타입 정의
      * **구현 규칙**:
        - Record에 `@Builder` 어노테이션 필수 사용
        - 여러 Provider별 Response가 필요한 경우 하나의 Dto 클래스에 중첩 Record로 정의
        - 외부 API 응답 구조를 그대로 반영하는 Record와 내부 비즈니스용 Record를 구분
      * **예제**:
        ```java
        // client/feign/domain/{domain}/contract/{Domain}Dto.java
        package com.tech.n.ai.client.feign.domain.{domain}.contract;
        
        import lombok.Builder;
        
        public class {Domain}Dto {
            @Builder
            public record {Domain}ApiRequest(
                String param1,
                Integer param2
            ) {}
            
            @Builder
            public record {Domain}ApiResponse(
                String result1,
                Integer result2
            ) {}
            
            // 외부 API 응답 구조를 그대로 반영하는 Record (필요시)
            @Builder
            public record {Domain}ExternalApiResponse(
                String field1,
                Integer field2
            ) {}
        }
        ```
      * **참고 파일**: 
        - `client/feign/domain/sample/contract/SampleDto.java` (기본 예시)
        - `client/feign/domain/oauth/contract/OAuthDto.java` (복잡한 예시 - 여러 Provider별 Response 포함)
   
   c. FeignClient 인터페이스 작성
      * **역할**: OpenFeign 클라이언트 인터페이스
      * **책임**: HTTP 요청 매핑 및 외부 API 호출
      * **구현 규칙**:
        - `@FeignClient`의 `name`은 대문자로 시작 (예: `SampleFeign`, `GoogleOAuth`)
        - `url`은 `${feign-clients.{domain}.uri}` 형식 사용
        - `consumes`와 `produces` 모두 명시 (APPLICATION_JSON_VALUE 또는 APPLICATION_FORM_URLENCODED_VALUE 등)
        - HTTP 메서드는 실제 API에 맞게 선택 (GET, POST 등)
        - 파라미터 타입: `@RequestBody`, `@RequestHeader`, `@RequestParam`, `MultiValueMap` 등 적절히 사용
        - 여러 FeignClient가 필요한 경우 (예: OAuth의 경우 token과 userinfo가 다른 URI) 별도 Client 생성
      * **예제**:
        ```java
        // client/feign/domain/{domain}/client/{Domain}FeignClient.java
        package com.tech.n.ai.client.feign.domain.{domain}.client;
        
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Dto;
        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.http.MediaType;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PostMapping;
        import org.springframework.web.bind.annotation.RequestBody;
        import org.springframework.web.bind.annotation.RequestHeader;
        
        @FeignClient(name = "{Domain}Feign", url = "${feign-clients.{domain}.uri}")
        public interface {Domain}FeignClient {
            @PostMapping(
                value = "/{domain}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE
            )
            {Domain}Dto.{Domain}ApiResponse get{Domain}(@RequestBody {Domain}Dto.{Domain}ApiRequest request);
            
            // GET 메서드 예시
            @GetMapping(value = "/{domain}/info", produces = MediaType.APPLICATION_JSON_VALUE)
            {Domain}Dto.{Domain}ApiResponse getInfo(@RequestHeader("Authorization") String authorization);
        }
        ```
      * **참고 파일**: 
        - `client/feign/domain/sample/client/SampleFeignClient.java` (기본 예시)
        - `client/feign/domain/oauth/client/GoogleOAuthFeignClient.java` (복잡한 예시 - 여러 메서드, 다양한 파라미터)
      * **OpenFeign 클라이언트 목록** (json/sources.json 참고):
        * CodeforcesFeignClient
        * KaggleFeignClient
        * GitHubFeignClient
        * HackerNewsFeignClient
        * NewsFeignClient
        * RedditFeignClient
        * DevToFeignClient
        * ProductHuntFeignClient
   
   d. Api 구현체 작성 (Contract 구현)
      * **역할**: Contract 인터페이스의 실제 구현체 (FeignClient 사용)
      * **책임**: FeignClient를 통한 외부 API 호출 및 에러 핸들링
      * **구현 규칙**:
        - `@RequiredArgsConstructor` 사용 (Lombok)
        - `@Slf4j` 사용 (로깅)
        - `@Component`는 선택사항 (Config에서 Bean으로 등록하므로 생략 가능)
        - FeignClient 호출 후 응답 검증 및 에러 핸들링 포함
        - 필요시 비즈니스 로직 포함 (단순 위임이 아닐 수 있음)
      * **예제**:
        ```java
        // client/feign/domain/{domain}/api/{Domain}Api.java
        package com.tech.n.ai.client.feign.domain.{domain}.api;
        
        import com.tech.n.ai.client.feign.domain.{domain}.client.{Domain}FeignClient;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Contract;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Dto.{Domain}ApiRequest;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Dto.{Domain}ApiResponse;
        import lombok.RequiredArgsConstructor;
        import lombok.extern.slf4j.Slf4j;
        
        @Slf4j
        @RequiredArgsConstructor
        public class {Domain}Api implements {Domain}Contract {
            private final {Domain}FeignClient {domain}Feign;
            
            @Override
            public {Domain}ApiResponse get{Domain}({Domain}ApiRequest request) {
                {Domain}ApiResponse response = {domain}Feign.get{Domain}(request);
                
                if (response == null) {
                    log.error("{Domain} API 호출 실패: response is null");
                    throw new RuntimeException("{Domain} API 호출에 실패했습니다.");
                }
                
                return response;
            }
        }
        ```
      * **참고 파일**: 
        - `client/feign/domain/sample/api/SampleApi.java` (기본 예시)
        - `client/feign/domain/oauth/api/GoogleOAuthApi.java` (복잡한 예시 - 에러 핸들링, 비즈니스 로직 포함)
   
   e. Mock 구현체 작성 (테스트용)
      * **역할**: Contract 인터페이스의 Mock 구현체 (테스트/개발용)
      * **책임**: 외부 API 없이 테스트 가능한 Mock 응답 제공
      * **구현 규칙**:
        - `@Slf4j` 사용 (로깅)
        - `@Builder`를 사용하여 Mock 응답 생성
        - 의미 있는 Mock 데이터 반환
      * **예제**:
        ```java
        // client/feign/domain/{domain}/mock/{Domain}Mock.java
        package com.tech.n.ai.client.feign.domain.{domain}.mock;
        
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Contract;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Dto.{Domain}ApiRequest;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Dto.{Domain}ApiResponse;
        import lombok.extern.slf4j.Slf4j;
        
        @Slf4j
        public class {Domain}Mock implements {Domain}Contract {
            @Override
            public {Domain}ApiResponse get{Domain}({Domain}ApiRequest request) {
                log.info("Mock {Domain} API called with request: {}", request);
                return {Domain}ApiResponse.builder()
                    .result1("mock-result")
                    .result2(0)
                    .build();
            }
        }
        ```
      * **참고 파일**: 
        - `client/feign/domain/sample/mock/SampleMock.java` (기본 예시)
        - `client/feign/domain/oauth/mock/GoogleOAuthMock.java` (복잡한 예시)
   
   f. FeignConfig 클래스 작성 (ConditionalOnProperty로 선택)
      * **역할**: Contract 구현체 빈 등록 및 Mock/Rest 선택
      * **책임**: 
        * FeignClient 활성화 (`@EnableFeignClients`)
        * ConditionalOnProperty로 mock/rest 모드 선택
        * OpenFeignConfig Import
      * **구현 규칙**:
        - Config 위치: 단순한 경우 `config/{Domain}FeignConfig.java`, 복잡한 경우 `domain/{domain}/config/{Domain}FeignConfig.java`
        - 여러 Provider가 있는 경우 `@Bean(name = "...")` 사용하여 구분
        - 여러 FeignClient를 등록할 수 있음
        - `CLIENT_MODE` 상수는 `"feign-clients.{domain}.mode"` 형식
      * **예제 (단순한 경우)**:
        ```java
        // client/feign/config/{Domain}FeignConfig.java
        package com.tech.n.ai.client.feign.config;
        
        import com.tech.n.ai.client.feign.domain.{domain}.api.{Domain}Api;
        import com.tech.n.ai.client.feign.domain.{domain}.client.{Domain}FeignClient;
        import com.tech.n.ai.client.feign.domain.{domain}.contract.{Domain}Contract;
        import com.tech.n.ai.client.feign.domain.{domain}.mock.{Domain}Mock;
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.cloud.openfeign.EnableFeignClients;
        import org.springframework.context.annotation.Bean;
        import org.springframework.context.annotation.Configuration;
        import org.springframework.context.annotation.Import;
        
        @EnableFeignClients(clients = {
            {Domain}FeignClient.class,
        })
        @Import({
            OpenFeignConfig.class
        })
        @Configuration
        public class {Domain}FeignConfig {
            private static final String CLIENT_MODE = "feign-clients.{domain}.mode";
            
            @Bean
            @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
            public {Domain}Contract {domain}Mock() { 
                return new {Domain}Mock(); 
            }
            
            @Bean
            @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
            public {Domain}Contract {domain}Api({Domain}FeignClient feignClient) { 
                return new {Domain}Api(feignClient); 
            }
        }
        ```
      * **예제 (여러 Provider가 있는 경우)**:
        ```java
        // client/feign/domain/{domain}/config/{Domain}FeignConfig.java
        @EnableFeignClients(clients = {
            {Provider1}FeignClient.class,
            {Provider2}FeignClient.class,
        })
        @Import({
            OpenFeignConfig.class
        })
        @Configuration
        public class {Domain}FeignConfig {
            private static final String CLIENT_MODE = "feign-clients.{domain}.mode";
            
            @Bean(name = "{provider1}Contract")
            @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "mock")
            public {Domain}Contract {provider1}Mock() {
                return new {Provider1}Mock();
            }
            
            @Bean(name = "{provider1}Contract")
            @ConditionalOnProperty(name = CLIENT_MODE, havingValue = "rest")
            public {Domain}Contract {provider1}Api({Provider1}FeignClient feignClient) {
                return new {Provider1}Api(feignClient);
            }
            
            // Provider2도 동일한 패턴으로 추가
        }
        ```
      * **참고 파일**: 
        - `client/feign/config/SampleFeignConfig.java` (단순 예시)
        - `client/feign/domain/oauth/config/OAuthFeignConfig.java` (복잡한 예시 - 여러 Provider)
   
   g. application.yml 설정 추가
      * **역할**: Feign Client 설정 및 모드 선택
      * **구현 규칙**:
        - 파일명: `application-feign-{domain}.yml`
        - 프로파일별 설정 분리 (`spring.config.activate.on-profile`)
        - 환경변수 사용 (`${ENV_VAR:default-value}` 형식)
        - 여러 URI가 필요한 경우 중첩 구조 사용
        - `spring.cloud.openfeign.client.config`로 각 FeignClient별 타임아웃 설정
        - prod 프로파일에서는 `okhttp.enabled: true`, `httpclient` 설정 포함
      * **예제 (단순한 경우)**:
        ```yaml
        # client/feign/src/main/resources/application-feign-{domain}.yml
        feign-clients:
          {domain}:
            mode: rest
            uri: http://localhost:8080
        
        ---
        spring:
          config.activate.on-profile: test
        feign-clients:
          {domain}:
            mode: mock
            uri: http://localhost:8080
        
        ---
        spring:
          config.activate.on-profile: local, dev
          cloud:
            openfeign:
              client:
                config:
                  {Domain}Feign:
                    readTimeout: 30000
                    connectTimeout: 3000
        
        feign-clients:
          {domain}:
            mode: rest
            uri: ${FEIGN_CLIENTS_{DOMAIN}_URI}
        
        ---
        spring:
          config.activate.on-profile: prod
          cloud:
            openfeign:
              okhttp:
                enabled: true
              httpclient:
                max-connections: 35000
                max-connections-per-route: 35000
                connection-timeout: 120000
                hc5:
                  connection-request-timeout: 120000
                  socket-timeout: 120000
              client:
                config:
                  {Domain}Feign:
                    readTimeout: 30000
                    connectTimeout: 3000
        
        feign-clients:
          {domain}:
            mode: rest
            uri: ${FEIGN_CLIENTS_{DOMAIN}_URI}
        ```
      * **예제 (여러 URI가 필요한 경우)**:
        ```yaml
        feign-clients:
          {domain}:
            mode: rest
            {provider1}:
              uri: ${FEIGN_CLIENTS_{DOMAIN}_{PROVIDER1}_URI:https://api1.example.com}
            {provider2}:
              uri: ${FEIGN_CLIENTS_{DOMAIN}_{PROVIDER2}_URI:https://api2.example.com}
              userinfo:
                uri: ${FEIGN_CLIENTS_{DOMAIN}_{PROVIDER2}_USERINFO_URI:https://api2-userinfo.example.com}
        ```
      * **참고 파일**: 
        - `client/feign/src/main/resources/application-feign-sample.yml` (단순 예시)
        - `client/feign/src/main/resources/application-feign-oauth.yml` (복잡한 예시 - 여러 URI, 프로파일별 설정)
   
   - OpenFeignConfig 설정 (로깅, 에러 핸들링, Retry) - 현재는 기본 구조만 있음
   - Rate Limiting 관리 (Redis 활용) - 향후 구현 예정

2. client-rss 모듈 구현
   
   **참고 문서**: `docs/step8/rss-scraper-modules-analysis.md` (client-rss 모듈 섹션 참고)
   
   **대상 출처** (json/sources.json 기준, total_score 순서):
   - Google Developers Blog RSS (total_score: 36, Priority: 1, 피드 형식: Atom 1.0)
   - TechCrunch RSS (total_score: 35, Priority: 1, 피드 형식: RSS 2.0)
   - Ars Technica RSS (total_score: 34, Priority: 2, 피드 형식: RSS 2.0)
   - Medium Technology RSS (total_score: 30, Priority: 2, 피드 형식: RSS 2.0)
   
   **기술 스택**:
   - **라이브러리**: Rome (RSS/Atom 피드 파싱)
     * Maven/Gradle: `com.rometools:rome:1.19.0`
     * 공식 문서: https://rometools.github.io/rome/
     * RSS 2.0 및 Atom 1.0 형식 모두 지원
   - **HTTP 클라이언트**: Spring WebClient (비동기 HTTP 요청)
     * `WebClient.Builder`를 빈으로 등록하여 재사용
     * `application.yml`을 통한 타임아웃, 연결 풀 설정 관리
   - **에러 핸들링**: Resilience4j (재시도 로직)
     * 비동기 지원, Circuit Breaker 패턴 제공
     * 최대 3회 재시도, 지수 백오프(exponential backoff) 적용
   - **Rate Limiting**: Redis 기반 (출처별 요청 간격 관리)
   
   **구현 구조** (클린코드 원칙 준수: SRP, DIP, OCP):
   ```
   client/rss/
   ├── parser/
   │   ├── RssParser.java (인터페이스 - DIP 준수)
   │   ├── TechCrunchRssParser.java (구현체 - SRP 준수)
   │   ├── GoogleDevelopersBlogRssParser.java
   │   ├── ArsTechnicaRssParser.java
   │   └── MediumTechnologyRssParser.java
   ├── dto/
   │   └── RssFeedItem.java (파싱된 RSS 아이템 DTO)
   ├── config/
   │   ├── RssParserConfig.java (WebClient 빈 설정)
   │   └── RssProperties.java (@ConfigurationProperties - 설정 관리)
   └── util/
       ├── RssFeedValidator.java (피드 검증 - SRP 준수)
       └── RssDataCleaner.java (데이터 정제 - SRP 준수)
   ```
   
   **구현 규칙**:
   - Spring Boot 베스트 프랙티스 준수:
     * 생성자 주입 패턴 사용 (`@RequiredArgsConstructor`)
     * `@ConfigurationProperties`를 통한 설정 관리
     * Resilience4j를 활용한 재시도 로직 (Thread.sleep() 제거)
     * `WebClient.Builder` 빈 설정 패턴
   - 클린코드 원칙:
     * 단일 책임 원칙 (SRP): 각 클래스가 하나의 책임만 담당
     * 의존성 역전 원칙 (DIP): 인터페이스 기반 설계
     * 개방-폐쇄 원칙 (OCP): 확장에는 열려있고 수정에는 닫혀있음
   - 오버엔지니어링 방지:
     * 현재 요구사항(4개 RSS 출처)에 맞는 최소한의 구조
     * 불필요한 팩토리 패턴이나 복잡한 추상화 제거
   
   **의존성 추가** (build.gradle):
   ```gradle
   dependencies {
       implementation project(':common-core')
       implementation 'com.rometools:rome:1.19.0'
       implementation 'org.springframework.boot:spring-boot-starter-webflux'
       implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
       implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
       annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
       
       // 테스트 의존성
       testImplementation 'org.springframework.boot:spring-boot-starter-test'
       testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }

   // Disable failure when no tests are discovered (test files exist but are commented out)
   tasks.named('test') {
       failOnNoDiscoveredTests = false
   }
   ```
   
   **application.yml 설정 예시**:
   ```yaml
   rss:
     timeout-seconds: 30
     max-retries: 3
     retry-delay-ms: 1000
     sources:
       google-developers-blog:
         feed-url: https://developers.googleblog.com/feeds/posts/default
         feed-format: ATOM_1.0
         update-frequency: 주간
       techcrunch:
         feed-url: https://techcrunch.com/feed/
         feed-format: RSS_2.0
         update-frequency: 일일
       ars-technica:
         feed-url: https://feeds.arstechnica.com/arstechnica/index
         feed-format: RSS_2.0
         update-frequency: 일일
       medium-technology:
         feed-url: https://medium.com/feed/tag/technology
         feed-format: RSS_2.0
         update-frequency: 일일
   
   # Resilience4j 설정은 공통 섹션 참고 (아래 client-scraper 모듈의 resilience4j 설정과 동일)
   resilience4j:
     retry:
       configs:
         default:
           max-attempts: 3
           wait-duration: 1000ms
           exponential-backoff-multiplier: 2
           retry-exceptions:
             - org.springframework.web.reactive.function.client.WebClientException
             - java.io.IOException
       instances:
         rssRetry:
           base-config: default
   ```
   
   **테스트 코드 작성** (client/feign 모듈과 일관된 형식):
   - **참고 문서**: `docs/step8/rss-scraper-modules-analysis.md` (4. 테스트 코드 작성 섹션 참고)
   - **테스트 컨텍스트**: `RssTestContext` 클래스 작성 (`@ImportAutoConfiguration`, `@Import` 사용)
   - **테스트 예시**: `TechCrunchRssParserTest` (Given-When-Then 패턴, `@SpringBootTest`, `@DisplayName` 사용)
   - **테스트 작성 가이드라인**:
     * `client/feign` 모듈과 동일하게 Given-When-Then 패턴 사용
     * `@SpringBootTest` 사용, `classes`에 테스트 컨텍스트와 Config 클래스 지정
     * `@DisplayName`으로 테스트 목적 명시, `@Test`는 주석 처리하여 기본적으로 비활성화
     * `@Autowired`로 테스트 대상 컴포넌트 주입, JUnit 5의 `Assertions` 사용
     * 실제 외부 API/RSS 피드에 의존하지 않도록 Mock 또는 테스트용 설정 사용 권장

3. client-scraper 모듈 구현
   
   **참고 문서**: `docs/step8/rss-scraper-modules-analysis.md` (client-scraper 모듈 섹션 참고)
   
   **대상 출처** (json/sources.json 기준, total_score 순서, 상위 5개):
   - LeetCode Contests (total_score: 32, Priority: 2, data_format: GraphQL/JSON 우선, HTML 대안)
   - Google Summer of Code (total_score: 32, Priority: 2, data_format: HTML)
   - Devpost (total_score: 30, Priority: 2, data_format: HTML)
   - Major League Hacking (MLH) (total_score: 29, Priority: 2, data_format: HTML)
   - AtCoder (total_score: 28, Priority: 2, data_format: HTML)
   
   **기술 스택**:
   - **라이브러리**:
     * **Jsoup** (정적 HTML 파싱, 권장)
       - Maven/Gradle: `org.jsoup:jsoup:1.17.2`
       - 공식 문서: https://jsoup.org/
       - Spring WebClient로 HTML을 가져온 후 Jsoup으로 파싱하는 패턴 권장
     * **Selenium WebDriver** (동적 콘텐츠 처리, 최소화)
       - Maven/Gradle: `org.seleniumhq.selenium:selenium-java:4.15.0`
       - 사용 시점: JavaScript로 동적으로 생성되는 콘텐츠가 필요한 경우에만 사용
       - 주의사항: 리소스 집약적이므로 정적 HTML 파싱으로 충분한 경우 사용하지 않음
     * **crawler-commons** (robots.txt 파싱, 권장)
       - Maven/Gradle: `com.github.crawler-commons:crawler-commons:1.2`
       - GitHub: https://github.com/crawler-commons/crawler-commons
   - **HTTP 클라이언트**: Spring WebClient (권장)
     * WebClient로 HTML을 가져온 후 Jsoup으로 파싱
     * 비동기 처리, 타임아웃 설정, 재시도 로직 통합 용이
   - **에러 핸들링**: Resilience4j (재시도 로직)
     * 최대 3회 재시도, 지수 백오프(exponential backoff) 적용
   - **Rate Limiting**: Redis 기반 (출처별 요청 간격 관리)
     * 기본 간격: 최소 1초 이상 (출처별 설정 가능)
   
   **구현 구조** (클린코드 원칙 준수: SRP, DIP, OCP):
   ```
   client/scraper/
   ├── scraper/
   │   ├── WebScraper.java (인터페이스 - DIP 준수)
   │   ├── LeetCodeScraper.java (GraphQL 우선, HTML 대안)
   │   ├── GoogleSummerOfCodeScraper.java
   │   ├── DevpostScraper.java
   │   ├── MLHScraper.java
   │   └── AtCoderScraper.java
   ├── scraper/selenium/ (선택사항 - 동적 콘텐츠가 필요한 경우만)
   │   └── SeleniumWebScraper.java (인터페이스)
   ├── dto/
   │   └── ScrapedContestItem.java (스크래핑된 대회 정보 DTO)
   ├── config/
   │   ├── ScraperConfig.java (WebClient 빈 설정)
   │   └── ScraperProperties.java (@ConfigurationProperties - 설정 관리)
   ├── util/
   │   ├── RobotsTxtChecker.java (robots.txt 확인 - SRP 준수)
   │   ├── ScrapedDataValidator.java (데이터 검증 - SRP 준수)
   │   └── ScrapedDataCleaner.java (데이터 정제 - SRP 준수)
   └── exception/
       └── ScrapingException.java (스크래핑 예외)
   ```
   
   **구현 규칙**:
   - Spring Boot 베스트 프랙티스 준수:
     * 생성자 주입 패턴 사용 (`@RequiredArgsConstructor`)
     * `@ConfigurationProperties`를 통한 설정 관리
     * Resilience4j를 활용한 재시도 로직
     * WebClient와 Jsoup 통합 패턴 (WebClient로 HTML 가져온 후 Jsoup으로 파싱)
   - 클린코드 원칙:
     * 단일 책임 원칙 (SRP): 각 클래스가 하나의 책임만 담당
     * 의존성 역전 원칙 (DIP): 인터페이스 기반 설계
     * 개방-폐쇄 원칙 (OCP): 확장에는 열려있고 수정에는 닫혀있음
   - 오버엔지니어링 방지:
     * 현재 요구사항(5개 웹 스크래핑 출처)에 맞는 최소한의 구조
     * 팩토리 패턴 제거 (단순 @Component 주입으로 충분)
     * Selenium 사용 최소화 (정적 HTML 파싱으로 충분한 경우 Jsoup만 사용)
   - 법적/윤리적 고려사항 (필수):
     * **robots.txt 준수**: 모든 웹 스크래핑 전 필수 확인 (crawler-commons 활용)
       - Disallow 경로는 절대 스크래핑하지 않음
       - Crawl-delay 지시사항이 있으면 해당 간격 준수
     * **Terms of Service (ToS) 확인**: 각 웹사이트의 ToS에서 스크래핑 관련 조항 확인
       - 명시적으로 금지된 경우 해당 출처 제외
       - 불명확한 경우 보수적으로 접근 (스크래핑 자제)
     * **Rate Limiting**: 최소 1초 간격 유지, robots.txt의 Crawl-delay 지시사항 준수
     * **User-Agent 설정**: 명확한 프로젝트 식별자 포함 (예: `ShrimpTM-Demo/1.0 (+https://github.com/your-repo)`)
   
   **의존성 추가** (build.gradle):
   ```gradle
   dependencies {
       implementation project(':common-core')
       implementation 'org.jsoup:jsoup:1.17.2'
       implementation 'com.github.crawler-commons:crawler-commons:1.2'
       implementation 'org.springframework.boot:spring-boot-starter-webflux'
       implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
       implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
       // Selenium (선택사항 - 필요한 경우만)
       // implementation 'org.seleniumhq.selenium:selenium-java:4.15.0'
       annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
       
       // 테스트 의존성
       testImplementation 'org.springframework.boot:spring-boot-starter-test'
       testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }

   // Disable failure when no tests are discovered (test files exist but are commented out)
   tasks.named('test') {
       failOnNoDiscoveredTests = false
   }
   ```
   
   **application.yml 설정 예시**:
   ```yaml
   scraper:
     timeout-seconds: 30
     max-retries: 3
     retry-delay-ms: 1000
     user-agent: "ShrimpTM-Demo/1.0 (+https://github.com/your-repo)"
     sources:
       leetcode:
         base-url: https://leetcode.com
         data-format: GraphQL
         min-interval-seconds: 1
         requires-selenium: false
       google-summer-of-code:
         base-url: https://summerofcode.withgoogle.com
         data-format: HTML
         min-interval-seconds: 1
         requires-selenium: false
       devpost:
         base-url: https://devpost.com
         data-format: HTML
         min-interval-seconds: 1
         requires-selenium: false
       mlh:
         base-url: https://mlh.io
         data-format: HTML
         min-interval-seconds: 1
         requires-selenium: false
       atcoder:
         base-url: https://atcoder.jp
         data-format: HTML
         min-interval-seconds: 1
         requires-selenium: false
   
   # Resilience4j 기본 설정은 위 client-rss 모듈과 동일 (configs.default)
   # scraper 모듈은 HttpStatusException도 재시도 대상에 포함
   resilience4j:
     retry:
       configs:
         default:
           max-attempts: 3
           wait-duration: 1000ms
           exponential-backoff-multiplier: 2
           retry-exceptions:
             - org.springframework.web.reactive.function.client.WebClientException
             - java.io.IOException
             - org.jsoup.HttpStatusException
       instances:
         scraperRetry:
           base-config: default
   ```
   
   **테스트 코드 작성** (client/feign 모듈과 일관된 형식):
   - **참고 문서**: `docs/step8/rss-scraper-modules-analysis.md` (4. 테스트 코드 작성 섹션 참고)
   - **테스트 컨텍스트**: `ScraperTestContext` 클래스 작성 (`@ImportAutoConfiguration`, `@Import` 사용)
   - **테스트 예시**: `DevpostScraperTest` (Given-When-Then 패턴, `@SpringBootTest`, `@DisplayName` 사용)
   - **테스트 작성 가이드라인**:
     * `client/feign` 모듈과 동일하게 Given-When-Then 패턴 사용
     * `@SpringBootTest` 사용, `classes`에 테스트 컨텍스트와 Config 클래스 지정
     * `@DisplayName`으로 테스트 목적 명시, `@Test`는 주석 처리하여 기본적으로 비활성화
     * `@Autowired`로 테스트 대상 컴포넌트 주입, JUnit 5의 `Assertions` 사용
     * 실제 외부 웹사이트에 의존하지 않도록 Mock 또는 테스트용 설정 사용 권장

4. client-slack 모듈 구현
   
   **참고 문서**: `docs/step8/slack-integration-design-guide.md` (전체 섹션 참고)
   
   **구현 구조** (Contract 패턴 적용, client/feign 모듈과 일관성 유지):
   ```
   client/slack/
   ├── config/
   │   └── SlackConfig.java (빈 설정)
   ├── domain/
   │   └── slack/
   │       ├── client/
   │       │   ├── SlackClient.java (인터페이스)
   │       │   ├── SlackWebhookClient.java (구현체)
   │       │   └── SlackBotClient.java (구현체, 선택사항)
   │       ├── contract/
   │       │   ├── SlackContract.java (인터페이스)
   │       │   └── SlackDto.java (DTO)
   │       ├── api/
   │       │   └── SlackApi.java (Contract 구현체)
   │       ├── service/
   │       │   ├── SlackNotificationService.java (인터페이스)
   │       │   └── SlackNotificationServiceImpl.java (구현체)
   │       └── builder/
   │           └── SlackMessageBuilder.java (Block Kit 메시지 빌더)
   └── util/
       └── SlackRateLimiter.java (Redis 기반 Rate Limiting)
   ```
   
   **계층 구조**:
   - **SlackClient**: HTTP 통신 담당 (SlackWebhookClient, SlackBotClient)
   - **SlackContract**: 비즈니스 메서드 시그니처 정의 (SlackApi가 구현)
   - **SlackNotificationService**: 고수준 알림 서비스 (SlackNotificationServiceImpl이 구현)
   
   **기술 스택**:
   - **HTTP 클라이언트**: Spring WebClient (비동기 HTTP 요청)
   - **메시지 포맷**: Block Kit (SlackMessageBuilder 활용)
   - **Rate Limiting**: Redis 기반 (SlackRateLimiter 활용)
   - **에러 핸들링**: Resilience4j (재시도 로직)
   - **설정 관리**: `@ConfigurationProperties` (SlackProperties)
   
   **구현 규칙**:
   - Contract 패턴 적용: `client/feign` 모듈과 동일한 패턴
   - Spring Boot 베스트 프랙티스:
     * 생성자 주입 패턴 사용 (`@RequiredArgsConstructor`)
     * `@ConfigurationProperties`를 통한 설정 관리
     * Resilience4j를 활용한 재시도 로직
     * `WebClient.Builder` 빈 설정 패턴
   - 클린코드 원칙:
     * 단일 책임 원칙 (SRP): 각 클래스가 하나의 책임만 담당
     * 의존성 역전 원칙 (DIP): 인터페이스 기반 설계
     * 개방-폐쇄 원칙 (OCP): 확장에는 열려있고 수정에는 닫혀있음
   - 오버엔지니어링 방지:
     * 현재 요구사항에 맞는 최소한의 구조
     * Bot API는 선택사항 (Webhook으로 충분한 경우)
   
   **의존성 추가** (build.gradle):
   ```gradle
   dependencies {
       implementation project(':common-core')
       
       // Spring WebFlux (WebClient 사용)
       implementation 'org.springframework.boot:spring-boot-starter-webflux'
       
       // Resilience4j (재시도 로직)
       implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.1.0'
       implementation 'io.github.resilience4j:resilience4j-reactor:2.1.0'
       
       // Configuration Processor (application.yml 자동완성)
       annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor'
       
       // 테스트 의존성
       testImplementation 'org.springframework.boot:spring-boot-starter-test'
       testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
   }

   // Disable failure when no tests are discovered (test files exist but are commented out)
   tasks.named('test') {
       failOnNoDiscoveredTests = false
   }
   ```
   
   **application.yml 설정 예시**:
   ```yaml
   slack:
     webhook:
       url: ${SLACK_WEBHOOK_URL:}
       enabled: ${SLACK_WEBHOOK_ENABLED:true}
     bot:
       token: ${SLACK_BOT_TOKEN:}
       enabled: ${SLACK_BOT_ENABLED:false}
     default-channel: ${SLACK_DEFAULT_CHANNEL:#general}
     notification:
       level: ${SLACK_NOTIFICATION_LEVEL:INFO}  # INFO, WARN, ERROR
     rate-limit:
       min-interval-ms: ${SLACK_RATE_LIMIT_MIN_INTERVAL_MS:1000}
       enabled: ${SLACK_RATE_LIMIT_ENABLED:true}

   resilience4j:
     retry:
       configs:
         default:
           maxAttempts: 3
           waitDuration: 500ms
           exponentialBackoffMultiplier: 2
           retryExceptions:
             - org.springframework.web.reactive.function.client.WebClientException
             - java.net.ConnectException
   ```
   
   **주요 기능**:
   - **SlackWebhookClient**: Incoming Webhook을 통한 메시지 전송
   - **SlackMessageBuilder**: Block Kit 메시지 빌더 패턴
   - **SlackRateLimiter**: Redis 기반 Rate Limiting (초당 1개 메시지 제한)
   - **SlackNotificationService**: 고수준 알림 서비스
     * `sendErrorNotification(String message, Throwable error)`: 에러 알림
     * `sendSuccessNotification(String message)`: 성공 알림
     * `sendInfoNotification(String message)`: 정보 알림
     * `sendBatchJobNotification(SlackDto.BatchJobResult result)`: 배치 작업 알림
   
   **보안 고려사항**:
   - Webhook URL 및 Bot Token은 환경 변수로 관리 (`SLACK_WEBHOOK_URL`, `SLACK_BOT_TOKEN`)
   - Git에 커밋하지 않음 (`.gitignore`에 환경 변수 파일 추가)
   - 로그에 민감 정보 출력 금지
   
   **테스트 코드 작성** (client/feign 모듈과 일관된 형식):
   - **참고 문서**: `docs/step8/slack-integration-design-guide.md` (5.11. 테스트 작성 가이드 섹션 참고)
   - **테스트 컨텍스트**: `SlackTestContext` 클래스 작성 (`@ImportAutoConfiguration`, `@Import` 사용)
   - **테스트 예시**: `SlackClientTest` (Given-When-Then 패턴, `@SpringBootTest`, `@DisplayName` 사용)
   - **테스트 작성 가이드라인**:
     * `client/feign` 모듈과 동일하게 Given-When-Then 패턴 사용
     * `@SpringBootTest` 사용, `classes`에 테스트 컨텍스트와 Config 클래스 지정
     * `@DisplayName`으로 테스트 목적 명시, `@Test`는 주석 처리하여 기본적으로 비활성화
     * `@Autowired`로 테스트 대상 컴포넌트 주입, JUnit 5의 `Assertions` 사용
     * 테스트 시 `slack.webhook.enabled=false` 설정하여 실제 Slack API 호출 방지

5. 공통 기능
   
   **참고 문서**: `docs/step8/rss-scraper-modules-analysis.md` (공통 기능 섹션 참고)
   
   **Rate Limiting 유틸리티** (Redis 활용):
   - 출처별 최소 간격 설정 지원
   - Redis를 활용한 분산 환경에서의 Rate Limiting
   - 캐시 키: `rate-limit:{source-name}`
   - 기본 간격: 최소 1초 이상 (출처별 설정 가능)
   - 구현 패턴: Redis를 통한 마지막 요청 시간 저장 및 간격 확인
   - 모든 출처에 대해 최소 1초 간격 유지, 순차 수집
   
   **Retry 로직** (Resilience4j 활용):
   - Resilience4j를 사용한 재시도 로직 (Thread.sleep() 제거)
   - 최대 3회 재시도, 지수 백오프(exponential backoff) 적용
   - 비동기 지원, Circuit Breaker 패턴 제공
   - 재시도 대상 예외: `WebClientException`, `IOException`, `HttpStatusException` 등
   - 수집 주기 내 재시도는 최대 3회, 다음 수집 주기까지 재시도 안 함
   
   **에러 핸들링**:
   - 네트워크 오류 시 재시도 (최대 3회)
   - 타임아웃 처리 (기본 30초)
   - 실패 시 로깅 및 알림
   - 커스텀 예외 클래스 정의 (예: `RssParsingException`, `ScrapingException`)
   
   **로깅**:
   - `@Slf4j` 사용 (Lombok)
   - 요청/응답 로깅 (디버그 레벨)
   - 에러 로깅 (에러 레벨)
   - Rate Limiting 대기 시간 로깅 (디버그 레벨)
   
   **데이터 수집 전략** (참고 문서: `docs/step8/rss-scraper-modules-analysis.md` 데이터 수집 전략 섹션):
   
   **수집 주기**:
   - **RSS 출처**:
     * Google Developers Blog RSS: 주 1회 (월요일 새벽 2시 권장)
     * TechCrunch RSS: 하루 1회 (새벽 2시 권장)
     * Ars Technica RSS: 하루 1회 (새벽 2시 권장)
     * Medium Technology RSS: 하루 1회 (새벽 2시 권장)
   - **웹 스크래핑 출처**:
     * LeetCode Contests: 주 1회 (월요일 새벽 3시 권장)
     * Google Summer of Code: 연간 (프로그램 기간 중 일일)
     * Devpost: 하루 1회 (새벽 3시 권장)
     * MLH: 주 1회 (월요일 새벽 3시 권장)
     * AtCoder: 주 1회 (월요일 새벽 3시 권장)
   - **Rate Limiting**: 모든 출처에 대해 최소 1초 간격 유지, 순차 수집
   
   **수집 프로세스**:
   1. **스케줄러 트리거** (Spring Scheduler)
      - 설정된 시간에 수집 작업 시작
      - 출처별 순차 처리
   2. **데이터 수집**
      - RSS: `client-rss` 모듈 사용
      - Web Scraping: `client-scraper` 모듈 사용
   3. **데이터 정제**
      - 중복 제거 (URL 기반)
      - 필수 필드 검증
      - 데이터 형식 정규화
   4. **데이터 저장**
      - MongoDB Atlas에 저장 (Query Side)
      - `NewsArticleDocument` 또는 `ContestDocument` 형식
   5. **에러 처리**
      - 실패한 출처는 로깅 및 알림
      - 다음 수집 주기까지 재시도 안 함 (수집 주기 내 재시도는 최대 3회)
   
   **Redis 캐싱 전략**:
   - **목적**: 중복 수집 방지 및 성능 최적화
   - **캐시 키**: `rss:last-collected:{source-name}` 또는 `scraper:last-collected:{source-name}`
   - **캐시 값**: 마지막 수집 시간 (ISO 8601 형식)
   - **TTL**: 7일

**검증 기준**: 
- [ ] 모든 Priority 1 API 출처와의 통합 완료 (client-feign 모듈) 
- [ ] 4개 RSS 출처로부터 데이터 수집 가능 (client-rss 모듈) 
- [ ] 5개 웹 스크래핑 출처로부터 데이터 수집 가능 (client-scraper 모듈, robots.txt, ToS 준수) 
- [ ] Slack 알림 기능 구현 완료 (client-slack 모듈, Contract 패턴, Block Kit 메시지 빌더, Rate Limiting) 
- [ ] Rate Limiting 및 Retry 로직 구현 완료 (공통 기능) 
- [ ] 모든 모듈이 기존 아키텍처와 일관성 유지 (Contract 패턴, Spring Boot 베스트 프랙티스) 
- [ ] Mock/Rest 모드 전환 가능 (client-feign 모듈) 
- [ ] 법적/윤리적 고려사항 준수 (robots.txt, ToS 확인, User-Agent 설정) 
- [ ] 보안 고려사항 준수 (Webhook URL, Bot Token 환경 변수 관리, 로그에 민감 정보 출력 금지) 
```

### 9단계: Contest 및 News API 모듈 구현

**단계 번호**: 9단계
**의존성**: 4단계 (Domain 모듈 구현 완료 필수), 8단계 (Client 모듈 구현 완료 권장)
**다음 단계**: 10단계 (외부 API 통합 및 데이터 수집)

```
plan task: Contest 및 News API 모듈 구현 (CQRS 패턴 기반 MongoDB 조회 API)

**description** (작업 설명):
- **작업 목표**:
  - `api-contest` 및 `api-news` 모듈의 RESTful API 구현
  - CQRS 패턴 기반: MongoDB Atlas를 사용한 읽기 전용 조회 API 제공
  - Batch 모듈을 위한 내부 저장 API 제공 (단건/다건 처리)
  - `docs/step9/contest-news-api-design.md` 설계서를 엄격히 준수

- **배경**:
  - Contest 및 News 데이터는 MongoDB Atlas에 저장되어 조회 전용 API로 제공됨
  - Batch 모듈이 Client 모듈에서 수집한 데이터를 API 모듈로 전달하여 MongoDB에 저장
  - `api-auth`, `api-gateway` 모듈의 구조를 참고하여 일관성 있는 패키지 구조 유지

- **예상 결과**:
  - `api-contest`: Contest 조회 API (목록, 상세, 검색) 및 내부 저장 API 구현 완료
  - `api-news`: News 조회 API (목록, 상세, 검색) 및 내부 저장 API 구현 완료
  - Controller → Facade → Service → Repository 계층 구조 구현
  - 트랜잭션 관리: 단건 처리(@Transactional), 다건 처리(부분 롤백) 구현

**requirements** (기술 요구사항 및 제약조건):
1. **설계서 엄격 준수**: `docs/step9/contest-news-api-design.md`의 모든 설계 사항을 정확히 구현
2. **오버엔지니어링 금지**: 
   - 설계서에 명시되지 않은 기능 추가 금지
   - 현재 요구사항에 맞는 최소한의 구조만 구현 (YAGNI 원칙)
   - 불필요한 추상화, 인터페이스, 레이어 추가 금지
3. **기존 패턴 준수**: 
   - `api-auth`, `api-gateway` 모듈의 패키지 구조 및 계층 구조 참고
   - `common-core` 모듈의 `ApiResponse<T>` 공통 응답 형식 사용
   - `domain-mongodb` 모듈의 Repository 인터페이스 활용
4. **CQRS 패턴 준수**: 
   - 읽기 전용 API는 MongoDB Atlas만 사용 (Aurora DB 미사용)
   - 내부 저장 API는 Batch 모듈 전용 (인증: 내부 API 키)
5. **트랜잭션 관리**:
   - 단건 처리: Service 레이어에서 `@Transactional` 사용, 실패 시 롤백
   - 다건 처리: Facade 레이어에서 단건 처리 Service 메서드 반복 호출, 부분 롤백 구현
   - Facade 레이어에는 `@Transactional` 사용하지 않음

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: `api-contest`, `api-news` 모듈의 RESTful API 구현, CQRS 패턴 기반 MongoDB 조회 API
  - 배경: Contest/News 데이터는 MongoDB Atlas에 저장, Batch 모듈이 데이터 수집 및 저장 담당
  - 예상 결과: 조회 API(목록, 상세, 검색) 및 내부 저장 API(단건/다건) 구현 완료
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - 설계서 엄격 준수 (`docs/step9/contest-news-api-design.md`)
  - 오버엔지니어링 금지 (YAGNI 원칙, 설계서에 없는 기능 추가 금지)
  - 기존 패턴 준수 (`api-auth`, `api-gateway` 참고)
  - CQRS 패턴 준수 (MongoDB Atlas만 사용, Aurora DB 미사용)
  - 트랜잭션 관리 전략 준수 (단건: @Transactional, 다건: 부분 롤백)
- **작업 분해 및 우선순위** (참고 정보):
  - **Phase 1: 패키지 구조 및 기본 클래스 생성** (우선순위: 높음)
    * 의존성: 4단계 (Domain 모듈 완료 필수)
    * 작업: Controller, Facade, Service, Repository 계층 기본 구조 생성
    * 참고: `api-auth` 모듈 구조, `docs/step9/contest-news-api-design.md` 패키지 구조
  - **Phase 2: DTO 클래스 구현** (우선순위: 높음)
    * 의존성: Phase 1 완료
    * 작업: Request/Response DTO 클래스 생성 (설계서의 DTO 구조 준수)
    * 참고: `docs/step9/contest-news-api-design.md` DTO 설계
  - **Phase 3: 조회 API 구현** (우선순위: 높음)
    * 의존성: Phase 1, 2 완료
    * 작업: 목록 조회, 상세 조회, 검색 API 구현
    * 참고: `docs/step9/contest-news-api-design.md` 조회 API 설계
  - **Phase 4: 내부 저장 API 구현** (우선순위: 중간)
    * 의존성: Phase 1, 2 완료
    * 작업: 단건/다건 처리 내부 저장 API 구현 (트랜잭션 관리 포함)
    * 참고: `docs/step9/contest-news-api-design.md` MongoDB 저장 API 설계
  - **Phase 5: 에러 처리 구현** (우선순위: 중간)
    * 의존성: Phase 1-4 완료
    * 작업: 커스텀 예외, 글로벌 예외 핸들러 구현
    * 참고: `docs/step9/contest-news-api-design.md` 에러 처리 설계
- **확인 사항**:
  - 설계서의 모든 요구사항 명확화
  - 기존 모듈(`api-auth`, `api-gateway`)과의 일관성 확인
  - 트랜잭션 관리 전략 이해 (단건/다건 처리 차이)

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **핵심 설정 파일 및 구조 확인**:
  - 루트 디렉토리 구조 및 중요 설정 파일 확인 (build.gradle, settings.gradle, shrimp-rules.md 등)
  - shrimp-rules.md가 존재하면 상세히 읽고 참고
  - 주요 디렉토리 조직 및 모듈 구분 분석
  - `api-contest`, `api-news` 모듈의 `build.gradle` 확인
  - `domain-mongodb` 모듈의 Repository 인터페이스 확인
  - `common-core` 모듈의 `ApiResponse<T>` 공통 응답 형식 확인
- **아키텍처 패턴 식별**:
  - CQRS 패턴: 읽기 전용 API (MongoDB Atlas), 쓰기 API는 내부 전용
  - 계층 구조: Controller → Facade → Service → Repository
  - 트랜잭션 전략: 단건 처리(@Transactional), 다건 처리(부분 롤백)
- **핵심 컴포넌트 분석**:
  - `api-auth` 모듈의 Controller, Facade, Service 구조 분석
  - `domain-mongodb` 모듈의 Repository 인터페이스 확인
  - `common-core` 모듈의 예외 처리 및 공통 응답 형식 확인
- **기존 패턴 문서화**:
  - `api-auth`, `api-gateway` 모듈의 패키지 구조 및 계층 구조 패턴 파악
  - 설계서(`docs/step9/contest-news-api-design.md`)의 설계 원칙 확인

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - `read_file`로 `docs/step9/contest-news-api-design.md` 설계서 전체 읽기
  - `codebase_search`로 `api-auth`, `api-gateway` 모듈의 구현 패턴 확인
  - `read_file`로 `domain-mongodb` 모듈의 Repository 인터페이스 확인
  - `read_file`로 `common-core` 모듈의 공통 응답 형식 및 예외 처리 확인
- **추측 금지**: 설계서에 명시되지 않은 기능은 구현하지 않음
- **정보 수집 대상**:
  - `docs/step9/contest-news-api-design.md` 설계서 전체 내용
  - `api-auth` 모듈의 Controller, Facade, Service 구현 예시
  - `domain-mongodb` 모듈의 ContestRepository, NewsArticleRepository 인터페이스
  - `common-core` 모듈의 ApiResponse, 예외 처리 클래스

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **중요 원칙: "check first, then design"**:
  - **반드시 기존 코드를 확인하기 전에 설계를 생성하지 않음**
  - 먼저 기존 코드를 확인한 후 설계를 진행해야 함
  - 기존 코드 확인 없이 설계를 먼저 생성하는 것은 엄격히 금지됨
- **정확한 검색 전략 사용**:
  - `read_file`로 `api-auth/src/main/java/com/ebson/shrimp/tm/demo/api/auth/` 구조 확인
  - `codebase_search`로 Controller, Facade, Service 구현 패턴 검색
  - `read_file`로 `domain-mongodb` 모듈의 Repository 인터페이스 확인
- **기존 코드 확인** (check first, then design):
  - `api-auth` 모듈의 Controller, Facade, Service 계층 구조 상세 분석
    * 패키지 구조, 클래스 구조, 의존성 주입 방식 확인
  - `domain-mongodb` 모듈의 Repository 인터페이스 확인
    * ContestRepository, NewsArticleRepository 메서드 시그니처 확인
  - `common-core` 모듈의 공통 응답 형식 및 예외 처리 확인
    * ApiResponse<T> 사용 방법, 예외 처리 패턴 확인
- **코드 스타일 및 규칙 분석**:
  - 기존 컴포넌트의 명명 규칙 확인 (camelCase 등)
  - 주석 스타일 및 형식 규칙 확인
  - 에러 처리 패턴 및 로깅 방법 분석
- **패턴 기록 및 준수**:
  - 발견한 코드 패턴 및 조직 구조를 상세히 기록
  - 설계 시 이러한 패턴을 어떻게 확장할지 계획
- **기존 기능과의 중복 판단**:
  - 기존 기능과의 중복 여부 판단
  - "재사용" 또는 "추상화 및 리팩토링" 중 선택 결정
- **설계서와 기존 코드 비교**:
  - 설계서의 패키지 구조와 `api-auth` 모듈 구조 비교
  - 설계서의 계층 구조와 기존 모듈의 계층 구조 일치 여부 확인

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **Backend API Tasks** 관련 추가 고려사항:
  - **API route 구조 및 명명 규칙 확인**:
    * `api-auth`, `api-gateway` 모듈의 `@RequestMapping` 패턴 확인
    * RESTful API 설계 원칙 준수 여부 확인
    * 엔드포인트 경로 구조 및 버전 관리 방식 확인
  - **요청 처리 및 미들웨어 패턴 분석**:
    * Controller → Facade → Service 계층 구조 패턴 확인
    * DTO 변환 및 검증 패턴 확인
    * 요청/응답 처리 흐름 분석
  - **에러 처리 및 응답 형식 표준 확인**:
    * `common-core` 모듈의 `ApiResponse<T>` 사용 패턴 확인
    * 글로벌 예외 핸들러 구현 패턴 확인
    * 에러 코드 체계 및 메시지 형식 확인
  - **인증/인가 구현 방법 이해**:
    * 내부 API 인증 방식 확인 (`X-Internal-Api-Key` 헤더 사용)
    * 공개 API 인증 방식 확인 (필요 시)
    * 권한 검증 패턴 확인
- **Database Operations** 관련 추가 고려사항:
  - **데이터 접근 패턴 및 추상화 계층 분석**:
    * `domain-mongodb` 모듈의 Repository 인터페이스 패턴 확인
    * MongoDB 쿼리 빌딩 및 페이징 처리 방법 확인
  - **트랜잭션 처리 방법 확인**:
    * 단건 처리와 다건 처리의 트랜잭션 전략 차이 확인
    * `@Transactional` 사용 패턴 및 롤백 처리 방법 확인
  - **데이터 검증 방법 이해**:
    * DTO 검증 패턴 (`@Valid`, `@NotNull` 등)
    * 비즈니스 로직 검증 패턴
  - **성능 최적화 기법 확인**:
    * MongoDB 인덱스 활용 방법
    * 페이징 및 정렬 최적화 방법

**6단계: Preliminary Solution Output (초기 설계 솔루션 출력)**
- **초기 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "초기 설계 솔루션" 작성
  - **복잡한 문제의 경우**: `process_thought` 도구를 활용하여 단계별로 사고하고 정리
    * 문제가 복잡하거나 여러 설계 선택지가 있는 경우 `process_thought` 도구를 사용하여 체계적으로 사고
    * 단계별로 가정, 검증, 조정 과정을 거쳐 최적의 설계 솔루션 도출
  - **사실(facts)**과 **추론(inferences)** 명확히 구분:
    * **사실**: 설계서(`docs/step9/contest-news-api-design.md`), 기존 코드(`api-auth`, `api-gateway`), 공식 문서에서 확인한 내용
    * **추론**: 사실을 바탕으로 한 설계 선택 및 근거
  - **모호한 표현 금지**: 최종 결과물 수준으로 구체적으로 작성
    * "~할 수 있다", "~가 좋을 것 같다" 같은 모호한 표현 사용 금지
    * 구체적인 클래스명, 메서드명, 패키지 구조를 명시
  - **기존 아키텍처 패턴과의 일관성 보장**:
    * `api-auth`, `api-gateway` 모듈의 패턴을 어떻게 재사용할지 명시
    * 설계서의 패키지 구조 및 계층 구조를 어떻게 준수할지 명시
  - **기존 컴포넌트 재사용 방법 설명**:
    * `common-core` 모듈의 `ApiResponse<T>` 재사용 방법
    * `domain-mongodb` 모듈의 Repository 인터페이스 재사용 방법
    * 기존 예외 처리 및 에러 코드 체계 재사용 방법
- **초기 설계 솔루션에 포함할 내용**:
  - **설계서 기반 구현 계획**:
    * `docs/step9/contest-news-api-design.md`의 패키지 구조를 정확히 따름
    * 설계서에 명시된 클래스, 메서드, DTO를 정확히 구현
    * 설계서에 없는 기능은 추가하지 않음 (오버엔지니어링 금지)
  - **패키지 구조**: 설계서에 명시된 패키지 구조를 정확히 따름
  - **계층별 설계**:
    * **Controller 계층**: 
      - `@RestController`, `@RequestMapping` 사용
      - Facade 호출, DTO 변환, `ApiResponse<T>` 사용
      - 내부 API는 `@RequestHeader("X-Internal-Api-Key")` 인증
    * **Facade 계층**: 
      - Service 호출, 비즈니스 로직 조합
      - 다건 처리: 단건 처리 Service 메서드 반복 호출, 부분 롤백 구현
      - `@Transactional` 사용하지 않음 (각 단건 처리가 독립적인 트랜잭션)
    * **Service 계층**: 
      - MongoDB Repository 호출, 데이터 검증, 페이징 처리
      - 단건 처리: `@Transactional` 사용, 실패 시 롤백
    * **Repository 계층**: 
      - `domain-mongodb` 모듈의 Repository 인터페이스 사용
  - **트랜잭션 관리 전략**:
    * 단건 처리: Service 레이어의 `saveContest()`, `saveNews()` 메서드에 `@Transactional` 적용
    * 다건 처리: Facade 레이어에서 단건 처리 Service 메서드를 반복 호출, 예외 catch하여 부분 롤백 구현
  - **DTO 설계**: 
    * 설계서의 Request/Response DTO 구조를 정확히 따름
    * 불필요한 필드 추가 금지 (설계서에 명시된 필드만 구현)
  - **에러 처리 설계**: 
    * 설계서의 커스텀 예외 클래스 구현 (ContestNotFoundException, ContestDuplicateException 등)
    * `@ControllerAdvice`를 사용한 글로벌 예외 핸들러 구현
    * `common-core` 모듈의 에러 코드 체계 준수
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구(`read_file`, `codebase_search`, `web_search` 등)를 활용하여 실제 정보를 수집해야 함
  - 추적 가능한 출처가 없는 정보는 사용하지 않음
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```
  - **주의**: `analyze_task` 도구를 호출하지 않으면 작업이 완료되지 않음
  - **중요**: 이 단계에서 실제 코드 구현은 수행하지 않음. 구현은 `execute_task` 단계에서 `implementationGuide`를 따라 수행됨

**검증 기준**:
- [ ] 설계서(`docs/step9/contest-news-api-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] `api-contest`, `api-news` 모듈의 초기 설계 솔루션 완성:
  - 조회 API(목록, 상세, 검색) 설계
  - 내부 저장 API(단건/다건) 설계
  - Controller → Facade → Service → Repository 계층 구조 설계
  - 트랜잭션 관리 전략 설계 (단건: @Transactional, 다건: 부분 롤백)
- [ ] `api-auth`, `api-gateway` 모듈과 일관성 있는 패키지 구조 설계
- [ ] `common-core` 모듈의 `ApiResponse<T>` 공통 응답 형식 재사용 방법 명시
- [ ] `domain-mongodb` 모듈의 Repository 인터페이스 재사용 방법 명시
- [ ] 에러 처리 설계: 커스텀 예외 및 글로벌 예외 핸들러 구조 설계
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음
- [ ] CQRS 패턴 준수: MongoDB Atlas만 사용, Aurora DB 미사용
- [ ] `analyze_task` 도구 호출 완료: summary와 initialConcept를 포함하여 호출
```

### 10단계: 배치 잡 통합 및 내부 API 호출 구현

**단계 번호**: 10단계
**의존성**: 8단계 (Client 모듈 구현 완료 필수), 4단계 (Domain 모듈 구현 완료 필수), 9단계 (Contest 및 News API 모듈 구현 완료 필수)
**다음 단계**: 11단계 (CQRS 패턴 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)

```
plan task: 배치 잡 통합 및 내부 API 호출 구현 (Spring Batch 기반 데이터 수집 파이프라인)

**description** (작업 설명):
- **작업 목표**:
  - `batch-source` 모듈의 배치 잡 통합 구현 (모든 클라이언트 모듈에 대한 JobConfig 추가)
  - `client-feign` 모듈의 내부 API 호출 Feign Client 구현
  - Client 모듈 → Batch 모듈 → API 모듈 → MongoDB Atlas 데이터 흐름 구현
  - `docs/step10/batch-job-integration-design.md` 설계서를 엄격히 준수

- **배경**:
  - 모든 클라이언트 모듈(`client-feign`, `client-rss`, `client-scraper`)의 데이터 수집을 위한 배치 잡 통합 필요
  - 기존 `ContestCodeforcesJobConfig` 패턴을 참고하여 일관된 배치 잡 구조 구현
  - Batch 모듈에서 수집한 데이터를 `api-contest`, `api-news` 모듈의 내부 API로 전달하여 MongoDB에 저장
  - DTO 독립성 원칙 준수: 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)

- **예상 결과**:
  - `client-feign` 모듈: ContestInternalContract, NewsInternalContract 및 Feign Client 구현 완료
  - `batch-source` 모듈: 모든 클라이언트 모듈에 대한 JobConfig 추가 완료
  - PagingItemReader, Item Processor, Item Writer 구현 완료
  - DTO 변환 흐름 구현: Client DTO → batch-source DTO → client-feign DTO → api-contest/api-news DTO

**requirements** (기술 요구사항 및 제약조건):
1. **설계서 엄격 준수**: `docs/step10/batch-job-integration-design.md`의 모든 설계 사항을 정확히 구현
2. **오버엔지니어링 금지**: 
   - 설계서에 명시되지 않은 기능 추가 금지
   - 현재 요구사항에 맞는 최소한의 구조만 구현 (YAGNI 원칙)
   - 불필요한 추상화, 인터페이스, 레이어 추가 금지
3. **기존 패턴 준수**: 
   - `ContestCodeforcesJobConfig` 패턴을 엄격히 준수하여 일관성 있는 배치 잡 구조 구현
   - 기존 `*FeignConfig` 패턴 준수 (ContestInternalFeignConfig, NewsInternalFeignConfig)
   - `OpenFeignConfig`를 `@Import`하여 공통 설정 사용
4. **DTO 독립성 원칙**: 
   - 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)
   - batch-source 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
   - client-feign 모듈의 내부 API DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
5. **JobConfig 클래스 이름 규칙**:
   - client-feign: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`)
   - client-rss: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
   - client-scraper: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
6. **외부 정보 제공자 공식 문서 참고 필수**:
   - 각 Processor는 반드시 외부 정보 제공자의 공식 문서를 참고하여 필드 매핑 수행
   - `json/sources.json`의 `documentation_url` 필드를 참고하여 공식 문서 URL 확인
   - 공식 문서만 참고: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음
7. **공식 문서만 참고**: 
   - Spring Boot, Spring Batch, Spring Cloud OpenFeign 공식 문서만 참고
   - 외부 정보 제공자 공식 문서만 참고 (sources.json의 documentation_url 참고)

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: 배치 잡 통합 및 내부 API 호출 구현, Spring Batch 기반 데이터 수집 파이프라인 구축
  - 배경: 모든 클라이언트 모듈의 데이터 수집을 위한 배치 잡 통합, 기존 ContestCodeforcesJobConfig 패턴 준수
  - 예상 결과: Feign Client 내부 API 호출, 모든 클라이언트 모듈에 대한 JobConfig 추가, DTO 변환 흐름 구현
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - 설계서 엄격 준수 (`docs/step10/batch-job-integration-design.md`)
  - 오버엔지니어링 금지 (YAGNI 원칙, 설계서에 없는 기능 추가 금지)
  - 기존 패턴 준수 (ContestCodeforcesJobConfig, *FeignConfig 패턴)
  - DTO 독립성 원칙 준수 (모듈 간 DTO 공유 금지)
  - JobConfig 클래스 이름 규칙 준수 (*ApiJobConfig, *RssParserJobConfig, *ScraperJobConfig)
  - 외부 정보 제공자 공식 문서 참고 필수
- **작업 분해 및 우선순위** (참고 정보):
  - **Phase 1: Feign Client 내부 API 호출 구현** (우선순위: 높음)
    * 의존성: 9단계 (api-contest, api-news 모듈 완료 필수)
    * 작업: ContestInternalContract, NewsInternalContract 인터페이스 구현
    * 작업: ContestInternalFeignClient, NewsInternalFeignClient 구현
    * 작업: ContestInternalApi, NewsInternalApi 구현
    * 작업: ContestInternalFeignConfig, NewsInternalFeignConfig 구현 (기존 *FeignConfig 패턴 준수)
    * 작업: InternalApiDto 구현
    * 참고: `docs/step10/batch-job-integration-design.md` Feign Client 설계, 기존 `*FeignConfig` 패턴
  - **Phase 2: batch-source 모듈 DTO 구현** (우선순위: 높음)
    * 의존성: Phase 1 완료
    * 작업: batch-source 모듈의 ContestCreateRequest, ContestBatchRequest, NewsCreateRequest, NewsBatchRequest 구현
    * 참고: `docs/step10/batch-job-integration-design.md` DTO 독립성 원칙 설계
  - **Phase 3: 배치 잡 통합 구현 (client-feign)** (우선순위: 높음)
    * 의존성: Phase 1, 2 완료
    * 작업: 모든 client-feign 클라이언트에 대한 *ApiJobConfig 추가
    * 작업: *PagingItemReader, *Step1Processor, *Step1Writer 구현
    * 참고: `docs/step10/batch-job-integration-design.md` 배치 잡 통합 설계, ContestCodeforcesJobConfig 패턴
  - **Phase 4: 배치 잡 통합 구현 (client-rss)** (우선순위: 중간)
    * 의존성: Phase 1, 2 완료
    * 작업: 모든 client-rss 클라이언트에 대한 *RssParserJobConfig 추가
    * 작업: *RssItemReader, *Step1Processor, *Step1Writer 구현
    * 참고: `docs/step10/batch-job-integration-design.md` 배치 잡 통합 설계
  - **Phase 5: 배치 잡 통합 구현 (client-scraper)** (우선순위: 중간)
    * 의존성: Phase 1, 2 완료
    * 작업: 모든 client-scraper 클라이언트에 대한 *ScraperJobConfig 추가
    * 작업: *ScrapingItemReader, *Step1Processor, *Step1Writer 구현
    * 참고: `docs/step10/batch-job-integration-design.md` 배치 잡 통합 설계
  - **Phase 6: Constants 클래스 업데이트** (우선순위: 중간)
    * 의존성: Phase 3-5 완료
    * 작업: 모든 Job 이름 상수 추가 (Contest 12개, News 8개)
    * 참고: `docs/step10/batch-job-integration-design.md` Constants 설계
  - **Phase 7: 에러 처리 구현** (우선순위: 낮음)
    * 의존성: Phase 1-6 완료
    * 작업: 배치 잡 실패 처리 전략 구현
    * 작업: 내부 API 호출 실패 처리 전략 구현
    * 참고: `docs/step10/batch-job-integration-design.md` 에러 처리 설계
- **확인 사항**:
  - 설계서의 모든 요구사항 명확화
  - 기존 모듈(`ContestCodeforcesJobConfig`, `*FeignConfig`)과의 일관성 확인
  - DTO 독립성 원칙 이해 (각 모듈에서 독립적으로 DTO 정의)
  - JobConfig 클래스 이름 규칙 이해 (*ApiJobConfig, *RssParserJobConfig, *ScraperJobConfig)
  - 외부 정보 제공자 공식 문서 참고 방법 이해 (sources.json의 documentation_url 활용)

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **핵심 설정 파일 및 구조 확인**:
  - 루트 디렉토리 구조 및 중요 설정 파일 확인 (build.gradle, settings.gradle, shrimp-rules.md 등)
  - shrimp-rules.md가 존재하면 상세히 읽고 참고
  - 주요 디렉토리 조직 및 모듈 구분 분석
  - `batch-source` 모듈의 `build.gradle` 확인
  - `client-feign` 모듈의 `build.gradle` 확인
  - `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 확인
- **아키텍처 패턴 식별**:
  - Spring Batch 패턴: Job → Step → Reader/Processor/Writer
  - DTO 변환 흐름: Client DTO → batch-source DTO → client-feign DTO → api-contest/api-news DTO
  - Feign Client 패턴: Contract → FeignClient → Api → FeignConfig
- **핵심 컴포넌트 분석**:
  - `batch-source` 모듈의 `ContestCodeforcesJobConfig` 구조 분석
  - `batch-source` 모듈의 `CodeforcesApiPagingItemReader`, `CodeforcesStep1Processor`, `CodeforcesStep1Writer` 구조 분석
  - `client-feign` 모듈의 기존 `*FeignConfig` 패턴 분석 (CodeforcesFeignConfig, DevToFeignConfig 등)
  - `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 확인
- **기존 패턴 문서화**:
  - `ContestCodeforcesJobConfig` 패턴 파악 (Job, Step, Reader, Processor, Writer 구조)
  - 기존 `*FeignConfig` 패턴 파악 (`@EnableFeignClients`, `@Import({OpenFeignConfig.class})`, Contract 빈 등록)
  - 설계서(`docs/step10/batch-job-integration-design.md`)의 설계 원칙 확인

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - `read_file`로 `docs/step10/batch-job-integration-design.md` 설계서 전체 읽기
  - `read_file`로 `docs/step9/contest-news-api-design.md` 설계서 확인 (내부 API 엔드포인트)
  - `read_file`로 `docs/step8/rss-scraper-modules-analysis.md` 설계서 확인 (RSS/Scraper 모듈 구조)
  - `codebase_search`로 `ContestCodeforcesJobConfig` 구현 패턴 확인
  - `codebase_search`로 기존 `*FeignConfig` 구현 패턴 확인
  - `read_file`로 `json/sources.json` 확인 (각 출처의 documentation_url)
  - `read_file`로 `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 확인
- **추측 금지**: 설계서에 명시되지 않은 기능은 구현하지 않음
- **정보 수집 대상**:
  - `docs/step10/batch-job-integration-design.md` 설계서 전체 내용
  - `docs/step9/contest-news-api-design.md` 설계서 (내부 API 엔드포인트)
  - `docs/step8/rss-scraper-modules-analysis.md` 설계서 (RSS/Scraper 모듈 구조)
  - `batch-source` 모듈의 `ContestCodeforcesJobConfig` 구현 예시
  - `client-feign` 모듈의 기존 `*FeignConfig` 구현 예시
  - `api-contest`, `api-news` 모듈의 내부 API 엔드포인트
  - `json/sources.json` (각 출처의 documentation_url)

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **중요 원칙: "check first, then design"**:
  - **반드시 기존 코드를 확인하기 전에 설계를 생성하지 않음**
  - 먼저 기존 코드를 확인한 후 설계를 진행해야 함
  - 기존 코드 확인 없이 설계를 먼저 생성하는 것은 엄격히 금지됨
- **정확한 검색 전략 사용**:
  - `read_file`로 `batch-source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/` 구조 확인
  - `read_file`로 `client-feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/config/` 구조 확인
  - `codebase_search`로 JobConfig, PagingItemReader, Processor, Writer 구현 패턴 검색
  - `read_file`로 `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 확인
- **기존 코드 확인** (check first, then design):
  - `batch-source` 모듈의 `ContestCodeforcesJobConfig` 구조 상세 분석
    * Job, Step, Reader, Processor, Writer 구조 확인
    * JobParameter, Incrementer 패턴 확인
    * Constants 사용 패턴 확인
  - `batch-source` 모듈의 `CodeforcesApiPagingItemReader` 구조 분석
    * `AbstractPagingItemReader` 상속 패턴 확인
    * Service를 통한 데이터 수집 패턴 확인
  - `batch-source` 모듈의 `CodeforcesStep1Processor` 구조 분석
    * Client DTO → batch-source DTO 변환 패턴 확인
    * 필드 매핑 로직 확인
  - `batch-source` 모듈의 `CodeforcesStep1Writer` 구조 분석
    * 내부 API 호출 패턴 확인 (현재 미완성 상태)
  - `client-feign` 모듈의 기존 `*FeignConfig` 구조 분석
    * `@EnableFeignClients`, `@Import({OpenFeignConfig.class})` 패턴 확인
    * Contract 빈 등록 패턴 확인
  - `api-contest`, `api-news` 모듈의 내부 API 엔드포인트 확인
    * `POST /api/v1/contest/internal`, `POST /api/v1/contest/internal/batch` 엔드포인트 확인
    * `POST /api/v1/news/internal`, `POST /api/v1/news/internal/batch` 엔드포인트 확인
    * 내부 API 키 인증 방식 확인 (`X-Internal-Api-Key` 헤더)
- **코드 스타일 및 규칙 분석**:
  - 기존 컴포넌트의 명명 규칙 확인 (camelCase 등)
  - 주석 스타일 및 형식 규칙 확인
  - 에러 처리 패턴 및 로깅 방법 분석
- **패턴 기록 및 준수**:
  - 발견한 코드 패턴 및 조직 구조를 상세히 기록
  - 설계 시 이러한 패턴을 어떻게 확장할지 계획
- **기존 기능과의 중복 판단**:
  - 기존 기능과의 중복 여부 판단
  - "재사용" 또는 "추상화 및 리팩토링" 중 선택 결정
- **설계서와 기존 코드 비교**:
  - 설계서의 패키지 구조와 기존 모듈 구조 비교
  - 설계서의 계층 구조와 기존 모듈의 계층 구조 일치 여부 확인

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **Spring Batch Tasks** 관련 추가 고려사항:
  - **JobConfig 구조 및 명명 규칙 확인**:
    * `ContestCodeforcesJobConfig` 패턴 확인 (Job, Step, Reader, Processor, Writer 구조)
    * JobConfig 클래스 이름 규칙 확인 (*ApiJobConfig, *RssParserJobConfig, *ScraperJobConfig)
    * Constants 사용 패턴 확인
  - **Item Reader 패턴 분석**:
    * `AbstractPagingItemReader` 상속 패턴 확인
    * Service를 통한 데이터 수집 패턴 확인
    * 페이징 처리 로직 확인
  - **Item Processor 패턴 분석**:
    * Client DTO → batch-source DTO 변환 패턴 확인
    * 외부 정보 제공자 공식 문서 참고 방법 확인 (sources.json의 documentation_url 활용)
    * 필드 매핑 로직 및 검증 패턴 확인
  - **Item Writer 패턴 분석**:
    * batch-source DTO → client-feign DTO 변환 패턴 확인
    * 내부 API 호출 패턴 확인 (Feign Client 사용)
    * Chunk 단위 배치 요청 처리 패턴 확인
    * 에러 처리 및 재시도 로직 확인
- **Feign Client Tasks** 관련 추가 고려사항:
  - **Feign Client 구조 및 명명 규칙 확인**:
    * 기존 `*FeignConfig` 패턴 확인 (`@EnableFeignClients`, `@Import({OpenFeignConfig.class})`)
    * Contract → FeignClient → Api → FeignConfig 구조 확인
    * OpenFeignConfig 공통 설정 사용 패턴 확인
  - **내부 API 호출 패턴 분석**:
    * `ContestInternalContract`, `NewsInternalContract` 인터페이스 설계
    * `ContestInternalFeignClient`, `NewsInternalFeignClient` 구현
    * `ContestInternalApi`, `NewsInternalApi` 구현
    * 내부 API 키 헤더 설정 방법 확인 (`X-Internal-Api-Key`)
  - **DTO 설계 패턴 확인**:
    * DTO 독립성 원칙 준수 (각 모듈에서 독립적으로 DTO 정의)
    * InternalApiDto 설계 (client-feign 모듈에서 독립적으로 정의)
    * DTO 변환 흐름 확인 (batch-source DTO → client-feign DTO)
- **DTO 변환 Tasks** 관련 추가 고려사항:
  - **DTO 독립성 원칙 이해**:
    * 각 모듈에서 독립적으로 DTO 정의 (모듈 간 DTO 공유 금지)
    * batch-source 모듈의 DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
    * client-feign 모듈의 내부 API DTO는 api-contest/api-news 모듈의 DTO와 별도 정의
  - **DTO 변환 흐름 확인**:
    * Client DTO → batch-source DTO 변환 (Item Processor)
    * batch-source DTO → client-feign DTO 변환 (Item Writer)
    * client-feign DTO → api-contest/api-news DTO 변환 (내부 API 엔드포인트)
  - **필드 매핑 방법 이해**:
    * 외부 정보 제공자 공식 문서 참고 필수 (sources.json의 documentation_url 활용)
    * 필수 필드 매핑 (sourceId, title, startDate, endDate, url 등)
    * 선택 필드 매핑 (metadata, status 등)
    * 날짜/시간 변환 규칙 (ISO 8601 권장)

**6단계: Preliminary Solution Output (초기 설계 솔루션 출력)**
- **초기 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "초기 설계 솔루션" 작성
  - **복잡한 문제의 경우**: `process_thought` 도구를 활용하여 단계별로 사고하고 정리
    * 문제가 복잡하거나 여러 설계 선택지가 있는 경우 `process_thought` 도구를 사용하여 체계적으로 사고
    * 단계별로 가정, 검증, 조정 과정을 거쳐 최적의 설계 솔루션 도출
  - **사실(facts)**과 **추론(inferences)** 명확히 구분:
    * **사실**: 설계서(`docs/step10/batch-job-integration-design.md`), 기존 코드(`ContestCodeforcesJobConfig`, `*FeignConfig`), 공식 문서에서 확인한 내용
    * **추론**: 사실을 바탕으로 한 설계 선택 및 근거
  - **모호한 표현 금지**: 최종 결과물 수준으로 구체적으로 작성
    * "~할 수 있다", "~가 좋을 것 같다" 같은 모호한 표현 사용 금지
    * 구체적인 클래스명, 메서드명, 패키지 구조를 명시
  - **기존 아키텍처 패턴과의 일관성 보장**:
    * `ContestCodeforcesJobConfig` 패턴을 어떻게 확장할지 명시
    * 기존 `*FeignConfig` 패턴을 어떻게 재사용할지 명시
    * 설계서의 패키지 구조 및 계층 구조를 어떻게 준수할지 명시
  - **기존 컴포넌트 재사용 방법 설명**:
    * `ContestCodeforcesJobConfig` 패턴 재사용 방법
    * 기존 `*FeignConfig` 패턴 재사용 방법
    * `AbstractPagingItemReader` 상속 패턴 재사용 방법
- **초기 설계 솔루션에 포함할 내용**:
  - **설계서 기반 구현 계획**:
    * `docs/step10/batch-job-integration-design.md`의 패키지 구조를 정확히 따름
    * 설계서에 명시된 클래스, 메서드, DTO를 정확히 구현
    * 설계서에 없는 기능은 추가하지 않음 (오버엔지니어링 금지)
  - **패키지 구조**: 설계서에 명시된 패키지 구조를 정확히 따름
  - **Feign Client 내부 API 호출 설계**:
    * **Contract 인터페이스**: 
      - `ContestInternalContract`, `NewsInternalContract` 인터페이스 설계
      - 내부 API 엔드포인트 매핑 (`POST /api/v1/contest/internal`, `POST /api/v1/contest/internal/batch` 등)
      - 내부 API 키 헤더 처리 (`@RequestHeader("X-Internal-Api-Key")`)
    * **Feign Client**: 
      - `ContestInternalFeignClient`, `NewsInternalFeignClient` 구현
      - `@FeignClient` 어노테이션 설정 (name, url)
    * **Api 클래스**: 
      - `ContestInternalApi`, `NewsInternalApi` 구현
      - Contract 인터페이스 구현, FeignClient 주입
    * **FeignConfig**: 
      - `ContestInternalFeignConfig`, `NewsInternalFeignConfig` 구현
      - 기존 `*FeignConfig` 패턴 준수 (`@EnableFeignClients`, `@Import({OpenFeignConfig.class})`)
      - Contract 빈 등록
    * **InternalApiDto**: 
      - `client-feign` 모듈에서 독립적으로 정의
      - api-contest/api-news 모듈의 DTO와 필드가 같아도 별도 정의
  - **배치 잡 통합 설계**:
    * **JobConfig 클래스 이름 규칙**:
      - client-feign: `*ApiJobConfig` (예: `ContestCodeforcesApiJobConfig`, `ContestGitHubApiJobConfig`)
      - client-rss: `*RssParserJobConfig` (예: `NewsTechCrunchRssParserJobConfig`)
      - client-scraper: `*ScraperJobConfig` (예: `ContestLeetCodeScraperJobConfig`)
    * **JobConfig 구조**: 
      - Job Bean: Job 이름, Step 연결, Incrementer 설정
      - Step Bean: Chunk 크기, Reader, Processor, Writer 설정
      - Reader Bean: `*PagingItemReader` 또는 `*ItemReader` 구현
      - Processor Bean: Client DTO → batch-source DTO 변환
      - Writer Bean: batch-source DTO → client-feign DTO 변환 후 내부 API 호출
      - JobParameter Bean: Job 파라미터 관리
      - Incrementer Bean: Job 실행 제어
    * **패키지 구조**: 
      - `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/{contest|news}/{source-name}/`
      - jobconfig/, reader/, processor/, writer/, service/, jobparameter/, incrementer/
  - **PagingItemReader 설계**: 
    * Feign API용: `*PagingItemReader` (예: `GitHubApiPagingItemReader`)
    * RSS용: `*RssItemReader` (예: `TechCrunchRssItemReader`)
    * Scraper용: `*ScrapingItemReader` (예: `LeetCodeScrapingItemReader`)
    * `AbstractPagingItemReader` 상속 패턴 준수
  - **Item Processor 설계**: 
    * Client DTO → batch-source DTO 변환 패턴
    * 외부 정보 제공자 공식 문서 참고 필수 (sources.json의 documentation_url 활용)
    * 필드 매핑 가이드 준수 (필수 필드, 선택 필드, 변환 규칙)
  - **Item Writer 설계**: 
    * batch-source DTO → client-feign DTO 변환 패턴
    * 내부 API 호출 패턴 (Feign Client 사용)
    * Chunk 단위 배치 요청 처리
    * 에러 처리 및 재시도 로직
  - **DTO 설계**: 
    * DTO 독립성 원칙 준수: 각 모듈에서 독립적으로 DTO 정의
    * batch-source 모듈의 DTO: `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/{contest|news}/dto/`
    * client-feign 모듈의 내부 API DTO: `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/domain/internal/contract/InternalApiDto.java`
    * api-contest/api-news 모듈의 DTO는 이미 구현됨 (9단계에서 완료)
  - **Constants 설계**: 
    * 모든 Job 이름 상수 정의 (Contest 12개, News 8개)
    * 기존 Constants 클래스와의 일관성 유지
  - **에러 처리 설계**: 
    * 배치 잡 실패 처리 전략 (Item Reader, Processor, Writer 실패 처리)
    * 내부 API 호출 실패 처리 전략 (타임아웃, 재시도, 에러 응답 처리)
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구(`read_file`, `codebase_search`, `web_search` 등)를 활용하여 실제 정보를 수집해야 함
  - 추적 가능한 출처가 없는 정보는 사용하지 않음
  - 외부 정보 제공자 공식 문서만 참고 (sources.json의 documentation_url 활용)
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```
  - **주의**: `analyze_task` 도구를 호출하지 않으면 작업이 완료되지 않음
  - **중요**: 이 단계에서 실제 코드 구현은 수행하지 않음. 구현은 `execute_task` 단계에서 `implementationGuide`를 따라 수행됨

**검증 기준**:
- [ ] 설계서(`docs/step10/batch-job-integration-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] Feign Client 내부 API 호출 설계 완성:
  - ContestInternalContract, NewsInternalContract 인터페이스 설계
  - ContestInternalFeignClient, NewsInternalFeignClient 구현 설계
  - ContestInternalApi, NewsInternalApi 구현 설계
  - ContestInternalFeignConfig, NewsInternalFeignConfig 구현 설계 (기존 *FeignConfig 패턴 준수)
  - InternalApiDto 설계 (DTO 독립성 원칙 준수)
- [ ] 배치 잡 통합 설계 완성:
  - 모든 클라이언트 모듈에 대한 JobConfig 목록 작성
  - JobConfig 클래스 이름 규칙 준수 (*ApiJobConfig, *RssParserJobConfig, *ScraperJobConfig)
  - PagingItemReader 설계 (Feign API, RSS, Scraper용)
  - Item Processor 설계 (외부 정보 제공자 공식 문서 참고 방법 포함)
  - Item Writer 설계 (내부 API 호출 패턴 포함)
- [ ] DTO 독립성 원칙 준수: 각 모듈에서 독립적으로 DTO 정의 설계
- [ ] `ContestCodeforcesJobConfig` 패턴과 일관성 있는 구조 설계
- [ ] 기존 `*FeignConfig` 패턴과 일관성 있는 구조 설계
- [ ] 외부 정보 제공자 공식 문서 참고 방법 설계 (sources.json의 documentation_url 활용)
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음
- [ ] 공식 문서만 참고: Spring Boot, Spring Batch, Spring Cloud OpenFeign, 외부 정보 제공자 공식 문서
- [ ] `analyze_task` 도구 호출 완료: summary와 initialConcept를 포함하여 호출

참고 파일: 
- `docs/step10/batch-job-integration-design.md` (배치 잡 통합 설계 문서, 필수)
- `docs/step9/contest-news-api-design.md` (Contest/News API 설계, 내부 API 엔드포인트 참고)
- `docs/step8/rss-scraper-modules-analysis.md` (RSS/Scraper 모듈 분석)
- `docs/step2/1. api-endpoint-design.md` (API 엔드포인트 설계)
- `docs/step2/2. data-model-design.md` (데이터 모델 설계)
- `json/sources.json` (각 출처의 documentation_url 참고, 필수)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/jobconfig/ContestCodeforcesJobConfig.java` (JobConfig 패턴 참고)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/reader/CodeforcesApiPagingItemReader.java` (Reader 패턴 참고)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/processor/CodeforcesStep1Processor.java` (Processor 패턴 참고)
- `batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/contest/codeforces/writer/CodeforcesStep1Writer.java` (Writer 패턴 참고)
- `client/feign/src/main/java/com/ebson/shrimp/tm/demo/client/feign/config/CodeforcesFeignConfig.java` (FeignConfig 패턴 참고)
- `api/contest/src/main/java/com/ebson/shrimp/tm/demo/api/contest/controller/ContestController.java` (내부 API 엔드포인트 참고)
- `api/news/src/main/java/com/ebson/shrimp/tm/demo/api/news/controller/NewsController.java` (내부 API 엔드포인트 참고)
```

### 11단계: CQRS 패턴 구현 (Kafka 동기화)

**단계 번호**: 11단계
**의존성**: 4단계 (Domain 모듈 구현 완료 필수), 8단계 (Client 모듈 구현 완료 권장)
**다음 단계**: 12단계 (사용자 아카이브 기능) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (API 컨트롤러 구현)

```
plan task: CQRS 패턴 기반 Kafka 동기화 서비스 구현 - User 및 Archive 엔티티 MongoDB Atlas 동기화

**단계 번호**: 11단계
**의존성**: 2단계 (API 설계 완료 필수), 4단계 (Domain 모듈 구현 완료 필수), 8단계 (Client 모듈 구현 완료 필수), 10단계 (Batch 모듈 구현 완료 필수)
**다음 단계**: 12단계 (사용자 아카이브 기능 구현) 또는 14단계 (API Gateway 서버 구현) 또는 15단계 (REST API 컨트롤러 및 비즈니스 로직 구현)

**역할**: 백엔드 개발자 (Kafka Consumer 및 동기화 서비스 구현)
**책임**: 
- `UserSyncService` 및 `ArchiveSyncService` 동기화 서비스 인터페이스 및 구현 클래스 생성
- `EventConsumer.processEvent` 메서드 구현
- Kafka 이벤트를 통한 Aurora MySQL → MongoDB Atlas 동기화 로직 구현
- `updatedFields` (Map<String, Object>) 처리 로직 구현 (부분 업데이트 지원)

**description** (작업 설명):
- **작업 요약**:
  CQRS 패턴의 Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 간 실시간 동기화를 위한 Kafka 기반 이벤트 동기화 시스템을 구현합니다. 이미 완료된 Kafka Producer (`EventPublisher`) 및 Consumer 기본 구조 (`EventConsumer`, 멱등성 보장 로직 포함)를 기반으로, User 및 Archive 엔티티의 MongoDB Atlas 동기화 서비스를 구현하고 `EventConsumer.processEvent` 메서드를 완성합니다. 설계서(`docs/step11/cqrs-kafka-sync-design.md`)의 "구현 가이드" 섹션을 엄격히 준수하여 구현합니다.
- **작업 목표**:
  - `common-kafka` 모듈의 `EventConsumer.processEvent` 메서드 구현
  - `UserSyncService` 및 `ArchiveSyncService` 동기화 서비스 구현
  - Kafka 이벤트를 통한 Aurora MySQL → MongoDB Atlas 실시간 동기화 완성
  - `docs/step11/cqrs-kafka-sync-design.md` 설계서를 엄격히 준수

- **배경**:
  - CQRS 패턴 적용: Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 분리
  - Kafka Producer (`EventPublisher`) 및 Consumer 기본 구조 (`EventConsumer`)는 이미 구현 완료
  - MongoDB Atlas 연결 설정 및 인덱스 설정 완료
  - 이벤트 모델 (UserCreatedEvent, UserUpdatedEvent, ArchiveCreatedEvent 등) 정의 완료
  - 현재 `EventConsumer.processEvent` 메서드가 빈 구현 상태이며, 동기화 서비스 미구현

- **예상 결과**:
  - `UserSyncService` 인터페이스 및 구현 클래스 완성 (User 이벤트 → UserProfileDocument 동기화)
  - `ArchiveSyncService` 인터페이스 및 구현 클래스 완성 (Archive 이벤트 → ArchiveDocument 동기화)
  - `EventConsumer.processEvent` 메서드 구현 완성 (이벤트 타입별 분기 처리)
  - `updatedFields` (Map<String, Object>) 처리 로직 구현 (부분 업데이트 지원)

**requirements** (기술 요구사항 및 제약조건):
1. **설계서 엄격 준수**: `docs/step11/cqrs-kafka-sync-design.md`의 모든 설계 사항을 정확히 구현
2. **오버엔지니어링 금지**: 
   - 설계서에 명시되지 않은 기능 추가 금지
   - Contest/News 동기화 서비스는 구현하지 않음 (배치 작업에서 직접 MongoDB에 저장되므로 불필요)
   - 복잡한 DLQ 처리, 모니터링 시스템은 기본 재시도 및 로깅으로 충분
3. **현재 구현 상태 반영**: 
   - 이미 완료된 항목은 구현하지 않음:
     * Kafka Producer (`EventPublisher`)
     * Kafka Consumer 기본 구조 (`EventConsumer`, 멱등성 보장 로직 포함)
     * 이벤트 모델 (모든 이벤트 타입 정의 완료: `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeletedEvent`, `UserRestoredEvent`, `ArchiveCreatedEvent`, `ArchiveUpdatedEvent`, `ArchiveDeletedEvent`, `ArchiveRestoredEvent`)
     * MongoDB Document 및 Repository (`UserProfileDocument`, `ArchiveDocument`, `UserProfileRepository`, `ArchiveRepository`)
     * MongoDB 인덱스 설정 (`MongoIndexConfig`)
     * MongoDB Atlas 연결 설정 (`application-mongodb-domain.yml`, `MongoClientConfig`) ← 이미 완료 (설계서 "구현 가이드" 섹션 5번 항목은 참고용)
   - 미구현 항목만 구현: 동기화 서비스 및 `processEvent` 메서드
4. **클린코드 원칙 준수**: 
   - 단일 책임 원칙 (SRP): 각 동기화 서비스는 하나의 엔티티 타입만 담당
   - 의존성 역전 원칙 (DIP): 인터페이스 기반 설계
   - 개방-폐쇄 원칙 (OCP): 새로운 이벤트 타입 추가 시 확장 가능
5. **Upsert 패턴 사용**: 
   - `findByTsid().orElse(new Document())` 패턴으로 생성/수정 통합 처리
   - MongoDB Document의 `createdAt`, `updatedAt` 필드 자동 관리
6. **updatedFields 처리**: 
   - `Map<String, Object>` 타입의 `updatedFields`를 Document 필드에 매핑
   - `switch` 문을 통한 필드별 타입 변환 및 매핑
   - 알 수 없는 필드는 경고 로그만 출력하고 무시

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: `EventConsumer.processEvent` 및 동기화 서비스 구현
  - 배경: Kafka Producer/Consumer 기본 구조는 완료, 동기화 로직만 구현 필요
  - 예상 결과: User 및 Archive 엔티티의 MongoDB Atlas 동기화 완성
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - 설계서 엄격 준수 (`docs/step11/cqrs-kafka-sync-design.md`)
  - 오버엔지니어링 금지 (Contest/News 동기화 서비스 제외)
  - 현재 구현 상태 반영 (이미 완료된 항목 제외)
  - 클린코드 원칙 준수 (SRP, DIP, OCP)
  - Upsert 패턴 및 updatedFields 처리 전략 준수
- **작업 분해 및 우선순위** (참고 정보, splitTasksRaw 단계에서 작업 1, 2, 3으로 분해됨):
  - **작업 1 (Phase 1): 동기화 서비스 인터페이스 생성** (우선순위: 높음)
    * 의존성: 11단계 (설계서 완료 필수)
    * 작업: `UserSyncService`, `ArchiveSyncService` 인터페이스 정의
    * 참고: `docs/step11/cqrs-kafka-sync-design.md` "동기화 서비스 설계" 섹션 (UserSyncService, ArchiveSyncService 인터페이스)
  - **작업 2 (Phase 2): 동기화 서비스 구현 클래스 생성** (우선순위: 높음)
    * 의존성: 작업 1 (Phase 1) 완료
    * 작업: `UserSyncServiceImpl`, `ArchiveSyncServiceImpl` 구현
    * 참고: Upsert 패턴, updatedFields 처리 로직 (설계서 "동기화 서비스 설계" 섹션의 구현 클래스 및 "updatedFields 처리 전략" 섹션)
  - **작업 3 (Phase 3): EventConsumer.processEvent 구현** (우선순위: 높음)
    * 의존성: 작업 1, 2 (Phase 1, 2) 완료
    * 작업: 이벤트 타입별 분기 처리 로직 구현
    * 참고: `switch` 문, Pattern Matching for `instanceof` (설계서 "이벤트 처리 로직 설계" 섹션의 "EventConsumer.processEvent 구현")
- **기존 시스템 통합 요구사항**:
  - Kafka Producer (`EventPublisher`)와의 통합: 이벤트 발행 방식 및 파티션 키 사용 방식 확인
  - MongoDB Repository와의 통합: Repository 인터페이스 메서드 시그니처 및 사용 패턴 확인
  - Redis 멱등성 보장 로직과의 통합: `EventConsumer`의 기존 멱등성 보장 로직 활용 (`isEventProcessed`, `markEventAsProcessed`)
  - Spring Kafka 재시도 메커니즘과의 통합: 설정 파일 기반 재시도 메커니즘 활용
- **확인 사항**:
  - 설계서의 모든 요구사항 명확화
  - 현재 구현 상태 확인 (이미 완료된 항목 제외)
  - Contest/News 동기화 서비스 제외 사유 이해
  - 기존 시스템과의 통합 요구사항 명확화

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **루트 디렉토리 구조 및 설정 파일 확인**:
  - 루트 디렉토리 구조 확인 (`list_dir`로 프로젝트 루트 구조 파악)
  - `build.gradle` 또는 `pom.xml` 확인 (프로젝트 빌드 설정, 모듈 의존성)
  - `settings.gradle` 확인 (Gradle 모듈 구조, `common/kafka`, `domain/mongodb` 모듈 포함 여부)
  - `application*.yml` 파일 확인 (Spring Boot 설정, 특히 Kafka Consumer 설정)
  - `shrimp-rules.md` 파일 확인 (프로젝트 규칙, 존재 시 상세히 읽고 참고)
- **핵심 설정 파일 및 구조 확인**:
  - `common/kafka` 모듈의 구조 확인 (`EventPublisher`, `EventConsumer` 클래스 위치)
  - `domain/mongodb` 모듈의 Repository 인터페이스 확인 (`UserProfileRepository`, `ArchiveRepository`)
  - `common/kafka` 모듈의 이벤트 모델 확인 (`UserCreatedEvent`, `UserUpdatedEvent` 등)
  - `docs/step11/cqrs-kafka-sync-design.md` 설계서 전체 읽기
  - Spring Kafka 재시도 설정 확인 (`codebase_search`로 `application*.yml` 파일에서 `spring.kafka.consumer` 설정 확인)
    * 재시도 횟수, 재시도 간격, Dead Letter Queue 설정 확인
    * 재시도 메커니즘 동작 방식 이해
- **아키텍처 패턴 식별**:
  - CQRS 패턴: Command Side (Aurora MySQL) → Kafka → Query Side (MongoDB Atlas)
  - 이벤트 기반 아키텍처: Kafka 이벤트를 통한 비동기 동기화
  - Upsert 패턴: 생성/수정 통합 처리
- **핵심 컴포넌트 분석**:
  - `EventConsumer` 클래스의 현재 구현 상태 확인 (멱등성 보장 로직 포함)
  - `EventPublisher` 클래스의 이벤트 발행 방식 확인
  - MongoDB Repository 인터페이스의 메서드 시그니처 확인
- **기존 패턴 문서화**:
  - `common/kafka` 모듈의 패키지 구조 파악
  - 설계서의 동기화 서비스 설계 원칙 확인

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - **사용자에게 질문하여 명확화** (설계서에 명시되지 않은 부분, 모호한 요구사항, 기술 선택지 등)
  - `read_file`로 `docs/step11/cqrs-kafka-sync-design.md` 설계서 전체 읽기 (특히 "구현 가이드" 섹션 참고)
  - `codebase_search`로 `EventConsumer`, `EventPublisher` 클래스 확인
  - `read_file`로 `domain/mongodb` 모듈의 Repository 인터페이스 확인
  - `read_file`로 이벤트 모델 클래스 확인 (`UserCreatedEvent`, `UserUpdatedEvent`, `ArchiveCreatedEvent`, `ArchiveUpdatedEvent` 등)
  - `web_search`로 기술 문서 검색 (MongoDB, Spring Kafka 공식 문서 등, 불熟悉한 개념이나 기술의 경우)
  - 이벤트 페이로드 구조 확인:
    * `UserCreatedEvent.payload()`: `userTsid`, `userId`, `username`, `email`, `profileImageUrl` 필드 확인
    * `UserUpdatedEvent.payload().updatedFields()`: `Map<String, Object>` 구조 확인 (필드명: `username`, `email`, `profileImageUrl` 등)
    * `UserDeletedEvent.payload()`: `userTsid`, `userId`, `deletedAt` 필드 확인
    * `UserRestoredEvent.payload()`: `userTsid`, `userId`, `username`, `email`, `profileImageUrl` 필드 확인
    * `ArchiveCreatedEvent.payload()`: `archiveTsid`, `userId`, `itemType`, `itemId`, `itemTitle`, `itemSummary`, `tag`, `memo`, `archivedAt` 필드 확인
    * `ArchiveUpdatedEvent.payload().updatedFields()`: `Map<String, Object>` 구조 확인 (필드명: `tag`, `memo`만 가능, `itemTitle`, `itemSummary`는 ArchiveEntity에 없는 필드이므로 제외)
    * `ArchiveDeletedEvent.payload()`: `archiveTsid`, `userId`, `deletedAt` 필드 확인
    * `ArchiveRestoredEvent.payload()`: `archiveTsid`, `userId`, `itemType`, `itemId`, `itemTitle`, `itemSummary`, `tag`, `memo`, `archivedAt` 필드 확인
- **추측 금지**: 설계서에 명시되지 않은 기능은 구현하지 않음
- **설계서 예제 코드 참고 시 주의사항**:
  * 설계서의 `UserSyncServiceImpl` 예제 코드(978라인)는 `MongoTemplate`을 주입하지만, 실제 구현에서는 Repository 인터페이스만 사용
  * 설계서 예제 코드는 참고용이며, 프롬프트의 지시사항(Repository만 사용)을 우선 준수
  * 설계서의 예제 코드 구조와 로직은 참고하되, 의존성 주입은 Repository 인터페이스만 사용
- **정보 수집 대상**:
  - `docs/step11/cqrs-kafka-sync-design.md` 설계서 전체 내용 (특히 "구현 가이드" 섹션)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java` 현재 구현 상태
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/publisher/EventPublisher.java` 구현 확인
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserCreatedEvent.java` 이벤트 모델 구조
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserUpdatedEvent.java` 이벤트 모델 구조 (updatedFields 확인)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveCreatedEvent.java` 이벤트 모델 구조
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveUpdatedEvent.java` 이벤트 모델 구조 (updatedFields 확인)
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/UserProfileRepository.java` 인터페이스
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/ArchiveRepository.java` 인터페이스
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/UserProfileDocument.java` Document 구조
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/ArchiveDocument.java` Document 구조

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **중요 원칙: "check first, then design"**:
  - **반드시 기존 코드를 확인하기 전에 설계를 생성하지 않음**
  - 먼저 기존 코드를 확인한 후 설계를 진행해야 함
  - 기존 코드 확인 없이 설계를 먼저 생성하는 것은 엄격히 금지됨
- **정확한 검색 전략 사용**:
  - `read_file`로 `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java` 확인
  - `read_file`로 `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/publisher/EventPublisher.java` 확인
  - `read_file`로 `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/` 디렉토리의 Repository 인터페이스 확인
  - `read_file`로 `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/` 디렉토리의 Document 클래스 확인
- **기존 코드 확인** (check first, then design):
  - `EventConsumer` 클래스의 현재 구현 상태 상세 분석
    * 멱등성 보장 로직 (`isEventProcessed`, `markEventAsProcessed`)
    * `processEvent` 메서드의 현재 상태 (빈 구현 확인)
    * 의존성 주입 구조 확인
  - `EventPublisher` 클래스의 이벤트 발행 방식 확인
    * 이벤트 발행 메서드 시그니처 확인
    * 파티션 키 사용 방식 확인
  - MongoDB Repository 인터페이스 메서드 확인
    * `findByUserTsid`, `findByArchiveTsid` 메서드 확인
    * `save` 메서드 확인
    * **삭제 메서드 확인 및 필요 시 선언**:
      - `UserProfileRepository`에 `void deleteByUserTsid(String userTsid)` 메서드가 있는지 확인
      - `ArchiveRepository`에 `void deleteByArchiveTsid(String archiveTsid)` 메서드가 있는지 확인
      - 메서드가 없으면 Repository 인터페이스에 선언 필요 (Spring Data MongoDB 메서드 네이밍 규칙에 따라 자동 구현됨)
  - Document 클래스 구조 확인
    * 필드 타입 및 구조 확인
    * `updatedFields` 매핑 대상 필드 확인
- **코드 스타일 및 규칙 분석**:
  - 기존 컴포넌트의 명명 규칙 확인 (camelCase 등)
  - 주석 스타일 및 형식 규칙 확인
  - 에러 처리 패턴 및 로깅 방법 분석
- **패턴 기록 및 준수**:
  - 발견한 코드 패턴 및 조직 구조를 상세히 기록
  - 설계 시 이러한 패턴을 어떻게 확장할지 계획
- **기존 기능과의 중복 판단**:
  - 기존 기능과의 중복 여부 판단
  - "재사용" 또는 "추상화 및 리팩토링" 중 선택 결정
- **설계서와 기존 코드 비교**:
  - 설계서의 동기화 서비스 설계와 기존 코드 구조 비교
  - 설계서의 Upsert 패턴과 Repository 메서드 일치 여부 확인
  - **설계서 예제 코드 참고 시 주의사항**:
    * 설계서의 `UserSyncServiceImpl` 예제 코드(978라인)는 `MongoTemplate`을 주입하지만, 실제 구현에서는 Repository만 사용
    * 설계서 예제 코드는 참고용이며, 프롬프트의 지시사항(Repository만 사용)을 우선 준수
    * 설계서의 예제 코드 구조는 참고하되, 의존성 주입은 Repository 인터페이스만 사용

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **공식 가이드 일반 가이드라인 참고**:
  - **Backend API Tasks** 가이드라인 참고:
    * API 라우트 구조 및 명명 규칙 확인 (이 작업은 API가 아니지만, 서비스 계층 설계 원칙 참고)
    * 에러 처리 및 응답 형식 표준 확인 (예외 처리 패턴 참고)
  - **Database Operations** 가이드라인 참고:
    * 기존 데이터 접근 패턴 및 추상화 레이어 분석 (Repository 인터페이스 패턴)
    * 쿼리 빌딩 및 트랜잭션 처리 방법 이해 (MongoDB Repository 메서드 사용)
    * 관계 처리 및 데이터 검증 방법 이해 (Document 필드 매핑)
- **Backend Service Tasks (Kafka Consumer 및 동기화 서비스)** 관련 추가 고려사항:
  - **서비스 인터페이스 설계 원칙**:
    * 인터페이스는 `public` 접근 제어자 사용
    * 각 메서드는 `void` 반환 타입 (비동기 처리)
    * 메서드 파라미터는 구체적인 이벤트 타입 사용 (예: `UserCreatedEvent`, `ArchiveUpdatedEvent`)
    * 패키지 위치: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/sync/`
  - **의존성 주입 패턴 분석**:
    * `@Service` 어노테이션 사용
    * `@RequiredArgsConstructor` 사용 (Lombok)
    * Repository 인터페이스 주입 방식 확인 (`UserProfileRepository`, `ArchiveRepository`)
      - **중요**: Repository 인터페이스만 주입 (설계서 예제 코드의 `MongoTemplate`은 사용하지 않음)
      - Repository 인터페이스에 삭제 메서드가 없으면 선언 필요:
        * `UserProfileRepository`에 `void deleteByUserTsid(String userTsid)` 선언
        * `ArchiveRepository`에 `void deleteByArchiveTsid(String archiveTsid)` 선언
        * Spring Data MongoDB 메서드 네이밍 규칙에 따라 자동 구현됨
    * `EventConsumer`에 동기화 서비스 주입 추가 (`UserSyncService`, `ArchiveSyncService`)
  - **에러 처리 및 로깅 패턴**:
    * 동기화 실패 시 예외 전파 전략:
      - `RuntimeException`으로 래핑하여 전파
      - Spring Kafka의 기본 재시도 메커니즘 활용 (설정 파일에서 재시도 횟수 및 간격 설정)
      - 최대 재시도 횟수 초과 시 Dead Letter Queue로 자동 이동 (Spring Kafka 기본 동작)
    * 로깅 레벨 및 형식:
      - 성공 시: `log.debug("Successfully synced ...")`
      - 실패 시: `log.error("Failed to sync ...", exception)`
      - 알 수 없는 필드: `log.warn("Unknown field in updatedFields: {}", fieldName)`
  - **Upsert 패턴 구현**:
    * `findByUserTsid().orElse(new UserProfileDocument())` 패턴 사용 (User 동기화)
    * `findByArchiveTsid().orElse(new ArchiveDocument())` 패턴 사용 (Archive 동기화)
    * `createdAt`, `updatedAt` 필드 자동 관리:
      - 생성 시: `setCreatedAt(LocalDateTime.now())`, `setUpdatedAt(LocalDateTime.now())`
      - 수정 시: `setUpdatedAt(LocalDateTime.now())`만 업데이트
  - **updatedFields 처리 전략**:
    * `Map<String, Object>` 타입의 `updatedFields`를 Document 필드에 매핑
    * `switch` 문을 통한 필드별 매핑:
      - User: `username`, `email`, `profileImageUrl` 등
      - Archive: `tag`, `memo`만 가능 (itemTitle, itemSummary는 ArchiveEntity에 없는 필드이므로 제외)
    * 타입 변환 로직:
      - String → String: 그대로 사용 (대부분의 필드)
      - Instant → LocalDateTime: `LocalDateTime.ofInstant(instant, ZoneId.systemDefault())` 사용 (설계서 예제 코드의 `convertToLocalDateTime` 메서드 참고, `archivedAt` 필드 등)
      - String → LocalDateTime: `updatedFields`에서 값이 `String` 타입인 경우에만 `Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime()` 사용 (ISO-8601 형식)
      - String → ObjectId: `new ObjectId(value)` (Archive의 `itemId` 필드)
      - null 값 처리: Document 필드에 `null` 설정 (nullable 필드인 경우), null이 아닌 경우에만 업데이트 (설계서 "updatedFields 처리 전략" 섹션 참고)
      - 타입 불일치 시: `ClassCastException` 발생 가능, 적절한 예외 처리 필요 (try-catch로 래핑하여 경고 로그 출력)
  - **이벤트 타입별 처리 방법**:
    * **이벤트 타입 상수 값** (설계서 "이벤트 처리 로직 설계" 섹션의 "EventConsumer.processEvent 구현" 참고):
      - User: `"USER_CREATED"`, `"USER_UPDATED"`, `"USER_DELETED"`, `"USER_RESTORED"`
      - Archive: `"ARCHIVE_CREATED"`, `"ARCHIVE_UPDATED"`, `"ARCHIVE_DELETED"`, `"ARCHIVE_RESTORED"`
    * **User 이벤트**:
      - `"USER_CREATED"`: `UserSyncService.syncUserCreated()` 호출 → `UserProfileDocument` 생성 (Upsert 패턴: `findByUserTsid().orElse(new UserProfileDocument())`)
      - `"USER_UPDATED"`: `UserSyncService.syncUserUpdated()` 호출 → `updatedFields`를 Document 필드에 매핑하여 부분 업데이트 (Document가 존재하지 않으면 예외 발생)
      - `"USER_DELETED"`: `UserSyncService.syncUserDeleted()` 호출 → `UserProfileDocument` 물리적 삭제 (MongoDB는 Soft Delete 미지원, `deleteByUserTsid()` 사용)
      - `"USER_RESTORED"`: `UserSyncService.syncUserRestored()` 호출 → `UserProfileDocument` 새로 생성 (MongoDB는 Soft Delete 미지원이므로 복원 시 새로 생성)
    * **Archive 이벤트**:
      - `"ARCHIVE_CREATED"`: `ArchiveSyncService.syncArchiveCreated()` 호출 → `ArchiveDocument` 생성 (Upsert 패턴: `findByArchiveTsid().orElse(new ArchiveDocument())`)
      - `"ARCHIVE_UPDATED"`: `ArchiveSyncService.syncArchiveUpdated()` 호출 → `updatedFields`를 Document 필드에 매핑하여 부분 업데이트 (Document가 존재하지 않으면 예외 발생)
      - `"ARCHIVE_DELETED"`: `ArchiveSyncService.syncArchiveDeleted()` 호출 → `ArchiveDocument` 물리적 삭제 (MongoDB는 Soft Delete 미지원, `deleteByArchiveTsid()` 사용)
      - `"ARCHIVE_RESTORED"`: `ArchiveSyncService.syncArchiveRestored()` 호출 → `ArchiveDocument` 새로 생성 (MongoDB는 Soft Delete 미지원이므로 복원 시 새로 생성)

**6단계: Preliminary Solution Output (초기 설계 솔루션 출력)**
- **초기 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "초기 설계 솔루션" 작성
  - **복잡한 문제의 경우**: `process_thought` 도구를 활용하여 단계별로 사고하고 정리
    * 문제가 복잡하거나 여러 설계 선택지가 있는 경우 `process_thought` 도구를 사용하여 체계적으로 사고
    * 단계별로 가정, 검증, 조정 과정을 거쳐 최적의 설계 솔루션 도출
  - **사실(facts)**과 **추론(inferences)** 명확히 구분:
    * **사실**: 설계서(`docs/step11/cqrs-kafka-sync-design.md`), 기존 코드(`EventConsumer`, `EventPublisher`), 공식 문서에서 확인한 내용
    * **추론**: 사실을 바탕으로 한 설계 선택 및 근거
  - **모호한 표현 금지**: 최종 결과물 수준으로 구체적으로 작성
    * "~할 수 있다", "~가 좋을 것 같다" 같은 모호한 표현 사용 금지
    * 구체적인 클래스명, 메서드명, 패키지 구조를 명시
  - **설계서의 구현 가이드 참조**:
    * `docs/step11/cqrs-kafka-sync-design.md`의 "구현 가이드" 섹션을 반드시 참고
    * 동기화 서비스 인터페이스 생성 방법, 구현 클래스 생성 방법, `EventConsumer.processEvent` 구현 방법 상세 참고
  - **기존 컴포넌트 재사용 및 패턴 준수**:
    * `EventConsumer`의 기존 멱등성 보장 로직 재사용 (`isEventProcessed`, `markEventAsProcessed` 메서드 활용)
    * `EventPublisher`의 이벤트 발행 패턴 참고 (Partition Key 사용 방식, 비동기 처리 방식)
    * MongoDB Repository 인터페이스의 기존 메서드 재사용 (`findByUserTsid`, `findByArchiveTsid`, `save` 메서드)
    * 기존 코드 스타일 및 명명 규칙 준수 (camelCase, Lombok 사용 패턴 등)
    * 기존 에러 처리 및 로깅 패턴 준수
- **설계 솔루션 작성 원칙**:
  - 설계서(`docs/step11/cqrs-kafka-sync-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
  - "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
  - 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- **초기 설계 솔루션 필수 포함 사항**:
  - **동기화 서비스 인터페이스 설계**: 
    * `UserSyncService` 인터페이스 전체 설계:
      - 인터페이스 전체 메서드 시그니처 설계 (파라미터 타입, 반환 타입, 예외 선언)
      - 메서드: `void syncUserCreated(UserCreatedEvent event)`, `void syncUserUpdated(UserUpdatedEvent event)`, `void syncUserDeleted(UserDeletedEvent event)`, `void syncUserRestored(UserRestoredEvent event)`
      - JavaDoc 주석 포함 설계 (각 메서드의 역할 및 파라미터 설명)
    * `ArchiveSyncService` 인터페이스 전체 설계:
      - 인터페이스 전체 메서드 시그니처 설계 (파라미터 타입, 반환 타입, 예외 선언)
      - 메서드: `void syncArchiveCreated(ArchiveCreatedEvent event)`, `void syncArchiveUpdated(ArchiveUpdatedEvent event)`, `void syncArchiveDeleted(ArchiveDeletedEvent event)`, `void syncArchiveRestored(ArchiveRestoredEvent event)`
      - JavaDoc 주석 포함 설계 (각 메서드의 역할 및 파라미터 설명)
  - **동기화 서비스 구현 클래스 설계**: 
    * `UserSyncServiceImpl` 클래스 전체 구조 설계:
      - 클래스 전체 구조 설계 (필드, 생성자, 메서드)
      - 각 메서드의 구현 로직을 pseudocode로 설계
      - `syncUserCreated`: Upsert 패턴 구현 로직 (pseudocode)
      - `syncUserUpdated`: `updatedFields` 처리 로직 (pseudocode)
      - `syncUserDeleted`: 물리적 삭제 로직 (pseudocode)
      - `syncUserRestored`: Document 새로 생성 로직 (pseudocode)
      - `updateDocumentFields`: switch 문 케이스 전체 설계 (`username`, `email`, `profileImageUrl` 등)
    * `ArchiveSyncServiceImpl` 클래스 전체 구조 설계:
      - 클래스 전체 구조 설계 (필드, 생성자, 메서드)
      - 각 메서드의 구현 로직을 pseudocode로 설계
      - `syncArchiveCreated`: Upsert 패턴 구현 로직 (pseudocode)
      - `syncArchiveUpdated`: `updatedFields` 처리 로직 (pseudocode)
      - `syncArchiveDeleted`: 물리적 삭제 로직 (pseudocode)
      - `syncArchiveRestored`: Document 새로 생성 로직 (pseudocode)
      - `updateDocumentFields`: switch 문 케이스 전체 설계 (`tag`, `memo`만 가능, `itemTitle`, `itemSummary`는 ArchiveEntity에 없는 필드이므로 제외)
  - **EventConsumer.processEvent 구현 설계**: 
    * 이벤트 타입별 분기 처리 로직 (`switch` 문) 전체 설계:
      - switch 문 케이스 전체 설계 (`"USER_CREATED"`, `"USER_UPDATED"`, `"USER_DELETED"`, `"USER_RESTORED"`, `"ARCHIVE_CREATED"`, `"ARCHIVE_UPDATED"`, `"ARCHIVE_DELETED"`, `"ARCHIVE_RESTORED"`, `default`)
      - 각 케이스별 처리 로직 설계 (pseudocode)
    * Pattern Matching for `instanceof` 사용 (Java 16+)
    * 예외 처리 전략 (예외 전파하여 재시도 메커니즘 활용): try-catch로 예외를 catch하되, `throw e`로 전파
  - **패키지 구조 설계**: 
    * `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/sync/` 패키지 구조
    * 인터페이스: `UserSyncService.java`, `ArchiveSyncService.java`
    * 구현 클래스: `UserSyncServiceImpl.java`, `ArchiveSyncServiceImpl.java`
    * `EventConsumer` 수정: `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`
  - **에러 처리 설계**: 
    * 동기화 실패 시 예외 전파 전략:
      - 동기화 서비스에서 `RuntimeException`으로 래핑하여 전파
      - `EventConsumer.processEvent`에서 예외를 catch하지 않고 그대로 전파
      - Spring Kafka의 기본 재시도 메커니즘 활용 (설정 파일에서 재시도 횟수 및 간격 설정)
      - 최대 재시도 횟수 초과 시 Dead Letter Queue로 자동 이동
    * 로깅 전략:
      - 성공 시: `log.debug("Successfully synced ...")`
      - 실패 시: `log.error("Failed to sync ...", exception)` (이벤트 ID, 엔티티 ID 포함)
      - 알 수 없는 필드: `log.warn("Unknown field in updatedFields: {}", fieldName)`
      - 알 수 없는 이벤트 타입: `log.warn("Unknown event type: eventType={}, eventId={}", eventType, event.eventId())`
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구(`read_file`, `codebase_search`, `web_search` 등)를 활용하여 실제 정보를 수집해야 함
  - 추적 가능한 출처가 없는 정보는 사용하지 않음
  - 외부 정보 제공자 공식 문서만 참고 (MongoDB, Spring Kafka 공식 문서)
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```
  - **주의**: `analyze_task` 도구를 호출하지 않으면 작업이 완료되지 않음
  - **중요**: 이 단계에서 실제 코드 구현은 수행하지 않음. 구현은 `execute_task` 단계에서 `implementationGuide`를 따라 수행됨

**검증 기준** (참고: 검증 기준은 선택사항이며, 구현 완료 후 참고용으로 활용):
- [ ] 설계서(`docs/step11/cqrs-kafka-sync-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] 동기화 서비스 초기 설계 솔루션 완성:
  - `UserSyncService`, `ArchiveSyncService` 인터페이스 설계
  - `UserSyncServiceImpl`, `ArchiveSyncServiceImpl` 구현 클래스 설계
  - Upsert 패턴 구현 설계
  - `updatedFields` 처리 로직 설계
- [ ] `EventConsumer.processEvent` 메서드 구현 설계 완성:
  - 이벤트 타입별 분기 처리 로직 설계 (`switch` 문, Pattern Matching for `instanceof`)
  - Contest/News 이벤트 처리 전략 (로깅만 수행)
  - 예외 처리 전략 설계 (예외 전파하여 Spring Kafka 재시도 메커니즘 활용)
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음 (Contest/News 동기화 서비스 제외)
- [ ] 클린코드 원칙 준수: SRP, DIP, OCP 원칙 반영
- [ ] **기능 검증** (설계서 "검증 기준" 섹션의 "1. 기능 검증" 참고):
  - [ ] 모든 이벤트 타입에 대한 동기화 동작 확인:
    * User 이벤트: `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeletedEvent`, `UserRestoredEvent` → `UserProfileDocument` 동기화
    * Archive 이벤트: `ArchiveCreatedEvent`, `ArchiveUpdatedEvent`, `ArchiveDeletedEvent`, `ArchiveRestoredEvent` → `ArchiveDocument` 동기화
  - [ ] 멱등성 보장 확인: 동일한 `eventId`로 이벤트 2회 수신 시 두 번째는 스킵 (Redis에서 처리 여부 확인)
  - [ ] 에러 핸들링 동작 확인: MongoDB 연결 실패, 잘못된 페이로드, 알 수 없는 이벤트 타입 처리
- [ ] **통합 테스트 작성** (설계서 "검증 기준" 섹션의 "1. 기능 검증" 참고):
  - [ ] 각 이벤트 타입별 통합 테스트 작성:
    * `UserCreatedEvent` → `UserProfileDocument` 생성 테스트
    * `UserUpdatedEvent` → `UserProfileDocument` 업데이트 테스트 (updatedFields 처리)
    * `UserDeletedEvent` → `UserProfileDocument` 삭제 테스트
    * `UserRestoredEvent` → `UserProfileDocument` 생성 테스트
    * `ArchiveCreatedEvent` → `ArchiveDocument` 생성 테스트
    * `ArchiveUpdatedEvent` → `ArchiveDocument` 업데이트 테스트 (updatedFields 처리)
    * `ArchiveDeletedEvent` → `ArchiveDocument` 삭제 테스트
    * `ArchiveRestoredEvent` → `ArchiveDocument` 생성 테스트
  - [ ] 멱등성 테스트 작성: 동일한 `eventId`로 이벤트 2회 수신 시나리오 테스트
  - [ ] 에러 핸들링 테스트 작성: MongoDB 연결 실패, 잘못된 페이로드, 알 수 없는 이벤트 타입 처리 테스트
- [ ] **성능 검증** (설계서 "검증 기준" 섹션의 "2. 성능 검증" 참고):
  - [ ] 동기화 지연 시간 측정: 평균 및 95 백분위수 < 1초
  - [ ] 동시성 처리 확인: 동일한 Partition Key로 여러 이벤트 동시 수신 시 순서 보장
- [ ] **빌드 검증**: `common/kafka`, `domain/mongodb` 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :common-kafka:build`, `./gradlew :domain-mongodb:build` 명령이 성공해야 함, 모듈 경로는 `common/kafka`, `domain/mongodb`)
- [ ] **빌드 검증**: 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
- [ ] **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
- [ ] **MongoDB Atlas 연결 검증** (설계서 "검증 기준" 섹션의 "4. MongoDB Atlas 연결 검증" 참고, 이미 완료된 항목이므로 구현 시 검증 불필요):
  - [ ] MongoDB Atlas Cluster 연결 성공 (이미 완료, `application-mongodb-domain.yml`, `MongoClientConfig` 구현 완료)
  - [ ] 연결 풀 정상 동작 (연결 수 모니터링) (이미 완료)
  - [ ] Read Preference 적용 확인 (`secondaryPreferred`) (이미 완료)
  - [ ] SSL/TLS 연결 확인 (프로덕션 환경) (이미 완료)

**참고 파일**:
- `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계서 (필수 참고)
- `docs/reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
- `docs/step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `docs/step1/3. aurora-schema-design.md`: Aurora 스키마 설계
- `docs/step2/2. data-model-design.md`: 데이터 모델 설계 (이벤트 모델 참고)

**작업 분해 참고 정보** (splitTasksRaw 단계에서만 활용):

**중요 참고**: 아래 정보는 **`splitTasksRaw` 단계에서만** 작업을 분해할 때 참고용입니다. **plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행**하세요. 이 섹션의 내용은 plan task 단계에서 직접 사용하지 않습니다.

**모듈 구조**:
- `common/kafka`: Kafka Producer/Consumer 및 동기화 서비스 구현 (모듈 경로: `common/kafka`)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/sync/`: 동기화 서비스 패키지
    * `UserSyncService.java`: User 동기화 서비스 인터페이스 (생성 필요)
    * `UserSyncServiceImpl.java`: User 동기화 서비스 구현 클래스 (생성 필요)
    * `ArchiveSyncService.java`: Archive 동기화 서비스 인터페이스 (생성 필요)
    * `ArchiveSyncServiceImpl.java`: Archive 동기화 서비스 구현 클래스 (생성 필요)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`: `processEvent` 메서드 구현 (수정 필요)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/publisher/EventPublisher.java`: 이미 구현 완료 (참고용)
  - `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/`: 이벤트 모델 (이미 정의 완료, 참고용)
    * `UserCreatedEvent.java`, `UserUpdatedEvent.java`, `UserDeletedEvent.java`, `UserRestoredEvent.java`
    * `ArchiveCreatedEvent.java`, `ArchiveUpdatedEvent.java`, `ArchiveDeletedEvent.java`, `ArchiveRestoredEvent.java`
- `domain/mongodb`: MongoDB Repository 및 Document (이미 구현 완료, 참고용, 모듈 경로: `domain/mongodb`)
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/UserProfileRepository.java`: Repository 인터페이스
    * **주의**: `deleteByUserTsid(String userTsid)` 메서드가 없으면 선언 필요 (Spring Data MongoDB 메서드 네이밍 규칙에 따라 자동 구현됨)
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/ArchiveRepository.java`: Repository 인터페이스
    * **주의**: `deleteByArchiveTsid(String archiveTsid)` 메서드가 없으면 선언 필요 (Spring Data MongoDB 메서드 네이밍 규칙에 따라 자동 구현됨)
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/UserProfileDocument.java`: Document 클래스
  - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/ArchiveDocument.java`: Document 클래스

**작업 의존성 관계**:
- **작업 1 (동기화 서비스 인터페이스 생성)**:
  * 입력: 설계서의 인터페이스 설계 (`docs/step11/cqrs-kafka-sync-design.md` "동기화 서비스 설계" 섹션의 UserSyncService 인터페이스, ArchiveSyncService 인터페이스)
  * 출력: `UserSyncService.java`, `ArchiveSyncService.java` 인터페이스 파일
  * 의존성: 없음 (최우선 작업)
  * 다음 작업: 작업 2, 3 전에 완료 필요
- **작업 2 (동기화 서비스 구현 클래스 생성)**:
  * 입력: 작업 1의 인터페이스, 설계서의 구현 클래스 설계 (`docs/step11/cqrs-kafka-sync-design.md` "동기화 서비스 설계" 섹션의 UserSyncServiceImpl, ArchiveSyncServiceImpl 구현 클래스 및 "updatedFields 처리 전략" 섹션의 `updateDocumentFields` 메서드), Repository 인터페이스
  * 출력: `UserSyncServiceImpl.java`, `ArchiveSyncServiceImpl.java` 구현 클래스 파일
  * 의존성: 작업 1 완료 필요
  * 다음 작업: 작업 3 전에 완료 필요
  * **주의사항**:
    * Repository 인터페이스에 삭제 메서드(`deleteByUserTsid`, `deleteByArchiveTsid`)가 없으면 선언 필요
    * 설계서 예제 코드의 `MongoTemplate` 의존성은 사용하지 않음 (Repository 인터페이스만 사용)
- **작업 3 (`EventConsumer.processEvent` 구현)**:
  * 입력: 작업 1, 2의 동기화 서비스, 설계서의 `processEvent` 구현 설계 (`docs/step11/cqrs-kafka-sync-design.md` "이벤트 처리 로직 설계" 섹션의 "EventConsumer.processEvent 구현"), 이벤트 모델
  * 출력: `EventConsumer.java`의 `processEvent` 메서드 구현
  * 의존성: 작업 1, 2 완료 필요
  * 다음 작업: 없음 (최종 작업)

**의존성 그래프**:
```
작업 1 (인터페이스 생성)
  ↓
작업 2 (구현 클래스 생성)
  ↓
작업 3 (processEvent 구현)
```

**의존성 근거**:
- 작업 1 → 작업 2: 구현 클래스는 인터페이스를 구현하므로 인터페이스가 먼저 정의되어야 함
- 작업 2 → 작업 3: `EventConsumer.processEvent`에서 동기화 서비스를 호출하므로 구현 클래스가 먼저 완성되어야 함
- 작업 1, 2 → 작업 3: `EventConsumer`는 동기화 서비스 인터페이스와 구현 클래스 모두 필요
```

### 12단계: 사용자 아카이브 기능 구현

**단계 번호**: 12단계
**의존성**: 5단계 (사용자 인증 시스템 구현 완료 필수), 4단계 (Domain 모듈 구현 완료 필수), 11단계 (CQRS 패턴 구현 완료 필수), 9단계 (Contest 및 News API 모듈 구현 완료 필수)
**다음 단계**: 13단계 (RAG 기반 챗봇 구현)

```
plan task: 사용자 아카이브 기능 구현 (CQRS 패턴 적용)

**description** (작업 설명):
`api-archive` 모듈의 사용자 아카이브 기능을 구현합니다. 로그인한 사용자가 조회할 수 있는 모든 contest, news 정보를 개인 아카이브에 저장하고, 태그와 메모를 수정하며, 삭제 및 복구할 수 있는 기능을 제공합니다. 또한 태그와 메모를 기준으로 검색하고, 원본 아이템 정보를 기준으로 정렬할 수 있는 기능을 포함합니다. 설계서(`docs/step13/user-archive-feature-design.md`)의 모든 설계 사항을 엄격히 준수하여 구현합니다.

**작업 목표**:
- `api-archive` 모듈의 11개 API 엔드포인트 구현
- ArchiveCommandService, ArchiveQueryService, ArchiveHistoryService 구현
- ArchiveFacade 및 ArchiveController 구현
- DTO 및 예외 처리 구현
- Domain 모듈 확장 (ArchiveDocument 스키마 확장)

**배경**:
- CQRS 패턴: Command Side (Aurora MySQL), Query Side (MongoDB Atlas)
- Kafka 이벤트 동기화: `EventPublisher.publish("archive-events", event, archiveTsid)` 사용, `ArchiveSyncService` 이미 구현 완료
- 기존 API 모듈 패턴 준수: `api-contest`, `api-news`, `api-auth` 참고

**requirements** (기술 요구사항 및 제약조건):
1. **설계서 엄격 준수**: `docs/step13/user-archive-feature-design.md`의 모든 설계 사항 정확히 구현
2. **프로젝트 구조 일관성**: `api-contest`, `api-news`, `api-auth` 모듈 패턴 준수, Facade 패턴 유지
3. **CQRS 패턴 적용**: Command Side (Aurora MySQL), Query Side (MongoDB Atlas), 삭제된 아카이브/히스토리 조회는 예외로 Aurora MySQL 사용
4. **Kafka 이벤트 통합**: `EventPublisher.publish("archive-events", event, archiveTsid)` 사용, `ArchiveSyncService` 이미 구현 완료 (참고: `docs/step11/cqrs-kafka-sync-design.md`)
5. **Soft Delete**: Aurora MySQL에서 `is_deleted = true`, MongoDB는 물리적 삭제
6. **권한 관리**: JWT 토큰에서 `userId` 추출, 사용자별 데이터 격리, 히스토리 조회/복구 권한 검증
7. **히스토리 자동 저장**: `@EntityListeners(HistoryEntityListener.class)` 활용
8. **원본 아이템 정보**: 저장/복구 시 조회하여 이벤트에 포함, 수정 시 `updatedFields`에는 `tag`/`memo`만 포함 (`itemTitle`/`itemSummary`는 `ArchiveEntity`에 없음)
9. **ArchiveDocument 스키마 확장**: `itemStartDate`, `itemEndDate`, `itemPublishedAt` 필드 추가
10. **클린코드 원칙**: SRP, DIP, OCP 준수
11. **오버엔지니어링 금지**: 설계서에 명시된 기능만 구현

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수):

**1단계: Analysis Purpose**
- Task Description 및 Requirements 이해
- 작업 분해 및 우선순위: Domain 모듈 확장 → Service 레이어 → Facade/Controller → DTO/예외 처리
- 기존 시스템 통합 요구사항: Kafka, MongoDB, 기존 API 모듈 패턴 확인
- 설계서(`docs/step13/user-archive-feature-design.md`) 요구사항 명확화

**2단계: Identify Project Architecture**
- 루트 디렉토리 구조 및 설정 파일 확인
- `api-archive` 모듈 구조, `api-contest`/`api-news`/`api-auth` 모듈 참고
- 아키텍처 패턴 식별: CQRS, Facade 패턴, 이벤트 기반 아키텍처
- 핵심 컴포넌트 분석: 기존 API 모듈, `HistoryEntityListener`, `EventPublisher`

**3단계: Collect Information**
- 불확실한 부분: 사용자 질문, `read_file`, `codebase_search`, `web_search` 활용
- 정보 수집 대상: 설계서, 기존 API 모듈, 엔티티/Document 구조, Repository, `EventPublisher`, `ArchiveSyncService` (이미 구현 완료), `docs/step11/cqrs-kafka-sync-design.md`
- 추측 금지

**4단계: Check Existing Programs and Structures**
- 원칙: "check first, then design"
- 검색 전략: `read_file`, `codebase_search` 활용
- 기존 코드 확인: Controller/Facade/Service 구조, 엔티티/Document, Repository, `EventPublisher.publish(topic, event, partitionKey)`, `ArchiveSyncService` 인터페이스
- 패턴 기록 및 준수

**5단계: Task Type-Specific Guidelines**
- Backend API Tasks: API 라우트 구조, 에러 처리, 응답 형식
- Database Operations: Repository 패턴, 트랜잭션 처리
- 사용자 아카이브 기능 특화:
  * 11개 API 엔드포인트: Base URL `/api/v1/archive`
  * 이벤트 발행: `EventPublisher.publish("archive-events", event, archiveTsid)` (Partition Key: `archiveTsid`)
  * `updatedFields`: `tag`/`memo`만 포함 (`itemTitle`/`itemSummary` 제외)
  * MongoDB Soft Delete 미지원 (물리적 삭제)

**6단계: Preliminary Solution Output**
- 초기 설계 솔루션 작성 (복잡한 경우 `process_thought` 활용)
- 사실(facts)과 추론(inferences) 명확히 구분
- 설계서 구현 가이드 참조
- 필수 포함 사항: 프로젝트 구조, API 엔드포인트, Service 레이어, Facade/Controller, DTO, 예외 처리, Domain 모듈 확장, Kafka 이벤트 통합
- 중요 경고: 가정/추측/상상 금지, 실제 정보 수집 필수
- 도구 호출 필수: `analyze_task` 호출

**검증 기준** (참고용):
- 설계서 요구사항 분석 및 초기 설계 솔루션 반영
- "check first, then design" 원칙 준수
- 오버엔지니어링 방지, 클린코드 원칙 준수
- 기능/성능/데이터 일관성/권한/빌드 검증 (설계서 "검증 기준" 섹션 참고)

**참고 파일**:
- `docs/step13/user-archive-feature-design.md`: 사용자 아카이브 기능 구현 설계서 (필수)
- `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계 (핵심 참고)
- `docs/step1/2. mongodb-schema-design.md`, `docs/step1/3. aurora-schema-design.md`: 데이터베이스 스키마 설계
- `docs/step2/1. api-endpoint-design.md`, `docs/step2/2. data-model-design.md`, `docs/step2/3. api-response-format-design.md`, `docs/step2/4. error-handling-strategy-design.md`: API 설계
- `docs/step9/contest-news-api-design.md`: Contest/News API 설계 (기존 API 모듈 구조 참고)

**작업 분해 참고 정보** (splitTasksRaw 단계에서만 활용):

**중요**: 아래 정보는 `splitTasksRaw` 단계에서만 참고용입니다. plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행하세요.

**모듈 구조**:
- `api-archive`: Controller, Facade, Service (Command/Query/History), DTO, Config, Exception
- `domain-aurora`: `ArchiveEntity`, `ArchiveHistoryEntity`, Repository (이미 구현 완료)
- `domain-mongodb`: `ArchiveDocument` (확장 필요), `ArchiveRepository` (확장 필요), `ContestRepository`, `NewsArticleRepository`
- `common-kafka`: `EventPublisher`, `EventConsumer`, `ArchiveSyncService` (이미 구현 완료)

**작업 의존성**: Domain 모듈 확장 → Service 레이어 → Facade/Controller → DTO/예외 처리
```

### 13단계: langchain4j를 활용한 RAG 기반 챗봇 구현

**단계 번호**: 13단계
**의존성**: 11단계 (CQRS 패턴 구현 완료 필수), 9단계 (Contest 및 News API 모듈 구현 완료 필수), 12단계 (사용자 아카이브 기능 구현 완료 권장)
**다음 단계**: 14단계 (API Gateway 서버 구현)

```
plan task: langchain4j를 활용한 RAG 기반 챗봇 구축 최적화 전략 구현

**description** (작업 설명):
`api-chatbot` 모듈의 RAG(Retrieval-Augmented Generation) 기반 챗봇 시스템을 구현합니다. langchain4j 오픈소스를 활용하여 MongoDB Atlas Vector Search 기반의 지식 검색 챗봇을 구축하며, ContestDocument, NewsArticleDocument, ArchiveDocument를 임베딩하여 벡터 검색 기반의 자연어 질의응답 시스템을 제공합니다. 설계서는 OpenAI GPT-4o-mini를 기본 LLM Provider로 선택하고, 동일 Provider인 OpenAI text-embedding-3-small을 Embedding Model로 사용하여, 비용 최적화($0.02 per 1M tokens), 빠른 응답 속도, 그리고 LLM과 Embedding Model 간의 완벽한 통합성을 제공합니다. 멀티턴 대화 히스토리 관리, Provider별 메시지 포맷 변환(OpenAI 기본, Anthropic 대안), JWT 토큰 기반 인증 통합, 토큰 제어, 비용 통제 전략을 포함합니다. 설계서(`docs/step12/rag-chatbot-design.md`)의 모든 설계 사항을 엄격히 준수하여 구현합니다.

**작업 목표** (구현할 구체적 기능):
- `api-chatbot` 모듈 구현 (langchain4j 통합, MongoDB Atlas Vector Search 설정)
- 설정 파일 구현 (`application-chatbot-api.yml`):
  - langchain4j 설정 (`langchain4j.open-ai.chat-model.*`, `langchain4j.open-ai.embedding-model.*`)
  - RAG 설정 (`chatbot.rag.*`: max-search-results, min-similarity-score, max-context-tokens)
  - 입력 전처리 설정 (`chatbot.input.*`: max-length, min-length)
  - 토큰 제어 설정 (`chatbot.token.*`: max-input-tokens, max-output-tokens, warning-threshold)
  - 캐싱 설정 (`chatbot.cache.*`: enabled, ttl-hours, max-size)
  - 세션 생명주기 설정 (`chatbot.session.*`: inactive-threshold-minutes, expiration-days, batch-enabled)
  - ChatMemory 설정 (`chatbot.chat-memory.*`: max-tokens, strategy)
- LLM Provider 및 Embedding Model 설정: OpenAI GPT-4o-mini (기본 LLM), OpenAI text-embedding-3-small (기본 Embedding, 1536 dimensions)
- TokenCountEstimator Bean 설정 (TokenWindowChatMemory용)
- Redis 캐싱 패턴 구현 (Duration 객체 사용, RedisTemplate<String, Object> Bean 설정, ttl-hours 설정)
- MongoDB Atlas Vector Search 설정:
  - 벡터 필드 추가 (ContestDocument, NewsArticleDocument, ArchiveDocument)
  - Vector Index 생성 (MongoDB Atlas 콘솔 작업, 1536 dimensions)
  - 임베딩 생성 배치 작업 (기존 도큐먼트 임베딩 생성, 신규 도큐먼트 자동 임베딩 생성)
- 멀티턴 대화 히스토리 관리 (세션 관리, 메시지 저장, ChatMemory 통합)
  - Memory vs History 구분: History(전체 대화 기록 저장), Memory(LLM 컨텍스트만, TokenWindowChatMemory 관리)
  - TokenWindowChatMemory 기본 전략 사용 (토큰 수 기준 메시지 유지, max-tokens: 2000)
  - 히스토리 조회 전략: Query Side(MongoDB Atlas) 우선, Command Side는 Fallback, 페이징 지원
  - 세션 생명주기 관리 (비활성화, 만료 처리, 자동 재활성화)
  - ConversationSessionLifecycleScheduler 구현 (배치 작업)
- Provider별 메시지 포맷 변환 (OpenAI 기본: system 역할, Anthropic 대안: system 파라미터 분리)
- JWT 토큰 기반 인증 통합 (Authentication 파라미터로 userId 추출, 세션 소유권 검증)
- RAG 파이프라인 구현 (입력 전처리, 벡터 검색, 프롬프트 체인, 답변 생성)
- 토큰 제어 및 비용 통제 전략 구현 (OpenAI GPT-4o-mini 기준: 128K 컨텍스트)
- CQRS 패턴 적용 (Command Side: Aurora MySQL, Query Side: MongoDB Atlas, Kafka 동기화)
- Chatbot API 엔드포인트 구현 (챗봇 대화, 세션 관리, 히스토리 조회)

**배경** (기술 선택 이유 및 아키텍처 배경):
- langchain4j 오픈소스 활용: LLM 통합 프레임워크
- MongoDB Atlas Vector Search: 벡터 데이터베이스로 RAG 최적화
- LLM Provider 선택: OpenAI GPT-4o-mini (기본, 비용 최적화: $0.15/$0.60 per 1M tokens, 128K 컨텍스트)
- Embedding Model 선택: OpenAI text-embedding-3-small (기본, LLM Provider와 동일, $0.02 per 1M tokens, 1536 dimensions)
- CQRS 패턴: Command Side (Aurora MySQL)와 Query Side (MongoDB Atlas) 분리, Kafka 이벤트 동기화
- 멀티턴 대화: 세션 관리 및 히스토리 저장을 통한 컨텍스트 유지
- Provider 지원: OpenAI (기본), Anthropic (대안) API 지원 (메시지 포맷 차이 고려)
- JWT 인증: Stateless 인증 방식, Authentication 파라미터로 userId 추출, 세션 소유권 검증 필수
- Redis 캐싱: 프로젝트 표준 패턴 준수 (Duration 객체 사용, JSON 직렬화)
- 세션 생명주기: 비활성화 임계값(30분), 만료 기간(90일), 배치 작업으로 자동 관리

**requirements** (기술 요구사항 및 제약조건):

**A. 설계서 준수 및 프로젝트 구조**
1. **설계서 엄격 준수**: `docs/step12/rag-chatbot-design.md`의 모든 설계 사항 정확히 구현
   - 구체적 섹션 참조: "langchain4j 통합 설계", "MongoDB Atlas Vector Search 설계", "멀티턴 대화 히스토리 관리 설계", "비용 통제 전략 설계", "세션 생명주기 관리", "Redis 캐싱 전략 설계"
2. **프로젝트 구조 일관성**: `api-contest`, `api-news`, `api-auth` 모듈 패턴 준수, Facade 패턴 유지
3. **BaseEntity 필드명 준수**: isDeleted (Boolean), deletedBy, updatedBy 필드 사용 (deleteYn 사용 금지)

**B. 기술 스택 및 프레임워크**
4. **langchain4j 오픈소스만 사용**: spring-ai 등 다른 LLM 통합 프레임워크 사용 금지
5. **설정 파일 구조**: `application-chatbot-api.yml`에 다음 설정 포함
   - `langchain4j.open-ai.chat-model.*`: API Key, Model Name (gpt-4o-mini), Temperature, Max Tokens, Timeout
   - `langchain4j.open-ai.embedding-model.*`: API Key, Model Name (text-embedding-3-small), Dimensions (1536), Timeout
   - `chatbot.rag.*`: max-search-results (5), min-similarity-score (0.7), max-context-tokens (3000)
   - `chatbot.input.*`: max-length (500), min-length (1)
   - `chatbot.token.*`: max-input-tokens (4000), max-output-tokens (2000), warning-threshold (0.8)
   - `chatbot.cache.*`: enabled (true), ttl-hours (1), max-size (1000)
   - `chatbot.session.*`: inactive-threshold-minutes (30), expiration-days (90), batch-enabled (true)
   - `chatbot.chat-memory.*`: max-tokens (2000), strategy (token-window)
6. **MongoDB Atlas Vector Search**: 
   - 벡터 필드 추가 (ContestDocument, NewsArticleDocument, ArchiveDocument, 1536 dimensions)
   - Vector Index 생성 (MongoDB Atlas 콘솔에서 Vector Search Index 생성)
   - 검색 쿼리 구현
   - 임베딩 생성 배치 작업: 기존 도큐먼트 임베딩 생성, 신규 도큐먼트 저장 시 자동 임베딩 생성

**C. 멀티턴 대화 히스토리 관리**
6. **Memory vs History 구분**: 
   - History (전체 대화 기록): 모든 메시지를 데이터베이스에 저장 (감사, 분석, 복구 목적)
     - 저장 위치: `ConversationMessage` 테이블 (Aurora MySQL) 및 `ConversationMessageDocument` 컬렉션 (MongoDB Atlas)
   - Memory (LLM 컨텍스트): LLM에 전달되는 메시지 집합 (토큰 제한 고려)
     - 관리: langchain4j의 `ChatMemory` 인터페이스
     - 관계: History에서 Memory로 메시지 선택 (최근 메시지 우선, 토큰 제한 고려, SystemMessage는 항상 포함)
7. **ChatMemory 전략**: TokenWindowChatMemory를 기본 전략으로 사용 (토큰 수 기준 메시지 유지, max-tokens: 2000)
8. **TokenCountEstimator Bean**: TokenWindowChatMemory용 별도 TokenCountEstimator Bean 설정 필수
9. **ConversationSession, ConversationMessage**: 엔티티/Document 구현, ChatMemory 통합
10. **히스토리 조회 전략**: 
    - 조회 위치: Query Side (MongoDB Atlas) 우선 사용 (읽기 최적화), Command Side는 동기화 지연 시 Fallback
    - 조회 범위: 세션별 최근 N개 메시지 또는 전체 메시지, ChatMemory용 조회는 토큰 제한 고려
    - 정렬: `sequenceNumber` 기준 오름차순 정렬 (대화 순서 보장)
    - 페이징: 대화 히스토리가 긴 경우 페이징 지원 (Spring Data의 `Pageable` 활용)
11. **세션 생명주기 관리**: 
    - 세션 자동 재활성화: 메시지 교환 시 `updateLastMessageAt()`에서 `isActive = true` 설정
    - 비활성화 처리: `inactive-threshold-minutes: 30` 설정, `deactivateInactiveSessions()` 구현
    - 만료 처리: `expiration-days: 90` 설정, `expireInactiveSessions()` 구현
    - 배치 스케줄러: `ConversationSessionLifecycleScheduler` 구현, `SchedulerConfig`로 @EnableScheduling 활성화

**D. Provider 및 메시지 변환**
12. **Provider별 메시지 변환**: OpenAI (기본, system 역할 사용, GPT-4o-mini는 system 지원, O1 모델 이상은 developer 역할 필요), Anthropic (대안, system 파라미터 분리) 차이 반영

**E. 토큰 제어 및 비용 통제**
13. **토큰 제어**: 토큰 수 예측, 검색 결과 토큰 제한, Provider별 컨텍스트 길이 제한 고려 (OpenAI GPT-4o-mini: 128K 컨텍스트)
14. **Redis 캐싱 전략**: 
    - 프로젝트 표준 패턴 준수: `Duration` 객체 직접 사용 (TimeUnit.SECONDS 대신)
    - TTL 설정: `ttl-hours: 1` (시간 단위)
    - RedisTemplate Bean: `RedisTemplate<String, Object>` 사용 (JSON 직렬화: GenericJackson2JsonRedisSerializer)
    - Bean 설정 위치: `common/core` 모듈의 `RedisConfig` 확장 (권장) 또는 `api/chatbot` 모듈에 `ChatbotRedisConfig` 생성
    - Key 네이밍: `chatbot:cache:{hash}` (네임스페이스 구분)
15. **비용 통제**: 요청 분류 및 라우팅, 캐싱 전략(Redis), Rate Limiting (RedisTemplate<String, String> 사용, Duration 객체), 토큰 사용량 추적

**F. 아키텍처 패턴**
16. **CQRS 패턴 적용**: Command Side (Aurora MySQL) 세션/메시지 저장, Query Side (MongoDB Atlas) 조회, Kafka 이벤트 동기화 (ConversationSyncService 구현 - 세션 및 메시지 동기화 통합)
17. **JWT 인증 통합**: Authentication 파라미터로 userId 추출, 모든 세션 조회/수정/삭제 시 세션 소유권 검증 필수, UnauthorizedException 처리

**G. 코드 품질 및 원칙**
18. **클린코드 원칙**: SRP, DIP, OCP 준수
19. **오버엔지니어링 금지**: 설계서에 명시된 기능만 구현, 공식 문서만 참고

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수):

**1단계: Analysis Purpose**
- Task Description 및 Requirements 이해: RAG 챗봇 구현 목표, OpenAI GPT-4o-mini + text-embedding-3-small 선택 이유, 멀티턴 대화 히스토리 관리, Provider별 차이점 파악, JWT 인증 통합, Redis 캐싱 패턴, 세션 생명주기 관리
- 작업 분해 및 우선순위: 
  1. langchain4j 통합 (OpenAI 설정, TokenCountEstimator Bean)
  2. Redis 캐싱 Bean 설정 (RedisTemplate<String, Object>)
  3. MongoDB Vector Search 설정 (1536 dimensions)
  4. 멀티턴 대화 히스토리 (TokenWindowChatMemory 기본 전략, 세션 생명주기 관리)
  5. JWT 인증 통합
  6. RAG 파이프라인
  7. API 엔드포인트
  8. 세션 생명주기 스케줄러 (배치 작업)
- 기존 시스템 통합 요구사항: MongoDB Atlas, Kafka, Redis, 기존 API 모듈 패턴 확인, JWT 인증 패턴 확인, Redis 캐싱 패턴 확인 (docs/step7/redis-optimization-best-practices.md)
- 설계서(`docs/step12/rag-chatbot-design.md`) 요구사항 명확화: 
  - LLM/Embedding Model 선택, BaseEntity 필드명, JWT 통합 방법
  - 설정 파일 구조 (langchain4j, chatbot.* 설정값들)
  - Redis 캐싱 전략 (Duration 객체, ttl-hours, RedisTemplate<String, Object>)
  - 세션 생명주기 관리 (inactive-threshold-minutes, expiration-days, 배치 스케줄러)
  - TokenWindowChatMemory 기본 전략 (max-tokens: 2000, TokenCountEstimator Bean)
  - Memory vs History 구분, 히스토리 조회 전략 (Query Side 우선, 페이징)
  - 임베딩 생성 배치 작업, Vector Index 생성 (MongoDB Atlas 콘솔)
  - 구현 가이드 단계별 참조 (설계서 "구현 가이드" 섹션)

**2단계: Identify Project Architecture**
- 루트 디렉토리 구조 및 설정 파일 확인: `build.gradle`, `settings.gradle`, `application*.yml`
- 설정 파일 경로 확인: `api/chatbot/src/main/resources/application-chatbot-api.yml` (챗봇 모듈 설정)
- 설정 파일 구조 확인: 
  - `langchain4j.open-ai.*` 설정 구조 (chat-model, embedding-model)
  - `chatbot.*` 설정 구조 (rag, input, token, cache, session, chat-memory)
- `api-chatbot` 모듈 구조, `api-contest`/`api-news`/`api-auth` 모듈 참고
- 아키텍처 패턴 식별: CQRS 패턴, Facade 패턴, 이벤트 기반 아키텍처, langchain4j 통합 패턴
- 핵심 컴포넌트 분석: 기존 API 모듈, MongoDB Repository, Kafka Producer, Redis 설정 (common/core의 RedisConfig 확인)

**3단계: Collect Information**
- 불확실한 부분: 사용자 질문, `read_file`, `codebase_search`, `web_search` 활용
- 정보 수집 대상: 설계서 전체, langchain4j 공식 문서, OpenAI/Anthropic API 공식 문서, MongoDB Atlas Vector Search 문서, 기존 API 모듈 구조
- 추측 금지: 공식 문서에 없는 내용은 추측하지 않음

**4단계: Check Existing Programs and Structures**
- 원칙: "check first, then design"
- 검색 전략: `read_file`, `codebase_search` 활용
- 기존 코드 확인: 
  - Controller/Facade/Service 구조
  - MongoDB Document 구조
  - Repository 패턴
  - Kafka 이벤트 발행 방식
  - Redis 설정 (common/core의 RedisConfig, RedisTemplate<String, String> 패턴 확인)
  - 세션 생명주기 관리 패턴 (기존 모듈의 스케줄러 구현 확인)
  - BaseEntity 필드명 패턴 (isDeleted, deletedBy, updatedBy)
- 패턴 기록 및 준수: 발견한 코드 패턴 기록, 설계 시 패턴 확장 계획

**5단계: Task Type-Specific Guidelines**
- Backend API Tasks: API 라우트 구조, 에러 처리, 응답 형식, JWT 인증 통합 (Authentication 파라미터)
- LLM 통합 Tasks: 
  - langchain4j OpenAiChatModel (GPT-4o-mini), OpenAiEmbeddingModel (text-embedding-3-small, 1536 dimensions) 설정
  - TokenCountEstimator Bean 설정 (TokenWindowChatMemory용)
  - Provider별 차이점 고려 (OpenAI system 역할, Anthropic system 파라미터 분리)
- Redis 캐싱 Tasks: 
  - RedisTemplate<String, Object> Bean 설정 (common/core의 RedisConfig 확장 또는 ChatbotRedisConfig 생성)
  - Duration 객체 직접 사용 (프로젝트 표준)
  - ttl-hours 설정 (시간 단위)
  - JSON 직렬화 (GenericJackson2JsonRedisSerializer)
- Vector Search Tasks: 
  - MongoDB Atlas Vector Index 설정 (1536 dimensions, MongoDB Atlas 콘솔에서 생성)
  - 벡터 필드 추가 (ContestDocument, NewsArticleDocument, ArchiveDocument)
  - 임베딩 생성 배치 작업 (기존 도큐먼트 임베딩 생성, 신규 도큐먼트 자동 임베딩 생성)
  - 검색 쿼리 구현, 성능 최적화
- 멀티턴 대화 Tasks: 
  - 세션 관리, 메시지 저장, ChatMemory 통합
  - Memory vs History 구분 (History: 전체 기록 저장, Memory: LLM 컨텍스트만)
  - TokenWindowChatMemory 기본 전략 사용 (max-tokens: 2000)
  - 히스토리 조회 전략 (Query Side 우선, 페이징 지원)
  - 세션 생명주기 관리 (자동 재활성화, 비활성화, 만료 처리)
  - Provider별 메시지 변환
  - JWT 기반 세션 소유권 검증
- 세션 생명주기 스케줄러 Tasks: 
  - ConversationSessionLifecycleScheduler 구현 (비활성화, 만료 처리)
  - SchedulerConfig로 @EnableScheduling 활성화
  - inactive-threshold-minutes, expiration-days 설정 활용
- RAG 파이프라인 Tasks: 입력 전처리, 벡터 검색, 프롬프트 체인, 답변 생성
- CQRS 동기화 Tasks: ConversationSyncService 구현 (세션 및 메시지 동기화 통합), Kafka 이벤트 발행/소비, 기존 ArchiveSyncService 패턴 참고

**6단계: Preliminary Solution Output**
- 초기 설계 솔루션 작성 (복잡한 경우 `process_thought` 활용)
- 사실(facts)과 추론(inferences) 명확히 구분
- 설계서 구현 가이드 참조 (구체적 섹션: "langchain4j 통합 설계", "Redis 캐싱 전략 설계", "세션 생명주기 관리", "멀티턴 대화 히스토리 관리 설계", "구현 가이드" 섹션의 8단계 프로세스)
- 필수 포함 사항: 
  - 프로젝트 구조
  - 설정 파일 구조 (`application-chatbot-api.yml`: langchain4j, chatbot.* 설정값들)
  - langchain4j 통합 (OpenAI GPT-4o-mini + text-embedding-3-small, TokenCountEstimator Bean)
  - Redis 캐싱 Bean 설정 (RedisTemplate<String, Object>, Duration 객체, ttl-hours)
  - MongoDB Vector Search 설정 (1536 dimensions, Vector Index 생성, 임베딩 생성 배치 작업)
  - 멀티턴 대화 히스토리 (Memory vs History 구분, TokenWindowChatMemory 기본 전략, max-tokens: 2000, 히스토리 조회 전략)
  - 세션 생명주기 관리 (자동 재활성화, 비활성화, 만료 처리, 배치 스케줄러)
  - Provider별 변환 (OpenAI 기본, Anthropic 대안)
  - JWT 인증 통합
  - RAG 파이프라인
  - API 엔드포인트
  - 토큰 제어 (128K 컨텍스트)
  - 비용 통제
  - CQRS 동기화 (Kafka)
- 중요 경고: 가정/추측/상상 금지, 실제 정보 수집 필수, 공식 문서만 참고
- 도구 호출 필수: `analyze_task` 호출

**Acceptance Criteria** (작업 완료 기준):
- 설계서 요구사항 분석 및 초기 설계 솔루션 반영
- "check first, then design" 원칙 준수
- 오버엔지니어링 방지, 클린코드 원칙 준수
- 모든 설정 파일 구현 완료 (`application-chatbot-api.yml`의 모든 설정값)
- 모든 Bean 설정 완료 (langchain4j, Redis, TokenCountEstimator)
- MongoDB Vector Index 생성 완료 (콘솔 작업)
- 임베딩 생성 배치 작업 구현 완료
- 모든 서비스 레이어 구현 완료
- 모든 API 엔드포인트 구현 완료
- 세션 생명주기 스케줄러 구현 완료
- CQRS 동기화 구현 완료

**검증 기준** (참고용, 설계서 "검증 기준" 섹션의 세부 항목 참고):
- **기능 검증** (설계서 "기능 검증 기준" 섹션 참고):
  - RAG 파이프라인 (입력 전처리, 벡터 검색, 프롬프트 체인, 답변 생성)
  - 멀티턴 대화 (TokenWindowChatMemory 기본 전략, Memory vs History 구분, 히스토리 조회 전략)
  - Provider별 변환 (OpenAI, Anthropic)
  - 토큰 제어 (토큰 수 예측, 검색 결과 토큰 제한, 128K 컨텍스트 준수)
  - 비용 통제 (Redis 캐싱, Rate Limiting, 토큰 사용량 추적)
  - 세션 생명주기 관리 (자동 재활성화, 비활성화, 만료 처리, 배치 스케줄러)
  - 세션 관리 (새 세션 생성, 기존 세션 사용, 소유권 검증, 히스토리 저장 및 조회)
- **성능 검증** (설계서 "성능 검증 기준" 섹션 참고):
  - 응답 시간 (평균 < 3초, 95 percentile < 5초)
  - 벡터 검색 성능 (검색 쿼리 실행 시간 < 500ms)
  - 히스토리 조회 성능
  - LLM 호출 응답 시간
  - Redis 캐싱 성능
  - 토큰 사용량 (평균 입력 토큰 < 2000, 평균 출력 토큰 < 1000, 최대 컨텍스트 길이 128K 토큰 준수)
- **비용 검증** (설계서 "비용 검증 기준" 섹션 참고):
  - 토큰 사용량 추적 (요청별, 사용자별 일일 집계)
  - 비용 예측 (일일/월간 예상 비용, 세션별/사용자별 비용 집계)
  - 캐싱 효과 (캐시 히트율 > 30%, 비용 절감 측정)
- **품질 검증** (설계서 "품질 검증 기준" 섹션 참고):
  - 응답 품질 (답변 정확도 > 80%, 참조 문서 관련성 > 70%, 멀티턴 대화 컨텍스트 유지)
  - 에러 처리 (모든 예외 상황 처리)
- **데이터 일관성 검증**: CQRS 패턴 동기화 (Aurora MySQL → Kafka → MongoDB Atlas)
- **빌드 검증**: `api-chatbot` 모듈 빌드 성공, 컴파일 에러 없음
- **Bean 설정 검증**: langchain4j Beans, RedisTemplate Beans (String, Object), TokenCountEstimator Bean

**참고 파일**:
- **설계서 (필수)**:
  - `docs/step12/rag-chatbot-design.md`: RAG 챗봇 설계서 (필수)
    - "langchain4j 통합 설계" 섹션: LLM/Embedding Model 설정, TokenCountEstimator Bean, 설정 파일 구조
    - "MongoDB Atlas Vector Search 설계" 섹션: 벡터 필드 추가, Vector Index 생성, 임베딩 생성 배치 작업
    - "Redis 캐싱 전략 설계" 섹션: Duration 객체, ttl-hours, RedisTemplate<String, Object> Bean
    - "멀티턴 대화 히스토리 관리 설계" 섹션: Memory vs History 구분, TokenWindowChatMemory 기본 전략, 히스토리 조회 전략, 세션 생명주기 관리
    - "세션 생명주기 관리" 섹션: inactive-threshold-minutes, expiration-days, 배치 스케줄러
    - "비용 통제 전략 설계" 섹션: 캐싱, Rate Limiting
    - "구현 가이드" 섹션: 8단계 구현 프로세스 (1단계: 의존성 설정, 2단계: langchain4j 통합, 3단계: MongoDB Vector Search, 4단계: 서비스 레이어, 5단계: 멀티턴 대화, 6단계: 체인, 7단계: API 레이어, 8단계: 테스트)
    - "검증 기준" 섹션: 기능/성능/비용/품질 검증 기준의 세부 항목
- **프로젝트 설계 문서**:
  - `docs/step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
  - `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스 (캐싱 패턴 참고)
  - `docs/step11/cqrs-kafka-sync-design.md`: CQRS Kafka 동기화 설계
  - `docs/step2/1. api-endpoint-design.md`: API 엔드포인트 설계
  - `docs/step9/contest-news-api-design.md`: Contest/News API 설계 (기존 API 모듈 구조 참고)
- **공식 가이드**:
  - Shrimp Task Manager 공식 가이드: http://localhost:9999/?template-view=preview&template-id=planTask#templates
- **외부 문서**:
  - langchain4j 공식 문서: https://docs.langchain4j.dev/
  - OpenAI API 공식 문서: https://platform.openai.com/docs/api-reference/chat
    - Chat Completions API: https://platform.openai.com/docs/guides/chat-completions
    - GPT-4o-mini: https://platform.openai.com/docs/models/gpt-4o-mini
    - text-embedding-3-small: https://platform.openai.com/docs/models/text-embedding-3-small
    - OpenAI API 역할 변경사항 (2025): GPT-4o-mini는 "system" 역할 지원, O1 모델 이상은 "developer" 역할 사용 필요
  - Anthropic API 공식 문서: https://docs.anthropic.com/en/api/messages
  - MongoDB Atlas Vector Search 문서: https://www.mongodb.com/docs/atlas/atlas-vector-search/

**작업 분해 참고 정보** (splitTasksRaw 단계에서만 활용):

**중요**: 아래 정보는 `splitTasksRaw` 단계에서만 참고용입니다. plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행하세요.

**모듈 구조**:
- `api-chatbot`: 
  - Controller (JWT 인증 통합, Authentication 파라미터)
  - Facade
  - Service (Chatbot, InputPreprocessing, IntentClassification, VectorSearch, Prompt, LLM, Token, Cache, ConversationSession, ConversationMessage)
  - Chain (InputInterpretation, ResultRefinement, AnswerGeneration)
  - Converter (MessageFormatConverter - OpenAiMessageConverter 기본, AnthropicMessageConverter 대안)
  - Memory (ChatMemoryStore - MongoDbChatMemoryStore, ChatMemoryProvider - TokenWindowChatMemory 기본 전략)
  - Scheduler (ConversationSessionLifecycleScheduler - 세션 생명주기 배치 작업)
  - DTO (ChatRequest에서 userId 필드 제거)
  - Config:
    - LangChain4jConfig (OpenAiChatModel, OpenAiEmbeddingModel, TokenCountEstimator Bean)
    - ChatbotRedisConfig (선택: RedisTemplate<String, Object> Bean, 방법 2인 경우)
    - SchedulerConfig (@EnableScheduling 활성화)
  - Exception (UnauthorizedException, ConversationSessionNotFoundException)
- `domain-aurora`: 
  - `ConversationSessionEntity` (BaseEntity 상속, isDeleted 필드 사용)
  - `ConversationMessageEntity`
  - Repository:
    - ConversationSessionWriterRepository (findByUserIdAndIsDeletedFalse 패턴, findByIsActiveTrueAndIsDeletedFalseAndLastMessageAtBefore, findByIsActiveFalseAndIsDeletedFalseAndLastMessageAtBefore)
    - ConversationMessageWriterRepository
- `domain-mongodb`: 
  - `ContestDocument`, `NewsArticleDocument`, `ArchiveDocument` (벡터 필드 추가 필요, 1536 dimensions)
  - `ConversationSessionDocument`, `ConversationMessageDocument` (새로 생성)
  - Repository (확장 필요)
- `common-core`: 
  - RedisConfig (확장: RedisTemplate<String, Object> Bean 추가, 방법 1인 경우 권장)
- `common-kafka`: 
  - `EventPublisher`, `EventConsumer`
  - `ConversationSyncService` (새로 생성, 세션 및 메시지 동기화 통합, 기존 ArchiveSyncService 패턴 참고)

**작업 의존성** (상세 순서, 설계서 "구현 가이드" 섹션의 8단계 프로세스 참고):
1. **의존성 설정** (설계서 1단계): langchain4j 의존성 추가, build.gradle 설정
2. **langchain4j 통합** (설계서 2단계): 
   - LangChain4jConfig 구현 (OpenAI 설정, TokenCountEstimator Bean)
   - Redis 캐싱 Bean 설정 (RedisTemplate<String, Object> - common/core 확장 또는 api/chatbot 생성)
   - 설정 파일 작성 (`application-chatbot-api.yml`: langchain4j, chatbot.* 설정값들)
3. **MongoDB Vector Search 설정** (설계서 3단계):
   - 벡터 필드 추가 (ContestDocument, NewsArticleDocument, ArchiveDocument, 1536 dimensions)
   - Vector Index 생성 (MongoDB Atlas 콘솔 작업)
   - 임베딩 생성 배치 작업 (기존 도큐먼트 임베딩 생성, 신규 도큐먼트 자동 임베딩 생성)
4. **서비스 레이어 구현** (설계서 4단계): InputPreprocessingService, IntentClassificationService, VectorSearchService, PromptService, LLMService, TokenService, CacheService, ConversationSessionService, ConversationMessageService, Repository 확장
5. **멀티턴 대화 히스토리 구현** (설계서 5단계):
   - ConversationSession, ConversationMessage 엔티티/Document
   - ChatMemoryStore 구현 (MongoDbChatMemoryStore)
   - ChatMemoryProvider 구현 (TokenWindowChatMemory 기본 전략)
   - Memory vs History 구분, 히스토리 조회 전략 (Query Side 우선, 페이징)
   - MessageFormatConverter 구현
   - 세션 생명주기 관리 (자동 재활성화, 비활성화, 만료 처리)
6. **체인 구현** (설계서 6단계): InputInterpretationChain, ResultRefinementChain, AnswerGenerationChain
7. **API 레이어 구현** (설계서 7단계): DTO, ChatbotFacade, ChatbotController, ExceptionHandler, JWT 인증 통합
8. **세션 생명주기 스케줄러** (배치 작업): ConversationSessionLifecycleScheduler, SchedulerConfig
9. **CQRS 동기화** (Kafka): ConversationSyncService 구현
10. **테스트** (설계서 8단계): 단위 테스트, 통합 테스트, 성능 테스트
```

### 14단계: API Gateway 서버 구현

**단계 번호**: 14단계
**의존성**: 2단계 (API 설계 완료 필수), 6단계 (OAuth 및 JWT 인증 구현 완료 필수), 9단계 (Contest 및 News API 구현 완료 필수), 12단계 (Archive API 구현 완료 필수), 13단계 (Chatbot API 구현 완료 필수)
**다음 단계**: 15단계 (API 컨트롤러 및 서비스 구현) 또는 16단계 (Batch 모듈 구현)

```
plan task: Spring Cloud Gateway 기반 API Gateway 서버 구현

참고 파일: docs/step17/gateway-design.md (API Gateway 설계서, 필수), docs/step17/gateway-implementation-plan.md (API Gateway 구현 계획서, 필수), api/gateway/REF.md (Saturn Gateway Server 아키텍처 참고), docs/step2/1. api-endpoint-design.md (API 엔드포인트 설계), docs/step6/spring-security-auth-design-guide.md (Spring Security 설계 가이드), docs/step9/contest-news-api-design.md (Contest/News API 설계), docs/step12/rag-chatbot-design.md (Chatbot API 설계), docs/step13/user-archive-feature-design.md (Archive API 설계)

**description** (작업 설명):
- **작업 목표**:
  - Spring Cloud Gateway 기반 API Gateway 서버 구현
  - URI 기반 라우팅 규칙 구현 (5개 API 서버: auth, archive, contest, news, chatbot)
  - JWT 토큰 기반 인증 필터 구현
  - 연결 풀 설정 및 Connection reset by peer 방지
  - CORS 설정 (환경별 차별화)
  - 에러 처리 및 모니터링 구현
  - `docs/step17/gateway-design.md` 설계서를 엄격히 준수

- **배경**:
  - 인프라 아키텍처: Client → ALB → Gateway Server → API Servers
  - Gateway 서버는 모든 외부 요청을 중앙에서 관리하고 적절한 백엔드 API 서버로 라우팅
  - **라우팅 흐름 명확화**:
    * 인증이 필요한 요청 (예: `/api/v1/archive/**`, `/api/v1/chatbot/**`):
      - Client → ALB → Gateway → [JWT 인증 필터 (Gateway 내부)] → Archive/Chatbot 서버
      - Gateway 내부의 JWT 인증 필터가 토큰을 검증하므로 인증 서버(`@api/auth`)를 거치지 않음
      - JWT는 stateless이므로 Gateway에서 직접 검증 가능 (`common-security` 모듈의 `JwtTokenProvider` 활용)
      - **JWT 토큰 만료/무효 시**: Gateway는 401 Unauthorized를 반환하고, 인증 서버를 자동으로 호출하지 않음
      - **토큰 갱신 흐름 (사용자 응답 기준)**:
        * **시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)**
          - **특징**: 사용자 개입 없이 클라이언트(프론트엔드/앱)가 자동으로 처리
          - **1단계**: Client → Gateway → Archive 요청 (만료된 Access Token)
            - 요청: `GET /api/v1/archive`, `Authorization: Bearer {만료된_토큰}`
            - Gateway 응답: `401 Unauthorized`
            - 응답 본문: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
            - 클라이언트 동작: 401 응답 감지 → Refresh Token으로 자동 갱신 요청
          - **2단계**: Client → Gateway → Auth 서버: `POST /api/v1/auth/refresh` (자동 요청)
            - 요청: `{"refreshToken": "{유효한_refresh_token}"}`
            - Gateway 라우팅: Gateway → Auth 서버 (인증 필터 우회)
            - Auth 서버 응답: `200 OK`
            - 응답 본문: `{"code": "2000", "messageCode": {"code": "SUCCESS", "text": "성공"}, "message": "success", "data": {"accessToken": "{새_토큰}", "refreshToken": "{새_refresh_토큰}", "tokenType": "Bearer", "expiresIn": 3600, "refreshTokenExpiresIn": 604800}}`
            - 클라이언트 동작: 새 Access Token 저장 후 원래 요청 자동 재시도
          - **3단계**: Client → Gateway → Archive 요청 (새 Access Token, 자동 재시도)
            - 요청: `GET /api/v1/archive`, `Authorization: Bearer {새_토큰}`
            - Gateway 응답: JWT 검증 성공 → Archive 서버로 라우팅
            - Archive 서버 응답: `200 OK` (정상 응답)
        * **시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)**
          - **특징**: 클라이언트가 자동 처리하다가 Refresh Token 만료 시 사용자 개입 필요
          - **1단계**: Client → Gateway → Archive 요청 (만료된 Access Token)
            - 요청: `GET /api/v1/archive`, `Authorization: Bearer {만료된_토큰}`
            - Gateway 응답: `401 Unauthorized`
            - 응답 본문: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
            - 클라이언트 동작: 401 응답 감지 → Refresh Token으로 자동 갱신 시도
          - **2단계**: Client → Gateway → Auth 서버: `POST /api/v1/auth/refresh` (자동 시도)
            - 요청: `{"refreshToken": "{만료된_또는_없는_refresh_token}"}`
            - Gateway 라우팅: Gateway → Auth 서버 (인증 필터 우회)
            - Auth 서버 응답: `401 Unauthorized`
            - 응답 본문: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "유효하지 않은 Refresh Token입니다."}}`
            - 클라이언트 동작: Refresh Token도 만료됨을 감지 → 사용자에게 로그인 화면 표시
          - **3단계**: 사용자 개입 (로그인 화면에서 이메일/비밀번호 입력)
            - 사용자 동작: 로그인 화면에서 이메일/비밀번호 입력
          - **4단계**: Client → Gateway → Auth 서버: `POST /api/v1/auth/login` (사용자 입력 후 요청)
            - 요청: `{"email": "user@example.com", "password": "password"}` (사용자 입력)
            - Gateway 라우팅: Gateway → Auth 서버 (인증 필터 우회)
            - Auth 서버 응답: `200 OK`
            - 응답 본문: `{"code": "2000", "messageCode": {"code": "SUCCESS", "text": "성공"}, "message": "success", "data": {"accessToken": "{새_토큰}", "refreshToken": "{새_refresh_토큰}", "tokenType": "Bearer", "expiresIn": 3600, "refreshTokenExpiresIn": 604800}}`
            - 클라이언트 동작: 새 Access Token 저장 후 원래 요청 자동 재시도
          - **5단계**: Client → Gateway → Archive 요청 (새 Access Token, 자동 재시도)
            - 요청: `GET /api/v1/archive`, `Authorization: Bearer {새_토큰}`
            - Gateway 응답: JWT 검증 성공 → Archive 서버로 라우팅
            - Archive 서버 응답: `200 OK` (정상 응답)
    * 인증 서버 요청 (예: `/api/v1/auth/login`, `/api/v1/auth/refresh`):
      - Client → ALB → Gateway → Auth 서버
      - `/api/v1/auth/**` 경로만 `@api/auth` 서버로 라우팅
  - JWT 토큰 기반 인증을 Gateway에서 수행하여 백엔드 서버로 사용자 정보 헤더 주입
  - `@api/auth` 모듈은 별도 서버로 유지, Gateway에서 JWT 검증만 수행 (옵션 B 채택)

- **예상 결과**:
  - Spring Cloud Gateway 기반 Gateway 서버 구현 완료
  - 5개 API 서버에 대한 라우팅 규칙 구현 완료
  - JWT 인증 필터 구현 완료 (인증 필요/불필요 경로 구분)
  - 연결 풀 설정 완료 (Connection reset by peer 방지)
  - CORS 설정 완료 (환경별 차별화)
  - 에러 처리 및 로깅 완료

**requirements** (기술 요구사항 및 제약조건):
1. **설계서 엄격 준수**: `docs/step17/gateway-design.md`의 모든 설계 사항을 정확히 구현
2. **오버엔지니어링 금지**: 
   - 설계서에 명시되지 않은 기능 추가 금지
   - Rate Limiting, Circuit Breaker 등은 명시적 요구 시에만 구현
   - 서비스 디스커버리 (Eureka, Consul 등)는 명시적 요구 없음
   - 불필요한 복잡한 라우팅 로직 금지
3. **공식 문서만 참고**: 
   - Spring Cloud Gateway 공식 문서만 참고 (https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
   - Reactor Netty 공식 문서만 참고 (https://projectreactor.io/docs/netty/release/reference/index.html)
   - Spring Boot 공식 문서만 참고
   - 신뢰할 수 없는 블로그나 커뮤니티 자료는 참고하지 않음
4. **인증 통합 방안**: 옵션 B 채택 (`@api/auth` 모듈을 별도 서버로 유지, Gateway에서 JWT 검증만 수행)
5. **연결 풀 설정**: Connection reset by peer 방지를 위한 구체적 설정 값 제시 및 근거 설명
6. **CORS 설정**: 환경별 차별화 (Local/Dev: 개발 편의성, Beta/Prod: 보안 우선)
7. **기존 프로젝트 구조 준수**: `api/gateway` 모듈 구조, 기존 API 모듈 패턴 준수

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: Spring Cloud Gateway 기반 API Gateway 서버 구현, URI 기반 라우팅, JWT 인증 필터, 연결 풀 설정, CORS 설정
  - 배경: Client → ALB → Gateway Server → API Servers 아키텍처, JWT 토큰 기반 인증, `@api/auth` 모듈은 별도 서버로 유지
  - **라우팅 흐름 이해**:
    * 인증이 필요한 요청: Gateway 내부 JWT 필터에서 검증 → 해당 API 서버로 직접 라우팅 (인증 서버 거치지 않음)
    * JWT 토큰 만료/무효 시: Gateway는 401 Unauthorized 반환, 인증 서버를 자동 호출하지 않음
    * 토큰 갱신 흐름 (사용자 응답 기준):
      - **시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)**
        * 특징: 사용자 개입 없이 클라이언트(프론트엔드/앱)가 자동으로 처리
        * 1단계: Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` → 클라이언트: 401 응답 감지
        * 2단계: `POST /api/v1/auth/refresh` (유효한 Refresh Token, 자동 요청) → Gateway → Auth 서버: `200 OK` (새 Access Token + Refresh Token 발급)
        * 3단계: Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
      - **시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)**
        * 특징: 클라이언트가 자동 처리하다가 Refresh Token 만료 시 사용자 개입 필요
        * 1단계: Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` → 클라이언트: 401 응답 감지
        * 2단계: `POST /api/v1/auth/refresh` (만료된/없는 Refresh Token, 자동 시도) → Gateway → Auth 서버: `401 Unauthorized` → 클라이언트: Refresh Token도 만료됨을 감지
        * 3단계: 사용자 개입 (로그인 화면 표시, 이메일/비밀번호 입력)
        * 4단계: `POST /api/v1/auth/login` (사용자 입력 후 요청) → Gateway → Auth 서버: `200 OK` (새 Access Token + Refresh Token 발급)
        * 5단계: Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
    * 인증 서버 요청: Gateway → Auth 서버로 직접 라우팅 (`/api/v1/auth/**` 경로)
  - 예상 결과: Gateway 서버 구현 완료, 5개 API 서버 라우팅, JWT 인증 필터, 연결 풀 설정, CORS 설정
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - 설계서 엄격 준수 (`docs/step17/gateway-design.md`)
  - 오버엔지니어링 금지 (Rate Limiting, Circuit Breaker 등은 명시적 요구 시에만)
  - 공식 문서만 참고 (Spring Cloud Gateway, Reactor Netty, Spring Boot)
  - 인증 통합 방안: 옵션 B 채택
  - 연결 풀 설정: Connection reset by peer 방지
  - CORS 설정: 환경별 차별화
- **작업 분해 및 우선순위** (참고 정보):
  - **Phase 1: 기본 설정 및 라우팅** (우선순위: 높음)
    * 의존성: 없음
    * 작업: Spring Cloud Gateway 의존성 추가, 기본 라우팅 설정, Gateway 서버 실행 확인
    * 참고: `docs/step17/gateway-design.md` "설정 파일 설계" 섹션
  - **Phase 2: JWT 인증 필터 구현** (우선순위: 높음)
    * 의존성: Phase 1 완료, 6단계 (JWT 인증 구현 완료 필수)
    * 작업: `JwtAuthenticationGatewayFilter` 구현, 인증 필요/불필요 경로 구분, 사용자 정보 헤더 주입
    * 참고: `docs/step17/gateway-design.md` "인증 및 보안 설계" 섹션, `common-security` 모듈의 `JwtTokenProvider` 활용
  - **Phase 3: 연결 풀 설정** (우선순위: 높음)
    * 의존성: Phase 1 완료
    * 작업: HTTP 클라이언트 연결 풀 설정, Connection reset by peer 방지 설정
    * 참고: `docs/step17/gateway-design.md` "연결 풀 및 성능 최적화" 섹션
  - **Phase 4: CORS 설정** (우선순위: 중간)
    * 의존성: Phase 1 완료
    * 작업: Global CORS 설정, 환경별 CORS 정책 설정, 외부 API 연동 시 중복 헤더 제거 필터
    * 참고: `docs/step17/gateway-design.md` "CORS 설정" 섹션
  - **Phase 5: 에러 처리 및 모니터링** (우선순위: 중간)
    * 의존성: Phase 1-4 완료
    * 작업: Global 예외 처리 구현 (`ErrorWebExceptionHandler` 또는 `GlobalFilter`), 공통 에러 응답 형식, 로깅 전략
    * 참고: `docs/step17/gateway-design.md` "에러 처리 및 모니터링" 섹션
- **확인 사항**:
  - 설계서의 모든 요구사항 명확화
  - 인증 통합 방안 이해 (옵션 B 채택)
  - 연결 풀 설정 값 및 근거 이해
  - CORS 설정 환경별 차별화 이해

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **루트 디렉토리 구조 및 설정 파일 확인**:
  - 루트 디렉토리 구조 확인 (`list_dir`로 프로젝트 루트 구조 파악)
  - `build.gradle` 확인 (프로젝트 빌드 설정, 모듈 의존성, Spring Boot 4.0.1, Spring Cloud 2025.1.0)
  - `settings.gradle` 확인 (Gradle 모듈 구조, `api/gateway` 모듈 포함 여부)
  - `api/gateway/build.gradle` 확인 (현재 의존성 상태, Spring Cloud Gateway 의존성 추가 필요 여부 확인)
  - `api/gateway/src/main/resources/application*.yml` 확인 (현재 설정 상태, 라우팅/연결 풀/CORS 설정 여부 확인)
- **핵심 설정 파일 및 구조 확인**:
  - `api/gateway` 모듈의 현재 구조 확인 (`GatewayApplication.java`, `ApiGatewayExceptionHandler.java` 등)
  - `common-security` 모듈의 `JwtTokenProvider` 확인 (`validateToken`, `getPayloadFromToken` 메서드)
  - `common-core` 모듈의 `ApiResponse`, `MessageCode`, `ErrorCodeConstants` 확인
  - `docs/step17/gateway-design.md` 설계서 전체 읽기 (패키지 구조, 라우팅 설정, 인증 필터, 연결 풀, CORS, 에러 처리)
  - `docs/step17/gateway-implementation-plan.md` 구현 계획서 전체 읽기 (6단계 프로세스, 초기 설계 솔루션)
  - `api/gateway/REF.md` 참고 문서 확인 (Saturn Gateway Server 아키텍처, 연결 풀 설정, CORS 설정 가이드라인)
- **아키텍처 패턴 식별**:
  - Spring Cloud Gateway 패턴: Route → Predicate → Filter → Backend Service
  - Gateway Filter 패턴: Reactive 기반 (Mono<Void> 반환, ServerWebExchange 활용)
  - JWT 인증 패턴: `JwtTokenProvider` 활용, Gateway Filter에서 검증 (블로킹 호출 허용)
  - 에러 처리 패턴: ErrorWebExceptionHandler 구현 (Reactive 기반, @ControllerAdvice 사용 불가)
- **핵심 컴포넌트 분석**:
  - `api/gateway` 모듈의 현재 구현 상태 확인 (기본 구조만 존재, Spring Cloud Gateway 의존성 없음)
  - `common-security` 모듈의 `JwtTokenProvider` 메서드 확인 (`validateToken`, `getPayloadFromToken`)
  - `JwtTokenPayload` 구조 확인 (`userId`, `email`, `role` 필드)
  - 기존 API 모듈의 엔드포인트 구조 확인 (라우팅 규칙 설계 참고: `/api/v1/{service}/**`)

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - `read_file`로 `docs/step17/gateway-design.md` 설계서 전체 읽기
  - `read_file`로 `docs/step17/gateway-implementation-plan.md` 구현 계획서 전체 읽기
  - `read_file`로 `api/gateway/REF.md` 참고 문서 확인
  - `codebase_search`로 `JwtTokenProvider` 구현 확인
  - `read_file`로 `api/gateway/build.gradle` 확인
  - `read_file`로 `api/gateway/src/main/java/com/ebson/shrimp/tm/demo/api/gateway/` 구조 확인
  - `read_file`로 기존 API 모듈의 엔드포인트 구조 확인
  - `web_search`로 Spring Cloud Gateway 공식 문서 검색 (불熟悉한 개념이나 기술의 경우)
- **추측 금지**: 설계서에 명시되지 않은 기능은 구현하지 않음, 공식 문서만 참고
- **정보 수집 대상**:
  - `docs/step17/gateway-design.md` 설계서 전체 내용 (패키지 구조, 라우팅 설정, 인증 필터, 연결 풀, CORS, 에러 처리)
  - `docs/step17/gateway-implementation-plan.md` 구현 계획서 전체 내용 (6단계 프로세스, 초기 설계 솔루션, 구현 우선순위)
  - `api/gateway/REF.md` 참고 문서 (Saturn Gateway Server 아키텍처, 연결 풀 설정 값 및 근거, CORS 설정 가이드라인)
  - `common-security` 모듈의 `JwtTokenProvider` 구현 (`validateToken`, `getPayloadFromToken` 메서드)
  - `common-core` 모듈의 `ApiResponse`, `MessageCode`, `ErrorCodeConstants` 구현
  - `api/gateway` 모듈의 현재 구조 (기본 구조만 존재, Spring Cloud Gateway 의존성 없음)
  - 기존 API 모듈의 엔드포인트 구조 (라우팅 규칙 설계 참고: `/api/v1/{service}/**`)

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **중요 원칙: "check first, then design"**:
  - **반드시 기존 코드를 확인하기 전에 설계를 생성하지 않음**
  - 먼저 기존 코드를 확인한 후 설계를 진행해야 함
  - 기존 코드 확인 없이 설계를 먼저 생성하는 것은 엄격히 금지됨
- **정확한 검색 전략 사용**:
  - `read_file`로 `api/gateway/src/main/java/com/ebson/shrimp/tm/demo/api/gateway/` 구조 확인
  - `read_file`로 `api/gateway/build.gradle` 확인
  - `read_file`로 `common-security/src/main/java/com/ebson/shrimp/tm/demo/common/security/jwt/JwtTokenProvider.java` 확인
  - `codebase_search`로 Gateway 관련 구현 검색
- **기존 코드 확인** (check first, then design):
  - `api/gateway` 모듈의 현재 구현 상태 상세 분석
    * `GatewayApplication.java` 확인
    * 기존 설정 파일 확인 (`application.yml`, `application-local.yml` 등)
    * 기존 컴포넌트 확인 (Config, Filter, Exception Handler 등)
  - `common-security` 모듈의 `JwtTokenProvider` 구조 분석
    * `validateToken(token)` 메서드 확인
    * `getPayloadFromToken(token)` 메서드 확인
    * `JwtTokenPayload` 구조 확인
  - 기존 API 모듈의 엔드포인트 구조 확인
    * `api/auth` 모듈: `/api/v1/auth/**`
    * `api/archive` 모듈: `/api/v1/archive/**`
    * `api/contest` 모듈: `/api/v1/contest/**`
    * `api/news` 모듈: `/api/v1/news/**`
    * `api/chatbot` 모듈: `/api/v1/chatbot/**`
- **코드 스타일 및 규칙 분석**:
  - 기존 컴포넌트의 명명 규칙 확인 (camelCase 등)
  - 주석 스타일 및 형식 규칙 확인
  - 에러 처리 패턴 및 로깅 방법 분석
- **패턴 기록 및 준수**:
  - 발견한 코드 패턴 및 조직 구조를 상세히 기록
  - 설계 시 이러한 패턴을 어떻게 확장할지 계획

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **Spring Cloud Gateway Tasks** 관련 추가 고려사항:
  - **라우팅 설정 패턴 분석**:
    * Route → Predicate → Filter → Backend Service 구조 확인
    * Path 기반 라우팅 (`Path=/api/v1/auth/**` 등)
    * 환경별 백엔드 서비스 URL 설정 (Local: localhost, Dev/Beta/Prod: service-name)
  - **Gateway Filter 패턴 분석**:
    * Reactive 기반 (`Mono<Void>` 반환, `ServerWebExchange` 활용)
    * `GatewayFilter` 인터페이스 구현 또는 `AbstractGatewayFilterFactory` 상속
    * `JwtTokenProvider`는 동기 메서드이므로 Gateway Filter에서 직접 사용 가능 (블로킹 호출 허용)
    * Route 설정에서 필터 등록 방법 확인 (`filters` 섹션 또는 `GatewayConfig`에서 Bean 등록)
  - **인증 필터 구현 패턴**:
    * 인증 필요/불필요 경로 구분 (`isPublicPath` 메서드)
    * JWT 토큰 추출 (`Authorization: Bearer {token}`)
    * JWT 토큰 검증 (`JwtTokenProvider.validateToken`)
    * **토큰 만료/무효 시 처리**: 401 Unauthorized 반환, 인증 서버를 자동 호출하지 않음 (`handleUnauthorized` 메서드)
      - 응답 형식: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
      - 클라이언트는 이 응답을 받고 별도로 토큰 갱신 요청을 수행
    * 사용자 정보 추출 및 헤더 주입 (`x-user-id`, `x-user-email`, `x-user-role`)
    * **토큰 갱신 흐름 (사용자 응답 기준)**: Gateway에서 자동 처리하지 않음
      - **시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)**
        * 특징: 사용자 개입 없이 클라이언트(프론트엔드/앱)가 자동으로 처리
        * Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` → 클라이언트: 401 응답 감지
        * `POST /api/v1/auth/refresh` (유효한 Refresh Token, 자동 요청) → Gateway → Auth 서버: `200 OK` (새 토큰 발급)
        * Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
      - **시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)**
        * 특징: 클라이언트가 자동 처리하다가 Refresh Token 만료 시 사용자 개입 필요
        * Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` → 클라이언트: 401 응답 감지
        * `POST /api/v1/auth/refresh` (만료된/없는 Refresh Token, 자동 시도) → Gateway → Auth 서버: `401 Unauthorized` → 클라이언트: Refresh Token도 만료됨을 감지
        * 사용자 개입: 로그인 화면 표시, 이메일/비밀번호 입력
        * `POST /api/v1/auth/login` (사용자 입력 후 요청) → Gateway → Auth 서버: `200 OK` (새 토큰 발급)
        * Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
  - **연결 풀 설정 패턴**:
    * Reactor Netty 연결 풀 설정 (`spring.cloud.gateway.httpclient.pool.*`)
    * Connection reset by peer 방지 설정 (`max-idle-time: 30초`, `max-life-time: 300초`)
    * 타임아웃 설정 (`connection-timeout: 30초`, `response-timeout: 60초`)
  - **CORS 설정 패턴**:
    * Global CORS 설정 (`spring.cloud.gateway.globalcors.*`)
    * 환경별 CORS 정책 차별화 (Local/Dev: 개발 편의성, Beta/Prod: 보안 우선)
    * 외부 API 연동 시 중복 헤더 제거 (`DedupeResponseHeader` 필터)
  - **에러 처리 Tasks** 관련 추가 고려사항:
  - **예외 처리 전략**:
    * Gateway Filter에서 발생하는 예외 처리: `handleUnauthorized` 메서드로 401 응답 반환 (`ServerHttpResponse.setStatusCode(HttpStatus.UNAUTHORIZED)`)
    * Global 예외 처리: 
      - 설계서에는 `ApiGatewayExceptionHandler`가 명시되어 있으나, Spring Cloud Gateway는 Reactive 기반이므로 `@ControllerAdvice` 사용 불가
      - `ErrorWebExceptionHandler` 구현 또는 `GlobalFilter` 활용하여 예외 처리
      - 설계서의 `ApiGatewayExceptionHandler`는 참고용이며, 실제 구현은 Reactive 기반으로 수행
    * 공통 에러 응답 형식: `ApiResponse` 형식 (JSON 응답, `ServerHttpResponse`에 직접 작성)
    * HTTP 상태 코드 매핑 (401: 인증 실패, 404: 라우팅 실패, 502: 백엔드 연결 실패, 504: 백엔드 타임아웃, 500: 내부 서버 오류)
  - **로깅 전략**:
    * 환경별 로그 레벨 (Local/Dev: DEBUG, Beta: INFO, Prod: WARN)
    * 요청 로깅 (URI, HTTP 메서드, 헤더 - 민감 정보 제외)
    * 인증 로깅 (인증 성공/실패, JWT 토큰 검증 결과)
    * 라우팅 로깅 (라우팅 규칙 매칭 결과, 백엔드 서버 URL)
    * 에러 로깅 (에러 발생 시 상세 스택 트레이스, 에러 코드 및 메시지)

**6단계: Preliminary Solution Output (초기 설계 솔루션 출력)**
- **초기 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "초기 설계 솔루션" 작성
  - **복잡한 문제의 경우**: `process_thought` 도구를 활용하여 단계별로 사고하고 정리
    * 문제가 복잡하거나 여러 설계 선택지가 있는 경우 `process_thought` 도구를 사용하여 체계적으로 사고
    * 단계별로 가정, 검증, 조정 과정을 거쳐 최적의 설계 솔루션 도출
  - **사실(facts)**과 **추론(inferences)** 명확히 구분:
    * **사실**: 설계서(`docs/step17/gateway-design.md`), 기존 코드(`api/gateway`, `common-security`), 공식 문서에서 확인한 내용
    * **추론**: 사실을 바탕으로 한 설계 선택 및 근거
  - **모호한 표현 금지**: 최종 결과물 수준으로 구체적으로 작성
    * "~할 수 있다", "~가 좋을 것 같다" 같은 모호한 표현 사용 금지
    * 구체적인 클래스명, 메서드명, 패키지 구조를 명시
  - **기존 아키텍처 패턴과의 일관성 보장**:
    * `api/gateway` 모듈의 기존 구조를 어떻게 확장할지 명시
    * `common-security` 모듈의 `JwtTokenProvider`를 어떻게 활용할지 명시
    * 설계서의 패키지 구조 및 계층 구조를 어떻게 준수할지 명시
  - **기존 컴포넌트 재사용 방법 설명**:
    * `common-security` 모듈의 `JwtTokenProvider` 재사용 방법
    * `common-core` 모듈의 `ApiResponse` 재사용 방법
    * 기존 코드 스타일 및 명명 규칙 준수
- **초기 설계 솔루션에 포함할 내용** (gateway-implementation-plan.md 구조 참고):
  - **설계서 기반 구현 계획**:
    * `docs/step17/gateway-design.md`의 패키지 구조를 정확히 따름 (`config/`, `filter/`, `common/exception/`, `util/`)
    * `docs/step17/gateway-implementation-plan.md`의 초기 설계 솔루션 구조 참고
    * 설계서에 명시된 클래스, 메서드, 설정을 정확히 구현
    * 설계서에 없는 기능은 추가하지 않음 (오버엔지니어링 금지)
  - **패키지 구조**: 설계서에 명시된 패키지 구조를 정확히 따름
    * `api/gateway/src/main/java/com/ebson/shrimp/tm/demo/api/gateway/`
    * `config/` (GatewayConfig.java, CorsConfig.java 선택)
    * `filter/` (JwtAuthenticationGatewayFilter.java, RequestLoggingFilter.java 선택)
    * `common/exception/` (ApiGatewayExceptionHandler.java - ErrorWebExceptionHandler 구현)
    * `util/` (HeaderUtils.java 선택)
  - **라우팅 설정 설계**:
    * 5개 API 서버에 대한 라우팅 규칙 설계 (auth, archive, contest, news, chatbot)
    * Path 기반 라우팅 (`Path=/api/v1/auth/**` 등)
    * 환경별 백엔드 서비스 URL 설정 (Local, Dev, Beta, Prod)
  - **JWT 인증 필터 설계**:
    * `JwtAuthenticationGatewayFilter` 클래스 전체 구조 설계 (Reactive 기반, `GatewayFilter` 인터페이스 구현)
    * 인증 필요/불필요 경로 구분 로직 설계 (`isPublicPath` 메서드)
    * JWT 토큰 추출 로직 설계 (`extractToken` 메서드, `Authorization: Bearer {token}`)
    * JWT 토큰 검증 로직 설계 (`JwtTokenProvider.validateToken` 활용, 블로킹 호출 허용)
    * **토큰 만료/무효 시 처리 로직 설계**:
      - 토큰이 없거나 만료/무효한 경우: `handleUnauthorized` 메서드로 401 Unauthorized 반환
      - 응답 형식: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
      - 인증 서버를 자동으로 호출하지 않음 (Gateway는 검증만 수행)
      - **토큰 갱신 흐름 (사용자 응답 기준)**:
        * **시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)**
          - 특징: 사용자 개입 없이 클라이언트(프론트엔드/앱)가 자동으로 처리
          - Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` (`{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`) → 클라이언트: 401 응답 감지
          - `POST /api/v1/auth/refresh` (유효한 Refresh Token, 자동 요청) → Gateway → Auth 서버: `200 OK` (새 Access Token + Refresh Token 발급)
          - Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
        * **시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)**
          - 특징: 클라이언트가 자동 처리하다가 Refresh Token 만료 시 사용자 개입 필요
          - Archive 요청 (만료된 토큰) → Gateway: `401 Unauthorized` → 클라이언트: 401 응답 감지
          - `POST /api/v1/auth/refresh` (만료된/없는 Refresh Token, 자동 시도) → Gateway → Auth 서버: `401 Unauthorized` (`{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "유효하지 않은 Refresh Token입니다."}}`) → 클라이언트: Refresh Token도 만료됨을 감지
          - 사용자 개입: 로그인 화면 표시, 이메일/비밀번호 입력
          - `POST /api/v1/auth/login` (사용자 입력 후 요청) → Gateway → Auth 서버: `200 OK` (새 Access Token + Refresh Token 발급)
          - Archive 요청 (새 토큰, 자동 재시도) → Gateway: JWT 검증 성공 → Archive 서버: `200 OK`
    * 사용자 정보 추출 및 헤더 주입 로직 설계 (`JwtTokenProvider.getPayloadFromToken` 활용, `x-user-id`, `x-user-email`, `x-user-role` 헤더 주입)
    * Route 설정에서 필터 등록 방법 설계 (`GatewayConfig`에서 Bean 등록 또는 `application.yml`에서 필터 설정)
  - **연결 풀 설정 설계**:
    * HTTP 클라이언트 연결 풀 설정 값 설계 (`max-idle-time: 30초`, `max-life-time: 300초` 등)
    * Connection reset by peer 방지 전략 설계
    * 타임아웃 설정 설계 (`connection-timeout: 30초`, `response-timeout: 60초`)
  - **CORS 설정 설계**:
    * Global CORS 설정 설계
    * 환경별 CORS 정책 설계 (Local/Dev: 개발 편의성, Beta/Prod: 보안 우선)
    * 외부 API 연동 시 중복 헤더 제거 필터 설계 (`DedupeResponseHeader`)
  - **에러 처리 설계**:
    * Gateway Filter에서 예외 처리 설계 (`handleUnauthorized` 메서드로 401 응답 반환)
    * Global 예외 처리 설계: 
      - 설계서에는 `ApiGatewayExceptionHandler`가 명시되어 있으나, Spring Cloud Gateway는 Reactive 기반이므로 `@ControllerAdvice` 사용 불가
      - `ErrorWebExceptionHandler` 구현 또는 `GlobalFilter` 활용하여 예외 처리
      - 설계서의 `ApiGatewayExceptionHandler`는 참고용이며, 실제 구현은 Reactive 기반으로 수행
    * 공통 에러 응답 형식 설계 (`ApiResponse` 형식, JSON 응답)
    * HTTP 상태 코드 매핑 설계 (401: 인증 실패, 404: 라우팅 실패, 502: 백엔드 연결 실패, 504: 백엔드 타임아웃, 500: 내부 서버 오류)
    * 로깅 전략 설계 (환경별 로그 레벨: Local/Dev: DEBUG, Beta: INFO, Prod: WARN)
  - **설정 파일 설계**:
    * `application.yml` 기본 설정 설계 (라우팅, 연결 풀, CORS 설정 포함)
    * 환경별 설정 파일 설계 (`application-local.yml`, `application-dev.yml`, `application-beta.yml`, `application-prod.yml`)
    * 라우팅 설정: 5개 Route 정의 (auth, archive, contest, news, chatbot)
    * 연결 풀 설정: `spring.cloud.gateway.httpclient.pool.*` 설정값 포함
    * CORS 설정: `spring.cloud.gateway.globalcors.*` 설정값 포함
    * JWT 설정: `jwt.*` 설정값 포함 (common-security 모듈 사용)
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구(`read_file`, `codebase_search`, `web_search` 등)를 활용하여 실제 정보를 수집해야 함
  - 추적 가능한 출처가 없는 정보는 사용하지 않음
  - 공식 문서만 참고 (Spring Cloud Gateway, Reactor Netty, Spring Boot)
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```
  - **주의**: `analyze_task` 도구를 호출하지 않으면 작업이 완료되지 않음
  - **중요**: 이 단계에서 실제 코드 구현은 수행하지 않음. 구현은 `execute_task` 단계에서 `implementationGuide`를 따라 수행됨

**검증 기준**:
- [ ] 설계서(`docs/step17/gateway-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] 라우팅 설정 설계 완성:
  - 5개 API 서버에 대한 라우팅 규칙 설계 (auth, archive, contest, news, chatbot)
  - Path 기반 라우팅 설계 (`Path=/api/v1/{service}/**`)
  - 환경별 백엔드 서비스 URL 설정 설계 (Local: localhost, Dev/Beta/Prod: service-name)
- [ ] JWT 인증 필터 설계 완성:
  - `JwtAuthenticationGatewayFilter` 클래스 전체 구조 설계 (Reactive 기반, `GatewayFilter` 인터페이스 구현)
  - 인증 필요/불필요 경로 구분 로직 설계 (`isPublicPath` 메서드)
  - JWT 토큰 추출 로직 설계 (`Authorization: Bearer {token}`)
  - JWT 토큰 검증 로직 설계 (`JwtTokenProvider.validateToken` 활용)
  - 토큰 만료/무효 시 처리 로직 설계:
    * `handleUnauthorized` 메서드로 401 Unauthorized 반환
    * 응답 형식: `{"code": "4001", "messageCode": {"code": "AUTH_FAILED", "text": "인증에 실패했습니다."}}`
    * 인증 서버를 자동으로 호출하지 않음 (Gateway는 검증만 수행)
    * 토큰 갱신 흐름 (사용자 응답 기준):
      - 시나리오 1: Access Token 만료 (사용자 개입 없음, 클라이언트 자동 처리)
        * `401 Unauthorized` → 클라이언트: 401 응답 감지 → `POST /api/v1/auth/refresh` (유효한 Refresh Token, 자동 요청) → `200 OK` (새 토큰) → 원래 요청 자동 재시도 → `200 OK`
      - 시나리오 2: Refresh Token도 만료/없음 (사용자 개입 필요)
        * `401 Unauthorized` → 클라이언트: 401 응답 감지 → `POST /api/v1/auth/refresh` (만료된/없는 Refresh Token, 자동 시도) → `401 Unauthorized` → 클라이언트: Refresh Token도 만료됨을 감지 → 사용자 개입 (로그인 화면 표시, 이메일/비밀번호 입력) → `POST /api/v1/auth/login` (사용자 입력 후 요청) → `200 OK` (새 토큰) → 원래 요청 자동 재시도 → `200 OK`
  - 사용자 정보 헤더 주입 로직 설계 (`x-user-id`, `x-user-email`, `x-user-role`)
- [ ] 연결 풀 설정 설계 완성:
  - HTTP 클라이언트 연결 풀 설정 값 설계 (`max-idle-time: 30000`, `max-life-time: 300000` 등)
  - Connection reset by peer 방지 전략 설계 (백엔드 keep-alive 60초보다 짧게 설정)
  - 타임아웃 설정 설계 (`connection-timeout: 30000`, `response-timeout: 60000`)
- [ ] CORS 설정 설계 완성:
  - Global CORS 설정 설계 (`spring.cloud.gateway.globalcors.*`)
  - 환경별 CORS 정책 설계 (Local/Dev: 개발 편의성, Beta/Prod: 보안 우선)
  - 외부 API 연동 시 중복 헤더 제거 필터 설계 (`DedupeResponseHeader=Access-Control-Allow-Origin, RETAIN_LAST`)
- [ ] 에러 처리 설계 완성:
  - Gateway Filter에서 예외 처리 설계 (`handleUnauthorized` 메서드로 401 응답 반환)
  - Global 예외 처리 설계:
    - 설계서에는 `ApiGatewayExceptionHandler`가 명시되어 있으나, Spring Cloud Gateway는 Reactive 기반이므로 `@ControllerAdvice` 사용 불가
    - `ErrorWebExceptionHandler` 구현 또는 `GlobalFilter` 활용하여 예외 처리
    - 설계서의 `ApiGatewayExceptionHandler`는 참고용이며, 실제 구현은 Reactive 기반으로 수행
  - 공통 에러 응답 형식 설계 (`ApiResponse` 형식, JSON 응답, HTTP 상태 코드 매핑: 401, 404, 502, 504, 500)
  - 로깅 전략 설계 (환경별 로그 레벨: Local/Dev: DEBUG, Beta: INFO, Prod: WARN)
- [ ] 설정 파일 설계 완성:
  - `application.yml` 기본 설정 설계 (라우팅, 연결 풀, CORS 설정 포함)
  - 환경별 설정 파일 설계 (`application-local.yml`, `application-dev.yml`, `application-beta.yml`, `application-prod.yml`)
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음 (Rate Limiting, Circuit Breaker 등은 명시적 요구 시에만)
- [ ] 공식 문서만 참고: Spring Cloud Gateway, Reactor Netty, Spring Boot 공식 문서
- [ ] **빌드 검증**: `api/gateway` 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-gateway:build` 명령이 성공해야 함)
- [ ] **빌드 검증**: 루트 프로젝트에서 전체 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
- [ ] **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
- [ ] `analyze_task` 도구 호출 완료: summary와 initialConcept를 포함하여 호출

참고 파일: 
- **설계서 (필수)**:
  - `docs/step17/gateway-design.md`: API Gateway 설계서 (필수, 모든 설계 사항 엄격 준수)
  - `docs/step17/gateway-implementation-plan.md`: API Gateway 구현 계획서 (필수, 6단계 프로세스 및 초기 설계 솔루션 참고)
- **참고 문서**:
  - `api/gateway/REF.md`: Saturn Gateway Server 아키텍처 참고 (연결 풀 설정, CORS 설정 가이드라인)
- **프로젝트 설계 문서**:
  - `docs/step2/1. api-endpoint-design.md`: API 엔드포인트 설계 (라우팅 규칙 설계 참고)
  - `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드 (JWT 인증 패턴 참고)
  - `docs/step9/contest-news-api-design.md`: Contest/News API 설계 (기존 API 모듈 구조 참고)
  - `docs/step12/rag-chatbot-design.md`: Chatbot API 설계 (기존 API 모듈 구조 참고)
  - `docs/step13/user-archive-feature-design.md`: Archive API 설계 (기존 API 모듈 구조 참고)
- **공식 문서**:
  - Spring Cloud Gateway 공식 문서: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/
  - Reactor Netty 공식 문서: https://projectreactor.io/docs/netty/release/reference/index.html
  - Spring Boot 공식 문서: https://docs.spring.io/spring-boot/docs/current/reference/html/
- **공식 가이드**:
  - Shrimp Task Manager 공식 가이드: http://localhost:9998/?template-view=preview&template-id=planTask#templates
```

### 15단계: Sources 동기화 Batch Job 구현

**단계 번호**: 15단계
**의존성**: 4단계 (Domain 모듈 구현 완료 필수 - SourcesDocument, MongoTemplate)
**다음 단계**: 16단계 (Batch 모듈 및 Jenkins 연동 구현)

```
plan task: Sources 동기화 Batch Job 구현 - json/sources.json 데이터를 MongoDB Atlas sources 컬렉션으로 동기화

---

## Task Description

json/sources.json 파일에 정의된 모든 Source 데이터를 MongoDB Atlas Cluster의 `sources` 컬렉션으로 동기화하는 Spring Batch Job을 구현합니다.

**목표**: JSON 파일 읽기 → DTO 변환 → MongoDB UPSERT (name 기준)
**예상 결과**: sources.sync.job 실행 시 약 20건의 Source 데이터가 MongoDB에 저장됨

---

## Task Requirements and Constraints

### 필수 요구사항
1. 기존 batch/source 모듈의 Job 패턴 준수 (NewsGoogleDevelopersRssParserJobConfig 참조)
2. domain/mongodb 모듈의 SourcesDocument 재사용
3. MongoTemplate 직접 사용 (내부 API 호출 금지, Feign 클라이언트 사용 금지)
4. UPSERT 전략: name 필드 기준으로 존재하면 UPDATE, 없으면 INSERT

### 제약사항
1. 오버엔지니어링 금지 (복잡한 재시도 로직, 분산 처리 배제)
2. 추가 인덱스 생성 금지 (기존 name UNIQUE 인덱스 활용)
3. 기존 SourcesDocument, SourcesRepository 수정 금지

### 공식 문서 참조
- Spring Batch: https://docs.spring.io/spring-batch/reference/
- Spring Data MongoDB: https://docs.spring.io/spring-data/mongodb/reference/
- MongoDB Java Driver: https://www.mongodb.com/docs/drivers/java/sync/current/

---

## Reference Files (Must Read Before Implementation)

| 파일 유형 | 경로 | 용도 |
|----------|------|------|
| 설계서 | docs/step2/sources-sync-batch-job-design.md | 전체 설계 참조 |
| 프롬프트 | prompts/sources-sync-batch-job-design-prompt.md | 설계 요구사항 |
| MongoDB 스키마 | docs/step1/2. mongodb-schema-design.md | SourcesDocument 스키마 |
| Document 클래스 | domain/mongodb/.../document/SourcesDocument.java | 필드 구조 확인 |
| Constants | batch/source/.../common/Constants.java | 상수 패턴 확인 |
| Job 패턴 | batch/source/.../news/googledevelopers/jobconfig/NewsGoogleDevelopersRssParserJobConfig.java | Job 구성 패턴 |
| Reader 패턴 | batch/source/.../news/googledevelopers/reader/GoogleDevelopersRssItemReader.java | Reader 구현 패턴 |
| 입력 데이터 | json/sources.json | 동기화 대상 데이터 |

---

## Task List

### Task 1: Constants.java에 Job 상수 추가

**Description:** batch/source 모듈의 Constants 클래스에 SOURCES_SYNC Job 상수를 추가합니다.

**Notes:** 기존 상수 네이밍 컨벤션 준수 (예: NEWS_GOOGLE_DEVELOPERS)

**Implementation Guide:**
- 파일: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/common/Constants.java
- 추가 내용: `public final static String SOURCES_SYNC = "sources.sync.job";`

**Verification Criteria:**
- 컴파일 오류 없음
- 기존 Constants 클래스 구조 유지

**Dependencies:** 없음

---

### Task 2: SourceJsonDto 클래스 생성

**Description:** json/sources.json 파싱용 DTO 클래스를 생성합니다. Jackson의 SnakeCaseStrategy를 사용하여 JSON 필드명을 자동 매핑합니다.

**Notes:** 
- JSON 필드명은 snake_case (예: api_endpoint)
- Java 필드명은 camelCase (예: apiEndpoint)
- @JsonNaming 어노테이션으로 자동 변환

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/dto/
- 파일명: SourceJsonDto.java
- 어노테이션: @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder, @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
- 필드: name, type, category, url, apiEndpoint, rssFeedUrl, description, priority, reliabilityScore, accessibilityScore, dataQualityScore, legalEthicalScore, totalScore, authenticationRequired, authenticationMethod, rateLimit, documentationUrl, updateFrequency, dataFormat

**Verification Criteria:**
- 컴파일 오류 없음
- json/sources.json의 source 객체와 필드 1:1 매핑

**Dependencies:** 없음

---

### Task 3: SourcesSyncIncrementer 클래스 생성

**Description:** Job 실행 시 run.id 및 baseDate 파라미터를 관리하는 Incrementer를 생성합니다.

**Notes:** NewsGoogleDevelopersIncrementer 패턴 준수

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/incrementer/
- 파일명: SourcesSyncIncrementer.java
- extends: RunIdIncrementer
- 참고: batch/source/.../incrementer/NewsGoogleDevelopersIncrementer.java

**Verification Criteria:**
- 컴파일 오류 없음
- getNext() 메서드가 run.id와 baseDate 파라미터 반환

**Dependencies:** 없음

---

### Task 4: SourcesJsonItemReader 클래스 생성

**Description:** json/sources.json 파일을 읽고 categories 배열을 평탄화하여 SourceJsonDto 리스트로 변환하는 Reader를 생성합니다.

**Notes:** 
- GoogleDevelopersRssItemReader 패턴 준수
- categories 배열 순회 → 각 category의 sources 추출 → source에 category 필드 매핑

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/reader/
- 파일명: SourcesJsonItemReader.java
- extends: AbstractPagingItemReader<SourceJsonDto>
- 주요 메서드:
  * doOpen(): JSON 파일 로드, ObjectMapper로 파싱, 평탄화
  * doReadPage(): 페이지 단위로 SourceJsonDto 반환
  * doClose(): 리소스 정리

**Verification Criteria:**
- 컴파일 오류 없음
- json/sources.json의 모든 source 항목이 읽힘 (약 20건)
- 각 source에 상위 category 필드가 매핑됨

**Dependencies:** Task 2 (SourceJsonDto)

---

### Task 5: SourcesSyncProcessor 클래스 생성

**Description:** SourceJsonDto를 SourcesDocument로 변환하고 기본값을 설정하는 Processor를 생성합니다.

**Notes:** 
- 필수 필드 검증: name, type, category가 null/빈 문자열이면 null 반환 (Skip)
- 기본값 설정: enabled = true, 감사 필드 설정

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/processor/
- 파일명: SourcesSyncProcessor.java
- implements: ItemProcessor<SourceJsonDto, SourcesDocument>
- 어노테이션: @Slf4j, @StepScope
- 변환 로직:
  * 필수 필드 검증
  * enabled = true
  * createdAt/updatedAt = LocalDateTime.now()
  * createdBy/updatedBy = "batch-system"
  * 필드 복사: SourceJsonDto → SourcesDocument

**Verification Criteria:**
- 컴파일 오류 없음
- 필수 필드 누락 시 null 반환 및 WARN 로그 출력
- 모든 필드가 정상적으로 매핑됨

**Dependencies:** Task 2 (SourceJsonDto)

---

### Task 6: SourcesMongoWriter 클래스 생성

**Description:** MongoDB에 UPSERT를 수행하는 Writer를 생성합니다. name 필드 기준으로 존재하면 UPDATE, 없으면 INSERT합니다.

**Notes:** 
- MongoTemplate.upsert() 사용
- Repository나 Feign 클라이언트 사용 금지

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/writer/
- 파일명: SourcesMongoWriter.java
- implements: ItemWriter<SourcesDocument>
- 어노테이션: @Slf4j, @StepScope, @RequiredArgsConstructor
- 의존성: MongoTemplate (생성자 주입)
- UPSERT 로직:
  * Query: Criteria.where("name").is(document.getName())
  * Update: 모든 필드 set, createdAt/createdBy는 setOnInsert
  * 호출: mongoTemplate.upsert(query, update, SourcesDocument.class)

**Verification Criteria:**
- 컴파일 오류 없음
- 신규 데이터 INSERT, 기존 데이터 UPDATE 정상 동작
- createdAt/createdBy는 INSERT 시에만 설정됨

**Dependencies:** 없음

---

### Task 7: SourcesSyncJobConfig 클래스 생성

**Description:** Job, Step, Reader, Processor, Writer Bean을 정의하는 Job Configuration 클래스를 생성합니다.

**Notes:** NewsGoogleDevelopersRssParserJobConfig 패턴 준수

**Implementation Guide:**
- 패키지: batch/source/src/main/java/com/ebson/shrimp/tm/demo/batch/source/domain/sources/sync/jobconfig/
- 파일명: SourcesSyncJobConfig.java
- 어노테이션: @Slf4j, @Configuration, @RequiredArgsConstructor
- 의존성: MongoTemplate
- Job 구성:
  * Job 이름: Constants.SOURCES_SYNC ("sources.sync.job")
  * Step: 단일 Step (step1)
  * Incrementer: SourcesSyncIncrementer
- Step 구성:
  * Chunk Size: Constants.CHUNK_SIZE_10 (10)
  * Reader: SourcesJsonItemReader (@StepScope)
  * Processor: SourcesSyncProcessor (@StepScope)
  * Writer: SourcesMongoWriter (@StepScope)
- Bean 이름 규칙: Constants.SOURCES_SYNC + Constants.STEP_1, Constants.SOURCES_SYNC + Constants.ITEM_READER 등

**Verification Criteria:**
- 컴파일 오류 없음
- `./gradlew :batch-source:build` 성공
- Bean 이름이 기존 패턴과 일관됨

**Dependencies:** Task 1, Task 3, Task 4, Task 5, Task 6

---

## Final Verification Criteria (전체 검증 기준)

### 빌드 검증
| 검증 항목 | 명령어/조건 | 예상 결과 |
|----------|------------|----------|
| Gradle 빌드 | `./gradlew :batch-source:build` | BUILD SUCCESSFUL |
| 컴파일 | 모든 Java 파일 | 에러 없음 |

### 기능 검증
| 검증 항목 | 실행 방법 | 예상 결과 |
|----------|----------|----------|
| Job 실행 | `java -jar batch-source.jar --spring.batch.job.names=sources.sync.job --baseDate=2026-01-20` | Job 상태 COMPLETED |
| 처리 건수 | MongoDB sources 컬렉션 조회 | 약 20건 저장 |
| 중복 방지 | name 필드 기준 | 중복 문서 없음 |
| 감사 필드 | createdAt, updatedAt, createdBy, updatedBy | 모든 필드 정상 설정 |
| enabled 필드 | 모든 문서 | true 설정 |
| UPSERT | 동일 Job 2회 실행 | 1회차 INSERT, 2회차 UPDATE |

---

## Critical Warning (중요 경고)

**가정, 추측, 상상 금지**: 모든 정보는 실제 코드베이스에서 확인해야 합니다.
- 기존 코드 패턴 확인: read_file, codebase_search 도구 활용
- 불확실한 기술: web_search 도구로 공식 문서 확인
- 추측 금지: 모든 정보는 추적 가능한 출처 필수
```

### 16단계: 이메일 인증 기능 구현 (api/auth 모듈)

**단계 번호**: 16단계
**의존성**: 5단계 (사용자 인증 및 관리 시스템 구현 완료 필수), 4단계 (Domain 모듈 구현 완료 필수)
**다음 단계**: 17단계 (Batch 모듈 및 Jenkins 연동)

```
plan task: api/auth 모듈 이메일 인증 기능 구현 - client/mail 모듈 생성 및 통합 

## Task Analysis

You must complete the following sub-steps in sequence, and at the end call the `analyze_task` tool to pass the preliminary design solution to the next stage.

### 1. Analysis Purpose

**Task Description**:
현재 api/auth 모듈의 회원가입 플로우에서 이메일 인증 토큰이 DB에 저장되지만 실제 이메일이 발송되지 않는 문제를 해결합니다.
Spring Mail (JavaMailSender)을 사용하여 이메일 발송 기능을 구현하고, Thymeleaf 템플릿으로 이메일 본문을 생성합니다.

**Task Requirements and Constraints**:
- **핵심 제약사항 (절대 준수 필수)**:
  1. **공식 개발문서만 참고**: Spring Boot Mail, Thymeleaf 공식 문서만 참고
  2. **오버엔지니어링 절대 금지**: 현재 요구사항(회원가입 인증, 비밀번호 재설정)에 필요한 기능만 구현
  3. **클린코드 및 SOLID 원칙 준수**: 단일 책임 원칙, 의존성 역전 원칙 준수
  4. **기존 패턴 일관성**: client/slack 모듈의 구조 패턴 완전히 준수
  5. **보안 필수 사항**:
     - 코드에 비밀번호 절대 하드코딩 금지
     - 환경 변수 또는 설정 파일의 플레이스홀더 사용
     - .gitignore에 민감 정보 파일 패턴 추가
     - 프로덕션 환경은 AWS Secrets Manager 사용 (설계서 참조)

**Confirm**:
- **Task objectives**: 이메일 발송 기능 완전 구현 및 기존 인증 플로우와 통합
- **Expected outcomes**: 회원가입 시 실제 이메일 발송, 비밀번호 재설정 이메일 발송
- **Technical challenges**: 비동기 이메일 발송, 트랜잭션 분리, 환경별 SMTP 설정
- **Integration requirements**: 기존 EmailVerificationService와의 통합, client/slack 패턴 일관성 유지

### 2. Identify Project Architecture

**View key configuration files and structures**:
- **필수 확인 파일**:
  - `settings.gradle`: 신규 client/mail 모듈 등록 위치 확인
  - `client/slack/build.gradle`: 모듈 의존성 패턴 참조
  - `client/slack/src/main/java/.../config/`: 설정 클래스 패턴 참조
  - `api/auth/src/main/resources/application*.yml`: 기존 설정 구조 확인
  - `shrimp-rules.md`: 프로젝트 규칙 (존재 시 필수 참조)

**Identify architectural patterns**:
- **멀티모듈 구조**: Spring Boot 기반 MSA 패턴
- **계층 구조**: client (인프라), api (프레젠테이션), domain (도메인)
- **비동기 패턴**: ThreadPoolTaskExecutor 기반 비동기 처리
- **설정 관리**: Spring Boot Auto-configuration + @ConfigurationProperties

**Analyze core components**:
- **client/slack 모듈**: 외부 서비스 통합 참조 패턴
  - SlackProperties: 설정 관리
  - SlackConfig: Bean 설정
  - SlackClient: 실제 통신 로직
- **api/auth/service/EmailVerificationService**: 이메일 발송 로직 통합 대상
- **common-core, common-exception**: 공통 모듈 의존성

**Document existing patterns**:
- **모듈 생성 패턴**: settings.gradle 등록 → build.gradle 작성 → 소스 구현
- **설정 클래스 패턴**: @ConfigurationProperties + @Configuration
- **서비스 인터페이스 패턴**: 인터페이스 정의 → 구현체 분리

### 3. Collect Information

**Required Reference Files** (반드시 읽고 참조):
- `docs/step19/email-verification-implementation-design.md` (이메일 인증 설계서)
  * 2장: Quick Start - 로컬 Gmail SMTP 설정 (필수)
  * 6장: 구현 가이드
  * 7장: 이메일 템플릿 설계
  * 10장: 설정 가이드
- `docs/step6/spring-security-auth-design-guide.md` (인증 설계 가이드)

**If there is any uncertainty, must do one of the following**:
- Ask the user for clarification
- Use `read_file`, `codebase_search` to query existing implementation patterns
- Use `web_search` to query Spring Boot Mail official documentation
- **Prohibited**: Speculation without sources; all information must have traceable sources

**Pre-configured Information** (사전 준비 완료):
- Gmail 계정: ebson024.v1@gmail.com
- 앱 비밀번호: rdxz emha tprw llck (공백 제거: rdxzemhatprwllck)
- App Name: Local-SMTP
- Quick Start: docs/step19/email-verification-implementation-design.md 2장 참조

**Module Current State** (모듈 현재 상태):
- `client/mail/build.gradle` 파일 존재 (초기화됨)
- **주의**: 현재 build.gradle에 `openfeign` 의존성이 있으면 삭제 필요
- `client/mail/src/` 폴더가 없으면 생성 필요 (settings.gradle 자동 탐색 조건)
- 필수 확인: `./gradlew projects | grep client-mail` 으로 모듈 인식 여부 확인

### 4. Check Existing Programs and Structures

**Use precise search strategies**:
- **client/slack 모듈 분석** (필수 - 패턴 참조):
  - `client/slack/build.gradle`: 의존성 패턴
  - `client/slack/src/main/java/.../config/SlackProperties.java`: 설정 클래스 패턴
  - `client/slack/src/main/java/.../config/SlackConfig.java`: Bean 설정 패턴
  - `client/slack/src/main/java/.../domain/slack/client/SlackClient.java`: 클라이언트 구현 패턴
  - `client/slack/src/main/java/.../domain/slack/service/SlackNotificationService.java`: 서비스 인터페이스 패턴
  - `client/slack/src/main/java/.../exception/SlackException.java`: 예외 클래스 패턴
  - **디렉토리 구조**: config/, domain/slack/, exception/, util/
- **api/auth 모듈 확인**:
  - `api/auth/src/main/java/.../service/EmailVerificationService.java`: 통합 대상 서비스
  - `api/auth/src/main/resources/application-local.yml`: 환경별 설정 패턴
  - `api/auth/build.gradle`: 모듈 의존성 추가 위치

**Analyze code style and conventions**:
- **Naming conventions**: camelCase (Java), kebab-case (YAML)
- **Package structure**: com.tech.n.ai.client.{module}
- **Comment styles**: JavaDoc for public APIs
- **Error handling**: try-catch with logging, custom exceptions in common-exception

**Record and follow discovered patterns**:
- 모든 client 모듈은 동일한 구조 패턴 준수
- @ConfigurationProperties로 외부 설정 바인딩
- 인터페이스/구현체 분리로 테스트 용이성 확보

**Determine overlap with existing functionality**:
- 이메일 발송 기능은 신규 구현
- EmailVerificationService는 수정 (이메일 발송 로직 추가)
- 기존 인증 토큰 생성 로직은 재사용

### 5. Task Type-Specific Guidelines

**Backend Service Integration Task**:
- **Check service integration patterns**:
  - client 모듈의 외부 서비스 통합 패턴 (client/slack, client/rss)
  - 비동기 처리 패턴 (@Async 또는 ThreadPoolTaskExecutor)
  - 설정 관리 패턴 (환경별 application*.yml)
- **Analyze error handling**:
  - 이메일 발송 실패 시 회원가입 트랜잭션과 분리 (Fail-Safe)
  - MailException 처리 → 로깅만 수행, 예외 던지지 않음
- **Confirm configuration patterns**:
  - Spring Boot Auto-configuration 활용 (JavaMailSender)
  - 환경 변수 우선순위: 환경 변수 > application-{profile}.yml > application.yml
- **Understand security practices**:
  - .gitignore에 민감 정보 파일 패턴 추가
  - 코드에 비밀번호 하드코딩 금지
  - 프로덕션은 AWS Secrets Manager 사용

### 6. Preliminary Solution Output

**Initial Implementation Approach**:
1. client/mail 모듈 생성 (client/slack 모듈 패턴 참조)
2. Spring Boot Starter Mail 의존성 추가
3. EmailSender 인터페이스 및 SmtpEmailSender 구현
4. Thymeleaf 기반 이메일 템플릿 작성
5. 기존 EmailVerificationService에 이메일 발송 로직 통합
6. 환경 변수 기반 설정 (로컬: Gmail SMTP 또는 MailHog)

**Based on the above, write a "Preliminary Design Solution"**:
- **Facts** (sources):
  - client/slack 모듈 패턴 확인 완료
  - EmailVerificationService 현재 구조 확인 완료
  - Spring Boot Mail 공식 문서 확인 완료
- **Inferences** (selection basis):
  - 비동기 발송으로 트랜잭션 분리 필요 (회원가입 실패 방지)
  - Thymeleaf 템플릿 엔진 선택 (Spring Boot 기본 지원, 프로젝트 표준)
  - Fail-Safe 패턴 적용 (이메일 발송 실패해도 회원가입은 성공)

**Call tool**:
```
analyze_task({ 
  summary: "api/auth 모듈 이메일 인증 기능 구현 - client/mail 모듈 생성 및 통합",
  initialConcept: "client/slack 패턴을 따라 client/mail 모듈 생성, Spring Boot Starter Mail + Thymeleaf로 이메일 발송 기능 구현, EmailVerificationService에 비동기 발송 로직 통합"
})
```

**Critical Warning**: All forms of `assumptions`, `guesses`, and `imagination` are strictly prohibited. You must use every `available tool` at your disposal to `gather real information`.

**Now start calling `analyze_task`, strictly forbidden not to call the tool**

---

## Implementation Details (작업 상세 내용)

### Implementation Task Breakdown (작업 내용)

이 섹션은 `split_tasks` 단계에서 참조할 상세 구현 가이드입니다.

#### Task 1: client/mail 모듈 생성
   
**Task 1.1: 모듈 디렉토리 구조 생성**
   - 역할: 이메일 클라이언트 모듈 생성
   - 책임: 독립적인 이메일 발송 기능 제공
   - **중요**: settings.gradle 자동 탐색을 위해 `src` 폴더가 반드시 존재해야 함
   - 패키지 구조: com.tech.n.ai.client.mail
   - 디렉토리 구조 (client/slack 참조):
     ```
     client/mail/
     ├── build.gradle
     └── src/main/
         ├── java/com/ebson/shrimp/tm/demo/client/mail/
         │   ├── config/                 # MailConfig, MailProperties
         │   ├── domain/mail/            # dto/, service/, template/
         │   └── exception/              # EmailSendException
         └── resources/templates/email/  # Thymeleaf 템플릿
     ```
   - 검증 기준: 
     * `./gradlew projects | grep client-mail` 출력 확인
     * client/slack 모듈과 동일한 구조

**Task 1.2: build.gradle 수정**
   - 역할: 모듈 의존성 관리
   - 책임: Spring Mail, Thymeleaf 의존성 추가
   - **현재 상태 주의**: 초기화된 build.gradle에 openfeign 의존성이 있으면 삭제 필요
   - 참고 파일: client/slack/build.gradle (패턴 참조)
   - 필수 의존성:
     * spring-boot-starter-mail
     * spring-boot-starter-thymeleaf
     * common-core, common-exception 프로젝트 의존성
     * spring-boot-configuration-processor (annotationProcessor)
   - 삭제해야 할 의존성:
     * spring-cloud-starter-openfeign (이메일과 무관)
   - 검증 기준: 
     * `./gradlew :client-mail:dependencies --configuration compileClasspath | grep mail` 출력 확인
     * `./gradlew :client-mail:build` 성공

**Task 1.3: settings.gradle 확인 (수정 불필요)**
   - 역할: 모듈 자동 등록 확인
   - 설명: 본 프로젝트의 settings.gradle은 `src` 폴더가 있는 모듈을 자동 탐색
   - 필요 조건: `client/mail/src` 폴더가 존재해야 함 (Task 1.1에서 생성)
   - 검증 기준: 
     * Gradle sync 후 모듈 인식
     * `./gradlew projects | grep client-mail` 출력 확인

#### Task 2: 설정 클래스 구현


**Task 2.1: MailProperties 구현**
   - 역할: 이메일 관련 설정 관리
   - 책임: 발신자 주소, 기본 URL, 템플릿 설정, 비동기 설정 관리
   - 파일 위치: client/mail/src/main/java/.../config/MailProperties.java
   - 참고 파일: client/slack/src/main/java/.../config/SlackProperties.java
   - 주요 속성:
     * fromAddress: 발신자 이메일 (환경 변수 ${MAIL_FROM_ADDRESS} 지원)
     * fromName: 발신자 이름
     * baseUrl: 인증 링크 기본 URL
     * async: 비동기 발송 설정
   - 검증 기준: @ConfigurationProperties("mail") 바인딩 정상 동작
   
**Task 2.2: MailConfig 구현**
   - 역할: 이메일 관련 Bean 설정
   - 책임: EmailSender, EmailTemplateService, ThreadPoolTaskExecutor Bean 생성
   - 파일 위치: client/mail/src/main/java/.../config/MailConfig.java
   - 참고 파일: client/slack/src/main/java/.../config/SlackConfig.java
   - 구현 포인트:
     * JavaMailSender는 Spring Boot Auto-configuration 사용 (수동 Bean 등록 불필요)
     * ThreadPoolTaskExecutor 설정 (비동기 이메일 발송용)
     * SpringTemplateEngine 설정 (Thymeleaf)
   - 검증 기준: 모든 Bean 정상 생성, 환경 변수 바인딩 확인

#### Task 3: 이메일 발송 서비스 구현

**패키지 구조 참고** (client/slack 패턴 준수):
```
client/mail/src/main/java/com/ebson/shrimp/tm/demo/client/mail/
├── config/              # MailConfig, MailProperties
├── domain/mail/         # 비즈니스 로직
│   ├── dto/             # EmailMessage
│   ├── service/         # EmailSender, SmtpEmailSender
│   └── template/        # EmailTemplateService, ThymeleafEmailTemplateService
└── exception/           # EmailSendException
```

**Task 3.1: EmailMessage DTO 구현**
   - 역할: 이메일 발송 요청 데이터 캡슐화
   - 책임: 수신자, 제목, HTML/텍스트 본문 데이터 보유 및 유효성 검증
   - 파일 위치: client/mail/src/main/java/.../client/mail/domain/mail/dto/EmailMessage.java
   - 검증 기준: record 타입, @Builder 패턴, null 체크 포함
   
**Task 3.2: EmailSender 인터페이스 정의**
   - 역할: 이메일 발송 추상화
   - 책임: send(), sendAsync() 메서드 정의
   - 파일 위치: client/mail/src/main/java/.../client/mail/domain/mail/service/EmailSender.java
   - 검증 기준: 단일 책임 원칙 준수, JavaDoc 포함

**Task 3.3: SmtpEmailSender 구현**
   - 역할: SMTP 기반 이메일 발송
   - 책임: JavaMailSender를 사용한 MimeMessage 생성 및 발송
   - 파일 위치: client/mail/src/main/java/.../client/mail/domain/mail/service/SmtpEmailSender.java
   - 구현 포인트:
     * MimeMessageHelper 사용 (UTF-8 인코딩)
     * HTML 및 Plain Text 지원 (multipart/alternative)
     * 비동기 발송 (ThreadPoolTaskExecutor 활용, 명시적 execute)
     * 발송 실패 시 로깅만 수행 (회원가입 트랜잭션과 분리)
     * MailProperties에서 발신자 정보 주입
     * 환경별 SMTP 설정 자동 적용 (spring.mail.* 속성)
   - 에러 처리:
     * send(): MailException 캐치 → EmailSendException 던짐
     * sendAsync(): Exception 캐치 → 로그만 출력 (Fail-Safe)
     * 비동기 실행으로 트랜잭션 영향 없음
   - 검증 기준: 
     * Gmail SMTP로 실제 이메일 발송 성공
     * 이메일 발송 실패 시 회원가입 정상 완료
     * 로그에 발송 성공/실패 기록

**Task 3.4: EmailSendException 정의**
   - 역할: 이메일 발송 실패 예외
   - 책임: 발송 실패 시 명확한 예외 제공 (동기 발송 시에만 사용)
   - 파일 위치: client/mail/src/main/java/.../client/mail/exception/EmailSendException.java
   - 참고: client/slack/exception/SlackException.java 패턴 참조

#### Task 4: 이메일 템플릿 서비스 구현


**Task 4.1: EmailTemplateService 인터페이스 정의**
   - 역할: 이메일 템플릿 렌더링 추상화
   - 책임: renderVerificationEmail(), renderPasswordResetEmail() 정의
   - 파일 위치: client/mail/src/main/java/.../client/mail/domain/mail/template/EmailTemplateService.java
   - 검증 기준: JavaDoc 포함, 인터페이스 분리 원칙 준수

**Task 4.2: ThymeleafEmailTemplateService 구현**
   - 역할: Thymeleaf 기반 템플릿 렌더링
   - 책임: TemplateEngine을 사용하여 HTML 생성
   - 파일 위치: client/mail/src/main/java/.../client/mail/domain/mail/template/ThymeleafEmailTemplateService.java
   - 구현 포인트:
     * Thymeleaf Context에 변수 설정 (email, token, verifyUrl/resetUrl)
     * `templateEngine.process("email/verification", context)` 호출
   - 검증 기준: 템플릿 변수 바인딩 정상 동작

**Task 4.3: 이메일 인증 템플릿 작성**
   - 역할: 회원가입 인증 이메일 HTML 템플릿
   - 파일 위치: client/mail/src/main/resources/templates/email/verification.html
   - 포함 내용: 
     * 이메일 주소 표시 (`th:text="${email}"`)
     * 인증 버튼 (`th:href="${verifyUrl}"`)
     * 인증 URL 텍스트 표시
     * 만료 안내 (24시간)
   - 참고 파일: docs/step19/email-verification-implementation-design.md (7.2 템플릿 설계)

**Task 4.4: 비밀번호 재설정 템플릿 작성**
   - 역할: 비밀번호 재설정 이메일 HTML 템플릿
   - 파일 위치: client/mail/src/main/resources/templates/email/password-reset.html
   - 포함 내용: 
     * 이메일 주소 표시 (`th:text="${email}"`)
     * 재설정 버튼 (`th:href="${resetUrl}"`)
     * 재설정 URL 텍스트 표시
     * 보안 경고 메시지
   - 참고 파일: docs/step19/email-verification-implementation-design.md (7.3 템플릿 설계)

#### Task 5: 기존 코드 통합


**Task 5.1: api/auth/build.gradle 수정**
   - 역할: client-mail 모듈 의존성 추가
   - 책임: implementation project(':client-mail') 추가
   - 검증 기준: api-auth 모듈 빌드 성공

**Task 5.2: EmailVerificationService 수정**
   - 역할: 이메일 발송 로직 통합
   - 책임: 토큰 생성 후 이메일 발송 호출
   - 파일 위치: api/auth/src/main/java/.../service/EmailVerificationService.java
   - 수정 내용:
     * EmailSender, EmailTemplateService, MailProperties 의존성 주입 (생성자 주입)
     * createEmailVerificationToken() 메서드에 이메일 발송 로직 추가:
       - 토큰 생성 및 DB 저장
       - EmailTemplateService로 HTML 생성
       - EmailSender.sendAsync()로 비동기 발송
     * requestPasswordReset() 메서드에 이메일 발송 로직 추가:
       - 토큰 생성 및 DB 저장
       - EmailTemplateService로 HTML 생성
       - EmailSender.sendAsync()로 비동기 발송
     * 이메일 발송 실패 시 로깅만 수행 (트랜잭션 롤백 없음)
   - 검증 기준: 
     * 회원가입 시 인증 이메일 발송 (Gmail 수신함 확인)
     * 비밀번호 재설정 요청 시 이메일 발송 (Gmail 수신함 확인)
     * 이메일 발송 실패 시 회원가입/토큰 생성은 정상 완료

**Task 5.3: AuthService 수정 (필요시)**
   - 역할: 변경된 메서드 호출
   - 책임: EmailVerificationService 메서드 호출 수정
   - 검증 기준: 기존 API 동작 유지

#### Task 6: 설정 파일 작성


**Task 6.1: application-mail.yml 작성**
   - 역할: 이메일 모듈 기본 설정
   - 파일 위치: client/mail/src/main/resources/application-mail.yml
   - 내용: mail.from-address, mail.from-name, mail.base-url, mail.async 설정
   - 참고 파일: docs/step19/email-verification-implementation-design.md (10.1 설정 가이드)


**Task 6.2: application-local.yml 수정 (api/auth)**
   - 역할: 로컬 환경 SMTP 설정
   - 파일 위치: api/auth/src/main/resources/application-local.yml
   
   - **옵션 A - Gmail SMTP (실제 이메일 발송, 추천)**:
       spring:
         mail:
           host: ${MAIL_HOST:smtp.gmail.com}
           port: ${MAIL_PORT:587}
           username: ${MAIL_USERNAME:ebson024.v1@gmail.com}
           password: ${MAIL_PASSWORD:rdxzemhatprwllck}
           properties:
             mail.smtp.auth: ${MAIL_SMTP_AUTH:true}
             mail.smtp.starttls.enable: ${MAIL_SMTP_STARTTLS:true}
             mail.smtp.starttls.required: true
             mail.smtp.connectiontimeout: 5000
             mail.smtp.timeout: 3000
             mail.smtp.writetimeout: 5000
       mail:
         from-address: ${MAIL_FROM_ADDRESS:ebson024.v1@gmail.com}
         from-name: Shrimp TM (Local)
         base-url: http://localhost:8080
     
     옵션 B - MailHog (가상 SMTP, 빠른 반복 테스트):
       spring:
         mail:
           host: localhost
           port: 1025
           properties:
             mail.smtp.auth: false
             mail.smtp.starttls.enable: false
       mail:
         from-address: noreply@localhost
         base-url: http://localhost:8080
     
     환경 변수 설정 (IntelliJ):
       Run/Debug Configurations → Environment variables:
       MAIL_HOST=smtp.gmail.com
       MAIL_PORT=587
       MAIL_USERNAME=ebson024.v1@gmail.com
       MAIL_PASSWORD=rdxzemhatprwllck
       MAIL_SMTP_AUTH=true
       MAIL_SMTP_STARTTLS=true
       MAIL_FROM_ADDRESS=ebson024.v1@gmail.com
     
   - 참고 파일: docs/step19/email-verification-implementation-design.md (2장 Quick Start, 10.2 환경별 설정)
   - 검증 기준: 환경 변수 설정 후 실제 Gmail로 이메일 발송 성공

**Task 6.3: docker-compose.yml 수정 (옵션 B 사용 시)**
   - 역할: MailHog 서비스 추가
   - 내용: 
     ```yaml
     services:
       mailhog:
         image: mailhog/mailhog
         ports:
           - "1025:1025"  # SMTP
           - "8025:8025"  # Web UI
     ```
   - 검증 기준: docker-compose up -d mailhog 후 http://localhost:8025 접속 가능

**Task 6.4: .gitignore 업데이트**
   - 역할: 민감 정보 파일 보호
   - 추가 내용:
     ```gitignore
     ### Environment Variables & Secrets ###
     .env
     .env.local
     .env.*.local
     *.private.yml
     *-private.yml
     application-local.yml
     ```
   - 검증 기준: Git 상태에서 제외됨 확인

---

## Verification Criteria (검증 기준)

### Build Verification (빌드 검증)
- [ ] client/mail 모듈 생성 및 빌드 성공 (`./gradlew :client-mail:build`)
- [ ] api/auth 모듈 빌드 성공 (`./gradlew :api-auth:build`)
- [ ] 전체 프로젝트 빌드 성공 (`./gradlew clean build`)
- [ ] 컴파일 에러 없음

### Functional Verification - Gmail SMTP (기능 검증 - 권장)
- [ ] 환경 변수 설정 완료 (IntelliJ 또는 터미널)
- [ ] POST /api/v1/auth/signup 요청 시 인증 이메일 발송 성공
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/signup \
    -H "Content-Type: application/json" \
    -d '{"email": "test@example.com", "username": "testuser", "password": "Test1234!@"}'
  ```
- [ ] Gmail 수신함(ebson024.v1@gmail.com)에서 인증 이메일 확인
- [ ] 이메일 HTML 템플릿 정상 렌더링 (버튼, 링크, 만료 시간 확인)
- [ ] POST /api/v1/auth/reset-password 요청 시 재설정 이메일 발송 성공
- [ ] Gmail 수신함에서 비밀번호 재설정 이메일 확인
- [ ] 이메일 발송 실패 시 회원가입 트랜잭션은 정상 완료 (비동기 분리 확인)

### Alternative Verification - MailHog (대안 검증 - 빠른 반복 테스트)
- [ ] docker-compose up -d mailhog 실행 성공
- [ ] http://localhost:8025 웹 UI 접속 가능
- [ ] 회원가입 요청 시 MailHog 웹 UI에서 이메일 확인
- [ ] 비밀번호 재설정 요청 시 MailHog 웹 UI에서 이메일 확인

### Security Verification (보안 검증)
- [ ] .gitignore에 민감 정보 파일 패턴 추가 확인
- [ ] git status에서 .env, application-local.yml 제외 확인
- [ ] 코드에 비밀번호 하드코딩 없음 확인

---

## Quick Start Guide (빠른 시작 가이드)

구현 완료 후 아래 명령어로 즉시 테스트 가능:

### Option A: Gmail SMTP (실제 이메일 발송)
```bash
# 1. 환경 변수 설정 (터미널)
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=ebson024.v1@gmail.com
export MAIL_PASSWORD=rdxzemhatprwllck
export MAIL_SMTP_AUTH=true
export MAIL_SMTP_STARTTLS=true
export MAIL_FROM_ADDRESS=ebson024.v1@gmail.com

# 2. 애플리케이션 실행
./gradlew :api-auth:bootRun

# 3. 회원가입 API 호출
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "username": "testuser",
    "password": "Test1234!@"
  }'

# 4. Gmail 수신함 확인
# ebson024.v1@gmail.com 로그인하여 인증 이메일 확인
```

### Option B: MailHog (빠른 반복 테스트)
```bash
# 1. MailHog 실행
docker-compose up -d mailhog

# 2. 환경 변수 없이 실행 (기본값 localhost:1025)
./gradlew :api-auth:bootRun

# 3. 회원가입 API 호출 (위와 동일)

# 4. 브라우저에서 http://localhost:8025 확인
```
```

### 17단계: 테스트 및 Spring REST Docs 기반 API 문서화

**단계 번호**: 17단계
**의존성**: 모든 이전 단계 완료 권장 (특히 15단계 API 컨트롤러 구현 완료 필수)
**다음 단계**: 없음 (최종 단계)

```
plan task: 테스트 작성 및 Spring REST Docs 기반 API 문서화

참고 파일: docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표), docs/step18/README.md (테스트 및 문서화 단계 개요)

**description** (작업 설명):
- **작업 목표**:
  - 모든 API 모듈에 대한 단위 테스트 작성
  - 통합 테스트 작성 (Spring REST Docs 포함)
  - Spring REST Docs 기반 API 문서 자동 생성
  - 테스트 커버리지 측정 및 개선
  - Asciidoctor 문서 작성 및 빌드

- **배경**:
  - 테스트 기반 API 문서 자동 생성으로 정확하고 최신 상태를 유지하는 API 문서 제공
  - 코드와 문서의 일관성 보장
  - 모든 REST API 엔드포인트에 대한 문서 자동 생성

- **예상 결과**:
  - 모든 API 모듈의 단위 테스트 및 통합 테스트 완료
  - Spring REST Docs 기반 API 문서 자동 생성 완료
  - Asciidoctor 형식의 문서 생성 (HTML 및 PDF 변환 가능)
  - 테스트 커버리지 측정 및 리포트 생성

**requirements** (기술 요구사항 및 제약조건):
1. **Spring REST Docs 사용**: MockMvc 또는 WebTestClient를 통한 테스트 기반 문서 생성
2. **Asciidoctor 형식**: Asciidoctor 템플릿 커스터마이징
3. **모듈별 독립 문서**: 각 API 모듈별 독립적인 문서 생성
4. **공통 섹션 분리**: 인증, 에러 처리, 페이징 등 공통 섹션 분리
5. **CI/CD 통합**: 문서 생성 자동화

**작업 수행 단계** (공식 가이드 6단계 프로세스 준수, 반드시 순서대로 수행):

**1단계: Analysis Purpose (작업 목표 분석)**
- **Task Description 이해** (위의 `description` 섹션 참고):
  - 작업 목표: 테스트 작성 및 Spring REST Docs 기반 API 문서화
  - 배경: 테스트 기반 문서 자동 생성, 코드와 문서의 일관성 보장
  - 예상 결과: 단위/통합 테스트 완료, API 문서 자동 생성 완료
- **Task Requirements 이해** (위의 `requirements` 섹션 참고):
  - Spring REST Docs 사용 (MockMvc 또는 WebTestClient)
  - Asciidoctor 형식 문서 생성
  - 모듈별 독립 문서 생성
  - CI/CD 통합
- **작업 분해 및 우선순위** (참고 정보):
  - **Phase 1: Spring REST Docs 설정** (우선순위: 높음)
    * 의존성: 없음
    * 작업: Spring REST Docs 의존성 추가, 설정 클래스 구현, Asciidoctor 플러그인 설정
  - **Phase 2: 단위 테스트 작성** (우선순위: 높음)
    * 의존성: Phase 1 완료
    * 작업: 모든 API 모듈의 단위 테스트 작성
  - **Phase 3: 통합 테스트 작성 (Spring REST Docs 포함)** (우선순위: 높음)
    * 의존성: Phase 1, 2 완료
    * 작업: MockMvc 또는 WebTestClient를 통한 통합 테스트 작성, 문서 스니펫 생성
  - **Phase 4: Asciidoctor 문서 작성** (우선순위: 중간)
    * 의존성: Phase 3 완료
    * 작업: Asciidoctor 템플릿 작성, 문서 통합
  - **Phase 5: 테스트 커버리지 측정** (우선순위: 중간)
    * 의존성: Phase 2, 3 완료
    * 작업: JaCoCo 설정, 커버리지 리포트 생성
  - **Phase 6: 문서 빌드 및 배포 자동화** (우선순위: 낮음)
    * 의존성: Phase 4 완료
    * 작업: CI/CD 파이프라인에 문서 생성 통합

**2단계: Identify Project Architecture (프로젝트 아키텍처 파악)**
- **루트 디렉토리 구조 및 설정 파일 확인**:
  - 루트 디렉토리 구조 확인 (`list_dir`로 프로젝트 루트 구조 파악)
  - `build.gradle` 확인 (프로젝트 빌드 설정, 테스트 의존성)
  - 각 API 모듈의 테스트 디렉토리 구조 확인
- **핵심 설정 파일 및 구조 확인**:
  - 기존 테스트 코드 패턴 확인
  - Spring REST Docs 설정 상태 확인
  - Asciidoctor 플러그인 설정 상태 확인

**3단계: Collect Information (정보 수집)**
- **불확실한 부분이 있으면 반드시 다음 중 하나 수행**:
  - `read_file`로 `docs/reference/shrimp-task-prompts-final-goal.md` 확인
  - `read_file`로 `docs/step16/README.md` 확인
  - `codebase_search`로 기존 테스트 코드 패턴 확인
  - `web_search`로 Spring REST Docs 공식 문서 검색
- **정보 수집 대상**:
  - Spring REST Docs 공식 문서
  - Asciidoctor 공식 문서
  - 기존 테스트 코드 패턴
  - 프로젝트의 API 모듈 구조

**4단계: Check Existing Programs and Structures (기존 프로그램 및 구조 확인)**
- **중요 원칙: "check first, then design"**:
  - **반드시 기존 코드를 확인하기 전에 설계를 생성하지 않음**
  - 먼저 기존 테스트 코드를 확인한 후 설계를 진행해야 함
- **정확한 검색 전략 사용**:
  - `read_file`로 각 API 모듈의 테스트 디렉토리 구조 확인
  - `codebase_search`로 기존 테스트 코드 패턴 검색
- **기존 코드 확인** (check first, then design):
  - 기존 테스트 코드 패턴 분석
  - 테스트 프레임워크 사용 현황 확인 (JUnit, Mockito 등)
  - Spring REST Docs 설정 상태 확인

**5단계: Task Type-Specific Guidelines (작업 유형별 가이드라인)**
- **Spring REST Docs Tasks** 관련 추가 고려사항:
  - **의존성 설정 패턴 분석**:
    * `spring-restdocs-mockmvc` 또는 `spring-restdocs-webtestclient` 선택
    * `spring-restdocs-asciidoctor` 의존성 추가
    * Asciidoctor Gradle 플러그인 설정
  - **문서 생성 패턴 분석**:
    * MockMvc 또는 WebTestClient를 통한 테스트 작성
    * 문서 스니펫 생성 (`document()` 메서드 활용)
    * Asciidoctor 템플릿 작성
  - **모듈별 문서 관리 패턴**:
    * 각 API 모듈별 독립적인 문서 생성
    * 공통 섹션 분리 및 재사용
- **테스트 작성 Tasks** 관련 추가 고려사항:
  - **단위 테스트 패턴 분석**:
    * Service, Repository 레이어 단위 테스트
    * Mock 객체 활용 패턴
  - **통합 테스트 패턴 분석**:
    * Controller 레이어 통합 테스트
    * Spring REST Docs 통합 패턴

**6단계: Preliminary Solution Output (초기 설계 솔루션 출력)**
- **초기 설계 솔루션 작성**:
  - 위 단계들을 기반으로 "초기 설계 솔루션" 작성
  - **사실(facts)**과 **추론(inferences)** 명확히 구분
  - **모호한 표현 금지**: 최종 결과물 수준으로 구체적으로 작성
  - **기존 아키텍처 패턴과의 일관성 보장**
- **초기 설계 솔루션에 포함할 내용**:
  - **Spring REST Docs 설정 설계**:
    * 의존성 추가 계획
    * 설정 클래스 설계
    * Asciidoctor 플러그인 설정 설계
  - **테스트 작성 전략 설계**:
    * 단위 테스트 작성 계획
    * 통합 테스트 작성 계획 (Spring REST Docs 포함)
    * 테스트 커버리지 측정 계획
  - **문서 생성 전략 설계**:
    * 모듈별 문서 생성 계획
    * 공통 섹션 분리 계획
    * Asciidoctor 템플릿 설계
  - **CI/CD 통합 설계**:
    * 문서 생성 자동화 계획
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구를 활용하여 실제 정보를 수집해야 함
  - 공식 문서만 참고 (Spring REST Docs, Asciidoctor 공식 문서)
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```

**검증 기준**:
- [ ] Spring REST Docs 설정 완료 (의존성 추가, 설정 클래스 구현)
- [ ] 모든 API 모듈의 단위 테스트 작성 완료
- [ ] 모든 API 모듈의 통합 테스트 작성 완료 (Spring REST Docs 포함)
- [ ] API 문서 자동 생성 완료 (Asciidoctor 형식)
- [ ] 테스트 커버리지 측정 및 리포트 생성 완료
- [ ] CI/CD 파이프라인에 문서 생성 통합 완료
- [ ] 모든 테스트가 통과하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
- [ ] 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
- [ ] `analyze_task` 도구 호출 완료: summary와 initialConcept를 포함하여 호출

참고 파일: 
- `docs/reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표 (Spring REST Docs 섹션 참고)
- `docs/step16/README.md`: 테스트 및 문서화 단계 개요
- Spring REST Docs 공식 문서: https://docs.spring.io/spring-restdocs/docs/current/reference/html5/
- Asciidoctor 공식 문서: https://asciidoctor.org/docs/
```
   - AnswerGenerationChain: 최종 답변 생성
5. API 레이어 구현
   - ChatRequest, ChatResponse DTO 구현
   - ChatbotFacade 구현
   - ChatbotController 구현
   - ExceptionHandler 구현

**핵심 제한사항 (절대 준수 필수)**:
1. **langchain4j만 사용**: spring-ai, 다른 LLM 통합 프레임워크 사용 절대 금지. langchain4j 오픈소스만 사용 (https://github.com/langchain4j/langchain4j)
2. **공식 문서만 참고**: langchain4j 공식 문서 (https://docs.langchain4j.dev/), MongoDB Atlas 공식 문서 (https://www.mongodb.com/docs/atlas/), LLM Provider 공식 문서만 참고. 블로그, 튜토리얼 등 비공식 자료 절대 금지
3. **오버엔지니어링 절대 금지**: YAGNI 원칙 준수. 설계서에 명시된 기능만 구현. 복잡한 에이전트 시스템, 실시간 스트리밍, 멀티턴 대화 히스토리 관리 등 설계서에 없는 기능 절대 구현하지 않음
4. **클린코드 및 SOLID 원칙 준수**: 의미 있는 이름, 작은 함수, 명확한 의도, 단일 책임 원칙 필수 준수
5. **프로젝트 구조 준수**: api/chatbot 모듈 신규 생성, 기존 API 모듈 패턴 완전히 준수 (controller, facade, service, dto, config 구조)

작업 내용:
1. 모듈 생성 및 기본 설정
   **주의**: api/chatbot 모듈을 신규 생성합니다. 기존 api 모듈 구조를 완전히 준수해야 합니다.
   
   - api/chatbot 모듈 생성
     역할: 챗봇 API 모듈 생성
     책임: 모듈 디렉토리 구조 생성, build.gradle 작성, application-chatbot-api.yml 작성
     패키지 구조: com.tech.n.ai.api.chatbot
     검증 기준: 모듈이 정상적으로 빌드 가능해야 함, 기존 API 모듈 구조와 일치해야 함
   
   - langchain4j 의존성 추가
     역할: langchain4j 라이브러리 통합
     책임: build.gradle에 필요한 langchain4j 모듈 의존성 추가
     필수 의존성:
       * dev.langchain4j:langchain4j (Core)
       * dev.langchain4j:langchain4j-mongodb-atlas (MongoDB Atlas Vector Store)
       * dev.langchain4j:langchain4j-open-ai (OpenAI LLM 및 Embedding)
     참고 파일: docs/step12/rag-chatbot-design.md (langchain4j 통합 설계 섹션)
     검증 기준: 의존성이 정상적으로 추가되어야 함, 빌드가 성공해야 함
   
   - LangChain4jConfig 구현
     역할: langchain4j Bean 설정
     책임: ChatLanguageModel, EmbeddingModel, EmbeddingStore Bean 생성
     참고 파일: docs/step12/rag-chatbot-design.md (langchain4j Bean 설정 섹션)
     검증 기준: 모든 Bean이 정상적으로 생성되어야 함, Spring Boot 애플리케이션이 정상적으로 시작되어야 함

2. MongoDB Atlas Vector Search 설정
   
   - 벡터 필드 추가
     역할: MongoDB Document에 벡터 필드 추가
     책임: ContestDocument, NewsArticleDocument, ArchiveDocument에 embeddingText, embeddingVector 필드 추가
     참고 파일: docs/step12/rag-chatbot-design.md (벡터 필드 스키마 설계 섹션)
     검증 기준: 모든 Document에 벡터 필드가 추가되어야 함, 컴파일 에러가 없어야 함
   
   - 임베딩 텍스트 생성 로직 구현
     역할: Document에서 임베딩 대상 텍스트 생성
     책임: 각 Document 타입별로 임베딩 텍스트 생성 메서드 구현
     ContestDocument: title + description + metadata.tags
     NewsArticleDocument: title + summary + content (content는 최대 2000자로 제한)
     ArchiveDocument: itemTitle + itemSummary + tag + memo
     참고 파일: docs/step12/rag-chatbot-design.md (임베딩 텍스트 생성 로직 섹션)
     검증 기준: 모든 Document 타입에 대해 임베딩 텍스트가 정상적으로 생성되어야 함
   
   - Vector Search Index 생성 (수동 작업)
     역할: MongoDB Atlas에서 Vector Search Index 생성
     책임: MongoDB Atlas 콘솔에서 Vector Search Index 생성
     참고 파일: docs/step12/rag-chatbot-design.md (Vector Index 설정 섹션)
     검증 기준: Vector Search Index가 정상적으로 생성되어야 함, 벡터 검색이 정상적으로 동작해야 함
     **주의**: 이 작업은 MongoDB Atlas 콘솔에서 수동으로 수행해야 합니다. 코드로는 Index 생성 스크립트만 제공합니다.

3. 서비스 레이어 구현
   
   - InputPreprocessingService 구현
     역할: 유저 입력 전처리
     책임: 입력 검증, 정규화, 특수 문자 필터링
     참고 파일: docs/step12/rag-chatbot-design.md (유저 입력 전처리 설계 섹션)
     검증 기준: 빈 입력, 길이 제한, 정규화가 정상적으로 동작해야 함
   
   - IntentClassificationService 구현
     역할: 의도 분류
     책임: RAG 필요 여부 판단 (일반 대화 vs RAG 필요 질문)
     참고 파일: docs/step12/rag-chatbot-design.md (의도 분류 로직 섹션)
     검증 기준: RAG 필요 질문과 일반 대화를 정확히 분류해야 함
   
   - VectorSearchService 구현
     역할: 벡터 검색 수행
     책임: 쿼리 임베딩 생성, MongoDB Atlas Vector Search 수행, 검색 결과 반환
     참고 파일: docs/step12/rag-chatbot-design.md (검색 쿼리 설계 섹션)
     검증 기준: 벡터 검색이 정상적으로 동작해야 함, 유사도 점수가 정상적으로 계산되어야 함, 사용자별 아카이브 필터링이 정상적으로 동작해야 함
   
   - PromptService 구현
     역할: 프롬프트 생성 및 최적화
     책임: 검색 결과를 포함한 프롬프트 생성, 토큰 수 제한, 프롬프트 최적화
     참고 파일: docs/step12/rag-chatbot-design.md (토큰 제어 설계 섹션)
     검증 기준: 프롬프트가 정상적으로 생성되어야 함, 토큰 수가 제한을 초과하지 않아야 함
   
   - LLMService 구현
     역할: LLM 호출
     책임: ChatLanguageModel을 사용하여 LLM 호출, 응답 반환
     참고 파일: docs/step12/rag-chatbot-design.md (langchain4j 통합 설계 섹션)
     검증 기준: LLM 호출이 정상적으로 동작해야 함, 응답이 정상적으로 반환되어야 함
   
   - TokenService 구현
     역할: 토큰 제어 및 추적
     책임: 토큰 수 예측, 검색 결과 토큰 제한, 토큰 사용량 추적, 비용 계산
     참고 파일: docs/step12/rag-chatbot-design.md (토큰 제어 설계 섹션)
     검증 기준: 토큰 수 예측이 정상적으로 동작해야 함, 토큰 사용량이 정상적으로 추적되어야 함
   
   - CacheService 구현
     역할: 캐싱 전략 구현
     책임: Redis를 사용한 캐싱, 캐시 키 생성, TTL 관리
     참고 파일: docs/step12/rag-chatbot-design.md (캐싱 전략 섹션)
     검증 기준: 캐싱이 정상적으로 동작해야 함, 캐시 히트율이 측정 가능해야 함

4. 체인 구현
   
   - InputInterpretationChain 구현
     역할: 입력을 검색 쿼리로 변환
     책임: 입력 정제, 검색 쿼리 추출, 컨텍스트 파악
     참고 파일: docs/step12/rag-chatbot-design.md (입력 해석 체인 섹션)
     검증 기준: 입력이 정상적으로 검색 쿼리로 변환되어야 함
   
   - ResultRefinementChain 구현
     역할: 검색 결과 정제
     책임: 유사도 점수 필터링, 중복 제거, 관련성 순 정렬
     참고 파일: docs/step12/rag-chatbot-design.md (검색 결과 정제 체인 섹션)
     검증 기준: 검색 결과가 정상적으로 정제되어야 함
   
   - AnswerGenerationChain 구현
     역할: 최종 답변 생성
     책임: 프롬프트 생성, LLM 호출, 답변 후처리
     참고 파일: docs/step12/rag-chatbot-design.md (답변 생성 체인 섹션)
     검증 기준: 답변이 정상적으로 생성되어야 함

5. API 레이어 구현
   
   - DTO 구현
     역할: 요청/응답 DTO 정의
     책임: ChatRequest, ChatResponse, Source, TokenUsage DTO 구현
     참고 파일: docs/step12/rag-chatbot-design.md (Chatbot API 엔드포인트 설계 섹션)
     검증 기준: 모든 DTO가 정상적으로 정의되어야 함, Validation 어노테이션이 적절히 적용되어야 함
   
   - ChatbotFacade 구현
     역할: Controller와 Service 사이의 중간 계층
     책임: 요청 변환, 서비스 호출, 응답 변환
     참고 파일: 기존 api 모듈의 Facade 패턴 (api/contest/facade/ContestFacade.java 등)
     검증 기준: Facade가 정상적으로 동작해야 함, 기존 Facade 패턴과 일치해야 함
   
   - ChatbotController 구현
     역할: RESTful API 엔드포인트 제공
     책임: POST /api/v1/chatbot 엔드포인트 구현, 요청 검증, 응답 반환
     참고 파일: docs/step12/rag-chatbot-design.md (Chatbot API 엔드포인트 설계 섹션), 기존 Controller 패턴
     검증 기준: API 엔드포인트가 정상적으로 동작해야 함, 기존 Controller 패턴과 일치해야 함
   
   - ChatbotExceptionHandler 구현
     역할: 전역 예외 처리
     책임: InvalidInputException, TokenLimitExceededException 등 예외 처리
     참고 파일: 기존 ExceptionHandler 패턴 (api/contest/common/exception/ContestExceptionHandler.java 등)
     검증 기준: 모든 예외가 정상적으로 처리되어야 함, 기존 ExceptionHandler 패턴과 일치해야 함

6. 설정 파일 작성
   
   - application-chatbot-api.yml 작성
     역할: 챗봇 API 모듈 설정
     책임: langchain4j 설정, chatbot 설정, 로깅 설정
     참고 파일: docs/step12/rag-chatbot-design.md (설정 파일 예제 섹션)
     검증 기준: 모든 설정이 정상적으로 로드되어야 함, 애플리케이션이 정상적으로 시작되어야 함

**분석 가이드** (plan task 실행 시 필수 확인 사항):
- **check first, then design** 원칙 준수: 기존 코드 확인 후 설계 진행
- **설계서 완전성 확인**: docs/step12/rag-chatbot-design.md의 모든 섹션을 확인하고 초기 설계 솔루션에 반영
- **기존 패턴 준수**: api/contest, api/news 등 기존 API 모듈의 패턴을 완전히 준수
- **의존성 확인**: domain-mongodb 모듈의 Repository 인터페이스 재사용 방법 확인
- **공통 모듈 활용**: common-core의 ApiResponse<T> 공통 응답 형식 재사용
- **오버엔지니어링 방지**: 설계서에 없는 기능 추가하지 않음
- **공식 문서 참고**: langchain4j, MongoDB Atlas 공식 문서만 참고
- **중요 경고**:
  - 모든 형태의 `가정`, `추측`, `상상`은 엄격히 금지됨
  - 사용 가능한 모든 도구(`read_file`, `codebase_search`, `web_search` 등)를 활용하여 실제 정보를 수집해야 함
  - 추적 가능한 출처가 없는 정보는 사용하지 않음
- **도구 호출 필수**:
  - 반드시 다음 도구를 호출하여 초기 설계 솔루션을 다음 단계로 전달:
  ```
  analyze_task({ 
    summary: <작업 목표, 범위, 핵심 기술 도전 과제를 포함한 구조화된 작업 요약 (최소 10자 이상)>,
    initialConcept: <기술 솔루션, 아키텍처 설계, 구현 전략을 포함한 최소 50자 이상의 초기 구상 (pseudocode 형식 사용 가능, 고급 로직 흐름과 핵심 단계만 제공)>
  })
  ```
  - **주의**: `analyze_task` 도구를 호출하지 않으면 작업이 완료되지 않음
  - **중요**: 이 단계에서 실제 코드 구현은 수행하지 않음. 구현은 `execute_task` 단계에서 `implementationGuide`를 따라 수행됨

**검증 기준**:
- [ ] 설계서(`docs/step12/rag-chatbot-design.md`)의 모든 요구사항을 분석하고 초기 설계 솔루션에 반영
- [ ] "check first, then design" 원칙 준수: 기존 코드 확인 후 설계 진행
- [ ] 사실(facts)과 추론(inferences) 명확히 구분하여 초기 설계 솔루션 작성
- [ ] `api/chatbot` 모듈의 초기 설계 솔루션 완성:
  - langchain4j 통합 설계
  - MongoDB Atlas Vector Search 설정 설계
  - 서비스 레이어 설계 (InputPreprocessingService, IntentClassificationService, VectorSearchService, PromptService, LLMService, TokenService, CacheService)
  - 체인 설계 (InputInterpretationChain, ResultRefinementChain, AnswerGenerationChain)
  - API 레이어 설계 (Controller, Facade, DTO, ExceptionHandler)
- [ ] 기존 API 모듈(`api/contest`, `api/news`, `api/auth` 등)과 일관성 있는 패키지 구조 설계
- [ ] `common-core` 모듈의 `ApiResponse<T>` 공통 응답 형식 재사용 방법 명시
- [ ] `domain-mongodb` 모듈의 Repository 인터페이스 재사용 방법 명시
- [ ] 에러 처리 설계: 커스텀 예외 및 글로벌 예외 핸들러 구조 설계
- [ ] 오버엔지니어링 방지: 설계서에 없는 기능 추가하지 않음
- [ ] langchain4j만 사용: spring-ai, 다른 LLM 통합 프레임워크 사용하지 않음
- [ ] 공식 문서만 참고: langchain4j, MongoDB Atlas, LLM Provider 공식 문서만 참고
- [ ] **빌드 검증**: api/chatbot 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-chatbot:build`)
- [ ] **빌드 검증**: 루트 프로젝트에서 전체 빌드가 성공해야 함 (`./gradlew clean build`)
- [ ] **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
```

**참고**: 아래 정보는 `splitTasksRaw` 단계에서 작업을 분해할 때 참고용입니다. plan task 단계에서는 위의 description과 requirements를 기반으로 분석을 수행하세요.

```
plan task: 단위 테스트, 통합 테스트 및 Spring REST Docs 기반 API 문서화

참고 파일: docs/reference/shrimp-task-prompts-final-goal.md (최종 프로젝트 목표)

작업 내용:
1. 단위 테스트 작성
   - Service 레이어 테스트
   - Repository 레이어 테스트
   - 유틸리티 클래스 테스트
   - JWT 토큰 관리 테스트

2. 통합 테스트 작성 (Spring REST Docs 포함)
   - API 엔드포인트 테스트 (MockMvc 또는 WebTestClient)
   - 외부 API 통합 테스트 (Mock)
   - 데이터베이스 통합 테스트
   - Kafka 동기화 테스트
   - 인증/인가 테스트

3. Spring REST Docs 기반 API 문서화
   
   Spring REST Docs 설정:
   - **역할**: API 문서 자동 생성 인프라 구축
   - **책임**: 
     * 의존성 설정
     * 플러그인 구성
     * 문서 생성 경로 설정
   - **검증 기준**: 
     * 의존성이 정상적으로 추가되어야 함
     * 문서 생성이 정상적으로 동작해야 함
   
   - build.gradle에 의존성 추가
     * **파일 위치**: 각 API 모듈의 build.gradle
     * **의존성 예제**:
       ```gradle
       dependencies {
           testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
           // 또는
           testImplementation 'org.springframework.restdocs:spring-restdocs-webtestclient'
           testImplementation 'org.springframework.restdocs:spring-restdocs-asciidoctor'
       }
       
       plugins {
           id 'org.asciidoctor.jvm.convert' version '3.3.2'
       }
       
       asciidoctor {
           dependsOn test
           sources {
               include '**/index.adoc'
           }
       }
       ```
     * spring-restdocs-mockmvc 또는 spring-restdocs-webtestclient
     * asciidoctor-gradle-plugin
   - REST Docs 설정 클래스 구현
     * **역할**: REST Docs 기본 설정
     * **책임**: 문서 스니펫 형식, 출력 경로 설정
   - 문서 출력 디렉토리 설정 (build/generated-snippets)
     * **기본 경로**: `build/generated-snippets`
     * **HTML 출력 경로**: `build/docs/asciidoc/html5`
   
   API 문서 생성:
   - 각 API 엔드포인트별 MockMvcTest 작성
   - 문서 스니펫 자동 생성:
     * request-fields.adoc (요청 필드)
     * response-fields.adoc (응답 필드)
     * path-parameters.adoc (경로 파라미터)
     * query-parameters.adoc (쿼리 파라미터)
     * request-body.adoc (요청 본문 예제)
     * response-body.adoc (응답 본문 예제)
     * http-request.adoc (HTTP 요청 예제)
     * http-response.adoc (HTTP 응답 예제)
   - 커스텀 스니펫 추가 (에러 응답, 인증 예제 등)
   
   Asciidoctor 문서 작성:
   - API 문서 템플릿 작성 (index.adoc)
   - 모듈별 섹션 구성:
     * 공개 API (Contest, News, Source)
     * 인증 API (Auth)
     * 사용자 아카이브 API (Archive)
     * 변경 이력 조회 API (History)
   - 공통 섹션:
     * 인증 방법 (JWT 토큰 사용법)
     * 에러 처리 (에러 코드 및 응답 형식)
     * 페이징 (페이징 파라미터 및 응답)
     * 필터링 및 정렬
   - 예제 코드 및 사용 가이드 포함
   - 에러 코드 및 응답 형식 문서화
   
   문서 빌드 및 배포:
   - Gradle 빌드 시 자동 문서 생성
   - HTML 형식으로 변환 (build/docs/asciidoc/html5)
   - PDF 형식으로 변환 (선택사항)
   - 정적 파일로 배포 (GitHub Pages, S3 등)
   - CI/CD 파이프라인에 문서 빌드 통합
   
   문서 품질 관리:
   - 문서 커버리지 확인 (모든 엔드포인트 문서화)
   - 문서 자동 검증 (빌드 시)
   - 문서 버전 관리
   - 변경 이력 추적

4. 성능 테스트
   - 부하 테스트
   - 캐싱 효과 측정
   - 응답 시간 최적화
   - Kafka 처리량 테스트
```

## 구현 우선순위 가이드

`json/sources.json`을 기반으로 한 구현 우선순위:

### Phase 1: 핵심 기능 (Priority 1 출처만)
1. **개발자 대회 정보**
   - Codeforces API (가장 쉬움, 인증 불필요)
   - Hacker News API (인증 불필요)
   - GitHub API (인증 선택)

2. **IT 테크 뉴스**
   - Hacker News API (인증 불필요)
   - Dev.to API (인증 선택)
   - Google Developers Blog RSS (RSS 파싱)

### Phase 2: 확장 기능
- NewsAPI (API 키 필요, 무료 티어 제한)
- Reddit API (OAuth 권장)
- TechCrunch RSS
- Kaggle API (API 키 필요)

### Phase 3: 보조 출처
- Priority 2, 3 출처 통합
- 웹 스크래핑이 필요한 출처

## 환경 설정

**역할**: 애플리케이션 설정 관리
**책임**: 
- 환경 변수 및 설정 파일 관리
- 민감 정보 보호 (환경 변수 사용)
- 설정 검증

**검증 기준**: 
- 모든 필수 설정이 제공되어야 함
- 민감 정보가 코드에 하드코딩되지 않아야 함
- 설정 값이 유효해야 함
- **빌드 검증**: 모든 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

필요한 API 키 및 설정:

```properties
# application.properties

# Amazon Aurora MySQL 설정
spring.datasource.url=jdbc:mysql://${AURORA_CLUSTER_ENDPOINT}:3306/${DATABASE_NAME}?useSSL=true&requireSSL=true
spring.datasource.username=${AURORA_USERNAME}
spring.datasource.password=${AURORA_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# HikariCP 연결 풀 설정 (Aurora 최적화)
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-test-query=SELECT 1

# JPA 설정
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# Flyway 마이그레이션 설정
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true

# NewsAPI
newsapi.api-key=your-api-key-here

# Kaggle
kaggle.username=your-username
kaggle.key=your-api-key

# GitHub (선택)
github.token=your-personal-access-token

# Reddit (선택)
reddit.client-id=your-client-id
reddit.client-secret=your-client-secret

# OAuth 2.0 클라이언트 (SNS 로그인)
# Google OAuth
spring.security.oauth2.client.registration.google.client-id=your-google-client-id
spring.security.oauth2.client.registration.google.client-secret=your-google-client-secret

# GitHub OAuth
spring.security.oauth2.client.registration.github.client-id=your-github-client-id
spring.security.oauth2.client.registration.github.client-secret=your-github-client-secret

# Kakao OAuth
spring.security.oauth2.client.registration.kakao.client-id=your-kakao-client-id
spring.security.oauth2.client.registration.kakao.client-secret=your-kakao-client-secret

# Naver OAuth
spring.security.oauth2.client.registration.naver.client-id=your-naver-client-id
spring.security.oauth2.client.registration.naver.client-secret=your-naver-client-secret

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

## 연속 실행 모드

모든 작업을 순차적으로 실행하려면:

```
continuous mode
```

## 작업 상태 확인

작업 진행 상황을 확인하려면:

```
list tasks
```

## 특정 작업 실행

특정 작업을 실행하려면:

```
execute task [task-id]
```

## 작업 검토 및 개선

작업을 검토하고 개선하려면:

```
reflect task [task-id]
```

## 빠른 시작 가이드

### 1. 프로젝트 초기화
```bash
init project rules
```

### 2. 작업 계획 수립
위의 "작업 계획 수립" 섹션의 프롬프트를 복사하여 실행:
```bash
plan task: [프롬프트 내용]
```

### 3. 단계별 작업 실행
각 단계를 순차적으로 실행하거나 `continuous mode`로 자동 실행:
```bash
# 단계별 실행
execute task [task-id]

# 또는 연속 실행
continuous mode
```

### 4. 작업 진행 상황 확인
```bash
list tasks
```

## 주요 개선 사항

이 프롬프트는 `json/sources.json`이 이미 생성된 상태와 확장 요구사항을 반영하여 업데이트되었습니다:

1. ✅ **1단계 완료 표시**: 정보 출처 탐색이 완료되었음을 명시
2. 📋 **json/sources.json 참조**: 모든 단계에서 `json/sources.json` 파일을 참조하도록 명시
3. 🎯 **구체적인 구현 가이드**: Priority 1 출처부터 구체적인 API 클라이언트 구현 방법 제시
4. 🔧 **환경 설정 가이드**: 필요한 API 키 및 설정 정보 제공
5. 📊 **구현 우선순위**: Phase별 구현 전략 제시
6. 💡 **실제 예제**: 각 출처별 통합 예제 코드 및 엔드포인트 정보 포함
7. 🔐 **사용자 인증 시스템**: JWT 토큰 기반 인증, OAuth 2.0 지원, 보안 강화
8. 📦 **사용자 아카이브 기능**: CRUD API, Soft Delete, 권한 관리
9. 🔄 **CQRS 패턴**: 읽기(MongoDB Atlas)와 쓰기(Amazon Aurora MySQL) 분리, Kafka 동기화
10. 🔄 **정보 출처 자동 업데이트**: AI LLM 통합을 통한 json/sources.json 자동 생성 시스템 (spring-ai/langchain4j 지원)
11. 📋 **데이터베이스 설계서**: MongoDB Atlas 및 Amazon Aurora MySQL 설계서 생성 가이드
12. 📝 **변경 이력 추적 시스템**: 모든 쓰기 API에 대한 히스토리 테이블 자동 저장
13. 🏗️ **MSA 멀티모듈 아키텍처**: domain, common, client, batch, api 모듈 분리
14. ⚙️ **Spring Batch 및 Jenkins 연동**: json/sources.json 업데이트 배치 작업 자동화
15. 📚 **Spring REST Docs 기반 API 문서화**: 테스트 기반 자동 문서 생성으로 코드와 문서 일관성 보장
16. 🔔 **Slack 알림 모듈**: 배치 작업 및 시스템 이벤트에 대한 실시간 Slack 알림 지원
17. 🤖 **RAG 기반 챗봇**: langchain4j와 MongoDB Atlas Vector Search를 활용한 지식 검색 챗봇 시스템

## 추가 참고 자료

- `json/sources.json`: 선정된 정보 출처 상세 정보 (20개 출처 평가 완료)
- `prompts/source-discovery-prompt.md`: 출처 탐색 및 평가 프롬프트
- `prompts/README.md`: 전체 프롬프트 사용 가이드

## Amazon Aurora MySQL 특화 가이드

### Aurora 클러스터 설정

**역할**: 고가용성 및 성능 최적화를 위한 Aurora 클러스터 구성
**책임**: 
- 클러스터 생성 및 설정
- 보안 그룹 구성
- 파라미터 그룹 설정
- 백업 정책 설정

**검증 기준**: 
- 클러스터가 정상적으로 생성되어야 함
- Multi-AZ 배포가 활성화되어야 함
- 자동 백업이 설정되어야 함

**권장 설정**:
- **인스턴스 클래스**: db.r6g.large 이상 (프로덕션)
- **Multi-AZ**: 활성화 (고가용성)
- **읽기 복제본**: 1-2개 (성능 최적화)
- **백업 보관 기간**: 7일
- **자동 백업**: 활성화
- **Point-in-Time Recovery**: 활성화

### 연결 설정 최적화

**역할**: 애플리케이션과 Aurora 간 연결 최적화
**책임**: 
- 연결 풀 설정
- 타임아웃 설정
- SSL 연결 설정

**검증 기준**: 
- 연결 풀이 정상적으로 동작해야 함
- 연결 누수가 없어야 함
- 성능이 최적화되어야 함

**권장 설정**:
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-test-query: SELECT 1
```

### 읽기 복제본 활용

**역할**: 읽기 성능 최적화를 위한 읽기 복제본 활용
**책임**: 
- 읽기 쿼리 라우팅
- 복제 지연 모니터링
- 장애 시 자동 전환

**검증 기준**: 
- 읽기 쿼리가 읽기 복제본으로 라우팅되어야 함
- 복제 지연이 허용 범위 내여야 함 (5초 이내)
- 장애 시 자동으로 Primary로 전환되어야 함

**구현 방법**:
- `@Transactional(readOnly = true)` 사용 시 읽기 복제본 자동 라우팅
- 읽기 복제본 엔드포인트를 별도로 설정하여 명시적 라우팅 가능

### 성능 모니터링

**역할**: Aurora 성능 모니터링 및 최적화
**책임**: 
- 성능 지표 수집
- 병목 지점 식별
- 최적화 제안

**검증 기준**: 
- 모든 주요 지표가 모니터링되어야 함
- 성능 이슈가 조기에 감지되어야 함

**모니터링 지표**:
- CPU 사용률
- 메모리 사용률
- 연결 수
- 쿼리 성능 (Slow Query Log)
- 읽기/쓰기 IOPS
- 읽기 복제본 지연 시간
- 백업 상태

**도구**:
- CloudWatch
- Performance Insights
- RDS Enhanced Monitoring

### MyBatis 사용 가이드

**목표**: 복잡한 조회 쿼리에만 MyBatis를 제한적으로 사용하여 유지보수성과 성능을 최적화

**MyBatis 사용 허용 사례**:

1. **데이터베이스 종속 함수 사용이 필요한 경우**:
   ```xml
   <!-- 예시: MySQL DATE_FORMAT 함수 사용 -->
   <select id="findContestsByDateRange" resultType="ContestDto">
       SELECT 
           id,
           title,
           DATE_FORMAT(start_date, '%Y-%m-%d') as formattedStartDate,
           DATE_FORMAT(end_date, '%Y-%m-%d') as formattedEndDate
       FROM contests
       WHERE start_date BETWEEN #{startDate} AND #{endDate}
   </select>
   ```

2. **인라인 뷰 서브쿼리를 사용하는 복잡한 조회**:
   ```xml
   <!-- 예시: 인라인 뷰를 사용한 복잡한 집계 쿼리 -->
   <select id="findUserStatistics" resultType="UserStatisticsDto">
       SELECT 
           u.id,
           u.email,
           COUNT(a.id) as archiveCount,
           MAX(a.created_at) as lastArchiveDate
       FROM users u
       LEFT JOIN (
           SELECT id, user_id, created_at
           FROM archives
           WHERE deleted_yn = 'N'
       ) a ON u.id = a.user_id
       WHERE u.id = #{userId}
       GROUP BY u.id, u.email
   </select>
   ```

3. **성능 최적화가 필요한 대량 데이터 조회**:
   ```xml
   <!-- 예시: 대량 데이터 조회를 위한 커서 기반 페이징 -->
   <select id="findContestsWithCursor" resultType="ContestDto">
       SELECT 
           id,
           title,
           start_date,
           end_date
       FROM contests
       WHERE id > #{cursorId}
       ORDER BY id
       LIMIT #{pageSize}
   </select>
   ```

**MyBatis 사용 금지 사례**:

1. **모든 쓰기 작업**:
   ```xml
   <!-- ❌ 금지: INSERT 쿼리 -->
   <insert id="insertUser">
       INSERT INTO users (email, username) VALUES (#{email}, #{username})
   </insert>
   
   <!-- ✅ 올바른 방법: JPA Entity 사용 -->
   User user = new User();
   user.setEmail(email);
   user.setUsername(username);
   userRepository.save(user);
   ```

2. **단순 조회 쿼리**:
   ```xml
   <!-- ❌ 금지: 단순 조회는 JPA Repository 사용 -->
   <select id="findUserById" resultType="User">
       SELECT * FROM users WHERE id = #{id}
   </select>
   
   <!-- ✅ 올바른 방법: JPA Repository 사용 -->
   Optional<User> user = userRepository.findById(id);
   ```

3. **기본 CRUD 작업**:
   ```xml
   <!-- ❌ 금지: UPDATE, DELETE는 JPA Entity 사용 -->
   <update id="updateUser">
       UPDATE users SET email = #{email} WHERE id = #{id}
   </update>
   
   <!-- ✅ 올바른 방법: JPA Entity 사용 -->
   User user = userRepository.findById(id).orElseThrow();
   user.setEmail(email);
   userRepository.save(user);
   ```

**MyBatis Mapper 인터페이스 작성 규칙**:

```java
// domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/aurora/mapper/ContestMapper.java
package com.tech.n.ai.datasource.aurora.mapper;

import com.tech.n.ai.datasource.aurora.dto.ContestDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/**
 * Contest 복잡한 조회 쿼리 전용 Mapper
 * 
 * 주의: 이 Mapper는 복잡한 조회 쿼리에만 사용됩니다.
 * 기본 CRUD 작업은 JPA Repository를 사용하세요.
 */
@Mapper
public interface ContestMapper {
    
    /**
     * 날짜 범위로 대회 조회 (MySQL DATE_FORMAT 함수 사용)
     * 
     * MyBatis 사용 사유: 데이터베이스 종속 함수 사용
     */
    List<ContestDto> findContestsByDateRange(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    /**
     * 인라인 뷰를 사용한 복잡한 집계 쿼리
     * 
     * MyBatis 사용 사유: 인라인 뷰 서브쿼리 사용
     */
    List<ContestDto> findContestsWithStatistics(@Param("userId") Long userId);
}
```

**검증 기준**:
- MyBatis는 복잡한 조회 쿼리에만 사용되어야 함
- 모든 쓰기 작업은 JPA Entity를 통해서만 수행되어야 함
- 단순 조회 쿼리는 JPA Repository 또는 QueryDSL을 사용해야 함
- MyBatis Mapper에 INSERT, UPDATE, DELETE 쿼리가 없어야 함
- MyBatis 사용 사유가 명확히 문서화되어야 함
- **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### 코드 품질 검증: 클린코드 및 객체지향 설계 원칙

**목표**: 개발된 코드가 클린코드 기법과 객체지향 설계 원칙을 준수하는지 검증하고 개선

**검증 범위**:
1. 클린코드 원칙 준수 여부
2. 객체지향 설계 원칙 (SOLID) 준수 여부
3. 코드 가독성 및 유지보수성
4. 네이밍 컨벤션 준수
5. 메서드 및 클래스 크기 적절성

**클린코드 원칙 검증 체크리스트**:

1. **의미 있는 이름 사용**:
   - [ ] 변수, 메서드, 클래스명이 의도를 명확히 표현하는가?
   - [ ] 축약어나 모호한 이름을 사용하지 않았는가?
   - [ ] 일관된 네이밍 컨벤션을 따르는가?
   - [ ] 불필요한 주석 없이 코드 자체로 이해 가능한가?

2. **함수/메서드 설계**:
   - [ ] 함수는 한 가지 일만 수행하는가? (Single Responsibility)
   - [ ] 함수 크기가 적절한가? (일반적으로 20줄 이하 권장)
   - [ ] 함수 인자가 3개 이하인가? (DTO 사용 권장)
   - [ ] 부수 효과(side effect)가 없는가?

3. **주석 및 문서화**:
   - [ ] 코드로 표현할 수 있는 내용을 주석으로 설명하지 않았는가?
   - [ ] 복잡한 로직에 대한 설명 주석이 있는가?
   - [ ] JavaDoc 주석이 필요한 public API에 작성되었는가?
   - [ ] TODO, FIXME 주석이 적절히 사용되었는가?

4. **포맷팅 및 구조**:
   - [ ] 일관된 들여쓰기와 공백 사용
   - [ ] 적절한 빈 줄로 논리적 구분
   - [ ] import 문 정리 (사용하지 않는 import 제거)
   - [ ] 코드 포맷팅 도구 적용 (예: Google Java Format)

5. **에러 처리**:
   - [ ] 예외를 사용한 에러 처리 (오류 코드 반환 지양)
   - [ ] 예외 메시지가 명확한가?
   - [ ] null 체크가 적절히 수행되는가?
   - [ ] 리소스 해제가 확실한가? (try-with-resources 사용)

**객체지향 설계 원칙 (SOLID) 검증 체크리스트**:

1. **Single Responsibility Principle (SRP)**:
   - [ ] 각 클래스가 단일 책임만 가지는가?
   - [ ] 클래스 변경 이유가 하나인가?
   - [ ] God Class (모든 것을 하는 클래스)가 없는가?

2. **Open/Closed Principle (OCP)**:
   - [ ] 확장에는 열려있고 수정에는 닫혀있는가?
   - [ ] 인터페이스나 추상 클래스를 활용하는가?
   - [ ] Strategy 패턴이나 Template Method 패턴을 적절히 사용하는가?

3. **Liskov Substitution Principle (LSP)**:
   - [ ] 하위 클래스가 상위 클래스를 대체 가능한가?
   - [ ] 상속 관계가 논리적으로 타당한가?
   - [ ] 하위 클래스가 상위 클래스의 계약을 위반하지 않는가?

4. **Interface Segregation Principle (ISP)**:
   - [ ] 인터페이스가 클라이언트가 사용하지 않는 메서드를 포함하지 않는가?
   - [ ] 큰 인터페이스를 작은 인터페이스로 분리했는가?
   - [ ] 클라이언트별로 필요한 메서드만 노출하는가?

5. **Dependency Inversion Principle (DIP)**:
   - [ ] 고수준 모듈이 저수준 모듈에 의존하지 않는가?
   - [ ] 추상화(인터페이스)에 의존하는가?
   - [ ] 의존성 주입(DI)을 사용하는가?

**코드 리뷰 체크리스트**:

1. **코드 복잡도**:
   - [ ] 순환 복잡도(Cyclomatic Complexity)가 10 이하인가?
   - [ ] 중첩된 if/for 문이 3단계 이하인가?
   - [ ] 복잡한 조건문을 메서드로 추출했는가?

2. **중복 코드**:
   - [ ] DRY (Don't Repeat Yourself) 원칙을 준수하는가?
   - [ ] 중복된 코드를 공통 메서드나 유틸리티로 추출했는가?
   - [ ] 비슷한 패턴의 코드가 반복되지 않는가?

3. **의존성 관리**:
   - [ ] 순환 의존성이 없는가?
   - [ ] 불필요한 의존성이 없는가?
   - [ ] 의존성 방향이 올바른가? (고수준 → 저수준)

4. **테스트 가능성**:
   - [ ] 메서드가 테스트하기 쉬운 구조인가?
   - [ ] 외부 의존성이 주입 가능한가?
   - [ ] 정적 메서드나 싱글톤을 남용하지 않았는가?

**개선 가이드**:

1. **리팩토링 우선순위**:
   - 높은 우선순위: 중복 코드 제거, God Class 분리, 순환 복잡도 감소
   - 중간 우선순위: 메서드 추출, 네이밍 개선, 주석 정리
   - 낮은 우선순위: 포맷팅 개선, import 정리

2. **코드 리뷰 프로세스**:
   - 모든 코드는 클린코드 및 SOLID 원칙 검증 
   - 자동화된 정적 분석 도구 활용 (예: SonarQube, Checkstyle)
   - 코드 리뷰 시 체크리스트 기반 검증

3. **지속적 개선**:
   - 주기적인 코드 리뷰 및 리팩토링
   - 기술 부채 관리 및 우선순위 설정
   - 팀 내 코드 품질 기준 공유 및 교육

**검증 기준**:
- 모든 클린코드 원칙 체크리스트 항목이 충족되어야 함
- SOLID 원칙 중 최소 4개 이상 준수해야 함
- 순환 복잡도가 10 이하인 메서드 비율이 90% 이상이어야 함
- 중복 코드가 전체 코드의 5% 이하이어야 함
- 코드 리뷰를 통한 품질 검증이 완료되어야 함
- **빌드 검증**: 정적 분석 도구를 통한 코드 품질 검증 통과 (`./gradlew check` 또는 SonarQube 분석 성공)

### 오버엔지니어링 방지 및 검증

**목표**: LLM을 연상시키는 불필요한 복잡성을 제거하고, 실제 필요한 기능만 구현

**오버엔지니어링 정의**:
- 현재 요구사항에 불필요한 추상화나 패턴 적용
- 미래의 가상의 요구사항을 위한 과도한 설계
- 단순한 문제를 복잡하게 해결하는 것
- 불필요한 레이어나 인터페이스 추가

**오버엔지니어링 검증 체크리스트**:

1. **필요성 검증**:
   - [ ] 구현하려는 기능이 실제로 필요한가?
   - [ ] 현재 요구사항에 해당 기능이 명시되어 있는가?
   - [ ] "나중에 필요할 수도 있다"는 이유로 추가한 기능이 아닌가?
   - [ ] YAGNI (You Aren't Gonna Need It) 원칙을 준수하는가?

2. **추상화 수준 검증**:
   - [ ] 현재 사용되지 않는 추상화(인터페이스, 추상 클래스)가 없는가?
   - [ ] 단일 구현체를 위한 인터페이스가 아닌가?
   - [ ] 불필요한 Factory 패턴이나 Builder 패턴이 없는가?
   - [ ] 단순한 DTO에 불필요한 복잡한 매핑 로직이 없는가?

3. **레이어 구조 검증**:
   - [ ] 불필요한 중간 레이어가 없는가?
   - [ ] 단순한 CRUD에 과도한 서비스 레이어가 없는가?
   - [ ] 직접 호출 가능한데 Facade나 Wrapper를 추가하지 않았는가?
   - [ ] 현재 요구사항에 맞는 최소한의 레이어만 사용하는가?

4. **패턴 적용 검증**:
   - [ ] 패턴 적용이 실제 문제 해결에 필요한가?
   - [ ] 단순한 if-else로 해결 가능한데 Strategy 패턴을 사용하지 않았는가?
   - [ ] 현재 하나의 구현체만 있는데 Factory 패턴을 사용하지 않았는가?
   - [ ] 불필요한 Observer, Decorator 패턴이 없는가?

5. **의존성 및 라이브러리 검증**:
   - [ ] 불필요한 외부 라이브러리를 추가하지 않았는가?
   - [ ] 간단한 유틸리티 메서드를 위해 무거운 라이브러리를 사용하지 않았는가?
   - [ ] Java 표준 라이브러리로 해결 가능한데 외부 라이브러리를 사용하지 않았는가?
   - [ ] 사용하지 않는 의존성이 없는가?

6. **코드 복잡도 검증**:
   - [ ] 단순한 로직을 복잡하게 구현하지 않았는가?
   - [ ] 불필요한 람다나 스트림 체이닝이 없는가?
   - [ ] 과도한 Optional 체이닝이 없는가?
   - [ ] 단순한 반복문을 복잡한 함수형 프로그래밍으로 구현하지 않았는가?

**오버엔지니어링 위험 신호**:

1. **레드 플래그 (즉시 개선 필요)**:
   - 사용되지 않는 인터페이스나 추상 클래스
   - 단일 구현체를 위한 Factory 패턴
   - 단순한 CRUD에 과도한 서비스 레이어
   - "나중을 위해" 추가한 미사용 코드

2. **옐로우 플래그 (검토 필요)**:
   - 현재는 하나의 구현체만 있지만 향후 확장 가능성이 높은 경우
   - 복잡하지만 실제 성능이나 유지보수성에 도움이 되는 경우
   - 팀 표준이나 프로젝트 규칙에 맞는 경우

**개선 가이드**:

1. **필요성 검증 프로세스**:
   ```
   기능 추가 전 질문:
   1. 이 기능이 현재 요구사항에 명시되어 있는가?
   2. 이 기능 없이 현재 요구사항을 만족할 수 있는가?
   3. 이 기능이 실제로 사용될 확률은 얼마인가?
   4. 이 기능을 나중에 추가하는 것이 더 비용이 많이 드는가?
   ```

2. **단순화 원칙**:
   - 가장 단순한 해결책부터 시작
   - 복잡도가 필요해질 때만 복잡하게 만들기
   - 실제 사용 사례가 생기면 그때 리팩토링

3. **코드 리뷰 시 확인 사항**:
   - "이 코드가 왜 필요한가?" 질문에 명확한 답변이 있는가?
   - 현재 사용되지 않는 코드가 있는가?
   - 더 간단한 방법으로 해결할 수 없는가?

**LLM 생성 코드 특징 및 주의사항**:

1. **LLM이 자주 하는 오버엔지니어링 패턴**:
   - 모든 클래스에 인터페이스 생성
   - 불필요한 Builder 패턴 적용
   - 과도한 추상화 레이어 추가
   - 사용되지 않는 유틸리티 클래스 생성
   - 미래를 위한 과도한 확장성 고려

2. **검증 방법**:
   - 코드의 각 부분이 실제로 사용되는지 확인
   - 단위 테스트를 통해 실제 사용 여부 확인
   - 코드 커버리지 분석으로 미사용 코드 식별

**검증 기준**:
- 모든 오버엔지니어링 체크리스트 항목이 충족되어야 함
- 사용되지 않는 코드가 전체 코드의 2% 이하이어야 함
- 단일 구현체를 위한 인터페이스가 없어야 함
- 불필요한 추상화 레이어가 없어야 함
- 모든 코드가 실제 요구사항과 직접적으로 연관되어야 함
- 코드 리뷰를 통한 필요성 검증이 완료되어야 함
- **빌드 검증**: 사용하지 않는 코드 검출 도구를 통한 검증 (`./gradlew check` 또는 코드 커버리지 분석)

### JUnit 단위 테스트 필수 작성 가이드

**목표**: 테스트 가능한 모든 Java 파일에 대해 JUnit 단위 테스트를 필수로 작성하여 코드 품질과 안정성 보장

**테스트 작성 범위**:
1. 모든 Service 클래스 (Service, ServiceImpl)
2. 모든 Repository 클래스 (JPA Repository, MongoDB Repository, MyBatis Mapper)
3. 모든 Controller 클래스
4. 모든 유틸리티 클래스
5. 모든 DTO 변환 로직 (Mapper, Converter)
6. 모든 비즈니스 로직을 포함한 클래스

**테스트 작성 제외 대상**:
- 단순한 데이터 클래스 (Getter/Setter만 있는 Entity, DTO)
- 상수만 정의한 클래스
- 설정 클래스 (Configuration) - 통합 테스트로 검증
- 인터페이스 (구현체 테스트로 검증)

**JUnit 테스트 작성 규칙**:

1. **테스트 클래스 네이밍**:
   - 형식: `{원본클래스명}Test`
   - 예시: `UserService` → `UserServiceTest`
   - 예시: `ContestRepository` → `ContestRepositoryTest`

2. **테스트 메서드 네이밍**:
   - 형식: `test{메서드명}_{시나리오}` 또는 `{메서드명}_{시나리오}_성공/실패`
   - 예시: `testCreateUser_성공()`, `testCreateUser_중복이메일_실패()`
   - 또는 `@DisplayName` 어노테이션 사용 권장

3. **테스트 구조 (Given-When-Then 패턴)**:
   ```java
   @Test
   @DisplayName("사용자 생성 성공")
   void testCreateUser_성공() {
       // Given: 테스트 데이터 준비
       UserCreateRequest request = UserCreateRequest.builder()
           .email("test@example.com")
           .username("testuser")
           .build();
       
       // When: 테스트 대상 메서드 실행
       UserResponse response = userService.createUser(request);
       
       // Then: 결과 검증
       assertThat(response).isNotNull();
       assertThat(response.getEmail()).isEqualTo("test@example.com");
       assertThat(response.getUsername()).isEqualTo("testuser");
   }
   ```

4. **테스트 커버리지 기준**:
   - **최소 커버리지**: 70% 이상
   - **권장 커버리지**: 80% 이상
   - **핵심 비즈니스 로직**: 90% 이상 커버리지 목표
   - **예외 처리**: 모든 예외 케이스 테스트 필수

**테스트 작성 가이드**:

1. **Service 레이어 테스트**:
   ```java
   @ExtendWith(MockitoExtension.class)
   class UserServiceTest {
       
       @Mock
       private UserRepository userRepository;
       
       @Mock
       private KafkaProducer kafkaProducer;
       
       @InjectMocks
       private UserServiceImpl userService;
       
       @Test
       @DisplayName("사용자 생성 성공")
       void createUser_성공() {
           // Given
           UserCreateRequest request = new UserCreateRequest("test@example.com", "testuser");
           User savedUser = User.builder()
               .id(1L)
               .email("test@example.com")
               .username("testuser")
               .build();
           
           when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
           when(userRepository.save(any(User.class))).thenReturn(savedUser);
           
           // When
           UserResponse response = userService.createUser(request);
           
           // Then
           assertThat(response).isNotNull();
           assertThat(response.getEmail()).isEqualTo("test@example.com");
           verify(userRepository).save(any(User.class));
           verify(kafkaProducer).publish(any(UserCreatedEvent.class));
       }
       
       @Test
       @DisplayName("사용자 생성 실패 - 중복 이메일")
       void createUser_중복이메일_실패() {
           // Given
           UserCreateRequest request = new UserCreateRequest("test@example.com", "testuser");
           when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
           
           // When & Then
           assertThatThrownBy(() -> userService.createUser(request))
               .isInstanceOf(DuplicateEmailException.class)
               .hasMessage("이미 존재하는 이메일입니다.");
           
           verify(userRepository, never()).save(any(User.class));
       }
   }
   ```

2. **Repository 레이어 테스트**:
   ```java
   @DataJpaTest
   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
   class UserRepositoryTest {
       
       @Autowired
       private UserRepository userRepository;
       
       @Autowired
       private TestEntityManager entityManager;
       
       @Test
       @DisplayName("이메일로 사용자 조회 성공")
       void findByEmail_성공() {
           // Given
           User user = User.builder()
               .email("test@example.com")
               .username("testuser")
               .build();
           entityManager.persistAndFlush(user);
           
           // When
           Optional<User> found = userRepository.findByEmail("test@example.com");
           
           // Then
           assertThat(found).isPresent();
           assertThat(found.get().getEmail()).isEqualTo("test@example.com");
       }
   }
   ```

3. **Controller 레이어 테스트**:
   ```java
   @WebMvcTest(UserController.class)
   class UserControllerTest {
       
       @Autowired
       private MockMvc mockMvc;
       
       @MockBean
       private UserService userService;
       
       @Test
       @DisplayName("사용자 생성 API 성공")
       void createUser_성공() throws Exception {
           // Given
           UserCreateRequest request = new UserCreateRequest("test@example.com", "testuser");
           UserResponse response = new UserResponse(1L, "test@example.com", "testuser");
           
           when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);
           
           // When & Then
           mockMvc.perform(post("/api/v1/users")
                   .contentType(MediaType.APPLICATION_JSON)
                   .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.email").value("test@example.com"))
               .andExpect(jsonPath("$.username").value("testuser"));
       }
   }
   ```

4. **유틸리티 클래스 테스트**:
   ```java
   class DateUtilsTest {
       
       @Test
       @DisplayName("날짜 포맷팅 성공")
       void formatDate_성공() {
           // Given
           LocalDate date = LocalDate.of(2024, 1, 15);
           
           // When
           String formatted = DateUtils.formatDate(date);
           
           // Then
           assertThat(formatted).isEqualTo("2024-01-15");
       }
   }
   ```

**Mock 및 Test Double 사용 가이드**:

1. **Mock 사용 시나리오**:
   - 외부 의존성 (Repository, 외부 API 클라이언트)
   - 비용이 큰 작업 (파일 I/O, 네트워크 호출)
   - 테스트 환경에서 실행 불가능한 작업

2. **Test Double 종류**:
   - **Mock**: 행위 검증 (verify)
   - **Stub**: 반환값 지정 (when().thenReturn())
   - **Spy**: 실제 객체 일부만 모킹

**테스트 실행 및 검증**:

1. **단위 테스트 실행**:
   ```bash
   # 특정 모듈 테스트 실행
   ./gradlew :domain-aurora:test
   
   # 전체 테스트 실행
   ./gradlew test
   
   # 테스트 커버리지 확인
   ./gradlew test jacocoTestReport
   ```

2. **테스트 커버리지 확인**:
   - JaCoCo 플러그인 사용
   - 커버리지 리포트 생성: `build/reports/jacoco/test/html/index.html`
   - CI/CD 파이프라인에서 커버리지 기준 검증

**테스트 작성 체크리스트**:

1. **테스트 작성 전**:
   - [ ] 테스트 대상 클래스의 책임이 명확한가?
   - [ ] 테스트 가능한 구조인가? (의존성 주입 가능한가?)
   - [ ] 테스트 시나리오가 명확히 정의되었는가?

2. **테스트 작성 중**:
   - [ ] Given-When-Then 패턴을 따르는가?
   - [ ] 테스트 메서드가 독립적으로 실행 가능한가?
   - [ ] Mock 객체가 적절히 사용되었는가?
   - [ ] 예외 케이스도 테스트하는가?

3. **테스트 작성 후**:
   - [ ] 모든 테스트가 통과하는가?
   - [ ] 테스트 커버리지가 기준을 만족하는가?
   - [ ] 테스트 코드가 읽기 쉬운가?
   - [ ] 중복된 테스트 코드가 없는가?

**검증 기준**:
- 테스트 가능한 모든 Java 파일에 대해 JUnit 단위 테스트가 작성되어야 함
- 테스트 커버리지가 최소 70% 이상이어야 함
- 핵심 비즈니스 로직의 테스트 커버리지가 최소 90% 이상이어야 함
- 모든 예외 케이스에 대한 테스트가 작성되어야 함
- 모든 테스트가 독립적으로 실행 가능해야 함
- 테스트 실행 시 모든 테스트가 통과해야 함
- **빌드 검증**: 테스트 실행 성공 (`./gradlew test` 명령이 성공해야 함)
- **빌드 검증**: 테스트 커버리지 기준 충족 (`./gradlew test jacocoTestReport` 실행 후 커버리지 리포트 확인)

