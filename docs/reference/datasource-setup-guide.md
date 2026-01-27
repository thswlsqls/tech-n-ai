# 데이터소스 구축 가이드

**작성 일시**: 2026-01-XX  
**대상**: AWS Aurora Cluster 및 MongoDB Atlas 구축  
**목적**: 프로젝트 데이터소스 구축을 위한 실용적인 단계별 가이드

## 목차

1. [AWS Aurora Cluster 사용 가이드](#1-aws-aurora-cluster-사용-가이드)
   - [1.1 Aurora Cluster 생성](#11-aurora-cluster-생성)
   - [1.2 테이블 생성 가이드](#12-테이블-생성-가이드)
   - [1.3 권장 IDE 추천](#13-권장-ide-추천)
   - [1.4 비용절감 전략](#14-비용절감-전략)

2. [MongoDB Atlas Cluster 사용 가이드](#2-mongodb-atlas-cluster-사용-가이드)
   - [2.1 Atlas Cluster 생성](#21-atlas-cluster-생성)
   - [2.2 콜렉션 생성 가이드](#22-콜렉션-생성-가이드)
   - [2.3 권장 IDE 추천](#23-권장-ide-추천)
   - [2.4 비용절감 전략](#24-비용절감-전략)

3. [MongoDB Atlas Vector Search 사용 가이드](#3-mongodb-atlas-vector-search-사용-가이드)
   - [3.1 Vector Search 인덱스 생성](#31-vector-search-인덱스-생성)
   - [3.2 RAG 챗봇 통합](#32-rag-챗봇-통합)
   - [3.3 비용절감 전략](#33-비용절감-전략)

4. [데이터소스 구축 후 모듈 통합 가이드](#4-데이터소스-구축-후-모듈-통합-가이드)
   - [4.1 환경변수 설정](#41-환경변수-설정)
   - [4.2 모듈 통합](#42-모듈-통합)
   - [4.3 연결 테스트](#43-연결-테스트)

---

## 1. AWS Aurora Cluster 사용 가이드

### 1.1 Aurora Cluster 생성

#### 단계별 생성 가이드

1. **AWS 콘솔 접속**
   - AWS Management Console → RDS 서비스 선택

2. **데이터베이스 생성**
   - "데이터베이스 생성" 버튼 클릭
   - **엔진 옵션**: Amazon Aurora 선택
   - **엔진 버전**: Aurora MySQL 3.x (MySQL 8.0 호환) 선택

3. **템플릿 선택**
   - **개발 환경**: "프로덕션" 또는 "프로덕션 - 다중 AZ" (비용 고려 시 "프로덕션" 선택)
   - **프로덕션 환경**: "프로덕션 - 다중 AZ" 권장

4. **설정 구성**
   ```
   DB 클러스터 식별자: aurora-cluster-{환경명}
   마스터 사용자 이름: admin
   마스터 암호: 강력한 비밀번호 설정 (AWS Secrets Manager 권장)
   ```

5. **인스턴스 구성**
   - **개발 환경**: `db.t3.medium` 또는 `db.t4g.medium` (ARM 기반, 비용 효율적)
   - **프로덕션 환경**: `db.r6g.large` 이상 권장
   - **인스턴스 수**: 1개 (Writer), Reader는 필요 시 추가

6. **연결 설정**
   - **VPC**: 기존 VPC 선택 또는 새로 생성
   - **서브넷 그룹**: 기본값 사용 또는 커스텀
   - **퍼블릭 액세스**: 개발 환경은 "예", 프로덕션은 "아니오" 권장
   - **VPC 보안 그룹**: 새로 생성 또는 기존 사용
     - 인바운드 규칙: MySQL/Aurora (3306) 포트 허용

7. **데이터베이스 인증**
   - **암호 인증**: "암호 인증" 선택

8. **추가 구성**
   - **백업 보존 기간**: 7일 (개발), 30일 (프로덕션)
   - **백업 윈도우**: 기본값 사용
   - **암호화**: 활성화 권장

9. **모니터링**
   - **향상된 모니터링**: 활성화 권장 (비용 발생)

10. **생성 완료**
    - "데이터베이스 생성" 클릭
    - 생성 완료까지 약 10-15분 소요

#### Writer/Reader 엔드포인트 확인

생성 완료 후 RDS 콘솔에서 클러스터 선택:

1. **Writer 엔드포인트** 확인
   ```
   예시: aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
   ```
   - 위치: "연결 및 보안" 탭 → "Writer" 엔드포인트

2. **Reader 엔드포인트** 확인
   ```
   예시: aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
   ```
   - 위치: "연결 및 보안" 탭 → "Reader" 엔드포인트

**주의사항**:
- Writer 엔드포인트는 쓰기 작업 전용
- Reader 엔드포인트는 읽기 작업 전용 (부하 분산)
- Reader 인스턴스가 없는 경우 Reader 엔드포인트는 Writer로 자동 라우팅됨

#### 보안 그룹 설정

1. **EC2 콘솔** → **보안 그룹** 선택
2. Aurora 클러스터에 연결된 보안 그룹 선택
3. **인바운드 규칙** 편집:
   ```
   유형: MySQL/Aurora
   프로토콜: TCP
   포트: 3306
   소스: 
     - 개발: 0.0.0.0/0 (모든 IP, 테스트용)
     - 프로덕션: 특정 IP 또는 VPC CIDR 블록
   ```

**보안 권장사항**:
- 프로덕션 환경에서는 반드시 특정 IP만 허용
- VPN 또는 AWS Systems Manager Session Manager 사용 권장

#### 파라미터 그룹 설정 (선택사항)

기본 파라미터 그룹으로 충분하지만, 필요 시 커스텀 파라미터 그룹 생성:

1. **RDS 콘솔** → **파라미터 그룹** → **파라미터 그룹 생성**
2. **파라미터 그룹 패밀리**: `aurora-mysql8.0` 선택
3. 주요 파라미터:
   ```
   time_zone: Asia/Seoul
   character_set_server: utf8mb4
   collation_server: utf8mb4_unicode_ci
   ```

**참고 문서**:
- [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
- [Amazon Aurora 연결 관리](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.Connecting.html)

---

### 1.2 테이블 생성 가이드

#### 스키마 생성

프로젝트는 API 모듈별로 독립적인 스키마를 사용합니다:

```sql
-- auth 스키마 생성 (api-auth 모듈용)
CREATE DATABASE IF NOT EXISTS auth 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- archive 스키마 생성 (api-archive 모듈용)
CREATE DATABASE IF NOT EXISTS archive 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- chatbot 스키마 생성 (api-chatbot 모듈용)
CREATE DATABASE IF NOT EXISTS chatbot 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;
```

#### TSID Primary Key 테이블 생성 예시

**users 테이블 (auth 스키마)**:

```sql
USE auth;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL,
    deleted_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT UNSIGNED NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by BIGINT UNSIGNED NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 테이블 - TSID Primary Key 사용';
```

**archives 테이블 (archive 스키마)**:

```sql
USE archive;

CREATE TABLE IF NOT EXISTS archives (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    user_id BIGINT UNSIGNED NOT NULL,
    item_type VARCHAR(50) NOT NULL COMMENT 'CONTEST, NEWS_ARTICLE',
    item_id VARCHAR(255) NOT NULL,
    item_title VARCHAR(500) NOT NULL,
    item_summary TEXT,
    tag VARCHAR(100),
    memo TEXT,
    archived_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL,
    deleted_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT UNSIGNED NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by BIGINT UNSIGNED NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_user_item (user_id, item_type, item_id),
    INDEX idx_archived_at (archived_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='아카이브 테이블 - TSID Primary Key 사용';
```

#### 히스토리 테이블 생성 예시

**user_history 테이블 (auth 스키마)**:

```sql
USE auth;

CREATE TABLE IF NOT EXISTS user_history (
    history_id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    user_id BIGINT UNSIGNED NOT NULL,
    operation_type VARCHAR(20) NOT NULL COMMENT 'INSERT, UPDATE, DELETE',
    before_data JSON,
    after_data JSON,
    changed_by BIGINT UNSIGNED NULL,
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    change_reason VARCHAR(500),
    INDEX idx_user_id (user_id),
    INDEX idx_changed_at (changed_at)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 변경 이력 테이블';
```

#### Flyway 마이그레이션 스크립트 작성

프로젝트는 Flyway를 사용하여 데이터베이스 마이그레이션을 관리합니다.

**마이그레이션 파일 위치**: `datasource/aurora/src/main/resources/db/migration/`

**파일 명명 규칙**: `V{버전}__{설명}.sql`

**예시**: `V1__create_auth_schema.sql`

```sql
-- V1__create_auth_schema.sql
CREATE DATABASE IF NOT EXISTS auth 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE auth;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'TSID',
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6) NULL,
    deleted_by BIGINT UNSIGNED NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BIGINT UNSIGNED NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    updated_by BIGINT UNSIGNED NULL,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci
COMMENT='사용자 테이블';
```

**Flyway 실행**:
- 애플리케이션 시작 시 자동 실행
- 또는 수동 실행: `./gradlew flywayMigrate`

**참고 문서**:
- [Aurora MySQL 스키마 설계서](../step1/3.%20aurora-schema-design.md)
- [Aurora MySQL 베스트 프랙티스](../step1/aurora-mysql-schema-design-best-practices.md)
- [Flyway 공식 문서](https://flywaydb.org/documentation/)

---

### 1.3 권장 IDE 추천

#### 1. DBeaver (무료, 추천)

**장점**:
- 무료 오픈소스
- 다양한 데이터베이스 지원
- 직관적인 UI
- SQL 편집기 및 쿼리 실행 기능

**설치 및 연결**:
1. [DBeaver 다운로드](https://dbeaver.io/download/)
2. 새 데이터베이스 연결 생성
3. **MySQL** 선택
4. 연결 정보 입력:
   ```
   호스트: aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
   포트: 3306
   데이터베이스: auth (또는 archive, chatbot)
   사용자 이름: admin
   비밀번호: {설정한 비밀번호}
   ```
5. SSL 설정: "SSL 활성화" 체크

#### 2. DataGrip (유료, JetBrains)

**장점**:
- 강력한 SQL 편집 기능
- 코드 자동 완성
- 데이터베이스 스키마 시각화
- IntelliJ IDEA와 통합 가능

**단점**:
- 유료 (연간 구독)

**설치 및 연결**:
1. [DataGrip 다운로드](https://www.jetbrains.com/datagrip/)
2. 새 데이터 소스 → MySQL 선택
3. 연결 정보 입력 (DBeaver와 동일)

#### 3. MySQL Workbench (무료, 공식)

**장점**:
- MySQL 공식 도구
- 무료
- 스키마 설계 도구 포함

**단점**:
- UI가 다소 구식
- 성능이 상대적으로 느림

**설치 및 연결**:
1. [MySQL Workbench 다운로드](https://dev.mysql.com/downloads/workbench/)
2. 새 연결 생성
3. 연결 정보 입력

#### 4. IntelliJ IDEA Ultimate (유료)

**장점**:
- 개발 환경과 통합
- 데이터베이스 도구 내장
- 코드와 데이터베이스 동시 작업 가능

**단점**:
- 유료
- IDE가 무거움

**연결 방법**:
1. View → Tool Windows → Database
2. 새 데이터 소스 → MySQL 선택
3. 연결 정보 입력

**권장사항**:
- **개인 개발자**: DBeaver (무료, 기능 충분)
- **팀 개발**: DataGrip (유료, 강력한 기능)
- **MySQL 전용**: MySQL Workbench (무료, 공식 도구)

---

### 1.4 비용절감 전략

#### 인스턴스 크기 선택

**개발 환경**:
- **권장**: `db.t3.medium` 또는 `db.t4g.medium`
  - `db.t4g.medium`: ARM 기반, 비용 효율적 (약 20% 저렴)
  - 월 예상 비용: 약 $50-70 (서울 리전 기준)
- **최소**: `db.t3.small` (테스트용, 성능 제한적)

**프로덕션 환경**:
- **권장**: `db.r6g.large` 이상
  - 자동 스케일링 설정으로 필요 시 확장
  - 월 예상 비용: 약 $200-300 (서울 리전 기준)

#### 자동 스케일링 설정

1. **RDS 콘솔** → 클러스터 선택 → **수정**
2. **인스턴스 구성** → **자동 스케일링** 활성화
3. 설정:
   ```
   최소 용량: 1 ACU (Aurora Capacity Unit)
   최대 용량: 16 ACU (트래픽에 따라 조정)
   ```

**주의사항**:
- 자동 스케일링은 CPU 사용률 기반
- 스케일 다운은 15분 후 자동 실행
- 비용 모니터링 필수

#### 예약 인스턴스 활용 (장기 운영)

**1년 예약 인스턴스**:
- 약 30-40% 비용 절감
- **조건**: 1년 이상 사용 예정인 경우

**3년 예약 인스턴스**:
- 약 50-60% 비용 절감
- **조건**: 장기 운영 확실한 경우

**구매 방법**:
1. **RDS 콘솔** → **예약 인스턴스** → **예약 구매**
2. 인스턴스 타입 및 기간 선택
3. 결제

#### 스토리지 최적화

1. **스토리지 자동 스케일링** 활성화
   - 기본값: 10GB, 최대 128TB
   - 사용량에 따라 자동 확장

2. **불필요한 스냅샷 삭제**
   - 오래된 스냅샷 정기 삭제
   - 자동 백업 보존 기간 조정 (개발: 7일, 프로덕션: 30일)

3. **스토리지 암호화** 비활성화 (개발 환경, 선택사항)
   - 암호화는 약 5-10% 성능 저하
   - 프로덕션은 반드시 활성화

#### Reader 인스턴스 최적화

**개발 환경**:
- Reader 인스턴스 불필요 (비용 절감)
- Reader 엔드포인트는 Writer로 자동 라우팅

**프로덕션 환경**:
- 읽기 부하가 높은 경우에만 Reader 인스턴스 추가
- Reader 인스턴스는 Writer보다 작은 크기 선택 가능

#### 모니터링 및 최적화

1. **CloudWatch 모니터링**
   - CPU 사용률, 메모리 사용률 확인
   - 사용률이 지속적으로 낮으면 인스턴스 다운사이징 고려

2. **Performance Insights 활용**
   - 느린 쿼리 식별
   - 인덱스 최적화로 쿼리 성능 향상

3. **비용 알림 설정**
   - AWS Cost Explorer에서 비용 알림 설정
   - 예산 초과 시 알림 받기

**예상 월 비용 (서울 리전)**:
- **개발 환경** (db.t4g.medium, 단일 인스턴스): 약 $50-70
- **프로덕션 환경** (db.r6g.large, 다중 AZ): 약 $400-600
- **프로덕션 환경** (예약 인스턴스 1년): 약 $280-420

**참고 문서**:
- [Amazon Aurora 가격](https://aws.amazon.com/ko/rds/aurora/pricing/)
- [Aurora Serverless v2 비용 최적화](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/aurora-serverless-v2.cost-optimization.html)

---

## 2. MongoDB Atlas Cluster 사용 가이드

### 2.1 Atlas Cluster 생성

#### 단계별 생성 가이드

1. **MongoDB Atlas 계정 생성**
   - [MongoDB Atlas](https://www.mongodb.com/cloud/atlas) 접속
   - 계정 생성 또는 로그인

2. **프로젝트 생성**
   - "New Project" 클릭
   - 프로젝트 이름 입력 (예: `tech-n-ai`)

3. **클러스터 생성**
   - "Build a Database" 클릭
   - **배포 유형**: "M0 FREE" (개발) 또는 "M10" 이상 (프로덕션)

4. **클라우드 제공자 및 리전 선택**
   ```
   클라우드 제공자: AWS
   리전: ap-northeast-2 (Seoul, South Korea)
   ```

5. **클러스터 티어 선택**
   - **개발 환경**: M0 (Free) 또는 M10
   - **프로덕션 환경**: M30 이상 권장

6. **추가 설정**
   - **클러스터 이름**: `cluster-{환경명}` (예: `cluster-dev`)
   - **MongoDB 버전**: 7.0 이상 선택

7. **생성 완료**
   - "Create Cluster" 클릭
   - 생성 완료까지 약 3-5분 소요

#### 네트워크 액세스 설정

1. **Network Access** 메뉴 선택
2. **IP Access List** → **Add IP Address**
3. **개발 환경**:
   ```
   Access List Entry: 0.0.0.0/0 (모든 IP 허용, 테스트용)
   Comment: Development - Allow all IPs
   ```
4. **프로덕션 환경**:
   ```
   Access List Entry: {특정 IP 주소}/32
   Comment: Production - Specific IP only
   ```
   - 또는 VPC Peering 설정 (AWS VPC와 직접 연결)

**보안 권장사항**:
- 프로덕션 환경에서는 반드시 특정 IP만 허용
- VPN 또는 프라이빗 엔드포인트 사용 권장

#### 데이터베이스 사용자 생성

1. **Database Access** 메뉴 선택
2. **Add New Database User** 클릭
3. **인증 방법**: "Password" 선택
4. 사용자 정보 입력:
   ```
   Username: {사용자명}
   Password: {강력한 비밀번호} (자동 생성 권장)
   ```
5. **Database User Privileges**: "Atlas admin" 선택 (또는 커스텀 권한)
6. **Add User** 클릭

**주의사항**:
- 비밀번호는 안전하게 보관 (환경변수로 관리)
- 프로덕션 환경에서는 최소 권한 원칙 적용

#### 연결 문자열 확인

1. **Database** 메뉴 → 클러스터 선택
2. **Connect** 버튼 클릭
3. **Connect your application** 선택
4. **Driver**: Java 선택
5. **Version**: 4.11 이상 선택
6. 연결 문자열 복사:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```

**연결 문자열 형식**:
```
mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true
```

**참고 문서**:
- [MongoDB Atlas 연결 가이드](https://www.mongodb.com/docs/atlas/connect-to-database-deployment/)
- [MongoDB Atlas 네트워크 액세스](https://www.mongodb.com/docs/atlas/security/ip-access-list/)

---

### 2.2 콜렉션 생성 가이드

#### 데이터베이스 생성

MongoDB Atlas는 스키마가 없으므로, 데이터베이스와 콜렉션은 첫 도큐먼트 삽입 시 자동 생성됩니다.

**수동 생성 (선택사항)**:
```javascript
use tech_n_ai
```

#### 콜렉션 생성 예시

프로젝트의 실제 도큐먼트 구조를 반영한 콜렉션 생성 예시:

**sources 콜렉션**:

```javascript
// MongoDB Shell 또는 Atlas UI에서 실행
db.sources.insertOne({
  name: "Codeforces",
  type: "API",
  category: "개발자 대회 정보",
  url: "https://codeforces.com",
  apiEndpoint: "https://codeforces.com/api",
  description: "Codeforces API",
  priority: 1,
  reliabilityScore: 9,
  accessibilityScore: 8,
  dataQualityScore: 9,
  legalEthicalScore: 10,
  totalScore: 36,
  authenticationRequired: false,
  authenticationMethod: "None",
  rateLimit: "100 requests per minute",
  documentationUrl: "https://codeforces.com/apiHelp",
  updateFrequency: "실시간",
  dataFormat: "JSON",
  enabled: true,
  createdAt: new Date(),
  updatedAt: new Date()
});
```

**contests 콜렉션**:

```javascript
db.contests.insertOne({
  sourceId: ObjectId("..."), // SourcesDocument의 _id
  title: "Example Contest",
  startDate: new Date("2026-02-01"),
  endDate: new Date("2026-02-28"),
  status: "UPCOMING",
  description: "Example contest description",
  url: "https://example.com/contest",
  metadata: {
    sourceName: "Codeforces",
    prize: "$10,000",
    participants: 1000,
    tags: ["algorithm", "programming"]
  },
  createdAt: new Date(),
  updatedAt: new Date()
});
```

#### 인덱스 생성

프로젝트는 `MongoIndexConfig`를 통해 애플리케이션 시작 시 자동으로 인덱스를 생성합니다.

**수동 생성 (선택사항)**:

```javascript
// sources 콜렉션 인덱스
db.sources.createIndex({ name: 1 }, { unique: true });
db.sources.createIndex({ category: 1, priority: 1 });
db.sources.createIndex({ type: 1, enabled: 1 });

// contests 콜렉션 인덱스
db.contests.createIndex({ sourceId: 1, startDate: -1 });
db.contests.createIndex({ status: 1, startDate: -1 });

// news_articles 콜렉션 인덱스 (TTL 인덱스 포함)
db.news_articles.createIndex({ sourceId: 1, publishedAt: -1 });
db.news_articles.createIndex({ publishedAt: 1 }, { expireAfterSeconds: 7776000 }); // 90일

// archives 콜렉션 인덱스
db.archives.createIndex({ archiveTsid: 1 }, { unique: true });
db.archives.createIndex({ userId: 1, createdAt: -1 });
db.archives.createIndex({ userId: 1, itemType: 1, itemId: 1 }, { unique: true });
```

#### TTL 인덱스 설정

임시 데이터 자동 삭제를 위한 TTL 인덱스:

```javascript
// news_articles: 90일 후 자동 삭제
db.news_articles.createIndex(
  { publishedAt: 1 },
  { expireAfterSeconds: 7776000 } // 90일 = 90 * 24 * 60 * 60
);

// exception_logs: 90일 후 자동 삭제
db.exception_logs.createIndex(
  { occurredAt: 1 },
  { expireAfterSeconds: 7776000 }
);

// conversation_sessions: 90일 후 자동 삭제
db.conversation_sessions.createIndex(
  { lastMessageAt: 1 },
  { expireAfterSeconds: 7776000 }
);

// conversation_messages: 1년 후 자동 삭제
db.conversation_messages.createIndex(
  { createdAt: 1 },
  { expireAfterSeconds: 31536000 } // 1년 = 365 * 24 * 60 * 60
);
```

**주의사항**:
- TTL 인덱스는 `Date` 타입 필드에만 적용 가능
- 삭제 작업은 백그라운드에서 약 60초마다 실행
- 인덱스 생성 후 즉시 삭제되지 않음

#### MongoIndexConfig를 통한 자동 인덱스 생성

프로젝트는 `MongoIndexConfig` 클래스를 통해 애플리케이션 시작 시 자동으로 인덱스를 생성합니다.

**위치**: `datasource/mongodb/src/main/java/com/tech/n/ai/datasource/mongodb/config/MongoIndexConfig.java`

**동작 방식**:
- `@PostConstruct` 메서드에서 인덱스 생성
- 애플리케이션 시작 시 한 번만 실행
- 이미 존재하는 인덱스는 스킵

**수동 실행 불필요**: 애플리케이션을 시작하면 자동으로 인덱스가 생성됩니다.

**참고 문서**:
- [MongoDB Atlas 스키마 설계서](../step1/2.%20mongodb-schema-design.md)
- [MongoDB Atlas 베스트 프랙티스](../step1/mongodb-atlas-schema-design-best-practices.md)
- [MongoDB 인덱스 문서](https://www.mongodb.com/docs/manual/indexes/)

---

### 2.3 권장 IDE 추천

#### 1. MongoDB Compass (무료, 공식, 추천)

**장점**:
- MongoDB 공식 GUI 도구
- 무료
- 직관적인 UI
- 쿼리 빌더 및 집계 파이프라인 시각화

**설치 및 연결**:
1. [MongoDB Compass 다운로드](https://www.mongodb.com/products/compass)
2. 새 연결 생성
3. 연결 문자열 입력:
   ```
   mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/
   ```
4. 연결 테스트 후 "Connect" 클릭

#### 2. Studio 3T (유료, 무료 체험)

**장점**:
- 강력한 쿼리 편집 기능
- IntelliShell (자동 완성)
- 데이터 임포트/익스포트 기능
- 집계 파이프라인 빌더

**단점**:
- 유료 (30일 무료 체험)

**설치 및 연결**:
1. [Studio 3T 다운로드](https://studio3t.com/download/)
2. 새 연결 생성 → MongoDB Atlas 선택
3. 연결 정보 입력

#### 3. NoSQLBooster for MongoDB (유료, 무료 체험)

**장점**:
- SQL 쿼리 지원 (MongoDB 쿼리를 SQL로 변환)
- 시각적 쿼리 빌더
- 성능 분석 도구

**단점**:
- 유료 (무료 체험 제한적)

#### 4. IntelliJ IDEA Ultimate (유료)

**장점**:
- 개발 환경과 통합
- MongoDB 플러그인 지원
- 코드와 데이터베이스 동시 작업

**단점**:
- 유료
- MongoDB 전용 기능 제한적

**연결 방법**:
1. View → Tool Windows → Database
2. 새 데이터 소스 → MongoDB 선택
3. 연결 정보 입력

**권장사항**:
- **개인 개발자**: MongoDB Compass (무료, 공식 도구)
- **팀 개발**: Studio 3T (유료, 강력한 기능)
- **SQL 선호**: NoSQLBooster (유료, SQL 지원)

---

### 2.4 비용절감 전략

#### 클러스터 티어 선택

**개발 환경**:
- **권장**: M0 (Free) 또는 M10
  - M0: 무료, 512MB 스토리지, 공유 리소스
  - M10: 월 약 $9-15, 2GB RAM, 전용 리소스
- **최소**: M0 (Free) - 테스트용

**프로덕션 환경**:
- **권장**: M30 이상
  - M30: 월 약 $200-300, 8GB RAM
  - 자동 스케일링 설정으로 필요 시 확장

#### 자동 스케일링 설정

1. **Atlas 콘솔** → 클러스터 선택 → **Edit Configuration**
2. **Cluster Tier** → **Auto Scaling** 활성화
3. 설정:
   ```
   최소 인스턴스 크기: M10
   최대 인스턴스 크기: M40 (트래픽에 따라 조정)
   ```

**주의사항**:
- 자동 스케일링은 CPU 사용률 기반
- 스케일 다운은 24시간 후 자동 실행
- 비용 모니터링 필수

#### 스토리지 최적화

1. **TTL 인덱스 활용**
   - 임시 데이터 자동 삭제 (news_articles, exception_logs 등)
   - 스토리지 사용량 감소

2. **데이터 압축**
   - MongoDB는 기본적으로 압축 사용 (WiredTiger 스토리지 엔진)
   - 추가 설정 불필요

3. **불필요한 인덱스 제거**
   - 사용하지 않는 인덱스는 스토리지와 메모리 사용
   - 정기적으로 인덱스 사용률 확인

#### 읽기 전용 복제본 최소화

**개발 환경**:
- 읽기 전용 복제본 불필요 (비용 절감)

**프로덕션 환경**:
- 읽기 부하가 높은 경우에만 추가
- 읽기 전용 복제본은 Primary보다 작은 크기 선택 가능

#### 모니터링 및 최적화

1. **Atlas 모니터링 대시보드**
   - CPU 사용률, 메모리 사용률 확인
   - 사용률이 지속적으로 낮으면 클러스터 다운사이징 고려

2. **Performance Advisor 활용**
   - 느린 쿼리 식별
   - 인덱스 제안 받기

3. **비용 알림 설정**
   - Atlas 콘솔에서 비용 알림 설정
   - 예산 초과 시 알림 받기

**예상 월 비용 (서울 리전)**:
- **개발 환경** (M0 Free): $0
- **개발 환경** (M10): 약 $9-15
- **프로덕션 환경** (M30): 약 $200-300
- **프로덕션 환경** (M30, 자동 스케일링): 약 $200-500 (트래픽에 따라)

**참고 문서**:
- [MongoDB Atlas 가격](https://www.mongodb.com/pricing)
- [Atlas 비용 최적화](https://www.mongodb.com/docs/atlas/optimize-database-costs/)

---

## 3. MongoDB Atlas Vector Search 사용 가이드

### 3.1 Vector Search 인덱스 생성

#### Atlas 콘솔에서 Vector Search 인덱스 생성

1. **Atlas 콘솔** → 프로젝트 선택 → **Atlas Search** 메뉴
2. **Create Search Index** 클릭
3. **JSON Editor** 선택
4. 인덱스 정의 입력:

**ArchiveDocument Vector Search 인덱스**:

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "user_id"
    }
  ]
}
```

**인덱스 설정**:
- **Database**: `tech_n_ai`
- **Collection**: `archives`
- **Index Name**: `archives_vector_index`

5. **Next** → **Create Search Index** 클릭
6. 인덱스 생성 완료까지 약 1-2분 소요

**ContestDocument Vector Search 인덱스** (선택사항):

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "status"
    }
  ]
}
```

**NewsArticleDocument Vector Search 인덱스** (선택사항):

```json
{
  "fields": [
    {
      "type": "vector",
      "path": "embedding_vector",
      "numDimensions": 1536,
      "similarity": "cosine"
    },
    {
      "type": "filter",
      "path": "publishedAt"
    }
  ]
}
```

#### 인덱스 파라미터 설명

- **numDimensions**: 1536 (OpenAI text-embedding-3-small 기본 차원)
- **similarity**: `cosine` (코사인 유사도, 텍스트 검색에 적합)
- **filter**: 메타데이터 필터링을 위한 필드 (userId, status 등)

**참고**: OpenAI text-embedding-3-small은 기본적으로 1536 dimensions를 사용하며, `dimensions` 파라미터를 통해 차원을 축소할 수 있습니다 (최소 256). 차원 축소 시 성능 손실은 최소화되며, 저장 공간과 검색 속도를 개선할 수 있습니다.

**참고 문서**:
- [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [RAG 챗봇 설계서](../step12/rag-chatbot-design.md)

---

### 3.2 RAG 챗봇 통합

#### Vector Search 쿼리 예시

**ArchiveRepository에서 Vector Search 사용**:

```java
// ArchiveRepository.java
@Repository
public interface ArchiveRepository extends MongoRepository<ArchiveDocument, ObjectId> {
    
    // Vector Search를 사용한 유사도 검색
    @Aggregation(pipeline = {
        "{ $vectorSearch: { " +
        "  index: 'archives_vector_index', " +
        "  path: 'embedding_vector', " +
        "  queryVector: ?0, " +
        "  numCandidates: 100, " +
        "  limit: 10, " +
        "  filter: { user_id: ?1 } " +
        "} }",
        "{ $project: { " +
        "  _id: 1, " +
        "  archive_tsid: 1, " +
        "  item_title: 1, " +
        "  item_summary: 1, " +
        "  score: { $meta: 'vectorSearchScore' } " +
        "} }"
    })
    List<ArchiveDocument> searchSimilarArchives(List<Float> queryVector, String userId);
}
```

**VectorSearchService에서 사용**:

```java
@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {
    
    private final EmbeddingModel embeddingModel; // OpenAI text-embedding-3-small
    private final ArchiveRepository archiveRepository;
    
    public List<SearchResult> searchArchives(String query, String userId) {
        // 1. 쿼리 임베딩 생성
        Embedding embedding = embeddingModel.embed(query).content();
        List<Float> queryVector = embedding.vectorAsList();
        
        // 2. Vector Search 실행
        List<ArchiveDocument> results = archiveRepository.searchSimilarArchives(queryVector, userId);
        
        // 3. 결과 변환
        return results.stream()
            .map(doc -> new SearchResult(
                doc.getItemTitle(),
                doc.getItemSummary(),
                doc.getScore() // 유사도 점수
            ))
            .collect(Collectors.toList());
    }
}
```

#### 임베딩 생성 및 저장

**ArchiveDocument에 임베딩 저장**:

```java
@Service
@RequiredArgsConstructor
public class ArchiveEmbeddingService {
    
    private final EmbeddingModel embeddingModel;
    private final ArchiveRepository archiveRepository;
    
    public void generateAndSaveEmbedding(ArchiveDocument archive) {
        // 1. 임베딩 텍스트 생성
        String embeddingText = generateEmbeddingText(archive);
        
        // 2. 임베딩 생성
        Embedding embedding = embeddingModel.embed(embeddingText).content();
        List<Float> embeddingVector = embedding.vectorAsList();
        
        // 3. 도큐먼트 업데이트
        archive.setEmbeddingText(embeddingText);
        archive.setEmbeddingVector(embeddingVector);
        archiveRepository.save(archive);
    }
    
    private String generateEmbeddingText(ArchiveDocument document) {
        StringBuilder sb = new StringBuilder();
        sb.append(document.getItemTitle() != null ? document.getItemTitle() : "").append(" ");
        sb.append(document.getItemSummary() != null ? document.getItemSummary() : "").append(" ");
        sb.append(document.getTag() != null ? document.getTag() : "").append(" ");
        sb.append(document.getMemo() != null ? document.getMemo() : "");
        return sb.toString().trim();
    }
}
```

**참고 문서**:
- [RAG 챗봇 설계서](../step12/rag-chatbot-design.md)
- [MongoDB Atlas Vector Search 쿼리 예시](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)

---

### 3.3 비용절감 전략

#### Vector Search 인덱스 스토리지 비용 최적화

1. **필요한 필드만 인덱싱**
   - Vector Search 인덱스는 벡터 필드와 필터 필드만 포함
   - 불필요한 필드 제외

2. **차원 축소 고려**
   - OpenAI text-embedding-3-small의 `dimensions` 파라미터로 차원 축소 가능 (최소 256)
   - 1536 → 512 차원 축소 시 스토리지 약 66% 절감
   - 성능 손실은 최소화됨

**차원 축소 예시**:

```java
// OpenAI EmbeddingModel 설정
EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
    .apiKey(apiKey)
    .modelName("text-embedding-3-small")
    .dimensions(512) // 1536 → 512로 축소
    .build();
```

#### 임베딩 생성 비용 최적화

1. **배치 처리**
   - 여러 도큐먼트의 임베딩을 한 번에 생성
   - OpenAI API의 배치 엔드포인트 활용

2. **캐싱 전략**
   - 동일한 텍스트의 임베딩은 재사용
   - Redis 등에 임베딩 캐싱

3. **변경된 도큐먼트만 재임베딩**
   - `embeddingText` 필드 변경 시에만 재임베딩
   - 변경 감지 로직 구현

#### 불필요한 Vector Search 인덱스 제거

- 사용하지 않는 컬렉션의 Vector Search 인덱스 제거
- 정기적으로 인덱스 사용률 확인

**예상 비용**:
- **Vector Search 인덱스 스토리지**: 인덱스 크기에 따라 월 $0.01-0.10 per GB
- **임베딩 생성** (OpenAI): $0.02 per 1M tokens
- **검색 쿼리**: Atlas 클러스터 비용에 포함 (추가 비용 없음)

**참고 문서**:
- [OpenAI 임베딩 가격](https://openai.com/pricing)
- [MongoDB Atlas Vector Search 비용](https://www.mongodb.com/docs/atlas/atlas-vector-search/pricing/)

---

## 4. 데이터소스 구축 후 모듈 통합 가이드

### 4.1 환경변수 설정

#### .env 파일 설정

프로젝트 루트 디렉토리에 `.env` 파일 생성:

```bash
# Aurora DB Cluster 연결 정보
AURORA_WRITER_ENDPOINT=aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_READER_ENDPOINT=aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_USERNAME=admin
AURORA_PASSWORD=your-password-here
AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true&cachePrepStmts=true

# MongoDB Atlas 연결 정보
MONGODB_ATLAS_CONNECTION_STRING=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/tech_n_ai?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true
MONGODB_ATLAS_DATABASE=tech_n_ai

# 기타 설정
DB_FETCH_CHUNKSIZE=250
DB_BATCH_SIZE=50
TZ=Asia/Seoul
```

**주의사항**:
- `.env` 파일은 `.gitignore`에 포함되어야 함
- 프로덕션 환경에서는 AWS Secrets Manager 또는 환경변수 직접 설정 권장

#### API 모듈별 application.yml 설정

**api-auth 모듈** (`api/auth/src/main/resources/api-auth-application.yml`):

```yaml
module:
  aurora:
    schema: auth  # auth 스키마 사용
```

**api-archive 모듈** (`api/archive/src/main/resources/api-archive-application.yml`):

```yaml
module:
  aurora:
    schema: archive  # archive 스키마 사용
```

**api-chatbot 모듈** (`api/chatbot/src/main/resources/api-chatbot-application.yml`):

```yaml
module:
  aurora:
    schema: chatbot  # chatbot 스키마 사용
```

**api-contest, api-news 모듈**:
- Aurora DB 사용하지 않음 (MongoDB Atlas만 사용)
- `module.aurora.schema` 설정 불필요

#### datasource 모듈 설정 확인

**application-api-domain.yml** (`datasource/aurora/src/main/resources/application-api-domain.yml`):

이 파일은 `${module.aurora.schema}` 환경변수를 사용하여 동적으로 스키마를 참조합니다. 각 API 모듈의 설정이 자동으로 적용됩니다.

**참고 문서**:
- [datasource/aurora/README.md](../../datasource/aurora/README.md)
- [Aurora MySQL 스키마 설계서](../step1/3.%20aurora-schema-design.md)

---

### 4.2 모듈 통합

#### datasource-aurora 모듈 통합

**의존성 추가** (`api/auth/build.gradle` 예시):

```gradle
dependencies {
    implementation project(':datasource-aurora')
    // ... 기타 의존성
}
```

**Repository 사용 예시**:

```java
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserWriterRepository userWriterRepository;
    private final UserReaderRepository userReaderRepository;
    
    public UserEntity createUser(CreateUserRequest request) {
        // TSID 생성
        long tsid = TsidCreator.getTsid256().toLong();
        
        // 사용자 생성
        UserEntity user = new UserEntity();
        user.setId(tsid);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        // ... 기타 필드 설정
        
        // Writer Repository로 저장
        return userWriterRepository.save(user);
    }
    
    public Optional<UserEntity> findByEmail(String email) {
        // Reader Repository로 조회
        return userReaderRepository.findByEmail(email);
    }
}
```

#### datasource-mongodb 모듈 통합

**의존성 추가** (`api/contest/build.gradle` 예시):

```gradle
dependencies {
    implementation project(':datasource-mongodb')
    // ... 기타 의존성
}
```

**Repository 사용 예시**:

```java
@Service
@RequiredArgsConstructor
public class ContestService {
    
    private final ContestRepository contestRepository;
    
    public List<ContestDocument> getUpcomingContests() {
        return contestRepository.findByStatusOrderByStartDateDesc("UPCOMING");
    }
    
    public Page<ContestDocument> getContestsBySource(ObjectId sourceId, Pageable pageable) {
        return contestRepository.findBySourceIdOrderByStartDateDesc(sourceId, pageable);
    }
}
```

#### 트랜잭션 설정

**Aurora (JPA 트랜잭션)**:

```java
@Service
@Transactional
@RequiredArgsConstructor
public class ArchiveService {
    
    private final ArchiveWriterRepository archiveWriterRepository;
    
    @Transactional
    public ArchiveEntity createArchive(CreateArchiveRequest request) {
        // 트랜잭션 내에서 실행
        ArchiveEntity archive = new ArchiveEntity();
        // ... 필드 설정
        return archiveWriterRepository.save(archive);
    }
}
```

**MongoDB (트랜잭션, 선택사항)**:

```java
@Service
@RequiredArgsConstructor
public class ContestService {
    
    private final MongoTemplate mongoTemplate;
    
    @Transactional
    public void createContestWithSources(ContestDocument contest, SourcesDocument source) {
        // MongoDB 트랜잭션 (복제본 세트 필요)
        mongoTemplate.insert(contest);
        mongoTemplate.insert(source);
    }
}
```

**주의사항**:
- MongoDB 트랜잭션은 복제본 세트에서만 지원
- 단일 인스턴스에서는 트랜잭션 미지원

**참고 문서**:
- [datasource/mongodb/README.md](../../datasource/mongodb/README.md)
- [MongoDB Atlas 스키마 설계서](../step1/2.%20mongodb-schema-design.md)

---

### 4.3 연결 테스트

#### Aurora 연결 테스트

**1. 애플리케이션 시작 시 자동 테스트**:

애플리케이션을 시작하면 Spring Boot가 자동으로 데이터베이스 연결을 테스트합니다.

**2. 수동 연결 테스트 (SQL)**:

```sql
-- Writer 엔드포인트 연결 테스트
SELECT 1;

-- 스키마 확인
SHOW DATABASES;

-- 테이블 확인
USE auth;
SHOW TABLES;

-- 데이터 조회 테스트
SELECT * FROM users LIMIT 1;
```

**3. 애플리케이션 코드에서 테스트**:

```java
@SpringBootTest
class AuroraConnectionTest {
    
    @Autowired
    private UserReaderRepository userReaderRepository;
    
    @Test
    void testConnection() {
        // 연결 테스트
        long count = userReaderRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
```

#### MongoDB Atlas 연결 테스트

**1. MongoDB Compass에서 테스트**:

1. MongoDB Compass 실행
2. 연결 문자열 입력
3. "Connect" 클릭
4. 연결 성공 시 데이터베이스 목록 표시

**2. MongoDB Shell에서 테스트**:

```javascript
// 연결
mongosh "mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/tech_n_ai"

// 데이터베이스 확인
show dbs

// 콜렉션 확인
use tech_n_ai
show collections

// 데이터 조회 테스트
db.sources.findOne()
```

**3. 애플리케이션 코드에서 테스트**:

```java
@SpringBootTest
class MongoDBConnectionTest {
    
    @Autowired
    private ContestRepository contestRepository;
    
    @Test
    void testConnection() {
        // 연결 테스트
        long count = contestRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
```

#### 일반적인 연결 오류 해결

**Aurora 연결 오류**:

1. **"Communications link failure"**
   - 보안 그룹에서 포트 3306 허용 확인
   - VPC 설정 확인
   - 엔드포인트 주소 확인

2. **"Access denied for user"**
   - 사용자 이름 및 비밀번호 확인
   - 데이터베이스 사용자 권한 확인

3. **"Unknown database"**
   - 스키마가 생성되었는지 확인
   - `module.aurora.schema` 환경변수 확인

**MongoDB Atlas 연결 오류**:

1. **"Authentication failed"**
   - 사용자 이름 및 비밀번호 확인
   - IP 화이트리스트 확인

2. **"Connection timeout"**
   - 네트워크 액세스 설정 확인 (IP 화이트리스트)
   - 방화벽 설정 확인

3. **"SSL handshake failed"**
   - 연결 문자열에 `ssl=true` 포함 확인
   - SSL 인증서 문제 가능성 (드물음)

**디버깅 팁**:
- 연결 문자열의 특수 문자 URL 인코딩 확인
- 환경변수 로드 확인 (`.env` 파일 또는 시스템 환경변수)
- 로그 레벨을 `DEBUG`로 설정하여 상세 로그 확인

**참고 문서**:
- [Aurora 연결 문제 해결](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.Connecting.html)
- [MongoDB Atlas 연결 문제 해결](https://www.mongodb.com/docs/atlas/troubleshoot-connection/)

---

## 참고 문서

### 프로젝트 내 문서
- [Aurora MySQL 스키마 설계서](../step1/3.%20aurora-schema-design.md)
- [MongoDB Atlas 스키마 설계서](../step1/2.%20mongodb-schema-design.md)
- [Aurora MySQL 베스트 프랙티스](../step1/aurora-mysql-schema-design-best-practices.md)
- [MongoDB Atlas 베스트 프랙티스](../step1/mongodb-atlas-schema-design-best-practices.md)
- [RAG 챗봇 설계서](../step12/rag-chatbot-design.md)
- [datasource/aurora/README.md](../../datasource/aurora/README.md)
- [datasource/mongodb/README.md](../../datasource/mongodb/README.md)

### 공식 문서
- [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
- [Amazon Aurora 가격](https://aws.amazon.com/ko/rds/aurora/pricing/)
- [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)
- [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [MongoDB Atlas 가격](https://www.mongodb.com/pricing)
- [Flyway 공식 문서](https://flywaydb.org/documentation/)

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-XX
