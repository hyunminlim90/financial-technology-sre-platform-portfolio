# 객체 레이아웃 (Object Layout)

> 정독: 0회

## 1. 이 기술이 무엇인가

객체 레이아웃은:

> **메모리 안에서 객체가 실제로 배치되는 물리 구조**

객체는 런타임에 힙 메모리에 생성되며, 내부적으로는 바이트 단위 구조를 가집니다.

**대표 구성:**

| 구성 요소 | 역할 |
|-----------|------|
| Object Header | 메타데이터 |
| Instance Data | 필드 데이터 |
| Padding | alignment 보정 |

> **핵심: 객체의 메모리 배치 규격**

---

## 2. 시스템 어디에서 등장하는가

| 영역 | 역할 |
|------|------|
| Heap Memory | 객체 저장 |
| Runtime Engine | object allocation |
| GC | object traversal |
| CPU Cache | memory locality |
| MMU | address translation |
| Compiler | field offset 계산 |
| Runtime Metadata | type/object info |

특히 객체 생성, 필드 접근, 메서드 호출, GC scanning 모두 객체 레이아웃을 기반으로 동작합니다.

---

## 3. 어떤 자원에 가장 영향이 큰가

객체 레이아웃은 메모리 구조에 직접 영향을 줍니다.

| 자원 | 영향 |
|------|------|
| RAM | object footprint |
| CPU Cache | locality |
| Memory Bus | access traffic |
| Heap | allocation density |
| CPU | pointer dereference |

특히 alignment, object size, field ordering은 cache efficiency에 큰 영향을 줍니다.

---

## 4. 왜 중요한가

객체 레이아웃은 객체 접근 성능, 메모리 효율, GC 효율, 캐시 효율을 결정합니다.

| 이유 | 설명 |
|------|------|
| 필드 위치 결정 | offset 기반 접근 |
| 메모리 사용량 결정 | object size |
| GC traversal 기준 | object boundary |
| 캐시 효율 | spatial locality |
| alignment 보장 | CPU access 최적화 |

객체 레이아웃이 비효율적이면 memory bloat, cache miss, GC overhead가 증가할 수 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Memory Bloat

작은 객체라도 large header, excessive padding 때문에 메모리 낭비가 발생할 수 있습니다.

### Cache Miss 증가

필드 배치가 비효율적이면 locality 감소, cache line utilization 감소가 발생할 수 있습니다.

### GC Overhead 증가

객체 수 증가 시 traversal cost 증가, marking/scanning 비용 증가가 발생할 수 있습니다.

### False Sharing

멀티스레드 환경에서 같은 cache line을 공유할 때 coherence contention이 발생할 수 있습니다.

### Fragmentation 증가

객체 크기가 다양하면 heap fragmentation, allocation slowdown이 발생할 수 있습니다.

### Pointer Chasing 문제

깊은 object graph는 random memory access 증가, CPU stall 증가를 유발할 수 있습니다.

---

## 6. 핵심 메커니즘

### 1) 객체 생성 요청

프로그램이 `new` / allocation을 수행합니다.

### 2) Runtime이 Object Layout 계산

런타임/컴파일러가 다음을 계산합니다.

| 요소 | 설명 |
|------|------|
| header size | 메타데이터 |
| field offsets | 필드 위치 |
| alignment | padding |
| total size | 최종 객체 크기 |

### 3) Heap Allocation 수행

힙에 연속 메모리 공간을 확보합니다.

```
[header][field1][field2][padding]
```

### 4) Base Address 반환

객체 참조는 object base address를 가리킵니다.

### 5) 필드 접근 시 Offset 계산

```
field_address = base_address + field_offset
```

### 6) Header 활용

헤더에는 일반적으로 다음 정보가 저장됩니다.

| 정보 | 역할 |
|------|------|
| type metadata | 객체 타입 |
| GC metadata | mark state |
| synchronization info | lock state |
| vtable/type pointer | method dispatch |

### 7) GC Traversal 수행

GC는 객체 레이아웃 기반으로 pointer field, primitive field, object boundary를 판별합니다.

### 8) Alignment 적용

CPU 효율을 위해 8-byte, 16-byte 정렬을 수행할 수 있습니다.

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 메모리 사용량
top
htop
smem
pmap

# Heap 분석
heap profiler
memory profiler

# Cache 분석 (cache-misses, LLC-load-misses)
perf stat
perf mem

# 바이너리 구조 분석
objdump
readelf
nm

# 메모리 접근 추적
valgrind
perf record
```

### Kubernetes

비효율 객체 레이아웃은 pod memory growth, OOMKill, GC pause, latency spike의 원인이 될 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*