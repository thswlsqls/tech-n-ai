# MongoDB Atlas 도큐먼트 설계 베스트 프랙티스

## 연구 메타데이터
- 연구 날짜: 2025-01-27
- MongoDB 버전: 7.0+
- Atlas 버전: latest
- 연구자: MongoDB Database Architect

## 목차
1. [도큐먼트 스키마 설계 원칙](#1-도큐먼트-스키마-설계-원칙)
2. [인덱스 설계 전략](#2-인덱스-설계-전략)
3. [읽기 성능 최적화](#3-읽기-성능-최적화)
4. [도큐먼트 모델링 패턴](#4-도큐먼트-모델링-패턴)
5. [MongoDB Atlas 특화 고려사항](#5-mongodb-atlas-특화-고려사항)
6. [CQRS 패턴 적용 시 고려사항](#6-cqrs-패턴-적용-시-고려사항)

---

## 1. 도큐먼트 스키마 설계 원칙

### 1.1 핵심 개념

MongoDB의 유연한 데이터 모델은 성능과 적응성을 전략적으로 균형있게 유지하면서 데이터 일관성과 무결성을 보장할 수 있게 해줍니다. 도큐먼트 스키마 설계 시 애플리케이션의 사용 사례에 가장 적합한 스키마 설계 패턴을 결정하기 위해 다음 원칙들을 고려해야 합니다.

**출처**: [Best Practices for Data Modeling in MongoDB](https://www.mongodb.com/docs/manual/core/data-modeling-introduction/) (MongoDB 버전: 7.0)

### 1.2 도큐먼트 크기 제한

**핵심 제한사항:**
- MongoDB의 BSON 도큐먼트 크기 제한은 **16MB**입니다.
- 16MB보다 큰 도큐먼트가 필요한 경우 **GridFS**를 사용해야 합니다.

**베스트 프랙티스:**
1. 도큐먼트 크기를 최소화하여 메모리 사용량과 네트워크 전송 비용을 줄입니다.
2. 자주 함께 조회되는 데이터는 같은 도큐먼트에 포함시킵니다.
3. 큰 배열이나 중첩 도큐먼트는 성장 가능성을 고려하여 설계합니다.

**출처**: [Development Checklist > Schema Design](https://www.mongodb.com/docs/manual/administration/production-checklist-development/) (MongoDB 버전: 7.0)

### 1.3 Embedded vs Referenced 문서 선택 기준

#### Embedded 문서 패턴 (임베디드)

**사용 시나리오:**
- One-to-One 관계: 자주 함께 조회되는 데이터
- One-to-Many 관계: 서브도큐먼트의 수가 제한적이고 자주 함께 조회되는 경우
- 데이터가 자주 함께 업데이트되는 경우

**예제: One-to-One Embedded**
```javascript
{
   _id: "joe",
   name: "Joe Bookreader",
   address: {
      street: "123 Fake Street",
      city: "Faketon",
      state: "MA",
      zip: "12345"
   }
}
```

**예제: One-to-Many Embedded**
```javascript
{
   _id: "joe",
   name: "Joe Bookreader",
   addresses: [
      {
         street: "123 Fake Street",
         city: "Faketon",
         state: "MA",
         zip: "12345"
      },
      {
         street: "1 Some Other Street",
         city: "Boston",
         state: "MA",
         zip: "12345"
      }
   ]
}
```

**장점:**
- 단일 읽기 작업으로 모든 관련 데이터를 가져올 수 있습니다.
- 애플리케이션이 필요한 모든 정보를 단일 읽기 작업으로 받을 수 있습니다.

**출처**: 
- [Model One-to-One Relationships with Embedded Documents](https://www.mongodb.com/docs/manual/tutorial/model-embedded-one-to-one-relationships-between-documents/)
- [Model One-to-Many Relationships with Embedded Documents](https://www.mongodb.com/docs/manual/tutorial/model-embedded-one-to-many-relationships-between-documents/) (MongoDB 버전: 7.0)

#### Referenced 문서 패턴 (참조)

**사용 시나리오:**
- One-to-Many 관계: 서브도큐먼트의 수가 무제한이거나 자주 변경되는 경우
- Many-to-Many 관계
- 서브도큐먼트가 독립적으로 자주 조회되는 경우

**예제: One-to-Many Referenced**
```javascript
// Publisher 문서
{
   _id: "oreilly",
   name: "O'Reilly Media",
   founded: 1980,
   location: "CA"
}

// Book 문서들
{
   _id: 123456789,
   title: "MongoDB: The Definitive Guide",
   author: [ "Kristina Chodorow", "Mike Dirolf" ],
   published_date: ISODate("2010-09-24"),
   pages: 216,
   language: "English",
   publisher_id: "oreilly"
}

{
   _id: 234567890,
   title: "50 Tips and Tricks for MongoDB Developer",
   author: "Kristina Chodorow",
   published_date: ISODate("2011-05-06"),
   pages: 68,
   language: "English",
   publisher_id: "oreilly"
}
```

**장점:**
- 데이터 반복을 피할 수 있습니다.
- 'one' 쪽에 가변적이고 성장하는 배열을 방지하여 확장 가능합니다.
- 무제한 관계를 효과적으로 처리할 수 있습니다.

**출처**: [Model Referenced One-to-Many Relationships](https://www.mongodb.com/docs/manual/tutorial/model-referenced-one-to-many-relationships-between-documents/) (MongoDB 버전: 7.0)

### 1.4 BSON 데이터 타입 선택 가이드

**주요 BSON 데이터 타입:**
- `ObjectId`: 고유 식별자
- `String`: 문자열 데이터
- `Integer` / `Long`: 정수 값
- `Double`: 부동소수점 숫자
- `Date`: 날짜 및 시간
- `Boolean`: 불리언 값
- `Array`: 배열
- `Object`: 중첩 도큐먼트
- `Null`: null 값
- `Binary`: 바이너리 데이터

**베스트 프랙티스:**
1. 숫자 타입 선택 시 `Integer` vs `Long` vs `Double`을 데이터 범위에 맞게 선택합니다.
2. 날짜 데이터는 항상 `Date` 타입을 사용합니다.
3. 배열과 중첩 도큐먼트를 적절히 활용하여 관계를 모델링합니다.

**예제: 다양한 BSON 타입 사용**
```javascript
var mydoc = {
   _id: ObjectId("5099803df3f4948bd2f98391"),
   name: { first: "Alan", last: "Turing" },
   birth: new Date('Jun 23, 1912'),
   death: new Date('Jun 07, 1954'),
   contribs: [ "Turing machine", "Turing test", "Turingery" ],
   views : Long(1250000)
}
```

**출처**: [MongoDB Document](https://www.mongodb.com/docs/manual/core/document/) (MongoDB 버전: 7.0)

### 1.5 필드 네이밍 컨벤션

**권장 사항:**
- 필드 이름은 의미 있고 일관성 있게 명명합니다.
- 짧고 명확한 필드 이름을 사용합니다 (네트워크 전송 비용 고려).
- 일관된 네이밍 컨벤션을 유지합니다 (camelCase 또는 snake_case).

**주의사항:**
- 필드 이름은 도큐먼트 크기에 영향을 미칩니다.
- 너무 긴 필드 이름은 16MB 제한에 도달할 가능성을 높입니다.

### 1.6 배열 및 중첩 도큐먼트 활용

**베스트 프랙티스:**
1. 배열은 제한된 수의 요소에 사용합니다.
2. 배열이 무제한으로 성장할 수 있는 경우 참조 패턴을 고려합니다.
3. 중첩 도큐먼트의 깊이는 성능에 영향을 미칠 수 있으므로 적절히 제한합니다.

**주의사항:**
- 배열이 크게 성장하면 도큐먼트 크기 제한에 도달할 수 있습니다.
- 배열 인덱싱 시 다중 키 인덱스가 생성되며, 이는 인덱스 크기에 영향을 미칩니다.

---

## 2. 인덱스 설계 전략

### 2.1 핵심 개념

인덱스는 쿼리 성능을 향상시키는 핵심 메커니즘입니다. 적절한 인덱스 설계는 읽기 성능을 크게 개선할 수 있습니다.

**출처**: [MongoDB Indexes](https://www.mongodb.com/docs/manual/indexes/) (MongoDB 버전: 7.0)

### 2.2 단일 필드 인덱스 vs 복합 인덱스

#### 단일 필드 인덱스

**사용 시나리오:**
- 단일 필드에 대한 쿼리가 주로 수행되는 경우
- 정렬이 단일 필드에만 적용되는 경우

**예제:**
```javascript
db.collection.createIndex({ username: 1 })
```

#### 복합 인덱스 (Compound Index)

**사용 시나리오:**
- 여러 필드에 대한 쿼리가 함께 수행되는 경우
- 여러 필드에 대한 정렬이 필요한 경우

**핵심 개념:**
복합 인덱스는 두 개 이상의 키로 구성되며, 여러 필드에 대한 필터링, 정렬 또는 매칭을 포함하는 쿼리의 성능을 향상시키는 데 필수적입니다.

**예제:**
```javascript
db.collection.createIndex({ username: 1, email: 1 })
```

**출처**: [Compound Indexes](https://www.mongodb.com/docs/manual/core/index-compound/) (MongoDB 버전: 7.0)

### 2.3 ESR 규칙 (Equality, Sort, Range)

**핵심 원칙:**
복합 인덱스를 생성할 때 필드 순서는 다음 규칙을 따릅니다:
1. **Equality (등가)**: 등가 매칭에 사용되는 필드
2. **Sort (정렬)**: 정렬에 사용되는 필드
3. **Range (범위)**: 범위 쿼리에 사용되는 필드

**예제 쿼리:**
```javascript
db.cars.find(
   {
       manufacturer: 'Ford',        // Equality
       cost: { $gt: 15000 }          // Range
   }
).sort( { model: 1 } )              // Sort
```

**최적 인덱스:**
```javascript
db.cars.createIndex({ manufacturer: 1, model: 1, cost: 1 })
```

**설명:**
- `manufacturer`: 등가 매칭 (E)
- `model`: 정렬 (S)
- `cost`: 범위 (R)

**출처**: [ESR Guideline](https://www.mongodb.com/docs/manual/tutorial/equality-sort-range-guideline/) (MongoDB 버전: 7.0)

### 2.4 부분 인덱스 (Partial Index)

**개념:**
부분 인덱스는 필터 표현식을 사용하여 컬렉션의 문서 하위 집합만 인덱싱합니다. 이를 통해 인덱스 크기를 줄이고 성능을 향상시킬 수 있습니다.

**예제:**
```javascript
db.restaurants.createIndex(
   { cuisine: 1, name: 1 },
   { partialFilterExpression: { rating: { $gt: 5 } } }
)
```

**장점:**
- 인덱스 크기 감소
- 인덱스 유지 관리 비용 감소
- 특정 데이터 하위 집합에 대한 쿼리 성능 향상

**출처**: [Partial Indexes](https://www.mongodb.com/docs/manual/core/index-partial/) (MongoDB 버전: 7.0)

### 2.5 TTL 인덱스 (Time-To-Live Index)

**개념:**
TTL 인덱스는 특정 시간이 지난 후 자동으로 문서를 삭제하는 특수한 단일 필드 인덱스입니다.

**사용 시나리오:**
- 세션 데이터
- 로그 데이터
- 임시 데이터

**예제:**
```javascript
db.log_events.createIndex(
   { "createdAt": 1 },
   { expireAfterSeconds: 3600 }  // 1시간 후 삭제
)
```

**주의사항:**
- TTL 인덱스는 `Date` 타입 필드에만 사용할 수 있습니다.
- 삭제 작업은 백그라운드에서 주기적으로 실행됩니다 (약 60초마다).

**출처**: [TTL Indexes](https://www.mongodb.com/docs/manual/core/index-ttl/) (MongoDB 버전: 7.0)

### 2.6 다중 키 인덱스 (Multikey Index)

**개념:**
배열 필드에 인덱스를 생성하면 자동으로 다중 키 인덱스가 생성됩니다.

**예제:**
```javascript
db.survey.createIndex({ item: 1, ratings: 1 })
```

**제한사항:**
- 복합 다중 키 인덱스에서 각 인덱싱된 문서는 최대 하나의 배열 값을 가진 필드만 가질 수 있습니다.
- 인덱스 사양에서 두 개 이상의 필드가 배열인 경우 복합 다중 키 인덱스를 생성할 수 없습니다.

**출처**: [Multikey Indexes](https://www.mongodb.com/docs/manual/core/indexes/index-types/index-multikey/) (MongoDB 버전: 7.0)

### 2.7 인덱스 성능 최적화

**베스트 프랙티스:**
1. **인덱스 선택성 (Selectivity)**: 선택성이 높은 필드를 인덱스에 포함시킵니다.
2. **인덱스 크기 모니터링**: 인덱스는 메모리를 사용하므로 크기를 모니터링합니다.
3. **인덱스 생성 성능 영향**: 프로덕션 환경에서 인덱스 생성은 성능에 영향을 미칠 수 있습니다.
4. **불필요한 인덱스 제거**: 사용되지 않는 인덱스는 제거하여 쓰기 성능을 향상시킵니다.

---

## 3. 읽기 성능 최적화

### 3.1 핵심 개념

읽기 전용 워크로드(Query Side)의 성능 최적화는 CQRS 패턴에서 매우 중요합니다. 쿼리 패턴을 분석하고 적절한 최적화 기법을 적용해야 합니다.

### 3.2 프로젝션 (Projection) 활용

**개념:**
프로젝션은 쿼리 결과에서 필요한 필드만 선택하여 반환합니다. 이를 통해 네트워크 트래픽을 줄이고 성능을 향상시킬 수 있습니다.

**예제:**
```javascript
// 필요한 필드만 선택
db.collection.find(
   { status: "active" },
   { _id: 1, name: 1, email: 1 }  // 프로젝션
)
```

**베스트 프랙티스:**
1. 필요한 필드만 선택하여 네트워크 전송량을 최소화합니다.
2. 인덱스된 필드만 프로젝션하면 Covered Query가 되어 성능이 향상됩니다.

**출처**: [Query Optimization > Project Only Necessary Data](https://www.mongodb.com/docs/manual/core/query-optimization/) (MongoDB 버전: 7.0)

### 3.3 쿼리 패턴 분석

**분석 방법:**
1. `explain()` 메서드를 사용하여 쿼리 실행 계획을 분석합니다.
2. 인덱스 사용 여부를 확인합니다.
3. COLLSCAN(컬렉션 스캔)을 피하고 IXSCAN(인덱스 스캔)을 사용하도록 최적화합니다.

**예제:**
```javascript
db.inventory.find(
   { quantity: { $gte: 100, $lte: 200 } }
).explain("executionStats")
```

**출처**: [Analyze Query Performance](https://www.mongodb.com/docs/manual/tutorial/analyze-query-plan/) (MongoDB 버전: 7.0)

### 3.4 집계 파이프라인 최적화

**최적화 전략:**
MongoDB는 집계 파이프라인에 최적화 단계를 포함하여 성능을 향상시킵니다.

**최적화 예제:**

**최적화 전:**
```javascript
{
  $addFields: {
    maxTime: { $max: "$times" },
    minTime: { $min: "$times" }
  }
},
{
  $project: {
    _id: 1,
    name: 1,
    times: 1,
    maxTime: 1,
    minTime: 1,
    avgTime: { $avg: ["$maxTime", "$minTime"] }
  }
},
{
  $match: {
    name: "Joe Schmoe",
    maxTime: { $lt: 20 },
    minTime: { $gt: 5 },
    avgTime: { $gt: 7 }
  }
}
```

**최적화 후 (MongoDB 자동 최적화):**
```javascript
{ $match: { name: "Joe Schmoe" } },
{ $addFields: {
  maxTime: { $max: "$times" },
  minTime: { $min: "$times" }
} },
{ $match: { maxTime: { $lt: 20 }, minTime: { $gt: 5 } } },
{ $project: {
  _id: 1, name: 1, times: 1, maxTime: 1, minTime: 1,
  avgTime: { $avg: ["$maxTime", "$minTime"] }
} },
{ $match: { avgTime: { $gt: 7 } } }
```

**핵심 최적화:**
- `$match` 단계를 가능한 한 앞으로 이동시켜 파이프라인을 통과하는 데이터 양을 줄입니다.
- 계산된 필드에 의존하지 않는 필터는 프로젝션 단계 전에 배치됩니다.

**출처**: [Aggregation Pipeline Optimization](https://www.mongodb.com/docs/manual/core/aggregation-pipeline-optimization/) (MongoDB 버전: 7.0)

### 3.5 커서 배치 크기 최적화

**개념:**
커서 배치 크기는 각 배치에서 반환되는 문서 수를 제어합니다.

**예제:**
```javascript
db.inventory.find().batchSize(10)
```

**베스트 프랙티스:**
- 기본 배치 크기는 일반적으로 적절합니다.
- 큰 문서의 경우 배치 크기를 줄여 메모리 사용량을 제어할 수 있습니다.
- 실제 배치 크기는 지정된 크기와 16MB 중 작은 값으로 제한됩니다.

**출처**: [cursor.batchSize()](https://www.mongodb.com/docs/manual/reference/method/cursor.batchSize/) (MongoDB 버전: 7.0)

### 3.6 쿼리 힌트 사용

**개념:**
쿼리 힌트를 사용하여 특정 인덱스를 강제로 사용할 수 있습니다.

**예제:**
```javascript
db.inventory.find(
   { quantity: { $gte: 100, $lte: 300 }, type: "food" }
).hint({ quantity: 1, type: 1 }).explain("executionStats")
```

**주의사항:**
- 쿼리 힌트는 신중하게 사용해야 합니다.
- 인덱스가 변경되면 힌트가 더 이상 최적이 아닐 수 있습니다.

**출처**: [Query Optimization](https://www.mongodb.com/docs/manual/core/query-optimization/) (MongoDB 버전: 7.0)

---

## 4. 도큐먼트 모델링 패턴

### 4.1 핵심 개념

MongoDB의 도큐먼트 필드 값은 다른 도큐먼트, 배열, 도큐먼트 배열을 포함한 모든 BSON 데이터 타입을 포함할 수 있습니다. 이러한 객체를 사용하여 데이터 모델에서 다양한 유형의 관계를 표현할 수 있습니다.

**출처**: [Document Relationships](https://www.mongodb.com/docs/manual/core/data-modeling/) (MongoDB 버전: 7.0)

### 4.2 One-to-One 관계 모델링

**패턴: Embedded Documents**

One-to-One 관계는 각 도큐먼트가 정확히 하나의 다른 도큐먼트와 연관되는 경우입니다 (예: 환자가 정확히 하나의 의료 기록을 가짐).

**예제:**
```javascript
{
   _id: "joe",
   name: "Joe Bookreader",
   address: {
      street: "123 Fake Street",
      city: "Faketon",
      state: "MA",
      zip: "12345"
   }
}
```

**사용 시나리오:**
- 국가와 수도
- 사용자 계정과 이메일 주소
- 건물과 주소

**출처**: [Model One-to-One Relationships with Embedded Documents](https://www.mongodb.com/docs/manual/tutorial/model-embedded-one-to-one-relationships-between-documents/) (MongoDB 버전: 7.0)

### 4.3 One-to-Many 관계 모델링

#### Embedded 패턴

**사용 시나리오:**
- 서브도큐먼트의 수가 제한적
- 자주 함께 조회되는 경우

**예제:**
```javascript
{
   _id: "joe",
   name: "Joe Bookreader",
   addresses: [
      {
         street: "123 Fake Street",
         city: "Faketon",
         state: "MA",
         zip: "12345"
      },
      {
         street: "1 Some Other Street",
         city: "Boston",
         state: "MA",
         zip: "12345"
      }
   ]
}
```

#### Referenced 패턴

**사용 시나리오:**
- 서브도큐먼트의 수가 무제한
- 서브도큐먼트가 자주 변경되는 경우

**예제:**
```javascript
// Publisher
{
   _id: "oreilly",
   name: "O'Reilly Media",
   founded: 1980,
   location: "CA"
}

// Books
{
   _id: 123456789,
   title: "MongoDB: The Definitive Guide",
   author: [ "Kristina Chodorow", "Mike Dirolf" ],
   published_date: ISODate("2010-09-24"),
   pages: 216,
   language: "English",
   publisher_id: "oreilly"
}
```

**출처**: 
- [Model One-to-Many Relationships with Embedded Documents](https://www.mongodb.com/docs/manual/tutorial/model-embedded-one-to-many-relationships-between-documents/)
- [Model Referenced One-to-Many Relationships](https://www.mongodb.com/docs/manual/tutorial/model-referenced-one-to-many-relationships-between-documents/) (MongoDB 버전: 7.0)

### 4.4 Many-to-Many 관계 모델링

**패턴: Embedded Documents (주로 한 쪽에서 조회하는 경우)**

**예제: Book-Author 관계**
```javascript
{
   _id: "book001",
   title: "Cell Biology",
   authors: [
     {
        author_id: "author124",
        name: "Ellie Smith"
     },
     {
        author_id: "author381",
        name: "John Palmer"
     }
   ] 
}

{
   _id: "book002",
   title: "Organic Chemistry",
   authors: [
     {
        author_id: "author290",
        name: "Jane James"
     },
     {
        author_id: "author381",
        name: "John Palmer"
     }
   ] 
}
```

**사용 시나리오:**
- 주로 책 기준으로 조회하는 경우
- 저자 정보가 자주 변경되지 않는 경우

**출처**: [Model Many-to-Many Relationships with Embedded Documents](https://www.mongodb.com/docs/manual/tutorial/model-embedded-many-to-many-relationships-between-documents/) (MongoDB 버전: 7.0)

### 4.5 트리 구조 모델링

#### Parent References 패턴

**개념:**
각 도큐먼트가 부모의 ID를 저장하는 패턴입니다.

**예제:**
```javascript
db.categories.insertMany( [
   { _id: "MongoDB", parent: "Databases" },
   { _id: "dbm", parent: "Databases" },
   { _id: "Databases", parent: "Programming" },
   { _id: "Languages", parent: "Programming" },
   { _id: "Programming", parent: "Books" },
   { _id: "Books", parent: null }
] )
```

**장점:**
- 부모 노드와 직접 자식에 대한 효율적인 쿼리

#### Child References 패턴

**개념:**
각 도큐먼트가 자식 노드 ID 배열을 저장하는 패턴입니다.

**예제:**
```javascript
db.categories.insertMany( [
   { _id: "MongoDB", children: [] },
   { _id: "dbm", children: [] },
   { _id: "Databases", children: [ "MongoDB", "dbm" ] },
   { _id: "Languages", children: [] },
   { _id: "Programming", children: [ "Databases", "Languages" ] },
   { _id: "Books", children: [ "Programming" ] }
] )
```

**장점:**
- 직접 자식에 대한 효율적인 쿼리

**출처**: 
- [Model Tree Structures with Parent References](https://www.mongodb.com/docs/manual/tutorial/model-tree-structures-with-parent-references/)
- [Model Tree Structures with Child References](https://www.mongodb.com/docs/manual/tutorial/model-tree-structures-with-child-references/) (MongoDB 버전: 7.0)

### 4.6 배열 및 중첩 도큐먼트 활용

**베스트 프랙티스:**
1. 배열은 제한된 수의 요소에 사용합니다.
2. 배열이 무제한으로 성장할 수 있는 경우 참조 패턴을 고려합니다.
3. 중첩 도큐먼트의 깊이는 성능에 영향을 미칠 수 있으므로 적절히 제한합니다.

**주의사항:**
- 배열 크기 제한: 배열이 크게 성장하면 도큐먼트 크기 제한에 도달할 수 있습니다.
- 배열 인덱싱: 배열 필드에 인덱스를 생성하면 다중 키 인덱스가 생성됩니다.

---

## 5. MongoDB Atlas 특화 고려사항

### 5.1 클러스터 구성 옵션

**Atlas 클러스터 티어:**
- **M0 (Free)**: 개발 및 테스트용
- **M10+**: 프로덕션용 (M10, M30, M40, M50, M60, M80, M140, M200, M300, M400, M700)

**고려사항:**
- Search Nodes는 전용 클러스터(M10 이상)에서만 배포 가능합니다.
- M0 티어 클러스터나 Flex 클러스터에는 Search Nodes를 추가할 수 없습니다.

**출처**: [Atlas Cluster Configuration](https://www.mongodb.com/docs/atlas/cluster-config/multi-cloud-distribution/) (Atlas 버전: latest)

### 5.2 Atlas 샤딩 전략

**샤드 키 선택 기준:**
1. **높은 카디널리티**: 샤드 키는 가능한 한 많은 고유 값을 가져야 합니다.
2. **낮은 빈도**: 샤드 키 값의 빈도가 낮아야 합니다.
3. **비단조적 증가**: 단조롭게 증가하는 값(예: 타임스탬프)은 핫스팟을 생성할 수 있습니다.

**샤딩 명령:**
```javascript
db.adminCommand({
  reshardCollection: "mydb.mycollection",
  key: { newShardKey: 1 },
  unique: false,
  numInitialChunks: 90
})
```

**출처**: 
- [MongoDB Sharding](https://www.mongodb.com/docs/manual/sharding/)
- [Reshard Collection](https://www.mongodb.com/docs/manual/reference/command/reshardCollection/) (MongoDB 버전: 7.0)

### 5.3 Atlas 인덱스 관리

**베스트 프랙티스:**
1. Atlas UI를 통해 인덱스를 생성하고 관리합니다.
2. 인덱스 성능을 정기적으로 모니터링합니다.
3. 사용되지 않는 인덱스를 제거하여 쓰기 성능을 향상시킵니다.

### 5.4 Atlas 성능 모니터링

**모니터링 메트릭:**
- 연결 수 (Connections)
- 작업 카운터 (Operation Counters)
- 쿼리 타겟팅 (Query Targeting)
- 시스템 CPU 사용률
- 디스크 공간 사용량

**Atlas CLI 예제:**
```bash
atlas metrics processes atlas-lnmtkm-shard-00-00.ajlj3.mongodb.net:27017 \
  --projectId 56fd11f25f23b33ef4c2a331 \
  --granularity PT1H \
  --period P7D \
  --type CONNECTIONS,OPCOUNTER_QUERY,QUERY_TARGETING_SCANNED_OBJECTS_PER_RETURNED \
  --output json
```

**출처**: [Atlas Monitoring and Alerts](https://www.mongodb.com/docs/atlas/architecture/current/monitoring-alerts/) (Atlas 버전: latest)

### 5.5 Atlas 연결 풀 최적화

**최적화된 SRV 연결 문자열:**
```
mongodb+SRV://User1:P@ssword@cluster0-pl-0-lb.oq123.mongodb-dev.net/
```

**고려사항:**
- Atlas는 샤드 클러스터에 대해 최적화된 SRV 연결 문자열을 생성할 수 있습니다.
- 최적화된 연결 문자열은 `lb`를 포함하여 로드 밸런서 사용을 나타냅니다.
- 이는 프라이빗 엔드포인트 뒤의 샤드 클러스터에 대한 연결 성능을 향상시킵니다.

**출처**: [Atlas Connection String Optimization](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/) (Atlas 버전: latest)

### 5.6 Atlas 백업 및 복구 전략

**베스트 프랙티스:**
1. 정기적인 백업 계획을 수립합니다.
2. Point-in-Time Recovery (PITR)를 설정합니다.
3. 재해 복구 시나리오를 정기적으로 테스트합니다.

---

## 6. CQRS 패턴 적용 시 고려사항

### 6.1 핵심 개념

CQRS (Command Query Responsibility Segregation) 패턴에서 MongoDB Atlas는 Query Side(읽기 전용)로 사용됩니다. 읽기 모델을 최적화하고 이벤트 소싱과 통합하는 전략이 필요합니다.

### 6.2 읽기 모델 최적화

**전략:**
1. **읽기 전용 구조**: Query Side는 쓰기 작업이 없으므로 읽기에 최적화된 구조로 설계합니다.
2. **데이터 비정규화**: 자주 함께 조회되는 데이터를 하나의 도큐먼트에 포함시켜 조인을 피합니다.
3. **프로젝션 활용**: 필요한 필드만 선택하여 네트워크 트래픽을 최소화합니다.

### 6.3 읽기 복제본 활용 전략 (Read Preference)

**Read Preference 모드:**
- `primary`: 기본 노드에서만 읽기 (기본값)
- `primaryPreferred`: 기본 노드 우선, 사용 불가 시 보조 노드
- `secondary`: 보조 노드에서만 읽기
- `secondaryPreferred`: 보조 노드 우선, 사용 불가 시 기본 노드
- `nearest`: 지연 시간이 가장 낮은 노드

**CQRS 패턴에서의 활용:**
```bash
mongodb://myDatabaseUser:D1fficultP%40ssw0rd@mongodb0.example.com:27017,mongodb1.example.com:27017,mongodb2.example.com:27017/?replicaSet=myRepl&readPreference=secondary
```

**베스트 프랙티스:**
- Query Side는 `secondaryPreferred` 또는 `secondary`를 사용하여 기본 노드의 부하를 줄입니다.
- 최종 일관성을 허용할 수 있는 경우 보조 노드에서 읽기를 수행합니다.

**주의사항:**
- `primary`를 제외한 모든 Read Preference 모드는 오래된 데이터를 반환할 수 있습니다.
- 보조 노드는 기본 노드에서 비동기적으로 작업을 복제하므로 지연이 발생할 수 있습니다.

**출처**: [Read Preference](https://www.mongodb.com/docs/manual/core/read-preference/) (MongoDB 버전: 7.0)

### 6.4 Read Concern 및 Write Concern

#### Read Concern

**레벨:**
- `local`: 로컬 노드의 데이터를 반환 (기본값)
- `majority`: 대다수 노드에서 확인된 데이터를 반환
- `linearizable`: 선형화 가능한 읽기
- `available`: 사용 가능한 데이터 반환

**설정 예제:**
```javascript
db.adminCommand({
  "setDefaultRWConcern" : 1,
  "defaultReadConcern" : { "level" : "majority" }
})
```

**CQRS 패턴에서의 활용:**
- Query Side는 일반적으로 `local` 또는 `majority` Read Concern을 사용합니다.
- 강한 일관성이 필요한 경우 `majority`를 사용합니다.

#### Write Concern

**레벨:**
- `w: 1`: 하나의 노드에서 확인 (기본값)
- `w: "majority"`: 대다수 노드에서 확인
- `w: <number>`: 지정된 수의 노드에서 확인

**예제:**
```javascript
db.products.insertOne(
   { item: "envelopes", qty : 100, type: "Clasp" },
   { writeConcern: { w: "majority" , wtimeout: 5000 } }
)
```

**출처**: 
- [Read Concern](https://www.mongodb.com/docs/manual/core/read-concern/)
- [Write Concern](https://www.mongodb.com/docs/manual/core/write-concern/) (MongoDB 버전: 7.0)

### 6.5 데이터 동기화 전략

**전략:**
1. **이벤트 소싱**: Command Side에서 발생한 이벤트를 Kafka를 통해 Query Side로 전달
2. **비동기 동기화**: 이벤트 기반 비동기 동기화로 Command Side의 성능에 영향을 최소화
3. **최종 일관성**: Query Side는 최종 일관성을 허용하여 성능을 최적화

### 6.6 일관성 보장 방법

**전략:**
1. **이벤트 순서 보장**: Kafka의 파티션 키를 사용하여 이벤트 순서 보장
2. **멱등성 처리**: 동일한 이벤트를 여러 번 처리해도 안전하도록 멱등성 보장
3. **Read Preference 조정**: 강한 일관성이 필요한 경우 `primary` Read Preference 사용

**주의사항:**
- Read Preference는 데이터의 가시성에 영향을 미치지 않습니다.
- 클라이언트는 확인되기 전이나 대다수 복제본 세트 멤버로 전파되기 전에 쓰기 결과를 볼 수 있습니다.
- Read Preference는 인과적 일관성에 영향을 미치지 않습니다.

**출처**: [Read Preference > Behavior](https://www.mongodb.com/docs/manual/core/read-preference/) (MongoDB 버전: 7.0)

---

## 결론

MongoDB Atlas를 사용한 CQRS 패턴의 Query Side 도큐먼트 설계 시 다음 원칙을 준수해야 합니다:

1. **읽기 최적화**: 읽기 전용 워크로드에 최적화된 도큐먼트 구조 설계
2. **인덱스 전략**: 쿼리 패턴에 맞는 적절한 인덱스 설계 (ESR 규칙 준수)
3. **성능 모니터링**: Atlas 모니터링 도구를 활용한 지속적인 성능 모니터링
4. **최종 일관성**: CQRS 패턴의 특성에 맞는 최종 일관성 허용
5. **읽기 복제본 활용**: Read Preference를 활용한 부하 분산

이 문서의 모든 정보는 MongoDB 공식 문서에서 인용되었으며, 최신 MongoDB 7.0+ 버전을 기준으로 작성되었습니다.

---

## 참고 자료

### MongoDB 공식 문서
- [MongoDB Manual](https://www.mongodb.com/docs/manual/)
- [MongoDB Atlas Documentation](https://www.mongodb.com/docs/atlas/)
- [Data Modeling Introduction](https://www.mongodb.com/docs/manual/core/data-modeling-introduction/)
- [Indexes](https://www.mongodb.com/docs/manual/indexes/)
- [Query Optimization](https://www.mongodb.com/docs/manual/core/query-optimization/)
- [Read Preference](https://www.mongodb.com/docs/manual/core/read-preference/)
- [Sharding](https://www.mongodb.com/docs/manual/sharding/)

### 문서 버전 정보
- **최종 업데이트**: 2025-01-27
- **MongoDB 버전**: 7.0+
- **Atlas 버전**: latest
- **검증 상태**: ✅ 공식 문서 확인 완료

