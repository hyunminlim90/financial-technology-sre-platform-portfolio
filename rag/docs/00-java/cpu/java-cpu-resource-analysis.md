# Java CPU Resource Analysis
# FinTech 결제 시스템 SRE 관점 — CPU 자원 E2E 분석

> 정독: 0회
> 
> 관점: SRE / Platform Engineering / Payment Reliability  </br>
> 범위: Hardware → OS Kernel → JVM Runtime → Framework → Application  </br>
> 목적: Java 기반 결제 시스템에서 CPU 자원이 실제로 소비되는 경로를 계층별로 분석하고, 병목 감지·튜닝·장애 대응 역량을 확보한다.  </br>

## 1. 물리/가상 CPU 스펙 확인 지표

### 1.1 물리 서버 CPU 구조

| 항목 | 확인 명령어 | SRE 분석 포인트 |
|------|-----------|--------------|
| Socket / Physical Core / Logical Core | `lscpu` | HT/SMT 활성화 여부 확인. 활성화 시 L1/L2 캐시를 2개 논리 코어가 공유 → 결제 처리 로직 집약 시 Cache Miss 증가 |
| L1 / L2 / L3 캐시 크기 | `lscpu -C` | L3 캐시가 클수록 Context Switch 시 데이터 재로드 비용 감소. 결제 Hot Path 데이터(환율, 한도)가 캐시에 상주 가능 여부 판단 |
| NUMA 구성 | `numactl -H` | 멀티 소켓 서버에서 JVM Heap이 원격 NUMA 노드 메모리를 참조하면 메모리 접근 지연 2~4배 증가. `numastat`으로 Remote Access 비율 확인 |
| CPU Topology | `lstopo` | NIC와 CPU 소켓 간의 PCIe 버스 위치 확인. NIC와 다른 소켓의 CPU가 IRQ 처리 시 NUMA Remote 접근 발생 |
| CPU 주파수 / Turbo | `cpupower frequency-info` | P-state 상태 확인. 저부하 상태에서 주파수 다운스케일 후 트래픽 급증 시 첫 응답 지연 |
| CPU Architecture | `lscpu \| grep Architecture` | x86_64 vs ARM(Graviton). JIT Intrinsics 지원 범위 차이. Graviton은 AES, CRC32 명령어 지원 여부 확인 필요 |

<details>
<summary>Deep Dive</summary>

</br>

CPU Socket [[M]](../../100-deep-dive/micro-foundations/cpu-socket.md) [C]

</details>

### 1.2 NUMA 메모리 접근 구조

```text
NUMA 노드 구성 예시 (2 Socket):
  Node 0: CPU 0-23, Memory 0-127GB (Local)
  Node 1: CPU 24-47, Memory 128-255GB (Remote)

JVM 프로세스가 Node 0 CPU에서 실행 중
  → JVM Heap이 Node 1 메모리에 할당된 경우
  → 모든 객체 접근이 QPI/UPI 버스를 통한 Remote Access
  → CAS 연산(AtomicLong 등)에서 Cache Coherency 오버헤드 급증

확인:
  numastat -p <pid>       → 프로세스별 NUMA 접근 통계
  numactl --membind=0 ... → Node 0 메모리만 사용 강제
```

### 1.3 가상화 / 컨테이너 환경 CPU 구조

| 항목 | 확인 방법 | SRE 분석 포인트 |
|------|---------|--------------|
| vCPU 구성 | `nproc`, `lscpu` | vCPU는 물리 코어와 1:1 매핑이 아님. 하이퍼바이저 오버커밋 시 실제 처리 성능 차이 |
| CPU Steal Time | `top`의 `%st`, `vmstat` | 하이퍼바이저가 CPU를 다른 VM에 할당한 시간. Steal이 높으면 결제 처리 지연이 발생해도 Java 코드 문제가 아님 |
| cgroup CPU Quota | `cat /sys/fs/cgroup/cpu.max` | K8s CPU Limit 설정값. Quota 소진 시 물리 CPU가 남아도 Throttling 발생 |
| Throttling 상태 | `cat /sys/fs/cgroup/cpu.stat` | `nr_throttled`, `throttled_usec` 지속 증가 시 CPU Limit 부족 |
| CPU Burst | `cat /sys/fs/cgroup/cpu.max.burst` | 순간 피크 허용량. 결제 트래픽 급증 시 Burst로 Throttling 완화 가능 |

```text
cgroup v2 CPU 제어 메커니즘:
  cpu.max = "50000 100000"
    → 100ms 주기 중 50ms만 CPU 사용 허용 (50% Quota)
    → Quota 소진 시 모든 Thread가 Runnable 상태이나 실행 불가
    → CFS (Completely Fair Scheduler)의 cfs_bandwidth 기능

  결제 처리 흐름 중 Throttling 발생 시:
    → Thread가 Runnable 상태로 Run Queue에 대기
    → PSI cpu.some 지표 급등
    → 결제 승인 응답 시간 증가 → Timeout 위험
```

---

## 2. Java 결제 시스템 CPU 실행 흐름 (E2E Execution Path)

