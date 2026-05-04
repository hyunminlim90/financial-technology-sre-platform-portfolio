package com.fintech.sre.agent.policy;

import java.util.List;

public record PolicyEvaluationResult(
		boolean allowed,
		List<PolicyViolation> violations
) {
	public static PolicyEvaluationResult allow() {
		return new PolicyEvaluationResult(true, List.of());
	}

	public static PolicyEvaluationResult deny(List<PolicyViolation> violations) {
		return new PolicyEvaluationResult(false, violations);
	}
}
