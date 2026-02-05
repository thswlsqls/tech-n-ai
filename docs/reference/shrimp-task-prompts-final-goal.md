```
-- plan task: 
개발자 대회 정보와 최신 IT 테크 뉴스를 제공하는 API Server 구축 프로젝트 (확장 버전)

## 프로젝트 개요

기존 프로젝트에 다음 기능들을 추가하여 확장합니다:
1. 정보 출처 자동 업데이트 시스템
2. 사용자 인증 및 관리 시스템
3. 사용자 북마크 기능
4. CQRS 패턴 기반 아키텍처
5. 데이터베이스 설계서 생성
6. 변경 이력 추적 시스템 (히스토리 테이블)
7. MSA 멀티모듈 아키텍처 구축
8. Spring Batch 기반 배치 처리 시스템
9. Spring Rest Docs 기반 API 문서화

## 기존 프로젝트 목표 (유지)

1. 개발자 대회 정보(해커톤, 알고리즘 대회, 오픈소스 대회 등)를 수집하고 제공하는 API 엔드포인트 구축
2. 최신 IT 테크 뉴스 정보를 수집하고 제공하는 API 엔드포인트 구축
3. 신뢰할 만한 정보 출처를 탐색하고 평가하여 최적의 데이터 소스를 선정
4. Spring Boot 기반 RESTful API Server 개발
5. 데이터 수집, 저장, 조회 기능 구현

## 추가 요구사항

### 1. 정보 출처 자동 업데이트 시스템

**목표**: `json/sources.json` 파일을 주기적으로 업데이트하여 최신 정보 출처를 유지

**입력 요구사항**:
- 기존 `json/sources.json` 파일 (있는 경우)
- `prompts/source-discovery-prompt.md` 프롬프트 파일
- Anthropic API 키 (환경 변수)
- AI 모델 선택 (claude-3-opus-20240229 권장)

**출력 결과**:
- 업데이트된 `json/sources.json` 파일
- 변경 이력 (버전 관리)
- Slack 알림 (변경 사항이 있는 경우)
- 배치 작업 실행 리포트

**성공 기준**:
- `json/sources.json` 파일이 정상적으로 생성/업데이트되어야 함
- JSON 스키마 검증을 통과해야 함
- 변경 사항이 있을 경우 Slack 알림이 발송되어야 함
- 배치 작업이 성공적으로 완료되어야 함

**에러 처리 시나리오**:
- LLM API 호출 실패: 재시도 로직 실행 (최대 3회), 실패 시 Slack 알림
- JSON 파싱 실패: 에러 로깅 및 Slack 알림, 기존 파일 유지
- 파일 쓰기 실패: 에러 로깅 및 Slack 알림, 롤백
- 배치 작업 실패: JobExecution 상태 추적, 재실행 가능하도록 설계

**구현 방법**:
- Spring Batch를 통한 배치 작업 실행 (월 1회 권장)
- `prompts/source-discovery-prompt.md` 프롬프트를 AI LLM에 전달하여 자동화된 출처 탐색
- AI LLM을 통한 구조화된 JSON 응답 수신 및 `json/sources.json` 생성
- 새로운 출처 발견 시 자동 평가 및 우선순위 결정
- 기존 출처의 상태 변경 감지 (API 중단, Rate Limit 변경 등)

**AI 통합 프레임워크 선택** (참고: `docs/step11/ai-integration-analysis.md`):
- **spring-ai** (권장): Spring 생태계 완벽 통합, 빠른 프로토타이핑, 최소 설정, Spring Boot Auto-Configuration 지원
- **langchain4j** (대안): 구조화된 출력 강력, 고급 AI 워크플로우 필요 시, 수동 설정 필요
- **구현 우선순위**: Phase 1에서 spring-ai로 프로토타입 구현, 구조화된 출력이 복잡하거나 고급 기능 필요 시 langchain4j 검토

**기술 요구사항**:
- Spring Batch를 통한 배치 작업 실행
- AI LLM 통합 (spring-ai 또는 langchain4j)
- `prompts/source-discovery-prompt.md` 프롬프트 파일 로딩 및 전달
- 구조화된 JSON 응답 파싱 및 검증
- json/sources.json 파일 버전 관리 및 변경 이력 추적
- 출처 상태 모니터링 및 Slack 알림 시스템 (client-slack 모듈 활용)
- Jenkins Server를 통한 스케줄링 및 모니터링

**작업 세부사항**:

1. **AI LLM 통합 구현** (참고: `docs/step11/ai-integration-analysis.md`)
   
   **Option A: spring-ai 프레임워크 구현** (참고: `docs/step11/ai-integration-analysis.md`)
   - 의존성 추가: `spring-ai-anthropic-spring-boot-starter`
   - application.yml 설정:
     ```yaml
     spring:
       ai:
         anthropic:
           api-key: ${ANTHROPIC_API_KEY}
           chat:
             options:
               model: claude-3-opus-20240229
               temperature: 0.7
               max-tokens: 4000
     ```
   - SourceDiscoveryTasklet 구현:
     * `ChatClient` 빈 주입 (Spring Boot Auto-Configuration)
     * `prompts/source-discovery-prompt.md` 파일 로딩 (`ResourceLoader` 활용)
     * 프롬프트를 Anthropic Claude LLM에 전달
     * JSON 응답 수신 및 파싱 (Jackson ObjectMapper)
     * `json/sources.json` 파일 생성 및 검증
   
   **Option B: langchain4j 프레임워크 구현** (참고: `docs/step11/ai-integration-analysis.md`)
   - 의존성 추가: `langchain4j`, `langchain4j-anthropic`
   - LangChain4jConfig 설정 클래스 구현:
     * `ChatLanguageModel` 빈 생성
     * `AnthropicChatModel.builder()` 사용
     * 모델명: `claude-3-opus-20240229`
     * API 키: `ANTHROPIC_API_KEY` 환경 변수
   - SourceDiscoveryTasklet 구현:
     * `ChatLanguageModel` 빈 주입
     * `prompts/source-discovery-prompt.md` 파일 로딩 (`ResourceLoader` 활용)
     * 프롬프트를 Anthropic Claude LLM에 전달
     * JSON 응답 수신 및 파싱 (Jackson ObjectMapper)
     * `json/sources.json` 파일 생성 및 검증
   
   **프레임워크 선택 전략**:
   - 환경 변수 또는 설정 파일로 프레임워크 선택 가능하도록 구현
   - 기본값: spring-ai (Spring 생태계 통합 우수)
   - 구조화된 출력이 복잡하거나 고급 기능 필요 시 langchain4j 사용
   - 두 프레임워크 모두 Anthropic Claude 모델 사용 (claude-3-opus-20240229 권장)

2. **Spring Batch Job 설계**
   - Job: SourceUpdateJob
   - Step 1: SourceDiscoveryStep
     * AI LLM을 통한 출처 탐색 (spring-ai 또는 langchain4j 활용)
     * `prompts/source-discovery-prompt.md` 프롬프트 로딩
     * LLM API 호출 및 JSON 응답 수신
     * 응답 파싱 및 검증
     * 임시 json/sources.json 생성
   - Step 2: SourceValidationStep
     * 기존 출처의 API 엔드포인트 유효성 검증
     * Rate Limit 변경 감지
     * 인증 방식 변경 감지
   - Step 3: SourceComparisonStep
     * 기존 json/sources.json과 새 버전 비교
     * 변경 사항 추출 및 분석
   - Step 4: SourceUpdateStep
     * json/sources.json 업데이트 (변경 사항이 있을 경우)
     * 버전 관리 및 변경 이력 저장
   - Step 5: NotificationStep
     * 변경 사항 Slack 알림 발송 (client-slack 모듈 활용, 참고: `docs/step8/slack-integration-design-guide.md`)
     * 배치 작업 성공/실패 알림
     * 에러 발생 시 즉시 알림
     * **Slack 알림 시나리오**: Job 실행 완료/실패, Step별 실행 결과, LLM API 호출 실패, JSON 파싱 실패, 파일 쓰기 실패 등

3. **배치 인프라 구성**
   - JobRepository: Amazon Aurora MySQL 사용 (Spring Batch 메타데이터)
     * **역할**: Spring Batch Job 실행 이력 및 메타데이터 저장
     * **책임**: JobExecution, StepExecution 상태 추적 및 재실행 지원
     * **검증 기준**: Job 실행 이력이 정상적으로 저장되고 조회 가능해야 함
   - JobLauncher: 배치 작업 실행
     * **역할**: 배치 Job 실행 담당
     * **책임**: JobParameters 전달 및 JobExecution 생성
   - JobParameters: 실행 파라미터 (실행일시, 버전, AI 프레임워크 선택 등)
     * **제약사항**: 동일한 JobParameters로는 JobInstance 중복 생성 불가
   - JobExecutionListener: 실행 전후 로깅 및 알림
     * **역할**: Job 실행 전후 이벤트 처리
     * **책임**: 로깅, 알림, 초기화/정리 작업

4. **AI LLM 통합 세부 구현** (참고: `docs/step11/ai-integration-analysis.md`)
   - 프롬프트 파일 로딩:
     * `ResourceLoader`를 통한 `prompts/source-discovery-prompt.md` 로딩
     * 파일 경로는 환경 변수 또는 설정 파일로 관리
   - LLM API 호출 (Anthropic Claude):
     * Anthropic API 키 설정 (`ANTHROPIC_API_KEY` 환경 변수)
     * 모델 선택: `claude-3-opus-20240229` (권장) 또는 `claude-3-sonnet-20240229`
     * 타임아웃 설정 (기본 60초)
     * 재시도 로직 (최대 3회)
     * 에러 핸들링 및 로깅
   - 응답 처리:
     * JSON 응답 파싱 (Jackson ObjectMapper)
     * `json/sources.json` 스키마 검증
     * 데이터 정제 및 변환
   - 파일 생성:
     * 검증된 데이터를 `json/sources.json` 파일로 저장
     * 기존 파일 백업 (버전별)
     * 파일 경로는 환경 변수 또는 설정 파일로 관리

5. **SourceValidationService 구현**
   - 기존 출처의 API 엔드포인트 유효성 검증
   - Rate Limit 변경 감지
   - 인증 방식 변경 감지
   - 검증 결과를 Slack 알림으로 전송

6. **SourceVersionControl 구현**
   - json/sources.json 버전 관리
   - 변경 이력 저장 (Git 또는 데이터베이스)
   - 롤백 기능
   - 버전별 비교 및 차이점 분석

7. **Jenkins Pipeline 구성**
   - Jenkinsfile 작성
   - 스케줄링 설정 (Cron: 매월 1일 자정)
   - 배치 JAR 빌드 및 실행
   - 실행 결과 모니터링 및 알림
   - 실패 시 재시도 로직

8. **에러 핸들링 및 모니터링**
   - LLM API 호출 실패 시 재시도
   - 타임아웃 처리
   - 비용 관리 (토큰 사용량 모니터링)
   - Step 실패 시 Skip 또는 Retry
   - Job 실패 시 알림 및 롤백
   - Dead Letter Queue 처리

### 2. 사용자 인증 및 관리 시스템

**목표**: 안전한 사용자 인증 및 관리 기능 제공

**입력 요구사항**:
- 사용자 회원가입 정보 (이메일, 비밀번호, 사용자명)
- OAuth 2.0 클라이언트 정보 (Google, GitHub, Kakao, Naver)
- JWT Secret Key (환경 변수)
- Redis 연결 정보 (Refresh Token 저장)

**출력 결과**:
- 사용자 계정 생성
- JWT Access Token 및 Refresh Token 발급
- 사용자 인증 상태 관리
- 이메일 인증 링크 발송

**성공 기준**:
- 회원가입이 정상적으로 완료되어야 함
- 로그인 시 JWT 토큰이 정상적으로 발급되어야 함
- 토큰 검증이 정상적으로 동작해야 함
- 비밀번호가 BCrypt로 암호화되어 저장되어야 함
- 이메일 인증이 정상적으로 동작해야 함

**에러 처리 시나리오**:
- 중복 이메일: 400 Bad Request, 에러 메시지 반환
- 잘못된 자격증명: 401 Unauthorized, 계정 잠금 카운트 증가
- 토큰 만료: 401 Unauthorized, Refresh Token으로 갱신 유도
- 계정 잠금: 403 Forbidden, 잠금 해제 시간 안내
- OAuth 인증 실패: 401 Unauthorized, 에러 메시지 반환

**보안 요구사항** (최우선):
- JWT (JSON Web Token) 기반 인증
- Access Token + Refresh Token 패턴
- 토큰 만료 시간 관리 (Access: 60분, Refresh: 7일)
- 비밀번호 암호화 (BCrypt, salt rounds: 12)
- HTTPS 필수 (프로덕션 환경)
- CSRF 보호
- SQL Injection 방지 (PreparedStatement 사용)
- XSS 방지 (입력값 검증 및 이스케이프)

**인증 방식 옵션**:
- 옵션 A: 자체 회원가입/로그인
  - 이메일 기반 회원가입
  - 이메일 인증 (이메일 인증 링크)
  - 비밀번호 재설정 기능
  - 계정 잠금 기능 (5회 실패 시 30분 잠금)
- 옵션 B: SNS 로그인 (OAuth 2.0, 참고: `docs/step6/oauth-provider-implementation-guide.md`)
  - Google OAuth 2.0
  - Naver OAuth 2.0 (한국 사용자 고려)
  - Kakao OAuth 2.0 (한국 사용자 고려)
  - **State 파라미터 저장**: Redis 사용 (Key: `oauth:state:{state_value}`, TTL: 10분)
  - **OAuth Provider API 호출**: OpenFeign 클라이언트 사용 (참고: `docs/step6/oauth-feign-client-migration-analysis.md`)
- 옵션 C: 하이브리드 (자체 + SNS)
  - 권장: 자체 회원가입 + SNS 로그인 지원
  - 지원 SNS: Google, Naver, Kakao

**구현 세부사항** (참고: `docs/step6/spring-security-auth-design-guide.md`):
1. User 엔티티 설계
   - id (BIGINT UNSIGNED, TSID Primary Key)
   - email, password (BCrypt 암호화, salt rounds: 12), username
   - provider_id (BIGINT UNSIGNED, Provider 테이블 참조)
   - provider_user_id (VARCHAR(255), OAuth 제공자의 사용자 ID)
   - is_email_verified (BOOLEAN, 기본값 FALSE)
   - last_login_at (TIMESTAMP(6))
   - is_deleted, deleted_at, deleted_by (Soft Delete)
   - created_at, created_by, updated_at, updated_by (감사 필드)

2. JWT 토큰 관리 (참고: `docs/step6/spring-security-auth-design-guide.md`)
   - JwtTokenProvider 구현 (common-security 모듈)
   - Access Token: 짧은 만료 시간 (기본 60분, 설정 가능)
   - Refresh Token: 긴 만료 시간 (기본 7일, 설정 가능), Aurora MySQL 저장 (RefreshToken 엔티티)
   - RefreshToken 회전(Rotation) 전략: 기존 토큰 무효화 후 새 토큰 생성 (RFC 6749 베스트 프랙티스)
   - 토큰 갱신 API: /api/v1/auth/refresh
   - 토큰 무효화 API: /api/v1/auth/logout

3. 인증 API 엔드포인트 (api-auth 모듈)
   - POST /api/v1/auth/signup - 회원가입
   - POST /api/v1/auth/login - 로그인
   - POST /api/v1/auth/logout - 로그아웃
   - POST /api/v1/auth/refresh - 토큰 갱신
   - GET /api/v1/auth/verify-email?token={token} - 이메일 인증
   - POST /api/v1/auth/reset-password - 비밀번호 재설정 요청
   - POST /api/v1/auth/reset-password/confirm - 비밀번호 재설정 확인
   - GET /api/v1/auth/oauth2/{provider} - SNS 로그인 시작
     * State 파라미터 생성 및 Redis 저장 (Key: `oauth:state:{state_value}`, TTL: 10분)
     * 구현: `OAuthStateService.saveState(state, providerName)` (참고: `docs/step6/oauth-provider-implementation-guide.md`)
   - GET /api/v1/auth/oauth2/{provider}/callback - SNS 로그인 콜백
     * State 파라미터 검증 및 삭제 (Redis에서 조회 후 즉시 삭제, 일회성 사용)
     * 구현: `OAuthStateService.validateAndDeleteState(state, providerName)` (참고: `docs/step6/oauth-provider-implementation-guide.md`)
     * OAuth Provider API 호출: OpenFeign 클라이언트 사용 (참고: `docs/step6/oauth-feign-client-migration-analysis.md`)
   - GET /api/v1/auth/history/{entityType}/{entityId} - 변경 이력 조회 (entityType: user, admin)
   - GET /api/v1/auth/history/{entityType}/{entityId}/at?timestamp={timestamp} - 특정 시점 데이터 조회 (entityType: user, admin)
   - POST /api/v1/auth/history/{entityType}/{entityId}/restore?historyId={historyId} - 특정 버전으로 복구 (entityType: user, admin)

4. 보안 필터 및 인터셉터 (참고: `docs/step6/spring-security-auth-design-guide.md`)
   - JwtAuthenticationFilter: JWT 토큰 검증 (common-security 모듈)
   - SecurityConfig: Spring Security 설정 (STATELESS 세션 정책, CSRF 비활성화)
   - Rate Limiting: 로그인 시도 제한 (Redis 기반, 5회/분)
   - CORS 설정: 허용된 도메인만 접근 (개발 환경: 모든 origin 허용, 운영 환경: 특정 도메인 지정)
   - PasswordEncoderConfig: BCryptPasswordEncoder 설정 (salt rounds: 12)

### 3. 사용자 북마크 기능

**목표**: 사용자가 관심 있는 대회/뉴스를 개인 북마크에 저장 및 관리

**입력 요구사항**:
- 사용자 ID (JWT 토큰에서 추출)
- 북마크 항목 정보 (itemType, itemId, title, description, tags, category, memo)
- 수정 요청 데이터 (북마크 ID, 수정할 필드)

**출력 결과**:
- 생성된 북마크 ID
- 북마크 목록 (페이징, 필터링, 정렬 적용)
- 북마크 상세 정보
- Soft Delete 상태 (deleteYn, deletedAt)

**성공 기준**:
- 북마크가 정상적으로 생성되어야 함
- 북마크 목록 조회가 정상적으로 동작해야 함
- 페이징, 필터링, 정렬이 정상적으로 적용되어야 함
- Soft Delete가 정상적으로 동작해야 함
- 북마크 복원이 정상적으로 동작해야 함
- 권한 검증이 정상적으로 동작해야 함 (본인만 조회/수정/삭제 가능)

**에러 처리 시나리오**:
- 권한 없음: 403 Forbidden, 에러 메시지 반환
- 북마크 없음: 404 Not Found, 에러 메시지 반환
- 잘못된 요청 데이터: 400 Bad Request, 검증 에러 메시지 반환
- 중복 북마크: 409 Conflict, 에러 메시지 반환

**기능 요구사항**:
- 북마크 저장: 대회 또는 뉴스 기사를 북마크에 추가
- 북마크 조회: 사용자의 북마크 목록 조회 (페이징, 필터링, 정렬)
- 북마크 수정: 메모, 태그, 카테고리 등 사용자 정의 정보 수정
- 북마크 삭제: Soft Delete 방식 (delete_yn 플래그 사용)
- 북마크 복원: 삭제된 항목 복원 기능

**Soft Delete 요구사항**:
- delete_yn 플래그 사용 (Y/N 또는 boolean)
- deleted_at 타임스탬프 저장
- 삭제된 항목은 기본 조회에서 제외
- 관리자 또는 사용자 본인만 삭제된 항목 조회 가능
- 물리적 삭제는 별도 배치 작업으로 처리 (30일 후)

**구현 세부사항**:
1. Bookmark 엔티티 설계
   - id (BIGINT UNSIGNED, TSID Primary Key)
   - user_id (BIGINT UNSIGNED, auth 스키마의 users 테이블 참조, 스키마 간 Foreign Key 미지원)
   - item_type (VARCHAR(50), CONTEST, NEWS_ARTICLE)
   - item_id (VARCHAR(255), MongoDB ObjectId 문자열)
   - tag (VARCHAR(100), nullable)
   - memo (TEXT, nullable)
   - is_deleted, deleted_at, deleted_by (Soft Delete)
   - created_at, created_by, updated_at, updated_by (감사 필드)

2. Bookmark API 엔드포인트 (api-bookmark 모듈, 참고: `docs/step2/1. api-endpoint-design.md`)
   - POST /api/v1/bookmark - 북마크 추가 (Aurora MySQL 저장, Kafka 이벤트 발행)
   - GET /api/v1/bookmark - 북마크 목록 조회 (MongoDB Atlas 조회, userId 필터링)
     * 파라미터: page, size, sort, itemType
   - GET /api/v1/bookmark/{id} - 북마크 상세 조회 (MongoDB Atlas 조회, bookmarkTsid 또는 ObjectId 사용)
   - PUT /api/v1/bookmark/{id} - 북마크 수정 (Aurora MySQL 업데이트, Kafka 이벤트 발행)
   - DELETE /api/v1/bookmark/{id} - 북마크 삭제 (Soft Delete, Aurora MySQL, Kafka 이벤트 발행)
   - POST /api/v1/bookmark/{id}/restore - 북마크 복원 (Aurora MySQL 복원, Kafka 이벤트 발행)
   - GET /api/v1/bookmark/deleted - 삭제된 북마크 목록 (Aurora MySQL 조회, CQRS 패턴 예외)
   - GET /api/v1/bookmark/history/{entityId} - 변경 이력 조회 (Aurora MySQL 조회, CQRS 패턴 예외)
   - GET /api/v1/bookmark/history/{entityId}/at?timestamp={timestamp} - 특정 시점 데이터 조회 (Aurora MySQL 조회, CQRS 패턴 예외)
   - POST /api/v1/bookmark/history/{entityId}/restore?historyId={historyId} - 특정 버전으로 복구 (Aurora MySQL 복구, Kafka 이벤트 발행)

3. 권한 관리
   - 사용자는 본인의 북마크만 조회/수정/삭제 가능
   - JWT 토큰에서 userId 추출하여 권한 검증
   - @PreAuthorize 어노테이션 활용

### 4. CQRS 패턴 구현

**목표**: 읽기와 쓰기 작업을 분리하여 성능 최적화 및 확장성 향상

**입력 요구사항**:
- Command 작업 (쓰기): 사용자 생성, 북마크 생성/수정/삭제, 대회/뉴스 수집
- Query 작업 (읽기): 대회 목록 조회, 뉴스 목록 조회, 북마크 목록 조회

**출력 결과**:
- Command Side: Amazon Aurora MySQL에 데이터 저장, Kafka 이벤트 발행
- Query Side: MongoDB Atlas 클라우드 서비스에서 데이터 조회 (읽기 최적화)
- 동기화: Kafka Consumer를 통한 MongoDB Atlas 동기화

**성공 기준**:
- 모든 쓰기 작업이 Amazon Aurora MySQL에 정상적으로 저장되어야 함
- 모든 쓰기 작업 후 Kafka 이벤트가 정상적으로 발행되어야 함
- Kafka Consumer가 이벤트를 정상적으로 수신하고 MongoDB Atlas에 동기화해야 함
- 동기화 지연 시간이 허용 범위 내여야 함 (실시간: 1초 이내, 배치: 5분 이내)
- 멱등성이 보장되어야 함 (중복 이벤트 처리 시 데이터 일관성 유지)
- 읽기 작업이 MongoDB Atlas 클라우드 서비스에서 정상적으로 수행되어야 함

**에러 처리 시나리오**:
- Aurora 쓰기 실패: 트랜잭션 롤백, 에러 로깅, MongoDB Atlas에 예외 로그 저장, 사용자에게 에러 응답
- MongoDB Atlas 읽기 실패: 에러 로깅, MongoDB Atlas에 예외 로그 저장, 사용자에게 에러 응답
- Kafka 이벤트 발행 실패: 재시도 로직 실행, 실패 시 Dead Letter Queue 처리, MongoDB Atlas에 예외 로그 저장
- MongoDB Atlas 동기화 실패: 재시도 로직 실행, 실패 시 Dead Letter Queue 처리, MongoDB Atlas에 예외 로그 저장
- 동기화 지연: 모니터링 알림, 수동 동기화 트리거 제공
- 데이터 불일치: 수동 동기화 트리거 제공, 데이터 검증 로직 실행

**예외 로깅 정책** (참고: `docs/step1/2. mongodb-schema-design.md`):
- 모든 예외는 MongoDB Atlas의 `exception_logs` 컬렉션에 저장 (ExceptionLogDocument)
- 읽기 예외와 쓰기 예외를 구분하여 저장 (source 필드: "READ" 또는 "WRITE")
- 예외 로깅 실패는 메인 로직에 영향을 주지 않도록 비동기 처리
- 예외 로깅 실패 시 로컬 로그 파일에 대체 기록
- TTL 인덱스로 90일 후 자동 삭제

**아키텍처 설계**:
- **Command (쓰기)**: Amazon Aurora MySQL 호환 RDBMS 사용
  - 사용자 생성, 수정, 삭제
  - 북마크 생성, 수정, 삭제
  - 정보 출처 업데이트
  - **Aurora 특화 고려사항**:
    * 고가용성: Multi-AZ 배포 (자동 장애 조치)
    * 성능: Aurora Storage 자동 스케일링
    * 백업: 자동 백업 및 Point-in-Time Recovery
    * 읽기 복제본: 최대 15개 읽기 복제본 지원
- **Query (읽기)**: MongoDB Atlas 클라우드 서비스 사용
  - 대회 목록 조회
  - 뉴스 목록 조회
  - 북마크 목록 조회
  - 검색 쿼리
  - **연결 정보**: MongoDB Atlas Cluster 연결 문자열 사용
  - **인증**: MongoDB Atlas에서 제공하는 사용자명/비밀번호 또는 X.509 인증서 사용
  - **네트워크**: IP Whitelist 또는 VPC Peering 설정 필수
  - **SSL/TLS**: 프로덕션 환경에서 필수 활성화
- **동기화**: Apache Kafka 사용
  - 모든 쓰기 작업을 Kafka 이벤트로 발행
  - Kafka Consumer가 MongoDB Atlas에 동기화
  - 이벤트 소싱 패턴 적용

**구현 세부사항**:
1. Command Side (Amazon Aurora MySQL)
   - **auth 스키마** (api-auth 모듈):
     - Provider 엔티티 (OAuth 제공자 정보)
     - User 엔티티 (쓰기 전용)
     - Admin 엔티티 (관리자 정보)
     - RefreshToken 엔티티 (JWT Refresh Token)
     - EmailVerification 엔티티 (이메일 인증 정보)
     - UserHistory 엔티티 (사용자 변경 이력)
     - AdminHistory 엔티티 (관리자 변경 이력)
     - **스키마 설정**: `api-auth-application.yml`에서 `module.aurora.schema=auth` 설정
     - **동적 연결**: `domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 사용
   - **bookmark 스키마** (api-bookmark 모듈):
     - Bookmark 엔티티 (쓰기 전용)
     - BookmarkHistory 엔티티 (북마크 변경 이력)
     - **스키마 설정**: `api-bookmark-application.yml`에서 `module.aurora.schema=bookmark` 설정
     - **동적 연결**: `domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 사용
     - **주의사항**: `bookmarks` 테이블의 `user_id`는 `auth` 스키마의 `users` 테이블을 참조하지만, MySQL은 스키마 간 Foreign Key를 지원하지 않으므로 애플리케이션 레벨에서 참조 무결성을 보장해야 함
   - **주의사항**: 
     - `api-contest`, `api-news` 모듈은 Aurora DB를 사용하지 않으므로 해당 스키마의 엔티티는 존재하지 않음
     - Contest와 NewsArticle 데이터는 MongoDB Atlas에만 저장됨 (읽기 전용 데이터)
     - Source는 `json/sources.json` 파일 기반으로 관리되며 Aurora MySQL에 Source 엔티티가 없음
     - **환경변수 관리**: Aurora DB Cluster 접속 정보는 환경변수로 관리 (`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`)
   - **Primary Key 전략**: 모든 테이블의 Primary Key는 TSID (Time-Sorted Unique Identifier) 사용
     * **TSID 적용 방법**: 커스텀 어노테이션 `@Tsid` 사용
     * **데이터 추가 규칙**: JPA Entity를 통해서만 데이터 추가 (직접 SQL INSERT 금지)
     * **TSID 생성**: `@Tsid` 어노테이션이 적용된 Primary Key Column에 자동으로 TSID 값 생성
   - **Aurora 연결 설정** (참고: `docs/step1/3. aurora-schema-design.md`):
     * **환경변수 관리**: Aurora DB Cluster 접속 정보는 환경변수로 관리 (보안성 및 환경별 설정 분리)
       - 필수 환경변수: `AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`
       - 로컬 환경에서는 `.env` 파일 사용 (`.gitignore`에 포함되어야 함)
       - 프로덕션 환경에서는 AWS Secrets Manager, Parameter Store 등 활용
     * **API 모듈별 스키마 매핑**:
   - 각 API 모듈의 `api-*-application.yml` 파일에서 `module.aurora.schema` 속성 설정
     - `api-auth` 모듈: `module.aurora.schema=auth` (auth 스키마 사용)
     - `api-bookmark` 모듈: `module.aurora.schema=bookmark` (bookmark 스키마 사용)
     - `api-contest`, `api-news` 모듈: Aurora DB 미사용 (MongoDB Atlas 사용)
     - `domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 환경변수를 사용하여 동적으로 스키마 참조
     - DataSource URL 형식: `jdbc:mysql://${AURORA_WRITER_ENDPOINT}:3306/${module.aurora.schema}?${AURORA_OPTIONS}`
     * **스키마별 관리 테이블** (참고: `docs/step1/3. aurora-schema-design.md`):
       - `auth` 스키마 (api-auth 모듈): providers, users, admins, refresh_tokens, email_verifications, user_history, admin_history
       - `bookmark` 스키마 (api-bookmark 모듈): bookmarks, bookmark_history
       - **주의사항**: `bookmarks` 테이블의 `user_id`는 `auth` 스키마의 `users` 테이블을 참조하지만, MySQL은 스키마 간 Foreign Key를 지원하지 않으므로 애플리케이션 레벨에서 참조 무결성을 보장해야 함
     * 연결 풀 최적화: HikariCP 설정 (최소/최대 연결 수 조정)
     * 타임아웃 설정: connectionTimeout, idleTimeout, maxLifetime
     * SSL 연결: 프로덕션 환경 필수

