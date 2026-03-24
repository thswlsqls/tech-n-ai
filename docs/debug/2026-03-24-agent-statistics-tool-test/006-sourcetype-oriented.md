# SourceType 중심 질의 테스트

SourceType 관련 질문으로 LLM이 `groupBy=source_type`을 유추하는지 테스트한다.

## Goal #15 — 특정 SourceType 건수 조회

- **Goal**: "GitHub Release로 수집된 데이터가 얼마나 되는지 알려줘"
- **기대 파라미터**: `groupBy=source_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: 소스 유형 관련 질문에서 `groupBy=source_type` 유추

## Goal #16 — SourceType 간 비교

- **Goal**: "RSS와 Web Scraping 데이터 비율을 비교해줘"
- **기대 파라미터**: `groupBy=source_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: 두 소스 유형 비교 시 전체 source_type 통계 조회

## Goal #17 — SourceType + 연간 기간

- **Goal**: "2025년에 소스 유형별로 데이터가 어떻게 분포되어 있어?"
- **기대 파라미터**: `groupBy=source_type`, `startDate="2025-01-01"`, `endDate="2025-12-31"`
- **검증 포인트**: "소스 유형별"이라는 한국어 표현에서 source_type 유추 + 연간 기간 변환
