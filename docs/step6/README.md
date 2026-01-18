# Step 6: OAuth Provider별 로그인 기능 구현

## 개요

이 단계는 OAuth Provider별 로그인 기능을 구현합니다. Google, Naver, Kakao OAuth 로그인을 지원합니다.

## 관련 설계서

### 생성 설계서
- `oauth-provider-implementation-guide.md`: OAuth Provider별 구현 가이드
- `oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 전환 검토 및 구현 가이드
- `oauth-http-client-selection-analysis.md`: HTTP Client 선택 분석
- `oauth-state-storage-research-result.md`: State 파라미터 저장 방법 연구 결과
- `spring-security-auth-design-guide.md`: Spring Security 설계 가이드

### 참조 문서
- `../step2/1. api-endpoint-design.md`: API 엔드포인트 설계
- `../step2/2. data-model-design.md`: 데이터 모델 설계
- `../step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계

## 주요 내용

- Google OAuth 로그인 구현
- Naver OAuth 로그인 구현
- Kakao OAuth 로그인 구현
- OAuth State 파라미터 저장 (Redis)
- Spring Security 통합

## 의존성

- 5단계: 사용자 인증 및 관리 시스템 구현 - AuthService OAuth 메서드 구현 완료 필수