2. Query Side (MongoDB Atlas)
   - SourcesDocument (정보 출처 정보, `json/sources.json` 기반)
   - ContestDocument (읽기 최적화, `client-scraper` 모듈에서 수집)
   - NewsArticleDocument (읽기 최적화, `client-rss` 모듈에서 수집)
   - BookmarkDocument (읽기 최적화, 사용자별 인덱스, `bookmarkTsid` 필드로 Aurora MySQL 동기화)
   - UserProfileDocument (읽기 최적화, 통계 정보 포함, `userTsid` 필드로 Aurora MySQL 동기화)
   - ExceptionLogDocument (예외 로그, 읽기/쓰기 예외 모두 기록, TTL 인덱스로 90일 후 자동 삭제)
   - **MongoDB Atlas 연결 설정**:
     * Connection String: `mongodb+srv://{username}:{password}@{cluster-endpoint}/{database}?retryWrites=true&w=majority`
     * 또는 Standard Connection String: `mongodb://{username}:{password}@{cluster-endpoint}:27017/{database}?ssl=true&replicaSet=...`
     * 연결 풀 최적화: Spring Data MongoDB 자동 연결 풀 관리
     * 타임아웃 설정: connectionTimeout, socketTimeout
     * SSL/TLS 연결: 프로덕션 환경 필수
     * Read Preference: secondaryPreferred (읽기 복제본 우선)

