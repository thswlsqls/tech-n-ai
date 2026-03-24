# 복합 분석 질의 테스트 (다중 호출)

하나의 Goal에서 `get_emerging_tech_statistics`가 여러 번 호출되는 시나리오를 테스트한다.
Idempotency guard(동일 파라미터 중복 호출 차단)가 정상 동작하는지도 함께 검증한다.

## Goal #22 — 세 가지 groupBy 전부 요청

- **Goal**: "Provider별, SourceType별, UpdateType별 전체 통계를 한번에 보여줘"
- **기대 호출 횟수**: 3회
- **기대 파라미터**:
  1. `groupBy=provider`, `startDate=""`, `endDate=""`
  2. `groupBy=source_type`, `startDate=""`, `endDate=""`
  3. `groupBy=update_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: 세 번 연속 호출 시 idempotency guard에 의한 차단 없이 정상 동작 (groupBy가 다르므로 서로 다른 호출)

## Goal #23 — 동일 groupBy + 기간 비교

- **Goal**: "2025년 상반기와 하반기의 Provider별 통계를 비교해줘"
- **기대 호출 횟수**: 2회
- **기대 파라미터**:
  1. `groupBy=provider`, `startDate="2025-01-01"`, `endDate="2025-06-30"`
  2. `groupBy=provider`, `startDate="2025-07-01"`, `endDate="2025-12-31"`
- **검증 포인트**: 같은 groupBy이지만 기간이 다르므로 idempotency guard 미적용

## Goal #24 — 전체 기간 vs 최근 기간 비교

- **Goal**: "전체 기간 Provider별 통계와 최근 3개월 Provider별 통계를 비교해줘"
- **기대 호출 횟수**: 2회
- **기대 파라미터**:
  1. `groupBy=provider`, `startDate=""`, `endDate=""`
  2. `groupBy=provider`, `startDate="(오늘-3개월)"`, `endDate="(오늘)"`
- **검증 포인트**: 전체 기간과 특정 기간 호출이 별도로 처리되는지 확인

## Goal #25 — 분기별 분할 분석

- **Goal**: "UpdateType별 통계를 2025년 분기별로 나눠서 보여줘"
- **기대 호출 횟수**: 4회
- **기대 파라미터**:
  1. `groupBy=update_type`, `startDate="2025-01-01"`, `endDate="2025-03-31"`
  2. `groupBy=update_type`, `startDate="2025-04-01"`, `endDate="2025-06-30"`
  3. `groupBy=update_type`, `startDate="2025-07-01"`, `endDate="2025-09-30"`
  4. `groupBy=update_type`, `startDate="2025-10-01"`, `endDate="2025-12-31"`
- **검증 포인트**: 4회 연속 호출이 loop detection에 걸리지 않는지 확인 (파라미터가 각각 다름)
