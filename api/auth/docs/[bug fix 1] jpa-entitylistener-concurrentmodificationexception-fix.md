# JPA EntityListener ConcurrentModificationException 버그 픽스 기술 분석

## 1. 핵심 개념

### 1.1 Hibernate ActionQueue

Hibernate ActionQueue는 `org.hibernate.engine.spi.ActionQueue` 클래스로 구현되며, 트랜잭션 내에서 실행될 데이터베이스 작업(INSERT, UPDATE, DELETE)을 순서대로 관리하는 큐 구조입니다. ActionQueue는 각 작업 유형별로 별도의 `ArrayList`를 유지하며, `insertions`, `updates`, `deletions` 등의 필드에 각각의 액션을 저장합니다.

`executeActions(ActionQueue.Executable executable)` 메서드는 등록된 액션들을 순회하며 실행합니다. 이 메서드는 내부적으로 `prepareActions()` 호출 후 `Collections.unmodifiableList()`로 래핑된 리스트의 Iterator를 사용합니다. 실행 순서는 다음과 같습니다: OrphanRemovalAction → EntityInsertAction → EntityUpdateAction → CollectionUpdateAction → CollectionRemoveAction → EntityDeleteAction.

Iterator 패턴을 사용하는 이유는 액션 실행 중 추가 액션이 발생할 수 있기 때문입니다. 하지만 Java의 fail-fast Iterator는 순회 중 컬렉션이 수정되면 `ConcurrentModificationException`을 발생시킵니다. `ArrayList.Itr` 내부의 `expectedModCount`와 `modCount`를 비교하는 `checkForComodification()`이 이를 감지합니다.

### 1.2 JPA EntityListener 라이프사이클

JPA 2.2 Specification (JSR 338) Section 3.5에 따르면, EntityListener 콜백 메서드는 다음 시점에 호출됩니다:

- `@PrePersist`: `EntityManager.persist()` 호출 직후, 실제 INSERT 전
- `@PostPersist`: INSERT 작업이 데이터베이스로 전송된 직후
- `@PreUpdate`: UPDATE 작업 전, flush 또는 commit 시점
- `@PostUpdate`: UPDATE 작업이 데이터베이스로 전송된 직후

`@PostPersist`는 Hibernate의 경우 `EntityInsertAction.execute()` 내부에서 `postInsert()` 호출 후 `eventListenerGroup.fireEventOnEachListener()`를 통해 실행됩니다. 이 시점에 엔티티의 ID는 이미 생성되어 있지만, ActionQueue는 여전히 순회 중입니다.

JPA Specification 3.5.2는 콜백 메서드 내에서 EntityManager 작업을 수행할 때의 제약사항을 명시하지 않지만, Hibernate 구현상 ActionQueue 순회 중 새로운 persist 호출은 `modCount` 변경을 유발하여 Iterator의 fail-fast 메커니즘을 트리거합니다.

### 1.3 Spring TransactionSynchronization

Spring Framework의 `TransactionSynchronizationManager`는 `org.springframework.transaction.support` 패키지에 속하며, 현재 스레드의 트랜잭션에 콜백을 등록하는 메커니즘을 제공합니다. `TransactionSynchronization` 인터페이스는 트랜잭션 라이프사이클의 특정 시점에 실행될 메서드를 정의합니다.

주요 콜백 메서드의 실행 순서는 다음과 같습니다:

1. `beforeCommit(boolean readOnly)`: 트랜잭션 커밋 전, 모든 변경사항이 flush된 후 실행
2. `beforeCompletion()`: 커밋 또는 롤백 전 실행
3. `afterCommit()`: 커밋 성공 후 실행
4. `afterCompletion(int status)`: 트랜잭션 완료 후 실행 (성공/실패 무관)

`AbstractPlatformTransactionManager.commit()` 메서드는 다음 순서로 실행됩니다:
1. `prepareForCommit()` - 동기화 객체의 `beforeCommit()` 호출
2. `doCommit()` - 실제 커밋 수행
3. `triggerAfterCommit()` - 동기화 객체의 `afterCommit()` 호출

중요한 점은 `beforeCommit()`이 Hibernate의 `SessionImpl.flushBeforeTransactionCompletion()` 이후, 실제 JDBC `Connection.commit()` 이전에 실행된다는 것입니다. 이 시점에는 ActionQueue의 모든 액션 실행이 완료되어 있어 새로운 persist 호출이 안전합니다.

### 1.4 ConcurrentModificationException

