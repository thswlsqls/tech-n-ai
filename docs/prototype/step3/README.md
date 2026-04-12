# Step 3: Common 모듈 구현

## Plan Task

```
plan task: Common 모듈 구현 (공통 기능 기반 구축)
```

## 개요

Common 모듈(common-core, common-exception, common-security, common-kafka)을 구현하여 프로젝트 전반에서 사용할 공통 기능을 제공합니다. 유틸리티 클래스, 예외 처리, 보안 기능, Kafka 이벤트 처리를 포함하며, 모든 모듈이 정상적으로 빌드되고 다른 모듈에서 import 가능해야 합니다.

## 역할

- **공통 모듈 개발자**

## 책임

- Common 모듈 4개 구현 (common-core, common-exception, common-security, common-kafka)
- 공통 기능 제공 및 재사용성 확보
- 빌드 검증 및 통합 테스트

## 주요 작업 내용

1. common-core: 유틸리티 클래스, 상수 클래스, 공통 DTO, 기본 예외 클래스 구현
2. common-exception: 전역 예외 처리, 커스텀 예외 클래스, MongoDB Atlas 예외 로깅 시스템 구현
3. common-security: JWT 토큰 관리, PasswordEncoder 설정, Spring Security 설정 구현
4. common-kafka: Kafka Producer/Consumer 설정, 이벤트 모델 정의 구현

## 의존성

- 1단계: 프로젝트 구조 생성 완료 필수
- 2단계: API 설계 완료 권장

## 다음 단계

- 4단계 (Domain 모듈 구현) 또는 5단계 (사용자 인증 시스템 구현)
