package com.fintech.sre.agent.decision;

import java.util.List;

import com.fintech.sre.agent.action.ActionCommand;

import lombok.Builder;

@Builder(toBuilder = true)
public record CandidateAction(
		int step,
		String action,
		ActionCommand command,
		String expectedEffect,
		String risk,
		String rollbackPlan,
		List<String> verification,
		boolean requiresHumanApproval,
		ActionSource source,
		ActionRiskLevel riskLevel
) {
}
