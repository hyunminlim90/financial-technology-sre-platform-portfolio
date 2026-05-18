package com.fintech.sre.agent.recommendation.verification;

import java.time.Instant;
import java.util.Map;

public record VerificationResultRecord(
		String verificationResultId,
		String executionResultId,
		String executionPlanId,
		String recommendationRecordId,
		String incidentId,
		VerificationStatus status,
		String operatorId,
		String summary,
		Instant verifiedAt,
		Map<String, String> metadata
) {
	public VerificationResultRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
