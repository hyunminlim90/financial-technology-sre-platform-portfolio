# platform-node (control-plane-1)

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.109 |
| Hostname | control-plane-1 |
| OS | Amazon Linux 2 |
| Network | ExternalSwitch |
| Kubernetes Role | Control Plane |
| Node Label | node-role=platform |

## 역할

platform-node는 Kubernetes Control Plane이자 SRE 자동화 플랫폼의 중심 노드이다.

## 구성 요소

- Kubernetes Control Plane
- ArgoCD
- Istio
- SRE Agent
- RAG
- LLM Gateway
- Terraform/OpenTofu Runner

## 책임

- Kubernetes API Server 제공
- Cluster 상태 관리
- GitOps 배포 관리
- Service Mesh 구성
- AI Agent 기반 장애 분석
- RAG 기반 Runbook 검색
- Terraform/OpenTofu 실행
- 자동 복구 또는 롤백 워크플로우 실행

## SRE 관점 의미

platform-node는 운영 자동화의 두뇌 역할을 한다.

주요 기능:

- Alert 수신
- 메트릭/로그/트레이스 분석 요청
- Runbook 검색
- 장애 원인 후보 추론
- 대응 계획 생성
- Guardrail 검증
- 승인 기반 실행
- 롤백 수행

## 구축 이력

### 2026-04-26

**공통 패키지 및 Docker 설치**

bootstrap.sh 실행. Docker CE CentOS repo가 Amazon Linux 2 환경에서 404 오류 발생.
`amazon-linux-extras install docker` 방식으로 해결 완료.

```bash
sudo amazon-linux-extras install docker -y
sudo systemctl enable docker
sudo systemctl start docker
```

**containerd 활성화 확인**

```
● containerd.service - containerd container runtime
   Active: active (running) since 일 2026-04-26 10:05:52 KST
   Main PID: 32036 (containerd)
   containerd successfully booted in 0.012175s
```

**kubeadm init 실행**

```bash
sudo kubeadm init \
  --apiserver-advertise-address=172.30.1.109 \
  --pod-network-cidr=192.168.0.0/16 \
  --cri-socket=unix:///run/containerd/containerd.sock
```

- Kubernetes 버전: v1.30.14 (remote v1.36.0, stable-1.30으로 fallback)

**kubeconfig 설정**

Control Plane 노드에서만 수행.

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

**Calico CNI 설치 (tigera-operator 방식 오류 → 해결 완료)**

tigera-operator.yaml 적용 중 CRD annotation size 초과 오류 발생:

```
The CustomResourceDefinition "installations.operator.tigera.io" is invalid:
metadata.annotations: Too long: must have at most 262144 bytes
```

tigera-operator 전체 삭제 및 CRD 잔여 리소스 정리 후 재설치하여 해결 완료.

**Worker Node Join 완료**

app-node-1, data-node-1, obs-node-1 세 노드 join 성공.

**4노드 Ready 확인 및 노드 라벨 적용 완료**

```
NAME              STATUS   ROLES           AGE     VERSION    LABELS(요약)
app-node-1        Ready    <none>          6m46s   v1.30.14   node-role=app
control-plane-1   Ready    control-plane   15m     v1.30.14   node-role=platform
data-node-1       Ready    <none>          6m41s   v1.30.14   node-role=data
obs-node-1        Ready    <none>          6m37s   v1.30.14   node-role=observability
```

## Kubernetes 초기화 기준

```bash
sudo kubeadm init \
  --apiserver-advertise-address=172.30.1.109 \
  --pod-network-cidr=192.168.0.0/16 \
  --cri-socket=unix:///run/containerd/containerd.sock
```

## kubeconfig 설정

이 설정은 Control Plane 노드인 control-plane-1에서만 수행한다.

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

## 모든 노드에서 영구 적용 검증 단계
sudo systemctl is-enabled kubelet <br/>
sudo systemctl is-enabled containerd <br/>
sudo systemctl is-enabled docker <br/>

## 재부팅 후 control-plane-1에서
kubectl get nodes

```
VM 재시작 및 중지 후 재시작 시 영구 적용 요청에 따라
swap 비활성화(/etc/fstab), 커널 모듈(/etc/modules-load.d/k8s.conf),
sysctl(/etc/sysctl.d/k8s.conf), containerd/docker systemctl enable
모두 영구 적용 설정 완료 확인.
```

## 추후 구성

- ArgoCD 설치
- Istio 설치
- SRE Agent 배포
- RAG / LLM Gateway 구성
- Terraform/OpenTofu Runner 구성