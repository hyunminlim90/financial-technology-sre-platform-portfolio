# Hyper-V FinTech SRE Platform Infrastructure

이 디렉토리는 토스페이먼츠와 유사한 핀테크/SRE 플랫폼을 Hyper-V VM 5대 위에 구축하기 위한 인프라 문서와 설치 스크립트를 관리한다.

## 목표

Hyper-V 기반 로컬 VM 환경에서 다음을 구축한다.

- 핀테크 API 플랫폼
- Kubernetes 기반 애플리케이션 실행 환경
- Istio 기반 트래픽 제어
- ArgoCD 기반 GitOps
- Jenkins 또는 GoCD 기반 CI/CD
- MySQL / Oracle XE / Redis / Kafka / Elasticsearch 데이터 계층
- Prometheus / Grafana / ELK / Alertmanager 관측성 계층
- AI Agent + LLM + RAG 기반 SRE 자동화 계층
- Terraform/OpenTofu 기반 인프라 실행 구조

## VM 구성

| IP | Hostname | Role |
|---|---|---|
| 172.30.1.105 | gateway | Nginx, React, Jenkins/GoCD, Git Webhook |
| 172.30.1.106 | app-node-1 | Kubernetes Worker, Spring Boot WebFlux, Spring Batch |
| 172.30.1.107 | data-node | MySQL, Oracle XE, Redis, Kafka, Elasticsearch |
| 172.30.1.108 | observability-node | Prometheus, Grafana, ELK, Loki, Alertmanager |
| 172.30.1.109 | platform-node | Kubernetes Control Plane, ArgoCD, Istio, SRE Agent, RAG, LLM Gateway, OpenTofu |

## Network

- Hyper-V Switch: ExternalSwitch
- VM Network: 172.30.1.0/24
- Kubernetes Pod CIDR: 192.168.0.0/16
- Kubernetes API Server: 172.30.1.109:6443

## Kubernetes 구성

| Node | 역할 |
|---|---|
| 172.30.1.109 | Control Plane |
| 172.30.1.106 | Worker |
| 172.30.1.107 | Worker |
| 172.30.1.108 | Worker |
| 172.30.1.105 | Gateway / CI / Frontend |

## 구축 순서

1. 모든 VM 공통 패키지 설치
2. 모든 VM Docker 설치
3. 모든 VM containerd 설정
4. 모든 VM Kubernetes 사전 설정
5. kubeadm / kubelet / kubectl 설치
6. platform-node에서 Kubernetes Control Plane 초기화
7. app-node-1, data-node, observability-node를 Worker로 Join
8. CNI 설치
9. Istio 설치
10. ArgoCD 설치
11. data-node에 MySQL / Oracle XE / Redis / Kafka / Elasticsearch 구성
12. app-node에 Spring Boot WebFlux 플랫폼 배포
13. gateway에 React + Nginx 구성
14. observability-node에 Prometheus / Grafana / ELK 구성
15. platform-node에 SRE Agent + RAG + LLM Gateway 구성
16. Jenkins 또는 GoCD 연결
17. Terraform/OpenTofu 기반 인프라 실행 구조 추가

## 이 디렉토리의 역할

`infra/hyperv`는 장애 대응 로직 자체가 아니라, 장애를 발생시키고 관찰하고 복구 자동화를 실험할 수 있는 VM 기반 인프라 환경을 정의한다.

장애 대응 로직은 다음 디렉토리와 연결된다.

- `platform/incident-response`
- `platform/observability`
- `platform/reliability`
- `runbooks/`
- `agent/`
- `rag/`
- `scenarios/`