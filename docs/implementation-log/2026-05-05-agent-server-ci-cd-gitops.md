# Agent Server CI/CD + GitOps 구축 작업 기록

---

## 1. 목적

agent-server를 포트폴리오 레포의 애플리케이션 소스로 편입하고, Jenkins 기반 CI, Private Docker Registry, GitOps, ArgoCD, Kubernetes, Istio, Cloudflare Tunnel까지 이어지는 end-to-end 배포 흐름을 구축한다.

---

## 2. 최종 배포 흐름

```text
portfolio/apps/agent-server
→ Jenkins CI
→ Gradle test
→ Docker image build
→ Private Registry push
→ GitOps repo image tag update
→ ArgoCD sync
→ Kubernetes rollout
→ Istio Gateway / VirtualService
→ Cloudflare Tunnel
→ External health check
```

---

## 3. 레포 역할

### `fin-tech-sre-platform-portfolio`

**역할:**
- 설계 문서
- SRE 지식 레이어
- agent-server 애플리케이션 소스 코드

**주요 경로:**
```
apps/agent-server
scripts/sync-agent-server.sh
```

### `fin-tech-sre-platform-gitops`

**역할:**
- Kubernetes desired state
- ArgoCD App-of-Apps
- agent-server Deployment / Service / ConfigMap / Istio routing

**주요 경로:**
```
apps/agent-server
bootstrap/apps/agent-server.yaml
```

---

## 4. agent-server 소스 반영

**Codex 작업 디렉토리:**
```
/Users/hyunminlim/Documents/New project/apps/agent-server
```

**portfolio 반영 위치:**
```
fin-tech-sre-platform-portfolio/apps/agent-server
```

**동기화 스크립트:**
```
scripts/sync-agent-server.sh
```

**제외 항목:**
```
.git
build
.gradle
.gradle-home
.idea
.classpath
.project
.settings
*.iml
.DS_Store
```

---

## 5. agent-server 상태

**검증 완료:**
- `./gradlew test`
- `bootRun`
- Actuator health check
- Docker image build
- Kubernetes deployment
- External health check

**외부 health check:**
```bash
curl https://ft-sre-agent.opentofu.click/actuator/health
curl https://ft-sre-agent.opentofu.click/actuator/health/liveness
curl https://ft-sre-agent.opentofu.click/actuator/health/readiness
```

**결과:**
```json
{"groups":["liveness","readiness"],"status":"UP"}
{"status":"UP"}
{"status":"UP"}
```

---

## 6. Jenkins 구축

**Jenkins 설치 위치:**
```
gateway VM
172.30.1.105
```

Amazon Linux 2에서 Jenkins 설치.

**이슈:**
```
Jenkins 2.555.1 requires Java 21
```

**해결:**
- Corretto 21 설치
- Jenkins systemd 재시작

**Jenkins Job:** `agent-server-ci`

**초기 목표:**
```
portfolio repo checkout
→ apps/agent-server
→ ./gradlew clean test
```

---

## 7. Docker 이미지 빌드

**Dockerfile 위치:**
```
apps/agent-server/Dockerfile
```

**베이스 이미지:**
```dockerfile
FROM eclipse-temurin:17-jdk AS build
FROM eclipse-temurin:17-jre
```

**이미지 태그 전략:**
```
ci-${BUILD_NUMBER}
${GIT_SHA}
```

**예시:**
```
172.30.1.105:5000/fin-tech-sre/agent-server:ci-4
172.30.1.105:5000/fin-tech-sre/agent-server:d08b72684dc1
```

---

## 8. Private Docker Registry

**Registry 위치:**
```
gateway VM
172.30.1.105:5000
```

**확인:**
```bash
curl http://localhost:5000/v2/_catalog
curl http://localhost:5000/v2/fin-tech-sre/agent-server/tags/list
```

**검증 결과:**
```json
{"repositories":["fin-tech-sre/agent-server","test/hello-world"]}
{"name":"fin-tech-sre/agent-server","tags":["ci-4","d08b72684dc1"]}
```

**Registry UI:**
```
http://172.30.1.105:5001
```

---

## 9. Docker insecure registry 설정

**Jenkins/gateway VM Docker daemon 설정:**
```json
{
  "insecure-registries": ["172.30.1.105:5000"]
}
```

**확인:**
```bash
docker info | grep -A 10 -i "Insecure Registries"
```

---

## 10. Kubernetes containerd registry 설정

**각 Kubernetes 노드에 적용:**
```
control-plane-1
app-node-1
data-node-1
obs-node-1
```

**`hosts.toml`:**
```toml
server = "http://172.30.1.105:5000"

[host."http://172.30.1.105:5000"]
  capabilities = ["pull", "resolve"]
```

**containerd 2.x 계열 설정:**
```toml
[plugins.'io.containerd.cri.v1.images'.registry]
  config_path = '/etc/containerd/certs.d'
```

