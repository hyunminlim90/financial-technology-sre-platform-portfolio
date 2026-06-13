package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceAdapterSelection(
		EvidenceSourceType sourceType,
		EvidenceAdapterSelectionScope scope,
		List<EvidenceAdapterSelectionPolicy> policies,
		List<EvidenceAdapterRegistration> registrations,
		EvidenceAdapterSelectionRejectionReason rejectionReason
) {
	public EvidenceAdapterSelection {
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(policies, "policies must not be null");
		Objects.requireNonNull(registrations, "registrations must not be null");
		policies = List.copyOf(policies);
		registrations = List.copyOf(registrations);
	}

	public boolean selected() {
		return !registrations.isEmpty();
	}

	public boolean restricted() {
		return scope == EvidenceAdapterSelectionScope.RESTRICTED_FALLBACK;
	}

	public boolean uncertain() {
		return scope == EvidenceAdapterSelectionScope.UNCERTAIN_FALLBACK;
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
