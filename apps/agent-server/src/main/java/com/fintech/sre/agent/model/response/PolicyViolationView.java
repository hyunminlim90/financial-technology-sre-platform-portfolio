package com.fintech.sre.agent.model.response;

import com.fintech.sre.agent.policy.PolicyViolation;

public record PolicyViolationView(
		String code,
		String severity,
		String message,
		String evidenceRef
) {
	public static PolicyViolationView from(PolicyViolation violation) {
		return new PolicyViolationView(
				violation.code(),
				violation.severity().name(),
				violation.message(),
				violation.evidenceRef()
		);
	}

	public static java.util.List<PolicyViolationView> fromAll(com.fintech.sre.agent.policy.PolicyEvaluationResult result) {
		if (result == null || result.violations() == null) {
			return java.util.List.of();
		}

		return result.violations().stream()
				.map(PolicyViolationView::from)
				.toList();
	}
}
