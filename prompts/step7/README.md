# Step 7: Redis 최적화 구현

## 개요

이 단계는 Redis를 활용한 캐싱 및 최적화 전략을 구현합니다.

## 관련 파일

### 프롬프트
- `redis-optimization-best-practices-prompt.md`: Redis 최적화 모범 사례 프롬프트

### 설계서
- `docs/step7/redis-optimization-best-practices.md`: Redis 최적화 모범 사례

## 주요 내용

- Redis 캐싱 전략 구현
- OAuth State 파라미터 저장
- 세션 관리
- 캐시 무효화 전략

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 6단계: OAuth Provider별 로그인 기능 구현 완료 필수 (OAuth State 저장에 Redis 사용 중)
