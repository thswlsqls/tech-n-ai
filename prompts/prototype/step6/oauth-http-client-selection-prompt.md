# OAuth Provider 구현을 위한 최적 HTTP Client 선택 분석 프롬프트

## 연구 목표

OAuth Provider별 로그인 기능 구현을 위해 **Spring WebClient**와 **RestTemplate**을 비교 분석하여, 현재 프로젝트의 아키텍처 특성과 설계 원칙에 가장 적합한 HTTP Client를 선택하고 그 근거를 제시합니다.

## 프로젝트 컨텍스트 분석

### 1단계: 프로젝트 아키텍처 특성 파악

다음 정보를 수집하여 프로젝트의 아키텍처 특성을 파악하세요:

#### 기술 스택 및 의존성 분석
- **프레임워크**: Spring Boot 버전 확인
- **Java 버전**: Java 21+
- **현재 HTTP Client 사용 현황**: 
  - `api/auth` 모듈의 `build.gradle` 확인
  - `common/security` 모듈의 `build.gradle` 확인
  - `common/kafka` 모듈의 `build.gradle` 확인
  - 프로젝트 전역에서 WebFlux 또는 Reactive 프로그래밍 사용 여부 확인

#### 아키텍처 패턴 분석
- **프로그래밍 모델**: 
  - WebMVC (Imperative) 기반인지 확인
  - WebFlux (Reactive) 기반인지 확인
  - 혼합 사용인지 확인
- **인증 시스템**: 
  - `docs/spring-security-auth-design-guide.md` 분석
  - Stateless JWT 인증 방식 확인
  - 필터 체인 구조 확인
- **이벤트 처리**:
  - `common/kafka` 모듈 분석
  - Kafka 이벤트 발행 방식 확인 (동기/비동기)
  - 이벤트 처리 패턴 확인

#### 설계 문서 분석
다음 설계 문서들을 분석하여 프로젝트의 설계 원칙과 특성을 파악하세요:

1. **`docs/spring-security-auth-design-guide.md`**
   - 인증 시스템 아키텍처
   - 필터 체인 구조
   - 동기/비동기 처리 패턴

2. **`docs/oauth-provider-implementation-guide.md`**
   - 현재 OAuth 구현 설계
   - HTTP Client 선택 가이드
   - 통합 전략

3. **`docs/oauth-state-storage-research-result.md`**
   - State 파라미터 저장 방식
   - Redis 사용 패턴

4. **`docs/phase2/1. api-endpoint-design.md`**
   - API 엔드포인트 설계 원칙
   - 요청/응답 처리 패턴

5. **`docs/phase2/2. data-model-design.md`**
   - 데이터 모델 설계
   - 트랜잭션 처리 패턴

6. **`shrimp-rules.md`**
   - 프로젝트 개발 가이드라인
   - 아키텍처 제약사항
   - 금지 사항

#### 모듈별 코드 분석
다음 모듈의 실제 코드를 분석하여 현재 구현 패턴을 파악하세요:

1. **`api/auth` 모듈**
   - `AuthService.java`: 비즈니스 로직 처리 방식
   - `AuthController.java`: 컨트롤러 구조
   - 트랜잭션 처리 방식
   - 동기/비동기 메서드 패턴

2. **`common/security` 모듈**
   - `JwtAuthenticationFilter.java`: 필터 처리 방식
   - `SecurityConfig.java`: 보안 설정
   - 동기/비동기 처리 패턴

3. **`common/kafka` 모듈**
   - 이벤트 발행 방식
   - 동기/비동기 처리 패턴

### 2단계: OAuth 구현 요구사항 분석

#### OAuth API 호출 특성
- **호출 빈도**: 사용자 로그인 시점에만 호출 (낮은 빈도)
- **응답 시간 요구사항**: 사용자 경험을 위해 빠른 응답 필요
- **동시성**: OAuth 로그인은 동시 다발적으로 발생할 수 있음
- **에러 처리**: 네트워크 오류 및 타임아웃 처리 필요

