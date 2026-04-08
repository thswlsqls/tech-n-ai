# C++과 Java로 구현하는 멀티스레딩 — 코드로 보는 두 언어의 철학 차이

> 시리즈: 클럭의 벽을 넘어서 — 게임 개발자가 풀어내는 멀티스레딩 성능 최적화 (3/4)

---

## SEO 제목 후보

- C++ std::thread vs Java Thread — 두 언어의 멀티스레딩 철학 비교
- C++과 Java 멀티스레딩 코드 비교 — 동기화, 메모리 모델, Virtual Threads까지
- lock-free부터 Virtual Threads까지, C++과 Java가 동시성을 다루는 방식
- std::atomic vs AtomicInteger — C++과 Java 메모리 모델이 다른 이유

---

## 들어가며

이전 글에서 멀티스레딩의 비용 구조와, 작업 성격에 따른 최적 전략의 차이를 살펴봤습니다. 이론적 배경을 정리했으니 이번에는 구현으로 넘어갈 차례입니다.

C++과 Java는 모두 멀티스레딩을 지원하지만, 접근 철학이 상당히 다릅니다. C++은 하드웨어에 가까운 제어권을 프로그래머에게 부여합니다. 메모리 순서를 직접 지정하고, 락의 생명주기를 RAII로 관리하며, 표준 라이브러리에 스레드 풀조차 포함하지 않을 만큼 "필요한 것만 제공한다"는 원칙을 고수합니다. 반면 Java는 안전성과 생산성에 무게를 둡니다. 메모리 모델을 단순화하고, 풍부한 동시성 유틸리티를 표준으로 제공하며, Virtual Threads처럼 런타임 수준의 추상화를 통해 동시성 프로그래밍의 진입 장벽을 낮추는 방향으로 발전하고 있습니다.

게임 엔진에서 C++ 멀티스레딩을 직접 다루다가 Java 기반 백엔드로 넘어오면서, 같은 문제를 두 언어가 얼마나 다르게 풀어내는지를 실감한 부분들이 있었습니다. 이 글에서는 스레드 생성, 동기화, 메모리 모델, 그리고 Virtual Threads까지 구체적인 코드와 함께 비교해 보겠습니다.

---

## 스레드 생성과 관리

### C++ — std::thread와 RAII의 의무

C++11에서 도입된 `std::thread`는 OS 네이티브 스레드를 직접 생성합니다.

```cpp
#include <thread>
#include <iostream>

void work(int id) {
    std::cout << "Thread " << id << " running\n";
}

int main() {
    std::thread t1(work, 1);
    std::thread t2(work, 2);

    t1.join();
    t2.join();
    return 0;
}
```

단순해 보이지만 C++만의 주의점이 있습니다. `std::thread` 객체가 소멸될 때 아직 `joinable` 상태라면 `std::terminate()`가 호출되어 프로그램이 즉시 종료됩니다. 이것은 설계상의 의도입니다. 스레드의 종료를 명시적으로 처리하지 않는 코드는 잠재적 버그이므로, 컴파일타임이 아닌 런타임에서라도 반드시 잡겠다는 C++의 엄격한 태도가 반영된 것입니다.

이를 안전하게 처리하려면 RAII 패턴을 사용합니다. C++20의 `std::jthread`는 소멸자에서 자동으로 `join()`을 호출하여 이 문제를 해결합니다.

```cpp
#include <thread>

int main() {
    std::jthread t1(work, 1);  // 소멸 시 자동 join
    std::jthread t2(work, 2);
    // join 호출 불필요 — 스코프를 벗어나면 자동 정리
    return 0;
}
```

### Java — Thread와 높은 수준의 추상화

Java에서 스레드를 생성하는 가장 기본적인 방법은 `Thread` 클래스를 직접 사용하는 것입니다.

```java
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = Thread.ofPlatform().start(() ->
            System.out.println("Thread 1 running"));
        Thread t2 = Thread.ofPlatform().start(() ->
            System.out.println("Thread 2 running"));

        t1.join();
        t2.join();
    }
}
```

Java에서는 `join()`을 호출하지 않아도 프로그램이 강제 종료되지 않습니다. 비데몬 스레드가 살아 있으면 JVM이 종료를 보류하고, 데몬 스레드는 JVM 종료 시 함께 정리됩니다. C++처럼 "실수하면 프로세스가 죽는" 방식이 아니라, GC처럼 런타임이 생명주기를 일정 부분 관리해 주는 구조입니다.