```text
[결제 요청 수신 ~ CPU 연산 완료 전체 경로]

1. NIC 패킷 수신
   NIC → DMA → Kernel Ring Buffer (sk_buff)
     → Hardware IRQ 발생 → CPU IRQ 핀 활성화
     → CPU가 현재 실행 중인 작업 중단 → IRQ Handler 실행
     → 빠른 처리 후 SoftIRQ 예약

2. SoftIRQ 처리 (ksoftirqd)
   ksoftirqd 커널 스레드가 CPU를 점유
     → TCP/IP 스택 처리 (IP/TCP 헤더 파싱, Checksum 검증)
     → Socket Receive Buffer에 데이터 적재
     → epoll wait_queue callback 호출

3. EventLoop / Acceptor 깨움
   epoll_wait() 반환
     → Tomcat Acceptor Thread 또는 Netty EventLoop Thread 깨움
     → Scheduler가 해당 Thread를 Run Queue에서 선택
     → Context Switch: 이전 Thread 상태 저장 → 새 Thread 복원

4. JVM User Space 실행
   Thread 실행 (User Mode CPU Time 시작)
     → JVM Bytecode → JIT Native Code 실행
     → L1/L2/L3 Cache Hit/Miss에 따라 실행 속도 결정
     → 결제 비즈니스 로직 처리

5. I/O 호출 시 CPU 상태 전환
   DB 쿼리 / 외부 PG API 호출
     → write() / read() syscall → User Mode → Kernel Mode 전환
     → Kernel이 I/O 요청 처리
     → Thread가 Wait Queue로 이동 (Off-CPU)
     → Scheduler가 다른 Thread 실행

6. 응답 반환
   I/O 완료 IRQ → Thread를 Run Queue로 복귀
     → Scheduler 선택 → Thread 재실행 (Context Switch)
     → 결과 직렬화 (JSON/Protobuf) → CPU 연산
     → write() syscall → Kernel Send Buffer → NIC
```

### 2.1 CPU Mode 전환 비용

| 전환 유형 | 원인 | CPU 비용 |
|---------|------|---------|
| User → Kernel (syscall) | `read()`, `write()`, `futex()`, `epoll_wait()` 호출 | ~100 ns, 레지스터 저장, 스택 전환 |
| Context Switch (voluntary) | I/O 완료 대기, `LockSupport.park()` | ~1~10 μs, TLB Flush, Cache 냉각 |
| Context Switch (involuntary) | Time Slice 만료, CPU 부족 | ~1~10 μs, 예측 불가한 시점 |
| IRQ Handler → User | 패킷 수신, DMA 완료 | CPU 실행 중단 후 재개 |

---

## 3. Java CPU 사용 메커니즘 및 실행 모델 분석

### 3.1 Thread 모델과 CPU 점유

#### Platform Thread (OS Thread)

```text
생성:
  Thread t = new Thread(...) → clone() syscall
  → OS가 task_struct 할당 (커널 스케줄링 단위)
  → Stack 메모리 확보 (기본 1MB, -Xss로 조정)

CPU 점유 방식:
  Runnable 상태 → Scheduler가 Run Queue에서 선택
  → CPU 할당 → Time Slice(기본 ~4ms) 동안 실행
  → I/O / Lock 대기 시 Wait Queue로 이동 (CPU 반납)
  → 완료 시 Run Queue 복귀

문제점:
  Thread 1개 = OS Thread 1개 = Native Memory 1MB+ 소비
  Blocking I/O 중 CPU를 반납하지만 Thread는 점유 상태
  → Thread 수 많아질수록 Context Switch 오버헤드 증가
  → L1/L2 캐시 냉각 → 결제 처리 Hot Path 성능 저하
```

#### Virtual Thread (JDK 21+)

```text
구조:
  Virtual Thread (수십만 개)
    → Carrier Thread (ForkJoinPool, CPU 코어 수와 유사)

CPU 점유 방식:
  Virtual Thread가 Blocking 호출 진입 감지
    → Continuation(Stack Frame)을 Heap에 직렬화
    → Carrier Thread에서 분리 (unmount)
    → Carrier Thread는 다른 Virtual Thread 실행
    → I/O 완료 시 Continuation 복원 → Carrier Thread에 재스케줄

CPU 효율:
  Carrier Thread 수 = CPU 코어 수 → Context Switch 최소화
  I/O 대기 중 Carrier Thread가 유휴 상태로 낭비되지 않음
  → 동일 Carrier Thread 수로 더 많은 결제 요청 처리 가능

주의: Pinning 발생 시 Carrier Thread 고정
  → synchronized 블록 + Blocking → Virtual Thread 장점 소실
  → JFR VirtualThreadPinned 이벤트로 감지
```

### 3.2 JIT Compiler와 CPU

```text
실행 흐름:
  Bytecode 로드
    → Interpreter 실행 (CPU 비효율: Bytecode 해석 오버헤드)
    → 호출 횟수 임계치 도달 (기본 10,000회)
    → C1 Compiler: 빠른 최적화 (Level 1~3)
    → C2 Compiler: 고성능 최적화 (Level 4)

C2 최적화가 CPU에 미치는 영향:
  Inlining: 메서드 호출 비용 제거 → CPU 분기 예측 향상
  Loop Unrolling: 루프 카운터 감소 → 루프 오버헤드 감소
  Escape Analysis: Heap 할당 → Stack 할당 → GC CPU 감소
  Intrinsics: CRC32, AES, SHA 연산을 CPU 명령어 직접 매핑
    → 결제 시스템의 암호화, 해시 연산 성능 직결
  SIMD Vectorization: 배열 처리를 SIMD 명령어로 최적화
    → 대량 거래 데이터 처리 시 CPU 효율 향상

Deoptimization (역최적화):
  최적화 가정이 깨지면 (새 클래스 로드, 타입 변경 등)
    → C2 Native Code → Interpreter로 회귀
    → 해당 구간 CPU 사용률 급등
    → JFR Deoptimization 이벤트로 감지
```

**CPU 관련 JIT 지표:**

| 지표 | 도구 | 의미 |
|------|------|------|
| JIT 컴파일 시간 | JFR Compilation | CPU를 Compiler Thread가 점유하는 시간 |
| Code Cache 사용률 | `jcmd Compiler.codecache` | 가득 차면 JIT 중단 → Interpreter 회귀 |
| Deoptimization 빈도 | JFR Deoptimization | 갑작스런 CPU 사용률 증가 원인 |
| Compiler Thread CPU | `top -H -p <pid>` | `C1 CompilerThread`, `C2 CompilerThread` |

