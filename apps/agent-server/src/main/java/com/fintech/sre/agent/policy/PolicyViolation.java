package com.fintech.sre.agent.policy;

public record PolicyViolation(
		String code,
		PolicySeverity severity,
		String message
) {
}
