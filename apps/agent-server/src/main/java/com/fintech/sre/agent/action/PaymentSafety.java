package com.fintech.sre.agent.action;

public record PaymentSafety(
		boolean idempotencySafe,
		boolean stateTransitionSafe,
		DuplicateExecutionRisk duplicateExecutionRisk
) {
	public static PaymentSafety requiredSafe() {
		return new PaymentSafety(true, true, DuplicateExecutionRisk.LOW);
	}

	public boolean unsafe() {
		return !idempotencySafe
				|| !stateTransitionSafe
				|| duplicateExecutionRisk == DuplicateExecutionRisk.HIGH;
	}
}
