package com.fintech.sre.agent.decision.policy;

public record PolicyViolation(
		String policyId,
		String message
) {
}
