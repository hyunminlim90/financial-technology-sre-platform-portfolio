package com.fintech.sre.agent.improvement;

import java.time.Instant;
import java.util.List;

public record ImprovementCandidateResponse(
		String id,
		String incidentId,
		String actionLogId,
		ImprovementCandidateType type,
		ImprovementCandidateStatus status,
		String title,
		String reason,
		String targetKnowledgePath,
		List<String> evidence,
		String humanDecisionReason,
		Instant createdAt,
		Instant updatedAt
) {
	public static ImprovementCandidateResponse from(ImprovementCandidate candidate) {
		return new ImprovementCandidateResponse(
				candidate.id(),
				candidate.incidentId(),
				candidate.actionLogId(),
				candidate.type(),
				candidate.status(),
				candidate.title(),
				candidate.reason(),
				candidate.targetKnowledgePath(),
				candidate.evidence(),
				candidate.humanDecisionReason(),
				candidate.createdAt(),
				candidate.updatedAt()
		);
	}
}
