# 정상 실행 (Normal Execution)

> 정독: 0회

## 1. 이 기술이 무엇인가

정상 실행은:

> 프로그램이 운영체제와 하드웨어의 제약 조건을 위반하지 않고, 정의된 제어 흐름과 데이터 처리 로직을 **안정적으로 수행하는 실행 상태**

**핵심 조건:**

- 허용된 메모리만 접근
- 유효한 명령어만 실행
- 자원 한계 내에서 동작
- 스레드와 프로세스 상태가 일관성 유지
- 예외 및 치명적 오류 없이 종료 또는 지속 실행

즉, 프로세스와 스레드가 **시스템 규칙을 깨지 않고 지속적으로 명령어 사이클을 수행하는 상태**입니다.

---

## 2. 시스템 어디에서 등장하는가

정상 실행은 시스템 전체의 **기본 목표 상태**입니다.

**등장 위치:** 운영체제 커널, 사용자 프로세스, 런타임 환경, 웹 서버, 데이터베이스, Kubernetes Pod, 컨테이너 런타임, 네트워크 서버, Terraform/OpenTofu 실행 환경, CI/CD 에이전트

**실제 예시:** nginx worker process 정상 응답, postgres query 처리, terraform apply 완료, kubelet 정상 heartbeat, API 서버 요청 처리 — 전부 정상 실행 상태의 사례입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

정상 실행은 시스템 전체 자원의 **균형 상태**와 직접 연결됩니다.

| 자원 | 영향 |
|------|------|
| CPU | 명령어 실행 안정성 |
| Memory | 주소 접근 무결성 |
| Disk | 파일/페이지 I/O |
| Network | 소켓 및 연결 상태 |
| Scheduler | 실행 흐름 공정성 |

특히 중요한 것은 메모리 보호, CPU 스케줄링 안정성, 동기화 무결성, 자원 누수 방지입니다.

---

## 4. 왜 중요한가

정상 실행은 **시스템 신뢰성의 핵심**입니다. 정상 실행이 깨지면 프로세스 크래시, 데이터 손상, 서비스 장애, 응답 지연, OOM, Deadlock, Kernel Panic 등이 발생합니다.

SRE/플랫폼 관점의 핵심 목표:

- 정상 실행 유지 시간 극대화
- 비정상 종료 최소화
- 오류 격리
- 자동 복구 가능성 확보

> **정상 실행은 "서비스 가용성"의 가장 근본적인 상태입니다.**

---

## 5. 실제 장애와 어떤 관련이 있는가

### Segmentation Fault

허용되지 않은 메모리 접근 시 발생합니다. 원인으로는 잘못된 포인터, 해제된 메모리 접근, 스택 오버플로우가 있습니다.

```bash
dmesg
journalctl
coredumpctl
```

### OOMKilled

프로세스가 메모리 한계를 초과하면 커널 OOM Killer가 개입하여 프로세스를 강제 종료합니다.

```bash
dmesg
kubectl describe pod
```

### Deadlock

스레드 간 락 대기 순환으로 실행 흐름이 정지되고 CPU idle 상태 증가, 처리량이 급감합니다.

```bash
jstack
pstack
gdb
```

### CPU Starvation

특정 스레드가 CPU를 지속 점유하면 다른 스레드 실행이 불가능해지고 응답시간이 증가합니다.

```bash
top
pidstat
vmstat
```

### File Descriptor Exhaustion

파일/소켓 자원이 고갈되면 `Too many open files` 오류가 발생합니다.

```bash
lsof
ulimit -n
cat /proc/sys/fs/file-max
```

---

## 6. 핵심 메커니즘

정상 실행은 아래 메커니즘이 동시에 안정적으로 유지될 때 성립합니다.

### 1) 메모리 보호

프로세스는 자신의 주소 공간만 접근할 수 있으며, MMU + Page Table 기반으로 보호됩니다. 위반 시 Segmentation Fault, Access Violation이 발생합니다.

### 2) 스케줄링 안정성

스레드들이 공정하게 CPU를 획득해야 합니다. 필수 조건으로 Context Switch 정상 수행, Run Queue 과포화 방지, CPU starvation 방지가 있습니다.

### 3) 명령어 사이클 정상 진행

CPU 내부에서 Fetch → Decode → Execute → Write Back이 중단 없이 지속 수행됩니다.

### 4) 자원 무결성 유지

메모리, FD(File Descriptor), 소켓, 락, CPU Time이 정상 범위를 유지해야 합니다.

### 5) 상태 전이 일관성

프로세스/스레드 상태가 정상적으로 전이되어야 합니다.

```
Ready → Running → Waiting → Running → Terminated
```

**비정상 상태:** Zombie, Deadlock, Hung Process

### 6) 예외 처리 안정성

오류 발생 시 프로세스 전체 붕괴 방지, 상태 복구, 자원 회수가 가능해야 합니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 프로세스 상태
ps -ef
top
htop

# 프로세스 상태 코드 (ps aux)
# R → Running
# S → Sleeping
# D → Uninterruptible Sleep
# Z → Zombie

# CPU/스케줄링
vmstat 1
pidstat
sar

# Run Queue
uptime

# 메모리 상태
free -m
cat /proc/meminfo
smem

# 시스템 로그
dmesg
journalctl
```

### Kubernetes

```bash
kubectl get pods
kubectl describe pod
```

**정상 상태:** `Running`, `Ready=True`

**비정상 상태:** `CrashLoopBackOff`, `OOMKilled`, `Error`

### Container Runtime

```bash
docker top
crictl ps
ctr task ls
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*