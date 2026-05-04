package com.fintech.sre.agent.postmortem;

import java.time.Instant;
import java.util.List;

import com.fintech.sre.agent.actionlog.ActionLog;

public record PostmortemDraft(
		String incidentId,
		String title,
		String rootCause,
		String impactSummary,
		List<ActionLog> actionLogs,
		List<String> learningCandidates,
		Instant createdAt,
		boolean requiresHumanReview
) {
}
