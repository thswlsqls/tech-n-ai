# UpdateType 중심 질의 테스트

UpdateType(EmergingTechType) 관련 질문으로 LLM이 `groupBy=update_type`을 유추하는지 테스트한다.

## Goal #18 — UpdateType 간 비교

- **Goal**: "모델 릴리스와 SDK 릴리스 중 어느 쪽이 더 많아?"
- **기대 파라미터**: `groupBy=update_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: UpdateType enum 값을 한국어로 언급했을 때 올바른 groupBy 유추

## Goal #19 — 특정 UpdateType 건수 조회

- **Goal**: "API 업데이트 관련 데이터 건수를 전체 기간으로 보여줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: "전체 기간"이라는 명시적 기간 지정 처리

## Goal #20 — UpdateType + 하반기 기간

- **Goal**: "2025년 하반기에 블로그 포스트와 플랫폼 업데이트 비율을 알려줘"
- **기대 파라미터**: `groupBy=update_type`, `startDate="2025-07-01"`, `endDate="2025-12-31"`
- **검증 포인트**: 한국어 UpdateType 명칭 + 기간 조합 처리

## Goal #21 — 특정 UpdateType 비중 분석

- **Goal**: "Product Launch 데이터가 전체에서 차지하는 비중은?"
- **기대 파라미터**: `groupBy=update_type`, `startDate=""`, `endDate=""`
- **검증 포인트**: 영문 enum 이름 직접 사용 시 정상 처리
