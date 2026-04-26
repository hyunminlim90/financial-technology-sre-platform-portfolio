# app-node-1

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.106 |
| Hostname | app-node-1 |
| OS | Amazon Linux 계열 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |

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

## 추후 구성

- Spring Boot WebFlux API 배포
- actuator endpoint 활성화
- Prometheus metrics 노출
- 로그 수집 구성
- Kafka producer/consumer 테스트