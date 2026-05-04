package com.fintech.sre.agent.knowledge.layering;

import java.util.List;

public record KnowledgeLayeringValidationResult(
		boolean valid,
		List<KnowledgeLayeringIssue> issues
) {
	public static KnowledgeLayeringValidationResult valid(List<KnowledgeLayeringIssue> issues) {
		return new KnowledgeLayeringValidationResult(true, issues == null ? List.of() : issues);
	}

	public static KnowledgeLayeringValidationResult invalid(List<KnowledgeLayeringIssue> issues) {
		return new KnowledgeLayeringValidationResult(false, issues == null ? List.of() : issues);
	}

	public boolean hasBlockingIssue() {
		return issues != null && issues.stream()
				.anyMatch(issue -> issue.severity() == KnowledgeLayeringIssueSeverity.BLOCKING);
	}
}
