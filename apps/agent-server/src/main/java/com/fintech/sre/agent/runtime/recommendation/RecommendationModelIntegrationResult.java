package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public record RecommendationModelIntegrationResult(
		RecommendationModel model,
		RecommendationModelIntegrationStatus status,
		RecommendationModelIntegrationReason reason,
		RecommendationModelIntegrationScope scope,
		boolean operatorFacingRecommendationVisible,
		boolean recommendationExposureCertaintyAllowed
) {
	public RecommendationModelIntegrationResult {
		Objects.requireNonNull(model, "model must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationMutation() {
		return false;
	}

	public boolean approvalAuthority() {
		return false;
	}

	public boolean actionAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
