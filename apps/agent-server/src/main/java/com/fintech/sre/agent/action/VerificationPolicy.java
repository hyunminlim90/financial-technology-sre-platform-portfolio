package com.fintech.sre.agent.action;

import java.util.List;

public record VerificationPolicy(
		boolean required,
		List<String> checks
) {
	public VerificationPolicy {
		checks = checks == null ? List.of() : List.copyOf(checks);
	}

	public static VerificationPolicy required(List<String> checks) {
		return new VerificationPolicy(true, checks == null ? List.of() : List.copyOf(checks));
	}
}
