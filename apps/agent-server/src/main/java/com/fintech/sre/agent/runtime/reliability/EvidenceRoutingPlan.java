package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceRoutingPlan(
		EvidenceRoutingPlanStatus status,
		EvidenceRoutingPlanScope scope,
		List<EvidenceQueryRoute> routes,
		boolean paymentConsistencyRequired,
		EvidenceRoutingPlanRejectionReason rejectionReason
) {
	public EvidenceRoutingPlan {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(routes, "routes must not be null");
		routes = List.copyOf(routes);
	}

	public boolean accepted() {
		return status != EvidenceRoutingPlanStatus.REJECTED;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean exposesRawCredentialOrConfiguration() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
