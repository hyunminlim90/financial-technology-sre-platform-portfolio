package com.fintech.sre.agent.runbook;

import java.util.List;

public record RunbookBranch(
		String id,
		RunbookCondition when,
		List<RunbookAction> actions
) {
}
