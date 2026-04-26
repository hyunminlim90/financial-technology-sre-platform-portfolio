# Hyper-V Platform Architecture

## 전체 아키텍처

```text
[Client / User]
      ↓
172.30.1.105 gateway
- Nginx
- React Web Console
- Jenkins or GoCD
- Git Webhook Endpoint
      ↓
Kubernetes Cluster
      |
      +-- 172.30.1.109 platform-node
      |   - Kubernetes Control Plane
      |   - ArgoCD
      |   - Istio
      |   - SRE Agent
      |   - RAG
      |   - LLM Gateway
      |   - Terraform/OpenTofu Runner
      |
      +-- 172.30.1.106 app-node-1
      |   - Spring Boot WebFlux API (+ R2DBC ─ 실시간 결제 요청 / 비동기 API)
      |   - Spring Batch (+ JPA/Hibernate ─ 정산 / 리포트 / 관리자성 작업)
      |   - Kafka Client
      |
      +-- 172.30.1.107 data-node
      |   - MySQL
      |   - Oracle XE
      |   - Redis
      |   - Kafka
      |   - Elasticsearch
      |
      +-- 172.30.1.108 observability-node
          - Prometheus
          - Grafana
          - ELK
          - Loki
          - Alertmanager