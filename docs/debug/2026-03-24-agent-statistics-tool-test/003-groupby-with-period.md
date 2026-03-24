# groupBy + 특정 기간 조합 테스트

groupBy와 다양한 기간 지정 방식의 조합을 테스트한다.

## Goal #4 — Provider별 + 절대 기간 (월 단위)

- **Goal**: "2025년 1월부터 3월까지 Provider별 통계를 보여줘"
- **기대 파라미터**: `groupBy=provider`, `startDate="2025-01-01"`, `endDate="2025-03-31"`
- **검증 포인트**: 3개월 범위 내 데이터만 집계되는지 확인

## Goal #5 — SourceType별 + 상반기

- **Goal**: "2025년 상반기 SourceType별 집계 결과를 알려줘"
- **기대 파라미터**: `groupBy=source_type`, `startDate="2025-01-01"`, `endDate="2025-06-30"`
- **검증 포인트**: LLM이 "상반기"를 올바른 날짜로 변환하는지 확인

## Goal #6 — UpdateType별 + 하반기

- **Goal**: "2025년 하반기 UpdateType별 데이터를 분석해줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate="2025-07-01"`, `endDate="2025-12-31"`
- **검증 포인트**: LLM이 "하반기"를 올바른 날짜로 변환하는지 확인

## Goal #7 — Provider별 + 상대 기간 (최근 한 달)

- **Goal**: "최근 한 달간 Provider별 통계를 보여줘"
- **기대 파라미터**: `groupBy=provider`, `startDate="(오늘-1개월)"`, `endDate="(오늘)"`
- **검증 포인트**: LLM이 상대적 시간 표현을 현재 날짜 기준으로 변환하는지 확인

## Goal #8 — UpdateType별 + 분기 지정

- **Goal**: "2026년 1분기 UpdateType별 통계를 집계해줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate="2026-01-01"`, `endDate="2026-03-31"`
- **검증 포인트**: "1분기" 표현의 날짜 변환 정확성

## Goal #9 — SourceType별 + 상대 기간 (지난주)

- **Goal**: "지난주 SourceType별 데이터 건수를 알려줘"
- **기대 파라미터**: `groupBy=source_type`, `startDate="(지난주 월요일)"`, `endDate="(지난주 일요일)"`
- **검증 포인트**: "지난주" 해석의 정확성, 짧은 기간 데이터 처리
