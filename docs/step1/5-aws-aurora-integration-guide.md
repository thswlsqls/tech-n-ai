# AWS Aurora MySQL 클라우드 데이터소스 연동 설계서 

**작성 일시**: 2026-01-16  
**대상**: 프로덕션 환경 구축  
**Aurora MySQL 버전**: 3.x (MySQL 8.0+ 호환)  
**버전**: 1.0  

## 목차

1. [개요](#개요)
2. [클러스터 생성 및 구성](#클러스터-생성-및-구성)
   - AWS Console (GUI) 가이드
   - AWS CLI 가이드
3. [스키마 구축](#스키마-구축)
   - AWS Console Query Editor 사용법
   - MySQL 클라이언트 사용법
4. [프로젝트 연동](#프로젝트-연동)
5. [성능 최적화](#성능-최적화)
6. [비용 절감 전략](#비용-절감-전략)
7. [모니터링 및 유지보수](#모니터링-및-유지보수)
   - CloudWatch 메트릭 및 알림 설정
   - Performance Insights 활성화
8. [트러블슈팅](#트러블슈팅)

---

## 개요

### 서비스 소개

Amazon Aurora MySQL은 MySQL과 호환되는 관계형 데이터베이스 서비스로, 본 프로젝트에서는 CQRS 패턴의 Command Side(쓰기 전용) 데이터 저장소로 사용됩니다.

### 아키텍처 개요

- **Writer 엔드포인트**: 쓰기 작업 전용 (Primary 인스턴스)
- **Reader 엔드포인트**: 읽기 작업 전용 (읽기 전용 복제본)
- **스키마 분리**: `auth`, `archive`, `chatbot` 스키마로 모듈별 데이터 격리
- **TSID Primary Key**: 모든 테이블의 Primary Key는 TSID 방식(BIGINT UNSIGNED) 사용

### 참고 설계서

- **스키마 설계**: `docs/step1/3. aurora-schema-design.md`
- **베스트 프랙티스**: `docs/step1/aurora-mysql-schema-design-best-practices.md`
- **프로젝트 구조**: `docs/step1/1. multimodule-structure-verification.md`

### 공식 문서

이 가이드는 다음 AWS 공식 문서를 기반으로 작성되었습니다:
- [Amazon Aurora MySQL 개요](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html)
- [Aurora MySQL 클러스터 생성](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)
- [AWS CLI RDS 명령어 참조](https://docs.aws.amazon.com/cli/latest/reference/rds/)

자세한 참고 자료는 문서 하단의 [참고 자료](#참고-자료) 섹션을 참고하세요.

---

## 클러스터 생성 및 구성

### 1. Aurora MySQL 클러스터 생성

#### 1.1 AWS Console (GUI)을 통한 생성

**참고**: [AWS 공식 문서 - Aurora MySQL 클러스터 생성](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)

##### 1.1.1 콘솔 접속 및 초기 설정

1. **AWS Management Console 접속**
   - AWS Console에 로그인 → 상단 검색창에서 "RDS" 검색 → **Amazon RDS** 서비스 선택
   - 또는 직접 URL: `https://console.aws.amazon.com/rds/`

2. **데이터베이스 생성 시작**
   - 왼쪽 메뉴에서 **Databases** 클릭
   - 우측 상단 **Create database** 버튼 클릭

3. **데이터베이스 생성 방식 선택**
   - **Standard create** 선택 (프로덕션 환경 권장)
     - 모든 설정 옵션을 세밀하게 제어 가능
     - 네트워크, 보안, 백업, 모니터링 등 상세 설정 가능
   - **Easy create** (개발/테스트 환경용)
     - 기본 설정으로 빠르게 생성
     - 프로덕션 환경에는 권장하지 않음

##### 1.1.2 엔진 및 버전 선택

1. **엔진 옵션**
   - **Engine type**: Amazon Aurora 선택
   - **Edition**: Amazon Aurora MySQL 선택
   - **Version**: Aurora MySQL 3.x (MySQL 8.0 호환) - 최신 버전 권장
     - 예: `Aurora MySQL 3.06.0 (compatible with MySQL 8.0.40)`

##### 1.1.3 템플릿 선택

- **Production**: 프로덕션 환경 권장
  - 다중 AZ 배포, 자동 백업, Performance Insights 등 자동 활성화
- **Dev/Test**: 개발/테스트 환경용
  - 단일 AZ, 최소 설정

##### 1.1.4 설정 구성

1. **DB 클러스터 식별자**
   - **DB cluster identifier**: `tech-n-ai-aurora-cluster`
   - 소문자, 숫자, 하이픈만 사용 가능

2. **마스터 자격 증명**
   - **Master username**: `admin` (또는 보안 정책에 맞는 사용자명)
   - **Master password**: 
     - **Auto generate a password**: AWS Secrets Manager에 자동 저장 (권장)
     - **Enter password**: 직접 입력 (강력한 비밀번호 필수)
   - **참고**: AWS Secrets Manager 사용 시 비밀번호는 콘솔에서 확인 가능

##### 1.1.5 인스턴스 구성

1. **DB instance class**
   - **초기 워크로드 분석 기반 추천**:
     - **소규모 (개발/테스트)**: `db.t3.medium` (2 vCPU, 4GB RAM)
     - **중규모 (프로덕션)**: `db.r6g.large` (2 vCPU, 16GB RAM)
     - **대규모 (프로덕션)**: `db.r6g.xlarge` (4 vCPU, 32GB RAM) 이상
   - **주의**: 실제 워크로드 모니터링 후 다운사이징 고려

2. **가용성 및 내구성**
   - **Multi-AZ DB cluster**: 프로덕션 환경 필수
     - 최소 2개 이상의 가용 영역(AZ)에 인스턴스 배치
     - 고가용성 및 자동 장애 조치 제공
   - **Reader instances**: 
     - 초기에는 0개로 시작 (비용 최적화)
     - 필요 시 나중에 추가 가능

##### 1.1.6 네트워크 및 보안 설정

1. **Virtual Private Cloud (VPC)**
   - 기존 VPC 선택 또는 새 VPC 생성
   - **DB subnet group**: 
     - 기존 서브넷 그룹 선택 또는 새로 생성
     - 최소 2개 이상의 가용 영역에 서브넷 포함 필수

2. **Public access**
   - **Publicly accessible**: `No` (프로덕션 환경 권장)
     - VPC 내부에서만 접근 가능
     - 보안 강화

3. **VPC security group**
   - **Create new**: 새 보안 그룹 생성
   - **Choose existing**: 기존 보안 그룹 선택
   - **인바운드 규칙 설정** (보안 그룹 생성/수정 시):
     - Type: MySQL/Aurora (3306)
     - Source: 애플리케이션 서버 보안 그룹 또는 VPC CIDR

##### 1.1.7 데이터베이스 인증

- **Database authentication**: 
  - **Password authentication**: 기본 방식 (권장)
  - **Password and IAM database authentication**: IAM 인증 추가 (고급)

##### 1.1.8 추가 구성 (Additional configuration)

1. **Initial database name**
   - **Database name**: `auth` (초기 스키마)
   - 나중에 `archive`, `chatbot` 스키마 추가 생성 가능

2. **DB cluster parameter group**
   - 기본 파라미터 그룹 사용 또는 커스텀 그룹 선택

3. **백업 설정**
   - **Backup retention period**: `7 days` (프로덕션 권장)
   - **Backup window**: 트래픽이 적은 시간대 (예: `03:00-04:00 UTC`)
   - **Copy tags to snapshots**: 활성화 권장

4. **모니터링**
   - **Enable Enhanced monitoring**: 활성화 권장
   - **Enable Performance Insights**: 활성화 권장 (프로덕션 필수)
     - **Retention period**: 7 days (무료 티어)

5. **로그 내보내기**
   - **Log exports**: 다음 로그 활성화
     - `audit`
     - `error`
     - `general`
     - `slowquery`

6. **유지보수**
   - **Auto minor version upgrade**: 활성화 권장
   - **Maintenance window**: 트래픽이 적은 시간대 (예: `mon:04:00-mon:05:00 UTC`)

##### 1.1.9 생성 완료

1. **생성 시작**
   - 하단 **Create database** 버튼 클릭
   - 생성 완료까지 약 10-15분 소요

2. **엔드포인트 확인**
   - 생성 완료 후 **Databases** 목록에서 클러스터 선택
   - **Connectivity & security** 탭에서 다음 엔드포인트 확인:
     - **Writer endpoint**: `tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com`
     - **Reader endpoint**: `tech-n-ai-aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com`
     - **Port**: `3306`

#### 1.2 AWS CLI를 통한 생성

**참고**: [AWS CLI 공식 문서 - create-db-cluster](https://docs.aws.amazon.com/cli/latest/reference/rds/create-db-cluster.html)

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
   aws rds create-db-subnet-group \
     --db-subnet-group-name tech-n-ai-aurora-subnet-group \
     --db-subnet-group-description "Subnet group for Aurora cluster" \
     --subnet-ids subnet-xxxxx subnet-yyyyy \
     --region ap-northeast-2
   ```
   - **주의**: 최소 2개 이상의 서브넷이 서로 다른 가용 영역에 있어야 함

3. **보안 그룹 생성** (없는 경우)
   ```bash
   # 보안 그룹 생성
   aws ec2 create-security-group \
     --group-name tech-n-ai-aurora-sg \
     --description "Security group for Aurora cluster" \
     --vpc-id vpc-xxxxx \
     --region ap-northeast-2
   
   # 보안 그룹 ID 저장 (다음 단계에서 사용)
   SG_ID=$(aws ec2 describe-security-groups \
     --group-names tech-n-ai-aurora-sg \
     --query 'SecurityGroups[0].GroupId' \
     --output text \
     --region ap-northeast-2)
   
   # 인바운드 규칙 추가 (MySQL/Aurora 포트 3306)
   aws ec2 authorize-security-group-ingress \
     --group-id $SG_ID \
     --protocol tcp \
     --port 3306 \
     --source-group <application-server-sg-id> \
     --region ap-northeast-2
   ```

##### 1.2.2 Aurora MySQL 클러스터 생성

```bash
# Aurora MySQL 클러스터 생성
aws rds create-db-cluster \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --engine aurora-mysql \
  --engine-version 8.0.mysql_aurora.3.06.0 \
  --master-username admin \
  --master-user-password 'YourSecurePassword123!' \
  --database-name auth \
  --db-subnet-group-name tech-n-ai-aurora-subnet-group \
  --vpc-security-group-ids $SG_ID \
  --db-cluster-parameter-group-name default.aurora-mysql8.0 \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --storage-encrypted \
  --enable-cloudwatch-logs-exports audit,error,general,slowquery \
  --region ap-northeast-2
```

**주요 파라미터 설명**:
- `--db-cluster-identifier`: 클러스터 고유 식별자
- `--engine`: `aurora-mysql` (MySQL 호환)
- `--engine-version`: Aurora MySQL 버전 (최신 버전 권장)
- `--master-username`: 마스터 사용자 이름
- `--master-user-password`: 마스터 비밀번호 (프로덕션에서는 AWS Secrets Manager 사용 권장)
- `--database-name`: 초기 데이터베이스 이름
- `--db-subnet-group-name`: 서브넷 그룹 이름
- `--vpc-security-group-ids`: 보안 그룹 ID (공백으로 구분하여 여러 개 지정 가능)
- `--backup-retention-period`: 백업 보존 기간 (일)
- `--storage-encrypted`: 스토리지 암호화 활성화

##### 1.2.3 Writer 인스턴스 생성

**중요**: 클러스터 생성 후 반드시 Writer 인스턴스를 별도로 생성해야 합니다.

```bash
# Writer 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier tech-n-ai-aurora-writer \
  --db-instance-class db.r6g.large \
  --engine aurora-mysql \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --publicly-accessible \
  --no-publicly-accessible \
  --region ap-northeast-2
```

**주요 파라미터 설명**:
- `--db-instance-identifier`: 인스턴스 고유 식별자
- `--db-instance-class`: 인스턴스 클래스 (크기)
- `--db-cluster-identifier`: 속할 클러스터 식별자
- `--no-publicly-accessible`: 공개 접근 비활성화 (프로덕션 권장)

##### 1.2.4 Reader 인스턴스 생성 (선택사항)

```bash
# Reader 인스턴스 생성 (읽기 전용 복제본)
aws rds create-db-instance \
  --db-instance-identifier tech-n-ai-aurora-reader \
  --db-instance-class db.r6g.large \
  --engine aurora-mysql \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --no-publicly-accessible \
  --region ap-northeast-2
```

##### 1.2.5 생성 상태 확인

```bash
# 클러스터 상태 확인
aws rds describe-db-clusters \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --query 'DBClusters[0].[Status,Endpoint,ReaderEndpoint]' \
  --output table \
  --region ap-northeast-2

# 인스턴스 상태 확인
aws rds describe-db-instances \
  --db-instance-identifier tech-n-ai-aurora-writer \
  --query 'DBInstances[0].[DBInstanceStatus,Endpoint.Address,Endpoint.Port]' \
  --output table \
  --region ap-northeast-2
```

**상태 확인**:
- `Status`: `available` (생성 완료)
- `Endpoint`: Writer 엔드포인트 주소
- `ReaderEndpoint`: Reader 엔드포인트 주소 (Reader 인스턴스가 있는 경우)

### 2. 네트워크 구성

#### 2.1 VPC 및 서브넷 그룹 설정

**참고**: [AWS 공식 문서 - DB 서브넷 그룹](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html)

##### 2.1.1 AWS Console을 통한 서브넷 그룹 생성

1. **RDS Console 접속**
   - AWS Console → RDS → 왼쪽 메뉴 **Subnet groups** 클릭
   - **Create DB subnet group** 버튼 클릭

2. **서브넷 그룹 설정**
   - **Name**: `tech-n-ai-aurora-subnet-group`
   - **Description**: "Subnet group for Aurora cluster"
   - **VPC**: 기존 VPC 선택 또는 새 VPC 생성
   - **Availability Zones**: 최소 2개 이상 선택
     - 예: `ap-northeast-2a`, `ap-northeast-2c`
   - **Subnets**: 각 가용 영역에서 서브넷 선택
     - 각 서브넷은 충분한 IP 주소 범위 확보 (최소 /24 권장)
   - **Create** 버튼 클릭

##### 2.1.2 AWS CLI를 통한 서브넷 그룹 생성

```bash
# 서브넷 그룹 생성
aws rds create-db-subnet-group \
  --db-subnet-group-name tech-n-ai-aurora-subnet-group \
  --db-subnet-group-description "Subnet group for Aurora cluster" \
  --subnet-ids subnet-xxxxx subnet-yyyyy \
  --region ap-northeast-2

# 서브넷 그룹 확인
aws rds describe-db-subnet-groups \
  --db-subnet-group-name tech-n-ai-aurora-subnet-group \
  --region ap-northeast-2
```

**주의사항**:
- 최소 2개 이상의 서브넷이 서로 다른 가용 영역에 있어야 함
- 각 서브넷은 충분한 IP 주소 범위 확보 (최소 /24 권장)

##### 2.1.3 보안 그룹 설정

**참고**: [AWS 공식 문서 - 보안 그룹](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Overview.RDSDBSecurityGroups.html)

**AWS Console을 통한 보안 그룹 생성**:

1. **EC2 Console 접속**
   - AWS Console → EC2 → 왼쪽 메뉴 **Security Groups** 클릭
   - **Create security group** 버튼 클릭

2. **보안 그룹 설정**
   - **Security group name**: `tech-n-ai-aurora-sg`
   - **Description**: "Security group for Aurora cluster"
   - **VPC**: Aurora 클러스터와 동일한 VPC 선택

3. **인바운드 규칙 추가**
   - **Type**: MySQL/Aurora
   - **Port**: `3306`
   - **Source**: 다음 중 하나 선택
     - **Custom**: 애플리케이션 서버 보안 그룹 ID 입력
     - **My IP**: 개발 환경에서만 사용
     - **VPC CIDR**: VPC 내부 전체 (예: `10.0.0.0/16`)

4. **아웃바운드 규칙**: 기본 설정 유지 (모든 트래픽 허용)

5. **Create security group** 버튼 클릭

**AWS CLI를 통한 보안 그룹 생성**:

```bash
# 보안 그룹 생성
SG_ID=$(aws ec2 create-security-group \
  --group-name tech-n-ai-aurora-sg \
  --description "Security group for Aurora cluster" \
  --vpc-id vpc-xxxxx \
  --query 'GroupId' \
  --output text \
  --region ap-northeast-2)

echo "Security Group ID: $SG_ID"

# 인바운드 규칙 추가 (애플리케이션 서버 보안 그룹에서 접근 허용)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 3306 \
  --source-group <application-server-sg-id> \
  --region ap-northeast-2

# 또는 VPC CIDR에서 접근 허용 (개발 환경용)
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 3306 \
  --cidr 10.0.0.0/16 \
  --region ap-northeast-2
```

#### 2.2 엔드포인트 확인

##### 2.2.1 AWS Console을 통한 엔드포인트 확인

1. **RDS Console 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택

2. **Connectivity & security 탭**
   - **Endpoint & port** 섹션에서 다음 정보 확인:
     - **Writer endpoint**: `tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com`
     - **Reader endpoint**: `tech-n-ai-aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com`
     - **Port**: `3306`

3. **엔드포인트 복사**
   - 각 엔드포인트 옆 **Copy** 버튼 클릭하여 복사
   - 애플리케이션 환경변수 설정에 사용

##### 2.2.2 AWS CLI를 통한 엔드포인트 확인

```bash
# Writer 엔드포인트 확인
aws rds describe-db-clusters \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --query 'DBClusters[0].[Endpoint,ReaderEndpoint,Port]' \
  --output table \
  --region ap-northeast-2

# 인스턴스별 엔드포인트 확인
aws rds describe-db-instances \
  --db-instance-identifier tech-n-ai-aurora-writer \
  --query 'DBInstances[0].Endpoint.[Address,Port]' \
  --output table \
  --region ap-northeast-2
```

### 3. 백업 및 복구 전략

**참고**: [AWS 공식 문서 - 백업 및 복구](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/BackupRestoreAurora.html)

#### 3.1 자동 백업 설정

- **백업 보존 기간**: 7일 (프로덕션 권장)
- **백업 윈도우**: 트래픽이 적은 시간대 (예: 03:00-04:00 KST)
- **Point-in-Time Recovery**: 자동 활성화 (백업 보존 기간 내)

##### 3.1.1 AWS Console을 통한 백업 설정

1. **RDS Console 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택

2. **백업 설정 수정**
   - **Actions** → **Modify** 클릭
   - **Backup** 섹션에서 설정:
     - **Backup retention period**: `7 days`
     - **Backup window**: `03:00-04:00 UTC` (또는 원하는 시간대)
   - **Continue** → **Modify cluster** 클릭

##### 3.1.2 AWS CLI를 통한 백업 설정

```bash
# 백업 보존 기간 및 백업 윈도우 수정
aws rds modify-db-cluster \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --region ap-northeast-2
```

#### 3.2 스냅샷 관리

- **수동 스냅샷**: 주요 배포 전 생성
- **자동 스냅샷**: 자동 백업과 함께 생성
- **스냅샷 정리**: 불필요한 스냅샷 정기 삭제 (비용 절감)

##### 3.2.1 AWS Console을 통한 스냅샷 생성

1. **수동 스냅샷 생성**
   - AWS Console → RDS → **Databases** → 클러스터 선택
   - **Actions** → **Take snapshot** 클릭
   - **Snapshot name**: `tech-n-ai-aurora-snapshot-YYYYMMDD`
   - **Take snapshot** 버튼 클릭

2. **스냅샷 목록 확인**
   - 왼쪽 메뉴 **Snapshots** 클릭
   - 생성된 스냅샷 목록 확인

3. **스냅샷 삭제**
   - **Snapshots** 목록에서 스냅샷 선택
   - **Actions** → **Delete snapshot** 클릭
   - 확인 후 삭제

##### 3.2.2 AWS CLI를 통한 스냅샷 관리

```bash
# 수동 스냅샷 생성
aws rds create-db-cluster-snapshot \
  --db-cluster-snapshot-identifier tech-n-ai-aurora-snapshot-$(date +%Y%m%d) \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --region ap-northeast-2

# 스냅샷 목록 확인
aws rds describe-db-cluster-snapshots \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --query 'DBClusterSnapshots[*].[DBClusterSnapshotIdentifier,Status,SnapshotCreateTime]' \
  --output table \
  --region ap-northeast-2

# 스냅샷 삭제
aws rds delete-db-cluster-snapshot \
  --db-cluster-snapshot-identifier tech-n-ai-aurora-snapshot-YYYYMMDD \
  --region ap-northeast-2
```

##### 3.2.3 스냅샷으로부터 복구

**AWS Console을 통한 복구**:

1. **Snapshots** 메뉴에서 복구할 스냅샷 선택
2. **Actions** → **Restore snapshot** 클릭
3. 복구 설정 입력:
   - **DB cluster identifier**: 새 클러스터 식별자
   - **DB instance class**: 인스턴스 클래스 선택
   - 기타 설정 (네트워크, 보안 등)
4. **Restore DB cluster** 버튼 클릭

**AWS CLI를 통한 복구**:

```bash
# 스냅샷으로부터 클러스터 복구
aws rds restore-db-cluster-from-snapshot \
  --db-cluster-identifier tech-n-ai-aurora-cluster-restored \
  --snapshot-identifier tech-n-ai-aurora-snapshot-YYYYMMDD \
  --engine aurora-mysql \
  --region ap-northeast-2

# 복구된 클러스터에 인스턴스 생성
aws rds create-db-instance \
  --db-instance-identifier tech-n-ai-aurora-writer-restored \
  --db-instance-class db.r6g.large \
  --engine aurora-mysql \
  --db-cluster-identifier tech-n-ai-aurora-cluster-restored \
  --region ap-northeast-2
```

### 4. 모니터링 및 알림 설정

**참고**: [AWS 공식 문서 - 모니터링](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/MonitoringOverview.html)

#### 4.1 CloudWatch 메트릭

주요 모니터링 메트릭:
- `CPUUtilization`: CPU 사용률 (80% 이상 알림)
- `DatabaseConnections`: 데이터베이스 연결 수
- `FreeableMemory`: 사용 가능한 메모리
- `ReadLatency` / `WriteLatency`: 읽기/쓰기 지연 시간
- `AuroraReplicaLag`: 복제 지연 시간

##### 4.1.1 AWS Console을 통한 CloudWatch 메트릭 확인

1. **RDS Console 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택

2. **Monitoring 탭**
   - **CloudWatch metrics** 섹션에서 주요 메트릭 확인
   - 각 메트릭 클릭 시 상세 그래프 확인

3. **CloudWatch Console에서 확인**
   - AWS Console → CloudWatch → **Metrics** → **RDS**
   - 클러스터별 메트릭 확인

##### 4.1.2 AWS CLI를 통한 CloudWatch 메트릭 확인

```bash
# CPU 사용률 확인 (최근 1시간)
aws cloudwatch get-metric-statistics \
  --namespace AWS/RDS \
  --metric-name CPUUtilization \
  --dimensions Name=DBClusterIdentifier,Value=tech-n-ai-aurora-cluster \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 \
  --statistics Average \
  --region ap-northeast-2
```

#### 4.2 Performance Insights 활성화

- **활성화**: 프로덕션 환경 필수
- **보존 기간**: 7일 (무료 티어)
- **주요 기능**: 쿼리 성능 분석, 대기 이벤트 분석

##### 4.2.1 AWS Console을 통한 Performance Insights 활성화

1. **RDS Console 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택

2. **Performance Insights 활성화**
   - **Actions** → **Modify** 클릭
   - **Performance Insights** 섹션:
     - **Enable Performance Insights**: 체크
     - **Retention period**: `7 days` (무료 티어)
   - **Continue** → **Modify cluster** 클릭

3. **Performance Insights 대시보드 확인**
   - 클러스터 선택 → **Performance Insights** 탭
   - Top SQL, 대기 이벤트 등 확인

##### 4.2.2 AWS CLI를 통한 Performance Insights 활성화

```bash
# Performance Insights 활성화
aws rds modify-db-cluster \
  --db-cluster-identifier tech-n-ai-aurora-cluster \
  --enable-performance-insights \
  --performance-insights-retention-period 7 \
  --region ap-northeast-2
```

#### 4.3 CloudWatch 알림 설정

##### 4.3.1 AWS Console을 통한 알림 설정

1. **CloudWatch Console 접속**
   - AWS Console → CloudWatch → **Alarms** → **All alarms**

2. **알림 생성**
   - **Create alarm** 버튼 클릭
   - **Select metric** 클릭
   - **RDS** → **Per-Database Metrics** 선택
   - 메트릭 선택 (예: `CPUUtilization`)

3. **알림 조건 설정**
   - **Statistic**: `Average`
   - **Period**: `5 minutes`
   - **Threshold type**: `Static`
   - **Whenever CPUUtilization is...**: `Greater than 80`
   - **Additional configuration**:
     - **Datapoints to alarm**: `2 out of 2`
     - **Missing data treatment**: `Treat missing data as breaching`

4. **알림 액션 설정**
   - **Notification**: SNS 토픽 선택 또는 새로 생성
   - **Alarm name**: `aurora-cpu-high`

5. **알림 생성 완료**
   - **Create alarm** 버튼 클릭

##### 4.3.2 AWS CLI를 통한 알림 설정

```bash
# SNS 토픽 생성 (없는 경우)
SNS_TOPIC_ARN=$(aws sns create-topic \
  --name aurora-alerts \
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
  --alarm-name aurora-cpu-high \
  --alarm-description "Aurora CPU utilization is high" \
  --metric-name CPUUtilization \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=DBClusterIdentifier,Value=tech-n-ai-aurora-cluster \
  --alarm-actions $SNS_TOPIC_ARN \
  --region ap-northeast-2

# 메모리 사용률 알림 생성
aws cloudwatch put-metric-alarm \
  --alarm-name aurora-memory-low \
  --alarm-description "Aurora freeable memory is low" \
  --metric-name FreeableMemory \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 1000000000 \
  --comparison-operator LessThanThreshold \
  --evaluation-periods 2 \
  --dimensions Name=DBClusterIdentifier,Value=tech-n-ai-aurora-cluster \
  --alarm-actions $SNS_TOPIC_ARN \
  --region ap-northeast-2
```

---

## 스키마 구축

### 1. 스키마 생성

`docs/step1/3. aurora-schema-design.md`의 DDL 예제를 참고하여 다음 스키마를 생성합니다:

```sql
-- auth 스키마 생성
CREATE DATABASE IF NOT EXISTS auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- archive 스키마 생성
CREATE DATABASE IF NOT EXISTS archive
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- chatbot 스키마 생성
CREATE DATABASE IF NOT EXISTS chatbot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

#### 1.1 AWS Console Query Editor를 통한 스키마 생성

**참고**: [AWS 공식 문서 - Query Editor](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/query-editor.html)

**전제 조건**: RDS Data API 활성화 필요 (클러스터 생성 시 자동 활성화 또는 수동 활성화)

1. **Query Editor 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택
   - 상단 탭에서 **Query Editor** 클릭

2. **인증 정보 입력**
   - **Database username**: 마스터 사용자 이름 (예: `admin`)
   - **Database password**: 마스터 비밀번호
   - **Database name**: 초기 데이터베이스 (예: `auth`)

3. **스키마 생성 쿼리 실행**
   - Query Editor 창에 위의 DDL 입력
   - **Auto-commit** 활성화 (기본값)
   - **Run** 버튼 클릭 또는 `Ctrl+Enter` 단축키

4. **결과 확인**
   - 실행 결과 확인
   - 오류 발생 시 쿼리 수정 후 재실행

5. **추가 스키마 생성**
   - `archive`, `chatbot` 스키마도 동일한 방법으로 생성

**주의사항**:
- Query Editor는 트랜잭션 모드도 지원 (Auto-commit 비활성화 시)
- DDL 문은 암묵적으로 커밋되므로 트랜잭션 모드에서도 즉시 반영됨

#### 1.2 MySQL 클라이언트를 통한 스키마 생성

**AWS CLI 또는 로컬 MySQL 클라이언트 사용**:

```bash
# MySQL 클라이언트로 연결
mysql -h tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com \
  -u admin \
  -p \
  --ssl-mode=REQUIRED

# 연결 후 스키마 생성
CREATE DATABASE IF NOT EXISTS auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS archive
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS chatbot
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

# 스키마 확인
SHOW DATABASES;
```

### 2. 테이블 생성

각 스키마별 테이블은 `docs/step1/3. aurora-schema-design.md`의 **DDL 예제** 섹션을 참고하여 생성합니다.

**주의사항**:
- DDL 예제는 스키마별로 `USE <schema>;` 명령어로 스키마를 선택한 후 실행
- Foreign Key 제약조건은 스키마 간 참조가 불가능하므로 애플리케이션 레벨에서 처리
- 인덱스는 Command Side 최적화를 위해 최소한만 생성

#### 2.1 AWS Console Query Editor를 통한 테이블 생성

1. **Query Editor 접속**
   - AWS Console → RDS → **Databases** → 클러스터 선택 → **Query Editor**

2. **스키마 선택**
   ```sql
   USE auth;
   ```

3. **테이블 생성 DDL 실행**
   - `docs/step1/3. aurora-schema-design.md`의 DDL 예제 복사
   - Query Editor에 붙여넣기
   - **Run** 버튼 클릭

4. **결과 확인**
   - 실행 성공 메시지 확인
   - 오류 발생 시 DDL 수정 후 재실행

5. **테이블 목록 확인**
   ```sql
   SHOW TABLES;
   ```

#### 2.2 MySQL 클라이언트를 통한 테이블 생성

```bash
# MySQL 클라이언트로 연결
mysql -h tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com \
  -u admin \
  -p \
  --ssl-mode=REQUIRED

# 스키마 선택 및 테이블 생성
USE auth;

-- providers 테이블 생성 (DDL 예제 참고)
CREATE TABLE providers (
    -- DDL 예제 참고
);

-- users 테이블 생성
CREATE TABLE users (
    -- DDL 예제 참고
);

-- 테이블 목록 확인
SHOW TABLES;
```

### 3. 인덱스 생성

인덱스 생성은 `docs/step1/3. aurora-schema-design.md`의 **인덱스 전략** 섹션을 참고합니다.

**Command Side 인덱스 최소화 원칙**:
- UNIQUE 제약조건 인덱스 (데이터 무결성)
- 외래 키 인덱스 (참조 무결성)
- Soft Delete 인덱스 (`is_deleted`)
- 읽기 최적화 인덱스는 Query Side(MongoDB Atlas)에서 처리

#### 3.1 AWS Console Query Editor를 통한 인덱스 생성

1. **Query Editor 접속**
   - 스키마 선택: `USE auth;`

2. **인덱스 생성 DDL 실행**
   ```sql
   -- 예시: users 테이블의 email UNIQUE 인덱스
   CREATE UNIQUE INDEX idx_users_email ON users(email);
   
   -- 예시: users 테이블의 is_deleted 인덱스
   CREATE INDEX idx_users_is_deleted ON users(is_deleted);
   ```

3. **인덱스 확인**
   ```sql
   SHOW INDEXES FROM users;
   ```

#### 3.2 MySQL 클라이언트를 통한 인덱스 생성

```bash
# MySQL 클라이언트로 연결 후 인덱스 생성
mysql -h tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com \
  -u admin \
  -p \
  --ssl-mode=REQUIRED

USE auth;

-- 인덱스 생성 (DDL 예제 참고)
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_deleted ON users(is_deleted);

-- 인덱스 확인
SHOW INDEXES FROM users;
```

### 4. Flyway 마이그레이션 (선택사항)

버전 관리가 필요한 경우 Flyway를 사용하여 마이그레이션 스크립트를 관리합니다:

```sql
-- V1__Create_auth_schema.sql
CREATE DATABASE IF NOT EXISTS auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE auth;

-- V2__Create_providers_table.sql
CREATE TABLE providers (
    -- DDL 예제 참고
);

-- V3__Create_users_table.sql
CREATE TABLE users (
    -- DDL 예제 참고
);
```

**Flyway 사용 시**:
- 애플리케이션 시작 시 자동으로 마이그레이션 실행
- 버전 관리 및 롤백 지원
- 프로덕션 환경에서 권장

---

## 프로젝트 연동

### 1. 환경변수 설정

#### 1.1 필수 환경변수

다음 환경변수를 설정합니다:

```bash
# Aurora DB Cluster 연결 정보
AURORA_WRITER_ENDPOINT=tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_READER_ENDPOINT=tech-n-ai-aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_USERNAME=admin
AURORA_PASSWORD=your-secure-password
AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true&cachePrepStmts=true

# 기타 설정
DB_FETCH_CHUNKSIZE=250
DB_BATCH_SIZE=50
TZ=Asia/Seoul
```

#### 1.2 로컬 환경 설정 (`.env` 파일)

로컬 개발 환경에서는 `.env` 파일을 사용합니다:

```bash
# .env 파일 (프로젝트 루트)
AURORA_WRITER_ENDPOINT=tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_READER_ENDPOINT=tech-n-ai-aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
AURORA_USERNAME=admin
AURORA_PASSWORD=your-secure-password
AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&rewriteBatchedStatements=true&cachePrepStmts=true
DB_FETCH_CHUNKSIZE=250
DB_BATCH_SIZE=50
TZ=Asia/Seoul
```

**주의사항**: `.env` 파일은 `.gitignore`에 포함되어야 합니다.

#### 1.3 프로덕션 환경 설정

프로덕션 환경에서는 다음 방법 중 하나를 사용합니다:

- **AWS Secrets Manager**: 비밀번호 등 민감 정보 관리
- **AWS Systems Manager Parameter Store**: 환경변수 관리
- **환경변수 직접 설정**: 컨테이너/서버 환경변수로 설정

### 2. JDBC 드라이버 설정

프로젝트는 AWS RDS JDBC Driver를 사용합니다:

```gradle
// build.gradle (domain/aurora)
dependencies {
    implementation 'software.amazon.awssdk:rds:2.20.0'
    implementation 'com.mysql:mysql-connector-j:8.0.33'
    // AWS RDS JDBC Driver는 별도로 추가하지 않음 (HikariCP에서 자동 처리)
}
```

**참고**: `application-api-domain.yml`에서 `driver-class-name: software.aws.rds.jdbc.mysql.Driver`로 설정되어 있으나, 실제로는 표준 MySQL Connector/J를 사용합니다.

### 3. 모듈별 스키마 매핑

각 API 모듈의 `application.yml`에서 스키마를 설정합니다:

#### 3.1 api-auth 모듈

```yaml
# api/auth/src/main/resources/application.yml
module:
  aurora:
    schema: auth
```

#### 3.2 api-archive 모듈

```yaml
# api/archive/src/main/resources/application.yml
module:
  aurora:
    schema: archive
```

#### 3.3 api-chatbot 모듈

```yaml
# api/chatbot/src/main/resources/application.yml
module:
  aurora:
    schema: chatbot
```

### 4. 연결 설정 확인

`domain/aurora/src/main/resources/application-api-domain.yml` 파일이 자동으로 `${module.aurora.schema}` 환경변수를 사용하여 스키마를 참조합니다.

### 5. 연결 테스트

#### 5.1 애플리케이션 시작 시 확인

애플리케이션 시작 시 다음 로그를 확인합니다:

```
Will be WORK WITH WRITER
Will be WORK WITH READER
```

#### 5.2 수동 연결 테스트

```bash
# MySQL 클라이언트로 연결 테스트
mysql -h tech-n-ai-aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com \
  -u admin \
  -p \
  --ssl-mode=REQUIRED

# 연결 후 스키마 확인
SHOW DATABASES;
USE auth;
SHOW TABLES;
```

---

## 성능 최적화

### 1. 읽기 전용 복제본 활용

#### 1.1 Reader 엔드포인트 사용

`ApiDataSourceConfig`에서 Reader DataSource는 `readOnly=true`로 설정되어 있습니다:

```java
apiReaderHikariConfig.setReadOnly(true);
```

#### 1.2 트랜잭션별 데이터소스 선택

- **쓰기 작업**: `@Transactional` 기본 (Writer 사용)
- **읽기 작업**: `@Transactional(readOnly = true)` (Reader 사용)

### 2. 연결 풀 최적화

#### 2.1 HikariCP 설정

현재 설정 (`application-api-domain.yml`):

```yaml
spring:
  datasource:
    api:
      writer:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 5000
          idle-timeout: 300000
          max-lifetime: 28795000
```

#### 2.2 워크로드 기반 최적화

- **소규모 워크로드**: `maximum-pool-size: 10`, `minimum-idle: 2`
- **중규모 워크로드**: `maximum-pool-size: 20`, `minimum-idle: 5` (현재 설정)
- **대규모 워크로드**: `maximum-pool-size: 50`, `minimum-idle: 10`

**주의**: 연결 풀 크기는 인스턴스 크기와 워크로드를 고려하여 조정합니다.

### 3. 인덱스 전략

Command Side는 쓰기 최적화를 위해 인덱스를 최소화합니다:

- **필수 인덱스만 생성**: UNIQUE, Foreign Key, Soft Delete
- **읽기 최적화 인덱스 제외**: Query Side(MongoDB Atlas)에서 처리

자세한 인덱스 전략은 `docs/step1/3. aurora-schema-design.md`의 **인덱스 전략** 섹션을 참고합니다.

### 4. 쿼리 최적화

#### 4.1 배치 INSERT

```java
// 배치 INSERT 최적화
@Transactional
public void batchInsert(List<Entity> entities) {
    for (Entity entity : entities) {
        repository.save(entity);
    }
    // Hibernate가 자동으로 배치 처리 (jdbc.batch_size: 50)
}
```

#### 4.2 Prepared Statement 캐싱

`application-api-domain.yml`에서 이미 활성화:

```yaml
data-source-properties:
  cachePrepStmts: true
  prepStmtCacheSize: 250
  prepStmtCacheSqlLimit: 2048
  useServerPrepStmts: true
```

### 5. Aurora 특화 기능 활용

#### 5.1 Fast Insert

Aurora MySQL은 순차적인 키(TSID)를 사용할 때 Fast Insert 기능을 자동으로 활용합니다.

#### 5.2 병렬 쿼리 (선택사항)

대규모 분석 쿼리의 경우 병렬 쿼리를 활성화할 수 있습니다:

```sql
SET SESSION aurora_parallel_query = ON;
```

---

## 개발/테스트 환경 예상 비용

**참고**: [AWS 공식 문서 - Aurora 가격](https://aws.amazon.com/rds/aurora/pricing/)

### 1. 비용 구성 요소

AWS Aurora의 비용은 다음 항목으로 구성됩니다:

| 비용 항목 | 설명 | 참고 |
|---|---|---|
| **인스턴스 시간** | 선택한 인스턴스 타입에 따라 시간 단위로 요금 부과 | On-Demand 또는 Reserved Instance 옵션 |
| **Aurora Capacity Units (ACU)** | Aurora Serverless v2의 단위, 자동 확장/축소 | 최소 ACU 설정 필요 |
| **스토리지** | GB-month 단위 스토리지 비용 | Standard: 약 $0.10/GB-month |
| **I/O 요청** | 백만 건당 I/O 요청 비용 (Standard 모드만) | 약 $0.20/백만 요청 |
| **백업 스토리지** | DB 스토리지 크기의 100%까지 무료, 초과분 요금 | 약 $0.021/GB-month |
| **데이터 전송** | 인터넷 아웃바운드 전송 시 요금 | 리전 내 AZ 간 전송은 무료 |

### 2. 무료 티어 여부

**중요**: AWS Aurora는 **AWS Free Tier에 포함되지 않습니다**. 즉, Aurora MySQL/PostgreSQL 모두 무료 티어가 제공되지 않으며, 사용 시 즉시 요금이 발생합니다.

- 일반 RDS (MySQL, PostgreSQL 등)의 Free Tier는 Aurora에 적용되지 않음
- Aurora DSQL (신규 기능)에는 별도의 Free Tier가 있으나, 일반 Aurora MySQL과는 다른 서비스

### 3. 개발/테스트 환경 최소 비용 예시

#### 3.1 Aurora Serverless v2 최소 구성

**가정 조건**:
- **Region**: `ap-northeast-2` (서울)
- **Serverless v2 최소 ACU**: 0.5 ACU
- **가동 시간**: 24시간 × 30일 = 720시간/월
- **스토리지**: 20-50 GB
- **I/O 요청**: 매우 낮음 (개발/테스트 환경)

**예상 비용 계산**:

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **컴퓨트 비용** | 0.5 ACU × $0.12/ACU-hour × 720시간 | 약 **$43-45/월** |
| **스토리지 비용** | 30 GB × $0.10/GB-month | 약 **$3/월** |
| **I/O 요청 비용** | 매우 낮은 사용량 (1M 요청 미만) | 약 **$0-1/월** |
| **백업 스토리지** | 30 GB 이내 (무료 범위) | **$0/월** |
| **데이터 전송** | 최소 (VPC 내부 통신) | 약 **$0-2/월** |
| **총 예상 비용** | | 약 **$46-51/월** |

**주의사항**:
- 위 비용은 최소 구성 기준이며, 실제 사용량에 따라 변동 가능
- 리전별 가격 차이 존재 (ap-northeast-2 기준)
- I/O 요청이 많을 경우 추가 비용 발생
- 백업 보존 기간이 길거나 스냅샷이 많을 경우 백업 스토리지 비용 추가

#### 3.2 On-Demand 인스턴스 최소 구성

**가정 조건**:
- **인스턴스 클래스**: `db.t3.medium` (2 vCPU, 4GB RAM)
- **가동 시간**: 24시간 × 30일 = 720시간/월
- **스토리지**: 20-50 GB
- **Single-AZ**: 고가용성 없음 (개발/테스트 환경)

**예상 비용 계산**:

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **인스턴스 비용** | $0.072/hour × 720시간 | 약 **$52/월** |
| **스토리지 비용** | 30 GB × $0.10/GB-month | 약 **$3/월** |
| **I/O 요청 비용** | 매우 낮은 사용량 | 약 **$0-1/월** |
| **백업 스토리지** | 30 GB 이내 (무료 범위) | **$0/월** |
| **총 예상 비용** | | 약 **$55-56/월** |

**참고**: 
- `db.t3.medium`은 개발/테스트 환경용 최소 권장 사양
- 프로덕션 환경에서는 `db.r6g.large` 이상 권장 (약 $155/월)

### 4. 비용 계산 도구

AWS에서는 다음 도구를 제공합니다:

1. **AWS Pricing Calculator**
   - URL: https://calculator.aws/
   - Aurora 인스턴스, 스토리지, I/O 요청 등을 입력하여 예상 비용 계산

2. **AWS Cost Explorer**
   - 실제 사용량 기반 비용 분석
   - 리전별, 서비스별 비용 추이 확인

3. **AWS Budgets**
   - 예산 설정 및 알림
   - 비용 임계값 초과 시 알림

### 5. 비용 절감 팁 (개발/테스트 환경)

1. **Serverless v2 최소 ACU 사용**
   - 유휴 상태에서 최소 용량(0.5 ACU)으로 유지
   - 자동 스케일링으로 필요 시에만 확장

2. **스냅샷 정리**
   - 불필요한 스냅샷 정기 삭제
   - 백업 보존 기간 최소화 (개발 환경)

3. **데이터 전송 최소화**
   - VPC 내부 통신 활용
   - 인터넷 아웃바운드 전송 최소화

4. **사용하지 않을 때 중지**
   - 개발 환경에서는 필요 시에만 클러스터 실행
   - 테스트 완료 후 클러스터 삭제 고려

5. **리전 선택**
   - ap-northeast-2 (서울) 리전 사용 시 데이터 전송 비용 절감

### 6. 예상 비용 요약

| 환경 | 구성 | 예상 월 비용 |
|---|---|---|
| **개발/테스트 (최소)** | Serverless v2, 0.5 ACU, 30GB 스토리지 | 약 **$46-51/월** |
| **개발/테스트 (On-Demand)** | db.t3.medium, 30GB 스토리지 | 약 **$55-56/월** |
| **소규모 프로덕션** | db.r6g.large, 100GB 스토리지 | 약 **$155-170/월** |

**참고**: 
- 위 비용은 ap-northeast-2 (서울) 리전 기준
- 실제 비용은 사용량, I/O 요청, 데이터 전송량에 따라 변동
- 최신 가격은 [AWS Aurora 가격 페이지](https://aws.amazon.com/rds/aurora/pricing/)에서 확인

---

## 비용 절감 전략

### 1. 인스턴스 크기 최적화

#### 1.1 워크로드 분석

CloudWatch 메트릭을 분석하여 인스턴스 크기를 최적화합니다:

- **CPU 사용률**: 평균 30% 이하 → 다운사이징 고려
- **메모리 사용률**: 평균 50% 이하 → 다운사이징 고려
- **연결 수**: 최대 연결 수가 인스턴스 사양보다 훨씬 적음 → 다운사이징 고려

#### 1.2 인스턴스 다운사이징

```bash
# 인스턴스 클래스 변경 (다운타임 발생)
aws rds modify-db-instance \
  --db-instance-identifier tech-n-ai-aurora-writer \
  --db-instance-class db.r6g.medium \
  --apply-immediately
```

### 2. 예약 인스턴스 활용

장기 사용(1년 이상) 시 예약 인스턴스를 활용하여 최대 40% 비용 절감:

```bash
# 예약 인스턴스 구매
aws rds purchase-reserved-db-instances-offering \
  --reserved-db-instances-offering-id <offering-id> \
  --db-instance-count 1
```

### 3. 스토리지 최적화

#### 3.1 자동 스케일링

Aurora는 스토리지가 자동으로 스케일링되므로 별도 설정 불필요합니다.

#### 3.2 스냅샷 정리

불필요한 스냅샷을 정기적으로 삭제합니다:

```bash
# 오래된 스냅샷 삭제
aws rds delete-db-snapshot \
  --db-snapshot-identifier <snapshot-id>
```

### 4. 모니터링 기반 최적화

#### 4.1 비용 분석

AWS Cost Explorer를 활용하여 데이터베이스 비용을 분석합니다.

#### 4.2 사용률 기반 최적화

- **낮은 사용률 시간대**: Reader 인스턴스 일시 중지 고려 (수동 작업 필요)
- **읽기 전용 복제본**: 실제 필요 시에만 생성

---

## 모니터링 및 유지보수

### 1. CloudWatch 대시보드

주요 메트릭을 대시보드에 추가하여 모니터링합니다:

- CPU 사용률
- 메모리 사용률
- 데이터베이스 연결 수
- 읽기/쓰기 지연 시간
- 복제 지연 시간

### 2. Performance Insights

Performance Insights를 활성화하여 쿼리 성능을 분석합니다:

- **Top SQL**: 가장 많이 실행되는 쿼리
- **대기 이벤트**: 쿼리 대기 시간 분석
- **데이터베이스 로드**: 시간대별 부하 분석

### 3. 정기 유지보수

#### 3.1 백업 확인

- 자동 백업이 정상적으로 실행되는지 확인
- Point-in-Time Recovery 테스트 (분기별)

#### 3.2 패치 및 업데이트

- **유지보수 윈도우**: 트래픽이 적은 시간대 설정
- **자동 업데이트**: 마이너 버전 자동 업데이트 활성화 권장

### 4. 알림 설정

주요 이벤트에 대한 알림을 설정합니다:

- CPU 사용률 80% 이상
- 메모리 사용률 90% 이상
- 연결 수 임계값 초과
- 복제 지연 시간 증가

---

## 트러블슈팅

### 1. 연결 문제

#### 1.1 연결 타임아웃

**증상**: `Connection timeout` 오류

**해결 방법**:
- 보안 그룹 인바운드 규칙 확인
- VPC 라우팅 테이블 확인
- 네트워크 ACL 확인

#### 1.2 연결 수 부족

**증상**: `Too many connections` 오류

**해결 방법**:
- 연결 풀 크기 조정 (`maximum-pool-size` 감소)
- 인스턴스 크기 증가
- `max_connections` 파라미터 확인

### 2. 성능 이슈

#### 2.1 느린 쿼리

**해결 방법**:
- Performance Insights에서 Top SQL 확인
- 인덱스 사용 여부 확인 (`EXPLAIN` 사용)
- 쿼리 최적화 또는 인덱스 추가 (필요 시)

#### 2.2 높은 CPU 사용률

**해결 방법**:
- Performance Insights에서 CPU를 많이 사용하는 쿼리 확인
- 인스턴스 크기 증가
- 읽기 전용 복제본 추가 (읽기 부하 분산)

### 3. 복제 지연

**증상**: Reader에서 최신 데이터를 조회하지 못함

**해결 방법**:
- `AuroraReplicaLag` 메트릭 확인
- Reader 인스턴스 성능 확인
- 필요 시 Reader 인스턴스 크기 증가

### 4. 스토리지 문제

#### 4.1 스토리지 부족

**증상**: 스토리지 사용률 90% 이상

**해결 방법**:
- 오래된 데이터 정리 (Soft Delete된 데이터 영구 삭제)
- 불필요한 스냅샷 삭제
- Aurora는 자동 스케일링되지만 모니터링 필요

---

## 결론

이 가이드는 AWS Aurora MySQL 클러스터를 프로덕션 환경에 구축하고 프로젝트와 연동하는 실무 가이드를 제공합니다.

### 주요 특징

1. ✅ **실무 중심**: 실제 구축 가능한 단계별 가이드
2. ✅ **AWS Console & CLI 지원**: GUI와 CLI 모두 상세 가이드 제공
3. ✅ **비용 최적화**: 워크로드 분석 기반 인스턴스 크기 추천
4. ✅ **성능 최적화**: 읽기 전용 복제본 활용, 연결 풀 최적화
5. ✅ **모니터링**: CloudWatch 및 Performance Insights 활용
6. ✅ **프로젝트 통합**: 기존 설계서 및 구현 코드와 완전 일치

### 다음 단계

1. Aurora 클러스터 생성 및 스키마 구축
2. 환경변수 설정 및 연결 테스트
3. 모니터링 및 알림 설정
4. 성능 튜닝 및 비용 최적화

---

## 참고 자료

### AWS 공식 문서

#### Aurora MySQL 개요 및 시작하기
- [Aurora MySQL 개요](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.Overview.html)
- [Aurora MySQL 시작하기](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/CHAP_GettingStartedAurora.html)
- [Aurora MySQL 클러스터 생성](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Aurora.CreateInstance.html)

#### 네트워크 및 보안
- [VPC에서 Aurora 사용하기](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html)
- [DB 서브넷 그룹](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html#USER_VPC.Subnets)
- [보안 그룹](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/Overview.RDSDBSecurityGroups.html)

#### 백업 및 복구
- [Aurora 백업 및 복구](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/BackupRestoreAurora.html)
- [Point-in-Time Recovery](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_PIT.html)

#### 모니터링
- [Aurora 모니터링 개요](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/MonitoringOverview.html)
- [Performance Insights](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_PerfInsights.html)
- [CloudWatch 메트릭](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/monitoring-cloudwatch.html)

#### 쿼리 및 스키마 관리
- [Query Editor](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/query-editor.html)
- [RDS Data API](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/data-api.html)

#### AWS CLI 참조
- [AWS CLI RDS 명령어](https://docs.aws.amazon.com/cli/latest/reference/rds/)
- [create-db-cluster](https://docs.aws.amazon.com/cli/latest/reference/rds/create-db-cluster.html)
- [create-db-instance](https://docs.aws.amazon.com/cli/latest/reference/rds/create-db-instance.html)
- [modify-db-cluster](https://docs.aws.amazon.com/cli/latest/reference/rds/modify-db-cluster.html)

#### 비용 최적화
- [Aurora 비용 최적화](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/cost_optimization.html)
- [예약 인스턴스](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/USER_WorkingWithReservedDBInstances.html)

---

**문서 버전**: 1.1  
**최종 업데이트**: 2026-01-16  
**작성자**: Infrastructure Architect  
**주요 업데이트**: AWS Console (GUI) 및 AWS CLI 상세 가이드 추가