### 3.3 GC와 CPU

```text
GC 단계별 CPU 소비:

G1GC:
  Minor GC (Young Collection, STW):
    → 모든 Application Thread 정지 (TTSP)
    → GC Thread가 CPU 점유하여 Survivor/Old 이동
    → 완료 후 Application Thread 재개

  Concurrent Marking:
    → Application Thread와 병렬 실행 (CPU 경합)
    → 결제 처리 중 GC Thread가 CPU 일부 점유
    → CPU 코어가 적을수록 결제 처리 지연 가능

  Full GC (최후 수단):
    → 단일 GC Thread, 전체 Heap 압축
    → 수백 ms ~ 수 초 동안 CPU 독점
    → 결제 승인 Timeout, Kafka Rebalance 유발

ZGC / Shenandoah:
  대부분의 GC 작업이 Application Thread와 동시 실행
  → GC로 인한 CPU 경합은 있으나 STW는 수 ms 이하
  → Load Barrier(ZGC): 객체 접근 시마다 포인터 색상 확인 → CPU 오버헤드 추가
```

**GC CPU 소비 지표:**

| 지표 | 확인 방법 | 의미 |
|------|---------|------|
| GC Thread CPU | `top -H -p <pid>` (`GC Thread` 이름) | GC에 소비되는 CPU 비율 |
| GC CPU Time 비율 | JFR GCPhaseParallel | 전체 CPU 대비 GC 점유율 |
| GC Pause (STW) | `jvm_gc_pause_seconds` | Application Thread 정지 시간 |
| Allocation Rate | JFR ObjectAllocationInNewTLAB | 할당 속도 → GC 빈도 결정 |

### 3.4 Scheduler와 Context Switch

```text
Linux CFS (Completely Fair Scheduler):
  task_struct 단위로 가상 실행 시간(vruntime) 관리
  Red-Black Tree에서 vruntime 가장 작은 Thread 선택
  → 공정한 CPU 배분, 단 결제 처리 Thread의 우선순위 차별화 없음

Context Switch 비용:
  1. 현재 Thread의 CPU 레지스터, PC, Stack Pointer 저장
  2. 새 Thread의 상태 복원
  3. TLB 부분 플러시 (process 변경 시 전체 플러시)
  4. CPU L1/L2 Cache 데이터 냉각 (새 Thread 데이터로 교체)
  → 1회 Context Switch = ~수 μs + 이후 Cache Miss 비용

결제 시스템 영향:
  Thread Pool 크기 > CPU 코어 수 시
    → 불필요한 Context Switch 증가
    → CPU가 실제 연산보다 Thread 관리에 시간 낭비
    → P99 Latency 증가
```

### 3.5 Lock / CAS / Spin과 CPU

```text
Monitor Lock (synchronized):
  경합 없을 때 (Biased Locking):
    → CAS 연산으로 Thread ID 각인 → 사실상 비용 없음
  경합 발생 시:
    → Inflate → ObjectMonitor 생성
    → futex() syscall → Thread를 Wait Queue에 등록
    → CPU 반납 (Off-CPU)
    → 락 해제 시 futex_wake() → Thread 재스케줄

CAS (Compare-And-Swap):
  CPU 명령어: LOCK CMPXCHG (x86)
  → User Space에서 원자적 처리, syscall 없음
  → 경합 시 Spin Loop → CPU 100% 점유 (Busy Wait)
  → 고경합 CAS는 CPU Cache Line 무효화 반복 (MESI 프로토콜)
    → Cache Line Thrashing → CPU Stall

SpinLock vs BlockingLock:
  SpinLock: 대기 중 CPU 점유 (짧은 임계구역에 유리)
  BlockingLock: 대기 중 CPU 반납, Context Switch 비용 발생
  결제 시스템: 대부분 임계구역이 수십 μs 이상 → BlockingLock 선호

False Sharing:
  서로 다른 Thread가 같은 Cache Line(64 bytes)의 다른 변수 접근
  → MESI 프로토콜의 Invalidate 메시지 반복
  → Cache Line이 CPU 간에 계속 이동 → CPU Stall
  → @Contended 어노테이션으로 패딩 삽입하여 해결
```

### 3.6 epoll과 CPU (Netty EventLoop)

```text
EventLoop Thread CPU 사용 패턴:
  epoll_wait() 대기 (Off-CPU)
    → 이벤트 발생 시 즉시 깨어남
    → ChannelPipeline 처리 (On-CPU)
    → 처리 완료 후 다시 epoll_wait() 대기

CPU 효율:
  Thread 1개 (EventLoop) = CPU 코어 1개 최적 사용
  Non-blocking I/O이므로 I/O 대기 중 CPU 반납 없이 다른 채널 처리
  → CPU 활용률 극대화

주의:
  EventLoop에서 Blocking 작업 실행 시
    → CPU를 점유한 채로 I/O 대기 → 다른 채널 처리 불가
    → 1개 Blocking 호출이 수천 연결에 영향
```

### 3.7 TLAB과 CPU

```text
TLAB (Thread-Local Allocation Buffer):
  각 Thread가 Eden Space 일부를 선점
  → 객체 할당 시 bump-pointer 이동만 필요 (동기화 없음)
  → 락 없이 CPU 연산만으로 할당 완료

TLAB 소진 시:
  → JVM 전역 락으로 새 TLAB 요청
  → 잠깐의 CPU 경합 발생
  → Eden 고갈 시 Minor GC 트리거 → GC Thread CPU 점유

결제 요청당 DTO, Entity 객체 생성이 많을수록
  → TLAB 소진 빈도 증가 → Minor GC 빈도 증가 → CPU 간헐적 급등
```

