# CPU L1 / L2 Cache와 메모리 계층 구조

## 1. CPU Cache란?

CPU Cache는 CPU와 메인 메모리(RAM) 사이에 위치하는 고속 메모리 계층이다.

CPU의 연산 속도는 RAM 접근 속도보다 훨씬 빠르기 때문에, RAM에만 의존할 경우 CPU가 데이터를 기다리는 대기 시간이 병목이 된다. 이를 해결하기 위해 CPU 내부에 자주 사용하는 데이터와 명령어를 임시 저장하는 Cache 계층이 존재한다.

### Cache의 목적

| 목적 | 설명 |
|------|------|
| 메모리 접근 속도 향상 | RAM 접근 빈도 최소화 |
| CPU Stall 감소 | 메모리 대기 시간 감소 |
| Pipeline 유지 | 실행 흐름 중단 방지 |
| IPC 향상 | 사이클당 처리량 증가 |

---

## 2. CPU 메모리 계층 구조

현대 CPU는 계층형 메모리 구조를 사용한다. 상위 계층일수록 속도가 빠르고 용량이 작으며, 하위 계층일수록 접근 지연이 크고 용량이 크다.

```
CPU Register        ← 가장 빠름 / 가장 작음
  ↓
L1 Cache
  ↓
L2 Cache
  ↓
L3 Cache
  ↓
Main Memory (RAM)
  ↓
Storage (SSD / Disk) ← 가장 느림 / 가장 큼
```

| 계층 | 속도 | 용량 | 접근 지연 |
|------|------|------|-----------|
| Register | 가장 빠름 | 가장 작음 | 거의 없음 |
| L1 Cache | 매우 빠름 | 수십 KB | ~1 ns |
| L2 Cache | 빠름 | 수백 KB ~ 수 MB | ~5 ns |
| L3 Cache | 보통 | 수 MB ~ 수십 MB | ~20 ns |
| RAM | 느림 | 수 GB | ~60–100 ns |
| Storage | 매우 느림 | 수백 GB ~ TB | 수십 μs 이상 |

---

## 3. L1 Cache

L1 Cache(Level 1 Cache)는 CPU Core 내부에 위치하는 가장 빠른 Cache 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Core 내부 |
| 속도 | 가장 빠름 |
| 용량 | 32KB ~ 128KB 수준 |
| 공유 여부 | Core 전용 (비공유) |

### L1 Cache 분리 구조

L1 Cache는 용도에 따라 두 영역으로 분리된다.

| 종류 | 역할 |
|------|------|
| L1 I-Cache (Instruction Cache) | 명령어 저장 |
| L1 D-Cache (Data Cache) | 데이터 저장 |

---

## 4. L2 Cache

L2 Cache(Level 2 Cache)는 L1 Cache Miss가 발생했을 때 데이터를 보완하는 중간 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Core 내부 또는 인접 영역 |
| 속도 | L1보다 느림 |
| 용량 | 256KB ~ 수 MB 수준 |
| 공유 여부 | 일반적으로 Core 전용 |

---

## 5. L3 Cache

L3 Cache(Level 3 Cache)는 여러 Core가 공유하는 대용량 Cache 계층이다.

| 항목 | 설명 |
|------|------|
| 위치 | CPU Package 내부 |
| 속도 | L2보다 느림 |
| 용량 | 수 MB ~ 수십 MB 수준 |
| 공유 여부 | 여러 Core 공유 |
| 역할 | Core 간 데이터 공유, RAM 접근 감소 |

---

## 6. Cache 접근 흐름

CPU가 데이터를 읽을 때 다음 순서로 탐색한다.

```
L1 Cache 탐색
  → Hit: 즉시 반환
  → Miss:
      L2 Cache 탐색
        → Hit: 반환 후 L1에 적재
        → Miss:
            L3 Cache 탐색
              → Hit: 반환 후 L1/L2에 적재
              → Miss:
                  RAM 접근 → 반환 후 Cache에 적재
```

---

## 7. Cache Hit와 Cache Miss

### Cache Hit

요청한 데이터가 Cache에 존재하는 경우다. 즉시 데이터를 반환하며 Pipeline이 중단되지 않는다.

```
CPU 데이터 요청 → L1 Hit → 즉시 반환
```

### Cache Miss

요청한 데이터가 Cache에 존재하지 않는 경우다. 하위 계층을 순차적으로 탐색하며, 최종적으로 RAM에 접근하면 높은 지연이 발생한다.

```
L1 Miss → L2 Miss → L3 Miss → RAM 접근 → 높은 지연 발생
```

---

## 8. Cache Miss와 CPU Stall

