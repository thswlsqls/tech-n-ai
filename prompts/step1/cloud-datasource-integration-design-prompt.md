# AWS Aurora 및 MongoDB Atlas 클라우드 데이터소스 연동 설계서 작성 프롬프트

## 역할 및 목적

당신은 **인프라 아키텍트 전문가**로서, AWS Aurora MySQL과 MongoDB Atlas Cluster 서비스를 활용한 클라우드 데이터소스 연동 설계서를 작성합니다. 이 설계서는 프로덕션 환경에서 안정적이고 비용 효율적인 데이터베이스 인프라 구축을 위한 실무 가이드를 제공합니다.

## 설계서 작성 범위

다음 두 가지 주요 영역에 대한 상세 설계서를 작성합니다:

1. **AWS Aurora MySQL 연동 설계서**
   - Aurora Cluster 생성 및 구성 가이드
   - 데이터베이스 스키마 및 테이블 구축 가이드
   - 프로젝트 연동 및 연결 설정 가이드
   - 성능 최적화 및 비용 절감 전략

2. **MongoDB Atlas Cluster 연동 설계서**
   - Atlas Cluster 생성 및 구성 가이드
   - 컬렉션 구축 및 인덱스 생성 가이드
   - 프로젝트 연동 및 연결 설정 가이드
   - 성능 최적화 및 비용 절감 전략

## 필수 참고 자료

### 설계서 문서
다음 설계서들을 **반드시 참고**하여 일관성 있는 설계를 유지합니다:

1. **Aurora MySQL 설계서**
   - `docs/step1/3. aurora-schema-design.md`: 스키마 설계 및 테이블 구조
   - `docs/step1/aurora-mysql-schema-design-best-practices.md`: 베스트 프랙티스

2. **MongoDB Atlas 설계서**
   - `docs/step1/2. mongodb-schema-design.md`: 도큐먼트 설계 및 컬렉션 구조
   - `docs/step1/mongodb-atlas-schema-design-best-practices.md`: 베스트 프랙티스

3. **관련 설계서**
   - `docs/step1/1. multimodule-structure-verification.md`: 프로젝트 구조
   - `docs/step1/4. schema-design-verification-report.md`: 스키마 검증 보고서
   - `docs/step11/cqrs-kafka-sync-design.md`: CQRS 패턴 및 동기화 설계
   - `docs/step13/user-bookmark-feature-design.md`: 북마크 기능 설계
   - `docs/step12/rag-chatbot-design.md`: 챗봇 설계

### 구현 코드
다음 구현 코드를 **반드시 분석**하여 실제 구현과 일치하는 가이드를 작성합니다:

1. **Aurora 연동 코드**
   - `domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/mariadb/config/`: 데이터소스 설정 클래스
   - `domain/aurora/src/main/resources/application-api-domain.yml`: API 모듈 연결 설정
   - `domain/aurora/src/main/resources/application-batch-domain.yml`: 배치 모듈 연결 설정
   - `domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/mariadb/entity/`: 엔티티 클래스

2. **MongoDB Atlas 연동 코드**
   - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/MongoClientConfig.java`: MongoDB 클라이언트 설정
   - `domain/mongodb/src/main/resources/application-mongodb-domain.yml`: MongoDB 연결 설정
   - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/`: 도큐먼트 클래스
   - `domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/MongoIndexConfig.java`: 인덱스 설정

## 설계서 구조 및 요구사항

### 1. AWS Aurora MySQL 연동 설계서

#### 1.1 Aurora Cluster 생성 및 구성 가이드
- **Aurora MySQL 버전 선택**: 3.x (MySQL 8.0+ 호환)
- **클러스터 구성**: Writer/Reader 엔드포인트 분리
- **인스턴스 사양 선택 가이드**: 워크로드 분석 기반 인스턴스 타입 추천
- **네트워크 구성**: VPC, 서브넷, 보안 그룹 설정
- **백업 및 복구 전략**: 자동 백업, Point-in-Time Recovery 설정
- **모니터링 및 알림**: CloudWatch 메트릭, Performance Insights 설정

#### 1.2 데이터베이스 스키마 및 테이블 구축 가이드
- **스키마 생성**: `auth`, `bookmark`, `chatbot` 스키마 생성 DDL
- **테이블 생성**: `docs/step1/3. aurora-schema-design.md`의 DDL 예제 기반
- **인덱스 생성**: Command Side 최적화를 위한 최소 인덱스 전략
- **Foreign Key 제약조건**: 스키마 간 참조 제약조건 처리 방법
- **Flyway 마이그레이션**: 버전 관리 및 마이그레이션 전략

