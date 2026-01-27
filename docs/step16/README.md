# Step 19: 이메일 인증 기능 구현

## Plan Task

```
plan task: api/auth 모듈 이메일 인증 기능 구현 - client/mail 모듈 생성 및 통합
```

## 개요

api/auth 모듈의 이메일 인증 기능을 완성하기 위한 설계 및 구현 단계입니다. 현재 api/auth 모듈의 회원가입 플로우에서 이메일 인증 토큰이 DB에 저장되지만 실제 이메일이 발송되지 않는 문제를 해결합니다.

## 단계 정보

- **단계 번호**: 16단계 (prompts/shrimp-task-prompt.md 기준)
- **의존성**: 5단계 (사용자 인증 및 관리 시스템), 4단계 (Domain 모듈)
- **다음 단계**: 17단계 (Batch 모듈 및 Jenkins 연동)

## 문서 목록

| 문서 | 설명 |
|------|------|
| [email-verification-implementation-design.md](./email-verification-implementation-design.md) | 이메일 인증 기능 구현 설계서 |

## 관련 프롬프트

| 프롬프트 | 위치 |
|----------|------|
| 이메일 인증 설계서 작성 프롬프트 | `prompts/step19/email-verification-design-prompt.md` |

## 주요 변경 사항

### 신규 모듈
- `client/mail`: 이메일 발송 클라이언트 모듈

### 수정 대상
- `api/auth`: EmailVerificationService 수정
- `settings.gradle`: 신규 모듈 등록

## 구현 순서

1. `client/mail` 모듈 생성
2. 이메일 발송 인터페이스 및 구현체 작성
3. Thymeleaf 템플릿 작성
4. EmailVerificationService 통합
5. 테스트 및 검증
