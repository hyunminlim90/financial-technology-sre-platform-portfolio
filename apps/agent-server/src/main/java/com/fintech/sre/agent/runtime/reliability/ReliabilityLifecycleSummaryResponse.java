package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ReliabilityLifecycleSummaryResponse(
		ReliabilityLifecycleSummaryView summary
) {
	public ReliabilityLifecycleSummaryResponse {
		Objects.requireNonNull(summary, "summary must not be null");
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean exposesRawEvidencePayload() {
		return false;
	}
}
