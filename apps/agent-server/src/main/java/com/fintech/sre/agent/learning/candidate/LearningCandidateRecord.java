package com.fintech.sre.agent.learning.candidate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LearningCandidateRecord(
		String learningCandidateId,
		String incidentId,
		String postmortemDraftId,
		String postmortemReviewId,
		LearningCandidateType type,
		LearningCandidateStatus status,
		String promotedBy,
		String summary,
		List<String> proposedChanges,
		Instant createdAt,
		Map<String, String> metadata
) {
	public LearningCandidateRecord {
		proposedChanges = proposedChanges == null ? List.of() : List.copyOf(proposedChanges);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
