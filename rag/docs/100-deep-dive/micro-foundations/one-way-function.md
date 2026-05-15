# One-way Function (단방향 함수)
## FinTech 결제 시스템 SRE 관점 Micro Foundations

> 정독: 0회

## 1. 이 기술이 무엇인가

**One-way Function(단방향 함수)** 은:

- 입력값으로 결과값을 계산하는 것은 **쉽지만**,
- 결과값만으로 원래 입력값을 복원하는 것은 **사실상 불가능한 함수**

를 의미한다.

### 대표 예시

- `SHA-256`
- `SHA-512`
- `bcrypt`
- `Argon2`

### 핵심 특징

| 특징 | 의미 |
|------|------|
| Forward Easy | 계산은 빠름 |
| Reverse Hard | 역산은 매우 어려움 |
| Deterministic | 같은 입력 → 같은 결과 |
| Avalanche Effect | 입력 일부 변경 시 결과 완전 변경 |

---

## 2. 시스템 어디에서 등장하는가

단방향 함수는 **보안 및 무결성 영역 전반**에 등장한다.

### 비밀번호 저장
- `Password → Hash` 형태로 저장
- 원문 저장 금지

### API Signature
- `HMAC`
- `JWT Signature`
- `Request Integrity Check`

### TLS / 인증서
- Certificate Signature
- Handshake 검증

### 결제 데이터 무결성
- 요청 데이터 변조 여부 검증

### 로그 마스킹
- 민감정보 직접 저장 대신 Hash 저장

### Blockchain / Ledger
- 트랜잭션 무결성 검증

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 영향: **CPU**

> 단방향 함수는 대량의 비트 연산과 논리 연산 반복 수행이기 때문

### Memory 영향도 큼

특히 아래 알고리즘은 의도적으로 Memory 사용량 증가:

- `bcrypt`
- `scrypt`
- `Argon2`

**이유:** GPU/ASIC 공격 방지

---

## 4. 왜 중요한가

핀테크 시스템에서 **원본 데이터를 노출하지 않고도 검증 가능**하게 만들기 때문.

### 핵심 역할

| 역할 | 의미 |
|------|------|
| 비밀번호 보호 | 원문 저장 방지 |
| 데이터 무결성 | 위변조 탐지 |
| 인증 | Signature 검증 |
| 개인정보 보호 | Masking |
| Ledger 보호 | 데이터 변조 방지 |

> ⚠️ 단방향 함수가 깨지면: **보안 체계 전체 붕괴 가능**

---

## 5. 실제 장애와 어떤 관련이 있는가

### CPU Saturation
대량 Signature 검증 시 CPU 사용률 급증 가능

- TLS Handshake 폭증
- JWT 검증 폭증

### Authentication Bottleneck
로그인 요청 폭증 시 `bcrypt` / `Argon2` 계산으로 인해 **인증 서버 병목** 발생 가능

### Retry Storm
서명 검증 실패 시 Retry 증가 → 인증 재시도 폭주 가능

### Hash Collision 공격
약한 Hash 사용 시 위조 가능성 증가 → 보안 사고 가능

### Tail Latency 증가
CPU Core가 암호화 연산에 장시간 점유되면:
- Event Loop Delay
- API 응답 지연 가능

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### Hash Function
대표적인 단방향 함수. 입력 → Digest 생성.

### Avalanche Effect
입력 1bit 변경 시 결과 완전 변화. 무결성 핵심 특성.

### Salt
Password Hash에 랜덤값 추가. Rainbow Table 공격 방지.

### HMAC
Secret Key 기반 단방향 검증. API 인증 핵심.

### Computational Hardness
역산 비용을 극단적으로 크게 만듦.

### bcrypt / Argon2
의도적으로 CPU 사용량 증가 + Memory 사용량 증가 → **공격 비용 증가**

### SHA-NI / Crypto Extension
최신 CPU는 단방향 함수 계산 가속 지원.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### CPU 사용률
```bash
top
mpstat -P ALL 1
```

### Crypto Hotspot
```bash
perf top
```
대표 함수: `sha256_transform`, `bcrypt_hash`, `aes_encrypt`

### JVM Profiling
```bash
jfr
# 또는
async-profiler
```

### OpenSSL Benchmark
```bash
openssl speed sha256
```

### Hardware Crypto 지원 확인
```bash
lscpu | grep sha
```

### Kubernetes CPU 상태
```bash
kubectl top pod
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*