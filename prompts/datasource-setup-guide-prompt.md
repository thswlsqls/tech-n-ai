# 데이터소스 구축 가이드 문서 작성 프롬프트

**작성 일시**: 2026-01-XX  
**대상**: 데이터소스 구축 가이드 문서 작성  
**목적**: AWS Aurora Cluster 및 MongoDB Atlas 구축을 위한 실용적인 가이드 문서 작성

## 프롬프트 목적

이 프롬프트는 다음 가이드 문서 작성을 위한 지시사항을 제공합니다:

1. **AWS Aurora Cluster 사용 가이드**
   - Aurora Cluster 생성 및 설정
   - 테이블 생성 가이드
   - 권장 IDE 추천
   - 비용절감 전략

2. **MongoDB Atlas Cluster 사용 가이드**
   - Atlas Cluster 생성 및 설정
   - 콜렉션 생성 가이드
   - 권장 IDE 추천
   - 비용절감 전략

3. **MongoDB Atlas Vector Search 사용 가이드**
   - Vector Search 인덱스 생성 및 설정
   - RAG 챗봇 통합 가이드
   - 비용절감 전략

4. **데이터소스 구축 후 모듈 통합 가이드**
   - datasource 모듈과 API 모듈 통합
   - 환경변수 설정
   - 연결 테스트 방법

## 핵심 요구사항

### 1. 실용성과 간결성
- **실행 가능한 단계별 가이드**: 복사-붙여넣기 가능한 명령어와 설정 예시 제공
- **불필요한 이론 제거**: LLM 오버엔지니어링 방지, 실제 구축에 필요한 정보만 포함
- **명확한 구조**: 각 섹션은 독립적으로 실행 가능하도록 구성
- **실제 사용 사례 중심**: 프로젝트의 실제 구조와 설정을 반영

