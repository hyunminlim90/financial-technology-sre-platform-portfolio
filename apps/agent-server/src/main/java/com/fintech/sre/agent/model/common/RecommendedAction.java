package com.fintech.sre.agent.model.common;

import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;

public record RecommendedAction(
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
}
