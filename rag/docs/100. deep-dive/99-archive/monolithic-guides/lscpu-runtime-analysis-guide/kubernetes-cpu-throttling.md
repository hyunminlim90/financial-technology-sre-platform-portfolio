## Kubernetes CPU Throttling 장애란?

Kubernetes에서 CPU **limit**를 설정하면  
Linux Kernel의 CFS(Completely Fair Scheduler)가
Container의 CPU 사용량을 일정한 "Quota(예산)" 기준으로 관리합니다.

## Linux CFS의 동작 방식

Linux CFS는 일반적으로:

```bash
cat /sys/fs/cgroup/cpu/cpu.cfs_period_us

100ms (cpu.cfs_period_us = 100000)
```

단위로 CPU 사용량을 계산합니다.

예를 들어:

```yaml
resources:
  requests:
    cpu: "500m"

  limits:
    cpu: "500m"
```

이면,

Container는:

```text
100ms Period 안에서
50ms 분량의 CPU 연산 예산(Quota)
```

을 부여받습니다.

#### CPU 단위 (m) 와 Quota 의미

Kubernetes에서:

```text
1000m = 1 CPU
500m = 0.5 CPU
````

를 의미합니다.

여기서 1 CPU는 일반적으로:

```text
Host Linux가 인식하는
논리 CPU(Logical CPU) 1개
= vCPU 1개
= Hyper-thread(CPU 하드웨어의 논리 실행 단위) 1개
```

에 해당하는 연산 능력을 의미합니다.

왜 Logical CPU 기준으로 설명하는가:

```text
Linux Scheduler(CFS)는 
Logical CPU 단위로 Software Thread를 스케줄링하고
CPU Time을 계산하기 때문에,

일반적으로 Logical CPU(vCPU) 기준으로 이해하는 것이 가장 정확합니다.
```

중요한 점은:

```text
50ms 분량의 CPU 연산 예산(Quota)은

CPU 점유량(CPU Time Resource)
에 가까운 개념입니다.
```

멀티 코어 / 멀티 스레드 / HT(SMT) 환경에서  
이 Quota가 실제 흐르는 시간보다 훨씬 빠르게 소모될 수 있습니다.

예:

```text
실제 시간 25ms 만에
50ms Quota 소진 가능
```

합니다.

즉:

```text
CPU 연산 처리 속도가 빠르거나,
병렬 실행량이 높을수록

Quota를 더 빠르게 소진하여
Throttling이 발생할 수 있습니다.
```

## Kubernetes CPU Limit와의 관계

예:

```yaml
resources:
  limits:
    cpu: "1000m"
```

이면,

일반적으로:

```text
Logical CPU 1개
분량의 CPU 실행 예산
```

을 의미합니다.

하지만 Container 내부에서는:

* Netty Event Loop Thread
* Kafka Consumer Thread
* GC Thread
* Application Worker Thread (내부 작업 처리용)

등 여러 Software Thread가:

```text
동일한 CPU Quota Pool