하지만 실무에서 `Thread`를 직접 생성하는 경우는 드뭅니다. Java는 `ExecutorService`를 통해 스레드 풀을 표준 API로 제공합니다.

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

Future<String> future = pool.submit(() -> {
    // 작업 수행
    return "result";
});

String result = future.get();  // 결과 대기
pool.shutdown();
```

C++의 표준 라이브러리에는 스레드 풀이 없습니다. 직접 구현하거나 Intel TBB, BS::thread_pool 같은 서드파티 라이브러리를 사용해야 합니다. 이 차이는 실무 생산성에 상당한 영향을 미칩니다. Java에서는 스레드 풀의 종류(`FixedThreadPool`, `CachedThreadPool`, `ForkJoinPool`)를 용도에 맞게 선택하는 것이 고민의 시작이지만, C++에서는 스레드 풀의 존재 여부와 구현 방식 자체가 고민의 시작입니다.

---

## 비동기 작업과 Future

### C++ — std::async와 launch 정책

C++은 `std::async`를 통해 비동기 태스크를 실행하고 `std::future`로 결과를 받습니다.

```cpp
#include <future>
#include <iostream>

int compute(int x) {
    return x * x;
}

int main() {
    std::future<int> f = std::async(std::launch::async, compute, 42);
    std::cout << f.get() << std::endl;  // 1764
    return 0;
}
```

여기서 주의할 점은 `std::launch` 정책입니다. `std::launch::async`는 별도 스레드에서 즉시 실행을 보장하지만, `std::launch::deferred`는 `get()` 호출 시점까지 실행을 지연합니다. 정책을 명시하지 않으면 구현에 따라 둘 중 하나가 선택되는데, 이 비결정성이 실무에서 미묘한 버그의 원인이 되기도 합니다. 비동기 실행을 기대했는데 `deferred`로 동작하여 메인 스레드에서 블로킹되는 상황이 대표적입니다.

또한 `std::async`가 반환하는 `std::future`는 소멸자에서 `get()`을 호출하여 스레드 완료를 대기합니다. 반환값을 무시하면 비동기로 실행한 의미가 없어지는 것입니다.

```cpp
// 주의: 반환된 future를 받지 않으면 즉시 블로킹됨
std::async(std::launch::async, compute, 42);  // 여기서 대기 발생
```

### Java — CompletableFuture의 체이닝

Java의 `CompletableFuture`는 C++의 `std::future`보다 훨씬 풍부한 조합 기능을 제공합니다.

```java
CompletableFuture<Integer> future = CompletableFuture
    .supplyAsync(() -> compute(42))
    .thenApply(result -> result + 10)
    .thenCompose(result -> fetchFromDb(result));

future.thenAccept(System.out::println);
```

비동기 작업을 체이닝하고, 여러 Future를 조합(`thenCombine`, `allOf`, `anyOf`)하는 것이 표준 API 수준에서 지원됩니다. C++에서 이와 동등한 기능을 구현하려면 상당한 양의 보일러플레이트 코드가 필요하거나 서드파티 라이브러리에 의존해야 합니다.

이 차이는 두 언어의 설계 철학을 반영합니다. C++은 최소한의 원시 도구를 제공하고 그 위에 무엇을 만들지는 프로그래머에게 맡기는 반면, Java는 실무에서 자주 사용되는 패턴을 표준으로 포함하여 일관된 사용 경험을 보장합니다.

---

## 동기화 메커니즘

### 뮤텍스와 락

임계 구역을 보호하는 가장 기본적인 방법은 뮤텍스를 사용하는 것입니다. C++과 Java 모두 이 메커니즘을 제공하지만, 락의 생명주기를 관리하는 방식이 다릅니다.

**C++ — RAII 기반 락 관리**

```cpp
#include <mutex>

std::mutex mtx;
int counter = 0;

void increment() {
    std::lock_guard<std::mutex> lock(mtx);  // 생성 시 락 획득
    ++counter;
    // 스코프를 벗어나면 자동으로 락 해제
}
```

`std::lock_guard`는 생성자에서 락을 획득하고 소멸자에서 해제합니다. 예외가 발생하더라도 스택 풀림(stack unwinding)에 의해 소멸자가 호출되므로 락이 반드시 해제됩니다. 이것이 C++의 RAII 패턴이 동기화에서 강력한 이유입니다.

조건 변수와 함께 사용해야 하거나 락을 중간에 해제해야 하는 경우에는 `std::unique_lock`을 사용합니다.

```cpp
std::mutex mtx;
std::condition_variable cv;
std::queue<int> queue;

