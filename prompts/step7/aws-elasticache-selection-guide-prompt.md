# AWS ElastiCache 선택 가이드 문서 작성 프롬프트

## 목표

현재 프로젝트의 Redis 사용 패턴을 분석하고, AWS ElastiCache의 세 가지 옵션(Valkey, Memcached, Redis OSS)을 AWS 공식 문서 기반으로 비교 분석하여, 프로젝트에 가장 적합한 선택지를 추천하는 가이드 문서를 작성하세요.

## 제약사항

1. **문서명**: `aws-elasticache-intergration-guide.md` (정확히 이 파일명 사용)
2. **참고 자료 범위**: AWS 공식 문서만 참고 (블로그, 서드파티 자료 제외)
3. **금지사항**: 
   - 오버엔지니어링 금지
   - 현재 프로젝트에서 사용하지 않는 기능 추가 제안 금지
   - 불필요한 마이그레이션 계획 작성 금지
   - 구현 코드 작성 금지 (분석만 수행)

## 작업 단계

### 1단계: 현재 프로젝트의 캐시 사용 패턴 파악

다음 파일들을 분석하여 현재 프로젝트의 Redis 사용 목적과 패턴을 정확히 파악하세요:

**분석 대상**:
- `common/core/src/main/java/com/tech/n/ai/common/core/config/RedisConfig.java`
- `common/core/src/main/resources/application-common-core.yml`
- `docs/step6/oauth-state-storage-research-result.md`
- `docs/step7/redis-optimization-best-practices.md`

**파악해야 할 정보**:
- [ ] 현재 Redis 사용 사례 (OAuth State, Kafka 멱등성, Rate Limiting, 챗봇 캐싱)
- [ ] 각 사용 사례의 Key 형식과 TTL 설정
- [ ] 데이터 타입 (String, Hash, List, Set 등)
- [ ] 데이터 지속성 요구사항
- [ ] 성능 요구사항 (읽기/쓰기 패턴, 응답 시간)
- [ ] 고가용성 요구사항

### 2단계: AWS ElastiCache 옵션 비교 분석

AWS 공식 문서를 참고하여 다음 세 가지 옵션을 비교 분석하세요:

#### 분석 대상
1. **Amazon ElastiCache for Valkey**
2. **Amazon ElastiCache for Memcached**
3. **Amazon ElastiCache for Redis OSS**

#### 비교 기준

**기능적 측면**:
- 지원하는 데이터 타입
- 데이터 지속성(Persistence) 지원 여부
- 복제(Replication) 및 고가용성
- TTL 지원
- 클러스터 모드
- 트랜잭션 지원
- Pub/Sub 지원

**성능적 측면**:
- 읽기/쓰기 처리량
- 레이턴시
- 메모리 효율성
- 네트워크 최적화

**비용적 측면**:
- 노드 타입별 시간당 비용
- 최소 구성 비용 (월간 예상)
- 백업 및 스냅샷 비용
- 데이터 전송 비용

**운영적 측면**:
- 관리 편의성
- 모니터링 및 로깅
- 백업 및 복구
- 버전 업그레이드

### 3단계: 프로젝트 요구사항 매칭

1단계에서 파악한 프로젝트 요구사항과 2단계의 비교 분석을 매칭하여 각 옵션의 적합성을 평가하세요.

**평가 기준**:
- ✅ 필수 요구사항 충족 여부
- ⚠️ 부분 충족 또는 제한사항
- ❌ 요구사항 미충족

### 4단계: 최종 추천 및 근거

**다음 질문에 답변하세요**:
1. 프로젝트에 가장 적합한 ElastiCache 옵션은 무엇인가?
2. 그 이유는 무엇인가? (핵심 근거 3-5개)
3. 해당 옵션의 제한사항이나 주의사항은 무엇인가?
4. 예상 월간 비용은 얼마인가? (개발/테스트 환경 기준)

## 문서 구조

다음 구조를 따라 문서를 작성하세요:

