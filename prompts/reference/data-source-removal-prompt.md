# Contest/News 수집 데이터 소스 제거 및 설계서 정합성 보정 프롬프트

## Role Definition

당신은 Spring Boot 멀티모듈 프로젝트 리팩토링 전문가입니다. 사용하지 않기로 결정된 수집 데이터 소스(Contest, News, Sources) 관련 코드를 완전히 제거하고, 설계서와 구현 코드의 정합성을 유지해야 합니다.

---

## Context & Background

### 프로젝트 개요

Shrimp Task Manager Demo는 Spring Boot 3.4.1 기반 멀티모듈 프로젝트로, Contest/News/Emerging Tech 데이터를 외부 소스에서 수집하여 CQRS 패턴으로 관리하는 구조입니다.

### 변경 방향

- **폐기**: Contest, News 수집 체계 전체 제거
- **폐기**: `SourcesDocument` 기반 수집 소스 관리 체계 제거
- **폐기**: `json/sources.json` 기반 소스 정의 제거
- **유지**: Emerging Tech 수집 체계 (api-emerging-tech, batch/emergingtech)

### AS-IS → TO-BE 비교

| 구분 | AS-IS (삭제 대상) | TO-BE (유지 대상) |
|------|-------------------|-------------------|
| **API 모듈** | api-contest (8084), api-news (8085) | api-emerging-tech, api-gateway, api-auth, api-bookmark, api-chatbot, api-agent |
| **배치 도메인** | batch/.../domain/contest/, batch/.../domain/news/ | batch/.../domain/emergingtech/ |
| **소스 관리** | batch/.../domain/sources/, json/sources.json | - (폐기) |

### ⚠️ 삭제 금지 대상 (반드시 유지)

아래 항목은 **절대 삭제하지 않습니다**:

| 모듈/경로 | 설명 |
|-----------|------|
| `api/emerging-tech/` | Emerging Tech API 모듈 전체 |
| `batch/source/src/main/java/.../domain/emergingtech/` | Emerging Tech 배치 수집 잡 |
| `domain/mongodb/.../document/EmergingTechDocument.java` | Emerging Tech MongoDB 도큐먼트 |
| `domain/mongodb/.../repository/EmergingTechRepository.java` | Emerging Tech MongoDB 레포지토리 |
| `domain/mongodb/.../enums/EmergingTechType.java` | Emerging Tech Enum |
| `client/feign/.../config/EmergingTechInternalFeignConfig.java` | Emerging Tech Feign 설정 |
| `client/feign/.../config/GitHubFeignConfig.java` | GitHub Feign 설정 (Emerging Tech에서 사용) |
| `client/feign/.../domain/github/` | GitHub Feign 클라이언트 (Emerging Tech에서 사용) |
| `client/feign/.../domain/internal/api/EmergingTechInternalApi.java` | Emerging Tech Internal API |
| `client/feign/.../domain/internal/client/EmergingTechInternalFeignClient.java` | Emerging Tech Internal Feign Client |
| `client/feign/.../domain/internal/contract/EmergingTechInternalContract.java` | Emerging Tech Internal Contract |

---

## Task 1: 코드 삭제 대상

### 1단계: API 모듈 삭제 (디렉토리 단위)

```
api/contest/          # api-contest 모듈 전체 삭제
api/news/             # api-news 모듈 전체 삭제
```

> ❌ `api/emerging-tech/` 삭제 금지

### 2단계: 배치 도메인 삭제 (디렉토리 단위)

```
batch/source/src/main/java/.../domain/contest/      # 12개 플랫폼(Codeforces, Kaggle 등) 수집 잡
batch/source/src/main/java/.../domain/news/          # 8개 뉴스 소스(HackerNews, DevTo 등) 수집 잡
batch/source/src/main/java/.../domain/sources/       # Sources 동기화 잡 (SourcesDocument 관련)
```

> ❌ `batch/.../domain/emergingtech/` 삭제 금지

### 3단계: Feign 클라이언트 삭제

**삭제 대상 Config 클래스** (`client/feign/src/main/java/.../config/`):

```
CodeforcesFeignConfig.java
ContestInternalFeignConfig.java
DevToFeignConfig.java
HackerNewsFeignConfig.java
KaggleFeignConfig.java
NewsAPIFeignConfig.java
NewsInternalFeignConfig.java
ProductHuntFeignConfig.java
RedditFeignConfig.java
```

> ❌ 유지: `EmergingTechInternalFeignConfig.java`, `GitHubFeignConfig.java`

**삭제 대상 도메인 클라이언트** (`client/feign/src/main/java/.../domain/`):

