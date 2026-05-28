package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record SafetyPolicyRequirement(
		ReliabilityAssessmentResult assessmentResult,
		ReliabilityRiskClassification riskClassification,
		HumanApprovalDecision humanApprovalDecision,
		ScenarioBindingDecision scenarioBindingDecision,
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
		boolean approvalProvided,
		boolean explicitApprovalProvided,
		boolean paymentSafetyAction
) {
	public SafetyPolicyRequirement {
		Objects.requireNonNull(
				assessmentResult,
				"assessmentResult must not be null"
		);
		Objects.requireNonNull(
				riskClassification,
				"riskClassification must not be null"
		);
		Objects.requireNonNull(
				humanApprovalDecision,
				"humanApprovalDecision must not be null"
		);
		Objects.requireNonNull(
				scenarioBindingDecision,
				"scenarioBindingDecision must not be null"
		);
		Objects.requireNonNull(
				rollbackVerificationBindingDecision,
				"rollbackVerificationBindingDecision must not be null"
		);
	}
}
