# 계층화 (Layering)
## 1. 계층화(Layering)란 무엇인가

> 정독: 0회

계층화(Layering)는:

> 복잡한 시스템을 역할별로 분리하여 **독립된 여러 단계(Layer)로 나누는 설계 방식**

핵심은:

"각 계층은 자기 역할만 담당하고, 아래 계층의 서비스를 이용한다"

즉 **상위 계층은 "무엇을 할지"**, **하위 계층은 "어떻게 처리할지"** 를 담당합니다.

**가장 중요한 본질**

계층화의 진짜 목적은 **복잡성을 통제하기 위함**입니다.

현대 컴퓨터 시스템은 CPU · 메모리 · 디스크 · 네트워크 · 운영체제 · 런타임 · 컨테이너 · 클라우드 같은 수많은 요소가 동시에 동작합니다.

이걸 하나의 거대한 시스템으로 직접 다루면 **설계 / 유지보수 / 장애분석 자체가 불가능**해집니다. 그래서 기능별로 층을 나누는 것입니다.

---

## 2. 시스템 어디에서 등장하는가

계층화는 현대 컴퓨터 시스템 거의 모든 곳에 존재합니다.

**네트워크**

```
Application
Transport
Network
Data Link
Physical
```

인터넷 자체가 계층 구조입니다.

**운영체제**

```
User Space
System Call
Kernel
Driver
Hardware
```

응용 프로그램은 하드웨어를 직접 제어하지 않습니다.

**CPU / 메모리**

```
Register
L1 Cache
L2 Cache
L3 Cache
DRAM
Disk
```

메모리 계층 구조 자체도 Layering입니다.

**Kubernetes / Cloud**

```
Application
Container
Pod
Node
Cluster
Infrastructure
```

클라우드 플랫폼도 다층 구조입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

계층화는 사실상 **CPU / Memory / Network / Disk 전체 자원 구조에 모두 영향**을 줍니다.

다만 실제 운영에서는 특히 CPU Context Switching · Memory Copy · Network Encapsulation · Disk I/O Stack에 영향을 크게 줍니다.

**왜 그런가?**

계층이 많아질수록 **중간 처리 단계가 증가**하기 때문입니다.

```
Application
→ Runtime
→ Kernel Socket
→ TCP/IP
→ NIC Driver
→ NIC Hardware
```

각 단계마다 버퍼 복사 · Queue 이동 · Context Switch · Interrupt · Parsing이 발생할 수 있습니다.

---

## 4. 왜 중요한가

계층화는 현대 시스템이 존재할 수 있게 만든 **핵심 설계 원칙** 중 하나입니다.

### ① 독립성 확보

하위 구현이 바뀌어도 상위는 유지됩니다.

> 구리 랜선 → 광케이블로 바뀌어도  
> 브라우저 · 메신저 · 게임은 수정할 필요가 없습니다.

### ② 표준화 가능

계층 간 인터페이스만 맞으면 Intel CPU · AMD CPU · Linux · Windows · Cisco Switch가 서로 통신 가능합니다.

### ③ 장애 분석 단순화

문제를 계층별로 분리 가능합니다.

| 증상 | 의심 계층 |
|---|---|
| 링크 다운 | Physical |
| ARP 실패 | Data Link |
| Ping 실패 | Network |
| TCP Timeout | Transport |
| HTTP 500 | Application |

SRE에서 매우 중요합니다.

### ④ 시스템 확장 가능

새로운 기술을 특정 계층에만 추가 가능합니다.

> HTTP/1.1 → HTTP/2 → HTTP/3이 바뀌어도  
> Ethernet 물리 계층은 그대로 유지됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애는 대부분 **계층 경계(Layer Boundary)** 에서 발생합니다.

**물리 계층 장애**

원인: 케이블 불량 · NIC 오류 · CRC Error

증상: `packet loss` · `link flap` · `network unreachable`

**네트워크 계층 장애**

원인: Routing 오류 · MTU mismatch · ARP 실패

증상: `timeout` · `high latency` · `partial connectivity`

**애플리케이션 계층 장애**

원인: Thread Pool 고갈 · Memory Leak · Deadlock

증상: `HTTP 500` · `slow response` · `pod restart`

**SRE 핵심**

> 장애 분석은 결국  
> **"어느 계층에서 문제가 발생했는가"** 를 추적하는 과정입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

계층화에서 가장 중요한 메커니즘은 **추상화(Abstraction) + 캡슐화(Encapsulation)** 입니다.

**추상화**

상위 계층은 **하위 내부 구현을 몰라도 됩니다**.

> 브라우저는 광케이블인지 · Wi-Fi인지 · 위성망인지 몰라도  
> TCP/IP만 사용하면 됩니다.

**캡슐화**

각 계층은 데이터를 자기 형식으로 감쌉니다.

```
HTTP Data
→ TCP Segment
→ IP Packet
→ Ethernet Frame
→ Electrical Signal
```

이것을 **Encapsulation**이라고 합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

**Linux**

```bash
ip addr
ip route
ethtool
ss -tuln
tcpdump
```

**Runtime**

관측 요소: Thread · Heap · Event Loop · Socket Buffer

**Kubernetes**

```
Container
→ Pod
→ CNI Network
→ Node
→ Overlay Network
→ Physical NIC
```

```bash
kubectl describe pod
kubectl logs
kubectl top
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*