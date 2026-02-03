# 로컬 Redis 설치 및 연동 가이드 추가 프롬프트

## 지시사항

`docs/step17/aws-elasticache-redis-integration-guide.md` 문서에 **로컬 개발 환경에서 Redis 설치 및 연동하는 방법**에 대한 상세 가이드를 추가하세요.

---

## 요구사항

### 1. 작업 범위

다음 내용을 포함하는 새로운 섹션을 추가하세요:

#### 추가할 섹션 제목
**"로컬 개발 환경 Redis 설치 및 설정"**

이 섹션은 목차에서 "프로젝트 연동" 섹션 **이전**에 위치해야 합니다.

#### 포함해야 할 내용

1. **로컬 Redis 설치 방법**
   - macOS (Homebrew 사용)
   - Windows (공식 설치 방법)
   - Linux (Ubuntu/Debian, CentOS/RHEL)
   - Docker를 통한 설치

2. **Redis 서버 시작 및 중지**
   - 각 OS별 명령어
   - 백그라운드 실행 방법
   - 자동 시작 설정

3. **기본 설정 확인 및 변경**
   - redis.conf 파일 위치
   - 주요 설정 항목 (포트, 비밀번호, 메모리 제한 등)
   - 개발 환경에 적합한 설정

4. **프로젝트와 로컬 Redis 연동**
   - 환경변수 설정 (.env 파일)
   - application-common-core.yml 설정 확인
   - RedisConfig.java 동작 확인

5. **연결 테스트**
   - redis-cli를 통한 연결 확인
   - Spring Boot 애플리케이션에서 연결 확인
   - 기본적인 명령어 테스트 (PING, SET, GET)

6. **로컬 Redis 사용 시 주의사항**
   - 보안 설정 (비밀번호 설정 권장)
   - 메모리 관리
   - 데이터 영속성 설정

---

### 2. 참고해야 할 프로젝트 문서 및 코드

문서 작성 시 다음 프로젝트 내부 자료를 **반드시 참고**하여 일관성을 유지하세요:

1. **설계서**
   - `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스
   - `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 패턴

2. **소스 코드**
   - `common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java`: Redis 설정 클래스
   - `common/core/src/main/resources/application-common-core.yml`: Redis 연결 설정

3. **프로젝트의 Redis 사용 패턴** (현재 문서에서 확인)
   - OAuth State 저장 (TTL: 10분)
   - Kafka 이벤트 멱등성 (TTL: 7일)
   - Rate Limiting (TTL: 1분)
   - 챗봇 캐싱 (가변 TTL)

---

### 3. 외부 자료 참고 지침

**공식 문서만 참고하세요**. 다음 공식 자료를 사용하세요:

