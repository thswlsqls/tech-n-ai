# Step 1: 프로젝트 구조 및 설계서 생성

## Plan Task

```
plan task: MSA 멀티모듈 프로젝트 구조 검증 및 데이터베이스 설계서 생성
```

## 개요

현재 MSA 멀티모듈 프로젝트 구조를 검증하고, CQRS 패턴에 맞춰 Amazon Aurora MySQL(Command Side)과 MongoDB Atlas(Query Side) 데이터베이스 설계서를 생성합니다. 기존 Gradle 멀티모듈 설정 검증, 모듈 간 의존성 검증, 스키마 설계 및 문서화를 포함합니다.

## 역할

- **프로젝트 아키텍트 및 데이터베이스 설계자**

## 책임

- 현재 멀티모듈 프로젝트 구조 검증
- CQRS 패턴 기반 데이터베이스 스키마 설계
- 설계서 문서화 및 검증

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 1단계 섹션 참고

### 설계서
- `docs/step1/1. multimodule-structure-verification.md`: 멀티모듈 프로젝트 구조 검증
- `docs/step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `docs/step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계
- `docs/step1/4. schema-design-verification-report.md`: 스키마 설계 검증 리포트

## 주요 작업 내용

1. 멀티모듈 프로젝트 구조 검증
   - 현재 Gradle 멀티모듈 프로젝트 구조 검증
   - build.gradle 루트 설정 검증
   - settings.gradle 모듈 등록 검증 (자동 검색 방식)
   - 모듈 간 의존성 검증

2. CQRS 패턴 기반 데이터베이스 스키마 설계
   - Command Side (Aurora MySQL) 스키마 설계
   - Query Side (MongoDB Atlas) 스키마 설계
   - 스키마 설계 문서화

## 의존성

- 없음 (최우선 작업)

## 다음 단계

- 2단계 (API 설계) 또는 3단계 (Domain 모듈 구현)
