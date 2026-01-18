# 시스템 인프라 구축 가이드 문서 작성 프롬프트

**작성 일시**: 2026-01-XX  
**대상**: 시스템 인프라 구축 가이드 문서 작성  
**목적**: AWS 기반 전체 시스템 인프라 구축을 위한 실용적인 가이드 문서 작성

## 프롬프트 목적

이 프롬프트는 다음 가이드 문서 작성을 위한 지시사항을 제공합니다:

1. **ALB → Gateway 라우팅 구축 가이드**
   - AWS Application Load Balancer 설정
   - Gateway 서버 타겟 그룹 구성
   - 라우팅 규칙 설정
   - 헬스체크 설정

2. **AWS 서비스 구성 가이드**
   - Amazon Aurora MySQL 클러스터 구축
   - Amazon MSK (Managed Streaming for Apache Kafka) 구축
   - Amazon ElastiCache for Redis 구축
   - AWS Secrets Manager 설정
   - 기타 필요한 AWS 서비스 구성

3. **Spring Batch 애플리케이션 스케줄링 가이드**
   - AWS 서비스를 활용한 비용 최적화 스케줄링
   - EventBridge Scheduler 활용
   - ECS Fargate 기반 배치 실행
   - 비용 절감 전략

4. **비용 절감 전략 가이드**
   - 각 AWS 서비스별 비용 최적화 방법
   - 리소스 크기 권장사항
   - 자동 스케일링 설정
   - 예약 인스턴스 활용

## 핵심 요구사항

### 1. 실용성과 간결성
- **실행 가능한 단계별 가이드**: 복사-붙여넣기 가능한 명령어와 설정 예시 제공
- **불필요한 이론 제거**: LLM 오버엔지니어링 방지, 실제 구축에 필요한 정보만 포함
- **명확한 구조**: 각 섹션은 독립적으로 실행 가능하도록 구성
- **실제 사용 사례 중심**: 프로젝트의 실제 구조와 설정을 반영

