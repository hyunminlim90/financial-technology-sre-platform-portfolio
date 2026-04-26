# observability-node

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.108 |
| Hostname | observability-node |
| OS | Amazon Linux 계열 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |

## 역할

observability-node는 플랫폼 전체의 관측성 계층을 담당한다.

## 구성 요소

- Prometheus
- Grafana
- ELK
- Loki
- Alertmanager

## 책임

- 메트릭 수집
- 로그 수집
- 대시보드 제공
- 알람 발송
- 장애 감지
- SRE Agent로 이벤트 전달

## SRE 관점 의미

observability-node는 장애 대응의 눈 역할을 한다.

분석 대상:

- CPU / Memory / Disk / Network
- Kubernetes Node 상태
- Pod 상태
- API latency
- HTTP error rate
- DB connection
- Redis latency
- Kafka lag
- Elasticsearch health

## 추후 구성

- Prometheus 설치
- Grafana 설치
- Alertmanager 설치
- ELK 또는 Loki 설치
- Node Exporter 구성
- Kubernetes metrics 수집