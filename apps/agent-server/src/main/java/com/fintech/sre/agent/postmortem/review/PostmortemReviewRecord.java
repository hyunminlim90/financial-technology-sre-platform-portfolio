package com.fintech.sre.agent.postmortem.review;

import java.time.Instant;
import java.util.Map;

public record PostmortemReviewRecord(
		String postmortemReviewId,
		String postmortemDraftId,
		String incidentId,
		PostmortemReviewStatus status,
		String reviewedBy,
		String reviewReason,
		String reviewSummary,
		Instant reviewedAt,
		Map<String, String> metadata
) {
	public PostmortemReviewRecord {
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
