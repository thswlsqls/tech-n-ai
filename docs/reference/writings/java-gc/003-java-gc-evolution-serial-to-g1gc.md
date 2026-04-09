# Serial GC에서 G1GC까지 — Java GC 알고리즘 20년의 진화

> 시리즈: Stop-the-World를 넘어서 — Java GC의 이해와 실전 튜닝 (3/5)

---

## SEO 제목 후보

- Java GC 알고리즘 비교 — Serial GC, Parallel GC, CMS, G1GC의 등장 배경과 핵심 차이
- G1GC는 어떻게 동작하는가 — Region, Remembered Set, Mixed GC 사이클 핵심 정리
- Java GC 발전사 완전 정리 — 세대별 가설부터 G1GC 내부 구조까지
- Stop-the-World를 줄여온 20년 — Java GC가 진화해 온 이유와 방향

---

## 들어가며

이전 글에서는 Java가 왜 GC를 필수적으로 채택해야 했는지를 JVM 아키텍처의 관점에서 살펴보았습니다. GC는 Java의 플랫폼 독립성과 메모리 안전성을 떠받치는 구조적 기둥이지만, 그 대가로 Stop-the-World(STW)라는 근본적인 트레이드오프를 안고 있었습니다. 특히 초기 Java에서 STW는 심각한 성능 문제였고, 이 문제를 완화하기 위한 노력이 이후 20년 넘는 GC 알고리즘의 발전을 이끌었습니다.

Java의 GC는 단일 스레드로 힙 전체를 정리하던 Serial GC에서 출발하여, 멀티스레드 병렬 처리(Parallel GC), 애플리케이션과의 동시 실행(CMS), 그리고 Region 기반의 예측 가능한 수집(G1GC)으로 진화해 왔습니다. 각 GC는 이전 세대의 한계를 해결하기 위해 등장했으며, 그 과정에는 분명한 인과 관계가 있습니다.

이번 글에서는 먼저 현대 GC의 이론적 토대인 세대별 가설과 Young Generation의 구조를 정리한 뒤, 네 가지 주요 GC 알고리즘의 등장 배경과 핵심 특성을 살펴보겠습니다. 특히 현재 Java의 기본 GC인 G1GC에 대해서는 내부 동작 구조를 좀 더 깊이 들여다볼 예정입니다.

---

## 세대별 가설 — 현대 GC의 이론적 토대

Java의 GC를 이해하려면 세대별 가설(Generational Hypothesis)에서 출발해야 합니다. 1편에서 간략히 언급한 바 있지만, 이 가설이 JVM의 힙 구조와 GC 전략에 미친 영향이 워낙 크기 때문에 좀 더 구체적으로 살펴보겠습니다.

세대별 가설은 두 가지 관찰로 구성됩니다. 약한 세대별 가설(Weak Generational Hypothesis)은 "대부분의 객체는 할당 직후 곧 사용되지 않게 된다"는 것이고, 강한 세대별 가설(Strong Generational Hypothesis)은 "오래 생존한 객체는 앞으로도 계속 생존할 가능성이 높다"는 것입니다. David Ungar가 1984년에 발표한 연구에서 이 관찰이 체계화되었으며, 이후 대부분의 GC 알고리즘이 이 가설에 기반하여 설계되었습니다.

이 관찰이 실무에서 왜 유효한지 생각해 보면 직관적으로 이해할 수 있습니다. 웹 애플리케이션의 요청 처리를 예로 들면, HTTP 요청이 들어올 때마다 DTO 객체, 중간 처리 결과, 응답 객체 등이 생성되지만, 응답이 완료되면 이 객체들은 더 이상 필요하지 않습니다. 반면 데이터베이스 커넥션 풀, 캐시, 설정 객체 같은 것들은 애플리케이션이 실행되는 동안 계속 살아 있습니다. 대다수의 객체는 전자에 해당하고, 후자는 소수입니다.

