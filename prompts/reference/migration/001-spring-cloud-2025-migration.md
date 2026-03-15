# Spring Cloud 2025.0.0 + Spring Boot 3.5.0 마이그레이션 프롬프트

## 📋 개요

이 프롬프트는 `shrimp-tm-demo` 프로젝트를 Spring Cloud 2025.0.0 (Northfields)와 Spring Boot 3.5.0 조합으로 마이그레이션하기 위한 작업 지침입니다.

---

## 🎯 목표

- **Spring Boot**: 3.4.1 → **3.5.0**
- **Spring Cloud**: 2024.0.0 (Moorgate) → **2025.0.0** (Northfields)
- 모든 모듈의 의존성 버전 변경 및 빌드 성공

---

## 📚 공식 참고 자료

| 자료 | URL |
|------|-----|
| Spring Cloud 2025.0.0 Release Notes | https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable |
| Spring Boot 3.5.0 Release Notes | https://spring.io/blog/2025/05/22/spring-boot-3-5-0-available-now |
| Spring Cloud Supported Versions | https://github.com/spring-cloud/spring-cloud-release/wiki/Supported-Versions |
| Spring Cloud Compatibility Matrix | https://spring.io/spring-cloud |

---

## 📂 프로젝트 구조 (18개 모듈)

```
shrimp-tm-demo/
├── build.gradle                    # 루트 빌드 설정 (버전 정의)
├── jpa.gradle                      # JPA 공통 설정
├── docs.gradle                     # 문서화 공통 설정
├── settings.gradle                 # 모듈 정의
│
├── api/
│   ├── bookmark/                    # bootJar=true
│   ├── auth/                       # bootJar=true
│   ├── chatbot/                    # bootJar=true
│   ├── contest/                    # jar=true (라이브러리)
│   ├── gateway/                    # bootJar=true, Spring Cloud Gateway 사용
│   └── news/                       # jar=true (라이브러리)
│
├── batch/
│   └── source/                     # bootJar=true
│
├── client/
│   ├── feign/                      # jar=true, Spring Cloud OpenFeign 사용
│   ├── rss/                        # jar=true
│   ├── scraper/                    # jar=true
│   └── slack/                      # jar=true
│
├── common/
│   ├── core/                       # jar=true
│   ├── exception/                  # jar=true
│   ├── kafka/                      # jar=true
│   └── security/                   # jar=true
│
└── domain/
    ├── aurora/                     # jar=true
    └── mongodb/                    # jar=true
```

---

## 🔧 Task 1: 루트 build.gradle 버전 업데이트

### 변경 대상
파일: `/build.gradle`

### 변경 내용

```groovy
// AS-IS
plugins {
    id 'org.springframework.boot' version '3.4.1'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'org.hibernate.orm' version '6.6.4.Final'
}

ext {
    set('springCloudVersion', "2024.0.0")
}

// TO-BE
plugins {
    id 'org.springframework.boot' version '3.5.0'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'org.hibernate.orm' version '6.7.0.Final'  // Spring Boot 3.5.0 호환 버전 확인 필요
}

ext {
    set('springCloudVersion', "2025.0.0")
}
```

### 검증 기준
- `./gradlew dependencies --configuration compileClasspath` 실행 시 버전 충돌 없음

---

## 🔧 Task 2: Spring Cloud Gateway 아티팩트 마이그레이션

### 변경 대상
파일: `/api/gateway/build.gradle`

### 변경 내용

```groovy
// AS-IS (deprecated)
implementation 'org.springframework.cloud:spring-cloud-starter-gateway'

// TO-BE (Spring Cloud 2025.0.0)
// WebFlux 기반 Gateway 사용 시:
implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'

// 또는 WebMVC 기반 Gateway 사용 시:
// implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc'
```

### 주의사항
- 기존 Gateway 설정이 WebFlux 기반인지 확인 후 적절한 아티팩트 선택
- application.yml의 `spring.cloud.gateway.*` prefix 변경 여부 확인

---

## 🔧 Task 3: Spring Cloud OpenFeign 확인

### 변경 대상
파일: `/client/feign/build.gradle`

### 확인 내용

