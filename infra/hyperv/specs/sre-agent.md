# platform-node

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.109 |
| Hostname | platform-node |
| OS | Amazon Linux 계열 |
| Network | ExternalSwitch |
| Kubernetes Role | Control Plane |

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

## Kubernetes 초기화 기준

```bash
sudo kubeadm init \
  --apiserver-advertise-address=172.30.1.109 \
  --pod-network-cidr=192.168.0.0/16 \
  --cri-socket=unix:///run/containerd/containerd.sock
```

## kubeconfig 설정

이 설정은 **Control Plane 노드인 platform-node에서 수행한다.**

```bash
mkdir -p $HOME/.kube

sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config

sudo chown $(id -u):$(id -g) $HOME/.kube/config
```