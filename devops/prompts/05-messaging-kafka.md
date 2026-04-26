# 05. 메시징 · Kafka(MSK) 설계

## 역할
당신은 스트리밍/메시징 플랫폼 운영을 담당하는 AWS DevOps 엔지니어입니다. 본 시스템의 CQRS 동기화(Aurora → MongoDB)와 도메인 이벤트 전달을 위한 **Amazon MSK 기반 설계**를 작성합니다.

## 작업 지시
산출물은 `/Users/m1/workspace/tech-n-ai/devops/docs/05-messaging/` 에 저장하세요.

### 산출물 1: `msk-cluster-design.md`
1. **MSK 형태 선정**
   - MSK Provisioned vs **MSK Serverless** 비교
   - 트래픽 예측(초기/중기/최대)에 기반한 선택 근거
2. **클러스터 사양**
   - 브로커 수, 인스턴스 타입(`kafka.m7g.large` 등), 스토리지(gp3), 브로커/AZ 분산
   - Kafka 버전(최신 LTS), KRaft 모드 사용 여부
3. **네트워크**
   - Private-Data 서브넷 배치, SG 규칙
   - VPC 내부 클라이언트(Spring Boot) ↔ MSK 접속 설정
4. **인증/인가**
   - IAM Access Control vs mTLS vs SASL/SCRAM 비교 및 선정
   - ACL 정책 설계(토픽 패턴별 최소 권한)
5. **암호화**
   - In-transit(TLS), At-rest(KMS CMK)
   - 클라이언트 측 암호화 필요 여부 (민감 데이터 토픽)

> **KRaft 모드 주의**: MSK Serverless는 내부적으로 KRaft를 사용하며 사용자 선택지가 없음. KRaft 선택 이슈는 **MSK Provisioned + Kafka 3.7 이상**을 택한 경우에만 적용.

### 산출물 2: `topic-and-schema.md`
1. **토픽 네이밍 규칙**
   - **현재 하드코딩된 기존 토픽**: `tech-n-ai.conversation.session.created`, `...session.updated`, `...session.deleted`, `tech-n-ai.conversation.message.created` (위치: `common/kafka/consumer/EventConsumer.java`)
   - 기존 토픽은 `{product}.{domain}.{entity}.{event}` 패턴. 환경 구분은 현재 **접두사가 아닌 클러스터 분리**로 처리 → AWS 이전 시에도 이 전제 유지할지, 아니면 `{env}.{product}.{domain}.{entity}.{event}.v{n}`로 재정의할지 결정 (후자는 마이그레이션 계획 동반 필수)
   - 결정된 규칙을 ADR로 문서화
2. **초기 토픽 카탈로그** — 위 기존 토픽 + 향후 추가될 도메인 이벤트(bookmark, auth, agent 등) 예측
3. **파티션/복제 정책**
   - 토픽별 파티션 수 결정 기준(처리량/순서 보장 요구사항)
   - Replication Factor 3, `min.insync.replicas=2`
4. **스키마 관리**
   - **AWS Glue Schema Registry** 적용 (Avro/JSON Schema/Protobuf 중 선택)
   - 스키마 호환성 모드(BACKWARD 권장) 및 근거
5. **Retention / Compaction**
   - 이벤트 소싱용 compacted topic vs 일반 스트림 구분

### 산출물 3: `consumer-and-dlq.md`
1. Spring Boot 컨슈머 그룹 네이밍 규칙 (`{service}-{purpose}`)
2. **멱등성 & 재처리** 전략(Idempotent Producer, `enable.idempotence=true`, 트랜잭션)
3. **DLQ(Dead Letter Queue)** 설계 — 실패 메시지 `{topic}.dlq` 토픽 또는 SQS로 이관
4. **백프레셔/재시도** — Spring Kafka `RetryTemplate`/`DefaultErrorHandler` 파라미터
5. **CDC 파이프라인** (Aurora → Kafka)
   - **MSK Connect + Debezium MySQL Connector** 또는 **DMS → Kinesis** 비교
   - 바이너리 로그 활성화, 슬롯/포지션 관리

### 산출물 4: `observability-and-ops.md`
- Prometheus(Open Monitoring) / CloudWatch 메트릭 수집
- 주요 SLI: Under-replicated partitions, Consumer lag, Request latency
- 알람 임계치 초안
- 브로커 패치/업그레이드 절차(Rolling)

## 베스트 프랙티스 체크리스트
- [ ] RF=3, `min.insync.replicas=2`, `acks=all`
- [ ] AZ 분산 브로커 배치
- [ ] IAM 인증 + SG 제한 + TLS 동시 적용
- [ ] 스키마 레지스트리로 스키마 드리프트 방지
- [ ] 컨슈머 lag 알람 설정
- [ ] 멀티 테넌트 시 토픽 단위 쿼터 적용

## 참고 자료 (공식 출처만)
- Amazon MSK Developer Guide: https://docs.aws.amazon.com/msk/latest/developerguide/
- MSK Best Practices: https://docs.aws.amazon.com/msk/latest/developerguide/bestpractices.html
- MSK Serverless: https://docs.aws.amazon.com/msk/latest/developerguide/serverless.html
- MSK Connect: https://docs.aws.amazon.com/msk/latest/developerguide/msk-connect.html
- AWS Glue Schema Registry: https://docs.aws.amazon.com/glue/latest/dg/schema-registry.html
- Apache Kafka Documentation: https://kafka.apache.org/documentation/
- Spring for Apache Kafka Reference: https://docs.spring.io/spring-kafka/reference/
- Debezium Documentation (MySQL Connector): https://debezium.io/documentation/reference/stable/connectors/mysql.html

## 제약
- 토픽 카탈로그는 **실제 코드에서 발견된 토픽만** 포함. 없는 경우 "추가 필요" 섹션에 설계 가이드라인 제시
- 스키마 예시는 실제 도메인 이벤트명에 맞춰 작성
