package com.fintech.sre.agent.postmortem.review;

public record PostmortemReviewResponse(
		String postmortemReviewId,
		String postmortemDraftId,
		String incidentId,
		PostmortemReviewStatus status,
		String reviewedBy,
		String reviewSummary
) {
}
