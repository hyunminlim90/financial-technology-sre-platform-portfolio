package com.fintech.sre.agent.improvement;

import java.time.Instant;
import java.util.List;

public record ImprovementCandidate(
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
	public ImprovementCandidate accept(String reason) {
		return new ImprovementCandidate(
				id,
				incidentId,
				actionLogId,
				type,
				ImprovementCandidateStatus.ACCEPTED_BY_HUMAN,
				title,
				this.reason,
				targetKnowledgePath,
				evidence,
				reason,
				createdAt,
				Instant.now()
		);
	}

	public ImprovementCandidate reject(String reason) {
		return new ImprovementCandidate(
				id,
				incidentId,
				actionLogId,
				type,
				ImprovementCandidateStatus.REJECTED_BY_HUMAN,
				title,
				this.reason,
				targetKnowledgePath,
				evidence,
				reason,
				createdAt,
				Instant.now()
		);
	}

	public ImprovementCandidate markAppliedExternally(String reason) {
		return new ImprovementCandidate(
				id,
				incidentId,
				actionLogId,
				type,
				ImprovementCandidateStatus.APPLIED_EXTERNALLY,
				title,
				this.reason,
				targetKnowledgePath,
				evidence,
				reason,
				createdAt,
				Instant.now()
		);
	}
}