Cache Miss가 발생하면 CPU는 데이터를 받을 때까지 대기해야 한다.

```
Cache Miss
  → Memory Wait 발생
  → Pipeline Stall (실행 중단)
  → CPU 실행 효율 저하
```

Cache Miss 빈도가 높을수록 IPC(Instructions Per Cycle)가 감소한다.

---

## 9. Cache Line

CPU는 데이터를 개별 바이트 단위가 아닌 **Cache Line** 단위로 Cache에 적재한다.

- **일반적인 크기**: 64 Bytes
- CPU가 특정 데이터에 접근하면, 해당 데이터를 포함한 64 Bytes 블록 전체를 Cache에 올린다.
- 이는 Spatial Locality를 활용하여 다음 접근 시 Cache Hit 가능성을 높인다.

---

## 10. Locality (지역성)

### Spatial Locality (공간적 지역성)

인접한 메모리 주소의 데이터가 연속적으로 사용될 가능성이 높다는 특성이다.

```java
int[] arr = new int[1000];
// 배열 요소는 메모리에 연속 배치됨
// → Cache Line 단위 적재 시 인접 요소도 함께 올라옴
// → 순차 접근 시 Cache Hit율 높음
```

### Temporal Locality (시간적 지역성)

최근에 사용된 데이터가 다시 사용될 가능성이 높다는 특성이다.

```java
for (int i = 0; i < 1000; i++) {
    sum += value;
    // value는 반복적으로 접근됨
    // → Cache에 계속 유지될 가능성 높음
}
```

---

## 11. Cache-Friendly vs Cache-Unfriendly 구조

### Cache-Friendly 구조

연속된 메모리 배치를 사용하는 구조는 Spatial Locality를 활용할 수 있다.

```java
int[] array    // 연속 메모리 배치 → Cache 효율 높음
long[] array
ByteBuffer
```

### Cache-Unfriendly 구조 (Pointer Chasing 문제)

포인터 기반 구조는 각 노드가 메모리의 임의 위치에 분산되어 Spatial Locality를 활용하기 어렵다.

```java
LinkedList<Node>  // 각 Node가 분산된 메모리 주소에 위치
// Node → Node → Node (각 접근마다 Cache Miss 가능성)
```

---

## 12. Cache Pollution

불필요한 데이터가 Cache를 점유하여 실제 필요한 데이터가 교체(Evict)되는 현상이다.

```
불필요한 데이터 Cache 적재
  → 유용한 데이터 Evict
  → 이후 해당 데이터 접근 시 Cache Miss 발생
  → Cache 효율 저하
```

잘못된 분기 예측(Speculative Execution)으로 인해 실행되지 않을 코드 경로의 데이터가 Cache에 올라오는 경우가 대표적인 원인 중 하나다.

---

## 13. Cache Coherency

멀티코어 환경에서 여러 Core가 동일 데이터를 각자의 L1/L2 Cache에 저장할 경우, 데이터 일관성 문제가 발생할 수 있다. CPU는 Cache Coherency 프로토콜을 통해 이를 관리한다.

| 프로토콜 | 설명 |
|----------|------|
| MESI | Modified / Exclusive / Shared / Invalid 상태 관리 |
| MOESI | MESI에 Owned 상태를 추가한 확장 프로토콜 |

---

## 14. False Sharing

서로 다른 Thread가 **논리적으로 독립된 데이터**를 수정하더라도, 두 데이터가 **같은 Cache Line**에 위치하면 Cache Invalidation이 반복적으로 발생한다.

```
Thread A → counter1 수정 (Cache Line X)
Thread B → counter2 수정 (Cache Line X 공유)

→ Thread A가 수정할 때마다 Thread B의 Cache Line도 무효화
→ 반복적인 Cache Invalidation 발생
→ 성능 저하
```

### False Sharing 방지 방법

- 두 데이터를 서로 다른 Cache Line에 위치하도록 **Padding** 추가
- `@Contended` 어노테이션 활용 (Java)

---

## 15. CPU 내부 구성 요소와 Cache의 관계

### LSU와 Cache

LSU(Load Store Unit)는 메모리 접근 시 Cache를 우선 조회한다.

```
LSU 메모리 접근 요청
  → L1 Cache 조회
  → L2 Cache 조회
  → L3 Cache 조회
  → RAM 접근
```

### Pipeline과 Cache

Pipeline의 MEM(Memory Access) 단계는 Cache 접근 결과에 직접 의존한다.

```
Cache Hit  → MEM 단계 빠른 완료 → Pipeline 유지
Cache Miss → MEM 단계 지연     → Pipeline Stall
```

### Branch Predictor와 Cache

