package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record RegressionSignal(
		RegressionSignalType type,
		String signalId,
		String summary
) {
	public RegressionSignal {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(signalId, "signalId must not be null");
		Objects.requireNonNull(summary, "summary must not be null");

		if (signalId.isBlank()) {
			throw new IllegalArgumentException("signalId must not be blank");
		}
		if (summary.isBlank()) {
			throw new IllegalArgumentException("summary must not be blank");
		}
	}
}
