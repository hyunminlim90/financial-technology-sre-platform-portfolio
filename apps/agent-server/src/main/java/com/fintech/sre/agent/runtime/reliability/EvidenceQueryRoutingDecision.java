package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceQueryRoutingDecision(
		EvidenceAdapterSelection selection,
		EvidenceQueryRoute route
) {
	public EvidenceQueryRoutingDecision {
		Objects.requireNonNull(selection, "selection must not be null");
		Objects.requireNonNull(route, "route must not be null");
	}

	public boolean accepted() {
		return route.accepted();
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
