# API Gateway 설계서 작성 프롬프트

## 역할 정의

당신은 Spring Cloud Gateway 기반의 API Gateway 서버 설계 전문가입니다. 
`api/gateway/REF.md`에 명시된 Saturn Gateway Server 아키텍처를 참고하여, 
`api/gateway` 모듈의 설계서를 작성하세요.

## 설계 목표

`api/gateway` 모듈은 다음 인프라 아키텍처에서 중앙 게이트웨이 역할을 수행합니다:

```
Client → ALB (AWS Application Load Balancer) → Gateway Server (@api/gateway) → API Servers
                                                                                ├─ @api/archive
                                                                                ├─ @api/news
                                                                                ├─ @api/contest
                                                                                ├─ @api/chatbot
                                                                                └─ @api/auth
```

## 필수 설계 요구사항

### 1. 인프라 아키텍처
- **요청 흐름**: Client → ALB → Gateway Server → API Servers
- **ALB 타임아웃**: 600초 (ALB 설정, Gateway에서 고려 필요)
- **Gateway 서버**: Spring Cloud Gateway (Netty 기반, Java 21, Spring Boot 4.0.1)
- **API 서버들**: Spring Boot 기반 마이크로서비스들

### 2. URI 기반 라우팅
요청 URI 경로를 기준으로 적절한 API 서버로 라우팅해야 합니다:

- `/api/v1/archive/**` → `@api/archive` 서버
- `/api/v1/news/**` → `@api/news` 서버
- `/api/v1/contest/**` → `@api/contest` 서버
- `/api/v1/chatbot/**` → `@api/chatbot` 서버
- `/api/v1/auth/**` → `@api/auth` 서버 (또는 Gateway 내부 처리)

**라우팅 규칙 설계 시 고려사항**:
- Path 기반 라우팅 우선 적용
- URL Rewrite 필요 시 고려 (예: `/api/v1/archive/**` → `/api/v1/archive/**` 그대로 전달)
- 환경별 백엔드 서비스 URL 설정 (Local, Dev, Beta, Prod)
- 서비스 디스커버리 필요 여부 검토 (현재는 정적 라우팅 우선)

### 3. JWT 토큰 기반 인증 및 보안
`@api/auth` 모듈의 인증 기능을 Gateway에서 수행할 수 있어야 합니다.

**검토 사항**:
1. **옵션 A**: `@api/auth` 모듈을 Gateway 서버로 통합
   - Gateway 서버에 `@api/auth`의 인증 로직을 포함
   - `/api/v1/auth/**` 요청은 Gateway 내부에서 처리
   - 장점: 단일 진입점, 인증 로직 중앙화
   - 단점: Gateway 서버 복잡도 증가

2. **옵션 B**: `@api/auth` 모듈을 별도 서버로 유지, Gateway에서 JWT 검증만 수행
   - `/api/v1/auth/**` 요청은 `@api/auth` 서버로 라우팅
   - 다른 API 요청은 Gateway에서 JWT 검증 후 라우팅
   - 장점: 관심사 분리, 서버 독립성
   - 단점: 인증 서버 추가 호출 필요

**권장 방안**: 
- **옵션 B 우선 검토** (현재 `@api/auth` 모듈이 독립적으로 존재하므로)
- Gateway에서 JWT 검증 필터 구현
- `common-security` 모듈의 JWT 검증 로직 활용
- 인증이 필요한 경로와 불필요한 경로 구분

**인증 필터 설계**:
- JWT 토큰 추출 (`Authorization: Bearer {token}`)
- JWT 토큰 검증 (서명, 만료 시간 확인)
- 사용자 정보 추출 및 헤더 주입 (`x-user-id`, `x-user-email` 등)
- 인증 불필요 경로: `/api/v1/auth/**`, `/api/v1/contest/**` (공개 API), `/api/v1/news/**` (공개 API)
- 인증 필요 경로: `/api/v1/archive/**`, `/api/v1/chatbot/**`

### 4. 연결 풀 설정 (Connection reset by peer 방지)
Connection reset by peer 에러가 발생하지 않도록 적절한 연결 풀 설정이 반영되어야 합니다.

