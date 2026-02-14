당신은 자바 개발자입니다. 다음의 주제로 7000자 이상 분량의 블로그 글을 작성하기 위한 프롬프트를 작성합니다. 

# 주제 : 
제목: 다중 필터 조건에서 최신 문서 누락을 방지하는 차원별 개별 쿼리 전략 — MongoDB $vectorSearch pre-filter와 직접 쿼리의 역할 분리 설계

  핵심 문제: 다중 provider(OPENAI, ANTHROPIC)와 다중 update_type(SDK_RELEASE, MODEL_RELEASE)을 $in 연산자로 단일 쿼리 처리할 때, 특정 provider의 문서가 최근에 집중되면 다른 provider의 최신 문서가 limit에
  의해 제외되는 문제 발생

  다루어야 할 내용:
  - $in + 글로벌 정렬의 문제 시나리오: OPENAI 문서 5건이 모두 최근이면 ANTHROPIC 최신 문서가 limit(5)에서 잘리는 상황 재현
  - 전략 매트릭스 설계: 단일 필터(1쿼리) / 다중 provider(N쿼리) / 다중 updateType(M쿼리) / 교차 쿼리(N×M, 최대 20) / $in fallback(N×M>20) 조건별 분기
  - $vectorSearch pre-filter와 직접 쿼리의 필터 필드 적용 범위 차이: Vector Search Index에 등록된 필드(provider, update_type, published_at)만 pre-filter 가능하고, source_type은 직접 쿼리에서만 필터링 가능한
  제약과 이를 활용한 역할 분리
  - perCombinationLimit = max(2, limit / totalCombinations)으로 각 차원 조합의 최소 결과 수를 보장하면서도 총 결과 수를 제어하는 설계
  - provider + published_at, update_type + published_at 복합 인덱스 설계와 인덱스 기반 정렬 스캔으로 O(log N + limit) 성능을 확보한 과정

 공식 출처:
 - https://docs.spring.io/spring-data/mongodb/reference/mongodb/aggregation-framework.html
 - https://www.mongodb.com/docs/manual/core/aggregation-pipeline/ 


# 프롬프트에서 지시해야 할 필수 항목: 
1. 글의 톤과 문체를 다음의 블로그 글과 일관되도록 프롬프트를 작성합니다. -ㅂ니다. 톤으로 통일하도록 프롬프트를 작성합니다. 
	•	https://ebson.tistory.com/419 
	•	https://ebson.tistory.com/409 

2. 표현이 업계의 전문가들이 읽기에 너무 오만해보이지 않도록 주의를 요청하는 프롬프트를 작성합니다. 
- 기술적인 사실에 대한 문장은, 독자를 가르치는 톤이 아닌, 신뢰할 수 있는 공식 출처를 기반으로 확인된 사실을 정제하고 유용한 정보를 제공하는 톤이어야 합니다. 
- 가능하면 단정적인 표현을 자제하고 개발자로서 성장하기 위해 얻은 인사이트를 개인적으로 정리하는 방향으로 마무리하면 좋습니다. 
3. 기술적인 내용은 반드시 신뢰할 수 있는 공식 기술 문서만 참고해서 정확한 내용을 작성하도록 프롬프트를 작성합니다. 
4. LLM 특유의 장황한 표현, 방어적인 표현, 억지논리, 과장된 논리를 정제하도록 프롬프트를 작성합니다. 
5. 제목과 소제목을 적절히 배치하되 번호는 생략하도록 프롬프트를 작성합니다. 
6. 불필요한 목록 나열 남발식 서술을 지양하고 가능하면 줄글로 작성하면서 문단을 잘 나누도록 프롬프트를 작성합니다.  

적절한 프롬프트 엔지니어링 기법을 사용해 이상의 요청사항을 충족하는 블로그 글 작성을 요청하는 프롬프트를 작성하세요