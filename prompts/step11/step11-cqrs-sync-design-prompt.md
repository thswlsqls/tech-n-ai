# 11단계 CQRS 동기화 설계서 작성 프롬프트

## 작업 개요

**목적**: 11단계 "CQRS 패턴 기반 아키텍처 구현 - Kafka를 통한 Amazon Aurora MySQL과 MongoDB Atlas 동기화"를 위한 상세 설계서 작성

**출력 파일**: `docs/step11/cqrs-kafka-sync-design.md`

**참고 프롬프트**: `prompts/shrimp-task-prompt.md` (5000-5066라인)

---

## 1. 현재 프로젝트 상태 파악

### 1.1 필수 확인 사항

다음 항목들을 반드시 확인하고 설계서에 반영하세요:

#### 프로젝트 구조
- [ ] `common/kafka` 모듈의 전체 구조 파악
  - `publisher/EventPublisher.java` 구현 상태
  - `consumer/EventConsumer.java` 구현 상태
  - `event/` 패키지의 모든 이벤트 모델 확인
- [ ] `domain/mongodb` 모듈의 구조 파악
  - `document/` 패키지의 Document 클래스들
  - `repository/` 패키지의 Repository 인터페이스들
- [ ] `domain/aurora` 모듈의 구조 파악 (Command Side)
  - User, Archive 엔티티 구조
  - TSID Primary Key 전략

#### 이미 구현된 코드 분석
- [ ] `EventPublisher`의 현재 구현 상태 분석
  - 이벤트 발행 메서드 시그니처
  - Partition Key 처리 방식
  - 에러 핸들링 로직
- [ ] `EventConsumer`의 현재 구현 상태 분석
  - 멱등성 보장 로직 (Redis 기반)
  - `processEvent` 메서드의 현재 상태 (빈 구현 확인)
- [ ] 이벤트 모델 구조 분석
  - `BaseEvent` 인터페이스 구조
  - 각 이벤트 타입별 페이로드 구조
  - `UserCreatedEvent`, `UserUpdatedEvent`, `UserDeletedEvent`, `UserRestoredEvent`
  - `ArchiveCreatedEvent`, `ArchiveUpdatedEvent`, `ArchiveDeletedEvent`, `ArchiveRestoredEvent`
- [ ] MongoDB Repository 인터페이스 확인
  - `UserProfileRepository`의 메서드 시그니처
  - `ArchiveRepository`의 메서드 시그니처

#### 미구현 부분 식별
- [ ] 동기화 서비스 (`UserSyncService`, `ArchiveSyncService`) 존재 여부 확인
  - **주의**: `ContestSyncService`, `NewsArticleSyncService`는 배치 작업을 통해 직접 MongoDB에 저장되므로 Kafka 동기화 불필요 (제외)
- [ ] `EventConsumer.processEvent` 메서드의 실제 구현 여부 확인
- [ ] MongoDB Repository의 실제 구현 클래스 존재 여부 확인
- [ ] 이벤트 페이로드 구조 확인 (특히 `updatedFields`가 `Map<String, Object>`인 경우 처리 방법)

---

## 2. 참고 문서 및 설계 패턴

### 2.1 필수 참고 설계서

다음 설계서들을 반드시 참고하여 일관된 구조로 작성하세요:

1. **데이터 모델 설계서**
   - `docs/step2/2. data-model-design.md`
   - 특히 "실시간 동기화 전략" 섹션 (620-808라인)
   - User 엔티티 → UserProfileDocument 동기화 흐름
   - Archive 엔티티 → ArchiveDocument 동기화 흐름

2. **MongoDB 스키마 설계서**
   - `docs/step1/2. mongodb-schema-design.md`
   - UserProfileDocument 필드 구조 및 인덱스
   - ArchiveDocument 필드 구조 및 인덱스
   - TSID 필드 기반 매핑 전략

3. **Aurora 스키마 설계서**
   - `docs/step1/3. aurora-schema-design.md`
   - User 엔티티 구조
   - Archive 엔티티 구조
   - TSID Primary Key 전략

4. **배치 잡 통합 설계서 (참고용)**
   - `docs/step10/batch-job-integration-design.md`
   - 설계서 작성 패턴 및 구조 참고

5. **최종 프로젝트 목표**
   - `docs/reference/shrimp-task-prompts-final-goal.md`
   - CQRS 패턴 설계 원칙 (416-533라인)

