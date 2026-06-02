# 06. Diagnostic Evidence and Tooling

> 정독: 1회

**목적**: 심층 진단 도구(eBPF, perf, async-profiler, jstack, tcpdump 등)의 실험 문서 기록 표준 형식을 정의합니다.

experiments/, systems-math/, postmortems/ 문서에서 Deep Evidence를 어떻게 기록하고 논문 근거로 활용하는지를 명세합니다.

## 1. 심층 진단 기록의 필요성

연구 논문에서 요구되는 심층 진단 기록 수준:

```
단순 기록 (연구 가치 낮음):
    CPU가 97%였으며 장애가 발생했습니다.

논문 수준 기록 (연구 가치 높음):
    Prometheus에서 CPU 97%를 감지하였다.
    perf top을 통해 ThreadPoolExecutor.run이 전체 CPU의 71%를
    소비하고 있음을 확인하였다.
    eBPF runqlat 분석 결과, CPU Run Queue Latency P99가
    1.8ms까지 증가한 것을 관측하였다.
    이를 기반으로 CPU 포화의 직접 원인이 ThreadPool 포화임을 확인하고
    Connection Pool 조정과 ThreadPool 크기 재설정을 권장 조치로 선정하였다.
```

## 2. experiments/ 문서 Deep Evidence 기록 표준 형식

experiments/ 문서에는 아래 섹션을 포함한다.

### 2-1. 실험 메타데이터

- 실험 ID: EXP-20260115-001
- 실험 날짜: 2026-01-15
- 스택: JVM Runtime
- 장애 유형: CPU Saturation
- 실험 Phase: Phase 1 (단일 장애)
- 실험 환경:
    - Kubernetes: 1.31
    - JDK: 21 (OpenJDK)
    - Spring Boot: 3.4.0
    - WebFlux / Netty
    - 컨테이너 사양: 4 CPU / 8GB Memory

### 2-2. 장애 주입 명세

### 주입 도구
stress-ng

### 주입 명령
stress-ng --cpu 16 --timeout 600

### 주입 의도
JVM 프로세스 외부에서 CPU 포화를 유발하여
ThreadPool 대기 시간 및 API Latency 증가 측정

### 2-3. 1차 Evidence (Primary Evidence)

```markdown
## Primary Evidence

### Prometheus 수집 결과
| 지표 | 정상값 (Baseline) | 주입 중 측정값 | 단위 |
|------|-------------------|----------------|------|
| JVM CPU Utilization | 35% | 97% | % |
| Load Average (1m) | 2.1 | 21.4 | - |
| ThreadPool Active Threads | 45 / 200 | 200 / 200 | threads |
| HTTP Error Rate | 0.1% | 12.4% | % |
| JVM Metaspace Used | 320MB | 321MB | MB |

### Loki 로그 패턴
| 로그 패턴 | 빈도 (정상) | 빈도 (주입 중) |
|-----------|------------|----------------|
| TimeoutException | 0건/분 | 184건/분 |
| Connection refused | 0건/분 | 47건/분 |
| Thread pool exhausted | 0건/분 | 31건/분 |

### Tempo 트레이스
| 지표 | 정상값 | 주입 중 측정값 | 단위 |
|------|--------|----------------|------|
| API P99 Latency | 120ms | 3,100ms | ms |
| DB Query P99 | 45ms | 1,800ms | ms |
| Trace Completion Rate | 99.8% | 87.2% | % |
```

### 2-4. 2차 Evidence (Secondary / Deep Evidence)

```markdown
## Secondary Evidence (Deep Diagnostic)

### 승인 정보
- 심층 진단 권고 시각: 2026-01-15 14:23:11 KST
- Human Approval: 승인 (14:23:45 KST)

### perf 분석

#### 실행 명령
perf top -p 12345 -d 10

#### 관측 결과
| 함수 | CPU 점유율 |
|------|-----------|
| ThreadPoolExecutor.run | 71% |
| SocketInputStream.socketRead0 | 14% |
| GarbageCollector (G1GC) | 8% |
| 기타 | 7% |

#### 판단 기여
ThreadPool의 CPU 점유가 71%로 확인됨
→ ThreadPool 포화가 CPU Saturation의 직접 원인으로 판단

### eBPF runqlat 분석

#### 실행 명령
runqlat-bpfcc -p 12345 10

#### 관측 결과
| 구간 | Latency |
|------|---------|
| P50 | 85us |
| P95 | 420us |
| P99 | 1,800us (1.8ms) |
| Max | 12,400us |

#### 판단 기여
CPU Run Queue Latency P99가 1.8ms까지 증가
→ CPU 스케줄러 대기가 전체 Latency에 기여하고 있음 확인

### async-profiler 분석

#### 실행 명령
./profiler.sh -p 12345 -d 30 -f /tmp/cpu_profile.html

#### 관측 결과
Flame Graph 주요 핫스팟:
    HikariCP.getConnection: 38% (DB Connection 대기)
    ThreadPoolExecutor.execute: 23%
    JSON Serialization: 11%

#### 판단 기여
DB Connection Pool 고갈로 인한 대기가 전체 CPU의 38%를 차지
→ Connection Pool 크기 조정이 필요한 근거 확보

### jstack 분석

#### 실행 명령
jstack 12345 > /tmp/thread_dump_20260115_142350.txt

#### 주요 스레드 상태
| 상태 | 스레드 수 |
|------|-----------|
| RUNNABLE | 198 |
| WAITING (HikariCP) | 47 |
| BLOCKED (DB Lock) | 12 |
| TIMED_WAITING (sleep) | 8 |

#### 판단 기여
WAITING 상태 스레드 47개 → DB Connection Pool 대기 확인
```

