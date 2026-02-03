# SourcesSyncJob Step2 Redis 캐싱 구현 설계서 작성 프롬프트

## Role Definition

당신은 Spring Batch와 Redis 캐싱 전문가입니다. 주어진 요구사항과 기존 코드 구조를 분석하여 실무에 바로 적용 가능한 설계서를 작성해야 합니다.

---

## Context & Background

### 프로젝트 구조

```
SourcesSyncJob (배치 잡)
├── Step1: JSON 파일 → MongoDB sources 컬렉션 저장 (✅ 구현 완료)
└── Step2: MongoDB sources 컬렉션 → Redis 캐시 저장 (🎯 설계 대상)
```

### 기존 구현 파일 (Step1 참고용)

**위치**: `batch/source/src/main/java/com/tech/n/ai/batch/source/domain/sources/sync/`

```
jobconfig/SourcesSyncJobConfig.java  (Step1 구현됨, Step2 추가 필요)
reader/SourcesJsonItemReader.java
processor/SourcesSyncProcessor.java
writer/SourcesMongoWriter.java
```

### 데이터 모델

**SourcesDocument** (`domain/mongodb/src/main/java/.../document/SourcesDocument.java`)

```java
@Document(collection = "sources")
public class SourcesDocument {
    @Id private ObjectId id;              // MongoDB ObjectId (Redis value로 사용)
    @Field("url") private String url;     // Redis key 1
    @Field("category") private String category;  // Redis key 2
    @Field("name") private String name;
    // ... 기타 필드 생략
}
```

### Redis 설정 정보

**RedisTemplate Bean**: `common/core/src/main/java/.../config/RedisConfig.java`
- `redisTemplate(RedisConnectionFactory)`: String-String 직렬화
- `redisTemplateForObjects(RedisConnectionFactory)`: String-Object JSON 직렬화

**Sources Redis 키 규칙**:
- **패턴**: `{url}:{category}`
- URL과 카테고리의 복합 키 사용
- 예시: `https://codeforces.com:contest` → `507f1f77bcf86cd799439011`

---

## Requirements

### 기능 요구사항

1. **Step2 ItemReader 구현**
   - MongoDB `sources` 컬렉션의 **모든 도큐먼트**를 읽어야 함
   - Spring Batch의 `MongoItemReader` 또는 이와 유사한 메커니즘 사용
   - 페이징 처리 적용 (청크 사이즈 고려)

2. **Step2 ItemWriter 구현**
   - 읽어온 각 `SourcesDocument`에 대해 Redis에 **1개의 키-값 쌍** 저장:
     - Key: `{url}:{category}` → Value: `{sourceId}` (ObjectId 문자열)
   - `RedisTemplate<String, String>` 사용 (문자열 직렬화)

3. **Step2 JobConfig 통합**
   - 기존 `SourcesSyncJobConfig`에 Step2 추가
   - Step1 → Step2 순차 실행 (`.next()` 메서드 사용)
   - Bean 이름 규칙 준수: `{JOB_NAME}+{STEP_NAME}+{COMPONENT_TYPE}` (Constants 클래스 참고)

### 비기능 요구사항

1. **키 네이밍 규칙 준수**
   - 복합 키 패턴: `{url}:{category}`
   - URL과 카테고리를 콜론으로 연결
   - 예시: `https://codeforces.com:contest`

2. **코드 품질 원칙**
   - 클린코드: 명확한 변수명, 단일 책임 원칙
   - 객체지향: 적절한 캡슐화, 역할 분리
   - SOLID: 특히 SRP(단일 책임), DIP(의존성 역전)

3. **금지 사항**
   - ❌ LLM 스타일의 장황한 주석 (예: "이 메서드는 ... 합니다" 반복)
   - ❌ 불필요한 추상화 레이어 추가
   - ❌ 요구사항에 없는 부가 기능 (로깅 제외)
   - ❌ 공식 문서에 없는 비표준 API 사용

---

## Design Constraints

### 참고해야 할 공식 문서

