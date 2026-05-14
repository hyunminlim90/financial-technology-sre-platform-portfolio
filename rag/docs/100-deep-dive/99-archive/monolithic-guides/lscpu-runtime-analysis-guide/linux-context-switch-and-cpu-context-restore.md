# Linux Context Switch와 CPU Context 복구

## 1. 개요

Context Switch는 현재 실행 중인 `task_struct`의 CPU 실행 상태(CPU Context)를 저장하고, 다음 실행할 `task_struct`의 CPU Context를 복구하여 실행 흐름을 이어가는 과정이다.

이 메커니즘을 통해 하나의 Logical CPU 위에서 여러 Software Thread가 번갈아 실행될 수 있다.

---

## 2. Runnable → Running 상태 전환

CFS는 `vruntime` 기준으로 Runnable 상태의 `task_struct` 중 실행 대상을 선택하고, Context Switch를 통해 CPU 실행 권한을 전환한다.

| 상태 | 설명 |
|------|------|
| Runnable | 실행 준비 완료, 아직 CPU를 점유하지 못한 상태 |
| Running | Logical CPU를 점유하여 명령어를 실행 중인 상태 |

---

## 3. CPU Context 구성 요소

Context Switch 시 저장/복구되는 CPU 상태 정보다. 모든 항목이 복구되어야 이전 실행 지점부터 정확히 이어서 실행할 수 있다.

| 항목 | 설명 |
|------|------|
| Program Counter (PC) | 다음에 실행할 명령어의 주소 |
| Stack Pointer (SP) | 현재 Stack 위치 (함수 호출 상태, 지역 변수, 반환 주소 포함) |
| General Purpose Registers | 연산 중 사용하던 임시 데이터 (`rax`, `rbx`, `rcx`, `rdx` 등) |
| FLAGS Register | 조건 분기에 필요한 CPU 상태 (Zero Flag, Carry Flag, Sign Flag 등) |
| Page Table 정보 (CR3) | 프로세스의 가상 메모리 매핑 정보 (MMU 기준) |

---

## 4. Context Switch 내부 흐름

```
CFS Scheduler: 다음 실행 대상 선택 (vruntime 기준)
  ↓ 현재 task_struct의 CPU Context 저장
  ↓ 다음 task_struct의 CPU Context 복구
  ↓ Running 상태 전환
  ↓ Logical CPU 실행
  ↓ JVM C++ Entry Point
  ↓ Java Thread run() 실행
```

---

## 5. 동일 프로세스 vs 프로세스 간 전환 비용

| 구분 | 특징 | 비용 |
|------|------|------|
| 동일 프로세스 내 Thread 전환 | 같은 메모리 공간 공유, Page Table 교체 없음 | 상대적으로 낮음 |
| 다른 프로세스 간 전환 | 주소 공간이 다름, CR3/Page Table 교체 발생, TLB Flush 가능성 | 상대적으로 높음 |

---

## 6. Context Switch 증가 시 발생 문제

Context Switch 자체에도 CPU 비용이 발생한다. 과도하게 증가하면 실제 비즈니스 로직보다 스케줄링 비용이 더 커지는 상황이 발생할 수 있다.

| 현상 | 원인 |
|------|------|
| CPU 사용률 증가 | Context Switch 자체 비용 누적 |
| Throughput 저하 | 스케줄링 오버헤드가 실행 시간을 잠식 |
| Cache Miss 증가 | CPU Cache 오염 |
| Latency 증가 | 실행 흐름 전환 비용 증가 |

### 주요 원인

- Runnable `task_struct` 수 과다
- Thread 수가 CPU Core 수 대비 과다
- Busy Waiting
- Lock Contention 증가

### 확인 방법

```bash
vmstat 1
# cs(context switches) 항목이 비정상적으로 높으면
# 과도한 Thread 경쟁 또는 CPU Saturation 가능성
```

---

## 7. 핵심 정리

| 항목 | 내용 |
|------|------|
| Context Switch 목적 | CPU Context 저장 → 복구 → 실행 흐름 전환 |
| 저장/복구 대상 | PC, SP, Registers, FLAGS, Page Table(CR3) |
| 상태 전환 | Runnable → Running (CFS vruntime 기준 선택) |
| Thread 전환 vs 프로세스 전환 | Thread 전환이 Page Table 교체 없어 비용이 낮음 |
| 성능 위험 | Context Switch 과다 시 Scheduler Overhead로 Throughput 저하 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*