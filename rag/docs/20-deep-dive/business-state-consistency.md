## Business Domain 계층의 Session / Payment 상태</summary>

Lock(Mutex/Spinlock)은 단순한 [CPU 동기화 기술](../20-deep-dive/thread-synchronization.md)`이 아니라 **비즈니스 데이터 무결성(Data Integrity)을 보호하기 위한 수단**입니다.

특히 아래와 같은 Business Domain 상태는 동시에 여러 요청(Request)이 접근할 수 있기 때문에 동시성 제어가 매우 중요합니다:

- Session
- Payment
- Order
- Balance
- Inventory

</br>

## 대표적인 예시

동일 결제(`paymentId`)에 대해 다음이 동시에 들어올 수 있습니다:

```text
Request A → 승인 처리
Request B → 승인 취소
Request C → 중복 승인 요청
```

적절한 동시성 제어가 없으면:

- 중복 결제
- 이중 승인
- 상태 불일치
- 데이터 손상

등이 발생할 수 있습니다.

## 왜 Lock이 필요한가?

Business Domain 계층에서는 **"동일 상태를 동시에 변경하지 못하도록"** 보호해야 합니다.

예:

```text
현재 결제 상태: PENDING

Thread A → APPROVED 변경
Thread B → FAILED 변경

→ 동시 발생 시 최종 상태 불일치 가능
```

따라서 다음을 사용하여 동일 Business 상태 변경 순서를 제어합니다:

- DB Row Lock
- Distributed Lock
- Optimistic Lock
- `synchronized`
- CAS (Compare-And-Set)

## SRE 관점에서 왜 중요한가?

Business Domain 계층의 Lock Contention은 단순 CPU 문제가 아니라 **서비스 신뢰성 문제**로 이어질 수 있습니다.

| 계층 | 영향 |
|---|---|
| Queue / Buffer 경합 | Throughput 저하 |
| DB Connection Pool 경합 | 응답 지연 |
| Payment 상태 경합 | 결제 실패 / 중복 승인 위험 |

> **어떤 공유 자원에서 Lock Contention이 발생했는가**에 따라 장애 심각도가 달라집니다.

## Business Domain 락의 특징

이 계층의 Lock은 외부 API / DB Transaction / 결제 승인 / 정산 처리 등과 연결되는 경우가 많습니다.

따라서 **Lock 유지 시간이 상대적으로 길어질 수 있습니다.**

예:

```text
결제 승인 요청
→ PG 응답 대기
→ DB Commit 대기
→ 이 동안 동일 상태를 보호하기 위해 Lock 유지
```

이 경우:

```text
Lock Contention 증가
→ Request Queue 증가
→ Timeout 증가
→ 사용자 Latency 증가
```

## 실무적으로 중요한 이유

결제/세션 계층의 동시성 문제는 성능 저하보다 **데이터 정합성(Data Consistency) 문제**가 더 치명적입니다.

CPU를 조금 더 쓰는 것보다 다음을 막는 것이 훨씬 중요합니다:

- 중복 결제
- 이중 승인
- 상태 불일치

특히 FinTech / Payment 환경에서는 다음이 최우선 원칙입니다:

```text
No Duplicate Payment
No Double Approval
No Inconsistent State
```

## 핵심 요약

> Business Domain 계층의 Lock은 단순 성능 제어가 아니라,  
> Session / Payment 상태의 **무결성과 신뢰성을 보호하기 위한 동시성 제어 메커니즘**입니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*