CPU Quota Pool: Container 내부 Software Thread들이 함께 공유하는 CPU 실행 예산(CPU Time Resource)
```

을 공유합니다.

즉:

```text
GC가 CPU Quota를 많이 사용하면
Event Loop가 Throttle
```

될 수도 있습니다.

## 그러면 무슨 일이 발생하나?

Linux CFS는:

```text
"이번 주기의 CPU 예산을 모두 사용했으므로
다음 주기까지 실행 금지"
```

상태로 Container를 잠시 멈춥니다.

이것이:

```text
CPU Throttling
```

입니다.

중요한 점은:

```text
CPU가 실제로 유휴(Idle) 상태여도
Container는 CPU Quota(연산 예산)이 없어서 멈출 수 있음
```

입니다.

즉:

```text
CPU 부족
≠
반드시 CPU Throttling
```

입니다.

## 장애의 연쇄 반응 (Chain Reaction)

짧은 CPU 중단도 Runtime 전체에 영향을 줄 수 있습니다.

```text
CPU Throttling
→ Event Loop 지연
→ Request Queue 증가
→ Timeout 증가
→ Retry 증가
→ Kafka Lag 증가
→ Latency Spike
```

특히:

* Event Loop 기반 Runtime (적은 Thread 기반 구조)
* Latency-sensitive 시스템

에서는 영향이 훨씬 큽니다.

## 특히 위험한 Runtime 구조

다음과 같은 시스템은 짧은 [CPU Stall](../20-deep-dive/cpu-stall.md)에도 민감합니다.

| Runtime           | 영향                 |
| ----------------- | ------------------ |
| Spring WebFlux    | Event Loop 지연      |
| Netty             | Connection 처리 지연   |
| Kafka Consumer    | Consumer Lag 증가    |
| JVM               | GC 지연 / STW 증가     |
| Redis Client      | Connection Timeout |
| Reactive Pipeline | Backpressure 증가    |

이들은:

```text
적은 수의 Thread
+
빠른 Event Loop 처리
```

를 기반으로 동작하기 때문입니다.

---

## CPU Usage는 낮은데 왜 느린가?

CPU Throttling의 가장 위험한 특징은:

```text
CPU Usage는 낮게 보일 수 있음
```

에도,

```text
Runtime Latency는 급격히 증가
```

할 수 있다는 점입니다.

즉:

```text
CPU Idle ≠ 서비스 정상
```

일 수 있습니다.

실제로는:

* Runnable Queue 증가
* Event Loop Stall
* Context Switch 증가
* Scheduler Delay
* Request Queue 증가

가 동시에 발생할 수 있습니다.

---

## Kubernetes는 무엇을 하는가?

중요한 점은:

```text
Kubernetes 자체가 CPU를 제한하는 것이 아님
```

입니다.

실제 Enforcement(강제 제한)는:

```text
Host Linux Kernel
+
cgroup
+
CFS Scheduler
```

가 수행합니다.

즉 흐름은:

```text
Kubernetes
→ kubelet
→ containerd / CRI
→ cgroup 설정 생성
→ Linux Kernel CFS Enforcement
```

입니다.

---

## 실제 Linux 제어 파일

### cgroup v1

```bash
cpu.cfs_period_us
cpu.cfs_quota_us
```

### cgroup v2

```bash
cpu.max
```

실제 경로 예시:

```bash
/sys/fs/cgroup/
/sys/fs/cgroup/kubepods.slice/
```

---

## CFS Period / Quota 조정 가능 여부

조정 가능합니다.

예:

```bash
cpu.cfs_period_us
```

기본값:

```text
100000 = 100ms
```

예:

```bash
echo 200000 > cpu.cfs_period_us
```

이면:

```text
Period = 200ms
```

가 됩니다.

즉:

```text
더 긴 CPU Burst 허용
```

효과가 발생할 수 있습니다.

---

## 하지만 왜 실무에서는 잘 안 바꾸나?

실무에서는 보통:

```text
Period 변경
```

보다,

```text
CPU limit 자체 완화
```

를 더 선호합니다.

왜냐면:

* kubelet 전체 영향
* Fairness 변화
* Noisy Neighbor 위험
* 특정 Container CPU 독점 가능성

이 생길 수 있기 때문입니다.

---

## SRE 관점 핵심

특히:

* Spring WebFlux
* Netty
* Kafka
* Reactive Runtime

환경에서는:

```text
짧은 CPU Stall
=
전체 Latency Spike
```

로 이어질 수 있습니다.

따라서 중요한 것은:

* 너무 타이트한 CPU limit 지양
* 충분한 Request 보장
* Burst 여유 확보
* P99 / P999 Latency 관측
* Event Loop Stall 모니터링
* CPU Throttling Metric 관측

입니다.

---

## 대표적인 모니터링 지표

### CPU Throttling 시간

```promql
container_cpu_cfs_throttled_seconds_total
```

### CPU Throttling 발생 횟수

```promql
container_cpu_cfs_throttled_periods_total
```

### CPU 사용률

```promql
container_cpu_usage_seconds_total
```

---

## 실무에서 자주 발생하는 오해

| 오해                | 실제                  |
| ----------------- | ------------------- |
| CPU Usage 낮음 = 정상 | Latency Spike 가능    |
| CPU Idle = 여유 있음  | Throttling 가능       |
| Container가 CPU 제한 | 실제론 Host Kernel CFS |
| limit은 안전장치       | 너무 타이트하면 장애 유발 가능   |

---

## 한 줄 요약

```text
Kubernetes CPU Throttling은
CPU 부족 자체보다,

"짧은 주기 안에서
CPU 실행 예산(Quota)을
너무 빠르게 소진"

해서 발생하는 Runtime Latency 문제에 가깝습니다.
```

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*