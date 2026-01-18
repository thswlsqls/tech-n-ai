# RSS 모듈 분석 문서 보완 프롬프트

## 작업 개요

`docs/step8/rss-scraper-modules-analysis.md` 문서의 `client-rss` 모듈 섹션을 검증하고 개선하여 Spring Boot 애플리케이션에서 RSS Feed 데이터를 활용하는 베스트 프랙티스를 반영한 완성도 높은 분석 문서로 보완합니다.

## 입력 파일

- **대상 문서**: `docs/step8/rss-scraper-modules-analysis.md`
- **참고 파일**: `json/sources.json` (RSS Feed 출처 정보)
- **프로젝트 컨텍스트**: Spring Boot 4.0.1, Java 21, 멀티모듈 MSA 아키텍처

## 작업 요구사항

### 1. Spring Boot RSS Feed 활용 베스트 프랙티스 검증 및 개선

**검증 항목**:
- Spring Boot에서 RSS Feed 파싱 시 권장되는 라이브러리 선택 (Rome vs 다른 대안)
- Spring WebClient를 사용한 비동기 HTTP 요청 패턴이 적절한지 검증
- Spring의 의존성 주입 및 빈 관리 패턴 준수 여부
- Spring Boot의 설정 관리 방식 (application.yml) 활용 여부
- 에러 핸들링 및 재시도 로직이 Spring의 RetryTemplate 또는 Resilience4j 패턴을 따르는지 검증

**개선 방향**:
- Spring Boot 공식 문서 및 Spring 공식 가이드를 참고하여 베스트 프랙티스 반영
- 불필요한 복잡성 제거 및 Spring 생태계 표준 패턴 적용
- Spring Boot의 자동 설정(Auto-Configuration) 활용 가능 여부 검토

### 2. RSS Feed 출처 선별 및 업데이트

**선별 기준**:
- `json/sources.json`에서 `"type": "RSS"`인 출처만 대상
- `"cost": "Free"`인 출처만 선별
- `total_score`가 높은 순서대로 정렬
- 상위 5개 선별 (현재는 4개만 존재하므로 모든 출처 포함)

**선별 결과** (json/sources.json 기준):
현재 RSS Feed 타입 출처는 총 4개이며, 모두 무료 제공:
1. **Google Developers Blog RSS** (total_score: 36, Priority: 1)
2. **TechCrunch RSS** (total_score: 35, Priority: 1)
3. **Ars Technica RSS** (total_score: 34, Priority: 2)
4. **Medium Technology RSS** (total_score: 30, Priority: 2)

**업데이트 작업**:
- 위 4개 출처 정보를 문서의 "대상 출처" 섹션에 반영
- 각 출처의 다음 정보 포함:
  - `rss_feed_url`: RSS 피드 URL
  - `update_frequency`: 업데이트 빈도 (일일/주간)
  - `documentation_url`: 공식 문서 URL
  - `priority`: 우선순위 (1 또는 2)
  - `total_score`: 총점

### 3. RSS Feed 제공자 공식 가이드 문서 참고

**참고 대상 및 확인 사항**:
각 RSS Feed 제공자의 공식 문서를 확인하고 다음 정보를 문서에 반영:

1. **Google Developers Blog RSS** (우선순위 1)
   - 공식 사이트: https://developers.googleblog.com
   - RSS 피드 URL: https://developers.googleblog.com/feeds/posts/default
   - 확인 사항: RSS 피드 형식 (RSS 2.0 또는 Atom), 업데이트 주기, 사용 정책

2. **TechCrunch RSS** (우선순위 1)
   - 공식 사이트: https://techcrunch.com
   - RSS 피드 URL: https://techcrunch.com/feed/
   - 확인 사항: RSS 피드 형식, 사용 가이드라인, Rate Limiting 정책

3. **Ars Technica RSS** (우선순위 2)
   - 공식 사이트: https://arstechnica.com
   - RSS 피드 URL: https://feeds.arstechnica.com/arstechnica/index
   - 확인 사항: RSS 피드 형식, 사용 정책