이 비대칭성을 활용한 것이 세대별 힙 구조입니다. 힙을 Young Generation과 Old Generation으로 나누고, 새로 생성된 객체를 Young Generation에 할당합니다. Young Generation은 작은 크기를 유지하면서 자주 수집하면, 금방 죽는 대다수의 객체를 빠르게 회수할 수 있습니다. 반대로 Old Generation은 장기 생존 객체가 모여 있으므로 드물게 수집하되, 수집 시 비용은 상대적으로 큽니다. 이 비대칭 전략이 GC 성능을 결정짓는 핵심 구조입니다.

---

## Young Generation의 구조 — Eden과 Survivor

Young Generation의 내부 구조를 이해하면, 이후에 다룰 각 GC 알고리즘의 동작 방식을 파악하기가 수월해집니다.

Young Generation은 Eden 영역과 두 개의 Survivor 영역(통상 S0, S1로 표기)으로 구성됩니다. 새로 생성되는 객체는 대부분 Eden 영역에 할당됩니다. Eden이 가득 차면 Minor GC(또는 Young GC)가 발생하고, 이 시점에서 Eden에 있는 객체들의 생사가 판정됩니다.

Minor GC가 실행되면 Eden과 현재 사용 중인 Survivor 영역에서 살아있는 객체를 찾아 다른 쪽 Survivor 영역으로 복사합니다. 1편에서 언급한 Cheney의 복사 수집(Copying Collection) 알고리즘이 여기에 적용됩니다. 복사가 완료되면 Eden과 이전 Survivor 영역은 통째로 비워집니다. 두 Survivor 영역을 번갈아 사용하는 이 구조 덕분에 Young Generation에서는 메모리 단편화가 발생하지 않습니다.

Survivor 영역에서 살아남은 객체에는 나이(Age)가 부여됩니다. Minor GC를 한 번 거칠 때마다 나이가 1씩 증가하고, `-XX:MaxTenuringThreshold`(기본값 15)에 도달한 객체는 Old Generation으로 승격(Promotion)됩니다. 세대별 가설의 관점에서 보면, 여러 차례의 Minor GC에서 살아남은 객체는 "앞으로도 오래 살 가능성이 높다"고 판단하여 Old Generation으로 옮기는 것입니다.

Minor GC는 STW를 수반하지만, 대상 영역이 전체 힙의 일부에 불과하므로 일반적으로 수 밀리초에서 수십 밀리초 수준으로 완료됩니다. 문제는 Old Generation이 가득 찼을 때 발생하는 Major GC 또는 Full GC이며, 이 시간을 어떻게 줄일 것인가가 GC 알고리즘 진화의 핵심 동력이었습니다.

---

## Serial GC — 모든 것의 시작

Serial GC는 JDK 1.3 이전부터 존재한 Java 최초의 가비지 컬렉터입니다. JVM 옵션으로는 `-XX:+UseSerialGC`로 명시적으로 지정할 수 있습니다.

이름에서 알 수 있듯이 Serial GC는 단일 스레드로 동작합니다. Young GC에서는 앞서 설명한 복사 수집을 수행하고, Old GC에서는 Mark-Sweep-Compact 알고리즘을 사용합니다. 살아있는 객체를 마킹하고, 죽은 객체를 제거(Sweep)한 뒤, 남은 객체를 한쪽으로 모아 압축(Compact)하는 세 단계를 순차적으로 진행합니다. GC가 실행되는 동안 모든 애플리케이션 스레드는 정지합니다.

Serial GC가 적합한 환경은 제한적입니다. 싱글 코어 머신이나 힙 크기가 수십에서 수백 MB 정도인 소규모 애플리케이션, 혹은 클라이언트 사이드 JVM에서 사용되었습니다. 멀티코어 서버에서 대규모 힙을 사용하는 현대적인 서버 애플리케이션에는 적합하지 않습니다.

