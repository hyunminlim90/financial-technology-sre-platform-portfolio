package com.fintech.sre.agent.runbook;

import java.util.List;

public record RunbookDefinition(
		String id,
		String scenario,
		String title,
		String description,
		List<String> requiredEvidence,
		List<RunbookBranch> branches,
		List<String> forbiddenActions
) {
}
