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
```

## 플랫폼 목적

이 플랫폼은 단순한 애플리케이션 서버가 아니라, 장애 감지부터 분석, 대응, 롤백, 문서화, AI Agent 자동화까지 포함하는 SRE 실습 플랫폼이다.

## 주요 흐름

```text
사용자 요청
  -> Gateway
  -> Spring Boot WebFlux API
  -> Queue / Kafka
  -> Worker / Batch
  -> Data Layer
  -> Observability
  -> Alert
  -> SRE Agent
  -> RAG / Runbook
  -> 대응 추천 또는 자동 실행
```

## 핵심 설계 원칙

1. API 서버는 오래 걸리는 작업을 직접 처리하지 않는다

Spring Boot WebFlux API는 요청을 빠르게 받고, 무거운 작업은 Kafka, Worker, Batch, Agent 계층으로 위임한다.

2. 장애는 관측 가능해야 한다

모든 주요 구성 요소는 로그, 메트릭, 트레이스, 이벤트를 남긴다.

3. 장애 대응은 Runbook 기반이어야 한다

AI Agent는 임의로 판단하지 않고, Runbook과 RAG 문서를 기반으로 대응한다.

4. 실행은 Guardrail을 통과해야 한다

AI Agent가 수행하는 조치는 승인, 권한, 위험도, 롤백 가능성을 기준으로 제한된다.

5. Kubernetes는 나중 확장이 아니라 기본 실행 환경이다

이 플랫폼에서 Kubernetes는 초기 구축 범위에 포함된다.