package com.fintech.sre.agent.runtime.reliability;

import java.time.Duration;
import java.util.Objects;

public record ConvergenceWindow(
		Duration requiredStabilization,
		Duration observedStabilization
) {
	public ConvergenceWindow {
		Objects.requireNonNull(
				requiredStabilization,
				"requiredStabilization must not be null"
		);
		Objects.requireNonNull(
				observedStabilization,
				"observedStabilization must not be null"
		);
		if (requiredStabilization.isNegative() || requiredStabilization.isZero()) {
			throw new IllegalArgumentException(
					"requiredStabilization must be positive"
			);
		}
		if (observedStabilization.isNegative()) {
			throw new IllegalArgumentException(
					"observedStabilization must not be negative"
			);
		}
	}

	public boolean satisfied() {
		return observedStabilization.compareTo(requiredStabilization) >= 0;
	}
}