```
codeforces/     # API, Client, Contract, DTO, Mock
devto/          # API, Client, Contract, DTO, Mock
hackernews/     # API, Client, Contract, DTO, Mock
kaggle/         # API, Client, Contract, DTO, Mock
newsapi/        # API, Client, Contract, DTO, Mock
producthunt/    # API, Client, Contract, DTO, Mock
reddit/         # API, Client, Contract, DTO, Mock
```

> ❌ 유지: `github/` (Emerging Tech에서 GitHub Releases 수집에 사용)

**삭제 대상 Internal API 클라이언트** (`client/feign/src/main/java/.../domain/internal/`):

```
api/ContestInternalApi.java
api/NewsInternalApi.java
client/ContestInternalFeignClient.java
client/NewsInternalFeignClient.java
contract/ContestInternalContract.java
contract/NewsInternalContract.java
```

> ❌ 유지: `EmergingTechInternalApi.java`, `EmergingTechInternalFeignClient.java`, `EmergingTechInternalContract.java`
> ⚠️ `internal/contract/InternalApiDto.java` - Emerging Tech에서 사용 여부 확인 후 판단

**삭제 대상 Feign YAML 설정** (`client/feign/src/main/resources/`):

```
application-feign-codeforces.yml
application-feign-devto.yml
application-feign-hackernews.yml
application-feign-kaggle.yml
application-feign-newsapi.yml
application-feign-producthunt.yml
application-feign-reddit.yml
```

> ❌ 유지: `application-feign-github.yml`, `application-feign-internal.yml` (Emerging Tech 사용)

### 4단계: MongoDB 도큐먼트 및 레포지토리 삭제

**삭제 대상 도큐먼트** (`domain/mongodb/src/main/java/.../document/`):

```
ContestDocument.java
NewsArticleDocument.java
SourcesDocument.java
```

> ❌ 유지: `EmergingTechDocument.java`

**삭제 대상 레포지토리** (`domain/mongodb/src/main/java/.../repository/`):

```
ContestRepository.java
NewsArticleRepository.java
SourcesRepository.java
```

> ❌ 유지: `EmergingTechRepository.java`

**삭제 대상 Enum** (`domain/mongodb/src/main/java/.../enums/`):

```
SourceType.java
```

> ❌ 유지: `EmergingTechType.java`

### 5단계: JSON 데이터 파일 삭제

```
json/sources.json
batch/source/src/main/resources/json/sources.json
```

---

## Task 2: 설정 파일 수정 대상

### 1. Gateway 라우팅 설정 수정

**대상 파일**:
- `api/gateway/src/main/resources/application.yml`
- `api/gateway/src/main/resources/application-local.yml`
- `api/gateway/src/main/resources/application-dev.yml`
- `api/gateway/src/main/resources/application-beta.yml`
- `api/gateway/src/main/resources/application-prod.yml`

**작업**: Contest, News 관련 라우트 정의 및 URI 설정 제거. Emerging Tech 라우트는 유지.

### 2. Gradle 빌드 설정 수정

**대상 파일**:
- `build.gradle` (루트) - api-contest, api-news 모듈 참조 제거
- `settings.gradle` - 삭제된 모듈 include 제거 (자동 탐색 방식이면 디렉토리 삭제로 충분)

> ❌ api-emerging-tech 참조 유지

### 3. 배치 모듈 설정 수정

**대상 파일**:
- `batch/source/src/main/java/.../BatchSourceApplication.java` - contest/news/sources 잡 import/참조 제거
- `batch/source/src/main/java/.../config/BatchConfig.java` - contest/news/sources 잡 설정 제거
- `batch/source/src/main/java/.../common/Constants.java` - contest/news/sources 잡 상수 제거
- `batch/source/src/main/resources/application.yml` - 삭제된 프로파일 include 제거
- `batch/source/src/main/resources/application-local.yml` - 삭제된 설정 제거

> ❌ emergingtech 잡 설정 유지

### 4. Feign 빌드/설정 수정

**대상 파일**:
- `client/feign/build.gradle` - 삭제된 의존성 제거 (GitHub, Emerging Tech 의존성 유지)
- `client/feign/src/test/java/.../FeignTestContext.java` - 삭제된 테스트 참조 제거

### 5. Kafka 이벤트/핸들러 수정

**대상 파일** (`common/kafka/`):
- Contest/News 관련 이벤트 클래스 삭제 확인 (이미 git에서 삭제된 상태일 수 있음)
- `EventConsumer.java` - 삭제된 이벤트 핸들러 참조 제거
- `EventHandlerRegistry.java` - 삭제된 핸들러 등록 제거

> ❌ Emerging Tech 관련 이벤트/핸들러는 유지

---

## Task 3: 설계서 수정 대상

아래 설계서에서 **Contest/News/Sources 관련 내용만** 제거하거나 수정합니다. **Emerging Tech 관련 내용은 유지**합니다.

### 필수 수정 설계서