---

## 4. CPU 병목 발생 지점

### 4.1 User CPU 병목

| 원인 | 발생 조건 | 확인 방법 |
|------|---------|---------|
| GC 과부하 | 할당 속도 > GC 회수 속도, 결제 DTO 대량 생성 | `mpstat %usr` 급등 + `jvm_gc_pause_seconds` 증가 |
| JIT 컴파일 | 서비스 기동 직후 Warm-up 구간 | `top -H`에서 `C2 CompilerThread` CPU 점유 |
| 직렬화 (Jackson) | 결제 요청/응답의 JSON 변환 반복 | JFR Method Profiling, `async-profiler` |
| CAS Spin | AtomicLong 고경합 (결제 카운터) | JFR `ThreadPark`, CPU 특정 Thread 집중 |
| Busy Loop | Event Loop 잘못된 폴링 | `top -H`에서 단일 Thread CPU 100% |

### 4.2 System CPU 병목

| 원인 | 발생 조건 | 확인 방법 |
|------|---------|---------|
| syscall 과다 | 소량 read/write 반복, 버퍼 미사용 | `strace -c -p <pid>` |
| Context Switch 폭증 | Thread Pool 과다, I/O 경합 | `vmstat cs`, `pidstat -w` |
| Futex 경합 | 다수 Thread가 동일 Lock 대기 | JFR `JavaMonitorWait`, `perf trace -e futex` |
| SoftIRQ 폭증 | 결제 트래픽 급증 시 네트워크 패킷 처리 집중 | `mpstat %soft`, `/proc/softirqs` |

### 4.3 Scheduler Delay / Run Queue Saturation

```text
발생 조건:
  Runnable Thread 수 > CPU 코어 수
  → Thread가 Runnable 상태이나 Run Queue에서 대기
  → 대기 시간만큼 결제 처리 지연

측정:
  vmstat r 컬럼: Run Queue 길이 (코어 수 초과 시 경고)
  perf sched latency: Thread별 스케줄링 대기 시간
  /proc/schedstat: 누적 스케줄링 통계
  PSI cpu.some: 일부 Task가 CPU를 기다리는 비율

결제 시스템 영향:
  Run Queue Saturation → 결제 승인 응답 지연
  특히 P99/P999 Tail Latency에 직접 영향
```

### 4.4 CPU Throttling (cgroup)

```text
발생 조건:
  K8s CPU Limit 설정이 실제 부하보다 낮음
  결제 트래픽 급증 시 CPU Quota 소진

증상:
  CPU 사용률이 Limit 수치에서 평탄해짐
  PSI cpu.some 증가
  애플리케이션 응답 지연 (CPU는 충분한데 느린 경우)

확인:
  /sys/fs/cgroup/cpu.stat
    throttled_usec 값 지속 증가 시 Throttling 확정
  kubectl describe pod → CPU Requests/Limits 확인
  cadvisor container_cpu_throttled_seconds_total
```

### 4.5 Cache Miss와 CPU Stall

```text
발생 조건:
  Context Switch 빈번 → 새 Thread 데이터 Cache Cold
  NUMA Remote 접근 → L3 Cache Miss 후 메모리 접근
  False Sharing → Cache Line 무효화 반복

CPU Pipeline Stall:
  CPU가 메모리 접근을 기다리는 동안 연산 파이프라인 정지
  L1 Cache Hit: ~4 cycle
  L2 Cache Hit: ~12 cycle
  L3 Cache Hit: ~40 cycle
  Main Memory (DRAM): ~200+ cycle
  NUMA Remote Memory: ~400+ cycle

결제 시스템 영향:
  결제 승인 Hot Path의 Cache Miss 증가
  → 동일 코드여도 지연 시간 편차 증가 (Tail Latency)

확인:
  perf stat -e cache-misses,cache-references -p <pid>
  perf stat -e L1-dcache-load-misses,LLC-load-misses -p <pid>
```

### 4.6 Lock Contention과 CPU

```text
발생 조건:
  중복 결제 방지 로직에서 동일 Lock 경합
  synchronized 메서드에 다수 Thread 진입

증상:
  CPU 사용률이 낮은데 처리량도 낮음 (Lock 대기 중 CPU 반납)
  Thread Dump에서 BLOCKED 상태 Thread 다수

CPU 관점 분석:
  futex() syscall 빈도 급증 (strace -c)
  mpstat %sys 증가 (futex 처리)
  JFR JavaMonitorWait 이벤트 증가
```

### 4.7 GC-induced CPU Spike

```text
발생 패턴:
  결제 트래픽 증가
    → Allocation Rate 증가 → Eden 빠른 소진
    → Minor GC 빈도 증가 (GC Thread CPU 사용)
    → Old 승격 증가 → Mixed GC 트리거
    → Full GC 발생 → 단일 GC Thread가 CPU 독점
    → Application Thread 전부 정지

CPU 관점:
  Minor GC: GC Thread들이 CPU 일부 점유 (병렬)
  Full GC: 단일 GC Thread가 CPU 점유, 나머지 Application Thread 정지

확인:
  mpstat에서 특정 구간 %usr 급등 + jvm_gc_pause_seconds 증가 패턴
  JFR GCPhaseParallel: GC Thread별 CPU 시간
```

### 4.8 Interrupt Storm