3. Kafka 이벤트 설계 (참고: `docs/step2/2. data-model-design.md`)
   - UserCreatedEvent (User 엔티티 생성 시 → UserProfileDocument 생성)
   - UserUpdatedEvent (User 엔티티 수정 시 → UserProfileDocument 업데이트)
   - UserDeletedEvent (User 엔티티 Soft Delete 시 → UserProfileDocument 물리적 삭제)
   - UserRestoredEvent (User 엔티티 복원 시 → UserProfileDocument 새로 생성)
   - BookmarkCreatedEvent (Bookmark 엔티티 생성 시 → BookmarkDocument 생성)
   - BookmarkUpdatedEvent (Bookmark 엔티티 수정 시 → BookmarkDocument 업데이트)
   - BookmarkDeletedEvent (Bookmark 엔티티 Soft Delete 시 → BookmarkDocument 물리적 삭제)
   - BookmarkRestoredEvent (Bookmark 엔티티 복원 시 → BookmarkDocument 새로 생성)
   - **동기화 매핑**: TSID 필드 기반 1:1 매핑 (`User.id(TSID)` → `UserProfileDocument.userTsid`, `Bookmark.id(TSID)` → `BookmarkDocument.bookmarkTsid`)
   - **동기화 지연 시간**: 실시간 동기화 목표 (1초 이내)

4. Kafka Producer 구현
   - 모든 Command 작업 후 이벤트 발행
   - 트랜잭션 관리 (DB 커밋 후 이벤트 발행)
   - 이벤트 순서 보장 (Partition Key 사용)

5. Kafka Consumer 구현
   - 이벤트 수신 및 MongoDB Atlas 동기화
   - 멱등성 보장 (이벤트 ID 기반 중복 처리 방지)
   - 에러 핸들링 및 재시도 로직
   - Dead Letter Queue (DLQ) 처리

6. 동기화 전략 (참고: `docs/step2/2. data-model-design.md`)
   - 실시간 동기화: 중요 이벤트 (사용자 생성, 북마크 변경), 목표 지연 시간 1초 이내
   - 배치 동기화: 대량 데이터 (대회/뉴스 수집)
   - 동기화 상태 모니터링 및 알림
   - **TSID 필드 기반 매핑**: Command Side의 TSID Primary Key를 Query Side의 UNIQUE 필드로 매핑하여 1:1 관계 보장
   - **MongoDB Soft Delete 미지원**: Soft Delete 시 Document 물리적 삭제, 복원 시 Document 새로 생성

### TSID Primary Key 전략 구현

**목표**: Amazon Aurora MySQL의 모든 테이블에 TSID (Time-Sorted Unique Identifier)를 Primary Key로 적용

**구현 범위**:
1. TSID 커스텀 어노테이션 생성
2. TSID 생성기 구현
3. JPA Entity에 TSID 적용
4. 데이터 추가 규칙 준수

**TSID 커스텀 어노테이션 구현**:

```java
// domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/aurora/annotation/Tsid.java
package com.ebson.shrimp.tm.demo.domain.aurora.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TSID (Time-Sorted Unique Identifier) Primary Key를 위한 커스텀 어노테이션
 * 
 * 이 어노테이션을 Primary Key Column에 적용하면 자동으로 TSID 값이 생성됩니다.
 * TSID는 시간 기반 정렬이 가능한 고유 식별자로, 분산 환경에서도 충돌 없이 사용할 수 있습니다.
 * 
 * 사용 예시:
 * ```java
 * @Entity
 * @Table(name = "users")
 * public class User {
 *     @Id
 *     @Tsid
 *     private Long id;
 *     // ...
 * }
 * ```
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tsid {
}
```

**TSID 생성기 구현**:

```java
// domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/aurora/generator/TsidGenerator.java
package com.ebson.shrimp.tm.demo.domain.aurora.generator;

import com.ebson.shrimp.tm.demo.domain.aurora.annotation.Tsid;
import io.hypersistence.tsid.TSID;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import java.io.Serializable;

/**
 * TSID 생성기
 * 
 * @Tsid 어노테이션이 적용된 필드에 대해 TSID 값을 자동 생성합니다.
 */
public class TsidGenerator implements IdentifierGenerator {
    
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return TSID.Factory.getTsid().toLong();
    }
}
```

**JPA Entity에 TSID 적용 예시**:

```java
// domain/aurora/src/main/java/com/ebson/shrimp/tm/demo/domain/aurora/entity/User.java
package com.ebson.shrimp.tm.demo.domain.aurora.entity;

import com.ebson.shrimp.tm.demo.domain.aurora.annotation.Tsid;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    
    @Id
    @Tsid
    @GeneratedValue(generator = "tsid-generator")
    @org.hibernate.annotations.GenericGenerator(
        name = "tsid-generator",
        type = com.ebson.shrimp.tm.demo.domain.aurora.generator.TsidGenerator.class
    )
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    // ... 기타 필드
}
```

**의존성 추가** (build.gradle):
```gradle
dependencies {
    // TSID 라이브러리
    implementation 'io.hypersistence:hypersistence-tsid:5.0.0'
    // 또는 최신 버전 사용
}
```

**데이터 추가 규칙**:
- ✅ **허용**: JPA Entity를 통한 데이터 추가 (`entityRepository.save(entity)`)
- ❌ **금지**: 직접 SQL INSERT 문 사용 (`INSERT INTO ...`)
- ❌ **금지**: MyBatis를 통한 INSERT 쿼리 작성
- ✅ **허용**: JPA의 `@PrePersist` 또는 `@PreUpdate`를 통한 추가 로직

**검증 기준**:
- 모든 Entity의 Primary Key에 `@Tsid` 어노테이션이 적용되어야 함
- TSID 생성기가 정상적으로 동작해야 함
- JPA Entity를 통해서만 데이터가 추가되어야 함
- 직접 SQL INSERT가 사용되지 않아야 함
- **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### 5. 데이터베이스 설계서 생성

**목표**: MongoDB Atlas 도큐먼트 설계서와 Amazon Aurora MySQL 테이블 설계서를 MD 파일로 생성

**생성할 파일**:
1. `docs/step1/2. mongodb-schema-design.md` - MongoDB Atlas 도큐먼트 설계서 (✅ 완료)
2. `docs/step1/3. aurora-schema-design.md` - Amazon Aurora MySQL 테이블 설계서 (✅ 완료)

**설계서 포함 내용** (✅ 완료):
- 컬렉션/테이블 목록 및 설명
- 각 컬렉션/테이블의 스키마 정의
- 필드 타입, 제약조건, 인덱스
- 관계 정의 (Foreign Key, Reference)
- 샘플 데이터 예제
- 성능 최적화 전략
- 마이그레이션 전략
- **MongoDB 설계서**: SourcesDocument, ContestDocument, NewsArticleDocument, BookmarkDocument, UserProfileDocument, ExceptionLogDocument 포함
- **Aurora 설계서**: API 모듈별 스키마 매핑, 환경변수 관리, TSID Primary Key 전략 포함