void producer() {
    std::unique_lock<std::mutex> lock(mtx);
    queue.push(42);
    lock.unlock();         // 명시적 해제 후 통지
    cv.notify_one();
}

void consumer() {
    std::unique_lock<std::mutex> lock(mtx);
    cv.wait(lock, [&]{ return !queue.empty(); });
    int value = queue.front();
    queue.pop();
}
```

**Java — synchronized와 ReentrantLock**

Java에서 가장 간단한 동기화 방법은 `synchronized` 키워드입니다.

```java
private final Object lock = new Object();
private int counter = 0;

public void increment() {
    synchronized (lock) {
        counter++;
    }  // 블록을 벗어나면 자동으로 락 해제
}
```

더 세밀한 제어가 필요하면 `ReentrantLock`을 사용합니다.

```java
private final ReentrantLock lock = new ReentrantLock();
private final Condition notEmpty = lock.newCondition();
private final Queue<Integer> queue = new LinkedList<>();

public void producer() {
    lock.lock();
    try {
        queue.add(42);
        notEmpty.signal();
    } finally {
        lock.unlock();  // 반드시 finally에서 해제
    }
}

public void consumer() throws InterruptedException {
    lock.lock();
    try {
        while (queue.isEmpty()) {
            notEmpty.await();
        }
        int value = queue.poll();
    } finally {
        lock.unlock();
    }
}
```

C++의 `lock_guard`/`unique_lock`은 RAII 덕분에 개발자가 해제를 잊을 수 없지만, Java의 `ReentrantLock`은 `finally` 블록에서 명시적으로 `unlock()`을 호출해야 합니다. 이 차이가 사소해 보이지만, 복잡한 코드에서 `unlock()`을 빠뜨리는 실수는 데드락으로 직결됩니다. `synchronized`는 블록 스코프 기반이라 이 문제에서 자유롭지만, 타임아웃이나 공정성(fairness) 설정 같은 세밀한 제어가 불가능합니다.

---

## Lock-free 프로그래밍과 원자적 연산

락 기반 동기화는 안전하지만, 스레드 경합이 심한 환경에서는 락 대기가 병목이 될 수 있습니다. 이때 원자적 연산(atomic operation)을 사용하면 락 없이도 스레드 안전한 코드를 작성할 수 있습니다.

### C++ — std::atomic과 memory_order

```cpp
#include <atomic>

std::atomic<int> counter{0};

void increment() {
    counter.fetch_add(1, std::memory_order_relaxed);
}
```

C++에서 `std::atomic`을 사용할 때 `memory_order`를 직접 지정할 수 있습니다. 이것이 C++ 원자적 연산의 가장 큰 특징입니다. `memory_order`는 컴파일러와 CPU가 메모리 접근 순서를 얼마나 자유롭게 재배치할 수 있는지를 지정합니다.

- `memory_order_relaxed`: 원자성만 보장하고 순서는 보장하지 않음. 카운터처럼 다른 데이터와의 순서 관계가 중요하지 않을 때 사용.
- `memory_order_acquire`: 이 연산 이후의 메모리 접근이 이 연산 이전으로 재배치되지 않음. 락 획득 시 사용.
- `memory_order_release`: 이 연산 이전의 메모리 접근이 이 연산 이후로 재배치되지 않음. 락 해제 시 사용.
- `memory_order_seq_cst`: 가장 강력한 순서 보장. 모든 스레드가 같은 순서를 관찰. 기본값이지만 성능 비용이 가장 큼.

이 세밀한 제어가 게임 엔진 같은 환경에서 의미를 가지는 이유는, 아키텍처에 따라 `seq_cst`와 `relaxed`의 성능 차이가 상당할 수 있기 때문입니다. ARM 프로세서에서는 `seq_cst`가 메모리 배리어 명령어를 추가로 삽입해야 하므로, 초당 수백만 회 실행되는 원자적 연산에서 이 차이가 누적됩니다.

### Java — java.util.concurrent.atomic

```java
import java.util.concurrent.atomic.AtomicInteger;

AtomicInteger counter = new AtomicInteger(0);

