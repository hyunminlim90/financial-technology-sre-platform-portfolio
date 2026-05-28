package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record RecommendationEligibility(
		boolean eligible,
		RecommendationScope scope,
		boolean verificationRequirement,
		boolean rollbackRequirement,
		List<RecommendationRestriction> restrictions,
		List<RecommendationBoundaryReason> reasons
) {
	public RecommendationEligibility {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(
				restrictions,
				"restrictions must not be null"
		);
		Objects.requireNonNull(reasons, "reasons must not be null");
		restrictions = List.copyOf(restrictions);
		reasons = List.copyOf(reasons);
	}

	public boolean advisoryOnly() {
		return scope != RecommendationScope.NONE;
	}

	public boolean executionPermission() {
		return false;
	}
}
