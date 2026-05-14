# Kafka Consumer Thread와 메시지 소비 실행 흐름

## 1. 개요

Kafka Consumer Thread는 Java 애플리케이션 내부에서 Kafka Broker로부터 메시지를 가져오고 처리하는 실행 흐름입니다.

기술적으로는 `java.lang.Thread` 기반이며, 운영 관점에서는 **Kafka 메시지 소비를 담당하는 특수 목적 Software Thread**입니다.

```
Kafka Consumer Thread
= Kafka Broker에서 메시지를 Poll하고 처리한 뒤 Offset을 Commit하는 실행 흐름
```

### 계층적 위치

```
Application Layer  →  Kafka Consumer Thread
JVM Layer          →  java.lang.Thread
OS Layer           →  Native Thread / Kernel Thread
Hardware Layer     →  Logical CPU → Hardware Thread → Physical Core
```

---

## 2. 메시지 소비 흐름

Consumer Thread는 다음 과정을 반복합니다.

```
Poll → Fetch → Deserialize → Process → Commit
```

### 2-1. Poll

Consumer Thread가 Kafka Broker에 메시지를 **직접 요청(Pull 방식)**합니다. Broker가 메시지를 밀어넣는 Push 방식이 아닙니다.

```
Consumer Thread → Kafka Broker (Fetch Request)
```

### 2-2. Fetch

Kafka Broker가 Topic Partition에 저장된 메시지를 Consumer에게 반환합니다.

```
Kafka Broker → Fetch Response → Consumer Thread
```

### 2-3. Deserialize

Broker에서 받은 바이트 데이터를 애플리케이션 객체로 변환합니다.

```
byte[] → JSON / Avro / Protobuf → Application Object
```

메시지 양이 많거나 변환이 복잡할수록 CPU 사용량이 증가합니다.

### 2-4. Process

역직렬화된 메시지를 비즈니스 로직에 전달합니다.

```
Payment Event → Validation → DB 저장 → 외부 API 호출 → 후속 이벤트 발행
```

### 2-5. Offset Commit

처리 완료된 메시지 위치를 Kafka에 기록합니다.

```
Processed Offset → Commit → Kafka Broker / Group Coordinator
```

Offset Commit은 Consumer가 어디까지 처리했는지를 나타내는 핵심 상태입니다.

---

## 3. Kafka Consumer Thread의 특성

### HTTP 흐름과의 차이

| 구분 | HTTP API 흐름 | Kafka Consumer 흐름 |
|------|--------------|---------------------|
| 실행 트리거 | 사용자 요청 | Kafka Broker 메시지 존재 |
| 실행 주체 | Web Thread / EventLoop | Consumer Poll Loop |
| 독립성 | 요청 기반 | 독립적으로 지속 실행 |

### Spring 애플리케이션 내 Thread 구분

| 구분 | 설명 |
|------|------|
| Main Thread | 애플리케이션 부팅 및 초기화 |
| Web/API Thread | 사용자 HTTP 요청 처리 |
| Scheduler Thread | 주기 작업 실행 |
| Kafka Consumer Thread | Kafka 메시지 소비 |

### 독립적 라이프사이클

```
Spring Application Start
        ↓
Kafka Listener Container Start
        ↓
Kafka Consumer Thread Start
        ↓
Poll Loop 유지 (사용자 요청과 무관하게 지속)
```

---

## 4. Kafka Broker와의 통신

### 통신 구조

```
Kafka Consumer Thread
        ↓
Kafka Client Library
        ↓
TCP Socket
        ↓
Kafka Broker
```

### 데이터 수신 경로

```
Kafka Broker → Network → NIC → Kernel Socket Buffer → Kafka Client → Consumer Thread
```

### Kafka Broker의 역할

| 역할 | 설명 |
|------|------|
| 메시지 저장 | Topic Partition에 메시지 보관 |
| 메시지 제공 | Consumer Fetch 요청 처리 |
| Offset 관리 | Consumer Group 처리 위치 관리 |
| Rebalance 관리 | Partition 소유권 재분배 |
| Replication | Broker 간 데이터 복제 |