**작업 세부사항** (참고: `docs/step1/2. mongodb-schema-design.md`, `docs/step1/3. aurora-schema-design.md`):
1. MongoDB Atlas 도큐먼트 설계서 작성 (✅ 완료)
   - SourcesDocument 스키마 (`json/sources.json` 기반)
   - ContestDocument 스키마 (`client-scraper` 모듈에서 수집)
   - NewsArticleDocument 스키마 (`client-rss` 모듈에서 수집)
   - BookmarkDocument 스키마 (`bookmarkTsid` 필드로 Aurora MySQL 동기화)
   - UserProfileDocument 스키마 (`userTsid` 필드로 Aurora MySQL 동기화)
   - ExceptionLogDocument 스키마 (예외 로그, TTL 인덱스)
   - 인덱스 전략 (ESR 규칙 준수)
   - 샤딩 전략 (필요 시)

2. Amazon Aurora MySQL 테이블 설계서 작성 (✅ 완료)
   - **auth 스키마** (api-auth 모듈):
     - Provider 테이블 (OAuth 제공자 정보)
     - User 테이블 (TSID Primary Key)
     - Admin 테이블 (TSID Primary Key)
     - RefreshToken 테이블 (TSID Primary Key)
     - EmailVerification 테이블 (TSID Primary Key)
     - UserHistory 테이블 (TSID Primary Key)
     - AdminHistory 테이블 (TSID Primary Key)
   - **bookmark 스키마** (api-bookmark 모듈):
     - Bookmark 테이블 (TSID Primary Key)
     - BookmarkHistory 테이블 (TSID Primary Key)
   - **주의사항**:
     - `api-contest`, `api-news` 모듈은 Aurora DB를 사용하지 않으므로 해당 스키마의 테이블은 존재하지 않음
     - Contest와 NewsArticle 데이터는 MongoDB Atlas에만 저장됨
     - Source는 `json/sources.json` 파일 기반으로 관리되며 Aurora MySQL에 Source 테이블이 없음
   - Foreign Key 관계 (스키마 간 Foreign Key 미지원)
   - 인덱스 전략 (쓰기 최적화를 위한 최소 인덱스)
   - **스키마 매핑 설정**: 각 API 모듈의 `api-*-application.yml`에서 `module.aurora.schema` 설정
   - **환경변수 관리**: Aurora DB Cluster 접속 정보는 환경변수로 관리 (`AURORA_WRITER_ENDPOINT`, `AURORA_READER_ENDPOINT`, `AURORA_USERNAME`, `AURORA_PASSWORD`, `AURORA_OPTIONS`)
   - **Aurora 특화 고려사항**:
     * 파티셔닝 전략 (대용량 테이블)
     * 읽기 복제본 활용 전략
     * 백업 및 복구 전략
     * 성능 모니터링 지표
     * 스키마 간 Foreign Key 제약조건 미지원 (애플리케이션 레벨에서 참조 무결성 보장 필요)

3. 설계서 자동 생성 스크립트 (선택사항)
   - JPA 엔티티 기반 자동 생성
   - MongoDB Atlas 스키마 기반 자동 생성
   - 정기적 업데이트 (스키마 변경 시)

### 6. 변경 이력 추적 시스템 (히스토리 테이블)

**목표**: 모든 쓰기 API에 대한 변경 이력을 Amazon Aurora MySQL에 추적하여 감사(Audit) 및 복구 기능 제공

**요구사항**:
- 모든 쓰기 작업(CREATE, UPDATE, DELETE)에 대한 변경 이력 자동 저장
- 변경 전/후 데이터 비교 가능
- 변경자, 변경 시간, 변경 사유 추적
- 특정 시점 데이터 복구 가능
- 성능 영향 최소화

**구현 방법**:
- JPA Entity Listener 또는 Hibernate Envers 활용
- 각 엔티티별 히스토리 테이블 생성
- 트리거 기반 또는 애플리케이션 레벨 구현

**작업 세부사항**:
1. 히스토리 테이블 설계
   - **auth 스키마** (api-auth 모듈):
     - UserHistory 테이블: User 엔티티 변경 이력 (history_id: TSID Primary Key)
     - AdminHistory 테이블: Admin 엔티티 변경 이력 (history_id: TSID Primary Key)
   - **bookmark 스키마** (api-bookmark 모듈):
     - BookmarkHistory 테이블: Bookmark 엔티티 변경 이력 (history_id: TSID Primary Key)
   - **주의**: ContestHistory, NewsArticleHistory, SourceHistory는 불필요함
     - Contest와 NewsArticle은 읽기 전용 데이터 (Query Side: MongoDB Atlas)
     - Command Side(Aurora MySQL)에는 Contest와 NewsArticle 엔티티가 존재하지 않음
     - Source는 json/sources.json 파일 기반으로 관리되며 Aurora MySQL에 Source 엔티티가 없음
   - 공통 필드: history_id (TSID Primary Key), entity_id, operation_type (INSERT, UPDATE, DELETE)
   - 변경 전 데이터 (before_data: JSON), 변경 후 데이터 (after_data: JSON)
   - changed_by (userId), changed_at, change_reason
   - **주의**: `operation_type='DELETE'`는 실제 SQL DELETE가 아닌 Soft Delete를 의미함

2. 변경 이력 자동 저장 구현
   - JPA Entity Listener (@PrePersist, @PreUpdate, @PreRemove)
   - 또는 Hibernate Envers 라이브러리 활용
   - AOP를 통한 변경 이력 자동 기록
   - 트랜잭션 내에서 원본 데이터와 함께 저장
   - **주의**: Soft Delete 시에도 히스토리 저장 (operation_type: DELETE)

3. 변경 이력 조회 API (참고: `docs/step2/1. api-endpoint-design.md`)
   - **api-auth 모듈**:
     - GET /api/v1/auth/history/{entityType}/{entityId} - 특정 엔티티의 변경 이력 조회 (entityType: user, admin, CQRS 패턴 예외로 Aurora MySQL 조회)
     - GET /api/v1/auth/history/{entityType}/{entityId}/at?timestamp={timestamp} - 특정 시점 데이터 조회 (entityType: user, admin, CQRS 패턴 예외로 Aurora MySQL 조회)
     - POST /api/v1/auth/history/{entityType}/{entityId}/restore?historyId={historyId} - 특정 버전으로 복구 (entityType: user, admin, 관리자만 접근 가능)
   - **api-bookmark 모듈**:
     - GET /api/v1/bookmark/history/{entityId} - 북마크 엔티티의 변경 이력 조회 (CQRS 패턴 예외로 Aurora MySQL 조회)
     - GET /api/v1/bookmark/history/{entityId}/at?timestamp={timestamp} - 특정 시점 데이터 조회 (CQRS 패턴 예외로 Aurora MySQL 조회)
     - POST /api/v1/bookmark/history/{entityId}/restore?historyId={historyId} - 특정 버전으로 복구 (관리자만 접근 가능)

4. 성능 최적화
   - 비동기 처리 (Kafka 이벤트 활용)
   - 히스토리 테이블 파티셔닝 (날짜 기준)
   - 오래된 히스토리 데이터 아카이빙 (1년 이상)
   - 인덱스 전략: `operation_type + changed_at` 복합 인덱스 활용

### 7. MSA 멀티모듈 아키텍처 구축

**목표**: 마이크로서비스 아키텍처 기반 멀티모듈 프로젝트 구조로 확장성 및 유지보수성 향상

**모듈 구조**:
  ``` 
  project-root/
  ├── domain/                    # 도메인 모듈 (DB 연동 담당)
  │   ├── domain-aurora/        # Amazon Aurora MySQL 관련 (Command Side)
  │   └── domain-mongodb/       # MongoDB Atlas 관련 (Query Side)
  ├── common/                    # 공통 모듈
  │   ├── common-core/          # 핵심 유틸리티
  │   ├── common-security/      # 보안 관련 (JWT, Spring Security)
  │   ├── common-kafka/         # Kafka 관련
  │   └── common-exception/     # 예외 처리
  ├── client/                    # 외부 API 연동 모듈
  │   ├── client-feign/         # OpenFeign 클라이언트
  │   ├── client-rss/           # RSS 피드 파서
  │   ├── client-scraper/       # 웹 스크래핑
  │   └── client-slack/          # Slack 알림 클라이언트
  ├── batch/                     # Spring Batch 모듈
  │   └── batch-source/   # json/sources.json 업데이트 배치
  └── api/                       # API Server 모듈(들)
      ├── api-gateway/           # API Gateway (선택사항)
      ├── api-contest/           # 대회 정보 API 
      ├── api-news/              # 뉴스 정보 API 
      ├── api-auth/              # 인증 API 
      └── api-bookmark/           # 북마크 API 
  ```

**모듈 설명**:

