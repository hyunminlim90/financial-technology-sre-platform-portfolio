# JVM GC Thread와 Garbage Collection 실행 흐름

## 1. 개요

GC Thread(Garbage Collection Thread)는 JVM이 Heap 메모리를 관리하기 위해 생성하고 실행하는 **시스템 Thread**입니다.

- 애플리케이션 비즈니스 로직을 직접 처리하지 않습니다.
- JVM Heap에 생성된 객체 중 더 이상 참조되지 않는 객체를 식별하고, 해당 메모리 공간을 회수합니다.

```
GC Thread = JVM Heap 메모리 회수를 담당하는 JVM 내부 시스템 실행 흐름
```

### 계층적 위치

```
Application Layer  →  API Thread / Kafka Consumer Thread / Scheduler Thread / GC Thread
JVM Layer          →  Garbage Collector
OS Layer           →  Native Thread / Kernel Thread
Hardware Layer     →  Logical CPU → Hardware Thread → Physical Core
```

GC Thread는 운영체제 관점에서 다른 Java Thread와 동일하게 CPU 스케줄링 대상입니다.

---

## 2. GC 대상 영역

GC Thread가 관리하는 핵심 대상은 **JVM Heap**입니다.

```
JVM Memory
├── Heap
│   ├── Young Generation  (새로 생성된 객체)
│   ├── Old Generation    (오래 살아남은 객체)
│   └── Object Data
├── Stack
├── Metaspace
└── Code Cache
```

대부분의 Java 객체는 Heap에 생성되며, 여러 Java Thread가 Heap 객체를 공유할 수 있습니다.

### GC가 필요한 이유

지속적인 객체 생성으로 Heap이 소진되면 다음 문제가 발생합니다.

```
Heap 사용량 증가 → 객체 할당 실패 → OutOfMemoryError
```

GC는 참조되지 않는 객체의 메모리를 회수하여 이 상황을 방지합니다.

---

## 3. 객체 생존 판단 기준

### Reachable vs Unreachable

| 구분 | 설명 |
|------|------|
| Reachable Object | GC Root에서 참조 가능한 객체 → 회수 대상 아님 |
| Unreachable Object | 더 이상 참조되지 않는 객체 → 회수 대상 |

### GC Root

GC Root는 객체 참조 탐색의 시작점입니다.

| GC Root 유형 | 설명 |
|--------------|------|
| Thread Stack 변수 | 현재 실행 중인 메서드의 지역 변수 |
| Static 변수 | 클래스 단위로 유지되는 참조 |
| JNI 참조 | Native 코드에서 유지하는 참조 |
| JVM 내부 참조 | ClassLoader, Monitor 등 |

---

## 4. GC 실행 단계

GC는 일반적으로 Mark → Sweep → Compact 순서로 동작합니다. Collector 종류에 따라 세부 동작은 다르지만, 객체 생존 판정과 메모리 회수라는 핵심은 동일합니다.

### Mark

GC Root에서 시작하여 참조 그래프를 따라가며 도달 가능한 객체를 표시합니다.

```
GC Root → Reference Traversal → Reachable Object Mark
```

### Sweep

Mark되지 않은 객체가 차지하던 메모리를 회수합니다.

```
Unmarked Object → Memory Reclaim
```

### Compact

살아남은 객체를 한쪽으로 모아 메모리 파편화를 줄이고, 연속된 빈 공간을 확보합니다.

```
Live Objects → Relocation → Contiguous Free Space
```

---

## 5. Stop-the-World (STW)

STW는 GC 수행 중 **애플리케이션 Thread 전체가 일시 정지**되는 현상입니다.

```
GC Start → Application Threads Pause → GC Work → Application Threads Resume
```

### STW가 필요한 이유

GC는 객체 참조 관계를 정확하게 파악해야 합니다. 애플리케이션 Thread가 계속 객체 참조를 변경하면 GC가 일관된 참조 그래프를 확인할 수 없으므로, 일부 단계에서 애플리케이션 Thread를 정지시킵니다.

### STW의 성능 영향

| 영향 대상 | 결과 |
|-----------|------|
| HTTP API Thread | 응답 지연 증가 |
| Netty EventLoop | 네트워크 이벤트 처리 지연 |
| Kafka Consumer Thread | Poll / Heartbeat 지연 |
| Scheduler Thread | 주기 작업 지연 |
| 전체 서비스 | Tail Latency 증가 |

### Kafka Consumer와 STW 영향

```
STW Pause → Consumer Poll 지연 → Heartbeat 지연 → Consumer Lag 증가 → Rebalance 가능성 증가
```

### Netty EventLoop와 STW 영향

```
STW Pause → EventLoop 정지 → Read/Write Event 처리 지연 → Request Latency 증가
```

---

## 6. GC Thread의 CPU / 메모리 자원 사용

GC Thread는 참조 탐색, 메모리 회수, 객체 이동 과정에서 CPU를 소비합니다. GC 작업이 많아지면 비즈니스 Thread가 사용할 수 있는 CPU 시간이 줄어들 수 있습니다.

| 자원 | 영향 |
|------|------|
| CPU | GC Thread 실행으로 비즈니스 Thread CPU 시간 감소 |
| LSU | Heap 메모리 Load/Store 증가 |
| Cache | 대량 객체 탐색으로 Cache Miss 증가 가능 |
| Memory Bandwidth | 대량 객체 스캔 시 사용량 증가 |

---

## 7. GC 대상 영역별 구분

