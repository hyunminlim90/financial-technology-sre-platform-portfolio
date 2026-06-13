# 프로세스 실행 (Process Execution)

> 정독: 0회

## 1. 이 기술이 무엇인가

프로세스 실행은:

> 디스크에 저장된 실행 파일이 메모리에 적재된 뒤, 운영체제의 관리 아래 CPU 코어에서 실제 명령어가 수행되는 **전체 실행 과정**을 의미

**핵심 흐름:**

```
실행 파일 적재 → 프로세스 생성 → 메모리 공간 구성
→ 스레드 생성 → CPU 스케줄링 → 명령어 실행 → 종료 및 자원 회수
```

즉, **"정적 파일"이 "동적으로 동작하는 실행 주체"가 되는 과정** 전체입니다.

---

## 2. 시스템 어디에서 등장하는가

프로세스 실행은 거의 모든 시스템 동작의 중심에 존재합니다.

**등장 위치:** 운영체제 커널, CPU 스케줄러, 메모리 관리자, 런타임 환경, 컨테이너 런타임, Kubernetes Pod 내부 프로세스, 웹 서버/DB/메시지 브로커, Terraform/OpenTofu 실행 프로세스, CI/CD 에이전트

**예시:** nginx 실행, postgres 실행, terraform apply 실행, docker container 내부 애플리케이션 실행, kubelet/containerd 실행 — 전부 프로세스 실행입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향 |
|------|------|
| CPU | 명령어 실행 |
| Memory | 코드/힙/스택 적재 |
| Disk | 실행 파일 로딩, swap |
| Network | 실행 중 네트워크 I/O 발생 |

특히 프로세스 실행의 핵심 병목은 **CPU 시간(CPU Time)**과 **메모리 사용량(RSS, Virtual Memory)** 두 가지입니다.

---

## 4. 왜 중요한가

프로세스 실행은 운영체제 전체의 성능과 안정성의 중심입니다.

- 모든 애플리케이션은 결국 프로세스로 실행됨
- CPU 자원 경쟁의 핵심 단위
- 메모리 고갈의 직접 원인
- 컨테이너/Kubernetes의 실제 실행 실체
- 장애 발생 시 가장 먼저 관찰되는 단위

실무의 다음 질문들은 모두 프로세스 실행과 연결됩니다: 왜 CPU 사용률이 100%인가? 왜 OOMKilled 되었는가? 왜 응답 지연이 발생하는가? 왜 Context Switch가 급증하는가? 왜 Load Average가 상승하는가?

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation

프로세스 수요가 코어 수를 초과하면 Run Queue 증가, 스케줄링 지연, 응답시간 증가, Context Switch 증가가 발생합니다.

```bash
top
htop
uptime
vmstat 1
```

### OOM (Out Of Memory)

프로세스가 메모리를 과도하게 사용하면 OOM Killer가 발동하여 프로세스를 강제 종료합니다. Kubernetes에서는 OOMKilled로 나타납니다.

```bash
dmesg
kubectl describe pod
free -m
```

### Fork Bomb / Process Explosion

프로세스가 비정상적으로 폭증하면 PID 고갈, 스케줄러 과부하, 시스템 응답 불가가 발생합니다.

```bash
ps -ef | wc -l
top
pidstat
```

### Excessive Context Switching

스레드가 지나치게 많으면 CPU가 실제 연산보다 문맥 저장/복원에 더 많은 시간을 사용하여 Throughput이 급감합니다.

```bash
vmstat 1
pidstat -w
sar -w
```

---

## 6. 핵심 메커니즘

프로세스 실행의 핵심은 아래 7단계입니다.

### 1) 실행 파일 로딩

운영체제가 실행 파일을 디스크에서 읽습니다. (ELF, PE, Mach-O 등)

### 2) 가상 메모리 공간 생성

프로세스 전용 주소 공간을 생성합니다: Code Segment, Data Segment, Heap, Stack

### 3) PCB 생성

커널이 Process Control Block을 생성합니다.

| 포함 정보 | 설명 |
|-----------|------|
| PID | 프로세스 식별자 |
| 상태 | 실행/대기/종료 등 |
| 레지스터 정보 | CPU 상태 |
| 메모리 맵 | 주소 공간 구성 |
| 파일 디스크립터 | 열린 파일 목록 |

### 4) 스레드 생성

최소 하나 이상의 실행 흐름을 생성합니다. 스레드는 PC(Register), Stack, Register Context를 독립적으로 보유합니다.

### 5) Ready Queue 진입

CPU 실행 대기 상태로 진입합니다.

### 6) 스케줄링 및 Dispatch

스케줄러가 코어를 할당하고, 실행 문맥을 CPU 레지스터에 복원합니다.

### 7) 명령어 사이클 실행

코어 내부에서 반복합니다:

```
Fetch → Decode → Execute → Write Back
```

이 과정이 실제 "프로세스 실행"입니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 프로세스 확인
ps -ef
top
htop
pstree

# 프로세스 상세
cat /proc/<PID>/status
cat /proc/<PID>/maps
cat /proc/<PID>/smaps

# 스케줄링 상태
vmstat 1
pidstat
sar

# 문맥 전환
pidstat -w

# 메모리
free -m
smem
pmap
```

### Runtime / Container

```bash
# 컨테이너 내부 실제 PID
docker top <container>
ctr task ls
crictl ps
```

### Kubernetes

```bash
# Pod 내부 프로세스
kubectl exec -it <pod> -- ps -ef

# 리소스 사용량
kubectl top pod
kubectl describe pod

# OOM 확인
kubectl describe pod
dmesg
```