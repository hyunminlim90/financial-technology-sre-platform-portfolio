package com.fintech.sre.agent.decision.action;

import java.util.List;

public record VerificationPolicy(
		boolean required,
		List<String> checks
) {
	public VerificationPolicy(boolean required) {
		this(required, List.of());
	}
}
