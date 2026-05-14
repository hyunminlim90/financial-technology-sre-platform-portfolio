# Linux Kernel의 task_struct 상태 관리

## 1. 개요

Linux Kernel에서 `task_struct`는 Process와 Thread를 포함한 모든 실행 단위를 표현하는 핵심 자료구조입니다.

CFS Scheduler는 각 `task_struct`의 상태(State)를 지속적으로 추적하며 CPU 자원을 분배합니다. 상태 전환은 Scheduler, Wait Queue, Interrupt, I/O Subsystem과 긴밀하게 연결되어 동작합니다.

---

## 2. task_struct 주요 상태

Linux Kernel 내부(`include/linux/sched.h`) 기준 상태 목록입니다.

| 상태 | 의미 | Scheduler 관점 |
|------|------|---------------|
| `TASK_RUNNING` | 실행 중(Running) 또는 실행 가능(Runnable) 상태 | CPU 실행 대상 또는 Runnable Queue 대기 |
| `TASK_INTERRUPTIBLE` | 이벤트 대기 상태 (Interruptible Sleep) | Wait Queue 대기, Signal 수신 시 즉시 복귀 가능 |
| `TASK_UNINTERRUPTIBLE` | 하드웨어 응답 대기 상태 (Deep Sleep) | Wait Queue 대기, Signal로 깨어나지 않음 |
| `TASK_STOPPED` | SIGSTOP 등에 의해 정지된 상태 | 스케줄링 제외 |
| `EXIT_ZOMBIE` | 실행 종료 후 부모 프로세스 확인 대기 | task_struct 일부 유지 |
| `EXIT_DEAD` | 완전 종료 상태 | Kernel 메모리 제거 대상 |

---

## 3. Running vs Runnable

Linux 내부에서는 `TASK_RUNNING` 하나로 표현되지만, Scheduler 동작 관점에서는 두 상태를 구분합니다.

| 구분 | 설명 | 특징 |
|------|------|------|
| **Running** | 현재 Logical CPU를 점유하여 명령어를 실행 중인 상태 | CPU Register 사용 중, `run()` 코드 실행 중 |
| **Runnable** | 실행 준비 완료, 아직 CPU를 할당받지 못한 상태 | CFS Runnable Queue(Red-Black Tree)에 등록, `vruntime` 기준 대기 |

---

## 4. Sleeping(Blocked) 상태

CPU를 사용하지 않고 특정 이벤트를 기다리는 상태입니다. Linux는 이를 두 종류로 구분합니다.

### 4-1. TASK_INTERRUPTIBLE (Interruptible Sleep)

`ps` 상태 코드: `S`

Signal 수신 시 즉시 깨어날 수 있습니다.

- 네트워크 응답 대기
- `Thread.sleep()`
- `select` / `poll` / `epoll` 대기

### 4-2. TASK_UNINTERRUPTIBLE (Uninterruptible Sleep)

`ps` 상태 코드: `D`

Signal로도 즉시 깨어나지 않습니다.

- Disk I/O 대기
- Kernel 내부 Lock 대기
- Hardware 응답 대기

`D` 상태 task가 증가하면 아래 문제 가능성을 의심해야 합니다.

| 현상 | 가능한 원인 |
|------|-----------|
| Load Average 증가 | I/O 대기 task 누적 |
| Storage Latency 증가 | Disk 병목, NFS 지연 |
| Kernel Lock Contention | Lock 대기 task 증가 |
| I/O Saturation | Storage 처리 한계 초과 |

---

## 5. Runnable Queue vs Wait Queue

| 구분 | 자료구조 | 대상 상태 | 역할 |
|------|---------|----------|------|
| Runnable Queue | Red-Black Tree (vruntime 정렬) | Runnable | CFS가 다음 실행 대상 선택 |
| Wait Queue | 이벤트별 대기 목록 | Sleeping | 이벤트 발생 시 Runnable로 복귀 |

Wait Queue 복귀 트리거 예시: Network Packet 도착, Disk I/O 완료, Timer Expire, Lock 해제

---

## 6. 상태 전환 흐름

```
[생성]
  ↓ Runnable (CFS Runqueue 등록)
  ↓ Running  (Logical CPU 점유)
  ↓
  ├─ Time Slice 종료         → Runnable 재진입
  ├─ I/O 요청 / Sleep        → Sleeping (Wait Queue)
  │     └─ 이벤트 완료       → Runnable 복귀
  └─ 작업 완료               → EXIT_ZOMBIE
                                   ↓
                               EXIT_DEAD (메모리 제거)
```

---

## 7. Zombie와 Dead 상태

### EXIT_ZOMBIE

실행은 종료되었지만 부모 프로세스가 종료 상태를 아직 수집하지 않은 상태입니다.

- 대부분의 자원은 정리되었지만 `task_struct` 일부가 유지됩니다.
- 부모가 `wait()` 호출 시 제거됩니다.

### EXIT_DEAD

완전히 종료되어 Kernel 메모리에서 제거되기 직전 상태입니다.

- Scheduler 대상이 아닙니다.
- 최종 자원 정리 단계입니다.

---

## 8. Runnable → Running 전환 시 수행 작업

Context Switch를 통해 다음 항목이 복구된 후 `run()` 코드가 실행됩니다.

| 복구 항목 | 설명 |
|----------|------|
| CPU Register | 연산 중간 상태 복원 |
| Program Counter (PC) | 다음 실행 명령어 주소 복원 |
| Stack Pointer (SP) | Stack 위치 복원 |
| Memory Context | 주소 공간 정보 복원 |

---

## 9. SRE 관점 핵심 포인트

### Load Average 증가 원인

Load Average에는 **Runnable** 상태와 **Uninterruptible Sleep(D)** 상태 task가 모두 포함됩니다. CPU를 실제 사용하는 task뿐 아니라 I/O 대기 task도 Load Average 증가 원인이 됩니다.

### Context Switch 증가 시 발생 문제

| 현상 | 원인 |
|------|------|
| CPU 사용률 증가 | Context Switch 자체 비용 누적 |
| Throughput 감소 | 스케줄링 오버헤드가 실행 시간을 잠식 |
| Cache Miss 증가 | CPU Cache 오염 |
| Latency 증가 | 실행 흐름 전환 비용 증가 |

---

## 10. 최종 정리

| 항목 | 내용 |
|------|------|
| 기본 자료구조 | `task_struct` (Process/Thread 공통) |
| 스케줄링 단위 | Runnable 상태의 `task_struct` |
| Runnable Queue | Red-Black Tree (`vruntime` 기준 정렬) |
| Sleeping 관리 | Wait Queue (이벤트 발생 시 Runnable 복귀) |
| 상태 순환 | Runnable ↔ Running ↔ Sleeping → Zombie → Dead |
| SRE 주요 지표 | `D` 상태 증가(I/O 병목), Context Switch 급증(Thread 과다) |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*