#### Redis 공식 문서
- [Redis 다운로드 및 설치](https://redis.io/docs/install/install-redis/)
- [Redis 시작하기](https://redis.io/docs/getting-started/)
- [Redis 설정 (redis.conf)](https://redis.io/docs/management/config/)
- [Redis CLI 사용법](https://redis.io/docs/ui/cli/)

#### OS별 패키지 관리자 공식 문서
- [Homebrew 공식 사이트](https://brew.sh/) (macOS)
- [Redis Docker Hub 공식 이미지](https://hub.docker.com/_/redis)

#### Spring Data Redis 공식 문서
- [Spring Data Redis 레퍼런스](https://docs.spring.io/spring-data/redis/reference/)
- [Spring Boot Redis 자동 설정](https://docs.spring.io/spring-boot/reference/data/nosql.html#data.nosql.redis)

**주의**: 블로그, 개인 사이트, 비공식 튜토리얼은 참고하지 마세요.

---

### 4. 작성 가이드라인

#### 문서 스타일 및 형식

1. **기존 문서 스타일 유지**
   - 현재 `aws-elasticache-redis-integration-guide.md`의 마크다운 형식, 톤, 구조를 따르세요
   - 코드 블록, 표, 인용구 등의 포맷을 일관되게 사용하세요

2. **명확하고 구체적인 명령어 제공**
   - 각 단계별로 실행 가능한 명령어를 제공하세요
   - 명령어 실행 결과 예시를 포함하세요

3. **각 OS별로 명확히 구분**
   - macOS, Windows, Linux, Docker를 명확히 분리하세요
   - 사용자가 자신의 환경에 맞는 부분만 빠르게 찾을 수 있도록 구성하세요

4. **공식 문서 링크 포함**
   - 참고한 공식 문서의 URL을 명시하세요
   - 각 단계마다 "참고" 형태로 링크를 추가하세요

#### 기술적 정확성

1. **프로젝트 설정과 일치**
   - `application-common-core.yml`에 정의된 설정값과 일치하는 환경변수를 제시하세요
   - `RedisConfig.java`에서 사용하는 RedisTemplate 설정을 고려하세요

2. **최신 버전 기준**
   - Redis 7.x 버전 기준으로 작성하세요 (AWS ElastiCache와 동일 버전)
   - 최신 안정 버전의 명령어와 옵션을 사용하세요

3. **실제 동작 가능한 코드**
   - 모든 명령어와 설정은 실제로 동작 검증된 것이어야 합니다
   - 추측이나 가정으로 작성하지 마세요

---

### 5. 오버엔지니어링 방지 규칙

다음 내용은 **절대 추가하지 마세요**:

❌ **불필요한 고급 설정**
- Redis Cluster 구성
- Redis Sentinel 설정
- 복잡한 복제 설정
- 프로덕션 수준의 튜닝

❌ **요청하지 않은 추가 기능**
- Redis GUI 도구 소개
- 성능 벤치마킹
- 대규모 데이터 마이그레이션
- 모니터링 도구 설치

❌ **과도한 설명**
- Redis 내부 동작 원리 상세 설명
- 데이터 구조 심층 분석
- 알고리즘 설명

❌ **추가 스크립트 작성**
- 자동화 스크립트
- 헬퍼 도구
- 커스텀 유틸리티

✅ **작성해야 할 내용**
- 기본 설치 및 시작 방법
- 프로젝트 연동에 필요한 최소한의 설정
- 연결 테스트 방법
- 개발 환경에 필요한 기본 설정만

---

## 작업 순서

다음 순서로 작업을 진행하세요:

1. **프로젝트 코드 확인**
   ```
   - docs/step7/redis-optimization-best-practices.md 읽기
   - docs/step6/oauth-state-storage-research-result.md 읽기
   - common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java 읽기
   - common/core/src/main/resources/application-common-core.yml 읽기
   ```

2. **현재 문서 구조 파악**
   ```
   - aws-elasticache-redis-integration-guide.md의 목차 확인
   - 문서 스타일 및 형식 파악
   ```

3. **공식 문서 참고**
   ```
   - Redis 공식 설치 가이드 확인
   - Spring Data Redis 공식 문서 확인
   ```

4. **새 섹션 작성**
   ```
   - 목차 업데이트 (새 섹션 추가)
   - "로컬 개발 환경 Redis 설치 및 설정" 섹션 작성
   - 각 OS별 설치 방법 작성
   - 프로젝트 연동 방법 작성
   - 연결 테스트 방법 작성
   ```

5. **검증**
   ```
   - 프로젝트 설정과의 일관성 확인
   - 공식 문서 링크 검증
   - 명령어 정확성 확인
   ```

---

## 예상 결과물

작업 완료 후 다음과 같은 구조가 되어야 합니다:

```markdown
## 목차

1. [개요](#개요)
2. [배포 옵션 선택](#배포-옵션-선택)
3. [클러스터 생성 및 구성](#클러스터-생성-및-구성)
4. [네트워크 및 보안 구성](#네트워크-및-보안-구성)
5. [로컬 개발 환경 Redis 설치 및 설정](#로컬-개발-환경-redis-설치-및-설정)  ← 새로 추가
   - macOS에서 Redis 설치
   - Windows에서 Redis 설치
   - Linux에서 Redis 설치
   - Docker로 Redis 실행
   - 기본 설정 및 시작
   - 프로젝트 연동 설정
   - 연결 테스트
6. [프로젝트 연동](#프로젝트-연동)
7. [모니터링 및 알림 설정](#모니터링-및-알림-설정)
...
```

---

## 최종 확인 체크리스트

작업 완료 후 다음을 확인하세요:

- [ ] 프로젝트의 Redis 설정 파일과 일치하는 환경변수 제시
- [ ] 모든 명령어가 실제 실행 가능한 것인지 확인
- [ ] 공식 문서 링크만 포함되어 있는지 확인
- [ ] 기존 문서 스타일과 일관성 유지
- [ ] 요청하지 않은 고급 기능이나 도구 추가하지 않았는지 확인
- [ ] 각 OS별 설치 방법이 명확히 구분되어 있는지 확인
- [ ] 문서 버전 및 최종 업데이트 날짜 수정
- [ ] 목차가 올바르게 업데이트되었는지 확인

---

## 작업 시작

위의 모든 지침을 따라 `docs/step17/aws-elasticache-redis-integration-guide.md` 문서에 로컬 Redis 설치 및 연동 가이드를 추가하세요.

**중요**: 요구사항에 명시된 내용만 작성하고, 추가적인 개선이나 기능 확장은 하지 마세요.
