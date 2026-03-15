# Kafka Docker Compose 로컬 개발 환경 구축 가이드 작성

## 역할 및 목표
당신은 분산 시스템 인프라 전문가입니다. Spring Boot 멀티모듈 프로젝트에서 Apache Kafka를 Docker Compose로 로컬 개발 환경에 구축하고 연동하는 실무 중심의 가이드 문서를 작성해야 합니다.

## 프로젝트 컨텍스트

### 현재 프로젝트 구조
- **프레임워크**: Spring Boot 3.x, Spring Kafka
- **빌드 도구**: Gradle 멀티모듈 프로젝트
- **Kafka 모듈**: `common/kafka` (spring-kafka, kafka-streams 사용)
- **구현된 컴포넌트**:
  - EventPublisher: Kafka 메시지 발행
  - EventConsumer: Kafka 메시지 소비
  - Event 클래스들: BaseEvent, Conversation 관련 이벤트 등
  - ConversationSyncService: 동기화 서비스

### 기존 Kafka 의존성
```gradle
api 'org.springframework.kafka:spring-kafka'
api 'org.apache.kafka:kafka-streams'
testImplementation 'org.springframework.kafka:spring-kafka-test'
```

## 가이드 문서 작성 요구사항

### 1. Docker Compose 구성
다음 컴포넌트를 포함한 `docker-compose.yml` 작성:
- **Zookeeper**: Kafka 클러스터 코디네이션
- **Kafka Broker**: 메시지 브로커 (단일 브로커, 로컬 개발용)
- **Kafka UI** (선택): 관리 및 모니터링 도구 (예: kafka-ui, akhq 중 하나)

**필수 설정 항목**:
- 포트 매핑 (호스트-컨테이너)
- 볼륨 마운트 (데이터 영속성)
- 환경변수 (리스너 설정, 로그 디렉토리 등)
- 네트워크 설정
- 재시작 정책

### 2. Spring Boot 연동 설정
`application.yml` 또는 `application-local.yml`에 추가할 Kafka 설정:
- **Producer 설정**:
  - bootstrap-servers
  - key/value serializer
  - acks, retries, batch-size 등 핵심 성능 설정
- **Consumer 설정**:
  - bootstrap-servers
  - group-id
  - key/value deserializer
  - auto-offset-reset, enable-auto-commit 등
- **Admin 설정**: Topic 자동 생성 여부

### 3. 개발/테스트 워크플로우
단계별 실행 방법:
1. Docker Compose 실행 명령어
2. Kafka 클러스터 헬스체크 방법
3. Topic 생성 및 확인 명령어
4. Spring Boot 애플리케이션 실행
5. 메시지 발행/소비 테스트 방법
6. Kafka UI를 통한 모니터링 (선택)
7. 로그 확인 및 트러블슈팅

### 4. 프로젝트별 Topic 설계
현재 프로젝트의 Event 클래스 기반으로:
- Topic 명명 규칙 (예: `shrimp-tm.conversation.session.created`)
- Partition 수 (로컬 개발용 권장값)
- Replication factor (로컬 단일 브로커 기준)
- Retention 정책

### 5. 테스트 시나리오
실제 코드 기반 테스트 예시:
- EventPublisher를 통한 메시지 발행 테스트
- EventConsumer를 통한 메시지 소비 테스트
- Spring Kafka Test를 활용한 단위 테스트 예시

### 6. 트러블슈팅
자주 발생하는 문제와 해결 방법:
- Connection refused 에러
- Offset commit 실패
- Serialization/Deserialization 에러
- Topic not found 에러
- Consumer rebalancing 이슈

## 제약사항 및 가이드라인

