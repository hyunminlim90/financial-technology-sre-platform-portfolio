package com.fintech.sre.agent.postmortem.review;

import java.util.Map;

public record PostmortemReviewRequest(
		PostmortemReviewStatus status,
		String reviewedBy,
		String reviewReason,
		String reviewSummary,
		Map<String, String> metadata
) {
}
