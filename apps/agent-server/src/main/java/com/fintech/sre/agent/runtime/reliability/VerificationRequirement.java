package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record VerificationRequirement(
		VerificationRequirementType type,
		EvidenceCorrelation correlation
) {
	public VerificationRequirement {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(correlation, "correlation must not be null");
	}
}