### 필수 준수사항
1. **공식 문서만 참조**: 
   - Apache Kafka 공식 문서 (https://kafka.apache.org/documentation/)
   - Spring Kafka 공식 문서 (https://docs.spring.io/spring-kafka/reference/)
   - Docker Hub 공식 Kafka 이미지 문서

2. **오버엔지니어링 금지**:
   - 로컬 개발 환경에 불필요한 복잡한 설정 제외
   - 프로덕션 최적화 설정 제외
   - Multi-broker 클러스터 구성 제외
   - Schema Registry, Kafka Connect 등 추가 컴포넌트 제외 (명시 요청 없는 한)

3. **불필요한 작업 금지**:
   - 기존 코드 리팩토링 제안 금지
   - 아키텍처 변경 제안 금지
   - 추가 라이브러리 도입 제안 금지 (필수 항목 제외)

4. **주석 작성 원칙**:
   - 설정값의 의미와 목적만 간결하게 설명
   - "이 설정은...", "여기서는..." 등 장황한 설명 금지
   - 자명한 설정에 주석 불필요

### 코드/설정 작성 원칙
- 실제 동작하는 설정만 제공
- 하드코딩된 값 대신 환경변수 활용 가능한 경우 표시
- 버전은 최신 안정화 버전 사용 (latest 태그 금지)

## 가이드 문서 구조

```markdown
# Kafka Docker Compose 로컬 개발 환경 구축 가이드

## 1. 개요
- 목적 및 범위
- 사전 요구사항 (Docker, Docker Compose 버전)

## 2. Docker Compose 설정
### 2.1 docker-compose.yml
[전체 파일 내용]

### 2.2 설정 설명
[주요 설정 항목 설명]

### 2.3 실행 및 종료
[명령어]

## 3. Spring Boot 연동
### 3.1 application-local.yml 설정
[Kafka 설정 추가]

### 3.2 설정 설명
[주요 설정 항목 설명]

## 4. Topic 설계
### 4.1 Topic 생성
[명령어 및 스크립트]

### 4.2 Topic 목록
[프로젝트에서 사용할 Topic 목록 및 설정]

## 5. 개발/테스트 워크플로우
### 5.1 환경 시작
[단계별 명령어]

### 5.2 동작 확인
[헬스체크 및 검증 방법]

### 5.3 메시지 테스트
[Producer/Consumer 테스트 방법]

## 6. 모니터링 (선택)
### 6.1 Kafka UI 사용법
[UI 접근 방법 및 주요 기능]

## 7. 트러블슈팅
### 7.1 일반적인 문제
[문제 및 해결 방법]

### 7.2 로그 확인
[로그 위치 및 확인 방법]

## 8. 참고자료
[공식 문서 링크]
```

## 실행 지침

1. **정보 수집**:
   - Apache Kafka 공식 문서에서 Docker 실행 방법 확인
   - Spring Kafka 공식 문서에서 설정 속성 확인
   - Docker Hub에서 공식 Kafka 이미지 확인

2. **문서 작성**:
   - 위 구조를 따라 각 섹션 작성
   - 모든 명령어와 설정은 실제 동작 검증된 내용만 포함
   - 로컬 개발 환경에 최적화된 최소 설정 제공

3. **검증**:
   - 제공한 설정으로 실제 환경 구축 가능한지 확인
   - 누락된 필수 설정이 없는지 확인
   - 불필요한 복잡성이 추가되지 않았는지 확인

## 출력 형식
- Markdown 형식 (`.md`)
- 코드 블록에 적절한 syntax highlighting 적용
- 명령어는 실행 가능한 형태로 제공
- 설정 파일은 전체 내용 제공 (일부 생략 금지)

## 품질 기준
- [ ] 공식 문서 기반 정보만 사용
- [ ] 로컬 개발 환경에 적합한 최소 구성
- [ ] 모든 명령어 실행 가능
- [ ] 주석은 간결하고 필요한 것만
- [ ] 단계별 검증 방법 포함
- [ ] 트러블슈팅 섹션 포함

이 프롬프트를 기반으로 실무에서 즉시 활용 가능한 Kafka Docker Compose 로컬 개발 환경 구축 가이드를 작성하세요.