그럼에도 Serial GC의 의의는 분명합니다. 구현이 단순하고 오버헤드가 최소라는 점에서 다른 GC 알고리즘의 비교 기준선(Baseline) 역할을 합니다. 그리고 Serial GC의 한계가 곧 다음 세대 GC의 설계 목표를 결정했습니다. "단일 스레드로는 부족하다"는 인식이 Parallel GC를, "STW를 줄여야 한다"는 요구가 CMS와 G1GC를 탄생시킨 것입니다.

---

## Parallel GC — Throughput의 극대화

Serial GC의 가장 직접적인 한계는 단일 스레드 동작입니다. 멀티코어 CPU가 보급되면서, GC 작업도 여러 스레드에서 병렬로 수행하면 전체 GC 시간을 줄일 수 있다는 아이디어가 자연스럽게 등장했습니다.

Parallel GC(`-XX:+UseParallelGC`)는 이 아이디어를 실현한 GC입니다. JDK 1.4.1에서 Young GC의 병렬화가 도입되었고, JDK 5 Update 6에서는 Old GC의 병렬 압축(Parallel Compaction)이 `-XX:+UseParallelOldGC` 옵션으로 추가되었습니다. 이후 JDK 6에서 이 옵션이 기본 활성화되면서 Young과 Old 모두 병렬로 처리되는 구조가 완성되었고, JDK 8까지 서버 환경에서 Java의 기본 GC로 사용되었습니다.

Parallel GC의 동작 방식은 Serial GC와 본질적으로 동일합니다. Young GC는 복사 수집, Old GC는 Mark-Sweep-Compact를 수행합니다. 차이점은 이 작업들을 여러 GC 스레드가 동시에 수행한다는 것입니다. 4코어 CPU에서 GC 스레드 4개가 병렬로 마킹과 압축을 수행하면, 이론적으로 Serial GC 대비 4배 가까운 속도 향상을 기대할 수 있습니다.

Parallel GC의 설계 목표는 Throughput의 극대화입니다. Throughput이란 전체 프로그램 실행 시간 중 GC가 아닌 애플리케이션 코드가 실행된 시간의 비율을 의미합니다. `-XX:GCTimeRatio` 옵션을 통해 "GC에 허용할 시간 비율"을 지정할 수 있으며, 기본값은 99입니다. 이는 GC 시간이 전체 실행 시간의 1% 이하가 되도록 GC가 스스로 힙 크기를 조절한다는 의미입니다.

Parallel GC는 배치 처리처럼 전체 처리량이 중요하고 개별 응답 시간에 덜 민감한 워크로드에서 효과적입니다. 하지만 근본적인 한계가 있습니다. 여전히 STW 방식이라는 점입니다. GC 스레드가 병렬로 동작하더라도, GC가 실행되는 동안 모든 애플리케이션 스레드는 정지합니다. 스레드 수를 늘려 GC의 총 소요 시간을 줄일 수는 있지만, 힙이 커질수록 Old GC 한 번에 수 초의 정지가 발생하는 것은 피할 수 없었습니다. 이는 응답 시간에 민감한 웹 서비스나 API 서버에서는 수용하기 어려운 특성이었습니다.

---

## CMS — 동시 수집이라는 새로운 시도

Parallel GC가 "GC를 더 빠르게 끝내자"는 접근이었다면, CMS(Concurrent Mark-Sweep)는 "GC의 대부분을 애플리케이션과 동시에 실행하자"는 근본적으로 다른 접근을 취했습니다. JDK 1.4.1에서 도입된 CMS는 낮은 지연 시간(Low Latency)을 설계 목표로 내세운 최초의 Java GC였습니다.

CMS의 핵심 아이디어는 Old Generation의 수집 과정 중 가장 오래 걸리는 단계 — 살아있는 객체를 찾아 마킹하고 죽은 객체를 회수하는 과정 — 를 애플리케이션 스레드가 실행되는 동안 함께 수행하는 것입니다. CMS의 수집은 네 단계로 이루어집니다.

