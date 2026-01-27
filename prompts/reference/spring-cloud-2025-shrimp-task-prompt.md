# Spring Cloud 2025.0.0 + Spring Boot 3.5.0 마이그레이션
## Shrimp Task Manager용 프롬프트

---

## 🎯 작업 요청

다음 버전 조합으로 프로젝트를 마이그레이션하세요:

- **Spring Boot**: 3.4.1 → **3.5.0**
- **Spring Cloud**: 2024.0.0 → **2025.0.0** (Northfields)

---

## 📋 컨텍스트

### 프로젝트 정보
- 프로젝트: `shrimp-tm-demo`
- 빌드 시스템: Gradle (멀티모듈)
- Gradle 버전: 9.2.1
- Java 버전: 21
- 모듈 수: 18개

### 현재 버전
```groovy
// build.gradle
plugins {
    id 'org.springframework.boot' version '3.4.1'
}
ext {
    set('springCloudVersion', "2024.0.0")
}
```

### 목표 버전
```groovy
// build.gradle
plugins {
    id 'org.springframework.boot' version '3.5.0'
}
ext {
    set('springCloudVersion', "2025.0.0")
}
```

---

## 📚 공식 참고 자료 (신뢰할 수 있는 출처만)

| 자료 | URL |
|------|-----|
| Spring Cloud 2025.0.0 GA 발표 | https://spring.io/blog/2025/05/29/spring-cloud-2025-0-0-is-abvailable |
| Spring Boot 3.5.0 GA 발표 | https://spring.io/blog/2025/05/22/spring-boot-3-5-0-available-now |
| Spring Cloud 호환성 매트릭스 | https://spring.io/spring-cloud |
| Spring Cloud 2025.0 Release Notes | https://github.com/spring-cloud/spring-cloud-release/wiki/Spring-Cloud-2025.0-Release-Notes |

---

## ⚙️ 변경 작업 목록

### 1. 루트 build.gradle 버전 업데이트

**파일**: `/build.gradle`

**변경 내용**:
```groovy
// 변경 전
id 'org.springframework.boot' version '3.4.1'
set('springCloudVersion', "2024.0.0")

// 변경 후
id 'org.springframework.boot' version '3.5.0'
set('springCloudVersion', "2025.0.0")
```

---

### 2. Spring Cloud Gateway 아티팩트 마이그레이션

**파일**: `/api/gateway/build.gradle`

**변경 이유**: Spring Cloud 2025.0.0에서 Gateway 아티팩트가 변경됨

**변경 내용**:
```groovy
// 변경 전 (deprecated)
implementation 'org.springframework.cloud:spring-cloud-starter-gateway'

// 변경 후 (WebFlux 기반)
implementation 'org.springframework.cloud:spring-cloud-starter-gateway-server-webflux'
```

---

### 3. 빌드 검증

**실행 명령**:
```bash
./gradlew clean build -x test
```

**성공 기준**:
- BUILD SUCCESSFUL 출력
- 모든 18개 모듈 빌드 성공

---

## ⚠️ 제약 조건

1. **오버엔지니어링 금지**
   - 버전 변경과 직접 관련된 수정만 수행
   - 코드 리팩터링, 기능 추가, 구조 변경 금지

2. **최소 변경 원칙**
   - deprecated 경고는 허용 (기능 동작하면 유지)
   - breaking change가 있는 경우에만 코드 수정

3. **BOM 관리 라이브러리 버전 제거**
   - Spring Boot/Cloud BOM에서 관리되는 라이브러리의 명시적 버전은 제거 권장

---

## 📝 영향받는 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `build.gradle` | Spring Boot/Cloud 버전 업데이트 |
| `api/gateway/build.gradle` | Gateway 아티팩트 변경 |

---

## ✅ 완료 조건

1. `./gradlew clean build -x test` 성공
2. 모든 모듈 BUILD SUCCESSFUL
3. 의존성 충돌 없음

---

## 🚫 하지 말아야 할 것

- 요청하지 않은 라이브러리 업데이트
- 코드 스타일 변경
- 문서 생성 (README, CHANGELOG 등)
- 테스트 코드 수정
- application.yml 설정 변경 (breaking change가 아닌 경우)
