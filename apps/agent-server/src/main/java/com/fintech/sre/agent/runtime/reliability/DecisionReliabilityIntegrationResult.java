package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record DecisionReliabilityIntegrationResult(
		DecisionReliability decisionReliability,
		EvidenceRuntimeApiResponse apiResponse,
		DecisionReliabilityIntegrationStatus status,
		DecisionReliabilityIntegrationReason reason,
		DecisionReliabilityIntegrationScope scope,
		boolean lifecycleStableAllowed,
		boolean recommendationCertaintyAllowed
) {
	public DecisionReliabilityIntegrationResult {
		Objects.requireNonNull(
				decisionReliability,
				"decisionReliability must not be null"
		);
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesDecision() {
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
