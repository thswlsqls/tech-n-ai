# Step 3: Common 모듈 구현

## 개요

이 단계는 공통 모듈(common-core, common-security, common-kafka, common-exception)을 구현합니다.

## 관련 설계서

### 참조 문서
- `reference/shrimp-task-prompts-final-goal.md`: 최종 프로젝트 목표
- `../step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계
- `../step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `../step2/1. api-endpoint-design.md`: API 엔드포인트 설계
- `../step2/2. data-model-design.md`: 데이터 모델 설계
- `../step2/3. api-response-format-design.md`: API 응답 형식 설계
- `../step2/4. error-handling-strategy-design.md`: 에러 처리 전략 설계

## 주요 내용

- common-core: 공통 DTO, 유틸리티 클래스
- common-security: 보안 관련 공통 기능
- common-kafka: Kafka 이벤트 발행 및 구독
- common-exception: 공통 예외 처리

## 의존성

- 1단계: 프로젝트 구조 생성 완료 필수
- 2단계: API 설계 완료 권장
