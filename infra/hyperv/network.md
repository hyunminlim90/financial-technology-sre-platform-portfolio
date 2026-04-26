# Hyper-V Network Design

---

## 🔌 Switch

- **Hyper-V Virtual Switch**: `ExternalSwitch`
- **목적**
  - VM이 외부 네트워크와 통신 가능
  - 로컬 호스트와 통신 가능

---

## 🧩 IP Allocation

| IP | Hostname | Role |
|---|---|---|
| 172.30.1.105 | gateway | External Entry / Frontend / CI |
| 172.30.1.106 | app-node-1 | Application Worker Node |
| 172.30.1.107 | data-node | Data Worker Node |
| 172.30.1.108 | observability-node | Observability Worker Node |
| 172.30.1.109 | platform-node | Kubernetes Control Plane |

---

## ☸️ Kubernetes Network

| 항목 | 값 |
|---|---|
| API Server | `172.30.1.109:6443` |
| Pod CIDR | `192.168.0.0/16` |
| Service CIDR | Kubernetes 기본값 |
| CNI | Calico (또는 대체 CNI) |

---

## 🔄 통신 흐름

```text
Client
  → gateway (80 / 443)
  → Kubernetes Ingress / Service
  → app-node workload
  → data-node services
  → observability-node (metrics / logs)
  → platform-node (SRE Agent)
```

---

## 🚪 주요 포트 정의

### 🔐 기본 인프라

| Port | 용도 |
|---|---|
| 22 | SSH |
| 80 | HTTP |
| 443 | HTTPS |

---

### ☸️ Kubernetes

| Port | 용도 |
|---|---|
| 6443 | Kubernetes API Server |
| 2379-2380 | etcd |
| 10250 | kubelet |
| 30000-32767 | NodePort |

---

### 🗄️ 데이터 및 메시징

| Port | 용도 |
|---|---|
| 3306 | MySQL |
| 1521 | Oracle XE |
| 6379 | Redis |
| 9092 | Kafka |

---

### 📊 Observability

| Port | 용도 |
|---|---|
| 9200 | Elasticsearch |
| 9090 | Prometheus |
| 3000 | Grafana |
| 5601 | Kibana |

---

### ⚙️ 애플리케이션 및 CI/CD

| Port | 용도 |
|---|---|
| 8080 | Jenkins / Application |