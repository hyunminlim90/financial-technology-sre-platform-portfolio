package com.fintech.sre.agent.model.common;

import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.model.response.PolicyDecisionView;
import com.fintech.sre.agent.model.response.PolicyViolationView;

public record RecommendedAction(
		Integer step,
		String action,
		ActionCommand command,
		String expectedEffect,
		String risk,
		String rollbackPlan,
		List<String> verification,
		Boolean requiresHumanApproval,
		ActionSource source,
		String candidateGenerationSource,
		PolicyDecisionView policyDecision,
		List<PolicyViolationView> policyViolations,
		String guardrailDecision,
		String blockedReason
) {
	public RecommendedAction(
			Integer step,
			String action,
			ActionCommand command,
			String expectedEffect,
			String risk,
			String rollbackPlan,
			List<String> verification,
			Boolean requiresHumanApproval,
			ActionSource source
	) {
		this(
				step,
				action,
				command,
				expectedEffect,
				risk,
				rollbackPlan,
				verification,
				requiresHumanApproval,
				source,
				null,
				null,
				List.of(),
				null,
				null
		);
	}
}
