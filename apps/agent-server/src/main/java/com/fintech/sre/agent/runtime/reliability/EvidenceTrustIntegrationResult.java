package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceTrustIntegrationResult(
		EvidenceTrustScore trustScore,
		EvidenceRuntimeApiResponse apiResponse,
		EvidenceTrustIntegrationStatus status,
		EvidenceTrustIntegrationReason reason,
		EvidenceTrustIntegrationScope scope
) {
	public EvidenceTrustIntegrationResult {
		Objects.requireNonNull(trustScore, "trustScore must not be null");
		Objects.requireNonNull(apiResponse, "apiResponse must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
	}

	public boolean readOnly() {
		return true;
	}

	public boolean mutatesEvidence() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