#### 1.3 프로젝트 연동 및 연결 설정 가이드
- **환경변수 설정**: `AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`
- **JDBC 드라이버**: AWS RDS JDBC Driver (`software.aws.rds.jdbc.mysql.Driver`) 설정
- **HikariCP 연결 풀**: Writer/Reader 분리 설정, 연결 풀 최적화
- **모듈별 스키마 매핑**: `module.aurora.schema` 설정 방법
- **트랜잭션 관리**: JPA 트랜잭션 설정 및 읽기 전용 복제본 활용

#### 1.4 성능 최적화 전략
- **읽기 전용 복제본 활용**: Reader 엔드포인트를 통한 읽기 부하 분산
- **연결 풀 최적화**: HikariCP 설정 최적화 (pool size, timeout 등)
- **인덱스 전략**: Command Side 쓰기 최적화를 위한 인덱스 최소화
- **쿼리 최적화**: 배치 INSERT, Prepared Statement 캐싱
- **Aurora 특화 기능**: Fast Insert, 병렬 쿼리 활용

#### 1.5 비용 절감 전략
- **인스턴스 크기 최적화**: 워크로드 분석 기반 인스턴스 다운사이징
- **예약 인스턴스**: 장기 사용 시 예약 인스턴스 활용
- **스토리지 최적화**: 자동 스케일링, 불필요한 스냅샷 정리
- **모니터링 기반 최적화**: CloudWatch 메트릭 분석을 통한 비용 최적화

### 2. MongoDB Atlas Cluster 연동 설계서

#### 2.1 Atlas Cluster 생성 및 구성 가이드
- **클러스터 티어 선택**: M10 이상 프로덕션 권장, 워크로드 분석 기반 선택
- **클러스터 구성**: Replica Set 구성, 샤딩 전략 (필요 시)
- **네트워크 구성**: VPC Peering, Private Endpoint 설정
- **백업 및 복구 전략**: 자동 백업, Point-in-Time Recovery 설정
- **모니터링 및 알림**: Atlas Monitoring, Performance Advisor 활용

#### 2.2 컬렉션 구축 및 인덱스 생성 가이드
- **컬렉션 생성**: `docs/step1/2. mongodb-schema-design.md`의 도큐먼트 구조 기반
- **인덱스 생성**: ESR 규칙 준수, 쿼리 패턴 기반 인덱스 설계
- **TTL 인덱스**: 임시 데이터 자동 삭제 설정
- **부분 인덱스**: 선택적 인덱싱을 통한 인덱스 크기 최소화
- **MongoIndexConfig 활용**: 애플리케이션 시작 시 자동 인덱스 생성

#### 2.3 프로젝트 연동 및 연결 설정 가이드
- **연결 문자열 설정**: `MONGODB_ATLAS_CONNECTION_STRING` 환경변수 설정
- **MongoClient 설정**: `MongoClientConfig` 클래스의 연결 풀 최적화
- **Read Preference**: `secondaryPreferred` 설정 (CQRS Query Side 최적화)
- **Write Concern**: `majority` 설정 (데이터 일관성 보장)
- **환경별 설정**: local, dev, prod 프로파일별 설정

#### 2.4 성능 최적화 전략
- **읽기 복제본 활용**: Read Preference를 통한 읽기 부하 분산
- **연결 풀 최적화**: 클러스터 티어별 최적 연결 풀 크기 설정
- **인덱스 전략**: ESR 규칙 준수, 커버링 인덱스 활용
- **프로젝션 활용**: 필요한 필드만 선택하여 네트워크 트래픽 최소화
- **집계 파이프라인 최적화**: `$match` 단계 최적화

#### 2.5 비용 절감 전략
- **클러스터 티어 최적화**: 워크로드 분석 기반 최소 사양 선택
- **스토리지 최적화**: 인덱스 크기 최소화, TTL 인덱스 활용
- **네트워크 최적화**: Private Endpoint를 통한 데이터 전송 비용 절감
- **모니터링 기반 최적화**: Performance Advisor 권장사항 적용

## 설계서 작성 원칙

### 필수 준수 사항

1. **공식 문서 기반**: 모든 기술 정보는 AWS Aurora 및 MongoDB Atlas 공식 문서를 기반으로 작성
   - AWS Aurora: https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/
   - MongoDB Atlas: https://www.mongodb.com/docs/atlas/
   - MongoDB Manual: https://www.mongodb.com/docs/manual/

2. **프로젝트 통합성**: 기존 설계서 및 구현 코드와 완전히 일치하는 가이드 작성
   - 스키마 설계서의 DDL 예제 그대로 사용
   - 구현된 설정 클래스와 일치하는 설정 방법 제시
   - 환경변수 이름 및 구조 일치

