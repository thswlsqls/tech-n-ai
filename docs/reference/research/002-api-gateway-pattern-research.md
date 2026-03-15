# API Gateway Pattern 자료 수집 및 분석

> 이 문서는 API Gateway Pattern과 API Gateway에 대한 포괄적인 분석을 제공합니다.
> 용어 정의, 등장 배경, 상용 서비스 비교, Java 진영 베스트 프랙티스, 프로젝트 구현 비교까지 5개 섹션으로 구성됩니다.

---

## 1. 용어 정의 — API Gateway Pattern vs API Gateway

### 1.1 API Gateway Pattern (아키텍처 패턴)

#### 정의

API Gateway Pattern은 마이크로서비스 아키텍처에서 **클라이언트가 개별 서비스에 직접 접근하는 문제**를 해결하기 위한 아키텍처 패턴이다.

Chris Richardson은 이 패턴을 다음과 같이 정의한다:

> "An API gateway acts as a single entry point into the application, routing and composing requests to services."
>
> — [Chris Richardson, Microservices.io - API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

Microsoft Azure Architecture Center는 보다 구체적으로 정의한다:

> "An API gateway provides a centralized entry point for managing interactions between clients and application services. It acts as a reverse proxy and routes client requests to the appropriate services. It can also perform various cross-cutting tasks such as authentication, SSL termination, mutual TLS, and rate limiting."
>
> — [Azure Architecture Center - API Gateways](https://learn.microsoft.com/en-us/azure/architecture/microservices/design/gateway)

#### 패턴의 본질 — 핵심 책임(Responsibility)

API Gateway Pattern이 추상적으로 규정하는 책임은 다음과 같다:

| 책임 | 설명 | 출처 |
|------|------|------|
| **단일 진입점 (Single Entry Point)** | 모든 클라이언트 요청이 하나의 엔드포인트로 수렴 | Richardson, Azure |
| **요청 라우팅 (Request Routing)** | 클라이언트 요청을 적절한 백엔드 서비스로 전달 | Richardson, Azure |
| **API Composition** | 여러 서비스의 응답을 집계하여 단일 응답으로 반환 | Richardson |
| **프로토콜 변환** | 외부 클라이언트 친화적 프로토콜(REST/HTTP)과 내부 프로토콜(gRPC 등) 간 변환 | Richardson |
| **횡단 관심사 집중 (Cross-cutting Concerns)** | 인증/인가, Rate Limiting, 로깅, SSL 종료 등을 한 곳에서 처리 | Azure |
| **클라이언트별 API 최적화** | 웹, 모바일, 서드파티 등 클라이언트 유형에 맞는 최적화된 API 제공 | Richardson |

#### 패턴 변형

**BFF (Backend for Frontend)**

BFF는 API Gateway Pattern의 **변형(variation)**이다. 단일 게이트웨이가 모든 클라이언트를 서비스하는 대신, 클라이언트 유형별(웹, 모바일, 외부 API)로 **별도의 API Gateway를 배포**하여 각 클라이언트에 최적화된 API를 제공한다.

- [Chris Richardson, Microservices.io - API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

**Edge Gateway vs Internal Gateway**

- **Edge Gateway**: 외부 트래픽이 내부 네트워크로 진입하는 지점에 배치. 인증, SSL 종료, Rate Limiting 등 보안 중심 책임 수행
- **Internal Gateway**: 내부 서비스 간 통신을 중개. 서비스 디스커버리, 로드밸런싱, 내부 인증 처리. Service Mesh의 사이드카 프록시와 역할이 중첩될 수 있음

#### 패턴 카탈로그에서의 위치 비교

| 출처 | 분류 | 핵심 강조점 |
|------|------|------------|
| **Chris Richardson** (Microservices Patterns) | External API Pattern | 클라이언트-서비스 간 커플링 해소, API Composition, 클라이언트별 최적화 |
| **Microsoft Azure Architecture Center** | Design Pattern (3가지 하위 패턴) | Gateway Routing, Gateway Aggregation, Gateway Offloading으로 세분화 |

Microsoft는 API Gateway를 세 가지 하위 설계 패턴으로 세분화한다:

1. **Gateway Routing** — L7 리버스 프록시로서 단일 엔드포인트에서 여러 서비스로 라우팅
2. **Gateway Aggregation** — 여러 백엔드 호출을 하나의 요청으로 집계
3. **Gateway Offloading** — SSL 종료, mTLS, IP 허용/차단, Rate Limiting, 로깅, 인증, 응답 캐싱, WAF, GZIP 압축, 정적 콘텐츠 서빙 등 횡단 관심사를 게이트웨이로 이전

— [Azure Architecture Center - API Gateways](https://learn.microsoft.com/en-us/azure/architecture/microservices/design/gateway)

### 1.2 API Gateway (구현체/제품)

#### 정의

API Gateway(제품)는 API Gateway Pattern을 구현한 **소프트웨어 제품 또는 관리형 서비스**이다. 패턴이 규정하는 핵심 책임을 구현하면서, 패턴의 범위를 넘어서는 부가 기능을 제공한다.

#### 제품이 패턴을 넘어서 제공하는 기능

| 기능 | 설명 | 예시 제품 |
|------|------|----------|
| API 생명주기 관리 | API 버전 관리, 스테이징, 배포, 폐기 | AWS API Gateway, Azure API Management |
| 개발자 포털 | API 문서, SDK 생성, 사용량 대시보드 | Kong Konnect, Apigee |
| API 키 관리 | API 키 발급/폐기, 사용량 추적, 쿼터 관리 | AWS API Gateway, Kong Enterprise |
| 모니터링 대시보드 | 실시간 트래픽 현황, 에러율, 레이턴시 시각화 | Kong Manager, Azure API Management |
| API 마켓플레이스 | 외부 개발자에게 API를 공개하고 수익화 | Apigee, Kong Konnect |
| 변환 정책 | 요청/응답 본문/헤더 변환, XSLT, JSON 매핑 | Azure API Management |

#### API Management Platform과의 관계

API Gateway는 **API Management Platform의 핵심 컴포넌트**이다. API Management Platform은 API의 전체 생명주기(설계 → 구현 → 배포 → 운영 → 폐기)를 관리하는 포괄적 플랫폼이며, API Gateway는 그 중 **런타임 트래픽 처리** 계층에 해당한다.

```
API Management Platform
├── API Design & Documentation (Swagger/OpenAPI)
├── Developer Portal
├── API Gateway ← 런타임 트래픽 처리
├── Analytics & Monitoring
└── API Lifecycle Management
```

일부 제품(Spring Cloud Gateway, Envoy)은 순수한 API Gateway만 제공하고, 다른 제품(AWS API Gateway, Apigee, Azure API Management)은 API Management 기능을 함께 제공한다.

### 1.3 구분 요약

| 구분 | API Gateway Pattern | API Gateway (제품) |
|------|--------------------|--------------------|
| **성격** | 아키텍처 설계 원칙/패턴 | 소프트웨어 제품 또는 관리형 서비스 |
| **범위** | 단일 진입점, 라우팅, 횡단 관심사의 추상적 책임 정의 | 패턴의 구현 + API 관리, 개발자 포털, 분석 등 부가 기능 |
| **예시** | Chris Richardson의 패턴 정의, Azure의 3가지 하위 패턴 | Spring Cloud Gateway, Kong, AWS API Gateway, Envoy |
| **핵심 관심사** | "어떤 문제를 해결하고, 어떤 책임을 수행해야 하는가?" | "어떻게 구현하고, 어떤 추가 기능을 제공하는가?" |

**실무에서 두 용어가 혼용되는 이유**: 대부분의 개발자가 API Gateway를 논할 때 특정 제품(Spring Cloud Gateway, Kong 등)을 염두에 두고 이야기하기 때문이다. 패턴과 제품의 경계가 명확하지 않은 이유는, 제품들이 패턴이 규정하는 책임을 충실히 구현하면서 추가 기능을 번들링하기 때문이다.

**구분이 중요한 맥락**: 아키텍처 의사결정 시 "API Gateway 패턴을 도입할 것인가?"와 "어떤 API Gateway 제품을 선택할 것인가?"는 별개의 결정이다. 패턴의 필요성을 먼저 평가하고, 패턴 도입이 결정되면 비즈니스 요구사항에 맞는 제품을 선택해야 한다. 패턴이 불필요한 상황에서 특정 제품의 기능에 이끌려 도입하면 오버엔지니어링에 빠진다.

---

## 2. 등장 배경과 히스토리

### 2.1 아키텍처 진화 관점

#### 1단계: 모놀리식 아키텍처

모놀리식 아키텍처에서는 하나의 애플리케이션이 모든 비즈니스 로직을 포함하며, 클라이언트는 **단일 엔드포인트**와 통신했다. 별도의 API Gateway가 불필요했다. 로드밸런서(L4/L7)가 수평 확장된 인스턴스로 트래픽을 분산하는 정도였다.

#### 2단계: SOA (Service-Oriented Architecture)

SOA는 비즈니스 기능을 독립적인 서비스로 분리했으나, **ESB(Enterprise Service Bus)**가 서비스 간 통신의 중앙 허브 역할을 수행했다. ESB는 메시지 변환, 라우팅, 오케스트레이션 등 광범위한 책임을 가졌으며, 이는 다음과 같은 한계로 이어졌다:

- ESB 자체가 **단일 장애점(SPOF)**이 됨
- ESB에 비즈니스 로직이 축적되어 **모놀리식 ESB** 문제 발생
- 벤더 종속, 높은 라이선스 비용, 복잡한 설정

#### 3단계: 마이크로서비스 아키텍처 등장

서비스를 더 작은 단위로 분해하고 각 서비스가 독립적으로 배포/확장되는 마이크로서비스 아키텍처가 등장했다. 그러나 서비스 분해 후 새로운 문제들이 발생했다:

**클라이언트-서비스 간 직접 통신의 문제**:
- **N:M 통신 복잡도**: 클라이언트가 수십 개의 서비스 엔드포인트를 알아야 함
- **프로토콜 불일치**: 내부 서비스가 gRPC, AMQP 등 다양한 프로토콜을 사용하지만 클라이언트는 HTTP/REST만 가능
- **횡단 관심사 분산**: 인증, Rate Limiting, 로깅 등을 각 서비스에서 중복 구현

**API Composition 문제**: 하나의 화면을 구성하기 위해 클라이언트가 여러 서비스를 순차적으로 호출해야 함 (네트워크 라운드트립 증가)

**보안 정책 분산 문제**: 인증/인가 로직이 각 서비스에 분산되어 일관성 유지가 어려움. 내부 서비스가 외부에 직접 노출되어 공격 표면 확대

#### 4단계: API Gateway Pattern 제안

위 문제들의 해결책으로 API Gateway Pattern이 제안되었다. ESB의 실패에서 교훈을 얻어, **얇은 프록시(thin proxy)** 원칙을 강조했다:

- ESB처럼 비즈니스 로직을 포함하지 않음
- 순수한 횡단 관심사(라우팅, 인증, Rate Limiting)만 처리
- 각 서비스의 자율성을 보장하면서 클라이언트 접점만 단순화

#### 5단계: 클라우드 네이티브 시대

**Service Mesh와의 역할 분담**: Istio, Linkerd 등 Service Mesh가 등장하면서, 서비스 간(East-West) 통신의 횡단 관심사는 사이드카 프록시가 처리하고, API Gateway는 외부(North-South) 트래픽에 집중하는 역할 분담이 확립되었다.

```
[클라이언트] → [API Gateway] → [Service Mesh (Sidecar)] → [서비스]
                 North-South        East-West
              (외부→내부 트래픽)   (내부 서비스 간 트래픽)
```

**BFF 패턴의 대두**: 다양한 클라이언트(웹, 모바일, IoT)의 요구사항이 상이해지면서, 단일 API Gateway 대신 클라이언트 유형별 전용 게이트웨이(BFF)를 배치하는 패턴이 확산

### 2.2 주요 마일스톤

| 시기 | 이벤트 | 의미 |
|------|--------|------|
| 2013 | Netflix Zuul 오픈소스 공개 | Netflix의 대규모 마이크로서비스 운영 경험을 바탕으로 최초의 JVM 기반 API Gateway 오픈소스화. Spring Cloud Netflix 생태계의 핵심 컴포넌트가 됨. [출처: [Netflix Zuul GitHub](https://github.com/Netflix/zuul)] |
| 2015 | Kong 오픈소스 출시 | OpenResty(NGINX + Lua) 기반의 고성능 API Gateway. 플러그인 아키텍처로 확장성을 확보하여 언어/프레임워크 독립적인 선택지를 제공. [출처: [Kong GitHub](https://github.com/Kong/kong)] |
| 2015 | AWS API Gateway 출시 | 최초의 주요 클라우드 관리형 API Gateway. 서버리스(Lambda) 아키텍처와의 결합으로 API Gateway의 대중화에 기여. [출처: [AWS API Gateway 공식 문서](https://docs.aws.amazon.com/apigateway/)] |
| 2016-2017 | Envoy Proxy 오픈소스 공개 / Istio 발표 | Lyft에서 개발한 Envoy가 Service Mesh의 데이터 플레인 표준으로 자리잡음. Google/IBM/Lyft가 Istio를 발표하며 Service Mesh + Ingress Gateway 패턴 대두. [출처: [Envoy Proxy](https://www.envoyproxy.io/), [Istio](https://istio.io/)] |
| 2017 | Spring Cloud Gateway 프로젝트 시작 | Spring 5의 WebFlux(Reactor/Netty) 기반으로 Zuul 1.x의 블로킹 한계를 극복한 논블로킹 API Gateway. Spring 생태계의 차세대 표준. [출처: [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway/)] |
| 2020 | Spring Cloud Netflix Zuul 제거 | Spring Cloud 2020.0(Ilford)에서 Zuul 통합 제거. Spring Cloud Gateway가 공식 후속 솔루션으로 확정. Netflix의 자체 Zuul 프로젝트(2.x/3.x)는 별도로 계속 개발 중. [출처: [Spring Cloud 2020.0 Release Notes](https://spring.io/blog/2020/12/22/spring-cloud-2020-0-0-aka-ilford-is-available)] |
| 2023-현재 | GraphQL Federation, gRPC Gateway, Kubernetes Gateway API | Apollo Federation을 통한 GraphQL 기반 API 집계, gRPC-JSON 변환 게이트웨이, K8s Gateway API 표준화 등 API Gateway의 역할과 프로토콜이 다양화. [출처: [K8s Gateway API](https://gateway-api.sigs.k8s.io/)] |

### 2.3 사용해야 하는 이유

#### 도입해야 하는 이유

**아키텍처적 이유**:
- **관심사 분리**: 횡단 관심사(인증, 로깅, Rate Limiting)를 각 서비스에서 제거하여 서비스가 비즈니스 로직에 집중 가능
- **단일 진입점**: 클라이언트는 하나의 엔드포인트만 알면 됨. 서비스 주소, 개수, 배포 토폴로지가 변경되어도 클라이언트는 영향받지 않음
- **백엔드 추상화**: 서비스 분해/병합, 프로토콜 변경 등 내부 아키텍처 변경을 클라이언트로부터 은닉

**운영적 이유**:
- **중앙 집중식 로깅/모니터링**: 모든 요청/응답이 게이트웨이를 통과하므로, 트래픽 패턴, 에러율, 레이턴시를 한 곳에서 관측
- **트래픽 제어**: Rate Limiting, 트래픽 셰이핑, 카나리 배포, A/B 테스트 등을 게이트웨이 레벨에서 수행
- **배포 유연성**: 새 서비스 추가/제거 시 게이트웨이의 라우팅 설정만 변경 (클라이언트 수정 불필요)

**보안적 이유**:
- **인증/인가 집중**: JWT 검증, OAuth2 토큰 유효성 확인 등을 한 곳에서 수행하여 일관된 보안 정책 적용
- **DDoS 방어**: Rate Limiting, 요청 크기 제한, IP 차단 등 1차 방어선 역할
- **내부 서비스 은닉**: 백엔드 서비스가 외부 네트워크에 직접 노출되지 않아 공격 표면 축소

**개발 생산성**:
- **횡단 관심사 중복 제거**: 각 서비스에서 인증/로깅/CORS 등을 반복 구현할 필요 없음
- **API 버전 관리**: 경로 기반 버전 라우팅(`/v1/...`, `/v2/...`)을 게이트웨이에서 처리
- **클라이언트 간소화**: 단일 엔드포인트, 일관된 에러 형식, 표준화된 인증 흐름

#### 도입하지 않아야 하는 경우

**오버엔지니어링 위험**:
- 서비스가 2~3개 이하인 소규모 시스템에서는 게이트웨이 운영 오버헤드가 이점을 초과할 수 있음
- 단일 서비스(모놀리스)에서는 로드밸런서만으로 충분

**단일 장애점(SPOF) 리스크**:
- 게이트웨이가 다운되면 모든 서비스 접근이 차단됨
- 이를 완화하려면 다중 인스턴스 배포, 헬스체크, 자동 복구가 필수 → 운영 복잡도 증가

**레이턴시 추가**:
- 모든 요청에 네트워크 홉 하나가 추가됨
- Chris Richardson은 "typically insignificant"라고 언급하나, 극도로 낮은 레이턴시가 요구되는 시스템(HFT 등)에서는 고려 대상
- [Chris Richardson, Microservices.io - API Gateway Pattern](https://microservices.io/patterns/apigateway.html)

**조직 병목**:
- 모든 API 변경이 게이트웨이 라우팅 설정 변경을 수반하면, 게이트웨이 관리 팀이 병목이 될 수 있음
- 셀프서비스 라우팅 설정(K8s Ingress, 선언적 설정)으로 완화 가능

---

## 3. 상용 서비스 비교 분석

### 3.1 분석 대상

Java 진영에서 실제로 많이 사용되는 API Gateway 솔루션 5개를 비교한다:

1. **Spring Cloud Gateway** — Java/Spring 네이티브
2. **Netflix Zuul** — Spring Cloud 초기의 사실상 표준
3. **Kong** — OpenResty 기반, 인프라 레벨에서 널리 채택
4. **AWS API Gateway** — 서버리스/클라우드 네이티브 환경
5. **Envoy Proxy (+ Istio)** — K8s 환경 Service Mesh

### 3.2 비교표

#### 아키텍처 및 핵심 기능

| 기준 | Spring Cloud Gateway | Netflix Zuul | Kong | AWS API Gateway | Envoy / Istio |
|------|---------------------|-------------|------|----------------|--------------|
| **아키텍처** | 비동기, 논블로킹 (Reactor Netty) | Zuul 1.x: 동기/블로킹 (Servlet). Zuul 2.x+: 비동기/논블로킹 (Netty) | 비동기 (NGINX 이벤트 루프 + Lua 코루틴) | 관리형 서비스 (내부 구현 비공개) | 비동기/논블로킹 (C++ 이벤트 루프) |
| **라우팅** | Predicate 기반 (Path, Host, Method, Header 등) | Filter 기반 라우팅 | Declarative Routes + Services + Upstream | REST API/HTTP API 리소스 기반 | Kubernetes Gateway API / VirtualService |
| **인증/인가** | 커스텀 GatewayFilter, Spring Security WebFlux 통합 | 커스텀 필터 | 플러그인 (JWT, OAuth2, OIDC, Basic Auth, HMAC) | IAM 권한, Lambda Authorizer, Cognito, JWT (HTTP API) | Istio AuthorizationPolicy, JWT/OIDC |
| **Rate Limiting** | `RequestRateLimiter` + Redis (Token Bucket) | 커스텀 필터 | 플러그인 (Rate Limiting, Advanced Rate Limiting) | 스로틀링 내장 (계정/API 키 기준) | Envoy Rate Limit Service (외부 서비스) |
| **Circuit Breaker** | `CircuitBreaker` 필터 + Resilience4j | Hystrix (레거시) | 플러그인 없음, Upstream health check로 대체 | 없음 (백엔드 Lambda에 위임) | Outlier Detection (자체 구현) |
| **로드밸런싱** | Spring Cloud LoadBalancer (Eureka/Consul 연동) | Ribbon (레거시) | Upstream 레벨 로드밸런싱 (Ring-balancer, Hash-based) | 내장 (API 레벨) | 고급 로드밸런싱 (WRR, Ring Hash, Maglev 등) |

#### 확장성, 프로토콜, 관측성

| 기준 | Spring Cloud Gateway | Netflix Zuul | Kong | AWS API Gateway | Envoy / Istio |
|------|---------------------|-------------|------|----------------|--------------|
| **확장 모델** | `GatewayFilter` / `GlobalFilter` (Java) | `ZuulFilter` (Java/Groovy) | 플러그인 (Lua, Go, JS, Python, WASM) | Lambda Authorizer, VTL 매핑 | HTTP Filter (C++, Lua, WASM) |
| **Java 통합** | 네이티브 (Spring Bean으로 확장) | 네이티브 (Java 필터) | 외부 인프라 (Java 코드와 분리) | Lambda 함수로 통합 | 외부 인프라 (K8s 리소스로 설정) |
| **프로토콜** | HTTP/1.1, HTTP/2, WebSocket, SSE. gRPC (JsonToGrpc 필터) | HTTP/1.1, HTTP/2, WebSocket | HTTP/1.1, HTTP/2, gRPC, WebSocket, GraphQL, TCP/UDP, Kafka | HTTP/1.1, HTTP/2, WebSocket, REST, HTTP API | HTTP/1.1, HTTP/2, HTTP/3, gRPC, TCP, UDP, MongoDB, Redis 등 |
| **로깅** | 커스텀 GlobalFilter로 Access Log 구현 | 커스텀 필터 | 플러그인 (File Log, TCP Log, HTTP Log 등) | CloudWatch Logs 자동 연동 | Access Log 내장 (stdout, gRPC, file) |
| **메트릭** | Micrometer + Prometheus/Grafana | Atlas (Netflix), 커스텀 | Prometheus 플러그인, Datadog, StatsD | CloudWatch Metrics 자동 연동 | Prometheus 네이티브, StatsD |
| **분산 추적** | Micrometer Tracing + OpenTelemetry | 커스텀 | OpenTelemetry 플러그인 (Enterprise) | X-Ray 통합 | OpenTelemetry, Zipkin, Jaeger 네이티브 |

#### 배포, 생태계, 비용

| 기준 | Spring Cloud Gateway | Netflix Zuul | Kong | AWS API Gateway | Envoy / Istio |
|------|---------------------|-------------|------|----------------|--------------|
| **배포 모델** | 셀프 호스팅 (JVM 프로세스), K8s 배포 가능 | 셀프 호스팅 (JVM 프로세스) | 셀프 호스팅, Docker, K8s (Kong Ingress Controller), Konnect(SaaS) | 완전 관리형 (SaaS) | 셀프 호스팅, K8s 네이티브 |
| **K8s 네이티브** | CRD 없음, K8s Gateway API 미지원 | CRD 없음 | Kong Ingress Controller, K8s Gateway API 지원 | 해당 없음 (AWS 관리형) | K8s Gateway API 공식 구현체, CRD 기반 |
| **커뮤니티** | Spring 생태계 (대규모), 적극적 유지보수 | Netflix OSS (성숙), Spring Cloud 통합은 제거됨 | GitHub ★40K+, 400+ 플러그인 | AWS 생태계 (대규모) | CNCF Graduated, GitHub ★26K+ |
| **문서 품질** | 우수 (Spring 공식 레퍼런스) | 보통 (Netflix Wiki) | 우수 (공식 문서 + 튜토리얼) | 우수 (AWS 공식 문서) | 우수 (Envoy 공식 + Istio 공식) |
| **비용** | 무료 (OSS, Apache 2.0) | 무료 (OSS, Apache 2.0) | OSS 무료. Enterprise/Konnect 유료 (구독) | 종량제 (API 호출 수 + 데이터 전송량). HTTP API가 REST API 대비 최소 71% 저렴 | 무료 (OSS, Apache 2.0). Istio Enterprise 지원은 벤더별 유료 |
| **학습 곡선** | 낮음 (Spring 개발자에게). WebFlux 이해 필요 | 낮음 (Java 개발자에게). 레거시 API 주의 | 중간 (Lua 플러그인 커스터마이징 시 높음) | 낮음 (AWS 콘솔/CDK). 세밀한 제어 시 복잡 | 높음 (Envoy 설정 복잡, Istio 운영 오버헤드) |

출처:
- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/reference/index.html)
- [Netflix Zuul GitHub](https://github.com/Netflix/zuul)
- [Kong Gateway Product Page](https://konghq.com/products/kong-gateway)
- [AWS API Gateway - REST vs HTTP API](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html)
- [Envoy Gateway Documentation](https://gateway.envoyproxy.io/docs/)
- [Istio Traffic Management](https://istio.io/latest/docs/concepts/traffic-management/)

#### 성능 참고

Kong은 공식적으로 노드당 최대 **50,000 TPS**를 표기한다.

— [Kong Gateway Product Page](https://konghq.com/products/kong-gateway)

Spring Cloud Gateway, Envoy 등의 공식 벤치마크는 공개되어 있지 않다. 비공식 벤치마크는 테스트 환경에 따라 결과가 크게 달라지므로 이 문서에서는 인용하지 않는다. 일반적으로 C++ 기반 Envoy > NGINX/Lua 기반 Kong > JVM 기반 SCG/Zuul 순으로 raw throughput이 높으나, Java 프로젝트에서는 개발 생산성과 생태계 통합이 raw 성능보다 중요한 선택 기준인 경우가 많다.

### 3.3 선택 가이드

| 시나리오 | 권장 제품 | 근거 |
|---------|----------|------|
| **Java/Spring 기반 엔터프라이즈 (중소 규모)** | **Spring Cloud Gateway** | Spring Boot와 네이티브 통합. Java 코드로 필터/라우팅을 확장할 수 있어 Spring 개발팀의 학습 곡선이 최소. 별도 인프라 운영 불필요 (같은 JVM 프로세스). Resilience4j, Micrometer 등 Spring 생태계와 자연스럽게 결합 |
| **Java/Spring 기반 엔터프라이즈 (대규모, K8s)** | **Spring Cloud Gateway** + **Kong** 또는 **Envoy/Istio** | Edge에 Kong/Envoy를 배치하여 L7 로드밸런싱, DDoS 방어, TLS 종료를 수행하고, 내부에 SCG를 배치하여 비즈니스 로직 밀접한 라우팅/인증 처리. 규모가 크면 Service Mesh(Istio)의 사이드카가 서비스 간 통신을 담당 |
| **AWS 기반 서버리스 + Java Lambda** | **AWS API Gateway (HTTP API)** | Lambda와의 네이티브 통합. 관리형 서비스로 인프라 운영 부담 제로. HTTP API가 REST API 대비 71%+ 저렴하고 지연시간도 낮음. JWT Authorizer 내장. 단, 세밀한 커스터마이징이 필요하면 REST API 유형이나 SCG 고려 |
| **폴리글랏 마이크로서비스 (K8s Service Mesh)** | **Envoy/Istio** | 언어 독립적. K8s Gateway API 공식 구현체. 사이드카 프록시로 서비스 간 mTLS, Rate Limiting, 분산 추적을 애플리케이션 코드 변경 없이 적용. 단, 운영 복잡도가 높아 전담 인프라팀 필요 |
| **Spring 생태계 중심 빠른 MVP** | **Spring Cloud Gateway** | `spring-cloud-starter-gateway-server-webflux` 의존성 하나로 시작. YAML 선언적 라우팅으로 코드 최소화. Spring Boot Actuator로 즉시 모니터링 가능. 프로토타입에서 프로덕션까지 같은 기술 스택 유지 |

**Netflix Zuul에 대한 참고**: Netflix의 자체 Zuul 프로젝트(3.x)는 여전히 활발히 개발되고 있으나(최신 릴리스 v3.4.7, 2025년 3월), **Spring Cloud Netflix Zuul 통합은 2020년에 제거**되었다. 신규 Java/Spring 프로젝트에서 Zuul을 선택하면 Spring Cloud 생태계의 이점(자동 설정, Actuator 통합 등)을 활용할 수 없으므로, Spring Cloud Gateway가 사실상의 후속 표준이다.

— [Netflix Zuul GitHub Releases](https://github.com/Netflix/zuul/releases), [Spring Cloud 2020.0 Release](https://spring.io/blog/2020/12/22/spring-cloud-2020-0-0-aka-ilford-is-available)

---

## 4. Java 진영 베스트 프랙티스

### 4.1 Spring Cloud Gateway 중심 베스트 프랙티스

#### 라우팅 (Routing)

**선언적(YAML) vs 프로그래밍 방식**:

| 기준 | YAML 선언적 | Java Fluent API (`RouteLocatorBuilder`) |
|------|------------|--------------------------------------|
| 적합한 경우 | 단순한 Path/Host 기반 라우팅, 운영 중 설정 변경이 잦은 경우 (ConfigMap 교체) | 동적 조건 분기, 커스텀 Predicate 로직, 복잡한 필터 체인 |
| 장점 | 코드 변경/재배포 불필요, 가독성 높음 | 타입 세이프, IDE 자동완성, 테스트 용이 |
| 단점 | 복잡한 조건 표현 한계 | 설정 변경 시 재배포 필요 |

**권장**: 대부분의 프로젝트에서 **YAML 선언적 방식**이 적합하다. 라우팅 규칙의 80% 이상은 단순한 Path Predicate로 충분하며, 복잡한 케이스만 Java 코드로 보완한다.

— [Spring Cloud Gateway - Configuring Route Predicate Factories and Filter Factories](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/configuring-route-predicate-factories-and-filter-factories.html)

**Service Discovery vs 정적 URI**:

| 기준 | Service Discovery (Eureka/Consul) | 정적 URI |
|------|----------------------------------|---------|
| 적합한 경우 | 서비스 인스턴스가 동적으로 변화, 비-K8s 환경 | K8s 환경 (K8s Service DNS가 LB 제공), 서비스 수가 적고 고정적 |
| 판단 기준 | **K8s를 사용하는가?** K8s라면 정적 URI(`http://service-name:port`)로 충분. K8s Service가 L4 로드밸런싱을 담당 |

**라우트별 필터 vs default-filter**:
- **default-filter**: 모든 라우트에 공통 적용되는 횡단 관심사 (DedupeResponseHeader, Retry, RequestSize 등)
- **라우트별 필터**: 특정 라우트에만 적용되는 정책 (RequestRateLimiter의 KeyResolver가 라우트마다 다른 경우 등)

— [Spring Cloud Gateway - Default Filters](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/default-filters.html)

#### 인증/인가 (Authentication & Authorization)

**JWT 검증 위치 — 심층 방어(Defense in Depth)**:

| 계층 | 역할 | 예시 |
|------|------|------|
| **Gateway** | 토큰 형식 검증, 서명 검증, 만료 확인, 사용자 정보 추출 후 헤더 주입 | `JwtAuthenticationGatewayFilter`에서 검증 후 `x-user-id` 주입 |
| **개별 서비스** | 비즈니스 레벨 권한 확인 (이 사용자가 이 리소스에 접근 가능한가?) | `@PreAuthorize("hasRole('ADMIN')")` |

**권장**: Gateway에서 **인증(Authentication)**을 처리하고, 개별 서비스에서 **인가(Authorization)**를 처리하는 것이 베스트 프랙티스이다. Gateway는 "누구인가?"를 판별하고, 서비스는 "이 사용자가 이 자원에 접근 가능한가?"를 판별한다. 단, 관리자 경로 같은 **조대 입도(coarse-grained) 접근 제어**는 Gateway에서도 수행할 수 있다.

**Spring Security WebFlux 통합 vs 커스텀 GatewayFilter**:

| 접근법 | 장점 | 단점 |
|--------|------|------|
| Spring Security WebFlux | OAuth2 Resource Server 표준 지원, SecurityFilterChain 통합, 선언적 경로 보안 | Gateway에 Spring Security 의존성 추가, 설정 복잡도 증가 |
| 커스텀 GatewayFilter | 경량, 불필요한 의존성 없음, 필터 순서를 명시적으로 제어 | OAuth2 표준 흐름 직접 구현 필요, 보안 모범사례 누락 위험 |

**판단 기준**: OAuth2/OIDC Provider(Keycloak, Auth0 등)와 통합하는 경우 Spring Security WebFlux가 표준적. 자체 JWT만 검증하는 경우 커스텀 GatewayFilter가 경량 대안으로 적합.

**경로별 접근 제어 설정 외부화**:

공개/보호/관리자 경로를 코드에 하드코딩하지 않고 `@ConfigurationProperties`로 외부화하면:
- YAML/ConfigMap 수정만으로 경로 정책 변경 가능 (코드 재배포 불필요)
- Spring의 `PathPattern`을 사용하여 WebFlux에 최적화된 패턴 매칭

```java
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(
    List<String> publicPaths,
    List<String> publicPathExclusions,
    List<String> adminOnlyPaths
) {}
```

#### 회복탄력성 (Resilience)

**Circuit Breaker**:

Spring Cloud Gateway는 `CircuitBreaker` GatewayFilter Factory를 네이티브로 제공하며, Resilience4j를 구현체로 사용한다.

```yaml
filters:
  - name: CircuitBreaker
    args:
      name: myCircuitBreaker
      fallbackUri: forward:/fallback
```

**권장 설정 원칙**:
- 라우트별 별도 CircuitBreaker 인스턴스 (서비스 간 장애 격리)
- Fallback 엔드포인트에서 표준 에러 응답 반환 (503 Service Temporarily Unavailable)
- `slidingWindowType: COUNT_BASED`, `failureRateThreshold: 50`, `waitDurationInOpenState: 10s`가 일반적 시작점

— [Spring Cloud Gateway - CircuitBreaker Filter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/circuitbreaker-filter-factory.html)

**Rate Limiting**:

| 방식 | 장점 | 단점 | 적합한 경우 |
|------|------|------|-----------|
| SCG `RequestRateLimiter` + Redis | 분산 환경에서 일관된 카운트, Token Bucket 알고리즘 | Redis 의존성 추가 | 다중 인스턴스 운영 (권장) |
| Bucket4j (인메모리) | 외부 의존성 없음 | 인스턴스별 독립 카운트 | 단일 인스턴스, POC |

**Retry 전략**:
- **멱등한 메서드만 재시도**: GET은 안전하게 재시도 가능. POST/PUT/DELETE는 중복 처리 위험
- **특정 상태 코드만 재시도**: 503(Service Unavailable)은 일시적 장애. 400/401/403은 재시도 무의미
- **지수 백오프**: 재시도 간격을 점점 늘려 Thundering Herd 방지

```yaml
- name: Retry
  args:
    retries: 2
    methods: GET
    statuses: SERVICE_UNAVAILABLE
    backoff:
      firstBackoff: 100ms
      maxBackoff: 500ms
      factor: 2
```

— [Spring Cloud Gateway - Retry Filter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/retry-filter-factory.html)

**Timeout 계층**:

| Timeout | 역할 | 권장 값 |
|---------|------|--------|
| `server.netty.connection-timeout` | 클라이언트 → 게이트웨이 TCP 연결 | 3~5초 |
| `httpclient.connection-timeout` | 게이트웨이 → 백엔드 TCP 연결 | 5~30초 |
| `httpclient.response-timeout` | 게이트웨이 → 백엔드 응답 대기 | 30~60초 (백엔드 처리 시간에 따라) |
| `pool.acquire-timeout` | 커넥션 풀에서 연결 획득 대기 | 30~45초 |

**원칙**: 클라이언트 facing timeout < 게이트웨이 → 백엔드 timeout. 클라이언트가 먼저 타임아웃되면 게이트웨이에 행 커넥션(orphan connection)이 남는다.

#### 관측성 (Observability)

**구조화된 Access Log**:
- 전용 Logger 사용 (`LoggerFactory.getLogger("ACCESS_LOG")`) — 비즈니스 로그와 분리
- `@Order(LOWEST_PRECEDENCE)` GlobalFilter — 응답 완료 후 정확한 처리 시간 측정
- 기록 항목: method, path, status, duration, requestId, clientIp, userId, routeId, userAgent
- 민감 정보(Authorization 헤더, 토큰 값) 마스킹
- prod 환경에서도 Access Log는 항상 INFO 이상으로 출력

**메트릭 파이프라인**:
```
SCG 내장 메트릭 → Micrometer → Prometheus → Grafana
```
- `spring.cloud.gateway.metrics.enabled: true`로 `spring.cloud.gateway.requests` 타이머 활성화
- 라우트별 요청 수, 응답 시간 분포, 에러율 자동 수집
- 커스텀 메트릭: 인증 실패 횟수, Rate Limit 도달 횟수 등

**분산 추적**:
- Micrometer Tracing + OpenTelemetry로 자동 Trace ID 생성/전파
- `X-Request-Id` 헤더를 별도 GlobalFilter로 관리하여, Trace ID와 독립적인 비즈니스 요청 추적
- 클라이언트가 제공한 `X-Request-Id`는 UUID 형식 검증 후 전파 (임의 문자열 주입 방지)

#### 보안 강화 (Security Hardening)

**헤더 스푸핑 방지**:
- 게이트웨이가 주입하는 내부 헤더(`x-user-id`, `x-user-email`, `x-user-role`)를 외부 클라이언트가 위조할 수 없도록, **JWT 필터 실행 전에 해당 헤더를 무조건 제거**해야 한다
- 구현 방식: `RemoveRequestHeader` default-filter 또는 커스텀 `GlobalFilter`(@Order HIGHEST_PRECEDENCE)

**요청 크기 제한**:
- SCG 네이티브 `RequestSize` 필터로 최대 본문 크기 제한 (기본 5MB)
- 초과 시 413 Payload Too Large 자동 반환
- 파일 업로드 등 특정 라우트는 개별 설정으로 제한 완화

**CORS 전략**:
- 환경별 Origin 관리: local은 `localhost:*`, production은 특정 도메인만 허용
- `DedupeResponseHeader=Access-Control-Allow-Origin, RETAIN_LAST`로 중복 CORS 헤더 방지 (백엔드에서도 CORS 헤더를 추가하는 경우)

**내부 서비스 보안**:
- K8s NetworkPolicy: 백엔드 서비스가 게이트웨이 Pod에서만 ingress를 받도록 제한 (가장 효과적)
- Gateway Secret 헤더: 게이트웨이가 `X-Gateway-Secret` 헤더를 주입하고 백엔드가 검증 (NetworkPolicy 보완)
- mTLS: 운영 복잡도가 높아 현 단계에서는 NetworkPolicy + Gateway Secret 조합 권장

#### 성능 최적화 (Performance)

**커넥션 풀 설정**:

| 설정 | 권장 값 | 근거 |
|------|--------|------|
| `max-connections` | 200~500 (서비스 수 × 예상 동시 연결) | 과도한 값은 메모리 낭비, 과소한 값은 연결 대기 |
| `max-idle-time` | 20~30초 | 백엔드 keep-alive보다 짧게 설정하여 이미 닫힌 연결에 요청 전송 방지 |
| `max-life-time` | 3~5분 | DNS 변경 반영을 위해 주기적으로 연결 재생성 (K8s 환경에서 중요) |

**블로킹 코드 방지**:
- WebFlux/Netty 환경에서 이벤트 루프 스레드를 블로킹하면 전체 게이트웨이 성능이 급격히 저하
- 테스트 시 [BlockHound](https://github.com/reactor/BlockHound)를 활용하여 블로킹 호출 검출
- 블로킹이 불가피한 경우 `Schedulers.boundedElastic()`으로 별도 스레드 풀에서 실행

**GC 권장**:
- Java 21 이상에서 **ZGC (Generational)** 권장: `-XX:+UseZGC -XX:+ZGenerational`
- 낮은 GC 일시정지 시간으로 게이트웨이의 테일 레이턴시(p99) 개선

### 4.2 비즈니스 요구사항별 아키텍처 선택 가이드

| 요구사항 | 권장 접근법 | 비권장 접근법 | 근거 |
|----------|------------|-------------|------|
| **단일 SPA + REST API** | SCG 단일 게이트웨이. YAML 라우팅, JWT 검증 | Kong/Envoy 같은 별도 인프라. BFF 패턴 | 서비스 수가 적고 클라이언트가 하나이므로 단순 구조가 최적. 별도 인프라는 운영 오버헤드만 증가 |
| **다중 클라이언트 (Web + Mobile + 3rd Party)** | BFF 패턴 — 클라이언트 유형별 SCG 인스턴스 또는 별도 라우트 그룹 | 단일 게이트웨이에서 모든 클라이언트 대응 | 모바일은 대역폭 최적화, 3rd Party는 API 키 관리 등 요구사항이 상이하므로 분리가 유리 |
| **실시간 양방향 통신 (WebSocket/SSE)** | SCG WebSocket 라우팅. 별도 라우트로 분리하여 타임아웃/커넥션 풀 독립 설정 | 일반 HTTP 라우트와 동일한 설정 적용 | WebSocket은 장기 연결이므로 `response-timeout`을 비활성화하거나 크게 설정해야 함. 일반 HTTP와 커넥션 풀을 공유하면 장기 연결이 풀을 점유 |
| **API 마켓플레이스 (외부 개발자 개방)** | Kong Enterprise 또는 AWS API Gateway + API Management 계층 | SCG 단독 | API 키 관리, 사용량 쿼터, 개발자 포털, 과금 등 API Management 기능이 필요. SCG는 런타임 프록시에 특화 |
| **내부 마이크로서비스 간 통신** | Service Mesh (Istio/Linkerd) 사이드카 프록시 또는 직접 HTTP/gRPC 호출 | API Gateway를 내부 통신에 사용 | 내부(East-West) 트래픽에 게이트웨이를 경유하면 불필요한 네트워크 홉과 SPOF 추가. Service Mesh가 mTLS, Rate Limiting, 분산 추적을 투명하게 처리 |
| **고가용성/무중단 배포** | 다중 인스턴스 + 헬스체크 + 롤링 업데이트. K8s Deployment로 관리 | 단일 인스턴스 배포 | 게이트웨이는 모든 트래픽의 유일한 진입점이므로 SPOF 방지가 필수. K8s에서 `replicas: 2+`, `readinessProbe`, `PodDisruptionBudget` 적용 |

### 4.3 안티패턴

#### 1. Gateway에 비즈니스 로직 포함

**증상**: 게이트웨이에서 DB 조회, 외부 API 호출, 데이터 변환 로직을 수행
**문제**: 게이트웨이가 "스마트 파이프"가 되어 ESB의 실패를 반복. 게이트웨이 장애 시 모든 비즈니스 로직이 함께 중단
**원칙**: 게이트웨이는 **"덤 파이프, 스마트 엔드포인트(Dumb pipe, smart endpoint)"** 원칙을 따라야 한다

#### 2. Gateway에서 블로킹 I/O 수행 (WebFlux 환경)

**증상**: GlobalFilter 내에서 `Thread.sleep()`, JDBC 호출, 동기 HTTP 호출 등 블로킹 코드 실행
**문제**: Netty의 이벤트 루프 스레드를 차단하여 **전체 게이트웨이가 먹통**이 됨. 소수의 느린 요청이 모든 요청에 영향
**해결**: 모든 I/O는 Reactor의 `Mono`/`Flux`를 통해 비동기로 수행. 불가피한 블로킹은 `Schedulers.boundedElastic()`으로 격리

#### 3. 과도한 요청/응답 변환

**증상**: 게이트웨이에서 JSON → XML 변환, 응답 집계(Aggregation), 대규모 페이로드 변환
**문제**: 게이트웨이가 CPU/메모리 병목이 되어 모든 라우트의 레이턴시가 증가
**원칙**: 변환은 서비스 레벨에서 수행. 게이트웨이는 헤더 추가/제거 정도의 경량 변환만 담당

#### 4. 서비스 디스커버리 없이 하드코딩된 URI

**증상**: `application.yml`에 `http://192.168.1.100:8080` 같은 IP/포트 직접 기재
**문제**: 서비스 스케일링, IP 변경 시 게이트웨이 재배포 필요
**단, K8s 환경에서는 예외**: `http://service-name:8080` 형태의 DNS 기반 URI는 K8s Service가 로드밸런싱을 담당하므로 실질적으로 디스커버리 역할을 수행. 서비스 수가 10개 미만이고 K8s를 사용한다면 별도의 Service Discovery(Eureka 등) 없이 정적 URI로 충분하다.

#### 5. 단일 Gateway 인스턴스 (SPOF)

**증상**: 게이트웨이를 단일 프로세스/Pod로 운영
**문제**: 해당 인스턴스 장애 시 모든 외부 트래픽 차단
**해결**: 최소 2개 이상의 인스턴스 배포, K8s `PodDisruptionBudget` 설정, 헬스체크 기반 자동 복구

#### 6. 모든 횡단 관심사를 Gateway에 집중

**증상**: 인증, Rate Limiting, Circuit Breaker, 로깅, mTLS, 분산 추적 등 모든 것을 게이트웨이 하나에서 처리
**문제**: 게이트웨이가 과도하게 복잡해지고, Service Mesh의 역할과 중복
**원칙**: **North-South 트래픽(외부→내부)**의 횡단 관심사는 게이트웨이가, **East-West 트래픽(서비스→서비스)**의 횡단 관심사는 Service Mesh가 담당하는 역할 분담이 바람직

---

## 5. 프로젝트 구현 비교 분석

> 이 섹션은 `api/gateway` 모듈의 현재 구현을 4단계의 베스트 프랙티스와 비교하여 분석한다.

### 5.1 잘 구현된 점 (Strengths)

#### S1. 순수 리액티브 아키텍처

**구현**: Spring Cloud Gateway(WebFlux/Netty) 기반. `spring-boot-starter-web`, `spring-boot-starter-security`(Servlet 기반)를 명시적으로 제외. 모든 필터가 `Mono<Void>`를 반환하는 논블로킹 구현.

**왜 좋은가**: SCG는 Netty 이벤트 루프 위에서 동작하며, Servlet 의존성이 하나라도 포함되면 WebFlux 자동 구성이 비활성화될 수 있다. `build.gradle`에서 Servlet/Security를 전역 제외한 것은 이 위험을 원천 차단하는 모범 사례이다.

#### S2. 명확한 필터 체인 순서 설계

**구현**:
```
1. HeaderSanitizeGlobalFilter  (HIGHEST_PRECEDENCE)     — 헤더 세정
2. RequestIdGlobalFilter       (HIGHEST_PRECEDENCE + 1)  — 요청 추적 ID
3. JwtAuthenticationGatewayFilter (HIGHEST_PRECEDENCE + 2) — 인증
4. Route-specific filters      (RateLimiter, Retry, RequestSize)
5. AccessLogGlobalFilter       (LOWEST_PRECEDENCE)       — 로깅
```

**왜 좋은가**: 보안 → 추적 → 인증 → 정책 적용 → 로깅 순서는 논리적으로 타당하다.
- 헤더 세정이 **가장 먼저** 실행되어 JWT 필터가 처리하기 전에 스푸핑된 헤더가 제거됨
- Request ID가 JWT 필터 전에 생성되어 인증 실패 로그에도 추적 ID가 포함됨
- Access Log가 **가장 마지막**에 실행되어 전체 처리 시간을 정확히 측정

#### S3. SCG 네이티브 기능 적극 활용

**구현**: `RequestRateLimiter`(Redis Token Bucket), `Retry`(GET/503, 지수 백오프), `RequestSize`(5MB), `DedupeResponseHeader`를 네이티브 필터로 사용.

**왜 좋은가**: 네이티브 필터는 Spring Cloud 팀이 유지보수하므로 버그 수정과 성능 최적화가 자동 반영된다. 커스텀 구현 대비 유지보수 부담이 크게 낮다. 프롬프트에서 언급된 "SCG 네이티브 우선" 원칙에 부합한다.

#### S4. 헤더 스푸핑 방지 (심층 방어)

**구현**: `HeaderSanitizeGlobalFilter`가 `HIGHEST_PRECEDENCE`로 모든 요청에서 `x-user-id`, `x-user-email`, `x-user-role` 헤더를 무조건 제거. 이후 JWT 필터가 검증된 값만 주입.

**왜 좋은가**: 공개 경로에서도 외부 헤더가 백엔드에 도달할 수 없다. 이는 많은 API Gateway 구현에서 놓치는 보안 취약점으로, 별도의 GlobalFilter로 처리한 것은 모범적이다.

#### S5. 라우트별 차등 Rate Limiting

**구현**: 공개 경로(auth, emerging-tech)는 IP 기반 `ipKeyResolver`, 인증 경로(bookmark, chatbot, agent)는 사용자 기반 `userKeyResolver`. 라우트별 replenishRate/burstCapacity 차등 설정. `deny-empty-key: false`로 Redis 장애 시 요청 통과 (Graceful Degradation).

**왜 좋은가**:
- IP 기반: 인증 전이므로 사용자 식별 불가 → IP가 유일한 키
- 사용자 기반: 인증 후이므로 공정한 사용자별 제한 가능
- `deny-empty-key: false`: Redis 장애 시 Rate Limiting이 실패해도 게이트웨이의 핵심 기능(라우팅, 인증)은 영향받지 않음

#### S6. 구조화된 Access Log

**구현**: `AccessLogGlobalFilter`가 전용 Logger(`ACCESS_LOG`)로 method, path, status, duration(나노초 정밀도), requestId, clientIp, userId, routeId, userAgent를 기록. X-Forwarded-For 헤더에서 실제 클라이언트 IP 추출.

**왜 좋은가**: 비즈니스 로그와 분리된 전용 Logger는 prod 환경에서 비즈니스 로그 레벨을 WARN으로 낮추더라도 Access Log를 독립적으로 INFO 출력할 수 있게 한다. 나노초 정밀도의 처리 시간 측정은 성능 분석에 유용하다.

#### S7. 환경별 프로필 분리

**구현**: local/dev/beta/prod 환경별 `application-{profile}.yml`에서 백엔드 URI, Redis 연결, CORS origin, 로그 레벨을 분리 관리. local은 localhost, dev/beta/prod는 K8s Service DNS.

**왜 좋은가**: 환경별 설정이 코드에 하드코딩되지 않고 프로필로 분리되어 K8s ConfigMap 교체만으로 환경 전환 가능.

#### S8. 커넥션 풀 최적화 설정

**구현**: `max-idle-time: 30s` (백엔드 keep-alive 60초보다 짧게), `max-life-time: 5m`, `max-connections: 500`.

**왜 좋은가**: idle timeout을 백엔드 keep-alive보다 짧게 설정하여 "이미 닫힌 연결에 요청 전송" 문제를 예방. `max-life-time`으로 주기적 연결 재생성을 보장하여 K8s 환경에서 DNS 변경이 반영됨.

### 5.2 개선할 점 (Areas for Improvement)

#### I1. ~~경로 분류 하드코딩~~ [High] — ✅ 구현 완료

**개선 전**: `isPublicPath()`, `isAdminOnlyPath()` 메서드에 경로 목록이 Java 코드에 직접 하드코딩되어 있었다. `/api/v1/auth/admin/login`만 공개로 열고 나머지 `/api/v1/auth/admin/**`은 보호하는 복잡한 if 분기가 포함.

**문제점**: 새 경로 추가 시 코드 수정과 재배포 필요. 복잡한 if 분기는 실수로 보안 허점을 만들 위험이 있었다.

**구현 내용**:
- `GatewaySecurityProperties` record (`@ConfigurationProperties(prefix = "gateway.security")`)로 `publicPaths`, `publicPathExclusions`, `adminOnlyPaths`를 YAML 외부화
- `JwtAuthenticationGatewayFilter`에서 `@PostConstruct`로 `PathPatternParser`를 사용하여 YAML 경로를 `PathPattern` 목록으로 파싱
- exclusion > inclusion 우선순위 로직으로 `isPublicPath()` 구현 — exclusion 패턴에 매칭되면 공개 경로에서 제외
- K8s ConfigMap 교체만으로 경로 정책 변경 가능

#### I2. ~~Circuit Breaker 미구현~~ [High] — ✅ 구현 완료

**개선 전**: 백엔드 서비스 장애 시 게이트웨이가 계속 요청을 전달. Retry 필터는 503에 대해 재시도하지만, 지속적 장애 시 차단 메커니즘이 없었다.

**문제점**: 하나의 백엔드 서비스가 지속적으로 타임아웃되면, 해당 라우트의 모든 요청이 60초 동안 대기하면서 커넥션 풀을 점유하여 장애 전파(cascading failure) 발생 가능.

**구현 내용**:
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j` 의존성 추가
- 5개 라우트 모두 SCG 네이티브 `CircuitBreaker` GatewayFilter 적용, 라우트별 개별 인스턴스로 장애 격리
- Resilience4j 설정: COUNT_BASED sliding window(size 10), failure-rate-threshold 50%, half-open 자동 전환
- chatbot/agent 라우트는 LLM 호출 특성을 반영하여 완화된 설정 (sliding-window-size: 20, minimum-number-of-calls: 10)
- `TimeLimiter`: 기본 60s, chatbot/agent 120s
- `FallbackController`에서 표준 `ApiResponse` 형식의 503 Service Unavailable 응답 반환

#### I3. 관리자 API IP 화이트리스트 미구현 [Medium]

**현재 상태**: 관리자 경로(`/api/v1/agent/**`, `/api/v1/auth/admin/**`)에 대해 JWT 기반 ADMIN role 검증만 수행. IP 기반 접근 제어 없음.

**문제점**: 관리자 JWT가 탈취된 경우, 공격자가 어떤 IP에서든 관리자 API에 접근 가능. 관리자 Access Token 유효기간이 15분으로 짧지만, 그 시간 동안 피해 가능.

**권장 개선 방향**: 관리자 경로에 IP 화이트리스트 적용. `application.yml`에서 허용 IP 관리. K8s 환경에서는 `XForwardedRemoteAddressResolver`로 실제 클라이언트 IP 추출.

**우선순위 근거**: 관리자 API는 시스템 전체에 영향을 줄 수 있는 고위험 엔드포인트이므로 다중 인증 계층(JWT + IP)이 바람직하다. 단, 현재 Admin Access Token 유효기간이 15분으로 이미 짧아 즉각적 위험은 제한적.

#### I4. ~~예외 핸들러의 문자열 기반 예외 매칭~~ [Medium] — ✅ 구현 완료

**개선 전**: `ApiGatewayExceptionHandler`에서 예외 유형을 `exception.getClass().getName().contains("Timeout")` 같은 문자열 매칭으로 판별.

**문제점**: 클래스 이름이 변경되거나 새로운 예외 유형이 추가되면 매칭 실패. `instanceof` 대비 타입 안전성이 낮고, 리팩토링 도구가 감지하지 못했다.

**구현 내용**:
- `instanceof` 타입 매칭으로 전면 변경: `TimeoutException`, `ReadTimeoutException` → 504, `ConnectException`, `ConnectTimeoutException`, `AbortedException` → 502
- `ex.getCause()` 체인 검사를 추가하여 Reactor/Netty가 래핑한 예외도 정확히 분류
- `ResponseStatusException`은 `instanceof`로 우선 처리 후 나머지 예외 유형을 분류하는 구조로 개선

#### I5. 내부 서비스 보안 미구현 [Medium]

**현재 상태**: 백엔드 서비스가 게이트웨이를 거치지 않고 직접 접근 가능한 환경(local/dev)에서, `x-user-*` 헤더를 위조하여 요청할 수 있다. 게이트웨이의 `HeaderSanitizeGlobalFilter`는 게이트웨이를 통과하는 요청에만 적용됨.

**문제점**: K8s prod 환경에서 NetworkPolicy가 설정되어 있지 않다면, Pod에서 직접 백엔드 서비스에 접근하여 헤더 위조 가능.

**권장 개선 방향**: K8s NetworkPolicy로 백엔드 서비스가 게이트웨이 Pod에서만 ingress를 받도록 제한. 보완적으로 `X-Gateway-Secret` 헤더 검증 추가.

**우선순위 근거**: local/dev 환경에서는 허용 가능하나, prod 환경에서는 내부 네트워크 공격 방어가 필요하다. K8s 배포 시 NetworkPolicy 설정이 선행되어야 한다.

#### I6. 토큰 해지(Revocation) 메커니즘 부재 [Low]

**현재 상태**: 순수 무상태 JWT 검증. 탈취된 토큰은 만료까지 유효하다.

**문제점**: 관리자 계정 탈취 시 최대 15분간 대응 불가. 일반 사용자는 60분.

**권장 개선 방향**: 현재 Admin Access Token 15분, User 60분의 짧은 유효기간이 이미 합리적인 완화 조치이다. Redis 기반 토큰 블록리스트는 게이트웨이에 Redis 의존성을 추가하고 무상태 원칙을 위반하므로, 보안 사고 발생 시 후속 조치로 검토하는 것이 적절하다.

**우선순위 근거**: 짧은 유효기간이 실질적 완화 조치로 기능하며, 즉시 도입의 트레이드오프(Redis 의존성, 무상태 위반)가 이점보다 크다.

#### I7. prod 로그 레벨 검토 [Low]

**현재 상태**: `application-prod.yml`에서 게이트웨이 비즈니스 로그가 WARN 레벨. Access Log는 별도 Logger로 INFO 출력 가능하나, 별도 Logback 설정이 없으면 루트 레벨에 의존.

**문제점**: prod에서 디버깅 시 유용한 INFO 레벨 로그가 출력되지 않을 수 있다.

**권장 개선 방향**: `ACCESS_LOG` Logger의 출력 레벨을 prod에서도 명시적으로 INFO로 설정. Logback 설정에서 `ACCESS_LOG` 전용 appender 구성 검토.

**우선순위 근거**: Access Log가 별도 Logger를 사용하므로 큰 문제는 아니나, 명시적 설정이 운영 안정성을 높인다.

### 5.3 종합 성숙도 평가

| 평가 축 | 점수 (1-5) | 근거 |
|---------|-----------|------|
| **라우팅** | 4 | YAML 선언적 라우팅, 환경별 프로필 분리, 정적 URI(K8s DNS)가 적절히 구현됨. Path Predicate만 사용하는 것은 현재 요구사항에 충분. Host/Method Predicate 조합이나 StripPrefix 등 고급 라우팅은 미사용이나 현재 필요하지 않음 |
| **인증/인가** | 4.5 | JWT 검증, 역할 기반 접근 제어, 헤더 주입이 잘 구현됨. 경로 분류가 `@ConfigurationProperties` + `PathPattern`으로 외부화되어 K8s ConfigMap 교체만으로 정책 변경 가능(I1 해결). 관리자 IP 화이트리스트(I3)가 추가되면 5점 |
| **회복탄력성** | 4.5 | Rate Limiting(Redis Token Bucket), Retry(GET/503, 지수 백오프), 요청 크기 제한에 더해 Resilience4j Circuit Breaker가 라우트별로 구현됨(I2 해결). LLM 라우트(chatbot/agent)에 완화된 설정 적용. Fallback 엔드포인트에서 표준 503 응답. Graceful Degradation(`deny-empty-key: false`)은 모범적 |
| **관측성** | 4 | 구조화된 Access Log(전용 Logger, 나노초 정밀도), X-Request-Id 생성/전파, Prometheus 메트릭 활성화. Micrometer Tracing/OpenTelemetry 연동이 완성되면 5점 |
| **보안 강화** | 4 | 헤더 스푸핑 방지(GlobalFilter), 요청 크기 제한, CORS 환경별 관리, DedupeResponseHeader. 내부 서비스 보안(I5)과 관리자 IP 화이트리스트(I3)가 추가되면 5점 |
| **성능 최적화** | 4 | 순수 리액티브 아키텍처, 커넥션 풀 최적화(idle < keep-alive, max-life-time), 적절한 타임아웃 계층. 블로킹 코드 검출(BlockHound), GC 튜닝은 미적용이나 현재 성능 이슈 없으면 충분 |
| **운영 준비도** | 4 | Actuator 엔드포인트(health, prometheus, gateway), 환경별 프로필, 구조화된 로깅, Circuit Breaker 기반 장애 격리. 커스텀 HealthIndicator(백엔드 서비스 상태 체크), Liveness/Readiness probe 분리가 추가되면 향상 |

**종합 평균: 4.1 / 5.0** (이전: 3.7)

I1(경로 분류 외부화), I2(Circuit Breaker), I4(예외 타입 매칭) 개선 이후, 인증/인가(3.5→4.5), 회복탄력성(3→4.5), 운영 준비도(3.5→4) 점수가 상향되었다. 현재 구현은 프로덕션 운영에 필요한 핵심 요구사항을 충족하며, 남은 개선 항목(I3 IP 화이트리스트, I5 내부 서비스 보안, I6 토큰 해지, I7 로그 레벨)은 보안 심화 및 운영 편의성 영역이다.

---

## 참고 자료

### 공식 문서
- [Chris Richardson, Microservices.io - API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [Microsoft Azure Architecture Center - API Gateways](https://learn.microsoft.com/en-us/azure/architecture/microservices/design/gateway)
- [Spring Cloud Gateway Reference Documentation](https://docs.spring.io/spring-cloud-gateway/reference/index.html)
- [Spring Cloud Gateway - GatewayFilter Factories](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories.html)
- [Spring Cloud Gateway - Global Filters](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/global-filters.html)
- [Netflix Zuul GitHub Repository](https://github.com/Netflix/zuul)
- [Kong Gateway Product Page](https://konghq.com/products/kong-gateway)
- [AWS API Gateway - REST API vs HTTP API](https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-vs-rest.html)
- [Envoy Gateway Documentation](https://gateway.envoyproxy.io/docs/)
- [Istio Traffic Management](https://istio.io/latest/docs/concepts/traffic-management/)
- [Kubernetes Gateway API](https://gateway-api.sigs.k8s.io/)

### 서적
- Chris Richardson, *Microservices Patterns*, Manning Publications, 2018. Chapter 8: External API Patterns

### Spring Cloud 관련
- [Spring Cloud 2020.0 (Ilford) Release - Zuul 제거](https://spring.io/blog/2020/12/22/spring-cloud-2020-0-0-aka-ilford-is-available)
- [Spring Cloud Gateway CVE-2025-41235](https://spring.io/blog/2025/05/29/spring-cloud-gateway-2025-05-29-releases/)

### 도구
- [Resilience4j 공식 문서](https://resilience4j.readme.io/docs)
- [Micrometer Tracing 공식 문서](https://micrometer.io/docs/tracing)
- [BlockHound - Reactor 블로킹 검출](https://github.com/reactor/BlockHound)
