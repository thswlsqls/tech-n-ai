# Step 16: 이메일 인증 기능 구현 (api/auth 모듈)

## Plan Task

```
plan task: api/auth 모듈 이메일 인증 기능 구현 - client/mail 모듈 생성 및 통합
```

## 개요

현재 api/auth 모듈의 회원가입 플로우에서 이메일 인증 토큰이 DB에 저장되지만 실제 이메일이 발송되지 않는 문제를 해결합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 16단계 섹션 참고
- `email-verification-design-prompt.md`: 이메일 인증 설계 프롬프트

### 설계서
- `docs/step19/email-verification-implementation-design.md`: 이메일 인증 구현 설계서

## 주요 작업 내용

- client/mail 모듈 생성 및 통합
- Spring Mail (JavaMailSender) 사용
- Thymeleaf 템플릿으로 이메일 본문 생성
- 비동기 이메일 발송, 트랜잭션 분리

## 의존성

- 5단계: 사용자 인증 및 관리 시스템 구현 완료 필수
- 4단계: Domain 모듈 구현 완료 필수

## 다음 단계

- 17단계 (Batch 모듈 및 Jenkins 연동)