```text
발생 조건:
  결제 트래픽 급증 → NIC 패킷 수신 폭증
  → Hardware IRQ가 특정 CPU에 집중
  → SoftIRQ 처리(ksoftirqd)가 해당 CPU 점유

증상:
  mpstat -P ALL에서 특정 CPU의 %irq, %soft 급등
  해당 CPU에서 실행 중인 결제 처리 Thread 지연

확인:
  /proc/interrupts: NIC 인터럽트의 CPU 분산 상태
  mpstat -P ALL 1: CPU별 %irq, %soft
  ethtool -l <nic>: RSS(Receive Side Scaling) 큐 수

해결:
  IRQ Affinity 설정으로 NIC 인터럽트를 여러 CPU에 분산
  RSS/RPS 설정으로 패킷 처리 CPU 분산
```

---

## 5. SRE 관점 모니터링 지표

### 5.1 CPU 핵심 지표

| 지표 | 수집 방법 | 경고 기준 | 의미 |
|------|---------|---------|------|
| CPU Usage (%usr, %sys) | `mpstat`, Prometheus `node_cpu_seconds_total` | %usr > 80%, %sys > 20% | User/Kernel 모드별 CPU 사용 분리 |
| Load Average | `uptime`, `cat /proc/loadavg` | CPU 코어 수 초과 | 실행 대기 + 실행 중 Thread 수 |
| PSI cpu.some | `/proc/pressure/cpu` | some > 10% 지속 | 일부 Task가 CPU 할당 대기 중 |
| PSI cpu.full | `/proc/pressure/cpu` | full > 0% | 전체 Task가 CPU 대기 (완전 포화) |
| Context Switch | `vmstat cs`, `pidstat -w` | cs/s 급증 | Thread 전환 오버헤드 |
| Run Queue Length | `vmstat r` | CPU 코어 수 초과 지속 | Scheduler 지연 |
| CPU Steal | `top %st`, `vmstat st` | steal > 5% | 하이퍼바이저 CPU 탈취 |
| SoftIRQ | `mpstat %soft`, `/proc/softirqs` | 특정 CPU %soft > 50% | 네트워크 패킷 처리 집중 |

### 5.2 JVM Java 특화 지표

| 지표 | 수집 방법 | 경고 기준 | 의미 |
|------|---------|---------|------|
| GC CPU Time | JFR GCPhaseParallel, `jstat -gcutil` | GC 시간 > 전체 10% | GC가 CPU 자원 과점유 |
| GC Pause (STW) | `jvm_gc_pause_seconds` | P99 > timeout * 10% | Application Thread 정지 |
| JIT Compilation CPU | JFR Compilation, `top -H` | CompilerThread > 20% | Warm-up 또는 Deoptimization 폭주 |
| Allocation Rate | JFR ObjectAllocationInNewTLAB | 급격한 증가 | GC 빈도 증가 예고 |
| Lock Contention | JFR JavaMonitorWait | 대기 Thread 수 증가 | Lock 병목 |
| Thread Count | `jvm_threads_live` | Platform Thread 수 과다 | Context Switch 증가 원인 |
| Virtual Thread Pinning | JFR VirtualThreadPinned | 발생 시 즉시 조사 | Carrier Thread 고정으로 처리량 저하 |
| TLAB Allocation | JFR ObjectAllocationInNewTLAB | 급증 시 | Minor GC 빈도 증가 예고 |

### 5.3 결제 시스템 특화 지표

| 지표 | 수집 방법 | 의미 |
|------|---------|------|
| P95 / P99 / P999 결제 응답 시간 | Micrometer, Prometheus | CPU 병목이 Tail Latency로 표출 |
| 결제 성공률 | Prometheus | CPU Throttling / GC Pause로 인한 Timeout 결제 실패 |
| CPU Throttled Time | `container_cpu_throttled_seconds_total` (cadvisor) | K8s CPU Limit 부족 |
| Run Queue Latency | `perf sched latency` | 결제 처리 Thread 스케줄링 대기 시간 |
| Event Loop Delay | Reactor Netty 메트릭 | Blocking 코드 혼입으로 EventLoop CPU 점유 |

---

## 6. 장애 시나리오

### 6.1 CPU Saturation으로 인한 결제 지연

```text
시나리오:
  결제 트래픽 급증 (이벤트/프로모션)
    → Runnable Thread 수 > CPU 코어 수
    → Run Queue 길이 증가
    → 결제 처리 Thread의 CPU 대기 시간 증가
    → P99 Latency 증가 → 결제 Timeout 발생

징후:
  vmstat r 컬럼 CPU 코어 수 초과 지속
  PSI cpu.some 급등
  mpstat CPU별 %usr 90%+ 지속

대응:
  단기: HPA로 Pod 수평 확장 (CPU 코어 수 증가)
  중기: CPU Limit 상향 또는 Requests 조정
  장기: 결제 처리 로직 CPU 효율 개선 (직렬화 최적화, GC 튜닝)
```

### 6.2 Thread Pool Exhaustion으로 인한 CPU 유휴 + 서비스 불능

```text
시나리오:
  DB 슬로우 쿼리로 인해 모든 Worker Thread가 I/O 대기
    → CPU 사용률은 낮음 (Thread들이 Off-CPU 상태)
    → 신규 결제 요청이 Thread를 할당받지 못함
    → 결제 실패 (CPU가 남아있는데 처리 불가)

징후:
  CPU 사용률 낮음 (mpstat %usr < 20%)
  hikaricp_pending_threads 증가
  Thread Dump: 대부분 Thread가 WAITING 상태
  executor_queue_size 증가

대응:
  Thread Dump로 병목 Thread 식별
  DB 슬로우 쿼리 확인 및 최적화
  Virtual Thread 도입 검토 (I/O 대기 중 Carrier Thread 반납)
```

### 6.3 Event Loop Blocking으로 인한 결제 처리 전체 지연

