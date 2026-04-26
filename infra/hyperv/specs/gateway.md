# gateway

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.105 |
| Hostname | gateway |
| OS | Amazon Linux 2 |
| Network | ExternalSwitch |
| Kubernetes Role | 클러스터 외부 진입점 (미참여) |

## 역할

gateway는 외부 사용자가 플랫폼에 접근하는 첫 진입점이다.
Kubernetes 클러스터에는 포함되지 않으며 독립 노드로 운영된다.

## 구성 요소

- Nginx
- React Web Console
- Jenkins 또는 GoCD
- Git Webhook Endpoint

## 책임

- React 정적 파일 서빙
- Nginx Reverse Proxy
- Kubernetes NodePort로 트래픽 전달
- GitHub Webhook 수신
- CI/CD Job 트리거

## SRE 관점 의미

gateway는 사용자 요청의 시작점이므로 다음 장애 분석에 중요하다.

- 4xx / 5xx 증가
- Reverse Proxy timeout
- upstream unavailable
- TLS 인증서 문제
- Git Webhook 실패
- CI/CD 배포 실패

## 구축 이력

### 2026-04-26

**공통 패키지 및 Docker 설치**

bootstrap.sh 실행. Docker CE CentOS repo 404 오류로 `amazon-linux-extras install docker` 방식으로 해결 완료.

**Kubernetes 클러스터 미참여 (설계 기준)**

gateway는 Kubernetes Worker Node로 join하지 않는다.
클러스터 구성 노드: control-plane-1, app-node-1, data-node-1, obs-node-1.
gateway(172.30.1.105)는 외부 진입점 및 CI/CD 역할로 독립 운영한다.

## 추후 구성

- Nginx 설치 및 Reverse Proxy 구성
- Jenkins 또는 GoCD 설치
- Webhook endpoint 구성
- React Web Console 배포
- Nginx access/error log 수집