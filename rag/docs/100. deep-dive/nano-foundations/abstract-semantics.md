# 추상적 시맨틱스 (Abstract Semantics / 추상적 의미론)

> 정독: 0회

## 1. 이 기술이 무엇인가

**추상적 시맨틱스(Abstract Semantics)** 는:

> 데이터나 연산이 논리적으로 무엇을 의미하는지 정의하는 개념 체계

### 핵심 관점

중요한 것은 **어떻게 저장되었는가**가 아니라, **무엇을 의미하는가**입니다.

### 예시

동일한 비트 배열이라도 해석 방식에 따라 의미가 달라집니다.

| 비트 해석 방식 | 의미 |
|---|---|
| UTF-8 문자열 | 텍스트 |
| JPEG 포맷 | 이미지 |
| IEEE754 | 부동소수점 |
| ELF 구조 | 실행 파일 |

추상적 시맨틱스는 물리적 표현이 아니라 **논리적 의미**를 다룹니다.

### 매우 중요한 점

CPU·SSD·RAM은 의미를 이해하지 못합니다. 단지 비트 이동, 전압 상태, 주소 계산만 수행합니다.

의미는 상위 계층의 **파일 포맷, 프로토콜 규약, 애플리케이션 로직, 데이터 모델**에서 발생합니다.

---

## 2. 시스템 어디에서 등장하는가

추상적 시맨틱스는 시스템 전반에 존재합니다.

### 파일 시스템

물리적으로는 단순한 바이트 배열이지만, 상위에서는 사진, 문서, 로그, 데이터베이스로 해석됩니다.

### 네트워크

TCP 패킷 자체는 단순 바이트 스트림이지만, 상위 프로토콜은 다음과 같이 해석합니다.

| 데이터 | 의미 |
|---|---|
| HTTP GET | 웹 요청 |
| SQL Query | DB 질의 |
| JSON | 구조화 데이터 |
| TLS Handshake | 암호 협상 |

### 데이터베이스

스토리지 입장에서는 페이지(page)와 블록(block)이지만, DB 엔진 입장에서는 트랜잭션, 인덱스, 레코드로 해석됩니다.

### Kubernetes

etcd 내부는 단순 key-value 저장이지만, 상위에서는 Deployment, Pod, Service, Desired State의 의미 체계로 동작합니다.

### AI / LLM

GPU 입장에서는 행렬 연산이지만, 상위 의미론에서는 언어, 질문, 추론, 지식으로 해석됩니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

추상적 시맨틱스는 직접 자원 소비보다는 **상위 시스템 설계와 데이터 해석 방식**에 영향을 줍니다.

- **CPU**: 의미 해석 과정(parsing, decoding, validation, serialization)에서 CPU 사용
- **Memory**: 의미 객체 유지(object graph, cache structure, semantic model)에서 메모리 사용
- **Network**: 프로토콜 의미 체계(REST, RPC, message schema)에 영향
- **Disk**: filesystem format, DB page structure, WAL semantics, metadata interpretation에 영향

> 핵심: 추상적 시맨틱스는 물리 자원 자체보다 **데이터를 어떻게 해석할지** 결정합니다.

---

## 4. 왜 중요한가

이것이 없으면 시스템은 **의미 없는 비트 집합**에 불과합니다.

중요한 이유는 다음과 같습니다.

- 데이터 해석 가능성
- 시스템 간 상호운용성
- API 계약 유지
- 데이터 독립성
- 애플리케이션 논리 유지
- 분산 시스템 상태 일관성

### Semantic Stability

물리 구조가 바뀌어도 **논리적 의미는 유지**되어야 합니다.

**예시**: 스토리지를 HDD → NVMe SSD로 교체할 때

| 변경되는 것 | 유지되어야 하는 것 |
|---|---|
| physical latency | 파일 내용 의미 |
| block allocation | 트랜잭션 의미 |
| IO path | 데이터 정합성 |

---

## 5. 실제 장애와 어떤 관련이 있는가

대부분의 심각한 장애는 물리 오류보다 **의미론 손상(Semantic Corruption)** 에서 발생합니다.

### 대표 사례

**데이터 정합성 손상**
- 물리 저장은 정상이지만 트랜잭션 의미가 깨짐
- 예시: double payment, lost update, stale read

**API Semantic Drift**
- JSON schema 변경 후 상위 서비스가 다른 의미로 해석하는 문제

**Filesystem 사례**
- 파일은 존재하지만 inode corruption, metadata inconsistency 발생 시 논리 의미 상실

**Kubernetes 사례**
- etcd 데이터는 존재하지만 desired state semantics가 깨지면 orphan pod, stuck rollout, inconsistent reconciliation 발생

### SRE 핵심

SRE는 단순 프로세스 생존보다 **시스템 의미론 유지**를 더 중요하게 봅니다.

> 프로세스는 살아 있어도 결제 의미가 깨지거나, 이벤트 순서가 깨지거나, exactly-once semantics이 붕괴되면 장애입니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

핵심 메커니즘은 **하위 비트 구조 위에 상위 의미 체계를 계층적으로 부여**하는 것입니다.

```
Physical Bits
  → Structured Bytes
    → File Format
      → Protocol Model
        → Application Meaning
```

**예시**

| 계층 | 내용 |
|---|---|
| 물리 계층 | `01101010` |
| 파일 포맷 계층 | JPEG Header |
| 애플리케이션 계층 | 가족 사진 |

의미는 물리 계층이 아니라 **해석 규칙**에서 발생합니다.

### Semantic Contract

시스템 간 **동일한 데이터 의미를 공유**해야 합니다. 분산 시스템에서 timestamp semantics, transaction semantics, ordering semantics가 불일치하면 장애가 발생합니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

추상적 시맨틱스는 직접 눈에 보이는 커널 객체가 아니라 **상위 데이터 해석 구조**로 관측됩니다.

### Linux

```bash
# 바이트 의미 해석
file sample.bin

# 메타데이터 해석
stat
ls -l

# 패킷 의미 해석
tcpdump
wireshark
```

### Runtime

관측 대상: serialization, schema validation, protocol decoding, transaction semantics

### Kubernetes

| Object | Semantic Meaning |
|---|---|
| Deployment | desired replica intent |
| Service | logical endpoint |
| PVC | persistence contract |

### 장애 분석 핵심

중요한 것은 **프로세스 생존 여부보다 의미 일관성 유지 여부**입니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*