### 2.2 설계서 작성 패턴

다음 설계서들의 구조를 참고하여 일관된 형식으로 작성하세요:

```
# [설계서 제목]

**작성 일시**: YYYY-MM-DD  
**대상 모듈**: [모듈명]  
**목적**: [설계서 목적]

## 목차

1. [개요](#개요)
2. [설계 원칙](#설계-원칙)
3. [현재 구현 상태 분석](#현재-구현-상태-분석)
4. [상세 설계](#상세-설계)
   - [아키텍처 설계](#아키텍처-설계)
   - [동기화 서비스 설계](#동기화-서비스-설계)
   - [이벤트 처리 로직 설계](#이벤트-처리-로직-설계)
5. [구현 가이드](#구현-가이드)
6. [검증 기준](#검증-기준)

---

## 개요

[설계서 개요 및 배경]

### 설계 원칙

1. [원칙 1]
2. [원칙 2]
...
```

---

## 3. 설계서 작성 요구사항

### 3.1 설계 원칙

다음 원칙들을 반드시 포함하세요:

1. **클린코드 원칙**
   - SOLID 원칙 준수
   - 단일 책임 원칙 (SRP): 각 서비스는 하나의 책임만 가짐
   - 의존성 역전 원칙 (DIP): 인터페이스 기반 설계
   - 개방-폐쇄 원칙 (OCP): 확장에는 열려있고 수정에는 닫혀있음

2. **객체지향 설계 기법**
   - 전략 패턴: 이벤트 타입별 처리 전략 분리
   - 팩토리 패턴: 이벤트 타입에 따른 동기화 서비스 선택
   - 템플릿 메서드 패턴: 공통 동기화 흐름 정의

3. **CQRS 패턴 원칙**
   - Command Side와 Query Side 완전 분리
   - 이벤트 기반 비동기 동기화
   - TSID 필드 기반 1:1 매핑 보장

4. **최소 구현 원칙**
   - 현재 필요한 기능만 구현
   - 오버엔지니어링 방지
   - 단계적 확장 가능한 구조

### 3.2 공식 문서 참고

다음 공식 문서만 참고하세요 (외부 블로그나 비공식 자료 금지):

1. **Spring Kafka 공식 문서**
   - https://docs.spring.io/spring-kafka/reference/html/
   - Producer/Consumer 설정 및 사용법
   - 트랜잭션 관리

2. **Apache Kafka 공식 문서**
   - https://kafka.apache.org/documentation/
   - Consumer API
   - 멱등성 보장

3. **Spring Data MongoDB 공식 문서**
   - https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
   - Repository 인터페이스 사용법
   - 쿼리 메서드

4. **MongoDB 공식 문서**
   - https://www.mongodb.com/docs/
   - 인덱스 전략
   - 트랜잭션 처리

### 3.3 베스트 프랙티스

다음 베스트 프랙티스를 반드시 포함하세요:

1. **Kafka 이벤트 처리**
   - 멱등성 보장 (이미 구현됨, 설계서에 명시)
   - 이벤트 순서 보장 (Partition Key 사용)
   - 에러 핸들링 및 재시도 전략

2. **MongoDB 동기화**
   - Upsert 패턴 사용 (생성/수정 통합)
   - 트랜잭션 처리 (필요 시)
   - 인덱스 활용 최적화

3. **서비스 설계**
   - 인터페이스 기반 설계
   - 단위 테스트 가능한 구조
   - 의존성 주입 활용

---

## 4. 설계서 필수 섹션

### 4.1 개요 섹션

다음 내용을 포함하세요:

- CQRS 패턴의 목적 및 배경
- 현재 구현 상태 요약
- 설계서의 범위 및 제외 사항

**제외 사항 명시**:
- Kafka 토픽 생성 (인프라 작업, 코드에 포함 불필요)
- 복잡한 DLQ 처리 (기본 재시도로 충분, 필요 시 후속 단계에서 추가)
- 복잡한 모니터링 시스템 (기본 로깅으로 충분)
- Contest/News 동기화 서비스 (배치 작업을 통해 직접 MongoDB에 저장되므로 Kafka 동기화 불필요)

### 4.2 현재 구현 상태 분석

다음 항목들을 상세히 분석하세요:

