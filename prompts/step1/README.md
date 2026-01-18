# Step 1: 프로젝트 구조 및 설계서 생성

## 개요

이 단계는 MSA 멀티모듈 프로젝트 구조를 검증하고, CQRS 패턴에 맞춰 Amazon Aurora MySQL(Command Side)과 MongoDB Atlas(Query Side) 데이터베이스 설계서를 생성합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 1단계 섹션 참고

### 설계서
- `docs/step1/1. multimodule-structure-verification.md`: 멀티모듈 프로젝트 구조 검증
- `docs/step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `docs/step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계
- `docs/step1/4. schema-design-verification-report.md`: 스키마 설계 검증 리포트

## 주요 내용

- Gradle 멀티모듈 프로젝트 구조 검증
- CQRS 패턴 기반 데이터베이스 스키마 설계
- 모듈 간 의존성 검증
- 설계서 문서화 및 검증

## 의존성

- 없음 (최우선 작업)