**필수 설정 항목**:
- HTTP 클라이언트 연결 풀 최적화
- `max-idle-time`: 백엔드 서비스 keep-alive 시간보다 짧게 설정 (권장: 30초)
- `max-life-time`: 연결 최대 생명주기 (권장: 300초)
- `max-connections`: 최대 연결 수 (권장: 500)
- `connection-timeout`: 연결 타임아웃 (권장: 30초)
- `socket-timeout`: 소켓 타임아웃 (권장: 60초, 백엔드 타임아웃보다 길게)

**참고**: `api/gateway/REF.md`의 "HTTP 클라이언트 연결 풀 설정" 섹션 참고

### 5. CORS 설정
- 환경별 CORS 정책 설정 (Local, Dev, Beta, Prod)
- `allowCredentials: true` 사용 시 `allowedOriginPatterns` 사용 (와일드카드 패턴 지원)
- Global CORS 설정 적용
- 외부 API 연동 시 중복 헤더 제거 고려 (`DedupeResponseHeader` 필터)

### 6. 기타 필수 기능
- **에러 처리**: 공통 예외 처리 핸들러 (`ApiGatewayExceptionHandler`)
- **로깅**: 요청/응답 로깅 (환경별 로그 레벨)
- **헬스체크**: Gateway 서버 상태 확인 엔드포인트
- **모니터링**: 요청 추적, 성능 모니터링 고려

## 참고 자료

### 필수 참고 문서
1. **`api/gateway/REF.md`**: Saturn Gateway Server 아키텍처 참고
   - Spring Cloud Gateway 기반 설계 패턴
   - 라우팅 규칙 예시
   - 인증 필터 구현 예시
   - 연결 풀 설정 예시
   - CORS 설정 예시

2. **`docs/step2/`**: API 엔드포인트 설계
   - API 서버들의 엔드포인트 구조
   - 인증 요구사항

3. **`docs/step6/`**: OAuth 및 인증 설계
   - JWT 토큰 기반 인증 구조
   - Spring Security 통합 방법

4. **`docs/step9/contest-news-api-design.md`**: Contest/News API 설계
   - 공개 API 엔드포인트 구조

5. **`docs/step12/rag-chatbot-design.md`**: Chatbot API 설계
   - Chatbot API 엔드포인트 구조

6. **`docs/step13/user-archive-feature-design.md`**: Archive API 설계
   - Archive API 엔드포인트 구조

### 기술 스택 참고
- **Java**: 21
- **Spring Boot**: 4.0.1
- **Spring Cloud Gateway**: Spring Cloud 2023.x 이상
- **공식 문서**: Spring Cloud Gateway 공식 문서만 참고
  - https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/
  - Reactor Netty 공식 문서

## 설계서 작성 구조

다음 구조로 설계서를 작성하세요:

### 1. 개요
- Gateway 서버의 역할과 목적
- 인프라 아키텍처 다이어그램
- 요청 처리 시퀀스 다이어그램

### 2. 아키텍처 설계
- 전체 시스템 아키텍처
- Gateway 서버 내부 구조
- 모듈 의존성 관계

### 3. 라우팅 설계
- URI 기반 라우팅 규칙
- 서비스별 라우팅 매핑 테이블
- URL Rewrite 규칙 (필요 시)
- 환경별 백엔드 서비스 URL 설정

### 4. 인증 및 보안 설계
- JWT 토큰 검증 필터 설계
- 인증 필요/불필요 경로 구분
- 사용자 정보 헤더 주입 규칙
- `@api/auth` 모듈 통합 방안 (옵션 A/B 비교 및 권장 방안)

### 5. 연결 풀 및 성능 최적화
- HTTP 클라이언트 연결 풀 설정
- Connection reset by peer 방지 전략
- 타임아웃 설정 (연결, 소켓, 읽기)
- 성능 튜닝 가이드

### 6. CORS 설정
- 환경별 CORS 정책
- Global CORS 설정
- 외부 API 연동 시 중복 헤더 처리

### 7. 에러 처리 및 모니터링
- 공통 예외 처리 전략
- 에러 응답 형식
- 로깅 전략
- 모니터링 포인트