1. **이미 구현된 부분**
   - `EventPublisher` 구현 상태
   - `EventConsumer` 기본 구조
   - 이벤트 모델 구조
   - 멱등성 보장 로직

2. **미구현 부분**
   - 동기화 서비스 (`UserSyncService`, `ArchiveSyncService`)
     - **주의**: `ContestSyncService`, `NewsArticleSyncService`는 제외 (배치 작업으로 직접 MongoDB 저장)
   - `EventConsumer.processEvent` 실제 구현
   - 이벤트 타입별 처리 로직
   - `updatedFields` (Map<String, Object>) 처리 로직

3. **구현 우선순위**
   - 필수 구현 항목
   - 선택 구현 항목 (후속 단계에서 구현 가능)

### 4.3 상세 설계

#### 4.3.1 아키텍처 설계

다음 다이어그램을 포함하세요:

```
[Command Side]                    [Kafka]                    [Query Side]
Aurora MySQL ──이벤트 발행──> Kafka Topic ──이벤트 수신──> MongoDB Atlas
   User                              │                          UserProfileDocument
   Archive                           │                          ArchiveDocument
                                     │
                              EventConsumer
                              └─> SyncService
```

#### 4.3.2 동기화 서비스 설계

각 동기화 서비스에 대해 다음을 포함하세요:

1. **UserSyncService**
   - 인터페이스 정의
   - 메서드 시그니처:
     - `syncUserCreated(UserCreatedEvent)`
     - `syncUserUpdated(UserUpdatedEvent)` - `updatedFields` (Map<String, Object>) 처리 방법 설계 필요
     - `syncUserDeleted(UserDeletedEvent)`
     - `syncUserRestored(UserRestoredEvent)`
   - 구현 클래스 설계
   - MongoDB Repository 의존성
   - Upsert 패턴 구현 (생성/수정 통합)
   - `updatedFields` 처리 전략 (부분 업데이트 vs 전체 교체)
   - 에러 핸들링 전략

2. **ArchiveSyncService**
   - 인터페이스 정의
   - 메서드 시그니처:
     - `syncArchiveCreated(ArchiveCreatedEvent)`
     - `syncArchiveUpdated(ArchiveUpdatedEvent)` - `updatedFields` (Map<String, Object>) 처리 방법 설계 필요
     - `syncArchiveDeleted(ArchiveDeletedEvent)`
     - `syncArchiveRestored(ArchiveRestoredEvent)`
   - 구현 클래스 설계
   - MongoDB Repository 의존성
   - Upsert 패턴 구현 (생성/수정 통합)
   - `updatedFields` 처리 전략 (부분 업데이트 vs 전체 교체)
   - 에러 핸들링 전략

#### 4.3.3 이벤트 처리 로직 설계

`EventConsumer.processEvent` 메서드 구현 설계:

1. **이벤트 타입별 분기 처리**
   - 전략 패턴 또는 팩토리 패턴 활용
   - 이벤트 타입 → 동기화 서비스 매핑

2. **동기화 흐름**
   - 이벤트 수신 → 이벤트 타입 확인 → 적절한 동기화 서비스 호출
   - 에러 발생 시 처리 전략

3. **에러 핸들링**
   - 동기화 실패 시 로깅
   - 재시도 전략 (Spring Kafka 기본 재시도 활용)
   - 예외 전파 전략

### 4.4 구현 가이드

다음 항목들을 단계별로 설명하세요:

1. **동기화 서비스 인터페이스 생성**
   - 패키지 구조: `common/kafka/src/main/java/.../sync/`
   - 인터페이스 정의 예시

2. **동기화 서비스 구현 클래스 생성**
   - 구현 클래스 예시
   - MongoDB Repository 주입
   - Upsert 패턴 구현
   - `updatedFields` (Map<String, Object>) 처리 로직
     - 부분 업데이트: MongoDB의 `$set` 연산자 활용
     - 필드 타입 변환 (String → LocalDateTime 등)
     - null 값 처리 전략

3. **EventConsumer.processEvent 구현**
   - 이벤트 타입별 분기 로직
   - 동기화 서비스 호출

4. **의존성 주입 설정**
   - Spring Bean 등록
   - 순환 의존성 방지

### 4.5 검증 기준

다음 검증 기준을 포함하세요:

