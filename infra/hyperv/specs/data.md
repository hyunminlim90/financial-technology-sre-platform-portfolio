# data-node

## 기본 정보

| 항목 | 값 |
|---|---|
| IP | 172.30.1.107 |
| Hostname | data-node |
| OS | Amazon Linux 계열 |
| Network | ExternalSwitch |
| Kubernetes Role | Worker |

## 역할

data-node는 플랫폼의 데이터 계층을 담당한다.

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

data-node는 다음 장애 시나리오의 핵심 대상이다.

- DB connection exhaustion
- slow query
- lock contention
- Redis memory pressure
- Kafka broker 장애
- Kafka consumer lag
- Elasticsearch disk pressure
- JVM 기반 데이터 컴포넌트 리소스 고갈

## 추후 구성

- MySQL 설치
- Oracle XE 설치
- Redis 설치
- Kafka 설치
- Elasticsearch 설치
- exporter 구성