## 3. systems-math/ 문서 Deep Evidence 기록 표준 형식

```markdown
## Primary Evidence 테이블

| Evidence 출처 | 지표 | Baseline | Injection | 단위 |
|---------------|------|----------|-----------|------|
| Prometheus | CPU Utilization | 35% | 97% | % |
| Prometheus | HTTP Error Rate | 0.1% | 12.4% | % |
| Tempo | API P99 Latency | 120 | 3,100 | ms |
| Loki | TimeoutException | 0 | 184 | 건/분 |

## Secondary Evidence 테이블

| Evidence 출처 | 도구 | 지표 | 측정값 |
|---------------|------|------|--------|
| perf top | perf | ThreadPoolExecutor.run CPU 점유 | 71% |
| eBPF runqlat | eBPF | Run Queue Latency P99 | 1.8ms |
| async-profiler | JVM Profiler | HikariCP.getConnection 점유 | 38% |
| jstack | JVM | WAITING 스레드 수 | 47개 |

## MTTD / MTTR

| 지표 | 측정값 |
|------|--------|
| MTTD (감지까지 소요 시간) | 35초 |
| MTTR (복구까지 소요 시간) | 17분 |
| Recommendation → Approval 소요 | 2분 15초 |
| Rollback 성공 여부 | 성공 |
| Verification 통과 여부 | 통과 |

## Recommendation 순환 횟수

| 순환 | 권장 조치 | 결과 |
|------|-----------|------|
| 1차 | ThreadPool 상태 점검 | CPU 95% → 여전히 높음 |
| 2차 | DB Connection Pool 확장 | CPU 65% → 개선 |
| 3차 | Connection Pool 최적화 + ThreadPool 재조정 | CPU 38% → 정상 |
```

## 4. 장애 유형별 권장 심층 진단 도구 매핑

| 장애 유형 | 1순위 심층 도구 | 2순위 심층 도구 | 주요 확인 항목 |
|-----------|----------------|----------------|----------------|
| JVM CPU Saturation | perf top | async-profiler | 함수별 CPU 점유, Flame Graph |
| JVM Memory Leak | jcmd GC.heap_dump | Eclipse MAT OQL | GC Root 참조 체인 |
| JVM Class Loader Leak | jcmd VM.classloader_stats | jcmd GC.heap_dump | 로더 인스턴스 수 |
| JVM Thread Deadlock | jstack | jcmd Thread.print | BLOCKED 스레드 상태 |
| Redis Latency Spike | eBPF biolatency | tcpdump | I/O Latency, 패킷 손실 |
| Kafka Consumer Lag | eBPF offcputime | jstack | Polling 스레드 블로킹 |
| Container OOM | eBPF runqlat | perf stat | 메모리 압력, GC 빈도 |
| Network Timeout | tcpdump | eBPF tcpretrans | TCP 재전송, 패킷 손실 |
| CPU Throttling (Cgroup) | eBPF runqlat | perf stat | Run Queue Latency, IPC |

## 5. 논문 데이터 체인 전체 구조

```
실험 수행 (experiments/)
    │
    ├── Primary Evidence (Prometheus / Loki / Tempo)
    │       → experiments/ 1차 Evidence 섹션
    │
    └── Secondary Evidence (eBPF / perf / async-profiler / jstack)
            → experiments/ 2차 Evidence 섹션
                    │
                    ▼
            systems-math/ 정량 데이터셋
                    │
                    ▼
            논문 Methodology 섹션
            논문 Experiments 섹션
            논문 Results 섹션
                    │
                    ▼
            논문 심사 → 석사 / 박사 학위
```

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*