package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceCollectionResult(
		List<EvidenceCollectionStage> stages,
		List<EvidenceQueryResult> adapterResults,
		List<EvidenceSignal> normalizedSignals,
		EvidenceCollectionStatus status,
		boolean paymentSafetyUncertain,
		boolean contradictionMarkerPresent,
		OperationalUncertainty uncertainty,
		EvidenceCollectionRejectionReason rejectionReason
) {
	public EvidenceCollectionResult {
		Objects.requireNonNull(stages, "stages must not be null");
		Objects.requireNonNull(adapterResults, "adapterResults must not be null");
		Objects.requireNonNull(
				normalizedSignals,
				"normalizedSignals must not be null"
		);
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(uncertainty, "uncertainty must not be null");
		stages = List.copyOf(stages);
		adapterResults = List.copyOf(adapterResults);
		normalizedSignals = List.copyOf(normalizedSignals);
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean exposesRawObservabilityPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
