# Step 4: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)

## Plan Task

```
plan task: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)
```

## 개요

CQRS 패턴 기반 아키텍처에서 Command Side와 Query Side 데이터 접근 계층을 구현합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 4단계 섹션 참고

## 주요 작업 내용

- domain-aurora(Aurora MySQL) 모듈 구현
- domain-mongodb(MongoDB Atlas) 모듈 구현
- TSID Primary Key 전략 구현
- Profile 기반 설정 분리
- Soft Delete 패턴 구현
- 히스토리 자동 저장 메커니즘

## 의존성

- 1단계: 프로젝트 구조 생성
- 2단계: API 설계 완료 권장
- 3단계: Common 모듈 구현 완료 필수

## 다음 단계

- 5단계 (사용자 인증 시스템 구현) 또는 8단계 (Client 모듈 구현)
