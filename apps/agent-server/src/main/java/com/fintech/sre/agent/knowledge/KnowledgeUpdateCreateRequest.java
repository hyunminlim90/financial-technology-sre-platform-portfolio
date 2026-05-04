package com.fintech.sre.agent.knowledge;

import java.util.List;

public record KnowledgeUpdateCreateRequest(
		String incidentId,
		String improvementCandidateId,
		KnowledgeUpdateType type,
		String targetKnowledgePath,
		String title,
		String reason,
		List<String> evidence,
		String proposedContentSummary
) {
}
