package com.fintech.sre.agent.runbook;

public record RunbookPaymentSafety(
		boolean idempotencySafe,
		boolean stateTransitionSafe,
		String duplicateExecutionRisk
) {
}
