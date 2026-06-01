# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime Memory Pressure 장애 분석 가이드

## JVM 기반 금융 결제 시스템을 운영하는 SRE / Platform Engineer
### Linux Kernel (Cgroup v2), Kubernetes, JDK 21+, Generational ZGC

> 정독: 0회

## 목차

1. [Memory Pressure 전체 구조](#1-memory-pressure-전체-구조)
2. [하부 인프라 및 OS 커널 계층의 연동 기전](#2-하부-인프라-및-os-커널-계층의-연동-기전)
3. [JVM 내부 서브시스템별 Memory Pressure 역학](#3-jvm-내부-서브시스템별-memory-pressure-역학)
4. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026)](#4-기술-발전-및-핀테크-아키텍처-채택-동향-2026)
5. [관측성(Observability) 및 트러블슈팅 런북](#5-관측성observability-및-트러블슈팅-런북)
6. [2026 Production Baseline JVM Manifest](#6-2026-production-baseline-jvm-manifest)

---

## 1. Memory Pressure 전체 구조

Memory Pressure는 **시스템이 필요한 메모리를 충분히 확보하지 못하는 상태**를 의미한다.

JVM 기반 Fin-Tech 결제 시스템에서는 Memory Pressure가 발생할 때 다음 증상이 복합적으로 나타난다.

- GC 빈도 및 레이턴시 증가
- 결제 트랜잭션 응답시간 급등
- OOM(Out of Memory) 예외 발생
- Container 재시작 (Exit Code 137)
- Kernel OOM Killer 발동

> **핵심 원칙**: Memory Pressure는 Java Heap 부족만을 의미하지 않는다. 아래 모든 영역의 합산이 실제 메모리 압박을 결정한다.

### JVM 메모리 구성 전체 계층

```
Container Memory Limit (Cgroup memory.max)
│
├─ Java Heap          (-Xms / -Xmx)
├─ Metaspace          (클래스 메타데이터)
├─ Thread Stack       (-Xss × Thread 수)
├─ Direct Memory      (ByteBuffer.allocateDirect)
├─ Code Cache         (JIT 컴파일 결과)
├─ GC Internal Memory (GC 내부 메타데이터)
└─ Native Library     (JNI, Unsafe)
```

운영체제는 위 모든 영역의 합계를 **RSS(Resident Set Size)** 로 인식한다.

```
Total RSS = Heap + Off-Heap + Native Memory
```

Container Limit 설정 시 Heap 크기만 고려하고 Off-Heap 마진을 부여하지 않으면, JVM 내부 상태와 무관하게 **커널 레벨에서 메모리 포화 상태로 인지**하여 OOM Killer가 발동한다.

#### 위험한 설정 예시

```yaml
resources:
  limits:
    memory: 16Gi

# JVM 실제 사용량
# Heap         = 14 GB
# Metaspace    =  1 GB
# Direct Memory=  2 GB
# 합계         = 17 GB  →  Limit 초과 → OOM Killer → Exit Code 137
```

---

## 2. 하부 인프라 및 OS 커널 계층의 연동 기전

JVM 프로세스가 리눅스 커널 및 컨테이너 격리 계층 위에서 대규모 금융 트래픽을 처리할 때, Memory Pressure에 직면하는 시스템 역학 구조다.

```
+-------------------------------------------------------------------------------------+
| [Container / Pod]  memory.max (Cgroup v2 물리 한계선)                               |
|                                                                                     |
|   +-------------------------------------------------------------------------+       |
|   | [JVM Process Virtual Memory Area (VMA)]                                 |       |
|   |                                                                         |       |
|   |  ┌─────────────────┐   ┌─────────────────────────────────────────────┐  |       |
|   |  │   Java Heap     │   │  Off-Heap (Native Memory Space)             │  |       |
|   |  │ (-Xms / -Xmx)  │   │                                             │  |       |
|   |  │                 │   │  Metaspace        Thread Stacks (-Xss)      │  |       |
|   |  │  Committed      │   │  Code Cache       Direct Byte Buffers       │  |       |
|   |  │ (AlwaysPreTouch)│   │  JNI / Unsafe     GC Internal Metadata      │  |       |
|   |  └────────┬────────┘   └───────────────────────┬─────────────────────┘  |       |
|   |           │                                    │                         |       |
|   +-----------┼────────────────────────────────────┼-------------------------+       |
|               ▼                                    ▼                                |
|        Anonymous Pages                      Anonymous Pages                         |
|               │                                    │                                |
|               +─────────────────┬─────────────────+                                |
|                                 ▼                                                   |
|                  [Linux Kernel Memory Subsystem]                                    |
|                    Active / Inactive Anon Lists                                     |
|                    vm.swappiness  /  kswapd  /  Direct Reclaim                      |
|                                 │                                                   |
|                                 ▼  (회수 실패 시)                                   |
|                  [Cgroup OOM Killer]  →  SIGKILL (Exit Code 137)                   |
+-------------------------------------------------------------------------------------+
```

### 2-1. Cgroup v2 Memory Controller

리눅스 커널은 cgroups 메모리 서브시스템을 통해 컨테이너 프로세스의 물리 메모리 상한선(`memory.max`)을 통제한다.

JVM의 총 물리 메모리 점유량(RSS)은 Heap + Off-Heap의 물리적 합산으로 결정된다. SRE 엔지니어가 Container Limits 설정 시 Heap 크기만 고려하고 Off-Heap 마진을 부여하지 않으면 커널은 메모리 포화 상태로 인지한다.

---

### 2-2. 커널 페이지 회수 기전: kswapd와 Direct Reclaim

리눅스 커널은 물리 메모리 가용량을 세 가지 임계 워터마크로 관리한다.

| 워터마크 | 상태 | 커널 동작 |
|---|---|---|
| `WMARK_HIGH` | 여유 충분 | 정상 동작 |
| `WMARK_LOW` | 여유 부족 | **kswapd** 백그라운드 비동기 Page Reclaim 시작 |
| `WMARK_MIN` | 임계 부족 | **Direct Reclaim** 동기적 강제 회수 발동 |

#### Direct Reclaim이 결제 레이턴시를 파괴하는 경로

금융 트래픽 스파이크로 메모리 할당 속도가 kswapd의 회수 속도를 초과해 `WMARK_MIN` 이하로 떨어지면, 커널은 **모든 애플리케이션 스레드의 할당 연산을 멈추고** 동기적으로 메모리를 회수하는 Direct Reclaim 모드를 발동한다.

- 커널 내부의 Active/Inactive Anon 페이지 리스트를 주사
- 파일 백업 페이지를 버리거나 스왑을 시도하는 오버헤드 발생
- **수 ns 단위의 메모리 쓰기 연산이 수십 ms 단위의 동기 블로킹으로 전이**
- 결과: 결제 트랜잭션 응답시간 SLA 위반

---

### 2-3. vm.swappiness 오설정과 JVM Heap Swap 유출

`vm.swappiness`는 커널이 Memory Pressure를 받을 때 파일 기반 캐시(File-backed Pages)를 버릴지, 익명 페이지(Anonymous Pages)를 스왑 공간으로 밀어낼지의 가중치를 결정한다.

**핀테크 장애 패턴**

Java Heap은 파일과 매핑되지 않은 순수 **Anonymous Pages** 로 채워진다. 호스트 OS의 `vm.swappiness`가 기본값(64) 수준으로 높게 방치된 상태에서 Memory Pressure가 발생하면:

1. 커널이 유휴 상태로 오인된 JVM Heap 일부 페이지를 디스크 스왑 공간으로 배출
2. GC가 해당 영역을 스캔하거나 비즈니스 스레드가 객체에 접근하는 순간
3. **디스크 I/O를 동반하는 Major Page Fault가 동기적으로 대량 유발**
4. 밀리초 단위로 수렴하던 결제 트랜잭션 타임아웃 방어선 무력화

```bash
# Fin-Tech 운영 권장 설정
vm.swappiness=0
```

---

### 2-4. Kernel OOM Killer 발동 메커니즘

Direct Reclaim으로도 `memory.max` 한계를 충족할 수 없는 극단적 상황에서 커널은 OOM Killer를 소환한다.

**선정 기준**: `/proc/<pid>/oom_score_adj` 수치와 물리 메모리 점유량을 합산한 배드 포인트 점수가 가장 높은 프로세스를 타깃으로 **SIGKILL(Exit Code 137)** 을 강제 주입한다.

> **주의**: 이 현상은 JVM 런타임의 `try-catch`나 힙 덤프 생성 메커니즘이 인터셉트할 수 없는 **OS 레벨의 완전 강제 종료**다. Heap Dump, GC Log, Error Log가 생성되지 않을 수 있다.

---

## 3. JVM 내부 서브시스템별 Memory Pressure 역학

### 3-1. Java Heap 내부의 누수 및 고갈 기전

#### ① Promotion Rate Violation (조기 승격)

Young Generation(Eden, Survivor)의 크기가 유입 트래픽 양에 비해 너무 작으면:

1. 객체 연령(Age)이 임계치(`-XX:MaxTenuringThreshold`)에 도달하기 전에 Old Generation으로 **Early Promotion** 대량 발생
2. Old Generation의 Memory Pressure 급격히 상승
3. 백그라운드 동시성 마킹 완결 전에 새 객체가 공간을 가득 채우는 **Concurrent Mode Failure** / **Allocation Stall** 발생
4. 초 단위의 STW(Stop-The-World) 서비스 정지 유발

#### ② 영구적 누수(Retained Heap) 축적

GC가 회수할 수 없는 객체가 지속적으로 증가하는 현상이다. 대표적 원인은 다음과 같다.

| 패턴 | 코드 예시 | 문제 |
|---|---|---|
| Static Collection | `private static final Map<String, Object> CACHE = new ConcurrentHashMap<>()` | `remove()` / `clear()` 누락 |
| ThreadLocal | `ThreadLocal<UserContext>` | 스레드 종료 후 `remove()` 누락 |
| Listener 등록 | 이벤트 리스너 등록 후 해제 누락 | 참조 체인 영속 유지 |

GC 관점에서 활성 스레드 스택 및 정적 루트(GC Roots)로부터 참조 체인이 연결되어 있으므로 완벽한 **Reachable Object** 로 인식되어 수거 대상에서 영구 제외된다. 시간이 지남에 따라 힙 포화도가 100%로 수렴한다.

**진단 시그널**: GC 이후에도 `jvm_gc_live_data_size_bytes` 저점이 매 사이클마다 계단식으로 우상향하면 누수로 확정 판정한다.

---

### 3-2. Off-Heap / Native Memory 폭증 및 경합 리스크

#### ① Metaspace 가변 팽창

Metaspace는 클래스 메타데이터를 저장한다. 다음 상황에서 팽창한다.

- Spring Framework 동적 프록시 생성
- Jackson, Protobuf 등 직렬화/역직렬화 라이브러리의 리플렉션 고밀도 수행
- 동적 Bytecode 생성, ClassLoader 누수

`-XX:MaxMetaspaceSize`를 명시하지 않으면 Metaspace는 OS 네이티브 메모리를 한계 없이 `mmap`으로 징발하여 Cgroup OOM Killer의 간접 도화선이 된다.

```bash
# 운영 권장: 상한선 고정
-XX:MetaspaceSize=512m
-XX:MaxMetaspaceSize=512m
```

---

#### ② Direct Byte Buffer 고갈과 네이티브 메모리 Leak

고성능 비동기 네트워크 통신(Netty, NIO, 이진 RPC 게이트웨이)은 `ByteBuffer.allocateDirect()`를 통해 JVM 힙 외부의 네이티브 영역에 버퍼를 직접 구축한다.

**이원적 해제 구조의 함정**

```
Direct Byte Buffer 해제 경로:
  [Native 메모리] ←── PhantomReference(Cleaner) ←── [Java Heap 내 경량 Java 객체]
                                                              ↑
                                                     Java 힙 GC 발생 시에만 해제
```

Java Heap에 여유가 있어 GC가 한참 동안 발생하지 않는 상황에서도 Off-Heap의 Direct Buffer 공간만 급격히 포화될 수 있다. 이 상태에서 신규 할당 요청이 들어오면:

```
java.lang.OutOfMemoryError: Direct buffer memory
```

힙 덤프에도 잡히지 않는 이 예외가 발생하며 개별 결제 트랜잭션이 즉각 다운스트림된다.

```bash
# 운영 권장: 상한선 격벽 설정
-XX:MaxDirectMemorySize=2g
```

---

#### ③ 가상 스레드(Virtual Threads)와 Continuation Memory Pressure

JDK 21+ Project Loom 환경에서 가상 스레드가 I/O 블로킹을 만나 **Unmount** 될 때, 스레드 스택의 로컬 변수 및 실행 컨텍스트 전체를 Java Heap의 **Continuation 객체** 형태로 복사(전사)한다.

**가상 스레드 폭증의 역습**

| 구분 | Platform Thread | Virtual Thread |
|---|---|---|
| OS 스레드 매핑 | 1:1 | N:1 (캐리어 스레드) |
| 동시 생성 규모 | 수천 개 (스택 메모리 예측 가능) | 수만~수십만 개 |
| I/O 대기 시 스택 위치 | Native Stack (Off-Heap) | **Java Heap (Continuation 객체)** |

대규모 결제 지연으로 수십만 개의 가상 스레드가 동시에 I/O 대기 상태로 전환되면, 수십만 개의 스레드 스택 프레임 데이터가 Java Heap으로 일시에 쏟아져 순식간에 **Heap Memory Pressure**를 가중시킨다.

---

## 4. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026)

### 4-1. 32GB 메모리 절벽(Compressed OOPs) 시대

과거 자바 힙 설계의 철칙은 **31.5GB 미만**으로 힙 크기를 강제 봉인하는 것이었다.

| 힙 크기 | OOP 포인터 크기 | 메모리 오버헤드 |
|---|---|---|
| ≤ ~31.5GB | 4 Bytes (Compressed OOPs) | 최적 |
| > ~32GB | 8 Bytes (Full Pointer) | **약 20% 메모리 풋프린트 증가** |

이 제약으로 인해 과거 금융권은 단일 인스턴스를 키우는 Scale-Up 대신, 소규모 힙(8~16GB)을 장착한 컨테이너를 무수히 분산 배포하는 **Scale-Out 전략**에 의존했다.

---

### 4-2. Generational ZGC 표준 정착 (2026 현재)

JDK 21에서 도입된 **Generational ZGC** 는 2026년 현재 고가용성 결제 원장 시스템의 표준 GC로 안착했다.

**핵심 아키텍처**: Colored Pointers 기반의 가상 주소 공간 다중 매핑

| 특성 | 내용 |
|---|---|
| STW 레이턴시 | **1ms 이하** (힙 크기에 무관) |
| Young Generation 처리 | 고속 순회 수거 → 단기 결제 인스턴스 메모리 파편화 방어 |
| 대용량 힙 지원 | 64GB~128GB 이상의 통힙(Large Heap Space) 단일 운영 가능 |

**아키텍처 전환의 공학적 근거**

```
32GB 절벽 포인터 오버헤드 손실 비용
  <
64GB~128GB 통힙에서 가상 스레드 스택 자원 +
대용량 인메모리 원장 캐시 맵 단일 적재의 성능 이득
```

이에 따라 현대 금융권 핵심 노드는 **대용량 단일 힙 아키텍처**를 전면 채택하여 운영 중이다.

---

## 5. 관측성(Observability) 및 트러블슈팅 런북

### 5-1. SRE 실시간 탐지 지표 매트릭스 (Prometheus)

#### Container 계층

| 지표 | 설명 | 임계 경보 발령선 |
|---|---|---|
| `container_memory_working_set_bytes` | Cgroup이 인지하는 실질 물리 메모리 | `working_set / memory.max > 0.90` 고착화 시 OOM Killer 임박 경보 |
| `container_memory_rss` | RSS (Resident Set Size) | 지속 우상향 시 Off-Heap 누수 의심 |

#### JVM 계층

| 지표 | 설명 | 임계 경보 발령선 |
|---|---|---|
| `jvm_memory_used_bytes` | Java Heap 현재 실사용량 | `Heap Used / Heap Committed > 0.92` 돌파 및 GC 후 회복 실패 시 힙 고갈 확정 |
| `jvm_gc_live_data_size_bytes` | Full GC 직후 생존 객체 총량 | 매 수거 사이클 저점이 계단식 우상향 → **메모리 누수 확정** |
| `jvm_threads_live` | 활성 스레드 수 | 비정상적 급증 시 가상 스레드 Continuation 압박 의심 |

#### Native 계층

| 지표 | 설명 |
|---|---|
| Metaspace Usage | ClassLoader 누수, 동적 프록시 폭증 감시 |
| Direct Memory Usage | Direct Byte Buffer 고갈 감시 |
| Code Cache Usage | JIT 캐시 포화 감시 |

---

### 5-2. 프로덕션 노이즈 최소화 진단 명령 명세

#### ① NMT (Native Memory Tracking): Off-Heap 세그먼트 분석

자바 힙 외부의 물리 메모리 누수가 의심될 때, 프로파일링 오버헤드를 최소화하며 네이티브 메모리 카테고리별 점유 현황을 분석한다.

**사전 조건**: JVM 기동 플래그에 `-XX:+NativeMemoryTracking=summary` 활성화 필요

```bash
# 기준점 수립
jcmd <PID> VM.native_memory baseline

# 일정 시간 경과 후 기준점 대비 증분량 정밀 추적
jcmd <PID> VM.native_memory detail.diff
```

**결과 판독**: `Internal`, `Symbol`, `Memory (Direct Byte Buffer)` 파트의 물리 할당 지표가 기준점 대비 수 GB 단위로 지속 우상향하면 **네이티브 Off-Heap 자원 해제 누락** 으로 즉각 확정한다.

---

#### ② 대용량 힙 무정지 분석: Class Histogram

힙 포화 상태에서 `jmap -dump` 전체 힙 덤프를 무작위로 실행하면 STW 유발 또는 덤프 연산 자체가 크래시될 수 있다. Class Histogram을 먼저 가동한다.

```bash
# 라이브 인스턴스의 개수와 바이트 합계를 상위 50위까지 고속 추출
jcmd <PID> GC.class_histogram | head -n 50
```

**결과 판독**: 상위 항목에서 원시 타입(`[B`, `[C` 등)을 제외하고 특정 도메인 엔티티(예: `com.fintech.payment.context.PaymentSessionContext`)의 인스턴스 카운트가 수백만 개 이상으로 고착되어 있다면 해당 참조 노드를 누수 주범으로 특정한다.

**분석 도구**: Eclipse MAT, VisualVM, JDK Mission Control (JMC)

---

### 5-3. 장애 복구 런북 (Remediation)

#### 단기 긴급 조치 (Mitigation)

1. Memory Pressure 경보가 발령된 Pod를 인그레스 게이트웨이 라우팅 테이블에서 **즉시 격리**하여 신규 결제 트래픽 인입 차단
2. 해당 Pod의 재시작 정책을 유예한 뒤 백그라운드 힙 덤프 백업 수립
3. 무중단 Scale-Out 가동으로 클러스터 전체의 결제 처리 용량 유지

#### 영구 해결 자동화 플래그 고착화

OOM 발생 즉시 힙 덤프를 안전하게 생성하고 프로세스를 자동 재시작하도록 JVM 플래그를 물리적으로 고착화한다.

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/jvm_dumps/fintech_core_oom.hprof
-XX:OnOutOfMemoryError="kill -9 %p"
```

이를 통해 쿠버네티스 오케스트레이터가 가용성 상실을 즉각 감지하고 신규 Clean 인스턴스를 대체 투입(Self-Healing Container Restart)할 수 있도록 보장한다.

---

## 6. 2026 Production Baseline JVM Manifest

2026년 현재 대용량 금융 결제 원장 운영 및 Memory Pressure 장애를 구조적으로 방어하기 위해 검증 완료된 SRE 기동 사양서다.

```bash
java \
  # ── 힙 공간 고정: 동적 확장에 따른 레이턴시 서지 원천 차단 ──────────────────────
  -Xms32g -Xmx32g \
  -XX:+AlwaysPreTouch \

  # ── Generational ZGC: 대용량 메모리 1ms 이하 STW 보장 ──────────────────────────
  -XX:+UseZGC \
  -XX:+ZGenerational \

  # ── Off-Heap 상한 격벽: Cgroup OOM Killer 간접 도화선 차단 ─────────────────────
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  -XX:ReservedCodeCacheSize=512m \
  -XX:MaxDirectMemorySize=2g \

  # ── 가상 스레드 Continuation 폭증 대응: 유저 스택 안정화 ────────────────────────
  -Xss1m \

  # ── OOM 최후 방어선 및 진단 자동화 ─────────────────────────────────────────────
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/jvm_dumps/fintech_core_oom.hprof \
  -XX:OnOutOfMemoryError="kill -9 %p" \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+NativeMemoryTracking=summary \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc_memory.log:time,uptime,pid:filecount=10,filesize=100M \
  -jar fintech-payment-ledger.jar
```

### 핵심 플래그 공학적 조항 분석

| 플래그 | 목적 | 공학적 근거 |
|---|---|---|
| `-Xms32g -Xmx32g` | 힙 크기 고정 | 힙 동적 확장 시 OS에 메모리를 요청하는 지연(`mmap` + 물리 매핑) 제거 |
| `-XX:+AlwaysPreTouch` | 힙 물리 페이지 선점 | JVM 기동 시 전체 힙을 즉시 OS 물리 메모리에 커밋하여 런타임 Page Fault 차단 |
| `-XX:+ZGenerational` | Generational ZGC 활성화 | Young Generation 고속 수거로 단기 결제 인스턴스 파편화 방어, 1ms 이하 STW 유지 |
| `-XX:MaxMetaspaceSize=512m` | Metaspace 상한 격벽 | 제약 미설정 시 Native 메모리 무제한 징발 → Cgroup OOM Killer 간접 도화선 차단 |
| `-XX:MaxDirectMemorySize=2g` | Direct Buffer 격벽 | 포화 시 SIGKILL 대신 `OutOfMemoryError: Direct buffer memory` 예외를 JVM 단에서 제어하여 힙 덤프 골든타임 확보 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 시 힙 덤프 자동 생성 | 커널 OOM Killer 작동 전 사후 분석 데이터 확보 |
| `-XX:OnOutOfMemoryError="kill -9 %p"` | OOM 즉시 자살 | 좀비 프로세스(포트 열림, 헬스체크 통과, 결제 처리 불가) 상태 방어 → K8s Self-Healing 연동 |
| `-XX:+NativeMemoryTracking=summary` | NMT 활성화 | 프로덕션 오버헤드 최소화로 Off-Heap 누수 실시간 추적 가능 |

---

## 운영 원칙 요약

> **Memory Pressure는 Heap 사용량만으로 판단할 수 없다.**
>
> Fin-Tech 결제 시스템에서는 **Container → JVM → Native Memory → Linux Kernel** 전체 경로를 하나의 메모리 시스템으로 분석해야 한다.

분석 대상 영역을 항상 다음 전체를 포괄하여 감시한다.

- Java Heap (Used / Committed / Live Data Size)
- Metaspace / Code Cache / Direct Memory
- Thread Stack (Platform + Virtual Thread Continuation)
- Container RSS / Cgroup working_set
- Swap 사용량 / Major Page Fault 빈도
- Kernel OOM Killer 발동 이력 (`dmesg | grep -i oom`)

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*