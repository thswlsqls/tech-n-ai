# History 관리 서비스 레이어 리팩토링 설계 프롬프트

## 배경 및 목표

현재 `HistoryEntityListener`는 JPA 스펙 권장사항을 위반하고 있습니다:
- EntityListener 내부에서 다른 엔티티의 영속성 상태 변경 (`persist()` 호출)
- EntityListener 내부에서 트랜잭션 제어 로직 수행 (`TransactionSynchronizationManager` 사용)

**목표**: EntityListener를 제거하고 서비스 레이어에서 History 저장을 명시적으로 처리하는 설계서를 작성하세요.

## 제약사항

### 필수 준수 사항

1. **클린코드 원칙**
   - 메서드는 단일 책임만 수행
   - 의미 있는 이름 사용
   - 중복 코드 제거 (DRY)
   - 매직 넘버/문자열 제거

2. **객체지향 설계**
   - 캡슐화: 내부 구현은 숨기고 인터페이스만 노출
   - 다형성: 인터페이스 기반 설계
   - 상속보다 조합 우선

3. **SOLID 원칙**
   - **S**ingle Responsibility: 각 클래스는 하나의 책임만
   - **O**pen/Closed: 확장에는 열려있고 수정에는 닫혀있음
   - **L**iskov Substitution: 하위 타입은 상위 타입을 대체 가능
   - **I**nterface Segregation: 클라이언트는 사용하지 않는 인터페이스에 의존하지 않음
   - **D**ependency Inversion: 고수준 모듈은 저수준 모듈에 의존하지 않음

4. **기능 보존**
   - 현재 `HistoryEntityListener`의 모든 기능 유지
   - INSERT, UPDATE, DELETE (soft delete) 시 History 저장
   - BeforeData 조회 로직 유지
   - JSON 직렬화 로직 유지
   - 트랜잭션 원자성 보장 (History 저장 실패 시 원본 엔티티도 롤백)

5. **참고 자료 제한**
   - JPA 2.2 Specification (JSR 338) 공식 문서만 참고
   - Spring Framework 공식 문서만 참고
   - Hibernate ORM 공식 문서만 참고
   - 추측이나 비공식 자료 사용 금지

### 금지 사항

1. **오버엔지니어링 금지**
   - 불필요한 추상화 계층 추가 금지
   - 현재 요구사항에 없는 기능 추가 금지
   - 과도한 디자인 패턴 적용 금지

2. **불필요한 작업 금지**
   - 기존 코드의 불필요한 리팩토링 금지
   - 현재 동작하는 기능의 변경 금지
   - History 저장 기능 외의 추가 기능 구현 금지

3. **장황한 주석 금지**
   - 코드 자체로 설명 가능한 경우 주석 금지
   - JavaDoc은 public API에만 작성
   - 구현 세부사항 주석 금지

## 현재 구조 분석

### 엔티티 구조
- `BaseEntity`: 모든 엔티티의 기본 클래스 (id, isDeleted, deletedAt, createdAt, updatedAt 등)
- `UserEntity`, `AdminEntity`, `BookmarkEntity`: History 저장 대상 엔티티
- `UserHistoryEntity`, `AdminHistoryEntity`, `BookmarkHistoryEntity`: History 엔티티

### WriterRepository 구조
- `UserWriterRepository`, `AdminWriterRepository`, `BookmarkWriterRepository`: 각각 `save()`, `saveAndFlush()`, `delete()`, `deleteById()` 메서드 제공
- 모든 WriterRepository는 `@Service`로 등록되어 있음

### 현재 HistoryEntityListener 기능
1. `@PostPersist`: INSERT 시 History 저장 (operationType = "INSERT", beforeData = null)
2. `@PreUpdate`: UPDATE/DELETE 시 History 저장
   - Soft delete 감지: `isDeleted = true`이고 이전 상태가 `isDeleted = false`인 경우 operationType = "DELETE"
   - 일반 UPDATE: operationType = "UPDATE"
   - BeforeData 조회: `entityManager.find()`로 변경 전 데이터 조회
3. JSON 직렬화: ObjectMapper를 사용하여 beforeData, afterData를 JSON 문자열로 변환
4. TransactionSynchronization: `beforeCommit()` 시점에 History 저장

## 설계서 작성 요구사항

### 1. 설계서 구조

다음 섹션을 포함하세요:

#### 1.1 개요
- 리팩토링 목적
- 현재 문제점 요약
- 해결 방안 요약

#### 1.2 아키텍처 설계
- 전체 구조도 (클래스 다이어그램)
- 컴포넌트 간 의존성 관계
- 패키지 구조

