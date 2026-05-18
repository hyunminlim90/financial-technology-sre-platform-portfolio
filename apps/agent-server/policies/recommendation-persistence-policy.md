# Recommendation Persistence Policy

## Principle

Recommendation records are operational decision records.

They are not execution records.

## Storage Rules

Do not store:
- full alert webhook payload
- full LLM prompt
- payment payload
- sensitive customer data

Allowed:
- incident id
- audit id
- service/domain/severity
- action type summary
- policy/guardrail decision
- blocked reason summary
- generated timestamp

## Future R2DBC Table

```sql
CREATE TABLE recommendation_records (
    recommendation_record_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    audit_id VARCHAR(128),
    source VARCHAR(64),
    service VARCHAR(128),
    domain VARCHAR(128),
    severity VARCHAR(64),
    status VARCHAR(64),
    generated_at TIMESTAMPTZ NOT NULL,
    recommended_action_count INT NOT NULL,
    forbidden_action_count INT NOT NULL,
    policy_decision VARCHAR(64),
    guardrail_decision VARCHAR(64),
    action_types JSONB NOT NULL DEFAULT '[]',
    blocked_reasons JSONB NOT NULL DEFAULT '[]',
    metadata JSONB NOT NULL DEFAULT '{}'
);

CREATE INDEX idx_recommendation_records_incident_id
    ON recommendation_records (incident_id);

CREATE INDEX idx_recommendation_records_generated_at
    ON recommendation_records (generated_at DESC);
```

## Approval Boundary

Recommendation approval is a human review record.

Approval does not execute any operational action.

Allowed approval states:

```text
PENDING
APPROVED
REJECTED
```

Approval records must store:

- recommendationRecordId
- incidentId
- operatorId
- decision
- reason
- decidedAt

Approval records must not store:

- payment payload
- customer data
- full alert payload
- LLM prompt

## Approval Audit Trail

Approval and rejection decisions must be auditable.

Audit records must include:

- operatorId
- decision
- reason
- incidentId
- recommendationRecordId
- decidedAt

Audit logs must be append-only.

Audit logs must not contain:

- payment payload
- customer data
- full alert payload
- LLM prompt

## Execution Plan Boundary

Execution plans are dry-run only.

They are not operational execution.

Execution plans may be created only from APPROVED recommendations.

Execution plans must preserve:

- rollback requirement
- verification requirement
- final approval requirement

Execution plans must not:

- execute kubectl
- mutate ArgoCD
- call Cloudflare API
- modify Kubernetes resources
- modify payment data

## Human Execution Result Boundary

Human execution result records describe what an operator did outside agent-server.

They are not execution commands.

agent-server must not:

- run kubectl
- mutate ArgoCD
- call Cloudflare API
- execute shell commands
- modify payment data

Execution result records may store:

- executionPlanId
- recommendationRecordId
- incidentId
- operatorId
- execution status
- summary
- startedAt / finishedAt
- safe metadata

Execution result records must not store:

- payment payload
- customer data
- secrets or tokens
- full command output
- raw logs

## Verification Result Boundary

Verification results describe whether operational recovery
was observed after human execution.

Verification results are observational records.

Verification results must not:

- automatically close incidents
- automatically execute remediation
- mutate infrastructure
- modify payment data

Verification results may include:

- alert recovery status
- latency normalization
- error-rate observations
- queue stabilization summaries
- regression detection summaries

## Incident Lifecycle Boundary

Incident lifecycle transitions are explicit operational decisions.

Incident state transitions must not:

- automatically execute remediation
- automatically close incidents
- bypass human review

Incident lifecycle records are append-only operational history.

## Re-analysis Candidate Boundary

Re-analysis candidates are operational review artifacts.

They indicate that:

- verification failed
- regression was detected
- stabilization failed
- new symptoms emerged

Re-analysis candidates must not automatically:

- trigger remediation
- execute infrastructure changes
- bypass human approval

## Postmortem Draft Policy

### Principle

Postmortem drafts are human-review artifacts.

AI must not assert root cause certainty.

Drafts must use language such as:

- probable contributing factor
- observed evidence
- requires human verification
- open question

Drafts must not:

- automatically become final postmortems
- automatically update knowledge base
- automatically trigger RAG ingestion
- store payment payloads
- store customer data
- store full LLM prompts
- store raw logs

## Postmortem Human Review Boundary

Postmortem drafts are review artifacts.

Human review is required before:

- learning knowledge promotion
- preventive-design updates
- scenario/runbook improvements
- RAG ingestion

AI-generated drafts must not:

- automatically become operational truth
- automatically modify knowledge systems
- bypass reviewer approval

## Learning Candidate Promotion Boundary

Learning candidates are human-reviewed knowledge candidates.

Promotion does not:

- modify portfolio repositories
- commit Git changes
- update RAG automatically
- ingest vectors automatically
- bypass human operational review

All learning promotion remains human-controlled.

## Knowledge Promotion Review Boundary

Knowledge promotion review is the final human review boundary before planning knowledge updates.

APPROVED_FOR_PROMOTION means the learning candidate is eligible for a knowledge promotion plan.

It does not:

- modify portfolio repositories
- commit Git changes
- create pull requests
- update RAG automatically
- ingest vectors automatically

## Knowledge Promotion Plan Boundary

Knowledge promotion plans are human execution plans for knowledge updates.

They may recommend:

- target knowledge type
- recommended file path
- proposed change summary
- validation checklist

They must not:

- modify portfolio files
- create Git commits
- create pull requests
- trigger RAG ingestion
- update vector stores

## Knowledge Update Application Boundary

Knowledge update application records are append-only operational audit records.

They document:

- applied knowledge files
- Git references
- human reviewers
- validation checks

They must not:

- directly modify repositories
- create commits automatically
- bypass Git review
- ingest RAG content automatically