```text
시나리오 (WebFlux/Netty):
  EventLoop Thread에서 JDBC 직접 호출 (실수 또는 라이브러리 내부)
    → Blocking I/O가 EventLoop Thread를 수초간 점유
    → 해당 EventLoop Thread에 연결된 수천 채널 처리 불가
    → CPU 사용률은 낮으나 처리량 급감

징후:
  netty_eventloop_* 메트릭 이상
  특정 EventLoop Thread의 CPU 100% 지속
  JFR ThreadPark에서 EventLoop Thread 장기 대기

대응:
  Blocking 호출을 subscribeOn(Schedulers.boundedElastic())으로 분리
  JFR SocketRead 이벤트로 EventLoop 내 I/O 대기 확인
```

### 6.4 Scheduler Starvation

```text
시나리오:
  특정 고우선순위 Thread들이 CPU를 독점
    → 저우선순위 결제 후처리 Thread가 CPU를 할당받지 못함
    → 결제 승인은 되나 원장 기록, 이벤트 발행 지연

징후:
  perf sched latency에서 특정 Thread의 대기 시간 비정상 증가
  pidstat에서 nvcswch/s (비자발적 Context Switch) 높은 Thread

대응:
  Thread 우선순위 재검토 (결제 처리 > 후처리 > 모니터링)
  CPU 코어 수 증가 또는 처리량 분산
```

### 6.5 GC-induced Timeout (결제 승인 Timeout)

```text
시나리오:
  결제 트래픽 급증 → 객체 할당 폭증
    → Old 영역 승격 증가 → Mixed GC 빈도 증가
    → Full GC 트리거 → GC Thread가 CPU 독점
    → Application Thread 전부 정지 (수백 ms ~ 수 초)
    → 외부 PG API Read Timeout 발생 → 결제 승인 실패
    → Kafka Consumer poll() 미호출 → session.timeout.ms 초과 → Rebalance

징후:
  jvm_gc_pause_seconds 급증
  GC 이후 외부 API Timeout 에러 급증
  Kafka Consumer Lag 급증

대응:
  JFR ObjectAllocationInNewTLAB으로 할당 핫스팟 확인
  Escape Analysis 최적화 (불필요한 Heap 할당 → Stack 할당)
  ZGC 도입 검토 (STW < 1ms)
  Kafka session.timeout.ms 상향 조정
```

### 6.6 Container CPU Throttling으로 인한 P99 Latency 증가

```text
시나리오:
  K8s CPU Limit = 2 Core
  결제 처리 Peak 시 실제 필요 CPU = 3 Core
    → 초과분은 cgroup CFS Bandwidth로 Throttling
    → Throttled Thread들이 Runnable 상태로 Run Queue 대기
    → P99 Latency 증가 (평균 응답 시간은 정상)

징후:
  CPU 사용률이 Limit에서 평탄해짐
  /sys/fs/cgroup/cpu.stat의 throttled_usec 지속 증가
  PSI cpu.some 증가

대응:
  CPU Requests/Limits 상향 조정
  cpu.max.burst 설정으로 순간 피크 허용
  VPA(Vertical Pod Autoscaler)로 자동 조정 검토
```

### 6.7 Interrupt Storm으로 인한 결제 처리 CPU 부족

```text
시나리오:
  결제 트래픽 급증 → NIC 패킷 폭증
    → NIC 인터럽트가 단일 CPU에 집중 (RSS 미설정)
    → 해당 CPU의 %soft 90%+ → 결제 처리 Thread에 CPU 배분 불가

징후:
  /proc/interrupts에서 NIC 인터럽트 특정 CPU 집중
  mpstat -P ALL에서 특정 CPU만 %soft 급등

대응:
  ethtool로 RSS 큐 수 증가
  /proc/irq/<irq>/smp_affinity로 인터럽트 CPU 분산
  RPS(Receive Packet Steering)로 소프트웨어 분산
```

---

## 7. 튜닝 포인트

### 7.1 JVM / Runtime 튜닝

| 항목 | 설정 | 효과 |
|------|------|------|
| Thread Stack 크기 | `-Xss512k` | 기본 1MB에서 감소 → Native Memory 절약, Thread 수 증가 허용 |
| GC 선택 | `-XX:+UseZGC` (JDK 15+) | STW < 1ms → 결제 처리 중 GC Pause 최소화 |
| GC Pause 목표 | `-XX:MaxGCPauseMillis=50` (G1GC) | GC가 목표 내에서 Region 선택 |
| Code Cache | `-XX:ReservedCodeCacheSize=256m` | JIT 컴파일 결과 저장 공간 확장 |
| Tiered Compilation | `-XX:+TieredCompilation` (기본 ON) | C1→C2 단계적 최적화로 Warm-up 효율화 |
| JIT 컴파일 임계치 | `-XX:CompileThreshold=1500` | Warm-up 시간 단축 (낮출수록 빠르나 CPU 소비 증가) |
| Escape Analysis | `-XX:+DoEscapeAnalysis` (기본 ON) | Heap → Stack 할당 최적화 → GC 압박 감소 |
| TLAB 크기 | `-XX:TLABSize=` | 결제 요청당 객체 수에 맞게 조정 → TLAB 재충전 빈도 감소 |
| Virtual Thread | `Thread.ofVirtual().start(...)` | I/O 대기 중 Carrier Thread 반납 → CPU 효율 향상 |

### 7.2 GC 튜닝

| GC | 설정 | 효과 |
|----|------|------|
| G1GC | `-XX:G1HeapRegionSize=16m` | Humongous 객체 방지 |
| G1GC | `-XX:ConcGCThreads=4` | Concurrent Marking Thread 수 조정 |
| G1GC | `-XX:G1ReservePercent=10` | Evacuation 실패 방지용 여유 공간 |
| ZGC | `-XX:ZCollectionInterval=0` | 트리거 조건에 따른 GC 빈도 조정 |
| 공통 | `-XX:+ParallelRefProcEnabled` | Reference 처리 병렬화 |

