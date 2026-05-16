# Stability (안정성)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Stability(안정성)는:

> 시스템이 장애·부하·오류·충격을 받아도 완전히 무너지지 않고 예측 가능한 범위 안에서 동작을 유지하는 능력

핵심은:

> **"절대 안 죽는 시스템"**

이 아니라,

> **"죽더라도 통제 가능하게 버티는 시스템"**

이다.

안정성은 보통 다음을 의미한다.

- 갑작스러운 부하에도 폭주하지 않음
- 일부 실패가 전체 붕괴로 이어지지 않음
- 자원 고갈 상황에서도 핵심 기능 유지
- 장애 후 회복 가능한 상태 유지

즉:

> **Stability는 시스템이 스스로 붕괴를 억제하는 능력이다.**

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
Fault(결함) [[M]](../../100-deep-dive/micro-foundations/fault.md)  
Load(부하) [[M]](../../100-deep-dive/micro-foundations/load.md)  
Error(오류) [[M]](../../100-deep-dive/micro-foundations/error.md)  
Spike(충격) [[M]](../../100-deep-dive/micro-foundations/spike.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

안정성은 시스템 전체에서 등장한다.

**하드웨어**
- 전력 안정성
- 발열 제어
- 메모리 오류 보정
- 디스크 오류 복구

**운영체제**
- 프로세스 격리
- memory protection
- scheduler fairness
- kernel panic 방지

**런타임**
- garbage collection 안정화
- task scheduling
- resource cleanup
- deadlock 회피

**네트워크**
- packet retransmission
- congestion control
- timeout
- retry 제한

**데이터 저장소**
- transaction rollback
- consistency 유지
- replication
- crash recovery

**분산 시스템**
- fault isolation
- leader election
- quorum
- failover

즉:

> **안정성은 특정 기능이 아니라 시스템 전체를 관통하는 생존 속성이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

안정성은:

> **모든 자원의 균형 상태**

와 연결된다. 특히 중요하게 보는 것은:

- CPU saturation
- Memory exhaustion
- Network congestion
- Disk I/O bottleneck

이다.

### CPU

문제: 과도한 연산, starvation, runaway task

결과:

- latency 증가
- throttling
- scheduling collapse

### Memory

문제: memory leak, allocation 폭주, cache explosion

결과:

- OOM
- swap storm
- process kill

### Network

문제: traffic burst, packet loss, retry amplification

결과:

- cascading timeout
- service isolation 실패

### Disk

문제: write saturation, queue buildup, fsync delay

결과:

- transaction stall
- replication lag

---

## 4. 왜 중요한가

현대 시스템은:

> **항상 실패 가능성 위에서 동작한다.**

따라서 안정성이 없으면:

- 작은 장애가 전체 장애로 확대
- 일부 서비스 장애가 연쇄 붕괴 유발
- 자원 폭주가 시스템 전체 마비 초래

반대로 안정성이 높은 시스템은:

- 일부 기능만 제한
- 핵심 서비스 유지
- 장애 범위 격리
- 회복 가능 상태 유지

를 수행한다. 즉:

> **Stability는 시스템 생존 확률을 높이는 핵심 속성이다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

안정성 부족은 대부분 **"폭주"** 형태로 나타난다.

### 1) Memory Explosion

메모리 사용량 발산. 결과:

- OOM Kill
- node crash

### 2) Retry Storm

실패한 요청을 무한 재시도. 결과:

- network overload
- downstream collapse

### 3) Cascading Failure

한 시스템 장애가 다른 시스템으로 전파. 결과:

- 전체 서비스 장애

### 4) Queue Saturation

처리 속도보다 입력 속도가 빠름. 결과:

- latency 폭증
- timeout 증가

### 5) Deadlock / Resource Contention

자원 경쟁 상태 고착. 결과:

- task 정지
- throughput 급감

### 6) Congestion Collapse

네트워크 혼잡 상태 지속. 결과:

- 전체 throughput 붕괴

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

안정성의 핵심은:

> **"문제가 생겨도 폭주하지 않게 만드는 것"**

이다. 즉:

> **Stability ≠ Error 없음**

오히려 안정성은:

> **Error를 제한된 범위 안에 가두는 능력**

에 가깝다.

안정성을 만드는 핵심 메커니즘:

- **Isolation** — 문제 전파 차단. 예: process isolation, container isolation, fault domain 분리
- **Constraints** — 자원 사용 제한. 예: CPU limit, memory limit, timeout
- **Backpressure** — 과부하 입력 억제. 예: queue limit, flow control
- **Graceful Degradation** — 핵심 기능 우선 유지. 예: 일부 기능 차단, read-only mode 전환
- **Retry Control** — 재시도 폭주 차단. 예: exponential backoff, retry budget
- **Failure Detection** — 빠른 장애 감지. 예: health check, heartbeat, watchdog
- **Recovery** — 자동 회복. 예: restart, failover, replication recovery

중요한 점:

> **안정성은 단일 기술이 아니라 수많은 보호 메커니즘의 조합이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**CPU / Load**

```bash
top
uptime
mpstat
vmstat
```

관찰: load average, steal, iowait

**Memory**

```bash
free -h
cat /proc/meminfo
```

관찰: available memory, swap usage, page cache

**OOM**

```bash
dmesg | grep -i oom
```

**Disk**

```bash
iostat
iotop
```

관찰: await, util, queue depth

**Network**

```bash
ss -s
netstat -s
sar -n DEV
```

관찰: retransmission, dropped packet, connection saturation

### Runtime

관찰 포인트:

- queue backlog
- GC pause
- task latency
- timeout rate
- retry count

### Kubernetes

안정성 관리 핵심 플랫폼 중 하나.

**Resource 상태**

```bash
kubectl top pod
kubectl top node
```

**이벤트 확인**

```bash
kubectl get events
```

**장애 상태**

```bash
kubectl describe pod
```

관찰: `OOMKilled`, `CrashLoopBackOff`, `Evicted`, `Unhealthy`

**노드 상태**

```bash
kubectl describe node
```

관찰: `MemoryPressure`, `DiskPressure`, `PIDPressure`, `Ready` 상태

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*