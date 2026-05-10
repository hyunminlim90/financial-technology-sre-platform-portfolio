# OS Thread와 Linux Thread의 관계

## 1. 핵심 차이

두 용어는 실무에서 혼용되지만 기술적으로 범위와 관점이 다르다.

| 구분 | OS Thread | Linux Thread |
|------|-----------|--------------|
| 범위 | 운영체제 일반 개념 | Linux Kernel의 구체적 구현 |
| 관리 주체 | OS Kernel (종류 무관) | Linux Kernel |
| 실행 단위 | Kernel 수준 실행 단위 | `task_struct` / LWP |
| 스케줄러 | Kernel Scheduler | CFS Scheduler |
| 식별 방식 | OS마다 다름 | TID / TGID |
| 적용 범위 | Linux, Windows, macOS 등 | Linux 전용 |

```
OS Thread  ⊃  Linux Thread
```

OS Thread는 상위 개념이고, Linux Thread는 Linux 환경에서의 구체적인 구현체다.

---

## 2. OS Thread

운영체제가 직접 생성하고 스케줄링하는 커널 수준 실행 단위를 의미한다. 운영체제 종류에 관계없이 사용하는 일반적인 개념이다.

**사용 문맥:** OS 구조나 플랫폼 독립적인 설명을 할 때 사용한다.

- Thread Scheduling, Context Switching
- Kernel-level Thread, OS Architecture

---

## 3. Linux Thread

Linux Kernel이 구현한 실제 스레드 실행 단위다.

Linux에서는 Process와 Thread를 모두 `task_struct`라는 동일한 Kernel 구조체로 관리한다.

```
Linux Thread = LWP (Lightweight Process) = task_struct
```

### Linux의 설계 특징

Linux Kernel은 Process와 Thread를 별개 객체로 구분하지 않는다. `clone()` 시스템 콜의 플래그 조합으로 동작이 결정된다.

| 조건 | 동작 |
|------|------|
| 메모리를 공유하지 않음 | Process로 동작 |
| 메모리를 공유함 | Thread(LWP)로 동작 |

**사용 문맥:** Linux 내부 구현을 구체적으로 설명할 때 사용한다.

- `task_struct`, `clone()`, `pthread`, CFS
- TID/TGID, `ulimit`, `threads-max`, `cgroup pids.max`

---

## 4. Java Thread와의 연결

Java Thread는 최종적으로 Linux Thread(task_struct)로 실체화된다.

```
Java Thread
  ↓ JVM Native Layer
  ↓ pthread_create()
  ↓ clone()
  ↓ task_struct (Linux Thread / LWP)
  ↓ CFS Scheduler
  ↓ Logical CPU
```

---

## 5. 실무에서 혼용되는 이유

대부분의 서버 환경이 Linux 기반이기 때문에 두 용어를 사실상 동일한 의미로 사용하는 경우가 많다.

```
OS Thread ≈ Linux Thread  (Linux 환경 한정)
```

JVM Thread 분석, Thread Dump, Context Switching, CFS 분석 등 Linux 중심 운영 환경에서는 구분 없이 사용해도 대부분 같은 대상을 가리킨다.

---

## 6. 계층 구조 정리

| 계층 | 실행 단위 |
|------|----------|
| Application | Java Thread / Go Routine |
| Runtime | JVM Thread Object |
| Library | pthread |
| Kernel (일반론) | OS Thread |
| Linux Kernel 구현 | Linux Thread (`task_struct` / LWP) |
| Scheduler | CFS |
| Hardware | Logical CPU / Physical Core |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*