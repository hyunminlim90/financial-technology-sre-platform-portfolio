package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ReliabilityLifecycleSummaryView(
		ReliabilityLifecycleSummaryViewStatus status,
		boolean auditTrusted,
		OperationalUncertainty risk,
		boolean regressionDetected,
		ReliabilityLifecycleSummaryReason uncertaintyReason
) {
	public ReliabilityLifecycleSummaryView {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(risk, "risk must not be null");
		Objects.requireNonNull(
				uncertaintyReason,
				"uncertaintyReason must not be null"
		);
	}
}
