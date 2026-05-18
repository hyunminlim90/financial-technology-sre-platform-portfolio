package com.fintech.sre.agent.model.common;

import java.util.List;

import com.fintech.sre.agent.model.response.PolicyDecisionView;
import com.fintech.sre.agent.model.response.PolicyViolationView;

public record ForbiddenAction(
		String action,
		String reason,
		String candidateGenerationSource,
		PolicyDecisionView policyDecision,
		List<PolicyViolationView> policyViolations,
		String guardrailDecision,
		String blockedReason
) {
	public ForbiddenAction(
			String action,
			String reason
	) {
		this(action, reason, null, null, List.of(), null, reason);
	}
}
