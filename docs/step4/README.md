# Step 4: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)

## Plan Task

```
plan task: Domain 모듈 구현 (데이터베이스 및 저장소 레이어)
```

## 개요

CQRS 패턴 기반 아키텍처에서 Command Side와 Query Side 데이터 접근 계층을 구현합니다.

- **목표**: domain-aurora(Aurora MySQL)와 domain-mongodb(MongoDB Atlas) 모듈 완성
- **배경**: 현재 프로젝트는 CQRS 패턴을 채택하여 Command/Query 분리가 필요함
- **예상 결과**: 두 모듈 모두 독립적으로 빌드 가능하며, 기본 CRUD 동작이 검증됨

## 주요 기술적 도전 과제

- TSID Primary Key 전략 구현 (애플리케이션 레벨 생성)
- Profile 기반 설정 분리 (API Domain / Batch Domain)
- 스키마 간 Foreign Key 제약조건 미지원에 대한 애플리케이션 레벨 처리
- Soft Delete 패턴 구현 및 히스토리 자동 저장 메커니즘
- CQRS 동기화 전략 (Kafka 이벤트 기반)
- MongoDB 인덱스 전략 (ESR 규칙 준수)

## 의존성

- 1단계: 프로젝트 구조 생성
- 2단계: API 설계 완료 권장
- 3단계: Common 모듈 구현 완료 필수

## 다음 단계

- 5단계 (사용자 인증 시스템 구현) 또는 8단계 (Client 모듈 구현)
