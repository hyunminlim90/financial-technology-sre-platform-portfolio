# Spike (충격)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

Spike(스파이크)는:

> 시스템 지표가 매우 짧은 시간 동안 비정상적으로 급격하게 치솟는 현상

핵심 특징은:

> **"짧은 시간"과 "급격한 변화"**

이다.

평소에는 안정적으로 유지되던:

- 요청량
- CPU 사용률
- 메모리 사용량
- 디스크 I/O
- 네트워크 트래픽
- connection 수

같은 값이 순간적으로 폭발하듯 상승하는 현상이 스파이크다.

즉:

> **Load가 지속적 압력이라면, Spike는 순간 충격이다.**

---

## 2. 시스템 어디에서 등장하는가

스파이크는 시스템 전체 어디서든 발생한다.

**사용자 입력 영역**
- 이벤트 오픈
- 재난 상황
- 알림 발송 직후

**애플리케이션 영역**
- task burst
- queue explosion
- retry storm

**데이터 영역**
- massive write burst
- lock contention spike
- replication delay spike

**네트워크 영역**
- sudden bandwidth burst
- packet flood
- connection surge

**운영체제 영역**
- context switch spike
- interrupt storm
- scheduler overload

**인프라 영역**
- pod surge
- autoscaling burst
- node saturation

즉:

> **Spike는 특정 계층 문제가 아니라 시스템 전체에서 발생 가능한 급격한 변화 현상이다.**

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

스파이크는 거의 항상 짧은 시간 안에 특정 자원을 순간 포화시킨다.

### CPU Spike

특징: scheduler queue 급증, latency 급증

징후:

- throttling
- run queue 증가
- response delay

### Memory Spike

특징: sudden allocation burst, cache accumulation

징후:

- reclaim 증가
- swap 증가
- OOM 위험

### Disk Spike

특징: write burst, flush storm

징후:

- I/O wait 증가
- queue depth 폭증
- fsync delay

### Network Spike

특징: sudden packet surge, connection explosion

징후:

- retransmission 증가
- packet drop
- timeout 증가

핵심:

> **Spike의 위험성은 평균값이 아니라 순간 최대치(Peak)에 있다.**

---

## 4. 왜 중요한가

현대 시스템은 대부분:

> **평균 부하보다 순간 스파이크 때문에 무너진다.**

왜냐하면 시스템은 보통 "지속적 처리량" 기준으로 설계되기 때문이다. 하지만 Spike는 짧은 시간 동안 설계 한계를 초과한다.

예: 평소 30% CPU 사용률 시스템도 1초 동안 1000배 burst가 오면 queue explosion이 발생할 수 있다.

즉:

> **Spike는 숨겨진 병목과 약한 연결부를 드러낸다.**

또한:

> **Spike는 cascading failure의 시작점이 되기 쉽다.**

```
traffic spike
  ↓ timeout 증가
  ↓ retry 증가
  ↓ load amplification
  ↓ resource exhaustion
  ↓ failure propagation
```

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 장애의 상당수는 지속 부하보다 순간 스파이크에서 시작된다.

**1) Traffic Spike → Queue Explosion**

```
요청 폭증
  ↓ 처리 속도 초과
  ↓ queue accumulation
  ↓ latency 증가
  ↓ timeout 증가
  ↓ retry storm
  ↓ 서비스 장애
```

**2) CPU Spike → Latency Spike**

```
갑작스러운 연산 폭증
  ↓ scheduler overload
  ↓ task delay
  ↓ response latency 급증
  ↓ client timeout
```

**3) Connection Spike → Resource Exhaustion**

```
동시 연결 급증
  ↓ connection pool exhaustion
  ↓ new request blocking
  ↓ system saturation
```

**4) Disk Spike → Persistence Delay**

```
write burst 발생
  ↓ flush backlog 증가
  ↓ transaction delay
  ↓ replication lag 증가
```

**5) Network Spike → Packet Loss**

```
bandwidth saturation
  ↓ packet drop
  ↓ TCP retransmission
  ↓ RTT 폭증
  ↓ distributed timeout
```

즉:

> **Spike는 시스템이 가장 약한 순간을 강제로 드러낸다.**

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

Spike의 핵심 메커니즘은:

> **Burst > Absorption Capacity**

이다. 즉 순간 유입 속도가 시스템 흡수 속도를 초과하면 스파이크 문제가 발생한다.

그래서 현대 시스템은 **충격 흡수 구조(Buffering)** 를 만든다.

핵심 메커니즘들:

- **Queue Buffering** — 순간 충격을 큐에 저장
- **Rate Limiting** — 초과 유입 차단
- **Backpressure** — 처리 불가능한 속도 억제
- **Autoscaling** — 자원 즉시 확장
- **Load Shedding** — 일부 요청 포기
- **Circuit Breaking** — 폭주 전파 차단
- **Graceful Degradation** — 핵심 기능만 유지

핵심 개념:

> **Spike를 완전히 없앨 수는 없다.**  
> **중요한 것은 충격이 시스템 전체로 퍼지지 않게 막는 것이다.**

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

### Linux

**CPU Spike**

```bash
top
htop
mpstat
```

관찰: sudden CPU burst, run queue 증가

**Load Spike**

```bash
uptime
cat /proc/loadavg
```

관찰: load average 급등

**메모리 스파이크**

```bash
vmstat
free -h
sar -r
```

관찰: reclaim, swap burst, sudden memory growth

**디스크 스파이크**

```bash
iostat -x
```

관찰: await spike, util 100%, queue depth 증가

**네트워크 스파이크**

```bash
sar -n DEV
iftop
ss -s
```

관찰: bandwidth burst, socket surge, retransmission 증가

### Runtime

관찰 포인트:

- queue length spike
- latency spike
- retry burst
- timeout burst
- task accumulation

### Kubernetes

**Pod 사용량 급등**

```bash
kubectl top pod
kubectl top node
```

**이벤트 확인**

```bash
kubectl get events
```

관찰: scaling event, OOM, throttling

**Pod 상태**

```bash
kubectl describe pod
```

관찰: restart burst, CPU throttling, memory pressure

**HPA 상태**

```bash
kubectl get hpa
```

관찰: rapid scale-out, replica burst

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*