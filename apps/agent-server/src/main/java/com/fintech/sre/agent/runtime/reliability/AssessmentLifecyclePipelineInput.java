package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record AssessmentLifecyclePipelineInput(
		EvidenceAssessmentPipelineResult assessmentPipelineResult,
		ScenarioReference scenarioReference,
		RollbackReference rollbackReference,
		VerificationReference verificationReference,
		boolean approvalProvided,
		boolean explicitApprovalProvided,
		boolean paymentSafetyAction,
		boolean unrestrictedRequested,
		boolean explicitExecutionAuthorized,
		boolean approvalCompleted,
		boolean rollbackReviewCompleted,
		boolean verificationReviewCompleted,
		LifecycleAuditDecision lifecycleAuditDecision
) {
	public AssessmentLifecyclePipelineInput {
		Objects.requireNonNull(
				assessmentPipelineResult,
				"assessmentPipelineResult must not be null"
		);
		Objects.requireNonNull(
				lifecycleAuditDecision,
				"lifecycleAuditDecision must not be null"
		);
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
