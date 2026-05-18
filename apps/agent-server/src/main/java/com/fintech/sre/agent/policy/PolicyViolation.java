package com.fintech.sre.agent.policy;

public record PolicyViolation(
		String code,
		PolicySeverity severity,
		String message,
		String evidenceRef
) {
	public PolicyViolation(String code, PolicySeverity severity, String message) {
		this(code, severity, message, null);
	}
}
