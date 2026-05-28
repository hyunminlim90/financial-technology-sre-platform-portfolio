package com.fintech.sre.agent.runtime.reliability;

import java.util.Objects;

public record ReliabilityLifecycleSummary(
		ReliabilityLifecycleSummaryStatus status,
		ReliabilityLifecycleSummaryScope scope,
		boolean trusted,
		OperationalUncertainty risk,
		RuntimeState lifecycleState,
		ReliabilityLifecycleSummaryReason reason
) {
	public ReliabilityLifecycleSummary {
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(risk, "risk must not be null");
		Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
	}

	public boolean recommendation() {
		return false;
	}

	public boolean executionPermission() {
		return false;
	}

	public boolean operatorFacingReadModel() {
		return scope == ReliabilityLifecycleSummaryScope.OPERATOR_READ_MODEL;
	}
}
