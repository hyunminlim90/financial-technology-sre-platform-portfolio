# 해시값 (Hash Value)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**해시값(Hash Value)** 은:

> 원본 데이터를 해시 함수(Hash Function)에 입력하여 생성된 **고정 길이 결과값**

즉, **데이터의 디지털 지문(Digital Fingerprint)** 역할을 수행한다.

### 예시

- 원본 데이터: `paymentId=12345&amount=10000`
- SHA-256 해시 결과: `2b4d7a4f0d9e...`

### 핵심 특징

| 특징 | 의미 |
|------|------|
| 고정 길이 | 입력 크기와 무관 |
| 결정론적 | 동일 입력 → 동일 결과 |
| 민감성 | 입력 일부 변경 시 결과 전체 변경 |
| 사실상 유일성 | 충돌 확률 매우 낮음 |

---

## 2. 시스템 어디에서 등장하는가

### 결제 API Signature
- `HMAC-SHA256`
- Request Validation

### Password Storage
- Password Hash
- Salted Hash

### TLS / HTTPS
- Certificate Integrity
- Digital Signature

### Blockchain
- Transaction Hash
- Block Hash

### Cache System
- Cache Key
- Deduplication

### Distributed System
- Consistent Hashing
- Partition Routing

### 로그 및 파일 검증
- File Checksum
- Integrity Validation

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접 영향: **CPU**

> 해시 계산은 내부적으로 `XOR`, `Rotate`, `Shift`, `Add` 같은 비트 연산을 반복 수행.
> 즉, **ALU + SIMD 연산 집중 사용** 구조.

### Memory 영향

낮은 편. 하지만 아래 환경에서는 Cache/Memory 압박 가능:

- 대규모 concurrent hashing
- TLS handshake 폭증

---

## 4. 왜 중요한가

핵심 목적: **데이터 무결성 보장**

### 해시값이 중요한 이유

| 목적 | 의미 |
|------|------|
| Integrity | 데이터 변조 감지 |
| Authentication | 요청 검증 |
| Deduplication | 중복 요청 탐지 |
| Security | 비밀번호 보호 |
| Routing | 데이터 분산 |

### FinTech에서 특히 중요한 이유

결제 데이터는 **1bit 변경도 치명적**일 수 있음:

- 금액 변조
- 계좌 변조
- 승인 데이터 손상

---

## 5. 실제 장애와 어떤 관련이 있는가

### Signature Mismatch
해시값 불일치 발생 시 결제 승인 실패 / API Reject 가능

### Retry Storm
서명 검증 실패 반복 시 Client Retry → Traffic Explosion 가능

### CPU Saturation
대량 TLS/HMAC 처리 시 Hash 계산 CPU 폭증 발생 가능

### Collision Risk
약한 해시 사용 시 보안 우회 / 데이터 충돌 가능

### Cache Inconsistency
Hash Key 불안정 시 Cache Miss 증가 → Throughput 감소 가능

### Blockchain Integrity Failure
Hash chain 깨지면 Ledger Integrity 손상 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Hash Function
원본 데이터를 고정 길이 값으로 변환

### Avalanche Effect
입력 일부 변경 시 출력 전체 변화

### Collision Resistance
서로 다른 입력이 동일 결과 생성 어려움

### Bitwise Operation
핵심 내부 연산: `XOR`, `Shift`, `Rotate`

### Digest
최종 생성 결과값. 즉, `Hash Value = Digest`

### HMAC
Secret Key 기반 Hash 인증 방식

### Consistent Hashing
분산 시스템 데이터 라우팅 핵심

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률
```bash
top
mpstat -P ALL 1
```

### OpenSSL Benchmark
```bash
openssl speed sha256
```

### 파일 Hash 확인
```bash
sha256sum file.txt
```

### JVM Profiling
```bash
jfr
async-profiler
```

### TLS 상태 확인
```bash
openssl s_client
```

### Kubernetes Pod CPU 확인
```bash
kubectl top pod
```

### perf Hotspot 확인
```bash
perf top
```
대표 함수: `sha256_transform`, `EVP_DigestUpdate`

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*