### 2. 공식 문서 기반
- **AWS 공식 문서만 참고**: 
  - [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
  - [Amazon Aurora 가격](https://aws.amazon.com/ko/rds/aurora/pricing/)
  - AWS 공식 블로그 및 기술 문서
- **MongoDB 공식 문서만 참고**:
  - [MongoDB Atlas 공식 문서](https://www.mongodb.com/docs/atlas/)
  - [MongoDB Atlas Vector Search](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
  - [MongoDB Atlas 가격](https://www.mongodb.com/pricing)
- **신뢰할 수 없는 자료 금지**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음

### 3. 프로젝트 구조 반영
- **datasource 모듈 구조 참고**: `datasource/aurora/`, `datasource/mongodb/` 모듈 구조 반영
- **설계서 기반**: `docs/step1/` 디렉토리의 설계서 내용 반영
  - `docs/step1/3. aurora-schema-design.md`
  - `docs/step1/2. mongodb-schema-design.md`
  - `docs/step1/aurora-mysql-schema-design-best-practices.md`
  - `docs/step1/mongodb-atlas-schema-design-best-practices.md`
- **실제 엔티티/도큐먼트 구조 반영**: 프로젝트의 실제 테이블/콜렉션 구조 기반

### 4. 비용절감 전략
- **실제 비용 절감 방법**: 이론적 설명이 아닌 실행 가능한 전략
- **티어 선택 가이드**: 개발/스테이징/프로덕션 환경별 권장 티어
- **자동 스케일링 설정**: 비용 효율적인 자동 스케일링 구성
- **사용하지 않는 리소스 정리**: 정기적인 리소스 정리 방법

## 필수 참고 문서

### 프로젝트 내 설계 문서
1. **Aurora MySQL 설계서**: `docs/step1/3. aurora-schema-design.md`
   - 테이블 구조 및 스키마 매핑
   - TSID Primary Key 전략
   - 환경변수 설정 방법

2. **MongoDB Atlas 설계서**: `docs/step1/2. mongodb-schema-design.md`
   - 도큐먼트 구조 및 인덱스 전략
   - CQRS 동기화 설계
   - Vector Search 구조

3. **베스트 프랙티스 문서**:
   - `docs/step1/aurora-mysql-schema-design-best-practices.md`
   - `docs/step1/mongodb-atlas-schema-design-best-practices.md`

4. **모듈 README**:
   - `datasource/aurora/README.md`
   - `datasource/mongodb/README.md`

5. **RAG 챗봇 설계서**: `docs/step12/rag-chatbot-design.md`
   - Vector Search 사용 사례
   - 임베딩 모델 설정

### 프로젝트 코드 구조
- **Aurora 엔티티**: `datasource/aurora/src/main/java/com/tech/n/ai/datasource/aurora/entity/`
- **MongoDB 도큐먼트**: `datasource/mongodb/src/main/java/com/tech/n/ai/datasource/mongodb/document/`
- **설정 파일**: 
  - `datasource/aurora/src/main/resources/application-api-domain.yml`
  - `datasource/aurora/src/main/resources/application-batch-domain.yml`

## 문서 구조

### 1. AWS Aurora Cluster 사용 가이드

#### 1.1 Aurora Cluster 생성
- **필수 내용**:
  - AWS 콘솔에서 Aurora MySQL 3.x 클러스터 생성 단계
  - Writer/Reader 엔드포인트 확인 방법
  - 보안 그룹 설정 (VPC, 포트 3306)
  - 파라미터 그룹 설정 (필요 시)
- **제외할 내용**:
  - Aurora 아키텍처의 상세한 이론적 설명
  - 불필요한 옵션 설명

#### 1.2 테이블 생성 가이드
- **필수 내용**:
  - 프로젝트의 실제 테이블 구조 기반 DDL 예시
  - `docs/step1/3. aurora-schema-design.md`의 테이블 구조 반영
  - TSID Primary Key 생성 방법
  - 인덱스 생성 방법
  - Flyway 마이그레이션 스크립트 작성 가이드
- **제외할 내용**:
  - 일반적인 SQL 문법 설명
  - 프로젝트와 무관한 예시

#### 1.3 권장 IDE 추천
- **필수 내용**:
  - 데이터베이스 연결 및 쿼리 실행에 적합한 IDE 목록
  - 각 IDE의 Aurora 연결 설정 방법
  - 장단점 비교 (간단히)
- **제외할 내용**:
  - IDE의 상세한 기능 설명
  - 프로젝트와 무관한 IDE

#### 1.4 비용절감 전략
- **필수 내용**:
  - 개발 환경: `db.t3.medium` 또는 `db.t4g.medium` 권장
  - 프로덕션 환경: 자동 스케일링 설정
  - 예약 인스턴스 활용 (장기 운영 시)
  - 스토리지 자동 스케일링 설정
  - 불필요한 Reader 인스턴스 제거
  - 모니터링을 통한 리소스 최적화
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

### 2. MongoDB Atlas Cluster 사용 가이드

#### 2.1 Atlas Cluster 생성
- **필수 내용**:
  - MongoDB Atlas 콘솔에서 클러스터 생성 단계
  - 클러스터 티어 선택 가이드 (M0, M10, M30 등)
  - 네트워크 액세스 설정 (IP 화이트리스트)
  - 데이터베이스 사용자 생성
  - 연결 문자열 확인 방법
- **제외할 내용**:
  - MongoDB 아키텍처의 상세한 이론적 설명
  - 불필요한 옵션 설명

#### 2.2 콜렉션 생성 가이드
- **필수 내용**:
  - 프로젝트의 실제 도큐먼트 구조 기반 콜렉션 생성 예시
  - `docs/step1/2. mongodb-schema-design.md`의 도큐먼트 구조 반영
  - 인덱스 생성 방법 (ESR 규칙 준수)
  - TTL 인덱스 설정 방법
  - `MongoIndexConfig`를 통한 자동 인덱스 생성 가이드
- **제외할 내용**:
  - 일반적인 MongoDB 문법 설명
  - 프로젝트와 무관한 예시

#### 2.3 권장 IDE 추천
- **필수 내용**:
  - MongoDB 연결 및 쿼리 실행에 적합한 IDE 목록
  - 각 IDE의 Atlas 연결 설정 방법
  - 장단점 비교 (간단히)
- **제외할 내용**:
  - IDE의 상세한 기능 설명
  - 프로젝트와 무관한 IDE

#### 2.4 비용절감 전략
- **필수 내용**:
  - 개발 환경: M0 (Free) 또는 M10 권장
  - 프로덕션 환경: M30 이상 권장, 자동 스케일링 설정
  - 스토리지 최적화 (압축, TTL 인덱스 활용)
  - 불필요한 인덱스 제거
  - 읽기 전용 복제본 최소화 (필요 시에만)
  - 모니터링을 통한 리소스 최적화
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

### 3. MongoDB Atlas Vector Search 사용 가이드

#### 3.1 Vector Search 인덱스 생성
- **필수 내용**:
  - Atlas 콘솔에서 Vector Search 인덱스 생성 방법
  - `docs/step12/rag-chatbot-design.md`의 Vector Search 구조 반영
  - OpenAI text-embedding-3-small 모델 설정 (1536차원)
  - `ArchiveDocument`의 `embeddingVector` 필드 인덱싱
- **제외할 내용**:
  - 벡터 검색의 상세한 이론적 설명
  - 프로젝트와 무관한 임베딩 모델 설명

#### 3.2 RAG 챗봇 통합
- **필수 내용**:
  - Vector Search를 사용한 쿼리 예시
  - 애플리케이션 코드에서 Vector Search 사용 방법
  - `ArchiveRepository`의 Vector Search 쿼리 예시
- **제외할 내용**:
  - RAG 아키텍처의 상세한 이론적 설명
  - 프로젝트와 무관한 예시

#### 3.3 비용절감 전략
- **필수 내용**:
  - Vector Search 인덱스 스토리지 비용 최적화
  - 필요한 필드만 인덱싱
  - 임베딩 생성 비용 최적화 (배치 처리)
  - 불필요한 Vector Search 인덱스 제거
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

### 4. 데이터소스 구축 후 모듈 통합 가이드

#### 4.1 환경변수 설정
- **필수 내용**:
  - Aurora 연결 환경변수 설정 (`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT` 등)
  - MongoDB Atlas 연결 환경변수 설정 (`MONGODB_ATLAS_CONNECTION_STRING` 등)
  - `.env` 파일 설정 예시
  - 각 API 모듈의 `application.yml` 설정 방법
  - `module.aurora.schema` 환경변수 설정 (auth, archive, chatbot)
- **제외할 내용**:
  - 환경변수의 일반적인 설명
  - 프로젝트와 무관한 설정

#### 4.2 모듈 통합
- **필수 내용**:
  - `datasource-aurora` 모듈과 API 모듈 통합 방법
  - `datasource-mongodb` 모듈과 API 모듈 통합 방법
  - Repository 사용 예시
  - 트랜잭션 설정 방법
- **제외할 내용**:
  - Spring Data JPA/MongoDB의 일반적인 설명
  - 프로젝트와 무관한 예시

#### 4.3 연결 테스트
- **필수 내용**:
  - Aurora 연결 테스트 방법
  - MongoDB Atlas 연결 테스트 방법
  - 쿼리 실행 테스트
  - 에러 해결 가이드 (일반적인 연결 오류)
- **제외할 내용**:
  - 디버깅의 일반적인 설명
  - 프로젝트와 무관한 문제 해결 방법

## 작성 가이드라인

### 1. 구조화된 문서 작성
- 각 섹션은 명확한 제목과 하위 섹션으로 구성
- 코드 블록은 언어 지정 및 실행 가능한 예시 제공
- 단계별 가이드는 번호 목록 사용

### 2. 실제 코드 예시
- 프로젝트의 실제 구조를 반영한 코드 예시 제공
- 복사-붙여넣기 가능한 설정 파일 예시
- 실제 테이블/콜렉션 이름 사용

### 3. 스크린샷 및 다이어그램
- AWS/MongoDB Atlas 콘솔의 주요 화면 스크린샷 포함 (선택사항)
- 복잡한 설정은 다이어그램으로 설명 (필요 시)

### 4. 주의사항 및 경고
- 중요한 설정에 대한 주의사항 명시
- 일반적인 실수에 대한 경고 포함
- 보안 관련 주의사항 강조

### 5. 참고 링크
- 각 섹션의 관련 공식 문서 링크 제공
- 프로젝트 내 관련 문서 링크 제공

## 제외할 내용

다음 내용은 문서에서 제외해야 합니다:

1. **이론적 설명**: 
   - 데이터베이스 아키텍처의 상세한 이론
   - CQRS 패턴의 상세한 이론 (설계서 참고로 충분)
   - 일반적인 데이터베이스 개념 설명

2. **불필요한 옵션 설명**:
   - 프로젝트에서 사용하지 않는 설정 옵션
   - 고급 기능 중 실제로 사용하지 않는 기능

3. **중복된 정보**:
   - 설계서에 이미 상세히 설명된 내용
   - README에 이미 포함된 내용

4. **프로젝트와 무관한 내용**:
   - 다른 프로젝트의 예시
   - 일반적인 베스트 프랙티스 중 프로젝트와 무관한 내용

## 검증 기준

작성된 문서는 다음 기준을 만족해야 합니다:

1. **실행 가능성**: 문서의 지시사항을 따라 실제로 데이터소스를 구축할 수 있어야 함
2. **정확성**: 공식 문서 기반의 정확한 정보 제공
3. **간결성**: 불필요한 내용 없이 핵심만 포함
4. **일관성**: 프로젝트의 다른 문서와 일관된 스타일 유지
5. **완전성**: 구축부터 통합까지 전체 과정을 다룸

## 최종 확인 사항

문서 작성 완료 후 다음 사항을 확인하세요:

- [ ] 모든 코드 예시가 프로젝트의 실제 구조를 반영하는가?
- [ ] 공식 문서 링크가 모두 유효한가?
- [ ] 불필요한 이론적 설명이 제거되었는가?
- [ ] 실행 가능한 단계별 가이드인가?
- [ ] 비용절감 전략이 실제로 실행 가능한가?
- [ ] IDE 추천이 실제로 사용 가능한 도구인가?
- [ ] 모듈 통합 가이드가 프로젝트 구조를 정확히 반영하는가?

---

**참고**: 이 프롬프트는 데이터소스 구축 가이드 문서 작성을 위한 지시사항입니다. 문서 작성 시 이 프롬프트의 모든 요구사항을 충족해야 합니다.