#### 통합 요구사항
- **기존 시스템 통합**:
  - JWT 토큰 발급 시스템과의 통합
  - UserEntity와의 통합
  - Kafka 이벤트 발행 시스템과의 통합
- **트랜잭션 처리**:
  - OAuth 콜백 처리 시 트랜잭션 경계
  - 데이터베이스 저장 및 이벤트 발행의 원자성

## 비교 분석 범위

### 비교 대상
1. **Spring WebClient**
   - 비동기/논블로킹 처리 방식
   - Reactive Streams 기반
   - WebFlux 의존성 필요

2. **RestTemplate**
   - 동기/블로킹 처리 방식
   - 스레드 풀 기반
   - WebMVC와 자연스러운 통합

### 비교 항목

#### 1. 아키텍처 호환성
- **프로젝트 아키텍처와의 일치성**:
  - WebMVC 기반 프로젝트에서 WebClient 사용 시 혼합 아키텍처 이슈
  - RestTemplate 사용 시 일관된 아키텍처 유지
- **의존성 추가 영향**:
  - WebClient 사용 시 WebFlux 의존성 추가 필요
  - 프로젝트 전역에 미치는 영향
  - 모듈 간 의존성 복잡도 증가

#### 2. 통합 용이성
- **기존 코드와의 통합**:
  - `AuthService`의 동기 메서드와의 통합
  - 트랜잭션 처리와의 통합
  - Kafka 이벤트 발행과의 통합
- **에러 처리 일관성**:
  - 기존 예외 처리 패턴과의 일치성
  - Spring Security 필터 체인과의 호환성

#### 3. 성능 및 확장성
- **동시 요청 처리**:
  - OAuth 로그인 동시성 요구사항
  - 리소스 사용 효율성 (스레드, 메모리)
- **실질적 성능 차이**:
  - OAuth API 호출 같은 단순 HTTP 요청에서 성능 차이의 의미
  - 프로젝트 요구사항에서 성능 차이의 중요성

#### 4. 사용 편의성
- **API 사용법**:
  - 코드 복잡도
  - 학습 곡선
- **에러 처리 및 타임아웃 설정**:
  - 설정 복잡도
  - 디버깅 용이성

#### 5. Spring Boot 6.x 권장 사항
- **공식 권장 사항**:
  - Spring Boot 6.x에서 권장하는 HTTP Client
  - RestTemplate의 향후 지원 계획
- **의존성 요구사항**:
  - WebClient 사용 시 필요한 의존성
  - RestTemplate 사용 시 필요한 의존성

## 참고 자료 (공식 문서만 사용)

### 필수 참고 문서
1. **Spring Framework 공식 문서**
   - Spring WebClient 공식 문서: https://docs.spring.io/spring-framework/reference/web/webflux/webclient.html
   - Spring RestTemplate 공식 문서: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-resttemplate
   - Spring Boot Reference Documentation: HTTP Clients 섹션
   - WebMVC와 WebFlux 혼합 사용 가이드

2. **Spring Boot 공식 문서**
   - Spring Boot 6.x Release Notes
   - Spring Boot Auto-configuration: WebClient, RestTemplate
   - WebMVC와 WebFlux 공존 시 주의사항

3. **Reactive Streams (WebClient 관련)**
   - Reactive Streams Specification: https://www.reactive-streams.org/
   - Project Reactor 공식 문서: https://projectreactor.io/docs/core/release/reference/
   - WebMVC에서 WebClient 사용 시 블로킹 처리 방법

### 참고 금지
- 블로그 포스트, 개인 의견, 비공식 튜토리얼
- 공식 문서에 근거하지 않은 성능 벤치마크
- 오래된 버전의 문서 (Spring Boot 5.x 이하)

## 출력 형식

다음 형식으로 결과를 제시하세요:

