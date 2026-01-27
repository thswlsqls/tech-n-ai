# MongoDB Atlas Cluster 클라우드 데이터소스 연동 설계서

**작성 일시**: 2026-01-16  
**대상**: 프로덕션 환경 구축  
**MongoDB 버전**: 7.0+  
**Atlas 버전**: latest  
**버전**: 1.2  

## 목차

1. [개요](#개요)
2. [클러스터 생성 및 구성](#클러스터-생성-및-구성)
   - Atlas UI (GUI) 가이드
   - Atlas CLI 가이드
3. [컬렉션 구축](#컬렉션-구축)
   - Atlas UI를 통한 컬렉션 및 인덱스 생성
   - MongoDB Shell을 통한 생성
4. [MongoDB Atlas Vector Search 구축](#mongodb-atlas-vector-search-구축)
   - Vector Search 개요
   - 벡터 필드 스키마 설계
   - Vector Search Index 생성
   - 임베딩 생성 및 저장
   - Vector Search 쿼리 실행
   - 성능 최적화
   - 비용 절감 전략
   - 개발/테스트 환경 예상 비용
5. [프로젝트 연동](#프로젝트-연동)
6. [성능 최적화](#성능-최적화)
7. [비용 절감 전략](#비용-절감-전략)
8. [모니터링 및 유지보수](#모니터링-및-유지보수)
   - Atlas UI를 통한 모니터링 및 알림 설정
   - Atlas CLI를 통한 설정
9. [트러블슈팅](#트러블슈팅)

---

## 개요

### 서비스 소개

MongoDB Atlas는 MongoDB의 완전 관리형 클라우드 데이터베이스 서비스로, 본 프로젝트에서는 CQRS 패턴의 Query Side(읽기 전용) 데이터 저장소로 사용됩니다.

### 아키텍처 개요

- **Replica Set 구성**: 고가용성 및 읽기 부하 분산
- **Read Preference**: `secondaryPreferred` (읽기 복제본 우선)
- **Write Concern**: `majority` (데이터 일관성 보장)
- **CQRS 동기화**: Aurora MySQL과 TSID 기반 동기화

### 참고 설계서

- **도큐먼트 설계**: `docs/step1/2. mongodb-schema-design.md`
- **베스트 프랙티스**: `docs/step1/mongodb-atlas-schema-design-best-practices.md`
- **CQRS 동기화**: `docs/step11/cqrs-kafka-sync-design.md`

### 공식 문서

이 가이드는 다음 MongoDB Atlas 공식 문서를 기반으로 작성되었습니다:
- [MongoDB Atlas 문서](https://www.mongodb.com/docs/atlas/)
- [Atlas 클러스터 생성 가이드](https://www.mongodb.com/docs/guides/atlas/cluster/)
- [Atlas CLI 문서](https://www.mongodb.com/docs/atlas/cli/current/)
- [MongoDB Manual](https://www.mongodb.com/docs/manual/)
- [MongoDB Java Driver](https://www.mongodb.com/docs/drivers/java/sync/current/)

자세한 참고 자료는 문서 하단의 [참고 자료](#참고-자료) 섹션을 참고하세요.

---

## 클러스터 생성 및 구성

### 1. MongoDB Atlas 클러스터 생성

**참고**: [MongoDB Atlas 공식 문서 - 클러스터 생성](https://www.mongodb.com/docs/guides/atlas/cluster/)

#### 1.1 Atlas UI (GUI)를 통한 생성

##### 1.1.1 Atlas Console 접속 및 프로젝트 설정

1. **MongoDB Atlas Console 접속**
   - https://cloud.mongodb.com 접속
   - 계정 로그인 또는 회원가입

2. **프로젝트 생성 또는 선택**
   - 왼쪽 상단 **Projects** 드롭다운 → **New Project** 클릭
   - **Project Name**: `Tech N AI` (또는 원하는 이름)
   - **Create Project** 클릭
   - 또는 기존 프로젝트 선택

##### 1.1.2 클러스터 생성 시작

1. **클러스터 생성 페이지 접속**
   - 왼쪽 사이드바 **Clusters** 메뉴 클릭
   - **Build a Database** 또는 **Create** 버튼 클릭

2. **클러스터 타입 선택**
   - **Free (M0)**: 개발/테스트용 (무료)
   - **Shared (M2/M5)**: 개발/테스트용 (저렴)
   - **Dedicated (M10 이상)**: 프로덕션용 (권장)
   - 프로덕션 환경에서는 **Dedicated** 선택

##### 1.1.3 클라우드 제공자 및 리전 선택

1. **Cloud Provider**
   - **AWS** 선택 (권장)
   - 또는 **Google Cloud**, **Azure** 선택 가능

2. **Region 선택**
   - **Region**: `Asia Pacific (Seoul) ap-northeast-2` 선택
   - 또는 애플리케이션과 가까운 리전 선택
   - **Multi-Region** 옵션: 고가용성 필요 시 선택 (추가 비용)

##### 1.1.4 클러스터 티어 선택

**워크로드 분석 기반 추천**:

- **개발/테스트**: M0 (Free Tier) 또는 M2 (Shared)
- **소규모 프로덕션**: M10 (2GB RAM, 10GB Storage)
- **중규모 프로덕션**: M20 (4GB RAM, 20GB Storage) 또는 M30 (8GB RAM, 40GB Storage)
- **대규모 프로덕션**: M40 이상

**주의**: 초기에는 M10으로 시작하고, 모니터링 후 필요 시 업그레이드

##### 1.1.5 클러스터 이름 및 추가 설정

1. **Cluster Name**
   - **Cluster Name**: `tech-n-ai-cluster`
   - 소문자, 숫자, 하이픈만 사용 가능

2. **MongoDB Version** (선택사항)
   - 최신 버전 권장 (예: `7.0.x`)
   - 특정 버전 필요 시 선택

3. **Additional Settings** (선택사항)
   - **Backup**: Cloud Backup 활성화 (M10 이상)
   - **BI Connector**: 비즈니스 인텔리전스 도구 연동 (선택사항)

##### 1.1.6 클러스터 생성 완료

1. **Create Cluster** 버튼 클릭
2. 생성 완료까지 약 3-5분 소요
3. 생성 완료 후 **Clusters** 페이지에서 상태 확인
   - **Status**: `Idle` (생성 완료)

#### 1.2 Atlas CLI를 통한 생성

**참고**: [MongoDB Atlas CLI 공식 문서](https://www.mongodb.com/docs/atlas/cli/current/)

##### 1.2.1 Atlas CLI 설치

```bash
# macOS (Homebrew)
brew install mongodb-atlas-cli

# Linux
curl -sSf https://fastdl.mongodb.org/mongocli/mongocli_1.32.0_linux_x86_64.tar.gz | tar -xz
sudo mv mongocli /usr/local/bin/

# Windows (Chocolatey)
choco install mongodb-atlas-cli

# 설치 확인
atlas --version
```

##### 1.2.2 Atlas CLI 인증

```bash
# Atlas CLI 로그인 (브라우저 인증)
atlas auth login

# 또는 API 키 사용
atlas config init
# API Public Key와 Private Key 입력
```

##### 1.2.3 프로젝트 설정

```bash
# 프로젝트 ID 확인
atlas projects list

# 프로젝트 ID 설정 (환경변수 또는 config 파일)
export ATLAS_PROJECT_ID=<your-project-id>

# 또는 config 파일에 저장
atlas config set project_id <your-project-id>
```

##### 1.2.4 클러스터 생성

```bash
# 기본 클러스터 생성
atlas clusters create tech-n-ai-cluster \
  --provider AWS \
  --region AP_NORTHEAST_2 \
  --tier M10 \
  --diskSizeGB 10 \
  --projectId <your-project-id>

# 상세 옵션 포함 클러스터 생성
atlas clusters create tech-n-ai-cluster \
  --provider AWS \
  --region AP_NORTHEAST_2 \
  --tier M10 \
  --diskSizeGB 10 \
  --mdbVersion 7.0 \
  --backup \
  --projectId <your-project-id>
```

**주요 파라미터 설명**:
- `--provider`: 클라우드 제공자 (`AWS`, `GCP`, `AZURE`)
- `--region`: 리전 코드 (예: `AP_NORTHEAST_2`는 서울)
- `--tier`: 클러스터 티어 (예: `M10`, `M20`, `M30`)
- `--diskSizeGB`: 디스크 크기 (GB)
- `--mdbVersion`: MongoDB 버전 (선택사항)
- `--backup`: Cloud Backup 활성화 (M10 이상)

##### 1.2.5 클러스터 상태 확인

```bash
# 클러스터 목록 확인
atlas clusters list --projectId <your-project-id>

# 특정 클러스터 상태 확인
atlas clusters describe tech-n-ai-cluster --projectId <your-project-id>

# 클러스터 생성 완료 대기
atlas clusters watch tech-n-ai-cluster --projectId <your-project-id>
```

**상태 확인**:
- `IDLE`: 클러스터 생성 완료
- `CREATING`: 생성 중
- `UPDATING`: 업데이트 중

### 2. 네트워크 구성

**참고**: [MongoDB Atlas 공식 문서 - 네트워크 연결](https://www.mongodb.com/docs/guides/atlas/network-connections/)

#### 2.1 IP 액세스 리스트 설정

##### 2.1.1 Atlas UI를 통한 IP 액세스 리스트 설정

1. **Network Access 메뉴 접속**
   - 왼쪽 사이드바 **Network Access** 클릭
   - 또는 **Security** → **Network Access** 클릭

2. **IP 주소 추가**
   - **Add IP Address** 버튼 클릭
   - 다음 중 하나 선택:
     - **Add My Current IP Address**: 현재 IP 주소 자동 추가 (개발 환경)
     - **Add IP Address**: 수동으로 IP 주소 또는 CIDR 블록 입력
       - 예: `203.0.113.0/24` (CIDR 블록)
       - 예: `203.0.113.5` (단일 IP)
   - **Comment**: 설명 입력 (선택사항)
     - 예: "Application Server", "Development Environment"
   - **Confirm** 버튼 클릭

3. **프로덕션 환경 설정**
   - 애플리케이션 서버 IP 주소 또는 CIDR 블록 추가
   - 여러 IP 주소를 개별적으로 추가 가능
   - **주의**: `0.0.0.0/0` (모든 IP 허용)은 개발 환경에서만 사용

4. **IP 주소 관리**
   - 기존 IP 주소 수정/삭제 가능
   - **Edit** 또는 **Delete** 버튼 사용

##### 2.1.2 Atlas CLI를 통한 IP 액세스 리스트 설정

```bash
# 현재 IP 주소 추가
atlas accessLists create \
  --ip $(curl -s https://api.ipify.org) \
  --comment "Current IP" \
  --projectId <your-project-id>

# 특정 IP 주소 추가
atlas accessLists create \
  --ip 203.0.113.5 \
  --comment "Application Server" \
  --projectId <your-project-id>

# CIDR 블록 추가
atlas accessLists create \
  --ip 203.0.113.0/24 \
  --comment "Application Server Network" \
  --projectId <your-project-id>

# IP 액세스 리스트 확인
atlas accessLists list --projectId <your-project-id>

# 특정 IP 주소 삭제
atlas accessLists delete <ip-address> --projectId <your-project-id>
```

#### 2.2 VPC Peering 설정 (프로덕션 권장)

**참고**: [MongoDB Atlas 공식 문서 - VPC Peering](https://www.mongodb.com/docs/atlas/security-vpc-peering/)

프로덕션 환경에서는 VPC Peering을 통해 네트워크 보안을 강화합니다.

##### 2.2.1 Atlas UI를 통한 VPC Peering 설정

1. **Network Access 메뉴 접속**
   - 왼쪽 사이드바 **Network Access** 클릭
   - **Peering** 탭 선택

2. **Peering Connection 생성**
   - **Create Peering Connection** 버튼 클릭
   - **Cloud Provider**: AWS 선택
   - **Region**: AWS VPC와 동일한 리전 선택
   - **VPC ID**: AWS VPC ID 입력
   - **Route Table CIDR Block**: VPC CIDR 블록 입력 (예: `10.0.0.0/16`)

3. **AWS VPC 라우팅 테이블 업데이트**
   - Atlas에서 제공하는 Peering Connection ID 확인
   - AWS Console → VPC → Route Tables 접속
   - 해당 VPC의 라우팅 테이블 선택 → **Routes** 탭 → **Edit routes**
   - **Add route** 클릭:
     - **Destination**: Atlas가 제공한 CIDR 블록 (예: `192.168.0.0/16`)
     - **Target**: Peering Connection 선택
   - **Save routes** 클릭

4. **Peering Connection 상태 확인**
   - Atlas UI에서 **Status** 확인
   - `Available`: 연결 완료

##### 2.2.2 Atlas CLI를 통한 VPC Peering 설정

```bash
# VPC Peering Connection 생성
atlas networking peering create \
  --atlasCidrBlock 192.168.0.0/16 \
  --awsAccountId <aws-account-id> \
  --region ap-northeast-2 \
  --routeTableCidrBlock 10.0.0.0/16 \
  --vpcId <vpc-id> \
  --projectId <your-project-id>

# VPC Peering 상태 확인
atlas networking peering list --projectId <your-project-id>

# VPC Peering 상세 정보 확인
atlas networking peering describe <peering-id> --projectId <your-project-id>
```

**주의**: VPC Peering 설정 후 AWS Console에서 라우팅 테이블을 수동으로 업데이트해야 합니다.

#### 2.3 Private Endpoint 설정 (프로덕션 권장)

**참고**: [MongoDB Atlas 공식 문서 - Private Endpoint](https://www.mongodb.com/docs/atlas/security-vpc-private-endpoint/)

M10 이상 클러스터에서 사용 가능합니다.

##### 2.3.1 Atlas UI를 통한 Private Endpoint 설정

1. **Network Access 메뉴 접속**
   - 왼쪽 사이드바 **Network Access** 클릭
   - **Private Endpoint** 탭 선택

2. **Private Endpoint 생성**
   - **Create Private Endpoint** 버튼 클릭
   - **Cloud Provider**: AWS 선택
   - **Region**: AWS VPC와 동일한 리전 선택
   - **VPC ID**: AWS VPC ID 선택
   - **Subnet IDs**: 서브넷 선택 (최소 2개 이상, 서로 다른 가용 영역)
   - **Security Group IDs**: 보안 그룹 선택 (선택사항)

3. **Private Endpoint 연결**
   - 생성된 Private Endpoint 선택
   - **Connect to Cluster** 버튼 클릭
   - 연결할 클러스터 선택
   - **Connect** 버튼 클릭

4. **AWS VPC 엔드포인트 확인**
   - AWS Console → VPC → Endpoints 접속
   - 생성된 VPC 엔드포인트 확인
   - **Status**: `available` 확인

##### 2.3.2 Atlas CLI를 통한 Private Endpoint 설정

```bash
# Private Endpoint 생성
atlas privateEndpoints aws create \
  --region ap-northeast-2 \
  --projectId <your-project-id>

# Private Endpoint 목록 확인
atlas privateEndpoints aws list --projectId <your-project-id>

# Private Endpoint에 클러스터 연결
atlas privateEndpoints aws describe <endpoint-id> \
  --projectId <your-project-id>

# 클러스터에 Private Endpoint 연결
atlas clusters connect <cluster-name> \
  --privateEndpointId <endpoint-id> \
  --projectId <your-project-id>
```

### 3. 데이터베이스 사용자 생성

**참고**: [MongoDB Atlas 공식 문서 - 데이터베이스 사용자 생성](https://www.mongodb.com/docs/atlas/tutorial/create-mongodb-user-for-cluster/)

##### 3.1 Atlas UI를 통한 데이터베이스 사용자 생성

1. **Database Access 메뉴 접속**
   - 왼쪽 사이드바 **Database Access** 클릭
   - 또는 **Security** → **Database Access** 클릭

2. **새 사용자 추가**
   - **Add New Database User** 버튼 클릭

3. **인증 방법 선택**
   - **Password**: 비밀번호 기반 인증 (권장)
   - **Certificate**: X.509 인증서 기반 인증 (고급)

4. **사용자 정보 입력** (Password 방식)
   - **Username**: `tech-n-ai-user`
   - **Password**: 
     - **Autogenerate Secure Password**: 자동 생성 (권장)
     - **Custom Password**: 직접 입력 (강력한 비밀번호 필수)
   - **Database User Privileges**: 
     - **Atlas admin**: 모든 데이터베이스에 대한 전체 권한 (개발 환경)
     - **Read and write to any database**: 모든 데이터베이스 읽기/쓰기 권한
     - **Read any database**: 모든 데이터베이스 읽기 권한
     - **Custom Role**: 커스텀 역할 지정 (고급)

5. **사용자 생성 완료**
   - **Add User** 버튼 클릭
   - 자동 생성된 비밀번호는 반드시 복사하여 안전하게 보관

##### 3.2 Atlas CLI를 통한 데이터베이스 사용자 생성

```bash
# 비밀번호 기반 사용자 생성
atlas dbusers create \
  --username tech-n-ai-user \
  --password 'YourSecurePassword123!' \
  --role atlasAdmin \
  --projectId <your-project-id>

# 특정 데이터베이스에 대한 읽기/쓰기 권한 부여
atlas dbusers create \
  --username tech-n-ai-user \
  --password 'YourSecurePassword123!' \
  --role readWrite@tech_n_ai \
  --projectId <your-project-id>

# 여러 역할 부여
atlas dbusers create \
  --username tech-n-ai-user \
  --password 'YourSecurePassword123!' \
  --role readWrite@tech_n_ai,read@admin \
  --projectId <your-project-id>

# 사용자 목록 확인
atlas dbusers list --projectId <your-project-id>

# 사용자 정보 확인
atlas dbusers describe tech-n-ai-user --projectId <your-project-id>

# 사용자 비밀번호 변경
atlas dbusers update tech-n-ai-user \
  --password 'NewSecurePassword123!' \
  --projectId <your-project-id>
```

**주요 역할 (Roles)**:
- `atlasAdmin`: Atlas 관리자 권한
- `readWrite@<database>`: 특정 데이터베이스 읽기/쓰기 권한
- `read@<database>`: 특정 데이터베이스 읽기 권한
- `dbAdmin@<database>`: 특정 데이터베이스 관리 권한

### 4. 연결 문자열 확인

**참고**: [MongoDB Atlas 공식 문서 - 클러스터 연결](https://www.mongodb.com/docs/atlas/tutorial/connect-to-your-cluster/)

##### 4.1 Atlas UI를 통한 연결 문자열 확인

1. **클러스터 연결 페이지 접속**
   - **Clusters** 페이지에서 클러스터 선택
   - **Connect** 버튼 클릭

2. **연결 방식 선택**
   - **Connect your application**: 애플리케이션 연결 (권장)
   - **Connect using MongoDB Compass**: Compass GUI 도구 연결
   - **Connect using MongoDB Shell**: mongosh 명령줄 도구 연결
   - **Connect using VS Code**: VS Code 확장 프로그램 연결

3. **애플리케이션 연결 설정**
   - **Driver**: `Java` 선택
   - **Version**: `4.11 or later` 선택
   - **Connection String Only**: 연결 문자열만 표시

4. **연결 문자열 복사**
   ```
   mongodb+srv://<username>:<password>@tech-n-ai-cluster.xxxxx.mongodb.net/?retryWrites=true&w=majority
   ```
   - `<username>`: 데이터베이스 사용자 이름
   - `<password>`: 데이터베이스 사용자 비밀번호
   - `<cluster-endpoint>`: 클러스터 엔드포인트

5. **연결 문자열 커스터마이징** (선택사항)
   - **Add your connection string into your application code** 섹션에서:
     - 데이터베이스 이름 추가: `mongodb+srv://...@...mongodb.net/tech_n_ai?...`
     - 추가 옵션:
       - `readPreference=secondaryPreferred`: 읽기 복제본 우선
       - `ssl=true`: SSL/TLS 연결 필수

##### 4.2 Atlas CLI를 통한 연결 문자열 확인

```bash
# 연결 문자열 확인 (SRV)
atlas clusters connectionStrings describe tech-n-ai-cluster \
  --projectId <your-project-id>

# 연결 문자열 확인 (Standard)
atlas clusters connectionStrings describe tech-n-ai-cluster \
  --type standard \
  --projectId <your-project-id>

# 연결 문자열 확인 (Private Endpoint)
atlas clusters connectionStrings describe tech-n-ai-cluster \
  --type private \
  --projectId <your-project-id>
```

**연결 문자열 구성 요소**:
- `mongodb+srv://`: SRV 연결 문자열 (DNS 기반)
- `<username>:<password>`: 데이터베이스 사용자 인증 정보
- `<cluster-endpoint>`: 클러스터 엔드포인트
- `<database>`: 데이터베이스 이름 (선택사항)
- `retryWrites=true`: 쓰기 재시도 활성화
- `w=majority`: Write Concern (대다수 노드 확인)
- `readPreference=secondaryPreferred`: 읽기 복제본 우선
- `ssl=true`: SSL/TLS 연결 필수

### 5. 백업 및 복구 전략

**참고**: [MongoDB Atlas 공식 문서 - 백업 및 복구](https://www.mongodb.com/docs/atlas/backup-restore-cluster/)

**주의**: Cloud Backup은 M10 이상 클러스터에서만 사용 가능합니다.

#### 5.1 자동 백업 설정

##### 5.1.1 Atlas UI를 통한 백업 설정

1. **Backup 메뉴 접속**
   - 왼쪽 사이드바 **Backup** 클릭
   - 또는 클러스터 선택 → **Backup** 탭 클릭

2. **Cloud Backup 활성화**
   - **Edit Configuration** 버튼 클릭
   - **Cloud Backup** 토글 활성화 (M10 이상 클러스터)

3. **백업 스케줄 설정**
   - **Backup Schedule**: 
     - **Daily**: 매일 백업 (권장)
     - **Weekly**: 주간 백업
     - **On Demand**: 수동 백업만
   - **Backup Time**: 백업 실행 시간 선택 (예: `03:00 UTC`)

4. **백업 보존 기간 설정**
   - **Retention**: `7 days` (프로덕션 권장)
   - 또는 `30 days`, `60 days` 선택 가능

5. **Point-in-Time Recovery 활성화**
   - **Point-in-Time Recovery**: 활성화 (M10 이상)
   - 특정 시점으로 데이터 복구 가능

6. **설정 저장**
   - **Save Changes** 버튼 클릭

##### 5.1.2 Atlas CLI를 통한 백업 설정

```bash
# Cloud Backup 활성화 (클러스터 생성 시)
atlas clusters create tech-n-ai-cluster \
  --backup \
  --projectId <your-project-id>

# 기존 클러스터에 Cloud Backup 활성화
atlas backups schedule update \
  --clusterName tech-n-ai-cluster \
  --enable \
  --daily \
  --retentionDays 7 \
  --projectId <your-project-id>

# 백업 스케줄 확인
atlas backups schedule describe tech-n-ai-cluster \
  --projectId <your-project-id>
```

#### 5.2 스냅샷 관리

##### 5.2.1 Atlas UI를 통한 스냅샷 관리

1. **수동 스냅샷 생성**
   - **Backup** 메뉴 → **Snapshots** 탭
   - **Take Snapshot** 버튼 클릭
   - **Snapshot Name**: `tech-n-ai-snapshot-YYYYMMDD` (선택사항)
   - **Take Snapshot** 버튼 클릭

2. **스냅샷 목록 확인**
   - **Snapshots** 탭에서 생성된 스냅샷 목록 확인
   - **Status**: `Completed` (생성 완료)

3. **스냅샷으로부터 복구**
   - 복구할 스냅샷 선택
   - **Restore** 버튼 클릭
   - **Restore to**: 
     - **New Cluster**: 새 클러스터로 복구
     - **Existing Cluster**: 기존 클러스터로 복구
   - 복구 설정 입력 후 **Restore** 버튼 클릭

4. **스냅샷 삭제**
   - 삭제할 스냅샷 선택
   - **Delete** 버튼 클릭
   - 확인 후 삭제

##### 5.2.2 Atlas CLI를 통한 스냅샷 관리

```bash
# 수동 스냅샷 생성
atlas backups snapshots create \
  --clusterName tech-n-ai-cluster \
  --desc "Manual snapshot before deployment" \
  --projectId <your-project-id>

# 스냅샷 목록 확인
atlas backups snapshots list tech-n-ai-cluster \
  --projectId <your-project-id>

# 스냅샷 상세 정보 확인
atlas backups snapshots describe <snapshot-id> \
  --clusterName tech-n-ai-cluster \
  --projectId <your-project-id>

# 스냅샷으로부터 새 클러스터 복구
atlas backups restores start \
  --clusterName tech-n-ai-cluster \
  --snapshotId <snapshot-id> \
  --targetClusterName tech-n-ai-cluster-restored \
  --projectId <your-project-id>

# 스냅샷 삭제
atlas backups snapshots delete <snapshot-id> \
  --clusterName tech-n-ai-cluster \
  --projectId <your-project-id>
```

### 6. 모니터링 및 알림 설정

**참고**: [MongoDB Atlas 공식 문서 - 모니터링](https://www.mongodb.com/docs/atlas/monitoring/)

#### 6.1 Atlas Monitoring

Atlas는 자동으로 다음 메트릭을 모니터링합니다:

- **Connections**: 연결 수
- **Operations**: 작업 카운터 (읽기/쓰기)
- **Query Targeting**: 쿼리 타겟팅 효율성
- **System CPU/Memory**: 시스템 리소스 사용률

##### 6.1.1 Atlas UI를 통한 모니터링 확인

1. **Metrics 메뉴 접속**
   - 클러스터 선택 → **Metrics** 탭 클릭
   - 또는 왼쪽 사이드바 **Metrics** 클릭

2. **주요 메트릭 확인**
   - **Connections**: 현재 연결 수 및 최대 연결 수
   - **Operations**: 초당 작업 수 (OPS)
   - **Query Targeting**: 쿼리 타겟팅 효율성 (100% 목표)
   - **System Metrics**: CPU, Memory, Disk 사용률

3. **Performance Advisor 확인**
   - **Performance Advisor** 탭 클릭
   - **Slow Queries**: 느린 쿼리 목록
   - **Suggested Indexes**: 권장 인덱스 제안

#### 6.2 알림 설정

##### 6.2.1 Atlas UI를 통한 알림 설정

1. **Alerts 메뉴 접속**
   - 왼쪽 사이드바 **Alerts** 클릭
   - 또는 **Monitoring** → **Alerts** 클릭

2. **알림 생성**
   - **Add Alert** 버튼 클릭
   - **Alert Type** 선택:
     - **Connection Count**: 연결 수 임계값
     - **CPU Utilization**: CPU 사용률
     - **Memory Utilization**: 메모리 사용률
     - **Disk Space**: 디스크 사용률
     - **Replication Lag**: 복제 지연 시간

3. **알림 조건 설정**
   - **Metric**: 알림 메트릭 선택
   - **Threshold**: 임계값 설정
     - 예: CPU Utilization `80%` 이상
   - **Condition**: 조건 설정
     - 예: `Greater than` 또는 `Less than`

4. **알림 수신자 설정**
   - **Notifications**: 알림 수신 방법 선택
     - **Email**: 이메일 알림
     - **SMS**: SMS 알림 (선택사항)
     - **Slack**: Slack 알림 (선택사항)
   - **Email Addresses**: 이메일 주소 입력

5. **알림 저장**
   - **Save** 버튼 클릭

##### 6.2.2 Atlas CLI를 통한 알림 설정

```bash
# 알림 설정 확인
atlas alerts list --projectId <your-project-id>

# 알림 설정 생성 (예: CPU 사용률)
atlas alerts config create \
  --event CPU_UTILIZATION \
  --enabled \
  --notificationType EMAIL \
  --notificationEmailAddress your-email@example.com \
  --matcherFieldName TYPE_NAME \
  --matcherOperator EQUALS \
  --matcherValue REPLICA_SET \
  --metricThresholdMetricName CPU_UTILIZATION \
  --metricThresholdOperator GREATER_THAN \
  --metricThresholdThreshold 80 \
  --metricThresholdUnits PERCENT \
  --projectId <your-project-id>

# 알림 설정 업데이트
atlas alerts config update <alert-config-id> \
  --enabled \
  --projectId <your-project-id>

# 알림 설정 삭제
atlas alerts config delete <alert-config-id> \
  --projectId <your-project-id>
```

---

## 컬렉션 구축

**참고**: [MongoDB Atlas 공식 문서 - 데이터베이스 및 컬렉션](https://www.mongodb.com/docs/atlas/databases-collections/)

### 1. 데이터베이스 생성

MongoDB Atlas는 첫 도큐먼트 삽입 시 자동으로 데이터베이스와 컬렉션을 생성합니다. 

프로젝트에서 사용하는 데이터베이스 이름: `tech_n_ai`

#### 1.1 Atlas UI를 통한 데이터베이스 확인

1. **Collections 메뉴 접속**
   - 왼쪽 사이드바 **Collections** 클릭
   - 또는 클러스터 선택 → **Collections** 탭 클릭

2. **데이터베이스 확인**
   - 데이터베이스 목록에서 `tech_n_ai` 확인
   - 데이터베이스가 없으면 첫 도큐먼트 삽입 시 자동 생성됨

#### 1.2 MongoDB Shell을 통한 데이터베이스 생성

```javascript
// MongoDB Shell 연결
mongosh "mongodb+srv://tech-n-ai-cluster.xxxxx.mongodb.net/tech_n_ai" \
  --username tech-n-ai-user \
  --password your-password

// 데이터베이스 선택 (없으면 자동 생성)
use tech_n_ai

// 데이터베이스 목록 확인
show dbs
```

### 2. 컬렉션 생성

컬렉션은 애플리케이션에서 첫 도큐먼트 삽입 시 자동으로 생성됩니다. 

**주요 컬렉션**:
- `sources`: 정보 출처
- `contests`: 개발자 대회 정보
- `news_articles`: IT 테크 뉴스 기사
- `archives`: 사용자 아카이브
- `user_profiles`: 사용자 프로필
- `exception_logs`: 예외 로그
- `conversation_sessions`: 대화 세션
- `conversation_messages`: 대화 메시지

#### 2.1 Atlas UI를 통한 컬렉션 확인

1. **Collections 메뉴 접속**
   - 왼쪽 사이드바 **Collections** 클릭
   - 데이터베이스 선택: `tech_n_ai`

2. **컬렉션 목록 확인**
   - 컬렉션 목록에서 생성된 컬렉션 확인
   - 컬렉션이 없으면 첫 도큐먼트 삽입 시 자동 생성됨

3. **컬렉션 수동 생성** (선택사항)
   - **Create Collection** 버튼 클릭
   - **Collection Name**: 컬렉션 이름 입력 (예: `sources`)
   - **Database Name**: `tech_n_ai`
   - **Collection Options**: 
     - **Capped Collection**: 크기 제한 컬렉션 (선택사항)
     - **Time Series Collection**: 시계열 컬렉션 (선택사항)
   - **Create** 버튼 클릭

4. **컬렉션 데이터 확인**
   - 컬렉션 선택 → **Documents** 탭
   - 도큐먼트 목록 및 상세 정보 확인

### 3. 인덱스 생성

**참고**: [MongoDB Atlas 공식 문서 - 인덱스](https://www.mongodb.com/docs/atlas/indexes/)

인덱스는 `MongoIndexConfig` 클래스에서 애플리케이션 시작 시 자동으로 생성됩니다.

#### 3.1 자동 인덱스 생성

`domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/MongoIndexConfig.java` 파일이 애플리케이션 시작 시 다음 인덱스를 자동 생성합니다:

- **sources**: `category + priority`, `type + enabled`, `priority`
- **contests**: `source_id + start_date`, `source_id`, `end_date`, `status + start_date`
- **news_articles**: `source_id + published_at`, `source_id`, `published_at` (TTL)
- **archives**: `user_id + created_at`, `user_id + item_type + created_at`, `user_id + item_type + item_id` (UNIQUE), 등
- **exception_logs**: `source + occurred_at`, `exception_type + occurred_at`, `occurred_at` (TTL)

#### 3.2 Atlas UI를 통한 인덱스 생성

1. **Collections 메뉴 접속**
   - 왼쪽 사이드바 **Collections** 클릭
   - 데이터베이스 선택: `tech_n_ai`
   - 컬렉션 선택 (예: `sources`)

2. **인덱스 생성**
   - **Indexes** 탭 클릭
   - **Create Index** 버튼 클릭

3. **인덱스 정의 입력**
   - **Index Name**: 인덱스 이름 (선택사항, 자동 생성 가능)
   - **Index Fields**: 인덱스 필드 추가
     - **Field Name**: 필드 이름 (예: `category`)
     - **Type**: 필드 타입 (예: `1` = 오름차순, `-1` = 내림차순)
     - **Add Field** 버튼으로 추가 필드 추가
   - **Index Options**:
     - **Unique**: UNIQUE 인덱스 (중복 방지)
     - **Sparse**: Sparse 인덱스 (null 값 제외)
     - **TTL**: TTL 인덱스 (자동 삭제)
       - **Expire After Seconds**: 만료 시간 (초)

4. **인덱스 생성 완료**
   - **Create Index** 버튼 클릭
   - 인덱스 생성 완료까지 대기

5. **인덱스 목록 확인**
   - **Indexes** 탭에서 생성된 인덱스 목록 확인
   - 인덱스 사용 통계 확인 (선택사항)

#### 3.3 MongoDB Shell을 통한 인덱스 생성

Atlas UI 또는 MongoDB Shell을 통해 수동으로 인덱스를 생성할 수도 있습니다:

```javascript
// MongoDB Shell 예제
use tech_n_ai

// sources 컬렉션 인덱스 생성
db.sources.createIndex({ category: 1, priority: 1 })
db.sources.createIndex({ type: 1, enabled: 1 })
db.sources.createIndex({ priority: 1 })
db.sources.createIndex({ url: 1 })

// contests 컬렉션 인덱스 생성
db.contests.createIndex({ source_id: 1, start_date: -1 })
db.contests.createIndex({ source_id: 1 })
db.contests.createIndex({ end_date: 1 })
db.contests.createIndex({ status: 1, start_date: -1 })

// news_articles 컬렉션 인덱스 생성 (TTL 인덱스)
db.news_articles.createIndex({ source_id: 1, published_at: -1 })
db.news_articles.createIndex({ source_id: 1 })
db.news_articles.createIndex({ published_at: 1 }, { expireAfterSeconds: 7776000 }) // 90일

// archives 컬렉션 인덱스 생성
db.archives.createIndex({ archive_tsid: 1 }, { unique: true })
db.archives.createIndex({ user_id: 1, created_at: -1 })
db.archives.createIndex({ user_id: 1, item_type: 1, created_at: -1 })
db.archives.createIndex({ user_id: 1, item_type: 1, item_id: 1 }, { unique: true })
db.archives.createIndex({ item_id: 1 })

// exception_logs 컬렉션 인덱스 생성 (TTL 인덱스)
db.exception_logs.createIndex({ source: 1, occurred_at: -1 })
db.exception_logs.createIndex({ exception_type: 1, occurred_at: -1 })
db.exception_logs.createIndex({ occurred_at: 1 }, { expireAfterSeconds: 7776000 }) // 90일
```

#### 3.4 인덱스 생성 확인

##### 3.4.1 Atlas UI를 통한 인덱스 확인

1. **Collections 메뉴 접속**
   - 컬렉션 선택 → **Indexes** 탭 클릭
   - 생성된 인덱스 목록 확인
   - 각 인덱스의 상세 정보 확인:
     - **Index Name**: 인덱스 이름
     - **Index Fields**: 인덱스 필드 및 정렬 방향
     - **Index Type**: 인덱스 타입 (Standard, Unique, TTL 등)
     - **Index Size**: 인덱스 크기

2. **Performance Advisor 확인**
   - **Performance Advisor** 탭 클릭
   - **Suggested Indexes**: 권장 인덱스 확인
   - 권장 인덱스 생성 가능

##### 3.4.2 MongoDB Shell을 통한 인덱스 확인

```javascript
// 인덱스 목록 확인
db.sources.getIndexes()
db.contests.getIndexes()
db.news_articles.getIndexes()
db.archives.getIndexes()
db.exception_logs.getIndexes()

// 인덱스 사용 통계 확인
db.sources.aggregate([{ $indexStats: {} }])
```

### 4. 샘플 데이터 삽입 (선택사항)

테스트를 위해 샘플 데이터를 삽입할 수 있습니다. 샘플 데이터는 `docs/step1/2. mongodb-schema-design.md`의 **샘플 데이터** 섹션을 참고합니다.

---

## MongoDB Atlas Vector Search 구축

**참고**: [MongoDB Atlas Vector Search 공식 문서](https://www.mongodb.com/docs/atlas/atlas-vector-search/)

### 1. Vector Search 개요

#### 1.1 Vector Search란

MongoDB Atlas Vector Search는 벡터 유사도 검색을 위한 기능으로, 임베딩 벡터를 사용하여 의미적으로 유사한 문서를 찾을 수 있습니다. 이 기능은 RAG(Retrieval-Augmented Generation) 기반 챗봇에서 핵심적인 역할을 합니다.

**주요 특징**:
- **벡터 유사도 검색**: 코사인 유사도, 유클리드 거리, 내적 등을 사용한 유사도 계산
- **메타데이터 필터링**: 벡터 검색과 함께 일반 필드 필터링 지원
- **고성능**: Atlas 클러스터 리소스를 활용한 빠른 검색
- **통합 관리**: MongoDB Atlas와 완전 통합된 관리

#### 1.2 RAG 활용

본 프로젝트에서는 RAG 기반 챗봇에서 Vector Search를 활용합니다:

- **ContestDocument**: 개발자 대회 정보 검색
- **NewsArticleDocument**: IT 테크 뉴스 기사 검색
- **ArchiveDocument**: 사용자 아카이브 항목 검색

#### 1.3 임베딩 모델

본 프로젝트는 **OpenAI text-embedding-3-small** 모델을 사용합니다:

- **차원 수**: 1536 dimensions (기본값)
- **비용**: $0.02 per 1M tokens
- **속도**: 빠른 응답 속도
- **통합성**: LLM Provider(OpenAI GPT-4o-mini)와 동일 Provider 사용

**참고**: RAG 챗봇 설계서는 `docs/step12/rag-chatbot-design.md`를 참고하세요.

### 2. 벡터 필드 스키마 설계

#### 2.1 Document 클래스 구조

본 프로젝트의 Document 클래스는 다음과 같이 벡터 필드를 포함합니다:

**ContestDocument** (`domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/ContestDocument.java`):
```java
@Field("embedding_text")
private String embeddingText;  // 임베딩 대상 텍스트

@Field("embedding_vector")
private List<Float> embeddingVector;  // 벡터 필드 (1536차원)
```

**NewsArticleDocument** (`domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/NewsArticleDocument.java`):
```java
@Field("embedding_text")
private String embeddingText;  // 임베딩 대상 텍스트

@Field("embedding_vector")
private List<Float> embeddingVector;  // 벡터 필드 (1536차원)
```

**ArchiveDocument** (`domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/document/ArchiveDocument.java`):
```java
@Field("embedding_text")
private String embeddingText;  // 임베딩 대상 텍스트

@Field("embedding_vector")
private List<Float> embeddingVector;  // 벡터 필드 (1536차원)
```

#### 2.2 임베딩 텍스트 생성 규칙

각 컬렉션별로 임베딩 대상 텍스트를 생성하는 규칙이 다릅니다:

**ContestDocument**:
- **규칙**: `title + description + metadata.tags`
- **예시**: "Codeforces Round 900 Regular Codeforces contest algorithm competitive-programming"

**NewsArticleDocument**:
- **규칙**: `title + summary + content` (content는 최대 2000자 제한)
- **예시**: "Spring Boot 4.0 Released Spring Boot 4.0 brings significant improvements... Spring Boot 4.0 has been released with new features..."

**ArchiveDocument**:
- **규칙**: `itemTitle + itemSummary + tag + memo`
- **예시**: "Codeforces Round 900 Regular Codeforces contest algorithm 참가 예정"

#### 2.3 벡터 차원

- **차원 수**: 1536 (OpenAI text-embedding-3-small 기본값)
- **타입**: `List<Float>` (Java), MongoDB에서는 배열로 저장
- **주의**: Vector Index의 `numDimensions`와 반드시 일치해야 함

### 3. Vector Search Index 생성

**참고**: [MongoDB Atlas Vector Search Index 생성 가이드](https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/)

#### 3.1 Atlas UI (GUI)를 통한 Index 생성

##### 3.1.1 Atlas UI 접속 및 네비게이션

1. **Collections 메뉴 접속**
   - 왼쪽 사이드바 **Collections** 클릭
   - 데이터베이스 선택: `tech_n_ai`
   - 컬렉션 선택 (예: `contests`)

2. **Search 탭 접속**
   - 컬렉션 선택 후 **Search** 탭 클릭
   - 또는 왼쪽 사이드바 **Atlas Search** 메뉴 직접 접속

##### 3.1.2 Vector Search Index 생성

1. **Create Search Index 버튼 클릭**
   - **Search** 탭에서 **Create Search Index** 버튼 클릭

2. **Index Type 선택**
   - **Index Type**: **Vector Search** 선택

3. **Index Name 입력**
   - **Index Name**: `vector_index_contests` (또는 원하는 이름)
   - 각 컬렉션별로 고유한 이름 사용 권장

4. **Index Definition JSON 입력**

   **ContestDocument Vector Index**:
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

   **NewsArticleDocument Vector Index**:
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
         "path": "published_at"
       }
     ]
   }
   ```

   **ArchiveDocument Vector Index**:
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

5. **Index 생성 완료**
   - **Create** 버튼 클릭
   - Index 생성 완료까지 대기 (보통 몇 분 소요)

##### 3.1.3 Index 상태 확인

1. **Index Status 확인**
   - **Search** 탭에서 생성된 Index 목록 확인
   - **Status**: `Active` (생성 완료)
   - **Status**: `Build` (생성 중)

2. **Index 통계 확인**
   - Index 선택 → **Statistics** 탭
   - Index 크기, 문서 수 등 확인

#### 3.2 Atlas CLI를 통한 Index 생성

**참고**: [MongoDB Atlas CLI 공식 문서](https://www.mongodb.com/docs/atlas/cli/current/)

##### 3.2.1 Atlas CLI 설치 및 인증

Atlas CLI 설치 및 인증은 **"클러스터 생성 및 구성"** 섹션의 **"1.2 Atlas CLI를 통한 생성"**을 참고하세요.

##### 3.2.2 Vector Search Index 생성 명령어

**ContestDocument Vector Index 생성**:
```bash
atlas clusters search indexes create \
  --clusterName tech-n-ai-cluster \
  --collectionName contests \
  --databaseName tech_n_ai \
  --name vector_index_contests \
  --definition '{
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
  }' \
  --projectId <your-project-id>
```

**NewsArticleDocument Vector Index 생성**:
```bash
atlas clusters search indexes create \
  --clusterName tech-n-ai-cluster \
  --collectionName news_articles \
  --databaseName tech_n_ai \
  --name vector_index_news_articles \
  --definition '{
    "fields": [
      {
        "type": "vector",
        "path": "embedding_vector",
        "numDimensions": 1536,
        "similarity": "cosine"
      },
      {
        "type": "filter",
        "path": "published_at"
      }
    ]
  }' \
  --projectId <your-project-id>
```

**ArchiveDocument Vector Index 생성**:
```bash
atlas clusters search indexes create \
  --clusterName tech-n-ai-cluster \
  --collectionName archives \
  --databaseName tech_n_ai \
  --name vector_index_archives \
  --definition '{
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
  }' \
  --projectId <your-project-id>
```

##### 3.2.3 Index 상태 확인

```bash
# Index 목록 확인
atlas clusters search indexes list \
  --clusterName tech-n-ai-cluster \
  --projectId <your-project-id>

# 특정 Index 상태 확인
atlas clusters search indexes describe <index-id> \
  --clusterName tech-n-ai-cluster \
  --projectId <your-project-id>
```

**Index 상태**:
- `IDLE`: Index 생성 완료
- `BUILDING`: Index 생성 중
- `FAILED`: Index 생성 실패

### 4. 임베딩 생성 및 저장

#### 4.1 임베딩 생성 전략

**애플리케이션 레벨**:
- langchain4j의 `EmbeddingModel` 사용 (OpenAI text-embedding-3-small)
- 새 도큐먼트 저장 시 자동 임베딩 생성

**배치 작업**:
- 기존 도큐먼트에 대한 임베딩 생성 배치 작업
- `embedding_text` 필드가 비어있는 도큐먼트 대상

#### 4.2 임베딩 저장

**필드 구조**:
- `embedding_text`: 임베딩 대상 텍스트 저장 (디버깅 및 검증용)
- `embedding_vector`: 1536차원 벡터 배열 저장 (`List<Float>`)

**주의사항**:
- OpenAI text-embedding-3-small은 document/query 구분 없이 동일한 모델 사용
- 벡터 차원은 반드시 Index의 `numDimensions`와 일치해야 함 (1536)
- 임베딩 생성 비용: $0.02 per 1M tokens (OpenAI API 비용)

### 5. Vector Search 쿼리 실행

**참고**: [MongoDB Atlas Vector Search $vectorSearch aggregation](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)

#### 5.1 Atlas UI를 통한 테스트

##### 5.1.1 Aggregation Pipeline 실행

1. **Collections 메뉴 접속**
   - 컬렉션 선택 (예: `contests`)

2. **Aggregations 탭 접속**
   - **Aggregations** 탭 클릭

3. **$vectorSearch Stage 추가**
   - **Add Stage** 버튼 클릭
   - **$vectorSearch** 선택

4. **$vectorSearch Stage 설정**
   ```javascript
   {
     index: "vector_index_contests",
     path: "embedding_vector",
     queryVector: [/* 1536차원 벡터 배열 */],
     numCandidates: 100,
     limit: 5,
     filter: {
       status: { $in: ["UPCOMING", "ONGOING"] }
     }
   }
   ```

5. **$project Stage 추가** (선택사항)
   - 유사도 점수 포함:
   ```javascript
   {
     _id: 1,
     title: 1,
     description: 1,
     score: { $meta: "vectorSearchScore" }
   }
   ```

6. **$match Stage 추가** (선택사항)
   - 유사도 점수 필터링:
   ```javascript
   {
     score: { $gte: 0.7 }
   }
   ```

7. **Run 실행**
   - **Run** 버튼 클릭하여 결과 확인

#### 5.2 애플리케이션 코드 통합

**Spring Data MongoDB의 `MongoTemplate` 사용**:

```java
// VectorSearchServiceImpl.java 예제
List<Float> queryVector = embeddingModel.embed(query).content().vectorAsList();

Aggregation aggregation = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("status").in("UPCOMING", "ONGOING")),
    // $vectorSearch는 MongoTemplate.executeCommand()로 직접 실행 필요
    // 또는 Spring Data MongoDB 4.5.0+의 VectorSearchOperation 사용
    Aggregation.project("_id", "title", "description")
        .and("vectorSearchScore").as("score")
);

List<ContestDocument> results = mongoTemplate.aggregate(
    aggregation, 
    "contests", 
    ContestDocument.class
).getMappedResults();
```

**$vectorSearch aggregation 예제**:
```javascript
[
  {
    $vectorSearch: {
      index: "vector_index_contests",
      path: "embedding_vector",
      queryVector: [/* 1536차원 벡터 배열 */],
      numCandidates: 100,
      limit: 5,
      filter: {
        status: { $in: ["UPCOMING", "ONGOING"] }
      }
    }
  },
  {
    $project: {
      _id: 1,
      title: 1,
      description: 1,
      score: { $meta: "vectorSearchScore" }
    }
  },
  {
    $match: {
      score: { $gte: 0.7 }
    }
  }
]
```

**참고**: 실제 구현은 `api/chatbot/src/main/java/com/ebson/shrimp/tm/demo/api/chatbot/service/VectorSearchServiceImpl.java`를 참고하세요.

### 6. 성능 최적화

**참고**: [MongoDB Atlas Vector Search 성능 최적화](https://www.mongodb.com/docs/atlas/atlas-vector-search/performance/)

#### 6.1 Index 파라미터 최적화

**numCandidates**:
- **설명**: 검색 정확도와 성능의 균형을 조절하는 파라미터
- **기본값**: 100
- **권장값**: 50-200 (데이터 크기에 따라 조정)
- **주의**: 값이 클수록 정확도는 높아지지만 성능은 저하

**similarity**:
- **설명**: 유사도 계산 방법
- **값**: `cosine` (텍스트 검색에 적합, 권장)
- **대안**: `euclidean`, `dotProduct`

**numDimensions**:
- **설명**: 벡터 차원 수
- **값**: 1536 (OpenAI text-embedding-3-small 기준, 변경 불가)
- **주의**: Index의 `numDimensions`와 도큐먼트의 벡터 차원이 반드시 일치해야 함

#### 6.2 검색 쿼리 최적화

**검색 결과 수 제한**:
- **기본값**: 5개
- **최대값**: 10개 (성능 고려)
- **권장**: RAG 챗봇에서는 5개 이하 권장

**유사도 임계값 설정**:
- **기본값**: 0.7
- **설명**: 유사도 점수가 0.7 이상인 결과만 반환
- **조정**: 데이터 품질에 따라 0.6-0.8 범위에서 조정

**필터 조건 활용**:
- **ContestDocument**: `status` 필터 (예: `UPCOMING`, `ONGOING`)
- **NewsArticleDocument**: `published_at` 필터 (예: 최근 30일)
- **ArchiveDocument**: `user_id` 필터 (사용자별 검색)

#### 6.3 리소스 관리

**클러스터 리소스 공유**:
- Vector Search는 클러스터 리소스를 공유
- 일반 쿼리와 Vector Search 쿼리가 동일한 리소스 사용

**클러스터 티어 권장**:
- **개발/테스트**: M0 (Free Tier) 또는 M10
- **프로덕션**: M10 이상 (워크로드에 따라 M20, M30 권장)

**Search Node** (고급 기능):
- 독립적인 스케일링 가능
- Vector Search 전용 리소스 할당
- 대규모 Vector Search 워크로드에 적합

### 7. 비용 절감 전략

#### 7.1 클러스터 티어 선택

**개발/테스트 환경**:
- **M0 (Free Tier)**: 무료, 제한적 성능
- **M10**: 최소 프로덕션, 약 $57-65/월

**프로덕션 환경**:
- **M10 이상**: 워크로드에 따라 선택
- **주의**: Vector Search 자체는 추가 비용 없음 (클러스터 비용에 포함)

#### 7.2 Index 최적화

**불필요한 필터 필드 제거**:
- 사용하지 않는 필터 필드는 Index에서 제거하여 Index 크기 최소화

**numCandidates 값 조정**:
- 성능과 비용의 균형을 고려하여 최적값 선택
- 기본값(100)에서 시작하여 워크로드에 따라 조정

#### 7.3 임베딩 생성 비용

**OpenAI API 비용**:
- **모델**: text-embedding-3-small
- **비용**: $0.02 per 1M tokens
- **주의**: Vector Search 자체 비용과 별도로 계산

**비용 절감 방법**:
- **배치 작업**: 중복 임베딩 생성 방지
- **캐싱 전략**: 동일한 텍스트에 대한 임베딩 캐싱
- **텍스트 최적화**: 불필요한 텍스트 제거 (예: content 필드 2000자 제한)

### 8. 개발/테스트 환경 예상 비용

#### 8.1 MongoDB Atlas Vector Search 비용 구성

**클러스터 비용**:
- 기존 클러스터 티어 비용과 동일
- Vector Search는 추가 비용 없음 (클러스터 비용에 포함)

**스토리지 비용**:
- 벡터 필드 저장 공간 추가
- 계산: 1536 dimensions × 4 bytes = 약 6KB per document
- 예시: 10,000 documents × 6KB = 약 60MB (무료 범위)

**컴퓨트 비용**:
- Vector Search 쿼리는 클러스터 리소스 사용
- 별도 비용 없음 (클러스터 비용에 포함)

#### 8.2 예상 비용 계산 (개발/테스트 환경)

| 항목 | 계산 | 예상 비용 |
|---|---|---|
| **클러스터 비용** | M0 (Free Tier) 또는 M10 | M0: $0/월, M10: $57-65/월 |
| **스토리지 추가** | 10,000 documents × 6KB | 약 60MB (무료 범위) |
| **임베딩 생성** | OpenAI API 호출 | $0.02 per 1M tokens |
| **총 예상 비용** | | M0: 약 $0-1/월, M10: 약 $57-66/월 |

**주의사항**:
- Vector Search 자체는 추가 비용 없음 (클러스터 비용에 포함)
- 임베딩 생성은 OpenAI API 비용 발생 (별도 계산)
- 대규모 벡터 데이터의 경우 스토리지 비용 고려

---

## 프로젝트 연동

### 1. 환경변수 설정

#### 1.1 필수 환경변수

다음 환경변수를 설정합니다:

```bash
# MongoDB Atlas Cluster 연결 문자열
MONGODB_ATLAS_CONNECTION_STRING=mongodb+srv://tech-n-ai-user:password@tech-n-ai-cluster.xxxxx.mongodb.net/tech_n_ai?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true
MONGODB_ATLAS_DATABASE=tech_n_ai
```

**연결 문자열 구성 요소**:
- `mongodb+srv://`: SRV 연결 문자열 (DNS 기반)
- `<username>:<password>`: 데이터베이스 사용자 인증 정보
- `<cluster-endpoint>`: 클러스터 엔드포인트
- `<database>`: 데이터베이스 이름
- `retryWrites=true`: 쓰기 재시도 활성화
- `w=majority`: Write Concern (대다수 노드 확인)
- `readPreference=secondaryPreferred`: 읽기 복제본 우선
- `ssl=true`: SSL/TLS 연결 필수

#### 1.2 로컬 환경 설정 (`.env` 파일)

로컬 개발 환경에서는 `.env` 파일을 사용합니다:

```bash
# .env 파일 (프로젝트 루트)
MONGODB_ATLAS_CONNECTION_STRING=mongodb+srv://tech-n-ai-user:password@tech-n-ai-cluster.xxxxx.mongodb.net/tech_n_ai?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true
MONGODB_ATLAS_DATABASE=tech_n_ai
```

**주의사항**: `.env` 파일은 `.gitignore`에 포함되어야 합니다.

#### 1.3 프로덕션 환경 설정

프로덕션 환경에서는 다음 방법 중 하나를 사용합니다:

- **환경변수 직접 설정**: 컨테이너/서버 환경변수로 설정
- **AWS Secrets Manager**: 연결 문자열 등 민감 정보 관리
- **AWS Systems Manager Parameter Store**: 환경변수 관리

### 2. MongoClient 설정 확인

`domain/mongodb/src/main/resources/application-mongodb-domain.yml` 파일이 환경변수를 사용하여 연결을 설정합니다:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_ATLAS_CONNECTION_STRING}
      database: ${MONGODB_ATLAS_DATABASE:tech_n_ai}
```

### 3. MongoClientConfig 확인

`domain/mongodb/src/main/java/com/ebson/shrimp/tm/demo/domain/mongodb/config/MongoClientConfig.java` 파일에서 다음 설정이 적용됩니다:

- **연결 풀 최적화**: `maxSize: 100`, `minSize: 10`
- **Read Preference**: `secondaryPreferred()` (읽기 복제본 우선)
- **Write Concern**: `majority` (데이터 일관성 보장)
- **Retry 설정**: `retryWrites: true`, `retryReads: true`

### 4. 연결 테스트

#### 4.1 애플리케이션 시작 시 확인

애플리케이션 시작 시 다음 로그를 확인합니다:

```
MongoDB Atlas connection configured: database=tech_n_ai, readPreference=secondaryPreferred, maxPoolSize=100, minPoolSize=10
```

#### 4.2 수동 연결 테스트

```bash
# MongoDB Shell로 연결 테스트
mongosh "mongodb+srv://tech-n-ai-cluster.xxxxx.mongodb.net/tech_n_ai" \
  --username tech-n-ai-user \
  --password your-password

# 연결 후 데이터베이스 확인
show dbs
use tech_n_ai
show collections
```

---

## 성능 최적화

### 1. 읽기 복제본 활용

#### 1.1 Read Preference 설정

`MongoClientConfig`에서 `ReadPreference.secondaryPreferred()`로 설정되어 있습니다:

```java
.readPreference(ReadPreference.secondaryPreferred())
```

이 설정으로 인해:
- 읽기 작업은 기본적으로 보조 노드(읽기 복제본)에서 수행
- 보조 노드가 사용 불가능한 경우에만 기본 노드에서 읽기 수행
- CQRS 패턴의 Query Side 특성상 최종 일관성 허용 가능

#### 1.2 복제 지연 모니터링

Atlas Monitoring에서 복제 지연을 모니터링합니다:
- **Replication Lag**: 복제 지연 시간
- **Oplog Window**: Oplog 보존 기간

### 2. 연결 풀 최적화

#### 2.1 클러스터 티어별 최적 연결 풀 크기

현재 설정 (`MongoClientConfig`):

```java
.maxSize(100)                    // 최대 연결 수
.minSize(10)                     // 최소 연결 수
```

**클러스터 티어별 권장 설정**:

- **M10**: `maxSize: 100`, `minSize: 10` (현재 설정)
- **M20**: `maxSize: 200`, `minSize: 20`
- **M30**: `maxSize: 300`, `minSize: 30`

#### 2.2 연결 풀 모니터링

Atlas Monitoring에서 연결 수를 모니터링합니다:
- **Connections**: 현재 연결 수
- **Connection Pool**: 연결 풀 사용률

### 3. 인덱스 전략

#### 3.1 ESR 규칙 준수

모든 복합 인덱스는 ESR 규칙을 준수합니다:
- **Equality (등가)**: 등가 조건 필드
- **Sort (정렬)**: 정렬 필드
- **Range (범위)**: 범위 쿼리 필드

자세한 인덱스 전략은 `docs/step1/2. mongodb-schema-design.md`의 **인덱스 전략** 섹션을 참고합니다.

#### 3.2 커버링 인덱스 활용

쿼리에 필요한 모든 필드가 인덱스에 포함되면 커버링 인덱스가 되어 성능이 향상됩니다:

```javascript
// 커버링 인덱스 예제
// 인덱스: { user_id: 1, item_type: 1, created_at: -1 }
// 쿼리: { user_id: "user123", item_type: "CONTEST" }, projection: { user_id: 1, item_type: 1, created_at: 1 }
```

#### 3.3 인덱스 사용 확인

```javascript
// 쿼리 실행 계획 확인
db.archives.find({ user_id: "user123" }).sort({ created_at: -1 }).explain("executionStats")

// 인덱스 사용 여부 확인
// "stage": "IXSCAN" → 인덱스 사용
// "stage": "COLLSCAN" → 컬렉션 스캔 (인덱스 미사용)
```

### 4. 프로젝션 활용

필요한 필드만 선택하여 네트워크 트래픽을 최소화합니다:

```java
// Spring Data MongoDB 프로젝션 예제
@Query(value = "{ 'user_id': ?0 }", fields = "{ 'item_type': 1, 'item_title': 1, 'created_at': 1 }")
List<ArchiveDocument> findArchivesByUserId(String userId);
```

### 5. 집계 파이프라인 최적화

MongoDB는 집계 파이프라인을 자동으로 최적화합니다:

- `$match` 단계를 가능한 한 앞으로 이동
- 계산된 필드에 의존하지 않는 필터는 프로젝션 단계 전에 배치

자세한 내용은 `docs/step1/mongodb-atlas-schema-design-best-practices.md`의 **집계 파이프라인 최적화** 섹션을 참고합니다.

---

## 개발/테스트 환경 예상 비용

**참고**: [MongoDB Atlas 공식 문서 - 가격](https://www.mongodb.com/pricing)

### 1. 비용 구성 요소

MongoDB Atlas의 비용은 다음 항목으로 구성됩니다:

| 비용 항목 | 설명 | 참고 |
|---|---|---|
| **클러스터 티어** | Free (M0), Shared (M2/M5), Dedicated (M10 이상) | 티어별 고정 월 비용 |
| **스토리지** | GB-month 단위 스토리지 비용 | 티어별 포함 용량 또는 추가 비용 |
| **데이터 전송** | 인터넷 아웃바운드 전송 시 요금 | 리전 내 전송은 무료 또는 저렴 |
| **백업** | Cloud Backup 사용 시 추가 비용 | M10 이상에서 사용 가능 |
| **App Services** | Atlas App Services 사용 시 함수 실행, 요청 수 등 | 사용량 기반 청구 |

### 2. 무료 티어 (Free Tier)

**중요**: MongoDB Atlas는 **M0 Free Tier**를 제공합니다. 이는 "Free forever"로 제공되며, 개발/테스트 환경에 적합합니다.

**M0 Free Tier 특징**:
- **비용**: $0/월 (무료)
- **스토리지**: 512 MB
- **RAM**: 공유 RAM (제한적)
- **vCPU**: 공유 vCPU (제한적)
- **백업**: 제한적 (일부 기능 없음)
- **용도**: 학습, 프로토타이핑, 소규모 개발/테스트

**제한사항**:
- 스토리지 용량 제한 (512 MB)
- 성능 제한 (공유 리소스)
- 일부 고급 기능 사용 불가

### 3. 개발/테스트 환경 비용 예시

#### 3.1 Free Tier (M0)

**가정 조건**:
- **티어**: M0 (Free Tier)
- **스토리지**: 512 MB (무료)
- **사용량**: 소규모 개발/테스트

**예상 비용**:

| 항목 | 비용 |
|---|---|
| **클러스터 비용** | **$0/월** (무료) |
| **스토리지** | 포함 (512 MB) |
| **데이터 전송** | 최소 (개발 환경) |
| **총 예상 비용** | **$0/월** |

**참고**: 
- M0는 학습 및 소규모 프로토타이핑에 적합
- 프로덕션 환경에는 권장하지 않음

#### 3.2 Shared Tier (M2/M5)

**가정 조건**:
- **티어**: M2 또는 M5 (Shared)
- **스토리지**: 2-5 GB
- **사용량**: 중간 규모 개발/테스트

**예상 비용**:

| 항목 | 비용 |
|---|---|
| **클러스터 비용** | 약 **$9-15/월** |
| **스토리지** | 포함 (티어별 기본 용량) |
| **데이터 전송** | 최소 |
| **총 예상 비용** | 약 **$9-15/월** |

#### 3.3 Dedicated Tier (M10) - 최소 프로덕션

**가정 조건**:
- **티어**: M10 (Dedicated)
- **스토리지**: 10 GB
- **사용량**: 소규모 프로덕션 또는 스테이징 환경

**예상 비용**:

| 항목 | 비용 |
|---|---|
| **클러스터 비용** | 약 **$57/월** (ap-northeast-2 기준) |
| **스토리지** | 포함 (10 GB) |
| **백업** | 포함 (Cloud Backup 기본) |
| **데이터 전송** | 최소 |
| **총 예상 비용** | 약 **$57-65/월** |

**참고**: 
- M10은 프로덕션 환경 최소 권장 사양
- 리전별 가격 차이 존재

#### 3.4 Flex Tier (Serverless)

**가정 조건**:
- **티어**: Flex (Serverless)
- **스토리지**: 5 GB
- **Ops/sec**: 0-100 (기본)

**예상 비용**:

| 항목 | 비용 |
|---|---|
| **클러스터 비용** | 약 **$8/월** (기본) |
| **스토리지** | 포함 (5 GB) |
| **Ops/sec 확장** | 사용량에 따라 추가 비용 |
| **총 예상 비용** | 약 **$8-30/월** (사용량에 따라 변동) |

**참고**: 
- Flex Tier는 사용량 기반으로 자동 확장
- 낮은 사용량에서는 저렴, 높은 사용량에서는 비용 증가

### 4. 비용 계산 도구

MongoDB Atlas에서는 다음 도구를 제공합니다:

1. **MongoDB Atlas Pricing Calculator**
   - URL: https://www.mongodb.com/pricing/calculator
   - 클러스터 티어, 리전, 스토리지 등을 입력하여 예상 비용 계산

2. **Atlas Billing Dashboard**
   - 실제 사용량 기반 비용 확인
   - 클러스터별, 프로젝트별 비용 분석

3. **Atlas Cost Alerts**
   - 예산 설정 및 알림
   - 비용 임계값 초과 시 알림

### 5. 비용 절감 팁 (개발/테스트 환경)

1. **Free Tier (M0) 활용**
   - 개발 초기 단계에서는 M0 사용
   - 학습 및 프로토타이핑에 적합

2. **스토리지 최적화**
   - TTL 인덱스로 임시 데이터 자동 삭제
   - 불필요한 인덱스 제거

3. **데이터 전송 최소화**
   - Private Endpoint 활용 (M10 이상)
   - VPC Peering을 통한 내부 통신

4. **클러스터 티어 선택**
   - 개발 환경: M0 (무료) 또는 M2/M5 (Shared)
   - 스테이징 환경: M10 (Dedicated)
   - 프로덕션 환경: M20 이상 권장

5. **사용하지 않을 때 중지**
   - 개발 환경에서는 필요 시에만 클러스터 실행
   - 테스트 완료 후 클러스터 일시 중지 고려

6. **리전 선택**
   - ap-northeast-2 (서울) 리전 사용 시 데이터 전송 비용 절감

### 6. 예상 비용 요약

| 환경 | 티어 | 예상 월 비용 |
|---|---|---|
| **개발/테스트 (무료)** | M0 (Free Tier) | **$0/월** |
| **개발/테스트 (Shared)** | M2/M5 (Shared) | 약 **$9-15/월** |
| **개발/테스트 (Flex)** | Flex (Serverless) | 약 **$8-30/월** |
| **소규모 프로덕션** | M10 (Dedicated) | 약 **$57-65/월** |
| **중규모 프로덕션** | M20 (Dedicated) | 약 **$200-250/월** |

**참고**: 
- 위 비용은 ap-northeast-2 (서울) 리전 기준
- 실제 비용은 사용량, 스토리지, 데이터 전송량에 따라 변동
- 최신 가격은 [MongoDB Atlas 가격 페이지](https://www.mongodb.com/pricing)에서 확인

### 7. Aurora vs Atlas 비용 비교

| 서비스 | 개발/테스트 최소 구성 | 예상 월 비용 |
|---|---|---|
| **AWS Aurora** | Serverless v2, 0.5 ACU, 30GB | 약 **$46-51/월** |
| **MongoDB Atlas** | M0 (Free Tier) | **$0/월** |
| **MongoDB Atlas** | M10 (Dedicated) | 약 **$57-65/월** |

**비교 요약**:
- MongoDB Atlas M0는 완전 무료로 개발/테스트 시작 가능
- AWS Aurora는 무료 티어가 없어 최소 $46-51/월 비용 발생
- 프로덕션 환경에서는 두 서비스 모두 비슷한 수준의 비용 발생

---

## 비용 절감 전략

### 1. 클러스터 티어 최적화

#### 1.1 워크로드 분석

Atlas Monitoring을 활용하여 워크로드를 분석합니다:

- **Connections**: 평균 연결 수
- **Operations**: 초당 작업 수 (OPS)
- **CPU/Memory**: 리소스 사용률
- **Storage**: 스토리지 사용량

#### 1.2 클러스터 다운사이징

워크로드 분석 결과를 바탕으로 클러스터 티어를 다운사이징합니다:

- **CPU 사용률**: 평균 30% 이하 → 다운사이징 고려
- **메모리 사용률**: 평균 50% 이하 → 다운사이징 고려
- **연결 수**: 최대 연결 수가 클러스터 사양보다 훨씬 적음 → 다운사이징 고려

**주의**: 다운사이징은 다운타임이 발생할 수 있으므로 트래픽이 적은 시간대에 수행합니다.

### 2. 스토리지 최적화

#### 2.1 인덱스 크기 최소화

- 불필요한 인덱스 제거
- 부분 인덱스 활용 (필요 시)
- TTL 인덱스로 임시 데이터 자동 삭제

#### 2.2 TTL 인덱스 활용

임시 데이터에 TTL 인덱스를 설정하여 자동 삭제:

- **news_articles**: `published_at` TTL 인덱스 (90일)
- **exception_logs**: `occurred_at` TTL 인덱스 (90일)

이미 `MongoIndexConfig`에서 설정되어 있습니다.

### 3. 네트워크 최적화

#### 3.1 Private Endpoint 활용

프로덕션 환경에서는 Private Endpoint를 통해 데이터 전송 비용을 절감합니다:

- **Public Endpoint**: 인터넷을 통한 데이터 전송 (비용 발생)
- **Private Endpoint**: VPC 내부 데이터 전송 (비용 절감)

#### 3.2 VPC Peering

AWS VPC와 VPC Peering을 설정하여 네트워크 비용을 절감합니다.

### 4. 모니터링 기반 최적화

#### 4.1 Performance Advisor 활용

Atlas Performance Advisor는 다음 권장사항을 제공합니다:

- **인덱스 권장사항**: 누락된 인덱스 제안
- **쿼리 최적화**: 느린 쿼리 개선 제안
- **스키마 최적화**: 도큐먼트 구조 개선 제안

#### 4.2 비용 분석

Atlas Billing을 활용하여 비용을 분석합니다:

- **클러스터별 비용**: 각 클러스터의 비용 분석
- **사용량 추이**: 시간대별 사용량 분석
- **비용 예측**: 예상 비용 계산

---

## 모니터링 및 유지보수

### 1. Atlas Monitoring 대시보드

Atlas는 자동으로 다음 메트릭을 모니터링합니다:

- **Connections**: 연결 수
- **Operations**: 작업 카운터 (읽기/쓰기)
- **Query Targeting**: 쿼리 타겟팅 효율성
- **System Metrics**: CPU, Memory, Disk 사용률

### 2. Performance Advisor

Performance Advisor는 다음 정보를 제공합니다:

- **Slow Queries**: 느린 쿼리 목록
- **Missing Indexes**: 누락된 인덱스 제안
- **Schema Suggestions**: 스키마 개선 제안

### 3. 정기 유지보수

#### 3.1 백업 확인

- 자동 백업이 정상적으로 실행되는지 확인
- Point-in-Time Recovery 테스트 (분기별)

#### 3.2 인덱스 최적화

- 사용되지 않는 인덱스 제거
- Performance Advisor 권장사항 적용

#### 3.3 데이터 정리

- TTL 인덱스로 임시 데이터 자동 삭제 확인
- 오래된 데이터 수동 정리 (필요 시)

### 4. 알림 설정

주요 이벤트에 대한 알림을 설정합니다:

- **Connection Count**: 임계값 초과
- **CPU Utilization**: 80% 이상
- **Memory Utilization**: 90% 이상
- **Disk Space**: 80% 이상
- **Replication Lag**: 복제 지연 증가

---

## 트러블슈팅

### 1. 연결 문제

#### 1.1 연결 타임아웃

**증상**: `Connection timeout` 오류

**해결 방법**:
- IP 액세스 리스트에 애플리케이션 서버 IP 추가
- 방화벽 설정 확인
- 네트워크 연결 확인

#### 1.2 인증 실패

**증상**: `Authentication failed` 오류

**해결 방법**:
- 데이터베이스 사용자 이름 및 비밀번호 확인
- 연결 문자열의 사용자 이름 및 비밀번호 확인
- 데이터베이스 사용자 권한 확인

### 2. 성능 이슈

#### 2.1 느린 쿼리

**해결 방법**:
- Performance Advisor에서 Slow Queries 확인
- 쿼리 실행 계획 확인 (`explain()` 사용)
- 인덱스 사용 여부 확인
- 누락된 인덱스 추가 (Performance Advisor 권장사항)

#### 2.2 높은 CPU 사용률

**해결 방법**:
- Performance Advisor에서 CPU를 많이 사용하는 쿼리 확인
- 클러스터 티어 업그레이드
- 쿼리 최적화 또는 인덱스 추가

#### 2.3 높은 메모리 사용률

**해결 방법**:
- 인덱스 크기 확인 및 최적화
- 불필요한 인덱스 제거
- 클러스터 티어 업그레이드

### 3. 복제 지연

**증상**: Reader에서 최신 데이터를 조회하지 못함

**해결 방법**:
- Atlas Monitoring에서 Replication Lag 확인
- Reader 인스턴스 성능 확인
- 필요 시 클러스터 티어 업그레이드
- `readPreference`를 `primary`로 변경 (강한 일관성 필요 시)

### 4. 스토리지 문제

#### 4.1 스토리지 부족

**증상**: 스토리지 사용률 90% 이상

**해결 방법**:
- TTL 인덱스로 임시 데이터 자동 삭제 확인
- 오래된 데이터 수동 정리
- 스토리지 업그레이드 (클러스터 티어 업그레이드)

### 5. 인덱스 문제

#### 5.1 인덱스 생성 실패

**증상**: `MongoIndexConfig`에서 인덱스 생성 실패

**해결 방법**:
- Atlas UI에서 수동으로 인덱스 생성
- 인덱스 정의 확인 (필드 이름, 정렬 방향)
- UNIQUE 인덱스 중복 데이터 확인

#### 5.2 인덱스 미사용

**증상**: 쿼리가 인덱스를 사용하지 않음 (`COLLSCAN`)

**해결 방법**:
- 쿼리 패턴 확인 (필드 순서, 정렬 방향)
- 인덱스 정의 확인
- Performance Advisor 권장사항 확인

---

## 결론

이 가이드는 MongoDB Atlas Cluster를 프로덕션 환경에 구축하고 프로젝트와 연동하는 실무 가이드를 제공합니다.

### 주요 특징

1. ✅ **실무 중심**: 실제 구축 가능한 단계별 가이드
2. ✅ **Atlas UI & CLI 지원**: GUI와 CLI 모두 상세 가이드 제공
3. ✅ **비용 최적화**: 워크로드 분석 기반 클러스터 티어 추천
4. ✅ **성능 최적화**: 읽기 복제본 활용, 인덱스 전략, 프로젝션 활용
5. ✅ **모니터링**: Atlas Monitoring 및 Performance Advisor 활용
6. ✅ **프로젝트 통합**: 기존 설계서 및 구현 코드와 완전 일치

### 다음 단계

1. Atlas 클러스터 생성 및 네트워크 구성
2. 데이터베이스 사용자 생성 및 연결 문자열 설정
3. 환경변수 설정 및 연결 테스트
4. 인덱스 생성 확인 및 모니터링 설정
5. 성능 튜닝 및 비용 최적화

---

## 참고 자료

### MongoDB Atlas 공식 문서

#### 클러스터 생성 및 관리
- [MongoDB Atlas 개요](https://www.mongodb.com/docs/atlas/)
- [Atlas 클러스터 생성 가이드](https://www.mongodb.com/docs/guides/atlas/cluster/)
- [Atlas 클러스터 연결](https://www.mongodb.com/docs/atlas/tutorial/connect-to-your-cluster/)
- [Atlas CLI 퀵스타트](https://www.mongodb.com/docs/atlas/cli/current/atlas-cli-quickstart/)

#### 네트워크 및 보안
- [Atlas 네트워크 연결 가이드](https://www.mongodb.com/docs/guides/atlas/network-connections/)
- [IP 액세스 리스트 설정](https://www.mongodb.com/docs/atlas/security/add-ip-address-to-list/)
- [VPC Peering 설정](https://www.mongodb.com/docs/atlas/security-vpc-peering/)
- [Private Endpoint 설정](https://www.mongodb.com/docs/atlas/security-vpc-private-endpoint/)
- [데이터베이스 사용자 생성](https://www.mongodb.com/docs/atlas/tutorial/create-mongodb-user-for-cluster/)

#### 백업 및 복구
- [Atlas 백업 및 복구](https://www.mongodb.com/docs/atlas/backup-restore-cluster/)
- [Cloud Backup 개요](https://www.mongodb.com/docs/atlas/backup/cloud-backup/)
- [스냅샷 복구](https://www.mongodb.com/docs/atlas/backup/cloud-backup/restore-overview/)

#### 모니터링 및 성능
- [Atlas 모니터링](https://www.mongodb.com/docs/atlas/monitoring/)
- [Performance Advisor](https://www.mongodb.com/docs/atlas/performance-advisor/)
- [알림 설정](https://www.mongodb.com/docs/atlas/configure-alerts/)

#### 데이터베이스 및 컬렉션
- [데이터베이스 및 컬렉션](https://www.mongodb.com/docs/atlas/databases-collections/)
- [인덱스 생성 및 관리](https://www.mongodb.com/docs/atlas/indexes/)

#### Vector Search
- [MongoDB Atlas Vector Search 개요](https://www.mongodb.com/products/platform/atlas-vector-search)
- [Atlas Vector Search 가이드](https://www.mongodb.com/docs/atlas/atlas-vector-search/)
- [Vector Search Index 생성](https://www.mongodb.com/docs/atlas/atlas-vector-search/create-index/)
- [$vectorSearch aggregation](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)
- [Vector Search 성능 최적화](https://www.mongodb.com/docs/atlas/atlas-vector-search/performance/)

#### Atlas CLI 참조
- [Atlas CLI 문서](https://www.mongodb.com/docs/atlas/cli/current/)
- [Atlas CLI 설치](https://www.mongodb.com/docs/atlas/cli/current/install-atlas-cli/)
- [Atlas CLI 인증](https://www.mongodb.com/docs/atlas/cli/current/connect-atlas-cli/)
- [클러스터 관리 명령어](https://www.mongodb.com/docs/atlas/cli/current/command/atlas-clusters/)

#### 드라이버 및 연결
- [MongoDB Java Driver](https://www.mongodb.com/docs/drivers/java/sync/current/)
- [드라이버 연결 가이드](https://www.mongodb.com/docs/atlas/driver-connection/)

#### MongoDB Manual
- [MongoDB Manual](https://www.mongodb.com/docs/manual/)
- [인덱스 전략](https://www.mongodb.com/docs/manual/applications/indexes/)
- [쿼리 최적화](https://www.mongodb.com/docs/manual/core/query-optimization/)

---

**문서 버전**: 1.2  
**최종 업데이트**: 2026-01-16  
**작성자**: Infrastructure Architect  
**주요 업데이트**: 
- Atlas UI (GUI) 및 Atlas CLI 상세 가이드 추가
- MongoDB Atlas Vector Search 구축 섹션 추가 (RAG 챗봇용)
