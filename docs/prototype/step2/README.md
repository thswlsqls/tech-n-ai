# Step 2: API 설계 및 데이터 모델링

## Plan Task

```
plan task: API Server 아키텍처 설계 및 데이터 모델 정의
```

## 개요

Phase 1에서 완료된 멀티모듈 프로젝트 구조 검증 및 데이터베이스 설계서(`docs/phase1/`)를 기반으로, API Server의 아키텍처를 설계하고 데이터 모델을 정의합니다.

주요 작업 내용:
1. RESTful API 엔드포인트 설계: 공개 API(대회, 뉴스, 출처), 인증 API, 사용자 북마크 API(JWT 토큰 기반 사용자별 타게팅), 변경 이력 조회 API(CQRS 패턴 예외로 Aurora MySQL 조회)를 CQRS 패턴에 맞춰 읽기/쓰기로 분리하여 설계
2. CQRS 패턴 기반 데이터 모델 설계: Command Side(Amazon Aurora MySQL)와 Query Side(MongoDB Atlas)의 데이터 모델을 분리 설계하고 실시간 동기화 전략 수립 (User → UserProfileDocument, Bookmark → BookmarkDocument, TSID 필드 기반 동기화)
3. API 응답 형식 표준화: 성공/에러 응답 구조를 일관되게 정의하여 API 인터페이스 표준화
4. 에러 핸들링 전략 수립: HTTP 상태 코드와 비즈니스 에러 코드를 분리하여 상세한 에러 정보 제공

## 역할

- **API 아키텍트 및 데이터 모델 설계자**

## 책임

- RESTful API 엔드포인트 설계
- CQRS 패턴 기반 데이터 모델 설계
- API 응답 형식 및 에러 핸들링 전략 수립
- 설계서 문서화 및 검증

## 관련 설계서

### 생성 설계서
- `1. api-endpoint-design.md`: API 엔드포인트 설계
- `2. data-model-design.md`: 데이터 모델 설계
- `3. api-response-format-design.md`: API 응답 형식 설계
- `4. error-handling-strategy-design.md`: 에러 처리 전략 설계
- `5. design-verification-report.md`: 설계 검증 보고서

### 참조 문서
- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
- `../step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `../step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계

## 주요 작업 내용

1. RESTful API 엔드포인트 설계
   - 공개 API(인증 불필요): 대회 목록/상세/검색, 뉴스 목록/상세/검색, 출처 목록 조회
   - 인증 API: 회원가입, 로그인, 로그아웃, 토큰 갱신, 이메일 인증, 비밀번호 재설정, OAuth 2.0
   - 사용자 북마크 API(인증 필요): JWT 토큰에서 userId 추출하여 해당 사용자의 북마크만 타게팅
   - 변경 이력 조회 API(인증 필요): 히스토리 조회, 특정 시점 데이터 조회, 복구 (모두 Aurora MySQL에서 조회, CQRS 패턴 예외)

2. CQRS 패턴 기반 데이터 모델 분리 설계
   - Command Side(Aurora MySQL): TSID Primary Key, Soft Delete, 감사 필드, 히스토리 엔티티 설계
   - Query Side(MongoDB Atlas): ObjectId Primary Key, 읽기 최적화, 비정규화, ESR 규칙 준수 인덱스 설계
   - Kafka 이벤트 기반 실시간 동기화 전략 수립

3. API 응답 형식 표준화
4. 에러 핸들링 전략 수립

## 의존성

- 1단계: 프로젝트 구조 및 설계서 생성 완료 필요

## 다음 단계

- 3단계 (Common 모듈 구현)
