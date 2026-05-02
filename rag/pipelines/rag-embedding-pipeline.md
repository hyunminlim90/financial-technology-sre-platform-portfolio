# RAG Embedding Pipeline

> Markdown 기반 운영 지식을 AI Agent가 검색 가능한 Knowledge로 변환하는 인덱싱 파이프라인

---

## 1. 목적

이 문서는 RAG 대상 문서를 AI Agent가 검색 가능한 형태로 변환하는 전체 파이프라인을 정의한다.

```text
Markdown 문서
→ Metadata 파싱
→ Validation
→ Chunking
→ Embedding
→ Vector Store 저장
→ Retrieval
```

이 파이프라인의 목적은 단순 embedding 생성이 아니다.

> 문서를 AI가 안전하게 판단할 수 있는 **운영 지식 구조로 변환**하는 것

---

## 2. 대상 문서

**Primary Knowledge**

```
scenarios/
runbooks/
improvements/
preventive-designs/
postmortems/
protocols/
```

**Secondary Knowledge**

```
rag/docs/
```

---

## 3. 전체 파이프라인

```
[1] Document Scan
        ↓
[2] Front Matter Parse
        ↓
[3] Document Validation
        ↓
[4] Chunking
        ↓
[5] Chunk Metadata Attach
        ↓
[6] Embedding Generate
        ↓
[7] Vector Store Upsert
        ↓
[8] Retrieval Smoke Test
```

---

## 4. 디렉터리 역할

```
rag/
├── docs/               # Secondary Knowledge
├── metadata/           # parsed document metadata
├── chunks/             # chunk 결과
├── embeddings/         # embedding 결과 또는 cache
├── pipelines/          # RAG pipeline 설계 문서
└── prompts/

scripts/
├── validate-rag-docs.py       # front matter / related path 검증
├── import-rag-docs.py         # 문서 scan + chunk 생성
├── generate-embeddings.py     # embedding 생성 + vector store upsert
├── retrieval-smoke-test.py    # 검색 품질 smoke test
└── seed-demo-data.py          # demo 데이터 적재
```

> 현재 `scripts/`는 RAG Pipeline 실행을 위한 엔트리포인트를 정의한다.  
> 실제 구현은 RAG Pipeline 구현 단계에서 작성한다.

---

## 5. Metadata Schema

모든 RAG 대상 문서는 YAML Front Matter를 가져야 한다.

```yaml
---
title: Redis Timeout Scenario
knowledge_type: scenario
domain: redis
failure_mode: redis-timeout
environment: production
severity: SEV-2
impact_scope: partial

services:
  - payment-api
  - redis
  - postgresql

related_scenarios: []
related_runbooks:
  - runbooks/redis/timeout.md
related_postmortems: []
related_improvements:
  - improvements/redis-timeout-idempotency-hardening.md
related_preventive_designs:
  - preventive-designs/redis-timeout-idempotency-fallback.md

tags:
  - redis
  - timeout
  - latency
---
```

---

## 6. Knowledge Type Mapping

| `knowledge_type` | 디렉터리 |
|---|---|
| `scenario` | `scenarios/` |
| `runbook` | `runbooks/` |
| `improvement` | `improvements/` |
| `preventive-design` | `preventive-designs/` |
| `postmortem` | `postmortems/` |
| `protocol` | `protocols/` |
| `rag-doc` | `rag/docs/` |

---

## 7. Validation Rule

인덱싱 전에 반드시 검증한다.

**필수 검증:**

1. front matter 존재 여부
2. `knowledge_type` 유효성
3. `domain` 존재 여부
4. `failure_mode` 존재 여부
5. `related_*` 경로 유효성
6. `tags` 존재 여부

**예외:**

`rag/docs`는 `failure_mode`가 없을 수 있다.

대신 반드시 다음을 가진다:
- `domain`
- `topic`
- `related_*` 경로
- `tags`

**Postmortem 인덱싱 조건:**

```
approval_status: approved
```

> 검증되지 않은 Postmortem은 RAG에 포함하지 않는다.

---

## 8. Chunking Strategy

문서 전체를 단순 길이로 자르지 않는다.

**기본 원칙:** 문서 구조 기반 chunking

**우선순위:**

1. Markdown heading 기준
2. Front Matter metadata 유지
3. Section 단위로 chunk 생성
4. 너무 긴 section만 token 기준 분할

**Chunk 크기 기준:**

| 항목 | 값 |
|---|---|
| `target_chunk_size` | 800 ~ 1200 tokens |
| `max_chunk_size` | 1500 tokens |
| `overlap` | 100 ~ 150 tokens |

> **주의:** Runbook / Scenario는 섹션 의미가 중요하므로 과도한 overlap 금지

---

## 9. Chunk Metadata

각 chunk는 원문 metadata를 상속한다.