4. **Medium Technology RSS** (우선순위 2)
   - 공식 사이트: https://medium.com
   - RSS 피드 URL: https://medium.com/feed/tag/technology
   - 공식 문서: https://help.medium.com (RSS 관련 정보)
   - 확인 사항: RSS 피드 형식, 제한사항, 사용 정책

**반영 사항**:
- 각 출처별 RSS 피드 형식 (RSS 2.0, Atom 1.0 등)
- 각 출처별 특별한 제약사항이나 권장사항
- Rate Limiting 정책 (명시된 경우)
- 실제 피드 업데이트 주기 및 예상 빈도
- 피드 구조의 차이점 (필드명, 네임스페이스 등)

### 4. 클린코드 원칙 및 객체지향 설계 기법 검증

**검증 항목**:

#### 클린코드 원칙
- **단일 책임 원칙 (SRP)**: 각 클래스가 하나의 책임만 가지는지 확인
- **의존성 역전 원칙 (DIP)**: 인터페이스 기반 설계인지 확인
- **개방-폐쇄 원칙 (OCP)**: 확장에는 열려있고 수정에는 닫혀있는지 확인
- **명명 규칙**: 클래스, 메서드, 변수명이 명확하고 일관성 있는지 확인

#### 객체지향 설계
- **인터페이스 분리**: 불필요한 의존성을 만들지 않는지 확인
- **전략 패턴**: 다양한 RSS 파서를 전략 패턴으로 구현 가능한지 검토
- **팩토리 패턴**: 파서 생성 로직이 적절히 캡슐화되어 있는지 확인
- **템플릿 메서드 패턴**: 공통 파싱 로직과 특화 로직이 적절히 분리되어 있는지 확인

**개선 방향**:
- 현재 제안된 구조가 위 원칙들을 준수하는지 검증
- 개선이 필요한 부분을 구체적으로 제시
- 코드 예시를 클린코드 원칙에 맞게 수정

### 5. 오버엔지니어링 방지