- **domain/**: 데이터베이스 연동 담당 모듈
  - `domain-aurora/`: Amazon Aurora MySQL 관련 엔티티 및 Repository (Command Side, 참고: `docs/step1/3. aurora-schema-design.md`)
    * **역할**: 쓰기 작업 전용 데이터베이스 연동
    * **책임**: JPA 엔티티 관리, 트랜잭션 처리, 데이터 무결성 보장
    * **검증 기준**: 모든 쓰기 작업이 정상적으로 저장되고 트랜잭션이 롤백 가능해야 함
    * **스키마 매핑**: API 모듈별 스키마 분리 (auth, bookmark)
    * **TSID Primary Key**: 모든 테이블의 Primary Key는 TSID 방식 사용
  - `domain-mongodb/`: MongoDB Atlas 클라우드 서비스 관련 Document 및 Repository (Query Side, 참고: `docs/step1/2. mongodb-schema-design.md`)
    * **연결 대상**: MongoDB Atlas Cluster (클라우드 서비스)
    * **역할**: 읽기 작업 전용 데이터베이스 연동
    * **책임**: 읽기 최적화된 쿼리, Full-text search, 인덱스 관리
    * **TSID 동기화**: `bookmarkTsid`, `userTsid` 필드로 Aurora MySQL과 1:1 매핑
- **common/**: 공통 기능 모듈
  - `common-core/`: 핵심 유틸리티 클래스
  - `common-security/`: 보안 관련 (JWT, Spring Security 설정)
  - `common-kafka/`: Kafka Producer/Consumer 설정 및 이벤트 모델
  - `common-exception/`: 전역 예외 처리 및 커스텀 예외

- **client/**: 외부 API 연동 모듈
  - `client-feign/`: OpenFeign 클라이언트 (Codeforces, Kaggle, GitHub 등, OAuth Provider API 호출 포함)
  - `client-rss/`: RSS 피드 파서 (Rome 라이브러리, NewsArticleDocument 수집)
  - `client-scraper/`: 웹 스크래핑 (Jsoup, Selenium, ContestDocument 수집)
  - `client-slack/`: Slack 알림 클라이언트 (Webhook, Bot API, 참고: `docs/step8/slack-integration-design-guide.md`)

- **batch/**: Spring Batch 모듈
  - `batch-source/`: json/sources.json 업데이트 배치 작업 (AI LLM 통합, 참고: `docs/step11/ai-integration-analysis.md`)

- **api/**: API Server 모듈(들)
  - `api-gateway/`: API Gateway (선택사항)
  - `api-contest/`: 대회 정보 API
  - `api-news/`: 뉴스 정보 API
  - `api-auth/`: 인증 API
  - `api-bookmark/`: 북마크 API

**작업 세부사항**:
1. 프로젝트 구조 설계
   - Gradle 멀티모듈 프로젝트 설정
   - 모듈 간 의존성 정의
   - 공통 의존성 관리 (dependencyManagement)
   - 버전 관리 전략

2. Domain 모듈 구현
   - **역할**: 데이터 접근 계층 구현
   - **책임**: 
     * 엔티티/Document 정의
     * Repository 인터페이스 구현
     * 데이터소스 설정
   - **검증 기준**: 
     * 모든 엔티티/Document가 정상적으로 저장/조회 가능해야 함
     * Repository 메서드가 정상적으로 동작해야 함
     * 데이터소스 연결이 정상적으로 동작해야 함
     * **빌드 검증**: domain 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build`, `./gradlew :domain-mongodb:build` 등 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - domain-aurora: JPA 엔티티, Repository (Command Side)
     * **파일 구조 예제**:
       ```
       domain/domain-aurora/
       ├── build.gradle
       ├── src/main/java/com/ebson/domain/aurora/
       │   ├── entity/
       │   │   ├── User.java
       │   │   ├── Bookmark.java
       │   │   └── ...
       │   └── repository/
       │       ├── UserRepository.java
       │       ├── BookmarkRepository.java
       │       └── ...
       └── src/main/resources/
           └── application.yml
       ```
     * **build.gradle 예제**:
       ```gradle
       dependencies {
           implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
           implementation 'mysql:mysql-connector-java:8.0.33'
       }
       ```
   - domain-mongodb: MongoDB Atlas Document, Repository (Query Side, 참고: `docs/step1/2. mongodb-schema-design.md`)
     * **파일 구조 예제**:
       ```
       domain/domain-mongodb/
       ├── build.gradle
       ├── src/main/java/com/ebson/domain/mongodb/
       │   ├── document/
       │   │   ├── SourcesDocument.java
       │   │   ├── ContestDocument.java
       │   │   ├── NewsArticleDocument.java
       │   │   ├── BookmarkDocument.java
       │   │   ├── UserProfileDocument.java
       │   │   ├── ExceptionLogDocument.java
       │   │   └── ...
       │   └── repository/
       │       ├── SourcesDocumentRepository.java
       │       ├── ContestDocumentRepository.java
       │       ├── NewsArticleDocumentRepository.java
       │       ├── BookmarkDocumentRepository.java
       │       ├── UserProfileDocumentRepository.java
       │       ├── ExceptionLogDocumentRepository.java
       │       └── ...
       └── src/main/resources/
           └── application.yml
       ```
     * **build.gradle 예제**:
       ```gradle
       dependencies {
           implementation 'org.springframework.boot:spring-boot-starter-data-mongodb'
       }
       ```
   - 각 모듈의 독립적인 데이터소스 설정
     * **역할**: 모듈별 데이터소스 연결 관리
     * **책임**: 
       - 데이터소스 URL, 사용자명, 비밀번호 설정
       - 연결 풀 설정
       - 트랜잭션 관리 설정
     * **예제** (domain-aurora/src/main/resources/application-api-domain.yml):
       ```yaml
       spring:
         datasource:
           writer:
             url: jdbc:mysql://${AURORA_WRITER_ENDPOINT}:3306/${module.aurora.schema}?${AURORA_OPTIONS}
             username: ${AURORA_USERNAME}
             password: ${AURORA_PASSWORD}
           reader:
             url: jdbc:mysql://${AURORA_READER_ENDPOINT}:3306/${module.aurora.schema}?${AURORA_OPTIONS}
             username: ${AURORA_USERNAME}
             password: ${AURORA_PASSWORD}
           hikari:
             minimum-idle: 5
             maximum-pool-size: 20
         jpa:
           hibernate:
             ddl-auto: validate
           show-sql: false
       ```
       **참고**: 각 API 모듈의 `api-*-application.yml`에서 `module.aurora.schema` 설정 필요 (api-auth: `auth`, api-bookmark: `bookmark`)

3. Common 모듈 구현
   - common-core: 유틸리티 클래스, 상수, 헬퍼
   - common-security: JWT, Spring Security 설정, 필터
   - common-kafka: Kafka Producer/Consumer 설정, 이벤트 모델
   - common-exception: 전역 예외 처리, 커스텀 예외

4. Client 모듈 구현
   - client-feign: OpenFeign 클라이언트 (Codeforces, Kaggle, GitHub 등)
   - client-rss: RSS 피드 파서 (Rome 라이브러리)
   - client-scraper: 웹 스크래핑 (Jsoup, Selenium)
   - client-slack: Slack 알림 클라이언트 (Webhook, Bot API)
   - Rate Limiting, Retry 로직 포함

5. Batch 모듈 구현
   - batch-source-update: json/sources.json 업데이트 배치 작업
   - Spring Batch Job, Step, Tasklet 구성
   - Jenkins 연동을 위한 독립 실행 가능한 JAR

6. API 모듈 구현
   - 각 API 모듈은 독립적인 Spring Boot 애플리케이션
   - domain, common, client 모듈 의존
   - 모듈별 포트 분리 (예: 8080, 8081, 8082)
   - API Gateway를 통한 통합 엔드포인트 제공 (선택사항)

7. 모듈 간 통신
   - 동기 통신: REST API (Feign Client)
   - 비동기 통신: Kafka 이벤트
   - 공통 DTO는 common 모듈에 정의

### 8. Spring Batch 기반 배치 처리 시스템

**목표**: json/sources.json 정기 자동 업데이트를 위한 Spring Batch 모듈 구축 및 Jenkins 연동

**주요 업무**:
- json/sources.json 정기 자동 업데이트 (월 1회)
- 출처 검증 및 상태 모니터링
- 변경 이력 관리 및 알림

**Jenkins 연동 요구사항**:
- Jenkins Server에서 스케줄링
- 배치 작업 실행 상태 모니터링
- 실패 시 알림 및 재시도

**작업 세부사항**:
1. Spring Batch Job 설계
   - Job: SourceUpdateJob
   - Step 1: SourceDiscoveryStep - source-discovery-prompt.md 실행
   - Step 2: SourceValidationStep - 기존 출처 검증
   - Step 3: SourceComparisonStep - 변경 사항 비교
   - Step 4: SourceUpdateStep - json/sources.json 업데이트
   - Step 5: NotificationStep - Slack 알림 발송 (client-slack 모듈 활용)

2. 배치 인프라 구성
   - JobRepository: Amazon Aurora MySQL 사용 (Spring Batch 메타데이터)
   - JobLauncher: 배치 작업 실행
   - JobParameters: 실행 파라미터 (실행일시, 버전 등)
   - JobExecutionListener: 실행 전후 로깅 및 알림

3. Jenkins Pipeline 구성
   - Jenkinsfile 작성
   - 스케줄링 설정 (Cron: 매월 1일 자정)
   - 배치 JAR 빌드 및 실행
   - 실행 결과 모니터링 및 알림
   - 실패 시 재시도 로직

4. 배치 모니터링
   - JobExecution 상태 추적
   - StepExecution 상세 로그
   - 실행 시간, 처리 건수 통계
   - 실패 원인 분석 및 리포트

5. 에러 핸들링
   - Step 실패 시 Skip 또는 Retry
   - Job 실패 시 알림 및 롤백
   - Dead Letter Queue 처리
   - 재실행 가능한 Job 설계

### 9-1. API 응답 형식 및 에러 핸들링 표준화

**목표**: 모든 API 응답이 일관된 형식을 따르도록 표준화하고, HTTP 상태 코드와 비즈니스 에러 코드를 분리하여 상세한 에러 정보 제공

**참고 문서**:
- `docs/step2/3. api-response-format-design.md` - API 응답 형식 설계서
- `docs/step2/4. error-handling-strategy-design.md` - 에러 핸들링 전략 설계서

**입력 요구사항**:
- 모든 API 엔드포인트
- 에러 발생 시나리오

**출력 결과**:
- 표준화된 성공 응답 형식
- 표준화된 에러 응답 형식
- 페이징 응답 형식
- GlobalExceptionHandler 구현

**성공 기준**:
- 모든 API 응답이 표준 형식을 따르야 함
- 에러 응답이 일관된 구조를 가져야 함
- HTTP 상태 코드와 비즈니스 에러 코드가 분리되어야 함
- GlobalExceptionHandler가 모든 예외를 중앙에서 처리해야 함

**구현 세부사항** (참고: `docs/step2/3. api-response-format-design.md`, `docs/step2/4. error-handling-strategy-design.md`):

1. **표준 성공 응답 형식**
   - 기본 구조:
     ```json
     {
       "code": "2000",
       "messageCode": {
         "code": "SUCCESS",
         "text": "성공"
       },
       "message": "success",
       "data": {
         // 응답 데이터
       }
     }
     ```
   - 필드 설명:
     * `code`: 응답 코드 (성공: "2000", 기타 비즈니스 코드)
     * `messageCode`: 메시지 코드 객체 (국제화 지원)
     * `message`: 응답 메시지 (기본: "success")
     * `data`: 응답 데이터 객체 (단일 객체, 배열, 또는 null)

2. **페이징 응답 형식**
   - 페이징이 있는 리스트 응답:
     ```json
     {
       "code": "2000",
       "messageCode": {
         "code": "SUCCESS",
         "text": "성공"
       },
       "message": "success",
       "data": {
         "pageSize": 10,
         "pageNumber": 1,
         "totalPageNumber": 10,
         "totalSize": 100,
         "list": [
           // 데이터 리스트 배열
         ]
       }
     }
     ```
   - 페이징 계산 규칙:
     * `totalPageNumber = Math.ceil(totalSize / pageSize)`
     * `pageNumber`는 1부터 시작
     * `pageSize`는 기본값 10, 최대값 100

3. **에러 응답 형식**
   - 기본 구조:
     ```json
     {
       "code": "4000",
       "messageCode": {
         "code": "BAD_REQUEST",
         "text": "잘못된 요청입니다."
       }
     }
     ```
   - **주의**: 에러 응답에는 `message` 필드와 `data` 필드가 없습니다.
   - 유효성 검증 에러 응답 (필드별 상세 정보 포함):
     ```json
     {
       "code": "4006",
       "messageCode": {
         "code": "VALIDATION_ERROR",
         "text": "유효성 검증에 실패했습니다."
       },
       "errors": [
         {
           "field": "email",
           "code": "EMAIL_INVALID",
           "message": "이메일 형식이 올바르지 않습니다."
         }
       ]
     }
     ```

4. **에러 코드 체계** (참고: `docs/step2/4. error-handling-strategy-design.md`)
   - **2xxx**: 성공
     * 2000: 일반 성공
   - **4xxx**: 클라이언트 에러
     * 4000: 잘못된 요청 (BAD_REQUEST)
     * 4001: 인증 실패 (AUTH_FAILED)
     * 4002: 인증 필요 (AUTH_REQUIRED)
     * 4003: 권한 없음 (FORBIDDEN)
     * 4004: 리소스 없음 (NOT_FOUND)
     * 4005: 충돌 (CONFLICT)
     * 4006: 유효성 검증 실패 (VALIDATION_ERROR)
     * 4029: Rate limit 초과 (RATE_LIMIT_EXCEEDED)
   - **5xxx**: 서버 에러
     * 5000: 내부 서버 오류 (INTERNAL_SERVER_ERROR)
     * 5001: 데이터베이스 오류 (DATABASE_ERROR)
     * 5002: 외부 API 오류 (EXTERNAL_API_ERROR)
     * 5003: 서비스 불가 (SERVICE_UNAVAILABLE)
     * 5004: 타임아웃 (TIMEOUT)

5. **HTTP 상태 코드와 비즈니스 에러 코드 분리**
   - HTTP 상태 코드: HTTP 프로토콜 레벨의 상태 (200, 400, 401, 403, 404, 500 등)
   - 비즈니스 에러 코드: 애플리케이션 레벨의 상세 에러 코드 (응답 body의 `code` 필드)
   - 매핑 규칙:
     * 200 OK → 비즈니스 코드: "2000"
     * 400 Bad Request → 비즈니스 코드: "4000", "4006"
     * 401 Unauthorized → 비즈니스 코드: "4001", "4002"
     * 403 Forbidden → 비즈니스 코드: "4003"
     * 404 Not Found → 비즈니스 코드: "4004"
     * 409 Conflict → 비즈니스 코드: "4005"
     * 429 Too Many Requests → 비즈니스 코드: "4029"
     * 500 Internal Server Error → 비즈니스 코드: "5000", "5001"
     * 502 Bad Gateway → 비즈니스 코드: "5002"
     * 503 Service Unavailable → 비즈니스 코드: "5003"
     * 504 Gateway Timeout → 비즈니스 코드: "5004"

6. **GlobalExceptionHandler 구현** (참고: `docs/step2/4. error-handling-strategy-design.md`)
   - 모든 예외를 중앙에서 처리하는 `GlobalExceptionHandler` 구현
   - 예외 타입별 처리:
     * 비즈니스 예외: 적절한 4xxx 에러 코드 반환
     * 시스템 예외: 적절한 5xxx 에러 코드 반환
     * 예상치 못한 예외: 5000 에러 코드 반환
   - 예외 처리 흐름:
     ```
     요청 → 컨트롤러 → 서비스 → 리포지토리
                               ↓
                         예외 발생
                               ↓
                   예외 타입에 따라 분기
                               ↓
         ┌─────────────────────┼─────────────────────┐
         ↓                     ↓                     ↓
    비즈니스 예외        시스템 예외        예상치 못한 예외
         ↓                     ↓                     ↓
    4xxx 에러 코드        5xxx 에러 코드        5000 에러 코드
         ↓                     ↓                     ↓
                  GlobalExceptionHandler
                               ↓
                        에러 응답 반환
     ```

7. **공통 DTO 클래스 생성**
   - `SuccessResponse<T>`: 성공 응답 DTO
   - `ErrorResponse`: 에러 응답 DTO
   - `MessageCode`: 메시지 코드 DTO (국제화 지원)
   - `PagedResponse<T>`: 페이징 응답 DTO
   - `FieldError`: 필드별 에러 정보 DTO

8. **비즈니스 예외 클래스 구현**
   - `BusinessException`: 기본 비즈니스 예외 클래스
   - `ResourceNotFoundException`: 리소스 없음 예외 (4004)
   - `ConflictException`: 충돌 예외 (4005)
   - `ValidationException`: 유효성 검증 실패 예외 (4006)
   - 기타 비즈니스 예외 클래스

9. **에러 로깅 전략**
   - 로깅 레벨:
     * ERROR: 시스템 예외, 예상치 못한 예외
     * WARN: 비즈니스 예외 (중요한 경우)
     * INFO: 일반적인 비즈니스 예외 (선택적)
   - 로깅 정보:
     * 에러 코드, 에러 메시지
     * 요청 정보 (URL, HTTP 메서드, 파라미터)
     * 사용자 정보 (userId, IP 주소)
     * 스택 트레이스 (시스템 예외, 예상치 못한 예외만)
     * 요청 ID (트레이싱용)

### 9. Spring REST Docs 기반 API 문서화

**목표**: 테스트 기반 API 문서 자동 생성으로 정확하고 최신 상태를 유지하는 API 문서 제공

**요구사항**:
- 모든 REST API 엔드포인트에 대한 문서 자동 생성
- 테스트 코드 기반 문서 생성으로 코드와 문서의 일관성 보장
- Asciidoctor 형식의 문서 생성
- HTML 및 PDF 형식으로 변환 가능
- 예제 요청/응답 자동 포함
- API 버전별 문서 관리

**구현 방법**:
- Spring REST Docs 라이브러리 활용
- MockMvc 또는 WebTestClient를 통한 테스트 기반 문서 생성
- Asciidoctor 템플릿 커스터마이징
- CI/CD 파이프라인에 문서 생성 통합

**작업 세부사항**:
1. Spring REST Docs 설정
   - **역할**: API 문서 자동 생성 인프라 구축
   - **책임**: 
     * 의존성 설정
     * 플러그인 구성
     * 문서 생성 경로 설정
   - **검증 기준**: 
     * 의존성이 정상적으로 추가되어야 함
     * 문서 생성이 정상적으로 동작해야 함
   
   - build.gradle에 의존성 추가
     * **파일 위치**: 루트 build.gradle 또는 각 모듈의 build.gradle
     * **의존성 예제**:
       ```gradle
       dependencies {
           testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
           // 또는
           testImplementation 'org.springframework.restdocs:spring-restdocs-webtestclient'
           testImplementation 'org.springframework.restdocs:spring-restdocs-asciidoctor'
       }
       
       plugins {
           id 'org.asciidoctor.jvm.convert' version '3.3.2'
       }
       
       asciidoctor {
           dependsOn test
           sources {
               include '**/index.adoc'
           }
       }
       ```
     * spring-restdocs-mockmvc 또는 spring-restdocs-webtestclient
     * asciidoctor-gradle-plugin
   - REST Docs 설정 클래스 구현
     * **역할**: REST Docs 기본 설정
     * **책임**: 문서 스니펫 형식, 출력 경로 설정
   - Asciidoctor 플러그인 설정
     * **역할**: Asciidoctor 문서 변환
     * **책임**: HTML/PDF 변환 설정
   - 문서 출력 디렉토리 설정 (build/generated-snippets)
     * **기본 경로**: `build/generated-snippets`
     * **HTML 출력 경로**: `build/docs/asciidoc/html5`

2. API 문서 생성 전략
   - 각 API 모듈별 독립적인 문서 생성
   - 공통 섹션 (인증, 에러 처리, 페이징 등) 분리
   - 모듈별 문서 통합 (선택사항)
   - 버전별 문서 관리

3. 테스트 기반 문서 작성
   - MockMvcTest 또는 WebTestClientTest 작성
   - 각 엔드포인트별 문서 스니펫 생성:
     * request-fields.adoc (요청 필드)
     * response-fields.adoc (응답 필드)
     * path-parameters.adoc (경로 파라미터)
     * query-parameters.adoc (쿼리 파라미터)
     * request-body.adoc (요청 본문 예제)
     * response-body.adoc (응답 본문 예제)
     * http-request.adoc (HTTP 요청 예제)
     * http-response.adoc (HTTP 응답 예제)
   - 커스텀 스니펫 추가 (에러 응답, 인증 예제 등)

4. Asciidoctor 문서 작성
   - API 문서 템플릿 작성 (index.adoc)
   - 모듈별 섹션 구성:
     * 공개 API (Contest, News, Source)
     * 인증 API (Auth)
     * 사용자 북마크 API (Bookmark)
     * 변경 이력 조회 API (History)
   - 공통 섹션:
     * 인증 방법 (JWT 토큰 사용법)
     * 에러 처리 (에러 코드 및 응답 형식)
     * 페이징 (페이징 파라미터 및 응답)
     * 필터링 및 정렬
   - 예제 코드 및 사용 가이드 포함
   - 에러 코드 및 응답 형식 문서화

5. 문서 빌드 및 배포
   - Gradle 빌드 시 자동 문서 생성
   - HTML 형식으로 변환 (build/docs/asciidoc/html5)
   - PDF 형식으로 변환 (선택사항)
   - 정적 파일로 배포 (GitHub Pages, S3 등)
   - CI/CD 파이프라인에 문서 빌드 통합

6. 문서 품질 관리
   - 문서 커버리지 확인 (모든 엔드포인트 문서화)
   - 문서 자동 검증 (빌드 시)
   - 문서 버전 관리
   - 변경 이력 추적

## 기술 스택 (확장)

**기존 기술 스택**:
- Java 21
- Spring Boot 4.0.1
- Spring Data JPA
- Amazon Aurora MySQL (MySQL 8.0 호환)
  * **버전**: Aurora MySQL 3.x (MySQL 8.0.34+ 호환)
  * **특징**: 고가용성, 자동 스케일링, 자동 백업, 읽기 복제본 지원
  * **연결**: AWS RDS Data API 또는 JDBC 연결
- Redis (캐싱)
- OpenFeign (외부 API 호출)

**추가 기술 스택**:
- Spring Security (인증/인가)
- JWT (jjwt 라이브러리)
- Spring Data MongoDB
- Apache Kafka
- Spring Cloud Stream (Kafka 통합)
- BCrypt (비밀번호 암호화)
- OAuth 2.0 Client (SNS 로그인)
- Flyway (데이터베이스 마이그레이션)
- Hibernate Envers (변경 이력 추적, 선택사항)
- Spring Batch (배치 처리)
- Spring AI (AI LLM 통합) 또는 LangChain4j (AI LLM 통합)
- Spring REST Docs (API 문서화)
- Asciidoctor (문서 변환)
- Slack Webhook API / Slack Bot API (알림)
- WebClient / RestTemplate (HTTP 클라이언트)
- Jenkins (CI/CD 및 스케줄링)
- Gradle (멀티모듈 빌드)

## 구현 우선순위

### Phase 1: 프로젝트 구조 및 인프라 (2주)

**목표**: 프로젝트 기반 구조 및 인프라 구축 완료

**역할 및 책임**:
- **개발자**: 멀티모듈 구조 설계 및 구현
- **인프라 엔지니어**: AWS Aurora 클러스터 설정 및 보안 구성
- **검증자**: 설계서 검토 및 인프라 구성 검증

**작업 항목**:
1. 멀티모듈 프로젝트 구조 설계 및 생성
   - **검증 기준**: 각 모듈이 독립적으로 빌드 및 테스트 가능해야 함
   - **검증 기준**: 각 서브 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :{module-name}:build` 명령이 성공해야 함)
   - **검증 기준**: 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   - **제약사항**: 모듈 간 순환 의존성 금지
