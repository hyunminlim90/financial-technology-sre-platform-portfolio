package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record ReliabilityRiskClassification(
		ReliabilityRiskLevel level,
		List<ReliabilityRiskFactor> factors,
		ReliabilityRiskReason reason
) {
	public ReliabilityRiskClassification {
		Objects.requireNonNull(level, "level must not be null");
		Objects.requireNonNull(factors, "factors must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		factors = List.copyOf(factors);
	}

	public boolean semanticOnly() {
		return true;
	}

	public boolean executionTrigger() {
		return false;
	}
}
