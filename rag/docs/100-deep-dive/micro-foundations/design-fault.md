# Design Fault (설계 결함)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Design Fault(설계 결함)는:

> 시스템의 구조·흐름·자원 정책·상호작용 방식 자체에 내재된 근본적인 구조적 결점

중요한 점은:

> **설계 결함은 단순 코딩 실수(Bug)와 다르다.**

코드는 문법적으로 완벽하게 동작할 수 있다. 하지만:

- 시스템 구조
- 데이터 흐름
- 자원 제한 방식
- 동시성 모델
- 장애 격리 구조

같은 "청사진(Blueprint)" 자체가 잘못되면:

> **시스템은 정상처럼 보이다가 특정 조건에서 구조적으로 붕괴한다.**

즉:

> **Bug는 코드 한 줄 문제일 수 있지만, Design Fault는 시스템 뼈대 자체의 문제이다.**

<details>
<summary>Deep Dive</summary></br>

System(시스템) [[M]](../../100-deep-dive/micro-foundations/system.md)  
System Architecture(시스템 구조) [[M]](../../100-deep-dive/micro-foundations/system-architecture.md)  
Data Flow(데이터 흐름) [[M]](../../100-deep-dive/micro-foundations/data-flow.md)  
Constraints(제약 조건) [[M]](../../100-deep-dive/micro-foundations/constraints.md)  

</details></br>

## 2. 시스템 어디에서 등장하는가

설계 결함은 시스템 전체 구조에서 등장한다.

**데이터 흐름 설계**
- 무한 queue 구조
- uncontrolled retry
- unbounded buffering

**자원 정책 설계**
- resource limit 부재
- backpressure 부재
- memory isolation 누락

**통신 구조 설계**
- synchronous dependency chain
- timeout propagation
- cascading retry

**상태 관리 설계**
- shared mutable state
- global lock dependency
- inconsistent ownership

**인프라 설계**
- SPOF
- replication 부재
- failover 부재

**장애 대응 설계**
- recovery path 없음
- isolation boundary 없음
- rollback 불가능 구조

즉:

> **Design Fault는 특정 코드 위치가 아니라 시스템 구조 전체에 숨어 있다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

설계 결함은 특정 자원 하나보다:

> **자원 사용 방식 전체를 왜곡한다.**

### CPU 측면

예: uncontrolled parallelism, excessive scheduling, retry storm

결과:

- saturation
- throttling
- latency explosion

### Memory 측면

예: unbounded queue, cache accumulation, retention leak

결과:

- memory pressure
- OOM
- reclaim storm

### Disk 측면

예: write amplification, synchronous flush dependency, uncontrolled persistence

결과:

- I/O bottleneck
- queue accumulation
- storage saturation

### Network 측면

예: chatty architecture, retry amplification, synchronous fan-out

결과:

- congestion
- retransmission
- timeout cascade

핵심:

> **설계 결함은 특정 자원 오류보다 자원 소비 패턴 자체를 위험하게 만든다.**

---

## 4. 왜 중요한가

설계 결함은:

> **평소에는 잘 드러나지 않는다.**

왜냐하면 낮은 부하와 정상 조건에서는 시스템이 우연히 버텨주기 때문이다.

하지만:

- 부하 증가
- Spike 발생
- 장애 상황
- 네트워크 지연
- 부분 시스템 실패

같은 현실 조건이 오면:

> **숨겨진 설계 결함이 폭발한다.**

즉:

> **설계 결함은 정상 상황보다 비정상 상황에서 드러난다.**

또한:

> **설계 결함은 수정 비용이 매우 크다.**

구현 버그는 코드 수정으로 해결 가능하지만, 설계 결함은 아키텍처 자체를 재구성해야 하는 경우가 많다.

---

## 5. 실제 장애와 어떤 관련이 있는가

대형 장애 상당수는 구현 버그보다 설계 결함에서 시작된다.

**1) SPOF 설계 결함**

```
DB 단일 노드 구조
  ↓ storage failure 발생
  ↓ 전체 서비스 정지
```

**2) Retry Amplification 설계 결함**

```
timeout 발생
  ↓ 모든 client retry 시작
  ↓ traffic 폭증
  ↓ resource exhaustion
  ↓ cascading failure
```

**3) Unbounded Queue 설계 결함**

```
Spike 발생
  ↓ queue accumulation
  ↓ memory growth
  ↓ OOM 발생
  ↓ service restart storm
```

**4) Shared State 설계 결함**

```
동시 업데이트 증가
  ↓ race condition 발생
  ↓ state corruption
  ↓ integrity failure
```

**5) Tight Coupling 설계 결함**

```
하위 시스템 장애
  ↓ 상위 서비스 blocking
  ↓ 전체 dependency chain 전파
  ↓ system-wide outage
```

즉:

> **설계 결함은 장애의 "폭발 반경"을 결정한다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Design Fault의 핵심 메커니즘은:

> **정상 상황 중심 설계**

이다. 많은 시스템은 "모든 것이 정상일 것"을 전제로 설계된다.

하지만 현실은:

- network partition 발생 가능
- timeout 발생 가능
- hardware fault 발생 가능
- spike 발생 가능
- resource exhaustion 발생 가능

하다. 그래서 현대 시스템 설계는:

> **Failure-aware architecture**

가 핵심이다.

핵심 메커니즘들:

- **Isolation** — 문제가 전체로 퍼지지 않게 분리
- **Bounded Resource** — 무한 성장 금지
- **Backpressure** — 처리 가능 속도 유지
- **Redundancy** — 단일 실패점 제거
- **Timeout** — 무한 대기 차단
- **Retry Control** — 폭주 방지
- **Failover** — 자동 대체 경로 제공
- **Graceful Degradation** — 부분 기능 유지

핵심 개념:

> **좋은 설계는 장애가 "없다"가 아니라,**  
> **장애가 발생해도 시스템 전체가 무너지지 않도록 만드는 것이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

설계 결함은 직접 보이지 않는다. 대신:

> **반복적 이상 패턴**

으로 드러난다.

### Linux

**Load 폭증 패턴**

```bash
uptime
vmstat
top
```

관찰: queue accumulation, context switching 증가, saturation 반복

**메모리 이상 패턴**

```bash
free -h
sar -r
```

관찰: 지속적 memory growth, reclaim storm, OOM 반복

**네트워크 이상 패턴**

```bash
ss -s
sar -n DEV
```

관찰: connection explosion, retransmission 증가, timeout 반복

### Runtime

관찰 포인트:

- retry storm
- queue accumulation
- latency amplification
- deadlock
- cascading timeout

### Kubernetes

**Pod 반복 재시작**

```bash
kubectl get pod
```

관찰: `CrashLoopBackOff`, `OOMKilled` 반복

**Node Pressure**

```bash
kubectl describe node
```

관찰: `MemoryPressure`, `DiskPressure`, `PIDPressure`

**Autoscaling 이상 패턴**

```bash
kubectl get hpa
```

관찰: scale-out 반복, oscillation, unstable recovery

**이벤트 분석**

```bash
kubectl get events
```

관찰: eviction 반복, throttling 증가, scheduling failure

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*