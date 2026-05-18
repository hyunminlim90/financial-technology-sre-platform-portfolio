# R2DBC Governance Schema and Index Strategy

## Purpose

This document defines the PostgreSQL schema and index strategy for governance records.

The governance persistence model is append-only oriented and supports:

- operational auditability
- dashboard summary queries
- incident timeline reconstruction
- recommendation history lookup
- future R2DBC store expansion

## Current Scope

Current schema scope:

- recommendation_records
- recommendation_approval_records
- execution_plan_records
- human_execution_result_records
- verification_result_records
- incident_lifecycle_records
- postmortem_draft_records
- postmortem_review_records
- learning_candidate_records
- knowledge_promotion_review_records
- knowledge_promotion_plan_records
- knowledge_update_application_records

## Migration Policy

The agent-server must not automatically mutate production database schema.

Schema changes must be applied through controlled DB operation process.

Current phase uses:

```text
src/main/resources/db/schema-governance.sql
```

Flyway or Liquibase is not introduced yet.

## Append-only Principle

Governance records represent operational history.

Records should not be updated for normal workflow transitions.

Instead, new records should be inserted for:

- approval decisions
- lifecycle transitions
- verification results
- learning reviews
- knowledge update applications

## JSONB Policy

JSONB fields may be used for:

- actionTypes
- blockedReasons
- sanitized metadata

JSONB fields must not store:

- payment payload
- customer data
- secrets
- tokens
- passwords
- raw logs
- full LLM prompts

## Index Strategy

Primary query patterns:

- find by incidentId
- recent dashboard query by generatedAt
- service/domain filtered dashboard query
- policy/guardrail decision breakdown
- sanitized metadata lookup

Indexes:

- idx_recommendation_records_incident_id
- idx_recommendation_records_generated_at
- idx_recommendation_records_service_generated_at
- idx_recommendation_records_domain_generated_at
- idx_recommendation_records_policy_decision
- idx_recommendation_records_guardrail_decision
- idx_recommendation_records_metadata_gin

## recommendation_approval_records

Stores append-only human approval decisions for recommendation records.

Primary query patterns:

- latest approval by recommendationRecordId
- approval history by recommendationRecordId
- incident-level approval history
- dashboard approval decision breakdown
- recent approval decisions

Sensitive metadata must not be persisted.

## human_execution_result_records

Stores append-only human-recorded execution outcomes for externally performed actions.

This table does not represent agent-server execution.

Primary query patterns:

- find by executionResultId
- find by executionPlanId
- find by recommendationRecordId
- incident-level execution outcome history
- recent dashboard query

The table stores:

- operator-reported execution status
- human summary
- started and finished timestamps
- recorded audit timestamp

Sensitive metadata must not be persisted.

## verification_result_records

Stores append-only human-recorded verification observations for prior execution outcomes.

Verification records do not automatically resolve incidents or trigger remediation.

Primary query patterns:

- find by verificationResultId
- find by executionResultId
- find by recommendationRecordId
- incident-level verification history
- recent dashboard query

The table stores:

- human verification status
- operator summary
- verified audit timestamp

Sensitive metadata must not be persisted.

## incident_lifecycle_records

Stores append-only human-controlled incident lifecycle transitions.

Verification records do not automatically resolve incidents. Lifecycle changes remain explicit audit records.

Primary query patterns:

- latest lifecycle by incidentId
- incident transition history
- recent dashboard query
- current status breakdown

The table stores:

- previous and current incident status
- human transition reason
- operator summary
- transitioned audit timestamp

Sensitive metadata must not be persisted.

## postmortem_draft_records

Stores append-only AI-generated postmortem drafts for later human review.

Postmortem drafts remain drafts only. They do not assert root cause certainty or automatically merge knowledge.

Primary query patterns:

- find by postmortemDraftId
- incident-level draft history
- recent dashboard query
- status breakdown

The table stores:

- draft summary
- timeline and recommendation snapshots
- execution and verification snapshots
- reanalysis and learning candidate notes
- open questions for human review

Sensitive metadata must not be persisted.

## postmortem_review_records

Stores append-only human review decisions for postmortem drafts.

AI drafts do not become operational truth automatically. Review records preserve approval, rejection, revision, and pending states explicitly.

Primary query patterns:

- latest review by postmortemDraftId
- review history by postmortemDraftId
- incident-level review history
- recent dashboard query
- status breakdown

The table stores:

- review decision status
- reviewer identity
- review reason and review summary
- reviewed audit timestamp

Sensitive metadata must not be persisted.

## learning_candidate_records

Stores append-only learning candidates derived from approved postmortem review outcomes.

Learning candidates are knowledge promotion candidates only. They do not perform Git writes, RAG ingestion, or direct knowledge updates.

Primary query patterns:

- find by learningCandidateId
- incident-level learning candidate history
- recent dashboard query
- type and status breakdown

The table stores:

- source draft and review references
- candidate type and review status
- human promotion summary
- sanitized proposed changes
- created audit timestamp

Sensitive metadata and unsafe proposed changes must not be persisted.

## knowledge_promotion_review_records

Stores append-only final human review decisions before knowledge promotion planning.

Approved promotion review records still do not modify files, create Git commits, or trigger RAG/vector ingestion.

Primary query patterns:

- latest review by learningCandidateId
- review history by learningCandidateId
- incident-level promotion review history
- recent dashboard query
- status breakdown

The table stores:

- final promotion review decision
- reviewer identity
- review reason and review summary
- reviewed audit timestamp

Sensitive metadata must not be persisted.

## knowledge_promotion_plan_records

Stores append-only human execution plans for future knowledge updates.

Knowledge promotion plans do not modify files, create Git commits, or trigger RAG/vector ingestion automatically.

Primary query patterns:

- find by promotionPlanId
- learningCandidate-level plan history
- incident-level plan history
- recent dashboard query
- status breakdown

The table stores:

- promotion plan summary
- structured target update recommendations
- required human checks
- blocked reasons
- created audit timestamp

Sensitive metadata must not be persisted.

## knowledge_update_application_records

Stores append-only audit records for human-applied knowledge updates after portfolio repository changes are completed externally.

These records do not mean agent-server modified repositories. They only document which file, commit, and pull request humans used.

Primary query patterns:

- find by knowledgeUpdateApplicationId
- incident-level application history
- learningCandidate-level application history
- recent dashboard query
- knowledge layer and change type breakdown

The table stores:

- knowledge file path and change type
- Git repository, branch, commit, and pull request references
- human applier, reviewer, and approver identities
- sanitized validation checks
- applied audit timestamp

Sensitive metadata and unsafe validation checks must not be persisted.

## execution_plan_records

Stores dry-run execution plans generated from approved recommendations.

Execution plans are not executable actions.

Primary query patterns:

- find by executionPlanId
- find by recommendationRecordId
- incident-level execution plan history
- recent dashboard query
- status breakdown

The table stores:

- dry-run execution steps
- rollback/verification availability
- blocked reasons
- final approval requirement

Sensitive metadata must not be persisted.

## Operational Notes

The schema is compatible with:

- Spring WebFlux
- Spring Data R2DBC
- PostgreSQL
- Prometheus/Grafana observability layer

This schema does not introduce:

- JPA
- Hibernate
- blocking JDBC repositories
- automatic migrations