public void increment() {
    counter.incrementAndGet();
}
```

Java의 원자적 연산은 `memory_order`를 개발자가 선택하지 않습니다. `AtomicInteger`의 모든 연산은 Java Memory Model이 정의하는 happens-before 관계를 보장하며, 이는 C++의 `memory_order_seq_cst`에 가까운 수준의 순서 보장입니다.

Java 9에서 `VarHandle`이 도입되면서 `getAcquire()`, `setRelease()`, `getOpaque()` 같은 약한 순서 보장 메서드가 추가되었지만, 실무에서 이를 직접 사용하는 경우는 JDK 내부 구현이나 고성능 라이브러리 개발에 한정됩니다. 대부분의 비즈니스 애플리케이션에서는 `AtomicInteger`, `AtomicReference` 수준의 API로 충분합니다.

이 차이가 보여주는 것은 두 언어의 대상 사용자에 대한 가정입니다. C++은 "프로그래머가 하드웨어를 이해하고 최적의 선택을 할 수 있다"고 가정하고, Java는 "올바른 프로그램을 작성하기 쉬워야 한다"고 가정합니다. 어느 쪽이 우월한 것이 아니라, 풀려는 문제의 성격이 다릅니다.

---

## 메모리 모델의 차이

멀티스레딩에서 가장 미묘하고 어려운 영역이 메모리 모델입니다. 하나의 스레드에서 변수에 값을 쓴 뒤, 다른 스레드에서 그 값이 보이는가(가시성, visibility)의 문제이며, 컴파일러와 CPU의 명령어 재배치(reordering)가 이를 더욱 복잡하게 만듭니다.

### C++ 메모리 모델

C++11부터 ISO 표준에 메모리 모델이 공식 포함되었습니다. 6종의 `memory_order`를 통해 프로그래머가 재배치 허용 범위를 명시적으로 지정합니다. 이 모델의 핵심은, 성능과 정확성 사이의 균형점을 프로그래머가 직접 선택한다는 것입니다.

```cpp
std::atomic<bool> ready{false};
int data = 0;

// 스레드 A (생산자)
void producer() {
    data = 42;                                          // (1)
    ready.store(true, std::memory_order_release);       // (2)
}

// 스레드 B (소비자)
void consumer() {
    while (!ready.load(std::memory_order_acquire)) {}   // (3)
    assert(data == 42);                                 // (4) 보장됨
}
```

`release`-`acquire` 쌍이 happens-before 관계를 만듭니다. (2)의 `release` 이전에 수행된 모든 쓰기(1)가, (3)의 `acquire` 이후의 읽기(4)에서 보이는 것이 보장됩니다. 만약 여기서 `memory_order_relaxed`를 사용했다면 (4)에서 `data`가 0일 수도 있습니다. 정확하게 사용하면 `seq_cst`보다 나은 성능을 얻을 수 있지만, 잘못 사용하면 재현 불가능한 버그가 됩니다.

### Java Memory Model

Java의 메모리 모델(JLS Chapter 17)은 happens-before 관계로 가시성 규칙을 정의합니다. 핵심 규칙은 다음과 같습니다.

- `synchronized` 블록의 unlock은 같은 모니터의 후속 lock에 대해 happens-before 관계를 형성합니다.
- `volatile` 변수에 대한 쓰기는 해당 변수의 후속 읽기에 대해 happens-before 관계를 형성합니다.
- `final` 필드는 생성자 완료 후 다른 스레드에서 올바르게 보이는 것이 보장됩니다.

```java
volatile boolean ready = false;
int data = 0;

// 스레드 A
void producer() {
    data = 42;          // (1)
    ready = true;       // (2) volatile 쓰기
}