2. Gradle 멀티모듈 설정
   - **검증 기준**: 루트 프로젝트에서 모든 모듈 빌드 성공
   - **검증 기준**: 각 서브 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :{module-name}:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. 데이터베이스 설계서 생성
   - **검증 기준**: 모든 테이블/컬렉션 스키마가 문서화되어야 함
4. Amazon Aurora MySQL 테이블 생성 (Flyway, 히스토리 테이블 포함)
   - **역할**: Command Side 데이터 저장소 구축
   - **책임**: 
     * Aurora 클러스터 생성 및 보안 그룹 설정
     * Flyway 마이그레이션 스크립트 작성 및 실행
     * 히스토리 테이블 생성 및 인덱스 설정
   - **검증 기준**: 
     * 모든 마이그레이션이 성공적으로 실행되어야 함
     * 테이블 구조가 설계서와 일치해야 함
     * 인덱스가 정상적으로 생성되어야 함
     * **빌드 검증**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   - **Aurora 특화 설정**:
     * Multi-AZ 배포 활성화 (고가용성)
     * 자동 백업 설정 (보관 기간: 7일)
     * 읽기 복제본 생성 (선택사항, 성능 최적화)
     * 파라미터 그룹 설정 (connection_timeout, max_connections 등)
5. MongoDB Atlas 컬렉션 생성
   - **검증 기준**: 모든 컬렉션과 인덱스가 정상적으로 생성되어야 함
   - **검증 기준**: domain-mongodb 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-mongodb:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
6. Kafka 설정 및 토픽 생성
   - **검증 기준**: 모든 토픽이 정상적으로 생성되고 Producer/Consumer 연결 가능해야 함
   - **검증 기준**: common-kafka 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-kafka:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
7. Jenkins 서버 설정 및 Pipeline 구성
   - **검증 기준**: Pipeline이 정상적으로 실행되고 빌드/테스트가 성공해야 함
   - **검증 기준**: 모든 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### Phase 2: Common 및 Domain 모듈 (1주)

**목표**: 공통 모듈 및 도메인 모듈 구현 완료

**역할 및 책임**:
- **백엔드 개발자**: 모듈 구현 및 테스트 작성
- **검증자**: 코드 리뷰 및 통합 테스트 검증

