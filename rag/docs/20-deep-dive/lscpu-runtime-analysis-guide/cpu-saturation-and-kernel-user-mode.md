# CPU Saturation, Context Switch, Blocking I/O, User Mode / Kernel Mode

## 1. 개요

Linux 기반 시스템에서 CPU Saturation은 단순히 CPU 사용률이 높은 상태만을 의미하지 않습니다.

특히 Thread-per-request 기반 Blocking 구조에서는 실제 비즈니스 로직보다 아래 항목들이 CPU 자원을 과도하게 소비하면서 시스템 전체 Throughput이 급격히 저하될 수 있습니다.

- Context Switch
- Scheduler Overhead
- Mode Transition (User ↔ Kernel)
- CPU Cache Miss

---

## 2. Thread-per-request 모델

하나의 요청(Request)마다 하나의 Thread가 전담하여 처리하는 구조입니다. 대표 사례로는 Tomcat 기반 Spring MVC가 있습니다.

```
HTTP Request 도착
  ↓ Thread Pool에서 Thread 할당
  ↓ Controller → Service → DB/API 호출
  ↓ 응답 반환
  ↓ Thread Pool 복귀
```

요청 하나가 끝날 때까지 동일한 `task_struct`가 전체 흐름을 담당합니다.

### Blocking I/O에서의 문제

I/O 대기 구간에서 Thread는 실제 연산을 수행하지 못합니다. 그러나 아래 자원은 계속 유지됩니다.

- Stack 메모리
- `task_struct`
- Scheduler 관리 대상

동시 요청 수가 증가할수록 Runnable 및 Sleeping 상태의 `task_struct` 수가 급격히 증가합니다.

---

## 3. Context Switch

현재 실행 중인 `task_struct`의 CPU Context를 저장하고, 다음 `task_struct`의 CPU Context를 복구하여 실행 흐름을 전환하는 과정입니다.

### Context Switch 시 저장/복구 항목

| 항목 | 역할 |
|------|------|
| Program Counter (PC) | 다음 실행 명령어 위치 |
| Stack Pointer (SP) | Stack 상태 |
| General Purpose Registers | 연산 중간 데이터 |
| FLAGS Register | CPU 상태 플래그 |
| Memory Context (CR3) | 가상 메모리 매핑 정보 |

### Context Switch 발생 비용

| 비용 항목 | 설명 |
|----------|------|
| Register Save / Restore | 현재 task 상태 저장 및 다음 task 상태 복구 |
| Scheduler 실행 | CFS가 다음 `task_struct` 선택 |
| Kernel Mode 진입 | Scheduler는 Kernel 영역에서만 실행 가능 |
| CPU Cache 오염 | 새로운 task 실행 시 L1/L2 Cache 무효화, Cache Miss 및 RAM 접근 증가 |

---

## 4. CPU Saturation과 Scheduler Overhead

Runnable `task_struct`가 과도하게 증가하면 Scheduler가 다음 실행 대상 선택 자체에 CPU를 소비하게 됩니다.

| 현상 | 원인 |
|------|------|
| CPU 사용률 100% | Context Switch 과다 |
| Throughput 감소 | 실제 로직보다 스케줄링 비용이 더 커짐 |
| 응답 지연 증가 | Runnable Queue 적체 |
| Cache Miss 증가 | Thread 전환 과다로 Cache 오염 |

### Thrashing

극단적인 경우, CPU가 실제 비즈니스 로직보다 Thread 전환(Context Switch)에 대부분의 시간을 소비하는 상태가 됩니다. 이를 **Thrashing** 상태라고 합니다.

---

## 5. User Mode와 Kernel Mode

CPU는 실행 권한 수준을 구분하여 동작합니다.

| 구분 | 실행 영역 | 특징 |
|------|----------|------|
| User Mode | 일반 애플리케이션 코드 (Java, JVM, Spring 등) | 제한된 권한, Hardware 직접 접근 불가, System Call 필요 |
| Kernel Mode | Linux Kernel | Hardware 직접 제어, Scheduler 실행, Memory 관리, Interrupt 처리 가능 |

### Mode Transition

애플리케이션이 OS 기능을 요청하면 `User Mode → Kernel Mode` 전환이 발생합니다.

대표 사례: `File I/O`, `Network I/O`, `clone()`, `epoll()`, `read()`, `write()`

### Context Switch와 Kernel Mode의 관계

Context Switch는 Scheduler가 수행하므로, Thread 교체 시 반드시 Kernel Mode 진입이 발생합니다.

```
User Mode 실행 중
  ↓ Scheduler 호출
  ↓ Kernel Mode 진입
  ↓ Context Switch 수행
  ↓ 다음 task_struct 실행
  ↓ User Mode 복귀
```

Runnable task가 많을수록 Mode Transition 비용도 함께 증가합니다.

---

## 6. Blocking vs Non-blocking 구조 비교

| 항목 | Blocking (Thread-per-request) | Non-blocking (Event-loop) |
|------|-------------------------------|--------------------------|
| Thread 수 | 요청 수에 비례하여 증가 | CPU Core 수 중심으로 고정 |
| Context Switch | 매우 많음 | 매우 적음 |
| Scheduler 부담 | 큼 | 작음 |
| CPU Cache 효율 | 낮음 | 높음 |
| CPU 효율 | 낮음 | 높음 |

### Non-blocking 처리 흐름

```
I/O 요청
  ↓ 응답을 기다리지 않고 다른 이벤트 처리 계속 수행
  ↓ I/O 완료 시 Callback / Event 처리
```

Blocked 상태의 `task_struct` 증가 자체를 최소화하는 구조입니다.

### Linux Scheduler 관점 비교

```
[Blocking 구조]
Runnable task_struct 폭증
  ↓ Context Switch 증가
  ↓ Scheduler Overhead 증가
  ↓ CPU Saturation

[Non-blocking 구조]
적은 수의 task_struct 유지
  ↓ Context Switch 최소화
  ↓ CPU Cache 효율 증가
  ↓ 높은 Throughput 유지
```

---

## 7. SRE 관점 핵심 지표

### vmstat

```bash
vmstat 1
```

| 항목 | 의미 |
|------|------|
| `cs` | Context Switch 횟수 |
| `r` | Runnable task 수 |
| `wa` | I/O Wait |
| `us` / `sy` | User / System CPU 사용률 |

### Load Average 증가 원인

- Runnable `task_struct` 수 증가
- `D` 상태(Uninterruptible Sleep) `task_struct` 수 증가

### CPU Saturation 징후

아래 항목이 동시에 나타날 경우 Scheduler 병목 가능성을 의심해야 합니다.

- `cs` 값 비정상 증가
- `r` 값 지속 높음
- Load Average 지속 증가
- Throughput 저하

---

## 8. 최종 정리

| 항목 | 내용 |
|------|------|
| Thread-per-request 문제 | Blocking I/O 대기 중 `task_struct` 수 급증 |
| Context Switch 비용 | Register 저장/복구, Kernel Mode 진입, Cache Miss |
| CPU Saturation 원인 | Runnable task 과다로 Scheduler Overhead가 실행 비용 초과 |
| Thrashing | CPU 대부분을 Context Switch에 소비하는 상태 |
| Mode Transition | Thread 교체 시 반드시 Kernel Mode 진입 발생 |
| Non-blocking 이점 | 적은 `task_struct`로 Context Switch 최소화, CPU 효율 극대화 |