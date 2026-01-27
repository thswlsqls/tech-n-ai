# Step 9: Contest 및 News API 모듈 구현

## Plan Task

```
plan task: Contest 및 News API 모듈 구현 (CQRS 패턴 기반 MongoDB 조회 API)
```

## 개요

`api-contest` 및 `api-news` 모듈의 RESTful API 구현. CQRS 패턴 기반: MongoDB Atlas를 사용한 읽기 전용 조회 API 제공. Batch 모듈을 위한 내부 저장 API 제공 (단건/다건 처리).

## 작업 목표

- `api-contest`: Contest 조회 API (목록, 상세, 검색) 및 내부 저장 API 구현 완료
- `api-news`: News 조회 API (목록, 상세, 검색) 및 내부 저장 API 구현 완료
- Controller → Facade → Service → Repository 계층 구조 구현
- 트랜잭션 관리: 단건 처리(@Transactional), 다건 처리(부분 롤백) 구현

## 관련 설계서

- `contest-news-api-design.md`: Contest 및 News API 설계서

## 의존성

- 4단계: Domain 모듈 구현 완료 필수
- 8단계: Client 모듈 구현 완료 권장

## 다음 단계

- 10단계 (외부 API 통합 및 데이터 수집)
