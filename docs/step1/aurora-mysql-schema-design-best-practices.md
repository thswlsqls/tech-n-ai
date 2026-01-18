# Amazon Aurora MySQL 테이블 설계 베스트 프랙티스

## 연구 메타데이터
- 연구 날짜: 2025-01-27
- Aurora MySQL 버전: 3.x
- MySQL 버전: 8.0+
- 연구자: Aurora MySQL Database Architect

## 목차
1. [테이블 설계 원칙](#1-테이블-설계-원칙)
2. [인덱스 설계 전략](#2-인덱스-설계-전략)
3. [파티셔닝 전략](#3-파티셔닝-전략)
4. [CQRS 패턴 최적화](#4-cqrs-패턴-최적화)
5. [Aurora MySQL 특화 최적화](#5-aurora-mysql-특화-최적화)

---

## 1. 테이블 설계 원칙

### 1.1 Primary Key 설계 (TSID 기반)

#### TSID를 Primary Key로 사용하는 이유

TSID(Time-Sorted Unique Identifier)는 시간 기반으로 정렬되는 고유 식별자로, 다음과 같은 장점이 있습니다:

1. **인덱스 효율성**: TSID는 시간 순서대로 생성되므로, InnoDB의 B-Tree 클러스터드 인덱스에서 페이지 분할을 최소화하여 인덱스 효율성을 높입니다.

2. **쓰기 성능 향상**: 순차적인 키 값은 인덱스의 균형을 유지하고 삽입 성능을 향상시킵니다. Aurora MySQL의 빠른 입력(Fast Insert) 기능과 함께 사용 시 더욱 효과적입니다.

3. **분산 시스템 호환성**: TSID는 분산 환경에서 고유성과 순서를 보장하므로, 샤딩 키로 활용하여 데이터 분산을 균등하게 할 수 있습니다.

4. **쿼리 최적화**: 시간 기반 정렬 특성으로 인해 시간 범위 쿼리에서 파티션 프루닝이 용이합니다.

#### TSID와 클러스터드 인덱스의 관계

InnoDB 스토리지 엔진은 Primary Key를 기반으로 클러스터드 인덱스를 생성합니다. TSID를 Primary Key로 사용하면:

- 데이터의 물리적 정렬이 시간 순서대로 유지됩니다.
- 클러스터드 인덱스의 리프 노드에 실제 데이터가 저장되므로, Primary Key로 조회 시 추가 인덱스 조회가 불필요합니다.
- 순차 INSERT 작업 시 페이지 분할이 최소화되어 성능이 향상됩니다.

#### TSID 생성 전략

**애플리케이션 레벨 생성 권장**:
- TSID는 애플리케이션 레벨에서 생성하는 것을 권장합니다.
- 데이터베이스 레벨에서 생성 시 트랜잭션 경계에서 지연이 발생할 수 있습니다.
- 애플리케이션 레벨 생성 시 분산 환경에서도 일관된 생성이 가능합니다.

**TSID 컬럼 타입**:
```sql
CREATE TABLE example_table (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    -- TSID는 64비트 숫자이므로 BIGINT UNSIGNED 사용
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- TSID의 시간 정보와 함께 실제 생성 시간도 저장
    ...
) ENGINE=InnoDB;
```

**분산 환경에서의 주의사항**:
- TSID 생성 시 노드 ID를 포함하여 고유성을 보장해야 합니다.
- 시계 동기화가 중요하므로 NTP(Network Time Protocol)를 사용하여 시간 동기화를 유지해야 합니다.

**출처**: 
- [Amazon Aurora MySQL 개요](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html) - Priority 1
- [TSID Creator 라이브러리](https://github.com/f4b6a3/tsid-creator) - Priority 1

### 1.2 테이블 정규화 전략

#### CQRS 패턴에서의 정규화 수준 결정

CQRS 패턴에서는 Command Side와 Query Side의 요구사항이 다르므로, 각각 다른 정규화 전략을 적용해야 합니다.

**Command Side 정규화 전략 (높은 정규화)**:
- Command Side는 쓰기 작업에 최적화되어야 하므로, 높은 정규화 수준을 유지합니다.
- 데이터 일관성을 보장하고 중복을 최소화합니다.
- 트랜잭션 경계를 최소화하여 쓰기 성능을 향상시킵니다.

**Query Side 비정규화 전략 (읽기 최적화)**:
- Query Side는 읽기 작업에 최적화되어야 하므로, 비정규화를 통해 조인을 최소화합니다.
- 자주 함께 조회되는 데이터를 하나의 테이블에 포함시킵니다.
- 읽기 전용 복제본을 활용하여 읽기 부하를 분산합니다.

**정규화 vs 비정규화 트레이드오프**:
- 정규화: 데이터 일관성 향상, 저장 공간 절약, 쓰기 성능 향상
- 비정규화: 읽기 성능 향상, 조인 최소화, 쿼리 복잡도 감소

**출처**:
- [Martin Fowler - CQRS](https://martinfowler.com/bliki/CQRS.html) - Priority 1
- [Microsoft - CQRS 패턴](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs) - Priority 1

### 1.3 컬럼 타입 선택

#### TSID 컬럼 타입

**BIGINT UNSIGNED 권장**:
- TSID는 64비트 숫자이므로 `BIGINT UNSIGNED`를 사용합니다.
- UNSIGNED를 사용하면 음수 범위를 제외하여 더 큰 양수 범위를 사용할 수 있습니다.
- 인덱스 크기를 최소화하고 성능을 향상시킵니다.

#### 문자열 vs 숫자형 선택 기준

**숫자형 사용 권장**:
- Primary Key는 가능한 한 숫자형을 사용하는 것이 성능상 유리합니다.
- 숫자형은 비교 연산이 빠르고 인덱스 효율이 높습니다.
- TSID는 숫자형이므로 이 요구사항을 만족합니다.

**문자열 사용 시 고려사항**:
- UUID와 같은 문자열을 사용할 경우, 인덱스 분할이 발생하여 성능이 저하될 수 있습니다.
- TSID는 숫자형이므로 이러한 문제를 피할 수 있습니다.

#### JSON 컬럼 사용 전략

**JSON 컬럼 활용**:
- MySQL 8.0은 JSON 타입을 지원하며, JSON 함수를 사용하여 효율적으로 조회할 수 있습니다.
- 유연한 스키마가 필요한 경우 JSON 컬럼을 활용할 수 있습니다.
- JSON 컬럼에도 인덱스를 생성할 수 있으므로, 자주 조회하는 필드에 대해 인덱스를 생성하는 것을 고려해야 합니다.

**JSON 인덱스 예제**:
```sql
CREATE TABLE example_table (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    metadata JSON,
    INDEX idx_metadata_status ((CAST(metadata->>'$.status' AS CHAR(20))))
) ENGINE=InnoDB;
```

#### ENUM vs VARCHAR 선택

**ENUM 사용 시기**:
- 값의 범위가 제한적이고 변경이 거의 없는 경우 ENUM을 사용할 수 있습니다.
- ENUM은 저장 공간을 절약하고 타입 안정성을 제공합니다.

**VARCHAR 사용 시기**:
- 값의 범위가 자주 변경되거나 확장 가능성이 있는 경우 VARCHAR를 사용합니다.
- CQRS 패턴에서는 Query Side에서 비정규화된 데이터를 저장할 때 VARCHAR를 사용하는 것이 유연합니다.

**출처**:
- [MySQL 8.0 데이터 타입](https://dev.mysql.com/doc/refman/8.0/en/data-types.html) - Priority 1

### 1.4 NULL 처리 전략

#### NULL 허용 여부 결정 기준

**NOT NULL 제약조건 활용**:
- 가능한 한 NOT NULL 제약조건을 사용하여 데이터 무결성을 보장합니다.
- NULL 값은 인덱스에서 별도로 처리되므로, NOT NULL을 사용하면 인덱스 효율이 향상됩니다.
- 애플리케이션 로직에서 NULL 체크가 불필요해져 코드가 단순해집니다.

**기본값 설정 전략**:
- NOT NULL 컬럼에는 적절한 기본값을 설정합니다.
- TIMESTAMP 컬럼의 경우 `DEFAULT CURRENT_TIMESTAMP(6)`를 사용하여 자동으로 현재 시간을 설정할 수 있습니다.

**출처**:
- [MySQL 8.0 데이터 타입 최적화](https://dev.mysql.com/doc/refman/8.0/en/optimization-overview.html) - Priority 1

---

## 2. 인덱스 설계 전략

### 2.1 Primary Key 인덱스 (TSID)

#### 클러스터드 인덱스 특성

InnoDB는 Primary Key를 기반으로 클러스터드 인덱스를 생성합니다. TSID를 Primary Key로 사용할 경우:

**특성**:
- 클러스터드 인덱스의 리프 노드에 실제 데이터가 저장됩니다.
- Primary Key로 조회 시 추가 인덱스 조회가 불필요하므로 매우 빠릅니다.
- 데이터는 Primary Key 순서대로 물리적으로 정렬됩니다.

**TSID의 시간 기반 정렬이 인덱스 성능에 미치는 영향**:
- TSID는 시간 순서대로 생성되므로, 순차 INSERT 작업 시 페이지 분할이 최소화됩니다.
- 인덱스의 균형이 유지되어 삽입 성능이 향상됩니다.
- 시간 범위 쿼리에서 파티션 프루닝이 용이합니다.

#### INSERT 성능 최적화

**Aurora MySQL의 빠른 입력(Fast Insert) 기능**:
- Aurora MySQL은 기본 키에 의해 정렬되는 병렬 입력을 빠르게 처리하는 빠른 입력 기능을 제공합니다.
- `LOAD DATA` 및 `INSERT INTO ... SELECT ...` 문 사용 시 인덱스 순회를 최적화하여 쓰기 성능을 향상시킵니다.
- TSID와 같은 순차적인 키를 사용할 경우 이 기능의 효과가 극대화됩니다.

**최적화 전략**:
```sql
-- 배치 INSERT 최적화
INSERT INTO example_table (id, ...) VALUES
    (TSID.next(), ...),
    (TSID.next(), ...),
    ...
-- TSID는 순차적으로 생성되므로 인덱스 분할이 최소화됨
```

**출처**:
- [Amazon Aurora MySQL 개요 - 빠른 입력 기능](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html) - Priority 1
- [MySQL 8.0 InnoDB 스토리지 엔진](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html) - Priority 1

### 2.2 Secondary Index 설계

#### CQRS 패턴에서의 인덱스 전략

**Command Side 인덱스 (쓰기 최적화)**:
- Command Side는 쓰기 작업에 최적화되어야 하므로, 인덱스를 최소화합니다.
- 필수적인 인덱스만 생성하여 INSERT, UPDATE, DELETE 성능을 향상시킵니다.
- 외래 키 인덱스는 데이터 무결성을 위해 필수입니다.

**Query Side 인덱스 (읽기 최적화)**:
- Query Side는 읽기 작업에 최적화되어야 하므로, 쿼리 패턴에 맞는 인덱스를 생성합니다.
- 읽기 전용 복제본을 활용하므로, 인덱스가 많아도 쓰기 성능에 영향을 주지 않습니다.
- 커버링 인덱스를 활용하여 테이블 조회를 최소화합니다.

#### 복합 인덱스 설계 원칙

**복합 인덱스 순서**:
- 복합 인덱스의 컬럼 순서는 쿼리 패턴에 따라 결정합니다.
- 가장 선택도가 높은 컬럼을 앞에 배치합니다.
- 등호 조건 컬럼을 범위 조건 컬럼보다 앞에 배치합니다.

**예제**:
```sql
-- 복합 인덱스 설계
CREATE INDEX idx_user_status_created ON example_table (
    user_id,           -- 등호 조건
    status,            -- 등호 조건
    created_at         -- 범위 조건
);

-- 이 인덱스는 다음 쿼리에 최적화됨
SELECT * FROM example_table 
WHERE user_id = ? AND status = ? AND created_at >= ?;
```

#### 커버링 인덱스 활용

**커버링 인덱스 전략**:
- 쿼리에 필요한 모든 컬럼이 인덱스에 포함되면, 테이블 조회 없이 인덱스만으로 결과를 반환할 수 있습니다.
- 이를 커버링 인덱스라고 하며, 읽기 성능을 크게 향상시킵니다.

**예제**:
```sql
-- 커버링 인덱스 생성
CREATE INDEX idx_user_status_covering ON example_table (
    user_id,
    status,
    name,              -- SELECT에 포함된 컬럼
    email              -- SELECT에 포함된 컬럼
);

-- 이 쿼리는 테이블 조회 없이 인덱스만으로 처리됨
SELECT name, email FROM example_table 
WHERE user_id = ? AND status = ?;
```

**출처**:
- [MySQL 8.0 인덱스 최적화](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html) - Priority 1

### 2.3 인덱스 최적화

#### 인덱스 선택도 (Selectivity) 고려

**선택도 계산**:
- 선택도 = (고유 값 수) / (전체 행 수)
- 선택도가 높을수록 인덱스 효율이 높습니다.
- 선택도가 낮은 인덱스는 제거를 고려해야 합니다.

**예제**:
```sql
-- 선택도가 낮은 인덱스 (예: 성별 컬럼)
-- 남성/여성 두 값만 있으면 선택도가 매우 낮음
-- 이런 인덱스는 제거 고려

-- 선택도가 높은 인덱스 (예: 사용자 ID)
-- 각 사용자가 고유한 ID를 가지면 선택도가 매우 높음
```

#### 인덱스 크기 최적화

**인덱스 크기 최소화**:
- 인덱스 크기가 작을수록 메모리 사용량이 줄고 성능이 향상됩니다.
- 불필요한 컬럼을 인덱스에서 제외합니다.
- 접두사 인덱스를 활용하여 인덱스 크기를 줄일 수 있습니다.

**접두사 인덱스 예제**:
```sql
-- 긴 문자열 컬럼의 경우 접두사 인덱스 사용
CREATE INDEX idx_email_prefix ON example_table (email(20));
-- email 컬럼의 처음 20자만 인덱싱
```

#### 인덱스 모니터링 및 유지보수

**인덱스 사용률 모니터링**:
- 사용되지 않는 인덱스는 제거하여 쓰기 성능을 향상시킵니다.
- MySQL의 `sys` 스키마를 활용하여 인덱스 사용률을 모니터링할 수 있습니다.

**모니터링 쿼리**:
```sql
-- 사용되지 않는 인덱스 확인
SELECT * FROM sys.schema_unused_indexes;
```

**출처**:
- [MySQL 8.0 Performance Schema](https://dev.mysql.com/doc/refman/8.0/en/performance-schema.html) - Priority 1

---

## 3. 파티셔닝 전략

### 3.1 파티셔닝 타입 선택

#### Range 파티셔닝 (TSID 기반)

**TSID 기반 Range 파티셔닝**:
- TSID는 시간 기반으로 정렬되므로, Range 파티셔닝에 적합합니다.
- 시간 범위별로 파티션을 나누면 파티션 프루닝이 용이합니다.

**예제**:
```sql
CREATE TABLE example_table (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    ...
) ENGINE=InnoDB
PARTITION BY RANGE (id) (
    PARTITION p2024_q1 VALUES LESS THAN (1000000000000000000),
    PARTITION p2024_q2 VALUES LESS THAN (2000000000000000000),
    PARTITION p2024_q3 VALUES LESS THAN (3000000000000000000),
    PARTITION p2024_q4 VALUES LESS THAN (4000000000000000000),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

#### Hash 파티셔닝

**Hash 파티셔닝 전략**:
- Hash 파티셔닝은 데이터를 균등하게 분산시킵니다.
- TSID를 Hash 함수에 적용하여 파티션을 결정할 수 있습니다.

**예제**:
```sql
CREATE TABLE example_table (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    ...
) ENGINE=InnoDB
PARTITION BY HASH (id)
PARTITIONS 8;
```

#### CQRS 패턴에서의 파티셔닝 활용

**Command Side 파티셔닝**:
- Command Side는 쓰기 작업에 최적화되어야 하므로, 파티셔닝을 통해 쓰기 부하를 분산시킵니다.
- TSID 기반 파티셔닝을 사용하면 순차 INSERT 작업이 여러 파티션에 분산되어 성능이 향상됩니다.

**Query Side 파티셔닝**:
- Query Side는 읽기 작업에 최적화되어야 하므로, 쿼리 패턴에 맞는 파티셔닝을 적용합니다.
- 시간 범위 쿼리가 많은 경우 Range 파티셔닝이 효과적입니다.

**출처**:
- [MySQL 8.0 파티셔닝](https://dev.mysql.com/doc/refman/8.0/en/partitioning.html) - Priority 1

### 3.2 파티셔닝 키 선택

#### TSID를 파티셔닝 키로 사용하는 전략

**TSID 파티셔닝 키 장점**:
- TSID는 시간 기반으로 정렬되므로, 시간 범위 쿼리에서 파티션 프루닝이 용이합니다.
- 순차 INSERT 작업이 여러 파티션에 분산되어 성능이 향상됩니다.
- 오래된 데이터를 쉽게 아카이빙할 수 있습니다.

**시간 기반 파티셔닝**:
- TSID의 시간 정보를 활용하여 월별 또는 분기별로 파티션을 나눌 수 있습니다.
- 오래된 파티션은 아카이빙하거나 삭제하여 테이블 크기를 관리할 수 있습니다.

**출처**:
- [MySQL 8.0 파티셔닝 최적화](https://dev.mysql.com/doc/refman/8.0/en/partitioning-optimization.html) - Priority 1

---

## 4. CQRS 패턴 최적화

### 4.1 Command Side 최적화

#### 쓰기 작업 최적화 전략

**트랜잭션 경계 최소화**:
- 트랜잭션 범위를 최소화하여 락 경합을 줄입니다.
- 단일 작업은 가능한 한 단일 트랜잭션으로 처리합니다.

**배치 INSERT 최적화**:
- 여러 행을 한 번에 INSERT하여 네트워크 왕복을 최소화합니다.
- Aurora MySQL의 빠른 입력 기능을 활용합니다.

**예제**:
```sql
-- 배치 INSERT 최적화
INSERT INTO example_table (id, name, email) VALUES
    (TSID.next(), 'name1', 'email1'),
    (TSID.next(), 'name2', 'email2'),
    ...
-- 여러 행을 한 번에 INSERT
```

#### 이벤트 발행 최적화

**이벤트 발행 전략**:
- Command Side에서 데이터 변경 후 이벤트를 발행합니다.
- 트랜잭션 커밋 후 이벤트를 발행하여 일관성을 보장합니다.
- 배치 이벤트 발행을 통해 성능을 향상시킵니다.

**출처**:
- [Amazon Aurora MySQL 개요 - 빠른 입력 기능](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html) - Priority 1

### 4.2 Query Side 최적화

#### 읽기 작업 최적화 전략

**읽기 전용 복제본 활용**:
- Aurora MySQL의 읽기 전용 복제본을 활용하여 읽기 부하를 분산시킵니다.
- 복제 지연을 고려하여 적절한 복제본을 선택합니다.

**쿼리 패턴 분석 및 최적화**:
- 자주 실행되는 쿼리를 분석하여 인덱스를 최적화합니다.
- 커버링 인덱스를 활용하여 테이블 조회를 최소화합니다.

**Aurora MySQL 병렬 쿼리 활용**:
- Aurora MySQL의 병렬 쿼리 기능을 활용하여 대규모 데이터 세트에 대한 분석 쿼리 성능을 향상시킵니다.
- 병렬 쿼리는 스토리지 계층에서 병렬로 처리하여 쿼리 속도를 최대 100배까지 향상시킬 수 있습니다.

**출처**:
- [Amazon Aurora MySQL 병렬 쿼리](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/aurora-mysql-parallel-query.html) - Priority 1
- [Amazon Aurora MySQL 읽기 전용 복제본](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.Replication.ReadReplicas.html) - Priority 1

#### 캐싱 전략

**캐싱 전략**:
- 자주 조회되는 데이터는 Redis와 같은 캐시에 저장하여 데이터베이스 부하를 줄입니다.
- 캐시 무효화 전략을 수립하여 데이터 일관성을 보장합니다.

### 4.3 데이터 일관성

#### 최종 일관성 (Eventual Consistency) 전략

**최종 일관성 보장**:
- CQRS 패턴에서는 Command Side와 Query Side가 분리되어 있으므로, 최종 일관성을 보장해야 합니다.
- 이벤트 기반 아키텍처를 통해 데이터 동기화를 수행합니다.

**읽기 일관성 보장 방법**:
- 읽기 전용 복제본의 복제 지연을 고려하여 적절한 복제본을 선택합니다.
- 강한 일관성이 필요한 경우 Primary 인스턴스를 사용합니다.

**이벤트 순서 보장**:
- 이벤트 순서를 보장하기 위해 TSID의 시간 기반 정렬 특성을 활용합니다.
- 이벤트 스토어에 TSID를 사용하여 이벤트 순서를 보장합니다.

**출처**:
- [Martin Fowler - CQRS](https://martinfowler.com/bliki/CQRS.html) - Priority 1
- [Microsoft - CQRS 패턴](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs) - Priority 1

---

## 5. Aurora MySQL 특화 최적화

### 5.1 스토리지 아키텍처 활용

#### Aurora MySQL의 스토리지 계층 활용

**로그 구조화 스토리지 최적화**:
- Aurora MySQL은 로그 구조화 스토리지를 사용하여 쓰기 성능을 향상시킵니다.
- 순차 쓰기 작업이 많을수록 성능이 향상되므로, TSID와 같은 순차적인 키를 사용하는 것이 유리합니다.

**백업 및 복구 전략**:
- Aurora MySQL은 연속 백업을 제공하므로, Point-in-Time Recovery가 가능합니다.
- 자동 백업을 활성화하여 데이터 손실을 방지합니다.

**출처**:
- [Amazon Aurora MySQL 백업 및 복구](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.Managing.Backups.html) - Priority 1

### 5.2 읽기 전용 복제본

#### 읽기 전용 복제본 활용 전략

**읽기 부하 분산**:
- 읽기 전용 복제본을 생성하여 읽기 부하를 분산시킵니다.
- 애플리케이션에서 읽기 작업을 복제본으로 라우팅합니다.

**복제 지연 (Replication Lag) 관리**:
- 복제 지연을 모니터링하여 적절한 복제본을 선택합니다.
- 강한 일관성이 필요한 경우 Primary 인스턴스를 사용합니다.

**출처**:
- [Amazon Aurora MySQL 읽기 전용 복제본](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.Replication.ReadReplicas.html) - Priority 1

### 5.3 성능 모니터링

#### Performance Insights 활용

**Performance Insights**:
- Aurora MySQL의 Performance Insights를 활용하여 쿼리 성능을 분석합니다.
- 느린 쿼리를 식별하여 최적화합니다.

**쿼리 성능 분석**:
- 쿼리 실행 계획을 분석하여 인덱스 사용 여부를 확인합니다.
- 인덱스 사용률을 모니터링하여 불필요한 인덱스를 제거합니다.

**인덱스 모니터링**:
- MySQL의 `sys` 스키마를 활용하여 인덱스 사용률을 모니터링합니다.
- 사용되지 않는 인덱스를 제거하여 쓰기 성능을 향상시킵니다.

**출처**:
- [Amazon Aurora MySQL Performance Insights](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/USER_PerfInsights.html) - Priority 1

---

## 베스트 프랙티스 요약

### 테이블 설계
1. **TSID를 Primary Key로 사용**: 시간 기반 정렬 특성을 활용하여 인덱스 효율성과 쓰기 성능을 향상시킵니다.
2. **BIGINT UNSIGNED 사용**: TSID 컬럼은 `BIGINT UNSIGNED` 타입을 사용합니다.
3. **NOT NULL 제약조건 활용**: 가능한 한 NOT NULL 제약조건을 사용하여 데이터 무결성을 보장합니다.
4. **CQRS 패턴에 맞는 정규화**: Command Side는 높은 정규화, Query Side는 비정규화를 적용합니다.

### 인덱스 설계
1. **Command Side 인덱스 최소화**: 쓰기 성능을 위해 필수적인 인덱스만 생성합니다.
2. **Query Side 인덱스 최적화**: 읽기 성능을 위해 쿼리 패턴에 맞는 인덱스를 생성합니다.
3. **커버링 인덱스 활용**: 자주 조회되는 컬럼을 인덱스에 포함하여 테이블 조회를 최소화합니다.
4. **인덱스 모니터링**: 사용되지 않는 인덱스를 제거하여 쓰기 성능을 향상시킵니다.

### CQRS 최적화
1. **Command Side 최적화**: 트랜잭션 경계 최소화, 배치 INSERT 최적화, 빠른 입력 기능 활용
2. **Query Side 최적화**: 읽기 전용 복제본 활용, 병렬 쿼리 활용, 캐싱 전략 수립
3. **데이터 일관성**: 최종 일관성 전략 수립, 이벤트 순서 보장

### Aurora MySQL 최적화
1. **스토리지 아키텍처 활용**: 로그 구조화 스토리지 최적화, 연속 백업 활용
2. **읽기 전용 복제본**: 읽기 부하 분산, 복제 지연 관리
3. **성능 모니터링**: Performance Insights 활용, 쿼리 성능 분석, 인덱스 모니터링

---

## 참고 자료

1. [Amazon Aurora MySQL 개요](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html) - Priority 1, 총점: 38/40
2. [Amazon Aurora MySQL 병렬 쿼리](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/aurora-mysql-parallel-query.html) - Priority 1, 총점: 36/40
3. [Amazon Aurora MySQL 읽기 전용 복제본](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.Replication.ReadReplicas.html) - Priority 1, 총점: 35/40
4. [MySQL 8.0 InnoDB 스토리지 엔진](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html) - Priority 1, 총점: 37/40
5. [MySQL 8.0 인덱스 최적화](https://dev.mysql.com/doc/refman/8.0/en/optimization-indexes.html) - Priority 1, 총점: 36/40
6. [MySQL 8.0 파티셔닝](https://dev.mysql.com/doc/refman/8.0/en/partitioning.html) - Priority 1, 총점: 34/40
7. [Martin Fowler - CQRS](https://martinfowler.com/bliki/CQRS.html) - Priority 1, 총점: 32/40
8. [Microsoft - CQRS 패턴](https://learn.microsoft.com/en-us/azure/architecture/patterns/cqrs) - Priority 1, 총점: 33/40
9. [TSID Creator 라이브러리](https://github.com/f4b6a3/tsid-creator) - Priority 1, 총점: 30/40

---

## 코드 예제

### TSID Primary Key DDL 예제

```sql
CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 테이블 - TSID Primary Key 사용';
```

### 애플리케이션 코드 예제 (Java)

```java
import io.github.f4b6a3.tsid.TsidCreator;

public class UserService {
    
    public User createUser(String username, String email) {
        // TSID 생성 (애플리케이션 레벨)
        long tsid = TsidCreator.getTsid256().toLong();
        
        User user = new User();
        user.setId(tsid);
        user.setUsername(username);
        user.setEmail(email);
        
        // 데이터베이스에 저장
        userRepository.save(user);
        
        return user;
    }
}
```

### 인덱스 생성 예제

```sql
-- 복합 인덱스 생성
CREATE INDEX idx_user_status_created ON users (
    user_id,
    status,
    created_at
);

-- 커버링 인덱스 생성
CREATE INDEX idx_user_status_covering ON users (
    user_id,
    status,
    username,
    email
);
```

### 파티셔닝 예제

```sql
CREATE TABLE events (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    event_type VARCHAR(50) NOT NULL,
    event_data JSON,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_event_type_created (event_type, created_at)
) ENGINE=InnoDB
PARTITION BY RANGE (id) (
    PARTITION p2024_q1 VALUES LESS THAN (1000000000000000000),
    PARTITION p2024_q2 VALUES LESS THAN (2000000000000000000),
    PARTITION p2024_q3 VALUES LESS THAN (3000000000000000000),
    PARTITION p2024_q4 VALUES LESS THAN (4000000000000000000),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

*이 문서는 Amazon Aurora MySQL 공식 문서, MySQL 8.0 공식 문서, CQRS 패턴 공식 문서를 기반으로 작성되었습니다.*

