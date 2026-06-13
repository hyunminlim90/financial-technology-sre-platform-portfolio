package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceAssessmentPipelineInput(
		EvidenceCollectionResult collectionResult,
		RuntimeState runtimeState,
		PropagationSignal propagationSignal,
		boolean propagationActive,
		boolean rollbackRecentlyApplied,
		ConvergenceWindow convergenceWindow,
		List<RegressionSignal> regressionSignals
) {
	public EvidenceAssessmentPipelineInput {
		Objects.requireNonNull(
				collectionResult,
				"collectionResult must not be null"
		);
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(
				propagationSignal,
				"propagationSignal must not be null"
		);
		Objects.requireNonNull(
				convergenceWindow,
				"convergenceWindow must not be null"
		);
		Objects.requireNonNull(
				regressionSignals,
				"regressionSignals must not be null"
		);
		regressionSignals = List.copyOf(regressionSignals);
	}

	public boolean exposesRawPayload() {
		return false;
	}

	public boolean mutatesPortfolioKnowledgeSource() {
		return false;
	}
}
