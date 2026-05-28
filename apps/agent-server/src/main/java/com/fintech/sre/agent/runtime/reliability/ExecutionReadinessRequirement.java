package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionReadinessRequirement(
		ExecutionBoundaryDecision executionBoundaryDecision,
		ExecutionPlan executionPlan,
		ExecutionAuditDecision executionAuditDecision
) {
	public ExecutionReadinessRequirement {
		Objects.requireNonNull(
				executionBoundaryDecision,
				"executionBoundaryDecision must not be null"
		);
		Objects.requireNonNull(executionPlan, "executionPlan must not be null");
		Objects.requireNonNull(
				executionAuditDecision,
				"executionAuditDecision must not be null"
		);
	}

	public boolean critical() {
		return executionBoundaryDecision.requirement().critical();
	}

	public boolean paymentImpacting() {
		return executionPlan.intent().paymentImpacting();
	}

	public boolean aiOnlyDecision() {
		return executionPlan.intent().aiOnlyApproved();
	}

	public boolean explicitAuthorizationPresent() {
		return executionPlan.intent().explicitExecutionAuthorized();
	}

	public boolean approvalCompleted() {
		return executionBoundaryDecision.requirement().approvalCompleted();
	}

	public boolean rollbackReviewCompleted() {
		return executionBoundaryDecision.requirement().rollbackReviewCompleted();
	}

	public boolean verificationReviewCompleted() {
		return executionBoundaryDecision.requirement().verificationReviewCompleted();
	}
}
