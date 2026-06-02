# 05. Observability and Deep Evidence Strategy

> 정독: 1회

**목적**: SRE 플랫폼의 관측 체계를 1차(상시 자동화)와 2차(심층 진단)로 구분하고, 각 도구의 역할, 사용 시점, 플랫폼과의 협동 전략을 정의합니다.

## 1. 관측 계층 구조 개요

```
결제 시스템 앱 (장애 주입 / 모니터링 대상)
    │
    ▼
[1차 관측 계층] 상시 자동 모니터링
    Prometheus / Loki / Tempo / Alertmanager
    → SRE 플랫폼으로 Evidence 전달
    │
    ▼
[SRE 플랫폼 판단]
    Evidence 평가 → Scenario Match → Recommendation 생성
    │
    ├─ Evidence 충분 → 권장 조치 생성 → Human Approval
    │
    └─ Evidence 부족 → 심층 진단 권고 → Human Approval
                            │
                            ▼
                   [2차 관측 계층] 심층 진단 도구
                       eBPF / perf / async-profiler / jstack / tcpdump
                       → 추가 Evidence 수집
                       → 플랫폼 재평가
                       → experiments / systems-math / 논문 근거
```

## 2. 1차 관측 계층: 상시 자동 모니터링

### 2-1. 역할

```
목적: 장애 감지 자동화, SRE 플랫폼의 Evidence 수집 자동화
특성:
    상시 수집 (always-on)
    낮은 오버헤드
    자동 알람 (Alertmanager)
    SRE 플랫폼 판단 로직에 직접 연결
```

### 2-2. 도구별 역할

| 도구 | 수집 대상 | SRE 플랫폼 활용 방식 |
|------|----------|----------------------|
| Prometheus | CPU, Memory, JVM Metrics, Error Rate, Latency | Evidence 수치 수집, Alertmanager 알람 트리거 |
| Loki | 애플리케이션 로그, JVM 로그, Kubernetes 이벤트 로그 | 로그 패턴 매칭, 에러 빈도 수집 |
| Tempo | 분산 트레이스, API 레이턴시, DB 쿼리 시간 | Latency 이상 감지, 호출 체인 추적 |
| Alertmanager | 임계값 초과 시 알람 라우팅 | SRE 플랫폼 최초 트리거 |

### 2-3. Evidence 수집 예시 (JVM CPU Saturation)

```
Prometheus:
    JVM CPU Utilization: 97%
    Load Average (1m): 21.4
    ThreadPoolExecutor Active Threads: 200 / 200 (포화)
    Error Rate: 12.4%

Tempo:
    P99 API Latency: 3.1s (정상: 120ms)
    DB Query P99: 1.8s

Loki:
    TimeoutException: 184건 / 1분
    Connection refused: 47건 / 1분
```

## 3. 2차 관측 계층: 심층 진단 도구

### 3-1. 1차 알람 시스템에 포함하지 않는 이유

```
높은 수집 비용      → 상시 수집 시 시스템 오버헤드 증가
높은 권한 요구      → eBPF는 CAP_BPF 또는 root 권한 필요
운영 리스크         → 잘못된 eBPF 프로그램은 커널 충돌 가능
데이터 과잉         → 상시 수집 시 분석 불가능한 데이터량
해석 난이도         → 운영자 전문성 필요
```

### 3-2. 사용 시점

```
1차 관측:  Alert 발생
SRE 플랫폼: Evidence 평가
    │
    └─ Evidence Reliability 부족 판단
            ↓
        심층 진단 권고 생성
            ↓
        Human Approval (심층 진단 도구 실행 승인)
            ↓
        운영자가 심층 진단 도구 실행 (diagnostic-agent가 실행 대행)
            ↓
        결과를 추가 Evidence로 플랫폼에 제출 (diagnostic-agent가 수집 대행)
            ↓
        Evidence Reliability 재평가
            ↓
        Recommendation 재생성
```

### 3-3. 도구별 역할 및 사용 목적

#### eBPF 계열

| 도구 | 목적 | 수집 정보 |
|------|------|-----------|
| `profile-bpfcc` | CPU 핫스팟 프로파일링 | 함수별 CPU 점유 비율 |
| `runqlat-bpfcc` | CPU Run Queue Latency 측정 | P50/P95/P99 스케줄러 대기 시간 |
| `offcputime-bpfcc` | Off-CPU 분석 | 블로킹 원인 (I/O wait, lock wait) |
| `tcpretrans` | TCP 재전송 추적 | 네트워크 불안정 여부 |
| `biolatency-bpfcc` | 블록 I/O 레이턴시 | Disk I/O 병목 |

