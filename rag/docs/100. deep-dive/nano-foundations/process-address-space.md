# 프로세스 주소 공간 (Process Address Space)

> 정독: 0회

프로세스 주소 공간(Process Address Space)은:

> **운영체제가 하나의 프로세스에게 독립적으로 제공하는 가상의 메모리 영토 전체**

**쉽게 말하면:**
"프로세스 전용 가상 메모리 세계"

프로세스는 실제 DRAM 물리 주소를 직접 보지 않으며, 대신 **자기만의 연속된 가상 주소 공간**을 사용합니다.

---

## 1. 이 기술이 무엇인가

프로세스가 실행되면 운영체제는 **독립된 Virtual Address Space**를 생성합니다.

이 공간 안에는 코드, 전역 데이터, 힙, 스택 등이 배치됩니다.

즉, **프로세스 실행에 필요한 모든 메모리 구조가 존재하는 논리적 영토**입니다.

### 핵심 구조

```
Process Address Space
├── Text (Code)
├── Data
├── Heap
└── Stack
```

---

## 2. 시스템 어디에서 등장하는가

프로세스 주소 공간은 모든 사용자 프로세스 실행 시 등장합니다.

대표 영역:

- 실행 파일 로딩
- 프로세스 생성
- 스레드 실행
- malloc / new
- mmap
- 공유 메모리
- Stack Frame
- Dynamic Linking
- Fork / Exec

즉, **프로세스 실행 자체의 기본 토대**입니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

| 자원 | 영향도 |
|------|--------|
| Memory | 절대적 |
| CPU | 매우 큼 |
| Disk | 큼 |
| Cache | 큼 |
| Network | 간접 영향 |

특히 **Virtual Memory, Page Table, TLB, Page Fault**와 깊게 연결됩니다.

---

## 4. 왜 중요한가

프로세스 주소 공간이 없다면 모든 프로그램이 실제 DRAM을 직접 공유하게 됩니다.

그러면 메모리 충돌, 데이터 오염, 보안 붕괴, 시스템 다운이 발생할 수 있습니다.

운영체제는 프로세스마다 독립된 가상 메모리 세계를 제공하여 이를 차단합니다.

즉, **프로세스 격리(Isolation)의 핵심**입니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### 5-1. Stack Overflow

스택이 과도하게 성장하면 Stack 영역을 초과합니다.

**결과:** Segmentation Fault 가능

### 5-2. Heap Memory Leak

힙 메모리 반환에 실패하면 Heap이 지속적으로 증가합니다.

**결과:** OOM 가능

### 5-3. Segmentation Fault

프로세스가 허용되지 않은 주소 접근을 시도하면 MMU가 차단합니다.

### 5-4. Page Fault 폭증

주소 공간 내 페이지가 DRAM에 없으면 **Page Fault**가 발생합니다.

심하면 Swap I/O가 증가합니다.

### 5-5. Address Space Fragmentation

메모리 매핑이 복잡해지면 **Virtual Address Fragmentation**이 발생할 수 있습니다.

### 5-6. Context Switch 비용

프로세스 변경 시 Page Table이 변경되어 **TLB Flush**가 발생할 수 있습니다.

---

## 6. 핵심 메커니즘

### 6-1. 프로세스마다 독립 주소 공간을 가진다

각 프로세스는 자기만의 가상 메모리 세계를 보유합니다.

즉, **다른 프로세스의 주소에 직접 접근할 수 없습니다.**

### 6-2. 실제 DRAM과는 다르다

프로세스가 보는 주소는 **가상 주소**입니다. 실제 DRAM 주소는 **MMU가 변환**합니다.

### 6-3. Page Table이 연결 지도다

운영체제는 `Virtual Page → Physical Frame` 매핑을 관리합니다.

즉, **Page Table 기반 주소 변환**입니다.

### 6-4. 코드 영역은 읽기 전용이다

Text Segment는 실행 코드 저장 영역으로, 보통 **Read + Execute** 권한만 존재합니다.

### 6-5. 힙은 동적 메모리 영역이다

Heap은 런타임 동적 할당 메모리이며, 보통 **낮은 주소 → 높은 주소** 방향으로 성장합니다.

### 6-6. 스택은 함수 호출 영역이다

Stack에는 지역 변수, 함수 인자, Return Address가 저장되며, 보통 **높은 주소 → 낮은 주소** 방향으로 성장합니다.

### 6-7. Heap과 Stack은 서로 마주 본다

```
Heap  ↑

 (빈 공간)

      ↓  Stack
```

메모리 부족 시 서로 충돌할 수 있습니다.

### 6-8. MMU가 보호를 수행한다

MMU는 **R/W/X 권한**을 검사하여 프로세스 메모리 침범을 차단합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

```bash
# 프로세스 주소 공간 확인
cat /proc/<pid>/maps

# 메모리 세그먼트 상세
pmap <pid>

# 메모리 통계
cat /proc/<pid>/smaps

# Stack 크기 제한
ulimit -s

# Page Fault 관측
vmstat 1

# 프로세스 메모리 사용량
top
htop
ps
```

### Runtime

핵심 관측 포인트:

- RSS
- VSZ
- Heap Usage
- Stack Usage
- Page Fault
- Memory Leak
- mmap

### Kubernetes

```bash
# OOMKilled 확인
kubectl describe pod
```

```yaml
# Pod 메모리 제한
resources:
  limits:
    memory: "2Gi"
```

> **cgroup 메모리 격리** — 컨테이너도 결국 독립 Process Address Space 기반입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*