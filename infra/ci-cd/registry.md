# Private Docker Registry - Operational Guide

이 문서는 FinTech SRE Platform에서 사용하는 Private Docker Registry의 구성, 동작 방식, Kubernetes 연동, 장애 대응을 정의합니다.

---

## 1. Overview

Registry는 CI/CD 파이프라인에서 생성된 Docker 이미지를 저장하고, Kubernetes에서 해당 이미지를 pull하기 위한 핵심 컴포넌트입니다.

**구성:**
```text
Jenkins → Docker Build → Registry Push
Kubernetes → Registry Pull
```

---

## 2. Runtime Environment

```text
Host: 172.30.1.105
Port: 5000
Protocol: HTTP (insecure)
Image: registry:2
```

---

## 3. Registry 구조

**이미지 경로:**
```
172.30.1.105:5000/fin-tech-sre/agent-server:<tag>
```

**예:**
```
172.30.1.105:5000/fin-tech-sre/agent-server:ci-4
172.30.1.105:5000/fin-tech-sre/agent-server:d08b72684dc1
```

---

## 4. API 확인

**전체 repository:**
```bash
curl http://172.30.1.105:5000/v2/_catalog
```

**tag 목록:**
```bash
curl http://172.30.1.105:5000/v2/fin-tech-sre/agent-server/tags/list
```

---

## 5. Docker Push 설정 (Jenkins / Gateway)

**Docker daemon 설정:**
```json
{
  "insecure-registries": ["172.30.1.105:5000"]
}
```

**적용:**
```bash
sudo systemctl restart docker
```

**확인:**
```bash
docker info | grep -A 10 "Insecure Registries"
```

---

## 6. Kubernetes Image Pull (핵심)

Docker 설정과 Kubernetes는 별개입니다.

```text
Docker OK ≠ Kubernetes OK
```

> Kubernetes는 containerd를 사용합니다.

---

## 7. containerd 설정 (Critical)

### 7.1 config.toml

```toml
[plugins.'io.containerd.cri.v1.images'.registry]
  config_path = '/etc/containerd/certs.d'
```

### 7.2 hosts.toml

**경로:**
```
/etc/containerd/certs.d/172.30.1.105:5000/hosts.toml
```

**내용:**
```toml
server = "http://172.30.1.105:5000"

[host."http://172.30.1.105:5000"]
  capabilities = ["pull", "resolve"]
```

### 7.3 적용

```bash
sudo systemctl restart containerd
sudo systemctl restart kubelet
```

### 7.4 검증

```bash
crictl pull 172.30.1.105:5000/fin-tech-sre/agent-server:ci-4
```

정상 응답:
```
Image is up to date
```

---

## 8. Kubernetes Pull 검증

```bash
kubectl run test-registry \
  --image=172.30.1.105:5000/fin-tech-sre/agent-server:ci-4 \
  --restart=Never
```

---

## 9. Failure Modes (핵심)

### 9.1 ImagePullBackOff

**증상:**
```
ErrImagePull
ImagePullBackOff
```

**원인:**
```text
containerd가 HTTP registry를 HTTPS로 접근
```

**에러:**
```
http: server gave HTTP response to HTTPS client
```

**해결:**
```text
hosts.toml 설정
config_path 활성화
containerd 재시작
```

### 9.2 Docker Push 실패

**증상:**
```
http: server gave HTTP response to HTTPS client
```

**원인:**
```text
docker daemon HTTPS 시도
```

**해결:**
```text
insecure registry 설정
```

### 9.3 특정 노드만 Pull 실패

**증상:**
```
한 노드만 ImagePullBackOff
```

**원인:**
```text
해당 node에 containerd 설정 없음
```

**해결:**
```text
모든 worker node에 동일 설정 적용
```

### 9.4 Registry 접근 불가

**체크:**
```bash
curl http://172.30.1.105:5000/v2/_catalog
```

### 9.5 Mixed Content (UI)

**증상:**
```
HTTPS UI에서 registry API 호출 실패
```

**원인:**
```text
HTTP registry + HTTPS UI
```

---

## 10. Operational Commands

| 목적 | 명령 |
|---|---|
| registry 확인 | `curl http://172.30.1.105:5000/v2/_catalog` |
| image 확인 | `curl http://172.30.1.105:5000/v2/fin-tech-sre/agent-server/tags/list` |
| node pull 테스트 | `crictl pull <image>` |
| pod 테스트 | `kubectl run test --image=<image> --restart=Never` |

---

## 11. System Boundaries

**Registry는 다음만 수행합니다:**
```text
image 저장
image 제공
```

**하지 않는 것:**
```text
인증 관리 (현재 없음)
보안 검사
access control
```

---

## 12. Security Considerations

**현재 리스크:**
```text
HTTP insecure registry
authentication 없음
encryption 없음
```

---

## 13. Next Improvements

- [ ] HTTPS registry 전환
- [ ] Harbor 도입
- [ ] image vulnerability scan
- [ ] RBAC
- [ ] image retention policy

---

## 14. Critical Rules

```text
모든 node에 containerd 설정 필수
Docker 설정 ≠ Kubernetes 설정
ImagePull 문제는 node 기준으로 확인
```