### 7.3 OS Scheduler 튜닝

```bash
# CFS 스케줄러 Time Slice 조정 (짧게 = 응답성 향상, 길게 = 처리량 향상)
sysctl -w kernel.sched_min_granularity_ns=1000000
sysctl -w kernel.sched_wakeup_granularity_ns=3000000

# NUMA 메모리 정책 (JVM 프로세스를 특정 노드에 고정)
numactl --cpunodebind=0 --membind=0 java -jar payment-service.jar

# CPU Pinning (결제 처리 Thread를 특정 CPU 코어에 고정)
taskset -cp 0-7 <jvm_pid>
```

### 7.4 IRQ Affinity 튜닝

```bash
# NIC 인터럽트를 특정 CPU 그룹에 분산
for i in $(grep eth0 /proc/interrupts | awk '{print $1}' | tr -d ':'); do
  echo "ff" > /proc/irq/$i/smp_affinity  # CPU 0-7에 분산
done

# RPS 설정 (소프트웨어 수신 패킷 분산)
echo "ff" > /sys/class/net/eth0/queues/rx-0/rps_cpus

# RSS 큐 수 증가
ethtool -L eth0 combined 8  # 8개 큐 (CPU 수에 맞게)
```

### 7.5 Thread Pool 튜닝

```text
Platform Thread Pool 크기 (Spring MVC / Tomcat):
  최적 크기 = CPU 코어 수 × (1 + 대기 시간 / 처리 시간)
  결제 시스템 (I/O 집약적): CPU 코어 수 × 2~4 수준
  → 너무 많으면 Context Switch 오버헤드 증가
  → 너무 적으면 I/O 대기 중 CPU 유휴

Virtual Thread (JDK 21+):
  Thread Pool 크기 불필요 (JVM이 자동 관리)
  단, Carrier Thread 수 = Runtime.getRuntime().availableProcessors()
  → 변경 시: -Djdk.virtualThreadScheduler.parallelism=N
```

### 7.6 cgroup CPU 튜닝

```yaml
# K8s Pod CPU 설정 예시 (결제 서비스)
resources:
  requests:
    cpu: "2000m"    # 2 vCPU 보장
  limits:
    cpu: "4000m"    # 최대 4 vCPU (Throttling 방지)

# CPU Burst 허용 (Linux 5.14+ / cgroup v2)
# cpu.max.burst 설정으로 순간 피크 허용
```

### 7.7 NUMA 튜닝

```bash
# JVM을 특정 NUMA 노드에 고정
numactl --cpunodebind=0 --membind=0 java \
  -XX:+UseNUMA \       # JVM NUMA 인식 활성화
  -XX:+UseParallelGC \ # NUMA-aware GC
  -jar payment-service.jar

# NUMA 접근 통계 확인
numastat -p <pid>
```

### 7.8 CPU Frequency Scaling 튜닝

```bash
# 성능 모드 설정 (레이턴시 민감 결제 시스템)
cpupower frequency-set -g performance

# C-state 비활성화 (레이턴시 < 절전)
echo 0 > /sys/devices/system/cpu/cpu*/cpuidle/state3/disable

# Intel P-state 고정 주파수
intel_pstate=passive cpufreq-set -g performance
```

---

## 8. 관련 Linux 명령어 및 분석 도구

### 8.1 CPU 전체 상태 확인

| 명령어 | 사용 예 | 확인 항목 |
|--------|--------|---------|
| `top` | `top -H -p <pid>` | Thread별 CPU 사용률, %st(Steal), Load Average |
| `mpstat` | `mpstat -P ALL 1` | CPU별 %usr, %sys, %soft, %irq, %idle |
| `vmstat` | `vmstat 1` | cs(Context Switch), r(Run Queue), us, sy |
| `pidstat` | `pidstat -u -w -t -p <pid> 1` | Thread별 CPU, 자발적/비자발적 Context Switch |
| `sar` | `sar -u 1 60` | CPU 사용률 시계열 분석 |
| `uptime` | `uptime` | Load Average 1/5/15분 |

### 8.2 CPU 세부 분석

| 명령어 | 사용 예 | 확인 항목 |
|--------|--------|---------|
| `perf top` | `perf top -p <pid>` | CPU 핫 함수 실시간 확인 |
| `perf stat` | `perf stat -e cache-misses,context-switches -p <pid>` | Cache Miss, Context Switch 횟수 |
| `perf record/report` | `perf record -g -p <pid>; perf report` | 함수별 CPU 사용 비율, Call Graph |
| `perf sched` | `perf sched latency` | Thread 스케줄링 대기 시간 분포 |
| `strace` | `strace -c -p <pid>` | syscall별 호출 횟수, 소요 시간 |
| `taskset` | `taskset -cp 0-3 <pid>` | CPU Affinity 설정/확인 |
| `numactl` | `numactl -H`, `numastat -p <pid>` | NUMA 구성, 프로세스별 NUMA 접근 통계 |

### 8.3 Java JVM 특화 도구