```markdown
# AWS ElastiCache 선택 가이드

**작성 일시**: {현재 날짜}
**대상**: 프로덕션 환경 구축
**버전**: 1.0

## 목차

1. [개요](#개요)
2. [현재 프로젝트의 캐시 사용 패턴](#현재-프로젝트의-캐시-사용-패턴)
3. [AWS ElastiCache 옵션 비교](#aws-elasticache-옵션-비교)
4. [프로젝트 요구사항 매칭](#프로젝트-요구사항-매칭)
5. [최종 추천](#최종-추천)
6. [참고 자료](#참고-자료)

## 개요

{프로젝트 컨텍스트와 문서 목적}

## 현재 프로젝트의 캐시 사용 패턴

### 사용 사례 요약

| 사용 사례 | Key 형식 | TTL | 데이터 타입 | 읽기/쓰기 패턴 |
|---------|---------|-----|------------|--------------|
| ... | ... | ... | ... | ... |

### 요구사항 정리

- **기능적 요구사항**: ...
- **성능적 요구사항**: ...
- **고가용성 요구사항**: ...
- **비용 제약사항**: ...

## AWS ElastiCache 옵션 비교

### 1. Amazon ElastiCache for Valkey

{기능, 성능, 비용, 운영 측면 분석}

### 2. Amazon ElastiCache for Memcached

{기능, 성능, 비용, 운영 측면 분석}

### 3. Amazon ElastiCache for Redis OSS

{기능, 성능, 비용, 운영 측면 분석}

### 비교 요약 테이블

| 비교 항목 | Valkey | Memcached | Redis OSS |
|----------|--------|-----------|-----------|
| ... | ... | ... | ... |

## 프로젝트 요구사항 매칭

{각 옵션이 프로젝트 요구사항을 얼마나 충족하는지 평가}

## 최종 추천

### 권장 옵션: {선택된 옵션}

**핵심 근거**:
1. {근거 1}
2. {근거 2}
3. {근거 3}

**제한사항 및 주의사항**:
- {제한사항 1}
- {제한사항 2}

**예상 비용**: 
- 개발/테스트 환경: ${금액}/월
- 프로덕션 환경: ${금액}/월

## 참고 자료

### AWS 공식 문서
- [링크 1]
- [링크 2]
- ...

### 프로젝트 내부 문서
- `docs/step6/oauth-state-storage-research-result.md`
- `docs/step7/redis-optimization-best-practices.md`
- `docs/step17/aws-elasticache-redis-integration-guide.md`
```

## 작성 원칙

### DO (해야 할 것)
- ✅ AWS 공식 문서의 정확한 정보만 사용
- ✅ 현재 프로젝트의 실제 사용 패턴에 집중
- ✅ 비용 정보는 ap-northeast-2(서울) 리전 기준으로 작성
- ✅ 객관적 사실과 데이터 기반으로 추천
- ✅ 각 옵션의 장단점을 균형있게 서술
- ✅ 명확한 근거와 함께 추천안 제시

### DON'T (하지 말아야 할 것)
- ❌ 현재 프로젝트에서 사용하지 않는 기능 추가 제안
- ❌ 구현 코드 작성 (설정 파일 예시도 불필요)
- ❌ 마이그레이션 가이드 작성 (선택 가이드만 작성)
- ❌ 추측성 정보나 개인 의견
- ❌ 과도한 최적화 제안
- ❌ 블로그나 서드파티 자료 참고

## 검증 체크리스트

작성 완료 후 다음을 확인하세요:

- [ ] 파일명이 `aws-elasticache-intergration-guide.md`인가?
- [ ] 현재 프로젝트의 Redis 사용 패턴을 정확히 파악했는가?
- [ ] 세 가지 옵션(Valkey, Memcached, Redis OSS)을 모두 비교했는가?
- [ ] AWS 공식 문서만 참고했는가?
- [ ] 비용 정보는 ap-northeast-2 리전 기준인가?
- [ ] 추천 근거가 명확하고 객관적인가?
- [ ] 불필요한 구현 상세나 코드가 포함되지 않았는가?
- [ ] 오버엔지니어링 없이 프로젝트 요구사항에만 집중했는가?

## 참고 정보

**AWS 공식 문서 링크**:
- ElastiCache 개요: https://docs.aws.amazon.com/AmazonElastiCache/
- ElastiCache for Valkey: https://docs.aws.amazon.com/AmazonElastiCache/latest/val-ug/
- ElastiCache for Memcached: https://docs.aws.amazon.com/AmazonElastiCache/latest/mem-ug/
- ElastiCache for Redis OSS: https://docs.aws.amazon.com/AmazonElastiCache/latest/red-ug/
- AWS Pricing Calculator: https://calculator.aws/

**프로젝트 컨텍스트**:
- Spring Boot 4.0.1 기반 멀티모듈 프로젝트
- 현재 Redis 7.x 사용 중
- Stateless 아키텍처 (세션 미사용)
- ap-northeast-2(서울) 리전 배포 예정

---

**프롬프트 버전**: 1.0  
**작성일**: 2026-01-20