**작업 항목**:
1. common 모듈 구현 (core, security, kafka, exception)
   - **검증 기준**: 각 서브 모듈이 독립적으로 사용 가능해야 함
   - **검증 기준**: 각 서브 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :common-core:build`, `./gradlew :common-security:build`, `./gradlew :common-kafka:build`, `./gradlew :common-exception:build` 명령이 성공해야 함)
   - **검증 기준**: 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   - **예제**: `common-core` 모듈의 유틸리티 클래스가 다른 모듈에서 정상적으로 import 가능해야 함
2. domain-aurora 모듈 구현 (엔티티, Repository, Profile 기반 설정 분리)
   - **역할**: Command Side 데이터 접근 계층
   - **책임**: 
     * JPA 엔티티 정의 및 매핑
     * Repository 인터페이스 및 커스텀 쿼리 구현
     * 트랜잭션 관리
     * API Domain / Batch Domain 설정 분리 (Profile 기반)
   - **검증 기준**: 
     * 모든 엔티티가 정상적으로 저장/조회/수정/삭제 가능해야 함
     * 트랜잭션이 정상적으로 롤백 가능해야 함
     * Profile별 설정이 정상적으로 로드되어야 함
     * Reader/Writer DataSource가 정상적으로 분리되어야 함
     * **빌드 검증**: domain-aurora 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
     * **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
   
   - **패키지 구조**:
     ```
     domain/aurora/
     ├── config/
     │   ├── ApiDomainConfig.java (@Profile("api-domain"))
     │   ├── ApiDataSourceConfig.java
     │   ├── ApiMybatisConfig.java
     │   ├── BatchDomainConfig.java (@Profile("batch-domain"))
     │   ├── BatchMetaDataSourceConfig.java
     │   ├── BatchBusinessDataSourceConfig.java
     │   ├── BatchEntityManagerConfig.java
     │   ├── BatchJpaTransactionConfig.java
     │   └── BatchMyBatisConfig.java
     ├── entity/
     │   └── {Entity}.java
     └── repository/
         ├── reader/
         └── writer/
     ```
   
   - **API Domain 설정** (@Profile("api-domain")):
     * **역할**: API 서버용 데이터베이스 설정
     * **책임**: 
       * API Writer/Reader DataSource 분리
       * API JPA 설정
       * API MyBatis 설정 (복잡한 조회 쿼리 전용)
     * **MyBatis 사용 제한 정책**:
       * **사용 허용 범위**: 복잡한 조회 쿼리에만 제한적으로 사용
         - 데이터베이스 종속 함수를 사용해야 하는 경우 (예: MySQL의 DATE_FORMAT, JSON 함수 등)
         - 인라인 뷰 서브쿼리를 사용하는 복잡한 조회 쿼리
         - 성능 최적화가 필요한 복잡한 조회 쿼리 (예: 대량 데이터 조회, 복잡한 JOIN 등)
       * **사용 금지 범위**:
         - 모든 쓰기 작업 (INSERT, UPDATE, DELETE) - JPA Entity를 통해서만 수행
         - 단순 조회 쿼리 - JPA Repository 또는 QueryDSL 사용
         - CRUD 기본 작업 - JPA Repository 사용
     * **설정 파일**: `application-api-domain.yml`
     * **Config 클래스**:
       ```java
       // domain/aurora/config/ApiDomainConfig.java
       @Configuration
       @Profile("api-domain")
       @EnableJpaRepositories(...)
       public class ApiDomainConfig {
           // API Domain 설정
       }
       
       // domain/aurora/config/ApiDataSourceConfig.java
       @Configuration
       @Profile("api-domain")
       public class ApiDataSourceConfig {
           @Bean
           @Qualifier("apiWriterDataSource")
           public DataSource apiWriterDataSource() { ... }
           
           @Bean
           @Qualifier("apiReaderDataSource")
           public DataSource apiReaderDataSource() { ... }
       }
       
       // domain/aurora/config/ApiMybatisConfig.java
       @Configuration
       @Profile("api-domain")
       public class ApiMybatisConfig {
           @Bean(name = "apiSqlSessionWriterFactory")
           public SqlSessionFactory apiSqlSessionWriterFactory(...) { ... }
           
           @Bean(name = "apiSqlSessionReaderFactory")
           public SqlSessionFactory apiSqlSessionReaderFactory(...) { ... }
       }
       ```
     * **설정 파일 예제**:
       ```yaml
       # domain/aurora/src/main/resources/application-api-domain.yml
       spring:
         jpa:
           api:
             open-in-view: false
             generate-ddl: false
             database: mysql
             database-platform: org.hibernate.dialect.MySQLDialect
             hibernate.ddl-auto: none
             show-sql: true
             properties:
               hibernate:
                 storage_engine: innodb
                 default_batch_fetch_size: ${DB_FETCH_CHUNKSIZE:250}
                 jdbc:
                   batch_size: ${DB_BATCH_SIZE:50}
                   time_zone: ${TZ:Asia/Seoul}
         
         datasource:
           api:
             writer:
               hikari:
                 driver-class-name: software.aws.rds.jdbc.mysql.Driver
                 connection-timeout: 5000
                 maximum-pool-size: 20
                 minimum-idle: 5
                 auto-commit: false
                 transaction-isolation: TRANSACTION_READ_UNCOMMITTED
             reader:
               hikari:
                 driver-class-name: software.aws.rds.jdbc.mysql.Driver
                 connection-timeout: 5000
                 maximum-pool-size: 20
                 minimum-idle: 5
                 auto-commit: false
                 transaction-isolation: TRANSACTION_READ_UNCOMMITTED
       ```
     * **참고 파일**: 
       - `domain/aurora/config/ApiDomainConfig.java`
       - `domain/aurora/config/ApiDataSourceConfig.java`
       - `domain/aurora/config/ApiMybatisConfig.java`
       - `domain/aurora/src/main/resources/application-api-domain.yml`
   
   - **Batch Domain 설정** (@Profile("batch-domain")):
     * **역할**: Batch 서버용 데이터베이스 설정
     * **책임**: 
       * Batch Meta DataSource (Spring Batch 메타데이터용)
       * Batch Business Writer/Reader DataSource 분리
       * Batch JPA 설정
       * Batch MyBatis 설정 (복잡한 조회 쿼리 전용)
     * **MyBatis 사용 제한 정책**: API Domain 설정과 동일 (복잡한 조회 쿼리에만 사용, 쓰기 작업은 JPA Entity 사용)
       * Batch Transaction Manager 설정
     * **설정 파일**: `application-batch-domain.yml`
     * **Config 클래스**:
       ```java
       // domain/aurora/config/BatchDomainConfig.java
       @Configuration
       @Profile("batch-domain")
       public class BatchDomainConfig {
           // Batch Domain 설정
       }
       
       // domain/aurora/config/BatchMetaDataSourceConfig.java
       @Configuration
       @Profile("batch-domain")
       public class BatchMetaDataSourceConfig {
           @Bean
           @Qualifier("batchMetaDataSource")
           public DataSource batchMetaDataSource() { ... }
       }
       
       // domain/aurora/config/BatchBusinessDataSourceConfig.java
       @Configuration
       @Profile("batch-domain")
       public class BatchBusinessDataSourceConfig {
           @Bean
           @Qualifier("batchBusinessWriterDataSource")
           public DataSource batchBusinessWriterDataSource() { ... }
           
           @Bean
           @Qualifier("batchBusinessReaderDataSource")
           public DataSource batchBusinessReaderDataSource() { ... }
       }
       
       // domain/aurora/config/BatchEntityManagerConfig.java
       @Configuration
       @Profile("batch-domain")
       @EnableJpaRepositories(
           basePackages = {...},
           transactionManagerRef = "jpaTransactionManagerAutoCommitF",
           entityManagerFactoryRef = "secondaryEMF"
       )
       public class BatchEntityManagerConfig {
           @Bean(name = "primaryEMF")
           @Primary
           public LocalContainerEntityManagerFactoryBean primaryEMF() { ... }
           
           @Bean(name = "secondaryEMF")
           public LocalContainerEntityManagerFactoryBean secondaryEMF() { ... }
       }
       
       // domain/aurora/config/BatchJpaTransactionConfig.java
       @Configuration
       @Profile("batch-domain")
       public class BatchJpaTransactionConfig {
           @Bean(name = "primaryPlatformTransactionManager")
           @Primary
           PlatformTransactionManager primaryPlatformTransactionManager() { ... }
           
           @Bean(name = "businessWriterTransactionManager")
           PlatformTransactionManager secondaryWriterTransactionManager() { ... }
           
           @Bean(name = "businessReaderTransactionManager")
           PlatformTransactionManager secondaryReaderTransactionManager() { ... }
       }
       
       // domain/aurora/config/BatchMyBatisConfig.java
       @Configuration
       public class BatchMyBatisConfig {
           @Bean
           @Primary
           public SqlSessionFactory batchSqlSessionWriterFactory(...) { ... }
           
           @Bean(name = "batchSqlSessionReaderFactory")
           public SqlSessionFactory batchSqlSessionReaderFactory(...) { ... }
       }
       ```
     * **설정 파일 예제**:
       ```yaml
       # domain/aurora/src/main/resources/application-batch-domain.yml
       spring:
         jpa:
           batch:
             open-in-view: false
             generate-ddl: false
             database: mysql
             database-platform: org.hibernate.dialect.MySQLDialect
             hibernate.ddl-auto: none
             show-sql: true
             properties:
               hibernate:
                 storage_engine: innodb
                 default_batch_fetch_size: ${DB_FETCH_CHUNKSIZE:250}
                 jdbc:
                   batch_size: ${DB_BATCH_SIZE:50}
                   time_zone: ${TZ:Asia/Seoul}
         
         datasource:
           batch:
             meta:
               hikari:
                 driver-class-name: software.aws.rds.jdbc.mysql.Driver
                 connection-timeout: 5000
                 maximum-pool-size: 20
                 minimum-idle: 5
                 auto-commit: false
             writer:
               hikari:
                 driver-class-name: software.aws.rds.jdbc.mysql.Driver
                 connection-timeout: 5000
                 maximum-pool-size: 20
                 minimum-idle: 5
                 auto-commit: false
             reader:
               hikari:
                 driver-class-name: software.aws.rds.jdbc.mysql.Driver
                 connection-timeout: 5000
                 maximum-pool-size: 20
                 minimum-idle: 5
                 auto-commit: false
       ```
     * **참고 파일**: 
       - `domain/aurora/config/BatchDomainConfig.java`
       - `domain/aurora/config/BatchMetaDataSourceConfig.java`
       - `domain/aurora/config/BatchBusinessDataSourceConfig.java`
       - `domain/aurora/config/BatchEntityManagerConfig.java`
       - `domain/aurora/config/BatchJpaTransactionConfig.java`
       - `domain/aurora/config/BatchMyBatisConfig.java`
       - `domain/aurora/src/main/resources/application-batch-domain.yml`
   
   - **Profile 활성화 방법**:
     * API 서버: `spring.profiles.active=api-domain`
     * Batch 서버: `spring.profiles.active=batch-domain`
3. domain-mongodb 모듈 구현 (Document, Repository)
   - **검증 기준**: 모든 Document가 정상적으로 저장/조회 가능해야 함
   - **검증 기준**: domain-mongodb 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :domain-mongodb:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. 변경 이력 추적 시스템 구현 (히스토리 테이블)
   - **검증 기준**: 모든 쓰기 작업에 대해 히스토리가 자동으로 저장되어야 함
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### Phase 3: Client 모듈 (1주)
1. client-feign 모듈 구현 (외부 API 클라이언트)
   - **검증 기준**: client-feign 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :client-feign:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
2. client-rss 모듈 구현 (RSS 피드 파서)
   - **검증 기준**: client-rss 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :client-rss:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. client-scraper 모듈 구현 (웹 스크래핑)
   - **검증 기준**: client-scraper 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :client-scraper:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. client-slack 모듈 구현 (Slack 알림 클라이언트)
   - **검증 기준**: client-slack 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :client-slack:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
5. Rate Limiting 및 Retry 로직 구현
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### Phase 4: 사용자 인증 (1주)
1. User 엔티티 및 Repository 구현
   - **검증 기준**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
2. JWT 토큰 관리 구현 (common-security 활용)
   - **검증 기준**: common-security 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-security:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. 회원가입/로그인 API 구현 (api-auth 모듈)
   - **검증 기준**: api-auth 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-auth:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. Spring Security 설정
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
5. 보안 테스트
   - **검증 기준**: 테스트가 통과하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)

### Phase 5: CQRS 기반 아키텍처 (2주)
1. Command Side 구현 (domain-aurora)
   - **검증 기준**: domain-aurora 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-aurora:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
2. Query Side 구현 (domain-mongodb)
   - **검증 기준**: domain-mongodb 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :domain-mongodb:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. Kafka Producer 구현 (common-kafka)
   - **검증 기준**: common-kafka 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :common-kafka:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. Kafka Consumer 구현
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
5. 동기화 테스트
   - **검증 기준**: 테스트가 통과하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)

### Phase 6: API 모듈 구현 (2주)
1. api-contest 모듈 구현 (참고: `docs/step2/1. api-endpoint-design.md`)
   - **데이터 저장소**: MongoDB Atlas Cluster (Aurora DB 미사용)
   - **데이터 수집**: `client-scraper` 모듈에서 웹 스크래핑을 통해 ContestDocument 수집
   - **검증 기준**: api-contest 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-contest:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
2. api-news 모듈 구현 (참고: `docs/step2/1. api-endpoint-design.md`)
   - **데이터 저장소**: MongoDB Atlas Cluster (Aurora DB 미사용)
   - **데이터 수집**: `client-rss` 모듈에서 RSS 피드 파싱을 통해 NewsArticleDocument 수집
   - **검증 기준**: api-news 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-news:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. api-bookmark 모듈 구현
   - **검증 기준**: api-bookmark 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :api-bookmark:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. Soft Delete 구현
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
5. 권한 검증 구현
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

### Phase 7: Batch 모듈 및 Jenkins 연동 (2주)
1. AI LLM 통합 구현 (spring-ai 또는 langchain4j 선택, 참고: `docs/step11/ai-integration-analysis.md`)
   - spring-ai 프레임워크 구현 (권장, Phase 1 프로토타입)
   - langchain4j 프레임워크 구현 (대안, 구조화된 출력이 복잡하거나 고급 기능 필요 시)
   - 프레임워크 선택 전략 구현
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew :batch-source:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
2. batch-source 모듈 구현 (참고: `docs/step11/ai-integration-analysis.md`)
   - **검증 기준**: batch-source 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :batch-source:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
3. Spring Batch Job 구성 (SourceDiscoveryStep 포함)
   - **검증 기준**: batch-source 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :batch-source:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
4. 프롬프트 파일 로딩 및 LLM 호출 구현
   - **검증 기준**: batch-source 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :batch-source:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
5. JSON 응답 파싱 및 json/sources.json 생성 구현
   - **검증 기준**: batch-source 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew :batch-source:build` 명령이 성공해야 함)
   - **검증 기준**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)
6. Jenkins Pipeline 구성
   - **검증 기준**: Pipeline이 정상적으로 실행되고 빌드/테스트가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
7. 스케줄링 설정 (매월 1일 자정)
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
8. 모니터링 및 알림 시스템 구현 (참고: `docs/step8/slack-integration-design-guide.md`)
   - **Slack 알림 시나리오**: Job 실행 완료/실패, Step별 실행 결과, LLM API 호출 실패, JSON 파싱 실패, 파일 쓰기 실패 등
   - **client-slack 모듈 활용**: Slack Webhook 또는 Bot API를 통한 알림 발송
   - **검증 기준**: 관련 모듈들이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)

### Phase 8: 테스트 및 API 문서화 (1주)
1. 단위 테스트 작성
   - **검증 기준**: 모든 테스트가 통과하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
2. 통합 테스트 작성 (Spring REST Docs 포함)
   - **검증 기준**: 모든 테스트가 통과하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
3. Spring REST Docs 설정 및 문서 생성
   - **검증 기준**: 문서 생성이 정상적으로 동작하고 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
4. Asciidoctor 문서 작성
   - **검증 기준**: 문서 빌드가 성공해야 함 (`./gradlew clean build` 명령이 성공해야 함)
5. 문서 빌드 및 배포 자동화
   - **검증 기준**: 문서 빌드 및 배포가 정상적으로 동작해야 함 (`./gradlew clean build` 명령이 성공해야 함)

## 보안 체크리스트

- [ ] JWT 토큰 서명 검증
- [ ] Refresh Token Redis 저장 및 검증
- [ ] 비밀번호 BCrypt 암호화 (salt rounds: 12)
- [ ] SQL Injection 방지 (PreparedStatement)
- [ ] XSS 방지 (입력값 검증)
- [ ] CSRF 보호 활성화
- [ ] Rate Limiting 구현 (로그인 시도)
- [ ] HTTPS 강제 (프로덕션)
- [ ] 민감 정보 환경 변수 관리
- [ ] 로그에 민감 정보 포함 금지
- [ ] 세션 타임아웃 설정
- [ ] 계정 잠금 기능
- [ ] 이메일 인증 필수

