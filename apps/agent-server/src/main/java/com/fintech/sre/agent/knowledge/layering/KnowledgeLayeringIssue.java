package com.fintech.sre.agent.knowledge.layering;

public record KnowledgeLayeringIssue(
		String code,
		KnowledgeLayeringIssueSeverity severity,
		String message
) {
}
