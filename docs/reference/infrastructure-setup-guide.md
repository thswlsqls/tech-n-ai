# 시스템 인프라 구축 가이드

**작성 일시**: 2026-01-XX  
**대상**: AWS 기반 전체 시스템 인프라 구축  
**목적**: 프로젝트의 실제 구조와 설정을 반영한 실용적인 인프라 구축 가이드

## 목차

1. [ALB → Gateway 라우팅 구축 가이드](#1-alb--gateway-라우팅-구축-가이드)
   - [1.1 ALB 생성 및 기본 설정](#11-alb-생성-및-기본-설정)
   - [1.2 Gateway 타겟 그룹 구성](#12-gateway-타겟-그룹-구성)
   - [1.3 라우팅 규칙 설정](#13-라우팅-규칙-설정)
   - [1.4 타임아웃 및 연결 설정](#14-타임아웃-및-연결-설정)

2. [AWS 서비스 구성 가이드](#2-aws-서비스-구성-가이드)
   - [2.1 Amazon Aurora MySQL 클러스터 구축](#21-amazon-aurora-mysql-클러스터-구축)
   - [2.2 Amazon MSK (Kafka) 구축](#22-amazon-msk-kafka-구축)
   - [2.3 Amazon ElastiCache for Redis 구축](#23-amazon-elasticache-for-redis-구축)
   - [2.4 AWS Secrets Manager 설정](#24-aws-secrets-manager-설정)
   - [2.5 Gateway 및 API 서버 ECS Fargate 구성](#25-gateway-및-api-서버-ecs-fargate-구성)

3. [Spring Batch 애플리케이션 스케줄링 가이드](#3-spring-batch-애플리케이션-스케줄링-가이드)
   - [3.1 EventBridge Scheduler 설정](#31-eventbridge-scheduler-설정)
   - [3.2 ECS Fargate 태스크 정의](#32-ecs-fargate-태스크-정의)
   - [3.3 배치 잡 실행 설정](#33-배치-잡-실행-설정)
   - [3.4 비용 최적화 스케줄링 전략](#34-비용-최적화-스케줄링-전략)

4. [비용 절감 전략 가이드](#4-비용-절감-전략-가이드)
   - [4.1 Aurora MySQL 비용 최적화](#41-aurora-mysql-비용-최적화)
   - [4.2 MSK 비용 최적화](#42-msk-비용-최적화)
   - [4.3 ElastiCache Redis 비용 최적화](#43-elasticache-redis-비용-최적화)
   - [4.4 ECS Fargate 비용 최적화](#44-ecs-fargate-비용-최적화)
   - [4.5 ALB 비용 최적화](#45-alb-비용-최적화)
   - [4.6 종합 비용 절감 전략](#46-종합-비용-절감-전략)

---

## 1. ALB → Gateway 라우팅 구축 가이드

### 1.1 ALB 생성 및 기본 설정

#### 단계별 생성 가이드

1. **AWS 콘솔 접속**
   - AWS Management Console → EC2 서비스 → 로드 밸런서 선택

2. **로드 밸런서 생성**
   - "로드 밸런서 생성" 버튼 클릭
   - **로드 밸런서 유형**: Application Load Balancer 선택

3. **기본 구성**
   ```
   이름: tech-n-ai-alb-{환경명}
   체계: 인터넷 대면 (프로덕션) 또는 내부 (개발)
   IP 주소 유형: IPv4
   ```

4. **네트워크 매핑**
   - **VPC**: Gateway 서버가 위치한 VPC 선택
   - **가용 영역**: 최소 2개 이상 선택 (고가용성)
   - **서브넷**: 각 가용 영역의 퍼블릭 서브넷 선택

5. **보안 그룹 설정**
   - 새 보안 그룹 생성 또는 기존 사용
   - **인바운드 규칙**:
     ```
     HTTP (80): 0.0.0.0/0 (HTTPS 리다이렉트용)
     HTTPS (443): 0.0.0.0/0 (프로덕션) 또는 특정 IP (개발)
     ```

6. **리스너 및 라우팅**
   - **HTTP 리스너 (포트 80)**: HTTPS로 리다이렉트 설정
   - **HTTPS 리스너 (포트 443)**: SSL/TLS 인증서 설정

7. **SSL/TLS 인증서 설정**
   - **인증서 소스**: ACM (AWS Certificate Manager)에서 인증서 선택
   - 인증서가 없는 경우 ACM에서 먼저 인증서 요청 필요
   - **보안 정책**: ELBSecurityPolicy-TLS-1-2-2017-01 권장

8. **생성 완료**
   - "로드 밸런서 생성" 클릭
   - 생성 완료까지 약 1-2분 소요

**참고 문서**:
- [AWS Application Load Balancer 사용자 가이드](https://docs.aws.amazon.com/ko_kr/elasticloadbalancing/latest/application/introduction.html)

---

### 1.2 Gateway 타겟 그룹 구성

#### 타겟 그룹 생성

1. **EC2 콘솔** → **타겟 그룹** → **타겟 그룹 생성**

2. **기본 설정**
   ```
   타겟 그룹 이름: tech-n-ai-gateway-tg-{환경명}
   대상 유형: IP 주소 또는 인스턴스 (ECS Fargate의 경우 IP 주소)
   프로토콜: HTTP
   포트: 8081 (Gateway 서버 포트, api/gateway/README.md 참고)
   VPC: Gateway 서버가 위치한 VPC 선택
   ```

3. **헬스체크 설정**
   ```
   프로토콜: HTTP
   경로: /actuator/health (Spring Boot Actuator 엔드포인트)
   포트: 트래픽 포트 사용
   정상 임계값: 2
   비정상 임계값: 2
   타임아웃: 5초
   간격: 30초
   성공 코드: 200
   ```

4. **고급 헬스체크 설정**
   ```
   요청 제한 시간: 5초
   정상 상태 코드: 200
   ```

5. **타겟 등록**
   - **ECS Fargate 서비스인 경우**:
     - ECS 서비스의 네트워크 모드가 `awsvpc`인지 확인
     - 타겟 그룹에 ECS 서비스의 태스크 IP 주소 등록
     - 또는 ECS 서비스의 로드 밸런서 통합 사용 (권장)
   - **EC2 인스턴스인 경우**:
     - Gateway 서버가 실행 중인 EC2 인스턴스 선택
     - 인스턴스의 프라이빗 IP 주소 등록

6. **타겟 그룹 속성 설정**
   ```
   연결 드레이닝: 활성화
   드레이닝 시간: 300초 (5분)
   느린 시작 기간: 0초 (비활성화)
   ```

**주의사항**:
- Gateway 서버는 포트 8081에서 실행됨 (`api/gateway/README.md` 참고)
- 헬스체크 경로는 Spring Boot Actuator의 `/actuator/health` 엔드포인트 사용
- 타겟 그룹의 헬스체크가 성공해야 ALB가 트래픽을 라우팅함

---

### 1.3 라우팅 규칙 설정

#### 기본 라우팅 규칙

Gateway 서버는 모든 요청을 받아서 내부적으로 라우팅하므로, ALB에서는 모든 요청을 Gateway 타겟 그룹으로 라우팅합니다.

1. **ALB 콘솔** → 생성한 ALB 선택 → **리스너** 탭

2. **HTTPS 리스너 (443) 편집**

3. **기본 규칙 설정**
   ```
   우선순위: 1
   조건: 없음 (모든 요청)
   작업: 
     - 전달 대상: tech-n-ai-gateway-tg-{환경명}
     - 대상 그룹: tech-n-ai-gateway-tg-{환경명}
   ```

4. **Gateway 라우팅 규칙 확인**

Gateway 서버의 라우팅 규칙 (`api/gateway/README.md` 참고):

| 경로 패턴 | 대상 서버 | 인증 필요 |
|----------|---------|---------|
| `/api/v1/auth/**` | `@api/auth` | ❌ |
| `/api/v1/archive/**` | `@api/archive` | ✅ |
| `/api/v1/contest/**` | `@api/contest` | ❌ |
| `/api/v1/news/**` | `@api/news` | ❌ |
| `/api/v1/chatbot/**` | `@api/chatbot` | ✅ |

**중요**: ALB에서는 모든 요청을 Gateway로 라우팅하고, Gateway가 내부적으로 각 API 서버로 라우팅합니다.

#### HTTP → HTTPS 리다이렉트 설정

1. **HTTP 리스너 (80) 편집**

2. **리다이렉트 규칙 추가**
   ```
   우선순위: 1
   조건: 없음 (모든 요청)
   작업:
     - 리다이렉트 대상: HTTPS
     - 포트: 443
     - 상태 코드: 301 (영구 리다이렉트)
   ```

**참고 문서**:
- [Gateway 설계서](docs/step14/gateway-design.md)
- [Gateway README](api/gateway/README.md)

---

### 1.4 타임아웃 및 연결 설정

#### ALB 타임아웃 설정

Gateway 서버의 요청 처리 흐름을 고려하여 ALB 타임아웃을 설정합니다.

1. **ALB 콘솔** → 생성한 ALB 선택 → **속성** 탭

2. **유휴 시간 제한 편집**
   ```
   유휴 시간 제한: 600초 (10분)
   ```

**설정 근거**:
- Gateway README.md에 명시된 ALB 타임아웃 600초 설정 반영
- Gateway 서버의 연결 풀 설정과 조화:
  - Gateway 연결 타임아웃: 30초
  - Gateway 응답 타임아웃: 60초
  - 백엔드 서비스 타임아웃 고려

3. **연결 드레이닝 설정**
   ```
   연결 드레이닝 활성화: 예
   연결 드레이닝 시간: 300초 (5분)
   ```

#### Gateway 연결 풀 설정 확인

Gateway 서버의 연결 풀 설정 (`api/gateway/src/main/resources/application.yml` 참고):

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-idle-time: 30000        # 30초
          max-life-time: 300000       # 5분
          max-connections: 500
          acquire-timeout: 45000      # 45초
          pending-acquire-timeout: 60000  # 60초
        connection-timeout: 30000     # 30초
        response-timeout: 60000       # 60초
```

**주의사항**:
- ALB 타임아웃(600초)은 Gateway의 응답 타임아웃(60초)보다 충분히 길게 설정
- Gateway가 백엔드 서비스로 요청을 전달하는 시간을 고려

---

## 2. AWS 서비스 구성 가이드

### 2.1 Amazon Aurora MySQL 클러스터 구축

#### 단계별 생성 가이드

1. **AWS 콘솔 접속**
   - AWS Management Console → RDS 서비스 선택

2. **데이터베이스 생성**
   - "데이터베이스 생성" 버튼 클릭
   - **엔진 옵션**: Amazon Aurora 선택
   - **엔진 버전**: Aurora MySQL 3.x (MySQL 8.0 호환) 선택

3. **템플릿 선택**
   - **개발 환경**: "프로덕션" (비용 고려)
   - **프로덕션 환경**: "프로덕션 - 다중 AZ" 권장

4. **설정 구성**
   ```
   DB 클러스터 식별자: aurora-cluster-{환경명}
   마스터 사용자 이름: admin
   마스터 암호: AWS Secrets Manager에서 관리 (2.4절 참고)
   ```

5. **인스턴스 구성**
   - **개발 환경**: `db.t3.medium` 또는 `db.t4g.medium` (ARM 기반, 비용 효율적)
   - **프로덕션 환경**: `db.r6g.large` 이상 권장
   - **인스턴스 수**: 1개 (Writer), Reader는 필요 시 추가

6. **연결 설정**
   - **VPC**: Gateway 및 API 서버와 동일한 VPC 선택
   - **서브넷 그룹**: 기본값 사용 또는 커스텀
   - **퍼블릭 액세스**: "아니오" (VPC 내부 통신만 허용)
   - **VPC 보안 그룹**: 새로 생성 또는 기존 사용
     - 인바운드 규칙: MySQL/Aurora (3306) 포트 허용
     - 소스: Gateway 및 API 서버의 보안 그룹

7. **데이터베이스 인증**
   - **암호 인증**: "암호 인증" 선택

8. **추가 구성**
   - **백업 보존 기간**: 7일 (개발), 30일 (프로덕션)
   - **백업 윈도우**: 기본값 사용
   - **암호화**: 활성화 권장

9. **생성 완료**
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

#### 스키마 생성

프로젝트는 API 모듈별로 독립적인 스키마를 사용합니다 (`docs/step1/3. aurora-schema-design.md` 참고):

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

#### 환경변수 설정

애플리케이션에서 사용할 환경변수 설정:

```bash
# Aurora DB Cluster 연결 정보
export AURORA_WRITER_ENDPOINT=aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com
export AURORA_READER_ENDPOINT=aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com
export AURORA_USERNAME=admin
export AURORA_PASSWORD=<Secrets Manager에서 가져오기>
export AURORA_OPTIONS=useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

**참고 문서**:
- [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
- [Aurora 스키마 설계서](docs/step1/3.%20aurora-schema-design.md)
- [데이터소스 구축 가이드](docs/datasource-setup-guide.md)

---

### 2.2 Amazon MSK (Kafka) 구축

#### 단계별 생성 가이드

1. **AWS 콘솔 접속**
   - AWS Management Console → Amazon MSK 서비스 선택

2. **클러스터 생성**
   - "클러스터 생성" 버튼 클릭

3. **클러스터 이름 및 설명**
   ```
   클러스터 이름: tech-n-ai-msk-{환경명}
   ```

4. **Apache Kafka 버전**
   - **Kafka 버전**: 최신 안정 버전 선택 (예: 3.5.1)

5. **브로커 구성**
   - **브로커 수**: 3개 (고가용성, 최소 2개)
   - **브로커 인스턴스 타입**:
     - **개발 환경**: `kafka.t3.small` (비용 최적화)
     - **프로덕션 환경**: `kafka.m5.large` 이상 권장
   - **스토리지**: EBS 볼륨 (100GB, 프로비저닝된 IOPS)

6. **네트워크 설정**
   - **VPC**: Gateway 및 API 서버와 동일한 VPC 선택
   - **서브넷**: 각 가용 영역의 프라이빗 서브넷 선택 (최소 2개)
   - **보안 그룹**: 새로 생성 또는 기존 사용
     - 인바운드 규칙: 사용자 지정 TCP (9092, 9094, 9096) 포트 허용
     - 소스: Gateway 및 API 서버의 보안 그룹

7. **액세스 제어 방법**
   - **인증**: SASL/SCRAM (개발) 또는 mTLS (프로덕션)
   - **암호화**: TLS 암호화 활성화 권장

8. **모니터링**
   - **향상된 모니터링**: CloudWatch Logs 활성화 권장

9. **생성 완료**
   - "클러스터 생성" 클릭
   - 생성 완료까지 약 15-20분 소요

#### 브로커 엔드포인트 확인

생성 완료 후 MSK 콘솔에서 클러스터 선택:

1. **브로커 엔드포인트** 확인
   ```
   예시: b-1.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092
   ```
   - 위치: "속성" 탭 → "브로커 엔드포인트" (플레인텍스트 또는 TLS)

2. **Bootstrap 서버 주소 구성**
   ```
   KAFKA_BOOTSTRAP_SERVERS=b-1.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-2.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-3.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092
   ```

#### 토픽 생성

프로젝트에서 사용하는 Kafka 토픽 (`docs/step11/cqrs-kafka-sync-design.md` 참고):

```bash
# Kafka 클러스터에 연결 (EC2 인스턴스 또는 로컬에서)
# MSK 클러스터의 보안 그룹에서 접근 허용 필요

# 토픽 생성
kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER_ENDPOINT> \
  --topic user-events \
  --partitions 3 \
  --replication-factor 3

kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER_ENDPOINT> \
  --topic archive-events \
  --partitions 3 \
  --replication-factor 3

kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER_ENDPOINT> \
  --topic contest-events \
  --partitions 3 \
  --replication-factor 3

kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER_ENDPOINT> \
  --topic news-events \
  --partitions 3 \
  --replication-factor 3

kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER_ENDPOINT> \
  --topic conversation-events \
  --partitions 3 \
  --replication-factor 3
```

#### 환경변수 설정

애플리케이션에서 사용할 환경변수 설정:

```bash
# Kafka 연결 정보
export KAFKA_BOOTSTRAP_SERVERS=b-1.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-2.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-3.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092
```

**참고 문서**:
- [Amazon MSK 개발자 가이드](https://docs.aws.amazon.com/ko_kr/msk/latest/developerguide/)
- [Kafka 동기화 설계서](docs/step11/cqrs-kafka-sync-design.md)
- [Kafka 모듈 README](common/kafka/README.md)

---

### 2.3 Amazon ElastiCache for Redis 구축

#### 단계별 생성 가이드

1. **AWS 콘솔 접속**
   - AWS Management Console → ElastiCache 서비스 선택

2. **Redis 클러스터 생성**
   - "Redis 클러스터 생성" 버튼 클릭

3. **클러스터 설정**
   ```
   클러스터 이름: tech-n-ai-redis-{환경명}
   설명: OAuth State 저장 및 Rate Limiting용 Redis
   ```

4. **엔진 및 버전**
   - **엔진**: Redis 선택
   - **버전**: 최신 안정 버전 선택 (예: 7.0)

5. **노드 구성**
   - **노드 타입**:
     - **개발 환경**: `cache.t3.micro` 또는 `cache.t3.small` (비용 최적화)
     - **프로덕션 환경**: `cache.r6g.large` 이상 권장
   - **복제본 수**: 1개 (개발), 2개 이상 (프로덕션, 고가용성)

6. **네트워크 설정**
   - **VPC**: Gateway 및 API 서버와 동일한 VPC 선택
   - **서브넷 그룹**: 새로 생성 또는 기존 사용
   - **가용 영역**: 자동 선택 또는 수동 지정
   - **보안 그룹**: 새로 생성 또는 기존 사용
     - 인바운드 규칙: 사용자 지정 TCP (6379) 포트 허용
     - 소스: Gateway 및 API 서버의 보안 그룹

7. **Redis 설정**
   - **파라미터 그룹**: 기본값 사용 또는 커스텀
   - **자동 백업**: 활성화 권장 (프로덕션)
   - **백업 보존 기간**: 1일 (개발), 7일 (프로덕션)

8. **보안 설정**
   - **암호화 전송**: 활성화 권장 (프로덕션)
   - **Redis AUTH**: 활성화 (비밀번호 설정)

9. **생성 완료**
   - "클러스터 생성" 클릭
   - 생성 완료까지 약 5-10분 소요

#### 엔드포인트 확인

생성 완료 후 ElastiCache 콘솔에서 클러스터 선택:

1. **Primary 엔드포인트** 확인
   ```
   예시: tech-n-ai-redis.xxxxx.cache.amazonaws.com:6379
   ```
   - 위치: "속성" 탭 → "Primary 엔드포인트"

2. **Reader 엔드포인트** 확인 (복제본이 있는 경우)
   ```
   예시: tech-n-ai-redis-ro.xxxxx.cache.amazonaws.com:6379
   ```
   - 위치: "속성" 탭 → "Reader 엔드포인트"

#### Redis 사용 패턴 반영

프로젝트의 Redis 사용 패턴 (`docs/step7/redis-optimization-best-practices.md` 참고):

1. **OAuth State 저장**
   - Key 형식: `oauth:state:{state_value}`
   - TTL: 10분
   - 사용 모듈: `api-auth`

2. **Rate Limiting**
   - Key 형식: `rate-limit:{source-name}` 또는 `rate-limit:slack:webhook:{webhook-id}`
   - TTL: 1분 (Slack), 없음 (RSS/Scraper)
   - 사용 모듈: `client-rss`, `client-scraper`, `client-slack`

3. **Kafka 이벤트 중복 처리 방지**
   - Key 형식: `processed_event:{eventId}`
   - TTL: 7일
   - 사용 모듈: `common-kafka`

#### 환경변수 설정

애플리케이션에서 사용할 환경변수 설정:

```bash
# Redis 연결 정보
export REDIS_HOST=tech-n-ai-redis.xxxxx.cache.amazonaws.com
export REDIS_PORT=6379
export REDIS_PASSWORD=<Secrets Manager에서 가져오기>
export REDIS_SSL_ENABLED=true  # 프로덕션 환경
```

**참고 문서**:
- [Amazon ElastiCache for Redis 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonElastiCache/latest/red-ug/)
- [Redis 최적화 가이드](docs/step7/redis-optimization-best-practices.md)

---

### 2.4 AWS Secrets Manager 설정

#### 시크릿 생성

프로젝트에서 사용하는 모든 민감 정보를 Secrets Manager에 저장합니다.

1. **AWS 콘솔 접속**
   - AWS Management Console → Secrets Manager 서비스 선택

2. **시크릿 생성**

#### JWT Secret Key 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/jwt-secret-key/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "secretKey": "your-jwt-secret-key-minimum-256-bits"
   }
   ```

4. **자동 교체**: 비활성화 (수동 관리)

#### Aurora MySQL 비밀번호 저장

1. **시크릿 유형**: "RDS 데이터베이스 자격 증명" 선택

2. **데이터베이스 선택**
   - 생성한 Aurora 클러스터 선택

3. **시크릿 이름**
   ```
   tech-n-ai/aurora/{환경명}
   ```

4. **자동 교체**: 활성화 권장 (비밀번호 자동 교체)

#### MongoDB Atlas 연결 정보 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/mongodb-atlas/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "connectionString": "mongodb+srv://username:password@cluster.mongodb.net/database?retryWrites=true&w=majority&readPreference=secondaryPreferred&ssl=true"
   }
   ```

#### Kafka 연결 정보 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/kafka/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "bootstrapServers": "b-1.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-2.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092,b-3.tech-n-ai-msk.xxxxx.c2.kafka.ap-northeast-2.amazonaws.com:9092"
   }
   ```

#### Redis 연결 정보 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/redis/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "host": "tech-n-ai-redis.xxxxx.cache.amazonaws.com",
     "port": 6379,
     "password": "your-redis-password"
   }
   ```

#### OAuth 클라이언트 정보 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/oauth/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "google": {
       "clientId": "your-google-client-id",
       "clientSecret": "your-google-client-secret"
     },
     "naver": {
       "clientId": "your-naver-client-id",
       "clientSecret": "your-naver-client-secret"
     },
     "kakao": {
       "clientId": "your-kakao-client-id",
       "clientSecret": "your-kakao-client-secret"
     }
   }
   ```

#### OpenAI API Key 저장

1. **시크릿 유형**: "기타 유형의 시크릿" 선택

2. **시크릿 이름**
   ```
   tech-n-ai/openai-api-key/{환경명}
   ```

3. **시크릿 값** (JSON 형식)
   ```json
   {
     "apiKey": "sk-your-openai-api-key"
   }
   ```

#### 환경변수와 Secrets Manager 연동

ECS Fargate 태스크 정의에서 Secrets Manager 연동:

```json
{
  "containerDefinitions": [
    {
      "name": "api-gateway",
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "JWT_SECRET_KEY",
          "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/jwt-secret-key/prod:secretKey::"
        },
        {
          "name": "AURORA_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/aurora/prod:password::"
        },
        {
          "name": "REDIS_PASSWORD",
          "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/redis/prod:password::"
        }
      ]
    }
  ]
}
```

**주의사항**:
- Secrets Manager ARN은 실제 생성된 시크릿의 ARN으로 교체 필요
- IAM 역할에 Secrets Manager 읽기 권한 필요

**참고 문서**:
- [AWS Secrets Manager 사용자 가이드](https://docs.aws.amazon.com/ko_kr/secretsmanager/latest/userguide/)
- [ECS에서 Secrets Manager 사용](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/specifying-sensitive-data-secrets.html)

---

### 2.5 Gateway 및 API 서버 ECS Fargate 구성

#### 아키텍처 선택: ECS Fargate vs EC2

**ECS Fargate 선택 근거** (AWS 공식 베스트 프랙티스):

1. **마이크로서비스 아키텍처에 적합**
   - 각 서비스(Gateway, auth, archive, contest, news, chatbot)를 독립적으로 스케일링 가능
   - 서비스별 리소스 할당 및 관리 용이
   - AWS 공식 문서: [Deploy Java microservices on Amazon ECS using AWS Fargate](https://docs.aws.amazon.com/prescriptive-guidance/latest/patterns/deploy-java-microservices-on-amazon-ecs-using-aws-fargate.html)

2. **운영 부담 최소화**
   - 서버리스 인프라 관리 불필요 (EC2 인스턴스 패치, OS 관리 불필요)
   - AWS가 인프라 프로비저닝 및 관리 담당
   - 개발팀은 애플리케이션 개발에 집중 가능

3. **비용 효율성** (가변 트래픽 환경)
   - 사용한 만큼만 비용 지불 (태스크 실행 시간 기준)
   - 트래픽이 예측 불가능하거나 가변적일 때 유리
   - EC2는 높은 활용률(60-70% 이상)이 예측 가능할 때 더 저렴

4. **서비스 격리 및 보안**
   - 각 태스크가 독립적인 마이크로VM에서 실행
   - 서비스 간 격리로 보안 강화

**EC2를 고려해야 하는 경우**:
- 24/7 고정 트래픽으로 활용률이 60-70% 이상 예측 가능한 경우
- 예약 인스턴스로 비용 절감 가능한 경우
- GPU 또는 특수 인스턴스 타입이 필요한 경우

**프로젝트 특성상 ECS Fargate 권장**:
- 6개 서비스(Gateway + 5개 API 서버)를 독립적으로 운영
- 각 서비스의 트래픽 패턴이 다를 수 있음
- 운영 부담 최소화가 중요

**참고 문서**:
- [AWS ECS Fargate vs EC2 비교](https://aws.amazon.com/blogs/containers/theoretical-cost-optimization-by-amazon-ecs-launch-type-fargate-vs-ec2/)
- [Amazon ECS Fargate 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/AWS_Fargate.html)

---

#### ECS 클러스터 생성

1. **AWS 콘솔 접속**
   - AWS Management Console → ECS 서비스 선택

2. **클러스터 생성**
   - "클러스터 생성" 버튼 클릭

3. **클러스터 구성**
   ```
   클러스터 이름: tech-n-ai-api-cluster-{환경명}
   인프라: AWS Fargate (서버리스)
   ```

4. **태그 설정** (선택사항)
   ```
   Environment: {환경명}
   Project: tech-n-ai
   ```

5. **생성 완료**
   - "생성" 클릭

**주의사항**:
- Fargate는 서버리스이므로 별도의 EC2 인스턴스 관리 불필요
- 클러스터는 논리적 그룹핑 용도

---

#### Amazon ECR (Elastic Container Registry) 설정

컨테이너 이미지를 저장할 ECR 리포지토리를 생성합니다.

1. **AWS 콘솔 접속**
   - AWS Management Console → ECR 서비스 선택

2. **리포지토리 생성**

**Gateway 서버 리포지토리**:
```
리포지토리 이름: tech-n-ai/api-gateway
가시성: 프라이빗
태그 불변성: 비활성화 (개발 환경), 활성화 (프로덕션)
이미지 스캔: 활성화 권장
```

**API 서버 리포지토리** (각 서비스별):
```
tech-n-ai/api-auth
tech-n-ai/api-archive
tech-n-ai/api-contest
tech-n-ai/api-news
tech-n-ai/api-chatbot
```

3. **이미지 푸시 명령어 확인**
   - 리포지토리 선택 → "푸시 명령어 보기" 클릭
   - 표시된 명령어를 사용하여 이미지 푸시

**이미지 빌드 및 푸시 예시**:
```bash
# Dockerfile이 있는 디렉토리에서 실행
# 1. AWS CLI 로그인
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin <계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com

# 2. 이미지 빌드
docker build -t tech-n-ai/api-gateway:latest -f api/gateway/Dockerfile .

# 3. 태그 지정
docker tag tech-n-ai/api-gateway:latest <계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com/tech-n-ai/api-gateway:latest

# 4. 푸시
docker push <계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com/tech-n-ai/api-gateway:latest
```

**참고 문서**:
- [Amazon ECR 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonECR/latest/userguide/)

---

#### Gateway 서버 ECS 태스크 정의

1. **ECS 콘솔** → **태스크 정의** → **새 태스크 정의 생성**

2. **태스크 정의 구성**
   ```
   제품군: Fargate
   태스크 정의 이름: tech-n-ai-api-gateway-{환경명}
   태스크 역할: ECS 태스크 실행 역할 (Secrets Manager 읽기 권한 포함)
   작업 실행 역할: ECS 작업 실행 역할
   ```

3. **태스크 크기**
   ```
   태스크 메모리: 2GB (2048)
   태스크 CPU: 1 vCPU (1024)
   ```

4. **컨테이너 정의 추가**
   ```
   컨테이너 이름: api-gateway
   이미지 URI: <계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com/tech-n-ai/api-gateway:latest
   필수 항목: 예
   ```

5. **컨테이너 세부 정보**
   ```
   메모리 제한: 2048 (하드 제한)
   CPU 단위: 1024
   ```

6. **포트 매핑**
   ```
   컨테이너 포트: 8081
   프로토콜: TCP
   ```

7. **환경 변수**
   ```json
   [
     {
       "name": "SPRING_PROFILES_ACTIVE",
       "value": "prod"
     },
     {
       "name": "AURORA_WRITER_ENDPOINT",
       "value": "aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com"
     },
     {
       "name": "AURORA_READER_ENDPOINT",
       "value": "aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com"
     },
     {
       "name": "AURORA_USERNAME",
       "value": "admin"
     },
     {
       "name": "AURORA_OPTIONS",
       "value": "useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
     },
     {
       "name": "MONGODB_ATLAS_CONNECTION_STRING",
       "value": "<Secrets Manager에서 가져오기>"
     },
     {
       "name": "KAFKA_BOOTSTRAP_SERVERS",
       "value": "<Secrets Manager에서 가져오기>"
     },
     {
       "name": "REDIS_HOST",
       "value": "tech-n-ai-redis.xxxxx.cache.amazonaws.com"
     },
     {
       "name": "REDIS_PORT",
       "value": "6379"
     }
   ]
   ```

8. **Secrets Manager 연동**
   ```json
   [
     {
       "name": "JWT_SECRET_KEY",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/jwt-secret-key/prod:secretKey::"
     },
     {
       "name": "AURORA_PASSWORD",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/aurora/prod:password::"
     },
     {
       "name": "REDIS_PASSWORD",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/redis/prod:password::"
     }
   ]
   ```

9. **로깅 구성**
   ```
   로그 드라이버: awslogs
   로그 옵션:
     awslogs-group: /ecs/tech-n-ai-api-gateway
     awslogs-region: ap-northeast-2
     awslogs-stream-prefix: ecs
   ```

10. **네트워크 모드**
    ```
    네트워크 모드: awsvpc (Fargate 필수)
    ```

11. **생성 완료**
    - "생성" 클릭

---

#### Gateway 서버 ECS 서비스 생성

1. **ECS 콘솔** → 클러스터 선택 → **서비스** 탭 → **서비스 생성**

2. **서비스 구성**
   ```
   시작 유형: Fargate
   태스크 정의: tech-n-ai-api-gateway-{환경명}
   서비스 이름: tech-n-ai-api-gateway-service-{환경명}
   서비스 유형: REPLICA
   태스크 수: 2 (고가용성, 최소 2개)
   ```

3. **네트워크 구성**
   ```
   VPC: Gateway 및 API 서버와 동일한 VPC 선택
   서브넷: 프라이빗 서브넷 선택 (최소 2개 가용 영역)
   보안 그룹: 새로 생성 또는 기존 사용
     - 인바운드: ALB 보안 그룹에서 포트 8081 허용
     - 아웃바운드: 모든 트래픽 허용
   자동 할당 공용 IP: 아니오 (프라이빗 서브넷 사용)
   ```

4. **로드 밸런싱 구성**
   ```
   로드 밸런서 유형: Application Load Balancer
   로드 밸런서: tech-n-ai-alb-{환경명} 선택
   리스너: HTTPS:443
   대상 그룹: tech-n-ai-gateway-tg-{환경명} 선택
   ```

5. **서비스 검색** (선택사항)
   - Cloud Map을 사용하여 서비스 간 통신 설정 가능
   - 현재는 ALB를 통한 통신 사용

6. **자동 스케일링 구성**
   ```
   자동 스케일링 활성화: 예
   최소 태스크 수: 2
   최대 태스크 수: 10
   대상 추적 정책: 
     - CPU 사용률: 70%
     - 메모리 사용률: 80%
   ```

7. **배포 구성**
   ```
   배포 유형: 롤링 업데이트
   최소 정상 비율: 100%
   최대 백분율: 200%
   ```

8. **생성 완료**
   - "서비스 생성" 클릭

---

#### API 서버 ECS 태스크 정의 및 서비스 생성

각 API 서버(auth, archive, contest, news, chatbot)에 대해 동일한 방식으로 태스크 정의 및 서비스를 생성합니다.

**태스크 정의 예시 (api-auth)**:
```
태스크 정의 이름: tech-n-ai-api-auth-{환경명}
컨테이너 이름: api-auth
이미지 URI: <계정ID>.dkr.ecr.ap-northeast-2.amazonaws.com/tech-n-ai/api-auth:latest
포트: 8080 (api/gateway/README.md의 환경별 백엔드 URL 참고)
태스크 메모리: 2GB
태스크 CPU: 1 vCPU
```

**서비스 이름 규칙**:
```
tech-n-ai-api-auth-service-{환경명}
tech-n-ai-api-archive-service-{환경명}
tech-n-ai-api-contest-service-{환경명}
tech-n-ai-api-news-service-{환경명}
tech-n-ai-api-chatbot-service-{환경명}
```

**서비스 간 통신 설정**:

Gateway 서버의 `application-dev.yml` 설정 (`api/gateway/src/main/resources/application-dev.yml` 참고):

```yaml
gateway:
  routes:
    auth:
      uri: http://api-auth-service:8080
    archive:
      uri: http://api-archive-service:8080
    contest:
      uri: http://api-contest-service:8080
    news:
      uri: http://api-news-service:8080
    chatbot:
      uri: http://api-chatbot-service:8080
```

**서비스 디스커버리 옵션**:

1. **옵션 A: ECS Service 이름 사용** (현재 방식)
   - 같은 VPC 내에서 ECS 서비스 이름을 DNS 이름으로 사용 가능
   - 예: `http://api-auth-service:8080`
   - 추가 설정 불필요

2. **옵션 B: AWS Cloud Map 사용** (고급)
   - 서비스 메시 및 고급 라우팅이 필요한 경우
   - 현재는 옵션 A로 충분

**보안 그룹 설정**:

각 API 서버의 보안 그룹:
```
인바운드 규칙:
  - Gateway 서버 보안 그룹에서 포트 8080 허용
  - 같은 보안 그룹 내 서비스 간 통신 허용
```

---

#### 자동 스케일링 설정

각 서비스별로 자동 스케일링을 설정합니다.

1. **ECS 콘솔** → 서비스 선택 → **자동 스케일링** 탭

2. **자동 스케일링 구성**
   ```
   최소 태스크 수: 1 (개발), 2 (프로덕션)
   최대 태스크 수: 5 (개발), 10 (프로덕션)
   대상 추적 정책:
     - CPU 사용률: 70%
     - 메모리 사용률: 80%
   ```

3. **CloudWatch 알림 설정**
   - 스케일링 이벤트 발생 시 SNS 알림 발송 (선택사항)

**서비스별 스케일링 권장사항**:

| 서비스 | 최소 태스크 | 최대 태스크 | 비고 |
|--------|------------|------------|------|
| Gateway | 2 | 10 | 모든 트래픽 진입점 |
| Auth | 1 | 5 | 로그인/회원가입 트래픽 |
| Archive | 1 | 5 | 사용자별 데이터 접근 |
| Contest | 1 | 3 | 공개 API, 읽기 위주 |
| News | 1 | 3 | 공개 API, 읽기 위주 |
| Chatbot | 1 | 5 | LLM 호출, 응답 시간 길 수 있음 |

---

#### 비용 최적화 설정

1. **태스크 크기 최적화**
   - CloudWatch 메트릭으로 실제 사용량 확인
   - 사용량에 맞춰 태스크 크기 조정
   - 예: CPU 50%, 메모리 1GB 사용 시 → CPU 0.5 vCPU, 메모리 2GB 할당

2. **Fargate Spot 활용** (비용 최적화)
   - 개발 환경 또는 비중요 서비스에 Spot 인스턴스 활용
   - Spot 인스턴스는 On-Demand 대비 최대 70% 할인
   - **주의**: Spot 인스턴스는 중단될 수 있으므로 프로덕션 Gateway 서버에는 권장하지 않음

3. **용량 공급자 전략 설정**
   ```
   용량 공급자: FARGATE_SPOT (개발), FARGATE (프로덕션)
   ```

**참고 문서**:
- [Amazon ECS Fargate 가격](https://aws.amazon.com/ko/fargate/pricing/)
- [ECS Fargate 비용 최적화](https://docs.aws.amazon.com/prescriptive-guidance/latest/optimize-costs-microsoft-workloads/optimizer-ecs-fargate.html)

---

#### 모니터링 및 로깅 설정

1. **CloudWatch Logs**
   - 각 서비스의 로그가 자동으로 CloudWatch Logs에 저장됨
   - 로그 그룹: `/ecs/tech-n-ai-api-{서비스명}`

2. **CloudWatch Container Insights**
   - ECS 서비스 메트릭 자동 수집
   - CPU, 메모리, 네트워크 사용량 모니터링

3. **알림 설정**
   - 서비스 실패 시 SNS 알림 발송
   - CPU/메모리 임계값 초과 시 알림

---

#### 배포 전략

1. **롤링 업데이트** (기본)
   - 새 태스크를 점진적으로 배포
   - 기존 태스크는 새 태스크가 정상 동작 확인 후 종료

2. **Blue/Green 배포** (고급)
   - 별도의 태스크 정의로 새 버전 배포
   - 검증 후 트래픽 전환

**참고 문서**:
- [Amazon ECS 배포 유형](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/deployment-types.html)
- [ECS 서비스 자동 스케일링](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/service-auto-scaling.html)

---

## 3. Spring Batch 애플리케이션 스케줄링 가이드

### 3.1 EventBridge Scheduler 설정

#### 스케줄 생성

1. **AWS 콘솔 접속**
   - AWS Management Console → EventBridge 서비스 → Scheduler 선택

2. **스케줄 생성**
   - "스케줄 생성" 버튼 클릭

3. **스케줄 정의**
   ```
   이름: tech-n-ai-batch-contest-codeforces-{환경명}
   설명: Codeforces Contest 데이터 수집 배치 잡
   ```

4. **스케줄 패턴**
   - **일정 유형**: 반복 일정
   - **일정 표현식**: Cron 표현식 또는 Rate 표현식
     ```
     # 매일 오전 2시 실행
     cron(0 2 * * ? *)
     
     # 또는 Rate 표현식 (매 6시간마다)
     rate(6 hours)
     ```

5. **대상 선택**
   - **대상 유형**: ECS Task
   - **클러스터**: 배치 작업용 ECS 클러스터 선택
   - **태스크 정의**: 배치 작업용 태스크 정의 선택
   - **태스크 개수**: 1
   - **플랫폼 버전**: LATEST

6. **태스크 오버라이드**
   - **컨테이너 오버라이드**: 
     ```json
     {
       "name": "batch-source",
       "command": [
         "--spring.profiles.active=prod",
         "--job.name=contest.codeforces.job"
       ],
       "environment": [
         {
           "name": "SPRING_PROFILES_ACTIVE",
           "value": "prod"
         }
       ],
       "secrets": [
         {
           "name": "AURORA_PASSWORD",
           "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/aurora/prod:password::"
         }
       ]
     }
     ```

7. **비용 최적화 설정**
   - **용량 공급자 전략**: Spot 인스턴스 우선 사용
   - **태스크 크기**: 최소 필요 리소스만 할당

8. **생성 완료**
   - "스케줄 생성" 클릭

#### 배치 잡별 스케줄 예시

프로젝트의 배치 잡 이름 상수 (`batch/source/README.md` 참고):

**Contest 데이터 수집 (12개 출처)**:
- `contest.codeforces.job`: 매일 오전 2시
- `contest.github.job`: 매일 오전 3시
- `contest.kaggle.job`: 매일 오전 4시
- `contest.producthunt.job`: 매일 오전 5시
- `contest.reddit.job`: 매일 오전 6시
- `contest.hackernews.job`: 매일 오전 7시
- `contest.devto.job`: 매일 오전 8시
- `contest.leetcode.job`: 매일 오전 9시
- `contest.gsoc.job`: 매일 오전 10시
- `contest.devpost.job`: 매일 오전 11시
- `contest.mlh.job`: 매일 오후 12시
- `contest.atcoder.job`: 매일 오후 1시

**News 데이터 수집 (8개 출처)**:
- `news.newsapi.job`: 매일 오후 2시
- `news.devto.job`: 매일 오후 3시
- `news.reddit.job`: 매일 오후 4시
- `news.hackernews.job`: 매일 오후 5시
- `news.techcrunch.job`: 매일 오후 6시
- `news.google.developers.job`: 매일 오후 7시
- `news.ars.technica.job`: 매일 오후 8시
- `news.medium.job`: 매일 오후 9시

**참고 문서**:
- [Amazon EventBridge Scheduler 사용자 가이드](https://docs.aws.amazon.com/ko_kr/scheduler/latest/UserGuide/)
- [배치 잡 통합 설계서](docs/step10/batch-job-integration-design.md)
- [배치 모듈 README](batch/source/README.md)

---

### 3.2 ECS Fargate 태스크 정의

#### 태스크 정의 생성

1. **AWS 콘솔 접속**
   - AWS Management Console → ECS 서비스 → 태스크 정의 선택

2. **새 태스크 정의 생성**
   - "새 태스크 정의 생성" 버튼 클릭

3. **태스크 정의 구성**
   ```
   제품군: Fargate
   태스크 정의 이름: tech-n-ai-batch-source-{환경명}
   태스크 역할: ECS 태스크 실행 역할 (Secrets Manager 읽기 권한 포함)
   작업 실행 역할: ECS 작업 실행 역할
   ```

4. **태스크 크기**
   ```
   태스크 메모리: 2GB (2048)
   태스크 CPU: 1 vCPU (1024)
   ```

5. **컨테이너 정의 추가**
   ```
   컨테이너 이름: batch-source
   이미지 URI: <ECR 리포지토리>/batch-source:latest
   필수 항목: 예
   ```

6. **컨테이너 세부 정보**
   ```
   메모리 제한: 2048 (하드 제한)
   CPU 단위: 1024
   ```

7. **환경 변수**
   ```json
   [
     {
       "name": "SPRING_PROFILES_ACTIVE",
       "value": "prod"
     },
     {
       "name": "AURORA_WRITER_ENDPOINT",
       "value": "aurora-cluster.cluster-xxxxx.ap-northeast-2.rds.amazonaws.com"
     },
     {
       "name": "AURORA_READER_ENDPOINT",
       "value": "aurora-cluster.cluster-ro-xxxxx.ap-northeast-2.rds.amazonaws.com"
     },
     {
       "name": "AURORA_USERNAME",
       "value": "admin"
     },
     {
       "name": "AURORA_OPTIONS",
       "value": "useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
     },
     {
       "name": "MONGODB_ATLAS_CONNECTION_STRING",
       "value": "<Secrets Manager에서 가져오기>"
     },
     {
       "name": "KAFKA_BOOTSTRAP_SERVERS",
       "value": "<Secrets Manager에서 가져오기>"
     },
     {
       "name": "REDIS_HOST",
       "value": "tech-n-ai-redis.xxxxx.cache.amazonaws.com"
     },
     {
       "name": "REDIS_PORT",
       "value": "6379"
     }
   ]
   ```

8. **Secrets Manager 연동**
   ```json
   [
     {
       "name": "AURORA_PASSWORD",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/aurora/prod:password::"
     },
     {
       "name": "REDIS_PASSWORD",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/redis/prod:password::"
     },
     {
       "name": "JWT_SECRET_KEY",
       "valueFrom": "arn:aws:secretsmanager:ap-northeast-2:123456789012:secret:tech-n-ai/jwt-secret-key/prod:secretKey::"
     }
   ]
   ```

9. **로깅 구성**
   ```
   로그 드라이버: awslogs
   로그 옵션:
     awslogs-group: /ecs/tech-n-ai-batch-source
     awslogs-region: ap-northeast-2
     awslogs-stream-prefix: ecs
   ```

10. **생성 완료**
    - "생성" 클릭

#### ECS 클러스터 구성

1. **ECS 콘솔** → **클러스터** → **클러스터 생성**

2. **클러스터 구성**
   ```
   클러스터 이름: tech-n-ai-batch-cluster-{환경명}
   인프라: AWS Fargate (서버리스)
   ```

3. **생성 완료**
   - "생성" 클릭

**참고 문서**:
- [Amazon ECS 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/)
- [배치 모듈 README](batch/source/README.md)

---

### 3.3 배치 잡 실행 설정

#### Job 이름 파라미터 설정

배치 잡은 `--job.name` 파라미터로 실행할 Job을 지정합니다 (`batch/source/README.md` 참고):

```bash
# EventBridge Scheduler의 태스크 오버라이드에서 설정
--spring.profiles.active=prod
--job.name=contest.codeforces.job
```

#### 환경변수 설정

필수 환경변수는 태스크 정의에서 설정하거나 Secrets Manager에서 가져옵니다.

#### 로그 설정

CloudWatch Logs에 배치 잡 실행 로그가 저장됩니다:

```
로그 그룹: /ecs/tech-n-ai-batch-source
로그 스트림: ecs/batch-source/{task-id}
```

#### 에러 처리 및 알림 설정

1. **EventBridge Scheduler** → 생성한 스케줄 선택 → **대상** 탭

2. **에러 처리 구성**
   - **Dead Letter Queue (DLQ)**: SQS 큐 선택 (선택사항)
   - **재시도 정책**: 기본값 사용

3. **CloudWatch 알림 설정**
   - EventBridge 규칙 생성하여 실패 시 SNS 알림 발송

---

### 3.4 비용 최적화 스케줄링 전략

#### Spot 인스턴스 활용

1. **ECS 클러스터** → **용량 공급자** 탭

2. **용량 공급자 추가**
   ```
   용량 공급자 이름: FARGATE_SPOT
   용량 공급자 전략: 
     - FARGATE_SPOT: 100 (우선순위)
     - FARGATE: 0 (폴백)
   ```

3. **EventBridge Scheduler 태스크 오버라이드**
   ```json
   {
     "capacityProviderStrategy": [
       {
         "capacityProvider": "FARGATE_SPOT",
         "weight": 1
       }
     ]
   }
   ```

**비용 절감 효과**:
- Spot 인스턴스는 On-Demand 대비 최대 70% 할인
- 배치 작업은 중단 가능하므로 Spot 인스턴스 적합

#### 태스크 크기 최적화

1. **최소 필요 리소스만 할당**
   ```
   태스크 메모리: 2GB (실제 사용량에 따라 조정)
   태스크 CPU: 1 vCPU (실제 사용량에 따라 조정)
   ```

2. **모니터링을 통한 최적화**
   - CloudWatch 메트릭으로 실제 사용량 확인
   - 사용량에 맞춰 태스크 크기 조정

#### 스케줄 최적화

1. **필요한 시간에만 실행**
   - 배치 잡 실행 시간을 분산하여 리소스 경합 최소화
   - 예: Contest 잡은 오전, News 잡은 오후 실행

2. **실행 빈도 조정**
   - 데이터 업데이트 주기에 맞춰 실행 빈도 조정
   - 불필요한 실행 제거

#### 불필요한 리소스 정리

1. **완료된 태스크 자동 정리**
   - ECS 태스크 정의에서 태스크 정리 설정

2. **CloudWatch Logs 보존 기간 설정**
   - 로그 그룹의 보존 기간 설정 (예: 7일)

**참고 문서**:
- [ECS Fargate Spot 가격](https://aws.amazon.com/ecs/pricing/)

---

## 4. 비용 절감 전략 가이드

### 4.1 Aurora MySQL 비용 최적화

#### 인스턴스 크기 선택

**개발 환경**:
- `db.t3.medium` 또는 `db.t4g.medium` (ARM 기반, 비용 효율적)
- 예상 비용: 월 $50-100

**프로덕션 환경**:
- 초기: `db.r6g.large` (필요 시 스케일 업)
- 예상 비용: 월 $200-300

#### 자동 스케일링 설정

1. **RDS 콘솔** → Aurora 클러스터 선택 → **작업** → **수정**

2. **자동 스케일링 설정**
   ```
   자동 스케일링 활성화: 예
   최소 용량: 2 ACU (Aurora Capacity Units)
   최대 용량: 16 ACU
   ```

**비용 절감 효과**:
- 트래픽이 적은 시간에는 자동으로 스케일 다운
- 트래픽이 많은 시간에는 자동으로 스케일 업

#### 예약 인스턴스 활용

장기 운영 시 (1년 이상) 예약 인스턴스 활용:

1. **RDS 콘솔** → **예약** → **예약 구매**

2. **예약 구성**
   ```
   인스턴스 클래스: db.r6g.large
   약정 기간: 1년 또는 3년
   결제 옵션: 선결제 (최대 할인)
   ```

**비용 절감 효과**:
- 1년 약정: 최대 40% 할인
- 3년 약정: 최대 55% 할인

#### 스토리지 자동 스케일링

1. **RDS 콘솔** → Aurora 클러스터 선택 → **작업** → **수정**

2. **스토리지 설정**
   ```
   자동 스케일링 활성화: 예
   최대 스토리지 크기: 128GB (필요에 따라 조정)
   ```

**비용 절감 효과**:
- 초기 스토리지 비용 최소화
- 필요 시 자동으로 스토리지 확장

#### 불필요한 Reader 인스턴스 제거

개발 환경에서는 Reader 인스턴스가 불필요할 수 있습니다:

1. **RDS 콘솔** → Aurora 클러스터 선택 → **인스턴스** 탭

2. **Reader 인스턴스 삭제** (개발 환경만)

**비용 절감 효과**:
- Reader 인스턴스 비용 절감 (월 $50-200)

#### 모니터링을 통한 리소스 최적화

1. **CloudWatch 메트릭 확인**
   - CPU 사용률
   - 메모리 사용률
   - 연결 수

2. **사용률이 낮은 경우**
   - 인스턴스 크기 다운그레이드 고려

**참고 문서**:
- [Amazon Aurora 가격](https://aws.amazon.com/ko/rds/aurora/pricing/)

---

### 4.2 MSK 비용 최적화

#### 브로커 인스턴스 타입 선택

**개발 환경**:
- `kafka.t3.small` (2 vCPU, 2GB RAM)
- 예상 비용: 월 $50-100 (브로커 3개 기준)

**프로덕션 환경**:
- `kafka.m5.large` (2 vCPU, 8GB RAM)
- 예상 비용: 월 $200-300 (브로커 3개 기준)

#### 스토리지 최적화

1. **MSK 콘솔** → 클러스터 선택 → **속성** 탭

2. **스토리지 설정**
   ```
   스토리지 타입: EBS gp3 (비용 효율적)
   스토리지 크기: 100GB (초기, 필요 시 확장)
   ```

**비용 절감 효과**:
- gp3는 gp2 대비 비용 효율적
- 필요한 만큼만 스토리지 할당

#### 불필요한 토픽 정리

1. **Kafka 토픽 목록 확인**
   ```bash
   kafka-topics.sh --list --bootstrap-server <MSK_BROKER_ENDPOINT>
   ```

2. **사용하지 않는 토픽 삭제**
   ```bash
   kafka-topics.sh --delete \
     --bootstrap-server <MSK_BROKER_ENDPOINT> \
     --topic <토픽명>
   ```

#### 데이터 보존 정책 설정

1. **토픽별 보존 정책 설정**
   ```bash
   kafka-configs.sh --alter \
     --bootstrap-server <MSK_BROKER_ENDPOINT> \
     --entity-type topics \
     --entity-name <토픽명> \
     --add-config retention.ms=604800000  # 7일
   ```

**비용 절감 효과**:
- 오래된 데이터 자동 삭제로 스토리지 비용 절감

**참고 문서**:
- [Amazon MSK 가격](https://aws.amazon.com/ko/msk/pricing/)

---

### 4.3 ElastiCache Redis 비용 최적화

#### 노드 인스턴스 타입 선택

**개발 환경**:
- `cache.t3.micro` 또는 `cache.t3.small`
- 예상 비용: 월 $10-20

**프로덕션 환경**:
- `cache.r6g.large` (초기, 필요 시 스케일 업)
- 예상 비용: 월 $100-200

#### 예약 노드 활용

장기 운영 시 (1년 이상) 예약 노드 활용:

1. **ElastiCache 콘솔** → **예약** → **예약 구매**

2. **예약 구성**
   ```
   노드 타입: cache.r6g.large
   약정 기간: 1년 또는 3년
   결제 옵션: 선결제 (최대 할인)
   ```

**비용 절감 효과**:
- 1년 약정: 최대 40% 할인
- 3년 약정: 최대 55% 할인

#### 불필요한 데이터 정리 (TTL 설정)

프로젝트의 Redis 사용 패턴에 따라 TTL이 설정되어 있지만, 추가 정리 필요 시:

1. **Redis CLI 접속**
   ```bash
   redis-cli -h <REDIS_HOST> -p 6379 -a <REDIS_PASSWORD>
   ```

2. **TTL 확인 및 설정**
   ```bash
   TTL oauth:state:{state_value}  # TTL 확인
   EXPIRE oauth:state:{state_value} 600  # 10분으로 설정
   ```

#### 모니터링을 통한 리소스 최적화

1. **CloudWatch 메트릭 확인**
   - CPU 사용률
   - 메모리 사용률
   - 캐시 적중률

2. **사용률이 낮은 경우**
   - 노드 타입 다운그레이드 고려

**참고 문서**:
- [Amazon ElastiCache 가격](https://aws.amazon.com/ko/elasticache/pricing/)

---

### 4.4 ECS Fargate 비용 최적화

#### 태스크 크기 최적화

AWS Compute Optimizer를 사용하여 태스크 크기를 최적화합니다:

1. **CloudWatch 메트릭 확인**
   - CPU 사용률
   - 메모리 사용률
   - 실제 사용량 측정 (최소 1주일 이상)

2. **Compute Optimizer 활용**
   - AWS Compute Optimizer가 ECS Fargate 태스크 분석
   - 권장 태스크 크기 제안
   - **비용 절감 효과**: 30-70% 절감 가능

3. **태스크 크기 조정 예시**
   ```
   실제 사용량: CPU 50%, 메모리 1GB
   현재 태스크 크기: CPU 1 vCPU, 메모리 2GB
   권장 태스크 크기: CPU 0.5 vCPU (512), 메모리 2GB (안전 마진 포함)
   ```

**비용 절감 효과**:
- 과도한 리소스 할당 방지
- 필요한 만큼만 비용 지불
- 태스크 크기 최적화만으로도 30-70% 비용 절감 가능

**참고 문서**:
- [AWS Compute Optimizer for ECS Fargate](https://docs.aws.amazon.com/prescriptive-guidance/latest/optimize-costs-microsoft-workloads/optimizer-ecs-fargate.html)

#### Spot 인스턴스 활용

**배치 작업** (3.4절 참고):
- Spot 인스턴스 활용 적합
- On-Demand 대비 최대 70% 할인
- 배치 작업은 중단 가능하므로 Spot 인스턴스 적합

**API 서버** (신중한 선택 필요):
- **개발 환경**: Spot 인스턴스 활용 가능
- **프로덕션 환경**: Gateway 서버는 On-Demand 권장 (중단 불가)
- **프로덕션 환경**: API 서버는 트래픽 패턴에 따라 선택
  - Contest, News (공개 API, 읽기 위주): Spot 고려 가능
  - Auth, Archive, Chatbot (사용자 데이터): On-Demand 권장

**용량 공급자 전략 설정**:
```
개발 환경:
  - FARGATE_SPOT: 100 (우선순위)
  - FARGATE: 0 (폴백)

프로덕션 환경:
  - Gateway: FARGATE만 사용
  - API 서버: FARGATE_SPOT 50%, FARGATE 50% (혼합)
```

#### ARM/Graviton 아키텍처 활용

Spring Boot 애플리케이션이 ARM64를 지원하는 경우:

1. **태스크 정의에서 아키텍처 설정**
   ```
   CPU 아키텍처: ARM64
   ```

2. **비용 절감 효과**
   - Graviton2/3는 x86 대비 15-40% 비용 절감
   - 동일한 성능에서 더 낮은 비용

3. **이미지 빌드**
   ```bash
   # ARM64 이미지 빌드
   docker buildx build --platform linux/arm64 -t tech-n-ai/api-gateway:latest .
   ```

**주의사항**:
- 모든 의존성이 ARM64를 지원하는지 확인 필요
- 로컬 개발 환경도 ARM64로 테스트 권장

#### Savings Plans 활용

예측 가능한 기본 사용량이 있는 경우:

1. **Compute Savings Plans 구매**
   - ECS Fargate, EC2, Lambda에 적용
   - 1년 약정: 최대 17% 할인
   - 3년 약정: 최대 52% 할인

2. **적용 범위**
   - Fargate On-Demand 사용량에 자동 적용
   - Spot 인스턴스는 적용되지 않음

#### 서비스별 최적화 전략

| 서비스 | 태스크 크기 | 용량 공급자 | 비고 |
|--------|------------|------------|------|
| Gateway | 1 vCPU, 2GB | FARGATE (On-Demand) | 모든 트래픽 진입점, 중단 불가 |
| Auth | 0.5 vCPU, 2GB | FARGATE_SPOT 50% | 로그인 트래픽, 일시 중단 허용 가능 |
| Archive | 0.5 vCPU, 2GB | FARGATE (On-Demand) | 사용자 데이터, 중단 최소화 |
| Contest | 0.25 vCPU, 1GB | FARGATE_SPOT 100% | 읽기 위주, 중단 허용 가능 |
| News | 0.25 vCPU, 1GB | FARGATE_SPOT 100% | 읽기 위주, 중단 허용 가능 |
| Chatbot | 1 vCPU, 2GB | FARGATE (On-Demand) | LLM 호출, 응답 시간 중요 |

#### 자동 스케일링 최적화

1. **스케일 다운 최적화**
   - 트래픽이 적은 시간에 자동으로 태스크 수 감소
   - 최소 태스크 수를 낮게 설정 (개발: 0, 프로덕션: 1-2)

2. **스케일 업 임계값 조정**
   ```
   CPU 사용률: 70% (기본값)
   메모리 사용률: 80% (기본값)
   ```
   - 실제 트래픽 패턴에 맞춰 조정
   - 너무 낮으면 불필요한 스케일 업 발생

#### 불필요한 리소스 정리

1. **완료된 태스크 자동 정리**
   - ECS 서비스 설정에서 태스크 정리 활성화

2. **CloudWatch Logs 보존 기간 설정**
   - 로그 그룹별 보존 기간 설정 (예: 7일)
   - 오래된 로그 자동 삭제

3. **사용하지 않는 태스크 정의 정리**
   - 오래된 태스크 정의 버전 삭제

**참고 문서**:
- [Amazon ECS Fargate 가격](https://aws.amazon.com/ko/fargate/pricing/)
- [ECS Fargate 비용 최적화 모범 사례](https://docs.aws.amazon.com/prescriptive-guidance/latest/optimize-costs-microsoft-workloads/optimizer-ecs-fargate.html)

---

### 4.5 ALB 비용 최적화

#### 사용량 기반 최적화

ALB는 사용량 기반 과금이므로:

1. **불필요한 리스너 제거**
   - HTTP 리스너는 HTTPS 리다이렉트용으로만 사용
   - 추가 리스너 제거

2. **트래픽 최적화**
   - CDN 사용으로 ALB 트래픽 감소
   - 정적 리소스는 S3 + CloudFront 사용

#### 모니터링을 통한 최적화

1. **CloudWatch 메트릭 확인**
   - 요청 수
   - 처리된 바이트 수
   - 활성 연결 수

2. **비용 분석**
   - AWS Cost Explorer에서 ALB 비용 분석
   - 불필요한 트래픽 원인 파악

**참고 문서**:
- [AWS Application Load Balancer 가격](https://aws.amazon.com/ko/elasticloadbalancing/pricing/)

---

### 4.6 종합 비용 절감 전략

#### 환경별 리소스 크기 권장사항

**개발 환경 (Dev)**:
```
Aurora: db.t3.medium (1개)
MSK: kafka.t3.small (3개)
ElastiCache: cache.t3.small (1개)
ECS Fargate: Spot 인스턴스, 1 vCPU, 2GB
예상 월 비용: $200-300
```

**스테이징 환경 (Beta)**:
```
Aurora: db.t3.medium (1개, Reader 1개)
MSK: kafka.t3.small (3개)
ElastiCache: cache.t3.medium (1개)
ECS Fargate: Spot 인스턴스, 1 vCPU, 2GB
예상 월 비용: $300-400
```

**프로덕션 환경 (Prod)**:
```
Aurora: db.r6g.large (1개, Reader 1개, 예약 인스턴스)
MSK: kafka.m5.large (3개)
ElastiCache: cache.r6g.large (1개, 예약 노드)
ECS Fargate: 
  - Gateway: On-Demand, 2 태스크 (1 vCPU, 2GB)
  - API 서버: On-Demand + Spot 혼합, 5-10 태스크 (0.25-1 vCPU, 1-2GB)
  - 배치: Spot 인스턴스
ALB: 1개
예상 월 비용: $1000-1500 (예약 인스턴스 및 Savings Plans 적용 시)
```

#### 자동 스케일링 설정 가이드

1. **Aurora 자동 스케일링**
   - 최소 2 ACU, 최대 16 ACU
   - CPU 사용률 기반 자동 스케일링

2. **ECS Fargate 자동 스케일링**
   - 최소 0, 최대 10
   - CPU 사용률 70% 기준

#### 예약 인스턴스 활용 가이드

1. **1년 이상 운영 예상 시**
   - Aurora 예약 인스턴스 구매
   - ElastiCache 예약 노드 구매

2. **비용 절감 효과**
   - 최대 40-55% 할인

#### 사용하지 않는 리소스 정리 방법

1. **정기적인 리소스 감사** (월 1회)
   - 사용하지 않는 보안 그룹 삭제
   - 사용하지 않는 스냅샷 삭제
   - 사용하지 않는 CloudWatch Logs 로그 그룹 삭제

2. **자동 정리 스크립트**
   ```bash
   # 30일 이상 사용하지 않는 스냅샷 삭제
   aws rds describe-db-snapshots \
     --query 'DBSnapshots[?SnapshotCreateTime<`2024-01-01`].DBSnapshotIdentifier' \
     --output text | xargs -I {} aws rds delete-db-snapshot --db-snapshot-identifier {}
   ```

#### 비용 모니터링 및 알림 설정

1. **AWS Cost Explorer 설정**
   - 일일/주간/월간 비용 리포트 확인
   - 서비스별 비용 분석

2. **CloudWatch 알림 설정**
   - 예산 초과 시 SNS 알림 발송
   - 비용 임계값 설정 (예: 월 $1000 초과 시 알림)

3. **AWS Budgets 설정**
   - 월 예산 설정
   - 예산 초과 시 알림

**참고 문서**:
- [AWS 비용 최적화 모범 사례](https://aws.amazon.com/ko/pricing/cost-optimization/)

---

## 참고 문서

### 프로젝트 내부 문서

- [Gateway 설계서](docs/step14/gateway-design.md)
- [Gateway README](api/gateway/README.md)
- [배치 잡 통합 설계서](docs/step10/batch-job-integration-design.md)
- [배치 모듈 README](batch/source/README.md)
- [Kafka 동기화 설계서](docs/step11/cqrs-kafka-sync-design.md)
- [Redis 최적화 가이드](docs/step7/redis-optimization-best-practices.md)
- [Aurora 스키마 설계서](docs/step1/3.%20aurora-schema-design.md)
- [데이터소스 구축 가이드](docs/datasource-setup-guide.md)

### AWS 공식 문서

- [AWS Application Load Balancer 사용자 가이드](https://docs.aws.amazon.com/ko_kr/elasticloadbalancing/latest/application/introduction.html)
- [Amazon Aurora MySQL 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonRDS/latest/AuroraUserGuide/Aurora.AuroraMySQL.html)
- [Amazon MSK 개발자 가이드](https://docs.aws.amazon.com/ko_kr/msk/latest/developerguide/)
- [Amazon ElastiCache for Redis 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonElastiCache/latest/red-ug/)
- [AWS Secrets Manager 사용자 가이드](https://docs.aws.amazon.com/ko_kr/secretsmanager/latest/userguide/)
- [Amazon EventBridge Scheduler 사용자 가이드](https://docs.aws.amazon.com/ko_kr/scheduler/latest/UserGuide/)
- [Amazon ECS 사용자 가이드](https://docs.aws.amazon.com/ko_kr/AmazonECS/latest/developerguide/)

---

**작성 완료일**: 2026-01-XX  
**최종 업데이트**: 2026-01-XX
