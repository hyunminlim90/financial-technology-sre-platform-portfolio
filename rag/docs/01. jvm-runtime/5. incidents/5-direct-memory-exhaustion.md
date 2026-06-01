# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime Direct Memory Exhaustion 장애 분석 가이드

## JVM 기반 금융 결제 시스템을 운영하는 SRE / Platform Engineer
### Linux Kernel (Cgroup v2), Kubernetes, JDK 21+, Generational ZGC

> 정독: 0회

## 목차

1. [Direct Memory Exhaustion 전체 구조](#1-direct-memory-exhaustion-전체-구조)
2. [하부 인프라 및 OS 커널 계층의 연동 기전](#2-하부-인프라-및-os-커널-계층의-연동-기전)
3. [JVM 내부 서브시스템의 Direct Memory Exhaustion 정밀 역학](#3-jvm-내부-서브시스템의-direct-memory-exhaustion-정밀-역학)
4. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026)](#4-기술-발전-및-핀테크-아키텍처-채택-동향-2026)
5. [관측성(Observability) 및 트러블슈팅 런북](#5-관측성observability-및-트러블슈팅-런북)
6. [2026 Production Baseline JVM Manifest](#6-2026-production-baseline-jvm-manifest)

---

## 1. Direct Memory Exhaustion 전체 구조

### 정의

**Direct Memory Exhaustion**은 JVM 외부 Native Memory 영역에 할당되는 Direct Memory가 설정된 한계에 도달하여 신규 메모리 할당이 실패하는 장애다.

```
java.lang.OutOfMemoryError: Direct buffer memory
```

> **핵심 특성**: Direct Memory는 Java Heap 외부에 존재하므로, **Heap 사용률이 완벽히 정상이어도 발생할 수 있다.** Heap 지표만 감시하는 관측 체계로는 탐지가 불가능한 장애 유형이다.

### Direct Memory 주요 사용 컴포넌트

Fin-Tech 결제 시스템에서 Direct Memory를 주로 사용하는 구성 요소는 다음과 같다.

| 컴포넌트 | 사용 목적 |
|---|---|
| Netty / NIO | 비동기 네트워크 I/O 버퍼 |
| TLS/SSL 엔진 | 암호화 패킷 처리 버퍼 |
| Kafka Client | 메시지 직렬화/역직렬화 버퍼 |
| Database Driver | 쿼리 결과 전송 버퍼 |
| Binary Protocol Gateway | ISO-8583 등 이진 금융 전문 처리 버퍼 |

### Java Heap vs Direct Memory 비교

| 구분 | Java Heap | Direct Memory |
|---|---|---|
| 할당 API | `ByteBuffer.allocate(size)` | `ByteBuffer.allocateDirect(size)` |
| 메모리 위치 | JVM 관리 Heap 내부 | Native Memory (Heap 외부) |
| GC 직접 관리 | 가능 | **불가능** (Cleaner 간접 해제) |
| 객체 이동 | GC가 이동 가능 | 고정 주소 (Zero-Copy 가능) |
| Container RSS 영향 | 있음 | **있음** (별도 계산 필수) |

### 전체 메모리 계층에서의 위치

```
Container Memory Limit (Cgroup memory.max)
│
├─ Java Heap          (-Xms / -Xmx)
├─ Metaspace
├─ Thread Stack
├─ Code Cache
├─ Direct Memory      (-XX:MaxDirectMemorySize)  ← 이 영역
└─ Native Library
```

**위험한 설정 예시**

```yaml
# Container Limit = 16 GB

# JVM 실제 사용량
# Heap         = 12 GB
# Direct Memory=  4 GB
# Metaspace    =  1 GB
# 합계         = 17 GB  →  Cgroup Limit 초과 → OOM Killer → Exit Code 137
```

---

## 2. 하부 인프라 및 OS 커널 계층의 연동 기전

JVM 런타임의 오프힙(Off-Heap) Direct Memory 고갈이 리눅스 커널 메모리 관리 아키텍처 및 시스템 콜 라이프사이클과 직접 맞물려 진행되는 물리적 역학 구조다.

```
+-------------------------------------------------------------------------------------+
| [Container / Pod]  memory.max (Cgroup v2 격리 경계선)                                  |
|                                                                                     |
|   +-------------------------------------------------------------------------+       |
|   | [JVM Process 가상 주소 공간 (VMA)]                                          |       |
|   |                                                                          |       |
|   |  ┌──────────────────────┐   ┌────────────────────────────────────────┐   |       |
|   |  │     Java Heap        │   │     Off-Heap (Native Memory)           │   |       |
|   |  │  (-Xms / -Xmx)       │   │                                        │   |       |
|   |  │                      │   │  Direct Memory Space                   │   |       |
|   |  │  [Managed Objects]   │   │  (-XX:MaxDirectMemorySize)             │   |       |
|   |  │                      │   │  [malloc / mmap] → [물리 페이지 매핑]      │   |       |
|   |  └──────────┬───────────┘   └───────────────────────┬────────────────┘   |       |
|   |             │                                       │                    |       |
|   +─────────────┼───────────────────────────────────────┼────────────────────+       |
|                 ▼                                       ▼                           |
|           Anonymous Pages                         Anonymous Pages                   |
|                 │                                       │                           |
|                 +────────────────────┬─────────────────+                           |
|                                      ▼                                              |
|                      [Linux Kernel Memory Subsystem]                                |
|                        kswapd / Direct Reclaim (페이지 회수 기전)                       |
|                                      │                                              |
|                                      ▼  (회수 실패 시)                                |
|                      [Cgroup OOM Killer] → SIGKILL (Exit Code 137)                 |
+-------------------------------------------------------------------------------------+
```

### 2-1. glibc 메모리 할당 아키텍처와 커널 가상 메모리 매핑

JVM이 Direct Memory를 징발할 때 내부 호출 흐름은 다음과 같다.

```
Java (ByteBuffer.allocateDirect)
        ↓
JNI (Java_java_nio_Bits_allocateMemory)
        ↓
glibc malloc(3) 또는 mmap(2)
        ↓
Linux Kernel (가상 주소 공간 VMA 등록)
        ↓
물리 페이지 매핑 (최초 쓰기 시 Page Fault로 완성)
```

**malloc vs mmap 분기 기준**

| 할당 크기 | glibc 동작 | 커널 동작 |
|---|---|---|
| MMAP_THRESHOLD (128KB) **미만** | `brk(2)`: 기존 힙 세그먼트 확장 | 연속 가상 주소 확장 |
| MMAP_THRESHOLD (128KB) **초과** | `mmap(2)`: 독립 VMA 신규 생성 | 독립 익명 가상 메모리 영역 생성 |

**물리 메모리 지연 바인딩**: `mmap`으로 확보된 Direct Memory 공간은 가상 주소만 선점된 상태다. 실제 물리 페이지 테이블 매핑은 금융 네트워크 패킷 데이터가 해당 버퍼에 **최초로 쓰여지는 순간 Page Fault 인터럽트를 거쳐** 완성된다. Direct Memory 사용량이 치솟을수록 VIRT(가상 주소 크기)와 RSS(물리 메모리 점유량)가 동반 폭증한다.

---

### 2-2. High I/O 환경에서의 Direct Reclaim 레이턴시 파괴

초당 수천 건의 이진 금융 전문(ISO-8583, 암호화 패킷 등)을 처리하는 결제 시스템에서는 Direct Buffer의 생성과 폐기가 극단적으로 반복된다.

**메모리 워터마크 붕괴 연쇄 경로**

```
Direct Buffer 대량 생성 → 가용 물리 메모리 감소
        ↓
WMARK_LOW 이하 → kswapd 백그라운드 회수 시작
        ↓
메모리 소모 속도 > 회수 속도
        ↓
WMARK_MIN 붕괴 → Direct Reclaim 발동
        ↓
모든 비즈니스 스레드의 Direct Memory 쓰기/할당 연산 동기 급제동
        ↓
네트워킹 버퍼 할당 레이턴시: 나노초 → 밀리초 (수만 배 폭증)
        ↓
Netty 이벤트 루프(Event Loop) 스레드 전면 블로킹
        ↓
전체 결제 트랜잭션 타임아웃
```

---

### 2-3. Cgroup OOM Killer 발동과 JVM의 무력화

Java Heap(`-Xmx`)이 컨테이너 `memory.max`보다 작게 잡혀있더라도, **Direct Memory가 한계 없이 증식하여 둘의 합산이 Cgroup 가용 한계를 넘어서면** 커널 OOM Killer가 즉각 개입한다.

커널은 물리 자원을 가장 많이 점유 중인 JVM 프로세스에 **SIGKILL(Exit Code 137)** 을 직접 투사한다.

> **주의**: 이는 JVM 내부 예외 핸들러가 가로챌 수 없는 외부 커널의 물리적 강제 종료다. 애플리케이션 로그에 아무런 흔적 없이 프로세스가 소멸되므로, 사전 격벽 설정과 진단 파이프라인 구축이 필수다.

---

## 3. JVM 내부 서브시스템의 Direct Memory Exhaustion 정밀 역학

### 3-1. Direct Byte Buffer와 Zero-Copy 아키텍처의 필요성

표준 Heap 메모리에 상주하는 버퍼 데이터를 네트워크 카드(NIC)로 전송하려면, GC가 객체 주소를 지속적으로 이동(Relocation)시키므로 안전한 전송을 위해 OS 네이티브 커널 메모리 영역에 데이터를 **한 번 더 복사하는 I/O 오버헤드**가 수반된다.

`ByteBuffer.allocateDirect()`는 이 오버헤드를 제거하기 위해 JVM 통제 영역 바깥의 네이티브 메모리 공간에 버퍼를 직접 구축하고, 커널이 Heap 가상화 장벽을 거치지 않고 직접 참조하도록 만드는 **Zero-Copy 파이프라인**을 실현한다.

```
[Zero-Copy 경로]
Application
      │
      ▼
Direct Buffer (Native Memory, 고정 주소)
      │  ← 복사 없음
      ▼
Kernel Socket Buffer
      │
      ▼
NIC (Network Interface Card)
```

---

### 3-2. Cleaner 메커니즘과 힙·오프힙 비대칭 고갈

Direct Memory는 네이티브 자원이므로 GC가 직접 수거하지 못한다. JVM은 Direct Buffer 생성 시 Java Heap 내부에 **`java.lang.ref.Cleaner` 객체(PhantomReference의 일종)** 를 짝지어 매핑해 둔다.

**이원적 해제 구조**

```
[Direct Buffer 해제 라이프사이클]

ByteBuffer.allocateDirect() 호출
        │
        ├── Native Memory 할당 (네이티브 공간)
        │
        └── Cleaner 객체 생성 (Java Heap 내부)
                │
                ▼ (GC가 Cleaner 객체를 수거할 때에만)
        JNI → C 런타임 free() 호출
                │
                ▼
        Native Direct Buffer 해제
```

**힙·오프힙 비대칭 고갈 패턴**

대용량 물리 메모리를 장착한 현대 핀테크 시스템에서 Java Heap을 넉넉하게 설정하고 Generational ZGC를 가동하는 경우:

| 상태 | Java Heap | Direct Memory |
|---|---|---|
| 정상 운영 | 여유 충분 | 정상 |
| **비대칭 고갈** | **여유 충분 (GC 미발생)** | **급격히 포화** |
| 결과 | GC 트리거 없음 → Cleaner 미실행 | Native Buffer 해제 불가 → OOM |

Heap 내부 압박이 없어 GC가 장시간 발생하지 않는 동안, Netty 기반 결제 API 게이트웨이는 수백만 건의 대형 금융 메시지를 Direct Buffer로 생성해 낸다. **Heap은 평온한데 Direct Memory만 물리 한계점까지 포화**되는 메모리 비대칭 고갈이 발생한다.

---

### 3-3. `Bits.java` 임계 검증선 붕괴와 OOM 예외 투사

JVM은 Direct Memory의 무한 증식을 막기 위해 `-XX:MaxDirectMemorySize` 플래그로 지정된 상한선을 `java.nio.Bits` 클래스의 내부 카운터로 실시간 체크한다.

**신규 Direct Buffer 할당 시 JVM 내부 분기 흐름**

```
ByteBuffer.allocateDirect() 호출
        ↓
Bits.java 내부 카운터 확인
        ↓
[카운터 < MaxDirectMemorySize]     [카운터 ≥ MaxDirectMemorySize]
        ↓                                       ↓
  정상 Native 할당                    System.gc() 강제 호출 (최후 수단)
                                              ↓
                              [DisableExplicitGC 설정 없음]  [DisableExplicitGC 설정됨]
                                              ↓                        ↓
                                    Cleaner 객체 수거 →          System.gc() 무시
                                    Native Buffer 해제            → 자원 0 회수
                                              ↓                        ↓
                                       재할당 시도              OutOfMemoryError:
                                                               Direct buffer memory
```

**`-XX:+DisableExplicitGC`의 치명적 부작용**

무분별한 Full GC 방어를 위해 `-XX:+DisableExplicitGC`를 설정하면, Bits.java가 호출하는 `System.gc()`가 완벽히 무시된다. JVM은 자원을 한 바이트도 회수하지 못하고 `OutOfMemoryError: Direct buffer memory` 예외를 투사하며 전체 트랜잭션 라인을 즉각 파괴한다.

---

### 3-4. Netty `PooledByteBufAllocator`의 버퍼 해제 누락 패턴

```java
// ❌ 누수 패턴: release() 누락
ByteBuf buf = allocator.buffer();
// buf.release(); ← 이 호출이 없으면 Native Memory 영구 점유
```

Netty의 `PooledByteBufAllocator`는 스레드 로컬 캐시와 메모리 슬랩(Slab) 기반으로 버퍼를 풀링한다. `release()`를 누락하면 해제되지 않는 네이티브 누수가 누적되다가 불시에 시스템이 크래시된다.

---

## 4. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026)

### 4-1. 과거 기저 기술의 취약성 (JDK 8~11)

과거 세대 시스템들은 `-XX:MaxDirectMemorySize` 플래그 설정을 생략하여 Direct Memory 최대치가 Java Heap 최대치(`-Xmx`)와 강제 연동되었다. 이 구조에서는:

- Netty `PooledByteBufAllocator` 가동 시 스레드 로컬 캐시 오염이나 메모리 슬랩 단편화로 인해 해제되지 않는 네이티브 누수가 누적
- 불시에 컨테이너 전체가 크래시되는 리스크를 상시 내포
- Heap 지표는 정상이므로 기존 모니터링 체계로는 예측 불가능

---

### 4-2. Project Panama (Foreign Function & Memory API) 패러다임 전환 (2026 현재)

JDK 21에서 표준화되고 JDK 25 LTS를 거쳐 2026년 현재 고성능 금융 모듈에 전면 도입된 **Project Panama(Foreign Function & Memory API)** 가 기존의 불안정한 `ByteBuffer.allocateDirect()` 체제를 고속 대체하고 있다.

**핵심 아키텍처 혁신: 결정론적(Deterministic) 해제**

```java
// Panama API: try-with-resources로 결정론적 해제 보장
try (Arena arena = Arena.ofConfined()) {
    MemorySegment segment = arena.allocate(1024);
    // 금융 데이터 처리
    // ...
} // ← try 블록 종료 즉시 Native Memory 즉시 해제 (GC/Cleaner 무관)
```

| 비교 항목 | `ByteBuffer.allocateDirect()` | Panama `MemorySegment` |
|---|---|---|
| 해제 주체 | GC + Cleaner (비결정론적) | `Arena.close()` (결정론적) |
| 해제 시점 | GC 발생 시 | try 블록 종료 즉시 |
| 힙·오프힙 비대칭 위험 | **있음** | **원천 소거** |
| 안전성 | GC 주기에 의존 | 컴파일 타임 + 런타임 보장 |

Panama 아키텍처는 GC와 Cleaner 메커니즘에 네이티브 메모리 해제를 구걸하지 않고, `Arena`가 닫히는 즉시 **결정론적으로 네이티브 메모리를 즉시 파괴**한다. 이로 인해 힙·오프힙 비대칭으로 유발되던 전통적인 Direct Memory Exhaustion 장애 리스크가 원천 소거되어, 2026년 현재 금융권 최상위 네이티브 메모리 인프라 표준으로 자리잡고 있다.

---

## 5. 관측성(Observability) 및 트러블슈팅 런북

### 5-1. SRE 실시간 탐지 지표 매트릭스 (Prometheus / Micrometer)

| 지표 | 감시 대상 | 핵심 경보 발령선 |
|---|---|---|
| `jvm_buffer_memory_used_bytes{id="direct"}` | JVM이 인지하는 실제 Direct Memory 점유량 | `MaxDirectMemorySize`의 **90% 초과** 시 고갈 임박 경보 |
| `jvm_buffer_count_buffers{id="direct"}` | 현재 활성 네이티브 Direct Buffer 총 개수 | 트래픽 유휴 상태로 리턴 후에도 **버퍼 개수 고착화** 시 네이티브 누수 확정 |
| `container_memory_working_set_bytes` | Cgroup 인지 실질 물리 메모리 | `working_set / memory.max > 0.90` 고착화 시 OOM Killer 임박 |
| `process_resident_memory_bytes` | JVM 프로세스 RSS | Heap Used 정상인데 RSS 지속 증가 시 Direct Memory 누수 의심 |

---

### 5-2. 프로덕션 노이즈 최소화 진단 명령 명세

#### ① NMT(Native Memory Tracking) Direct Memory 세그먼트 스캔

Direct Memory 고갈로 인한 OOM 예외가 관측되거나 Cgroup RSS 압박이 심화될 때 JVM 내부 메모리 지도를 무정지로 추출한다.

**사전 조건**: JVM 기동 플래그에 `-XX:+NativeMemoryTracking=detail` 활성화 필요

```bash
# 대상 결제 JVM(PID 404)의 네이티브 메모리 상세 분석
jcmd 404 VM.native_memory detail
```

**결과 판독 예시**

```
-                    Internal (reserved=2105MB, committed=2105MB)
                       (mmap: reserved=2048MB, committed=2048MB)
                       (Tracking id=0x00007f8a9c000000)
                       [Direct Byte Buffer Allocation Source Root]
```

`Internal` 및 `Other` 카테고리, 특히 `Direct Byte Buffer` 세그먼트의 `reserved` 및 `committed` 수치가 `-XX:MaxDirectMemorySize` 한계선에 수렴하고 있음을 확인하는 즉시 **Direct Memory 완전 전소 장애로 확정** 판정한다.

---

#### ② Netty Leak Detection 레벨 격상으로 소스코드 저격

Netty 기반 애플리케이션에서 버퍼 해제 누수를 잡기 위해 기동 플래그에 탐지 레벨을 격상한다.

```bash
# 기동 플래그 추가 (롤링 배포 적용)
-Dio.netty.leakDetection.level=PARANOID
```

**탐지 레벨별 동작**

| 레벨 | 샘플링 비율 | 성능 오버헤드 | 용도 |
|---|---|---|---|
| `DISABLED` | 0% | 없음 | 비권장 |
| `SIMPLE` | 1% | 최소 | 프로덕션 기본값 |
| `ADVANCED` | 1% | 낮음 | 누수 위치 추적 |
| `PARANOID` | 100% | 높음 | **누수 원인 코드 완전 저격** |

`PARANOID` 레벨은 모든 버퍼 할당 건을 샘플링 추적하여 누수 발생 즉시 표준 에러 로그에 다음을 인쇄한다.

```
LEAK: ByteBuf.release() was not called before it's garbage-collected.
Recent access records:
  #1: Created at:
    com.fintech.payment.gateway.PaymentHandler.processRequest(PaymentHandler.java:42)
    ...
```

SRE 팀은 이 Stack Trace를 확보하여 `release()` 누락 위치를 즉각 특정한다.

---

### 5-3. 장애 복구 런북 (Remediation)

#### 1단계: 트래픽 격리

`Direct buffer memory` 고갈을 던지기 시작한 Pod는 정상적인 금융 통신 인터페이스가 완전 마비된 상태다. 즉각 로드밸런서 타깃 그룹에서 **가중치 0으로 차단(Isolate)** 처리한다.

#### 2단계: 긴급 런타임 설정 보정

원인이 `-XX:+DisableExplicitGC`와 Cleaner 해제 지연의 결착으로 판명된 경우:

```bash
# 제거할 플래그
-XX:+DisableExplicitGC

# 대체 플래그 (긴급 롤링 배포 적용)
-XX:+ExplicitGCInvokesConcurrent
```

`-XX:+ExplicitGCInvokesConcurrent`는 `System.gc()` 호출을 수용하되, 비즈니스 스레드를 멈추는 Full GC STW 대신 백그라운드 동시성 GC(ZGC)가 Cleaner 객체를 부드럽게 청소하도록 승화시킨다.

#### 3단계: 근본 원인 코드 수정

| 누수 패턴 | 수정 조치 |
|---|---|
| `ByteBuf.release()` 누락 | `try-finally` 블록에서 `release()` 강제 실행 |
| 미래 전환 (JDK 21+) | `ByteBuffer.allocateDirect()` → Panama `MemorySegment` + `Arena` |
| `-XX:MaxDirectMemorySize` 미설정 | Heap과 별도로 명시적 상한선 설정 |

---

## 6. 2026 Production Baseline JVM Manifest

2026년 현재 초고속 비동기 금융 네트워크 통신 노드의 Direct Memory 고갈 및 커널 크래시를 물리적으로 예방하기 위해 확정된 고가용성 기동 플래그 매니페스트다.

```bash
java \
  # ── 힙 크기 고정: 동적 확장 레이턴시 서지 원천 차단 ─────────────────────────────
  -Xms12g -Xmx12g \
  -XX:+AlwaysPreTouch \

  # ── Generational ZGC: 초저지연 세대별 수거 엔진 고착 ────────────────────────────
  -XX:+UseZGC \
  -XX:+ZGenerational \

  # ── Direct Memory 물리 격벽: Cgroup OOM Killer 간접 도화선 차단 ─────────────────
  -XX:MaxDirectMemorySize=4g \

  # ── System.gc() 동기 블로킹 제거 및 동시성 Cleaner 실행 허용 ─────────────────────
  -XX:+ExplicitGCInvokesConcurrent \

  # ── Off-Heap 메타데이터 상한 격벽: Cgroup OOM Killer 차단선 ─────────────────────
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  -XX:ReservedCodeCacheSize=512m \

  # ── OOM 시 바이너리 증거 자동 확보 및 JVM 자살 조항 ─────────────────────────────
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/jvm_dumps/direct_mem_oom.hprof \
  -XX:OnOutOfMemoryError="kill -9 %p" \

  # ── 관측성 확보: 무정지 추적 파이프라인 탑재 ────────────────────────────────────
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+NativeMemoryTracking=detail \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc_direct.log:time,uptime,pid:filecount=10,filesize=100M \
  -jar payment-async-gateway.jar
```

### 핵심 플래그 공학적 조항 분석

| 플래그 | 목적 | 공학적 근거 |
|---|---|---|
| `-Xms12g -Xmx12g` | 힙 크기 고정 | 동적 확장 시 OS 메모리 요청 지연 제거 |
| `-XX:+AlwaysPreTouch` | 힙 물리 페이지 선점 | 기동 시 전체 힙 즉시 물리 커밋 → 런타임 Page Fault 차단 |
| `-XX:+ZGenerational` | Generational ZGC 활성화 | Young Generation 고속 수거, 1ms 이하 STW 유지 |
| `-XX:MaxDirectMemorySize=4g` | Direct Memory 물리 격벽 | 미설정 시 `-Xmx`(12GB)까지 무한 확장 → Cgroup Limit 초과 → SIGKILL. 설정 시 포화 시 JVM 단에서 `OOM: Direct buffer memory` 예외를 안전하게 투사하여 힙 덤프 골든타임 확보 |
| `-XX:+ExplicitGCInvokesConcurrent` | `DisableExplicitGC` 완전 대체 | `System.gc()` 호출을 수용하되 Full GC STW 대신 백그라운드 ZGC 동시성 수거로 승화 → Cleaner 객체 제때 청소 → 네이티브 버퍼 공간 신속 환수 |
| `-XX:MaxMetaspaceSize=512m` | Metaspace 상한 격벽 | 미설정 시 Native 메모리 무제한 징발 → Cgroup OOM Killer 간접 도화선 차단 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 시 힙 덤프 자동 생성 | 커널 OOM Killer 작동 전 사후 분석 데이터 확보 |
| `-XX:OnOutOfMemoryError="kill -9 %p"` | OOM 즉시 자살 | 힙 덤프 인쇄 완료 직후 자기 사살 → 좀비 프로세스 방어 → K8s Self-Healing 연동 |
| `-XX:+NativeMemoryTracking=detail` | NMT 상세 모드 활성화 | `summary` 대비 Direct Buffer 세그먼트별 할당 소스 추적 가능 (프로덕션 오버헤드 약 5~10%) |

---

## 운영 원칙 요약

> **Direct Memory Exhaustion은 Heap 사용률만으로는 탐지할 수 없다.**
>
> Heap 지표가 완벽히 정상이어도 발생하는 장애이므로, Direct Memory와 RSS를 **Heap과 완전히 독립된 별도 자원**으로 관리해야 한다.

**필수 동시 감시 영역**

- `jvm_buffer_memory_used_bytes{id="direct"}` — Direct Memory 사용량
- `jvm_buffer_count_buffers{id="direct"}` — Direct Buffer 개수 추세
- `process_resident_memory_bytes` — RSS (Heap + 모든 Off-Heap 합산)
- `container_memory_working_set_bytes` — Cgroup 인지 물리 메모리
- Memory Pressure / Page Fault 빈도
- Netty Buffer Lifecycle (`leakDetection.level`)

**Direct Memory 장애 분석 접근 순서**

```
Direct Memory 사용량 확인 (jvm_buffer_memory_used_bytes)
        ↓
RSS 증가 추세 확인 (Heap Used 정상 여부 교차 검증)
        ↓
NMT 분석 (jcmd VM.native_memory detail)
        ↓
Netty Leak Detection 활성화 (PARANOID 레벨)
        ↓
Stack Trace로 ByteBuf.release() 누락 위치 확정
        ↓
코드 수정 또는 Panama MemorySegment 전환 배포
```

Fin-Tech 결제 시스템에서는 **`-XX:MaxDirectMemorySize`를 Heap과 별도로 명시적으로 설정**하고, **`-XX:+ExplicitGCInvokesConcurrent`로 Cleaner 해제 파이프라인을 안전하게 유지**하는 것이 Direct Memory Exhaustion 방어의 핵심 기준이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*