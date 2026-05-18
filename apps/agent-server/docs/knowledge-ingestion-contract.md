# Knowledge Ingestion Contract

## Source of Truth

portfolio repository is the source of truth.

agent-server must not be the source of truth for production knowledge.

## Supported document types

- SCENARIO
- RUNBOOK
- POSTMORTEM
- IMPROVEMENT
- PREVENTIVE_DESIGN
- POLICY
- PROTOCOL
- RAG_DOC

## Required common fields

- id
- type
- title
- path
- domain
- service
- summary
- content

## Safety rules

- No Scenario -> No Action
- rag/docs must not define actionTypes
- Only RUNBOOK or POLICY may define actionTypes
- Actionable document must include scenarioIds
- RUNBOOK must include runbookIds
- RUNBOOK must include evidenceCodes
- RAG_DOC is auxiliary knowledge only

## Qdrant payload fields

```json
{
  "id": "runbook/payment-latency-mitigation",
  "type": "RUNBOOK",
  "title": "Payment Latency Mitigation",
  "path": "runbooks/payment-latency-mitigation.md",
  "domain": "payment",
  "service": "payment-api",
  "scenarioIds": ["scenario/payment-latency-spike"],
  "runbookIds": ["runbook/payment-latency-mitigation"],
  "postmortemIds": [],
  "improvementIds": [],
  "preventiveDesignIds": [],
  "policyIds": [],
  "evidenceCodes": ["LATENCY_SPIKE", "ERROR_RATE_SPIKE"],
  "actionTypes": ["RATE_LIMIT"],
  "summary": "Mitigate payment API latency spike safely.",
  "content": "...",
  "metadata": {
    "owner": "sre",
    "severity": "high"
  }
}
```

## Pipeline

portfolio repo
-> markdown scan
-> metadata validation
-> chunking
-> embedding
-> qdrant upsert
-> agent-server retrieval
