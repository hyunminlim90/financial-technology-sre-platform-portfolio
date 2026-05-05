# Jenkins (CI) - Operational Guide

이 문서는 FinTech SRE Platform의 Jenkins CI 구성, 동작 방식, 운영 절차, 장애 대응을 정의합니다.

---

## 1. Overview

Jenkins는 CI(Continuous Integration) 역할만 수행합니다.

**역할:**
```text
✔ 코드 검증 (test)
✔ Docker image build
✔ Private registry push
✔ GitOps repository 업데이트
```

**금지:**
```text
✖ Kubernetes 직접 배포 (kubectl)
✖ rollout 실행
✖ cluster 상태 변경
```

---

## 2. Runtime Environment

```text
Host: 172.30.1.105
OS: Amazon Linux 2
User: jenkins
```

**설치 요구사항:**
```text
Java 21 (Jenkins 요구사항)
Docker
Git
```

---

## 3. Directory Structure

```
/var/lib/jenkins/
├── workspace/
├── .ssh/
│    ├── id_ed25519
│    ├── id_ed25519.pub
│    └── known_hosts
```

---

## 4. CI Pipeline Flow

```text
portfolio/apps/agent-server
→ Jenkins Job
→ ./gradlew clean test
→ docker build
→ docker push
→ GitOps repo clone
→ kustomize image update
→ git commit
→ git push
→ ArgoCD sync
```

---

## 5. Image Tag Strategy

```text
ci-<build-number>
<git-sha>
```

**운영 규칙:**
```text
배포는 반드시 git-sha 기반 tag 사용
ci tag는 trace/debug 용도
```

---

## 6. GitOps Update

**핵심 명령:**
```bash
kustomize edit set image \
  172.30.1.105:5000/fin-tech-sre/agent-server=172.30.1.105:5000/fin-tech-sre/agent-server:${GIT_SHA}
```

```bash
git add apps/agent-server/kustomization.yaml
git commit -m "deploy(agent-server): ${GIT_SHA}"
git push origin main
```

---

## 7. SSH Configuration (Critical)

Jenkins는 반드시 `jenkins` user 기준으로 GitHub 접근해야 합니다.

### 7.1 Key 위치
```
/var/lib/jenkins/.ssh/id_ed25519
```

### 7.2 권한 설정
```bash
sudo chown -R jenkins:jenkins /var/lib/jenkins/.ssh
sudo chmod 700 /var/lib/jenkins/.ssh
sudo chmod 600 /var/lib/jenkins/.ssh/id_ed25519
sudo chmod 644 /var/lib/jenkins/.ssh/id_ed25519.pub
sudo chmod 600 /var/lib/jenkins/.ssh/known_hosts
```

### 7.3 known_hosts
```bash
ssh-keyscan github.com >> /var/lib/jenkins/.ssh/known_hosts
```

### 7.4 검증
```bash
sudo -u jenkins ssh -i /var/lib/jenkins/.ssh/id_ed25519 -T git@github.com
```

정상 응답:
```
successfully authenticated
```

---

## 8. Docker Configuration

```json
{
  "insecure-registries": ["172.30.1.105:5000"]
}
```

**확인:**
```bash
docker info | grep -A 10 "Insecure Registries"
```

---

## 9. Failure Modes

### 9.1 Permission denied (publickey)

**증상:**
```
git push 실패
Permission denied (publickey)
```

**원인:**
```text
jenkins user key 없음
권한 root
known_hosts 없음
```

**해결:**
```text
jenkins user 기준 ssh 설정
```

### 9.2 Host key verification failed

**원인:**
```text
known_hosts 없음
```

**해결:**
```bash
ssh-keyscan github.com >> /var/lib/jenkins/.ssh/known_hosts
```

### 9.3 Docker push 실패

**증상:**
```
http: server gave HTTP response to HTTPS client
```

**원인:**
```text
registry HTTP
docker HTTPS 시도
```

**해결:**
```text
insecure registry 설정
```

### 9.4 Gradle 실행 실패

**원인:**
```text
경로 문제
gradlew 권한 없음
```

**해결:**
```bash
chmod +x ./gradlew
cd apps/agent-server
```

---

## 10. Operational Commands

**Jenkins 상태:**
```bash
systemctl status jenkins
```

**Jenkins 재시작:**
```bash
sudo systemctl restart jenkins
```

**로그 확인:**
```bash
journalctl -u jenkins -f
```

**Docker 상태:**
```bash
docker ps
docker images
```

---

## 11. Security Considerations

**현재 리스크:**
```text
✔ SSH key 파일 기반 관리
✔ HTTP registry 사용
✔ Jenkins 단일 노드
✔ GitOps direct push
```

---

## 12. System Boundaries

**Jenkins는 다음을 하지 않습니다:**
```text
Kubernetes 리소스 변경
external system mutation
direct deployment
```

**Jenkins는 다음만 수행합니다:**
```text
build
test
image push
gitops update
```

---

## 13. Next Improvements

- [ ] Jenkinsfile (Pipeline as Code)
- [ ] GitOps PR 기반 승인 구조
- [ ] Credentials Store 사용
- [ ] Webhook 기반 자동 트리거
- [ ] retry / timeout 정책
- [ ] parallel build

---

## 14. Critical Rules

```text
Jenkins에서 kubectl 실행 금지
GitOps repo 직접 수정 금지
main branch 직접 push 금지 (향후 PR 전환)
manual deployment 금지
```