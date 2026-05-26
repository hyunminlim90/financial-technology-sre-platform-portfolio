package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ConvergenceAssessment(
		RuntimeState runtimeState,
		ConvergenceEvidence evidence,
		ConvergenceWindow convergenceWindow,
		PropagationSignal propagationSignal,
		boolean propagationActive,
		boolean rollbackRecentlyApplied
) {
	public ConvergenceAssessment {
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(evidence, "evidence must not be null");
		Objects.requireNonNull(
				convergenceWindow,
				"convergenceWindow must not be null"
		);
		Objects.requireNonNull(
				propagationSignal,
				"propagationSignal must not be null"
		);
	}

	public boolean temporalStabilitySatisfied() {
		return convergenceWindow.satisfied();
	}

	public boolean canDegradeAfterConvergence() {
		return runtimeState == RuntimeState.CONVERGED
				|| runtimeState == RuntimeState.VERIFIED;
	}
}
