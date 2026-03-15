# Phase 1: Level 1 자동화 - 데이터 수집 파이프라인 설계서 작성 프롬프트

## 목표
빅테크 AI 서비스(OpenAI, Anthropic, Google, Meta) 업데이트 정보를 수집하는 Spring Batch 기반 데이터 파이프라인 설계서를 작성한다.

## 배경
- 현재 프로젝트는 Spring Batch로 Codeforces, Kaggle 등 외부 소스 데이터를 수집하는 패턴이 정립되어 있음
- Reader → Processor → Writer 패턴을 동일하게 적용
- 기존 `client-feign`, `client-rss`, `client-scraper` 모듈 재사용

## 설계서에 포함할 내용

### 1. 데이터 소스 정의
아래 소스별로 수집 방식을 구체화:

| 소스 | URL | 수집 방식 | 클라이언트 모듈 |
|------|-----|----------|----------------|
| OpenAI Blog | https://openai.com/blog | RSS 또는 HTML 크롤링 | client-rss 또는 client-scraper |
| OpenAI GitHub | https://github.com/openai | GitHub API Releases | client-feign |
| Anthropic News | https://www.anthropic.com/news | HTML 크롤링 | client-scraper |
| Anthropic GitHub | https://github.com/anthropics | GitHub API Releases | client-feign |
| Google AI Blog | https://blog.google/technology/ai/ | RSS | client-rss |
| Meta AI Blog | https://ai.meta.com/blog/ | HTML 크롤링 | client-scraper |
| Meta GitHub | https://github.com/facebookresearch | GitHub API Releases | client-feign |

### 2. 엔티티/도큐먼트 스키마 설계
`AiUpdateEntity` (MariaDB) 및 `AiUpdateDocument` (MongoDB) 스키마 정의:
- id (TSID)
- provider (OPENAI, ANTHROPIC, GOOGLE, META)
- updateType (MODEL_RELEASE, API_UPDATE, SDK_RELEASE, RESEARCH_PAPER, BLOG_POST)
- title
- summary
- url
- publishedAt
- sourceType (RSS, GITHUB_RELEASE, WEB_SCRAPING)
- status (DRAFT, PENDING, PUBLISHED, REJECTED)
- metadata (JSON)
- createdAt, updatedAt

### 3. Batch Job 구조 설계
기존 `ContestCodeforcesJobConfig` 패턴을 참고하여:

```
batch/source/domain/ai-update/
├── openai/
│   ├── jobconfig/AiUpdateOpenAiJobConfig.java
│   ├── reader/OpenAiBlogRssReader.java
│   ├── reader/OpenAiGitHubReleaseReader.java
│   ├── processor/OpenAiUpdateProcessor.java
│   ├── writer/OpenAiUpdateWriter.java
│   └── listener/OpenAiJobListener.java
├── anthropic/
├── google/
└── meta/
```

### 4. Feign Client 추가 (GitHub Releases API)
기존 `GitHubFeignClient` 확장 또는 신규 메서드 추가:
```java
@GetMapping("/repos/{owner}/{repo}/releases")
List<GitHubDto.Release> getReleases(
    @PathVariable String owner,
    @PathVariable String repo,
    @RequestParam(defaultValue = "10") int perPage
);
```

공식 문서: https://docs.github.com/en/rest/releases/releases

### 5. 내부 API 엔드포인트 설계
`api-news` 또는 신규 `api-ai-update` 모듈에 추가:

```
POST /api/v1/ai-update/internal
POST /api/v1/ai-update/internal/batch
GET  /api/v1/ai-update
GET  /api/v1/ai-update/{id}
POST /api/v1/ai-update/{id}/approve
POST /api/v1/ai-update/{id}/reject
```

### 6. Slack 알림 통합
기존 `client-slack` 모듈을 활용하여 Draft 생성 시 알림:
- 채널: 설정 가능하도록 application.yml에서 관리
- 메시지 포맷: 제목, 요약, 소스, [승인] [거부] 버튼 (Interactive Message)

Slack Block Kit 공식 문서: https://api.slack.com/block-kit

### 7. 스케줄링 설정
- Spring @Scheduled 또는 외부 스케줄러(Jenkins, Kubernetes CronJob)
- 권장 주기: 1시간 ~ 6시간

## 제약 조건
- 기존 아키텍처 패턴(Reader-Processor-Writer) 준수
- 오버엔지니어링 금지: 현재 필요한 기능만 구현
- 모든 외부 API는 Rate Limit 고려 (GitHub: 5,000 req/hour)
- Robots.txt 준수 (웹 크롤링 시)

## 산출물
1. 엔티티/도큐먼트 클래스 설계
2. Batch Job 클래스 다이어그램
3. API 엔드포인트 명세
4. Feign Client 인터페이스 설계
5. 설정 파일(application.yml) 구조

## 참고 자료
- 기존 패턴: `batch/source/domain/contest/codeforces/`
- GitHub REST API: https://docs.github.com/en/rest
- Spring Batch: https://docs.spring.io/spring-batch/reference/
