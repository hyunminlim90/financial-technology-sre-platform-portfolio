package com.fintech.sre.agent.actionlog.model;

import java.time.Instant;
import java.util.List;

public record ExecutedActionLog(
		Long actionId,
		String recommendationId,
		String action,
		String executedBy,
		Instant executedAt,
		String executionMethod,
		String executionDetail,
		String expectedEffect,
		String actualEffect,
		String rollbackPlan,
		Boolean rollbackExecuted,
		List<VerificationLog> verifications,
		List<RollbackLog> rollbacks
) {
}
