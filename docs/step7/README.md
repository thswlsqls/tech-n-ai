# Step 7: Redis 최적화 구현

## Plan Task

```
plan task: Redis 최적화 구현
```

## 개요

멀티모듈 Spring Boot 애플리케이션에서 Redis를 최적화하여 사용하기 위한 설정 및 코드 개선을 구현합니다. 현재 Redis는 OAuth State 저장 및 Kafka 이벤트 멱등성 보장에 사용되고 있으며, 연결 풀 설정 부재, TTL 설정 일관성 문제, 모니터링 설정 부재 등의 개선이 필요합니다.

## 주요 개선 사항

- Lettuce 연결 풀 최적화 설정 적용
- RedisConfig 최적화 (명시적 직렬화 설정 및 트랜잭션 지원 명시)
- EventConsumer의 TTL 설정이 OAuthStateService와 일관되게 `Duration` 객체 직접 사용으로 통일
- Spring Boot Actuator를 통한 Redis 메트릭 수집 활성화
- Redis 보안 설정 강화 (인증, TLS/SSL)

## 관련 설계서

- `redis-optimization-best-practices.md`: Redis 최적화 베스트 프랙티스 분석 결과

## 의존성

- 1단계: 프로젝트 구조 생성
- 3단계: Common 모듈 구현 완료 필수
- 6단계: OAuth Provider별 로그인 기능 구현 완료 필수 - OAuth State 저장에 Redis 사용 중

## 다음 단계

- 8단계 (Client 모듈 구현)
