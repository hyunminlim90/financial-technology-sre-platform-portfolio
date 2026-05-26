package com.fintech.sre.agent.runtime.reliability;

import java.util.List;
import java.util.Objects;

public record EvidenceContext(
		List<String> evidenceIds,
		boolean sufficientEvidence,
		boolean humanValidated
) {
	public EvidenceContext {
		Objects.requireNonNull(evidenceIds, "evidenceIds must not be null");
		evidenceIds = List.copyOf(evidenceIds);
	}

	public boolean actionable() {
		return sufficientEvidence && humanValidated;
	}
}
