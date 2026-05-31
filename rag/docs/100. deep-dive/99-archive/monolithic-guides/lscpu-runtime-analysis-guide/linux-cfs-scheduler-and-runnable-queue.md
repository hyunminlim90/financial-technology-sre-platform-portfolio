# Linux CFS Scheduler와 Runnable Queue

## 1. 개요

CFS(Completely Fair Scheduler)는 Runnable 상태의 `task_struct`를 관리하고 다음 실행 대상을 선택한다.

Runnable Queue는 단순한 FIFO 구조가 아니라 `vruntime` 기반의 **Red-Black Tree**로 구현된다.

---

## 2. vruntime (Virtual Runtime)

CFS는 각 `task_struct`에 대해 `vruntime` 값을 유지한다. `vruntime`은 해당 task가 CPU를 사용한 누적 실행 시간이다.

| 상태 | vruntime | CFS 우선순위 |
|------|----------|-------------|
| CPU를 적게 사용한 task | 낮음 | 높음 (우선 실행 대상) |
| CPU를 많이 사용한 task | 높음 | 낮음 |

CFS는 `vruntime`이 가장 낮은 `task_struct`를 우선 실행 대상으로 선택한다.

---

## 3. Red-Black Tree 기반 Runnable Queue

`task_struct`는 `vruntime` 값을 기준으로 Red-Black Tree에 정렬된다.

```
Red-Black Tree

  Left Node                Right Node
  vruntime 낮음            vruntime 높음
  (CPU 사용량 적음)         (CPU 사용량 많음)
       ↑
  다음 실행 대상
```

트리의 **가장 왼쪽 노드**(`vruntime` 최솟값)가 다음 실행 대상으로 선택된다.

---

## 4. CFS 스케줄링 흐름

```
① task_struct 선택
   Red-Black Tree 최좌측 노드 선택 (vruntime 최솟값)

② Logical CPU 배치
   선택된 task_struct → Logical CPU → Physical Core 실행

③ vruntime 증가
   실행 시간에 비례해 vruntime 값 증가

④ Context Switch 및 재등록
   Time Slice 종료 → 현재 task_struct 상태 저장
   → 증가된 vruntime 기준으로 Red-Black Tree에 재배치
   → 다음 task_struct 상태 복원 → Logical CPU 실행 재개
```

---

## 5. Context Switch와 Runnable Queue의 관계

Context Switch 발생 시 Kernel이 수행하는 작업이다.

```
현재 task_struct 상태 저장
  - Register Set
  - Program Counter (PC)
  - Stack Pointer

다음 task_struct 상태 복원

Logical CPU 실행 대상 교체
```

Context Switch 이후 직전 task_struct는 증가된 `vruntime`으로 Runnable Queue에 재등록된다.

---

## 6. Software Thread와 Runnable Queue

Java Thread, Worker Thread, Netty Thread, Kafka Consumer Thread 등 모든 Software Thread는 `task_struct`로 실체화되어 Runnable Queue에 등록된다.

```
Java Thread
  ↓ pthread_create()
  ↓ clone()
  ↓ task_struct 생성
  ↓ Runnable Queue 등록
  ↓ CFS 스케줄링
```

---

## 7. Runnable Queue 과부하 시 발생 문제

Runnable 상태의 `task_struct`가 과도하게 증가하면 Runnable Queue 적체가 발생한다.

| 현상 | 원인 |
|------|------|
| Context Switch 증가 | Runnable `task_struct` 과다 |
| CPU 사용률 증가 | Scheduler Overhead 증가 |
| Throughput 감소 | 실행보다 task 전환 비용이 더 커짐 |
| Load Average 증가 | Runqueue 적체 |
| Latency 증가 | CPU 대기 시간 증가 |

---

## 8. 핵심 정리

| 항목 | 내용 |
|------|------|
| Runnable Queue 구조 | `vruntime` 기반 Red-Black Tree |
| 다음 실행 대상 선택 기준 | `vruntime` 최솟값 (트리 최좌측 노드) |
| vruntime 갱신 시점 | task 실행 중 지속 증가, Context Switch 후 재정렬 |
| Context Switch 역할 | 현재 task 상태 저장 → 다음 task 상태 복원 → 실행 대상 교체 |
| 과부하 시 위험 | Scheduler Overhead 증가, CPU Saturation, Latency 증가 |

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*