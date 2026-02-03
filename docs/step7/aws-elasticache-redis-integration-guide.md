# AWS ElastiCache for Redis 클라우드 데이터소스 연동 설계서

**작성 일시**: 2026-01-20  
**대상**: 프로덕션 및 로컬 개발 환경 구축  
**Redis 버전**: 7.x  
**버전**: 1.3  

## 목차

1. [개요](#개요)
2. [배포 옵션 선택](#배포-옵션-선택)
   - 서버리스 vs 노드 기반 캐시 비교
   - 현재 프로젝트 워크로드 분석
   - 추천 배포 옵션
3. [클러스터 생성 및 구성](#클러스터-생성-및-구성)
   - AWS Console (GUI) 가이드
   - AWS CLI 가이드
4. [네트워크 및 보안 구성](#네트워크-및-보안-구성)
5. [로컬 개발 환경 Redis 설치 및 설정](#로컬-개발-환경-redis-설치-및-설정)
   - macOS에서 Redis 설치
   - Windows에서 Redis 설치
   - Linux에서 Redis 설치
   - Docker로 Redis 실행
   - Redis 서버 시작 및 중지
   - 기본 설정 확인 및 변경
   - 프로젝트 연동 설정
   - 연결 테스트
   - 주의사항
   - Redis GUI 관리 도구
6. [프로젝트 연동](#프로젝트-연동)
   - 환경변수 설정
   - 로컬 환경에서 접속하기 (SSH 터널링, VPN, SSM)
   - 연결 테스트
7. [모니터링 및 알림 설정](#모니터링-및-알림-설정)
8. [개발/테스트 환경 예상 비용](#개발테스트-환경-예상-비용)
9. [비용 절감 전략](#비용-절감-전략)
10. [트러블슈팅](#트러블슈팅)
11. [참고 자료](#참고-자료)

---

## 개요

### 서비스 소개

AWS ElastiCache for Redis는 완전 관리형 인메모리 데이터 스토어 서비스로, 본 프로젝트에서는 다음과 같은 목적으로 사용됩니다:

1. **OAuth State 저장**: OAuth 2.0 인증 플로우의 CSRF 방지
2. **Kafka 이벤트 멱등성 보장**: 이벤트 중복 처리 방지
3. **Rate Limiting**: API 호출 빈도 제한
4. **챗봇 캐싱**: langchain4j 기반 RAG 챗봇 응답 캐싱

### 아키텍처 개요

- **클러스터 모드**: Disabled (단일 샤드 구성)
- **복제본 구성**: Primary + Replica (고가용성)
- **엔드포인트**: Primary Endpoint (쓰기), Reader Endpoint (읽기)
- **자동 장애 조치**: 활성화 (Multi-AZ)

### 현재 Redis 사용 패턴

| 패턴 | Key 형식 | TTL | 용도 |
|------|---------|-----|------|
| OAuth State | `oauth:state:{state}` | 10분 | CSRF 방지 |
| Kafka 멱등성 | `processed_event:{eventId}` | 7일 | 이벤트 중복 방지 |
| Rate Limiting | `rate-limit:{source}` | 1분 | API 호출 제한 |
| Sources 캐싱 | `{url}:{category}` | 없음 | 데이터 소스 ID 조회 |
| 챗봇 캐싱 | (JSON 직렬화) | 가변 | 응답 캐싱 |

### 참고 설계서

- **Redis 최적화**: `docs/step7/redis-optimization-best-practices.md`
- **OAuth State 저장**: `docs/step6/oauth-state-storage-research-result.md`
- **Redis 설정 코드**: `common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java`

### 공식 문서

이 가이드는 다음 AWS 공식 문서를 기반으로 작성되었습니다:
- [ElastiCache for Redis 개요](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/WhatIs.html)
- [시작하기](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/GettingStarted.html)
- [클러스터 생성](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Clusters.Create.html)
- [AWS CLI 참조](https://docs.aws.amazon.com/cli/latest/reference/elasticache/)

자세한 참고 자료는 문서 하단의 [참고 자료](#참고-자료) 섹션을 참고하세요.

---

## 배포 옵션 선택

AWS ElastiCache for Redis는 두 가지 배포 옵션을 제공합니다. 프로젝트 요구사항에 맞는 적절한 옵션을 선택하는 것이 중요합니다.

### 1. 배포 옵션 개요

**참고**: [AWS 공식 문서 - ElastiCache Serverless](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/WhatIs.Serverless.html)

AWS ElastiCache는 다음 두 가지 배포 옵션을 제공합니다:

1. **ElastiCache Serverless for Redis** (2023년 출시)
   - 자동 스케일링 및 관리형 인프라
   - 사용량 기반 과금 (ECPUs + 스토리지)
   - 노드 관리 불필요

2. **ElastiCache for Redis with Nodes** (전통적 방식)
   - 고정 노드 타입 및 용량
   - 시간당 과금 (인스턴스 기반)
   - 노드 타입 및 수량 직접 관리

### 2. 서버리스 vs 노드 기반 캐시 상세 비교

#### 2.1 기술적 특성 비교

| 특성 | **서버리스** | **노드 기반** |
|------|------------|------------|
| **용량 관리** | 자동 스케일링 (0.5 ECPUs ~ 5,000 ECPUs) | 수동 관리 (노드 타입 선택 필요) |
| **가용성** | 자동 Multi-AZ, 장애 조치 | 수동 설정 (Multi-AZ, Auto-failover) |
| **복제본** | 자동 관리 | 수동 설정 (0-5개) |
| **스냅샷/백업** | 자동 관리 | 수동 설정 |
| **버전 업그레이드** | 자동 | 수동 또는 유지보수 윈도우 |
| **클러스터 모드** | 지원 안 함 (단일 샤드만) | Enabled/Disabled 선택 가능 |
| **Redis 버전** | 7.1 이상 | 6.x, 7.x |
| **데이터 티어링** | 지원 안 함 | r6gd 노드 타입에서 지원 |

#### 2.2 과금 모델 비교

**서버리스 과금**:
- **ECPUs (ElastiCache Processing Units)**: 컴퓨팅 리소스 사용량
  - 예: 서울 리전 $0.125/ECPU-hour
- **스토리지**: 저장된 데이터 용량
  - 예: 서울 리전 $0.125/GB-month
- **데이터 전송**: 표준 AWS 데이터 전송 요금

**노드 기반 과금**:
- **노드 시간**: 선택한 노드 타입의 시간당 요금
  - 예: cache.t3.micro $0.020/hour
  - 예: cache.t3.medium $0.088/hour
- **백업 스토리지**: 백업 크기에 따른 요금
- **데이터 전송**: 표준 AWS 데이터 전송 요금

#### 2.3 성능 특성

**서버리스**:
- **장점**: 자동 스케일링으로 피크 트래픽 대응
- **단점**: Cold Start 가능성 (유휴 상태에서 활성화 시)
- **지연 시간**: 일반적으로 노드 기반과 유사하나, 스케일링 중 약간의 지연 가능

**노드 기반**:
- **장점**: 예측 가능한 성능, 일관된 지연 시간
- **단점**: 고정 용량으로 인한 over-provisioning 필요
- **지연 시간**: 일관되고 낮은 지연 시간 보장

#### 2.4 운영 관리

**서버리스**:
- **관리 복잡도**: 낮음 (AWS가 자동 관리)
- **확장성**: 자동 (애플리케이션 수정 불필요)
- **모니터링**: CloudWatch 메트릭 제공 (ECPUs, Storage)
- **장애 조치**: 자동 (사용자 개입 불필요)

**노드 기반**:
- **관리 복잡도**: 중간 (노드 타입, 수량 결정 필요)
- **확장성**: 수동 (노드 추가/제거, 타입 변경)
- **모니터링**: 상세한 노드별 메트릭 제공
- **장애 조치**: 설정 필요 (Multi-AZ, Auto-failover)

### 3. 현재 프로젝트 워크로드 분석

#### 3.1 프로젝트 Redis 사용 패턴 특성

본 프로젝트의 Redis 사용 패턴을 분석한 결과:

| 사용 케이스 | 데이터 크기 | 액세스 패턴 | TTL | 예측 가능성 |
|------------|-----------|-----------|-----|-----------|
| **OAuth State** | 매우 작음 (~100B/키) | 쓰기 후 즉시 읽기 | 10분 | 높음 (로그인 시) |
| **Kafka 멱등성** | 작음 (~50B/키) | 쓰기 후 간헐적 읽기 | 7일 | 중간 (이벤트 발생 시) |
| **Rate Limiting** | 매우 작음 (~20B/키) | 빈번한 읽기/쓰기 | 1분 | 높음 (API 호출 시) |
| **챗봇 캐싱** | 중간 (~1-10KB/키) | 읽기 위주 | 가변 | 낮음 (사용자 질의 시) |

#### 3.2 예상 워크로드 특성

**데이터 볼륨**:
- **초기 단계**: ~100MB 미만 (OAuth, 멱등성, Rate Limiting)
- **챗봇 활성화**: ~500MB - 1GB (캐싱 데이터 증가)
- **프로덕션 성장**: 1-5GB (사용자 증가)

**트래픽 패턴**:
- **피크 시간**: 업무 시간 (09:00 - 18:00 KST)
- **야간**: 낮은 트래픽 (배치 작업 위주)
- **주말**: 중간 트래픽

**예측 가능성**: **높음**
- OAuth, Rate Limiting은 API 트래픽과 비례
- Kafka 이벤트는 배치 작업과 연동
- 챗봇은 점진적 증가 예상

#### 3.3 비용 예측

**서버리스 비용 시뮬레이션** (예상):
- **평균 ECPUs**: 0.5 - 2 ECPUs (낮은 워크로드)
- **스토리지**: 1GB
- **월 비용 (서울 리전)**:
  - ECPUs: 2 ECPUs × 730시간 × $0.125 = ~$182.50/월
  - 스토리지: 1GB × $0.125 = ~$0.13/월
  - **합계**: 약 **$183/월**

**노드 기반 비용 (cache.t3.medium, 2노드)**:
- **월 비용**: 2 × $0.088 × 730시간 = ~$128.48/월
- **백업**: ~$0.5/월
- **합계**: 약 **$129/월**

### 4. 추천 배포 옵션

#### 4.1 현재 프로젝트 추천: **노드 기반 캐시**

본 프로젝트에는 **노드 기반 ElastiCache for Redis**를 권장합니다.

**추천 이유**:

1. **예측 가능한 워크로드**
   - 현재 Redis 사용 패턴이 명확하고 예측 가능
   - OAuth, Rate Limiting, Kafka 멱등성 모두 예측 가능한 패턴

2. **낮은 데이터 볼륨**
   - 초기 데이터 볼륨이 1GB 미만으로 작음
   - TTL 관리가 잘 되어 있어 데이터 증가 제한적

3. **비용 효율성**
   - 노드 기반이 서버리스보다 약 30% 저렴 (~$129 vs ~$183)
   - 지속적인 사용 패턴에서 노드 기반이 유리

4. **일관된 성능**
   - 낮은 지연 시간 필요 (OAuth, Rate Limiting)
   - Cold Start 없이 일관된 성능 보장

5. **운영 경험 축적**
   - 노드 타입 선택, 모니터링 등 운영 경험 축적 가능
   - 추후 최적화 및 비용 절감 기회

6. **기능 지원**
   - 현재 프로젝트는 단일 샤드로 충분 (서버리스 제약 없음)
   - 데이터 티어링 등 추가 기능 필요 시 활용 가능

**권장 구성**:
- **노드 타입**: `cache.t3.medium` (3.09GB RAM)
- **노드 수**: 2개 (Primary + Replica)
- **Multi-AZ**: 활성화
- **Auto-failover**: 활성화

#### 4.2 서버리스가 적합한 경우

다음과 같은 상황에서는 서버리스를 고려할 수 있습니다:

1. **예측 불가능한 워크로드**
   - 트래픽 패턴이 매우 불규칙
   - 갑작스러운 트래픽 급증 (바이럴 이벤트 등)

2. **간헐적 사용 패턴**
   - 특정 시간대에만 사용 (예: 주말만)
   - 개발/테스트 환경에서 비정기적 사용

3. **운영 부담 최소화**
   - 인프라 관리 리소스 부족
   - 완전 관리형 서비스 선호

4. **빠른 프로토타이핑**
   - POC 또는 MVP 단계
   - 빠른 시작과 실험 필요

#### 4.3 향후 전환 시나리오

**노드 기반 → 서버리스 전환 고려 시점**:

1. **트래픽 패턴 변화**
   - 예측 불가능한 트래픽 증가
   - 피크 시간대 트래픽이 평소의 10배 이상

2. **운영 부담 증가**
   - 노드 관리 및 최적화에 많은 시간 소요
   - 빈번한 스케일링 작업 필요

3. **비용 역전**
   - 서버리스 비용이 노드 기반보다 저렴해지는 시점
   - 평균 ECPU 사용량이 1 미만으로 낮아질 때

**전환 방법**:
- 블루/그린 배포 방식으로 점진적 전환
- 데이터 마이그레이션 도구 활용 (Redis SYNC)
- 애플리케이션 코드 변경 불필요 (엔드포인트만 변경)

### 5. 배포 옵션 결정 가이드

다음 의사결정 트리를 활용하여 배포 옵션을 선택하세요:

```
1. 트래픽 패턴이 예측 가능한가?
   ├─ YES → 2번으로
   └─ NO  → 서버리스 권장

2. 데이터 볼륨이 10GB 미만인가?
   ├─ YES → 3번으로
   └─ NO  → 노드 기반 권장 (r6g 타입)

3. 지속적으로 사용되는가? (24시간 가동)
   ├─ YES → 노드 기반 권장 (t3/r6g)
   └─ NO  → 서버리스 권장

4. 일관된 낮은 지연 시간이 필요한가?
   ├─ YES → 노드 기반 권장
   └─ NO  → 서버리스 고려 가능

5. 운영 리소스가 충분한가?
   ├─ YES → 노드 기반 권장 (비용 최적화 가능)
   └─ NO  → 서버리스 권장
```

**본 프로젝트 결과**: 1(YES) → 2(YES) → 3(YES) → 4(YES) → **노드 기반 권장**

---

## 클러스터 생성 및 구성

### 1. ElastiCache for Redis 클러스터 생성

#### 1.1 AWS Console (GUI)을 통한 생성

**참고**: [AWS 공식 문서 - Redis 클러스터 생성](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Clusters.Create.html)

##### 1.1.1 콘솔 접속 및 초기 설정

1. **AWS Management Console 접속**
   - AWS Console에 로그인 → 상단 검색창에서 "ElastiCache" 검색
   - **Amazon ElastiCache** 서비스 선택
   - 또는 직접 URL: `https://console.aws.amazon.com/elasticache/`

2. **Redis 클러스터 생성 시작**
   - 왼쪽 메뉴에서 **Redis clusters** 클릭
   - 우측 상단 **Create Redis cluster** 버튼 클릭

3. **클러스터 생성 방법 선택**
   - **Design your own cache** 선택 (프로덕션 환경 권장)
   - **Easy create** (개발/테스트 환경용)

##### 1.1.2 클러스터 모드 선택

**참고**: [클러스터 모드 선택 가이드](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Replication.Redis-RedisCluster.html)

1. **Cluster mode**
   - **Disabled**: 단일 샤드 구성 (현재 프로젝트 권장)
     - 간단한 구성
     - 현재 사용 패턴에 적합
     - 최대 5개 읽기 복제본 지원
   - **Enabled**: 멀티 샤드 구성
     - 대규모 데이터셋 및 높은 처리량 필요 시
     - 현재 프로젝트에서는 불필요

2. **프로젝트 권장 사항**: **Cluster mode: Disabled**

##### 1.1.3 클러스터 설정

1. **Cluster info**
   - **Name**: `tech-n-ai-redis-cluster`
   - **Description**: "Tech N AI Redis Cache"

2. **Location**
   - **AWS Cloud**: 선택 (기본값)
   - **Multi-AZ**: 활성화 (고가용성)
     - 자동 장애 조치 지원
     - 프로덕션 환경 필수

##### 1.1.4 클러스터 설정 구성

1. **Engine version**
   - **Engine version**: `7.1` (최신 안정 버전 권장)
   - 7.x 버전은 향상된 성능과 보안 기능 제공

2. **Port**
   - **Port**: `6379` (Redis 기본 포트)

3. **Parameter group**
   - **Parameter group**: `default.redis7` (기본값)
   - 커스텀 파라미터 필요 시 별도 생성 가능

4. **Node type**
   - **초기 워크로드 분석 기반 추천**:
     - **소규모 (개발/테스트)**: `cache.t3.micro` (0.5GB RAM)
     - **중규모 (프로덕션)**: `cache.t3.medium` (3.09GB RAM)
     - **대규모 (프로덕션)**: `cache.r6g.large` (13.07GB RAM)
   - **주의**: 실제 워크로드 모니터링 후 조정 가능

5. **Number of replicas**
   - **Replicas**: `1` (최소 권장)
     - 고가용성 및 읽기 부하 분산
     - 프로덕션 환경 권장: 1-2개

##### 1.1.5 서브넷 그룹 설정

1. **Subnet group settings**
   - **Create a new subnet group**: 새 서브넷 그룹 생성
   - **Choose existing subnet group**: 기존 서브넷 그룹 선택

2. **새 서브넷 그룹 생성 시**
   - **Name**: `tech-n-ai-redis-subnet-group`
   - **Description**: "Subnet group for Redis cluster"
   - **VPC ID**: 기존 VPC 선택
   - **Subnets**: 최소 2개 이상의 서로 다른 가용 영역(AZ) 선택
     - 예: `subnet-xxxxx (ap-northeast-2a)`, `subnet-yyyyy (ap-northeast-2c)`

##### 1.1.6 보안 설정

1. **Security groups**
   - **Manage**: 기존 보안 그룹 선택 또는 새로 생성
   - **보안 그룹 선택**: VPC 내부 애플리케이션 서버에서만 접근 허용
   - **인바운드 규칙 설정** (보안 그룹 생성/수정 시):
     - Type: Custom TCP
     - Port: 6379
     - Source: 애플리케이션 서버 보안 그룹 또는 VPC CIDR

2. **Encryption at-rest**
   - **Encryption**: 활성화 권장 (프로덕션 환경)
   - **Encryption key**: AWS managed key 또는 Customer managed key

3. **Encryption in-transit**
   - **Encryption in-transit**: 활성화 (프로덕션 필수)
   - **참고**: [암호화 전송 설정](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/in-transit-encryption.html)

4. **Redis AUTH**
   - **Access control**: **User group access control** 선택
   - **User group**: 기본 그룹 사용 또는 새로 생성
   - **AUTH token**: 활성화 권장
     - 자동 생성 또는 직접 입력
     - 최소 16자 이상, 영문/숫자/특수문자 포함
   - **참고**: [Redis AUTH 설정](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth.html)

##### 1.1.7 백업 설정

1. **Backup**
   - **Enable automatic backups**: 활성화
   - **Backup retention period**: `7 days` (프로덕션 권장)
   - **Backup window**: 트래픽이 적은 시간대 (예: `03:00-04:00 UTC`)

##### 1.1.8 유지보수 설정

1. **Maintenance**
   - **Maintenance window**: 트래픽이 적은 시간대
     - 예: `mon:04:00-mon:05:00 UTC`
   - **Auto upgrade minor versions**: 활성화 권장

2. **Logs**
   - **Slow log**: 활성화 (성능 모니터링)
   - **Engine log**: 활성화 (디버깅 및 문제 해결)

##### 1.1.9 생성 완료

1. **생성 시작**
   - 하단 **Create** 버튼 클릭
   - 생성 완료까지 약 10-15분 소요

2. **엔드포인트 확인**
   - 생성 완료 후 **Redis clusters** 목록에서 클러스터 선택
   - **Details** 탭에서 다음 엔드포인트 확인:
     - **Primary endpoint**: `tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com:6379`
     - **Reader endpoint**: `tech-n-ai-redis-cluster-ro.xxxxx.ng.0001.apn2.cache.amazonaws.com:6379`

#### 1.2 AWS CLI를 통한 생성

**참고**: [AWS CLI 공식 문서 - create-replication-group](https://docs.aws.amazon.com/cli/latest/reference/elasticache/create-replication-group.html)

##### 1.2.1 사전 준비

1. **AWS CLI 설치 및 구성**
   ```bash
   # AWS CLI 버전 확인
   aws --version
   
   # AWS 자격 증명 구성 (아직 안 했다면)
   aws configure
   ```

2. **서브넷 그룹 생성** (없는 경우)
   ```bash
   # 서브넷 그룹 생성
   aws elasticache create-cache-subnet-group \
     --cache-subnet-group-name tech-n-ai-redis-subnet-group \
     --cache-subnet-group-description "Subnet group for Redis cluster" \
     --subnet-ids subnet-xxxxx subnet-yyyyy \
     --region ap-northeast-2
   ```
   - **주의**: 최소 2개 이상의 서브넷이 서로 다른 가용 영역에 있어야 함

3. **보안 그룹 생성** (없는 경우)
   ```bash
   # 보안 그룹 생성
   SG_ID=$(aws ec2 create-security-group \
     --group-name tech-n-ai-redis-sg \
     --description "Security group for Redis cluster" \
     --vpc-id vpc-xxxxx \
     --query 'GroupId' \
     --output text \
     --region ap-northeast-2)
   
   echo "Security Group ID: $SG_ID"
   
   # 인바운드 규칙 추가 (애플리케이션 서버 보안 그룹에서 접근 허용)
   aws ec2 authorize-security-group-ingress \
     --group-id $SG_ID \
     --protocol tcp \
     --port 6379 \
     --source-group <application-server-sg-id> \
     --region ap-northeast-2
   ```

##### 1.2.2 Redis Replication Group 생성

```bash
# Redis Replication Group 생성 (Cluster Mode Disabled)
aws elasticache create-replication-group \
  --replication-group-id tech-n-ai-redis-cluster \
  --replication-group-description "Tech N AI Redis Cache" \
  --engine redis \
  --engine-version 7.1 \
  --cache-node-type cache.t3.medium \
  --num-cache-clusters 2 \
  --cache-subnet-group-name tech-n-ai-redis-subnet-group \
  --security-group-ids $SG_ID \
  --cache-parameter-group-name default.redis7 \
  --port 6379 \
  --multi-az-enabled \
  --automatic-failover-enabled \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --auth-token "YourSecureAuthToken123!" \
  --snapshot-retention-limit 7 \
  --snapshot-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --log-delivery-configurations \
    "LogType=slow-log,DestinationType=cloudwatch-logs,DestinationDetails={CloudWatchLogsDetails={LogGroup=/aws/elasticache/tech-n-ai-redis}},LogFormat=json" \
  --region ap-northeast-2
```

**주요 파라미터 설명**:
- `--replication-group-id`: Replication Group 고유 식별자
- `--engine`: `redis` (Redis 엔진)
- `--engine-version`: Redis 버전 (7.1 권장)
- `--cache-node-type`: 노드 타입 (인스턴스 크기)
- `--num-cache-clusters`: 클러스터 수 (Primary + Replica)
  - 2: Primary 1개 + Replica 1개
  - 3: Primary 1개 + Replica 2개
- `--cache-subnet-group-name`: 서브넷 그룹 이름
- `--security-group-ids`: 보안 그룹 ID
- `--multi-az-enabled`: Multi-AZ 활성화
- `--automatic-failover-enabled`: 자동 장애 조치 활성화
- `--at-rest-encryption-enabled`: 저장 데이터 암호화
- `--transit-encryption-enabled`: 전송 중 암호화
- `--auth-token`: Redis AUTH 토큰 (16자 이상)
- `--snapshot-retention-limit`: 백업 보존 기간 (일)

##### 1.2.3 생성 상태 확인

```bash
# Replication Group 상태 확인
aws elasticache describe-replication-groups \
  --replication-group-id tech-n-ai-redis-cluster \
  --query 'ReplicationGroups[0].[Status,NodeGroups[0].PrimaryEndpoint.Address,NodeGroups[0].ReaderEndpoint.Address]' \
  --output table \
  --region ap-northeast-2
```

**상태 확인**:
- `Status`: `available` (생성 완료)
- `PrimaryEndpoint.Address`: Primary 엔드포인트 주소
- `ReaderEndpoint.Address`: Reader 엔드포인트 주소

---

## 네트워크 및 보안 구성

### 1. VPC 및 서브넷 그룹 설정

**참고**: [AWS 공식 문서 - VPC 설정](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/VPCs.html)

#### 1.1 AWS Console을 통한 서브넷 그룹 생성

1. **ElastiCache Console 접속**
   - AWS Console → ElastiCache → 왼쪽 메뉴 **Subnet groups** 클릭
   - **Create subnet group** 버튼 클릭

2. **서브넷 그룹 설정**
   - **Name**: `tech-n-ai-redis-subnet-group`
   - **Description**: "Subnet group for Redis cluster"
   - **VPC ID**: 기존 VPC 선택 또는 새 VPC 생성
   - **Availability Zones**: 최소 2개 이상 선택
     - 예: `ap-northeast-2a`, `ap-northeast-2c`
   - **Subnets**: 각 가용 영역에서 서브넷 선택
     - 각 서브넷은 충분한 IP 주소 범위 확보 (최소 /24 권장)
   - **Create** 버튼 클릭

#### 1.2 AWS CLI를 통한 서브넷 그룹 생성

```bash
# 서브넷 그룹 생성
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name tech-n-ai-redis-subnet-group \
  --cache-subnet-group-description "Subnet group for Redis cluster" \
  --subnet-ids subnet-xxxxx subnet-yyyyy \
  --region ap-northeast-2

# 서브넷 그룹 확인
aws elasticache describe-cache-subnet-groups \
  --cache-subnet-group-name tech-n-ai-redis-subnet-group \
  --region ap-northeast-2
```

### 2. 보안 그룹 설정

**참고**: [AWS 공식 문서 - 보안 그룹](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/VPCs.SecurityGroups.html)

#### 2.1 AWS Console을 통한 보안 그룹 생성

1. **EC2 Console 접속**
   - AWS Console → EC2 → 왼쪽 메뉴 **Security Groups** 클릭
   - **Create security group** 버튼 클릭

2. **보안 그룹 설정**
   - **Security group name**: `tech-n-ai-redis-sg`
   - **Description**: "Security group for Redis cluster"
   - **VPC**: Redis 클러스터와 동일한 VPC 선택

3. **인바운드 규칙 추가**
   - **Type**: Custom TCP
   - **Port range**: `6379`
   - **Source**: 다음 중 하나 선택
     - **Custom**: 애플리케이션 서버 보안 그룹 ID 입력
     - **My IP**: 개발 환경에서만 사용
     - **Custom**: VPC CIDR (예: `10.0.0.0/16`)

4. **아웃바운드 규칙**: 기본 설정 유지 (모든 트래픽 허용)

5. **Create security group** 버튼 클릭

#### 2.2 AWS CLI를 통한 보안 그룹 생성

```bash
# 보안 그룹 생성
SG_ID=$(aws ec2 create-security-group \
  --group-name tech-n-ai-redis-sg \
  --description "Security group for Redis cluster" \
  --vpc-id vpc-xxxxx \
  --query 'GroupId' \
  --output text \
  --region ap-northeast-2)

echo "Security Group ID: $SG_ID"

# 인바운드 규칙 추가 (애플리케이션 서버 보안 그룹에서 접근 허용)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 6379 \
  --source-group <application-server-sg-id> \
  --region ap-northeast-2

# 또는 VPC CIDR에서 접근 허용 (개발 환경용)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 6379 \
  --cidr 10.0.0.0/16 \
  --region ap-northeast-2
```

### 3. 암호화 설정

#### 3.1 전송 중 암호화 (TLS/SSL)

**참고**: [AWS 공식 문서 - 전송 중 암호화](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/in-transit-encryption.html)

**프로덕션 환경 필수 설정**:
- 클러스터 생성 시 `--transit-encryption-enabled` 플래그 사용
- 클라이언트 연결 시 TLS/SSL 활성화 필요
- Spring Data Redis는 자동으로 TLS/SSL 지원

**Spring Boot 설정** (`application-common-core.yml`):
```yaml
spring:
  data:
    redis:
      ssl:
        enabled: true  # 프로덕션 환경
```

#### 3.2 저장 데이터 암호화

**참고**: [AWS 공식 문서 - 저장 데이터 암호화](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/at-rest-encryption.html)

- 클러스터 생성 시 `--at-rest-encryption-enabled` 플래그 사용
- AWS KMS로 암호화 키 관리
- 추가 성능 오버헤드 없음

### 4. Redis AUTH 인증

**참고**: [AWS 공식 문서 - Redis AUTH](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth.html)

#### 4.1 AUTH 토큰 설정

1. **클러스터 생성 시 AUTH 토큰 설정**
   - `--auth-token` 파라미터로 토큰 지정
   - 최소 16자 이상, 최대 128자
   - 영문, 숫자, 특수문자 포함 권장

2. **AUTH 토큰 보안 관리**
   - AWS Secrets Manager에 저장 권장
   - 환경변수로 관리 (코드에 하드코딩 금지)

#### 4.2 Spring Boot 연동

```yaml
# application-common-core.yml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD:}  # AUTH 토큰
```

---

## 로컬 개발 환경 Redis 설치 및 설정

로컬 개발 환경에서 프로젝트를 실행하기 위해서는 Redis를 로컬에 설치하고 설정해야 합니다. 이 섹션에서는 각 운영체제별 Redis 설치 방법과 프로젝트 연동 방법을 안내합니다.

**참고**: [Redis 공식 설치 가이드](https://redis.io/docs/install/install-redis/)

### 1. Redis 설치 방법

#### 1.1 macOS에서 Redis 설치

**참고**: [Redis on macOS](https://redis.io/docs/install/install-redis/install-redis-on-mac-os/)

##### Homebrew를 통한 설치 (권장)

```bash
# Homebrew 업데이트
brew update

# Redis 설치
brew install redis

# 설치 확인
redis-server --version
```

예상 출력:
```
Redis server v=7.2.4 sha=00000000:0 malloc=libc bits=64 build=...
```

##### Redis 시작 및 중지

```bash
# Redis 서버 시작 (포그라운드)
redis-server

# Redis 서버 시작 (백그라운드 - 권장)
brew services start redis

# Redis 서버 중지
brew services stop redis

# Redis 서버 재시작
brew services restart redis

# Redis 서버 상태 확인
brew services info redis
```

##### 자동 시작 설정

```bash
# 시스템 부팅 시 자동 시작
brew services start redis

# 자동 시작 해제
brew services stop redis
```

#### 1.2 Windows에서 Redis 설치

**참고**: [Redis on Windows](https://redis.io/docs/install/install-redis/install-redis-on-windows/)

**중요**: Redis는 공식적으로 Windows를 지원하지 않습니다. Windows 환경에서는 **Docker 사용을 권장**합니다.

##### WSL2 (Windows Subsystem for Linux 2)를 통한 설치

1. **WSL2 활성화**
   ```powershell
   # PowerShell 관리자 권한으로 실행
   wsl --install
   ```

2. **Ubuntu 설치 후 Redis 설치**
   ```bash
   # WSL Ubuntu 터미널에서
   sudo apt update
   sudo apt install redis-server -y
   
   # 설치 확인
   redis-server --version
   ```

3. **Redis 시작**
   ```bash
   # Redis 서버 시작
   sudo service redis-server start
   
   # Redis 서버 상태 확인
   sudo service redis-server status
   
   # Redis 서버 중지
   sudo service redis-server stop
   ```

##### Memurai (상용 솔루션 - 선택 사항)

Windows 네이티브 지원이 필요한 경우 Memurai를 고려할 수 있으나, 이 문서에서는 다루지 않습니다.

#### 1.3 Linux에서 Redis 설치

**참고**: [Redis on Linux](https://redis.io/docs/install/install-redis/install-redis-on-linux/)

##### Ubuntu / Debian

```bash
# 패키지 목록 업데이트
sudo apt update

# Redis 설치
sudo apt install redis-server -y

# 설치 확인
redis-server --version
```

##### CentOS / RHEL / Rocky Linux

```bash
# EPEL 저장소 활성화
sudo yum install epel-release -y

# Redis 설치
sudo yum install redis -y

# 설치 확인
redis-server --version
```

##### Redis 서비스 관리 (systemd)

```bash
# Redis 서버 시작
sudo systemctl start redis

# Redis 서버 중지
sudo systemctl stop redis

# Redis 서버 재시작
sudo systemctl restart redis

# Redis 서버 상태 확인
sudo systemctl status redis

# 부팅 시 자동 시작 설정
sudo systemctl enable redis

# 자동 시작 해제
sudo systemctl disable redis
```

#### 1.4 Docker로 Redis 실행 (모든 OS 지원)

**참고**: [Redis Docker Official Image](https://hub.docker.com/_/redis)

##### 기본 실행

```bash
# Redis 7.x 컨테이너 실행 (기본 포트 6379)
docker run -d \
  --name local-redis \
  -p 6379:6379 \
  redis:7-alpine

# 컨테이너 상태 확인
docker ps | grep local-redis

# 로그 확인
docker logs local-redis
```

##### 데이터 영속성을 위한 볼륨 마운트

```bash
# 로컬 디렉토리 생성
mkdir -p ~/redis-data

# 볼륨 마운트하여 실행
docker run -d \
  --name local-redis \
  -p 6379:6379 \
  -v ~/redis-data:/data \
  redis:7-alpine redis-server --appendonly yes

# 설명:
# --appendonly yes: AOF (Append Only File) 영속성 활성화
# -v ~/redis-data:/data: 로컬 디렉토리에 데이터 저장
```

##### 비밀번호 설정 (권장)

```bash
# 비밀번호와 함께 실행
docker run -d \
  --name local-redis \
  -p 6379:6379 \
  redis:7-alpine redis-server --requirepass mypassword

# 연결 테스트
docker exec -it local-redis redis-cli -a mypassword ping
```

##### Docker Compose 사용 (권장)

**docker-compose.yml 파일 생성** (프로젝트 루트):

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: local-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

volumes:
  redis-data:
```

**Docker Compose 명령어**:

```bash
# Redis 시작
docker-compose up -d

# Redis 중지
docker-compose down

# 로그 확인
docker-compose logs -f redis

# 상태 확인
docker-compose ps
```

### 2. Redis 기본 설정 확인 및 변경

#### 2.1 redis.conf 파일 위치

| OS | 기본 위치 |
|----|---------|
| **macOS (Homebrew)** | `/opt/homebrew/etc/redis.conf` (Apple Silicon)<br>`/usr/local/etc/redis.conf` (Intel) |
| **Ubuntu / Debian** | `/etc/redis/redis.conf` |
| **CentOS / RHEL** | `/etc/redis.conf` |
| **Docker** | 컨테이너 내부 `/etc/redis/redis.conf` |

#### 2.2 개발 환경에 적합한 주요 설정

**참고**: [Redis 설정 문서](https://redis.io/docs/management/config/)

##### 필수 설정 확인

```bash
# redis.conf 파일 열기 (macOS Homebrew 예시)
vi /opt/homebrew/etc/redis.conf

# 또는 (Ubuntu/Debian 예시)
sudo vi /etc/redis/redis.conf
```

##### 주요 설정 항목

**1. 포트 설정**
```conf
# 기본 포트 (변경 불필요)
port 6379
```

**2. 바인드 주소**
```conf
# 로컬 개발 환경: localhost만 허용 (보안)
bind 127.0.0.1

# 또는 모든 인터페이스 허용 (주의: 개발 환경에서만)
# bind 0.0.0.0
```

**3. 비밀번호 설정 (권장)**
```conf
# 비밀번호 설정
requirepass your_secure_password_here

# 예시:
requirepass dev_redis_password_123
```

**4. 메모리 제한**
```conf
# 최대 메모리 설정 (개발 환경: 256MB)
maxmemory 256mb

# 메모리 제거 정책 (TTL이 있는 키 중 LRU)
maxmemory-policy volatile-lru
```

**5. 데이터 영속성 설정**

**RDB (스냅샷)**:
```conf
# 스냅샷 저장 간격
save 900 1      # 900초 동안 1개 이상 키 변경 시 저장
save 300 10     # 300초 동안 10개 이상 키 변경 시 저장
save 60 10000   # 60초 동안 10000개 이상 키 변경 시 저장

# 스냅샷 파일 이름
dbfilename dump.rdb

# 스냅샷 저장 디렉토리
dir /var/lib/redis  # Linux
# 또는
dir /opt/homebrew/var/db/redis  # macOS Homebrew
```

**AOF (Append Only File)** - 권장:
```conf
# AOF 활성화 (더 안전한 영속성)
appendonly yes

# AOF 파일 이름
appendfilename "appendonly.aof"

# AOF 동기화 정책 (매 초 - 성능과 안전성 균형)
appendfsync everysec
```

#### 2.3 설정 변경 후 재시작

```bash
# macOS (Homebrew)
brew services restart redis

# Linux (systemd)
sudo systemctl restart redis

# Docker
docker restart local-redis
```

### 3. 프로젝트와 로컬 Redis 연동

#### 3.1 환경변수 설정

프로젝트의 `application-common-core.yml`은 다음 환경변수를 사용합니다:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
```

##### .env 파일 생성 (프로젝트 루트)

```bash
# .env 파일 생성
cat > .env << 'EOF'
# Redis 연결 정보 (로컬 개발 환경)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
REDIS_SSL_ENABLED=false

# 시간대 설정
TZ=Asia/Seoul
EOF
```

**비밀번호 설정한 경우**:
```bash
REDIS_PASSWORD=dev_redis_password_123
```

**.gitignore에 추가** (중요):
```bash
# .gitignore에 추가
echo ".env" >> .gitignore
```

##### IntelliJ IDEA / IDE 환경변수 설정

1. **Run/Debug Configurations** 열기
2. **Environment variables** 섹션에서 추가:
   ```
   REDIS_HOST=localhost;REDIS_PORT=6379;REDIS_PASSWORD=;REDIS_SSL_ENABLED=false
   ```

#### 3.2 application-common-core.yml 확인

프로젝트의 공통 설정 파일은 이미 로컬 Redis 연동을 위한 설정을 포함하고 있습니다:

```yaml:common/core/src/main/resources/application-common-core.yml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}  # 기본값: localhost
      port: ${REDIS_PORT:6379}       # 기본값: 6379
      password: ${REDIS_PASSWORD:}   # 기본값: 없음
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}  # 로컬: false
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
        shutdown-timeout: 100ms
```

**변경 불필요**: 환경변수만 설정하면 자동으로 로컬 Redis에 연결됩니다.

#### 3.3 RedisConfig 동작 확인

프로젝트의 `RedisConfig` 클래스는 두 개의 RedisTemplate 빈을 제공합니다:

**1. redisTemplate (String 직렬화)**:
- OAuth State 저장 (`oauth:state:{state}`)
- Kafka 이벤트 멱등성 (`processed_event:{eventId}`)
- Rate Limiting (`rate-limit:{source}`)

**2. redisTemplateForObjects (JSON 직렬화)**:
- 챗봇 캐싱 (복잡한 객체 저장)

**참고**: `common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java`

**변경 불필요**: 기존 설정으로 로컬 Redis와 연동 가능합니다.

### 4. 연결 테스트

#### 4.1 redis-cli를 통한 연결 테스트

##### 비밀번호 없는 경우

```bash
# redis-cli 실행
redis-cli

# 연결 테스트
127.0.0.1:6379> PING
PONG

# 기본 명령어 테스트
127.0.0.1:6379> SET test:key "Hello Redis"
OK

127.0.0.1:6379> GET test:key
"Hello Redis"

127.0.0.1:6379> TTL test:key
(integer) -1

# TTL 설정하여 저장 (10초)
127.0.0.1:6379> SETEX test:ttl 10 "Expires in 10 seconds"
OK

127.0.0.1:6379> TTL test:ttl
(integer) 8

# 키 삭제
127.0.0.1:6379> DEL test:key test:ttl
(integer) 1

# 종료
127.0.0.1:6379> QUIT
```

##### 비밀번호 설정한 경우

```bash
# 비밀번호와 함께 연결
redis-cli -a dev_redis_password_123

# 또는 연결 후 인증
redis-cli
127.0.0.1:6379> AUTH dev_redis_password_123
OK

127.0.0.1:6379> PING
PONG
```

#### 4.2 Spring Boot 애플리케이션 연결 테스트

##### 애플리케이션 시작

```bash
# 프로젝트 루트에서
./gradlew :api:auth:bootRun

# 또는 IntelliJ IDEA에서 AuthApplication 실행
```

##### 시작 로그 확인

정상 연결 시 다음과 유사한 로그가 출력됩니다:

```
INFO  o.s.d.r.c.LettuceConnectionFactory - Opening LettuceConnection
INFO  i.l.c.c.ConnectionFactory - Connecting to Redis server: localhost:6379
INFO  o.s.d.r.c.LettuceConnectionFactory - Created new connection for localhost:6379
```

##### Redis 연결 상태 확인

**redis-cli로 확인**:

```bash
redis-cli

# 현재 연결된 클라이언트 확인
127.0.0.1:6379> CLIENT LIST

# 예상 출력:
# id=3 addr=127.0.0.1:50123 name= age=5 idle=0 ...
```

#### 4.3 프로젝트 사용 패턴별 테스트

##### OAuth State 저장 테스트 (수동)

```bash
redis-cli

# OAuth State 키 저장 (10분 TTL)
127.0.0.1:6379> SETEX oauth:state:test-state-12345 600 "google"
OK

# 저장된 값 확인
127.0.0.1:6379> GET oauth:state:test-state-12345
"google"

# TTL 확인 (초 단위)
127.0.0.1:6379> TTL oauth:state:test-state-12345
(integer) 595

# 키 삭제
127.0.0.1:6379> DEL oauth:state:test-state-12345
(integer) 1
```

##### Kafka 이벤트 멱등성 테스트 (수동)

```bash
# 이벤트 처리 완료 표시 (7일 TTL)
127.0.0.1:6379> SETEX processed_event:event-123 604800 "processed"
OK

# 확인
127.0.0.1:6379> GET processed_event:event-123
"processed"

# TTL 확인 (7일 = 604800초)
127.0.0.1:6379> TTL processed_event:event-123
(integer) 604795
```

##### Rate Limiting 테스트 (수동)

```bash
# Rate Limit 키 저장 (1분 TTL)
127.0.0.1:6379> SETEX rate-limit:api 60 "1"
OK

# 확인
127.0.0.1:6379> GET rate-limit:api
"1"
```

#### 4.4 Spring Boot Actuator를 통한 헬스 체크

```bash
# Redis 헬스 체크
curl http://localhost:8080/actuator/health/redis
```

예상 응답:
```json
{
  "status": "UP",
  "details": {
    "version": "7.2.4"
  }
}
```

### 5. 로컬 Redis 사용 시 주의사항

#### 5.1 보안 설정

##### 비밀번호 설정 권장

**개발 환경에서도 비밀번호 설정을 권장합니다**:

```conf
# redis.conf
requirepass dev_redis_password_123
```

```bash
# .env 파일
REDIS_PASSWORD=dev_redis_password_123
```

##### 네트워크 바인드 주소

```conf
# localhost만 허용 (기본값 - 권장)
bind 127.0.0.1

# 외부 접근이 필요한 경우 (주의: 개발 환경에서만)
# bind 0.0.0.0
# requirepass your_password  # 반드시 비밀번호 설정
```

#### 5.2 메모리 관리

##### 최대 메모리 설정

```conf
# 개발 환경: 256MB로 제한
maxmemory 256mb

# 메모리 제거 정책
maxmemory-policy volatile-lru  # TTL이 있는 키 중 LRU 제거
```

##### 메모리 사용량 모니터링

```bash
redis-cli

# 메모리 사용량 확인
127.0.0.1:6379> INFO memory

# 주요 항목:
# used_memory_human: 실제 메모리 사용량
# used_memory_peak_human: 피크 메모리 사용량
# maxmemory_human: 최대 메모리 제한
```

#### 5.3 데이터 영속성 설정

##### 개발 환경 권장 설정

**AOF (Append Only File) 활성화 권장**:

```conf
# 더 안전한 영속성
appendonly yes

# 동기화 정책 (매 초)
appendfsync everysec
```

##### 데이터 백업 및 복구

```bash
# 스냅샷 파일 위치 확인
redis-cli CONFIG GET dir

# 수동 스냅샷 생성
redis-cli SAVE

# 또는 백그라운드 저장
redis-cli BGSAVE
```

#### 5.4 개발 중 주의사항

##### TTL 설정 확인

프로젝트의 Redis 사용 패턴은 모두 TTL을 설정합니다:

| 패턴 | TTL | 목적 |
|------|-----|------|
| OAuth State | 10분 | CSRF 방지 |
| Kafka 멱등성 | 7일 | 이벤트 중복 방지 |
| Rate Limiting | 1분 | API 호출 제한 |
| 챗봇 캐싱 | 가변 | 응답 캐싱 |

**중요**: TTL이 설정되지 않은 키는 메모리 누수를 일으킬 수 있으므로 주의하세요.

##### 키 네이밍 규칙 준수

프로젝트의 키 네이밍 규칙을 따르세요:
- OAuth State: `oauth:state:{state}`
- Kafka 이벤트: `processed_event:{eventId}`
- Rate Limiting: `rate-limit:{source}`
- Sources 캐싱: `{url}:{category}` (예: `https://codeforces.com:contest`)

##### 개발 중 데이터 초기화

```bash
# 모든 데이터 삭제 (주의: 개발 환경에서만)
redis-cli FLUSHALL

# 특정 패턴의 키만 삭제
redis-cli --scan --pattern "oauth:state:*" | xargs redis-cli DEL
```

#### 5.5 Docker 사용 시 주의사항

##### 컨테이너 상태 확인

```bash
# 컨테이너 상태 확인
docker ps -a | grep redis

# 로그 확인
docker logs local-redis

# 실시간 로그 확인
docker logs -f local-redis
```

##### 데이터 영속성 확인

```bash
# 볼륨 마운트 확인
docker inspect local-redis | grep -A 10 Mounts

# 데이터 디렉토리 확인
ls -lh ~/redis-data
```

##### 컨테이너 재시작 시 데이터 유지

```bash
# 볼륨을 사용하여 실행했다면 재시작 후에도 데이터 유지
docker restart local-redis

# 확인
redis-cli
127.0.0.1:6379> KEYS *
```

### 6. Redis GUI 관리 도구 (선택 사항)

Redis CLI만으로도 충분하지만, 시각적으로 데이터를 관리하고 모니터링하고 싶다면 다음 무료 GUI 도구를 사용할 수 있습니다.

**참고**: [Redis GUI Tools](https://redis.io/docs/connect/clients/#gui)

#### 6.1 RedisInsight (공식 권장) ⭐

**참고**: [RedisInsight 공식 사이트](https://redis.io/insight/)

Redis에서 공식적으로 제공하는 무료 GUI 도구입니다.

##### 주요 기능
- 키 브라우저 및 데이터 시각화
- Redis 명령어 실행 (CLI 내장)
- 메모리 분석 및 최적화 제안
- Pub/Sub 모니터링
- 슬로우 로그 분석
- 클러스터 및 센티널 지원
- 다크 모드 지원

##### 설치 방법 (macOS)

**Homebrew로 설치**:
```bash
# RedisInsight 설치
brew install --cask redisinsight

# 설치 확인
open -a RedisInsight
```

**직접 다운로드**:
1. [RedisInsight 다운로드 페이지](https://redis.io/insight/) 방문
2. macOS 버전 다운로드 (.dmg 파일)
3. 다운로드한 파일을 실행하여 Applications 폴더로 드래그

##### 연결 설정

1. **RedisInsight 실행**
2. **Add Redis Database** 클릭
3. **연결 정보 입력**:
   ```
   Host: localhost
   Port: 6379
   Name: Local Redis (임의 지정)
   Username: (비워둠 - Redis 6.0 이전)
   Password: (설정한 경우 입력)
   ```
4. **Test Connection** → **Add Redis Database** 클릭

##### 프로젝트 데이터 확인

**OAuth State 조회**:
1. **Browser** 탭 클릭
2. 검색창에 `oauth:state:*` 입력
3. 키 목록 및 값 확인
4. TTL (만료 시간) 확인 가능

**Kafka 이벤트 멱등성 확인**:
1. `processed_event:*` 패턴으로 검색
2. 처리된 이벤트 ID 목록 확인

**Rate Limiting 모니터링**:
1. `rate-limit:*` 패턴으로 검색
2. 현재 Rate Limit 상태 확인

##### CLI 사용

RedisInsight 내장 CLI 사용:
1. **CLI** 탭 클릭
2. Redis 명령어 실행:
   ```
   KEYS oauth:state:*
   GET oauth:state:test-123
   TTL oauth:state:test-123
   ```

#### 6.2 Redis Commander (오픈소스)

**참고**: [Redis Commander GitHub](https://github.com/joeferner/redis-commander)

Node.js 기반의 웹 기반 Redis 관리 도구입니다.

##### 주요 기능
- 웹 브라우저 기반 (설치 불필요)
- 키 브라우저 및 편집
- CLI 인터페이스
- 여러 Redis 인스턴스 관리
- JSON 및 MessagePack 지원

##### 설치 및 실행 (Node.js 필요)

**npm으로 전역 설치**:
```bash
# Redis Commander 설치
npm install -g redis-commander

# Redis Commander 실행
redis-commander

# 또는 특정 포트로 실행
redis-commander --port 8082
```

**Docker로 실행**:
```bash
# Redis Commander 컨테이너 실행
docker run -d \
  --name redis-commander \
  -p 8081:8081 \
  --env REDIS_HOSTS=local:host.docker.internal:6379 \
  ghcr.io/joeferner/redis-commander:latest

# macOS Docker Desktop 사용 시 host.docker.internal로 로컬 Redis 접근
```

##### 웹 인터페이스 접속

```bash
# 브라우저에서 접속
open http://localhost:8081
```

#### 6.3 Medis (macOS 전용)

**참고**: [Medis GitHub](https://github.com/luin/medis)

macOS 네이티브 앱으로 제공되는 Redis 클라이언트입니다.

##### 주요 기능
- macOS 네이티브 UI
- 직관적인 키 브라우저
- 여러 Redis 연결 관리
- SSH 터널링 지원 (원격 Redis)

##### 설치 방법

**Homebrew로 설치**:
```bash
# Medis 설치
brew install --cask medis
```

**Mac App Store에서 다운로드** (유료 버전):
- 무료 오픈소스 버전과 유료 App Store 버전이 있음
- GitHub에서 무료 버전 다운로드 가능

##### 연결 설정

1. **Medis 실행**
2. **+** 버튼 클릭하여 새 연결 추가
3. **연결 정보 입력**:
   ```
   Name: Local Redis
   Host: 127.0.0.1
   Port: 6379
   Auth: (비밀번호 설정한 경우)
   ```
4. **Connect** 클릭

#### 6.4 도구 비교 및 권장 사항

| 도구 | 플랫폼 | 타입 | 공식 지원 | 권장 용도 |
|------|--------|------|----------|-----------|
| **RedisInsight** | macOS, Windows, Linux | 데스크톱 | ✅ Redis 공식 | 전체 기능, 프로덕션급 분석 |
| **Redis Commander** | 웹 브라우저 | 웹 기반 | ❌ 커뮤니티 | 간단한 관리, 멀티 인스턴스 |
| **Medis** | macOS | 네이티브 앱 | ❌ 커뮤니티 | macOS 전용, 간단한 관리 |

##### 프로젝트 개발 환경 권장 사항

**RedisInsight 권장** (1순위):
- Redis 공식 도구로 가장 신뢰할 수 있음
- 메모리 분석 및 성능 최적화 제안 기능
- 슬로우 로그 분석으로 성능 문제 파악 가능
- 무료이면서 기능이 풍부함

**사용 시나리오별 추천**:
- **개발 중 데이터 확인**: RedisInsight Browser 탭
- **Redis 명령어 테스트**: RedisInsight CLI 탭
- **메모리 사용량 분석**: RedisInsight Analysis 탭
- **여러 Redis 인스턴스 관리**: Redis Commander

#### 6.5 GUI 도구 사용 시 주의사항

##### 프로덕션 환경 접근

**주의**: GUI 도구로 프로덕션 Redis에 직접 연결하지 마세요.

- ❌ **프로덕션 환경**: AWS ElastiCache는 VPC 내부 전용이므로 GUI 도구로 직접 연결 불가
- ✅ **개발/로컬 환경**: 로컬 Redis에만 GUI 도구 사용 권장

**프로덕션 Redis 확인이 필요한 경우**:
1. SSH 터널링 (Bastion Host)을 통한 연결
2. RedisInsight에서 SSH 터널 설정 지원
3. 또는 AWS Console의 CloudWatch 메트릭 사용

##### 대용량 키 조회 주의

```bash
# ❌ 위험: KEYS * 명령어 (프로덕션에서 절대 금지)
KEYS *

# ✅ 안전: SCAN 명령어 사용 (점진적 조회)
SCAN 0 MATCH oauth:state:* COUNT 100
```

**GUI 도구에서**:
- RedisInsight: 자동으로 SCAN 사용 (안전)
- Redis Commander: KEYS 사용 가능하므로 주의
- 많은 키가 있을 경우 패턴 검색 사용

##### 데이터 삭제 주의

GUI 도구에서 키 삭제 시:
- 삭제 전 반드시 키 이름 확인
- 프로젝트 키 네이밍 규칙 준수:
  - `oauth:state:*`: OAuth 인증 중 필요
  - `processed_event:*`: Kafka 이벤트 중복 방지
  - `rate-limit:*`: API Rate Limiting
- 실수로 삭제한 경우 TTL이 있으므로 자동 복구되지만, OAuth는 재인증 필요

##### 비밀번호 보안

RedisInsight 등 GUI 도구에 비밀번호 저장 시:
- 개발 환경에서만 비밀번호 저장 기능 사용
- 프로덕션 환경 비밀번호는 저장하지 않음
- macOS Keychain 등 안전한 저장소 사용 권장

---

## 프로젝트 연동

### 1. 환경변수 설정

#### 1.1 필수 환경변수

다음 환경변수를 설정합니다:

```bash
# ElastiCache Redis 연결 정보
REDIS_HOST=tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=YourSecureAuthToken123!  # AUTH 토큰
REDIS_SSL_ENABLED=true  # 프로덕션 환경

# 기타 설정
TZ=Asia/Seoul
```

#### 1.2 로컬 환경 설정 (`.env` 파일)

로컬 개발 환경에서는 `.env` 파일을 사용합니다:

```bash
# .env 파일 (프로젝트 루트)
REDIS_HOST=tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com
REDIS_PORT=6379
REDIS_PASSWORD=YourSecureAuthToken123!
REDIS_SSL_ENABLED=true
TZ=Asia/Seoul
```

**주의사항**: `.env` 파일은 `.gitignore`에 포함되어야 합니다.

#### 1.3 프로덕션 환경 설정

프로덕션 환경에서는 다음 방법 중 하나를 사용합니다:

- **AWS Secrets Manager**: AUTH 토큰 등 민감 정보 관리
- **AWS Systems Manager Parameter Store**: 환경변수 관리
- **환경변수 직접 설정**: 컨테이너/서버 환경변수로 설정

### 2. application-common-core.yml 확인

현재 설정은 ElastiCache for Redis와 호환됩니다:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}  # AUTH 토큰
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}  # 프로덕션: true
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
        shutdown-timeout: 100ms
```

**추가 설정 불필요**: 기존 설정으로 ElastiCache for Redis 연동 가능

### 3. RedisConfig 확인

현재 `RedisConfig` 클래스는 ElastiCache for Redis와 완벽 호환:

```java:common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        // ... 기존 설정 유지
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplateForObjects(RedisConnectionFactory connectionFactory) {
        // ... 기존 설정 유지
    }
}
```

**변경 불필요**: 기존 코드로 ElastiCache for Redis 사용 가능

### 4. 로컬 환경에서 접속하기

#### 4.1 중요: ElastiCache 접근 제한 사항

**AWS ElastiCache for Redis는 VPC 내부 전용 서비스입니다**:

- ❌ **공개 IP 주소 없음**: 인터넷에서 직접 접근 불가
- ❌ **보안그룹에 로컬 IP 추가만으로는 접속 불가**: VPC 외부에서 직접 연결 불가능
- ✅ **VPC 내부 리소스만 접근 가능**: EC2, ECS, Lambda 등

**로컬 개발 환경에서 접속하려면 다음 방법 중 하나를 사용해야 합니다**:

#### 4.2 방법 1: SSH 터널링 (Bastion Host) - **권장**

가장 일반적이고 안전한 방법입니다.

##### 4.2.1 사전 준비

1. **Bastion Host (EC2) 생성**
   - VPC의 Public Subnet에 EC2 인스턴스 생성
   - ElastiCache와 동일한 VPC 내에 위치
   - Bastion Host 보안 그룹: SSH 포트 22 허용 (로컬 IP에서)
   - ElastiCache 보안 그룹: 포트 6379 허용 (Bastion Host 보안 그룹에서)

2. **보안 그룹 설정**

**Bastion Host 보안 그룹** (`bastion-sg`):
```
인바운드 규칙:
- Type: SSH
- Port: 22
- Source: <Your-Local-IP>/32  # 로컬 IP 추가
```

**ElastiCache 보안 그룹** (`tech-n-ai-redis-sg`):
```
인바운드 규칙:
- Type: Custom TCP
- Port: 6379
- Source: bastion-sg  # Bastion Host 보안 그룹 ID
```

##### 4.2.2 SSH 터널 생성

**로컬 터미널에서 실행**:

```bash
# SSH 터널 생성 (포트 포워딩)
ssh -i ~/.ssh/your-key.pem \
  -N -L 6379:tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com:6379 \
  ec2-user@<bastion-public-ip>
```

**파라미터 설명**:
- `-N`: 원격 명령 실행 없이 포트 포워딩만 수행
- `-L 6379:redis-endpoint:6379`: 로컬 6379 포트를 Redis 엔드포인트로 포워딩
- `ec2-user@<bastion-public-ip>`: Bastion Host 접속 정보

**터널이 활성화된 상태에서 로컬 애플리케이션 실행**:

```bash
# .env 파일 (SSH 터널 사용 시)
REDIS_HOST=localhost  # 로컬호스트로 변경
REDIS_PORT=6379
REDIS_PASSWORD=YourSecureAuthToken123!
REDIS_SSL_ENABLED=false  # SSH 터널 내부는 암호화되므로 false
TZ=Asia/Seoul
```

##### 4.2.3 연결 테스트 (SSH 터널 사용)

```bash
# redis-cli로 연결 테스트 (로컬호스트)
redis-cli -h localhost -p 6379 -a YourSecureAuthToken123!

> PING
PONG

> SET test:key "Hello from Local"
OK

> GET test:key
"Hello from Local"
```

#### 4.3 방법 2: AWS Client VPN

VPN을 통해 VPC에 직접 연결합니다.

##### 4.3.1 AWS Client VPN 설정

1. **AWS Client VPN Endpoint 생성**
   - AWS Console → VPC → Client VPN Endpoints → Create
   - 인증 방법: 인증서 기반 또는 Active Directory

2. **VPN 클라이언트 구성 파일 다운로드**
   - Client VPN Endpoint에서 구성 파일 다운로드

3. **로컬에 VPN 클라이언트 설치 및 연결**
   - [AWS Client VPN](https://aws.amazon.com/vpn/client-vpn-download/) 설치
   - 구성 파일 임포트 후 연결

4. **VPN 연결 후 직접 접근**
   ```bash
   # .env 파일 (VPN 연결 시)
   REDIS_HOST=tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com
   REDIS_PORT=6379
   REDIS_PASSWORD=YourSecureAuthToken123!
   REDIS_SSL_ENABLED=true
   TZ=Asia/Seoul
   ```

**참고**: [AWS Client VPN 공식 문서](https://docs.aws.amazon.com/vpn/latest/clientvpn-admin/what-is.html)

#### 4.4 방법 3: AWS Systems Manager Session Manager (포트 포워딩)

IAM 권한 기반으로 EC2를 통한 포트 포워딩을 설정합니다.

##### 4.4.1 사전 준비

1. **EC2 인스턴스에 SSM Agent 설치** (Amazon Linux 2는 기본 설치)
2. **EC2 인스턴스에 IAM 역할 연결**: `AmazonSSMManagedInstanceCore` 정책 포함
3. **로컬에 AWS CLI 및 Session Manager 플러그인 설치**
   ```bash
   # Session Manager 플러그인 설치 (macOS)
   brew install --cask session-manager-plugin
   
   # 또는 직접 다운로드
   # https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html
   ```

##### 4.4.2 포트 포워딩 설정

```bash
# SSM 포트 포워딩 시작
aws ssm start-session \
  --target <ec2-instance-id> \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{
    "host":["tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com"],
    "portNumber":["6379"],
    "localPortNumber":["6379"]
  }' \
  --region ap-northeast-2
```

**포워딩이 활성화된 상태에서**:

```bash
# .env 파일 (SSM 포트 포워딩 사용 시)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=YourSecureAuthToken123!
REDIS_SSL_ENABLED=false  # 터널 내부는 암호화되므로 false
TZ=Asia/Seoul
```

**참고**: [AWS Systems Manager 포트 포워딩](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-remote-port-forwarding)

#### 4.5 방법 비교 및 권장 사항

| 방법 | 복잡도 | 비용 | 보안 | 권장 시나리오 |
|------|--------|------|------|-------------|
| **SSH 터널링** | 낮음 | 낮음 (Bastion EC2만) | 높음 | 개발 환경, 간단한 테스트 |
| **AWS Client VPN** | 중간 | 높음 (VPN 시간당 과금) | 매우 높음 | 팀 전체가 VPC 리소스 접근 필요 |
| **SSM 포트 포워딩** | 중간 | 낮음 (EC2만) | 매우 높음 | SSH 키 관리 불필요, IAM 기반 권한 |

**권장 사항**:
- **개발 환경**: SSH 터널링 (방법 1) - 가장 간단하고 비용 효율적
- **팀 협업**: AWS Client VPN (방법 2) - 팀원 모두가 VPC 접근 필요 시
- **높은 보안 요구**: SSM 포트 포워딩 (방법 3) - SSH 키 관리 불필요

#### 4.6 프로덕션 환경 접속

**프로덕션 환경에서는 직접 접속하지 않습니다**:

1. **애플리케이션 서버에서만 접근**
   - EC2, ECS, Lambda 등 VPC 내부 리소스
   - 애플리케이션 서버 보안 그룹이 ElastiCache 보안 그룹에 허용됨

2. **모니터링 및 디버깅**
   - CloudWatch 메트릭 활용
   - CloudWatch Logs 활용
   - redis-cli 접속은 최소화 (필요 시 Bastion Host 사용)

### 5. 연결 테스트

#### 5.1 애플리케이션 시작 시 확인

애플리케이션 시작 시 다음 로그를 확인합니다:

```
INFO  o.s.d.r.c.LettuceConnectionFactory - Opening LettuceConnection
INFO  i.l.c.c.ConnectionFactory - Connecting to Redis server: tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com:6379
```

#### 5.2 수동 연결 테스트

**VPC 내부 (EC2 등)에서 연결 테스트**:

```bash
# redis-cli로 연결 테스트 (TLS/AUTH 사용)
redis-cli -h tech-n-ai-redis-cluster.xxxxx.ng.0001.apn2.cache.amazonaws.com \
  -p 6379 \
  --tls \
  -a YourSecureAuthToken123!

# 연결 후 테스트
> PING
PONG

> SET test:key "Hello ElastiCache"
OK

> GET test:key
"Hello ElastiCache"

> DEL test:key
(integer) 1
```

**로컬 환경에서 연결 테스트** (SSH 터널 또는 SSM 포트 포워딩 사용 시):

```bash
# redis-cli로 로컬호스트 연결 테스트 (TLS 불필요)
redis-cli -h localhost -p 6379 -a YourSecureAuthToken123!

# 연결 후 테스트
> PING
PONG
```

#### 5.3 Spring Boot Actuator를 통한 헬스 체크

```bash
# Redis 헬스 체크
curl http://localhost:8080/actuator/health/redis
```

예상 응답:
```json
{
  "status": "UP",
  "details": {
    "version": "7.1.0"
  }
}
```

### 6. 사용 패턴별 동작 확인

#### 6.1 OAuth State 저장 확인

```java
// OAuthStateService 테스트
oauthStateService.saveState("test-state-123", "google");

// Redis에서 확인
redis-cli -h <endpoint> --tls -a <auth-token>
> GET oauth:state:test-state-123
"google"

> TTL oauth:state:test-state-123
(integer) 599  # 10분 TTL
```

#### 6.2 Kafka 이벤트 멱등성 확인

```java
// EventConsumer 테스트
eventConsumer.markEventAsProcessed("event-123");

// Redis에서 확인
> GET processed_event:event-123
"processed"

> TTL processed_event:event-123
(integer) 604799  # 7일 TTL
```

---

## 모니터링 및 알림 설정

### 1. CloudWatch 메트릭

**참고**: [AWS 공식 문서 - CloudWatch 메트릭](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/CacheMetrics.html)

주요 모니터링 메트릭:
- `CPUUtilization`: CPU 사용률 (80% 이상 알림)
- `DatabaseMemoryUsagePercentage`: 메모리 사용률 (90% 이상 알림)
- `CurrConnections`: 현재 연결 수
- `Evictions`: 키 제거 횟수 (메모리 부족 지표)
- `CacheHits` / `CacheMisses`: 캐시 히트/미스 비율
- `ReplicationLag`: 복제 지연 시간 (Replica 노드)

#### 1.1 AWS Console을 통한 CloudWatch 메트릭 확인

1. **ElastiCache Console 접속**
   - AWS Console → ElastiCache → **Redis clusters** → 클러스터 선택

2. **Metrics 탭**
   - **CloudWatch metrics** 섹션에서 주요 메트릭 확인
   - 각 메트릭 클릭 시 상세 그래프 확인

3. **CloudWatch Console에서 확인**
   - AWS Console → CloudWatch → **Metrics** → **ElastiCache**
   - 클러스터별 메트릭 확인

#### 1.2 AWS CLI를 통한 CloudWatch 메트릭 확인

```bash
# CPU 사용률 확인 (최근 1시간)
aws cloudwatch get-metric-statistics \
  --namespace AWS/ElastiCache \
  --metric-name CPUUtilization \
  --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-001 \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region ap-northeast-2

# 메모리 사용률 확인
aws cloudwatch get-metric-statistics \
  --namespace AWS/ElastiCache \
  --metric-name DatabaseMemoryUsagePercentage \
  --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-001 \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region ap-northeast-2
```

### 2. CloudWatch 알림 설정

**참고**: [AWS 공식 문서 - 모니터링 베스트 프랙티스](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/BestPractices.Monitoring.html)

#### 2.1 AWS Console을 통한 알림 설정

1. **CloudWatch Console 접속**
   - AWS Console → CloudWatch → **Alarms** → **All alarms**

2. **알림 생성 - CPU 사용률**
   - **Create alarm** 버튼 클릭
   - **Select metric** 클릭
   - **ElastiCache** → **Per-Cache Node Metrics** 선택
   - `CPUUtilization` 메트릭 선택

3. **알림 조건 설정**
   - **Statistic**: `Average`
   - **Period**: `5 minutes`
   - **Threshold type**: `Static`
   - **Whenever CPUUtilization is...**: `Greater than 80`
   - **Datapoints to alarm**: `2 out of 2`

4. **알림 액션 설정**
   - **Notification**: SNS 토픽 선택 또는 새로 생성
   - **Alarm name**: `redis-cpu-high`

5. **알림 생성 완료**
   - **Create alarm** 버튼 클릭

#### 2.2 AWS CLI를 통한 알림 설정

```bash
# SNS 토픽 생성 (없는 경우)
SNS_TOPIC_ARN=$(aws sns create-topic \
  --name redis-alerts \
  --query 'TopicArn' \
  --output text \
  --region ap-northeast-2)

# SNS 구독 추가 (이메일)
aws sns subscribe \
  --topic-arn $SNS_TOPIC_ARN \
  --protocol email \
  --notification-endpoint your-email@example.com \
  --region ap-northeast-2

# CPU 사용률 알림 생성
aws cloudwatch put-metric-alarm \
  --alarm-name redis-cpu-high \
  --alarm-description "Redis CPU utilization is high" \
  --metric-name CPUUtilization \
  --namespace AWS/ElastiCache \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-001 \
  --alarm-actions $SNS_TOPIC_ARN \
  --region ap-northeast-2

# 메모리 사용률 알림 생성
aws cloudwatch put-metric-alarm \
  --alarm-name redis-memory-high \
  --alarm-description "Redis memory usage is high" \
  --metric-name DatabaseMemoryUsagePercentage \
  --namespace AWS/ElastiCache \
  --statistic Average \
  --period 300 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-001 \
  --alarm-actions $SNS_TOPIC_ARN \
  --region ap-northeast-2

# 연결 수 알림 생성
aws cloudwatch put-metric-alarm \
  --alarm-name redis-connections-high \
  --alarm-description "Redis connection count is high" \
  --metric-name CurrConnections \
  --namespace AWS/ElastiCache \
  --statistic Average \
  --period 300 \
  --threshold 50 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-001 \
  --alarm-actions $SNS_TOPIC_ARN \
  --region ap-northeast-2
```

### 3. CloudWatch 대시보드

주요 메트릭을 대시보드에 추가하여 한눈에 모니터링:

1. **CloudWatch Console → Dashboards**
2. **Create dashboard** → `Redis Monitoring`
3. 위젯 추가:
   - CPU 사용률 그래프
   - 메모리 사용률 그래프
   - 연결 수 그래프
   - 캐시 히트/미스 비율 그래프
   - Evictions 그래프

---

## 개발/테스트 환경 예상 비용

**참고**: [AWS 공식 문서 - ElastiCache 가격](https://aws.amazon.com/elasticache/pricing/)

### 1. 비용 구성 요소

ElastiCache for Redis의 비용은 배포 옵션에 따라 다르게 구성됩니다:

#### 1.1 노드 기반 캐시 비용 구성

| 비용 항목 | 설명 | 참고 |
|---|---|---|
| **노드 시간** | 선택한 노드 타입에 따라 시간 단위로 요금 부과 | On-Demand 또는 Reserved Instance 옵션 |
| **데이터 전송** | 인터넷 아웃바운드 전송 시 요금 | 리전 내 AZ 간 전송은 무료 |
| **백업 스토리지** | 자동 백업 스토리지 비용 | 백업 보존 기간에 따라 비용 발생 |

#### 1.2 서버리스 비용 구성

| 비용 항목 | 설명 | 참고 |
|---|---|---|
| **ECPUs** | ElastiCache Processing Units 사용량 | 시간당 과금, 최소 0.5 ECPUs |
| **데이터 스토리지** | 저장된 데이터 용량 | GB당 월별 과금 |
| **데이터 전송** | 인터넷 아웃바운드 전송 시 요금 | 리전 내 AZ 간 전송은 무료 |
| **스냅샷 스토리지** | 자동 스냅샷 스토리지 비용 (선택 사항) | 스토리지 비용과 별도 |

**참고**: 서버리스는 백업이 자동 관리되며, 노드 기반보다 유연한 과금 모델을 제공합니다.

### 2. 무료 티어 여부

**중요**: AWS ElastiCache for Redis는 **AWS Free Tier에 포함되지 않습니다**. 즉, 무료 티어가 제공되지 않으며, 사용 시 즉시 요금이 발생합니다.

### 3. 개발/테스트 환경 최소 비용 예시

#### 3.1 최소 구성

**가정 조건**:
- **Region**: `ap-northeast-2` (서울)
- **노드 타입**: `cache.t3.micro` (0.5GB RAM)
- **노드 수**: 2개 (Primary + Replica)
- **가동 시간**: 24시간 × 30일 = 720시간/월
- **데이터 전송**: 최소 (VPC 내부 통신)
- **백업**: 자동 백업 활성화 (7일 보존)

**예상 비용 계산**:

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **노드 비용** | 2개 × $0.020/hour × 720시간 | 약 **$28.80/월** |
| **백업 스토리지** | 매우 적은 데이터 (<1GB) | 약 **$0-1/월** |
| **데이터 전송** | 최소 (VPC 내부 통신) | 약 **$0-1/월** |
| **총 예상 비용** | | 약 **$29-31/월** |

#### 3.2 중규모 프로덕션 구성

**가정 조건**:
- **노드 타입**: `cache.t3.medium` (3.09GB RAM)
- **노드 수**: 2개 (Primary + Replica)
- **가동 시간**: 24시간 × 30일 = 720시간/월

**예상 비용 계산**:

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **노드 비용** | 2개 × $0.088/hour × 720시간 | 약 **$126.72/월** |
| **백업 스토리지** | 약 5GB | 약 **$0.5/월** |
| **데이터 전송** | 최소 | 약 **$0-2/월** |
| **총 예상 비용** | | 약 **$127-129/월** |

#### 3.3 대규모 프로덕션 구성

**가정 조건**:
- **노드 타입**: `cache.r6g.large` (13.07GB RAM)
- **노드 수**: 2개 (Primary + Replica)
- **가동 시간**: 24시간 × 30일 = 720시간/월

**예상 비용 계산**:

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **노드 비용** | 2개 × $0.243/hour × 720시간 | 약 **$349.92/월** |
| **백업 스토리지** | 약 10GB | 약 **$1/월** |
| **데이터 전송** | 최소 | 약 **$0-2/월** |
| **총 예상 비용** | | 약 **$350-353/월** |

**주의사항**:
- 위 비용은 ap-northeast-2 (서울) 리전 기준
- 실제 비용은 사용량, 데이터 전송량에 따라 변동
- 최신 가격은 [AWS ElastiCache 가격 페이지](https://aws.amazon.com/elasticache/pricing/)에서 확인

### 4. 비용 계산 도구

AWS에서는 다음 도구를 제공합니다:

1. **AWS Pricing Calculator**
   - URL: https://calculator.aws/
   - ElastiCache 노드 타입, 수량, 리전 등을 입력하여 예상 비용 계산

2. **AWS Cost Explorer**
   - 실제 사용량 기반 비용 분석
   - 리전별, 서비스별 비용 추이 확인

3. **AWS Budgets**
   - 예산 설정 및 알림
   - 비용 임계값 초과 시 알림

### 5. 예상 비용 요약

| 환경 | 구성 | 예상 월 비용 |
|---|---|---|
| **개발/테스트 (최소)** | cache.t3.micro, 2 nodes | 약 **$29-31/월** |
| **소규모 프로덕션** | cache.t3.medium, 2 nodes | 약 **$127-129/월** |
| **중규모 프로덕션** | cache.r6g.large, 2 nodes | 약 **$350-353/월** |

---

## 비용 절감 전략

### 1. 노드 타입 최적화

#### 1.1 워크로드 분석

CloudWatch 메트릭을 분석하여 노드 타입을 최적화합니다:

- **CPU 사용률**: 평균 30% 이하 → 다운사이징 고려
- **메모리 사용률**: 평균 50% 이하 → 다운사이징 고려
- **연결 수**: 최대 연결 수가 노드 사양보다 훨씬 적음 → 다운사이징 고려

#### 1.2 노드 타입 변경

```bash
# 노드 타입 변경 (다운타임 발생)
aws elasticache modify-replication-group \
  --replication-group-id tech-n-ai-redis-cluster \
  --cache-node-type cache.t3.small \
  --apply-immediately \
  --region ap-northeast-2
```

### 2. Reserved Instance 활용

장기 사용(1년 이상) 시 Reserved Instance를 활용하여 최대 40% 비용 절감:

```bash
# Reserved Cache Node 구매
aws elasticache purchase-reserved-cache-nodes-offering \
  --reserved-cache-nodes-offering-id <offering-id> \
  --cache-node-count 2 \
  --region ap-northeast-2
```

### 3. 백업 최적화

#### 3.1 백업 보존 기간 조정

```bash
# 백업 보존 기간 조정
aws elasticache modify-replication-group \
  --replication-group-id tech-n-ai-redis-cluster \
  --snapshot-retention-limit 3 \
  --region ap-northeast-2
```

#### 3.2 불필요한 스냅샷 삭제

```bash
# 스냅샷 목록 확인
aws elasticache describe-snapshots \
  --replication-group-id tech-n-ai-redis-cluster \
  --query 'Snapshots[*].[SnapshotName,SnapshotStatus,SnapshotCreateTime]' \
  --output table \
  --region ap-northeast-2

# 스냅샷 삭제
aws elasticache delete-snapshot \
  --snapshot-name <snapshot-name> \
  --region ap-northeast-2
```

### 4. 읽기 전용 복제본 최적화

#### 4.1 복제본 수 조정

현재 사용 패턴 분석 결과, 읽기 부하가 낮으므로 복제본 1개로 충분:

```bash
# 복제본 수 감소 (필요 시)
aws elasticache decrease-replica-count \
  --replication-group-id tech-n-ai-redis-cluster \
  --new-replica-count 1 \
  --apply-immediately \
  --region ap-northeast-2
```

### 5. 모니터링 기반 최적화

#### 5.1 비용 분석

AWS Cost Explorer를 활용하여 ElastiCache 비용을 분석합니다.

#### 5.2 사용률 기반 최적화

- **낮은 사용률 시간대**: 개발 환경의 경우 사용하지 않을 때 중지 고려
- **데이터 전송 최소화**: VPC 내부 통신 활용

---

## 트러블슈팅

### 1. 연결 문제

#### 1.1 연결 타임아웃

**증상**: `Connection timeout` 오류

**해결 방법**:
1. **보안 그룹 인바운드 규칙 확인**
   - 포트 6379 허용 여부 확인
   - Source 설정 확인 (애플리케이션 서버 보안 그룹 또는 VPC CIDR)

2. **서브넷 그룹 확인**
   - 클러스터와 애플리케이션 서버가 동일한 VPC에 있는지 확인
   - 라우팅 테이블 확인

3. **네트워크 ACL 확인**
   - VPC 네트워크 ACL이 포트 6379 트래픽을 허용하는지 확인

#### 1.2 AUTH 인증 실패

**증상**: `NOAUTH Authentication required` 오류

**해결 방법**:
1. **환경변수 확인**
   ```bash
   echo $REDIS_PASSWORD
   ```

2. **application-common-core.yml 확인**
   ```yaml
   spring:
     data:
       redis:
         password: ${REDIS_PASSWORD:}
   ```

3. **AUTH 토큰 재생성** (필요 시)
   ```bash
   # AUTH 토큰 변경
   aws elasticache modify-replication-group \
     --replication-group-id tech-n-ai-redis-cluster \
     --auth-token "NewAuthToken123!" \
     --apply-immediately \
     --region ap-northeast-2
   ```

#### 1.3 TLS/SSL 연결 실패

**증상**: `SSL/TLS connection failed` 오류

**해결 방법**:
1. **TLS 활성화 확인**
   ```yaml
   spring:
     data:
       redis:
         ssl:
           enabled: true
   ```

2. **클러스터 TLS 설정 확인**
   ```bash
   aws elasticache describe-replication-groups \
     --replication-group-id tech-n-ai-redis-cluster \
     --query 'ReplicationGroups[0].TransitEncryptionEnabled' \
     --region ap-northeast-2
   ```

### 2. 성능 이슈

#### 2.1 높은 CPU 사용률

**증상**: CPU 사용률 80% 이상

**해결 방법**:
1. **CloudWatch 메트릭 확인**
   - CPU 사용률 추이 분석
   - 높은 CPU를 사용하는 명령어 식별 (Slow Log)

2. **노드 타입 업그레이드**
   ```bash
   aws elasticache modify-replication-group \
     --replication-group-id tech-n-ai-redis-cluster \
     --cache-node-type cache.r6g.large \
     --apply-immediately \
     --region ap-northeast-2
   ```

3. **애플리케이션 최적화**
   - 불필요한 연결 제거
   - 연결 풀 크기 조정

#### 2.2 높은 메모리 사용률

**증상**: 메모리 사용률 90% 이상, Evictions 증가

**해결 방법**:
1. **메모리 사용량 확인**
   ```bash
   redis-cli -h <endpoint> --tls -a <auth-token> INFO memory
   ```

2. **TTL 설정 확인**
   - 모든 키에 적절한 TTL이 설정되어 있는지 확인
   - TTL이 없는 키 삭제 또는 TTL 추가

3. **노드 타입 업그레이드**
   - 메모리가 더 큰 노드 타입으로 변경

4. **Eviction Policy 확인**
   ```bash
   redis-cli -h <endpoint> --tls -a <auth-token> CONFIG GET maxmemory-policy
   ```
   - 기본값: `volatile-lru` (TTL이 있는 키 중 LRU)

#### 2.3 연결 수 부족

**증상**: `Too many connections` 오류

**해결 방법**:
1. **현재 연결 수 확인**
   ```bash
   redis-cli -h <endpoint> --tls -a <auth-token> INFO clients
   ```

2. **연결 풀 크기 조정**
   ```yaml
   # application-common-core.yml
   spring:
     data:
       redis:
         lettuce:
           pool:
             max-active: 8  # 필요 시 조정
   ```

3. **노드 타입 업그레이드**
   - 더 큰 노드 타입은 더 많은 연결을 지원

4. **유휴 연결 정리**
   - `idle-timeout` 설정으로 유휴 연결 자동 종료

### 3. 복제 지연

**증상**: Reader Endpoint에서 최신 데이터를 조회하지 못함

**해결 방법**:
1. **ReplicationLag 메트릭 확인**
   ```bash
   aws cloudwatch get-metric-statistics \
     --namespace AWS/ElastiCache \
     --metric-name ReplicationLag \
     --dimensions Name=CacheClusterId,Value=tech-n-ai-redis-cluster-002 \
     --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
     --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
     --period 300 \
     --statistics Average \
     --region ap-northeast-2
   ```

2. **복제본 성능 확인**
   - 복제본의 CPU/메모리 사용률 확인
   - 필요 시 노드 타입 업그레이드

3. **네트워크 지연 확인**
   - Primary와 Replica 간 네트워크 지연 확인
   - Multi-AZ 구성 시 AZ 간 지연 가능

### 4. 스토리지 문제

#### 4.1 백업 실패

**증상**: 자동 백업 실패 알림

**해결 방법**:
1. **백업 윈도우 조정**
   ```bash
   aws elasticache modify-replication-group \
     --replication-group-id tech-n-ai-redis-cluster \
     --snapshot-window "03:00-04:00" \
     --region ap-northeast-2
   ```

2. **메모리 사용률 확인**
   - 백업 시 메모리가 충분한지 확인

---

## 참고 자료

### AWS 공식 문서

#### ElastiCache for Redis 개요 및 시작하기
- [ElastiCache for Redis 개요](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/WhatIs.html)
- [시작하기](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/GettingStarted.html)
- [Redis 클러스터 생성](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Clusters.Create.html)

#### 배포 옵션 및 선택 가이드
- [ElastiCache Serverless for Redis](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/WhatIs.Serverless.html)
- [Serverless 시작하기](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/getting-started-serverless.html)
- [Serverless vs 노드 기반 비교](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/serverless-vs-nodes.html)
- [배포 옵션 선택 가이드](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/SelectEngine.html)

#### 클러스터 모드
- [클러스터 모드 선택](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Replication.Redis-RedisCluster.html)
- [Replication Group 관리](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/Replication.html)

#### 네트워크 및 보안
- [VPC 설정](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/VPCs.html)
- [보안 그룹](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/VPCs.SecurityGroups.html)
- [전송 중 암호화](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/in-transit-encryption.html)
- [저장 데이터 암호화](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/at-rest-encryption.html)
- [Redis AUTH 인증](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/auth.html)

#### 로컬 접속 및 VPN
- [AWS Client VPN 관리자 가이드](https://docs.aws.amazon.com/vpn/latest/clientvpn-admin/what-is.html)
- [AWS Systems Manager Session Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager.html)
- [Session Manager 포트 포워딩](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions-start.html#sessions-remote-port-forwarding)
- [ElastiCache 접근 패턴](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/accessing-elasticache.html)

#### 백업 및 복구
- [백업 및 복원](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/backups.html)
- [자동 백업](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/backups-automatic.html)

#### 모니터링
- [모니터링 개요](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/MonitoringECMetrics.html)
- [CloudWatch 메트릭](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/CacheMetrics.html)
- [모니터링 베스트 프랙티스](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/BestPractices.Monitoring.html)

#### AWS CLI 참조
- [ElastiCache CLI](https://docs.aws.amazon.com/cli/latest/reference/elasticache/)
- [create-replication-group](https://docs.aws.amazon.com/cli/latest/reference/elasticache/create-replication-group.html)
- [modify-replication-group](https://docs.aws.amazon.com/cli/latest/reference/elasticache/modify-replication-group.html)
- [describe-replication-groups](https://docs.aws.amazon.com/cli/latest/reference/elasticache/describe-replication-groups.html)

#### 비용 최적화
- [ElastiCache 가격](https://aws.amazon.com/elasticache/pricing/)
- [Reserved Instance](https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/reserved-nodes.html)

### Redis 공식 문서

#### Redis 설치 및 시작하기
- [Redis 다운로드 및 설치](https://redis.io/docs/install/install-redis/)
- [Redis 시작하기](https://redis.io/docs/getting-started/)
- [Redis on macOS](https://redis.io/docs/install/install-redis/install-redis-on-mac-os/)
- [Redis on Windows](https://redis.io/docs/install/install-redis/install-redis-on-windows/)
- [Redis on Linux](https://redis.io/docs/install/install-redis/install-redis-on-linux/)
- [Redis Docker Official Image](https://hub.docker.com/_/redis)

#### Redis 설정 및 관리
- [Redis 설정 (redis.conf)](https://redis.io/docs/management/config/)
- [Redis 보안](https://redis.io/docs/management/security/)
- [Redis 영속성 (Persistence)](https://redis.io/docs/management/persistence/)
- [Redis CLI 사용법](https://redis.io/docs/ui/cli/)
- [Redis 메모리 최적화](https://redis.io/docs/management/optimization/memory-optimization/)

#### Redis 명령어 및 데이터 타입
- [Redis Commands](https://redis.io/commands/)
- [Redis Data Types](https://redis.io/docs/data-types/)
- [Redis TTL 및 Expiration](https://redis.io/commands/expire/)

#### Redis GUI 도구
- [Redis GUI Tools 개요](https://redis.io/docs/connect/clients/#gui)
- [RedisInsight 공식 사이트](https://redis.io/insight/) (Redis 공식 GUI 도구)
- [RedisInsight 다운로드](https://redis.io/insight/)
- [Redis Commander GitHub](https://github.com/joeferner/redis-commander)
- [Medis GitHub](https://github.com/luin/medis) (macOS 전용)

### Spring Data Redis 공식 문서

- [Spring Data Redis 레퍼런스](https://docs.spring.io/spring-data/redis/reference/)
- [Spring Boot Redis 자동 설정](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis)
- [RedisTemplate 사용법](https://docs.spring.io/spring-data/redis/reference/redis/redis-template.html)
- [Lettuce 연결 설정](https://docs.spring.io/spring-data/redis/reference/redis/connection.html)

### 기타 참고 자료

#### Homebrew (macOS)
- [Homebrew 공식 사이트](https://brew.sh/)
- [Homebrew Redis Formula](https://formulae.brew.sh/formula/redis)

#### Docker
- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)

### 프로젝트 내부 문서

#### Redis 설정 및 베스트 프랙티스
- `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스
- `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 패턴

#### 소스 코드
- `common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java`
- `common/core/src/main/resources/application-common-core.yml`

---

**문서 버전**: 1.3  
**최종 업데이트**: 2026-01-20  
**작성자**: Infrastructure Architect  
**주요 내용**: AWS ElastiCache for Redis 프로덕션 환경 구축, 로컬 Redis 설치 및 연동, Redis GUI 도구, 배포 옵션 분석 및 프로젝트 연동 가이드
