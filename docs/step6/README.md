# Step 6: OAuth Provider별 로그인 기능 구현

## Plan Task

```
plan task: OAuth Provider별 로그인 기능 구현
```

## 개요

Google, Naver, Kakao OAuth 2.0 로그인 기능을 구현하여 각 Provider별 인증 URL 생성, Access Token 교환, 사용자 정보 조회 기능을 제공합니다. 기존 AuthService의 OAuth 메서드와 통합하여 완전한 OAuth 로그인 플로우를 완성합니다.

## 주요 특징

- 각 Provider의 API 엔드포인트 URL과 Redirect URI는 환경변수로 관리
- OAuth 2.0 CSRF 공격 방지를 위한 State 파라미터는 Redis를 활용하여 저장 및 검증
- OAuth Provider API 호출은 `client/feign` 모듈의 OpenFeign 클라이언트를 사용

## 예상 결과

- Google, Naver, Kakao OAuth 로그인이 정상적으로 동작하는 API 서버
- 각 Provider별 인증 URL 생성이 정상적으로 동작함
- Authorization Code로 Access Token 교환이 정상적으로 동작함
- Access Token으로 사용자 정보 조회가 정상적으로 동작함
- State 파라미터가 Redis에 저장되고 검증되어 CSRF 공격이 방지됨

## 관련 설계서

- `oauth-provider-implementation-guide.md`: OAuth Provider 구현 가이드
- `oauth-feign-client-migration-analysis.md`: OpenFeign 클라이언트 사용 방식 분석
- `oauth-state-storage-research-result.md`: OAuth State 저장 방법 연구 결과
- `spring-security-auth-design-guide.md`: Spring Security 설계 가이드

## 의존성

- 5단계: 사용자 인증 및 관리 시스템 구현 - AuthService OAuth 메서드 구현 완료 필수

## 다음 단계

- 7단계 (Redis 최적화 구현) 또는 8단계 (Client 모듈 구현)
