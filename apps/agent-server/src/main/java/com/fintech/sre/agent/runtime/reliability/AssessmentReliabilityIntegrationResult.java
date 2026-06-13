package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record AssessmentReliabilityIntegrationResult(
		AssessmentReliability assessmentReliability,
		EvidenceRuntimeApiResponse apiResponse,
		AssessmentReliabilityIntegrationStatus status,
		AssessmentReliabilityIntegrationReason reason,
		AssessmentReliabilityIntegrationScope scope,
		boolean lifecycleStableAllowed,
		boolean recommendationCertaintyAllowed
) {
	public AssessmentReliabilityIntegrationResult {
		Objects.requireNonNull(
				assessmentReliability,
				"assessmentReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesAssessment() {
		return false;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionAdmission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
