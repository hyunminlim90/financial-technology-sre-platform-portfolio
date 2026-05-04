package com.fintech.sre.agent.actionlog;

import java.util.List;

public record ActionOutcomeRequest(
		ActionOutcomeStatus outcomeStatus,
		String outcomeSummary,
		List<String> observedSignals
) {
}
