package com.fintech.sre.agent.decision.report;

import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;
import com.fintech.sre.agent.policy.PolicyViolation;

public record DecisionReportAction(
		String actionText,
		ActionCommand command,
		boolean recommended,
		boolean blocked,
		List<PolicyViolation> policyViolations,
		List<String> guardrailViolations,
		String decisionReason
) {
}
