```md
# Hyper-V Network Design

## Switch

- Hyper-V Virtual Switch: ExternalSwitch
- 목적: VM들이 외부 네트워크 및 로컬 호스트와 통신 가능하도록 구성

## IP Allocation

| IP | Hostname | Role |
|---|---|---|
| 172.30.1.105 | gateway | External Entry / Frontend / CI |
| 172.30.1.106 | app-node-1 | Application Worker Node |
| 172.30.1.107 | data-node | Data Worker Node |
| 172.30.1.108 | observability-node | Observability Worker Node |
| 172.30.1.109 | platform-node | Kubernetes Control Plane |

## Kubernetes Network

| 항목 | 값 |
|---|---|
| API Server | 172.30.1.109:6443 |
| Pod CIDR | 192.168.0.0/16 |
| Service CIDR | Kubernetes 기본값 |
| CNI | Calico 또는 대체 CNI |

## 통신 방향

```text
Client
  -> gateway:80/443
  -> Kubernetes Service / Ingress
  -> app-node workload
  -> data-node services
  -> observability-node metrics/logs
  -> platform-node SRE Agent