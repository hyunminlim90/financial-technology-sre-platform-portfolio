package com.fintech.sre.agent.runtime.readiness;

import java.util.Objects;

import com.fintech.sre.agent.runtime.reliability.OperationalUncertainty;
import com.fintech.sre.agent.runtime.reliability.RecommendationReliability;

public record RecommendationReadiness(
		RecommendationReadinessLevel level,
		RecommendationReadinessReason reason,
		RecommendationReadinessScope scope,
		RecommendationReliability recommendationReliability,
		OperationalUncertainty lifecycleRisk,
		boolean lifecycleUncertaintyDetected
) {
	public RecommendationReadiness {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				recommendationReliability,
				"recommendationReliability must not be null"
		);
		Objects.requireNonNull(lifecycleRisk, "lifecycleRisk must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean recommendationGeneration() {
		return false;
	}

	public boolean operatorExposure() {
		return false;
	}

	public boolean approvalRequest() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean actionCommandGeneration() {
		return false;
	}
}
