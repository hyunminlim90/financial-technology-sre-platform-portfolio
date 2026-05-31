# 엔드 유저 (End User)

> 정독: 0회

엔드 유저(End User)는:

> **컴퓨터 시스템, 네트워크, 운영체제, 애플리케이션, 클라우드 인프라 등 수많은 기술 계층의 최종 결과물을 실제로 사용하는 최종 소비 주체**

즉, **시스템이 존재하는 궁극적인 이유이자, 모든 컴퓨팅 리소스가 최종적으로 서비스를 제공해야 하는 대상**입니다.

대표 예시: 스마트폰 앱 사용자, 웹 사이트 방문자, 게임 플레이어, 쇼핑몰 구매자, 회사 ERP 입력 직원, 영상 시청자, 은행/결제 서비스 이용자

**핵심:**
엔드 유저는 시스템 내부 구조를 직접 제어하지 않고, 추상화된 UI/UX를 통해 결과만 소비하는 존재입니다.

---

## 1. 이 기술이 무엇인가

엔드 유저는 컴퓨터 시스템 계층 구조의 **최상위 최종 소비자**입니다.

---

## 2. 시스템 어디에서 등장하는가

엔드 유저는 모든 시스템의 최상위 계층에서 등장합니다.

```
반도체 → CPU → Memory → Kernel → Operating System
→ Runtime → Network Stack → Application → UI/UX → End User
```

하드웨어, 운영체제, 네트워크, 클라우드/쿠버네티스/SRE 모두 결국 **엔드 유저 경험 보호**가 목적입니다.

### 실무 영역별 엔드 유저 영향

| 영역 | 엔드 유저 영향 |
|------|--------------|
| 웹 서비스 | 페이지 응답 속도 |
| 결제 시스템 | 결제 성공 여부 |
| 게임 서버 | 지연(Lag), 끊김 |
| 스트리밍 | 버퍼링 |
| 모바일 앱 | 앱 크래시 |
| 클라우드 서비스 | SLA/SLO 체감 품질 |

---

## 3. 어떤 자원에 가장 영향이 큰가

엔드 유저 자체는 리소스가 아니라 **"리소스를 소비하는 최종 원인"** 입니다.

```
엔드 유저 행동 → 시스템 부하 발생 → CPU/Memory/Network/Disk 사용 증가
```

| 사용자 행동 | 주요 영향 자원 |
|------------|--------------|
| 페이지 접속 증가 | Network / CPU |
| 대량 요청 발생 | CPU / Memory |
| 파일 업로드 | Network / Disk |
| 대규모 다운로드 | Network |
| 검색 요청 증가 | CPU / Disk I/O |
| 영상 스트리밍 | Network Throughput |
| 게임 동시 접속 증가 | CPU / Network |

> 현대 분산 시스템에서는 **"엔드 유저 트래픽 패턴"이 전체 인프라 구조를 결정**합니다.

---

## 4. 왜 중요한가

엔드 유저는 **시스템 품질의 최종 판정자**이기 때문입니다.

서버 내부 지표가 아무리 좋아도 사용자가 느리다고 느끼면 장애이고, 결제가 실패하면 장애이며, 앱이 멈추면 장애입니다.

즉, 실무에서는 다음이 매우 중요합니다.

```
System Healthy  ≠  User Healthy
```

| 내부 상태 | 엔드 유저 체감 |
|----------|--------------|
| CPU 정상 | 페이지 느림 |
| Pod Running | 로그인 실패 |
| DB Alive | 결제 지연 |
| 네트워크 연결 OK | 패킷 손실 발생 |
| 서버 정상 | 모바일 앱 크래시 |

그래서 SRE/플랫폼 운영에서는 항상 **Latency, Error Rate, Availability, User Experience**를 함께 봅니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 장애는 대부분 **"엔드 유저 영향"으로 정의**됩니다.

| 장애 유형 | 엔드 유저 체감 |
|----------|--------------|
| CPU Saturation | 응답 느려짐 |
| GC Pause | UI 멈춤 |
| Packet Loss | 끊김 |
| DB Lock | 결제 지연 |
| DNS 장애 | 사이트 접속 불가 |
| Memory Leak | 서비스 다운 |
| Thread Exhaustion | 요청 무한 대기 |

> **장애 = 엔드 유저 기능 상실**
>
> 로그인 불가, 주문 실패, 결제 실패, 채팅 끊김, 동영상 재생 실패 등은 모두 엔드 유저 관점 장애입니다.

---

## 6. 핵심 메커니즘

### 6-1. 엔드 유저는 "추상화 소비자"이다

엔드 유저는 커널 syscall, TCP congestion control, CPU scheduling, Memory allocator 같은 내부 구조를 직접 보지 않습니다.

대신 버튼 클릭, 화면 터치, URL 입력 같은 **고수준 인터페이스**만 사용합니다.

즉, 운영체제와 애플리케이션은 복잡한 하드웨어를 **"안전하고 단순한 인터페이스"로 추상화하여 엔드 유저에게 제공**하는 역할을 합니다.

### 6-2. 모든 시스템은 결국 엔드 유저 요청 처리 파이프라인이다

```
User Action → UI → Application → Runtime → OS → Kernel
→ NIC → Network → Remote Server → DB/Storage → Response → User
```

엔드 유저 클릭 하나가 CPU interrupt, packet transmission, scheduler, syscall, storage I/O까지 모두 유발합니다.

### 6-3. 엔드 유저 경험(User Experience)이 최종 KPI다

현대 시스템에서는 TPS, CPU usage, Memory usage보다 더 중요한 것이:

> **"사용자가 실제로 정상 사용 가능한가?"**

입니다. 그래서 **SLO, SLA, Apdex, Real User Monitoring(RUM)** 같은 개념이 등장합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

엔드 유저 자체는 시스템 내부 객체가 아니지만, **엔드 유저 활동은 시스템 메트릭으로 관측**됩니다.

### Linux

```bash
# 시스템 상태 관측
top
htop
vmstat
iostat
sar
ss
netstat
```

엔드 유저 증가 시 CPU 급증, 메모리 사용 증가, TCP 연결 증가, 디스크 I/O 증가가 관측됩니다.

### Network

```bash
tcpdump
iftop
nload
wireshark
```

엔드 유저 요청 증가 시 SYN 증가, Throughput 증가, Retransmission 증가 등이 보입니다.

### Kubernetes

```bash
kubectl top pod
kubectl get hpa
kubectl describe ingress
```

동시 사용자 증가 → Pod Scale Out, 트래픽 급증, Latency 증가를 관측할 수 있습니다.

### Observability

현대 시스템에서는 **Prometheus, Grafana, OpenTelemetry, Jaeger, ELK** 등으로 최종 엔드 유저 경험을 추적합니다.

| 메트릭 | 의미 |
|--------|------|
| p95 Latency | 사용자 체감 지연 |
| Error Rate | 실패율 |
| Request Count | 사용자 요청량 |
| Session Count | 동시 사용자 |
| Availability | 서비스 사용 가능 여부 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*