1. **Spring Batch 5.x**: https://docs.spring.io/spring-batch/docs/current/reference/html/
   - Chapter 6: ItemReaders and ItemWriters
   - `MongoItemReader` 설정 방법

2. **Spring Data Redis**: https://docs.spring.io/spring-data/redis/reference/
   - RedisTemplate 사용법
   - 키-값 저장 메서드 (`opsForValue().set()`)

3. **MongoDB Java Driver**: https://www.mongodb.com/docs/drivers/java/sync/current/
   - ObjectId 문자열 변환 (`ObjectId.toString()`)

### 기술 스택 버전

- Java 21
- Spring Boot 3.x
- Spring Batch 5.x
- Spring Data MongoDB 4.x
- Spring Data Redis 3.x

---

## Expected Output Format

설계서는 다음 섹션으로 구성되어야 합니다:

### 1. 개요
- Step2의 목적 및 역할
- 데이터 흐름 다이어그램 (텍스트 기반)

### 2. 컴포넌트 설계

#### 2.1 ItemReader (`SourcesMongoItemReader`)
- 클래스명, 패키지 경로
- 상속/구현: `MongoItemReader<SourcesDocument>` 또는 커스텀 Reader
- 설정값:
  - MongoDB 쿼리 (전체 조회)
  - 정렬 기준 (옵션)
  - 페이지 사이즈

#### 2.2 ItemProcessor (선택적)
- 필요성 판단 (Reader → Writer 직접 연결 가능 시 생략 가능)
- 필요 시: 데이터 변환 로직 명시

#### 2.3 ItemWriter (`SourcesRedisWriter`)
- 클래스명, 패키지 경로
- 의존성: `RedisTemplate<String, String>`
- Redis 저장 로직:
  ```
  입력: List<SourcesDocument> items
  처리:
    for each item:
      sourceId = item.getId().toString()
      key = item.url + ":" + item.category
      set key=key value=sourceId
  ```

#### 2.4 JobConfig 수정
- `SourcesSyncJobConfig` 변경 사항:
  - Step2 Bean 정의
  - Job 빌더에 `.next(step2)` 추가

### 3. 키 설계

| 키 형식 | 예시 | Value 예시 | 용도 |
|---------|------|-----------|------|
| `{url}:{category}` | `https://codeforces.com:contest` | `507f1f77bcf86cd799439011` | URL+카테고리로 소스 ID 조회 |

### 4. 구현 가이드

#### 4.1 디렉토리 구조
```
batch/source/src/main/java/.../domain/sources/sync/
├── jobconfig/SourcesSyncJobConfig.java (수정)
├── reader/SourcesMongoItemReader.java (신규)
└── writer/SourcesRedisWriter.java (신규)
```

#### 4.2 Bean 이름 규칙
- Reader: `{Constants.SOURCES_SYNC}+{Constants.STEP_2}+{Constants.ITEM_READER}`
- Writer: `{Constants.SOURCES_SYNC}+{Constants.STEP_2}+{Constants.ITEM_WRITER}`

#### 4.3 코드 스켈레톤 (핵심 메서드만)
```java
// SourcesRedisWriter.java 핵심 로직 예시
@Override
public void write(Chunk<? extends SourcesDocument> chunk) {
    for (SourcesDocument doc : chunk) {
        String sourceId = doc.getId().toString();
        String key = doc.getUrl() + ":" + doc.getCategory();
        
        redisTemplate.opsForValue().set(key, sourceId);
    }
}
```

### 5. 검증 방법
- 단위 테스트 전략 (Testcontainers Redis)
- 통합 테스트 전략 (실제 MongoDB + Redis)
- 수동 검증 명령어:
  ```bash
  # Redis CLI로 확인
  redis-cli GET "https://codeforces.com:contest"
  redis-cli KEYS "*:*"
  ```

### 6. 운영 고려사항
- Redis 메모리 사용량 추정 (sources 컬렉션 문서 수 기반)
- TTL 설정 필요성 판단
- 에러 처리 (중복 키, Redis 연결 실패 등)