#### perf 계열

| 도구 | 목적 | 수집 정보 |
|------|------|-----------|
| `perf top -p <PID>` | JVM 프로세스 CPU 핫스팟 | 함수별 실시간 CPU 점유 |
| `perf record / perf report` | 상세 CPU 프로파일 | 호출 스택, 함수 샘플 |
| `perf stat` | 하드웨어 성능 카운터 | Cache Miss, Context Switch, IPC |

#### JVM 전용

| 도구 | 목적 | 수집 정보 |
|------|------|-----------|
| `async-profiler` | JVM CPU / Memory 프로파일 (low overhead) | Flame Graph 생성 가능 |
| `jstack <PID>` | JVM 스레드 덤프 | Thread 상태, Deadlock 감지 |
| `jcmd <PID> VM.classloader_stats` | Class Loader 누수 감지 | 로더 인스턴스 수 |
| `jcmd <PID> GC.heap_dump` | 힙 덤프 수집 | 메모리 누수 원인 분석 |

#### 네트워크 / 시스템

| 도구 | 목적 | 수집 정보 |
|------|------|-----------|
| `tcpdump` | 패킷 캡처 | 네트워크 레벨 병목, 패킷 손실 |
| `strace -p <PID>` | 시스템 콜 추적 | 과다 시스템 콜, Blocking I/O |
| `ss -s` | 소켓 상태 요약 | TCP 연결 상태, CLOSE_WAIT 누수 |

## 4. 플랫폼과의 협동 전략 (Full Flow)

```
[1단계] 상시 자동 감지
    Prometheus Alert: CPU 97%
    Loki: Timeout 184건
    Tempo: P99 3.1s
        ↓
[2단계] SRE 플랫폼 판단
    Evidence 평가
    Scenario Match: JVM CPU Saturation (신뢰도 72%)
        ↓
[3단계] Evidence 부족 판단 시
    플랫폼: "심층 진단이 필요합니다. 아래 도구 실행을 권장합니다."
    권장 도구:
        perf top -p <PID>
        async-profiler -p <PID> -d 30
        runqlat-bpfcc
        jstack <PID>
    Human Approval: 승인
        ↓
[4단계] 운영자 심층 진단 실행 (diagnostic-agent가 진단 실행)
    perf top 결과:
        ThreadPoolExecutor.run: 71% CPU
    async-profiler Flame Graph:
        DB Connection Wait 확인
    eBPF runqlat:
        P99 Run Queue Latency: 1.8ms
        ↓
[5단계] 추가 Evidence 플랫폼 제출 (diagnostic-agent가 수집)
    Evidence Reliability: 72% → 91% 상승
    Scenario Match 정확도 상승
        ↓
[6단계] 최종 Recommendation 생성
    1순위 권장: ThreadPool 크기 조정 + DB Connection Pool 점검
    Rollback 계획 포함
    Verification 기준 포함
    Human Approval
        ↓
[7단계] Verification → Re-Assessment 
        ↓
[8단계] 필요시 반복
        ↓
[9단계] Incident Close (사용자 종료 선언 + 시스템 정상화 평가)
```

## 5. Evidence 등급 분류

```
Primary Evidence (1차 자동 수집)
    Prometheus: 정량 메트릭 (CPU, Memory, Latency, Error Rate)
    Loki: 로그 패턴 및 빈도
    Tempo: 분산 트레이스 레이턴시

Secondary Evidence (2차 심층 진단)
    perf: 함수 레벨 CPU 핫스팟
    eBPF: 커널 레벨 스케줄러 / I/O 분석
    async-profiler: JVM 레벨 Flame Graph
    jstack: Thread 상태 덤프
    tcpdump: 네트워크 패킷 레벨 분석
```

## 6. 논문에서 심층 진단 데이터의 역할

```
논문 심사 관점에서:

약한 기술:
    CPU 97%였습니다.

강한 기술:
    Prometheus에서 CPU 97%를 확인했고,
    perf top으로 ThreadPoolExecutor.run이 CPU의 71%를 소비하는 것을 확인했으며,
    eBPF runqlat으로 Run Queue Latency P99가 1.8ms까지 증가한 것을 관측했다.
    이를 통해 CPU 포화의 직접 원인이 ThreadPool 포화임을 확인하였다.

→ 2차 Deep Evidence가 논문 심사 가치를 결정한다.
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*