package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record EvidenceLineageEdge(
		EvidenceLineageNode from,
		EvidenceLineageNode to
) {
	public EvidenceLineageEdge {
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");
	}

	public boolean traceabilityOnly() {
		return true;
	}
}