---

## 5. Consumer Group, Partition, 병렬성

### Partition 할당 구조

하나의 Partition은 같은 Consumer Group 안에서 하나의 Consumer에게만 할당됩니다.

```
Topic
├── Partition 0 → Consumer A
├── Partition 1 → Consumer B
└── Partition 2 → Consumer C
```

### 병렬성 제한

```
최대 병렬 Consumer 수 ≈ Partition 수
```

Consumer Thread 수가 Partition 수를 초과하면 초과 Thread는 할당받을 Partition이 없습니다.

---

## 6. Heartbeat와 Rebalance

### Heartbeat

Consumer Thread는 자신이 정상 동작 중임을 Group Coordinator에 주기적으로 전송합니다.

```
Consumer Thread → Heartbeat → Group Coordinator
```

Heartbeat가 일정 시간 내에 도달하지 않으면 해당 Consumer를 장애로 판단합니다.

### Rebalance 발생 원인

| 원인 | 설명 |
|------|------|
| Consumer 추가 | 새 Consumer가 Group에 참여 |
| Consumer 종료 | 기존 Consumer 이탈 |
| Heartbeat 실패 | Consumer 장애로 판단 |
| Poll 지연 | 처리 지연으로 비정상 판단 |
| Partition 변경 | Topic Partition 수 변경 |

Rebalance 발생 중에는 소비가 일시적으로 중단됩니다.

---

## 7. Consumer Thread Blocking 문제

Consumer Thread 내부에서 오래 걸리는 작업이 발생하면 Poll Loop가 지연됩니다.

**주요 원인:**
- 외부 API Blocking 호출
- DB 지연
- 무거운 CPU 연산
- 대량 Deserialize
- Lock 대기

**문제 전파 흐름:**

```
Consumer Thread Blocking
        ↓
    Poll 지연
        ↓
  Heartbeat 지연
        ↓
  Rebalance 발생
        ↓
Consumer Lag 증가
```

---

## 8. Consumer Lag

```
Consumer Lag = Latest Offset - Committed Offset
```

Lag 증가는 메시지 생산 속도가 소비 속도를 초과하고 있음을 의미합니다.

### Consumer Lag 원인

| 원인 | 설명 |
|------|------|
| 처리 로직 지연 | Business Logic이 느림 |
| DB 지연 | 저장소 병목 |
| 외부 API 지연 | Downstream 의존성 |
| Deserialize 비용 증가 | CPU 사용 증가 |
| Partition 수 부족 | 병렬성 제한 |
| Consumer 수 부족 | 처리량 부족 |
| Rebalance 빈발 | 소비 중단 구간 증가 |

---

## 9. Offset Commit 전략

| 전략 | 설명 | 특성 |
|------|------|------|
| Auto Commit | 일정 주기마다 자동 Commit | 구현 단순, 처리 완료 전 Commit 시 유실 위험 |
| Manual Commit | 처리 완료 후 명시적 Commit | 안정성 높음, 구현 복잡도 증가 |

### 메시지 처리 보장 수준

| 방식 | 설명 |
|------|------|
| At-most-once | 최대 한 번 처리, 유실 가능 |
| At-least-once | 최소 한 번 처리, 중복 가능 |
| Exactly-once | 정확히 한 번 처리, 조건 제한 존재 |

일반적인 업무 시스템에서는 **At-least-once** 기준으로 두고, Consumer 로직을 멱등성 있게 설계합니다.

### 멱등성 설계 요소

| 설계 요소 | 목적 |
|-----------|------|
| 중복 키 관리 | 동일 이벤트 중복 처리 방지 |
| 처리 이력 저장 | 이미 처리한 메시지 식별 |
| 트랜잭션 처리 | DB 반영과 Offset 처리 일관성 보장 |
| Idempotent Logic | 재처리 안전성 확보 |

---

## 10. Worker Thread 분리 구조

무거운 작업은 Consumer Thread에서 직접 처리하지 않고 Worker Thread Pool로 분리합니다.

