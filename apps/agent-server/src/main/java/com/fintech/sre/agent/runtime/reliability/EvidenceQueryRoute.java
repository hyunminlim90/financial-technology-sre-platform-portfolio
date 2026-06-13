package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceQueryRoute(
		EvidenceSourceType sourceType,
		EvidenceQueryRoutingScope scope,
		List<EvidenceAdapterRegistration> registrations,
		boolean paymentSupporting,
		EvidenceQueryRoutingRejectionReason rejectionReason
) {
	public EvidenceQueryRoute {
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(registrations, "registrations must not be null");
		registrations = List.copyOf(registrations);
	}

	public boolean accepted() {
		return !registrations.isEmpty()
				&& scope != EvidenceQueryRoutingScope.REJECTED_ROUTE;
	}

	public boolean restricted() {
		return scope == EvidenceQueryRoutingScope.RESTRICTED_ROUTE;
	}

	public boolean uncertain() {
		return scope == EvidenceQueryRoutingScope.UNCERTAIN_ROUTE;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean exposesRawCredentialOrConfiguration() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