| 설계서 경로 | 수정 내용 |
|-------------|-----------|
| `docs/step1/2. mongodb-schema-design.md` | SourcesDocument, ContestDocument, NewsArticleDocument 스키마 섹션 제거. EmergingTechDocument 유지 |
| `docs/step1/3. aurora-schema-design.md` | Contest, News 관련 테이블 정의 제거 (존재 시) |
| `docs/step2/1. api-endpoint-design.md` | api-contest, api-news 엔드포인트 섹션 제거. api-emerging-tech 유지 |
| `docs/step2/2. data-model-design.md` | Contest, News 데이터 모델 섹션 제거. Emerging Tech 유지 |
| `docs/step8/rss-scraper-modules-analysis.md` | 데이터 수집 대상에서 Contest/News 소스 제거. Emerging Tech RSS/Scraper 소스 유지 |
| `docs/step9/contest-news-api-design.md` | 전체 문서 삭제 또는 "폐기됨" 표시 |
| `docs/step10/batch-job-integration-design.md` | Contest/News 배치 잡 설계 섹션 제거. Emerging Tech 배치 잡 유지 |
| `docs/step11/cqrs-kafka-sync-design.md` | Contest/News 이벤트 동기화 섹션 제거. Emerging Tech 이벤트 유지 |
| `docs/step15/sources-sync-batch-job-design.md` | 전체 문서 삭제 또는 "폐기됨" 표시 |
| `docs/reference/API-SPECIFICATION.md` | Contest, News API 엔드포인트 섹션 제거. Emerging Tech API 유지 |

### 수정 시 주의사항

- 설계서에서 섹션을 제거할 때, 문서 전체 구조(목차, 번호 매기기)가 깨지지 않도록 조정
- 제거된 기능에 대한 참조가 다른 섹션에 남아있지 않도록 교차 검증
- **Emerging Tech 관련 내용이 실수로 삭제되지 않도록 주의**
- RAG 챗봇 설계서(`docs/step12/`)에서 Contest/News 데이터를 벡터 검색 대상으로 참조하는 부분이 있으면 수정 (Emerging Tech 참조는 유지)

---

## Constraints (제한 조건)

1. **오버엔지니어링 금지**: 삭제 대상만 정확히 제거. 불필요한 리팩토링, 추가 기능 구현 금지
2. **최소 한글 주석**: 수정되는 코드에 변경 사유를 1줄 한글 주석으로 남김
3. **SOLID 원칙 준수**: 제거 후 남은 코드가 단일 책임 원칙, 인터페이스 분리 원칙을 위반하지 않도록 확인
4. **빌드 검증**: 모든 삭제/수정 후 `./gradlew clean build` 성공 확인
5. **설계-구현 정합성**: 설계서에 기술된 모듈/클래스/API가 실제 코드에 존재하는지 교차 검증
6. **Emerging Tech 무결성**: 삭제 작업이 Emerging Tech 기능에 영향을 주지 않는지 반드시 확인

---

## Verification Checklist

작업 완료 후 아래 항목을 검증합니다:

- [ ] `./gradlew clean build` 성공
- [ ] api-contest, api-news 모듈 디렉토리 완전 삭제
- [ ] `api/emerging-tech/` 모듈 정상 존재 및 빌드 성공
- [ ] batch contest/news/sources 도메인 디렉토리 완전 삭제
- [ ] `batch/.../domain/emergingtech/` 정상 존재 및 빌드 성공
- [ ] 삭제된 Feign 클라이언트에 대한 참조가 남아있지 않음
- [ ] GitHub Feign 클라이언트 및 Emerging Tech Internal Feign 클라이언트 정상 동작
- [ ] 삭제된 MongoDB 도큐먼트/레포지토리에 대한 참조가 남아있지 않음
- [ ] EmergingTechDocument, EmergingTechRepository 정상 존재
- [ ] Gateway 라우팅에서 contest/news 경로 제거, emerging-tech 경로 유지
- [ ] 설계서와 실제 코드의 정합성 확인
- [ ] Kafka 이벤트/핸들러에서 삭제된 이벤트 참조 제거 완료
- [ ] json/sources.json 파일 삭제 완료

---

## Execution Order (권장 작업 순서)

1. **코드 삭제**: 1단계(API) → 2단계(배치) → 3단계(Feign) → 4단계(MongoDB) → 5단계(JSON)
2. **설정 파일 수정**: Gateway → Gradle → Batch → Feign → Kafka
3. **빌드 검증**: `./gradlew clean build`
4. **Emerging Tech 검증**: api-emerging-tech 모듈 단독 빌드 및 배치 잡 참조 정상 확인
5. **설계서 수정**: 필수 수정 설계서 목록 순서대로  
6. **최종 검증**: Verification Checklist 전체 항목 확인