첫 번째는 Initial Mark 단계입니다. GC Root에서 직접 참조하는 객체만 마킹하는 단계로, STW가 발생하지만 대상이 제한적이므로 매우 짧게 끝납니다.

두 번째는 Concurrent Mark 단계입니다. Initial Mark에서 찾은 객체들을 출발점으로 참조 체인을 따라가며 도달 가능한 객체를 추적합니다. 이 단계가 CMS의 핵심으로, 애플리케이션 스레드와 동시에 실행됩니다. 전체 Old Generation을 스캔하는 작업이므로 시간이 오래 걸릴 수 있지만, 애플리케이션이 멈추지 않으므로 사용자 입장에서는 지연을 체감하지 않습니다.

세 번째는 Remark 단계입니다. Concurrent Mark가 진행되는 동안 애플리케이션 스레드가 객체 참조를 변경했을 수 있으므로, 이를 보정하기 위한 최종 마킹을 수행합니다. STW가 발생하지만, 변경된 부분만 확인하므로 Full GC보다 훨씬 짧습니다.

네 번째는 Concurrent Sweep 단계입니다. 마킹되지 않은 객체를 회수하는 작업으로, 역시 애플리케이션과 동시에 실행됩니다.

CMS는 STW 구간을 Initial Mark와 Remark 두 번의 짧은 정지로 줄임으로써, 애플리케이션의 응답 시간을 크게 개선했습니다. Parallel GC에서 Old GC 한 번에 수 초가 걸리던 것을, 수십 밀리초 수준의 짧은 정지 두 번으로 대체한 것입니다.

하지만 CMS에는 구조적인 한계가 세 가지 있었고, 이 한계들이 결국 CMS의 퇴장과 G1GC의 등장으로 이어집니다.

첫 번째는 메모리 단편화(Fragmentation) 문제입니다. CMS의 이름에서 알 수 있듯이, 이 GC는 Sweep만 수행하고 Compact는 하지 않습니다. 죽은 객체의 공간을 비우되, 살아있는 객체를 한쪽으로 모아 정리하는 작업은 생략합니다. 시간이 지나면 Old Generation에 작은 빈 공간들이 흩어져 단편화가 누적됩니다. 전체 여유 공간은 충분하지만 연속된 공간이 없어 대형 객체를 할당하지 못하는 상황이 오면, CMS는 Serial Old GC로 폴백(Fallback)하여 힙 전체를 대상으로 단일 스레드 Compact를 수행합니다. 이때의 STW는 Parallel GC의 Full GC보다도 길 수 있습니다.

두 번째는 Concurrent Mode Failure입니다. CMS의 Concurrent Mark/Sweep이 완료되기 전에 Old Generation이 가득 차면, CMS 사이클이 중단되고 역시 Serial Old GC로 폴백합니다. 객체 승격(Promotion) 속도가 CMS의 회수 속도를 초과하는 상황에서 발생하며, 한번 발생하면 매우 긴 STW가 불가피합니다.

세 번째는 CPU 오버헤드입니다. Concurrent 단계에서 GC 스레드가 CPU를 소비하므로, 애플리케이션이 사용할 수 있는 CPU 자원이 줄어듭니다. Throughput 관점에서는 Parallel GC보다 불리할 수 있습니다.

이러한 한계들이 축적되면서 CMS는 JDK 9에서 deprecated(JEP 291)되었고, JDK 14에서 완전히 제거(JEP 363)되었습니다. CMS의 "동시 수집"이라는 핵심 아이디어는 유효했지만, 단편화와 폴백이라는 구조적 약점을 안고 있었던 것입니다.

---

## G1GC — Region 기반의 새로운 패러다임