| 도구 | 사용 예 | 확인 항목 |
|------|--------|---------|
| `jstack` | `jstack <pid>` | Thread Dump, Thread 상태 (BLOCKED, WAITING, RUNNABLE) |
| `top -H + jstack` | LWP ID → 16진수 → jstack nid 매칭 | CPU 점유 Thread의 실제 Java Stack |
| `jstat` | `jstat -gcutil <pid> 1s` | GC 횟수, 시간, 힙 사용률 |
| `jcmd` | `jcmd <pid> Thread.print` | 전체 Thread 상태 |
| JFR | `jcmd <pid> JFR.start name=cpu duration=60s` | GC, JIT, Lock, CPU 핫 메서드, SafePoint |
| JDK Mission Control | GUI 분석 | JFR 파일 시각화, Flame Graph |
| `async-profiler` | `./profiler.sh -e cpu -d 30 -f flame.html <pid>` | CPU Flame Graph (JIT 코드 포함) |

### 8.4 eBPF / BCC 도구

| 도구 | 용도 | 사용 예 |
|------|------|--------|
| `runqlat` | Run Queue 대기 시간 분포 | `runqlat -p <pid> 10` |
| `offcputime` | Off-CPU Time 분석 (I/O, Lock 대기) | `offcputime -p <pid> 10` |
| `cpudist` | CPU On-CPU 시간 분포 | `cpudist -p <pid> 10` |
| `hardirqs` | 하드웨어 인터럽트 처리 시간 | `hardirqs 10` |
| `softirqs` | SoftIRQ 처리 시간 | `softirqs 10` |
| `funclatency` | 특정 함수 실행 시간 | `funclatency java::<method>` |
| `profile` | CPU Sampling 프로파일링 | `profile -p <pid> -F 99 10` |
| `bpftrace` | 커스텀 eBPF 스크립트 | `bpftrace -e 'tracepoint:sched:sched_switch { @[comm] = count(); }'` |

### 8.5 Flame Graph 생성

```bash
# async-profiler를 이용한 Java CPU Flame Graph
./profiler.sh -e cpu -d 60 -f /tmp/cpu-flame.html <pid>

# perf + FlameGraph.pl
perf record -F 99 -g -p <pid> -- sleep 30
perf script > out.perf
./FlameGraph/stackcollapse-perf.pl out.perf > out.folded
./FlameGraph/flamegraph.pl out.folded > cpu-flame.svg

# Off-CPU Flame Graph (I/O, Lock 대기 시간)
./profiler.sh -e wall -d 60 -f /tmp/wall-flame.html <pid>
```

### 8.6 PSI 및 cgroup 확인

```bash
# PSI (Pressure Stall Information)
cat /proc/pressure/cpu      # CPU 압력
cat /proc/pressure/memory   # 메모리 압력
cat /proc/pressure/io       # I/O 압력

# cgroup CPU Throttling 확인
cat /sys/fs/cgroup/cpu.stat
  nr_throttled              # Throttling 발생 횟수
  throttled_usec            # 총 Throttling 시간 (마이크로초)

# K8s Pod 내부에서 확인
cat /sys/fs/cgroup/cpu.max  # "quota period" 형식
```

---

## 9. 분석 절차 요약

```text
[CPU 병목 진단 순서]

Step 1. 전체 CPU 상태 확인
  uptime → Load Average 확인
  mpstat -P ALL 1 → CPU별 %usr/%sys/%soft/%idle 확인
  cat /proc/pressure/cpu → PSI로 실제 포화 여부 확정

Step 2. 컨테이너 Throttling 확인
  cat /sys/fs/cgroup/cpu.stat → throttled_usec 증가 여부
  → 증가 시 CPU Limit 상향 우선 검토

Step 3. Run Queue / Scheduler 확인
  vmstat 1 → r 컬럼 (CPU 코어 수 초과 시 Runnable 과부하)
  perf sched latency → Thread별 스케줄링 대기 시간

Step 4. Java Thread/GC 확인
  top -H -p <pid> → CPU 점유 Thread 식별
  jstack <pid> → 해당 Thread Stack 확인 (GC / JIT / 비즈니스 로직)
  jstat -gcutil → GC 빈도, 시간 확인

Step 5. 심층 프로파일링
  JFR 수집 → GC, Lock, JIT, SafePoint, Allocation 확인
  async-profiler CPU Flame Graph → 핫 메서드 확인
  offcputime eBPF → Off-CPU 원인 분리

Step 6. Kernel 수준 확인
  perf stat -e cache-misses → Cache Miss 비율
  strace -c -p <pid> → syscall 오버헤드
  /proc/interrupts → IRQ 분산 상태
```

---

## 10. Java CPU 분석 핵심 정리

| 계층 | 핵심 병목 | 확인 도구 |
|------|---------|---------|
| Hardware | Cache Miss, NUMA Remote 접근, C-state 지연 | `perf stat -e cache-misses`, `numastat`, `cpupower` |
| OS Kernel | SoftIRQ 집중, Scheduler 지연, Context Switch 폭증 | `mpstat`, `perf sched`, `vmstat`, `/proc/interrupts` |
| cgroup | CPU Throttling (K8s CPU Limit 부족) | `/sys/fs/cgroup/cpu.stat`, `cadvisor` |
| JVM GC | GC Thread CPU 점유, STW로 Application Thread 정지 | JFR, `jstat`, `jvm_gc_pause_seconds` |
| JVM JIT | Warm-up 중 Compiler Thread CPU 점유, Deoptimization | JFR Compilation, `top -H` |
| Thread | Context Switch 오버헤드, Lock Contention, False Sharing | Thread Dump, JFR `JavaMonitorWait`, `perf stat` |
| Virtual Thread | Carrier Thread Pinning으로 CPU 효율 저하 | JFR `VirtualThreadPinned`, `-Djdk.tracePinnedThreads` |
| Application | 직렬화 CPU 비용, 불필요한 객체 할당 | `async-profiler` Flame Graph, JFR Allocation |
| 결제 특화 | GC Pause → PG API Timeout, Throttling → P99 급등 | Trace Span + GC Pause 상관관계, PSI |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*