1. **기능 검증**
   - 모든 이벤트 타입에 대한 동기화 동작 확인
   - 멱등성 보장 확인
   - 에러 핸들링 동작 확인

2. **성능 검증**
   - 동기화 지연 시간 측정 (목표: 1초 이내)
   - 동시성 처리 확인

3. **빌드 검증**
   - `./gradlew :common-kafka:build` 성공
   - `./gradlew :domain-mongodb:build` 성공
   - `./gradlew clean build` 성공
   - 컴파일 에러 없음

---

## 5. 작성 시 주의사항

### 5.1 오버엔지니어링 방지

다음 항목들은 **설계서에 포함하지 마세요**:

- ❌ 복잡한 DLQ 처리 로직 (기본 재시도로 충분)
- ❌ 복잡한 모니터링 시스템 (기본 로깅으로 충분)
- ❌ Kafka 토픽 생성 스크립트 (인프라 작업)
- ❌ 불필요한 추상화 레이어
- ❌ 미래 확장을 위한 과도한 설계

### 5.2 최소 구현 원칙

다음 원칙을 준수하세요:

- ✅ 현재 필요한 기능만 설계
- ✅ 단순하고 명확한 구조
- ✅ 단계적 확장 가능한 구조
- ✅ 테스트 가능한 설계

### 5.3 코드 예시 작성 규칙

코드 예시 작성 시 다음을 준수하세요:

1. **인터페이스 정의**
   ```java
   /**
    * User 동기화 서비스 인터페이스
    */
   public interface UserSyncService {
       void syncUserCreated(UserCreatedEvent event);
       void syncUserUpdated(UserUpdatedEvent event);
       void syncUserDeleted(UserDeletedEvent event);
       void syncUserRestored(UserRestoredEvent event);
   }
   ```

2. **구현 클래스 예시**
   - 핵심 로직만 포함
   - 주석으로 설명 추가
   - 실제 구현 가능한 수준의 예시

3. **의존성 주입 예시**
   - `@Service`, `@RequiredArgsConstructor` 사용
   - Repository 주입 방식

---

## 6. 설계서 검증 체크리스트

설계서 작성 완료 후 다음을 확인하세요:

- [ ] 현재 구현 상태가 정확히 분석되었는가?
- [ ] 미구현 부분이 명확히 식별되었는가?
- [ ] 동기화 서비스 설계가 구체적인가?
- [ ] 이벤트 처리 로직이 명확한가?
- [ ] 구현 가이드가 단계별로 작성되었는가?
- [ ] 검증 기준이 구체적인가?
- [ ] 오버엔지니어링이 방지되었는가?
- [ ] 클린코드 원칙이 반영되었는가?
- [ ] 객체지향 설계 기법이 적용되었는가?
- [ ] 공식 문서만 참고했는가?
- [ ] 기존 설계서와 일관된 구조인가?

---

## 7. 참고 코드 위치

설계서 작성 시 다음 코드를 참고하세요:

### 7.1 Kafka 모듈
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/publisher/EventPublisher.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/consumer/EventConsumer.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/BaseEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserCreatedEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserUpdatedEvent.java` (updatedFields 구조 확인)
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserDeletedEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/UserRestoredEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveCreatedEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveUpdatedEvent.java` (updatedFields 구조 확인)
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveDeletedEvent.java`
- `common/kafka/src/main/java/com/ebson/shrimp/tm/demo/common/kafka/event/ArchiveRestoredEvent.java`

### 7.2 MongoDB 모듈
- `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/UserProfileDocument.java`
- `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/ArchiveDocument.java`
- `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/UserProfileRepository.java`
- `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/repository/ArchiveRepository.java`

---

## 8. 최종 확인

설계서 작성 완료 후 다음을 최종 확인하세요:

1. **완성도**
   - 모든 필수 섹션이 포함되었는가?
   - 각 섹션이 충분히 상세한가?

2. **정확성**
   - 현재 코드 상태와 일치하는가?
   - 설계가 실제 구현 가능한가?

3. **일관성**
   - 기존 설계서와 구조가 일치하는가?
   - 명명 규칙이 일관되는가?

4. **실용성**
   - 구현 가이드가 명확한가?
   - 검증 기준이 구체적인가?

---

**작성 완료 후**: `docs/step11/cqrs-kafka-sync-design.md` 파일로 저장하세요.
