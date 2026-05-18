package com.fintech.sre.agent.recommendation.verification;

import java.util.Map;

public record VerificationResultRequest(
		VerificationStatus status,
		String operatorId,
		String summary,
		Map<String, String> metadata
) {
}
