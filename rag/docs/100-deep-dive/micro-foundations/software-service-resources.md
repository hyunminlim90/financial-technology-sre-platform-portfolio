# 소프트웨어 서비스 자원 (Software Service Resources / SaaS)

> 정독: 0회

## 1. 이 기술이 무엇인가

소프트웨어 서비스 자원(SaaS)은:

> 클라우드 인프라와 플랫폼 위에서 완전히 실행 중인 애플리케이션 기능 자체를 인터넷을 통해 서비스 형태로 제공하는 최상위 소프트웨어 자원

사용자는 서버 구축, 운영체제 관리, 런타임 설치, 데이터베이스 운영, 패치, 배포를 직접 수행하지 않습니다.

### 핵심 특징

사용자는 **애플리케이션 기능만 소비**합니다.

제공자는 인프라, 플랫폼, 애플리케이션, 운영, 보안, 확장성 전체를 관리합니다.

---

## 2. 시스템 어디에서 등장하는가

현대 인터넷 서비스 대부분에서 등장합니다.

### 협업 서비스

문서 편집, 메신저, 화상회의

### 기업 업무 시스템

ERP, CRM, HR system

### 개발 플랫폼

Git hosting, CI/CD SaaS

### AI 서비스

hosted AI assistant, inference API platform

### 금융 서비스

payment SaaS, billing platform

### 데이터 분석 플랫폼

BI dashboard, analytics platform

### 보안 플랫폼

identity platform, zero trust service

---

## 3. 어떤 자원(CPU/Memory/Network/Disk)에 가장 영향이 큰가

SaaS는 전체 자원을 통합적으로 사용합니다.

| 자원 | 영향 | 주요 지표 |
|---|---|---|
| **Network** | 매우 중요 | global access, API traffic, realtime collaboration |
| **CPU** | 중요 | request processing, application logic, analytics |
| **Memory** | 중요 | session state, cache, runtime execution |
| **Disk** | 매우 중요 | user data, backups, logs, persistence |

> SaaS는 단일 리소스가 아니라 클라우드 전체 스택(IaaS/PaaS)을 최종 사용자 기능으로 통합한 계층입니다.

---

## 4. 왜 중요한가

현대 인터넷 서비스 핵심 전달 모델입니다.

### 설치 제거

브라우저/API 기반 즉시 사용 가능.

### 운영 부담 제거

사용자 측 인프라 운영 불필요.

### 중앙 집중 업데이트

전체 사용자 동시 업데이트 가능.

### 글로벌 확장 가능

인터넷 기반 전 세계 제공 가능.

### 비용 효율성

구독형 운영 가능.

### 멀티테넌시 기반 대규모 운영

수백만 사용자 수용 가능.

### 지속적 배포 가능

기능 개선 속도 향상.

---

## 5. 실제 장애와 어떤 관련이 있는가

현대 대규모 인터넷 장애 대부분이 **SaaS 계층**과 연결됩니다.

### Authentication Failure

로그인 시스템 장애. → 전체 서비스 접근 불가

### Multi-Tenant Isolation Failure

테넌트 격리 실패. → 데이터 노출, 보안 사고

### Database Saturation

DB 과부하. → global latency increase

### Regional Outage

특정 리전 장애. → 서비스 일부 중단

### Deployment Failure

잘못된 배포. → application crash, rollback 발생

### Dependency Failure

외부 서비스 장애. → cascading failure

### API Rate Limit Failure

과도한 요청 처리 실패. → throttling, request rejection

### CDN / DNS Failure

네트워크 진입 장애. → global access failure

---

## 6. 현재 문맥에서 꼭 알아야 하는 핵심 메커니즘만 설명

### ① Multi-Tenancy

다수 사용자 논리적 격리 운영.

### ② Centralized Application Model

중앙 애플리케이션 공용 실행.

### ③ Continuous Deployment

무중단 업데이트 및 기능 배포.

### ④ Identity & Access Control

사용자 인증·권한 관리.

### ⑤ Elastic Scaling

트래픽 기반 자동 확장.

### ⑥ Service API Exposure

기능 API 기반 공개.

### ⑦ Shared Infrastructure

공용 플랫폼 및 인프라 활용.

### ⑧ Observability

서비스 상태 중앙 관측.

### ⑨ Resiliency Architecture

복제·자동복구 기반 운영.

### ⑩ Global Distribution

다중 리전 기반 글로벌 서비스.

---

## 7. Linux / Runtime / K8s에서 어디서 관측되는가

### Linux

**프로세스 상태**

```bash
ps -ef
top
```

**네트워크 상태**

```bash
ss -ant
ip addr
```

**디스크 상태**

```bash
df -h
iostat
```

**로그 확인**

```bash
journalctl
tail -f
```

### Kubernetes

**Pod 상태**

```bash
kubectl get pods
```

**서비스 상태**

```bash
kubectl get svc
```

**Ingress 상태**

```bash
kubectl get ingress
```

**Autoscaling 상태**

```bash
kubectl get hpa
```

**이벤트 확인**

```bash
kubectl get events
```

**로그 확인**

```bash
kubectl logs
```

### SaaS 운영 관측 핵심 지표

관측 대상: request latency, error rate, tenant isolation, API throughput, active sessions, deployment health, replication lag

</br>

*이 문서는 SRE 팀의 Base Knowledge로 관리됩니다. 내용 수정 시 SRE 채널에 변경 사항을 공유해주세요.*