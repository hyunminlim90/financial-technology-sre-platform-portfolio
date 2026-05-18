CREATE TABLE IF NOT EXISTS recommendation_records (
    recommendation_record_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    audit_id VARCHAR(128),
    source VARCHAR(128),
    service VARCHAR(128),
    domain VARCHAR(128),
    severity VARCHAR(64),
    status VARCHAR(64),
    generated_at TIMESTAMPTZ NOT NULL,
    recommended_action_count INTEGER NOT NULL DEFAULT 0,
    forbidden_action_count INTEGER NOT NULL DEFAULT 0,
    policy_decision VARCHAR(64),
    guardrail_decision VARCHAR(64),
    action_types JSONB NOT NULL DEFAULT '[]'::jsonb,
    blocked_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_incident_id
    ON recommendation_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_generated_at
    ON recommendation_records (generated_at DESC);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_service_generated_at
    ON recommendation_records (service, generated_at DESC);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_domain_generated_at
    ON recommendation_records (domain, generated_at DESC);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_policy_decision
    ON recommendation_records (policy_decision);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_guardrail_decision
    ON recommendation_records (guardrail_decision);

CREATE INDEX IF NOT EXISTS idx_recommendation_records_metadata_gin
    ON recommendation_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS recommendation_approval_records (
    recommendation_approval_id VARCHAR(128) PRIMARY KEY,
    recommendation_record_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    operator_id VARCHAR(128),
    reason TEXT,
    decided_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_recommendation_approval_records_incident_id
    ON recommendation_approval_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_recommendation_approval_records_recommendation_record_id
    ON recommendation_approval_records (recommendation_record_id);

CREATE INDEX IF NOT EXISTS idx_recommendation_approval_records_status
    ON recommendation_approval_records (status);

CREATE INDEX IF NOT EXISTS idx_recommendation_approval_records_decided_at
    ON recommendation_approval_records (decided_at DESC);

CREATE INDEX IF NOT EXISTS idx_recommendation_approval_records_metadata_gin
    ON recommendation_approval_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS execution_plan_records (
    execution_plan_id VARCHAR(128) PRIMARY KEY,
    recommendation_record_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    executable BOOLEAN NOT NULL DEFAULT false,
    requires_final_approval BOOLEAN NOT NULL DEFAULT true,
    created_by VARCHAR(128),
    reason TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    blocked_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_execution_plan_records_incident_id
    ON execution_plan_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_execution_plan_records_recommendation_record_id
    ON execution_plan_records (recommendation_record_id);

CREATE INDEX IF NOT EXISTS idx_execution_plan_records_status
    ON execution_plan_records (status);

CREATE INDEX IF NOT EXISTS idx_execution_plan_records_created_at
    ON execution_plan_records (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_execution_plan_records_metadata_gin
    ON execution_plan_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS human_execution_result_records (
    execution_result_id VARCHAR(128) PRIMARY KEY,
    execution_plan_id VARCHAR(128) NOT NULL,
    recommendation_record_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    operator_id VARCHAR(128),
    summary TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    recorded_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_incident_id
    ON human_execution_result_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_execution_plan_id
    ON human_execution_result_records (execution_plan_id);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_recommendation_record_id
    ON human_execution_result_records (recommendation_record_id);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_status
    ON human_execution_result_records (status);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_recorded_at
    ON human_execution_result_records (recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_human_execution_result_records_metadata_gin
    ON human_execution_result_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS verification_result_records (
    verification_result_id VARCHAR(128) PRIMARY KEY,
    execution_result_id VARCHAR(128) NOT NULL,
    execution_plan_id VARCHAR(128) NOT NULL,
    recommendation_record_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    operator_id VARCHAR(128),
    summary TEXT,
    verified_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_incident_id
    ON verification_result_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_execution_result_id
    ON verification_result_records (execution_result_id);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_recommendation_record_id
    ON verification_result_records (recommendation_record_id);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_status
    ON verification_result_records (status);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_verified_at
    ON verification_result_records (verified_at DESC);

CREATE INDEX IF NOT EXISTS idx_verification_result_records_metadata_gin
    ON verification_result_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS incident_lifecycle_records (
    incident_lifecycle_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    previous_status VARCHAR(64),
    current_status VARCHAR(64) NOT NULL,
    transition_reason VARCHAR(128),
    operator_id VARCHAR(128),
    summary TEXT,
    transitioned_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_incident_lifecycle_records_incident_id
    ON incident_lifecycle_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_incident_lifecycle_records_current_status
    ON incident_lifecycle_records (current_status);

CREATE INDEX IF NOT EXISTS idx_incident_lifecycle_records_transitioned_at
    ON incident_lifecycle_records (transitioned_at DESC);

CREATE INDEX IF NOT EXISTS idx_incident_lifecycle_records_incident_transitioned_at
    ON incident_lifecycle_records (incident_id, transitioned_at DESC);

CREATE INDEX IF NOT EXISTS idx_incident_lifecycle_records_metadata_gin
    ON incident_lifecycle_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS postmortem_draft_records (
    postmortem_draft_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    requested_by VARCHAR(128),
    summary TEXT,
    timeline JSONB NOT NULL DEFAULT '[]'::jsonb,
    recommendations JSONB NOT NULL DEFAULT '[]'::jsonb,
    execution_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    verification_results JSONB NOT NULL DEFAULT '[]'::jsonb,
    reanalysis_candidates JSONB NOT NULL DEFAULT '[]'::jsonb,
    learning_candidates JSONB NOT NULL DEFAULT '[]'::jsonb,
    open_questions JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_postmortem_draft_records_incident_id
    ON postmortem_draft_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_postmortem_draft_records_status
    ON postmortem_draft_records (status);

CREATE INDEX IF NOT EXISTS idx_postmortem_draft_records_created_at
    ON postmortem_draft_records (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_postmortem_draft_records_metadata_gin
    ON postmortem_draft_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS postmortem_review_records (
    postmortem_review_id VARCHAR(128) PRIMARY KEY,
    postmortem_draft_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(128),
    review_reason TEXT,
    review_summary TEXT,
    reviewed_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_postmortem_review_records_draft_id
    ON postmortem_review_records (postmortem_draft_id);

CREATE INDEX IF NOT EXISTS idx_postmortem_review_records_incident_id
    ON postmortem_review_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_postmortem_review_records_status
    ON postmortem_review_records (status);

CREATE INDEX IF NOT EXISTS idx_postmortem_review_records_reviewed_at
    ON postmortem_review_records (reviewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_postmortem_review_records_metadata_gin
    ON postmortem_review_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS learning_candidate_records (
    learning_candidate_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    postmortem_draft_id VARCHAR(128) NOT NULL,
    postmortem_review_id VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    promoted_by VARCHAR(128),
    summary TEXT,
    proposed_changes JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_incident_id
    ON learning_candidate_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_draft_id
    ON learning_candidate_records (postmortem_draft_id);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_review_id
    ON learning_candidate_records (postmortem_review_id);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_type
    ON learning_candidate_records (type);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_status
    ON learning_candidate_records (status);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_created_at
    ON learning_candidate_records (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_learning_candidate_records_metadata_gin
    ON learning_candidate_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS knowledge_promotion_review_records (
    promotion_review_id VARCHAR(128) PRIMARY KEY,
    learning_candidate_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    reviewed_by VARCHAR(128),
    review_reason TEXT,
    review_summary TEXT,
    reviewed_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_review_records_candidate_id
    ON knowledge_promotion_review_records (learning_candidate_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_review_records_incident_id
    ON knowledge_promotion_review_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_review_records_status
    ON knowledge_promotion_review_records (status);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_review_records_reviewed_at
    ON knowledge_promotion_review_records (reviewed_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_review_records_metadata_gin
    ON knowledge_promotion_review_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS knowledge_promotion_plan_records (
    promotion_plan_id VARCHAR(128) PRIMARY KEY,
    learning_candidate_id VARCHAR(128) NOT NULL,
    incident_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    planned_by VARCHAR(128),
    summary TEXT,
    targets JSONB NOT NULL DEFAULT '[]'::jsonb,
    required_human_checks JSONB NOT NULL DEFAULT '[]'::jsonb,
    blocked_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_plan_records_candidate_id
    ON knowledge_promotion_plan_records (learning_candidate_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_plan_records_incident_id
    ON knowledge_promotion_plan_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_plan_records_status
    ON knowledge_promotion_plan_records (status);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_plan_records_created_at
    ON knowledge_promotion_plan_records (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_promotion_plan_records_metadata_gin
    ON knowledge_promotion_plan_records USING GIN (metadata);

CREATE TABLE IF NOT EXISTS knowledge_update_application_records (
    knowledge_update_application_id VARCHAR(128) PRIMARY KEY,
    incident_id VARCHAR(128) NOT NULL,
    learning_candidate_id VARCHAR(128) NOT NULL,
    promotion_plan_id VARCHAR(128) NOT NULL,
    knowledge_type VARCHAR(128),
    knowledge_layer VARCHAR(128),
    file_path TEXT NOT NULL,
    change_type VARCHAR(64) NOT NULL,
    git_repository VARCHAR(256),
    git_branch VARCHAR(256),
    git_commit_sha VARCHAR(128) NOT NULL,
    pull_request_reference VARCHAR(512),
    applied_by VARCHAR(128),
    reviewed_by VARCHAR(128),
    approved_by VARCHAR(128),
    validation_checks JSONB NOT NULL DEFAULT '[]'::jsonb,
    applied_at TIMESTAMPTZ NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_incident_id
    ON knowledge_update_application_records (incident_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_learning_candidate_id
    ON knowledge_update_application_records (learning_candidate_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_promotion_plan_id
    ON knowledge_update_application_records (promotion_plan_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_knowledge_layer
    ON knowledge_update_application_records (knowledge_layer);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_change_type
    ON knowledge_update_application_records (change_type);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_applied_at
    ON knowledge_update_application_records (applied_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_update_application_records_metadata_gin
    ON knowledge_update_application_records USING GIN (metadata);
