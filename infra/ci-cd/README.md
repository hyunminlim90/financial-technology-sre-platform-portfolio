# CI/CD Infrastructure

이 디렉토리는 FinTech SRE Platform의 CI/CD 인프라 레이어를 정의합니다.

---

## 1. Overview

현재 CI/CD는 다음 구성으로 이루어집니다:

```text
Jenkins (CI)
→ Docker Image Build
→ Private Registry
→ GitOps Repository Update
→ ArgoCD (CD)
→ Kubernetes Deployment
```

---

## 2. Design Principles

CI/CD는 다음 원칙을 반드시 따릅니다:

```text
CI는 빌드와 검증만 수행한다
CD는 GitOps를 통해서만 수행된다
Jenkins는 Kubernetes에 직접 배포하지 않는다
Git repository가 single source of truth이다
모든 배포는 추적 가능해야 한다
```

---

## 3. Components

### 3.1 Jenkins (CI)

역할:
- 소스 코드 검증 (test)
- Docker image build
- Private registry push
- GitOps repository 업데이트

실행 환경:
```text
VM: 172.30.1.105
OS: Amazon Linux 2
```

---

### 3.2 Private Registry

역할:
- Docker image 저장

현재 상태:
```text
172.30.1.105:5000
HTTP 기반 (insecure)
```

> **주의:** containerd 설정 필요 (HTTPS 기본)

---

### 3.3 GitOps Repository

역할:
- Kubernetes desired state 관리

내용:
- Deployment
- Service
- Gateway
- VirtualService

---

### 3.4 ArgoCD

역할:
- Git 상태 → Kubernetes sync

---

## 4. CI Pipeline Flow

```text
portfolio/apps/agent-server
→ Jenkins Job
→ Gradle Test
→ Docker Build
→ Docker Push
→ GitOps repo image tag update
→ Git commit / push
→ ArgoCD sync
→ Kubernetes rollout
```

---

## 5. Image Tag Strategy

```text
ci-<build number>
<git sha>
```

> **운영 기준:** 배포는 반드시 `git sha` tag 사용

---

## 6. Critical Rules

**절대 금지:**

```text
Jenkins에서 kubectl 실행
Jenkins에서 직접 배포
GitOps repo 외부 변경
manual deployment
```

---

## 7. Failure Modes

### 7.1 Docker Push 실패

**원인:**
```text
HTTP registry + HTTPS client
```

**해결:**
```text
Docker insecure registry 설정
```

---

### 7.2 Kubernetes ImagePullBackOff

**원인:**
```text
containerd HTTPS 강제
```

**해결:**
```text
hosts.toml 설정
config_path 설정
```

---

### 7.3 Jenkins Git 실패

**원인:**
```text
SSH key 문제
known_hosts 없음
```

---

## 8. Operational Entry Points

```bash
docker info | grep -i insecure
curl http://172.30.1.105:5000/v2/_catalog
kubectl get pods -A
kubectl describe pod
```

---

## 9. Security Considerations

**현재 리스크:**

```text
HTTP registry 사용
Jenkins 단일 노드
GitOps direct push
```

---

## 10. Next Improvements

- [ ] Jenkinsfile 기반 pipeline 전환
- [ ] GitOps PR 기반 승인 구조
- [ ] HTTPS registry 전환
- [ ] Harbor 도입
- [ ] Secret 관리 개선