```
Consumer Thread → Poll → Worker Queue → Worker Thread Pool → Business Logic → Commit Coordination
```

이 구조에서는 **Offset Commit 순서와 처리 성공 여부 관리**가 중요합니다.

---

## 11. Netty Event Loop vs Kafka Consumer Thread 비교

| 항목 | Netty Event Loop | Kafka Consumer Thread |
|------|------------------|-----------------------|
| 목적 | 네트워크 이벤트 처리 | Kafka 메시지 소비 |
| 실행 방식 | Event-Driven | Poll Loop |
| 주요 대상 | Socket Channel | Kafka Broker |
| 주의점 | Blocking 금지 | Poll 지연 방지 |
| 병목 지표 | EventLoop Stall | Consumer Lag |

---

## 12. 운영(SRE) 관점

### 주요 모니터링 지표

| 지표 | 의미 |
|------|------|
| Consumer Lag | 메시지 적체량 |
| Poll Latency | Poll 호출 지연 |
| Processing Time | 메시지 처리 시간 |
| Commit Latency | Offset Commit 지연 |
| Rebalance Count | Rebalance 발생 횟수 |
| Heartbeat Failure | Consumer 생존 신호 실패 |
| Thread CPU Usage | Consumer Thread CPU 사용률 |
| GC Pause | JVM 정지 시간 영향 |

### 주요 장애 패턴

| 증상 | 원인 | 판단 방향 |
|------|------|-----------|
| Consumer Lag 증가 | 생산 속도 > 소비 속도 | 처리량 부족 또는 Downstream 병목 |
| Rebalance 반복 | Poll / Heartbeat 지연 | Consumer Thread Blocking 또는 설정 문제 |
| CPU 사용률 증가 | 대량 Deserialize, 복잡한 로직 | CPU Bound 병목 |
| 낮은 CPU + 높은 Lag | DB / API Blocking, Lock 대기 | I/O Bound 병목 |

### 운영 설계 원칙

| 원칙 | 설명 |
|------|------|
| Poll Loop 지연 최소화 | Rebalance 방지 |
| 처리 로직 멱등성 확보 | 중복 처리 대응 |
| Partition 수와 Consumer 수 정렬 | 병렬성 확보 |
| Blocking 작업 분리 | Consumer Thread 보호 |
| Commit 전략 명확화 | 유실/중복 위험 제어 |
| Lag 모니터링 | 처리 지연 조기 감지 |
| GC Pause 관리 | Heartbeat 지연 방지 |

---

## 13. 전체 처리 흐름

```
Producer
    ↓
Kafka Broker
    ↓
Topic / Partition
    ↓
Consumer Group
    ↓
Kafka Consumer Thread
    ↓
Poll → Deserialize → Business Logic → Offset Commit
```

---

## 14. 핵심 정리

| 구성 요소 | 역할 |
|-----------|------|
| Kafka Consumer Thread | 메시지 소비 실행 흐름 |
| Kafka Broker | 메시지 저장 미들웨어 |
| Poll | 메시지 요청 (Pull 방식) |
| Fetch | 메시지 수신 |
| Deserialize | 바이트 데이터를 객체로 변환 |
| Process | 비즈니스 로직 처리 |
| Offset Commit | 처리 위치 기록 |
| Heartbeat | Consumer 생존 신호 |
| Rebalance | Partition 재할당 |
| Consumer Lag | 소비 지연 지표 |

### 결론

```
Kafka Consumer Thread = Java Thread + Kafka Poll Loop + Message Processing + Offset Commit
```

Kafka Consumer Thread의 안정성은 다음 요소에 의해 결정됩니다.

```
Poll Loop 안정성
+ 처리 로직 속도
+ Offset Commit 전략
+ Heartbeat 유지
+ Rebalance 최소화
+ Consumer Lag 관리
= 안정적인 메시지 소비 구조
```

Kafka 기반 시스템에서는 Consumer Thread를 일반 Java Thread로만 보지 않고, **메시지 처리 파이프라인의 핵심 실행 흐름**으로 별도 관리해야 합니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*