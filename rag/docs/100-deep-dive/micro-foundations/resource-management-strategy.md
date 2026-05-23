# 자원 관리 전략(Resource Management Strategy)

> 정독: 0회

## 1. 이 기술이 무엇인가

자원 관리 전략은:

> 소프트웨어 시스템이 제한된 컴퓨팅 자원을 안정적이고 효율적으로 사용하기 위해 정의하는 **운영 규칙과 제어 방식**

**대상 자원:**

- CPU
- 메모리
- 디스크
- 네트워크
- 스레드
- 파일 디스크립터
- 연결(Connection)

**핵심은:**

- 얼마나 사용할지
- 언제 할당할지
- 언제 반환할지
- 어떤 우선순위로 사용할지
- 과부하 시 어떻게 제한할지

를 시스템 차원에서 통제하는 것입니다.

---

## 2. 시스템 어디에서 등장하는가

자원 관리 전략은 거의 모든 시스템 계층에서 등장합니다.

| 계층 | 자원 관리 대상 |
|------|--------------|
| 운영체제 | CPU 스케줄링, 메모리 관리 |
| 런타임 | Heap, Thread |
| 데이터베이스 | Connection, Buffer Cache |
| 웹 서버 | Worker, Socket |
| Kubernetes | CPU/Memory Quota |
| 네트워크 시스템 | Queue, Rate Limit |

실제 운영 환경에서는 **"비즈니스 로직"** 보다 **"자원 관리 실패"** 가 더 많은 장애를 발생시킵니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

모든 자원에 직접 영향이 있습니다.

### CPU

관련 요소: 스레드 수, 스케줄링 정책, Busy Waiting, Context Switching

**영향:** CPU Saturation, Load Average 증가

### Memory

관련 요소: 캐시 전략, 객체 생명주기, 버퍼 크기, Pool 관리

**영향:** OOM, GC 증가, Swap 발생

### Network

관련 요소: Connection Pool, Backpressure, Queue, Retry 정책

**영향:** Timeout, Packet Drop, Connection Exhaustion

### Disk

관련 요소: Buffering, Flush 정책, I/O Queue, Write Strategy

**영향:** I/O Wait 증가, Throughput 저하, Latency 증가

---

## 4. 왜 중요한가

컴퓨팅 자원은 유한합니다.

자원 관리 전략이 없으면:

- 메모리 고갈
- CPU 폭주
- 스레드 폭증
- Queue 적체
- 네트워크 포화

가 발생합니다.

특히 대규모 시스템에서는 기능 구현 자체보다:

> **"자원을 어떻게 제한하고 재사용하는가"** 가 안정성을 결정합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

실제 운영 장애 대부분은 자원 관리 실패와 연결됩니다.

| 장애 | 원인 |
|------|------|
| OOM Kill | 메모리 전략 실패 |
| CPU 100% | Thread 폭증 |
| DB 장애 | Connection Pool 고갈 |
| 서비스 응답 지연 | Queue 적체 |
| 시스템 멈춤 | Deadlock |
| 파일 열기 실패 | FD(File Descriptor) 고갈 |
| Pod Eviction | K8s Memory 초과 |

SRE 관점에서는:

> **"장애 = 자원 통제 실패"** 인 경우가 매우 많습니다.

---

## 6. 핵심 메커니즘

### (1) 할당(Allocation)

시스템은 실행 중 자원을 요청합니다.

- 메모리 할당
- 스레드 생성
- 파일 열기
- 소켓 생성

운영체제 또는 런타임이 이를 승인합니다.

**핵심 문제:** 얼마나 빠르게 할당되는가, 얼마나 많이 생성되는가

### (2) 재사용(Reusing)

자원을 매번 새로 만들면 비용이 큽니다. 그래서 재사용 전략을 사용합니다.

| 전략 | 목적 |
|------|------|
| Thread Pool | 스레드 재사용 |
| Connection Pool | DB 연결 재사용 |
| Buffer Pool | 메모리 버퍼 재사용 |
| Cache | 반복 데이터 재사용 |

재사용은 CPU 비용 감소, 메모리 단편화 감소, 생성/삭제 비용 감소에 중요합니다.

### (3) 제한(Limiting)

무제한 자원 사용은 시스템 붕괴로 이어집니다. 그래서 제한 정책이 필요합니다.

| 제한 대상 | 전략 |
|----------|------|
| Thread | Max Thread |
| Connection | Pool Size |
| Memory | Heap Limit |
| Request | Rate Limit |

> **핵심:** "과부하 시 어디서 차단할 것인가"

### (4) 스케줄링(Scheduling)

여러 작업이 동시에 자원을 요구하면 순서를 정해야 합니다.

- CPU Scheduler
- I/O Scheduler
- Queue Scheduler

이 구조가 처리량, 응답시간, 공정성을 결정합니다.

### (5) 회수(Reclaiming)

사용 끝난 자원은 반드시 반환되어야 합니다.

반환 실패 시 발생하는 문제:

- Memory Leak
- FD Leak
- Zombie Process

운영체제와 런타임은 Reference Count, Garbage Collection, Resource Cleanup 등으로 자원을 회수합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 도구 | 관측 대상 |
|------|----------|
| top / htop | CPU, Thread |
| free | 메모리 |
| vmstat | Swap, Memory |
| iostat | Disk I/O |
| sar | 시스템 자원 추세 |
| lsof | 열린 파일 |
| ss / netstat | Socket 상태 |
| ulimit | 자원 제한 |

### Runtime

| 영역 | 관측 |
|------|------|
| Heap Usage | 메모리 사용량 |
| Thread Count | 스레드 수 |
| GC 상태 | 메모리 회수 |
| Connection Pool | 연결 사용량 |
| Queue Depth | 적체 상태 |

### Kubernetes

| 대상 | 의미 |
|------|------|
| requests | 최소 보장 자원 |
| limits | 최대 사용 자원 |
| HPA | 자동 확장 |
| OOMKilled | 메모리 전략 실패 |
| Eviction | 노드 자원 부족 |
| QoS Class | 자원 우선순위 |

Kubernetes 운영 핵심도 결국 **자원 관리 전략**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*