**검증:**
```bash
crictl pull 172.30.1.105:5000/fin-tech-sre/agent-server:ci-4
```

---

## 11. Kubernetes pull 검증

**테스트 Pod:**
```bash
kubectl run test-registry \
  --image=172.30.1.105:5000/fin-tech-sre/agent-server:ci-4 \
  --restart=Never
```

**결과:** `Running`

---

## 12. GitOps 배포

GitOps repo에 agent-server Application 추가.

**주요 리소스:**
- Namespace
- ConfigMap
- Deployment
- Service
- Gateway
- VirtualService
- Kustomization
- ArgoCD Application

**ArgoCD 확인:**
```bash
kubectl -n argocd get application agent-server
```

**결과:**
```
agent-server   Synced   Healthy
```

**Deployment 확인:**
```bash
kubectl -n sre-agent get all
kubectl -n sre-agent rollout status deploy/agent-server
```

---

## 13. Istio / Cloudflare 외부 노출

**외부 도메인:**
```
ft-sre-agent.opentofu.click
```

**라우팅 흐름:**
```
Cloudflare
→ cloudflared
→ 172.30.1.109:30660
→ Istio IngressGateway
→ Gateway
→ VirtualService
→ agent-server Service
→ Pod
```

**검증:**
```bash
curl https://ft-sre-agent.opentofu.click/actuator/health
```

**결과:**
```json
{"groups":["liveness","readiness"],"status":"UP"}
```

---

## 14. Jenkins → GitOps 자동 업데이트

Jenkins가 GitOps repo를 clone하고 image tag를 갱신한다.

**핵심 명령:**
```bash
kustomize edit set image \
  172.30.1.105:5000/fin-tech-sre/agent-server=172.30.1.105:5000/fin-tech-sre/agent-server:${GIT_SHA}
```

**이후:**
```bash
git add apps/agent-server/kustomization.yaml
git commit -m "deploy(agent-server): ${GIT_SHA}"
git push origin main
```

---

## 15. SSH 권한 이슈

**문제:**
```
Host key verification failed
Permission denied (publickey)
Load key "/var/lib/jenkins/.ssh/id_ed25519": Permission denied
```

**원인:**
- Jenkins build는 `jenkins` user로 실행
- SSH key/known_hosts가 `ec2-user` 기준으로 잡혀 있었음
- private key owner가 `root`였음

**해결:**
```bash
sudo chown -R jenkins:jenkins /var/lib/jenkins/.ssh
sudo chmod 700 /var/lib/jenkins/.ssh
sudo chmod 600 /var/lib/jenkins/.ssh/id_ed25519
sudo chmod 644 /var/lib/jenkins/.ssh/id_ed25519.pub
sudo chmod 600 /var/lib/jenkins/.ssh/known_hosts
```

**검증:**
```bash
sudo -u jenkins ssh -i /var/lib/jenkins/.ssh/id_ed25519 -T git@github.com
```

---

## 16. 주요 트러블슈팅

### 16.1 Jenkins Java 버전

**문제:**
```
Java 17 is older than minimum required version
```

**해결:**
- Jenkins용 Java 21 설치
- agent-server 컨테이너 Java 17과 분리해서 판단

### 16.2 Docker push 실패

**문제:**
```
http: server gave HTTP response to HTTPS client
```

**해결:**
- Docker daemon insecure registry 설정

### 16.3 Kubernetes ImagePullBackOff

**문제:**
```
ErrImagePull
ImagePullBackOff
```

**원인:** containerd가 HTTP registry를 HTTPS로 접근

**해결:**
- `certs.d hosts.toml`
- containerd `config_path` 설정

### 16.4 Registry UI Mixed Content

**원인:** HTTPS UI에서 HTTP registry API 직접 호출

**해결:** Registry UI proxy 모드 사용

### 16.5 Logback pattern 오류

**문제:**
```
[notEmpty] is not a valid conversion word
```

**해결:** Spring Boot 4 / Logback 호환 `key=value` logging pattern으로 수정

---

## 17. 최종 결과

**완성된 흐름:**
```
portfolio push
→ Jenkins test
→ Docker build
→ Private registry push
→ GitOps repo image tag commit/push
→ ArgoCD sync
→ Kubernetes rollout
→ External health check UP
```

**최종 상태:**

| 항목 | 상태 |
|---|---|
| Jenkins | SUCCESS |
| Registry | image pushed |
| ArgoCD | Synced / Healthy |
| Kubernetes | rollout successful |
| External health | UP |

---

## 18. 후속 작업

- [ ] Jenkins Freestyle → Jenkinsfile 전환
- [ ] GitOps 직접 push → PR 기반 배포 승인 전환
- [ ] actuator 외부 노출 제한
- [ ] Prometheus scrape 연결
- [ ] rollback 절차 문서화
- [ ] Harbor 도입 검토
- [ ] image retention 정책
- [ ] Argo Rollouts 기반 canary/blue-green 검토