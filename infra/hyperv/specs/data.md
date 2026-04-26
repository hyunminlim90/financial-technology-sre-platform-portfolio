# data-node-1

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.107 |
| Hostname | data-node-1 |
| OS | Amazon Linux 2 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |
| Node Label | node-role=data |

## 역할

data-node-1은 플랫폼의 데이터 계층을 담당한다.

## 구성 요소

- MySQL
- Oracle XE
- Redis
- Kafka
- Elasticsearch

## 책임

- RDBMS 저장소 제공
- Redis 기반 캐시 및 동시성 제어
- Kafka 기반 이벤트 스트리밍
- Elasticsearch 기반 로그/검색 저장소
- 장애 시나리오 재현

## SRE 관점 의미

data-node-1은 다음 장애 시나리오의 핵심 대상이다.

- DB connection exhaustion
- slow query
- lock contention
- Redis memory pressure
- Kafka broker 장애
- Kafka consumer lag
- Elasticsearch disk pressure
- JVM 기반 데이터 컴포넌트 리소스 고갈

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
data-node-1   Ready   <none>   6m41s   v1.30.14
```

**노드 라벨 적용 완료**

```bash
kubectl label node data-node-1 node-role=data
```

## 추후 구성

- MySQL 설치
- Oracle XE 설치
- Redis 설치
- Kafka 설치
- Elasticsearch 설치
- exporter 구성