package com.fintech.sre.agent.actionlog.model;

import java.util.List;

public record IncidentActionLogSnapshot(
		String incidentId,
		List<RecommendationLog> recommendations,
		List<ExecutedActionLog> executedActions,
		List<VerificationLog> verifications,
		List<RollbackLog> rollbacks
) {
}
