# Step 4: Domain 모듈 구현

## 개요

이 단계는 데이터베이스 및 저장소 레이어를 구현합니다. Command Side(Aurora MySQL)와 Query Side(MongoDB Atlas) 엔티티 및 Repository를 구현합니다.

## 관련 파일

### 프롬프트
- 메인 프롬프트(`prompts/shrimp-task-prompt.md`)의 4단계 섹션 참고

### 설계서
- `docs/step1/2. mongodb-schema-design.md`: MongoDB 스키마 설계
- `docs/step1/3. aurora-schema-design.md`: Aurora MySQL 스키마 설계

## 주요 내용

- domain-aurora: Aurora MySQL 엔티티 및 Repository 구현
- domain-mongodb: MongoDB Atlas Document 및 Repository 구현
- CQRS 패턴에 맞춘 데이터 모델 분리

## 의존성

- 1단계: 프로젝트 구조 생성
- 2단계: API 설계 완료 권장
- 3단계: Common 모듈 구현 완료 필수
