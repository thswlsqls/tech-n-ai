# Scraper 모듈 분석 문서 보완 프롬프트

## 작업 개요

`docs/step8/rss-scraper-modules-analysis.md` 문서의 `client-scraper` 모듈 섹션을 검증하고 개선하여 Spring Boot 애플리케이션에서 웹 스크래핑을 활용하는 베스트 프랙티스를 반영한 완성도 높은 분석 문서로 보완합니다.

## 입력 파일

- **대상 문서**: `docs/step8/rss-scraper-modules-analysis.md` (client-scraper 모듈 섹션)
- **참고 파일**: `json/sources.json` (웹 스크래핑 출처 정보)
- **프로젝트 컨텍스트**: Spring Boot 4.0.1, Java 21, 멀티모듈 MSA 아키텍처

## 작업 요구사항

### 1. Spring Boot 웹 스크래핑 활용 베스트 프랙티스 검증 및 개선

**검증 항목**:
- Spring Boot에서 웹 스크래핑 시 권장되는 라이브러리 선택 (Jsoup vs Selenium vs 다른 대안)
- Spring WebClient를 사용한 HTTP 요청 패턴이 적절한지 검증 (Jsoup과의 통합 방식)
- Spring의 의존성 주입 및 빈 관리 패턴 준수 여부
- Spring Boot의 설정 관리 방식 (application.yml) 활용 여부
- 에러 핸들링 및 재시도 로직이 Spring의 RetryTemplate 또는 Resilience4j 패턴을 따르는지 검증
- 비동기 처리 및 스레드 풀 관리가 Spring의 @Async 또는 WebFlux 패턴을 따르는지 검증

**개선 방향**:
- Spring Boot 공식 문서 및 Spring 공식 가이드를 참고하여 베스트 프랙티스 반영
- 불필요한 복잡성 제거 및 Spring 생태계 표준 패턴 적용
- Spring Boot의 자동 설정(Auto-Configuration) 활용 가능 여부 검토
- Jsoup과 Spring WebClient 통합 패턴 검증

### 2. 웹 스크래핑 출처 선별 및 업데이트

**선별 기준**:
- `json/sources.json`에서 `"type": "Web Scraping"`인 출처만 대상
- `"cost": "Free"`인 출처만 선별
- `total_score`가 높은 순서대로 정렬
- 상위 5개 선별 (동점인 경우 Priority가 높은 것 우선)

**선별 결과** (json/sources.json 기준):
현재 Web Scraping 타입 출처 중 무료 제공 출처 (total_score 순서):
1. **LeetCode Contests** (total_score: 32, Priority: 2, data_format: GraphQL/JSON)
2. **Google Summer of Code** (total_score: 32, Priority: 2, data_format: HTML)
3. **Devpost** (total_score: 30, Priority: 2, data_format: HTML)
4. **Major League Hacking (MLH)** (total_score: 29, Priority: 2, data_format: HTML)
5. **AtCoder** (total_score: 28, Priority: 2, data_format: HTML)
6. **GitHub Trending** (total_score: 28, Priority: 2, data_format: HTML) - 동점이지만 Priority 동일
7. **Hackathon.io** (total_score: 26, Priority: 3, data_format: HTML)

**선별 결과**: 상위 5개 출처
1. LeetCode Contests (32점)
2. Google Summer of Code (32점)
3. Devpost (30점)
4. MLH (29점)
5. AtCoder (28점) - GitHub Trending과 동점이지만 우선 포함

**업데이트 작업**:
- 위 5개 출처 정보를 문서의 "대상 출처" 섹션에 반영
- 각 출처의 다음 정보 포함:
  - `url`: 웹사이트 URL
  - `data_format`: 데이터 형식 (HTML, GraphQL/JSON)
  - `update_frequency`: 업데이트 빈도
  - `documentation_url`: 공식 문서 URL
  - `priority`: 우선순위
  - `total_score`: 총점
  - `rate_limit`: Rate Limiting 정책
  - 특별 주의사항 (robots.txt, ToS 등)

### 3. 웹사이트 공식 가이드 문서 참고

**참고 대상 및 확인 사항**:
각 웹사이트의 공식 문서를 확인하고 다음 정보를 문서에 반영:

1. **LeetCode Contests** (우선순위 1)
   - 공식 사이트: https://leetcode.com
   - 확인 사항: GraphQL 엔드포인트 존재 여부, 사용 정책, Rate Limiting, robots.txt

2. **Google Summer of Code** (우선순위 2)
   - 공식 사이트: https://summerofcode.withgoogle.com
   - 확인 사항: 웹 스크래핑 정책, robots.txt, 사용 가이드라인

3. **Devpost** (우선순위 2)
   - 공식 사이트: https://devpost.com
   - 확인 사항: robots.txt, Terms of Service, 스크래핑 정책

4. **Major League Hacking (MLH)** (우선순위 2)
   - 공식 사이트: https://mlh.io
   - 확인 사항: robots.txt, 사용 정책, 스크래핑 가이드라인

5. **AtCoder** (우선순위 2)
   - 공식 사이트: https://atcoder.jp
   - 확인 사항: robots.txt, 사용 정책, 비공식 API 존재 여부

