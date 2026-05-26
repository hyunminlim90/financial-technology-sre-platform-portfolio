package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ReliabilityAssessmentInput(
		RuntimeState runtimeState,
		List<EvidenceSignal> evidenceSignals,
		boolean contradictoryEvidence,
		PropagationSignal propagationSignal,
		boolean propagationActive,
		boolean rollbackRecentlyApplied,
		ConvergenceWindow convergenceWindow,
		List<RegressionSignal> regressionSignals
) {
	public ReliabilityAssessmentInput {
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(
				evidenceSignals,
				"evidenceSignals must not be null"
		);
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
		evidenceSignals = List.copyOf(evidenceSignals);
		regressionSignals = List.copyOf(regressionSignals);
	}
}
