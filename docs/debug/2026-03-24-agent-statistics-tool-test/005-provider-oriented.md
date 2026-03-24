# Provider 중심 질의 테스트

groupBy를 명시하지 않고 Provider 관련 질문으로 LLM이 `groupBy=provider`를 유추하는지 테스트한다.

## Goal #12 — Provider 간 비교 (암시적 groupBy)

- **Goal**: "OpenAI와 Anthropic 중 어디 데이터가 더 많아?"
- **기대 파라미터**: `groupBy=provider`, `startDate=""`, `endDate=""`
- **검증 포인트**: 특정 Provider 이름을 언급했을 때 `groupBy=provider`로 유추하는지 확인

## Goal #13 — Provider 비율 분석

- **Goal**: "Google 관련 데이터가 전체에서 몇 퍼센트인지 알려줘"
- **기대 파라미터**: `groupBy=provider`, `startDate=""`, `endDate=""`
- **검증 포인트**: 비율 계산을 위해 전체 Provider 통계를 조회하는지 확인

## Goal #14 — Provider 비교 + 기간

- **Goal**: "xAI와 Meta의 최근 3개월 데이터 건수를 비교해줘"
- **기대 파라미터**: `groupBy=provider`, `startDate="(오늘-3개월)"`, `endDate="(오늘)"`
- **검증 포인트**: Provider 비교 + 상대 기간 조합 처리
