package com.fintech.sre.agent.policy;

import java.util.List;

public record PolicyEvaluationResult(
		PolicyDecision decision,
		List<PolicyViolation> violations
) {
	public PolicyEvaluationResult {
		violations = violations == null ? List.of() : List.copyOf(violations);
	}

	public static PolicyEvaluationResult allow() {
		return new PolicyEvaluationResult(PolicyDecision.ALLOW, List.of());
	}

	public static PolicyEvaluationResult deny(List<PolicyViolation> violations) {
		return new PolicyEvaluationResult(
				PolicyDecision.DENY,
				violations == null ? List.of() : List.copyOf(violations)
		);
	}

	public boolean allowed() {
		return decision == PolicyDecision.ALLOW;
	}

	public boolean denied() {
		return decision == PolicyDecision.DENY;
	}

	public boolean hasBlockingViolation() {
		return violations.stream()
				.anyMatch(violation -> violation.severity() == PolicySeverity.BLOCKING);
	}
}
