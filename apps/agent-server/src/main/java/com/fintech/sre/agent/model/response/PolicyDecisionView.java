package com.fintech.sre.agent.model.response;

import java.util.List;

import com.fintech.sre.agent.policy.PolicyEvaluationResult;

public record PolicyDecisionView(
		String decision,
		List<PolicyViolationView> violations
) {
	public static PolicyDecisionView from(PolicyEvaluationResult result) {
		if (result == null) {
			return new PolicyDecisionView("UNKNOWN", List.of());
		}

		return new PolicyDecisionView(
				result.decision().name(),
				result.violations().stream()
						.map(PolicyViolationView::from)
						.toList()
		);
	}
}