#### 1.3 핵심 컴포넌트 설계

**1.3.1 HistoryService 인터페이스**
- 메서드 시그니처 정의
- 파라미터 및 반환값 타입
- 예외 처리 전략

**1.3.2 HistoryService 구현체**
- 구현 로직 개요
- BeforeData 조회 전략
- JSON 직렬화 처리
- 트랜잭션 처리

**1.3.3 WriterRepository 수정**
- 각 WriterRepository의 수정 사항
- HistoryService 의존성 주입
- save(), delete() 메서드 수정 로직

#### 1.4 데이터 흐름
- INSERT 시나리오
- UPDATE 시나리오
- DELETE (soft delete) 시나리오
- 각 시나리오별 메서드 호출 순서

#### 1.5 트랜잭션 처리
- 트랜잭션 경계
- 원자성 보장 방법
- 실패 처리 전략

#### 1.6 마이그레이션 계획
- EntityListener 제거 단계
- WriterRepository 수정 단계
- 테스트 전략

### 2. 설계 원칙 적용

#### 2.1 Single Responsibility Principle
- HistoryService: History 저장만 담당
- WriterRepository: 엔티티 저장 + History 저장 호출
- 각 클래스의 책임을 명확히 정의

#### 2.2 Open/Closed Principle
- 새로운 엔티티 타입 추가 시 확장 가능한 구조
- HistoryService 인터페이스 기반 설계로 구현체 교체 가능

#### 2.3 Dependency Inversion Principle
- WriterRepository는 HistoryService 인터페이스에 의존
- 구체 구현체가 아닌 인터페이스에 의존

#### 2.4 Interface Segregation Principle
- HistoryService 인터페이스는 필요한 메서드만 포함
- 클라이언트가 사용하지 않는 메서드 포함 금지

### 3. 구현 세부사항

#### 3.1 BeforeData 조회 전략
- UPDATE/DELETE 시 변경 전 데이터 조회 방법
- `entityManager.find()` 사용 시점
- 조회 실패 시 처리

#### 3.2 JSON 직렬화
- ObjectMapper 설정 (JavaTimeModule, SerializationFeature)
- 직렬화 실패 시 처리

#### 3.3 엔티티 타입별 처리
- UserEntity, AdminEntity, BookmarkEntity 각각에 대한 History 엔티티 생성 로직
- 타입별 분기 처리 방법 (instanceof vs 다형성)

#### 3.4 에러 처리
- History 저장 실패 시 원본 트랜잭션 롤백
- 예외 전파 전략

### 4. 검증 기준

설계서는 다음을 만족해야 합니다:

1. **기능 완전성**
   - 현재 HistoryEntityListener의 모든 기능이 서비스 레이어로 이동
   - INSERT, UPDATE, DELETE 시나리오 모두 처리
   - BeforeData 조회 로직 포함

2. **트랜잭션 원자성**
   - History 저장 실패 시 원본 엔티티 저장도 롤백
   - 같은 트랜잭션 내에서 처리

3. **코드 품질**
   - SOLID 원칙 준수
   - 중복 코드 없음
   - 명확한 책임 분리

4. **확장성**
   - 새로운 엔티티 타입 추가 시 최소한의 변경으로 확장 가능

5. **유지보수성**
   - 코드 가독성
   - 테스트 가능한 구조

## 출력 형식

Markdown 형식으로 작성하세요. 다음 구조를 따르세요:

```markdown
# History 관리 서비스 레이어 리팩토링 설계서

## 1. 개요
...

## 2. 아키텍처 설계
...

## 3. 핵심 컴포넌트 설계
...

## 4. 데이터 흐름
...

## 5. 트랜잭션 처리
...

## 6. 마이그레이션 계획
...

## 7. 참고 자료
- JPA 2.2 Specification (JSR 338)
- Spring Framework Reference Documentation
- Hibernate ORM User Guide
```

## 주의사항

1. **구현 코드 작성 금지**: 설계서만 작성하고 실제 구현 코드는 포함하지 마세요.
2. **의사코드 사용**: 필요한 경우 의사코드(pseudocode)로 로직을 설명하세요.
3. **클래스 다이어그램**: PlantUML 또는 Mermaid 형식으로 작성하세요.
4. **구체성**: 추상적인 설명보다 구체적인 설계를 제공하세요.
5. **검증 가능성**: 설계가 검증 가능하도록 명확한 기준을 제시하세요.

## 시작 지시

위 요구사항에 따라 설계서를 작성하세요. 각 섹션을 명확하고 구체적으로 작성하고, SOLID 원칙과 클린코드 원칙을 준수하는 설계를 제시하세요.
