# Fin-Tech 결제 시스템 SRE 관점의 JVM Runtime: Class Loader Leak 장애 유형 E2E 명세

> 정독: 0회

## 목차

1. [하부 인프라 및 OS 커널 계층의 Class Loader Leak 연동 기전](#1-하부-인프라-및-os-커널-계층의-class-loader-leak-연동-기전)
2. [JVM 내부 메모리 및 클래스 로딩 서브시스템별 Class Loader Leak 정밀 역학](#2-jvm-내부-메모리-및-클래스-로딩-서브시스템별-class-loader-leak-정밀-역학)
3. [기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)](#3-기술-발전-및-핀테크-아키텍처-채택-동향-2026년-기준)
4. [Class Loader Leak 장애 상황 관측성 및 트러블슈팅 런북](#4-class-loader-leak-장애-상황-관측성-및-트러블슈팅-런북)
5. [2026 Production Baseline: Metaspace 방어 특화 JVM Runtime Manifest](#5-2026-production-baseline-metaspace-방어-특화-jvm-runtime-manifest)

---

## 개요

**Class Loader Leak**은 클래스 로더(Class Loader)가 더 이상 사용되지 않음에도 가비지 컬렉션 대상이 되지 못하여, 해당 클래스 로더가 로딩한 클래스 메타데이터와 관련 객체들이 지속적으로 메모리에 유지되는 현상이다.

일반적인 Heap Memory Leak과 달리, **Metaspace 및 Native Memory 영역을 지속적으로 잠식**한다는 점이 핵심 특징이다. 특히 동적 클래스 로딩, 플러그인 구조, 애플리케이션 재배포 환경에서 주의가 필요하며, 장시간 운영되는 JVM 프로세스에서 누적되어 결국 커널 레벨의 프로세스 강제 종료(SIGKILL)를 유발한다.

### 대표 증상

| 계층 | 증상 |
|------|------|
| JVM | Metaspace 사용량 지속 증가, Loaded Class Count 증가, Full GC 빈도 증가 |
| 애플리케이션 | `OutOfMemoryError: Metaspace`, `OutOfMemoryError: Compressed class space` |
| 컨테이너 / OS | Container RSS 지속 증가, OOM Killer 발생 (Exit Code 137) |

---

## 1. 하부 인프라 및 OS 커널 계층의 Class Loader Leak 연동 기전

JVM 클래스 로더 누수는 단순한 자바 객체 누수를 넘어, 가상머신이 OS 커널로부터 할당받은 **네이티브 메모리 영역(Native Memory)을 무한히 잠식**하여 시스템 전체를 파괴하는 기전을 가진다.

```
+-----------------------------------------------------------------------------------------+
| [Host / Container Space] memory.max (Cgroup v2 격리 경계선)                                |
|                                                                                         |
|   +---------------------------------------------------------------------------------+   |
|   | [JVM Process 가상 주소 공간 (VMA)]                                               |   |
|   |                                                                                 |   |
|   |   +-----------------------+  +----------------------------------------------+   |   |
|   |   |       Java Heap       |  |                 Metaspace                    |   |   |
|   |   |     (-Xms / -Xmx)     |  |          (-XX:MaxMetaspaceSize)              |   |   |
|   |   |                       |  |                                              |   |   |
|   |   |   [AppClassLoader]    |  |  [Klass 구조체] -> [Method Metadata]         |   |   |
|   |   |         ▲             |  |  [Constant Pool] -> [Bytecode]               |   |   |
|   |   |         │ 강한 참조    |  |                                              |   |   |
|   |   |   [CustomClassLoader] |  |  - High-Watermark 도달 -> Full GC 트리거      |   |   |
|   |   +-----------------------+  +----------------------┬-----------------------+   |   |
|   |                                                     │                           |   |
|   +-----------------------------------------------------┼---------------------------+   |
|                                                         │                               |
|                                                         ▼                               |
|                                               [Native Memory (Malloc)]                  |
|                                                         │                               |
|                                                         ▼ (회수 불능 시)                  |
|                                         [Linux Kernel Cgroup OOM Killer]                |
|                                           - SIGKILL (Exit Code 137) 투사                |
+-----------------------------------------------------------------------------------------+
```

### 1-1. 가상 메모리 주소 공간(VMA) 내 Metaspace 세그먼트와 네이티브 메모리 할당

**오프힙 메타데이터 이주**

JVM은 클래스 파일의 바이너리 데이터(메서드 메타데이터, 런타임 상수 풀, 어노테이션 등)를 자바 힙이 아닌 OS가 관장하는 네이티브 메모리 영역인 **Metaspace**에 할당한다. 클래스 로더가 기동될 때 JVM은 glibc 런타임의 `malloc(3)` 또는 커널 시스템 콜 인터페이스를 통해 익명 가상 메모리 영역(VMA)을 확보한다.

**물리 메모리(RSS)의 영구 임계치 상승**

클래스 로더 누수가 발생하면 메타데이터가 네이티브 메모리를 끝없이 잠식하여, 프로세스의 RSS(Resident Set Size) 점유율이 지속적인 우상향 그래프를 그린다.

### 1-2. Metaspace High-Watermark 작동 및 커널 메모리 단편화

**High-Watermark 동적 제어**

JVM은 Metaspace 크기가 동적으로 계산된 High-Watermark에 도달하면, 메타데이터를 정돈하기 위해 가비지 컬렉션을 강제 트리거한다.

**네이티브 할당자의 단편화 병목**

Metachunk 할당 메커니즘과 하부 glibc malloc 메커니즘 간의 불일치로 인해, 클래스 로더가 해제되더라도 오프힙 주소 공간이 OS 커널로 즉각 반환되지 않는 **메모리 단편화(Memory Fragmentation)** 가 누적된다. 가상머신이 내부적으로는 가용 공간이 있다고 판단함에도 불구하고, 커널 레벨에서는 물리 메모리가 지속적으로 소모되는 괴리가 발생한다.

### 1-3. Cgroup v2 OOM Killer 진입과 JVM 프로세스 강제 종료 (SIGKILL)

**격리 경계 붕괴**

최대 힙 크기(`-Xmx`)를 컨테이너 사양보다 낮게 제약하더라도, 클래스 로더 누수로 인해 Metaspace 영역이 상한 없이 폭증하면 결국 Cgroup v2가 통제하는 `memory.max` 경계선을 돌파한다.

**침묵의 프로세스 사살**

커널은 호스트 시스템의 안정성을 보존하기 위해 해당 JVM 프로세스에 가로챌 수 없는 시그널인 **SIGKILL (Exit Code 137)** 을 직접 투사한다. 애플리케이션 내부의 에러 핸들러 및 결제 완결성 인터페이스를 완전히 무시하고 프로세스를 종료하므로, 분산 트랜잭션의 정합성이 파괴되는 극한의 장애 상황을 야기한다.

---

## 2. JVM 내부 메모리 및 클래스 로딩 서브시스템별 Class Loader Leak 정밀 역학

### 2-1. 클래스 언로딩(Class Unloading) 삼위일체 조건

JVM 스펙상 클래스 로더가 로딩한 클래스 메타데이터가 Metaspace에서 물리적으로 해제(Unloading)되려면 다음 세 가지 조건이 **동시에** 충족되어야 한다.

```
조건 1: 해당 클래스 로더가 로딩한 모든 클래스의 인스턴스가 힙 내에 존재하지 않을 것 (Instance Count = 0)
         +
조건 2: 해당 클래스 로더가 로딩한 모든 java.lang.Class 객체가 힙 내부에서 참조되지 않을 것
         +
조건 3: 해당 클래스 로더 객체 자체가 GC Root로부터 도달 불가능(Unreachable)한 상태일 것
         ↓
세 조건 모두 충족 시 → Class Unloading → Metaspace 회수
```

**상호 참조의 족쇄**

자바의 모든 객체 헤더는 자신이 속한 클래스(Klass 구조체)를 가리키고, 클래스는 자신을 로딩한 클래스 로더를 역참조(`Class.getClassLoader()`)한다. 따라서 단 하나의 비즈니스 객체 인스턴스나 ThreadLocal 변수가 특정 클래스를 참조하고 있다면, 해당 클래스 로더와 그 로더가 로딩한 **수천 개의 클래스 메타데이터 전체** 가 가비지 컬렉션의 대상에서 영구 제외되는 연쇄 고착화가 성립된다.

### 2-2. 주요 누수 유발 패턴

#### ① ThreadLocal과 Context Class Loader의 결착 누수

Tomcat과 같은 WAS는 웹 애플리케이션 재배포 시 자식 클래스 로더인 `ParallelWebappClassLoader`를 폐기하고 새로 생성한다. 그러나 공용 워커 스레드 내부의 `ThreadLocalMap`에 커스텀 클래스 로더가 로딩한 객체를 적재한 뒤 명시적으로 `ThreadLocal.remove()`를 호출하지 않으면, 강한 참조 체인이 유지된다.

```
강한 참조 체인 구조:
Thread → ThreadLocalMap → Entry → Key/Value → Custom Class → Custom Class Loader
```

재배포가 반복될 때마다 구버전의 클래스 로더들이 Metaspace에 고착되어 누수가 선형적으로 누적된다.

```java
// 문제 패턴
private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

// CONTEXT.set(context) 호출 후 아래를 누락하면 누수 발생
// CONTEXT.remove(); // ← 반드시 finally 블록에서 호출해야 함
```

#### ② Static Registry 누수 (DriverManager 및 레지스트리 고착)

핀테크 데이터베이스 연동을 위해 커스텀 웹 애플리케이션 내부에서 `java.sql.DriverManager.registerDriver()`를 호출하는 패턴이다. `DriverManager`는 Bootstrap ClassLoader에 의해 로딩되므로 영구적인 GC Root이다.

애플리케이션 셧다운 시 `DriverManager.deregisterDriver()`를 명시적으로 호출하지 않으면, 시스템 레지스트리가 자식 클래스 로더의 드라이버 객체를 계속 강하게 참조하므로 자식 클래스 로더 전체가 메모리에 고착된다.

```java
// 문제 패턴: 등록만 하고 해제를 누락
DriverManager.registerDriver(driver);

// 셧다운 훅 또는 ServletContextListener.contextDestroyed()에서 반드시 호출 필요
// DriverManager.deregisterDriver(driver);
```

#### ③ Static Cache 기반 누수

정적 캐시에 클래스 로더가 로딩한 객체를 저장한 뒤 명시적으로 제거하지 않으면, 정적 필드가 GC Root 역할을 하여 클래스 로더가 회수되지 않는다.

```java
// 문제 패턴
private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

// 참조 체인: Static Cache → Business Object → Class → Class Loader
```

#### ④ Dynamic Proxy 환경 (JDK Proxy / CGLIB / ByteBuddy)

런타임에 새로운 클래스를 동적으로 생성하는 프록시 기술(JDK Proxy, CGLIB, ByteBuddy, ASM)은 클래스를 지속적으로 생성하지만 해제가 수행되지 않으면 `Loaded Class Count`와 Metaspace가 지속적으로 증가한다.

### 2-3. Compressed Class Space 포화와 OutOfMemoryError

64비트 JVM은 메모리 효율을 위해 Metaspace 내부에 클래스 메타데이터 주소를 32비트 포인터로 압축하여 참조하는 **Compressed Class Space** 를 독립 운영한다(`-XX:+UseCompressedClassPointers`).

```
JVM Process
│
├─ Metaspace
│    └─ Compressed Class Space (기본 상한: 1GB)
│         └─ 클래스 포인터 압축 저장 영역
```

전체 네이티브 가용 메모리가 넉넉하게 남아있더라도, 클래스 로더 누수로 인해 이 영역의 상한선이 전소되면 JVM은 즉각 연산을 중단하고 다음 에러를 투사한다.

```
java.lang.OutOfMemoryError: Metaspace
java.lang.OutOfMemoryError: Compressed class space
```

---

## 3. 기술 발전 및 핀테크 아키텍처 채택 동향 (2026년 기준)

### 3-1. 레거시 아키텍처 (JDK 7~8 / PermGen / 중량 WAS 동적 재배포)

**PermGen의 물리적 한계**

JDK 7 이전에는 클래스 메타데이터를 자바 힙의 고정 세그먼트인 `PermGen(Permanent Generation)` 영역에 가두어 관리했다. 크기가 고정되어 있어 클래스 로더 누수 발생 시 즉각 시스템이 크래시되었다.

**동적 재배포 아키텍처의 구조적 결함**

JDK 8에서 가변 확장되는 Metaspace로 전환되었으나, 과거 핀테크 시스템은 인프라 자원 절약을 위해 단일 거대 Tomcat/WebLogic 인스턴스 위에 여러 금융 결제 애플리케이션 컨텍스트를 올리고 런타임에 WAR 파일을 동적 재배포(Dynamic Redeployment)하는 방식을 채택했다. 이 방식은 ThreadLocal 및 드라이버 고착화로 인한 클래스 로더 누수를 필연적으로 양산하는 구조적 결함을 안고 있었다.

### 3-2. 마이크로서비스 아키텍처(MSA) 및 Immutable Infrastructure

**동적 배포의 종말**

인프라 단위가 컨테이너(Docker/Kubernetes) 기반의 가벼운 독립형 마이크로서비스로 파편화되면서, 런타임에 클래스 로더를 갈아끼우는 동적 재배포 기전 자체가 완전 도태되었다.

**불변 인프라(Immutable Infrastructure) 사상의 정착**

새로운 소스코드를 배포할 때는 프로세스를 리로드하는 대신, 기존 컨테이너를 통째로 폐기하고 새로 빌드된 이미지를 배포하는 방식이 정착되었다. 이를 통해 전통적인 클래스 로더 누수 시나리오의 상당 부분이 인프라 아키텍처 레벨에서 원천적으로 차단되었다.

### 3-3. GraalVM Native Image 및 런타임 정적 최적화 (2026년 현재)

**클래스 로더 개념의 완전 소멸**

2026년 현재 초고속 결제 및 정산 마이크로서비스 노드들은 **GraalVM Native Image** 아키텍처를 적극 도입하여 운영하고 있다.

GraalVM 네이티브 이미지 빌드 파이프라인은 컴파일 타임에 애플리케이션의 전수 도달 가능성 분석(AOT, Ahead-Of-Time Compilation)을 완료하여 런타임에 필요한 클래스 구조만 단일 바이너리(Executable Binary) 파일로 고착화한다.

```
[기존 JVM 런타임]                    [GraalVM Native Image]
  .class 파일                           단일 실행 바이너리
      ↓                                      ↓
  Class Loader (런타임 동적 로딩)    → Class Loader 서브시스템 자체가 제거됨
      ↓
  Metaspace 오버헤드                 → Metaspace 오버헤드 = 0
```

런타임에 자바 바이트코드를 해석하고 클래스를 동적으로 로딩하는 클래스 로더 서브시스템 자체가 아키텍처 레벨에서 제거됨으로써, 2026년 현재 최고 수준의 핀테크 인프라에서는 **Class Loader Leak이라는 장애 유형 자체가 공학적으로 소멸** 되었으며, 메타스페이스 오버헤드 제로화로 극상의 시동 속도와 저자원 고가용성을 실현하고 있다.

---

## 4. Class Loader Leak 장애 상황 관측성 및 트러블슈팅 런북

프로덕션 클러스터에서 런타임 기동 시간이 수 주 이상 경과함에 따라 Metaspace 점유율이 지속적으로 우상향하여 시스템을 위협할 때 SRE 엔지니어가 가동해야 하는 정밀 진단 런북이다.

### 4-1. SRE 실시간 탐지 임계 지표 매트릭스 (Prometheus)

| 수집 매트릭 식별자 (Prometheus) | 감시 대상 세그먼트 | 핵심 크리티컬 알람 발령선 |
|---|---|---|
| `jvm_memory_used_bytes{area="nonheap", id="Metaspace"}` | JVM이 물리적으로 점유 중인 오프힙 Metaspace 크기 | **Critical**: `-XX:MaxMetaspaceSize` 제한 값의 **92% 초과** 시 발령 |
| `jvm_classes_loaded_classes` | 현재 JVM에 로딩된 총 클래스 수 | 결제 트래픽 유휴 시간대(새벽)로 진입했음에도 수치가 **우하향하지 않고 고착화** 시 Class Loader Leak 판정 |
| `jvm_classes_unloaded_classes` | 언로드된 클래스 수 | Metaspace는 증가하는데 언로드 수가 **0에 수렴** 하면 누수 의심 |
| `container_memory_working_set_bytes` | 컨테이너 실제 사용 메모리 | `memory.limit` 대비 지속 우상향 시 OOM Killer 위험 |

**누수 의심 패턴 vs 정상 패턴 비교**

```
[정상 상태]                          [누수 상태]
Loaded Classes 증가                  Loaded Classes 증가
      ↓                                    ↓
Unloaded Classes 증가                Unloaded Classes 증가 없음 (= 0)
      ↓                                    ↓
Metaspace 안정                       Metaspace 지속 증가
                                           ↓
                                     Full GC 빈도 증가
                                           ↓
                                     응답시간 증가
```

### 4-2. 진단 런북

#### Step 1. Metaspace 사용량 및 Loaded Class Count 확인

```bash
# Prometheus 또는 JMX를 통한 실시간 메트릭 확인
jvm_memory_used_bytes{id="Metaspace"}
jvm_classes_loaded_classes
```

#### Step 2. jcmd를 이용한 Class Loader 통계 및 트리 스캔

Metaspace 고갈 경보 수신 시, 프로세스에 락 오버헤드를 최소화하는 진단 커맨드를 투사하여 로더 계층 구조를 확보한다.

```bash
# 대상 프로세스(PID 909)를 타깃으로 살아있는 클래스 로더 객체들의 인스턴스 통계 및 관계도 추출
jcmd 909 VM.classloader_stats > /var/log/jvm_dumps/classloader_leak_tree.txt
```

**결과 판독**

아웃풋에서 동일한 이름의 커스텀 클래스 로더(예: `PluginClassLoader`, 레거시 프레임워크 로더)가 수십~수백 개 이상 독자적인 주소값을 가진 채 독립된 라인으로 출력되어 있다면, 이전 클래스 로더가 수거되지 못하고 잔존해 있는 **Class Loader Leak 상태** 로 최종 판정한다.

#### Step 3. Heap Dump 주조 및 OQL(Object Query Language) 원인 규명

클래스 로더를 힙 내부에 묶어두고 있는 상위 GC Root의 강한 참조 주소를 규명하기 위해 바이너리 힙 프로파일 덤프를 디스크에 플러시한다.

```bash
# 가상머신 메모리 전역의 참조 관계가 포함된 바이너리 덤프 파일 주조
jcmd 909 GC.heap_dump /var/log/jvm_dumps/metaspace_leak_dump.hprof
```

**Eclipse Memory Analyzer (MAT) OQL 분석**

주조된 `.hprof` 파일을 MAT로 로드한 뒤, OQL 콘솔을 가동하여 잔존하는 유휴 클래스 로더 객체들을 필터링한다.

```sql
/* 시스템에 등록된 임의의 커스텀 클래스 로더 객체 전수 조회 예시 */
SELECT cl FROM instanceof java.lang.ClassLoader cl WHERE cl.@objectAddress != null
```

수천 개의 로더 객체가 검출되면, 특정 로더 인스턴스를 선택하고 **Path To GC Roots → exclude all phantom/weak/soft references** 메뉴를 구동한다. 이를 통해 어떤 공용 플랫폼 스레드의 `ThreadLocalMap` 변수나 정적 싱글톤 레지스트리가 해당 클래스 로더를 강하게 붙잡고 있는지 참조 체인의 종착지(범인 소스코드 변수)를 정밀 적발해 낸다.

**우선 확인 GC Root 목록**

- ThreadLocal 변수
- Static Cache (Map, List 등)
- Driver Registry (DriverManager)
- Plugin Registry

#### Step 4. Class Unloading 로그 점검

```bash
# JVM 시작 옵션에 활성화되어 있어야 함
# -Xlog:class+unload=info:file=/var/log/jvm_dumps/class_unload.log:time,pid:filecount=3,filesize=50M
cat /var/log/jvm_dumps/class_unload.log
```

**판정 기준**: 시스템 배포 및 스케줄러 기동이 반복됨에도 불구하고 본 로그 파일에 **단 한 줄의 언로드 이력이 기록되지 않으면서** Metaspace 점유 메트릭만 우상향하고 있다면, Class Loader Leak 장애 징후를 조기 확진한다.

### 4-3. 장애 복구 런북 (Remediation)

**1단계: 해당 Pod 격리 및 롤링 리스타트**

Class Loader Leak은 JVM 내부 수거 알고리즘으로 해결할 수 없는 영구 고착 상태이다. 임계치에 도달한 Pod 인스턴스는 즉각 로드밸런서 타깃 그룹에서 배제(Isolate) 처리하고, 쿠버네티스 Deployment 단위의 의도적 롤링 리스타트(Rolling Restart)를 단행하여 프로세스를 신속하게 교체한다.

```bash
kubectl rollout restart deployment/fintech-core-settlement -n payment
```

**2단계: 근본 원인 코드 수정**

- ThreadLocal 사용 지점: 반드시 `finally` 블록에서 `ThreadLocal.remove()` 호출
- JDBC 드라이버: `ServletContextListener.contextDestroyed()` 등 셧다운 훅에서 `DriverManager.deregisterDriver()` 호출
- Static Cache: 약한 참조(`WeakReference`, `SoftReference`) 기반 캐시 구조로 전환 검토

---

## 5. 2026 Production Baseline: Metaspace 방어 특화 JVM Runtime Manifest

컴파일 타임 최적화를 적용하기 어려운 동적 자바 환경에서 Class Loader Leak으로 인한 커널 강제 종료(SIGKILL) 리스크를 차단하기 위해 조율 완료된 최종 기동 사양서이다.

```bash
java \
  # [힙 메모리 원천 선점 및 페이지 테이블 물리 매핑]
  -Xms12g -Xmx12g \
  -XX:+AlwaysPreTouch \
  \
  # [초저지연 세대별 ZGC 컬렉터 엔진 탑재]
  -XX:+UseZGC \
  -XX:+ZGenerational \
  \
  # [★ 클래스 메타데이터 오프힙 가상 격벽 사양 확정]
  -XX:MetaspaceSize=512m \
  -XX:MaxMetaspaceSize=512m \
  -XX:CompressedClassSpaceSize=256m \
  \
  # [명시적 GC 호출 허용 및 동시성 처리 안전장치]
  -XX:+ExplicitGCInvokesConcurrent \
  \
  # [클래스 언로딩 진단 로그 활성화 파이프라인]
  -Xlog:class+unload=info:file=/var/log/jvm_dumps/class_unload.log:time,pid:filecount=3,filesize=50M \
  \
  # [임계 고갈 시 자동 힙 덤프 수집 및 프로세스 자살 조항]
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/jvm_dumps/metaspace_oom_dump.hprof \
  -XX:OnOutOfMemoryError="kill -9 %p" \
  -jar fintech-core-settlement.jar
```

### 기동 옵션별 SRE 공학적 조항 분석

| 옵션 | 목적 | 누락 시 리스크 |
|------|------|----------------|
| `-XX:MaxMetaspaceSize=512m` | 클래스 메타데이터가 사용할 수 있는 최대 네이티브 메모리 한계선을 512MB로 강제 제약 | 누락 또는 무제한 방치 시, 누수 발생 시 호스트 Cgroup `memory.max`를 침범하여 커널 OOM Killer가 프로세스를 즉각 사살(SIGKILL) |
| `-XX:MetaspaceSize=512m` | 최초 Metaspace 할당 크기를 지정하여 초기 GC 트리거를 억제 | 기본값이 너무 낮아 초기 기동 시 불필요한 Full GC가 반복될 수 있음 |
| `-XX:CompressedClassSpaceSize=256m` | Compressed Class Space 상한을 명시적으로 제약 | 기본값(1GB)으로 방치 시 포화 감지가 늦어짐 |
| `-Xlog:class+unload=info` | GC 사이클에서 클래스 메타데이터 언로딩 여부를 독립 로그 스트림으로 실시간 추적 | Class Loader Leak 조기 감지 불가. 메타스페이스 점유 증가를 사후에야 인지하게 됨 |
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 발생 시 힙 덤프를 자동 수집하여 사후 원인 분석 골든타임 확보 | 프로세스 종료 후 원인 규명 불가 |
| `-XX:OnOutOfMemoryError="kill -9 %p"` | OOM 발생 시 JVM이 좀비 상태로 잔존하는 것을 방지하고 즉각 프로세스를 종료하여 쿠버네티스 재기동 유도 | 좀비 프로세스가 컨테이너를 점유하며 트래픽을 수신하는 이중 장애 유발 가능 |

> **핵심 원칙**: `-XX:MaxMetaspaceSize`를 통해 사전 제약 방어선을 구축해 두면, 포화 시 JVM이 제어권을 상실하기 직전 `java.lang.OutOfMemoryError: Metaspace` 예외를 자발적으로 던지게 만들며, 동시에 지정된 `-XX:HeapDumpPath`에 따라 바이너리 힙 덤프 파일 수집의 골든타임을 확보할 수 있다.

---

## 부록: 핵심 관측 지표 종합 참조표

### JVM 지표

| 메트릭 | 설명 | 누수 판정 임계 |
|--------|------|----------------|
| `jvm_memory_used_bytes{id="Metaspace"}` | Metaspace 사용량 | `MaxMetaspaceSize`의 92% 초과 |
| `jvm_classes_loaded_classes` | 로드된 클래스 수 | 유휴 시간대 우하향 없이 고착화 |
| `jvm_classes_unloaded_classes` | 언로드된 클래스 수 | Metaspace 증가 중 언로드가 0에 수렴 |

### 컨테이너 / OS 지표

| 메트릭 | 설명 |
|--------|------|
| `container_memory_usage_bytes` | 컨테이너 전체 메모리 사용량 |
| `container_memory_working_set_bytes` | 컨테이너 실제 사용 메모리 (RSS 기반) |
| `process_resident_memory_bytes` | JVM 프로세스 RSS |

### 진단 도구 요약

| 도구 | 커맨드 / 경로 | 목적 |
|------|--------------|------|
| `jcmd` | `jcmd <pid> VM.classloader_stats` | 클래스 로더 인스턴스 수 및 계층 구조 확인 |
| `jcmd` | `jcmd <pid> GC.heap_dump <path>` | 힙 덤프 주조 |
| Eclipse MAT | OQL + Path To GC Roots | GC Root 추적 및 누수 원인 변수 적발 |
| JVM 로그 | `-Xlog:class+unload=info` | 클래스 언로딩 여부 실시간 추적 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*