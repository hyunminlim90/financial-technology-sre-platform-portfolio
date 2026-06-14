package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public record RecommendationContentIntegrationResult(
		RecommendationContent content,
		RecommendationContentIntegrationStatus status,
		RecommendationContentIntegrationReason reason,
		RecommendationContentIntegrationScope scope,
		boolean operatorFacingContentVisible,
		boolean contentExposureCertaintyAllowed
) {
	public RecommendationContentIntegrationResult {
		Objects.requireNonNull(content, "content must not be null");
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