`java.util.ConcurrentModificationException`은 Java Collections Framework의 fail-fast Iterator가 감지하는 예외로, 컬렉션 순회 중 구조적 수정(structural modification)이 발생했을 때 던져집니다.

`ArrayList.Itr` 클래스는 `expectedModCount` 필드에 Iterator 생성 시점의 `ArrayList.modCount` 값을 저장합니다. `next()`, `remove()`, `forEachRemaining()` 호출 시마다 `checkForComodification()` 메서드가 실행되며, `modCount != expectedModCount`이면 예외를 발생시킵니다.

```java
// ArrayList.Itr 내부 코드
final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

`modCount`는 컬렉션의 구조가 변경될 때마다 증가합니다. `add()`, `remove()`, `clear()` 등의 메서드가 `modCount++`를 수행합니다. Hibernate ActionQueue의 경우, `entityManager.persist()`는 내부적으로 `ActionQueue.addAction()`을 호출하여 `insertions` 리스트에 새 액션을 추가하고 `modCount`를 변경합니다.

Collections.unmodifiableList()로 래핑된 리스트도 원본 리스트의 Iterator를 사용하므로, 원본이 수정되면 동일하게 예외가 발생합니다.

## 2. 버그 발생 원인 분석

버그는 다음 호출 스택에서 발생합니다:

```
JpaTransactionManager.doCommit()
  → TransactionImpl.commit()
    → SessionImpl.beforeTransactionCompletion()
      → SessionImpl.flushBeforeTransactionCompletion()
        → SessionImpl.managedFlush()
          → SessionImpl.doFlush()
            → ActionQueue.executeActions()
```

`ActionQueue.executeActions(ActionQueue.Executable executable)` 메서드는 다음과 같이 구현되어 있습니다:

```java
public <E extends Executable & Comparable<? super E> & Serializable> 
void executeActions(ExecutableList<E> list) throws HibernateException {
    if (list.isEmpty()) {
        return;
    }
    
    List<E> executableList = list.sort();
    for (E e : executableList) {  // Iterator 순회 시작
        execute(e);
    }
    list.clear();
}
```

EntityInsertAction의 경우 `execute()` 메서드 내부에서:

```java
// EntityInsertAction.execute()
public void execute() throws HibernateException {
    // 1. ID 생성
    generatedId = persister.insert(...);
    
    // 2. Entity의 ID 설정
    persister.setIdentifier(instance, generatedId, session);
    
    // 3. PostInsert 이벤트 발생
    postInsert();  // ← @PostPersist 콜백 실행
    
    // 4. 후처리
    markExecuted();
}
```

`postInsert()` 메서드는 `EventListenerGroup.fireEventOnEachListener()`를 통해 등록된 모든 EntityListener의 `@PostPersist` 메서드를 호출합니다. 이 시점에 `HistoryEntityListener.postPersist()`가 실행되며, 여기서 `entityManager.persist(history)`를 호출합니다.

`EntityManager.persist()` 호출은 다음 과정을 거칩니다:

```java
SessionImpl.persist()
  → SessionImpl.firePersist()
    → DefaultPersistEventListener.onPersist()
      → EntityInsertAction 생성
        → ActionQueue.addInsertAction()
          → insertions.add(action)  // ← modCount++
```

이때 `insertions` ArrayList에 새 액션이 추가되면서 `modCount`가 증가합니다. ActionQueue의 `executeActions()`로 돌아가면, for-each 루프(내부적으로 Iterator 사용)가 다음 요소를 가져오기 위해 `Iterator.next()`를 호출합니다:

```java
// ArrayList.Itr.next()
public E next() {
    checkForComodification();  // ← 여기서 예외 발생!
    // ...
}
```

`checkForComodification()`은 Iterator 생성 시점의 `expectedModCount`와 현재 `modCount`를 비교하여, 값이 다르면 `ConcurrentModificationException`을 던집니다.

결과적으로 트랜잭션은 롤백되고, User 엔티티와 UserHistory 엔티티 모두 데이터베이스에 저장되지 않습니다.

## 3. 해결 방안

### 3.1 시도한 방안들과 실패 이유

**방안 1: @PrePersist 사용**

```java
@PrePersist
public void prePersist(Object entity) {
    saveHistory(entity, "INSERT", null, entity);
}
```

실패 이유: `@PrePersist`는 ID 생성 전에 호출됩니다. TSID 생성기를 사용하는 경우 `@PrePersist` 단계에서 ID가 할당되지만, `UserHistoryEntity`가 참조하는 `UserEntity`는 아직 데이터베이스에 존재하지 않습니다. `user_history` 테이블의 FK 제약 조건(`FOREIGN KEY (user_id) REFERENCES users(id)`)에 의해 다음 오류가 발생합니다:

```
SQLIntegrityConstraintViolationException: Cannot add or update a child row: 
a foreign key constraint fails (`auth`.`user_history`, 
CONSTRAINT `fk_user_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`))
```

**방안 2: @PostPersist + 직접 persist**

```java
@PostPersist
public void postPersist(Object entity) {
    entityManager.persist(history);  // 직접 호출
}
```

실패 이유: Section 2에서 분석한 대로 `ConcurrentModificationException` 발생.

### 3.2 최종 해결 방안

```java
@PostPersist
public void postPersist(Object entity) {
    saveHistory(entity, "INSERT", null, entity);
}

