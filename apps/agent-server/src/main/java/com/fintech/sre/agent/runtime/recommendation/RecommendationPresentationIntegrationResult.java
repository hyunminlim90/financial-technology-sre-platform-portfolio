package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public record RecommendationPresentationIntegrationResult(
		RecommendationPresentation presentation,
		RecommendationPresentationIntegrationStatus status,
		RecommendationPresentationIntegrationReason reason,
		RecommendationPresentationIntegrationScope scope,
		boolean operatorFacingPresentationVisible,
		boolean presentationExposureCertaintyAllowed
) {
	public RecommendationPresentationIntegrationResult {
		Objects.requireNonNull(presentation, "presentation must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean uiImplementation() {
		return false;
	}

	public boolean restApi() {
		return false;
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
