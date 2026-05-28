package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ActionAdmissionRequirement(
		ReliabilityRiskClassification riskClassification,
		HumanApprovalDecision humanApprovalDecision,
		RecommendationEligibility recommendationEligibility,
		ActionCommandEligibility actionCommandEligibility,
		ScenarioBindingDecision scenarioBindingDecision,
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
		SafetyPolicyDecision safetyPolicyDecision,
		boolean unrestrictedRequested
) {
	public ActionAdmissionRequirement {
		Objects.requireNonNull(
				riskClassification,
				"riskClassification must not be null"
		);
		Objects.requireNonNull(
				humanApprovalDecision,
				"humanApprovalDecision must not be null"
		);
		Objects.requireNonNull(
				recommendationEligibility,
				"recommendationEligibility must not be null"
		);
		Objects.requireNonNull(
				actionCommandEligibility,
				"actionCommandEligibility must not be null"
		);
		Objects.requireNonNull(
				scenarioBindingDecision,
				"scenarioBindingDecision must not be null"
		);
		Objects.requireNonNull(
				rollbackVerificationBindingDecision,
				"rollbackVerificationBindingDecision must not be null"
		);
		Objects.requireNonNull(
				safetyPolicyDecision,
				"safetyPolicyDecision must not be null"
		);
	}

	public boolean approvalRequired() {
		return riskClassification.level().ordinal() >= ReliabilityRiskLevel.HIGH.ordinal()
				|| humanApprovalDecision.approvalRequired();
	}

	public boolean explicitApprovalRequired() {
		return riskClassification.level() == ReliabilityRiskLevel.CRITICAL
				|| humanApprovalDecision.scope() == HumanApprovalScope.CRITICAL_EXPLICIT;
	}

	public boolean rollbackReviewRequired() {
		return riskClassification.level() == ReliabilityRiskLevel.CRITICAL
				|| recommendationEligibility.rollbackRequirement()
				|| actionCommandEligibility.requirement().rollbackRequirement();
	}

	public boolean verificationReviewRequired() {
		return riskClassification.level() == ReliabilityRiskLevel.CRITICAL
				|| recommendationEligibility.verificationRequirement()
				|| actionCommandEligibility.requirement().verificationRequirement();
	}
}