## 성능 최적화 전략

1. **읽기 최적화 (MongoDB Atlas)**
   - 적절한 인덱스 설계
   - Aggregation Pipeline 활용
   - 읽기 전용 복제본 활용

2. **쓰기 최적화 (Amazon Aurora MySQL)**
   - 배치 삽입 활용
     * **예제**: `JpaRepository.saveAll()` 사용하여 여러 엔티티를 한 번에 저장
     * **검증 기준**: 배치 삽입 시 개별 삽입 대비 성능 향상 확인
   - 트랜잭션 최소화
     * **제약사항**: 트랜잭션 범위를 최소화하여 락 경합 감소
     * **검증 기준**: 트랜잭션 시간이 1초 이내로 유지되어야 함
   - 커넥션 풀 최적화
     * **Aurora 특화 설정**:
       - 최소 연결 수: 5 (유휴 상태 유지)
       - 최대 연결 수: 20 (Aurora 인스턴스 크기에 따라 조정)
       - 연결 타임아웃: 30초
       - 유휴 타임아웃: 10분
       - 최대 수명: 30분
     * **검증 기준**: 연결 풀 모니터링을 통해 연결 누수 없음 확인
   - **Aurora 읽기 복제본 활용**
     * 읽기 전용 쿼리는 읽기 복제본으로 라우팅
     * **예제**: `@Transactional(readOnly = true)` 사용 시 읽기 복제본 자동 라우팅
     * **검증 기준**: 읽기 복제본을 통한 쿼리 성능 향상 확인

3. **캐싱 전략**
   - Redis를 활용한 자주 조회되는 데이터 캐싱
   - 사용자 북마크 캐싱
   - JWT 토큰 검증 결과 캐싱

4. **Kafka 최적화**
   - 적절한 파티션 수 설정
   - Consumer 그룹 최적화
   - 배치 처리 활용

## 테스트 전략

1. **단위 테스트**
   - Service 레이어 테스트
   - Repository 레이어 테스트
   - JWT 토큰 관리 테스트

2. **통합 테스트**
   - API 엔드포인트 테스트
   - Kafka 동기화 테스트
   - 인증/인가 테스트

3. **보안 테스트**
   - SQL Injection 테스트
   - XSS 테스트
   - JWT 토큰 변조 테스트
   - Rate Limiting 테스트

4. **성능 테스트**
   - 부하 테스트
   - 동시 사용자 테스트
   - Kafka 처리량 테스트

## 배포 전략

**역할**: 프로덕션 환경 배포 및 운영 전략 수립
**책임**: 
- 안전한 배포 프로세스 수립
- 롤백 계획 수립
- 모니터링 및 알림 설정

**검증 기준**: 
- 배포가 안전하게 수행되어야 함
- 롤백이 정상적으로 동작해야 함
- 모니터링이 정상적으로 동작해야 함
- **빌드 검증**: 배포 전 모든 모듈이 정상적으로 빌드 가능해야 함 (`./gradlew clean build` 명령이 성공해야 함)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

1. **환경 변수 관리**
   - **역할**: 민감 정보 및 설정 관리
   - **책임**: 
     * 환경 변수 정의 및 관리
     * 보안 정책 준수
   - **검증 기준**: 
     * 모든 필수 환경 변수가 설정되어야 함
     * 민감 정보가 코드에 하드코딩되지 않아야 함
   
   필수 환경 변수:
   - JWT Secret Key
   - Amazon Aurora MySQL 연결 정보
     * `AURORA_WRITER_ENDPOINT`: Aurora Writer 엔드포인트
     * `AURORA_READER_ENDPOINT`: Aurora Reader 엔드포인트
     * `AURORA_USERNAME`: 데이터베이스 사용자명
     * `AURORA_PASSWORD`: 데이터베이스 비밀번호
     * `AURORA_OPTIONS`: JDBC 연결 옵션 (예: `useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8`)
     * **참고**: 각 API 모듈의 `api-*-application.yml`에서 `module.aurora.schema` 설정 (api-auth: `auth`, api-bookmark: `bookmark`)
     * **참고**: `domain/aurora/src/main/resources/application-api-domain.yml`에서 `${module.aurora.schema}` 사용하여 동적 스키마 참조
   - Kafka 연결 정보
   - OAuth 클라이언트 정보 (Google, Naver, Kakao, 참고: `docs/step6/oauth-provider-implementation-guide.md`)
   - Redis 연결 정보 (OAuth State 파라미터 저장용, Rate Limiting용, 참고: `docs/step7/redis-optimization-best-practices.md`)
     * **OAuth State 저장**: Key: `oauth:state:{state_value}`, TTL: 10분
     * **Rate Limiting**: RSS/Scraper 모듈 출처별 요청 간격 관리, Slack API 호출 빈도 제어
     * **연결 풀 설정**: 프로덕션 환경에서 연결 풀 최적화 필요
     * **타임아웃 설정**: 네트워크 지연 시 무한 대기 방지
     * **보안 설정**: Redis 인증 설정, TLS/SSL 설정 (프로덕션 환경 필수)
   - Anthropic API Key (AI LLM 통합, 참고: `docs/step11/ai-integration-analysis.md`)

2. **마이그레이션 전략**
   - **역할**: 데이터베이스 스키마 변경 관리
   - **책임**: 
     * Flyway를 통한 마이그레이션 실행
     * 롤백 계획 수립
   - **검증 기준**: 
     * 마이그레이션이 정상적으로 실행되어야 함
     * 롤백이 정상적으로 동작해야 함
   
   - Flyway를 통한 Amazon Aurora MySQL 마이그레이션
     * **Aurora 특화 고려사항**:
       - Multi-AZ 배포 시 자동으로 모든 인스턴스에 적용
       - 읽기 복제본에도 자동으로 적용
       - 마이그레이션 실행 전 백업 필수
   - MongoDB Atlas 스키마 버전 관리
   - 롤백 계획
     * **Aurora 롤백 전략**:
       - Point-in-Time Recovery 활용
       - 읽기 복제본을 통한 롤백 (필요 시)
       - Flyway 롤백 스크립트 준비

3. **모니터링**
   - **역할**: 시스템 상태 및 성능 모니터링
   - **책임**: 
     * 실시간 모니터링 및 알림
     * 성능 지표 수집 및 분석
     * 장애 감지 및 대응
   - **검증 기준**: 
     * 모든 주요 지표가 모니터링되어야 함
     * 알림이 정상적으로 동작해야 함
     * 대시보드가 실시간으로 업데이트되어야 함
   
   모니터링 항목:
   - 애플리케이션 로그
     * **도구**: CloudWatch Logs, ELK Stack
     * **지표**: 에러 로그, 성능 로그, 비즈니스 로그
   - Kafka 메시지 처리 모니터링
     * **지표**: 메시지 처리량, 지연 시간, 에러율
   - Amazon Aurora MySQL 성능 모니터링
     * **Aurora 특화 지표**:
       - CPU 사용률
       - 메모리 사용률
       - 연결 수
       - 쿼리 성능 (Slow Query Log)
       - 읽기/쓰기 IOPS
       - 읽기 복제본 지연 시간
       - 백업 상태
     * **도구**: CloudWatch, Performance Insights, RDS Enhanced Monitoring
     * **알림 임계값**:
       - CPU 사용률 > 80%
       - 연결 수 > 최대 연결 수의 80%
       - 읽기 복제본 지연 > 5초
   - MongoDB Atlas 성능 모니터링
     * **지표**: 쿼리 성능, 인덱스 사용률, 컬렉션 크기
   - 보안 이벤트 모니터링
     * **지표**: 인증 실패, 권한 위반, 비정상 접근

## 아키텍처 설계 원칙

### MSA 멀티모듈 구조 원칙
1. **모듈 독립성**: 각 모듈은 독립적으로 빌드 및 배포 가능
2. **의존성 방향**: API → Domain → Common → Client 순서
3. **공통 코드 중복 방지**: Common 모듈에 공통 기능 집중
4. **도메인 분리**: Command/Query 분리를 Domain 모듈에서 구현
5. **느슨한 결합**: 모듈 간 통신은 인터페이스 기반

### 변경 이력 추적 원칙
1. **자동화**: 모든 쓰기 작업에 대해 자동으로 히스토리 저장
2. **비동기 처리**: 성능 영향 최소화를 위한 비동기 저장
3. **불변성**: 히스토리 데이터는 수정 불가 (Append-only)
4. **복구 가능**: 특정 시점 데이터 복구 기능 제공
5. **보관 정책**: 오래된 히스토리 데이터 아카이빙

### 배치 처리 원칙
1. **독립 실행**: 배치 모듈은 독립적으로 실행 가능
2. **재실행 가능**: 실패 시 재실행 가능한 Job 설계
3. **모니터링**: 실행 상태 및 결과 모니터링 필수
4. **에러 핸들링**: 실패 시 알림 및 재시도 로직
5. **Jenkins 연동**: CI/CD 파이프라인 통합

### API 문서화 원칙
1. **테스트 기반**: 테스트 코드로 문서 자동 생성 (코드와 문서 일관성)
2. **자동화**: 빌드 시 자동으로 문서 생성 및 업데이트
3. **완전성**: 모든 API 엔드포인트 문서화 필수
4. **예제 포함**: 실제 사용 가능한 요청/응답 예제 포함
5. **버전 관리**: API 버전별 문서 관리 및 변경 이력 추적

## 다음 단계

이 작업 계획을 기반으로 다음 작업들을 순차적으로 실행하세요:

1. 멀티모듈 프로젝트 구조 생성 작업
2. 데이터베이스 설계서 생성 작업 (히스토리 테이블 포함)
3. Common 및 Domain 모듈 구현 작업
4. Client 모듈 구현 작업
5. 사용자 인증 시스템 구현 작업
6. CQRS 패턴 구현 작업
7. API 모듈 구현 작업
8. Batch 모듈 및 Jenkins 연동 작업
9. 테스트 및 Spring REST Docs 기반 API 문서화 작업

각 작업은 독립적으로 실행 가능하며, 의존성이 있는 경우 명시되어 있습니다.
```

## 단계별 작업 실행

### ✅ 1단계: 정보 출처 탐색 및 평가 (완료)

**상태**: `json/sources.json` 파일이 이미 생성되어 있습니다.

**생성된 파일**: `json/sources.json`

**주요 발견 사항**:
- **개발자 대회 정보**: 10개 출처 (Priority 1: 4개, Priority 2: 5개, Priority 3: 1개)
- **최신 IT 테크 뉴스**: 10개 출처 (Priority 1: 7개, Priority 2: 3개)

**다음 단계**: `json/sources.json` 파일을 참조하여 Priority 1 출처부터 통합을 시작하세요.

**참고**: 출처 탐색이 필요한 경우 `prompts/source-discovery-prompt.md` 프롬프트를 사용하세요.

**MongoDB Atlas 동기화**: `json/sources.json` 파일의 데이터는 MongoDB Atlas의 `SourcesDocument`로도 저장되며, `client-rss`와 `client-scraper` 모듈에서 참조됩니다 (참고: `docs/step1/2. mongodb-schema-design.md`).

### 0단계: 멀티모듈 프로젝트 구조 및 데이터베이스 설계서 생성 (최우선)

**역할**: 프로젝트 기반 구조 및 데이터베이스 설계
**책임**: 
- 멀티모듈 프로젝트 구조 설계
- 데이터베이스 스키마 설계
- 설계서 문서화

**입력 요구사항**:
- `json/sources.json` 파일 (정보 출처 데이터)
- 프로젝트 요구사항 문서
- 기술 스택 선정 결과

**출력 결과**:
- 멀티모듈 프로젝트 구조 (Gradle 설정)
- MongoDB Atlas 도큐먼트 설계서 (`docs/mongodb-schema-design.md`)
- Amazon Aurora MySQL 테이블 설계서 (`docs/aurora-schema-design.md`)

**성공 기준**:
- 모든 모듈이 정상적으로 생성되어야 함
- Gradle 빌드가 성공해야 함
- 설계서가 완전하고 정확해야 함
- 모든 테이블/컬렉션 스키마가 문서화되어야 함
- **빌드 검증**: 각 서브 모듈이 독립적으로 빌드 가능해야 함 (`./gradlew :{module-name}:build` 명령이 성공해야 함)
- **빌드 검증**: 루트 프로젝트에서 전체 빌드 성공 (`./gradlew clean build` 명령이 성공해야 함)
- **빌드 검증**: 컴파일 에러 없음 (모든 Java 파일이 정상적으로 컴파일되어야 함)

**에러 처리 시나리오**:
- 모듈 생성 실패: 의존성 재검토 및 구조 재설계
- Gradle 빌드 실패: 의존성 버전 확인 및 수정
- 설계서 검증 실패: 스키마 재검토 및 수정

```