**반영 사항**:
- 각 웹사이트별 robots.txt 확인 방법 및 결과
- Terms of Service (ToS)에서 스크래핑 관련 조항
- Rate Limiting 정책 (명시된 경우)
- 권장 User-Agent 설정
- 스크래핑 시 주의사항 및 제약사항
- 데이터 형식별 처리 방법 (HTML vs GraphQL)

### 4. 클린코드 원칙 및 객체지향 설계 기법 검증

**검증 항목**:

#### 클린코드 원칙
- **단일 책임 원칙 (SRP)**: 각 클래스가 하나의 책임만 가지는지 확인
  - Scraper: 스크래핑만 담당
  - RobotsTxtChecker: robots.txt 확인만 담당
  - ScrapedDataValidator: 데이터 검증만 담당
  - ScrapedDataCleaner: 데이터 정제만 담당
- **의존성 역전 원칙 (DIP)**: 인터페이스 기반 설계인지 확인
- **개방-폐쇄 원칙 (OCP)**: 확장에는 열려있고 수정에는 닫혀있는지 확인
- **명명 규칙**: 클래스, 메서드, 변수명이 명확하고 일관성 있는지 확인

#### 객체지향 설계
- **인터페이스 분리**: 불필요한 의존성을 만들지 않는지 확인
- **전략 패턴**: 다양한 웹사이트 스크래퍼를 전략 패턴으로 구현 가능한지 검토
- **템플릿 메서드 패턴**: 공통 스크래핑 로직과 특화 로직이 적절히 분리되어 있는지 확인
- **팩토리 패턴**: 스크래퍼 생성 로직이 적절히 캡슐화되어 있는지 확인 (필요한 경우만)

**개선 방향**:
- 현재 제안된 구조가 위 원칙들을 준수하는지 검증
- 개선이 필요한 부분을 구체적으로 제시
- 코드 예시를 클린코드 원칙에 맞게 수정
- Spring Boot의 표준 패턴 (생성자 주입, @ConfigurationProperties 등) 반영

### 5. 오버엔지니어링 방지

**주의사항**:
- **불필요한 추상화 금지**: 현재 요구사항에 맞는 수준의 추상화만 유지
- **과도한 디자인 패턴 사용 금지**: 실제 필요에 맞는 패턴만 적용
  - 전략 패턴: 필요 (다양한 웹사이트 지원)
  - 팩토리 패턴: 불필요할 수 있음 (단순 @Component 주입으로 충분)
  - 템플릿 메서드 패턴: 공통 로직이 많을 때만 고려
