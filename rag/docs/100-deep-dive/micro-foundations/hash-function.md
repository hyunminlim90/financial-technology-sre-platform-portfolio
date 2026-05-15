# Hash Function (해시 함수)
> FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**Hash Function(해시 함수)** 은 임의 길이의 데이터를 **고정 길이의 값(Hash Value)** 으로 변환하는 **단방향 함수**다.

결과값은 다음과 같이 부른다.

- Hash Value
- Digest
- Hash Code

### 핵심 특징

| 특징 | 의미 |
|------|------|
| 단방향성 | 해시값으로 원본 복원 불가 |
| 고정 길이 출력 | 입력 크기와 무관 |
| Avalanche Effect | 입력 1bit 변경 시 결과 완전 변경 |
| Deterministic | 동일 입력 → 동일 결과 |

### 대표 알고리즘

- SHA-256
- SHA-512
- BLAKE3
- MD5 *(보안용 비권장)*
- SHA-1 *(보안용 비권장)*

---

## 2. 시스템 어디에서 등장하는가

해시 함수는 시스템 전반에 등장한다.

**결제 인증**
- 요청 무결성 검증
- Signature 생성

**비밀번호 저장**
- Password Hashing
- Salt 적용

**JWT / API 인증**
- HMAC
- Token Signature

**분산 시스템**
- Consistent Hashing
- Sharding
- Cache Routing

**DB / Storage**
- Index Hash
- Deduplication
- Checksum

**JVM**
- HashMap
- HashSet
- Object `hashCode()`

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 영향이 큰 자원: **CPU**

**이유:** 해시 함수는 비트 연산과 산술/논리 연산의 반복 수행이기 때문.

특히 많이 사용하는 연산:

- XOR
- Shift
- Rotate
- AND
- ADD

대규모 암호화 환경에서는 다음도 영향이 큼:

- CPU Cache
- SIMD Unit
- Crypto Accelerator

---

## 4. 왜 중요한가

핀테크 시스템에서 해시는 **무결성과 인증의 핵심**이다.

### 대표 역할

| 역할 | 의미 |
|------|------|
| 데이터 위변조 검증 | Integrity |
| 비밀번호 보호 | Security |
| Signature 생성 | Authentication |
| 데이터 분산 | Sharding |
| 세션 라우팅 | Load Balancing |

> ⚠️ 해시 오류는 곧 **결제 무결성 오류**로 연결 가능.

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation

대량 암호화/해시 계산 발생 시 CPU 사용률 급증 가능.

- TLS 폭증
- Signature 검증 폭증

### Hash Collision

서로 다른 데이터가 동일 Hash 생성.

- HashMap 성능 급락
- 데이터 충돌

### Retry Storm

서명 검증 실패 시 재시도 증가 → API 폭주 가능.

### GC Pressure

대량 `byte[]` 생성 시 Heap Allocation 증가 → GC 증가 발생 가능.

### Crypto Hotspot

특정 CPU Core에 암호화 부하 집중 가능.

### Tail Latency 증가

해시 계산이 CPU를 오래 점유하면:

- Event Loop Delay
- Thread Queue 증가

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Bitwise Operation

해시 함수의 핵심 연산.

```
XOR / Shift / Rotate
```

### Avalanche Effect

입력 일부 변경 시 출력 완전 변화. 무결성의 핵심 특성.

### Salt

Password Hash 강화용 랜덤 값. Rainbow Table 공격 방지.

### HMAC

Hash + Secret Key 조합. API 인증의 핵심.

### SIMD / SHA-NI

최신 CPU는 해시 가속 명령 제공.

- Intel SHA Extensions
- AVX2
- AVX-512

### HashMap Bucket

Java 내부에서도 해시 기반 데이터 분산 사용. Collision 많으면 성능 저하.

### Consistent Hashing

분산 시스템 노드 분배 핵심. Kafka / Redis / CDN 등에 사용.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률

```bash
top
mpstat -P ALL 1
```

### Crypto CPU Hotspot

```bash
perf top
# 대표 함수: sha256_transform, aes_encrypt
```

### JVM Profiling

```bash
jfr
async-profiler
```

### TLS/암호화 확인

```bash
openssl speed sha256
```

### Kubernetes CPU 상태

```bash
kubectl top pod
```

### Hardware Crypto 지원 확인

```bash
lscpu | grep sha
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*