# Step 2: API 설계 및 데이터 모델링

## Plan Task

```
plan task: API Server 아키텍처 설계 및 데이터 모델 정의
```

## 개요

Phase 1에서 완료된 멀티모듈 프로젝트 구조 검증 및 데이터베이스 설계서(`docs/phase1/`)를 기반으로, API Server의 아키텍처를 설계하고 데이터 모델을 정의합니다.

## 역할

- **API 아키텍트 및 데이터 모델 설계자**

## 책임

- RESTful API 엔드포인트 설계
- CQRS 패턴 기반 데이터 모델 설계
- API 응답 형식 및 에러 핸들링 전략 수립
- 설계서 문서화 및 검증

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 2단계 섹션 참고

### 설계서
- `docs/step2/1. api-endpoint-design.md`: API 엔드포인트 설계
- `docs/step2/2. data-model-design.md`: 데이터 모델 설계
- `docs/step2/3. api-response-format-design.md`: API 응답 형식 설계
- `docs/step2/4. error-handling-strategy-design.md`: 에러 처리 전략 설계
- `docs/step2/5. design-verification-report.md`: 설계 검증 리포트

## 주요 작업 내용

1. RESTful API 엔드포인트 설계
2. CQRS 패턴 기반 데이터 모델 분리 설계
3. API 응답 형식 표준화
4. 에러 핸들링 전략 수립

## 의존성

- 1단계: 프로젝트 구조 및 설계서 생성 완료 필요

## 다음 단계

- 3단계 (Common 모듈 구현)
