# CPU Branch Predictor(분기 예측기)와 분기 실행 구조

## 1. Branch Predictor란?

Branch Predictor(분기 예측기)는 **CPU 내부에서 분기 명령의 실행 경로를 예측하는 하드웨어 구성 요소**입니다.

CPU는 `if`, `else`, `switch`, `for`, `while`과 같은 조건 분기 명령을 실행할 때 실제 조건 결과가 계산되기 전에 다음 실행 경로를 미리 선택하려고 시도합니다.

| 목적 | 설명 |
|---|---|
| **Pipeline 유지** | 명령어 흐름 중단 방지 |
| **Stall 감소** | 분기 결과 대기 최소화 |
| **IPC 향상** | 사이클당 명령 처리량 증가 |
| **CPU 활용도 향상** | 유휴 시간 감소 |

---

## 2. 분기 명령(Branch Instruction)

분기 명령은 프로그램 실행 흐름을 변경하는 명령입니다.

```java
if (value > 10) {
    processA();
} else {
    processB();
}
```

CPU는 조건 결과에 따라 서로 다른 경로를 실행해야 합니다.

---

## 3. 왜 Branch Predictor가 필요한가?

### Pipeline 구조

현대 CPU는 여러 명령어를 동시에 서로 다른 단계에서 병렬 처리하는 **Pipeline** 구조를 사용합니다.

```text
Fetch → Decode → Execute → Memory Access → Write Back
```

CPU는 Pipeline을 비우지 않고 지속적으로 명령어를 공급해야 최대 성능을 유지할 수 있습니다.

### 분기 명령의 문제

분기 명령은 다음 실행 주소를 즉시 결정할 수 없습니다.

```text
if (x > 0)
→ 실제 결과는 Execute 단계 이후에야 계산됨
→ 하지만 CPU는 이전 단계에서 다음 명령어를 가져와야 함
```

예측이 없다면 CPU는 조건 결과가 나올 때까지 Pipeline을 멈춰야 합니다.

```text
Branch Result Unknown
→ Next Instruction Unknown
→ Pipeline Stop (Branch Stall)
```

---

## 4. Branch Prediction 기본 실행 흐름

```text
Branch Instruction 발견
        ↓
Branch Predictor 예측 수행
        ↓
예측된 경로의 명령어 Fetch
        ↓
Speculative Execution 수행
        ↓
실제 조건 계산
        ↓
예측 성공 또는 실패 판정
```

---

## 5. Speculative Execution

CPU는 예측된 경로의 명령어를 실제 결과 이전에 미리 실행할 수 있습니다.

```java
if (flag) {
    processA();
}
```

CPU가 `flag == true`라고 예측하면 `processA()` 명령어를 미리 실행합니다.  
실제 결과가 맞다면 그대로 Commit됩니다.

---

## 6. Branch Prediction 성공과 실패

### 예측 성공

```text
Prediction Correct
→ Pipeline Continue
→ High IPC
→ Low Stall
```

### 예측 실패

잘못 실행한 명령어를 모두 폐기하고 올바른 경로를 다시 로드해야 합니다.

```text
Prediction Incorrect
→ Speculative Instructions Discard
→ Pipeline Flush
→ Correct Path Reload
```

**예측 실패의 성능 영향:**

| 영향 | 설명 |
|---|---|
| **Pipeline Flush** | 기존 작업 제거 |
| **Instruction Re-fetch** | 명령어 재로드 |
| **CPU Cycle 손실** | 수십 사이클 손실 가능 |
| **IPC 감소** | 처리량 감소 |
| **Latency 증가** | 응답 시간 증가 |

---

## 7. Branch Target Buffer (BTB)

CPU는 과거 분기 정보를 저장하기 위해 **Branch Target Buffer(BTB)** 를 사용합니다.

| 저장 정보 | 설명 |
|---|---|
| **Branch Address** | 분기 명령 위치 |
| **Target Address** | 이동 대상 주소 |
| **Taken 여부** | 이전 분기 결과 |
| **History Pattern** | 실행 패턴 기록 |

CPU는 반복되는 패턴을 학습합니다. 예를 들어 동일 분기가 연속으로 `true`였다면 다음에도 `true`일 가능성이 높다고 판단합니다.

---

## 8. 루프와 Branch Prediction

반복문은 예측 성공률이 매우 높습니다.