private void saveHistory(Object entity, String operationType, 
                        Object beforeData, Object afterData) {
    // JSON 직렬화는 즉시 수행
    String beforeJson = beforeData != null ? 
        objectMapper.writeValueAsString(beforeData) : null;
    String afterJson = afterData != null ? 
        objectMapper.writeValueAsString(afterData) : null;
    
    // History 저장은 트랜잭션 동기화로 지연
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                EntityManager em = getEntityManager();
                em.persist(history);
            }
        }
    );
}
```

이 방식이 동작하는 이유:

1. **ActionQueue 격리**: `beforeCommit()`은 `ActionQueue.executeActions()` 완료 후 호출됩니다. Spring의 `AbstractPlatformTransactionManager.commit()` 메서드는 다음 순서로 실행됩니다:
   - `prepareForCommit()` → 이미 완료된 flush 작업
   - `triggerBeforeCommit()` → 여기서 `TransactionSynchronization.beforeCommit()` 실행
   - `doCommit()` → JDBC commit

2. **트랜잭션 원자성**: `beforeCommit()`에서 발생하는 `persist()` 호출은 새로운 flush를 트리거하지만, 이는 독립적인 ActionQueue 실행입니다. 실패 시 전체 트랜잭션이 롤백되어 원자성이 보장됩니다.

3. **FK 제약 조건 만족**: `beforeCommit()` 시점에는 User 엔티티가 이미 데이터베이스에 INSERT되어 있어 FK 참조가 가능합니다.

## 4. 기술적 트레이드오프

### 4.1 장점

**ActionQueue 독립성**: TransactionSynchronization을 사용하면 EntityListener 실행과 History 저장이 별도의 실행 흐름으로 분리됩니다. 원본 엔티티의 ActionQueue 실행은 방해받지 않으며, History 저장은 독립적인 flush 사이클에서 처리됩니다.

**트랜잭션 원자성 유지**: `beforeCommit()`은 JDBC `Connection.commit()` 이전에 실행되므로, History 저장 실패 시 전체 트랜잭션이 롤백됩니다. User 엔티티와 UserHistory 엔티티가 함께 저장되거나 함께 롤백되어 데이터 일관성이 보장됩니다.

**코드 침투성**: EntityListener는 도메인 엔티티에 대한 선언적 설정(`@EntityListeners(HistoryEntityListener.class)`)만으로 동작하며, 서비스 레이어 코드 수정이 불필요합니다.

### 4.2 제약사항

**실행 시점 지연**: History 저장이 원본 엔티티 저장보다 늦게 실행됩니다. `@PostPersist`에서 History의 `historyId`를 즉시 얻을 수 없으며, 이는 동기적 감사 로그 시나리오에서 제약이 될 수 있습니다.

**트랜잭션 동기화 의존성**: `TransactionSynchronizationManager.isSynchronizationActive()`가 false인 경우 History가 저장되지 않습니다. Spring 트랜잭션 관리가 활성화되지 않은 환경에서는 동작하지 않습니다.

**중첩 플러시**: `beforeCommit()`에서의 `persist()` 호출은 추가 flush를 트리거합니다. 대량의 엔티티 저장 시 flush 횟수가 증가하여 성능 영향이 있을 수 있습니다. Hibernate의 `hibernate.jdbc.batch_size` 설정이 적용되지만, 원본 엔티티와 History 엔티티의 배치가 분리됩니다.

## 참고 문헌

- JSR 338: Java Persistence API, Version 2.2 (https://jakarta.ee/specifications/persistence/2.2/)
- Hibernate ORM 6.6 User Guide (https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html)
- Spring Framework Reference Documentation - Transaction Management (https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- Java SE 21 API Specification - java.util.ConcurrentModificationException (https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ConcurrentModificationException.html)
- Hibernate ORM GitHub Repository (https://github.com/hibernate/hibernate-orm)
