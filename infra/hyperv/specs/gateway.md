# gateway

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.105 |
| Hostname | gateway |
| OS | Amazon Linux 계열 |
| Network | ExternalSwitch |
| Kubernetes Role | Cluster 외부 진입점 |

## 역할

gateway는 외부 사용자가 플랫폼에 접근하는 첫 진입점이다.

## 구성 요소

- Nginx
- React Web Console
- Jenkins 또는 GoCD
- Git Webhook Endpoint

## 책임

- React 정적 파일 서빙
- Nginx Reverse Proxy
- Kubernetes Ingress 또는 NodePort로 트래픽 전달
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

## 추후 구성

- Nginx access/error log 수집
- Jenkins 또는 GoCD 설치
- Webhook endpoint 구성
- React Web Console 배포