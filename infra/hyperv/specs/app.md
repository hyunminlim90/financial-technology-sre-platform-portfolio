# app-node-1

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.106 |
| Hostname | app-node-1 |
| OS | Amazon Linux 2 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |
| Node Label | node-role=app |

## 역할

app-node-1은 애플리케이션 워크로드가 실행되는 Kubernetes Worker Node이다.

## 구성 요소

- Spring Boot WebFlux API
- Spring Batch
- Kafka Client
- JPA/Hibernate
- Gradle
- Kotlin/Java
- Netty

## 책임

- 비동기 API 요청 처리
- Kafka 기반 이벤트 발행/소비
- Batch Job 실행
- DB 연동
- Redis 연동
- 장애 시나리오 발생 대상

## SRE 관점 의미

app-node-1은 서비스 장애의 주요 분석 대상이다.

주요 장애 유형:

- API latency 증가
- Netty event loop blocking
- DB connection pool exhaustion
- Kafka consumer lag
- Redis timeout
- JVM GC pause
- Thread pool saturation

## 구축 이력

### 2026-04-26

**공통 패키지 및 Docker 설치**

bootstrap.sh 실행. Docker CE CentOS repo가 Amazon Linux 2 환경에서 404 오류 발생.

```bash
# 실패한 방식
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install -y containerd.io
# → https://download.docker.com/linux/centos/2/x86_64/stable/repodata/repomd.xml: 404 Not Found
```

bootstrap.sh의 분기 처리에 따라 `amazon-linux-extras install docker`로 해결 완료.

```bash
sudo amazon-linux-extras install docker -y
sudo systemctl enable docker
sudo systemctl start docker
```

**containerd 활성화 확인**

Docker 설치 시 containerd가 함께 구성되어 활성화됨. 상태 확인 완료.

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
app-node-1   Ready   <none>   6m46s   v1.30.14
```

**노드 라벨 적용 완료**

```bash
kubectl label node app-node-1 node-role=app
```

## 모든 노드에서 영구 적용 검증 단계
sudo systemctl is-enabled kubelet <br/>
sudo systemctl is-enabled containerd <br/>
sudo systemctl is-enabled docker <br/>

## 재부팅 후 control-plane-1에서
kubectl get nodes

## 추후 구성

- Spring Boot WebFlux API 배포
- actuator endpoint 활성화
- Prometheus metrics 노출
- 로그 수집 구성
- Kafka producer/consumer 테스트