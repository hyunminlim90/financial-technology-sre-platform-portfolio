package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RecommendationReliabilityIntegrationResult(
		RecommendationReliability recommendationReliability,
		EvidenceRuntimeApiResponse apiResponse,
		RecommendationReliabilityIntegrationStatus status,
		RecommendationReliabilityIntegrationReason reason,
		RecommendationReliabilityIntegrationScope scope,
		boolean operatorFacingRecommendationAllowed,
		boolean recommendationCertaintyAllowed
) {
	public RecommendationReliabilityIntegrationResult {
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesRecommendation() {
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

	public boolean humanApproval() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
