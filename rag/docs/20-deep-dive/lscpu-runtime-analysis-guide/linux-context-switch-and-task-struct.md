# Linux에서의 Context Switch와 task_struct

## 1. 시분할(Time Sharing)과 Context Switch 개요

Physical CPU는 한 순간에 하나의 명령어 흐름만 실행할 수 있다. 하나의 Logical CPU 위에서 여러 Software Thread가 실행될 수 있는 이유는 CFS가 CPU 실행 시간을 짧은 단위(Time Slice)로 분할해 여러 `task_struct`에 번갈아 할당하기 때문이다.

```
Runnable task_struct
  ↓ CFS Runqueue 등록
  ↓ vruntime 기준 선택
  ↓ Logical CPU 배치
  ↓ Physical Core 실행
  ↓ Time Slice 종료 또는 높은 우선순위 task 등장
  ↓ Context Switch 발생
```

---

## 2. Context Switch 과정

Context Switch 시 Kernel은 현재 실행 중인 `task_struct`의 CPU 상태를 저장하고, 다음 `task_struct`의 상태를 복원한다.

```
현재 task_struct 실행 중
  ↓ Time Slice 종료 또는 선점(Preemption) 발생
  ↓ 현재 task_struct 상태 저장
      - Register Set
      - Program Counter (PC)
      - Stack Pointer
      - CPU Flags
  ↓ 다음 task_struct 상태 복원
  ↓ Logical CPU가 새로운 task_struct 실행 시작
```

---

## 3. task_struct 구조

`task_struct`는 하나의 실행 단위에 대한 상태 정보를 저장하는 Linux Kernel의 핵심 자료구조다. CFS Scheduler는 이 구조체를 기준으로 CPU 실행 순서를 결정한다.

| 항목 | 설명 |
|------|------|
| 실행 상태 (State) | RUNNING / SLEEPING / WAITING 등 |
| PID / TGID | 실행 단위 식별자 |
| Register 상태 | CPU 실행 상태 |
| Stack 정보 | Kernel Stack / User Stack |
| Scheduling 정보 | vruntime 등 |
| Memory 정보 | mm_struct (주소 공간) |
| CPU Affinity | 실행 가능한 Logical CPU 범위 |
| Parent / Child 관계 | 프로세스 계층 정보 |

---

## 4. Software Thread 유형별 실행 특성

Java Thread, Worker Thread, Kafka Consumer Thread, Netty Event Loop Thread 등 모든 Software Thread는 Linux Kernel 수준에서 `task_struct`로 실체화된다. CFS Scheduler는 `task_struct`를 스케줄링 단위로 관리하며 Java Thread 자체를 직접 인식하지 않는다.

### Netty / Kafka Thread

네트워크 I/O 중심 작업이 많다. 외부 데이터 도착을 기다리는 동안 `task_struct`는 WAITING 상태로 전환되며 CPU를 점유하지 않는다.

### Worker / Java Thread

비즈니스 로직, 데이터 처리, 계산 작업을 수행한다. CPU 사용 시간이 길어질 경우 Time Slice 종료 시점에 Context Switch가 발생할 수 있다.

### GC Thread

Heap 메모리 탐색 및 정리 작업을 수행한다. 특정 GC 구간에서는 다수의 `task_struct` 실행이 중단되고 CPU 사용 비중이 급격히 증가할 수 있다.

---

## 5. Context Switch 증가 시 발생 문제

Runnable 상태의 `task_struct` 수가 과도하게 증가하면 Context Switch 자체가 성능 병목이 된다. CPU가 실제 비즈니스 로직(`run()`) 실행보다 task 전환에 더 많은 시간을 소모하는 상황이 발생할 수 있다.

| 현상 | 원인 |
|------|------|
| CPU 사용률 증가 | Runnable `task_struct` 과다로 인한 전환 비용 증가 |
| Throughput 감소 | Context Switch 오버헤드 증가 |
| Latency 증가 | Runqueue 적체로 실행 대기 시간 증가 |
| Load Average 증가 | CPU 대기 task 수 증가 |

---

## 6. 핵심 정리

| 항목 | 내용 |
|------|------|
| Software Thread의 실체 | Linux Kernel 수준에서 `task_struct`로 실체화 |
| CFS 스케줄링 단위 | `task_struct` (Java Thread를 직접 인식하지 않음) |
| Context Switch 목적 | 하나의 Logical CPU에서 여러 `task_struct`를 순차 실행 |
| Context Switch 동작 | 현재 `task_struct` 상태 저장 → 다음 `task_struct` 상태 복원 |
| 성능 위험 | Runnable `task_struct` 과다 시 CPU Saturation 및 성능 저하 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*