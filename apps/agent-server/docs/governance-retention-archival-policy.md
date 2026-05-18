# Governance Retention and Archival Policy

## Purpose

This document defines retention and archival policy boundaries for governance persistence records.

Governance records are append-only operational audit artifacts.

The current phase defines policy boundaries only.

Automatic deletion, archival jobs, or partition management are not introduced yet.

## Governance Record Categories

- Recommendation
- Approval
- ExecutionPlan
- HumanExecutionResult
- VerificationResult
- IncidentLifecycle
- PostmortemDraft
- PostmortemReview
- LearningCandidate
- KnowledgePromotionReview
- KnowledgePromotionPlan
- KnowledgeUpdateApplication

## Retention Strategy

| Category | Hot Retention | Archive Retention |
|---|---|---|
| Recommendation / Approval | 90d | 1y+ |
| Execution / Verification | 90d | 1y+ |
| Incident Lifecycle | 180d | 2y+ |
| Postmortem / Learning | 180d | 3y+ |
| Knowledge Update Application | 1y | Long-term |

## Critical Incident Retention

Security incidents and payment integrity incidents may require extended retention based on operational or compliance policy.

These records must not be automatically deleted.

## Operational Principles

The agent-server does not automatically:

- delete governance records
- archive governance records
- mutate operational audit history

All retention and archival operations must be human-controlled operational procedures.

## Future Expansion

Future operational scalability options may include:

- PostgreSQL partitioning
- cold storage archival
- object storage export
- warehouse replication
- governance analytics pipeline
