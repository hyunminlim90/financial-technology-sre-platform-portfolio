package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record RegressionAssessment(
		RuntimeState runtimeState,
		ConvergenceStatus convergenceStatus,
		List<RegressionSignal> signals
) {
	public RegressionAssessment {
		Objects.requireNonNull(runtimeState, "runtimeState must not be null");
		Objects.requireNonNull(
				convergenceStatus,
				"convergenceStatus must not be null"
		);
		Objects.requireNonNull(signals, "signals must not be null");
		signals = List.copyOf(signals);
	}

	public boolean postConvergence() {
		return runtimeState == RuntimeState.CONVERGED
				|| convergenceStatus == ConvergenceStatus.CONVERGED;
	}

	public boolean hasSignals() {
		return !signals.isEmpty();
	}

	public boolean requiresReverification() {
		return hasSignals();
	}
}
