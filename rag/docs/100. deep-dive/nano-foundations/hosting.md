# 호스팅(Hosting)

> 정독: 0회

## 1. 이 기술이 무엇인가

호스팅은:

> 특정 소프트웨어나 서비스를 실행할 수 있도록 **컴퓨팅 자원과 실행 환경을 제공·관리하는 기술**

| 요소 | 의미 |
|------|------|
| 실행 환경 제공 | 프로그램이 동작할 수 있는 환경 생성 |
| 자원 관리 | CPU, 메모리, 네트워크, 스토리지 배분 |

호스팅은 단순 서버 대여가 아니라 프로세스 실행, 메모리 배치, 네트워크 연결, 파일 시스템 접근, 런타임 운영 전체를 포함하는 **실행 인프라 개념**입니다.

---

## 2. 시스템 어디에서 등장하는가

호스팅은 거의 모든 컴퓨팅 계층에 존재합니다.

```
Hardware
→ Operating System
→ Runtime Environment
→ Application Server
→ Application
→ User Request
```

| 계층 | 무엇을 호스팅하는가 |
|------|-------------------|
| 하드웨어 | 운영체제 |
| 운영체제 | 프로세스 |
| 컨테이너 런타임 | 컨테이너 |
| JVM | 바이트코드 |
| 웹 서버 | HTTP 애플리케이션 |
| Kubernetes | Pod/Container |

> 호스팅은 **"상위 실행 주체를 위한 실행 공간 제공"** 입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

### CPU

프로세스 스케줄링과 실행 제어에 직접 영향이 있습니다. multi-tenant scheduling, runtime execution, container orchestration 등이 해당됩니다.

### Memory

프로세스 메모리 격리, heap allocation, cache, page management를 관리하며 매우 중요합니다.

### Network

cloud hosting, distributed system, service mesh, ingress routing 등 현대 호스팅에서 핵심 자원입니다.

### Disk

volume mount, filesystem, container layer, snapshot 등 지속성 저장과 직접 연결됩니다.

---

## 4. 왜 중요한가

**호스팅 구조가 시스템 안정성과 운영 품질을 결정합니다.**

| 영역 | 영향 |
|------|------|
| 가용성 | 서비스 지속성 |
| 확장성 | scale out/in |
| 성능 | resource isolation |
| 보안 | process/container isolation |
| 운영성 | deployment simplicity |
| 장애 복구 | failover/restart |

소프트웨어는 반드시 어떤 호스팅 환경 위에서 실행됩니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

| 장애 | 원인 |
|------|------|
| OOMKilled | 메모리 부족 |
| CPU Throttling | CPU quota 제한 |
| Pod Eviction | node resource pressure |
| File Descriptor Exhaustion | host resource exhaustion |
| Port Collision | network binding conflict |
| Disk Full | storage exhaustion |
| Container CrashLoop | runtime startup failure |

호스팅 문제는 대부분 **자원 관리 실패**와 연결됩니다.

---

## 6. 핵심 메커니즘

### (1) 실행 환경 제공

프로그램은 단독으로 실행되지 않습니다. 반드시 process space, runtime, filesystem, network stack이 필요합니다.

### (2) 자원 격리

현대 호스팅의 핵심 메커니즘입니다.

| 자원 | 격리 방식 |
|------|----------|
| CPU | scheduler/quota |
| Memory | virtual memory/cgroup |
| Network | namespace/vswitch |
| Filesystem | mount/chroot/container layer |

격리가 없으면 시스템 간 충돌이 발생합니다.

### (3) 계층적 호스팅

호스팅은 중첩 구조입니다.

```
Hardware
→ OS
→ Container Runtime
→ Container
→ JVM
→ Application
```

각 계층은 상위 계층을 호스팅합니다.

### (4) Runtime Hosting

| Runtime | 역할 |
|---------|------|
| JVM | bytecode hosting |
| Node.js Runtime | JS execution |
| Python Runtime | interpreter hosting |
| containerd | container hosting |

런타임은 단순 실행기가 아니라 메모리 관리자, 스케줄러, 로더, I/O 관리자 역할까지 수행합니다.

### (5) 프로세스 기반 호스팅

운영체제는 각 프로그램을 프로세스로 호스팅합니다.

포함 요소: virtual address space, file descriptor table, signal handling, scheduling context

### (6) 컨테이너 기반 호스팅

컨테이너는 namespace, cgroup, overlay filesystem 위에서 실행됩니다.

**호스트 OS가 컨테이너를 격리 호스팅합니다.**

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
ps aux
top
systemctl status
cat /proc/<pid>/status
```

관측 대상: process, memory, fd, scheduling, limits

### Runtime

| 도구 | 의미 |
|------|------|
| `jps` | JVM process |
| `jstack` | thread 상태 |
| `jmap` | heap 상태 |
| runtime metrics | execution 상태 |

### Kubernetes

K8s 자체가 거대한 호스팅 플랫폼입니다.

```bash
kubectl get pods
kubectl describe pod
kubectl top pod
```

| 리소스 | 의미 |
|--------|------|
| Pod | 실행 단위 |
| Node | 호스트 머신 |
| Container Runtime | 실제 hosting layer |
| cgroup | resource isolation |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*