package com.fintech.sre.agent.operator;

import java.util.List;

import com.fintech.sre.agent.actionlog.ActionOutcomeStatus;

public record OperatorOutcomeReportRequest(
		ActionOutcomeStatus outcomeStatus,
		String outcomeSummary,
		List<String> observedSignals
) {
}
