package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record PostExecutionRegressionRequirement(
		PostExecutionConvergenceDecision convergenceDecision,
		List<RegressionSignal> signals
) {
	public PostExecutionRegressionRequirement {
		Objects.requireNonNull(
				convergenceDecision,
				"convergenceDecision must not be null"
		);
		Objects.requireNonNull(signals, "signals must not be null");
		signals = List.copyOf(signals);
	}

	public boolean postExecutionConverged() {
		return convergenceDecision.converged();
	}

	public boolean hasSignals() {
		return !signals.isEmpty();
	}

	public boolean requiresReverificationAndReconvergence() {
		return hasSignals();
	}
}
