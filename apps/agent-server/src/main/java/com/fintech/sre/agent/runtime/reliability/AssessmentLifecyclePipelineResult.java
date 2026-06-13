package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record AssessmentLifecyclePipelineResult(
		List<AssessmentLifecyclePipelineStage> stages,
		EvidenceAssessmentPipelineResult assessmentPipelineResult,
		ReliabilityRiskClassification riskClassification,
		HumanApprovalDecision humanApprovalDecision,
		RecommendationEligibility recommendationEligibility,
		ActionCommandEligibility actionCommandEligibility,
		ScenarioBindingDecision scenarioBindingDecision,
		RollbackVerificationBindingDecision rollbackVerificationBindingDecision,
		SafetyPolicyDecision safetyPolicyDecision,
		ActionAdmissionDecision actionAdmissionDecision,
		ExecutionReadinessDecision executionReadinessDecision,
		ReliabilityLifecycleSummary lifecycleSummary,
		AssessmentLifecyclePipelineRejectionReason rejectionReason
) {
	public AssessmentLifecyclePipelineResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(
				assessmentPipelineResult,
				"assessmentPipelineResult must not be null"
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
		Objects.requireNonNull(
				actionAdmissionDecision,
				"actionAdmissionDecision must not be null"
		);
		Objects.requireNonNull(
				executionReadinessDecision,
				"executionReadinessDecision must not be null"
		);
		Objects.requireNonNull(
				lifecycleSummary,
				"lifecycleSummary must not be null"
		);
		stages = List.copyOf(stages);
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
