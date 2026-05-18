package com.fintech.sre.agent.postmortem.draft;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PostmortemDraftRecord(
		String postmortemDraftId,
		String incidentId,
		PostmortemDraftStatus status,
		String requestedBy,
		String summary,
		List<String> timeline,
		List<String> recommendations,
		List<String> executionResults,
		List<String> verificationResults,
		List<String> reanalysisCandidates,
		List<String> learningCandidates,
		List<String> openQuestions,
		Instant createdAt,
		Map<String, String> metadata
) {
	public PostmortemDraftRecord {
		timeline = timeline == null ? List.of() : List.copyOf(timeline);
		recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
		executionResults = executionResults == null ? List.of() : List.copyOf(executionResults);
		verificationResults = verificationResults == null ? List.of() : List.copyOf(verificationResults);
		reanalysisCandidates = reanalysisCandidates == null ? List.of() : List.copyOf(reanalysisCandidates);
		learningCandidates = learningCandidates == null ? List.of() : List.copyOf(learningCandidates);
		openQuestions = openQuestions == null ? List.of() : List.copyOf(openQuestions);
		metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
	}
}
