# CPU 레지스터 (CPU Register)

> 정독: 0회

## 1. 이 기술이 무엇인가

CPU 레지스터(Register)는:

> CPU 내부에 존재하는 초고속 저장 공간

CPU는 연산을 수행할 때 대부분의 데이터를 먼저 레지스터로 가져온 뒤 처리합니다.

핵심적으로 레지스터는 다음 역할을 수행합니다.

- 데이터 저장
- 주소 저장
- 명령어 저장
- 연산 결과 저장
- CPU 상태 저장

CPU가 직접 접근하는 저장 공간 중 가장 빠른 계층입니다.

---

## 2. 시스템 어디에서 등장하는가

CPU가 실행하는 거의 모든 작업에 등장합니다.

**명령어 실행**

- Instruction Fetch
- Instruction Decode
- Instruction Execute

과정에서 사용됩니다.

**메모리 접근**

RAM, Cache, Storage에서 읽은 데이터는 최종적으로 레지스터에 적재된 후 연산됩니다.

**하드웨어 제어**

- Device Register
- Control Register
- Status Register

형태로 사용됩니다.

**네트워크**

NIC(Network Interface Controller)의 송신 제어, 수신 제어, 인터럽트 제어, DMA 설정 등을 담당하는 레지스터가 존재합니다.

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

가장 직접적인 자원은 **CPU**입니다. 레지스터는 CPU 내부에 존재하기 때문입니다.

간접적으로는 **Memory** 성능에도 큰 영향을 줍니다. 레지스터에 데이터가 오래 머물수록 RAM 접근과 Cache 접근이 감소합니다.

---

## 4. 왜 중요한가

CPU의 실제 연산은 대부분 레지스터 위에서 수행됩니다.

예를 들어 `A + B`를 계산할 경우, 실제로는 다음 순서로 진행됩니다.

```
RAM → Register
RAM → Register
ALU 연산
Register → RAM
```

따라서 레지스터 활용이 비효율적이면 다음 문제가 발생합니다.

- 메모리 접근 증가
- 캐시 미스 증가
- CPU Stall 증가

현대 CPU 성능 최적화의 상당 부분은 **레지스터 활용 극대화**에 집중되어 있습니다.

---

## 5. 실제 장애와 어떤 관련이 있는가

### Context Switch 증가

스레드 전환 시 모든 레지스터 상태를 저장하고 복원하는 작업이 발생합니다. 과도한 Context Switch는 CPU 사용률 증가를 유발합니다.

### 인터럽트 폭주

인터럽트 발생 시 현재 레지스터 상태 저장 → 인터럽트 처리 → 원래 상태 복원 과정이 반복됩니다.

### Register Pressure

컴파일러가 사용할 레지스터가 부족하면 레지스터 → 메모리 저장 및 메모리 → 레지스터 복구가 반복됩니다. 이를 **Register Spill**이라고 하며, 성능 저하의 원인이 됩니다.

### 하드웨어 제어 오류

장치 레지스터 값이 잘못 설정되면 NIC 오동작, DMA 오류, 인터럽트 오류 등이 발생할 수 있습니다.

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘

현재까지 정리한 개념들과의 연결 흐름은 다음과 같습니다.

```
Data Field
↓
Bit Field Member
↓
Bit Field Member Value
↓
Bit Field Encoding
↓
Bit Field Image Data
↓
CPU Register
↓
Bitwise Operation
↓
Hardware Action
```

예를 들어 Control Register가 있다고 가정하면 다음과 같은 비트 필드 구조를 가질 수 있습니다.

```
Bit 0     : Enable
Bit 1     : Interrupt
Bit 2~3   : Mode
```

CPU는 레지스터를 읽어 비트 마스킹 → 비트 추출 → 비트 테스트를 수행하여 상태를 확인합니다.

또는 레지스터 읽기 → 비트 조작 → 레지스터 쓰기를 수행하여 장치를 제어합니다.

중요한 점은 **비트 필드 이미지 데이터가 실제로 존재하는 장소 중 하나가 CPU 레지스터**라는 것입니다.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**CPU 정보 확인**

```bash
lscpu
```

**레지스터 상태 확인 (gdb)**

```bash
gdb
info registers
```

**프로세스 분석**

```bash
perf
strace
```

**인터럽트 및 CPU 상태**

```bash
/proc/interrupts
/proc/cpuinfo
```

### Kubernetes

Kubernetes에서 직접 레지스터를 확인하지는 않습니다. 하지만 아래 계층이 모두 레지스터 기반으로 동작합니다.

- Container Runtime
- Kernel Scheduler
- Network Stack
- eBPF
- Device Driver

예를 들어 `Pod → veth → NIC Driver` 경로에서, NIC 드라이버는 내부적으로 장치 레지스터를 읽고 씁니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*