```groovy
// 현재 설정 (BOM 관리됨 - 별도 버전 지정 불필요)
api 'org.springframework.cloud:spring-cloud-starter-openfeign'
```

### 검증 기준
- Spring Cloud 2025.0.0 BOM에서 OpenFeign 버전이 자동 관리됨
- 별도 버전 오버라이드 불필요

---

## 🔧 Task 4: 서드파티 라이브러리 호환성 확인

### 변경 대상 라이브러리 목록

| 라이브러리 | 현재 버전 | 확인 필요 |
|------------|-----------|-----------|
| MyBatis Spring Boot Starter | 3.0.4 | Spring Boot 3.5.0 호환 버전 확인 |
| QueryDSL | 5.1.0 | 호환성 확인 |
| Resilience4j Spring Boot3 | 2.1.0 | 최신 버전 확인 |
| LangChain4j | 0.35.0 | 호환성 확인 |
| JJWT | 0.12.5 | 유지 가능 |
| Rome | 1.19.0 | 유지 가능 |
| Jsoup | 1.17.2 | 유지 가능 |
| TSID Creator | 5.2.6 | 유지 가능 |
| HikariCP | 6.2.1 | Spring Boot 관리 버전 사용 권장 |
| Flyway | (Spring Boot 관리) | 자동 업데이트 |

### 조치 방법
- Spring Boot BOM에서 관리되는 라이브러리는 버전 명시 제거
- 명시적 버전이 필요한 경우만 유지

---

## 🔧 Task 5: jpa.gradle 검토

### 변경 대상
파일: `/jpa.gradle`

### 확인 내용

```groovy
// 현재 설정
implementation 'com.zaxxer:HikariCP:6.2.1'  // Spring Boot 관리 버전 사용 권장
implementation "com.querydsl:querydsl-jpa:5.1.0:jakarta"
annotationProcessor("com.querydsl:querydsl-apt:5.1.0:jakarta")
```

### 권장 변경
- HikariCP 버전 명시 제거 (Spring Boot BOM 관리)
- QueryDSL 5.1.0은 Jakarta EE 호환이므로 유지 가능

---

## 🔧 Task 6: 빌드 및 검증

### 실행 명령어

```bash
# 1. 의존성 캐시 클리어 및 새로고침
./gradlew clean --refresh-dependencies

# 2. 전체 빌드 (테스트 제외)
./gradlew build -x test

# 3. 의존성 트리 확인 (버전 충돌 검사)
./gradlew dependencies --configuration compileClasspath > dependency-report.txt

# 4. 테스트 실행 (선택)
./gradlew test
```

### 성공 기준
- 모든 모듈 빌드 성공 (BUILD SUCCESSFUL)
- 의존성 충돌 없음
- Deprecated 경고는 허용 (기능 영향 없음)

---

## ⚠️ 주의사항

1. **오버엔지니어링 금지**
   - 버전 변경과 직접 관련된 수정만 수행
   - 코드 리팩터링, UI 변경, 비즈니스 로직 수정 금지

2. **최소 변경 원칙**
   - deprecated 경고가 발생해도 기존 기능이 동작하면 유지
   - breaking change가 있는 경우에만 코드 수정

3. **공식 자료 기반**
   - 모든 버전 결정은 공식 호환성 매트릭스 참조
   - 비공식 블로그나 Stack Overflow 참조 금지

4. **순차적 실행**
   - Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6 순서 준수
   - 각 Task 완료 후 빌드 확인 권장

---

## 📝 예상 변경 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `/build.gradle` | 버전 업데이트 |
| `/jpa.gradle` | HikariCP 버전 제거 (선택) |
| `/api/gateway/build.gradle` | Gateway 아티팩트 변경 |
| `/api/gateway/src/main/resources/application.yml` | prefix 변경 (필요시) |

---

## ✅ 완료 체크리스트

- [ ] build.gradle - Spring Boot 버전 3.5.0으로 변경
- [ ] build.gradle - Spring Cloud 버전 2025.0.0으로 변경
- [ ] api/gateway - Gateway 아티팩트 마이그레이션
- [ ] 전체 빌드 성공 확인
- [ ] 의존성 충돌 없음 확인
