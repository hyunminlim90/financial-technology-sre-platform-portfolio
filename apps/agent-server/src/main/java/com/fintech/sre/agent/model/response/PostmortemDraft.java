package com.fintech.sre.agent.model.response;

import java.util.List;

public record PostmortemDraft(
		String overview,
		String impact,
		List<TimelineEvent> timeline,
		List<String> symptoms,
		List<String> rootCauseHypotheses,
		List<String> contributingFactors,
		List<String> whatWentWell,
		List<String> whatWentWrong,
		List<String> actionItems,
		List<String> lessonsLearned,
		List<String> reproductionNotes
) {
}
