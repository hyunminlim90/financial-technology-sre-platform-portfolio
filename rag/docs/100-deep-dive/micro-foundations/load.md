# Load (부하)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Load(부하)는:

> 시스템이 특정 시점에 처리해야 하는 작업량의 총합과, 그 작업으로 인해 발생하는 자원 압박 상태

중요한 점은:

> **Load는 단순 트래픽 숫자만 의미하지 않는다.**

부하는 크게 두 가지를 함께 포함한다.

**1) 입력 측면** — 시스템으로 들어오는 일거리. 예:

- 요청 수
- 데이터 양
- 동시 사용자 수
- 메시지 유입량

**2) 결과 측면** — 그 일거리를 처리하느라 발생한 자원 사용 압박. 예:

- CPU saturation
- memory pressure
- disk queue
- network congestion

즉:

> **Load는 "들어오는 일의 양"과 "그 일 때문에 시스템이 받는 압박" 전체를 의미한다.**

---

## 2. 시스템 어디에서 등장하는가

부하는 시스템 전체에서 등장한다.

**사용자 레이어**
- 로그인 폭주
- 결제 이벤트
- API burst

**애플리케이션 레이어**
- task accumulation
- queue backlog
- retry storm

**데이터 레이어**
- DB query 증가
- replication lag
- write amplification

**네트워크 레이어**
- bandwidth saturation
- packet congestion
- retransmission

**운영체제 레이어**
- run queue 증가
- context switching 증가
- page cache pressure

**인프라 레이어**
- node overload
- pod saturation
- storage bottleneck

즉:

> **Load는 특정 기술의 문제가 아니라 시스템 전체 흐름에 걸쳐 발생하는 압력 현상이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Load는 모든 자원에 영향을 준다. 다만 부하의 종류에 따라 병목 지점이 달라진다.

### CPU Load

특징: 계산량 증가, scheduling pressure 증가

징후:

- run queue 증가
- latency 증가
- throttling 발생

### Memory Load

특징: working set 증가, cache miss 증가

징후:

- swapping
- OOM
- GC pressure
- reclaim 증가

### Disk Load

특징: write/read backlog 증가

징후:

- I/O wait 증가
- queue depth 증가
- flush delay 발생

### Network Load

특징: packet flow 증가

징후:

- retransmission
- packet drop
- RTT 증가

핵심:

> **Load는 단순 CPU 문제가 아니라 시스템 자원 전체의 균형 문제이다.**

---

## 4. 왜 중요한가

시스템 장애의 상당수는:

> **결함(Fault) 자체보다 부하 폭증 상황에서 발생한다.**

왜냐하면 작은 버그도 높은 부하 상황에서는 폭발적으로 증폭되기 때문이다.

예시: 평소에는 문제 없는 race condition도 동시 요청이 급증하면 데이터 무결성 파괴로 이어질 수 있다.

즉:

> **Load는 시스템의 숨겨진 약점을 드러내는 증폭기이다.**

또한 부하는 Stability와 직접 연결된다. 자원이 임계치를 넘으면:

- latency 증가
- timeout 증가
- retry 증가
- queue accumulation 증가
- cascading failure 발생

결국:

> **Load 제어 실패는 시스템 붕괴로 연결된다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

부하는 대부분의 장애 시나리오의 시작점이다.

**1) CPU Saturation**

```
부하 증가
  ↓ run queue 증가
  ↓ scheduler delay
  ↓ latency explosion
  ↓ timeout
  ↓ 서비스 장애
```

**2) Memory Pressure**

```
부하 증가
  ↓ cache accumulation
  ↓ memory exhaustion
  ↓ OOM Kill
  ↓ service restart
```

**3) Retry Storm**

```
외부 서비스 지연
  ↓ client retry 증가
  ↓ 부하 폭증
  ↓ queue overload
  ↓ 전체 시스템 마비
```

**4) Database Overload**

```
query 증가
  ↓ connection pool exhaustion
  ↓ lock contention
  ↓ slow query accumulation
  ↓ DB saturation
```

**5) Network Congestion**

```
traffic burst
  ↓ packet drop
  ↓ TCP retransmission 증가
  ↓ RTT 증가
  ↓ API timeout 증가
```

핵심:

> **부하는 단독으로 위험한 것이 아니라 연쇄 반응을 유발하기 때문에 위험하다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Load의 핵심 메커니즘은:

> **Queueing(대기열)**

이다.

시스템 자원은 유한하다. 따라서:

> **들어오는 작업 속도 > 처리 가능한 속도**

가 되는 순간 대기열(queue)이 형성된다. 그리고 queue가 커질수록:

- latency 증가
- timeout 증가
- retry 증가
- resource retention 증가

가 발생한다. 즉:

> **Load 문제의 본질은 "대기열 폭발"이다.**

그래서 현대 시스템은 다음 메커니즘으로 부하를 통제한다:

- **Rate Limiting** — 과도한 입력 제한
- **Load Balancing** — 부하 분산
- **Backpressure** — 처리 불가능한 속도 차단
- **Autoscaling** — 자원 자동 확장
- **Queue Isolation** — 작업 종류별 분리
- **Circuit Breaking** — 폭주 전파 차단
- **Graceful Degradation** — 부분 기능 유지

핵심 개념:

> **Load 자체는 피할 수 없다.**  
> **중요한 것은 부하가 제어 가능한 범위를 넘지 않게 유지하는 것이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**CPU Load Average**

```bash
uptime
cat /proc/loadavg
```

관찰: run queue pressure, CPU saturation

**CPU 사용률**

```bash
top
htop
mpstat
```

**메모리 압박**

```bash
free -h
vmstat
sar -r
```

관찰: swap usage, reclaim, memory pressure

**디스크 부하**

```bash
iostat -x
iotop
```

관찰: await, queue depth, utilization

**네트워크 부하**

```bash
sar -n DEV
iftop
ss -s
```

관찰: throughput, retransmission, socket accumulation

### Runtime

관찰 포인트:

- queue size
- task backlog
- latency percentile
- timeout 증가
- retry 증가

### Kubernetes

**Pod 자원 사용량**

```bash
kubectl top pod
kubectl top node
```

**Pod 상태**

```bash
kubectl describe pod
```

관찰: throttling, `OOMKilled`, restart 증가

**이벤트 확인**

```bash
kubectl get events
```

**HPA 상태**

```bash
kubectl get hpa
```

관찰: scaling pressure, replica 증가 여부

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*