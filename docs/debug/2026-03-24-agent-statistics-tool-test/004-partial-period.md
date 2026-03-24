# 시작일만 / 종료일만 지정 테스트

한쪽 날짜만 지정하여 개방형 기간 조회를 테스트한다.

## Goal #10 — 시작일만 지정 (이후 전체)

- **Goal**: "2025년 6월 이후 Provider별 통계를 보여줘"
- **기대 파라미터**: `groupBy=provider`, `startDate="2025-06-01"`, `endDate=""`
- **검증 포인트**: endDate가 빈 문자열로 전달되어 시작일 이후 전체 데이터 집계

## Goal #11 — 종료일만 지정 (이전 전체)

- **Goal**: "2025년 3월까지의 UpdateType별 집계를 알려줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate=""`, `endDate="2025-03-31"`
- **검증 포인트**: startDate가 빈 문자열로 전달되어 종료일까지 전체 데이터 집계
