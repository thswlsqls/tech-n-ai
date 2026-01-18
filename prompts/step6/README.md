# Step 6: OAuth Provider별 로그인 기능 구현

## 개요

이 단계는 OAuth Provider별 로그인 기능을 구현합니다. Google, Naver, Kakao OAuth 로그인을 지원합니다.

## 관련 파일

### 프롬프트
- `oauth-provider-implementation-prompt.md`: OAuth Provider 구현 프롬프트
- `oauth-feign-client-migration-prompt.md`: OpenFeign 클라이언트 전환 프롬프트
- `oauth-http-client-selection-prompt.md`: HTTP Client 선택 프롬프트
- `oauth-state-storage-research-prompt.md`: State 파라미터 저장 연구 프롬프트
- `spring-security-auth-design-prompt.md`: Spring Security 설계 프롬프트

### 설계서
- `docs/step6/oauth-provider-implementation-guide.md`: OAuth Provider 구현 가이드
- `docs/step6/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 전환 분석
- `docs/step6/oauth-http-client-selection-analysis.md`: HTTP Client 선택 분석
- `docs/step6/oauth-state-storage-research-result.md`: State 파라미터 저장 연구 결과
- `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드

## 주요 내용

- Google OAuth 로그인 구현
- Naver OAuth 로그인 구현
- Kakao OAuth 로그인 구현
- OAuth State 파라미터 저장 (Redis)
- Spring Security 통합

## 의존성

- 5단계: 사용자 인증 및 관리 시스템 구현 - AuthService OAuth 메서드 구현 완료 필수
