package com.fintech.sre.agent.runtime.recommendation;

import java.util.Objects;

public record RecommendationCandidateIntegrationResult(
		RecommendationCandidate recommendationCandidate,
		RecommendationCandidateIntegrationStatus status,
		RecommendationCandidateIntegrationReason reason,
		RecommendationCandidateIntegrationScope scope,
		boolean recommendationGenerationReadyView,
		boolean recommendationCertaintyAllowed
) {
	public RecommendationCandidateIntegrationResult {
		Objects.requireNonNull(
				recommendationCandidate,
				"recommendationCandidate must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendation() {
		return false;
	}

	public boolean recommendationMutation() {
		return false;
	}

	public boolean approval() {
		return false;
	}

	public boolean actionCommand() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
