package com.fintech.sre.agent.postmortem;

import java.time.Instant;
import java.util.List;

public record PostmortemDraftResponse(
		String incidentId,
		String title,
		String rootCause,
		String impactSummary,
		List<String> learningCandidates,
		String markdown,
		Instant createdAt,
		boolean requiresHumanReview
) {
}
