# 04. 데이터베이스 · 캐시 · 스토리지 설계

## 역할
당신은 데이터 계층 설계를 담당하는 AWS DevOps 엔지니어입니다. CQRS(Aurora + MongoDB Atlas) 구조를 AWS 관리형으로 안정적으로 운영하는 설계서를 작성합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/04-data/` 에 저장하세요.

### 산출물 1: `aurora-mysql.md` — 쓰기 계층
1. **엔진 선정**: Aurora MySQL vs Aurora Serverless v2 vs RDS for MySQL — 현재 워크로드 특성(배치 + OLTP)에 맞춰 선정
2. **클러스터 구성**
   - Writer 1 + Reader 2 (AZ 분산), Aurora Serverless v2 최소/최대 ACU
   - 엔진 버전 (LTS 확인), 파라미터 그룹 커스터마이징 (timezone, character_set, max_connections)
3. **고가용성 & DR**
   - Multi-AZ Writer failover, Global Database(DR 리전) 적용 여부
   - 자동 백업 보존 35일, 스냅샷 수동 정책, PITR
4. **보안**
   - IAM DB Authentication 활성화 검토
   - 전송 중/저장 시 암호화 (KMS CMK 분리)
   - `rds-data-api` 사용 여부
5. **Spring Boot 연동**
   - `api-auth`, `api-bookmark` 등 Writer 트래픽 모듈의 연결 풀 (HikariCP) 설정
   - Reader Endpoint를 사용하는 읽기 쿼리 분리 전략 (현재 CQRS에서 읽기는 MongoDB이지만, 보조 읽기 경로 존재 시)

### 산출물 2: `mongodb-atlas.md` — 읽기/벡터 계층
1. MongoDB Atlas는 관리형 외부 서비스 → **AWS PrivateLink** 기반 연결 설계
2. Atlas 클러스터 티어, 리전(`ap-northeast-2` 매칭), Backup Snapshot 주기
3. **Vector Search 인덱스** 설계 (langchain4j 임베딩 차원, similarity metric)
4. Spring Boot 연결 문자열 관리(Secrets Manager 연동)
5. 네트워크: Atlas PrivateLink Endpoint → VPC 내부 DNS 해석

### 산출물 3: `cache-elasticache.md`
- **엔진 선정**: `ElastiCache for Valkey`(AWS 권장·오픈소스·BSD 라이선스) vs `ElastiCache for Redis OSS`(Redis 7.x 계열). 두 서비스는 AWS 상 **별개 엔진 옵션**이므로 선정 근거를 라이선스/호환성/가격 관점에서 명시
- 현재 프로젝트의 Redis 사용 지점(`common/core/RedisConfig.java`, `api-auth/OAuthStateService`, `api-chatbot/CacheServiceImpl`, `common/kafka/IdempotencyService`, `client-slack/SlackRateLimiter`) 전부 Valkey로 무중단 전환 가능한지 검증
- 클러스터 모드 on/off, 샤딩, Replica, Multi-AZ, Automatic Failover
- 용도별 분리 전략: JWT 블랙리스트 / OAuth state / 멱등성 키 / 챗봇 캐시 / 레이트리밋 — 키 네임스페이스와 TTL 정책
- 엔드포인트 TLS, AUTH 토큰 또는 RBAC

### 산출물 4: `object-storage.md`
- S3 버킷 설계 (환경·용도별 분리: 콘텐츠 업로드/정적 자산/로그/백업/IaC 상태)
- 버킷 명명 규칙, Block Public Access 강제, Versioning, Object Lock(규제 데이터 시)
- 암호화(SSE-KMS) + Bucket Key
- Lifecycle (Intelligent-Tiering / Glacier Instant Retrieval 등)
- CloudFront OAC(Origin Access Control) 사용 (OAI 금지, OAC 권장)

### 산출물 5: `data-migration.md`
- 기존 로컬 개발 DB 데이터 → AWS 이관 절차
- **AWS DMS** 사용 여부, 스키마 마이그레이션(SCT)
- 컷오버 계획(읽기 전용 모드 → 최종 동기화 → 트래픽 전환)

## 베스트 프랙티스 체크리스트
- [ ] 모든 데이터 스토어는 Private-Data 서브넷
- [ ] 저장/전송 암호화(KMS CMK)
- [ ] 자동 백업 + 크로스 리전 스냅샷 복제(프로덕션)
- [ ] Secrets Manager로 DB 자격 증명 로테이션
- [ ] Performance Insights / Enhanced Monitoring 활성화
- [ ] Aurora Backtrack(MySQL) 또는 PITR 전략 결정

## 참고 자료 (공식 출처만)
- Amazon Aurora 사용자 가이드: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/
- Aurora Best Practices: https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/AuroraMySQL.BestPractices.html
- ElastiCache for Redis/Valkey: https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/
- Amazon S3 User Guide: https://docs.aws.amazon.com/AmazonS3/latest/userguide/
- S3 Security Best Practices: https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html
- MongoDB Atlas Documentation: https://www.mongodb.com/docs/atlas/
- MongoDB Atlas AWS PrivateLink: https://www.mongodb.com/docs/atlas/security-private-endpoint/
- AWS Database Migration Service: https://docs.aws.amazon.com/dms/latest/userguide/

## 제약
- MongoDB Atlas 관련 내용은 **MongoDB 공식 문서**만 인용
- RPO ≤ 5분, RTO ≤ 30분 (프로덕션 기준)을 충족하는지 각 선택의 근거에 명시