### 8. 설정 파일 설계
- `application.yml` 기본 설정
- 환경별 설정 파일 (`application-local.yml`, `application-dev.yml`, `application-beta.yml`, `application-prod.yml`)
- Gateway 라우팅 설정
- 연결 풀 설정
- CORS 설정

### 9. 구현 가이드
- 필수 컴포넌트 목록
- 필터 구현 가이드
- 설정 적용 방법
- 테스트 방법

## 제약사항 및 주의사항

### 엄격히 준수할 필요 없는 부분
- `api/gateway/REF.md`의 세부 구현은 참고용이며, 현재 프로젝트 구조에 맞게 최적화 가능
- Saturn Gateway의 특정 비즈니스 로직 (쇼핑몰/백오피스 구분 등)은 불필요

### 오버엔지니어링 방지
- **하지 말아야 할 것**:
  - 불필요한 복잡한 라우팅 로직
  - 요구사항에 없는 기능 추가 (예: Rate Limiting, Circuit Breaker 등은 명시적 요구 시에만)
  - 과도한 모니터링/로깅 시스템
  - 서비스 디스커버리 (Eureka, Consul 등) - 명시적 요구 없음

- **해야 할 것**:
  - 단순하고 명확한 URI 기반 라우팅
  - 필수 인증 필터만 구현
  - 연결 풀 최적화에 집중
  - 현재 프로젝트 구조에 맞는 최소한의 설계

### 공식 문서만 참고
- Spring Cloud Gateway 공식 문서
- Reactor Netty 공식 문서
- Spring Boot 공식 문서
- 신뢰할 수 없는 블로그나 커뮤니티 자료는 참고하지 않음

## 설계서 작성 지시

1. **현재 프로젝트 구조 분석**
   - `api/gateway` 모듈의 현재 상태 확인
   - `api/auth`, `api/archive`, `api/news`, `api/contest`, `api/chatbot` 모듈의 엔드포인트 구조 확인
   - `common-security` 모듈의 JWT 검증 로직 확인

2. **아키텍처 설계**
   - `api/gateway/REF.md`를 참고하여 현재 프로젝트에 맞는 아키텍처 설계
   - 요청 처리 플로우 다이어그램 작성
   - 인증 필터 플로우 다이어그램 작성

3. **라우팅 규칙 설계**
   - 각 API 서버별 라우팅 규칙 정의
   - 환경별 백엔드 서비스 URL 설정 방법 제시

4. **인증 통합 방안 제시**
   - 옵션 A와 옵션 B 비교 분석
   - 권장 방안 제시 및 근거
   - 선택한 방안의 상세 설계

5. **연결 풀 설정 설계**
   - Connection reset by peer 방지를 위한 구체적 설정 값 제시
   - 설정 근거 설명

6. **설정 파일 예시 작성**
   - YAML 설정 파일 예시 포함
   - 환경별 차이점 명시

7. **구현 우선순위 제시**
   - 필수 구현 항목
   - 선택적 구현 항목

## 검증 기준

설계서가 다음 기준을 만족해야 합니다:

1. ✅ 인프라 아키텍처가 명확히 정의되어 있음
2. ✅ URI 기반 라우팅 규칙이 모든 API 서버를 포함함
3. ✅ JWT 토큰 기반 인증 방안이 제시되고 근거가 명확함
4. ✅ Connection reset by peer 방지 전략이 구체적으로 제시됨
5. ✅ CORS 설정이 환경별로 정의됨
6. ✅ 오버엔지니어링 없이 최소한의 필수 기능만 포함됨
7. ✅ 공식 문서 기반의 기술적 근거가 제시됨
8. ✅ 현재 프로젝트 구조와 호환됨

## 최종 산출물

`docs/stepX/gateway-design.md` 파일로 설계서를 작성하세요.
(X는 적절한 step 번호로 결정)

설계서는 마크다운 형식으로 작성하며, 다음을 포함해야 합니다:
- 명확한 섹션 구분
- 다이어그램 (Mermaid 형식 권장)
- 설정 파일 예시
- 구현 가이드
- 참고 자료 링크

---

**중요**: 이 프롬프트는 설계서 작성을 위한 지시사항입니다. 
실제 구현은 별도의 작업으로 진행되며, 설계서가 완성된 후 구현 단계에서 상세 검토가 이루어집니다.
