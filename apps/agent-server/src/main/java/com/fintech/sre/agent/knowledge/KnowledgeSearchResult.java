package com.fintech.sre.agent.knowledge;

import java.util.List;

public record KnowledgeSearchResult(
		List<KnowledgeDocument> documents
) {
	public KnowledgeSearchResult {
		documents = documents == null ? List.of() : List.copyOf(documents);
	}

	public static KnowledgeSearchResult empty() {
		return new KnowledgeSearchResult(List.of());
	}

	public boolean isEmpty() {
		return documents == null || documents.isEmpty();
	}
}