| 구분 | 대상 | 특성 |
|------|------|------|
| Minor GC | Young Generation | 빈번, 비용 낮음 |
| Major GC | Old Generation | 상대적으로 높은 비용 |
| Full GC | Heap 전체 및 관련 영역 | 가장 높은 비용 |

### 객체 이동 흐름

```
New Object → Young Generation → (Minor GC 생존) → Survivor → (반복 생존) → Old Generation
```

Promotion(Old 영역 이동)이 많아질수록 Old Generation 압박이 증가합니다.

### Allocation Rate와 GC 빈도

```
Allocation Rate 증가 → Young Generation 압박 증가 → Minor GC 빈도 증가
```

---

## 8. 대표 GC 알고리즘

| GC | 특징 |
|----|------|
| Serial GC | 단일 Thread 중심 |
| Parallel GC | Throughput 중심 |
| G1GC | Region 기반, Pause Time 목표 |
| ZGC | Low Latency 중심 |
| Shenandoah | Concurrent GC 중심 |

### G1GC

Heap을 Region 단위로 나누어 회수 효율이 높은 Region을 우선적으로 선택하여 Pause Time을 제어합니다.

```
Heap
├── Region 1
├── Region 2
├── Region 3
└── Region N
```

### Concurrent GC

일부 Collector는 애플리케이션 Thread와 동시에 GC 작업을 수행하여 STW 시간을 줄입니다. 단, 완전히 STW가 없어지는 것은 아닙니다.

---

## 9. Memory Leak

Memory Leak은 실제로 필요하지 않은 객체가 계속 참조되어 GC 대상이 되지 않는 상황입니다.

```
Unused Object + Still Referenced → Not Collectable → Heap 사용량 지속 증가
```

GC가 실행되어도 Heap 사용량이 줄지 않는다면 Memory Leak을 의심해야 합니다.

---

## 10. 운영(SRE) 관점

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| GC Pause Time | STW 또는 GC 정지 시간 |
| GC Frequency | GC 발생 빈도 |
| Allocation Rate | 객체 생성 속도 |
| Promotion Rate | Old 영역 이동 속도 |
| Heap Usage | Heap 사용량 |
| Old Gen Usage | Old 영역 사용량 |
| GC CPU Time | GC Thread CPU 사용량 |
| Full GC Count | Full GC 발생 횟수 |

### 주요 장애 패턴

| 패턴 | 원인 | 결과 |
|------|------|------|
| 긴 GC Pause | High Heap Usage + Old Gen 압박 + 대형 객체 그래프 | API 응답 지연, Kafka Lag, Timeout 증가 |
| 잦은 Minor GC | High Allocation Rate + 단명 객체 다수 | CPU 사용량 증가, 응답 지연 |
| Full GC 반복 | Old Gen 부족 + Memory Leak 가능성 | OutOfMemoryError 위험 |

### 운영 설계 원칙

| 원칙 | 설명 |
|------|------|
| 객체 생성량 관리 | Allocation Rate 감소 |
| Heap 크기 적정화 | GC 빈도와 Pause 시간 균형 |
| GC 로그 수집 | 원인 분석 가능성 확보 |
| Collector 선택 | Latency / Throughput 목표에 맞춤 |
| Memory Leak 탐지 | Old Gen 지속 증가 확인 |
| Kafka / Netty 연계 관찰 | STW 영향 확인 |

---

## 11. GC 분석 도구

### GC 로그 수집 옵션

```bash
# Java 9+
-Xlog:gc*:file=gc.log:time,uptime,level,tags

# Java 8 이하
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

### Thread Dump에서 GC Thread 확인

Collector 종류에 따라 이름이 다르며, 대표 예시는 다음과 같습니다.

```
GC Thread
G1 Main Marker
G1 Conc#
VM Thread
```

### Heap Dump 분석 항목

| 항목 | 설명 |
|------|------|
| Dominator Tree | 메모리 점유 구조 |
| Retained Size | 객체가 보유하는 총 메모리 |
| Reference Chain | GC Root까지의 참조 경로 |
| Leak Suspect | 누수 의심 객체 |

---

## 12. 전체 실행 흐름

```
Java Application
      ↓
Object Allocation
      ↓
JVM Heap Usage 증가
      ↓
GC Trigger
      ↓
GC Thread 실행
      ↓
Mark / Sweep / Compact
      ↓
Memory Reclaim
      ↓
Application Thread Resume
```

---

## 13. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| GC Thread | JVM Heap 메모리 회수 실행 흐름 |
| Heap | Java 객체 저장 영역 |
| GC Root | 생존 객체 탐색 시작점 |
| Mark | 생존 객체 식별 |
| Sweep | 미참조 객체 회수 |
| Compact | 메모리 파편화 감소 |
| STW | 애플리케이션 Thread 일시 정지 |
| Minor GC | Young 영역 회수 |
| Major GC | Old 영역 회수 |
| Full GC | 전체 Heap 중심 회수 |

### 결론

```
GC Thread = JVM 내부 시스템 Thread + Heap 객체 생존 분석 + 메모리 회수
```

GC Thread는 애플리케이션 비즈니스 로직과 직접 관련되지 않지만, STW와 CPU 소비를 통해 전체 애플리케이션 성능에 직접적인 영향을 줍니다.

```
High Allocation Rate + Old Gen 압박 + Long GC Pause
= API Latency 증가 + Kafka Lag 증가 + Netty EventLoop 지연
```

따라서 JVM 기반 서버에서는 GC Thread를 단순한 내부 기능이 아니라, **전체 Software Thread 실행 흐름에 영향을 주는 핵심 시스템 Thread**로 관리해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*