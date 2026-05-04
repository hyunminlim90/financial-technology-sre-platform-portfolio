package com.fintech.sre.agent.action;

public record VerificationCommand(
		String metric,
		String condition,
		String description
) {
}
