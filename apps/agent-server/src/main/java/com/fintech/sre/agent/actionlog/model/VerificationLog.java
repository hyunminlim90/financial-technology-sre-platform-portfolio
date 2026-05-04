package com.fintech.sre.agent.actionlog.model;

import java.time.Instant;

public record VerificationLog(
		Long executedActionId,
		String metricName,
		String query,
		Double beforeValue,
		Double afterValue,
		String expectedCondition,
		String status,
		Instant checkedAt
) {
}
