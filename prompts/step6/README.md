# Step 6: OAuth Provider별 로그인 기능 구현

## Plan Task

```
plan task: OAuth Provider별 로그인 기능 구현
```

## 개요

Google, Naver, Kakao OAuth 2.0 로그인 기능을 구현하여 각 Provider별 인증 URL 생성, Access Token 교환, 사용자 정보 조회 기능을 제공합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 6단계 섹션 참고
- `oauth-provider-implementation-prompt.md`: OAuth Provider 구현 프롬프트
- `oauth-feign-client-migration-prompt.md`: OpenFeign 클라이언트 마이그레이션 프롬프트
- `oauth-http-client-selection-prompt.md`: HTTP 클라이언트 선택 프롬프트
- `oauth-state-storage-research-prompt.md`: OAuth State 저장 연구 프롬프트
- `spring-security-auth-design-prompt.md`: Spring Security 인증 설계 프롬프트

### 설계서
- `docs/step6/oauth-provider-implementation-guide.md`: OAuth Provider 구현 가이드
- `docs/step6/oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 사용 방식 분석
- `docs/step6/oauth-state-storage-research-result.md`: OAuth State 저장 방법 연구 결과
- `docs/step6/spring-security-auth-design-guide.md`: Spring Security 설계 가이드

## 주요 작업 내용

- Google, Naver, Kakao OAuth 2.0 로그인 기능 구현
- 각 Provider별 인증 URL 생성, Access Token 교환, 사용자 정보 조회
- State 파라미터 Redis 저장 및 검증
- OpenFeign 클라이언트를 통한 OAuth Provider API 호출

## 의존성

- 5단계: 사용자 인증 및 관리 시스템 구현 - AuthService OAuth 메서드 구현 완료 필수

## 다음 단계

- 7단계 (Redis 최적화 구현) 또는 8단계 (Client 모듈 구현)
