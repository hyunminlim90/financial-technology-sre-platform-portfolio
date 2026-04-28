# Scenario: Kafka Consumer Lag

---

## 1. 시나리오 개요

이 시나리오는 Kafka Consumer가 메시지를 처리하지 못하고 지연이 누적되는 상황을 가정합니다.

Kafka 기반 시스템에서 Lag은 단순 성능 문제가 아니라 다음을 의미합니다.

```text
시스템이 들어오는 이벤트 속도를 처리 속도가 따라가지 못하고 있다
```

---

## 2. 장애 정의

### 조건

```text
consumer lag > 1000
또는
consumer lag 지속 증가
또는
lag 증가 상태가 5분 이상 지속
```

---

### 실무 기준 (권장)

```text
lag 증가 추세 + 감소하지 않음
= 실제 장애
```

---

## 3. 사용자 영향

```text
- 결제 승인 결과 지연
- 결제 상태 업데이트 지연
- 사용자 결제 완료까지 대기 증가
- webhook / 알림 지연
```

---

## 4. 시스템 영향 범위

```text
- Kafka Cluster
- Payment Worker (Consumer)
- External Payment Provider
- Downstream 시스템 (정산, 알림)
```

---

## 5. 주요 증상

---

### 5.1 Metrics

```text
kafka.consumer.lag 증가
kafka.consumer.records-lag-max 증가
consumer 처리량 감소
```

---

### 5.2 Logs

```text
consumer processing delay
retry 증가
timeout 증가
```

---

### 5.3 특징적인 패턴

```text
lag이 지속적으로 우상향
consumer는 정상 동작하지만 backlog 증가
```

---

## 6. 원인 후보

---

### 6.1 Consumer 처리 속도 부족

```text
- CPU 부족
- thread 부족
- 비효율 로직
```

---

### 6.2 Downstream 지연 (핵심)

```text
- External API latency 증가
- DB write 지연
```

---

### 6.3 Retry 폭증

```text
- 실패 → retry → lag 증가
```

---

### 6.4 Partition 불균형

```text
- 특정 partition에만 load 집중
```

---

### 6.5 Consumer 장애

```text
- pod restart
- consumer crash
```

---

### 6.6 Poison Message

```text
- 특정 메시지에서 계속 실패
- offset 진행 안됨
```

---

## 7. 탐지 방법

---

### PromQL

```promql
sum(kafka_consumer_lag) by (group)
```

---

```promql
max(kafka_consumer_records_lag_max)
```

---

```promql
rate(kafka_consumer_records_consumed_total[1m])
```

---

### Alert 기준

```text
lag > threshold AND 증가 추세 유지
```

---

## 8. 진단 흐름

```text
1. lag 증가 여부 확인
2. 특정 topic인지 전체인지 구분
3. consumer 정상 동작 여부 확인
4. 처리 속도 vs 유입 속도 비교
5. downstream 병목 확인
```

---

## 9. 재현 방법 (Simulation)

---

### 패턴 A. Consumer 성능 저하

```text
CPU limit 설정 낮춤
→ 처리 속도 감소
→ lag 증가
```

---

### 패턴 B. External API 지연

```text
Mock API 응답 5~10초 지연
→ consumer 처리 속도 급감
→ lag 증가
```

---

### 패턴 C. Retry 폭증

```text
의도적으로 실패 응답 반환
→ retry 증가
→ lag 폭증
```

---

### 패턴 D. Poison Message

```text
특정 메시지에서 예외 발생
→ offset commit 안됨
→ lag 고정
```

---

## 10. 핵심 포인트

```text
Kafka Lag은 “느림”이 아니라 “적체(backlog)” 문제다
```

---

## 11. SRE 관점 핵심 통찰

```text
lag 증가 = 시스템이 감당 못하는 상태
```

---

### 중요 구분

```text
Latency 문제 → 요청 느림
Lag 문제 → 결과 늦음
```

---

## 12. Runbook 연결

```text
runbooks/kafka/consumer-lag.md
```

---

## 13. 요약

```text
1. lag은 backlog의 신호다
2. consumer 속도 vs 입력 속도 비교가 핵심
3. downstream dependency가 주요 원인이다
```

---

## 14. 핵심 메시지

> Kafka Lag은 시스템이 느린 것이 아니라,
> **시스템이 따라가지 못하고 있다는 신호다.**
