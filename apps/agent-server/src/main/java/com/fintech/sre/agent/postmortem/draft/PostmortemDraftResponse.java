package com.fintech.sre.agent.postmortem.draft;

import java.util.List;

public record PostmortemDraftResponse(
		String postmortemDraftId,
		String incidentId,
		PostmortemDraftStatus status,
		String summary,
		List<String> openQuestions
) {
	public PostmortemDraftResponse {
		openQuestions = openQuestions == null ? List.of() : List.copyOf(openQuestions);
	}
}
