# 정보 출처 탐색 실행 프롬프트

이 프롬프트는 Tech N AI의 `research` 모드에서 사용하거나, 직접 AI 에이전트에게 전달하여 신뢰할 만한 정보 출처를 체계적으로 탐색하는 데 사용합니다.

---

## 프롬프트 시작

```
당신은 정보 아키텍트 및 데이터 소싱 전문가입니다. 개발자 대회 정보와 최신 IT 테크 뉴스를 제공하는 API Server를 구축하기 위해, 신뢰할 수 있고 검증 가능한 정보 출처를 체계적으로 탐색하고 평가해야 합니다.

## 작업 목표

다음 두 가지 카테고리의 신뢰할 만한 정보 출처를 찾아야 합니다:

### 카테고리 1: 개발자 대회 정보
- 해커톤 (Hackathon)
- 알고리즘 대회 (Algorithm Contests)  
- 오픈소스 기여 대회 (Open Source Contests)
- 기타 개발자 경진대회

### 카테고리 2: 최신 IT 테크 뉴스 정보
- 기술 트렌드 뉴스
- 제품 출시 및 업데이트
- 산업 동향 및 분석
- 개발자 커뮤니티 뉴스

## 탐색 방법론

### 1단계: 출처 유형별 분류

각 카테고리를 다음 유형으로 분류하여 탐색하세요:

**A. 공식 API 제공 서비스**
- 공식 문서가 있는 API
- 인증 메커니즘이 명확한 API
- Rate Limit이 명시된 API

**B. RSS 피드 제공 사이트**
- 정기적으로 업데이트되는 RSS
- 구조화된 메타데이터 포함
- 신뢰할 만한 출처의 RSS

**C. 공식 웹사이트 및 블로그**
- 기술 회사 공식 블로그
- 대회 주최 기관 공식 웹사이트
- 웹 스크래핑 가능한 구조화된 사이트

**D. 커뮤니티 플랫폼**
- Reddit API
- Hacker News API
- Dev.to API
- GitHub API

**E. 뉴스 집계 서비스**
- NewsAPI
- TechCrunch API
- RSS 집계 서비스

### 2단계: 평가 기준 적용

각 출처를 다음 기준으로 평가하세요:

#### 신뢰성 (Reliability) - 10점 만점
- [ ] 공식 인증 여부 (3점)
- [ ] 업데이트 빈도 및 최신성 (3점)
- [ ] 정보 검증 가능성 (2점)
- [ ] 출처의 권위 및 평판 (2점)

#### 접근성 (Accessibility) - 10점 만점
- [ ] API 제공 여부 (4점)
- [ ] RSS 피드 제공 여부 (2점)
- [ ] 웹 스크래핑 가능성 (2점)
- [ ] 공식 문서 및 가이드 존재 (2점)

#### 데이터 품질 (Data Quality) - 10점 만점
- [ ] 구조화된 데이터 형식 (JSON, XML, RSS) (3점)
- [ ] 메타데이터 완성도 (3점)
- [ ] 중복 및 오류 최소화 (2점)
- [ ] 다국어 지원 (2점)

#### 법적/윤리적 고려사항 (Legal/Ethical) - 10점 만점
- [ ] Terms of Service 준수 (3점)
- [ ] Rate Limiting 정책 명확성 (2점)
- [ ] 개인정보 보호 정책 (2점)
- [ ] 저작권 및 라이선스 명확성 (3점)

### 3단계: 우선순위 결정

각 출처를 다음 우선순위로 분류:

**Priority 1 (최우선)**: 
- 공식 API 제공
- 구조화된 데이터 (JSON)
- 정기적 업데이트 (일일 또는 실시간)
- 명확한 문서화
- 무료 또는 합리적인 가격
- 총점 35점 이상

**Priority 2 (고려 대상)**:
- RSS 피드 제공
- 공식 웹사이트 (웹 스크래핑 가능)
- 신뢰할 만한 커뮤니티
- 주간 업데이트
- 총점 25-34점

**Priority 3 (보조 출처)**:
- 웹 스크래핑 필요
- 제한적 접근
- 비공식 소스
- 월간 업데이트
- 총점 15-24점

## 구체적 탐색 대상

### 개발자 대회 정보 출처

**필수 확인 대상:**

1. **Devpost API**
   - URL: https://devpost.com
   - API 문서 확인 필요
   - 해커톤 정보 집계

2. **ChallengePost**
   - URL: https://challengepost.com
   - 대회 정보 제공

3. **Hackathon.io**
   - URL: https://hackathon.io
   - 해커톤 일정 및 정보

4. **Kaggle Competitions API**
   - URL: https://www.kaggle.com/docs/api
   - 데이터 사이언스 대회

5. **LeetCode Contests**
   - URL: https://leetcode.com
   - 알고리즘 대회

6. **Codeforces API**
   - URL: https://codeforces.com/apiHelp
   - 알고리즘 대회 API

7. **AtCoder API**
   - URL: https://atcoder.jp
   - 일본 알고리즘 대회

8. **Google Summer of Code**
   - URL: https://summerofcode.withgoogle.com
   - 오픈소스 기여 프로그램

9. **GitHub Explore**
   - URL: https://github.com/explore
   - 오픈소스 프로젝트 및 이벤트

10. **Major League Hacking (MLH)**
    - URL: https://mlh.io
    - 학생 해커톤 정보

### 최신 IT 테크 뉴스 정보 출처

**필수 확인 대상:**

1. **NewsAPI**
   - URL: https://newsapi.org
   - 기술 카테고리 뉴스 API
   - 무료 티어 제공

2. **TechCrunch API**
   - URL: https://techcrunch.com
   - RSS 피드 확인

3. **Hacker News API**
   - URL: https://github.com/HackerNews/API
   - 공식 API 제공
   - 실시간 업데이트

4. **Reddit API**
   - URL: https://www.reddit.com/dev/api
   - r/programming, r/technology 등
   - 공식 API 제공

5. **Dev.to API**
   - URL: https://dev.to/api
   - 개발자 커뮤니티 뉴스

6. **GitHub Trending API**
   - URL: https://github.com/trending
   - 웹 스크래핑 또는 비공식 API

7. **Product Hunt API**
   - URL: https://www.producthunt.com/v2/docs
   - 신제품 및 스타트업 뉴스

8. **RSS 피드 제공 사이트**
   - Ars Technica RSS
   - The Verge RSS
   - Wired RSS
   - TechRepublic RSS

9. **회사 공식 블로그**
   - Google Developers Blog
   - Microsoft Tech Community
   - AWS News Blog
   - Google Cloud Blog

10. **Medium API**
    - URL: https://medium.com
    - 기술 카테고리 RSS

## 출력 형식

**중요**: 탐색 및 평가가 완료되면, `json` 폴더 아래에 `sources.json` 파일을 생성하고 다음 JSON 형식으로 결과를 저장하세요.

파일 경로: `json/sources.json`

다음 JSON 형식으로 결과를 제공하세요:

```json
{
  "category": "개발자 대회 정보" | "최신 IT 테크 뉴스",
  "sources": [
    {
      "name": "출처 이름",
      "type": "API" | "RSS" | "Web Scraping" | "Newsletter" | "Social Media",
      "url": "출처 URL",
      "api_endpoint": "API 엔드포인트 (해당 시)",
      "rss_feed_url": "RSS 피드 URL (해당 시)",
      "description": "출처 설명 (한국어, 100-200자)",
      "reliability_score": 1-10,
      "accessibility_score": 1-10,
      "data_quality_score": 1-10,
      "legal_ethical_score": 1-10,
      "total_score": 1-40,
      "priority": 1 | 2 | 3,
      "authentication_required": true | false,
      "authentication_method": "API Key" | "OAuth" | "None" | "기타",
      "rate_limit": "Rate limit 정보 (예: 1000 requests/day)",
      "documentation_url": "문서 URL",
      "update_frequency": "실시간" | "일일" | "주간" | "월간",
      "data_format": "JSON" | "RSS" | "XML" | "HTML",
      "pros": [
        "장점 1 (구체적)",
        "장점 2 (구체적)",
        "장점 3 (구체적)"
      ],
      "cons": [
        "단점 1 (구체적)",
        "단점 2 (구체적)"
      ],
      "implementation_difficulty": "Easy" | "Medium" | "Hard",
      "cost": "Free" | "Freemium" | "Paid",
      "cost_details": "비용 상세 정보 (해당 시)",
      "recommended_use_case": "추천 사용 사례 (구체적)",
      "integration_example": "통합 예제 코드 또는 방법 (간단히)",
      "alternative_sources": ["대안 출처 이름 목록"]
    }
  ],
  "summary": {
    "total_sources_found": "발견된 총 출처 수",
    "priority_1_count": "Priority 1 출처 수",
    "priority_2_count": "Priority 2 출처 수",
    "priority_3_count": "Priority 3 출처 수",
    "recommended_primary_sources": [
      {
        "name": "출처 이름",
        "reason": "선정 이유"
      }
    ],
    "recommended_backup_sources": [
      {
        "name": "출처 이름",
        "reason": "백업으로 사용하는 이유"
      }
    ],
    "integration_strategy": "통합 전략 설명 (구체적)",
    "potential_challenges": [
      "도전 과제 1 (구체적)",
      "도전 과제 2 (구체적)"
    ],
    "alternative_approaches": [
      "대안적 접근 방법 1",
      "대안적 접근 방법 2"
    ],
    "next_steps": [
      "다음 단계 1",
      "다음 단계 2"
    ]
  }
}
```

## 실행 지시사항

1. **체계적 탐색**: 위의 분류 체계를 따라 각 카테고리별로 출처를 탐색하세요.

2. **실제 검증**: 가능한 경우 각 출처의 API 문서를 확인하고 실제 접근 가능성을 검증하세요. 웹 검색을 활용하여 최신 정보를 확인하세요.

3. **비교 분석**: 유사한 출처들을 비교하여 장단점을 명확히 분석하세요.

4. **우선순위 결정**: API Server 구축에 가장 적합한 출처를 우선순위별로 정리하세요.

5. **구현 고려사항**: 각 출처를 통합하는 데 필요한 기술적 요구사항을 명시하세요.

6. **최소 요구사항**: 각 카테고리별로 최소 8개 이상의 출처를 찾아 평가하세요.

## 특별 주의사항

- **법적 준수**: 각 출처의 Terms of Service를 반드시 확인하고 준수해야 합니다.
- **Rate Limiting**: API 호출 제한을 고려한 설계가 필요합니다.
- **에러 핸들링**: 외부 API 장애 시 대응 방안을 고려해야 합니다.
- **데이터 정제**: 수집된 데이터의 품질 관리가 중요합니다.
- **확장성**: 향후 추가 출처 통합을 고려한 아키텍처 설계가 필요합니다.

이제 위의 가이드라인에 따라 체계적으로 정보 출처를 탐색하고 평가하세요.
```

