# 객체의 상태 (Object State)

> 정독: 0회

## 1. 이 기술이 무엇인가

**객체의 상태(Object State)** 는:

> 객체 내부 필드(Field)에 현재 저장되어 있는 실제 데이터 값

**예시:**

```
Account.balance = 120000
User.loggedIn   = true
Order.status    = "PAID"
```

여기서 `balance`, `loggedIn`, `status`에 저장된 실제 값 자체가 객체의 상태입니다.

**핵심:**
> 객체의 상태는 메모리에 저장된 현재 데이터 내용물

---

## 2. 시스템 어디에서 등장하는가

객체 상태는 거의 모든 애플리케이션 런타임에서 등장합니다.

| 영역 | 상태 예시 |
|------|-----------|
| 사용자 객체 | 로그인 여부 |
| 결제 객체 | 승인 상태 |
| 세션 객체 | 인증 토큰 |
| 캐시 객체 | cached value |
| 네트워크 객체 | connection state |
| 파일 객체 | file offset |
| 스레드 객체 | execution state |

> 프로그램의 대부분은 상태를 읽고 수정하는 과정입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **Memory** 입니다.

| 자원 | 영향 |
|------|------|
| Heap Memory | object storage |
| CPU Cache | state access |
| Memory Bus | load/store traffic |
| CPU | branch decisions |
| Disk | persistence serialization |

상태는 결국 **메모리 안의 비트 데이터**이므로 memory subsystem 영향이 큽니다.

---

## 4. 왜 중요한가

객체 상태는:

> 프로그램의 현재 상황과 역사 자체

프로그램은 상태 기반으로 동작합니다.

```java
if (account.balance > 0) { ... }
```

실행 흐름, 비즈니스 로직, 권한 판단, 장애 처리 — **모두 상태 기반**입니다.

> 상태가 달라지면 프로그램 동작도 달라짐

---

## 5. 실제 장애와 어떤 관련이 있는가

### Race Condition
멀티스레드 환경에서 동시에 상태를 수정하면 inconsistent state가 발생합니다.

```
balance = balance - 100  ← 동시 수행 시 데이터 손상 가능
```

### Stale State
캐시/복제 환경에서 오래된 상태를 읽어 잘못된 판단이 발생할 수 있습니다.

### Memory Corruption
잘못된 write로 인해 field overwrite, invalid pointer access가 발생할 수 있습니다.

### Distributed State Inconsistency
분산 시스템에서 replica 간 상태 불일치, eventual consistency delay가 발생할 수 있습니다.

### Partial Update
트랜잭션 실패 시 일부 상태만 변경되어 데이터 무결성이 붕괴될 수 있습니다.

### State Explosion
상태 조합이 많아질수록 예외 케이스 증가, 장애 복잡도가 증가합니다.

### Memory Leak
상태 객체가 계속 유지되면 heap growth → GC pressure → OOM으로 이어질 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 흐름

```
1) 객체 생성
   └─ 힙 메모리 할당 → object instance 생성

2) 필드 공간 확보
   └─ 객체 레이아웃 내부에 field slots 생성
      예: balance, status, flag

3) 상태 저장 (Store)
   └─ CPU가 store instruction 수행
      예: balance = 1000 → memory write

4) 상태 읽기 (Load)
   └─ 필요 시 memory load 수행
      예: if (balance > 0)

5) 상태 기반 분기
   └─ CPU: CMP / TEST / branch 수행
      → 객체 상태가 실행 흐름 결정

6) 상태 변경 반복
   └─ 프로그램은 계속 read → compute → write 반복

7) 상태 수명 종료
   └─ 참조가 끊기면 unreachable → garbage object 판정

8) 메모리 회수
   └─ GC 또는 allocator가 heap memory reclamation 수행
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Heap Usage 확인

```bash
top
htop
ps
pmap
```

### Memory Growth 확인

```bash
smem
vmstat
sar -r
```

### Cache/Mem Pressure 확인

```bash
perf
perf mem
```

### OOM 상황 확인

```bash
dmesg
journalctl
kubectl describe pod  # → OOMKilled
```

### Kubernetes 관점

상태 객체 과다 시:

```
heap growth → GC 증가 → memory limit 초과
```

특히 아래 워크로드에서 중요합니다.

- session-heavy app
- cache-heavy service
- object churn 높은 workload

### Runtime Level 관측 대상

| 항목 | 의미 |
|------|------|
| live objects | 살아있는 상태 객체 |
| retained size | 유지 메모리 |
| object graph | 상태 연결 구조 |
| heap dump | 전체 상태 스냅샷 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*