G1GC(Garbage-First Garbage Collector)는 CMS의 구조적 한계를 해결하면서, 예측 가능한 STW 시간을 제공하는 것을 목표로 설계되었습니다. JDK 7u4에서 정식 도입되었고, JDK 9부터 Java의 기본 GC가 되었습니다(JEP 248). 이론적 기반은 2004년에 발표된 Detlefs 외의 논문 *"Garbage-First Garbage Collection"*(ACM ISMM '04)에서 찾을 수 있습니다.

G1GC가 이전 GC들과 근본적으로 다른 점은 힙의 물리적 구조를 바꾼 것입니다. Serial, Parallel, CMS까지의 GC에서 Young Generation과 Old Generation은 힙 내에서 연속된 고정 영역이었습니다. G1GC는 이 구조를 버리고, 힙 전체를 동일 크기의 Region으로 분할하는 접근을 취했습니다.

### Region 단위 힙 분할

G1GC에서 힙은 동일한 크기의 Region들로 나뉩니다. Region의 크기는 `-XX:G1HeapRegionSize` 옵션으로 지정할 수 있으며, 1MB에서 32MB 사이의 2의 거듭제곱 값이어야 합니다. 명시적으로 지정하지 않으면 JVM이 힙 크기에 따라 자동으로 결정하며, 대체로 Region 수가 약 2048개가 되도록 설정합니다.

각 Region은 고정된 세대에 속하지 않습니다. 시점에 따라 Eden, Survivor, Old, Humongous, Free 중 하나의 역할을 동적으로 부여받습니다. 어떤 Region이 지금은 Eden이었다가, GC 후에 Free가 되고, 나중에 Old로 사용될 수 있습니다. 이 동적 역할 할당이 G1GC의 유연성의 근간입니다.

Humongous Region은 Region 크기의 50%를 초과하는 대형 객체를 위한 것입니다. 하나의 Region에 담기지 않으면 연속된 여러 Region을 점유하며, Old Generation으로 직접 할당됩니다. Humongous 객체의 빈번한 할당은 GC 성능에 부정적인 영향을 줄 수 있는데, 이에 대해서는 5편의 튜닝 전략에서 다루겠습니다.

이 구조가 제공하는 핵심 이점은 선택적 수집입니다. 힙 전체를 수집하는 대신, 가비지 비율이 높은 Region만 골라서 수집할 수 있습니다. "Garbage-First"라는 이름은 여기서 유래합니다. 가비지가 가장 많은(수집 효율이 가장 높은) Region을 먼저(First) 수집한다는 뜻입니다.

### Remembered Set과 Card Table — 다른 Region의 참조를 어떻게 추적하는가

Region 단위로 수집할 때 한 가지 문제가 있습니다. 수집 대상 Region의 객체가 살아있는지 판단하려면, 다른 Region에서 이 객체를 참조하고 있는지를 알아야 합니다. 만약 다른 Region의 객체가 참조하고 있다면 그 객체는 살아있는 것이고, 아무도 참조하지 않는다면 회수할 수 있습니다. 이 cross-region 참조를 파악하기 위해 매번 힙 전체를 스캔하는 것은 비효율적이므로, G1GC는 Remembered Set(RSet)과 Card Table이라는 자료구조를 사용합니다.

Card Table은 힙 전체를 512바이트 크기의 Card로 논리적으로 분할한 배열입니다. 애플리케이션 코드에서 객체의 참조 필드에 값을 쓰면, JVM이 자동으로 삽입한 Write Barrier가 실행되어 해당 Card를 "dirty"로 표시합니다. 이는 "이 Card에 해당하는 메모리 영역에서 참조 변경이 발생했다"는 신호입니다.

Remembered Set은 각 Region이 개별적으로 보유하는 자료구조입니다. "이 Region의 객체를 참조하는 외부 Region의 Card" 목록을 기록합니다. JVM의 Refinement 스레드가 백그라운드에서 dirty로 표시된 Card를 처리하여, 해당 참조 정보를 대상 Region의 RSet에 반영합니다.

Region 수집 시에는 해당 Region의 RSet만 확인하면 외부 참조를 파악할 수 있습니다. 힙 전체를 스캔하지 않고도 "이 Region의 객체를 누가 참조하고 있는가"를 빠르게 파악할 수 있는 것입니다. 이 메커니즘이 G1GC가 Region 단위의 선택적 수집을 효율적으로 수행할 수 있는 핵심 기반입니다.

G1GC의 Write Barrier는 두 종류가 있습니다. Post-write barrier는 참조 필드에 새 값이 쓰인 후 실행되어 RSet 갱신을 위한 Card 마킹을 수행합니다. Pre-write barrier는 참조 필드의 이전 값을 기록하는 것으로, 뒤에서 설명할 SATB 알고리즘을 위한 것입니다.

### Mixed GC 사이클 — G1GC의 핵심 동작 흐름

G1GC의 수집 과정은 크게 세 단계로 구성됩니다. Young GC, Concurrent Marking, 그리고 Mixed GC입니다. 이 세 단계가 어떻게 연결되는지를 이해하면 G1GC의 전체 동작 구조가 파악됩니다.

첫 번째 단계는 Young GC(Evacuation Pause)입니다. Eden Region이 가득 차면 발생합니다. STW 상태에서 Eden Region과 현재 Survivor Region에 있는 살아있는 객체를 찾아, 새로운 Survivor Region 또는 Old Region으로 복사(Evacuation)합니다. 복사가 완료되면 원래 Region은 Free로 반환됩니다. 복사 기반이므로 Compaction이 자연스럽게 이루어지고, CMS에서 문제가 되었던 단편화가 발생하지 않습니다.

두 번째 단계는 Concurrent Marking입니다. Old Generation의 점유율이 `-XX:InitiatingHeapOccupancyPercent`(기본 45%)를 초과하면 시작됩니다. 이 단계의 목적은 Old Region들의 liveness(살아있는 객체의 비율)를 파악하는 것입니다. 어떤 Region에 가비지가 많은지 알아야, 나중에 Mixed GC에서 수집 효율이 높은 Region을 선택할 수 있기 때문입니다.

Concurrent Marking은 다섯 개의 세부 단계로 구성됩니다. Initial Mark는 GC Root에서 직접 도달 가능한 객체를 마킹하는 STW 단계인데, 독립적으로 수행되지 않고 Young GC에 편승(Piggyback)하여 함께 처리됩니다. 별도의 STW를 추가하지 않는 것입니다. Root Region Scan은 Survivor Region에서 Old Region으로의 참조를 스캔하는 단계로, 애플리케이션과 동시에 실행됩니다. Concurrent Mark는 힙 전체를 대상으로 참조 체인을 추적하며 도달 가능한 객체를 마킹하는 단계입니다. 역시 동시에 실행되며, 가장 오래 걸리는 단계입니다.

여기서 G1GC가 사용하는 SATB(Snapshot-At-The-Beginning) 알고리즘을 살펴볼 필요가 있습니다. Concurrent Mark가 진행되는 동안 애플리케이션 스레드는 객체의 참조를 변경할 수 있습니다. 만약 마킹 중인 객체의 참조가 변경되어 도달 가능했던 객체가 끊기면, 살아있는 객체를 죽은 것으로 잘못 판단할 위험이 있습니다. SATB는 이 문제를 해결하기 위해, 마킹 시작 시점의 객체 그래프 스냅샷을 기준으로 도달 가능성을 판단합니다. Pre-write barrier가 참조 변경 전의 이전 값을 SATB 버퍼에 기록하고, 이를 통해 시작 시점에 살아있던 객체가 마킹 과정에서 놓치지 않도록 보장합니다.

Remark는 SATB 버퍼를 처리하고 최종 마킹을 완료하는 STW 단계입니다. Cleanup은 부분적으로 STW를 수반하며, 완전히 비어 있는 Region을 Free로 회수하고 각 Region의 liveness 정보를 계산합니다.

세 번째 단계는 Mixed GC입니다. Concurrent Marking이 완료되어 각 Old Region의 liveness 정보가 확보되면, G1GC는 Young Region과 함께 가비지 비율이 높은 Old Region들을 선택하여 수집합니다. Young Region만 수집하는 것이 아니라 Old Region도 섞여(Mixed) 있기 때문에 Mixed GC라고 부릅니다.

Mixed GC는 한 번에 모든 대상 Old Region을 수집하지 않습니다. `-XX:G1MixedGCCountTarget`(기본 8)에 따라 여러 번의 Mixed GC로 나누어 수행합니다. 한 번의 Mixed GC에서 너무 많은 Region을 수집하면 STW가 길어지므로, 여러 번에 걸쳐 조금씩 수집하여 각 정지 시간을 목표 범위 안에 유지하는 것입니다. Region 선택 기준은 liveness가 낮은, 즉 가비지가 가장 많은 Region을 우선으로 합니다. 이것이 Garbage-First 전략의 실체입니다.

### Pause Time Target — 예측 가능한 정지 시간

G1GC의 가장 중요한 설정 중 하나는 `-XX:MaxGCPauseMillis`이며, 기본값은 200ms입니다. 이 값은 G1GC가 목표로 하는 최대 STW 시간입니다.

G1GC는 이 목표를 달성하기 위해 과거 GC 이벤트의 통계를 기반으로 예측 모델을 운용합니다. Region 하나를 수집하는 데 걸리는 시간, Region별 살아있는 객체의 양 등을 기록하고, 이를 바탕으로 목표 시간 내에 수집 가능한 Region 수를 예측하여 Collection Set(CSet)을 구성합니다. 이것이 G1GC의 Adaptive Sizing 메커니즘입니다.

여기서 한 가지 명확히 해야 할 점이 있습니다. `-XX:MaxGCPauseMillis`는 보장(Guarantee)이 아니라 목표(Goal)입니다. 힙 상태에 따라 이 값을 초과하는 정지가 발생할 수 있습니다. 그러나 G1GC는 지속적으로 이 목표에 수렴하도록 전략을 조정합니다.

이 값의 설정에는 주의가 필요합니다. 값을 너무 낮게 설정하면 한 번에 수집하는 Region 수가 줄어들어 GC 빈도가 증가하고, Old Region의 수집이 밀려 결국 Full GC로 이어질 위험이 있습니다. 반대로 너무 높게 설정하면 한 번의 정지가 길어져 응답 시간에 민감한 서비스에서 문제가 될 수 있습니다. 구체적인 설정 전략은 5편에서 워크로드 유형별로 정리할 예정입니다.

---

## CMS에서 G1GC로 — 무엇이 달라졌는가

CMS의 세 가지 구조적 한계와 G1GC가 이를 어떻게 해결했는지를 정리하면, G1GC가 기본 GC로 선택된 이유가 명확해집니다.

단편화 문제에 대해, CMS는 Sweep만 수행하고 Compact를 하지 않아 단편화가 누적되었습니다. G1GC는 객체를 새 Region으로 복사(Evacuation)하는 방식이므로, 수집이 곧 압축입니다. 단편화가 구조적으로 발생하지 않습니다.

폴백 위험에 대해, CMS는 단편화로 인한 할당 실패나 Concurrent Mode Failure 시 Serial Old GC로 폴백하여 긴 STW가 발생했습니다. G1GC도 최악의 경우 Full GC가 발생할 수 있지만, Region 단위의 선택적 수집과 Adaptive Sizing을 통해 이 상황에 이르기 전에 대응할 수 있는 여지가 훨씬 큽니다.

예측 가능성에 대해, CMS는 STW 시간을 제어하는 메커니즘이 없었습니다. G1GC는 `-XX:MaxGCPauseMillis`를 통해 목표 정지 시간을 설정할 수 있으며, GC가 이 목표에 맞춰 수집 범위를 스스로 조정합니다.

이런 개선점들이 JDK 9에서 G1GC가 기본 GC로 선택된 근거입니다. CMS의 "동시 수집"이라는 핵심 아이디어는 G1GC의 Concurrent Marking에 계승되었으며, Region 기반 구조라는 새로운 기반 위에서 CMS의 약점이 해결된 것으로 볼 수 있습니다.

---

## G1GC 이후의 방향

G1GC의 성공은 "Region 기반 수집 + Concurrent 처리"라는 패러다임이 대규모 힙 환경에서 유효하다는 것을 증명했습니다. 이 방향을 더 극단적으로 밀어붙인 것이 ZGC(JEP 333, JDK 11)와 Shenandoah(JEP 189, JDK 12)입니다. 두 GC 모두 sub-millisecond 수준의 정지 시간과 테라바이트 단위의 힙 지원을 목표로 하고 있으며, G1GC에서도 존재하는 STW 구간(Evacuation Pause)까지 동시 처리로 전환하려는 시도입니다. 이 주제는 이번 시리즈의 범위를 넘어서므로 상세히 다루지 않지만, Java GC의 진화가 여전히 진행 중이라는 점을 참고로 남겨 둡니다.

---

## 마치며

이번 글에서는 Java GC 알고리즘이 어떤 문제를 해결하며 진화해 왔는지를 Serial GC에서 G1GC까지 순서대로 정리해 보았습니다. 각 GC의 등장에는 이전 세대의 구체적인 한계가 있었고, 그 한계를 극복하는 과정에서 새로운 아이디어가 도입되었습니다.

Serial GC의 단일 스레드 한계가 Parallel GC의 병렬 처리를 이끌었고, Parallel GC의 STW 문제가 CMS의 동시 수집을 탄생시켰으며, CMS의 단편화와 폴백 문제가 G1GC의 Region 기반 구조를 만들었습니다. 이 인과 관계를 따라가면 각 GC의 설계 의도와 적합한 사용 환경이 자연스럽게 이해됩니다.

글을 정리하면서 개인적으로 다시 느낀 점은, G1GC의 내부 구조 — Region, RSet, Card Table, SATB, Mixed GC 사이클 — 를 이해하는 것이 단순한 지식 축적이 아니라 실무에서의 문제 진단과 직결된다는 것입니다. GC 로그에서 Mixed GC가 반복되는 패턴을 보거나, Humongous 할당이 빈번하게 발생하는 상황을 마주할 때, 이 구조를 알고 있느냐 없느냐가 원인 파악의 속도를 결정합니다.

다음 글에서는 실제 운영 환경에서 GC 문제를 진단할 때 사용하는 두 가지 핵심 도구 — GC 로그 분석과 힙 덤프 분석 — 를 비교하고, 어떤 상황에서 어떤 도구를 선택해야 하는지를 정리해 보겠습니다.

---

## 참고 출처

- Oracle. "Java Platform, Standard Edition HotSpot Virtual Machine Garbage Collection Tuning Guide, Release 21." — Chapters: The Garbage-First (G1) Garbage Collector.
- Detlefs, D. et al. (2004). "Garbage-First Garbage Collection." ACM SIGPLAN International Symposium on Memory Management (ISMM '04).
- Ungar, D. (1984). "Generation Scavenging: A Non-disruptive High Performance Storage Reclamation Algorithm." ACM SIGSOFT/SIGPLAN Software Engineering Symposium on Practical Software Development Environments.
- JEP 248: Make G1 the Default Garbage Collector (OpenJDK)
- JEP 291: Deprecate the Concurrent Mark Sweep (CMS) Garbage Collector (OpenJDK)
- JEP 363: Remove the Concurrent Mark Sweep (CMS) Garbage Collector (OpenJDK)
- Lindholm, T. et al. "The Java Virtual Machine Specification, Java SE 21 Edition." Oracle.

