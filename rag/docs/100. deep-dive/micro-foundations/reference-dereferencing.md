# 참조 역참조 (Reference Dereferencing)

> 정독: 0회

## 1. 이 기술이 무엇인가

**참조 역참조(Dereferencing)** 는:

> 메모리 주소(참조/포인터)를 따라가 실제 메모리 데이터에 접근하는 행위

**핵심:**

주소 자체가 아니라, 그 주소가 가리키는 실제 데이터에 접근하는 것

**예시:**

```
ptr -> 0x1000
```

여기서 `*ptr` 는 `0x1000` 위치의 메모리를 읽는다는 의미입니다.

---

## 2. 시스템 어디에서 등장하는가

참조 역참조는 거의 모든 런타임 시스템의 핵심 동작입니다.

| 영역 | 사용 사례 |
|------|-----------|
| Object Access | field access |
| Method Call | object method invocation |
| Array Access | index dereference |
| Pointer Arithmetic | memory traversal |
| GC | object graph traversal |
| CPU | memory addressing |
| Kernel | page table traversal |
| Runtime | reference resolution |

> 객체 접근(Object Access)의 본질이 역참조입니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향은 **CPU + Memory** 입니다.

| 자원 | 영향 |
|------|------|
| CPU Cache | cache miss |
| TLB | address translation |
| RAM Latency | memory access delay |
| Memory Bus | data transfer |
| Branch Prediction | pointer chasing |

---

## 4. 왜 중요한가

프로그램의 거의 모든 데이터 접근은 역참조 기반입니다.

```java
object.field
array[i]
ptr->value
```

모두 내부적으로 **주소 계산 + 메모리 접근** 을 수행합니다.

> 역참조 없이는 객체 데이터 접근 자체 불가능

field read, field write, method dispatch, dynamic binding, GC traversal 모두 역참조에 의존합니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Null Dereference
대표 장애로, null pointer dereference로 인한 잘못된 주소 접근이 발생합니다.

### Segmentation Fault
유효하지 않은 주소 접근 시 `SIGSEGV`, access violation이 발생할 수 있습니다.

### Use-After-Free
이미 해제된 객체 주소를 역참조하면 memory corruption, crash가 발생할 수 있습니다.

### Cache Miss 폭증
랜덤 pointer traversal이 많으면 cache locality 감소, memory stall 증가가 발생할 수 있습니다.

### GC Pause 증가
reference graph traversal이 많으면 object scan 증가, tracing 비용 증가가 발생할 수 있습니다.

### Pointer Chasing 병목
linked structure traversal이 심하면 CPU가 메모리 latency를 대기하는 상황이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

### 핵심 흐름

```
1) 참조 변수 확보
   └─ 프로그램 변수 내부에는 객체 주소(reference) 저장
      예: obj = 0x1000

2) 역참조 시작
   └─ field access 수행 시 (obj.field)
      runtime은 obj 내부 주소를 읽음

3) Base Address 확보
   └─ 역참조 결과: 0x1000 획득

4) Field Offset 계산
   └─ field offset = +16 bytes 라면
      effective address = 0x1000 + 16 계산

5) MMU 주소 변환
   └─ 가상 주소 → physical address 변환

6) Cache/TLB 조회
   └─ CPU가 TLB lookup → cache lookup 수행

7) 실제 Memory Access
   └─ 최종적으로 load / store 수행
```

> **역참조는 실제 메모리 접근의 시작 단계**입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Segmentation Fault 확인

```bash
dmesg
journalctl
```

### Core Dump 분석

```bash
gdb core
```

### Memory Access 분석

```bash
perf mem
perf stat
```

### Page Fault 확인

```bash
vmstat
sar -B
```

### Pointer/Address Debugging

```bash
gdb
lldb
```

### CPU Cache Miss 확인

```bash
perf top
perf record
```

### Kubernetes 환경

container 내부 process에서도 virtual memory, pointer dereference, page fault, cache miss가 동일하게 발생합니다.

특히 아래 상황 분석 시 중요합니다.

- OOM
- native crash
- segmentation fault

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*