// 스레드 B
void consumer() {
    while (!ready) {}   // (3) volatile 읽기
    assert data == 42;  // (4) 보장됨
}
```

C++의 예시와 논리적으로 동일하지만, Java에서는 `volatile` 키워드 하나로 가시성이 보장됩니다. `memory_order`를 선택할 필요가 없고, 잘못된 순서를 지정할 가능성 자체가 없습니다. 대신 C++처럼 `relaxed` 수준의 약한 보장을 선택하여 성능을 끌어올리는 것도 불가능합니다.

---

## Java Virtual Threads — 새로운 패러다임

Java 21에서 정식 도입된 Virtual Threads(JEP 444)는 멀티스레딩의 비용 구조 자체를 바꾸는 시도입니다.

기존 Java의 플랫폼 스레드는 OS 네이티브 스레드와 1:1로 매핑됩니다. 스레드 하나당 약 1MB의 스택 메모리를 소비하고, 생성과 소멸에 커널 호출이 필요합니다. 이 때문에 동시에 수천 개의 스레드를 유지하는 것은 메모리와 OS 리소스 측면에서 부담이 됩니다.

Virtual Threads는 JVM이 관리하는 경량 스레드입니다. 하나의 플랫폼 스레드(캐리어 스레드) 위에 수천 개의 Virtual Thread가 매핑되며, I/O 대기 시 자동으로 캐리어 스레드에서 분리되어 다른 Virtual Thread가 실행됩니다.

```java
// 100만 개의 Virtual Thread 생성
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1_000_000; i++) {
        executor.submit(() -> {
            // I/O 바운드 작업
            String result = httpClient.send(request, bodyHandler).body();
            processResult(result);
        });
    }
}
```

이전 글에서 I/O 바운드 작업의 적정 스레드 수를 `N_cores × (1 + W/C)`로 산정하는 공식을 살펴봤습니다. Virtual Threads는 이 고민을 근본적으로 줄여줍니다. I/O 대기 시 캐리어 스레드가 자동으로 해제되므로, 스레드 풀 크기를 신중하게 튜닝하지 않아도 됩니다.

다만 Virtual Threads가 만능은 아닙니다. CPU 바운드 작업에서는 이점이 없습니다. Virtual Threads가 아무리 많아도 실제 CPU 연산은 캐리어 스레드(플랫폼 스레드)에서 실행되므로, 물리 코어 수라는 상한은 동일합니다. 또한 `synchronized` 블록 내에서 I/O를 수행하면 캐리어 스레드가 고정(pinning)되어 Virtual Threads의 이점이 사라지는 문제가 있습니다. 이런 경우에는 `synchronized` 대신 `ReentrantLock`을 사용하면 pinning을 피할 수 있습니다.

### C++에는 대응물이 있는가

C++20에서 도입된 코루틴(Coroutines)이 유사한 문제를 다루지만, 접근 방식이 근본적으로 다릅니다. C++ 코루틴은 스택리스(stackless)입니다. 코루틴 프레임만 힙에 할당하고, 코루틴이 중단되면 프레임만 보존합니다. 반면 Java Virtual Threads는 스택풀(stackful)로, 각 Virtual Thread가 자체 콜 스택을 가집니다.

스택리스 코루틴은 메모리 효율이 높지만, 코루틴 함수의 호출 체인 전체가 코루틴으로 작성되어야 하는 "함수 색칠(function coloring)" 문제가 있습니다. Java Virtual Threads는 기존의 블로킹 코드를 그대로 사용할 수 있어 마이그레이션 비용이 낮다는 것이 실무적 강점입니다.

---

## 마치며

같은 멀티스레딩이라도 C++과 Java가 풀어내는 방식에는 분명한 차이가 있습니다. C++은 하드웨어의 동작을 이해하는 프로그래머에게 최대한의 제어권을 주고, Java는 올바른 프로그램을 더 쉽게 작성할 수 있도록 안전장치를 제공합니다.

게임 엔진에서 `memory_order_release`와 `memory_order_acquire`의 조합으로 락 없이 스레드 간 데이터를 전달하던 코드를 작성하다가, Java에서 `volatile` 하나로 같은 보장을 얻는 경험은 편리함과 동시에 "내가 포기한 제어권이 얼마나 되는가"를 생각하게 만들었습니다. 반대로 Java의 `CompletableFuture` 체이닝이나 Virtual Threads의 편의성을 보면, C++에서 같은 기능을 구현하기 위해 들였던 노력이 떠오르기도 합니다.

어떤 언어가 더 낫다는 결론보다는, 각 언어가 제공하는 도구의 특성을 정확히 이해하고 문제에 맞게 선택하는 것이 중요하다는 점을 다시 확인하게 됩니다. 다음 글에서는 실무에서 멀티스레딩을 도입하기 전에 확인해야 할 핵심 지표와, 도입 후 운영 안정성을 검증하는 방법을 C++ 게임과 Java 웹 애플리케이션의 관점에서 비교해 보겠습니다.

---

## 참고 출처

- ISO/IEC 14882 (C++ Standard) — Thread support library, Atomic operations library
- cppreference.com — std::thread, std::jthread, std::atomic, std::mutex, std::async, std::memory_order
- JLS (Java Language Specification) Chapter 17 — Threads and Locks
- Oracle Java SE Documentation — java.util.concurrent 패키지, java.lang.Thread
- JEP 444 — Virtual Threads
- JEP 425 — Virtual Threads (Preview), pinning 관련 설명