### 2. 공식 문서 기반
- **AWS 공식 문서만 참고**: 
  - [AWS Application Load Balancer 사용자 가이드](https://docs.aws.amazon.com/ko_kr/elasticloadbalancing/latest/application/introduction.html)
  - [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
  - [Amazon MSK 개발자 가이드](https://docs.aws.amazon.com/ko_kr/msk/latest/developerguide/)
  - [Amazon ElastiCache for Redis 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonElastiCache/latest/red-ug/)
  - [AWS Secrets Manager 사용자 가이드](https://docs.aws.amazon.com/ko_kr/secretsmanager/latest/userguide/)
  - [Amazon EventBridge Scheduler 사용자 가이드](https://docs.aws.amazon.com/ko_kr/scheduler/latest/UserGuide/)
  - [Amazon ECS 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/)
  - AWS 공식 블로그 및 기술 문서
- **신뢰할 수 없는 자료 금지**: 블로그, 커뮤니티 자료, 비공식 문서는 참고하지 않음

### 3. 프로젝트 구조 반영
- **실제 구현 코드 기준**: 프로젝트 내 모든 모듈의 실제 구현 코드를 참고
- **Gateway 모듈 필수 참고**: `api/gateway` 모듈의 README.md 파일과 구현 코드 필수 참고
  - `api/gateway/README.md`: Gateway 아키텍처 및 라우팅 규칙
  - `api/gateway/src/main/java/com/tech/n/ai/api/gateway/config/GatewayConfig.java`: Gateway 설정
  - `api/gateway/src/main/resources/application*.yml`: 환경별 설정 파일
- **설계서 기반**: `docs/` 디렉토리의 모든 설계서 참고
  - `docs/step14/gateway-design.md`: Gateway 설계서
  - `docs/step10/batch-job-integration-design.md`: 배치 잡 통합 설계서
  - `docs/step11/cqrs-kafka-sync-design.md`: Kafka 동기화 설계서
  - `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 가이드
  - 기타 관련 설계서
- **모든 모듈 구현 참고**: 프로젝트 내 모든 모듈의 실제 구현 코드 반영

### 4. ALB → Gateway 라우팅 필수
- **ALB 타겟 그룹 설정**: Gateway 서버를 타겟으로 하는 타겟 그룹 구성
- **라우팅 규칙**: Gateway의 라우팅 규칙과 일치하는 ALB 라우팅 규칙 설정
- **헬스체크 설정**: Gateway 서버의 헬스체크 엔드포인트 설정 (`/actuator/health`)
- **타임아웃 설정**: ALB 타임아웃 600초 설정 (Gateway README.md 참고)
- **SSL/TLS 설정**: HTTPS 리스너 설정 및 인증서 구성

### 5. AWS 서비스 구성 필수
- **Kafka (Amazon MSK)**: 
  - 클러스터 생성 및 설정
  - 보안 그룹 설정
  - VPC 구성
  - 프로젝트의 Kafka 사용 패턴 반영 (`common/kafka` 모듈 참고)
- **Redis (Amazon ElastiCache)**:
  - 클러스터 생성 및 설정
  - OAuth State 저장용 설정 (`docs/step7/redis-optimization-best-practices.md` 참고)
  - Rate Limiting 설정
  - 보안 그룹 설정
- **Aurora MySQL**:
  - 클러스터 생성 및 설정 (Writer/Reader 엔드포인트)
  - 스키마 설정 (`docs/step1/3. aurora-schema-design.md` 참고)
  - 보안 그룹 설정
  - 파라미터 그룹 설정
- **Secrets Manager**:
  - JWT Secret Key 저장
  - 데이터베이스 비밀번호 저장
  - OAuth 클라이언트 정보 저장
  - Kafka 연결 정보 저장
  - Redis 연결 정보 저장
  - 환경변수와 Secrets Manager 연동 방법

### 6. 배치 애플리케이션 스케줄링 필수
- **비용 최적화 스케줄링**: AWS 서비스를 활용한 비용 최적화 방법
- **EventBridge Scheduler 활용**: 
  - 스케줄 정의
  - ECS Fargate 태스크 실행
  - 비용 최적화 설정
- **ECS Fargate 기반 실행**:
  - 태스크 정의
  - 클러스터 구성
  - 비용 최적화 설정 (Spot 인스턴스 활용 등)
- **배치 잡 실행 방법**: `batch/source` 모듈의 실제 Job 실행 방법 반영
  - `batch/source/README.md` 참고
  - Job 이름 상수 (`Constants` 클래스 참고)
  - 환경변수 설정

### 7. 비용 절감 전략 필수
- **각 AWS 서비스별 비용 최적화**:
  - Aurora: 인스턴스 크기, 자동 스케일링, 예약 인스턴스
  - MSK: 브로커 크기, 스토리지 최적화
  - ElastiCache: 노드 크기, 예약 노드
  - ALB: 사용량 기반 최적화
  - ECS Fargate: Spot 인스턴스 활용, 태스크 크기 최적화
  - EventBridge Scheduler: 비용 최적화 설정
- **환경별 권장사항**: Dev/Staging/Prod 환경별 리소스 크기 권장사항
- **자동 스케일링 설정**: 비용 효율적인 자동 스케일링 구성
- **사용하지 않는 리소스 정리**: 정기적인 리소스 정리 방법

## 필수 참고 문서

### 프로젝트 내 설계 문서
1. **Gateway 설계서**: `docs/step14/gateway-design.md`
   - ALB → Gateway 아키텍처
   - 라우팅 규칙
   - 타임아웃 설정

2. **Gateway README**: `api/gateway/README.md`
   - Gateway 아키텍처
   - 라우팅 규칙 상세
   - 요청 처리 흐름
   - 환경별 백엔드 서비스 URL

3. **Gateway 구현 코드**:
   - `api/gateway/src/main/java/com/tech/n/ai/api/gateway/config/GatewayConfig.java`
   - `api/gateway/src/main/resources/application*.yml`

4. **배치 잡 통합 설계서**: `docs/step10/batch-job-integration-design.md`
   - 배치 잡 구조
   - Job 이름 상수
   - 실행 방법

5. **배치 모듈 README**: `batch/source/README.md`
   - 배치 잡 실행 방법
   - 환경변수 설정
   - Job 이름 상수

6. **Kafka 동기화 설계서**: `docs/step11/cqrs-kafka-sync-design.md`
   - Kafka 사용 패턴
   - 토픽 구조

7. **Redis 최적화 가이드**: `docs/step7/redis-optimization-best-practices.md`
   - Redis 사용 패턴
   - OAuth State 저장
   - Rate Limiting

8. **Aurora 스키마 설계서**: `docs/step1/3. aurora-schema-design.md`
   - 스키마 구조
   - 환경변수 설정

9. **기타 설계서**: `docs/` 디렉토리의 모든 관련 설계서

### 프로젝트 코드 구조
- **Gateway 모듈**: `api/gateway/`
- **배치 모듈**: `batch/source/`
- **Kafka 모듈**: `common/kafka/`
- **Redis 설정**: 각 API 모듈의 설정 파일
- **Aurora 설정**: `datasource/aurora/`

## 문서 구조

### 1. ALB → Gateway 라우팅 구축 가이드

#### 1.1 ALB 생성 및 기본 설정
- **필수 내용**:
  - AWS 콘솔에서 ALB 생성 단계
  - 인터넷 대면/내부 ALB 선택 가이드
  - VPC 및 서브넷 선택
  - 보안 그룹 설정
  - SSL/TLS 인증서 설정 (ACM)
- **제외할 내용**:
  - ALB 아키텍처의 상세한 이론적 설명
  - 불필요한 옵션 설명

#### 1.2 Gateway 타겟 그룹 구성
- **필수 내용**:
  - Gateway 서버를 타겟으로 하는 타겟 그룹 생성
  - 헬스체크 설정 (`/actuator/health` 엔드포인트)
  - 타겟 등록 (ECS Fargate 서비스 또는 EC2 인스턴스)
  - 타겟 그룹 속성 설정 (연결 드레이닝, 타임아웃 등)
- **제외할 내용**:
  - 일반적인 타겟 그룹 개념 설명
  - 프로젝트와 무관한 예시

#### 1.3 라우팅 규칙 설정
- **필수 내용**:
  - Gateway의 라우팅 규칙과 일치하는 ALB 라우팅 규칙 설정
  - `api/gateway/README.md`의 라우팅 규칙 반영:
    - `/api/v1/auth/**` → Gateway 서버
    - `/api/v1/archive/**` → Gateway 서버
    - `/api/v1/contest/**` → Gateway 서버
    - `/api/v1/news/**` → Gateway 서버
    - `/api/v1/chatbot/**` → Gateway 서버
  - 기본 라우팅 규칙 설정
- **제외할 내용**:
  - 일반적인 라우팅 개념 설명
  - 프로젝트와 무관한 예시

#### 1.4 타임아웃 및 연결 설정
- **필수 내용**:
  - ALB 타임아웃 600초 설정 (`api/gateway/README.md` 참고)
  - Gateway 서버의 연결 풀 설정과의 조화
  - Keep-alive 설정
- **제외할 내용**:
  - 이론적인 타임아웃 설명
  - 프로젝트와 무관한 설정

### 2. AWS 서비스 구성 가이드

#### 2.1 Amazon Aurora MySQL 클러스터 구축
- **필수 내용**:
  - Aurora MySQL 3.x 클러스터 생성 단계
  - Writer/Reader 엔드포인트 확인 방법
  - 보안 그룹 설정 (VPC, 포트 3306)
  - 파라미터 그룹 설정 (필요 시)
  - `docs/step1/3. aurora-schema-design.md`의 스키마 구조 반영
  - 환경변수 설정 방법 (`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT` 등)
- **제외할 내용**:
  - Aurora 아키텍처의 상세한 이론적 설명
  - 불필요한 옵션 설명

#### 2.2 Amazon MSK (Kafka) 구축
- **필수 내용**:
  - MSK 클러스터 생성 단계
  - 브로커 구성 (개수, 인스턴스 타입)
  - 보안 그룹 설정
  - VPC 구성
  - 프로젝트의 Kafka 사용 패턴 반영 (`common/kafka` 모듈, `docs/step11/cqrs-kafka-sync-design.md` 참고)
  - 환경변수 설정 방법 (`KAFKA_BOOTSTRAP_SERVERS` 등)
- **제외할 내용**:
  - Kafka 아키텍처의 상세한 이론적 설명
  - 프로젝트와 무관한 설정

#### 2.3 Amazon ElastiCache for Redis 구축
- **필수 내용**:
  - ElastiCache Redis 클러스터 생성 단계
  - 노드 구성 (개수, 인스턴스 타입)
  - 보안 그룹 설정
  - VPC 구성
  - 프로젝트의 Redis 사용 패턴 반영 (`docs/step7/redis-optimization-best-practices.md` 참고)
    - OAuth State 저장: Key: `oauth:state:{state_value}`, TTL: 10분
    - Rate Limiting 설정
  - 환경변수 설정 방법 (`REDIS_HOST`, `REDIS_PORT` 등)
- **제외할 내용**:
  - Redis 아키텍처의 상세한 이론적 설명
  - 프로젝트와 무관한 설정

#### 2.4 AWS Secrets Manager 설정
- **필수 내용**:
  - Secrets Manager 시크릿 생성 단계
  - 저장할 시크릿 목록:
    - JWT Secret Key
    - Aurora MySQL 비밀번호
    - MongoDB Atlas 연결 정보
    - Kafka 연결 정보
    - Redis 연결 정보
    - OAuth 클라이언트 정보 (Google, Naver, Kakao)
    - OpenAI API Key
    - 기타 민감 정보
  - 환경변수와 Secrets Manager 연동 방법
  - 애플리케이션에서 Secrets Manager 사용 방법
- **제외할 내용**:
  - Secrets Manager의 상세한 이론적 설명
  - 프로젝트와 무관한 예시

### 3. Spring Batch 애플리케이션 스케줄링 가이드

#### 3.1 EventBridge Scheduler 설정
- **필수 내용**:
  - EventBridge Scheduler 생성 단계
  - 스케줄 정의 (Cron 표현식 또는 Rate 표현식)
  - ECS Fargate 태스크 실행 설정
  - 비용 최적화 설정
  - `batch/source/README.md`의 Job 이름 상수 반영
- **제외할 내용**:
  - EventBridge의 상세한 이론적 설명
  - 프로젝트와 무관한 예시

#### 3.2 ECS Fargate 태스크 정의
- **필수 내용**:
  - ECS 태스크 정의 생성 단계
  - 컨테이너 이미지 설정
  - 환경변수 설정 (Secrets Manager 연동)
  - 리소스 할당 (CPU, 메모리)
  - 비용 최적화 설정 (Spot 인스턴스 활용)
  - `batch/source` 모듈의 실제 실행 방법 반영
- **제외할 내용**:
  - ECS의 상세한 이론적 설명
  - 프로젝트와 무관한 예시

#### 3.3 배치 잡 실행 설정
- **필수 내용**:
  - Job 이름 파라미터 설정 (`--job.name=contest.codeforces.job`)
  - 환경변수 설정
  - Secrets Manager 연동
  - 로그 설정 (CloudWatch Logs)
  - 에러 처리 및 알림 설정
- **제외할 내용**:
  - Spring Batch의 상세한 이론적 설명
  - 프로젝트와 무관한 예시

#### 3.4 비용 최적화 스케줄링 전략
- **필수 내용**:
  - Spot 인스턴스 활용
  - 태스크 크기 최적화
  - 스케줄 최적화 (필요한 시간에만 실행)
  - 불필요한 리소스 정리
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

### 4. 비용 절감 전략 가이드

#### 4.1 Aurora MySQL 비용 최적화
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

#### 4.2 MSK 비용 최적화
- **필수 내용**:
  - 브로커 인스턴스 타입 선택 가이드
  - 스토리지 최적화
  - 불필요한 토픽 정리
  - 데이터 보존 정책 설정
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

#### 4.3 ElastiCache Redis 비용 최적화
- **필수 내용**:
  - 노드 인스턴스 타입 선택 가이드
  - 예약 노드 활용 (장기 운영 시)
  - 불필요한 데이터 정리 (TTL 설정)
  - 모니터링을 통한 리소스 최적화
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

#### 4.4 ECS Fargate 비용 최적화
- **필수 내용**:
  - Spot 인스턴스 활용
  - 태스크 크기 최적화 (CPU, 메모리)
  - 불필요한 태스크 정리
  - 자동 스케일링 설정
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

#### 4.5 ALB 비용 최적화
- **필수 내용**:
  - 사용량 기반 최적화
  - 불필요한 리스너 제거
  - 모니터링을 통한 최적화
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

#### 4.6 종합 비용 절감 전략
- **필수 내용**:
  - 환경별 리소스 크기 권장사항 (Dev/Staging/Prod)
  - 자동 스케일링 설정 가이드
  - 예약 인스턴스 활용 가이드
  - 사용하지 않는 리소스 정리 방법
  - 비용 모니터링 및 알림 설정
- **제외할 내용**:
  - 이론적인 비용 계산 공식
  - 프로젝트와 무관한 최적화 방법

## 작성 가이드라인

### 1. 구조화된 문서 작성
- 각 섹션은 명확한 제목과 하위 섹션으로 구성
- 코드 블록은 언어 지정 및 실행 가능한 예시 제공
- 단계별 가이드는 번호 목록 사용

### 2. 실제 코드 예시
- 프로젝트의 실제 구조를 반영한 코드 예시 제공
- 복사-붙여넣기 가능한 설정 파일 예시
- 실제 환경변수 이름 사용

### 3. 다이어그램
- 복잡한 아키텍처는 다이어그램으로 설명 (필요 시)
- AWS 콘솔의 주요 화면 스크린샷 포함 (선택사항)

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
   - AWS 서비스 아키텍처의 상세한 이론
   - 일반적인 클라우드 개념 설명
   - 프로젝트와 무관한 일반적인 설명

2. **불필요한 옵션 설명**:
   - 프로젝트에서 사용하지 않는 설정 옵션
   - 고급 기능 중 실제로 사용하지 않는 기능

3. **중복된 정보**:
   - 설계서에 이미 상세히 설명된 내용
   - README에 이미 포함된 내용

4. **프로젝트와 무관한 내용**:
   - 다른 프로젝트의 예시
   - 일반적인 베스트 프랙티스 중 프로젝트와 무관한 내용

5. **오버엔지니어링**:
   - 불필요하게 복잡한 설정
   - 실제로 필요하지 않은 최적화
   - 과도한 보안 설정

## 검증 기준

작성된 문서는 다음 기준을 만족해야 합니다:

1. **실행 가능성**: 문서의 지시사항을 따라 실제로 인프라를 구축할 수 있어야 함
2. **정확성**: 공식 문서 기반의 정확한 정보 제공
3. **간결성**: 불필요한 내용 없이 핵심만 포함
4. **일관성**: 프로젝트의 다른 문서와 일관된 스타일 유지
5. **완전성**: 구축부터 운영까지 전체 과정을 다룸
6. **프로젝트 반영**: 실제 구현 코드와 설계서를 정확히 반영

## 최종 확인 사항

문서 작성 완료 후 다음 사항을 확인하세요:

- [ ] 모든 설정이 프로젝트의 실제 구조를 반영하는가?
- [ ] 공식 문서 링크가 모두 유효한가?
- [ ] 불필요한 이론적 설명이 제거되었는가?
- [ ] 실행 가능한 단계별 가이드인가?
- [ ] 비용절감 전략이 실제로 실행 가능한가?
- [ ] ALB → Gateway 라우팅이 정확히 반영되었는가?
- [ ] 모든 AWS 서비스 구성 가이드가 포함되었는가?
- [ ] 배치 애플리케이션 스케줄링 가이드가 비용 최적화를 고려했는가?
- [ ] Gateway 모듈의 README.md와 구현 코드가 정확히 반영되었는가?
- [ ] 모든 모듈의 구현이 정확히 반영되었는가?

---

**참고**: 이 프롬프트는 시스템 인프라 구축 가이드 문서 작성을 위한 지시사항입니다. 문서 작성 시 이 프롬프트의 모든 요구사항을 충족해야 합니다.