```java
for (int i = 0; i < 1000; i++) { }
```

```text
Taken × 999 → Not Taken × 1
```

대부분의 반복에서 조건이 동일 패턴을 유지하므로 Branch Predictor 적중률이 높습니다.

---

## 9. 예측이 어려운 코드 구조

| 유형 | 예시 | 이유 |
|---|---|---|
| **랜덤 데이터 기반 조건** | `if (randomValue > threshold)` | 결과 패턴이 일정하지 않음 |
| **복잡한 중첩 조건** | `if (a) { if (b) { if (c) { } } }` | 분기 경로 수 증가 |
| **데이터 의존적 분기** | `if (userInput == target)` | 외부 입력 기반으로 예측 불가 |

---

## 10. Branch Prediction과 Cache 관계

예측 실패는 Cache 효율에도 영향을 줍니다.

```text
Wrong Branch
→ 잘못된 경로의 명령어 Fetch
→ Instruction Cache Pollution
→ LSU가 불필요한 데이터 미리 읽음
→ Cache Pollution 증가
```

---

## 11. 현대 CPU의 Branch Prediction 기술

| 기술 | 설명 |
|---|---|
| **Global History** | 전체 분기 패턴 분석 |
| **Local History** | 특정 분기 패턴 분석 |
| **Two-Level Predictor** | 다단계 예측 |
| **TAGE Predictor** | 고급 히스토리 기반 예측 |
| **Hybrid Predictor** | 여러 Predictor 조합 |

---

## 12. JVM 및 애플리케이션 관점

### Hot Path 최적화

자주 실행되는 코드 경로의 조건문을 단순화하면 CPU 효율이 향상됩니다.

```java
if (likelyCondition) {
    fastPath();
}
```

### 데이터 정렬 효과

정렬된 데이터는 예측 가능성을 높입니다.

```java
Arrays.sort(data);
// 연속된 패턴 → Predictor 적중률 향상
```

### 불규칙한 분기가 많은 영역

| 영역 | 영향 |
|---|---|
| **금융 계산** | 조건 분기 증가 |
| **정책 엔진** | 복잡한 if-chain |
| **Rule Engine** | 동적 분기 증가 |
| **AI 추론** | 불규칙 분기 |

---

## 13. Branch Prediction 최적화 전략

| 전략 | 목적 |
|---|---|
| **Hot Path 단순화** | Prediction Accuracy 향상 |
| **연속 데이터 사용** | 패턴 예측 향상 |
| **중첩 조건 감소** | Branch Depth 감소 |
| **Switch 최적화** | Jump Table 활용 |
| **불필요한 분기 제거** | Pipeline 안정화 |
| **Branchless Programming** | 조건문 자체 감소 |

### Branchless Programming

분기 자체를 제거해 CPU 내부적으로 조건 이동(CMOV) 명령으로 최적화될 수 있습니다.

```java
max = (a > b) ? a : b;
```

---

## 14. LSU / Pipeline / Branch Predictor 관계

```text
Branch Predictor
        ↓
Instruction Fetch 결정
        ↓
Pipeline 실행 유지
        ↓
LSU 메모리 접근 수행
        ↓
ALU / FPU 연산 수행
```

---

## 15. 전체 개념 정리

| 구성 요소 | 역할 |
|---|---|
| **Branch Predictor** | 분기 결과 예측 |
| **Speculative Execution** | 예측 기반 사전 실행 |
| **BTB** | 분기 이력 저장 |
| **Pipeline Flush** | 예측 실패 시 초기화 |
| **Branch Stall** | 분기 결과 대기 |
| **IPC** | 사이클당 명령 처리량 |
| **Prediction Accuracy** | 예측 적중률 |

---

## 핵심 결론

현대 CPU는 단순 연산 장치가 아니라 **분기 패턴을 지속적으로 분석하는 고도의 병렬 실행 시스템**입니다.

```text
Branch Prediction Accuracy 향상
  → Pipeline 유지
  → Stall 감소
  → IPC 향상
  → CPU 효율 증가
  → 전체 시스템 성능 향상
```

이는 JVM / 대규모 트래픽 처리 / 논블로킹 서버 / 데이터 처리 엔진 / 금융 시스템과 같은 고성능 애플리케이션의 처리량과 Latency에 직접적인 영향을 줍니다.

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*