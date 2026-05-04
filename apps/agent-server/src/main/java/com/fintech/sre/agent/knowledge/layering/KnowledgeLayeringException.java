package com.fintech.sre.agent.knowledge.layering;

import java.util.List;

public class KnowledgeLayeringException extends RuntimeException {

	private final List<KnowledgeLayeringIssue> issues;

	public KnowledgeLayeringException(List<KnowledgeLayeringIssue> issues) {
		super("Knowledge layering validation failed");
		this.issues = issues;
	}

	public List<KnowledgeLayeringIssue> issues() {
		return issues;
	}
}
