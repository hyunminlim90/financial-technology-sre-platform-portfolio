## CPU Stall이란?

```
CPU가 명령어를 계속 실행하지 못하고,

데이터 준비 지연,
메모리 접근(Cache Miss),
분기 처리(Branch Prediction 실패) 등을

기다리면서

실제 연산이 일시적으로 멈추거나 지연되는 상태
```

대표적인 원인:

| 원인                       | 설명                                                        |
| ------------------------ | --------------------------------------------------------- |
| **Data Dependency**      | 이전 명령어의 연산 결과가 아직 준비되지 않아 다음 연산이 대기하는 상태                  |
| **Cache Miss**           | 필요한 데이터가 CPU Cache(L1/L2/L3)에 없어 RAM에서 데이터를 가져오느라 지연되는 상태 |
| **Branch Misprediction** | CPU의 분기 예측이 실패하여 잘못 실행한 명령어를 폐기하고 다시 실행하는 상태              |
| **Memory Latency**       | 메모리 접근 자체가 느려 CPU가 데이터를 기다리는 상태                           |
| [**I/O Wait**](../20-deep-dive/io-operations.md)             | 디스크·네트워크·파일 시스템 응답을 기다리며 CPU 작업 진행이 지연되는 상태               |
| [**Lock Contention**](../20-deep-dive/lock-contention.md)      | 여러 Software Thread가 동일 Lock(Mutex/Spinlock)을 경쟁하면서 대기하는 상태         |
| **CPU Throttling**       | Linux CFS Quota 제한으로 Container 실행이 일시적으로 제한되는 상태          |
| **Context Switch**       | Scheduler가 실행 Thread를 교체하면서 발생하는 CPU 전환 비용                |
| **NUMA Remote Access**   | 다른 NUMA Node의 메모리에 접근하면서 메모리 지연이 증가하는 상태                  |

즉:

```
연산 진행이 중간에 계속 끊기는 상태
```

에 가깝습니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*