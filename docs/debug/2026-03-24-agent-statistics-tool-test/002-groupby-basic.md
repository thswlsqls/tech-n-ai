# groupBy별 기본 테스트 (전체 기간)

groupBy 파라미터의 세 가지 유효값을 전체 기간으로 각각 테스트한다.

## Goal #1 — Provider별 전체 기간 통계

- **Goal**: "전체 기간 동안 Provider별 통계를 보여줘"
- **기대 파라미터**: `groupBy=provider`, `startDate=""`, `endDate=""`
- **검증 포인트**: Provider 5종(OPENAI, ANTHROPIC, GOOGLE, META, XAI)에 대한 집계 결과 반환

## Goal #2 — SourceType별 전체 기간 통계

- **Goal**: "전체 기간 SourceType별 데이터 건수를 집계해줘"
- **기대 파라미터**: `groupBy=source_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: SourceType 3종(GITHUB_RELEASE, RSS, WEB_SCRAPING)에 대한 집계 결과 반환

## Goal #3 — UpdateType별 전체 기간 통계

- **Goal**: "전체 기간 UpdateType별 통계를 알려줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: UpdateType 6종에 대한 집계 결과 반환