잘못된 분기 예측은 실제로 실행되지 않을 경로의 데이터를 Cache에 올려 Cache Pollution을 유발한다.

```
잘못된 Speculative Execution
  → 불필요한 데이터 Cache 적재
  → Cache Pollution 발생
```

---

## 16. JVM과 Cache 효율

### Java 객체 분산 문제

Java 객체는 GC Heap에 분산 배치될 수 있어 메모리 연속성이 낮아질 수 있다.

```
Object A (주소 0x1000)
Object B (주소 0x5A30)  ← 비연속
Object C (주소 0x8F00)  ← 비연속
```

### Cache 친화적 구조 선택

| 구조 | Cache 효율 | 이유 |
|------|-----------|------|
| `int[]`, `long[]` | 높음 | 연속 메모리 배치 |
| `ByteBuffer` (Direct) | 높음 | Off-Heap 연속 메모리 |
| `ArrayList<Integer>` | 낮음 | 박싱으로 인한 포인터 분산 |
| `LinkedList<T>` | 낮음 | 노드 분산 배치 |

---

## 17. 고성능 시스템의 Cache 최적화 전략

Netty, Kafka, Redis 등 고성능 시스템에서 사용하는 대표 전략이다.

| 전략 | 목적 |
|------|------|
| Sequential Access | Spatial Locality 활용, Cache Hit 증가 |
| Off-Heap Memory | JVM GC 영향 제거, 메모리 배치 직접 제어 |
| False Sharing 방지 | Cache Line 충돌 제거 |
| Padding 사용 | 독립 데이터를 별도 Cache Line에 분리 |
| Ring Buffer | 연속 메모리 구조로 Cache 효율 유지 |

---

## 18. 주요 성능 지표

| 지표 | 의미 |
|------|------|
| Cache Hit Ratio | Cache 적중률 (높을수록 좋음) |
| Cache Miss Ratio | Cache Miss 비율 (낮을수록 좋음) |
| Memory Stall Cycles | 메모리 대기로 낭비된 CPU 사이클 수 |
| IPC | 사이클당 명령 처리량 |
| LLC Miss | L3 (Last Level Cache) Miss 발생 수 |

---

## 19. Linux 및 SRE 관점 모니터링

### CPU Cache 구조 확인

```bash
lscpu
# Cache 항목에서 L1d / L1i / L2 / L3 크기 확인 가능
```

### Hardware Counter 분석

```bash
perf stat <command>
perf top
```

### Cache Miss 분석

```bash
perf stat -e cache-misses,cache-references <command>
# Cache Hit Ratio = 1 - (cache-misses / cache-references)
```

---

## 20. CPU 내부 실행 흐름 전체 연결

```
Branch Predictor (분기 예측)
  ↓
Instruction Fetch (명령어 인출)
  ↓
Pipeline (명령어 디코딩 / 실행)
  ↓
LSU (메모리 접근 요청)
  ↓
L1 → L2 → L3 Cache 조회
  ↓ (Miss 시)
RAM 접근
  ↓
ALU / FPU 연산 완료
```

---

## 21. 구성 요소 요약

| 구성 요소 | 역할 |
|-----------|------|
| L1 Cache | Core 전용 최고속 Cache |
| L2 Cache | L1 Miss 보완 |
| L3 Cache | Core 간 공유 대용량 Cache |
| Cache Line | Cache 저장 및 전송 단위 (64 Bytes) |
| Cache Hit | Cache에서 데이터 즉시 반환 |
| Cache Miss | 하위 계층 탐색 후 RAM 접근 |
| Spatial Locality | 인접 메모리 연속 접근 특성 |
| Temporal Locality | 최근 사용 데이터 재사용 특성 |
| False Sharing | 독립 데이터가 동일 Cache Line 공유로 발생하는 충돌 |
| Cache Coherency | 멀티코어 간 Cache 데이터 일관성 유지 |
| Cache Pollution | 불필요한 데이터로 인한 Cache 효율 저하 |

---

## 22. 성능 최적화 핵심 요소

```
High Cache Hit Ratio
+ Low Memory Stall
+ Efficient Data Locality (Spatial / Temporal)
+ Low Cache Contention (False Sharing 방지)
= High IPC
= High Throughput + Low Latency
```

이 원칙은 다음 환경의 성능 최적화와 직접 연결된다.

- JVM 기반 서버의 자료구조 및 메모리 배치 설계
- Kubernetes 환경에서의 CPU Cache 활용
- Netty / Kafka / Redis 등 고성능 I/O 처리
- 금융 시스템 저지연 처리
- 대규모 트래픽 처리 아키텍처 설계

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*