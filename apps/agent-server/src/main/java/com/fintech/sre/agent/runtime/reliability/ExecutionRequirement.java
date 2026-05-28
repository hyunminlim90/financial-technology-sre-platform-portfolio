package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ExecutionRequirement(
		ActionAdmissionDecision actionAdmissionDecision,
		boolean explicitExecutionAuthorized,
		boolean approvalCompleted,
		boolean rollbackReviewCompleted,
		boolean verificationReviewCompleted
) {
	public ExecutionRequirement {
		Objects.requireNonNull(
				actionAdmissionDecision,
				"actionAdmissionDecision must not be null"
		);
	}

	public ReliabilityAssessmentResult assessmentResult() {
		return actionAdmissionDecision.requirement()
				.safetyPolicyDecision()
				.requirement()
				.assessmentResult();
	}

	public boolean critical() {
		return actionAdmissionDecision.requirement().riskClassification().level()
				== ReliabilityRiskLevel.CRITICAL;
	}
}