- **미래 확장성을 위한 과도한 설계 금지**: YAGNI (You Aren't Gonna Need It) 원칙 준수
- **요청하지 않은 기능 추가 금지**: 문서 개선에 집중, 구현 코드 작성은 제외
- **Selenium 사용 최소화**: 정적 HTML 파싱으로 충분한 경우 Jsoup만 사용

**검증 기준**:
- 제안된 구조가 현재 요구사항을 충족하는 최소한의 복잡도인지 확인
- 각 클래스와 인터페이스가 실제로 필요한지 검증
- 불필요한 레이어나 래퍼 클래스가 없는지 확인
- Selenium이 정말 필요한 경우만 포함 (대부분 Jsoup으로 충분)

### 6. 신뢰할 수 있는 공식 출처만 참고

**참고 가능한 출처**:
- Spring Boot 공식 문서: https://spring.io/projects/spring-boot
- Spring Framework 공식 문서: https://spring.io/projects/spring-framework
- Jsoup 공식 문서: https://jsoup.org/
  - Cookbook: https://jsoup.org/cookbook/
  - API 문서: https://jsoup.org/apidocs/
- Selenium WebDriver 공식 문서: https://www.selenium.dev/documentation/
  - Java API: https://www.selenium.dev/selenium/docs/api/java/
- robots.txt 표준: https://www.robotstxt.org/
- HTTP 표준 (RFC 7231): https://tools.ietf.org/html/rfc7231

**참고 불가능한 출처**:
- 개인 블로그나 비공식 튜토리얼
- Stack Overflow (공식 문서 우선 참고)
- 비공식 GitHub 저장소 (공식 저장소만 참고)

**참고 출처 정리**:
문서 마지막에 "참고 자료" 섹션을 확장하여 다음 정보를 포함:
- 각 참고 출처의 URL
- 참고한 내용의 요약
- 해당 출처에서 확인한 주요 정보
- 웹 스크래핑 관련 특별 고려사항

## 출력 형식

### 문서 구조

개선된 문서는 다음 구조를 유지하되 `client-scraper` 모듈 섹션 내용을 보완:

1. **개요** (유지)
2. **client-rss 모듈** (유지)
3. **client-scraper 모듈**
   - 용도 (유지)
   - **대상 출처** (업데이트: 선별된 5개 출처 정보 반영, 총점 순서대로 정렬)
   - **기술 스택** (개선: Spring Boot 베스트 프랙티스 반영)
   - **구현 구조** (개선: 클린코드 원칙 검증 및 개선)
   - **주요 기능** (유지)
   - **활용 예시** (개선: Spring Boot 표준 패턴 반영)
   - **법적/윤리적 고려사항** (개선: 공식 문서 기반 정확한 정보 반영)
4. **데이터 수집 전략** (유지)
5. **구현 가이드** (개선: Spring Boot 베스트 프랙티스 반영, client-scraper 부분)
6. **참고 자료** (확장: 모든 참고 출처 정리)

### 개선 사항 표시

- 개선된 내용은 명확히 표시
- 변경 이유를 간단히 설명
- 기존 내용과의 차이점을 명시

## 검증 체크리스트

작업 완료 후 다음 항목을 확인:

- [ ] Spring Boot 공식 문서를 참고하여 웹 스크래핑 베스트 프랙티스 반영 여부 확인
- [ ] 웹 스크래핑 출처 5개 선별 및 정보 업데이트 완료 (총점 순서대로 정렬)
- [ ] 각 웹사이트의 공식 문서 확인 및 반영 완료 (robots.txt, ToS 포함)
- [ ] 클린코드 원칙 및 객체지향 설계 기법 검증 완료
- [ ] 오버엔지니어링 요소 제거 완료 (Selenium 사용 최소화, 불필요한 패턴 제거)
- [ ] 모든 참고 출처를 "참고 자료" 섹션에 정리 완료
- [ ] 문서의 일관성 및 가독성 확인

## 제약 조건

1. **코드 작성 금지**: 문서 개선에만 집중, 실제 구현 코드는 작성하지 않음
2. **기존 구조 유지**: 문서의 전체 구조와 섹션 순서는 유지
3. **간결성 유지**: 불필요한 설명이나 중복 내용 제거
4. **실용성 우선**: 이론보다는 실제 구현에 도움이 되는 내용 중심
5. **법적/윤리적 준수 강조**: robots.txt 및 ToS 확인의 중요성 명시

## 작업 순서

1. **웹 스크래핑 출처 선별**
   - `json/sources.json`에서 `"type": "Web Scraping"`이고 `"cost": "Free"`인 출처 확인
   - `total_score` 기준으로 정렬하여 상위 5개 선별

2. **공식 문서 확인**
   - 각 웹사이트의 공식 사이트 및 문서 확인
   - robots.txt 확인 방법 및 결과 파악
   - Terms of Service에서 스크래핑 관련 조항 확인
   - Spring Boot 공식 문서에서 웹 스크래핑 처리 베스트 프랙티스 확인
   - Jsoup 및 Selenium 공식 문서 확인

3. **현재 문서 검증**
   - Spring Boot 베스트 프랙티스 준수 여부 검증
   - 클린코드 원칙 및 객체지향 설계 기법 검증
   - 오버엔지니어링 요소 확인 (특히 Selenium 사용 필요성)

4. **개선 사항 도출**
   - 검증 결과를 바탕으로 개선 사항 도출
   - 구체적인 개선 방안 제시

5. **문서 업데이트**
   - 선별된 웹 스크래핑 출처 정보 반영
   - Spring Boot 베스트 프랙티스 반영
   - 클린코드 원칙 준수한 설계 검증 결과 반영
   - 공식 문서 기반 정확한 정보 반영 (robots.txt, ToS)
   - 법적/윤리적 고려사항 강화

6. **참고 출처 정리**
   - 모든 참고 출처를 "참고 자료" 섹션에 정리
   - 각 출처의 URL, 참고 내용 요약, 주요 정보 포함

## 특별 고려사항

### robots.txt 확인

- 모든 웹 스크래핑 전 robots.txt 확인 필수
- robots.txt 파싱 라이브러리 사용 권장 (예: crawler-commons)
- Disallow 경로는 절대 스크래핑하지 않음
- Crawl-delay 지시사항 준수

### Terms of Service (ToS) 확인

- 각 웹사이트의 ToS를 확인하여 스크래핑 금지 조항 확인
- 스크래핑이 명시적으로 금지된 경우 해당 출처 제외
- 불명확한 경우 보수적으로 접근 (스크래핑 자제)

### Rate Limiting

- 각 웹사이트별 권장 요청 간격 확인
- 기본값: 최소 1초 간격
- 출처별 설정 가능하도록 설계
- Redis를 활용한 분산 환경에서의 Rate Limiting 고려

### User-Agent 설정

- 명확한 프로젝트 식별자 포함
- 연락처 정보 포함 (선택사항, 권장)
- 예시: `ShrimpTM-Demo/1.0 (+https://github.com/your-repo)`

## 예상 산출물

- 개선된 `docs/step8/rss-scraper-modules-analysis.md` 문서 (client-scraper 모듈 섹션)
- Spring Boot 웹 스크래핑 베스트 프랙티스 반영
- 선별된 웹 스크래핑 출처 5개 정보 업데이트
- 공식 문서 기반 정확한 정보 반영 (robots.txt, ToS)
- 클린코드 원칙 준수한 설계 검증
- 참고 출처 정리 완료
- 법적/윤리적 고려사항 강화

---

**작성 일시**: 2026-01-07  
**작성자**: System Architect  
**버전**: 1.0
