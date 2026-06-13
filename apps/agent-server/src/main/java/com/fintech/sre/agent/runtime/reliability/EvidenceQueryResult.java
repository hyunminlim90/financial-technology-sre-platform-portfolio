package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceQueryResult(
		EvidenceSourceType sourceType,
		EvidenceCollectionStatus status,
		List<EvidenceSignal> signals,
		boolean paymentConsistencyMetadataPresent
) {
	public EvidenceQueryResult {
		Objects.requireNonNull(sourceType, "sourceType must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(signals, "signals must not be null");
		signals = List.copyOf(signals);

		if (sourceType == EvidenceSourceType.PAYMENT_CONSISTENCY
				&& !paymentConsistencyMetadataPresent) {
			throw new IllegalArgumentException(
					"payment consistency evidence requires consistency metadata"
			);
		}
	}

	public boolean normalizedSemanticEvidenceOnly() {
		return true;
	}

	public boolean exposesRawObservabilityPayload() {
		return false;
	}

	public boolean maintainsUncertainty() {
		return status == EvidenceCollectionStatus.UNKNOWN;
	}

	public boolean systemFailure() {
		return false;
	}

	public boolean executionAuthority() {
		return false;
	}

	public boolean recommendationAuthority() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
