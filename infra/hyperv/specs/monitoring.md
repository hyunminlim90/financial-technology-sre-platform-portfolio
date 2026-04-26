# observability-node (obs-node-1)

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.108 |
| Hostname | obs-node-1 |
| OS | Amazon Linux 2 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |
| Node Label | node-role=observability |

## 역할

obs-node-1은 플랫폼 전체의 관측성 계층을 담당한다.

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

obs-node-1은 장애 대응의 눈 역할을 한다.

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

## 구축 이력

### 2026-04-26

**공통 패키지 및 Docker 설치**

bootstrap.sh 실행. Docker CE CentOS repo 404 오류로 `amazon-linux-extras install docker` 방식으로 해결 완료.

**containerd 활성화 확인**

```bash
sudo systemctl status containerd
# → active (running)
```

**Worker Node Join 완료**

```bash
kubeadm join 172.30.1.109:6443 \
  --token <token> \
  --discovery-token-ca-cert-hash sha256:<hash>
```

클러스터 합류 및 Ready 확인 완료:

```
obs-node-1   Ready   <none>   6m37s   v1.30.14
```

**노드 라벨 적용 완료**

```bash
kubectl label node obs-node-1 node-role=observability
```

## 추후 구성

- Prometheus 설치
- Grafana 설치
- Alertmanager 설치
- ELK 또는 Loki 설치
- Node Exporter 구성
- Kubernetes metrics 수집