### 1. 프로젝트 아키텍처 특성 분석

#### 기술 스택 분석
- **Spring Boot 버전**: [버전]
- **프로그래밍 모델**: [WebMVC/WebFlux/혼합]
- **현재 HTTP Client 사용**: [사용 여부 및 종류]
- **Reactive 프로그래밍 사용**: [사용 여부]

#### 아키텍처 패턴
- **인증 시스템**: [설명]
- **이벤트 처리**: [설명]
- **트랜잭션 처리**: [설명]

#### 설계 원칙
- **주요 설계 원칙**: [설계 문서에서 추출]
- **아키텍처 제약사항**: [shrimp-rules.md에서 추출]
- **금지 사항**: [관련 금지 사항]

### 2. HTTP Client 비교 분석

#### 아키텍처 호환성 비교

| 항목 | WebClient | RestTemplate | 프로젝트 특성과의 일치성 |
|------|----------|-------------|----------------------|
| 프로그래밍 모델 | Reactive (논블로킹) | Imperative (블로킹) | [프로젝트 모델과 비교] |
| 의존성 추가 | WebFlux 필요 | WebMVC 기본 포함 | [의존성 영향 분석] |
| 아키텍처 일관성 | 혼합 아키텍처 | 일관된 아키텍처 | [일관성 평가] |

**아키텍처 호환성 평가**:
- 프로젝트가 WebMVC 기반인 경우, WebClient 사용 시 혼합 아키텍처가 되는가?
- 혼합 아키텍처가 프로젝트 설계 원칙과 일치하는가?
- 의존성 추가가 프로젝트 전역에 미치는 영향은 무엇인가?

#### 통합 용이성 비교

| 항목 | WebClient | RestTemplate | 통합 난이도 |
|------|----------|-------------|------------|
| AuthService 통합 | [설명] | [설명] | [비교] |
| 트랜잭션 처리 | [설명] | [설명] | [비교] |
| Kafka 이벤트 발행 | [설명] | [설명] | [비교] |
| 에러 처리 일관성 | [설명] | [설명] | [비교] |

**통합 용이성 평가**:
- 기존 동기 메서드와의 통합 난이도
- 트랜잭션 경계와의 호환성
- 기존 예외 처리 패턴과의 일치성

#### 성능 및 확장성 비교

| 항목 | WebClient | RestTemplate | OAuth 요구사항 대응 |
|------|----------|-------------|-------------------|
| 동시 요청 처리 | [설명] | [설명] | [요구사항 대응 평가] |
| 리소스 사용 (스레드) | [설명] | [설명] | [비교] |
| 리소스 사용 (메모리) | [설명] | [설명] | [비교] |
| 응답 시간 (단일 요청) | [설명] | [설명] | [비교] |

**성능 차이의 실질적 의미**:
- OAuth API 호출 같은 단순 HTTP 요청에서 성능 차이가 의미 있는가?
- 현재 프로젝트의 OAuth 로그인 동시성 요구사항에서 성능 차이가 중요한가?
- 리소스 사용 효율성 차이가 프로젝트에 미치는 영향은 무엇인가?

#### 사용 편의성 비교

| 항목 | WebClient | RestTemplate | 평가 |
|------|----------|-------------|------|
| API 사용법 | [설명] | [설명] | [비교] |
| 코드 복잡도 | [설명] | [설명] | [비교] |
| 에러 처리 | [설명] | [설명] | [비교] |
| 타임아웃 설정 | [설명] | [설명] | [비교] |
| 디버깅 용이성 | [설명] | [설명] | [비교] |

### 3. Spring Boot 6.x 권장 사항

**공식 권장 사항**:
- Spring Boot 6.x에서 권장하는 HTTP Client는 무엇인가?
- RestTemplate의 향후 지원 계획은 무엇인가?
- WebMVC 프로젝트에서 WebClient 사용 시 권장 사항은 무엇인가?

