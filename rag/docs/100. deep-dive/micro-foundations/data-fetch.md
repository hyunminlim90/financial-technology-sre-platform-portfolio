# Data Fetch (데이터 페칭)
## Micro Foundations — 범용 시스템 관점

> 정독: 0회

## 1. 이 기술이 무엇인가

**Data Fetch** 는:

> 필요한 데이터를 현재 위치에서 읽어와 **실행 가능한 위치로 이동시키는 과정**

핵심은 단순 "읽기(Read)"보다 더 넓다. Fetch에는:

- 어디 있는지 찾고
- 접근하고
- 이동시키고
- 사용할 수 있는 상태로 만드는

전체 흐름이 포함된다.

즉:

> **Fetch = "데이터를 실행 흐름 안으로 끌어오는 행위"**

---

## 2. 시스템 어디에서 등장하는가

Fetch는 **시스템 전체에서 끊임없이 발생**한다.

### CPU 내부
- 명령어 Fetch
- Operand Fetch
- Cache Fetch

### 메모리 계층
- RAM → Cache
- Disk → RAM
- Swap → Memory

### 네트워크
- Remote API Fetch
- Packet Fetch
- Stream Fetch

### 저장소
- Database Fetch
- Object Fetch
- Block Fetch

### 분산 시스템
- Replica Fetch
- Leader Sync Fetch
- Event Fetch

즉 시스템은 본질적으로 **계속 Fetch 하면서 움직이는 구조**라고 볼 수 있다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

Fetch는 거의 모든 자원과 연결되지만, 특히 영향이 큰 것은 **Memory + Network + Disk**이다.

| 자원 | Fetch와의 관계 |
|------|-------------|
| Memory | Fetch 대상이 없으면 cache miss / page fault / swap access 발생 |
| Disk | 느린 저장소에서 Fetch 시 I/O wait 증가, queue depth 증가, latency 급증 |
| Network | 원격 Fetch 시 RTT, bandwidth, packet loss, retransmission 영향 |
| CPU | Fetch 자체보다 waiting / stall / scheduling delay 형태로 효율에 영향 |

---

## 4. 왜 중요한가

현대 시스템 성능의 대부분은:

> **연산 속도보다 Fetch 속도에 의해 결정된다**

CPU는 매우 빠르다. 문제는 **필요한 데이터를 제때 공급받지 못하는 것**이다.

즉, 계산보다 **기다림(waiting)** 이 병목이 되는 경우가 많다.

대표 사례: `cache miss`, `disk seek`, `network round trip`, `DB query fetch delay`

> **Fetch 최적화 = Latency 최적화**

---

## 5. 실제 장애와 어떤 관련이 있는가

Fetch 문제는 **매우 흔한 장애 원인**이다.

### 1) Cache Miss 폭증
필요 데이터가 cache에 없음 시 RAM 접근 증가 → CPU stall 증가 → latency 급증

### 2) Slow Disk Fetch
저장소에서 데이터 Fetch 지연 시 application hang / I/O wait 증가 / timeout 발생

### 3) Remote Fetch 장애
원격 시스템/API Fetch 실패 시 cascading failure → retry storm → thread starvation 가능

### 4) Excessive Fetch
너무 자주 Fetch 시 bandwidth 낭비 → DB overload → memory pressure 발생

### 5) Large Fetch
한 번에 과도한 데이터 Fetch 시 OOM → GC 폭증 → eviction 발생 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심은 **"가까운 곳에서 Fetch 할수록 빠르다"** 이다.

### Fetch 비용 계층

```
Register
  ↓
CPU Cache
  ↓
RAM
  ↓
Local SSD
  ↓
Remote Storage
  ↓
Internet
```

| 방향 | 특성 |
|------|------|
| 위로 갈수록 | 빠름 / 비용 낮음 / latency 작음 |
| 아래로 갈수록 | 느림 / latency 큼 / 실패 가능성 증가 |

시스템 설계 핵심은 **필요한 데이터를 가능한 가까운 위치에 유지하는 것**이다.

그래서 등장하는 개념들: `Cache`, `Prefetch`, `Buffer`, `Replica`, `CDN`, `Read-through architecture`

### Fetch는 동기(Synchronous) 비용

> Fetch가 끝날 때까지 다음 작업이 멈춘다.

즉 **Fetch latency는 전체 시스템 응답시간에 직접 연결**된다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU Stall / Wait
```bash
top
htop
vmstat 1
mpstat -P ALL 1
```

### Disk Fetch 상태
```bash
iostat -x 1
iotop
```

### Memory Fetch 상태
```bash
free -h
sar -B 1
```

### Network Fetch 상태
```bash
ss -s
netstat -s
iftop
tcpdump -i eth0
```

### Runtime 관찰 포인트
- `cache hit ratio`
- `fetch latency`
- `query latency`
- `object loading`
- `page fault`

### Kubernetes Storage Fetch
- `PV/PVC latency`
- `CSI volume delay`
- `network storage latency`

### Kubernetes Remote Service Fetch
- `service-to-service call`
- `DNS lookup`
- `ingress latency`

### Observability 대표 지표
- `request latency`
- `P95 / P99`
- `retry count`
- `timeout rate`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*