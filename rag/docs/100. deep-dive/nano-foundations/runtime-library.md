# 런타임 라이브러리 (Runtime Library)

> 정독: 0회

## 1. 이 기술이 무엇인가

런타임 라이브러리는:

> 프로그램 실행 시 자주 사용하는 공통 기능들을 미리 구현해 둔 **실행 지원 라이브러리**

**대표 기능:** 메모리 할당, 문자열 처리, 파일 입출력, 네트워크 I/O, 수학 연산, 스레드 제어, 예외 처리, 시스템 호출 래핑

프로그램은 이 기능들을 직접 구현하지 않고 런타임 라이브러리를 호출하여 사용합니다.

**핵심:** 표준 기능 재사용, 운영체제 인터페이스 추상화, 실행 환경 지원

---

## 2. 시스템 어디에서 등장하는가

런타임 라이브러리는 거의 모든 프로그램 실행 과정에 개입합니다.

**등장 위치:** 사용자 애플리케이션, 시스템 데몬, 웹 서버, DBMS, 컨테이너 런타임, Kubernetes 노드 컴포넌트, Terraform/OpenTofu 실행 바이너리, CLI 툴, 컴파일러 런타임

**대표 파일 형태:**

| OS | 형태 |
|----|------|
| Linux | .so |
| Windows | .dll |
| macOS | .dylib |

**예시:** `libc.so`, `libpthread.so`, `libm.so`

---

## 3. 어떤 자원에 가장 영향이 큰가

런타임 라이브러리는 시스템 거의 모든 자원과 연결됩니다.

| 자원 | 영향 |
|------|------|
| Memory | malloc/free, heap 관리 |
| CPU | 함수 호출 및 런타임 처리 |
| Disk | 파일 I/O |
| Network | socket API |
| Scheduler | thread API |

특히 영향이 큰 영역은 메모리 관리, 시스템 호출, 동기화 처리, I/O 처리입니다.

---

## 4. 왜 중요한가

런타임 라이브러리는 **실행 환경의 기반 계층**입니다.

- 프로그램 코드 중복 제거
- OS 차이 추상화
- 검증된 기능 재사용
- 성능 최적화 집중
- 시스템 인터페이스 표준화

애플리케이션 대부분은 직접 커널과 대화하지 않고 런타임 라이브러리를 통해 시스템 기능을 사용합니다.

```
Application
 → Runtime Library
   → System Call
     → Kernel
```

---

## 5. 실제 장애와 어떤 관련이 있는가

### Shared Library Missing

필수 라이브러리 누락 시 `error while loading shared libraries` 오류가 발생합니다.

```bash
ldd <binary>
```

### ABI Compatibility 문제

라이브러리 버전 충돌 시 `undefined symbol` 오류가 발생합니다. glibc 버전 불일치나 binary compatibility가 깨진 경우 발생합니다.

### Memory Corruption

런타임 메모리 함수 오용으로 double free, invalid free, heap corruption이 발생할 수 있습니다.

```bash
valgrind
asan
gdb
```

### Thread Synchronization Failure

런타임 동기화 함수 문제로 deadlock, race condition이 발생할 수 있습니다.

### Dynamic Linking Failure

동적 링크 실패 시 `cannot open shared object file` 오류가 발생합니다.

```bash
ldd
strace
```

---

## 6. 핵심 메커니즘

### 1) 실행 파일 생성 시 참조 정보 기록

컴파일/링크 시 실제 라이브러리 코드 전체를 삽입하지 않고, 필요한 라이브러리 이름만 기록합니다. (예: `libc.so`, `libpthread.so`)

### 2) 프로세스 시작 시 동적 로더 실행

프로세스 실행 시 동적 링커(loader)인 `ld-linux.so`가 개입하여 필요 라이브러리를 메모리에 매핑합니다.

### 3) 가상 메모리 매핑

라이브러리 코드가 프로세스 주소 공간에 연결됩니다. 여러 프로세스가 공유 가능하여 실제 물리 메모리 중복이 감소합니다.

### 4) 함수 호출 시 라이브러리 코드 실행

```
프로그램: printf() / malloc() / socket() 호출
       ↓
런타임 라이브러리 함수 실행
       ↓
System Call 진입 가능
```

### 5) 커널 시스템 호출 연결

일부 함수는 결국 `read()`, `write()`, `mmap()`, `fork()` 같은 시스템 호출로 연결됩니다.

```
Application
 → Runtime Library
   → Kernel
     → Hardware
```

---

## 7. Linux/Runtime/K8s에서 관측 방법

### Linux

```bash
# 실행 바이너리 의존 라이브러리
ldd <binary>

# 메모리 매핑 확인
cat /proc/<PID>/maps
# 예: /lib/x86_64-linux-gnu/libc.so

# 동적 링크 디버깅
strace
ltrace

# 열린 라이브러리 확인
lsof -p <PID>

# ELF 정보 확인
readelf -d <binary>
objdump -p <binary>
```

### Kubernetes / Container

```bash
# 컨테이너 내부 라이브러리 확인
kubectl exec -it <pod> -- ldd <binary>
```

이미지 크기 최적화 시 Alpine(musl), glibc, static binary 간 차이가 발생합니다.

### Runtime Environment

런타임 로딩 실패 시 `CrashLoopBackOff`가 발생할 수 있으며, shared library missing이나 incompatible libc가 원인 중 하나일 수 있습니다.

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*