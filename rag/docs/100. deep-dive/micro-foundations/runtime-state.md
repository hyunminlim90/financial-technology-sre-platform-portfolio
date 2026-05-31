# 런타임 상태 (Runtime State / 동적 상태)

> 정독: 0회

## 1. 이 기술이 무엇인가

**런타임 상태(Runtime State)** 는:

> 프로그램·프로세스·파일·소켓·스레드 등이 실제로 실행 중인 순간에 커널이나 메모리 내부에서 동적으로 유지되는 **실시간 상태 데이터**

### 핵심 특징

런타임 상태는 실행 중에 생성되고, 지속적으로 변경되며, 종료 시 제거 가능한 **메모리 기반 동적 정보**입니다.

### 정적 상태 vs 동적 상태

| 구분 | 성격 | 예시 |
|------|------|------|
| **정적 상태 (Static State)** | 설계 정보 | inode metadata, executable binary, configuration file, file permission |
| **동적 상태 (Runtime State)** | 현재 실행 정보 | current file offset, process state, TCP connection state, memory mapping, scheduler state |

> **핵심:** 정적 데이터는 "설계 정보" / 런타임 상태는 "현재 실행 정보"

---

## 2. 시스템 어디에서 등장하는가

운영체제 **전체**에 존재합니다.

| 영역 | 구조체 | 주요 상태 |
|------|--------|-----------|
| **프로세스** | `task_struct` | PID, scheduling state, CPU affinity, signal state |
| **파일 시스템** | `struct file` | file offset, open flags, async mode |
| **네트워크** | TCP runtime state | ESTABLISHED, SYN_SENT, CLOSE_WAIT |
| **메모리** | 가상 메모리 상태 | page table, page cache, mmap state |
| **Kubernetes** | container runtime state | pod lifecycle, cgroup usage, namespace mapping |
| **Device** | device queue state | pending I/O, DMA status, interrupt state |

---

## 3. 어떤 자원에 가장 영향이 큰가

가장 큰 영향: **Memory + CPU synchronization**

| 자원 | 영향 | 설명 |
|------|------|------|
| **Memory** | 매우 큼 | 런타임 상태 대부분은 RAM에 존재 (kernel object, process table, socket buffer, page cache) |
| **CPU** | 큼 | 상태 변경 시 locking, scheduling, interrupt handling 발생 |
| **Network** | 있음 | TCP state machine 유지 필요 |
| **Disk** | 있음 | I/O queue state 유지 |

> **핵심:** 런타임 상태는 메모리 위에서 유지되는 **실시간 시스템 제어 데이터**입니다.

---

## 4. 왜 중요한가

운영체제와 분산 시스템의 **핵심**입니다.

**이유 1. 동시성 관리**
멀티프로세스 환경 유지 가능.

**이유 2. 상태 기반 실행**
프로그램은 현재 어디까지 수행됐는가를 상태로 유지해야 합니다.

**이유 3. 자원 추적**
누가 파일 사용 중인지, 메모리 점유 중인지, socket 연결 중인지 추적 가능.

**이유 4. 장애 복구**
runtime state 기반으로 retry, recovery, timeout, rollback 수행 가능.

**이유 5. 스케줄링 핵심**
CPU scheduler는 runtime state 기반으로 동작합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 주요 장애 유형

| 장애 | 설명 |
|------|------|
| **State Corruption** | invalid state transition, inconsistent metadata, race condition |
| **CLOSE_WAIT 누적** | socket runtime state 정리 실패 |
| **zombie process** | process runtime state cleanup 실패 |
| **file offset corruption** | 동시 접근 시 offset 충돌 가능 |
| **deadlock** | runtime synchronization state 충돌 |
| **memory leak** | runtime object 해제 실패 |
| **stale runtime state** | distributed system에서 자주 발생 (stale leader, stale session, stale lock) |

### Kubernetes

Pod state mismatch: `Running`인데 실제로는 죽어 있는 상태 불일치 발생 가능.

> **핵심:** 실제 운영 장애 상당수는 **런타임 상태 관리 실패**입니다.

---

## 6. 핵심 메커니즘

### 1. 생성 (Create)
실행 시 생성. (`open()`, `fork()`, `socket()`, `accept()`)

### 2. 상태 전이 (State Transition)
실행 중 지속 변화.
```
READY → RUNNING → BLOCKED
```

### 3. 동기화 (Synchronization)
동시 접근 보호 필요. (spinlock, mutex, atomic op)

### 4. reference counting
runtime object 생명주기 관리.

### 5. cleanup
종료 시 memory free, resource release 필요.

### 6. kernel object 기반 관리
대부분 kernel structure로 존재.

**예:** `struct file`, `task_struct`, `socket`, `inode`

### 7. volatile 특성
대부분 reboot 시 사라집니다.

### 핵심 구조 흐름

```
Static Metadata
    → Runtime Activation
    → Dynamic State Object
    → Real-time Updates
    → Cleanup
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| 프로세스 상태 | `ps`, `top`, `htop` |
| 프로세스 runtime state | `/proc/<pid>` |
| 열린 파일 상태 | `lsof` |
| socket 상태 | `ss -ant`, `netstat` |
| memory 상태 | `free -h`, `vmstat`, `slabtop` |
| interrupt 상태 | `cat /proc/interrupts` |
| block I/O 상태 | `iostat`, `iotop` |

### Kubernetes

| 목적 | 명령어 |
|------|--------|
| Pod 상태 | `kubectl get pod` |
| Container runtime 상태 | `crictl ps`, `crictl inspect` |
| cgroup 상태 | `/sys/fs/cgroup` |
| systemd 상태 | `systemctl status` |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*