**의존성 요구사항**:
- WebClient 사용 시 필요한 의존성: [의존성 목록]
- RestTemplate 사용 시 필요한 의존성: [의존성 목록]
- 의존성 추가가 프로젝트 전역에 미치는 영향: [영향 분석]

### 4. 프로젝트 특성 기반 최종 권장 선택

**선택된 HTTP Client**: [WebClient 또는 RestTemplate]

**선택 근거 (프로젝트 특성 기반)**:

1. **아키텍처 일관성**:
   - [프로젝트 아키텍처와의 일치성 설명]
   - [설계 원칙 준수 여부]

2. **통합 용이성**:
   - [기존 시스템과의 통합 난이도]
   - [코드 일관성 유지]

3. **의존성 영향**:
   - [의존성 추가 영향 분석]
   - [프로젝트 전역 복잡도 증가 여부]

4. **실용성**:
   - [OAuth 구현 요구사항 충족 여부]
   - [성능 차이의 실질적 의미]

5. **유지보수성**:
   - [코드 복잡도]
   - [학습 곡선]
   - [향후 확장성]

**공식 문서 근거**:
- [Spring Framework 문서 참조]
- [Spring Boot 문서 참조]
- [WebMVC와 WebFlux 혼합 사용 가이드 참조]

**프로젝트 설계 문서 근거**:
- [관련 설계 문서 참조]
- [아키텍처 제약사항 준수 여부]

### 5. 구현 가이드 (간결하게)

**필수 구현 사항**:
1. [구현 사항 1]
2. [구현 사항 2]
3. [구현 사항 3]

**의존성 추가** (필요한 경우):
```gradle
// 필요한 의존성
```

**기본 설정 예시** (간결하게):
```java
// 핵심 설정만 포함
```

**OAuth API 호출 예시** (간결하게):
```java
// Access Token 교환 예시
// 사용자 정보 조회 예시
```

**주의사항**:
- [프로젝트 아키텍처와의 통합 시 주의할 점]
- [피해야 할 함정]

### 6. 대안 방법 (선택적)

만약 권장 방법 외에 대안이 있다면, 언제 사용해야 하는지 명시하세요.

**대안 방법**: [방법명]

**사용 시나리오**:
- [언제 이 방법을 선택해야 하는가?]
- [이 방법의 장점이 필요한 경우]

## 검증 기준

제시된 분석은 다음 기준을 만족해야 합니다:

1. ✅ **프로젝트 특성 기반 분석**: 실제 프로젝트 아키텍처와 설계 원칙 반영
2. ✅ **공식 문서 기반**: Spring Framework 공식 문서만 참고
3. ✅ **실용성**: OAuth 구현 요구사항에 적합한 비교
4. ✅ **아키텍처 일관성**: 프로젝트 설계 원칙 준수
5. ✅ **간결성**: 오버엔지니어링 없이 핵심만 제시
6. ✅ **현재성**: Spring Boot 6.x 기준 최신 정보

## 주의사항

- **프로젝트 특성 우선**: 이론적 완벽함보다 프로젝트 아키텍처와의 일치성 우선
- **공식 문서만 참고**: 추측이나 개인 의견 배제
- **설계 원칙 준수**: shrimp-rules.md의 아키텍처 제약사항 준수
- **실용성 우선**: OAuth 구현 요구사항에 적합한 실용적 선택 제시
- **간결성**: 불필요한 복잡성 도입 지양
- **오버엔지니어링 방지**: 요구사항을 충족하는 가장 단순하고 일관된 방법 우선
- **의존성 최소화**: 불필요한 의존성 추가 지양
- **아키텍처 일관성**: 프로젝트 전역의 아키텍처 일관성 유지

---

**작성일**: 2025-01-27  
**프롬프트 버전**: 1.0  
**대상**: OAuth Provider 구현을 위한 최적 HTTP Client 선택 분석  
**목적**: `docs/oauth-provider-implementation-guide.md` 문서 보완
