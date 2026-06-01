# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime Heap Leak 장애 분석 가이드

## JVM 기반 금융 결제 시스템을 운영하는 SRE / Platform Engineer
### Linux Kernel (Cgroup v2), Kubernetes, JDK 21+, Generational ZGC

> 정독: 0회

## 목차

1. [Heap Leak 전체 구조](#1-heap-leak-전체-구조)
2. [하부 인프라 및 OS 커널 계층의 Heap Leak 연동 기전](#2-하부-인프라-및-os-커널-계층의-heap-leak-연동-기전)
3. [JVM 내부 서브시스템별 Heap Leak 유발 역학](#3-jvm-내부-서브시스템별-heap-leak-유발-역학)
4. [기술 발전 및 GC 아키텍처 채택 동향 (2026)](#4-기술-발전-및-gc-아키텍처-채택-동향-2026)
5. [관측성(Observability) 및 트러블슈팅 런북](#5-관측성observability-및-트러블슈팅-런북)
6. [2026 Production Baseline JVM Manifest](#6-2026-production-baseline-jvm-manifest)

---

## 1. Heap Leak 전체 구조

### 정의

**Heap Leak**은 애플리케이션에서 더 이상 사용되지 않는 객체가 GC(Garbage Collection)에 의해 회수되지 못하고 지속적으로 Java Heap 메모리를 점유하는 현상이다.

> Heap Leak은 단순한 애플리케이션 메모리 문제가 아니다. JVM → Container Runtime → Linux Kernel 전 계층에 영향을 미치는 복합 장애 유형이다.

Heap Leak이 지속되면 다음 증상이 복합적으로 나타난다.

- Heap 사용량 지속 증가 → Old Generation 포화
- GC 빈도 및 처리 시간 증가
- 결제 트랜잭션 응답시간 급등
- `OutOfMemoryError: Java heap space` 발생
- Container OOM → Exit Code 137 → 서비스 재시작

### 메모리 계층 전사 경로

```
Application (Heap Leak 발생)
        │
        ▼
Java Heap 사용량 증가
        │
        ▼
JVM Process RSS 증가  (물리 페이지 매핑 확장)
        │
        ▼
Container Memory 사용량 증가  (Cgroup memory.current 상승)
        │
        ▼
Linux Kernel Memory Pressure 가중
        │
        ▼  (한계 돌파 시)
OOM Killer → SIGKILL (Exit Code 137)
```

### 정상 상태 vs Heap Leak 상태

| 구분 | 정상 상태 | Heap Leak 상태 |
|---|---|---|
| 객체 생명주기 | 생성 → 비즈니스 처리 → 참조 제거 → GC 회수 | 생성 → 비즈니스 처리 종료 → **참조 유지** → GC 회수 불가 |
| Heap 사용량 | GC 후 일정 수준으로 복귀 | GC 후에도 저점이 우상향 |
| Old Generation | 안정적 유지 | 지속 증가 → 포화 |
| Live Data Size | 안정적 유지 | 매 사이클 계단식 증가 |

---

## 2. 하부 인프라 및 OS 커널 계층의 Heap Leak 연동 기전

JVM 관리형 가상 메모리 공간(Java Heap) 내부에서 발생한 객체 누수가 호스트 OS 커널 및 물리 인프라 계층으로 전사되어 하부 시스템을 파괴하는 단계별 물리적 역학 관계다.

```
+-------------------------------------------------------------------------------------+
| [Container / Pod]  memory.max (Cgroup v2 격리 장벽)                                   |
|                                                                                     |
|   +-------------------------------------------------------------------------+       |
|   | [JVM Process 가상 주소 공간 (VMA)]                                         |       |
|   |                                                                         |       |
|   |  ┌──────────────────────────────────┐   ┌──────────────────────────┐    |       |
|   |  │          Java Heap               │   │       Off-Heap           │    |       |
|   |  │                                  │   │                          │    |       |
|   |  │  [Live Objects]   [Heap Leak]    │   │  Metaspace               │    |       |
|   |  │  (정상 비즈니스)  (GC 수거 불가)       │   │  Thread Stacks           │    |       |
|   |  │       │               │          │   │  Direct Byte Buffer      │    |       |
|   |  │       ▼               ▼          │   │  JIT Code Cache          │    |       |
|   |  │  정상 수거/유지   영구 잔존            │   │                          │    |       |
|   |  └────────┬──────────────┬──────────┘   └──────────────┬───────────┘    |       |
|   |           │              │                             │                 |       |
|   +-----------┼──────────────┼─────────────────────────────┼─────────────────+       |
|               ▼              ▼                             ▼                        |
|         Active(Anon)   Inactive(Anon)               Anonymous Pages                 |
|               │              │                             │                        |
|               +-─────────────┴──────────────┬─────────────+                        |
|                                             ▼                                       |
|                            [Linux Kernel Memory Subsystem]                          |
|                              kswapd / Direct Reclaim 가동                            |
|                              vm.swappiness → Heap 페이지 Swap Out                    |
|                                             │                                       |
|                                             ▼  (물리 한계 돌파 시)                      |
|                            [Cgroup OOM Killer] → SIGKILL (Exit Code 137)            |
+-------------------------------------------------------------------------------------+
```

### 2-1. Heap Leak → RSS 확장 → Cgroup v2 압착

JVM 기동 시 `-Xms` / `-Xmx`로 선언된 공간은 일차적으로 가상 메모리 영역(VMA)에 등록된다. Heap Leak이 발생하여 누수 객체가 메모리를 점유하고 쓰기 연산이 지속되면, 커널은 해당 가상 주소에 실제 물리 메모리 페이지를 할당하며 프로세스의 **RSS(Resident Set Size)를 밀어 올린다.**

Cgroup v2 환경에서 `memory.current` 지표는 JVM RSS와 커널 페이지 캐시를 합산하여 `memory.max`와의 임계 오프셋을 실시간 감시한다. 힙 내부 누수가 가속화될수록 컨테이너 전체 물리 메모리 점유량이 한계선으로 압착된다.

---

### 2-2. Active/Inactive 페이지 전이와 디스크 I/O 지연 유발

Heap Leak으로 수거되지 않는 객체들이 증가하면, 커널은 이 객체들이 매핑된 물리 페이지들을 **오랜 시간 참조된 페이지로 오인**하여 Active(Anon) 리스트에 장기 고착화한다.

호스트 메모리 압박이 심화되어 커널 워터마크가 `WMARK_LOW` 이하로 떨어지면 kswapd가 기동되어 유휴 힙 영역으로 오인된 Anonymous Pages를 디스크 스왑 공간으로 배출(Swap Out)한다.

이후 GC가 누수 객체 추적을 위해 힙 전역을 마킹 스캔하는 순간:

```
나노초 단위 메모리 버스 액세스
        ↓
디스크로 배출된 페이지 재로드 요구 (Major Page Fault 동시다발 폭증)
        ↓
수십 ms 단위 디스크 I/O 대기로 전이
        ↓
GC 스레드 + 비즈니스 스레드 동반 블로킹
        ↓
결제 트랜잭션 SLA 위반 → 연쇄 타임아웃
```

---

### 2-3. Cgroup OOM Killer 발동과 JVM의 무력화

Direct Reclaim으로도 Cgroup 한계선을 방어하지 못하는 극단적 상황에서 커널의 OOM Killer가 개입한다. `/proc/<pid>/oom_score_adj` 수치 기반으로 배드 포인트 점수가 가장 높은 JVM 프로세스에 **SIGKILL(Exit Code 137)** 을 투사한다.

> **주의**: 이는 JVM의 `try-catch`, 힙 덤프 생성 메커니즘이 절대 개입할 수 없는 **OS 레벨 강제 종료**다. 애플리케이션 로그에 OOM 예외 문구 없이 시스템이 완전 소멸되므로, 사전 진단 데이터 확보가 필수다.

---

## 3. JVM 내부 서브시스템별 Heap Leak 유발 역학

### 3-1. GC Reachability 판정 원리와 Heap Leak의 공학적 정의

GC는 **Reachability Analysis** 알고리즘으로 객체 생존 여부를 판정한다.

**GC Root 유형**

| GC Root 종류 | 예시 |
|---|---|
| Thread Stack (로컬 변수) | 현재 실행 중인 스레드의 스택 프레임 내 변수 |
| Static Field | 클래스로더가 로딩한 정적 필드 |
| JNI 네이티브 글로벌 포인터 | JNI 코드에서 보유한 참조 |

```
GC Root (정적 필드, 스레드 스택 등)
   │
   ├── Object A  ──► Object B  ──► Object C   ← Reachable (수거 불가)
   │
   └── (참조 체인 단절)  Object D              ← Unreachable (수거 대상)
```

**Heap Leak의 공학적 정의**

논리적으로 수명이 다해 비즈니스에 쓰이지 않는 객체(Dead Object)이지만, 소스코드의 설계적 오류로 인해 활성 GC Root와의 참조 체인이 끊어지지 않아 **GC가 Reachable 객체로 판단, 영구히 메모리에 방치하는 현상**이다.

---

### 3-2. 핀테크 특화 Heap Leak 핵심 유발 패턴

#### ① 전역 정적 컬렉션(Static Collection)의 금융 세션 점유

```java
public class SessionCache {
    // Static Field = 영구 GC Root
    private static final Map<String, PaymentContext>
        CACHE = new ConcurrentHashMap<>();
}
```

**누수 발생 경로**: 결제 라이프사이클 마감 시점에 `CACHE.remove(transactionId)` 또는 `clear()` 처리를 누락할 경우 발생한다.

Static Field는 클래스로더가 메모리에 상주하는 한 그 자체가 **영구적인 GC Root**로 작동한다. 결제 트래픽 수백만 건이 인입될 때마다 유령 객체들이 Old Generation에 계단식으로 누적되며 힙 가용 용량을 영구 점유한다.

---

#### ② ThreadLocal 자원 해제 누락과 스레드 풀 재사용의 결착

```java
// 선언
private static final ThreadLocal<UserTransactionContext>
    TX_CONTEXT = new ThreadLocal<>();

// 요청 처리 시작
TX_CONTEXT.set(new UserTransactionContext(userId));

// ❌ 요청 처리 완료 후 remove() 누락
// TX_CONTEXT.remove();  ← 이 호출이 없으면 누수 발생
```

**풀링 아키텍처의 부작용**

Tomcat, Netty 등 핀테크 서버는 플랫폼 스레드를 `ThreadPoolExecutor`로 풀링하여 재사용한다. `remove()`를 누락하면:

```
스레드 객체 (GC Root)
    └── ThreadLocalMap
            └── Entry[n]
                    └── UserTransactionContext  ← 결제 완료 후에도 영구 박제
```

해당 스레드가 다음 결제 트래픽을 배정받을 때마다 메모리 오염과 함께 힙 누수가 스레드 개수만큼 동시다발적으로 누적된다.

---

#### ③ 불완전한 `equals` / `hashCode` 오버라이딩과 인메모리 캐시 포화

결제 요청 객체(`PaymentRequestKey`)를 `HashMap` 또는 `HashSet`의 키로 사용할 때, `equals()`와 `hashCode()`를 수학적으로 정밀하게 오버라이딩하지 않으면:

```
동일한 결제 요청 키로 조회
        ↓
hashCode 불일치 → 버킷 미탐색 → 새 엔트리로 오인
        ↓
중복 put() 반복 → 캐시 무한 팽창
        ↓
Old Generation 가용 장벽 붕괴
```

데이터가 캐시 공간 내부에 좀비 인스턴스로 무한 적재되며 Heap을 잠식한다.

---

#### ④ Listener 등록 누락

```java
eventBus.register(listener);   // 등록
// ❌ eventBus.unregister(listener);  ← 해제 누락
```

이벤트 버스가 Listener 객체를 강한 참조(Strong Reference)로 보유하므로, 해제하지 않으면 Listener 및 연관 객체 전체가 GC Root 사슬에 묶여 영구 잔존한다.

---

#### ⑤ Virtual Threads 폭증 환경의 Continuation 힙 포화

JDK 21+ Project Loom 환경에서 가상 스레드가 I/O 대기에 진입하면(Unmount), 실행 스택 전체가 Java Heap의 **Continuation 객체**로 이동한다.

**Heap Leak성 전환 조건**

| 조건 | 영향 |
|---|---|
| 가상 스레드 수십만 개 동시 생성 | Continuation 객체 수십만 개 Heap 적재 |
| 외부 핀테크 기관 네트워크 지연 | 대량 가상 스레드 장시간 Unmount 고착 |
| 스레드 로컬 변수에 대용량 JSON 전문 데이터 보유 | Continuation당 메모리 풋프린트 폭증 |

이 가상 스레드들의 컨텍스트 스택 프레임 자체가 거대한 Heap Leak성 자원으로 돌변하여 힙 공간을 순간적으로 전소시키는 최신 아키텍처 특이적 장애 패턴이다.

---

## 4. 기술 발전 및 GC 아키텍처 채택 동향 (2026)

### 4-1. 과거 기저 기술(CMS, Parallel GC)의 Heap Leak 취약성

과거 세대의 GC들은 힙 전역을 한 번에 스캔하는 구조적 한계로 인해, Heap Leak이 발생하면 마킹 스캔 시간이 기하급수적으로 증가하며 **STW가 수 초에서 수십 초까지 선형 비례**하여 늘어나는 치명적 결함을 보였다.

---

### 4-2. G1 GC의 리전 기반 부분 수거와 한계 (중기)

힙을 리전(Region) 단위로 파티셔닝하여 가비지가 가장 많은 리전만 부분 수거하는 방식으로, Heap Leak 발생 시 전체 시스템이 한 번에 결빙되는 리스크를 완화했다.

그러나 Heap Leak이 임계점에 도달해 가용 리전이 고갈되면 **단일 스레드 직렬 수거 모드인 Full GC(Evacuation Failure)** 로 낙마하는 구조적 취약성을 극복하지 못했다.

---

### 4-3. Generational ZGC 표준화와 SRE 관점의 새로운 위험 (2026 현재)

2026년 현재 초저지연 금융 시스템은 **Generational ZGC** 를 메인 GC 엔진으로 영구 고착화했다. Young 세대와 Old 세대를 분리하여 동시성 마킹을 수행하므로, Heap 내부에 심각한 누수가 진행 중이더라도 **STW 자체는 1ms 이하로 영구 동결**시킨다.

**SRE 관점의 핵심 경고: 지표의 왜곡**

| 관측 지표 | Generational ZGC + Heap Leak 상태 |
|---|---|
| GC Pause Time | **1ms 이하 (완벽하게 정상처럼 보임)** |
| Heap Used | 지속 증가 |
| `jvm_gc_live_data_size_bytes` | 매 사이클 저점 우상향 |

과거에는 GC STW 폭증을 보고 Heap Leak을 사전에 감지할 수 있었으나, Generational ZGC 환경에서는 누수가 99%까지 차오르는 순간에도 STW 지표가 완벽히 정상인 것처럼 은폐된다.

> **현대 핀테크 SRE 아키텍처 필수 원칙**: GC 레이턴시가 아닌, **GC 마감 직후 남는 Live Data Size의 저점 추세선**을 장기 감시하는 정량적 관측 시스템 구축이 필수 조건이다.

---

## 5. 관측성(Observability) 및 트러블슈팅 런북

### 5-1. SRE 실시간 Heap Leak 탐지 지표 매트릭스 (Prometheus / Grafana)

| 지표 | 감시 대상 | 핵심 경보 발령선 |
|---|---|---|
| `jvm_gc_live_data_size_bytes` | 메이저 수거 사이클 직후 생존 힙 용량 | **7일 연속 저점 우상향** 시 Heap Leak 자동 확정 |
| `jvm_memory_used_bytes` | 현재 실사용 힙 메모리 총량 | `Used / Committed > 0.94` 돌파 시 즉각 위험 경보 |
| `container_memory_working_set_bytes` | Cgroup 인지 실질 물리 메모리 | `working_set / memory.max > 0.90` 고착화 시 OOM Killer 임박 |
| `jvm_gc_pause_seconds` | GC STW 지연 | Generational ZGC 환경에서는 단독 지표로 Leak 판단 불가 |

---

### 5-2. 프로덕션 노이즈 최소화 진단 명령 명세

#### ① Class Histogram 비교 분석 (무정지 1차 진단)

수십 GB 단위의 대형 힙 환경에서 전체 힙 덤프(`.hprof`)를 즉시 인쇄하면 STW로 서비스가 파괴된다. 시스템 부하가 0.1% 미만인 클래스 히스토그램 스냅샷을 선제 가동한다.

```bash
# 1차 스냅샷 추출
jcmd <PID> GC.class_histogram > /var/log/jvm_dumps/hist_baseline.txt

# 결제 트래픽 인입 1시간 대기 후 2차 스냅샷 추출
jcmd <PID> GC.class_histogram > /var/log/jvm_dumps/hist_after_1h.txt
```

**결과 판독**: 두 파일의 인스턴스 카운트 증분값(Diff)을 대조한다. 원시 배열(`[B`, `[C` 등)을 제외하고 특정 비즈니스 엔티티(예: `com.fintech.payment.model.PaymentContext`)의 개수가 트래픽 처리 후 회수되지 않고 수십만 개 이상 순증했다면 해당 도메인의 Heap Leak을 즉각 확정한다.

---

#### ② 라이브 컨테이너 힙 덤프 확보 (2차 물리적 증거 확보)

히스토그램으로 원인 객체를 좁힌 후, 실제 어떤 GC Root가 해당 객체를 붙잡고 있는지 완벽한 물리적 증거를 확보하기 위해 힙 덤프를 집행한다.

```bash
# live 옵션: 살아있는 객체만 필터링하여 덤프 크기 최소화 및 STW 시간 단축
jcmd <PID> GC.heap_dump /var/log/jvm_dumps/payment_leak_evidence.hprof
```

---

#### ③ Eclipse MAT Dominator Tree 분석

생성된 `.hprof` 파일을 Eclipse MAT에 적재한 후 다음 순서로 분석한다.

```
MAT 분석 경로:
  Dominator Tree  (메모리 점유율 최상위 노드 확인)
        ↓
  "Path to GC Roots → Exclude all phantom/weak/soft references"
        ↓
  참조 사슬 끝의 클래스 명세 확인  →  누수 코드 확정
```

**MAT Dominator Tree 참조 체인 분석 예시**

```
[GC Root: Thread] "http-nio-8080-exec-1" (0x45001a)
   └── java.lang.Thread
          └── threadLocals  java.lang.ThreadLocal$ThreadLocalMap
                  └── table  java.lang.ThreadLocal$ThreadLocalMap$Entry[]
                          └── [4]  java.lang.ThreadLocal$ThreadLocalMap$Entry (0x4501b8)
                                  └── value  com.fintech.payment.context.PaymentSessionContext
                                                    ↑
                                            ★ 누수 원천 확정
```

**분석 도구**

- Eclipse MAT (Memory Analyzer Tool)
- JDK Mission Control (JMC)
- VisualVM

---

### 5-3. 장애 복구 런북 (Remediation)

#### 1단계: 트래픽 격리

Heap 고갈 경보가 발령된 타깃 Pod의 인그레스 게이트웨이 라우팅 가중치를 **즉시 0으로 하향**하여 결제 경로에서 완전 격리한다.

#### 2단계: 사후 분석 데이터 확보

격리된 Pod를 즉각 파괴하기 전, 힙 덤프 명령을 실행하여 바이너리 증거를 안전하게 확보한다.

```bash
jcmd <PID> GC.heap_dump /var/log/jvm_dumps/payment_leak_evidence.hprof
```

#### 3단계: 인프라 자동화 세대교체

Pod를 재시작하여 클린 힙 상태의 신규 인스턴스로 자동 교체(Self-Healing)하고 클러스터 결제 처리 용량을 즉시 복원한다.

#### 4단계: 근본 원인 제거

MAT 분석 결과를 기반으로 소스코드의 참조 누락 패턴을 확정하고 수정한다.

| 누수 패턴 | 수정 조치 |
|---|---|
| Static Collection | 라이프사이클 마감 시점에 `remove()` / `clear()` 명시적 호출 |
| ThreadLocal | `try-finally` 블록에서 `remove()` 강제 실행 |
| Listener | 해제 메서드 `unregister()` 명시적 호출 |
| Cache 키 | `equals()` / `hashCode()` 정밀 재구현 |

---

## 6. 2026 Production Baseline JVM Manifest

2026년 현재 대규모 결제 클러스터에서 Heap Leak 장애를 구조적으로 방어하고 완벽한 관측성을 유지하기 위해 강제 규격화된 기동 옵션 매니페스트다.

```bash
java \
  # ── 힙 크기 고정: 동적 확장에 따른 런타임 레이턴시 서지 원천 차단 ──────────────
  -Xms16g -Xmx16g \
  -XX:+AlwaysPreTouch \

  # ── Generational ZGC: 초저지연 세대별 수거 엔진 고착 ────────────────────────────
  -XX:+UseZGC \
  -XX:+ZGenerational \

  # ── Safepoint TTSP 마킹 지연 블로킹 원천 소거 ───────────────────────────────────
  -XX:+UseCountedLoopSafepoints \
  -XX:SafepointTimeoutDelay=100 \
  -XX:+SafepointTimeout \

  # ── Off-Heap 규격 상한 격벽: Cgroup OOM Killer 간접 도화선 차단 ─────────────────
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  -XX:ReservedCodeCacheSize=512m \

  # ── Heap Leak 고갈 임계 시점의 바이너리 증거 자동 확보 ──────────────────────────
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/jvm_dumps/fintech_heap_leak.hprof \
  -XX:OnOutOfMemoryError="kill -9 %p" \

  # ── 런타임 무정지 진단 파이프라인 통합 로깅 ─────────────────────────────────────
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+NativeMemoryTracking=summary \
  -Xlog:gc*,safepoint=info:file=/var/log/jvm_dumps/gc_leak_trace.log:time,uptime,pid:filecount=10,filesize=100M \
  -jar fintech-payment-service.jar
```

### 핵심 플래그 공학적 조항 분석

| 플래그 | 목적 | 공학적 근거 |
|---|---|---|
| `-Xms16g -Xmx16g` | 힙 크기 고정 | 힙 동적 확장 시 OS 메모리 요청 지연(`mmap` + 물리 매핑) 제거 |
| `-XX:+AlwaysPreTouch` | 힙 물리 페이지 선점 | 기동 시 전체 힙을 즉시 OS 물리 메모리에 커밋하여 런타임 Page Fault 차단 |
| `-XX:+ZGenerational` | Generational ZGC 활성화 | Young Generation 고속 수거, 1ms 이하 STW 유지 |
| `-XX:+UseCountedLoopSafepoints` | Safepoint 도달 보장 | 카운티드 루프 내에서 GC가 Safepoint를 찾지 못해 STW가 장기 지연되는 현상(TTSP) 차단 |
| `-XX:SafepointTimeoutDelay=100` | Safepoint 지연 경보 | 100ms 초과 Safepoint 진입 지연 시 로그 기록으로 블로킹 원인 추적 |
| `-XX:MaxMetaspaceSize=512m` | Metaspace 상한 격벽 | 미설정 시 Native 메모리 무제한 징발 → Cgroup OOM Killer 간접 도화선 차단 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 시 힙 덤프 자동 생성 | 커널 OOM Killer 작동 전 사후 분석 데이터 확보 |
| `-XX:OnOutOfMemoryError="kill -9 %p"` | OOM 즉시 자살 | 힙 덤프 인쇄 완료 직후 JVM 자기 사살 → 좀비 프로세스(포트 열림·헬스체크 통과·결제 처리 불가) 상태 방어 → K8s Self-Healing 연동 |
| `-XX:+NativeMemoryTracking=summary` | NMT 활성화 | 프로덕션 오버헤드 최소화로 Off-Heap 누수 실시간 추적 가능 |

---

## 운영 원칙 요약

> **Heap Leak은 GC 문제로 시작되는 장애가 아니다.**
> 실제 원인은 애플리케이션이 객체 참조를 유지하는 **구조적 설계 문제**이며, Generational ZGC 환경에서는 GC Pause 정상 여부만으로 Leak을 감지할 수 없다.

**Heap Leak 분석 접근 순서**

```
Heap 사용량 확인 (jvm_memory_used_bytes)
        ↓
Live Data Size 추세 확인 (jvm_gc_live_data_size_bytes 7일 저점 추세)
        ↓
Class Histogram 비교 분석 (jcmd GC.class_histogram)
        ↓
Heap Dump 확보 (jcmd GC.heap_dump)
        ↓
MAT Dominator Tree 분석
        ↓
GC Root 참조 체인 추적 → 누수 클래스 확정
        ↓
소스코드 참조 해제 수정 → 배포 검증
```

Fin-Tech 결제 시스템에서는 **GC Pause 시간보다 GC 이후에도 지속적으로 증가하는 `jvm_gc_live_data_size_bytes`의 장기 추세**를 관찰하는 것이 Heap Leak 탐지의 핵심 판정 기준이다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*