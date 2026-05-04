package com.fintech.sre.agent.decision.action;

public record PaymentSafety(
		boolean idempotencySafe,
		boolean stateTransitionSafe,
		DuplicateExecutionRisk duplicateExecutionRisk
) {
}
