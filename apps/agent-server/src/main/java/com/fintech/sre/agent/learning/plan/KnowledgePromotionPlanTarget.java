package com.fintech.sre.agent.learning.plan;

import java.util.List;

public record KnowledgePromotionPlanTarget(
		KnowledgePromotionTargetType targetType,
		String recommendedPath,
		String changeSummary,
		List<String> proposedChanges,
		List<String> validationChecklist
) {
	public KnowledgePromotionPlanTarget {
		proposedChanges = proposedChanges == null ? List.of() : List.copyOf(proposedChanges);
		validationChecklist = validationChecklist == null ? List.of() : List.copyOf(validationChecklist);
	}
}