```json
{
  "document_id": "scenarios/redis/timeout.md",
  "chunk_id": "scenarios/redis/timeout.md#section-4",
  "path": "scenarios/redis/timeout.md",
  "title": "Redis Timeout Scenario",
  "knowledge_type": "scenario",
  "domain": "redis",
  "failure_mode": "redis-timeout",
  "environment": "production",
  "severity": "SEV-2",
  "impact_scope": "partial",
  "section_title": "Propagation",
  "tags": ["redis", "timeout", "latency"],
  "related_runbooks": ["runbooks/redis/timeout.md"]
}
```

---

## 10. Chunk Content Format

Embedding 대상 content는 metadata context를 앞에 붙인다.

```
Title: Redis Timeout Scenario
Knowledge Type: scenario
Domain: redis
Failure Mode: redis-timeout
Section: Propagation
Tags: redis, timeout, latency

Content:
Redis timeout
→ API latency 증가
→ retry 증가
→ DB overload
```

**이유:**

> Vector search가 본문만 보지 않고 문서 의미와 context까지 함께 학습하게 한다.

---

## 11. Embedding 생성

**입력:**

```
chunk.content_for_embedding
```

**출력:**

```json
{
  "chunk_id": "scenarios/redis/timeout.md#section-4",
  "embedding": [0.012, -0.032],
  "metadata": {}
}
```

---

## 12. Vector Store Schema

Vector Store에는 최소 다음 필드를 저장한다.

| 필드 |
|---|
| `chunk_id` |
| `document_id` |
| `path` |
| `knowledge_type` |
| `domain` |
| `failure_mode` |
| `environment` |
| `severity` |
| `impact_scope` |
| `tags` |
| `related_*` |
| `content` |
| `embedding` |
| `created_at` |
| `updated_at` |

---

## 13. Retrieval Query Strategy

agent-server는 다음 정보로 검색한다.

```
incident_id
alert_name
service
environment
domain_hint
failure_mode_hint
severity_hint
impact_scope_hint
metric keywords
log keywords
trace span keywords
operator_note
```

**검색 query 예:**

```
redis timeout payment-api retry_rate db_connection_pending production
```

---

## 14. Hybrid Retrieval

실무 기준으로 Vector Search만 사용하지 않는다.

**추천 방식:** `Hybrid Search = metadata filter + keyword search + vector search`

**순서:**

1. **metadata filter**
   - `environment`
   - `domain`
   - `failure_mode`

2. **keyword match**
   - `alert_name`
   - `tags`
   - body keywords

3. **vector similarity**
   - semantic match

4. **reranking**
   - knowledge priority
   - exact match boost

---

## 15. Retrieval Priority Boost

점수 보정 기준:

| 조건 | 보정 |
|---|---|
| `failure_mode` exact match | `+0.30` |
| `domain` exact match | `+0.20` |
| `related_*` path match | `+0.20` |
| tag match | `+0.10` |
| same environment | `+0.10` |
| recent postmortem | `+0.10` |
| `rag/docs` | `-0.10` |

> **주의:** `rag/docs`는 검색될 수 있지만, Action 결정 우선순위에서는 항상 최하위다.

---

## 16. Knowledge Priority

Decision Engine에서 최종 우선순위는 다음을 따른다.

```
Preventive Design
  > Improvement
  > Postmortem
  > Runbook
  > Scenario
  > rag/docs
```

> Pipeline은 이 우선순위를 metadata로 보존해야 한다.

---

## 17. Jenkins Pipeline 연계

RAG 문서가 변경되면 Jenkins Pipeline에서 인덱싱 작업을 수행한다.

**감지 대상:**

```
scenarios/**
runbooks/**
improvements/**
preventive-designs/**
postmortems/**
protocols/**
rag/docs/**
```

**Pipeline 단계:**

1. Checkout
2. Validate RAG Documents
3. Generate Chunks
4. Generate Embeddings
5. Upsert Vector Store
6. Retrieval Smoke Test

---

## 18. Jenkinsfile 예시

```groovy
pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Validate RAG Documents') {
            steps {
                sh 'python scripts/validate-rag-docs.py'
            }
        }

        stage('Generate Chunks') {
            steps {
                sh 'python scripts/import-rag-docs.py'
            }
        }

        stage('Generate Embeddings') {
            steps {
                sh 'python scripts/generate-embeddings.py'
            }
        }

        stage('Retrieval Smoke Test') {
            steps {
                sh 'python scripts/retrieval-smoke-test.py'
            }
        }
    }
}
```

---

## 19. Jenkins 실패 기준

다음 조건에서는 Jenkins Build를 실패 처리한다.

- Front Matter 누락
- `knowledge_type` 오류
- `related_*` 경로 오류
- `approved`되지 않은 postmortem 인덱싱 시도
- required metadata 누락
- expected document 검색 실패