### 7. 참고 자료
- 관련 설계서 링크
- 공식 문서 링크 (실제 존재하는 URL만)

---

## Execution Guidelines

### Step-by-Step 설계 프로세스

1. **현재 코드 분석**
   - `SourcesSyncJobConfig.java` 의 Step1 구현 패턴 파악
   - `Constants.java` 의 네이밍 규칙 확인
   - `RedisConfig.java` 의 Bean 이름 확인

2. **컴포넌트 설계**
   - MongoItemReader 설정 방법 결정 (공식 문서 참조)
   - RedisWriter의 배치 저장 로직 설계 (Pipeline 또는 개별 SET)

3. **통합 설계**
   - Step1 → Step2 연결 방법
   - 트랜잭션 경계 설정 (MongoDB는 트랜잭션, Redis는 비트랜잭션)

4. **검증 계획 수립**
   - 어떤 테스트가 필요한가?
   - 어떤 데이터로 검증할 것인가?

### 품질 체크리스트

설계서 작성 완료 후 다음 항목을 확인하세요:

- [ ] 모든 컴포넌트의 클래스명과 패키지 경로가 명시되었는가?
- [ ] Redis 키 네이밍이 프로젝트 규칙을 따르는가?
- [ ] 공식 문서 링크가 정확하고 접근 가능한가?
- [ ] 불필요한 추상화나 복잡한 패턴이 제거되었는가?
- [ ] 코드 예시가 실제 컴파일 가능한 수준인가?
- [ ] 에러 처리 전략이 명시되었는가?

---

## Example (Few-Shot Learning)

### 참고: Step1 Writer 구현 패턴

**파일**: `batch/source/.../writer/SourcesMongoWriter.java`

```java
@Slf4j
@RequiredArgsConstructor
public class SourcesMongoWriter implements ItemWriter<SourcesDocument> {
    
    private final MongoTemplate mongoTemplate;
    
    @Override
    public void write(Chunk<? extends SourcesDocument> chunk) {
        List<SourcesDocument> items = chunk.getItems();
        
        for (SourcesDocument item : items) {
            mongoTemplate.save(item, "sources");
        }
        
        log.info("Saved {} sources to MongoDB", items.size());
    }
}
```

**분석**:
- `ItemWriter<T>` 인터페이스 구현
- `write(Chunk<?>)` 메서드 오버라이드
- 의존성 주입: `MongoTemplate`
- 단순 반복문으로 배치 저장
- 간결한 로그 (LLM 스타일 주석 없음)

**Step2 Writer는 이 패턴을 따르되, `MongoTemplate` 대신 `RedisTemplate` 사용**

---

## Constraints & Boundaries

### 해도 되는 것 (DO)
✅ Spring Batch/Redis/MongoDB 공식 API 사용  
✅ 간결한 로그 메시지 (`log.info()`)  
✅ 예외 처리 (checked exception wrapping)  
✅ Null 체크 (방어적 프로그래밍)  

### 하지 말아야 할 것 (DON'T)
❌ Reactive 스택 사용 (프로젝트는 imperative 방식)  
❌ Redis Pub/Sub, Streams 같은 고급 기능  
❌ 커스텀 Serializer 구현 (기존 `StringRedisSerializer` 사용)  
❌ AOP 기반 캐싱 (`@Cacheable` 등)  
❌ "이 코드는 ... 역할을 합니다" 같은 주석  

---

## Final Notes

- 설계서는 **실무 개발자가 즉시 구현 가능한 수준**이어야 합니다.
- 모호한 표현 대신 **구체적인 클래스명, 메서드명, 설정값**을 명시하세요.
- 공식 문서 인용 시 **정확한 URL과 섹션명**을 제공하세요.
- 코드 예시는 **실제 컴파일 가능한 코드**만 작성하세요.

**설계 목표**: "이 문서만 보고 30분 안에 구현 가능한 설계서"

---

## 설계서 작성 시작

위의 모든 가이드라인을 따라, **SourcesSyncJob Step2 Redis 캐싱 구현 설계서**를 작성하세요.