3. **베스트 프랙티스 준수**: 
   - `aurora-mysql-schema-design-best-practices.md`의 원칙 준수
   - `mongodb-atlas-schema-design-best-practices.md`의 원칙 준수
   - CQRS 패턴 특성 고려 (Command Side 쓰기 최적화, Query Side 읽기 최적화)

4. **실무 중심**: 이론보다는 실제 구축 가능한 단계별 가이드 제공
   - AWS Console/CLI 명령어 예제
   - MongoDB Atlas UI/CLI 명령어 예제
   - 환경변수 설정 예제
   - 연결 테스트 방법

5. **비용 효율성**: 인프라 아키텍트 관점에서 비용 최적화 전략 포함
   - 워크로드 분석 기반 인스턴스/클러스터 크기 추천
   - 예약 인스턴스 활용 전략
   - 스토리지 최적화 방법
   - 모니터링 기반 비용 최적화

### 지양 사항

1. **오버엔지니어링 금지**: 
   - 불필요한 고가용성 구성 (단일 인스턴스로 충분한 경우)
   - 과도한 복제본 수 (실제 워크로드에 맞는 최소 구성)
   - 불필요한 샤딩 (데이터 크기가 작은 경우)

2. **이론적 설명 최소화**: 
   - 개념 설명보다는 실무 가이드 중심
   - 공식 문서 링크로 대체 가능한 상세 설명 생략

3. **중복 정보 제거**: 
   - 기존 설계서에 이미 포함된 정보는 참조만 하고 중복 작성 금지
   - DDL 예제는 설계서 참조로 대체

## 설계서 출력 형식

### 문서 구조

```markdown
# [서비스명] 클라우드 데이터소스 연동 설계서

**작성 일시**: YYYY-MM-DD
**대상**: 프로덕션 환경 구축
**버전**: 1.0

## 목차
1. [개요](#개요)
2. [클러스터 생성 및 구성](#클러스터-생성-및-구성)
3. [스키마/컬렉션 구축](#스키마컬렉션-구축)
4. [프로젝트 연동](#프로젝트-연동)
5. [성능 최적화](#성능-최적화)
6. [비용 절감 전략](#비용-절감-전략)
7. [모니터링 및 유지보수](#모니터링-및-유지보수)
8. [트러블슈팅](#트러블슈팅)

## 개요
- 서비스 소개
- 아키텍처 개요
- 참고 설계서 링크

## 클러스터 생성 및 구성
- 단계별 생성 가이드
- 네트워크 구성
- 보안 설정
- 백업 설정

## 스키마/컬렉션 구축
- 스키마/데이터베이스 생성
- 테이블/컬렉션 생성 (설계서 참조)
- 인덱스 생성
- 마이그레이션 전략

## 프로젝트 연동
- 환경변수 설정
- 연결 설정
- 연결 테스트
- 환경별 설정

## 성능 최적화
- 연결 풀 최적화
- 인덱스 전략
- 쿼리 최적화
- 읽기 복제본 활용

## 비용 절감 전략
- 인스턴스/클러스터 크기 최적화
- 예약 인스턴스 활용
- 스토리지 최적화
- 모니터링 기반 최적화

## 모니터링 및 유지보수
- 모니터링 설정
- 알림 설정
- 성능 분석
- 유지보수 계획

## 트러블슈팅
- 일반적인 문제 및 해결 방법
- 성능 이슈 해결
- 연결 문제 해결
```

### 코드 예제 형식

- **AWS CLI 명령어**: 실제 실행 가능한 명령어 제공
- **SQL/DDL**: 설계서 참조 또는 간단한 예제
- **환경변수 설정**: `.env` 파일 예제
- **연결 테스트**: 실제 테스트 가능한 코드 예제

## 검증 기준

설계서 작성 완료 후 다음 기준으로 자체 검증:

1. ✅ 모든 참고 설계서 및 구현 코드 분석 완료
2. ✅ 공식 문서 기반 정보만 포함 (출처 명시)
3. ✅ 프로젝트 구조와 완전히 일치
4. ✅ 실제 구축 가능한 단계별 가이드 제공
5. ✅ 비용 최적화 전략 포함
6. ✅ 성능 최적화 전략 포함
7. ✅ 오버엔지니어링 없음
8. ✅ 중복 정보 최소화

## 최종 출력

다음 두 개의 독립적인 설계서를 작성합니다:

1. `docs/step1/5. aws-aurora-integration-guide.md`
2. `docs/step1/6. mongodb-atlas-integration-guide.md`

각 설계서는 위의 구조를 따르며, 실무에서 바로 활용 가능한 수준의 상세 가이드를 제공합니다.

---

**중요**: 이 프롬프트는 설계서 작성을 지시하는 메타 프롬프트입니다. 실제 설계서 작성 시 이 프롬프트의 모든 요구사항을 충실히 반영하여 작성하세요.
