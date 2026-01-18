# Step 5: 사용자 인증 및 관리 시스템 구현

## 개요

이 단계는 사용자 인증 및 관리 시스템을 구현합니다. 회원가입, 로그인, 로그아웃, 토큰 갱신, 비밀번호 재설정 기능을 포함합니다.

## 관련 설계서

### 참조 문서
- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
- `../step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계
- `../step2/1. api-endpoint-design.md`: API 엔드포인트 설계
- `../step2/4. error-handling-strategy-design.md`: 에러 처리 전략 설계

## 주요 내용

- 회원가입 및 이메일 인증
- 로그인 및 JWT 토큰 발급
- 로그아웃 및 토큰 무효화
- 토큰 갱신
- 비밀번호 재설정

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 4단계: Domain 모듈 - User 엔티티 구현 완료 필수
