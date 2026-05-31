# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime E2E 아키텍처 및 장애 징후 대응 명세

**대상 환경**: OpenJDK 21/25 LTS · Kubernetes · Cgroup v2 · 2026년 기준 Production Baseline

> 정독: 0회

## 목차

1. [인프라스트럭처 하부 전사 계층](#1-인프라스트럭처-하부-전사-계층)
2. [JVM 내부 메모리 토폴로지](#2-jvm-내부-메모리-토폴로지)
3. [실행 엔진과 컴파일러 아키텍처](#3-실행-엔진과-컴파일러-아키텍처)
4. [가비지 컬렉션 및 STW 완화 전략](#4-가비지-컬렉션-및-stw-완화-전략)
5. [스레드 동기화 및 동시성 아키텍처](#5-스레드-동기화-및-동시성-아키텍처)
6. [런타임 진단 및 트러블슈팅 사양](#6-런타임-진단-및-트러블슈팅-사양)
7. [2026 Production Baseline 가이드라인](#7-2026-production-baseline-가이드라인)

---

## 1. 인프라스트럭처 하부 전사 계층

핀테크 인프라 가상화 패러다임은 커널 네임스페이스와 자원 격리 메커니즘을 적극 활용하여 하드웨어 종속성을 제거한다. SRE 관점에서 JVM은 호스트 커널 위에서 고립된 단일 프로세스로 동작하며, 가상화 레이어와의 정밀한 얼라이먼트가 요구된다.

### 1.1 전체 계층 구조

```
┌─────────────────────────────────────────────────────────────────┐
│  [핀테크 트래픽/트랜잭션 가용 영토] 마이크로서비스 Pod / 컨테이너 환경         │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │  [JVM Process System Space]                             │   │
│   │                                                         │   │
│   │  ┌───────────────┐ ┌──────────────────┐ ┌────────────┐  │   │
│   │  │  Java Heap    │ │ Off-Heap (Direct) │ │ Metaspace │  │   │
│   │  └───────────────┘ └──────────────────┘ └────────────┘  │   │
│   └─────────────────────────────────────────────────────────┘   │
│                             │                                   │
│   ┌─────────────────────────▼───────────────────────────────┐   │
│   │  [Cgroup v2 및 리눅스 커널 가상 메모리 하부 시스템 (VMA)]        │   │
│   │                                                         │   │
│   │  - Memory Controller : memory.max / memory.high         │   │
│   │  - CPU Controller    : cpu.max CFS 대역폭 제어             │   │
│   └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│  [Host Hardware Layer] NUMA 노드 & 물리 메모리 버스 영역              │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Cgroup v1/v2 격리 및 JVM 컨테이너 인식 메커니즘

**`-XX:+UseContainerSupport`**

JVM은 기동 시 호스트의 CPU 코어 수와 가용 메모리를 동적으로 계산하여 스레드 풀 크기와 힙 풋프린트를 결정한다.  
이 플래그를 활성화하면 JVM이 cgroups 경로(`/sys/fs/cgroup/memory`, `/sys/fs/cgroup/cpu`)를 파싱하여 컨테이너 스케일의 자원 제한을 정확히 획득한다.

> **장애 패턴**: JDK 8 초기 버전은 Container Unawareness 특성으로 인해 컨테이너 내부에서 호스트 전체 자원을 쿼리하는 오동작을 유발하여 OOM Killer의 대상이 되었다.

**Cgroup v2 전환 고도화 (2026 표준)**

Cgroup v2 환경에서는 파일 시스템 계층이 통합되어 모니터링 데몬의 CPU/Memory 쿼리 오버헤드가 단축되었으며, `memory.high` 제어를 통해 시스템 물리 한계에 도달하기 전 선제적인 GC 트리거가 동기적으로 연동된다.

| 제어 파일 | 역할 |
|---|---|
| `memory.max` | 컨테이너 메모리 상한 (초과 시 OOM) |
| `memory.high` | 소프트 상한 — 초과 시 GC 선제 트리거 |
| `cpu.max` | CFS 대역폭 제어 (Quota/Period) |

### 1.3 CFS 쿼터 분배와 CPU Throttling

**커널 스케줄링 메커니즘**

Linux CFS 스케줄러는 `cpu.cfs_period_us` 주기 동안 컨테이너 프로세스가 사용할 수 있는 총 CPU 실행 시간인 `cpu.cfs_quota_us`를 집행한다.

```
# 예시: 200000 100000
# → 100ms 주기 동안 최대 200ms CPU 사용 가능 = 2 vCPU
```

**핀테크 장애 패턴**

대규모 동시 결제 스파이크 트래픽 유입 시 GC STW 멀티스레드 스캔 또는 스레드 폭증으로 할당 쿼터가 순식간에 소진된다. 커널이 프로세스를 강제 유휴 상태(Throttling)로 전환하면:

- JVM 내부 시계 기준의 **10ms 연산 → 리얼 타임 기준 수백 ms** 지연
- 타임아웃 도미노 현상 → 커넥션 풀 전소 → 임계 장애로 확산

> **SRE 감시 지표**: `container.cpu.throttled_time` — 초당 발생 비율이 전체 시간의 **15% 초과** 시 즉각 경보

### 1.4 리눅스 가상 메모리 관리와 JVM 메모리 맵핑

**Anonymous Pages와 커널 스왑 제어**

JVM 힙 영역은 주로 파일과 매핑되지 않은 Anonymous Pages로 구성된다. `vm.swappiness`가 높게 설정된 상태에서 메모리 압박이 가해지면 힙 메모리 일부가 디스크 스왑 공간으로 밀려난다.  
GC 가동 시 스왑 영역의 Anonymous Page에 접근하는 순간 수십 ms 단위의 디스크 I/O 블로킹(Page Fault)이 수반되어 레이턴시 보장 한계선이 파괴된다.

> **SRE 필수 설정**: `vm.swappiness = 0` (최소화) + 커널 단의 `mlockall(2)` 연동

### 1.5 NUMA 오버헤드와 메모리 할당 아키텍처

멀티 소켓 CPU 인프라에서 다른 소켓의 리모트 메모리 노드를 참조할 경우 QPI/UPI 상호 연결 버스를 경유하여 레이턴시가 **2~3배** 폭증한다.

**SRE JVM 튜닝 제어**: `-XX:+UseNUMA`를 명시하여 힙 공간(특히 Young Generation의 Eden 영역)을 NUMA 노드별로 분할 할당한다. 스레드가 객체를 생성할 때 현재 스레드가 실행 중인 CPU의 로컬 NUMA 노드에 우선 배정되어 Tail Latency를 방어한다.

---

## 2. JVM 내부 메모리 토폴로지

JVM 런타임의 가상 주소 공간은 GC가 제어하는 **관리형 영역(Managed Heap)**과 네이티브 런타임이 점유하는 **비관리형 영역(Off-Heap / Native Memory)**으로 파티셔닝된다.

### 2.1 Java Heap 구조

```
Java Heap
├── Young Generation
│   ├── Eden          ← 신규 객체 최초 안착
│   ├── Survivor 0    ← Minor GC 생존 객체
│   └── Survivor 1    ← 교차 이동 (Age 기록)
│         │
│         │ Age > MaxTenuringThreshold(-XX:MaxTenuringThreshold)
│         ▼
└── Old (Tenured) Generation
          ← 장기 생존 객체, 대규모 배열, 결제 세션 컨텍스트 상주
          ← 포화 시 Major GC / Full GC 트리거
```

**객체 생명주기 흐름**

1. 신규 객체 → Eden 영역 할당
2. Eden 포화 → **Minor GC** 트리거
3. 생존 객체 → Survivor 영역 복사 (Age +1 기록)
4. Age ≥ Threshold → **Old Generation 영구 승격(Promotion)**

### 2.2 Compressed OOPs와 32GB 메모리 절벽

64비트 아키텍처에서 Java 객체 포인터는 기본 **8바이트**를 점유한다. 힙 크기가 **32GB 미만**일 때 Compressed OOPs가 활성화되어 포인터를 **4바이트**로 압축 저장한다. CPU 레지스터 레벨에서 메모리 참조 시 가상 주소 값을 좌측으로 3비트 시프트하여 64비트 실제 오프셋 주소로 복원한다.

> **메모리 절벽(Memory Cliff)**: 힙 크기가 32GB를 **1바이트라도 초과**하는 순간 Compressed OOPs가 비활성화되어 메모리 사용량이 일시에 **약 20~30% 폭증**한다.

### 2.3 Metaspace 구조 및 튜닝

JDK 8 이후 고정 크기의 Permanent Generation이 제거되고 가변 크기의 네이티브 메모리 영역인 Metaspace로 전사되었다.

**저장 대상**: 클래스 메타데이터, 메서드 디스크립터, 런타임 상수 풀(Constant Pool)

**SRE 필수 제어**: `-XX:MaxMetaspaceSize`를 무제한으로 방치하면 동적 클래스 로딩이나 리플렉션을 다용하는 프레임워크 환경에서 메타스페이스가 무한 증식하여 커널 OOM Killer를 호출한다.

```bash
# 권장 설정: 초기값과 상한선을 동기화하여 런타임 GC 트리거 오버헤드 차단
-XX:MetaspaceSize=512m
-XX:MaxMetaspaceSize=512m
```

### 2.4 Thread Stack과 가상 메모리 매핑

각 Java 스레드는 OS 물리 네이티브 스레드와 **1:1 매핑**된다. 스레드 생성 시 지정된 크기(기본 1MB)만큼의 스택 영역이 `mmap`으로 할당된다.

| 예외 유형 | 발생 원인 |
|---|---|
| `StackOverflowError` | 깊은 호출 스택 또는 무한 재귀 |
| `OutOfMemoryError: unable to create new native thread` | 과도한 스레드 생성으로 커널 주소 공간 고갈 |

### 2.5 Off-Heap & Native Memory 파이프라인

**Direct Byte Buffer와 Zero-Copy 아키텍처**

고성능 금융 네트워크 통신(Netty 기반 이진 RPC)에서는 `ByteBuffer.allocateDirect()` 시스템 콜을 사용하여 JVM 힙 내부 버퍼에서 네이티브 시스템 버퍼로 데이터를 복사하는 오버헤드를 소거한다.

> **위험 요소**: 이 영역은 GC 관리 대상이 아니므로 자원 해제(Cleaner 작동)가 누락될 경우 **Heap Dump에도 잡히지 않는** 치명적인 네이티브 메모리 누수(Native Memory Leak)가 발생한다.

> **SRE 대응**: `NMT(Native Memory Tracking)` 상시 활성화 — `-XX:+NativeMemoryTracking=summary`

---

## 3. 실행 엔진과 컴파일러 아키텍처

자바 바이트코드(`.class`)가 물리 CPU의 하드웨어 명령어(x86/ARM 어셈블리)로 변환되는 실행 엔진 최적화 아키텍처이다.

### 3.1 인터프리터와 JIT 컴파일러의 하이브리드 아키텍처

```
.class 파일
    │
    ▼
Bytecode
    │
    ▼ (최초 실행: 한 줄씩 해석)
Interpreter (Tier 0)
    │
    │ 실행 빈도 임계치 초과 → Hot Spot 규정
    ▼
JIT Compiler → Native Machine Code (최고 전산 속도)
```

### 3.2 계층형 컴파일(Tiered Compilation) 아키텍처

| 계층 (Tier) | 수행 엔진 | 최적화 레벨 |
|---|---|---|
| **Tier 0** | Interpreter | 최적화 없음. 프로파일링 데이터(MDO) 수집 단계 |
| **Tier 1** | C1 (Client Compiler) | 단순 고속 컴파일. 프로파일링 배제 |
| **Tier 2** | C1 | 제한적 프로파일링 수집하며 경량 컴파일 |
| **Tier 3** | C1 | 풀 프로파일링(Full Profiling) 장착 C1 한계 최적화 |
| **Tier 4** | C2 (Server Compiler) | 그래프 이론 기반 전역 최적화. 최고 성능 기계어 |

**Code Cache Exhaustion — 핀테크 장애 유발 징후**

JIT 컴파일러가 생성한 기계어 코드는 Code Cache 영역에 적재된다. 대규모 결제 프레임워크나 동적 프록시 생성이 빈번하여 코드 캐시 용량 한계(`-XX:ReservedCodeCacheSize`)에 도달하면:

- JVM이 C2 최적화 컴파일을 중단
- 모든 스레드의 실행 엔진이 **Tier 0(인터프리터)**으로 강제 전락
- 연산 처리 성능 **10배 이상 급락** → CPU 사용량 100% 고착

### 3.3 고도화 최적화 기전

**탈출 분석(Escape Analysis) & 스칼라 대체(Scalar Replacement)**

객체의 생명주기가 단일 스레드/메서드 스코프를 벗어나지 않음이 확증되면 JVM은 객체를 힙에 할당하지 않고, 멤버 필드들을 CPU 스레드 스택 프레임에 직접 할당한다. 힙 메모리 압박을 근본적으로 줄여 GC 빈도를 획기적으로 낮춘다.

**메서드 인라이닝(Method Inlining)**

실행 빈도가 높은 미시 메서드(Getter/Setter 등)의 바이트코드를 호출부에 직접 이식하여 CPU의 스택 프레임 생성 오버헤드와 프로그램 카운터(PC) 점프 오버헤드를 완전히 제거한다.

### 3.4 GraalVM 컴파일러와 AOT (2026 최신 동향)

| 항목 | JIT (표준 HotSpot/GraalVM) | AOT (GraalVM Native Image) |
|---|---|---|
| 기동 시간 | 수백 ms ~ 수 초 | 수 ms |
| 메모리 풋프린트 | 기본 크기 | 약 1/5 수준 |
| 피크 처리량(Throughput) | 고도 튜닝 완료 시 최우위 | 동적 JIT 최적화 배제로 상대적 열위 |
| 적합 시나리오 | 롱런 결제 코어 서비스 | 빠른 Scale-Out이 필요한 경량 서비스 |

---

## 4. 가비지 컬렉션 및 STW 완화 전략

핀테크 서비스의 핵심 가치인 **트랜잭션 지연 방어**를 위해 GC 동작 시 발생하는 STW(Stop-The-World)의 미시적 분석과 대응 사양을 확정한다.

### 4.1 현대 핀테크 표준 가비지 컬렉터 비교

| 가비지 컬렉터 | 메모리 토폴로지 | 마킹 메커니즘 | STW 목표 |
|---|---|---|---|
| **G1 GC** | 힙 공간을 수천 개의 동적 리전(Region)으로 분할 | SATB(Snapshot-At-The-Beginning) 기반 동시 마킹 | 설정된 목표 타임(10~50ms) 내 가비지 많은 리전부터 우선 수거 |
| **ZGC** | 리전 기반 구조 + 64비트 Colored Pointers 기술 | 포인터 자체에 마킹 비트를 심어 Load Barrier로 처리 | 힙 크기(수 GB ~ 수 TB)에 무관하게 **STW 1ms 이하** 결빙 |

### 4.2 G1 GC 내부 정밀 역학

**메모리 단편화 방어 (Compaction)**

바둑판 모양의 리전 단위로 메모리를 관리하며, 가용 객체를 다른 빈 리전으로 복사하는 Compact 연산으로 메모리 단편화를 근본적으로 예방한다.

**장애 임계 징후**

- **Humongous Allocation**: 단일 리전 크기의 50%를 초과하는 대형 객체(금융 거래 이력, 대규모 배치 리스트 등)가 폭증하면 Evacuation Failure 상태에 빠짐
- **Full GC 전락**: 단일 스레드 직렬 연산으로 수 초간 결제 시스템을 물리적으로 마비

### 4.3 ZGC 내부 정밀 역학 — 현대 핀테크 최적 표준

**Colored Pointers 기술 포맷**

64비트 가상 메모리 주소의 상위 비트 필드를 마킹 용도로 활용한다.

```
64-bit Virtual Address (ZGC 전용)
┌──────────────────┬───────────┬────────────────────────────────────────────┐
│  16-bit (Unused) │ 4-bit Flags│       44-bit Object Virtual Address (16TB) │
└──────────────────┴───────────┴────────────────────────────────────────────┘
                         ▲
                         │ Marked0 / Marked1 / Remapped / Finalizable 비트 활성화
```

**Load Barrier 연산 파이프라인**

스레드가 힙 상의 객체 포인터를 참조하는 순간 CPU 레지스터 레벨에서 Load Barrier 코드가 동기적으로 개입한다. 참조 객체가 GC에 의해 이동 중(Relocation) 상태이면 스레드가 직접 포인터를 최신 포워딩 테이블 주소로 수정하는 **자가 치유(Self-Healing)** 연산을 집행한다.

모든 GC 스캔과 객체 이동 프로세스가 애플리케이션 스레드와 **동시성(Concurrent) 상태**로 실행되어 STW가 1ms 미만으로 제어된다.

### 4.4 Safepoint 메커니즘과 TTSP 오버헤드

**Safepoint의 공학적 정의**

GC가 안전하게 객체의 메모리 주소를 변경하고 그래프를 갱신하려면, 모든 애플리케이션 스레드가 변수 상태를 변경할 수 없는 안정된 지점인 **세이프포인트(Safepoint)**에 도달하여 대기해야 한다.

**TTSP(Time To Safepoint) 장애 메커니즘**

Counted Loop(인덱스 기반 명시적 반복문) 내부에서 대형 연산을 처리 중인 스레드는 루프가 완전히 종료될 때까지 Safepoint 폴링 체크를 수행하지 않는다.

> **기현상**: GC 로그 상에 GC 실 연산 시간은 **5ms**인데 총 지연 시간은 **500ms**로 찍히는 원인

> **SRE 필수 설정**: `-XX:+UseCountedLoopSafepoints` — Counted Loop 내부 Safepoint 폴링 강제 활성화

---

## 5. 스레드 동기화 및 동시성 아키텍처

결제 요청의 동시 처리를 제어하기 위해 CPU 멀티코어 메모리 가시성 법칙과 Java 스레드 스케줄링 메커니즘을 연동 제어하는 아키텍처 레이어이다.

### 5.1 Java 메모리 모델(JMM)과 자원 가시성

현대 멀티코어 CPU는 각 코어 내부에 L1/L2/L3 캐시 메모리를 운용한다. Java의 `volatile` 키워드는 컴파일러와 CPU의 명령어 재정렬(Reordering)을 원천 차단하고, 해당 변수의 읽기/쓰기 연산 시 물리 메인 메모리에 직접 동기화(**Memory Barrier 집행**)하도록 강제하여 멀티스레드 간 자원 가시성을 완벽히 확보한다.

### 5.2 가상 스레드(Virtual Threads / Project Loom) 아키텍처

**M:N 스레드 매핑 아키텍처** (JDK 21 표준)

수백만 개의 가상 스레드(M)를 소수의 OS 네이티브 플랫폼 스레드(Carrier Thread, N) 위에서 스케줄링하는 유저 레벨 동시성 모델이다.

```
┌────────────────────────────────────────────────────────────────────┐
│  [Java User Space] Virtual Threads 파이프라인 (M:N 매핑)              │
│                                                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ ... 수만 개   │
│  │ Virtual      │  │ Virtual      │  │ Virtual      │              │
│  │ Thread (V1)  │  │ Thread (V2)  │  │ Thread (V3)  │              │
│  └──────────────┘  └──────────────┘  └──────────────┘              │
│          │                │                 │                      │
│          └────────────────┴─────────────────┘                      │
│                           │ (Continuation 힙 스택 전사)              │
│                           ▼                                        │
│              ┌────────────────────────┐                            │
│              │ Carrier Thread (N개)   │                            │
│              └────────────────────────┘                            │
└────────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────────────┐
│  [Kernel Space] Linux 네이티브 LWP 커널 스레드 레이어                   │
└────────────────────────────────────────────────────────────────────┘
```

**Unmount와 Continuation 메커니즘**

1. 가상 스레드 내부에서 DB I/O, HTTP 블로킹 API 콜 발생
2. 호출 스택 프레임 상태를 힙 영역의 **Continuation** 데이터 구조로 전사
3. Carrier Thread에서 즉시 **분리(Unmount)** → 다른 가상 스레드 할당
4. I/O 완료 시 커널 이벤트(Epoll) 수신 → Continuation을 Carrier Thread에 **복원(Mount)** → 실행 재개

**Thread Pinning 장애 — 핀테크 실전 안티 패턴**

`synchronized` 블록 또는 JNI 네이티브 라이브러리 연산 도중 I/O 블로킹을 만나면 가상 스레드가 Carrier Thread에서 Unmount되지 못하고 고착된다. 플랫폼 스레드까지 동반 블로킹되어 **전체 가상 스레드 풀이 마비**된다.

> **SRE 필수 검증**: 소스코드 전반의 잠금 메커니즘이 `ReentrantLock` 체제로 완벽히 마이그레이션되었는지 정밀 검증
> **탐지 플래그**: `-Djdk.tracePinnedThreads=short`

---

## 6. 런타임 진단 및 트러블슈팅 사양

금융 결제 트랜잭션 도중 발생하는 임계 런타임 이상 징후를 감지하고 골든타임 내에 원인을 규명하기 위한 정량적 진단 체계이다.

### 6.1 SRE 임계 감시 지표 매트릭스

| 지표 그룹 | 핵심 수집 필드 | 경보 발령 조건 |
|---|---|---|
| **JVM Memory Usage** | `jvm.memory.used / committed` | Committed 대비 Used 비율 **92% 초과** 고착 시 메모리 누수 경보 |
| **GC Pause Latency** | `jvm.gc.pause` | 단일 STW **200ms 초과** 또는 분당 총 GC 점유 시간 **5초 상회** 시 경보 |
| **CPU Throttling Rate** | `container.cpu.throttled_time` | 초당 발생 비율이 전체 배정 시간의 **15% 초과** 시 타임아웃 장애로 확정 |
| **Thread State Count** | `jvm.threads.states` | BLOCKED 상태 스레드 **30개 이상** 폭증 시 데드락 또는 커넥션 풀 전소 탐지 |
| **Native Memory** | NMT 수집 | 지속적 증가 추세 감지 시 Native Memory Leak 경보 |
| **Code Cache** | JIT 캐시 사용량 | **90% 이상** 도달 시 코드 캐시 전소 임박 경보 |

### 6.2 프로덕션 노이즈 최소화 진단 기법

**JFR (Java Flight Recorder)**

JVM 커널 내부에 임베디드된 초경량 이벤트 기록기이다.

- **가동 오버헤드**: 1% 미만 → 핀테크 실운영 노드 상시 가동 가능
- **수집 항목**: 스레드 세이프포인트 지연 시간, 가상 스레드 피닝 이벤트, 락 경합 기간, GC, 메모리 할당

**Async-Profiler**

표준 프로파일러(`jstack` 등)의 **Safepoint Bias 오류**를 완벽히 극복하는 저오버헤드 툴이다.

- Linux 커널의 `perf_events` API와 JVM 내부 `AsyncGetCallTrace` API를 결합
- 프로파일링 대상 프로세스를 세이프포인트에 강제로 멈추지 않고도 CPU 점유 연산 스택을 정밀 추적
- **Flame Graph** 형태로 출력

### 6.3 실전 런타임 임계 장애 대응 런북

---

#### ⚡ 상황 A: API 타임아웃 간헐적 발생 + CPU 사용량 100% 고착

**1단계 — 커널 메트릭 추출**
```bash
# 타깃 Pod의 cgroup 경로에서 CPU 스로틀링 지표 쿼리
cat /sys/fs/cgroup/cpu.stat | grep -E 'nr_throttled|throttled_time'
```
`nr_throttled` 및 `throttled_time`의 가파른 우상향 곡선 검증

**2단계 — JIT Code Cache 검증**
```bash
jcmd <pid> VM.codecache
```
`Non-nmethods`, `Profiled nmethods`, `Non-profiled nmethods` 영역의 잔여 용량이 0인지 판별

**3단계 — 조치 파이프라인**

코드 캐시 전소 확인 시:
- `-XX:ReservedCodeCacheSize` 공간을 **2배 상향** 재배치
- 블루-그린 아키텍처로 **무중단 롤링 재시작**

---

#### ⚡ 상황 B: `java.lang.OutOfMemoryError: Java heap space` 검출

**1단계 — 정적 덤프 확보**

프로덕션 기동 플래그에 자동 탑재된 `-XX:+HeapDumpOnOutOfMemoryError` 사양에 의거하여 생성된 바이너리 힙 덤프(`.hprof`) 파일의 무결성을 격리 보존한다.

**2단계 — 동적 누수 패스 추적**

Eclipse Memory Analyzer(MAT)를 가동하여 힙 내부의 **Dominator Tree**를 쿼리한다.

- 특정 결제 트랜잭션 세션 객체나 인메모리 캐시 맵 구조체가 Retained Heap의 **80% 이상 독점** 여부 검증
- GC Root 레퍼런스 주소를 추적하여 소스코드 레벨의 자원 누수 지점 확정

---

## 7. 2026 Production Baseline 가이드라인

2026년 05월 기준 핀테크 코어 결제 시스템 환경에서 고가용성·초저지연·시스템 원천 안정성을 보장하기 위해 확정된 글로벌 표준 JVM 런타임 기동 사양이다.

### 7.1 전제 조건

- **Runtime Core**: OpenJDK 21 / 25 LTS (64-Bit Server VM)
- **Target Container Resource**: 최소 4 Cores, 16GB Memory 보장 환경
- **권장 GC**: ZGC
- **OS**: Linux Kernel + Cgroup v2 + Kubernetes

### 7.2 SRE 권장 Production Flag Manifest

```bash
java \
  # ── 인프라스트럭처 및 컨테이너 얼라이먼트 ──────────────────────────
  -XX:+UseContainerSupport \          # cgroups 경로 파싱으로 컨테이너 자원 정확 획득
  -XX:+UseNUMA \                      # NUMA 노드별 힙 분할 할당으로 Tail Latency 방어
  -Xms12g -Xmx12g \                  # 힙 고정 크기 (동적 확장 블로킹 오버헤드 차단)

  # ── 현대 초저지연 ZGC 가비지 컬렉터 ────────────────────────────────
  -XX:+UseZGC \
  -XX:ZAllocationSpikeTolerance=5 \   # 트래픽 스파이크 시 GC 마킹 스케줄 가중치 5배 상향

  # ── Safepoint TTSP 지연 오버헤드 봉쇄 ──────────────────────────────
  -XX:+UseCountedLoopSafepoints \     # Counted Loop 내 Safepoint 폴링 강제 활성화
  -XX:SafepointTimeoutDelay=100 \
  -XX:+SafepointTimeout \

  # ── 가상 스레드 Pinning 탐지 ────────────────────────────────────────
  -Djdk.tracePinnedThreads=short \    # Pinning 탐지 시 호출 스택 즉각 에러 로그 인쇄

  # ── Metaspace 물리 자원 보장 ────────────────────────────────────────
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \         # 초기값=상한선 동기화로 런타임 GC 트리거 차단
  -XX:ReservedCodeCacheSize=512m \    # JIT Code Cache 공간 확보

  # ── 임계 장애 골든타임 진단 자동 인쇄 ──────────────────────────────
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/jvm_dumps/payment_core_oom.hprof \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+NativeMemoryTracking=summary \ # Native Memory Leak 상시 추적
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc.log:time,uptime,pid:filecount=5,filesize=100M \

  -jar payment-core-service.jar
```

### 7.3 기동 플래그 아키텍처 조항 해설

**`-Xms12g -Xmx12g` 동기화 보장 조항**

초기 힙 크기와 최대 힙 크기를 완전히 일치시킨다. 런타임 도중 힙이 유동적으로 확장되면 가상 메모리 공간을 재할당하는 동안 커널이 주소 맵을 갱신하는 블로킹이 발생하여 결제 트랜잭션 수십 개가 타임아웃되는 오버헤드를 유발하므로 정적 고정 배치를 의무화한다.

**`-XX:ZAllocationSpikeTolerance=5` 급증 완화 조항**

ZGC의 동시성 마킹 가동 중 트래픽 스파이크로 객체 할당 속도가 비정상적으로 치솟는 경우, GC 엔진의 사전 마킹 계산 스케줄링 가중치를 5배 상향하여 메모리 고갈로 인한 동시성 모드 파괴(Fatal Error)를 선제적으로 방어한다.

**`-Djdk.tracePinnedThreads=short` 예방 조항**

Project Loom 가상 스레드 환경에서 Pinning 현상이 탐지되는 순간 해당 스레드의 축약된 호출 스택을 표준 에러 로그에 즉각 인쇄하여, SRE 팀이 배포 직후 락 고착 유발 모듈을 실시간으로 적발할 수 있는 관측 가능성(Observability)을 확보한다.

---

## 참고: OS-JVM 계층 간 인터페이스 분류표

| 구분 | 주도권 | 메커니즘 실체 | SRE 분석 도구/키워드 |
|---|---|---|---|
| 시스템 콜 (System Call) | App → Kernel | `int 0x80` / `syscall` 명령어. User Mode → Kernel Mode 전환 (Trap). `clone()`, `read()`, `write()`, `epoll_wait()` | `strace`, `perf trace`, `/proc/PID/syscall` |
| 가속 인터페이스 (vDSO) | App (커널 전환 없이) | 커널 메모리 일부를 User Space에 매핑. `gettimeofday()`, `clock_gettime()` | `/proc/PID/maps` 의 vdso 항목 |
| 하드웨어 인터럽트 (IRQ) | Hardware → Kernel | NIC 패킷 수신, Disk I/O 완료 시 발생. DMA 완료 신호 → Kernel 수신 | `/proc/interrupts`, `mpstat`의 `%irq` |
| 소프트 인터럽트 (SoftIRQ) | Kernel (후처리) | TCP/IP 스택 처리. `ksoftirqd` 커널 스레드 처리 | `mpstat`의 `%soft`, `/proc/softirqs` |
| 메모리 맵 (mmap) | App ↔ Kernel | 파일/장치를 프로세스 가상 주소에 직접 매핑. Page Fault 발생 시 Kernel이 실제 로드 | `/proc/PID/maps`, `vmstat`의 `pgfault` |
| cgroup & CPU Throttling | Kernel (자원 격리) | CFS Bandwidth Control: CPU Quota/Period. Quota 소진 시 컨테이너 Throttling 발생 | `/sys/fs/cgroup/cpu.stat`, `kubectl top` |
| 시스템 압력 지표 (PSI) | Kernel (관찰 인터페이스) | CPU/Memory/IO 자원 부족 압력 측정. `some`: 일부 task 대기 / `full`: 전체 대기 | `/proc/pressure/cpu`, `/proc/pressure/memory` |
| 커널 스케줄러 (CFS) | Kernel (능동 개입) | Red-Black Tree 기반 `vruntime` 관리. Runqueue / Wait Queue 관리. Context Switch 수행 | `/proc/schedstat`, `perf sched`, `vmstat`의 `cs` |
| TLB | Hardware + Kernel | 물리 주소 ↔ 가상 주소 변환 캐시. Context Switch 시 TLB Flush 발생 | `perf stat -e dTLB-load-misses` |
| 페이지 캐시 & 페이지 폴트 | Kernel (능동 관리) | Page Cache: 파일 데이터를 메모리에 캐시. Major Fault: Disk에서 실제 로드 필요 | `vmstat`의 `pgfault / pgmajfault` |
| eBPF Map | Kernel → App | Kernel 내부 통계 데이터를 Map에 기록. 애플리케이션 무수정 커널 관찰 | `bpftool map`, `bpftrace`, `Cilium` |

---

*본 문서는 Fin-Tech 결제 시스템 SRE 환경에서 JVM Runtime을 운영하기 위한 핵심 구조, 장애 유형, 모니터링 기준 및 운영 원칙을 정리한 기술 명세서입니다.*  
*기준 일자: 2026년 05월 | 대상 런타임: OpenJDK 21/25 LTS | 권장 GC: ZGC | 환경: Kubernetes + Cgroup v2*

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*