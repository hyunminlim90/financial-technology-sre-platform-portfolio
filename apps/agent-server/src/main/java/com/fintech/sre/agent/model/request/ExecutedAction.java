package com.fintech.sre.agent.model.request;

import java.time.Instant;
import java.util.List;

public record ExecutedAction(
		Integer step,
		String action,
		Instant executedAt,
		String executedBy,
		String expectedEffect,
		String actualEffect,
		String rollbackPlan,
		Boolean rollbackExecuted,
		List<String> verificationResult
) {
}