---

## 20. Pipeline Script 역할

### `validate-rag-docs.py`
- front matter 검증
- `knowledge_type` 검증
- `related_*` 경로 검증
- 필수 필드 검증
- `approved`되지 않은 postmortem 제외

### `import-rag-docs.py`
- 문서 scan
- metadata parse
- heading 기반 chunk 생성
- `rag/chunks/*.json` 저장

### `generate-embeddings.py`
- chunk 파일 읽기
- embedding 생성
- `rag/embeddings/*.json` 저장
- vector store upsert

### `retrieval-smoke-test.py`
- 대표 query 실행
- expected document 검색 여부 검증
- retrieval 품질 최소 기준 확인

---

## 21. Pseudo Pipeline

```python
def pipeline():
    docs = scan_documents([
        "scenarios",
        "runbooks",
        "improvements",
        "preventive-designs",
        "postmortems",
        "protocols",
        "rag/docs",
    ])

    validated_docs = validate_front_matter(docs)

    chunks = []
    for doc in validated_docs:
        chunks.extend(chunk_markdown_by_heading(doc))

    embeddings = embedding_client.embed([
        chunk.content_for_embedding for chunk in chunks
    ])

    vector_store.upsert(chunks, embeddings)
```

---

## 22. 문서 변경 시 처리

문서가 변경되면 다음을 수행한다.

1. changed file detect
2. 해당 문서 metadata 재파싱
3. 해당 문서 기존 chunk 삭제
4. 새 chunk 생성
5. embedding 재생성
6. vector store upsert

---

## 23. Retrieval Smoke Test

인덱싱 후 반드시 테스트한다.

**Query:**
```
redis timeout retry db pending
```

**Expected:**
```
- scenarios/redis/timeout.md
- runbooks/redis/timeout.md
- improvements/redis-timeout-idempotency-hardening.md
- preventive-designs/redis-timeout-idempotency-fallback.md
```

**실패 원인 후보:**
- metadata 오류
- chunking 오류
- embedding 반영 실패
- vector store upsert 실패
- retrieval scoring 오류

---

## 24. 운영 안전 규칙

**검증되지 않은 postmortem은 인덱싱 금지**
```
approval_status != approved → exclude
```

**rag/docs는 Action Source로 사용 금지**
```
knowledge_type=rag-doc → action_generation=false
```

**No Scenario → No Action**

---

## 25. Vector Store 선택

초기 포트폴리오 단계 추천:

| 단계 | 방식 |
|---|---|
| 1단계 | Local JSON + in-memory search |
| 2단계 | PostgreSQL pgvector |
| 3단계 | OpenSearch / Qdrant |

**추천: 초기에는 `pgvector`가 가장 적합**

이유:
- PostgreSQL 이미 사용
- 운영 복잡도 낮음
- metadata filter 용이

---

## 26. 구현 단계

**현재 단계:** 설계 문서 확정

**다음 구현 순서:**

1. `validate-rag-docs.py` 구현
2. `import-rag-docs.py` 구현
3. `generate-embeddings.py` 구현
4. `retrieval-smoke-test.py` 구현
5. `pgvector` 연동
6. `agent-server` `RagVectorStoreClient` 구현

---

## 27. 핵심 원칙

> **RAG Pipeline은 단순 embedding 작업이 아니다.**
>
> 문서를 AI가 안전하게 판단할 수 있는  
> **운영 지식 구조로 변환**하는 과정이다.

---

## 28. Re-index & Versioning Strategy

문서 변경 시 반드시 version을 관리한다.

```text
document_version: v1, v2, v3
chunk_version: 동일 상속
```

### Versioning 처리 방식

문서 변경 시:

1. 기존 chunk → is_active=false 처리
2. 새로운 version chunk 생성
3. embedding 재생성
4. vector store upsert

retrieval 시:

- is_active=true만 조회

원칙:

overwrite 금지
append-only + soft delete 방식

---

## 29. Embedding Model Strategy

Embedding 모델은 다음 기준으로 선택한다:

1. 한국어 + 영어 혼합 지원
2. latency < 100ms
3. cost 효율성
4. semantic retrieval 품질

초기 선택:

- OpenAI text-embedding-3-large
또는
- bge-m3 (self-hosted)

원칙:

- 모델 변경 시 전체 re-index 수행

---

## 30. Cold Start Strategy

초기 RAG 상태에서는 다음 전략을 사용한다:

1. scenarios + runbooks 최소 세트 필수 구성
2. 최소 1개 이상 postmortem 포함 권장
3. rag/docs는 선택

Cold Start 상태 판단:

```text
vector_store document_count < N
```

처리:

```text
AI confidence 자동 LOW
aggressive recommendation 금지
```