**주의사항**:
- **불필요한 추상화 금지**: 현재 요구사항에 맞는 수준의 추상화만 유지
- **과도한 디자인 패턴 사용 금지**: 실제 필요에 맞는 패턴만 적용
- **미래 확장성을 위한 과도한 설계 금지**: YAGNI (You Aren't Gonna Need It) 원칙 준수
- **요청하지 않은 기능 추가 금지**: 문서 개선에 집중, 구현 코드 작성은 제외

**검증 기준**:
- 제안된 구조가 현재 요구사항을 충족하는 최소한의 복잡도인지 확인
- 각 클래스와 인터페이스가 실제로 필요한지 검증
- 불필요한 레이어나 래퍼 클래스가 없는지 확인

### 6. 신뢰할 수 있는 공식 출처만 참고

**참고 가능한 출처**:
- Spring Boot 공식 문서: https://spring.io/projects/spring-boot
- Spring Framework 공식 문서: https://spring.io/projects/spring-framework
- Rome 라이브러리 공식 문서: https://rometools.github.io/rome/
- RSS 2.0 표준 스펙: https://www.rssboard.org/rss-specification
- Atom 표준 스펙: https://tools.ietf.org/html/rfc4287

**참고 불가능한 출처**:
- 개인 블로그나 비공식 튜토리얼
- Stack Overflow (공식 문서 우선 참고)
- 비공식 GitHub 저장소 (공식 저장소만 참고)

**참고 출처 정리**:
문서 마지막에 "참고 자료" 섹션을 확장하여 다음 정보를 포함:
- 각 참고 출처의 URL
- 참고한 내용의 요약
- 해당 출처에서 확인한 주요 정보

## 출력 형식

### 문서 구조

개선된 문서는 다음 구조를 유지하되 `client-rss` 모듈 섹션 내용을 보완:

1. **개요** (유지)
2. **client-rss 모듈**
   - 용도 (유지)
   - RSS 피드란? (유지)
   - **대상 출처** (업데이트: 선별된 4개 출처 정보 반영, 총점 순서대로 정렬)
   - **기술 스택** (개선: Spring Boot 베스트 프랙티스 반영)
   - **구현 구조** (개선: 클린코드 원칙 검증 및 개선)
   - **주요 기능** (유지)
   - **활용 예시** (개선: Spring Boot 표준 패턴 반영)
3. **client-scraper 모듈** (유지 - 이 프롬프트 범위 외)
4. **데이터 수집 전략** (유지 - RSS 관련 부분만 검토)
5. **구현 가이드** (개선: Spring Boot 베스트 프랙티스 반영 - client-rss 부분만)
6. **참고 자료** (확장: 모든 참고 출처 정리)

### 개선 사항 표시

- 개선된 내용은 명확히 표시
- 변경 이유를 간단히 설명
- 기존 내용과의 차이점을 명시

## 검증 체크리스트

작업 완료 후 다음 항목을 확인:

- [ ] Spring Boot 공식 문서를 참고하여 베스트 프랙티스 반영 여부 확인
- [ ] RSS Feed 출처 4개 선별 및 정보 업데이트 완료 (총점 순서대로 정렬)
- [ ] 각 RSS Feed 제공자의 공식 문서 확인 및 반영 완료
- [ ] 클린코드 원칙 및 객체지향 설계 기법 검증 완료
- [ ] 오버엔지니어링 요소 제거 완료
- [ ] 모든 참고 출처를 "참고 자료" 섹션에 정리 완료
- [ ] 문서의 일관성 및 가독성 확인

## 제약 조건

1. **코드 작성 금지**: 문서 개선에만 집중, 실제 구현 코드는 작성하지 않음
2. **기존 구조 유지**: 문서의 전체 구조와 섹션 순서는 유지
3. **간결성 유지**: 불필요한 설명이나 중복 내용 제거
4. **실용성 우선**: 이론보다는 실제 구현에 도움이 되는 내용 중심

## 작업 순서

1. **RSS Feed 출처 선별**
   - `json/sources.json`에서 `"type": "RSS"`이고 `"cost": "Free"`인 출처 확인
   - `total_score` 기준으로 정렬 (현재 4개 출처 확인됨)

2. **공식 문서 확인**
   - 각 RSS Feed 제공자의 공식 사이트 및 문서 확인
   - RSS 피드 형식, 사용 정책, 제약사항 파악
   - Spring Boot 공식 문서에서 RSS Feed 처리 베스트 프랙티스 확인
   - Rome 라이브러리 공식 문서 확인

3. **현재 문서 검증**
   - Spring Boot 베스트 프랙티스 준수 여부 검증
   - 클린코드 원칙 및 객체지향 설계 기법 검증
   - 오버엔지니어링 요소 확인

4. **개선 사항 도출**
   - 검증 결과를 바탕으로 개선 사항 도출
   - 구체적인 개선 방안 제시

5. **문서 업데이트**
   - 선별된 RSS Feed 출처 정보 반영
   - Spring Boot 베스트 프랙티스 반영
   - 클린코드 원칙 준수한 설계 검증 결과 반영
   - 공식 문서 기반 정확한 정보 반영

6. **참고 출처 정리**
   - 모든 참고 출처를 "참고 자료" 섹션에 정리
   - 각 출처의 URL, 참고 내용 요약, 주요 정보 포함

## 예상 산출물

- 개선된 `docs/step8/rss-scraper-modules-analysis.md` 문서
- Spring Boot 베스트 프랙티스 반영
- 선별된 RSS Feed 출처 정보 업데이트
- 공식 문서 기반 정확한 정보 반영
- 클린코드 원칙 준수한 설계 검증
- 참고 출처 정리 완료

---

**작성 일시**: 2026-01-07  
**작성자**: System Architect  
**버전**: 1.0
