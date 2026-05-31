# Implementation Fault (구현 결함)
## **Micro Foundations — 범용 시스템 관점**

> 정독: 0회

## 1. 이 기술이 무엇인가

**Implementation Fault(구현 결함)**는:

> 설계된 시스템 구조와 규칙을 실제 코드와 실행 로직으로 구현하는 과정에서 유입된 **논리적·기계적 실수**

즉:

- 설계 자체는 맞을 수 있다.
- 아키텍처 방향도 맞을 수 있다.
- 데이터 흐름도 맞을 수 있다.

하지만 **실제 구현 과정에서 개발자가 잘못 코딩한 상태**가 바로 구현 결함이다.

**대표 예시:**

| 유형 | 예시 |
|------|------|
| 참조 오류 | null 참조 |
| 자원 관리 | close() 누락 |
| 제어 흐름 | 잘못된 조건문 |
| 재시도 | 잘못된 retry |
| 범위 오류 | off-by-one |
| 동시성 | race condition |
| 동기화 | lock 누락 |
| 타임아웃 | timeout 처리 누락 |

> **핵심:** Implementation Fault는 "실행 로직"의 결함이다.

---

## 2. 시스템 어디에서 등장하는가

구현 결함은 거의 **모든 시스템 레이어**에서 등장한다.

### 애플리케이션 로직
- 잘못된 분기 처리
- 상태 업데이트 오류
- validation 누락

### 메모리 관리
- memory leak
- dangling reference
- resource leak

### 네트워크 처리
- retry storm 유발
- timeout 미처리
- connection close 누락

### 동시성 처리
- lock 누락
- race condition
- deadlock

### 파일/스토리지 처리
- partial write
- flush 누락
- sync 처리 누락

### 운영 자동화 스크립트
- 잘못된 cleanup script
- destructive command
- rollback 누락

> **결론:** Implementation Fault는 설계가 실제 실행 코드로 변환되는 **모든 구간**에서 발생 가능하다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

구현 결함은 특정 자원을 **직접 오염**시키는 경우가 많다.

### CPU 영향
- **원인:** infinite loop, retry loop, busy waiting
- **결과:** CPU saturation, scheduling pressure, throttling

### Memory 영향
- **원인:** memory leak, queue accumulation, object retention
- **결과:** memory pressure, GC storm, OOM

### Network 영향
- **원인:** connection leak, uncontrolled retry, packet amplification
- **결과:** bandwidth exhaustion, latency spike, timeout cascade

### Disk 영향
- **원인:** excessive logging, write amplification, flush storm
- **결과:** I/O bottleneck, storage saturation, persistence latency

> **핵심:** 구현 결함은 실행 시점에서 **실제 자원 소비 이상 현상**으로 나타난다.

---

## 4. 왜 중요한가

구현 결함은 **작게 보여도 실행 환경에서는 증폭**된다.

특히 다음 환경에서 위험하다:

- high concurrency
- distributed system
- asynchronous processing
- large-scale traffic

**증폭 예시:**

```
connection close 누락
  ↓
몇 시간 동안 누적
  ↓
connection pool exhaustion
  ↓
DB timeout 증가
  ↓
request queue 증가
  ↓
latency spike
  ↓
system-wide failure
```

> Implementation Fault는 **"작은 실수의 증폭 메커니즘"**이다.

또한, 구현 결함은 **정상 상황에서는 잘 숨어 있다.**
낮은 부하, 적은 동시성, 작은 데이터량에서는 우연히 정상처럼 보일 수 있기 때문이다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 장애 상당수는 구현 결함에서 **직접** 시작된다.

### 1) Memory Leak Fault
```
resource 반환 누락 → memory accumulation → memory pressure → OOM Kill → service restart
```

### 2) Connection Leak Fault
```
connection close 누락 → pool exhaustion → DB request blocking → timeout cascade → system latency explosion
```

### 3) Race Condition Fault
```
동시성 보호 누락 → shared state corruption → integrity violation → financial inconsistency
```

### 4) Retry Loop Fault
```
에러 발생 시 무한 retry → traffic amplification → network saturation → dependency collapse
```

### 5) Error Handling Fault
```
예외 처리 누락 → unexpected crash → process termination → service unavailable
```

> **결론:** Implementation Fault는 실행 중 실제 Error를 **가장 직접적으로** 유발한다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

구현 결함의 핵심은:

> **실행(Runtime) 시점에서만 드러난다.**

컴파일은 통과할 수 있다. 테스트도 일부 통과할 수 있다.

하지만 실제 운영 환경에서는 다음 조건이 존재한다:

- concurrency
- latency
- load / spike
- fault injection

이 때문에 구현 결함은 **운영 환경에서 증폭**된다.

### 핵심 메커니즘

| 메커니즘 | 설명 |
|----------|------|
| **State Corruption** | 잘못된 상태 갱신 |
| **Resource Exhaustion** | 자원 반환 실패 |
| **Unbounded Growth** | queue/memory 무한 증가 |
| **Synchronization Failure** | 동시성 제어 실패 |
| **Error Propagation** | 에러 전파 차단 실패 |
| **Timing Sensitivity** | 특정 timing에서만 발생 |

> **핵심 개념:** Implementation Fault는 "코드가 존재하는 방식"의 문제라기보다, **"코드가 실행되는 방식"**의 문제다.

---

## 7. Linux/Runtime/K8s에서 어디서 관측되는가

구현 결함은 **운영 지표 이상**으로 관측된다.

### Linux

**메모리 이상**
```bash
free -h
vmstat
sar -r
```
관찰: memory growth, swap 증가, reclaim 증가

**CPU 이상**
```bash
top
uptime
pidstat
```
관찰: runaway process, abnormal CPU usage, excessive context switching

**파일 디스크립터 누수**
```bash
lsof
ulimit -n
```
관찰: FD exhaustion, socket leak, connection accumulation

**네트워크 이상**
```bash
ss -s
netstat
```
관찰: TIME_WAIT explosion, connection leak, retransmission 증가

---

### Runtime

관찰 포인트:
- unexpected exception
- queue accumulation
- thread starvation
- deadlock
- retry amplification
- latency spike

---

### Kubernetes

| 증상 | 명령어 | 관찰 |
|------|--------|------|
| **OOMKilled** | `kubectl describe pod` | memory leak, unbounded allocation |
| **CrashLoopBackOff** | `kubectl get pods` | startup failure, unhandled exception, runtime crash |
| **Throttling** | `kubectl top pod` | CPU runaway, busy loop |
| **Restart Count 증가** | `kubectl get pods` | unstable runtime, repeated process failure |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*