package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RecommendationReliability(
		RecommendationReliabilityLevel level,
		RecommendationReliabilityReason reason,
		RecommendationReliabilityScope scope,
		DecisionReliability decisionReliability,
		HumanApprovalDecision humanApprovalDecision
) {
	public RecommendationReliability {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				decisionReliability,
				"decisionReliability must not be null"
		);
	}

	public boolean readOnly() {
		return true;
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

	public boolean actualRecommendation() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
