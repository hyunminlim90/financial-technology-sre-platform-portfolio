package com.fintech.sre.agent.recommendation.approval;

import java.util.Map;

public record RecommendationApprovalRequest(
		RecommendationApprovalDecision decision,
		String operatorId,
		String reason,
		Map<String, String> metadata
) {
}
