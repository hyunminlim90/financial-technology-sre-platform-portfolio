package com.fintech.sre.agent.model.response;

import java.util.List;

import com.fintech.sre.agent.model.common.ConfidenceLevel;

public record PostmortemDraftResponse(
		String incidentId,
		String status,
		String recommendedFilename,
		PostmortemFrontMatter frontMatter,
		PostmortemDraft draft,
		List<LearningCandidate> improvementCandidates,
		List<LearningCandidate> preventiveDesignCandidates,
		ConfidenceLevel confidenceLevel,
		Boolean humanValidationRequired,
		List<String> warnings
) {
}
