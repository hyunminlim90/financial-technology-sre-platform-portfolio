# 장치 파일 (Device File / Device Node)

> 정독: 0회

## 1. 이 기술이 무엇인가

**장치 파일(Device File)** 은:

> 하드웨어 장치를 파일 시스템 내부의 파일 형태로 표현한 **커널의 특수 인터페이스 노드**

리눅스/유닉스는 **장치도 파일처럼 접근**하도록 설계되어 있습니다.

### 대표 예시

- `/dev/sda`
- `/dev/nvme0n1`
- `/dev/null`
- `/dev/tty`
- `/dev/random`

### 핵심 특징

장치 파일은 일반 데이터 파일이 아니며, 실제 사용자 데이터를 저장하지 않고 **하드웨어 접근 통로** 역할을 수행합니다.

### 핵심 목적

응용 프로그램이 `open()`, `read()`, `write()`, `close()` 만으로 장치를 제어할 수 있게 합니다.

---

## 2. 시스템 어디에서 등장하는가

주로 `/dev` 디렉터리에서 등장합니다.

| 분류 | 예시 |
|------|------|
| **Storage 장치** | `/dev/sda`, `/dev/sdb`, `/dev/nvme0n1` |
| **파티션** | `/dev/sda1`, `/dev/nvme0n1p1` |
| **Terminal** | `/dev/tty`, `/dev/pts/*` |
| **Random Device** | `/dev/random`, `/dev/urandom` |
| **Memory Device** | `/dev/mem`, `/dev/kmem` |
| **Virtual Device** | `/dev/null`, `/dev/zero` |
| **컨테이너 내부** | `/dev/null`, `/dev/random`, `/dev/shm` |
| **Virtualization** | VM 내부 장치도 device node로 노출 |

---

## 3. 어떤 자원에 가장 영향이 큰가

장치 종류에 따라 다르지만 일반적으로 **Disk + Kernel Memory** 영향이 큽니다.

| 자원 | 영향 | 설명 |
|------|------|------|
| **Disk** | 큼 | block device node는 storage I/O에 직접 연결 |
| **Memory** | 큼 | VFS inode/dentry cache 사용 |
| **CPU** | 있음 | system call 및 driver dispatch 영향 |
| **Network** | 있음 | network device도 device interface와 연결 |

> **핵심:** 장치 파일은 실제 I/O 경로의 **시작점** 역할을 합니다.

---

## 4. 왜 중요한가

운영체제의 **장치 추상화 핵심**입니다.

**이유 1. 장치 통일 인터페이스 제공**
모든 장치를 파일처럼 처리 가능.

**이유 2. 유저 공간 ↔ 커널 연결**
응용 프로그램이 직접 hardware access 하지 않고 device node를 통해 접근.

**이유 3. 드라이버 연결 핵심**
major/minor number 기반으로 **device node → driver** 매핑.

**이유 4. 자동 장치 관리**
udev/systemd가 동적으로 생성.

**이유 5. 보안 통제 가능**
permission 기반 접근 제한 가능. (예: `crw-rw---- root disk`)

---

## 5. 실제 장애와 어떤 관련이 있는가

### Device node missing

증상: `/dev/nvme0n1 not found`

원인: udev 문제, driver load 실패, hardware failure

### Permission 문제

증상: `Permission denied`

원인: device node permission 오류

### Driver mismatch

장치 파일은 존재하지만 read/write 실패, ioctl 실패 발생 가능.

### Kubernetes 문제

| 증상 | 원인 |
|------|------|
| container device mount 실패 | GPU device plugin 문제 |
| CSI volume attach 실패 | device node 연결 오류 |

### Storage 장애

device node가 사라지면 mount 실패, filesystem inaccessible 발생.

> **핵심:** device file은 실제 hardware 접근 가능 여부를 결정하는 **운영체제 관문**입니다.

---

## 6. 핵심 메커니즘

### 1. inode 기반 특수 파일
device file도 inode를 가지지만 일반 파일과 다릅니다.

### 2. major / minor number

| 번호 | 역할 |
|------|------|
| **Major** | 어떤 driver를 사용할지 식별 |
| **Minor** | driver 내부 어느 장치인지 식별 |

```bash
ls -l /dev/sda
# brw-rw---- 8, 0
# 8 = major, 0 = minor
```

### 3. VFS dispatch
`open` / `read` / `write` 호출 시 **VFS → inode → device driver** 경로로 이동.

### 4. file_operations 연결
driver가 `struct file_operations`를 등록하면 VFS가 해당 함수를 호출합니다.

### 5. Character vs Block Device

| 구분 | 특징 | 예시 | 표시 |
|------|------|------|------|
| **Character Device** | 스트림 기반 | keyboard, serial, tty | `c` |
| **Block Device** | 블록 기반 storage | SSD, HDD | `b` |

### 6. udev 동적 생성
장치 연결 시 자동으로 device node가 생성됩니다.

### 7. syscall 흐름

```
Application
    → open("/dev/...")
    → VFS
    → inode
    → major/minor lookup
    → driver dispatch
    → hardware controller
```

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

| 목적 | 명령어 |
|------|--------|
| 장치 파일 확인 | `ls /dev` |
| 상세 정보 | `ls -l /dev/sda` |
| 장치 번호 확인 | `stat /dev/sda` |
| block device 확인 | `lsblk` |
| driver 연결 확인 | `lspci -k` |
| udev 이벤트 확인 | `udevadm monitor` |
| 커널 로그 | `dmesg`, `journalctl -k` |

### Kubernetes

| 목적 | 방법 |
|------|------|
| container device mount 확인 | `kubectl describe pod` |
| GPU device plugin | device node mount 여부 확인 |
| CSI volume | 실제 `/dev/*` node 연결 확인 |

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*